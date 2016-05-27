
package com.reconinstruments.gatt;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.DEVM;
import com.stonestreetone.bluetopiapm.DEVM.AdvertisingDataType;
import com.stonestreetone.bluetopiapm.DEVM.ConnectFlags;
import com.stonestreetone.bluetopiapm.DEVM.EventCallback;
import com.stonestreetone.bluetopiapm.DEVM.LocalDeviceProperties;
import com.stonestreetone.bluetopiapm.DEVM.LocalPropertyField;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceFlags;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceProperties;
import com.stonestreetone.bluetopiapm.DEVM.RemotePropertyField;
import com.stonestreetone.bluetopiapm.GATM;
import com.stonestreetone.bluetopiapm.GATM.AttributeProtocolErrorType;
import com.stonestreetone.bluetopiapm.GATM.CharacteristicDefinition;
import com.stonestreetone.bluetopiapm.GATM.CharacteristicDescriptor;
import com.stonestreetone.bluetopiapm.GATM.ConnectionInformation;
import com.stonestreetone.bluetopiapm.GATM.ConnectionType;
import com.stonestreetone.bluetopiapm.GATM.GenericAttributeClientManager;
import com.stonestreetone.bluetopiapm.GATM.GenericAttributeClientManager.ClientEventCallback;
import com.stonestreetone.bluetopiapm.GATM.RequestErrorType;
import com.stonestreetone.bluetopiapm.GATM.ServiceDefinition;
import com.stonestreetone.bluetopiapm.GATM.ServiceInformation;
import com.stonestreetone.bluetopiapm.GATM.CharacteristicProperty;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HUDGattService extends Service {
    public final static String TAG = "GattTestService";
    static final boolean DBG = true;
    BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    /** A GATT operation completed successfully */
    private static final int GATT_SUCCESS = 0;
    /** GATT read operation is not permitted */
    public static final int GATT_READ_NOT_PERMITTED = 0x2;

    /** GATT write operation is not permitted */
    public static final int GATT_WRITE_NOT_PERMITTED = 0x3;

    /** Insufficient authentication for a given operation */
    public static final int GATT_INSUFFICIENT_AUTHENTICATION = 0x5;

    /** The given request is not supported */
    public static final int GATT_REQUEST_NOT_SUPPORTED = 0x6;

    /** Insufficient encryption for a given operation */
    public static final int GATT_INSUFFICIENT_ENCRYPTION = 0xf;

    /** A read or write operation was requested with an invalid offset */
    public static final int GATT_INVALID_OFFSET = 0x7;

    /** A write operation exceeds the maximum length of the attribute */
    public static final int GATT_INVALID_ATTRIBUTE_LENGTH = 0xd;

    /** Indication that the channel is not encrypted */
    public static final int GATT_NO_ENCRYPTION = 0x100;
    /** A GATT operation failed, errors other than the above */
    public static final int GATT_FAILURE = 0x101;

    /**
     * Max of allowed clients number.
     */
    private static final int MAX_CLIENT_NUM = 50;
    // If 10 second without and client registored, then disable BLE profile
    private static final int PROFILE_DISABLE_TIME = 10000;
    private int mClientID;

    private HUDGattService mService = null;
    private HandlerThread mThread;
    private GattHandler mHandler;
    /**
     * GATT event parameters
     */
    private static final int BLE_SCAN_STOP = 0;
    private static final int BLE_SCANNING = 1;
    private static final int BLE_ADV_DATA = 2;
    private static final int BLE_DISCOVER_SERVICE = 3;
    private static final int BLE_GATT_CONNECT = 4;
    private static final int BLE_GATT_CONNECT_STATE = 5;
    private static final int BLE_GATT_HANDLE_VALUE = 6;
    private static final int BLE_GATT_WRITE_RESPONSE = 7;
    private static final int BLE_GATT_READ_RESPONSE = 8;
    private static final int BLE_READ_RSSI = 9;
    private static final int BLE_PROFILE_ENABLE = 10;
    private static final int BLE_PROFILE_DISABLE = 11;

    private int mScanStatus;

    private int mScanCommand;
    // The desired duration (specified in seconds) of the scan
    private static final int SCAN_DURATION = 5;

    // Recon remote
    static final String remotePrefix = "ReconR";

    // Advertise data type defined by Bluetooth SIG
    public static final int GAP_ADTYPE_FLAGS_LIMITED = 0x01; // !< Discovery
                                                             // Mode: LE Limited
                                                             // Discoverable
                                                             // Mode
    public static final int GAP_ADTYPE_FLAGS_GENERAL = 0x02; // !< Discovery
                                                             // Mode: LE General
                                                             // Discoverable
                                                             // Mode
    public static final int GAP_ADTYPE_FLAGS_BREDR_NOT_SUPPORTED = 0x04; // !<
                                                                         // Discovery
                                                                         // Mode:
                                                                         // BR/EDR
                                                                         // Not
                                                                         // Supported

    private static final byte ADVERTISING_DATA_TYPE_FLAGS = 0x01;
    private static final byte ADVERTISING_DATA_TYPE_16_BIT_SERVICE_UUID_PARTIAL = 0x02;
    private static final byte ADVERTISING_DATA_TYPE_16_BIT_SERVICE_UUID_COMPLETE = 0x03;
    private static final byte ADVERTISING_DATA_TYPE_32_BIT_SERVICE_UUID_PARTIAL = 0x04;
    private static final byte ADVERTISING_DATA_TYPE_32_BIT_SERVICE_UUID_COMPLETE = 0x05;
    private static final byte ADVERTISING_DATA_TYPE_128_BIT_SERVICE_UUID_PARTIAL = 0x06;
    private static final byte ADVERTISING_DATA_TYPE_128_BIT_SERVICE_UUID_COMPLETE = 0x07;
    private static final byte ADVERTISING_DATA_TYPE_LOCAL_NAME_SHORTENED = 0x08;
    private static final byte ADVERTISING_DATA_TYPE_LOCAL_NAME_COMPLETE = 0x09;
    private static final byte ADVERTISING_DATA_TYPE_TX_POWER_LEVEL = 0x0A;
    private static final byte ADVERTISING_DATA_TYPE_CLASS_OF_DEVICE = 0x0D;
    private static final byte ADVERTISING_DATA_TYPE_SIMPLE_PAIRING_HASH_C = 0x0E;
    private static final byte ADVERTISING_DATA_TYPE_SIMPLE_PAIRING_RANDOMIZER_R = 0x0F;
    private static final byte ADVERTISING_DATA_TYPE_SECURITY_MANAGER_TK = 0x10;
    private static final byte ADVERTISING_DATA_TYPE_SECURITY_MANAGER_OUT_OF_BAND_FLAGS = 0x11;
    private static final byte ADVERTISING_DATA_TYPE_SLAVE_CONNECTION_INTERVAL_RANGE = 0x12;
    private static final byte ADVERTISING_DATA_TYPE_LIST_OF_16_BIT_SERVICE_SOLICITATION_UUIDS = 0x14;
    private static final byte ADVERTISING_DATA_TYPE_LIST_OF_128_BIT_SERVICE_SOLICITATION_UUIDS = 0x15;
    private static final byte ADVERTISING_DATA_TYPE_SERVICE_DATA = 0x16;
    private static final byte ADVERTISING_DATA_TYPE_PUBLIC_TARGET_ADDRESS = 0x17;
    private static final byte ADVERTISING_DATA_TYPE_RANDOM_TARGET_ADDRESS = 0x18;
    private static final byte ADVERTISING_DATA_TYPE_APPEARANCE = 0x19;
    private static final byte ADVERTISING_DATA_TYPE_MANUFACTURER_SPECIFIC = (byte) 0xFF;

    /**
     * List of our registered clients.
     */
    class ClientMap extends ContextMap<IBluetoothGattCallback> {
    }

    ClientMap mClientMap = new ClientMap();

    /**
     * List of clients interested in scan results.
     */
    private ScanQueue mScanQueue = new ScanQueue();

    /**
     * List of clients interested in Gatt communications.
     */
    private GattQueue mGattQueue = new GattQueue();
    /**
     * List of transcations for read or write in Gatt communications.
     */
    private List<AttReadWriteID> mRWQueue = new ArrayList<AttReadWriteID>();

    private AttReadWriteID getAttReadWriteID(int transcation) {
        for (AttReadWriteID readwriteid : mRWQueue) {
            if (readwriteid.transactionID == transcation)
                return readwriteid;
        }
        return null;
    }

    private void removeAttReadWriteID(int transcation) {
        for (AttReadWriteID readwriteid : mRWQueue) {
            if (readwriteid.transactionID == transcation)
                mRWQueue.remove(readwriteid);
        }
    }

    /* package */DEVM mDeviceManager;
    /* package */GenericAttributeClientManager mGenericAttributeClientManager;
    DEVMCallbackWrapper mDEVMCallback;
    GATMCallbackWrapper mGATMCallback;

    private void profileEnable() {
        try {
            if (mDeviceManager == null) {
                if (DBG)
                    Log.d(TAG, "profileEnable");
                mDeviceManager = new DEVM(mDEVMCallback);
                if (mGenericAttributeClientManager == null)
                    mGenericAttributeClientManager = new GenericAttributeClientManager(
                            mGATMCallback);
            }
        } catch (Exception e) {
            /*
             * BluetopiaPM server couldn't be contacted. This should never
             * happen if Bluetooth was successfully enabled.
             */
            Log.e(TAG, "Cannot init SS1 manager");
        }
    }

    private void profileDisable() {
        if (DBG)
            Log.d(TAG, "profileDisable");
        if (mGenericAttributeClientManager != null) {
            mGenericAttributeClientManager.dispose();
            mGenericAttributeClientManager = null;
        }
        if (mDeviceManager != null) {
            mDeviceManager.dispose();
            mDeviceManager = null;
        }
    }

    @Override
    public void onCreate() {
        if (DBG)
            Log.d(TAG, "onCreate");
        mThread = new HandlerThread("GattThread");
        mThread.start();
        mHandler = new GattHandler(mThread.getLooper());
        mDEVMCallback = new DEVMCallbackWrapper();
        mGATMCallback = new GATMCallbackWrapper();

        mDeviceManager = null;
        mGenericAttributeClientManager = null;

        mClientID = 0;
        mScanStatus = BLE_SCAN_STOP;
        mScanCommand = BLE_SCAN_STOP;
        // Register a receiver for a Bluetooth "State Changed" broadcast event.
        registerReceiver(bluetoothBroadcastReceiver, new IntentFilter(
                BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    public void onDestroy() {
        if (DBG)
            Log.d(TAG, "onDestroy");

        // TODO: Unregister all callbacks.
        mClientMap.clear();
        mScanQueue.clear();
        mGattQueue.clear();
        mRWQueue.clear();
        profileDisable();
        // TOOD unregister broadcast receivers?
        unregisterReceiver(bluetoothBroadcastReceiver);
    }

    public boolean onUnbind(Intent intent) {
        if (DBG)
            Log.d(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (DBG)
            Log.d(TAG, "onBind");
        // TODO: check if ANT mode
        /*
         * if (SettingsUtil.getBleOrAnt() ==
         * SettingsUtil.LOW_POWER_WIRELESS_MODE_ANT) {
         * Log.d(TAG,"ANT mode, stopping BLE service"); return null; } else {
         * Log.d(TAG,"BLE mode"); }
         */
        if (mDeviceManager == null) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null) {
                if (!bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.enable();
                }
            }
        }
        mService = this;
        return mBinder;
    }

    /**
     * DeathReceipient handlers used to unregister applications that disconnect
     * ungracefully (ie. crash or forced close).
     */

    private class ClientDeathRecipient implements IBinder.DeathRecipient {
        int mAppIf;

        public ClientDeathRecipient(int appIf) {
            mAppIf = appIf;
        }

        public void binderDied() {
            if (DBG)
                Log.d(TAG, "Binder is dead - unregistering client (" + mAppIf + ")!");
            unregisterClient(mAppIf);
        }
    }

    /**
     * The IBluetoothGatt is defined through IDL
     */
    private final IBluetoothGatt.Stub mBinder = new IBluetoothGatt.Stub() {
        public void registerClient(ParcelUuid uuid, IBluetoothGattCallback callback)
                throws RemoteException {
            // TODO
            // enforceCallingOrSelfPermission(BLUETOOTH_PERM,
            // "Need BLUETOOTH permission");

            if (DBG)
                Log.d(TAG, "registerClient() - UUID=" + uuid);
            if (mService == null)
                return;
            mService.registerClient(uuid.getUuid(), callback);
        }

        // Client call its clientid as clientIf from API18
        public void unregisterClient(int clientIf) {
            if (DBG)
                Log.d(TAG, "unregisterClient() - clientIf=" + clientIf);
            if (mService == null)
                return;
            mService.unregisterClient(clientIf);
        }

        public void startScan(int appIf, boolean isServer) {
            if (mService == null)
                return;
            mService.startScan(appIf);
        }

        public void startScanWithUuids(int appIf, boolean isServer, ParcelUuid[] ids) {
            if (mService == null)
                return;
            UUID[] uuids = new UUID[ids.length];
            for (int i = 0; i != ids.length; ++i) {
                uuids[i] = ids[i].getUuid();
            }
            mService.startScanWithUuids(appIf, uuids);
        }

        public void stopScan(int appIf, boolean isServer) {
            if (mService == null)
                return;
            mService.stopScan(appIf);
        }

        public void clientConnect(int clientIf, String address, boolean isDirect) {
            if (mService == null)
                return;
            mService.clientConnect(clientIf, address);
        }

        public void clientDisconnect(int clientIf, String address) {
            if (mService == null)
                return;
            mService.clientDisconnect(clientIf, address);
        }

        public void discoverServices(int clientIf, String address) {
            if (mService == null)
                return;
            mService.discoverServices(clientIf, address);
        }

        public void writeAttValue(int clientIf, String address, int AttHandle, byte[] data,
                boolean response) {
            if (mService == null)
                return;
            mService.writeAttValue(clientIf, address, AttHandle, data, response);
        }

        public void readAttValue(int clientIf, String address, int AttHandle) {
            if (mService == null)
                return;
            mService.readAttValue(clientIf, address, AttHandle);
        }

        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
            // TODO Auto-generated method stub
            if (mService == null)
                return new ArrayList<BluetoothDevice>();
            return mService.getDevicesMatchingConnectionStates(states);
        }

        public void readRemoteRssi(int clientIf, String address) {
            if (mService == null)
                return;
            mService.readRemoteRssi(clientIf, address);
        }

        public int getDeviceType(String address) {
            if (mService == null)
                return 0;
            return mService.getDeviceType(address);
        }

        public void encryptRemoteDevice(int clientIf, String address) {
            if (mService == null)
                return;
            mService.encryptRemoteDevice(clientIf, address);

        }

        public void authenticateRemoteDevice(int clientIf, String address) {
            if (mService == null)
                return;
            mService.authenticateRemoteDevice(clientIf, address);
        }
    };

    // END_INCLUDE(exposing_a_service)

    /**************************************************************************
     * GATT Service functions - CLIENT
     * 
     * @throws RemoteException
     *************************************************************************/
    void registerClient(UUID uuid, IBluetoothGattCallback callback) {

        synchronized (mBinder) {
            int app_id;
            if (mClientMap.mApps.size() == MAX_CLIENT_NUM) {
                Log.e(TAG, "Maximum clients reached!");
                return;
            }
            else if (mClientID < MAX_CLIENT_NUM) {
                mClientID++;// Auto increse the mClientID
                app_id = mClientID;
            } else {// Reach max index
                for (app_id = 1; app_id <= MAX_CLIENT_NUM; app_id++) {
                    if (mClientMap.getById(app_id) == null) {
                        break; // Found empty id availible
                    }
                }
            }
            mClientMap.add(app_id, callback);
            mHandler.sendMessage(mHandler.obtainMessage(BLE_PROFILE_ENABLE, app_id, 0));
        }
    }

    void unregisterClient(int clientIf) {
        synchronized (mBinder) {
            mScanQueue.removeScanClient(clientIf);
            mGattQueue.removeGattClient(clientIf);
            mClientMap.remove(clientIf);
            if (mScanQueue.isEmpty() && mGattQueue.isEmpty()) {
                mClientMap.clear();
                mClientID = 0;

                synchronized (ScanStateChange_Lock) {
                    mScanStatus = BLE_SCAN_STOP;
                    mScanCommand = BLE_SCAN_STOP;
                }
                mHandler.removeMessages(BLE_PROFILE_DISABLE);
                mHandler.sendMessageDelayed(mHandler.obtainMessage(BLE_PROFILE_DISABLE),
                        PROFILE_DISABLE_TIME);
            }
        }
    }

    void gattClientScanSS1(boolean enable) {
        Log.d(TAG, "Scaning - mScanStatus=" + mScanStatus + ",enable=" + enable);
        synchronized (ScanStateChange_Lock) {
            if (enable) {
                mScanCommand = BLE_SCANNING;

                if (mScanStatus == BLE_SCAN_STOP) {
                    mHandler.sendMessage(mHandler.obtainMessage(BLE_SCANNING));
                } else {
                    Log.w(TAG, "Scan already on");
                }
            }
            else {
                mScanCommand = BLE_SCAN_STOP;
                // Do nothing here, the BLE remote may need to enable scan
            }
        }
    }

    void startScan(int appIf) {

        if (DBG)
            Log.d(TAG, "startScan() - queue=" + mScanQueue.mScanQueue.size());

        if (mScanQueue.getScanClient(appIf) == null) {
            if (DBG)
                Log.d(TAG, "startScan() - adding client=" + appIf);
            mScanQueue.add(appIf);
        }

        gattClientScanSS1(true);
    }

    void startScanWithUuids(int appIf, UUID[] uuids) {

        if (DBG)
            Log.d(TAG, "startScanWithUuids() - queue=" + mScanQueue.mScanQueue.size());

        if (mScanQueue.getScanClient(appIf) == null) {
            if (DBG)
                Log.d(TAG, "startScanWithUuids() - adding client=" + appIf);
            mScanQueue.add(appIf, uuids);
        }

        gattClientScanSS1(true);
    }

    void stopScan(int appIf) {
        if (DBG)
            Log.d(TAG, "stopScan() - queue=" + mScanQueue.mScanQueue.size());
        mScanQueue.removeScanClient(appIf);

        if (mScanQueue.isEmpty()) {
            if (DBG)
                Log.d(TAG, "stopScan() - queue empty; stopping scan");
            gattClientScanSS1(false);
            // TODO: Check if BLE remote service still wants to scan
            // deviceManager.s
        }
    }

    void clientConnect(int clientIf, String address) {
        if (mGattQueue.getGattClient(clientIf, address) == null)
            mGattQueue.add(clientIf, address);
        mHandler.sendMessage(mHandler.obtainMessage(BLE_GATT_CONNECT, clientIf, 0, address));
    }

    void clientDisconnect(int clientIf, String address) {
        int result = mDeviceManager.disconnectRemoteLowEnergyDevice(new BluetoothAddress(address),
                true);
        if (result < 0) {
            Log.d(TAG, "Already disconnect? result= " + result);
        }
    }

    void discoverServices(int clientIf, String address) {

        Integer connId = mClientMap.connIdByAddress(clientIf, address);
        if (DBG)
            Log.d(TAG, "discoverServices() - address=" + address + ", connId=" + connId);

        if (connId != null)
            mHandler.sendMessage(mHandler.obtainMessage(BLE_DISCOVER_SERVICE, clientIf, 0, address));
        else
            Log.e(TAG, "discoverServices() - No connection for " + address + "...");
    }

    void writeAttValue(int clientIf, String address, int AttHandle, byte[] data, boolean response) {
        if (DBG)
            Log.d(TAG, "writeAttValue() - address=" + address + "handle=" + AttHandle
                    + " response= " + response);
        Integer connId = mClientMap.connIdByAddress(clientIf, address);
        if (connId == null) {
            Log.e(TAG, "No connection for write " + address + "...");
            return;
        }

        int result;
        BluetoothAddress remoteDeviceAddress = new BluetoothAddress(address);
        if (response) {
            result = mGenericAttributeClientManager
                    .writeValue(remoteDeviceAddress, AttHandle, data);
        } else {
            result = mGenericAttributeClientManager.writeValueWithoutResponse(remoteDeviceAddress,
                    AttHandle, data);
        }

        if (result < 0) {
            Log.e(TAG, "writeAttribute fail result= " + result);
            // TODO: send status to users
        } else {
            if (response) {
                if (DBG)
                    Log.d(TAG, "result=" + result);
                if (getAttReadWriteID(result) != null) {
                    // This should never happened based on SS1 stack, but we
                    // handle the error here in case
                    Log.e(TAG, "duplicated ID= " + result);
                    removeAttReadWriteID(result);
                }
                mRWQueue.add(new AttReadWriteID(result, connId, true));
            }
        }
    }

    void readAttValue(int clientIf, String address, int AttHandle) {
        if (DBG)
            Log.d(TAG, "readAttValue() - address=" + address + " handle=" + AttHandle);
        Integer connId = mClientMap.connIdByAddress(clientIf, address);
        if (connId == null) {
            Log.e(TAG, "No connection for read " + address + "...");
            return;
        }
        BluetoothAddress remoteDeviceAddress = new BluetoothAddress(address);
        int result = mGenericAttributeClientManager.readValue(remoteDeviceAddress, AttHandle, 0,
                true);

        if (result < 0) {
            Log.e(TAG, "readAttValue fail result= " + result);
            // TODO: send status to users
        } else {
            if (DBG)
                Log.d(TAG, "result=" + result);
            if (getAttReadWriteID(result) != null) {
                // This should never happened based on SS1 stack, but we handle
                // the error here in case
                Log.e(TAG, "duplicated ID= " + result);
                removeAttReadWriteID(result);
            }
            mRWQueue.add(new AttReadWriteID(result, connId, false));
        }
    }

    int getDeviceType(String address) {
        int type = 0;
        RemoteDeviceProperties remoteDeviceProperties =
                mDeviceManager.queryRemoteLowEnergyDeviceProperties
                        (new BluetoothAddress(address), false);

        if (remoteDeviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_BR_EDR))
            type = 1;
        if (remoteDeviceProperties.remoteDeviceFlags
                .contains(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY))
        {
            if (type == 0)
                type = 2;
            else
                type = 3; // Dual mode
        }
        if (DBG)
            Log.d(TAG, "getDeviceType() - device=" + address
                    + ", type=" + type);
        return type;
    }

    List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        // enforceCallingOrSelfPermission(BLUETOOTH_PERM,
        // "Need BLUETOOTH permission");

        final int DEVICE_TYPE_BREDR = 0x1;

        Map<BluetoothDevice, Integer> deviceStates = new HashMap<BluetoothDevice,
                Integer>();

        // Add paired LE devices

        Set<BluetoothDevice> bondedDevices = mAdapter.getBondedDevices();
        for (BluetoothDevice device : bondedDevices) {
            if (getDeviceType(device.getAddress()) != DEVICE_TYPE_BREDR) {
                deviceStates.put(device, BluetoothProfile.STATE_DISCONNECTED);
            }
        }

        // Add connected deviceStates

        Set<String> connectedDevices = new HashSet<String>();
        connectedDevices.addAll(mClientMap.getConnectedDevices());

        for (String address : connectedDevices) {
            BluetoothDevice device = mAdapter.getRemoteDevice(address);
            if (device != null) {
                deviceStates.put(device, BluetoothProfile.STATE_CONNECTED);
            }
        }

        // Create matching device sub-set

        List<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();

        for (Map.Entry<BluetoothDevice, Integer> entry : deviceStates.entrySet()) {
            for (int state : states) {
                if (entry.getValue() == state) {
                    deviceList.add(entry.getKey());
                }
            }
        }

        return deviceList;
    }

    void readRemoteRssi(int clientIf, String address) {
        mHandler.sendMessage(mHandler.obtainMessage(BLE_READ_RSSI, clientIf, 0, address));
    }

    void encryptRemoteDevice(int clientIf, String address) {
        int result = mDeviceManager.encryptRemoteLowEnergyDevice(new BluetoothAddress(
                address));
        if (result < 0)
            Log.w(TAG, "encrypt result=" + result);
    }

    void authenticateRemoteDevice(int clientIf, String address) {
        int result = mDeviceManager.authenticateRemoteLowEnergyDevice(new BluetoothAddress(
                address));
        if (result < 0)
            Log.w(TAG, "authenticate result=" + result);
    }

    /**************************************************************************
     * GATT Service functions - CLIENT End
     * 
     * @throws RemoteException
     *************************************************************************/

    private final BroadcastReceiver bluetoothBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    case BluetoothAdapter.STATE_ON:
                        profileEnable();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        profileDisable();
                        break;
                }
            }
        }
    };

    /**
     * Lock for scan state change from SS! callback and scan comand changes.
     */
    private final Object ScanStateChange_Lock = new Object();

    /**
     * Handler to handle messages from SS1 and clients
     */
    private class GattHandler extends Handler {
        public GattHandler(Looper looper) {
            super(looper);
        }

        /**
         * Check paired recon remote (The LED is blinking as blue)
         */
        private boolean isReconPairedRemote(BluetoothAddress deviceAddress,
                Map<AdvertisingDataType, byte[]> result) {
            // Recon remote has fixed advertise data and format
            if (result.size() != 2)
                return false;
            // Check AdvertisingDataType.FLAGS
            if (!result.containsKey(AdvertisingDataType.FLAGS))
                return false;

            byte[] ad_flag = result.get(AdvertisingDataType.FLAGS);
            if (ad_flag.length != 1)
                return false;
            if (ad_flag[0] != (GAP_ADTYPE_FLAGS_LIMITED | GAP_ADTYPE_FLAGS_BREDR_NOT_SUPPORTED))
                return false;

            // Check AdvertisingDataType.MANUFACTURER_SPECIFIC
            if (!result.containsKey(AdvertisingDataType.MANUFACTURER_SPECIFIC))
                return false;

            byte[] b = result.get(AdvertisingDataType.MANUFACTURER_SPECIFIC);
            if (b.length != 6)
                return false;
            if (DBG)
                Log.v(TAG, "compare mac address:" + deviceAddress);
            byte[] address = deviceAddress.toByteArray();
            for (int i = 0; i < 6; i++) {
                if (b[i] != address[5 - i])
                    return false;
            }
            // If the remote BLE meet all the criteria here but still not Recon
            // remote, we may have trouble here. But generally speaking only the
            // customeried BLE remote can happen, and this chance is rare. We
            // discard such remote anyway.
            if (DBG) {
                StringBuilder sb = new StringBuilder(12);
                for (byte b1 : b)
                    sb.append(String.format("%02x", b1 & 0xFF));
                Log.v(TAG, "Recon remote:" + sb.toString());
            }
            return true;
        }

        private byte parseADType(AdvertisingDataType type) {
            byte ad_type;
            switch (type) {
                case FLAGS:
                    ad_type = ADVERTISING_DATA_TYPE_FLAGS;
                    break;
                case SERVICE_CLASS_UUID_16_BIT_PARTIAL:
                    ad_type = ADVERTISING_DATA_TYPE_16_BIT_SERVICE_UUID_PARTIAL;
                    break;
                case SERVICE_CLASS_UUID_16_BIT_COMPLETE:
                    ad_type = ADVERTISING_DATA_TYPE_16_BIT_SERVICE_UUID_COMPLETE;
                    break;
                case SERVICE_CLASS_UUID_32_BIT_PARTIAL:
                    ad_type = ADVERTISING_DATA_TYPE_32_BIT_SERVICE_UUID_PARTIAL;
                    break;
                case SERVICE_CLASS_UUID_32_BIT_COMPLETE:
                    ad_type = ADVERTISING_DATA_TYPE_32_BIT_SERVICE_UUID_COMPLETE;
                    break;
                case SERVICE_CLASS_UUID_128_BIT_PARTIAL:
                    ad_type = ADVERTISING_DATA_TYPE_128_BIT_SERVICE_UUID_PARTIAL;
                    break;
                case SERVICE_CLASS_UUID_128_BIT_COMPLETE:
                    ad_type = ADVERTISING_DATA_TYPE_128_BIT_SERVICE_UUID_COMPLETE;
                    break;
                case LOCAL_NAME_SHORTENED:
                    ad_type = ADVERTISING_DATA_TYPE_LOCAL_NAME_SHORTENED;
                    break;
                case LOCAL_NAME_COMPLETE:
                    ad_type = ADVERTISING_DATA_TYPE_LOCAL_NAME_COMPLETE;
                    break;
                case TX_POWER_LEVEL:
                    ad_type = ADVERTISING_DATA_TYPE_TX_POWER_LEVEL;
                    break;
                case CLASS_OF_DEVICE:
                    ad_type = ADVERTISING_DATA_TYPE_CLASS_OF_DEVICE;
                    break;
                case SIMPLE_PAIRING_HASH_C:
                    ad_type = ADVERTISING_DATA_TYPE_SIMPLE_PAIRING_HASH_C;
                    break;
                case SIMPLE_PAIRING_RANDOMIZER_R:
                    ad_type = ADVERTISING_DATA_TYPE_SIMPLE_PAIRING_RANDOMIZER_R;
                    break;
                case SECURITY_MANAGER_TK:
                    ad_type = ADVERTISING_DATA_TYPE_SECURITY_MANAGER_TK;
                    break;
                case SECURITY_MANAGER_OUT_OF_BAND_FLAGS:
                    ad_type = ADVERTISING_DATA_TYPE_SECURITY_MANAGER_OUT_OF_BAND_FLAGS;
                    break;
                case SLAVE_CONNECTION_INTERVAL_RANGE:
                    ad_type = ADVERTISING_DATA_TYPE_SLAVE_CONNECTION_INTERVAL_RANGE;
                    break;
                case LIST_OF_16_BIT_SERVICE_SOLICITATION_UUIDS:
                    ad_type = ADVERTISING_DATA_TYPE_LIST_OF_16_BIT_SERVICE_SOLICITATION_UUIDS;
                    break;
                case LIST_OF_128_BIT_SERVICE_SOLICITATION_UUIDS:
                    ad_type = ADVERTISING_DATA_TYPE_LIST_OF_128_BIT_SERVICE_SOLICITATION_UUIDS;
                    break;
                case SERVICE_DATA:
                    ad_type = ADVERTISING_DATA_TYPE_SERVICE_DATA;
                    break;
                case PUBLIC_TARGET_ADDRESS:
                    ad_type = ADVERTISING_DATA_TYPE_PUBLIC_TARGET_ADDRESS;
                    break;
                case RANDOM_TARGET_ADDRESS:
                    ad_type = ADVERTISING_DATA_TYPE_RANDOM_TARGET_ADDRESS;
                    break;
                case APPEARANCE:
                    ad_type = ADVERTISING_DATA_TYPE_APPEARANCE;
                    break;
                case MANUFACTURER_SPECIFIC:
                    ad_type = ADVERTISING_DATA_TYPE_MANUFACTURER_SPECIFIC;
                    break;
                default:
                    ad_type = 0;
                    break;

            }
            return ad_type;
        }

        private void sendScanResult(int rssi, BluetoothAddress deviceAddress) {
            Map<AdvertisingDataType, byte[]> result;
            result = mDeviceManager.queryRemoteLowEnergyDeviceAdvertisingData(deviceAddress);
            if (result == null) {
                Log.e(TAG, "No AD data found");
                return;
            }
            // Check if recon paired remote
            if (isReconPairedRemote(deviceAddress, result)) {
                if (DBG)
                    Log.d(TAG, "Recon paired remote");
                return;
            }

            /**
             * Parse result to byte array
             */
            String address = deviceAddress.toString();

            int AD_MAX_LENGTH = 31;// 31 is defined by bluetooth SIG
            byte[] adv_data = new byte[AD_MAX_LENGTH];// by default AD data are
                                                      // filled with 0
            int adv_offset = 0;
            List<UUID> remoteUuids = new ArrayList<UUID>();

            for (AdvertisingDataType type : result.keySet()) {
                byte[] b = result.get(type);
                int len = b.length;
                // Check GAT service UUIDs
                if (type == AdvertisingDataType.SERVICE_CLASS_UUID_16_BIT_PARTIAL
                        || type == AdvertisingDataType.SERVICE_CLASS_UUID_16_BIT_COMPLETE) {
                    int offset = 0;
                    while (len > 1) {
                        int uuid16 = b[offset++];
                        uuid16 += (b[offset++] << 8);
                        len -= 2;
                        remoteUuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                    }
                }

                int check_length = adv_offset + b.length + 2;
                if (check_length > AD_MAX_LENGTH) {
                    Log.e(TAG, "AD data exceed maximum length: " + check_length);
                    break;
                }
                adv_data[adv_offset++] = (byte) (b.length + 1); // AD length
                adv_data[adv_offset++] = parseADType(type); // AD Type
                for (byte bb : b)
                    adv_data[adv_offset++] = bb; // AD payload
            }

            List<Integer> clientIfs = mScanQueue.getScanClientIdbyUuids(remoteUuids);
            if (!clientIfs.isEmpty()) {
                // Send to dedicated client
                for (Integer clientIf : clientIfs) {
                    ClientMap.App app = mClientMap.getById(clientIf);
                    if (app != null) {
                        try {
                            app.callback.onScanResult(address, rssi, adv_data);
                        } catch (RemoteException e) {
                            Log.e(TAG, "Exception: " + e);
                            mClientMap.remove(clientIf);
                            mScanQueue.removeScanClient(clientIf);
                        }
                    }
                }
            }
        }

        void sendConnectstate(int clientIf, String address, boolean state) {
            ClientMap.App app = mClientMap.getById(clientIf);
            if (app != null) {
                int gatt_status = (state == true) ? GATT_SUCCESS : GATT_FAILURE;

                try {
                    app.callback.onClientConnectionState(gatt_status, clientIf, state, address);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        private void gattConnect(int clientIf, String address) {
            // Check connected devices if it's already stored in mClientMap
            Set<String> connectedDevices = new HashSet<String>();
            connectedDevices.addAll(mClientMap.getConnectedDevices());
            for (String connected : connectedDevices) {
                if (connected.equals(address)) {
                    Log.d(TAG, address + " already connected");
                    // Check if it's new client requesting the conneciton
                    if (mClientMap.connIdByAddress(clientIf, address) == null) {
                        Log.d(TAG, "new gatt client: " + clientIf);
                        mClientMap.addConnection(clientIf, clientIf, address);
                    }
                    sendConnectstate(clientIf, address, true);
                    return;
                }
            }

            // Check if it's already connected before service run
            ConnectionInformation[] connectionInformation =
                    mGenericAttributeClientManager.queryConnectedDevices();
            for (ConnectionInformation connectioninfor : connectionInformation) {
                if (connectioninfor.connectionType.equals(ConnectionType.LOW_ENERGY)) {
                    if (connectioninfor.remoteDeviceAddress.toString().equals(address)) {
                        Log.d(TAG, address + " already connected before gattservice");
                        mClientMap.addConnection(clientIf, clientIf, address);
                        sendConnectstate(clientIf, address, true);
                        return;
                    }
                }
            }

            // TODO: We ask for better security at first
            // EnumSet<ConnectFlags> connectionFlags =
            // EnumSet.of(ConnectFlags.AUTHENTICATE,
            // ConnectFlags.ENCRYPT);
            EnumSet<ConnectFlags> connectionFlags = EnumSet.noneOf(ConnectFlags.class);
            int result = mDeviceManager.connectWithRemoteLowEnergyDevice(new BluetoothAddress(
                    address),
                    connectionFlags);
            if (result != 0) {
                Log.e(TAG, "Cannot connect to the remote" + result);
                sendConnectstate(clientIf, address, false);
            }
        }

        private void gattConnectionReport(int status, int connection, String address) {
            if (DBG)
                Log.d(TAG, "connectedEvent=" + connection + " address: " + address);
            boolean state;
            List<Integer> clientIfs = mGattQueue.getGattClientIdbyAddress(address);
            if (!clientIfs.isEmpty()) {
                for (Integer clientIf : clientIfs) {
                    // TODO: connection id?
                    if (connection == 0) {
                        mClientMap.removeConnection(clientIf, address);
                        state = false;
                    }
                    else {
                        mClientMap.addConnection(clientIf, clientIf, address);
                        state = true;
                    }

                    ClientMap.App app = mClientMap.getById(clientIf);
                    if (app != null) {
                        try {
                            app.callback.onClientConnectionState(status, clientIf, state,
                                    address);
                        } catch (RemoteException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        private int getCharProperty(EnumSet<GATM.CharacteristicProperty> properties) {
            if (properties.isEmpty())
                return 0;
            int result = 0;
            for (CharacteristicProperty prop : properties) {
                switch (prop) {
                    case AUTHENTICATED_SIGNED_WRITES:
                        result |= BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE;
                        break;
                    case BROADCAST:
                        result |= BluetoothGattCharacteristic.PROPERTY_BROADCAST;
                        break;
                    case EXTENDED_PROPERTIES:
                        result |= BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS;
                        break;
                    case INDICATE:
                        result |= BluetoothGattCharacteristic.PROPERTY_INDICATE;
                        break;
                    case NOTIFY:
                        result |= BluetoothGattCharacteristic.PROPERTY_NOTIFY;
                        break;
                    case READ:
                        result |= BluetoothGattCharacteristic.PROPERTY_READ;
                        break;
                    case WRITE:
                        result |= BluetoothGattCharacteristic.PROPERTY_WRITE;
                        break;
                    case WRITE_WITHOUT_RESPONSE:
                        result |= BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
                        break;
                    default:
                        Log.e(TAG, "CharacteristicProperty invalid!");
                        break;
                }
                result &= (byte) 0xFF;
            }
            return result;
        }

        private void sendGattDiscoveredService(int clientIf, String address) throws RemoteException {
            if (DBG)
                Log.d(TAG, "handle DISCOVE_SERVICE- address=" + address + ", clientIf=" + clientIf);
            ClientMap.App app = mClientMap.getById(clientIf);
            if (app == null) {
                Log.e(TAG, "No app found");
                return;
            }
            BluetoothAddress bluetoothAddress = new BluetoothAddress(address);
            ServiceDefinition[] services =
                    mGenericAttributeClientManager.queryRemoteDeviceServices(bluetoothAddress);
            if (services == null) {
                int result;
                Log.w(TAG, "No services found");
                result = mGattQueue.setGattClientCmd(clientIf, address,
                        GattQueue.UPDATE_REMOTE_SERVICES);
                if (result == 0) {
                    Log.e(TAG, "No GattClient found");
                    return;
                }
                result = mDeviceManager.updateRemoteLowEnergyDeviceServices(bluetoothAddress);
                if (result < 0) {
                    Log.w(TAG, "services update fail result=" + result);
                    // Check BTPM_ERROR_CODE_DEVICE_DISCOVERY_IN_PROGRESS
                    if (result == -10047) {
                        mHandler.sendMessageDelayed(
                                mHandler.obtainMessage(BLE_DISCOVER_SERVICE, clientIf, 0, address),
                                500);
                    }
                }
                return;
            }

            // main services----
            for (ServiceDefinition service : services) {
                app.callback.onGetService(address, service.serviceDetails.serviceID,
                        new ParcelUuid(service.serviceDetails.uuid),
                        service.serviceDetails.startHandle, service.serviceDetails.endHandle);

                // Included services----
                if (service.includedServices.length > 0) {
                    for (ServiceInformation includedService : service.includedServices) {
                        app.callback.onGetIncludedService(address, includedService.serviceID,
                                new ParcelUuid(includedService.uuid),
                                includedService.startHandle, includedService.endHandle);
                    }
                }

                // Characteristics----
                if (service.characteristics.length > 0) {
                    for (CharacteristicDefinition characteristic : service.characteristics) {
                        int charProps = getCharProperty(characteristic.properties);
                        app.callback.onGetCharacteristic(address, characteristic.handle,
                                new ParcelUuid(characteristic.uuid), charProps);

                        // Descriptors----
                        if (characteristic.descriptors.length > 0) {
                            for (CharacteristicDescriptor descriptor : characteristic.descriptors) {
                                app.callback.onGetDescriptor(address, descriptor.handle,
                                        new ParcelUuid(descriptor.uuid));
                            }
                        }
                    }
                }

            }
            app.callback.onSearchComplete(address, GATT_SUCCESS);
        }

        private void sendNotify(AttributeData attdata) {
            List<Integer> clientIfs = mGattQueue.getGattClientIdbyAddress(attdata.address);
            if (clientIfs.isEmpty()) {
                if (DBG)
                    Log.d(TAG, "no clients for notification");
                return;
            }
            for (Integer clientIf : clientIfs) {
                ClientMap.App app = mClientMap.getById(clientIf);
                if (app != null) {
                    try {
                        app.callback.onNotify(attdata.address, attdata.handle, attdata.values);
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

        private void sendWriteResponse(int clientIf, int status, AttributeData attdata) {
            if (DBG)
                Log.d(TAG, "sendWriteResponse handle= " + attdata.handle +
                        " address: " + attdata.address + " status:" + status + " client:"
                        + clientIf);

            ClientMap.App app = mClientMap.getById(clientIf);
            if (app != null) {
                try {
                    app.callback.onWriteResponse(attdata.address, attdata.handle, status);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        private void sendReadResponse(int clientIf, int status, AttributeData attdata) {
            if (DBG)
                Log.d(TAG, "sendReadResponse handle=" + attdata.handle +
                        " address: " + attdata.address + " status:" + status + " client:"
                        + clientIf);

            ClientMap.App app = mClientMap.getById(clientIf);
            if (app != null) {
                try {
                    app.callback.onReadResponse(attdata.address, attdata.handle, status,
                            attdata.values);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        private void sendRssi(int clientIf, String address) {
            // Note: not all remote support such features
            RemoteDeviceProperties remoteDeviceProperties =
                    mDeviceManager.queryRemoteLowEnergyDeviceProperties
                            (new BluetoothAddress(address), false);
            if (remoteDeviceProperties != null) {
                ClientMap.App app = mClientMap.getById(clientIf);
                if (app != null) {
                    try {
                        app.callback.onReadRemoteRssi(address, remoteDeviceProperties.rssi,
                                GATT_SUCCESS);
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == BLE_PROFILE_ENABLE) {
                int app_id = msg.arg1;
                mHandler.removeMessages(BLE_PROFILE_DISABLE);
                profileEnable();
                ClientMap.App app = mClientMap.getById(app_id);
                if (app != null) {
                    app.linkToDeath(new ClientDeathRecipient(app_id));
                    try {
                        app.callback.onClientRegistered(GATT_SUCCESS, app_id);
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                return;
            }

            if (mDeviceManager == null || mGenericAttributeClientManager == null) {
                Log.e(TAG, "no managers");
                return;
            }

            switch (msg.what) {
                case BLE_SCAN_STOP: // BLE_SCAN_STOP event comes from SS1
                                    // callback
                    synchronized (ScanStateChange_Lock) {
                        if (mScanCommand == BLE_SCANNING) {
                            if (mScanStatus == BLE_SCAN_STOP) {// Restart the
                                                               // scan
                                Log.d(TAG, "Rescan");
                                mHandler.sendMessage(mHandler.obtainMessage(BLE_SCANNING));
                            }
                        }
                    }
                    break;

                case BLE_SCANNING: // BLE_SCANNING event comes from clients
                    int result = mDeviceManager.startLowEnergyDeviceScan(SCAN_DURATION);
                    if (result != 0) {
                        // TODO: Check error code from SS1
                        Log.w(TAG, "Scan may already start result=" + result);
                        if (result == -10047) {
                            // Note: if reach
                            // BTPM_ERROR_CODE_DEVICE_DISCOVERY_IN_PROGRESS
                            // state, all scanning stop
                            // and have to stop scan first.
                            result = mDeviceManager.stopLowEnergyDeviceScan();
                            if (result != 0) {
                                Log.e(TAG, "Stop scan fail=" + result);
                            }
                        }
                    }
                    break;

                case BLE_ADV_DATA:
                    sendScanResult(msg.arg1, (BluetoothAddress) msg.obj);
                    break;
                case BLE_GATT_CONNECT:
                    gattConnect(msg.arg1, (String) msg.obj);
                    break;
                case BLE_GATT_CONNECT_STATE:
                    gattConnectionReport(msg.arg1, msg.arg2, (String) msg.obj);
                    break;
                case BLE_DISCOVER_SERVICE:
                    try {
                        sendGattDiscoveredService(msg.arg1, (String) msg.obj);
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;
                case BLE_GATT_HANDLE_VALUE:
                    sendNotify((AttributeData) msg.obj);
                    break;
                case BLE_GATT_WRITE_RESPONSE:
                    sendWriteResponse(msg.arg1, msg.arg2, (AttributeData) msg.obj);
                    break;
                case BLE_GATT_READ_RESPONSE:
                    sendReadResponse(msg.arg1, msg.arg2, (AttributeData) msg.obj);
                    break;
                case BLE_READ_RSSI:
                    sendRssi(msg.arg1, (String) msg.obj);
                    break;
                case BLE_PROFILE_DISABLE:
                    profileDisable();
                    break;
            }
        }

    }

    /**
     * SS1 DEVM callbacks wrapper
     */
    private class DEVMCallbackWrapper implements EventCallback {

        @Override
        public void deviceAdvertisingStarted() {
            // TODO Auto-generated method stub

        }

        @Override
        public void deviceAdvertisingStopped() {
            // TODO Auto-generated method stub

        }

        @Override
        public void devicePoweredOffEvent() {
            // TODO Auto-generated method stub

        }

        @Override
        public void devicePoweredOnEvent() {
            // TODO Auto-generated method stub

        }

        @Override
        public void devicePoweringOffEvent(int arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void deviceScanStartedEvent() {
            // Scan may be begun by BLE remote service
            // TODO: integrate with BLE remote service
            if (DBG)
                Log.d(TAG, "deviceScanStartedEvent");
            synchronized (ScanStateChange_Lock) {
                mScanStatus = BLE_SCANNING;
            }
        }

        @Override
        public void deviceScanStoppedEvent() {
            if (DBG)
                Log.d(TAG, "deviceScanStoppedEvent");
            synchronized (ScanStateChange_Lock) {
                mScanStatus = BLE_SCAN_STOP;
            }
            mHandler.sendMessage(mHandler.obtainMessage(BLE_SCAN_STOP));
        }

        @Override
        public void discoveryStartedEvent() {
            // TODO Auto-generated method stub

        }

        @Override
        public void discoveryStoppedEvent() {
            // TODO Auto-generated method stub

        }

        @Override
        public void localDevicePropertiesChangedEvent(LocalDeviceProperties arg0,
                EnumSet<LocalPropertyField> arg1) {
            // TODO Auto-generated method stub

        }

        @Override
        public void remoteDeviceAuthenticationStatusEvent(BluetoothAddress remoteDevice, int status) {
            if (status < 0) {
                Log.e(TAG, "Authentication fail status=" + status);
            }
        }

        @Override
        public void remoteDeviceConnectionStatusEvent(BluetoothAddress remoteDevice, int status) {
            if (status < 0) {
                Log.e(TAG, "Conneciton fail status=" + status);
            }
        }

        @Override
        public void remoteDeviceDeletedEvent(BluetoothAddress arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void remoteDeviceEncryptionStatusEvent(BluetoothAddress remoteDevice, int status) {
            if (DBG)
                Log.d(TAG, "EncryptionStatusEvent");
            if (status < 0) {
                Log.e(TAG, "Encryption fail status=" + status);
            } else
                mHandler.sendMessage(mHandler.obtainMessage(BLE_GATT_CONNECT_STATE, GATT_SUCCESS,
                        1, remoteDevice.toString()));
        }

        @Override
        public void remoteDeviceFoundEvent(RemoteDeviceProperties deviceProperties) {
            if (DBG)
                Log.d(TAG, "remoteDeviceFoundEvent");
            // Check if BLE devices
            if (!deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY))
                return;
            // Check if new recon remote
            if (isNewRemote(deviceProperties.deviceName)) {
                if (DBG)
                    Log.d(TAG, "Recon new remote");
                return;
            }
            mHandler.sendMessage(mHandler.obtainMessage(BLE_ADV_DATA,
                    deviceProperties.lowEnergyRSSI, 0, deviceProperties.deviceAddress));
        }

        @Override
        public void remoteDevicePairingStatusEvent(BluetoothAddress arg0, boolean arg1, int arg2) {
            // TODO Auto-generated method stub

        }

        @Override
        public void remoteDevicePropertiesChangedEvent(RemoteDeviceProperties deviceProperties,
                EnumSet<RemotePropertyField> changedFields) {

            if (!deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY))
                return;

            if (changedFields.contains(RemotePropertyField.LE_CONNECTION_STATE)) {
                int connection = 0;
                int status = GATT_SUCCESS;
                String address = deviceProperties.deviceAddress.toString();

                if (!deviceProperties.remoteDeviceFlags
                        .contains(RemoteDeviceFlags.LE_LINK_CURRENTLY_ENCRYPTED)) {
                    if (DBG)
                        Log.d(TAG, "not encrypted");
                    status = GATT_NO_ENCRYPTION;
                }
                if (deviceProperties.remoteDeviceFlags
                        .contains(RemoteDeviceFlags.CURRENTLY_CONNECTED_OVER_LE)) {
                    if (DBG)
                        Log.d(TAG, "CURRENTLY_CONNECTED_OVER_LE");
                    connection = 1;
                }
                else if (DBG)
                    Log.d(TAG, "Device Disconnected Over LE");

                mHandler.sendMessage(mHandler.obtainMessage(BLE_GATT_CONNECT_STATE, status,
                        connection, address));
            }
        }

        @Override
        public void remoteDevicePropertiesStatusEvent(boolean arg0, RemoteDeviceProperties arg1) {
            // TODO Auto-generated method stub

        }

        @Override
        public void remoteDeviceServicesStatusEvent(BluetoothAddress arg0, boolean arg1) {
            // TODO Auto-generated method stub

        }

        @Override
        public void remoteLowEnergyDeviceAddressChangedEvent(BluetoothAddress arg0,
                BluetoothAddress arg1) {
            // TODO Auto-generated method stub

        }

        @Override
        public void remoteLowEnergyDeviceServicesStatusEvent(BluetoothAddress remoteDevice,
                boolean success) {
            if (success == false) {
                Log.e(TAG, "remoteLowEnergyDeviceServicesStatusEvent fail");
                return;
            }
            String address = remoteDevice.toString();
            int clientIf = mGattQueue.checkCmdandClear(address, GattQueue.UPDATE_REMOTE_SERVICES);
            if (clientIf > 0) {
                if (DBG)
                    Log.d(TAG, "found client " + clientIf + "to update remote services");
                mHandler.sendMessage(mHandler.obtainMessage(BLE_DISCOVER_SERVICE, clientIf, 0,
                        address));
            } else
                Log.e(TAG, "No client found for remote services update");
        }

        /**
         * Check new recon remote (The LED is blinking as orange)
         */
        private boolean isNewRemote(String name) {
            return (name != null && name.startsWith(remotePrefix));
        }

    }

    /**
     * SS1 GATM callbacks wrapper
     */
    private class GATMCallbackWrapper implements ClientEventCallback {

        private int getGattStatus(AttributeProtocolErrorType type) {
            int status;
            switch (type) {
                case READ_NOT_PERMITTED:
                    status = GATT_READ_NOT_PERMITTED;
                    break;
                case WRITE_NOT_PERMITTED:
                    status = GATT_WRITE_NOT_PERMITTED;
                    break;
                case INSUFFICIENT_AUTHENTICATION:
                    status = GATT_INSUFFICIENT_AUTHENTICATION;
                    break;
                case REQUEST_NOT_SUPPORTED:
                    status = GATT_REQUEST_NOT_SUPPORTED;
                    break;
                case INSUFFICIENT_ENCRYPTION:
                    status = GATT_INSUFFICIENT_ENCRYPTION;
                    break;
                case INVALID_OFFSET:
                    status = GATT_INVALID_OFFSET;
                    break;
                case INVALID_ATTRIBUTE_VALUE_LENGTH:
                    status = GATT_INVALID_ATTRIBUTE_LENGTH;
                    break;
                default:
                    status = GATT_FAILURE;
                    break;
            }
            return status;
        }

        @Override
        public void connectedEvent(ConnectionType Type, BluetoothAddress remoteDeviceAddress,
                int MTU) {
            // if (!Type.equals(ConnectionType.LOW_ENERGY))
            // return;
            // String address = remoteDeviceAddress.toString();
            // mHandler.sendMessage(mHandler.obtainMessage(BLE_GATT_CONNECT_STATE,
            // 1, 0, address));
        }

        @Override
        public void connectionMTUUpdateEvent(ConnectionType Type, BluetoothAddress arg1, int arg2) {
            // TODO Auto-generated method stub

        }

        @Override
        public void disconnectedEvent(ConnectionType Type, BluetoothAddress remoteDeviceAddress) {
            // if (!Type.equals(ConnectionType.LOW_ENERGY))
            // return;
            // String address = remoteDeviceAddress.toString();
            // mHandler.sendMessage(mHandler.obtainMessage(BLE_GATT_CONNECT_STATE,
            // 0, 0, address));
        }

        @Override
        public void handleValueEvent(ConnectionType Type, BluetoothAddress remoteDeviceAddress,
                boolean handleValueIndication,
                int attributeHandle, byte[] attributeValue) {
            if (!Type.equals(ConnectionType.LOW_ENERGY))
                return;
            String address = remoteDeviceAddress.toString();
            AttributeData AttData = new AttributeData(address, attributeHandle, attributeValue);

            mHandler.sendMessage(mHandler.obtainMessage(BLE_GATT_HANDLE_VALUE, AttData));
        }

        @Override
        public void errorResponseEvent(ConnectionType Type, BluetoothAddress remoteDeviceAddress,
                int transactionID,
                int handle, RequestErrorType requestErrorType,
                AttributeProtocolErrorType attributeProtoccolErrorType) {
            if (DBG)
                Log.d(TAG, "errorResponseEvent id= " + transactionID);
            if (!Type.equals(ConnectionType.LOW_ENERGY))
                return;

            AttReadWriteID id = getAttReadWriteID(transactionID);
            removeAttReadWriteID(transactionID);
            if (id == null) {
                Log.e(TAG, "no client for errorResponseEvent found");
                return;
            }

            String address = remoteDeviceAddress.toString();
            int status = getGattStatus(attributeProtoccolErrorType);
            if (status == GATT_INSUFFICIENT_AUTHENTICATION) {
                Log.w(TAG, "not insufficent authentication");
                int result = mDeviceManager.authenticateRemoteLowEnergyDevice(remoteDeviceAddress);
                if (result < 0) {
                    Log.e(TAG, "authenticate remote fail resuilt=" + result);
                }
            }
            AttributeData AttData = new AttributeData(address, handle);
            if (id.rw) {
                mHandler.sendMessage(mHandler.obtainMessage(BLE_GATT_WRITE_RESPONSE, id.clientid,
                        status, AttData));
            } else {
                mHandler.sendMessage(mHandler.obtainMessage(BLE_GATT_READ_RESPONSE, id.clientid,
                        status, AttData));
            }
        }

        @Override
        public void readResponseEvent(ConnectionType Type, BluetoothAddress remoteDeviceAddress,
                int transactionID,
                int handle, boolean isFinal, byte[] value) {
            if (DBG)
                Log.d(TAG, "readResponseEvent id= " + transactionID);
            if (!Type.equals(ConnectionType.LOW_ENERGY))
                return;

            AttReadWriteID id = getAttReadWriteID(transactionID);
            removeAttReadWriteID(transactionID);
            if (id == null) {
                Log.e(TAG, "no client for writeResponseEvent found");
                return;
            }

            String address = remoteDeviceAddress.toString();
            AttributeData AttData = new AttributeData(address, handle, value);
            mHandler.sendMessage(mHandler.obtainMessage(BLE_GATT_READ_RESPONSE, id.clientid,
                    GATT_SUCCESS, AttData));
        }

        @Override
        public void writeResponseEvent(ConnectionType Type, BluetoothAddress remoteDeviceAddress,
                int transactionID, int handle) {
            if (DBG)
                Log.d(TAG, "writeResponseEvent id= " + transactionID);
            if (!Type.equals(ConnectionType.LOW_ENERGY))
                return;

            AttReadWriteID id = getAttReadWriteID(transactionID);
            removeAttReadWriteID(transactionID);
            if (id == null) {
                Log.e(TAG, "no client for writeResponseEvent found");
                return;
            }
            String address = remoteDeviceAddress.toString();
            AttributeData AttData = new AttributeData(address, handle);
            mHandler.sendMessage(mHandler.obtainMessage(BLE_GATT_WRITE_RESPONSE, id.clientid,
                    GATT_SUCCESS, AttData));
        }

    }
}
