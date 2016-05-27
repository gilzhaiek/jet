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

package com.reconinstruments.os.hardware.led;

import android.os.Build;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Slog; /* This is to avoid generating events for ourselves */

/**
 * Class that controls the LEDs on the HUD.
 *
 * To obtain an instance of the LED manager, call HUDLedManager.getInstance().
 * {@hide}
 */
public class HUDLedManager {

    private static final String TAG = "HUDLedManager";
    private static final String REMOTE_SERVICE_NAME = IHUDLedService.class.getName();
    private static final boolean DEBUG = false; // change to true to enable debugging

    /**
     * Low LED brightness value. Used as intensity field in {@link #blinkPowerLED} and {@link #contBlinkPowerLED}.
     */
    public static final int BRIGHTNESS_LOW = Build.MODEL.equalsIgnoreCase("JET") ? 2 : 20;

    /**
     * Normal LED brightness value. Used as intensity field in {@link #blinkPowerLED} and {@link #contBlinkPowerLED}.
     */
    public static final int BRIGHTNESS_NORMAL = Build.MODEL.equalsIgnoreCase("JET") ? 26 : 62;

    /**
     * High LED brightness value. Used as intensity field in {@link #blinkPowerLED} and {@link #contBlinkPowerLED}.
     */
    public static final int BRIGHTNESS_HIGH = Build.MODEL.equalsIgnoreCase("JET") ? 255 : 255;

    // Singleton instance
    private static HUDLedManager sInstance;

    private final IHUDLedService mService;

    /**
     * Get an instance (Singleton) to the HUDLedManager.
     *
     * @return The HUDLedManager object to control the LEDs on the HUD.
     *
     * @throws IllegalStateException if HUDLedManager cannot connect to the IHUDLedService.
     * {@hide}
     */
    public static synchronized HUDLedManager getInstance() {
        if (sInstance == null) {
            sInstance = new HUDLedManager();
        }
        return sInstance;
    }

    private HUDLedManager() {
        Log.d(TAG, "Connecting to IHUDLedService by name [" + REMOTE_SERVICE_NAME + "]");
        mService = IHUDLedService.Stub.asInterface(ServiceManager.getService(REMOTE_SERVICE_NAME));
        if (mService == null) {
            throw new IllegalStateException("Failed to find IHUDLedService by name [" + REMOTE_SERVICE_NAME + "]");
        }
    }

    /**
     * Blink the power LED based on a pattern (blocking call).
     * <p>This expects the pattern array to represent the durations in milliseconds to turn on
     * or off the LED. The first value indicates the number of milliseconds to wait before turning
     * the LED off. The next value indicates the number of milliseconds for which to keep the LED
     * off before turning it on. Subsequent values alternate between durations in milliseconds to
     * turn the LED off or to turn the LEDs on.
     *
     * <p>The array represents the following: [wait, off, on, off, on, ... , off, on, off]
     *
     * <p>For example, an array of [500, 100, 5000, 1000] will wait 500 ms before turning
     * off the LED for 100 ms. The LEDs will turn on for 5000 ms before turning off for 1000 ms.
     * The LEDs will turn back on (return to its original state) before the function returns.
     *
     * @param intensity
     *        The intensity of the power LED to use when blinking. Acceptable range [0,255].
     *
     * @param pattern
     *        An array of ints of times for which to turn the LED on or off.
     *
     * @return 1 if successful, otherwise is failure.
     *
     * @throws RuntimeException if HUDLedManager cannot blink power LED with the IHUDLedService.
     */
    public int blinkPowerLED(int intensity, int[] pattern) {
        try {
            if (DEBUG) Slog.d(TAG, "Blinking power LED");
            return mService.blinkPowerLED(intensity, pattern);
        } catch (Exception e) {
            throw new RuntimeException("Failed to blink power LED", e);
        }
    }

    /**
     * Continuously blink power LED.
     *
     * @param intensity
     *        The intensity of the power LED to use when blinking continuously.
     *        Acceptable range [0,255].
     *
     * @param onMs
     *        The duration to have the power LED on in milliseconds.
     *
     * @param offMs
     *        The duration to have the power LED off in milliseconds.
     *
     * @param onOff
     *        True to start the continous blinking. False to stop the continuous blinking.
     *        If set to false, other parameters are ignored.
     *
     * @return 1 if successful, otherwise is failure.
     *
     * @throws RuntimeException if HUDLedManager cannot continuously blink power LED with the IHUDLedService.
     */
    public int contBlinkPowerLED(int intensity, int onMs, int offMs, boolean onOff) {
        try {
            if (DEBUG) Slog.d(TAG, "Continuously blink power LED");
            return mService.contBlinkPowerLED(intensity, onMs, offMs, onOff);
        } catch (Exception e) {
            throw new RuntimeException("Failed to continuously blink power LED", e);
        }
    }

    /**
     * Recon internal function to set brightness of power LED.
     *
     * @param intensity
     *        The intensity of the power LED to set. Acceptable range [0,255].
     *
     * @return 1 if successful, otherwise is failure.
     *
     * @throws RuntimeException if HUDLedManager cannot set the power LED brightness with the IHUDLedService.
     *
     * {@hide}
     */
    public int setPowerLEDBrightness(int intensity) {
        try {
            if (DEBUG) Slog.d(TAG, "Set power LED");
            return mService.setPowerLEDBrightness(intensity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set power LED brightness", e);
        }
    }
}

