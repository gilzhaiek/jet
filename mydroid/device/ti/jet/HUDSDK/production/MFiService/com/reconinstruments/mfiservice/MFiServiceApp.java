package com.reconinstruments.mfiservice;

import android.app.Application;
import android.os.Build;
import android.os.ServiceManager;
import android.util.Log;

import com.reconinstruments.mfi.IMFiService;

public class MFiServiceApp extends Application {
    private static final String TAG = "MFiServiceApp";

    private static final String MFI_SERVICE_NAME = IMFiService.class.getName();
    private MFiServiceImpl mfiServiceImpl;

    public void onCreate() {
        super.onCreate();

        try {
            this.mfiServiceImpl = new MFiServiceImpl(this);
            ServiceManager.addService(MFI_SERVICE_NAME, this.mfiServiceImpl);
            Log.d(TAG, "Registered [" + this.mfiServiceImpl.getClass().getName() + "] as [" + MFI_SERVICE_NAME + "]");
        } catch (Exception e) {
            Log.e(TAG, "Unabled to register " + MFI_SERVICE_NAME);
        }
    }

    public void onTerminate() {
        super.onTerminate();
        Log.d(TAG, "Terminated");
    }
}

