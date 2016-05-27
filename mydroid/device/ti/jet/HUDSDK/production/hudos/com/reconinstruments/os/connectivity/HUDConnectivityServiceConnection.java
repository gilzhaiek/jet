package com.reconinstruments.os.connectivity;

import java.io.IOException;

import android.os.ServiceManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.reconinstruments.os.connectivity.http.HUDHttpRequest;
import com.reconinstruments.os.connectivity.http.HUDHttpResponse;

/** {@hide}*/
public class HUDConnectivityServiceConnection extends IHUDConnectivityListener.Stub implements IHUDConnectivityConnection {
    private static final String TAG = "HUDConnectivityManager";

    private static final boolean DEBUG = true;

    private static final String REMOTE_SERVICE_NAME = IHUDConnectivityService.class.getName();

    private IHUDConnectivityService mService; // reference to our service

    private final IHUDConnectivity mHUDConnectivity;

    /** {@hide} */
    public HUDConnectivityServiceConnection(IHUDConnectivity hudConnectivity) {
        mHUDConnectivity = hudConnectivity;

        if(this == null) {
            Log.e(TAG,"This is null");
        }

        Log.w(TAG,"Connecting to " + REMOTE_SERVICE_NAME);
        this.mService = IHUDConnectivityService.Stub.asInterface(ServiceManager.getService(REMOTE_SERVICE_NAME));

        try {

            this.mService.register(this);

        } catch (RemoteException e) {
            Log.e(TAG, "Failed to register listener into the service", e);
            this.mService = null;
        }
    }

    @Override
    public HUDHttpResponse sendWebRequest(HUDHttpRequest request) throws RemoteException {
        if(this.mService != null) {
            return this.mService.sendWebRequest(request);
        }
        return null;
    }

    @Override
    public boolean hasWebConnection() throws RemoteException {
        if(this.mService != null) {
            return this.mService.hasWebConnection();
        }
        return false;
    }

    /** {@hide} */
    @Override
    public void onDeviceName(String deviceName) throws RemoteException {
        mHUDConnectivity.onDeviceName(deviceName);
    }

    /** {@hide} */
    @Override
    public void onConnectionStateChanged(int connectionState) throws RemoteException {
        mHUDConnectivity.onConnectionStateChanged(IHUDConnectivity.ConnectionState.values()[connectionState]); 
    }

    /** {@hide} */
    @Override
    public void onNetworkEvent(int networkEvent, boolean hasNetworkAccess) throws RemoteException {
        mHUDConnectivity.onNetworkEvent(IHUDConnectivity.NetworkEvent.values()[networkEvent], hasNetworkAccess);
    }

    /** {@hide} */
    @Override
    public void start() {
        Log.w(TAG, "start: Not implemented on the Service Connection");
    }

    /** {@hide} */
    @Override
    public void stop() {
        Log.w(TAG, "stop: Not implemented on the Service Connection");
    }
}
