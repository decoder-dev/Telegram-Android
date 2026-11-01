package org.telegram.messenger.banana;

import android.util.AtomicFile;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.tl.TL_stars;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.Consumer;

/** Opt-in, local observations of complete public gift lists, never ownership claims. */
public final class GiftHistory {
    private static final int MAX_GIFTS = 1000, MAX_MISSING = 200, MAX_BYTES = 2 * 1024 * 1024;
    private static final DispatchQueue queue = new DispatchQueue("banana-gift-history");
    private static final AtomicLongArray generations = new AtomicLongArray(UserConfig.MAX_ACCOUNT_COUNT);

    private GiftHistory() { }

    private static String key(TL_stars.SavedStarGift gift) {
        if (gift == null || gift.gift == null) throw new IllegalArgumentException("Missing gift");
        if (gift.gift instanceof TL_stars.TL_starGiftUnique) return "unique:" + gift.gift.id;
        if (gift.saved_id != 0) return "saved:" + gift.saved_id;
        if (gift.msg_id != 0) return "message:" + gift.msg_id;
        return "generic:" + gift.gift.id + ":" + gift.date + ":" + DialogObject.getPeerDialogId(gift.from_id)
                + ":" + gift.gift_num + ":" + (gift.message == null ? "" : gift.message.text);
    }

    /** A short or duplicated paginated list is not evidence of disappearance. */
    public static boolean isComplete(List<TL_stars.SavedStarGift> current, int serverCount) {
        if (serverCount < 0 || serverCount > MAX_GIFTS || current.size() != serverCount) return false;
        HashSet<String> seen = new HashSet<>();
        for (TL_stars.SavedStarGift gift : current) {
            if (gift == null || gift.gift == null || !seen.add(key(gift))) return false;
        }
        return true;
    }

    public static ArrayList<TL_stars.SavedStarGift> missing(List<TL_stars.SavedStarGift> previous,
                                                            List<TL_stars.SavedStarGift> current) {
        HashSet<String> present = new HashSet<>();
        for (TL_stars.SavedStarGift gift : current) present.add(key(gift));
        ArrayList<TL_stars.SavedStarGift> result = new ArrayList<>();
        for (TL_stars.SavedStarGift gift : previous) {
            if (!present.add(key(gift))) continue;
            // Upgrades can change the reference. A matching original gift is not gone.
            boolean upgraded = false;
            if (!(gift.gift instanceof TL_stars.TL_starGiftUnique)) {
                for (TL_stars.SavedStarGift candidate : current) {
                    if (!(candidate.gift instanceof TL_stars.TL_starGiftUnique) || candidate.gift.gift_id != gift.gift.id) continue;
                    int originalDate = candidate.date;
                    org.telegram.tgnet.TLRPC.Peer sender = candidate.from_id;
                    for (TL_stars.StarGiftAttribute attribute : candidate.gift.attributes) {
                        if (attribute instanceof TL_stars.starGiftAttributeOriginalDetails) {
                            originalDate = ((TL_stars.starGiftAttributeOriginalDetails) attribute).date;
                            sender = ((TL_stars.starGiftAttributeOriginalDetails) attribute).sender_id;
                            break;
                        }
                    }
                    if (originalDate == gift.date && (sender == null || gift.from_id == null
                            || DialogObject.getPeerDialogId(sender) == DialogObject.getPeerDialogId(gift.from_id))) {
                        upgraded = true;
                        break;
                    }
                }
            }
            if (!upgraded) result.add(gift);
            if (result.size() >= MAX_MISSING) break;
        }
        return result;
    }

    private static File directory(int account) {
        return new File(ApplicationLoader.applicationContext.getNoBackupFilesDir(), "banana-gift-history-" + account);
    }

    public static void clear(int account) {
        generations.incrementAndGet(account);
        queue.postRunnable(() -> {
            File[] files = directory(account).listFiles();
            if (files != null) for (File file : files) file.delete();
            directory(account).delete();
        });
    }

    public static void reconcile(int account, long peer, List<TL_stars.SavedStarGift> current,
                                 int serverCount, Consumer<ArrayList<TL_stars.SavedStarGift>> callback) {
        final long owner = UserConfig.getInstance(account).getClientUserId();
        if (owner == 0 || peer == 0 || peer == owner || !isComplete(current, serverCount)
                || !BananaHooks.showDisappearedGifts(account)) return;
        final boolean test = ConnectionsManager.getInstance(account).isTestBackend();
        final long generation = generations.get(account);
        final ArrayList<TL_stars.SavedStarGift> snapshot = new ArrayList<>(current);
        queue.postRunnable(() -> {
            if (!active(account, owner, test, generation)) return;
            try {
                File dir = directory(account);
                if (!dir.isDirectory() && !dir.mkdirs()) throw new java.io.IOException("Gift cache directory unavailable");
                AtomicFile file = new AtomicFile(new File(dir, owner + "-" + test + "-" + peer + ".json"));
                ArrayList<TL_stars.SavedStarGift> previous = new ArrayList<>();
                if (file.getBaseFile().isFile()) {
                    try {
                        if (file.getBaseFile().length() > MAX_BYTES) throw new java.io.IOException("Gift cache too large");
                        JSONArray stored = new JSONObject(new String(file.readFully(), StandardCharsets.UTF_8)).getJSONArray("gifts");
                        if (stored.length() > MAX_GIFTS + MAX_MISSING) throw new java.io.IOException("Too many cached gifts");
                        for (int i = 0; i < stored.length(); i++) {
                            SerializedData data = new SerializedData(Base64.decode(stored.getString(i), Base64.NO_WRAP));
                            try { previous.add(TL_stars.SavedStarGift.TLdeserialize(data, data.readInt32(true), true)); }
                            finally { data.cleanup(); }
                        }
                    } catch (Exception corrupt) {
                        previous.clear();
                        FileLog.e("BananaGram: invalid gift snapshot discarded");
                    }
                }
                ArrayList<TL_stars.SavedStarGift> disappeared = missing(previous, snapshot);
                ArrayList<TL_stars.SavedStarGift> retained = new ArrayList<>(snapshot);
                retained.addAll(disappeared);
                JSONArray encoded = new JSONArray();
                for (TL_stars.SavedStarGift gift : retained) {
                    SerializedData data = new SerializedData();
                    try { gift.serializeToStream(data); encoded.put(Base64.encodeToString(data.toByteArray(), Base64.NO_WRAP)); }
                    finally { data.cleanup(); }
                }
                byte[] bytes = new JSONObject().put("version", 1).put("gifts", encoded).toString().getBytes(StandardCharsets.UTF_8);
                if (bytes.length > MAX_BYTES || !active(account, owner, test, generation)) return;
                FileOutputStream out = null;
                try {
                    out = file.startWrite();
                    out.write(bytes);
                    file.finishWrite(out);
                } catch (Exception error) {
                    if (out != null) file.failWrite(out);
                    throw error;
                }
                File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
                if (files != null && files.length > 20) {
                    Arrays.sort(files, Comparator.comparingLong(File::lastModified));
                    for (int i = 0; i < files.length - 20; i++) new AtomicFile(files[i]).delete();
                }
                AndroidUtilities.runOnUIThread(() -> {
                    if (active(account, owner, test, generation)) callback.accept(disappeared);
                });
            } catch (Exception error) { FileLog.e("BananaGram: gift snapshot update failed (" + error.getClass().getSimpleName() + ")"); }
        });
    }

    private static boolean active(int account, long owner, boolean test, long generation) {
        return generations.get(account) == generation && UserConfig.getInstance(account).getClientUserId() == owner
                && ConnectionsManager.getInstance(account).isTestBackend() == test && BananaHooks.showDisappearedGifts(account);
    }
}
