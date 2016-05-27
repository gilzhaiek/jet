package com.reconinstruments.os.connectivity;

import java.io.IOException;
import java.lang.ref.WeakReference;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;

import com.reconinstruments.os.connectivity.http.HUDHttpRequest;
import com.reconinstruments.os.connectivity.http.HUDHttpResponse;

/**
 * HUDConnectivityManager has the state of network connectivity and responsible for web requests.
 * It also notifies applications when network connectivity changes.
 * Get an instance of this class by calling {@link com.reconinstruments.os.HUDOS#getHUDService} with the argument {@link com.reconinstruments.os.HUDOS#HUD_CONNECTIVITY_SERVICE}.
 */
public class HUDConnectivityManager implements IHUDConnectivity {
    private static final String TAG = "HUDConnectivityManager";

    private static final boolean DEBUG = true;

    private ConnectivityHandler mHandler = null;
    private IHUDConnectivityConnection mHUDConnectivityConnection = null;

    // Message types sent from the BluetoothChatService Handler
    /**{@hide}*/
    public static final int MESSAGE_BT_STATE_CHANGE = 1;
    /**{@hide}*/
    public static final int MESSAGE_NETWORK_EVENT = 2;
    /**{@hide}*/
    public static final int MESSAGE_READ = 3;
    /**{@hide}*/
    public static final int MESSAGE_DEVICE_NAME = 4;

    // Key names sent to the Handler
    private static final String DEVICE_NAME = "device_name";

    private String mDeviceName = "";
    private ConnectionState mConnectionState = ConnectionState.DISCONNECTED;
    private boolean mHasLocalWeb = false;
    private boolean mHasRemoteWeb = false;
    private final boolean mHUDConnected = false;

    /** {@hide} */
    public HUDConnectivityManager() {
        mHandler = new ConnectivityHandler();
        Log.d(TAG, "Initialize HUDConnectivity Manager");
        if(mHandler == null) {
            Log.e(TAG, "HUDConnectivityManager() mHandler is null !!");
        }
    }

    /** {@hide} */
    public void initOnHUD() {
        mHUDConnectivityConnection = new HUDConnectivityServiceConnection(this);
    }

    /** {@hide} */
    public void initOnPhone(Context context, int socketCount) throws Exception {
        mHandler = new ConnectivityHandler();
        mHUDConnectivityConnection = new HUDConnectivityPhoneConnection(context, this, socketCount);
        mHUDConnectivityConnection.start();
    }

    /** {@hide} */
    public void stop() throws Exception{
        mHUDConnectivityConnection.stop();
    }

    /**
     * Register {@link com.reconinstruments.os.connectivity.IHUDConnectivity} to get latest connectivity changes.
     * Informations such as device name, connection states, and network status will be notified.
     * For more details please see {@link com.reconinstruments.os.connectivity.IHUDConnectivity}.
     *
     * @param hudConnectivity an interface to listen to connectivity updates
     */
    public void register(IHUDConnectivity hudConnectivity) {
        if(mHandler == null) {
            Log.e(TAG, "register(hudConnectivity) mHandler is null !!");
        }

        if (mHandler.register(hudConnectivity)) {
            hudConnectivity.onDeviceName(mDeviceName);
            hudConnectivity.onConnectionStateChanged(mConnectionState);
            hudConnectivity.onNetworkEvent(hasLocalWeb() ? NetworkEvent.LOCAL_WEB_GAINED : NetworkEvent.LOCAL_WEB_LOST, hasWebConnection());
            hudConnectivity.onNetworkEvent(hasRemoteWeb() ? NetworkEvent.REMOTE_WEB_GAINED : NetworkEvent.REMOTE_WEB_LOST, hasWebConnection());
        }

    }

    /**
     * Unregister {@link com.reconinstruments.os.connectivity.IHUDConnectivity} to stop listening to connectivity status updates.
     *
     * @param hudConnectivity this should be the same hudConnectivity interface which used during registration at {@link #register}
     */
    public void unregister(IHUDConnectivity hudConnectivity) {
        mHandler.unregister(hudConnectivity);
    }

    /**
     * Indicates if a HUD is connected to a smart phone
     * This doesn't mean that the connected smart phone is connected to the web<br>
     *
     * @return true if smart phone is connected.
     */
    public boolean isHUDConnected() {
        return mHUDConnected;
    }

    /**
     * Indicates if there is a path to the web (locally or through a smart phone)
     *
     * @return true if the device can reach the web
     * @throws RemoteException
     */
    public boolean hasWebConnection() {
        try {
            return mHUDConnectivityConnection.hasWebConnection();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get hasWebConnection", e);
            return false;
        }
    }

    /**
     * Sends HUDHttpRequest to the network
     *
     * @param request HUDHttpRequest
     * @return a HUDHttpResponse from the request
     * @throws Exception
     */
    public HUDHttpResponse sendWebRequest(HUDHttpRequest request) throws Exception {
        if (DEBUG) Log.d(TAG, "sendWebRequest " + request.getURL());
        return mHUDConnectivityConnection.sendWebRequest(request);
    }

    /** {@hide} */
    static class ConnectivityHandler extends Handler {
        private ArrayList<WeakReference<IHUDConnectivity>> mWeakHUDConnectivities = new ArrayList<WeakReference<IHUDConnectivity>>();

        private WeakReference<IHUDConnectivity> get(IHUDConnectivity hudConnectivity) {
            synchronized (mWeakHUDConnectivities) {
                for(int i = 0; i < mWeakHUDConnectivities.size(); i++) {
                    if(mWeakHUDConnectivities.get(i).get() == hudConnectivity) {
                        return mWeakHUDConnectivities.get(i);
                    }
                }
            }
            return null;
        }

        public boolean register(IHUDConnectivity hudConnectivity) {
            if(hudConnectivity == null) {
                return false;
            }

            WeakReference<IHUDConnectivity> weakHUDConnectivity = get(hudConnectivity);
            if(weakHUDConnectivity != null) {
                Log.w(TAG, "IHUDConnectivity already registered");
                return false;
            }

            synchronized (mWeakHUDConnectivities) {
                mWeakHUDConnectivities.add(new WeakReference<IHUDConnectivity>(hudConnectivity));
            }
            return true;
        }

        public void unregister(IHUDConnectivity hudConnectivity) {
            if(hudConnectivity == null) {
                return;
            }

            WeakReference<IHUDConnectivity> weakHUDConnectivity = get(hudConnectivity);
            if(weakHUDConnectivity == null) {
                Log.w(TAG, "IHUDConnectivity is not registered");
                return;
            }

            synchronized (mWeakHUDConnectivities) {
                mWeakHUDConnectivities.remove(weakHUDConnectivity);
            }
        }

        @Override
        public void handleMessage(Message msg) {
            if (mWeakHUDConnectivities.size() == 0) {
                Log.d(TAG, "No IHUDConnectivity registered in ConnectivityHandler");
                return;
            }

            switch (msg.what) {
                case MESSAGE_BT_STATE_CHANGE:
                    if (DEBUG) Log.i(TAG, "MESSAGE_BT_STATE_CHANGE: " + msg.arg1);
                    synchronized (mWeakHUDConnectivities) {
                        for(int i = 0; i < mWeakHUDConnectivities.size(); i++) {
                            IHUDConnectivity hudConnectivity = mWeakHUDConnectivities.get(i).get();
                            if (hudConnectivity != null) {
                                hudConnectivity.onConnectionStateChanged(ConnectionState.values()[msg.arg1]);
                            } else {
                                mWeakHUDConnectivities.remove(i);
                                i--; // Don't want to skip the next value
                            }
                        }
                    }
                    break;
                case MESSAGE_NETWORK_EVENT:
                    if (DEBUG) Log.i(TAG, "MESSAGE_NETWORK_EVENT: " + msg.arg1);
                    synchronized (mWeakHUDConnectivities) {
                        for(int i = 0; i < mWeakHUDConnectivities.size(); i++) {
                            IHUDConnectivity hudConnectivity = mWeakHUDConnectivities.get(i).get();
                            if (hudConnectivity != null) {
                                hudConnectivity.onNetworkEvent(NetworkEvent.values()[msg.arg1], (msg.arg2 == 1));
                            } else {
                                mWeakHUDConnectivities.remove(i);
                                i--; // Don't want to skip the next value
                            }
                        }
                    }
                    break;
                case MESSAGE_READ:
                    break;
                case MESSAGE_DEVICE_NAME:
                    if (DEBUG) Log.i(TAG, "MESSAGE_DEVICE_NAME: " + msg.getData().getString(DEVICE_NAME));
                    synchronized (mWeakHUDConnectivities) {
                        for(int i = 0; i < mWeakHUDConnectivities.size(); i++) {
                            IHUDConnectivity hudConnectivity = mWeakHUDConnectivities.get(i).get();
                            if (hudConnectivity != null) {
                                hudConnectivity.onDeviceName(msg.getData().getString(DEVICE_NAME));
                            } else {
                                mWeakHUDConnectivities.remove(i);
                                i--; // Don't want to skip the next value
                            }
                        }
                    }
                    break;
            }
        }
    }

    /**
     * Get remote connected device name.
     * @return device name or NULL if no device connected
     **/
    public String getDeviceName() {
        return mDeviceName;
    }

    /**
     * Get connection state between HUD and remote device.
     * @return connection state, see {@link com.reconinstruments.os.connectivity.IHUDConnectivity#ConnectionState} for more detail
     */
    public ConnectionState getConnectionState() {
        return mConnectionState;
    }

    /**
     * Check HUD has local web connectivity
     * @return true if there is local web connectivity, otherwise false
     */
    public boolean hasLocalWeb() {
        return mHasLocalWeb;
    }

    /**
     * Check if remote device has web connectivity
     * @return true if remote device has web connectivity, otherwise false
     */
    public boolean hasRemoteWeb() {
        return mHasRemoteWeb;
    }

    /** {@hide} */
    @Override
    public void onDeviceName(String deviceName) {
        if(DEBUG) Log.d(TAG, "onDeviceName:" + deviceName);
        mDeviceName = deviceName;
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, deviceName);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /** {@hide} */
    @Override
    public void onConnectionStateChanged(ConnectionState state) {
        if(DEBUG) Log.d(TAG, "onConnectionStateChanged:" + state);
        mConnectionState = state;
        mHandler.obtainMessage(MESSAGE_BT_STATE_CHANGE, state.ordinal(), -1).sendToTarget();
    }

    /** {@hide} */
    @Override
    public void onNetworkEvent(NetworkEvent networkEvent, boolean hasNetworkAccess) {
        if(DEBUG) Log.d(TAG, "onNetworkEvent:" + networkEvent + " hasNetworkAccess:" + hasNetworkAccess);
        switch (networkEvent) {
            case LOCAL_WEB_GAINED  : mHasLocalWeb = true; break;
            case LOCAL_WEB_LOST    : mHasLocalWeb = false; break;
            case REMOTE_WEB_GAINED : mHasRemoteWeb = true; break;
            case REMOTE_WEB_LOST   : mHasRemoteWeb = false; break;
        }

        mHandler.obtainMessage(
                HUDConnectivityManager.MESSAGE_NETWORK_EVENT,
                networkEvent.ordinal(), hasWebConnection() ? 1 : 0).sendToTarget();

    }
}
