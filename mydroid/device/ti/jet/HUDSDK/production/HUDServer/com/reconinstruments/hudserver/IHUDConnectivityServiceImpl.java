package com.reconinstruments.hudserver;

import java.io.IOException;
import java.lang.Override;
import java.util.concurrent.Executors;

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.reconinstruments.os.connectivity.IHUDConnectivity;
import com.reconinstruments.os.connectivity.IHUDConnectivityListener;
import com.reconinstruments.os.connectivity.IHUDConnectivityService;
import com.reconinstruments.os.connectivity.bluetooth.HUDMFiService;
import com.reconinstruments.os.connectivity.bluetooth.HUDSPPService;
import com.reconinstruments.os.connectivity.bluetooth.IHUDBTService;
import com.reconinstruments.os.connectivity.http.HUDHttpBTConnection;
import com.reconinstruments.os.connectivity.http.HUDHttpRequest;
import com.reconinstruments.os.connectivity.http.HUDHttpResponse;
import com.reconinstruments.os.connectivity.http.URLConnectionHUDAdaptor;

public class IHUDConnectivityServiceImpl extends IHUDConnectivityService.Stub implements IHUDConnectivity {
    private static final String TAG = "IHUDConnectivityServiceImpl";

    private static final boolean DEBUG = true;

    private static final int HUD_STATE_DISCONNECTED = 0;
    private static final int HUD_STATE_CONNECTING = 1;
    private static final int HUD_STATE_CONNECTED = 2;

    private static final int TYPE_NULL = -1;
    private static final int TYPE_ANDROID = 0;
    private static final int TYPE_IOS = 1;

    final RemoteCallbackList<IHUDConnectivityListener> mListeners = new RemoteCallbackList<IHUDConnectivityListener>();

    private final HUDHttpBTConnection mHUDHttpBTConnection;
    private IHUDBTService mHUDBTService = null;
    private boolean mPhoneConnected = false;

    private int mHUDState = HUD_STATE_DISCONNECTED;
    private int mLastPairedDeviceType = TYPE_NULL;

    public IHUDConnectivityServiceImpl(Context context) {
        mHUDHttpBTConnection = new HUDHttpBTConnection(context, this, true);
        context.registerReceiver(mHUDStateReceiver, new IntentFilter("HUD_STATE_CHANGED"));
    }

    private BroadcastReceiver mHUDStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();

            // Get the Connection State
            int state = bundle.getInt("state");
            if (state == mHUDState) {
                return;
            }
            Log.d(TAG, "HUDStateReceiver: Connection State change from " + mHUDState + " to " + state);
            mHUDState = state;

            // Only proceed of we are connected to a phone (through the main application)

            switch (state) {
                case HUD_STATE_CONNECTED:
                    //Continue to connect mHUDBTService
                    break;
                case HUD_STATE_CONNECTING:
                    if (mHUDBTService != null) {
                        //TODO: notify mHUDBTService status changed !
                    } else {
                        Log.e(TAG, "HUDBTService is null !");
                    }
                    return;
                case HUD_STATE_DISCONNECTED:
                    if (mHUDBTService != null) {
                        try{
                            mHUDBTService.disconnect();
                        } catch (Exception e) {
                            Log.e(TAG, "Failed in connecting to BT Service", e);
                        }
                    } else {
                        Log.e(TAG, "HUDBTService is null !");
                    }
                    return;
                default:
                    Log.e(TAG, "Unknown HUD STATE: " + mHUDBTService);
                    return;
            }

            // Get the type we of phone we are connected to: iOS / Android
            int lastPairedDeviceType = TYPE_NULL;
            try {
                lastPairedDeviceType = Settings.System.getInt(context.getContentResolver(), "LastPairedDeviceType");
            } catch (SettingNotFoundException e) {
                Log.e(TAG, "HUDStateReceiver: LastPairedDeviceType wasn't found", e);
                return;
            }
            if (lastPairedDeviceType == TYPE_NULL) {
                Log.e(TAG, "HUDStateReceiver: LastPairedDeviceType wasn't found");
                return;
            }

            // Get the device address to connect to
            final String deviceAddress = Settings.System.getString(context.getContentResolver(), "LastPairedDeviceAddress");
            if (deviceAddress == null) {
                Log.e(TAG, "HUDStateReceiver: Failed to get the device address");
                return;
            }
            Log.d(TAG, "Connected to:" + ((lastPairedDeviceType == TYPE_ANDROID) ? "Android" : "iOS") + " Address:" + deviceAddress);

            //TODO: Need to remove the thread sleep. This is just for DEBUGING for now!!!!!
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
            }

            try {
                // If the previous device type is different than what we already establishe - make the correct one
                if (lastPairedDeviceType != mLastPairedDeviceType) {
                    if (lastPairedDeviceType == TYPE_ANDROID) {
                        mHUDBTService = new HUDSPPService(IHUDConnectivityServiceImpl.this);
                    } else {
                        mHUDBTService = new HUDMFiService(IHUDConnectivityServiceImpl.this);
                    }
                    mHUDHttpBTConnection.setBTService(mHUDBTService);
                }

                // Connect to the device's address
                mHUDBTService.connect(deviceAddress);
                mLastPairedDeviceType = lastPairedDeviceType;
            } catch (Exception e) {
                Log.e(TAG, "Failed in connecting to BT Service", e);
            }

        }
    };

    /* TODO Change the above on Receive code to Handler Post Delay 
    private class ConnectTask extends AsyncTask<Void, Void, Void> {

        private int mLastPairedDeviceType = TYPE_NULL;
        private int lastPairedDeviceType = TYPE_NULL;
        private HUDHttpBTConnection mHUDHttpBTConnection = null;
        private IHUDBTService mHUDBTService = null;

        public ConnectTask(int lastPairedDeviceType, int mLastPairedDeviceType) {
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                // If the previous device type is different than what we already establishe - make the correct one
                if (lastPairedDeviceType != mLastPairedDeviceType) {
                    if (lastPairedDeviceType == TYPE_ANDROID) {
                        mHUDBTService = new HUDSPPService(IHUDConnectivityServiceImpl.this);
                    } else {
                        mHUDBTService = new HUDMFiService(IHUDConnectivityServiceImpl.this);
                    }
                    mHUDHttpBTConnection.setBTService(mHUDBTService);
                }

                // Connect to the device's address
                mHUDBTService.connect(deviceAddress);
                mLastPairedDeviceType = lastPairedDeviceType;
            } catch (Exception e) {
                Log.e(TAG, "Failed in connecting to BT Service", e);
            }
            return null;
        }
    };*/

    @Override
    public void register(IHUDConnectivityListener listener) throws RemoteException {
        if (listener != null) {
            synchronized (mListeners) {
                if (DEBUG) Log.d(TAG, "register new listener");
                if (mHUDBTService != null) {
                    listener.onConnectionStateChanged(mHUDBTService.getState().ordinal());
                    listener.onDeviceName(mHUDBTService.getDeviceName());
                }
                listener.onNetworkEvent(
                        mHUDHttpBTConnection.hasLocalWebConnection() ?
                                NetworkEvent.LOCAL_WEB_GAINED.ordinal() : NetworkEvent.LOCAL_WEB_LOST.ordinal(),
                        mHUDHttpBTConnection.hasWebConnection());

                listener.onNetworkEvent(
                        mHUDHttpBTConnection.hasRemoteWebConnection() ?
                                NetworkEvent.REMOTE_WEB_GAINED.ordinal() : NetworkEvent.REMOTE_WEB_LOST.ordinal(),
                        mHUDHttpBTConnection.hasWebConnection());

                mListeners.register(listener);
            }
        }
    }

    @Override
    public void unregister(IHUDConnectivityListener listener) throws RemoteException {
        if (listener != null) {
            synchronized (mListeners) {
                if (DEBUG) Log.d(TAG, "unregister a listener");
                mListeners.unregister(listener);
            }
        }
    }

    @Override
    public HUDHttpResponse sendWebRequest(HUDHttpRequest request) throws RemoteException {
        if (DEBUG) Log.d(TAG, "sendWebRequest:" + request.getURL());

        try {
            if (mHUDHttpBTConnection.hasLocalWebConnection()) {
                return URLConnectionHUDAdaptor.sendWebRequest(request);
            }

            if (isPhoneConnected()) {
                HUDHttpResponse response = mHUDHttpBTConnection.sendWebRequest(request);
                if (DEBUG) Log.d(TAG, "sendWebRequest(response):" + request.getURL());
                return response;
            } else {
                throw new NetworkErrorException("Not connected to a smartphone");
            }
        } catch (Exception e) {
            throw new RemoteException(e.toString());
        }
    }

    @Override
    public boolean isPhoneConnected() throws RemoteException {
        return mPhoneConnected;
    }

    private void setPhoneConnected(boolean phoneConnected) throws InterruptedException {
        if(DEBUG) Log.d(TAG, "setPhoneConnected() : mPhoneConnected:" + mPhoneConnected + " phoneConnected:" + phoneConnected);

        if (mPhoneConnected != phoneConnected) {
            mPhoneConnected = phoneConnected;
            //TODO Review if there is a better way of updating remote web connection 
            new Thread() {
                @Override
                public void run() {
                    try {
                        mHUDHttpBTConnection.updateRemoteWebConnection();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    @Override
    public boolean hasWebConnection() throws RemoteException {
        return mHUDHttpBTConnection.hasWebConnection();
    }

    @Override
    public void onDeviceName(String deviceName) {
        int i = mListeners.beginBroadcast();
        while (i > 0) {
            i--;
            try {
                mListeners.getBroadcastItem(i).onDeviceName(deviceName);
            } catch (RemoteException e) {
                Log.e(TAG, "Couldn't send a onDeviceName to listener #" + i, e);
            }
        }
        mListeners.finishBroadcast();
    }

    @Override
    public void onConnectionStateChanged(ConnectionState state) {
        int i = mListeners.beginBroadcast();
        while (i > 0) {
            i--;
            try {
                mListeners.getBroadcastItem(i).onConnectionStateChanged(state.ordinal());
            } catch (RemoteException e) {
                Log.e(TAG, "Couldn't send a onConnectionStateChanged to listener #" + i, e);
            }
        }
        mListeners.finishBroadcast();

        // TODO : move this to an internal command handler which receives commands (such as state) and send it to the other side
        try {
            setPhoneConnected(state == ConnectionState.CONNECTED);
        } catch (InterruptedException e) {
            Log.e(TAG, "Couldn't change Connection State", e);
        }
    }

    @Override
    public void onNetworkEvent(NetworkEvent networkEvent, boolean hasNetworkAccess) {
        if (DEBUG)
            Log.d(TAG, "onNetworkEvent " + networkEvent + " hasNetworkAccess=" + hasNetworkAccess);
        int i = mListeners.beginBroadcast();
        while (i > 0) {
            i--;
            try {
                mListeners.getBroadcastItem(i).onNetworkEvent(networkEvent.ordinal(), hasNetworkAccess);
            } catch (RemoteException e) {
                Log.e(TAG, "Couldn't send a onNetworkEvent to listener #" + i, e);
            }
        }
        mListeners.finishBroadcast();
    }
}
