package com.reconinstruments.hudconnectivity;

import com.reconinstruments.hudconnectivity.bluetooth.HUDBTService.ConnectionState;

public interface IHUDConnectivity {
    enum NetworkEvent {
        LOCAL_WEB_GAINED,
        LOCAL_WEB_LOST,
        REMOTE_WEB_GAINED,
        REMOTE_WEB_LOST,
    }

    public void onDeviceName(String deviceName);

    public void onConnectionStateChanged(ConnectionState state);

    public void onNetworkEvent(NetworkEvent networkEvent, boolean hasNetworkAccess);
}
