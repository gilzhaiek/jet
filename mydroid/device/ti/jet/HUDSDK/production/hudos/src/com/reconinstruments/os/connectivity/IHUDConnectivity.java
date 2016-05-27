package com.reconinstruments.os.connectivity;

/**
 * An interface to notify client on any network and connectivity changes
 */
public interface IHUDConnectivity {

    /**
     * The connection state which describe the connection status between HUD and remote device.
     */
    public enum ConnectionState {
        /**
         * HUD's bluetooth has come to a complete stop
         */
        STOPPED,
        /**
         * HUD is waiting for remote device to connect
         */
        LISTENING,
        /**
         * HUD and remote device connection is connected
         */
        CONNECTED,
        /**
         * HUD and remote device is in the process of connection
         */
        CONNECTING,
        /**
         * HUD and remote device connection is disconnected
         */
        DISCONNECTED,
    }

    /**
     * The network state which describe the network status.
     */
    public enum NetworkEvent {
        /**
         * Local device gained it's web connectivity
         */
        LOCAL_WEB_GAINED,
        /**
         * Local device lost it's web connectivity
         */
        LOCAL_WEB_LOST,
        /**
         * Remote device gained it's web connectivity
         */
        REMOTE_WEB_GAINED,
        /**
         * Remote device lost it's web connectivity
         */
        REMOTE_WEB_LOST,
    }

    /**
     * Provides connected remote device name.
     * @param deviceName is the device name of the remote device, deviceName is null if disconnected
     */
    public void onDeviceName(String deviceName);

    /**
     * Provides the connection state change between HUD and remote device
     * @param state of the current connection, see {@link com.reconinstruments.os.connectivity.IHUDConnectivity.ConnectionState} for more detail
     */
    public void onConnectionStateChanged(ConnectionState state);

    /**
     * Changes on network status.
     * @param networkEvent a network event, see {@link com.reconinstruments.os.connectivity.IHUDConnectivity.NetworkEvent} for more detail
     * @param hasNetworkAccess web accessbility condition
     */
    public void onNetworkEvent(NetworkEvent networkEvent, boolean hasNetworkAccess);
}
