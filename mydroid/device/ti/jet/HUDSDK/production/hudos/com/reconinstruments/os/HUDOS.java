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

package com.reconinstruments.os;

import java.lang.Object;

import com.reconinstruments.os.hardware.glance.HUDGlanceManager;
import com.reconinstruments.os.hardware.led.HUDLedManager;
import com.reconinstruments.os.hardware.motion.HUDActivityMotionManager;
import com.reconinstruments.os.hardware.power.HUDPowerManager;
import com.reconinstruments.os.hardware.sensors.HUDHeadingManager;
import com.reconinstruments.os.connectivity.HUDConnectivityManager;

/**
 * This is a class which allows application to access specific recon os services
 */
public class HUDOS {
    private static final String TAG = "HUDOS";

    /**
     * Used to retrieve HUD glance service. See {@link #getHUDService}.
     * @hide
     */
    public static final int HUD_GLANCE_SERVICE = 1;

    /**
     * Used to retrieve HUD led service. See {@link #getHUDService}.
     * @hide
     */
    public static final int HUD_LED_SERVICE = 2;

    /**
     * Used to retrieve HUD activity motion service. See {@link #getHUDService}.
     * @hide
     */
    public static final int HUD_ACTIVITY_MOTION_SERVICE = 3;

    /**
     * Used to retrieve HUD power service. See {@link #getHUDService}.
     * @hide
     */
    public static final int HUD_POWER_SERVICE = 4;

    /**
     * Used to retrieve HUD heading service. See {@link #getHUDService}.
     */
    public static final int HUD_HEADING_SERVICE = 5;

    /**
     * Used to retrieve HUD connectivity service. See {@link #getHUDService}.
     */
    public static final int HUD_CONNECTIVITY_SERVICE = 6;

    private static HUDGlanceManager mHUDGlanceManager = null;
    private static HUDLedManager mHUDLedManager = null;
    private static HUDActivityMotionManager mHUDActivityMotionManager = null;
    private static HUDPowerManager mHUDPowerManager = null;
    private static HUDHeadingManager mHUDHeadingManager = null;
    private static HUDConnectivityManager mHUDConnectivityManager = null;

    /**
     * Return the handle to recon hudos service by service number. The class of the
     * returned object varies by the requested service number. Currently available
     * service numbers are:
     *
     * <dl>
     *     <dt> {@link #HUD_HEADING_SERVICE}
     *     <dd> A HUDHeadingManager that reports the heading position on the HUD.
     *     <dt> {@link #HUD_CONNECTIVITY_SERVICE}
     *     <dd> A HUDConnectivityManager that manages the web connectivity on the HUD.
     * </dl>
     *
     * @param hudService The number of the desired service
     *
     * @return The service or null if the service does not exist
     *
     * @see #HUD_HEADING_SERVICE
     * @see com.reconinstruments.os.hardware.sensors.HUDHeadingManager
     * @see #HUD_CONNECTIVITY_SERVICE
     * @see com.reconinstruments.os.connectivity.HUDConnectivityManager
     */
    public static Object getHUDService(int hudService) {
        if (hudService == HUD_GLANCE_SERVICE) {
            if (mHUDGlanceManager == null) {
                mHUDGlanceManager = HUDGlanceManager.getInstance();
            }
            return mHUDGlanceManager;
        }

        if (hudService == HUD_LED_SERVICE) {
            if (mHUDLedManager == null) {
                mHUDLedManager = HUDLedManager.getInstance();
            }
            return mHUDLedManager;
        }

        if (hudService == HUD_ACTIVITY_MOTION_SERVICE) {
            if (mHUDActivityMotionManager == null) {
                mHUDActivityMotionManager = HUDActivityMotionManager.getInstance();
            }
            return mHUDActivityMotionManager;
        }

        if (hudService == HUD_POWER_SERVICE) {
            if (mHUDPowerManager == null) {
                mHUDPowerManager = HUDPowerManager.getInstance();
            }
            return mHUDPowerManager;
        }

        if (hudService == HUD_HEADING_SERVICE) {
            if (mHUDHeadingManager == null) {
                mHUDHeadingManager = HUDHeadingManager.getInstance();
            }
            return mHUDHeadingManager;
        }

        if (hudService == HUD_CONNECTIVITY_SERVICE) {
            if (mHUDConnectivityManager == null) {
                mHUDConnectivityManager = new HUDConnectivityManager();
                mHUDConnectivityManager.initOnHUD();
            }
            return mHUDConnectivityManager;
        }

        return null;
    }
}

