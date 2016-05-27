package com.reconinstruments.hudconnectivitylib;

public interface IHUDConnectivity {
    public enum ConnectionState {
        LISTENING,
        CONNECTED,
        CONNECTING,
        DISCONNECTED,
    }

    public enum NetworkEvent {
        LOCAL_WEB_GAINED,
        LOCAL_WEB_LOST,
        REMOTE_WEB_GAINED,
        REMOTE_WEB_LOST,
    }

    public void onDeviceName(String deviceName);

    public void onConnectionStateChanged(ConnectionState state);

    public void onNetworkEvent(NetworkEvent networkEvent, boolean hasNetworkAccess);
}
