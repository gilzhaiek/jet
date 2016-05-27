
package com.dsi.ant.stonestreetoneantservice;

import android.util.Log;

import com.dsi.ant.chip.AntChipBase;
import com.dsi.ant.chip.BurstWriter;
import com.dsi.ant.chip.BurstWriter.HciResult;
import com.dsi.ant.chip.IAntChipEventReceiver;
import com.dsi.ant.chip.PacketFormat;

import java.util.EnumSet;

import com.dsi.ant.chip.AntChipState;
import com.stonestreetone.bluetopiapm.ANTM;
import com.stonestreetone.bluetopiapm.ANTM.ANTCapabilities;
import com.stonestreetone.bluetopiapm.ANTM.ChannelResponseMessageCode;
import com.stonestreetone.bluetopiapm.ANTM.StartupMessageFlags;
import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.DEVM;
import com.stonestreetone.bluetopiapm.DEVM.EventCallback;
import com.stonestreetone.bluetopiapm.DEVM.LocalDeviceFeature;
import com.stonestreetone.bluetopiapm.DEVM.LocalDeviceProperties;
import com.stonestreetone.bluetopiapm.DEVM.LocalPropertyField;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceProperties;
import com.stonestreetone.bluetopiapm.DEVM.RemotePropertyField;
import com.stonestreetone.bluetopiapm.ServerNotReachableException;

/**
 * Java wrapper for ANT Plus Manager API for Stonestreet One Bluetooth Protocol Stack Platform 
 * Manager.
 */
public class StonestreetOneAntChip extends AntChipBase implements BurstWriter.IHostControllerInterface {
    private static final String TAG = StonestreetOneAntChip.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final int BTPM_ERROR_CODE_SUCCESS = 0;
    private static final int BTPM_ERROR_CODE_ANT_UNABLE_TO_SEND_ANT_MESSAGE = -24001;
    private static final int BTPM_ERROR_CODE_ANT_MESSAGE_RESPONSE_ERROR = -24002;

    // Chip specific settings
    /** There is no sync byte in an ANT message. */
    private static final boolean HAS_SYNC_BYTE = false;
    /** There is no checksum in an ANT message. */
    private static final boolean HAS_CHECKSUM = false;

    /**
     * Combine this number of ANT burst packets in one HCI write packet.
     */
    private static final int NUM_BURST_MESSAGES_CONCATENATED = 8;

    private static final PacketFormat PACKET_FORMAT =
            new PacketFormat(HAS_SYNC_BYTE, HAS_CHECKSUM, NUM_BURST_MESSAGES_CONCATENATED);

    /** Stonestreet One stack Device Manager, for power control. */
    /* package */DEVM mDeviceManager;
    /** Stonestreet One stack ANT+ Manager. */
    /* package */ANTM mAntm;

    /** Sends events to ANT Radio Service */
    IAntChipEventReceiver mEventReceiver = null;

    /** The chip can only be enabled when the WiLink is in "ANT low power wireless mode" */
    private boolean mIsLowPowerWirelessModeAnt;

    /** Best guess at the power state of the ANT chip */
    private int mKnownChipState = AntChipState.CHIPSTATE_DISABLED;


    /**
     * Lock for state change requests and state change events to prevent others
     * from changing the known chip power state while they are using it.
     */
    private final Object mKnownChipStateChange_LOCK = new Object();

    private final BurstWriter mBurstWriter;
    /**
     * Constructs a new instance of the SS1 ANT+ API wrapper class, which will
     * parse any received packets.
     * 
     * @param isLowPowerWirelessModeAnt If the initial WiLink low power mode is ANT.
     */
    public StonestreetOneAntChip(boolean isLowPowerWirelessModeAnt) {
        if(DEBUG) Log.v(TAG, "init SS1 DEVM,ANTM");

        mIsLowPowerWirelessModeAnt = isLowPowerWirelessModeAnt;

        try {
            if(DEBUG) Log.v(TAG, "init SS1 DEVM start");
            try {
                mDeviceManager = new DEVM(mDeviceEventCallback);
            } catch(ServerNotReachableException e) {
                Log.e(TAG, "Failed to load DEVM", e);
            }

            if(DEBUG) Log.v(TAG, "init SS1 ANTM start");
            mAntm = new ANTM(mANTEventCallback);
        } catch(Exception e) {
            Log.e(TAG, "Cannot init SS1 DEVM, ANTM");
            throw new RuntimeException("Cannot init SS1 DEVM, ANTM");
        }

        if(DEBUG) Log.v(TAG, "init SS1 DEVM,ANTM DONE");

        mBurstWriter = new BurstWriter(this, PACKET_FORMAT);
        //if(mDeviceManager.disableLocalDeviceFeature(LocalDeviceFeature.BLUETOOTH_LOW_ENERGY)<0)
            //Log.e(TAG, "Cannot Disable BLE feature");
    }

    /**
     * Cleanup resources in use by this object. This object is no longer usable after this is
     * called.
     */
    public void destroy() {
        // To be safe, make sure the chip is disabled.
        disable();

        // Make sure our callback handlers are unregistered.
        if(mDeviceManager != null){
            mDeviceManager.dispose();
            mDeviceManager = null;
        }
        
        if(mAntm != null){
            mAntm.dispose();
            mAntm = null;
        }
    }

    /**
     * Notify this ANT chip that the WiLink is in "ANT mode" or not. 
     * 
     * @param isAnt true if the WiLink has become set to ANT mode.
     */
    public void onIsLowPowerWirelessModeAntChange(boolean isAnt) {
        if(DEBUG) Log.v(TAG, "onIsLowPowerWirelessModeAntChange: "+ isAnt);

        mIsLowPowerWirelessModeAnt = isAnt;

        if(isAnt) {
            enable();
        } else {
            // No "ANT part" to communicate with

            // Make sure there are no state change events processing
            synchronized(mKnownChipStateChange_LOCK) {
                // Update state if this is a new state
                if(!alreadyDisabling()) {
                    updateState(AntChipState.CHIPSTATE_DISABLED);
                }
            }
        }
    }

    @Override
    public String getChipName() {
        if(DEBUG) Log.v(TAG, "getChipName");

        return "built-in";
    }

    @Override
    public String getHardwareType() {
        if(DEBUG) Log.v(TAG, "getHardwareType");

        return "built-in";
    }

    @Override
    public int hardReset() {
        if(DEBUG) Log.v(TAG, "hardReset");

        // Hard reset not supported, ignore
        return getChipState();
    }

    @Override
    public void setEventReceiver(IAntChipEventReceiver eventReceiver) {
        if(DEBUG) Log.v(TAG, "setEventReceiver");

        mEventReceiver = eventReceiver;
    }

    @Override
    public boolean txBurst(int channel, byte[] data) {
        if(DEBUG) Log.d(TAG, "txBurst");

        return mBurstWriter.write(channel, data);
    }

    @Override
    public boolean txMessage(byte[] message) {
        if(DEBUG) Log.v(TAG, "txMessage");

        int sendRawPacketResult = sendRawPacket(message);

        return (BTPM_ERROR_CODE_SUCCESS == sendRawPacketResult);

    }

    @Override
    public BurstWriter.HciResult txBurstMessage(byte[] message) {
        if(DEBUG) Log.v(TAG, "txBurstMessage");

        BurstWriter.HciResult hciResult;

        int sendRawPacketResult = mAntm.sendRawPacket(message);

        switch(sendRawPacketResult) {
            case BTPM_ERROR_CODE_ANT_MESSAGE_RESPONSE_ERROR://Wilink ANT packet buffer full
                // Possibly "burst buffer full"
                if(DEBUG) Log.v(TAG, "BTPM_ERROR_CODE_ANT_MESSAGE_RESPONSE_ERROR");
                hciResult = HciResult.RETRY_DELAYED;
                break;
            case BTPM_ERROR_CODE_SUCCESS:
                // Success
                hciResult = HciResult.SUCCESS;
                break;
            case BTPM_ERROR_CODE_ANT_UNABLE_TO_SEND_ANT_MESSAGE:
            default:
                // Error
                Log.e(TAG, "Burst TX fail sendRawPacketResult = "+ sendRawPacketResult);
                hciResult = HciResult.FAIL;
                break;
        }

        return hciResult;
    }

    // TODO This assumes there is no way to get the current state from the SS1 stack. Is there?
    @Override
    public int enable() {
        int devmResult;

        if(DEBUG) Log.v(TAG, "enable");

        if(!mIsLowPowerWirelessModeAnt) {
            // There is no "ANT part" to talk to
            if(DEBUG) Log.d(TAG, "ANT enable request ignored as WiLink is not in ANT mode");

            return AntChipState.CHIPSTATE_DISABLED;
        }

        // Make sure there are no state change events processing
        synchronized(mKnownChipStateChange_LOCK) {
            // If already enabling/enabled we don't need to request again
            if(alreadyEnabling()) {
                Log.d(TAG, "Chip state is already enabling/enabled");

                return getChipState();
            } else {
                // We have started enabling
                updateState(AntChipState.CHIPSTATE_ENABLING);
            }
        }

        try {
            if(mDeviceManager.acquireLock()) {
                devmResult = mDeviceManager.enableLocalDeviceFeature(LocalDeviceFeature.ANT_PLUS);
            } else {
                Log.e(TAG, "Cannot acquire SS1 DEVM lock");
                throw new RuntimeException("Cannot acquire SS1 DEVM lock");
            }
        } finally {
            mDeviceManager.releaseLock();
        }

        if(devmResult < 0) {
            Log.e(TAG, "Enable failed, result code = " + devmResult);

            // New chip state stays at old chip state
            updateState(AntChipState.CHIPSTATE_DISABLED);
        } else {
            // TODO This assumes enableLocalDeviceFeature() is blocking. Is it?
            // We have finished enabling
            updateState(AntChipState.CHIPSTATE_ENABLED);
        }

        return getChipState();
    }

    // TODO This assumes there is no way to get the current state from the SS1 stack. Is there?
    @Override
    public int disable() {
        int devmResult;

        if(DEBUG) Log.v(TAG, "disable");

        if(!mIsLowPowerWirelessModeAnt) {
            // There is no "ANT part" to talk to
            return AntChipState.CHIPSTATE_DISABLED;
        }

        // Make sure there are no state change events processing
        synchronized(mKnownChipStateChange_LOCK) {
            // If already disabling/disabled we don't need to request again
            if(alreadyDisabling()) {
                if(DEBUG) Log.d(TAG, "Chip state is already disabling/disabled");

                return getChipState();
            } else {
                // We have started disabling
                updateState(AntChipState.CHIPSTATE_DISABLING);
            }
        }

        // TODO Is it required we reset to disable, or is disableLocalDeviceFeature() enough?
        mAntm.resetSystem();
        try {
            Thread.sleep(500); // Must wait for chip. This sleep is a nasty hack as I am lazy.
        } catch (InterruptedException e) {
            // Ignore
        }

        try {
            if(mDeviceManager.acquireLock()) {
                devmResult = mDeviceManager.disableLocalDeviceFeature(LocalDeviceFeature.ANT_PLUS);
            } else {
                Log.e(TAG, "Cannot acquire SS1 DEVM lock");
                throw new RuntimeException("Cannot acquire SS1 DEVM lock");
            }
        } finally {
            mDeviceManager.releaseLock();
        }

        if(devmResult < 0) {
            Log.e(TAG, "Disable failed, result code = " + devmResult);

            // New chip state stays at old chip state
            updateState(AntChipState.CHIPSTATE_ENABLED);
        } else {
            // TODO This assumes disableLocalDeviceFeature() is blocking. Is it?
            // We have finished disabling
            updateState(AntChipState.CHIPSTATE_DISABLED);
        }

        return getChipState();
    }

    /**
     * Checks if an enable is already in progress/completed.
     * 
     * @return true, if chip is enabling or enabled.
     */
    private boolean alreadyEnabling() {
        int chipState = getChipState();

        return ((AntChipState.CHIPSTATE_ENABLED == chipState) || 
                (AntChipState.CHIPSTATE_ENABLING == chipState));
    }

    /**
     * Checks if a disable is already in progress/completed.
     * 
     * @return true, if chip is disabling or disabled.
     */
    private boolean alreadyDisabling() {
        int chipState = getChipState();

        return ((AntChipState.CHIPSTATE_DISABLED == chipState) || 
                (AntChipState.CHIPSTATE_DISABLING == chipState));
    }

    /**
     * Gets the current power state of the ANT chip.
     * 
     * @return AntChipState from the chip.
     */
    @Override
    public int getChipState() {
        // TODO Can we query the ANT+ Local Device Feature state from DEVM/ANTM instead?
        return mKnownChipState;
    }

    /**
     * Record a new chip power state and send out an event with this new state.
     * 
     * @param newChipState The new power state.
     */
    private void updateState(int newChipState) {
        // Make sure to only process one state change event/request at a time
        synchronized(mKnownChipStateChange_LOCK) {
            if(newChipState != mKnownChipState) {
                if(DEBUG) Log.d(TAG, "updateState to " + newChipState);

                int oldChipState = mKnownChipState;
                mKnownChipState = newChipState;

                IAntChipEventReceiver eventReceiver = mEventReceiver;
                if(null != eventReceiver) {
                    eventReceiver.stateChanged(oldChipState, newChipState);
                } else {
                    if(DEBUG) Log.w(TAG, "No state changed event sent as no callback set.");
                }
            }
        }
    }

    /*
     * Forwards the given packet (singular) directly to the ANT chip.
     * @param message The raw ANT packet.
     * <p>
     *  The format is<br>
     *  {@code | II JJ ---- |}<br>
     *  {@code | ANT Packet |}<br>
     * <br>
     *  where:<br>
     *  {@code II   is the 1 byte size of the ANT data (0-249)}<br>
     *  {@code JJ   is the 1 byte ID of the ANT message (1-255, 0 is invalid)}<br>
     *  {@code ---- is the data of the ANT message (0-249 bytes of data)}<br>
     * 
     * @return Zero if successful, otherwise this function returns a negative error code.
     */
    private int sendRawPacket(byte[] message) {
        //if(DEBUG) Log.d(TAG, "sendRawPacket");

        //if(SerialLog.LOG_SERIAL) SerialLog.logWrite(message);

        return mAntm.sendRawPacket(message);
    }

    /** ANTM callback handlers */
    private final ANTM.EventCallback mANTEventCallback = new ANTM.EventCallback() {

        @Override
        public void ANTVersionEvent(byte[] arg0) {
            // Ignored, using rawDataPacketEvent(rawData) for all events

        }

        @Override
        public void acknowledgedDataPacketEvent(int arg0, byte[] arg1) {
            // Ignored, using rawDataPacketEvent(rawData) for all events

        }

        @Override
        public void broadcastDataPacketEvent(int arg0, byte[] arg1) {
            // Ignored, using rawDataPacketEvent(rawData) for all events

        }

        @Override
        public void burstDataPacketEvent(int arg0, byte[] arg1) {
            // Ignored, using rawDataPacketEvent(rawData) for all events

        }

        @Override
        public void capabilitiesEvent(int arg0, int arg1,
                EnumSet<ANTCapabilities> arg2) {
            // Ignored, using rawDataPacketEvent(rawData) for all events

        }

        @Override
        public void channelIDEvent(int arg0, int arg1, int arg2, int arg3) {
            // Ignored, using rawDataPacketEvent(rawData) for all events

        }

        @Override
        public void channelResponseEvent(int arg0, int arg1,
                ChannelResponseMessageCode arg2) {
            // Ignored, using rawDataPacketEvent(rawData) for all events

        }

        @Override
        public void channelStatusEvent(int arg0, int arg1) {
            // Ignored, using rawDataPacketEvent(rawData) for all events

        }

        @Override
        public void extendedAcknowledgedDataPacketEvent(int arg0, int arg1,
                int arg2, int arg3, byte[] arg4) {
            // Ignored, using rawDataPacketEvent(rawData) for all events

        }

        @Override
        public void extendedBroadcastDataPacketEvent(int arg0, int arg1,
                int arg2, int arg3, byte[] arg4) {
            // Ignored, using rawDataPacketEvent(rawData) for all events

        }

        @Override
        public void extendedBurstDataPacketEvent(int arg0, int arg1, int arg2,
                int arg3, byte[] arg4) {
            // Ignored, using rawDataPacketEvent(rawData) for all events

        }

        @Override
        public void rawDataPacketEvent(byte[] rawData) {
            //if(DEBUG) Log.v(TAG, "rawDataPacketEvent");

            //if(SerialLog.LOG_SERIAL) SerialLog.logRead(rawData);
            /*
            if(DEBUG) {
                StringBuilder sb = new StringBuilder();
                for(byte b: rawData)
                    sb.append(String.format("%02X", b));
                Log.d(TAG, "Rx: "+ sb);
            }
            */
            IAntChipEventReceiver eventReceiver = mEventReceiver;
            if(null != eventReceiver) {
                eventReceiver.messageReceived(rawData);
            }
        }

        @Override
        public void startupMessageEvent(EnumSet<StartupMessageFlags> arg0) {
            // Ignored, using rawDataPacketEvent(rawData) for all events

        }

    };
    /** DEVm and GATT Callback handlers */
    private final EventCallback mDeviceEventCallback = new EventCallback() {

        // TODO I assume these events are for the whole chip (to match
        // powerOnDevice() and powerOffDevice()) not specific for the ANT_PLUS
        // local device feature. How are we notified when ANT is enabled/disabled?
        @Override
        public void devicePoweredOffEvent() {
            Log.v(TAG, "devicePoweredOffEvent");

            // Make sure we know we are disabled
            if(AntChipState.CHIPSTATE_DISABLED != mKnownChipState) {
                updateState(AntChipState.CHIPSTATE_DISABLED);
            }
        }

        @Override
        public void devicePoweredOnEvent() {
            Log.v(TAG, "devicePoweredOnEvent");

            if(mIsLowPowerWirelessModeAnt) {
                enable();
            }
        }

        @Override
        public void devicePoweringOffEvent(int poweringOffTimeout) {
            Log.v(TAG, "devicePoweringOffEvent");

            if(mIsLowPowerWirelessModeAnt) {
                disable();
            }

            // Cleanup complete
            mDeviceManager.acknowledgeDevicePoweringDown();
        }

        @Override
        public void deviceScanStartedEvent() {
            // Ignored, only using DEVM for local device power control

        }

        @Override
        public void deviceScanStoppedEvent() {
            // Ignored, only using DEVM for local device power control

        }

        @Override
        public void discoveryStartedEvent() {
            // Ignored, only using DEVM for local device power control

        }

        @Override
        public void discoveryStoppedEvent() {
            // Ignored, only using DEVM for local device power control

        }

        @Override
        public void localDevicePropertiesChangedEvent(
                LocalDeviceProperties arg0, EnumSet<LocalPropertyField> arg1) {
            // Ignored, only using DEVM for local device power control

        }

        @Override
        public void remoteDeviceAuthenticationStatusEvent(
                BluetoothAddress arg0, int arg1) {
            // Ignored, only using DEVM for local device power control

        }

        @Override
        public void remoteDeviceConnectionStatusEvent(BluetoothAddress arg0,
                int arg1) {
            // Ignored, only using DEVM for local device power control

        }

        @Override
        public void remoteDeviceDeletedEvent(BluetoothAddress arg0) {
            // Ignored, only using DEVM for local device power control

        }

        @Override
        public void remoteDeviceEncryptionStatusEvent(BluetoothAddress arg0,
                int arg1) {
            // Ignored, only using DEVM for local device power control

        }

        @Override
        public void remoteDeviceFoundEvent(RemoteDeviceProperties arg0) {
            // Ignored, only using DEVM for local device power control

        }

        @Override
        public void remoteDevicePairingStatusEvent(BluetoothAddress arg0,
                boolean arg1, int arg2) {
            // Ignored, only using DEVM for local device power control

        }

        @Override
        public void remoteDevicePropertiesChangedEvent(
                RemoteDeviceProperties arg0, EnumSet<RemotePropertyField> arg1) {
            // Ignored, only using DEVM for local device power control

        }

        @Override
        public void remoteDevicePropertiesStatusEvent(boolean arg0,
                RemoteDeviceProperties arg1) {
            // Ignored, only using DEVM for local device power control

        }

        @Override
        public void remoteDeviceServicesStatusEvent(BluetoothAddress arg0,
                boolean arg1) {
            // Ignored, only using DEVM for local device power control

        }

        @Override
        public void remoteLowEnergyDeviceAddressChangedEvent(
                BluetoothAddress arg0, BluetoothAddress arg1) {
            // Ignored, only using DEVM for local device power control

        }

        @Override
        public void remoteLowEnergyDeviceServicesStatusEvent(
                BluetoothAddress arg0, boolean arg1) {
            // Ignored, only using DEVM for local device power control

        }

        @Override
        public void deviceAdvertisingStarted() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void deviceAdvertisingStopped() {
            // TODO Auto-generated method stub
            
        }
    };

}
