package com.reconinstruments.mfi;

import java.io.IOException;

import android.os.ServiceManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class MFiServiceManager extends IMFiServiceListener.Stub {
    private static final String TAG = "MFiServiceManager";

    private static final boolean DEBUG = true;

    private static final String REMOTE_SERVICE_NAME = IMFiService.class.getName();

    private IMFiService mService; // reference to our service

    private MFiServiceListener mListener = null;

    public MFiServiceManager() {
        if (this == null) {
            Log.e(TAG, "This is null");
        }

        Log.w(TAG, "Connecting to " + REMOTE_SERVICE_NAME);
        this.mService = IMFiService.Stub.asInterface(ServiceManager.getService(REMOTE_SERVICE_NAME));
    }

    private void getService() {
        if (this.mService == null) {
            this.mService = IMFiService.Stub.asInterface(ServiceManager.getService(REMOTE_SERVICE_NAME)); // Try to reconnect
        }
    }

    public void start(String listenerUUID, MFiServiceListener listener) throws Exception {
        getService();

        try {
            this.mService.start(listenerUUID, this);
            mListener = listener;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to start and register listener into the service", e);
            this.mService = null;
            throw new Exception("Failed to start and register listener into the service");
        }
    }

    public String getDeviceName() throws RemoteException {
        return mService.getDeviceName();
    }

    public String getLastConnectedBTAddress() throws RemoteException {
        return mService.getLastConnectedBTAddress();
    }

    public boolean isConnectingOrConnected() throws RemoteException {
        return mService.isConnectingOrConnected();
    }

    public int getSessionID(int protocolIndex) throws RemoteException {
        return mService.getSessionID(protocolIndex);
    }

    public void stop(String listenerUUID) throws Exception {
        mListener = null;
        try {
            this.mService.stop(listenerUUID, this);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to stop and unregister listener from the service", e);
            this.mService = null;
            throw new Exception("Failed to stop and unregister listener from the service");
        }
    }

    public void connect(String address) throws RemoteException {
        this.mService.connect(address);
    }

    public void disconnect() throws RemoteException {
        this.mService.disconnect();
    }

    public void writeData(int sessionID, byte[] data) throws RemoteException {
        this.mService.writeData(sessionID, data);
    }

    @Override
    public void onConnected(String deviceName, String address, boolean success) throws RemoteException {
        if (mListener != null) {
            mListener.onConnected(deviceName, address, success);
        }
    }

    @Override
    public void onDisconnected() throws RemoteException {
        if (mListener != null) {
            mListener.onDisconnected();
        }
    }

    @Override
    public void onSessionOpened(int sessionID, int protocolIndex, boolean success) throws RemoteException {
        if (mListener != null) {
            mListener.onSessionOpened(sessionID, protocolIndex, success);
        }
    }

    @Override
    public void onSessionClosed() throws RemoteException {
        if (mListener != null) {
            mListener.onSessionClosed();
        }
    }

    @Override
    public void onReceivedData(int sessionID, byte[] sessionData) throws RemoteException {
        if (mListener != null) {
            mListener.onReceivedData(sessionID, sessionData);
        }
    }


    @Override
    public void onSessionDataConfirmationEvent(int sessionID, int packetID, int dataConfirmationStatus) throws RemoteException {
        if (mListener != null) {
            mListener.onSessionDataConfirmationEvent(sessionID, packetID, dataConfirmationStatus);
        }
    }


    @Override
    public void onPacketID(int packetID) {
        if (mListener != null) {
            mListener.onPacketID(packetID);
        }
    }
}
