package com.reconinstruments.hudserver;

import android.app.Application;
import android.os.Build;
import android.os.ServiceManager;
import android.util.Log;
import com.reconinstruments.os.hardware.power.IHUDPowerService;
import com.reconinstruments.os.hardware.led.IHUDLedService;
import com.reconinstruments.os.hardware.glance.IHUDGlanceService;
import com.reconinstruments.os.hardware.screen.IHUDScreenService;
import com.reconinstruments.os.hardware.sensors.IHUDHeadingService;
import com.reconinstruments.os.hardware.motion.IHUDActivityMotionService;
import com.reconinstruments.os.connectivity.IHUDConnectivityService;

public class HUDServerApp extends Application {
    private static final String TAG = "HUDServerApp";

    private static final String HUD_POWER_SERVICE_NAME = IHUDPowerService.class.getName();
    private IHUDPowerServiceImpl powerServiceImpl;

    private static final String HUD_LED_SERVICE_NAME = IHUDLedService.class.getName();
    private IHUDLedServiceImpl ledServiceImpl;

    private static final String HUD_GLANCE_SERVICE_NAME = IHUDGlanceService.class.getName();
    private IHUDGlanceServiceImpl glanceServiceImpl;

    private static final String HUD_SCREEN_SERVICE_NAME = IHUDScreenService.class.getName();
    private IHUDScreenServiceImpl screenServiceImpl;

    private static final String HUD_HEADING_SERVICE_NAME = IHUDHeadingService.class.getName();
    private IHUDHeadingServiceImpl headingServiceImpl;

    private static final String HUD_ACTIVITY_MOTION_SERVICE_NAME = IHUDActivityMotionService.class.getName();
    private IHUDActivityMotionServiceImpl activityMotionServiceImpl;

    private static final String HUD_CONNECTIVITY_SERVICE_NAME = IHUDConnectivityService.class.getName();
    private IHUDConnectivityServiceImpl connectivityServiceImpl;

    public void onCreate() {
        super.onCreate();

        // Loading the JNI for ashmem support
        System.load("/system/lib/libreconinstruments_jni.so");

        // Add all Recon Instruments Service implemenations

        // HUD Power Service
        this.powerServiceImpl = new IHUDPowerServiceImpl(this);
        ServiceManager.addService(HUD_POWER_SERVICE_NAME, this.powerServiceImpl);
        Log.d(TAG, "Registered [" + powerServiceImpl.getClass().getName() + "] as [" + HUD_POWER_SERVICE_NAME + "]");

        // HUD Led Service
        this.ledServiceImpl = new IHUDLedServiceImpl(this);
        ServiceManager.addService(HUD_LED_SERVICE_NAME, this.ledServiceImpl);
        Log.d(TAG, "Registered [" + ledServiceImpl.getClass().getName() + "] as [" + HUD_LED_SERVICE_NAME + "]");

        // Heading Service
        this.headingServiceImpl = new IHUDHeadingServiceImpl(this);
        ServiceManager.addService(HUD_HEADING_SERVICE_NAME, this.headingServiceImpl);
        Log.d(TAG, "Registered [" + headingServiceImpl.getClass().getName() + "] as [" + HUD_HEADING_SERVICE_NAME + "]");

        // Activity Motion Service
        this.activityMotionServiceImpl = new IHUDActivityMotionServiceImpl(this);
        ServiceManager.addService(HUD_ACTIVITY_MOTION_SERVICE_NAME, this.activityMotionServiceImpl);
        Log.d(TAG, "Registered [" + activityMotionServiceImpl.getClass().getName() + "] as [" + HUD_ACTIVITY_MOTION_SERVICE_NAME + "]");

        // Enable Glance and Screen Services only on JET
        if (!Build.MODEL.equalsIgnoreCase("Snow2")) {
            // HUD Glance Service
            this.glanceServiceImpl = new IHUDGlanceServiceImpl(this);
            ServiceManager.addService(HUD_GLANCE_SERVICE_NAME, this.glanceServiceImpl);
            Log.d(TAG, "Registered [" + glanceServiceImpl.getClass().getName() + "] as [" + HUD_GLANCE_SERVICE_NAME + "]");

            // HUD Screen Service
            this.screenServiceImpl = new IHUDScreenServiceImpl(this);
            ServiceManager.addService(HUD_SCREEN_SERVICE_NAME, this.screenServiceImpl);
            Log.d(TAG, "Registered [" + screenServiceImpl.getClass().getName() + "] as [" + HUD_SCREEN_SERVICE_NAME + "]");
        }

        // Enable the Connectivity Service
        this.connectivityServiceImpl = new IHUDConnectivityServiceImpl(this);
        ServiceManager.addService(HUD_CONNECTIVITY_SERVICE_NAME, this.connectivityServiceImpl);
        Log.d(TAG, "Registered [" + connectivityServiceImpl.getClass().getName() + "] as [" + HUD_CONNECTIVITY_SERVICE_NAME + "]");
    }

    public void onTerminate() {
        super.onTerminate();
        Log.d(TAG, "Terminated");
    }
}

