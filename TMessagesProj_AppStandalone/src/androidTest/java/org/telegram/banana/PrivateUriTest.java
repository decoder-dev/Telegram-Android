package org.telegram.banana;

import android.net.Uri;
import android.os.ParcelFileDescriptor;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import java.io.*;
import java.util.UUID;
import static org.junit.Assert.*;

public class PrivateUriTest {
    private File fixture() throws Exception {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(ApplicationLoader::postInitApplication);
        File file=new File(ApplicationLoader.applicationContext.getFilesDir(),"audit-private-"+UUID.randomUUID()+".bin");
        try(FileOutputStream stream=new FileOutputStream(file)){stream.write(1);}
        return file;
    }

    @Test public void credentialProtectedPathIsRejected() throws Exception {
        File file=fixture();
        try {assertTrue("Canonical app-private paths must be rejected",AndroidUtilities.isInternalUri(Uri.fromFile(file.getCanonicalFile())));}
        finally {file.delete();}
    }

    @Test public void privateFileDescriptorIsRejected() throws Exception {
        File file=fixture();
        try(FileInputStream input=new FileInputStream(file);ParcelFileDescriptor fd=ParcelFileDescriptor.dup(input.getFD())) {
            assertTrue("Opening a descriptor must not bypass app-private path protection",AndroidUtilities.isInternalUri(fd.getFd()));
        } finally {file.delete();}
    }

    @Test public void publicDescriptorCheckDoesNotCloseTheOriginal() throws Exception {
        File file=fixture();
        try(FileInputStream input=new FileInputStream(file)) {
            assertTrue(AndroidUtilities.isInternalFileDescriptor(input.getFD()));
            assertEquals(1,input.read());
        } finally {file.delete();}
    }

    @Test public void deviceProtectedStorageIsRejected() throws Exception {
        fixture().delete();
        File file=new File(ApplicationLoader.applicationContext.createDeviceProtectedStorageContext().getFilesDir(),"audit-device-"+UUID.randomUUID());
        try(FileOutputStream output=new FileOutputStream(file)) {output.write(1);}
        try(FileInputStream input=new FileInputStream(file)) {
            assertTrue(AndroidUtilities.isInternalUri(Uri.fromFile(file)));
            assertTrue(AndroidUtilities.isInternalFileDescriptor(input.getFD()));
        } finally {file.delete();}
    }

    @Test public void externalCacheRemainsImportable() throws Exception {
        File external=new File(AndroidUtilities.getCacheDir(),"audit-external-"+UUID.randomUUID());
        try {
            try(FileOutputStream output=new FileOutputStream(external)) {output.write(2);}
            assertFalse(AndroidUtilities.isInternalUri(Uri.fromFile(external)));
            try(FileInputStream input=new FileInputStream(external)) {assertFalse(AndroidUtilities.isInternalFileDescriptor(input.getFD()));assertEquals(2,input.read());}
        } finally {external.delete();}
    }

    @Test public void exportableThemesAndVoipLogsKeepTheirExistingContract() throws Exception {
        File file=fixture();file.delete();
        File logs=new File(ApplicationLoader.applicationContext.getCacheDir(),"voip_logs");logs.mkdirs();
        File log=new File(logs,"987654321098765.log");
        File theme=new File(ApplicationLoader.applicationContext.getFilesDir(),"audit-theme-"+UUID.randomUUID()+".attheme");
        try {
            try(FileOutputStream out=new FileOutputStream(log)) {out.write(1);}
            try(FileOutputStream out=new FileOutputStream(theme)) {out.write(2);}
            for(File allowed:new File[]{log,theme}) {
                assertFalse(AndroidUtilities.isInternalUri(Uri.fromFile(allowed)));
                try(FileInputStream input=new FileInputStream(allowed)) {assertFalse(AndroidUtilities.isInternalFileDescriptor(input.getFD()));}
            }
        } finally {log.delete();theme.delete();}
    }
    @Test public void externalFileUriRetainsItsCanonicalPath() throws Exception {
        File external=new File(AndroidUtilities.getCacheDir(),"audit-canonical-"+UUID.randomUUID());
        try {
            try(FileOutputStream output=new FileOutputStream(external)) {output.write(3);}
            assertEquals(external.getCanonicalPath(),Uri.fromFile(external).getPath());
            assertFalse(AndroidUtilities.isInternalUri(Uri.fromFile(external)));
        } finally {external.delete();}
    }
}
