package com.reconinstruments.hudconnectivitylib;

import java.io.IOException;
import java.lang.ref.WeakReference;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.reconinstruments.hudconnectivitylib.http.HUDHttpRequest;
import com.reconinstruments.hudconnectivitylib.http.HUDHttpResponse;

public class HUDConnectivityManager implements IHUDConnectivity {
    private static final String TAG = "HUDConnectivityManager";

    private static final boolean DEBUG = true;

    private IHUDConnectivity mHUDConnectivity = null;
    private ConnectivityHandler mHandler = null;
    private IHUDConnectivityConnection mHUDConnectivityConnection = null;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_BT_STATE_CHANGE = 1;
    public static final int MESSAGE_NETWORK_EVENT = 2;
    public static final int MESSAGE_READ = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;

    // Key names sent to the Handler
    private static final String DEVICE_NAME = "device_name";

    private final boolean mHUDConnected = false;
    private boolean mIsHUD = true;

    public HUDConnectivityManager(Context context, IHUDConnectivity hudConnectivity, boolean isHUD) throws Exception {
        mHUDConnectivity = hudConnectivity;
        mIsHUD = isHUD;

        mHandler = new ConnectivityHandler(mHUDConnectivity);

        if (mIsHUD) {
            mHUDConnectivityConnection = new HUDConnectivityServiceConnection(context, this);
        } else {
            mHUDConnectivityConnection = new HUDConnectivityPhoneConnection(context, this);
        }
        mHUDConnectivityConnection.start();
    }

    public void DEBUG_ONLY_stop() {
        try {
            mHUDConnectivityConnection.DEBUG_ONLY_stop();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    /**
     * Creates a bluetooth connection between two devices
     * 
     * @param address MAC address of the remote device
     * @throws IOException
     * @throws RemoteException
     */
    public void connect(String address) throws IOException, RemoteException {
        mHUDConnectivityConnection.connect(address);
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

    static class ConnectivityHandler extends Handler {
        private final WeakReference<IHUDConnectivity> mWeakHUDConnectivity;

        ConnectivityHandler(IHUDConnectivity hudConnectivity) {
            mWeakHUDConnectivity = new WeakReference<IHUDConnectivity>(hudConnectivity);
        }

        @Override
        public void handleMessage(Message msg) {
            IHUDConnectivity hudConnectivity = mWeakHUDConnectivity.get();
            if (hudConnectivity == null) {
                Log.d(TAG, "hudConnectivity is null in ConnectivityHandler");
                return;
            }

            switch (msg.what) {
                case MESSAGE_BT_STATE_CHANGE:
                    if (DEBUG) Log.i(TAG, "MESSAGE_BT_STATE_CHANGE: " + msg.arg1);
                    if (hudConnectivity != null) {
                        hudConnectivity.onConnectionStateChanged(ConnectionState.values()[msg.arg1]);
                    }
                    break;
                case MESSAGE_NETWORK_EVENT:
                    if (DEBUG) Log.i(TAG, "MESSAGE_NETWORK_EVENT: " + msg.arg1);
                    if (hudConnectivity != null) {
                        hudConnectivity.onNetworkEvent(NetworkEvent.values()[msg.arg1], (msg.arg2 == 1));
                    }
                    break;
                case MESSAGE_READ:
                    break;
                case MESSAGE_DEVICE_NAME:
                    if (DEBUG)
                        Log.i(TAG, "MESSAGE_DEVICE_NAME: " + msg.getData().getString(DEVICE_NAME));
                    if (hudConnectivity != null) {
                        hudConnectivity.onDeviceName(msg.getData().getString(DEVICE_NAME));
                    }
                    break;
            }
        }
    }

    @Override
    public void onDeviceName(String deviceName) {
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, deviceName);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    @Override
    public void onConnectionStateChanged(ConnectionState state) {
        mHandler.obtainMessage(MESSAGE_BT_STATE_CHANGE, state.ordinal(), -1).sendToTarget();
    }

    @Override
    public void onNetworkEvent(NetworkEvent networkEvent, boolean hasNetworkAccess) {
        mHandler.obtainMessage(
                HUDConnectivityManager.MESSAGE_NETWORK_EVENT,
                networkEvent.ordinal(), hasWebConnection() ? 1 : 0).sendToTarget();

    }
}
