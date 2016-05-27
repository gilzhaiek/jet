// IMfiService.aidl
package com.reconinstruments.mfi;

import com.reconinstruments.mfi.IMFiServiceListener;

interface IMFiService {
    void start(String listenerUUID, in IMFiServiceListener listener);
    void stop(String listenerUUID, in IMFiServiceListener listener);
    void connect(String address);
    void disconnect();
    oneway void writeData(int sessionID, in byte[] data);
    String getDeviceName();
    String getLastConnectedBTAddress();
    boolean isConnectingOrConnected();
    int getSessionID(int protocolIndex);
}

