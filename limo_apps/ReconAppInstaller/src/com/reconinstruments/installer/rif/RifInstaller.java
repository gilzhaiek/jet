package com.reconinstruments.installer.rif;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

import com.reconinstruments.installer.PackageCodes;
import com.reconinstruments.installer.apk.ApkInstaller;
import com.reconinstruments.installer.apk.ApkInstaller.OnPackageInstalled;

/**
 * RifInstaller:
 * 
 * processes RIF files in the installer folder, continuing until
 * either no RIF files remain, or an error occurs and it fails to
 * delete the current RIF file
 * 
 * begins checking process either on startup or when a file changes in
 * the installer folder and the installer isn't already busy
 * rifFolderEvent decodes rifs synchronously so should never be called
 * from the main thread
 */
public class RifInstaller {

    private static final String TAG = "RifInstaller";
    Context context;
    
    boolean processingRif = false;
    // 
    String rifPath;
    String apkPath;

    public RifInstaller(Context context) {
        this.context = context;
    }

    public synchronized void rifFolderEvent() {
        if(!processingRif) {
            processingRif = true;
            checkRifFiles();
        }
    }

    private void checkRifFiles() {
        ArrayList<String> rifFiles = RifUtils.getRifList();
        if(!rifFiles.isEmpty()) {
            rifPath = rifFiles.get(0);
            apkPath = RifUtils.getApkPathForRif(context,rifPath);
            decodeRif();
        } else {
            Log.d(TAG, "No rif files found");
            processingRif = false;
        }
    }

    private void decodeRif() {
        try {
            Log.d(TAG, "decoding rif: " + rifPath);
            RifUtils.decodeRif(rifPath,apkPath,null);
            Log.d(TAG, "Successfully decoded rif file: " + rifPath + " to apk: "+apkPath);
            installApkInBackground();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Error decoding: " + rifPath + ", attempting to clean up");
            processsingComplete();
        }
    }

    private void installApkInBackground() {
        if(!ApkInstaller.installPackage(apkPath, context, new OnPackageInstalled() {
            public void packageInstalled(String packageName, int returnCode) {
                if (returnCode == PackageCodes.INSTALL_SUCCEEDED) {
                    Log.d(TAG, apkPath+": Install succeeded");
                } else {
                    Log.d(TAG, apkPath+": Install failed: " + returnCode);
                }
                processsingComplete();
            }
        })) {
        	// install package failed
            processsingComplete();
        }
    }

    // all code paths from decodeRif on must end with this function
    public void processsingComplete() {
        deleteFileIfExists(apkPath);
        if(deleteFileIfExists(rifPath)) {
            checkRifFiles();
        } else {
            Log.d(TAG, "Failed to cleanup RIF files, stopping RIF file checker");
            processingRif = false;
        }
    }

    // return true if file deleted or doesn't exist
    private boolean deleteFileIfExists(String path) {
        File file = new File(path);
        if(file.exists()) {
            if(file.delete()) {
                Log.d(TAG, "Successfully deleted: " + path);
                return true;
            }
            Log.d(TAG, "Error deleting: " + path);
            return false;
        }
        return true;
    }
}
