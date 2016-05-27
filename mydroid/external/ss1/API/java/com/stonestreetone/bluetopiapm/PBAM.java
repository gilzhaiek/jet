/*
 * PBAM.java Copyright 2010 - 2014 Stonestreet One, LLC. All Rights Reserved.
 */
package com.stonestreetone.bluetopiapm;

import java.util.EnumSet;

import com.stonestreetone.bluetopiapm.PBAM.PhonebookAccessClientManager;

/**
 * Java wrapper for Phone Book Access Profile Manager API for Stonestreet One
 * Bluetooth Protocol Stack Platform Manager.
 */
public class PBAM {

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
     * Private Constructor.
     *
     * @throws ServerNotReachableException
     */
    protected PBAM(int type, EventCallback eventCallback) throws ServerNotReachableException {

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

    /* Event Callback Handlers */

    protected void connectionStatusEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int connectionStatus) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    protected void disconnectedEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int reason) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    /*
     *  'data' may contain one, multiple, and/or partial vcards depending on the request and the contents
     *  of the vcards.
     */
    protected void vCardDataEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int status, boolean isFinal, int newMissedCalls, byte data[]) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    protected void vCardListingEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int status, boolean isFinal, int newMissedCalls, byte data[]) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    protected void phoneBookSizeEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int status, int size) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    protected void phoneBookSetEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int status, String currentPath) {
        throw new UnsupportedOperationException("Event received which is not supported by this Hands-Free Profile Manager type");
    }

    protected interface EventCallback {
    }

    /**
     * Manager for a single PBAP Client connection to a remote PBAP Server.
     * <p>
     * There can only be one outstanding PBAP profile request active at any one
     * time. Because of this, another PBAM request cannot be issued until either
     * the current request is aborted (by calling {@link #abort}) or the current
     * request is completed.
     */
    public static class PhonebookAccessClientManager extends PBAM {

        /**
         * Creates a new management connection to the local PBAP Client Manager.
         * Note that PBAP Client profile connections are not persistent.
         * Connections are tied to the Manager object that creates them so, when
         * the Manager object is disposed, the connection will be closed.
         *
         * @param eventCallback
         *            Receiver for events sent by the server.
         *
         * @throws ServerNotReachableException
         *             if the BluetopiaPM server cannot be reached.
         */
        public PhonebookAccessClientManager(ClientEventCallback eventCallback) throws ServerNotReachableException {
            super(MANAGER_TYPE_CLIENT, eventCallback);
        }

        /**
         * Connect to a remote PBAP Device.
         * <p>
         * If {@code waitForConnection} is {@code true}, this call will block
         * until the connection status is received (that is, the connection is
         * completed). If this parameter is not specified (NULL), then the
         * connection status will be returned asynchronously by way of a call to
         * {@link ClientEventCallback#connectionStatusEvent} on the
         * {@link ClientEventCallback} provided when this Manager was
         * constructed.
         *
         * @param remoteDeviceAddress
         *            Bluetooth address of the remote device.
         * @param remoteServerPort
         *            Port number of the Phonebook Access Profile Server on the
         *            remote device.
         * @param connectionFlags
         *            Set of flags which control whether encryption and/or
         *            authentication is used.
         * @param waitForConnection
         *            If {@code true}, this method will block until the
         *            connection attempt completes.
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error issuing the connection attempt. If
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

            return connectRemoteDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5], remoteServerPort, flags, waitForConnection);
        }

        /**
         * Closes an active connection to a remote device.
         * <p>
         * Connections are opened by a call to {@link #connectRemoteDevice}.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the device to disconnect.
         * @return Zero if successful, or a negative return error code if there
         *         was an error issuing the disconnection.
         *
         * @see ClientEventCallback#disconnectedEvent
         */
        public int disconnect(BluetoothAddress remoteDeviceAddress) {
            byte[] address = remoteDeviceAddress.internalByteArray();

            return disconnectNative(address[0], address[1], address[2], address[3], address[4], address[5]);
        }

        /**
         * Aborts any outstanding PBAM profile client request.
         * <p>
         * There can only be one outstanding PBAM request active at any one
         * time. Because of this, another PBAM request cannot be issued until
         * either the current request is aborted or the request is completed
         * (signified by a call to the {@link ClientEventCallback} that was
         * registered when the PBAM port was opened).
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the device with the current
         *            outstanding PBAM request to abort.
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error issuing the abort.
         */
        public int abort(BluetoothAddress remoteDeviceAddress) {
            byte[] address = remoteDeviceAddress.internalByteArray();

            return abortNative(address[0], address[1], address[2], address[3], address[4], address[5]);
        }


        /**
         * Generates a PBAP Pull PhoneBook Request to the server.
         * <p>
         * A successful return code indicates only that the request was
         * successfully passed to the Platform Manager service on the local
         * device. The final result of the request will be indicated by a call
         * to {@link ClientEventCallback#vCardDataEvent}. This event will
         * continue to be triggered until either {@link #abort} is called or all
         * requested vCard data has been transmitted (indicated when the event
         * handler's {@code final} parameter is {@code true}).
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @param objectName
         *            The absolute name/path of the PhoneBook being requested by
         *            this operation.
         * @param filter
         *            A set of filters that determine what information should be
         *            requested.
         * @param vCardFormat
         *            Specifies the vCard format requested in this Pull
         *            PhoneBook request. A {@code null} value will use the
         *            default format selected by the PBAP server.
         * @param maxListCount
         *            Specifies the maximum number of entries the client can
         *            handle. A value of 65535 means that the number of entries
         *            is not restricted.
         * @param listStartOffset
         *            If non-zero, the PBAP server will send resulting records
         *            beginning with this offset. This is useful in combination
         *            with {@code maxListCount} to make a request for the first
         *            N records followed by a second request for records N+1
         *            through M.
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error issuing the Pull PhoneBook request.
         */
        public int pullPhoneBook(BluetoothAddress remoteDeviceAddress, String objectName, EnumSet<Filter> filter, VCardFormat vCardFormat, int maxListCount, int listStartOffset) {
            int filterMask;
            int format;
            byte[] address = remoteDeviceAddress.internalByteArray();

            filterMask = 0;
            if(filter != null) {
                filterMask |= (filter.contains(Filter.VERSION)              ? (1 << FILTER_VERSION)              : 0);
                filterMask |= (filter.contains(Filter.FN)                   ? (1 << FILTER_FN)                   : 0);
                filterMask |= (filter.contains(Filter.N)                    ? (1 << FILTER_N)                    : 0);
                filterMask |= (filter.contains(Filter.PHOTO)                ? (1 << FILTER_PHOTO)                : 0);
                filterMask |= (filter.contains(Filter.BDAY)                 ? (1 << FILTER_BDAY)                 : 0);
                filterMask |= (filter.contains(Filter.ADR)                  ? (1 << FILTER_ADR)                  : 0);
                filterMask |= (filter.contains(Filter.LABEL)                ? (1 << FILTER_LABEL)                : 0);
                filterMask |= (filter.contains(Filter.TEL)                  ? (1 << FILTER_TEL)                  : 0);
                filterMask |= (filter.contains(Filter.EMAIL)                ? (1 << FILTER_EMAIL)                : 0);
                filterMask |= (filter.contains(Filter.MAILER)               ? (1 << FILTER_MAILER)               : 0);
                filterMask |= (filter.contains(Filter.TZ)                   ? (1 << FILTER_TZ)                   : 0);
                filterMask |= (filter.contains(Filter.GEO)                  ? (1 << FILTER_GEO)                  : 0);
                filterMask |= (filter.contains(Filter.TITLE)                ? (1 << FILTER_TITLE)                : 0);
                filterMask |= (filter.contains(Filter.ROLE)                 ? (1 << FILTER_ROLE)                 : 0);
                filterMask |= (filter.contains(Filter.LOGO)                 ? (1 << FILTER_LOGO)                 : 0);
                filterMask |= (filter.contains(Filter.AGENT)                ? (1 << FILTER_AGENT)                : 0);
                filterMask |= (filter.contains(Filter.ORG)                  ? (1 << FILTER_ORG)                  : 0);
                filterMask |= (filter.contains(Filter.NOTE)                 ? (1 << FILTER_NOTE)                 : 0);
                filterMask |= (filter.contains(Filter.REV)                  ? (1 << FILTER_REV)                  : 0);
                filterMask |= (filter.contains(Filter.SOUND)                ? (1 << FILTER_SOUND)                : 0);
                filterMask |= (filter.contains(Filter.URL)                  ? (1 << FILTER_URL)                  : 0);
                filterMask |= (filter.contains(Filter.UID)                  ? (1 << FILTER_UID)                  : 0);
                filterMask |= (filter.contains(Filter.KEY)                  ? (1 << FILTER_KEY)                  : 0);
                filterMask |= (filter.contains(Filter.NICKNAME)             ? (1 << FILTER_NICKNAME)             : 0);
                filterMask |= (filter.contains(Filter.CATEGORIES)           ? (1 << FILTER_CATEGORIES)           : 0);
                filterMask |= (filter.contains(Filter.PROID)                ? (1 << FILTER_PROID)                : 0);
                filterMask |= (filter.contains(Filter.CLASS)                ? (1 << FILTER_CLASS)                : 0);
                filterMask |= (filter.contains(Filter.SORT_STRING)          ? (1 << FILTER_SORT_STRING)          : 0);
                filterMask |= (filter.contains(Filter.X_IRMC_CALL_DATETIME) ? (1 << FILTER_X_IRMC_CALL_DATETIME) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_40) ? (1 << FILTER_PROPRIETARY_BIT_40) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_41) ? (1 << FILTER_PROPRIETARY_BIT_41) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_42) ? (1 << FILTER_PROPRIETARY_BIT_42) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_43) ? (1 << FILTER_PROPRIETARY_BIT_43) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_44) ? (1 << FILTER_PROPRIETARY_BIT_44) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_45) ? (1 << FILTER_PROPRIETARY_BIT_45) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_46) ? (1 << FILTER_PROPRIETARY_BIT_46) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_47) ? (1 << FILTER_PROPRIETARY_BIT_47) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_48) ? (1 << FILTER_PROPRIETARY_BIT_48) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_49) ? (1 << FILTER_PROPRIETARY_BIT_49) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_50) ? (1 << FILTER_PROPRIETARY_BIT_50) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_51) ? (1 << FILTER_PROPRIETARY_BIT_51) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_52) ? (1 << FILTER_PROPRIETARY_BIT_52) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_53) ? (1 << FILTER_PROPRIETARY_BIT_53) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_54) ? (1 << FILTER_PROPRIETARY_BIT_54) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_55) ? (1 << FILTER_PROPRIETARY_BIT_55) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_56) ? (1 << FILTER_PROPRIETARY_BIT_56) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_57) ? (1 << FILTER_PROPRIETARY_BIT_57) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_58) ? (1 << FILTER_PROPRIETARY_BIT_58) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_59) ? (1 << FILTER_PROPRIETARY_BIT_59) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_60) ? (1 << FILTER_PROPRIETARY_BIT_60) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_61) ? (1 << FILTER_PROPRIETARY_BIT_61) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_62) ? (1 << FILTER_PROPRIETARY_BIT_62) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_63) ? (1 << FILTER_PROPRIETARY_BIT_63) : 0);

                /*
                 * Check whether any of the proprietary filters were included.
                 * If so, we need to set the PROPRIETARY_FILTER bit to indicate
                 * that proprietary filters are in use.
                 */
                filter.retainAll(PROPRIETARY_FILTERS);
                if(filter.isEmpty() == false) {
                    filterMask |= FILTER_PROPRIETARY_FILTER;
                }
            }

            if(vCardFormat != null) {
                switch(vCardFormat) {
                case VCARD21:
                    format = VCARD_FORMAT_VCARD21;
                    break;
                case VCARD30:
                    format = VCARD_FORMAT_VCARD30;
                    break;
                default:
                    format = VCARD_FORMAT_DEFAULT;
                }
            } else {
                format = VCARD_FORMAT_DEFAULT;
            }

            return pullPhoneBookNative(address[0], address[1], address[2], address[3], address[4], address[5], objectName, filterMask, format, maxListCount, listStartOffset);
        }

        /**
         * Generates a PBAP Pull PhoneBook Size Request to the remote PBAP
         * server. This will act on the last directory selected with
         * {@link #setPhoneBook} or the default phonebook "telephone/pb.vcf" if
         * no directory has been selected.
         * <p>
         * A successful return code indicates only that the request was
         * successfully passed to the Platform Manager service on the local
         * device. The final result of the request will be indicated by a call
         * to {@link ClientEventCallback#phoneBookSizeEvent}.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device.
         * @return Zero if successful, or a negative return error code if there
         *         was an error issuing the Pull PhoneBook request.
         *
         * @see ClientEventCallback#phoneBookSizeEvent
         */
        public int pullPhoneBookSize(BluetoothAddress remoteDeviceAddress) {
            byte[] address = remoteDeviceAddress.internalByteArray();

            return pullPhoneBookSizeNative(address[0], address[1], address[2], address[3], address[4], address[5]);
        }

        /**
         * Generates a PBAP Set Phone Book request to the remote PBAP Server.
         * <p>
         * A successful return code indicates only that the request was
         * successfully passed to the Platform Manager service on the local
         * device. The final result of the request will be indicated by a call
         * to {@link ClientEventCallback#phoneBookSetEvent}.
         *
         * @param remoteDeviceAddress
         *            The Bluetooth address of the remote device
         * @param pathOption
         *            Indicates the type of path change to request
         * @param folderName
         *            Contains the folder name to include with this Set
         *            PhoneBook request. Can be {@code NULL} if no name is
         *            required.
         * @return Zero if successful, or a negative return error code if there
         *         was an error issuing the Pull PhoneBook request.
         *
         * @see ClientEventCallback#phoneBookSetEvent
         */
        public int setPhoneBook(BluetoothAddress remoteDeviceAddress, PathOption pathOption, String folderName) {
            int option     = 0;
            byte[] address = remoteDeviceAddress.internalByteArray();

            switch(pathOption) {
            case ROOT:
                option = PATH_OPTION_ROOT;
                break;
            case DOWN:
                option = PATH_OPTION_DOWN;
                break;
            case UP:
                option = PATH_OPTION_UP;
            }

            return setPhoneBookNative(address[0], address[1], address[2], address[3], address[4], address[5], option, folderName);
        }

        /**
         * Generates a PBAP Pull vCardListing request to the remote PBAP Server.
         * <p>
         * A successful return code indicates only that the request was
         * successfully passed to the Platform Manager service on the local
         * device. The final result of the request will be indicated by a call
         * to {@link ClientEventCallback#vCardListingEvent}.
         *
         * @param remoteDeviceAddress
         *              The Bluetooth address of the remote device.
         * @param phonebookPath
         *              The name of the phonebook to pull the vCard Listing from.
         * @param listOrder
         *              An enumerated type indicating how the listing should be ordered
         * @param searchAttribute
         *              An enumerated type indicating what attribute to use the SearchValue for.
         * @param searchValue
         *              The value to search the listing for. Can be {@code NULL} if none is
         *              required.
         * @param maxListCount
         *              The maximum amount of vCards to pull in the listing. A value of {@value 65535}
         *              is unlimited.
         * @param listStartOffset
         *              Indicated the point at which to start the listing.
         *
         * @return Zero if successful, or a negative return error code if there
         *         was an error issuing the Pull vCard Listing request.
         *
         * @see ClientEventCallback#vCardListingEvent
         */
        public int pullvCardListing(BluetoothAddress remoteDeviceAddress, String phonebookPath, ListOrder listOrder, SearchAttribute searchAttribute, String searchValue, int maxListCount, int listStartOffset) {
            int order = 0;
            int attribute = 0;
            byte[] address = remoteDeviceAddress.internalByteArray();

            switch(listOrder) {
            case INDEXED:
                order = LIST_ORDER_INDEXED;
                break;
            case ALPHABETICAL:
                order = LIST_ORDER_ALPHABETICAL;
                break;
            case PHONETICAL:
                order = LIST_ORDER_PHONETICAL;
                break;
            case DEFAULT:
                order = LIST_ORDER_DEFAULT;
            }

            switch(searchAttribute) {
            case NAME:
                attribute = SEARCH_ATTRIBUTE_NAME;
                break;
            case NUMBER:
                attribute = SEARCH_ATTRIBUTE_NUMBER;
                break;
            case SOUND:
                attribute = SEARCH_ATTRIBUTE_SOUND;
                break;
            case DEFAULT:
                attribute = SEARCH_ATTRIBUTE_DEFAULT;
            }

            return pullvCardListingNative(address[0], address[1], address[2], address[3], address[4], address[5], phonebookPath, order, attribute, searchValue, maxListCount, listStartOffset);
        }

        /**
         * Generates a PBAP Pull vCard Entry request to the remote PBAP Server.
         * <p>
         * A successful return code indicates only that the request was
         * successfully passed to the Platform Manager service on the local
         * device. The final result of the request will be indicated by a call
         * to {@link ClientEventCallback#vCardDataEvent}.
         *
         * @param remoteDeviceAddress
         *              The Bluetooth address of the remote device.
         * @param vCardName
         *              The name of the vCard object to be pulled.
         * @param filter
         *              A set of filters that determine what information should be
         *              requested.
         * @param vCardFormat
         *              Specifies the vCard format requested in this Pull
         *              PhoneBook request. A {@code null} value will use the
         *              default format selected by the PBAP server.
         * @return Zero if successful, or a negative return error code if there
         *         was an error issuing the Pull vCard request.
         *
         * @see ClientEventCallback#vCardDataEvent
         */
        public int pullvCard(BluetoothAddress remoteDeviceAddress, String vCardName, EnumSet<Filter> filter, VCardFormat vCardFormat) {
            int filterMask;
            int format;
            byte[] address = remoteDeviceAddress.internalByteArray();

            filterMask = 0;
            if(filter != null) {
                filterMask |= (filter.contains(Filter.VERSION)              ? (1 << FILTER_VERSION)              : 0);
                filterMask |= (filter.contains(Filter.FN)                   ? (1 << FILTER_FN)                   : 0);
                filterMask |= (filter.contains(Filter.N)                    ? (1 << FILTER_N)                    : 0);
                filterMask |= (filter.contains(Filter.PHOTO)                ? (1 << FILTER_PHOTO)                : 0);
                filterMask |= (filter.contains(Filter.BDAY)                 ? (1 << FILTER_BDAY)                 : 0);
                filterMask |= (filter.contains(Filter.ADR)                  ? (1 << FILTER_ADR)                  : 0);
                filterMask |= (filter.contains(Filter.LABEL)                ? (1 << FILTER_LABEL)                : 0);
                filterMask |= (filter.contains(Filter.TEL)                  ? (1 << FILTER_TEL)                  : 0);
                filterMask |= (filter.contains(Filter.EMAIL)                ? (1 << FILTER_EMAIL)                : 0);
                filterMask |= (filter.contains(Filter.MAILER)               ? (1 << FILTER_MAILER)               : 0);
                filterMask |= (filter.contains(Filter.TZ)                   ? (1 << FILTER_TZ)                   : 0);
                filterMask |= (filter.contains(Filter.GEO)                  ? (1 << FILTER_GEO)                  : 0);
                filterMask |= (filter.contains(Filter.TITLE)                ? (1 << FILTER_TITLE)                : 0);
                filterMask |= (filter.contains(Filter.ROLE)                 ? (1 << FILTER_ROLE)                 : 0);
                filterMask |= (filter.contains(Filter.LOGO)                 ? (1 << FILTER_LOGO)                 : 0);
                filterMask |= (filter.contains(Filter.AGENT)                ? (1 << FILTER_AGENT)                : 0);
                filterMask |= (filter.contains(Filter.ORG)                  ? (1 << FILTER_ORG)                  : 0);
                filterMask |= (filter.contains(Filter.NOTE)                 ? (1 << FILTER_NOTE)                 : 0);
                filterMask |= (filter.contains(Filter.REV)                  ? (1 << FILTER_REV)                  : 0);
                filterMask |= (filter.contains(Filter.SOUND)                ? (1 << FILTER_SOUND)                : 0);
                filterMask |= (filter.contains(Filter.URL)                  ? (1 << FILTER_URL)                  : 0);
                filterMask |= (filter.contains(Filter.UID)                  ? (1 << FILTER_UID)                  : 0);
                filterMask |= (filter.contains(Filter.KEY)                  ? (1 << FILTER_KEY)                  : 0);
                filterMask |= (filter.contains(Filter.NICKNAME)             ? (1 << FILTER_NICKNAME)             : 0);
                filterMask |= (filter.contains(Filter.CATEGORIES)           ? (1 << FILTER_CATEGORIES)           : 0);
                filterMask |= (filter.contains(Filter.PROID)                ? (1 << FILTER_PROID)                : 0);
                filterMask |= (filter.contains(Filter.CLASS)                ? (1 << FILTER_CLASS)                : 0);
                filterMask |= (filter.contains(Filter.SORT_STRING)          ? (1 << FILTER_SORT_STRING)          : 0);
                filterMask |= (filter.contains(Filter.X_IRMC_CALL_DATETIME) ? (1 << FILTER_X_IRMC_CALL_DATETIME) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_40) ? (1 << FILTER_PROPRIETARY_BIT_40) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_41) ? (1 << FILTER_PROPRIETARY_BIT_41) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_42) ? (1 << FILTER_PROPRIETARY_BIT_42) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_43) ? (1 << FILTER_PROPRIETARY_BIT_43) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_44) ? (1 << FILTER_PROPRIETARY_BIT_44) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_45) ? (1 << FILTER_PROPRIETARY_BIT_45) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_46) ? (1 << FILTER_PROPRIETARY_BIT_46) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_47) ? (1 << FILTER_PROPRIETARY_BIT_47) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_48) ? (1 << FILTER_PROPRIETARY_BIT_48) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_49) ? (1 << FILTER_PROPRIETARY_BIT_49) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_50) ? (1 << FILTER_PROPRIETARY_BIT_50) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_51) ? (1 << FILTER_PROPRIETARY_BIT_51) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_52) ? (1 << FILTER_PROPRIETARY_BIT_52) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_53) ? (1 << FILTER_PROPRIETARY_BIT_53) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_54) ? (1 << FILTER_PROPRIETARY_BIT_54) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_55) ? (1 << FILTER_PROPRIETARY_BIT_55) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_56) ? (1 << FILTER_PROPRIETARY_BIT_56) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_57) ? (1 << FILTER_PROPRIETARY_BIT_57) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_58) ? (1 << FILTER_PROPRIETARY_BIT_58) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_59) ? (1 << FILTER_PROPRIETARY_BIT_59) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_60) ? (1 << FILTER_PROPRIETARY_BIT_60) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_61) ? (1 << FILTER_PROPRIETARY_BIT_61) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_62) ? (1 << FILTER_PROPRIETARY_BIT_62) : 0);
                filterMask |= (filter.contains(Filter.PROPRIETARY_FILTER_63) ? (1 << FILTER_PROPRIETARY_BIT_63) : 0);
                /*
                 * Check whether any of the proprietary filters were included.
                 * If so, we need to set the PROPRIETARY_FILTER bit to indicate
                 * that proprietary filters are in use.
                 */
                filter.retainAll(PROPRIETARY_FILTERS);
                if(filter.isEmpty() == false) {
                    filterMask |= FILTER_PROPRIETARY_FILTER;
                }
            }

            if(vCardFormat != null) {
                switch(vCardFormat) {
                case VCARD21:
                    format = VCARD_FORMAT_VCARD21;
                    break;
                case VCARD30:
                    format = VCARD_FORMAT_VCARD30;
                    break;
                default:
                    format = VCARD_FORMAT_DEFAULT;
                }
            } else {
                format = VCARD_FORMAT_DEFAULT;
            }

            return pullvCardNative(address[0], address[1], address[2], address[3], address[4], address[5], vCardName, filterMask, format);
        }

        /**
         * @param remoteDeviceAddress
         * @param absolutePath
         * @return
         */
        public int setPhoneBookAbsolute(BluetoothAddress remoteDeviceAddress, String absolutePath) {
            byte[] address = remoteDeviceAddress.internalByteArray();

            return setPhoneBookAbsoluteNative(address[0], address[1], address[2], address[3], address[4], address[5], absolutePath);
        }

        @Override
        protected void connectionStatusEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int connectionStatus) {
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
            }

            if(callbackHandler != null) {
                try {
                    ((ClientEventCallback)callbackHandler).connectionStatusEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), status);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void disconnectedEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int reason) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((ClientEventCallback)callbackHandler).disconnectedEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), reason);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void vCardDataEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int status, boolean isFinal, int newMissedCalls, byte data[]) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((ClientEventCallback)callbackHandler).vCardDataEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), status, isFinal, newMissedCalls, data);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void vCardListingEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int status, boolean isFinal, int newMissedCalls, byte data[]) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((ClientEventCallback)callbackHandler).vCardListingEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), status, isFinal, newMissedCalls, data);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void phoneBookSizeEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int status, int size) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((ClientEventCallback)callbackHandler).phoneBookSizeEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), status, size);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void phoneBookSetEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int status, String currentPath) {
            EventCallback callbackHandler = this.callbackHandler;

            if(callbackHandler != null) {
                try {
                    ((ClientEventCallback)callbackHandler).phoneBookSetEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), status, currentPath);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         *
         * Public interface for handling PBAM Client Events.
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
        public interface ClientEventCallback extends EventCallback {

            /**
             * Invoked when an outgoing connection attempt completes.
             *
             * @param remoteDeviceAddress
             *            Bluetooth address of the remote device.
             * @param connectionStatus
             *            Indication of success or reason for failure.
             *
             * @see PhonebookAccessClientManager#connectRemoteDevice
             */
            public void connectionStatusEvent(BluetoothAddress remoteDeviceAddress, ConnectionStatus connectionStatus);

            /**
             * Invoked when an established connection is disconnected.
             *
             * @param remoteDeviceAddress
             *            Bluetooth address of the remote device.
             * @param reason
             *            Reason for the disconnection.
             *
             * @see PhonebookAccessClientManager#disconnect
             */
            public void disconnectedEvent(BluetoothAddress remoteDeviceAddress, int reason);

            /**
             * Invoked when vCard Data is received from the remote PBAP Server.
             *
             * @param remoteDeviceAddress
             *            Bluetooth address of the remote device.
             * @param status
             *            Status of the Pull PhoneBook/vCard request.
             * @param isFinal
             *            Indicates whether this event is the final data from
             *            the request.
             * @param newMissedCalls
             *            Number of new missed calls.
             * @param data
             *            Raw vCard data. This may contain one, multiple, and/or
             *            partial vCards depending on the type of the request
             *            and the contents of the resulting vCards.
             *
             * @see PhonebookAccessClientManager#pullPhoneBook
             */
            public void vCardDataEvent(BluetoothAddress remoteDeviceAddress, int status, boolean isFinal, int newMissedCalls, byte data[]);

            /**
             * Invoked when a Pull vCard Listing returns a set of data.
             *
             * @param remoteDeviceAddress
             *            Bluetooth address of the remote device.
             * @param status
             *            Status of the Pull vCardListing request.
             * @param isFinal
             *            Indicates whether this event is the final data from
             *            the request.
             * @param newMissedCalls
             *            Number of new missed calls.
             * @param data
             *            Raw vCard Listing data. This may contain one, multiple, and/or
             *            partial listings.
             */
            public void vCardListingEvent(BluetoothAddress remoteDeviceAddress, int status, boolean isFinal, int newMissedCalls, byte data[]);

            /**
             * Invoked when a Pull PhoneBook Size request is completed.
             *
             * @param remoteDeviceAddress
             *            Bluetooth address of the remote device
             * @param status
             *            Status of the Pull PhoneBook Size request
             * @param size
             *            Size returned by successful request
             *
             * @see PhonebookAccessClientManager#pullPhoneBookSize
             */
            public void phoneBookSizeEvent(BluetoothAddress remoteDeviceAddress, int status, int size);

            /**
             * Invoked when a Set PhoneBook request is completed.
             *
             * @param remoteDeviceAddress
             *            Bluetooth address of the remote device
             * @param status
             *            Status of the Set PhoneBook request
             * @param currentPath
             *            The current working directory upon completion of the last command
             *
             * @see PhonebookAccessClientManager#setPhoneBook
             */
            public void phoneBookSetEvent(BluetoothAddress remoteDeviceAddress, int status, String currentPath);
        }
    }

    public static class PhonebookAccessServerManager extends PBAM {

        public PhonebookAccessServerManager(ServerEventCallback eventCallback) throws ServerNotReachableException {
            super(MANAGER_TYPE_SERVER, eventCallback);

            throw new UnsupportedOperationException();
        }

        public interface ServerEventCallback extends EventCallback {
            // TODO PBAP server events
        }
    }

    private final static int CONNECTION_STATUS_SUCCESS                  = 0;
    private final static int CONNECTION_STATUS_FAILURE_TIMEOUT          = 1;
    private final static int CONNECTION_STATUS_FAILURE_REFUSED          = 2;
    private final static int CONNECTION_STATUS_FAILURE_SECURITY         = 3;
    private final static int CONNECTION_STATUS_FAILURE_DEVICE_POWER_OFF = 4;
    private final static int CONNECTION_STATUS_FAILURE_UNKNOWN          = 5;

    /*
     * NOTE:
     * 		Static constant duplicates of enum types are used for translation to JNI.
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

    /* The following defines specify the bit locations for the 64-bit    */
    /* filter field specified by the PBAP specification. Each of these   */
    /* bits can be OR'ed together to form a filter mask that should be   */
    /* passed in the FilterLow parameter where a filter is required.     */
    /* The FilterHigh portion contains proprietary filter settings, if   */
    /* these are not used it should be set to zero. Refer to the PBAP    */
    /* specification for more information.                               */
    private static final int FILTER_VERSION              = 0;
    private static final int FILTER_FN                   = 1;
    private static final int FILTER_N                    = 2;
    private static final int FILTER_PHOTO                = 3;
    private static final int FILTER_BDAY                 = 4;
    private static final int FILTER_ADR                  = 5;
    private static final int FILTER_LABEL                = 6;
    private static final int FILTER_TEL                  = 7;
    private static final int FILTER_EMAIL                = 8;
    private static final int FILTER_MAILER               = 9;
    private static final int FILTER_TZ                   = 10;
    private static final int FILTER_GEO                  = 11;
    private static final int FILTER_TITLE                = 12;
    private static final int FILTER_ROLE                 = 13;
    private static final int FILTER_LOGO                 = 14;
    private static final int FILTER_AGENT                = 15;
    private static final int FILTER_ORG                  = 16;
    private static final int FILTER_NOTE                 = 17;
    private static final int FILTER_REV                  = 18;
    private static final int FILTER_SOUND                = 19;
    private static final int FILTER_URL                  = 20;
    private static final int FILTER_UID                  = 21;
    private static final int FILTER_KEY                  = 22;
    private static final int FILTER_NICKNAME             = 23;
    private static final int FILTER_CATEGORIES           = 24;
    private static final int FILTER_PROID                = 25;
    private static final int FILTER_CLASS                = 26;
    private static final int FILTER_SORT_STRING          = 27;
    private static final int FILTER_X_IRMC_CALL_DATETIME = 28;
    private static final int FILTER_PROPRIETARY_FILTER   = 39;
    private static final int FILTER_PROPRIETARY_BIT_40   = 40;
    private static final int FILTER_PROPRIETARY_BIT_41   = 41;
    private static final int FILTER_PROPRIETARY_BIT_42   = 42;
    private static final int FILTER_PROPRIETARY_BIT_43   = 43;
    private static final int FILTER_PROPRIETARY_BIT_44   = 44;
    private static final int FILTER_PROPRIETARY_BIT_45   = 45;
    private static final int FILTER_PROPRIETARY_BIT_46   = 46;
    private static final int FILTER_PROPRIETARY_BIT_47   = 47;
    private static final int FILTER_PROPRIETARY_BIT_48   = 48;
    private static final int FILTER_PROPRIETARY_BIT_49   = 49;
    private static final int FILTER_PROPRIETARY_BIT_50   = 50;
    private static final int FILTER_PROPRIETARY_BIT_51   = 51;
    private static final int FILTER_PROPRIETARY_BIT_52   = 52;
    private static final int FILTER_PROPRIETARY_BIT_53   = 53;
    private static final int FILTER_PROPRIETARY_BIT_54   = 54;
    private static final int FILTER_PROPRIETARY_BIT_55   = 55;
    private static final int FILTER_PROPRIETARY_BIT_56   = 56;
    private static final int FILTER_PROPRIETARY_BIT_57   = 57;
    private static final int FILTER_PROPRIETARY_BIT_58   = 58;
    private static final int FILTER_PROPRIETARY_BIT_59   = 59;
    private static final int FILTER_PROPRIETARY_BIT_60   = 60;
    private static final int FILTER_PROPRIETARY_BIT_61   = 61;
    private static final int FILTER_PROPRIETARY_BIT_62   = 62;
    private static final int FILTER_PROPRIETARY_BIT_63   = 63;

    /**
     * Filters for selecting specific vCard fields to query.
     */
    public enum Filter {
        /**
         * vCard version
         */
        VERSION,

        /**
         * Formatted Name
         */
        FN,

        /**
         * Name
         */
        N,

        /**
         * Associated Image Photo
         */
        PHOTO,

        /**
         * Birthday
         */
        BDAY,

        /**
         * Delivery Address
         */
        ADR,

        /**
         * Delivery
         */
        LABEL,

        /**
         * Telephone Number
         */
        TEL,

        /**
         * Electronic Mail Address
         */
        EMAIL,

        /**
         * Electronic Mail
         */
        MAILER,

        /**
         * Time Zone
         */
        TZ,

        /**
         * Geographic Position
         */
        GEO,

        /**
         * Job
         */
        TITLE,

        /**
         * Role within the Organization
         */
        ROLE,

        /**
         * Organization Logo
         */
        LOGO,

        /**
         * vCard of Person Representing
         */
        AGENT,

        /**
         * Name of Organization
         */
        ORG,

        /**
         * Comments
         */
        NOTE,

        /**
         * Revision
         */
        REV,

        /**
         * Pronunciation of Name
         */
        SOUND,

        /**
         * Uniform Resource Locator
         */
        URL,

        /**
         * Unique ID
         */
        UID,

        /**
         * Public Encryption Key
         */
        KEY,

        /**
         * Nickname
         */
        NICKNAME,

        /**
         * Categories
         */
        CATEGORIES,

        /**
         * Product ID
         */
        PROID,

        /**
         * Class information
         */
        CLASS,

        /**
         * String used for sorting
         */
        SORT_STRING,

        /**
         * Time stamp
         */
        X_IRMC_CALL_DATETIME,

        /**
         * Vendor-specific filter (Filter mask bit 40)
         */
        PROPRIETARY_FILTER_40,

        /**
         * Vendor-specific filter (Filter mask bit 41)
         */
        PROPRIETARY_FILTER_41,

        /**
         * Vendor-specific filter (Filter mask bit 42)
         */
        PROPRIETARY_FILTER_42,

        /**
         * Vendor-specific filter (Filter mask bit 43)
         */
        PROPRIETARY_FILTER_43,

        /**
         * Vendor-specific filter (Filter mask bit 44)
         */
        PROPRIETARY_FILTER_44,

        /**
         * Vendor-specific filter (Filter mask bit 45)
         */
        PROPRIETARY_FILTER_45,

        /**
         * Vendor-specific filter (Filter mask bit 46)
         */
        PROPRIETARY_FILTER_46,

        /**
         * Vendor-specific filter (Filter mask bit 47)
         */
        PROPRIETARY_FILTER_47,

        /**
         * Vendor-specific filter (Filter mask bit 48)
         */
        PROPRIETARY_FILTER_48,

        /**
         * Vendor-specific filter (Filter mask bit 49)
         */
        PROPRIETARY_FILTER_49,

        /**
         * Vendor-specific filter (Filter mask bit 50)
         */
        PROPRIETARY_FILTER_50,

        /**
         * Vendor-specific filter (Filter mask bit 51)
         */
        PROPRIETARY_FILTER_51,

        /**
         * Vendor-specific filter (Filter mask bit 52)
         */
        PROPRIETARY_FILTER_52,

        /**
         * Vendor-specific filter (Filter mask bit 53)
         */
        PROPRIETARY_FILTER_53,

        /**
         * Vendor-specific filter (Filter mask bit 54)
         */
        PROPRIETARY_FILTER_54,

        /**
         * Vendor-specific filter (Filter mask bit 55)
         */
        PROPRIETARY_FILTER_55,

        /**
         * Vendor-specific filter (Filter mask bit 56)
         */
        PROPRIETARY_FILTER_56,

        /**
         * Vendor-specific filter (Filter mask bit 57)
         */
        PROPRIETARY_FILTER_57,

        /**
         * Vendor-specific filter (Filter mask bit 58)
         */
        PROPRIETARY_FILTER_58,

        /**
         * Vendor-specific filter (Filter mask bit 59)
         */
        PROPRIETARY_FILTER_59,

        /**
         * Vendor-specific filter (Filter mask bit 60)
         */
        PROPRIETARY_FILTER_60,

        /**
         * Vendor-specific filter (Filter mask bit 61)
         */
        PROPRIETARY_FILTER_61,

        /**
         * Vendor-specific filter (Filter mask bit 62)
         */
        PROPRIETARY_FILTER_62,

        /**
         * Vendor-specific filter (Filter mask bit 63)
         */
        PROPRIETARY_FILTER_63,
    }

    // TODO Make these filter sets public. We need an immutable EnumSet implementation to do this.

    /**
     * Minimum required set of filters which a PBAP server must support for vCard version 2.1.
     */
    /*pkg*/ static final EnumSet<Filter> MINIMUM_FILTER_VCARD21 = EnumSet.of(Filter.VERSION, Filter.N, Filter.TEL);

    /**
     * Minimum required set of filters which a PBAP server must support for vCard version 3.0.
     */
    /*pkg*/ static final EnumSet<Filter> MINIMUM_FILTER_VCARD30 = EnumSet.of(Filter.VERSION, Filter.N, Filter.FN, Filter.TEL);

    /*
     * Proprietary filters
     */
    /*pkg*/ static final EnumSet<Filter> PROPRIETARY_FILTERS = EnumSet.range(Filter.PROPRIETARY_FILTER_40, Filter.PROPRIETARY_FILTER_63);

    /* The following are the valid operations that  */
    /* can be performed using the PBAM_SetPhonebook operation. This      */
    /* corresponds to flag settings for the OBEX SetPath command.         */
    private static final int PATH_OPTION_ROOT = 0;
    private static final int PATH_OPTION_DOWN = 1;
    private static final int PATH_OPTION_UP   = 2;

    /**
     * Specifies the type of navigation for directory change requests.
     *
     * @see PhonebookAccessClientManager#setPhoneBook
     */
    public enum PathOption
    {
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

    /* The following are the format values that   */
    /* can be used when pulling vCards from a remote PBAP server.        */
    private static final int VCARD_FORMAT_VCARD21 = 0;
    private static final int VCARD_FORMAT_VCARD30 = 1;
    private static final int VCARD_FORMAT_DEFAULT = 2;

    /**
     * Specifies the format of the requested or supplied vCard data.
     *
     * @see PhonebookAccessClientManager#pullPhoneBook
     * @see PhonebookAccessClientManager.ClientEventCallback#vCardDataEvent
     */
    public enum VCardFormat
    {
        /**
         * vCard version 2.1
         */
        VCARD21,

        /**
         * vCard version 3.0
         */
        VCARD30
    }

    private static final int LIST_ORDER_INDEXED = 0;
    private static final int LIST_ORDER_ALPHABETICAL = 1;
    private static final int LIST_ORDER_PHONETICAL = 2;
    private static final int LIST_ORDER_DEFAULT = 3;

    /**
     * Specifies the order in which a vCard Listing should be returned.
     *
     * @see PhonebookAccessClientManager#pullvCardListing
     */
    public enum ListOrder
    {
        /**
         * Indexed Order
         */
        INDEXED,

        /**
         * Alphabetical Order
         */
        ALPHABETICAL,

        /**
         * Phonetical Order
         */
        PHONETICAL,

        /**
         * Default (unspecified) Order
         */
        DEFAULT

    }

    private static final int SEARCH_ATTRIBUTE_NAME = 0;
    private static final int SEARCH_ATTRIBUTE_NUMBER = 1;
    private static final int SEARCH_ATTRIBUTE_SOUND = 2;
    private static final int SEARCH_ATTRIBUTE_DEFAULT = 3;

    /**
     * Specifies the Attribute to be searched for in a vCard Listing.
     *
     * @see PhonebookAccessClientManager#pullvCardListing
     */
    public enum SearchAttribute
    {
        /**
         * Search by Name
         */
        NAME,
        /**
         * Search by Number
         */
        NUMBER,
        /**
         * Search by Sound
         */
        SOUND,
        /**
         * Default (unspecified) Attribute
         */
        DEFAULT
    }

    /* Native method declarations */

    private native static void initClassNative();

    private native void initObjectNative() throws ServerNotReachableException;

    private native void cleanupObjectNative();

    protected native int connectRemoteDeviceNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int remoteServerPort, int connectionFlags, boolean waitForConnection);
    protected native int disconnectNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);
    protected native int abortNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);
    protected native int pullPhoneBookNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String objectName, long filter, int vCardFormat, int maxListCount, int listStartOffset);
    protected native int pullPhoneBookSizeNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);
    protected native int setPhoneBookNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int pathOption, String folderName);
    protected native int pullvCardListingNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String phonebookPath, int listOrder, int searchAttribute, String searchValue, int maxListCount, int listStartOffset);
    protected native int pullvCardNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String vCardName, long filter, int vCardFormat);
    protected native int setPhoneBookAbsoluteNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String absolutePath);
}
