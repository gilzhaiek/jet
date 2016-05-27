//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.dashwarning;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.limopm.LimoPMNative;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import com.reconinstruments.utils.UIUtils;
import com.reconinstruments.utils.DeviceUtils;
import com.reconinstruments.utils.SettingsUtil;
import com.reconinstruments.commonwidgets.FeedbackDialog;
import android.support.v4.app.FragmentActivity;

public class WarningActivity extends FragmentActivity {
    public static final String TAG = "WarningActivity";
    private static final File SYSTEM_SENSORS_CONF = new File("/system/lib/hw/sensors.conf");
    private static final File DATA_SENSORS_CONF = new File("/data/system/sensors.conf");
    private static final float INTEGRITY_RATIO  = 0.9f;

    private static final long SERVICE_PARTS_DELAY = 1000; // 1 Second
    private static final long USB_PLUGGED_OUT_COUNTDOWN = 20000; // 20 seconds
    private static final long SCREEN_OFF_TIME = 60000; // 1 min
    private static final long POWER_OFF_TIME = 540000; // 9 min
    // private static final long SCREEN_OFF_TIME = 10000; // 10 sec
    // private static final long POWER_OFF_TIME = 30000; // 30 sec

    public static final int POWER_MODE_NORMAL = 1;
    public static final int POWER_MODE_DISPLAY_OFF = 2;

    private static final String ADB_ENABLE_FILE = "ReconApps/Debug/PleaseEnableADB";

    Handler timerHandler = new Handler();
    private static final int EVENT_USB_IN               = 0;
    private static final int EVENT_USB_OUT              = 1;
    private static final int EVENT_POWER_BUTTON         = 2;
    private static final int EVENT_ENTER_BUTTON         = 3;
    private static final int EVENT_OTHER_BUTTON         = 4;
    private static final int EVENT_ON_TIMER_EXP         = 5;
    private static final int EVENT_OFF_TIMER_EXP        = 6;
    private static final int EVENT_PAUSED               = 7;
    private static final int EVENT_RESUMED              = 8;

    private static final int INIT_MODE                  = 0;
    private static final int CHARGE_MODE_USB_IN         = 1;
    private static final int CHARGE_MODE_USB_OUT        = 2; // TIMER
    private static final int WARNING_SCREEN_ON          = 4; // TIMER
    private static final int WARNING_SCREEN_OFF         = 5; // TIMER
    private static final int WARNING_PAUSED             = 6;
    private static final int ANDROID_BOOT               = 7;
    private static final int WARNING_PENDING_SHUTDOWN   = 8;
    private static final int SHUTDOWN                   = 9;

    private static boolean mBTServiceStarted            = false;
    private static boolean mUSBPlugged                  = true; // We assume the default is on but will be over-writte in onCreate
    private static int mCurrentState = INIT_MODE;
    private static boolean mWifiEnabled = false;

    // Event                                    | STATE TRANSITION
    // sys.boot_to_android==0 && mUSBPlugged    | INIT_MODE -> CHARGE_MODE_USB_IN
    // sys.boot_to_android==0 && !mUSBPlugged   | INIT_MODE -> CHARGE_MODE_USB_OUT
    // sys.boot_to_android==1                   | INIT_MODE -> WARNING_SCREEN_ON
    // EVENT_POWER_BUTTON                       | CHARGE_MODE_USBx -> WARNING_SCREEN_ON
    // EVENT_USB_OUT                            | CHARGE_MODE_USB_IN -> CHARGE_MODE_USB_OUT
    // EVENT_USB_IN                             | CHARGE_MODE_USB_OUT -> CHARGE_MODE_USB_IN
    // EVENT_OFF_TIMER_EXP                      | CHARGE_MODE_USB_OUT -> SHUTDOWN
    // EVENT_ON_TIMER_EXP                       | WARNING_SCREEN_ON -> WARNING_SCREEN_OFF
    // EVENT_PAUSED                             | WARNING_SCREEN_ON -> WARNING_PAUSED
    // EVENT_RESUMED                            | WARNING_PAUSED -> WARNING_SCREEN_ON
    // EVENT_POWER/ENTER_BUTTON                 | WARNING_SCREEN_ON -> ANDROID_BOOT
    // EVENT_POWER/ENTER/REMOTE_BUTTON          | WARNING_SCREEN_OFF | WARNING_PENDING_SHUTDOWN -> WARNING_SCREEN_ON
    // EVENT_OFF_TIMER_EXP && !mUSBPlugged      | WARNING_SCREEN_OFF -> SHUTDOWN
    // EVENT_OFF_TIMER_EXP && mUSBPlugged       | WARNING_SCREEN_OFF -> WARNING_PENDING_SHUTDOWN
    // EVENT_USB_OUT                            | WARNING_PENDING_SHUTDOWN -> SHUTDOWN
    public void setState(int state) {
        mCurrentState = state;
   }
 
    public void setEvent(int currentEvent) {
        Log.v(TAG,"setEvent");

        if (mCurrentState == ANDROID_BOOT) {
            // We don't really care what the event is. We need to launch the launcher:
            launchTheMainComponent();
        }
        // Power Button pressed while charge mode, we switch to Warning screen
        if(((mCurrentState == CHARGE_MODE_USB_IN) || (mCurrentState == CHARGE_MODE_USB_OUT))
           && (currentEvent == EVENT_POWER_BUTTON)) {
            Log.v(TAG,"EVENT_POWER_BUTTON: CHARGE_MODE_USBx -> WARNING_SCREEN_ON");

            if(mWifiEnabled == true){
                WifiManager wifi = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
                Log.d(TAG, "Enable wifi");
                wifi.setWifiEnabled(true);
                mWifiEnabled = false;
            }

            doAllServices();
            startScreenOffTimer();
            if (isLimo()) { LimoPMNative.SetPowerMode(POWER_MODE_NORMAL); }

            setState(WARNING_SCREEN_ON);
            return;
        }

        // USB Unplugged in Chager mode, we are going to start a timer of 20 seconds
        if((mCurrentState == CHARGE_MODE_USB_IN) && (currentEvent == EVENT_USB_OUT)) {
            Log.v(TAG,"EVENT_USB_OUT: CHARGE_MODE_USB_IN -> CHARGE_MODE_USB_OUT");

            timerHandler.postDelayed(powerOffRunnable, USB_PLUGGED_OUT_COUNTDOWN);

            setState(CHARGE_MODE_USB_OUT);
            return;
        }

        // USB Plugged - we going to remove all the timers
        if((mCurrentState == CHARGE_MODE_USB_OUT) && (currentEvent == EVENT_USB_IN)) {
            Log.v(TAG,"EVENT_USB_IN: CHARGE_MODE_USB_OUT -> CHARGE_MODE_USB_IN");

            timerHandler.removeCallbacks(powerOffRunnable);

            setState(CHARGE_MODE_USB_IN);
            return;
        }

        // 20 Seconds timer expired while USB is out
        if((mCurrentState == CHARGE_MODE_USB_OUT) && (currentEvent == EVENT_OFF_TIMER_EXP)) {
            Log.v(TAG,"EVENT_OFF_TIMER_EXP: CHARGE_MODE_USB_OUT -> SHUTDOWN");

            startShutdown();

            setState(SHUTDOWN);
            return;
        }

        // If we on, and we paused - we need to disable the screen timeout
        if((mCurrentState == WARNING_SCREEN_ON) && (currentEvent == EVENT_PAUSED)) {
            Log.v(TAG,"EVENT_PAUSED: WARNING_SCREEN_ON -> WARNING_PAUSED");

            removeRunnables();

            setState(WARNING_PAUSED);
            return;
        }

        // If we paused and then resumed - we need to enable the screen timeout
        if((mCurrentState == WARNING_PAUSED) && (currentEvent == EVENT_RESUMED)) {
            Log.v(TAG,"EVENT_RESUMED:  WARNING_PAUSED -> WARNING_SCREEN_ON");

            startScreenOffTimer();

            setState(WARNING_SCREEN_ON);
            return;
        }

        // After timer on exipred - we are turning off the screen
        if((mCurrentState == WARNING_SCREEN_ON) && (currentEvent == EVENT_ON_TIMER_EXP)) {
            Log.v(TAG,"EVENT_ON_TIMER_EXP: WARNING_SCREEN_ON -> WARNING_SCREEN_OFF");

            if (isLimo()) {
                LimoPMNative.SetPowerMode(POWER_MODE_DISPLAY_OFF);
            } else {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                pm.goToSleep(System.currentTimeMillis());
            }
            timerHandler.postDelayed(powerOffRunnable, POWER_OFF_TIME);

            registerReceiver(turnOnScreenReceiver, new IntentFilter("turn_on_screen"));

            setState(WARNING_SCREEN_OFF);
            return;
        }

        // During screen on - enter or power button will cause an android boot
        if((mCurrentState == WARNING_SCREEN_ON) && ((currentEvent == EVENT_POWER_BUTTON)
                                                    || (currentEvent == EVENT_ENTER_BUTTON))) {
            Log.v(TAG,"EVENT_POWER/REMOTE_BUTTON: WARNING_SCREEN_ON -> ANDROID_BOOT");
            launchTheMainComponent();

            setState(ANDROID_BOOT);
            return;
        }

        // During screen off - any button will cause an during screen on
        if(((mCurrentState == WARNING_SCREEN_OFF) || (mCurrentState == WARNING_PENDING_SHUTDOWN)) &&
           ((currentEvent == EVENT_POWER_BUTTON)  || (currentEvent == EVENT_ENTER_BUTTON) || (currentEvent == EVENT_OTHER_BUTTON))) {
            Log.v(TAG,"EVENT_POWER/REMOTE_BUTTON: WARNING_SCREEN_OFF | WARNING_PENDING_SHUTDOWN -> WARNING_SCREEN_ON");

            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            pm.userActivity(System.currentTimeMillis(), true);
            startScreenOffTimer();
            if (isLimo()) { LimoPMNative.SetPowerMode(POWER_MODE_NORMAL); }
            try {
                unregisterReceiver(turnOnScreenReceiver);
            } catch (Exception e) {
                Log.w(TAG,"turnOnScreenReceiver couldn't beregistered");
            }

            setState(WARNING_SCREEN_ON);
            return;
        }

        // During screen off - if the timer expired and USB is not connected - we shutdown
        if((mCurrentState == WARNING_SCREEN_OFF) && (currentEvent == EVENT_OFF_TIMER_EXP) && !mUSBPlugged) {
            Log.v(TAG,"EVENT_OFF_TIMER_EXP: WARNING_SCREEN_OFF (USB is not plugged) -> SHUTDOWN");

            startShutdown();

            setState(SHUTDOWN);
            return;
        }

        // During screen off - if the timer expired and USB is connceted - we will pend a shutdown, unless the USB will get disconnected or the power button is pressed
        if((mCurrentState == WARNING_SCREEN_OFF) && (currentEvent == EVENT_OFF_TIMER_EXP) && mUSBPlugged) {
            Log.v(TAG,"EVENT_OFF_TIMER_EXP: WARNING_SCREEN_OFF (USB is plugged) -> WARNING_PENDING_SHUTDOWN");

            setState(WARNING_PENDING_SHUTDOWN);
            return;
        }

        // USB Unplugged in while pending shutdown
        if((mCurrentState == WARNING_PENDING_SHUTDOWN) && (currentEvent == EVENT_USB_OUT)) {
            Log.v(TAG,"EVENT_USB_OUT: WARNING_PENDING_SHUTDOWN -> SHUTDOWN");

            startShutdown();

            setState(SHUTDOWN);
            return;
        }
    }

    private void startShutdown()        {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        pm.reboot("BegForShutdown"); // Magic reason that
    }

    private boolean isLimo() { return DeviceUtils.isLimo(); }

    private void copySensorsConf() {
        Log.v(TAG,"copySensorsConf");
 
        // If /data/system/sensors.conf corrupted, copy it from default
        if (!checkSensorsConfIntegrity()) {
            Log.d(TAG, "sensors.conf corrupted, copy from the default");
            copy(SYSTEM_SENSORS_CONF, DATA_SENSORS_CONF);
            // Recalibration Needed
            Settings.Secure.putInt(getContentResolver(), "hasWrittenMagOffsetsV2", 0);
        }
    }
        
    /**
     * checks if copying of the sensors conf needed
     * 
     * @return isFileOkay
     */
    private boolean checkSensorsConfIntegrity() {
        Log.v(TAG,"checkSensorsConfIntegrity");
        if (isLimo())
            return true;
        if(!DATA_SENSORS_CONF.exists()) {
            Log.w(TAG,"checkSensorsConfIntegrity: generated sensors.conf doesn't exists");
            return false;
        }
                
        if((DATA_SENSORS_CONF.length() < (SYSTEM_SENSORS_CONF.length() * INTEGRITY_RATIO))) {
            Log.w(TAG,"checkSensorsConfIntegrity: generated sensors.conf size=" + DATA_SENSORS_CONF.length() + " is too small");
            return false;
        }
                
        if(!checkSensorsConfEndWithExtra()) {
            Log.w(TAG,"checkSensorsConfIntegrity: generated sensors.conf is missing #Extra");
            return false;
        }
                
        return true;
    }
        
    /**
     * Checks if /data/system/sensors.conf has #Extra line  
     */
    private boolean checkSensorsConfEndWithExtra() { 
                
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(DATA_SENSORS_CONF));
            String line = null;
            
            while ((line = reader.readLine()) != null) {
                if (line.contains("#Extra")) {
                    return true;
                }
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException Reading sensors.conf");
        } catch (IOException e) {
            Log.e(TAG, "IOException Reading sonsors.conf");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e2) {
                    Log.e(TAG, "IOException closing BufferedReader");
                }
            }
        }
        
        return false;
    }

    public void checkAndPossiblyEnableADB() {
        //Check for magic file to enable adb
        File enableAdb = new File(Environment.getExternalStorageDirectory(), ADB_ENABLE_FILE);
        if (enableAdb.exists()) {
            Log.v(TAG,"Enabling ADB");
            Settings.Secure.putInt(getBaseContext().getContentResolver(), Settings.Secure.ADB_ENABLED, 1);
        }
        
    }
        
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG,"onCreate");
        super.onCreate(savedInstanceState);

        copySensorsConf();

        // Enable sound effects. Typically This should be done by
        // def_sound_effects_enabled in
        // /jet/mydroid/frameworks/base/packages/SettingsProvider/res/values/defaults.xml
        // or the overlay but because we don't want sound on Snow2 we put it here
        if (DeviceUtils.isSun()) {
            Settings.System.putInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 1);
        }
        Log.v(TAG,"sounds enabled");

        // sys.boot_to_android==0 && USB_IN     | INIT_MODE -> CHARGE_MODE_USB_IN
        // sys.boot_to_android==0 && USB_OUT    | INIT_MODE -> CHARGE_MODE_USB_OUT
        // sys.boot_to_android==1               | INIT_MODE -> WARNING_SCREEN_ON

        Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

        Intent usbState = registerReceiver(null, new IntentFilter("android.hardware.usb.action.USB_STATE"));
        boolean connected = usbState.getExtras().getBoolean("connected");

        Log.d(TAG, "status: " + status + " connected:" + connected);
        if(status != BatteryManager.BATTERY_STATUS_CHARGING && !connected) {
            mUSBPlugged = false;
        } else {
            mUSBPlugged = true;
        }

        // The below log occasionally makes limo freez:
        // apparently getting RuntimeProp is sometimes too taxing on limo.
        //Log.d(TAG, "sys.boot_to_android : " + getRuntimeProp("sys.boot_to_android"));
        if (!isLimo() && getRuntimeProp("sys.boot_to_android").trim().equals("0") ){
            Log.d(TAG, "INIT_MODE -> CHARGE_MODE_USB_IN");

            IntentFilter filter = new IntentFilter(Intent.ACTION_POWER_CONNECTED);
            filter.addAction(Intent.ACTION_POWER_DISCONNECTED);

            setState(CHARGE_MODE_USB_IN);

            if(!mUSBPlugged) {
                setEvent(EVENT_USB_OUT);
            } 

            WifiManager wifi = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
            if (wifi.isWifiEnabled()){
                wifi.setWifiEnabled(false);
                Log.d(TAG, "Disable wifi for PC charging");
                mWifiEnabled=true; //We will turn on Wifi after user pressing button to fully boot the device
            }
        }
        else {
            Log.d(TAG, "INIT_MODE -> WARNING_SCREEN_ON");

            setState(WARNING_SCREEN_ON);
            doAllServices();
            startScreenOffTimer();
        }

        IntentFilter filter = new IntentFilter(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(USBReceiver, filter);
        filter = new IntentFilter("RECON_BOOT_START");
        filter.addAction("RECON_BOOT_END");
        registerReceiver(unpackMapsReceiver, filter);
    }

    public void onStart() {
        Log.v(TAG,"onStart");
        super.onStart();
        checkAndPossiblyEnableADB();
        if (mUSBPlugged) {            // USB is plugged:
            Log.v(TAG,"USB connected");
            startService(new Intent("com.reconinstruments.lispxml.LispXmlService"));
        } else {
            Log.v(TAG,"USB not connected");
        }
    }

    public void onResume() {
        super.onResume();
        Log.v(TAG,"onResume");
        UIUtils.setButtonHoldShouldLaunchApp(this,false);
        setEvent(EVENT_RESUMED);
        if (SettingsUtil
            .getCachableSystemIntOrSet(this,
                                       SettingsUtil.PASSCODE_ENABLED,
                                       0) == 1) { // Passcode enabled
            if (System.getProperty("DashWarningLockedOnce") == null) {
                System.setProperty("DashWarningLockedOnce","yes");
                startActivity(new Intent("com.reconinstruments.passcodelock.PASSCODE_MAIN"));
            }
        }
    }

    public void onPause() {
        super.onPause();
        Log.v(TAG,"onPause");
        UIUtils.setButtonHoldShouldLaunchApp(this,true);
        setEvent(EVENT_PAUSED);
    }

    // Write function that starts all the services that were in
    // onCreate onSTart and onResume didn't call onCreate() onStart()
    // directly since I'm not sure what super.oncreate() .. call would
    // cause if called again
    private void doAllServices () {
        Log.v(TAG,"doAllServices");
        // Normalize LED light:
        Intent ledIntent = new Intent("com.reconinstruments.hud.led.SET_NORMAL");
        sendBroadcast(ledIntent);

        if(DeviceUtils.isSnow2()){
                this.setContentView(R.layout.snow2_warning_layout);
        }
        else {
                this.setContentView(R.layout.warning_layout);
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        // Ali: We run all the services from here. Later on
        // we can put a better logic so that we don't blindly
        // start all of them:
        startService(new Intent("RECON_INSTALLER_SERVICE"));
        startService(new Intent("RECON_MOD_SERVICE"));
        startService(new Intent("RECON_BLE_TEST_SERVICE"));
        startService(new Intent("RECON_PHONE_RELAY_SERVICE"));
        startService(new Intent("com.reconinstruments.geodataservice.start"));
        startService(new Intent("com.reconinstruments.jetapplauncher.settings.service.GlanceAppService"));
        //performVideoPlaybackLogic(); // Disabled until new video is put it
        timerHandler.postDelayed(startBTServicesRunnable, SERVICE_PARTS_DELAY);
    }

    private void startScreenOffTimer() {
        Log.v(TAG,"startScreenOffTimer");
        removeRunnables();
        timerHandler.postDelayed(screenOffRunnable, SCREEN_OFF_TIME);
    }

    public boolean onTouchEvent(MotionEvent event) {
        Log.v(TAG,"onTouchEvent");
        setEvent(EVENT_OTHER_BUTTON);
        return true;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.v(TAG, "onKeyUp with keyCode"+keyCode);

        if(keyCode == KeyEvent.KEYCODE_POWER ||
           keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
           keyCode == KeyEvent.KEYCODE_ENTER ||
           keyCode == KeyEvent.KEYCODE_BUTTON_1) {
            // Keycode button 1 is HACK should be removed by Li
            Log.v(TAG,"Go power");
            setEvent(EVENT_POWER_BUTTON);
            return true;
        }

        setEvent(EVENT_OTHER_BUTTON);
        return true;
    }

    public void onBackPressed() {
        return;
    }

    private void removeRunnables() {
        timerHandler.removeCallbacks(screenOffRunnable);
        timerHandler.removeCallbacks(powerOffRunnable);
    }

    Runnable screenOffRunnable = new Runnable() {
            public void run() {setEvent(EVENT_ON_TIMER_EXP);}
        };

    Runnable powerOffRunnable = new Runnable() {
            public void run() { setEvent(EVENT_OFF_TIMER_EXP);}
        };

    Runnable startBTServicesRunnable = new Runnable() {
            public void run() {
                // FIXME: Start the ble service on jet We start this on resume
                // as opposed to create so that in case the_ble_service barfs
                // out windows it doesn't intefere with gps initialization as
                // gps initializatino happens on create.  Once Gil fixes his
                // bug this line should come
                if (!mBTServiceStarted) {
                    Log.v(TAG,"Starting other BT Services");
                    startService(new Intent("RECON_THE_BLE_SERVICE"));
                    startService(new Intent("SS1_HFP_SERVICE"));
                    startService(new Intent("SS1_MAP_SERVICE"));
                    startService(new Intent ("RECON_HUD_SERVICE"));
                    mBTServiceStarted = true;
                    
                }
            
            }
        };
    private static boolean SIMULATE_JET = false;
    private void launchTheMainComponent() {
        try {
            unregisterReceiver(USBReceiver);
            unregisterReceiver(unpackMapsReceiver);
        } catch (Exception e) {
            Log.w(TAG,"couldn't unregister receivers: " + e);
        }
        removeRunnables();
        Intent intent = new Intent("com.reconinstruments.itemhost.ItemHostActivity");
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        intent = new Intent("com.reconinstruments.showIfBadShutdown");
        sendBroadcast(intent);
    }

    /**
     *
     * Returns the value of the system property
     *
     * @param <b>String name</b> name of the property
     * @return <b>String value</b> value of the property, if not defined, 1 is returned for now
     */
    private String getRuntimeProp(String name) {
        InputStream inputstream = null;
        try {
            inputstream = Runtime.getRuntime().exec(new String[]{"/system/bin/getprop",name}).getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String propval = "";
        try {
            propval = new Scanner(inputstream).useDelimiter("\\A").next();
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "SystemProperty - " + name + " : " + propval);
        return propval;
    }

    /**
     *
     * Sets the system property. Note that DashWarning is system user, so there's only a certain type of
     * system property it can set. See system/core/init/property_service.c.
     *
     * @param key - name of the property
     * @param val - value of the property to set
     */
    private void setRuntimeProp(String key, String val) {
        try {
            Runtime.getRuntime().exec(new String[]{"/system/bin/setprop", key, val});
            Log.d(TAG, "Set system property: " + key + " : " + val);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Simple Method for copying file
     * @param src
     * @param dst
     * @throws IOException
     */
    public boolean copy(File src, File dst) {
        InputStream in;
        OutputStream out;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
                        
            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getLocalizedMessage());
            return false;
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
            return false;
        }
        Log.d(TAG, "Copied " + src.getAbsolutePath() + " to " + dst.getAbsolutePath());
        return true;
    }

    BroadcastReceiver USBReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)) {
                    mUSBPlugged = false;
                    setEvent(EVENT_USB_OUT);
                } else if (intent.getAction().equalsIgnoreCase(Intent.ACTION_POWER_CONNECTED)) {
                    mUSBPlugged = true;
                    setEvent(EVENT_USB_IN);
                }
            }
        };

    BroadcastReceiver turnOnScreenReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                setEvent(EVENT_OTHER_BUTTON);
            }
        };

    BroadcastReceiver unpackMapsReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("RECON_BOOT_START")) {
                Log.d(TAG, "Received intent indicating unpacking maps.");
                setEvent(EVENT_PAUSED);
                FeedbackDialog.showDialog(WarningActivity.this, "Finishing Update", null, null, FeedbackDialog.SHOW_SPINNER);
            } else if (intent.getAction().equals("RECON_BOOT_END")) {
                Log.d(TAG, "Received intent indicating unpacking maps finished.");
                setEvent(EVENT_RESUMED);
                FeedbackDialog.dismissDialog(WarningActivity.this);
            }
        }
    };
}

