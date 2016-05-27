/*
 * ANTM.java Copyright 2010 - 2014 Stonestreet One, LLC. All Rights Reserved.
 */
package com.stonestreetone.bluetopiapm;

import java.util.EnumSet;

/**
 * Java wrapper for ANT Plus Manager API for Stonestreet One Bluetooth Protocol
 * Stack Platform Manager.
 */
public class ANTM {

    private final EventCallback callbackHandler;

    private boolean             disposed;

    private long                localData;

    static {
        System.loadLibrary("btpmj");
        initClassNative();
    }

    private static final int    BTPM_ERROR_CODE_INVALID_PARAMETER = 10001;

    /**
     *
     * Constructs a new instance of the ANT Plus Manager class.
     *
     * @param eventCallback
     *            The interface to which ANT Plus events are served.
     * @throws ServerNotReachableException
     *             Thrown if the server is not reachable.
     * @throws BluetopiaPMException
     *             Thrown if the ANT Manager fails to register a callback with
     *             the PM Server.
     */
    public ANTM(EventCallback eventCallback) throws ServerNotReachableException, BluetopiaPMException {

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

    /**
     * Assigns an ANT channel on the local ANT+ system.
     *
     * @param channelNumber
     *            The channel number to register.
     * @param channelType
     *            The channel type to be assigned to the channel.
     * @param networkNumber
     *            The network number to be user for the channel, or zero to use
     *            the default public network.
     * @param extendedAssignment
     *            The extended assignment to be used for the channel.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int assignChannel(int channelNumber, ChannelType channelType, int networkNumber, EnumSet<ExtendedAssignmentFeatures> extendedAssignment) {
        int assignmentMask = 0;

        for(ExtendedAssignmentFeatures feature : extendedAssignment)
            assignmentMask |= feature.value;

        return assignChannel(channelNumber, channelType.value, networkNumber, assignmentMask);
    }

    /**
     * Assigns an ANT channel on the local ANT+ system.
     *
     * @param channelNumber
     *            The channel number to register.
     * @param channelType
     *            The channel type to be assigned to the channel.
     * @param networkNumber
     *            The network number to be user for the channel, or zero to use
     *            the default public network.
     * @param extendedAssignment
     *            The extended assignment to be used for the channel.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int assignChannel(int channelNumber, int channelType, int networkNumber, int extendedAssignment) {
        return assignChannelNative(channelNumber, channelType, networkNumber, extendedAssignment);
    }

    /**
     * Unassigns an ANT channel on the local ANT+ system. A channel must be
     * unassigned before it can be reassigned using the ANTM_Assign_Channel()
     * API.
     *
     * @param channelNumber
     *            Channel number to un-assign.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int unAssignChannel(int channelNumber) {
        return unAssignChannelNative(channelNumber);
    }

    /**
     * Configures an ANT channel on the local ANT+ system.
     *
     * @param channelNumber
     *            Channel number to configure.
     * @param deviceNumber
     *            Device number to search for on the channel, or zero to scan
     *            for any device number.
     * @param deviceType
     *            Device type to search for on the channel, or zero to scan for
     *            any device type.
     * @param transmissionType
     *            Transmission type to search for on the channel, or zero to
     *            scan for any transmission type.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int setChannelID(int channelNumber, int deviceNumber, int deviceType, int transmissionType) {
        return setChannelIDNative(channelNumber, deviceNumber, deviceType, transmissionType);
    }

    /**
     * Configures the messaging period for an ANT channel on the local ANT+
     * system. <br>
     * <br>
     * <i>Note:</i> The actual messaging period calculated by the ANT device
     * will be {@code messagingPeriod} * 32768 (e.g. to send at 4Hz, set
     * {@code messagingPeriod} to 32678/4 = 8192).
     *
     * @param channelNumber
     *            Channel number to configure.
     * @param messagingPeriod
     *            Channel messaging period to set on the channel.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int setChannelPeriod(int channelNumber, int messagingPeriod) {
        return setChannelPeriodNative(channelNumber, messagingPeriod);
    }

    /**
     * Configures the amount of time that the receiver will search for an ANT
     * channel before timing out. <br>
     * <br>
     * <i>Note:</i> The actual search timeout calculated by the ANT device will
     * be {@code searchTimeout} * 2.5 seconds. A special search timeout value of
     * zero will disable high priority search mode on Non-AP1 devices. A special
     * search value of 255 will result in an infinite search timeout. Specifying
     * these search values on AP1 devices will not have any special effect.
     *
     * @param channelNumber
     *            Channel number to configure.
     * @param searchTimeout
     *            Search timeout to set on the channel.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int setChannelSearchTimeout(int channelNumber, int searchTimeout) {
        return setChannelSearchTimeoutNative(channelNumber, searchTimeout);
    }

    /**
     * Configures the channel frequency for an ANT channel. <br>
     * <br>
     * <i>Note:</i> The actual messaging period calculated by the ANT device
     * will be (2400 + RFFrequency) MHz.
     *
     * @param channelNumber
     *            Channel number to configure.
     * @param RFFrequency
     *            Channel frequency to set on the channel.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int setChannelRfFrequency(int channelNumber, int RFFrequency) {
        return setChannelRfFrequencyNative(channelNumber, RFFrequency);
    }

    /**
     * Configures the network key for an ANT channel. <br>
     * <br>
     * <i>Note:</i> Setting this network key is not required when using the
     * default public channel.
     *
     * @param networkNumber
     *            Channel number to configure.
     * @param networkKey
     *            ANT network key to set on the channel.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int setNetworkKey(int networkNumber, NetworkKey networkKey) {
        byte[] networkKeyBytes = networkKey.toArray();
        return setNetworkKeyNative(networkNumber, networkKeyBytes[0], networkKeyBytes[1], networkKeyBytes[2], networkKeyBytes[3], networkKeyBytes[4], networkKeyBytes[5], networkKeyBytes[6], networkKeyBytes[7]);
    }

    /**
     * Configures the transmit power on the local ANT system.
     *
     * @param transmitPower
     *            Transmit power to set on the device.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int setTransmitPower(TransmitPowerLevel transmitPower) {
        return setTransmitPowerNative(transmitPower.ordinal());
    }

    /**
     * Configures the transmit power on the local ANT system.
     *
     * @param transmitPower
     *            Transmit power to set on the device.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int setTransmitPower(int transmitPower) {
        return setTransmitPowerNative(transmitPower);
    }

    /**
     * Adds a channel number to the device's inclusion / exclusion list. <br>
     * <br>
     * <i>Note:</i> This feature is not available on all ANT devices. Check the
     * capabilities of this device before using this function.
     *
     * @param channelNumber
     *            Channel number to add to the list.
     * @param deviceNumber
     *            Device number to add to the list.
     * @param deviceType
     *            Device type to add to the list.
     * @param transmissionType
     *            Transmission type to add to the list.
     * @param listIndex
     *            List index to overwrite with the updated entry.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int addChannelID(int channelNumber, int deviceNumber, int deviceType, int transmissionType, int listIndex) {
        return addChannelIDNative(channelNumber, deviceNumber, deviceType, transmissionType, listIndex);
    }

    /**
     * Configures the inclusion / exclusion list on the local ANT+ system. <br>
     * <br>
     * <i>Note:</i> This feature is not available on all ANT devices. Check the
     * capabilities of this device before using this function.
     *
     * @param channelNumber
     *            Channel number on which the list should be configured.
     * @param listSize
     *            Size of the list.
     * @param exclude
     *            List type. Zero for inclusion, and one for exclusion.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int configureInclusionExclusionList(int channelNumber, int listSize, int exclude) {
        return configureInclusionExclusionListNative(channelNumber, listSize, exclude);
    }

    /**
     * Configures the transmit power for an ANT channel. <br>
     * <br>
     * <i>Note:</i> This feature is not available on all ANT devices.
     *
     * @param channelNumber
     *            Channel number to configure.
     * @param transmitPower
     *            Transmit power level for the specified channel.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int setChannelTransmitPower(int channelNumber, TransmitPowerLevel transmitPower) {
        return setChannelTransmitPowerNative(channelNumber, transmitPower.ordinal());
    }

    /**
     * Configures the transmit power for an ANT channel. <br>
     * <br>
     * <i>Note:</i> This feature is not available on all ANT devices.
     *
     * @param channelNumber
     *            Channel number to configure.
     * @param transmitPower
     *            Transmit power level for the specified channel.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int setChannelTransmitPower(int channelNumber, int transmitPower) {
        return setChannelTransmitPowerNative(channelNumber, transmitPower);
    }

    /**
     * Configures the duration in which the receiver will search for a channel
     * in low priority mode before switching to high priority mode. <br>
     * <br>
     * <i>Note:</i> This feature is not available on all ANT devices. <br>
     * <br>
     * <i>Note:</i> The actual search timeout calculated by the ANT device will
     * be SearchTimeout * 2.5 seconds. A special search timeout value of zero
     * will disable low priority search mode. A special search value of 255 will
     * result in an infinite low priority search timeout.
     *
     * @param channelNumber
     *            Channel number to configure.
     * @param searchTimeout
     *            Search timeout to set on the channel.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int setLowPriorityChannelSearchTimeout(int channelNumber, int searchTimeout) {
        return setLowPriorityChannelSearchTimeoutNative(channelNumber, searchTimeout);
    }

    /**
     * Configures an ANT channel on the local ANT+ system. Configures the
     * channel ID in the same way as ANTM_Set_Channel_ID(), except it uses the
     * two LSB of the device's serial number as the device's number. <br>
     * <br>
     * <i>Note:</i> This feature is not available on all ANT devices. Check the
     * capabilities of this device before using this function.
     *
     * @param channelNumber
     *            Channel number to configure.
     * @param deviceType
     *            Device type to search for on the channel, or zero to scan for
     *            any device type.
     * @param transmissionType
     *            Transmission type to search for on the channel, or zero to
     *            scan for any transmission type.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int setSerialNumberChannelID(int channelNumber, int deviceType, int transmissionType) {
        return setSerialNumberChannelIDNative(channelNumber, deviceType, transmissionType);
    }

    /**
     * Enables or disables extended Rx messages for an ANT channel on the local
     * ANT+ system. <br>
     * <br>
     * <i>Note:</i> This feature is not available on all ANT devices. Check the
     * capabilities of this device before using this function.
     *
     * @param enable
     *            Whether or not to enable extended Rx messages.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int enableExtendedMessages(boolean enable) {
        return enableExtendedMessagesNative(enable);
    }

    /**
     * Enables or disables the LED on the local ANT+ system. <br>
     * <br>
     * <i>Note:</i> This feature is not available on all ANT devices. Check the
     * capabilities of this device before using this function.
     *
     * @param enable
     *            Whether or not to enable the LED.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int enableLED(boolean enable) {
        return enableLEDNative(enable);
    }

    /**
     * Enables the 32kHz crystal input on the local ANT+ system. <br>
     * <br>
     * <i>Note:</i> This feature is not available on all ANT devices. <br>
     * <br>
     * <i>Note:</i> This function should only be sent when a startup message is
     * received.
     *
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int enableCrystal() {
        return enableCrystalNative();
    }

    /**
     * Enables or disables each extended Rx message on the local ANT+ system. <br>
     * <br>
     * <i>Note:</i> This feature is not available on all ANT devices.
     *
     * @param enableExtendedMessages
     *            EnumSet of Rx messages that should be enabled.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int configureExtendedMessages(EnumSet<ExtendedMessagesFlags> enableExtendedMessages) {
        int enableExtendedMessagesMask = 0;

        for(ExtendedMessagesFlags flag : enableExtendedMessages)
            enableExtendedMessagesMask |= flag.value;

        return configureExtendedMessages(enableExtendedMessagesMask);
    }

    /**
     * Enables or disables each extended Rx message on the local ANT+ system. <br>
     * <br>
     * <i>Note:</i> This feature is not available on all ANT devices.
     *
     * @param enableExtendedMessagesMask
     *            Bitmask of extended Rx messages that shall be enabled or
     *            disabled.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int configureExtendedMessages(int enableExtendedMessagesMask) {
        return configureExtendedMessagesNative(enableExtendedMessagesMask);
    }

    /**
     * Configures the three operating frequencies for an ANT channel. <br>
     * <br>
     * <i>Note:</i> This feature is not available on all ANT devices.
     *
     * <br>
     * <br>
     * <i>Note:</i> The operating frequency agilities should only be configured
     * after channel assignment and only if frequency agility bit has been set
     * in the ExtendedAssignment argument of ANTM_Assign_Channel. Frequency
     * agility should NOT be used with shared, Tx only, or Rx only channels.
     *
     * @param channelNumber
     *            Channel number to configure.
     * @param frequencyAgility1
     *            Operating agility frequency to set.
     * @param frequencyAgility2
     *            Operating agility frequency to set.
     * @param frequencyAgility3
     *            Operating agility frequency to set.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int configureFrequencyAgility(int channelNumber, int frequencyAgility1, int frequencyAgility2, int frequencyAgility3) {
        return configureFrequencyAgilityNative(channelNumber, frequencyAgility1, frequencyAgility2, frequencyAgility3);
    }

    /**
     * Configures proximity search requirement on the local ANT+ system. <br>
     * <br>
     * <i>Note:</i> This feature is not available on all ANT devices. Check the
     * request capabilities of the device before using this function. <br>
     * <br>
     * <i>Note:</i> The search threshold value is cleared once a proximity
     * search has completed successfully. If another proximity search is desired
     * after a successful search, then the threshold value must be reset.
     *
     * @param channelNumber
     *            Channel number to configure.
     * @param searchThreshold
     *            Search threshold to set.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int setProximitySearch(int channelNumber, int searchThreshold) {
        return setProximitySearchNative(channelNumber, searchThreshold);
    }

    /**
     * Configures the search priority of an ANT channel on the local ANT+
     * system. <br>
     * <br>
     * <i>Note:</i> This feature is not available on all ANT devices.
     *
     * @param channelNumber
     *            Channel number to configure.
     * @param searchPriority
     *            Search priority to set.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int setChannelSearchPriority(int channelNumber, int searchPriority) {
        return setChannelSearchPriorityNative(channelNumber, searchPriority);
    }

    /**
     * Configures the USB descriptor string on the local ANT+ system. <br>
     * <br>
     * <i>Note:</i> This feature is not available on all ANT devices.
     *
     * @param stringNumber
     *            Descriptor string type to set.
     * @param descriptorString
     *            Descriptor string to be set.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int setUSBDescriptorString(USBDescriptorStringNumber stringNumber, String descriptorString) {
        return setUSBDescriptorString(stringNumber.ordinal(), descriptorString);
    }

    /**
     * Configures the USB descriptor string on the local ANT+ system. <br>
     * <br>
     * <i>Note:</i> This feature is not available on all ANT devices.
     *
     * @param stringNumber
     *            Descriptor string type to set.
     * @param descriptorString
     *            Descriptor string to be set.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int setUSBDescriptorString(int stringNumber, String descriptorString) {
        return setUSBDescriptorStringNative(stringNumber, descriptorString);
    }

    /**
     * Resets the ANT module on the local ANT+ system. A delay of at least 500ms
     * is suggested after calling this function to allow time for the module to
     * reset.
     *
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int resetSystem() {
        return resetSystemNative();
    }

    /**
     * Opens an ANT channel on the local ANT+ system.
     *
     * @param channelNumber
     *            Channel number to be opened.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int openChannel(int channelNumber) {
        return openChannelNative(channelNumber);
    }

    /**
     * Closes an ANT channel on the local ANT+ system.
     *
     * @param channelNumber
     *            Channel number to be closed.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int closeChannel(int channelNumber) {
        return closeChannelNative(channelNumber);
    }

    /**
     * Requests an information message from an ANT channel on the local ANT+
     * system.
     *
     * @param channelNumber
     *            Channel number that the request will be sent to.
     * @param messageID
     *            Message ID being requested from the channel.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int requestMessage(int channelNumber, int messageID) {
        return requestMessageNative(channelNumber, messageID);
    }

    /**
     * Opens an ANT channel in continuous scan mode on the local ANT+ system. <br>
     * <br>
     * <i>Note:</i> This feature is not available on all ANT devices. Check the
     * request capabilities of the device before using this function. <br>
     * <br>
     * <i>Note:</i> No other channels can operate when a single channel is
     * opened in Rx scan mode.
     *
     * @param channelNumber
     *            Channel number to be opened.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int openRxScanMode(int channelNumber) {
        return openRxScanModeNative(channelNumber);
    }

    /**
     * Puts the ANT+ system in ultra low-power mode. <br>
     * <br>
     * <i>Note:</i> This feature is not available on all ANT devices. <br>
     * <br>
     * <i>Note:</i> This feature must be used in conjunction with setting the
     * SLEEP/(!MSGREADY) line on the ANT chip to the appropriate value.
     *
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int sleepMessage() {
        return sleepMessageNative();
    }

    /**
     * Responsible for sending broadcast data from an ANT channel no the local
     * ANT+ system. <br>
     * <br>
     * <i>Note:</i> The maximum payload size for an ANT packet is 8 bytes. The
     * function will not accept more than this maximum.
     *
     * @param channelNumber
     *            Channel number that the data will be broadcast on.
     * @param data
     *            Byte array of the data to send.
     *
     * @throws IllegalArgumentException
     *             if the data size is invalid.
     *
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int sendBroadcastData(int channelNumber, byte[] data) throws IllegalArgumentException {
        return sendBroadcastData(channelNumber, data, 0, (data == null)?0:data.length);
    }

    /**
     * Sends acknowledged data from an ANT channel on the local ANT+ system. <br>
     * <br>
     * <i>Note:</i> The maximum payload size for an ANT packet is 8 bytes. The
     * function will not accept more than this maximum.
     *
     * @param channelNumber
     *            Channel number that the data will be sent on.
     * @param data
     *            Byte array of the acknowledged data to send.
     *
     * @throws IllegalArgumentException
     *             if the data size is invalid.
     *
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int sendAcknowledgedData(int channelNumber, byte[] data) throws IllegalArgumentException {
        return sendAcknowledgedData(channelNumber, data, 0, (data == null)?0:data.length);
    }

    /**
     * Sends burst transfer data from an ANT channel on the local ANT+ system. <br>
     * <br>
     * <i>Note:</i> The maximum payload size for an ANT packet is 8 bytes. The
     * function will not accept more than this maximum.
     *
     * @param sequenceChannelNumber
     *            Sequence / channel number that the data will be sent on.
     * @param data
     *            Byte array of the burst data to send.
     *
     * @throws IllegalArgumentException
     *             if the data size is invalid.
     *
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int sendBurstTransferData(int sequenceChannelNumber, byte[] data) throws IllegalArgumentException {
        return sendBurstTransferData(sequenceChannelNumber, data, 0, (data == null)?0:data.length);
    }

    /**
     * Responsible for sending broadcast data from an ANT channel no the local
     * ANT+ system. <br>
     * <br>
     * <i>Note:</i> The maximum payload size for an ANT packet is 8 bytes. The
     * function will not accept more than this maximum.
     *
     * @param channelNumber
     *            Channel number that the data will be broadcast on.
     * @param data
     *            Byte array of the data to send.
     * @param offset
     *            Offset.
     * @param length
     *            Length of the data being sent.
     *
     * @throws IllegalArgumentException
     *             if the data size is invalid.
     *
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int sendBroadcastData(int channelNumber, byte[] data, int offset, int length) throws IllegalArgumentException {
        int ret_val = sendBroadcastDataNative(channelNumber, data, offset, length);

        if(ret_val == BTPM_ERROR_CODE_INVALID_PARAMETER)
            throw new IllegalArgumentException("Data is incorrect size");

        return ret_val;
    }

    /**
     * Sends acknowledged data from an ANT channel on the local ANT+ system. <br>
     * <br>
     * <i>Note:</i> The maximum payload size for an ANT packet is 8 bytes. The
     * function will not accept more than this maximum.
     *
     * @param channelNumber
     *            Channel number that the data will be sent on.
     * @param data
     *            Byte array of the acknowledged data to send.
     * @param offset
     *            Offset.
     * @param length
     *            Length of the acknowledged data being sent.
     *
     * @throws IllegalArgumentException
     *             if the data size is invalid.
     *
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int sendAcknowledgedData(int channelNumber, byte[] data, int offset, int length) throws IllegalArgumentException {
        int ret_val = sendAcknowledgedDataNative(channelNumber, data, offset, length);

        if(ret_val == BTPM_ERROR_CODE_INVALID_PARAMETER)
            throw new IllegalArgumentException("Data is incorrect size");

        return ret_val;
    }

    /**
     * Sends burst transfer data from an ANT channel on the local ANT+ system. <br>
     * <br>
     * <i>Note:</i> The maximum payload size for an ANT packet is 8 bytes. The
     * function will not accept more than this maximum.
     *
     * @param sequenceChannelNumber
     *            Sequence / Channel number that the data will be sent on.
     * @param data
     *            Byte array of the burst data to send.
     * @param offset
     *            Offset.
     * @param length
     *            Length of the burst data being sent.
     *
     * @throws IllegalArgumentException
     *             if the data size is invalid.
     *
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int sendBurstTransferData(int sequenceChannelNumber, byte[] data, int offset, int length) throws IllegalArgumentException {
        int ret_val = sendBurstTransferDataNative(sequenceChannelNumber, data, offset, length);

        if(ret_val == BTPM_ERROR_CODE_INVALID_PARAMETER)
            throw new IllegalArgumentException("Data is incorrect size");

        return ret_val;
    }

    /**
     * Puts the ANT+ system in CW test mode. <br>
     * <br>
     * <i>Note:</i> This feature should ONLY be used immediately after resetting
     * the ANT module.
     *
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int initializeCWTestMode() {
        return initializeCWTestModeNative();
    }

    /**
     * Puts the ANT module in CW test mode using a given transmit power level
     * and RF frequency. <br>
     * <br>
     * <i>Note:</i> This feature should ONLY be used immediately after calling
     * {@link #initializeCWTestMode()}.
     *
     * @param txPower
     *            Transmit power level to be used.
     * @param RFFrequency
     *            RF frequency to be used.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int setCWTestMode(int txPower, int RFFrequency) {
        return setCWTestModeNative(txPower, RFFrequency);
    }

    /**
     * Send a preformatted ANT packet to the controller.
     *
     * @param packet
     *            The preformatted ANT packet. The buffer must contain a
     *            formatted ANT packet _without_ the leading Sync or trailing
     *            Checksum bytes.
     * @return Zero if successful, otherwise this function returns a negative
     *         error code.
     */
    public int sendRawPacket(byte packet[]) {
        return sendRawPacketNative(packet);
    }

    /**
     * Send a preformatted ANT packet to the controller without blocking.
     *
     * @param packet
     *            The preformatted ANT packet. The buffer must contain a
     *            formatted ANT packet _without_ the leading Sync or trailing
     *            Checksum bytes.
     */
    public void sendRawPacketAsync(byte packet[]) {
        sendRawPacketAsyncNative(packet);
    }

    /* Event Callback Handlers */
    private void startupMessageEvent(int startupMessage) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.startupMessageEvent(StartupMessageFlags.fromMask(startupMessage));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void channelResponseEvent(int channelNumber, int messageID, int messageCode) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.channelResponseEvent(channelNumber, messageID, ChannelResponseMessageCode.fromValue(messageCode));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void channelStatusEvent(int channelNumber, int channelStatus) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.channelStatusEvent(channelNumber, channelStatus);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void channelIDEvent(int channelNumber, int deviceNumber, int deviceTypeID, int transmissionType) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.channelIDEvent(channelNumber, deviceNumber, deviceTypeID, transmissionType);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void ANTVersionEvent(byte[] versionData) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.ANTVersionEvent(versionData);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * Format for options from JNI: ********************************************
     * Byte 3 * Byte 2 * Byte 1 * Byte 0 *
     * ******************************************** Standard * Advanced *
     * Advanced2 * Reserved * ********************************************
     */

    private void capabilitiesEvent(int maxChannels, int maxNetworks, int options) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                EnumSet<ANTCapabilities> capabilities = ANTCapabilities.fromMask(options);

                callbackHandler.capabilitiesEvent(maxChannels, maxNetworks, capabilities);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcastDataPacketEvent(int channelNumber, byte[] data) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.broadcastDataPacketEvent(channelNumber, data);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void acknowledgedDataPacketEvent(int channelNumber, byte[] data) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.acknowledgedDataPacketEvent(channelNumber, data);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void burstDataPacketEvent(int sequenceChannelNumber, byte[] data) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.burstDataPacketEvent(sequenceChannelNumber, data);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void extendedBroadcastDataPacketEvent(int channelNumber, int deviceNumber, int deviceType, int transmissionType, byte[] data) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.extendedBroadcastDataPacketEvent(channelNumber, deviceNumber, deviceType, transmissionType, data);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void extendedAcknowledgedDataPacketEvent(int channelNumber, int deviceNumber, int deviceType, int transmissionType, byte[] data) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.extendedAcknowledgedDataPacketEvent(channelNumber, deviceNumber, deviceType, transmissionType, data);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void extendedBurstDataPacketEvent(int sequenceChannelNumber, int deviceNumber, int deviceType, int transmissionType, byte[] data) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.extendedBurstDataPacketEvent(sequenceChannelNumber, deviceNumber, deviceType, transmissionType, data);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void rawDataPacketEvent(byte[] data) {
        EventCallback callbackHandler = this.callbackHandler;

        if(callbackHandler != null) {
            try {
                callbackHandler.rawDataPacketEvent(data);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *
     * The interface should be implemented and provided to a call to
     * {@link ANTM#ANTM(EventCallback)} in order to receive callbacks.
     *
     */
    public interface EventCallback {
        /**
         * Received when the ANT modules is started or reset.
         *
         * @param startupMessage
         *            The set of flags representing what event this startup is
         *            coming from.
         */
        public void startupMessageEvent(EnumSet<StartupMessageFlags> startupMessage);

        /**
         *
         * Received either in response to a sent message or from an RF event.
         *
         * @param channelNumber
         *            The channel number the message is related to.
         * @param messageID
         *            The ID of the message being responded to (set to 1 if this
         *            is an RF event).
         * @param messageCode
         *            The code for the response.
         */
        public void channelResponseEvent(int channelNumber, int messageID, ChannelResponseMessageCode messageCode);

        /**
         * Contains information about the status of a channel.
         *
         * @param channelNumber
         *            The channel the status relates to.
         * @param channelStatus
         *            The status of the channel.
         */
        public void channelStatusEvent(int channelNumber, int channelStatus);

        /**
         * Response to a request for a channel ID
         *
         * @param channelNumber
         *            The channel number requested.
         * @param deviceNumber
         *            The device number for the channel.
         * @param deviceTypeID
         *            The device type for the channel.
         * @param transmissionType
         *            The transmission type for the channel.
         */
        public void channelIDEvent(int channelNumber, int deviceNumber, int deviceTypeID, int transmissionType);

        /**
         * Response to a request for the ANT Version.
         *
         * @param versionData
         *            The version data returned.
         */
        public void ANTVersionEvent(byte[] versionData);

        /**
         * Response to a request for ANT Capabilities.
         *
         * @param maxChannels
         *            Maximum number of available channels.
         * @param maxNetworks
         *            Maximum number of available networks.
         * @param capabilityOptions
         *            Set of capabilities that are supported.
         */
        public void capabilitiesEvent(int maxChannels, int maxNetworks, EnumSet<ANTCapabilities> capabilityOptions);

        /**
         *
         * Received when a broadcast data packet is received.
         *
         * @param channelNumber
         *            The channel that received the packet.
         * @param data
         *            The packet data.
         */
        public void broadcastDataPacketEvent(int channelNumber, byte[] data);

        /**
         * Received when an acknowledged data packet is received.
         *
         * @param channelNumber
         *            The channel that received the packet.
         * @param data
         *            The packet data.
         */
        public void acknowledgedDataPacketEvent(int channelNumber, byte[] data);

        /**
         * Received when a burst data packet is received.
         *
         * @param sequenceChannelNumber
         *            The channel that received the packet.
         * @param data
         *            The packet data.
         */
        public void burstDataPacketEvent(int sequenceChannelNumber, byte[] data);

        /**
         * Received when a flagged extended broadcast data packet is received.
         *
         * @param channelNumber
         *            The channel the received the packet.
         * @param deviceNumber
         *            The device number associated with the channel.
         * @param deviceType
         *            The device type associated with the channel.
         * @param transmissionType
         *            The device type associated with the channel.
         * @param data
         *            The packet data.
         */
        public void extendedBroadcastDataPacketEvent(int channelNumber, int deviceNumber, int deviceType, int transmissionType, byte[] data);

        /**
         * Received when a flagged extended acknowledged data packet is
         * received.
         *
         * @param channelNumber
         *            The channel the received the packet.
         * @param deviceNumber
         *            The device number associated with the channel.
         * @param deviceType
         *            The device type associated with the channel.
         * @param transmissionType
         *            The device type associated with the channel.
         * @param data
         *            The packet data.
         */
        public void extendedAcknowledgedDataPacketEvent(int channelNumber, int deviceNumber, int deviceType, int transmissionType, byte[] data);

        /**
         * Received when a flagged extended burst data packet is received.
         *
         * @param sequenceChannelNumber
         *            The channel the received the packet.
         * @param deviceNumber
         *            The device number associated with the channel.
         * @param deviceType
         *            The device type associated with the channel.
         * @param transmissionType
         *            The device type associated with the channel.
         * @param data
         *            The packet data.
         */
        public void extendedBurstDataPacketEvent(int sequenceChannelNumber, int deviceNumber, int deviceType, int transmissionType, byte[] data);

        /**
         * Received when any ANT packet is received when operating in Raw Mode.
         *
         * @param data
         *            The raw ANT packet data
         *
         */
        public void rawDataPacketEvent(byte[] data);
    }

    /**
     * Represents an ANT Network Key.
     */
    public static class NetworkKey {
        private final byte[] octets;

        /**
         * Creates an ANT NetworkKey based on the the supplied byte array.
         *
         * @param octets
         *            The 8 bytes of the network key
         *
         * @throws IllegalArgumentException
         *             Thrown if the supplied byte array is not 8 bytes
         */
        public NetworkKey(byte[] octets) throws IllegalArgumentException {
            if(octets.length != 8)
                throw new IllegalArgumentException("Network key must have 8 octets");

            this.octets = octets.clone();
        }

        /**
         * Creates an ANT NetworkKey base on the supplied bytes.
         *
         * @param b0
         *            Byte 0.
         * @param b1
         *            Byte 1.
         * @param b2
         *            Byte 2.
         * @param b3
         *            Byte 3.
         * @param b4
         *            Byte 4.
         * @param b5
         *            Byte 5.
         * @param b6
         *            Byte 6.
         * @param b7
         *            Byte 7.
         */
        public NetworkKey(byte b0, byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7) {
            octets = new byte[] {b0, b1, b2, b3, b4, b5, b6, b7};
        }

        /**
         * Get the array representation of this network key.
         *
         * @return An 8-byte long array representing the network key.
         */
        byte[] toArray() {
            return octets.clone();
        }
    }

    /**
     * Represents Flags supplied in a Startup Message.
     */
    public static enum StartupMessageFlags {
        /**
         * Indicates the module has powered on.
         */
        POWER_ON_RESET(0x00),

        /**
         * Indicates the startup is after a hardware reset.
         */
        HARDWARE_RESET(0x01),

        /**
         * Indicates the startup is after a watch dog reset.
         */
        WATCH_DOG_RESET(0x02),

        /**
         * Indicates the startup is after a command reset.
         */
        COMMAND_RESET(0x20),

        /**
         * Indicates the startup is after a synchronous reset.
         */
        SYNCHRONOUS_RESET(0x40),

        /**
         * Indicates the startup is after a suspend reset.
         */
        SUSPEND_RESET(0x80);

        /**
         * The value for the flag assigned by the ANT protocol.
         */
        public final int value;

        private StartupMessageFlags(int v) {
            value = v;
        }

        /**
         * Creates a set of startup flags from the corresponding startup message
         * mask.
         *
         * @param mask
         *            The start message received.
         * @return The corresponding set of flags.
         */
        public static EnumSet<StartupMessageFlags> fromMask(int mask) {
            EnumSet<StartupMessageFlags> flags = EnumSet.noneOf(StartupMessageFlags.class);
            if(mask == 0x00)
                flags.add(POWER_ON_RESET);
            else {
                for(StartupMessageFlags flag : StartupMessageFlags.values()) {
                    if((flag.value & mask) > 0)
                        flags.add(flag);
                }
            }

            return flags;
        }
    }

    /**
     * Represents Extended Features that can be enabled.
     */
    public static enum ExtendedAssignmentFeatures {
        /**
         * Flag to enable background scanning channel.
         */
        BACKGROUND_SCANNING_CHANNEL_ENABLE(0x01),

        /**
         * Flag to enable frequency agility.
         */
        FREQUENCY_AGILITY_ENABLE(0x04);

        /**
         * The value of the feature flag assigned by the ANT protocol
         */
        public final int value;

        private ExtendedAssignmentFeatures(int v) {
            value = v;
        }
    }

    /**
     * Represents types of extended messages to be enabled.
     *
     */
    public static enum ExtendedMessagesFlags {
        /**
         * Flag to enable Rx Timestamp extended messages.
         */
        RX_TIMESTAMP_OUTPUT(0x20),

        /**
         * Flag to enable RSSI extended messages.
         */
        RSSI_OUTPUT(0x40),

        /**
         * Flag to enable Channel id extended messages.
         */
        CHANNEL_ID_OUTPUT(0x80);

        /**
         * The value of the flag assigned by the ANT protocol.
         */
        public final int value;

        private ExtendedMessagesFlags(int v) {
            value = v;
        }
    }

    /**
     * Represents the possible Message Codes in the ChannelResponse event.
     */
    public static enum ChannelResponseMessageCode {
        /**
         * Returned on a successful operation.
         */
        RESPONSE_NO_ERROR(0x00),

        /**
         * A receive channel has timed out on searching. The search is
         * terminated, and the channel has been automatically closed. In order
         * to restart the search the Open Channel message must be sent again.
         */
        EVENT_RX_SEARCH_TIMEOUT(0x01),

        /**
         * A receive channel missed a message which it was expecting.
         */
        EVENT_RX_FAIL(0x02),

        /**
         * A Broadcast message has been transmitted successfully . This event
         * should be used to send the next message for transmission to the ANT
         * device.
         */
        EVENT_TX(0x03),

        /**
         * A receive transfer has failed.
         */
        EVENT_TRANSFER_RX_FAILED(0x04),

        /**
         * An Acknowledged Data message or a Burst Transfer sequence has been
         * completed successfully. When transmitting Acknowledged Data or Burst
         * Transfer, there is no EVENT_TX message.
         */
        EVENT_TRANSFER_TX_COMPLETED(0x05),

        /**
         * An Acknowledged Data message, or a Burst Transfer Message has been
         * initiated and the transmission failed to complete successfully.
         */
        EVENT_TRANSFER_TX_FAILED(0x06),

        /**
         * The channel has been successfully closed. When the Host sends a
         * message to close a channel, it first receives a
         * {@link #RESPONSE_NO_ERROR} to indicate that the message was
         * successfully received by ANT; however, {@code EVENT_CHANNEL_CLOSED}
         * is the actual indication of the closure of the channel. As such, the
         * Host must use this event message rather than the
         * {@link #RESPONSE_NO_ERROR} message to let a channel state machine
         * continue.
         */
        EVENT_CHANNEL_CLOSED(0x07),

        /**
         * The channel has dropped to search mode after missing too many
         * messages.
         */
        EVENT_RX_FAIL_GO_TO_SEARCH(0x08),

        /**
         * Two channels have drifted into each other and overlapped in time on
         * the device causing one channel to be blocked.
         */
        EVENT_CHANNEL_COLLISION(0x09),

        /**
         * Sent after a burst transfer begins.
         */
        EVENT_TRANSFER_TX_START(0x0A),

        /**
         * Returned on attempt to perform an action on a channel that is not
         * valid for the channel‟s state.
         */
        CHANNEL_IN_WRONG_STATE(0x15),

        /**
         * Attempted to transmit data on an unopened channel.
         */
        CHANNEL_NOT_OPENED(0x16),

        /**
         * Returned on attempt to open a channel before setting a valid ID.
         */
        CHANNEL_ID_NOT_SET(0x18),

        /**
         * Returned when an OpenRxScanMode() command is sent while other
         * channels are open.
         */
        CLOSE_ALL_CHANNELS(0x19),

        /**
         * Returned on an attempt to communicate on a channel with a transmit
         * transfer in progress.
         */
        TRANSFER_IN_PROGRESS(0x1F),

        /**
         * Returned when sequence number is out of order on a Burst Transfer
         */
        TRANSFER_SEQUENCE_NUMBER_ERROR(0x20),

        /**
         * Returned when a burst message passes the sequence number check but
         * will not be transmitted due to other reasons.
         */
        TRANSFER_IN_ERROR(0x21),

        /**
         * Returned if a data message is provided that is too large.
         */
        MESSAGE_SIZE_EXCEEDS_LIMIT(0x27),

        /**
         * Returned when message has invalid parameters.
         */
        INVALID_MESSAGE(0x28),

        /**
         * Returned when an invalid network number is provided.
         */
        INVALID_NETWORK_NUMBER(0x29),

        /**
         * Returned when the provided list ID or size exceeds the limit.
         */
        INVALID_LIST_ID(0x30),

        /**
         * Returned when attempting to transmit on ANT channel 0 in scan mode.
         */
        INVALID_SCAN_TX_CHANNEL(0x31),

        /**
         * Returned when invalid configuration commands are requested.
         */
        INVALID_PARAMETER_PROVIDED(0x33),

        /**
         * Only possible when using synchronous serial port. Indicates that one
         * or more events was lost due to excessive latency in reading out
         * events over the port.
         */
        EVENT_QUEUE_OVERFLOW(0x35),

        /**
         * Returned when the NVM for SensRcore mode is full.
         */
        NVM_FULL_ERROR(0x40),

        /**
         * Returned when writing to the NVM for SensRcore mode fails.
         */
        NVM_WRITE_ERROR(0x41),

        /**
         * Returned when configuration of a USB descriptor string fails.
         */
        USB_STRING_WRITE_FAIL(0x70),

        /**
         * Returned when a serial error occurs.
         */
        MSG_SERIAL_ERROR_ID(0xAE),

        /**
         * Returned when an unknown message code is received.
         */
        UNKNOWN_RESPONSE_CODE(0xFF);

        /**
         * The defined value for this code in the ANT protocol.
         */
        public final int value;

        private ChannelResponseMessageCode(int v) {
            value = v;
        }

        /**
         * Returns a ChannelResponseMessageCode value that corresponds to the
         * supplied protocol value.
         *
         * @param v
         *            The defined value of the message code.
         * @return The corresponding ChannelResponseMessageCode, or
         *         {@link ChannelResponseMessageCode#UNKNOWN_RESPONSE_CODE} if
         *         the supplied value is not valid.
         */
        public static ChannelResponseMessageCode fromValue(int v) {
            for(ChannelResponseMessageCode code : ChannelResponseMessageCode.values()) {
                if(v == code.value)
                    return code;
            }

            return UNKNOWN_RESPONSE_CODE;
        }
    }

    /**
     * Represents the types of channels which can be assigned.
     */
    public static enum ChannelType {
        /**
         * A bidirectional receive channel.
         */
        RECEIVE_CHANNEL(0x00),

        /**
         * A bidirectional transmit channel.
         */
        TRANSMIT_CHANNEL(0x10),

        /**
         * A unidirectional receive channel.
         */
        RECEIVE_ONLY_CHANNEL(0x40),

        /**
         * A unidirectional transmit channel.
         */
        TRANSMIT_ONLY_CHANNEL(0x50),

        /**
         * A shared bidirectional receive channel.
         */
        SHARED_BIDIRECTIONAL_RECEIVE_CHANNEL(0x20),

        /**
         * A shared bidirectional transmit channel.
         */
        SHARED_BIDIRECTIONAL_TRANSMIT_CHANNEL(0x30);

        /**
         * The value of the flag assigned by the ANT protocol.
         */
        public final int value;

        private ChannelType(int v) {
            value = v;
        }
    }

    /**
     * Represents the defined transmit power levels.
     */
    public static enum TransmitPowerLevel {
        /**
         * -20 dBm transmit power.
         */
        NEGATIVE_TWENTY_DBM,

        /**
         * -10 dBm transmit power.
         */
        NEGATIVE_TEN_DBM,

        /**
         * -5 dBm transmit power.
         */
        NEGATIVE_FIVE_DBM,

        /**
         * 0 dBm transmit power.
         */
        ZERO_DBM,

        /**
         * +4 dBm transmit power.
         */
        FOUR_DBM;
    }

    /**
     * Represents the different types of USB Descriptor strings that can be set.
     */
    public static enum USBDescriptorStringNumber {
        /**
         * Flag to set the PID and VID strings.
         */
        PID_VID,

        /**
         * Flag to set the manufacturer string.
         */
        MANUFACTURER,

        /**
         * Flag to set the device string.
         */
        DEVICE,

        /**
         * Flag to set the serial number string.
         */
        SERIAL_NUMBER
    }

    /**
     * Represents different capabilities that the ANT module is able to support.
     */
    public static enum ANTCapabilities {
        /*
         * Format for options from JNI:
         * ********************************************
         *  Byte 3  *  Byte 2  *  Byte 1   *  Byte 0  *
         * ********************************************
         * Standard * Advanced * Advanced2 * Reserved *
         * ********************************************
         */

        //Standard (Byte 3)

        /**
         * NO_RECEIVE_CHANNEL capability.
         */
        NO_RECEIVE_CHANNELS(0x01 << 24),

        /**
         * NO_TRANSMIT_CHANNEL capability.
         */
        NO_TRANSMIT_CHANNELS(0x02 << 24),

        /**
         * NO_RECEIVE_MESSAGES capability.
         */
        NO_RECEIVE_MESSAGES(0x04 << 24),

        /**
         * NO_TRANSMIT_MESSAGES capability.
         */
        NO_TRANSMIT_MESSAGES(0x08 << 24),

        /**
         * NO_ACKNOWLEDGED_MESSAGES capability.
         */
        NO_ACKNOWLEDGED_MESSAGES(0x10 << 24),

        /**
         * NO_BURST_MESSAGES capability.
         */
        NO_BURST_MESSAGES(0x20 << 24),

        //Advanced (Byte 2)

        /**
         * Network Enabled capability.
         */
        NETWORK_ENABLED(0x02 << 16),

        /**
         * Serial number capability.
         */
        SERIAL_NUMBER_ENABLED(0x08 << 16),

        /**
         * Per-Channel Tx Power capability.
         */
        PER_CHANNEL_TX_POWER_ENABLED(0x10 << 16),

        /**
         * Low priority search capability.
         */
        LOW_PRIORITY_SEARCH_ENABLED(0x20 << 16),

        /**
         * Script capability.
         */
        SCRIPT_ENABLED(0x40 << 16),

        /**
         * Search list capability.
         */
        SEARCH_LIST_ENABLED(0x80 << 16),

        //Advanced 2 (Byte 1)

        /**
         * LED Capability.
         */
        LED_ENABLED(0x01 << 8),

        /**
         * Extended Messaged capability.
         */
        EXTENDED_MESSAGE_ENABLED(0x02 << 8),

        /**
         * Scan Mode capability.
         */
        SCAN_MODE_ENABLED(0x04 << 8),

        /**
         * Proximity Search capability.
         */
        PROXIMITY_SEARCH_ENABLED(0x10 << 8),

        /**
         * Extended Assignment Capability.
         */
        EXTENDED_ASSIGN_ENABLED(0x20 << 8),

        /**
         * FS ANTFS capability.
         */
        FS_ANTFS_ENABLED(0x40 << 8);

        private final int value;

        private ANTCapabilities(int v) {
            value = v;
        }

        /**
         * Creates a set of capability flags from a corresponding bit mask.
         *
         * @param mask
         *            The bit mask which contains the flags to set.
         *
         * @return The set of flags corresponding to the bit mask
         */
        public static EnumSet<ANTCapabilities> fromMask(int mask) {
            EnumSet<ANTCapabilities> set = EnumSet.noneOf(ANTCapabilities.class);

            for(ANTCapabilities capabilities : ANTCapabilities.values()) {
                if((capabilities.value & mask) > 0) {
                    set.add(capabilities);
                }
            }

            return set;
        }
    }

    /* Native method declarations */

    private native static void initClassNative();

    private native void initObjectNative() throws ServerNotReachableException;

    private native void cleanupObjectNative();

    private native int assignChannelNative(int channelNumber, int channelType, int networkNumber, int extendedAssignment);

    private native int unAssignChannelNative(int channelNumber);

    private native int setChannelIDNative(int channelNumber, int deviceNumber, int deviceType, int transmissionType);

    private native int setChannelPeriodNative(int channelNumber, int messagingPeriod);

    private native int setChannelSearchTimeoutNative(int channelNumber, int searchTimeout);

    private native int setChannelRfFrequencyNative(int channelNumber, int RFFrequency);

    private native int setNetworkKeyNative(int networkNumber, byte byte0, byte byte1, byte byte2, byte byte3, byte byte4, byte byte5, byte byte6, byte byte7);

    private native int setTransmitPowerNative(int transmitPower);

    private native int addChannelIDNative(int channelNumber, int deviceNumber, int deviceType, int transmissionType, int listIndex);

    private native int configureInclusionExclusionListNative(int channelNumber, int listSize, int exclude);

    private native int setChannelTransmitPowerNative(int channelNumber, int transmitPower);

    private native int setLowPriorityChannelSearchTimeoutNative(int channelNumber, int searchTimeout);

    private native int setSerialNumberChannelIDNative(int channelNumber, int deviceType, int transmissionType);

    private native int enableExtendedMessagesNative(boolean enable);

    private native int enableLEDNative(boolean enable);

    private native int enableCrystalNative();

    private native int configureExtendedMessagesNative(int enableExtendedMessagesMask);

    private native int configureFrequencyAgilityNative(int channelNumber, int frequencyAgility1, int frequencyAgility2, int frequencyAgility3);

    private native int setProximitySearchNative(int channelNumber, int searchThreshold);

    private native int setChannelSearchPriorityNative(int channelNumber, int searchPriority);

    private native int setUSBDescriptorStringNative(int stringNumber, String descriptorString);

    private native int resetSystemNative();

    private native int openChannelNative(int channelNumber);

    private native int closeChannelNative(int channelNumber);

    private native int requestMessageNative(int channelNumber, int messageID);

    private native int openRxScanModeNative(int channelNumber);

    private native int sleepMessageNative();

    private native int sendBroadcastDataNative(int channelNumber, byte[] data, int offset, int length);

    private native int sendAcknowledgedDataNative(int channelNumber, byte[] data, int offset, int length);

    private native int sendBurstTransferDataNative(int sequenceChannelNumber, byte[] data, int offset, int length);

    private native int initializeCWTestModeNative();

    private native int setCWTestModeNative(int txPower, int RFFrequency);

    private native int sendRawPacketNative(byte[] packet);

    private native void sendRawPacketAsyncNative(byte[] packet);
}
