package com.stonestreetone.bluetopiapm;

import java.util.EnumSet;

/**
 * Java wrapper for Headset Profile Manager API for Stonestreet One Bluetooth
 * Protocol Stack Platform Manager.
 */
public class HDSM {

    protected final EventCallback callbackHandler;

    private boolean               disposed;

    private long                  localData;

    static {
        System.loadLibrary("btpmj");
        initClassNative();
    }

    protected final int           serverType;
    private final boolean         controller;

    private final static int      SERVER_TYPE_HEADSET         = 1;
    private final static int      SERVER_TYPE_AUDIOGATEWAY    = 2;

    /**
     * Minimum allowed value for speaker gain volume.
     *
     * @see #setRemoteSpeakerGain
     * @see EventCallback#speakerGainIndicationEvent
     */
    public final static int       SPEAKER_GAIN_MINIMUM        = 0;

    /**
     * Maximum allowed value for speaker gain volume.
     *
     * @see #setRemoteSpeakerGain
     * @see EventCallback#speakerGainIndicationEvent
     */
    public final static int       SPEAKER_GAIN_MAXIMUM        = 15;

    /**
     * Minimum allowed value for microphone gain volume.
     *
     * @see #setRemoteMicrophoneGain
     * @see EventCallback#microphoneGainIndicationEvent
     */
    public final static int       MICROPHONE_GAIN_MINIMUM     = 0;

    /**
     * Maximum allowed value for microphone gain volume.
     *
     * @see #setRemoteMicrophoneGain
     * @see EventCallback#microphoneGainIndicationEvent
     */
    public final static int       MICROPHONE_GAIN_MAXIMUM     = 15;

    /**
     * Private constructor.
     *
     * @throws ServerNotReachableException
     */
    // FIXME needs exception if Controller claim fails.
    protected HDSM(int type, boolean controller, EventCallback eventCallback) throws ServerNotReachableException {

        try {

            initObjectNative(type, controller);
            disposed        = false;
            callbackHandler = eventCallback;
            this.serverType = type;
            this.controller = controller;

        } catch(ServerNotReachableException exception) {

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
     * Determines whether this Manager object is the registered controller for
     * associated service.
     *
     * @return {@code true} if this Manager is the registered controller for the
     *         service.
     */
    public boolean isController() {
        return controller;
    }

    /**
     * Responds to a request from a remote device to connect to a Local Server.
     *
     * A successful return value does not necessarily indicate that the port has
     * been successfully opened. A {@link EventCallback#connectedEvent} call
     * will notify of this status.
     *
     * @param remoteDeviceAddress Bluetooth address of the remote device
     *            attempting to connect.
     * @param acceptConnection {@code true} if the connection should be
     *            accepted.
     * @return Zero if successful, or a negative return error code if there was
     *         an error.
     */
    public int connectionRequestResponse(BluetoothAddress remoteDeviceAddress, boolean acceptConnection) {
        byte[] address = remoteDeviceAddress.internalByteArray();
        return connectionRequestResponseNative(serverType, address[0], address[1], address[2], address[3], address[4], address[5], acceptConnection);
    }

    /**
     * Connect to a remote Headset/Audio Gateway device.
     * <p>
     * If {@code waitForConnection} is {@code true}, this call will block until
     * the connection status is received (that is, the connection is completed).
     * If this parameter is {@code false}, then the connection status
     * will be returned asynchronously by way of a call to
     * {@link EventCallback#connectionStatusEvent} on the {@link EventCallback}
     * provided when this Manager was constructed.
     *
     * @param remoteDeviceAddress Bluetooth address of the remote device.
     * @param remoteServerPort Port number of the Hands Free Profile on the
     *            remote device.
     * @param connectionFlags Bit flags which control whether encryption and/or
     *            authentication is used.
     * @param waitForConnection If true, this call will block until the
     *            connection attempt finishes.
     * @return Zero if successful, or a negative return error code if there was
     *         an error issuing the connection attempt. If
     *         {@code waitForConnection} is {@code true} and the connection
     *         attempt was successfully issued, a positive return value
     *         indicates the reason why the connection failed.
     */
    public int connectRemoteDevice(BluetoothAddress remoteDeviceAddress, int remoteServerPort, EnumSet<ConnectionFlags> connectionFlags, boolean waitForConnection) {
        int flags;
        byte[] address = remoteDeviceAddress.internalByteArray();

        flags  = 0;
        flags |= (connectionFlags.contains(ConnectionFlags.REQUIRE_AUTHENTICATION) ? CONNECTION_FLAGS_REQUIRE_AUTHENTICATION : 0);
        flags |= (connectionFlags.contains(ConnectionFlags.REQUIRE_ENCRYPTION) ? CONNECTION_FLAGS_REQUIRE_ENCRYPTION : 0);

        return connectRemoteDeviceNative(serverType, address[0], address[1], address[2], address[3], address[4], address[5], remoteServerPort, flags, waitForConnection);
    }

    /**
     * Gets a list of currently connected remote Bluetooth devices.
     *
     * @return List of Bluetooth addresses of the currently connected remote
     *         devices. An empty list indicates no connected devices. A
     *         {@code null} value indicates a problem occurred while querying
     *         the list of connected devices.
     */
    public BluetoothAddress[] queryConnectedDevices() {
        return queryConnectedDevicesNative(serverType);
    }

    /**
     * Closes an active connection to a remote device.
     *
     * Connections may be opened by any of the following mechanisms:
     * <ul>
     * <li>Successful call to {@link #connectRemoteDevice}.</li>
     * <li>Incoming open request (
     * {@link EventCallback#incomingConnectionRequestEvent}) which was accepted
     * either automatically or by a call to {@link #connectionRequestResponse}.</li>
     * </ul>
     *
     * @param remoteDeviceAddress Bluetooth address of the device to disconnect.
     * @return Zero if successful, or a negative value if there was an error.
     */
    public int disconnectDevice(BluetoothAddress remoteDeviceAddress) {
        byte[] address = remoteDeviceAddress.internalByteArray();
        return disconnectDeviceNative(serverType, address[0], address[1], address[2], address[3], address[4], address[5]);
    }

    /**
     * Changes the connection flags for incoming connections from remote
     * devices.
     *
     * @param incomingConnectionFlags New connection flags to be used for all
     *            incoming connection requests.
     * @return Zero if successful, or a negative return error code if there was
     *         an error.
     */
    public int changeIncomingConnectionFlags(EnumSet<IncomingConnectionFlags> incomingConnectionFlags) {
        int flags;

        flags  = 0;
        flags |= (incomingConnectionFlags.contains(IncomingConnectionFlags.REQUIRE_AUTHORIZATION)  ? INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION  : 0);
        flags |= (incomingConnectionFlags.contains(IncomingConnectionFlags.REQUIRE_AUTHENTICATION) ? INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION : 0);
        flags |= (incomingConnectionFlags.contains(IncomingConnectionFlags.REQUIRE_ENCRYPTION)     ? INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION     : 0);

        return changeIncomingConnectionFlagsNative(serverType, flags);
    }

    /**
     * Sets the speaker gain for an established connection.
     * <p>
     * When called by a Headset device this function is provided as a means
     * to inform the remote Audio Gateway of the current speaker gain value.
     * When called by an Audio Gateway this function provides a means for the
     * Audio Gateway to control the speaker gain of the remote Headset
     * device.
     * <p>
     * This method may only be called if a valid service level connection exists
     * and the Manager object is the Controller of the server.
     *
     * @param remoteDeviceAddress Bluetooth address of the remote device.
     * @param speakerGain New speaker gain value. Must be between
     *            {@link #SPEAKER_GAIN_MINIMUM} and
     *            {@link #SPEAKER_GAIN_MAXIMUM}.
     * @return Zero if successful or a negative return error code if there was
     *         an error.
     */
    public int setRemoteSpeakerGain(BluetoothAddress remoteDeviceAddress, int speakerGain) {
        byte[] address = remoteDeviceAddress.internalByteArray();
        return setRemoteSpeakerGainNative(serverType, address[0], address[1], address[2], address[3], address[4], address[5], speakerGain);
    }

    /**
     * Sets the remote devices microphone gain.
     * <p>
     * When called by a Headset device this function is provided as a means
     * to inform the remote Audio Gateway of the current microphone gain value.
     * When called by an Audio Gateway this function provides a means for the
     * Audio Gateway to control the microphone gain of the remote Headset
     * device.
     * <p>
     * This method may only be called if a valid service level connection exists
     * and this Manager object is the Controller of the local server.
     *
     * @param remoteDeviceAddress Bluetooth address of the remote device.
     * @param microphoneGain New microphone gain value. Must be between
     *            {@code MICROPHONE_GAIN_MINIMUM} and
     *            {@code MICROPHONE_GAIN_MAXIMUM}.
     * @return Zero if successful or a negative return error code if there was
     *         an error.
     */
    public int setRemoteMicrophoneGain(BluetoothAddress remoteDeviceAddress, int microphoneGain) {
        byte[] address = remoteDeviceAddress.internalByteArray();
        return setRemoteMicrophoneGainNative(serverType, address[0], address[1], address[2], address[3], address[4], address[5], microphoneGain);
    }

    /**
     * Registers this Manager object as the SCO audio data provider for the
     * local server.
     * <p>
     * There can only be a single data event handler registered for each type of
     * Hands Free Manager connection type. Ownership of the data provider role
     * can be released by calling {@link #releaseSCODataProvider}.
     *
     * @return This function returns Zero if successful, or a negative return
     *         error code if there was an error.
     */
    public int registerSCODataProvider() {
        int result;

        result = registerDataEventCallbackNative(serverType);

        if(result > 0)
            result = 0;
        else if(result == 0)
            result = -1;

        return result;
    }

    /**
     * Releases this Manager object from being the registered SCO data provider
     * for the local server.
     */
    public void releaseSCODataProvider() {
            unregisterDataEventCallbackNative();
    }

    /**
     * Sends SCO audio data to a remote device.
     * <p>
     * This method can only be called once an audio connection has been
     * established and only on a Manager object which is successfully registered
     * as the SCO audio data provider for the local server.
     * <p>
     * This function is only applicable for Bluetooth devices that are
     * configured to support packetized SCO audio. This function will have no
     * effect on Bluetooth devices that are configured to process SCO audio by
     * way of a hardware codec. The data that is sent *MUST* be formatted in the
     * correct SCO format that is expected by the device. It is assumed the
     * audio data is being sent at real time pacing, and the data is queued to
     * be sent immediately.
     *
     * @param remoteDeviceAddress Bluetooth address of the remote device.
     * @param audioData Raw PCM audio data.
     * @return This function returns zero if successful or a negative return
     *         error code if there was an error.
     */
    public int sendAudioData(BluetoothAddress remoteDeviceAddress, byte[] audioData) {
        byte[] address = remoteDeviceAddress.internalByteArray();
        return sendAudioDataNative(serverType, address[0], address[1], address[2], address[3], address[4], address[5], audioData);
    }


    /* Event Callback handlers */

    /* The following structure is a container structure that holds the */
    /* information that is returned in a aetIncomingConnectionRequest */
    /* event. */
    private void incomingConnectionRequestEvent(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        assert (this.serverType == serverType) : "Event delivered for incorrect connection type";

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.incomingConnectionRequestEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* The following event is dispatched when a Headset connection */
    /* occurs. The ConnectionType member specifies which local Hands */
    /* Free role type has been connected to and the RemoteDeviceAddress */
    /* member specifies the remote Bluetooth device that has connected to */
    /* the specified Headset Role. */
    private void connectedEvent(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        assert (this.serverType == serverType) : "Event delivered for incorrect connection type";

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.connectedEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* The following event is dispatched when a remote device disconnects */
    /* from the local device (for the specified Headset role). The */
    /* ConnectionType member identifies the local Headset role type */
    /* being disconnected and the RemoteDeviceAddress member specifies */
    /* the Bluetooth device address of the device that disconnected from */
    /* the profile. */
    private void disconnectedEvent(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int reason) {
        assert (this.serverType == serverType) : "Event delivered for incorrect connection type";

        DisconnectedReason disconnectReason;
        EventCallback      callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            switch(reason) {
            case DISCONNECTION_STATUS_SUCCESS:
                disconnectReason = DisconnectedReason.SUCCESS;
                break;
            case DISCONNECTION_STATUS_SERVICE_LEVEL_CONNECTION_ERROR:
            default:
                disconnectReason = DisconnectedReason.SUCCESS;
                break;
            }
            try {
                callbackHandler.disconnectedEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), disconnectReason);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* The following event is dispatched when a client receives the */
    /* connection response from a remote server which was previously */
    /* attempted to be connected to. The ConnectionType member specifies */
    /* the local client that has requested the connection, the */
    /* RemoteDeviceAddress member specifies the remote device that was */
    /* attempted to be connected to, and the ConnectionStatus member */
    /* represents the connection status of the request. */
    private void connectionStatusEvent(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int connectionStatus) {
        assert (this.serverType == serverType) : "Event delivered for incorrect connection type";

        ConnectionStatus connectionStatusEnum;
        EventCallback    callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            switch(connectionStatus) {
            case CONNECTION_STATUS_SUCCESS:
                connectionStatusEnum = ConnectionStatus.SUCCESS;
                break;
            case CONNECTION_STATUS_FAILURE_TIMEOUT:
                connectionStatusEnum = ConnectionStatus.FAILURE_TIMEOUT;
                break;
            case CONNECTION_STATUS_FAILURE_REFUSED:
                connectionStatusEnum = ConnectionStatus.FAILURE_REFUSED;
                break;
            case CONNECTION_STATUS_FAILURE_SECURITY:
                connectionStatusEnum = ConnectionStatus.FAILURE_SECURITY;
                break;
            case CONNECTION_STATUS_FAILURE_DEVICE_POWER_OFF:
                connectionStatusEnum = ConnectionStatus.FAILURE_DEVICE_POWER_OFF;
                break;
            case CONNECTION_STATUS_FAILURE_UNKNOWN:
            default:
                connectionStatusEnum = ConnectionStatus.FAILURE_UNKNOWN;
                break;
            }

            try {
                callbackHandler.connectionStatusEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), connectionStatusEnum);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* The following event is dispatched to the local device when an */
    /* audio connection is established. The ConnectionType member */
    /* identifies the connection that is receiving this indication. The */
    /* RemoteDeviceAddress member specifies the Bluetooth device address */
    /* of the remote device that the audio connection is established */
    /* with. */
    private void audioConnectedEvent(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        assert (this.serverType == serverType) : "Event delivered for incorrect connection type";

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.audioConnectedEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* The following event is dispatched to the local device when an */
    /* audio connection is disconnected. The ConnectionType member */
    /* identifies the connection that is receiving this indication. The */
    /* RemoteDeviceAddress member specifies the Bluetooth device address */
    /* of the remote device that the audio connection is no longer */
    /* established with. */
    private void audioDisconnectedEvent(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        assert (this.serverType == serverType) : "Event delivered for incorrect connection type";

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.audioDisconnectedEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* Default handler for audioConnectionStatus event. */
    protected void audioConnectionStatusEvent(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean successful) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to the local device upon the */
    /* reception of SCO audio data. The ConnectionType member identifies */
    /* the connection that has received the audio data. The */
    /* RemoteDeviceAddress member specifies the Bluetooth device address */
    /* of the Bluetooth device that the specified audio data was received */
    /* from. The AudioDataLength member represents the size of the audio */
    /* data pointed to the buffer that is specified by the AudioData */
    /* member. */
    private void audioDataEvent(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte[] audioData) {
        assert (this.serverType == serverType) : "Event delivered for incorrect connection type";

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.audioDataEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), audioData);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* The following event may be dispatched to either a local Headset */
    /* unit or a local Audio Gateway. When this event is received by a */
    /* local Headset unit it is to set the local speaker gain. When */
    /* this event is received by a local audio gateway it is used in */
    /* volume level synchronization to inform the local Audio Gateway of */
    /* the current speaker gain on the remote Headset unit. The */
    /* ConnectionType member identifies the local connection that has */
    /* received this indication. The RemoteDeviceAddress member */
    /* specifies the remote Bluetooth device address of the remote */
    /* Bluetooth device that is informing the local device of the new */
    /* speaker gain. The SpeakerGain member is used to set or inform the */
    /* device of the speaker gain. */
    private void speakerGainIndicationEvent(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int speakerGain) {
        assert (this.serverType == serverType) : "Event delivered for incorrect connection type";

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.speakerGainIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), speakerGain);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* The following event may be dispatched to either a local Headset */
    /* unit or a local Audio Gateway. When this event is received by a */
    /* local Headset unit it is to set the local microphone gain. */
    /* When this event is received by a local audio gateway it is used in */
    /* volume level synchronization to inform the local Audio Gateway of */
    /* the current microphone gain on the remote Headset unit. The */
    /* ConnectionType member identifies the local connection that has */
    /* received this indication. The RemoteDeviceAddress member */
    /* specifies the remote Bluetooth device address of the remote */
    /* Bluetooth device that is informing the local device of the new */
    /* microphone gain. The MicrophoneGain member is used to set or */
    /* inform the device of the microphone gain. */
    private void microphoneGainIndicationEvent(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int microphoneGain) {
        assert (this.serverType == serverType) : "Event delivered for incorrect connection type";

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.microphoneGainIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), microphoneGain);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* Default handler for ringIndication event. */
    protected void ringIndicationEvent(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* Default handler for buttonPressIndication event. */
    protected void buttonPressedIndicationEvent(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    protected interface EventCallback {
        /**
         * Invoked when a remote device attempts to connect to the local server
         * and authorization is required from the controlling Manager.
         * <p>
         * Respond to this event by calling
         * {@link HDSM#connectionRequestResponse}.
         *
         * @param remoteDeviceAddress Bluetooth address of the device requesting
         *            a connection.
         */
        public void incomingConnectionRequestEvent(BluetoothAddress remoteDeviceAddress);

        /**
         * Invoked when a connection is established between the local server and
         * a remote device.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote device.
         */
        public void connectedEvent(BluetoothAddress remoteDeviceAddress);

        /**
         * Invoked when an established connection is disconnected.
         *
         * @param remoteDeviceAddress Bluetooth address of the disconnected
         *            device.
         * @param reason Reason for the disconnection.
         *
         * @see HDSM#disconnectDevice
         */
        public void disconnectedEvent(BluetoothAddress remoteDeviceAddress, DisconnectedReason reason);

        /**
         * Invoked when an outgoing connection attempt completes.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote device.
         * @param connectionStatus Indication of success or the reason for
         *            failure.
         *
         * @see HDSM#connectRemoteDevice
         */
        public void connectionStatusEvent(BluetoothAddress remoteDeviceAddress, ConnectionStatus connectionStatus);

        /**
         * Invoked when a SCO audio connection is successfully established.
         * <p>
         * This event is only sent to non-controller Managers which did not
         * initiate the SCO audio connection.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote device.
         *
         * @see HDSM#setupSCOAudioConnection
         * @see #audioConnectionStatusEvent
         */
        public void audioConnectedEvent(BluetoothAddress remoteDeviceAddress);

        /**
         * Invoked when a SCO audio connection is disconnected.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote device.
         *
         * @see HDSM#releaseSCOAudioConnection
         */
        public void audioDisconnectedEvent(BluetoothAddress remoteDeviceAddress);

        /**
         * Invoked when SCO audio data is received from the remote device.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote device.
         * @param audioData Raw PCM audio data.
         *
         * @see HDSM#sendAudioData
         */
        public void audioDataEvent(BluetoothAddress remoteDeviceAddress, byte[] audioData);

        /**
         * Invoked when the remote device notifies the local server of a change
         * to the current speaker gain.
         * <p>
         * When this event is received by a local Headset Manager, it is used
         * to set the local speaker gain. When this event is received by a local
         * Audio Gateway Manager, it is used in volume level synchronization to
         * inform the local Audio Gateway of the current speaker gain on the
         * remote Hands Free unit.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote device.
         * @param speakerGain The current or new speaker gain value. Will always
         *            be between {@link HDSM#SPEAKER_GAIN_MINIMUM} and
         *            {@link HDSM#SPEAKER_GAIN_MAXIMUM}, inclusive.
         *
         * @see HDSM#setRemoteSpeakerGain
         */
        public void speakerGainIndicationEvent(BluetoothAddress remoteDeviceAddress, int speakerGain);

        /**
         * Invoked when the remote device notifies the local server of a change
         * to the current microphone gain.
         * <p>
         * When this event is received by a local Headset Manager, it is used
         * to set the local microphone gain. When this event is received by a
         * local Audio Gateway Manager, it is used in volume level
         * synchronization to inform the local Audio Gateway of the current
         * microphone gain on the remote Hands Free unit.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote device.
         * @param microphoneGain The current or new microphone gain value. Will
         *            always be between {@link HDSM#SPEAKER_GAIN_MINIMUM} and
         *            {@link HDSM#SPEAKER_GAIN_MAXIMUM}, inclusive.
         *
         * @see HDSM#setRemoteMicrophoneGain
         */
        public void microphoneGainIndicationEvent(BluetoothAddress remoteDeviceAddress, int microphoneGain);


    }


    /**
     * Management connection to the local Headset Server.
     */
    public static final class HeadsetServerManager extends HDSM {

        /**
         * Create a new connection to the local Headset Server.
         * <p>
         * Note that this is distinct from the Headset Profile. This server
         * is specific to the role of a Headset device (such as a wearable
         * headset). This server supports connecting to remote
         * Audio Gateway servers (typically, a mobile phone or other
         * communication endpoint).
         * <p>
         * The local server supports one "Controller" Manager. This Manager is
         * the only Manager permitted to issue controlling commands to the
         * server. All other registered Managers are permitted only to receive
         * events and query the various statuses of the local server.
         *
         * @param controller Claim the role of Controller for the local server.
         * @param eventCallback Receiver for events sent by the local server.
         * @throws ServerNotReachableException if the BluetopiaPM server cannot
         *             be reached.
         */
        public HeadsetServerManager(boolean controller, HeadsetEventCallback eventCallback) throws ServerNotReachableException {
            super(SERVER_TYPE_HEADSET, controller, eventCallback);
        }

        private static final int SUPPORTED_FEATURE_REMOTE_AUDIO_VOLUME_CONTROLS = 0x00000001;

        /**
         * Features which a Headset device can optionally support.
         *
         */
        public enum SupportedFeatures {
            /**
             * Support for controlling volume from a remote device.
             */
            REMOTE_AUDIO_VOLUME_CONTROLS,
        }

        /**
         * Local Headset service configuration.
         *
         * @see #queryLocalConfiguration
         */
        public static final class LocalConfiguration {
            private final EnumSet<IncomingConnectionFlags> incomingConnectionFlags;
            private final EnumSet<SupportedFeatures>       supportedFeatures;

            /*pkg*/ LocalConfiguration(EnumSet<IncomingConnectionFlags> incomingConnectionFlags, EnumSet<SupportedFeatures> supportedFeatures) {
                this.incomingConnectionFlags = incomingConnectionFlags;
                this.supportedFeatures       = supportedFeatures;
            }

            public EnumSet<IncomingConnectionFlags> getIncomingConnectionFlags() {
                return incomingConnectionFlags;
            }

            public EnumSet<SupportedFeatures> getSupportedFeatures() {
                return supportedFeatures;
            }

        }

        /**
         * Retreive the current configuration of the local service.
         *
         * @return The configuration parameters of the local service or
         *         {@code null} if there was an error.
         **/
        public LocalConfiguration queryCurrentConfiguration() {
            int[] flags               = new int[2];
            LocalConfiguration config = null;

            if(queryCurrentConfigurationNative(serverType, flags) == 0) {
                EnumSet<IncomingConnectionFlags> incomingConnectionFlags = EnumSet.noneOf(IncomingConnectionFlags.class);
                EnumSet<SupportedFeatures> supportedFeatures = EnumSet.noneOf(SupportedFeatures.class);

                if((flags[CONFIGURATION_INCOMING_CONNECTION_FLAGS] & INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION) == INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION)
                    incomingConnectionFlags.add(IncomingConnectionFlags.REQUIRE_AUTHORIZATION);
                if((flags[CONFIGURATION_INCOMING_CONNECTION_FLAGS] & INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION) == INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION)
                    incomingConnectionFlags.add(IncomingConnectionFlags.REQUIRE_AUTHENTICATION);
                if((flags[CONFIGURATION_INCOMING_CONNECTION_FLAGS] & INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION) == INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION)
                    incomingConnectionFlags.add(IncomingConnectionFlags.REQUIRE_ENCRYPTION);

                if((flags[CONFIGURATION_SUPPORTED_FEATURES_MASK] & SUPPORTED_FEATURE_REMOTE_AUDIO_VOLUME_CONTROLS) == SUPPORTED_FEATURE_REMOTE_AUDIO_VOLUME_CONTROLS)
                    supportedFeatures.add(SupportedFeatures.REMOTE_AUDIO_VOLUME_CONTROLS);

                config = new LocalConfiguration(incomingConnectionFlags, supportedFeatures);
            }

            return config;
        }

        /**
         * Sends a button press the the remote Audio Gateway.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote device
         * @return Zero if successful, or a negative return error code if there was
         *         an error.
         */
        public int sendButtonPress(BluetoothAddress remoteDeviceAddress) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return sendButtonPressNative(address[0], address[1], address[2], address[3], address[4], address[5]);
        }


        /* Handler for ringIndicationEvent. */
        @Override
        protected void ringIndicationEvent(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((HeadsetEventCallback)callbackHandler).ringIndication(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }



        /**
         * Receiver for event notifications which apply to a Headset server.
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
        public interface HeadsetEventCallback extends EventCallback {
            /**
             * Invoked when the remote Audio Gateway sends a ring indication.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote Audio Gateway
             */
            public void ringIndication(BluetoothAddress remoteDeviceAddress);
        }

    }

    /**
     * Management connection to the local Audio Gateway Server.
     */
    public static final class AudioGatewayServerManager extends HDSM {

        /**
         * Create a new connection to the local Audio Gateway Server.
         * <p>
         * The local server supports one "Controller" Manager. This Manager is
         * the only Manager permitted to issue controlling commands to the
         * server. All other registered Managers are permitted only to receive
         * events and query the various statuses of the local server.
         *
         * @param controller Claim the role of Controller for the local server.
         * @param eventCallback Receiver for events sent by the local server.
         * @throws ServerNotReachableException if the BluetopiaPM server cannot
         *             be reached.
         */
        public AudioGatewayServerManager(boolean controller, AudioGatewayEventCallback eventCallback) throws ServerNotReachableException {
            super(SERVER_TYPE_AUDIOGATEWAY, controller, eventCallback);
        }

        private static final int SUPPORTED_FEATURE_IN_BAND_RING = 0x00000001;

        /**
         * Features which a Headset device can optionally support.
         *
         */
        public enum SupportedFeatures {
            /**
             * Support for in-band ringing.
             */
            IN_BAND_RING,
        }

        /**
         * Local Audio Gateway service configuration.
         *
         * @see #queryLocalConfiguration
         */
        public static final class LocalConfiguration {
            private final EnumSet<IncomingConnectionFlags> incomingConnectionFlags;
            private final EnumSet<SupportedFeatures>       supportedFeatures;

            /*pkg*/ LocalConfiguration(EnumSet<IncomingConnectionFlags> incomingConnectionFlags, EnumSet<SupportedFeatures> supportedFeatures) {
                this.incomingConnectionFlags = incomingConnectionFlags;
                this.supportedFeatures       = supportedFeatures;
            }

            public EnumSet<IncomingConnectionFlags> getIncomingConnectionFlags() {
                return incomingConnectionFlags;
            }

            public EnumSet<SupportedFeatures> getSupportedFeatures() {
                return supportedFeatures;
            }
        }

        /**
         * Retreive the current configuration of the local service.
         *
         * @return The configuration parameters of the local service or
         *         {@code null} if there was an error.
         **/
        public LocalConfiguration queryCurrentConfiguration() {
            int[] flags               = new int[2];
            LocalConfiguration config = null;

            if(queryCurrentConfigurationNative(serverType, flags) == 0) {
                EnumSet<IncomingConnectionFlags> incomingConnectionFlags = EnumSet.noneOf(IncomingConnectionFlags.class);
                EnumSet<SupportedFeatures> supportedFeatures = EnumSet.noneOf(SupportedFeatures.class);

                if((flags[CONFIGURATION_INCOMING_CONNECTION_FLAGS] & INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION) == INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION)
                    incomingConnectionFlags.add(IncomingConnectionFlags.REQUIRE_AUTHORIZATION);
                if((flags[CONFIGURATION_INCOMING_CONNECTION_FLAGS] & INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION) == INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION)
                    incomingConnectionFlags.add(IncomingConnectionFlags.REQUIRE_AUTHENTICATION);
                if((flags[CONFIGURATION_INCOMING_CONNECTION_FLAGS] & INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION) == INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION)
                    incomingConnectionFlags.add(IncomingConnectionFlags.REQUIRE_ENCRYPTION);

                if((flags[CONFIGURATION_SUPPORTED_FEATURES_MASK] & SUPPORTED_FEATURE_IN_BAND_RING) == SUPPORTED_FEATURE_IN_BAND_RING)
                    supportedFeatures.add(SupportedFeatures.IN_BAND_RING);

                config = new LocalConfiguration(incomingConnectionFlags, supportedFeatures);
            }

            return config;
        }

        /**
         * Sends a ring indication to remote Headset device.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote device.
         * @return Zero if successful, or a negative return error code if there was
         *         an error.
         */
        public int ringIndication(BluetoothAddress remoteDeviceAddress) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return ringIndicationNative(address[0], address[1], address[2], address[3], address[4], address[5]);
        }

        /**
         * Establish a SCO audio connection to a remove device.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote device.
         * @param inBandRinging Whether to support in-band ringing for the audio connection
         * @return This function returns zero if successful or a negative return
         *         error code if there was an error.
         */
        public int setupSCOAudioConnection(BluetoothAddress remoteDeviceAddress, boolean inBandRinging) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return setupAudioConnectionNative(serverType, address[0], address[1], address[2], address[3], address[4], address[5], inBandRinging);
        }

        /**
         * Disconnect an existing SCO audio connection to a remote device.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote device.
         * @return This function returns zero if successful or a negative return
         *         error code if there was an error.
         */
        public int releaseSCOAudioConnection(BluetoothAddress remoteDeviceAddress) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return releaseAudioConnectionNative(serverType, address[0], address[1], address[2], address[3], address[4], address[5]);
        }

        /* Handler for buttonPressedIndication event. */
        @Override
        protected void buttonPressedIndicationEvent(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((AudioGatewayEventCallback)callbackHandler).buttonPressedIndication(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched when the originator of the audio */
        /* connection (Audio Gateway only) receives the audio connection */
        /* response from a remote device from which an audio connection */
        /* request was previously sent. The ConnectionType member identifies */
        /* the connection that was attempting the audio connection. The */
        /* RemoteDeviceAddress member specifies the remote device address of */
        /* the remote device that the audio connection was requested. The */
        /* Status member specifies the result of the audio connection event. */
        @Override
        protected void audioConnectionStatusEvent(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean successful) {
            assert (this.serverType == serverType) : "Event delivered for incorrect connection type";

            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((AudioGatewayEventCallback)callbackHandler).audioConnectionStatusEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), successful);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Receiver for event notifications which apply to a Audio Gateway server.
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
        public interface AudioGatewayEventCallback extends EventCallback {
            /**
             * Invoked when a remote Headset sends a button pressed indication.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote Headset.
             */
            public void buttonPressedIndication(BluetoothAddress remoteDeviceAddress);

            /**
             * Invoked when a SCO audio connection attempt completes.
             * <p>
             * This event is only sent to the controlling Manager which initiates
             * the audio connection.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote device.
             * @param successful {@code true} if the connection was established
             *            successfully.
             *
             * @see HDSM#setupSCOAudioConnection
             * @see #audioConnectedEvent
             */
            public void audioConnectionStatusEvent(BluetoothAddress remoteDeviceAddress, boolean successful);
        }

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
     * @see #connectRemoteDevice
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

    /* The following constants are used with the ConnectionStatus member */
    /* of the HDSM_Device_Connection_Status_Message_t message to describe*/
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
     * @see EventCallback#connectionStatusEvent
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


    /* The following constants represent the Port Close Status Values */
    /* that are possible in the Port Close Indication Event. */
    private final static int DISCONNECTION_STATUS_SUCCESS                        = 0;
    private final static int DISCONNECTION_STATUS_SERVICE_LEVEL_CONNECTION_ERROR = 1;

    /**
     * Reason for a remote device being disconnected.
     *
     * @see EventCallback#disconnectedEvent
     */
    public enum DisconnectedReason {
        /**
         * The disconnection was requested explicitly.
         */
        SUCCESS,

        /**
         * An error occurred on the Service Level Connection which required the
         * link to be disconnected.
         */
        SERVICE_LEVEL_CONNECTION_ERROR
    }


    protected static final int CONFIGURATION_INCOMING_CONNECTION_FLAGS = 0;
    protected static final int CONFIGURATION_SUPPORTED_FEATURES_MASK = 1;



    private native static void initClassNative();

    private native void initObjectNative(int serverType, boolean controller) throws ServerNotReachableException;

    private native void cleanupObjectNative();

    private native int connectionRequestResponseNative(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean acceptConnection);

    private native int connectRemoteDeviceNative(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int remoteServerPort, int connectionFlags, boolean waitForConnection);

    private native int disconnectDeviceNative(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    private native BluetoothAddress[] queryConnectedDevicesNative(int serverType);

    protected native int queryCurrentConfigurationNative(int serverType, int[] flags);

    private native int changeIncomingConnectionFlagsNative(int serverType, int connectionFlags);

    private native int setRemoteSpeakerGainNative(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int speakerGain);

    private native int setRemoteMicrophoneGainNative(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int microphoneGain);

    protected native int sendButtonPressNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    protected native int ringIndicationNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    private native int registerDataEventCallbackNative(int serverType);

    private native void unregisterDataEventCallbackNative();

    protected native int setupAudioConnectionNative(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean inBandRinging);

    protected native int releaseAudioConnectionNative(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    private native int sendAudioDataNative(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte[] audioData);
}
