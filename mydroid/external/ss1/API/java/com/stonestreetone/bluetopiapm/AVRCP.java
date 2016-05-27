package com.stonestreetone.bluetopiapm;

import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Java wrapper for the Remote Control Manager API (AVRCP 1.3+) for Stonestreet One
 * Bluetooth Protocol Stack Platform Manager.
 */
public class AVRCP {
    
    protected final EventCallback callbackHandler;
    
    private boolean               disposed;

    private long                  localData;
    
    protected static final int TYPE_CONTROLLER = 1;
    protected static final int TYPE_TARGET = 2;
    
    private static final byte MAX_VOLUME = 0x7F;
    
    protected int type;

    static {
        System.loadLibrary("btpmj");
        initClassNative();
    }
    
    protected AVRCP(int type, EventCallback eventCallback) throws ServerNotReachableException, RemoteControlAlreadyRegisteredException {
        try {
            initObjectNative(type);
            this.disposed = false;
            this.type = type;
            this.callbackHandler = eventCallback;
        }
        catch(ServerNotReachableException e) {
            dispose();
            throw e;
        }
    }
    
    /**
     * Disposes of all resources allocated by this Manager object.
     */
    public void dispose() {
        if(!disposed) {
            cleanupObjectNative();
            disposed = true;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if(!disposed) {
                System.err.println("Error: Possible memory leak: Manager object of type '" + this.getClass().getName() + "' not 'dispose()'ed correctly.");
                dispose();
            }
        } finally {
            super.finalize();
        }
    }
    
    /*
     * Common AVRCP shared API Commands
     */
    
    /**
     * Responds to a request from a remote device to connect to a Local Server.
     *
     * A successful return value does not necessarily indicate that the port has
     * been successfully opened. A {@link EventCallback#audioStreamConnectedEvent}
     * call will notify of this status.
     *
     * @param requestType
     *            The type of the request.
     * @param remoteDeviceAddress
     *            The {@link BluetoothAddress} of the remote device attempting
     *            to connect.
     * @param acceptConnection
     *            {@code true} if the connection should be accepted.
     * @return Zero if successful, or a negative return error code if there was
     *         an error.
     */
    public int connectionRequestResponse(BluetoothAddress remoteDeviceAddress, boolean acceptConnection) {
        byte[] address = remoteDeviceAddress.internalByteArray();

        return connectionRequestResponseNative(address[0], address[1], address[2], address[3], address[4], address[5], acceptConnection);
    }
    
    /**
     * Connects to a remote Remote Control Device.
     *
     * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
     * @param connectionFlags
     *            Bit flags which control whether encryption and/or
     *            authentication is used.
     * @param waitForConnection
     *            If true, this call will block until the connection attempt
     *            finishes.
     * @return Zero if successful, or a negative return error code if there was
     *         an error issuing the connection attempt. If
     *         {@code waitForConnection} is {@code true} and the connection
     *         request was successfully issued, a positive return value
     *         indicates the reason why the connection failed.
     */
    public int connectRemoteControl(BluetoothAddress remoteDeviceAddress, EnumSet<ConnectionFlags> connectionFlags, boolean waitForConnection) {
        int flags;
        System.out.println(remoteDeviceAddress);
        byte[] address = remoteDeviceAddress.internalByteArray();

        flags = 0;
        flags |= (connectionFlags.contains(ConnectionFlags.REQUIRE_AUTHENTICATION) ? CONNECTION_FLAGS_REQUIRE_AUTHENTICATION : 0);
        flags |= (connectionFlags.contains(ConnectionFlags.REQUIRE_ENCRYPTION)     ? CONNECTION_FLAGS_REQUIRE_ENCRYPTION     : 0);

        return connectRemoteControlNative(address[0], address[1], address[2], address[3], address[4], address[5], flags, waitForConnection);
    }
    
    /**
     * Modifies the incoming connection flags for Audio Manager connections.
     *
     * @param connectionFlags
     *            The desired {@link IncomingConnectionFlags}.
     *
     * @return Zero if successful, or a negative return error code if there was
     *         an error.
     */
    public int changeIncomingConnectionFlags(EnumSet<IncomingConnectionFlags> connectionFlags) {
        int flags = 0;

        if(connectionFlags == null)
            throw new IllegalArgumentException("connectionFlags must be defined");

        flags |= (connectionFlags.contains(IncomingConnectionFlags.REQUIRE_AUTHORIZATION)  ? INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION  : 0);
        flags |= (connectionFlags.contains(IncomingConnectionFlags.REQUIRE_AUTHENTICATION) ? INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION : 0);
        flags |= (connectionFlags.contains(IncomingConnectionFlags.REQUIRE_ENCRYPTION)     ? INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION     : 0);

        return changeIncomingConnectionFlagsNative(flags);
    }
    
    /**
     * Disconnects a currently connected audio stream.
     *
     * @param remoteDeviceAddress
     *          The {@link BluetoothAddress} of the remote device.
     *
     * @return Zero if successful, or a negative return error code if there was
     *         an error.
     */
    public int disconnectRemoteControl(BluetoothAddress remoteDeviceAddress) {
        byte[] address = remoteDeviceAddress.internalByteArray();
        return disconnectRemoteConrolNative(address[0], address[1], address[2], address[3], address[4], address[5]);
    }

    /**
     * Queries the currently connected Remote Control Devices.
     *
     * @return The list of connected Bluetooth Addresses. The list will
     *         be an empty list if no connections are active, and {@code NULL}
     *         if there is an error.
     */
    public BluetoothAddress[] queryConnectedRemoteControlDevices() {
        int                  result;
        BluetoothAddress[][] addressList;

        addressList    = new BluetoothAddress[1][0];

        result = queryConnectedRemoteControlDevicesNative(addressList);

        if(result < 0)
            addressList[0] = null;

        return addressList[0];
    }
    
    /*
     * Common AVRCP native callback handlers
     */
    
    protected void remoteControlConnectionRequestEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.incomingConnectionRequestEvent(this, new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    protected void remoteControlConnectedEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.remoteControlConnectedEvent(this, new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }


    protected void remoteControlConnectionStatusEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int connectionStatus) {
        ConnectionStatus status;
        EventCallback    callbackHandler = this.callbackHandler;

        switch(connectionStatus) {
        case CONNECTION_STATUS_SUCCESS:
            status = ConnectionStatus.SUCCESS;
            break;
        case CONNECTION_STATUS_FAILURE_TIMEOUT:
            status = ConnectionStatus.FAILURE_TIMEOUT;
            break;
        case CONNECTION_STATUS_FAILURE_REFUSED:
            status = ConnectionStatus.FAILURE_REFUSED;
            break;
        case CONNECTION_STATUS_FAILURE_SECURITY:
            status = ConnectionStatus.FAILURE_SECURITY;
            break;
        case CONNECTION_STATUS_FAILURE_DEVICE_POWER_OFF:
            status = ConnectionStatus.FAILURE_DEVICE_POWER_OFF;
            break;
        case CONNECTION_STATUS_FAILURE_UNKNOWN:
        default:
            status = ConnectionStatus.FAILURE_UNKNOWN;
            break;
        }

        if(callbackHandler != null) {
            try {
                callbackHandler.remoteControlConnectionStatusEvent(this, new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), status);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void remoteControlDisconnectedEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int disconnectReason) {
        RemoteControlDisconnectReason reason;
        EventCallback                 callbackHandler = this.callbackHandler;

        switch(disconnectReason) {
        case REMOTE_CONTROL_DISCONNECT_REASON_DISCONNECT:
            reason = RemoteControlDisconnectReason.DISCONNECT;
            break;
        case REMOTE_CONTROL_DISCONNECT_REASON_LINK_LOSS:
            reason = RemoteControlDisconnectReason.LINK_LOSS;
            break;
        case REMOTE_CONTROL_DISCONNECT_REASON_TIMEOUT:
        default:
            reason = RemoteControlDisconnectReason.TIMEOUT;
            break;
        }

        if(callbackHandler != null) {
            try {
                callbackHandler.remoteControlDisconnectedEvent(this, new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), reason);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    
    /*
     * AVRCP Controller native callback handlers
     */
    protected void remoteControlPassThroughCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, byte subunitType, byte subunitID, int operationID, boolean stateFlag, byte operationData[]) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }
    
    protected void vendorDependentCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, byte subunitType, byte subunitID, byte ID0, byte ID1, byte ID2, byte[] data) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }

    protected void groupNavigationCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, boolean stateFlag) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }

    protected void getCompanyIDCapabilitiesCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, byte[] ids) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }

    protected void getEventsSupportedCapabilitiesCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, int eventIDs) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }

    protected void listPlayerApplicationSettingAttributesCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, byte[] attributeIDs) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }
 
    protected void listPlayerApplicationSettingValuesCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, byte[] values) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }
  
    protected void getCurrentPlayerApplicationSettingValueCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, byte[] ids, byte[] values) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }

    protected void setPlayerApplicationSettingValueCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }

    protected void getPlayerApplicationSettingAttributeTextCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, byte[] ids, short[] characterSets, byte[][] textData) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }

    protected void getPlayerApplicationSettingValueTextCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, byte[] ids, short[] characterSets, byte[][] textData) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }

    protected void informDisplayableCharacterSetCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }
 
    protected void informBatteryStatusCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }

    protected void getElementAttributesCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, int[] eventIDs, short[] characterSets, byte[][] elementData) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }

    protected void getPlayStatusCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, int length, int position, int playStatus) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }

    protected void playbackStatusChangedNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, int playbackStatus) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }

    protected void trackChangedNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, long identifier) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }

    protected void trackReachedEndNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }

    protected void trackReachedStartNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }

    protected void playbackPositionChangedNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, int position) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }
    
    protected void batteryStatusChangedNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, int batteryStatus) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }

    protected void systemStatusChangedNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, int systemStatus) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }

    protected void volumeChangeNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, byte volume) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }
    
    protected void playerApplicationSettingChangedNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, byte[] attributeIDs, byte[] valueIDs) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }

    protected void setAbsoluteVolumeCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, byte volume) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }
    
    protected void commandRejectResponseEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, int errorCode) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }
    
    protected void commandFailureEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, int status) {
        throw new UnsupportedOperationException("Event received which is not supported by this AVRCP Manager type");
    }
    
    protected interface EventCallback {
        /**
         * Invoked when a remote device attempts to connect to the local
         * server and authorization is required from the controlling
         * Manager.
         * <p>
         * Respond to this event by calling {@link AVRCP#connectionRequestResponse}.
         *
         * @param manager The manager with which this callback is associated. Will either be an instance of
         *                {@link RemoteControlControllerManager} or {@link RemoteControlTargetManager}.
         * @param remoteDeviceAddress
         *            The {@link BluetoothAddress} of the device requesting a
         *            connection.
         */
        public void incomingConnectionRequestEvent(AVRCP manager, BluetoothAddress remoteDeviceAddress);



        /**
         * Invoked when a remote control session is established.
         *
         * @param manager The manager with which this callback is associated. Will either be an instance of
         *                {@link RemoteControlControllerManager} or {@link RemoteControlTargetManager}.
         * @param remoteDeviceAddress
         *            The {@link BluetoothAddress} of the remote device.
         */
        public void remoteControlConnectedEvent(AVRCP manager, BluetoothAddress remoteDeviceAddress);

        /**
         * Invoked to notify the client about the status of an outgoing remote
         * control session connection.
         * <p>
         * This method is <i>only</i> invoked when the remote control session
         * connection was attempted via the {@link AVRCP#connectRemoteControl}
         * method. This is in contrast to the
         * {@link #remoteControlConnectedEvent}, which applies to both incoming
         * and outgoing connections.
         *
         * @param manager The manager with which this callback is associated. Will either be an instance of
         *                {@link RemoteControlControllerManager} or {@link RemoteControlTargetManager}.
         * @param remoteDeviceAddress
         *            The {@link BluetoothAddress} of the remote device.
         * @param connectionStatus
         *            A {@link ConnectionStatus} value corresponding to the
         *            status of the connection.
         */
        public void remoteControlConnectionStatusEvent(AVRCP manager, BluetoothAddress remoteDeviceAddress, ConnectionStatus connectionStatus);

        /**
         * Invoked when a Remote Control session is disconnected.
         *
         * @param manager The manager with which this callback is associated. Will either be an instance of
         *                {@link RemoteControlControllerManager} or {@link RemoteControlTargetManager}.
         * @param remoteDeviceAddress
         *            The {@link BluetoothAddress} of the remote device.
         * @param disconnectReason
         *            The reason for the disconnection.
         */
        public void remoteControlDisconnectedEvent(AVRCP manager, BluetoothAddress remoteDeviceAddress, RemoteControlDisconnectReason disconnectReason);
    }
    
    /**
     * Management connection to the local Remote Control Module in
     * the Controller role.
     */
    public static final class RemoteControlControllerManager extends AVRCP {
        
        /**
         * Create a new management connection to the local Remote Control Controller module.
         * <p>
         * Note that only one manager can be registered at a given time.
         * 
         * @param eventCallback
         *          Receiver for events sent by the local server.
         * @throws ServerNotReachableException
         * @throws RemoteControlAlreadyRegisteredException 
         */
        public RemoteControlControllerManager(ControllerEventCallback eventCallback) throws ServerNotReachableException, RemoteControlAlreadyRegisteredException {
            super(TYPE_CONTROLLER, eventCallback);
        }
        
        /**
         * Sends a Remote Control Passthrough Command
         * 
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device
         * @param responseTimeout The length of time (in milliseconds) to wait for a response.
         * @param operationID {@link RemoteControlPassThroughOperationID} of the command.
         * @param stateFlag Indicates the state of the button sending this command.
         * @param operationData Any necessary operation-specific data for the command.
         * @return A positive value indicating the Transaction ID of the
         *         command, or a negative return error code indicating failure.
         *         The Transaction ID can be used to match this command with its
         *         associated Command Confirmation event.
         */
        public int sendPassthroughCommand(BluetoothAddress remoteDeviceAddress, long responseTimeout, RemoteControlPassThroughOperationID operationID, RemoteControlButtonState stateFlag, byte[] operationData) {
            int     oID;
            boolean state;
            byte[]  address = remoteDeviceAddress.internalByteArray();

            switch(operationID) {
            case SELECT:
                oID = AVRCP_PASS_THROUGH_ID_SELECT;
                break;
            case UP:
                oID = AVRCP_PASS_THROUGH_ID_UP;
                break;
            case DOWN:
                oID = AVRCP_PASS_THROUGH_ID_DOWN;
                break;
            case LEFT:
                oID = AVRCP_PASS_THROUGH_ID_LEFT;
                break;
            case RIGHT:
                oID = AVRCP_PASS_THROUGH_ID_RIGHT;
                break;
            case RIGHT_UP:
                oID = AVRCP_PASS_THROUGH_ID_RIGHT_UP;
                break;
            case RIGHT_DOWN:
                oID = AVRCP_PASS_THROUGH_ID_RIGHT_DOWN;
                break;
            case LEFT_UP:
                oID = AVRCP_PASS_THROUGH_ID_LEFT_UP;
                break;
            case LEFT_DOWN:
                oID = AVRCP_PASS_THROUGH_ID_LEFT_DOWN;
                break;
            case ROOT_MENU:
                oID = AVRCP_PASS_THROUGH_ID_ROOT_MENU;
                break;
            case SETUP_MENU:
                oID = AVRCP_PASS_THROUGH_ID_SETUP_MENU;
                break;
            case CONTENTS_MENU:
                oID = AVRCP_PASS_THROUGH_ID_CONTENTS_MENU;
                break;
            case FAVORITE_MENU:
                oID = AVRCP_PASS_THROUGH_ID_FAVORITE_MENU;
                break;
            case EXIT:
                oID = AVRCP_PASS_THROUGH_ID_EXIT;
                break;
            case NUM_0:
                oID = AVRCP_PASS_THROUGH_ID_0;
                break;
            case NUM_1:
                oID = AVRCP_PASS_THROUGH_ID_1;
                break;
            case NUM_2:
                oID = AVRCP_PASS_THROUGH_ID_2;
                break;
            case NUM_3:
                oID = AVRCP_PASS_THROUGH_ID_3;
                break;
            case NUM_4:
                oID = AVRCP_PASS_THROUGH_ID_4;
                break;
            case NUM_5:
                oID = AVRCP_PASS_THROUGH_ID_5;
                break;
            case NUM_6:
                oID = AVRCP_PASS_THROUGH_ID_6;
                break;
            case NUM_7:
                oID = AVRCP_PASS_THROUGH_ID_7;
                break;
            case NUM_8:
                oID = AVRCP_PASS_THROUGH_ID_8;
                break;
            case NUM_9:
                oID = AVRCP_PASS_THROUGH_ID_9;
                break;
            case DOT:
                oID = AVRCP_PASS_THROUGH_ID_DOT;
                break;
            case ENTER:
                oID = AVRCP_PASS_THROUGH_ID_ENTER;
                break;
            case CLEAR:
                oID = AVRCP_PASS_THROUGH_ID_CLEAR;
                break;
            case CHANNEL_UP:
                oID = AVRCP_PASS_THROUGH_ID_CHANNEL_UP;
                break;
            case CHANNEL_DOWN:
                oID = AVRCP_PASS_THROUGH_ID_CHANNEL_DOWN;
                break;
            case PREVIOUS_CHANNEL:
                oID = AVRCP_PASS_THROUGH_ID_PREVIOUS_CHANNEL;
                break;
            case SOUND_SELECT:
                oID = AVRCP_PASS_THROUGH_ID_SOUND_SELECT;
                break;
            case INPUT_SELECT:
                oID = AVRCP_PASS_THROUGH_ID_INPUT_SELECT;
                break;
            case DISPLAY_INFORMATION:
                oID = AVRCP_PASS_THROUGH_ID_DISPLAY_INFORMATION;
                break;
            case HELP:
                oID = AVRCP_PASS_THROUGH_ID_HELP;
                break;
            case PAGE_UP:
                oID = AVRCP_PASS_THROUGH_ID_PAGE_UP;
                break;
            case PAGE_DOWN:
                oID = AVRCP_PASS_THROUGH_ID_PAGE_DOWN;
                break;
            case POWER:
                oID = AVRCP_PASS_THROUGH_ID_POWER;
                break;
            case VOLUME_UP:
                oID = AVRCP_PASS_THROUGH_ID_VOLUME_UP;
                break;
            case VOLUME_DOWN:
                oID = AVRCP_PASS_THROUGH_ID_VOLUME_DOWN;
                break;
            case MUTE:
                oID = AVRCP_PASS_THROUGH_ID_MUTE;
                break;
            case PLAY:
                oID = AVRCP_PASS_THROUGH_ID_PLAY;
                break;
            case STOP:
                oID = AVRCP_PASS_THROUGH_ID_STOP;
                break;
            case PAUSE:
                oID = AVRCP_PASS_THROUGH_ID_PAUSE;
                break;
            case RECORD:
                oID = AVRCP_PASS_THROUGH_ID_RECORD;
                break;
            case REWIND:
                oID = AVRCP_PASS_THROUGH_ID_REWIND;
                break;
            case FAST_FORWARD:
                oID = AVRCP_PASS_THROUGH_ID_FAST_FORWARD;
                break;
            case EJECT:
                oID = AVRCP_PASS_THROUGH_ID_EJECT;
                break;
            case FORWARD:
                oID = AVRCP_PASS_THROUGH_ID_FORWARD;
                break;
            case BACKWARD:
                oID = AVRCP_PASS_THROUGH_ID_BACKWARD;
                break;
            case ANGLE:
                oID = AVRCP_PASS_THROUGH_ID_ANGLE;
                break;
            case SUBPICTURE:
                oID = AVRCP_PASS_THROUGH_ID_SUBPICTURE;
                break;
            case F1:
                oID = AVRCP_PASS_THROUGH_ID_F1;
                break;
            case F2:
                oID = AVRCP_PASS_THROUGH_ID_F2;
                break;
            case F3:
                oID = AVRCP_PASS_THROUGH_ID_F3;
                break;
            case F4:
                oID = AVRCP_PASS_THROUGH_ID_F4;
                break;
            case F5:
                oID = AVRCP_PASS_THROUGH_ID_F5;
                break;
            case VENDOR_UNIQUE:
            default:
                oID = AVRCP_PASS_THROUGH_ID_VENDOR_UNIQUE;
                break;
            }

            switch(stateFlag) {
            case PRESSED:
                state = false;
                break;
            case RELEASED:
            default:
                state = true;
                break;
            }

            return sendRemoteControlPassThroughCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], responseTimeout, RemoteControlCommandType.CONTROL.value, RemoteControlSubunitType.PANEL.value, RemoteControlSubunitID.INSTANCE_0.value, oID, state, operationData);
        }
        
        /**
         * Send a generic AVRCP Vendor Dependent command.
         * 
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device
         * @param responseTimeout The length of time (in milliseconds) to wait for a response.
         * @param commandType The command type for the command.
         * @param subunitType The subunit type for the command.
         * @param subunitID The subunit ID for the command.
         * @param companyID The Company ID for the command.
         * @param commandData Any specific PDU data to send in the command.
         * @return A positive value indicating the Transaction ID of the
         *         command, or a negative return error code indicating failure.
         *         The Transaction ID can be used to match this command with its
         *         associated Command Confirmation event.
         */
        public int vendorDependentGenericCommand(BluetoothAddress remoteDeviceAddress, long responseTimeout, RemoteControlCommandType commandType, RemoteControlSubunitType subunitType, RemoteControlSubunitID subunitID, CompanyID companyID, byte[] commandData) {
            byte[]  address = remoteDeviceAddress.internalByteArray();
            
            return vendorDependentCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], responseTimeout, commandType.value, subunitType.value, subunitID.value, companyID.id0, companyID.id1, companyID.id2, commandData);
        }
        
        /**
         * Navigate between media groups on the remote player application.
         * 
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device
         * @param responseTimeout The length of time (in milliseconds) to wait for a response.
         * @param buttonState Indicates the state of the button sending this command.
         * @param navigationType The type of navigation command to perform.
         * @return A positive value indicating the Transaction ID of the
         *         command, or a negative return error code indicating failure.
         *         The Transaction ID can be used to match this command with its
         *         associated Command Confirmation event.
         */
        public int groupNavigation(BluetoothAddress remoteDeviceAddress, long responseTimeout, RemoteControlButtonState buttonState, GroupNavigationType navigationType) {
            boolean state;
            byte[]  address = remoteDeviceAddress.internalByteArray();
            
            switch(buttonState) {
            case PRESSED:
                state = true;
                break;
            case RELEASED:
                state = false;
                break;
            default:
                state = true;
                break;
            
            }
            
            return groupNavigationCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], responseTimeout, state, navigationType.value);
        }

        /**
         * Get the capabilities of the remote AVRCP device.
         * 
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device
         * @param responseTimeout The length of time (in milliseconds) to wait for a response.
         * @param capabilityID Indicated the type of capabilities to request.
         * @return A positive value indicating the Transaction ID of the
         *         command, or a negative return error code indicating failure.
         *         The Transaction ID can be used to match this command with its
         *         associated Command Confirmation event.
         */
        public int getRemoteCapabilities(BluetoothAddress remoteDeviceAddress, long responseTimeout, CapabilityID capabilityID) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            
            return getCapabilitiesCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], responseTimeout, capabilityID.value);
        }
        
        /**
         * List all settings supported by the remote player Media Player application.
         * 
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device
         * @param responseTimeout The length of time (in milliseconds) to wait for a response.
         * @return A positive value indicating the Transaction ID of the
         *         command, or a negative return error code indicating failure.
         *         The Transaction ID can be used to match this command with its
         *         associated Command Confirmation event.
         */
        public int listPlayerApplicationSettingAttributes(BluetoothAddress remoteDeviceAddress, long responseTimeout) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            
            return listPlayerApplicationSettingAttribtutesCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], responseTimeout);
        }
        
        /**
         * List all of the possible values for a given setting on the remote Media Player application.
         * 
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device
         * @param responseTimeout The length of time (in milliseconds) to wait for a response.
         * @param attributeID The attribute for which to request the possible values.
         * @return A positive value indicating the Transaction ID of the
         *         command, or a negative return error code indicating failure.
         *         The Transaction ID can be used to match this command with its
         *         associated Command Confirmation event.
         */
        public int listPlayerApplicationSettingValues(BluetoothAddress remoteDeviceAddress, long responseTimeout, byte attributeID) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            
            return listPlayerApplicationSettingValuesCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], responseTimeout, attributeID);
        }
        
        /**
         * Get the current value for the specified settings on the remote Media Player application.
         * 
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device
         * @param responseTimeout The length of time (in milliseconds) to wait for a response.
         * @param attributeIDs A list of attributes for which to request the current value.
         * @return A positive value indicating the Transaction ID of the
         *         command, or a negative return error code indicating failure.
         *         The Transaction ID can be used to match this command with its
         *         associated Command Confirmation event.
         */
        public int getCurrentPlayerApplicationSettingValue(BluetoothAddress remoteDeviceAddress, long responseTimeout, byte[] attributeIDs) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            
            return getCurrentPlayerApplicationSettingValueCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], responseTimeout, attributeIDs);
        }
        
        /**
         * Assign a new value to the specified settings on the remote Media Player application.
         * 
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device
         * @param responseTimeout The length of time (in milliseconds) to wait for a response.
         * @param attributeValues A map of all attributeIDs to set along with the new value to assign.
         * @return A positive value indicating the Transaction ID of the
         *         command, or a negative return error code indicating failure.
         *         The Transaction ID can be used to match this command with its
         *         associated Command Confirmation event.
         */
        public int setPlayerApplicationSettingValue(BluetoothAddress remoteDeviceAddress, long responseTimeout, Map<Byte,Byte> attributeValues) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            
            if(attributeValues != null && attributeValues.size() > 0) {
                byte[] attributeIDs = new byte[attributeValues.size()];
                byte[] values = new byte[attributeValues.size()];
                
                int count = 0;
                
                Set<Byte> keys = attributeValues.keySet();
                
                for(byte key : keys) {
                    attributeIDs[count] = key;
                    values[count] = attributeValues.get(key);
                    
                    ++count;
                }
                
                return setPlayerApplicationSettingValueCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], responseTimeout, attributeIDs, values);
            }
            else {
                //Invalid parameter
                return -10001;
            }
        }
        
        /**
         * Get the displayable text description of the specified settings on the remote Media Player application.
         * 
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device
         * @param responseTimeout The length of time (in milliseconds) to wait for a response.
         * @param attributeIDs A list of attribute IDs for which to request the displayable text.
         * @return A positive value indicating the Transaction ID of the
         *         command, or a negative return error code indicating failure.
         *         The Transaction ID can be used to match this command with its
         *         associated Command Confirmation event.
         */
        public int getPlayerApplicationSettingAttributeText(BluetoothAddress remoteDeviceAddress, long responseTimeout, byte[] attributeIDs) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            
            return getPlayerApplicationSettingAttributeTextCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], responseTimeout, attributeIDs);
        }
        
        /**
         * Get the displayable text description of the specified possible values for the corresponding setting on the remote Media Player application.
         * 
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device
         * @param responseTimeout The length of time (in milliseconds) to wait for a response.
         * @param attributeID The attributeID to which the value IDs apply.
         * @param valueIDs A list of value IDs for which to request the displayable text.
         * @return A positive value indicating the Transaction ID of the
         *         command, or a negative return error code indicating failure.
         *         The Transaction ID can be used to match this command with its
         *         associated Command Confirmation event.
         */
        public int getPlayerApplicationSettingValueText(BluetoothAddress remoteDeviceAddress, long responseTimeout, byte attributeID, byte[] valueIDs) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            
            return getPlayerApplicationSettingValueTextCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], responseTimeout, attributeID, valueIDs);
        }
        
        /**
         * Inform the remote AVRCP device of the local device's displayable character sets.
         * 
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device
         * @param responseTimeout The length of time (in milliseconds) to wait for a response.
         * @param characterSets The list of supported character set codes. These codes are the MIBenum codes as defined by the 
         *                      <a href="http://www.iana.org/assignments/character-sets/character-sets.xhtml">IANA Specification</a>
         * @return A positive value indicating the Transaction ID of the
         *         command, or a negative return error code indicating failure.
         *         The Transaction ID can be used to match this command with its
         *         associated Command Confirmation event.
         *         
         * @see AVRCP#CHARACTER_SET_US_ASCII
         * @see AVRCP#CHARACTER_SET_UTF_16
         * @see AVRCP#CHARACTER_SET_UTF_8
         */
        public int informDisplayableCharacterSet(BluetoothAddress remoteDeviceAddress, long responseTimeout, short[] characterSets) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            
            return informDisplayableCharacterSetCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], responseTimeout, characterSets);
        }
        
        /**
         * Inform the remote AVRCP device is the local device's current battery status.
         * 
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device
         * @param responseTimeout The length of time (in milliseconds) to wait for a response.
         * @param batteryStatus The battery status of the RC Controller.
         * @return A positive value indicating the Transaction ID of the
         *         command, or a negative return error code indicating failure.
         *         The Transaction ID can be used to match this command with its
         *         associated Command Confirmation event.
         */
        public int informBatteryStatusOfController(BluetoothAddress remoteDeviceAddress, long responseTimeout, BatteryStatus batteryStatus) {
            byte[] address = remoteDeviceAddress.internalByteArray();
         
            return informBatteryStatusOfControllerCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], responseTimeout, batteryStatus.value);
        }
        
        /**
         * Get the meta-data attributes for the specified media.
         * 
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device
         * @param responseTimeout The length of time (in milliseconds) to wait for a response.
         * @param mediaIndentifier The indentifier of the media item for which to pull the attributes. To pull the current media, use {@link AVRCP#MEDIA_INDENTIFIER_CURRENTLY_PLAYING}.
         * @param attributeIDs The set of attributes to pull.
         * @return A positive value indicating the Transaction ID of the
         *         command, or a negative return error code indicating failure.
         *         The Transaction ID can be used to match this command with its
         *         associated Command Confirmation event.
         */
        public int getElementAttributes(BluetoothAddress remoteDeviceAddress, long responseTimeout, long mediaIndentifier, EnumSet<ElementAttributeID> attributeIDs) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            
            return getElementAttributesCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], responseTimeout, mediaIndentifier, ElementAttributeID.toMask(attributeIDs));
        }
        
        /**
         * Get the current status of the remote media player.
         * 
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device
         * @param responseTimeout The length of time (in milliseconds) to wait for a response.
         * @return A positive value indicating the Transaction ID of the
         *         command, or a negative return error code indicating failure.
         *         The Transaction ID can be used to match this command with its
         *         associated Command Confirmation event.
         */
        public int getPlayStatus(BluetoothAddress remoteDeviceAddress, long responseTimeout) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            
            return getPlayStatusCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], responseTimeout);
        }
        
        /**
         * Register to receive a notification from the remote AVRCP device when specified values change.
         * <p>
         * The caller will receive a callback to the corresponding event in the registered {@link ControllerEventCallback}.
         * If the remote device accepts the command, this callback will be called with the {@link RemoteControlResponseCode#INTERIM} response code immediately.
         * When the value on the remote device changes, another callback will be received with the {@link RemoteControlResponseCode#CHANGED} response code.
         * After receiving this call, it is up to the application to re-register for the event if it wants to continue to
         * receive notifications.
         * 
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device
         * @param responseTimeout The length of time (in milliseconds) to wait for a response.
         * @param eventID The type of event for which to register.
         * @param requestParameter Parameter data to include if the request requires it. Only the 
         *                         {@link EventID#PLAYBACK_POSITION_CHANGED} event requires this parameter 
         *                         to be specified as the interval (in seconds) to receive the event.
         * @return A positive value indicating the Transaction ID of the
         *         command, or a negative return error code indicating failure.
         *         The Transaction ID can be used to match this command with its
         *         associated Command Confirmation event.
         */
        public int registerNotification(BluetoothAddress remoteDeviceAddress, long responseTimeout, EventID eventID, int requestParameter) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            
            return registerNotificationCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], responseTimeout, eventID.value, requestParameter);
        }
        
        /**
         * Set the absolute volume on the remote AVRCP device.
         * 
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device
         * @param responseTimeout The length of time (in milliseconds) to wait for a response.
         * @param volume The absolute volume to set. This value MUST be between 0.0f and 1.0f inclusively.
         * @return A positive value indicating the Transaction ID of the
         *         command, or a negative return error code indicating failure.
         *         The Transaction ID can be used to match this command with its
         *         associated Command Confirmation event.
         */
        public int setAbsoluteVolume(BluetoothAddress remoteDeviceAddress, long responseTimeout, float volume) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            
            if(volume >= 0.0f && volume <= 1.0f) {
                byte vol = (byte)(MAX_VOLUME * volume);
            
                return setAbsoluteVolumeCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], responseTimeout, vol);
            }
            else
                return -10001;
        }
        
        @Override
        protected void remoteControlPassThroughCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, byte subunitType, byte subunitID, int operationID, boolean stateFlag, byte operationData[]) {
            ControllerEventCallback callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress remoteDeviceAddress;
            
            RemoteControlResponseCode           respCode = RemoteControlResponseCode.fromValue(responseCode);
            RemoteControlPassThroughOperationID key;
            RemoteControlButtonState            button;

            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                switch(operationID) {
                case AVRCP_PASS_THROUGH_ID_SELECT:
                    key = RemoteControlPassThroughOperationID.SELECT;
                    break;
                case AVRCP_PASS_THROUGH_ID_UP:
                    key = RemoteControlPassThroughOperationID.UP;
                    break;
                case AVRCP_PASS_THROUGH_ID_DOWN:
                    key = RemoteControlPassThroughOperationID.DOWN;
                    break;
                case AVRCP_PASS_THROUGH_ID_LEFT:
                    key = RemoteControlPassThroughOperationID.LEFT;
                    break;
                case AVRCP_PASS_THROUGH_ID_RIGHT:
                    key = RemoteControlPassThroughOperationID.RIGHT;
                    break;
                case AVRCP_PASS_THROUGH_ID_RIGHT_UP:
                    key = RemoteControlPassThroughOperationID.RIGHT_UP;
                    break;
                case AVRCP_PASS_THROUGH_ID_RIGHT_DOWN:
                    key = RemoteControlPassThroughOperationID.RIGHT_DOWN;
                    break;
                case AVRCP_PASS_THROUGH_ID_LEFT_UP:
                    key = RemoteControlPassThroughOperationID.LEFT_UP;
                    break;
                case AVRCP_PASS_THROUGH_ID_LEFT_DOWN:
                    key = RemoteControlPassThroughOperationID.LEFT_DOWN;
                    break;
                case AVRCP_PASS_THROUGH_ID_ROOT_MENU:
                    key = RemoteControlPassThroughOperationID.ROOT_MENU;
                    break;
                case AVRCP_PASS_THROUGH_ID_SETUP_MENU:
                    key = RemoteControlPassThroughOperationID.SETUP_MENU;
                    break;
                case AVRCP_PASS_THROUGH_ID_CONTENTS_MENU:
                    key = RemoteControlPassThroughOperationID.CONTENTS_MENU;
                    break;
                case AVRCP_PASS_THROUGH_ID_FAVORITE_MENU:
                    key = RemoteControlPassThroughOperationID.FAVORITE_MENU;
                    break;
                case AVRCP_PASS_THROUGH_ID_EXIT:
                    key = RemoteControlPassThroughOperationID.EXIT;
                    break;
                case AVRCP_PASS_THROUGH_ID_0:
                    key = RemoteControlPassThroughOperationID.NUM_0;
                    break;
                case AVRCP_PASS_THROUGH_ID_1:
                    key = RemoteControlPassThroughOperationID.NUM_1;
                    break;
                case AVRCP_PASS_THROUGH_ID_2:
                    key = RemoteControlPassThroughOperationID.NUM_2;
                    break;
                case AVRCP_PASS_THROUGH_ID_3:
                    key = RemoteControlPassThroughOperationID.NUM_3;
                    break;
                case AVRCP_PASS_THROUGH_ID_4:
                    key = RemoteControlPassThroughOperationID.NUM_4;
                    break;
                case AVRCP_PASS_THROUGH_ID_5:
                    key = RemoteControlPassThroughOperationID.NUM_5;
                    break;
                case AVRCP_PASS_THROUGH_ID_6:
                    key = RemoteControlPassThroughOperationID.NUM_6;
                    break;
                case AVRCP_PASS_THROUGH_ID_7:
                    key = RemoteControlPassThroughOperationID.NUM_7;
                    break;
                case AVRCP_PASS_THROUGH_ID_8:
                    key = RemoteControlPassThroughOperationID.NUM_8;
                    break;
                case AVRCP_PASS_THROUGH_ID_9:
                    key = RemoteControlPassThroughOperationID.NUM_9;
                    break;
                case AVRCP_PASS_THROUGH_ID_DOT:
                    key = RemoteControlPassThroughOperationID.DOT;
                    break;
                case AVRCP_PASS_THROUGH_ID_ENTER:
                    key = RemoteControlPassThroughOperationID.ENTER;
                    break;
                case AVRCP_PASS_THROUGH_ID_CLEAR:
                    key = RemoteControlPassThroughOperationID.CLEAR;
                    break;
                case AVRCP_PASS_THROUGH_ID_CHANNEL_UP:
                    key = RemoteControlPassThroughOperationID.CHANNEL_UP;
                    break;
                case AVRCP_PASS_THROUGH_ID_CHANNEL_DOWN:
                    key = RemoteControlPassThroughOperationID.CHANNEL_DOWN;
                    break;
                case AVRCP_PASS_THROUGH_ID_PREVIOUS_CHANNEL:
                    key = RemoteControlPassThroughOperationID.PREVIOUS_CHANNEL;
                    break;
                case AVRCP_PASS_THROUGH_ID_SOUND_SELECT:
                    key = RemoteControlPassThroughOperationID.SOUND_SELECT;
                    break;
                case AVRCP_PASS_THROUGH_ID_INPUT_SELECT:
                    key = RemoteControlPassThroughOperationID.INPUT_SELECT;
                    break;
                case AVRCP_PASS_THROUGH_ID_DISPLAY_INFORMATION:
                    key = RemoteControlPassThroughOperationID.DISPLAY_INFORMATION;
                    break;
                case AVRCP_PASS_THROUGH_ID_HELP:
                    key = RemoteControlPassThroughOperationID.HELP;
                    break;
                case AVRCP_PASS_THROUGH_ID_PAGE_UP:
                    key = RemoteControlPassThroughOperationID.PAGE_UP;
                    break;
                case AVRCP_PASS_THROUGH_ID_PAGE_DOWN:
                    key = RemoteControlPassThroughOperationID.PAGE_DOWN;
                    break;
                case AVRCP_PASS_THROUGH_ID_POWER:
                    key = RemoteControlPassThroughOperationID.POWER;
                    break;
                case AVRCP_PASS_THROUGH_ID_VOLUME_UP:
                    key = RemoteControlPassThroughOperationID.VOLUME_UP;
                    break;
                case AVRCP_PASS_THROUGH_ID_VOLUME_DOWN:
                    key = RemoteControlPassThroughOperationID.VOLUME_DOWN;
                    break;
                case AVRCP_PASS_THROUGH_ID_MUTE:
                    key = RemoteControlPassThroughOperationID.MUTE;
                    break;
                case AVRCP_PASS_THROUGH_ID_PLAY:
                    key = RemoteControlPassThroughOperationID.PLAY;
                    break;
                case AVRCP_PASS_THROUGH_ID_STOP:
                    key = RemoteControlPassThroughOperationID.STOP;
                    break;
                case AVRCP_PASS_THROUGH_ID_PAUSE:
                    key = RemoteControlPassThroughOperationID.PAUSE;
                    break;
                case AVRCP_PASS_THROUGH_ID_RECORD:
                    key = RemoteControlPassThroughOperationID.RECORD;
                    break;
                case AVRCP_PASS_THROUGH_ID_REWIND:
                    key = RemoteControlPassThroughOperationID.REWIND;
                    break;
                case AVRCP_PASS_THROUGH_ID_FAST_FORWARD:
                    key = RemoteControlPassThroughOperationID.FAST_FORWARD;
                    break;
                case AVRCP_PASS_THROUGH_ID_EJECT:
                    key = RemoteControlPassThroughOperationID.EJECT;
                    break;
                case AVRCP_PASS_THROUGH_ID_FORWARD:
                    key = RemoteControlPassThroughOperationID.FORWARD;
                    break;
                case AVRCP_PASS_THROUGH_ID_BACKWARD:
                    key = RemoteControlPassThroughOperationID.BACKWARD;
                    break;
                case AVRCP_PASS_THROUGH_ID_ANGLE:
                    key = RemoteControlPassThroughOperationID.ANGLE;
                    break;
                case AVRCP_PASS_THROUGH_ID_SUBPICTURE:
                    key = RemoteControlPassThroughOperationID.SUBPICTURE;
                    break;
                case AVRCP_PASS_THROUGH_ID_F1:
                    key = RemoteControlPassThroughOperationID.F1;
                    break;
                case AVRCP_PASS_THROUGH_ID_F2:
                    key = RemoteControlPassThroughOperationID.F2;
                    break;
                case AVRCP_PASS_THROUGH_ID_F3:
                    key = RemoteControlPassThroughOperationID.F3;
                    break;
                case AVRCP_PASS_THROUGH_ID_F4:
                    key = RemoteControlPassThroughOperationID.F4;
                    break;
                case AVRCP_PASS_THROUGH_ID_F5:
                    key = RemoteControlPassThroughOperationID.F5;
                    break;
                case AVRCP_PASS_THROUGH_ID_VENDOR_UNIQUE:
                default:
                    key = RemoteControlPassThroughOperationID.VENDOR_UNIQUE;
                    break;
                }
                
                button = stateFlag?RemoteControlButtonState.PRESSED:RemoteControlButtonState.RELEASED;
                
                try {
                    callbackHandler.passthroughResponse(this, remoteDeviceAddress, transactionID, respCode, key, button, operationData);
                } catch(Exception e) {

                }
            }
        }
        
        @Override
        protected void vendorDependentCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, byte subunitType, byte subunitID, byte ID0, byte ID1, byte ID2, byte[] data) {
            ControllerEventCallback   callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress          remoteDeviceAddress;
            RemoteControlResponseCode respCode = RemoteControlResponseCode.fromValue(responseCode);
            RemoteControlSubunitType  subType = RemoteControlSubunitType.fromValue(subunitType);
            RemoteControlSubunitID    subID   = RemoteControlSubunitID.fromValue(subunitID);
            CompanyID                 cID = new CompanyID(ID0, ID1, ID2);

            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
                
                try {
                    callbackHandler.vendorDependentResponse(this, remoteDeviceAddress, transactionID, respCode, subType, subID, cID, data);
                } catch(Exception e) {

                }
            }
        }
        
        @Override
        protected void groupNavigationCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, boolean stateFlag) {
            ControllerEventCallback   callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress          remoteDeviceAddress;
            RemoteControlResponseCode respCode = RemoteControlResponseCode.fromValue(responseCode);

            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
                
                try {
                    callbackHandler.groupNavigationResponse(this, remoteDeviceAddress, transactionID, respCode, stateFlag?RemoteControlButtonState.PRESSED:RemoteControlButtonState.RELEASED);
                } catch(Exception e) {

                }
            }
        }
        
        @Override
        protected void getCompanyIDCapabilitiesCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, byte[] ids) {
            ControllerEventCallback   callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress          remoteDeviceAddress;
            RemoteControlResponseCode respCode = RemoteControlResponseCode.fromValue(responseCode);
            CompanyID[]               companyIDs;

            companyIDs = new CompanyID[(ids != null)?(ids.length / 3):0];

            for(int i=0;i<companyIDs.length;i++)
                companyIDs[i] = new CompanyID(ids[3*i], ids[3*i+1], ids[3*i+2]);

            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
                
                try {
                    callbackHandler.getCompanyIDCapabilitiesResponse(this, remoteDeviceAddress, transactionID, respCode, companyIDs);
                } catch(Exception e) {

                }
            }
        }

        @Override
        protected void getEventsSupportedCapabilitiesCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, int eventIDs) {
            ControllerEventCallback   callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress          remoteDeviceAddress;
            RemoteControlResponseCode respCode = RemoteControlResponseCode.fromValue(responseCode);

            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
                
                try {
                    callbackHandler.getEventCapabilitiesResponse(this, remoteDeviceAddress, transactionID, respCode, EventID.fromMask(eventIDs));
                } catch(Exception e) {

                }
            }
        }

        @Override
        protected void listPlayerApplicationSettingAttributesCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, byte[] attributeIDs) {
            ControllerEventCallback   callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress          remoteDeviceAddress;
            RemoteControlResponseCode respCode = RemoteControlResponseCode.fromValue(responseCode);

            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
                
                try {
                    callbackHandler.listPlayerApplicationSettingAttributesResponse(this, remoteDeviceAddress, transactionID, respCode, attributeIDs);
                } catch(Exception e) {

                }
            }
        }
        
        @Override
        protected void listPlayerApplicationSettingValuesCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, byte[] values) {
            ControllerEventCallback   callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress          remoteDeviceAddress;
            RemoteControlResponseCode respCode = RemoteControlResponseCode.fromValue(responseCode);
            
            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
                
                try {
                    callbackHandler.listPlayerApplicationSettingValuesResponse(this, remoteDeviceAddress, transactionID, respCode, values);
                } catch(Exception e) {

                }
            }
        }

        @Override
        protected void getCurrentPlayerApplicationSettingValueCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, byte[] ids, byte[] values) {
            ControllerEventCallback   callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress          remoteDeviceAddress;
            RemoteControlResponseCode respCode = RemoteControlResponseCode.fromValue(responseCode);
            Map<Byte,Byte>      valueMap;

            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
                
                if(ids != null && values != null && ids.length == values.length) {
                    valueMap = new HashMap<Byte,Byte>();
                    
                    for(int i=0; i<ids.length; i++)
                        valueMap.put(ids[i], values[i]);
                }
                else
                    valueMap = null;
                
                try {
                    callbackHandler.getCurrentPlayerApplicationSettingValueResponse(this, remoteDeviceAddress, transactionID, respCode, valueMap);
                } catch(Exception e) {

                }
            }
        }

        @Override
        protected void setPlayerApplicationSettingValueCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode) {
            ControllerEventCallback   callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress          remoteDeviceAddress;
            RemoteControlResponseCode respCode = RemoteControlResponseCode.fromValue(responseCode);

            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
                
                try {
                    callbackHandler.setPlayerApplicationSettingValueResponse(this, remoteDeviceAddress, transactionID, respCode);
                } catch(Exception e) {

                }
            }
        }
        
        @Override
        protected void getPlayerApplicationSettingAttributeTextCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, byte[] ids, short[] characterSets, byte[][] textData) {
            ControllerEventCallback        callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress               remoteDeviceAddress;
            RemoteControlResponseCode      respCode = RemoteControlResponseCode.fromValue(responseCode);
            PlayerApplicationSettingText[] textEntries;

            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
        
                if(ids != null && characterSets != null && textData != null && ids.length == characterSets.length && ids.length == textData.length) {
                    textEntries = new PlayerApplicationSettingText[ids.length];
                    
                    for(int i=0; i<ids.length; i++)
                        textEntries[i] = new PlayerApplicationSettingText(ids[i], characterSets[i], textData[i]);
                }
                else
                    textEntries = null;
                
                try {
                    callbackHandler.getPlayerApplicationSettingAttributeTextResponse(this, remoteDeviceAddress, transactionID, respCode, textEntries);
                } catch(Exception e) {

                }
            }
        }
        
        @Override
        protected void getPlayerApplicationSettingValueTextCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, byte[] ids, short[] characterSets, byte[][] textData) {
            ControllerEventCallback        callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress               remoteDeviceAddress;
            RemoteControlResponseCode      respCode = RemoteControlResponseCode.fromValue(responseCode);
            PlayerApplicationSettingText[] textEntries;

            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
                
                if(ids != null && characterSets != null && textData != null && ids.length == characterSets.length && ids.length == textData.length) {
                    textEntries = new PlayerApplicationSettingText[ids.length];
                    
                    for(int i=0; i<ids.length; i++)
                        textEntries[i] = new PlayerApplicationSettingText(ids[i], characterSets[i], textData[i]);
                }
                else
                    textEntries = null;
                
                try {
                    callbackHandler.getPlayerApplicationSettingValueTextResponse(this, remoteDeviceAddress, transactionID, respCode, textEntries);
                } catch(Exception e) {

                }
            }
        }
        
        @Override
        protected void informDisplayableCharacterSetCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode) {
            ControllerEventCallback   callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress          remoteDeviceAddress;
            RemoteControlResponseCode respCode = RemoteControlResponseCode.fromValue(responseCode);

            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
                
                try {
                    callbackHandler.informDisplayableCharacterSetResponse(this, remoteDeviceAddress, transactionID, respCode);
                } catch(Exception e) {

                }
            }
        }
        
        @Override
        protected void informBatteryStatusCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode) {
            ControllerEventCallback   callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress          remoteDeviceAddress;
            RemoteControlResponseCode respCode = RemoteControlResponseCode.fromValue(responseCode);

            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
                
                try {
                    callbackHandler.informBatteryStatusResponse(this, remoteDeviceAddress, transactionID, respCode);
                } catch(Exception e) {

                }
            }
        }

        @Override
        protected void getElementAttributesCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, int[] attributeIDs, short[] characterSets, byte[][] elementData) {
            ControllerEventCallback   callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress          remoteDeviceAddress;
            RemoteControlResponseCode respCode = RemoteControlResponseCode.fromValue(responseCode);
            ElementAttribute[]        attributes;

            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
                
                if(attributeIDs != null && characterSets != null && elementData != null && attributeIDs.length == characterSets.length && attributeIDs.length == elementData.length) {
                    attributes = new ElementAttribute[attributeIDs.length];

                    for(int i=0;i<attributes.length;i++) {
                        attributes[i] = new ElementAttribute(ElementAttributeID.fromValue(attributeIDs[i]), characterSets[i], elementData[i]);
                    }
                }
                else
                    attributes = null;
                
                try {
                    callbackHandler.getElementAttributesResponse(this, remoteDeviceAddress, transactionID, respCode, attributes);
                } catch(Exception e) {

                }
            }
        }

        @Override
        protected void getPlayStatusCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, int length, int position, int playStatus) {
            ControllerEventCallback   callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress          remoteDeviceAddress;
            RemoteControlResponseCode respCode = RemoteControlResponseCode.fromValue(responseCode);

            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
                
                try {
                    callbackHandler.getPlayStatusResponse(this, remoteDeviceAddress, transactionID, respCode, length, position, PlayStatus.fromValue(playStatus));
                } catch(Exception e) {

                }
            }
        }

        @Override
        protected void playbackStatusChangedNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, int playStatus) {
            ControllerEventCallback   callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress          remoteDeviceAddress;
            RemoteControlResponseCode respCode = RemoteControlResponseCode.fromValue(responseCode);

            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                try {
                    callbackHandler.playbackStatusChangedNotification(this, remoteDeviceAddress, transactionID, respCode, PlayStatus.fromValue(playStatus));
                } catch(Exception e) {

                }
            }
        }

        @Override
        protected void trackChangedNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, long identifier) {
            ControllerEventCallback   callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress          remoteDeviceAddress;
            RemoteControlResponseCode respCode = RemoteControlResponseCode.fromValue(responseCode);

            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
                
                try {
                    callbackHandler.trackChangedNotification(this, remoteDeviceAddress, transactionID, respCode, identifier);
                } catch(Exception e) {

                }
            }
        }

        @Override
        protected void trackReachedEndNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode) {
            ControllerEventCallback   callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress          remoteDeviceAddress;
            RemoteControlResponseCode respCode = RemoteControlResponseCode.fromValue(responseCode);

            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
                
                try {
                    callbackHandler.trackReachedEndNotification(this, remoteDeviceAddress, transactionID, respCode);
                } catch(Exception e) {

                }
            }
        }

        @Override
        protected void trackReachedStartNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode) {
            ControllerEventCallback   callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress          remoteDeviceAddress;
            RemoteControlResponseCode respCode = RemoteControlResponseCode.fromValue(responseCode);

            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
                
                try {
                    callbackHandler.trackReachedStartNotification(this, remoteDeviceAddress, transactionID, respCode);
                } catch(Exception e) {

                }
            }
        }

        @Override
        protected void playbackPositionChangedNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, int position) {
            ControllerEventCallback   callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress          remoteDeviceAddress;
            RemoteControlResponseCode respCode = RemoteControlResponseCode.fromValue(responseCode);
            
            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
                
                try {
                    callbackHandler.playbackPositionChangedNotification(this, remoteDeviceAddress, transactionID, respCode, position);
                } catch(Exception e) {

                }
            }
        }

        @Override
        protected void batteryStatusChangedNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, int batteryStatus) {
            ControllerEventCallback   callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress          remoteDeviceAddress;
            RemoteControlResponseCode respCode = RemoteControlResponseCode.fromValue(responseCode);

            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
              
                try {
                    callbackHandler.batteryStatusChangedNotification(this, remoteDeviceAddress, transactionID, respCode, BatteryStatus.fromValue(batteryStatus));
                } catch(Exception e) {

                }
            }
        }
        
        @Override
        protected void systemStatusChangedNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, int systemStatus) {
            ControllerEventCallback   callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress          remoteDeviceAddress;
            RemoteControlResponseCode respCode = RemoteControlResponseCode.fromValue(responseCode);
 
            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
                
                try {
                    callbackHandler.systemStatusChangedNotification(this, remoteDeviceAddress, transactionID, respCode, SystemStatus.fromValue(systemStatus));
                } catch(Exception e) {

                }
            }
        }
        
        @Override
        protected void playerApplicationSettingChangedNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, byte[] attributeIDs, byte[] valueIDs) {
            ControllerEventCallback   callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress          remoteDeviceAddress;
            RemoteControlResponseCode respCode = RemoteControlResponseCode.fromValue(responseCode);
            Map<Byte,Byte>            changedValues;
 
            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
                
                if(attributeIDs != null && valueIDs != null && attributeIDs.length == valueIDs.length) {
                    changedValues = new HashMap<Byte,Byte>();
                    
                    for(int i=0;i<attributeIDs.length;i++) {
                        changedValues.put(attributeIDs[i], valueIDs[i]);
                    }
                }
                else
                    changedValues = null;
                
                try {
                    callbackHandler.playerApplicationSettingChangedNotifications(this, remoteDeviceAddress, transactionID, respCode, changedValues);
                } catch(Exception e) {

                }
            }
        }

        @Override
        protected void volumeChangeNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, byte volume) {
            ControllerEventCallback   callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress          remoteDeviceAddress;
            RemoteControlResponseCode respCode = RemoteControlResponseCode.fromValue(responseCode);
            
            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
                
                try {
                    callbackHandler.volumeChangedNotification(this, remoteDeviceAddress, transactionID, respCode, (float)volume/(float)MAX_VOLUME);
                } catch(Exception e) {

                }
            }
        }

        @Override
        protected void setAbsoluteVolumeCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, byte volume) {
            ControllerEventCallback   callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress          remoteDeviceAddress;
            RemoteControlResponseCode respCode = RemoteControlResponseCode.fromValue(responseCode);

            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
                
                try {
                    callbackHandler.setAbsoluteVolumeResponse(this, remoteDeviceAddress, transactionID, respCode, (float)volume/(float)MAX_VOLUME);
                } catch(Exception e) {

                }
            }
        }
        
        @Override
        protected void commandRejectResponseEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, byte responseCode, int errorCode) {
            ControllerEventCallback   callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress          remoteDeviceAddress;
            RemoteControlResponseCode respCode = RemoteControlResponseCode.fromValue(responseCode);

            if(callbackHandler != null) {

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
                
                try {
                    callbackHandler.commandRejectedResponse(this, remoteDeviceAddress, transactionID, respCode, CommandErrorCode.fromValue(errorCode));
                } catch(Exception e) {

                }
            }
        }
        
        @Override
        protected void commandFailureEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, int status) {
            ControllerEventCallback        callbackHandler = (ControllerEventCallback)this.callbackHandler;
            BluetoothAddress               remoteDeviceAddress;
    
            if(callbackHandler != null) {
                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                try {
                    callbackHandler.commandFailureNotification(this, remoteDeviceAddress, transactionID, CommandFailureStatus.fromValue(status));
                } catch(Exception e) {

                }
            }
        }
        
        /**
         * Receiver for event notifications generated by the local Remote Control Controller module.
         * <p>
         * It is guaranteed that no two event methods will be invoked
         * simultaneously. However, because of this guarantee, the implementation of
         * each event handler must be as efficient as possible, as subsequent events
         * will not be sent until the in-progress event handler returns.
         * Furthermore, event handlers must not block and wait on conditions that
         * can only be satisfied by receiving other events.
         * <p>
         * Implementors should also note that events are sent from a thread context
         * owned by the BluetopiaPM API, so standard locking practices should be
         * observed for data sharing.
         * 
         * @see BaseControllerEventCallback
         */
        public interface ControllerEventCallback extends EventCallback {
            /**
             * Invoked when a passthrough command is completed.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             * @param operationID {@link RemoteControlPassThroughOperationID} of the response.
             * @param buttonState Indicates the state of the button that sent the command.
             * @param operationData Any necessary operation-specific data contained in the response. 
             */
            public void passthroughResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, RemoteControlPassThroughOperationID operationID, RemoteControlButtonState buttonState, byte[] operationData);
            
            /**
             * Invoked when a vendor dependent command is completed.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             * @param subunitType The subunit type for the command.
             * @param subunitID The subunit ID for the command.
             * @param companyID The Company ID for the command.
             * @param data And response-specific data sent by the remote side. 
             */
            public void vendorDependentResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, RemoteControlSubunitType subunitType, RemoteControlSubunitID subunitID, CompanyID companyID, byte[] data);
            
            /**
             * Invoked when a group navigation command is completed.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             * @param buttonState Indicates the state of the button that sent the command.
             */
            public void groupNavigationResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, RemoteControlButtonState buttonState);
            
            /**
             * Invoked when a get capabilities command with the capabilityID set to {@link CapabilityID#COMPANY_ID}
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             * @param ids The supported company IDs.
             */
            public void getCompanyIDCapabilitiesResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, CompanyID[] ids);
            
            /**
             * Invoked when a get capabilities command with the capabilityID set to {@link CapabilityID#EVENTS_SUPPORTED}
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             * @param ids The supported event IDs.
             */
            public void getEventCapabilitiesResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, EnumSet<EventID> ids);
            
            /**
             * Invoked when a list player application setting attributes command completes.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             * @param attributeIDs The list of settings attributes supported.
             */
            public void listPlayerApplicationSettingAttributesResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, byte[] attributeIDs);
            
            /**
             * Invoked when a list player application setting values command completes.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             * @param values The supported values for the requested attribute.
             */
            public void listPlayerApplicationSettingValuesResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, byte[] values);
            
            /**
             * Invoked when a get current player application setting value command completes.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             * @param currentValues A map of all the the requested attributes to their current values.
             */
            public void getCurrentPlayerApplicationSettingValueResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, Map<Byte, Byte> currentValues);
            
            /**
             * Invoked when a set player application setting value command completes.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             */
            public void setPlayerApplicationSettingValueResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode);
            
            /**
             * Invoked when a get player application setting attribute text command completes.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             * @param textEntries All of the requested attributes and their displayable text descriptions.
             */
            public void getPlayerApplicationSettingAttributeTextResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, PlayerApplicationSettingText[] textEntries);
            
            /**
             * Invoked when get player application setting value text command completes.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             * @param textEntries All of the requested values and their displayable text descriptions.
             */
            public void getPlayerApplicationSettingValueTextResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, PlayerApplicationSettingText[] textEntries);
            
            /**
             * Invoked when an inform displayable character set command completes.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             */
            public void informDisplayableCharacterSetResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode);
            
            /**
             * Invoked when an inform battery status command completes.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             */
            public void informBatteryStatusResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode);
            
            /**
             * Invoked when a get element attributes command completes.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             * @param attributes The element data for the requested attributes.
             */
            public void getElementAttributesResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, ElementAttribute[] attributes);
            
            /**
             * Invoked when a get play status command completes
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             * @param length The length of the currently playing media in milliseconds.
             * @param position The current position of the media in milliseconds.
             * @param playStatus The current {@link PlayStatus} of the remote media.
             */
            public void getPlayStatusResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, int length, int position, PlayStatus playStatus);
            
            /**
             * Invoked when a register notification command with the eventID set to {@link EventID#PLAYBACK_STATUS_CHANGED} 
             * completes or the value changes after a successful registration.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             * @param playStatus The current/updated {@link PlayStatus} of the remote media.
             */
            public void playbackStatusChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, PlayStatus playStatus);
            
            /**
             * Invoked when a register notification command with the eventID set to {@link EventID#TRACK_CHANGED} 
             * completes or the value changes after a successful registration.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             * @param identifier The identifier for the newly playing media.
             */
            public void trackChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, long identifier);
            
            /**
             * Invoked when a register notification command with the eventID set to {@link EventID#TRACK_REACHED_END} 
             * completes or the value changes after a successful registration.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             */
            public void trackReachedEndNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode);
            
            /**
             * Invoked when a register notification command with the eventID set to {@link EventID#TRACK_REACHED_START} 
             * completes or the value changes after a successful registration.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             */
            public void trackReachedStartNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode);
            
            /**
             * Invoked when a register notification command with the eventID set to {@link EventID#PLAYBACK_POSITION_CHANGED} 
             * completes or the value changes after a successful registration.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             * @param position The current/updated position of the currently playing media (in milliseconds).
             */
            public void playbackPositionChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, int position);
            
            /**
             * Invoked when a register notification command with the eventID set to {@link EventID#BATTERY_STATUS_CHANGED} 
             * completes or the value changes after a successful registration.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             * @param batteryStatus The current/updated battery status of the remote device.
             */
            public void batteryStatusChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, BatteryStatus batteryStatus);
            
            /**
             * Invoked when a register notification command with the eventID set to {@link EventID#SYSTEM_STATUS_CHANGED} 
             * completes or the value changes after a successful registration.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             * @param systemStatus The current/updated system status of the remote device.
             */
            public void systemStatusChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, SystemStatus systemStatus);
            
            /**
             * Invoked when a register notification command with the eventID set to {@link EventID#PLAYER_APPLICATION_SETTING_CHANGED} 
             * completes or the value changes after a successful registration.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             * @param changedValues The map of all the changed attributes to their new values.
             */
            public void playerApplicationSettingChangedNotifications(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, Map<Byte,Byte> changedValues);
            
            /**
             * Invoked when a register notification command with the eventID set to {@link EventID#VOLUME_CHANGED} 
             * completes or the value changes after a successful registration.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             * @param volume The current/updated volume of the remote device.
             */
            public void volumeChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, float volume);
            
            /**
             * Invoked when a set absolute volume command completes.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             * @param volume The volume that was actually assigned by the remote device (may not be exactly as requested).
             */
            public void setAbsoluteVolumeResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, float volume);
            
            //XXX Add PDU id?
            /**
             * Invoked when a remote device rejects a command.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID to which the response correlates.
             * @param responseCode The {@link RemoteControlResponseCode} from the remote device.
             * @param errorCode The error code sent by the remote device.
             */
            public void commandRejectedResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, CommandErrorCode errorCode);
            
            /**
             * Invoked when an error occurs while waiting on a response for a command that has been sent.
             * 
             * @param manager The {@link RemoteControlControllerManager} that this event is associated with.
             * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
             * @param transactionID The transactionID of the command that experience the error.
             * @param status The status code of the failed command.
             */
            public void commandFailureNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, CommandFailureStatus status);
        }
        
        /**
         * Provides a default implementation for all {@link ControllerEventCallback} functions. Extend this class if you
         * only need to implement a subset of the Event Callback's functionality.
         */
        public static abstract class BaseControllerEventCallback implements ControllerEventCallback {

            @Override
            public void incomingConnectionRequestEvent(AVRCP manager, BluetoothAddress remoteDeviceAddress) {
                
                
            }

            @Override
            public void remoteControlConnectedEvent(AVRCP manager, BluetoothAddress remoteDeviceAddress) {
                
                
            }

            @Override
            public void remoteControlConnectionStatusEvent(AVRCP manager, BluetoothAddress remoteDeviceAddress, ConnectionStatus connectionStatus) {
                
                
            }

            @Override
            public void remoteControlDisconnectedEvent(AVRCP manager, BluetoothAddress remoteDeviceAddress, RemoteControlDisconnectReason disconnectReason) {
                
                
            }

            @Override
            public void passthroughResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, RemoteControlPassThroughOperationID operationID, RemoteControlButtonState buttonState, byte[] operationData) {
                
                
            }

            @Override
            public void vendorDependentResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, RemoteControlSubunitType subunitType, RemoteControlSubunitID subunitID, CompanyID companyID, byte[] data) {
                
                
            }

            @Override
            public void groupNavigationResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, RemoteControlButtonState buttonState) {
                
                
            }

            @Override
            public void getCompanyIDCapabilitiesResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, CompanyID[] ids) {
                
                
            }

            @Override
            public void getEventCapabilitiesResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, EnumSet<EventID> ids) {
                
                
            }

            @Override
            public void listPlayerApplicationSettingAttributesResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, byte[] attributeIDs) {
                
                
            }

            @Override
            public void listPlayerApplicationSettingValuesResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, byte[] values) {
                
                
            }

            @Override
            public void getCurrentPlayerApplicationSettingValueResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, Map<Byte, Byte> currentValues) {
                
                
            }

            @Override
            public void setPlayerApplicationSettingValueResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode) {
                
                
            }

            @Override
            public void getPlayerApplicationSettingAttributeTextResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, PlayerApplicationSettingText[] textEntries) {
                
                
            }

            @Override
            public void getPlayerApplicationSettingValueTextResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, PlayerApplicationSettingText[] textEntries) {
                
                
            }

            @Override
            public void informDisplayableCharacterSetResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode) {
                
                
            }

            @Override
            public void informBatteryStatusResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode) {
                
                
            }

            @Override
            public void getElementAttributesResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, ElementAttribute[] attributes) {
                
                
            }

            @Override
            public void getPlayStatusResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, int length, int position, PlayStatus playStatus) {
                
                
            }

            @Override
            public void playbackStatusChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, PlayStatus playStatus) {
                
                
            }

            @Override
            public void trackChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, long identifier) {
                
                
            }

            @Override
            public void trackReachedEndNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode) {
                
                
            }

            @Override
            public void trackReachedStartNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode) {
                
                
            }

            @Override
            public void playbackPositionChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, int position) {
                
                
            }

            @Override
            public void batteryStatusChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, BatteryStatus batteryStatus) {
                
                
            }

            @Override
            public void systemStatusChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, SystemStatus systemStatus) {
                
                
            }

            @Override
            public void playerApplicationSettingChangedNotifications(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, Map<Byte, Byte> changedValues) {
                
                
            }

            @Override
            public void volumeChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, float volume) {
                
                
            }

            @Override
            public void setAbsoluteVolumeResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, float volume) {
                
                
            }

            @Override
            public void commandRejectedResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, CommandErrorCode errorCode) {
                
                
            }

            @Override
            public void commandFailureNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, CommandFailureStatus status) {
                
                
            }
            
        }
        
    }
    
    
    /**
     * Unsupported. Use built-in Android APIs if using Android 4.3+.
     */
    private static final class RemoteControlTargetManager extends AVRCP {
        
        public RemoteControlTargetManager(TargetEventCallback eventCallback) throws ServerNotReachableException, RemoteControlAlreadyRegisteredException {
            super(TYPE_TARGET, eventCallback);
        }
        
        public interface TargetEventCallback extends EventCallback {
            
        }
        
    }
    
    /**
     * Thrown when a new Remote Control Manager is attempting to register when another
     * manager is already registered for the same AVRCP role.
     */
    public static class RemoteControlAlreadyRegisteredException extends BluetopiaPMException {
        public RemoteControlAlreadyRegisteredException() {
            super("Remote Control Manager is already registered for the given role");
        }
        
        public RemoteControlAlreadyRegisteredException(Throwable throwable) {
            super("Remote Control Manager is already registered for the given role", throwable);
        }
    }
    
    /**
     * Equalizer Player Application Setting Attribute ID.<br>
     * <p>
     * Possible corresponding values:<br>
     * <ul>
     * <li>{@link AVRCP#EQUALIZER_ATTRIBUTE_VALUE_OFF}</li>
     * <li>{@link AVRCP#EQUALIZER_ATTRIBUTE_VALUE_ON}</li>
     * </ul>
     */
    public static final byte PLAYER_APP_SETTING_ATTRIBUTE_EQUALIZER   = 0x01;
    /**
     * Repeat Mode Player Application Setting Attribute ID.<br>
     * <p>
     * Possible corresponding values:<br>
     * <ul>
     * <li>{@link AVRCP#REPEAT_MODE_ATTRIBUTE_VALUE_OFF}</li>
     * <li>{@link AVRCP#REPEAT_MODE_ATTRIBUTE_VALUE_SINGLE_TRACK}</li>
     * <li>{@link AVRCP#REPEAT_MODE_ATTRIBUTE_VALUE_ALL_TRACKS}</li>
     * <li>{@link AVRCP#REPEAT_MODE_ATTRIBUTE_VALUE_GROUP}</li>
     * </ul>
     */
    public static final byte PLAYER_APP_SETTING_ATTRIBUTE_REPEAT_MODE = 0x02;
    /**
     * Shuffle Player Application Setting Attribute ID.<br>
     * <p>
     * Possible corresponding values:<br>
     * <ul>
     * <li>{@link AVRCP#SHUFFLE_ATTRIBUTE_VALUE_OFF}</li>
     * <li>{@link AVRCP#SHUFFLE_ATTRIBUTE_VALUE_ALL_TRACKS}</li>
     * <li>{@link AVRCP#SHUFFLE_ATTRIBUTE_VALUE_GROUP}</li>
     * </ul>
     */
    public static final byte PLAYER_APP_SETTING_ATTRIBUTE_SHUFFLE     = 0x03;
    /**
     * Scan Player Application Setting Attribute ID.<br>
     * <p>
     * Possible corresponding values:<br>
     * <ul>
     * <li>{@link AVRCP#SCAN_ATTRIBUTE_VALUE_OFF}</li>
     * <li>{@link AVRCP#SCAN_ATTRIBUTE_VALUE_ALL_TRACKS}</li>
     * <li>{@link AVRCP#SCAN_ATTRIBUTE_VALUE_GROUP}</li>
     * </ul>
     */
    public static final byte PLAYER_APP_SETTING_ATTRIBUTE_SCAN        = 0x04;
    
    /* Equalizer setting values. */
    /**
     * Equalizer player application setting value - OFF.       
     */
    public static final byte EQUALIZER_ATTRIBUTE_VALUE_OFF            = 0x01;
    /**
     * Equalizer player application setting value - ON.       
     */
    public static final byte EQUALIZER_ATTRIBUTE_VALUE_ON             = 0x02;
    
    /* Repeat setting values. */
    /**
     * Repeat Mode player application setting value - OFF.       
     */
    public static final byte REPEAT_MODE_ATTRIBUTE_VALUE_OFF          = 0x01;
    /**
     * Repeat Mode player application setting value - Single Track.       
     */
    public static final byte REPEAT_MODE_ATTRIBUTE_VALUE_SINGLE_TRACK = 0x02;
    /**
     * Repeat Mode player application setting value - All Tracks.       
     */
    public static final byte REPEAT_MODE_ATTRIBUTE_VALUE_ALL_TRACKS   = 0x03;
    /**
     * Repeat Mode player application setting value - Group.       
     */
    public static final byte REPEAT_MODE_ATTRIBUTE_VALUE_GROUP        = 0x04;
    
    /* Shuffle setting values. */
    /**
     * Shuffle player application setting value - OFF.       
     */
    public static final byte SHUFFLE_ATTRIBUTE_VALUE_OFF              = 0x01;
    /**
     * Shuffle player application setting value - All tracks.       
     */
    public static final byte SHUFFLE_ATTRIBUTE_VALUE_ALL_TRACKS       = 0x02;
    /**
     * Shuffle player application setting value - Group.       
     */
    public static final byte SHUFFLE_ATTRIBUTE_VALUE_GROUP            = 0x03;
    
    /* Scan setting values. */
    /**
     * Scan player application setting value - OFF.       
     */
    public static final byte SCAN_ATTRIBUTE_VALUE_OFF                 = 0x01;
    /**
     * Scan player application setting value - All tracks.       
     */
    public static final byte SCAN_ATTRIBUTE_VALUE_ALL_TRACKS          = 0x02;
    /**
     * Scan player application setting value - Group.       
     */
    public static final byte SCAN_ATTRIBUTE_VALUE_GROUP               = 0x03;
    
    /* The following constants are used with the ConnectFlags member of */
    /* the HFRM_Connect_Remote_Device_Request_t message to control */
    /* various connection options. */
    private final static int CONNECTION_FLAGS_REQUIRE_AUTHENTICATION = 1;
    private final static int CONNECTION_FLAGS_REQUIRE_ENCRYPTION     = 2;

    /**
     * Optional flags to control behavior when a remote connection is
     * established by the local service.
     *
     * @see #connectAudioStream
     */
    public enum ConnectionFlags {
        /**
         * Require the Bluetooth link to be authenticated. This requires that
         * the devices are paired and will automatically initiate the pairing
         * process if required.
         */
        REQUIRE_AUTHENTICATION,

        /**
         * Require the Bluetooth link to be encrypted.
         */
        REQUIRE_ENCRYPTION
    }

    /* The following constants are used with the ConnectionFlags member  */
    /* of the HFRM_Change_Incoming_Connection_Flags_Request_t structure  */
    /* to specify the various flags to apply to incoming Connections.    */
    private final static int INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION  = 1;
    private final static int INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION = 2;
    private final static int INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION     = 4;

    /**
     * Optional flags to control behavior when a remote connection is
     * established by a remote device.
     *
     * @see #changeIncomingConnectionFlags
     */
    public enum IncomingConnectionFlags {
        /**
         * Require incoming connections be approved by the Manager. This will
         * generate a callback for every incoming connection which must be
         * approved by the controlling Manager before the profile connection is
         * allowed to be completed.
         */
        REQUIRE_AUTHORIZATION,

        /**
         * Require the Bluetooth link to be authenticated. This requires that
         * the devices are paired and will automatically initiate the pairing
         * process if required.
         */
        REQUIRE_AUTHENTICATION,

        /**
         * Require the Bluetooth link to be encrypted.
         */
        REQUIRE_ENCRYPTION
    }

    private final static int CONNECTION_STATUS_SUCCESS                  = 0;
    private final static int CONNECTION_STATUS_FAILURE_TIMEOUT          = 1;
    private final static int CONNECTION_STATUS_FAILURE_REFUSED          = 2;
    private final static int CONNECTION_STATUS_FAILURE_SECURITY         = 3;
    private final static int CONNECTION_STATUS_FAILURE_DEVICE_POWER_OFF = 4;
    private final static int CONNECTION_STATUS_FAILURE_UNKNOWN          = 5;

    /**
     * Possible results of a connection attempt.
     *
     * @see EventCallback#audioStreamConnectionStatusEvent
     */
    public enum ConnectionStatus {
        /**
         * The connection succeeded.
         */
        SUCCESS,

        /**
         * The remote device did not respond.
         */
        FAILURE_TIMEOUT,

        /**
         * The remote device actively refused the connection.
         */
        FAILURE_REFUSED,

        /**
         * One or both devices required authentication and/or encryption which
         * could not be satisfied.
         */
        FAILURE_SECURITY,

        /**
         * The local Bluetooth radio was powered down during the connection
         * process.
         */
        FAILURE_DEVICE_POWER_OFF,

        /**
         * An internal error occurred during the connection attempt.
         */
        FAILURE_UNKNOWN
    }

    private final static int REMOTE_CONTROL_DISCONNECT_REASON_DISCONNECT = 0;
    private final static int REMOTE_CONTROL_DISCONNECT_REASON_LINK_LOSS  = 1;
    private final static int REMOTE_CONTROL_DISCONNECT_REASON_TIMEOUT    = 2;

    /**
     * Possible reasons for the disconnection of a Remote Control session.
     */
    public enum RemoteControlDisconnectReason {
        /**
         * The remote control session was disconnected by request of one of the
         * connected devices.
         */
        DISCONNECT,

        /**
         * The remote control session was disconnected due to an error at the
         * Link layer (i.e., devices are out of range).
         */
        LINK_LOSS,

        /**
         * The remote control session was disconnected due to a timeout while
         * waiting for a response to a command.
         */
        TIMEOUT
    }

    /**
     * Possible types of commands that are sent to a particular
     * AV unit or subunit. <p>
     *
     * <i>Note:</i> These values are defined in the IEEE <i>1394 AV/C
     * Digital Interface Command Set</i> specification.
     */
    public enum RemoteControlCommandType {
        /**
         * Sent to instruct a device to perform an operation.
         */
        CONTROL((byte)0x00),

        /**
         * Sent to query the current status of a device.
         */
        STATUS((byte)0x01),

        /**
         * Sent to determine whether a device supports a particular
         * control command.
         */
        SPECIFIC_INQUIRY((byte)0x02),

        /**
         * Sent to indicate the desire receive notification of
         * future state changes.
         */
        NOTIFY((byte)0x03),

        /**
         * Sent to determine whether a device supports a particular
         * control command <i>without</i> being required to specify
         * a particular set of parameters for that command.
         */
        GENERAL_INQUIRY((byte)0x04);
        
        /*package*/byte value;
        
        private RemoteControlCommandType(byte value) {
            this.value = value;
        }
        
        /*package*/static RemoteControlCommandType fromValue(byte value) {
            RemoteControlCommandType type = null;
            
            for(RemoteControlCommandType rcct : RemoteControlCommandType.values()) {
                if(rcct.value == value) {
                    type = rcct;
                    break;
                }
            }
            
            return type;
        }
    }

    /**
     * Possible response codes.<p>
     *
     * <i>Note:</i> The meaning of the response codes may vary depending on the type of
     * command sent.  The specific meanings and values of these codes are defined in
     * the IEEE <i>1394 AV/C Digital Interface Command Set</i> specification.
     */
    public enum RemoteControlResponseCode {
        /**
         * The device does not support the command.
         */
        NOT_IMPLEMENTED((byte)0x08),

        /**
         * The device supports the command and the current state
         * permits execution.<p>
         *
         * <i>Note:</i> execution of control commands may not
         * necessarily be completed at the time an ACCEPTED
         * response is returned.
         */
        ACCEPTED((byte)0x09),

        /**
         * The device supports the command or event notification.
         * However, the current state does not permit execution
         * of the command, or the requested information cannot
         * currently be supplied.
         */
        REJECTED((byte)0x0A),

        /**
         * The device supports the command but is currently in a
         * state of transition.<p>
         *
         * <i>Note:</i> A subsequent status command (at some
         * unspecified future time) may result in a STABLE state.
         */
        IN_TRANSITION((byte)0x0B),

        /**
         * The device supports the command and the requested
         * information has been returned.<p>
         *
         * Also corresponds to an <i>IMPLEMENTED</i> response to
         * an inquiry.
         */
        STABLE((byte)0x0C),

        /**
         * The target supports the specified event notification, and
         * the
         *

         */
        CHANGED((byte)0x0D),

        /**
         * For control commands, indicates that the device supports
         * the command but is unable to respond with ACCEPTED or REJECTED
         * within 100ms.  Unless a subsequent bus reset causes the
         * transaction to be aborted, the device will ultimately return
         * an ACCEPTED or REJECTED response.<p>
         *
         * For notification commands, indicates that the device supports
         * the requested event notification and has accepted the notify
         * command for any future change of state.  At some future time,
         * the device will return a REJECTED or CHANGED response code.
         */
        INTERIM((byte)0x0F);
        
        /*package*/byte value;
        
        private RemoteControlResponseCode(byte value) {
            this.value = value;
        }
        
        /*package*/static RemoteControlResponseCode fromValue(byte value) {
            RemoteControlResponseCode code = null;
            
            for(RemoteControlResponseCode rc: RemoteControlResponseCode.values()) {
                if(rc.value == value) {
                    code = rc;
                    break;
                }
            }
            
            return code;
        }
    }

    /**
     * Possible subunit types.<p>
     *
     * <i>Note:</i> These values are defined in the IEEE <i>1394 AV/C
     * Digital Interface Command Set</i> specification.
     */
    public enum RemoteControlSubunitType {
        /**
         * A video monitor.
         */
        VIDEO_MONITOR((byte)0x00),

        /**
         * A disc recorder or player (audio or video).
         */
        DISC_RECORDER_PLAYER((byte)0x03),

        /**
         * A tape recorder or player (audio or video).
         */
        TAPE_RECORDER_PLAYER((byte)0x04),

        /**
         * A tuner.
         */
        TUNER((byte)0x05),

        /**
         * A video camera.
         */
        VIDEO_CAMERA((byte)0x07),

        /**
         * A user interface panel.
         */
        PANEL((byte)0x09),

        /**
         * A vendor specific subunit.
         */
        VENDOR_SPECIFIC((byte)0x1C),

        /**
         * The subunit type is extended to the next byte.
         */
        EXTENDED((byte)0x1E),

        /**
         *  Refers to the entire AV unit instead of one of its subunits,
         *  so long as the specified subunit ID is
         *  {@link RemoteControlSubunitID#IGNORE}.
         */
        UNIT((byte)0x1F);
        
        /*package*/byte value;
        
        private RemoteControlSubunitType(byte value) {
            this.value = value;
        }
        
        /*package*/static RemoteControlSubunitType fromValue(byte value) {
            RemoteControlSubunitType type = null;
            
            for(RemoteControlSubunitType rcst : RemoteControlSubunitType.values()) {
                if(rcst.value == value) {
                    type = rcst;
                    break;
                }
            }
            
            return type;
        }
    }

    /**
     * Possible subunit identifiers.<p>
     *
     * <i>Note:</i> These values are defined in the IEEE <i>1394 AV/C
     * Digital Interface Command Set</i> specification, section 5.3.3.
     */
    public enum RemoteControlSubunitID {
        /**
         * Instance 0.
         */
        INSTANCE_0((byte)0x00),

        /**
         * Instance 1.
         */
        INSTANCE_1((byte)0x01),

        /**
         * Instance 2.
         */
        INSTANCE_2((byte)0x02),

        /**
         * Instance 3.
         */
        INSTANCE_3((byte)0x03),

        /**
         * Instance 4.
         */
        INSTANCE_4((byte)0x04),

        /**
         * The subunit ID is extended to the next byte.
         */
        EXTENDED((byte)0x05),

        /**
         * Used in conjunction with
         * {@link RemoteControlSubunitType#UNIT} to refer to the
         * complete AV unit instead of one of its subunits.
         */
        IGNORE((byte)0x07);
        
        /*package*/byte value;
        
        private RemoteControlSubunitID(byte value) {
            this.value = value;
        }
        
        /*package*/static RemoteControlSubunitID fromValue(byte value) {
            RemoteControlSubunitID id = null;
            
            for(RemoteControlSubunitID rcsi : RemoteControlSubunitID.values()) {
                if(rcsi.value == value) {
                    id = rcsi;
                    break;
                }
            }
            
            return id;
        }
    }
    
    private final static int AVRCP_PASS_THROUGH_ID_SELECT              = 0;
    private final static int AVRCP_PASS_THROUGH_ID_UP                  = 1;
    private final static int AVRCP_PASS_THROUGH_ID_DOWN                = 2;
    private final static int AVRCP_PASS_THROUGH_ID_LEFT                = 3;
    private final static int AVRCP_PASS_THROUGH_ID_RIGHT               = 4;
    private final static int AVRCP_PASS_THROUGH_ID_RIGHT_UP            = 5;
    private final static int AVRCP_PASS_THROUGH_ID_RIGHT_DOWN          = 6;
    private final static int AVRCP_PASS_THROUGH_ID_LEFT_UP             = 7;
    private final static int AVRCP_PASS_THROUGH_ID_LEFT_DOWN           = 8;
    private final static int AVRCP_PASS_THROUGH_ID_ROOT_MENU           = 9;
    private final static int AVRCP_PASS_THROUGH_ID_SETUP_MENU          = 10;
    private final static int AVRCP_PASS_THROUGH_ID_CONTENTS_MENU       = 11;
    private final static int AVRCP_PASS_THROUGH_ID_FAVORITE_MENU       = 12;
    private final static int AVRCP_PASS_THROUGH_ID_EXIT                = 13;
    private final static int AVRCP_PASS_THROUGH_ID_0                   = 14;
    private final static int AVRCP_PASS_THROUGH_ID_1                   = 15;
    private final static int AVRCP_PASS_THROUGH_ID_2                   = 16;
    private final static int AVRCP_PASS_THROUGH_ID_3                   = 17;
    private final static int AVRCP_PASS_THROUGH_ID_4                   = 18;
    private final static int AVRCP_PASS_THROUGH_ID_5                   = 19;
    private final static int AVRCP_PASS_THROUGH_ID_6                   = 20;
    private final static int AVRCP_PASS_THROUGH_ID_7                   = 21;
    private final static int AVRCP_PASS_THROUGH_ID_8                   = 22;
    private final static int AVRCP_PASS_THROUGH_ID_9                   = 23;
    private final static int AVRCP_PASS_THROUGH_ID_DOT                 = 24;
    private final static int AVRCP_PASS_THROUGH_ID_ENTER               = 25;
    private final static int AVRCP_PASS_THROUGH_ID_CLEAR               = 26;
    private final static int AVRCP_PASS_THROUGH_ID_CHANNEL_UP          = 27;
    private final static int AVRCP_PASS_THROUGH_ID_CHANNEL_DOWN        = 28;
    private final static int AVRCP_PASS_THROUGH_ID_PREVIOUS_CHANNEL    = 29;
    private final static int AVRCP_PASS_THROUGH_ID_SOUND_SELECT        = 30;
    private final static int AVRCP_PASS_THROUGH_ID_INPUT_SELECT        = 31;
    private final static int AVRCP_PASS_THROUGH_ID_DISPLAY_INFORMATION = 32;
    private final static int AVRCP_PASS_THROUGH_ID_HELP                = 33;
    private final static int AVRCP_PASS_THROUGH_ID_PAGE_UP             = 34;
    private final static int AVRCP_PASS_THROUGH_ID_PAGE_DOWN           = 35;
    private final static int AVRCP_PASS_THROUGH_ID_POWER               = 36;
    private final static int AVRCP_PASS_THROUGH_ID_VOLUME_UP           = 37;
    private final static int AVRCP_PASS_THROUGH_ID_VOLUME_DOWN         = 38;
    private final static int AVRCP_PASS_THROUGH_ID_MUTE                = 39;
    private final static int AVRCP_PASS_THROUGH_ID_PLAY                = 40;
    private final static int AVRCP_PASS_THROUGH_ID_STOP                = 41;
    private final static int AVRCP_PASS_THROUGH_ID_PAUSE               = 42;
    private final static int AVRCP_PASS_THROUGH_ID_RECORD              = 43;
    private final static int AVRCP_PASS_THROUGH_ID_REWIND              = 44;
    private final static int AVRCP_PASS_THROUGH_ID_FAST_FORWARD        = 45;
    private final static int AVRCP_PASS_THROUGH_ID_EJECT               = 46;
    private final static int AVRCP_PASS_THROUGH_ID_FORWARD             = 47;
    private final static int AVRCP_PASS_THROUGH_ID_BACKWARD            = 48;
    private final static int AVRCP_PASS_THROUGH_ID_ANGLE               = 49;
    private final static int AVRCP_PASS_THROUGH_ID_SUBPICTURE          = 50;
    private final static int AVRCP_PASS_THROUGH_ID_F1                  = 51;
    private final static int AVRCP_PASS_THROUGH_ID_F2                  = 52;
    private final static int AVRCP_PASS_THROUGH_ID_F3                  = 53;
    private final static int AVRCP_PASS_THROUGH_ID_F4                  = 54;
    private final static int AVRCP_PASS_THROUGH_ID_F5                  = 55;
    private final static int AVRCP_PASS_THROUGH_ID_VENDOR_UNIQUE       = 56;

    /**
     * Possible remote control pass-through operation identifiers.<p>
     *
     */
    public enum RemoteControlPassThroughOperationID {
        /**
         * The <i>select</i> operation.
         */
        SELECT,

        /**
         * The <i>up</i> operation.
         */
        UP,

        /**
         * The <i>down</i> operation.
         */
        DOWN,

        /**
         * The <i>left</i> operation.
         */
        LEFT,

        /**
         * The <i>right</i> operation.
         */
        RIGHT,

        /**
         * The <i>right/up</i> operation.
         */
        RIGHT_UP,

        /**
         * The <i>right/down</i> operation.
         */
        RIGHT_DOWN,

        /**
         * The <i>left/up</i> operation.
         */
        LEFT_UP,

        /**
         * The <i>left/down</i> operation.
         */
        LEFT_DOWN,

        /**
         * The <i>root menu</i> operation.
         */
        ROOT_MENU,

        /**
         * The <i>setup menu</i> operation.
         */
        SETUP_MENU,

        /**
         * The <i>contents menu</i> operation.
         */
        CONTENTS_MENU,

        /**
         * The <i>favorite menu</i> operation.
         */
        FAVORITE_MENU,

        /**
         * The <i>exit</i> operation.
         */
        EXIT,

        /**
         * The <i>number 0</i> operation.
         */
        NUM_0,

        /**
         * The <i>number 1</i> operation.
         */
        NUM_1,

        /**
         * The <i>number 2</i> operation.
         */
        NUM_2,

        /**
         * The <i>number 3</i> operation.
         */
        NUM_3,

        /**
         * The <i>number 4</i> operation.
         */
        NUM_4,

        /**
         * The <i>number 5</i> operation.
         */
        NUM_5,

        /**
         * The <i>number 6</i> operation.
         */
        NUM_6,

        /**
         * The <i>number 7</i> operation.
         */
        NUM_7,

        /**
         * The <i>number 8</i> operation.
         */
        NUM_8,

        /**
         * The <i>number 9</i> operation.
         */
        NUM_9,

        /**
         * The <i>dot</i> operation.
         */
        DOT,

        /**
         * The <i>enter</i> operation.
         */
        ENTER,

        /**
         * The <i>clear</i> operation.
         */
        CLEAR,

        /**
         * The <i>channel up</i> operation.
         */
        CHANNEL_UP,

        /**
         * The <i>channel down</i> operation.
         */
        CHANNEL_DOWN,

        /**
         * The <i>previous channel</i> operation.
         */
        PREVIOUS_CHANNEL,

        /**
         * The <i>sound select</i> operation.
         */
        SOUND_SELECT,

        /**
         * The <i>input select</i> operation.
         */
        INPUT_SELECT,

        /**
         * The <i>display information</i> operation.
         */
        DISPLAY_INFORMATION,

        /**
         * The <i>help</i> operation.
         */
        HELP,

        /**
         * The <i>page up</i> operation.
         */
        PAGE_UP,

        /**
         * The <i>page down</i> operation.
         */
        PAGE_DOWN,

        /**
         * The <i>power</i> operation.
         */
        POWER,

        /**
         * The <i>volume up</i> operation.
         */
        VOLUME_UP,

        /**
         * The <i>volume down</i> operation.
         */
        VOLUME_DOWN,

        /**
         * The <i>mute</i> operation.
         */
        MUTE,

        /**
         * The <i>play</i> operation.
         */
        PLAY,

        /**
         * The <i>stop</i> operation.
         */
        STOP,

        /**
         * The <i>pause</i> operation.
         */
        PAUSE,

        /**
         * The <i>record</i> operation.
         */
        RECORD,

        /**
         * The <i>rewind</i> operation.
         */
        REWIND,

        /**
         * The <i>fast forward</i> operation.
         */
        FAST_FORWARD,

        /**
         * The <i>eject</i> operation.
         */
        EJECT,

        /**
         * The <i>forward</i> operation.
         */
        FORWARD,

        /**
         * The <i>backward</i> operation.
         */
        BACKWARD,

        /**
         * The <i>angle</i> operation.
         */
        ANGLE,

        /**
         * The <i>subpicture</i> operation.
         */
        SUBPICTURE,

        /**
         * The <i>F1</i> operation.
         */
        F1,

        /**
         * The <i>F2</i> operation.
         */
        F2,

        /**
         * The <i>F3</i> operation.
         */
        F3,

        /**
         * The <i>F4</i> operation.
         */
        F4,

        /**
         * The <i>F5</i> operation.
         */
        F5,

        /**
         * A vendor specific operation.
         */
        VENDOR_UNIQUE;
    }

    /**
     * Defines the state of a remote control button.
     */
    public enum RemoteControlButtonState {
        /**
         * The button is pressed.
         */
        PRESSED,

        /**
         * The button is released.
         */
        RELEASED
    }
    
    /**
     * Defines a company id type.
     */
    public static class CompanyID {
        /**
         * The most significant byte of the ID.
         */
        public byte id0;

        /**
         * The most significant byte of the ID.
         */
        public byte id1;

        /**
         * The least significant byte of the ID.
         */
        public byte id2;

        /**
         * Constructs a Company ID.
         *
         * @param mostSignificant The most significant byte of the ID.
         * @param middleSignificant The most significant byte of the ID.
         * @param leastSignificant The least significant byte of the ID.
         */
        public CompanyID(byte mostSignificant, byte middleSignificant, byte leastSignificant) {
            id0 = mostSignificant;
            id1 = middleSignificant;
            id2 = leastSignificant;
        }

        /**
         * The Company ID assigned by the Bluetooth SIG.
         */
        public static final CompanyID BLUETOOTH_SIG_COMPANY_ID = new CompanyID((byte)0x00, (byte)0x19, (byte)0x58);

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof CompanyID) {
                CompanyID other = (CompanyID)obj;
                return ((id0 == other.id0) && (id1 == other.id1) && (id2 == other.id2));
            }
            else
                return false;
        }
        
        @Override
        public String toString() {
            return String.format("%02X%02X%02X", id0, id1, id2);
        }
    }
    
    /**
     * The types of group navigation available.
     *
     */
    public enum GroupNavigationType {
        /**
         * Navigate to the next group.
         */
        NEXT_GROUP(0x01),
        
        /**
         * Navigate to the previous group.
         */
        PREV_GROUP(0x02);
        
        /*package*/int value;
        
        private GroupNavigationType(int value) {
            this.value = value;
        }
        
        /*package*/static GroupNavigationType fromValue(int value) {
            GroupNavigationType type = null;
            
            for(GroupNavigationType gnt : GroupNavigationType.values()) {
                if(gnt.value == value) {
                    type = gnt;
                    break;
                }
            }
            
            return type;
        }
    }

    /**
     * The types of capability sets which can be requested
     * from a remote control endpoint.
     *
     */
    public enum CapabilityID {
        /**
         * The list of supported companyIDs.
         * @see AVRCP#BLUETOOTH_SIG_COMPANY_ID
         */
        COMPANY_ID(0x02),

        /**
         * The list of notifiable events the Target
         * supports registration for.
         */
        EVENTS_SUPPORTED(0x03);
        
        /*package*/int value;
        
        private CapabilityID(int value) {
            this.value = value;
        }
        
        /*package*/static CapabilityID fromValue(int value) {
            CapabilityID capability = null;
            
            for(CapabilityID cap : CapabilityID.values()) {
                if(cap.value == value) {
                    capability = cap;
                    break;
                }
            }
            
            return capability;
        }
    }
    
    /**
     * Defines the types of battery statuses that can be informed.
     */
    public enum BatteryStatus {
        /**
         * Normal battery status.
         */
        NORMAL(0x00),
        
        /**
         * Device will be unable to operate soon.
         */
        WARNING(0x01),
        
        /**
         * Device can no longer operate.
         */
        CRITICAL(0x02),
        
        /**
         * Connected to an external power supply.
         */
        EXTERNAL(0x03),
        
        /**
         * Device is completely charged.
         */
        FULL_CHARGE(0x04);
        
        /*package*/int value;
        
        private BatteryStatus(int value) {
            this.value = value;
        }
        
        /*package*/static BatteryStatus fromValue(int value) {
            BatteryStatus status = null;
            
            for(BatteryStatus bs : BatteryStatus.values()) {
                if(bs.value == value) {
                    status = bs;
                    break;
                }
            }
            
            return status;
        }
    }
    
    

    /**
     * The possible media meta-data elements.
     *
     */
    public enum ElementAttributeID {
        /**
         * Title of the Media
         */
        TITLE(0x01),

        /**
         * Artist of the Media.
         */
        ARTIST(0x02),

        /**
         * Album of the Media.
         */
        ALBUM(0x03),

        /**
         * Number of the Media (ex. Track Number).
         */
        NUMBER_OF_MEDIA(0x04),

        /**
         * Total number of Media (ex. Number of tracks on album).
         */
        TOTAL_NUMBER_OF_MEDIA(0x05),

        /**
         * Genre of the media.
         */
        GENRE(0x06),

        /**
         * Playing time of the Media.
         * <br>
         * NOTE: It is returned in milliseconds.
         */
        PLAYING_TIME(0x07);
        
        private static final int BIT_TITLE                 = 0x00000001;
        private static final int BIT_ARTIST                = 0x00000002;
        private static final int BIT_ALBUM                 = 0x00000004;
        private static final int BIT_NUMBER_OF_MEDIA       = 0x00000008;
        private static final int BIT_TOTAL_NUMBER_OF_MEDIA = 0x00000010;
        private static final int BIT_GENRE                 = 0x00000020;
        private static final int BIT_PLAYING_TIME          = 0x00000040;
        
        /*package*/ int value;
        
        private ElementAttributeID(int value) {
            this.value = value;
        }
        
        /*package*/static ElementAttributeID fromValue(int value) {
            ElementAttributeID attribute = null;
            
            for(ElementAttributeID att : ElementAttributeID.values()) {
                if(att.value == value) {
                    attribute = att;
                    break;
                }
            }
            
            return attribute;
        }
        
        /*package*/static EnumSet<ElementAttributeID> fromMask(int mask) {
            EnumSet<ElementAttributeID> set = EnumSet.noneOf(ElementAttributeID.class);
            if((mask & BIT_TITLE) > 0)
                set.add(TITLE);
            if((mask & BIT_ARTIST) > 0)
                set.add(ARTIST);
            if((mask & BIT_ALBUM) > 0)
                set.add(ALBUM);
            if((mask & BIT_NUMBER_OF_MEDIA) > 0)
                set.add(NUMBER_OF_MEDIA);
            if((mask & BIT_TOTAL_NUMBER_OF_MEDIA) > 0)
                set.add(TOTAL_NUMBER_OF_MEDIA);
            if((mask & BIT_GENRE) > 0)
                set.add(GENRE);
            if((mask & BIT_PLAYING_TIME) > 0)
                set.add(PLAYING_TIME);
            
            return set;
        }
        
        /*package*/static int toMask(EnumSet<ElementAttributeID> set) {
            int mask = 0;
            
            if(set.contains(TITLE))
                mask |= BIT_TITLE;
            if(set.contains(ARTIST))
                mask |= BIT_ARTIST;
            if(set.contains(ALBUM))
                mask |= BIT_ALBUM;
            if(set.contains(NUMBER_OF_MEDIA))
                mask |= BIT_NUMBER_OF_MEDIA;
            if(set.contains(TOTAL_NUMBER_OF_MEDIA))
                mask |= BIT_TOTAL_NUMBER_OF_MEDIA;
            if(set.contains(GENRE))
                mask |= BIT_GENRE;
            if(set.contains(PLAYING_TIME))
                mask |= BIT_PLAYING_TIME;
            
            return mask;
        }
    }
    
    /**
     * Defines an element attribute for media meta-data.
     *
     */
    public static class ElementAttribute {
        /**
         * The type of attribute represented.
         */
        public ElementAttributeID attributeID;

        /**
         * The character set code is defined by the
         * IANA MIBenum list.
         */
        public short characterSet;

        /**
         * The raw attribute data for the element.
         */
        public byte[] attributeData;

        /**
         * Constructs an element attribute for media meta-data.
         *
         * @param attributeID The type of attribute represented.
         * @param characterSet The character set code is defined by the
         * IANA MIBenum list.
         * @param attributeData The raw attribute data for the element.
         */
        public ElementAttribute(ElementAttributeID attributeID, short characterSet, byte[] attributeData) {
            this.attributeID = attributeID;
            this.characterSet = characterSet;
            this.attributeData = attributeData;
        }

        /**
         * This function checks the {@link #characterSet} and will return a decoded
         * String if the character set is one of the defined Java Standard sets.
         * 
         * @see Charset
         * 
         * @return A String representation of the raw textData or {@code null} if the characterSet is not supported.
         */
        public String decodeAttributeData() {
            Charset charset;
            switch(characterSet) {
            case CHARACTER_SET_US_ASCII:
                charset = Charset.forName("US_ASCII");
                break;
            case CHARACTER_SET_ISO_8859_1:
                charset = Charset.forName("ISO_8859_1");
                break;
            case CHARACTER_SET_UTF_8:
                charset = Charset.forName("UTF_8");
                break;
            case CHARACTER_SET_UTF_16_BE:
                charset = Charset.forName("UTF_16BE");
                break;
            case CHARACTER_SET_UTF_16_LE:
                charset = Charset.forName("UTF_16LE");
                break;
            case CHARACTER_SET_UTF_16:
                charset = Charset.forName("UTF_16");
                break;
            default:
                return null;
            }
            
            return new String(attributeData, charset);
        }
    }

    /**
     * The possible play statuses of a Target.
     *
     */
    public enum PlayStatus {
        /**
         * Media is stopped.
         */
        STOPPED(0x00),

        /**
         * Media is playing.
         */
        PLAYING(0x01),

        /**
         * Media is paused.
         */
        PAUSED(0x02),

        /**
         * Media is seeking forward.
         */
        FWD_SEEK(0x03),

        /**
         * Media is seeking backward.
         */
        REV_SEEK(0x04),

        /**
         * The media encountered an error.
         */
        ERROR(0xFF);
        
        /*package*/int value;
        
        private PlayStatus(int value) {
            this.value = value;
        }
        
        /*package*/static PlayStatus fromValue(int value) {
            PlayStatus status = null;
            
            for(PlayStatus ps : PlayStatus.values()) {
                if(ps.value == value) {
                    status = ps;
                    break;
                }
            }
            
            return status;
        }
    }

    /**
     * The possible system statuses.
     *
     */
    public enum SystemStatus {
        /**
         * System is powered on.
         */
        POWER_ON(0x00),

        /**
         * System is powered off.
         */
        POWER_OFF(0x01),

        /**
         * System is unplugged.
         */
        UNPLUGGED(0x02);
        
        /*package*/int value;
        
        private SystemStatus(int value) {
            this.value = value;
        }
        
        /*package*/static SystemStatus fromValue(int value) {
            SystemStatus status = null;
            
            for(SystemStatus ss : SystemStatus.values()) {
                if(ss.value == value) {
                    status = ss;
                    break;
                }
            }
            
            return status;
        }
    }
    
    /**
     * The Media Identifier which corresponds to the currently playing media.
     */
    public static final long MEDIA_INDENTIFIER_CURRENTLY_PLAYING = 0x00;
    
    /**
     * The IANA MIBenum value for the US ASCII character set.
     */
    public static final short CHARACTER_SET_US_ASCII   = 3;
    /**
     * The IANA MIBenum value for the ISO 8859-1 character set.
     */
    public static final short CHARACTER_SET_ISO_8859_1 = 4;
    /**
     * The IANA MIBenum value for the UTF-8 character set.
     */
    public static final short CHARACTER_SET_UTF_8      = 106;
    /**
     * The IANA MIBenum value for the UTF-16 character set
     * (Big Endian).
     */
    public static final short CHARACTER_SET_UTF_16_BE  = 1013;
    /**
     * The IANA MIBenum value for the UTF-16 character set
     * (Little Endian).
     */
    public static final short CHARACTER_SET_UTF_16_LE  = 1014;
    /**
     * The IANA MIBenum value for the UTF-16 character set
     * (w/ Byte Order Mark).
     */
    public static final short CHARACTER_SET_UTF_16     = 1015;
    
    /**
     * The possible Notification Events which can be
     * registered for on a Target.
     *
     */
    public enum EventID {
        /**
         * Notifies when the playback status has changed.
         */
        PLAYBACK_STATUS_CHANGED(0x01),

        /**
         * Notifies when the track has changed.
         */
        TRACK_CHANGED(0x02),

        /**
         * Notifies when the end of a track is reached.
         */
        TRACK_REACHED_END(0x03),

        /**
         * Notifies when the start of a track is reached.
         */
        TRACK_REACHED_START(0x04),

        /**
         * Notifies when the playback position has changed passed the
         * specified playback interval.
         */
        PLAYBACK_POSITION_CHANGED(0x05),

        /**
         * Notifies when the battery status of the remote device has changed.
         */
        BATTERY_STATUS_CHANGED(0x06),

        /**
         * Notifies when the system status has changed.
         */
        SYSTEM_STATUS_CHANGED(0x07),

        /**
         * Notifies when a setting on the remote media player application has changed.
         */
        PLAYER_APPLICATION_SETTING_CHANGED(0x08),

        /**
         * Notifies when the content in the Now Playing list has changed.
         */
        NOW_PLAYING_CONTENT_CHANGED(0x09),

        /**
         * This event is not currently supported.
         */
        AVAILABLE_PLAYERS_CHANGED(0x0A), //XXX

        /**
         * This event is not currently supported.
         */
        ADDRESSED_PLAYER_CHANGED(0x0B), //XXX

        /**
         * This event is not currently supported.
         */
        UIDS_CHANGED(0x0C), //XXX

        /**
         * Notifies when the Target changes the volume.
         */
        VOLUME_CHANGED(0x0D);
        
        private static final int BIT_PLAYBACK_STATUS_CHANGED            = 0x00000001;
        private static final int BIT_TRACK_CHANGED                      = 0x00000002;
        private static final int BIT_TRACK_REACHED_END                  = 0x00000004;
        private static final int BIT_TRACK_REACHED_START                = 0x00000008;
        private static final int BIT_PLAYBACK_POSITION_CHANGED          = 0x00000010;
        private static final int BIT_BATTERY_STATUS_CHANGED             = 0x00000020;
        private static final int BIT_SYSTEM_STATUS_CHANGED              = 0x00000040;
        private static final int BIT_PLAYER_APPLICATION_SETTING_CHANGED = 0x00000080;
        private static final int BIT_NOW_PLAYING_CONTENT_CHANGED        = 0x00000100;
        private static final int BIT_AVAILABLE_PLAYERS_CHANGED          = 0x00000200;
        private static final int BIT_ADDRESSED_PLAYER_CHANGED           = 0x00000400;
        private static final int BIT_UIDS_CHANGED                       = 0x00000800;
        private static final int BIT_VOLUME_CHANGED                     = 0x00001000;
        
        /*package*/int value;
        
        private EventID(int value) {
            this.value = value;
        }
        
        /*package*/static EventID fromValue(int value) {
            EventID event = null;
            
            for(EventID eid : EventID.values()) {
                if(eid.value == value) {
                    event = eid;
                    break;
                }
            }
            
            return event;
        }
    
        /*package*/static EnumSet<EventID> fromMask(int mask) {
            EnumSet<EventID> set = EnumSet.noneOf(EventID.class);
            
            if((mask & BIT_PLAYBACK_STATUS_CHANGED) > 0)
                set.add(PLAYBACK_STATUS_CHANGED);
            if((mask &  BIT_TRACK_CHANGED) > 0)
                set.add(TRACK_CHANGED);
            if((mask & BIT_TRACK_REACHED_END) > 0)
                set.add(TRACK_REACHED_END);
            if((mask & BIT_TRACK_REACHED_START) > 0)
                set.add(TRACK_REACHED_START);
            if((mask & BIT_PLAYBACK_POSITION_CHANGED) > 0)
                set.add(PLAYBACK_POSITION_CHANGED);
            if((mask & BIT_BATTERY_STATUS_CHANGED) > 0)
                set.add(BATTERY_STATUS_CHANGED);
            if((mask & BIT_SYSTEM_STATUS_CHANGED) > 0)
                set.add(SYSTEM_STATUS_CHANGED);
            if((mask & BIT_PLAYER_APPLICATION_SETTING_CHANGED) > 0)
                set.add(PLAYER_APPLICATION_SETTING_CHANGED);
            if((mask & BIT_NOW_PLAYING_CONTENT_CHANGED) > 0)
                set.add(NOW_PLAYING_CONTENT_CHANGED);
            if((mask & BIT_AVAILABLE_PLAYERS_CHANGED) > 0)
                set.add(AVAILABLE_PLAYERS_CHANGED);
            if((mask & BIT_ADDRESSED_PLAYER_CHANGED) > 0)
                set.add(ADDRESSED_PLAYER_CHANGED);
            if((mask & BIT_UIDS_CHANGED) > 0)
                set.add(UIDS_CHANGED);
            if((mask & BIT_VOLUME_CHANGED) > 0)
                set.add(VOLUME_CHANGED);
            
            return set;
        }
        
        /*package*/static int toMask(EnumSet<EventID> set) {
            int mask = 0;
            
            //XXX Assumes that the event values are in sequential order (they are)
            for(EventID event : set) {
                mask |= (1 << (event.value - 1));
            }
            
            return mask;
        }
    }
    
    /**
     * Represents the displayable text description of a Player Application Setting attribute or attribute value value.
     */
    public static class PlayerApplicationSettingText {
        /**
         * The identifier of either the attribute or attribute value.
         */
        public byte id;
        
        /**
         * The character set the {@link #textData} is encoded in.
         */
        public short characterSet;
        
        /**
         * The raw undecoded displayable text description.
         */
        public byte[] textData;
        
        /**
         * Constructor
         *
         * @param id 
         * @param characterSet
         * @param textData
         */
        public PlayerApplicationSettingText(byte id, short characterSet, byte[] textData) {
            this.id = id;
            this.characterSet = characterSet;
            this.textData = textData;
        }
        
        /**
         * This function checks the {@link #characterSet} and will return a decoded
         * String if the character set is one of the defined Java Standard sets.
         * 
         * @see Charset
         * 
         * @return A String representation of the raw textData or {@code null} if the characterSet is not supported.
         */
        public String decodeTextData() {
            Charset charset;
            switch(characterSet) {
            case CHARACTER_SET_US_ASCII:
                charset = Charset.forName("US_ASCII");
                break;
            case CHARACTER_SET_ISO_8859_1:
                charset = Charset.forName("ISO_8859_1");
                break;
            case CHARACTER_SET_UTF_8:
                charset = Charset.forName("UTF_8");
                break;
            case CHARACTER_SET_UTF_16_BE:
                charset = Charset.forName("UTF_16BE");
                break;
            case CHARACTER_SET_UTF_16_LE:
                charset = Charset.forName("UTF_16LE");
                break;
            case CHARACTER_SET_UTF_16:
                charset = Charset.forName("UTF_16");
                break;
            default:
                return null;
            }
            
            return new String(textData, charset);
        }
    }
    
    /**
     * The possible AVRCP error codes in a Command Reject Response.
     */
    public enum CommandErrorCode {
        /**
         * The Invalid Command error code.
         */
        INVALID_COMMAND(0x00),
        /**
         * The Invalid Parameter error code.
         */
        INVALID_PARAMETER(0x01),
        /**
         * The Parameter Not Found error code.
         */
        PARAMETER_NOT_FOUND(0x02),
        /**
         * The Internal Error error code.
         */
        INTERNAL_ERROR(0x03),
        /**
         * The Complete No Error error code.
         */
        COMPLETE_NO_ERROR(0x04),
        /**
         * The UID Changed error code.
         */
        UID_CHANGED(0x05),
        /**
         * The Invalid Direction error code.
         */
        INVALID_DIRECTION(0x07),
        /**
         * The Not a Directory error code.
         */
        NOT_A_DIRECTORY(0x08),
        /**
         * The Does Not Exist error code.
         */
        DOES_NOT_EXIST(0x09),
        /**
         * The Invalid Scope error code.
         */
        INVALID_SCOPE(0x0A),
        /**
         * The Range Out of Bounds error code.
         */
        RANGE_OUT_OF_BOUNDS(0x0B),
        /**
         * The UID Is a Directory error code.
         */
        UID_IS_A_DIRECTORY(0x0C),
        /**
         * The Media In Use error code.
         */
        MEDIA_IN_USE(0x0D),
        /**
         * The Now Playing List Full error code.
         */
        NOW_PLAYING_LIST_FULL(0x0E),
        /**
         * The Search Not Supported error code.
         */
        SEARCH_NOT_SUPPORTED(0x0F),
        /**
         * The Search in Progress error code.
         */
        SEARCH_IN_PROGRESS(0x10),
        /**
         * The Invalid Player ID error code.
         */
        INVALID_PLAYER_ID(0x11),
        /**
         * The Player Not Browsable error code.
         */
        PLAYER_NOT_BROWSABLE(0x12),
        /**
         * The Player Not Addressed error code.
         */
        PLAYER_NOT_ADDRESSED(0x13),
        /**
         * The No Valid Search Results error code.
         */
        NO_VALID_SEARCH_RESULTS(0x14),
        /**
         * The No Available Players error code.
         */
        NO_AVAILABLE_PLAYERS(0x15),
        /**
         * The Addressed Players Changed error code.
         */
        ADDRESSED_PLAYERS_CHANGED(0x16);
        
        /*package*/int value;
        
        private CommandErrorCode(int value) {
            this.value = value;
        }
        
        /*package*/static CommandErrorCode fromValue(int value) {
            CommandErrorCode code = null;
            
            for(CommandErrorCode c : CommandErrorCode.values()) {
                if(c.value == value) {
                    code = c;
                    break;
                }
            }
            
            return code;
        }
    }

    /**
     * The possible statuses returned when a Remote Control Command fails to receive a response.
     */
    public enum CommandFailureStatus {
        /**
         * The local device timed out waiting for a response to a command.
         */
        TIMEOUT(-10218),
        /**
         * An unknown error has occurred while waiting on a response to a command.
         */
        UNKNOWN(-10219);
        
        /*package*/int value;
        
        private CommandFailureStatus(int value) {
            this.value = value;
        }
        
        /*package*/static CommandFailureStatus fromValue(int value) {
            CommandFailureStatus code = null;
            
            for(CommandFailureStatus cfs : CommandFailureStatus.values()) {
                if(cfs.value == value) {
                    code = cfs;
                    break;
                }
            }
            
            return code;
        }
    }
    
    private native static void initClassNative();

    private native void initObjectNative(int type) throws ServerNotReachableException, RemoteControlAlreadyRegisteredException;

    private native void cleanupObjectNative();
    
    protected native int connectionRequestResponseNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean accept);
    
    protected native int connectRemoteControlNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int flags, boolean waitForConnection);
    
    protected native int changeIncomingConnectionFlagsNative(int connectionFlags);
    
    protected native int disconnectRemoteConrolNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    protected native int queryConnectedRemoteControlDevicesNative(BluetoothAddress[][] addresses);
    
    protected native int sendRemoteControlPassThroughCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, long responseTimeout, byte commandType, byte subunitType, byte subunitID, int operationID, boolean stateFlag, byte operationData[]);

    protected native int vendorDependentCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, long responseTimeout, byte commandType, byte subunitType, byte subunitID, byte id0, byte id1, byte id2, byte operationData[]);
    
    protected native int groupNavigationCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, long responseTimeout, boolean buttonState, int navigationType);

    protected native int getCapabilitiesCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, long responseTimeout, int capabilityID);
    
    protected native int listPlayerApplicationSettingAttribtutesCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, long responseTimeout);
    
    protected native int listPlayerApplicationSettingValuesCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, long responseTimeout, byte attributeID);
    
    protected native int getCurrentPlayerApplicationSettingValueCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, long responseTimeout, byte[] attributeIDs);
    
    protected native int setPlayerApplicationSettingValueCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, long responseTimeout, byte[] attributeIDs, byte[] values);
    
    protected native int getPlayerApplicationSettingAttributeTextCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, long responseTimeout, byte[] attributeIDs);
    
    protected native int getPlayerApplicationSettingValueTextCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, long responseTimeout, byte attributeID, byte[] valueIDs);

    protected native int informDisplayableCharacterSetCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, long responseTimeout, short[] characterSets);
    
    protected native int informBatteryStatusOfControllerCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, long responseTimeout, int batteryStatus);

    protected native int getElementAttributesCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, long responseTimeout, long indentifier, int attributeMask);

    protected native int getPlayStatusCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, long responseTimeout);

    protected native int registerNotificationCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, long responseTimeout, int eventID, int playbackInterval);

    protected native int setAbsoluteVolumeCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, long responseTimeout, byte absoluteVolume);

}
