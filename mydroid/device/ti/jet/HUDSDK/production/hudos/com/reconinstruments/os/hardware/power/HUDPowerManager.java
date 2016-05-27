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

package com.reconinstruments.os.hardware.power;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Slog; /* This is to avoid generating events for ourselves */

/**
 * Class that controls, manages and monitors the power system on the HUD.
 *
 * To obtain an instance of the power system, call HUDPowerManager.getInstance().
 * {@hide}
 */
public class HUDPowerManager {
    private static final String TAG = "HUDPowerManager";
    private static final String REMOTE_SERVICE_NAME = IHUDPowerService.class.getName();
    private static final boolean DEBUG = false; // change to true to enable debugging

    /**
     * Extra for android.content.Intent.ACTION_SHUTDOWN_REASON:
     * Integer describing the reason for device shutdown
     */
    public static final String EXTRA_REASON = "reason";

    /**
     * Extra for android.content.Intent.ACTION_SHUTDOWN_REASON}:
     * String describing the reason for device shutdown
     */
    public static final String EXTRA_REASON_STR = "reason_str";

    /**
     * Indicates a graceful last shutdown or reboot. Used with {@link #getLastShutdownReason}.
     */
    public static final int SHUTDOWN_GRACEFUL           = 0;

    /**
     * Indicates an abrupt unknown last shutdown. Used with {@link #getLastShutdownReason}.
     */
    public static final int SHUTDOWN_ABRUPT             = 1;

    /**
     * Indicates a last shutdown due to battery being removed. Used with {@link #getLastShutdownReason}.
     */
    public static final int SHUTDOWN_BATT_REMOVED       = 2;

    /**
     * Instant current update with 10 second interval. Used with {@link #getCurrent}.
     */
    public static final int INSTANT_CURRENT_UPDATE_INTERVAL_10_SEC = 0;

    /**
     * Average current update with 10 second interval. Used with {@link #getCurrent}.
     */
    public static final int AVERAGE_CURRENT_UPDATE_INTERVAL_10_SEC = 1;

    /**
     * Invalid return value.
     */
    public static final int BAD_VALUE                   = 0x80000000;

    /**
     * hotplug frequency scaling governor
     * @hide
     */
    public static final int FREQ_SCALING_GOV_HOTPLUG        = 0;

    /**
     * interactive frequency scaling governor
     * @hide
     */
    public static final int FREQ_SCALING_GOV_INTERACTIVE    = 1;

    /**
     * conservative frequency scaling governor
     * @hide
     */
    public static final int FREQ_SCALING_GOV_CONSERVATIVE   = 2;

    /**
     * userspace frequency scaling governor
     * @hide
     */
    public static final int FREQ_SCALING_GOV_USERSPACE      = 3;

    /**
     * powersave frequency scaling governor
     * @hide
     */
    public static final int FREQ_SCALING_GOV_POWERSAVE      = 4;

    /**
     * ondemand frequency scaling governor
     * @hide
     */
    public static final int FREQ_SCALING_GOV_ONDEMAND       = 5;

    /**
     * performance frequency scaling governor
     * @hide
     */
    public static final int FREQ_SCALING_GOV_PERFORMANCE    = 6;

    // Singleton instance
    private static HUDPowerManager sInstance;

    private final IHUDPowerService mService;

    /**
     * Get an instance (Singleton) to the HUDPowerManager.
     *
     * @return the HUDPowerManager object to control, manage and monitor the power
     * system on the HUD
     *
     * @throws IllegalStateException if HUDPowerManager cannot connect to the IHUDPowerService.
     * {@hide}
     */
    public static synchronized HUDPowerManager getInstance() {
        if (sInstance == null) {
            sInstance = new HUDPowerManager();
        }
        return sInstance;
    }

    private HUDPowerManager() {
        Log.d(TAG, "Connecting to IHUDPowerService by name [" + REMOTE_SERVICE_NAME + "]");
        mService = IHUDPowerService.Stub.asInterface(ServiceManager.getService(REMOTE_SERVICE_NAME));
        if (mService == null) {
            throw new IllegalStateException("Failed to find IHUDPowerService by name [" + REMOTE_SERVICE_NAME + "]");
        }
    }

    /**
     * Get the average battery voltage value for the past 10 seconds.
     * Use this function less frequently then 10 seconds as the value wouldn't change.
     *
     * @return The average battery voltage in mV for the past 10 seconds,
     *         or (INT_MIN), (INT_MIN+1) for an error.
     *
     * @throws RuntimeException if HUDPowerManager cannot retrieve the battery voltage from the
     *         IHUDPowerService.
     */
    public int getBatteryVoltage() {
        try {
            return mService.getBatteryVoltage();
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke getBatteryVoltage", e);
        }
    }

    /**
     * Get average or instant current in mA for the past 10 seconds.
     * Use this function less frequently then 10 seconds as the system updates both
     * current values every 10 seconds.
     *
     * @param currenttype
     *     {@link #INSTANT_CURRENT_UPDATE_INTERVAL_10_SEC} or {@link #AVERAGE_CURRENT_UPDATE_INTERVAL_10_SEC}.
     *
     * @return The average current in mA for the past 10 or 50 seconds (at least),
     *         Negative means discharge, postive means that the battery is being charged,
     *         or (INT_MIN), (INT_MIN+1) for an error.
     *
     * @throws RuntimeException if HUDPowerManager cannot retrieve the current from the
     *         IHUDPowerService.
     */
    public int getCurrent(int currenttype) {
        try {
            if (DEBUG) Slog.d(TAG, "Getting Current");
            if (currenttype == AVERAGE_CURRENT_UPDATE_INTERVAL_10_SEC) {
                return mService.getAverageCurrent();
            } else if (currenttype == INSTANT_CURRENT_UPDATE_INTERVAL_10_SEC) {
                return mService.getCurrent();
            } else {
                return BAD_VALUE;
            }
        } catch (Exception e) {
            if (currenttype == AVERAGE_CURRENT_UPDATE_INTERVAL_10_SEC) {
                throw new RuntimeException("Failed to invoke getAverageCurrent", e);
            } else {
                throw new RuntimeException("Failed to invoke getCurrent", e);
            }
        }
    }

    /**
     * Get the battery capacity percentage.
     * Use this function less frequently then 10 seconds as the value wouldn't change.
     *
     * @return The battery capacity percentage,
     *         or (INT_MIN), (INT_MIN+1) for an error.
     *
     * @throws RuntimeException if HUDPowerManager cannot retrieve the battery percentage from the
     *         IHUDPowerService.
     */
    public int getBatteryPercentage() {
        try {
            return mService.getBatteryPercentage();
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke getBatteryPercentage", e);
        }
    }

    /**
     * Get the battery temperature in Celsius.
     * Use this function less frequently then 10 seconds as the value wouldn't change.
     *
     * @return The battery temperature in Celsius,
     *         or (INT_MIN), (INT_MIN+1) for an error.
     *
     * @throws RuntimeException if HUDPowerManager cannot retrieve the battery temperature from the
     *         IHUDPowerService.
     */
    public int getBatteryTemperature_C() {
        try {
            return mService.getBatteryTemperature_C();
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke getBatteryTemperature_C", e);
        }
    }

    /**
     * Indicates whether the last shutdown was gracefull or abrupt.
     *
     * @return One of {@link #SHUTDOWN_ABRUPT}, {@link #SHUTDOWN_BATT_REMOVED}, or {@link #SHUTDOWN_GRACEFUL}.
     *
     * @throws RuntimeException if HUDPowerManager cannot retrieve the last shutdown reason from the
     *         IHUDPowerService.
     */
    public int getLastShutdownReason() {
        try {
            if (DEBUG) Slog.d(TAG, "Checking the reason for the last shutdown");
            return mService.getLastShutdownReason();
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke getLastShutdownReason", e);
        }
    }

    /**
     * Enable or disable compass temperature sensing in the real hardware.
     * @param enable_disable
     *  0 for disable and 1 for enable.
     * @return 1 if successful, otherwise is failure.
     *
     * @throws RuntimeException if HUDPowerManager cannot set the compass temperature to the
     *         IHUDPowerService.
     * @hide
     */
    public int setCompassTemperature(boolean enable_disable) {
         try {
            return mService.setCompassTemperature(enable_disable);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke setCompassTemperature", e);
        }
    }

    /**
     * Get the compass raw temperature data in u16 format from OUT_TEMP_L_XM
     * and OUT_TEMP_H_XM, check lsm9ds0 data sheet.
     *
     * @return The compass raw temperature data in u16 format,
     *         or (INT_MIN), (INT_MIN+1) for an error.
     *
     * @throws RuntimeException if HUDPowerManager cannot retrieve the compass temperature from the
     *         IHUDPowerService.
     *
     * @hide
     */
    public int getCompassTemperature() {
        try {
            return mService.getCompassTemperature();
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke getCompassTemperature", e);
        }
    }

    /**
     * Get the mainboard temperature in Celsius.
     * When polling, use this function at least every 2 second time interval.
     *
     * @return The mainboard temperature in Celsius,
     *         or (INT_MIN), (INT_MIN+1) for an error.
     *
     * @throws RuntimeException if HUDPowerManager cannot retrieve the board temperature from the
     *         IHUDPowerService.
     */
    public int getBoardTemperature() {
        try {
            return mService.getBoardTemperature();
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke getCompassTemperature", e);
        }
    }

    /**
     * Set the frequency scaling governor.
     *
     * @param governor
     *  one of FREQ_SCALING_GOV_HOTPLUG, FREQ_SCALING_GOV_INTERACTIVE, FREQ_SCALING_GOV_CONSERVATIVE,
     *  FREQ_SCALING_GOV_USERSPACE, FREQ_SCALING_GOV_POWERSAVE, FREQ_SCALING_GOV_ONDEMAND, FREQ_SCALING_GOV_PERFORMANCE
     *
     * @return 1 if successful, otherwise is failure
     *
     * @throws RuntimeException if HUDPowerManager cannot set the frequency scaling governor to the
     *         IHUDPowerService.
     *
     * @hide
     */
    public int setFreqScalingGovernor(int governor) {
        try {
            return mService.setFreqScalingGovernor(governor);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke setFreqScalingGovernor", e);
        }
    }
}

