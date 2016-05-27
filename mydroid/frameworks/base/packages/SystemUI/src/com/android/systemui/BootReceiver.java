/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.systemui;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.view.IWindowManager;
import android.util.Slog;

import com.reconinstruments.os.hardware.led.HUDLedManager;

/**
 * Performs a number of miscellaneous, non-system-critical actions
 * after the system has finished booting.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "SystemUIBootReceiver";

    private HUDLedManager mHUDLedManager = null;
    private static final float DEFAULT_ANIMATION_SCALE = 1.0f;

    @Override
    public void onReceive(final Context context, Intent intent) {
        try {
            if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
                // Start the load average overlay, if activated
                ContentResolver res = context.getContentResolver();

                if (Settings.System.getInt(res, Settings.System.SHOW_PROCESSES, 0) != 0) {
                    Intent loadavg = new Intent(context, com.android.systemui.LoadAverageService.class);
                    context.startService(loadavg);
                }

                // Enforce animation scale to the default one.
                IWindowManager wm = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
                try {
                    Slog.d(TAG, "Animation scale: " + wm.getAnimationScale(0));
                    if (wm.getAnimationScale(0) != DEFAULT_ANIMATION_SCALE) {
                        Slog.d(TAG, "Setting animation scale to " + DEFAULT_ANIMATION_SCALE);
                        wm.setAnimationScale(0, DEFAULT_ANIMATION_SCALE);
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to set animation scale");
                }
            }
            else if (intent.getAction().equals("com.reconinstruments.hud.led.SET_NORMAL")) {
                if (mHUDLedManager == null) {
                    // Retrieve an instance of LED manager to set the default LED brightness
                    mHUDLedManager = HUDLedManager.getInstance();
                    mHUDLedManager.setPowerLEDBrightness(HUDLedManager.BRIGHTNESS_NORMAL);
                }
            }
        } catch (Exception e) {
            Slog.e(TAG, "Can't start load average service", e);
        }
    }
}
