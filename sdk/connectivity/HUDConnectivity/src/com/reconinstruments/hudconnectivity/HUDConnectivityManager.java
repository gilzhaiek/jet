package com.reconinstruments.hudconnectivity;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.UUID;

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.reconinstruments.hudconnectivity.bluetooth.HUDBTService;
import com.reconinstruments.hudconnectivity.bluetooth.HUDBTService.ConnectionState;
import com.reconinstruments.hudconnectivity.http.HUDHttpBTConnection;
import com.reconinstruments.hudconnectivity.http.HUDHttpRequest;
import com.reconinstruments.hudconnectivity.http.HUDHttpResponse;
import com.reconinstruments.hudconnectivity.http.URLConnectionHUDAdaptor;

public class HUDConnectivityManager implements IHUDConnectivity {
    private static final String TAG = "HUDConnectivityManager";

    private static final boolean DEBUG = true;

    private HUDHttpBTConnection mHUDHttpBTConnection = null;
    private IHUDConnectivity mHUDConnectivity = null;
    private HUDBTService mHUDBTService = null;
    private ConnectivityHandler mHandler = null;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_BT_STATE_CHANGE = 1;
    public static final int MESSAGE_NETWORK_EVENT = 2;
    public static final int MESSAGE_READ = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;

    // Key names sent to the Handler
    public static final String DEVICE_NAME = "device_name";

    private boolean mHUDConnected = false;
    private boolean mIsHUD = true;

    /**
     * @param context
     * @param runningOn       This class runs on a HUD or a smart phone: HUDConnectivityManager.RUNNING_ON_XXX
     * @param appUniqueName   A Unique Name for your application, for example: com.mycompany.myapp
     * @param appUUID         Unique UUID for the application that uses this service
     * @param hudConnectivity an interface to receiver IHUDConnectivity call backs
     * @throws Exception
     */
    public HUDConnectivityManager(Context context, IHUDConnectivity hudConnectivity, boolean isHUD, boolean forceBTEnable, String appUniqueName, UUID hudRequestUUID, UUID phoneRequestUUID) throws Exception {
        mHUDConnectivity = hudConnectivity;
        mIsHUD = isHUD;

        mHandler = new ConnectivityHandler(mHUDConnectivity);
        mHUDBTService = new HUDBTService(this, isHUD, forceBTEnable, appUniqueName, hudRequestUUID, phoneRequestUUID);
        mHUDHttpBTConnection = new HUDHttpBTConnection(context, this, isHUD, mHUDBTService);
    }

    public void start() throws IOException {
        mHUDHttpBTConnection.start();
        mHUDBTService.startListening();
    }

    /**
     * Open a connection to already an paired device
     */
    public void connect() {
        // TODO: extract the address from the global settings and connect to that
    }

    public void connect(String address) throws IOException {
        mHUDBTService.connect(address);
    }

    public void disconnect() {

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
     * @hide
     */
    private void setHUDConnected(boolean hudConnected) {
        if (mHUDConnected != hudConnected) {
            mHUDConnected = hudConnected;
            if (mIsHUD) {
                mHUDHttpBTConnection.updateRemoteWebConnection();
            }
        }
    }

    /**
     * Indicates if there is a path to the web (locally or through a smart phone)
     *
     * @return true if the device can reach the web
     */
    public boolean hasWebConnection() {
        return mHUDHttpBTConnection.hasWebConnection();
    }

    /**
     * Indicates of the connected smart phone (or tablet) is connected to the cloud
     *
     * @return true if the connected smart phone is connected to the cloud
     */
    public boolean hasRemoteWebConnection() {
        return mHUDHttpBTConnection.hasRemoteWebConnection() && isHUDConnected();
    }

    public HUDHttpResponse sendWebRequest(HUDHttpRequest request) throws Exception {
        if (DEBUG) Log.d(TAG, "sendWebRequest");

        if (mHUDHttpBTConnection.hasLocalWebConnection()) {
            return URLConnectionHUDAdaptor.sendWebRequest(request);
        }

        if (!mIsHUD) {
            throw new NetworkErrorException("Phone is not connected to the internet");
        }

        if (isHUDConnected()) {
            return mHUDHttpBTConnection.sendWebRequest(request);
        } else {
            throw new NetworkErrorException("Not connected to a smartphone");
        }
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

    ;

    @Override
    public void onDeviceName(String deviceName) {
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, deviceName);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    @Override
    public void onConnectionStateChanged(HUDBTService.ConnectionState state) {
        mHandler.obtainMessage(MESSAGE_BT_STATE_CHANGE, state.ordinal(), -1).sendToTarget();
        setHUDConnected(state == ConnectionState.CONNECTED);
    }

    @Override
    public void onNetworkEvent(NetworkEvent networkEvent, boolean hasNetworkAccess) {
        mHandler.obtainMessage(
                HUDConnectivityManager.MESSAGE_NETWORK_EVENT,
                networkEvent.ordinal(), hasWebConnection() ? 1 : 0).sendToTarget();

    }
}
