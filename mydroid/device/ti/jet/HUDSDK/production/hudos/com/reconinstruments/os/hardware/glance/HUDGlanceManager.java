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
package com.reconinstruments.os.hardware.glance;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog; /* This is to avoid generating events for ourselves */

import java.util.HashMap;

/**
 * Class that controls glance detection on the HUD.
 *
 * To obtain an instance of the glance manager, call HUDGlanceManager.getInstance().
 * {@hide}
 */
public class HUDGlanceManager {

    private static final String TAG = "HUDGlanceManager";
    private static final String REMOTE_SERVICE_NAME = IHUDGlanceService.class.getName();
    private static final boolean DEBUG = false; // change to true to enable debugging

    /**
     * Unknown glance detection event.
     * {@hide}
     */
    public static final int EVENT_UNKNOWN = -1;

    /**
     * Ahead calibrated glance detection event.
     * {@hide}
     */
    public static final int EVENT_AHEAD_CALIBRATED = 0;

    /**
     * Display calibrated glance detection event.
     * {@hide}
     */
    public static final int EVENT_DISPLAY_CALIBRATED = 1;

    /**
     * Ahead glance detection event.
     * {@hide}
     */
    public static final int EVENT_GLANCE_AHEAD = 2;

    /**
     * Display glance detection event.
     * {@hide}
     */
    public static final int EVENT_GLANCE_DISPLAY = 3;

    /**
     * Glance detection stopped event.
     * {@hide}
     */
    public static final int EVENT_GLANCE_STOPPED = 4;

    /**
     * HUD removed detection event.
     * {@hide}
     */
    public static final int EVENT_REMOVED = 5;

    // Singleton instance
    private static HUDGlanceManager sInstance;

    private IHUDGlanceService mService;
    private HashMap<GlanceDetectionListener, DetectionListenerTransport> mDetectionListeners =
        new HashMap<GlanceDetectionListener, DetectionListenerTransport>();
    private HashMap<GlanceCalibrationListener, CalibrationListenerTransport> mCalibrationListeners =
        new HashMap<GlanceCalibrationListener, CalibrationListenerTransport>();

    private DeathListener mDeathRecipient;
    private static final int MAX_NUM_CNX_RETRIES = 10;
    private static final int ONE_SEC = 1000; // One second per retry. So duration of retry is MAX_NUM_CNX_RETRIES * ONE_SEC

    /**
     * Get an instance (Singleton) to the HUDGlanceManager.
     *
     * @return The HUDGlanceManager object to control glance detection on the HUD.
     *
     * @throws IllegalStateException if HUDGlanceManager cannot connect to the IHUDGlanceService.
     *
     * @throws RuntimeException if HUDGlanceManager cannot link a IBinder.DeathRecipient to the IHUDGlanceService.
     * {@hide}
     */
    public static synchronized HUDGlanceManager getInstance() {
        if (Build.MODEL.equalsIgnoreCase("Snow2")) {
            return null;
        }

        if (sInstance == null) {
            sInstance = new HUDGlanceManager();
        }
        return sInstance;

    }

    /**
     * Private constructor for HUDGlanceManager.
     */
    private HUDGlanceManager() {
        Slog.d(TAG, "Connecting to IHUDGlanceService by name [" + REMOTE_SERVICE_NAME + "]");
        mService = IHUDGlanceService.Stub.asInterface(ServiceManager.getService(REMOTE_SERVICE_NAME));
        if (mService == null) {
            throw new IllegalStateException("Failed to find IHUDGlanceService by name [" + REMOTE_SERVICE_NAME + "]");
        }
        mDeathRecipient = new DeathListener(this);
        try {
            mService.asBinder().linkToDeath(mDeathRecipient, 0);
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to link Death Recipient to IHUDGlanceService");
        }
    }

    /**
     * Helper function to reconnect to IHUDGlanceService when IHUDGlanceService died.
     */
    private void reconnect() {
        Slog.d(TAG, "Reconnecting to IHUDGlanceService by name [" + REMOTE_SERVICE_NAME + "]");
        int num = 0;

        // Try a few times in case the IHUDGlanceService does not get restarted right away.
        while (num < MAX_NUM_CNX_RETRIES) {
            mService = IHUDGlanceService.Stub.asInterface(ServiceManager.getService(REMOTE_SERVICE_NAME));
            if (mService != null) {
                if (DEBUG) Slog.d(TAG, "Successfully reconnected to IHUDGlanceService");
                break;
            }
            Slog.w(TAG, "Cannot reconnect to IHUDGlanceService. Retrying...");
            try {
                Thread.sleep(ONE_SEC);
            } catch (InterruptedException e) {
                Slog.e(TAG, "Interrupted while sleeping!");
                break;
            }
            num++;
        }

        if (mService == null) {
            throw new IllegalStateException("Failed to find IHUDGlanceService by name [" + REMOTE_SERVICE_NAME + "]");
        }

        // Since it's a new IHUDGlanceService, need to re-link to Death.
        try {
            mService.asBinder().linkToDeath(mDeathRecipient, 0);
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to link Death Recipient to IHUDGlanceService");
        }

        // Re-register all existing glance detection listeners to the new IHUDGlanceService.
        synchronized (mDetectionListeners) {
            for (DetectionListenerTransport wrapper : mDetectionListeners.values()) {
                try {
                    mService.registerGlanceDetection(wrapper);
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed to reregister");
                }
            }
        }
        // Re-register all existing glance calibration listeners to the new IHUDGlanceService.
        synchronized (mCalibrationListeners) {
            for (CalibrationListenerTransport wrapper : mCalibrationListeners.values()) {
                try {
                    mService.registerGlanceCalibration(wrapper);
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed to reregister");
                }
            }
        }
    }

    /**
     * Initiate ahead glance detection calibration.
     *
     * When the ahead glance calibration step is complete, the glance calibration event will
     * be broadcasted to indicate completion.
     *
     * @throws RuntimeException if HUDGlanceManager cannot start ahead glance detection calibration with
     * the IHUDGlanceService.
     *
     * {@hide}
     */
    public void aheadCalibration() {
        try {
            if (DEBUG) Slog.d(TAG, "Initiate glance calibration (ahead)");
            if (mService.aheadCalibration() < 0) {
                throw new RuntimeException("Ahead calibration failed");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initiate ahead calibration", e);
        }
    }

    /**
     * Initiate display glance detection calibration.
     *
     * When the display glance calibration step is complete, the display calibration event
     * will be broadcasted to indicate completion.
     *
     * @throws RuntimeException if HUDGlanceManager cannot start display glance detection calibration with
     * the IHUDGlanceService.
     * {@hide}
     */
    public void displayCalibration() {
        try {
            if (DEBUG) Slog.d(TAG, "Initiate glance calibration (display)");
            if (mService.displayCalibration() < 0) {
                throw new RuntimeException("Display calibration failed");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initiate display calibration", e);
        }
    }

    /**
     * Register glance detection listener to get glance detection events.
     *
     * @param listener The glance detection listener to register. See {@link com.reconinstruments.os.hardware.glance.GlanceDetectionListener}.
     *
     * @return The last detected glance event, -1 on failure.
     *
     * @throws RuntimeException if HUDGlanceManager cannot register the {@link com.reconinstruments.os.hardware.glance.GlanceDetectionListener}
     * with the IHUDGlanceService.
     */
    public int registerGlanceDetection(GlanceDetectionListener listener) {
        int event = -1;
        if (listener != null) {
            if (mDetectionListeners.containsKey(listener)) {
                Slog.w(TAG, "Already registered: " + listener);
            } else {
                synchronized (mDetectionListeners) {
                    DetectionListenerTransport wrapper = new DetectionListenerTransport(listener);
                    try {
                        if (DEBUG) Slog.d(TAG, "Registering detection remote listener: " + listener);
                        event = mService.registerGlanceDetection(wrapper);
                    } catch (RemoteException e) {
                        throw new RuntimeException("Failed to register: " + listener);
                    }
                    mDetectionListeners.put(listener, wrapper);
                }
            }
        }
        return event;
    }

    /**
     * Unregister glance detection listener.
     *
     * @param listener The glance detection listener to unregister. This is the same {@link com.reconinstruments.os.hardware.glance.GlanceDetectionListener}
     * used to register to glance detection events in {@link #registerGlanceDetection}.
     *
     * @throws RuntimeException if HUDGlanceManager cannot unregister the {@link com.reconinstruments.os.hardware.glance.GlanceDetectionListener}
     * with the IHUDGlanceService.
     */
    public void unregisterGlanceDetection(GlanceDetectionListener listener) {
        if (listener != null) {
            if (!mDetectionListeners.containsKey(listener)) {
                Slog.w(TAG, "Not registered: " + listener);
            } else {
                synchronized (mDetectionListeners) {
                    DetectionListenerTransport wrapper = mDetectionListeners.remove(listener);
                    if (wrapper != null) {
                        try {
                            if (DEBUG) Slog.d(TAG, "Unregistering detection remote listener: " + listener);
                            mService.unregisterGlanceDetection(wrapper);
                        } catch (RemoteException e) {
                            throw new RuntimeException("Failed to unregister: " + listener);
                        }
                    }
                    wrapper.clearListener();
                }
            }
        }
    }

    /**
     * Helper class to copy data back to registered listeners.
     */
    private class DetectionListenerTransport extends IGlanceDetectionListener.Stub {
        private GlanceDetectionListener mListener;

        static private final int DETECT_EVENT = 0;
        static private final int REMOVAL_EVENT = 1;

        public DetectionListenerTransport(GlanceDetectionListener listener) {
            mListener = listener;
        }

        private void clearListener() {
            mListener = null;
        }

        @Override
        public void onDetectEvent(final boolean atDisplay) {
            if (DEBUG) Slog.d(TAG, "onDetectEvent: " + atDisplay);
            Message message = mHandler.obtainMessage(DETECT_EVENT);
            Bundle data = new Bundle();
            data.putBoolean("atDisplay", atDisplay);
            message.setData(data);
            mHandler.sendMessage(message);
        }

        @Override
        public void onRemovalEvent(final boolean removed) {
            if (DEBUG) Slog.d(TAG, "onRemovalEvent: " + removed);
            Message message = mHandler.obtainMessage(REMOVAL_EVENT);
            Bundle data = new Bundle();
            data.putBoolean("removed", removed);
            message.setData(data);
            mHandler.sendMessage(message);
        }

        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (mListener != null) {
                    Bundle data = message.getData();
                    switch (message.what) {
                        case DETECT_EVENT:
                            boolean atDisplay = data.getBoolean("atDisplay");
                            if (DEBUG) Slog.d(TAG, "Notifying local listener: " + atDisplay);
                            mListener.onDetectEvent(atDisplay);
                        break;
                        case REMOVAL_EVENT:
                            boolean removed = data.getBoolean("removed");
                            if (DEBUG) Slog.d(TAG, "Notifying local listener: " + removed);
                            mListener.onRemovalEvent(removed);
                        break;
                        default:
                        break;
                    }
                }
            }
        };
    }

    /**
     * Register glance calibration listener to get glance calibration events.
     *
     * @param listener The glance calibration listener to register. See {@link com.reconinstruments.os.hardware.glance.GlanceCalibrationListener}.
     *
     * @throws RuntimeException if HUDGlanceManager cannot register the {@link com.reconinstruments.os.hardware.glance.GlanceCalibrationListener}
     * with the IHUDGlanceService.
     *
     * {@hide}
     */
    public void registerGlanceCalibration(GlanceCalibrationListener listener) {
        if (listener != null) {
            if (mCalibrationListeners.containsKey(listener)) {
                Slog.w(TAG, "Already registered: " + listener);
            } else {
                synchronized (mCalibrationListeners) {
                    CalibrationListenerTransport wrapper = new CalibrationListenerTransport(listener);
                    try {
                        if (DEBUG) Slog.d(TAG, "Registering calibration remote listener: " + listener);
                        mService.registerGlanceCalibration(wrapper);
                    } catch (RemoteException e) {
                        throw new RuntimeException("Failed to register: " + listener);
                    }
                    mCalibrationListeners.put(listener, wrapper);
                }
            }
        }
    }

    /**
     * Unregister glance calibration listener.
     *
     * @param listener The glance calibration listener to unregister.
     *
     * @throws RuntimeException if HUDGlanceManager cannot unregister the {@link com.reconinstruments.os.hardware.glance.GlanceCalibrationListener}
     * with the IHUDGlanceService.
     *
     * {@hide}
     */
    public void unregisterGlanceCalibration(GlanceCalibrationListener listener) {
        if (listener != null) {
            if (!mCalibrationListeners.containsKey(listener)) {
                Slog.w(TAG, "Not registered: " + listener);
            } else {
                synchronized (mCalibrationListeners) {
                    CalibrationListenerTransport wrapper = mCalibrationListeners.remove(listener);
                    if (wrapper != null) {
                        try {
                            if (DEBUG) Slog.d(TAG, "Unregistering calibration remote listener: " + listener);
                            mService.unregisterGlanceCalibration(wrapper);
                        } catch (RemoteException e) {
                            throw new RuntimeException("Failed to unregister: " + listener);
                        }
                    }
                    wrapper.clearListener();
                }
            }
        }
    }

    /**
     * Helper class to copy data back to registered listeners.
     */
    private class CalibrationListenerTransport extends IGlanceCalibrationListener.Stub {
        private GlanceCalibrationListener mListener;

        public CalibrationListenerTransport(GlanceCalibrationListener listener) {
            mListener = listener;
        }

        private void clearListener() {
            mListener = null;
        }

        @Override
        public void onCalibrationEvent(final boolean atDisplay) {
            if (DEBUG) Slog.d(TAG, "onCalibrationEvent: " + atDisplay);
            Message message = mHandler.obtainMessage();
            Bundle data = new Bundle();
            data.putBoolean("atDisplay", atDisplay);
            message.setData(data);
            mHandler.sendMessage(message);
        }

        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (mListener != null) {
                    Bundle data = message.getData();
                    boolean atDisplay = data.getBoolean("atDisplay");
                    if (DEBUG) Slog.d(TAG, "Notifying local listener: " + atDisplay);
                    mListener.onCalibrationEvent(atDisplay);
                }
            }
        };
    }

    /**
     * Helper class to listen on binder Death.
     */
    private final class DeathListener implements IBinder.DeathRecipient {
        HUDGlanceManager mHUDGlanceManager;
        public DeathListener(HUDGlanceManager mgr) {
            this.mHUDGlanceManager = mgr;
        }

        public void binderDied() {
            Slog.e(TAG, "IHUDGlanceService died");
            // Try to reconnect to the service
            mHUDGlanceManager.reconnect();
        }
    }
}

