/**
 *	Copyright 2013 Recon Instruments.
 *	All Rights Reserved.
 */
package com.reconinstruments.installer;


import com.reconinstruments.installer.firmware.FirmwareUpdateFileObserver;
import com.reconinstruments.installer.rif.InstallerFileObserver;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Basic service holding file observers, 
 * InstallerFileObserver:
 *     checks for .rif files in sdcard/ReconApps/Installer, if they are found install them silently in the background
 * FirmwareUpdateFileObserver:
 *      update.bin files for upgrading firmware
 *
 * 	@author Patrick Cho
 *  @author Chris Tolliday
 *
 */
public class InstallerService extends Service
{
    public static final String TAG = "InstallerService";
    public static final boolean DEBUG = true;

    private InstallerFileObserver mInstallerFileObserver = null;
    private FirmwareUpdateFileObserver mCoreInstallerFileObserver = null;

    private boolean mAlreadyStarted = false;

    @Override
    public IBinder onBind( Intent intent )
    {
        return null;
    }


    @Override
    public void onCreate()
    {
        mInstallerFileObserver = new InstallerFileObserver(this);
        mCoreInstallerFileObserver = new FirmwareUpdateFileObserver(this);
        Log.d(TAG,"Installer service created");
    }

    @Override
    public void onDestroy()
    {
        mInstallerFileObserver.stopWatching();
        mCoreInstallerFileObserver.stopWatching();
        
        Log.d(TAG,"Installer service destroyed");
    }

    @Override
    public int onStartCommand( Intent intent, int flag, int startId )
    {
        Log.d(TAG,"onStartCommand ()");
        if (!mAlreadyStarted){
            mInstallerFileObserver.startWatching();
            mCoreInstallerFileObserver.startWatching();
            mAlreadyStarted = true;
        }
        return Service.START_STICKY;
    }
}
