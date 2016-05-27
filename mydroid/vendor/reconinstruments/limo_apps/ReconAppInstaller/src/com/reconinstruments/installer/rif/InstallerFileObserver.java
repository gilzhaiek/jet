/**
 *	Copyright 2013 Recon Instruments
 *	All Rights Reserved.
 *	This class monitor the /EXTERNAL_STORAGE/ReconApps/Installer/ folder for any new created .rif.
 *	If finding any, the ReconAppInstaller activity will be launched for querying for installation
 *
 * 	@author Patrick Cho
 * 
 */

package com.reconinstruments.installer.rif;

import java.io.File;

import android.os.Environment;
import android.os.FileObserver;
import android.util.Log;

import com.reconinstruments.installer.InstallerService;
import com.reconinstruments.utils.FileUtils;

public class InstallerFileObserver extends FileObserver
{
    public static final String TAG = "InstallerFileObserver";
    
    // Path monitored by this file observer
    public static final String INSTALLER_USER_PATH = "/ReconApps/Installer";
    public static final String INSTALLER_USER_FULL_PATH = Environment.getExternalStorageDirectory() + INSTALLER_USER_PATH;

    public static final File INSTALLER_USER_DIR = new File(INSTALLER_USER_FULL_PATH);

    private InstallerService mContext = null;
    private RifInstaller rifInstaller;

    public InstallerFileObserver( InstallerService context )
    {
        super(INSTALLER_USER_FULL_PATH, FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO | FileObserver.DELETE);
        mContext = context;
        rifInstaller = new RifInstaller(mContext);
    }

    @Override
    public void startWatching() {
        
        if(!FileUtils.forceCreateDirectory(INSTALLER_USER_DIR)) {
            Log.e(TAG,"Error creating directory ("+INSTALLER_USER_FULL_PATH+") for user RIF files, not starting file observer");
            return;
        }

        // check rif folder on service start up, but call from new thread
        // since the service start command, and this call, run on the main thread
        new Thread() {
            @Override
            public void run() {
                rifInstaller.rifFolderEvent();
            }
        }.start();

        super.startWatching();
    }

    @Override
    public void stopWatching() {
        super.stopWatching();
    }

    @Override
    public void onEvent(int event, String path)
    {
        Log.d(TAG,"File changed: "+path);
        // check for any RIF files on any event
        rifInstaller.rifFolderEvent();
    }
}