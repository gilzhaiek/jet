/*
 * PANM.java Copyright 2010 - 2014 Stonestreet One, LLC. All Rights Reserved.
 */
package com.stonestreetone.bluetopiapm;

import java.util.EnumSet;


/**
 * Java wrapper for Personal Area Network Profile Manager API for Stonestreet
 * One Bluetooth Protocol Stack Platform Manager.
 */
public class PANM {

    private final EventCallback callbackHandler;

    private boolean             disposed;

    private long                localData;

    static {
        System.loadLibrary("btpmj");
        initClassNative();
    }

    /**
     * Creates a new management connection to the local PAN Manager.
     *
     * @param eventCallback
     *            Receiver for events sent by the server.
     *
     * @throws ServerNotReachableException
     *             if the BluetopiaPM server cannot be reached.
     */
    public PANM(EventCallback eventCallback) throws ServerNotReachableException {

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

    /* Personal Area Networking (PAN) Connection Management Functions.   */

    /**
     * Responds to a request from a remote device to connect to a Local Server.
     *
     * A successful return value does not necessarily indicate that the
     * connection has been successfully established. A
     * {@link EventCallback#connectedEvent} call will notify of this status.
     *
     * @param remoteDeviceAddress
     *            Bluetooth address of the remote device attempting to connect.
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
     * Connect to a remote Personal Area Network device.
     * <p>
     * If {@code waitForConnection} is {@code true}, this call will block until
     * the connection status is received (that is, the connection is completed).
     * If this parameter is not specified (NULL), then the connection status
     * will be returned asynchronously by way of a call to
     * {@link EventCallback#connectionStatusEvent} on the {@link EventCallback}
     * provided when this Manager was constructed.
     *
     * @param remoteDeviceAddress
     *            Bluetooth address of the remote device.
     * @param localServiceType
     *            PAN Service Type which the local device will use to establish
     *            the connection.
     * @param remoteServiceType
     *            PAN Service Type to which the connection should be made.
     * @param connectionFlags
     *            Bit flags which control whether encryption and/or
     *            authentication is used.
     * @param waitForConnection
     *            If true, this call will block until the connection attempt
     *            finishes.
     * @return Zero if successful, or a negative return error code if there was
     *         an error issuing the connection attempt. If
     *         {@code waitForConnection} is {@code true} and the connection
     *         attempt was successfully issued, a positive return value
     *         indicates the reason why the connection failed.
     */
    public int connectRemoteDevice(BluetoothAddress remoteDeviceAddress, ServiceType localServiceType, ServiceType remoteServiceType, EnumSet<ConnectionFlags> connectionFlags, boolean waitForConnection) {
        int localType;
        int remoteType;
        int flags;
        byte[] address = remoteDeviceAddress.internalByteArray();

        switch(localServiceType) {
        case PERSONAL_AREA_NETWORK_USER:
            localType = SERVICE_TYPE_PERSONAL_AREA_NETWORK_USER;
            break;
        case NETWORK_ACCESS_POINT:
            localType = SERVICE_TYPE_NETWORK_ACCESS_POINT;
            break;
        case GROUP_ADHOC_NETWORK:
        default:
            localType = SERVICE_TYPE_GROUP_ADHOC_NETWORK;
            break;
        }

        switch(remoteServiceType) {
        case PERSONAL_AREA_NETWORK_USER:
            remoteType = SERVICE_TYPE_PERSONAL_AREA_NETWORK_USER;
            break;
        case NETWORK_ACCESS_POINT:
            remoteType = SERVICE_TYPE_NETWORK_ACCESS_POINT;
            break;
        case GROUP_ADHOC_NETWORK:
        default:
            remoteType = SERVICE_TYPE_GROUP_ADHOC_NETWORK;
            break;
        }

        flags  = 0;
        flags |= (connectionFlags.contains(ConnectionFlags.REQUIRE_AUTHENTICATION) ? CONNECTION_FLAGS_REQUIRE_AUTHENTICATION : 0);
        flags |= (connectionFlags.contains(ConnectionFlags.REQUIRE_ENCRYPTION)     ? CONNECTION_FLAGS_REQUIRE_ENCRYPTION     : 0);

        return connectRemoteDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5], localType, remoteType, flags, waitForConnection);
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
     * @param remoteDeviceAddress
     *            Bluetooth address of the device to disconnect.
     * @return Zero if successful, or a negative value if there was an error.
     */
    public int disconnectRemoteDevice(BluetoothAddress remoteDeviceAddress) {
        byte[] address = remoteDeviceAddress.internalByteArray();

        return disconnectRemoteDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5]);
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
        int                  result;
        BluetoothAddress[][] addressList;

        addressList    = new BluetoothAddress[1][0];

        result = queryConnectedDevicesNative(addressList);

        if(result < 0)
            addressList[0] = null;

        return addressList[0];
    }

    /**
     * Retrieves the current configuration of the local service.
     *
     * @return The configuration parameters of the local service or
     *         {@code null} if there was an error.
     **/
    public CurrentConfiguration queryCurrentConfiguration() {
        int                              result;
        int[]                            serviceTypeFlags = new int[1];
        int[]                            incomingConnectionFlags = new int[1];
        EnumSet<ServiceType>             serviceTypesEnum;
        EnumSet<IncomingConnectionFlags> connectionFlagsEnum;
        CurrentConfiguration             config;

        result = queryCurrentConfigurationNative(serviceTypeFlags, incomingConnectionFlags);

        serviceTypesEnum = EnumSet.noneOf(ServiceType.class);
        if((serviceTypeFlags[0] & SERVICE_TYPE_PERSONAL_AREA_NETWORK_USER) == SERVICE_TYPE_PERSONAL_AREA_NETWORK_USER)
            serviceTypesEnum.add(ServiceType.PERSONAL_AREA_NETWORK_USER);
        if((serviceTypeFlags[0] & SERVICE_TYPE_NETWORK_ACCESS_POINT) == SERVICE_TYPE_NETWORK_ACCESS_POINT)
            serviceTypesEnum.add(ServiceType.NETWORK_ACCESS_POINT);
        if((serviceTypeFlags[0] & SERVICE_TYPE_GROUP_ADHOC_NETWORK) == SERVICE_TYPE_GROUP_ADHOC_NETWORK)
            serviceTypesEnum.add(ServiceType.GROUP_ADHOC_NETWORK);

        connectionFlagsEnum = EnumSet.noneOf(IncomingConnectionFlags.class);
        if((incomingConnectionFlags[0] & INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION) == INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION)
            connectionFlagsEnum.add(IncomingConnectionFlags.REQUIRE_AUTHORIZATION);
        if((incomingConnectionFlags[0] & INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION) == INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION)
            connectionFlagsEnum.add(IncomingConnectionFlags.REQUIRE_AUTHENTICATION);
        if((incomingConnectionFlags[0] & INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION) == INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION)
            connectionFlagsEnum.add(IncomingConnectionFlags.REQUIRE_ENCRYPTION);

        if(result == 0)
            config = new CurrentConfiguration(serviceTypesEnum, connectionFlagsEnum);
        else
            config = null;

        return config;
    }

    /**
     * Changes the connection flags for incoming connections from remote
     * devices.
     *
     * @param incomingConnectionFlags
     *            New connection flags to be used for all incoming connection
     *            requests.
     * @return Zero if successful, or a negative return error code if there was
     *         an error.
     */
    public int changeIncomingConnectionFlags(EnumSet<IncomingConnectionFlags> incomingConnectionFlags) {
        int flags;

        flags  = 0;
        flags |= (incomingConnectionFlags.contains(IncomingConnectionFlags.REQUIRE_AUTHORIZATION)  ? INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION  : 0);
        flags |= (incomingConnectionFlags.contains(IncomingConnectionFlags.REQUIRE_AUTHENTICATION) ? INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION : 0);
        flags |= (incomingConnectionFlags.contains(IncomingConnectionFlags.REQUIRE_ENCRYPTION)     ? INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION     : 0);

        return changeIncomingConnectionFlagsNative(flags);
    }

    /* The following structure is a container structure that holds the   */
    /* information that is returned in a petPANMIncomingConnectionRequest*/
    /* event.                                                            */
    private void incomingConnectionRequestEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.incomingConnectionRequestEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* The following structure is a container structure that holds the   */
    /* information that is returned in a petPANMConnected event.         */
    private void connectedEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int serviceType) {
        ServiceType   type;
        EventCallback callbackHandler = this.callbackHandler;

        switch(serviceType) {
        case SERVICE_TYPE_PERSONAL_AREA_NETWORK_USER:
            type = ServiceType.PERSONAL_AREA_NETWORK_USER;
            break;
        case SERVICE_TYPE_NETWORK_ACCESS_POINT:
            type = ServiceType.NETWORK_ACCESS_POINT;
            break;
        case SERVICE_TYPE_GROUP_ADHOC_NETWORK:
        default:
            type = ServiceType.GROUP_ADHOC_NETWORK;
            break;
        }

        if(callbackHandler != null) {
            try {
                callbackHandler.connectedEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), type);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* The following event is dispatched when a remote device disconnects*/
    /* from the local device).  The RemoteDeviceAddress member specifies */
    /* the Bluetooth device address of the device that disconnected from */
    /* the profile.                                                      */
    private void disconnectedEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int serviceType) {
        ServiceType   type;
        EventCallback callbackHandler = this.callbackHandler;

        switch(serviceType) {
        case SERVICE_TYPE_PERSONAL_AREA_NETWORK_USER:
            type = ServiceType.PERSONAL_AREA_NETWORK_USER;
            break;
        case SERVICE_TYPE_NETWORK_ACCESS_POINT:
            type = ServiceType.NETWORK_ACCESS_POINT;
            break;
        case SERVICE_TYPE_GROUP_ADHOC_NETWORK:
        default:
            type = ServiceType.GROUP_ADHOC_NETWORK;
            break;
        }

        if(callbackHandler != null) {
            try {
                callbackHandler.disconnectedEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), type);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* The following structure is a container structure that holds the   */
    /* information that is returned in a petPANMConnectionStatus event.  */
    private void connectionStatusEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int serviceType, int connectionStatus) {
        ServiceType      type;
        ConnectionStatus status;
        EventCallback    callbackHandler = this.callbackHandler;

        switch(serviceType) {
        case SERVICE_TYPE_PERSONAL_AREA_NETWORK_USER:
            type = ServiceType.PERSONAL_AREA_NETWORK_USER;
            break;
        case SERVICE_TYPE_NETWORK_ACCESS_POINT:
            type = ServiceType.NETWORK_ACCESS_POINT;
            break;
        case SERVICE_TYPE_GROUP_ADHOC_NETWORK:
        default:
            type = ServiceType.GROUP_ADHOC_NETWORK;
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

        if(callbackHandler != null) {
            try {
                callbackHandler.connectionStatusEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), type, status);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Public interface for handling PANM events.
     */
    public static interface EventCallback {

        /**
         * Invoked when an incoming connection request is received.
         *
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
         */
        public void incomingConnectionRequestEvent(BluetoothAddress remoteDeviceAddress);

        /**
         * Invoked when a connection is established.
         *
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
         *
         * @param serviceType The {@link ServiceType} specifying the type of connection.
         */
        public void connectedEvent(BluetoothAddress remoteDeviceAddress, ServiceType serviceType);

        /**
         * Invoked when a remote device disconnects from the local device.
         *
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
         *
         * @param serviceType The {@link ServiceType} that was disconnected.
         */
        public void disconnectedEvent(BluetoothAddress remoteDeviceAddress, ServiceType serviceType);

        /**
         * Invoked when a response to a request from a remote device has
         * been received.
         *
         * @param remoteDeviceAddress The {@link BluetoothAddress} of the remote device.
         *
         * @param serviceType The {@link ServiceType} involved in the request.
         *
         * @param status The {@link ConnectionStatus} of the request.
         */
        public void connectionStatusEvent(BluetoothAddress remoteDeviceAddress, ServiceType serviceType, ConnectionStatus status);
    }

    private final static int CONNECTION_STATUS_SUCCESS                  = 0;
    private final static int CONNECTION_STATUS_FAILURE_TIMEOUT          = 1;
    private final static int CONNECTION_STATUS_FAILURE_REFUSED          = 2;
    private final static int CONNECTION_STATUS_FAILURE_SECURITY         = 3;
    private final static int CONNECTION_STATUS_FAILURE_DEVICE_POWER_OFF = 4;
    private final static int CONNECTION_STATUS_FAILURE_UNKNOWN          = 5;

    /*
     * NOTE:
     *          Static constant duplicates of enum types are used for translation to JNI.
     */

    /**
     * Possible results of a connection attempt.
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

    private final static int CONNECTION_FLAGS_REQUIRE_AUTHENTICATION = 1;
    private final static int CONNECTION_FLAGS_REQUIRE_ENCRYPTION     = 2;

    /**
     * Optional flags to control behavior when a remote connection is
     * established by the local service.
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

    private static final int AUTOMATIC_ACCEPT = 0;
    private static final int AUTOMATIC_REJECT = 1;
    private static final int MANUAL_ACCEPT    = 2;

    /**
     * The supported server connection modes.
     */
    public enum ServerConnectionMode {
        /**
         * The server automatically accepts connection requests.
         */
        AUTOMATIC_ACCEPT,

        /**
         * The server automatically rejects connection requests.
         */
        AUTOMATIC_REJECT,

        /**
         * The server manually accepts (or rejects) connection requests.
         */
        MANUAL_ACCEPT
    }

    private static final int SERVICE_TYPE_PERSONAL_AREA_NETWORK_USER = 0x01;
    private static final int SERVICE_TYPE_NETWORK_ACCESS_POINT       = 0x02;
    private static final int SERVICE_TYPE_GROUP_ADHOC_NETWORK        = 0x04;

    /**
     * The different service types that are supported by the PAN profile.
     */
    public enum ServiceType {
        /**
         * Specifies the PAN User (client) role of either a Network Access Point
         * service or a Group Ad-hoc Network services.
         */
        PERSONAL_AREA_NETWORK_USER,

        /**
         * Specifies the Network Access Point service, which provides some of
         * the features of an Ethernet bridge to support routing between
         * connected PAN User devices and external networks.
         */
        NETWORK_ACCESS_POINT,

        /**
         * Specifies the Group Ad-hoc Network service, which is able to
         * forward Ethernet packets between connected PAN User devices.
         */
        GROUP_ADHOC_NETWORK
    }

    /**
     * Specifies the current configuration of a PAN server.
     */
    public class CurrentConfiguration {
        /**
         * An EnumSet representing the currently supported
         * {@link ServiceType ServiceTypes}.
         */
        public final EnumSet<ServiceType> serviceTypes;

        /**
         * An EnumSet representing the currently configured
         * {@link IncomingConnectionFlags}.
         */
        public final EnumSet<IncomingConnectionFlags> incomingConnectionFlags;

        /*package*/ CurrentConfiguration(EnumSet<ServiceType> types, EnumSet<IncomingConnectionFlags> connectionFlags) {
            serviceTypes            = types;
            incomingConnectionFlags = connectionFlags;
        }
    }

    /* Native method declarations */

    private native static void initClassNative();

    private native void initObjectNative() throws ServerNotReachableException;

    private native void cleanupObjectNative();

    private native int connectionRequestResponseNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean acceptConnection);

    private native int connectRemoteDeviceNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int localServiceType, int remoteServiceType, int connectionFlags, boolean waitForConnection);

    private native int disconnectRemoteDeviceNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    private native int queryConnectedDevicesNative(BluetoothAddress[][] remoteDeviceAddressList);

    private native int queryCurrentConfigurationNative(int[] serviceTypeFlags, int[] incomingConnectionFlags);

    private native int changeIncomingConnectionFlagsNative(int incomingConnectionFlags);
}
