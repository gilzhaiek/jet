/*
 * AUDM.java Copyright 2010 - 2014 Stonestreet One, LLC. All Rights Reserved.
 */

package com.stonestreetone.bluetopiapm;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Java wrapper for the Audio (A2DP+AVRCP) Manager API for Stonestreet One
 * Bluetooth Protocol Stack Platform Manager.
 */
public abstract class AUDM {

    protected final EventCallback callbackHandler;

    private boolean               disposed;

    private long                  localData;

    static {
        System.loadLibrary("btpmj");
        initClassNative();
    }

    //FIXME Don't allow any method to be called after the object is dispose()'d

    //TODO Consider adding a Source method and Sink event which wraps in the SBC encode/decode in JNI. Maybe use NIO DirectByteBuffer for speed?

    //FIXME Need SBC encoding API

    private final static int      SERVER_TYPE_SOURCE     = 1;   // A2DP Source (+ AVRCP Target)
    private final static int      SERVER_TYPE_SINK       = 2;   // A2DP Sink (+ AVRCP Controller)
    private final static int      SERVER_TYPE_CONTROLLER = 3;   // AVRCP Controller
    private final static int      SERVER_TYPE_TARGET     = 4;   // AVRCP Target
    private final static int      SERVER_TYPE_AVRCP_ONLY = 5;   // AVRCP Manager

    protected final int           serverType;
    protected final int           audioStreamType;

    /**
     * Private constructor.
     *
     * @throws ServerNotReachableException
     * @throws BluetopiaPMException
     */
    protected AUDM(int type, boolean data, boolean remoteControlController, boolean remoteControlTarget, EventCallback eventCallback) throws ServerNotReachableException, BluetopiaPMException {

        try {

            initObjectNative(type, data, remoteControlController, remoteControlTarget);
            disposed = false;
            callbackHandler = eventCallback;

            serverType = type;

            switch(type) {
            case SERVER_TYPE_SINK:
                this.audioStreamType = STREAM_TYPE_SNK;
                break;
            case SERVER_TYPE_SOURCE:
                this.audioStreamType = STREAM_TYPE_SRC;
                break;
            default:
                this.audioStreamType = -1;
                break;
            }

        } catch(BluetopiaPMException exception) {

            dispose();
            throw exception;
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

    /**
     * Responds to a request from a remote device to connect to a Local Server.
     *
     * A successful return value does not necessarily indicate that the port has
     * been successfully opened. A
     * {@link EventCallback#audioStreamConnectedEvent} call will notify of this
     * status.
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
    public int connectionRequestResponse(ConnectionRequestType requestType, BluetoothAddress remoteDeviceAddress, boolean acceptConnection) {
        int type;
        byte[] address = remoteDeviceAddress.internalByteArray();

        switch(requestType) {
        case AUDIO:
            type = CONNECTION_REQUEST_TYPE_STREAM;
            break;
        case REMOTE_CONTROL:
        default:
            type = CONNECTION_REQUEST_TYPE_REMOTE_CONTROL;
            break;
        }

        return connectionRequestResponseNative(type, address[0], address[1], address[2], address[3], address[4], address[5], acceptConnection);
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

        flags |= (connectionFlags.contains(IncomingConnectionFlags.REQUIRE_AUTHORIZATION) ? INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION : 0);
        flags |= (connectionFlags.contains(IncomingConnectionFlags.REQUIRE_AUTHENTICATION) ? INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION : 0);
        flags |= (connectionFlags.contains(IncomingConnectionFlags.REQUIRE_ENCRYPTION) ? INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION : 0);

        return changeIncomingConnectionFlagsNative(flags);
    }

    /* The following function is responsible for sending the specified */
    /* Remote Control Response to the remote Device. This function */
    /* accepts as input the Audio Manager Remote Control Handler ID */
    /* (registered via call to the */
    /* AUDM_Register_Remote_Control_Event_Callback() function), followed */
    /* by the Device Address of the Device to send the command to, */
    /* followed by the Transaction ID of the Remote Control Event, */
    /* followed by a pointer to the actual Remote Control Response */
    /* Message to send. This function returns zero if successful or a */
    /* negative return error code if there was an error. */
    /* The following structure is a container structure that holds the */
    /* information that is returned in a aetIncomingConnectionRequest */
    /* event. */
    protected void incomingConnectionRequestEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int requestType) {
        ConnectionRequestType type;
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            switch(requestType) {
            case CONNECTION_REQUEST_TYPE_STREAM:
                type = ConnectionRequestType.AUDIO;
                break;
            case CONNECTION_REQUEST_TYPE_REMOTE_CONTROL:
            default:
                type = ConnectionRequestType.REMOTE_CONTROL;
                break;
            }

            try {
                callbackHandler.incomingConnectionRequestEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), type);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* The following structure is a container structure that holds the */
    /* information that is returned in a aetAudioStreamConnected event. */
    protected void audioStreamConnectedEvent(int streamType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int mediaMTU, long sampleFrequency, int numberChannels, int formatFlags) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    /* The following structure is a container structure that holds the */
    /* information that is returned in a aetAudioStreamConnectionStatus */
    /* event. */
    protected void audioStreamConnectionStatusEvent(int connectionStatus, int mtu, int streamType, long sampleFrequency, int numberChannels) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    /* The following structure is a container structure that holds the */
    /* information that is returned in a aetAudioStreamDisconnected */
    /* event. */
    protected void audioStreamDisconnectedEvent(int streamType) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    /* The following structure is a container structure that holds the */
    /* information that is returned in a aetAudioStreamStateChanged */
    /* event. */
    protected void audioStreamStateChangedEvent(int streamType, int streamState) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    /* The following structure is a container structure that holds the */
    /* information that is returned in a aetChangeAudioStreamStateStatus */
    /* event. */
    protected void changeAudioStreamStateStatusEvent(boolean successful, int streamType, int streamState) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    /* The following structure is a container structure that holds the */
    /* information that is returned in a aetAudioStreamFormatChanged */
    /* event. */
    protected void audioStreamFormatChangedEvent(int streamType, long sampleFrequency, int numberChannels, int formatFlags) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    /* The following structure is a container structure that holds the */
    /* information that is returned in a aetChangeAudioStreamFormatStatus */
    /* event. */
    protected void changeAudioStreamFormatStatusEvent(boolean successful, int streamType, long sampleFrequency, int numberChannels, int formatFlags) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    /* The following structure is a container structure that holds the */
    /* information that is returned in a aetEncodedAudioStreamData event. */
    protected void encodedAudioStreamDataEvent(int streamDataEventsHandlerID, byte rawAudioDataFrame[]) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    /* The following structure is a container structure that holds the */
    /* information that is returned in a */
    /* aetRemoteControlCommandIndication event. */
    protected void remoteControlPassThroughCommandIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int commandType, int subunitType, int subunitID, int operationID, boolean stateFlag, byte operationData[]) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    /* The following structure is a container structure that holds the */
    /* information that is returned in a */
    /* aetRemoteControlCommandConfirmation event. */
    protected void remoteControlPassThroughCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, int subunitType, int subunitID, int operationID, boolean stateFlag, byte operationData[]) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    protected void remoteControlConnectedEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.remoteControlConnectedEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void remoteControlConnectionStatusEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int connectionStatus) {
        ConnectionStatus status;
        EventCallback callbackHandler = this.callbackHandler;

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
                callbackHandler.remoteControlConnectionStatusEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), status);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void remoteControlDisconnectedEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int disconnectReason) {
        RemoteControlDisconnectReason reason;
        EventCallback callbackHandler = this.callbackHandler;

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
                callbackHandler.remoteControlDisconnectedEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), reason);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void vendorDependentCommandIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int commandType, int subunitType, int subunitID, byte id0, byte id1, byte id2, byte operationData[]) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    protected void getCapabilitiesCommandIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int capabilityID) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    protected void getElementAttributesCommandIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, long indentifier, int numberAttributes, int attributeMask) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    protected void getPlayStatusCommandIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    protected void registerNotificationCommandIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int eventID, int playbackInterval) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    protected void setAbsoluteVolumeCommandIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, byte absoluteVolume) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    protected void vendorDependentCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, int subunitType, int subunitID, byte id0, byte id1, byte id2, byte operationData[]) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    protected void getCompanyIDCapabilitiesCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, byte[] ids) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    protected void getEventsSupportedCapabilitiesCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, int eventIDs) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    protected void getElementAttributesCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, int[] eventIDs, int[] characterSets, byte[][] elementData) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    protected void getPlayStatusCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, int length, int position, int playStatus) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    protected void playbackStatusChangedNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, int playbackStatus) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    protected void trackChangedNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, long identifier) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    protected void trackReachedEndNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    protected void trackReachedStartNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    protected void playbackPositionChangedNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, int position) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    protected void systemStatusChangedNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, int systemStatus) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    protected void volumeChangeNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, byte volume) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    protected void setAbsoluteVolumeCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, byte volume) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    protected void getCapabilitiesRejectedEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    protected void registerNotificationRejectedEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status) {
        throw new UnsupportedOperationException("Event received which is not supported by this AUD Manager type");
    }

    /**
     * Receiver for event notifications which are common among both Audio and
     * Remote Control managers.
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
     */
    public abstract interface EventCallback {

        /**
         * Invoked when a remote device attempts to connect to the local server
         * and authorization is required from the controlling Manager.
         * <p>
         * Respond to this event by calling
         * {@link AUDM#connectionRequestResponse}.
         *
         * @param remoteDeviceAddress
         *            The {@link BluetoothAddress} of the device requesting a
         *            connection.
         * @param requestType
         *            A {@link ConnectionRequestType} value corresponding to the
         *            type of service requested.
         */
        public void incomingConnectionRequestEvent(BluetoothAddress remoteDeviceAddress, ConnectionRequestType requestType);

        /**
         * Invoked when a remote control session is established.
         *
         * @param remoteDeviceAddress
         *            The {@link BluetoothAddress} of the remote device.
         */
        public void remoteControlConnectedEvent(BluetoothAddress remoteDeviceAddress);

        /**
         * Invoked to notify the client about the status of an outgoing remote
         * control session connection.
         * <p>
         * This method is <i>only</i> invoked when the remote control session
         * connection was attempted via the {@link AUDM#connectRemoteControl}
         * method. This is in contrast to the
         * {@link #remoteControlConnectedEvent}, which applies to both incoming
         * and outgoing connections.
         *
         * @param remoteDeviceAddress
         *            The {@link BluetoothAddress} of the remote device.
         * @param connectionStatus
         *            A {@link ConnectionStatus} value corresponding to the
         *            status of the connection.
         */
        public void remoteControlConnectionStatusEvent(BluetoothAddress remoteDeviceAddress, ConnectionStatus connectionStatus);

        /**
         * Invoked when a Remote Control session is disconnected.
         *
         * @param remoteDeviceAddress
         *            The {@link BluetoothAddress} of the remote device.
         * @param disconnectReason
         *            The reason for the disconnection.
         */
        public void remoteControlDisconnectedEvent(BluetoothAddress remoteDeviceAddress, RemoteControlDisconnectReason disconnectReason);
    }

    /**
     * Generic Audio management for sink and source to inherit from.
     *
     */
    public static abstract class GenericAudioManager extends AUDM {

        //Just map the constructor
        protected GenericAudioManager(int type, boolean data, boolean remoteControlController, boolean remoteControlTarget, EventCallback eventCallback) throws ServerNotReachableException, BluetopiaPMException {
            super(type, data, remoteControlController, remoteControlTarget, eventCallback);

        }

        /**
         * Connects to a remote A2DP device.
         * <p>
         * If {@code waitForConnection} is {@code true}, this call will block
         * until the connection status is received (that is, the connection is
         * completed). If this parameter is not specified ({@code null}), then
         * the connection status will be returned asynchronously by way of a
         * call to {@link EventCallback#audioStreamConnectionStatusEvent} on the
         * {@link EventCallback} provided when this Manager was constructed.
         *
         * @param remoteDeviceAddress
         *            The {@link BluetoothAddress} of the remote device.
         * @param connectionFlags
         *            Bit flags which control whether encryption and/or
         *            authentication is used.
         * @param waitForConnection
         *            If true, this call will block until the connection attempt
         *            finishes.
         * @return Zero if successful, or a negative return error code if there
         *         was an error issuing the connection attempt. If
         *         {@code waitForConnection} is {@code true} and the connection
         *         request was successfully issued, a positive return value
         *         indicates the reason why the connection failed.
         */
        public int connectAudioStream(BluetoothAddress remoteDeviceAddress, EnumSet<ConnectionFlags> connectionFlags, boolean waitForConnection) {
            int type;
            int flags;
            byte[] address = remoteDeviceAddress.internalByteArray();

            switch(serverType) {
            case SERVER_TYPE_SINK:
            case SERVER_TYPE_CONTROLLER:
                type = STREAM_TYPE_SNK;
                break;
            case SERVER_TYPE_SOURCE:
            case SERVER_TYPE_TARGET:
            default:
                type = STREAM_TYPE_SRC;
                break;
            }

            flags = 0;
            flags |= (connectionFlags.contains(ConnectionFlags.REQUIRE_AUTHENTICATION) ? CONNECTION_FLAGS_REQUIRE_AUTHENTICATION : 0);
            flags |= (connectionFlags.contains(ConnectionFlags.REQUIRE_ENCRYPTION) ? CONNECTION_FLAGS_REQUIRE_ENCRYPTION : 0);

            return connectAudioStreamNative(address[0], address[1], address[2], address[3], address[4], address[5], type, flags, waitForConnection);
        }

        /**
         * Disconnects a currently connected audio stream.
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int disconnectAudioStream() {
            int type;

            switch(serverType) {
            case SERVER_TYPE_SINK:
            case SERVER_TYPE_CONTROLLER:
                type = STREAM_TYPE_SNK;
                break;
            case SERVER_TYPE_SOURCE:
            case SERVER_TYPE_TARGET:
            default:
                type = STREAM_TYPE_SRC;
                break;
            }

            return disconnectAudioStreamNative(type);
        }

        /**
         * Determines whether an audio stream is currently connected.
         *
         * @return The current {@link AudioStreamConnectionState}, or
         *         {@code null} if an error occurs.
         */
        public AudioStreamConnectionState queryAudioStreamConnected() {
            int type;
            int result;
            AudioStreamConnectionState state;

            switch(serverType) {
            case SERVER_TYPE_SINK:
            case SERVER_TYPE_CONTROLLER:
                type = STREAM_TYPE_SNK;
                break;
            case SERVER_TYPE_SOURCE:
            case SERVER_TYPE_TARGET:
            default:
                type = STREAM_TYPE_SRC;
                break;
            }

            result = queryAudioStreamConnectedNative(type, null);

            if(result >= 0) {
                switch(result) {
                case AUDIO_STREAM_CONNECTED_STATE_CONNECTED:
                    state = AudioStreamConnectionState.CONNECTED;
                    break;
                case AUDIO_STREAM_CONNECTED_STATE_CONNECTING:
                    state = AudioStreamConnectionState.CONNECTING;
                    break;
                case AUDIO_STREAM_CONNECTED_STATE_DISCONNECTED:
                default:
                    state = AudioStreamConnectionState.DISCONNECTED;
                    break;
                }
            } else {
                // TODO handle error case. Exception?
                state = AudioStreamConnectionState.DISCONNECTED;
            }

            return state;
        }

        /**
         * Determines the Bluetooth address of the remote device for a currently
         * connected audio stream.
         *
         * @return The {@link BluetoothAddress} of the remote device; returns
         *         {@code null} if an error occurs or if the audio stream is not
         *         currently connected.
         */
        public BluetoothAddress queryAudioStreamConnectedDevice() {
            int type;
            int result;
            byte address[];
            BluetoothAddress remoteDeviceAddress;

            address = new byte[6];

            switch(serverType) {
            case SERVER_TYPE_SINK:
            case SERVER_TYPE_CONTROLLER:
                type = STREAM_TYPE_SNK;
                break;
            case SERVER_TYPE_SOURCE:
            case SERVER_TYPE_TARGET:
            default:
                type = STREAM_TYPE_SRC;
                break;
            }

            result = queryAudioStreamConnectedNative(type, address);

            if((result == AUDIO_STREAM_CONNECTED_STATE_CONNECTED) || (result == AUDIO_STREAM_CONNECTED_STATE_CONNECTING)) {
                try {
                    remoteDeviceAddress = new BluetoothAddress(address);
                } catch(IllegalArgumentException e) {
                    remoteDeviceAddress = null;
                }
            } else {
                remoteDeviceAddress = null;
            }

            return remoteDeviceAddress;
        }

        /**
         * Determines the current state of an audio stream.
         *
         * @return The {@link AudioStreamState} of the audio stream, or
         *         {@code null} if an error occurs.
         */
        public AudioStreamState queryAudioStreamState() {
            int type;
            int result;
            int state[];
            AudioStreamState streamState;

            switch(serverType) {
            case SERVER_TYPE_SINK:
            case SERVER_TYPE_CONTROLLER:
                type = STREAM_TYPE_SNK;
                break;
            case SERVER_TYPE_SOURCE:
            case SERVER_TYPE_TARGET:
            default:
                type = STREAM_TYPE_SRC;
                break;
            }

            state = new int[1];

            result = queryAudioStreamStateNative(type, state);

            if(result == 0) {
                switch(state[0]) {
                case STREAM_STATE_STARTED:
                    streamState = AudioStreamState.STARTED;
                    break;
                case STREAM_STATE_STOPPED:
                default:
                    streamState = AudioStreamState.STOPPED;
                    break;
                }
            } else {
                // TODO throw exception for error
                streamState = null;
            }

            return streamState;
        }

        /**
         * Determines the current format of an audio stream.
         *
         * @return The {@link AudioStreamFormat} of the audio stream, or
         *         {@code null} if an error occurs.
         */
        public AudioStreamFormat queryAudioStreamFormat() {
            int type;
            int result;
            int channels[];
            long frequency[];
            AudioStreamFormat streamFormat;

            switch(serverType) {
            case SERVER_TYPE_SINK:
            case SERVER_TYPE_CONTROLLER:
                type = STREAM_TYPE_SNK;
                break;
            case SERVER_TYPE_SOURCE:
            case SERVER_TYPE_TARGET:
            default:
                type = STREAM_TYPE_SRC;
                break;
            }

            channels = new int[1];
            frequency = new long[1];

            result = queryAudioStreamFormatNative(type, frequency, channels);

            if(result == 0) {
                streamFormat = new AudioStreamFormat(frequency[0], channels[0]);
            } else {
                // TODO throw exception for error
                streamFormat = null;
            }

            return streamFormat;
        }

        /**
         * Starts or stops an audio stream.
         *
         * @param streamState
         *            The desired {@link AudioStreamState}.
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int changeAudioStreamState(AudioStreamState streamState) {
            int type;
            int state;

            switch(serverType) {
            case SERVER_TYPE_SINK:
            case SERVER_TYPE_CONTROLLER:
                type = STREAM_TYPE_SNK;
                break;
            case SERVER_TYPE_SOURCE:
            case SERVER_TYPE_TARGET:
            default:
                type = STREAM_TYPE_SRC;
                break;
            }

            switch(streamState) {
            case STARTED:
                state = STREAM_STATE_STARTED;
                break;
            case STOPPED:
            default:
                state = STREAM_STATE_STOPPED;
                break;
            }

            return changeAudioStreamStateNative(type, state);
        }

        /**
         * Modifies the format of a connected (but stopped) audio stream.
         * <p>
         * Note that the stream format can <i>only</i> be changed when the
         * {@link AudioStreamState} is {@code STOPPED}.
         *
         * @param streamFormat
         *            The desired {@link AudioStreamFormat}.
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int changeAudioStreamFormat(AudioStreamFormat streamFormat) {
            int type;

            switch(serverType) {
            case SERVER_TYPE_SINK:
            case SERVER_TYPE_CONTROLLER:
                type = STREAM_TYPE_SNK;
                break;
            case SERVER_TYPE_SOURCE:
            case SERVER_TYPE_TARGET:
            default:
                type = STREAM_TYPE_SRC;
                break;
            }

            return changeAudioStreamFormatNative(type, streamFormat.sampleFrequency, streamFormat.numberChannels);
        }

        /**
         * Determines the current configuration of an audio stream.
         *
         * @return The {@link AudioStreamConfiguration} for the audio stream, or
         *         {@code null} if an error occurs.
         */
        public AudioStreamConfiguration queryAudioStreamConfiguration() {
            int type;
            int result;
            int mtuChannelsCodecType[];
            long sampleFrequency[];
            byte mediaCodecInformation[];
            AudioStreamConfiguration configuration;

            switch(serverType) {
            case SERVER_TYPE_SINK:
            case SERVER_TYPE_CONTROLLER:
                type = STREAM_TYPE_SNK;
                break;
            case SERVER_TYPE_SOURCE:
            case SERVER_TYPE_TARGET:
            default:
                type = STREAM_TYPE_SRC;
                break;
            }

            mtuChannelsCodecType = new int[3];
            sampleFrequency = new long[1];
            mediaCodecInformation = new byte[32];

            result = queryAudioStreamConfigurationNative(type, mtuChannelsCodecType, sampleFrequency, mediaCodecInformation);

            if(result == 0) {
                configuration = new AudioStreamConfiguration();

                configuration.mediaMTU = mtuChannelsCodecType[0];
                configuration.streamFormat.numberChannels = mtuChannelsCodecType[1];
                configuration.streamFormat.sampleFrequency = sampleFrequency[0];
                configuration.mediaCodecType = mtuChannelsCodecType[2];
                configuration.mediaCodecInformation = mediaCodecInformation;
            } else {
                // FIXME throw exception for error
                configuration = null;
            }

            return configuration;
        }

        /* The following structure is a container structure that holds the */
        /* information that is returned in a aetAudioStreamConnected event. */
        @Override
        protected void audioStreamConnectedEvent(int streamType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int mediaMTU, long sampleFrequency, int numberChannels, int formatFlags) {
            AudioStreamFormat format;
            EventCallback callbackHandler = this.callbackHandler;

            if((streamType == audioStreamType) && (callbackHandler != null)) {
                format = new AudioStreamFormat(sampleFrequency, numberChannels);

                try {
                    ((AudioEventCallback)callbackHandler).audioStreamConnectedEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), mediaMTU, format);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following structure is a container structure that holds the */
        /* information that is returned in a aetAudioStreamConnectionStatus */
        /* event. */
        @Override
        protected void audioStreamConnectionStatusEvent(int connectionStatus, int mtu, int streamType, long sampleFrequency, int numberChannels) {
            ConnectionStatus status;
            AudioStreamFormat format;
            EventCallback callbackHandler = this.callbackHandler;

            if((streamType == audioStreamType) && (callbackHandler != null)) {
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

                format = new AudioStreamFormat(sampleFrequency, numberChannels);

                try {
                    ((AudioEventCallback)callbackHandler).audioStreamConnectionStatusEvent(status, mtu, format);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following structure is a container structure that holds the */
        /* information that is returned in a aetAudioStreamDisconnected */
        /* event. */
        @Override
        protected void audioStreamDisconnectedEvent(int streamType) {
            EventCallback callbackHandler = this.callbackHandler;

            if((streamType == audioStreamType) && (callbackHandler != null)) {
                try {
                    ((AudioEventCallback)callbackHandler).audioStreamDisconnectedEvent();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following structure is a container structure that holds the */
        /* information that is returned in a aetAudioStreamStateChanged */
        /* event. */
        @Override
        protected void audioStreamStateChangedEvent(int streamType, int streamState) {
            AudioStreamState state;
            EventCallback callbackHandler = this.callbackHandler;

            if((streamType == audioStreamType) && (callbackHandler != null)) {
                switch(streamState) {
                case STREAM_STATE_STARTED:
                    state = AudioStreamState.STARTED;
                    break;
                case STREAM_STATE_STOPPED:
                default:
                    state = AudioStreamState.STOPPED;
                    break;
                }

                try {
                    ((AudioEventCallback)callbackHandler).audioStreamStateChangedEvent(state);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following structure is a container structure that holds the */
        /* information that is returned in a aetChangeAudioStreamStateStatus */
        /* event. */
        @Override
        protected void changeAudioStreamStateStatusEvent(boolean successful, int streamType, int streamState) {
            AudioStreamState state;
            EventCallback callbackHandler = this.callbackHandler;

            if((streamType == audioStreamType) && (callbackHandler != null)) {
                switch(streamState) {
                case STREAM_STATE_STARTED:
                    state = AudioStreamState.STARTED;
                    break;
                case STREAM_STATE_STOPPED:
                default:
                    state = AudioStreamState.STOPPED;
                    break;
                }

                try {
                    ((AudioEventCallback)callbackHandler).changeAudioStreamStateStatusEvent(successful, state);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following structure is a container structure that holds the */
        /* information that is returned in a aetAudioStreamFormatChanged */
        /* event. */
        @Override
        protected void audioStreamFormatChangedEvent(int streamType, long sampleFrequency, int numberChannels, int formatFlags) {
            AudioStreamFormat format;
            EventCallback callbackHandler = this.callbackHandler;

            if((streamType == audioStreamType) && (callbackHandler != null)) {
                format = new AudioStreamFormat(sampleFrequency, numberChannels);

                try {
                    ((AudioEventCallback)callbackHandler).audioStreamFormatChangedEvent(format);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following structure is a container structure that holds the */
        /* information that is returned in a aetChangeAudioStreamFormatStatus */
        /* event. */
        @Override
        protected void changeAudioStreamFormatStatusEvent(boolean successful, int streamType, long sampleFrequency, int numberChannels, int formatFlags) {
            AudioStreamFormat format;
            EventCallback callbackHandler = this.callbackHandler;

            if((streamType == audioStreamType) && (callbackHandler != null)) {
                format = new AudioStreamFormat(sampleFrequency, numberChannels);

                try {
                    ((AudioEventCallback)callbackHandler).changeAudioStreamFormatStatusEvent(successful, format);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Receiver for event notifications which are common among both Source
         * and Sink sides of an A2DP connection.
         * <p>
         * It is guaranteed that no two event methods will be invoked
         * simultaneously. However, because of this guarantee, the
         * implementation of each event handler must be as efficient as
         * possible, as subsequent events will not be sent until the in-progress
         * event handler returns. Furthermore, event handlers must not block and
         * wait on conditions that can only be satisfied by receiving other
         * events.
         * <p>
         * Implementors should also note that events are sent from a thread
         * context owned by the BluetopiaPM API, so standard locking practices
         * should be observed for data sharing.
         */
        public abstract interface AudioEventCallback extends EventCallback {
            /**
             * Invoked when an incoming or outgoing audio stream is connected.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device
             * @param mediaMTU
             *            The negotiated media MTU of the stream connection.
             *            This value signifies the largest encoded data packet
             *            that can be sent or received, including the header.
             * @param streamFormat
             *            An {@link AudioStreamFormat} value corresponding to
             *            the stream format of the new connection.
             */
            public void audioStreamConnectedEvent(BluetoothAddress remoteDeviceAddress, int mediaMTU, AudioStreamFormat streamFormat);

            /**
             * Invoked to notify the client about the status of an outgoing
             * audio stream connection.
             * <p>
             * This method is <i>only</i> invoked when the remote audio stream
             * connection was attempted via the {@link AUDM#connectAudioStream}
             * method. This is in contrast to the
             * {@link #audioStreamConnectedEvent} , which applies to both
             * incoming and outgoing connections.
             *
             * @param connectionStatus
             *            A {@link ConnectionStatus} value corresponding to the
             *            status of the connection.
             * @param mediaMTU
             *            The negotiated media MTU of the stream connection.
             *            This value signifies the largest encoded data packet
             *            that can be sent or received, including the header.
             *            This value is only valid if the connection was
             *            successful.
             * @param streamFormat
             *            An {@link AudioStreamFormat} value corresponding to
             *            the stream format of the new connection. This value is
             *            only valid if the connection was successful.
             */
            public void audioStreamConnectionStatusEvent(ConnectionStatus connectionStatus, int mediaMTU, AudioStreamFormat streamFormat);

            /**
             * Invoked when an active audio stream (either incoming or outgoing)
             * has been disconnected.
             */
            public void audioStreamDisconnectedEvent();

            /**
             * Invoked when the local audio stream state has changed.
             *
             * @param streamState
             *            An {@link AudioStreamState} value corresponding to the
             *            current state of the audio stream.
             */
            public void audioStreamStateChangedEvent(AudioStreamState streamState);

            /**
             * Invoked to notify the client of the status of a request to change
             * the audio stream state.
             *
             * @param successful
             *            Indicates whether the request was successful.
             * @param streamState
             *            An {@link AudioStreamState} value corresponding to the
             *            current state of the audio stream.
             */
            public void changeAudioStreamStateStatusEvent(boolean successful, AudioStreamState streamState);

            /**
             * Invoked when the local audio stream format has changed.
             *
             * @param streamFormat
             *            An {@link AudioStreamFormat} value corresponding to
             *            the stream format of the new connection.
             */
            public void audioStreamFormatChangedEvent(AudioStreamFormat streamFormat);

            /**
             * Invoked to notify the client of the status of a requested to
             * change the audio stream format.
             *
             * @param successful
             *            Indicates whether the request was successful.
             * @param streamFormat
             *            An {@link AudioStreamFormat} value corresponding to
             *            the current local audio stream format.
             */
            public void changeAudioStreamFormatStatusEvent(boolean successful, AudioStreamFormat streamFormat);
        }

    }

    /**
     * Management connection to a local Audio Sink.
     *
     */
    public static final class AudioSinkManager extends GenericAudioManager {

        /**
         * Create a new management connection to the local Audio Sink.
         * <p>
         * Note that this is distinct from the Advanced Audio Distribution
         * Profile. This server is specific to the role of a A2DP Sink (such as
         * speakers or headphones). This server supports connecting to remote
         * A2DP Source servers (typically, a mobile phone, MP3 player, or other
         * playback device).
         * <p>
         * The local server supports one "Data" Manager. This Manager is the
         * only Manager permitted to send and receive audio data to and from the
         * server. All other registered Managers are permitted only to receive
         * events, make/break connections, and query the various statuses of the
         * local server.
         *
         * @param data
         *            Claim the role of Data Manager for the local server.
         * @param remoteControl
         *            Register to send Remote Control Commands and receive
         *            Remote Control Response Events.
         * @param eventCallback
         *            Receiver for events sent by the local server.
         * @throws BluetopiaPMException
         */
        public AudioSinkManager(boolean data, boolean remoteControl, AudioSinkEventCallback eventCallback) throws BluetopiaPMException {
            super(SERVER_TYPE_SINK, data, remoteControl, false, eventCallback);
        }

        /**
         * Sends a remote control command to a remote device.
         * <p>
         * NOTE: This function only supports
         * {@link AUDM#RemoteControlPassThroughCommans}. To obtain further AVRCP
         * functionality, use an instance of {@link AUDM#RemoteControlManager}.
         *
         * @param remoteDeviceAddress
         *            The {@link BluetoothAddress} of the remote device.
         * @param responseTimeout
         *            The length of time (in milliseconds) to wait for a
         *            response.
         * @param command
         *            The {@link RemoteControlCommand} to send to the remote
         *            device.
         * @return A positive value indicating the Transaction ID of the
         *         command, or a negative return error code indicating failure.
         *         The Transaction ID can be used to match this command with its
         *         associated Command Confirmation event.
         */
        public int sendRemoteControlCommand(BluetoothAddress remoteDeviceAddress, long responseTimeout, RemoteControlCommand command) {
            switch(command.type) {
            case RemoteControlCommand.REMOTE_CONTROl_COMMAND_TYPE_PASSTHROUGH:
                return handlePassThroughCommand(remoteDeviceAddress, responseTimeout, (RemoteControlPassThroughCommand)command);
            default:
                return -1;
            }
        }

        /* The following structure is a container structure that holds the */
        /* information that is returned in a aetEncodedAudioStreamData event. */
        @Override
        protected void encodedAudioStreamDataEvent(int streamDataEventsHandlerID, byte rawAudioDataFrame[]) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((AudioSinkEventCallback)callbackHandler).encodedAudioStreamDataEvent(rawAudioDataFrame);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following structure is a container structure that holds the */
        /* information that is returned in a */
        /* aetRemoteControlCommandConfirmation event. */
        @Override
        protected void remoteControlPassThroughCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, int subunitType, int subunitID, int operationID, boolean stateFlag, byte operationData[]) {
            RemoteControlPassThroughResponse response;
            EventCallback callbackHandler = this.callbackHandler;

            response = convertPassThroughConfirmation(responseCode, subunitType, subunitID, operationID, stateFlag, operationData);

            try {
                ((AudioSinkEventCallback)callbackHandler).remoteControlPassThroughCommandConfirmationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), transactionID, status, response);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Receiver for event notifications which apply to an Audio Sink.
         * <p>
         * It is guaranteed that no two event methods will be invoked
         * simultaneously. However, because of this guarantee, the
         * implementation of each event handler must be as efficient as
         * possible, as subsequent events will not be sent until the in-progress
         * event handler returns. Furthermore, event handlers must not block and
         * wait on conditions that can only be satisfied by receiving other
         * events.
         * <p>
         * Implementors should also note that events are sent from a thread
         * context owned by the BluetopiaPM API, so standard locking practices
         * should be observed for data sharing.
         */
        public interface AudioSinkEventCallback extends AudioEventCallback {
            /**
             * Invoked when incoming audio data is received.
             *
             * @param rawAudioDataFrame
             *            A buffer containing encoded audio data.
             */
            public void encodedAudioStreamDataEvent(byte rawAudioDataFrame[]);

            /**
             * Invoked to inform the client about the result of a previously
             * issued remote control command.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device.
             * @param transactionID
             *            The transactionID associated with the remote control
             *            command. This will match the return value from a
             *            successful call to
             *            {@link AudioSinkManager#sendRemoteControlCommand}.
             * @param status
             *            The status of the command. The value will be zero if
             *            the command was successfully acknowledged, or a
             *            negative return error code.
             * @param response
             *            The {@link RemoteControlPassThroughResponse}
             *            containing the response to the command.
             */
            public void remoteControlPassThroughCommandConfirmationEvent(BluetoothAddress remoteDeviceAddress, byte transactionID, int status, RemoteControlPassThroughResponse response);
        }
    }

    /**
     * Management connection to the local Audio Source.
     */
    public static final class AudioSourceManager extends GenericAudioManager {

        public AudioSourceManager(boolean data, boolean remoteControl, AudioSourceEventCallback eventCallback) throws BluetopiaPMException {
            super(SERVER_TYPE_SOURCE, data, false, remoteControl, eventCallback);

        }

        /* The following function is responsible for sending the specified */
        /* Encoded Audio Data to the remote SNK. This function accepts as */
        /* input the Audio Manager Data Handler ID (registered via call to */
        /* the AUDM_Register_Data_Event_Callback() function), followed by the */// change comments
        /* number of bytes of raw, encoded, audio frame information, followed */
        /* by the raw, encoded, Audio Data to send. This function returns */
        /* zero if successful or a negative return error code if there was an */
        /* error. */
        /* * NOTE * This is a low level function that exists for applications */
        /* that would like to encode the Audio Data themselves (as */
        /* opposed to having this module encode and send the data). */
        /* The caller can determine the current configuration of the */
        /* stream by calling the */
        /* AUDM_Query_Audio_Stream_Configuration() function. */
        /* * NOTE * The data that is sent *MUST* contain the AVDTP Header */
        /* Information (i.e. the first byte of the data *MUST* be a */
        /* valid AVDTP Header byte). */
        /* * NOTE * This function assumes the specified data is being sent at */
        /* real time pacing, and the data is queued to be sent */
        /* immediately. */
        public int sendEncodedAudioData(byte rawAudioDataFrame[]) {
            return sendEncodedAudioDataNative(rawAudioDataFrame);
        }

        /* The following function is responsible for sending the specified */
        /* Remote Control Response to the remote Device. This function */
        /* accepts as input the Audio Manager Remote Control Handler ID */
        /* (registered via call to the */
        /* AUDM_Register_Remote_Control_Event_Callback() function), followed */
        /* by the Device Address of the Device to send the command to, */
        /* followed by the Transaction ID of the Remote Control Event, */
        /* followed by a pointer to the actual Remote Control Response */
        /* Message to send. This function returns zero if successful or a */
        /* negative return error code if there was an error. */

        /**
         * Sends a remote control response.
         * <p>
         * NOTE: This function only supports
         * {@link AUDM#RemoteControlPassThroughResponse}. To obtain further
         * AVRCP functionality, use an instance of
         * {@link AUDM#RemoteControlManager}.
         *
         * @param remoteDeviceAddress
         *            The {@link BluetoothAddress} of the remote device.
         * @param transactionID
         *            The Transaction ID associated with this remote control
         *            response.
         * @param response
         *            The response code associated with this response.
         *
         * @return Zero if successful or a negative error code if there was an
         *         error.
         */
        public int sendRemoteControlResponse(BluetoothAddress remoteDeviceAddress, byte transactionID, RemoteControlResponse response) {
            switch(response.type) {
            case RemoteControlResponse.REMOTE_CONTROL_RESPONSE_TYPE_PASSTHROUGH:
                return handlePassThroughResponse(remoteDeviceAddress, transactionID, (RemoteControlPassThroughResponse)response);
            default:
                return -1;
            }

        }

        /* The following structure is a container structure that holds the */
        /* information that is returned in a */
        /* aetRemoteControlCommandIndication event. */
        @Override
        protected void remoteControlPassThroughCommandIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int commandType, int subunitType, int subunitID, int operationID, boolean stateFlag, byte operationData[]) {
            RemoteControlPassThroughCommand command;
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {

                command = convertPassThroughIndication(commandType, subunitType, subunitID, operationID, stateFlag, operationData);

                try {
                    ((AudioSourceEventCallback)callbackHandler).remoteControlPassThroughCommandIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), transactionID, command);
                } catch(Exception e) {
                    e.printStackTrace();
                }

            }

        }

        /**
         * Receiver for event notifications which apply to an Audio Source.
         * <p>
         * It is guaranteed that no two event methods will be invoked
         * simultaneously. However, because of this guarantee, the
         * implementation of each event handler must be as efficient as
         * possible, as subsequent events will not be sent until the in-progress
         * event handler returns. Furthermore, event handlers must not block and
         * wait on conditions that can only be satisfied by receiving other
         * events.
         * <p>
         * Implementors should also note that events are sent from a thread
         * context owned by the BluetopiaPM API, so standard locking practices
         * should be observed for data sharing.
         */
        public interface AudioSourceEventCallback extends AudioEventCallback {
            /**
             * Invoked when a remote control command is received.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device that
             *            issued the command.
             * @param transactionID
             *            The transaction ID that should be supplied when
             *            responding to the command by way of
             *            {@link AudioSourceManager#sendRemoteControlResponse}.
             * @param remoteControlCommand
             *            The {@link RemoteControlPassThroughCommand} that was
             *            issued.
             */
            public void remoteControlPassThroughCommandIndicationEvent(BluetoothAddress remoteDeviceAddress, byte transactionID, RemoteControlPassThroughCommand remoteControlCommand);
        }
    }

    /**
     * Manages connection to a local remote control module.
     *
     */
    public static class RemoteControlManager extends AUDM {
        private Map<Byte, EventID>      notificationTransactionMap;
        private Map<Byte, CapabilityID> capabilityTransactionMap;

        /**
         * Creates a new connection to the local remote control module.
         *
         * @param supportController
         *            {@code true} if this module should support AVRCP
         *            Controller.
         * @param supportTarget
         *            {@code true} if this module should support AVRCP
         *            Controller.
         *
         * @param eventHandler
         *            The remote control event callback to receives events
         *            related to this module.
         *
         * @throws ServerNotReachableException
         * @throws BluetopiaPMException
         */
        public RemoteControlManager(boolean supportController, boolean supportTarget, RemoteControlEventCallback eventHandler) throws ServerNotReachableException, BluetopiaPMException {
            super(SERVER_TYPE_AVRCP_ONLY, false, supportController, supportTarget, eventHandler);
            if(supportController) {
                notificationTransactionMap = new HashMap<Byte, EventID>();
                capabilityTransactionMap = new HashMap<Byte, CapabilityID>();
            }
        }

        /**
         * Connects to a remote Remote Control Device.
         *
         * @param remoteDeviceAddress
         *            The {@link BluetoothAddress} of the remote device.
         * @param connectionFlags
         *            Bit flags which control whether encryption and/or
         *            authentication is used.
         * @param waitForConnection
         *            If true, this call will block until the connection attempt
         *            finishes.
         * @return Zero if successful, or a negative return error code if there
         *         was an error issuing the connection attempt. If
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
            flags |= (connectionFlags.contains(ConnectionFlags.REQUIRE_ENCRYPTION) ? CONNECTION_FLAGS_REQUIRE_ENCRYPTION : 0);

            return connectRemoteControlNative(address[0], address[1], address[2], address[3], address[4], address[5], flags, waitForConnection);
        }

        /**
         * Disconnects a currently connected audio stream.
         *
         * @param remoteDeviceAddress
         *            The {@link BluetoothAddress} of the remote device.
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int disconnectRemoteControl(BluetoothAddress remoteDeviceAddress) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return disconnectRemoteConrolNative(address[0], address[1], address[2], address[3], address[4], address[5]);
        }

        /**
         * Queries the currently connected Remote Control Devices.
         *
         * @return The list of connected Bluetooth Addresses. The list will be
         *         an empty list if no connections are active, and {@code NULL}
         *         if there is an error.
         */
        public BluetoothAddress[] queryConnectedRemoteControlDevices() {
            int result;
            BluetoothAddress[][] addressList;

            addressList = new BluetoothAddress[1][0];

            result = queryConnectedRemoteControlDevicesNative(addressList);

            if(result < 0)
                addressList[0] = null;

            return addressList[0];
        }

        /**
         * Sends a remote control command to a remote device.
         *
         * @param remoteDeviceAddress
         *            The {@link BluetoothAddress} of the remote device.
         * @param responseTimeout
         *            The length of time (in milliseconds) to wait for a
         *            response.
         * @param command
         *            The {@link RemoteControlCommand} to send to the remote
         *            device.
         * @return A positive value indicating the Transaction ID of the
         *         command, or a negative return error code indicating failure.
         *         The Transaction ID can be used to match this command with its
         *         associated Command Confirmation event.
         */
        public int sendRemoteControlCommand(BluetoothAddress remoteDeviceAddress, long responseTimeout, RemoteControlCommand command) {
            switch(command.type) {
            case RemoteControlCommand.REMOTE_CONTROL_COMMAND_TYPE_VENDOR_DEPENDENT:
                return handleVendorDependentCommand(remoteDeviceAddress, responseTimeout, (VendorDependentGenericCommand)command);

            case RemoteControlCommand.REMOTE_CONTROL_COMMAND_TYPE_GET_CAPABILITIES:
                return handleGetCapabilitiesCommand(remoteDeviceAddress, responseTimeout, (GetCapabilitiesCommand)command);

            case RemoteControlCommand.REMOTE_CONTROL_COMMAND_TYPE_GET_ELEMENT_ATTRIBUTES:
                return handleGetElementAttributesCommand(remoteDeviceAddress, responseTimeout, (GetElementAttributesCommand)command);

            case RemoteControlCommand.REMOTE_CONTROL_COMMAND_TYPE_GET_PLAY_STATUS:
                return handleGetPlayStatusCommand(remoteDeviceAddress, responseTimeout, (GetPlayStatusCommand)command);

            case RemoteControlCommand.REMOTE_CONTROL_COMMAND_TYPE_REGISTER_NOTIFICATION:
                return handleRegisterNotificationCommand(remoteDeviceAddress, responseTimeout, (RegisterNotificationCommand)command);

            case RemoteControlCommand.REMOTE_CONTROL_COMMAND_TYPE_SET_ABSOLUTE_VOLUME:
                return handleSetAbsoluteVolumeCommand(remoteDeviceAddress, responseTimeout, (SetAbsoluteVolumeCommand)command);

            case RemoteControlCommand.REMOTE_CONTROl_COMMAND_TYPE_PASSTHROUGH:
                return handlePassThroughCommand(remoteDeviceAddress, responseTimeout, (RemoteControlPassThroughCommand)command);

            default:
                //We should not get any other types because
                //the type is determine at instantiation
                throw new IllegalArgumentException("Unsupported Remote Control Command");
            }
        }

        /**
         * Sends a remote control response.
         *
         * @param remoteDeviceAddress
         *            The {@link BluetoothAddress} of the remote device.
         * @param transactionID
         *            The Transaction ID associated with this remote control
         *            response.
         * @param response
         *            The response code associated with this response.
         *
         * @return Zero if successful or a negative error code if there was an
         *         error.
         */
        public int sendRemoteControlResponse(BluetoothAddress remoteDeviceAddress, byte transactionID, RemoteControlResponse response) {
            switch(response.type) {
            case RemoteControlResponse.REMOTE_CONTROL_RESPONSE_TYPE_VENDOR_DEPENDENT:
                return handleVendorDependentResponse(remoteDeviceAddress, transactionID, (VendorDependentGenericResponse)response);

            case RemoteControlResponse.REMOTE_CONTROL_RESPONSE_TYPE_GET_COMPANY_CAPABILITIES:
                return handleGetCompanyIDCapabilititesResponse(remoteDeviceAddress, transactionID, (GetCompanyIDCapabilitiesResponse)response);

            case RemoteControlResponse.REMOTE_CONTROL_RESPONSE_TYPE_GET_EVENT_CAPABILITIES:
                return handleGetEventsSupportedCapabilitiesResponse(remoteDeviceAddress, transactionID, (GetEventsSupportedCapabilitiesResponse)response);

            case RemoteControlResponse.REMOTE_CONTROL_RESPONSE_TYPE_GET_ELEMENT_ATTRIBUTES:
                return handleGetElementAttributesResponse(remoteDeviceAddress, transactionID, (GetElementAttributesResponse)response);

            case RemoteControlResponse.REMOTE_CONTROL_RESPONSE_TYPE_GET_PLAY_STATUS:
                return handleGetPlayStatusResponse(remoteDeviceAddress, transactionID, (GetPlayStatusResponse)response);

            case RemoteControlResponse.REMOTE_CONTROL_RESPONSE_TYPE_NOTIFICATION:
                return handleRegisterNotificationResponse(remoteDeviceAddress, transactionID, (RegisterNotificationResponse)response);

            case RemoteControlResponse.REMOTE_CONTROL_RESPONSE_TYPE_SET_ABSOLUTE_VOLUME:
                return handleSetAbsoluteVolumeResponse(remoteDeviceAddress, transactionID, (SetAbsoluteVolumeResponse)response);

            case RemoteControlResponse.REMOTE_CONTROL_RESPONSE_TYPE_PASSTHROUGH:
                return handlePassThroughResponse(remoteDeviceAddress, transactionID, (RemoteControlPassThroughResponse)response);

            default:
                //We should not get any other types because
                //the type is determine at instantiation
                throw new IllegalArgumentException("Unsupported Remote Control Response");
            }
        }

        private int handleVendorDependentCommand(BluetoothAddress remoteDeviceAddress, long responseTimeout, VendorDependentGenericCommand command) {
            int commandType, subunitType, subunitID;

            byte[] address = remoteDeviceAddress.internalByteArray();

            switch(command.commandType) {
            case CONTROL:
                commandType = AVRCP_CTYPE_CONTROL;
                break;
            case STATUS:
                commandType = AVRCP_CTYPE_STATUS;
                break;
            case SPECIFIC_INQUIRY:
                commandType = AVRCP_CTYPE_SPECIFIC_INQUIRY;
                break;
            case NOTIFY:
                commandType = AVRCP_CTYPE_NOTIFY;
                break;
            case GENERAL_INQUIRY:
            default:
                commandType = AVRCP_CTYPE_GENERAL_INQUIRY;
                break;
            }

            switch(command.subunitType) {
            case VIDEO_MONITOR:
                subunitType = AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR;
                break;
            case DISC_RECORDER_PLAYER:
                subunitType = AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER;
                break;
            case TAPE_RECORDER_PLAYER:
                subunitType = AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER;
                break;
            case TUNER:
                subunitType = AVRCP_SUBUNIT_TYPE_TUNER;
                break;
            case VIDEO_CAMERA:
                subunitType = AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA;
                break;
            case PANEL:
                subunitType = AVRCP_SUBUNIT_TYPE_PANEL;
                break;
            case VENDOR_SPECIFIC:
                subunitType = AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC;
                break;
            case EXTENDED:
                subunitType = AVRCP_SUBUNIT_TYPE_EXTENDED;
                break;
            case UNIT:
            default:
                subunitType = AVRCP_SUBUNIT_TYPE_UNIT;
                break;
            }

            switch(command.subunitID) {
            case INSTANCE_0:
                subunitID = AVRCP_SUBUNIT_ID_INSTANCE_0;
                break;
            case INSTANCE_1:
                subunitID = AVRCP_SUBUNIT_ID_INSTANCE_1;
                break;
            case INSTANCE_2:
                subunitID = AVRCP_SUBUNIT_ID_INSTANCE_2;
                break;
            case INSTANCE_3:
                subunitID = AVRCP_SUBUNIT_ID_INSTANCE_3;
                break;
            case INSTANCE_4:
                subunitID = AVRCP_SUBUNIT_ID_INSTANCE_4;
                break;
            case EXTENDED:
                subunitID = AVRCP_SUBUNIT_ID_EXTENDED;
                break;
            case IGNORE:
            default:
                subunitID = AVRCP_SUBUNIT_ID_IGNORE;
                break;
            }

            return vendorDependentCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], responseTimeout, commandType, subunitType, subunitID, command.companyID.id0, command.companyID.id1, command.companyID.id2, command.commandData);
        }

        private int handleGetCapabilitiesCommand(BluetoothAddress remoteDeviceAddress, long responseTimeout, GetCapabilitiesCommand command) {
            int capabilityID;

            int transaction;

            byte[] address = remoteDeviceAddress.internalByteArray();

            switch(command.capabilityID) {
            case COMPANY_ID:
                capabilityID = CAPABILITY_ID_COMPANY_ID;
                break;
            case EVENTS_SUPPORTED:
            default:
                capabilityID = CAPABILITY_ID_EVENTS_SUPPORTED;
                break;

            }

            transaction = getCapabilitiesCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], responseTimeout, capabilityID);

            if(transaction > 0)
                capabilityTransactionMap.put((byte)transaction, command.capabilityID);

            return transaction;
        }

        private int handleGetElementAttributesCommand(BluetoothAddress remoteDeviceAddress, long responseTimeout, GetElementAttributesCommand command) {
            int attributeMask = 0;

            byte[] address = remoteDeviceAddress.internalByteArray();

            if(command.attributeIDs.contains(ElementAttributeID.TITLE))
                attributeMask |= ELEMENT_ATTRIBUTE_ID_TITLE;
            if(command.attributeIDs.contains(ElementAttributeID.ARTIST))
                attributeMask |= ELEMENT_ATTRIBUTE_ID_ARTIST;
            if(command.attributeIDs.contains(ElementAttributeID.ALBUM))
                attributeMask |= ELEMENT_ATTRIBUTE_ID_ALBUM;
            if(command.attributeIDs.contains(ElementAttributeID.NUMBER_OF_MEDIA))
                attributeMask |= ELEMENT_ATTRIBUTE_ID_NUMBER_OF_MEDIA;
            if(command.attributeIDs.contains(ElementAttributeID.TOTAL_NUMBER_OF_MEDIA))
                attributeMask |= ELEMENT_ATTRIBUTE_ID_TOTAL_NUMBER_OF_MEDIA;
            if(command.attributeIDs.contains(ElementAttributeID.GENRE))
                attributeMask |= ELEMENT_ATTRIBUTE_ID_GENRE;
            if(command.attributeIDs.contains(ElementAttributeID.PLAYING_TIME))
                attributeMask |= ELEMENT_ATTRIBUTE_ID_PLAYING_TIME;

            return getElementAttributesCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], responseTimeout, command.identifier, command.attributeIDs.size(), attributeMask);
        }

        private int handleGetPlayStatusCommand(BluetoothAddress remoteDeviceAddress, long responseTimeout, GetPlayStatusCommand command) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return getPlayStatusCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], responseTimeout);
        }

        private int handleRegisterNotificationCommand(BluetoothAddress remoteDeviceAddress, long responseTimeout, RegisterNotificationCommand command) {
            int eventID;
            byte[] address = remoteDeviceAddress.internalByteArray();

            int transaction;

            switch(command.eventID) {
            case ADDRESSED_PLAYER_CHANGED:
                eventID = EVENT_ID_ADDRESSED_PLAYER_CHANGED;
                break;
            case AVAILABLE_PLAYERS_CHANGED:
                eventID = EVENT_ID_AVAILABLE_PLAYERS_CHANGED;
                break;
            case BATTERY_STATUS_CHANGED:
                eventID = EVENT_ID_BATTERY_STATUS_CHANGED;
                break;
            case NOW_PLAYING_CONTENT_CHANGED:
                eventID = EVENT_ID_NOW_PLAYING_CONTENT_CHANGED;
                break;
            case PLAYBACK_POSITION_CHANGED:
                eventID = EVENT_ID_PLAYBACK_POSITION_CHANGED;
                break;
            case PLAYBACK_STATUS_CHANGED:
                eventID = EVENT_ID_PLAYBACK_STATUS_CHANGED;
                break;
            case PLAYER_APPLICATION_SETTING_CHANGED:
                eventID = EVENT_ID_PLAYER_APPLICATION_SETTING_CHANGED;
                break;
            case SYSTEM_STATUS_CHANGED:
                eventID = EVENT_ID_SYSTEM_STATUS_CHANGED;
                break;
            case TRACK_CHANGED:
                eventID = EVENT_ID_TRACK_CHANGED;
                break;
            case TRACK_REACHED_END:
                eventID = EVENT_ID_TRACK_REACHED_END;
                break;
            case TRACK_REACHED_START:
                eventID = EVENT_ID_TRACK_REACHED_START;
                break;
            case UIDS_CHANGED:
                eventID = EVENT_ID_UIDS_CHANGED;
                break;
            case VOLUME_CHANGED:
            default:
                eventID = EVENT_ID_VOLUME_CHANGED;
                break;

            }
            transaction = registerNotificationCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], responseTimeout, eventID, command.playbackInterval);

            if(transaction > 0)
                notificationTransactionMap.put((byte)transaction, command.eventID);

            return transaction;
        }

        private int handleSetAbsoluteVolumeCommand(BluetoothAddress remoteDeviceAddress, long responseTimeout, SetAbsoluteVolumeCommand command) {
            byte[] address = remoteDeviceAddress.internalByteArray();

            byte volume = (byte)(0x7F * command.absoluteVolume);

            return setAbsoluteVolumeCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], responseTimeout, volume);
        }

        private int handleVendorDependentResponse(BluetoothAddress remoteDeviceAddress, byte transactionID, VendorDependentGenericResponse response) {
            int responseCode, subunitType, subunitID;

            byte[] address = remoteDeviceAddress.internalByteArray();

            switch(response.responseCode) {
            case NOT_IMPLEMENTED:
                responseCode = AVRCP_RESPONSE_NOT_IMPLEMENTED;
                break;
            case ACCEPTED:
                responseCode = AVRCP_RESPONSE_ACCEPTED;
                break;
            case REJECTED:
                responseCode = AVRCP_RESPONSE_REJECTED;
                break;
            case IN_TRANSITION:
                responseCode = AVRCP_RESPONSE_IN_TRANSITION;
                break;
            case STABLE:
                responseCode = AVRCP_RESPONSE_STABLE;
                break;
            case CHANGED:
                responseCode = AVRCP_RESPONSE_CHANGED;
                break;
            case INTERIM:
            default:
                responseCode = AVRCP_RESPONSE_INTERIM;
                break;
            }

            switch(response.subunitType) {
            case VIDEO_MONITOR:
                subunitType = AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR;
                break;
            case DISC_RECORDER_PLAYER:
                subunitType = AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER;
                break;
            case TAPE_RECORDER_PLAYER:
                subunitType = AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER;
                break;
            case TUNER:
                subunitType = AVRCP_SUBUNIT_TYPE_TUNER;
                break;
            case VIDEO_CAMERA:
                subunitType = AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA;
                break;
            case PANEL:
                subunitType = AVRCP_SUBUNIT_TYPE_PANEL;
                break;
            case VENDOR_SPECIFIC:
                subunitType = AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC;
                break;
            case EXTENDED:
                subunitType = AVRCP_SUBUNIT_TYPE_EXTENDED;
                break;
            case UNIT:
            default:
                subunitType = AVRCP_SUBUNIT_TYPE_UNIT;
                break;
            }

            switch(response.subunitID) {
            case INSTANCE_0:
                subunitID = AVRCP_SUBUNIT_ID_INSTANCE_0;
                break;
            case INSTANCE_1:
                subunitID = AVRCP_SUBUNIT_ID_INSTANCE_1;
                break;
            case INSTANCE_2:
                subunitID = AVRCP_SUBUNIT_ID_INSTANCE_2;
                break;
            case INSTANCE_3:
                subunitID = AVRCP_SUBUNIT_ID_INSTANCE_3;
                break;
            case INSTANCE_4:
                subunitID = AVRCP_SUBUNIT_ID_INSTANCE_4;
                break;
            case EXTENDED:
                subunitID = AVRCP_SUBUNIT_ID_EXTENDED;
                break;
            case IGNORE:
            default:
                subunitID = AVRCP_SUBUNIT_ID_IGNORE;
                break;
            }

            return vendorDependentResponseNative(address[0], address[1], address[2], address[3], address[4], address[5], transactionID, responseCode, subunitType, subunitID, response.companyID.id0, response.companyID.id1, response.companyID.id2, response.commandData);
        }

        private int handleGetCompanyIDCapabilititesResponse(BluetoothAddress remoteDeviceAddress, byte transactionID, GetCompanyIDCapabilitiesResponse response) {
            int responseCode;
            byte[] address = remoteDeviceAddress.internalByteArray();
            byte[] idData;

            switch(response.responseCode) {
            case NOT_IMPLEMENTED:
                responseCode = AVRCP_RESPONSE_NOT_IMPLEMENTED;
                break;
            case ACCEPTED:
                responseCode = AVRCP_RESPONSE_ACCEPTED;
                break;
            case REJECTED:
                responseCode = AVRCP_RESPONSE_REJECTED;
                break;
            case IN_TRANSITION:
                responseCode = AVRCP_RESPONSE_IN_TRANSITION;
                break;
            case STABLE:
                responseCode = AVRCP_RESPONSE_STABLE;
                break;
            case CHANGED:
                responseCode = AVRCP_RESPONSE_CHANGED;
                break;
            case INTERIM:
            default:
                responseCode = AVRCP_RESPONSE_INTERIM;
                break;
            }

            idData = new byte[response.companyIDs.length * 3];

            for(int i = 0; i < response.companyIDs.length; i++) {
                idData[i] = response.companyIDs[i].id0;
                idData[i + 1] = response.companyIDs[i].id1;
                idData[i + 2] = response.companyIDs[i].id2;
            }

            return getCompanyIDCapabilitiesResponseNative(address[0], address[1], address[2], address[3], address[4], address[5], transactionID, responseCode, idData);
        }

        private int handleGetEventsSupportedCapabilitiesResponse(BluetoothAddress remoteDeviceAddress, byte transactionID, GetEventsSupportedCapabilitiesResponse response) {
            int responseCode;
            byte[] address = remoteDeviceAddress.internalByteArray();
            int eventIDs;

            switch(response.responseCode) {
            case NOT_IMPLEMENTED:
                responseCode = AVRCP_RESPONSE_NOT_IMPLEMENTED;
                break;
            case ACCEPTED:
                responseCode = AVRCP_RESPONSE_ACCEPTED;
                break;
            case REJECTED:
                responseCode = AVRCP_RESPONSE_REJECTED;
                break;
            case IN_TRANSITION:
                responseCode = AVRCP_RESPONSE_IN_TRANSITION;
                break;
            case STABLE:
                responseCode = AVRCP_RESPONSE_STABLE;
                break;
            case CHANGED:
                responseCode = AVRCP_RESPONSE_CHANGED;
                break;
            case INTERIM:
            default:
                responseCode = AVRCP_RESPONSE_INTERIM;
                break;
            }

            eventIDs = 0;

            if(response.eventIDs.contains(EventID.ADDRESSED_PLAYER_CHANGED))
                eventIDs |= EVENT_ID_ADDRESSED_PLAYER_CHANGED;
            if(response.eventIDs.contains(EventID.AVAILABLE_PLAYERS_CHANGED))
                eventIDs |= EVENT_ID_AVAILABLE_PLAYERS_CHANGED;
            if(response.eventIDs.contains(EventID.BATTERY_STATUS_CHANGED))
                eventIDs |= EVENT_ID_BATTERY_STATUS_CHANGED;
            if(response.eventIDs.contains(EventID.NOW_PLAYING_CONTENT_CHANGED))
                eventIDs |= EVENT_ID_NOW_PLAYING_CONTENT_CHANGED;
            if(response.eventIDs.contains(EventID.PLAYBACK_POSITION_CHANGED))
                eventIDs |= EVENT_ID_PLAYBACK_POSITION_CHANGED;
            if(response.eventIDs.contains(EventID.PLAYBACK_STATUS_CHANGED))
                eventIDs |= EVENT_ID_PLAYBACK_STATUS_CHANGED;
            if(response.eventIDs.contains(EventID.PLAYER_APPLICATION_SETTING_CHANGED))
                eventIDs |= EVENT_ID_PLAYER_APPLICATION_SETTING_CHANGED;
            if(response.eventIDs.contains(EventID.SYSTEM_STATUS_CHANGED))
                eventIDs |= EVENT_ID_SYSTEM_STATUS_CHANGED;
            if(response.eventIDs.contains(EventID.TRACK_CHANGED))
                eventIDs |= EVENT_ID_TRACK_CHANGED;
            if(response.eventIDs.contains(EventID.TRACK_REACHED_END))
                eventIDs |= EVENT_ID_TRACK_REACHED_END;
            if(response.eventIDs.contains(EventID.TRACK_REACHED_START))
                eventIDs |= EVENT_ID_TRACK_REACHED_START;
            if(response.eventIDs.contains(EventID.UIDS_CHANGED))
                eventIDs |= EVENT_ID_UIDS_CHANGED;
            if(response.eventIDs.contains(EventID.VOLUME_CHANGED))
                eventIDs |= EVENT_ID_VOLUME_CHANGED;

            return getEventsSupportedCapabilitiesResponseNative(address[0], address[1], address[2], address[3], address[4], address[5], transactionID, responseCode, eventIDs);
        }

        private int handleGetElementAttributesResponse(BluetoothAddress remoteDeviceAddress, byte transactionID, GetElementAttributesResponse response) {
            int responseCode;
            byte[] address = remoteDeviceAddress.internalByteArray();
            int[] attributeIDs;
            int[] characterSets;
            byte[][] attributeData;

            switch(response.responseCode) {
            case NOT_IMPLEMENTED:
                responseCode = AVRCP_RESPONSE_NOT_IMPLEMENTED;
                break;
            case ACCEPTED:
                responseCode = AVRCP_RESPONSE_ACCEPTED;
                break;
            case REJECTED:
                responseCode = AVRCP_RESPONSE_REJECTED;
                break;
            case IN_TRANSITION:
                responseCode = AVRCP_RESPONSE_IN_TRANSITION;
                break;
            case STABLE:
                responseCode = AVRCP_RESPONSE_STABLE;
                break;
            case CHANGED:
                responseCode = AVRCP_RESPONSE_CHANGED;
                break;
            case INTERIM:
            default:
                responseCode = AVRCP_RESPONSE_INTERIM;
                break;
            }

            attributeIDs = new int[response.attributes.length];
            characterSets = new int[response.attributes.length];
            attributeData = new byte[response.attributes.length][];

            for(int i = 0; i < response.attributes.length; i++) {
                switch(response.attributes[i].attributeID) {
                case ALBUM:
                    attributeIDs[i] = ELEMENT_ATTRIBUTE_ID_ALBUM;
                    break;
                case ARTIST:
                    attributeIDs[i] = ELEMENT_ATTRIBUTE_ID_ARTIST;
                    break;
                case GENRE:
                    attributeIDs[i] = ELEMENT_ATTRIBUTE_ID_GENRE;
                    break;
                case NUMBER_OF_MEDIA:
                    attributeIDs[i] = ELEMENT_ATTRIBUTE_ID_NUMBER_OF_MEDIA;
                    break;
                case PLAYING_TIME:
                    attributeIDs[i] = ELEMENT_ATTRIBUTE_ID_PLAYING_TIME;
                    break;
                case TITLE:
                    attributeIDs[i] = ELEMENT_ATTRIBUTE_ID_TITLE;
                    break;
                case TOTAL_NUMBER_OF_MEDIA:
                default:
                    attributeIDs[i] = ELEMENT_ATTRIBUTE_ID_TOTAL_NUMBER_OF_MEDIA;
                    break;

                }

                characterSets[i] = response.attributes[i].ianaCharacterSet;
                attributeData[i] = response.attributes[i].attributeData;
            }

            return getElementAttributesResponseNative(address[0], address[1], address[2], address[3], address[4], address[5], transactionID, responseCode, attributeIDs, characterSets, attributeData);
        }

        private int handleGetPlayStatusResponse(BluetoothAddress remoteDeviceAddress, byte transactionID, GetPlayStatusResponse response) {
            int responseCode;
            byte[] address = remoteDeviceAddress.internalByteArray();
            int playStatus;

            switch(response.responseCode) {
            case NOT_IMPLEMENTED:
                responseCode = AVRCP_RESPONSE_NOT_IMPLEMENTED;
                break;
            case ACCEPTED:
                responseCode = AVRCP_RESPONSE_ACCEPTED;
                break;
            case REJECTED:
                responseCode = AVRCP_RESPONSE_REJECTED;
                break;
            case IN_TRANSITION:
                responseCode = AVRCP_RESPONSE_IN_TRANSITION;
                break;
            case STABLE:
                responseCode = AVRCP_RESPONSE_STABLE;
                break;
            case CHANGED:
                responseCode = AVRCP_RESPONSE_CHANGED;
                break;
            case INTERIM:
            default:
                responseCode = AVRCP_RESPONSE_INTERIM;
                break;
            }

            switch(response.playStatus) {
            case ERROR:
            default:
                playStatus = PLAY_STATUS_ERROR;
                break;
            case FWD_SEEK:
                playStatus = PLAY_STATUS_FWD_SEEK;
                break;
            case PAUSED:
                playStatus = PLAY_STATUS_PAUSED;
                break;
            case PLAYING:
                playStatus = PLAY_STATUS_PLAYING;
                break;
            case REV_SEEK:
                playStatus = PLAY_STATUS_REV_SEEK;
                break;
            case STOPPED:
                playStatus = PLAY_STATUS_STOPPED;
                break;
            }

            return getPlayStatusResponseNative(address[0], address[1], address[2], address[3], address[4], address[5], transactionID, responseCode, playStatus, response.songLength, response.songPosition);
        }

        private int handleRegisterNotificationResponse(BluetoothAddress remoteDeviceAddress, byte transactionID, RegisterNotificationResponse response) {
            int responseCode;
            int intParam;
            byte[] address = remoteDeviceAddress.internalByteArray();
            byte volume;

            switch(response.responseCode) {
            case NOT_IMPLEMENTED:
                responseCode = AVRCP_RESPONSE_NOT_IMPLEMENTED;
                break;
            case ACCEPTED:
                responseCode = AVRCP_RESPONSE_ACCEPTED;
                break;
            case REJECTED:
                responseCode = AVRCP_RESPONSE_REJECTED;
                break;
            case IN_TRANSITION:
                responseCode = AVRCP_RESPONSE_IN_TRANSITION;
                break;
            case STABLE:
                responseCode = AVRCP_RESPONSE_STABLE;
                break;
            case CHANGED:
                responseCode = AVRCP_RESPONSE_CHANGED;
                break;
            case INTERIM:
            default:
                responseCode = AVRCP_RESPONSE_INTERIM;
                break;
            }

            switch(response.eventData.eventID) {

            case PLAYBACK_POSITION_CHANGED:
                return playbackPositionChangedNotificationNative(address[0], address[1], address[2], address[3], address[4], address[5], transactionID, responseCode, ((PlaybackPosititionChangedNotificationData)response.eventData).playbackPosition);
            case PLAYBACK_STATUS_CHANGED:
                switch(((PlaybackStatusChangeNotificationData)response.eventData).playStatus) {
                case ERROR:
                default:
                    intParam = PLAY_STATUS_ERROR;
                    break;
                case FWD_SEEK:
                    intParam = PLAY_STATUS_FWD_SEEK;
                    break;
                case PAUSED:
                    intParam = PLAY_STATUS_PAUSED;
                    break;
                case PLAYING:
                    intParam = PLAY_STATUS_PLAYING;
                    break;
                case REV_SEEK:
                    intParam = PLAY_STATUS_REV_SEEK;
                    break;
                case STOPPED:
                    intParam = PLAY_STATUS_STOPPED;
                    break;
                }

                return playbackStatusChangedNotificationNative(address[0], address[1], address[2], address[3], address[4], address[5], transactionID, responseCode, intParam);
            case SYSTEM_STATUS_CHANGED:
                switch(((SystemStatusChangedNotificationData)response.eventData).status) {
                case POWER_OFF:
                default:
                    intParam = SYSTEM_STATUS_POWER_OFF;
                    break;
                case POWER_ON:
                    intParam = SYSTEM_STATUS_POWER_ON;
                    break;
                case UNPLUGGED:
                    intParam = SYSTEM_STATUS_UNPLUGGED;
                    break;
                }

                return systemStatusChangedNotificationNative(address[0], address[1], address[2], address[3], address[4], address[5], transactionID, responseCode, intParam);
            case TRACK_CHANGED:
                return trackChangedNotificationNative(address[0], address[1], address[2], address[3], address[4], address[5], transactionID, responseCode, ((TrackChangedNotificationData)response.eventData).identifier);
            case TRACK_REACHED_END:
                return trackReachedEndNotificationNative(address[0], address[1], address[2], address[3], address[4], address[5], transactionID, responseCode);
            case TRACK_REACHED_START:
                return trackReachedStartNotificationNative(address[0], address[1], address[2], address[3], address[4], address[5], transactionID, responseCode);
            case VOLUME_CHANGED:
                volume = (byte)(0x7F * ((VolumeChangeNotificationData)response.eventData).absoluteVolume);
                return volumeChangeNotificationNative(address[0], address[1], address[2], address[3], address[4], address[5], transactionID, responseCode, volume);
            default:
                throw new IllegalArgumentException("Unknown notification type");

            }
        }

        private int handleSetAbsoluteVolumeResponse(BluetoothAddress remoteDeviceAddress, byte transactionID, SetAbsoluteVolumeResponse response) {
            int responseCode;
            byte[] address = remoteDeviceAddress.internalByteArray();
            byte volume = (byte)(0x7F * response.absoluteVolume);

            switch(response.responseCode) {
            case NOT_IMPLEMENTED:
                responseCode = AVRCP_RESPONSE_NOT_IMPLEMENTED;
                break;
            case ACCEPTED:
                responseCode = AVRCP_RESPONSE_ACCEPTED;
                break;
            case REJECTED:
                responseCode = AVRCP_RESPONSE_REJECTED;
                break;
            case IN_TRANSITION:
                responseCode = AVRCP_RESPONSE_IN_TRANSITION;
                break;
            case STABLE:
                responseCode = AVRCP_RESPONSE_STABLE;
                break;
            case CHANGED:
                responseCode = AVRCP_RESPONSE_CHANGED;
                break;
            case INTERIM:
            default:
                responseCode = AVRCP_RESPONSE_INTERIM;
                break;
            }

            return setAbsoluteVolumeResponseNative(address[0], address[1], address[2], address[3], address[4], address[5], transactionID, responseCode, volume);
        }

        @Override
        protected void vendorDependentCommandIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int commandType, int subunitType, int subunitID, byte id0, byte id1, byte id2, byte operationData[]) {
            BluetoothAddress address;
            RemoteControlEventCallback callbackHandler = (RemoteControlEventCallback)this.callbackHandler;
            RemoteControlCommandType cmdType;
            RemoteControlSubunitType subType;
            RemoteControlSubunitID subID;
            VendorDependentGenericCommand command;

            if(callbackHandler != null) {

                address = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                try {
                    switch(commandType) {
                    case AVRCP_CTYPE_CONTROL:
                        cmdType = RemoteControlCommandType.CONTROL;
                        break;
                    case AVRCP_CTYPE_STATUS:
                        cmdType = RemoteControlCommandType.STATUS;
                        break;
                    case AVRCP_CTYPE_SPECIFIC_INQUIRY:
                        cmdType = RemoteControlCommandType.SPECIFIC_INQUIRY;
                        break;
                    case AVRCP_CTYPE_NOTIFY:
                        cmdType = RemoteControlCommandType.NOTIFY;
                        break;
                    case AVRCP_CTYPE_GENERAL_INQUIRY:
                    default:
                        cmdType = RemoteControlCommandType.GENERAL_INQUIRY;
                        break;
                    }

                    switch(subunitType) {
                    case AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR:
                        subType = RemoteControlSubunitType.VIDEO_MONITOR;
                        break;
                    case AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER:
                        subType = RemoteControlSubunitType.DISC_RECORDER_PLAYER;
                        break;
                    case AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER:
                        subType = RemoteControlSubunitType.TAPE_RECORDER_PLAYER;
                        break;
                    case AVRCP_SUBUNIT_TYPE_TUNER:
                        subType = RemoteControlSubunitType.TUNER;
                        break;
                    case AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA:
                        subType = RemoteControlSubunitType.VIDEO_CAMERA;
                        break;
                    case AVRCP_SUBUNIT_TYPE_PANEL:
                        subType = RemoteControlSubunitType.PANEL;
                        break;
                    case AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC:
                        subType = RemoteControlSubunitType.VENDOR_SPECIFIC;
                        break;
                    case AVRCP_SUBUNIT_TYPE_EXTENDED:
                        subType = RemoteControlSubunitType.EXTENDED;
                        break;
                    case AVRCP_SUBUNIT_TYPE_UNIT:
                    default:
                        subType = RemoteControlSubunitType.UNIT;
                        break;
                    }

                    switch(subunitID) {
                    case AVRCP_SUBUNIT_ID_INSTANCE_0:
                        subID = RemoteControlSubunitID.INSTANCE_0;
                        break;
                    case AVRCP_SUBUNIT_ID_INSTANCE_1:
                        subID = RemoteControlSubunitID.INSTANCE_1;
                        break;
                    case AVRCP_SUBUNIT_ID_INSTANCE_2:
                        subID = RemoteControlSubunitID.INSTANCE_2;
                        break;
                    case AVRCP_SUBUNIT_ID_INSTANCE_3:
                        subID = RemoteControlSubunitID.INSTANCE_3;
                        break;
                    case AVRCP_SUBUNIT_ID_INSTANCE_4:
                        subID = RemoteControlSubunitID.INSTANCE_4;
                        break;
                    case AVRCP_SUBUNIT_ID_EXTENDED:
                        subID = RemoteControlSubunitID.EXTENDED;
                        break;
                    case AVRCP_SUBUNIT_ID_IGNORE:
                    default:
                        subID = RemoteControlSubunitID.IGNORE;
                        break;
                    }

                    command = new VendorDependentGenericCommand(cmdType, subType, subID, new CompanyID(id0, id1, id2), operationData);

                    callbackHandler.vendorDependentCommandIndication(address, transactionID, command);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void getCapabilitiesCommandIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int capabilityID) {
            RemoteControlEventCallback callbackHandler = (RemoteControlEventCallback)this.callbackHandler;

            BluetoothAddress address;
            CapabilityID id;

            if(callbackHandler != null) {

                address = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                switch(capabilityID) {
                case CAPABILITY_ID_COMPANY_ID:
                    id = CapabilityID.COMPANY_ID;
                    break;
                case CAPABILITY_ID_EVENTS_SUPPORTED:
                default:
                    id = CapabilityID.EVENTS_SUPPORTED;
                    break;
                }

                try {
                    callbackHandler.getCapabilitiesCommandIndication(address, transactionID, new GetCapabilitiesCommand(id));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void getElementAttributesCommandIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, long identifier, int numberAttributes, int attributeMask) {
            RemoteControlEventCallback callbackHandler = (RemoteControlEventCallback)this.callbackHandler;

            BluetoothAddress address;
            EnumSet<ElementAttributeID> attributeIDs;

            if(callbackHandler != null) {

                address = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                attributeIDs = EnumSet.noneOf(ElementAttributeID.class);

                if((attributeMask & ELEMENT_ATTRIBUTE_ID_TITLE) > 0)
                    attributeIDs.add(ElementAttributeID.TITLE);
                if((attributeMask & ELEMENT_ATTRIBUTE_ID_ARTIST) > 0)
                    attributeIDs.add(ElementAttributeID.ARTIST);
                if((attributeMask & ELEMENT_ATTRIBUTE_ID_ALBUM) > 0)
                    attributeIDs.add(ElementAttributeID.ALBUM);
                if((attributeMask & ELEMENT_ATTRIBUTE_ID_NUMBER_OF_MEDIA) > 0)
                    attributeIDs.add(ElementAttributeID.NUMBER_OF_MEDIA);
                if((attributeMask & ELEMENT_ATTRIBUTE_ID_TOTAL_NUMBER_OF_MEDIA) > 0)
                    attributeIDs.add(ElementAttributeID.TOTAL_NUMBER_OF_MEDIA);
                if((attributeMask & ELEMENT_ATTRIBUTE_ID_GENRE) > 0)
                    attributeIDs.add(ElementAttributeID.GENRE);
                if((attributeMask & ELEMENT_ATTRIBUTE_ID_PLAYING_TIME) > 0)
                    attributeIDs.add(ElementAttributeID.PLAYING_TIME);

                try {
                    callbackHandler.getElementAttributesCommandIndication(address, transactionID, new GetElementAttributesCommand(identifier, attributeIDs));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void getPlayStatusCommandIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID) {
            RemoteControlEventCallback callbackHandler = (RemoteControlEventCallback)this.callbackHandler;

            BluetoothAddress address;

            if(callbackHandler != null) {

                address = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                try {
                    callbackHandler.getPlayStatusCommandIndication(address, transactionID, new GetPlayStatusCommand());
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void registerNotificationCommandIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int eventID, int playbackInterval) {
            RemoteControlEventCallback callbackHandler = (RemoteControlEventCallback)this.callbackHandler;

            BluetoothAddress address;
            EventID id;

            if(callbackHandler != null) {

                address = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                switch(eventID) {
                case EVENT_ID_PLAYBACK_STATUS_CHANGED:
                    id = EventID.PLAYBACK_STATUS_CHANGED;
                    break;
                case EVENT_ID_TRACK_CHANGED:
                    id = EventID.TRACK_CHANGED;
                    break;
                case EVENT_ID_TRACK_REACHED_END:
                    id = EventID.TRACK_REACHED_END;
                    break;
                case EVENT_ID_TRACK_REACHED_START:
                    id = EventID.TRACK_REACHED_START;
                    break;
                case EVENT_ID_PLAYBACK_POSITION_CHANGED:
                    id = EventID.PLAYBACK_POSITION_CHANGED;
                    break;
                case EVENT_ID_BATTERY_STATUS_CHANGED:
                    id = EventID.BATTERY_STATUS_CHANGED;
                    break;
                case EVENT_ID_SYSTEM_STATUS_CHANGED:
                    id = EventID.SYSTEM_STATUS_CHANGED;
                    break;
                case EVENT_ID_PLAYER_APPLICATION_SETTING_CHANGED:
                    id = EventID.PLAYER_APPLICATION_SETTING_CHANGED;
                    break;
                case EVENT_ID_NOW_PLAYING_CONTENT_CHANGED:
                    id = EventID.NOW_PLAYING_CONTENT_CHANGED;
                    break;
                case EVENT_ID_AVAILABLE_PLAYERS_CHANGED:
                    id = EventID.AVAILABLE_PLAYERS_CHANGED;
                    break;
                case EVENT_ID_ADDRESSED_PLAYER_CHANGED:
                    id = EventID.ADDRESSED_PLAYER_CHANGED;
                    break;
                case EVENT_ID_UIDS_CHANGED:
                    id = EventID.UIDS_CHANGED;
                    break;
                case EVENT_ID_VOLUME_CHANGED:
                default:
                    id = EventID.VOLUME_CHANGED;
                    break;
                }

                try {
                    callbackHandler.registerNotificationCommandIndication(address, transactionID, new RegisterNotificationCommand(id, playbackInterval));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void setAbsoluteVolumeCommandIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, byte absoluteVolume) {
            RemoteControlEventCallback callbackHandler = (RemoteControlEventCallback)this.callbackHandler;

            BluetoothAddress address;

            if(callbackHandler != null) {

                address = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                try {
                    callbackHandler.setAbsoluteVolumeCommandIndication(address, transactionID, new SetAbsoluteVolumeCommand((float)absoluteVolume / (float)0x7F));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void vendorDependentCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, int subunitType, int subunitID, byte id0, byte id1, byte id2, byte operationData[]) {
            RemoteControlEventCallback callbackHandler = (RemoteControlEventCallback)this.callbackHandler;
            RemoteControlResponseCode respCode;
            RemoteControlSubunitType subType;
            RemoteControlSubunitID subID;
            BluetoothAddress address;

            if(callbackHandler != null) {

                address = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                switch(responseCode) {
                case AVRCP_RESPONSE_NOT_IMPLEMENTED:
                    respCode = RemoteControlResponseCode.NOT_IMPLEMENTED;
                    break;
                case AVRCP_RESPONSE_ACCEPTED:
                    respCode = RemoteControlResponseCode.ACCEPTED;
                    break;
                case AVRCP_RESPONSE_REJECTED:
                    respCode = RemoteControlResponseCode.REJECTED;
                    break;
                case AVRCP_RESPONSE_IN_TRANSITION:
                    respCode = RemoteControlResponseCode.IN_TRANSITION;
                    break;
                case AVRCP_RESPONSE_STABLE:
                    respCode = RemoteControlResponseCode.STABLE;
                    break;
                case AVRCP_RESPONSE_CHANGED:
                    respCode = RemoteControlResponseCode.CHANGED;
                    break;
                case AVRCP_RESPONSE_INTERIM:
                default:
                    respCode = RemoteControlResponseCode.INTERIM;
                    break;
                }

                switch(subunitType) {
                case AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR:
                    subType = RemoteControlSubunitType.VIDEO_MONITOR;
                    break;
                case AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER:
                    subType = RemoteControlSubunitType.DISC_RECORDER_PLAYER;
                    break;
                case AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER:
                    subType = RemoteControlSubunitType.TAPE_RECORDER_PLAYER;
                    break;
                case AVRCP_SUBUNIT_TYPE_TUNER:
                    subType = RemoteControlSubunitType.TUNER;
                    break;
                case AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA:
                    subType = RemoteControlSubunitType.VIDEO_CAMERA;
                    break;
                case AVRCP_SUBUNIT_TYPE_PANEL:
                    subType = RemoteControlSubunitType.PANEL;
                    break;
                case AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC:
                    subType = RemoteControlSubunitType.VENDOR_SPECIFIC;
                    break;
                case AVRCP_SUBUNIT_TYPE_EXTENDED:
                    subType = RemoteControlSubunitType.EXTENDED;
                    break;
                case AVRCP_SUBUNIT_TYPE_UNIT:
                default:
                    subType = RemoteControlSubunitType.UNIT;
                    break;
                }

                switch(subunitID) {
                case AVRCP_SUBUNIT_ID_INSTANCE_0:
                    subID = RemoteControlSubunitID.INSTANCE_0;
                    break;
                case AVRCP_SUBUNIT_ID_INSTANCE_1:
                    subID = RemoteControlSubunitID.INSTANCE_1;
                    break;
                case AVRCP_SUBUNIT_ID_INSTANCE_2:
                    subID = RemoteControlSubunitID.INSTANCE_2;
                    break;
                case AVRCP_SUBUNIT_ID_INSTANCE_3:
                    subID = RemoteControlSubunitID.INSTANCE_3;
                    break;
                case AVRCP_SUBUNIT_ID_INSTANCE_4:
                    subID = RemoteControlSubunitID.INSTANCE_4;
                    break;
                case AVRCP_SUBUNIT_ID_EXTENDED:
                    subID = RemoteControlSubunitID.EXTENDED;
                    break;
                case AVRCP_SUBUNIT_ID_IGNORE:
                default:
                    subID = RemoteControlSubunitID.IGNORE;
                    break;
                }
                try {
                    callbackHandler.vendorDependentCommandConfirmation(address, transactionID, status, new VendorDependentGenericResponse(respCode, subType, subID, new CompanyID(id0, id1, id2), operationData));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void getCompanyIDCapabilitiesCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, byte[] ids) {
            RemoteControlEventCallback callbackHandler = (RemoteControlEventCallback)this.callbackHandler;

            BluetoothAddress address;
            RemoteControlResponseCode respCode;
            CompanyID[] companyIDs;

            capabilityTransactionMap.remove(transactionID);

            if(callbackHandler != null) {

                address = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                switch(responseCode) {
                case AVRCP_RESPONSE_NOT_IMPLEMENTED:
                    respCode = RemoteControlResponseCode.NOT_IMPLEMENTED;
                    break;
                case AVRCP_RESPONSE_ACCEPTED:
                    respCode = RemoteControlResponseCode.ACCEPTED;
                    break;
                case AVRCP_RESPONSE_REJECTED:
                    respCode = RemoteControlResponseCode.REJECTED;
                    break;
                case AVRCP_RESPONSE_IN_TRANSITION:
                    respCode = RemoteControlResponseCode.IN_TRANSITION;
                    break;
                case AVRCP_RESPONSE_STABLE:
                    respCode = RemoteControlResponseCode.STABLE;
                    break;
                case AVRCP_RESPONSE_CHANGED:
                    respCode = RemoteControlResponseCode.CHANGED;
                    break;
                case AVRCP_RESPONSE_INTERIM:
                default:
                    respCode = RemoteControlResponseCode.INTERIM;
                    break;
                }

                companyIDs = new CompanyID[(ids != null) ? (ids.length / 3) : 0];

                for(int i = 0; i < companyIDs.length; i++)
                    companyIDs[i] = new CompanyID(ids[3 * i], ids[3 * i + 1], ids[3 * i + 2]);

                try {
                    callbackHandler.getCompanyIDCapabilitiesCommandConfirmation(address, transactionID, status, new GetCompanyIDCapabilitiesResponse(respCode, companyIDs));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void getEventsSupportedCapabilitiesCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, int eventIDs) {
            RemoteControlEventCallback callbackHandler = (RemoteControlEventCallback)this.callbackHandler;

            BluetoothAddress address;
            RemoteControlResponseCode respCode;
            EnumSet<EventID> ids;

            capabilityTransactionMap.remove(transactionID);

            if(callbackHandler != null) {

                switch(responseCode) {
                case AVRCP_RESPONSE_NOT_IMPLEMENTED:
                    respCode = RemoteControlResponseCode.NOT_IMPLEMENTED;
                    break;
                case AVRCP_RESPONSE_ACCEPTED:
                    respCode = RemoteControlResponseCode.ACCEPTED;
                    break;
                case AVRCP_RESPONSE_REJECTED:
                    respCode = RemoteControlResponseCode.REJECTED;
                    break;
                case AVRCP_RESPONSE_IN_TRANSITION:
                    respCode = RemoteControlResponseCode.IN_TRANSITION;
                    break;
                case AVRCP_RESPONSE_STABLE:
                    respCode = RemoteControlResponseCode.STABLE;
                    break;
                case AVRCP_RESPONSE_CHANGED:
                    respCode = RemoteControlResponseCode.CHANGED;
                    break;
                case AVRCP_RESPONSE_INTERIM:
                default:
                    respCode = RemoteControlResponseCode.INTERIM;
                    break;
                }

                address = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                ids = EnumSet.noneOf(EventID.class);

                if((eventIDs & EVENT_ID_PLAYBACK_STATUS_CHANGED) > 0)
                    ids.add(EventID.PLAYBACK_STATUS_CHANGED);
                if((eventIDs & EVENT_ID_TRACK_CHANGED) > 0)
                    ids.add(EventID.TRACK_CHANGED);
                if((eventIDs & EVENT_ID_TRACK_REACHED_END) > 0)
                    ids.add(EventID.TRACK_REACHED_END);
                if((eventIDs & EVENT_ID_TRACK_REACHED_START) > 0)
                    ids.add(EventID.TRACK_REACHED_START);
                if((eventIDs & EVENT_ID_PLAYBACK_POSITION_CHANGED) > 0)
                    ids.add(EventID.PLAYBACK_POSITION_CHANGED);
                if((eventIDs & EVENT_ID_BATTERY_STATUS_CHANGED) > 0)
                    ids.add(EventID.BATTERY_STATUS_CHANGED);
                if((eventIDs & EVENT_ID_SYSTEM_STATUS_CHANGED) > 0)
                    ids.add(EventID.SYSTEM_STATUS_CHANGED);
                if((eventIDs & EVENT_ID_PLAYER_APPLICATION_SETTING_CHANGED) > 0)
                    ids.add(EventID.PLAYER_APPLICATION_SETTING_CHANGED);
                if((eventIDs & EVENT_ID_NOW_PLAYING_CONTENT_CHANGED) > 0)
                    ids.add(EventID.NOW_PLAYING_CONTENT_CHANGED);
                if((eventIDs & EVENT_ID_AVAILABLE_PLAYERS_CHANGED) > 0)
                    ids.add(EventID.AVAILABLE_PLAYERS_CHANGED);
                if((eventIDs & EVENT_ID_ADDRESSED_PLAYER_CHANGED) > 0)
                    ids.add(EventID.ADDRESSED_PLAYER_CHANGED);
                if((eventIDs & EVENT_ID_UIDS_CHANGED) > 0)
                    ids.add(EventID.UIDS_CHANGED);
                if((eventIDs & EVENT_ID_VOLUME_CHANGED) > 0)
                    ids.add(EventID.VOLUME_CHANGED);

                try {
                    callbackHandler.getEventsSupportedCapabilitiesConfirmation(address, transactionID, status, new GetEventsSupportedCapabilitiesResponse(respCode, ids));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void getElementAttributesCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, int[] attributeIDs, int[] characterSets, byte[][] elementData) {
            RemoteControlEventCallback callbackHandler = (RemoteControlEventCallback)this.callbackHandler;

            BluetoothAddress address;
            RemoteControlResponseCode respCode;
            ElementAttributeID id;
            ElementAttribute[] attributes;

            if(callbackHandler != null) {

                switch(responseCode) {
                case AVRCP_RESPONSE_NOT_IMPLEMENTED:
                    respCode = RemoteControlResponseCode.NOT_IMPLEMENTED;
                    break;
                case AVRCP_RESPONSE_ACCEPTED:
                    respCode = RemoteControlResponseCode.ACCEPTED;
                    break;
                case AVRCP_RESPONSE_REJECTED:
                    respCode = RemoteControlResponseCode.REJECTED;
                    break;
                case AVRCP_RESPONSE_IN_TRANSITION:
                    respCode = RemoteControlResponseCode.IN_TRANSITION;
                    break;
                case AVRCP_RESPONSE_STABLE:
                    respCode = RemoteControlResponseCode.STABLE;
                    break;
                case AVRCP_RESPONSE_CHANGED:
                    respCode = RemoteControlResponseCode.CHANGED;
                    break;
                case AVRCP_RESPONSE_INTERIM:
                default:
                    respCode = RemoteControlResponseCode.INTERIM;
                    break;
                }

                address = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                attributes = new ElementAttribute[(attributeIDs != null) ? attributeIDs.length : 0];

                for(int i = 0; i < attributes.length; i++) {
                    switch(attributeIDs[i]) {
                    case ELEMENT_ATTRIBUTE_ID_TITLE:
                    default:
                        id = ElementAttributeID.TITLE;
                        break;
                    case ELEMENT_ATTRIBUTE_ID_ARTIST:
                        id = ElementAttributeID.ARTIST;
                        break;
                    case ELEMENT_ATTRIBUTE_ID_ALBUM:
                        id = ElementAttributeID.ALBUM;
                        break;
                    case ELEMENT_ATTRIBUTE_ID_NUMBER_OF_MEDIA:
                        id = ElementAttributeID.NUMBER_OF_MEDIA;
                        break;
                    case ELEMENT_ATTRIBUTE_ID_TOTAL_NUMBER_OF_MEDIA:
                        id = ElementAttributeID.TOTAL_NUMBER_OF_MEDIA;
                        break;
                    case ELEMENT_ATTRIBUTE_ID_GENRE:
                        id = ElementAttributeID.GENRE;
                        break;
                    case ELEMENT_ATTRIBUTE_ID_PLAYING_TIME:
                        id = ElementAttributeID.PLAYING_TIME;
                        break;
                    }

                    attributes[i] = new ElementAttribute(id, characterSets[i], elementData[i]);
                }

                try {
                    callbackHandler.getElementAttributesCommandConfirmation(address, transactionID, status, new GetElementAttributesResponse(respCode, attributes));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void getPlayStatusCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, int length, int position, int playbackStatus) {
            RemoteControlEventCallback callbackHandler = (RemoteControlEventCallback)this.callbackHandler;

            BluetoothAddress address;
            RemoteControlResponseCode respCode;
            PlayStatus pStatus;

            if(callbackHandler != null) {

                switch(responseCode) {
                case AVRCP_RESPONSE_NOT_IMPLEMENTED:
                    respCode = RemoteControlResponseCode.NOT_IMPLEMENTED;
                    break;
                case AVRCP_RESPONSE_ACCEPTED:
                    respCode = RemoteControlResponseCode.ACCEPTED;
                    break;
                case AVRCP_RESPONSE_REJECTED:
                    respCode = RemoteControlResponseCode.REJECTED;
                    break;
                case AVRCP_RESPONSE_IN_TRANSITION:
                    respCode = RemoteControlResponseCode.IN_TRANSITION;
                    break;
                case AVRCP_RESPONSE_STABLE:
                    respCode = RemoteControlResponseCode.STABLE;
                    break;
                case AVRCP_RESPONSE_CHANGED:
                    respCode = RemoteControlResponseCode.CHANGED;
                    break;
                case AVRCP_RESPONSE_INTERIM:
                default:
                    respCode = RemoteControlResponseCode.INTERIM;
                    break;
                }

                address = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                switch(playbackStatus) {
                case PLAY_STATUS_STOPPED:
                    pStatus = PlayStatus.STOPPED;
                    break;
                case PLAY_STATUS_PLAYING:
                    pStatus = PlayStatus.PLAYING;
                    break;
                case PLAY_STATUS_PAUSED:
                    pStatus = PlayStatus.PAUSED;
                    break;
                case PLAY_STATUS_FWD_SEEK:
                    pStatus = PlayStatus.FWD_SEEK;
                    break;
                case PLAY_STATUS_REV_SEEK:
                    pStatus = PlayStatus.REV_SEEK;
                    break;
                case PLAY_STATUS_ERROR:
                default:
                    pStatus = PlayStatus.ERROR;
                    break;

                }

                try {
                    callbackHandler.getPlayStatusCommandConfirmation(address, transactionID, status, new GetPlayStatusResponse(respCode, length, position, pStatus));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void playbackStatusChangedNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, int playbackStatus) {
            RemoteControlEventCallback callbackHandler = (RemoteControlEventCallback)this.callbackHandler;

            BluetoothAddress address;
            RemoteControlResponseCode respCode;
            PlayStatus pStatus;

            notificationTransactionMap.remove(transactionID);

            if(callbackHandler != null) {

                switch(responseCode) {
                case AVRCP_RESPONSE_NOT_IMPLEMENTED:
                    respCode = RemoteControlResponseCode.NOT_IMPLEMENTED;
                    break;
                case AVRCP_RESPONSE_ACCEPTED:
                    respCode = RemoteControlResponseCode.ACCEPTED;
                    break;
                case AVRCP_RESPONSE_REJECTED:
                    respCode = RemoteControlResponseCode.REJECTED;
                    break;
                case AVRCP_RESPONSE_IN_TRANSITION:
                    respCode = RemoteControlResponseCode.IN_TRANSITION;
                    break;
                case AVRCP_RESPONSE_STABLE:
                    respCode = RemoteControlResponseCode.STABLE;
                    break;
                case AVRCP_RESPONSE_CHANGED:
                    respCode = RemoteControlResponseCode.CHANGED;
                    break;
                case AVRCP_RESPONSE_INTERIM:
                default:
                    respCode = RemoteControlResponseCode.INTERIM;
                    break;
                }

                switch(playbackStatus) {
                case PLAY_STATUS_STOPPED:
                    pStatus = PlayStatus.STOPPED;
                    break;
                case PLAY_STATUS_PLAYING:
                    pStatus = PlayStatus.PLAYING;
                    break;
                case PLAY_STATUS_PAUSED:
                    pStatus = PlayStatus.PAUSED;
                    break;
                case PLAY_STATUS_FWD_SEEK:
                    pStatus = PlayStatus.FWD_SEEK;
                    break;
                case PLAY_STATUS_REV_SEEK:
                    pStatus = PlayStatus.REV_SEEK;
                    break;
                case PLAY_STATUS_ERROR:
                default:
                    pStatus = PlayStatus.ERROR;
                    break;
                }

                address = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                try {
                    callbackHandler.playbackStatusChangeNotification(address, transactionID, status, new PlaybackStatusChangeNotificationData(respCode, pStatus));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void trackChangedNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, long identifier) {
            RemoteControlEventCallback callbackHandler = (RemoteControlEventCallback)this.callbackHandler;

            BluetoothAddress address;
            RemoteControlResponseCode respCode;

            notificationTransactionMap.remove(transactionID);

            if(callbackHandler != null) {

                switch(responseCode) {
                case AVRCP_RESPONSE_NOT_IMPLEMENTED:
                    respCode = RemoteControlResponseCode.NOT_IMPLEMENTED;
                    break;
                case AVRCP_RESPONSE_ACCEPTED:
                    respCode = RemoteControlResponseCode.ACCEPTED;
                    break;
                case AVRCP_RESPONSE_REJECTED:
                    respCode = RemoteControlResponseCode.REJECTED;
                    break;
                case AVRCP_RESPONSE_IN_TRANSITION:
                    respCode = RemoteControlResponseCode.IN_TRANSITION;
                    break;
                case AVRCP_RESPONSE_STABLE:
                    respCode = RemoteControlResponseCode.STABLE;
                    break;
                case AVRCP_RESPONSE_CHANGED:
                    respCode = RemoteControlResponseCode.CHANGED;
                    break;
                case AVRCP_RESPONSE_INTERIM:
                default:
                    respCode = RemoteControlResponseCode.INTERIM;
                    break;
                }

                address = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                try {
                    callbackHandler.trackChangedNotification(address, transactionID, status, new TrackChangedNotificationData(respCode, identifier));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void trackReachedEndNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode) {
            RemoteControlEventCallback callbackHandler = (RemoteControlEventCallback)this.callbackHandler;

            BluetoothAddress address;
            RemoteControlResponseCode respCode;

            notificationTransactionMap.remove(transactionID);

            if(callbackHandler != null) {

                switch(responseCode) {
                case AVRCP_RESPONSE_NOT_IMPLEMENTED:
                    respCode = RemoteControlResponseCode.NOT_IMPLEMENTED;
                    break;
                case AVRCP_RESPONSE_ACCEPTED:
                    respCode = RemoteControlResponseCode.ACCEPTED;
                    break;
                case AVRCP_RESPONSE_REJECTED:
                    respCode = RemoteControlResponseCode.REJECTED;
                    break;
                case AVRCP_RESPONSE_IN_TRANSITION:
                    respCode = RemoteControlResponseCode.IN_TRANSITION;
                    break;
                case AVRCP_RESPONSE_STABLE:
                    respCode = RemoteControlResponseCode.STABLE;
                    break;
                case AVRCP_RESPONSE_CHANGED:
                    respCode = RemoteControlResponseCode.CHANGED;
                    break;
                case AVRCP_RESPONSE_INTERIM:
                default:
                    respCode = RemoteControlResponseCode.INTERIM;
                    break;
                }

                address = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                try {
                    callbackHandler.trackReachedEndNotification(address, transactionID, status, respCode);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void trackReachedStartNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode) {
            RemoteControlEventCallback callbackHandler = (RemoteControlEventCallback)this.callbackHandler;

            BluetoothAddress address;
            RemoteControlResponseCode respCode;

            notificationTransactionMap.remove(transactionID);

            if(callbackHandler != null) {

                switch(responseCode) {
                case AVRCP_RESPONSE_NOT_IMPLEMENTED:
                    respCode = RemoteControlResponseCode.NOT_IMPLEMENTED;
                    break;
                case AVRCP_RESPONSE_ACCEPTED:
                    respCode = RemoteControlResponseCode.ACCEPTED;
                    break;
                case AVRCP_RESPONSE_REJECTED:
                    respCode = RemoteControlResponseCode.REJECTED;
                    break;
                case AVRCP_RESPONSE_IN_TRANSITION:
                    respCode = RemoteControlResponseCode.IN_TRANSITION;
                    break;
                case AVRCP_RESPONSE_STABLE:
                    respCode = RemoteControlResponseCode.STABLE;
                    break;
                case AVRCP_RESPONSE_CHANGED:
                    respCode = RemoteControlResponseCode.CHANGED;
                    break;
                case AVRCP_RESPONSE_INTERIM:
                default:
                    respCode = RemoteControlResponseCode.INTERIM;
                    break;
                }

                address = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                try {
                    callbackHandler.trackReachedStartNotification(address, transactionID, status, respCode);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void playbackPositionChangedNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, int position) {
            RemoteControlEventCallback callbackHandler = (RemoteControlEventCallback)this.callbackHandler;

            BluetoothAddress address;
            RemoteControlResponseCode respCode;

            notificationTransactionMap.remove(transactionID);

            if(callbackHandler != null) {

                switch(responseCode) {
                case AVRCP_RESPONSE_NOT_IMPLEMENTED:
                    respCode = RemoteControlResponseCode.NOT_IMPLEMENTED;
                    break;
                case AVRCP_RESPONSE_ACCEPTED:
                    respCode = RemoteControlResponseCode.ACCEPTED;
                    break;
                case AVRCP_RESPONSE_REJECTED:
                    respCode = RemoteControlResponseCode.REJECTED;
                    break;
                case AVRCP_RESPONSE_IN_TRANSITION:
                    respCode = RemoteControlResponseCode.IN_TRANSITION;
                    break;
                case AVRCP_RESPONSE_STABLE:
                    respCode = RemoteControlResponseCode.STABLE;
                    break;
                case AVRCP_RESPONSE_CHANGED:
                    respCode = RemoteControlResponseCode.CHANGED;
                    break;
                case AVRCP_RESPONSE_INTERIM:
                default:
                    respCode = RemoteControlResponseCode.INTERIM;
                    break;
                }

                address = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                try {
                    callbackHandler.playbackPositionChangedNotification(address, transactionID, status, new PlaybackPosititionChangedNotificationData(respCode, position));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void systemStatusChangedNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, int systemStatus) {
            RemoteControlEventCallback callbackHandler = (RemoteControlEventCallback)this.callbackHandler;

            BluetoothAddress address;
            RemoteControlResponseCode respCode;
            SystemStatus sStatus;

            notificationTransactionMap.remove(transactionID);

            if(callbackHandler != null) {

                switch(responseCode) {
                case AVRCP_RESPONSE_NOT_IMPLEMENTED:
                    respCode = RemoteControlResponseCode.NOT_IMPLEMENTED;
                    break;
                case AVRCP_RESPONSE_ACCEPTED:
                    respCode = RemoteControlResponseCode.ACCEPTED;
                    break;
                case AVRCP_RESPONSE_REJECTED:
                    respCode = RemoteControlResponseCode.REJECTED;
                    break;
                case AVRCP_RESPONSE_IN_TRANSITION:
                    respCode = RemoteControlResponseCode.IN_TRANSITION;
                    break;
                case AVRCP_RESPONSE_STABLE:
                    respCode = RemoteControlResponseCode.STABLE;
                    break;
                case AVRCP_RESPONSE_CHANGED:
                    respCode = RemoteControlResponseCode.CHANGED;
                    break;
                case AVRCP_RESPONSE_INTERIM:
                default:
                    respCode = RemoteControlResponseCode.INTERIM;
                    break;
                }

                address = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                switch(systemStatus) {
                case SYSTEM_STATUS_POWER_OFF:
                default:
                    sStatus = SystemStatus.POWER_OFF;
                    break;
                case SYSTEM_STATUS_POWER_ON:
                    sStatus = SystemStatus.POWER_ON;
                    break;
                case SYSTEM_STATUS_UNPLUGGED:
                    sStatus = SystemStatus.UNPLUGGED;
                    break;

                }

                try {
                    callbackHandler.systemStatusChangedNotification(address, transactionID, status, new SystemStatusChangedNotificationData(respCode, sStatus));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void volumeChangeNotificationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, byte volume) {
            RemoteControlEventCallback callbackHandler = (RemoteControlEventCallback)this.callbackHandler;

            BluetoothAddress address;
            RemoteControlResponseCode respCode;

            notificationTransactionMap.remove(transactionID);

            if(callbackHandler != null) {

                switch(responseCode) {
                case AVRCP_RESPONSE_NOT_IMPLEMENTED:
                    respCode = RemoteControlResponseCode.NOT_IMPLEMENTED;
                    break;
                case AVRCP_RESPONSE_ACCEPTED:
                    respCode = RemoteControlResponseCode.ACCEPTED;
                    break;
                case AVRCP_RESPONSE_REJECTED:
                    respCode = RemoteControlResponseCode.REJECTED;
                    break;
                case AVRCP_RESPONSE_IN_TRANSITION:
                    respCode = RemoteControlResponseCode.IN_TRANSITION;
                    break;
                case AVRCP_RESPONSE_STABLE:
                    respCode = RemoteControlResponseCode.STABLE;
                    break;
                case AVRCP_RESPONSE_CHANGED:
                    respCode = RemoteControlResponseCode.CHANGED;
                    break;
                case AVRCP_RESPONSE_INTERIM:
                default:
                    respCode = RemoteControlResponseCode.INTERIM;
                    break;
                }

                address = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                try {
                    callbackHandler.volumeChangedNotification(address, transactionID, status, new VolumeChangeNotificationData(respCode, (float)volume / (float)0x7F));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void setAbsoluteVolumeCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, byte volume) {
            RemoteControlEventCallback callbackHandler = (RemoteControlEventCallback)this.callbackHandler;

            BluetoothAddress address;
            RemoteControlResponseCode respCode;

            if(callbackHandler != null) {

                switch(responseCode) {
                case AVRCP_RESPONSE_NOT_IMPLEMENTED:
                    respCode = RemoteControlResponseCode.NOT_IMPLEMENTED;
                    break;
                case AVRCP_RESPONSE_ACCEPTED:
                    respCode = RemoteControlResponseCode.ACCEPTED;
                    break;
                case AVRCP_RESPONSE_REJECTED:
                    respCode = RemoteControlResponseCode.REJECTED;
                    break;
                case AVRCP_RESPONSE_IN_TRANSITION:
                    respCode = RemoteControlResponseCode.IN_TRANSITION;
                    break;
                case AVRCP_RESPONSE_STABLE:
                    respCode = RemoteControlResponseCode.STABLE;
                    break;
                case AVRCP_RESPONSE_CHANGED:
                    respCode = RemoteControlResponseCode.CHANGED;
                    break;
                case AVRCP_RESPONSE_INTERIM:
                default:
                    respCode = RemoteControlResponseCode.INTERIM;
                    break;
                }

                address = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                try {
                    callbackHandler.setAbsoluteVolumeCommandConfirmation(address, transactionID, status, new SetAbsoluteVolumeResponse(respCode, (float)volume / (float)0x7F));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void getCapabilitiesRejectedEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status) {
            RemoteControlEventCallback callbackHandler = (RemoteControlEventCallback)this.callbackHandler;
            CapabilityID capID;
            BluetoothAddress remoteDeviceAddress;

            capabilityTransactionMap.remove(transactionID);

            if(callbackHandler != null) {
                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                try {
                    capID = capabilityTransactionMap.get(transactionID);

                    switch(capID) {
                    case COMPANY_ID:
                        callbackHandler.getCompanyIDCapabilitiesCommandConfirmation(remoteDeviceAddress, transactionID, status, new GetCompanyIDCapabilitiesResponse(RemoteControlResponseCode.REJECTED, null));
                        break;
                    case EVENTS_SUPPORTED:
                        callbackHandler.getEventsSupportedCapabilitiesConfirmation(remoteDeviceAddress, transactionID, status, new GetEventsSupportedCapabilitiesResponse(RemoteControlResponseCode.REJECTED, null));
                        break;
                    default:
                        break;

                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void registerNotificationRejectedEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status) {
            RemoteControlEventCallback callbackHandler = (RemoteControlEventCallback)this.callbackHandler;
            EventID eventID;
            BluetoothAddress remoteDeviceAddress;

            if(callbackHandler != null) {
                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                eventID = notificationTransactionMap.get(transactionID);

                if(eventID != null) {
                    try {
                        switch(eventID) {
                        case PLAYBACK_STATUS_CHANGED:
                            callbackHandler.playbackStatusChangeNotification(remoteDeviceAddress, transactionID, status, new PlaybackStatusChangeNotificationData(RemoteControlResponseCode.REJECTED, null));
                            break;
                        case TRACK_CHANGED:
                            callbackHandler.trackChangedNotification(remoteDeviceAddress, transactionID, status, new TrackChangedNotificationData(RemoteControlResponseCode.REJECTED, 0));
                            break;
                        case TRACK_REACHED_END:
                            callbackHandler.trackReachedEndNotification(remoteDeviceAddress, transactionID, status, RemoteControlResponseCode.REJECTED);
                            break;
                        case TRACK_REACHED_START:
                            callbackHandler.trackReachedStartNotification(remoteDeviceAddress, transactionID, status, RemoteControlResponseCode.REJECTED);
                            break;
                        case PLAYBACK_POSITION_CHANGED:
                            callbackHandler.playbackPositionChangedNotification(remoteDeviceAddress, transactionID, status, new PlaybackPosititionChangedNotificationData(RemoteControlResponseCode.REJECTED, 0));
                            break;
                        case SYSTEM_STATUS_CHANGED:
                            callbackHandler.systemStatusChangedNotification(remoteDeviceAddress, transactionID, status, new SystemStatusChangedNotificationData(RemoteControlResponseCode.REJECTED, null));
                            break;
                        case VOLUME_CHANGED:
                            callbackHandler.volumeChangedNotification(remoteDeviceAddress, transactionID, status, new VolumeChangeNotificationData(RemoteControlResponseCode.REJECTED, 0));
                            break;
                        default:
                            break;
                        }
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        /* The following structure is a container structure that holds the */
        /* information that is returned in a */
        /* aetRemoteControlCommandIndication event. */
        @Override
        protected void remoteControlPassThroughCommandIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int commandType, int subunitType, int subunitID, int operationID, boolean stateFlag, byte operationData[]) {
            RemoteControlEventCallback callbackHandler = (RemoteControlEventCallback)this.callbackHandler;
            RemoteControlPassThroughCommand command;
            BluetoothAddress remoteDeviceAddress;

            if(callbackHandler != null) {
                command = convertPassThroughIndication(commandType, subunitType, subunitID, operationID, stateFlag, operationData);

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                try {
                    callbackHandler.remoteControlPassthroughCommandIndication(remoteDeviceAddress, transactionID, command);
                } catch(Exception e) {

                }
            }
        }

        /* The following structure is a container structure that holds the */
        /* information that is returned in a */
        /* aetRemoteControlCommandConfirmation event. */
        @Override
        protected void remoteControlPassThroughCommandConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int status, int responseCode, int subunitType, int subunitID, int operationID, boolean stateFlag, byte operationData[]) {
            RemoteControlEventCallback callbackHandler = (RemoteControlEventCallback)this.callbackHandler;
            RemoteControlPassThroughResponse response;
            BluetoothAddress remoteDeviceAddress;

            if(callbackHandler != null) {
                response = convertPassThroughConfirmation(responseCode, subunitType, subunitID, operationID, stateFlag, operationData);

                remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

                try {
                    callbackHandler.remoteControlPassthroughCommandConfirmation(remoteDeviceAddress, transactionID, status, response);
                } catch(Exception e) {

                }
            }
        }

        /**
         * Receiver for event notifications which apply to a remote control.
         * <p>
         * It is guaranteed that no two event methods will be invoked
         * simultaneously. However, because of this guarantee, the
         * implementation of each event handler must be as efficient as
         * possible, as subsequent events will not be sent until the in-progress
         * event handler returns. Furthermore, event handlers must not block and
         * wait on conditions that can only be satisfied by receiving other
         * events.
         * <p>
         * Implementors should also note that events are sent from a thread
         * context owned by the BluetopiaPM API, so standard locking practices
         * should be observed for data sharing.
         */
        public interface RemoteControlEventCallback extends EventCallback {
            /**
             * Invoked when a vendor dependent command is received.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device.
             * @param transactionID
             *            The Transaction ID associated with this command
             *            indication.
             * @param command
             *            The specific command data associated with this
             *            command.
             */
            public void vendorDependentCommandIndication(BluetoothAddress remoteDeviceAddress, byte transactionID, VendorDependentGenericCommand command);

            /**
             * Invoked when a get capabilities command is received.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device.
             * @param transactionID
             *            The Transaction ID associated with this command
             *            indication.
             * @param command
             *            The specific command data associated with this
             *            command.
             */
            public void getCapabilitiesCommandIndication(BluetoothAddress remoteDeviceAddress, byte transactionID, GetCapabilitiesCommand command);

            /**
             * Invoked when a get element attributes command is received.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device.
             * @param transactionID
             *            The Transaction ID associated with this command
             *            indication.
             * @param command
             *            The specific command data associated with this
             *            command.
             */
            public void getElementAttributesCommandIndication(BluetoothAddress remoteDeviceAddress, byte transactionID, GetElementAttributesCommand command);

            /**
             * Invoked when a get play status command is received.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device.
             * @param transactionID
             *            The Transaction ID associated with this command
             *            indication.
             * @param command
             *            The specific command data associated with this
             *            command.
             */
            public void getPlayStatusCommandIndication(BluetoothAddress remoteDeviceAddress, byte transactionID, GetPlayStatusCommand command);

            /**
             * Invoked when a register notification command is received.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device.
             * @param transactionID
             *            The Transaction ID associated with this command
             *            indication.
             * @param command
             *            The specific command data associated with this
             *            command.
             */
            public void registerNotificationCommandIndication(BluetoothAddress remoteDeviceAddress, byte transactionID, RegisterNotificationCommand command);

            /**
             * Invoked when a set absolute volume command is received.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device.
             * @param transactionID
             *            The Transaction ID associated with this command
             *            indication.
             * @param command
             *            The specific command data associated with this
             *            command.
             */
            public void setAbsoluteVolumeCommandIndication(BluetoothAddress remoteDeviceAddress, byte transactionID, SetAbsoluteVolumeCommand command);

            /**
             * Invoked when a pass through command is received.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device.
             * @param transactionID
             *            The Transaction ID associated with this command
             *            indication.
             * @param command
             *            The specific command data associated with this
             *            command.
             */
            public void remoteControlPassthroughCommandIndication(BluetoothAddress remoteDeviceAddress, byte transactionID, RemoteControlPassThroughCommand command);

            /**
             * Invoked when a vendor dependent command is completed.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device.
             * @param transactionID
             *            The Transaction ID associated with this command
             *            indication.
             * @param status
             *            The status of the command.
             * @param response
             *            The specific response data associated with this
             *            response.
             */
            public void vendorDependentCommandConfirmation(BluetoothAddress remoteDeviceAddress, byte transactionID, int status, VendorDependentGenericResponse response);

            /**
             * Invoked when a get company ID capabilities command is completed.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device.
             * @param transactionID
             *            The Transaction ID associated with this command
             *            indication.
             * @param status
             *            The status of the command.
             * @param response
             *            The specific response data associated with this
             *            response.
             */
            public void getCompanyIDCapabilitiesCommandConfirmation(BluetoothAddress remoteDeviceAddress, byte transactionID, int status, GetCompanyIDCapabilitiesResponse response);

            /**
             * Invoked when a get events supported capabilities command is
             * completed.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device.
             * @param transactionID
             *            The Transaction ID associated with this command
             *            indication.
             * @param status
             *            The status of the command.
             * @param response
             *            The specific response data associated with this
             *            response.
             */
            public void getEventsSupportedCapabilitiesConfirmation(BluetoothAddress remoteDeviceAddress, byte transactionID, int status, GetEventsSupportedCapabilitiesResponse response);

            /**
             * Invoked when a get element attributes command is completed.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device.
             * @param transactionID
             *            The Transaction ID associated with this command
             *            indication.
             * @param status
             *            The status of the command.
             * @param response
             *            The specific response data associated with this
             *            response.
             */
            public void getElementAttributesCommandConfirmation(BluetoothAddress remoteDeviceAddress, byte transactionID, int status, GetElementAttributesResponse response);

            /**
             * Invoked when a get play status command is completed.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device.
             * @param transactionID
             *            The Transaction ID associated with this command
             *            indication.
             * @param status
             *            The status of the command.
             * @param response
             *            The specific response data associated with this
             *            response.
             */
            public void getPlayStatusCommandConfirmation(BluetoothAddress remoteDeviceAddress, byte transactionID, int status, GetPlayStatusResponse response);

            /**
             * Invoked when a set absolute volume command is completed.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device.
             * @param transactionID
             *            The Transaction ID associated with this command
             *            indication.
             * @param status
             *            The status of the command.
             * @param response
             *            The specific response data associated with this
             *            response.
             */
            public void setAbsoluteVolumeCommandConfirmation(BluetoothAddress remoteDeviceAddress, byte transactionID, int status, SetAbsoluteVolumeResponse response);

            /**
             * Invoked when a pass through command is completed.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device.
             * @param transactionID
             *            The Transaction ID associated with this command
             *            indication.
             * @param status
             *            The status of the command.
             * @param response
             *            The specific response data associated with this
             *            response.
             */
            public void remoteControlPassthroughCommandConfirmation(BluetoothAddress remoteDeviceAddress, byte transactionID, int status, RemoteControlPassThroughResponse response);

            /**
             * Invoked when a register notification response is received for the
             * playback status changed event. This could indicate either a
             * request to register notifications has completed or a notification
             * data has been received. The Response Code in the Notification
             * data will indicate which instance this event corresponds to.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device.
             * @param transactionID
             *            The Transaction ID associated with this command
             *            indication.
             * @param status
             *            The status of the command.
             * @param notification
             *            The specific notification data
             */
            public void playbackStatusChangeNotification(BluetoothAddress remoteDeviceAddress, byte transactionID, int status, PlaybackStatusChangeNotificationData notification);

            /**
             * Invoked when a register notification response is received for the
             * track changed event. This could indicate either a request to
             * register notifications has completed or a notification data has
             * been received. The Response Code in the Notification data will
             * indicate which instance this event corresponds to.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device.
             * @param transactionID
             *            The Transaction ID associated with this command
             *            indication.
             * @param status
             *            The status of the command.
             * @param notification
             *            The specific notification data
             */
            public void trackChangedNotification(BluetoothAddress remoteDeviceAddress, byte transactionID, int status, TrackChangedNotificationData notification);

            /**
             * Invoked when a register notification response is received for the
             * track reached end event. This could indicate either a request to
             * register notifications has completed or a notification data has
             * been received. The Response Code will indicate which instance
             * this event corresponds to.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device.
             * @param transactionID
             *            The Transaction ID associated with this command
             *            indication.
             * @param status
             *            The status of the command.
             * @param responseCode
             *            The response code for this notification.
             */
            public void trackReachedEndNotification(BluetoothAddress remoteDeviceAddress, byte transactionID, int status, RemoteControlResponseCode responseCode);

            /**
             * Invoked when a register notification response is received for the
             * track reached start event. This could indicate either a request
             * to register notifications has completed or a notification data
             * has been received. The Response Code will indicate which instance
             * this event corresponds to.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device.
             * @param transactionID
             *            The Transaction ID associated with this command
             *            indication.
             * @param status
             *            The status of the command.
             * @param responseCode
             *            The response code for this notification.
             */
            public void trackReachedStartNotification(BluetoothAddress remoteDeviceAddress, byte transactionID, int status, RemoteControlResponseCode responseCode);

            /**
             * Invoked when a register notification response is received for the
             * playback position changed event. This could indicate either a
             * request to register notifications has completed or a notification
             * data has been received. The Response Code in the Notification
             * data will indicate which instance this event corresponds to.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device.
             * @param transactionID
             *            The Transaction ID associated with this command
             *            indication.
             * @param status
             *            The status of the command.
             * @param notification
             *            The specific notification data
             */
            public void playbackPositionChangedNotification(BluetoothAddress remoteDeviceAddress, byte transactionID, int status, PlaybackPosititionChangedNotificationData notification);

            /**
             * Invoked when a register notification response is received for the
             * system status changed event. This could indicate either a request
             * to register notifications has completed or a notification data
             * has been received. The Response Code in the Notification data
             * will indicate which instance this event corresponds to.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device.
             * @param transactionID
             *            The Transaction ID associated with this command
             *            indication.
             * @param status
             *            The status of the command.
             * @param notification
             *            The specific notification data
             */
            public void systemStatusChangedNotification(BluetoothAddress remoteDeviceAddress, byte transactionID, int status, SystemStatusChangedNotificationData notification);

            /**
             * Invoked when a register notification response is received for the
             * volume changed event. This could indicate either a request to
             * register notifications has completed or a notification data has
             * been received. The Response Code in the Notification data will
             * indicate which instance this event corresponds to.
             *
             * @param remoteDeviceAddress
             *            The {@link BluetoothAddress} of the remote device.
             * @param transactionID
             *            The Transaction ID associated with this command
             *            indication.
             * @param status
             *            The status of the command.
             * @param notification
             *            The specific notification data
             */
            public void volumeChangedNotification(BluetoothAddress remoteDeviceAddress, byte transactionID, int status, VolumeChangeNotificationData notification);
        }
    }

    //Helpers to share pass-through handling code between Audio and AVRCP managers
    protected int handlePassThroughCommand(BluetoothAddress remoteDeviceAddress, long responseTimeout, RemoteControlPassThroughCommand command) {
        int commandType;
        int subunitType;
        int subunitID;
        int operationID;
        boolean state;
        byte[] address = remoteDeviceAddress.internalByteArray();

        switch(command.commandType) {
        case CONTROL:
            commandType = AVRCP_CTYPE_CONTROL;
            break;
        case STATUS:
            commandType = AVRCP_CTYPE_STATUS;
            break;
        case SPECIFIC_INQUIRY:
            commandType = AVRCP_CTYPE_SPECIFIC_INQUIRY;
            break;
        case NOTIFY:
            commandType = AVRCP_CTYPE_NOTIFY;
            break;
        case GENERAL_INQUIRY:
        default:
            commandType = AVRCP_CTYPE_GENERAL_INQUIRY;
            break;
        }

        switch(command.subunitType) {
        case VIDEO_MONITOR:
            subunitType = AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR;
            break;
        case DISC_RECORDER_PLAYER:
            subunitType = AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER;
            break;
        case TAPE_RECORDER_PLAYER:
            subunitType = AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER;
            break;
        case TUNER:
            subunitType = AVRCP_SUBUNIT_TYPE_TUNER;
            break;
        case VIDEO_CAMERA:
            subunitType = AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA;
            break;
        case PANEL:
            subunitType = AVRCP_SUBUNIT_TYPE_PANEL;
            break;
        case VENDOR_SPECIFIC:
            subunitType = AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC;
            break;
        case EXTENDED:
            subunitType = AVRCP_SUBUNIT_TYPE_EXTENDED;
            break;
        case UNIT:
        default:
            subunitType = AVRCP_SUBUNIT_TYPE_UNIT;
            break;
        }

        switch(command.subunitID) {
        case INSTANCE_0:
            subunitID = AVRCP_SUBUNIT_ID_INSTANCE_0;
            break;
        case INSTANCE_1:
            subunitID = AVRCP_SUBUNIT_ID_INSTANCE_1;
            break;
        case INSTANCE_2:
            subunitID = AVRCP_SUBUNIT_ID_INSTANCE_2;
            break;
        case INSTANCE_3:
            subunitID = AVRCP_SUBUNIT_ID_INSTANCE_3;
            break;
        case INSTANCE_4:
            subunitID = AVRCP_SUBUNIT_ID_INSTANCE_4;
            break;
        case EXTENDED:
            subunitID = AVRCP_SUBUNIT_ID_EXTENDED;
            break;
        case IGNORE:
        default:
            subunitID = AVRCP_SUBUNIT_ID_IGNORE;
            break;
        }

        switch(command.operationID) {
        case SELECT:
            operationID = AVRCP_PASS_THROUGH_ID_SELECT;
            break;
        case UP:
            operationID = AVRCP_PASS_THROUGH_ID_UP;
            break;
        case DOWN:
            operationID = AVRCP_PASS_THROUGH_ID_DOWN;
            break;
        case LEFT:
            operationID = AVRCP_PASS_THROUGH_ID_LEFT;
            break;
        case RIGHT:
            operationID = AVRCP_PASS_THROUGH_ID_RIGHT;
            break;
        case RIGHT_UP:
            operationID = AVRCP_PASS_THROUGH_ID_RIGHT_UP;
            break;
        case RIGHT_DOWN:
            operationID = AVRCP_PASS_THROUGH_ID_RIGHT_DOWN;
            break;
        case LEFT_UP:
            operationID = AVRCP_PASS_THROUGH_ID_LEFT_UP;
            break;
        case LEFT_DOWN:
            operationID = AVRCP_PASS_THROUGH_ID_LEFT_DOWN;
            break;
        case ROOT_MENU:
            operationID = AVRCP_PASS_THROUGH_ID_ROOT_MENU;
            break;
        case SETUP_MENU:
            operationID = AVRCP_PASS_THROUGH_ID_SETUP_MENU;
            break;
        case CONTENTS_MENU:
            operationID = AVRCP_PASS_THROUGH_ID_CONTENTS_MENU;
            break;
        case FAVORITE_MENU:
            operationID = AVRCP_PASS_THROUGH_ID_FAVORITE_MENU;
            break;
        case EXIT:
            operationID = AVRCP_PASS_THROUGH_ID_EXIT;
            break;
        case NUM_0:
            operationID = AVRCP_PASS_THROUGH_ID_0;
            break;
        case NUM_1:
            operationID = AVRCP_PASS_THROUGH_ID_1;
            break;
        case NUM_2:
            operationID = AVRCP_PASS_THROUGH_ID_2;
            break;
        case NUM_3:
            operationID = AVRCP_PASS_THROUGH_ID_3;
            break;
        case NUM_4:
            operationID = AVRCP_PASS_THROUGH_ID_4;
            break;
        case NUM_5:
            operationID = AVRCP_PASS_THROUGH_ID_5;
            break;
        case NUM_6:
            operationID = AVRCP_PASS_THROUGH_ID_6;
            break;
        case NUM_7:
            operationID = AVRCP_PASS_THROUGH_ID_7;
            break;
        case NUM_8:
            operationID = AVRCP_PASS_THROUGH_ID_8;
            break;
        case NUM_9:
            operationID = AVRCP_PASS_THROUGH_ID_9;
            break;
        case DOT:
            operationID = AVRCP_PASS_THROUGH_ID_DOT;
            break;
        case ENTER:
            operationID = AVRCP_PASS_THROUGH_ID_ENTER;
            break;
        case CLEAR:
            operationID = AVRCP_PASS_THROUGH_ID_CLEAR;
            break;
        case CHANNEL_UP:
            operationID = AVRCP_PASS_THROUGH_ID_CHANNEL_UP;
            break;
        case CHANNEL_DOWN:
            operationID = AVRCP_PASS_THROUGH_ID_CHANNEL_DOWN;
            break;
        case PREVIOUS_CHANNEL:
            operationID = AVRCP_PASS_THROUGH_ID_PREVIOUS_CHANNEL;
            break;
        case SOUND_SELECT:
            operationID = AVRCP_PASS_THROUGH_ID_SOUND_SELECT;
            break;
        case INPUT_SELECT:
            operationID = AVRCP_PASS_THROUGH_ID_INPUT_SELECT;
            break;
        case DISPLAY_INFORMATION:
            operationID = AVRCP_PASS_THROUGH_ID_DISPLAY_INFORMATION;
            break;
        case HELP:
            operationID = AVRCP_PASS_THROUGH_ID_HELP;
            break;
        case PAGE_UP:
            operationID = AVRCP_PASS_THROUGH_ID_PAGE_UP;
            break;
        case PAGE_DOWN:
            operationID = AVRCP_PASS_THROUGH_ID_PAGE_DOWN;
            break;
        case POWER:
            operationID = AVRCP_PASS_THROUGH_ID_POWER;
            break;
        case VOLUME_UP:
            operationID = AVRCP_PASS_THROUGH_ID_VOLUME_UP;
            break;
        case VOLUME_DOWN:
            operationID = AVRCP_PASS_THROUGH_ID_VOLUME_DOWN;
            break;
        case MUTE:
            operationID = AVRCP_PASS_THROUGH_ID_MUTE;
            break;
        case PLAY:
            operationID = AVRCP_PASS_THROUGH_ID_PLAY;
            break;
        case STOP:
            operationID = AVRCP_PASS_THROUGH_ID_STOP;
            break;
        case PAUSE:
            operationID = AVRCP_PASS_THROUGH_ID_PAUSE;
            break;
        case RECORD:
            operationID = AVRCP_PASS_THROUGH_ID_RECORD;
            break;
        case REWIND:
            operationID = AVRCP_PASS_THROUGH_ID_REWIND;
            break;
        case FAST_FORWARD:
            operationID = AVRCP_PASS_THROUGH_ID_FAST_FORWARD;
            break;
        case EJECT:
            operationID = AVRCP_PASS_THROUGH_ID_EJECT;
            break;
        case FORWARD:
            operationID = AVRCP_PASS_THROUGH_ID_FORWARD;
            break;
        case BACKWARD:
            operationID = AVRCP_PASS_THROUGH_ID_BACKWARD;
            break;
        case ANGLE:
            operationID = AVRCP_PASS_THROUGH_ID_ANGLE;
            break;
        case SUBPICTURE:
            operationID = AVRCP_PASS_THROUGH_ID_SUBPICTURE;
            break;
        case F1:
            operationID = AVRCP_PASS_THROUGH_ID_F1;
            break;
        case F2:
            operationID = AVRCP_PASS_THROUGH_ID_F2;
            break;
        case F3:
            operationID = AVRCP_PASS_THROUGH_ID_F3;
            break;
        case F4:
            operationID = AVRCP_PASS_THROUGH_ID_F4;
            break;
        case F5:
            operationID = AVRCP_PASS_THROUGH_ID_F5;
            break;
        case VENDOR_UNIQUE:
        default:
            operationID = AVRCP_PASS_THROUGH_ID_VENDOR_UNIQUE;
            break;
        }

        switch(command.stateFlag) {
        case PRESSED:
            state = false;
            break;
        case RELEASED:
        default:
            state = true;
            break;
        }

        return sendRemoteControlPassThroughCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], responseTimeout, commandType, subunitType, subunitID, operationID, state, command.operationData);

    }

    protected int handlePassThroughResponse(BluetoothAddress remoteDeviceAddress, byte transactionID, RemoteControlPassThroughResponse response) {
        int responseCode;
        int subunitType;
        int subunitID;
        int operationID;
        boolean state;
        byte[] address = remoteDeviceAddress.internalByteArray();

        switch(response.responseCode) {
        case NOT_IMPLEMENTED:
            responseCode = AVRCP_RESPONSE_NOT_IMPLEMENTED;
            break;
        case ACCEPTED:
            responseCode = AVRCP_RESPONSE_ACCEPTED;
            break;
        case REJECTED:
            responseCode = AVRCP_RESPONSE_REJECTED;
            break;
        case IN_TRANSITION:
            responseCode = AVRCP_RESPONSE_IN_TRANSITION;
            break;
        case STABLE:
            responseCode = AVRCP_RESPONSE_STABLE;
            break;
        case CHANGED:
            responseCode = AVRCP_RESPONSE_CHANGED;
            break;
        case INTERIM:
        default:                    // QUESTION
            responseCode = AVRCP_RESPONSE_INTERIM;
            break;
        }

        switch(response.subunitType) {
        case VIDEO_MONITOR:
            subunitType = AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR;
            break;
        case DISC_RECORDER_PLAYER:
            subunitType = AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER;
            break;
        case TAPE_RECORDER_PLAYER:
            subunitType = AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER;
            break;
        case TUNER:
            subunitType = AVRCP_SUBUNIT_TYPE_TUNER;
            break;
        case VIDEO_CAMERA:
            subunitType = AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA;
            break;
        case PANEL:
            subunitType = AVRCP_SUBUNIT_TYPE_PANEL;
            break;
        case VENDOR_SPECIFIC:
            subunitType = AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC;
            break;
        case EXTENDED:
            subunitType = AVRCP_SUBUNIT_TYPE_EXTENDED;
            break;
        case UNIT:
        default:
            subunitType = AVRCP_SUBUNIT_TYPE_UNIT;
            break;
        }

        switch(response.subunitID) {
        case INSTANCE_0:
            subunitID = AVRCP_SUBUNIT_ID_INSTANCE_0;
            break;
        case INSTANCE_1:
            subunitID = AVRCP_SUBUNIT_ID_INSTANCE_1;
            break;
        case INSTANCE_2:
            subunitID = AVRCP_SUBUNIT_ID_INSTANCE_2;
            break;
        case INSTANCE_3:
            subunitID = AVRCP_SUBUNIT_ID_INSTANCE_3;
            break;
        case INSTANCE_4:
            subunitID = AVRCP_SUBUNIT_ID_INSTANCE_4;
            break;
        case EXTENDED:
            subunitID = AVRCP_SUBUNIT_ID_EXTENDED;
            break;
        case IGNORE:
        default:
            subunitID = AVRCP_SUBUNIT_ID_IGNORE;
            break;
        }

        switch(response.operationID) {
        case SELECT:
            operationID = AVRCP_PASS_THROUGH_ID_SELECT;
            break;
        case UP:
            operationID = AVRCP_PASS_THROUGH_ID_UP;
            break;
        case DOWN:
            operationID = AVRCP_PASS_THROUGH_ID_DOWN;
            break;
        case LEFT:
            operationID = AVRCP_PASS_THROUGH_ID_LEFT;
            break;
        case RIGHT:
            operationID = AVRCP_PASS_THROUGH_ID_RIGHT;
            break;
        case RIGHT_UP:
            operationID = AVRCP_PASS_THROUGH_ID_RIGHT_UP;
            break;
        case RIGHT_DOWN:
            operationID = AVRCP_PASS_THROUGH_ID_RIGHT_DOWN;
            break;
        case LEFT_UP:
            operationID = AVRCP_PASS_THROUGH_ID_LEFT_UP;
            break;
        case LEFT_DOWN:
            operationID = AVRCP_PASS_THROUGH_ID_LEFT_DOWN;
            break;
        case ROOT_MENU:
            operationID = AVRCP_PASS_THROUGH_ID_ROOT_MENU;
            break;
        case SETUP_MENU:
            operationID = AVRCP_PASS_THROUGH_ID_SETUP_MENU;
            break;
        case CONTENTS_MENU:
            operationID = AVRCP_PASS_THROUGH_ID_CONTENTS_MENU;
            break;
        case FAVORITE_MENU:
            operationID = AVRCP_PASS_THROUGH_ID_FAVORITE_MENU;
            break;
        case EXIT:
            operationID = AVRCP_PASS_THROUGH_ID_EXIT;
            break;
        case NUM_0:
            operationID = AVRCP_PASS_THROUGH_ID_0;
            break;
        case NUM_1:
            operationID = AVRCP_PASS_THROUGH_ID_1;
            break;
        case NUM_2:
            operationID = AVRCP_PASS_THROUGH_ID_2;
            break;
        case NUM_3:
            operationID = AVRCP_PASS_THROUGH_ID_3;
            break;
        case NUM_4:
            operationID = AVRCP_PASS_THROUGH_ID_4;
            break;
        case NUM_5:
            operationID = AVRCP_PASS_THROUGH_ID_5;
            break;
        case NUM_6:
            operationID = AVRCP_PASS_THROUGH_ID_6;
            break;
        case NUM_7:
            operationID = AVRCP_PASS_THROUGH_ID_7;
            break;
        case NUM_8:
            operationID = AVRCP_PASS_THROUGH_ID_8;
            break;
        case NUM_9:
            operationID = AVRCP_PASS_THROUGH_ID_9;
            break;
        case DOT:
            operationID = AVRCP_PASS_THROUGH_ID_DOT;
            break;
        case ENTER:
            operationID = AVRCP_PASS_THROUGH_ID_ENTER;
            break;
        case CLEAR:
            operationID = AVRCP_PASS_THROUGH_ID_CLEAR;
            break;
        case CHANNEL_UP:
            operationID = AVRCP_PASS_THROUGH_ID_CHANNEL_UP;
            break;
        case CHANNEL_DOWN:
            operationID = AVRCP_PASS_THROUGH_ID_CHANNEL_DOWN;
            break;
        case PREVIOUS_CHANNEL:
            operationID = AVRCP_PASS_THROUGH_ID_PREVIOUS_CHANNEL;
            break;
        case SOUND_SELECT:
            operationID = AVRCP_PASS_THROUGH_ID_SOUND_SELECT;
            break;
        case INPUT_SELECT:
            operationID = AVRCP_PASS_THROUGH_ID_INPUT_SELECT;
            break;
        case DISPLAY_INFORMATION:
            operationID = AVRCP_PASS_THROUGH_ID_DISPLAY_INFORMATION;
            break;
        case HELP:
            operationID = AVRCP_PASS_THROUGH_ID_HELP;
            break;
        case PAGE_UP:
            operationID = AVRCP_PASS_THROUGH_ID_PAGE_UP;
            break;
        case PAGE_DOWN:
            operationID = AVRCP_PASS_THROUGH_ID_PAGE_DOWN;
            break;
        case POWER:
            operationID = AVRCP_PASS_THROUGH_ID_POWER;
            break;
        case VOLUME_UP:
            operationID = AVRCP_PASS_THROUGH_ID_VOLUME_UP;
            break;
        case VOLUME_DOWN:
            operationID = AVRCP_PASS_THROUGH_ID_VOLUME_DOWN;
            break;
        case MUTE:
            operationID = AVRCP_PASS_THROUGH_ID_MUTE;
            break;
        case PLAY:
            operationID = AVRCP_PASS_THROUGH_ID_PLAY;
            break;
        case STOP:
            operationID = AVRCP_PASS_THROUGH_ID_STOP;
            break;
        case PAUSE:
            operationID = AVRCP_PASS_THROUGH_ID_PAUSE;
            break;
        case RECORD:
            operationID = AVRCP_PASS_THROUGH_ID_RECORD;
            break;
        case REWIND:
            operationID = AVRCP_PASS_THROUGH_ID_REWIND;
            break;
        case FAST_FORWARD:
            operationID = AVRCP_PASS_THROUGH_ID_FAST_FORWARD;
            break;
        case EJECT:
            operationID = AVRCP_PASS_THROUGH_ID_EJECT;
            break;
        case FORWARD:
            operationID = AVRCP_PASS_THROUGH_ID_FORWARD;
            break;
        case BACKWARD:
            operationID = AVRCP_PASS_THROUGH_ID_BACKWARD;
            break;
        case ANGLE:
            operationID = AVRCP_PASS_THROUGH_ID_ANGLE;
            break;
        case SUBPICTURE:
            operationID = AVRCP_PASS_THROUGH_ID_SUBPICTURE;
            break;
        case F1:
            operationID = AVRCP_PASS_THROUGH_ID_F1;
            break;
        case F2:
            operationID = AVRCP_PASS_THROUGH_ID_F2;
            break;
        case F3:
            operationID = AVRCP_PASS_THROUGH_ID_F3;
            break;
        case F4:
            operationID = AVRCP_PASS_THROUGH_ID_F4;
            break;
        case F5:
            operationID = AVRCP_PASS_THROUGH_ID_F5;
            break;
        case VENDOR_UNIQUE:
        default:
            operationID = AVRCP_PASS_THROUGH_ID_VENDOR_UNIQUE;
            break;
        }

        switch(response.stateFlag) {
        case PRESSED:
            state = false;
            break;
        case RELEASED:
        default:
            state = true;
            break;
        }

        return sendRemoteControlPassThroughResponseNative(address[0], address[1], address[2], address[3], address[4], address[5], transactionID, responseCode, subunitType, subunitID, operationID, state, response.operationData);
    }

    protected RemoteControlPassThroughCommand convertPassThroughIndication(int commandType, int subunitType, int subunitID, int operationID, boolean stateFlag, byte operationData[]) {
        RemoteControlCommandType cmdType;
        RemoteControlSubunitType subType;
        RemoteControlSubunitID subID;
        RemoteControlPassThroughOperationID key;

        switch(commandType) {
        case AVRCP_CTYPE_CONTROL:
            cmdType = RemoteControlCommandType.CONTROL;
            break;
        case AVRCP_CTYPE_STATUS:
            cmdType = RemoteControlCommandType.STATUS;
            break;
        case AVRCP_CTYPE_SPECIFIC_INQUIRY:
            cmdType = RemoteControlCommandType.SPECIFIC_INQUIRY;
            break;
        case AVRCP_CTYPE_NOTIFY:
            cmdType = RemoteControlCommandType.NOTIFY;
            break;
        case AVRCP_CTYPE_GENERAL_INQUIRY:
        default:
            cmdType = RemoteControlCommandType.GENERAL_INQUIRY;
            break;
        }

        switch(subunitType) {
        case AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR:
            subType = RemoteControlSubunitType.VIDEO_MONITOR;
            break;
        case AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER:
            subType = RemoteControlSubunitType.DISC_RECORDER_PLAYER;
            break;
        case AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER:
            subType = RemoteControlSubunitType.TAPE_RECORDER_PLAYER;
            break;
        case AVRCP_SUBUNIT_TYPE_TUNER:
            subType = RemoteControlSubunitType.TUNER;
            break;
        case AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA:
            subType = RemoteControlSubunitType.VIDEO_CAMERA;
            break;
        case AVRCP_SUBUNIT_TYPE_PANEL:
            subType = RemoteControlSubunitType.PANEL;
            break;
        case AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC:
            subType = RemoteControlSubunitType.VENDOR_SPECIFIC;
            break;
        case AVRCP_SUBUNIT_TYPE_EXTENDED:
            subType = RemoteControlSubunitType.EXTENDED;
            break;
        case AVRCP_SUBUNIT_TYPE_UNIT:
        default:
            subType = RemoteControlSubunitType.UNIT;
            break;
        }

        switch(subunitID) {
        case AVRCP_SUBUNIT_ID_INSTANCE_0:
            subID = RemoteControlSubunitID.INSTANCE_0;
            break;
        case AVRCP_SUBUNIT_ID_INSTANCE_1:
            subID = RemoteControlSubunitID.INSTANCE_1;
            break;
        case AVRCP_SUBUNIT_ID_INSTANCE_2:
            subID = RemoteControlSubunitID.INSTANCE_2;
            break;
        case AVRCP_SUBUNIT_ID_INSTANCE_3:
            subID = RemoteControlSubunitID.INSTANCE_3;
            break;
        case AVRCP_SUBUNIT_ID_INSTANCE_4:
            subID = RemoteControlSubunitID.INSTANCE_4;
            break;
        case AVRCP_SUBUNIT_ID_EXTENDED:
            subID = RemoteControlSubunitID.EXTENDED;
            break;
        case AVRCP_SUBUNIT_ID_IGNORE:
        default:
            subID = RemoteControlSubunitID.IGNORE;
            break;
        }

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

        return new RemoteControlPassThroughCommand(cmdType, subType, subID, key, ((stateFlag == false) ? RemoteControlPassThroughState.PRESSED : RemoteControlPassThroughState.RELEASED), operationData);

    }

    protected RemoteControlPassThroughResponse convertPassThroughConfirmation(int responseCode, int subunitType, int subunitID, int operationID, boolean stateFlag, byte operationData[]) {
        RemoteControlResponseCode respCode;
        RemoteControlSubunitType subType;
        RemoteControlSubunitID subID;
        RemoteControlPassThroughOperationID key;

        switch(responseCode) {
        case AVRCP_RESPONSE_NOT_IMPLEMENTED:
            respCode = RemoteControlResponseCode.NOT_IMPLEMENTED;
            break;
        case AVRCP_RESPONSE_ACCEPTED:
            respCode = RemoteControlResponseCode.ACCEPTED;
            break;
        case AVRCP_RESPONSE_REJECTED:
            respCode = RemoteControlResponseCode.REJECTED;
            break;
        case AVRCP_RESPONSE_IN_TRANSITION:
            respCode = RemoteControlResponseCode.IN_TRANSITION;
            break;
        case AVRCP_RESPONSE_STABLE:
            respCode = RemoteControlResponseCode.STABLE;
            break;
        case AVRCP_RESPONSE_CHANGED:
            respCode = RemoteControlResponseCode.CHANGED;
            break;
        case AVRCP_RESPONSE_INTERIM:
        default:
            respCode = RemoteControlResponseCode.INTERIM;
            break;
        }

        switch(subunitType) {
        case AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR:
            subType = RemoteControlSubunitType.VIDEO_MONITOR;
            break;
        case AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER:
            subType = RemoteControlSubunitType.DISC_RECORDER_PLAYER;
            break;
        case AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER:
            subType = RemoteControlSubunitType.TAPE_RECORDER_PLAYER;
            break;
        case AVRCP_SUBUNIT_TYPE_TUNER:
            subType = RemoteControlSubunitType.TUNER;
            break;
        case AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA:
            subType = RemoteControlSubunitType.VIDEO_CAMERA;
            break;
        case AVRCP_SUBUNIT_TYPE_PANEL:
            subType = RemoteControlSubunitType.PANEL;
            break;
        case AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC:
            subType = RemoteControlSubunitType.VENDOR_SPECIFIC;
            break;
        case AVRCP_SUBUNIT_TYPE_EXTENDED:
            subType = RemoteControlSubunitType.EXTENDED;
            break;
        case AVRCP_SUBUNIT_TYPE_UNIT:
        default:
            subType = RemoteControlSubunitType.UNIT;
            break;
        }

        switch(subunitID) {
        case AVRCP_SUBUNIT_ID_INSTANCE_0:
            subID = RemoteControlSubunitID.INSTANCE_0;
            break;
        case AVRCP_SUBUNIT_ID_INSTANCE_1:
            subID = RemoteControlSubunitID.INSTANCE_1;
            break;
        case AVRCP_SUBUNIT_ID_INSTANCE_2:
            subID = RemoteControlSubunitID.INSTANCE_2;
            break;
        case AVRCP_SUBUNIT_ID_INSTANCE_3:
            subID = RemoteControlSubunitID.INSTANCE_3;
            break;
        case AVRCP_SUBUNIT_ID_INSTANCE_4:
            subID = RemoteControlSubunitID.INSTANCE_4;
            break;
        case AVRCP_SUBUNIT_ID_EXTENDED:
            subID = RemoteControlSubunitID.EXTENDED;
            break;
        case AVRCP_SUBUNIT_ID_IGNORE:
        default:
            subID = RemoteControlSubunitID.IGNORE;
            break;
        }

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

        return new RemoteControlPassThroughResponse(respCode, subType, subID, key, ((stateFlag == false) ? RemoteControlPassThroughState.PRESSED : RemoteControlPassThroughState.RELEASED), operationData);
    }

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

    /* The following constants are used with the ConnectionFlags member */
    /* of the HFRM_Change_Incoming_Connection_Flags_Request_t structure */
    /* to specify the various flags to apply to incoming Connections. */
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

    /* The following constants are used with the ConnectionStatus member */
    /* of the HFRM_Device_Connection_Status_Message_t message to describe */
    /* the actual connection result Status. */
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

    /* The following constants are used with the ConnectedState member of */
    /* the AUDM_Query_Audio_Stream_Connected_Response_t message to denote */
    /* the various connection states of the Audio Stream. */
    private final static int AUDIO_STREAM_CONNECTED_STATE_DISCONNECTED = 0;
    private final static int AUDIO_STREAM_CONNECTED_STATE_CONNECTING   = 1;
    private final static int AUDIO_STREAM_CONNECTED_STATE_CONNECTED    = 2;

    /**
     * Possible connection states for an audio stream.
     */
    public enum AudioStreamConnectionState {
        /**
         * The audio stream is disconnected.
         */
        DISCONNECTED,
        /**
         * The audio stream is in the process of connecting.
         */
        CONNECTING,
        /**
         * The audio stream is connected.
         */
        CONNECTED
    }

    /* The following enumerated type is used to denote a specific Stream */
    /* Endpoint Type. */
    private final static int STREAM_TYPE_SNK = 0;
    private final static int STREAM_TYPE_SRC = 1;

    /**
     * Possible audio stream types.
     */
    public enum AudioStreamType {
        /**
         * The stream represents an audio sink (destination).
         */
        SINK,
        /**
         * The stream represents an audio source.
         */
        SOURCE
    }

    /* The following enumerated type is used to specify the current state */
    /* of an established Stream (either SRC or SNK). */
    private final static int STREAM_STATE_STOPPED = 0;
    private final static int STREAM_STATE_STARTED = 1;

    /**
     * Indicates the state of the audio stream as being either stopped or
     * started.
     */
    public enum AudioStreamState {
        /**
         * The audio stream is stopped.
         */
        STOPPED,
        /**
         * The audio stream has been started.
         */
        STARTED
    }

    /* The following enumerated type is used with the */
    /* AUD_Open_Request_Indication_Data_t event to denote the type of */
    /* connection that is being requested. */
    private final static int CONNECTION_REQUEST_TYPE_STREAM         = 0;
    private final static int CONNECTION_REQUEST_TYPE_REMOTE_CONTROL = 1;

    /**
     * Indicates whether the purpose of the connection request is for audio or
     * remote control commands.
     */
    public enum ConnectionRequestType {
        /**
         * The connection will be for audio.
         */
        AUDIO,
        /**
         * The connection will be for remote control commands.
         */
        REMOTE_CONTROL
    }

    /* The following structure is used to denote the settings of Stream */
    /* Data. */
    /**
     * Describes the format of an audio stream.
     */
    public static class AudioStreamFormat {
        /**
         * The sample frequency of the audio stream.
         */
        public long sampleFrequency;
        /**
         * The number of channels in the audio stream.
         */
        public int  numberChannels;

        /**
         * Sets the format of an audio stream.
         *
         * @param sampleFrequency
         *            The sample frequency of the audio stream.
         * @param numberChannels
         *            The number of channels in the audio stream.
         */
        public AudioStreamFormat(long sampleFrequency, int numberChannels) {
            this.sampleFrequency = sampleFrequency;
            this.numberChannels = numberChannels;
        }
    }

    /* The following structure is a container structure that is used with */
    /* the AUD_Query_Stream_Configuration() function to query the current */
    /* Stream Configuration. */
    /* * NOTE * The Media Codec members denote the low level A2DP */
    /* information that is being used on the stream. */
    /**
     * Describes the configuration of an audio stream.
     */
    public static class AudioStreamConfiguration {
        /**
         * The negotiated media MTU of the stream connection. This value
         * signifies the largest encoded data packet that can be sent or
         * received, including the header.
         */
        public int               mediaMTU;
        /**
         * The {@link AudioStreamFormat} describing the format of an audio
         * stream.
         */
        public AudioStreamFormat streamFormat;
        /**
         * The specific media codec type used for the audio stream.
         */
        public int               mediaCodecType;
        /**
         * The specific ADTP/GAVD configured media codec information. This
         * information contains the parameters required to encode and decode the
         * audio stream data.
         */
        public byte              mediaCodecInformation[];

        /**
         * Package-scope constructor for easy structure creation. Since this
         * class is only used to query the current configuration, this
         * constructor guarantees that all members are sufficiently initialized
         * for passing down to the native method.
         */
        AudioStreamConfiguration() {
            streamFormat = new AudioStreamFormat(0, 0);
        }
    }

    /* The following enumerated type is used to identify a reason for a */
    /* Disconnect. */
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

    /* AVRCP constants */

    /* The following constants define the various CTYPE values that are */
    /* used by AVRCP. */
    /* * NOTE * These values are defined in the IEEE 1394 "AV/C Digital */
    /* Interface Command Set" Specification. */
    private final static int AVRCP_CTYPE_CONTROL          = 0;
    private final static int AVRCP_CTYPE_STATUS           = 1;
    private final static int AVRCP_CTYPE_SPECIFIC_INQUIRY = 2;
    private final static int AVRCP_CTYPE_NOTIFY           = 3;
    private final static int AVRCP_CTYPE_GENERAL_INQUIRY  = 4;

    /**
     * Possible types of commands that are sent to a particular AV unit or
     * subunit.
     * <p>
     *
     * <i>Note:</i> These values are defined in the IEEE <i>1394 AV/C Digital
     * Interface Command Set</i> specification.
     */
    public enum RemoteControlCommandType {
        /**
         * Sent to instruct a device to perform an operation.
         */
        CONTROL,

        /**
         * Sent to query the current status of a device.
         */
        STATUS,

        /**
         * Sent to determine whether a device supports a particular control
         * command.
         */
        SPECIFIC_INQUIRY,

        /**
         * Sent to indicate the desire receive notification of future state
         * changes.
         */
        NOTIFY,

        /**
         * Sent to determine whether a device supports a particular control
         * command <i>without</i> being required to specify a particular set of
         * parameters for that command.
         */
        GENERAL_INQUIRY,
    }

    /* The following constants define the various RESPONSE values that */
    /* are used by AVRCP. */
    /* * NOTE * These values are defined in the IEEE 1394 "AV/C Digital */
    /* Interface Command Set" Specification. */
    private final static int AVRCP_RESPONSE_NOT_IMPLEMENTED = 0;
    private final static int AVRCP_RESPONSE_ACCEPTED        = 1;
    private final static int AVRCP_RESPONSE_REJECTED        = 2;
    private final static int AVRCP_RESPONSE_IN_TRANSITION   = 3;
    private final static int AVRCP_RESPONSE_STABLE          = 4;
    private final static int AVRCP_RESPONSE_CHANGED         = 5;
    private final static int AVRCP_RESPONSE_INTERIM         = 6;

    /**
     * Possible response codes.
     * <p>
     *
     * <i>Note:</i> The meaning of the response codes may vary depending on the
     * type of command sent. The specific meanings and values of these codes are
     * defined in the IEEE <i>1394 AV/C Digital Interface Command Set</i>
     * specification.
     */
    public enum RemoteControlResponseCode {
        /**
         * The device does not support the command.
         */
        NOT_IMPLEMENTED,

        /**
         * The device supports the command and the current state permits
         * execution.
         * <p>
         *
         * <i>Note:</i> execution of control commands may not necessarily be
         * completed at the time an ACCEPTED response is returned.
         */
        ACCEPTED,

        /**
         * The device supports the command or event notification. However, the
         * current state does not permit execution of the command, or the
         * requested information cannot currently be supplied.
         */
        REJECTED,

        /**
         * The device supports the command but is currently in a state of
         * transition.
         * <p>
         *
         * <i>Note:</i> A subsequent status command (at some unspecified future
         * time) may result in a STABLE state.
         */
        IN_TRANSITION,

        /**
         * The device supports the command and the requested information has
         * been returned.
         * <p>
         *
         * Also corresponds to an <i>IMPLEMENTED</i> response to an inquiry.
         */
        STABLE,

        /**
         * The target supports the specified event notification, and the
         *
         */
        CHANGED,

        /**
         * For control commands, indicates that the device supports the command
         * but is unable to respond with ACCEPTED or REJECTED within 100ms.
         * Unless a subsequent bus reset causes the transaction to be aborted,
         * the device will ultimately return an ACCEPTED or REJECTED response.
         * <p>
         *
         * For notification commands, indicates that the device supports the
         * requested event notification and has accepted the notify command for
         * any future change of state. At some future time, the device will
         * return a REJECTED or CHANGED response code.
         */
        INTERIM,
    }

    /* The following constants represent the supported SUBUNIT types. */
    /* * NOTE * These values are defined in the IEEE 1394 "AV/C Digital */
    /* Interface Command Set" Specification. */
    private final static int AVRCP_SUBUNIT_TYPE_VIDEO_MONITOR        = 0;
    private final static int AVRCP_SUBUNIT_TYPE_DISC_RECORDER_PLAYER = 1;
    private final static int AVRCP_SUBUNIT_TYPE_TAPE_RECORDER_PLAYER = 2;
    private final static int AVRCP_SUBUNIT_TYPE_TUNER                = 3;
    private final static int AVRCP_SUBUNIT_TYPE_VIDEO_CAMERA         = 4;
    private final static int AVRCP_SUBUNIT_TYPE_PANEL                = 5;
    private final static int AVRCP_SUBUNIT_TYPE_VENDOR_SPECIFIC      = 6;
    private final static int AVRCP_SUBUNIT_TYPE_EXTENDED             = 7;
    private final static int AVRCP_SUBUNIT_TYPE_UNIT                 = 8;

    /**
     * Possible subunit types.
     * <p>
     *
     * <i>Note:</i> These values are defined in the IEEE <i>1394 AV/C Digital
     * Interface Command Set</i> specification.
     */
    public enum RemoteControlSubunitType {
        /**
         * A video monitor.
         */
        VIDEO_MONITOR,

        /**
         * A disc recorder or player (audio or video).
         */
        DISC_RECORDER_PLAYER,

        /**
         * A tape recorder or player (audio or video).
         */
        TAPE_RECORDER_PLAYER,

        /**
         * A tuner.
         */
        TUNER,

        /**
         * A video camera.
         */
        VIDEO_CAMERA,

        /**
         * A user interface panel.
         */
        PANEL,

        /**
         * A vendor specific subunit.
         */
        VENDOR_SPECIFIC,

        /**
         * The subunit type is extended to the next byte.
         */
        EXTENDED,

        /**
         * Refers to the entire AV unit instead of one of its subunits, so long
         * as the specified subunit ID is {@link RemoteControlSubunitID#IGNORE}.
         */
        UNIT,
    }

    /* The following constants represent the supported SUBUNIT */
    /* identifiers. */
    /* * NOTE * These values are defined in the IEEE 1394 "AV/C Digital */
    /* Interface Command Set" Specification. */
    private final static int AVRCP_SUBUNIT_ID_INSTANCE_0 = 0;
    private final static int AVRCP_SUBUNIT_ID_INSTANCE_1 = 1;
    private final static int AVRCP_SUBUNIT_ID_INSTANCE_2 = 2;
    private final static int AVRCP_SUBUNIT_ID_INSTANCE_3 = 3;
    private final static int AVRCP_SUBUNIT_ID_INSTANCE_4 = 4;
    private final static int AVRCP_SUBUNIT_ID_EXTENDED   = 5;
    private final static int AVRCP_SUBUNIT_ID_IGNORE     = 6;

    /**
     * Possible subunit identifiers.
     * <p>
     *
     * <i>Note:</i> These values are defined in the IEEE <i>1394 AV/C Digital
     * Interface Command Set</i> specification, section 5.3.3.
     */
    public enum RemoteControlSubunitID {
        /**
         * Instance 0.
         */
        INSTANCE_0,

        /**
         * Instance 1.
         */
        INSTANCE_1,

        /**
         * Instance 2.
         */
        INSTANCE_2,

        /**
         * Instance 3.
         */
        INSTANCE_3,

        /**
         * Instance 4.
         */
        INSTANCE_4,

        /**
         * The subunit ID is extended to the next byte.
         */
        EXTENDED,

        /**
         * Used in conjunction with {@link RemoteControlSubunitType#UNIT} to
         * refer to the complete AV unit instead of one of its subunits.
         */
        IGNORE,
    }

    /* The following constants define the various Operation IDs. */
    /* * NOTE * A value of 0xFF is invalid and is a placeholder until the */
    /* correct value is known for that operation ID. */
    /* * NOTE * These values are defined in the IEEE 1394 "AV/C Digital */
    /* Interface Command Set" Specification. */
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
     * Possible remote control pass-through operation identifiers.
     * <p>
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
        VENDOR_UNIQUE,
    }

    /**
     * Defines the state of a remote control pass-through operation.
     */
    public enum RemoteControlPassThroughState {
        /**
         * The button performing the operation is pressed.
         */
        PRESSED,

        /**
         * The button performing the operation is released.
         */
        RELEASED
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
     * Defines a remote control command.
     */
    public abstract static class RemoteControlCommand {
        final static int                REMOTE_CONTROl_COMMAND_TYPE_PASSTHROUGH                                   = 0;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_UNIT_INFO                                     = 1;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_SUBUNIT_INFO                                  = 2;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_VENDOR_DEPENDENT                              = 3;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_BROWSING_CHANNEL_MESSAGE                      = 4;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_GROUP_NAVIGATION                              = 5;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_GET_CAPABILITIES                              = 6;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_LIST_PLAYER_APPLICATION_SETTING_ATTRIBUTES    = 7;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_LIST_PLAYER_APPLICATION_SETTING_VALUES        = 8;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_GET_CURRENT_PLAYER_APPLICATION_SETTING_VALUE  = 9;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_SET_PLAYER_APPLICATION_SETTING_VALUE          = 10;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_GET_PLAYER_APPLICATION_SETTING_ATTRIBUTE_TEXT = 11;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_GET_PLAYER_APPLICATION_SETTING_VALUE_TEXT     = 12;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_INFORM_DISPLAYABLE_CHARACTER_SET              = 13;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_INFORM_BATTERY_STATUS_OF_CT                   = 14;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_GET_ELEMENT_ATTRIBUTES                        = 15;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_REQUSET_CONTINUING_RESPONSE                   = 16;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_ABORT_CONTINUING_RESPONSE                     = 17;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_GET_PLAY_STATUS                               = 18;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_REGISTER_NOTIFICATION                         = 19;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_SET_ABSOLUTE_VOLUME                           = 20;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_SET_ADDRESSED_PLAYER                          = 21;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_PLAY_ITEM                                     = 22;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_ADD_TO_NOW_PLAYING                            = 23;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_SET_BROWSED_PLAYER                            = 24;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_CHANGE_PATH                                   = 25;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_GET_ITEM_ATTRIBUTE                            = 26;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_SEARCH_COMMAND                                = 27;
        final static int                REMOTE_CONTROL_COMMAND_TYPE_GET_FOLDER_ITEMS                              = 28;

        /* package */private final int  type;

        /**
         * The corresponding {@link RemoteControlCommandType} of the command.
         */
        public RemoteControlCommandType commandType;

        /**
         * The corresponding {@link RemoteControlSubunitType} of the command.
         */
        public RemoteControlSubunitType subunitType;

        /**
         * The corresponding {@link RemoteControlSubunitID} of the command.
         */
        public RemoteControlSubunitID   subunitID;

        private RemoteControlCommand(int type, RemoteControlCommandType command, RemoteControlSubunitType subtype, RemoteControlSubunitID subID) {
            this.type = type;
            this.commandType = command;
            this.subunitType = subtype;
            this.subunitID = subID;
        }
    }

    /**
     * Defines a remote control response.
     */
    public abstract static class RemoteControlResponse {
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_PASSTHROUGH                                   = 0;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_UNIT_INFO                                     = 1;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_SUBUNIT_INFO                                  = 2;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_VENDOR_DEPENDENT                              = 3;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_GROUP_NAVIGATION                              = 4;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_GET_COMPANY_CAPABILITIES                      = 5;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_GET_EVENT_CAPABILITIES                        = 6;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_LIST_PLAYER_APPLICATION_SETTING_ATTRIBUTES    = 7;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_LIST_PLAYER_APPLICATION_SETTING_VALUES        = 8;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_GET_CURRENT_PLAYER_APPLICATION_SETTING        = 9;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_SET_PLAYER_APPLICATION_SETTING_VALUE          = 10;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_GET_PLAYER_APPLICATION_SETTING_ATTRIBUTE_TEXT = 11;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_GET_PLAYER_APPLICATION_SETTING_VALUE_TEXT     = 12;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_INFORM_DISPLAYABLE_CHARACTER_SET              = 13;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_INFORM_BATTERY_STATUS                         = 14;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_GET_ELEMENT_ATTRIBUTES                        = 15;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_GET_PLAY_STATUS                               = 16;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_NOTIFICATION                                  = 17;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_ABORT_CONTINUING                              = 18;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_SET_ABSOLUTE_VOLUME                           = 19;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_SET_ADDRESSED_PLAYER                          = 20;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_PLAY_ITEM                                     = 21;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_ADD_TO_NOW_PLAYING                            = 22;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_COMMAND_REJECT                                = 23;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_SET_BROWSED_PLAYER                            = 24;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_CHANGE_PATH_RESPONSE                          = 25;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_GET_ITEM_ATTRIBUTES                           = 26;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_SEARCH                                        = 27;
        final static int                       REMOTE_CONTROL_RESPONSE_TYPE_GET_FOLDER_ITEMS                              = 28;

        /* package */final int                 type;

        /**
         * The corresponding {@link RemoteControlResponseCode} of the response.
         */
        public final RemoteControlResponseCode responseCode;

        /**
         * The corresponding {@link RemoteControlSubunitType} of the response.
         */
        public final RemoteControlSubunitType  subunitType;

        /**
         * The corresponding {@link RemoteControlSubunitID} of the response.
         */
        public final RemoteControlSubunitID    subunitID;

        private RemoteControlResponse(int type, RemoteControlResponseCode response, RemoteControlSubunitType subtype, RemoteControlSubunitID subID) {
            this.type = type;
            this.responseCode = response;
            this.subunitType = subtype;
            this.subunitID = subID;
        }
    }

    /**
     * Defines a remote control pass-through command.
     */
    public static class RemoteControlPassThroughCommand extends RemoteControlCommand {

        /**
         * The corresponding {@link RemoteControlPassThroughOperationID} of the
         * command.
         */
        public final RemoteControlPassThroughOperationID operationID;

        /**
         * The corresponding {@link RemoteControlPassThroughState} of the
         * command.
         */
        public final RemoteControlPassThroughState       stateFlag;

        /**
         * The operation-specific data.
         */
        public final byte                                operationData[];

        /**
         * Creates a RemoteControlPassThroughCommand with the given parameters
         * (without operation-specific data.)
         *
         * @param keyID
         *            The {@link RemoteControlPassThroughOperationID} of the
         *            command.
         *
         * @param keyState
         *            The {@link RemoteControlPassThroughState} of the command.
         */
        public RemoteControlPassThroughCommand(RemoteControlPassThroughOperationID keyID, RemoteControlPassThroughState keyState) {
            this(keyID, keyState, null);
        }

        /**
         * Creates a RemoteControlPassThroughCommand with the given parameters.
         *
         * @param keyID
         *            The {@link RemoteControlPassThroughOperationID} of the
         *            command.
         *
         * @param keyState
         *            The {@link RemoteControlPassThroughState} of the command.
         *
         * @param data
         *            The operation-specific data for the command.
         */
        public RemoteControlPassThroughCommand(RemoteControlPassThroughOperationID keyID, RemoteControlPassThroughState keyState, byte data[]) {
            super(REMOTE_CONTROl_COMMAND_TYPE_PASSTHROUGH, RemoteControlCommandType.CONTROL, RemoteControlSubunitType.PANEL, RemoteControlSubunitID.INSTANCE_0);

            operationID = keyID;
            stateFlag = keyState;
            operationData = ((data != null) ? data.clone() : null);
        }

        /* package */RemoteControlPassThroughCommand(RemoteControlCommandType commandType, RemoteControlSubunitType subunitType, RemoteControlSubunitID subunitID, RemoteControlPassThroughOperationID keyID, RemoteControlPassThroughState keyState, byte data[]) {
            super(REMOTE_CONTROl_COMMAND_TYPE_PASSTHROUGH, commandType, subunitType, subunitID);

            operationID = keyID;
            stateFlag = keyState;
            operationData = ((data != null) ? data.clone() : null);
        }
    }

    /**
     * Defines a response to a remote control pass-through command.
     */
    public static class RemoteControlPassThroughResponse extends RemoteControlResponse {

        /**
         * The {@link RemoteControlPassThroughOperationID} of the response.
         */

        public final RemoteControlPassThroughOperationID operationID;

        /**
         * The {@link RemoteControlPassThroughState} of the response.
         */
        public final RemoteControlPassThroughState       stateFlag;

        /**
         * The operation-specific data of the response.
         */
        public final byte                                operationData[];

        /* package */RemoteControlPassThroughResponse(RemoteControlResponseCode response, RemoteControlSubunitType subunitType, RemoteControlSubunitID subunitID, RemoteControlPassThroughOperationID keyID, RemoteControlPassThroughState keyState, byte data[]) {
            super(REMOTE_CONTROL_RESPONSE_TYPE_PASSTHROUGH, response, subunitType, subunitID);

            operationID = keyID;
            stateFlag = keyState;
            operationData = ((data != null) ? data.clone() : null);
        }

        /**
         * Creates a RemoteControlPassThroughResponse to a command directly from
         * the supplied parameters (without operation-specific data.)
         *
         * @param response
         *            The {@link RemoteControlResponseCode} of the response.
         *
         * @param keyID
         *            The {@link RemoteControlPassThroughOperationID} of the
         *            response.
         *
         * @param keyState
         *            The {@link RemoteControlPassThroughState} of the response.
         */
        public RemoteControlPassThroughResponse(RemoteControlResponseCode response, RemoteControlPassThroughOperationID keyID, RemoteControlPassThroughState keyState) {
            this(response, keyID, keyState, null);
        }

        /**
         * Creates a RemoteControlPassThroughResponse to a command directly from
         * the supplied parameters.
         *
         * @param response
         *            The {@link RemoteControlResponseCode} of the response.
         *
         * @param keyID
         *            The {@link RemoteControlPassThroughOperationID} of the
         *            response.
         *
         * @param keyState
         *            The {@link RemoteControlPassThroughState} of the command.
         *
         * @param data
         *            The operation-specific data.
         */
        public RemoteControlPassThroughResponse(RemoteControlResponseCode response, RemoteControlPassThroughOperationID keyID, RemoteControlPassThroughState keyState, byte data[]) {
            this(response, RemoteControlSubunitType.PANEL, RemoteControlSubunitID.INSTANCE_0, keyID, keyState, data);
        }

        /**
         * Creates a RemoteControlPassThroughResponse to a command.
         *
         * @param response
         *            The {@link RemoteControlResponseCode} of the response.
         *
         * @param command
         *            The {@link RemoteControlPassThroughCommand} of the
         *            response.
         */
        public RemoteControlPassThroughResponse(RemoteControlResponseCode response, RemoteControlPassThroughCommand command) {
            this(response, command.subunitType, command.subunitID, command.operationID, command.stateFlag, command.operationData);
        }
    }

    //    public static class UnitInfoCommand extends RemoteControlCommand {
    //        /**
    //         * The Unit Type of the Unit Info Command.
    //         */
    //        public RemoteControlSubunitType unitType;
    //
    //        /**
    //         * The Unit value of the Unit Info Command.
    //         */
    //        public int unit;
    //
    //        public UnitInfoCommand(RemoteControlSubunitType unitType, int unit) {
    //            super(REMOTE_CONTROL_COMMAND_TYPE_UNIT_INFO, RemoteControlCommandType.STATUS, RemoteControlSubunitType.UNIT, RemoteControlSubunitID.IGNORE);
    //
    //            this.unitType = unitType;
    //            this.unit = unit;
    //        }
    //    }
    //
    //
    //    public static class SubunitInfoCommand extends RemoteControlCommand {
    //        public int page;
    //
    //        SubunitInfoCommand(int page) {
    //            super(REMOTE_CONTROL_COMMAND_TYPE_SUBUNIT_INFO, RemoteControlCommandType.STATUS, RemoteControlSubunitType.UNIT, RemoteControlSubunitID.IGNORE);
    //
    //            this.page = page;
    //        }
    //    }

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
         * @param mostSignificant
         *            The most significant byte of the ID.
         * @param middleSignificant
         *            The most significant byte of the ID.
         * @param leastSignificant
         *            The least significant byte of the ID.
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
    }

    /**
     * Defines a vendor-dependent remote control command.
     *
     */
    public static class VendorDependentGenericCommand extends RemoteControlCommand {

        /**
         * The company ID.
         */
        public CompanyID companyID;

        /**
         * The raw data to include in the command
         */
        public byte[]    commandData;

        /**
         * Constructs a vendor-dependent remote control command.
         *
         * @param type
         *            The AV/C CType of this command.
         * @param subunitType
         *            The AV/C subunit type of this command.
         * @param subunitID
         *            The AV/C subunit ID of this command.
         * @param companyID
         *            The company ID.
         * @param commandData
         *            The raw data to include in the command.
         */
        public VendorDependentGenericCommand(RemoteControlCommandType type, RemoteControlSubunitType subunitType, RemoteControlSubunitID subunitID, CompanyID companyID, byte[] commandData) {
            super(REMOTE_CONTROL_COMMAND_TYPE_VENDOR_DEPENDENT, type, subunitType, subunitID);

            this.companyID = companyID;
            this.commandData = (commandData == null) ? null : commandData.clone();
        }
    }

    /*
     * public static class GroupNavigationCommand extends RemoteControlCommand {
     * 
     * static final int GROUP_NAVIGATION_TYPE_NEXT_GROUP = 0x0000; static final
     * int GROUP_NAVIGATION_TYPE_PREV_GROUP = 0x0001;
     * 
     * public enum GroupNavigationType { NEXT_GROUP, PREV_GROUP }
     * 
     * RemoteControlButtonState buttonState; GroupNavigationType navigationType;
     * 
     * public GroupNavigationCommand(RemoteControlButtonState buttonState,
     * GroupNavigationType navigationType) {
     * super(REMOTE_CONTROL_COMMAND_TYPE_GROUP_NAVIGATION,
     * RemoteControlCommandType.CONTROL, RemoteControlSubunitType.PANEL,
     * RemoteControlSubunitID.INSTANCE_0);
     * 
     * this.buttonState = buttonState; this.navigationType = navigationType; }
     * 
     * }
     */

    protected static final int CAPABILITY_ID_COMPANY_ID       = 0x01;
    protected static final int CAPABILITY_ID_EVENTS_SUPPORTED = 0x02;

    /**
     * The types of capability sets which can be requested from a remote control
     * endpoint.
     *
     */
    public enum CapabilityID {
        /**
         * The list of supported companyIDs.
         * 
         * @see AUDM#BLUETOOTH_SIG_COMPANY_ID
         */
        COMPANY_ID,

        /**
         * The list of notifiable events the Target supports registration for.
         */
        EVENTS_SUPPORTED
    }

    /**
     * Defines a Get Capabilities remote control command.
     *
     */
    public static class GetCapabilitiesCommand extends RemoteControlCommand {

        /**
         * The type of capabilities requested.
         */
        public CapabilityID capabilityID;

        /**
         * Constructs a Get Capabilities remote control command.
         *
         * @param capabilityID
         *            The capability type to request.
         */
        public GetCapabilitiesCommand(CapabilityID capabilityID) {
            super(REMOTE_CONTROL_COMMAND_TYPE_GET_CAPABILITIES, RemoteControlCommandType.STATUS, RemoteControlSubunitType.PANEL, RemoteControlSubunitID.INSTANCE_0);

            this.capabilityID = capabilityID;
        }
    }

    protected static final int ELEMENT_ATTRIBUTE_ID_TITLE                 = 0x00000001;
    protected static final int ELEMENT_ATTRIBUTE_ID_ARTIST                = 0x00000002;
    protected static final int ELEMENT_ATTRIBUTE_ID_ALBUM                 = 0x00000004;
    protected static final int ELEMENT_ATTRIBUTE_ID_NUMBER_OF_MEDIA       = 0x00000008;
    protected static final int ELEMENT_ATTRIBUTE_ID_TOTAL_NUMBER_OF_MEDIA = 0x00000010;
    protected static final int ELEMENT_ATTRIBUTE_ID_GENRE                 = 0x00000020;
    protected static final int ELEMENT_ATTRIBUTE_ID_PLAYING_TIME          = 0x00000040;

    /**
     * The possible media meta-data elements.
     *
     */
    public enum ElementAttributeID {
        /**
         * Title of the Media
         */
        TITLE,

        /**
         * Artist of the Media.
         */
        ARTIST,

        /**
         * Album of the Media.
         */
        ALBUM,

        /**
         * Number of the Media (ex. Track Number).
         */
        NUMBER_OF_MEDIA,

        /**
         * Total number of Media (ex. Number of tracks on album).
         */
        TOTAL_NUMBER_OF_MEDIA,

        /**
         * Genre of the media.
         */
        GENRE,

        /**
         * Playing time of the Media. <br>
         * NOTE: It is returned in milliseconds.
         */
        PLAYING_TIME,
    }

    /**
     * Defines a Get Element Attribute remote control command.
     *
     */
    public static class GetElementAttributesCommand extends RemoteControlCommand {
        /**
         * Media identifier which represents the currently playing media.
         */
        public static final int            MEDIA_IDENTIFIER_CURRENTLY_PLAYING = 0x00;

        /**
         * The identifier of the media to retrieve attributes for.
         * 
         * @see #MEDIA_IDENTIFIER_CURRENTLY_PLAYING
         */
        public long                        identifier;

        /**
         * The Attributes to request.
         */
        public EnumSet<ElementAttributeID> attributeIDs;

        /**
         * Constructs an instance of a Get Element Attributes Command.
         *
         * @param identifier
         *            The identifier of the media to retrieve attributes for.
         * @param attributeIDs
         *            The Attributes to request.
         *            <p>
         *            NOTE:<br>
         *            An empty attribute set will request all attributes.
         */
        public GetElementAttributesCommand(long identifier, EnumSet<ElementAttributeID> attributeIDs) {
            super(REMOTE_CONTROL_COMMAND_TYPE_GET_ELEMENT_ATTRIBUTES, RemoteControlCommandType.STATUS, RemoteControlSubunitType.PANEL, RemoteControlSubunitID.INSTANCE_0);

            this.identifier = identifier;
            this.attributeIDs = attributeIDs;
        }

        /**
         * Constructs an instance of a Get Element Attributes Command to get the
         * attributes of the currently playing media.
         *
         * @param attributeIDs
         *            The Attributes to request.
         *            <p>
         *            NOTE:<br>
         *            An empty attribute set will request all attributes.
         */
        public GetElementAttributesCommand(EnumSet<ElementAttributeID> attributeIDs) {
            this(MEDIA_IDENTIFIER_CURRENTLY_PLAYING, attributeIDs);
        }

    }

    /**
     * Defines a Get Play Status remote control command.
     *
     */
    public static class GetPlayStatusCommand extends RemoteControlCommand {
        /**
         * Constructs a Get Play Status remote control command.
         */
        public GetPlayStatusCommand() {
            super(REMOTE_CONTROL_COMMAND_TYPE_GET_PLAY_STATUS, RemoteControlCommandType.STATUS, RemoteControlSubunitType.PANEL, RemoteControlSubunitID.INSTANCE_0);
        }
    }

    protected static final int EVENT_ID_PLAYBACK_STATUS_CHANGED            = 0x0001;
    protected static final int EVENT_ID_TRACK_CHANGED                      = 0x0002;
    protected static final int EVENT_ID_TRACK_REACHED_END                  = 0x0004;
    protected static final int EVENT_ID_TRACK_REACHED_START                = 0x0008;
    protected static final int EVENT_ID_PLAYBACK_POSITION_CHANGED          = 0x0010;
    protected static final int EVENT_ID_BATTERY_STATUS_CHANGED             = 0x0020;
    protected static final int EVENT_ID_SYSTEM_STATUS_CHANGED              = 0x0040;
    protected static final int EVENT_ID_PLAYER_APPLICATION_SETTING_CHANGED = 0x0080;
    protected static final int EVENT_ID_NOW_PLAYING_CONTENT_CHANGED        = 0x0100;
    protected static final int EVENT_ID_AVAILABLE_PLAYERS_CHANGED          = 0x0200;
    protected static final int EVENT_ID_ADDRESSED_PLAYER_CHANGED           = 0x0400;
    protected static final int EVENT_ID_UIDS_CHANGED                       = 0x0800;
    protected static final int EVENT_ID_VOLUME_CHANGED                     = 0x1000;

    /**
     * The possible Notification Events which can be registered for on a Target.
     *
     */
    public enum EventID {
        /**
         * Notifies when the playback status has changed.
         */
        PLAYBACK_STATUS_CHANGED,

        /**
         * Notifies when the track has changed.
         */
        TRACK_CHANGED,

        /**
         * Notifies when the end of a track is reached.
         */
        TRACK_REACHED_END,

        /**
         * Notifies when the start of a track is reached.
         */
        TRACK_REACHED_START,

        /**
         * Notifies when the playback position has changed passed the specified
         * playback interval.
         */
        PLAYBACK_POSITION_CHANGED,

        /**
         * This event is not currently supported.
         */
        BATTERY_STATUS_CHANGED, //XXX

        /**
         * Notifies when the system status has changed.
         */
        SYSTEM_STATUS_CHANGED,

        /**
         * This event is not currently supported.
         */
        PLAYER_APPLICATION_SETTING_CHANGED, //XXX

        /**
         * This event is not currently supported.
         */
        NOW_PLAYING_CONTENT_CHANGED, //XXX

        /**
         * This event is not currently supported.
         */
        AVAILABLE_PLAYERS_CHANGED, //XXX

        /**
         * This event is not currently supported.
         */
        ADDRESSED_PLAYER_CHANGED, //XXX

        /**
         * This event is not currently supported.
         */
        UIDS_CHANGED, //XXX

        /**
         * Notifies when the Target changes the volume.
         */
        VOLUME_CHANGED
    }

    /**
     * Defines a remote control command to register for a specific event
     * notification.
     *
     */
    public static class RegisterNotificationCommand extends RemoteControlCommand {
        /**
         * The event to with which to register notifications.
         */
        public EventID eventID;

        /**
         * The interval at which to receive Playback Posistion changed events.
         * This is only used if {@link #eventID} is set to
         * {@link EventID#PLAYBACK_POSITION_CHANGED}
         */
        public int     playbackInterval;

        /**
         * Constructs an instance of a Register Notification Command
         *
         * @param eventID
         *            The event to with which to register notifications.
         * @param playbackInterval
         *            The interval at which to receive Playback Posistion
         *            changed events. This is only used if {@link #eventID} is
         *            set to {@link EventID#PLAYBACK_POSITION_CHANGED}
         */
        public RegisterNotificationCommand(EventID eventID, int playbackInterval) {
            super(REMOTE_CONTROL_COMMAND_TYPE_REGISTER_NOTIFICATION, RemoteControlCommandType.NOTIFY, RemoteControlSubunitType.PANEL, RemoteControlSubunitID.INSTANCE_0);

            this.eventID = eventID;
            this.playbackInterval = playbackInterval;
        }

    }

    /**
     * Defines a Set Absolute Volume remote control command.
     *
     */
    public static class SetAbsoluteVolumeCommand extends RemoteControlCommand {

        /**
         * The absolute volume to be set. The floating point value MUST be
         * between 0.0-1.0 inclusive.
         */
        public float absoluteVolume;

        /**
         * Constructs an instance of a Set Absolute Volume Command
         *
         * @param absoluteVolume
         *            The absolute volume to be set. The floating point value
         *            MUST be between 0.0-1.0 inclusive.
         */
        public SetAbsoluteVolumeCommand(float absoluteVolume) {
            super(REMOTE_CONTROL_COMMAND_TYPE_SET_ABSOLUTE_VOLUME, RemoteControlCommandType.CONTROL, RemoteControlSubunitType.PANEL, RemoteControlSubunitID.INSTANCE_0);

            this.absoluteVolume = absoluteVolume;
        }
    }

    /**
     * Defines a vendor specific remote control response.
     *
     */
    public static class VendorDependentGenericResponse extends RemoteControlResponse {

        /**
         * The companyID if this response.
         */
        public CompanyID companyID;

        /**
         * The raw data to send in this response.
         */
        public byte[]    commandData;

        /**
         * Constructs a vendor-dependent remote control response.
         *
         * @param response
         *            The response code to send.
         * @param subunitType
         *            The AV/C subunit type of this command.
         * @param subunitID
         *            The AV/C subunit ID of this command.
         * @param companyID
         *            The company ID.
         * @param commandData
         *            The raw data to send in the response.
         */
        public VendorDependentGenericResponse(RemoteControlResponseCode response, RemoteControlSubunitType subunitType, RemoteControlSubunitID subunitID, CompanyID companyID, byte[] commandData) {
            super(REMOTE_CONTROL_RESPONSE_TYPE_VENDOR_DEPENDENT, response, subunitType, subunitID);

            this.companyID = companyID;
            this.commandData = (commandData == null) ? null : commandData.clone();
        }

        /**
         * Constructs a vendor-dependent remote control response in response to
         * a given vendor-dependent command.
         *
         * @param command
         *            The command to format the response for.
         * @param response
         *            The response code.
         * @param companyID
         *            The companyID.
         * @param commandData
         *            The raw data to send in the response.
         */
        public VendorDependentGenericResponse(VendorDependentGenericCommand command, RemoteControlResponseCode response, CompanyID companyID, byte[] commandData) {
            this(response, command.subunitType, command.subunitID, companyID, commandData);

        }
    }

    /**
     * Defines a Get Capabilities Response for the
     * {@link CapabilityID#COMPANY_ID} command type.
     *
     */
    public static class GetCompanyIDCapabilitiesResponse extends RemoteControlResponse {
        /**
         * The list of company IDs supported.<br>
         * The first element in this list shall be
         * {@link AUDM#BLUETOOTH_SIG_COMPANY_ID}.
         */
        public CompanyID[] companyIDs;

        /**
         * Constructs a Get Capabilities Response for the
         * {@link CapabilityID#COMPANY_ID} command type.
         *
         * @param response
         *            The response code.
         * @param companyIDs
         *            The supported companyIDS.
         *
         */
        public GetCompanyIDCapabilitiesResponse(RemoteControlResponseCode response, CompanyID[] companyIDs) {
            super(REMOTE_CONTROL_RESPONSE_TYPE_GET_COMPANY_CAPABILITIES, response, RemoteControlSubunitType.PANEL, RemoteControlSubunitID.INSTANCE_0);

            this.companyIDs = companyIDs;
        }

    }

    /**
     * Defines a Get Capabilities Response for the
     * {@link CapabilityID#EVENTS_SUPPORTED} command type.
     *
     */
    public static class GetEventsSupportedCapabilitiesResponse extends RemoteControlResponse {
        /**
         * The supported event types.
         */
        public EnumSet<EventID> eventIDs;

        /**
         * Constructs a Get Capabilities Response for the
         * {@link CapabilityID#EVENTS_SUPPORTED} command type.
         *
         * @param responseCode
         *            The response code.
         * @param eventIDs
         *            The support event types.
         */
        public GetEventsSupportedCapabilitiesResponse(RemoteControlResponseCode responseCode, EnumSet<EventID> eventIDs) {
            super(REMOTE_CONTROL_RESPONSE_TYPE_GET_EVENT_CAPABILITIES, responseCode, RemoteControlSubunitType.PANEL, RemoteControlSubunitID.INSTANCE_0);

            this.eventIDs = eventIDs;
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
         * The character set code is defined by the IANA MIBenum list.
         */
        public int                ianaCharacterSet;

        /**
         * The raw attribute data for the element.
         */
        public byte[]             attributeData;

        /**
         * Constructs an element attribute for media meta-data.
         *
         * @param attributeID
         *            The type of attribute represented.
         * @param ianaCharacterSet
         *            The character set code is defined by the IANA MIBenum
         *            list.
         * @param attributeData
         *            The raw attribute data for the element.
         */
        public ElementAttribute(ElementAttributeID attributeID, int ianaCharacterSet, byte[] attributeData) {
            this.attributeID = attributeID;
            this.ianaCharacterSet = ianaCharacterSet;
            this.attributeData = attributeData;
        }

    }

    /**
     * Defines a Get Element Attributes remote control response.
     *
     */
    public static class GetElementAttributesResponse extends RemoteControlResponse {
        /**
         * The list of responded attribute data.
         */
        public ElementAttribute[] attributes;

        /**
         * Constructs a Get Element Attributes remote control response.
         *
         * @param responseCode
         *            The response code.
         * @param attributes
         *            The attributes to include in the response.
         */
        public GetElementAttributesResponse(RemoteControlResponseCode responseCode, ElementAttribute[] attributes) {
            super(REMOTE_CONTROL_RESPONSE_TYPE_GET_ELEMENT_ATTRIBUTES, responseCode, RemoteControlSubunitType.PANEL, RemoteControlSubunitID.INSTANCE_0);

            this.attributes = attributes;
        }
    }

    /**
     * Defines a Get Play Status remote control response.
     *
     */
    public static class GetPlayStatusResponse extends RemoteControlResponse {
        /**
         * The length of the current song (in milliseconds).
         */
        public int        songLength;

        /**
         * The current position of the song (in milliseconds).
         */
        public int        songPosition;

        /**
         * The current status of the song.
         */
        public PlayStatus playStatus;

        /**
         * Constructs a Get Play Status remote control response.
         *
         * @param response
         *            The response code.
         * @param length
         *            The length of the current song (in milliseconds).
         * @param position
         *            The current position of the song (in milliseconds).
         * @param status
         *            The current status of the song.
         */
        public GetPlayStatusResponse(RemoteControlResponseCode response, int length, int position, PlayStatus status) {
            super(REMOTE_CONTROL_RESPONSE_TYPE_GET_PLAY_STATUS, response, RemoteControlSubunitType.PANEL, RemoteControlSubunitID.INSTANCE_0);

            this.songLength = length;
            this.songPosition = position;
            this.playStatus = status;
        }
    }

    /**
     * An abstract base class representing Notification Data sent in a
     * {@link RegisterNotificationResponse}.
     *
     */
    public static abstract class RemoteControlNotificationData {
        /* package */EventID             eventID;

        /**
         * The response code associated with this notification.
         */
        public RemoteControlResponseCode responseCode;

        /* package */RemoteControlNotificationData(EventID id, RemoteControlResponseCode responseCode) {
            eventID = id;
            this.responseCode = responseCode;
        }
    }

    protected static final int PLAY_STATUS_STOPPED  = 0x01;
    protected static final int PLAY_STATUS_PLAYING  = 0x02;
    protected static final int PLAY_STATUS_PAUSED   = 0x03;
    protected static final int PLAY_STATUS_FWD_SEEK = 0x04;
    protected static final int PLAY_STATUS_REV_SEEK = 0x05;
    protected static final int PLAY_STATUS_ERROR    = 0x06;

    /**
     * The possible play statuses of a Target.
     *
     */
    public enum PlayStatus {
        /**
         * Media is stopped.
         */
        STOPPED,

        /**
         * Media is playing.
         */
        PLAYING,

        /**
         * Media is paused.
         */
        PAUSED,

        /**
         * Media is seeking forward.
         */
        FWD_SEEK,

        /**
         * Media is seeking backward.
         */
        REV_SEEK,

        /**
         * The media encountered an error.
         */
        ERROR
    }

    /**
     * Defines data returned in a Playback Status Change notification.
     *
     */
    public static class PlaybackStatusChangeNotificationData extends RemoteControlNotificationData {
        /**
         * The new Playback Status.
         */
        public PlayStatus playStatus;

        /**
         * Constructs the data returned in a Playback Status Change
         * notification.
         * 
         * @param responseCode
         *            The response code associated with this notification.
         *
         * @param playStatus
         *            The new Playback Status.
         */
        public PlaybackStatusChangeNotificationData(RemoteControlResponseCode responseCode, PlayStatus playStatus) {
            super(EventID.PLAYBACK_STATUS_CHANGED, responseCode);
            this.playStatus = playStatus;
        }
    }

    /**
     * Defines the data returned in a Track Changed notification.
     *
     */
    public static class TrackChangedNotificationData extends RemoteControlNotificationData {
        /**
         * The identifier of the track.
         */
        public long identifier;

        /**
         * Constructs the data returned in a Track Changed notification.
         * 
         * @param responseCode
         *            The response code associated with this notification.
         *
         * @param id
         *            The identifier of the track.
         */
        public TrackChangedNotificationData(RemoteControlResponseCode responseCode, long id) {
            super(EventID.TRACK_CHANGED, responseCode);
            this.identifier = id;
        }
    }

    /**
     * Defines the data returned in a Playback Position Changed notification.
     *
     */
    public static class PlaybackPosititionChangedNotificationData extends RemoteControlNotificationData {
        /**
         * The current position of the media (in milliseconds).
         */
        public int playbackPosition;

        /**
         * Constructs the data returned in a Playback Position Changed
         * notification.
         * 
         * @param responseCode
         *            The response code associated with this notification.
         *
         * @param pos
         *            The current position of the media (in milliseconds).
         */
        public PlaybackPosititionChangedNotificationData(RemoteControlResponseCode responseCode, int pos) {
            super(EventID.PLAYBACK_POSITION_CHANGED, responseCode);
            this.playbackPosition = pos;
        }
    }

    protected static final int SYSTEM_STATUS_POWER_ON  = 0x01;
    protected static final int SYSTEM_STATUS_POWER_OFF = 0x02;
    protected static final int SYSTEM_STATUS_UNPLUGGED = 0x03;

    /**
     * The possible system statuses.
     *
     */
    public enum SystemStatus {
        /**
         * System is powered on.
         */
        POWER_ON,

        /**
         * System is powered off.
         */
        POWER_OFF,

        /**
         * System is unplugged.
         */
        UNPLUGGED
    }

    /**
     * Defines the data returned in a System Status Changed notification.
     *
     */
    public static class SystemStatusChangedNotificationData extends RemoteControlNotificationData {
        /**
         * The new system status.
         */
        public SystemStatus status;

        /**
         * Constructs the data returned in a System Status Changed notification.
         * 
         * @param responseCode
         *            The response code associated with this notification.
         *
         * @param status
         *            The new system status.
         */
        public SystemStatusChangedNotificationData(RemoteControlResponseCode responseCode, SystemStatus status) {
            super(EventID.SYSTEM_STATUS_CHANGED, responseCode);
            this.status = status;
        }
    }

    /**
     * Defines the data returned in a Volume Changed notification.
     *
     */
    public static class VolumeChangeNotificationData extends RemoteControlNotificationData {
        /**
         * The new absolute volume.
         * <p>
         * Note: This value shall be between 0.0-1.0 indicating the volume
         * percentage of maximum.
         */
        public float absoluteVolume;

        /**
         * Constructs the data returned in a Volume Changed notification.
         * 
         * @param responseCode
         *            The response code associated with this notification.
         *
         * @param absoluteVolume
         *            The new absolute volume.
         *            <p>
         *            Note: This value shall be between 0.0-1.0 indicating the
         *            volume percentage of maximum.
         */
        public VolumeChangeNotificationData(RemoteControlResponseCode responseCode, float absoluteVolume) {
            super(EventID.VOLUME_CHANGED, responseCode);
            this.absoluteVolume = absoluteVolume;
        }
    }

    /**
     * Defines a Register Notification response.
     * <p>
     *
     * Note: Applications should use this command to respond to a
     * {@link RegisterNotificationCommand} with a response code value of
     * {@link RemoteControlResponseCode#INTERIM} to acknowledge the request.
     * Then, a subsequent call to send notification data should use the code
     * {@link RemoteControlResponseCode#CHANGED}. After a Controller receives
     * this notification, it must re-register with the Target if it wants to be
     * notified again
     *
     */
    public static class RegisterNotificationResponse extends RemoteControlResponse {
        /**
         * The event data associated with the given event type.
         */
        public RemoteControlNotificationData eventData;

        /**
         * Constructs a Defines a Register Notification response.
         *
         * @param responseCode
         *            The response code.
         * @param notificationData
         *            The event data associated with the given event type.
         */
        public RegisterNotificationResponse(RemoteControlResponseCode responseCode, RemoteControlNotificationData notificationData) {
            super(REMOTE_CONTROL_RESPONSE_TYPE_NOTIFICATION, responseCode, RemoteControlSubunitType.PANEL, RemoteControlSubunitID.INSTANCE_0);

            this.eventData = notificationData;
        }
    }

    /**
     * Defines a Set Absolute Volume remote control response.
     *
     */
    public static class SetAbsoluteVolumeResponse extends RemoteControlResponse {
        /**
         * The absolute volume actually set by the target.
         * <p>
         * Note: This value shall be between 0.0-1.0 indicating the volume
         * percentage of maximum.
         */
        public float absoluteVolume;

        /**
         * Constructs a Set Absolute Volume remote control response.
         *
         * @param response
         *            The response code.
         * @param absoluteVolume
         *            The absolute volume.
         */
        public SetAbsoluteVolumeResponse(RemoteControlResponseCode response, float absoluteVolume) {
            super(REMOTE_CONTROL_RESPONSE_TYPE_SET_ABSOLUTE_VOLUME, response, RemoteControlSubunitType.PANEL, RemoteControlSubunitID.INSTANCE_0);

            this.absoluteVolume = absoluteVolume;
        }
    }

    //    public static class UnitInfoResponse extends RemoteControlResponse {
    //        /**
    //         * The Unit Type of the Unit Info Command.
    //         */
    //        public RemoteControlSubunitType unitType;
    //
    //        /**
    //         * The Unit value of the Unit Info Command.
    //         */
    //        public int unit;
    //
    //        /**
    //         * The Company ID assigned.
    //         */
    //        public int companyID;
    //
    //        public UnitInfoResponse(RemoteControlResponseCode responseCode, RemoteControlSubunitType unitType, int unit, int companyID) {
    //            super(REMOTE_CONTROL_RESPONSE_TYPE_UNIT_INFO, responseCode, RemoteControlSubunitType.UNIT, RemoteControlSubunitID.IGNORE);
    //
    //            this.unitType = unitType;
    //            this.unit     = unit;
    //            this.companyID = companyID;
    //        }
    //    }

    /* Native method declarations */

    private native static void initClassNative();

    private native void initObjectNative(int serverType, boolean dataCallbacks, boolean remoteControlController, boolean remoteControlTarget) throws ServerNotReachableException;

    private native void cleanupObjectNative();

    protected native int connectionRequestResponseNative(int requestType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean accept);

    protected native int connectAudioStreamNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int streamType, int streamFlags, boolean waitForConnection);

    protected native int disconnectAudioStreamNative(int streamType);

    protected native int queryAudioStreamConnectedNative(int streamType, byte addr[]);

    protected native int queryAudioStreamStateNative(int streamType, int streamState[]);

    protected native int queryAudioStreamFormatNative(int streamType, long sampleFrequency[], int numberChannels[]);

    protected native int changeAudioStreamStateNative(int streamType, int streamState);

    protected native int changeAudioStreamFormatNative(int streamType, long sampleFrequency, int numberChannels);

    protected native int queryAudioStreamConfigurationNative(int streamType, int mtuChannelsCodecType[], long sampleFrequency[], byte mediaCodecInformation[]);

    protected native int changeIncomingConnectionFlagsNative(int connectionFlags);

    protected native int sendEncodedAudioDataNative(byte rawAudioDataFrame[]);

    protected native int sendRemoteControlPassThroughCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, long responseTimeout, int commandType, int subunitType, int subunitID, int operationID, boolean stateFlag, byte operationData[]);

    protected native int sendRemoteControlPassThroughResponseNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int responseCode, int subunitType, int subunitID, int operationID, boolean stateFlag, byte operationData[]);

    protected native int connectRemoteControlNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int flags, boolean waitForConnection);

    protected native int disconnectRemoteConrolNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    protected native int queryConnectedRemoteControlDevicesNative(BluetoothAddress[][] addresses);

    protected native int vendorDependentCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, long responseTimeout, int commandType, int subunitType, int subunitID, byte id0, byte id1, byte id2, byte operationData[]);

    protected native int getCapabilitiesCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, long responseTimeout, int capabilityID);

    protected native int getElementAttributesCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, long responseTimeout, long indentifier, int numberAttributes, int attributeMask);

    protected native int getPlayStatusCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, long responseTimeout);

    protected native int registerNotificationCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, long responseTimeout, int eventID, int playbackInterval);

    protected native int setAbsoluteVolumeCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, long responseTimeout, byte absoluteVolume);

    protected native int vendorDependentResponseNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int responseCode, int subunitType, int subunitID, byte id0, byte id1, byte id2, byte operationData[]);

    protected native int getCompanyIDCapabilitiesResponseNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int responseCode, byte[] ids);

    protected native int getEventsSupportedCapabilitiesResponseNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int responseCode, int eventIDs);

    protected native int getElementAttributesResponseNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int responseCode, int[] eventIDs, int[] characterSets, byte[][] elementData);

    protected native int getPlayStatusResponseNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int responseCode, int length, int position, int status);

    protected native int playbackStatusChangedNotificationNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int responseCode, int status);

    protected native int trackChangedNotificationNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int responseCode, long identifier);

    protected native int trackReachedEndNotificationNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int responseCode);

    protected native int trackReachedStartNotificationNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int responseCode);

    protected native int playbackPositionChangedNotificationNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int responseCode, int position);

    protected native int systemStatusChangedNotificationNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int responseCode, int status);

    protected native int volumeChangeNotificationNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int responseCode, byte volume);

    protected native int setAbsoluteVolumeResponseNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte transactionID, int responseCode, byte volume);

}
