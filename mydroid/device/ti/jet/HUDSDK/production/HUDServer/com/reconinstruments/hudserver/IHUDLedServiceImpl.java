package com.reconinstruments.hudserver;

import android.content.Context;
import android.os.RemoteException;
import android.os.PowerManager;
import android.util.Log;

import java.util.Arrays;

import com.reconinstruments.os.hardware.led.IHUDLedService;

class IHUDLedServiceImpl extends IHUDLedService.Stub {
    private static final String TAG = "IHUDLedServiceImpl";
    private static final boolean DEBUG = false;
    private static final int POWER_LED_ID = 4; // Must be same value as LightsService.LIGHT_ID_NOTIFICATIONS
    private static final int NORMAL_INTENSITY = 62;

    private final Context context;
    private final PowerManager mPm;
    private static int curIntensity = NORMAL_INTENSITY;

    IHUDLedServiceImpl(Context context) {
        this.context = context;
        this.mPm = (PowerManager)this.context.getSystemService(Context.POWER_SERVICE);
    }

    public int blinkPowerLED(int intensity, int[] pattern) {
        if (DEBUG) {
            Log.d(TAG, "Blink Power LED [" + intensity + "]");
            Log.d(TAG, "Pattern: " + Arrays.toString(pattern));
        }

        // Sanity checks.
        if (pattern.length < 2) {
            Log.e(TAG, "Pattern length too short. Must be at least 2 entries!");
            return 0;
        }

        if (intensity < 0 || intensity > 255) {
            Log.e(TAG, "Intensity value invalid. Should be [0, 255]");
            return 0;
        }

        // Make sure all values in pattern[] are valid
        for (int i = 0; i < pattern.length; i++) {
            if (pattern[i] < 0) {
                Log.e(TAG, "Pattern value in pattern[" + i + "]: " + pattern[i] + " is invalid");
                return 0;
            }
        }

        this.curIntensity = this.mPm.getBrightness(POWER_LED_ID);
        if (DEBUG) Log.d(TAG, "Current intensity: " + this.curIntensity);

        if (this.curIntensity < 0 || this.curIntensity > 255) {
            Log.w(TAG, "Current intensity is out of range! [" + this.curIntensity + "]");
            this.curIntensity = NORMAL_INTENSITY;
        }

        for (int i = 0; i < pattern.length; i++) {
            int sleepMs = pattern[i];
            if (i == 0) {
                // First index indicates how long to wait before starting the pattern.
                lightSleep(sleepMs);
            } else {
                // If odd index into pattern, then we turn off by setting LED brightness to 0.
                int setIntensity = ((i % 2) == 1) ? 0 : intensity;
                this.mPm.setBrightness(POWER_LED_ID, setIntensity);
                lightSleep(sleepMs);
            }
        }

        this.mPm.setBrightness(POWER_LED_ID, this.curIntensity);

        return 1;
    }

    public int contBlinkPowerLED(int intensity, int onMs, int offMs, boolean onOff) {
        if (DEBUG) Log.d(TAG, "Continuous blink Power LED [" + intensity +
                              ", " + onMs + ", " + offMs + ", " + onOff + "]");
        // Sanity checks.
        if (onOff) {
            this.curIntensity = this.mPm.getBrightness(POWER_LED_ID);
            if (DEBUG) Log.d(TAG, "Current intensity: " + this.curIntensity);

            if (this.curIntensity < 0 || this.curIntensity > 255) {
                Log.w(TAG, "Current intensity is out of range! [" + this.curIntensity + "]");
                this.curIntensity = NORMAL_INTENSITY;
            }


            if (intensity < 0 || intensity > 255) {
                Log.e(TAG, "Intensity value invalid. Should be [0, 255]");
                return 0;
            }
            if (onMs <= 0 || offMs <= 0) {
                Log.e(TAG, "Duration must be greater than 0");
                return 0;
            }
            this.mPm.setFlashing(POWER_LED_ID, intensity, onMs, offMs);
        } else {
            if (DEBUG) Log.d(TAG, "Resetting to " + this.curIntensity);
            this.mPm.setBrightness(POWER_LED_ID, this.curIntensity);
        }

        return 1;
    }

    public int setPowerLEDBrightness(int intensity) {
        if (DEBUG) Log.d(TAG, "Setting power LED brightness [" + intensity + "]");

        if (intensity < 0 || intensity > 255) {
            Log.e(TAG, "Intensity value invalid. Should be [0, 255]");
            return 0;
        }

        this.curIntensity = intensity;
        this.mPm.setBrightness(POWER_LED_ID, this.curIntensity);
        return 1;
    }

    private void lightSleep(int milliseconds) {
        try {
            if (milliseconds > 0) Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Log.d(TAG, "Failed to sleep");
        }
    }
}
