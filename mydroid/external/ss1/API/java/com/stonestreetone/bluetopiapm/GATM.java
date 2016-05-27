/*
 * GATM.java Copyright 2010 - 2014 Stonestreet One, LLC. All Rights Reserved.
 */
package com.stonestreetone.bluetopiapm;

import java.lang.ref.WeakReference;
import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import com.stonestreetone.bluetopiapm.GATM.GenericAttributeServerManager.ServerEventCallback;

/**
 * Java wrapper for Generic Attribute Profile Manager API
 */
public abstract class GATM {

    protected final EventCallback callbackHandler;

    private boolean               disposed;

    private long                  localData;

    static {
        System.loadLibrary("btpmj");
        initClassNative();
    }

    private final static int      MANAGER_TYPE_CLIENT = 1;
    private final static int      MANAGER_TYPE_SERVER = 2;

    /**
     * Private Constructor
     *
     * @throws ServerNotReachableException
     */
    protected GATM(int type, EventCallback eventCallback) throws ServerNotReachableException {

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
     * Dispose of resources allocated by this manager object.
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
                System.err.println("Error: Possible memory leak: Manager object of type " + this.getClass().getName() + " disposed = false");
                dispose();
            }
        } finally {
            super.finalize();
        }
    }

    /**
     * Determine the number of currently connected devices.
     *
     * @return Return the number of currently connected devices, or a negative
     *         error code.
     *
     */
    public int queryNumberOfConnectedDevices() {

        return queryConnectedDevicesNative(null, null);
    }

    /**
     * Get the Bluetooth address and connection type of currently connected devices.
     *
     * @return Return an array of ConnectionInformation objects
     *
     */
    public ConnectionInformation[] queryConnectedDevices() {

        int connectedDevicesReturned;
        ConnectionInformation[] connectionInformation;
        int[][] connectionTypeConstants = new int[1][];
        byte[][] remoteDeviceAddresses = new byte[1][];
        ConnectionType connectionType;

        connectedDevicesReturned = queryConnectedDevicesNative(connectionTypeConstants, remoteDeviceAddresses);

        if(connectedDevicesReturned >= 0) {
            connectionInformation = new ConnectionInformation[connectedDevicesReturned];

            for(int index = 0; index < connectedDevicesReturned; index++ ) {

                switch(connectionTypeConstants[0][index]) {
                case CONNECTION_TYPE_LOW_ENERGY:
                    connectionType = ConnectionType.LOW_ENERGY;
                    break;
                case CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE:
                    connectionType = ConnectionType.BASIC_RATE_ENHANCED_DATA_RATE;
                    break;
                default:
                    connectionType = null;
                    break;
                }

                connectionInformation[index] = new ConnectionInformation(connectionType, new BluetoothAddress(remoteDeviceAddresses[0][(index * 6) + 0], remoteDeviceAddresses[0][(index * 6) + 1], remoteDeviceAddresses[0][(index * 6) + 2], remoteDeviceAddresses[0][(index * 6) + 3], remoteDeviceAddresses[0][(index * 6) + 4], remoteDeviceAddresses[0][(index * 6) + 5]));
            }

        } else {

            connectionInformation = null;
        }

        return connectionInformation;
    }

    protected void connected(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int MTU) {

        ConnectionType connectionType;
        EventCallback  callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {

            switch(connectionTypeConstant) {
            case CONNECTION_TYPE_LOW_ENERGY:
                connectionType = ConnectionType.LOW_ENERGY;
                break;
            case CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE:
                connectionType = ConnectionType.BASIC_RATE_ENHANCED_DATA_RATE;
                break;
            default:
                connectionType = null;
                break;
            }

            try {
                callbackHandler.connectedEvent(connectionType, new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), MTU);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void disconnected(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {

        ConnectionType   connectionType;
        EventCallback    callbackHandler = this.callbackHandler;
        BluetoothAddress remoteDeviceAddress;

        if(callbackHandler != null) {

            switch(connectionTypeConstant) {
            case CONNECTION_TYPE_LOW_ENERGY:
                connectionType = ConnectionType.LOW_ENERGY;
                break;
            case CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE:
                connectionType = ConnectionType.BASIC_RATE_ENHANCED_DATA_RATE;
                break;
            default:
                connectionType = null;
                break;
            }

            remoteDeviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);

            try {
                callbackHandler.disconnectedEvent(connectionType, remoteDeviceAddress);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void connectionMTUUpdate(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int MTU) {

        ConnectionType connectionType;
        EventCallback  callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {

            switch(connectionTypeConstant) {
            case CONNECTION_TYPE_LOW_ENERGY:
                connectionType = ConnectionType.LOW_ENERGY;
                break;
            case CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE:
                connectionType = ConnectionType.BASIC_RATE_ENHANCED_DATA_RATE;
                break;
            default:
                connectionType = null;
                break;
            }

            try {
                callbackHandler.connectionMTUUpdateEvent(connectionType, new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), MTU);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void handleValue(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean handleValueIndication, int attributeHandle, byte[] attributeValue) {

        ConnectionType connectionType;
        EventCallback  callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {

            switch(connectionTypeConstant) {
            case CONNECTION_TYPE_LOW_ENERGY:
                connectionType = ConnectionType.LOW_ENERGY;
                break;
            case CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE:
                connectionType = ConnectionType.BASIC_RATE_ENHANCED_DATA_RATE;
                break;
            default:
                connectionType = null;
                break;
            }

            try {
                callbackHandler.handleValueEvent(connectionType, new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), handleValueIndication, attributeHandle, attributeValue);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void readResponse(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, int attributeHandle, boolean isFinal, byte[] attributeValue) {
        throw new UnsupportedOperationException();
    }

    protected void writeResponse(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, int attributeHandle) {
        throw new UnsupportedOperationException();
    }

    protected void errorResponse(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, int attributeHandle, int requestErrorTypeConstant, short attributeProtocolErrorCode) {
        throw new UnsupportedOperationException();
    }

    protected void writeRequest(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int serviceID, int requestID, int attributeOffset, byte[] attributeValue) {
        throw new UnsupportedOperationException();
    }

    protected void signedWrite(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int serviceID, boolean validSignature, int attributeOffset, byte[] attributeValue) {
        throw new UnsupportedOperationException();
    }

    protected void readRequest(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int serviceID, int requestID, int attributeOffset, int attributeValueOffset) {
        throw new UnsupportedOperationException();
    }

    protected void prepareWriteRequest(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int serviceID, int requestID, int attributeOffset, int attributeValueOffset, byte[] attributeValue) {
        throw new UnsupportedOperationException();
    }

    protected void commitPrepareWrite(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int serviceID, boolean commitWrites) {
        throw new UnsupportedOperationException();
    }

    protected void handleValueConfirmation(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int serviceID, int transactionID, int attributeOffset, int confirmationStatus) {
        throw new UnsupportedOperationException();
    }

    /**
     * Interface of functions called for both attribute servers and clients
     * <p>
     * It is guaranteed that no two event methods will be invoked
     * simultaneously. The implementation of each event handler must be as
     * efficient as possible, as subsequent events will not be sent until the
     * event handler returns. Event handlers must not block and wait on
     * conditions that can only be satisfied by receiving other events.
     * <p>
     * Users should note that events are sent from a thread context owned by the
     * BluetopiaPM API, so standard locking practices should be observed.
     */
    public abstract interface EventCallback {

        /**
         * React to the establishment of a connection.
         *
         * @param connectionType
         *            The type of connection - client or server
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device
         * @param MTU
         *            The MTU for the connection
         */
        public void connectedEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int MTU);

        /**
         * React to the termination of a connection.
         *
         * @param connectionType
         *            The type of connection - client or server
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device
         */
        public void disconnectedEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress);

        /**
         * React to a change in the MTU of a connection.
         *
         * @param connectionType
         *            The type of connection - client or server
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device
         * @param MTU
         *            The updated MTU
         */
        public void connectionMTUUpdateEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int MTU);

        /**
         * React to the delivery of a handle value.
         * <p>
         * When a confirmation is expected, this event is called an indication. Otherwise it is called a notification.
         *
         * @param connectionType
         *            The type of connection - client or server
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device
         * @param handleValueIndication
         *            Boolean value specifying whether a confirmation is expected.
         * @param attributeHandle
         *            The attribute handle
         * @param attributeValue
         *            The value of the attribute
         */
        public void handleValueEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, boolean handleValueIndication, int attributeHandle, byte[] attributeValue);
    }

    /**
     * Generic Attribute Client Class
     */
    public static final class GenericAttributeClientManager extends GATM {
        /**
         * Constructor for a Generic Attribute Client object
         *
         * @param eventCallback
         *            Interface of callback functions
         *
         * @throws ServerNotReachableException
         *             if the BluetopiaPM server cannot be reached
         */
        public GenericAttributeClientManager(ClientEventCallback eventCallback) throws ServerNotReachableException {
            super(MANAGER_TYPE_CLIENT, eventCallback);
        }

        /**
         * Retrieves a list of information about GATT-based services on the
         * remote device.
         *
         * <i>Note:</i> This method acts on the copy of the remote device's
         * service records cached by the Platform Manager service. If there are
         * no cached records available, no results will be returned. Use the
         * DEVM (Device Manager) API to determine whether cached records are
         * available and to request that records be updated.
         *
         * @param remoteDeviceAddress
         *            Bluetooth address of the remote device.
         * @return A list of available GATT services on the remote device, built
         *         from the locally cached copy of the device's service records,
         *         or {@code null} if an error occurs.
         */
        public ServiceDefinition[] queryRemoteDeviceServices(BluetoothAddress remoteDeviceAddress) {
            byte[]              address          = remoteDeviceAddress.internalByteArray();
            Object[]            nativeParameters = new Object[11];
            ServiceDefinition[] services         = null;

            if(queryRemoteDeviceServicesNative(address[0], address[1], address[2], address[3], address[4], address[5], nativeParameters) == 0) {
                long[][]   serviceUUIDsMajor        = (long[][])   nativeParameters[0];
                long[][]   serviceUUIDsMinor        = (long[][])   nativeParameters[1];
                int[][]    startHandles             = (int[][])    nativeParameters[2];
                int[][]    endHandles               = (int[][])    nativeParameters[3];
                long[][]   characteristicUUIDsMajor = (long[][])   nativeParameters[4];
                long[][]   characteristicUUIDsMinor = (long[][])   nativeParameters[5];
                int[][]    characteristicHandles    = (int[][])    nativeParameters[6];
                int[][]    characteristicProperties = (int[][])    nativeParameters[7];
                long[][][] descriptorUUIDsMajor     = (long[][][]) nativeParameters[8];
                long[][][] descriptorUUIDsMinor     = (long[][][]) nativeParameters[9];
                int[][][]  descriptorHandles        = (int[][][])  nativeParameters[10];

                if((serviceUUIDsMajor != null) && (serviceUUIDsMinor != null) && (startHandles != null) && (endHandles != null) && (characteristicUUIDsMajor != null) && (characteristicUUIDsMinor != null) && (characteristicHandles != null) && (characteristicProperties != null)) {
                    if((serviceUUIDsMajor.length == serviceUUIDsMinor.length) && (serviceUUIDsMajor.length == startHandles.length) && (serviceUUIDsMajor.length == endHandles.length) && (serviceUUIDsMajor.length == characteristicUUIDsMajor.length) && (serviceUUIDsMajor.length == characteristicUUIDsMinor.length) && (serviceUUIDsMajor.length == characteristicHandles.length) && (serviceUUIDsMajor.length == characteristicProperties.length) && (serviceUUIDsMajor.length ==  descriptorUUIDsMajor.length) && (serviceUUIDsMajor.length == descriptorUUIDsMinor.length) && (serviceUUIDsMajor.length == descriptorHandles.length)) {
                        services = new ServiceDefinition[serviceUUIDsMajor.length];

                        for(int i = 0; i < services.length; i++) {
                            if((serviceUUIDsMajor[i] != null) && (serviceUUIDsMinor[i] != null) && (startHandles[i] != null) && (endHandles[i] != null)) {
                                if((serviceUUIDsMajor[i].length >= 1) && (serviceUUIDsMajor[i].length == serviceUUIDsMinor[i].length) && (serviceUUIDsMajor[i].length == startHandles[i].length) && (serviceUUIDsMajor[i].length == endHandles[i].length)) {
                                    ServiceInformation         service;
                                    ServiceInformation[]       includedServices;
                                    CharacteristicDefinition[] characteristics;

                                    service = new ServiceInformation(new UUID(serviceUUIDsMajor[i][0], serviceUUIDsMinor[i][0]), startHandles[i][0], endHandles[i][0]);

                                    includedServices = new ServiceInformation[serviceUUIDsMajor[i].length - 1];
                                    for(int j = 0; j < includedServices.length; j++) {
                                        includedServices[j] = new ServiceInformation(new UUID(serviceUUIDsMajor[i][j+1], serviceUUIDsMinor[i][j+1]), startHandles[i][j+1], endHandles[i][j+1]);
                                    }

                                    if((characteristicUUIDsMajor[i] != null) && (characteristicUUIDsMinor[i] != null) && (characteristicHandles[i] != null) && (characteristicProperties[i] != null)) {
                                        if((characteristicUUIDsMajor[i].length > 0) && (characteristicUUIDsMajor[i].length == characteristicUUIDsMinor[i].length) && (characteristicUUIDsMajor[i].length == characteristicHandles[i].length) && (characteristicUUIDsMajor[i].length == characteristicProperties[i].length)) {
                                            characteristics = new CharacteristicDefinition[characteristicUUIDsMajor[i].length];

                                            for(int j = 0; j < characteristics.length; j++) {
                                                CharacteristicDescriptor[] descriptors = null;

                                                /*
                                                 * Build the descriptor list, if available.
                                                 */
                                                if((descriptorUUIDsMajor[i] != null) && (descriptorUUIDsMinor[i] != null) && (descriptorHandles[i] != null)) {
                                                    if((characteristicUUIDsMajor[i].length == descriptorUUIDsMajor[i].length) && (characteristicUUIDsMajor[i].length == descriptorUUIDsMinor[i].length) && (characteristicUUIDsMajor[i].length == descriptorHandles[i].length)) {
                                                        if((descriptorUUIDsMajor[i][j] != null) && (descriptorUUIDsMinor[i][j] != null) && (descriptorHandles[i][j] != null)) {
                                                            if((descriptorUUIDsMajor[i][j].length == descriptorUUIDsMinor[i][j].length) && (descriptorUUIDsMajor[i][j].length == descriptorHandles[i][j].length)) {
                                                                descriptors = new CharacteristicDescriptor[descriptorUUIDsMajor[i][j].length];

                                                                for(int k = 0; k < descriptorUUIDsMajor[i][j].length; k++) {
                                                                    descriptors[k] = new CharacteristicDescriptor(new UUID(descriptorUUIDsMajor[i][j][k], descriptorUUIDsMinor[i][j][k]), descriptorHandles[i][j][k]);
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                if(descriptors == null)
                                                    descriptors = new CharacteristicDescriptor[0];

                                                characteristics[j] = new CharacteristicDefinition(new UUID(characteristicUUIDsMajor[i][j], characteristicUUIDsMinor[i][j]), characteristicHandles[i][j], characteristicProperties[i][j], descriptors);
                                            }
                                        } else {
                                            characteristics = new CharacteristicDefinition[0];
                                        }
                                    } else {
                                        /*
                                         * Characteristics for this service were
                                         * not correctly formatted. Abort all
                                         * service parsing and return an error.
                                         */
                                        services = null;
                                        break;
                                    }

                                    services[i] = new ServiceDefinition(service, includedServices, characteristics);
                                } else {
                                    /*
                                     * The Primary Service was undefined. Abort
                                     * all service parsing and return an error.
                                     */
                                    services = null;
                                    break;
                                }
                            } else {
                                /*
                                 * The service data was not returned. Abort all
                                 * service parsing and return an error.
                                 */
                                services = null;
                                break;
                            }
                        }
                    }
                }
            }

            return services;
        }

        /**
         * Request an attribute value from a remote device.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device
         * @param attributeHandle
         *            The handle of the attribute to read
         * @param offset
         *            The offset at which to begin reading the attribute
         * @param readAll
         *            Boolean value that specifies whether the server should continue
         *            sending packets if the value cannot fit into a single packet
         * @return Return a positive value if successful, or a negative code in case of an error.
         *         <p>
         *         <i>Note:</i> a positive return value is a
         *         {@code TransactionID} which can be used to track the response to this call.
         *
         * @see ClientEventCallback#readResponseEvent
         */
        public int readValue(BluetoothAddress remoteDeviceAddress, int attributeHandle, int offset, boolean readAll) {

            byte[] address;

            address = remoteDeviceAddress.internalByteArray();

            return readValueNative(address[0], address[1], address[2], address[3], address[4], address[5], attributeHandle, offset, readAll);
        }

        /**
         * Edit an attribute on the remote device.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device
         * @param attributeHandle
         *            The attribute handle
         * @param data
         *            The data to be written
         * @return Return a positive value if successful, or a negative code in case of an error.
         *         <p>
         *         <i>Note:</i> a positive return value is a
         *         {@code TransactionID} which can be used to track the response to this call.
         *
         * @see ClientEventCallback#writeResponseEvent
         */
        public int writeValue(BluetoothAddress remoteDeviceAddress, int attributeHandle, byte[] data) {

            byte[] address = remoteDeviceAddress.internalByteArray();

            return writeValueNative(address[0], address[1], address[2], address[3], address[4], address[5], attributeHandle, data);
        }

        /**
         * Edit an attribute on the remote device with no confirmation response.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device
         * @param attributeHandle
         *            The attribute handle
         * @param data
         *            The value of the attribute.
         *
         * @return Return the number of bytes written, or a negative code in case of an error.
         *         <p>
         *         <i>Note:</i> There will be no response.
         *         <p>
         *         <i>Note:</i> A signed write can only be used when the remote device and local device have previously paired.
         *
         */
        public int writeValueWithoutResponse(BluetoothAddress remoteDeviceAddress, int attributeHandle, byte[] data) {

            byte[] address = remoteDeviceAddress.internalByteArray();

            return writeValueWithoutResponseNative(address[0], address[1], address[2], address[3], address[4], address[5], attributeHandle, data);
        }

        @Override
        protected void readResponse(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, int handle, boolean isFinal, byte[] value) {

            ConnectionType connectionType;
            EventCallback  callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {

                switch(connectionTypeConstant) {
                case CONNECTION_TYPE_LOW_ENERGY:
                    connectionType = ConnectionType.LOW_ENERGY;
                    break;
                case CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE:
                default:
                    connectionType = ConnectionType.BASIC_RATE_ENHANCED_DATA_RATE;
                    break;
                }

                try {
                    ((ClientEventCallback)callbackHandler).readResponseEvent(connectionType, new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), transactionID, handle, isFinal, value);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

        }

        @Override
        protected void writeResponse(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, int handle) {

            ConnectionType connectionType;
            EventCallback  callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {

                switch(connectionTypeConstant) {
                case CONNECTION_TYPE_LOW_ENERGY:
                    connectionType = ConnectionType.LOW_ENERGY;
                    break;
                case CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE:
                default:
                    connectionType = ConnectionType.BASIC_RATE_ENHANCED_DATA_RATE;
                    break;
                }

                try {
                    ((ClientEventCallback)callbackHandler).writeResponseEvent(connectionType, new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), transactionID, handle);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

        }

        @Override
        protected void errorResponse(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int transactionID, int handle, int requestErrorTypeConstant, short attributeProtocolErrorCode) {

            ConnectionType             connectionType;
            RequestErrorType           requestErrorType;
            AttributeProtocolErrorType attributeProtocolErrorType;
            EventCallback              callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {

                switch(connectionTypeConstant) {
                    case CONNECTION_TYPE_LOW_ENERGY:
                        connectionType = ConnectionType.LOW_ENERGY;
                        break;
                    case CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE:
                    default:
                        connectionType = ConnectionType.BASIC_RATE_ENHANCED_DATA_RATE;
                        break;
                }

                switch(requestErrorTypeConstant) {
                    case REQUEST_ERROR_TYPE_ERROR_RESPONSE:
                        requestErrorType = RequestErrorType.ERROR_RESPONSE;
                        break;
                    case REQUEST_ERROR_TYPE_PROTOCOL_TIMEOUT:
                        requestErrorType = RequestErrorType.PROTOCOL_TIMEOUT;
                        break;
                    case REQUEST_ERROR_TYPE_PREPARE_WRITE_DATA_MISMATCH:
                        requestErrorType = RequestErrorType.PREPARE_WRITE_DATA_MISMATCH;
                        break;
                    default:
                        requestErrorType = null;
                        break;
                }

                if(requestErrorType == RequestErrorType.ERROR_RESPONSE) {

                    switch(attributeProtocolErrorCode) {
                        case ATT_PROTOCOL_ERROR_CODE_INVALID_HANDLE:
                            attributeProtocolErrorType = AttributeProtocolErrorType.INVALID_HANDLE;
                            break;
                        case ATT_PROTOCOL_ERROR_CODE_READ_NOT_PERMITTED:
                            attributeProtocolErrorType = AttributeProtocolErrorType.READ_NOT_PERMITTED;
                            break;
                        case ATT_PROTOCOL_ERROR_CODE_WRITE_NOT_PERMITTED:
                            attributeProtocolErrorType = AttributeProtocolErrorType.WRITE_NOT_PERMITTED;
                            break;
                        case ATT_PROTOCOL_ERROR_CODE_INVALID_PDU:
                            attributeProtocolErrorType = AttributeProtocolErrorType.INVALID_PDU;
                            break;
                        case ATT_PROTOCOL_ERROR_CODE_INSUFFICIENT_AUTHENTICATION:
                            attributeProtocolErrorType = AttributeProtocolErrorType.INSUFFICIENT_AUTHENTICATION;
                            break;
                        case ATT_PROTOCOL_ERROR_CODE_REQUEST_NOT_SUPPORTED:
                            attributeProtocolErrorType = AttributeProtocolErrorType.REQUEST_NOT_SUPPORTED;
                            break;
                        case ATT_PROTOCOL_ERROR_CODE_INVALID_OFFSET:
                            attributeProtocolErrorType = AttributeProtocolErrorType.INVALID_OFFSET;
                            break;
                        case ATT_PROTOCOL_ERROR_CODE_INSUFFICIENT_AUTHORIZATION:
                            attributeProtocolErrorType = AttributeProtocolErrorType.INSUFFICIENT_AUTHORIZATION;
                            break;
                        case ATT_PROTOCOL_ERROR_CODE_PREPARE_QUEUE_FULL:
                            attributeProtocolErrorType = AttributeProtocolErrorType.PREPARE_QUEUE_FULL;
                            break;
                        case ATT_PROTOCOL_ERROR_CODE_ATTRIBUTE_NOT_FOUND:
                            attributeProtocolErrorType = AttributeProtocolErrorType.ATTRIBUTE_NOT_FOUND;
                            break;
                        case ATT_PROTOCOL_ERROR_CODE_ATTRIBUTE_NOT_LONG:
                            attributeProtocolErrorType = AttributeProtocolErrorType.ATTRIBUTE_NOT_LONG;
                            break;
                        case ATT_PROTOCOL_ERROR_CODE_INSUFFICIENT_ENCRYPTION_KEY_SIZE:
                            attributeProtocolErrorType = AttributeProtocolErrorType.INSUFFICIENT_ENCRYPTION_KEY_SIZE;
                            break;
                        case ATT_PROTOCOL_ERROR_CODE_INVALID_ATTRIBUTE_VALUE_LENGTH:
                            attributeProtocolErrorType = AttributeProtocolErrorType.INVALID_ATTRIBUTE_VALUE_LENGTH;
                            break;
                        case ATT_PROTOCOL_ERROR_CODE_UNLIKELY_ERROR:
                            attributeProtocolErrorType = AttributeProtocolErrorType.UNLIKELY_ERROR;
                            break;
                        case ATT_PROTOCOL_ERROR_CODE_INSUFFICIENT_ENCRYPTION:
                            attributeProtocolErrorType = AttributeProtocolErrorType.INSUFFICIENT_ENCRYPTION;
                            break;
                        case ATT_PROTOCOL_ERROR_CODE_UNSUPPORTED_GROUP_TYPE:
                            attributeProtocolErrorType = AttributeProtocolErrorType.UNSUPPORTED_GROUP_TYPE;
                            break;
                        case ATT_PROTOCOL_ERROR_CODE_INSUFFICIENT_RESOURCES:
                            attributeProtocolErrorType = AttributeProtocolErrorType.INSUFFICIENT_RESOURCES;
                            break;
                        default:
                            attributeProtocolErrorType = null;
                            break;
                    }
                } else {

                    attributeProtocolErrorType = null;
                }


                try {
                    ((ClientEventCallback)callbackHandler).errorResponseEvent(connectionType, new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), transactionID, handle, requestErrorType, attributeProtocolErrorType);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

        }

        /**
         * Interface of GATT Client callback functions
         * <p>
         * It is guaranteed that no two event methods will be invoked
         * simultaneously. The implementation of each event handler should as
         * efficient as possible, as subsequent events will not be sent until
         * the event handler returns. Event handlers must not block and wait on
         * conditions that can only be satisfied by receiving other events.
         * <p>
         * Users should also note that events are sent from a thread context
         * owned by the BluetopiaPM API. Standard locking practices should be
         * observed.
         */
        public interface ClientEventCallback extends EventCallback {

            /**
             * React to the response to a readValue request.
             *
             * @param connectionType
             *            The type of connection - client or server
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device
             * @param transactionID
             *            The transactionID of the read attempt, as
             *            returned by the call to
             *            {@link GenericAttributeClientManager#readValue}.
             * @param handle
             *            The attribute handle
             * @param isFinal
             *            Boolean value indicating whether the packet completes the read response
             * @param value
             *            The attribute value
             */
            public void readResponseEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int transactionID, int handle, boolean isFinal, byte[] value);

            /**
             * React to the response to a writeValue request.
             *
             * @param connectionType
             *            The type of connection - client or server
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device
             * @param transactionID
             *            The transactionID of the write attempt, as
             *            returned by the call to
             *            {@link GenericAttributeClientManager#writeValue}.
             * @param handle
             *            The attribute handle.
             */
            public void writeResponseEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int transactionID, int handle);

            /**
             * React to the delivery of an error notification.
             * <p>
             * If and
             *
             * @param connectionType
             *            The type of connection - client or server
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device
             * @param transactionID
             *            The transactionID of the request, as
             *            returned by the call to either
             *            {@link GenericAttributeClientManager#readValue} or
             *            {@link GenericAttributeClientManager#writeValue}.
             * @param handle
             *            The attribute handle from the failed request
             * @param requestErrorType
             *            The type of error
             * @param attributeProtocolErrorType
             *            The attribute protocol error code
             *            <p>
             *            <i>Note:</i> This error code is only valid if the
             *            {@code requestErrorType} member is
             *            {@link RequestErrorType#ERROR_RESPONSE}.
             */
            public void errorResponseEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int transactionID, int handle, RequestErrorType requestErrorType, AttributeProtocolErrorType attributeProtocolErrorType);
        }
    }

    /**
     * Generic Attribute Server Class
     */
    public static final class GenericAttributeServerManager extends GATM {

        private final EnumSet<ServiceFlag>      serviceFlags;

        private final HashMap<Integer, Service> serviceByServiceID;

        /**
         * Creates a new management connection to the local GATT Server Manager.
         * Note that GATT Server profile connections are not persistent.
         * Connections are tied to the Manager object that creates them; when
         * the Manager object is disposed, the server and any associated
         * connections will be closed.
         * @param serviceFlags
         *
         * @param eventCallback
         *            Receiver for events sent by the server.
         *
         * @throws ServerNotReachableException
         *             if the BluetopiaPM server cannot be reached.
         */
        public GenericAttributeServerManager(EnumSet<ServiceFlag> serviceFlags, ServerEventCallback eventCallback) throws ServerNotReachableException {

            super(MANAGER_TYPE_SERVER, eventCallback);

            this.serviceFlags = serviceFlags;

            serviceByServiceID = new HashMap<Integer, Service>();
        }

        /**
         * Dispose of resources allocated by this manager object.
         */
        @Override
        public void dispose() {
            super.dispose();

            if(serviceByServiceID != null) {
                for(Service service : serviceByServiceID.values()) {
                    service.delete();
                }

                serviceByServiceID.clear();
            }
        }

        /**
         * Query all services published by the GATT Server.
         *
         * @return An array of matching, published services. The array may be empty
         *         if no matching services are found, or if an
         *         error occurs.
         */
        public ServiceInformation[] queryPublishedServices() {

            return queryPublishedServicesNative();
        }

        /**
         * Query services published by this GATT Server matching a particular
         * UUID.
         *
         * @param uuid
         *            UUID of the service definitions to query.
         *
         * @return An array of matching, published services. The array may be empty
         *         if no matching services are found, or if an
         *         error occurs.
         */
        public ServiceInformation[] queryPublishedServices(UUID uuid) {

            return queryPublishedServicesNative(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
        }

        @Override
        protected void writeRequest(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int serviceID, int requestID, int attributeOffset, byte[] attributeValue) {

            Service        service;
            Attribute      targetAttribute;
            ConnectionType connectionType;
            EventCallback  callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {

                switch(connectionTypeConstant) {
                case CONNECTION_TYPE_LOW_ENERGY:
                    connectionType = ConnectionType.LOW_ENERGY;
                    break;
                case CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE:
                default:
                    connectionType = ConnectionType.BASIC_RATE_ENHANCED_DATA_RATE;
                    break;
                }

                service = serviceByServiceID.get(serviceID);

                if(service != null) {
                    targetAttribute = service.getAttributeByOffset(attributeOffset);

                    if(targetAttribute != null) {
                        try {
                            targetAttribute.issueWriteRequestEvent(((ServerEventCallback)callbackHandler), connectionType, new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), requestID, attributeValue);
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        @Override
        protected void signedWrite(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int serviceID, boolean validSignature, int attributeOffset, byte[] attributeValue) {

            Service        service;
            Attribute      targetAttribute;
            ConnectionType connectionType;
            EventCallback  callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {

                switch(connectionTypeConstant) {
                case CONNECTION_TYPE_LOW_ENERGY:
                    connectionType = ConnectionType.LOW_ENERGY;
                    break;
                case CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE:
                default:
                    connectionType = ConnectionType.BASIC_RATE_ENHANCED_DATA_RATE;
                    break;
                }

                service = serviceByServiceID.get(serviceID);

                if(service != null) {
                    targetAttribute = service.getAttributeByOffset(attributeOffset);

                    if(targetAttribute != null) {
                        try {
                            targetAttribute.issueSignedWriteEvent((ServerEventCallback)callbackHandler, connectionType, new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), validSignature, attributeValue);
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        @Override
        protected void readRequest(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int serviceID, int requestID, int attributeOffset, int attributeValueOffset) {

            Service        service;
            Attribute      targetAttribute;
            ConnectionType connectionType;
            EventCallback  callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {

                switch(connectionTypeConstant) {
                case CONNECTION_TYPE_LOW_ENERGY:
                    connectionType = ConnectionType.LOW_ENERGY;
                    break;
                case CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE:
                default:
                    connectionType = ConnectionType.BASIC_RATE_ENHANCED_DATA_RATE;
                    break;
                }

                service = serviceByServiceID.get(serviceID);

                if(service != null) {
                    targetAttribute = service.getAttributeByOffset(attributeOffset);

                    if(targetAttribute != null) {
                        try {
                            targetAttribute.issueReadRequestEvent((ServerEventCallback)callbackHandler, connectionType, new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), requestID, attributeValueOffset);
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        @Override
        protected void prepareWriteRequest(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int serviceID, int requestID, int attributeOffset, int attributeValueOffset, byte[] attributeValue) {

            Service        service;
            Attribute      targetAttribute;
            ConnectionType connectionType;
            EventCallback  callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {

                switch(connectionTypeConstant) {
                case CONNECTION_TYPE_LOW_ENERGY:
                    connectionType = ConnectionType.LOW_ENERGY;
                    break;
                case CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE:
                default:
                    connectionType = ConnectionType.BASIC_RATE_ENHANCED_DATA_RATE;
                    break;
                }

                service = serviceByServiceID.get(serviceID);

                if(service != null) {
                    targetAttribute = service.getAttributeByOffset(attributeOffset);

                    if(targetAttribute != null) {
                        try {
                            targetAttribute.issuePrepareWriteRequestEvent((ServerEventCallback)callbackHandler, connectionType, new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), requestID, attributeValueOffset, attributeValue);
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        @Override
        protected void commitPrepareWrite(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int serviceID, boolean commitWrites) {

            Service        service;
            ConnectionType connectionType;
            EventCallback  callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {

                switch(connectionTypeConstant) {
                case CONNECTION_TYPE_LOW_ENERGY:
                    connectionType = ConnectionType.LOW_ENERGY;
                    break;
                case CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE:
                default:
                    connectionType = ConnectionType.BASIC_RATE_ENHANCED_DATA_RATE;
                    break;
                }

                service = serviceByServiceID.get(serviceID);

                if(service != null) {
                    try {
                        ((ServerEventCallback)callbackHandler).commitPrepareWriteEvent(connectionType, new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), service, commitWrites);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        protected void handleValueConfirmation(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int serviceID, int transactionID, int attributeOffset, int confirmationStatusConstant) {

            Service                       service;
            Attribute                     targetAttribute;
            ConnectionType                connectionType;
            HandleValueConfirmationStatus confirmationStatus;
            EventCallback                 callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {

                switch(connectionTypeConstant) {
                case CONNECTION_TYPE_LOW_ENERGY:
                    connectionType = ConnectionType.LOW_ENERGY;
                    break;
                case CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE:
                default:
                    connectionType = ConnectionType.BASIC_RATE_ENHANCED_DATA_RATE;
                    break;
                }

                switch(confirmationStatusConstant) {
                case HANDLE_VALUE_CONFIRMATION_STATUS_TIMEOUT:
                    confirmationStatus = HandleValueConfirmationStatus.TIMEOUT;
                    break;
                case HANDLE_VALUE_CONFIRMATION_STATUS_SUCCESS:
                default:
                    confirmationStatus = HandleValueConfirmationStatus.SUCCESS;
                    break;
                }


                service = serviceByServiceID.get(serviceID);

                if(service != null) {
                    targetAttribute = service.getAttributeByOffset(attributeOffset);

                    if(targetAttribute != null) {
                        try {
                            targetAttribute.issueHandleValueConfirmationEvent((ServerEventCallback)callbackHandler, connectionType, new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), transactionID, confirmationStatus);
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        /**
         * getServices
         *
         * @return Return the array list of Service objects.
         */
        public List<Service> getServices() {

            return new ArrayList<Service>(serviceByServiceID.values());
        }

        /**
         * Register a {@link Service} and publish it in the GATT database.
         *
         * Any included services will also be registered, if they are not
         * already {@link Service} .
         *
         * @param service
         *            The service to add.
         *
         * @return Return the platform manager return code from
         *         GATM_RegisterService. A positive value, the service ID,
         *         indicates success. A negative number indicates an error has
         *         occurred. See BTPMERR.h and GATMAPI.h.
         */
        public int registerService(Service service) {
            return registerService(service, 0);
        }

        /**
         * Register a {@link Service} and publish it in the GATT database at the
         * reserved location identified by the provided UID.
         *
         * Any included services will also be registered, if they are not
         * already {@link Service} .
         *
         * @param service
         *            The service to add
         * @param reservedHandleRangeUID
         *            The UID obtained by previously reserving space in the GATT
         *            table for publishing this Service.
         *
         * @return Return the platform manager return code from
         *         GATM_RegisterService. A positive value, the service ID,
         *         indicates success. A negative number indicates an error has
         *         occurred. See BTPMERR.h and GATMAPI.h.
         */
        public int registerService(Service service, int reservedHandleRangeUID) {

            int serviceID = -1;

            if(service != null) {
                service.setManager(this);

                synchronized(serviceByServiceID) {
                    if(serviceByServiceID.containsValue(service) == false) {

                        serviceID = service.register(ServiceFlag.maskFromSet(serviceFlags), reservedHandleRangeUID);

                        if(serviceID >= 0) {
                            serviceByServiceID.put(serviceID, service);
                        } else {
                            service.setManager(null);
                        }
                    } else {
                        throw new IllegalStateException("Service already registered to this Manager");
                    }
                }
            } else {
                throw new NullPointerException();
            }

            return serviceID;
        }

        /**
         * Unregister a previously registered {@link Service}.
         *
         * @param service
         *            The Service to be unregistered.
         *
         * @return <code>true</code> if the Service was previously registered
         *         and is now unregistered.
         */
        public boolean unregisterService(Service service) {

            if(service != null) {
                service = serviceByServiceID.remove(service.getServiceID());

                if(service != null) {
                    service.delete();
                }
            }

            return (service != null);
        }

        /**
         * Allocate a persistent reservation of a range of attribute handles
         * sufficient to store the given {@link Service}. If this reservation is
         * successful then the reservation will be persistent across executions
         * of the application and Bluetooth stack.
         *
         * Note that the Service must be fully defined with all Characteristics,
         * Descriptors, and Included Services added.
         *
         * @return The Handle Range ID for this reserved handle range.
         *
         * @throws BufferOverflowException
         *             No contiguous block of handles sufficient to hold the
         *             {@link Service} could be found.
         */
        public int allocateReservedHandleRange(Service service) {

            int[] handleData  = new int[3];

            if(registerPersistentUIDNative(service.countHandles(), handleData) == 0)
                return handleData[0];
            else
                throw new BufferOverflowException();
        }

        /**
         * Release a previously reserved handle range.
         *
         * @throws IllegalArgumentException
         *             The specified Handle Range ID is not valid.
         */
        public void releaseReservedHandleRange(int handleRangeID) {

            if(unRegisterPersistentUIDNative(handleRangeID) != 0) {
                throw new IllegalArgumentException();
            }
        }

        /**
         * Receiver for GATT Server events
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
        public interface ServerEventCallback extends EventCallback {

            /**
             *
             * @param connection
             * @param requestID
             * @param characteristic
             * @param value
             */
            public void writeCharacteristicRequestEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, Characteristic characteristic, int requestID, byte[] value);

            /**
             *
             * @param connection
             * @param requestID
             * @param descriptor
             * @param value
             */
            public void writeDescriptorRequestEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, Descriptor descriptor, int requestID, byte[] value);

            /**
             *
             * @param connection
             * @param validSignature
             * @param characteristic
             * @param value
             */
            public void signedWriteCharacteristicEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, Characteristic characteristic, boolean validSignature, byte[] value);

           /**
             *
             * @param connection
             * @param validSignature
             * @param descriptor
             * @param value
             */
            public void signedWriteDescriptorEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, Descriptor descriptor, boolean validSignature, byte[] value);

            /**
             *
             * @param connection
             * @param requestID
             * @param characteristic
             * @param valueOffset
             */
            public void readCharacteristicRequestEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, Characteristic characteristic, int requestID, int valueOffset);

            /**
             *
             * @param connection
             * @param requestID
             * @param descriptor
             * @param valueOffset
             */
            public void readDescriptorRequestEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, Descriptor descriptor, int requestID, int valueOffset);

            /**
             *
             * @param connection
             * @param requestID
             * @param characteristic
             * @param valueOffset
             * @param value
             */
            public void prepareCharacteristicWriteRequestEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, Characteristic characteristic, int requestID, int valueOffset, byte[] value);

            /**
             *
             * @param connection
             * @param requestID
             * @param descriptor
             * @param valueOffset
             * @param value
             */
            public void prepareDescriptorWriteRequestEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, Descriptor descriptor, int requestID, int valueOffset, byte[] value);

            /**
             *
             * @param connection
             * @param commitWrites
             */
            public void commitPrepareWriteEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, Service service, boolean commitWrites);

            /**
             *
             * @param connection
             * @param transactionID
             * @param confirmationStatus
             */
            public void handleValueConfirmationEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, Characteristic characteristic, int transactionID, HandleValueConfirmationStatus confirmationStatus);
        }

        /**
         * Extended Properties Descriptor UUID
         */
        public final static UUID extendedPropertiesDescriptorUUID  = UUID.fromString("00002900-0000-1000-8000-00805F9B34FB");
        /**
         * User Description Descriptor UUID
         */
        public final static UUID userDescriptionDescriptorUUID     = UUID.fromString("00002901-0000-1000-8000-00805F9B34FB");
        /**
         * Client Configuration Descriptor UUID
         */
        public final static UUID clientConfigurationDescriptorUUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");
        /**
         * Server Configuration Descriptor UUID
         */
        public final static UUID serverConfigurationDescriptorUUID = UUID.fromString("00002903-0000-1000-8000-00805F9B34FB");
        /**
         * Presentation Format Descriptor UUID
         */
        public final static UUID presentationFormatDescriptorUUID  = UUID.fromString("00002904-0000-1000-8000-00805F9B34FB");
        /**
         * Aggregate Format Descriptor UUID
         */
        public final static UUID aggregateFormatDescriptorUUID     = UUID.fromString("00002904-0000-1000-8000-00805F9B34FB");
    }

    /**
     * Definition of a GATT service that can be offered by the local GATT
     * server.
     */
    public static class Service {

        private final UUID                        uuid;
        private       int                         serviceID;
        private final ArrayList<ServiceInclude>   includedServices;
        private final ArrayList<Characteristic>   characteristics;
        private final boolean                     primary;
        private final TreeMap<Integer, Attribute> attributeByOffset;

        private WeakReference<GenericAttributeServerManager> managerRef;

        public Service(UUID uuid, boolean primary) {

            this.uuid         = uuid;
            this.primary      = primary;
            includedServices  = new ArrayList<ServiceInclude>();
            characteristics   = new ArrayList<Characteristic>();
            attributeByOffset = new TreeMap<Integer, Attribute>();
        }

        /**
         * getAttributeByOffset
         *
         * @param offset - offset of the attribute within the service
         *
         * @return Return the object mapped to the given offset.
         *         The Object might be a ServiceInclude, a Characteristic or a Descriptor.
         */
        /*pkg*/ Attribute getAttributeByOffset(int offset) {

            Attribute attribute;

            synchronized(attributeByOffset) {
                attribute = attributeByOffset.get(offset);

                if(attribute == null) {
                    Entry<Integer, Attribute> entry = attributeByOffset.floorEntry(offset);

                    if(entry != null) {
                        attribute = entry.getValue().getChildAttributeByOffset(offset);
                    }
                }
            }

            return attribute;
        }

        /**
         * @return Total number of handles from the GATT table consumed by the
         *         attributes of this service.
         */
        /*pkg*/ int countHandles() {

            int totalHandles = 0;

            for(Attribute attribute : includedServices) {
                totalHandles += attribute.countHandles();
            }

            for(Attribute attribute : characteristics) {
                totalHandles += attribute.countHandles();
            }

            return totalHandles;
        }

        /**
         * getServiceID
         *
         * @return Return the ServiceID.
         */
        public int getServiceID() {

            if(managerRef != null) {
                return serviceID;
            } else {
                throw new IllegalStateException("Service ID is only available after registering a Service");
            }
        }

        /**
         * getUUID
         *
         * @return Return the UUID.
         */
        public UUID getUUID() {

            return uuid;
        }

        /**
         * isPrimary
         *
         * @return Return true when the service has been registered as a primary service.
         */
        public boolean isPrimary() {

            return primary;
        }

        /**
         * Include a Service within this Service.
         *
         * @param serviceToInclude - the service to include
         */
        public void addInclude(Service serviceToInclude) {

            if(managerRef == null) {
                if(serviceToInclude != null) {

                    ServiceInclude includeDecl = new ServiceInclude(serviceToInclude);

                    includeDecl.setService(this);

                    synchronized(includedServices) {
                        includedServices.add(includeDecl);
                    }
                } else {
                    throw new NullPointerException();
                }
            } else {
                throw new IllegalStateException("Cannot include a Service after the parent Service is registered");
            }
        }

        /**
         * @return The list of included services.
         */
        public List<Service> getIncludedServices() {
            ArrayList<Service> services;

            synchronized(includedServices) {
                services = new ArrayList<Service>(includedServices.size());

                for(ServiceInclude serviceInclude : includedServices) {
                    services.add(serviceInclude.getIncludedService());
                }
            }

            return services;
        }

        /**
         * Register a {@link Characteristic} to this {@link Service}.
         *
         * @param characteristic The Characteristic to be registered.
         */
        public void addCharacteristic(Characteristic characteristic) {

            if(managerRef == null) {
                if(characteristic != null) {
                    characteristic.setService(this);

                    synchronized(characteristics) {
                        characteristics.add(characteristic);
                    }
                } else {
                    throw new NullPointerException();
                }
            } else {
                throw new IllegalStateException("Cannot add a Characteristic after the Service is registered");
            }
        }

        /**
         * @return The list of registered {@link Characteristic}s.
         */
        public List<Characteristic> getCharacteristics() {

            synchronized(characteristics) {
                return new ArrayList<Characteristic>(characteristics);
            }
        }

        /*pkg*/ int register(int serviceFlags, int reservedHandleRangeUID) {

            int result;
            GenericAttributeServerManager manager = getManager();

            result = manager.registerServiceNative(primary, countHandles(), uuid.getMostSignificantBits(), uuid.getLeastSignificantBits(), reservedHandleRangeUID);

            if(result > 0) {
                serviceID = result;

                result = registerAttributeList(includedServices);

                if(result == 0)
                    result = registerAttributeList(characteristics);

                if(result == 0)
                    result = manager.publishServiceNative(serviceID, serviceFlags, null);

                if(result == 0) {
                    result = serviceID;
                } else {
                    delete();
                }
            }

            return result;
        }

        /*pkg*/ void delete() {
            getManager().deleteServiceNative(serviceID);

            serviceID = 0;

            for(Attribute attribute : attributeByOffset.values()) {
                attribute.delete();
            }

            attributeByOffset.clear();
        }

        private int registerAttributeList(Collection<? extends Attribute> attributeList) {

            int result = 0;
            int attrOffset;
            Entry<Integer, Attribute> lastEntry = attributeByOffset.lastEntry();

            if(lastEntry == null) {
                attrOffset = 1;
            } else {
                attrOffset = lastEntry.getKey() + lastEntry.getValue().countHandles();
            }

            for(Attribute attribute : attributeList) {
                result = attribute.register(attrOffset);

                if(result == 0) {
                    attributeByOffset.put(attrOffset, attribute);

                    attrOffset += attribute.countHandles();
                } else {
                    break;
                }
            }

            return result;
        }

        /*pkg*/ GenericAttributeServerManager getManager() {
            GenericAttributeServerManager manager = managerRef.get();

            if(manager != null) {
                return manager;
            } else {
                throw new IllegalStateException("Service must be registered to a GenericAttributeServerManager");
            }
        }

        /*pkg*/ synchronized void setManager(GenericAttributeServerManager manager) {

            if(manager == null) {
                managerRef = null;
            } else {
                if(managerRef == null) {
                    managerRef = new WeakReference<GenericAttributeServerManager>(manager);
                } else {
                    if(managerRef.get() != manager) {
                        throw new IllegalStateException("Cannot add a Service to more than one Manager");
                    }
                }
            }
        }
    }

    /**
     * Attribute Abstract Class
     *
     * The class is extended by Characteristic and Descriptor.
     */
    public static abstract class Attribute {

        protected int  offset;
        protected UUID uuid;
        protected long properties;
        protected long securityProperties;

        /*pkg*/ WeakReference<Service> serviceRef;

        /*pkg*/ final int numberAttributes;
        /*pkg*/ final int valueOffset;

        private Attribute(UUID uuid, long properties, long securityProperties, int numberAttributes, int valueOffset) {

            this.uuid               = uuid;
            this.properties         = properties;
            this.securityProperties = securityProperties;
            this.serviceRef         = null;
            this.numberAttributes   = numberAttributes;
            this.valueOffset        = valueOffset;
        }

        /**
         * setValue
         *
         * Set the value of an attribute.
         *
         * @param value
         *
         * @return Return the platform manager return code from GATM_AddServiceAttributeData.
         *         Zero indicates success. A negative number indicates an error has occurred.
         *         See BTPMERR.h and GATMAPI.h.
         */
        public int setStaticValue(byte[] value) {

            Service service = getService();

            return service.getManager().addServiceAttributeDataNative(service.getServiceID(), (offset + valueOffset), value);
        }

        /**
         * Confirm or reject a write request.
         *
         * @param requestID  - identifier of the write
         * @param resultCode - return value from the write that will determine whether an error response will be sent.
         *
         * @return Return the platform manager return code from either GATM_WriteResponse or GATM_ErrorResponse.
         *         Zero indicates success. A negative number indicates an error has occurred.
         *         See BTPMERR.h and GATMAPI.h.
         */
        public int verifyWrite(int requestID, AttributeProtocolErrorType resultCode) {

            Service service = getService();

            if(resultCode == AttributeProtocolErrorType.SUCCESS) {
                return service.getManager().writeResponseNative(requestID);
            } else {
                return service.getManager().errorResponseNative(requestID, resultCode.rawCode);
            }
        }

        /**
         * Send a portion of the value of the attribute to a remote device.
         * The offset dictates where in the whole value the returned segment begins.
         *
         * @param requestID Identifier of the read request.
         * @param value     Data to be sent to the GATT client.
         *
         * @return Return the platform manager return code from GATM_ReadResponse.
         *         Zero indicates success. A negative number indicates an error has occurred.
         *         See BTPMERR.h and GATMAPI.h.
         */
        public int sendRequestedValue(int requestID, byte[] value) {

            return getService().getManager().readResponseNative(requestID, value);
        }

        /**
         * getUUID
         *
         * @return Return attribute UUID.
         */
        public UUID getUUID() {

            return uuid;
        }

        /**
         * @return The {@link Service} to which this {@link Attribute} is
         *         registered.
         *
         * @throws IllegalStateException
         *             if this Attribute is not registered to any Service.
         */
        public Service getService() {

            if(serviceRef != null) {
                return serviceRef.get();
            } else {
                throw new IllegalStateException("Attribute must be bound to a Service");
            }
        }

        /*pkg*/ synchronized void setService(Service service) {

            if(service == null) {
                serviceRef = null;
            } else {
                if(serviceRef == null) {
                    serviceRef = new WeakReference<GATM.Service>(service);
                } else {
                    if(serviceRef.get() != service) {
                        throw new IllegalStateException("Cannot add an Attribute to more than one Service");
                    }
                }
            }
        }

        /**
         * @param offset
         *            Handle offset
         * @return The Attribute registered at the given offset, or
         *         <code>null</code> if none exists.
         */
        /*pkg*/ Attribute getChildAttributeByOffset(int offset) {

            return null;
        }

        /**
         * @return Number of entries in the GATT table consumed by this object.
         */
        /*pkg*/ int countHandles() {

            return 1;
        }

        /**
         * Register attribute with Bluetopia.
         *
         * @return the Platform Manager return code from the appropriate
         *         <code>GATM_AddService*()</code> method. Zero indicates
         *         success. A negative number indicates an error has occured.
         *         See BTPMERR.h and GATMAPI.h
         */
        /*pkg*/ int register(int baseOffset) {

            if(offset == 0) {
                offset = baseOffset;
            } else {
                throw new IllegalStateException("Attribute already registered");
            }

            return 0;
        }

        /*pkg*/ void delete() {

            if(offset != 0) {
                offset = 0;
            } else {
                throw new IllegalStateException("Attribute not registered");
            }
        }

        /*pkg*/ void issueWriteRequestEvent(ServerEventCallback handler, ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int requestID, byte[] value) {
            throw new UnsupportedOperationException();
        }

        /*pkg*/ void issueSignedWriteEvent(ServerEventCallback handler, ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, boolean validSignature, byte[] value) {
            throw new UnsupportedOperationException();
        }

        /*pkg*/ void issueReadRequestEvent(ServerEventCallback handler, ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int requestID, int valueOffset) {
            throw new UnsupportedOperationException();
        }

        /*pkg*/ void issuePrepareWriteRequestEvent(ServerEventCallback handler, ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int requestID, int valueOffset, byte[] value) {
            throw new UnsupportedOperationException();
        }

        /*pkg*/ void issueHandleValueConfirmationEvent(ServerEventCallback handler, ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int transactionID, HandleValueConfirmationStatus confirmationStatus) {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * Characteristic Class extends Attribute
     * GATT Characteristic
     */
    public static class Characteristic extends Attribute {

        private final ArrayList<Descriptor>       descriptors;
        private final TreeMap<Integer, Attribute> attributeByOffset;

        public Characteristic(UUID uuid, EnumSet<CharacteristicProperty> characteristicProperties, EnumSet<SecurityProperty> securityProperties) {

            super(uuid, CharacteristicProperty.maskFromSet(characteristicProperties), SecurityProperty.maskFromSet(securityProperties), 2, 1);

            descriptors = new ArrayList<Descriptor>();
            attributeByOffset = new TreeMap<Integer, Attribute>();
        }

        public Characteristic(UUID uuid, long properties, long securityProperties) {

            super(uuid, properties, securityProperties, 2, 1);

            descriptors = new ArrayList<Descriptor>();
            attributeByOffset = new TreeMap<Integer, Attribute>();
        }

        /**
         * Send the attribute value to a remote device that has configured either indications(confirmation expected) or
         * notifications. The
         *
         * @param remoteDeviceAddress - address of remote device
         * @param requestConfirmation - determines whether the message requires a confirmation that it has been received
         *
         * @return Return the platform manager return code from GATM_SendHandleValueNotification
         *         or GATM_SendHandleValueIndication.
         *         GATM_SendHandleValueIndication returns a transaction ID that can be matched with the ID in a
         *         handle value confirmation event. Otherwise a zero indicates success.
         *         A negative number indicates an error has occurred.
         *         See BTPMERR.h and GATMAPI.h.
         */
        public int sendNotification(BluetoothAddress remoteDeviceAddress, byte[] value, boolean requestConfirmation) {

            int     returnValue;
            byte[]  address;
            Service service = getService();

            address = remoteDeviceAddress.internalByteArray();

            if(requestConfirmation)
                returnValue = service.getManager().sendHandleValueIndicationNative(address[0], address[1], address[2], address[3], address[4], address[5], service.getServiceID(), offset, value);
            else
                returnValue = service.getManager().sendHandleValueNotificationNative(address[0], address[1], address[2], address[3], address[4], address[5], service.getServiceID(), offset, value);

            return returnValue;
        }

        /**
         * Add a descriptor to a characteristic's descriptors array list.
         *
         * @param descriptor Descriptor to add to this Characteristic.
         *
         * @throws NullPointerException if <code>descriptor</code> is <code>null</code>.
         */
        public void addDescriptor(Descriptor descriptor) {

            if(offset == 0) {
                if(descriptor != null) {
                    descriptor.setCharacteristic(this);

                    if(serviceRef != null) {
                        descriptor.setService(serviceRef.get());
                    }

                    synchronized(descriptors) {
                        descriptors.add(descriptor);
                    }
                } else {
                    throw new NullPointerException();
                }
            } else {
                throw new IllegalStateException("Cannot add a Descriptor after the Characteristic is registered");
            }
        }

        /**
         * @return The list of registered {@link Descriptor} objects for this
         *         {@link Characteristic}.
         */
        public List<Descriptor> getDescriptors() {

            return new ArrayList<Descriptor>(descriptors);
        }

        /*pkg*/ synchronized void setService(Service service) {

            super.setService(service);

            for(Descriptor descriptor : descriptors) {
                descriptor.setService(service);
            }
        }

        /**
         * @param offset
         *            Handle offset
         * @return The Attribute registered at the given offset, or
         *         <code>null</code> if none exists.
         */
        @Override
        /*pkg*/ Attribute getChildAttributeByOffset(int offset) {

            Attribute attribute;

            synchronized(attributeByOffset) {
                attribute = attributeByOffset.get(offset);

                if(attribute == null) {
                    Entry<Integer, Attribute> entry = attributeByOffset.floorEntry(offset);

                    if(entry != null) {
                        attribute = entry.getValue().getChildAttributeByOffset(offset);
                    }
                }
            }

            return attribute;
        }

        @Override
        /*pkg*/ int countHandles() {
            int handles = 2;

            for(Descriptor descriptor : descriptors) {
                handles += descriptor.countHandles();
            }

            return handles;
        }

        /**
         * Register {@link Characteristic} with Bluetopia.
         *
         * @return the Platform Manager return code from
         *         <code>GATM_AddServiceCharacteristic()</code>. Zero indicates
         *         success. A negative number indicates an error has occurred.
         *         See BTPMERR.h and GATMAPI.h
         */
        @Override
        /*pkg*/ int register(int baseOffset) {

            int result = super.register(baseOffset);

            if(result == 0) {
                Service service = getService();

                result = service.getManager().addServiceCharacteristicNative(service.getServiceID(), baseOffset, properties, securityProperties, uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());

                if(result == 0) {

                    baseOffset += numberAttributes;

                    for(Descriptor descriptor : descriptors) {
                        result = descriptor.register(baseOffset);

                        if(result == 0) {
                            attributeByOffset.put(baseOffset, descriptor);
                        } else {
                            break;
                        }
                    }
                }
            }

            return result;
        }

        @Override
        /*pkg*/ void delete() {
            super.delete();

            for(Descriptor descriptor : descriptors) {
                descriptor.delete();
            }

            descriptors.clear();
        }

        @Override
        /*pkg*/ void issueWriteRequestEvent(ServerEventCallback handler, ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int requestID, byte[] value) {

            handler.writeCharacteristicRequestEvent(connectionType, remoteDeviceAddress, this, requestID, value);
        }

        @Override
        /*pkg*/ void issueSignedWriteEvent(ServerEventCallback handler, ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, boolean validSignature, byte[] value) {

            handler.signedWriteCharacteristicEvent(connectionType, remoteDeviceAddress, this, validSignature, value);
        }

        @Override
        /*pkg*/ void issueReadRequestEvent(ServerEventCallback handler, ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int requestID, int valueOffset) {

            handler.readCharacteristicRequestEvent(connectionType, remoteDeviceAddress, this, requestID, valueOffset);
        }

        @Override
        /*pkg*/ void issuePrepareWriteRequestEvent(ServerEventCallback handler, ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int requestID, int valueOffset, byte[] value) {

            handler.prepareCharacteristicWriteRequestEvent(connectionType, remoteDeviceAddress, this, requestID, valueOffset, value);
        }

        @Override
        /*pkg*/ void issueHandleValueConfirmationEvent(ServerEventCallback handler, ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int transactionID, HandleValueConfirmationStatus confirmationStatus) {

            handler.handleValueConfirmationEvent(connectionType, remoteDeviceAddress, this, transactionID, confirmationStatus);
        }

    }

    /**
     * Descriptor Class extends Attribute
     */
    public static class Descriptor extends Attribute {

        /*pkg*/ WeakReference<Characteristic> characteristicRef;

        public Descriptor(UUID uuid, EnumSet<DescriptorProperty> descriptorProperties, EnumSet<SecurityProperty> securityProperties) {

            super(uuid, DescriptorProperty.maskFromSet(descriptorProperties), SecurityProperty.maskFromSet(securityProperties), 1, 0);
        }

        public Descriptor(UUID uuid, long properties, long securityProperties) {

            super(uuid, properties, securityProperties, 1, 0);
        }

        /**
         * @return The parent {@link Characteristic} for this {@link Descriptor}.
         */
        public Characteristic getCharacteristic() {

            if(characteristicRef != null) {
                return characteristicRef.get();
            } else {
                throw new IllegalStateException("Descriptor must be bound to a Characteristic");
            }
        }

        /*pkg*/ synchronized void setCharacteristic(Characteristic characteristic) {

            if(characteristic == null) {
                characteristicRef = null;
            } else {
                if(characteristicRef == null) {
                    characteristicRef = new WeakReference<Characteristic>(characteristic);
                } else {
                    if(characteristicRef.get() != characteristic) {
                        throw new IllegalStateException("Cannot bind a Descriptor to multiple Characteristics");
                    }
                }
            }
        }

        /**
         * Register {@link Descriptor} with Bluetopia.
         *
         * @return the Platform Manager return code from
         *         <code>GATM_AddServiceDescriptor()</code>. Zero indicates
         *         success. A negative number indicates an error has occurred.
         *         See BTPMERR.h and GATMAPI.h
         */
        @Override
        /*pkg*/ int register(int baseOffset) {

            int result = super.register(baseOffset);

            if(result == 0) {
                Service service = getService();

                result = service.getManager().addServiceDescriptorNative(service.getServiceID(), baseOffset, properties, securityProperties, uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
            }

            return result;
        }

        @Override
        /*pkg*/ void issueWriteRequestEvent(ServerEventCallback handler, ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int requestID, byte[] value) {

            handler.writeDescriptorRequestEvent(connectionType, remoteDeviceAddress, this, requestID, value);
        }

        @Override
        /*pkg*/ void issueSignedWriteEvent(ServerEventCallback handler, ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, boolean validSignature, byte[] value) {

            handler.signedWriteDescriptorEvent(connectionType, remoteDeviceAddress, this, validSignature, value);
        }

        @Override
        /*pkg*/ void issueReadRequestEvent(ServerEventCallback handler, ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int requestID, int valueOffset) {

            handler.readDescriptorRequestEvent(connectionType, remoteDeviceAddress, this, requestID, valueOffset);
        }

        @Override
        /*pkg*/ void issuePrepareWriteRequestEvent(ServerEventCallback handler, ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int requestID, int valueOffset, byte[] value) {

            handler.prepareDescriptorWriteRequestEvent(connectionType, remoteDeviceAddress, this, requestID, valueOffset, value);
        }
    }

    /**
     * Included Service Class
     */
    /*pkg*/ static class ServiceInclude extends Attribute {

        private final Service includedService;

        /*pkg*/ ServiceInclude(Service service) {

            super(includeDeclarationUUID, 0, 0, 1, 0);

            if(service == null)
                throw new NullPointerException();

            includedService = service;
        }

        /**
         * @return The included service.
         */
        public Service getIncludedService() {

            return includedService;
        }

        /**
         * Register {@link ServiceInclude} with Bluetopia.
         *
         * @return the Platform Manager return code from
         *         <code>GATM_AddServiceInclude()</code>. Zero indicates
         *         success. A negative number indicates an error has occurred.
         *         See BTPMERR.h and GATMAPI.h
         */
        @Override
        /*pkg*/ int register(int baseOffset) {

            int result = super.register(baseOffset);

            if(result == 0) {
                Service service = getService();

                result = service.getManager().addServiceIncludeNative(0, service.getServiceID(), baseOffset, includedService.getServiceID());
            }

            return result;
        }

        private static final UUID includeDeclarationUUID = UUID.fromString("00002802-0000-1000-8000-00805F9B34FB");
    }

    private final static int REQUEST_ERROR_TYPE_ERROR_RESPONSE              = 1;
    private final static int REQUEST_ERROR_TYPE_PROTOCOL_TIMEOUT            = 2;
    private final static int REQUEST_ERROR_TYPE_PREPARE_WRITE_DATA_MISMATCH = 3;

    /**
     * Enumerates possible errors that can result from a read or write request.
     */
    public enum RequestErrorType {
        /**
         * An error response has been received.
         */
        ERROR_RESPONSE,
        /**
         * A protocol timeout error occurred.
         */
        PROTOCOL_TIMEOUT,
        /**
         * An error occurred while preparing the write data.
         */
        PREPARE_WRITE_DATA_MISMATCH
    }

    /**
     * Platform Manager GATT Error Code - NOT SUPPORTED
     */
    public final static int BTPM_ERROR_CODE_GENERIC_ATTRIBUTE_NOT_SUPPORTED          = -10801;
    /**
     * Platform Manager GATT Error Code - INVALID CONNECTION
     */
    public final static int BTPM_ERROR_CODE_GENERIC_INVALID_CONNECTION               = -10803;
    /**
     * Platform Manager GATT Error Code - INVALID OPERATION
     */
    public final static int BTPM_ERROR_CODE_GENERIC_INVALID_OPERATION                = -10805;
    /**
     * Platform Manager GATT Error Code - INVALID OPERATION
     */
    public final static int BTPM_ERROR_CODE_GENERIC_INVALID_ATTRIBUTE_OFFSET         = -10809;

    /**
     * ATT Protocol Error Code - Success (No Error)
     */
    private final static int ATT_PROTOCOL_ERROR_CODE_SUCCESS                          = 0;
    /**
     * ATT Protocol Error Code - INVALID_HANDLE
     */
    private final static int ATT_PROTOCOL_ERROR_CODE_INVALID_HANDLE                   = 0x01;
    /**
     * ATT Protocol Error Code -
     */
    private final static int ATT_PROTOCOL_ERROR_CODE_READ_NOT_PERMITTED               = 0x02;
    /**
     * ATT Protocol Error Code - WRITE NOT PERMITTED
     */
    private final static int ATT_PROTOCOL_ERROR_CODE_WRITE_NOT_PERMITTED              = 0x03;
    /**
     * ATT Protocol Error Code - INVALID PDU
     */
    private final static int ATT_PROTOCOL_ERROR_CODE_INVALID_PDU                      = 0x04;
    /**
     * ATT Protocol Error Code - INSUFFICIENT AUTHENTICATION
     */
    private final static int ATT_PROTOCOL_ERROR_CODE_INSUFFICIENT_AUTHENTICATION      = 0x05;
    /**
     * ATT Protocol Error Code - REQUEST NOT SUPPORTED
     */
    private final static int ATT_PROTOCOL_ERROR_CODE_REQUEST_NOT_SUPPORTED            = 0x06;
    /**
     * ATT Protocol Error Code - INVALID OFFSET
     */
    private final static int ATT_PROTOCOL_ERROR_CODE_INVALID_OFFSET                   = 0x07;
    /**
     * ATT Protocol Error Code - INSUFFICIENT AUTHORIZATION
     */
    private final static int ATT_PROTOCOL_ERROR_CODE_INSUFFICIENT_AUTHORIZATION       = 0x08;
    /**
     * ATT Protocol Error Code - PREPARE QUEUE FULL
     */
    private final static int ATT_PROTOCOL_ERROR_CODE_PREPARE_QUEUE_FULL               = 0x09;
    /**
     * ATT Protocol Error Code - ATTRIBUTE NOT FOUND
     */
    private final static int ATT_PROTOCOL_ERROR_CODE_ATTRIBUTE_NOT_FOUND              = 0x0A;
    /**
     * ATT Protocol Error Code - ATTRIBUTE NOT LONG
     */
    private final static int ATT_PROTOCOL_ERROR_CODE_ATTRIBUTE_NOT_LONG               = 0x0B;
    /**
     * ATT Protocol Error Code - INSUFFICIENT ENCRYPTION KEY SIZE
     */
    private final static int ATT_PROTOCOL_ERROR_CODE_INSUFFICIENT_ENCRYPTION_KEY_SIZE = 0x0C;
    /**
     * ATT Protocol Error Code - INVALID ATTRIBUTE VALUE LENGTH
     */
    private final static int ATT_PROTOCOL_ERROR_CODE_INVALID_ATTRIBUTE_VALUE_LENGTH   = 0x0D;
    /**
     * ATT Protocol Error Code - UNLIKELY ERROR
     */
    private final static int ATT_PROTOCOL_ERROR_CODE_UNLIKELY_ERROR                   = 0x0E;
    /**
     * ATT Protocol Error Code - INSUFFICIENT ENCRYPTION
     */
    private final static int ATT_PROTOCOL_ERROR_CODE_INSUFFICIENT_ENCRYPTION          = 0x0F;
    /**
     * ATT Protocol Error Code - UNSUPPORTED GROUP TYPE
     */
    private final static int ATT_PROTOCOL_ERROR_CODE_UNSUPPORTED_GROUP_TYPE           = 0x10;
    /**
     * ATT Protocol Error Code - INSUFFICIENT RESOURCES
     */
    private final static int ATT_PROTOCOL_ERROR_CODE_INSUFFICIENT_RESOURCES           = 0x11;


    /**
     * Enumerates the types of errors that can generate an
     * {@link RequestErrorType#ERROR_RESPONSE}
     */
    public enum AttributeProtocolErrorType {
        /**
         * Success (no error)
         */
        SUCCESS(ATT_PROTOCOL_ERROR_CODE_SUCCESS),
        /**
         * Invalid handle.
         */
        INVALID_HANDLE(ATT_PROTOCOL_ERROR_CODE_INVALID_HANDLE),
        /**
         * Read not permitted.
         */
        READ_NOT_PERMITTED(ATT_PROTOCOL_ERROR_CODE_READ_NOT_PERMITTED),
        /**
         * Write not permitted.
         */
        WRITE_NOT_PERMITTED(ATT_PROTOCOL_ERROR_CODE_WRITE_NOT_PERMITTED),
        /**
         * Invalid PDU.
         */
        INVALID_PDU(ATT_PROTOCOL_ERROR_CODE_INVALID_PDU),
        /**
         * Insufficient authentication.
         */
        INSUFFICIENT_AUTHENTICATION(ATT_PROTOCOL_ERROR_CODE_INSUFFICIENT_AUTHENTICATION),
        /**
         * Request not supported.
         */
        REQUEST_NOT_SUPPORTED(ATT_PROTOCOL_ERROR_CODE_REQUEST_NOT_SUPPORTED),
        /**
         * Invalid offset.
         */
        INVALID_OFFSET(ATT_PROTOCOL_ERROR_CODE_INVALID_OFFSET),
        /**
         * Insufficient authorization.
         */
        INSUFFICIENT_AUTHORIZATION(ATT_PROTOCOL_ERROR_CODE_INSUFFICIENT_AUTHORIZATION),
        /**
         * Prepare queue full.
         */
        PREPARE_QUEUE_FULL(ATT_PROTOCOL_ERROR_CODE_PREPARE_QUEUE_FULL),
        /**
         * Attribute not found.
         */
        ATTRIBUTE_NOT_FOUND(ATT_PROTOCOL_ERROR_CODE_ATTRIBUTE_NOT_FOUND),
        /**
         * Attribute not long.
         */
        ATTRIBUTE_NOT_LONG(ATT_PROTOCOL_ERROR_CODE_ATTRIBUTE_NOT_LONG),
        /**
         * Insufficient encryption key size.
         */
        INSUFFICIENT_ENCRYPTION_KEY_SIZE(ATT_PROTOCOL_ERROR_CODE_INSUFFICIENT_ENCRYPTION_KEY_SIZE),
        /**
         * Invalid attribute value length.
         */
        INVALID_ATTRIBUTE_VALUE_LENGTH(ATT_PROTOCOL_ERROR_CODE_INVALID_ATTRIBUTE_VALUE_LENGTH),
        /**
         * Unlikely error.
         */
        UNLIKELY_ERROR(ATT_PROTOCOL_ERROR_CODE_UNLIKELY_ERROR),
        /**
         * Insufficient encryption.
         */
        INSUFFICIENT_ENCRYPTION(ATT_PROTOCOL_ERROR_CODE_INSUFFICIENT_ENCRYPTION),
        /**
         * Unsupported group type,
         */
        UNSUPPORTED_GROUP_TYPE(ATT_PROTOCOL_ERROR_CODE_UNSUPPORTED_GROUP_TYPE),
        /**
         * Insufficient resources.
         */
        INSUFFICIENT_RESOURCES(ATT_PROTOCOL_ERROR_CODE_INSUFFICIENT_RESOURCES)
        ;

        /*pkg*/ int rawCode;

        private AttributeProtocolErrorType(int rawCode) {
            this.rawCode = rawCode;
        }
    }

    private final static int CONNECTION_TYPE_LOW_ENERGY                    = 1;
    private final static int CONNECTION_TYPE_BASIC_RATE_ENHANCED_DATA_RATE = 2;

    /**
     * Types of Connections
     */
    public enum ConnectionType {

        /**
         * A Bluetooth LE connection.
         */
        LOW_ENERGY,

        /**
         * A Bluetooth BR/EDR connection.
         */
        BASIC_RATE_ENHANCED_DATA_RATE
    }

    private final static int CHARACTERISTIC_PROPERTY_BROADCAST                   = 0x00000001;
    private final static int CHARACTERISTIC_PROPERTY_READ                        = 0x00000002;
    private final static int CHARACTERISTIC_PROPERTY_WRITE_WITHOUT_RESPONSE      = 0x00000004;
    private final static int CHARACTERISTIC_PROPERTY_WRITE                       = 0x00000008;
    private final static int CHARACTERISTIC_PROPERTY_NOTIFY                      = 0x00000010;
    private final static int CHARACTERISTIC_PROPERTY_INDICATE                    = 0x00000020;
    private final static int CHARACTERISTIC_PROPERTY_AUTHENTICATED_SIGNED_WRITES = 0x00000040;
    private final static int CHARACTERISTIC_PROPERTY_EXTENDED_PROPERTIES         = 0x00000080;

    /**
     * Bluetooth LE Characteristic Properties
     */
    public enum CharacteristicProperty {
        /**
         * Broadcast.
         */
        BROADCAST(CHARACTERISTIC_PROPERTY_BROADCAST),
        /**
         * Read.
         */
        READ(CHARACTERISTIC_PROPERTY_READ),
        /**
         * Write without response.
         */
        WRITE_WITHOUT_RESPONSE(CHARACTERISTIC_PROPERTY_WRITE_WITHOUT_RESPONSE),
        /**
         * Write.
         */
        WRITE(CHARACTERISTIC_PROPERTY_WRITE),
        /**
         * Notify.
         */
        NOTIFY(CHARACTERISTIC_PROPERTY_NOTIFY),
        /**
         * Indicate.
         */
        INDICATE(CHARACTERISTIC_PROPERTY_INDICATE),
        /**
         * Authenticated signed writes.
         */
        AUTHENTICATED_SIGNED_WRITES(CHARACTERISTIC_PROPERTY_AUTHENTICATED_SIGNED_WRITES),
        /**
         * Extended properties.
         */
        EXTENDED_PROPERTIES(CHARACTERISTIC_PROPERTY_EXTENDED_PROPERTIES)
        ;

        /**
         * Mask associated with enum value in the constructor
         */
        public int propertyMask;

        CharacteristicProperty(int propertyMask) {

            this.propertyMask = propertyMask;
        }

        /*pkg*/ static int maskFromSet(EnumSet<CharacteristicProperty> charPropSet) {

            int mask = 0;

            if(charPropSet != null) {
                for(CharacteristicProperty charProp : charPropSet)
                    mask |= charProp.propertyMask;
            }

            return mask;
        }
    }

    private final static int SECURITY_PROPERTIES_NO_SECURITY                      = 0x00000000;
    private final static int SECURITY_PROPERTIES_UNAUTHENTICATED_ENCRYPTION_WRITE = 0x00000001;
    private final static int SECURITY_PROPERTIES_AUTHENTICATED_ENCRYPTION_WRITE   = 0x00000002;
    private final static int SECURITY_PROPERTIES_UNAUTHENTICATED_ENCRYPTION_READ  = 0x00000004;
    private final static int SECURITY_PROPERTIES_AUTHENTICATED_ENCRYPTION_READ    = 0x00000008;
    private final static int SECURITY_PROPERTIES_UNAUTHENTICATED_SIGNED_WRITES    = 0x00000010;
    private final static int SECURITY_PROPERTIES_AUTHENTICATED_SIGNED_WRITES      = 0x00000020;

    /**
     * Security Properties
     */
    public enum SecurityProperty {
        /**
         * No security.
         */
        NO_SECURITY(SECURITY_PROPERTIES_NO_SECURITY),
        /**
         * Unauthenticated encryption write.
         */
        UNAUTHENTICATED_ENCRYPTION_WRITE(SECURITY_PROPERTIES_UNAUTHENTICATED_ENCRYPTION_WRITE),
        /**
         * Authenticated encryption write.
         */
        AUTHENTICATED_ENCRYPTION_WRITE(SECURITY_PROPERTIES_AUTHENTICATED_ENCRYPTION_WRITE),
        /**
         * Unauthenticated encryption read.
         */
        UNAUTHENTICATED_ENCRYPTION_READ(SECURITY_PROPERTIES_UNAUTHENTICATED_ENCRYPTION_READ),
        /**
         * Authenticated encryption read.
         */
        AUTHENTICATED_ENCRYPTION_READ(SECURITY_PROPERTIES_AUTHENTICATED_ENCRYPTION_READ),
        /**
         * Unauthenticated(no MITM protection) signed writes - only valid with un-encrypted connections
         */
        UNAUTHENTICATED_SIGNED_WRITES(SECURITY_PROPERTIES_UNAUTHENTICATED_SIGNED_WRITES),
        /**
         * Authenticated(no MITM protection) signed writes - only valid with un-encrypted connections
         */
        AUTHENTICATED_SIGNED_WRITES(SECURITY_PROPERTIES_AUTHENTICATED_SIGNED_WRITES)
        ;

        /**
         * Mask associated with enum value in the constructor
         */
        public int propertyMask;

        SecurityProperty(int propertyMask) {

            this.propertyMask = propertyMask;
        }

        /*pkg*/ static int maskFromSet(EnumSet<SecurityProperty> secPropSet) {

            int mask = 0;

            if(secPropSet != null) {
                for(SecurityProperty secProp : secPropSet)
                    mask |= secProp.propertyMask;
            }

            return mask;
        }
    }

    private final static int DESCRIPTOR_PROPERTY_READ  = 0x00000001;
    private final static int DESCRIPTOR_PROPERTY_WRITE = 0x00000002;

    /**
     * Descriptor Properties
     */
    public enum DescriptorProperty {
        /**
         * Read.
         */
        READ(DESCRIPTOR_PROPERTY_READ),
        /**
         * Write.
         */
        WRITE(DESCRIPTOR_PROPERTY_WRITE)
        ;

        /**
         * Mask associated with enum value in the constructor
         */
        public int propertyMask;

        DescriptorProperty(int propertyMask) {

            this.propertyMask = propertyMask;
        }

        /*pkg*/ static int maskFromSet(EnumSet<DescriptorProperty> descPropSet) {

            int mask = 0;

            if(descPropSet != null) {
                for(DescriptorProperty desc : descPropSet)
                    mask |= desc.propertyMask;
            }

            return mask;
        }
    }

    private final static int HANDLE_VALUE_CONFIRMATION_STATUS_SUCCESS = 0x00000000;
    private final static int HANDLE_VALUE_CONFIRMATION_STATUS_TIMEOUT = 0x00000001;

    /**
     * Result of a Notification with Response on a {@link Characteristic} value.
     */
    public enum HandleValueConfirmationStatus {
        /**
         * Success
         */
        SUCCESS,
        /**
         * Timeout
         */
        TIMEOUT
    }

    private final static int SERVICE_FLAGS_SUPPORT_LOW_ENERGY        = 0x00000001;
    private final static int SERVICE_FLAGS_SUPPORT_CLASSIC_BLUETOOTH = 0x00000002;

    /**
     * Service Flags
     */
    public enum ServiceFlag {
        /**
         * Bluetooth Low Energy.
         */
        LOW_ENERGY(SERVICE_FLAGS_SUPPORT_LOW_ENERGY),
        /**
         * Bluetooth Classic.
         */
        CLASSIC_BLUETOOTH(SERVICE_FLAGS_SUPPORT_CLASSIC_BLUETOOTH)
        ;

        /**
         * Mask associated with enum value in the constructor
         */
        public int flagMask;

        ServiceFlag(int flagMask) {

            this.flagMask = flagMask;
        }

        /*pkg*/ static int maskFromSet(EnumSet<ServiceFlag> flagSet) {

            int mask = 0;

            if(flagSet != null) {
                for(ServiceFlag flag : flagSet)
                    mask |= flag.flagMask;
            }

            return mask;
        }
    }

    /**
     * Details of a GATT service.
     */
    public static class ServiceInformation {
        /**
         * Service UUID.
         */
        public final UUID uuid;
        /**
         * Start handle.
         */
        public final int  startHandle;
        /**
         * End handle.
         */
        public final int  endHandle;

        /**
         * Service ID
         */
        public final int  serviceID;

        /**
         * ServiceInformation constructor.
         *
         * @param uuid
         *            The service UUID.
         * @param startHandle
         *            The service start handle.
         * @param endHandle
         *            The service end handle.
         */
        /*pkg*/ ServiceInformation(UUID uuid, int startHandle, int endHandle) {
            this.uuid = uuid;
            this.startHandle = startHandle;
            this.endHandle   = endHandle;
            this.serviceID   = 0;
        }
        /*pkg*/ ServiceInformation(long mostSignificantBits, long leastSignificantBits, int startHandle, int endHandle, int serviceID) {
            this.uuid        = new UUID(mostSignificantBits, leastSignificantBits);
            this.startHandle = startHandle;
            this.endHandle   = endHandle;
            this.serviceID   = serviceID;
        }
        /*pkg*/ ServiceInformation(int startHandle, int endHandle, int serviceID) {
            this.uuid        = null;
            this.startHandle = startHandle;
            this.endHandle   = endHandle;
            this.serviceID   = serviceID;
        }
    }

    /**
     * Definition of a GATT service.
     */
    public static final class ServiceDefinition {
        /**
         * An instance of the ServiceInformation class which includedServices UUID and the starting and ending handles.
         */
        public final ServiceInformation         serviceDetails;
        /**
         * Array of service includedServices
         */
        public final ServiceInformation[]       includedServices;
        /**
         * Array of characteristic definitions
         */
        public final CharacteristicDefinition[] characteristics;

        /*pkg*/ ServiceDefinition(ServiceInformation serviceDetails, ServiceInformation[] includedServices, CharacteristicDefinition[] characteristics) {
            this.serviceDetails   = serviceDetails;
            this.includedServices = includedServices;
            this.characteristics  = characteristics;
        }
    }

    /**
     * Definition of a Characteristic of a GATT Service.
     */
    public static final class CharacteristicDefinition {
        /**
         * UUID value of the characteristic
         */
        public final UUID                            uuid;
        /**
         * Identifying value from the GATT server
         */
        public final int                             handle;
        /**
         * Characteristic Properties - @see {@link CharacteristicProperty}
         */
        public final EnumSet<CharacteristicProperty> properties;
        /**
         * Array of descriptors
         */
        public final CharacteristicDescriptor[]      descriptors;

        /*pkg*/ CharacteristicDefinition(UUID uuid, int handle, EnumSet<CharacteristicProperty> properties, CharacteristicDescriptor[] descriptors) {
            this.uuid        = uuid;
            this.handle      = handle;
            this.properties  = properties;
            this.descriptors = descriptors;
        }

        /*pkg*/ CharacteristicDefinition(UUID uuid, int handle, int properties, CharacteristicDescriptor[] descriptors) {
            EnumSet<CharacteristicProperty> propSet = EnumSet.noneOf(CharacteristicProperty.class);

            if((properties & CHARACTERISTIC_PROPERTY_BROADCAST) == CHARACTERISTIC_PROPERTY_BROADCAST)
                propSet.add(CharacteristicProperty.BROADCAST);
            if((properties & CHARACTERISTIC_PROPERTY_READ) == CHARACTERISTIC_PROPERTY_READ)
                propSet.add(CharacteristicProperty.READ);
            if((properties & CHARACTERISTIC_PROPERTY_WRITE_WITHOUT_RESPONSE) == CHARACTERISTIC_PROPERTY_WRITE_WITHOUT_RESPONSE)
                propSet.add(CharacteristicProperty.WRITE_WITHOUT_RESPONSE);
            if((properties & CHARACTERISTIC_PROPERTY_WRITE) == CHARACTERISTIC_PROPERTY_WRITE)
                propSet.add(CharacteristicProperty.WRITE);
            if((properties & CHARACTERISTIC_PROPERTY_NOTIFY) == CHARACTERISTIC_PROPERTY_NOTIFY)
                propSet.add(CharacteristicProperty.NOTIFY);
            if((properties & CHARACTERISTIC_PROPERTY_INDICATE) == CHARACTERISTIC_PROPERTY_INDICATE)
                propSet.add(CharacteristicProperty.INDICATE);
            if((properties & CHARACTERISTIC_PROPERTY_AUTHENTICATED_SIGNED_WRITES) == CHARACTERISTIC_PROPERTY_AUTHENTICATED_SIGNED_WRITES)
                propSet.add(CharacteristicProperty.AUTHENTICATED_SIGNED_WRITES);
            if((properties & CHARACTERISTIC_PROPERTY_EXTENDED_PROPERTIES) == CHARACTERISTIC_PROPERTY_EXTENDED_PROPERTIES)
                propSet.add(CharacteristicProperty.EXTENDED_PROPERTIES);

            this.uuid        = uuid;
            this.handle      = handle;
            this.properties  = propSet;
            this.descriptors = descriptors;
        }
    }

    /**
     * Details of a Characteristic Descriptor.
     */
    public static final class CharacteristicDescriptor {
        /**
         * UUID
         */
        public final UUID uuid;
        /**
         * Attribute Handle
         */
        public final int  handle;

        /*pkg*/ CharacteristicDescriptor(UUID uuid, int handle) {
            this.uuid   = uuid;
            this.handle = handle;
        }
    }

    /**
    * Connection information Class
    */
    public static final class ConnectionInformation {

        /**
        * Type of Connection
        */
        public ConnectionType   connectionType;
        /**
        * The Bluetooth address of the remote device
        */
        public BluetoothAddress remoteDeviceAddress;

        /**
         * ConnectionInformation constructor.
         *
         * @param connectionType
         *            The type of connection.
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device
         */
        public ConnectionInformation(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress) {

            this.connectionType      = connectionType;
            this.remoteDeviceAddress = remoteDeviceAddress;
        }

        /**
         * Override
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object connectionInformation) {

            boolean returnValue;

            if(connectionInformation instanceof ConnectionInformation) {

                if((Arrays.equals(remoteDeviceAddress.internalByteArray(), ((ConnectionInformation)connectionInformation).remoteDeviceAddress.internalByteArray())) && (connectionType == ((ConnectionInformation)connectionInformation).connectionType))
                    returnValue = true;
                else
                    returnValue = false;
            } else
                returnValue = false;

            return returnValue;
        }
    }

    private native static void initClassNative();

    private native void initObjectNative() throws ServerNotReachableException;

    private native void cleanupObjectNative();

    private native int queryConnectedDevicesNative(int[][] connectionTypeConstants, byte[][] remoteDeviceAddresses);

    protected native int readValueNative(byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, int attributeHandle, int offset, boolean readAll);

    protected native int writeValueNative(byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, int attributeHandle, byte[] data);

    protected native int writeValueWithoutResponseNative(byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, int attributeHandle, byte[] data);

    /**
     * @param addr1
     * @param addr2
     * @param addr3
     * @param addr4
     * @param addr5
     * @param addr6
     * @param nativeParameters
     *            Object array, of length 11, which will be populated with
     *            arrays:
     *            <ul>
     *            <li>long[][]   serviceUUIDsMajor</li>
     *            <li>long[][]   serviceUUIDsMinor</li>
     *            <li>int[][]    startHandles</li>
     *            <li>int[][]    endHandles</li>
     *            <li>long[][]   characteristicUUIDsMajor</li>
     *            <li>long[][]   characteristicUUIDsMinor</li>
     *            <li>int[][]    characteristicHandles</li>
     *            <li>int[][]    characteristicProperties</li>
     *            <li>long[][][] descriptorUUIDsMajor</li>
     *            <li>long[][][] descriptorUUIDsMinor</li>
     *            <li>int[][]    descriptorhandles</li>
     *            </ul>
     *            Such that the first index of each array refers to the service
     *            definition. For each service:
     *            <ul>
     *            <li>Element 0 of the serviceUUIDs*, startHandles, and
     *            endHandles arrays represents the primary service data. Each
     *            additional element maps to an included (secondary) service.</li>
     *            <li>Each element of the characteristic* arrays maps to a
     *            single characteristic definition.</li>
     *            <li>The second index of each descriptor* array refers to the
     *            associated characteristic, while the third index maps to a
     *            single descriptor definition.</li>
     *            </ul>
     * @return
     */

    protected native int queryRemoteDeviceServicesNative(byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, Object[] nativeParameters);

    protected native int registerPersistentUIDNative(int numberOfAttributes, int[] handleData);

    protected native int unRegisterPersistentUIDNative(int reservedHandleRangeUID);

    protected native int registerServiceNative(boolean primaryService, int numberOfAttributes, long mostSignificantBits, long leastSignificantBits, int reservedHandleRangeUID);

    protected native int addServiceIncludeNative(long flags, int serviceID, int attributeOffset, int includedServiceID);

    protected native int addServiceCharacteristicNative(int serviceID, int attributeOffset, long characteristicPropertiesMask, long securityPropertiesMask, long mostSignificantBits, long leastSignificantBits);

    protected native int addServiceDescriptorNative(int serviceID, int attributeOffset, long descriptorPropertiesMask, long securityPropertiesMask, long mostSignificantBits, long leastSignificantBits);

    protected native int addServiceAttributeDataNative(int serviceID, int attributeOffset, byte[] value);

    protected native int publishServiceNative(int serviceID, long serviceFlags, int[] handleRange);

    protected native int deleteServiceNative(int serviceID);

    protected native ServiceInformation[] queryPublishedServicesNative();

    protected native ServiceInformation[] queryPublishedServicesNative(long mostSignificantBits, long leastSignificantBits);

    protected native int sendHandleValueIndicationNative(byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, int serviceID, int attributeOffset, byte[] valueData);

    protected native int sendHandleValueNotificationNative(byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, int serviceID, int attributeOffset, byte[] valueData);

    protected native int writeResponseNative(int requestID);

    protected native int readResponseNative(int requestID, byte[] data);

    protected native int errorResponseNative(int requestID, int error);
}
