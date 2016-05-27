package com.reconinstruments.utils.installer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;

public class FirmwareUtils {

    private static final String TAG = "FirmwareUtils";
    
    public static final File JET_UPDATE_BIN_STORAGE     = new File(Environment.getExternalStorageDirectory()+"/ReconApps/cache/update.bin");
    public static final File JET_UPDATE_BIN_CACHE       = new File("/cache/update.bin");
    public static final File JET_COMMAND_RECOVERY       = new File("/cache/recovery/command");
    public static final File JET_COMMAND_RECOVERY_BAK   = new File("/cache/command_bak");
    public static final File JET_RECOVERY_PATH          = new File("/cache/recovery");

    public static final String SYSTEM_PROP_UPDATE_SHOWN     = "jet.show_update_popup";
    
    /**
     * Initiates the jet firmware upgrade
     * 
     * @return boolean successfully started?
     */
    public static boolean doJetFirmwareUpgrade(final Context context) {
        Log.d (TAG,"doJetFirmwareUpgrade() called");

        if (JET_COMMAND_RECOVERY_BAK.renameTo(JET_COMMAND_RECOVERY))
            Log.d(TAG, JET_COMMAND_RECOVERY_BAK + " was renamed to " + JET_COMMAND_RECOVERY);

        ReconPackageRecorder.PACKAGES_XML_FILE.delete();

        Log.d (TAG,"rebooting the device");

        context.sendBroadcast(new Intent("bShutDown"));

        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Log.e(TAG, e.getMessage());
                }
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                pm.reboot("recovery");
            }
        }.start();

        return true; // unreachable haha
    }

    /**
     * Helper Method for creating command file : /cache/recovery/command
     * 
     * @return isCommandFileCreated
     */
    public static boolean createUpdateCommand() {
        JET_COMMAND_RECOVERY_BAK.delete(); // run for failsafe purpose
        OutputStream out;
        try {
            out = new FileOutputStream(JET_COMMAND_RECOVERY_BAK);
            out.write("--update_package=/cache/update.bin".getBytes());
            out.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getLocalizedMessage());
            return false;
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
            return false;
        }
        return true;
    }

    public static void discardUpdateFiles() {
        // Erase Completely
        JET_UPDATE_BIN_STORAGE.delete();
        JET_UPDATE_BIN_CACHE.delete();
        JET_COMMAND_RECOVERY.delete();
        JET_COMMAND_RECOVERY_BAK.delete();
    }
}
