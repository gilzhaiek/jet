/*
 * DEVM.java Copyright 2010 - 2014 Stonestreet One, LLC. All Rights Reserved.
 */
package com.stonestreetone.bluetopiapm;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Java wrapper for the Device Manager API for the Stonestreet One Bluetooth
 * Protocol Stack.
 */
public class DEVM {

    protected final EventCallback          callbackHandler;
    protected final AuthenticationCallback authHandler;

    protected boolean                      disposed;

    private long                           localData;

    static {
        System.loadLibrary("btpmj");
        initClassNative();
    }

    /**
     * Specifies that the function should block indefinitely until queued data
     * has been received or sent.
     */
    public static final int                TIMEOUT_INFINITE = 0;

    /**
     * Creates a new connection to the local Device Manager.
     *
     * @param eventCallback
     *            Receiver for events sent by the DEVM service.
     *
     * @throws ServerNotReachableException
     *             if the BluetopiaPM server cannot be reached.
     */
    public DEVM(EventCallback eventCallback) throws ServerNotReachableException {

        try {

            initObjectNative(false);
            disposed        = false;
            callbackHandler = eventCallback;
            authHandler     = null;

        } catch(ServerNotReachableException exception) {

            dispose();
            throw exception;
        }
    }

    /**
     * Creates a new connection to the local Device Manager. If an
     * {@link AuthenticationCallback} is provided, this Manager object will
     * register itself as the handler for authentication requests from the
     * Device Manager service. There can be only one registered authentication
     * handler in the system at any given time.
     *
     * @param eventCallback
     *            Receiver for events sent by the DEVM service.
     * @param authenticationCallback
     *            Receiver for authentication events sent by the DEVM service.
     *
     * @throws ServerNotReachableException
     *             if the BluetopiaPM server cannot be reached.
     * @throws BluetopiaPMException
     */
    public DEVM(EventCallback eventCallback, AuthenticationCallback authenticationCallback) throws ServerNotReachableException, BluetopiaPMException {

        if(authenticationCallback != null) {

            try {

                initObjectNative(true);
                authHandler = authenticationCallback;

            } catch(ServerNotReachableException exception) {

                dispose();
                throw exception;
            }

        } else {

            try {

                initObjectNative(false);
                authHandler = null;

            } catch(ServerNotReachableException exception) {

                dispose();
                throw exception;
            }
        }

        disposed        = false;
        callbackHandler = eventCallback;
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
     * Retrieves the human-readable string value for the specified device
     * manufacturer.
     *
     * @param manufacturerID
     *            The manufacturer ID.
     * @return String value corresponding to the given manufacturer ID.
     */
    public static String convertManufacturerIDToString(int manufacturerID) {
        return convertManufacturerNameToStringNative(manufacturerID);
    }

//    /* The following function is a utility function that exists to parse */
//    /* the specified Raw SDP Data Stream into the Bluetopia SDP API      */
//    /* (Parsed) format.  This function accepts as input the length of the*/
//    /* Raw stream (must be greater than zero), followed by a pointer to  */
//    /* the actual Raw SDP Stream.  The final parameter is a pointer to a */
//    /* buffer that will contain the header information for the parsed    */
//    /* data.  This function returns zero if successful or a negative     */
//    /* value if an error occurred.                                       */
//    /* * NOTE * If this function is successful the final parameter *MUST**/
//    /*          be passed to the DEVM_FreeParsedSDPData() to free any    */
//    /*          allocated resources that were allocated to track the     */
//    /*          Parsed SDP Stream.                                       */
//    /* * NOTE * The Raw SDP Stream Buffer (second parameter) *MUST*      */
//    /*          remain active while the data is processed as well as even*/
//    /*          during the call to the DEVM_FreeParsedSDPData() function.*/
//    public static DEVM_Parsed_SDP_Data_t convertRawSDPStreamToParsedSDPData(byte[] rawSDPData) {
//        //FIXME
//    }
//
//    /* The following function is a utility function that exists to       */
//    /* convert the specified parsed SDP Data Information to the internal */
//    /* Raw SDP Stream format that is used to store the representation.   */
//    /* This function accepts as input, the Parsed SDP Data to convert    */
//    /* (first parameter), followed by an optional buffer to build the    */
//    /* stream into.  If the buffer is not specified, then this function  */
//    /* can be used to determine the size (in bytes) that will be required*/
//    /* to hold the converted stream.  If a buffer is specified (last two */
//    /* parameters are not zero and NULL (respectively) then the buffer is*/
//    /* required to be large enough to hold the data.  This function      */
//    /* returns a positive value if the data was able to be converted (and*/
//    /* possibly written to a given buffer), or a negative error code if  */
//    /* there was an error.                                               */
//    public static byte[] convertParsedSDPDataToRawSDPStream(DEVM_Parsed_SDP_Data_t *parsedSDPData) {
//        //FIXME
//    }
//
//    /* The following function is a utility function that exists to parse */
//    /* the specified Raw Bluetooth Low Energy (BLE) Data Stream into the */
//    /* Bluetopia PM API (Parsed) format.  This function accepts as input */
//    /* the length of the Raw stream (must be greater than zero), followed*/
//    /* by a pointer to the actual Raw Services Stream.  The final        */
//    /* parameter is a pointer to a buffer that will contain the header   */
//    /* information for the parsed data.  This function returns zero if   */
//    /* successful or a negative value if an error occurred.              */
//    /* * NOTE * If this function is successful the final parameter *MUST**/
//    /*          be passed to the DEVM_FreeParsedServicesData() to free   */
//    /*          any allocated resources that were allocated to track the */
//    /*          Parsed Services Stream.                                  */
//    /* * NOTE * The Raw Services Stream Buffer (second parameter) *MUST* */
//    /*          remain active while the data is processed as well as even*/
//    /*          during the call to the DEVM_FreeParsedServicesData()     */
//    /*          function.                                                */
//    public static DEVM_Parsed_Services_Data_t convertRawServicesStreamToParsedServicesData(byte rawServicesData[]);
//
//    /* The following function is a utility function that exists to       */
//    /* convert the specified parsed Service Data Information to the      */
//    /* internal Raw Service Stream format that is used to store the      */
//    /* representation.  This function accepts as input, the Parsed       */
//    /* Service Data to convert (first parameter), followed by an optional*/
//    /* buffer to build the stream into.  If the buffer is not specified, */
//    /* then this function can be used to determine the size (in bytes)   */
//    /* that will be required to hold the converted stream.  If a buffer  */
//    /* is specified (last two parameters are not zero and NULL           */
//    /* (respectively) then the buffer is required to be large enough to  */
//    /* hold the data.  This function returns a positive value if the data*/
//    /* was able to be converted (and possibly written to a given buffer),*/
//    /* or a negative error code if there was an error.                   */
//    public static byte[] convertParsedServicesDataToRawServicesStream(DEVM_Parsed_Services_Data_t parsedServiceData);

    /**
     * Used to lock the Device Manager (required to prevent simultaneous thread
     * access from corrupting internal resources.)
     *
     * @return Boolean value indicating whether the lock was successfully
     *         acquired.
     * @see DEVM#releaseLock()
     */
    public boolean acquireLock() {
        return acquireLockNative();
    }

    /**
     * Releases a lock which was previously acquired using
     * {@link DEVM#acquireLock()}.
     */
    public void releaseLock() {
        releaseLockNative();
    }

    /**
     * Instructs the Device Manager to power on the local device (i.e., open the
     * device.)
     * <p>
     * NOTE: It is not recommended to use this function within the Android OS.
     *       Intead, you should use BluetoothAdapter.enable() from the Android framework.
     * @return Zero if successful, or a negative return error code.
     * @see DEVM#powerOffDevice()
     */
    public int powerOnDevice() {
        return powerOnDeviceNative();
    }

    /**
     * Instructs the Device Manager to power off the local device (i.e., close
     * the device.)
     *
     * <p>
     * NOTE: It is not recommended to use this function within the Android OS.
     *       Intead, you should use BluetoothAdapter.disable() from the Android framework.
     * @return Zero if successful, or a negative return error code.
     * @see DEVM#powerOnDevice()
     */
    public int powerOffDevice() {
        return powerOffDeviceNative();
    }

    /**
     * Determines the current power state of the local device.
     *
     * @return <b>0</b> if the device is powered off,<br>
     *         <b>1</b> if the device is powered on,<br>
     *         or a negative return error code.
     */
    public int queryDevicePowerState() {
        return queryDevicePowerStateNative();
    }

    /**
     * Allows local modules that have registered callbacks to inform the local
     * device manager that they have received the
     * {@link DEVM#devicePoweringOffEvent} and have completed their necessary
     * cleanup.
     * <p>
     * <i>Note:</i> When the Device Manager is powering down the local device it
     * will dispatch a {@link DEVM#devicePoweringOffEvent} to all registered
     * event callbacks. All event callbacks that have received this event
     * <i>must</i> call this function to acknowledge the power down event
     * (usually after any cleanup that might be needed). This mechanism allows
     * all modules to attempt to clean up any resources (e.g. close an active
     * connection) before the device is actually powered off.
     * <p>
     * If a registered event callback does <i>not</i> acknowledge the powering
     * down event (i.e. does <i>not</i> call this function), the device will
     * still be shutdown after a timeout has elapsed. Using this mechanism
     * allows the timeout to be cut short because no modules will be using the
     * active connections any longer.
     */
    public void acknowledgeDevicePoweringDown() {
        acknowledgeDevicePoweringDownNative();
    }

    /**
     * Determines the current properties of the local device.
     *
     * @return A {@link LocalDeviceProperties} class indicating the current
     *         properties of the local device, or {@code null} if there was an
     *         error.
     */
    public LocalDeviceProperties queryLocalDeviceProperties() {
        int[]                 properties = new int[13];
        byte[]                address    = new byte[6];
        String[]              name       = new String[1];
        LocalDeviceProperties devProps;

        if(queryLocalDevicePropertiesNative(address, name, properties) == 0) {
            devProps = new LocalDeviceProperties();

            devProps.deviceAddress           = new BluetoothAddress(address);
            devProps.classOfDevice           = properties[NATIVE_LOCAL_PROP_INDEX_CLASS_OF_DEVICE];
            devProps.deviceName              = name[0];
            devProps.hciVersion              = properties[NATIVE_LOCAL_PROP_INDEX_HCI_VERSION];
            devProps.hciRevision             = properties[NATIVE_LOCAL_PROP_INDEX_HCI_REVISION];
            devProps.lmpVersion              = properties[NATIVE_LOCAL_PROP_INDEX_LMP_VERSION];
            devProps.lmpSubversion           = properties[NATIVE_LOCAL_PROP_INDEX_LMP_SUBVERSION];
            devProps.deviceManufacturer      = properties[NATIVE_LOCAL_PROP_INDEX_DEVICE_MANUFACTURER];
            devProps.discoverableMode        = (properties[NATIVE_LOCAL_PROP_INDEX_DISCOVERABLE_MODE] != 0);
            devProps.discoverableModeTimeout = properties[NATIVE_LOCAL_PROP_INDEX_DISCOVERABLE_MODE_TIMEOUT];
            devProps.connectableMode         = (properties[NATIVE_LOCAL_PROP_INDEX_CONNECTABLE_MODE] != 0);
            devProps.connectableModeTimeout  = properties[NATIVE_LOCAL_PROP_INDEX_CONNECTABLE_MODE_TIMEOUT];
            devProps.pairableMode            = (properties[NATIVE_LOCAL_PROP_INDEX_PAIRABLE_MODE] != 0);
            devProps.pairableModeTimeout     = properties[NATIVE_LOCAL_PROP_INDEX_PAIRABLE_MODE_TIMEOUT];

            devProps.localDeviceFlags        = EnumSet.noneOf(LocalDeviceFlags.class);
            if((properties[NATIVE_LOCAL_PROP_INDEX_LOCAL_DEVICE_FLAGS] & LOCAL_DEVICE_FLAGS_DEVICE_DISCOVERY_IN_PROGRESS) == LOCAL_DEVICE_FLAGS_DEVICE_DISCOVERY_IN_PROGRESS)
                devProps.localDeviceFlags.add(LocalDeviceFlags.DEVICE_DISCOVERY_IN_PROGRESS);
            if((properties[NATIVE_LOCAL_PROP_INDEX_LOCAL_DEVICE_FLAGS] & LOCAL_DEVICE_FLAGS_LE_SCANNING_IN_PROGRESS) == LOCAL_DEVICE_FLAGS_LE_SCANNING_IN_PROGRESS)
                devProps.localDeviceFlags.add(LocalDeviceFlags.LE_SCANNING_IN_PROGRESS);
            if((properties[NATIVE_LOCAL_PROP_INDEX_LOCAL_DEVICE_FLAGS] & LOCAL_DEVICE_FLAGS_LE_ADVERTISING_IN_PROGRESS) == LOCAL_DEVICE_FLAGS_LE_ADVERTISING_IN_PROGRESS)
                devProps.localDeviceFlags.add(LocalDeviceFlags.LE_ADVERTISING_IN_PROGRESS);
            if((properties[NATIVE_LOCAL_PROP_INDEX_LOCAL_DEVICE_FLAGS] & LOCAL_DEVICE_FLAGS_LE_ROLE_IS_CURRENTLY_SLAVE) == LOCAL_DEVICE_FLAGS_LE_ROLE_IS_CURRENTLY_SLAVE)
                devProps.localDeviceFlags.add(LocalDeviceFlags.LE_ROLE_IS_CURRENTLY_SLAVE);
            if((properties[NATIVE_LOCAL_PROP_INDEX_LOCAL_DEVICE_FLAGS] & LOCAL_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY) == LOCAL_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY)
                devProps.localDeviceFlags.add(LocalDeviceFlags.DEVICE_SUPPORTS_LOW_ENERGY);
            if((properties[NATIVE_LOCAL_PROP_INDEX_LOCAL_DEVICE_FLAGS] & LOCAL_DEVICE_FLAGS_DEVICE_SUPPORTS_ANT_PLUS) == LOCAL_DEVICE_FLAGS_DEVICE_SUPPORTS_ANT_PLUS)
                devProps.localDeviceFlags.add(LocalDeviceFlags.DEVICE_SUPPORTS_ANT_PLUS);

        } else {
            devProps = null;
        }

        return devProps;
    }

    /**
     * Updates the class of the local device.
     *
     * @param classOfDevice
     *            The new class of device.
     * @return Zero if successful, or a negative return error code.
     */
    public int updateClassOfDevice(int classOfDevice) {
        return updateClassOfDeviceNative(classOfDevice);
    }

    /**
     * Updates the name of the local device.
     *
     * @param deviceName
     *            The new local device name.
     * @return Zero if successful, or a negative return error code.
     */
    public int updateDeviceName(String deviceName) {
        return updateDeviceNameNative(deviceName);
    }

    /**
     * Updates the discoverability of the local device.
     *
     * @param discoverable
     *            Whether the local device is discoverable.
     * @param timeout
     *            The length of time after which discoverable mode will timeout
     *            (specified in milliseconds).
     * @return Zero if successful, or a negative return error code.
     */
    public int updateDiscoverableMode(boolean discoverable, int timeout) {
        return updateDiscoverableModeNative(discoverable, timeout);
    }

    /**
     * Updates the connectability of the local device.
     * <p>
     * <i>Note:</i> connectable mode defines whether the chip will respond to
     * connection attempts, and does not imply that all connections will be
     * automatically accepted.
     *
     * @param connectable
     *            Whether the local device is connectable.
     * @param timeout
     *            The length of time after which connectable mode will timeout
     *            (specified in milliseconds.)
     * @return Zero if successful, or a negative return error code.
     */
    public int updateConnectableMode(boolean connectable, int timeout) {
        return updateConnectableModeNative(connectable, timeout);
    }

    /**
     * Updates the pairability of the local device.
     *
     * @param pairable
     *            Whether the local device is pairable.
     * @param timeout
     *            The length of time after which pairable mode will timeout
     *            (specified in milliseconds).
     * @return Zero if successful, or a negative return error code.
     */
    public int updatePairableMode(boolean pairable, int timeout) {
        return updatePairableModeNative(pairable, timeout);
    }

    /**
     * Retrieves the current Local Device ID Information for the local device.
     * the Local Device.
     *
     * @return Zero if successful, or a negative return error code.
     */
    public DeviceIDInformation queryLocalDeviceIDInformation() {
        int[]               values = new int[4];
        DeviceIDInformation didInfo;

        if(queryLocalDeviceIDInformationNative(values) == 0) {
            didInfo = new DeviceIDInformation();
            didInfo.VendorID = values[0];
            didInfo.ProductID = values [1];
            didInfo.DeviceVersion = values[2];
            didInfo.USBVendorID = (values[3] != 0);
        } else {
            didInfo = null;
        }

        return didInfo;
    }

    /**
     * Enables a specific feature on the local device.
     *
     * @param localDeviceFeature The feature to enable
     *
     * @return Zero if successful, or a negative return error code.
     */
    public int enableLocalDeviceFeature(LocalDeviceFeature localDeviceFeature) {
        int feature = 0;

        switch(localDeviceFeature) {
            case BLUETOOTH_LOW_ENERGY:
                feature = LOCAL_DEVICE_FEATURE_BLUETOOTH_LOW_ENERGY;
                break;
            case ANT_PLUS:
                feature = LOCAL_DEVICE_FEATURE_ANT_PLUS;
        }

        return enableLocalDeviceFeatureNative(feature);
    }

    /**
     *
     * Disables a specific feature on the local device.
     *
     * @param localDeviceFeature The feature to disable.
     *
     * @return Zero if successful, or a negative return error code.
     */
    public int disableLocalDeviceFeature(LocalDeviceFeature localDeviceFeature) {
        int feature = 0;

        switch(localDeviceFeature) {
            case BLUETOOTH_LOW_ENERGY:
                feature = LOCAL_DEVICE_FEATURE_BLUETOOTH_LOW_ENERGY;
                break;
            case ANT_PLUS:
                feature = LOCAL_DEVICE_FEATURE_ANT_PLUS;
        }

        return disableLocalDeviceFeatureNative(feature);
    }

    /**
     * Queries the active feature on the local device.
     *
     * @return The active feature, of NULL if there was an error.
     */
    public LocalDeviceFeature queryActiveLocalDeviceFeature() {
        int[] activeFeatureNative = new int[1];

        LocalDeviceFeature activeFeature = null;

        if(queryActiveLocalDeviceFeatureNative(activeFeatureNative) == 0) {
            switch(activeFeatureNative[0]) {
                case LOCAL_DEVICE_FEATURE_BLUETOOTH_LOW_ENERGY:
                    activeFeature = LocalDeviceFeature.BLUETOOTH_LOW_ENERGY;
                    break;
                case LOCAL_DEVICE_FEATURE_ANT_PLUS:
                    activeFeature = LocalDeviceFeature.ANT_PLUS;
            }
        }

        return activeFeature;
    }

    /**
     * Begins the device discovery process, including inquiry and name
     * discovery.
     *
     * @param duration
     *            The desired duration (specified in milliseconds) of the
     *            discovery process.
     * @return Zero if successful, or a negative return error code.
     * @see #stopDeviceDiscovery
     */
    public int startDeviceDiscovery(int duration) {
        return startDeviceDiscoveryNative(duration);
    }

    /**
     * Stops an ongoing device discovery process.
     *
     * @return Zero if successful, or a negative return error code.
     * @see #startDeviceDiscovery
     */
    public int stopDeviceDiscovery() {
        return stopDeviceDiscoveryNative();
    }

    /**
     * Begins an low-energy active device scan.
     *
     * @param duration
     *            The desired duration (specified in milliseconds) of the scan.
     * @return Zero if successful, or a negative return error code.
     * @see #stopLowEnergyDeviceScan
     */
    public int startLowEnergyDeviceScan(int duration) {
        return startLowEnergyDeviceScanNative(duration);
    }

    /**
     * Stops an ongoing low-energy device scan.
     *
     * @return Zero if successful, or a negative return error code.
     * @see #startLowEnergyDeviceScan
     */
    public int stopLowEnergyDeviceScan() {
        return stopLowEnergyDeviceScanNative();
    }

    /**
     * Begins low-energy advertising.
     * @param advertisingInformation
     *          The Advertising information to set when advertising.
     * @return Zero if successful, or a negative return error code.
     */
    public int startLowEnergyAdvertising(EnumSet<AdvertisingFlags> advertisingFlags, int duration, Map<AdvertisingDataType, byte[]> advertisingData)
    {
        int index;
        int flags;
        int[] dataTypes;
        byte[][] dataList;

        flags = 0;

        if(advertisingFlags.contains(AdvertisingFlags.USE_PUBLIC_ADDRESS))
            flags |= ADVERTISING_FLAGS_USE_PUBLIC_ADDRESS;
        if(advertisingFlags.contains(AdvertisingFlags.DISCOVERABLE))
            flags |= ADVERTISING_FLAGS_DISCOVERABLE;
        if(advertisingFlags.contains(AdvertisingFlags.CONNECTABLE))
            flags |= ADVERTISING_FLAGS_CONNECTABLE;
        if(advertisingFlags.contains(AdvertisingFlags.ADVERTISE_DEVICE_NAME))
            flags |= ADVERTISING_FLAGS_ADVERTISE_DEVICE_NAME;
        if(advertisingFlags.contains(AdvertisingFlags.ADVERTISE_TX_POWER))
            flags |= ADVERTISING_FLAGS_ADVERTISE_TX_POWER;
        if(advertisingFlags.contains(AdvertisingFlags.ADVERTISE_APPEARANCE))
            flags |= ADVERTISING_FLAGS_ADVERTISE_APPEARANCE;

        if(advertisingData != null) {
            dataTypes = new int[advertisingData.size()];
            dataList = new byte[advertisingData.size()][];

            index = 0;

            for(AdvertisingDataType type : advertisingData.keySet()) {
                switch(type) {
                    case FLAGS:
                        dataTypes[index] = ADVERTISING_DATA_TYPE_FLAGS;
                        break;
                    case SERVICE_CLASS_UUID_16_BIT_PARTIAL:
                        dataTypes[index] = ADVERTISING_DATA_TYPE_16_BIT_SERVICE_UUID_PARTIAL;
                        break;
                    case SERVICE_CLASS_UUID_16_BIT_COMPLETE:
                        dataTypes[index] = ADVERTISING_DATA_TYPE_16_BIT_SERVICE_UUID_COMPLETE;
                        break;
                    case SERVICE_CLASS_UUID_32_BIT_PARTIAL:
                        dataTypes[index] = ADVERTISING_DATA_TYPE_32_BIT_SERVICE_UUID_PARTIAL;
                        break;
                    case SERVICE_CLASS_UUID_32_BIT_COMPLETE:
                        dataTypes[index] = ADVERTISING_DATA_TYPE_32_BIT_SERVICE_UUID_COMPLETE;
                        break;
                    case SERVICE_CLASS_UUID_128_BIT_PARTIAL:
                        dataTypes[index] = ADVERTISING_DATA_TYPE_128_BIT_SERVICE_UUID_PARTIAL;
                        break;
                    case SERVICE_CLASS_UUID_128_BIT_COMPLETE:
                        dataTypes[index] = ADVERTISING_DATA_TYPE_128_BIT_SERVICE_UUID_COMPLETE;
                        break;
                    case LOCAL_NAME_SHORTENED:
                        dataTypes[index] = ADVERTISING_DATA_TYPE_LOCAL_NAME_SHORTENED;
                        break;
                    case LOCAL_NAME_COMPLETE:
                        dataTypes[index] = ADVERTISING_DATA_TYPE_LOCAL_NAME_COMPLETE;
                        break;
                    case TX_POWER_LEVEL:
                        dataTypes[index] = ADVERTISING_DATA_TYPE_TX_POWER_LEVEL;
                        break;
                    case CLASS_OF_DEVICE:
                        dataTypes[index] = ADVERTISING_DATA_TYPE_CLASS_OF_DEVICE;
                        break;
                    case SIMPLE_PAIRING_HASH_C:
                        dataTypes[index] = ADVERTISING_DATA_TYPE_SIMPLE_PAIRING_HASH_C;
                        break;
                    case SIMPLE_PAIRING_RANDOMIZER_R:
                        dataTypes[index] = ADVERTISING_DATA_TYPE_SIMPLE_PAIRING_RANDOMIZER_R;
                        break;
                    case SECURITY_MANAGER_TK:
                        dataTypes[index] = ADVERTISING_DATA_TYPE_SECURITY_MANAGER_TK;
                        break;
                    case SECURITY_MANAGER_OUT_OF_BAND_FLAGS:
                        dataTypes[index] = ADVERTISING_DATA_TYPE_SECURITY_MANAGER_OUT_OF_BAND_FLAGS;
                        break;
                    case SLAVE_CONNECTION_INTERVAL_RANGE:
                        dataTypes[index] = ADVERTISING_DATA_TYPE_SLAVE_CONNECTION_INTERVAL_RANGE;
                        break;
                    case LIST_OF_16_BIT_SERVICE_SOLICITATION_UUIDS:
                        dataTypes[index] = ADVERTISING_DATA_TYPE_LIST_OF_16_BIT_SERVICE_SOLICITATION_UUIDS;
                        break;
                    case LIST_OF_128_BIT_SERVICE_SOLICITATION_UUIDS:
                        dataTypes[index] = ADVERTISING_DATA_TYPE_LIST_OF_128_BIT_SERVICE_SOLICITATION_UUIDS;
                        break;
                    case SERVICE_DATA:
                        dataTypes[index] = ADVERTISING_DATA_TYPE_SERVICE_DATA;
                        break;
                    case PUBLIC_TARGET_ADDRESS:
                        dataTypes[index] = ADVERTISING_DATA_TYPE_PUBLIC_TARGET_ADDRESS;
                        break;
                    case RANDOM_TARGET_ADDRESS:
                        dataTypes[index] = ADVERTISING_DATA_TYPE_RANDOM_TARGET_ADDRESS;
                        break;
                    case APPEARANCE:
                        dataTypes[index] = ADVERTISING_DATA_TYPE_APPEARANCE;
                        break;
                    case MANUFACTURER_SPECIFIC:
                        dataTypes[index] = ADVERTISING_DATA_TYPE_MANUFACTURER_SPECIFIC;
                        break;

                }

                dataList[index] = advertisingData.get(type);

                index++;
            }
        }
        else {
            //Send empty lists to JNI
            dataTypes = new int[0];
            dataList = new byte[0][];
        }

        return startLowEnergyAdvertisingNative(flags, duration, dataTypes, dataList);
    }

    /**
     * Stops an ongoing low-energy advertising.
     *
     * @return Zero if successful, or a negative return error code.
     */
    public int stopLowEnergyAdvertising() {
        return stopLowEnergyAdvertisingNative();
    }

    /**
     * Returns a list of currently known remote devices.
     *
     * @param deviceFilter
     *            Specifies the desired group of devices to be returned.
     * @param classOfDeviceMask
     *            Specifies the class of device to be returned.
     * @return A list of {@link BluetoothAddress Bluetooth addresses} matching
     *         the given criteria.
     */
    public BluetoothAddress[] queryRemoteDeviceList(RemoteDeviceFilter deviceFilter, int classOfDeviceMask) {
        int                  filter;
        BluetoothAddress[][] deviceList = { null };

        filter = 0;
        if(deviceFilter != null) {
            switch(deviceFilter) {
            case ALL_DEVICES:
                filter = REMOTE_DEVICE_FILTER_ALL_DEVICES;
                break;
            case CURRENTLY_CONNECTED:
                filter = REMOTE_DEVICE_FILTER_CURRENTLY_CONNECTED;
                break;
            case CURRENTLY_PAIRED:
                filter = REMOTE_DEVICE_FILTER_CURRENTLY_PAIRED;
                break;
            case CURRENTLY_UNPAIRED:
                filter = REMOTE_DEVICE_FILTER_CURRENTLY_UNPAIRED;
                break;
            }
        }

        queryRemoteDeviceListNative(filter, classOfDeviceMask, deviceList);

        return deviceList[0];
    }

    //TODO What is the result if the device was not previously known? If 'forceUpdate'=FALSE, I expect null. What about 'forceUpdate'=TRUE?
    /**
     * Returns the properties of a given remote device.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @param forceUpdate
     *            If {@code TRUE}, the Device Manager will queue an asynchronous
     *            event to update its cache of the device's properties; any such
     *            updates will return via the
     *            {@link EventCallback#remoteDevicePropertiesStatusEvent
     *            remoteDevicePropertiesStatusEvent}.
     * @return The properties of the device.
     */
    public RemoteDeviceProperties queryRemoteDeviceProperties(BluetoothAddress remoteDevice, boolean forceUpdate) {
        int[]                  properties;
        byte[]                 address;
        byte[]                 priorAddress;
        String[]               names;
        RemoteDeviceProperties devProps;

        address = remoteDevice.internalByteArray();

        names = new String[2];
        properties = new int[10];
        priorAddress = new byte[6];

        if(queryRemoteDevicePropertiesNative(address[0], address[1], address[2], address[3], address[4], address[5], forceUpdate, names, properties, priorAddress) == 0) {
            devProps = new RemoteDeviceProperties();

            devProps.deviceAddress = remoteDevice;
            devProps.classOfDevice = properties[NATIVE_REMOTE_PROP_INDEX_CLASS_OF_DEVICE];
            devProps.rssi = properties[NATIVE_REMOTE_PROP_INDEX_RSSI];
            devProps.transmitPower = properties[NATIVE_REMOTE_PROP_INDEX_TRANSMIT_POWER];
            devProps.sniffInterval = properties[NATIVE_REMOTE_PROP_INDEX_SNIFF_INTERVAL];

            devProps.remoteDeviceFlags = EnumSet.noneOf(RemoteDeviceFlags.class);

            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_NAME_KNOWN) == REMOTE_DEVICE_FLAGS_NAME_KNOWN) {
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.NAME_KNOWN);
                devProps.deviceName = names[NATIVE_REMOTE_STRING_INDEX_NAME];
            } else {
                devProps.deviceName = null;
            }

            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_APPLICATION_DATA_VALID) == REMOTE_DEVICE_FLAGS_APPLICATION_DATA_VALID) {
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.APPLICATION_DATA_VALID);
                devProps.applicationData = new RemoteDeviceApplicationData();
                devProps.applicationData.friendlyName = names[NATIVE_REMOTE_STRING_INDEX_FRIENDLY_NAME];
                devProps.applicationData.applicationInfo = properties[NATIVE_REMOTE_PROP_INDEX_APPLICATION_INFO];
            } else {
                devProps.applicationData = null;
            }

            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED) == REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.CURRENTLY_PAIRED);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED) == REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.CURRENTLY_CONNECTED);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_ENCRYPTED) == REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_ENCRYPTED)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.LINK_CURRENTLY_ENCRYPTED);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_SNIFF_MODE) == REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_SNIFF_MODE)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.LINK_CURRENTLY_SNIFF_MODE);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_LINK_INITIATED_LOCALLY) == REMOTE_DEVICE_FLAGS_LINK_INITIATED_LOCALLY)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.LINK_INITIATED_LOCALLY);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_AUTHENTICATED_KEY) == REMOTE_DEVICE_FLAGS_AUTHENTICATED_KEY)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.AUTHENTICATED_KEY);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_SERVICES_KNOWN) == REMOTE_DEVICE_FLAGS_SERVICES_KNOWN)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.SERVICES_KNOWN);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_TX_POWER_KNOWN) == REMOTE_DEVICE_FLAGS_TX_POWER_KNOWN)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.TX_POWER_KNOWN);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_EIR_DATA_KNOWN) == REMOTE_DEVICE_FLAGS_EIR_DATA_KNOWN)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.EIR_DATA_KNOWN);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_LE_SERVICES_KNOWN) == REMOTE_DEVICE_FLAGS_LE_SERVICES_KNOWN)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.LE_SERVICES_KNOWN);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN) == REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.LE_APPEARANCE_KNOWN);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED_OVER_LE) == REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED_OVER_LE)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.CURRENTLY_PAIRED_OVER_LE);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED_OVER_LE) == REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED_OVER_LE)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.CURRENTLY_CONNECTED_OVER_LE);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_LE_LINK_CURRENTLY_ENCRYPTED) == REMOTE_DEVICE_FLAGS_LE_LINK_CURRENTLY_ENCRYPTED)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.LE_LINK_CURRENTLY_ENCRYPTED);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_LE_AUTHENTICATED_KEY) == REMOTE_DEVICE_FLAGS_LE_AUTHENTICATED_KEY)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.LE_AUTHENTICATED_KEY);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_LE_LINK_INITIATED_LOCALLY) == REMOTE_DEVICE_FLAGS_LE_LINK_INITIATED_LOCALLY)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.LE_LINK_INITIATED_LOCALLY);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN) == REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.LE_TX_POWER_KNOWN);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY) == REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_BR_EDR) == REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_BR_EDR)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.SUPPORTS_BR_EDR);

            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY) == REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY) {
                switch(properties[NATIVE_REMOTE_PROP_INDEX_LOW_ENERGY_ADDRESS_TYPE]) {
                case ADDRESS_TYPE_PUBLIC:
                    devProps.lowEnergyAddressType = AddressType.PUBLIC;
                    break;
                case ADDRESS_TYPE_STATIC:
                    devProps.lowEnergyAddressType = AddressType.STATIC;
                    break;
                case ADDRESS_TYPE_PRIVATE_RESOLVABLE:
                    devProps.lowEnergyAddressType = AddressType.PUBLIC;
                    break;
                case ADDRESS_TYPE_PRIVATE_NONRESOLVABLE:
                    devProps.lowEnergyAddressType = AddressType.PUBLIC;
                    break;
                default:
                    devProps.lowEnergyAddressType = null;
                }

                devProps.lowEnergyRSSI = properties[NATIVE_REMOTE_PROP_INDEX_LOW_ENERGY_RSSI];

                if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN) == REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN)
                    devProps.lowEnergyTransmitPower = properties[NATIVE_REMOTE_PROP_INDEX_LOW_ENERGY_TRANSMIT_POWER];
                else
                    devProps.lowEnergyTransmitPower = 0;

                if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN) == REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN)
                    devProps.deviceAppearance = properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_APPEARANCE];
                else
                    devProps.deviceAppearance = 0;

                devProps.priorResolvableAddress = new BluetoothAddress(priorAddress);
            } else {
                devProps.lowEnergyAddressType   = null;
                devProps.lowEnergyRSSI          = 0;
                devProps.lowEnergyTransmitPower = 0;
                devProps.deviceAppearance       = 0;
                devProps.priorResolvableAddress = null;
            }
        } else {
            devProps = null;
        }

        return devProps;
    }

    /**
     * Returns the properties of a given remote low-energy device.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @param forceUpdate
     *            If {@code TRUE}, the Device Manager will queue an asynchronous
     *            event to update its cache of the device's properties; any such
     *            updates will return via the
     *            {@link EventCallback#remoteDevicePropertiesStatusEvent
     *            remoteDevicePropertiesStatusEvent}.
     * @return The properties of the device.
     */
    public RemoteDeviceProperties queryRemoteLowEnergyDeviceProperties(BluetoothAddress remoteDevice, boolean forceUpdate) {
        int[] properties;
        byte[] address;
        byte[] priorAddress;
        String[] names;
        RemoteDeviceProperties devProps;

        address = remoteDevice.internalByteArray();

        names = new String[2];
        properties = new int[10];
        priorAddress = new byte[6];

        if(queryRemoteLowEnergyDevicePropertiesNative(address[0], address[1], address[2], address[3], address[4], address[5], forceUpdate, names, properties, priorAddress) == 0) {
            devProps = new RemoteDeviceProperties();

            devProps.deviceAddress = remoteDevice;
            devProps.classOfDevice = properties[NATIVE_REMOTE_PROP_INDEX_CLASS_OF_DEVICE];
            devProps.rssi = properties[NATIVE_REMOTE_PROP_INDEX_RSSI];
            devProps.transmitPower = properties[NATIVE_REMOTE_PROP_INDEX_TRANSMIT_POWER];
            devProps.sniffInterval = properties[NATIVE_REMOTE_PROP_INDEX_SNIFF_INTERVAL];

            devProps.remoteDeviceFlags = EnumSet.noneOf(RemoteDeviceFlags.class);

            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_NAME_KNOWN) == REMOTE_DEVICE_FLAGS_NAME_KNOWN) {
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.NAME_KNOWN);
                devProps.deviceName = names[NATIVE_REMOTE_STRING_INDEX_NAME];
            } else {
                devProps.deviceName = null;
            }

            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_APPLICATION_DATA_VALID) == REMOTE_DEVICE_FLAGS_APPLICATION_DATA_VALID) {
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.APPLICATION_DATA_VALID);
                devProps.applicationData = new RemoteDeviceApplicationData();
                devProps.applicationData.friendlyName = names[NATIVE_REMOTE_STRING_INDEX_FRIENDLY_NAME];
                devProps.applicationData.applicationInfo = properties[NATIVE_REMOTE_PROP_INDEX_APPLICATION_INFO];
            } else {
                devProps.applicationData = null;
            }

            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED) == REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.CURRENTLY_PAIRED);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED) == REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.CURRENTLY_CONNECTED);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_ENCRYPTED) == REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_ENCRYPTED)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.LINK_CURRENTLY_ENCRYPTED);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_SNIFF_MODE) == REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_SNIFF_MODE)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.LINK_CURRENTLY_SNIFF_MODE);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_LINK_INITIATED_LOCALLY) == REMOTE_DEVICE_FLAGS_LINK_INITIATED_LOCALLY)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.LINK_INITIATED_LOCALLY);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_AUTHENTICATED_KEY) == REMOTE_DEVICE_FLAGS_AUTHENTICATED_KEY)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.AUTHENTICATED_KEY);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_SERVICES_KNOWN) == REMOTE_DEVICE_FLAGS_SERVICES_KNOWN)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.SERVICES_KNOWN);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_TX_POWER_KNOWN) == REMOTE_DEVICE_FLAGS_TX_POWER_KNOWN)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.TX_POWER_KNOWN);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_EIR_DATA_KNOWN) == REMOTE_DEVICE_FLAGS_EIR_DATA_KNOWN)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.EIR_DATA_KNOWN);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_LE_SERVICES_KNOWN) == REMOTE_DEVICE_FLAGS_LE_SERVICES_KNOWN)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.LE_SERVICES_KNOWN);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN) == REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.LE_APPEARANCE_KNOWN);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED_OVER_LE) == REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED_OVER_LE)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.CURRENTLY_PAIRED_OVER_LE);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED_OVER_LE) == REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED_OVER_LE)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.CURRENTLY_CONNECTED_OVER_LE);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_LE_LINK_CURRENTLY_ENCRYPTED) == REMOTE_DEVICE_FLAGS_LE_LINK_CURRENTLY_ENCRYPTED)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.LE_LINK_CURRENTLY_ENCRYPTED);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_LE_AUTHENTICATED_KEY) == REMOTE_DEVICE_FLAGS_LE_AUTHENTICATED_KEY)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.LE_AUTHENTICATED_KEY);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_LE_LINK_INITIATED_LOCALLY) == REMOTE_DEVICE_FLAGS_LE_LINK_INITIATED_LOCALLY)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.LE_LINK_INITIATED_LOCALLY);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN) == REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.LE_TX_POWER_KNOWN);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY) == REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY);
            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_BR_EDR) == REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_BR_EDR)
                devProps.remoteDeviceFlags.add(RemoteDeviceFlags.SUPPORTS_BR_EDR);

            if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY) == REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY) {
                switch(properties[NATIVE_REMOTE_PROP_INDEX_LOW_ENERGY_ADDRESS_TYPE]) {
                case ADDRESS_TYPE_PUBLIC:
                    devProps.lowEnergyAddressType = AddressType.PUBLIC;
                    break;
                case ADDRESS_TYPE_STATIC:
                    devProps.lowEnergyAddressType = AddressType.STATIC;
                    break;
                case ADDRESS_TYPE_PRIVATE_RESOLVABLE:
                    devProps.lowEnergyAddressType = AddressType.PUBLIC;
                    break;
                case ADDRESS_TYPE_PRIVATE_NONRESOLVABLE:
                    devProps.lowEnergyAddressType = AddressType.PUBLIC;
                    break;
                default:
                    devProps.lowEnergyAddressType = null;
                }

                devProps.lowEnergyRSSI = properties[NATIVE_REMOTE_PROP_INDEX_LOW_ENERGY_RSSI];

                if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN) == REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN)
                    devProps.lowEnergyTransmitPower = properties[NATIVE_REMOTE_PROP_INDEX_LOW_ENERGY_TRANSMIT_POWER];
                else
                    devProps.lowEnergyTransmitPower = 0;

                if((properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS] & REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN) == REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN)
                    devProps.deviceAppearance = properties[NATIVE_REMOTE_PROP_INDEX_DEVICE_APPEARANCE];
                else
                    devProps.deviceAppearance = 0;

                devProps.priorResolvableAddress = new BluetoothAddress(priorAddress);
            } else {
                devProps.lowEnergyAddressType   = null;
                devProps.lowEnergyRSSI          = 0;
                devProps.lowEnergyTransmitPower = 0;
                devProps.deviceAppearance       = 0;
                devProps.priorResolvableAddress = null;
            }
        } else {
            devProps = null;
        }

        return devProps;
    }

    /**
     * Retrieves the BR/EDR Extended Inquiry Response (EIR) data of a given
     * remote device
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @return A mapping of Type->Data for each entry in the EIR data, or
     *         {@code null} if there was an error.
     */
    public Map<EIRDataType, byte[]> queryRemoteDeviceEIRData(BluetoothAddress remoteDevice) {
        byte[]                   address = remoteDevice.internalByteArray();
        int[][]                  types   = {null};
        byte[][][]               data    = {null};
        Map<EIRDataType, byte[]> map     = null;;

        if(queryRemoteDeviceEIRDataNative(address[0], address[1], address[2], address[3], address[4], address[5], types, data) >= 0) {
            if((types[0].length > 0) && (types[0].length == data[0].length)) {
                map = new HashMap<EIRDataType, byte[]>(types[0].length);

                for(int i = 0; i < types[0].length; i++) {
                    switch(types[0][i]) {
                    case EIR_DATA_TYPE_FLAGS:
                        map.put(EIRDataType.FLAGS, data[0][i]);
                        break;
                    case EIR_DATA_TYPE_16_BIT_SERVICE_CLASS_UUID_PARTIAL:
                        map.put(EIRDataType.SERVICE_CLASS_UUID_16_BIT_PARTIAL, data[0][i]);
                        break;
                    case EIR_DATA_TYPE_16_BIT_SERVICE_CLASS_UUID_COMPLETE:
                        map.put(EIRDataType.SERVICE_CLASS_UUID_16_BIT_COMPLETE, data[0][i]);
                        break;
                    case EIR_DATA_TYPE_32_BIT_SERVICE_CLASS_UUID_PARTIAL:
                        map.put(EIRDataType.SERVICE_CLASS_UUID_32_BIT_PARTIAL, data[0][i]);
                        break;
                    case EIR_DATA_TYPE_32_BIT_SERVICE_CLASS_UUID_COMPLETE:
                        map.put(EIRDataType.SERVICE_CLASS_UUID_32_BIT_COMPLETE, data[0][i]);
                        break;
                    case EIR_DATA_TYPE_128_BIT_SERVICE_CLASS_UUID_PARTIAL:
                        map.put(EIRDataType.SERVICE_CLASS_UUID_128_BIT_PARTIAL, data[0][i]);
                        break;
                    case EIR_DATA_TYPE_128_BIT_SERVICE_CLASS_UUID_COMPLETE:
                        map.put(EIRDataType.SERVICE_CLASS_UUID_128_BIT_COMPLETE, data[0][i]);
                        break;
                    case EIR_DATA_TYPE_LOCAL_NAME_SHORTENED:
                        map.put(EIRDataType.LOCAL_NAME_SHORTENED, data[0][i]);
                        break;
                    case EIR_DATA_TYPE_LOCAL_NAME_COMPLETE:
                        map.put(EIRDataType.LOCAL_NAME_COMPLETE, data[0][i]);
                        break;
                    case EIR_DATA_TYPE_TX_POWER_LEVEL:
                        map.put(EIRDataType.TX_POWER_LEVEL, data[0][i]);
                        break;
                    case EIR_DATA_TYPE_CLASS_OF_DEVICE:
                        map.put(EIRDataType.CLASS_OF_DEVICE, data[0][i]);
                        break;
                    case EIR_DATA_TYPE_SIMPLE_PAIRING_HASH_C:
                        map.put(EIRDataType.SIMPLE_PAIRING_HASH_C, data[0][i]);
                        break;
                    case EIR_DATA_TYPE_SIMPLE_PAIRING_RANDOMIZER_R:
                        map.put(EIRDataType.SIMPLE_PAIRING_RANDOMIZER_R, data[0][i]);
                        break;
                    case EIR_DATA_TYPE_DEVICE_ID:
                        map.put(EIRDataType.DEVICE_ID, data[0][i]);
                        break;
                    case EIR_DATA_TYPE_MANUFACTURER_SPECIFIC:
                    default:
                        map.put(EIRDataType.MANUFACTURER_SPECIFIC, data[0][i]);
                        break;
                    }
                }
            } else {
                map = new HashMap<EIRDataType, byte[]>(0);
            }
        }

        return map;
    }

    /**
     * Retrieves the Low-Energy Advertising data of a given remote low-energy
     * device.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @return A mapping of Type->Data for each entry in the Advertising data,
     *         or {@code null} if there was an error.
     */
    public Map<AdvertisingDataType, byte[]> queryRemoteLowEnergyDeviceAdvertisingData(BluetoothAddress remoteDevice) {
        byte[]               address = remoteDevice.internalByteArray();
        int[][]              types   = {null};
        byte[][][]           data    = {null};
        Map<AdvertisingDataType, byte[]> map     = null;;

        if(queryRemoteLowEnergyDeviceAdvertisingDataNative(address[0], address[1], address[2], address[3], address[4], address[5], types, data, false) >= 0) {
            if((types[0].length > 0) && (types[0].length == data[0].length)) {
                map = new HashMap<AdvertisingDataType, byte[]>(types[0].length);

                for(int i = 0; i < types[0].length; i++) {
                    switch(types[0][i]) {
                    case ADVERTISING_DATA_TYPE_FLAGS:
                        map.put(AdvertisingDataType.FLAGS, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_16_BIT_SERVICE_UUID_PARTIAL:
                        map.put(AdvertisingDataType.SERVICE_CLASS_UUID_16_BIT_PARTIAL, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_16_BIT_SERVICE_UUID_COMPLETE:
                        map.put(AdvertisingDataType.SERVICE_CLASS_UUID_16_BIT_COMPLETE, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_32_BIT_SERVICE_UUID_PARTIAL:
                        map.put(AdvertisingDataType.SERVICE_CLASS_UUID_32_BIT_PARTIAL, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_32_BIT_SERVICE_UUID_COMPLETE:
                        map.put(AdvertisingDataType.SERVICE_CLASS_UUID_32_BIT_COMPLETE, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_128_BIT_SERVICE_UUID_PARTIAL:
                        map.put(AdvertisingDataType.SERVICE_CLASS_UUID_128_BIT_PARTIAL, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_128_BIT_SERVICE_UUID_COMPLETE:
                        map.put(AdvertisingDataType.SERVICE_CLASS_UUID_128_BIT_COMPLETE, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_LOCAL_NAME_SHORTENED:
                        map.put(AdvertisingDataType.LOCAL_NAME_SHORTENED, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_LOCAL_NAME_COMPLETE:
                        map.put(AdvertisingDataType.LOCAL_NAME_COMPLETE, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_TX_POWER_LEVEL:
                        map.put(AdvertisingDataType.TX_POWER_LEVEL, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_CLASS_OF_DEVICE:
                        map.put(AdvertisingDataType.CLASS_OF_DEVICE, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_SIMPLE_PAIRING_HASH_C:
                        map.put(AdvertisingDataType.SIMPLE_PAIRING_HASH_C, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_SIMPLE_PAIRING_RANDOMIZER_R:
                        map.put(AdvertisingDataType.SIMPLE_PAIRING_RANDOMIZER_R, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_SECURITY_MANAGER_TK:
                        map.put(AdvertisingDataType.SECURITY_MANAGER_TK, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_SECURITY_MANAGER_OUT_OF_BAND_FLAGS:
                        map.put(AdvertisingDataType.SECURITY_MANAGER_OUT_OF_BAND_FLAGS, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_SLAVE_CONNECTION_INTERVAL_RANGE:
                        map.put(AdvertisingDataType.SLAVE_CONNECTION_INTERVAL_RANGE, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_LIST_OF_16_BIT_SERVICE_SOLICITATION_UUIDS:
                        map.put(AdvertisingDataType.LIST_OF_16_BIT_SERVICE_SOLICITATION_UUIDS, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_LIST_OF_128_BIT_SERVICE_SOLICITATION_UUIDS:
                        map.put(AdvertisingDataType.LIST_OF_128_BIT_SERVICE_SOLICITATION_UUIDS, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_SERVICE_DATA:
                        map.put(AdvertisingDataType.SERVICE_DATA, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_PUBLIC_TARGET_ADDRESS:
                        map.put(AdvertisingDataType.PUBLIC_TARGET_ADDRESS, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_RANDOM_TARGET_ADDRESS:
                        map.put(AdvertisingDataType.RANDOM_TARGET_ADDRESS, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_APPEARANCE:
                        map.put(AdvertisingDataType.APPEARANCE, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_MANUFACTURER_SPECIFIC:
                    default:
                        map.put(AdvertisingDataType.MANUFACTURER_SPECIFIC, data[0][i]);
                        break;
                    }
                }
            } else {
                map = new HashMap<AdvertisingDataType, byte[]>(0);
            }
        }

        return map;
    }

    /**
     * Retrieves the Low-Energy Scan Response data of a given remote low-energy
     * device.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @return A mapping of Type->Data for each entry in the Scan Response data,
     *         or {@code null} if there was an error.
     */
    public Map<AdvertisingDataType, byte[]> queryRemoteLowEnergyDeviceScanResponseData(BluetoothAddress remoteDevice) {
        byte[]                           address = remoteDevice.internalByteArray();
        int[][]                          types   = {null};
        byte[][][]                       data    = {null};
        Map<AdvertisingDataType, byte[]> map     = null;;

        if(queryRemoteLowEnergyDeviceAdvertisingDataNative(address[0], address[1], address[2], address[3], address[4], address[5], types, data, true) >= 0) {
            if((types[0].length > 0) && (types[0].length == data[0].length)) {
                map = new HashMap<AdvertisingDataType, byte[]>(types[0].length);

                for(int i = 0; i < types[0].length; i++) {
                    switch(types[0][i]) {
                    case ADVERTISING_DATA_TYPE_FLAGS:
                        map.put(AdvertisingDataType.FLAGS, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_16_BIT_SERVICE_UUID_PARTIAL:
                        map.put(AdvertisingDataType.SERVICE_CLASS_UUID_16_BIT_PARTIAL, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_16_BIT_SERVICE_UUID_COMPLETE:
                        map.put(AdvertisingDataType.SERVICE_CLASS_UUID_16_BIT_COMPLETE, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_32_BIT_SERVICE_UUID_PARTIAL:
                        map.put(AdvertisingDataType.SERVICE_CLASS_UUID_32_BIT_PARTIAL, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_32_BIT_SERVICE_UUID_COMPLETE:
                        map.put(AdvertisingDataType.SERVICE_CLASS_UUID_32_BIT_COMPLETE, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_128_BIT_SERVICE_UUID_PARTIAL:
                        map.put(AdvertisingDataType.SERVICE_CLASS_UUID_128_BIT_PARTIAL, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_128_BIT_SERVICE_UUID_COMPLETE:
                        map.put(AdvertisingDataType.SERVICE_CLASS_UUID_128_BIT_COMPLETE, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_LOCAL_NAME_SHORTENED:
                        map.put(AdvertisingDataType.LOCAL_NAME_SHORTENED, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_LOCAL_NAME_COMPLETE:
                        map.put(AdvertisingDataType.LOCAL_NAME_COMPLETE, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_TX_POWER_LEVEL:
                        map.put(AdvertisingDataType.TX_POWER_LEVEL, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_CLASS_OF_DEVICE:
                        map.put(AdvertisingDataType.CLASS_OF_DEVICE, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_SIMPLE_PAIRING_HASH_C:
                        map.put(AdvertisingDataType.SIMPLE_PAIRING_HASH_C, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_SIMPLE_PAIRING_RANDOMIZER_R:
                        map.put(AdvertisingDataType.SIMPLE_PAIRING_RANDOMIZER_R, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_SECURITY_MANAGER_TK:
                        map.put(AdvertisingDataType.SECURITY_MANAGER_TK, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_SECURITY_MANAGER_OUT_OF_BAND_FLAGS:
                        map.put(AdvertisingDataType.SECURITY_MANAGER_OUT_OF_BAND_FLAGS, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_SLAVE_CONNECTION_INTERVAL_RANGE:
                        map.put(AdvertisingDataType.SLAVE_CONNECTION_INTERVAL_RANGE, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_LIST_OF_16_BIT_SERVICE_SOLICITATION_UUIDS:
                        map.put(AdvertisingDataType.LIST_OF_16_BIT_SERVICE_SOLICITATION_UUIDS, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_LIST_OF_128_BIT_SERVICE_SOLICITATION_UUIDS:
                        map.put(AdvertisingDataType.LIST_OF_128_BIT_SERVICE_SOLICITATION_UUIDS, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_SERVICE_DATA:
                        map.put(AdvertisingDataType.SERVICE_DATA, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_PUBLIC_TARGET_ADDRESS:
                        map.put(AdvertisingDataType.PUBLIC_TARGET_ADDRESS, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_RANDOM_TARGET_ADDRESS:
                        map.put(AdvertisingDataType.RANDOM_TARGET_ADDRESS, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_APPEARANCE:
                        map.put(AdvertisingDataType.APPEARANCE, data[0][i]);
                        break;
                    case ADVERTISING_DATA_TYPE_MANUFACTURER_SPECIFIC:
                    default:
                        map.put(AdvertisingDataType.MANUFACTURER_SPECIFIC, data[0][i]);
                        break;
                    }
                }
            } else {
                map = new HashMap<AdvertisingDataType, byte[]>(0);
            }
        }

        return map;
    }

    /**
     * Updates the list of services associated with a given remote device.
     * <p>
     * <i>Note:</i> Results will be returned via the
     * {@link EventCallback#remoteDeviceServicesStatusEvent(BluetoothAddress, boolean)}
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @return Zero if successful, or a negative return error code.
     */
    public int updateRemoteDeviceServices(BluetoothAddress remoteDevice) {
        byte address[] = remoteDevice.internalByteArray();

        return queryRemoteDeviceServicesNative(address[0], address[1], address[2], address[3], address[4], address[5], true, null);
    }

    /**
     * Updates the list of services associated with a given remote low-energy
     * device.
     * <p>
     * <i>Note:</i> Results will be returned via the
     * {@link EventCallback#remoteLowEnergyDeviceServicesStatusEvent(BluetoothAddress, boolean)}
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @return Zero if successful, or a negative return error code.
     */
    public int updateRemoteLowEnergyDeviceServices(BluetoothAddress remoteDevice) {
        byte address[] = remoteDevice.internalByteArray();

        return queryRemoteLowEnergyDeviceServicesNative(address[0], address[1], address[2], address[3], address[4], address[5], true, null);
    }

    /**
     * Returns a raw data buffer containing a list of services provided by a
     * given remote device.
     * <p>
     * <i>Note:</i> This method does not refresh the internal list of services
     * provided by the device; invoke {@link #updateRemoteDeviceServices} before
     * using this method to ensure accurate results.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @return A byte array containing the raw service data.
     * @see #queryRemoteDeviceSupportedServices
     */
    public byte[] queryRemoteDeviceServicesRaw(BluetoothAddress remoteDevice) {
        byte[]   address = remoteDevice.internalByteArray();
        byte[][] sdpData = { null };

        if(queryRemoteDeviceServicesNative(address[0], address[1], address[2], address[3], address[4], address[5], false, sdpData) >= 0)
            return sdpData[0];
        else
            return null;
    }

    /**
     * Returns a raw data buffer containing a list of services provided by a
     * given remote low-energy device.
     * <p>
     * <i>Note:</i> This method does not refresh the internal list of services
     * provided by the device; invoke
     * {@link #updateRemoteLowEnergyDeviceServices} before using this method to
     * ensure accurate results.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @return A byte array containing the raw service data.
     * //XXX @see #queryRemoteDeviceSupportedServices
     */
    public byte[] queryRemoteLowEnergyDeviceServicesRaw(BluetoothAddress remoteDevice) {
        byte[]   address = remoteDevice.internalByteArray();
        byte[][] serviceData = {null};

        if(queryRemoteLowEnergyDeviceServicesNative(address[0], address[1], address[2], address[3], address[4], address[5], false, serviceData) >= 0)
            return serviceData[0];
        else
            return null;
    }

    /**
     * Returns a list of remote services provided by a given remote device.
     * <p>
     * <i>Note:</i> This method does not refresh the internal list of services
     * provided by the device; invoke {@link #updateRemoteDeviceServices} before
     * using this method to ensure accurate results.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @return A list of UUIDs corresponding to the services provided by the
     *         device.
     */
    public UUID[] queryRemoteDeviceSupportedServices(BluetoothAddress remoteDevice) {
        byte address[] = remoteDevice.internalByteArray();
        long[][] result;
        UUID[] services;

        result = querySupportedServicesNative(address[0], address[1], address[2], address[3], address[4], address[5]);

        /*
         * 'result' should now be an array of size [2][n], for 'n' services. For
         * service 'x', result[0][x] contains the most significant bytes and
         * result[1][x] contains the least significant bytes.
         */
        if((result != null) && (result.length == 2) && (result[0] != null) && (result[1] != null) && (result[0].length == result[1].length)) {
            services = new UUID[result[0].length];

            for(int i = 0; i < result[0].length; i++)
                services[i] = new UUID(result[0][i], result[1][i]);
        } else {
            services = null;
        }

        return services;
    }

    /**
     * Returns a list of remote services provided by a given remote device.
     * <p>
     * <i>Note:</i> This method does not refresh the internal list of services
     * provided by the device; invoke {@link #updateRemoteDeviceServices} before
     * using this method to ensure accurate results.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @return A list of UUIDs corresponding to the services provided by the
     *         device.
     */
    public UUID[] queryRemoteDeviceSupportedLowEnergyServices(BluetoothAddress remoteDevice) {
        byte address[] = remoteDevice.internalByteArray();
        long[][] result;
        UUID[] services;

        result = querySupportedLowEnergyServicesNative(address[0], address[1], address[2], address[3], address[4], address[5]);

        /*
         * 'result' should now be an array of size [2][n], for 'n' services. For
         * service 'x', result[0][x] contains the most significant bytes and
         * result[1][x] contains the least significant bytes.
         */
        if((result != null) && (result.length == 2) && (result[0] != null) && (result[1] != null) && (result[0].length == result[1].length)) {
            services = new UUID[result[0].length];

            for(int i = 0; i < result[0].length; i++)
                services[i] = new UUID(result[0][i], result[1][i]);
        } else {
            services = null;
        }

        return services;
    }

    /**
     * Adds a specific remote device entry to the current list of remote devices
     * maintained by the Bluetopia Platform Manager.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @param classOfDevice
     *            The class of the remote device.
     * @param applicationData
     *            The application-specific data to be associated with the entry,
     *            or {@code null} if no such data should be maintained.
     * @return Zero if successful, or a negative return error code.
     */
    public int addRemoteDevice(BluetoothAddress remoteDevice, int classOfDevice, RemoteDeviceApplicationData applicationData) {
        if(((classOfDevice & 0x00FFFFFF) == 0x00FFFFFF) || ((classOfDevice & 0x00FFFFFF) == 0x00FFFFFE))
            throw new IllegalArgumentException("The value provided for classOfDevice (0x" + Integer.toHexString(classOfDevice) + ") has special meaning and cannot be used for manually created remote devices.");

        byte[] address = remoteDevice.internalByteArray();

        if(applicationData != null)
            return addRemoteDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5], classOfDevice, applicationData.friendlyName, applicationData.applicationInfo);
        else
            return addRemoteDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5], classOfDevice);
    }

    /**
     * Adds a specific remote low-energy device entry to the current list of
     * remote devices maintained by the Bluetopia Platform Manager.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @param applicationData
     *            The application-specific data to be associated with the entry,
     *            or {@code null} if no such data should be maintained.
     * @return Zero if successful, or a negative return error code.
     */
    public int addRemoteLowEnergyDevice(BluetoothAddress remoteDevice, RemoteDeviceApplicationData applicationData) {
        byte[] address = remoteDevice.internalByteArray();

        if(applicationData != null)
            return addRemoteDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5], 0x00FFFFFF, applicationData.friendlyName, applicationData.applicationInfo);
        else
            return addRemoteDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5], 0x00FFFFFF);
    }

    /**
     * Adds a specific remote dual-mode device entry to the current list of
     * remote devices maintained by the Bluetopia Platform Manager.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @param applicationData
     *            The application-specific data to be associated with the entry,
     *            or {@code null} if no such data should be maintained.
     * @return Zero if successful, or a negative return error code.
     */
    public int addRemoteDualModeDevice(BluetoothAddress remoteDevice, RemoteDeviceApplicationData applicationData) {
        byte[] address = remoteDevice.internalByteArray();

        if(applicationData != null)
            return addRemoteDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5], 0x00FFFFFE, applicationData.friendlyName, applicationData.applicationInfo);
        else
            return addRemoteDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5], 0x00FFFFFE);
    }

    /**
     * Removes a specific remote device entry from the current list of remote
     * devices maintained by the Bluetopia Platform Manager.
     *
     * @param remoteDevice
     *            The Bluetooth address of the device to remove.
     * @return Zero if successful, or a negative return error code.
     */
    public int deleteRemoteDevice(BluetoothAddress remoteDevice) {
        byte address[] = remoteDevice.internalByteArray();

        return deleteRemoteDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5]);
    }

    /**
     * Sets or updates the application-specific data associated with a specific
     * remote device.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @param applicationData
     *            The application-specific data.
     * @return Zero if successful, or a negative return error code.
     */
    public int updateRemoteDeviceApplicationData(BluetoothAddress remoteDevice, RemoteDeviceApplicationData applicationData) {
        byte address[] = remoteDevice.internalByteArray();

        return updateRemoteDeviceApplicationDataNative(address[0], address[1], address[2], address[3], address[4], address[5], applicationData.friendlyName, applicationData.applicationInfo);
    }

    /**
     * Removes a specific group of remote devices from the device list
     * maintained by Bluetopia Platform Manager.
     *
     * @param devicesFilter
     *            The group of devices to be removed.
     * @return Zero if successful, or a negative return error code.
     */
    public int deleteRemoteDevices(RemoteDeviceFilter devicesFilter) {
        int filter = 0;

        if(devicesFilter != null) {
            switch(devicesFilter) {
            case ALL_DEVICES:
                filter = REMOTE_DEVICE_FILTER_ALL_DEVICES;
                break;
            case CURRENTLY_CONNECTED:
                filter = REMOTE_DEVICE_FILTER_CURRENTLY_CONNECTED;
                break;
            case CURRENTLY_PAIRED:
                filter = REMOTE_DEVICE_FILTER_CURRENTLY_PAIRED;
                break;
            case CURRENTLY_UNPAIRED:
                filter = REMOTE_DEVICE_FILTER_CURRENTLY_UNPAIRED;
                break;
            }
        }

        return deleteRemoteDevicesNative(filter);
    }

    /**
     * Begins the pairing process with a given remote device.
     * <p>
     * If required, this method will initiate a connection with the remote
     * device.
     * <p>
     * <i>Note:</i> the pairing process itself is asynchronous. The caller can
     * determine the status of the pairing procedure by handling the
     * {@link DEVM#remoteDevicePairingStatusEvent
     * remoteDevicePairingStatusEvent}.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @param forcePair
     *            Forces the pairing process to begin anew even if there is an
     *            existing link key associated with the remote device.
     * @return Zero if successful, or a negative return error code.
     */
    public int pairWithRemoteDevice(BluetoothAddress remoteDevice, boolean forcePair) {
        byte address[] = remoteDevice.internalByteArray();

        return pairWithRemoteDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5], forcePair);
    }

    /**
     * Begins the pairing process with a given remote device.
     * <p>
     * If required, this method will initiate a connection with the remote
     * device and will automatically disconnect from the remote device when the
     * pairing process is complete.
     * <p>
     * <i>Note:</i> the pairing process itself is asynchronous. The caller can
     * determine the status of the pairing procedure by handling the
     * {@link DEVM#remoteDevicePairingStatusEvent
     * remoteDevicePairingStatusEvent}.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @param forcePair
     *            Forces the pairing process to begin anew even if there is an
     *            existing link key associated with the remote device.
     * @return Zero if successful, or a negative return error code.
     */
    public int pairWithRemoteLowEnergyDevice(BluetoothAddress remoteDevice, boolean forcePair) {
        byte address[] = remoteDevice.internalByteArray();

        return pairWithRemoteLowEnergyDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5], forcePair, false);
    }

    /**
     * Begins the pairing process with a given remote device.
     * <p>
     * If required, this method will initiate a connection with the remote
     * device.
     * <p>
     * <i>Note:</i> the pairing process itself is asynchronous. The caller can
     * determine the status of the pairing procedure by handling the
     * {@link DEVM#remoteDevicePairingStatusEvent
     * remoteDevicePairingStatusEvent}.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @param forcePair
     *            Forces the pairing process to begin anew even if there is an
     *            existing link key associated with the remote device.
     * @param stayConnected
     *            If {@code false}, and the call to
     *            {@code #pairWithRemoteDevice} caused a connection to be
     *            established with the remote LE device, the connection will
     *            automatically be closed when the pairing operation is
     *            complete. If the remote LE device was already connected when
     *            {@code #pairWithRemoteLowEnergyDevice} was called, this
     *            parameter is ignored.
     * @return Zero if successful, or a negative return error code.
     */
    public int pairWithRemoteLowEnergyDevice(BluetoothAddress remoteDevice, boolean forcePair, boolean stayConnected) {
        byte address[] = remoteDevice.internalByteArray();

        return pairWithRemoteLowEnergyDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5], forcePair, stayConnected);
    }

    /**
     * Cancels an ongoing pairing process with a given remote device.
     * <p>
     * <i>Note:</i> the pairing process itself is asynchronous. The caller can
     * determine the status of the pairing procedure by handling the
     * {@link DEVM#remoteDevicePairingStatusEvent
     * remoteDevicePairingStatusEvent}.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @return Zero if successful, or a negative return error code.
     */
    public int cancelPairWithRemoteDevice(BluetoothAddress remoteDevice) {
        byte address[] = remoteDevice.internalByteArray();

        return cancelPairWithRemoteDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5]);
    }

    /**
     * Removes all stored pairing information for a given remote device.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @return Zero if successful, or a negative return error code.
     */
    public int unpairRemoteDevice(BluetoothAddress remoteDevice) {
        byte address[] = remoteDevice.internalByteArray();

        return unPairRemoteDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5]);
    }

    /**
     * Removes all stored pairing information for a given remote low-energy
     * device.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @return Zero if successful, or a negative return error code.
     */
    public int unpairRemoteLowEnergyDevice(BluetoothAddress remoteDevice) {
        byte address[] = remoteDevice.internalByteArray();

        return unPairRemoteLowEnergyDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5]);
    }

    /**
     * Performs authentication with a given remote device.
     * <p>
     * <i>Note:</i> A connection to the device must already exist; this method
     * will <i>not</i> make a connection and then attempt to authenticate the
     * device.
     * <p>
     * <i>Note:</i> the results of the authentication attempt can be tracked via
     * the {@link EventCallback#remoteDeviceAuthenticationStatusEvent
     * remoteDeviceAuthenticationStatusEvent}.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @return Zero if successful, or a negative return error code.
     */
    public int authenticateRemoteDevice(BluetoothAddress remoteDevice) {
        byte address[] = remoteDevice.internalByteArray();

        return authenticateRemoteDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5]);
    }

    /**
     * Performs authentication with a given remote low-energy device.
     * <p>
     * <i>Note:</i> A connection to the device must already exist; this method
     * will <i>not</i> make a connection and then attempt to authenticate the
     * device.
     * <p>
     * <i>Note:</i> the results of the authentication attempt can be tracked via
     * the {@link EventCallback#remoteDeviceAuthenticationStatusEvent
     * remoteDeviceAuthenticationStatusEvent}.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @return Zero if successful, or a negative return error code.
     */
    public int authenticateRemoteLowEnergyDevice(BluetoothAddress remoteDevice) {
        byte address[] = remoteDevice.internalByteArray();

        return authenticateRemoteLowEnergyDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5]);
    }

    /**
     * Encrypts the connection to a given remote device.
     * <p>
     * <i>Note:</i> Bluetooth encryption requires that a link key be established
     * between two devices before the link can be encrypted; as such, this
     * method must be invoked <i>after</i> the link has been authenticated
     * (either locally or remotely).
     * <p>
     * <i>Note:</i> A connection to the device must already exist; this method
     * will <i>not</i> make a connection and then attempt to encrypt the
     * connection.
     * <p>
     * <i>Note:</i> the results of the encryption attempt can be tracked via the
     * {@link EventCallback#remoteDeviceEncryptionStatusEvent
     * remoteDeviceEncryptionStatusEvent}.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @return Zero if successful, or a negative return error code.
     */
    public int encryptRemoteDevice(BluetoothAddress remoteDevice) {
        byte address[] = remoteDevice.internalByteArray();

        return encryptRemoteDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5]);
    }

    /**
     * Encrypts the connection to a given remote low-energy device.
     * <p>
     * <i>Note:</i> Bluetooth encryption requires that a link key be established
     * between two devices before the link can be encrypted; as such, this
     * method must be invoked <i>after</i> the link has been authenticated
     * (either locally or remotely).
     * <p>
     * <i>Note:</i> A connection to the device must already exist; this method
     * will <i>not</i> make a connection and then attempt to encrypt the
     * connection.
     * <p>
     * <i>Note:</i> the results of the encryption attempt can be tracked via the
     * {@link EventCallback#remoteDeviceEncryptionStatusEvent
     * remoteDeviceEncryptionStatusEvent}.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @return Zero if successful, or a negative return error code.
     */
    public int encryptRemoteLowEnergyDevice(BluetoothAddress remoteDevice) {
        byte address[] = remoteDevice.internalByteArray();

        return encryptRemoteLowEnergyDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5]);
    }

    /**
     * Creates a connection to a given remote device.
     * <p>
     * <i>Note:</i> this method can be used to force authentication on an
     * outgoing link before a profile is connected.
     * <p>
     * <i>Note:</i> this method maintains an internal counter representing the
     * number of requested connections to a device. This counter is decremented
     * when a corresponding call to {@link #disconnectRemoteDevice} is made.
     * <p>
     * <i>Note:</i> the results of the connection attempt can be tracked via the
     * {@link EventCallback#remoteDeviceConnectionStatusEvent
     * remoteDeviceConnectionStatusEvent}.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @param connectFlags
     *            EnumSet specifying whether the connection should use
     *            authentication and/or encryption.
     * @return Zero if successful, or a negative return error code.
     * @see #disconnectRemoteDevice
     */
    public int connectWithRemoteDevice(BluetoothAddress remoteDevice, EnumSet<ConnectFlags> connectFlags) {
        int flags;
        byte address[] = remoteDevice.internalByteArray();

        flags = 0;
        if(connectFlags != null) {
            flags |= (connectFlags.contains(ConnectFlags.AUTHENTICATE) ? CONNECT_FLAGS_AUTHENTICATE : 0);
            flags |= (connectFlags.contains(ConnectFlags.ENCRYPT) ? CONNECT_FLAGS_AUTHENTICATE : 0);
        }

        return connectWithRemoteDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5], flags);
    }

    /**
     * Creates a connection to a given remote low-energy device.
     * <p>
     * <i>Note:</i> this method can be used to force authentication on an
     * outgoing link before a profile is connected.
     * <p>
     * <i>Note:</i> this method maintains an internal counter representing the
     * number of requested connections to a device. This counter is decremented
     * when a corresponding call to {@link #disconnectRemoteDevice} is made.
     * <p>
     * <i>Note:</i> the results of the connection attempt can be tracked via the
     * {@link EventCallback#remoteDeviceConnectionStatusEvent
     * remoteDeviceConnectionStatusEvent}.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @param connectFlags
     *            EnumSet specifying whether the connection should use
     *            authentication and/or encryption.
     * @return Zero if successful, or a negative return error code.
     * @see #disconnectRemoteDevice
     */
    public int connectWithRemoteLowEnergyDevice(BluetoothAddress remoteDevice, EnumSet<ConnectFlags> connectFlags) {
        int flags;
        byte address[] = remoteDevice.internalByteArray();

        flags = 0;
        if(connectFlags != null) {
            flags |= (connectFlags.contains(ConnectFlags.AUTHENTICATE) ? CONNECT_FLAGS_AUTHENTICATE : 0);
            flags |= (connectFlags.contains(ConnectFlags.ENCRYPT) ? CONNECT_FLAGS_AUTHENTICATE : 0);
        }

        return connectWithRemoteLowEnergyDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5], flags);
    }

    /**
     * Disconnects from a given remote device.
     * <p>
     * <i>Note:</i> Each call to {@link #connectWithRemoteDevice} increments an
     * internal counter representing of the number of requested connections to a
     * device; when this method is invoked, the counter is decremented.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @param force
     *            Physically Disconnects the device regardless of the internal
     *            connection count (see <i>Note</i>).
     *            <p>
     *            <i>Caution:</i> the 'force' parameter should be used with
     *            extreme care. Forcing physical disconnection from a remote
     *            device may negatively influence the behavior of other
     *            applications that have independently established connections
     *            to the device.
     * @return Zero if successful, or a negative return error code.
     * @see #connectWithRemoteDevice
     */
    public int disconnectRemoteDevice(BluetoothAddress remoteDevice, boolean force) {
        byte address[] = remoteDevice.internalByteArray();

        return disconnectRemoteDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5], force);
    }

    /**
     * Disconnects from a given remote low-energy device.
     * <p>
     * <i>Note:</i> Each call to {@link #connectWithRemoteLowEnergyDevice}
     * increments an internal counter representing of the number of requested
     * connections to a device; when this method is invoked, the counter is
     * decremented.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @param force
     *            Physically Disconnects the device regardless of the internal
     *            connection count (see <i>Note</i>).
     *            <p>
     *            <i>Caution:</i> the 'force' parameter should be used with
     *            extreme care. Forcing physical disconnection from a remote
     *            device may negatively influence the behavior of other
     *            applications that have independently established connections
     *            to the device.
     * @return Zero if successful, or a negative return error code.
     */
    public int disconnectRemoteLowEnergyDevice(BluetoothAddress remoteDevice, boolean force) {
        byte address[] = remoteDevice.internalByteArray();

        return disconnectRemoteLowEnergyDeviceNative(address[0], address[1], address[2], address[3], address[4], address[5], force);
    }

    /**
     * Sets the current link state to <i>Active</i> mode. This means that if the
     * device is currently in <i>Park</i> mode, <i>Hold</i> mode, or
     * <i>Sniff</i> mode, this method will attempt to change the current state
     * to <i>Active</i> mode.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @return Zero if the request was successfully submitted, a positive value
     *         if the link is already in <i>Active</i> mode, or a negative
     *         return error code.
     */
    public int setRemoteDeviceLinkActive(BluetoothAddress remoteDevice) {
        byte address[] = remoteDevice.internalByteArray();

        return setRemoteDeviceLinkActiveNative(address[0], address[1], address[2], address[3], address[4], address[5]);
    }

    //    /* The following function is provided to allow a mechanism for local */
    //    /* modules to create an SDP Service Record in the Local Devices SDP  */
    //    /* Database.  This function returns a positive value if successful or*/
    //    /* a negative return error code if there was an error.               */
    //    /* * NOTE * If this function returns success then the return value   */
    //    /*          represents the actual Service Record Handle of the       */
    //    /*          created Service Record.                                  */
    //    /* * NOTE * Upon Client De-registration all Registered SDP Records   */
    //    /*          (registered by the client) are deleted from the SDP      */
    //    /*          Database if the PersistClient parameters is FALSE.  If   */
    //    /*          PersistClient is TRUE the record will stay in the SDP    */
    //    /*          database until it is either explicitly deleted OR the    */
    //    /*          device is powered down.                                  */
    //    public int createServiceRecord(boolean persistClient, UUID[] uuidList) {
    //        if((uuidList == null) || uuidList.length < 1)
    //            throw new IllegalArgumentException("uuidList must contain at least one entry");
    //
    //        long[] lsb;
    //        long[] msb;
    //
    //        lsb = new long[uuidList.length];
    //        msb = new long[uuidList.length];
    //
    //        for(int i = 0; i < uuidList.length; i++) {
    //            lsb[i] = uuidList[i].getLeastSignificantBits();
    //            msb[i] = uuidList[i].getMostSignificantBits();
    //        }
    //
    //        return createServiceRecordNative(persistClient, lsb, msb);
    //    }
    //
    //    /* The following function is provided to allow a mechanism for local */
    //    /* modules to delete a previously registered SDP Service Record from */
    //    /* the Local Devices SDP Database.  This function returns zero if    */
    //    /* successful or a negative return error code if there was an error. */
    //    /* * NOTE * Upon Client De-registration all Registered SDP Records   */
    //    /*          (registered by the client) are deleted from the SDP      */
    //    /*          Database.                                                */
    //    public int deleteServiceRecord(int serviceRecordHandle) {
    //        return deleteServiceRecordNative(serviceRecordHandle);
    //    }
    //
    //    /* The following function is provided to allow a mechanism for local */
    //    /* modules to add a Service Record Attribute to a Service Record in  */
    //    /* the Local Devices SDP Database.  This function returns a zero if  */
    //    /* successful or a negative return error code if there was an error. */
    //    public int addServiceRecordAttribute(int serviceRecordHandle, int attributeID, SDP_Data_Element_t *SDP_Data_Element) {
    //        //FIXME
    //        return addServiceRecordAttributeNative(serviceRecordHandle, attributeID, );
    //    }
    //
    //    /* The following function is provided to allow a mechanism for local */
    //    /* modules to delete a Service Record Attribute from a Service Record*/
    //    /* in the Local Devices SDP Database.  This function returns a zero  */
    //    /* if successful or a negative return error code if there was an     */
    //    /* error.                                                            */
    //    public int deleteServiceRecordAttribute(int serviceRecordHandle, int attributeID) {
    //        return deleteServiceRecordAttributeNative(serviceRecordHandle, attributeID);
    //    }

    /*
     * pin code must be 16 bytes or less
     */
    /**
     * Sends a PIN code authentication response to a remote device.
     * <p>
     * <i>Note:</i> the PIN code must be 16 bytes or less.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @param pinCode
     *            The PIN code to send.
     * @return Zero for success, or a negative return error code.
     */
    public int sendPinCode(BluetoothAddress remoteDevice, byte[] pinCode) {
        byte address[] = remoteDevice.internalByteArray();

        return sendPinCodeNative(address[0], address[1], address[2], address[3], address[4], address[5], pinCode);
    }

    /**
     * Sends a user confirmation authentication response to a remote device.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @param accept
     *            Whether the user accepted the confirmation.
     * @return Zero for success, or a negative return error code.
     */
    public int sendUserConfirmation(BluetoothAddress remoteDevice, boolean accept) {
        byte address[] = remoteDevice.internalByteArray();

        return sendUserConfirmationNative(address[0], address[1], address[2], address[3], address[4], address[5], accept);
    }

    /**
     * Sends a passkey authentication response to a remote device.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @param passkey
     *            The passkey to send.
     * @return Zero for success, or a negative return error code.
     */
    public int sendPasskey(BluetoothAddress remoteDevice, int passkey) {
        byte address[] = remoteDevice.internalByteArray();

        return sendPasskeyNative(address[0], address[1], address[2], address[3], address[4], address[5], passkey);
    }

    /**
     * Sends out-of-band data to a remote device.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @param simplePairingHash
     *            The pairing hash value to send. (<i>Note:</i> this value must
     *            be 16 bytes.)
     * @param simplePairingRandomizer
     *            The pairing randomizer value to send. (<i>Note:</i> this value
     *            must be 16 bytes.)
     * @return @return Zero for success, or a negative return error code.
     */
    public int sendOutOfBandData(BluetoothAddress remoteDevice, byte[] simplePairingHash, byte[] simplePairingRandomizer) {
        if((simplePairingHash == null) || (simplePairingRandomizer == null))
            throw new NullPointerException("The simple pairing hash and randomizer must both be valid.");
        if((simplePairingHash.length != 16) || (simplePairingRandomizer.length != 16))
            throw new IllegalArgumentException("The simple pairing hash and randomizer must both be 16 bytes in length.");

        byte address[] = remoteDevice.internalByteArray();

        return sendOutOfBandDataNative(address[0], address[1], address[2], address[3], address[4], address[5], simplePairingHash, simplePairingRandomizer);
    }

    /**
     * Sends a description of the local device's I/O capabilities to a remote
     * device.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @param capability
     *            The capabilities of the local device.
     * @param outOfBandDataPresent
     *            Whether previously stored out-of-band data (associated with
     *            this particular remote device) is present on the local device.
     * @param mitmProtectionRequired
     *            Whether the local device requires man-in-the-middle
     *            protection.
     * @param bondingType
     *            The bonding requirements of the local device.
     * @return Zero for success, or a negative return error code.
     */
    public int sendIOCapabilities(BluetoothAddress remoteDevice, IOCapability capability, boolean outOfBandDataPresent, boolean mitmProtectionRequired, BondingType bondingType) {
        byte address[] = remoteDevice.internalByteArray();
        int ioCap;
        int bondType;

        switch(capability) {
        case DISPLAY_ONLY:
            ioCap = IO_CAPABILITY_DISPLAY_ONLY;
            break;
        case DISPLAY_YES_NO:
            ioCap = IO_CAPABILITY_DISPLAY_YES_NO;
            break;
        case KEYBOARD_ONLY:
            ioCap = IO_CAPABILITY_KEYBOARD_ONLY;
            break;
        case NO_INPUT_NO_OUTPUT:
        default:
            ioCap = IO_CAPABILITY_NO_INPUT_NO_OUTPUT;
            break;
        }

        switch(bondingType) {
        case NO_BONDING:
        default:
            bondType = BONDING_TYPE_NO_BONDING;
            break;
        case DEDICATED_BONDING:
            bondType = BONDING_TYPE_DEDICATED_BONDING;
            break;
        case GENERAL_BONDING:
            bondType = BONDING_TYPE_GENERAL_BONDING;
            break;
        }

        return sendIOCapabilitiesNative(address[0], address[1], address[2], address[3], address[4], address[5], ioCap, outOfBandDataPresent, mitmProtectionRequired, bondType);
    }

    /**
     * Sends a user confirmation authentication response to a remote low-energy device.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @param accept
     *            Whether the user accepted the confirmation.
     * @return Zero for success, or a negative return error code.
     */
    public int sendLowEnergyUserConfirmation(BluetoothAddress remoteDevice, boolean accept) {
        byte address[] = remoteDevice.internalByteArray();

        return sendLowEnergyUserConfirmationNative(address[0], address[1], address[2], address[3], address[4], address[5], accept);
    }

    /**
     * Sends a passkey authentication response to a remote low-energy device.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @param passkey
     *            The passkey to send.
     * @return Zero for success, or a negative return error code.
     */
    public int sendLowEnergyPasskey(BluetoothAddress remoteDevice, int passkey) {
        byte address[] = remoteDevice.internalByteArray();

        return sendLowEnergyPasskeyNative(address[0], address[1], address[2], address[3], address[4], address[5], passkey);
    }

    /**
     * Sends out-of-band pairing data to a remote low-energy device.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @param encryptionKey
     *            The encryption key to send. (<i>Note:</i> this value must be
     *            16 bytes.)
     * @return @return Zero for success, or a negative return error code.
     */
    public int sendLowEnergyOutOfBandData(BluetoothAddress remoteDevice, byte[] encryptionKey) {
        if(encryptionKey == null)
            throw new NullPointerException("The encryption key must be valid.");
        if(encryptionKey.length != 16)
            throw new IllegalArgumentException("The encryption key must be 16 bytes in length.");

        byte address[] = remoteDevice.internalByteArray();

        return sendLowEnergyOutOfBandDataNative(address[0], address[1], address[2], address[3], address[4], address[5], encryptionKey);
    }

    /**
     * Sends a description of the local device's I/O capabilities to a remote
     * low-energy device.
     *
     * @param remoteDevice
     *            The Bluetooth address of the remote device.
     * @param capability
     *            The capabilities of the local device.
     * @param outOfBandDataPresent
     *            Whether previously stored out-of-band data (associated with
     *            this particular remote device) is present on the local device.
     * @param mitmProtectionRequired
     *            Whether the local device requires man-in-the-middle
     *            protection.
     * @param bondingType
     *            The bonding requirements of the local device.
     * @return Zero for success, or a negative return error code.
     */
    public int sendLowEnergyIOCapabilities(BluetoothAddress remoteDevice, LowEnergyIOCapability capability, boolean outOfBandDataPresent, boolean mitmProtectionRequired, LowEnergyBondingType bondingType) {
        byte address[] = remoteDevice.internalByteArray();
        int ioCap;
        int bondType;

        switch(capability) {
        case DISPLAY_ONLY:
            ioCap = LE_IO_CAPABILITY_DISPLAY_ONLY;
            break;
        case DISPLAY_YES_NO:
            ioCap = LE_IO_CAPABILITY_DISPLAY_YES_NO;
            break;
        case KEYBOARD_ONLY:
            ioCap = LE_IO_CAPABILITY_KEYBOARD_ONLY;
            break;
        case NO_INPUT_NO_OUTPUT:
        default:
            ioCap = LE_IO_CAPABILITY_NO_INPUT_NO_OUTPUT;
            break;
        case KEYBOARD_DISPLAY:
            ioCap = LE_IO_CAPABILITY_KEYBOARD_DISPLAY;
            break;
        }

        switch(bondingType) {
        case NO_BONDING:
        default:
            bondType = LE_BONDING_TYPE_NO_BONDING;
            break;
        case BONDING:
            bondType = LE_BONDING_TYPE_BONDING;
            break;
        }

        return sendLowEnergyIOCapabilitiesNative(address[0], address[1], address[2], address[3], address[4], address[5], ioCap, outOfBandDataPresent, mitmProtectionRequired, bondType);
    }


    /* Event Callbacks */

    private void devicePoweredOnEvent() {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.devicePoweredOnEvent();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void devicePoweringOffEvent(int poweringOffTimeout) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.devicePoweringOffEvent(poweringOffTimeout);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void devicePoweredOffEvent() {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.devicePoweredOffEvent();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void localDevicePropertiesChangedEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int classOfDevice, String deviceName, int hciVersion, int hciRevision, int lmpVersion, int lmpSubversion, int deviceManufacturer, int localDeviceFlags, boolean discoverableMode, int discoverableTimeout, boolean connectableMode, int connectableTimeout, boolean pairableMode, int pairableTimeout, int changedFieldsMask) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            LocalDeviceProperties devProps = new LocalDeviceProperties();
            EnumSet<LocalPropertyField> changeMask = EnumSet.noneOf(LocalPropertyField.class);

            devProps.deviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
            devProps.classOfDevice = classOfDevice;
            devProps.deviceName = deviceName;
            devProps.hciVersion = hciVersion;
            devProps.hciRevision = hciRevision;
            devProps.lmpVersion = lmpVersion;
            devProps.lmpSubversion = lmpSubversion;
            devProps.deviceManufacturer = deviceManufacturer;
            devProps.discoverableMode = discoverableMode;
            devProps.discoverableModeTimeout = discoverableTimeout;
            devProps.connectableMode = connectableMode;
            devProps.connectableModeTimeout = connectableTimeout;
            devProps.pairableMode = pairableMode;
            devProps.pairableModeTimeout = pairableTimeout;

            devProps.localDeviceFlags = EnumSet.noneOf(LocalDeviceFlags.class);
            if((localDeviceFlags & LOCAL_DEVICE_FLAGS_DEVICE_DISCOVERY_IN_PROGRESS) == LOCAL_DEVICE_FLAGS_DEVICE_DISCOVERY_IN_PROGRESS)
                devProps.localDeviceFlags.add(LocalDeviceFlags.DEVICE_DISCOVERY_IN_PROGRESS);
            if((localDeviceFlags & LOCAL_DEVICE_FLAGS_LE_SCANNING_IN_PROGRESS) == LOCAL_DEVICE_FLAGS_LE_SCANNING_IN_PROGRESS)
                devProps.localDeviceFlags.add(LocalDeviceFlags.LE_SCANNING_IN_PROGRESS);
            if((localDeviceFlags & LOCAL_DEVICE_FLAGS_LE_ADVERTISING_IN_PROGRESS) == LOCAL_DEVICE_FLAGS_LE_ADVERTISING_IN_PROGRESS)
                devProps.localDeviceFlags.add(LocalDeviceFlags.LE_ADVERTISING_IN_PROGRESS);
            if((localDeviceFlags & LOCAL_DEVICE_FLAGS_LE_ROLE_IS_CURRENTLY_SLAVE) == LOCAL_DEVICE_FLAGS_LE_ROLE_IS_CURRENTLY_SLAVE)
                devProps.localDeviceFlags.add(LocalDeviceFlags.LE_ROLE_IS_CURRENTLY_SLAVE);
            if((localDeviceFlags & LOCAL_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY) == LOCAL_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY)
                devProps.localDeviceFlags.add(LocalDeviceFlags.DEVICE_SUPPORTS_LOW_ENERGY);
            if((localDeviceFlags & LOCAL_DEVICE_FLAGS_DEVICE_SUPPORTS_ANT_PLUS) == LOCAL_DEVICE_FLAGS_DEVICE_SUPPORTS_ANT_PLUS)
                devProps.localDeviceFlags.add(LocalDeviceFlags.DEVICE_SUPPORTS_ANT_PLUS);

            if((changedFieldsMask & LOCAL_PROPERTY_FIELD_CLASS_OF_DEVICE) == LOCAL_PROPERTY_FIELD_CLASS_OF_DEVICE)
                changeMask.add(LocalPropertyField.CLASS_OF_DEVICE);
            if((changedFieldsMask & LOCAL_PROPERTY_FIELD_DEVICE_NAME) == LOCAL_PROPERTY_FIELD_DEVICE_NAME)
                changeMask.add(LocalPropertyField.DEVICE_NAME);
            if((changedFieldsMask & LOCAL_PROPERTY_FIELD_DISCOVERABLE_MODE) == LOCAL_PROPERTY_FIELD_DISCOVERABLE_MODE)
                changeMask.add(LocalPropertyField.DISCOVERABLE_MODE);
            if((changedFieldsMask & LOCAL_PROPERTY_FIELD_CONNECTABLE_MODE) == LOCAL_PROPERTY_FIELD_CONNECTABLE_MODE)
                changeMask.add(LocalPropertyField.CONNECTABLE_MODE);
            if((changedFieldsMask & LOCAL_PROPERTY_FIELD_PAIRABLE_MODE) == LOCAL_PROPERTY_FIELD_PAIRABLE_MODE)
                changeMask.add(LocalPropertyField.PAIRABLE_MODE);
            if((changedFieldsMask & LOCAL_PROPERTY_FIELD_DEVICE_FLAGS) == LOCAL_PROPERTY_FIELD_DEVICE_FLAGS)
                changeMask.add(LocalPropertyField.DEVICE_FLAGS);
            if((changedFieldsMask & LOCAL_PROPERTY_FIELD_DEVICE_APPEARANCE) == LOCAL_PROPERTY_FIELD_DEVICE_APPEARANCE)
                changeMask.add(LocalPropertyField.DEVICE_APPEARANCE);

            try {
                callbackHandler.localDevicePropertiesChangedEvent(devProps, changeMask);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void discoveryStartedEvent() {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.discoveryStartedEvent();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void discoveryStoppedEvent() {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.discoveryStoppedEvent();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void remoteDeviceFoundEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int classOfDevice, String deviceName, int remoteDeviceFlags, int rssi, int transmitPower, int sniffInterval, String friendlyName, int applicationInfo, int lowEnergyAddressType, int lowEnergyRSSI, int lowEnergyTransmitPower, int deviceAppearance, byte pAddr1, byte pAddr2, byte pAddr3, byte pAddr4, byte pAddr5, byte pAddr6) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            RemoteDeviceProperties props = new RemoteDeviceProperties();

            props.deviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
            props.classOfDevice = classOfDevice;
            props.deviceName = deviceName;
            props.remoteDeviceFlags = EnumSet.noneOf(RemoteDeviceFlags.class);
            props.rssi = rssi;
            props.transmitPower = transmitPower;
            props.sniffInterval = sniffInterval;

            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_NAME_KNOWN) == REMOTE_DEVICE_FLAGS_NAME_KNOWN)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.NAME_KNOWN);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_APPLICATION_DATA_VALID) == REMOTE_DEVICE_FLAGS_APPLICATION_DATA_VALID)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.APPLICATION_DATA_VALID);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED) == REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.CURRENTLY_PAIRED);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED) == REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.CURRENTLY_CONNECTED);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_ENCRYPTED) == REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_ENCRYPTED)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LINK_CURRENTLY_ENCRYPTED);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_SNIFF_MODE) == REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_SNIFF_MODE)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LINK_CURRENTLY_SNIFF_MODE);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LINK_INITIATED_LOCALLY) == REMOTE_DEVICE_FLAGS_LINK_INITIATED_LOCALLY)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LINK_INITIATED_LOCALLY);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_AUTHENTICATED_KEY) == REMOTE_DEVICE_FLAGS_AUTHENTICATED_KEY)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.AUTHENTICATED_KEY);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_SERVICES_KNOWN) == REMOTE_DEVICE_FLAGS_SERVICES_KNOWN)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.SERVICES_KNOWN);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_TX_POWER_KNOWN) == REMOTE_DEVICE_FLAGS_TX_POWER_KNOWN)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.TX_POWER_KNOWN);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_EIR_DATA_KNOWN) == REMOTE_DEVICE_FLAGS_EIR_DATA_KNOWN)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.EIR_DATA_KNOWN);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_SERVICES_KNOWN) == REMOTE_DEVICE_FLAGS_LE_SERVICES_KNOWN)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LE_SERVICES_KNOWN);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN) == REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LE_APPEARANCE_KNOWN);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED_OVER_LE) == REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED_OVER_LE)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.CURRENTLY_PAIRED_OVER_LE);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED_OVER_LE) == REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED_OVER_LE)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.CURRENTLY_CONNECTED_OVER_LE);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_LINK_CURRENTLY_ENCRYPTED) == REMOTE_DEVICE_FLAGS_LE_LINK_CURRENTLY_ENCRYPTED)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LE_LINK_CURRENTLY_ENCRYPTED);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_AUTHENTICATED_KEY) == REMOTE_DEVICE_FLAGS_LE_AUTHENTICATED_KEY)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LE_AUTHENTICATED_KEY);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_LINK_INITIATED_LOCALLY) == REMOTE_DEVICE_FLAGS_LE_LINK_INITIATED_LOCALLY)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LE_LINK_INITIATED_LOCALLY);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN) == REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LE_TX_POWER_KNOWN);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY) == REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_BR_EDR) == REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_BR_EDR)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.SUPPORTS_BR_EDR);

            if(props.remoteDeviceFlags.contains(RemoteDeviceFlags.APPLICATION_DATA_VALID)) {
                props.applicationData = new RemoteDeviceApplicationData();

                props.applicationData.friendlyName = friendlyName;
                props.applicationData.applicationInfo = applicationInfo;
            }

            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY) == REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY) {
                switch(lowEnergyAddressType) {
                case ADDRESS_TYPE_PUBLIC:
                    props.lowEnergyAddressType = AddressType.PUBLIC;
                    break;
                case ADDRESS_TYPE_STATIC:
                    props.lowEnergyAddressType = AddressType.STATIC;
                    break;
                case ADDRESS_TYPE_PRIVATE_RESOLVABLE:
                    props.lowEnergyAddressType = AddressType.PUBLIC;
                    break;
                case ADDRESS_TYPE_PRIVATE_NONRESOLVABLE:
                    props.lowEnergyAddressType = AddressType.PUBLIC;
                    break;
                default:
                    props.lowEnergyAddressType = null;
                }

                props.lowEnergyRSSI = lowEnergyRSSI;

                if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN) == REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN)
                    props.lowEnergyTransmitPower = lowEnergyTransmitPower;
                else
                    props.lowEnergyTransmitPower = 0;

                if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN) == REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN)
                    props.deviceAppearance = deviceAppearance;
                else
                    props.deviceAppearance = 0;

                props.priorResolvableAddress = new BluetoothAddress(pAddr1, pAddr2, pAddr3, pAddr4, pAddr5, pAddr6);
            } else {
                props.lowEnergyAddressType   = null;
                props.lowEnergyRSSI          = 0;
                props.lowEnergyTransmitPower = 0;
                props.deviceAppearance       = 0;
                props.priorResolvableAddress = null;
            }

            try {
                callbackHandler.remoteDeviceFoundEvent(props);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void remoteDeviceDeletedEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.remoteDeviceDeletedEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void remoteDevicePropertiesChangedEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int classOfDevice, String deviceName, int remoteDeviceFlags, int rssi, int transmitPower, int sniffInterval, String friendlyName, int applicationInfo, int changedFieldsMask, int lowEnergyAddressType, int lowEnergyRSSI, int lowEnergyTransmitPower, int deviceAppearance, byte pAddr1, byte pAddr2, byte pAddr3, byte pAddr4, byte pAddr5, byte pAddr6) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            RemoteDeviceProperties props = new RemoteDeviceProperties();
            EnumSet<RemotePropertyField> changeMask = EnumSet.noneOf(RemotePropertyField.class);

            props.deviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
            props.classOfDevice = classOfDevice;
            props.deviceName = deviceName;
            props.remoteDeviceFlags = EnumSet.noneOf(RemoteDeviceFlags.class);
            props.rssi = rssi;
            props.transmitPower = transmitPower;
            props.sniffInterval = sniffInterval;

            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_NAME_KNOWN) == REMOTE_DEVICE_FLAGS_NAME_KNOWN)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.NAME_KNOWN);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_APPLICATION_DATA_VALID) == REMOTE_DEVICE_FLAGS_APPLICATION_DATA_VALID)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.APPLICATION_DATA_VALID);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED) == REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.CURRENTLY_PAIRED);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED) == REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.CURRENTLY_CONNECTED);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_ENCRYPTED) == REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_ENCRYPTED)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LINK_CURRENTLY_ENCRYPTED);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_SNIFF_MODE) == REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_SNIFF_MODE)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LINK_CURRENTLY_SNIFF_MODE);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LINK_INITIATED_LOCALLY) == REMOTE_DEVICE_FLAGS_LINK_INITIATED_LOCALLY)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LINK_INITIATED_LOCALLY);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_AUTHENTICATED_KEY) == REMOTE_DEVICE_FLAGS_AUTHENTICATED_KEY)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.AUTHENTICATED_KEY);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_SERVICES_KNOWN) == REMOTE_DEVICE_FLAGS_SERVICES_KNOWN)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.SERVICES_KNOWN);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_TX_POWER_KNOWN) == REMOTE_DEVICE_FLAGS_TX_POWER_KNOWN)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.TX_POWER_KNOWN);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_EIR_DATA_KNOWN) == REMOTE_DEVICE_FLAGS_EIR_DATA_KNOWN)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.EIR_DATA_KNOWN);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_SERVICES_KNOWN) == REMOTE_DEVICE_FLAGS_LE_SERVICES_KNOWN)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LE_SERVICES_KNOWN);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN) == REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LE_APPEARANCE_KNOWN);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED_OVER_LE) == REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED_OVER_LE)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.CURRENTLY_PAIRED_OVER_LE);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED_OVER_LE) == REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED_OVER_LE)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.CURRENTLY_CONNECTED_OVER_LE);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_LINK_CURRENTLY_ENCRYPTED) == REMOTE_DEVICE_FLAGS_LE_LINK_CURRENTLY_ENCRYPTED)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LE_LINK_CURRENTLY_ENCRYPTED);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_AUTHENTICATED_KEY) == REMOTE_DEVICE_FLAGS_LE_AUTHENTICATED_KEY)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LE_AUTHENTICATED_KEY);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_LINK_INITIATED_LOCALLY) == REMOTE_DEVICE_FLAGS_LE_LINK_INITIATED_LOCALLY)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LE_LINK_INITIATED_LOCALLY);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN) == REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LE_TX_POWER_KNOWN);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY) == REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_BR_EDR) == REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_BR_EDR)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.SUPPORTS_BR_EDR);

            if(props.remoteDeviceFlags.contains(RemoteDeviceFlags.APPLICATION_DATA_VALID)) {
                props.applicationData = new RemoteDeviceApplicationData();

                props.applicationData.friendlyName = friendlyName;
                props.applicationData.applicationInfo = applicationInfo;
            }

            if((changedFieldsMask & REMOTE_PROPERTY_FIELD_CLASS_OF_DEVICE) == REMOTE_PROPERTY_FIELD_CLASS_OF_DEVICE)
                changeMask.add(RemotePropertyField.CLASS_OF_DEVICE);
            if((changedFieldsMask & REMOTE_PROPERTY_FIELD_DEVICE_NAME) == REMOTE_PROPERTY_FIELD_DEVICE_NAME)
                changeMask.add(RemotePropertyField.DEVICE_NAME);
            if((changedFieldsMask & REMOTE_PROPERTY_FIELD_APPLICATION_DATA) == REMOTE_PROPERTY_FIELD_APPLICATION_DATA)
                changeMask.add(RemotePropertyField.APPLICATION_DATA);
            if((changedFieldsMask & REMOTE_PROPERTY_FIELD_DEVICE_FLAGS) == REMOTE_PROPERTY_FIELD_DEVICE_FLAGS)
                changeMask.add(RemotePropertyField.DEVICE_FLAGS);
            if((changedFieldsMask & REMOTE_PROPERTY_FIELD_RSSI) == REMOTE_PROPERTY_FIELD_RSSI)
                changeMask.add(RemotePropertyField.RSSI);
            if((changedFieldsMask & REMOTE_PROPERTY_FIELD_PAIRING_STATE) == REMOTE_PROPERTY_FIELD_PAIRING_STATE)
                changeMask.add(RemotePropertyField.PAIRING_STATE);
            if((changedFieldsMask & REMOTE_PROPERTY_FIELD_CONNECTION_STATE) == REMOTE_PROPERTY_FIELD_CONNECTION_STATE)
                changeMask.add(RemotePropertyField.CONNECTION_STATE);
            if((changedFieldsMask & REMOTE_PROPERTY_FIELD_ENCRYPTION_STATE) == REMOTE_PROPERTY_FIELD_ENCRYPTION_STATE)
                changeMask.add(RemotePropertyField.ENCRYPTION_STATE);
            if((changedFieldsMask & REMOTE_PROPERTY_FIELD_SNIFF_STATE) == REMOTE_PROPERTY_FIELD_SNIFF_STATE)
                changeMask.add(RemotePropertyField.SNIFF_STATE);
            if((changedFieldsMask & REMOTE_PROPERTY_FIELD_SERVICES_STATE) == REMOTE_PROPERTY_FIELD_SERVICES_STATE)
                changeMask.add(RemotePropertyField.SERVICES_STATE);
            if((changedFieldsMask & REMOTE_PROPERTY_FIELD_LE_RSSI) == REMOTE_PROPERTY_FIELD_LE_RSSI)
                changeMask.add(RemotePropertyField.LE_RSSI);
            if((changedFieldsMask & REMOTE_PROPERTY_FIELD_LE_PAIRING_STATE) == REMOTE_PROPERTY_FIELD_LE_PAIRING_STATE)
                changeMask.add(RemotePropertyField.LE_PAIRING_STATE);
            if((changedFieldsMask & REMOTE_PROPERTY_FIELD_LE_CONNECTION_STATE) == REMOTE_PROPERTY_FIELD_LE_CONNECTION_STATE)
                changeMask.add(RemotePropertyField.LE_CONNECTION_STATE);
            if((changedFieldsMask & REMOTE_PROPERTY_FIELD_LE_ENCRYPTION_STATE) == REMOTE_PROPERTY_FIELD_LE_ENCRYPTION_STATE)
                changeMask.add(RemotePropertyField.LE_ENCRYPTION_STATE);
            if((changedFieldsMask & REMOTE_PROPERTY_FIELD_PRIOR_RESOLVABLE_ADDRESS) == REMOTE_PROPERTY_FIELD_PRIOR_RESOLVABLE_ADDRESS)
                changeMask.add(RemotePropertyField.PRIOR_RESOLVABLE_ADDRESS);
            if((changedFieldsMask & REMOTE_PROPERTY_FIELD_DEVICE_APPEARANCE) == REMOTE_PROPERTY_FIELD_DEVICE_APPEARANCE)
                changeMask.add(RemotePropertyField.DEVICE_APPEARANCE);
            if((changedFieldsMask & REMOTE_PROPERTY_FIELD_LE_SERVICES_STATE) == REMOTE_PROPERTY_FIELD_LE_SERVICES_STATE)
                changeMask.add(RemotePropertyField.LE_SERVICES_STATE);

            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY) == REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY) {
                switch(lowEnergyAddressType) {
                case ADDRESS_TYPE_PUBLIC:
                    props.lowEnergyAddressType = AddressType.PUBLIC;
                    break;
                case ADDRESS_TYPE_STATIC:
                    props.lowEnergyAddressType = AddressType.STATIC;
                    break;
                case ADDRESS_TYPE_PRIVATE_RESOLVABLE:
                    props.lowEnergyAddressType = AddressType.PUBLIC;
                    break;
                case ADDRESS_TYPE_PRIVATE_NONRESOLVABLE:
                    props.lowEnergyAddressType = AddressType.PUBLIC;
                    break;
                default:
                    props.lowEnergyAddressType = null;
                }

                props.lowEnergyRSSI = lowEnergyRSSI;

                if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN) == REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN)
                    props.lowEnergyTransmitPower = lowEnergyTransmitPower;
                else
                    props.lowEnergyTransmitPower = 0;

                if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN) == REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN)
                    props.deviceAppearance = deviceAppearance;
                else
                    props.deviceAppearance = 0;

                props.priorResolvableAddress = new BluetoothAddress(pAddr1, pAddr2, pAddr3, pAddr4, pAddr5, pAddr6);
            } else {
                props.lowEnergyAddressType   = null;
                props.lowEnergyRSSI          = 0;
                props.lowEnergyTransmitPower = 0;
                props.deviceAppearance       = 0;
                props.priorResolvableAddress = null;
            }

            try {
                callbackHandler.remoteDevicePropertiesChangedEvent(props, changeMask);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void remoteDevicePropertiesStatusEvent(boolean success, byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int classOfDevice, String deviceName, int remoteDeviceFlags, int rssi, int transmitPower, int sniffInterval, String friendlyName, int applicationInfo, int lowEnergyAddressType, int lowEnergyRSSI, int lowEnergyTransmitPower, int deviceAppearance, byte pAddr1, byte pAddr2, byte pAddr3, byte pAddr4, byte pAddr5, byte pAddr6) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            RemoteDeviceProperties props = new RemoteDeviceProperties();

            props.deviceAddress = new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6);
            props.classOfDevice = classOfDevice;
            props.deviceName = deviceName;
            props.remoteDeviceFlags = EnumSet.noneOf(RemoteDeviceFlags.class);
            props.rssi = rssi;
            props.transmitPower = transmitPower;
            props.sniffInterval = sniffInterval;

            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_NAME_KNOWN) == REMOTE_DEVICE_FLAGS_NAME_KNOWN)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.NAME_KNOWN);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_APPLICATION_DATA_VALID) == REMOTE_DEVICE_FLAGS_APPLICATION_DATA_VALID)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.APPLICATION_DATA_VALID);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED) == REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.CURRENTLY_PAIRED);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED) == REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.CURRENTLY_CONNECTED);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_ENCRYPTED) == REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_ENCRYPTED)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LINK_CURRENTLY_ENCRYPTED);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_SNIFF_MODE) == REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_SNIFF_MODE)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LINK_CURRENTLY_SNIFF_MODE);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LINK_INITIATED_LOCALLY) == REMOTE_DEVICE_FLAGS_LINK_INITIATED_LOCALLY)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LINK_INITIATED_LOCALLY);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_AUTHENTICATED_KEY) == REMOTE_DEVICE_FLAGS_AUTHENTICATED_KEY)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.AUTHENTICATED_KEY);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_SERVICES_KNOWN) == REMOTE_DEVICE_FLAGS_SERVICES_KNOWN)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.SERVICES_KNOWN);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_TX_POWER_KNOWN) == REMOTE_DEVICE_FLAGS_TX_POWER_KNOWN)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.TX_POWER_KNOWN);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_EIR_DATA_KNOWN) == REMOTE_DEVICE_FLAGS_EIR_DATA_KNOWN)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.EIR_DATA_KNOWN);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_SERVICES_KNOWN) == REMOTE_DEVICE_FLAGS_LE_SERVICES_KNOWN)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LE_SERVICES_KNOWN);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN) == REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LE_APPEARANCE_KNOWN);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED_OVER_LE) == REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED_OVER_LE)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.CURRENTLY_PAIRED_OVER_LE);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED_OVER_LE) == REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED_OVER_LE)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.CURRENTLY_CONNECTED_OVER_LE);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_LINK_CURRENTLY_ENCRYPTED) == REMOTE_DEVICE_FLAGS_LE_LINK_CURRENTLY_ENCRYPTED)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LE_LINK_CURRENTLY_ENCRYPTED);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_AUTHENTICATED_KEY) == REMOTE_DEVICE_FLAGS_LE_AUTHENTICATED_KEY)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LE_AUTHENTICATED_KEY);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_LINK_INITIATED_LOCALLY) == REMOTE_DEVICE_FLAGS_LE_LINK_INITIATED_LOCALLY)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LE_LINK_INITIATED_LOCALLY);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN) == REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.LE_TX_POWER_KNOWN);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY) == REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY);
            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_BR_EDR) == REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_BR_EDR)
                props.remoteDeviceFlags.add(RemoteDeviceFlags.SUPPORTS_BR_EDR);

            if(props.remoteDeviceFlags.contains(RemoteDeviceFlags.APPLICATION_DATA_VALID)) {
                props.applicationData = new RemoteDeviceApplicationData();

                props.applicationData.friendlyName = friendlyName;
                props.applicationData.applicationInfo = applicationInfo;
            }

            if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY) == REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY) {
                switch(lowEnergyAddressType) {
                case ADDRESS_TYPE_PUBLIC:
                    props.lowEnergyAddressType = AddressType.PUBLIC;
                    break;
                case ADDRESS_TYPE_STATIC:
                    props.lowEnergyAddressType = AddressType.STATIC;
                    break;
                case ADDRESS_TYPE_PRIVATE_RESOLVABLE:
                    props.lowEnergyAddressType = AddressType.PUBLIC;
                    break;
                case ADDRESS_TYPE_PRIVATE_NONRESOLVABLE:
                    props.lowEnergyAddressType = AddressType.PUBLIC;
                    break;
                default:
                    props.lowEnergyAddressType = null;
                }

                props.lowEnergyRSSI = lowEnergyRSSI;

                if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN) == REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN)
                    props.lowEnergyTransmitPower = lowEnergyTransmitPower;
                else
                    props.lowEnergyTransmitPower = 0;

                if((remoteDeviceFlags & REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN) == REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN)
                    props.deviceAppearance = deviceAppearance;
                else
                    props.deviceAppearance = 0;

                props.priorResolvableAddress = new BluetoothAddress(pAddr1, pAddr2, pAddr3, pAddr4, pAddr5, pAddr6);
            } else {
                props.lowEnergyAddressType   = null;
                props.lowEnergyRSSI          = 0;
                props.lowEnergyTransmitPower = 0;
                props.deviceAppearance       = 0;
                props.priorResolvableAddress = null;
            }

            try {
                callbackHandler.remoteDevicePropertiesStatusEvent(success, props);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void remoteDeviceServicesStatusEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean success) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.remoteDeviceServicesStatusEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), success);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void remoteLowEnergyDeviceServicesStatusEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean success) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.remoteLowEnergyDeviceServicesStatusEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), success);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void remoteDevicePairingStatusEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean success, int authenticationStatus) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.remoteDevicePairingStatusEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), success, authenticationStatus);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void remoteDeviceAuthenticationStatusEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int status) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.remoteDeviceAuthenticationStatusEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), status);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void remoteDeviceEncryptionStatusEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int status) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.remoteDeviceEncryptionStatusEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), status);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void remoteDeviceConnectionStatusEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int status) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.remoteDeviceConnectionStatusEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), status);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void deviceScanStartedEvent() {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.deviceScanStartedEvent();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void deviceScanStoppedEvent() {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.deviceScanStoppedEvent();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void deviceAdvertisingStartedEvent() {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.deviceAdvertisingStarted();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void deviceAdvertisingStoppedEvent() {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.deviceAdvertisingStopped();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void remoteLowEnergyDeviceAddressChangedEvent(byte priorAddr1, byte priorAddr2, byte priorAddr3, byte priorAddr4, byte priorAddr5, byte priorAddr6, byte currentAddr1, byte currentAddr2, byte currentAddr3, byte currentAddr4, byte currentAddr5, byte currentAddr6) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.remoteLowEnergyDeviceAddressChangedEvent(new BluetoothAddress(priorAddr1, priorAddr2, priorAddr3, priorAddr4, priorAddr5, priorAddr6), new BluetoothAddress(currentAddr1, currentAddr2, currentAddr3, currentAddr4, currentAddr5, currentAddr6));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void pinCodeRequestEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        if(authHandler != null) {
            try {
                authHandler.pinCodeRequestEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void userConfirmationRequestEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int passkey) {
        if(authHandler != null) {
            try {
                authHandler.userConfirmationRequestEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), passkey);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void passkeyRequestEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        if(authHandler != null) {
            try {
                authHandler.passkeyRequestEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void passkeyIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int passkey) {
        if(authHandler != null) {
            try {
                authHandler.passkeyIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), passkey);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void keypressIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int type) {
        if(authHandler != null) {
            Keypress keypressType;

            switch(type) {
            case KEYPRESS_ENTRY_STARTED:
                keypressType = Keypress.ENTRY_STARTED;
                break;
            case KEYPRESS_DIGIT_ENTERED:
                keypressType = Keypress.DIGIT_ENTERED;
                break;
            case KEYPRESS_DIGIT_ERASED:
                keypressType = Keypress.DIGIT_ERASED;
                break;
            case KEYPRESS_CLEARED:
                keypressType = Keypress.CLEARED;
                break;
            case KEYPRESS_ENTRY_COMPLETED:
            default:
                keypressType = Keypress.ENTRY_COMPLETED;
                break;
            }

            try {
                authHandler.keypressIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), keypressType);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void outOfBandDataRequestEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        if(authHandler != null) {
            try {
                authHandler.outOfBandDataRequestEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void ioCapabilitiesRequestEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        if(authHandler != null) {
            try {
                authHandler.ioCapabilitiesRequestEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void authenticationStatusEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int status) {
        if(authHandler != null) {
            try {
                authHandler.authenticationStatusEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), status);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void lowEnergyUserConfirmationRequestEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int passkey) {
        if(authHandler != null) {
            try {
                authHandler.lowEnergyUserConfirmationRequestEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), passkey);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void lowEnergyPasskeyRequestEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        if(authHandler != null) {
            try {
                authHandler.lowEnergyPasskeyRequestEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void lowEnergyPasskeyIndicationEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int passkey) {
        if(authHandler != null) {
            try {
                authHandler.lowEnergyPasskeyIndicationEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6), passkey);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void lowEnergyOutOfBandDataRequestEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        if(authHandler != null) {
            try {
                authHandler.lowEnergyOutOfBandDataRequestEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void lowEnergyIOCapabilitiesRequestEvent(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6) {
        if(authHandler != null) {
            try {
                authHandler.lowEnergyIOCapabilitiesRequestEvent(new BluetoothAddress(rAddr1, rAddr2, rAddr3, rAddr4, rAddr5, rAddr6));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Receiver for notifications of events from the Device Manager interface.
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
         * The local device has been powered on.
         */
        public void devicePoweredOnEvent();

        /**
         * The local device is powering off.
         *
         * @param poweringOffTimeout
         *            Specifies the timeout duration (in milliseconds) that will
         *            be used to wait for all dispatch power down callbacks to
         *            acknowledge that the device is powering down.
         */
        public void devicePoweringOffEvent(int poweringOffTimeout);

        /**
         * The local device has powered off.
         */
        public void devicePoweredOffEvent();

        /**
         * One or more of the properties of the local device has changed.
         *
         * @param localProperties
         *            The new properties of the local device.
         * @param changedFields
         *            An EnumSet specifying which property fields have changed.
         */
        public void localDevicePropertiesChangedEvent(LocalDeviceProperties localProperties, EnumSet<LocalPropertyField> changedFields);

        /**
         * Remote device discovery has started.
         */
        public void discoveryStartedEvent();

        /**
         * Remote device discovery has stopped.
         */
        public void discoveryStoppedEvent();

        /**
         * A remote device has been found and added to the internally maintained
         * remote device list..
         *
         * @param properties
         *            The properties of the device.
         */
        public void remoteDeviceFoundEvent(RemoteDeviceProperties properties);

        /**
         * A remote device has been deleted from the internally maintained
         * remote device list.
         *
         * @param remoteDevice
         *            The Bluetooth address of the device.
         */
        public void remoteDeviceDeletedEvent(BluetoothAddress remoteDevice);

        /**
         * One or more properties of a remote device has changed.
         *
         * @param remoteProperties
         *            The new properties of the remote device.
         * @param changedFields
         *            An EnumSet specifying which property fields have changed.
         */
        public void remoteDevicePropertiesChangedEvent(RemoteDeviceProperties remoteProperties, EnumSet<RemotePropertyField> changedFields);

        /**
         * Invoked in response to a previously issued command to retrieve the
         * properties of a remote device, sent via
         * {@link DEVM#queryRemoteDeviceProperties queryRemoteDeviceProperties}
         * with {@code ForceUpdate} set to {@code TRUE}.
         *
         * @param success
         *            Whether the command was successful.
         * @param deviceProperties
         *            The properties of the remote device.
         */
        public void remoteDevicePropertiesStatusEvent(boolean success, RemoteDeviceProperties deviceProperties);

        /**
         * Invoked in response to a previously issued command to update the
         * internally maintained list of services offered by a remote device, as
         * requested via {@link DEVM#updateRemoteDeviceServices
         * updateRemoteDeviceServices}.
         *
         * @param remoteDevice
         *            The Bluetooth Address of the device.
         * @param success
         *            Whether the command was successful.
         */
        public void remoteDeviceServicesStatusEvent(BluetoothAddress remoteDevice, boolean success);

        /**
         * Invoked in response to a previously issued command to update the
         * internally maintained list of services offered by a remote low-energy device, as
         * requested via {@link DEVM#updateRemoteLowEnergyDeviceServices
         * updateRemoteLowEnergyDeviceServices}.
         *
         * @param remoteDevice
         *            The Bluetooth Address of the device.
         * @param success
         *            Whether the command was successful.
         */
        public void remoteLowEnergyDeviceServicesStatusEvent(BluetoothAddress remoteDevice, boolean success);

        /**
         * Invoked in response to a previously issued pairing request, as sent
         * via {@link DEVM#pairWithRemoteDevice pairWithRemoteDevice}.
         *
         * @param remoteDevice
         *            The Bluetooth address of the remote device.
         * @param success
         *            Whether the pairing process was successful.
         * @param authenticationStatus
         *            The Bluetooth authentication status: zero for success, or
         *            an HCI Error Code. (See the Bluetopia API for more
         *            information about HCI Error Codes.)
         */
        public void remoteDevicePairingStatusEvent(BluetoothAddress remoteDevice, boolean success, int authenticationStatus);

        /**
         * Invoked in response to a previously issued authentication request, as
         * sent via {@link DEVM#authenticateRemoteDevice
         * authenticateRemoteDevice}.
         *
         * @param remoteDevice
         *            The Bluetooth address of the remote device.
         * @param status
         *            Zero for success, or a negative return error code.
         */
        public void remoteDeviceAuthenticationStatusEvent(BluetoothAddress remoteDevice, int status);

        /**
         * Invoked in response to a previously issued encryption request, as
         * sent via {@link DEVM#encryptRemoteDevice
         * encryptRemoteDevice}.
         *
         * @param remoteDevice
         *            The Bluetooth address of the remote device.
         * @param status
         *            Zero for success, or a negative return error code.
         */
        public void remoteDeviceEncryptionStatusEvent(BluetoothAddress remoteDevice, int status);

        /**
         * Invoked in response to a previously issued connection request, as
         * sent via {@link DEVM#connectWithRemoteDevice
         * connectWithRemoteDevice}.
         *
         * @param remoteDevice
         *            The Bluetooth address of the remote device.
         * @param status
         *            Zero for success, or a negative return error code.
         */
        public void remoteDeviceConnectionStatusEvent(BluetoothAddress remoteDevice, int status);

        /**
         * Invoked in response to a previously issued request to begin a
         * low-energy device scan, as sent via
         * {@link DEVM#startLowEnergyDeviceScan startLowEnergyDeviceScan}.
         */
        public void deviceScanStartedEvent();

        /**
         * Invoked when an on-going low-energy device scan ends, either due to
         * timeout or in response to a request to stop the scan, as sent via
         * {@link DEVM#stopLowEnergyDeviceScan stopLowEnergyDeviceScan}.
         */
        public void deviceScanStoppedEvent();

        /**
         * Invoked when the resolvable address of a low-energy device has been
         * resolved.
         *
         * After this event occurs, the remote low-energy device will be
         * referenced by the new addreses.
         *
         * @param PriorResolvableDeviceAddress
         *            The original Bluetooth address by which the remote
         *            low-energy device was known.
         * @param CurrentResolvableDeviceAddress
         *            The resolved Bluetooth address by which the remote
         *            low-energy device is now known.
         */
        public void remoteLowEnergyDeviceAddressChangedEvent(BluetoothAddress PriorResolvableDeviceAddress, BluetoothAddress CurrentResolvableDeviceAddress);

        /**
         * Invoked in response to a previously issued request to begin
         * low-energy advertising, as sent via
         * {@link DEVM#startLowEnergyAdvertising}
         */
        public void deviceAdvertisingStarted();

        /**
         * Invoked when an on-going low-energy advertising ends, either due to
         * timeout or in response to a request to stop advertising, as sent via
         * {@link DEVM#stopLowEnergyDeviceScan stopLowEnergyAdvertising}.
         */
        public void deviceAdvertisingStopped();
    }

    /**
     * Receiver for authentication events from the Device Manager interface.
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
    public abstract interface AuthenticationCallback {

        /**
         * Invoked when a remote device has requested a PIN code from the local
         * device. Respond to this request with {@link DEVM#sendPinCode}.
         *
         * @param remoteDevice
         *            The Bluetooth address of the remote device.
         */
        public void pinCodeRequestEvent(BluetoothAddress remoteDevice);

        /**
         * Invoked when a remote device has requested a user confirmation from
         * the local device. Respond to this request with
         * {@link DEVM#sendUserConfirmation}.
         *
         * @param remoteDevice
         *            The Bluetooth address of the remote device.
         * @param passkey
         *            The passkey to be displayed to the user.
         */
        public void userConfirmationRequestEvent(BluetoothAddress remoteDevice, int passkey);

        /**
         * Invoked when a remote device has requested a passkey from the local
         * device. Respond to this request with {@link DEVM#sendPasskey}.
         *
         * @param remoteDevice
         *            The Bluetooth address of the remote device.
         */
        public void passkeyRequestEvent(BluetoothAddress remoteDevice);

        /**
         * Invoked when a remote device has sent a passkey to the local device.
         *
         * @param remoteDevice
         *            The Bluetooth address of the remote device.
         * @param passkey
         *            The passkey that has been sent.
         */
        public void passkeyIndicationEvent(BluetoothAddress remoteDevice, int passkey);

        /**
         * Invoked when a remote device has sent a key-press event to the local
         * device.
         *
         * @param remoteDevice
         *            The Bluetooth address of the remote device.
         * @param type
         *            The type of key-press that has occurred.
         */
        public void keypressIndicationEvent(BluetoothAddress remoteDevice, Keypress type);

        /**
         * Invoked when a remote device has requested out-of-band data from the
         * local device. Respond to this request with
         * {@link DEVM#sendOutOfBandData}.
         *
         * @param remoteDevice
         *            The Bluetooth address of the remote device.
         */
        public void outOfBandDataRequestEvent(BluetoothAddress remoteDevice);

        /**
         * Invoked when a remote device has requested the I/O capabilities of
         * the local device. Respond to this request with
         * {@link DEVM#sendIOCapabilities}.
         *
         * @param remoteDevice
         *            The Bluetooth address of the remote device.
         */
        public void ioCapabilitiesRequestEvent(BluetoothAddress remoteDevice);

        /**
         * Invoked when an authentication process is complete.
         *
         * @param remoteDevice
         *            The Bluetooth address of the remote device.
         * @param status
         *            Zero if authentication succeeded, or a negative return
         *            error code.
         */
        public void authenticationStatusEvent(BluetoothAddress remoteDevice, int status);

        /**
         * Invoked when a remote low-energy device has requested a user
         * confirmation from the local device. Respond to this request with
         * {@link DEVM#sendLowEnergyUserConfirmation}.
         *
         * @param remoteDevice
         *            The Bluetooth address of the remote device.
         * @param passkey
         *            The passkey to be displayed to the user.
         */
        public void lowEnergyUserConfirmationRequestEvent(BluetoothAddress remoteDevice, int passkey);

        /**
         * Invoked when a remote low-energy device has requested a passkey from
         * the local device. Respond to this request with
         * {@link DEVM#sendLowEnergyPasskey}.
         *
         * @param remoteDevice
         *            The Bluetooth address of the remote device.
         */
        public void lowEnergyPasskeyRequestEvent(BluetoothAddress remoteDevice);

        /**
         * Invoked when a remote low-energy device has sent a passkey to the
         * local device.
         *
         * @param remoteDevice
         *            The Bluetooth address of the remote device.
         * @param passkey
         *            The passkey that has been sent.
         */
        public void lowEnergyPasskeyIndicationEvent(BluetoothAddress remoteDevice, int passkey);

        /**
         * Invoked when a remote low-energy device has requested out-of-band
         * data from the local device. Respond to this request with
         * {@link DEVM#sendLowEnergyOutOfBandData}.
         *
         * @param remoteDevice
         *            The Bluetooth address of the remote device.
         */
        public void lowEnergyOutOfBandDataRequestEvent(BluetoothAddress remoteDevice);

        /**
         * Invoked when a remote low-energy device has requested the I/O
         * capabilities of the local device. Respond to this request with
         * {@link DEVM#sendLowEnergyIOCapabilities}.
         *
         * @param remoteDevice
         *            The Bluetooth address of the remote device.
         */
        public void lowEnergyIOCapabilitiesRequestEvent(BluetoothAddress remoteDevice);
    }

    /**
     * Positioning service class.
     */
    public static final int SERVICE_CLASS_POSITIONING                        = 0x00010000;
    /**
     * Networking service class.
     */
    public static final int SERVICE_CLASS_NETWORKING                         = 0x00020000;
    /**
     * Rendering service class.
     */
    public static final int SERVICE_CLASS_RENDERING                          = 0x00040000;
    /**
     * Capturing service class.
     */
    public static final int SERVICE_CLASS_CAPTURING                          = 0x00080000;
    /**
     * Object transfer service class.
     */
    public static final int SERVICE_CLASS_OBJECT_TRANSFER                    = 0x00100000;
    /**
     * Audio service class.
     */
    public static final int SERVICE_CLASS_AUDIO                              = 0x00200000;
    /**
     * Telephony service class.
     */
    public static final int SERVICE_CLASS_TELEPHONY                          = 0x00400000;
    /**
     * Information service class.
     */
    public static final int SERVICE_CLASS_INFORMATION                        = 0x00800000;

    /**
     * Miscellaneous major device class.
     */
    public static final int MAJOR_DEVICE_CLASS_MISCELLANEOUS                 = 0x00000000;
    /**
     * Computer major device class.
     */
    public static final int MAJOR_DEVICE_CLASS_COMPUTER                      = 0x00000100;
    /**
     * Phone major device class.
     */
    public static final int MAJOR_DEVICE_CLASS_PHONE                         = 0x00000200;
    /**
     * LAN major device class.
     */
    public static final int MAJOR_DEVICE_CLASS_LAN                           = 0x00000300;
    /**
     * Audio/video major device class.
     */
    public static final int MAJOR_DEVICE_CLASS_AUDIO_VIDEO                   = 0x00000400;
    /**
     * Peripheral major device class.
     */
    public static final int MAJOR_DEVICE_CLASS_PERIPHERAL                    = 0x00000500;
    /**
     * Imaging major device class.
     */
    public static final int MAJOR_DEVICE_CLASS_IMAGING                       = 0x00000600;
    /**
     * Wearable major device class.
     */
    public static final int MAJOR_DEVICE_CLASS_WEARABLE                      = 0x00000700;
    /**
     * Toy major device class.
     */
    public static final int MAJOR_DEVICE_CLASS_TOY                           = 0x00000800;
    /**
     * Health major device class.
     */
    public static final int MAJOR_DEVICE_CLASS_HEALTH                        = 0x00000900;
    /**
     * Uncategorized major device class.
     */
    public static final int MAJOR_DEVICE_CLASS_UNCATEGORIZED                 = 0x00001f00;

    /**
     * Uncategorized minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_UNCATEGORIZED                 = 0x00000000;

    /* Minor Device Classes - Computer */
    /**
     * Desktop workstation minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_DESKTOP_WORKSTATION           = 0x00000004;
    /**
     * Server minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_SERVER_CLASS_COMPUTER         = 0x00000008;
    /**
     * Laptop minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_LAPTOP                        = 0x0000000c;
    /**
     * Handheld PC / PDA minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_HANDHELD_PC_PDA               = 0x00000010;
    /**
     * Palm-sized PC / PDA minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_PALM_SIZED_PC_PDA             = 0x00000014;
    /**
     * Wearable computer minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_WEARABLE_COMPUTER             = 0x00000018;

    /* Minor Device Classes - Phone */
    /**
     * Cellular phone minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_CELLULAR                      = 0x00000004;
    /**
     * Cordless phone minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_CORDLESS                      = 0x00000008;
    /**
     * Smartphone minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_SMART_PHONE                   = 0x0000000c;
    /**
     * Wired modem voice gateway minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_WIRED_MODEM_VOICE_GATEWAY     = 0x00000010;
    /**
     * Common ISDN access minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_COMMON_ISDN_ACCESS            = 0x00000014;

    /* Minor Device Classes - LAN */
    /**
     * LAN / Network Access Point (fully available) minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_FULLY_AVAILABLE               = 0x00000000;
    /**
     * LAN / Network Access Point (1% - 17% utilized) minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_UTILIZED_PERCENT_1__TO_17     = 0x00000020;
    /**
     * LAN / Network Access Point (17% - 33% utilized) minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_UTILIZED_PERCENT_17_TO_33     = 0x00000040;
    /**
     * LAN / Network Access Point (33% - 50% utilized) minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_UTILIZED_PERCENT_33_TO_50     = 0x00000060;
    /**
     * LAN / Network Access Point (50% - 67% utilized) minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_UTILIZED_PERCENT_50_TO_67     = 0x00000080;
    /**
     * LAN / Network Access Point (67% - 83% utilized) minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_UTILIZED_PERCENT_67_TO_83     = 0x000000a0;
    /**
     * LAN / Network Access Point (83% - 99% utilized) minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_UTILIZED_PERCENT_83_TO_99     = 0x000000c0;
    /**
     * LAN / Network Access Point (no service available) minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_NO_SERVICE_AVAILABLE          = 0x000000e0;

    /* Minor Device Classes - Audio/Video */
    /**
     * Wearable headset minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_WEARABLE_HEADSET_DEVICE       = 0x00000004;
    /**
     * Hands-free minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_HANDS_FREE_DEVICE             = 0x00000008;
    /**
     * Microphone minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_MICROPHONE                    = 0x00000010;
    /**
     * Loudspeaker minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_LOUDSPEAKER                   = 0x00000014;
    /**
     * Headphones minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_HEADPHONES                    = 0x00000018;
    /**
     * Portable audio minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_PORTABLE_AUDIO                = 0x0000001c;
    /**
     * Car audio minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_CAR_AUDIO                     = 0x00000020;
    /**
     * Set top box minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_SET_TOP_BOX                   = 0x00000024;
    /**
     * High fidelity audio minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_HIFI_AUDIO_DEVICE             = 0x00000028;
    /**
     * VCR minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_VCR                           = 0x0000002c;
    /**
     * Video camera minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_VIDEO_CAMERA                  = 0x00000030;
    /**
     * Camcorder minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_CAMCORDER                     = 0x00000034;
    /**
     * Video monitor minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_VIDEO_MONITOR                 = 0x00000038;
    /**
     * Video display and loudspeaker minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_VIDEO_DISPLAY_AND_LOUDSPEAKER = 0x0000003c;
    /**
     * Video conferencing minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_VIDEO_CONFERENCING            = 0x00000040;
    /**
     * Gaming or toy minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_GAMING_OR_TOY                 = 0x00000048;

    /* Minor Device Classes - Peripheral */
    /**
     * Keyboard minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_KEYBOARD                      = 0x000000040;
    /**
     * Pointer minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_POINTER                       = 0x000000080;
    /**
     * Keyboard and pointer minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_KEYBOARD_AND_POINTER          = 0x0000000c0;

    /**
     * Joystick minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_JOYSTICK                      = 0x000000004;
    /**
     * Gamepad minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_GAMEPAD                       = 0x000000008;
    /**
     * Remote control minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_REMOTE_CONTROL                = 0x00000000c;
    /**
     * Sensing device minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_SENSING_DEVICE                = 0x000000010;
    /**
     * Digitizer tablet minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_DIGITIZER_TABLET              = 0x000000014;
    /**
     * Card reader minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_CARD_READER                   = 0x000000018;
    /**
     * Digital pen minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_DIGITAL_PEN                   = 0x00000001c;
    /**
     * Handheld scanner minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_HANDHELD_SCANNER              = 0x000000020;
    /**
     * Handheld gesture minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_HANDHELD_GESTURE_DEVICE       = 0x000000024;

    /* Minor Device Classes - Imaging */
    /**
     * Display minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_DISPLAY                       = 0x00000004;
    /**
     * Camera minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_CAMERA                        = 0x00000008;
    /**
     * Scanner minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_SCANNER                       = 0x00000010;
    /**
     * Printer minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_PRINTER                       = 0x00000020;

    /* Minor Device Classes - Wearable */
    /**
     * Wristwatch minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_WRIST_WATCH                   = 0x00000004;
    /**
     * Pager minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_PAGER                         = 0x00000008;
    /**
     * Jacket minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_JACKET                        = 0x0000000c;
    /**
     * Helmet minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_HELMET                        = 0x00000010;
    /**
     * Glasses minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_GLASSES                       = 0x00000014;

    /* Minor Device Classes - Toy */
    /**
     * Robot minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_ROBOT                         = 0x00000004;
    /**
     * Vehicle minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_VEHICLE                       = 0x00000008;
    /**
     * Doll or action figure minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_DOLL_OR_ACTION_FIGURE         = 0x0000000c;
    /**
     * Controller minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_CONTROLLER                    = 0x00000010;
    /**
     * Game minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_GAME                          = 0x00000014;

    /* Minor Device Classes - Health */
    /**
     * Blood pressure monitor minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_BLOOD_PRESSURE_MONITOR        = 0x00000004;
    /**
     * Thermometer minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_THERMOMETER                   = 0x00000008;
    /**
     * Weighing scale minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_WEIGHING_SCALE                = 0x0000000c;
    /**
     * Glucose meter minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_GLUCOSE_METER                 = 0x00000010;
    /**
     * Pulse oximeter minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_PULSE_OXIMETER                = 0x00000014;
    /**
     * Heart or pulse rate monitor minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_HEART_OR_PULSE_RATE_MONITOR   = 0x00000018;
    /**
     * Health data display minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_HEALTH_DATA_DISPLAY           = 0x0000001c;
    /**
     * Step counter minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_STEP_COUNTER                  = 0x00000020;
    /**
     * Body composition analyzer minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_BODY_COMPOSITION_ANALYZER     = 0x00000024;
    /**
     * Peak flow monitor minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_PEAK_FLOW_MONITOR             = 0x00000028;
    /**
     * Medication monitor minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_MEDICATION_MONITOR            = 0x0000002c;
    /**
     * Knee prosthesis minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_KNEE_PROSTHESIS               = 0x00000030;
    /**
     * Ankle prosthesis minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_ANKLE_PROSTHESIS              = 0x00000034;
    /**
     * Generic health manager minor device class.
     */
    public static final int MINOR_DEVICE_CLASS_GENERIC_HEALTH_MANAGER        = 0x00000038;

    private static final int ADDRESS_TYPE_PUBLIC                = 1;
    private static final int ADDRESS_TYPE_STATIC                = 2;
    private static final int ADDRESS_TYPE_PRIVATE_RESOLVABLE    = 3;
    private static final int ADDRESS_TYPE_PRIVATE_NONRESOLVABLE = 4;

    /**
     * The available types of Bluetooth Low Energy (BLE) Addresses.
     */
    public enum AddressType {
        /**
         * BLE Address which is permanently assigned to the device in accordance
         * with Section 9.2 of the <a
         * href="http://standards.ieee.org/getieee802/download/802-2001.pdf"
         * >IEEE 802-2001</a> standard.
         */
        PUBLIC,
        /**
         * BLE Address which is randomly generated and may only be (optionally)
         * changed when the device is power-cycled.
         */
        STATIC,
        /**
         * BLE Address which is randomly generated and used only on a single
         * connection but which may be used to identify a device when combined
         * with that device's Identity Resolving Key (IRK).
         */
        PRIVATE_RESOLVABLE,
        /**
         * BLE Address which is randomly generated and used only on a single
         * connection or for specific types of actions as prescribed by the Bluetooth 4.0
         * spec.
         */
        PRIVATE_NONRESOLVABLE
    }

    private static final int NATIVE_LOCAL_PROP_INDEX_CLASS_OF_DEVICE           = 0;
    private static final int NATIVE_LOCAL_PROP_INDEX_HCI_VERSION               = 1;
    private static final int NATIVE_LOCAL_PROP_INDEX_HCI_REVISION              = 2;
    private static final int NATIVE_LOCAL_PROP_INDEX_LMP_VERSION               = 3;
    private static final int NATIVE_LOCAL_PROP_INDEX_LMP_SUBVERSION            = 4;
    private static final int NATIVE_LOCAL_PROP_INDEX_DEVICE_MANUFACTURER       = 5;
    private static final int NATIVE_LOCAL_PROP_INDEX_LOCAL_DEVICE_FLAGS        = 6;
    private static final int NATIVE_LOCAL_PROP_INDEX_DISCOVERABLE_MODE         = 7;
    private static final int NATIVE_LOCAL_PROP_INDEX_DISCOVERABLE_MODE_TIMEOUT = 8;
    private static final int NATIVE_LOCAL_PROP_INDEX_CONNECTABLE_MODE          = 9;
    private static final int NATIVE_LOCAL_PROP_INDEX_CONNECTABLE_MODE_TIMEOUT  = 10;
    private static final int NATIVE_LOCAL_PROP_INDEX_PAIRABLE_MODE             = 11;
    private static final int NATIVE_LOCAL_PROP_INDEX_PAIRABLE_MODE_TIMEOUT     = 12;

    /**
     * Holds information pertaining to the current state of the local device.
     */
    public static final class LocalDeviceProperties {
        /**
         * The Bluetooth address of the device.
         */
        public BluetoothAddress          deviceAddress;
        /**
         * The class of the device.
         */
        public int                       classOfDevice;
        /**
         * The name of the device.
         */
        public String                    deviceName;
        /**
         * The HCI version.
         */
        public int                       hciVersion;
        /**
         * The HCI revision.
         */
        public int                       hciRevision;
        /**
         * The LMP version.
         */
        public int                       lmpVersion;
        /**
         * The LMP subversion.
         */
        public int                       lmpSubversion;
        /**
         * The device manufacturer.
         */
        public int                       deviceManufacturer;
        /**
         * The local device flags.
         */
        public EnumSet<LocalDeviceFlags> localDeviceFlags;
        /**
         * Whether the device is discoverable.
         */
        public boolean                   discoverableMode;
        /**
         * The discoverable mode timeout.
         */
        public int                       discoverableModeTimeout;
        /**
         * Whether the device is connectable.
         */
        public boolean                   connectableMode;
        /**
         * The connectable mode timeout.
         */
        public int                       connectableModeTimeout;
        /**
         * Whether the device is pairable.
         */
        public boolean                   pairableMode;
        /**
         * The pairable mode timeout.
         */
        public int                       pairableModeTimeout;
        /**
         * The general type of Bluetooth Low-Energy device.
         */
        public int                       deviceAppearance;
        /**
         * The Bluetooth address by which this low-energy device is know.
         */
        public BluetoothAddress          lowEnergyAddress;
        /**
         * The type of Bluetooth Low-Energy address stored in
         * {@code lowEnergyAddress}.
         */
        public AddressType               lowEnergyAddressType;
        /**
         * The timeout of the low-energy device scan currently in progress.
         */
        public int                       scanTimeout;
        /**
         * The timeout for the currently-active Bluetooth Low-Energy advertising
         * mode.
         */
        public int                       advertisingTimeout;

    }

    /* The following bit definitions are used with the LocalDeviceFlags  */
    /* member of the DEVM_Local_Device_Properties_t structure to denote  */
    /* various State information.                                        */
    private static final int LOCAL_DEVICE_FLAGS_DEVICE_DISCOVERY_IN_PROGRESS = 0x00000001;
    private static final int LOCAL_DEVICE_FLAGS_LE_SCANNING_IN_PROGRESS      = 0x00010000;
    private static final int LOCAL_DEVICE_FLAGS_LE_ADVERTISING_IN_PROGRESS   = 0x00020000;
    private static final int LOCAL_DEVICE_FLAGS_LE_ROLE_IS_CURRENTLY_SLAVE   = 0x40000000;
    private static final int LOCAL_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY   = 0x80000000;
    private static final int LOCAL_DEVICE_FLAGS_DEVICE_SUPPORTS_ANT_PLUS     = 0x01000000;

    /**
     * Used to denote miscellaneous state information regarding the local
     * device.
     */
    public enum LocalDeviceFlags {
        /**
         * Denotes that a device discovery is in progress.
         */
        DEVICE_DISCOVERY_IN_PROGRESS,
        /**
         * Denotes that a low-energy device scan is in progress.
         */
        LE_SCANNING_IN_PROGRESS,
        /**
         * Denotes that the local low-energy device is currently advertising
         * itself.
         */
        LE_ADVERTISING_IN_PROGRESS,
        /**
         * Denotes that the local low-energy device is currently in the slave
         * role.
         */
        LE_ROLE_IS_CURRENTLY_SLAVE,
        /**
         * Denotes that the local device supports the Bluetooth Low-Energy
         * protocol.
         */
        DEVICE_SUPPORTS_LOW_ENERGY,
        /**
         * Denotes that the local device supports the ANT Plus
         * protocol.
         */
        DEVICE_SUPPORTS_ANT_PLUS,
    }

    private static final int LOCAL_PROPERTY_FIELD_CLASS_OF_DEVICE   = 0x00000001;
    private static final int LOCAL_PROPERTY_FIELD_DEVICE_NAME       = 0x00000002;
    private static final int LOCAL_PROPERTY_FIELD_DISCOVERABLE_MODE = 0x00000004;
    private static final int LOCAL_PROPERTY_FIELD_CONNECTABLE_MODE  = 0x00000008;
    private static final int LOCAL_PROPERTY_FIELD_PAIRABLE_MODE     = 0x00000010;
    private static final int LOCAL_PROPERTY_FIELD_DEVICE_FLAGS      = 0x00000020;
    private static final int LOCAL_PROPERTY_FIELD_DEVICE_APPEARANCE = 0x00000040;

    /**
     * The different local device property fields. Used by the
     * {@link EventCallback#localDevicePropertiesChangedEvent Local Device
     * Properties Changed} event to indicate which fields of the
     * {@link LocalDeviceProperties} structure were changed.
     */
    public enum LocalPropertyField {
        /**
         * The classOfDevice field.
         */
        CLASS_OF_DEVICE,
        /**
         * The deviceName field.
         */
        DEVICE_NAME,
        /**
         * The discoverableMode field.
         */
        DISCOVERABLE_MODE,
        /**
         * The connectableMode field.
         */
        CONNECTABLE_MODE,
        /**
         * The pairableMode field.
         */
        PAIRABLE_MODE,
        /**
         * The localDeviceFlags field.
         */
        DEVICE_FLAGS,
        /**
         * The deviceAppearance field.
         */
        DEVICE_APPEARANCE,
    }

    /**
     * Holds details of the Device ID Information of the Local Device.
     */
    public static final class DeviceIDInformation {
        /**
         * The VendorID of the local device.
         */
        public int     VendorID;
        /**
         * The VendorID of the local device.
         */
        public int     ProductID;
        /**
         * The VendorID of the local device.
         */
        public int     DeviceVersion;
        /**
         * {@code false} if the {@code VendorID} was assigned by the Bluetooth
         * SIG and published in the Assigned Numbers document, or {@code true}
         * if it was allocated by the USB Implementer’s Forum.
         */
        public boolean USBVendorID;
    }

    private static final int NATIVE_REMOTE_PROP_INDEX_CLASS_OF_DEVICE           = 0;
    private static final int NATIVE_REMOTE_PROP_INDEX_DEVICE_FLAGS              = 1;
    private static final int NATIVE_REMOTE_PROP_INDEX_RSSI                      = 2;
    private static final int NATIVE_REMOTE_PROP_INDEX_TRANSMIT_POWER            = 3;
    private static final int NATIVE_REMOTE_PROP_INDEX_SNIFF_INTERVAL            = 4;
    private static final int NATIVE_REMOTE_PROP_INDEX_APPLICATION_INFO          = 5;
    private static final int NATIVE_REMOTE_PROP_INDEX_LOW_ENERGY_ADDRESS_TYPE   = 6;
    private static final int NATIVE_REMOTE_PROP_INDEX_LOW_ENERGY_RSSI           = 7;
    private static final int NATIVE_REMOTE_PROP_INDEX_LOW_ENERGY_TRANSMIT_POWER = 8;
    private static final int NATIVE_REMOTE_PROP_INDEX_DEVICE_APPEARANCE         = 9;

    private static final int NATIVE_REMOTE_STRING_INDEX_NAME           = 0;
    private static final int NATIVE_REMOTE_STRING_INDEX_FRIENDLY_NAME  = 1;

    /**
     * Holds information pertaining to the state of a specific remote device.
     */
    public static final class RemoteDeviceProperties {
        /**
         * The Bluetooth address of the remote device.
         */
        public BluetoothAddress            deviceAddress;
        /**
         * The class of device reported by the remote device.
         */
        public int                         classOfDevice;
        /**
         * The name reported by the remote device.
         */
        public String                      deviceName;
        /**
         * The set of flags indicating the current state of the remote device.
         */
        public EnumSet<RemoteDeviceFlags>  remoteDeviceFlags;
        /**
         * The Received Signal Strength Indication, in dBm, as determined during device
         * discovery if the local device supports this feature.
         */
        public int                         rssi;
        /**
         * The current transmit power, in dBm, used when transmitting to this
         * remote device. Only valid when {@link #remoteDeviceFlags} contains
         * {@link RemoteDeviceFlags#CURRENTLY_CONNECTED}.
         */
        public int                         transmitPower;
        /**
         * The interval between communications when the remote device is placed
         * in sniff mode. Only valid when {@link #remoteDeviceFlags} contains
         * {@link RemoteDeviceFlags#LINK_CURRENTLY_SNIFF_MODE}.
         */
        public int                         sniffInterval;
        /**
         * Additional settings for this device which are configurable by the
         * client application.
         */
        public RemoteDeviceApplicationData applicationData;
        /**
         * The type of Bluetooth Low-Energy address stored in
         * {@code deviceAddress}. Only valid if this is a low-energy device.
         */
        public AddressType                 lowEnergyAddressType;
        /**
         * The Received Signal Strength Indication, in dBm, of the communication
         * path to the remote low-energy device, if the local device supports
         * this feature.
         */
        public int                         lowEnergyRSSI;
        /**
         * The current transmit power, in dBm, used when transmitting to this
         * remote low-energy device. Only valid when {@link #remoteDeviceFlags}
         * contains {@link RemoteDeviceFlags#CURRENTLY_CONNECTED_OVER_LE}.
         */
        public int                         lowEnergyTransmitPower;
        /**
         * The declared Appearance of this low-energy device.
         */
        public int                         deviceAppearance;
        /**
         * If the Bluetooth Low-Energy address of this device was resolved, this
         * is the original address known before the resolution completed.
         */
        public BluetoothAddress            priorResolvableAddress;
    }

    /**
     * Holds information manipulated by the client application framework. (This
     * data is stored alongside any other remote device information.) Note that
     * this storage space is shared between all Platform Manager clients.
     */
    public static final class RemoteDeviceApplicationData {
        /**
         * A user-customizable name for a remote device which a user-interface
         * can display in place of the name reported by the device (presented in
         * the {@link RemoteDeviceProperties#deviceName} field). Typically, this
         * is used by an application to allow a user to specify an alias for a
         * paired device.
         */
        public String friendlyName;
        /**
         * Storage space for arbitrary data. Recommended usage is as a bit field
         * for an application's per-device settings or as an index into an
         * application-controlled configuration database.
         */
        public int    applicationInfo;
    }

    /* The following bit definitions are used with the RemoteDeviceFlags */
    /* member of the DEVM_Remote_Device_Properties_t structure to denote */
    /* various State information about the Remote Device.                */
    private static final int REMOTE_DEVICE_FLAGS_NAME_KNOWN                  = 0x00000001;
    private static final int REMOTE_DEVICE_FLAGS_APPLICATION_DATA_VALID      = 0x00000002;
    private static final int REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED            = 0x00000004;
    private static final int REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED         = 0x00000008;
    private static final int REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_ENCRYPTED    = 0x00000010;
    private static final int REMOTE_DEVICE_FLAGS_LINK_CURRENTLY_SNIFF_MODE   = 0x00000020;
    private static final int REMOTE_DEVICE_FLAGS_LINK_INITIATED_LOCALLY      = 0x00000040;
    private static final int REMOTE_DEVICE_FLAGS_AUTHENTICATED_KEY           = 0x00000080;
    private static final int REMOTE_DEVICE_FLAGS_SERVICES_KNOWN              = 0x00000100;
    private static final int REMOTE_DEVICE_FLAGS_TX_POWER_KNOWN              = 0x00000200;
    private static final int REMOTE_DEVICE_FLAGS_EIR_DATA_KNOWN              = 0x00000400;
    private static final int REMOTE_DEVICE_FLAGS_LE_SERVICES_KNOWN           = 0x00001000;
    private static final int REMOTE_DEVICE_FLAGS_LE_APPEARANCE_KNOWN         = 0x00002000;
    private static final int REMOTE_DEVICE_FLAGS_CURRENTLY_PAIRED_OVER_LE    = 0x00004000;
    private static final int REMOTE_DEVICE_FLAGS_CURRENTLY_CONNECTED_OVER_LE = 0x00008000;
    private static final int REMOTE_DEVICE_FLAGS_LE_LINK_CURRENTLY_ENCRYPTED = 0x00010000;
    private static final int REMOTE_DEVICE_FLAGS_LE_AUTHENTICATED_KEY        = 0x00020000;
    private static final int REMOTE_DEVICE_FLAGS_LE_LINK_INITIATED_LOCALLY   = 0x00040000;
    private static final int REMOTE_DEVICE_FLAGS_LE_TX_POWER_KNOWN           = 0x00080000;
    private static final int REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY  = 0x40000000;
    private static final int REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_BR_EDR      = 0x80000000;

    /**
     * Holds various state information about a remote device.
     */
    public enum RemoteDeviceFlags {
        /**
         * The name of the remote device is known.
         */
        NAME_KNOWN,
        /**
         * The stored application data is valid.
         */
        APPLICATION_DATA_VALID,
        /**
         * The device is currently paired.
         */
        CURRENTLY_PAIRED,
        /**
         * The device is currently connected.
         */
        CURRENTLY_CONNECTED,
        /**
         * The link is currently encrypted.
         */
        LINK_CURRENTLY_ENCRYPTED,
        /**
         * The link is currently in sniff mode.
         */
        LINK_CURRENTLY_SNIFF_MODE,
        /**
         * The link was initiated locally.
         */
        LINK_INITIATED_LOCALLY,
        /**
         * The link key was created through user interaction.
         */
        AUTHENTICATED_KEY,
        /**
         * The services offered by the device are known.
         */
        SERVICES_KNOWN,
        /**
         * The transmit power of the connection to this device is known.
         */
        TX_POWER_KNOWN,
        /**
         * The Extended Inquiry Response Data for this device is known.
         */
        EIR_DATA_KNOWN,
        /**
         * The low-energy services offered by this device are known.
         */
        LE_SERVICES_KNOWN,
        /**
         * The low-energy appearance type of this device is known.
         */
        LE_APPEARANCE_KNOWN,
        /**
         * This low-energy device is currently paired.
         */
        CURRENTLY_PAIRED_OVER_LE,
        /**
         * This low-energy device is currently connected.
         */
        CURRENTLY_CONNECTED_OVER_LE,
        /**
         * The low-energy link to this device is currently encrypted.
         */
        LE_LINK_CURRENTLY_ENCRYPTED,
        /**
         * The low-energy link key was created through user-interaction.
         */
        LE_AUTHENTICATED_KEY,
        /**
         * The low-energy link was initiated locally.
         */
        LE_LINK_INITIATED_LOCALLY,
        /**
         * The transmit power of the low-energy connection to this device is
         * known.
         */
        LE_TX_POWER_KNOWN,
        /**
         * This device supports the Bluetooth Low Energy protocol.
         */
        SUPPORTS_LOW_ENERGY,
        /**
         * This device supports the Bluetooth Classic (BR/EDR) protocol.
         */
        SUPPORTS_BR_EDR,
    }

    private static final int REMOTE_PROPERTY_FIELD_CLASS_OF_DEVICE          = 0x00000001;
    private static final int REMOTE_PROPERTY_FIELD_DEVICE_NAME              = 0x00000002;
    private static final int REMOTE_PROPERTY_FIELD_APPLICATION_DATA         = 0x00000004;
    private static final int REMOTE_PROPERTY_FIELD_DEVICE_FLAGS             = 0x00000008;
    private static final int REMOTE_PROPERTY_FIELD_RSSI                     = 0x00000010;
    private static final int REMOTE_PROPERTY_FIELD_PAIRING_STATE            = 0x00000020;
    private static final int REMOTE_PROPERTY_FIELD_CONNECTION_STATE         = 0x00000040;
    private static final int REMOTE_PROPERTY_FIELD_ENCRYPTION_STATE         = 0x00000080;
    private static final int REMOTE_PROPERTY_FIELD_SNIFF_STATE              = 0x00000100;
    private static final int REMOTE_PROPERTY_FIELD_SERVICES_STATE           = 0x00000200;
    private static final int REMOTE_PROPERTY_FIELD_LE_RSSI                  = 0x00000400;
    private static final int REMOTE_PROPERTY_FIELD_LE_PAIRING_STATE         = 0x00000800;
    private static final int REMOTE_PROPERTY_FIELD_LE_CONNECTION_STATE      = 0x00001000;
    private static final int REMOTE_PROPERTY_FIELD_LE_ENCRYPTION_STATE      = 0x00002000;
    private static final int REMOTE_PROPERTY_FIELD_PRIOR_RESOLVABLE_ADDRESS = 0x00004000;
    private static final int REMOTE_PROPERTY_FIELD_DEVICE_APPEARANCE        = 0x00008000;
    private static final int REMOTE_PROPERTY_FIELD_LE_SERVICES_STATE        = 0x00010000;

    /**
     * The different remote device property fields.
     */
    public enum RemotePropertyField {
        /**
         * The classOfDevice field.
         */
        CLASS_OF_DEVICE,
        /**
         * The deviceName field.
         */
        DEVICE_NAME,
        /**
         * The applicationData field.
         */
        APPLICATION_DATA,
        /**
         * The remoteDeviceFlags field.
         */
        DEVICE_FLAGS,
        /**
         * the rssi field.
         */
        RSSI,
        /**
         * The CURRENTLY_PAIRED value of the remoteDeviceFlags field.
         */
        PAIRING_STATE,
        /**
         * The CURRENTLY_CONNECTED value of the remoteDeviceFlags field.
         */
        CONNECTION_STATE,
        /**
         * The LINK_CURRENTLY_ENCRYPTED value of the remoteDeviceFlags field.
         */
        ENCRYPTION_STATE,
        /**
         * The LINK_CURRENTLY_SNIFF_MODE value of the remoteDeviceFlags field.
         */
        SNIFF_STATE,
        /**
         * The SERVICES_KNOWN value of the remoteDeviceFlags field.
         */
        SERVICES_STATE,
        /**
         * The lowEnergyRSSI field.
         */
        LE_RSSI,
        /**
         * The CURRENTLY_PAIRED_OVER_LE value of the remoteDeviceFlags field.
         */
        LE_PAIRING_STATE,
        /**
         * The CURRENTLY_CONNECTED_OVER_LE value of the remoteDeviceFlags field.
         */
        LE_CONNECTION_STATE,
        /**
         * The LE_LINK_CURRENTLY_ENCRYPTED value of the remoteDeviceFlags field.
         */
        LE_ENCRYPTION_STATE,
        /**
         * The priorResolvableAddress field.
         */
        PRIOR_RESOLVABLE_ADDRESS,
        /**
         * The deviceAppearance field.
         */
        DEVICE_APPEARANCE,
        /**
         * The LE_SERVICES_KNOWN value of the remoteDeviceFlags field.
         */
        LE_SERVICES_STATE,
    }

    /* The following constants are used with the RemoteDeviceFilter      */
    /* member of the DEVM_Query_Remote_Device_List_Request_t message to  */
    /* specify the type of Devices to return (e.g. a filter to apply).   */
    private static final int REMOTE_DEVICE_FILTER_ALL_DEVICES         = 0;
    private static final int REMOTE_DEVICE_FILTER_CURRENTLY_CONNECTED = 1;
    private static final int REMOTE_DEVICE_FILTER_CURRENTLY_PAIRED    = 2;
    private static final int REMOTE_DEVICE_FILTER_CURRENTLY_UNPAIRED  = 3;

    /**
     * The possible remote device filters.
     */
    public enum RemoteDeviceFilter {
        /**
         * All devices.
         */
        ALL_DEVICES,
        /**
         * Currently connected devices.
         */
        CURRENTLY_CONNECTED,
        /**
         * Currently paired devices.
         */
        CURRENTLY_PAIRED,
        /**
         * Currently unpaired devices.
         */
        CURRENTLY_UNPAIRED,
    }

    /* The following constants represent the defined Bluetooth Values    */
    /* that specify the specific Bluetooth HCI Extended Inquiry Response */
    /* Data values used with the Extended Inquiry Response (Version 2.1 +*/
    /* EDR).                                                             */
    private static final int EIR_DATA_TYPE_FLAGS                                      = 0x01;
    private static final int EIR_DATA_TYPE_16_BIT_SERVICE_CLASS_UUID_PARTIAL          = 0x02;
    private static final int EIR_DATA_TYPE_16_BIT_SERVICE_CLASS_UUID_COMPLETE         = 0x03;
    private static final int EIR_DATA_TYPE_32_BIT_SERVICE_CLASS_UUID_PARTIAL          = 0x04;
    private static final int EIR_DATA_TYPE_32_BIT_SERVICE_CLASS_UUID_COMPLETE         = 0x05;
    private static final int EIR_DATA_TYPE_128_BIT_SERVICE_CLASS_UUID_PARTIAL         = 0x06;
    private static final int EIR_DATA_TYPE_128_BIT_SERVICE_CLASS_UUID_COMPLETE        = 0x07;
    private static final int EIR_DATA_TYPE_LOCAL_NAME_SHORTENED                       = 0x08;
    private static final int EIR_DATA_TYPE_LOCAL_NAME_COMPLETE                        = 0x09;
    private static final int EIR_DATA_TYPE_TX_POWER_LEVEL                             = 0x0A;
    private static final int EIR_DATA_TYPE_CLASS_OF_DEVICE                            = 0x0D;
    private static final int EIR_DATA_TYPE_SIMPLE_PAIRING_HASH_C                      = 0x0E;
    private static final int EIR_DATA_TYPE_SIMPLE_PAIRING_RANDOMIZER_R                = 0x0F;
    private static final int EIR_DATA_TYPE_DEVICE_ID                                  = 0x10;
    private static final int EIR_DATA_TYPE_MANUFACTURER_SPECIFIC                      = 0xFF;

    public enum EIRDataType {
        FLAGS,
        SERVICE_CLASS_UUID_16_BIT_PARTIAL,
        SERVICE_CLASS_UUID_16_BIT_COMPLETE,
        SERVICE_CLASS_UUID_32_BIT_PARTIAL,
        SERVICE_CLASS_UUID_32_BIT_COMPLETE,
        SERVICE_CLASS_UUID_128_BIT_PARTIAL,
        SERVICE_CLASS_UUID_128_BIT_COMPLETE,
        LOCAL_NAME_SHORTENED,
        LOCAL_NAME_COMPLETE,
        TX_POWER_LEVEL,
        CLASS_OF_DEVICE,
        SIMPLE_PAIRING_HASH_C,
        SIMPLE_PAIRING_RANDOMIZER_R,
        DEVICE_ID,
        MANUFACTURER_SPECIFIC,
    }

    private static final int ADVERTISING_DATA_TYPE_FLAGS                                      = 0x01;
    private static final int ADVERTISING_DATA_TYPE_16_BIT_SERVICE_UUID_PARTIAL                = 0x02;
    private static final int ADVERTISING_DATA_TYPE_16_BIT_SERVICE_UUID_COMPLETE               = 0x03;
    private static final int ADVERTISING_DATA_TYPE_32_BIT_SERVICE_UUID_PARTIAL                = 0x04;
    private static final int ADVERTISING_DATA_TYPE_32_BIT_SERVICE_UUID_COMPLETE               = 0x05;
    private static final int ADVERTISING_DATA_TYPE_128_BIT_SERVICE_UUID_PARTIAL               = 0x06;
    private static final int ADVERTISING_DATA_TYPE_128_BIT_SERVICE_UUID_COMPLETE              = 0x07;
    private static final int ADVERTISING_DATA_TYPE_LOCAL_NAME_SHORTENED                       = 0x08;
    private static final int ADVERTISING_DATA_TYPE_LOCAL_NAME_COMPLETE                        = 0x09;
    private static final int ADVERTISING_DATA_TYPE_TX_POWER_LEVEL                             = 0x0A;
    private static final int ADVERTISING_DATA_TYPE_CLASS_OF_DEVICE                            = 0x0D;
    private static final int ADVERTISING_DATA_TYPE_SIMPLE_PAIRING_HASH_C                      = 0x0E;
    private static final int ADVERTISING_DATA_TYPE_SIMPLE_PAIRING_RANDOMIZER_R                = 0x0F;
    private static final int ADVERTISING_DATA_TYPE_SECURITY_MANAGER_TK                        = 0x10;
    private static final int ADVERTISING_DATA_TYPE_SECURITY_MANAGER_OUT_OF_BAND_FLAGS         = 0x11;
    private static final int ADVERTISING_DATA_TYPE_SLAVE_CONNECTION_INTERVAL_RANGE            = 0x12;
    private static final int ADVERTISING_DATA_TYPE_LIST_OF_16_BIT_SERVICE_SOLICITATION_UUIDS  = 0x14;
    private static final int ADVERTISING_DATA_TYPE_LIST_OF_128_BIT_SERVICE_SOLICITATION_UUIDS = 0x15;
    private static final int ADVERTISING_DATA_TYPE_SERVICE_DATA                               = 0x16;
    private static final int ADVERTISING_DATA_TYPE_PUBLIC_TARGET_ADDRESS                      = 0x17;
    private static final int ADVERTISING_DATA_TYPE_RANDOM_TARGET_ADDRESS                      = 0x18;
    private static final int ADVERTISING_DATA_TYPE_APPEARANCE                                 = 0x19;
    private static final int ADVERTISING_DATA_TYPE_MANUFACTURER_SPECIFIC                      = 0xFF;

    public enum AdvertisingDataType {
        FLAGS,
        SERVICE_CLASS_UUID_16_BIT_PARTIAL,
        SERVICE_CLASS_UUID_16_BIT_COMPLETE,
        SERVICE_CLASS_UUID_32_BIT_PARTIAL,
        SERVICE_CLASS_UUID_32_BIT_COMPLETE,
        SERVICE_CLASS_UUID_128_BIT_PARTIAL,
        SERVICE_CLASS_UUID_128_BIT_COMPLETE,
        LOCAL_NAME_SHORTENED,
        LOCAL_NAME_COMPLETE,
        TX_POWER_LEVEL,
        CLASS_OF_DEVICE,
        SIMPLE_PAIRING_HASH_C,
        SIMPLE_PAIRING_RANDOMIZER_R,
        SECURITY_MANAGER_TK,
        SECURITY_MANAGER_OUT_OF_BAND_FLAGS,
        SLAVE_CONNECTION_INTERVAL_RANGE,
        LIST_OF_16_BIT_SERVICE_SOLICITATION_UUIDS,
        LIST_OF_128_BIT_SERVICE_SOLICITATION_UUIDS,
        SERVICE_DATA,
        PUBLIC_TARGET_ADDRESS,
        RANDOM_TARGET_ADDRESS,
        APPEARANCE,
        MANUFACTURER_SPECIFIC,
    }

    /* The following constants are used with the ConnectFlags member of  */
    /* the DEVM_Connect_With_Remote_Device_Request_t message to specify  */
    /* various flags to apply to the connection process.                 */
    private static final int CONNECT_FLAGS_AUTHENTICATE = 0x00000001;
    private static final int CONNECT_FLAGS_ENCRYPT      = 0x00000002;

    /**
     * The possible flags to apply to the connection process.
     */
    public enum ConnectFlags {
        /**
         * Authenticate the connection.
         */
        AUTHENTICATE,
        /**
         * Encrypt the connection.
         */
        ENCRYPT,
    }

    private static final int KEYPRESS_ENTRY_STARTED   = 1;
    private static final int KEYPRESS_DIGIT_ENTERED   = 2;
    private static final int KEYPRESS_DIGIT_ERASED    = 3;
    private static final int KEYPRESS_CLEARED         = 4;
    private static final int KEYPRESS_ENTRY_COMPLETED = 5;

    /**
     * The possible Keypress actions that can be specified within an
     * authentication event.
     */
    public enum Keypress {
        /**
         * The user has started entering digits.
         */
        ENTRY_STARTED,
        /**
         * The user entered a digit.
         */
        DIGIT_ENTERED,
        /**
         * The user erased a digit.
         */
        DIGIT_ERASED,
        /**
         * The user cleared the entry.
         */
        CLEARED,
        /**
         * The user completed the entry.
         */
        ENTRY_COMPLETED,
    }

    private static final int IO_CAPABILITY_DISPLAY_ONLY       = 1;
    private static final int IO_CAPABILITY_DISPLAY_YES_NO     = 2;
    private static final int IO_CAPABILITY_KEYBOARD_ONLY      = 3;
    private static final int IO_CAPABILITY_NO_INPUT_NO_OUTPUT = 4;

    /**
     * The possible I/O capabilities of a device.
     */
    public enum IOCapability {
        /**
         * The device only has a display (with no input capability.)
         */
        DISPLAY_ONLY,
        /**
         * The device has a display and the ability for a user to enter a
         * (yes/no) response.
         */
        DISPLAY_YES_NO,
        /**
         * The device has a key or keypad (but does not have a display.)
         */
        KEYBOARD_ONLY,
        /**
         * The device has no input or output capabilities.  (Such a device
         * may use out-of-band or JustWorks associations.)
         */
        NO_INPUT_NO_OUTPUT
    }

    private static final int LE_IO_CAPABILITY_DISPLAY_ONLY       = 1;
    private static final int LE_IO_CAPABILITY_DISPLAY_YES_NO     = 2;
    private static final int LE_IO_CAPABILITY_KEYBOARD_ONLY      = 3;
    private static final int LE_IO_CAPABILITY_NO_INPUT_NO_OUTPUT = 4;
    private static final int LE_IO_CAPABILITY_KEYBOARD_DISPLAY   = 5;

    /**
     * The possible I/O capabilities of a low-energy device.
     */
    public enum LowEnergyIOCapability {
        /**
         * The device only has a display (with no input capability.)
         */
       DISPLAY_ONLY,
       /**
        * The device has a display and the ability for a user to enter a
        * (yes/no) response.
        */
       DISPLAY_YES_NO,
       /**
        * The device has a key or keypad (but does not have a display.)
        */
       KEYBOARD_ONLY,
       /**
        * The device has no input or output capabilities.  (Such a device
        * may use out-of-band or JustWorks associations.)
        */
       NO_INPUT_NO_OUTPUT,
       /**
        * The device has a display and a keypad.
        */
       KEYBOARD_DISPLAY
    }

    private static final int BONDING_TYPE_NO_BONDING        = 1;
    private static final int BONDING_TYPE_DEDICATED_BONDING = 2;
    private static final int BONDING_TYPE_GENERAL_BONDING   = 3;

    /**
     * The possible authentication requirements of a device.
     */
    public enum BondingType {
        /**
         * Specifies no bonding.
         */
        NO_BONDING,
        /**
         * Specifies dedicated bonding.
         */
        DEDICATED_BONDING,
        /**
         * Specifies general bonding.
         */
        GENERAL_BONDING,
    }

    private static final int LE_BONDING_TYPE_NO_BONDING = 1;
    private static final int LE_BONDING_TYPE_BONDING    = 2;

    /**
     * The possible authentication requirements of a device.
     */
    public enum LowEnergyBondingType {
        /**
         * Specifies that bonding is not desired for the connection.
         */
        NO_BONDING,
        /**
         * Specifies that bonding is required for the connection.
         */
        BONDING
    }

    private static final int LOCAL_DEVICE_FEATURE_BLUETOOTH_LOW_ENERGY = 1;
    private static final int LOCAL_DEVICE_FEATURE_ANT_PLUS             = 2;

    /**
     * The Local Device Feature which can be enabled and disabled
     * (if the hardware supports them).
     *
     */
    public enum LocalDeviceFeature {
        /**
         * The Bluetooth Low Energy feature.
         */
        BLUETOOTH_LOW_ENERGY,
        /**
         * The ANT Plus feature.
         */
        ANT_PLUS
    }

    private static final int ADVERTISING_FLAGS_USE_PUBLIC_ADDRESS    = 0x00000001;
    private static final int ADVERTISING_FLAGS_DISCOVERABLE          = 0x00000002;
    private static final int ADVERTISING_FLAGS_CONNECTABLE           = 0x00000004;
    private static final int ADVERTISING_FLAGS_ADVERTISE_DEVICE_NAME = 0x00000008;
    private static final int ADVERTISING_FLAGS_ADVERTISE_TX_POWER    = 0x00000010;
    private static final int ADVERTISING_FLAGS_ADVERTISE_APPEARANCE  = 0x00000020;

    /**
     * Flags that can be set when advertising low energy.
     *
     */
    public enum AdvertisingFlags {
        /**
         * Use the device's public address (rather than a random resolvable).
         */
        USE_PUBLIC_ADDRESS,

        /**
         * Advertise as discoverable.
         */
        DISCOVERABLE,

        /**
         * Advertise as connectable.
         */
        CONNECTABLE,

        /**
         * Advertise device name.
         */
        ADVERTISE_DEVICE_NAME,

        /**
         * Advertise Tx Power.
         */
        ADVERTISE_TX_POWER,

        /**
         * Advertise appearance.
         */
        ADVERTISE_APPEARANCE
    }

    /* Native method declarations */

    private native static void   initClassNative();

    private native static String convertManufacturerNameToStringNative(int deviceManufacturer);

    //FIXME
    //    private native static int    convertRawSDPStreamToParsedSDPDataNative(int rawSDPDataLength, byte[] rawSDPData, Parsed_SDP_Data_t parsedSDPData);

    //FIXME
    //    private native static void   freeParsedSDPDataNative(Parsed_SDP_Data_t parsedSDPData);

    //FIXME
    //    private native static int    convertParsedSDPDataToRawSDPStreamNative(Parsed_SDP_Data_t parsedSDPData, int rawSDPDataLength, byte[] rawSDPData);

    private native void          initObjectNative(boolean authentication) throws ServerNotReachableException;

    private native void          cleanupObjectNative();

    private native boolean       acquireLockNative();

    private native void          releaseLockNative();

    private native int           powerOnDeviceNative();

    private native int           powerOffDeviceNative();

    private native int           queryDevicePowerStateNative();

    private native void          acknowledgeDevicePoweringDownNative();

    private native int           queryLocalDevicePropertiesNative(byte[] address, String[] name, int[] properties);

    private native int           updateClassOfDeviceNative(int classOfDevice);

    private native int           updateDeviceNameNative(String deviceName);

    private native int           updateDiscoverableModeNative(boolean discoverable, int timeout);

    private native int           updateConnectableModeNative(boolean connectable, int timeout);

    private native int           updatePairableModeNative(boolean pairable, int timeout);

    private native int           queryLocalDeviceIDInformationNative(int[] values);

    private native int           enableLocalDeviceFeatureNative(int feature);

    private native int           disableLocalDeviceFeatureNative(int feature);

    private native int           queryActiveLocalDeviceFeatureNative(int[] features);

    private native int           startDeviceDiscoveryNative(int discoveryDuration);

    private native int           stopDeviceDiscoveryNative();

    private native int           startLowEnergyDeviceScanNative(int scanDuration);

    private native int           stopLowEnergyDeviceScanNative();

    private native int           startLowEnergyAdvertisingNative(int flags, int duration, int[] dataTypes, byte[][] dataList);

    private native int           stopLowEnergyAdvertisingNative();

    private native int           queryRemoteDeviceListNative(int remoteDeviceFilter, int classOfDeviceMask, BluetoothAddress[][] remoteDeviceList);

    private native int           queryRemoteDevicePropertiesNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean forceUpdate, String[] names, int[] properties, byte[] priorAddress);

    private native int           queryRemoteLowEnergyDevicePropertiesNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean forceUpdate, String[] names, int[] properties, byte[] priorAddress);

    private native int           queryRemoteDeviceEIRDataNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int[][] types, byte[][][] data);

    private native int           queryRemoteLowEnergyDeviceAdvertisingDataNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int[][] types, byte[][][] data, boolean scanResponse);

    private native int           queryRemoteDeviceServicesNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean forceUpdate, byte[][] serviceDataBuffer);

    private native int           queryRemoteLowEnergyDeviceServicesNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean forceUpdate, byte[][] serviceDataBuffer);

    private native int           addRemoteDeviceNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int classOfDevice);

    private native int           addRemoteDeviceNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int classOfDevice, String friendlyName, int applicationInfo);

    private native int           deleteRemoteDeviceNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    private native int           updateRemoteDeviceApplicationDataNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, String friendlyName, int applicationInfo);

    private native int           deleteRemoteDevicesNative(int deleteDevicesFilter);

    private native int           pairWithRemoteDeviceNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean forcePair);

    private native int           pairWithRemoteLowEnergyDeviceNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean forcePair, boolean stayConnected);

    private native int           cancelPairWithRemoteDeviceNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    private native int           unPairRemoteDeviceNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    private native int           unPairRemoteLowEnergyDeviceNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    private native int           authenticateRemoteDeviceNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    private native int           authenticateRemoteLowEnergyDeviceNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    private native int           encryptRemoteDeviceNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    private native int           encryptRemoteLowEnergyDeviceNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    private native int           connectWithRemoteDeviceNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int connectFlags);

    private native int           connectWithRemoteLowEnergyDeviceNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int connectFlags);

    private native int           disconnectRemoteDeviceNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean force);

    private native int           disconnectRemoteLowEnergyDeviceNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean force);

    private native int           setRemoteDeviceLinkActiveNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    //FIXME
    //    private native int           createServiceRecordNative(boolean persistClient, long[] leastSigBytes, long[] mostSigBytes);
    //
    //    private native int           deleteServiceRecordNative(int serviceRecordHandle);
    //
    //
    //    private native int           addServiceRecordAttributeNative(int serviceRecordHandle, int attributeID, SDP_Data_Element_t *SDP_Data_Element);
    //
    //    private native int           deleteServiceRecordAttributeNative(int serviceRecordHandle, int attributeID);

    private native int           sendPinCodeNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte[] pinCode);

    private native int           sendUserConfirmationNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean accept);

    private native int           sendPasskeyNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int passkey);

    private native int           sendOutOfBandDataNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte[] simplePairingHash, byte[] simplePairingRandomizer);

    private native int           sendIOCapabilitiesNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int ioCapability, boolean outOfBandDataPresent, boolean mitmProtectionRequired, int bondingType);

    private native int           sendLowEnergyUserConfirmationNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, boolean accept);

    private native int           sendLowEnergyPasskeyNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int passkey);

    private native int           sendLowEnergyOutOfBandDataNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, byte[] encryptionKey);

    private native int           sendLowEnergyIOCapabilitiesNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6, int ioCapability, boolean outOfBandDataPresent, boolean mitmProtectionRequired, int bondingType);

    private native long[][]      querySupportedServicesNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);

    private native long[][]      querySupportedLowEnergyServicesNative(byte rAddr1, byte rAddr2, byte rAddr3, byte rAddr4, byte rAddr5, byte rAddr6);
}
