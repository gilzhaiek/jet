/**
 *	Copyright 2013 Recon Instruments
 *	All Rights Reserved.
 *	This class monitor the /EXTERNAL_STORAGE/ReconApps/cache/ folder for any new created update_package.xml
 *	If finding any, the ReconAppInstaller activity will be launched for querying for installation
 *
 *	@author Patrick Cho
 * 
 */

package com.reconinstruments.installer.firmware;

import java.io.File;

import android.content.Intent;
import android.os.Environment;
import android.os.FileObserver;
import android.util.Log;

import com.reconinstruments.installer.InstallerService;
import com.reconinstruments.utils.FileUtils;
import com.reconinstruments.utils.SystemPropUtil;
import com.reconinstruments.utils.installer.FirmwareUtils;

public class FirmwareUpdateFileObserver extends FileObserver
{
	public static final String TAG = "FirmwareUpdateFileObserver";

	private static final int NUMBER_COPY_LIMIT = 3;
	

    public static final String INSTALLER_SYSTEM_PATH = "/ReconApps/cache";
    public static final File INSTALLER_SYSTEM_DIR = new File(Environment.getExternalStorageDirectory()+INSTALLER_SYSTEM_PATH);

	private InstallerService mContext = null;
	private InstallThread mInstallThread;

	public FirmwareUpdateFileObserver( InstallerService context )
	{
		super(Environment.getExternalStorageDirectory() + INSTALLER_SYSTEM_PATH, FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO );
		mContext = context;
	}


    @Override
    public void startWatching() {
        Log.d(TAG, "Checking to see if " + FirmwareUtils.JET_UPDATE_BIN_STORAGE + " exists");
        FileUtils.forceCreateDirectory(FirmwareUtils.JET_RECOVERY_PATH);
        
        File coreInstallerPath = new File(Environment.getExternalStorageDirectory() + INSTALLER_SYSTEM_PATH);
        FileUtils.forceCreateDirectory(coreInstallerPath);
        
        if (FileUtils.isFileAndExists(FirmwareUtils.JET_UPDATE_BIN_STORAGE)) {
            mInstallThread = new InstallThread();
            mInstallThread.start();
        }
        upgradeFirmwareIfNeeded();

        super.startWatching();
    }
    
    @Override
    public void onEvent(int event, String path)
    {
        Log.d( TAG, "Event : " + event + ", PATH : " + path );

        if (path.equals(FirmwareUtils.JET_UPDATE_BIN_STORAGE.getName())) {
            Log.d(TAG,"update.bin inserted");

            if (FirmwareUtils.JET_UPDATE_BIN_STORAGE.isFile() && FirmwareUtils.JET_UPDATE_BIN_STORAGE.exists()) {
                mInstallThread = new InstallThread();
                mInstallThread.start();
            }
        }
    }

	class InstallThread extends Thread {
		@Override
		public void run() {
			int count = 0;
			String srcMd5 = FileUtils.md5(FirmwareUtils.JET_UPDATE_BIN_STORAGE);
			String dstMd5 = "";

			Log.d(TAG, "MD5 of source file is " + srcMd5);

			while (!srcMd5.equalsIgnoreCase(dstMd5) && count < NUMBER_COPY_LIMIT) {
			    FileUtils.copy(FirmwareUtils.JET_UPDATE_BIN_STORAGE, FirmwareUtils.JET_UPDATE_BIN_CACHE);
			    dstMd5 =  FileUtils.md5(FirmwareUtils.JET_UPDATE_BIN_CACHE);
				Log.d(TAG, "Copy finished, MD5 of copied file is " + dstMd5);
				count++;
			}

			if (!srcMd5.equalsIgnoreCase(dstMd5)) {

				Log.e(TAG, "Copying of " + FirmwareUtils.JET_UPDATE_BIN_STORAGE + " to " + FirmwareUtils.JET_UPDATE_BIN_CACHE + " failed. Erasing corrupted cache file ...");

				if (FirmwareUtils.JET_UPDATE_BIN_CACHE.delete()) {
					Log.d(TAG, FirmwareUtils.JET_UPDATE_BIN_CACHE + " deleted");
				} else {
					Log.d(TAG, FirmwareUtils.JET_UPDATE_BIN_CACHE + " was not deleted, try after reboot");
				}
				return;
			}

			Log.d(TAG, "Successfully copied " + FirmwareUtils.JET_UPDATE_BIN_STORAGE + " to " + FirmwareUtils.JET_UPDATE_BIN_CACHE + ". Erasing storage update.bin file");

			if (FirmwareUtils.JET_UPDATE_BIN_STORAGE.delete()) {
				Log.d(TAG, FirmwareUtils.JET_UPDATE_BIN_STORAGE + " deleted");
			} else {
				Log.d(TAG, FirmwareUtils.JET_UPDATE_BIN_STORAGE + " was not deleted, try after reboot");
			}

			if (FirmwareUtils.createUpdateCommand())
				Log.d(TAG, FirmwareUtils.JET_COMMAND_RECOVERY_BAK + " is created ");

			// Log.d(TAG, "Set SYSTEM PROPERTY " + InstallerUtil.SYSTEM_PROP_SHOW_UPDATE + " to 1");
			// System.setProperty(InstallerUtil.SYSTEM_PROP_SHOW_UPDATE, "1");

			startFirmwareUpgradeActivity();
		}
	}
    
    private void upgradeFirmwareIfNeeded() {
        /**
         * Check If /cache/update.bin and /cache/recovery/command file exists, and if does, check /sdcard/ReconApps/cache/update.bin,
         * and if both exists, check if md5 mismatch and if mismatch, erase /cache/update.bin since it isn't copied completely
         * 
         * InstallerUtil.getSystemProp(InstallerUtil.SYSTEM_PROP_SHOW_UPDATE).equals("0")
         * In case ReconApplauncher is crashed, ther services are all restarted, for failsafe purpose, set system property which is only
         * alive while system is up. static variable will still reset in case of app crash 
         */
        if (FileUtils.isFileAndExists(FirmwareUtils.JET_UPDATE_BIN_CACHE) &&
                FileUtils.isFileAndExists(FirmwareUtils.JET_COMMAND_RECOVERY_BAK) &&
                !SystemPropUtil.getSystemProp(FirmwareUtils.SYSTEM_PROP_UPDATE_SHOWN).equals("1")) {


            Log.d(TAG, "JET UPGRADING FIRMWARE : " + FirmwareUtils.JET_UPDATE_BIN_CACHE + " exists, check " + FirmwareUtils.JET_UPDATE_BIN_STORAGE );
            if (FileUtils.isFileAndExists(FirmwareUtils.JET_UPDATE_BIN_STORAGE)) {

                // for Readability assigned 2 String Object
                String storageMD5 = FileUtils.md5(FirmwareUtils.JET_UPDATE_BIN_STORAGE);
                String cacheMD5 = FileUtils.md5(FirmwareUtils.JET_UPDATE_BIN_CACHE);

                Log.d(TAG, "MD5 " + FirmwareUtils.JET_UPDATE_BIN_STORAGE + " : " + storageMD5);
                Log.d(TAG, "MD5 " + FirmwareUtils.JET_UPDATE_BIN_CACHE + " : " + cacheMD5);

                // start Firmware Update Activity md5 matches
                if (storageMD5.equalsIgnoreCase(cacheMD5)) {
                    Log.d(TAG, "MD5 matches, start firmware update activity, if command file exists");
                    startFirmwareUpgradeActivity();
                }
                // Delete /cache/update.bin since it is corrupted
                else {
                    Log.d(TAG, "MD5 does NOT match, delete " + FirmwareUtils.JET_UPDATE_BIN_CACHE);
                    if (FirmwareUtils.JET_UPDATE_BIN_CACHE.delete()) {
                        Log.d(TAG, FirmwareUtils.JET_UPDATE_BIN_CACHE + " deleted");
                    }
                }
            }
            else {
                // start Firmware Update Activity if no sdcard update.bin
                Log.d(TAG, FirmwareUtils.JET_UPDATE_BIN_STORAGE + " does not EXIST, which means " + FirmwareUtils.JET_UPDATE_BIN_CACHE + " was copied correctly");
                Log.d(TAG, "Starting Firmware update activity ... ");
                startFirmwareUpgradeActivity();
            }
        }
    }
    
    public void startFirmwareUpgradeActivity() {
        Intent firmwareUpdateIntent = new Intent(mContext, FirmwareUpdateActivity.class);
        firmwareUpdateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(firmwareUpdateIntent);
    }
}
