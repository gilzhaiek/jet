/*
 * SPPM.java Copyright 2010 - 2014 Stonestreet One, LLC. All Rights Reserved.
 */
package com.stonestreetone.bluetopiapm;

import java.security.InvalidParameterException;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Java wrapper for Serial Port Profile Manager API for Stonestreet One
 * Bluetooth Protocol Stack Platform Manager.
 */
public class SPPM {

    protected final EventCallback callbackHandler;

    private boolean               disposed;

    private long                  localData;

    static {
        System.loadLibrary("btpmj");
        initClassNative();
    }
    // TODO Add a check to methods with optional timeouts to protect against calling from an event callback context. Perhaps compare Thread.currentThread() objects?

    private static final int      MANAGER_TYPE_CLIENT = 1;
    private static final int      MANAGER_TYPE_SERVER = 2;

    protected int                 portHandle;

    /**
     * Specifies that the function should return immediately without
     * waiting for queued data to be read or sent.
     */
    public static final int       TIMEOUT_IMMEDIATE = 0x00000000;

    /**
     * Specifies that the function should block indefinitely until
     * queued data has been received or sent.
     */
    public static final int       TIMEOUT_INFINITE  = 0xFFFFFFFF;

    /**
     * Denotes the minimum length (in milliseconds) of the break signal.
     */
    public static final int       BREAK_TIMEOUT_MINIMUM = 0;

    /**
     * Denotes the maximum length (in milliseconds) of the break signal.
     */
    public static final int       BREAK_TIMEOUT_MAXIMUM = 3000;

    private static final int      NATIVE_ERROR_INVALID_PARAMETER = -10001;

    /**
     * Private constructor.
     *
     * @throws ServerNotReachableException
     */
    protected SPPM(int type, EventCallback eventCallback) throws ServerNotReachableException {

        try {

            initObjectNative();
            disposed        = false;
            callbackHandler = eventCallback;

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

    /* Commands shared by both Server and Client Managers */

    /**
     * Disconnects any active connection to the port.
     * <p>
     * If this is called on a a Server Manager, the connection to a
     * remote device will be disconnected but the SPP server will
     * remain open and available for future connections.
     * <p>
     * This method CANNOT be called with a Timeout if it is issued
     * within an Event Callback as this will cause a deadlock to
     * occur.
     *
     * @param flushTimeout
     *        A timeout value (given in milliseconds) to wait for all
     *        queued data to be written before the port is closed. Can
     *        also be {@link #TIMEOUT_IMMEDIATE} to disconnect
     *        immediately, or {@link #TIMEOUT_INFINITE} to wait until
     *        all queued data is sent. This parameter MUST be zero if
     *        called from the thread context of an event callback.
     *
     * @return Zero if successful or a negative return error code if
     *         there was an error.
     */
    public int disconnectRemoteDevice(int flushTimeout) {
        return disconnectNative(portHandle, flushTimeout);
    }

    /**
     * Reads data that has been written to the port.
     * <p>
     * This method CANNOT be called with a Timeout if it is issued
     * within an Event Callback as this will cause a deadlock to
     * occur.
     * <p>
     * If {@code dataBuffer} is {@code null}, then the method will
     * return the amount of data (in bytes) which is currently buffered
     * and immediately available for reading.
     * <p>
     * This method does not attempt to fill the entire buffer. It will
     * return as soon as at least a single byte of data is received
     * (or when the specified timeout is exceeded).
     *
     * @param dataBuffer
     *        The byte array to copy the data into.
     *
     * @param readTimeout
     *        A timeout (specified in milliseconds) after which the
     *        method will return if no data has been read. Can also be
     *        {@link #TIMEOUT_IMMEDIATE} to only read immediately
     *        available data, or {@link #TIMEOUT_INFINITE} to block
     *        indefinitely until some data becomes available or the
     *        connection is closed.
     *
     * @return The number of bytes read into the buffer if successful,
     *         zero if the timeout is exceeded, or a negative error
     *         return code if there was an error. If {@code dataBuffer}
     *         is {@code null}, returns the number of currently buffered
     *         bytes available for reading.
     */
    public int readData(byte[] dataBuffer, int readTimeout) {
        return readData(dataBuffer, 0, (dataBuffer == null ? 0 : dataBuffer.length), readTimeout);
    }

    /**
     * Reads data that has been written to the port, with support for
     * specifying a maximum number of bytes to be read from a specific
     * starting location within the buffer..
     * <p>
     * This method CANNOT be called with a Timeout if it is issued
     * within the SPPM Event Callback as this will cause a deadlock will
     * occur.
     * <p>
     * If {@code dataBuffer} is {@code null}, then the method will
     * return the amount of data (in bytes) which is currently buffered
     * and immediately available for reading.
     * <p>
     * This method does not attempt to fill the entire buffer. It will
     * return as soon as at least a single byte of data is received
     * (or when the specified timeout is exceeded).
     *
     * @param dataBuffer
     *        The byte array to copy the data into.
     *
     * @param startPosition
     *        The starting location in the buffer where the read
     *        data should be placed.
     *
     * @param maxDataLength
     *        The maximum number of bytes to read.
     *
     * @param readTimeout
     *        A timeout (specified in milliseconds) after which the
     *        method will return if no data has been read. Can also be
     *        {@link #TIMEOUT_IMMEDIATE} to only read immediately
     *        available data, or {@link #TIMEOUT_INFINITE} to block
     *        indefinitely until some data becomes available or the
     *        connection is closed.
     *
     * @return
     *        The number of bytes read into the buffer if successful,
     *        zero if the timeout is exceeded, or a negative error
     *        return code if there was an error. If {@code dataBuffer}
     *        is {@code null}, returns the number of currently buffered
     *        bytes available for reading.
     */
    public int readData(byte[] dataBuffer, int startPosition, int maxDataLength, int readTimeout) {
        if(dataBuffer != null) {
            return readDataNative(portHandle, readTimeout, dataBuffer, startPosition, maxDataLength);
        } else {
            return readDataNative(portHandle, 0, null, 0, 0);
        }
    }

    /**
     * Writes data to a connected serial port.
     * <p>
     * This method will send data as internal buffer space becomes
     * available. It is not guaranteed to send the entire buffer. A
     * timeout can be specified which will allow the method to block for
     * up to the specified limit, waiting for internal buffer space to
     * become available. Regardless of the specified timeout, the method
     * will return immediately if all the supplied data is queued for
     * transmission.
     * <p>
     *
     * @param writeTimeout
     *        Specifies a length of time to wait before a force return even
     *        if data is not completely sent.
     *
     * @param dataBuffer
     *        The buffer containing the data to send.
     *
     * @return Returns the number of bytes written to the port (either the
     *         length of the buffer or the number of bytes written before a
     *         timeout occurred).
     */
    public int writeData(byte[] dataBuffer, int writeTimeout) {
        return writeData(dataBuffer, 0, (dataBuffer == null ? 0 : dataBuffer.length), writeTimeout);
    }

    /**
     * Writes data to a connected serial port with support for
     * specifying the number of bytes to be written, starting at a
     * specific starting location within the buffer.
     * <p>
     * This method will send data as internal buffer space becomes
     * available. It is not guaranteed to send the entire buffer. A
     * timeout can be specified which will allow the method to block for
     * up to the specified limit, waiting for internal buffer space to
     * become available. Regardless of the specified timeout, the method
     * will return immediately if all the supplied data is queued for
     * transmission.
     * <p>
     *
     * @param writeTimeout
     *        Specifies a length of time to wait before a force return even
     *        if data is not completely sent.
     *
     * @param dataBuffer
     *        The buffer containing the data to send.
     *
     * @param startPosition
     *        The starting location in the buffer from which data
     *        will be retrieved.
     *
     * @param dataLength
     *        The number of bytes to send.
     *
     * @return Returns the number of bytes written to the port (either the
     *         length of the buffer or the number of bytes written before a
     *         timeout occurred).
     */
    public int writeData(byte[] dataBuffer, int startPosition, int dataLength, int writeTimeout) {
        return writeDataNative(portHandle, writeTimeout, dataBuffer, startPosition, dataLength);
    }

    /**
     * Sends one or more Line Status bits to the connected remote device.
     *
     * @param lineStatus
     *        The set of line statuses to send.
     *
     * @return Zero if successful or a negative return error code if there was
     *         an error.
     */
    public int sendLineStatus(EnumSet<LineStatus> lineStatus) {
        int mask;

        mask = LINE_STATUS_MASK_NO_ERROR_VALUE;
        if(lineStatus != null) {
            mask |= (lineStatus.contains(LineStatus.OVERRUN_ERROR) ? LINE_STATUS_MASK_OVERRUN_ERROR_MASK : 0);
            mask |= (lineStatus.contains(LineStatus.PARITY_ERROR)  ? LINE_STATUS_MASK_PARITY_ERROR_MASK  : 0);
            mask |= (lineStatus.contains(LineStatus.FRAMING_ERROR) ? LINE_STATUS_MASK_FRAMING_ERROR_MASK : 0);
        }

        return sendLineStatusNative(portHandle, mask);
    }

    /**
     * Sends a port status indication to the connected remote device.
     *
     * @param portStatus
     *        The set of active port status bits to send.
     *
     * @param breakSignal
     *        {@code True} if a break signal should be transmitted.
     *
     * @param breakTimeout
     *        If {@code breakSignal} is {@code true}, the length of the
     *        break signal, specified in milliseconds. Must be between
     *        {@link #BREAK_TIMEOUT_MINIMUM} and
     *        {@link #BREAK_TIMEOUT_MAXIMUM}, inclusive.
     *
     * @return Zero if successful or a negative return error code if there was
     *         an error.
     */
    public int sendPortStatus(EnumSet<PortStatus> portStatus, boolean breakSignal, int breakTimeout) {
        int mask;

        mask = PORT_STATUS_MASK_NO_ERROR_VALUE;
        if(portStatus != null) {
            mask |= (portStatus.contains(PortStatus.RTS_CTS)        ? PORT_STATUS_MASK_RTS_CTS_MASK        : 0);
            mask |= (portStatus.contains(PortStatus.DTR_DSR)        ? PORT_STATUS_MASK_DTR_DSR_MASK        : 0);
            mask |= (portStatus.contains(PortStatus.RING_INDICATOR) ? PORT_STATUS_MASK_RING_INDICATOR_MASK : 0);
            mask |= (portStatus.contains(PortStatus.CARRIER_DETECT) ? PORT_STATUS_MASK_CARRIER_DETECT_MASK : 0);
        }

        return sendPortStatusNative(portHandle, mask, breakSignal, breakTimeout);
    }

    /**
     * Retrieves a list of information about services on the remote device
     * which are hosted on an RFCOMM port (to which SPPM can establish
     * connections).
     *
     * <i>Note:</i> This method acts on the copy of the remote device's
     * service records cached by the Platform Manager service. If there are no
     * cached records available, no results will be returned. Us the DEVM
     * (Device Manager) API to determine whether cached records are available
     * and to request that records be updated.
     *
     * @param remoteDeviceAddress
     *        Bluetooth address of the remote device.
     * @return
     *        A list of available services on the remote device, built from
     *        the locally cached copy of the device's service records, or
     *        {@code null} if an error occurs.
     */
    public ServiceRecordInformation[] queryRemoteDeviceServices(BluetoothAddress remoteDeviceAddress) {
        int                        result;
        byte[]                     address      = remoteDeviceAddress.internalByteArray();
        int[][]                    handles      = { null };
        long[][]                   serviceTypes = { null, null };
        int[][]                    ports        = { null };
        String[][]                 names        = { null };
        ServiceRecordInformation[] recordInfo;

        result = queryRemoteDeviceServicesNative(address[0], address[1], address[2], address[3], address[4], address[5], handles, serviceTypes, ports, names);

        if((result == 0) && (handles[0] != null) && (serviceTypes[0] != null) && (serviceTypes[1] != null) && (ports[0] != null) && (names[0] != null)) {
            recordInfo = new ServiceRecordInformation[handles[0].length];

            for(int i = 0; i < handles[0].length; i++)
                recordInfo[i] = new ServiceRecordInformation(handles[0][i], new UUID(serviceTypes[0][i], serviceTypes[1][i]), ports[0][i], names[0][i]);
        } else {
            recordInfo = null;
        }

        return recordInfo;
    }

    /**
     * Enables MFi support in the Serial Port Profile Manager (SPPM).
     * <p>
     * MFi settings can only be configured once and are global in nature.
     * <p>
     * <i>Note:</i> Enabling MFi support with this method does not mean that MFi
     * will be available for all ports. Each port can specify whether MFi is
     * (allowed/requested) when it is (configured/opened).
     *
     * @param maximumReceivePacketSize
     *            The maximum allowed packet size.
     *
     * @param dataPacketTimeout
     *            The packet timeout (in milliseconds.)
     *
     * @param supportedLingoes
     *            A list of Lingo IDs representing the Lingoes supported by this
     *            accessory.
     *
     * @param accessoryInformation
     *            Information structure containing details identifying this
     *            device.
     *
     * @param supportedProtocols
     *            List of reverse-DNS strings representing the protocols
     *            supported by this accessory.
     *
     * @param bundleSeedID
     *            The Bundle Seed ID of this accessory. This is provided by
     *            Apple, Inc.
     *
     * @param fidTokens
     *            A list of additional FID Tokens for data and roles not covered
     *            by the other parameters. Generally, this will include any
     *            optional FID Tokens.
     *
     * @return Zero if successful or a negative return error code if there was
     *         an error.
     */
    public int configureMFiSettings(int maximumReceivePacketSize, int dataPacketTimeout, byte[] supportedLingoes, MFiAccessoryInfo accessoryInformation, String[] supportedProtocols, String bundleSeedID, MFiFIDTokenValue[] fidTokens) {
        int      result;
        int[]    fidTypes;
        int[]    fidSubtypes;
        byte[][] fidDataBuffers;

        if(fidTokens != null) {
            fidTypes       = new int[fidTokens.length];
            fidSubtypes    = new int[fidTokens.length];
            fidDataBuffers = new byte[fidTokens.length][];

            for(int i = 0; i < fidTokens.length; i++) {
                fidTypes[i]       = fidTokens[i].type;
                fidSubtypes[i]    = fidTokens[i].subtype;
                fidDataBuffers[i] = fidTokens[i].data;
            }
        } else {
            fidTypes       = null;
            fidSubtypes    = null;
            fidDataBuffers = null;
        }

        result = configureMFiSettingsNative(maximumReceivePacketSize,
                                            dataPacketTimeout,
                                            supportedLingoes,
                                            (accessoryInformation != null ? accessoryInformation.accessoryCapabilitiesBitmask : 0),
                                            (accessoryInformation != null ? accessoryInformation.accessoryName                : null),
                                            (accessoryInformation != null ? accessoryInformation.accessoryFirmwareVersion     : 0),
                                            (accessoryInformation != null ? accessoryInformation.accessoryHardwareVersion     : 0),
                                            (accessoryInformation != null ? accessoryInformation.accessoryManufacturer        : null),
                                            (accessoryInformation != null ? accessoryInformation.accessoryModelNumber         : null),
                                            (accessoryInformation != null ? accessoryInformation.accessorySerialNumber        : null),
                                            (accessoryInformation != null ? accessoryInformation.accessoryRFCertification     : 0),
                                            supportedProtocols,
                                            null,
                                            bundleSeedID,
                                            fidTypes,
                                            fidSubtypes,
                                            fidDataBuffers,
                                            null,
                                            null,
                                            null,
                                            null);

        return result;
    }

    /**
     * Enables MFi support in the Serial Port Profile Manager (SPPM).
     * <p>
     * MFi settings can only be configured once and are global in nature.
     * <p>
     * This version of the method also enables support for the iAP v2 protocol
     * for MFi.
     * <p>
     * <i>Note:</i> Enabling MFi support with this method does not mean that MFi
     * will be available for all ports. Each port can specify whether MFi is
     * (allowed/requested) when it is (configured/opened).
     *
     * @param maximumReceivePacketSize
     *            The maximum allowed packet size.
     *
     * @param dataPacketTimeout
     *            The packet timeout (in milliseconds.)
     *
     * @param supportedLingoes
     *            A list of Lingo IDs representing the Lingoes supported by this
     *            accessory.
     *
     * @param accessoryInformation
     *            Information structure containing details identifying this
     *            device.
     *
     * @param supportedProtocols
     *            List of reverse-DNS strings representing the protocols
     *            supported by this accessory.
     *
     * @param bundleSeedID
     *            The Bundle Seed ID of this accessory. This is provided by
     *            Apple, Inc.
     *
     * @param fidTokens
     *            A list of additional FID Tokens for data and roles not covered
     *            by the other parameters. Generally, this will include any
     *            optional FID Tokens.
     *
     * @return Zero if successful or a negative return error code if there was
     *         an error.
     */
    public int configureMFiSettings(int maximumReceivePacketSize, int dataPacketTimeout, byte[] supportedLingoes, MFiAccessoryInfo accessoryInformation, MFiProtocol[] supportedProtocols, String bundleSeedID, MFiFIDTokenValue[] fidTokens, String currentLanguage, String[] supportedLanguages) {
        return configureMFiSettings(maximumReceivePacketSize, dataPacketTimeout, supportedLingoes, accessoryInformation, supportedProtocols, bundleSeedID, fidTokens, currentLanguage, supportedLanguages, null, null);
    }

    /**
     * Enables MFi support in the Serial Port Profile Manager (SPPM).
     * <p>
     * MFi settings can only be configured once and are global in nature.
     * <p>
     * This version of the method also enables support for the iAP v2 protocol
     * for MFi.
     * <p>
     * <i>Note:</i> Enabling MFi support with this method does not mean that MFi
     * will be available for all ports. Each port can specify whether MFi is
     * (allowed/requested) when it is (configured/opened).
     *
     * @param maximumReceivePacketSize
     *            The maximum allowed packet size.
     *
     * @param dataPacketTimeout
     *            The packet timeout (in milliseconds.)
     *
     * @param supportedLingoes
     *            A list of Lingo IDs representing the Lingoes supported by this
     *            accessory.
     *
     * @param accessoryInformation
     *            Information structure containing details identifying this
     *            device.
     *
     * @param supportedProtocols
     *            List of reverse-DNS strings representing the protocols
     *            supported by this accessory.
     *
     * @param bundleSeedID
     *            The Bundle Seed ID of this accessory. This is provided by
     *            Apple, Inc.
     *
     * @param fidTokens
     *            A list of additional FID Tokens for data and roles not covered
     *            by the other parameters. Generally, this will include any
     *            optional FID Tokens.
     *
     * @param controlMessageIDsSent
     *            A list of 16-bit Control Message IDs which may be sent by the
     *            accessory.
     *
     * @param controlMessageIDsReceived
     *            A list of 16-bit Control Message IDs which may be received by
     *            the accessory.
     *
     * @return Zero if successful or a negative return error code if there was
     *         an error.
     */
    public int configureMFiSettings(int maximumReceivePacketSize, int dataPacketTimeout, byte[] supportedLingoes, MFiAccessoryInfo accessoryInformation, MFiProtocol[] supportedProtocols, String bundleSeedID, MFiFIDTokenValue[] fidTokens, String currentLanguage, String[] supportedLanguages, short[] controlMessageIDsSent, short[] controlMessageIDsReceived) {
        int      result;
        int[]    fidTypes;
        int[]    fidSubtypes;
        byte[][] fidDataBuffers;
        int[]    matchActions;
        String[] protocols;

        if(fidTokens != null) {
            fidTypes       = new int[fidTokens.length];
            fidSubtypes    = new int[fidTokens.length];
            fidDataBuffers = new byte[fidTokens.length][];

            for(int i = 0; i < fidTokens.length; i++) {
                fidTypes[i]       = fidTokens[i].type;
                fidSubtypes[i]    = fidTokens[i].subtype;
                fidDataBuffers[i] = fidTokens[i].data;
            }
        } else {
            fidTypes       = null;
            fidSubtypes    = null;
            fidDataBuffers = null;
        }

        protocols    = new String[supportedProtocols.length];
        matchActions = new int[supportedProtocols.length];

        for(int i = 0; i < supportedProtocols.length; i++) {
            protocols[i] = supportedProtocols[i].name;

            switch(supportedProtocols[i].matchAction) {
            case NONE:
                matchActions[i] = MATCH_ACTION_NONE;
                break;
            case AUTOMATIC_SEARCH:
                matchActions[i] = MATCH_ACTION_AUTOMATIC_SEARCH;
                break;
            case SEARCH_BUTTON_ONLY:
                matchActions[i] = MATCH_ACTION_SEARCH_BUTTON_ONLY;
                break;
            }
        }

        result = configureMFiSettingsNative(maximumReceivePacketSize,
                                            dataPacketTimeout,
                                            supportedLingoes,
                                            (accessoryInformation != null ? accessoryInformation.accessoryCapabilitiesBitmask : 0),
                                            (accessoryInformation != null ? accessoryInformation.accessoryName                : null),
                                            (accessoryInformation != null ? accessoryInformation.accessoryFirmwareVersion     : 0),
                                            (accessoryInformation != null ? accessoryInformation.accessoryHardwareVersion     : 0),
                                            (accessoryInformation != null ? accessoryInformation.accessoryManufacturer        : null),
                                            (accessoryInformation != null ? accessoryInformation.accessoryModelNumber         : null),
                                            (accessoryInformation != null ? accessoryInformation.accessorySerialNumber        : null),
                                            (accessoryInformation != null ? accessoryInformation.accessoryRFCertification     : 0),
                                            protocols,
                                            matchActions,
                                            bundleSeedID,
                                            fidTypes,
                                            fidSubtypes,
                                            fidDataBuffers,
                                            currentLanguage,
                                            supportedLanguages,
                                            controlMessageIDsSent,
                                            controlMessageIDsReceived);

        return result;
    }

    /**
     * Determines whether the port is operating in SPP or MFi mode.
     * <p>
     * <i>Note:</i>  This function can only be called on ports that are
     * currently connected.
     *
     * @return
     *         {@link ConnectionType#MFI} if the port is operating
     *         in MFi mode, {@link ConnectionType#SPP} if the port is
     *         operating in SPP mode, or {@code NULL} if there was an
     *         error.
     */
    public ConnectionType queryConnectionType() {
        int            result;
        int[]          type = new int[1];

        result = queryConnectionTypeNative(portHandle, type);

        if(result == 0)
            return ((type[0] == CONNECTION_TYPE_MFI) ? ConnectionType.MFI : ConnectionType.SPP);
        else
            return null;
    }

    /**
     * Responds to an incoming MFi Open Session request.
     *
     * @param sessionID
     *        The session ID associated with the request.
     *
     * @param accept
     *        {@code boolean} value indicating whether or not
     *        to accept the specified session.
     *
     * @return
     *         Zero, if the response was processed successfully, or a
     *         negative return code if there was an error.
     */
    public int openSessionRequestResponse(int sessionID, boolean accept) {
        return openSessionRequestResponseNative(portHandle, sessionID, accept);
    }

    /**
     * Sends preformatted session data packets to a currently connected
     * session, based on the session ID.
     * @param sessionID
     *        The desired session ID to send data to.
     *
     * @param sessionData
     *        The data to send.
     *
     * @return
     *         A positive nonzero PacketID value if the operation was
     *         successful or a negative return error code if there was
     *         an error.
     *         <p>
     *         <i>Note:</i> if successful, the returned value is a
     *         PacketID that can be used to track the confirmation
     *         status of the packet.
     */
    public int sendSessionData(int sessionID, byte[] sessionData) {
        return sendSessionDataNative(portHandle, sessionID, sessionData);
    }

    /**
     * Sends preformatted non-session data packets to a currently
     * connected MFi device.
     * <p>
     * <i>Note:</i> Non-session data is considered to be any protocol
     * data that is represented by one of the defined Lingoes, including
     * the General Lingo.
     *
     * @param lingoID
     *        The Lingo ID of the packet to send.
     *
     * @param commandID
     *        The Command ID of the packet to send.
     *
     * @param transactionID
     *        The Transaction ID of the packet to send.
     *
     * @param data
     *        The actual packet data to send.
     *
     * @return
     *         A positive nonzero PacketID value if the operation was
     *         successful or a negative return error code if there was
     *         an error.
     *         <p>
     *         <i>Note:</i> if successful, the returned value is a
     *         PacketID that can be used to track the confirmation
     *         status of the packet.
     */
    public int sendNonSessionData(int lingoID, int commandID, int transactionID, byte[] data) {
        return sendNonSessionDataNative(portHandle, lingoID, commandID, transactionID, data);
    }

    /**
     * Cancels the transmission of a previously queued packet.
     * <p>
     * <i>Note:</i> This method can cancel packets that have already
     * been queued for transmit, but cannot cancel packets that have
     * already been transmitted.
     *
     * @param packetID
     *        The Packet ID of the packet to cancel.
     *
     * @return Zero if successful, or a negative return error code.
     *
     * @deprecated Packet cancellation is incompatible with iAPv2.
     */
    @Deprecated
    public int cancelPacket(int packetID) {
        return cancelPacketNative(portHandle, packetID);
    }

    /**
     * Sends preformatted command message data packets to a currently
     * connected MFi device.
     *
     * @param messageID
     *        The Control Message ID of the packet to send.
     *
     * @param data
     *        The actual packet data to send.
     *
     * @return
     *         A positive nonzero PacketID value if the operation was
     *         successful or a negative return error code if there was
     *         an error.
     */
    public int sendControlMessage(short messageID, byte[] data) {
        return sendControlMessageNative(portHandle, messageID, data);
    }

    /* Event Callback Handlers. */
    protected void disconnectedEvent(int portHandle) {
        assert(this.portHandle == portHandle) : "Received an event for the incorrect port";

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
           try {
               callbackHandler.disconnectedEvent();
           } catch(Exception e) {
               e.printStackTrace();
           }
        }
    }

    protected void lineStatusChangedEvent(int portHandle, int lineStatusMask) {
        assert(this.portHandle == portHandle) : "Received an event for the incorrect port";

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            EnumSet<LineStatus> status = EnumSet.noneOf(LineStatus.class);

            if((lineStatusMask & LINE_STATUS_MASK_OVERRUN_ERROR_MASK) == LINE_STATUS_MASK_OVERRUN_ERROR_MASK)
                status.add(LineStatus.OVERRUN_ERROR);
            if((lineStatusMask & LINE_STATUS_MASK_PARITY_ERROR_MASK) == LINE_STATUS_MASK_PARITY_ERROR_MASK)
                status.add(LineStatus.PARITY_ERROR);
            if((lineStatusMask & LINE_STATUS_MASK_FRAMING_ERROR_MASK) == LINE_STATUS_MASK_FRAMING_ERROR_MASK)
                status.add(LineStatus.FRAMING_ERROR);

            try {
                callbackHandler.lineStatusChangedEvent(status);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void portStatusChangedEvent(int portHandle, int portStatusMask, boolean breakSignal, int breakTimeout) {
        assert(this.portHandle == portHandle) : "Received an event for the incorrect port";

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            EnumSet<PortStatus> status = EnumSet.noneOf(PortStatus.class);

            if((portStatusMask & PORT_STATUS_MASK_RTS_CTS_MASK) == PORT_STATUS_MASK_RTS_CTS_MASK)
                status.add(PortStatus.RTS_CTS);
            if((portStatusMask & PORT_STATUS_MASK_DTR_DSR_MASK) == PORT_STATUS_MASK_DTR_DSR_MASK)
                status.add(PortStatus.DTR_DSR);
            if((portStatusMask & PORT_STATUS_MASK_RING_INDICATOR_MASK) == PORT_STATUS_MASK_RING_INDICATOR_MASK)
                status.add(PortStatus.RING_INDICATOR);
            if((portStatusMask & PORT_STATUS_MASK_CARRIER_DETECT_MASK) == PORT_STATUS_MASK_CARRIER_DETECT_MASK)
                status.add(PortStatus.CARRIER_DETECT);

            try {
                callbackHandler.portStatusChangedEvent(status, breakSignal, breakTimeout);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void dataReceivedEvent(int portHandle, int dataLength) {
        assert(this.portHandle == portHandle) : "Received an event for the incorrect port";

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.dataReceivedEvent(dataLength);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void transmitBufferEmptyEvent(int portHandle) {
        assert(this.portHandle == portHandle) : "Received an event for the incorrect port";

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.transmitBufferEmptyEvent();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void idpsStatusEvent(int portHandle, int idpsState, int idpsStatus) {
        assert(this.portHandle == portHandle) : "Received an event for the incorrect port";

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            IDPSState  state  = null;
            IDPSStatus status = null;

            switch(idpsState) {
            case IDPS_STATE_START_IDENTIFICATION_REQUEST:
                state = IDPSState.START_IDENTIFICATION_REQUEST;
                break;
            case IDPS_STATE_START_IDENTIFICATION_PROCESS:
                state = IDPSState.START_IDENTIFICATION_PROCESS;
                break;
            case IDPS_STATE_IDENTIFICATION_PROCESS:
                state = IDPSState.IDENTIFICATION_PROCESS;
                break;
            case IDPS_STATE_IDENTIFICATION_PROCESS_COMPLETE:
                state = IDPSState.IDENTIFICATION_PROCESS_COMPLETE;
                break;
            case IDPS_STATE_START_AUTHENTICATION_PROCESS:
                state = IDPSState.START_AUTHENTICATION_PROCESS;
                break;
            case IDPS_STATE_AUTHENTICATION_PROCESS:
                state = IDPSState.AUTHENTICATION_PROCESS;
                break;
            case IDPS_STATE_AUTHENTICATION_PROCESS_COMPLETE:
                state = IDPSState.AUTHENTICATION_PROCESS_COMPLETE;
                break;
            }

            switch(idpsStatus) {
            case IDPS_STATUS_SUCCESS:
                status = IDPSStatus.SUCCESS;
                break;
            case IDPS_STATUS_ERROR_RETRYING:
                status = IDPSStatus.ERROR_RETRYING;
                break;
            case IDPS_STATUS_TIMEOUT_HALTING:
                status = IDPSStatus.TIMEOUT_HALTING;
                break;
            case IDPS_STATUS_GENERAL_FAILURE:
                status = IDPSStatus.GENERAL_FAILURE;
                break;
            case IDPS_STATUS_PROCESS_FAILURE:
                status = IDPSStatus.PROCESS_FAILURE;
                break;
            case IDPS_STATUS_PROCESS_TIMEOUT_RETRYING:
                status = IDPSStatus.PROCESS_TIMEOUT_RETRYING;
                break;
            }

            if((state != null) && (status != null)) {
                try {
                    callbackHandler.idpsStatusEvent(state, status);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void sessionOpenRequestEvent(int portHandle, int maximumTransmitPacket, int maximumReceivePacket, int sessionID, int protocolIndex) {
        assert(this.portHandle == portHandle) : "Received an event for the incorrect port";

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.sessionOpenRequestEvent(maximumTransmitPacket, maximumReceivePacket, sessionID, protocolIndex);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void sessionCloseEvent(int portHandle, int sessionID) {
        assert(this.portHandle == portHandle) : "Received an event for the incorrect port";

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.sessionCloseEvent(sessionID);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void sessionDataReceivedEvent(int portHandle, int sessionID, byte[] sessionData) {
        assert(this.portHandle == portHandle) : "Received an event for the incorrect port";

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.sessionDataReceivedEvent(sessionID, sessionData);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void sessionDataConfirmationEvent(int portHandle, int sessionID, int packetID, int status) {
        assert(this.portHandle == portHandle) : "Received an event for the incorrect port";

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            DataConfirmationStatus confStatus = null;

            switch(status) {
            case SPPM_DATA_CONFIRMATION_STATUS_PACKET_SENT:
                confStatus = DataConfirmationStatus.SENT;
                break;
            case SPPM_DATA_CONFIRMATION_STATUS_PACKET_ACKNOWLEDGED:
                confStatus = DataConfirmationStatus.ACKNOWLEDGED;
                break;
            case SPPM_DATA_CONFIRMATION_STATUS_PACKET_FAILED:
                confStatus = DataConfirmationStatus.FAILED;
                break;
            case SPPM_DATA_CONFIRMATION_STATUS_PACKET_CANCELED:
                confStatus = DataConfirmationStatus.CANCELED;
                break;
            }

            try {
                callbackHandler.sessionDataConfirmationEvent(sessionID, packetID, confStatus);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void nonSessionDataReceivedEvent(int portHandle, int lingoID, int commandID, byte[] data) {
        assert(this.portHandle == portHandle) : "Received an event for the incorrect port";

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.nonSessionDataReceivedEvent(lingoID, commandID, data);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void nonSessionDataConfirmationEvent(int portHandle, int packetID, int transactionID, int status) {
        assert(this.portHandle == portHandle) : "Received an event for the incorrect port";

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            DataConfirmationStatus confStatus = null;

            switch(status) {
            case SPPM_DATA_CONFIRMATION_STATUS_PACKET_SENT:
                confStatus = DataConfirmationStatus.SENT;
                break;
            case SPPM_DATA_CONFIRMATION_STATUS_PACKET_ACKNOWLEDGED:
                confStatus = DataConfirmationStatus.ACKNOWLEDGED;
                break;
            case SPPM_DATA_CONFIRMATION_STATUS_PACKET_FAILED:
                confStatus = DataConfirmationStatus.FAILED;
                break;
            case SPPM_DATA_CONFIRMATION_STATUS_PACKET_CANCELED:
                confStatus = DataConfirmationStatus.CANCELED;
                break;
            }

            try {
                callbackHandler.nonSessionDataConfirmationEvent(packetID, transactionID, confStatus);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void unhandledControlMessageReceivedEvent(int portHandle, short controlMessageID, byte[] data) {
        assert(this.portHandle == portHandle) : "Received an event for the incorrect port";

        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.unhandledControlMessageReceivedEvent(controlMessageID, data);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* Event handlers specific to a certain manager type. */
    protected void connectionRequestEvent(int portHandle, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        throw new UnsupportedOperationException("Event received which is not supporeted by this Serial Port Profile Manager type");
    }

    protected void connectedEvent(int portHandle, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int connectionType) {
        throw new UnsupportedOperationException("Event received which is not supporeted by this Serial Port Profile Manager type");
    }

    protected void connectionStatusEvent(int portHandle, int connectionStatus, int connectionType) {
        throw new UnsupportedOperationException("Event received which is not supporeted by this Serial Port Profile Manager type");
    }

    /**
     * Receiver for event notifications which are common among both SPPM
     * Client and Server Managers.
     * <p>
     * It is guaranteed that no two event methods will be invoked
     * simultaneously. However, because of this guarantee, the
     * implementation of each event handler must be as efficient
     * as possible, as subsequent events will not be sent until the
     * in-progress event handler returns.  Furthermore, event
     * handlers must not block and wait on conditions that can only
     * be satisfied by receiving other events.
     * <p>
     * Implementors should also note that events are sent from a
     * thread context owned by the BluetopiaPM API, so standard
     * locking practices should be observed for data sharing.
     */
    public interface EventCallback {
        /**
         * Invoked when an existing connection is closed.
         *
         * @see SPPM#disconnectRemoteDevice
         */
        public void disconnectedEvent();

        /**
         * Invoked when the line status of an opened port is changed.
         *
         * @param lineStatus
         *        The set of line status bits that were sent by the
         *        remote device.
         *
         * @see SPPM#sendLineStatus
         */
        public void lineStatusChangedEvent(EnumSet<LineStatus> lineStatus);

        /**
         * Invoked when the status of an opened port is changed.
         *
         * @param portStatus
         *        The set of port status bits that are active on the
         *        remote device.
         *
         * @param breakSignal
         *        {@code True}, if a break has been sent by the remote
         *        device.
         *
         * @param breakTimeout
         *        The length, in milliseconds, of the break sent by the
         *        remote device. If {@code breakSignal} is
         *        {@code false}, this value should be ignored.
         *
         * @see SPPM#sendPortStatus
         */
        public void portStatusChangedEvent(EnumSet<PortStatus> portStatus, boolean breakSignal, int breakTimeout);

        /**
         * Invoked when data is received on an opened port.
         * <p>
         * The data will remain in an internal buffer until the data is
         * read with a call to {@link SPPM#readData}.
         *
         * @param dataLength
         *        The length of the data received. The actual amount of
         *        data available to be read may be greater if multiple
         *        Data Received Events have been received since the last
         *        read.
         */
        public void dataReceivedEvent(int dataLength);

        /**
         * Invoked when the transmit buffer is emptied.
         */
        public void transmitBufferEmptyEvent();

        /**
         * Invoked when an IDPS status event is received.
         *
         * @param idpsState
         *        The relevant {@link IDPSState}.
         *
         * @param idpsStatus
         *        The status information.
         */
        public void idpsStatusEvent(IDPSState idpsState, IDPSStatus idpsStatus);

        /**
         * Invoked when a session open request is received.
         *
         * @param maximumTransmitPacket
         *        The maximum transmit packet size.
         *
         * @param maximumReceivePacket
         *        The maximum receive packet size.
         *
         * @param sessionID
         *        The requested session ID.
         *
         * @param protocolIndex
         *        The protocol index.
         */
        public void sessionOpenRequestEvent(int maximumTransmitPacket, int maximumReceivePacket, int sessionID, int protocolIndex);

        /**
         * Invoked when a session has been closed.
         *
         * @param sessionID The session ID.
         */
        public void sessionCloseEvent(int sessionID);

        /**
         * Invoked when session data is received.
         *
         * @param sessionID
         *        The session ID associated with the packet.
         *
         * @param sessionData
         *        The received data.
         */
        public void sessionDataReceivedEvent(int sessionID, byte[] sessionData);

        /**
         * Invoked to confirm the status of a session packet.
         *
         * @param sessionID
         *        The session ID associated with the packet.
         *
         * @param packetID
         *        The packet ID, as returned from the original call to
         *        {@code sendSessionData}.
         *
         * @param status
         *        The status of the packet.
         */
        public void sessionDataConfirmationEvent(int sessionID, int packetID, DataConfirmationStatus status);


        /**
         * Invoked when non-session data is received.
         *
         * @param lingoID
         *        The Lingo ID associated with the data.
         *
         * @param commandID
         *        The Command ID associated with the data.
         *
         * @param data
         *        The received data.
         */
        public void nonSessionDataReceivedEvent(int lingoID, int commandID, byte[] data);

        /**
         * Invoked to confirm the status of a non-session packet.
         *
         * @param packetID
         *        The packet ID, as returned from the original call to
         *        {@code sendNonSessionData}.
         *
         * @param transactionID
         *        The transaction ID of the packet.
         *
         * @param status
         *        The status of the packet.
         */
        public void nonSessionDataConfirmationEvent(int packetID, int transactionID, DataConfirmationStatus status);

        /**
         * Invoked when an unhandled Command Message is received.
         *
         * @param controlMessageID
         *        The type of control message.
         *
         * @param data
         *        The data sent with the message.
         */
        public void unhandledControlMessageReceivedEvent(short controlMessageID, byte[] data);
    }

    /**
     * Manager for a single SPPM client connection to a remote SPPM Server.
     */
    public static class SerialPortClientManager extends SPPM {

        /**
         * Constructs an instance of a Serial Port Client Manager.
         *
         * @param eventCallback
         *        The event callback to be registered with the Serial Port Client Manager.
         *
         * @throws ServerNotReachableException
         */
        public SerialPortClientManager(ClientEventCallback eventCallback) throws ServerNotReachableException {
            super(MANAGER_TYPE_CLIENT, eventCallback);
        }

        /* Client specific commands. */

        /**
         * Creates a connection to a remote Serial Port Server.
         * <p>
         * If {@code waitForConnection} is {@code true}, this call will
         * block until the connection status is received (that is, the
         * connection attempt is completed). If this parameter is
         * {@code false}, then this method will return immediately and
         * the connection status will be returned asynchronously by way
         * of a call to
         * {@link ClientEventCallback#connectionStatusEvent} on the
         * {@link ClientEventCallback} provided when this Manager was
         * constructed.
         *
         * @param remoteDeviceAddress
         *        Bluetooth address of the remote device.
         *
         * @param remotePortNumber
         *        The port of the Serial Port Server on the remote
         *        device.
         *
         * @param connectionFlags
         *        A set of flags which determine how the connection should
         *        be created.
         *
         * @param waitForConnection
         *        If {@code true}, the method will block until the
         *        connection attempt completes.
         *
         * @return Zero if successful or a negative return error code if
         *         the connection attempt could not be started. If
         *         {@code waitForConnection} is {@code true}, a positive
         *         (non-zero) value indicates that the connection
         *         attempt completed but the connection could not be
         *         established.
         *
         * @see ClientEventCallback#connectionStatusEvent
         */
        public int connectRemoteDevice(BluetoothAddress remoteDeviceAddress, int remotePortNumber, EnumSet<ConnectionFlags> connectionFlags, boolean waitForConnection) {
            int    flags;
            int    result;
            int[]  connectionStatus;
            byte[] address = remoteDeviceAddress.internalByteArray();

            flags = 0;
            if(connectionFlags != null) {
                flags |= (connectionFlags.contains(ConnectionFlags.REQUIRE_AUTHENTICATION) ? CONNECTION_FLAGS_REQUIRE_AUTHENTICATION : 0);
                flags |= (connectionFlags.contains(ConnectionFlags.REQUIRE_ENCRYPTION)     ? CONNECTION_FLAGS_REQUIRE_ENCRYPTION     : 0);
                flags |= (connectionFlags.contains(ConnectionFlags.MFI_REQUIRED)           ? CONNECTION_FLAGS_MFI_REQUIRED           : 0);
            }

            connectionStatus = (waitForConnection ? new int[1] : null);

            result = connectRemoteDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5], remotePortNumber, flags, connectionStatus);

            /*
             * Note the port handle and notify success
             */
            if(result > 0) {
                portHandle = result;

                result = (waitForConnection ? connectionStatus[0] : 0);
            }

            return result;
        }

        /* Client specific event handlers. */
        @Override
        protected void connectionStatusEvent(int portHandle, int connectionStatus, int connectionType) {
            assert(this.portHandle == portHandle) : "Received an event for the incorrect port";

            ConnectionType   type;
            ConnectionStatus status;
            EventCallback    callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                switch(connectionType) {
                case CONNECTION_TYPE_MFI:
                    type = ConnectionType.MFI;
                    break;
                case CONNECTION_TYPE_SPP:
                default:
                    type = ConnectionType.SPP;
                    break;
                }

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

                try {
                    ((ClientEventCallback)callbackHandler).connectionStatusEvent(status, type);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }



        /**
         * Receiver for event notifications which are common specific
         * to the Serial Port Client.
         * <p>
         * It is guaranteed that no two event methods will be invoked
         * simultaneously. However, because of this guarantee, the
         * implementation of each event handler must be as efficient
         * as possible, as subsequent events will not be sent until the
         * in-progress event handler returns.  Furthermore, event
         * handlers must not block and wait on conditions that can only
         * be satisfied by receiving other events.
         * <p>
         * Implementors should also note that events are sent from a
         * thread context owned by the BluetopiaPM API, so standard
         * locking practices should be observed for data sharing.
         */
        public interface ClientEventCallback extends EventCallback {

            /**
             * Invoked when an outgoing remote connection attempt
             * completes.
             *
             * @param status
             *        The status code of the remote connection attempt.
             *
             * @param type
             *        The type of connection.
             *
             * @see SerialPortClientManager#connectRemoteDevice
             */
            public void connectionStatusEvent(ConnectionStatus status, ConnectionType type);
        }

        private final static int CONNECTION_FLAGS_REQUIRE_AUTHENTICATION = 0x00000001;
        private final static int CONNECTION_FLAGS_REQUIRE_ENCRYPTION     = 0x00000002;
        private final static int CONNECTION_FLAGS_MFI_REQUIRED           = 0x00000004;

        /**
         * Optional flags to control behavior when a remote connection
         * is established by the local service.
         */
        public enum ConnectionFlags {
            /**
             * Require the Bluetooth link to be authenticated. This
             * requires that the devices are paired and will
             * automatically initiate the pairing process if required.
             */
            REQUIRE_AUTHENTICATION,

            /**
             * Require the Bluetooth link to be encrypted.
             */
            REQUIRE_ENCRYPTION,

            /**
             * Require the usage of MFi.
             */
            MFI_REQUIRED,
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
         * @see ClientEventCallback#connectionStatusEvent
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
    }

    /**
     * Manager for a local SPPM Server Port.
     */
    public static class SerialPortServerManager extends SPPM {
        /**
         * Constructs an instance of a Serial Port Server Manager on an
         * available port.
         * <p>
         * This constructor attempts to register a Server Port with the given
         * port number.
         *
         * @param eventCallback
         *            The event callback to be registered with the Serial Port
         *            Server Manager.
         *
         * @param connectionFlags
         *            The {@link IncomingConnectionFlags} that dictate how
         *            incoming connections should be handled. NOTE: If {@link IncomingConnectionFlags#MFI_REQUIRED}
         *            is supplied, {@link IncomingConnectionFlags#MFI_ALLOWED} is overridden.
         *
         * @throws ServerNotReachableException
         *             Thrown when the BluetopiaPM server is not available.
         *
         * @throws BluetopiaPMException
         *             Thrown when the server port cannot be registered.
         *
         * @throws IllegalArgumentException
         *             Thrown if an illegal argument is supplied.
         */
        public SerialPortServerManager(ServerEventCallback eventCallback, EnumSet<IncomingConnectionFlags> connectionFlags) throws ServerNotReachableException, BluetopiaPMException, IllegalArgumentException {
            this(eventCallback, -1, connectionFlags);
        }

        /**
         * Constructs an instance of a Serial Port Server Manager on an
         * available port.
         * <p>
         * This constructor attempts to register a Server Port with the given
         * port number as well as a Service Record for the port with the given
         * service name.
         *
         * @param eventCallback
         *            The event callback to be registered with the Serial Port
         *            Server Manager.
         *
         * @param connectionFlags
         *            The {@link IncomingConnectionFlags} that dictate how
         *            incoming connections should be handled. NOTE: If {@link IncomingConnectionFlags#MFI_REQUIRED}
         *            is supplied, {@link IncomingConnectionFlags#MFI_ALLOWED} is overridden.
         *
         * @param serviceClassIDs
         *            (Optional) List of Service Class IDs representing this
         *            service, in order of most specific to most general class.
         *            Note: If {@link IncomingConnectionFlags#MFI_REQUIRED} is supplied,
         *            no custom service class IDs are allowed, and a {@link IllegalArgumentException} is thrown.
         *
         * @param additionalProtocolIDs
         *            (Optional) List of UUIDs for Bluetooth protocols used by
         *            the this service, in addition to the RFCOMM and L2CAP
         *            protocols. Typically, this is only needed to indicate the
         *            use of the OBEX protocol (UUID:
         *            "00000008-0000-1000-8000-00805F9B34FB").
         *
         * @param serviceName
         *            The service name that should be provided in the SDP
         *            record.
         *
         * @throws ServerNotReachableException
         *             Thrown when the BluetopiaPM server is not available.
         *
         * @throws BluetopiaPMException
         *             Thrown when the server port cannot be registered or the
         *             SDP record could not be registered.
         *
         * @throws IllegalArgumentException
         *             Thrown if an illegal argument is supplied.
         */
        public SerialPortServerManager(ServerEventCallback eventCallback, EnumSet<IncomingConnectionFlags> connectionFlags, UUID serviceClassIDs[], UUID additionalProtocolIDs[], String serviceName) throws ServerNotReachableException, BluetopiaPMException, IllegalArgumentException {
            this(eventCallback, -1, connectionFlags, serviceClassIDs, additionalProtocolIDs, serviceName);
        }

        /**
         * Constructs an instance of a Serial Port Server Manager.
         * <p>
         * This constructor attempts to register a Server Port with the given
         * port number.
         *
         * @param eventCallback
         *            The event callback to be registered with the Serial Port
         *            Server Manager.
         *
         * @param portNumber
         *            The port number to be opened.
         *
         * @param connectionFlags
         *            The {@link IncomingConnectionFlags} that dictate how
         *            incoming connections should be handled. NOTE: If {@link IncomingConnectionFlags#MFI_REQUIRED}
         *            is supplied, {@link IncomingConnectionFlags#MFI_ALLOWED} is overridden.
         *
         * @throws ServerNotReachableException
         *             Thrown when the BluetopiaPM server is not available.
         *
         * @throws BluetopiaPMException
         *             Thrown when the server port cannot be registered.
         *
         * @throws IllegalArgumentException
         *             Thrown if an illegal argument is supplied.
         */
        public SerialPortServerManager(ServerEventCallback eventCallback, int portNumber, EnumSet<IncomingConnectionFlags> connectionFlags) throws ServerNotReachableException, BluetopiaPMException, IllegalArgumentException {
            super(MANAGER_TYPE_SERVER, eventCallback);

            int flags;
            int result;

            flags = 0;

            if(connectionFlags != null) {
                flags |= (connectionFlags.contains(IncomingConnectionFlags.REQUIRE_AUTHORIZATION)  ? INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION  : 0);
                flags |= (connectionFlags.contains(IncomingConnectionFlags.REQUIRE_AUTHENTICATION) ? INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION : 0);
                flags |= (connectionFlags.contains(IncomingConnectionFlags.REQUIRE_ENCRYPTION)     ? INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION     : 0);
                flags |= (connectionFlags.contains(IncomingConnectionFlags.MFI_ALLOWED)            ? INCOMING_CONNECTION_FLAGS_MFI_ALLOWED            : 0);
                flags |= (connectionFlags.contains(IncomingConnectionFlags.MFI_REQUIRED)           ? INCOMING_CONNECTION_FLAGS_MFI_REQUIRED           : 0);
            }

            result = registerServerPortNative(portNumber,flags);

            if(result > 0) {
                portHandle = result;

                if((result = registerSerialPortSDPRecordNative(portHandle, null, null, null, null, null)) < 0) {
                    unRegisterServerPortNative(portHandle);

                    if(result == NATIVE_ERROR_INVALID_PARAMETER)
                    	throw new IllegalArgumentException("Illegal Argument for SerialPortServerManager");
                    else
                    	throw new BluetopiaPMException("Unable to register SDP record for SPP server") {};
                }
            } else {
                throw new BluetopiaPMException("Unable to register SPP server port") {};
            }
        }

        /**
         * Constructs an instance of a Serial Port Server Manager.
         * <p>
         * This constructor attempts to register a Server Port with the given
         * port number as well as a Service Record for the port with the given
         * service name.
         *
         * @param eventCallback
         *            The event callback to be registered with the Serial Port
         *            Server Manager.
         *
         * @param portNumber
         *            The port number to be opened.
         *
         * @param connectionFlags
         *            The {@link IncomingConnectionFlags} that dictate how
         *            incoming connections should be handled. NOTE: If {@link IncomingConnectionFlags#MFI_REQUIRED}
         *            is supplied, {@link IncomingConnectionFlags#MFI_ALLOWED} is overridden.
         *
         * @param serviceClassIDs
         *            (Optional) List of Service Class IDs representing this
         *            service, in order of most specific to most general class.
         *            Note: If {@link IncomingConnectionFlags#MFI_REQUIRED} is supplied,
         *            no custom service class IDs are allowed, and a {@link IllegalArgumentException} is thrown.
         *
         * @param additionalProtocolIDs
         *            (Optional) List of UUIDs for Bluetooth protocols used by
         *            the this service, in addition to the RFCOMM and L2CAP
         *            protocols. Typically, this is only needed to indicate the
         *            use of the OBEX protocol (UUID:
         *            "00000008-0000-1000-8000-00805F9B34FB").
         *
         * @param serviceName
         *            The service name that should be provided in the SDP
         *            record.
         *
         * @throws ServerNotReachableException
         *             Thrown when the BluetopiaPM server is not available.
         *
         * @throws BluetopiaPMException
         *             Thrown when the server port cannot be registered or the
         *             SDP record could not be registered.
         *
         * @throws IllegalArgumentException
         *             Thrown if an illegal argument is supplied.
         */
        public SerialPortServerManager(ServerEventCallback eventCallback, int portNumber, EnumSet<IncomingConnectionFlags> connectionFlags, UUID serviceClassIDs[], UUID additionalProtocolIDs[], String serviceName) throws ServerNotReachableException, BluetopiaPMException, IllegalArgumentException {
            super(MANAGER_TYPE_SERVER, eventCallback);

            int    flags;
            int    result;
            long[] serviceClassIDsHigh;
            long[] serviceClassIDsLow;
            long[] protocolsHigh;
            long[] protocolsLow;

            flags = 0;

            if(connectionFlags != null) {
                flags |= (connectionFlags.contains(IncomingConnectionFlags.REQUIRE_AUTHORIZATION)  ? INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION  : 0);
                flags |= (connectionFlags.contains(IncomingConnectionFlags.REQUIRE_AUTHENTICATION) ? INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION : 0);
                flags |= (connectionFlags.contains(IncomingConnectionFlags.REQUIRE_ENCRYPTION)     ? INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION     : 0);
                flags |= (connectionFlags.contains(IncomingConnectionFlags.MFI_ALLOWED)            ? INCOMING_CONNECTION_FLAGS_MFI_ALLOWED            : 0);
                flags |= (connectionFlags.contains(IncomingConnectionFlags.MFI_REQUIRED)           ? INCOMING_CONNECTION_FLAGS_MFI_REQUIRED           : 0);
            }

            result = registerServerPortNative(portNumber,flags);

            if(result > 0)
                portHandle = result;
            else
                throw new BluetopiaPMException("Unable to register SPP server port") {};

            if(serviceClassIDs != null) {
                serviceClassIDsHigh = new long[serviceClassIDs.length];
                serviceClassIDsLow  = new long[serviceClassIDs.length];

                for(int i = 0; i < serviceClassIDs.length; i++) {
                    serviceClassIDsHigh[i] = serviceClassIDs[i].getMostSignificantBits();
                    serviceClassIDsLow[i]  = serviceClassIDs[i].getLeastSignificantBits();
                }
            } else {
                serviceClassIDsHigh = null;
                serviceClassIDsLow  = null;
            }

            if(additionalProtocolIDs != null) {
                protocolsHigh = new long[additionalProtocolIDs.length];
                protocolsLow  = new long[additionalProtocolIDs.length];

                for(int i = 0; i < additionalProtocolIDs.length; i++) {
                    protocolsHigh[i] = additionalProtocolIDs[i].getMostSignificantBits();
                    protocolsLow[i]  = additionalProtocolIDs[i].getLeastSignificantBits();
                }
            } else {
                protocolsHigh = null;
                protocolsLow  = null;
            }

            if((result = registerSerialPortSDPRecordNative(portHandle, serviceClassIDsHigh, serviceClassIDsLow, protocolsHigh, protocolsLow, serviceName)) < 0) {
                unRegisterServerPortNative(portHandle);

                if(result == NATIVE_ERROR_INVALID_PARAMETER)
                	throw new IllegalArgumentException("Illegal Argument for SerialPortServerManager");
                else
                	throw new BluetopiaPMException("Unable to register SDP record for SPP server") {};
            }
        }

        /**
         * Responds to a request to open the local Server Port.
         *
         * @param acceptConnection Determines whether to accept or
         * reject the connection.
         *
         * @return Zero if successful or a negative return
         *         error code if there was an error.
         *
         * @see ServerEventCallback#connectionRequestEvent
         */
        public int connectionRequestResponse(boolean acceptConnection) {
            return connectionRequestResponseNative(portHandle, acceptConnection);
        }

        /**
         * Overrides {@link SPPM#dispose()} to make sure the Server Port
         * is unregistered.
         */
        @Override
        public void dispose() {
            unRegisterServerPortNative(portHandle);
            super.dispose();
        }

        /* Server specific event handlers */
        @Override
        protected void connectionRequestEvent(int portHandle, byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6) {
            assert(this.portHandle == portHandle) : "Received an event for the incorrect port";

            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((ServerEventCallback)callbackHandler).connectionRequestEvent(new BluetoothAddress(addr1, addr2, addr3, addr4, addr5, addr6));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void connectedEvent(int portHandle, byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, int connectionType) {
            assert(this.portHandle == portHandle) : "Received an event for the incorrect port";

            ConnectionType type;
            EventCallback  callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                switch(connectionType) {
                case CONNECTION_TYPE_MFI:
                    type = ConnectionType.MFI;
                    break;
                case CONNECTION_TYPE_SPP:
                default:
                    type = ConnectionType.SPP;
                    break;
                }

                try {
                    ((ServerEventCallback)callbackHandler).connectedEvent(new BluetoothAddress(addr1, addr2, addr3, addr4, addr5, addr6), type);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Receiver for event notifications which are common specific to
         * the Serial Port Server.
         * <p>
         * It is guaranteed that no two event methods will be invoked
         * simultaneously. However, because of this guarantee, the
         * implementation of each event handler must be as efficient
         * as possible, as subsequent events will not be sent until the
         * in-progress event handler returns.  Furthermore, event
         * handlers must not block and wait on conditions that can only
         * be satisfied by receiving other events.
         * <p>
         * Implementors should also note that events are sent from a
         * thread context owned by the BluetopiaPM API, so standard
         * locking practices should be observed for data sharing.
         */
        public interface ServerEventCallback extends EventCallback {

            /**
             * Invoked when a remote device requests to open a local
             * port that is flagged with
             * {@link IncomingConnectionFlags#REQUIRE_AUTHORIZATION}.
             * <p>
             * Accept or reject this request with a call to
             * {@link SerialPortServerManager#connectionRequestResponse}.
             *
             * @param remoteDeviceAddress
             *            The Bluetooth Address of the device requested
             *            a connection.
             *
             */
            public void connectionRequestEvent(BluetoothAddress remoteDeviceAddress);

            /**
             * Invoked when the local server port has been opened.
             *
             * @param remoteDeviceAddress
             *        The Bluetooth Address of the the device that is
             *        connected.
             *
             * @param type
             *        The type of connection.
             */
            public void connectedEvent(BluetoothAddress remoteDeviceAddress, ConnectionType type);
        }

        private final static int INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION  = 0X00000001;
        private final static int INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION = 0X00000002;
        private final static int INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION     = 0X00000004;
        private final static int INCOMING_CONNECTION_FLAGS_MFI_ALLOWED            = 0x00000008;
        private final static int INCOMING_CONNECTION_FLAGS_MFI_REQUIRED           = 0x00000010;


        /**
         * Optional flags to control behavior when a remote connection is
         * established by a remote device.
         *
         * @see SerialPortServerManager
         *
         */
        public enum IncomingConnectionFlags {
            /**
             * Require incoming connections be approved by the Manager.
             * This will generate a callback for every incoming
             * connection which must be approved by the controlling
             * Manager before the profile connection is allowed to be
             * completed.
             */
            REQUIRE_AUTHORIZATION,

            /**
             * Require the Bluetooth link to be authenticated. This
             * requires that the devices are paired and will
             * automatically initiate the pairing process if required.
             */
            REQUIRE_AUTHENTICATION,

            /**
             * Require the Bluetooth link to be encrypted.
             */
            REQUIRE_ENCRYPTION,

            /**
             * Allow the usage of MFi.
             */
            MFI_ALLOWED,

            /**
             * Require the usage of MFi.
             */
            MFI_REQUIRED,
        }
    }

    private final static int CONNECTION_TYPE_SPP = 1;
    private final static int CONNECTION_TYPE_MFI = 2;

    /**
     * Types of connections supported by the Serial Port Profile
     * Manager.
     */
    public enum ConnectionType {
        /**
         * Denotes a Serial Port Profile connection.
         */
        SPP,
        /**
         * Denotes an MFi connection.
         */
        MFI,
    }

    /* The following constants are used with the PortStatusMask member of*/
    /* the SPPM_Port_Status_t to specify the Port Status Mask.           */
    private final static int PORT_STATUS_MASK_NO_ERROR_VALUE      = 0;
    private final static int PORT_STATUS_MASK_RTS_CTS_MASK        = 1;
    private final static int PORT_STATUS_MASK_DTR_DSR_MASK        = 2;
    private final static int PORT_STATUS_MASK_RING_INDICATOR_MASK = 4;
    private final static int PORT_STATUS_MASK_CARRIER_DETECT_MASK = 8;

    /**
     * Used with the {@code portStatus} parameter when calling
     * {@link SPPM#sendPortStatus sendPortStatus} or the
     * {@code portStatusMask} parameter when receiving the
     * {@link EventCallback#portStatusChangedEvent portStatusChangedEvent()}.
     */
    public enum PortStatus {
        /**
         * The [request to send / clear to send] pin is active.
         */
        RTS_CTS,

        /**
         * The [data terminal ready / data set ready] pin is active.
         */
        DTR_DSR,

        /**
         * The ring indicator pin is active
         */
        RING_INDICATOR,

        /**
         * The carrier detect pin is active.
         */
        CARRIER_DETECT,
    }

    /* The following constants are used with the LineStatusMask members  */
    /* of the SPPM_Send_Line_Status_Request_t and                        */
    /* SPPM_Line_Status_Changed_Message_t messages to specify the Line   */
    /* Status Mask.                                                      */
    private final static int LINE_STATUS_MASK_NO_ERROR_VALUE     = 0;
    private final static int LINE_STATUS_MASK_OVERRUN_ERROR_MASK = 1;
    private final static int LINE_STATUS_MASK_PARITY_ERROR_MASK  = 2;
    private final static int LINE_STATUS_MASK_FRAMING_ERROR_MASK = 4;

    /**
     * Used with the {@code lineStatus} parameter when calling
     * {@link SPPM#sendLineStatus sendLineStatus()} or the
     * {@code lineStatusMask} parameter when receiving the
     * {@link EventCallback#lineStatusChangedEvent lineStatusChangedEvent()}.
     */
    public enum LineStatus {
        /**
         * Overrun error.
         */
        OVERRUN_ERROR,

        /**
         * Parity error.
         */
        PARITY_ERROR,

        /**
         * Framing error.
         */
        FRAMING_ERROR,
    }

    /**
     * Represents the information advertised in a remote device's Service
     * Discovery Profile service record which is relevant to the Serial Port
     * Profile.
     */
    public static class ServiceRecordInformation {
        /**
         * Handle of the service record. This is an index which identifies a
         * particular record of a remote device. This is not unique between
         * devices.
         */
        public final int    serviceRecordHandle;

        /**
         * UUID which identifies the type of the service. Generally, this UUID
         * indicates the Bluetooth Profile to which the advertised service
         * conforms.
         */
        public final UUID   serviceClassID;

        /**
         * The RFCOMM/SPP port number to which devices can connect to access
         * this service.
         */
        public final int    rfcommPortNumber;

        /**
         * The descriptive name of the service, if available.
         */
        public final String serviceName;

        /*package*/ ServiceRecordInformation(int handle, UUID type, int port, String name) {
            serviceRecordHandle = handle;
            serviceClassID      = type;
            rfcommPortNumber    = port;
            serviceName         = name;
        }
    }

    private final static int IDPS_STATE_START_IDENTIFICATION_REQUEST    = 1;
    private final static int IDPS_STATE_START_IDENTIFICATION_PROCESS    = 2;
    private final static int IDPS_STATE_IDENTIFICATION_PROCESS          = 3;
    private final static int IDPS_STATE_IDENTIFICATION_PROCESS_COMPLETE = 4;
    private final static int IDPS_STATE_START_AUTHENTICATION_PROCESS    = 5;
    private final static int IDPS_STATE_AUTHENTICATION_PROCESS          = 6;
    private final static int IDPS_STATE_AUTHENTICATION_PROCESS_COMPLETE = 7;

    /**
     * Represents the defined Identify Device Preferences and Settings (IDPS)
     * states for reporting status for MFi.
     */
    public enum IDPSState {
        /**
         * Start identification request.
         */
        START_IDENTIFICATION_REQUEST,

        /**
         * Start identification process.
         */
        START_IDENTIFICATION_PROCESS,

        /**
         * Identification process.
         */
        IDENTIFICATION_PROCESS,

        /**
         * Identification process complete.
         */
        IDENTIFICATION_PROCESS_COMPLETE,

        /**
         * Start authentication process.
         */
        START_AUTHENTICATION_PROCESS,

        /**
         * Authentication process.
         */
        AUTHENTICATION_PROCESS,

        /**
         * Authentication process complete.
         */
        AUTHENTICATION_PROCESS_COMPLETE,
    }

    private final static int IDPS_STATUS_SUCCESS                         = 1;
    private final static int IDPS_STATUS_ERROR_RETRYING           = 2;
    private final static int IDPS_STATUS_TIMEOUT_HALTING          = 3;
    private final static int IDPS_STATUS_GENERAL_FAILURE          = 4;
    private final static int IDPS_STATUS_PROCESS_FAILURE          = 5;
    private final static int IDPS_STATUS_PROCESS_TIMEOUT_RETRYING = 6;

    /**
     * Represents the defined Identify Device Preferences and Settings (IDPS)
     * process statuses for MFi.
     */
    public enum IDPSStatus {
        /**
         * IDPS succeeded. All required token-value fields were received by
         * the iDevice and authentication can proceed.
         */
        SUCCESS,

        /**
         * The stack was unable to start IDPS and is retrying.
         */
        ERROR_RETRYING,

        /**
         * The maximum number of packet transmission retry attempts has been
         * reached.
         */
        TIMEOUT_HALTING,

        /**
         * A general communication error occurred during or while start IDPS.
         */
        GENERAL_FAILURE,

        /**
         * The iDevice reported an error during IDPS or authentication.
         */
        PROCESS_FAILURE,

        /**
         * A packet was not acknowledged. The stack is retransmitting.
         */
        PROCESS_TIMEOUT_RETRYING,
    }

    /* The following constants represent the defined status values for   */
    /* the Data Confirmation events (both the Session Data and Non       */
    /* Session Data events - SPPM_Session_Data_Confirmation_Message_t and*/
    /* SPPM_Non_Session_Data_Confirmation_Message_t events).             */
    private final static int SPPM_DATA_CONFIRMATION_STATUS_PACKET_SENT         = 1;
    private final static int SPPM_DATA_CONFIRMATION_STATUS_PACKET_ACKNOWLEDGED = 2;
    private final static int SPPM_DATA_CONFIRMATION_STATUS_PACKET_FAILED       = 3;
    private final static int SPPM_DATA_CONFIRMATION_STATUS_PACKET_CANCELED     = 4;

    /**
     * Represents the possible status for transmitted MFi packets.
     *
     * @see EventCallback#sessionDataConfirmationEvent
     * @see EventCallback#nonSessionDataConfirmationEvent
     */
    public enum DataConfirmationStatus {
        /**
         * The packet was successfully sent and does not require
         * acknowledgment.
         */
        SENT,

        /**
         * An acknowledgment response for the packet was received.
         */
        ACKNOWLEDGED,

        /**
         * The packet could not be sent or acknowledgment was not received
         * within the allowed timeout.
         */
        FAILED,

        /**
         * The packet was cancelled by user intervention before it was
         * transmitted.
         */
        CANCELED,
    }

    /**
     * Represents an individual MFi Full ID String (FID) token.  A token
     * consists of the FID type and subType, followed by the FID data.
     *
     * This structure is used when specifying the MFi configuration
     * settings by calling
     * {@link SPPM#configureMFiSettings configureMFiSettings()}.
     */
    public static class MFiFIDTokenValue {
        /*package*/ int    type;
        /*package*/ int    subtype;
        /*package*/ byte[] data;

        /**
         * Constructs an MFi Full ID String (FID) token from the given
         * FID type, subType, and data.
         *
         * @param FIDType
         *        The FID type.
         *
         * @param FIDSubType
         *        The FID subType.
         *
         * @param FIDData
         *        The FID data.
         */
        public MFiFIDTokenValue(int FIDType, int FIDSubType, byte[] FIDData) {
            if((FIDType > 0x00FF) || (FIDType < 0))
                throw new IllegalArgumentException("FIDType must be between [0,255] inclusive");
            if((FIDSubType > 0x00FF) || (FIDType < 0))
                throw new IllegalArgumentException("FIDSubType must be between [0,255] inclusive");

            type    = FIDType;
            subtype = FIDSubType;
            data    = FIDData.clone();
        }
    }

    /* The following structure is a container structure which is used    */
    /* with the SPPM_MFi_Configuration_Settings_t structure to denote the*/
    /* various Accessory Inforamation (required fields for MFi           */
    /* configuration).                                                   */
    /* * NOTE * The AccessoryInformationBitMask member is a bit mask     */
    /*          that specifies which of the following optional fields    */
    /*          are valid.                                               */
    /* * NOTE * The Manufacturer, Model Number, and Serial Number members*/
    /*          are NULL terminated UTF-8 character strings that can be  */
    /*          up to the length specified (including NULL terminator).  */
    /*          The length specified specifies the largest length,       */
    /*          however the strings can be shorter.                      */
    /**
     * Represents the identifying information of an MFi Accessory.
     */
    public static class MFiAccessoryInfo
    {
        /*package*/ long   accessoryCapabilitiesBitmask;
        /*package*/ String accessoryName;
        /*package*/ int    accessoryFirmwareVersion;
        /*package*/ int    accessoryHardwareVersion;
        /*package*/ String accessoryManufacturer;
        /*package*/ String accessoryModelNumber;
        /*package*/ String accessorySerialNumber;
        /*package*/ int    accessoryRFCertification;

        /**
         * Constructs an MFiAccessoryInfo object representing this accessory.
         *
         * @param capabilitiesBitmask
         *            A mask of the Capability bits. This is a union of the
         *            {@code MFI_ACCESSORY_CAPABILITY_*} constants.
         *
         * @param name
         *            The user-visible name of the accessory.
         *
         * @param firmwareVersion
         *            The firmware version of the accessory. Only the lower
         *            three bytes are used. This value should be supplied in the
         *            format 0x00XXYYZZ where XX is the major version, YY is the
         *            minor version, and ZZ is the revision version number.
         *
         * @param hardwareVersion
         *            The hardware version of the accessory. Only the lower
         *            three bytes are used. This value should be supplied in the
         *            format 0x00XXYYZZ where XX is the major version, YY is the
         *            minor version, and ZZ is the revision version number.
         *
         * @param manufacturer
         *            (Optional) The name of the accessory manufacturer.
         *
         * @param modelNumber
         *            (Optional) The string representation of the of the
         *            accessory model number.
         *
         * @param serialNumber
         *            (Optional) The string representation of the of the
         *            accessory serial number.
         *
         * @param rfCertification
         *            A bit mask of the accessory's certified RF Classes. This
         *            is the union of the appropriate
         *            {@code MFI_ACCESSORY_RF_CERTIFICATION_CLASS_*} constants.
         */
        public MFiAccessoryInfo(long capabilitiesBitmask, String name, int firmwareVersion, int hardwareVersion, String manufacturer, String modelNumber, String serialNumber, int rfCertification) {
            accessoryCapabilitiesBitmask = capabilitiesBitmask;
            accessoryName                = name;
            accessoryFirmwareVersion     = (firmwareVersion & 0x00FFFFFF);
            accessoryHardwareVersion     = (hardwareVersion & 0x00FFFFFF);
            accessoryManufacturer        = manufacturer;
            accessoryModelNumber         = modelNumber;
            accessorySerialNumber        = serialNumber;
            accessoryRFCertification     = rfCertification;
        }
    }

    /* Accessory capabilities.                                           */

    /**
     * Indicates that this accessory supports analog audio output.
     *
     * @see MFiAccessoryInfo
     */
    public final static long MFI_ACCESSORY_CAPABILITY_ANALOG_LINE_OUT               = (1L <<  0);
    /**
     * Indicates that this accessory supports analog audio input.
     *
     * @see MFiAccessoryInfo
     */
    public final static long MFI_ACCESSORY_CAPABILITY_ANALOG_LINE_IN                = (1L <<  1);
    /**
     * Indicates that this accessory supports analog video output.
     *
     * @see MFiAccessoryInfo
     */
    public final static long MFI_ACCESSORY_CAPABILITY_ANALOG_VIDEO_OUT              = (1L <<  2);
    /**
     * Indicates that this accessory supports USB audio.
     *
     * @see MFiAccessoryInfo
     */
    public final static long MFI_ACCESSORY_CAPABILITY_USB_AUDIO                     = (1L <<  4);
    /**
     * Indicates that this accessory supports communication with an application.
     *
     * @see MFiAccessoryInfo
     */
    public final static long MFI_ACCESSORY_CAPABILITY_SUPPORTS_COMM_WITH_APP        = (1L <<  9);
    /**
     * Indicates that this accessory supports checking the iPod's volume.
     *
     * @see MFiAccessoryInfo
     */
    public final static long MFI_ACCESSORY_CAPABILITY_CHECKS_IPOD_VOLUME            = (1L << 11);
    /**
     * Indicates that this accessory supports iPod accessibility features.
     *
     * @see MFiAccessoryInfo
     */
    public final static long MFI_ACCESSORY_CAPABILITY_USES_IPOD_ACCESSABILITY       = (1L << 17);
    /**
     * Indicates that this accessory can handle multiple outstanding packets
     * simultaneously.
     *
     * @see MFiAccessoryInfo
     */
    public final static long MFI_ACCESSORY_CAPABILITY_HANDLES_MULTI_PACKET_RESPONSE = (1L << 19);

    /* Accessory capabilities.                                           */

    /**
     * Indicates that this accessory has been certified for RF Certification
     * Class 1.
     *
     * @see MFiAccessoryInfo
     */
    public final static int MFI_ACCESSORY_RF_CERTIFICATION_CLASS_1 = (1 << 0);
    /**
     * Indicates that this accessory has been certified for RF Certification
     * Class 2.
     *
     * @see MFiAccessoryInfo
     */
    public final static int MFI_ACCESSORY_RF_CERTIFICATION_CLASS_2 = (1 << 1);
    /**
     * Indicates that this accessory has been certified for RF Certification
     * Class 3.
     *
     * @see MFiAccessoryInfo
     */
    public final static int MFI_ACCESSORY_RF_CERTIFICATION_CLASS_3 = (1 << 2);

    private final static int MATCH_ACTION_NONE               = 0;
    private final static int MATCH_ACTION_AUTOMATIC_SEARCH   = 1;
    private final static int MATCH_ACTION_SEARCH_BUTTON_ONLY = 2;

    /**
     * Types of actions that can be taken by a remote MFi device when no handler
     * for a particular protocol is installed.
     */
    public static enum MFiProtocolMatchAction {
        /**
         * Take no action.
         */
        NONE,
        /**
         * Search for an available handler automatically.
         */
        AUTOMATIC_SEARCH,
        /**
         * Offer the user a choice of whether to search for an available
         * handler.
         */
        SEARCH_BUTTON_ONLY,
    }

    /**
     * Identifier for a user-defined MFi protocol.
     */
    public static class MFiProtocol {
        /*package*/ String                 name;
        /*package*/ MFiProtocolMatchAction matchAction;

        /**
         * Constructs an MFiProtocol object representing a custom MFi protocol.
         *
         * @param name
         *            The name of the protocol.
         *
         * @param matchAction
         *            The action to be taken by the remote device if no handler
         *            for this protocol is available.
         */
        public MFiProtocol(String name, MFiProtocolMatchAction matchAction) {
            if(name == null)
                throw new InvalidParameterException("name cannot be null");

            this.name        = name;
            this.matchAction = matchAction;
        }
    }

    /* Native method declarations */

    private native static void initClassNative();

    private native void initObjectNative() throws ServerNotReachableException;

    private native void cleanupObjectNative();

    private native int disconnectNative(int portHandle, int closeTimeout);

    private native int readDataNative(int portHandle, int readTimeout, byte[] dataBuffer, int start, int length);

    private native int writeDataNative(int portHandle, int writeTimeout, byte[] dataBuffer, int start, int length);

    private native int sendLineStatusNative(int portHandle, int lineStatusMask);

    private native int sendPortStatusNative(int portHandle, int portStatusMask, boolean breakSignal, int breakTimeout);

    protected native int connectRemoteDeviceNative(byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, int serverPort, int openFlags, int[] connectionStatus);

    protected native int connectionRequestResponseNative(int portHandle, boolean acceptConnection);

    protected native int registerServerPortNative(int portNumber, int connectionFlags);

    protected native int registerSerialPortSDPRecordNative(int portHandle, long serviceClassIDsHigh[], long serviceClassIDsLow[], long protocolsHigh[], long protocolsLow[], String serviceName);

    protected native int unRegisterServerPortNative(int portHandle);

    private native int queryRemoteDeviceServicesNative(byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, int handles[][], long serviceTypes[][], int ports[][], String names[][]);

    private native int configureMFiSettingsNative(int maximumReceivePacketSize, int dataPacketTimeout, byte[] supportedLingoes, long accessoryCapabilitiesBitmask, String accessoryName, int accessoryFirmwareVersion, int accessoryHardwareVersion, String accessoryManufacturer, String accessoryModelNumber, String accessorySerialNumber, int accessoryRFCertification, String[] supportedProtocolList, int[] matchActionList, String bundleSeedID, int[] fidTypes, int[] fidSubtypes, byte[][] fidDataBuffers, String currentLanguage, String[] supportedLanguages, short[] controlMessageIDsSent, short[] controlMessageIDsReceived);

    private native int queryConnectionTypeNative(int portHandle, int[] connectionType);

    private native int openSessionRequestResponseNative(int portHandle, int sessionID, boolean accept);

    private native int sendSessionDataNative(int portHandle, int sessionID, byte[] sessionData);

    private native int sendNonSessionDataNative(int portHandle, int lingoID, int commandID, int transactionID, byte[] data);

    private native int cancelPacketNative(int portHandle, int packetID);

    private native int sendControlMessageNative(int portHandle, short messageID, byte[] data);
}
