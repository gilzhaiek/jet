/*
 * MAPM.java Copyright 2010 - 2014 Stonestreet One, LLC. All Rights Reserved.
 */
package com.stonestreetone.bluetopiapm;

import java.util.Calendar;
import java.util.EnumSet;

/**
 * Java wrapper for Message Access Profile Manager API for Stonestreet One
 * Bluetooth Protocol Stack Platform Manager.
 */
public abstract class MAPM {

    protected final EventCallback callbackHandler;

    private boolean               disposed;

    private long                  localData;

    static {
        System.loadLibrary("btpmj");
        initClassNative();
    }

    private final static int      MANAGER_TYPE_CLIENT = 1;
    private final static int      MANAGER_TYPE_SERVER = 2;

    protected final int           managerType;

    /**
     * Private Constructor.
     *
     * @throws ServerNotReachableException
     */
    protected MAPM(int type, EventCallback eventCallback) throws ServerNotReachableException {

        try {

            initObjectNative();
            disposed         = false;
            callbackHandler  = eventCallback;
            this.managerType = type;

        } catch(ServerNotReachableException exception) {

            dispose();
            throw exception;
        }
    }

    /**
     * Disposes of all resources allocated by this Manager object.
     */
    public void dispose() {
        // FIXME Close remaining connections
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
     * Close a connection.
     * <p>
     * If the connection was established on a local server port, this function
     * disconnects any remote devices but leaves the server registered.
     *
     * @param connectionCategory
     *            Identifies whether the connection is a message access
     *            connection or a notification connection.
     * @param remoteDeviceAddress
     *            The Bluetooth address of the remote device.
     * @param instanceID
     *            The instance ID; applies only to message access connections.
     *
     * @return Zero if successful, or a negative code if there was an error.
     */
    public int disconnect(ConnectionCategory connectionCategory, BluetoothAddress remoteDeviceAddress, int instanceID) {
        byte[] address;
        int connectionTypeConstant;

        if(remoteDeviceAddress != null)
            address = remoteDeviceAddress.internalByteArray();
        else
            return -1;

        switch(connectionCategory) {
        case NOTIFICATION:
            if(this.managerType == MANAGER_TYPE_CLIENT)
                connectionTypeConstant = CONNECTION_TYPE_NOTIFICATION_SERVER;
            else
                connectionTypeConstant = CONNECTION_TYPE_NOTIFICATION_CLIENT;
            break;
        case MESSAGE_ACCESS:
            if(this.managerType == MANAGER_TYPE_CLIENT)
                connectionTypeConstant = CONNECTION_TYPE_MESSAGE_ACCESS_CLIENT;
            else
                connectionTypeConstant = CONNECTION_TYPE_MESSAGE_ACCESS_SERVER;
            break;
        default:
            connectionTypeConstant = 0;
            break;
        }

        return disconnectNative(connectionTypeConstant, address[0], address[1], address[2], address[3], address[4], address[5], instanceID);
    }

    /**
     * Abort a MAPM transaction.
     * <p>
     * <i>Note:</i> there can only be one outstanding MAPM request at a time.
     * Additional MAPM requests cannot be issued until the current request is
     * aborted or completed.
     *
     * @param connectionCategory
     *            Identifies whether the connection is a message access
     *            connection or a notification connection.
     * @param remoteDeviceAddress
     *            The Bluetooth address of the remote device
     * @param instanceID
     *            The instance ID; applies only to message access connections.
     *
     * @return Zero if successful, or a negative return error code if there was
     *         an error.
     */
    public int abort(ConnectionCategory connectionCategory, BluetoothAddress remoteDeviceAddress, int instanceID) {
        byte[] address;
        int connectionTypeConstant;

        switch(connectionCategory) {
        case NOTIFICATION:
            if(this.managerType == MANAGER_TYPE_CLIENT)
                connectionTypeConstant = CONNECTION_TYPE_NOTIFICATION_CLIENT;
            else
                connectionTypeConstant = CONNECTION_TYPE_NOTIFICATION_SERVER;
            break;
        case MESSAGE_ACCESS:
            if(this.managerType == MANAGER_TYPE_CLIENT)
                connectionTypeConstant = CONNECTION_TYPE_MESSAGE_ACCESS_SERVER;
            else
                connectionTypeConstant = CONNECTION_TYPE_MESSAGE_ACCESS_CLIENT;
            break;
        default:
            connectionTypeConstant = 0;
            break;
        }

        if(remoteDeviceAddress != null)
            address = remoteDeviceAddress.internalByteArray();
        else
            return -1;

        return abortNative(connectionTypeConstant, address[0], address[1], address[2], address[3], address[4], address[5], instanceID);
    }

    /**
     * Query the current working directory of the message access server.
     *
     * @param remoteDeviceAddress
     *            The Bluetooth address of the remote device.
     * @param instanceID
     *            The message access server instance ID.
     *
     * @return The current working directory of the message access server. (A
     *         return of NULL indicates the root directory.)
     * @throws BluetopiaPMException
     *
     */
    public String queryCurrentFolder(BluetoothAddress remoteDeviceAddress, int instanceID) throws BluetopiaPMException {
        byte[] address;

        address = remoteDeviceAddress.internalByteArray();

        return queryCurrentFolderNative(address[0], address[1], address[2], address[3], address[4], address[5], instanceID);
    }

    protected void disconnected(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID) {

        ConnectionType connectionType;
        EventCallback  callbackHandler = this.callbackHandler;

        switch(connectionTypeConstant) {
        case CONNECTION_TYPE_NOTIFICATION_SERVER:
            connectionType = ConnectionType.NOTIFICATION_SERVER;
            break;
        case CONNECTION_TYPE_NOTIFICATION_CLIENT:
            connectionType = ConnectionType.NOTIFICATION_CLIENT;
            break;
        case CONNECTION_TYPE_MESSAGE_ACCESS_SERVER:
            connectionType = ConnectionType.MESSAGE_ACCESS_SERVER;
            break;
        case CONNECTION_TYPE_MESSAGE_ACCESS_CLIENT:
            connectionType = ConnectionType.MESSAGE_ACCESS_CLIENT;
            break;
        default:
            connectionType = null;
            break;
        }

        if(callbackHandler != null) {
            try {
                callbackHandler.disconnectedEvent(connectionType, new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), instanceID);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

    }

    protected void connectionStatus(int connectionTypeConstant, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int connectionStatusConstant) {

        ConnectionType   connectionType;
        ConnectionStatus connectionStatus;
        EventCallback    callbackHandler = this.callbackHandler;

        switch(connectionTypeConstant) {
        case CONNECTION_TYPE_NOTIFICATION_SERVER:
            connectionType = ConnectionType.NOTIFICATION_SERVER;
            break;
        case CONNECTION_TYPE_NOTIFICATION_CLIENT:
            connectionType = ConnectionType.NOTIFICATION_CLIENT;
            break;
        case CONNECTION_TYPE_MESSAGE_ACCESS_SERVER:
            connectionType = ConnectionType.MESSAGE_ACCESS_SERVER;
            break;
        case CONNECTION_TYPE_MESSAGE_ACCESS_CLIENT:
            connectionType = ConnectionType.MESSAGE_ACCESS_CLIENT;
            break;
        default:
            connectionType = null;
            break;
        }

        switch(connectionStatusConstant) {
        case CONNECTION_STATUS_SUCCESS:
            connectionStatus = ConnectionStatus.SUCCESS;
            break;
        case CONNECTION_STATUS_FAILURE_TIMEOUT:
            connectionStatus = ConnectionStatus.FAILURE_TIMEOUT;
            break;
        case CONNECTION_STATUS_FAILURE_REFUSED:
            connectionStatus = ConnectionStatus.FAILURE_REFUSED;
            break;
        case CONNECTION_STATUS_FAILURE_SECURITY:
            connectionStatus = ConnectionStatus.FAILURE_SECURITY;
            break;
        case CONNECTION_STATUS_FAILURE_DEVICE_POWER_OFF:
            connectionStatus = ConnectionStatus.FAILURE_DEVICE_POWER_OFF;
            break;
        case CONNECTION_STATUS_FAILURE_UNKNOWN:
            connectionStatus = ConnectionStatus.FAILURE_UNKNOWN;
            break;
        default:
            connectionStatus = ConnectionStatus.FAILURE_UNKNOWN;
            break;
        }

        if(callbackHandler != null) {
            try {
                callbackHandler.connectionStatusEvent(connectionType, new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), instanceID, connectionStatus);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

    }

    protected void enableNotificationsResponse(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int responseStatusCodeConstant) {
        throw new UnsupportedOperationException();
    }

    protected void getFolderListingResponse(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int responseStatusCodeConstant, boolean isFinal, byte[] folderListingBuffer) {
        throw new UnsupportedOperationException();
    }

    protected void getFolderListingSizeResponse(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int responseStatusCodeConstant, int numberOfFolders) {
        throw new UnsupportedOperationException();
    }

    protected void getMessageListingResponse(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int responseStatusCodeConstant, boolean newMessage, int[] dateTime, boolean validUTC, int offsetUTC, boolean isFinal, int numberOfMessages, byte[] messageListingBuffer) {
        throw new UnsupportedOperationException();
    }

    protected void getMessageListingSizeResponse(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int responseStatusCodeConstant, boolean newMessage, int[] dateTime, boolean validUTC, int offsetUTC, int numberOfMessages) {
        throw new UnsupportedOperationException();
    }

    protected void getMessageResponse(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int responseStatusCodeConstant, int fractionDeliverConstant, boolean isFinal, byte[] messageBuffer) {
        throw new UnsupportedOperationException();
    }

    protected void setMessageStatusResponse(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int responseStatusCodeConstant) {
        throw new UnsupportedOperationException();
    }

    protected void pushMessageResponse(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int responseStatusCodeConstant, String messageHandle) {
        throw new UnsupportedOperationException();
    }

    protected void updateInboxResponse(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int responseStatusCodeConstant) {
        throw new UnsupportedOperationException();
    }

    protected void setFolderResponse(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int responseStatusCodeConstant, String currentPath) {
        throw new UnsupportedOperationException();
    }

    protected void notificationIndication(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, boolean isFinal, byte[] eventReportBuffer) {
        throw new UnsupportedOperationException();
    }

    protected void connected(ConnectionType connectionType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID) {
        throw new UnsupportedOperationException();
    }

    protected void incomingConnectionRequest(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID) {
        throw new UnsupportedOperationException();
    }

    protected void enableNotificationsRequest(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, boolean enabled) {
        throw new UnsupportedOperationException();
    }

    protected void notificationConfirmation(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int status) {
        throw new UnsupportedOperationException();
    }

    protected void getFolderListingRequest(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int maxListCount, int listStartOffset) {
        throw new UnsupportedOperationException();
    }

    protected void getFolderListingSizeRequest(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID) {
        throw new UnsupportedOperationException();
    }

    protected void getmessageListingRequest(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, String folderName, int maxListCount, int listStartOffset, ListingInfo listingInfo) {
        throw new UnsupportedOperationException();
    }

    protected void getMessageListingSizeRequest(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, String folderName) {
        throw new UnsupportedOperationException();
    }

    protected void getMessageRequest(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, boolean attachment, CharSet charSet, FractionRequest fractionRequest, String messageHandle) {
        throw new UnsupportedOperationException();
    }

    protected void setMessageStatusRequest(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, String messageHandle, StatusIndicator statusIndicator, boolean statusValue) {
        throw new UnsupportedOperationException();
    }

    protected void pushMessageRequest(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, String folderName, boolean transparent, boolean retry, CharSet charSet, boolean isFinal, byte[] buffer) {
        throw new UnsupportedOperationException();
    }

    protected void updateInboxRequest(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID) {
        throw new UnsupportedOperationException();
    }

    protected void setFolderRequest(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, PathOption pathOption, String folderName) {
        throw new UnsupportedOperationException();
    }

    /**
     * Receiver for event notifications which are common to the message server
     * equipment (MSE) and message client equipment (MCE).
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
         * Invoked when a connection has been closed.
         *
         * @param connectionType
         *            Distinguishes between notification and message access
         *            connections, as well as distinguishing between server and
         *            client.
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @param instanceID
         *            The instance ID; applies only to message access
         *            connections.
         */
        public void disconnectedEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int instanceID);

        /**
         * Delivers a response to a connection request.
         *
         * @param connectionType
         *            Distinguishes between notification and message access
         *            connections, as well as distinguishing between server and
         *            client.
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @param instanceID
         *            The instance ID; applies only to message access
         *            connections.
         * @param connectionStatus
         *            The status of the connection attempt.
         */
        public void connectionStatusEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int instanceID, ConnectionStatus connectionStatus);
    }

    /**
     * Message Client Equipment (MCE) class.
     * <p>
     * Message access client and message notification server roles.
     */
    public static final class MessageAccessClientManager extends MAPM {
        /**
         * Constructor for a message client equipment (MCE) object.
         *
         * @param eventCallback
         *            Receiver for events.
         *
         * @throws ServerNotReachableException
         *             if the BluetopiaPM server cannot be reached.
         */
        public MessageAccessClientManager(ClientEventCallback eventCallback) throws ServerNotReachableException {
            super(MANAGER_TYPE_CLIENT, eventCallback);
        }

        /**
         * Connect to a remote message access server (MAS)
         * <p>
         * If {@code waitForConnection} is {@code true}, this function will block
         * until the attempt is completed. If it is {@code false}, the status of the
         * connection will be returned via {@link EventCallback#connectionStatusEvent}.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @param remoteServerPort
         *            The remote server port.
         * @param instanceID
         *            The message access server instance ID. (<i>Note: this
         *            parameter is not applicable to notification servers.</i>)
         * @param connectionFlags
         *            Flags indicating the encryption and authentication
         *            requirements for the connection.
         * @param waitForConnection
         *            Determines whether the function should block (wait) until the
         *            connection has been established.
         *
         * @return If {@code waitForConnection} is {@code true}, zero indicates
         *         success and a positive value indicates an error.
         *         <p>
         *         If {@code waitForConnection} is {@code false}, zero indicates
         *         success and a negative value indicates an error.
         * @see EventCallback#connectionStatusEvent
         */
        public int connectRemoteDevice(BluetoothAddress remoteDeviceAddress, int remoteServerPort, int instanceID, EnumSet<ConnectionFlags> connectionFlags, boolean waitForConnection) {

            byte[] address;
            int connectionMask;

            address = remoteDeviceAddress.internalByteArray();

            connectionMask = 0;
            connectionMask = (connectionFlags.contains(ConnectionFlags.REQUIRE_AUTHENTICATION) ? CONNECTION_FLAGS_REQUIRE_AUTHENTICATION : 0);
            connectionMask |= (connectionFlags.contains(ConnectionFlags.REQUIRE_ENCRYPTION) ? CONNECTION_FLAGS_REQUIRE_ENCRYPTION : 0);

            return connectRemoteDeviceNative(CONNECTION_CLIENT_MESSAGE_ACCESS, address[0], address[1], address[2], address[3], address[4], address[5], remoteServerPort, instanceID, connectionMask, waitForConnection);

        }

        /**
         * Parse the message access service records of the message server equipment (MSE).
         * <p>
         * The MSE can host more than one message access server(MAS).
         * There will be an entry in the returned array for each
         * message access server. The class contains the service name, the server port,
         * the instance ID and the supported message types.
         *
         * @param remoteDeviceAddress
         * @return Array of the class MessageAccessServices
         * @throws BluetopiaPMException
         *
         */
        public MessageAccessServices[] parseRemoteMessageAccessServices(BluetoothAddress remoteDeviceAddress) throws BluetopiaPMException {

            MessageAccessServices[] messageAccessServices;
            EnumSet<SupportedMessageType> supportedMessageSet;
            String[][] serviceName = new String[1][0];
            int[][] instanceID = new int[1][0];
            int[][] serverPort = new int[1][0];
            long[][] supportedMessageTypes= new long[1][0];

            byte[] address = remoteDeviceAddress.internalByteArray();

             int result = parseRemoteMessageAccessServicesNative(address[0], address[1], address[2], address[3], address[4], address[5], serviceName, instanceID, serverPort, supportedMessageTypes);

             if(result == 0) {

                 messageAccessServices = new MessageAccessServices[instanceID[0].length];
                 supportedMessageSet = EnumSet.noneOf(SupportedMessageType.class);

                 for(int index = 0; index < instanceID[0].length; index++ ) {

                     for(SupportedMessageType supportedMessage : SupportedMessageType.values()) {
                         if(((supportedMessageTypes[0][index]) & (1 << supportedMessage.ordinal())) == (1 << supportedMessage.ordinal()))
                             supportedMessageSet.add(supportedMessage);
                     }

                     messageAccessServices[index] = new MessageAccessServices(serviceName[0][index], instanceID[0][index], serverPort[0][index], supportedMessageSet);
                     supportedMessageSet.clear();
                 }

             }
             else
                 messageAccessServices = null;

             return messageAccessServices;
        }

        /**
         * Enable or disable notifications from the remote message access
         * server.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device
         * @param instanceID
         *            The instance ID of the message access server.
         * @param enable
         *            Indicates whether to enable or disable notifications.
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         *
         * @see ClientEventCallback#enableNotificationsResponseEvent
         */
        public int enableNotifications(BluetoothAddress remoteDeviceAddress, int instanceID, boolean enable) {
            byte[] address = remoteDeviceAddress.internalByteArray();

            return enableNotificationsNative(address[0], address[1], address[2], address[3], address[4], address[5], instanceID, enable);
        }

        /**
         * Set the current working directory.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device
         * @param instanceID
         *            The message access server instance ID.
         * @param pathOption
         *            Determines how the directory structure is navigated. The
         *            options are {@code ROOT}, {@code DOWN} and {@code UP}.
         *            ROOT navigates directly to the root directory. DOWN moves
         *            to a child directory; it is used in conjunction with the
         *            folderName parameter. UP moves to the parent directory. By
         *            using UP with the folderName parameter, the user can set
         *            the current folder to another folder in the same parent
         *            directory, similar to issuing {@code "cd ../folderName"}
         *            at a command prompt.
         * @param folderName
         *            The name of the folder.
         *
         * @see ClientEventCallback#setFolderResponseEvent
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int setFolder(BluetoothAddress remoteDeviceAddress, int instanceID, PathOption pathOption, String folderName) {
            int pathOptionConstant;

            switch(pathOption) {
            case ROOT:
                pathOptionConstant = PATH_OPTION_ROOT;
                break;
            case DOWN:
                pathOptionConstant = PATH_OPTION_DOWN;
                break;
            case UP:
                pathOptionConstant = PATH_OPTION_UP;
                break;
            default:
                pathOptionConstant = 0;
                break;
            }

            byte[] address = remoteDeviceAddress.internalByteArray();

            return setFolderNative(address[0], address[1], address[2], address[3], address[4], address[5], instanceID, pathOptionConstant, folderName);
        }

        /**
         * Set the current working directory using an absolute path.
         * <p>
         * <i>Note:</i> this function will return success if the initial request
         * is sent successfully. If one of the chained requests fails, a
         * {@link ClientEventCallback#setFolderResponseEvent} event will be received
         * containing the current working directory.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @param instanceID
         *            The message access server instance ID.
         * @param absolutePath
         *            An absolute path string (for example,
         *            {@code "/telecom/msg/inbox"}). A forward slash is the
         *            delimiter.
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int setFolderAbsolute(BluetoothAddress remoteDeviceAddress, int instanceID, String absolutePath) {
            byte[] address = remoteDeviceAddress.internalByteArray();

            return setFolderAbsoluteNative(address[0], address[1], address[2], address[3], address[4], address[5], instanceID, absolutePath);
        }

        /**
         * Request a folder listing from the remote message access server.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @param instanceID
         *            The message access server instance ID.
         * @param maxListCount
         *            The maximum number of items to be included in the listing.
         * @param listStartOffset
         *            A value indicating how many folders to skip before the
         *            start of the listing. Note that the Bluetooth
         *            specification does not require the folders be sorted in
         *            any particular manner.
         *
         * @see ClientEventCallback#getFolderListingResponseEvent
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int getFolderListing(BluetoothAddress remoteDeviceAddress, int instanceID, int maxListCount, int listStartOffset) {
            byte[] address = remoteDeviceAddress.internalByteArray();

            return getFolderListingNative(address[0], address[1], address[2], address[3], address[4], address[5], instanceID, maxListCount, listStartOffset);
        }

        /**
         * Request the number of folders in the current working directory of a
         * message access server.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @param instanceID
         *            The message access server instance ID.
         *
         * @see ClientEventCallback#getFolderListingSizeResponseEvent
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int getFolderListingSize(BluetoothAddress remoteDeviceAddress, int instanceID) {
            byte[] address = remoteDeviceAddress.internalByteArray();

            return getFolderListingSizeNative(address[0], address[1], address[2], address[3], address[4], address[5], instanceID);
        }

        /**
         * Request a message listing from the remote message access server.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @param instanceID
         *            The message access server instance ID.
         * @param folderName
         *            The folder from which the listing should be obtained. The
         *            value can only be the current working directory or child
         *            directory of the current working directory; if the value
         *            is NULL then the current working directory is assumed.
         * @param maxListCount
         *            The maximum number of entries to be included in the
         *            listing. Any filters will be applied before maxListCount
         *            is enforced.
         * @param listStartOffset
         *            Specifies how many entries to skip before the start of the
         *            listing. The listing will sorted be chronologically in
         *            descending order (newest first). Any filters will be
         *            applied before listStartOffset is considered.
         * @param listingInfo
         *            A set of values which can be used to customize the
         *            returned listing. Options include limiting the length of
         *            the subject in message entries, setting which values to
         *            return, and filtering the results.
         * @see ListingInfo
         *
         * @see ClientEventCallback#getMessageListingResponseEvent
         *
         * @return Zero if successful, or a negative return error code.
         */
        public int getMessageListing(BluetoothAddress remoteDeviceAddress, int instanceID, String folderName, int maxListCount, int listStartOffset, ListingInfo listingInfo) {

            short subjectLength = 0;
            int optionMask = 0;
            long parameterMask = 0;
            short filterMessageType = 0;
            short filterReadStatus = 0;
            int[] filterPeriodBeginArray = null;
            int[] filterPeriodEndArray = null;
            short filterPriority = 0;
            String filterRecipient = null;
            String filterOriginator = null;

            byte[] addressArray = remoteDeviceAddress.internalByteArray();

            if(listingInfo != null) {
                if(listingInfo.subjectLength > 0 && listingInfo.subjectLength < (MESSAGE_LISTING_MAXIMUM_SUBJECT_LENGTH + 1))
                    optionMask = 1 << MESSAGE_LISTING_OPTION_SUBJECT_LENGTH;
                else
                    listingInfo.subjectLength = 0;

                subjectLength = listingInfo.subjectLength;

                if(listingInfo.listingParameters != null) {
                    optionMask |= (1 << MESSAGE_LISTING_OPTION_PARAMETER_MASK);

                    for(MessageListingParameter messageListingParameter : listingInfo.listingParameters)
                        parameterMask |= (1 << messageListingParameter.ordinal());
                }

                if(listingInfo.filterMessageType != null) {
                    optionMask |= (1 << MESSAGE_LISTING_OPTION_MESSAGE_TYPE);

                    for(MessageType messageType : listingInfo.filterMessageType)
                        filterMessageType |= (1 << messageType.ordinal());
                }

                if(listingInfo.filterPeriodBegin != null) {
                    optionMask |= (1 << MESSAGE_LISTING_OPTION_PERIOD_BEGIN);

                    filterPeriodBeginArray = new int[] {listingInfo.filterPeriodBegin.get(Calendar.YEAR), listingInfo.filterPeriodBegin.get(Calendar.MONTH), listingInfo.filterPeriodBegin.get(Calendar.DATE), listingInfo.filterPeriodBegin.get(Calendar.HOUR), listingInfo.filterPeriodBegin.get(Calendar.MINUTE), listingInfo.filterPeriodBegin.get(Calendar.SECOND), listingInfo.filterPeriodBegin.isSet(Calendar.DST_OFFSET) ? 1 : 0, listingInfo.filterPeriodBegin.isSet(Calendar.DST_OFFSET) ? listingInfo.filterPeriodBegin.get(Calendar.DST_OFFSET) / 1000 / 60 : 0};
                }

                if(listingInfo.filterPeriodEnd != null) {
                    optionMask |= (1 << MESSAGE_LISTING_OPTION_PERIOD_END);
                    filterPeriodEndArray = new int[] {listingInfo.filterPeriodEnd.get(Calendar.YEAR), listingInfo.filterPeriodEnd.get(Calendar.MONTH), listingInfo.filterPeriodEnd.get(Calendar.DATE), listingInfo.filterPeriodEnd.get(Calendar.HOUR), listingInfo.filterPeriodEnd.get(Calendar.MINUTE), listingInfo.filterPeriodEnd.get(Calendar.SECOND), listingInfo.filterPeriodEnd.isSet(Calendar.DST_OFFSET) ? 1 : 0, listingInfo.filterPeriodEnd.isSet(Calendar.DST_OFFSET) ? listingInfo.filterPeriodEnd.get(Calendar.DST_OFFSET) / 1000 / 60 : 0};
                }

                if(listingInfo.filterReadStatus != null) {
                    filterReadStatus = 0;

                    optionMask |= (1 << MESSAGE_LISTING_OPTION_READ_STATUS);

                    filterReadStatus = (short)(listingInfo.filterReadStatus == FilterReadStatus.UNREAD_ONLY ? (1 << READ_STATUS_FILTER_UNREAD_ONLY) : (1 << READ_STATUS_FILTER_READ_ONLY));
                }

                if(listingInfo.filterRecipient != null) {
                    optionMask |= (1 << MESSAGE_LISTING_OPTION_RECIPIENT);

                    filterRecipient = listingInfo.filterRecipient;
                }

                if(listingInfo.filterOriginator != null) {
                    optionMask |= (1 << MESSAGE_LISTING_OPTION_ORIGINATOR);

                    filterOriginator = listingInfo.filterOriginator;
                }

                if(listingInfo.filterPriority != null) {
                    filterPriority = 0;

                    optionMask |= (1 << MESSAGE_LISTING_OPTION_PRIORITY);

                    filterPriority = (short)(listingInfo.filterPriority == FilterPriority.HIGH_ONLY ? (1 << PRIORITY_FILTER_HIGH_ONLY) : (1 << PRIORITY_FILTER_NON_HIGH_ONLY));
                }
            }

            return getMessageListingNative(addressArray, instanceID, folderName, maxListCount, listStartOffset, optionMask, subjectLength, parameterMask, filterMessageType, filterPeriodBeginArray, filterPeriodEndArray, filterReadStatus, filterRecipient, filterOriginator, filterPriority);

        }

        /**
         * Request the number of messages in a listing.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @param instanceID
         *            The message access server instance ID.
         * @param folderName
         *            The folder from which the listing should be obtained. The
         *            value can only be the current working directory or child
         *            directory of the current working directory; if the value
         *            is NULL then the current working directory is assumed.
         * @param listingSizeInfo
         *            A set of values which dictate the properties of the
         *            returned listing. (Only the values which filter entries
         *            will be used when requesting the listing size.).
         * @see ListingInfo
         *
         * @see ClientEventCallback#getMessageListingSizeResponseEvent
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int getMessageListingSize(BluetoothAddress remoteDeviceAddress, int instanceID, String folderName, ListingSizeInfo listingSizeInfo) {
            int optionMask = 0;
            short filterMessageType = 0;
            short filterReadStatus = 0;
            int[] filterPeriodBeginArray = null;
            int[] filterPeriodEndArray = null;
            short filterPriority = 0;
            String filterRecipient = null;
            String filterOriginator = null;

            byte[] addressArray = remoteDeviceAddress.internalByteArray();

            if(listingSizeInfo != null) {
                if(listingSizeInfo.filterMessageType != null) {
                    optionMask |= (1 << MESSAGE_LISTING_OPTION_MESSAGE_TYPE);

                    for(MessageType messageType : listingSizeInfo.filterMessageType)
                        filterMessageType |= (1 << messageType.ordinal());
                }

                if(listingSizeInfo.filterPeriodBegin != null) {
                    optionMask |= (1 << MESSAGE_LISTING_OPTION_PERIOD_BEGIN);

                    filterPeriodBeginArray = new int[] {listingSizeInfo.filterPeriodBegin.get(Calendar.YEAR), listingSizeInfo.filterPeriodBegin.get(Calendar.MONTH), listingSizeInfo.filterPeriodBegin.get(Calendar.DATE), listingSizeInfo.filterPeriodBegin.get(Calendar.HOUR), listingSizeInfo.filterPeriodBegin.get(Calendar.MINUTE), listingSizeInfo.filterPeriodBegin.get(Calendar.SECOND), listingSizeInfo.filterPeriodBegin.isSet(Calendar.DST_OFFSET) ? 1 : 0, listingSizeInfo.filterPeriodBegin.isSet(Calendar.DST_OFFSET) ? listingSizeInfo.filterPeriodBegin.get(Calendar.DST_OFFSET) / 1000 / 60 : 0};
                }

                if(listingSizeInfo.filterPeriodEnd != null) {
                    optionMask |= (1 << MESSAGE_LISTING_OPTION_PERIOD_END);
                    filterPeriodEndArray = new int[] {listingSizeInfo.filterPeriodEnd.get(Calendar.YEAR), listingSizeInfo.filterPeriodEnd.get(Calendar.MONTH), listingSizeInfo.filterPeriodEnd.get(Calendar.DATE), listingSizeInfo.filterPeriodEnd.get(Calendar.HOUR), listingSizeInfo.filterPeriodEnd.get(Calendar.MINUTE), listingSizeInfo.filterPeriodEnd.get(Calendar.SECOND), listingSizeInfo.filterPeriodEnd.isSet(Calendar.DST_OFFSET) ? 1 : 0, listingSizeInfo.filterPeriodEnd.isSet(Calendar.DST_OFFSET) ? listingSizeInfo.filterPeriodEnd.get(Calendar.DST_OFFSET) / 1000 / 60 : 0};
                }

                if(listingSizeInfo.filterReadStatus != null) {
                    filterReadStatus = 0;

                    optionMask |= (1 << MESSAGE_LISTING_OPTION_READ_STATUS);

                    filterReadStatus = (short)(listingSizeInfo.filterReadStatus == FilterReadStatus.UNREAD_ONLY ? (1 << READ_STATUS_FILTER_UNREAD_ONLY) : (1 << READ_STATUS_FILTER_READ_ONLY));
                }

                if(listingSizeInfo.filterRecipient != null) {
                    optionMask |= (1 << MESSAGE_LISTING_OPTION_RECIPIENT);

                    filterRecipient = listingSizeInfo.filterRecipient;
                }

                if(listingSizeInfo.filterOriginator != null) {
                    optionMask |= (1 << MESSAGE_LISTING_OPTION_ORIGINATOR);

                    filterOriginator = listingSizeInfo.filterOriginator;
                }

                if(listingSizeInfo.filterPriority != null) {
                    filterPriority = 0;

                    optionMask |= (1 << MESSAGE_LISTING_OPTION_PRIORITY);

                    filterPriority = (short)(listingSizeInfo.filterPriority == FilterPriority.HIGH_ONLY ? (1 << PRIORITY_FILTER_HIGH_ONLY) : (1 << PRIORITY_FILTER_NON_HIGH_ONLY));
                }
            }

            return getMessageListingSizeNative(addressArray, instanceID, folderName, optionMask, filterMessageType, filterPeriodBeginArray, filterPeriodEndArray, filterReadStatus, filterRecipient, filterOriginator, filterPriority);
        }

        /**
         * Retrieve a message from the remote message access server.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device
         * @param instanceID
         *            The message access server instance ID.
         * @param messageHandle
         *            The handle of the message requested. (Note that message
         *            handles can be retrieved from the message listing.)
         * @param attachment
         *            Specifies whether attachments should be included.
         * @param charSet
         *            Dictates whether and how the message access server should
         *            code the text of the message. The options are NATIVE and
         *            UTF8. If the message type is MMS or Email, then the text
         *            portion of the messages will always be coded in UTF8;
         *            thus, this parameter only applies if the message type is
         *            SMS and if the SMS message includes text. @see CharSet
         * @param fractionRequest
         *            Indicates whether the fraction requested is the first or
         *            the next in a fractioned email. This parameter is only
         *            used when retrieving a fractioned email. The options are
         *            FIRST and NEXT; the user will know a request using the
         *            NEXT option needs to be issued when the FractionDeliver
         *            parameter in the response is MORE. @see FractionalType
         *
         * @see ClientEventCallback#getMessageResponseEvent
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int getMessage(BluetoothAddress remoteDeviceAddress, int instanceID, String messageHandle, boolean attachment, CharSet charSet, FractionRequest fractionRequest) {
            int charSetConstant;
            int fractionRequestConstant;
            byte[] address;

            if(remoteDeviceAddress != null)
                address = remoteDeviceAddress.internalByteArray();
            else
                return -1;

            if(messageHandle == null)
                return -1;

            if(charSet != null)
                charSetConstant = charSet == CharSet.UTF8 ? CHARACTER_SET_UTF8 : CHARACTER_SET_NATIVE;
            else
                return -1;

            if(fractionRequest != null)
                fractionRequestConstant = fractionRequest == FractionRequest.FIRST ? FRACTION_REQUEST_FIRST : FRACTION_REQUEST_NEXT;
            else
                fractionRequestConstant = 0;

            return getMessageNative(address[0], address[1], address[2], address[3], address[4], address[5], instanceID, messageHandle, attachment, charSetConstant, fractionRequestConstant);
        }

        /**
         * Set the status of a message on a remote message access server.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @param instanceID
         *            The message access server instance ID.
         * @param messageHandle
         *            The handle of the message requested. (Note that message
         *            handles can be retrieved from the message listing.)
         * @param statusIndicator
         *            The type of status. The options are READ_STATUS and
         *            DELETED_STATUS. @see StatusIndicator
         * @param statusValue
         *            The boolean value that sets the status. {@code true} means
         *            read or deleted (moved to deleted folder). {@code false}
         *            means unread or undeleted (moved to the inbox folder).
         *
         * @see ClientEventCallback#setMessageStatusResponseEvent
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int setMessageStatus(BluetoothAddress remoteDeviceAddress, int instanceID, String messageHandle, StatusIndicator statusIndicator, boolean statusValue) {
            int statusIndicatorConstant;

            byte[] address;

            if(remoteDeviceAddress != null)
                address = remoteDeviceAddress.internalByteArray();
            else
                return -1;

            if(messageHandle == null)
                return -1;

            if(statusIndicator != null)
                statusIndicatorConstant = statusIndicator == StatusIndicator.READ_STATUS ? STATUS_INDICATOR_READ_STATUS : STATUS_INDICATOR_DELETED_STATUS;
            else
                return -1;

            return setMessageStatusNative(address[0], address[1], address[2], address[3], address[4], address[5], instanceID, messageHandle, statusIndicatorConstant, statusValue);
        }

        /**
         * Push a message to the remote message access server.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device
         * @param instanceID
         *            The message access server instance ID.
         * @param folderName
         *            The folder from which the listing should be obtained. The
         *            value can only be the current working directory or child
         *            directory of the current working directory; if the value
         *            is NULL then the current working directory is assumed. If
         *            the message is pushed to the outbox it will be sent.
         * @param transparent
         *            Determines whether a copy of a sent message will be saved
         *            to the sent folder. This parameter only applies to
         *            messages pushed to the outbox.
         * @param retry
         *            Determines whether the MSE will attempt to re-send message
         *            in case of failure. This parameter only applies to
         *            messages pushed to the outbox.
         * @param charSet
         *            Indicates how the text portion of the pushed message is
         *            coded; the options are NATIVE and UTF8. The NATIVE option
         *            is only used if the message type is a SMA_Deliver PDU.
         *            This type of message can be sent as-is by the MAS.
         * @see CharSet
         * @param messageBuffer
         *            The formatted message data object (x-bt/message).
         * @param isFinal
         *            Indicates whether the entire message is being sent in this
         *            buffer.
         *
         * @see ClientEventCallback#pushMessageResponseEvent
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int pushMessage(BluetoothAddress remoteDeviceAddress, int instanceID, String folderName, boolean transparent, boolean retry, CharSet charSet, byte[] messageBuffer, boolean isFinal) {
            int charSetConstant;

            byte[] address;

            if(remoteDeviceAddress != null)
                address = remoteDeviceAddress.internalByteArray();
            else
                return -1;

            if(charSet != null)
                charSetConstant = charSet == CharSet.UTF8 ? CHARACTER_SET_UTF8 : CHARACTER_SET_NATIVE;
            else
                return -1;

            if(messageBuffer == null)
                return -1;

            return pushMessageNative(address[0], address[1], address[2], address[3], address[4], address[5], instanceID, folderName, transparent, retry, charSetConstant, messageBuffer, isFinal);
        }

        /**
         * Signal the remote message access server to update the inbox.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @param instanceID
         *            The message access server instance ID.
         *
         * @see ClientEventCallback#updateInboxResponseEvent
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int updateInbox(BluetoothAddress remoteDeviceAddress, int instanceID) {

            byte[] address;

            if(remoteDeviceAddress != null)
                address = remoteDeviceAddress.internalByteArray();
            else
                return -1;

            return updateInboxNative(address[0], address[1], address[2], address[3], address[4], address[5], instanceID);
        }

        @Override
        protected void enableNotificationsResponse(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int responseStatusCodeConstant) {

            ResponseStatusCode responseStatusCode;
            EventCallback      callbackHandler = this.callbackHandler;

            switch(responseStatusCodeConstant) {
            case RESPONSE_STATUS_CODE_SUCCESS:
                responseStatusCode = ResponseStatusCode.SUCCESS;
                break;
            case RESPONSE_STATUS_CODE_NOT_FOUND:
                responseStatusCode = ResponseStatusCode.NOT_FOUND;
                break;
            case RESPONSE_STATUS_CODE_SERVICE_UNAVAILABLE:
                responseStatusCode = ResponseStatusCode.SERVICE_UNAVAILABLE;
                break;
            case RESPONSE_STATUS_CODE_BAD_REQUEST:
                responseStatusCode = ResponseStatusCode.BAD_REQUEST;
                break;
            case RESPONSE_STATUS_CODE_NOT_IMPLEMENTED:
                responseStatusCode = ResponseStatusCode.NOT_IMPLEMENTED;
                break;
            case RESPONSE_STATUS_CODE_UNAUTHORIZED:
                responseStatusCode = ResponseStatusCode.UNAUTHORIZED;
                break;
            case RESPONSE_STATUS_CODE_PRECONDITION_FAILED:
                responseStatusCode = ResponseStatusCode.PRECONDITION_FAILED;
                break;
            case RESPONSE_STATUS_CODE_NOT_ACCEPTABLE:
                responseStatusCode = ResponseStatusCode.NOT_ACCEPTABLE;
                break;
            case RESPONSE_STATUS_CODE_FORBIDDEN:
                responseStatusCode = ResponseStatusCode.FORBIDDEN;
                break;
            case RESPONSE_STATUS_CODE_SERVER_ERROR:
                responseStatusCode = ResponseStatusCode.SERVER_ERROR;
                break;
            case RESPONSE_STATUS_CODE_OPERATION_ABORTED:
                responseStatusCode = ResponseStatusCode.OPERATION_ABORTED;
                break;
            case RESPONSE_STATUS_CODE_DEVICE_POWER_OFF:
                responseStatusCode = ResponseStatusCode.DEVICE_POWER_OFF;
                break;
            case RESPONSE_STATUS_CODE_UNKNOWN:
                responseStatusCode = ResponseStatusCode.UNKNOWN;
                break;
            default:
                responseStatusCode = ResponseStatusCode.UNKNOWN;
                break;
            }

            if(callbackHandler != null) {
                try {
                    ((ClientEventCallback)callbackHandler).enableNotificationsResponseEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), instanceID, responseStatusCode);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

        }

        @Override
        protected void getFolderListingResponse(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int responseStatusCodeConstant, boolean isFinal, byte[] folderListingBuffer) {

            ResponseStatusCode responseStatusCode;
            EventCallback      callbackHandler = this.callbackHandler;

            switch(responseStatusCodeConstant) {
            case RESPONSE_STATUS_CODE_SUCCESS:
                responseStatusCode = ResponseStatusCode.SUCCESS;
                break;
            case RESPONSE_STATUS_CODE_NOT_FOUND:
                responseStatusCode = ResponseStatusCode.NOT_FOUND;
                break;
            case RESPONSE_STATUS_CODE_SERVICE_UNAVAILABLE:
                responseStatusCode = ResponseStatusCode.SERVICE_UNAVAILABLE;
                break;
            case RESPONSE_STATUS_CODE_BAD_REQUEST:
                responseStatusCode = ResponseStatusCode.BAD_REQUEST;
                break;
            case RESPONSE_STATUS_CODE_NOT_IMPLEMENTED:
                responseStatusCode = ResponseStatusCode.NOT_IMPLEMENTED;
                break;
            case RESPONSE_STATUS_CODE_UNAUTHORIZED:
                responseStatusCode = ResponseStatusCode.UNAUTHORIZED;
                break;
            case RESPONSE_STATUS_CODE_PRECONDITION_FAILED:
                responseStatusCode = ResponseStatusCode.PRECONDITION_FAILED;
                break;
            case RESPONSE_STATUS_CODE_NOT_ACCEPTABLE:
                responseStatusCode = ResponseStatusCode.NOT_ACCEPTABLE;
                break;
            case RESPONSE_STATUS_CODE_FORBIDDEN:
                responseStatusCode = ResponseStatusCode.FORBIDDEN;
                break;
            case RESPONSE_STATUS_CODE_SERVER_ERROR:
                responseStatusCode = ResponseStatusCode.SERVER_ERROR;
                break;
            case RESPONSE_STATUS_CODE_OPERATION_ABORTED:
                responseStatusCode = ResponseStatusCode.OPERATION_ABORTED;
                break;
            case RESPONSE_STATUS_CODE_DEVICE_POWER_OFF:
                responseStatusCode = ResponseStatusCode.DEVICE_POWER_OFF;
                break;
            case RESPONSE_STATUS_CODE_UNKNOWN:
                responseStatusCode = ResponseStatusCode.UNKNOWN;
                break;
            default:
                responseStatusCode = ResponseStatusCode.UNKNOWN;
                break;
            }

            if(callbackHandler != null) {
                try {
                    ((ClientEventCallback)callbackHandler).getFolderListingResponseEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), instanceID, responseStatusCode, isFinal, folderListingBuffer);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

        }

        @Override
        protected void getFolderListingSizeResponse(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int responseStatusCodeConstant, int numberOfFolders) {

            ResponseStatusCode responseStatusCode;
            EventCallback      callbackHandler = this.callbackHandler;

            switch(responseStatusCodeConstant) {
            case RESPONSE_STATUS_CODE_SUCCESS:
                responseStatusCode = ResponseStatusCode.SUCCESS;
                break;
            case RESPONSE_STATUS_CODE_NOT_FOUND:
                responseStatusCode = ResponseStatusCode.NOT_FOUND;
                break;
            case RESPONSE_STATUS_CODE_SERVICE_UNAVAILABLE:
                responseStatusCode = ResponseStatusCode.SERVICE_UNAVAILABLE;
                break;
            case RESPONSE_STATUS_CODE_BAD_REQUEST:
                responseStatusCode = ResponseStatusCode.BAD_REQUEST;
                break;
            case RESPONSE_STATUS_CODE_NOT_IMPLEMENTED:
                responseStatusCode = ResponseStatusCode.NOT_IMPLEMENTED;
                break;
            case RESPONSE_STATUS_CODE_UNAUTHORIZED:
                responseStatusCode = ResponseStatusCode.UNAUTHORIZED;
                break;
            case RESPONSE_STATUS_CODE_PRECONDITION_FAILED:
                responseStatusCode = ResponseStatusCode.PRECONDITION_FAILED;
                break;
            case RESPONSE_STATUS_CODE_NOT_ACCEPTABLE:
                responseStatusCode = ResponseStatusCode.NOT_ACCEPTABLE;
                break;
            case RESPONSE_STATUS_CODE_FORBIDDEN:
                responseStatusCode = ResponseStatusCode.FORBIDDEN;
                break;
            case RESPONSE_STATUS_CODE_SERVER_ERROR:
                responseStatusCode = ResponseStatusCode.SERVER_ERROR;
                break;
            case RESPONSE_STATUS_CODE_OPERATION_ABORTED:
                responseStatusCode = ResponseStatusCode.OPERATION_ABORTED;
                break;
            case RESPONSE_STATUS_CODE_DEVICE_POWER_OFF:
                responseStatusCode = ResponseStatusCode.DEVICE_POWER_OFF;
                break;
            case RESPONSE_STATUS_CODE_UNKNOWN:
                responseStatusCode = ResponseStatusCode.UNKNOWN;
                break;
            default:
                responseStatusCode = ResponseStatusCode.UNKNOWN;
                break;
            }

            if(callbackHandler != null) {
                try {
                    ((ClientEventCallback)callbackHandler).getFolderListingSizeResponseEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), instanceID, responseStatusCode, numberOfFolders);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

        }

        @Override
        protected void getMessageListingResponse(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int responseStatusCodeConstant, boolean newMessage, int[] dateTime, boolean validUTC, int offsetUTC, boolean isFinal, int numberOfMessages, byte[] messageListingBuffer) {

            ResponseStatusCode responseStatusCode;
            Calendar           timeMSE = Calendar.getInstance();
            EventCallback      callbackHandler = this.callbackHandler;

            switch(responseStatusCodeConstant) {
            case RESPONSE_STATUS_CODE_SUCCESS:
                responseStatusCode = ResponseStatusCode.SUCCESS;
                break;
            case RESPONSE_STATUS_CODE_NOT_FOUND:
                responseStatusCode = ResponseStatusCode.NOT_FOUND;
                break;
            case RESPONSE_STATUS_CODE_SERVICE_UNAVAILABLE:
                responseStatusCode = ResponseStatusCode.SERVICE_UNAVAILABLE;
                break;
            case RESPONSE_STATUS_CODE_BAD_REQUEST:
                responseStatusCode = ResponseStatusCode.BAD_REQUEST;
                break;
            case RESPONSE_STATUS_CODE_NOT_IMPLEMENTED:
                responseStatusCode = ResponseStatusCode.NOT_IMPLEMENTED;
                break;
            case RESPONSE_STATUS_CODE_UNAUTHORIZED:
                responseStatusCode = ResponseStatusCode.UNAUTHORIZED;
                break;
            case RESPONSE_STATUS_CODE_PRECONDITION_FAILED:
                responseStatusCode = ResponseStatusCode.PRECONDITION_FAILED;
                break;
            case RESPONSE_STATUS_CODE_NOT_ACCEPTABLE:
                responseStatusCode = ResponseStatusCode.NOT_ACCEPTABLE;
                break;
            case RESPONSE_STATUS_CODE_FORBIDDEN:
                responseStatusCode = ResponseStatusCode.FORBIDDEN;
                break;
            case RESPONSE_STATUS_CODE_SERVER_ERROR:
                responseStatusCode = ResponseStatusCode.SERVER_ERROR;
                break;
            case RESPONSE_STATUS_CODE_OPERATION_ABORTED:
                responseStatusCode = ResponseStatusCode.OPERATION_ABORTED;
                break;
            case RESPONSE_STATUS_CODE_DEVICE_POWER_OFF:
                responseStatusCode = ResponseStatusCode.DEVICE_POWER_OFF;
                break;
            case RESPONSE_STATUS_CODE_UNKNOWN:
                responseStatusCode = ResponseStatusCode.UNKNOWN;
                break;
            default:
                responseStatusCode = ResponseStatusCode.UNKNOWN;
                break;
            }

            timeMSE.set(dateTime[0], dateTime[1], dateTime[2], dateTime[3], dateTime[4], dateTime[5]);

            if(callbackHandler != null) {
                try {
                    ((ClientEventCallback)callbackHandler).getMessageListingResponseEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), instanceID, responseStatusCode, newMessage, timeMSE, validUTC, offsetUTC, isFinal, numberOfMessages, messageListingBuffer);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

        }

        @Override
        protected void getMessageListingSizeResponse(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int responseStatusCodeConstant, boolean newMessage, int[] dateTime, boolean validUTC, int offsetUTC, int numberOfMessages) {

            ResponseStatusCode responseStatusCode = null;
            Calendar           timeMSE = Calendar.getInstance();
            EventCallback      callbackHandler = this.callbackHandler;

            switch(responseStatusCodeConstant) {
            case RESPONSE_STATUS_CODE_SUCCESS:
                responseStatusCode = ResponseStatusCode.SUCCESS;
                break;
            case RESPONSE_STATUS_CODE_NOT_FOUND:
                responseStatusCode = ResponseStatusCode.NOT_FOUND;
                break;
            case RESPONSE_STATUS_CODE_SERVICE_UNAVAILABLE:
                responseStatusCode = ResponseStatusCode.SERVICE_UNAVAILABLE;
                break;
            case RESPONSE_STATUS_CODE_BAD_REQUEST:
                responseStatusCode = ResponseStatusCode.BAD_REQUEST;
                break;
            case RESPONSE_STATUS_CODE_NOT_IMPLEMENTED:
                responseStatusCode = ResponseStatusCode.NOT_IMPLEMENTED;
                break;
            case RESPONSE_STATUS_CODE_UNAUTHORIZED:
                responseStatusCode = ResponseStatusCode.UNAUTHORIZED;
                break;
            case RESPONSE_STATUS_CODE_PRECONDITION_FAILED:
                responseStatusCode = ResponseStatusCode.PRECONDITION_FAILED;
                break;
            case RESPONSE_STATUS_CODE_NOT_ACCEPTABLE:
                responseStatusCode = ResponseStatusCode.NOT_ACCEPTABLE;
                break;
            case RESPONSE_STATUS_CODE_FORBIDDEN:
                responseStatusCode = ResponseStatusCode.FORBIDDEN;
                break;
            case RESPONSE_STATUS_CODE_SERVER_ERROR:
                responseStatusCode = ResponseStatusCode.SERVER_ERROR;
                break;
            case RESPONSE_STATUS_CODE_OPERATION_ABORTED:
                responseStatusCode = ResponseStatusCode.OPERATION_ABORTED;
                break;
            case RESPONSE_STATUS_CODE_DEVICE_POWER_OFF:
                responseStatusCode = ResponseStatusCode.DEVICE_POWER_OFF;
                break;
            case RESPONSE_STATUS_CODE_UNKNOWN:
                responseStatusCode = ResponseStatusCode.UNKNOWN;
                break;
            default:
                responseStatusCode = ResponseStatusCode.UNKNOWN;
                break;
            }

            timeMSE.set(dateTime[0], dateTime[1], dateTime[2], dateTime[3], dateTime[4], dateTime[5]);

            if(callbackHandler != null) {
                try {
                    ((ClientEventCallback)callbackHandler).getMessageListingSizeResponseEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), instanceID, responseStatusCode, newMessage, timeMSE, validUTC, offsetUTC, numberOfMessages);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

        }

        @Override
        protected void getMessageResponse(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int responseStatusCodeConstant, int fractionDeliverConstant, boolean isFinal, byte[] messageBuffer) {

            ResponseStatusCode responseStatusCode;
            FractionDeliver    fractionDeliver;
            EventCallback      callbackHandler = this.callbackHandler;

            switch(responseStatusCodeConstant) {
            case RESPONSE_STATUS_CODE_SUCCESS:
                responseStatusCode = ResponseStatusCode.SUCCESS;
                break;
            case RESPONSE_STATUS_CODE_NOT_FOUND:
                responseStatusCode = ResponseStatusCode.NOT_FOUND;
                break;
            case RESPONSE_STATUS_CODE_SERVICE_UNAVAILABLE:
                responseStatusCode = ResponseStatusCode.SERVICE_UNAVAILABLE;
                break;
            case RESPONSE_STATUS_CODE_BAD_REQUEST:
                responseStatusCode = ResponseStatusCode.BAD_REQUEST;
                break;
            case RESPONSE_STATUS_CODE_NOT_IMPLEMENTED:
                responseStatusCode = ResponseStatusCode.NOT_IMPLEMENTED;
                break;
            case RESPONSE_STATUS_CODE_UNAUTHORIZED:
                responseStatusCode = ResponseStatusCode.UNAUTHORIZED;
                break;
            case RESPONSE_STATUS_CODE_PRECONDITION_FAILED:
                responseStatusCode = ResponseStatusCode.PRECONDITION_FAILED;
                break;
            case RESPONSE_STATUS_CODE_NOT_ACCEPTABLE:
                responseStatusCode = ResponseStatusCode.NOT_ACCEPTABLE;
                break;
            case RESPONSE_STATUS_CODE_FORBIDDEN:
                responseStatusCode = ResponseStatusCode.FORBIDDEN;
                break;
            case RESPONSE_STATUS_CODE_SERVER_ERROR:
                responseStatusCode = ResponseStatusCode.SERVER_ERROR;
                break;
            case RESPONSE_STATUS_CODE_OPERATION_ABORTED:
                responseStatusCode = ResponseStatusCode.OPERATION_ABORTED;
                break;
            case RESPONSE_STATUS_CODE_DEVICE_POWER_OFF:
                responseStatusCode = ResponseStatusCode.DEVICE_POWER_OFF;
                break;
            case RESPONSE_STATUS_CODE_UNKNOWN:
                responseStatusCode = ResponseStatusCode.UNKNOWN;
                break;
            default:
                responseStatusCode = ResponseStatusCode.UNKNOWN;
                break;
            }

            switch(fractionDeliverConstant) {
            case FRACTION_DELIVER_MORE:
                fractionDeliver = FractionDeliver.MORE;
                break;
            case FRACTION_DELIVER_LAST:
                fractionDeliver = FractionDeliver.LAST;
                break;
            default:
                fractionDeliver = null;
                break;
            }

            if(callbackHandler != null) {
                try {
                    ((ClientEventCallback)callbackHandler).getMessageResponseEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), instanceID, responseStatusCode, fractionDeliver, isFinal, messageBuffer);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

        }

        @Override
        protected void setMessageStatusResponse(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int responseStatusCodeConstant) {

            ResponseStatusCode responseStatusCode;
            EventCallback      callbackHandler = this.callbackHandler;

            switch(responseStatusCodeConstant) {
            case RESPONSE_STATUS_CODE_SUCCESS:
                responseStatusCode = ResponseStatusCode.SUCCESS;
                break;
            case RESPONSE_STATUS_CODE_NOT_FOUND:
                responseStatusCode = ResponseStatusCode.NOT_FOUND;
                break;
            case RESPONSE_STATUS_CODE_SERVICE_UNAVAILABLE:
                responseStatusCode = ResponseStatusCode.SERVICE_UNAVAILABLE;
                break;
            case RESPONSE_STATUS_CODE_BAD_REQUEST:
                responseStatusCode = ResponseStatusCode.BAD_REQUEST;
                break;
            case RESPONSE_STATUS_CODE_NOT_IMPLEMENTED:
                responseStatusCode = ResponseStatusCode.NOT_IMPLEMENTED;
                break;
            case RESPONSE_STATUS_CODE_UNAUTHORIZED:
                responseStatusCode = ResponseStatusCode.UNAUTHORIZED;
                break;
            case RESPONSE_STATUS_CODE_PRECONDITION_FAILED:
                responseStatusCode = ResponseStatusCode.PRECONDITION_FAILED;
                break;
            case RESPONSE_STATUS_CODE_NOT_ACCEPTABLE:
                responseStatusCode = ResponseStatusCode.NOT_ACCEPTABLE;
                break;
            case RESPONSE_STATUS_CODE_FORBIDDEN:
                responseStatusCode = ResponseStatusCode.FORBIDDEN;
                break;
            case RESPONSE_STATUS_CODE_SERVER_ERROR:
                responseStatusCode = ResponseStatusCode.SERVER_ERROR;
                break;
            case RESPONSE_STATUS_CODE_OPERATION_ABORTED:
                responseStatusCode = ResponseStatusCode.OPERATION_ABORTED;
                break;
            case RESPONSE_STATUS_CODE_DEVICE_POWER_OFF:
                responseStatusCode = ResponseStatusCode.DEVICE_POWER_OFF;
                break;
            case RESPONSE_STATUS_CODE_UNKNOWN:
                responseStatusCode = ResponseStatusCode.UNKNOWN;
                break;
            default:
                responseStatusCode = ResponseStatusCode.UNKNOWN;
                break;
            }

            if(callbackHandler != null) {
                try {
                    ((ClientEventCallback)callbackHandler).setMessageStatusResponseEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), instanceID, responseStatusCode);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

        }

        @Override
        protected void pushMessageResponse(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int responseStatusCodeConstant, String messageHandle) {

            ResponseStatusCode responseStatusCode;
            EventCallback      callbackHandler = this.callbackHandler;

            switch(responseStatusCodeConstant) {
            case RESPONSE_STATUS_CODE_SUCCESS:
                responseStatusCode = ResponseStatusCode.SUCCESS;
                break;
            case RESPONSE_STATUS_CODE_NOT_FOUND:
                responseStatusCode = ResponseStatusCode.NOT_FOUND;
                break;
            case RESPONSE_STATUS_CODE_SERVICE_UNAVAILABLE:
                responseStatusCode = ResponseStatusCode.SERVICE_UNAVAILABLE;
                break;
            case RESPONSE_STATUS_CODE_BAD_REQUEST:
                responseStatusCode = ResponseStatusCode.BAD_REQUEST;
                break;
            case RESPONSE_STATUS_CODE_NOT_IMPLEMENTED:
                responseStatusCode = ResponseStatusCode.NOT_IMPLEMENTED;
                break;
            case RESPONSE_STATUS_CODE_UNAUTHORIZED:
                responseStatusCode = ResponseStatusCode.UNAUTHORIZED;
                break;
            case RESPONSE_STATUS_CODE_PRECONDITION_FAILED:
                responseStatusCode = ResponseStatusCode.PRECONDITION_FAILED;
                break;
            case RESPONSE_STATUS_CODE_NOT_ACCEPTABLE:
                responseStatusCode = ResponseStatusCode.NOT_ACCEPTABLE;
                break;
            case RESPONSE_STATUS_CODE_FORBIDDEN:
                responseStatusCode = ResponseStatusCode.FORBIDDEN;
                break;
            case RESPONSE_STATUS_CODE_SERVER_ERROR:
                responseStatusCode = ResponseStatusCode.SERVER_ERROR;
                break;
            case RESPONSE_STATUS_CODE_OPERATION_ABORTED:
                responseStatusCode = ResponseStatusCode.OPERATION_ABORTED;
                break;
            case RESPONSE_STATUS_CODE_DEVICE_POWER_OFF:
                responseStatusCode = ResponseStatusCode.DEVICE_POWER_OFF;
                break;
            case RESPONSE_STATUS_CODE_UNKNOWN:
                responseStatusCode = ResponseStatusCode.UNKNOWN;
                break;
            default:
                responseStatusCode = ResponseStatusCode.UNKNOWN;
                break;
            }

            if(callbackHandler != null) {
                try {
                    ((ClientEventCallback)callbackHandler).pushMessageResponseEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), instanceID, responseStatusCode, messageHandle);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

        }

        @Override
        protected void updateInboxResponse(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int responseStatusCodeConstant) {

            ResponseStatusCode responseStatusCode;
            EventCallback      callbackHandler = this.callbackHandler;

            switch(responseStatusCodeConstant) {
            case RESPONSE_STATUS_CODE_SUCCESS:
                responseStatusCode = ResponseStatusCode.SUCCESS;
                break;
            case RESPONSE_STATUS_CODE_NOT_FOUND:
                responseStatusCode = ResponseStatusCode.NOT_FOUND;
                break;
            case RESPONSE_STATUS_CODE_SERVICE_UNAVAILABLE:
                responseStatusCode = ResponseStatusCode.SERVICE_UNAVAILABLE;
                break;
            case RESPONSE_STATUS_CODE_BAD_REQUEST:
                responseStatusCode = ResponseStatusCode.BAD_REQUEST;
                break;
            case RESPONSE_STATUS_CODE_NOT_IMPLEMENTED:
                responseStatusCode = ResponseStatusCode.NOT_IMPLEMENTED;
                break;
            case RESPONSE_STATUS_CODE_UNAUTHORIZED:
                responseStatusCode = ResponseStatusCode.UNAUTHORIZED;
                break;
            case RESPONSE_STATUS_CODE_PRECONDITION_FAILED:
                responseStatusCode = ResponseStatusCode.PRECONDITION_FAILED;
                break;
            case RESPONSE_STATUS_CODE_NOT_ACCEPTABLE:
                responseStatusCode = ResponseStatusCode.NOT_ACCEPTABLE;
                break;
            case RESPONSE_STATUS_CODE_FORBIDDEN:
                responseStatusCode = ResponseStatusCode.FORBIDDEN;
                break;
            case RESPONSE_STATUS_CODE_SERVER_ERROR:
                responseStatusCode = ResponseStatusCode.SERVER_ERROR;
                break;
            case RESPONSE_STATUS_CODE_OPERATION_ABORTED:
                responseStatusCode = ResponseStatusCode.OPERATION_ABORTED;
                break;
            case RESPONSE_STATUS_CODE_DEVICE_POWER_OFF:
                responseStatusCode = ResponseStatusCode.DEVICE_POWER_OFF;
                break;
            case RESPONSE_STATUS_CODE_UNKNOWN:
                responseStatusCode = ResponseStatusCode.UNKNOWN;
                break;
            default:
                responseStatusCode = ResponseStatusCode.UNKNOWN;
                break;
            }

            if(callbackHandler != null) {
                try {
                    ((ClientEventCallback)callbackHandler).updateInboxResponseEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), instanceID, responseStatusCode);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

        }

        @Override
        protected void setFolderResponse(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int responseStatusCodeConstant, String currentPath) {

            ResponseStatusCode responseStatusCode;
            EventCallback      callbackHandler = this.callbackHandler;

            switch(responseStatusCodeConstant) {
            case RESPONSE_STATUS_CODE_SUCCESS:
                responseStatusCode = ResponseStatusCode.SUCCESS;
                break;
            case RESPONSE_STATUS_CODE_NOT_FOUND:
                responseStatusCode = ResponseStatusCode.NOT_FOUND;
                break;
            case RESPONSE_STATUS_CODE_SERVICE_UNAVAILABLE:
                responseStatusCode = ResponseStatusCode.SERVICE_UNAVAILABLE;
                break;
            case RESPONSE_STATUS_CODE_BAD_REQUEST:
                responseStatusCode = ResponseStatusCode.BAD_REQUEST;
                break;
            case RESPONSE_STATUS_CODE_NOT_IMPLEMENTED:
                responseStatusCode = ResponseStatusCode.NOT_IMPLEMENTED;
                break;
            case RESPONSE_STATUS_CODE_UNAUTHORIZED:
                responseStatusCode = ResponseStatusCode.UNAUTHORIZED;
                break;
            case RESPONSE_STATUS_CODE_PRECONDITION_FAILED:
                responseStatusCode = ResponseStatusCode.PRECONDITION_FAILED;
                break;
            case RESPONSE_STATUS_CODE_NOT_ACCEPTABLE:
                responseStatusCode = ResponseStatusCode.NOT_ACCEPTABLE;
                break;
            case RESPONSE_STATUS_CODE_FORBIDDEN:
                responseStatusCode = ResponseStatusCode.FORBIDDEN;
                break;
            case RESPONSE_STATUS_CODE_SERVER_ERROR:
                responseStatusCode = ResponseStatusCode.SERVER_ERROR;
                break;
            case RESPONSE_STATUS_CODE_OPERATION_ABORTED:
                responseStatusCode = ResponseStatusCode.OPERATION_ABORTED;
                break;
            case RESPONSE_STATUS_CODE_DEVICE_POWER_OFF:
                responseStatusCode = ResponseStatusCode.DEVICE_POWER_OFF;
                break;
            case RESPONSE_STATUS_CODE_UNKNOWN:
                responseStatusCode = ResponseStatusCode.UNKNOWN;
                break;
            default:
                responseStatusCode = ResponseStatusCode.UNKNOWN;
                break;
            }

            if(callbackHandler != null) {
                try {
                    ((ClientEventCallback)callbackHandler).setFolderResponseEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), instanceID, responseStatusCode, currentPath);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

        }

        @Override
        protected void notificationIndication(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, boolean isFinal, byte[] eventReportBuffer) {

            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((ClientEventCallback)callbackHandler).notificationIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), instanceID, isFinal, eventReportBuffer);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

        }

        /**
         * Receiver for MAP Client events.
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
             * Invoked when a response to an enable/disable notifications
             * request has been received.
             *
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The message access server instance ID.
             * @param responseStatusCode
             *            The response code from the server.
             */
            public void enableNotificationsResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode);

            /**
             * Invoked when a folder listing has been received from the message
             * access server.
             *
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The message access server instance ID.
             * @param responseStatusCode
             *            The response code from the server.
             * @param isFinal
             *            Indicates whether this is the final data packet.
             * @param folderListingBuffer
             *            A buffer containing the folder listing data.
             */
            public void getFolderListingResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, boolean isFinal, byte[] folderListingBuffer);

            /**
             * Invoked when a folder listing count has been received from the
             * message access server.
             *
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The message access server instance ID.
             * @param responseStatusCode
             *            The response code from the server.
             * @param numberOfFolders
             *            The number of folders.
             */
            public void getFolderListingSizeResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, int numberOfFolders);

            /**
             * Invoked when a message listing has been received from the message
             * access server.
             *
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The message access server instance ID.
             * @param responseStatusCode
             *            The response code from the server.
             * @param isFinal
             *            Indicates whether this is the final data packet.
             * @param newMessage
             *            Indicates whether the listing includes an unread
             *            message.
             * @param timeMSE
             *            The time and date stamp from the message server
             *            equipment (MSE).
             * @param validUTC
             *            Indicates whether the UTC offset value is valid.
             * @param offsetUTC
             *            Signed value equaling the number of minutes difference
             *            between the MSE time and UTC time.
             * @param numberOfMessages
             *            The number of messages in the listing.
             * @param messageListingBuffer
             *            The buffer containing the listing object data.
             */
            public void getMessageListingResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, boolean newMessage, Calendar timeMSE, boolean validUTC, int offsetUTC, boolean isFinal, int numberOfMessages, byte[] messageListingBuffer);

            /**
             * Invoked when a message listing count has been received from the
             * message access server.
             *
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The message access server instance ID.
             * @param responseStatusCode
             *            The response code from the server.
             * @param newMessage
             *            Indicates whether the listing includes an unread
             *            message.
             * @param timeMSE
             *            The time and date stamp from the message server
             *            equipment (MSE).
             * @param validUTC
             *            Indicates whether the UTC offset value is valid.
             * @param offsetUTC
             *            Signed value equaling the number of minutes difference
             *            between the MSE time and UTC time.
             * @param numberMessages
             *            The number of messages.
             */
            public void getMessageListingSizeResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, boolean newMessage, Calendar timeMSE, boolean validUTC, int offsetUTC, int numberMessages);

            /**
             * Invoked when a message has been delivered.
             *
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The message access server instance ID.
             * @param responseStatusCode
             *            The response code from the server.
             * @param fractionDeliver
             *            Indicates whether the fraction delivered is the last
             *            This parameter is only used when delivering a
             *            fractioned email. The options are LAST and MORE. The
             *            user will know a request using the NEXT option needs
             *            to be issued when the FractionDeliver parameter in the
             *            response is MORE. @see FractionRequest @see
             *            FractionDeliver
             * @param isFinal
             *            Indicates whether this is the final data packet.
             * @param messageBuffer
             *            The buffer containing the message object data.
             */
            public void getMessageResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, FractionDeliver fractionDeliver, boolean isFinal, byte[] messageBuffer);

            /**
             * Invoked when a response to a set message status request arrives.
             *
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The message access server instance ID.
             * @param responseStatusCode
             *            Response code from the server.
             */
            public void setMessageStatusResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode);

            /**
             * Invoked when a response to a push message request has arrived.
             *
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The message access server instance ID.
             * @param responseStatusCode
             *            The response code from the server.
             * @param messageHandle
             *            The handle assigned to the pushed message.
             */
            public void pushMessageResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, String messageHandle);

            /**
             * Invoked when a response from an update inbox request has arrived.
             *
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The message access server instance ID.
             * @param responseStatusCode
             *            The response code from the server.
             */
            public void updateInboxResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode);

            /**
             * Invoked when a response to a set folder request arrives.
             *
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The message access server instance ID.
             * @param responseStatusCode
             *            Response code from the server.
             * @param currentPath
             *            The full path of the current working directory.
             */
            public void setFolderResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, String currentPath);

            /**
             * Invoked when a message notification client (on the MSE) sends an
             * event notification to a notification server (on the MCE).
             *
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The message access server instance ID.
             * @param isFinal
             *            Indicates whether the event report has been completed.
             * @param eventReportBuffer
             *            The notification event data.
             */
            public void notificationIndicationEvent(BluetoothAddress remoteDeviceAddress, int instanceID, boolean isFinal, byte[] eventReportBuffer);
        }
    }

    /**
     *
     */
    public static class MessageAccessServerManager extends MAPM {

        /**
         * Creates a new management connection to the local MAP Server Manager.
         * Note that MAP Server profile connections are not persistent.
         * Connections are tied to the Manager object that creates them; when
         * the Manager object is disposed, the server and any associated
         * connections will be closed.
         *
         * @param eventCallback
         *            Receiver for events sent by the server.
         *
         * @throws ServerNotReachableException
         *             if the BluetopiaPM server cannot be reached.
         */
        public MessageAccessServerManager(ServerEventCallback eventCallback) throws ServerNotReachableException {
            super(MANAGER_TYPE_SERVER, eventCallback);
        }

        /**
         * Respond to a connection request from the message client equipment
         * (MCE).
         * <p>
         * The Bluetooth address of the MCE and the instance ID of the message
         * access server(MAS) running on the message server equipment (MSE)
         * uniquely identify the connection request.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @param instanceID
         *            The instance ID from the SDP record of the message access
         *            server.
         * @param accept
         *            Boolean value indicating whether the request has been
         *            accepted or not.
         * @return The function returns zero if launched successfully, or a
         *         negative return error code. This return code does not
         *         indicate whether the connection has been established; a
         *         connection event( {@link ServerEventCallback#connectedEvent} )
         *         will be dispatched with the result of the attempt.
         */
        public int connectionRequestResponse(BluetoothAddress remoteDeviceAddress, int instanceID, boolean accept) {
            throw new UnsupportedOperationException();
        }

        /**
         * Register a local message access server (MAS) on a specified RFCOMM
         * port.
         *
         * @param serverPort
         *            The RFCOMM port on which to register the server.
         * @param serverFlags
         *            The incoming connection requirements -
         *            {@link IncomingConnectionFlags}
         * @param instanceID
         *            The instance ID of the message access server being
         *            registered.
         * @param supportedMessageTypes
         *            The types of messages that this message access server
         *            supports - {@link MessageType}
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int registerServer(int serverPort, EnumSet<IncomingConnectionFlags> serverFlags, int instanceID, EnumSet<MessageType> supportedMessageTypes) {
            throw new UnsupportedOperationException();
        }

        /**
         * Unregister a local message access server (MAS) identified by the
         * instance ID.
         *
         * @param instanceID
         *            The instance ID of the message access server being
         *            unregistered.
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int unRegisterServer(int instanceID) {
            throw new UnsupportedOperationException();
        }

        /**
         * Register the SDP record for a successfully registered
         * message access server (MAS).
         *
         * @param instanceID
         *            The instance ID that identifies the message access server
         *            in the SDP record.
         * @param serviceName
         *            The service name to appear in the SDP record, and that is
         *            unique to the message access server instance.
         * @return The function returns a positive value if successful, or a
         *         negative return error code if there was an error. If
         *         successful, the return value is the SDP record handle.
         */
        public long registerServiceRecord(int instanceID, String serviceName) {
            throw new UnsupportedOperationException();
        }

        /**
         * Unregister the SDP record associated with a message
         * access server.
         *
         * @param instanceID
         *            The instance ID that identifies the message access server.
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int unRegisterServiceRecord(int instanceID) {
            throw new UnsupportedOperationException();
        }

        /**
         * Respond to a request to enable notifications.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @param instanceID
         *            The ID of the local server instance.
         * @param responseStatusCode
         *            The OBEX response to include.
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int enableNotificationsConfirmation(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode) {
            throw new UnsupportedOperationException();
        }

        /**
         * Send a notification event to the remote notification server.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @param instanceID
         *            The ID of the local server instance.
         * @param buffer
         *            The event data to send.
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int sendNotification(BluetoothAddress remoteDeviceAddress, int instanceID, byte[] buffer) {
            throw new UnsupportedOperationException();
        }

        /**
         * Respond to a request to get a folder listing.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @param instanceID
         *            The ID of the local server instance.
         * @param responseStatusCode
         *            The OBEX response to include.
         * @param buffer
         *            The listing data to send with the response. Can be
         *            {@code NULL} if the ResponseStatusCode indicates an error.
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int sendFolderListing(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, byte[] buffer) {
            throw new UnsupportedOperationException();
        }

        /**
         * Respond to a request to get the size of a folder listing.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @param instanceID
         *            The ID of the local server instance.
         * @param responseStatusCode
         *            The OBEX response to include.
         * @param size
         *            The size of the listing (if the request was successful.)
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int sendFolderListingSize(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, int size) {
            throw new UnsupportedOperationException();
        }

        /**
         * Respond to a request to get a message listing.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @param instanceID
         *            The ID of the local server instance.
         * @param responseStatusCode
         *            The OBEX response to include.
         * @param messageCount
         *            The number of messages (if the request was successful.)
         * @param currentTime
         *            The time at which this response is being sent.
         * @param buffer
         *            The listing data to send with the response. Can be
         *            {@code NULL} if the ResponseStatusCode indicates an error.
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int sendMessageListing(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, int messageCount, Calendar currentTime, byte[] buffer) {
            throw new UnsupportedOperationException();
        }

        /**
         * Respond to a request to get the size of a message listing.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @param instanceID
         *            The ID of the local server instance.
         * @param responseStatusCode
         *            The OBEX response to include.
         * @param size
         *            The size of the listing (if the request was successful.)
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int sendMessageListingSize(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, int size) {
            throw new UnsupportedOperationException();
        }

        /**
         * Respond to a request to get a message.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @param instanceID
         *            The ID of the local server instance.
         * @param responseStatusCode
         *            The OBEX response to include.
         * @param fractionDeliver
         *            The fragment type of the data in this response.
         * @param buffer
         *            The message data.
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int sendMessage(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, FractionDeliver fractionDeliver, byte[] buffer) {
            throw new UnsupportedOperationException();
        }

        /**
         * Respond to a request to set message status.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @param instanceID
         *            The ID of the local server instance.
         * @param responseStatusCode
         *            The OBEX response to include.
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int setMessageStatusConfirmation(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode) {
            throw new UnsupportedOperationException();
        }

        /**
         * Respond to a request to push a message.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @param instanceID
         *            The ID of the local server instance.
         * @param responseStatusCode
         *            The OBEX response to include.
         * @param messageHandle
         *            The handle generated for the pushed message.
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int pushMessageConfirmation(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, String messageHandle) {
            throw new UnsupportedOperationException();
        }

        /**
         * Respond to a request to update the inbox.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @param instanceID
         *            The ID of the local server instance.
         * @param responseStatusCode
         *            The OBEX response to include.
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int updateInboxConfirmation(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode) {
            throw new UnsupportedOperationException();
        }

        /**
         * Respond to a request to set folder.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @param instanceID
         *            The ID of the local server instance.
         * @param responseStatusCode
         *            The OBEX response to include.
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error.
         */
        public int setFolderConfirmation(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void connected(ConnectionType connectionType, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void incomingConnectionRequest(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void enableNotificationsRequest(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, boolean enabled) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void notificationConfirmation(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int status) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void getFolderListingRequest(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, int maxListCount, int listStartOffset) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void getFolderListingSizeRequest(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void getmessageListingRequest(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, String folderName, int maxListCount, int listStartOffset, ListingInfo listingInfo) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void getMessageListingSizeRequest(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, String folderName) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void getMessageRequest(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, boolean attachment, CharSet charSet, FractionRequest fractionRequest, String messageHandle) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void setMessageStatusRequest(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, String messageHandle, StatusIndicator statusIndicator, boolean statusValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void pushMessageRequest(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, String folderName, boolean transparent, boolean retry, CharSet charSet, boolean isFinal, byte[] buffer) {
            throw new UnsupportedOperationException();

        }

        @Override
        protected void updateInboxRequest(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void setFolderRequest(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int instanceID, PathOption pathOption, String folderName) {
            throw new UnsupportedOperationException();
        }

        /**
         * Receiver for MAP Client event notifications.
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
             * Invoked when a device connects to a local server port.
             *
             * @param connectionType
             *            Distinguishes between notification and message access
             *            connections, as well as distinguishing between server and
             *            client.
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The instance ID; applies only to message access
             *            connections.
             */
            public void connectedEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int instanceID);

            /**
             * Invoked when a remote client requests to connect to a local
             * server port.
             *
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The ID of the local server instance.
             */
            public void incomingConnectionRequestEvent(BluetoothAddress remoteDeviceAddress, int instanceID);

            /**
             * Invoked when a remote client requests to enable/disable
             * notifications.
             *
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The ID of the local server instance.
             * @param enabled
             *            Indicates whether notifications are enabled or
             *            disabled.
             */
            public void enableNotificationsRequestEvent(BluetoothAddress remoteDeviceAddress, int instanceID, boolean enabled);

            /**
             * Invoked when a remote client responds to a notification.
             *
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The ID of the local server instance.
             * @param status
             *            The status of the request.
             */
            public void notificationConfirmationEvent(BluetoothAddress remoteDeviceAddress, int instanceID, int status);

            /**
             * Invoked when a remote client requests a folder listing.
             *
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The ID of the local server instance.
             * @param maxListCount
             *            The maximum number of items to be included in the
             *            response.
             * @param listStartOffset
             *            The offset in the list to begin from.
             */
            public void getFolderListingRequestEvent(BluetoothAddress remoteDeviceAddress, int instanceID, int maxListCount, int listStartOffset);

            /**
             * Invoked when a remote client requests a folder listing size.
             *
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The ID of the local server instance.
             */
            public void getFolderListingSizeRequestEvent(BluetoothAddress remoteDeviceAddress, int instanceID);

            /**
             * Invoked when a remote client requests a message listing.
             *
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The ID of the local server instance.
             * @param folderName
             *            The folder to pull the listing from. A {@code NULL}
             *            value indicates the current folder.
             * @param maxListCount
             *            The maximum number of items to be included in the
             *            response.
             * @param listStartOffset
             *            The offset in the list to begin from.
             * @param listingInfo
             */
            public void getmessageListingRequestEvent(BluetoothAddress remoteDeviceAddress, int instanceID, String folderName, int maxListCount, int listStartOffset, ListingInfo listingInfo);

            /**
             * Invoked when a remote client requests to connect to a local
             * server port.
             *
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The ID of the local server instance.
             * @param folderName
             *            The folder to pull the listing from. A {@code NULL}
             *            value indicates the current folder.
             * @param listingSizeInfo
             */
            public void getMessageListingSizeRequestEvent(BluetoothAddress remoteDeviceAddress, int instanceID, String folderName, ListingSizeInfo listingSizeInfo);

            /**
             * Invoked when a remote client requests a message.
             *
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The ID of the local server instance.
             * @param attachment
             *            Indicates if any attachments should be included in the
             *            response.
             * @param charSet
             *            The format of the message.
             * @param fractionRequest
             *            Indicates the message fragment type being requested.
             * @param messageHandle
             *            The handle of the message to pull.
             */
            public void getMessageRequestEvent(BluetoothAddress remoteDeviceAddress, int instanceID, boolean attachment, CharSet charSet, FractionRequest fractionRequest, String messageHandle);

            /**
             * Invoked when a remote client requests to set the status of a
             * message.
             *
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The ID of the local server instance.
             * @param messageHandle
             *            The handle of the message.
             * @param statusIndicator
             *            The status to set.
             * @param statusValue
             *            The new value.
             */
            public void setMessageStatusRequestEvent(BluetoothAddress remoteDeviceAddress, int instanceID, String messageHandle, StatusIndicator statusIndicator, boolean statusValue);

            /**
             * Invoked when a remote client requests to push/send a message.
             *
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The ID of the local server instance.
             * @param folderName
             *            The folder to push the message to. A {@code NULL}
             *            value indicates the current folder.
             * @param transparent
             *            Indicates if a copy should be stored in the sent
             *            folder.
             * @param retry
             *            Indicates whether the message should be resent if
             *            sending fails.
             * @param charSet
             *            The format of the message.
             * @param isFinal
             *            Indicates whether this is the last data packet of the
             *            request.
             * @param buffer
             *            The message data.
             */
            public void pushMessageRequestEvent(BluetoothAddress remoteDeviceAddress, int instanceID, String folderName, boolean transparent, boolean retry, CharSet charSet, boolean isFinal, byte[] buffer);

            /**
             * Invoked when a remote client requests to update the local inbox.
             *
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The ID of the local server instance.
             */
            public void updateInboxRequestEvent(BluetoothAddress remoteDeviceAddress, int instanceID);

            /**
             * Invoked when a remote client requests to set the folder.
             *
             * @param remoteDeviceAddress
             *            The Bluetooth address of the remote device.
             * @param instanceID
             *            The ID of the local server instance.
             * @param pathOption
             *            The direction of the action to take.
             * @param folderName
             *            The folder to move into.
             */
            public void setFolderRequestEvent(BluetoothAddress remoteDeviceAddress, int instanceID, PathOption pathOption, String folderName);
        }
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

    private final static int INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHORIZATION  = 1;
    private final static int INCOMING_CONNECTION_FLAGS_REQUIRE_AUTHENTICATION = 2;
    private final static int INCOMING_CONNECTION_FLAGS_REQUIRE_ENCRYPTION     = 3;

    /**
     * Flags which control how incoming connections to a server are handled.
     */
    public enum IncomingConnectionFlags {
        /**
         * Require the Bluetooth link to be authorized. This requires that the
         * user manually respond to the request to accept or reject it.
         */
        REQUIRE_AUTHORIZATION,

        /**
         * Require the Bluetooth link to be authenticated. This requires that
         * the devices are paired and will automatically initiate the pairing
         * process if required.
         */
        REQUIRE_AUTENTICATION,

        /**
         * Require the Bluetooth link to be encrypted.
         */
        REQUIRE_ENCRYPTION
    }

    private final static int RESPONSE_STATUS_CODE_SUCCESS                     = 0;
    private final static int RESPONSE_STATUS_CODE_NOT_FOUND                   = 1;
    private final static int RESPONSE_STATUS_CODE_SERVICE_UNAVAILABLE         = 2;
    private final static int RESPONSE_STATUS_CODE_BAD_REQUEST                 = 3;
    private final static int RESPONSE_STATUS_CODE_NOT_IMPLEMENTED             = 4;
    private final static int RESPONSE_STATUS_CODE_UNAUTHORIZED                = 5;
    private final static int RESPONSE_STATUS_CODE_PRECONDITION_FAILED         = 6;
    private final static int RESPONSE_STATUS_CODE_NOT_ACCEPTABLE              = 7;
    private final static int RESPONSE_STATUS_CODE_FORBIDDEN                   = 8;
    private final static int RESPONSE_STATUS_CODE_SERVER_ERROR                = 9;
    private final static int RESPONSE_STATUS_CODE_OPERATION_ABORTED           = 10;
    private final static int RESPONSE_STATUS_CODE_OPERATION_ABORTED_RESOURCES = 11;
    private final static int RESPONSE_STATUS_CODE_DEVICE_POWER_OFF            = 12;
    private final static int RESPONSE_STATUS_CODE_UNABLE_TO_SUBMIT_REQUEST    = 13;
    private final static int RESPONSE_STATUS_CODE_UNKNOWN                     = 14;

    /**
     *
     */
    public enum ResponseStatusCode {
        /**
         *
         */
        SUCCESS,
        /**
         *
         */
        NOT_FOUND,
        /**
         * .
         */
        SERVICE_UNAVAILABLE,
        /**
         *
         */
        BAD_REQUEST,
        /**
         *
         */
        NOT_IMPLEMENTED,
        /**
         *
         */
        UNAUTHORIZED,
        /**
         *
         */
        PRECONDITION_FAILED,
        /**
         *
         */
        NOT_ACCEPTABLE,
        /**
         *
         */
        FORBIDDEN,
        /**
         *
         */
        SERVER_ERROR,
        /**
         *
         */
        OPERATION_ABORTED,
        /**
         *
         */
        DEVICE_POWER_OFF,
        /**
         *
         */
        UNKNOWN
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

    private final static int CONNECTION_TYPE_NOTIFICATION_SERVER   = 1;
    private final static int CONNECTION_TYPE_NOTIFICATION_CLIENT   = 2;
    private final static int CONNECTION_TYPE_MESSAGE_ACCESS_SERVER = 3;
    private final static int CONNECTION_TYPE_MESSAGE_ACCESS_CLIENT = 4;

    /**
     * Types of connections.
     */
    public enum ConnectionType {
        /**
         * A connection to a local notification server.
         */
        NOTIFICATION_SERVER,

        /**
         * A connection to a remote notification server.
         */
        NOTIFICATION_CLIENT,

        /**
         * A connection to a local message access server.
         */
        MESSAGE_ACCESS_SERVER,

        /**
         * A connection to a remote message access server.
         */
        MESSAGE_ACCESS_CLIENT
    }

    private final static int CONNECTION_CATEGORY_NOTIFICATION   = 1;
    private final static int CONNECTION_CATEGORY_MESSAGE_ACCESS = 2;

    /**
     * Categories of connections.
     */
    public enum ConnectionCategory {
        /**
         * A connection between a notification client and server.
         */
        NOTIFICATION,

        /**
         * A connection between a message access client and server.
         */
        MESSAGE_ACCESS
    }

    private final static int CONNECTION_CLIENT_NOTIFICATION   = CONNECTION_TYPE_NOTIFICATION_CLIENT;
    private final static int CONNECTION_CLIENT_MESSAGE_ACCESS = CONNECTION_TYPE_MESSAGE_ACCESS_CLIENT;

    /**
     * Types of clients.
     */
    public enum ConnectionClient {
        /**
         * A notification client.
         */
        NOTIFICATION_CLIENT,

        /**
         * A message access client.
         */
        MESSAGE_ACCESS_CLIENT
    }

    private final static int CONNECTION_SERVER_NOTIFICATION   = CONNECTION_TYPE_NOTIFICATION_SERVER;
    private final static int CONNECTION_SERVER_MESSAGE_ACCESS = CONNECTION_TYPE_MESSAGE_ACCESS_SERVER;

    /**
     * Types of servers.
     */
    public enum ConnectionServer {
        /**
         * A notification server.
         */
        NOTIFICATION_SERVER,

        /**
         * A message access server.
         */
        MESSAGE_ACCESS_SERVER
    }

    private static final int MESSAGE_LISTING_MAXIMUM_SUBJECT_LENGTH      = 255;

    /*
     * The following constants represents the bit locations for each of the
     * options in a Message Listing Option mask.
     */
    private static final int MESSAGE_LISTING_OPTION_SUBJECT_LENGTH       = 0;
    private static final int MESSAGE_LISTING_OPTION_PARAMETER_MASK       = 1;
    private static final int MESSAGE_LISTING_OPTION_MESSAGE_TYPE         = 2;
    private static final int MESSAGE_LISTING_OPTION_PERIOD_BEGIN         = 3;
    private static final int MESSAGE_LISTING_OPTION_PERIOD_END           = 4;
    private static final int MESSAGE_LISTING_OPTION_READ_STATUS          = 5;
    private static final int MESSAGE_LISTING_OPTION_RECIPIENT            = 6;
    private static final int MESSAGE_LISTING_OPTION_ORIGINATOR           = 7;
    private static final int MESSAGE_LISTING_OPTION_PRIORITY             = 8;

    /*
     * The following constants represent the bit locations for each of the
     * parameters in a Message Listing Parameter Mask.
     */
    private static final int MESSAGE_LISTING_PARAMETER_SUBJECT           = 0;
    private static final int MESSAGE_LISTING_PARAMETER_DATE_TIME         = 1;
    private static final int MESSAGE_LISTING_PARAMETER_SENDER_NAME       = 2;
    private static final int MESSAGE_LISTING_PARAMETER_SENDER_ADDRESS    = 3;
    private static final int MESSAGE_LISTING_PARAMETER_RECIPIENT_NAME    = 4;
    private static final int MESSAGE_LISTING_PARAMETER_RECIPIENT_ADDRESS = 5;
    private static final int MESSAGE_LISTING_PARAMETER_TYPE              = 6;
    private static final int MESSAGE_LISTING_PARAMETER_SIZE              = 7;
    private static final int MESSAGE_LISTING_PARAMETER_RECEPTION_STATUS  = 8;
    private static final int MESSAGE_LISTING_PARAMETER_TEXT              = 9;
    private static final int MESSAGE_LISTING_PARAMETER_ATTACHMENT_SIZE   = 10;
    private static final int MESSAGE_LISTING_PARAMETER_PRIORITY          = 11;
    private static final int MESSAGE_LISTING_PARAMETER_READ              = 12;
    private static final int MESSAGE_LISTING_PARAMETER_SENT              = 13;
    private static final int MESSAGE_LISTING_PARAMETER_PROTECTED         = 14;
    private static final int MESSAGE_LISTING_PARAMETER_REPLY_TO_ADDRESS  = 15;

    /**
     * Indicates the filters to be returned a message listing.
     */
    public enum MessageListingParameter {
        /**
         * The subject of the message.
         */
        SUBJECT,

        /**
         * The date and time of the message.
         */
        DATE_TIME,

        /**
         * The name of the sender.
         */
        SENDER_NAME,

        /**
         * The address of the sender.
         */
        SENDER_ADDRESS,

        /**
         * The name of the recipient.
         */
        RECIPIENT_NAME,

        /**
         * The address of the recipient.
         */
        RECIPIENT_ADDRESS,

        /**
         * The type of message.
         */
        TYPE,

        /**
         * The size of the message.
         */
        SIZE,

        /**
         * The status of the reception of the message.
         */
        RECEPTION_STATUS,

        /**
         * Indicates whether the message has textual information.
         */
        TEXT,

        /**
         * The size of any attachments.
         */
        ATTACHMENT_SIZE,

        /**
         * The priority of the message.
         */
        PRIORITY,

        /**
         * The 'read' status of the message
         */
        READ,

        /**
         * The 'sent' status of the message
         */
        SENT,

        /**
         * Indicates if the message has DRM.
         */
        PROTECTED,

        /**
         * The reply-to address of the message.
         */
        REPLY_TO_ADDRESS
    }

    private static final int MESSAGE_TYPE_MMS      = 4;

    /**
     * Use this enum for get message filtering.
     */
    public enum MessageType {
        /**
         * GSM-based SMS messages.
         */
        SMS_GSM,

        /**
         * CDMA-based SMS messages.
         */
        SMS_CDMA,

        /**
         * E-mail messages.
         */
        EMAIL,

        /**
         * MMS Messages.
         */
        MMS
    }


    /**
     * Use this enum for message types supported by the server.
     */
    public enum SupportedMessageType {
        /**
         * E-mail messages.
         */
        EMAIL,
        /**
         * GSM-based SMS messages.
         */
        SMS_GSM,
        /**
         * CDMA-based SMS messages.
         */
        SMS_CDMA,
        /**
         * MMS Messages.
         */
        MMS
    }

    private static final int READ_STATUS_FILTER_UNREAD_ONLY = 0;
    private static final int READ_STATUS_FILTER_READ_ONLY   = 1;

    /**
     * Indicates how a message listing should be filtered by read status.
     */
    public enum FilterReadStatus {
        /**
         * Only return unread messages.
         */
        UNREAD_ONLY,

        /**
         * Only return read messages.
         */
        READ_ONLY
    }

    private static final int PRIORITY_FILTER_HIGH_ONLY     = 0;
    private static final int PRIORITY_FILTER_NON_HIGH_ONLY = 1;

    /**
     * Indicates the how a message listing should be filtered by priority.
     */
    public enum FilterPriority {
        /**
         * Only return high priority messages.
         */
        HIGH_ONLY,

        /**
         * Only return low priority messages.
         */
        NON_HIGH_ONLY
    }

    private static final int CHARACTER_SET_NATIVE = 1;
    private static final int CHARACTER_SET_UTF8   = 2;

    /**
     * Possible character sets used in messages.
     */
    public enum CharSet {

        /**
         * Native character set.
         */
        NATIVE,

        /**
         * UTF-8 character set.
         */
        UTF8
    }

    private static final int FRACTIONAL_TYPE_FIRST = 1;
    private static final int FRACTIONAL_TYPE_NEXT  = 2;
    private static final int FRACTIONAL_TYPE_MORE  = 3;
    private static final int FRACTIONAL_TYPE_LAST  = 4;

    /**
     * The type of fraction delivered for fractioned email.
     */
    public enum FractionalType {
        /**
         * Requests the first part of a fragmented message.
         */
        FIRST,
        /**
         * Requests the next part of a fragmented message.
         */
        NEXT,
        /**
         * Indicates that there are more fragments.
         */
        MORE,
        /**
         * Indicates the last fragment.
         */
        LAST
    }

    private static final int FRACTION_REQUEST_FIRST = FRACTIONAL_TYPE_FIRST;
    private static final int FRACTION_REQUEST_NEXT  = FRACTIONAL_TYPE_NEXT;

    /**
     * The type of fraction requested for fractioned email.
     */
    public enum FractionRequest {
        /**
         * Request the first part of a fragmented message.
         */
        FIRST,

        /**
         * Request the next part of a fragmented message.
         */
        NEXT
    }

    private static final int FRACTION_DELIVER_MORE = FRACTIONAL_TYPE_MORE;
    private static final int FRACTION_DELIVER_LAST = FRACTIONAL_TYPE_LAST;

    /**
     * The type of fraction delivered for fractioned email.
     */
    public enum FractionDeliver {

        /**
         * Indicates that there are more fragments.
         */
        MORE,

        /**
         * Indicates the last fragment.
         */
        LAST
    }

    private static final int PATH_OPTION_ROOT = 1;
    private static final int PATH_OPTION_DOWN = 2;
    private static final int PATH_OPTION_UP   = 3;

    /**
     * Specifies the type of navigation for directory change requests.
     *
     * @see MessageAccessClientManager#setFolder
     */
    public enum PathOption {
        /**
         * Navigate to the root directory.
         */
        ROOT,

        /**
         * Navigate down one level into the specified directory.
         */
        DOWN,

        /**
         * Navigate up to the parent directory.
         */
        UP
    }

    private static final int STATUS_INDICATOR_READ_STATUS    = 1;
    private static final int STATUS_INDICATOR_DELETED_STATUS = 2;

    /**
     * Specifies what status indicator to update.
     *
     * @see MessageAccessClientManager#setMessageStatus
     */
    public enum StatusIndicator {
        /**
         * Update read/unread status.
         */
        READ_STATUS,

        /**
         * Update deleted status.
         */
        DELETED_STATUS
    }

    /**
     * Options information for a message listing request.
     *
     * @see MessageAccessClientManager#getMessageListing
     */
    public static final class ListingInfo {
        short                            subjectLength;
        EnumSet<MessageListingParameter> listingParameters;
        EnumSet<MessageType>             filterMessageType;
        Calendar                         filterPeriodBegin;
        Calendar                         filterPeriodEnd;
        FilterReadStatus                 filterReadStatus;
        String                           filterRecipient;
        String                           filterOriginator;
        FilterPriority                   filterPriority;

        /**
         * @param subjectLength
         *            Determines the maximum string length of the subject
         *            parameter in a message listing object entry.
         * @param listingParameters
         *            Indicates which parameters will be included in message
         *            listing entries. If the parameters listing mask is not
         *            included, the parameters labeled as required in the
         *            message listing object document type definition shall be
         *            included.
         * @param filterMessageType
         *            Filters the listing according to message type.
         * @param filterPeriodBegin
         *            Filters out message listings with a delivery date older
         *            than the {@code periodBeginFilter} date.
         * @param filterPeriodEnd
         *            Filters out message listings with a delivery date more
         *            recent than the {@code periodEndFilter} date.
         * @param filterReadStatus
         *            Filters listings based on message read or unread status.
         * @param filterRecipient
         *            Filters listings based on recipient.
         * @param filterOriginator
         *            Filters listings based on VCARD N(name), TEL(telephone
         *            number) or EMAIL value.
         * @param filterPriority
         *            Filters listing based on message priority value.
         *            <p>
         *            Message listings will only be included when they satisfy
         *            all the filter tests ('AND' filtering). MaxListCount and
         *            ListStartOffset should only be applied after filtering.
         */
        public ListingInfo(short subjectLength, EnumSet<MessageListingParameter> listingParameters, EnumSet<MessageType> filterMessageType, Calendar filterPeriodBegin, Calendar filterPeriodEnd, FilterReadStatus filterReadStatus, String filterRecipient, String filterOriginator, FilterPriority filterPriority) {
            this.subjectLength = subjectLength;
            this.listingParameters = listingParameters;
            this.filterMessageType = filterMessageType;
            this.filterPeriodBegin = filterPeriodBegin;
            this.filterPeriodEnd = filterPeriodEnd;
            this.filterReadStatus = filterReadStatus;
            this.filterRecipient = filterRecipient;
            this.filterOriginator = filterOriginator;
            this.filterPriority = filterPriority;
        }
    }

    /**
     * Options information for a message listing size request.
     *
     * @see MessageAccessClientManager#getMessageListing
     */
    public static final class ListingSizeInfo {
        EnumSet<MessageType> filterMessageType;
        Calendar             filterPeriodBegin;
        Calendar             filterPeriodEnd;
        FilterReadStatus     filterReadStatus;
        String               filterRecipient;
        String               filterOriginator;
        FilterPriority       filterPriority;

        /**
         * @param filterMessageType
         *            Filters the listing according to message type.
         * @param filterPeriodBegin
         *            Filters out message listings with a delivery date older
         *            than the {@code periodBeginFilter} date.
         * @param filterPeriodEnd
         *            Filters out message listings with a delivery date more
         *            recent than the {@code periodEndFilter} date.
         * @param filterReadStatus
         *            Filters listings based on message read or unread status.
         * @param filterRecipient
         *            Filters listings based on recipient.
         * @param filterOriginator
         *            Filters listings based on VCARD N(name), TEL(telephone
         *            number) or EMAIL value.
         * @param filterPriority
         *            Filters listing based on message priority value.
         *            <p>
         *            Message listings will only be included when they satisfy
         *            all the filter tests ('AND' filtering). MaxListCount and
         *            ListStartOffset should only be applied after filtering.
         */
        public ListingSizeInfo(EnumSet<MessageType> filterMessageType, Calendar filterPeriodBegin, Calendar filterPeriodEnd, FilterReadStatus filterReadStatus, String filterRecipient, String filterOriginator, FilterPriority filterPriority) {
            this.filterMessageType = filterMessageType;
            this.filterPeriodBegin = filterPeriodBegin;
            this.filterPeriodEnd = filterPeriodEnd;
            this.filterReadStatus = filterReadStatus;
            this.filterRecipient = filterRecipient;
            this.filterOriginator = filterOriginator;
            this.filterPriority = filterPriority;
        }
    }

    /**
     *
     */
    public static final class MessageAccessServices {
        /**
         *
         */
        private final String serviceName;
        private final int    instanceID;
        private final int    serverPort;
        private final EnumSet<SupportedMessageType> supportedMessageTypes;

        /**
         * @param serviceName
         * @param instanceID
         * @param serverPort
         * @param supportedMessageTypes
         */
        public MessageAccessServices(String serviceName, int instanceID, int serverPort, EnumSet<SupportedMessageType> supportedMessageTypes) {
            this.serviceName           = serviceName;
            this.instanceID            = instanceID;
            this.serverPort            = serverPort;
            this.supportedMessageTypes = supportedMessageTypes.clone();
        }

        /**
         * @return Message Access Service Name
         */
        public String getServiceName() {
            return this.serviceName;
        }

        /**
         * @return Message Access Server Instance ID
         */
        public int getInstanceID() {
            return this.instanceID;
        }

        /**
         * @return Message Access Server Port
         */
        public int getServerPort() {
            return this.serverPort;
        }

        /**
         * @return Message Access Server Supported Message Types
         */
        public EnumSet<SupportedMessageType> getSupportedMessageTypes() {
            return this.supportedMessageTypes;
        }
    }

    private native static void initClassNative();

    private native void initObjectNative() throws ServerNotReachableException;

    private native void cleanupObjectNative();

    private native int disconnectNative(int connectionType, byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, int instanceID);

    private native String queryCurrentFolderNative(byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, int instanceID) throws BluetopiaPMException;

    private native int abortNative(int connectionType, byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, int instanceID);

    protected native int connectRemoteDeviceNative(int connectionClient, byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, int remoteServerPort, int instanceID, int connectionFlags, boolean waitForConnection);

    protected native int parseRemoteMessageAccessServicesNative(byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, String[][] ServiceNameArray, int[][] InstanceIDArray, int[][] ServerPortArray, long[][] SupportedMessageTypesArray) throws BluetopiaPMException;

    protected native int enableNotificationsNative(byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, int instanceID, boolean enable);

    protected native int setFolderNative(byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, int instanceID, int pathOption, String folderName);

    protected native int setFolderAbsoluteNative(byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, int instanceID, String path);

    protected native int getFolderListingNative(byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, int instanceID, int maxListCount, int listStartOffset);

    protected native int getFolderListingSizeNative(byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, int instanceID);

    protected native int getMessageListingNative(byte[] addressArray, int instanceID, String folderName, int maxListCount, int listStartOffset, int optionMask, short subjectlength, long parameterMask, short filterMessageType, int[] filterPeriodBeginArray, int[] filterPeriodEndArray, short filterReadStatus, String filterRecipient, String filterOriginator, short filterPriority);

    protected native int getMessageListingSizeNative(byte[] addressArray, int instanceID, String folderName, int optionMask, short filterMessageType, int[] filterPeriodBeginArray, int[] filterPeriodEndArray, short filterReadStatus, String filterRecipient, String filterOriginator, short filterPriority);

    protected native int getMessageNative(byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, int instanceID, String messageHandle, boolean attachment, int charSet, int fractionRequest);

    protected native int setMessageStatusNative(byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, int instanceID, String messageHandle, int statusIndicator, boolean statusValue);

    protected native int pushMessageNative(byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, int instanceID, String folderName, boolean transparent, boolean retry, int charSet, byte[] buffer, boolean isFinal);

    protected native int updateInboxNative(byte addr1, byte addr2, byte addr3, byte addr4, byte addr5, byte addr6, int instanceID);
}
