package com.reconinstruments.hudconnectivityservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

public class HUDConnectivityService extends Service {
    private final String TAG = this.getClass().getSimpleName();

    private static final boolean DEBUG = true;

    private IHUDConnectivityServiceImpl mService; // <2>

    @Override
    public void onCreate() {
        super.onCreate();
        System.load("/system/lib/libreconinstruments_jni.so");
        try {
            Log.d(TAG, "Creating IHUDConnectivityServiceImpl");
            mService = new IHUDConnectivityServiceImpl(this);
            if (DEBUG) Log.d(TAG, "onCreate()'ed");
        } catch (Exception e) {
            Log.e(TAG, "onCreate() failed", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (DEBUG) Log.d(TAG, "onBind()'ed");
        return this.mService;
    }


    @Override
    public boolean onUnbind(Intent intent) {
        if (DEBUG) Log.d(TAG, "onUnbind()'ed");
        return super.onUnbind(intent);
    }


    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy()'ed");
        try {
            this.mService.stop();
        } catch (IOException e) {
            Log.e(TAG, "Failed to stop the service", e);
        }
        this.mService = null;
        super.onDestroy();
    }
}
