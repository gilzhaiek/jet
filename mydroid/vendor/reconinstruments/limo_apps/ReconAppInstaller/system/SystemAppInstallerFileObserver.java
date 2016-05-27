/**
 *	Copyright 2013 Recon Instruments
 *	All Rights Reserved.
 *	This class monitor the /EXTERNAL_STORAGE/ReconApps/cache/ folder for any new created update_package.xml
 *	If finding any, the ReconAppInstaller activity will be launched for querying for installation
 *
 *	@author Patrick Cho
 * 
 */

package com.reconinstruments.installer.system;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.reconinstruments.installer.firmware.FirmwareUpdateActivity;
import com.reconinstruments.utils.FileUtils;
import com.reconinstruments.installer.InstallerService;
import com.reconinstruments.installer.InstallerUtil;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.os.FileObserver;
import android.util.Log;

public class SystemAppInstallerFileObserver extends FileObserver
{
	public static final String TAG = "CoreInstallerFileObserver";
	private static final boolean DEBUG = InstallerService.DEBUG;

	private Context mContext = null;
	private InstallCommand mInstallCommand = null;
	private IntentFilter installedFilter = null;
	private IntentFilter decodedFilter = null;
	

	public SystemAppInstallerFileObserver( Context context )
	{
		super(Environment.getExternalStorageDirectory() + InstallerUtil.INSTALLER_SYSTEM_PATH, FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO );
		mContext = context;

		if (DEBUG) Log.d(TAG, Environment.getExternalStorageDirectory() + InstallerUtil.INSTALLER_SYSTEM_PATH);

		decodedFilter = new IntentFilter(SystemDecodeTask.ACTION_DECODED);
		decodedFilter.addAction(SystemDecodeTask.ACTION_FAILED);

        installedFilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        installedFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        installedFilter.addDataScheme("package");
	}

	@Override
	public void onEvent(int event, String path)
	{
		if (DEBUG) Log.d( TAG, "Event : " + event + ", PATH : " + path );

		if (path.equalsIgnoreCase("update_package.xml") && new File(Environment.getExternalStorageDirectory() + InstallerUtil.INSTALLER_SYSTEM_PATH + "/" + path).isFile()){
			Log.d( TAG, "update_package.xml inserted" );
			if ((mInstallCommand = InstallCommand.parseUpdatePackageXML(mContext, null )) != null)
			{
				if (DEBUG) Log.d(TAG, "Command is Parsed, Register BroadcastReceiver and startInstalling");

				mContext.registerReceiver(installationStatusListener, installedFilter);
                mContext.registerReceiver(installationStatusListener, decodedFilter);
				mInstallCommand.startInstall();
			}
			return;
		}
	}

    BroadcastReceiver installationStatusListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (DEBUG) Log.d(TAG, intent.getAction());

            if (intent.getAction().equalsIgnoreCase(SystemDecodeTask.ACTION_DECODED)) {
                if (DEBUG) Log.d(TAG, "RIF is decoded");
                RifInfo rifInfo = (RifInfo) intent.getParcelableExtra(SystemDecodeTask.EXTRA_RIF_INFO);

                File apkfile = rifInfo.getApkFile();
                if (!apkfile.exists()) {
                    if (DEBUG) Log.d(TAG, "Decode APK file does not exist in " + apkfile);
                    return;
                }
                Intent installIntent = new Intent(Intent.ACTION_VIEW);
                installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                installIntent.setDataAndType(Uri.parse("file://" + apkfile.toString()),
                        "application/vnd.android.package-archive");
                mContext.startActivity(installIntent);

            }
            else if (intent.getAction().equalsIgnoreCase(SystemDecodeTask.ACTION_FAILED)) {

                RifInfo rifInfo = (RifInfo) intent.getParcelableExtra(SystemDecodeTask.EXTRA_RIF_INFO);
                int status = intent.getIntExtra(SystemDecodeTask.EXTRA_FAIL_STATUS, -1);

                if (DEBUG) Log.d(TAG, "RIF decoding failed status code : " + status + " , check and remove from installCommand Object and proceed to next");
                if (mInstallCommand != null) {
                    mInstallCommand.notifyInstalled(rifInfo.getPackageName());
                }
            }
            else if (intent.getAction().equalsIgnoreCase(Intent.ACTION_PACKAGE_ADDED)) {
                if (DEBUG) Log.d(TAG, "APK is install, check and remove from installCommand Object and proceed to next");
                if (mInstallCommand != null) {
                    mInstallCommand.notifyInstalled(intent.getData().getEncodedSchemeSpecificPart());
                }
            }
        }
    };
}
