/*
  * HFRM.java Copyright 2010 - 2014 Stonestreet One, LLC. All Rights Reserved.
 */

package com.stonestreetone.bluetopiapm;

import java.util.EnumSet;

import com.stonestreetone.bluetopiapm.HFRM.HandsFreeServerManager.HandsFreeEventCallback;

/**
 * Java wrapper for Hands-Free Profile Manager API for Stonestreet One Bluetooth
 * Protocol Stack Platform Manager.
 */
public abstract class HFRM {

    protected final EventCallback callbackHandler;

    private boolean               disposed;

    private long                  localData;

    static {
        System.loadLibrary("btpmj");
        initClassNative();
    }

    protected final int           serverType;
    private final boolean         controller;

    private final static int      SERVER_TYPE_HANDSFREE       = 1;
    private final static int      SERVER_TYPE_AUDIOGATEWAY    = 2;

    //FIXME Need a note about "Controller" status

    //TODO Class for representing phone numbers?

    //FIXME Don't allow any method to be called after the object is dispose()'d

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

    /* The following constant represents the Minimum and Maximum */
    /* allowable lengths for phone number strings when used with */
    /* functions requiring phone number as a parameter. */
    /**
     * Minimum allowed length for phone numbers represented as strings.
     */
    public final static int       PHONE_NUMBER_LENGTH_MINIMUM = 1;

    /**
     * Maximum allowed length for phone numbers represented as strings.
     */
    public final static int       PHONE_NUMBER_LENGTH_MAXIMUM = 64;

    /**
     * Private constructor.
     *
     * @throws ServerNotReachableException
     */
    // FIXME needs exception if Controller claim fails.
    protected HFRM(int type, boolean controller, EventCallback eventCallback) throws ServerNotReachableException {

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

    /* Hands Free Manager Connection Management Functions. */

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
     * Connect to a remote Hands Free/Audio Gateway device.
     * <p>
     * If {@code waitForConnection} is {@code true}, this call will block until
     * the connection status is received (that is, the connection is completed).
     * If this parameter is not specified (NULL), then the connection status
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

    /* Shared Hands Free/Audio Gateway Functions. */

    /**
     * Disables echo cancellation and noise reduction on the remote device.
     * <p>
     * This method can only be called a valid service level connection exists
     * but no audio connection exists. This object must be a controller for the
     * local service (specified when this object was constructed).
     * <p>
     * It is not possible to enable this feature once it has been disabled
     * because the Hands-Free Profile specification provides no means to
     * re-enable this feature. This feature will remained disabled until the
     * current service level connection has been dropped.
     *
     * @param remoteDeviceAddress Bluetooth address of the remote device.
     * @return Zero if successful or a negative return error code if there was
     *         an error.
     */
    public int disableRemoteEchoCancellationNoiseReduction(BluetoothAddress remoteDeviceAddress) {
        byte[] address = remoteDeviceAddress.internalByteArray();
        return disableRemoteEchoCancellationNoiseReductionNative(serverType, address[0], address[1], address[2], address[3], address[4], address[5]);
    }

    /**
     * Changes the voice recognition state of a connection.
     * <p>
     * When called by a Hands Free device, this method is responsible for
     * requesting activation or deactivation of the voice recognition which
     * resides on the remote Audio Gateway. When called by an Audio Gateway,
     * this method is responsible for informing the remote Hands Free device of
     * the current activation state of the local voice recognition function.
     * <p>
     * This method may only be called for local devices that were opened with
     * support for voice recognition. This Manager object must have been
     * instantiated as a Controller.
     *
     * @param remoteDeviceAddress Bluetooth address of the remote device.
     * @param voiceRecognitionActive Enable ({@code true}) or disable (
     *            {@code false}) voice recognition.
     * @return Zero if successful or a negative return error code if there was
     *         an error.
     */
    public int setRemoteVoiceRecognitionActivation(BluetoothAddress remoteDeviceAddress, boolean voiceRecognitionActive) {
        byte[] address = remoteDeviceAddress.internalByteArray();
        return setRemoteVoiceRecognitionActivationNative(serverType, address[0], address[1], address[2], address[3], address[4], address[5], voiceRecognitionActive);
    }

    /**
     * Sets the speaker gain for an established connection.
     * <p>
     * When called by a Hands Free device this function is provided as a means
     * to inform the remote Audio Gateway of the current speaker gain value.
     * When called by an Audio Gateway this function provides a means for the
     * Audio Gateway to control the speaker gain of the remote Hands Free
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
     * When called by a Hands Free device this function is provided as a means
     * to inform the remote Audio Gateway of the current microphone gain value.
     * When called by an Audio Gateway this function provides a means for the
     * Audio Gateway to control the microphone gain of the remote Hands Free
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
     * Establish a SCO audio connection to a remove device.
     *
     * @param remoteDeviceAddress Bluetooth address of the remote device.
     * @return This function returns zero if successful or a negative return
     *         error code if there was an error.
     */
    public int setupSCOAudioConnection(BluetoothAddress remoteDeviceAddress) {
        byte[] address = remoteDeviceAddress.internalByteArray();
        return setupAudioConnectionNative(serverType, address[0], address[1], address[2], address[3], address[4], address[5]);
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

    /* The following event is dispatched when a Hands Free connection */
    /* occurs. The ConnectionType member specifies which local Hands */
    /* Free role type has been connected to and the RemoteDeviceAddress */
    /* member specifies the remote Bluetooth device that has connected to */
    /* the specified Hands Free Role. */
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
    /* from the local device (for the specified Hands Free role). The */
    /* ConnectionType member identifies the local Hands Free role type */
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

    /* The following event acts as an indication to inform the current */
    /* state of the service level connection. The ConnectionType member */
    /* identifies the connection type to which this event applies. The */
    /* RemoteSupportedFeaturesValid member specifies whether or not the */
    /* Remote Support Features member is valid. The */
    /* RemoteSupportedFeatures member specifies supported features which */
    /* are supported by the remote device. The */
    /* RemoteCallHoldMultipartySupported member specifies the support of */
    /* this feature by the remote device. This indication must be */
    /* received before performing any other action on this port. If an */
    /* error occurs during the establishment of the service level */
    /* connection the connection will be closed and local device will */
    /* receive a disconnection event. */
    /* * NOTE * The RemoteCallHoldMultipartySupport member will only be */
    /* valid if the local and remote device both have the */
    /* "Three-way Calling Support" bit set in their supported */
    /* features. */
    /* * NOTE * The RemoteCall Hold Multipary Support member will always */
    /* be set to */
    /* HFRE_CALL_HOLD_MULTIPARTY_SUPPORTED_FEATURES_ERROR in the */
    /* case when this indication is received by an audio gateway */
    /* as Hands Free units have no call hold multiparty */
    /* supported features to query. */
    protected void serviceLevelConnectionEstablishedEvent(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean remoteSupportedFeaturesValid, int remoteSupportedFeatures, int remoteCallHoldMultipartySupport) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
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

    /* The following event is dispatched when the originator of the audio */
    /* connection (Audio Gateway only) receives the audio connection */
    /* response from a remote device from which an audio connection */
    /* request was previously sent. The ConnectionType member identifies */
    /* the connection that was attempting the audio connection. The */
    /* RemoteDeviceAddress member specifies the remote device address of */
    /* the remote device that the audio connection was requested. The */
    /* Status member specifies the result of the audio connection event. */
    private void audioConnectionStatusEvent(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean successful) {
        assert (this.serverType == serverType) : "Event delivered for incorrect connection type";

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.audioConnectionStatusEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), successful);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
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

    /* The following event may be dispatched to either a local Hands Free */
    /* unit or a local Audio Gateway. When this event is received by a */
    /* local Hands Free unit it is to inform the local Hands Free unit of */
    /* the remote Audio Gateways current voice recognition activation */
    /* state. When this event is received by a local Audio Gateway it is */
    /* responsible for activating or deactivating the voice recognition */
    /* functions which reside locally. The ConnectionType member */
    /* identifies the connection receiving this indication. On a Hands */
    /* Free unit the VoiceRecognitionActive member is used to inform the */
    /* local device of the remote Audio Gateway device's voice */
    /* recognition activation state. On an Audio Gateway the */
    /* VoiceRecognitionActive member indicates whether to activate or */
    /* deactivate the local voice recognition functions. */
    private void voiceRecognitionIndicationEvent(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean voiceRecognitionActive) {
        assert (this.serverType == serverType) : "Event delivered for incorrect connection type";

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.voiceRecognitionIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), voiceRecognitionActive);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* The following event may be dispatched to either a local Hands Free */
    /* unit or a local Audio Gateway. When this event is received by a */
    /* local Hands Free unit it is to set the local speaker gain. When */
    /* this event is received by a local audio gateway it is used in */
    /* volume level synchronization to inform the local Audio Gateway of */
    /* the current speaker gain on the remote Hands Free unit. The */
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

    /* The following event may be dispatched to either a local Hands Free */
    /* unit or a local Audio Gateway. When this event is received by a */
    /* local Hands Free unit it is to set the local microphone gain. */
    /* When this event is received by a local audio gateway it is used in */
    /* volume level synchronization to inform the local Audio Gateway of */
    /* the current microphone gain on the remote Hands Free unit. The */
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

    /* The following event may be dispatched to either a local Hands Free */
    /* unit or a local Audio Gateway. The local Audio Gateway service */
    /* will receive this event when a remote Hands Free device sends a */
    /* command to set the current call state. The local Hands Free */
    /* device will receive this event when the remote Audio Gateway sends */
    /* a notification on a change in the current Response/Hold Status. */
    /* The ConnectionType member identifies the local connection that is */
    /* receiving the indication. The RemoteDeviceAddress member */
    /* specifies the remote Bluetooth device address of the remote */
    /* Bluetooth device of the connection. The CallState member contains */
    /* the call state requested by the remote device. */
    private void incomingCallStateIndicationEvent(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int callState) {
        assert (this.serverType == serverType) : "Event delivered for incorrect connection type";

        CallState     state;
        EventCallback callbackHandler = this.callbackHandler;

        switch(callState) {
        case CALL_STATE_HOLD:
            state = CallState.HOLD;
            break;
        case CALL_STATE_ACCEPT:
            state = CallState.ACCEPT;
            break;
        case CALL_STATE_REJECT:
            state = CallState.REJECT;
            break;
        case CALL_STATE_NONE:
        default:
            state = CallState.NONE;
            break;
        }

        if(callbackHandler != null) {
            try {
                callbackHandler.incomingCallStateIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), state);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* Hands Free specific events. */

    /* This event is dispatched to a local Hands Free service when a */
    /* remote Audio Gateway device responds to a request for the current */
    /* Response/Hold status. The CallState member contains the call */
    /* state returned by the remote device. The ConnectionType member */
    /* identifies the local connection that is receiving the event. The */
    /* RemoteDeviceAddress member specifies the remote Bluetooth device */
    /* address of the remote Bluetooth device of the connection. The */
    /* CallState member contains the call state of the remote device. */
    protected void incomingCallStateConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int callState) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Hands Free service */
    /* when a control undicator changes on the remote Audio Gateway and */
    /* control indicator change notification is enabled. The */
    /* ConnectionType member identifies the local connection that is */
    /* receiving the event. The RemoteDeviceAddress member specifies the */
    /* remote Bluetooth device address of the remote Bluetooth device of */
    /* the connection. The ControlIndicatorEntry member contains the */
    /* Indicator that has changed. */
    protected void controlIndicatorStatusIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String name, boolean value) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Hands Free service */
    /* when a control undicator changes on the remote Audio Gateway and */
    /* control indicator change notification is enabled. The */
    /* ConnectionType member identifies the local connection that is */
    /* receiving the event. The RemoteDeviceAddress member specifies the */
    /* remote Bluetooth device address of the remote Bluetooth device of */
    /* the connection. The ControlIndicatorEntry member contains the */
    /* Indicator that has changed. */
    protected void controlIndicatorStatusIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String name, int value, int rangeMinimum, int rangeMaximum) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Hands Free service */
    /* in response to an explicit request for the control indicator */
    /* status. The ConnectionType member identifies the local connection */
    /* that is receiving the event. The RemoteDeviceAddress member */
    /* specifies the remote Bluetooth device address of the remote */
    /* Bluetooth device of the connection. The ControlIndicatorEntry */
    /* member contains the Indicator that has changed. */
    protected void controlIndicatorStatusConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String name, boolean value) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Hands Free service */
    /* in response to an explicit request for the control indicator */
    /* status. The ConnectionType member identifies the local connection */
    /* that is receiving the event. The RemoteDeviceAddress member */
    /* specifies the remote Bluetooth device address of the remote */
    /* Bluetooth device of the connection. The ControlIndicatorEntry */
    /* member contains the Indicator that has changed. */
    protected void controlIndicatorStatusConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String name, int value, int rangeMinimum, int rangeMaximum) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Hands Free service */
    /* when the remote Audio Gateway responds to a request for the remote */
    /* call hold and multiparty supported features. The ConnectionType */
    /* and RemoteDeviceAddress members specify the local connection for */
    /* which the event is valid. The CallHoldSupportMaskValid member is */
    /* flag which specifies whether or not the CallHoldSupportMask member */
    /* is valid. */
    /* * NOTE * If the remote Audio Gateway does not have call hold and */
    /* multiparty support, the CallHoldSupportMaskValid member */
    /* will be FALSE. */
    protected void callHoldMultipartySupportConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean callHoldSupportMaskValid, int callHoldSupportMask) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Hands Free service */
    /* when the remote Audio Gateway receives a call while there is an */
    /* on-going call in progress. Note that call waiting notification */
    /* must be active in order to receive this event. The ConnectionType */
    /* and RemoteDeviceAddress members specify the local connection for */
    /* which the event is valid. The PhoneNumber member is a NULL */
    /* terminated ASCII string representing the phone number of the */
    /* waiting call. */
    protected void callWaitingNotificationIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String phoneNumber) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Hands Free service */
    /* when the remote Audio Gateway receives a call line notification. */
    /* Note that call line identification notification must be active in */
    /* order to receive this event. The ConnectionType and */
    /* RemoteDeviceAddress members specify the local connection for which */
    /* the event is valid. The PhoneNumber member is a NULL terminated */
    /* ASCII string representing the phone number of the incoming call. */
    protected void callLineIdentificationNotificationIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String phoneNumber) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Hands Free unit when */
    /* the remote Audio Gateway sends a RING indication to the local */
    /* device. The ConnectionType and RemoteDeviceAddress members */
    /* specify the local connection which is receiving this indication. */
    protected void ringIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Hands Free service */
    /* when the remote Audio Gateway wants to change the in-band ring */
    /* tone setting during an ongoing service level connection. The */
    /* ConnectionType and RemoteDeviceAddress members specify the local */
    /* connection which is receiving this indication. The Enabled member */
    /* specifies whether this is an indication that in-band ringing is */
    /* enabled (TRUE) or disabled (FALSE). */
    protected void inBandRingToneSettingIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean enabled) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Hands Free service */
    /* when the remote Audio Gateway responds to a request for a phone */
    /* number to attach to a voice tag. The ConnectionType and */
    /* RemoteDeviceAddress members identify the connection receiving this */
    /* confirmation. The Success member specifies whether or not the */
    /* phone number was associated with a voice tag. The PhoneNumber */
    /* member is a NULL terminated ASCII string representing the phone */
    /* number that was attached to a voice tag. */
    protected void voiceTagRequestConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String phoneNumber) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Hands Free service */
    /* when a remote Audio gateway responds to a request for the list of */
    /* current calls. The ConnectionType and RemoteDeviceAddress members */
    /* identify the connection receiving the confirmation. The */
    /* CurrentCallList member is an array that contains */
    /* CurrentCallListLength entries each describing a single call. */
    protected void currentCallsListConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int index, int callDirection, int callStatus, int callMode, boolean multiparty, String phoneNumber, int numberFormat) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* This event is dispatched to a local Hands Free service when a */
    /* remote Audio Gateway responds to the request for current operator */
    /* selection. The ConnectionType and RemoteDeviceAddress members */
    /* identify the connection receiving the event. The NetworkMode */
    /* member contains the mode returned in the response. The */
    /* NetworkOperator member is a pointer to a NULL terminated ASCII */
    /* string that contains the returned operator name. */
    protected void networkOperatorSelectionConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int networkMode, String networkOperator) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* This event is dispatched to a local Hands Free service when a */
    /* remote Audio Gateway responds to a request for the current */
    /* subscriber number. The ConnectionType and RemoteDeviceAddress */
    /* members identify the connection receiving the confirmation. The */
    /* ServiceType member contains the service type value included in the */
    /* response. The NumberSbuscriberEntries member specifies the number */
    /* of Subscriber Number Information entries that are pointed to by */
    /* the SubscriberNumberList member. */
    protected void subscriberNumberInformationConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int serviceType, int numberFormat, String phoneNumber) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* This event is dispatched to a local Hands Free service when a */
    /* remote Audio Gateway device responds to a request for the current */
    /* Response/Hold status. The ConnectionType and RemoteDeviceAddress */
    /* members identify the connection receiving the event. The */
    /* CallState member contains the call state sent in the response. */
    protected void responseHoldStatusConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int callState) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Hands Free service */
    /* when the remote Audio Gateway responds to a commmand sent by the */
    /* Hands Free unit or generates an unsolicited result code. The */
    /* ConnectionType and RemoteDeviceAddress members identify the */
    /* connection receiving this indication. The ResultValue member */
    /* contains the actual result code value if the ResultType parameter */
    /* indicates that a result code is expected (erResultCode). */
    protected void commandResultEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int resultType, int resultValue) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Hands Free unit when */
    /* the remote Audio Gateway issues either a non-solicited arbitrary */
    /* response OR a solicited arbitrary response that is not recognized */
    /* by the local Hands Free unit (i.e. an arbitrary AT Response). */
    /* The ConnectionType and RemoteDeviceAddress members identify the */
    /* connection receiving this evnet. The ResponseData member is a */
    /* pointer to a NULL terminated ASCII string that represents the */
    /* actual response data that was received. */
    protected void arbitraryResponseIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String responseData) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* Audio Gateway specific events. */

    /* The following event is dispatched to a local Audio Gateway when */
    /* the remote Hands Free service responds to a call waiting */
    /* notification by selecting how to handle the waiting call. The */
    /* ConnectionType and RemoteDeviceAddress members identify the local */
    /* service receiving this indication. The CallHoldMultipartyHandling */
    /* member specifies the requested action to take regarding the */
    /* waiting call. If the CallHoldMultipartyHandling member indicates */
    /* an extended type then the Index member will contain the call index */
    /* to which the operation refers. */
    protected void callHoldMultipartySelectionIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int callHoldMultipartyHandling, int index) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Audio Gateway when */
    /* the remote Hands Free device issues the command to enable or */
    /* disable call waiting notification. The ConnectionType and */
    /* RemoteDeviceAddress members identify the local service receiving */
    /* this indication. The Enabled member specifies whether this is an */
    /* indication to enable (TRUE) or disable (FALSE) call waiting */
    /* notification. */
    protected void callWaitingNotificationActivationIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean enabled) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Audio Gateway when */
    /* the remote Hands Free device issues the command to enable or */
    /* disable call line identification notification. The ConnectionType */
    /* and RemoteDeviceAddress members identify the local service */
    /* receiving this indication. The Enable member specifies whether */
    /* this is an indication to enable (TRUE) or disable (FALSE) call */
    /* line identification notification. */
    protected void callLineIdentificationNotificationActivationIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean enabled) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Audio Gateway when */
    /* the remote Hands Free device requests turning off the Audio */
    /* Gateways echo cancelling and noise reduction functions. The */
    /* ConnectionType and RemoteDeviceAddress members identify the local */
    /* connection receiving this indication. */
    protected void disableSoundEnhancementIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Audio Gateway when */
    /* the remote Hands Free device issues the command to place a call to */
    /* a specific phone number. The ConnectionType and */
    /* RemoteDeviceAddress members identify the local connection */
    /* receiving this indication. The PhoneNumber member is a NULL */
    /* terminated ASCII string representing the phone number in which to */
    /* place the call. */
    protected void dialPhoneNumberIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String phoneNumber) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Audio Gateway when */
    /* the remote Hands Free device issues the command for memory */
    /* dialing. The ConnectionType and RemoteDeviceAddress members */
    /* identify the local connection receiving this indication. The */
    /* MemoryLocation member specifies the memory location in which the */
    /* phone number to dial exists. */
    protected void dialPhoneNumberFromMemoryIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int memoryLocation) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Audio Gateway when */
    /* the remote Hands Free device issues the command to re-dial the */
    /* last number dialed. The ConnectionType and RemoteDeviceAddress */
    /* member identifies the local connection receiving this indication. */
    protected void redialLastPhoneNumberIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Audio Gateway when */
    /* the remote Hands Free device issues the command to transmit a DTMF */
    /* code. The ConnectionType and RemoteDeviceAddress members identify */
    /* the local connection receiving this indication. The DTMFCode */
    /* member specifies the DTMF code to generate. */
    protected void generateDTMFCodeIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, char dtmfCode) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Audio Gateway when */
    /* the remote Hands Free device issues the command to answer an */
    /* incoming call. The ConnectionType and RemoteDeviceAddress members */
    /* identify the local connection receiving this indication. */
    protected void answerCallIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Audio Gateway when */
    /* the remote Hands Free device makes a request for a phone number to */
    /* attach to a voice tag. The ConnectionType and RemoteDeviceAddress */
    /* members identify the local connection receiving this indication. */
    protected void voiceTagRequestIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Audio Gateway when */
    /* the remote Hands Free device issues the command to hang-up an */
    /* on-going call or to reject and incoming call request. The */
    /* ConnectionType and RemoteDeviceAddress members identifu the local */
    /* connection receiving this indication. */
    protected void hangUpIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Audio gateway when it */
    /* receives a request for the current call list. The ConnectionType */
    /* and RemoteDeviceAddress members identify the local connection */
    /* receiving the indication. */
    protected void currentCallsListIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* This event is dispatched to a local Audio Gateway service when the */
    /* remote Hands Free device issues a set operator selection format */
    /* command. The ConnectionType and RemoteDeviceAddress members */
    /* identify the local connection receiving the indication. The */
    /* Format member contains the format value provided in this */
    /* operation. The Bluetooth Hands Free specification requires that */
    /* the remote device choose format 0. As a result, this event can */
    /* generally be ignored. */
    protected void networkOperatorSelectionFormatIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int format) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* This event is dispatched to a local Audio Gateway service when the */
    /* remote Hands Free device has requested the current operator */
    /* selection. The ConnectionType and RemoteDeviceAddress members */
    /* identify the local connection receiving the indication. */
    protected void networkOperatorSelectionIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* This event is dispatched to a local Audio Gateway service when a */
    /* remote Hands Free device sends a command activate extended error */
    /* reporting. The ConnectionType and RemoteDeviceAddress members */
    /* identify the local connection receiving the indication. The */
    /* Enabled member contains a BOOLEAN value which indicates the */
    /* current state of extended error reporting. */
    protected void extendedErrorResultActivationIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean enabled) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* This event is dispatched to a local Audio Gateway service when a */
    /* remote Hands Free device requests the current subscriber number. */
    /* The ConnectionType and RemoteDeviceAddress members identify the */
    /* local connection receiving the indication. */
    protected void subscriberNumberInformationIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* This event is dispatched to a local Audio Gateway service when a */
    /* remote Hands Free device requests the current response/hold */
    /* status. The ConnectionType and RemoteDeviceAddress members */
    /* identify the local connection receiving the indication. */
    protected void responseHoldStatusIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following event is dispatched to a local Audio Gateway service */
    /* when the remote Hands Free device issues a commmand that is not */
    /* recognized by the local Audio Gateway (i.e. an arbitrary AT */
    /* Command). The ConnectionType and RemoteDeviceAddress members */
    /* identify the local connection receiving this indication. The */
    /* CommandData member is a pointer to a NULL terminated ASCII string */
    /* that represents the actual command data that was received. */
    protected void arbitraryCommandIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String commandData) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /* The following declared type represents the Prototype Function for */
    /* an Event Callback. This function will be called whenever the */
    /* Hands Free Manager dispatches an event (and the client has */
    /* registered for events). This function passes to the caller the */
    /* Hands Free Manager Event and the Callback Parameter that was */
    /* specified when this Callback was installed. The caller is free to */
    /* use the contents of the Event Data ONLY in the context of this */
    /* callback. If the caller requires the Data for a longer period of */
    /* time, then the callback function MUST copy the data into another */
    /* Data Buffer. This function is guaranteed NOT to be invoked more */
    /* than once simultaneously for the specified installed callback */
    /* (i.e. this function DOES NOT have be reentrant). Because of */
    /* this, the processing in this function should be as efficient as */
    /* possible. It should also be noted that this function is called in */
    /* the Thread Context of a Thread that the User does NOT own. */
    /* Therefore, processing in this function should be as efficient as */
    /* possible (this argument holds anyway because another Message will */
    /* not be processed while this function call is outstanding). */
    /* * NOTE * This function MUST NOT block and wait for events that can */
    /* only be satisfied by Receiving other Events. A deadlock */
    /* WILL occur because NO Event Callbacks will be issued */
    /* while this function is currently outstanding. */
    /**
     * Receiver for event notifications which are common among both Hands-Free
     * and Audio Gateway sides of a Hands-Free Profile connection.
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
         * {@link HFRM#connectionRequestResponse}.
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
         * @see HFRM#disconnectDevice
         */
        public void disconnectedEvent(BluetoothAddress remoteDeviceAddress, DisconnectedReason reason);

        /**
         * Invoked when an outgoing connection attempt completes.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote device.
         * @param connectionStatus Indication of success or the reason for
         *            failure.
         *
         * @see HFRM#connectRemoteDevice
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
         * @see AudioGatewayServerManager#setupAudioConnection
         * @see #audioConnectionStatusEvent
         */
        public void audioConnectedEvent(BluetoothAddress remoteDeviceAddress);

        /**
         * Invoked when a SCO audio connection is disconnected.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote device.
         *
         * @see AudioGatewayServerManager#releaseAudioConnection
         */
        public void audioDisconnectedEvent(BluetoothAddress remoteDeviceAddress);

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
         * @see AudioGatewayServerManager#setupAudioConnection
         * @see #audioConnectedEvent
         */
        public void audioConnectionStatusEvent(BluetoothAddress remoteDeviceAddress, boolean successful);

        /**
         * Invoked when SCO audio data is received from the remote device.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote device.
         * @param audioData Raw PCM audio data.
         *
         * @see HFRM#sendAudioData
         */
        public void audioDataEvent(BluetoothAddress remoteDeviceAddress, byte[] audioData);

        /**
         * Invoked when the remote device requests or enables voice recognition.
         * <p>
         * When this event is received by a local Hands Free Manager, it
         * indicates that the remote Audio Gateway has enabled voice
         * recognition. When this event is received by a local Audio Gateway
         * Manager, it indicates that the remote Hands Free device is requesting
         * that voice recognition should be enabled.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote device.
         * @param voiceRecognitionActive {@code true} if voice activation is or
         *            should be activated.
         *
         * @see HFRM#setRemoteVoiceRecognitionActivation
         */
        public void voiceRecognitionIndicationEvent(BluetoothAddress remoteDeviceAddress, boolean voiceRecognitionActive);

        /**
         * Invoked when the remote device notifies the local server of a change
         * to the current speaker gain.
         * <p>
         * When this event is received by a local Hands Free Manager, it is used
         * to set the local speaker gain. When this event is received by a local
         * Audio Gateway Manager, it is used in volume level synchronization to
         * inform the local Audio Gateway of the current speaker gain on the
         * remote Hands Free unit.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote device.
         * @param speakerGain The current or new speaker gain value. Will always
         *            be between {@link HFRM#SPEAKER_GAIN_MINIMUM} and
         *            {@link HFRM#SPEAKER_GAIN_MAXIMUM}, inclusive.
         *
         * @see HFRM#setRemoteSpeakerGain
         */
        public void speakerGainIndicationEvent(BluetoothAddress remoteDeviceAddress, int speakerGain);

        /**
         * Invoked when the remote device notifies the local server of a change
         * to the current microphone gain.
         * <p>
         * When this event is received by a local Hands Free Manager, it is used
         * to set the local microphone gain. When this event is received by a
         * local Audio Gateway Manager, it is used in volume level
         * synchronization to inform the local Audio Gateway of the current
         * microphone gain on the remote Hands Free unit.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote device.
         * @param microphoneGain The current or new microphone gain value. Will
         *            always be between {@link HFRM#SPEAKER_GAIN_MINIMUM} and
         *            {@link HFRM#SPEAKER_GAIN_MAXIMUM}, inclusive.
         *
         * @see HFRM#setRemoteMicrophoneGain
         */
        public void microphoneGainIndicationEvent(BluetoothAddress remoteDeviceAddress, int microphoneGain);

        /**
         * Invoked when the remote device notifies or requests a change to the
         * state of a call.
         * <p>
         * When this event is received by a local Hands Free Manager, it
         * indicates a change in the current Response/Hold Status on the remote
         * Audio Gateway. When this event is received by a local Audio Gateway
         * Manager, it indicates a request from the remote Hands Free device to
         * set the current call state.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote device.
         * @param callState The requested current call state or the new
         *            response/hold status, depending on the Manager type
         *            receiving this event.
         */
        public void incomingCallStateIndicationEvent(BluetoothAddress remoteDeviceAddress, CallState callState);
    }

    /**
     * Management connection to the local Hands-Free Server.
     */
    public static final class HandsFreeServerManager extends HFRM {
        /* The following Bit Definitions represent the Hands-Free Server */
        /* supported features. These Bit Definitions are used with the */
        /* HFRE_Open_HandsFree_Server_Port() function to specify the features */
        /* supported by the server. */
        private final static int SUPPORTED_FEATURE_SOUND_ENHANCEMENT              = 0x00000001;
        private final static int SUPPORTED_FEATURE_CALL_WAITING_THREE_WAY_CALLING = 0x00000002;
        private final static int SUPPORTED_FEATURE_CLI                            = 0x00000004;
        private final static int SUPPORTED_FEATURE_VOICE_RECOGNITION              = 0x00000008;
        private final static int SUPPORTED_FEATURE_REMOTE_VOLUME_CONTROL          = 0x00000010;
        private final static int SUPPORTED_FEATURE_ENHANCED_CALL_STATUS           = 0x00000020;
        private final static int SUPPORTED_FEATURE_ENHANCED_CALL_CONTROL          = 0x00000040;

        /**
         * Features which a Hands-Free device can optionally support.
         */
        public enum SupportedFeatures {
            /**
             * Support for error correction and/or noise reduction.
             */
            SOUND_ENHANCEMENT,

            /**
             * Support for call waiting and multi-party calls.
             */
            CALL_WAITING_THREE_WAY_CALLING,

            /**
             * Support for the CLI presentation capability.
             */
            CLI,

            /**
             * Support for voice recognition activation.
             */
            VOICE_RECOGNITION,

            /**
             * Support for remote volume control.
             */
            REMOTE_VOLUME_CONTROL,

            /**
             * Support for receiving enhanced call status notifications.
             */
            ENHANCED_CALL_STATUS,

            /**
             * Support for extended call control commands which require an index
             * parameter.
             */
            ENHANCED_CALL_CONTROL
        }

        /**
         * Local Hands-Free service configuration.
         *
         * @see #queryLocalConfiguration
         */
        public static final class LocalConfiguration {
            private final EnumSet<IncomingConnectionFlags> incomingConnectionFlags;
            private final EnumSet<SupportedFeatures>       supportedFeatures;
            private final long                             networkType;
            private final ConfigurationIndicatorEntry[]    additionalIndicatorList;

            /*pkg*/ LocalConfiguration(EnumSet<IncomingConnectionFlags> incomingConnectionFlags, EnumSet<SupportedFeatures> supportedFeatures, int networkType, ConfigurationIndicatorEntry[] additionalIndicatorList) {
                this.incomingConnectionFlags = incomingConnectionFlags;
                this.supportedFeatures       = supportedFeatures;
                this.networkType             = networkType;
                this.additionalIndicatorList = additionalIndicatorList;
            }

            public EnumSet<IncomingConnectionFlags> getIncomingConnectionFlags() {
                return incomingConnectionFlags;
            }

            public EnumSet<SupportedFeatures> getSupportedFeatures() {
                return supportedFeatures;
            }

            public long getNetworkType() {
                return networkType;
            }

            public ConfigurationIndicatorEntry[] getAdditionalIndicators() {
                return additionalIndicatorList;
            }
        }

        /**
         * Create a new connection to the local Hands-Free Server.
         * <p>
         * Note that this is distinct from the Hands-Free Profile. This server
         * is specific to the role of a Hands-Free device (such as a wearable
         * headset or a car-kit). This server supports connecting to remote
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
        public HandsFreeServerManager(boolean controller, HandsFreeEventCallback eventCallback) throws ServerNotReachableException {
            super(SERVER_TYPE_HANDSFREE, controller, eventCallback);
        }

        /* Hands Free Functions. */

        /**
         * Retreive the current configuration of the local service.
         *
         * @return The configuration parameters of the local service or
         *         {@code null} if there was an error.
         **/
        public LocalConfiguration queryLocalConfiguration() {
            LocalConfiguration               config;
            LocalConfigurationNative         nativeConfig;
            EnumSet<IncomingConnectionFlags> incomingConnectionFlags;
            EnumSet<SupportedFeatures>       supportedFeatures;

            nativeConfig = queryCurrentConfigurationNative(serverType);

            if(nativeConfig != null) {
                incomingConnectionFlags = EnumSet.noneOf(IncomingConnectionFlags.class);

                if((nativeConfig.incomingConnectionFlags & INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION) == INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION)
                    incomingConnectionFlags.add(IncomingConnectionFlags.REQUIRE_AUTHORIZATION);
                if((nativeConfig.incomingConnectionFlags & INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION) == INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION)
                    incomingConnectionFlags.add(IncomingConnectionFlags.REQUIRE_AUTHENTICATION);
                if((nativeConfig.incomingConnectionFlags & INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION) == INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION)
                    incomingConnectionFlags.add(IncomingConnectionFlags.REQUIRE_ENCRYPTION);

                supportedFeatures = EnumSet.noneOf(SupportedFeatures.class);

                if((nativeConfig.supportedFeaturesMask & SUPPORTED_FEATURE_SOUND_ENHANCEMENT) == SUPPORTED_FEATURE_SOUND_ENHANCEMENT)
                    supportedFeatures.add(SupportedFeatures.SOUND_ENHANCEMENT);
                if((nativeConfig.supportedFeaturesMask & SUPPORTED_FEATURE_CALL_WAITING_THREE_WAY_CALLING) == SUPPORTED_FEATURE_CALL_WAITING_THREE_WAY_CALLING)
                    supportedFeatures.add(SupportedFeatures.CALL_WAITING_THREE_WAY_CALLING);
                if((nativeConfig.supportedFeaturesMask & SUPPORTED_FEATURE_CLI) == SUPPORTED_FEATURE_CLI)
                    supportedFeatures.add(SupportedFeatures.CLI);
                if((nativeConfig.supportedFeaturesMask & SUPPORTED_FEATURE_VOICE_RECOGNITION) == SUPPORTED_FEATURE_VOICE_RECOGNITION)
                    supportedFeatures.add(SupportedFeatures.VOICE_RECOGNITION);
                if((nativeConfig.supportedFeaturesMask & SUPPORTED_FEATURE_REMOTE_VOLUME_CONTROL) == SUPPORTED_FEATURE_REMOTE_VOLUME_CONTROL)
                    supportedFeatures.add(SupportedFeatures.REMOTE_VOLUME_CONTROL);
                if((nativeConfig.supportedFeaturesMask & SUPPORTED_FEATURE_ENHANCED_CALL_STATUS) == SUPPORTED_FEATURE_ENHANCED_CALL_STATUS)
                    supportedFeatures.add(SupportedFeatures.ENHANCED_CALL_STATUS);
                if((nativeConfig.supportedFeaturesMask & SUPPORTED_FEATURE_ENHANCED_CALL_CONTROL) == SUPPORTED_FEATURE_ENHANCED_CALL_CONTROL)
                    supportedFeatures.add(SupportedFeatures.ENHANCED_CALL_CONTROL);

                config = new LocalConfiguration(incomingConnectionFlags, supportedFeatures, nativeConfig.networkType, nativeConfig.additionalIndicatorList);
            } else {
                config = null;
            }

            return config;
        }

        /**
         * Submit a request to the remote Audio Gateway for the current status
         * of the control indicators.
         * <p>
         * The results to this query will be returned as part of the Control
         * Indicator Status Confirmation event (
         * {@link HandsFreeEventCallback#controlIndicatorStatusConfirmationEvent}
         * ).
         * <p>
         * This call requires the Manager to be the Controller for the local
         * server.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote device.
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int queryRemoteControlIndicatorStatus(BluetoothAddress remoteDeviceAddress) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return queryRemoteControlIndicatorStatusNative(address[0], address[1], address[2], address[3], address[4], address[5]);
        }

        /**
         * Enable or disable notification of changes to Indicators on the remote
         * Audio Gateway.
         * <p>
         * When enabled, the remote Audio Gateway device will send unsolicited
         * responses to update the local Hands-Free server of the current
         * control indicator values. Registered Managers will receive these
         * notifications by way of
         * {@link HandsFreeEventCallback#controlIndicatorStatusIndicationEvent}.
         * <p>
         * This call requires the Manager to be a Controller for the local
         * server and that a valid Service Level Connection has been
         * established.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Audio
         *            Gateway.
         * @param enableEventNotification {@code true} if notification should be
         *            enabled.
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int enableRemoteIndicatorEventNotification(BluetoothAddress remoteDeviceAddress, boolean enableEventNotification) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return enableRemoteIndicatorEventNotificationNative(address[0], address[1], address[2], address[3], address[4], address[5], enableEventNotification);
        }

        /**
         * Submit a request to the remote Audio Gateway for its supported Call
         * Hold and Multi-party services.
         * <p>
         * The result of this query will be returned by way of the Call Hold and
         * Multi-party Support Confirmation event (
         * {@link HandsFreeEventCallback#callHoldMultipartySupportConfirmationEvent}
         * ).
         * <p>
         * This call requires the Manager to be a Controller for the local
         * server and that a valid Service Level Connection has been
         * established.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Audio
         *            Gateway.
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int queryRemoteCallHoldingMultipartyServiceSupport(BluetoothAddress remoteDeviceAddress) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return queryRemoteCallHoldingMultipartyServiceSupportNative(address[0], address[1], address[2], address[3], address[4], address[5]);
        }

        /**
         * Send commands to the remote Audio Gateway to manage multiple
         * concurrent calls.
         * <p>
         * This call requires the Manager to be a Controller for the local
         * server and that a valid Service Level Connection has been
         * established. The local server must also support call waiting and
         * multi-party services.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Audio
         *            Gateway.
         * @param callHoldMultipartyHandling Command to send to the remote Audio
         *            Gateway.
         * @param index Index which applies to certain commands from
         *            {@link CallHoldMultipartyHandlingType}. For commands which
         *            do not require an index, this parameter is ignored.
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int sendCallHoldingMultipartySelection(BluetoothAddress remoteDeviceAddress, CallHoldMultipartyHandlingType callHoldMultipartyHandling, int index) {
            int handlingMethod = 0;
            byte[] address = remoteDeviceAddress.internalByteArray();

            switch(callHoldMultipartyHandling) {
            case RELEASE_ALL_HELD_CALLS:
                handlingMethod = MULTIPARTY_HANDLING_RELEASE_ALL_HELD_CALLS;
                break;
            case RELEASE_ALL_ACTIVE_CALLS_ACCEPT_WAITING_CALL:
                handlingMethod = MULTIPARTY_HANDLING_RELEASE_ALL_ACTIVE_CALLS_ACCEPT_WAITING_CALL;
                break;
            case PLACE_ALL_ACTIVE_CALLS_ON_HOLD_ACCEPT_THE_OTHER:
                handlingMethod = MULTIPARTY_HANDLING_PLACE_ALL_ACTIVE_CALLS_ON_HOLD_ACCEPT_THE_OTHER;
                break;
            case ADD_A_HELD_CALL_TO_CONVERSATION:
                handlingMethod = MULTIPARTY_HANDLING_ADD_A_HELD_CALL_TO_CONVERSATION;
                break;
            case CONNECT_TWO_CALLS_AND_DISCONNECT:
                handlingMethod = MULTIPARTY_HANDLING_CONNECT_TWO_CALLS_AND_DISCONNECT;
                break;
            case RELEASE_SPECIFIED_CALL_INDEX:
                handlingMethod = MULTIPARTY_HANDLING_RELEASE_SPECIFIED_CALL_INDEX;
                break;
            case PRIVATE_CONSULTATION_MODE:
                handlingMethod = MULTIPARTY_HANDLING_PRIVATE_CONSULTATION_MODE;
                break;
            }

            return sendCallHoldingMultipartySelectionNative(address[0], address[1], address[2], address[3], address[4], address[5], handlingMethod, index);
        }

        /**
         * Enable or disable notification of call waiting status from the remote
         * Audio Gateway.
         * <p>
         * This notification is disabled by default.
         * <p>
         * This call requires the Manager to be a Controller for the local
         * server and that a valid Service Level Connection has been
         * established. The local server must also support Call Waiting and
         * Multi-party services.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Audio
         *            Gateway.
         * @param enableNotification If {@code true}, enable notifications. If
         *            {@code false}, the remote Audio Gateway will not send
         *            notifications.
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int enableRemoteCallWaitingNotification(BluetoothAddress remoteDeviceAddress, boolean enableNotification) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return enableRemoteCallWaitingNotificationNative(address[0], address[1], address[2], address[3], address[4], address[5], enableNotification);
        }

        /**
         * Enable or disable notification of Calling Line Identification from
         * the remote Audio Gateway.
         * <p>
         * This notification is disabled by default.
         * <p>
         * This call requires the Manager to be a Controller for the local
         * server and that a valid Service Level Connection has been
         * established. The local server must also support Calling Line
         * Identification.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Audio
         *            Gateway.
         * @param enableNotification If {@code true}, enable the notification.
         *            If {@code false} , the remote Audio Gateway will not send
         *            the notification.
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int enableRemoteCallLineIdentificationNotification(BluetoothAddress remoteDeviceAddress, boolean enableNotification) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return enableRemoteCallLineIdentificationNotificationNative(address[0], address[1], address[2], address[3], address[4], address[5], enableNotification);
        }

        /**
         * Instruct the remote Audio Gateway to dial a phone number.
         * <p>
         * This call requires the Manager to be a Controller for the local
         * server and that a valid Service Level Connection has been
         * established.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Audio
         *            Gateway.
         * @param phoneNumber The phone number to dial. The length of the string
         *            must be between {@link HFRM#PHONE_NUMBER_LENGTH_MINIMUM}
         *            and {@link HFRM#PHONE_NUMBER_LENGTH_MAXIMUM}.
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int dialPhoneNumber(BluetoothAddress remoteDeviceAddress, String phoneNumber) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return dialPhoneNumberNative(address[0], address[1], address[2], address[3], address[4], address[5], phoneNumber);
        }

        /**
         * Instruct the remote Audio Gateway to dial a phone number from a list
         * of numbers preloaded on the Audio Gateway.
         * <p>
         * This call requires the Manager to be a Controller for the local
         * server and that a valid Service Level Connection has been
         * established.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Audio
         *            Gateway.
         * @param memoryLocation Index of the phone number to dial.
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int dialPhoneNumberFromMemory(BluetoothAddress remoteDeviceAddress, int memoryLocation) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return dialPhoneNumberFromMemoryNative(address[0], address[1], address[2], address[3], address[4], address[5], memoryLocation);
        }

        /**
         * Instruct the remote Audio Gateway to redial the most recently dialed
         * phone number.
         * <p>
         * This call requires the Manager to be a Controller for the local
         * server and that a valid Service Level Connection has been
         * established.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Audio
         *            Gateway.
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int redialLastPhoneNumber(BluetoothAddress remoteDeviceAddress) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return redialLastPhoneNumberNative(address[0], address[1], address[2], address[3], address[4], address[5]);
        }

        /**
         * Instruct the remote Audio Gateway to answer an incoming call.
         * <p>
         * This call requires the Manager to be a Controller for the local
         * server and that a valid Service Level Connection has been
         * established.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Audio
         *            Gateway.
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int answerIncomingCall(BluetoothAddress remoteDeviceAddress) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return answerIncomingCallNative(address[0], address[1], address[2], address[3], address[4], address[5]);
        }

        /**
         * Instruct the remote Audio Gateway to send a DTMF tone over an active
         * phone call.
         * <p>
         * This call requires the Manager to be a Controller for the local
         * server and that a valid Service Level Connection has been
         * established.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Audio
         *            Gateway.
         * @param dtmfCode Code representing the desired DTMF tone. Valid codes
         *            include the characters {@code 0}-{@code 9}, {@code *},
         *            {@code #}, and {@code A}-{@code D}.
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int transmitDTMFCode(BluetoothAddress remoteDeviceAddress, char dtmfCode) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return transmitDTMFCodeNative(address[0], address[1], address[2], address[3], address[4], address[5], dtmfCode);
        }

        /**
         * Request a phone number from the remote Audio Gateway to associated
         * with a unique voice tag.
         * <p>
         * The result of this request is returned by way of a Voice Tag Request
         * Confirmation event (
         * {@link HandsFreeEventCallback#voiceTagRequestConfirmationEvent}). The
         * Hands-Free unit can then store this phone number for use with its own
         * internal voice recognition features and dial the numbers using
         * {@link #dialPhoneNumber(BluetoothAddress, String)}.
         * <p>
         * This call requires the Manager to be a Controller for the local
         * server and that a valid Service Level Connection has been
         * established. The local server must support voice recognition. Once
         * this method is called, no other method may be called until a Voice
         * Tag Response Confirmation event is received from the remote Audio
         * Gateway.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Audio
         *            Gateway.
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int voiceTagRequest(BluetoothAddress remoteDeviceAddress) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return voiceTagRequestNative(address[0], address[1], address[2], address[3], address[4], address[5]);
        }

        /**
         * Instruct the remote Audio Gateway to reject an incoming call or
         * terminate an active call.
         * <p>
         * This call requires the Manager to be a Controller for the local
         * server and that a valid Service Level Connection has been
         * established.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Audio
         *            Gateway.
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int hangUpCall(BluetoothAddress remoteDeviceAddress) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return hangUpCallNative(address[0], address[1], address[2], address[3], address[4], address[5]);
        }

        /**
         * Query the remote Audio Gateway for the list of current calls.
         * <p>
         * The result of this query is returned by way of the Current Calls List
         * Confirmation event (
         * {@link HandsFreeEventCallback#currentCallsListConfirmationEvent}).
         * <p>
         * This call requires the Manager to be a Controller for the local
         * server and that a valid Service Level Connection has been
         * established.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Audio
         *            Gateway.
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int queryRemoteCurrentCallsList(BluetoothAddress remoteDeviceAddress) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return queryRemoteCurrentCallsListNative(address[0], address[1], address[2], address[3], address[4], address[5]);
        }

        /**
         * Inform the Audio Gateway that the expected format of the Network
         * Operator Name shall be Long Alphanumeric.
         * <p>
         * This call requires the Manager to be a Controller for the local
         * server and that a valid Service Level Connection has been
         * established.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Audio
         *            Gateway.
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int setNetworkOperatorSelectionFormat(BluetoothAddress remoteDeviceAddress) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return setNetworkOperatorSelectionFormatNative(address[0], address[1], address[2], address[3], address[4], address[5]);
        }

        /**
         * Query the remote Audio Gateway for the name of the current Network
         * Operator.
         * <p>
         * The network operator format must be set by a call to
         * {@link #setNetworkOperatorSelectionFormat} before calling this
         * method. The result of this query is returned by way of a Network
         * Operator Selection Confirmation event (
         * {@link HandsFreeEventCallback#networkOperatorSelectionConfirmationEvent}
         * ).
         * <p>
         * This call requires the Manager to be a Controller for the local
         * server and that a valid Service Level Connection has been
         * established.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Audio
         *            Gateway.
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int queryRemoteNetworkOperatorSelection(BluetoothAddress remoteDeviceAddress) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return queryRemoteNetworkOperatorSelectionNative(address[0], address[1], address[2], address[3], address[4], address[5]);
        }

        /**
         * Enable or disable Extended Error Result reports from the remote Audio
         * Gateway.
         * <p>
         * This call requires the Manager to be a Controller for the local
         * server and that a valid Service Level Connection has been
         * established.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Audio
         *            Gateway.
         * @param enableExtendedErrorResults If {@code true}, enable Extended
         *            Error Result notifications.
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int enableRemoteExtendedErrorResult(BluetoothAddress remoteDeviceAddress, boolean enableExtendedErrorResults) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return enableRemoteExtendedErrorResultNative(address[0], address[1], address[2], address[3], address[4], address[5], enableExtendedErrorResults);
        }

        /**
         * Query the remote Audio Gateway for the Subscriber Number Information.
         * <p>
         * The result of this query is returned by a way of a Subscriber Number
         * Information Confirmation event (
         * {@link HandsFreeEventCallback#subscriberNumberInformationConfirmationEvent}
         * ).
         * <p>
         * This call requires the Manager to be a Controller for the local
         * server and that a valid Service Level Connection has been
         * established.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Audio
         *            Gateway.
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int querySubscriberNumberInformation(BluetoothAddress remoteDeviceAddress) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return querySubscriberNumberInformationNative(address[0], address[1], address[2], address[3], address[4], address[5]);
        }

        /**
         * Query the remote Audio Gateway for the current Response/Hold status.
         * <p>
         * The result of the query is returned by way of a Response/Hold Status
         * Confirmation event (
         * {@link HandsFreeEventCallback#responseHoldStatusConfirmationEvent}).
         * <p>
         * This call requires the Manager to be a Controller for the local
         * server and that a valid Service Level Connection has been
         * established.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Audio
         *            Gateway.
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int queryResponseHoldStatus(BluetoothAddress remoteDeviceAddress) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return queryResponseHoldStatusNative(address[0], address[1], address[2], address[3], address[4], address[5]);
        }

        /**
         * Instruct the remote Audio Gateway to respond to an incoming phone
         * call.
         * <p>
         * This call requires the Manager to be a Controller for the local
         * server and that a valid Service Level Connection has been
         * established.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Audio
         *            Gateway.
         * @param callState The action the remote Audio Gateway should take
         *            regarding the current incoming phone call.
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int setIncomingCallState(BluetoothAddress remoteDeviceAddress, CallState callState) {
            int state = 0;
            byte[] address = remoteDeviceAddress.internalByteArray();

            switch(callState) {
            case HOLD:
                state = CALL_STATE_HOLD;
                break;
            case ACCEPT:
                state = CALL_STATE_ACCEPT;
                break;
            case REJECT:
                state = CALL_STATE_REJECT;
                break;
            case NONE:
                state = CALL_STATE_NONE;
                break;
            }

            return setIncomingCallStateNative(address[0], address[1], address[2], address[3], address[4], address[5], state);
        }

        /**
         * Send an arbitrary command to the remote Audio Gateway.
         * <p>
         * The command sent to the Audio Gateway must begin with "AT" and end
         * with a carriage return ('\r') if this is the first portion of an
         * arbitrary command that will span multiple writes. Subsequent calls
         * (until the actual status response is received) can begin with any
         * character, however, they must end with a carriage return ('\r').
         * <p>
         * This call requires the Manager to be a Controller for the local
         * server and that a valid Service Level Connection has been
         * established.
         *
         * @param remoteDeviceAddress
         *            Bluetooth address of the remote Audio Gateway.
         * @param arbitraryCommand
         *            The command to send to the remote Audio Gateway. This must
         *            begin with "AT" and end with '\r'.
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         *
         * @see HandsFreeEventCallback#arbitraryResponseIndicationEvent
         */
        public int sendArbitraryCommand(BluetoothAddress remoteDeviceAddress, String arbitraryCommand) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return sendArbitraryCommandNative(address[0], address[1], address[2], address[3], address[4], address[5], arbitraryCommand);
        }

        /* The following event acts as an indication to inform the current */
        /* state of the service level connection. The ConnectionType member */
        /* identifies the connection type to which this event applies. The */
        /* RemoteSupportedFeaturesValid member specifies whether or not the */
        /* Remote Support Features member is valid. The */
        /* RemoteSupportedFeatures member specifies supported features which */
        /* are supported by the remote device. The */
        /* RemoteCallHoldMultipartySupported member specifies the support of */
        /* this feature by the remote device. This indication must be */
        /* received before performing any other action on this port. If an */
        /* error occurs during the establishment of the service level */
        /* connection the connection will be closed and local device will */
        /* receive a disconnection event. */
        /* * NOTE * The RemoteCallHoldMultipartySupport member will only be */
        /* valid if the local and remote device both have the */
        /* "Three-way Calling Support" bit set in their supported */
        /* features. */
        /* * NOTE * The RemoteCall Hold Multipary Support member will always */
        /* be set to */
        /* HFRE_CALL_HOLD_MULTIPARTY_SUPPORTED_FEATURES_ERROR in the */
        /* case when this indication is received by an audio gateway */
        /* as Hands Free units have no call hold multiparty */
        /* supported features to query. */
        @Override
        protected void serviceLevelConnectionEstablishedEvent(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean remoteSupportedFeaturesValid, int remoteSupportedFeatures, int remoteCallHoldMultipartySupport) {
            EnumSet<AudioGatewayServerManager.SupportedFeatures>  supportedFeatureSet = null;
            EnumSet<AudioGatewayServerManager.MultipartyFeatures> multipartyFeatureSet = null;
            EventCallback                                         callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                if(remoteSupportedFeaturesValid) {
                    supportedFeatureSet = EnumSet.noneOf(AudioGatewayServerManager.SupportedFeatures.class);

                    if((remoteSupportedFeatures & AudioGatewayServerManager.SUPPORTED_FEATURE_THREE_WAY_CALLING) == AudioGatewayServerManager.SUPPORTED_FEATURE_THREE_WAY_CALLING)
                        supportedFeatureSet.add(AudioGatewayServerManager.SupportedFeatures.THREE_WAY_CALLING);
                    if((remoteSupportedFeatures & AudioGatewayServerManager.SUPPORTED_FEATURE_AG_SOUND_ENHANCEMENT) == AudioGatewayServerManager.SUPPORTED_FEATURE_AG_SOUND_ENHANCEMENT)
                        supportedFeatureSet.add(AudioGatewayServerManager.SupportedFeatures.SOUND_ENHANCEMENT);
                    if((remoteSupportedFeatures & AudioGatewayServerManager.SUPPORTED_FEATURE_AG_VOICE_RECOGNITION) == AudioGatewayServerManager.SUPPORTED_FEATURE_AG_VOICE_RECOGNITION)
                        supportedFeatureSet.add(AudioGatewayServerManager.SupportedFeatures.VOICE_RECOGNITION);
                    if((remoteSupportedFeatures & AudioGatewayServerManager.SUPPORTED_FEATURE_INBAND_RINGING) == AudioGatewayServerManager.SUPPORTED_FEATURE_INBAND_RINGING)
                        supportedFeatureSet.add(AudioGatewayServerManager.SupportedFeatures.INBAND_RINGING);
                    if((remoteSupportedFeatures & AudioGatewayServerManager.SUPPORTED_FEATURE_VOICE_TAGS) == AudioGatewayServerManager.SUPPORTED_FEATURE_VOICE_TAGS)
                        supportedFeatureSet.add(AudioGatewayServerManager.SupportedFeatures.VOICE_TAGS);
                    if((remoteSupportedFeatures & AudioGatewayServerManager.SUPPORTED_FEATURE_REJECT_CALL) == AudioGatewayServerManager.SUPPORTED_FEATURE_REJECT_CALL)
                        supportedFeatureSet.add(AudioGatewayServerManager.SupportedFeatures.REJECT_CALL);
                    if((remoteSupportedFeatures & AudioGatewayServerManager.SUPPORTED_FEATURE_AG_ENHANCED_CALL_STATUS) == AudioGatewayServerManager.SUPPORTED_FEATURE_AG_ENHANCED_CALL_STATUS)
                        supportedFeatureSet.add(AudioGatewayServerManager.SupportedFeatures.ENHANCED_CALL_STATUS);
                    if((remoteSupportedFeatures & AudioGatewayServerManager.SUPPORTED_FEATURE_AG_ENHANCED_CALL_CONTROL) == AudioGatewayServerManager.SUPPORTED_FEATURE_AG_ENHANCED_CALL_CONTROL)
                        supportedFeatureSet.add(AudioGatewayServerManager.SupportedFeatures.ENHANCED_CALL_CONTROL);
                    if((remoteSupportedFeatures & AudioGatewayServerManager.SUPPORTED_FEATURE_AG_EXTENDED_ERROR_RESULT_CODES) == AudioGatewayServerManager.SUPPORTED_FEATURE_AG_EXTENDED_ERROR_RESULT_CODES)
                        supportedFeatureSet.add(AudioGatewayServerManager.SupportedFeatures.EXTENDED_ERROR_RESULT_CODES);

                    // FIXME Need to check local features for this, too
                    if(supportedFeatureSet.contains(AudioGatewayServerManager.SupportedFeatures.THREE_WAY_CALLING)) {
                        multipartyFeatureSet = EnumSet.noneOf(AudioGatewayServerManager.MultipartyFeatures.class);

                        if((remoteCallHoldMultipartySupport & AudioGatewayServerManager.MULTIPARTY_SUPPORT_RELEASE_ALL_HELD_CALLS) == AudioGatewayServerManager.MULTIPARTY_SUPPORT_RELEASE_ALL_HELD_CALLS)
                            multipartyFeatureSet.add(AudioGatewayServerManager.MultipartyFeatures.RELEASE_ALL_HELD_CALLS);
                        if((remoteCallHoldMultipartySupport & AudioGatewayServerManager.MULTIPARTY_SUPPORT_RELEASE_ALL_ACTIVE_CALLS_AND_ACCEPT_WAITING_CALL) == AudioGatewayServerManager.MULTIPARTY_SUPPORT_RELEASE_ALL_ACTIVE_CALLS_AND_ACCEPT_WAITING_CALL)
                            multipartyFeatureSet.add(AudioGatewayServerManager.MultipartyFeatures.RELEASE_ALL_ACTIVE_CALLS_AND_ACCEPT_WAITING_CALL);
                        if((remoteCallHoldMultipartySupport & AudioGatewayServerManager.MULTIPARTY_SUPPORT_PLACE_ALL_ACTIVE_CALLS_ON_HOLD_AND_ACCEPT_THE_OTHER) == AudioGatewayServerManager.MULTIPARTY_SUPPORT_PLACE_ALL_ACTIVE_CALLS_ON_HOLD_AND_ACCEPT_THE_OTHER)
                            multipartyFeatureSet.add(AudioGatewayServerManager.MultipartyFeatures.PLACE_ALL_ACTIVE_CALLS_ON_HOLD_AND_ACCEPT_THE_OTHER);
                        if((remoteCallHoldMultipartySupport & AudioGatewayServerManager.MULTIPARTY_SUPPORT_ADD_A_HELD_CALL_TO_CONVERSATION) == AudioGatewayServerManager.MULTIPARTY_SUPPORT_ADD_A_HELD_CALL_TO_CONVERSATION)
                            multipartyFeatureSet.add(AudioGatewayServerManager.MultipartyFeatures.ADD_A_HELD_CALL_TO_CONVERSATION);
                        if((remoteCallHoldMultipartySupport & AudioGatewayServerManager.MULTIPARTY_SUPPORT_CONNECT_TWO_CALLS_AND_DISCONNECT_SUBSCRIBER) == AudioGatewayServerManager.MULTIPARTY_SUPPORT_CONNECT_TWO_CALLS_AND_DISCONNECT_SUBSCRIBER)
                            multipartyFeatureSet.add(AudioGatewayServerManager.MultipartyFeatures.CONNECT_TWO_CALLS_AND_DISCONNECT_SUBSCRIBER);
                        if((remoteCallHoldMultipartySupport & AudioGatewayServerManager.MULTIPARTY_SUPPORT_RELEASE_SPECIFIED_ACTIVE_CALL_ONLY) == AudioGatewayServerManager.MULTIPARTY_SUPPORT_RELEASE_SPECIFIED_ACTIVE_CALL_ONLY)
                            multipartyFeatureSet.add(AudioGatewayServerManager.MultipartyFeatures.RELEASE_SPECIFIED_ACTIVE_CALL_ONLY);
                        if((remoteCallHoldMultipartySupport & AudioGatewayServerManager.MULTIPARTY_SUPPORT_REQUEST_PRIVATE_CONSULTATION_MODE) == AudioGatewayServerManager.MULTIPARTY_SUPPORT_REQUEST_PRIVATE_CONSULTATION_MODE)
                            multipartyFeatureSet.add(AudioGatewayServerManager.MultipartyFeatures.REQUEST_PRIVATE_CONSULTATION_MODE);
                    }
                }

                try {
                    ((HandsFreeEventCallback)callbackHandler).serviceLevelConnectionEstablishedEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), supportedFeatureSet, multipartyFeatureSet);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }


        /* This event is dispatched to a local Hands Free service when a */
        /* remote Audio Gateway device responds to a request for the current */
        /* Response/Hold status. The CallState member contains the call */
        /* state returned by the remote device. The ConnectionType member */
        /* identifies the local connection that is receiving the event. The */
        /* RemoteDeviceAddress member specifies the remote Bluetooth device */
        /* address of the remote Bluetooth device of the connection. The */
        /* CallState member contains the call state of the remote device. */
        @Override
        protected void incomingCallStateConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int callState) {
            CallState     state;
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                switch(callState) {
                case CALL_STATE_HOLD:
                    state = CallState.HOLD;
                    break;
                case CALL_STATE_ACCEPT:
                    state = CallState.ACCEPT;
                    break;
                case CALL_STATE_REJECT:
                    state = CallState.REJECT;
                    break;
                case CALL_STATE_NONE:
                default:
                    state = CallState.NONE;
                    break;
                }

                try {
                    ((HandsFreeEventCallback)callbackHandler).incomingCallStateConfirmationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), state);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Hands Free service */
        /* when a control undicator changes on the remote Audio Gateway and */
        /* control indicator change notification is enabled. The */
        /* ConnectionType member identifies the local connection that is */
        /* receiving the event. The RemoteDeviceAddress member specifies the */
        /* remote Bluetooth device address of the remote Bluetooth device of */
        /* the connection. The ControlIndicatorEntry member contains the */
        /* Indicator that has changed. */
        @Override
        protected void controlIndicatorStatusIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String name, boolean value) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((HandsFreeEventCallback)callbackHandler).controlIndicatorStatusIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), name, value);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Hands Free service */
        /* when a control undicator changes on the remote Audio Gateway and */
        /* control indicator change notification is enabled. The */
        /* ConnectionType member identifies the local connection that is */
        /* receiving the event. The RemoteDeviceAddress member specifies the */
        /* remote Bluetooth device address of the remote Bluetooth device of */
        /* the connection. The ControlIndicatorEntry member contains the */
        /* Indicator that has changed. */
        @Override
        protected void controlIndicatorStatusIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String name, int value, int rangeMinimum, int rangeMaximum) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((HandsFreeEventCallback)callbackHandler).controlIndicatorStatusIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), name, value, rangeMinimum, rangeMaximum);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Hands Free service */
        /* in response to an explicit request for the control indicator */
        /* status. The ConnectionType member identifies the local connection */
        /* that is receiving the event. The RemoteDeviceAddress member */
        /* specifies the remote Bluetooth device address of the remote */
        /* Bluetooth device of the connection. The ControlIndicatorEntry */
        /* member contains the Indicator that has changed. */
        @Override
        protected void controlIndicatorStatusConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String name, boolean value) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((HandsFreeEventCallback)callbackHandler).controlIndicatorStatusIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), name, value);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Hands Free service */
        /* in response to an explicit request for the control indicator */
        /* status. The ConnectionType member identifies the local connection */
        /* that is receiving the event. The RemoteDeviceAddress member */
        /* specifies the remote Bluetooth device address of the remote */
        /* Bluetooth device of the connection. The ControlIndicatorEntry */
        /* member contains the Indicator that has changed. */
        @Override
        protected void controlIndicatorStatusConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String name, int value, int rangeMinimum, int rangeMaximum) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((HandsFreeEventCallback)callbackHandler).controlIndicatorStatusIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), name, value, rangeMinimum, rangeMaximum);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Hands Free service */
        /* when the remote Audio Gateway responds to a request for the remote */
        /* call hold and multiparty supported features. The ConnectionType */
        /* and RemoteDeviceAddress members specify the local connection for */
        /* which the event is valid. The CallHoldSupportMaskValid member is */
        /* flag which specifies whether or not the CallHoldSupportMask member */
        /* is valid. */
        /* * NOTE * If the remote Audio Gateway does not have call hold and */
        /* multiparty support, the CallHoldSupportMaskValid member */
        /* will be FALSE. */
        @Override
        protected void callHoldMultipartySupportConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean callHoldSupportMaskValid, int callHoldSupportMask) {
            EnumSet<AudioGatewayServerManager.MultipartyFeatures> supportSet = null;
            EventCallback                                         callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                if(callHoldSupportMaskValid) {
                    supportSet = EnumSet.noneOf(AudioGatewayServerManager.MultipartyFeatures.class);

                    if((callHoldSupportMask & AudioGatewayServerManager.MULTIPARTY_SUPPORT_RELEASE_ALL_HELD_CALLS) == AudioGatewayServerManager.MULTIPARTY_SUPPORT_RELEASE_ALL_HELD_CALLS)
                        supportSet.add(AudioGatewayServerManager.MultipartyFeatures.RELEASE_ALL_HELD_CALLS);
                    if((callHoldSupportMask & AudioGatewayServerManager.MULTIPARTY_SUPPORT_RELEASE_ALL_ACTIVE_CALLS_AND_ACCEPT_WAITING_CALL) == AudioGatewayServerManager.MULTIPARTY_SUPPORT_RELEASE_ALL_ACTIVE_CALLS_AND_ACCEPT_WAITING_CALL)
                        supportSet.add(AudioGatewayServerManager.MultipartyFeatures.RELEASE_ALL_ACTIVE_CALLS_AND_ACCEPT_WAITING_CALL);
                    if((callHoldSupportMask & AudioGatewayServerManager.MULTIPARTY_SUPPORT_PLACE_ALL_ACTIVE_CALLS_ON_HOLD_AND_ACCEPT_THE_OTHER) == AudioGatewayServerManager.MULTIPARTY_SUPPORT_PLACE_ALL_ACTIVE_CALLS_ON_HOLD_AND_ACCEPT_THE_OTHER)
                        supportSet.add(AudioGatewayServerManager.MultipartyFeatures.PLACE_ALL_ACTIVE_CALLS_ON_HOLD_AND_ACCEPT_THE_OTHER);
                    if((callHoldSupportMask & AudioGatewayServerManager.MULTIPARTY_SUPPORT_ADD_A_HELD_CALL_TO_CONVERSATION) == AudioGatewayServerManager.MULTIPARTY_SUPPORT_ADD_A_HELD_CALL_TO_CONVERSATION)
                        supportSet.add(AudioGatewayServerManager.MultipartyFeatures.ADD_A_HELD_CALL_TO_CONVERSATION);
                    if((callHoldSupportMask & AudioGatewayServerManager.MULTIPARTY_SUPPORT_CONNECT_TWO_CALLS_AND_DISCONNECT_SUBSCRIBER) == AudioGatewayServerManager.MULTIPARTY_SUPPORT_CONNECT_TWO_CALLS_AND_DISCONNECT_SUBSCRIBER)
                        supportSet.add(AudioGatewayServerManager.MultipartyFeatures.CONNECT_TWO_CALLS_AND_DISCONNECT_SUBSCRIBER);
                    if((callHoldSupportMask & AudioGatewayServerManager.MULTIPARTY_SUPPORT_RELEASE_SPECIFIED_ACTIVE_CALL_ONLY) == AudioGatewayServerManager.MULTIPARTY_SUPPORT_RELEASE_SPECIFIED_ACTIVE_CALL_ONLY)
                        supportSet.add(AudioGatewayServerManager.MultipartyFeatures.RELEASE_SPECIFIED_ACTIVE_CALL_ONLY);
                    if((callHoldSupportMask & AudioGatewayServerManager.MULTIPARTY_SUPPORT_REQUEST_PRIVATE_CONSULTATION_MODE) == AudioGatewayServerManager.MULTIPARTY_SUPPORT_REQUEST_PRIVATE_CONSULTATION_MODE)
                        supportSet.add(AudioGatewayServerManager.MultipartyFeatures.REQUEST_PRIVATE_CONSULTATION_MODE);
                }

                try {
                    ((HandsFreeEventCallback)callbackHandler).callHoldMultipartySupportConfirmationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), supportSet);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Hands Free service */
        /* when the remote Audio Gateway receives a call while there is an */
        /* on-going call in progress. Note that call waiting notification */
        /* must be active in order to receive this event. The ConnectionType */
        /* and RemoteDeviceAddress members specify the local connection for */
        /* which the event is valid. The PhoneNumber member is a NULL */
        /* terminated ASCII string representing the phone number of the */
        /* waiting call. */
        @Override
        protected void callWaitingNotificationIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String phoneNumber) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((HandsFreeEventCallback)callbackHandler).callWaitingNotificationIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), phoneNumber);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Hands Free service */
        /* when the remote Audio Gateway receives a call line notification. */
        /* Note that call line identification notification must be active in */
        /* order to receive this event. The ConnectionType and */
        /* RemoteDeviceAddress members specify the local connection for which */
        /* the event is valid. The PhoneNumber member is a NULL terminated */
        /* ASCII string representing the phone number of the incoming call. */
        @Override
        protected void callLineIdentificationNotificationIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String phoneNumber) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((HandsFreeEventCallback)callbackHandler).callLineIdentificationNotificationIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), phoneNumber);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Hands Free unit when */
        /* the remote Audio Gateway sends a RING indication to the local */
        /* device. The ConnectionType and RemoteDeviceAddress members */
        /* specify the local connection which is receiving this indication. */
        @Override
        protected void ringIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((HandsFreeEventCallback)callbackHandler).ringIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Hands Free service */
        /* when the remote Audio Gateway wants to change the in-band ring */
        /* tone setting during an ongoing service level connection. The */
        /* ConnectionType and RemoteDeviceAddress members specify the local */
        /* connection which is receiving this indication. The Enabled member */
        /* specifies whether this is an indication that in-band ringing is */
        /* enabled (TRUE) or disabled (FALSE). */
        @Override
        protected void inBandRingToneSettingIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean enabled) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((HandsFreeEventCallback)callbackHandler).inBandRingToneSettingIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), enabled);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Hands Free service */
        /* when the remote Audio Gateway responds to a request for a phone */
        /* number to attach to a voice tag. The ConnectionType and */
        /* RemoteDeviceAddress members identify the connection receiving this */
        /* confirmation. The Success member specifies whether or not the */
        /* phone number was associated with a voice tag. The PhoneNumber */
        /* member is a NULL terminated ASCII string representing the phone */
        /* number that was attached to a voice tag. */
        @Override
        protected void voiceTagRequestConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String phoneNumber) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((HandsFreeEventCallback)callbackHandler).voiceTagRequestConfirmationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), phoneNumber);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Hands Free service */
        /* when a remote Audio gateway responds to a request for the list of */
        /* current calls. The ConnectionType and RemoteDeviceAddress members */
        /* identify the connection receiving the confirmation. The */
        /* CurrentCallList member is an array that contains */
        /* CurrentCallListLength entries each describing a single call. */
        @Override
        protected void currentCallsListConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int index, int callDirection, int callStatus, int callMode, boolean multiparty, String phoneNumber, int numberFormat) {
            CallDirection direction;
            CallStatus    status;
            CallMode      mode;
            EventCallback callbackHandler = this.callbackHandler;

            switch(callDirection) {
            case CALL_DIRECTION_MOBILE_ORIGINATED:
                direction = CallDirection.MOBILE_ORIGINATED;
                break;
            case CALL_DIRECTION_MOBILE_TERMINATED:
            default:
                direction = CallDirection.MOBILE_TERMINATED;
            }

            switch(callStatus) {
            case CALL_STATUS_ACTIVE:
                status = CallStatus.ACTIVE;
                break;
            case CALL_STATUS_HELD:
                status = CallStatus.HELD;
                break;
            case CALL_STATUS_DIALING:
                status = CallStatus.DIALING;
                break;
            case CALL_STATUS_ALERTING:
                status = CallStatus.ALERTING;
                break;
            case CALL_STATUS_INCOMING:
                status = CallStatus.INCOMING;
                break;
            case CALL_STATUS_WAITING:
            default:
                status = CallStatus.WAITING;
                break;
            }

            switch(callMode) {
            case CALL_MODE_VOICE:
                mode = CallMode.VOICE;
                break;
            case CALL_MODE_DATA:
                mode = CallMode.DATA;
                break;
            case CALL_MODE_FAX:
            default:
                mode = CallMode.FAX;
                break;
            }

            if(callbackHandler != null) {
                try {
                    ((HandsFreeEventCallback)callbackHandler).currentCallsListConfirmationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), new CurrentCallListEntry(index, direction, status, mode, multiparty, phoneNumber, numberFormat));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* This event is dispatched to a local Hands Free service when a */
        /* remote Audio Gateway responds to the request for current operator */
        /* selection. The ConnectionType and RemoteDeviceAddress members */
        /* identify the connection receiving the event. The NetworkMode */
        /* member contains the mode returned in the response. The */
        /* NetworkOperator member is a pointer to a NULL terminated ASCII */
        /* string that contains the returned operator name. */
        @Override
        protected void networkOperatorSelectionConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int networkMode, String networkOperator) {
            NetworkMode   mode;
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                switch(networkMode) {
                case NETWORK_MODE_MANUAL:
                    mode = NetworkMode.MANUAL;
                    break;
                case NETWORK_MODE_DEREGISTER:
                    mode = NetworkMode.DEREGISTER;
                    break;
                case NETWORK_MODE_SETONLY:
                    mode = NetworkMode.SETONLY;
                    break;
                case NETWORK_MODE_MANUAL_AUTO:
                    mode = NetworkMode.MANUAL_AUTO;
                    break;
                case NETWORK_MODE_AUTOMATIC:
                default:
                    mode = NetworkMode.AUTOMATIC;
                    break;
                }

                try {
                    ((HandsFreeEventCallback)callbackHandler).networkOperatorSelectionConfirmationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), mode, networkOperator);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* This event is dispatched to a local Hands Free service when a */
        /* remote Audio Gateway responds to a request for the current */
        /* subscriber number. The ConnectionType and RemoteDeviceAddress */
        /* members identify the connection receiving the confirmation. The */
        /* ServiceType member contains the service type value included in the */
        /* response. The NumberSbuscriberEntries member specifies the number */
        /* of Subscriber Number Information entries that are pointed to by */
        /* the SubscriberNumberList member. */
        @Override
        protected void subscriberNumberInformationConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int serviceType, int numberFormat, String phoneNumber) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((HandsFreeEventCallback)callbackHandler).subscriberNumberInformationConfirmationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), new SubscriberNumberInformation(serviceType, numberFormat, phoneNumber));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* This event is dispatched to a local Hands Free service when a */
        /* remote Audio Gateway device responds to a request for the current */
        /* Response/Hold status. The ConnectionType and RemoteDeviceAddress */
        /* members identify the connection receiving the event. The */
        /* CallState member contains the call state sent in the response. */
        @Override
        protected void responseHoldStatusConfirmationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int callState) {
            CallState     callStateEnum;
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                switch(callState) {
                case CALL_STATE_HOLD:
                    callStateEnum = CallState.HOLD;
                    break;
                case CALL_STATE_ACCEPT:
                    callStateEnum = CallState.ACCEPT;
                    break;
                case CALL_STATE_REJECT:
                    callStateEnum = CallState.REJECT;
                    break;
                case CALL_STATE_NONE:
                default:
                    callStateEnum = CallState.NONE;
                    break;
                }

                try {
                    ((HandsFreeEventCallback)callbackHandler).responseHoldStatusConfirmationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), callStateEnum);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Hands Free service */
        /* when the remote Audio Gateway responds to a commmand sent by the */
        /* Hands Free unit or generates an unsolicited result code. The */
        /* ConnectionType and RemoteDeviceAddress members identify the */
        /* connection receiving this indication. The ResultValue member */
        /* contains the actual result code value if the ResultType parameter */
        /* indicates that a result code is expected (erResultCode). */
        @Override
        protected void commandResultEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int resultType, int resultValue) {
            ExtendedResult type;
            EventCallback  callbackHandler = this.callbackHandler;

            switch(resultType) {
            case EXTENDED_RESULT_OK:
                type = ExtendedResult.OK;
                break;
            case EXTENDED_RESULT_ERROR:
                type = ExtendedResult.ERROR;
                break;
            case EXTENDED_RESULT_NOCARRIER:
                type = ExtendedResult.NO_CARRIER;
                break;
            case EXTENDED_RESULT_BUSY:
                type = ExtendedResult.BUSY;
                break;
            case EXTENDED_RESULT_NOANSWER:
                type = ExtendedResult.NO_ANSWER;
                break;
            case EXTENDED_RESULT_DELAYED:
                type = ExtendedResult.DELAYED;
                break;
            case EXTENDED_RESULT_BLACKLISTED:
                type = ExtendedResult.BLACKLISTED;
                break;
            case EXTENDED_RESULT_RESULTCODE:
            default:
                type = ExtendedResult.RESULT_CODE;
                break;
            }

            if(callbackHandler != null) {
                try {
                    ((HandsFreeEventCallback)callbackHandler).commandResultEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), type, resultValue);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Hands Free unit when */
        /* the remote Audio Gateway issues either a non-solicited arbitrary */
        /* response OR a solicited arbitrary response that is not recognized */
        /* by the local Hands Free unit (i.e. an arbitrary AT Response). */
        /* The ConnectionType and RemoteDeviceAddress members identify the */
        /* connection receiving this event. The ResponseData member is a */
        /* pointer to a NULL terminated ASCII string that represents the */
        /* actual response data that was received. */
        @Override
        protected void arbitraryResponseIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String responseData) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((HandsFreeEventCallback)callbackHandler).arbitraryResponseIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), responseData);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Receiver for event notifications which apply to a Hands-Free server.
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
        public interface HandsFreeEventCallback extends EventCallback {
            /**
             * Invoked when a Service Level Connection is established
             * successfully.
             * <p>
             * This event must be received before any other action on the port
             * is valid, except for sending arbitrary commands. If an error
             * occurs during the establishment of the service level connection
             * the connection will be closed and local device will receive a
             * disconnection event.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote
             *            device.
             * @param remoteSupportedFeatures Bit field indicating the supported
             *            features of the remote device. May be NULL if the
             *            remote Audio Gateway does not support sending
             *            supported feature flags.
             * @param remoteCallHoldMultipartySupport Indicates supported
             *            Multi-party and call-hold features. This field is only
             *            valid when both devices support Three-way Calling
             *            (that is, the local Hands-Free server supports
             *            {@link HandsFreeServerManager.SupportedFeatures#CALL_WAITING_THREE_WAY_CALLING}
             *            and {@code remoteSupportedFeatures} includes
             *            {@link AudioGatewayServerManager.SupportedFeatures#THREE_WAY_CALLING}
             *            .
             */
            public void serviceLevelConnectionEstablishedEvent(BluetoothAddress remoteDeviceAddress, EnumSet<AudioGatewayServerManager.SupportedFeatures> remoteSupportedFeatures, EnumSet<AudioGatewayServerManager.MultipartyFeatures> remoteCallHoldMultipartySupport);

            /**
             * Invoked when the remote Audio Gateway responds to a request for
             * the current Response/Hold status.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote Audio
             *            Gateway.
             * @param callState Current Response/Hold status according to the
             *            Audio Gateway.
             * @see HandsFreeServerManager#queryResponseHoldStatus
             */
            public void incomingCallStateConfirmationEvent(BluetoothAddress remoteDeviceAddress, CallState callState);

            /**
             * Invoked when a change to the value of a boolean-type Control
             * Indicator is reported by the remote Audio Gateway.
             * <p>
             * This event will be triggered only when Control Indicator Change
             * Notification is enabled by way of
             * {@link HandsFreeServerManager#enableRemoteIndicatorEventNotification}.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote Audio
             *            Gateway.
             * @param name Textual name of the indicator.
             * @param value New value of the indicator.
             */
            public void controlIndicatorStatusIndicationEvent(BluetoothAddress remoteDeviceAddress, String name, boolean value);

            /**
             * Invoked when a change to the value of a range-type Control
             * Indicator is reported by the remote Audio Gateway.
             * <p>
             * This event will be triggered only when Control Indicator Change
             * Notification is enabled by way of
             * {@link HandsFreeServerManager#enableRemoteIndicatorEventNotification}.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote Audio
             *            Gateway.
             * @param name Textual name of the indicator.
             * @param value New value of the indicator.
             * @param rangeMinimum Minimum allowed value for the indicator.
             * @param rangeMaximum Maximum allowed value for the indicator.
             */
            public void controlIndicatorStatusIndicationEvent(BluetoothAddress remoteDeviceAddress, String name, int value, int rangeMinimum, int rangeMaximum);

            /**
             * Invoked when the remote Audio Gateway responds to a request for
             * the current Control Indicator values.
             * <p>
             * This event is used to report range-type indicators. See
             * {@link #controlIndicatorStatusConfirmationEvent(BluetoothAddress, String, int, int, int)}
             * for range-type indicators.
             * <p>
             * Indicator values are reported in order of their index, one per
             * event. Because event notifications are serialized, notifications
             * for the two types of indicators are guaranteed to respect
             * indicator index ordering.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote Audio
             *            Gateway.
             * @param name Textual name of the indicator.
             * @param value New value of the indicator.
             * @see HandsFreeServerManager#queryRemoteControlIndicatorStatus
             */
            public void controlIndicatorStatusConfirmationEvent(BluetoothAddress remoteDeviceAddress, String name, boolean value);

            /**
             * Invoked when the remote Audio Gateway responds to a request for
             * the current Control Indicator values.
             * <p>
             * This event is used to report range-type indicators. See
             * {@link #controlIndicatorStatusConfirmationEvent(BluetoothAddress, String, boolean)}
             * for boolean-type indicators.
             * <p>
             * Indicator values are reported in order of their index, one per
             * event. Because event notifications are serialized, notifications
             * for the two types of indicators are guaranteed to respect
             * indicator index ordering.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote Audio
             *            Gateway.
             * @param name Textual name of the indicator.
             * @param value New value of the indicator.
             * @param rangeMinimum Minimum allowed value for the indicator.
             * @param rangeMaximum Maximum allowed value for the indicator.
             * @see HandsFreeServerManager#queryRemoteControlIndicatorStatus
             */
            public void controlIndicatorStatusConfirmationEvent(BluetoothAddress remoteDeviceAddress, String name, int value, int rangeMinimum, int rangeMaximum);

            /**
             * Invoked when the remote Audio Gateway responds to a request for
             * the remote Call Hold and Multi-party Supported Features.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote Audio
             *            Gateway.
             * @param callHoldSupportMask Bit flags indicating support for the
             *            various Call Hold and Multi-party features. Can be
             *            {@code null} if the remote Audio Gateway does not
             *            support Call Hold or Multi-party features.
             * @see HandsFreeServerManager#queryRemoteCallHoldingMultipartyServiceSupport
             */
            public void callHoldMultipartySupportConfirmationEvent(BluetoothAddress remoteDeviceAddress, EnumSet<AudioGatewayServerManager.MultipartyFeatures> callHoldSupportMask);

            /**
             * Invoked when the remote Audio Gateway receives a call while there
             * is an on-going call in progress.
             * <p>
             * This event will be triggered only when Call Waiting Notification
             * is enabled by way of
             * {@link HandsFreeServerManager#enableRemoteCallWaitingNotification}.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote Audio
             *            Gateway.
             * @param phoneNumber The phone number of the incoming call.
             */
            public void callWaitingNotificationIndicationEvent(BluetoothAddress remoteDeviceAddress, String phoneNumber);

            /**
             * Invoked when the remote Audio Gateway receives a Call Line
             * Notification.
             * <p>
             * This event will be triggered only when Call Line Notification is
             * enabled by way of
             * {@link HandsFreeServerManager#enableRemoteCallLineIdentificationNotification}.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote Audio
             *            Gateway.
             * @param phoneNumber the phone number of the incoming call.
             */
            public void callLineIdentificationNotificationIndicationEvent(BluetoothAddress remoteDeviceAddress, String phoneNumber);

            /**
             * Invoked when the remote Audio Gateway sends a RING indication to
             * the local Hands-Free device.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote Audio
             *            Gateway.
             */
            public void ringIndicationEvent(BluetoothAddress remoteDeviceAddress);

            /**
             * Invoked when the remote Audio Gateway wants to change the in-band
             * ring tone setting during an ongoing Service Level Connection.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote Audio
             *            Gateway.
             * @param enabled If {@code true}, in-band ringing is enabled.
             */
            public void inBandRingToneSettingIndicationEvent(BluetoothAddress remoteDeviceAddress, boolean enabled);

            /**
             * Invoked when the remote Audio Gateway responds to a request for a
             * phone number to attach to a voice tag.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote Audio
             *            Gateway.
             * @param phoneNumber The phone number provided by the remote Audio
             *            Gateway.
             * @see HandsFreeServerManager#voiceTagRequest
             */
            public void voiceTagRequestConfirmationEvent(BluetoothAddress remoteDeviceAddress, String phoneNumber);

            /**
             * Invoked when a remote Audio Gateway responds to a request for the
             * list of current calls.
             * <p>
             * Multiple events will be received, one for each in-progress call.
             * The sequence will be terminated by a Command Result event.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote Audio
             *            Gateway.
             * @param currentCall Details of the in-progress call.
             * @see HandsFreeServerManager#queryRemoteCurrentCallsList
             */
            public void currentCallsListConfirmationEvent(BluetoothAddress remoteDeviceAddress, CurrentCallListEntry currentCall);

            /**
             * Invoked when the remote Audio Gateway responds to a request for
             * the current Network Operator Selection.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote Audio
             *            Gateway.
             * @param mode Current operator selection mode.
             * @param operator Name of the network operator.
             * @see HandsFreeServerManager#queryRemoteNetworkOperatorSelection
             */
            public void networkOperatorSelectionConfirmationEvent(BluetoothAddress remoteDeviceAddress, NetworkMode mode, String operator);

            /**
             * Invoked when the remote Audio Gateway responds to a request for
             * the current subscriber number.
             * <p>
             * Multiple events will be received, one for each subscriber number.
             * The sequence will be terminated by a Command Result event.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote Audio
             *            Gateway.
             * @param subscriberNumber Subscriber phone number.
             * @see HandsFreeServerManager#querySubscriberNumberInformation
             */
            public void subscriberNumberInformationConfirmationEvent(BluetoothAddress remoteDeviceAddress, SubscriberNumberInformation subscriberNumber);

            /**
             * Invoked when the remote Audio Gateway device responds to a
             * request for the current Response/Hold status.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote Audio
             *            Gateway.
             * @param responseHoldStatus Current status.
             * @see HandsFreeServerManager#queryResponseHoldStatus
             */
            public void responseHoldStatusConfirmationEvent(BluetoothAddress remoteDeviceAddress, CallState responseHoldStatus);

            /**
             * Invoked when the remote Audio Gateway responds to a command sent
             * by the Hands Free unit or generates an unsolicited result code.
             * <p>
             * Successful command responses are terminated by a result of
             * {@code ExtendedResult.OK}. Other responses either indicate an
             * error or, in the case of {@code ExtendedResult.ResultCode}, a
             * detailed result status.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote Audio
             *            Gateway.
             * @param resultType Basic result of the operation.
             * @param resultValue Extended result code. Valid if
             *            {@code resultType} is
             *            {@code ExtendedResult.ResultCode}.
             */
            public void commandResultEvent(BluetoothAddress remoteDeviceAddress, ExtendedResult resultType, int resultValue);

            /**
             * Invoked when the remote Audio Gateway issues either a unsolicited
             * arbitrary response OR a solicited arbitrary response that is not
             * recognized by the local Hands Free unit (that is, an arbitrary AT
             * Response).
             *
             * @param remoteDeviceAddress Bluetooth address of the remote Audio
             *            Gateway.
             * @param responseData Raw AT response string.
             */
            public void arbitraryResponseIndicationEvent(BluetoothAddress remoteDeviceAddress, String responseData);
        }

    }

    /**
     * Management connection to the local Audio Gateway Server.
     */
    public static final class AudioGatewayServerManager extends HFRM {

        /* The following Bit Definitions represent the Audio Gateway Server */
        /* supported features. These Bit Definitions are used with the */
        /* HFRE_Open_Audio_Gateway_Server_Port() function to specify the */
        /* features supported by the server. */
        private final static int SUPPORTED_FEATURE_THREE_WAY_CALLING              = 0x00000001;
        private final static int SUPPORTED_FEATURE_AG_SOUND_ENHANCEMENT           = 0x00000002;
        private final static int SUPPORTED_FEATURE_AG_VOICE_RECOGNITION           = 0x00000004;
        private final static int SUPPORTED_FEATURE_INBAND_RINGING                 = 0x00000008;
        private final static int SUPPORTED_FEATURE_VOICE_TAGS                     = 0x00000010;
        private final static int SUPPORTED_FEATURE_REJECT_CALL                    = 0x00000020;
        private final static int SUPPORTED_FEATURE_AG_ENHANCED_CALL_STATUS        = 0x00000040;
        private final static int SUPPORTED_FEATURE_AG_ENHANCED_CALL_CONTROL       = 0x00000080;
        private final static int SUPPORTED_FEATURE_AG_EXTENDED_ERROR_RESULT_CODES = 0x00000100;

        /**
         * Features which an Audio Gateway device can optionally support.
         */
        public enum SupportedFeatures {
            /**
             * Support for multi-party calls.
             */
            THREE_WAY_CALLING,

            /**
             * Support for error correction and/or noice reduction.
             */
            SOUND_ENHANCEMENT,

            /**
             * Support for voice recognition.
             */
            VOICE_RECOGNITION,

            /**
             * Support for sending in-band ring tone notifications.
             */
            INBAND_RINGING,

            /**
             * Support for attaching a phone number to a voice tag.
             */
            VOICE_TAGS,

            /**
             * Support for rejecting incoming calls.
             */
            REJECT_CALL,

            /**
             * Support for sending enhanced call status notifications.
             */
            ENHANCED_CALL_STATUS,

            /**
             * Support for receiving enhanced call control commands (those
             * commands which include an index value).
             */
            ENHANCED_CALL_CONTROL,

            /**
             * Support for sending extended error result code notifications.
             */
            EXTENDED_ERROR_RESULT_CODES
        }

        private final static int MULTIPARTY_SUPPORT_RELEASE_ALL_HELD_CALLS                              = 0x00000001;
        private final static int MULTIPARTY_SUPPORT_RELEASE_ALL_ACTIVE_CALLS_AND_ACCEPT_WAITING_CALL    = 0x00000002;
        private final static int MULTIPARTY_SUPPORT_PLACE_ALL_ACTIVE_CALLS_ON_HOLD_AND_ACCEPT_THE_OTHER = 0x00000004;
        private final static int MULTIPARTY_SUPPORT_ADD_A_HELD_CALL_TO_CONVERSATION                     = 0x00000008;
        private final static int MULTIPARTY_SUPPORT_CONNECT_TWO_CALLS_AND_DISCONNECT_SUBSCRIBER         = 0x00000010;
        private final static int MULTIPARTY_SUPPORT_RELEASE_SPECIFIED_ACTIVE_CALL_ONLY                  = 0x00000020;
        private final static int MULTIPARTY_SUPPORT_REQUEST_PRIVATE_CONSULTATION_MODE                   = 0x00000040;

        /**
         * Call waiting and multi-party features which an Audio Gateway device
         * can optionally support.
         */
        public enum MultipartyFeatures {
            /**
             * Support for hanging-up all calls in the Hold state.
             */
            RELEASE_ALL_HELD_CALLS,

            /**
             * Support for hanging-up all active calls and accepting the
             * incoming call.
             */
            RELEASE_ALL_ACTIVE_CALLS_AND_ACCEPT_WAITING_CALL,

            /**
             * Support for putting all active calls on Hold and accepting the
             * incoming call.
             */
            PLACE_ALL_ACTIVE_CALLS_ON_HOLD_AND_ACCEPT_THE_OTHER,

            /**
             * Support for adding a held call to a multi-party conversation
             * (also known as a conference call).
             */
            ADD_A_HELD_CALL_TO_CONVERSATION,

            /**
             * Support for connecting the two active calls to each other and
             * hanging-up (also known as explicit call transfer).
             */
            CONNECT_TWO_CALLS_AND_DISCONNECT_SUBSCRIBER,

            /**
             * Support for hanging-up a specific call, given by index.
             */
            RELEASE_SPECIFIED_ACTIVE_CALL_ONLY,

            /**
             * Support for placing all calls on hold except for the specified
             * call, given by index.
             */
            REQUEST_PRIVATE_CONSULTATION_MODE,
        }

        /**
         * Local Hands-Free service configuration.
         *
         * @see #queryLocalConfiguration
         */
        public static final class LocalConfiguration {
            private final EnumSet<IncomingConnectionFlags> incomingConnectionFlags;
            private final EnumSet<SupportedFeatures>       supportedFeatures;
            private final EnumSet<MultipartyFeatures>      multipartyFeatures;
            private final long                             networkType;
            private final ConfigurationIndicatorEntry[]    additionalIndicatorList;

            /*pkg*/ LocalConfiguration(EnumSet<IncomingConnectionFlags> incomingConnectionFlags, EnumSet<SupportedFeatures> supportedFeatures, EnumSet<MultipartyFeatures> multipartyFeatures, int networkType, ConfigurationIndicatorEntry[] additionalIndicatorList) {
                this.incomingConnectionFlags = incomingConnectionFlags;
                this.supportedFeatures       = supportedFeatures;
                this.multipartyFeatures      = multipartyFeatures;
                this.networkType             = networkType;
                this.additionalIndicatorList = additionalIndicatorList;
            }

            public EnumSet<IncomingConnectionFlags> getIncomingConnectionFlags() {
                return incomingConnectionFlags;
            }

            public EnumSet<SupportedFeatures> getSupportedFeatures() {
                return supportedFeatures;
            }

            public EnumSet<MultipartyFeatures> getMultipartyFeatures() {
                return multipartyFeatures;
            }

            public long getNetworkType() {
                return networkType;
            }

            public ConfigurationIndicatorEntry[] getAdditionalIndicators() {
                return additionalIndicatorList;
            }
        }

        /**
         * Create a new connection to the local Audio Gateway Server.
         * <p>
         * Note that this is distinct from the Hands-Free Profile. This server
         * is specific to the role of a Audio Gateway device (such as a Phone).
         * This server supports connecting to remote Hands-Free servers
         * (typically, a headset or carkit or some other communication endpoint).
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

        /**
         * Retreive the current configuration of the local service.
         *
         * @return The configuration parameters of the local service or
         *         {@code null} if there was an error.
         **/
        public LocalConfiguration queryLocalConfiguration() {
            LocalConfiguration       config;
            LocalConfigurationNative nativeConfig;
            EnumSet<IncomingConnectionFlags> incomingConnectionFlags;
            EnumSet<SupportedFeatures>       supportedFeatureSet;
            EnumSet<MultipartyFeatures>      multipartyFeatureSet;

            nativeConfig = queryCurrentConfigurationNative(serverType);

            if(nativeConfig != null) {
                incomingConnectionFlags = EnumSet.noneOf(IncomingConnectionFlags.class);

                if((nativeConfig.incomingConnectionFlags & INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION) == INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION)
                    incomingConnectionFlags.add(IncomingConnectionFlags.REQUIRE_AUTHORIZATION);
                if((nativeConfig.incomingConnectionFlags & INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION) == INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION)
                    incomingConnectionFlags.add(IncomingConnectionFlags.REQUIRE_AUTHENTICATION);
                if((nativeConfig.incomingConnectionFlags & INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION) == INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION)
                    incomingConnectionFlags.add(IncomingConnectionFlags.REQUIRE_ENCRYPTION);

                supportedFeatureSet = EnumSet.noneOf(AudioGatewayServerManager.SupportedFeatures.class);

                if((nativeConfig.supportedFeaturesMask & AudioGatewayServerManager.SUPPORTED_FEATURE_THREE_WAY_CALLING) == AudioGatewayServerManager.SUPPORTED_FEATURE_THREE_WAY_CALLING)
                    supportedFeatureSet.add(AudioGatewayServerManager.SupportedFeatures.THREE_WAY_CALLING);
                if((nativeConfig.supportedFeaturesMask & AudioGatewayServerManager.SUPPORTED_FEATURE_AG_SOUND_ENHANCEMENT) == AudioGatewayServerManager.SUPPORTED_FEATURE_AG_SOUND_ENHANCEMENT)
                    supportedFeatureSet.add(AudioGatewayServerManager.SupportedFeatures.SOUND_ENHANCEMENT);
                if((nativeConfig.supportedFeaturesMask & AudioGatewayServerManager.SUPPORTED_FEATURE_AG_VOICE_RECOGNITION) == AudioGatewayServerManager.SUPPORTED_FEATURE_AG_VOICE_RECOGNITION)
                    supportedFeatureSet.add(AudioGatewayServerManager.SupportedFeatures.VOICE_RECOGNITION);
                if((nativeConfig.supportedFeaturesMask & AudioGatewayServerManager.SUPPORTED_FEATURE_INBAND_RINGING) == AudioGatewayServerManager.SUPPORTED_FEATURE_INBAND_RINGING)
                    supportedFeatureSet.add(AudioGatewayServerManager.SupportedFeatures.INBAND_RINGING);
                if((nativeConfig.supportedFeaturesMask & AudioGatewayServerManager.SUPPORTED_FEATURE_VOICE_TAGS) == AudioGatewayServerManager.SUPPORTED_FEATURE_VOICE_TAGS)
                    supportedFeatureSet.add(AudioGatewayServerManager.SupportedFeatures.VOICE_TAGS);
                if((nativeConfig.supportedFeaturesMask & AudioGatewayServerManager.SUPPORTED_FEATURE_REJECT_CALL) == AudioGatewayServerManager.SUPPORTED_FEATURE_REJECT_CALL)
                    supportedFeatureSet.add(AudioGatewayServerManager.SupportedFeatures.REJECT_CALL);
                if((nativeConfig.supportedFeaturesMask & AudioGatewayServerManager.SUPPORTED_FEATURE_AG_ENHANCED_CALL_STATUS) == AudioGatewayServerManager.SUPPORTED_FEATURE_AG_ENHANCED_CALL_STATUS)
                    supportedFeatureSet.add(AudioGatewayServerManager.SupportedFeatures.ENHANCED_CALL_STATUS);
                if((nativeConfig.supportedFeaturesMask & AudioGatewayServerManager.SUPPORTED_FEATURE_AG_ENHANCED_CALL_CONTROL) == AudioGatewayServerManager.SUPPORTED_FEATURE_AG_ENHANCED_CALL_CONTROL)
                    supportedFeatureSet.add(AudioGatewayServerManager.SupportedFeatures.ENHANCED_CALL_CONTROL);
                if((nativeConfig.supportedFeaturesMask & AudioGatewayServerManager.SUPPORTED_FEATURE_AG_EXTENDED_ERROR_RESULT_CODES) == AudioGatewayServerManager.SUPPORTED_FEATURE_AG_EXTENDED_ERROR_RESULT_CODES)
                    supportedFeatureSet.add(AudioGatewayServerManager.SupportedFeatures.EXTENDED_ERROR_RESULT_CODES);

                // FIXME Need to check local features for this, too
                if(supportedFeatureSet.contains(AudioGatewayServerManager.SupportedFeatures.THREE_WAY_CALLING)) {
                    multipartyFeatureSet = EnumSet.noneOf(AudioGatewayServerManager.MultipartyFeatures.class);

                    if((nativeConfig.callHoldingSupportMask & AudioGatewayServerManager.MULTIPARTY_SUPPORT_RELEASE_ALL_HELD_CALLS) == AudioGatewayServerManager.MULTIPARTY_SUPPORT_RELEASE_ALL_HELD_CALLS)
                        multipartyFeatureSet.add(AudioGatewayServerManager.MultipartyFeatures.RELEASE_ALL_HELD_CALLS);
                    if((nativeConfig.callHoldingSupportMask & AudioGatewayServerManager.MULTIPARTY_SUPPORT_RELEASE_ALL_ACTIVE_CALLS_AND_ACCEPT_WAITING_CALL) == AudioGatewayServerManager.MULTIPARTY_SUPPORT_RELEASE_ALL_ACTIVE_CALLS_AND_ACCEPT_WAITING_CALL)
                        multipartyFeatureSet.add(AudioGatewayServerManager.MultipartyFeatures.RELEASE_ALL_ACTIVE_CALLS_AND_ACCEPT_WAITING_CALL);
                    if((nativeConfig.callHoldingSupportMask & AudioGatewayServerManager.MULTIPARTY_SUPPORT_PLACE_ALL_ACTIVE_CALLS_ON_HOLD_AND_ACCEPT_THE_OTHER) == AudioGatewayServerManager.MULTIPARTY_SUPPORT_PLACE_ALL_ACTIVE_CALLS_ON_HOLD_AND_ACCEPT_THE_OTHER)
                        multipartyFeatureSet.add(AudioGatewayServerManager.MultipartyFeatures.PLACE_ALL_ACTIVE_CALLS_ON_HOLD_AND_ACCEPT_THE_OTHER);
                    if((nativeConfig.callHoldingSupportMask & AudioGatewayServerManager.MULTIPARTY_SUPPORT_ADD_A_HELD_CALL_TO_CONVERSATION) == AudioGatewayServerManager.MULTIPARTY_SUPPORT_ADD_A_HELD_CALL_TO_CONVERSATION)
                        multipartyFeatureSet.add(AudioGatewayServerManager.MultipartyFeatures.ADD_A_HELD_CALL_TO_CONVERSATION);
                    if((nativeConfig.callHoldingSupportMask & AudioGatewayServerManager.MULTIPARTY_SUPPORT_CONNECT_TWO_CALLS_AND_DISCONNECT_SUBSCRIBER) == AudioGatewayServerManager.MULTIPARTY_SUPPORT_CONNECT_TWO_CALLS_AND_DISCONNECT_SUBSCRIBER)
                        multipartyFeatureSet.add(AudioGatewayServerManager.MultipartyFeatures.CONNECT_TWO_CALLS_AND_DISCONNECT_SUBSCRIBER);
                    if((nativeConfig.callHoldingSupportMask & AudioGatewayServerManager.MULTIPARTY_SUPPORT_RELEASE_SPECIFIED_ACTIVE_CALL_ONLY) == AudioGatewayServerManager.MULTIPARTY_SUPPORT_RELEASE_SPECIFIED_ACTIVE_CALL_ONLY)
                        multipartyFeatureSet.add(AudioGatewayServerManager.MultipartyFeatures.RELEASE_SPECIFIED_ACTIVE_CALL_ONLY);
                    if((nativeConfig.callHoldingSupportMask & AudioGatewayServerManager.MULTIPARTY_SUPPORT_REQUEST_PRIVATE_CONSULTATION_MODE) == AudioGatewayServerManager.MULTIPARTY_SUPPORT_REQUEST_PRIVATE_CONSULTATION_MODE)
                        multipartyFeatureSet.add(AudioGatewayServerManager.MultipartyFeatures.REQUEST_PRIVATE_CONSULTATION_MODE);
                }
                else
                    multipartyFeatureSet = null;

                config = new LocalConfiguration(incomingConnectionFlags, supportedFeatureSet, multipartyFeatureSet, nativeConfig.networkType, nativeConfig.additionalIndicatorList);
            }
            else
                config = null;

            return config;
        }

        /* Audio Gateway Functions. */

        /**
         *  Update the current control indicator status.
         * <p>
         * Note This function may only be performed by
         * an Audio Gateway with a valid service level connection to a
         * connected remote Hands Free device. This call requires the manager to be
         * the controller.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Hands-Free device.
         * @param updateIndicators The list of indicators to update.
         *
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int updateCurrentControlIndicatorStatus(BluetoothAddress remoteDeviceAddress, ControlIndicatorUpdate[] updateIndicators) {
            byte [] address = remoteDeviceAddress.internalByteArray();

            String[] indicatorNames  = new String[updateIndicators.length];
            int[]    indicatorTypes  = new int[updateIndicators.length];
            int[]    indicatorValues = new int[updateIndicators.length];

            for(int i=0;i<updateIndicators.length;i++) {
                indicatorNames[i] = updateIndicators[i].indicatorName;

                switch(updateIndicators[i].getType()) {
                case BOOLEAN:
                    indicatorTypes[i]  = INDICATOR_TYPE_BOOLEAN;
                    indicatorValues[i] = ((IndicatorUpdateBoolean)updateIndicators[i]).getValue()?1:0;
                    break;
                case RANGE:
                    indicatorTypes[i]  = INDICATOR_TYPE_RANGE;
                    indicatorValues[i] = ((IndicatorUpdateRange)updateIndicators[i]).getValue();
                    break;
                default:
                    indicatorTypes[i]  = 0;
                    indicatorValues[i] = 0;
                }

            }

            return updateCurrentControlIndicatorStatusNative(address[0], address[1], address[2], address[3], address[4], address[5], indicatorNames, indicatorTypes, indicatorValues);
        }

        /**
         * Update a specific control indicator's status (range).
         * <p>
         * Note This function may only be performed by
         * an Audio Gateway with a valid service level connection to a
         * connected remote Hands Free device. This call requires the manager to be
         * the controller.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Hands-Free device.
         * @param indicatorName  The name of the indicator to be updated.
         * @param indicatorValue The new indicator value.
         *
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int updateCurrentControlIndicatorStatusByName(BluetoothAddress remoteDeviceAddress, String indicatorName, int indicatorValue) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return updateCurrentControlIndicatorStatusByNameNative(address[0], address[1], address[2], address[3], address[4], address[5], indicatorName, indicatorValue);
        }

        /**
         * Update a specific control indicator's status (boolean).
         * <p>
         * Note This function may only be performed by
         * an Audio Gateway with a valid service level connection to a
         * connected remote Hands Free device. This call requires the manager to be
         * the controller.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Hands-Free device.
         * @param indicatorName  The name of the indicator to be updated.
         * @param indicatorValue The new indicator value.
         *
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int updateCurrentControlIndicatorStatusByName(BluetoothAddress remoteDeviceAddress, String indicatorName, boolean indicatorValue) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return updateCurrentControlIndicatorStatusByNameNative(address[0], address[1], address[2], address[3], address[4], address[5], indicatorName, indicatorValue?1:0);
        }

        /* This function is responsible for sending a call waiting */
        /* notifications to a remote Hands Free device. This function may */
        /* only be performed by Audio Gateways which have call waiting */
        /* notification enabled and have a valid service level connection to */
        /* a connected remote Hands Free device. This function accepts as */
        /* its input parameter the phone number of the incoming call, if a */
        /* number is available. This parameter should be a pointer to a NULL */
        /* terminated ASCII string (if specified) and must have a length less */
        /* than: */
        /*                                                                   */
        /* HFRE_PHONE_NUMBER_LENGTH_MAXIMUM */
        /*                                                                   */
        /* This function returns zero if successful or a negative return */
        /* error code if there was an error. */
        /* * NOTE * It is valid to either pass a NULL for the PhoneNumber */
        /* parameter or a blank string to specify that there is no */
        /* phone number present. */

        /**
         * Send a call waiting notification.
         * <p>
         * Note This function may only be performed by
         * an Audio Gateway with a valid service level connection to a
         * connected remote Hands Free device and call waiting notification enabled.
         * This call requires the manager to be the controller.
         * <p>
         * It is valid for the {@code phoneNumber} parameter to contain
         * an empty string if there is no phone number present.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Hands-Free device.
         * @param phoneNumber The phone number of the incoming call.
         *
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int sendCallWaitingNotification(BluetoothAddress remoteDeviceAddress, String phoneNumber) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return sendCallWaitingNotificationNative(address[0], address[1], address[2], address[3], address[4], address[5], phoneNumber);
        }

        /* This function is responsible for sending call line identification */
        /* notifications to a remote Hands Free device. This function may */
        /* only be performed by Audio Gateways which have call line */
        /* identification notification enabled and have a valid service level */
        /* connection to a connected remote Hands Free device. This function */
        /* accepts as its input parameters the phone number of the incoming */
        /* call. This parameter should be a pointer to a NULL terminated */
        /* string and its length *MUST* be between the values of: */
        /*                                                                   */
        /* HFRE_PHONE_NUMBER_LENGTH_MINIMUM */
        /* HFRE_PHONE_NUMBER_LENGTH_MAXIMUM */
        /*                                                                   */
        /* This function return zero if successful or a negative return error */
        /* code if there was an error. */
        /**
         * Send a call line indentification notification.
         * <p>
         * Note This function may only be performed by
         * an Audio Gateway with a valid service level connection to a
         * connected remote Hands Free device and line identification notification enabled.
         * This call requires the manager to be the controller.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Hands-Free device.
         * @param phoneNumber The phone number of the incoming call.
         *
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int sendCallLineIdentificationNotification(BluetoothAddress remoteDeviceAddress, String phoneNumber) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return sendCallLineIdentificationNotificationNative(address[0], address[1], address[2], address[3], address[4], address[5], phoneNumber);
        }

        /* This function is responsible for sending a ring indication to a */
        /* remote Hands Free unit. This function may only be performed by */
        /* Audio Gateways for which a valid service level connection to a */
        /* connected remote Hands Free device exists. This function returns */
        /* zero if successful or a negative return error code if there was an */
        /* error. */
        /**
         * Send a ring indication.
         * <p>
         * Note This function may only be performed by
         * an Audio Gateway with a valid service level connection to a
         * connected remote Hands Free device. This call requires the
         * manager to be the controller.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Hands-Free device.
         *
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int ringIndication(BluetoothAddress remoteDeviceAddress) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return ringIndicationNative(address[0], address[1], address[2], address[3], address[4], address[5]);
        }

        /* This function is responsible for enabling or disabling in-band */
        /* ring tone Capabilities for a connected Hands Free device. This */
        /* function may only be performed by Audio Gateways for which a valid */
        /* service kevel connection exists. This function may only be used */
        /* to enable in-band ring tone capabilities if the local Audio */
        /* Gateway supports this feature. This function accepts as its input */
        /* parameter a BOOLEAN flag specifying if this is a call to Enable or */
        /* Disable this functionality. This function returns zero if */
        /* successful or a negative return error code if there was an error. */
        /**
         * Set the enable in band ring tone setting.
         * <p>
         * Note This function may only be performed by
         * an Audio Gateway with a valid service level connection to a
         * connected remote Hands Free device and the local Audio Gateway supports
         * in band ringing. This call requires the manager to be the controller.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Hands-Free device.
         * @param enableInBandRing Indicates whether to enable in band ringing.
         *
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int enableRemoteInBandRingToneSetting(BluetoothAddress remoteDeviceAddress, boolean enableInBandRing) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return enableRemoteInBandRingToneSettingNative(address[0], address[1], address[2], address[3], address[4], address[5], enableInBandRing);
        }

        /* This function is responsible for responding to a request that was */
        /* received for a phone number to be associated with a unique voice */
        /* tag by a remote Hands Free device. This function may only be */
        /* performed by Audio Gateways that have received a voice tag request */
        /* Indication. This function accepts as its input parameter the */
        /* phone number to be associated with the voice tag. If the request */
        /* is accepted, the phone number Parameter string length *MUST* be */
        /* between the values: */
        /*                                                                   */
        /* HFRE_PHONE_NUMBER_LENGTH_MINIMUM */
        /* HFRE_PHONE_NUMBER_LENGTH_MAXIMUM */
        /*                                                                   */
        /* If the caller wishes to reject the request, the phone number */
        /* parameter should be set to NULL to indicate this. This function */
        /* returns zero if successful or a negative return error code if */
        /* there was an error. */
        /**
         * Send a voice tag response.
         * <p>
         * Note This function may only be performed by
         * an Audio Gateway with a valid service level connection to a
         * connected remote Hands Free device and that has received a
         * voice tag request. This call requires the manager to be the controller.
         * <p>
         * If the caller wished to reject the request, the phoneNumber should be NULL.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Hands-Free device.
         * @param phoneNumber The phone number of the incoming call.
         *
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int voiceTagResponse(BluetoothAddress remoteDeviceAddress, String phoneNumber) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return voiceTagResponseNative(address[0], address[1], address[2], address[3], address[4], address[5], phoneNumber);
        }

        /* The following function is responsible for sending the current */
        /* calls list entries to a remote Hands Free device. This function */
        /* may only be performed by Audio Gateways that have received a */
        /* request to query the remote current calls list. This function */
        /* accepts as its input parameters the list of current call entries */
        /* to be sent and length of the list. This function returns zero if */
        /* successful or a negative return error code if there was an error. */
        /**
         * Send the current call list.
         * <p>
         * Note This function may only be performed by
         * an Audio Gateway with a valid service level connection to a
         * connected remote Hands Free device and that has received a request
         * to query the remote current calls. This call requires the manager
         * to be the controller.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Hands-Free device.
         * @param currentCallList The list of current call entries to be sent.
         *
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int sendCurrentCallsList(BluetoothAddress remoteDeviceAddress, CurrentCallListEntry[] currentCallList) {
            byte[] address = remoteDeviceAddress.internalByteArray();

            int[] indices          = new int[currentCallList.length];
            int[] callDirections   = new int[currentCallList.length];
            int[] callStatuses     = new int[currentCallList.length];
            int[] callModes        = new int[currentCallList.length];
            boolean[] multiparties = new boolean[currentCallList.length];
            String[] phoneNumbers  = new String[currentCallList.length];
            int[] numberFormats    = new int[currentCallList.length];

            for(int i=0;i<currentCallList.length;i++) {
                indices[i] = currentCallList[i].index;

                switch(currentCallList[i].callDirection) {
                case MOBILE_ORIGINATED:
                    callDirections[i] = CALL_DIRECTION_MOBILE_ORIGINATED;
                    break;
                case MOBILE_TERMINATED:
                    callDirections[i] = CALL_DIRECTION_MOBILE_TERMINATED;
                    break;
                default:
                    callDirections[i] = 0;
                }

                switch(currentCallList[i].callStatus) {
                case ACTIVE:
                    callStatuses[i] = CALL_STATUS_ACTIVE;
                    break;
                case HELD:
                    callStatuses[i] = CALL_STATUS_HELD;
                    break;
                case DIALING:
                    callStatuses[i] = CALL_STATUS_DIALING;
                    break;
                case ALERTING:
                    callStatuses[i] = CALL_STATUS_ALERTING;
                    break;
                case INCOMING:
                    callStatuses[i] = CALL_STATUS_INCOMING;
                    break;
                case WAITING:
                    callStatuses[i] = CALL_STATUS_WAITING;
                    break;
                default:
                    callStatuses[i] = 0;
                }

                switch(currentCallList[i].callMode) {
                case VOICE:
                    callModes[i] = CALL_MODE_VOICE;
                    break;
                case DATA:
                    callModes[i] = CALL_MODE_DATA;
                    break;
                case FAX:
                    callModes[i] = CALL_MODE_FAX;
                    break;
                default:
                    callModes[i] = 0;
                }

                multiparties[i]  = currentCallList[i].multiparty;
                phoneNumbers[i]  = currentCallList[i].phoneNumber;
                numberFormats[i] = currentCallList[i].numberFormat;
            }

            return sendCurrentCallsListNative(address[0], address[1], address[2], address[3], address[4], address[5], indices, callDirections, callStatuses, callModes, multiparties, phoneNumbers, numberFormats);
        }

        /* The following function is responsible for sending the network */
        /* operator. This function may only be performed by Audio Gateways */
        /* that have received a request to query the remote network operator */
        /* selection. This function accepts as input the current network */
        /* mode and the current network operator. The network operator */
        /* should be expressed as a NULL terminated ASCII string (if */
        /* specified) and must have a length less than: */
        /*                                                                   */
        /* HFRE_NETWORK_OPERATOR_LENGTH_MAXIMUM */
        /*                                                                   */
        /* This function returns zero if successful or a negative return */
        /* error code if there was an error. */
        /* * NOTE * It is valid to either pass a NULL for the NetworkOperator */
        /* parameter or a blank string to specify that there is no */
        /* network operator present. */
        /**
         * Send the network operator selection.
         * <p>
         * Note This function may only be performed by
         * an Audio Gateway with a valid service level connection to a
         * connected remote Hands Free device and has received a request
         * to query the remote network operator selection. This call requires
         * the manager to be the controller.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Hands-Free device.
         * @param networkMode The current network mode.
         * @param networkOperator The current network operator.
         *
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int sendNetworkOperatorSelection(BluetoothAddress remoteDeviceAddress, int networkMode, String networkOperator) {
            byte[] address = remoteDeviceAddress.internalByteArray();
            return sendNetworkOperatorSelectionNative(address[0], address[1], address[2], address[3], address[4], address[5], networkMode, networkOperator);
        }

        /* The following function is responsible for sending extended error */
        /* results. This function may only be performed by an Audio Gateway */
        /* with a valid service level connection. This function accepts as */
        /* its input parameter the result code to send as part of the error */
        /* message. This function returns zero if successful or a negative */
        /* return error code if there was an error. */
        /**
         * Send an extended error result.
         * <p>
         * Note This function may only be performed by
         * an Audio Gateway with a valid service level connection to a
         * connected remote Hands Free device. This call requires
         * the manager to be the controller.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Hands-Free device.
         * @param resultCode The result code to send with the error message.
         *
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int sendExtendedErrorResult(BluetoothAddress remoteDeviceAddress, int resultCode) {
            //xxx Check whether resultcode can be > SINT_MAX
            byte[] address = remoteDeviceAddress.internalByteArray();
            return sendExtendedErrorResultNative(address[0], address[1], address[2], address[3], address[4], address[5], resultCode);
        }

        /* The following function is responsible for sending subscriber */
        /* number information. This function may only be performed by an */
        /* Audio Gateway that has received a request to query the subscriber */
        /* number information. This function accepts as its input parameters */
        /* the number of subscribers followed by a list of subscriber */
        /* numbers. This function returns zero if successful or a negative */
        /* return error code if there was an error. */
        /**
         * Send subscriber number information.
         * <p>
         * Note This function may only be performed by
         * an Audio Gateway with a valid service level connection to a
         * connected remote Hands Free device and that has received a
         * to query the subscriber number information. This call requires
         * the manager to be the controller.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Hands-Free device.
         * @param subscriberNumberList The list of subscriber number information.
         *
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int sendSubscriberNumberInformation(BluetoothAddress remoteDeviceAddress, SubscriberNumberInformation[] subscriberNumberList) {
            byte[] address = remoteDeviceAddress.internalByteArray();

            int[] serviceTypes    = new int[subscriberNumberList.length];
            int[] numberFormats   = new int[subscriberNumberList.length];
            String[] phoneNumbers = new String[subscriberNumberList.length];

            for(int i=0;i<subscriberNumberList.length;i++) {
                serviceTypes[i]  = subscriberNumberList[i].serviceType;
                numberFormats[i] = subscriberNumberList[i].numberFormat;
                phoneNumbers[i]  = subscriberNumberList[i].phoneNumber;
            }

            return sendSubscriberNumberInformationNative(address[0], address[1], address[2], address[3], address[4], address[5], serviceTypes, numberFormats, phoneNumbers);
        }

        /* The following function is responsible for sending information */
        /* about the incoming call state. This function may only be */
        /* performed by an Audio Gateway that has a valid service level */
        /* connection to a remote Hands Free device. This function accepts */
        /* as its input parameter the call state to set as part of this */
        /* message. This function returns zero if successful or a negative */
        /* return error code if there was an error. */
        /**
         * Send the incoming call state.
         * <p>
         * Note This function may only be performed by
         * an Audio Gateway with a valid service level connection to a
         * connected remote Hands Free device. This call requires
         * the manager to be the controller.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Hands-Free device.
         * @param callState The call state to send.
         *
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int sendIncomingCallState(BluetoothAddress remoteDeviceAddress, CallState callState) {
            int callStateValue;

            byte[] address = remoteDeviceAddress.internalByteArray();

            switch(callState) {
            case HOLD:
                callStateValue = CALL_STATE_HOLD;
                break;
            case ACCEPT:
                callStateValue = CALL_STATE_ACCEPT;
                break;
            case REJECT:
                callStateValue = CALL_STATE_REJECT;
                break;
            case NONE:
                callStateValue = CALL_STATE_NONE;
                break;
            default:
                callStateValue = CALL_STATE_NONE;
            }
            return sendIncomingCallStateNative(address[0], address[1], address[2], address[3], address[4], address[5], callStateValue);
        }

        /* The following function is responsible for sending a terminating */
        /* response code from an Audio Gateway to a remote Hands Free device. */
        /* This function may only be performed by an Audio Gateway that has a */
        /* valid service level connection to a remote Hands Free device. */
        /* This function can be called in any context where a normal Audio */
        /* Gateway response function is called if the intention is to */
        /* generate an error in response to the request. It also must be */
        /* called after certain requests that previously automatically */
        /* generated an OK response. In general, either this function or an */
        /* explicit response must be called after each request to the Audio */
        /* Gateway. This function accepts as its input parameters the type */
        /* of result to return in the terminating response and, if the result */
        /* type indicates an extended error code value, the error code. This */
        /* function returns zero if successful or a negative return error */
        /* code if there was an error. */
        /**
         * Sends a terminating response code.
         * <p>
         * Note This function may only be performed by
         * an Audio Gateway with a valid service level connection to a
         * connected remote Hands Free device. This call requires
         * the manager to be the controller.
         * <p>
         * This function can be called in any context where a normal Audio
         * Gateway response function is called if the intention is to
         * generate an error in response to the request. It also must be
         * called after certain requests that previously automatically
         * generated an OK response. In general, either this function or
         * an explicit response must be called after each request to the
         * Audio Gateway.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Hands-Free device.
         * @param resultType The result type to be sent.
         * @param resultValue The error code to send of the result type indicated an extended error.
         *
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int sendTerminatingResponse(BluetoothAddress remoteDeviceAddress, ExtendedResult resultType, int resultValue) {
            //xxx Check whether resultValue can be > SINT_MAX
            int result      = 0;
            byte [] address = remoteDeviceAddress.internalByteArray();

            switch(resultType) {
            case OK:
                result = EXTENDED_RESULT_OK;
                break;
            case ERROR:
                result = EXTENDED_RESULT_ERROR;
                break;
            case NO_CARRIER:
                result = EXTENDED_RESULT_NOCARRIER;
                break;
            case BUSY:
                result = EXTENDED_RESULT_BUSY;
                break;
            case NO_ANSWER:
                result = EXTENDED_RESULT_NOANSWER;
                break;
            case DELAYED:
                result = EXTENDED_RESULT_DELAYED;
                break;
            case BLACKLISTED:
                result = EXTENDED_RESULT_BLACKLISTED;
                break;
            case RESULT_CODE:
                result = EXTENDED_RESULT_RESULTCODE;
                break;
            }

            return sendTerminatingResponseNative(address[0], address[1], address[2], address[3], address[4], address[5], result, resultValue);
        }

        /* The following function is responsible for enabling the processing */
        /* of arbitrary commands from a remote Hands Free device. Once this */
        /* function is called the hetHFRArbitraryCommandIndication event will */
        /* be dispatched when an arbitrary command is received (i.e. a non */
        /* Hands Free profile command). If this function is not called, the */
        /* Audio Gateway will silently respond to any arbitrary commands with */
        /* an error response ("ERROR"). If support is enabled, then the */
        /* caller is responsible for responding TO ALL arbitrary command */
        /* indications (hetHFRArbitraryCommandIndication). If the arbitrary */
        /* command is not supported, then the caller should simply respond */
        /* with: */
        /*                                                                   */
        /* HFRM_Send_Terminating_Response() */
        /*                                                                   */
        /* specifying the erError response. This function returns zero if */
        /* successful or a negative return error code if there was an error. */
        /* * NOTE * Once arbitrary command processing is enabled for an */
        /* Audio Gateway it cannot be disabled. */
        /* * NOTE * The default value is disabled (i.e. the */
        /* hetHFRArbitraryCommandIndication will NEVER be dispatched */
        /* and the Audio Gateway will always respond with an error */
        /* response ("ERROR") when an arbitrary command is received. */
        /* * NOTE * If support is enabled, the caller is guaranteed that a */
        /* hetHFRArbitraryCommandIndication will NOT be dispatched */
        /* before a service level indication is present. If an */
        /* arbitrary command is received, it will be responded with */
        /* silently with an error response ("ERROR"). */
        /* * NOTE * This function is not applicable to Hands Free devices, */
        /* as Hands Free devices will always receive the */
        /* hetHFRArbitraryResponseIndication. No action is required */
        /* and the event can simply be ignored. */
        /**
         * Enable arbitrary command processing.
         * <p>
         * Note This function may only be performed by
         * an Audio Gateway with a valid service level connection to a
         * connected remote Hands Free device. This call requires
         * the manager to be the controller.
         * <p>
         * If this function is not called, all arbitrary commands will be
         * silently responded to with an error. Once this function is called,
         * arbitrary commands cannot be disabled. The caller will be responsible
         * for handling all arbitrary commands. Any unsupported commands can be
         * responded to with a call to {@link #sendTerminatingResponse} using the
         * {@link ExtendedResult#ERROR} result type.
         *
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int enableArbitraryCommandProcessing() {
            return enableArbitraryCommandProcessingNative();
        }

        /* The following function is responsible for sending an arbitrary    */
        /* response to the remote Hands Free device (i.e. non Bluetooth      */
        /* Hands Free Profile response) - either solicited or non-solicited. */
        /* This function may only be performed by an Audio Gateway with a    */
        /* valid service level connection. This function accepts as its      */
        /* input parameter a NULL terminated ASCII string that represents    */
        /* the arbitrary response to send. This function returns zero if     */
        /* successful or a negative return error code if there was an error. */
        /* * NOTE * The Response string passed to this function *MUST* begin */
        /*          with a carriage return/line feed ("\r\n").               */
        /**
         * Sends an arbitrary response.
         * <p>
         * Note This function may only be performed by
         * an Audio Gateway with a valid service level connection to a
         * connected remote Hands Free device. This call requires
         * the manager to be the controller.
         * <p>
         * The {@code arbitraryResponse} parameter must begun with a
         * carriage return/line feed ("\r\n").
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Hands-Free device.
         * @param arbitraryResponse The String to be sent as the response.
         *
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int sendArbitraryResponse(BluetoothAddress remoteDeviceAddress, String arbitraryResponse) {
            byte [] address = remoteDeviceAddress.internalByteArray();
            return sendArbitraryResponseNative(address[0], address[1], address[2], address[3], address[4], address[5], arbitraryResponse);
        }

        /* Hands Free Manager Audio Connection Management Functions. */

        /* This function is responsible for setting up an audio connection */
        /* between the local and remote device. This function may be used by */
        /* either an Audio Gateway or a Hands Free device for which a valid */
        /* service level connection Exists. This function accepts as its */
        /* input parameter the connection type indicating which connection */
        /* will process the command. This function returns zero if */
        /* successful or a negative return error code if there was an error. */
        /**
         * Sets up an audio connection between the local Audio gateway
         * and the remote Hands-Free device.
         * <p>
         * Note This function may only be performed by
         * an Audio Gateway with a valid service level connection to a
         * connected remote Hands Free device. This call requires
         * the manager to be the controller.
         *
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Hands-Free device.
         *
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int setupAudioConnection(BluetoothAddress remoteDeviceAddress) {
            byte [] address = remoteDeviceAddress.internalByteArray();
            return setupAudioConnectionNative(SERVER_TYPE_AUDIOGATEWAY, address[0], address[1], address[2], address[3], address[4], address[5]);
        }

        /* This function is responsible for releasing an audio connection */
        /* which was previously established by the remote device or by a */
        /* successful call to the HFRM_Setup_Audio_Connection() function. */
        /* This function may be used by either an Audio Gateway or a Hands */
        /* Free device. This function returns zero if successful or a */
        /* negative return error code if there was an error. */
        /**
         * Releases a previously set up audio connection between the
         * local Audio Gateway and the remote Hands-Free device.
         * <p>
         * Note This function may only be performed by
         * an Audio Gateway with a valid service level connection to a
         * connected remote Hands Free device. This call requires
         * the manager to be the controller.
         *
         * @param remoteDeviceAddress Bluetooth address of the remote Hands-Free device.
         *
         * @return Zero if successful or a negative return error code if there
         *         was an error.
         */
        public int releaseAudioConnection(BluetoothAddress remoteDeviceAddress) {
            byte [] address = remoteDeviceAddress.internalByteArray();
            return releaseAudioConnectionNative(SERVER_TYPE_AUDIOGATEWAY, address[0], address[1], address[2], address[3], address[4], address[5]);
        }

        /* The following event acts as an indication to inform the current */
        /* state of the service level connection. The ConnectionType member */
        /* identifies the connection type to which this event applies. The */
        /* RemoteSupportedFeaturesValid member specifies whether or not the */
        /* Remote Support Features member is valid. The */
        /* RemoteSupportedFeatures member specifies supported features which */
        /* are supported by the remote device. The */
        /* RemoteCallHoldMultipartySupported member specifies the support of */
        /* this feature by the remote device. This indication must be */
        /* received before performing any other action on this port. If an */
        /* error occurs during the establishment of the service level */
        /* connection the connection will be closed and local device will */
        /* receive a disconnection event. */
        /* * NOTE * The RemoteCallHoldMultipartySupport member will only be */
        /* valid if the local and remote device both have the */
        /* "Three-way Calling Support" bit set in their supported */
        /* features. */
        /* * NOTE * The RemoteCall Hold Multipary Support member will always */
        /* be set to */
        /* HFRE_CALL_HOLD_MULTIPARTY_SUPPORTED_FEATURES_ERROR in the */
        /* case when this indication is received by an audio gateway */
        /* as Hands Free units have no call hold multiparty */
        /* supported features to query. */
        @Override
        protected void serviceLevelConnectionEstablishedEvent(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean remoteSupportedFeaturesValid, int remoteSupportedFeatures, int remoteCallHoldMultipartySupport) {
            EnumSet<HandsFreeServerManager.SupportedFeatures> supportedFeatureSet = null;
            EventCallback                                     callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                if(remoteSupportedFeaturesValid) {
                    supportedFeatureSet = EnumSet.noneOf(HandsFreeServerManager.SupportedFeatures.class);

                    if((remoteSupportedFeatures & HandsFreeServerManager.SUPPORTED_FEATURE_CALL_WAITING_THREE_WAY_CALLING) == HandsFreeServerManager.SUPPORTED_FEATURE_CALL_WAITING_THREE_WAY_CALLING)
                        supportedFeatureSet.add(HandsFreeServerManager.SupportedFeatures.CALL_WAITING_THREE_WAY_CALLING);
                    if((remoteSupportedFeatures & HandsFreeServerManager.SUPPORTED_FEATURE_CLI) == HandsFreeServerManager.SUPPORTED_FEATURE_CLI)
                        supportedFeatureSet.add(HandsFreeServerManager.SupportedFeatures.CLI);
                    if((remoteSupportedFeatures & HandsFreeServerManager.SUPPORTED_FEATURE_ENHANCED_CALL_CONTROL) == HandsFreeServerManager.SUPPORTED_FEATURE_ENHANCED_CALL_CONTROL)
                        supportedFeatureSet.add(HandsFreeServerManager.SupportedFeatures.ENHANCED_CALL_CONTROL);
                    if((remoteSupportedFeatures & HandsFreeServerManager.SUPPORTED_FEATURE_ENHANCED_CALL_STATUS) == HandsFreeServerManager.SUPPORTED_FEATURE_ENHANCED_CALL_STATUS)
                        supportedFeatureSet.add(HandsFreeServerManager.SupportedFeatures.ENHANCED_CALL_STATUS);
                    if((remoteSupportedFeatures & HandsFreeServerManager.SUPPORTED_FEATURE_REMOTE_VOLUME_CONTROL) == HandsFreeServerManager.SUPPORTED_FEATURE_REMOTE_VOLUME_CONTROL)
                        supportedFeatureSet.add(HandsFreeServerManager.SupportedFeatures.REMOTE_VOLUME_CONTROL);
                    if((remoteSupportedFeatures & HandsFreeServerManager.SUPPORTED_FEATURE_SOUND_ENHANCEMENT) == HandsFreeServerManager.SUPPORTED_FEATURE_SOUND_ENHANCEMENT)
                        supportedFeatureSet.add(HandsFreeServerManager.SupportedFeatures.SOUND_ENHANCEMENT);
                    if((remoteSupportedFeatures & HandsFreeServerManager.SUPPORTED_FEATURE_VOICE_RECOGNITION) == HandsFreeServerManager.SUPPORTED_FEATURE_VOICE_RECOGNITION)
                        supportedFeatureSet.add(HandsFreeServerManager.SupportedFeatures.VOICE_RECOGNITION);

                }

                try {
                    ((AudioGatewayEventCallback)callbackHandler).serviceLevelConnectionEstablishedEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), supportedFeatureSet);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Audio Gateway when */
        /* the remote Hands Free service responds to a call waiting */
        /* notification by selecting how to handle the waiting call. The */
        /* ConnectionType and RemoteDeviceAddress members identify the local */
        /* service receiving this indication. The CallHoldMultipartyHandling */
        /* member specifies the requested action to take regarding the */
        /* waiting call. If the CallHoldMultipartyHandling member indicates */
        /* an extended type then the Index member will contain the call index */
        /* to which the operation refers. */
        @Override
        protected void callHoldMultipartySelectionIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int handlingType, int index) {
            CallHoldMultipartyHandlingType callHoldMultipartyHandling;
            EventCallback                  callbackHandler = this.callbackHandler;

            switch(handlingType) {
            case MULTIPARTY_HANDLING_RELEASE_ALL_HELD_CALLS:
                callHoldMultipartyHandling = CallHoldMultipartyHandlingType.RELEASE_ALL_HELD_CALLS;
                break;
            case MULTIPARTY_HANDLING_RELEASE_ALL_ACTIVE_CALLS_ACCEPT_WAITING_CALL:
                callHoldMultipartyHandling = CallHoldMultipartyHandlingType.RELEASE_ALL_ACTIVE_CALLS_ACCEPT_WAITING_CALL;
                break;
            case MULTIPARTY_HANDLING_PLACE_ALL_ACTIVE_CALLS_ON_HOLD_ACCEPT_THE_OTHER:
                callHoldMultipartyHandling = CallHoldMultipartyHandlingType.PLACE_ALL_ACTIVE_CALLS_ON_HOLD_ACCEPT_THE_OTHER;
                break;
            case MULTIPARTY_HANDLING_ADD_A_HELD_CALL_TO_CONVERSATION:
                callHoldMultipartyHandling = CallHoldMultipartyHandlingType.ADD_A_HELD_CALL_TO_CONVERSATION;
                break;
            case MULTIPARTY_HANDLING_CONNECT_TWO_CALLS_AND_DISCONNECT:
                callHoldMultipartyHandling = CallHoldMultipartyHandlingType.CONNECT_TWO_CALLS_AND_DISCONNECT;
                break;
            case MULTIPARTY_HANDLING_RELEASE_SPECIFIED_CALL_INDEX:
                callHoldMultipartyHandling = CallHoldMultipartyHandlingType.RELEASE_SPECIFIED_CALL_INDEX;
                break;
            default:
                callHoldMultipartyHandling = CallHoldMultipartyHandlingType.PRIVATE_CONSULTATION_MODE;
                break;
            }

            if(callbackHandler != null) {
                try {
                    ((AudioGatewayEventCallback)callbackHandler).callHoldMultipartySelectionIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), callHoldMultipartyHandling, index);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Audio Gateway when */
        /* the remote Hands Free device issues the command to enable or */
        /* disable call waiting notification. The ConnectionType and */
        /* RemoteDeviceAddress members identify the local service receiving */
        /* this indication. The Enabled member specifies whether this is an */
        /* indication to enable (TRUE) or disable (FALSE) call waiting */
        /* notification. */
        @Override
        protected void callWaitingNotificationActivationIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean enabled) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((AudioGatewayEventCallback)callbackHandler).callWaitingNotificationActivationIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), enabled);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Audio Gateway when */
        /* the remote Hands Free device issues the command to enable or */
        /* disable call line identification notification. The ConnectionType */
        /* and RemoteDeviceAddress members identify the local service */
        /* receiving this indication. The Enable member specifies whether */
        /* this is an indication to enable (TRUE) or disable (FALSE) call */
        /* line identification notification. */
        @Override
        protected void callLineIdentificationNotificationActivationIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean enabled) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((AudioGatewayEventCallback)callbackHandler).callLineIdentificationNotificationActivationIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), enabled);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Audio Gateway when */
        /* the remote Hands Free device requests turning off the Audio */
        /* Gateways echo cancelling and noise reduction functions. The */
        /* ConnectionType and RemoteDeviceAddress members identify the local */
        /* connection receiving this indication. */
        @Override
        protected void disableSoundEnhancementIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((AudioGatewayEventCallback)callbackHandler).disableSoundEnhancementIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Audio Gateway when */
        /* the remote Hands Free device issues the command to place a call to */
        /* a specific phone number. The ConnectionType and */
        /* RemoteDeviceAddress members identify the local connection */
        /* receiving this indication. The PhoneNumber member is a NULL */
        /* terminated ASCII string representing the phone number in which to */
        /* place the call. */
        @Override
        protected void dialPhoneNumberIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String phoneNumber) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((AudioGatewayEventCallback)callbackHandler).dialPhoneNumberIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), phoneNumber);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Audio Gateway when */
        /* the remote Hands Free device issues the command for memory */
        /* dialing. The ConnectionType and RemoteDeviceAddress members */
        /* identify the local connection receiving this indication. The */
        /* MemoryLocation member specifies the memory location in which the */
        /* phone number to dial exists. */
        @Override
        protected void dialPhoneNumberFromMemoryIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int memoryLocation) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((AudioGatewayEventCallback)callbackHandler).dialPhoneNumberFromMemoryIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), memoryLocation);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Audio Gateway when */
        /* the remote Hands Free device issues the command to re-dial the */
        /* last number dialed. The ConnectionType and RemoteDeviceAddress */
        /* member identifies the local connection receiving this indication. */
        @Override
        protected void redialLastPhoneNumberIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((AudioGatewayEventCallback)callbackHandler).redialLastPhoneNumberIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Audio Gateway when */
        /* the remote Hands Free device issues the command to transmit a DTMF */
        /* code. The ConnectionType and RemoteDeviceAddress members identify */
        /* the local connection receiving this indication. The DTMFCode */
        /* member specifies the DTMF code to generate. */
        @Override
        protected void generateDTMFCodeIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, char dtmfCode) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((AudioGatewayEventCallback)callbackHandler).generateDTMFCodeIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), dtmfCode);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Audio Gateway when */
        /* the remote Hands Free device issues the command to answer an */
        /* incoming call. The ConnectionType and RemoteDeviceAddress members */
        /* identify the local connection receiving this indication. */
        @Override
        protected void answerCallIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((AudioGatewayEventCallback)callbackHandler).answerCallIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Audio Gateway when */
        /* the remote Hands Free device makes a request for a phone number to */
        /* attach to a voice tag. The ConnectionType and RemoteDeviceAddress */
        /* members identify the local connection receiving this indication. */
        @Override
        protected void voiceTagRequestIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((AudioGatewayEventCallback)callbackHandler).voiceTagRequestIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Audio Gateway when */
        /* the remote Hands Free device issues the command to hang-up an */
        /* on-going call or to reject and incoming call request. The */
        /* ConnectionType and RemoteDeviceAddress members identifu the local */
        /* connection receiving this indication. */
        @Override
        protected void hangUpIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((AudioGatewayEventCallback)callbackHandler).hangUpIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Audio gateway when it */
        /* receives a request for the current call list. The ConnectionType */
        /* and RemoteDeviceAddress members identify the local connection */
        /* receiving the indication. */
        @Override
        protected void currentCallsListIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((AudioGatewayEventCallback)callbackHandler).currentCallsListIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* This event is dispatched to a local Audio Gateway service when the */
        /* remote Hands Free device issues a set operator selection format */
        /* command. The ConnectionType and RemoteDeviceAddress members */
        /* identify the local connection receiving the indication. The */
        /* Format member contains the format value provided in this */
        /* operation. The Bluetooth Hands Free specification requires that */
        /* the remote device choose format 0. As a result, this event can */
        /* generally be ignored. */
        @Override
        protected void networkOperatorSelectionFormatIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int format) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((AudioGatewayEventCallback)callbackHandler).networkOperatorSelectionFormatIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), format);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* This event is dispatched to a local Audio Gateway service when the */
        /* remote Hands Free device has requested the current operator */
        /* selection. The ConnectionType and RemoteDeviceAddress members */
        /* identify the local connection receiving the indication. */
        @Override
        protected void networkOperatorSelectionIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((AudioGatewayEventCallback)callbackHandler).networkOperatorSelectionIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* This event is dispatched to a local Audio Gateway service when a */
        /* remote Hands Free device sends a command activate extended error */
        /* reporting. The ConnectionType and RemoteDeviceAddress members */
        /* identify the local connection receiving the indication. The */
        /* Enabled member contains a BOOLEAN value which indicates the */
        /* current state of extended error reporting. */
        @Override
        protected void extendedErrorResultActivationIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean enabled) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((AudioGatewayEventCallback)callbackHandler).extendedErrorResultActivationIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), enabled);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* This event is dispatched to a local Audio Gateway service when a */
        /* remote Hands Free device requests the current subscriber number. */
        /* The ConnectionType and RemoteDeviceAddress members identify the */
        /* local connection receiving the indication. */
        @Override
        protected void subscriberNumberInformationIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((AudioGatewayEventCallback)callbackHandler).subscriberNumberInformationIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* This event is dispatched to a local Audio Gateway service when a */
        /* remote Hands Free device requests the current response/hold */
        /* status. The ConnectionType and RemoteDeviceAddress members */
        /* identify the local connection receiving the indication. */
        @Override
        protected void responseHoldStatusIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((AudioGatewayEventCallback)callbackHandler).responseHoldStatusIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /* The following event is dispatched to a local Audio Gateway service */
        /* when the remote Hands Free device issues a commmand that is not */
        /* recognized by the local Audio Gateway (i.e. an arbitrary AT */
        /* Command). The ConnectionType and RemoteDeviceAddress members */
        /* identify the local connection receiving this indication. The */
        /* CommandData member is a pointer to a NULL terminated ASCII string */
        /* that represents the actual command data that was received. */
        @Override
        protected void arbitraryCommandIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String commandData) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((AudioGatewayEventCallback)callbackHandler).arbitraryCommandIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), commandData);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Receiver for event notifications which apply to an Audio Gateway
         * server.
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
             * Invoked when a Service Level Connection is established
             * successfully.
             * <p>
             * This event must be received before any other action on the port
             * is valid, except for sending arbitrary commands. If an error
             * occurs during the establishment of the service level connection
             * the connection will be closed and local device will receive a
             * disconnection event.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote
             *            device.
             * @param remoteSupportedFeatures Set of features of the remote
             *            device. May be NULL if the remote Hands-Free device
             *            does not support sending supported features flags.
             */
            public void serviceLevelConnectionEstablishedEvent(BluetoothAddress remoteDeviceAddress, EnumSet<HandsFreeServerManager.SupportedFeatures> remoteSupportedFeatures);

            /* The following event is dispatched to a local Audio Gateway when */
            /* the remote Hands Free service responds to a call waiting */
            /* notification by selecting how to handle the waiting call. The */
            /* ConnectionType and RemoteDeviceAddress members identify the local */
            /* service receiving this indication. The CallHoldMultipartyHandling */
            /* member specifies the requested action to take regarding the */
            /* waiting call. If the CallHoldMultipartyHandling member indicates */
            /*
             * an extended type then the Index member will contain the call
             * index
             */
            /* to which the operation refers. */
            /**
             * Invoked when a remote Hands-Free device responds to a call
             * waiting notification by selecting how to handle the call.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote
             *            device.
             * @param callHoldMultipartyHandling Indicates the requested action
             *            to take regarding the waiting call.
             * @param index The index of the call to which the operation refers.
             */
            public void callHoldMultipartySelectionIndicationEvent(BluetoothAddress remoteDeviceAddress, CallHoldMultipartyHandlingType callHoldMultipartyHandling, int index);

            /* The following event is dispatched to a local Audio Gateway when */
            /* the remote Hands Free device issues the command to enable or */
            /* disable call waiting notification. The ConnectionType and */
            /* RemoteDeviceAddress members identify the local service receiving */
            /* this indication. The Enabled member specifies whether this is an */
            /* indication to enable (TRUE) or disable (FALSE) call waiting */
            /* notification. */
            /**
             * Invoked when a remote Hands-Free devices issues the command
             * to enable or disable call waiting notifications.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote
             *            device.
             * @param enabled Indicates whether the indication should be
             *            enables or disabled.
             */
            public void callWaitingNotificationActivationIndicationEvent(BluetoothAddress remoteDeviceAddress, boolean enabled);

            /* The following event is dispatched to a local Audio Gateway when */
            /* the remote Hands Free device issues the command to enable or */
            /* disable call line identification notification. The ConnectionType */
            /* and RemoteDeviceAddress members identify the local service */
            /* receiving this indication. The Enable member specifies whether */
            /* this is an indication to enable (TRUE) or disable (FALSE) call */
            /* line identification notification. */
            /**
             * Invoked when a remote Hands-Free device issues the command
             * to enable or disable call line identification notifications.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote
             *            device.
             * @param enabled
             */
            public void callLineIdentificationNotificationActivationIndicationEvent(BluetoothAddress remoteDeviceAddress, boolean enabled);

            /* The following event is dispatched to a local Audio Gateway when */
            /* the remote Hands Free device requests turning off the Audio */
            /* Gateways echo cancelling and noise reduction functions. The */
            /* ConnectionType and RemoteDeviceAddress members identify the local */
            /* connection receiving this indication. */
            /**
             * Invoked when a remote Hands-Free device issues the command
             * to turn off Audio Gateways echo cancelling and noise reduction
             * functions.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote
             *            device.
             */
            public void disableSoundEnhancementIndicationEvent(BluetoothAddress remoteDeviceAddress);

            /* The following event is dispatched to a local Audio Gateway when */
            /*
             * the remote Hands Free device issues the command to place a call
             * to
             */
            /* a specific phone number. The ConnectionType and */
            /* RemoteDeviceAddress members identify the local connection */
            /* receiving this indication. The PhoneNumber member is a NULL */
            /* terminated ASCII string representing the phone number in which to */
            /* place the call. */
            /**
             * Invoked when a remote Hands-Free device issues the command
             * to place a call to a specific phone number.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote
             *            device.
             * @param phoneNumber The phone number to dial.
             */
            public void dialPhoneNumberIndicationEvent(BluetoothAddress remoteDeviceAddress, String phoneNumber);

            /* The following event is dispatched to a local Audio Gateway when */
            /* the remote Hands Free device issues the command for memory */
            /* dialing. The ConnectionType and RemoteDeviceAddress members */
            /* identify the local connection receiving this indication. The */
            /* MemoryLocation member specifies the memory location in which the */
            /* phone number to dial exists. */
            /**
             * Invoked when a remote Hands-Free device issues the command
             * @param remoteDeviceAddress Bluetooth address of the remote
             *            device.
             * @param memoryLocation
             */
            public void dialPhoneNumberFromMemoryIndicationEvent(BluetoothAddress remoteDeviceAddress, int memoryLocation);

            /* The following event is dispatched to a local Audio Gateway when */
            /* the remote Hands Free device issues the command to re-dial the */
            /* last number dialed. The ConnectionType and RemoteDeviceAddress */
            /* member identifies the local connection receiving this indication. */
            /**
             * Invoked when a remote Hands-Free device issues the command
             * to re-dial the last number dialed.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote
             *            device.
             */
            public void redialLastPhoneNumberIndicationEvent(BluetoothAddress remoteDeviceAddress);

            /* The following event is dispatched to a local Audio Gateway when */
            /*
             * the remote Hands Free device issues the command to transmit a
             * DTMF
             */
            /* code. The ConnectionType and RemoteDeviceAddress members identify */
            /* the local connection receiving this indication. The DTMFCode */
            /* member specifies the DTMF code to generate. */
            /**
             * Invoked when a remote Hands-Free device issues the command
             * to transmit a DTMF code.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote
             *            device.
             * @param dtmfCode Specifies the DTMF code to generate.
             */
            public void generateDTMFCodeIndicationEvent(BluetoothAddress remoteDeviceAddress, char dtmfCode);

            /* The following event is dispatched to a local Audio Gateway when */
            /* the remote Hands Free device issues the command to answer an */
            /* incoming call. The ConnectionType and RemoteDeviceAddress members */
            /* identify the local connection receiving this indication. */
            /**
             * Invoked when a remote Hands-Free device issues the command
             * to answer an incoming call.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote
             *            device.
             */
            public void answerCallIndicationEvent(BluetoothAddress remoteDeviceAddress);

            /* The following event is dispatched to a local Audio Gateway when */
            /*
             * the remote Hands Free device makes a request for a phone number
             * to
             */
            /* attach to a voice tag. The ConnectionType and RemoteDeviceAddress */
            /* members identify the local connection receiving this indication. */
            /**
             * Invoked when a remote Hands-Free device issues the command
             * to request for a phone number to attach a voice tag.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote
             *            device.
             */
            public void voiceTagRequestIndicationEvent(BluetoothAddress remoteDeviceAddress);

            /* The following event is dispatched to a local Audio Gateway when */
            /* the remote Hands Free device issues the command to hang-up an */
            /* on-going call or to reject and incoming call request. The */
            /* ConnectionType and RemoteDeviceAddress members identifu the local */
            /* connection receiving this indication. */
            /**
             * Invoked when a remote Hands-Free device issues the command
             * to hang up on ongoing call or reject an incoming call.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote
             *            device.
             */
            public void hangUpIndicationEvent(BluetoothAddress remoteDeviceAddress);

            /*
             * The following event is dispatched to a local Audio gateway when
             * it
             */
            /* receives a request for the current call list. The ConnectionType */
            /* and RemoteDeviceAddress members identify the local connection */
            /* receiving the indication. */
            /**
             * Invoked when a remote Hands-Free device issues the command
             * to request the current call list.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote
             *            device.
             */
            public void currentCallsListIndicationEvent(BluetoothAddress remoteDeviceAddress);

            /*
             * This event is dispatched to a local Audio Gateway service when
             * the
             */
            /* remote Hands Free device issues a set operator selection format */
            /* command. The ConnectionType and RemoteDeviceAddress members */
            /* identify the local connection receiving the indication. The */
            /* Format member contains the format value provided in this */
            /* operation. The Bluetooth Hands Free specification requires that */
            /* the remote device choose format 0. As a result, this event can */
            /* generally be ignored. */
            /**
             * Invoked when a remote Hands-Free device issues the command
             * to set the operator selection format.
             * <p>
             * The Bluetooth Hands Free specification requires that the
             * remote device choose format 0. As a result, this event can
             * generally be ignored.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote
             *            device.
             * @param format The format value.
             */
            public void networkOperatorSelectionFormatIndicationEvent(BluetoothAddress remoteDeviceAddress, int format);

            /*
             * This event is dispatched to a local Audio Gateway service when
             * the
             */
            /* remote Hands Free device has requested the current operator */
            /* selection. The ConnectionType and RemoteDeviceAddress members */
            /* identify the local connection receiving the indication. */
            /**
             * Invoked when a remote Hands-Free device issues the command
             * to request the current operator selection.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote
             *            device.
             */
            public void networkOperatorSelectionIndicationEvent(BluetoothAddress remoteDeviceAddress);

            /* This event is dispatched to a local Audio Gateway service when a */
            /* remote Hands Free device sends a command activate extended error */
            /* reporting. The ConnectionType and RemoteDeviceAddress members */
            /* identify the local connection receiving the indication. The */
            /* Enabled member contains a BOOLEAN value which indicates the */
            /* current state of extended error reporting. */
            /**
             * Invoked when a remote Hands-Free device issues the command
             * to activate exteneded error reporting.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote
             *            device.
             * @param enabled Indicates whether extended errors should be enabled.
             */
            public void extendedErrorResultActivationIndicationEvent(BluetoothAddress remoteDeviceAddress, boolean enabled);

            /* This event is dispatched to a local Audio Gateway service when a */
            /* remote Hands Free device requests the current subscriber number. */
            /* The ConnectionType and RemoteDeviceAddress members identify the */
            /* local connection receiving the indication. */
            /**
             * Invoked when a remote Hands-Free device issues the command
             * to request the current subscriber number.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote
             *            device.
             */
            public void subscriberNumberInformationIndicationEvent(BluetoothAddress remoteDeviceAddress);

            /* This event is dispatched to a local Audio Gateway service when a */
            /* remote Hands Free device requests the current response/hold */
            /* status. The ConnectionType and RemoteDeviceAddress members */
            /* identify the local connection receiving the indication. */
            /**
             * Invoked when a remote Hands-Free device issues the command
             * to request the current response/hold status.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote
             *            device.
             */
            public void responseHoldStatusIndicationEvent(BluetoothAddress remoteDeviceAddress);

            /*
             * The following event is dispatched to a local Audio Gateway
             * service
             */
            /* when the remote Hands Free device issues a commmand that is not */
            /* recognized by the local Audio Gateway (i.e. an arbitrary AT */
            /* Command). The ConnectionType and RemoteDeviceAddress members */
            /* identify the local connection receiving this indication. The */
            /* CommandData member is a pointer to a NULL terminated ASCII string */
            /* that represents the actual command data that was received. */
            /**
             * Invoked when a remote Hands-Free device issues a command
             * that is not recognized by the local Audio Gateway.
             *
             * @param remoteDeviceAddress Bluetooth address of the remote
             *            device.
             * @param commandData Contains the actual command data.
             *
             */
            public void arbitraryCommandIndicationEvent(BluetoothAddress remoteDeviceAddress, String commandData);
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

    /* The following enumerated type represents the available types for */
    /* the controlling of multiple concurrent calls. */
    private final static int MULTIPARTY_HANDLING_RELEASE_ALL_HELD_CALLS                          = 0;
    private final static int MULTIPARTY_HANDLING_RELEASE_ALL_ACTIVE_CALLS_ACCEPT_WAITING_CALL    = 1;
    private final static int MULTIPARTY_HANDLING_PLACE_ALL_ACTIVE_CALLS_ON_HOLD_ACCEPT_THE_OTHER = 2;
    private final static int MULTIPARTY_HANDLING_ADD_A_HELD_CALL_TO_CONVERSATION                 = 3;
    private final static int MULTIPARTY_HANDLING_CONNECT_TWO_CALLS_AND_DISCONNECT                = 4;
    private final static int MULTIPARTY_HANDLING_RELEASE_SPECIFIED_CALL_INDEX                    = 5;
    private final static int MULTIPARTY_HANDLING_PRIVATE_CONSULTATION_MODE                       = 6;

    /**
     * Possible actions to take to manage multiple concurrent calls.
     */
    public enum CallHoldMultipartyHandlingType {
        /**
         * Hang-up all calls in the Hold state.
         */
        RELEASE_ALL_HELD_CALLS,

        /**
         * Hang-up all active calls and accept the incoming call.
         */
        RELEASE_ALL_ACTIVE_CALLS_ACCEPT_WAITING_CALL,

        /**
         * Put all active calls on Hold and accept the incoming call.
         */
        PLACE_ALL_ACTIVE_CALLS_ON_HOLD_ACCEPT_THE_OTHER,

        /**
         * Add a held call to a multi-party conversation (also known as a
         * conference call).
         */
        ADD_A_HELD_CALL_TO_CONVERSATION,

        /**
         * Connect the two active calls to each other and hang-up (also known as
         * explicit call transfer).
         */
        CONNECT_TWO_CALLS_AND_DISCONNECT,

        /**
         * Hang-up a specific call, given by index.
         */
        RELEASE_SPECIFIED_CALL_INDEX,

        /**
         * Place all calls on hold except for the specified call, given by
         * index.
         */
        PRIVATE_CONSULTATION_MODE
    }

    /* The following enumerated type represents all of the allowable call */
    /* state types that are sent or received as part of a Response and */
    /* Hold Status Response. */
    private final static int CALL_STATE_HOLD   = 0;
    private final static int CALL_STATE_ACCEPT = 1;
    private final static int CALL_STATE_REJECT = 2;
    private final static int CALL_STATE_NONE   = 3;

    /**
     * Possible states of an in-progress call. Used to indicate the current
     * state of in-progress calls and to instruct on how to handle new calls.
     */
    public enum CallState {
        /**
         * Call is on hold or should be placed on hold.
         */
        HOLD,

        /**
         * Call is active or should be made active.
         */
        ACCEPT,

        /**
         * Call should be rejected.
         */
        REJECT,

        /**
         * Call should be ignored.
         */
        NONE
    }

    /* The following enumerated type represents all of the possible */
    /* defined return values from AT command execution. All except */
    /* 'erOK' can also be received as unsolicited result codes. */
    private final static int EXTENDED_RESULT_OK          = 0;
    private final static int EXTENDED_RESULT_ERROR       = 1;
    private final static int EXTENDED_RESULT_NOCARRIER   = 2;
    private final static int EXTENDED_RESULT_BUSY        = 3;
    private final static int EXTENDED_RESULT_NOANSWER    = 4;
    private final static int EXTENDED_RESULT_DELAYED     = 5;
    private final static int EXTENDED_RESULT_BLACKLISTED = 6;
    private final static int EXTENDED_RESULT_RESULTCODE  = 7;

    /**
     * Possible responses to commands and unsolicited status updates, which are
     * sent by the Audio Gateway to the connected Hands-Free device. Any result
     * except {@code ExtendedResult.OK} can be received as an unsolicited result
     * code.
     */
    public enum ExtendedResult {
        /**
         * The command completed successfully.
         */
        OK,

        /**
         * The command failed to complete for an unspecified reason.
         */
        ERROR,

        /**
         * Carrier signal was lost.
         */
        NO_CARRIER,

        /**
         * The dialed phone call resulted in a busy signal.
         */
        BUSY,

        /**
         * The dialed phone call was not answered.
         */
        NO_ANSWER,

        DELAYED,

        BLACKLISTED,

        /**
         * An extended result code is specified.
         */
        RESULT_CODE
    }

    private final static int CALL_DIRECTION_MOBILE_ORIGINATED = 0;
    private final static int CALL_DIRECTION_MOBILE_TERMINATED = 1;

    /**
     * Specifies who initiated a phone call.
     */
    public enum CallDirection {
        /**
         * The local phone placed an outgoing call.
         */
        MOBILE_ORIGINATED,

        /**
         * The local phone received an incoming call.
         */
        MOBILE_TERMINATED
    }

    private final static int CALL_STATUS_ACTIVE   = 0;
    private final static int CALL_STATUS_HELD     = 1;
    private final static int CALL_STATUS_DIALING  = 2;
    private final static int CALL_STATUS_ALERTING = 3;
    private final static int CALL_STATUS_INCOMING = 4;
    private final static int CALL_STATUS_WAITING  = 5;

    /**
     * Possible call statuses as given by a Current Calls List Confirmation
     * event.
     *
     * @see HandsFreeServerManager#queryRemoteCurrentCallsList
     * @see HandsFreeEventCallback#currentCallsListConfirmationEvent
     */
    public enum CallStatus {
        /**
         * Call is active.
         */
        ACTIVE,

        /**
         * Call is on-hold.
         */
        HELD,

        /**
         * The phone number is being dialed (outgoing calls only).
         */
        DIALING,

        /**
         * The call is ringing (outgoing calls only).
         */
        ALERTING,

        /**
         * The call is ringing (incoming calls only).
         */
        INCOMING,

        /**
         * The call is waiting while another call is active (incoming calls
         * only)
         */
        WAITING
    }

    private final static int CALL_MODE_VOICE = 0;
    private final static int CALL_MODE_DATA  = 1;
    private final static int CALL_MODE_FAX   = 2;

    /**
     * Possible call modes as given by a Current Calls List Confirmation event.
     *
     * @see HandsFreeServerManager#queryRemoteCurrentCallsList
     * @see HandsFreeEventCallback#currentCallsListConfirmationEvent
     */
    public enum CallMode {
        /**
         * Voice call.
         */
        VOICE,

        /**
         * Data call.
         */
        DATA,

        /**
         * Fax call.
         */
        FAX
    }

    /* The following constant define the values that can be used for */
    /* network mode information. They are used to translate between JNI */
    /* and the enumerated type. */
    private final static int NETWORK_MODE_AUTOMATIC   = 0;
    private final static int NETWORK_MODE_MANUAL      = 1;
    private final static int NETWORK_MODE_DEREGISTER  = 2;
    private final static int NETWORK_MODE_SETONLY     = 3;
    private final static int NETWORK_MODE_MANUAL_AUTO = 4;

    /* The following constant define the values that can be used for */
    /* network mode information. Generally this information is ignored */
    /* by HFRE. */
    /**
     * Possible network operator selection modes. This is generally ignored by
     * the Hands-Free Profile.
     *
     * @see HandsFreeServerManager#queryRemoteNetworkOperatorSelection
     * @see HandsFreeEventCallback#networkOperatorSelectionConfirmationEvent
     */
    public enum NetworkMode {
        AUTOMATIC,
        MANUAL,
        DEREGISTER,
        SETONLY,
        MANUAL_AUTO
    }

    /**
     * Description of the subscriber's phone number.
     */
    public static final class SubscriberNumberInformation {
        public int    serviceType;

        /**
         * Format of the phone number. This can be one of the following:
         * <ul>
         * <li>128-143: May be a national or international format. May contain
         * prefix and/or escape digits.</li>
         * <li>144-159: International number, including the country code prefix.
         * Begins with the plus (+) sign.</li>
         * <li>160-175: National number. No prefix or escape digits are
         * included.</li>
         * </ul>
         */
        public int    numberFormat;

        /**
         * The subscriber's phone number.
         */
        public String phoneNumber;

        /**
         * Initialize this {@code SubscriberNumberInformation} with a service
         * type, number format, and phone number.
         *
         * @param serviceType The service type of this subscription.
         * @param numberFormat The format of the provided phone number. See
         *            {@link #numberFormat}.
         * @param phoneNumber The phone number.
         */
        public SubscriberNumberInformation(int serviceType, int numberFormat, String phoneNumber) {
            this.serviceType = serviceType;
            this.numberFormat = numberFormat;
            this.phoneNumber = phoneNumber;
        }
    }

    /**
     * Contains all relevant information regarding an in-progress call.
     *
     * @see HandsFreeServerManager#queryRemoteCurrentCallsList
     * @see HandsFreeEventCallback#currentCallsListConfirmationEvent
     * @see AudioGatewayServerManager#sendCurrentCallsList
     */
    public static final class CurrentCallListEntry {
        int           index;
        CallDirection callDirection;
        CallStatus    callStatus;
        CallMode      callMode;
        boolean       multiparty;
        String        phoneNumber;
        int           numberFormat;

        /**
         * Initialize this {@code CurrentCallListEntry}.
         *
         * @param index Numeric index of this call for use in call waiting and
         *            multi-party control commands.
         * @param callDirection Origination direction of the call.
         * @param callStatus Current status of the call.
         * @param callMode Current mode of the call.
         * @param multiparty {@code true} if the call is a multi-party call.
         * @param phoneNumber Phone number of the connected party.
         * @param numberFormat Format of the {@code phoneNumber} field. See
         *            {@link SubscriberNumberInformation#numberFormat}.
         */
        public CurrentCallListEntry(int index, CallDirection callDirection, CallStatus callStatus, CallMode callMode, boolean multiparty, String phoneNumber, int numberFormat) {
            this.index = index;
            this.callDirection = callDirection;
            this.callStatus = callStatus;
            this.callMode = callMode;
            this.multiparty = multiparty;
            this.phoneNumber = phoneNumber;
            this.numberFormat = numberFormat;
        }

        public int getIndex() {
            return index;
        }

        public CallDirection getCallDirection() {
            return callDirection;
        }

        public CallStatus getCallStatus() {
            return callStatus;
        }

        public CallMode getCallMode() {
            return callMode;
        }

        public boolean isMultiparty() {
            return multiparty;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public int getNumberFormat() {
            return numberFormat;
        }
    }

    /* Private constants to denote what type of indicator is being used. */
    private static final int INDICATOR_TYPE_BOOLEAN = 0;
    private static final int INDICATOR_TYPE_RANGE = 1;

    /**
     * Allowed types for Indicators.
     *
     * @see IndicatorUpdateBoolean
     * @see IndicatorUpdateRange
     */
    public enum IndicatorType {
        BOOLEAN,
        RANGE
    }

    /**
     * Abstract base type for a Configuration Indicator.
     */
    public static abstract class ConfigurationIndicatorEntry {
        private final String indicatorDescription;

        protected ConfigurationIndicatorEntry(String description) {
            indicatorDescription = description;
        }

        public abstract IndicatorType getType();

        public String getDescription() {
            return indicatorDescription;
        }
    }

    /**
     * Boolean-type configuration indicator.
     */
    public static final class ConfigurationIndicatorBoolean extends ConfigurationIndicatorEntry {
        private final boolean value;

        public ConfigurationIndicatorBoolean(String description, boolean value) {
            super(description);

            this.value = value;
        }

        @Override
        public IndicatorType getType() {
            return IndicatorType.BOOLEAN;
        }

        public boolean getValue() {
            return value;
        }
    }

    /**
     * Range-type configuration indicator.
     */
    public static final class ConfigurationIndicatorRange extends ConfigurationIndicatorEntry {
        private final int minValue;
        private final int maxValue;
        private final int value;

        public ConfigurationIndicatorRange(String description, int value, int minimum, int maximum) {
            super(description);

            this.value    = value;
            this.minValue = minimum;
            this.maxValue = maximum;
        }

        @Override
        public IndicatorType getType() {
            return IndicatorType.RANGE;
        }

        public int getValue() {
            return value;
        }

        public int getMinimumValue() {
            return minValue;
        }

        public int getMaximumValue() {
            return maxValue;
        }
    }

    /**
     * Abstract base type for a Control Indicator.
     *
     * @see AudioGatewayServerManager#updateCurrentControlIndicatorStatus(BluetoothAddress, ControlIndicatorUpdate[])
     */
    public static abstract class ControlIndicatorUpdate {
        protected final String indicatorName;

        protected ControlIndicatorUpdate(String name) {
            indicatorName = name;
        }

        public abstract IndicatorType getType();

        public String getName() {
            return indicatorName;
        }
    }

    /**
     * Boolean-type Control Indicator.
     *
     * @see AudioGatewayServerManager#updateCurrentControlIndicatorStatus(BluetoothAddress, ControlIndicatorUpdate[])
     */
    public static final class IndicatorUpdateBoolean extends ControlIndicatorUpdate {
        private final boolean value;

        public IndicatorUpdateBoolean(String name, boolean value) {
            super(name);

            this.value = value;
        }

        @Override
        public IndicatorType getType() {
            return IndicatorType.BOOLEAN;
        }

        public boolean getValue() {
            return value;
        }
    }

    /**
     * Integer range-type Control Indicator.
     *
     * @see AudioGatewayServerManager#updateCurrentControlIndicatorStatus(BluetoothAddress, ControlIndicatorUpdate[])
     */
    public static final class IndicatorUpdateRange extends ControlIndicatorUpdate {
        private final int value;

        public IndicatorUpdateRange(String name, int value) {
            super(name);

            this.value = value;
        }

        @Override
        public IndicatorType getType() {
            return IndicatorType.RANGE;
        }

        public int getValue() {
            return value;
        }
    }

    private static final class LocalConfigurationNative {
        /*pkg*/ int                           incomingConnectionFlags;
        /*pkg*/ int                           supportedFeaturesMask;
        /*pkg*/ int                           callHoldingSupportMask;
        /*pkg*/ int                           networkType;
        /*pkg*/ ConfigurationIndicatorEntry[] additionalIndicatorList;

        /*pkg*/ LocalConfigurationNative(int incomingConnectionFlags, int supportedFeaturesMask, int callHoldingSupportMask, int networkType, ConfigurationIndicatorEntry[] additionalIndicatorList) {
            this.incomingConnectionFlags = incomingConnectionFlags;
            this.supportedFeaturesMask   = supportedFeaturesMask;
            this.callHoldingSupportMask  = callHoldingSupportMask;
            this.networkType             = networkType;
            this.additionalIndicatorList = additionalIndicatorList;
        }
    }

    /* Native method declarations */

    private native static void initClassNative();

    private native void initObjectNative(int serverType, boolean controller) throws ServerNotReachableException;

    private native void cleanupObjectNative();

    private native int connectionRequestResponseNative(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean acceptConnection);

    private native int connectRemoteDeviceNative(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int remoteServerPort, int connectionFlags, boolean waitForConnection);

    private native int disconnectDeviceNative(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    private native BluetoothAddress[] queryConnectedDevicesNative(int serverType);

    protected native LocalConfigurationNative queryCurrentConfigurationNative(int serverType);

    private native int changeIncomingConnectionFlagsNative(int serverType, int connectionFlags);

    private native int disableRemoteEchoCancellationNoiseReductionNative(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    private native int setRemoteVoiceRecognitionActivationNative(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean voiceRecognitionActive);

    private native int setRemoteSpeakerGainNative(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int speakerGain);

    private native int setRemoteMicrophoneGainNative(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int microphoneGain);

    private native int registerDataEventCallbackNative(int serverType);

    private native void unregisterDataEventCallbackNative();

    protected native int setupAudioConnectionNative(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    protected native int releaseAudioConnectionNative(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    private native int sendAudioDataNative(int serverType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte[] audioData);

    protected native int queryRemoteControlIndicatorStatusNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    protected native int enableRemoteIndicatorEventNotificationNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean enableEventNotification);

    protected native int queryRemoteCallHoldingMultipartyServiceSupportNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    protected native int sendCallHoldingMultipartySelectionNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int callHoldMultipartyHandling, int index);

    protected native int enableRemoteCallWaitingNotificationNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean enableNotification);

    protected native int enableRemoteCallLineIdentificationNotificationNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean enableNotification);

    protected native int dialPhoneNumberNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String phoneNumber);

    protected native int dialPhoneNumberFromMemoryNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int memoryLocation);

    protected native int redialLastPhoneNumberNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    protected native int answerIncomingCallNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    protected native int transmitDTMFCodeNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, char dtmfCode);

    protected native int voiceTagRequestNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    protected native int hangUpCallNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    protected native int queryRemoteCurrentCallsListNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    protected native int setNetworkOperatorSelectionFormatNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    protected native int queryRemoteNetworkOperatorSelectionNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    protected native int enableRemoteExtendedErrorResultNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean enableExtendedErrorResults);

    protected native int querySubscriberNumberInformationNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    protected native int queryResponseHoldStatusNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    protected native int setIncomingCallStateNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int callState);

    protected native int sendArbitraryCommandNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String arbitraryCommand);

    protected native int updateCurrentControlIndicatorStatusNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String[] indicatorNames, int[] indicatorTypes, int[] indicatorValues);

    protected native int updateCurrentControlIndicatorStatusByNameNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String indicatorName, int indicatorValue);

    protected native int sendCallWaitingNotificationNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String phoneNumber);

    protected native int sendCallLineIdentificationNotificationNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String phoneNumber);

    protected native int ringIndicationNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    protected native int enableRemoteInBandRingToneSettingNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean enableInBandRing);

    protected native int voiceTagResponseNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String phoneNumber);

    protected native int sendCurrentCallsListNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int[] indices, int[] callDirectrions, int[] callStatuses, int[] callModes, boolean[] multiparties, String[] phoneNumbers, int[] numberFormats);

    protected native int sendNetworkOperatorSelectionNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int networkMode, String networkOperator);

    protected native int sendExtendedErrorResultNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int resultCode);

    protected native int sendSubscriberNumberInformationNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int[] serviceTypes, int[] numberFormats, String[] phoneNumbers);

    protected native int sendIncomingCallStateNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int callState);

    protected native int sendTerminatingResponseNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int resultType, int resultValue);

    protected native int enableArbitraryCommandProcessingNative();

    protected native int sendArbitraryResponseNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String arbitraryResponse);
}
