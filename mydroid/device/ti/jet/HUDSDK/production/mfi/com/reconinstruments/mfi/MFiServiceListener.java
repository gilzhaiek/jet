package com.reconinstruments.mfi;

import android.os.RemoteException;

public interface MFiServiceListener {
    public void onConnected(String deviceName, String address, boolean success) throws RemoteException;

    public void onDisconnected() throws RemoteException;

    public void onSessionOpened(int sessionID, int protocolIndex, boolean success) throws RemoteException;

    public void onSessionClosed() throws RemoteException;

    public void onReceivedData(int sessionID, byte[] sessionData) throws RemoteException;

    public void onSessionDataConfirmationEvent(int sessionID, int packetID, int dataConfirmationStatus) throws RemoteException;

    public void onPacketID(int packetID);
}

