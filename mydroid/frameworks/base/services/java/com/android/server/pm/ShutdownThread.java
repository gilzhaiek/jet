/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 
package com.android.server.pm;

import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.IActivityManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.IBluetooth;
import android.nfc.NfcAdapter;
import android.nfc.INfcAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Vibrator;
import android.os.SystemVibrator;
import android.os.storage.IMountService;
import android.os.storage.IMountShutdownObserver;

import com.android.internal.policy.PolicyManager;
import com.android.internal.telephony.ITelephony;
import com.android.server.PowerManagerService;

import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import dalvik.system.DexClassLoader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

// ANALYTICS
import com.reconinstruments.os.analyticsagent.IAnalyticsService;
import com.reconinstruments.os.analyticsagent.IAnalyticsServiceShutdownObserver;


public final class ShutdownThread extends Thread {
    // constants
    private static final String TAG = "ShutdownThread";
    private static final int PHONE_STATE_POLL_SLEEP_MSEC = 500;
    // maximum time we wait for the shutdown broadcast before going on.
    private static final int MAX_BROADCAST_TIME = 10*1000;
    private static final int MAX_SHUTDOWN_WAIT_TIME = 20*1000;
    private static final int MAX_RADIO_WAIT_TIME = 12*1000;

    // length of vibration before shutting down
    private static final int SHUTDOWN_VIBRATE_MS = 0;
    
    // state tracking
    private static Object sIsStartedGuard = new Object();
    private static boolean sIsStarted = false;
    
    private static boolean mReboot;
    private static boolean mRebootSafeMode;
    private static String mRebootReason;
    private static String mLastShutdownReason = "NULL";

    // Provides shutdown assurance in case the system_server is killed
    public static final String SHUTDOWN_ACTION_PROPERTY = "sys.shutdown.requested";

    // Provides shutdown reason across reboots
    public static final String SHUTDOWN_REASON_PROPERTY = "persist.sys.shutdown.reason";

    // Indicates whether we are rebooting into safe mode
    public static final String REBOOT_SAFEMODE_PROPERTY = "persist.sys.safemode";

    // static instance of this thread
    private static final ShutdownThread sInstance = new ShutdownThread();
    
    private final Object mActionDoneSync = new Object();
    private boolean mActionDone;
    private Context mContext;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mCpuWakeLock;
    private PowerManager.WakeLock mScreenWakeLock;
    private Handler mHandler;
    
    private ShutdownThread() {
    }

    public static String getLastShutdownReason() {
        if(mLastShutdownReason.equals("NULL")) {
            mLastShutdownReason = SystemProperties.get(SHUTDOWN_REASON_PROPERTY,"");
            SystemProperties.set(SHUTDOWN_REASON_PROPERTY, "BOOT");
        }
        return mLastShutdownReason;
    }
 
    /**
     * Request a clean shutdown, waiting for subsystems to clean up their
     * state etc.  Must be called from a Looper thread in which its UI
     * is shown.
     *
     * @param context Context used to display the shutdown progress dialog.
     * @param confirm true if user confirmation is needed before shutting down.
     */
    public static void shutdown(final Context context, boolean confirm) {
        mReboot = false;
        mRebootSafeMode = false;
        shutdownInner(context, confirm);
    }

    static void shutdownInner(final Context context, boolean confirm) {
        // ensure that only one thread is trying to power down.
        // any additional calls are just returned
        synchronized (sIsStartedGuard) {
            if (sIsStarted) {
                Log.d(TAG, "Request to shutdown already running, returning.");
                return;
            }
        }

        final int longPressBehavior = context.getResources().getInteger(
                        com.android.internal.R.integer.config_longPressOnPowerBehavior);
        final int resourceId = mRebootSafeMode
                ? com.android.internal.R.string.reboot_safemode_confirm
                : (longPressBehavior == 2
                        ? com.android.internal.R.string.shutdown_confirm_question
                        : com.android.internal.R.string.shutdown_confirm);

        Log.d(TAG, "Notifying thread to start shutdown longPressBehavior=" + longPressBehavior);

        if (confirm) {
            final CloseDialogReceiver closer = new CloseDialogReceiver(context);
            final AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle(mRebootSafeMode
                            ? com.android.internal.R.string.reboot_safemode_title
                            : com.android.internal.R.string.power_off)
                    .setMessage(resourceId)
                    .setPositiveButton(com.android.internal.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            beginShutdownSequence(context);
                        }
                    })
                    .setNegativeButton(com.android.internal.R.string.no, null)
                    .create();
            closer.dialog = dialog;
            dialog.setOnDismissListener(closer);
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
            dialog.show();
        } else {
            beginShutdownSequence(context);
        }
    }

    private static void setFreqScalingGovernor(final Context context) {
        DexClassLoader hudClassLoader;
        Class hudPowerManagerClass = null;
        String hudJarLocation = "/system/framework/com.reconinstruments.os.jar";
        hudClassLoader = new DexClassLoader(hudJarLocation,
            new ContextWrapper(context).getCacheDir().getAbsolutePath(),
            null, ClassLoader.getSystemClassLoader());

        try {
            // Load the HUDPowerManager class
            hudPowerManagerClass = hudClassLoader.loadClass("com.reconinstruments.os.hardware.power.HUDPowerManager");

            // Retrieve HUDPowerManager.getInstance() static method
            Method m = hudPowerManagerClass.getDeclaredMethod("getInstance");

            // Execute HUDPowerManager.getInstance()
            Object o = m.invoke(null);

            // Set the frequency scaling governor to powersave
            Field f = hudPowerManagerClass.getField("FREQ_SCALING_GOV_POWERSAVE");
            int governor = f.getInt(o);
            m = hudPowerManagerClass.getMethod("setFreqScalingGovernor", new Class[] {int.class});
            m.invoke(o, governor);

        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Failed to find HUD classes");
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Illegal access to HUDPowerManager");
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "No such method in HUDPowerManager");
        } catch (Exception e) {
            Log.e(TAG, "Exception executing HUDPowerManager");
        }
    }

    private static void shutdownBlinkLED(final Context context) {
        // Create class loader to load HUDLedManager class
        DexClassLoader hudClassLoader;
        Class hudLedManagerClass = null;
        String hudJarLocation = "/system/framework/com.reconinstruments.os.jar";
        hudClassLoader = new DexClassLoader(hudJarLocation,
            new ContextWrapper(context).getCacheDir().getAbsolutePath(),
            null, ClassLoader.getSystemClassLoader());

        try {
            // Load the HUDLedManager class
            hudLedManagerClass = hudClassLoader.loadClass("com.reconinstruments.os.hardware.led.HUDLedManager");

            // Retrieve HUDLedManager.getInstance() static method
            Method m = hudLedManagerClass.getDeclaredMethod("getInstance");

            // Execute HUDLedManager.getInstance()
            Object o = m.invoke(null);

            // Retrieve the default brightness value
            Field f = hudLedManagerClass.getField("BRIGHTNESS_NORMAL");
            int brightness = f.getInt(o);

            // Set the LED brightness to 0 to turn it off
            m = hudLedManagerClass.getMethod("setPowerLEDBrightness", new Class[] {int.class});
            m.invoke(o, 0);

            // Blink the LED 2 times
            m = hudLedManagerClass.getMethod("blinkPowerLED", new Class[] {int.class, int[].class});
            int[] pattern = new int[] {0, 200, 200, 200, 200, 200};
            m.invoke(o, brightness, pattern);

        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Failed to find HUD classes");
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Illegal access to HUDLedManager");
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "No such method in HUDLedManager");
        } catch (Exception e) {
            Log.e(TAG, "Exception executing HUDLedManager");
        }
    }

    private static class CloseDialogReceiver extends BroadcastReceiver
            implements DialogInterface.OnDismissListener {
        private Context mContext;
        public Dialog dialog;

        CloseDialogReceiver(Context context) {
            mContext = context;
            IntentFilter filter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.registerReceiver(this, filter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            dialog.cancel();
        }

        public void onDismiss(DialogInterface unused) {
            mContext.unregisterReceiver(this);
        }
    }

    /**
     * Request a clean shutdown, waiting for subsystems to clean up their
     * state etc.  Must be called from a Looper thread in which its UI
     * is shown.
     *
     * @param context Context used to display the shutdown progress dialog.
     * @param reason code to pass to the kernel (e.g. "recovery"), or null.
     * @param confirm true if user confirmation is needed before shutting down.
     */
    public static void reboot(final Context context, String reason, boolean confirm) {
        if(reason == null) {
            reason = "";
        }

        //HACK JET-121:
        //We introduce a magic reason that will cause a shutdown
        if (reason.equals("BegForShutdown")){
            mReboot = false;
            mRebootReason = null;
            mRebootSafeMode = false;
        } else {
            mReboot = true;
            mRebootSafeMode = false;
            mRebootReason = reason;
        }
        shutdownInner(context, confirm);
    }

    /**
     * Request a reboot into safe mode.  Must be called from a Looper thread in which its UI
     * is shown.
     *
     * @param context Context used to display the shutdown progress dialog.
     * @param confirm true if user confirmation is needed before shutting down.
     */
    public static void rebootSafeMode(final Context context, boolean confirm) {
        mReboot = true;
        mRebootSafeMode = true;
        mRebootReason = null;
        shutdownInner(context, confirm);
    }

    // Helper listener class to get callback when shutdown
    private static class ShutdownGlobalLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {
        Context mContext;
        boolean blinked;

        public ShutdownGlobalLayoutListener(Context context) {
            mContext = context;
            blinked = false;
        }

        @Override
        public void onGlobalLayout() {
            // onGlobalLayout() may be called multiple times. Make sure we blink only once.
            if (!blinked) {
                // Create and trigger thread to blink LED
                ShutdownBlinkLEDThread r = new ShutdownBlinkLEDThread(mContext);
                r.start();
                blinked = true;
            }
        }
    }

    // Helper class to make blinking LED asynchronous
    private static class ShutdownBlinkLEDThread extends Thread {
        Context mContext;

        public ShutdownBlinkLEDThread(Context c) {
            mContext = c;
        }

        @Override
        public void run() {
            // Blink the LED to indicate shutdown
            shutdownBlinkLED(mContext);
        }
    }

    private static void beginShutdownSequence(Context context) {
        synchronized (sIsStartedGuard) {
            if (sIsStarted) {
                Log.d(TAG, "Shutdown sequence already running, returning.");
                return;
            }
            sIsStarted = true;
        }

        // Create window to show the Shutdown overlay view
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        Window win;
        win = PolicyManager.makeNewWindow(context);
        win.setWindowManager(wm, null, null);
        win.requestFeature(Window.FEATURE_NO_TITLE);
        win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ColorDrawable cd = new ColorDrawable(0);
        win.setBackgroundDrawable(cd);
        win.setContentView(com.android.internal.R.layout.recon_progress);
        View v = win.getDecorView();

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.type = WindowManager.LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY;
        lp.format = PixelFormat.RGBA_8888;

        TextView tv = (TextView)v.findViewById(com.android.internal.R.id.recon_progress_text);
        // Show "Shutting Down" if the reboot flag is false. There is also a case when the flag is true
        // but the reboot reason is "ChargeShutdown" that the HUD should still show "Shutting Down" because
        // the HUD is rebooting to a charging mode.
        tv.setText((mReboot == false || mRebootReason.equals("ChargeShutdown")) ? "Shutting Down" : "Rebooting");

        ViewTreeObserver o = v.getViewTreeObserver();
        o.addOnGlobalLayoutListener(new ShutdownGlobalLayoutListener(context));

        // Add the view to show the overlay
        wm.addView(v, lp);

        // Set frequency scaling governor to powersave to reduce current spikes during shutdown
        setFreqScalingGovernor(context);

        sInstance.mContext = context;
        sInstance.mPowerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);

        // make sure we never fall asleep again
        sInstance.mCpuWakeLock = null;
        try {
            sInstance.mCpuWakeLock = sInstance.mPowerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK, TAG + "-cpu");
            sInstance.mCpuWakeLock.setReferenceCounted(false);
            sInstance.mCpuWakeLock.acquire();
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to acquire wake lock", e);
            sInstance.mCpuWakeLock = null;
        }

        // also make sure the screen stays on for better user experience
        sInstance.mScreenWakeLock = null;
        if (sInstance.mPowerManager.isScreenOn()) {
            try {
                sInstance.mScreenWakeLock = sInstance.mPowerManager.newWakeLock(
                        PowerManager.FULL_WAKE_LOCK, TAG + "-screen");
                sInstance.mScreenWakeLock.setReferenceCounted(false);
                sInstance.mScreenWakeLock.acquire();
            } catch (SecurityException e) {
                Log.w(TAG, "No permission to acquire wake lock", e);
                sInstance.mScreenWakeLock = null;
            }
        }

        // start the thread that initiates shutdown
        sInstance.mHandler = new Handler() {
        };
        sInstance.start();
    }

    void actionDone() {
        synchronized (mActionDoneSync) {
            mActionDone = true;
            mActionDoneSync.notifyAll();
        }
    }

    /**
     * Makes sure we handle the shutdown gracefully.
     * Shuts off power regardless of radio and bluetooth state if the alloted time has passed.
     */
    public void run() {
        BroadcastReceiver br = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                // We don't allow apps to cancel this, so ignore the result.
                actionDone();
            }
        };

        /*
         * Write a system property in case the system_server reboots before we
         * get to the actual hardware restart. If that happens, we'll retry at
         * the beginning of the SystemServer startup.
         */
        {
            String reason = (mReboot ? "1" : "0") + (mRebootReason != null ? mRebootReason : "");
            SystemProperties.set(SHUTDOWN_ACTION_PROPERTY, reason);
            if(mRebootReason == null) {
                if(mReboot) {
                    SystemProperties.set(SHUTDOWN_REASON_PROPERTY, "Reboot");
                } else {
                    SystemProperties.set(SHUTDOWN_REASON_PROPERTY, "Shutdown");
                }
            } else {
                SystemProperties.set(SHUTDOWN_REASON_PROPERTY, mRebootReason);
            }
        }

        /*
         * If we are rebooting into safe mode, write a system property
         * indicating so.
         */
        if (mRebootSafeMode) {
            SystemProperties.set(REBOOT_SAFEMODE_PROPERTY, "1");
        }

        /*  // ANALYTICS
         * Ask AnalyticsService to shutdown properly so no analytics data is lost...
         */
        IAnalyticsServiceShutdownObserver asShutdownObserver = new IAnalyticsServiceShutdownObserver.Stub() {
            public void onShutDownComplete(int statusCode) throws RemoteException {
                actionDone();
            }
        };

        Log.i(TAG, "Shutting down AnalyticsService");

        // Set initial variables and time out time.
        mActionDone = false;
        final long asEndShutTime = SystemClock.elapsedRealtime() + MAX_SHUTDOWN_WAIT_TIME;
        synchronized (mActionDoneSync) {
            try {
                final IAnalyticsService as =
                        IAnalyticsService.Stub.asInterface(ServiceManager.checkService("com.reconinstruments.os.analyticsagent.IAnalyticsService"));
                if (as != null) {
                    as.shutdown(asShutdownObserver);
                } else {
                    Log.w(TAG, "AnalyticsService unavailable for shutdown");
                    mActionDone = true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception during AnalyticsService shutdown", e);
                mActionDone = true;
            }
            while (!mActionDone) {
                long delay = asEndShutTime - SystemClock.elapsedRealtime();
                if (delay <= 0) {
                    Log.e(TAG, "AnalyticsService shutdown timed out");
                    break;
                }
                try {
                    mActionDoneSync.wait(delay);
                } catch (InterruptedException e) {
                }
            }
        }



        Log.i(TAG, "Sending shutdown broadcast...");

        // First send the high-level shut down broadcast.
        mActionDone = false;
        mContext.sendOrderedBroadcast(new Intent(Intent.ACTION_SHUTDOWN), null,
                br, mHandler, 0, null, null);
        
        final long endTime = SystemClock.elapsedRealtime() + MAX_BROADCAST_TIME;
        synchronized (mActionDoneSync) {
            while (!mActionDone) {
                long delay = endTime - SystemClock.elapsedRealtime();
                if (delay <= 0) {
                    Log.w(TAG, "Shutdown broadcast timed out");
                    break;
                }
                try {
                    mActionDoneSync.wait(delay);
                } catch (InterruptedException e) {
                }
            }
        }
        
        Log.i(TAG, "Shutting down activity manager...");
        
        final IActivityManager am =
            ActivityManagerNative.asInterface(ServiceManager.checkService("activity"));
        if (am != null) {
            try {
                am.shutdown(MAX_BROADCAST_TIME);
            } catch (RemoteException e) {
            }
        }

        // Shutdown radios.
        shutdownRadios(MAX_RADIO_WAIT_TIME);

        // Shutdown MountService to ensure media is in a safe state
        IMountShutdownObserver observer = new IMountShutdownObserver.Stub() {
            public void onShutDownComplete(int statusCode) throws RemoteException {
                Log.w(TAG, "Result code " + statusCode + " from MountService.shutdown");
                actionDone();
            }
        };

        Log.i(TAG, "Shutting down MountService");

        // Set initial variables and time out time.
        mActionDone = false;
        final long endShutTime = SystemClock.elapsedRealtime() + MAX_SHUTDOWN_WAIT_TIME;
        synchronized (mActionDoneSync) {
            try {
                final IMountService mount = IMountService.Stub.asInterface(
                        ServiceManager.checkService("mount"));
                if (mount != null) {
                    mount.shutdown(observer);
                } else {
                    Log.w(TAG, "MountService unavailable for shutdown");
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception during MountService shutdown", e);
            }
            while (!mActionDone) {
                long delay = endShutTime - SystemClock.elapsedRealtime();
                if (delay <= 0) {
                    Log.w(TAG, "Shutdown wait timed out");
                    break;
                }
                try {
                    mActionDoneSync.wait(delay);
                } catch (InterruptedException e) {
                }
            }
        }

        rebootOrShutdown(mReboot, mRebootReason);
    }

    private void shutdownRadios(int timeout) {
        // If a radio is wedged, disabling it may hang so we do this work in another thread,
        // just in case.
        final long endTime = SystemClock.elapsedRealtime() + timeout;
        final boolean[] done = new boolean[1];
        Thread t = new Thread() {
            public void run() {
                boolean nfcOff;
                boolean bluetoothOff;
                boolean radioOff;

                final INfcAdapter nfc =
                        INfcAdapter.Stub.asInterface(ServiceManager.checkService("nfc"));
                final ITelephony phone =
                        ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
                final IBluetooth bluetooth =
                        IBluetooth.Stub.asInterface(ServiceManager.checkService(
                                BluetoothAdapter.BLUETOOTH_SERVICE));

                try {
                    nfcOff = nfc == null ||
                             nfc.getState() == NfcAdapter.STATE_OFF;
                    if (!nfcOff) {
                        Log.w(TAG, "Turning off NFC...");
                        nfc.disable(false); // Don't persist new state
                    }
                } catch (RemoteException ex) {
                Log.e(TAG, "RemoteException during NFC shutdown", ex);
                    nfcOff = true;
                }

                try {
                    bluetoothOff = bluetooth == null ||
                                   bluetooth.getState() == BluetoothAdapter.STATE_OFF;
                    if (!bluetoothOff) {
                        Log.w(TAG, "Disabling Bluetooth...");
                        bluetooth.disable(false);  // disable but don't persist new state
                    }
                } catch (RemoteException ex) {
                    Log.e(TAG, "RemoteException during bluetooth shutdown", ex);
                    bluetoothOff = true;
                }

                try {
                    radioOff = phone == null || !phone.isRadioOn();
                    if (!radioOff) {
                        Log.w(TAG, "Turning off radio...");
                        phone.setRadio(false);
                    }
                } catch (RemoteException ex) {
                    Log.e(TAG, "RemoteException during radio shutdown", ex);
                    radioOff = true;
                }

                Log.i(TAG, "Waiting for NFC, Bluetooth and Radio...");

                while (SystemClock.elapsedRealtime() < endTime) {
                    if (!bluetoothOff) {
                        try {
                            bluetoothOff =
                                    bluetooth.getState() == BluetoothAdapter.STATE_OFF;
                        } catch (RemoteException ex) {
                            Log.e(TAG, "RemoteException during bluetooth shutdown", ex);
                            bluetoothOff = true;
                        }
                        if (bluetoothOff) {
                            Log.i(TAG, "Bluetooth turned off.");
                        }
                    }
                    if (!radioOff) {
                        try {
                            radioOff = !phone.isRadioOn();
                        } catch (RemoteException ex) {
                            Log.e(TAG, "RemoteException during radio shutdown", ex);
                            radioOff = true;
                        }
                        if (radioOff) {
                            Log.i(TAG, "Radio turned off.");
                        }
                    }
                    if (!nfcOff) {
                        try {
                            nfcOff = nfc.getState() == NfcAdapter.STATE_OFF;
                        } catch (RemoteException ex) {
                            Log.e(TAG, "RemoteException during NFC shutdown", ex);
                            nfcOff = true;
                        }
                        if (radioOff) {
                            Log.i(TAG, "NFC turned off.");
                        }
                    }

                    if (radioOff && bluetoothOff && nfcOff) {
                        Log.i(TAG, "NFC, Radio and Bluetooth shutdown complete.");
                        done[0] = true;
                        break;
                    }
                    SystemClock.sleep(PHONE_STATE_POLL_SLEEP_MSEC);
                }
            }
        };

        t.start();
        try {
            t.join(timeout);
        } catch (InterruptedException ex) {
        }
        if (!done[0]) {
            Log.w(TAG, "Timed out waiting for NFC, Radio and Bluetooth shutdown.");
        }
    }

    /**
     * Do not call this directly. Use {@link #reboot(Context, String, boolean)}
     * or {@link #shutdown(Context, boolean)} instead.
     *
     * @param reboot true to reboot or false to shutdown
     * @param reason reason for reboot
     */
    public static void rebootOrShutdown(boolean reboot, String reason) {
        if (reboot) {
            Log.i(TAG, "Rebooting, reason: " + reason);
            try {
                PowerManagerService.lowLevelReboot(reason);
            } catch (Exception e) {
                Log.e(TAG, "Reboot failed, will attempt shutdown instead", e);
            }
        } else if (SHUTDOWN_VIBRATE_MS > 0) {
            // vibrate before shutting down
            Vibrator vibrator = new SystemVibrator();
            try {
                vibrator.vibrate(SHUTDOWN_VIBRATE_MS);
            } catch (Exception e) {
                // Failure to vibrate shouldn't interrupt shutdown.  Just log it.
                Log.w(TAG, "Failed to vibrate during shutdown.", e);
            }

            // vibrator is asynchronous so we need to wait to avoid shutting down too soon.
            try {
                Thread.sleep(SHUTDOWN_VIBRATE_MS);
            } catch (InterruptedException unused) {
            }
        }

        // Shutdown power
        Log.i(TAG, "Performing low-level shutdown...");
        PowerManagerService.lowLevelShutdown();
    }
}
