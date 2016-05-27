/*
 * Copyright (C) 2015 Recon Instruments
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
package com.reconinstruments.os.hardware.motion;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog; /* This is to avoid generating events for ourselves */

import java.util.HashMap;

/**
 * Class that controls motion detection on the HUD.
 *
 * To obtain an instance of the activity motion manager, call HUDActivityMotionManager.getInstance().
 *
 * {@hide}
 */
public class HUDActivityMotionManager {

    private static final String TAG = "HUDActivityMotionManager";
    private static final String REMOTE_SERVICE_NAME = IHUDActivityMotionService.class.getName();
    private static final boolean DEBUG = false; // change to true to enable debugging

    /**
     * Activity motion detection type not supported.
     */
    public static final int MOTION_DETECT_NOT_SUPPORTED     = -1;

    /**
     * Running activity motion detection type.
     */
    public static final int MOTION_DETECT_RUNNING           = 2;

    /**
     * Cycling activity motion detection type.
     */
    public static final int MOTION_DETECT_CYCLING           = 1;

    /**
     * Snow activity motion detection type.
     * {@hide}
     */
    public static final int MOTION_DETECT_SNOW              = 0;

    /**
     * Invalid activity motion detection event.
     */
    public static final int EVENT_INVALID                   = -1;

    /**
     * Stationary activty motion detection event.
     */
    public static final int EVENT_STATIONARY                = 0;

    /**
     * In motion activity motion detection event.
     */
    public static final int EVENT_IN_MOTION                 = 1;

    // Singleton instance
    private static HUDActivityMotionManager sInstance;
    private int mType;

    private IHUDActivityMotionService mService;
    private HashMap<ActivityMotionDetectionListener, DetectionListenerTransport> mDetectionListeners =
        new HashMap<ActivityMotionDetectionListener, DetectionListenerTransport>();

    private DeathListener mDeathRecipient;
    private static final int MAX_NUM_CNX_RETRIES = 10;
    private static final int ONE_SEC = 1000; // One second per retry. So duration of retry is MAX_NUM_CNX_RETRIES * ONE_SEC

    /**
     * Get an instance (Singleton) to the HUDActivityMotionManager.
     *
     * @return The HUDActivityMotionManager object to control motion detection on the HUD.
     * {@hide}
     */
    public static synchronized HUDActivityMotionManager getInstance() {
        if (sInstance == null) {
            sInstance = new HUDActivityMotionManager();
        }
        return sInstance;

    }

    /**
     * Private constructor for HUDActivityMotionManager
     */
    private HUDActivityMotionManager() {
        Slog.d(TAG, "Connecting to IHUDActivityMotionService by name [" + REMOTE_SERVICE_NAME + "]");
        mService = IHUDActivityMotionService.Stub.asInterface(ServiceManager.getService(REMOTE_SERVICE_NAME));
        if (mService == null) {
            throw new IllegalStateException("Failed to find IHUDActivityMotionService by name [" + REMOTE_SERVICE_NAME + "]");
        }
        mDeathRecipient = new DeathListener(this);
        try {
            mService.asBinder().linkToDeath(mDeathRecipient, 0);
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to link Death Recipient to IHUDActivityMotionService");
        }
        mType = MOTION_DETECT_NOT_SUPPORTED;
    }

    /**
     * Helper function to reconnect to IHUDActivityMotionService when IHUDActivityMotionService died.
     */
    private void reconnect() {
        Slog.d(TAG, "Reconnecting to IHUDActivityMotionService by name [" + REMOTE_SERVICE_NAME + "]");
        int num = 0;

        // Try a few times in case the IHUDActivityMotionService does not get restarted right away.
        while (num < MAX_NUM_CNX_RETRIES) {
            mService = IHUDActivityMotionService.Stub.asInterface(ServiceManager.getService(REMOTE_SERVICE_NAME));
            if (mService != null) {
                if (DEBUG) Slog.d(TAG, "Successfully reconnected to IHUDActivityMotionService");
                break;
            }
            Slog.w(TAG, "Cannot reconnect to IHUDActivityMotionService. Retrying...");
            try {
                Thread.sleep(ONE_SEC);
            } catch (InterruptedException e) {
                Slog.e(TAG, "Interrupted while sleeping!");
                break;
            }
            num++;
        }

        if (mService == null) {
            throw new IllegalStateException("Failed to find IHUDActivityMotionService by name [" + REMOTE_SERVICE_NAME + "]");
        }

        // Since it's a new IHUDActivityMotionService, need to re-link to Death.
        try {
            mService.asBinder().linkToDeath(mDeathRecipient, 0);
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to link Death Recipient to IHUDActivityMotionService");
        }

        // Re-register all existing motion detection listeners to the new IHUDActivityMotionService.
        synchronized (mDetectionListeners) {
            for (DetectionListenerTransport wrapper : mDetectionListeners.values()) {
                try {
                    mService.registerActivityMotionDetection(wrapper, mType);
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed to reregister");
                }
            }
        }
    }

    /**
     * Helper function to determine if the type is supported.
     */
    private boolean isSupported(int type) {
        boolean ret = false;
        switch (type) {
            case MOTION_DETECT_CYCLING:
            case MOTION_DETECT_RUNNING:
                ret = true;
                break;
        }
        if (DEBUG) Slog.d(TAG, "isSupported: type: " + type + " is supported: " + ret);
        return ret;
    }

    /**
     * Register activity motion detection listener to get motion detection events.
     * Only one type of activity motion detection can be registered at a time.
     *
     * @param listener The motion detection listener to register.
     * @param type The type of activity.
     *
     * @return 1 if successful in registering activity motino detection listener; 0 otherwise.
     *
     * @throws RuntimeException if failure to register the listener.
     */
    public int registerActivityMotionDetection(ActivityMotionDetectionListener listener, int type) {
        int rc = 0;

        // Sanity check
        if (!isSupported(type)) {
            Slog.e(TAG, "Invalid activity type");
            return rc;
        }

        // As of now, we only allow one type of activity to be registered at a time. Also, first time
        // around, mType will be MOTION_DETECT_NOT_SUPPORTED, so no need to check against type.
        if (mType == MOTION_DETECT_NOT_SUPPORTED || mType == type) {
            if (listener != null) {
                if (mDetectionListeners.containsKey(listener)) {
                    Slog.w(TAG, "Already registered: " + listener);
                } else {
                    synchronized (mDetectionListeners) {
                        DetectionListenerTransport wrapper = new DetectionListenerTransport(listener);
                        try {
                            if (DEBUG) Slog.d(TAG, "Registering detection remote listener: " + listener);
                            rc = mService.registerActivityMotionDetection(wrapper, type);
                        } catch (RemoteException e) {
                            throw new RuntimeException("Failed to register: " + listener);
                        }
                        mDetectionListeners.put(listener, wrapper);
                        if (mDetectionListeners.size() == 1) {
                            mType = type;
                        }
                    }
                }
            }
        }
        return rc;
    }

    /**
     * Unregister motion detection listener.
     *
     * @param listener The motion detection listener to unregister.
     *
     * @throws RuntimeException if failure to unregister the listener.
     */
    public void unregisterActivityMotionDetection(ActivityMotionDetectionListener listener) {
        if (listener != null) {
            if (!mDetectionListeners.containsKey(listener)) {
                Slog.w(TAG, "Not registered: " + listener);
            } else {
                synchronized (mDetectionListeners) {
                    DetectionListenerTransport wrapper = mDetectionListeners.remove(listener);
                    if (wrapper != null) {
                        try {
                            if (DEBUG) Slog.d(TAG, "Unregistering detection remote listener: " + listener);
                            mService.unregisterActivityMotionDetection(wrapper);

                            // If it's the last one in our listener list, then reset our type to allow for
                            // a different kind of activity.
                            if (mDetectionListeners.isEmpty()) {
                                mType = MOTION_DETECT_NOT_SUPPORTED;
                            }
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
     * Retrieve most recent activity motion detected event.
     *
     * @return HUDActivityMotionDetection.EVENT_STATIONARY if the most recent motion detected
     *         event within the context of the current activity the user is stationary.
     *         Otherwise, HUDActivityMotionDetection.EVENT_IN_MOTION is returned.
     *
     * @throws RuntimeException if failure to retrieve the most recent activity motion detected event.
     */
    public int getActivityMotionDetectedEvent() {
        try {
            if (DEBUG) Slog.d(TAG, "Retrieving most recent activity motion detected event.");
            return mService.getActivityMotionDetectedEvent();
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to retrieve most recent activity motion detected event.");
        }
    }


    /**
     * Helper class to copy data back to registered listeners.
     */
    private class DetectionListenerTransport extends IActivityMotionDetectionListener.Stub {
        private ActivityMotionDetectionListener mListener;

        static private final int DETECT_EVENT = 0;
        static private final int REMOVAL_EVENT = 1;

        public DetectionListenerTransport(ActivityMotionDetectionListener listener) {
            mListener = listener;
        }

        private void clearListener() {
            mListener = null;
        }

        @Override
        public void onDetectEvent(final boolean inMotion, final int type) {
            if (DEBUG) Slog.d(TAG, "onDetectEvent: " + inMotion + " type: " + type);
            Message message = mHandler.obtainMessage();
            Bundle data = new Bundle();
            data.putBoolean("inMotion", inMotion);
            data.putInt("type", type);
            message.setData(data);
            mHandler.sendMessage(message);
        }

        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (mListener != null) {
                    Bundle data = message.getData();
                    boolean inMotion = data.getBoolean("inMotion");
                    int type = data.getInt("type");
                    if (DEBUG) Slog.d(TAG, "Notifying local listener: " + inMotion);
                    mListener.onDetectEvent(inMotion, type);
                }
            }
        };
    }

    /**
     * Helper class to listen on binder Death.
     */
    private final class DeathListener implements IBinder.DeathRecipient {
        HUDActivityMotionManager mHUDActivityMotionManager;
        public DeathListener(HUDActivityMotionManager mgr) {
            this.mHUDActivityMotionManager = mgr;
        }

        public void binderDied() {
            Slog.e(TAG, "IHUDActivityMotionService died");
            // Try to reconnect to the service
            mHUDActivityMotionManager.reconnect();
        }
    }
}

