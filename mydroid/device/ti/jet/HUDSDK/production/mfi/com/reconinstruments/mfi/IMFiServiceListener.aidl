// IMFiServiceListener.aidl
package com.reconinstruments.mfi;

oneway interface IMFiServiceListener {
    void onConnected(String deviceName, String address, boolean success);
    void onDisconnected();
    void onSessionOpened(int sessionID, int protocolIndex, boolean success);
    void onSessionClosed();
    void onReceivedData(int sessionID, in byte[] sessionData);
    void onSessionDataConfirmationEvent(int sessionID, int packetID, int dataConfirmationStatus);
    void onPacketID(int packetID);
}
