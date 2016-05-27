package com.reconinstruments.hudconnectivitylib;

import java.io.IOException;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.reconinstruments.hudconnectivitylib.http.HUDHttpRequest;
import com.reconinstruments.hudconnectivitylib.http.HUDHttpResponse;

public class HUDConnectivityServiceConnection extends IHUDConnectivityListener.Stub implements IHUDConnectivityConnection, ServiceConnection {
    private static final String TAG = "HUDConnectivityManager";

    private static final boolean DEBUG = true;

    private IHUDConnectivityService mService; // reference to our service

    private final IHUDConnectivity mHUDConnectivity;

    public HUDConnectivityServiceConnection(Context context, IHUDConnectivity hudConnectivity) {
        mService = null;

        mHUDConnectivity = hudConnectivity;

        Intent serviceIntent = new Intent();
        serviceIntent.setComponent(new ComponentName("com.reconinstruments.hudconnectivityservice",
                "com.reconinstruments.hudconnectivityservice.HUDConnectivityService"));

        if (!context.bindService(serviceIntent, this, Context.BIND_AUTO_CREATE)) {
            Log.w(TAG, "Failed to bind to service");
        }
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        if (DEBUG) Log.d(TAG, "onServiceConnected()'ed to " + name);
        this.mService = IHUDConnectivityService.Stub.asInterface(service);
        try {
            this.mService.register(this);
            start();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to register listener into the service", e);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        if (DEBUG) Log.d(TAG, "onServiceDisconnected()'ed to " + name);
        // our IFibonacciService service is no longer connected
        this.mService = null;
    }

    @Override
    public void start() {
        try {
            if (this.mService != null) {
                this.mService.DEBUG_ONLY_start();
            }
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void connect(String address) throws RemoteException {
        this.mService.connect(address);
    }

    @Override
    public HUDHttpResponse sendWebRequest(HUDHttpRequest request) throws RemoteException {
        return this.mService.sendWebRequest(request);
    }

    @Override
    public boolean hasWebConnection() throws RemoteException {
        return this.mService.hasWebConnection();
    }

    @Override
    public void onDeviceName(String deviceName) throws RemoteException {
        mHUDConnectivity.onDeviceName(deviceName);

    }

    @Override
    public void onConnectionStateChanged(int connectionState) throws RemoteException {
        mHUDConnectivity.onConnectionStateChanged(IHUDConnectivity.ConnectionState.values()[connectionState]);
    }

    @Override
    public void onNetworkEvent(int networkEvent, boolean hasNetworkAccess) throws RemoteException {
        mHUDConnectivity.onNetworkEvent(IHUDConnectivity.NetworkEvent.values()[networkEvent], hasNetworkAccess);
    }


    @Override
    public void DEBUG_ONLY_stop() throws IOException {
        try {
            mService.DEBUG_ONLY_stop();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
