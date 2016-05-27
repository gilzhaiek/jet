package com.reconinstruments.os.hardware.sensors;

import android.os.ServiceManager;
import java.util.HashMap;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

/**
 * HUDHeadingManager lets you access the virtual heading sensors.
 * To obtain an instance of this class, call {@link com.reconinstruments.os.HUDOS#getHUDService} with the argument {@link com.reconinstruments.os.HUDOS#HUD_HEADING_SERVICE}.
 *
 * Note that the system will not disable the service automatically. Always make sure to disable the service when you don't need.
 */
public class HUDHeadingManager {
    private static final String TAG = "HUDHeadingManager";
    private static final boolean DEBUG = false;

    private static final String REMOTE_SERVICE_NAME = IHUDHeadingService.class.getName();

    private static final String YAW_BUNDLE_KEY = "Yaw";
    private static final String PITCH_BUNDLE_KEY = "Pitch";
    private static final String ROLL_BUNDLE_KEY = "Roll";

    // Singleton instance
    private static HUDHeadingManager sInstance;

    private HashMap<HeadLocationListener, HeadingLocationListenerTransport> mListeners =
        new HashMap<HeadLocationListener, HeadingLocationListenerTransport>();

    private final IHUDHeadingService mService;

    /**
     * Get an instance (Singleton) to the HUDHeadingManager.
     *
     * @return The HUDHeadingManager object to control and manage the heading location
     * on the HUD.
     *
     * {@hide}
     */
    public static synchronized HUDHeadingManager getInstance() {
        if (sInstance == null) {
            sInstance = new HUDHeadingManager();
        }
        return sInstance;
    }

    private HUDHeadingManager() {
        Log.d(TAG, "Connecting to IHUDHeadingService by name [" + REMOTE_SERVICE_NAME + "]");

        mService = IHUDHeadingService.Stub.asInterface(ServiceManager.getService(REMOTE_SERVICE_NAME));
        if (mService == null) {
            throw new IllegalStateException("Failed to find IHUDHeadingService by name [" + REMOTE_SERVICE_NAME + "]");
        }
    }

    /**
     * Register to heading location. Applications will receive events via the listener.
     *
     * @param listener A {@link com.reconinstruments.os.hardware.sensors.HeadLocationListener} listener
     * to receive heading events.
     *
     * @throws RuntimeException if failure to register the {@link com.reconinstruments.os.hardware.sensors.HeadLocationListener} listener.
     */
    public void register(HeadLocationListener listener) {
        if (listener != null) {
            if (mListeners.containsKey(listener)) {
                Log.w(TAG, "Already registered: " + listener);
            } else {
                synchronized (mListeners) {
                    HeadingLocationListenerTransport wrapper = new HeadingLocationListenerTransport(listener);
                    try {
                        if (DEBUG) Log.d(TAG, "Registering heading location listener: " + listener);
                        mService.register(wrapper);
                    } catch (RemoteException e) {
                        throw new RuntimeException("Failed to register " + listener, e);
                    }
                    mListeners.put(listener, wrapper);
                }
            }
        }
    }

    /**
     * Unregister to heading location. The listener should be the same as the one used in {@link #register}.
     *
     * @param listener A {@link com.reconinstruments.os.hardware.sensors.HeadLocationListener} listener to unregister.
     *
     * @throws RuntimeException if failure to unregister the {@link com.reconinstruments.os.hardware.sensors.HeadLocationListener} listener.
     */
    public void unregister(HeadLocationListener listener) {
        if (listener != null) {
            if (!mListeners.containsKey(listener)) {
                Log.w(TAG, "Not registered: " + listener);
            } else {
                synchronized (mListeners) {
                    HeadingLocationListenerTransport wrapper = mListeners.remove(listener);
                    if (wrapper != null) {
                        try {
                            if (DEBUG) Log.d(TAG, "Unregistering heading location listener: " + listener);
                            mService.unregister(wrapper);
                        } catch (RemoteException e) {
                            throw new RuntimeException("Failed to register " + listener, e);
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
    private class HeadingLocationListenerTransport extends IHeadingServiceCallback.Stub {
        private HeadLocationListener mListener;

        public HeadingLocationListenerTransport(HeadLocationListener listener) {
            mListener = listener;
        }

        private void clearListener() {
            mListener = null;
        }

        @Override
        public void onHeadLocation(float yaw, float pitch, float roll) throws RemoteException {
            if (DEBUG) Log.d(TAG, "onHeadLocation: yaw=" + yaw + " pitch=" + pitch + " roll="+roll);

            Message message = mHandler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putFloat(YAW_BUNDLE_KEY, yaw);
            bundle.putFloat(PITCH_BUNDLE_KEY, pitch);
            bundle.putFloat(ROLL_BUNDLE_KEY, roll);
            message.setData(bundle);
            mHandler.sendMessage(message);
        }

        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (mListener != null) {
                    Bundle data = message.getData();
                    float yaw = data.getFloat(YAW_BUNDLE_KEY);
                    float pitch = data.getFloat(PITCH_BUNDLE_KEY);
                    float roll = data.getFloat(ROLL_BUNDLE_KEY);
                    if (DEBUG) Log.d(TAG, "Notifying location listener: " + yaw + ", " + pitch + ", " + roll);
                    mListener.onHeadLocation(yaw, pitch, roll);
                }
            }
        };
    }
}
