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
package com.reconinstruments.lib.hardware;

import java.lang.ref.WeakReference;
import android.util.Log;
import android.content.IntentFilter;
import android.content.Intent;
import android.app.ActivityManagerNative;

public class HUDGlance {
    private static final String TAG = "HUDGlance";

    private static boolean mGlanceStopping = false;

    private int mNativeContext; // accessed by native methods
    public GlanceEventCallback mGlanceEventCallback;

    private static final int EVENT_GLANCE_STOPPED = 4; // Must equal HUDGlanceManager.EVENT_GLANCE_STOPPED

    public native final int native_setup(Object weak_this);
    public native final int native_aheadCalibration();
    public native final int native_displayCalibration();
    public native final int native_startGlanceDetection();
    public native final int native_stopGlanceDetection();

    public HUDGlance(GlanceEventCallback callback) {
        mGlanceEventCallback = callback;
    }

    // Called from JNI level for event reporting
    private static synchronized void postEventFromNative(Object weak_ref, int event)
    {
        HUDGlance glance = (HUDGlance)((WeakReference)weak_ref).get();
        if (glance == null) {
            Log.e(TAG, "Callback from native returned null reference");
            return;
        }

        if (event == EVENT_GLANCE_STOPPED) {
            mGlanceStopping = false;
        }

        if (!mGlanceStopping && glance.mGlanceEventCallback != null) {
            glance.mGlanceEventCallback.onEvent(event);
        }
    }

    public synchronized void requestGlanceStop() {
        mGlanceStopping = true;
    }

    public interface GlanceEventCallback {
        void onEvent(int event);
    }
}
