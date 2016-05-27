/*
 * Copyright (C) 2014 Recon Instruments
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

package com.reconinstruments.os.hardware.screen;

import android.os.Build;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;

import com.reconinstruments.os.hardware.glance.HUDGlanceManager;
import com.reconinstruments.os.hardware.glance.GlanceDetectionListener;

/**
 * Class that controls the screen on the HUD
 *
 * To obtain an instance of the screen manager, call HUDScreenManager.getInstance()
 *
 * {@hide}
 */
public class HUDScreenManager implements GlanceDetectionListener {

    private static final String TAG = "HUDScreenManager";
    private static final String REMOTE_SERVICE_NAME = IHUDScreenService.class.getName();
    private static final boolean DEBUG = false; // change to true to enable debugging

    // Singleton instance
    private static HUDScreenManager sInstance;
    private static HUDGlanceManager sGlanceInstance;
    private final IHUDScreenService mService;

    // Screen states. These MUST have the same values as SCREEN_STATE enum in
    // HUDScreen's JNI. See getScreenState().
    public static final int SCREEN_STATE_BL_OFF = 0;            // Backlight turned off
    public static final int SCREEN_STATE_POWER_OFF = 1;         // Display system powered off
    public static final int SCREEN_STATE_PENDING_OFF = 2;       // Backlight pending to be turned off
    public static final int SCREEN_STATE_FADING_OFF = 3;        // Backlight fading off
    public static final int SCREEN_STATE_ON = 4;                // Display and backlight are on
    public static final int SCREEN_STATE_FORCED_ON = 5;         // Display and backlight forced on
    public static final int SCREEN_STATE_FORCED_STAY_ON = 6;    // Display and backlight forced stay on

    /**
     * Get an instance (Singleton) to the HUDScreenManager
     *
     * @return the HUDScreenManager object to control the Screens on the HUD
     *
     * @throws IllegalStateException if HUDScreenManager cannot connect to the IHUDScreenService.
     * {@hide}
     */
    public static synchronized HUDScreenManager getInstance() {
        if (Build.MODEL.equalsIgnoreCase("Snow2")) {
            return null;
        }
        if (sInstance == null) {
            sInstance = new HUDScreenManager();
        }
        return sInstance;
    }

    private HUDScreenManager() {
        Slog.d(TAG, "Connecting to IHUDScreenService by name [" + REMOTE_SERVICE_NAME + "]");
        mService = IHUDScreenService.Stub.asInterface(ServiceManager.getService(REMOTE_SERVICE_NAME));
        if (mService == null) {
            throw new IllegalStateException("Failed to find IHUDScreenService by name [" + REMOTE_SERVICE_NAME + "]");
        }
    }

    /**
     * Register to the glance detection service
     *
     * @return event - The last detected glance event. -1 on failure.
     *
     * @throws RuntimeException if HUDScreenManager cannot register to glance detection.
     */
    public int registerToGlance() {
        try {
            if (DEBUG) Slog.d(TAG, "Registering to glance using HUDGlanceManager");

            if (sGlanceInstance == null) {
                sGlanceInstance = HUDGlanceManager.getInstance();
            }
            return sGlanceInstance.registerGlanceDetection(this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register to glance", e);
        }
    }

    /**
     * Unregister to the glance detection service
     *
     * @return 1 if successful, -1 on failure.
     *
     * @throws RuntimeException if HUDScreenManager cannot unregister to glance detection.
     */
    public int unregisterToGlance() {
        try {
            if (DEBUG) Slog.d(TAG, "Unregistering to glance using HUDGlanceManager");

            // Before unregistering to glance, make sure the screen is on and cancel
            // any forced screen on timers.
            try {
                mService.cancelForceScreen();
                mService.screenOn(true);
                mService.setScreenState(SCREEN_STATE_ON);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to execute screenOn");
            }

            if (sGlanceInstance == null) {
                Slog.e(TAG, "Glance instance not initialized!");
                return -1;
            } else {
                sGlanceInstance.unregisterGlanceDetection(this);
                return 1;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to unregister to glance", e);
        }
    }

    /**
     * Retrieve the current screen state.
     *
     * @return the screen state.
     *
     * @throws RuntimeException if HUDScreenManager fails to retrieve the screen state from the
     * IHUDScreenService.
     */
    public int getScreenState() {
        try {
            if (DEBUG) Slog.d(TAG, "Retrieving screen state");
            return mService.getScreenState();
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve screen state", e);
        }
    }

    /**
     * Set the screen off delay. The screen will wait delay ms before fading
     * off the display when the glance detection mechanism detected an ahead glance.
     *
     * @param delay - the delay in ms.
     *
     * @return 1 if successful, otherwise is failure.
     *
     * @throws RuntimeException if HUDScreenManager fails to set the screen off delay to the
     * IHUDScreenService.
     */
    public int setScreenOffDelay(int delay) {
        // Sanity check
        if (delay < 0) {
            Slog.e(TAG, "setScreenOffDelay: invalid delay " + delay);
            return 0;
        }

        try {
            if (DEBUG) Slog.d(TAG, "Setting screen off delay: " + delay);
            mService.setScreenOffDelay(delay);
            return 1;
        } catch (Exception e) {
            throw new RuntimeException("Failed to set screen off delay", e);
        }
    }

    /**
     * Force the screen on. The screen will turn on for delay seconds.
     *
     * @param delay - the delay in seconds.
     *        stayOn - if true, screen stays on regardless of any screen events, otherwise,
     *                 screen may turn on/off depending on other screen events.
     *
     * @return 1 if successful, otherwise is failure.
     *
     * @throws RuntimeException if HUDScreenManager fails to force the screen on with the
     * IHUDScreenService.
     */
    public int forceScreenOn(int delay, boolean stayOn) {
        // Sanity check
        if (delay < 0) {
            Slog.e(TAG, "forceScreenOn: invalid delay " + delay);
            return 0;
        }

        try {
            if (DEBUG) Slog.d(TAG, "Forcing screen on for: " + delay + " stayOn: " + stayOn);
            mService.forceScreenOn(delay, stayOn);
        } catch (Exception e) {
            throw new RuntimeException("Failed to force screen on", e);
        }
        return 1;
    }

    @Override
    /**
     * GlanceDetectionListener callback function to indicate ahead/display glance detected
     *
     * @param atDisplay - true to indicate display glance, false to indicate ahead glance
     *
     * @throws RuntimeException if HUDScreenManager fails to set the screen state with the
     * IHUDScreenService.
     */
    public void onDetectEvent(boolean atDisplay) {
        String text = (atDisplay) ? "Display glance" : "Ahead glance";
        if (DEBUG) Slog.d(TAG, "Detected: " + text);
        try {
            mService.screenOn(atDisplay);
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to execute screenOn");
        }
    }

    @Override
    /**
     * GlanceDetectionListener callback function to indicate put on/removal detection.
     *
     * @param removed - true to indicate removal, false to indicate put on
     *
     * @throws RuntimeException if HUDScreenManager fails to set the screen state with the
     * IHUDScreenService.
     */
    public void onRemovalEvent(boolean removed) {
        String text = (removed) ? "Removed" : "Put on";
        if (DEBUG) Slog.d(TAG, "Detected: " + text);
        try {
            mService.screenOn(!removed);
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to execute screenOn");
        }
    }
}

