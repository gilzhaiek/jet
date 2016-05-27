package com.reconinstruments.hudconnectivityservice;

import java.io.IOException;

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import com.reconinstruments.hudconnectivitylib.IHUDConnectivity;
import com.reconinstruments.hudconnectivitylib.IHUDConnectivityListener;
import com.reconinstruments.hudconnectivitylib.IHUDConnectivityService;
import com.reconinstruments.hudconnectivitylib.bluetooth.HUDMFiService;
import com.reconinstruments.hudconnectivitylib.bluetooth.HUDSPPService;
import com.reconinstruments.hudconnectivitylib.bluetooth.IHUDBTService;
import com.reconinstruments.hudconnectivitylib.http.HUDHttpBTConnection;
import com.reconinstruments.hudconnectivitylib.http.HUDHttpRequest;
import com.reconinstruments.hudconnectivitylib.http.HUDHttpResponse;
import com.reconinstruments.hudconnectivitylib.http.URLConnectionHUDAdaptor;

public class IHUDConnectivityServiceImpl extends IHUDConnectivityService.Stub implements IHUDConnectivity {
    private static final String TAG = "IHUDConnectivityServiceImpl";

    private static final boolean DEBUG = true;

    final RemoteCallbackList<IHUDConnectivityListener> mListeners = new RemoteCallbackList<IHUDConnectivityListener>();

    private HUDHttpBTConnection mHUDHttpBTConnection = null;
    private IHUDBTService mHUDBTService = null;

    private boolean mPhoneConnected = false;

    public IHUDConnectivityServiceImpl(Context context) throws Exception {
        mHUDHttpBTConnection = new HUDHttpBTConnection(context, this, true);

        // TODO: Only based on if we are connected to iOS or Android we provide the correct HUDSPPService or HUDMFIService
        //mHUDBTService = new HUDSPPService(this);
        mHUDBTService = new HUDMFiService(this);
        mHUDHttpBTConnection.setBTService(mHUDBTService);
    }

    public void start() throws RemoteException {
        try {
            mHUDHttpBTConnection.start();
            mHUDBTService.startListening();
        } catch (Exception e) {
            throw new RemoteException(e.toString());
        }
    }

    public void stop() throws IOException {
        mHUDHttpBTConnection.stop();
        mHUDBTService.stopListening();
    }


    @Override
    public void register(IHUDConnectivityListener listener) throws RemoteException {
        if (listener != null) {
            synchronized (mListeners) {
                if (DEBUG) Log.d(TAG, "register new listener");

                listener.onNetworkEvent(
                        mHUDHttpBTConnection.hasLocalWebConnection() ?
                                NetworkEvent.LOCAL_WEB_GAINED.ordinal() : NetworkEvent.LOCAL_WEB_LOST.ordinal(),
                                mHUDHttpBTConnection.hasWebConnection());

                listener.onNetworkEvent(
                        mHUDHttpBTConnection.hasRemoteWebConnection() ?
                                NetworkEvent.REMOTE_WEB_GAINED.ordinal() : NetworkEvent.REMOTE_WEB_LOST.ordinal(),
                                mHUDHttpBTConnection.hasWebConnection());

                listener.onConnectionStateChanged(mHUDBTService.getState().ordinal());

                listener.onDeviceName(mHUDBTService.getDeviceName());

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
    public void connect(String address) throws RemoteException {
        try {
            mHUDBTService.connect(address);
        } catch (IOException e) {
            throw new RemoteException(e.toString());
        }
    }

    @Override
    public boolean isPhoneConnected() throws RemoteException {
        return mPhoneConnected;
    }

    private void setPhoneConnected(boolean phoneConnected) throws InterruptedException {
        if (mPhoneConnected != phoneConnected) {
            mPhoneConnected = phoneConnected;
            //TODO Review if there is a better way of updating remote web connection 
            new Thread(){
                @Override
                public void run(){
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

    @Override
    public void DEBUG_ONLY_start() throws RemoteException {
        start();
    }

    @Override
    public void DEBUG_ONLY_stop() throws RemoteException {
        try {
            stop();
        } catch (IOException e) {
            throw new RemoteException(e.toString());
        }
    }
}
