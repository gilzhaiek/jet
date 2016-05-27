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
package com.reconinstruments.hudserver;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.reconinstruments.os.hardware.glance.IGlanceDetectionListener;
import com.reconinstruments.os.hardware.glance.IGlanceCalibrationListener;
import com.reconinstruments.os.hardware.glance.IHUDGlanceService;
import com.reconinstruments.os.hardware.glance.HUDGlanceManager;
import com.reconinstruments.lib.hardware.HUDGlance;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

class IHUDGlanceServiceImpl extends IHUDGlanceService.Stub {
    private static final String TAG = "IHUDGlanceServiceImpl";
    private static final boolean DEBUG = false;
    private final Context mContext;
    private HUDGlance mHUDGlance;
    private static final String PERMISSION = "com.reconinstruments.hudserver.GLANCE_CALIBRATION";
    private static int mLastGlanceEvent = HUDGlanceManager.EVENT_GLANCE_DISPLAY;

    private final Map<IBinder, DetectionListenerTracker> mDetectionListeners =
        new HashMap<IBinder, DetectionListenerTracker>();
    private final Map<IBinder, CalibrationListenerTracker> mCalibrationListeners =
        new HashMap<IBinder, CalibrationListenerTracker>();

    IHUDGlanceServiceImpl(Context context) {
        this.mContext = context;
        this.mHUDGlance = new HUDGlance(mCallback);
        // Setup JNI layer
        if (this.mHUDGlance.native_setup(new WeakReference<HUDGlance>(this.mHUDGlance)) == -1) {
            Log.e(TAG, "Failed to setup glance service.");
            this.mHUDGlance = null;
        }
    }

    public int aheadCalibration() {
        int ret = -1;
        if (mContext.checkCallingOrSelfPermission(PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Requires GLANCE_CALIBRATION permission");
        }

        if (DEBUG) Log.d(TAG, "Start ahead calibration");
        if (this.mHUDGlance != null) {
            ret = this.mHUDGlance.native_aheadCalibration();
        }
        return ret;
    }

    public int displayCalibration() {
        int ret = -1;
        if (mContext.checkCallingOrSelfPermission(PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Requires GLANCE_CALIBRATION permission");
        }

        if (DEBUG) Log.d(TAG, "Start display calibration");
        if (this.mHUDGlance != null) {
            ret = this.mHUDGlance.native_displayCalibration();
        }
        return ret;
    }

    public int registerGlanceDetection(IGlanceDetectionListener listener) throws RemoteException {
        if (listener != null && this.mHUDGlance != null) {
            IBinder binder = listener.asBinder();
            synchronized(this.mDetectionListeners) {
                if (this.mDetectionListeners.containsKey(binder)) {
                    Log.w(TAG, "Ignoring duplicate detection listener: " + listener);
                } else {
                    DetectionListenerTracker listenerTracker = new DetectionListenerTracker(listener);
                    binder.linkToDeath(listenerTracker, 0);
                    this.mDetectionListeners.put(binder, listenerTracker);
                    if (DEBUG) Log.d(TAG, "Registered detection listener: " + listener);
                    if (this.mDetectionListeners.size() == 1) {
                        if (DEBUG) Log.d(TAG, "Start glance detection");

                        // First listener, so start glance detection
                        this.mHUDGlance.native_startGlanceDetection();
                    }
                }
            }
            return mLastGlanceEvent;
        }
        return -1;
    }

    public void unregisterGlanceDetection(IGlanceDetectionListener listener) {
        if (listener != null && this.mHUDGlance != null) {
            IBinder binder = listener.asBinder();
            synchronized(this.mDetectionListeners) {
                DetectionListenerTracker listenerTracker = this.mDetectionListeners.remove(binder);
                if (listenerTracker == null) {
                    Log.w(TAG, "Ignoring unregistered listener: " + binder);
                } else {
                    if (DEBUG) Log.d(TAG, "Unregistered listener: " + binder);
                    binder.unlinkToDeath(listenerTracker, 0);
                    if (this.mDetectionListeners.isEmpty()) {
                        if (DEBUG) Log.d(TAG, "Stop glance detection");

                        // Notify HUDGlance that glance detection is stopping
                        this.mHUDGlance.requestGlanceStop();
                        this.mHUDGlance.native_stopGlanceDetection();
                    }
                }
            }
        }
    }

    private final class DetectionListenerTracker implements IBinder.DeathRecipient {
        private final IGlanceDetectionListener listener;

        public DetectionListenerTracker(IGlanceDetectionListener listener) {
            this.listener = listener;
        }

        public IGlanceDetectionListener getListener() {
            return this.listener;
        }

        public void binderDied() {
            IHUDGlanceServiceImpl.this.unregisterGlanceDetection(this.listener);
        }
    }

    public void registerGlanceCalibration(IGlanceCalibrationListener listener) throws RemoteException {
        if (mContext.checkCallingOrSelfPermission(PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Requires GLANCE_CALIBRATION permission");
        }

        if (listener != null && this.mHUDGlance != null) {
            IBinder binder = listener.asBinder();
            synchronized(this.mCalibrationListeners) {
                if (this.mCalibrationListeners.containsKey(binder)) {
                    Log.w(TAG, "Ignoring duplicate calibration listener: " + listener);
                } else {
                    CalibrationListenerTracker listenerTracker = new CalibrationListenerTracker(listener);
                    binder.linkToDeath(listenerTracker, 0);
                    this.mCalibrationListeners.put(binder, listenerTracker);
                    if (DEBUG) Log.d(TAG, "Registered calibration listener: " + listener);
                }
            }
        }
    }

    public void unregisterGlanceCalibration(IGlanceCalibrationListener listener) {
        if (mContext.checkCallingOrSelfPermission(PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Requires GLANCE_CALIBRATION permission");
        }

        if (listener != null && this.mHUDGlance != null) {
            IBinder binder = listener.asBinder();
            synchronized(this.mCalibrationListeners) {
                CalibrationListenerTracker listenerTracker = this.mCalibrationListeners.remove(binder);
                if (listenerTracker == null) {
                    Log.w(TAG, "Ignoring unregistered calibration listener: " + binder);
                } else {
                    if (DEBUG) Log.d(TAG, "Unregistered listener: " + binder);
                    binder.unlinkToDeath(listenerTracker, 0);
                }
            }
        }
    }

    private final class CalibrationListenerTracker implements IBinder.DeathRecipient {
        private final IGlanceCalibrationListener listener;

        public CalibrationListenerTracker(IGlanceCalibrationListener listener) {
            this.listener = listener;
        }

        public IGlanceCalibrationListener getListener() {
            return this.listener;
        }

        public void binderDied() {
            IHUDGlanceServiceImpl.this.unregisterGlanceCalibration(this.listener);
        }
    }

    private HUDGlance.GlanceEventCallback mCallback = new HUDGlance.GlanceEventCallback() {
        @Override
        public void onEvent(int event) {
            if (DEBUG) Log.d(TAG, "Received event from HUDGlance " + event);
            boolean atDisplay = true;
            boolean removed = true;
            switch (event) {
                case HUDGlanceManager.EVENT_AHEAD_CALIBRATED:
                    atDisplay = false;
                    // Fall through
                case HUDGlanceManager.EVENT_DISPLAY_CALIBRATED:
                    synchronized(IHUDGlanceServiceImpl.this.mCalibrationListeners) {
                        // Go through all registered listeners and report the event.
                        for (Map.Entry<IBinder, CalibrationListenerTracker> entry : IHUDGlanceServiceImpl.this.mCalibrationListeners.entrySet()) {
                            IGlanceCalibrationListener listener = entry.getValue().getListener();
                            try {
                                if (DEBUG) Log.d(TAG, "Notifying calibration listener: " + entry.getKey());
                                listener.onCalibrationEvent(atDisplay);
                            } catch (RemoteException e) {
                                Log.e(TAG, "Failed to update calibration listener: " + entry.getKey(), e);
                                IHUDGlanceServiceImpl.this.unregisterGlanceCalibration(listener);
                            }
                        }
                    }
                    break;
                case HUDGlanceManager.EVENT_GLANCE_AHEAD:
                    atDisplay = false;
                    // fall thru
                case HUDGlanceManager.EVENT_GLANCE_DISPLAY:
                    synchronized(IHUDGlanceServiceImpl.this.mDetectionListeners) {
                        // Go through all registered listeners and report the event.
                        for (Map.Entry<IBinder, DetectionListenerTracker> entry : IHUDGlanceServiceImpl.this.mDetectionListeners.entrySet()) {
                            IGlanceDetectionListener listener = entry.getValue().getListener();
                            try {
                                if (DEBUG) Log.d(TAG, "Notifying detection listener: " + entry.getKey());
                                listener.onDetectEvent(atDisplay);
                            } catch (RemoteException e) {
                                Log.e(TAG, "Failed to update detection listener: " + entry.getKey(), e);
                                IHUDGlanceServiceImpl.this.unregisterGlanceDetection(listener);
                            }
                        }
                    }
                    mLastGlanceEvent = event;
                    break;
                case HUDGlanceManager.EVENT_REMOVED:
                    synchronized(IHUDGlanceServiceImpl.this.mDetectionListeners) {
                        // Go through all registered listeners and report the event.
                        for (Map.Entry<IBinder, DetectionListenerTracker> entry : IHUDGlanceServiceImpl.this.mDetectionListeners.entrySet()) {
                            IGlanceDetectionListener listener = entry.getValue().getListener();
                            try {
                                if (DEBUG) Log.d(TAG, "Notifying detection listener of removal event: " + entry.getKey());
                                listener.onRemovalEvent(removed);
                            } catch (RemoteException e) {
                                Log.e(TAG, "Failed to update detection listener: " + entry.getKey(), e);
                                IHUDGlanceServiceImpl.this.unregisterGlanceDetection(listener);
                            }
                        }
                    }
                    mLastGlanceEvent = event;
                    break;
                default:
                    // Event not reported.
                    break;
            }
        }
    };
}
