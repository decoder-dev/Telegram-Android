package org.telegram.messenger.banana;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Atomic private copies. Call on a worker and pass an already-open source before cache deletion. */
public final class SavedAttachments {
    public static final long MAX_FILE_BYTES = 64L * 1024 * 1024;
    public static final long MAX_TOTAL_BYTES = 512L * 1024 * 1024;
    private static final Object PROCESS_LOCK = new Object();
    private static final String OWN_NAME = "[a-f0-9]{32}\\.[a-z0-9]{1,10}";
    private final File directory;
    private final long maxFile, maxTotal;

    public SavedAttachments(File privateOwnerDirectory) throws IOException {
        this(privateOwnerDirectory, MAX_FILE_BYTES, MAX_TOTAL_BYTES);
    }

    SavedAttachments(File privateOwnerDirectory, long maxFile, long maxTotal) throws IOException {
        if (maxFile <= 0 || maxTotal < maxFile) throw new IllegalArgumentException("Invalid attachment budget");
        if (!privateOwnerDirectory.isDirectory() && !privateOwnerDirectory.mkdirs()) throw new IOException("Cannot create attachment directory");
        directory = privateOwnerDirectory.getCanonicalFile();
        this.maxFile = maxFile;
        this.maxTotal = maxTotal;
    }

    /** Takes ownership of input, including on failure. Original source files are never deleted here. */
    public File save(InputStream input, String extension, long expectedSize) throws IOException {
        try (InputStream source = input) {
            if (source == null) throw new IOException("Missing attachment source");
            if (expectedSize < 0 || expectedSize > maxFile) throw new IOException("Attachment exceeds file budget");
            synchronized (PROCESS_LOCK) {
                try (RandomAccessFile lockFile = new RandomAccessFile(new File(directory, ".lock"), "rw");
                     FileChannel channel = lockFile.getChannel(); FileLock ignored = channel.lock()) {
                    discardInterruptedCopies();
                    long[] usage = usage();
                    if (usage[1] >= 5000 || usage[0] + expectedSize > maxTotal) throw new IOException("Attachment archive budget reached");
                    String ext = extension == null ? "bin" : extension.toLowerCase(Locale.ROOT);
                    if (!ext.matches("[a-z0-9]{1,10}")) ext = "bin";
                    String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
                    File target = new File(directory, name);
                    File temporary = new File(directory, name + ".part");
                    boolean committed = false;
                    try {
                        long copied = 0;
                        try (FileOutputStream output = new FileOutputStream(temporary)) {
                            byte[] buffer = new byte[64 * 1024];
                            int count;
                            while ((count = source.read(buffer)) != -1) {
                                if (count == 0) continue;
                                copied += count;
                                if (copied > maxFile || usage[0] + copied > maxTotal) throw new IOException("Attachment archive budget reached");
                                output.write(buffer, 0, count);
                            }
                            if (expectedSize != 0 && copied != expectedSize) throw new IOException("Attachment changed while copying");
                            output.getFD().sync();
                        }
                        if (target.exists() || !temporary.renameTo(target)) throw new IOException("Cannot commit attachment copy");
                        committed = true;
                        return target;
                    } finally {
                        if (!committed && temporary.exists() && !temporary.delete()) throw new IOException("Cannot discard interrupted attachment");
                    }
                }
            }
        }
    }

    public File resolve(String name) throws IOException {
        if (name == null || !name.matches(OWN_NAME)) return null;
        File file = new File(directory, name);
        return isOwned(file) && file.isFile() ? file : null;
    }

    public boolean delete(String name) throws IOException {
        synchronized (PROCESS_LOCK) {
            try (RandomAccessFile lockFile = new RandomAccessFile(new File(directory, ".lock"), "rw");
                 FileChannel channel = lockFile.getChannel(); FileLock ignored = channel.lock()) {
                File file = resolve(name);
                return file != null && file.delete();
            }
        }
    }

    /** Orphan grace avoids racing another process between atomic file commit and DB linking. */
    public int prune(Set<String> referenced, long olderThanMillis) throws IOException {
        synchronized (PROCESS_LOCK) {
            try (RandomAccessFile lockFile = new RandomAccessFile(new File(directory, ".lock"), "rw");
                 FileChannel channel = lockFile.getChannel(); FileLock ignored = channel.lock()) {
                File[] files = directory.listFiles();
                if (files == null) throw new IOException("Cannot inspect attachment directory");
                int count = 0;
                for (File file : files) {
                    if (file.getName().matches(OWN_NAME) && isOwned(file) && file.isFile() &&
                            !referenced.contains(file.getName()) && file.lastModified() < olderThanMillis) {
                        if (!file.delete()) throw new IOException("Cannot remove orphaned attachment");
                        count++;
                    }
                }
                return count;
            }
        }
    }

    private boolean isOwned(File file) throws IOException {
        return directory.equals(file.getCanonicalFile().getParentFile()) && file.getAbsoluteFile().equals(file.getCanonicalFile());
    }

    private long[] usage() throws IOException {
        File[] files = directory.listFiles();
        if (files == null) throw new IOException("Cannot inspect attachment directory");
        long bytes = 0, count = 0;
        for (File file : files) {
            if (file.getName().matches(OWN_NAME) && isOwned(file) && file.isFile()) { bytes += file.length(); count++; }
        }
        return new long[]{bytes, count};
    }

    private void discardInterruptedCopies() throws IOException {
        File[] files = directory.listFiles();
        if (files == null) throw new IOException("Cannot inspect attachment directory");
        for (File file : files) {
            if (file.getName().matches(OWN_NAME + "\\.part") && isOwned(file) && file.isFile() && !file.delete()) {
                throw new IOException("Cannot discard interrupted attachment");
            }
        }
    }
}
