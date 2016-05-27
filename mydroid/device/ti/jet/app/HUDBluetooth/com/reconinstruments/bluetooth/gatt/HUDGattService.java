/*
 * Copyright (C) 2015 Recon Instruments
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reconinstruments.bluetooth.gatt;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.IBluetoothGatt;
import android.bluetooth.IBluetoothGattCallback;
import android.bluetooth.IBluetoothGattServerCallback ;

import android.content.Intent;
import android.content.IntentFilter;

import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;
import java.util.Iterator;

import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.DEVM;
import com.stonestreetone.bluetopiapm.DEVM.AdvertisingDataType;
import com.stonestreetone.bluetopiapm.DEVM.ConnectFlags;
import com.stonestreetone.bluetopiapm.DEVM.EventCallback;
import com.stonestreetone.bluetopiapm.DEVM.LocalDeviceFeature;
import com.stonestreetone.bluetopiapm.DEVM.LocalDeviceProperties;
import com.stonestreetone.bluetopiapm.DEVM.LocalPropertyField;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceFlags;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceProperties;
import com.stonestreetone.bluetopiapm.DEVM.RemotePropertyField;
import com.stonestreetone.bluetopiapm.GATM;
import com.stonestreetone.bluetopiapm.GATM.AttributeProtocolErrorType;
import com.stonestreetone.bluetopiapm.GATM.CharacteristicDefinition;
import com.stonestreetone.bluetopiapm.GATM.CharacteristicDescriptor;
import com.stonestreetone.bluetopiapm.GATM.CharacteristicProperty;
import com.stonestreetone.bluetopiapm.GATM.ConnectionInformation;
import com.stonestreetone.bluetopiapm.GATM.ConnectionType;
import com.stonestreetone.bluetopiapm.GATM.GenericAttributeClientManager;
import com.stonestreetone.bluetopiapm.GATM.GenericAttributeClientManager.ClientEventCallback;
import com.stonestreetone.bluetopiapm.GATM.RequestErrorType;
import com.stonestreetone.bluetopiapm.GATM.ServiceDefinition;
import com.stonestreetone.bluetopiapm.GATM.ServiceInformation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import com.reconinstruments.bluetooth.gatt.ProfileService;
import com.reconinstruments.bluetooth.gatt.ProfileService.IProfileServiceBinder;

/**
 * Provides Bluetooth Gatt profile, as a service in
 * the Bluetooth application.
 * @hide
 */
public class HUDGattService extends ProfileService {
    static final boolean DBG = false;
    private static final String TAG = "HUDGattService";
    BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    /**
     * GATT event parameters
     */
    private static final int BLE_SCAN_STOP = 0;
    private static final int BLE_SCANNING = 1;
    private static final int BLE_ADV_DATA = 2;
    private static final int BLE_DISCOVER_SERVICE = 3;
    private static final int BLE_GATT_CONNECT = 4;
    private static final int BLE_GATT_CONNECT_STATE = 5;
    private static final int BLE_GATT_HANDLE_VALUE= 6;
    private static final int BLE_GATT_WRITE_RESPONSE = 7;
    private static final int BLE_GATT_READ_RESPONSE = 8;
    private static final int BLE_READ_RSSI = 9;
    private static final int BLE_PROFILE_ENABLE = 10;
    private static final int BLE_PROFILE_DISABLE = 11;
    private static final int BLE_CLIENT_UNREGISTER = 12;
    /**
     * Scan parameters
     */
    private int mScanStatus;
    private int mScanCommand;

    /**
     * Lock for scan state change from SS1 callback and scan command changes.
     */
    private final Object ScanStateChange_Lock = new Object();

    /**
     * The desired duration (specified in seconds) of the scan.
     */
    private static final int SCAN_DURATION = 5;

    /**
     * Recon remote prefix string and related.
     */
    private static final String remotePrefix = "ReconR";
    private String mRemoteAddress;
    private boolean mIsRemoteScanning;

    private static final int BLE_DISCONNECT_NUM_RETRIES = 5;
    private static final int BLE_DISCONNECT_WAIT_MS = 500;
    /**
     * Advertise data type defined by Bluetooth SIG.
     */
    private static final int GAP_ADTYPE_FLAGS_LIMITED                                           = 0x01; //!< Discovery Mode: LE Limited Discoverable Mode
    private static final int GAP_ADTYPE_FLAGS_GENERAL                                           = 0x02; //!< Discovery Mode: LE General Discoverable Mode
    private static final int GAP_ADTYPE_FLAGS_BREDR_NOT_SUPPORTED                               = 0x04; //!< Discovery Mode: BR/EDR Not Supported

    private static final byte ADVERTISING_DATA_TYPE_FLAGS                                       = 0x01;
    private static final byte ADVERTISING_DATA_TYPE_16_BIT_SERVICE_UUID_PARTIAL                 = 0x02;
    private static final byte ADVERTISING_DATA_TYPE_16_BIT_SERVICE_UUID_COMPLETE                = 0x03;
    private static final byte ADVERTISING_DATA_TYPE_32_BIT_SERVICE_UUID_PARTIAL                 = 0x04;
    private static final byte ADVERTISING_DATA_TYPE_32_BIT_SERVICE_UUID_COMPLETE                = 0x05;
    private static final byte ADVERTISING_DATA_TYPE_128_BIT_SERVICE_UUID_PARTIAL                = 0x06;
    private static final byte ADVERTISING_DATA_TYPE_128_BIT_SERVICE_UUID_COMPLETE               = 0x07;
    private static final byte ADVERTISING_DATA_TYPE_LOCAL_NAME_SHORTENED                        = 0x08;
    private static final byte ADVERTISING_DATA_TYPE_LOCAL_NAME_COMPLETE                         = 0x09;
    private static final byte ADVERTISING_DATA_TYPE_TX_POWER_LEVEL                              = 0x0A;
    private static final byte ADVERTISING_DATA_TYPE_CLASS_OF_DEVICE                             = 0x0D;
    private static final byte ADVERTISING_DATA_TYPE_SIMPLE_PAIRING_HASH_C                       = 0x0E;
    private static final byte ADVERTISING_DATA_TYPE_SIMPLE_PAIRING_RANDOMIZER_R                 = 0x0F;
    private static final byte ADVERTISING_DATA_TYPE_SECURITY_MANAGER_TK                         = 0x10;
    private static final byte ADVERTISING_DATA_TYPE_SECURITY_MANAGER_OUT_OF_BAND_FLAGS          = 0x11;
    private static final byte ADVERTISING_DATA_TYPE_SLAVE_CONNECTION_INTERVAL_RANGE             = 0x12;
    private static final byte ADVERTISING_DATA_TYPE_LIST_OF_16_BIT_SERVICE_SOLICITATION_UUIDS   = 0x14;
    private static final byte ADVERTISING_DATA_TYPE_LIST_OF_128_BIT_SERVICE_SOLICITATION_UUIDS  = 0x15;
    private static final byte ADVERTISING_DATA_TYPE_SERVICE_DATA                                = 0x16;
    private static final byte ADVERTISING_DATA_TYPE_PUBLIC_TARGET_ADDRESS                       = 0x17;
    private static final byte ADVERTISING_DATA_TYPE_RANDOM_TARGET_ADDRESS                       = 0x18;
    private static final byte ADVERTISING_DATA_TYPE_APPEARANCE                                  = 0x19;
    private static final byte ADVERTISING_DATA_TYPE_MANUFACTURER_SPECIFIC                       = (byte) 0xFF;

    /**
     * SS1 components
     */
    private DEVM mDeviceManager;
    private GenericAttributeClientManager mGenericAttributeClientManager;
    DEVMCallbackWrapper mDEVMCallback;
    GATMCallbackWrapper mGATMCallback;

    private void profileEnable() {
        try {
            if (mDeviceManager == null) {
                Log.d(TAG, "profileEnable");
                mDeviceManager = new DEVM(mDEVMCallback);

                //TODO: Check ANT or BLE mode first
                //Enable BLE feature
                try {
                    if (mDeviceManager.acquireLock()) {
                        int res = mDeviceManager.enableLocalDeviceFeature(LocalDeviceFeature.BLUETOOTH_LOW_ENERGY);
                    } else {
                        throw new RuntimeException("Cannot acquire SS1 DEVM lock");
                    }
                } finally {
                    mDeviceManager.releaseLock();
                }

                if (mGenericAttributeClientManager == null) {
                    mGenericAttributeClientManager = new GenericAttributeClientManager(mGATMCallback);
                }
            }
        } catch (Exception e) {
            /**
             * BluetopiaPM server couldn't be contacted. This should never
             * happen if Bluetooth was successfully enabled.
             */
            Log.e(TAG, "Cannot init SS1 manager: " + e);
        }
    }

    /**
     * The function is called if the service begins to shutdown,
     * or no client registed for more than PROFILE_DISABLE_TIME,
     * so require synchronize.
     * @param isShutdown set ture if we don't want to check connected remote devices;
     * set false if we also want to disconnect all the connected devices.
     */
    private synchronized void profileDisable(boolean isShutdown) {
        Log.d(TAG, "profileDisable");
        if (mGenericAttributeClientManager != null) {
            if (!isShutdown) {
                //in case some BLE devices still connected
                ConnectionInformation[] connectionInformation = mGenericAttributeClientManager.queryConnectedDevices();
                if (connectionInformation.length > 0) {
                    Log.d(TAG, "disconnect BLE devices");
                    for(ConnectionInformation connectionInfo : connectionInformation) {
                        if (DBG)  Log.d(TAG, "find " + connectionInfo.remoteDeviceAddress.toString());
                        if (connectionInfo.connectionType.equals(ConnectionType.LOW_ENERGY)) {
                            int result = mDeviceManager.disconnectRemoteLowEnergyDevice(connectionInfo.remoteDeviceAddress,true);
                            Log.d(TAG, "disconnecting result= " + result);
                        }
                    }
                }
            }
            mGenericAttributeClientManager.dispose();
            mGenericAttributeClientManager = null;
        }

        if (mDeviceManager != null) {
            if (!isShutdown) {
                //Disable BLE feature
                try {
                    if (mDeviceManager.acquireLock()) {
                        int counter = 0;
                        // Retry disabling local feature a few times as we need to wait for the disconnect from above to finish
                        while (counter < BLE_DISCONNECT_NUM_RETRIES) {
                            if (mDeviceManager.disableLocalDeviceFeature(LocalDeviceFeature.BLUETOOTH_LOW_ENERGY) == 0) {
                                break;
                            }
                            try {
                                Thread.sleep(BLE_DISCONNECT_WAIT_MS);
                            } catch (InterruptedException ex) {
                            }
                            counter++;
                        }
                    } else {
                        throw new RuntimeException("Cannot aquire SS1 DEVM lock");
                    }
                } finally {
                    mDeviceManager.releaseLock();
                }
            }
            mDeviceManager.dispose();
            mDeviceManager = null;
        }
    }

    /**
     * Clients control block.
     */
    private static final int MAX_CLIENT_NUM = 50;
    //This is rECON remote dedicated ID
    private static final int RECON_REMOTE_ID = MAX_CLIENT_NUM + 1;
    /**
     * If 10 seconds without any client registered, then disable BLE profile
     */
    private static final int PROFILE_DISABLE_TIME = 10000;

    /**
     * Client Id counter to assign to registered clients.
     */
    private int mClientID;

    private HandlerThread mThread = null;
    private GattHandler mHandler;

    /**
     * List of our registered clients.
     */
    class ClientMap extends ContextMap<IBluetoothGattCallback> {}
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
     * List of transactions for read or write in Gatt communications.
     */
    private List<AttReadWriteID> mRWQueue = new ArrayList<AttReadWriteID>();

    private AttReadWriteID getAttReadWriteID(int transcation) {
        for (AttReadWriteID readWriteId : mRWQueue ) {
            if (readWriteId.transactionID == transcation)
                return readWriteId;
        }
        return null;
    }

    private void removeAttReadWriteID(int transcation) {
        synchronized(mRWQueue) {
            Iterator <AttReadWriteID> iter = mRWQueue.iterator();
            while (iter.hasNext()) {
                AttReadWriteID readWriteId = iter.next();
                if (readWriteId.transactionID == transcation)
                    iter.remove();
            }
        }
    }

    /**
     * Binder init
     */
    protected IProfileServiceBinder initBinder() {
        return new BluetoothGattBinder(this);
    }

    protected boolean start() {
        Log.d(TAG, "start()");
        synchronized(this) {
            if (mThread == null) {
                mDeviceManager = null;
                mGenericAttributeClientManager = null;

                mDEVMCallback = new DEVMCallbackWrapper();
                mGATMCallback = new GATMCallbackWrapper();

                mThread = new HandlerThread("GattThread");
                mThread.start();
                mHandler = new GattHandler(mThread.getLooper());

                mClientID = 0;
                mScanStatus = BLE_SCAN_STOP;
                mScanCommand = BLE_SCAN_STOP;
                //Recon remote special client
                mRemoteAddress = null;
                mIsRemoteScanning = false;
            }
        }
        return true;
    }

    protected boolean stop() {
        Log.d(TAG, "stop()");
        mScanQueue.clear();
        mGattQueue.clear();
        mRWQueue.clear();
        mClientMap.clear();
        //Set true below because it's no need to check connected devices during shutdown
        profileDisable(true);

        return true;
    }

    protected boolean cleanup() {
        if (DBG) Log.d(TAG, "cleanup()");

        return stop();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * DeathReceipient handlers used to unregister applications that
     * disconnect ungracefully. (ie. crash or forced close).
     */
    private class ClientDeathRecipient implements IBinder.DeathRecipient {
        int mAppIf;

        public ClientDeathRecipient(int appIf) {
            mAppIf = appIf;
        }

        public void binderDied() {
            Log.d(TAG, "Binder is dead - unregistering client (" + mAppIf + ")!");
            unregisterClient(mAppIf);
        }
    }

    /**
     * Handlers for incoming service calls.
     */
    private static class BluetoothGattBinder extends IBluetoothGatt.Stub implements IProfileServiceBinder {
        private HUDGattService mService;

        public BluetoothGattBinder(HUDGattService svc) {
            mService = svc;
        }

        public boolean cleanup()  {
            mService = null;
            return true;
        }

        private HUDGattService getService() {
            if (mService != null && mService.isAvailable()) return mService;
            Log.e(TAG, "getService() - Service requested, but not available!");
            return null;
        }

        public void registerClient(ParcelUuid uuid, IBluetoothGattCallback callback) {
            HUDGattService service = getService();
            if (service == null) return;
            service.registerClient(uuid.getUuid(), callback);
        }

        public boolean registerReconRemote(IBluetoothGattCallback callback) {
            HUDGattService service = getService();
            if (service == null) return false;
            return service.registerReconRemote(callback);
        }

        public void unregisterClient(int clientIf) {
            HUDGattService service = getService();
            if (service == null) return;
            service.unregisterClient(clientIf);
        }

        public void startScan(int appIf, boolean isServer) {
            HUDGattService service = getService();
            if (service == null) return;
            service.startScan(appIf);
        }

        public void startScanWithUuids(int appIf, boolean isServer, ParcelUuid[] ids) {
            HUDGattService service = getService();
            if (service == null) return;
            UUID[] uuids = new UUID[ids.length];
            for (int i = 0; i != ids.length; ++i) {
                uuids[i] = ids[i].getUuid();
            }
            service.startScanWithUuids(appIf, uuids);
        }

        public void stopScan(int appIf, boolean isServer) {
            HUDGattService service = getService();
            if (service == null) return;
            service.stopScan(appIf);
        }

        public void clientConnect(int clientIf, String address, boolean isDirect) {
            HUDGattService service = getService();
            if (service == null) return;
            service.clientConnect(clientIf, address);
        }

        public void clientDisconnect(int clientIf, String address) {
            HUDGattService service = getService();
            if (service == null) return;
            service.clientDisconnect(clientIf, address);
        }

        public void discoverServices(int clientIf, String address) {
            HUDGattService service = getService();
            if (service == null) return;
            service.discoverServices(clientIf, address);
        }

       public void writeAttributeValue(int clientIf, String address, int attHandle, byte[] data, boolean response) {
            HUDGattService service = getService();
            if (service == null) return;
            mService.writeAttributeValue(clientIf, address, attHandle, data,response);
        }

        public void readAttributeValue(int clientIf, String address, int attHandle) {
            HUDGattService service = getService();
            if (service == null) return;
            mService.readAttributeValue(clientIf, address, attHandle);
        }

        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
            HUDGattService service = getService();
            if (service == null) return new ArrayList<BluetoothDevice>();
            return service.getDevicesMatchingConnectionStates(states);
        }

        public void readRemoteRssi(int clientIf, String address) {
            HUDGattService service = getService();
            if (service == null) return;
            service.readRemoteRssi(clientIf, address);
        }

        public int getDeviceType(String address) {
            HUDGattService service = getService();
            if (service == null) return 0;
            return mService.getDeviceType(address);
        }

        public void encryptRemoteDevice(int clientIf, String address) {
            HUDGattService service = getService();
            if (service == null) return;
            mService.encryptRemoteDevice(clientIf, address);
        }

        public void authenticateRemoteDevice(int clientIf, String address) {
            HUDGattService service = getService();
            if (service == null) return;
            mService.authenticateRemoteDevice(clientIf, address);
        }
        /**-------------------------------------------
         * Gatt Server, not support yet
         */
        public void registerServer(ParcelUuid uuid, IBluetoothGattServerCallback callback) {

        }

        public void unregisterServer(int serverIf) {

        }

        public void serverConnect(int serverIf, String address, boolean isDirect) {

        }

        public void serverDisconnect(int serverIf, String address) {

        }

        public void beginServiceDeclaration(int serverIf, int srvcType,
                                            int srvcInstanceId, int minHandles,
                                            ParcelUuid srvcId) {

        }

        public void addIncludedService(int serverIf, int srvcType,
                            int srvcInstanceId, ParcelUuid srvcId) {

        }

        public void addCharacteristic(int serverIf, ParcelUuid charId,
                            int properties, int permissions) {

        }

        public void addDescriptor(int serverIf, ParcelUuid descId,
                           int permissions) {

        }

        public void endServiceDeclaration(int serverIf) {

        }

        public void removeService(int serverIf, int srvcType,
                           int srvcInstanceId, ParcelUuid srvcId) {

        }

        public void clearServices(int serverIf) {

        }

        public void sendResponse(int serverIf, String address, int requestId,
                                 int status, int offset, byte[] value) {

        }

        public void sendNotification(int serverIf, String address, int srvcType,
                                              int srvcInstanceId, ParcelUuid srvcId,
                                              int charInstanceId, ParcelUuid charId,
                                              boolean confirm, byte[] value) {

        }
    };

    /**************************************************************************
     * GATT Service functions - CLIENT
     *************************************************************************/
    private void registerClient(UUID uuid, IBluetoothGattCallback callback) {
        enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");

        synchronized(this) {
            int app_id;
            if (mClientMap.mApps.size() == MAX_CLIENT_NUM) {
                Log.e(TAG, "Maximum number of clients reached!");
                return;
            }

            if (mClientID < MAX_CLIENT_NUM) {
                mClientID++; // Auto increase the mClientID
                app_id = mClientID;
            } else { // Reach max index
                for (app_id = 1; app_id < MAX_CLIENT_NUM; app_id++) {
                    if (mClientMap.getById(app_id) == null) {
                        break; // Found empty id available
                    }
                }
            }

            if (DBG) Log.d(TAG, "registerClient() - UUID=" + uuid + " app_id=" + app_id);
            else Log.d(TAG, "registerClient");
            mClientMap.add(app_id, callback);
            mHandler.sendMessage(mHandler.obtainMessage(BLE_PROFILE_ENABLE, app_id, 0));
        }
    }

    private boolean registerReconRemote(IBluetoothGattCallback callback)  {
        synchronized(this) {
            if (mThread == null)
                return false;
            int app_id = RECON_REMOTE_ID;
            if (mClientMap.getById(app_id) == null) {
                Log.d(TAG, "registerReconRemote");
            } else {
                Log.w(TAG, "Only allow one remote client");
                //in case the RemoteService crash
                mScanQueue.removeScanClient(app_id);
                mGattQueue.removeGattClient(app_id);
                mClientMap.remove(app_id);
            }
            mClientMap.add(app_id, callback);
            mHandler.sendMessage(mHandler.obtainMessage(BLE_PROFILE_ENABLE, app_id, 0));
        }
        return true;
    }

    private void unregisterClient(int clientIf) {
        mHandler.sendMessage(mHandler.obtainMessage(BLE_CLIENT_UNREGISTER, clientIf, 0));
    }

    private void gattClientScanSS1(boolean enable) {
        Log.d(TAG, "Scaning - mScanStatus= " + mScanStatus + ",enable=" + enable);
        synchronized(ScanStateChange_Lock) {
            if (enable) {
                mScanCommand = BLE_SCANNING;

                if (mScanStatus == BLE_SCAN_STOP) {
                    if (DBG) Log.d(TAG, "Send scan event");
                    mHandler.sendMessage(mHandler.obtainMessage(BLE_SCANNING));
                }
            } else {
                mScanCommand = BLE_SCAN_STOP;
                //Do nothing here, the BLE remote may need to enable scan
            }
        }
    }

    private void startScan(int appIf) {
        if (DBG) Log.d(TAG, "startScan() - queue=" + mScanQueue.mScanQueue.size());
        //Check Recon remote
        synchronized(ScanStateChange_Lock) {
            if (appIf == RECON_REMOTE_ID) mIsRemoteScanning = true;
        }

        if (mScanQueue.getScanClient(appIf) == null) {
            if (DBG) Log.d(TAG, "startScan() - adding client=" + appIf);
            mScanQueue.add(appIf);
        }

        gattClientScanSS1(true);
    }

    private void startScanWithUuids(int appIf, UUID[] uuids) {
        if (DBG) Log.d(TAG, "startScanWithUuids() - queue=" + mScanQueue.mScanQueue.size());

        if (mScanQueue.getScanClient(appIf) == null) {
            if (DBG) Log.d(TAG, "startScanWithUuids() - adding client=" + appIf);
            mScanQueue.add(appIf, uuids);
        }

        gattClientScanSS1(true);
    }

    private void stopScan(int appIf) {
        if (DBG) Log.d(TAG, "stopScan() - queue=" + mScanQueue.mScanQueue.size());
        mScanQueue.removeScanClient(appIf);
        //Check Recon remote
        synchronized(ScanStateChange_Lock) {
            if (appIf == RECON_REMOTE_ID) mIsRemoteScanning = false;
        }

        if (mScanQueue.isEmpty()) {
            if (DBG) Log.d(TAG, "stopScan() - queue empty; stopping scan");
            gattClientScanSS1(false);
            //TODO: Check if BLE remote service still wants to scan
        }
    }

    private void clientConnect(int clientIf, String address) {
        if (mGattQueue.getGattClient(clientIf, address) == null)
            mGattQueue.add(clientIf, address);
        mHandler.sendMessage(mHandler.obtainMessage(BLE_GATT_CONNECT, clientIf, 0, address));
    }

    private void clientDisconnect(int clientIf, String address) {
        int result = mDeviceManager.disconnectRemoteLowEnergyDevice(new BluetoothAddress(address),true);
        if (result < 0) {
            Log.d(TAG, "Already disconnected? result= " + result);
        }
    }

    private void discoverServices(int clientIf, String address) {
        Integer connId = mClientMap.connIdByAddress(clientIf, address);
        if (DBG) Log.d(TAG, "discoverServices() - address=" + address + ", connId=" + connId);

        if (connId != null)
            mHandler.sendMessage(mHandler.obtainMessage(BLE_DISCOVER_SERVICE,clientIf,0,address));
        else
            Log.e(TAG, "discoverServices() - No connection for " + address + "...");
    }

    private void writeAttributeValue(int clientIf, String address, int attHandle, byte[] data, boolean response) {
        if (DBG) Log.d(TAG, "writeAttributeValue() - address= " + address + "handle=" + attHandle + " response= " + response);
        Integer connId = mClientMap.connIdByAddress(clientIf, address);
        if (connId == null) {
            Log.e(TAG, "No connection for write " + address + "...");
            return;
        }

        int result;
        BluetoothAddress remoteDeviceAddress = new BluetoothAddress(address);
        if (response) {
            result = mGenericAttributeClientManager.writeValue(remoteDeviceAddress, attHandle, data);
        } else {
            result = mGenericAttributeClientManager.writeValueWithoutResponse(remoteDeviceAddress, attHandle, data);
        }

        if (result < 0 ) {
            Log.e(TAG, "writeAttribute fail result= " + result);
            //TODO: send status to users
        } else {
            if (DBG) Log.d(TAG, "result=" +result);
            if (response) {
                if (getAttReadWriteID(result) != null) {
                    // This should never happened based on SS1 stack, but we handle the error here in case
                    Log.e(TAG, "duplicated ID= " + result);
                    removeAttReadWriteID(result);
                }
                synchronized(mRWQueue) {
                    mRWQueue.add(new AttReadWriteID(result, connId, true));
                }
            }
        }
    }

    private void readAttributeValue(int clientIf, String address, int attHandle) {
        if (DBG) Log.d(TAG, "readAttributeValue() - address=" + address + " handle=" + attHandle);
        Integer connId = mClientMap.connIdByAddress(clientIf, address);
        if (connId == null) {
            Log.e(TAG, "No connection for read " + address + "...");
            return;
        }

        BluetoothAddress remoteDeviceAddress = new BluetoothAddress(address);
        int result = mGenericAttributeClientManager.readValue(remoteDeviceAddress, attHandle, 0, true);

        if (result < 0) {
            Log.e(TAG, "readAttributeValue fail result= " + result);
            //TODO: send status to users
        } else {
            if (DBG) Log.d(TAG, "result=" +result);
            if (getAttReadWriteID(result) != null) {
                //This should never happened based on SS1 stack, but we handle the error here in case
                Log.e(TAG, "duplicated ID= " + result);
                removeAttReadWriteID(result);
            }
            synchronized(mRWQueue) {
                mRWQueue.add(new AttReadWriteID(result, connId, false));
            }
        }
    }

    private int getDeviceType(String address) {
        int type = BluetoothDevice.DEVICE_TYPE_UNKNOWN;
        RemoteDeviceProperties remoteDeviceProperties =
                mDeviceManager.queryRemoteLowEnergyDeviceProperties
                        (new BluetoothAddress(address), false);

        if (remoteDeviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_BR_EDR)) {
            type = BluetoothDevice.DEVICE_TYPE_CLASSIC;
        }

        if (remoteDeviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY)) {
            if (type == BluetoothDevice.DEVICE_TYPE_UNKNOWN)
                type = BluetoothDevice.DEVICE_TYPE_LE;
            else
                type = BluetoothDevice.DEVICE_TYPE_DUAL; // Dual mode
        }

        if (DBG)
            Log.d(TAG, "getDeviceType() - device=" + address
                    + ", type=" + type);
        return type;
    }

    private List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");

        Map<BluetoothDevice, Integer> deviceStates = new HashMap<BluetoothDevice, Integer>();

        // Add paired LE devices
        Set<BluetoothDevice> bondedDevices = mAdapter.getBondedDevices();
        for (BluetoothDevice device : bondedDevices) {
            if (getDeviceType(device.getAddress()) != BluetoothDevice.DEVICE_TYPE_CLASSIC) {
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

    private void readRemoteRssi(int clientIf, String address) {
        mHandler.sendMessage(mHandler.obtainMessage(BLE_READ_RSSI, clientIf, 0, address));
    }

    private void encryptRemoteDevice(int clientIf, String address) {
        int result = mDeviceManager.encryptRemoteLowEnergyDevice(new BluetoothAddress(
                address));
        if (result <0) Log.w(TAG,"encrypt result=" + result);
    }

    private void authenticateRemoteDevice(int clientIf, String address) {
        int result = mDeviceManager.authenticateRemoteLowEnergyDevice(new BluetoothAddress(
                address));
        if (result <0) Log.w(TAG,"authenticate result=" + result);
    }

    /**************************************************************************
     * GATT Service functions - CLIENT End
     *************************************************************************/

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

        private void reportFoundReconRemote(String address, int rssi) {
            if (DBG)
                Log.d(TAG, "reportFoundReconRemote");
            if (mScanQueue.getScanClient(RECON_REMOTE_ID) != null) {
                ClientMap.App app = mClientMap.getById(RECON_REMOTE_ID);
                if (app != null) {
                    try {
                        byte[] dummy = {0};
                        app.callback.onScanResult(address, rssi, dummy);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Exception: " + e);
                        mClientMap.remove(RECON_REMOTE_ID);
                        mScanQueue.removeScanClient(RECON_REMOTE_ID);
                    }
                }
            }
        }

        /**
         * Send Scan result to the clients
         * @param rssi remote device rssi value
         * @param report 1 indicates to report to special recon remote client and 0 for normal clients report
         * @param deviceAddress remote BluetoothAddress object
         */
        private void sendScanResult(int rssi, int report, BluetoothAddress deviceAddress) {
            String address = deviceAddress.toString();
            if (report == 1) {
                reportFoundReconRemote(address, rssi);
                return;
            }

            Map<AdvertisingDataType, byte[]> result;
            result = mDeviceManager.queryRemoteLowEnergyDeviceAdvertisingData(deviceAddress);
            if (result == null) {
                Log.w(TAG, "No AD data found");
                return;
            }
            // Check if recon paired remote
            if (isReconPairedRemote(deviceAddress, result)) {
                if (DBG)
                    Log.d(TAG, "Recon paired remote");
                return;
            }

            List<Integer> clientIfs = mScanQueue.getScanClientIds();
            if (clientIfs.isEmpty()) {
                Log.w(TAG, "Empty ScanClient");
                return;
            }
            if (clientIfs.size()==1 && clientIfs.get(0)==RECON_REMOTE_ID) {
                if (DBG)
                    Log.d(TAG, "no normal ScanClients here");
                return;
            }
            /**
             * Parse result to byte array
             */
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

            clientIfs = mScanQueue.getScanClientIdbyUuids(remoteUuids);
            if (!clientIfs.isEmpty()) {
                if (DBG)
                    Log.d(TAG, "send Scan result");
                // Send to dedicated client
                for (Integer clientIf : clientIfs) {
                    if (clientIf == RECON_REMOTE_ID)
                        continue;
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

        private void sendConnectState(int clientIf, String address, boolean state, int gatt_status) {
            ClientMap.App app = mClientMap.getById(clientIf);
            if (app != null) {
                try {
                    app.callback.onClientConnectionState(gatt_status, clientIf, state, address);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        private void gattConnect(int clientIf, String address) {
            if (clientIf == RECON_REMOTE_ID)
                mRemoteAddress = address;

            // Check if it's already connected
            ConnectionInformation[] connectionInformation =
                    mGenericAttributeClientManager.queryConnectedDevices();
            for (ConnectionInformation connectionInfo : connectionInformation) {
                if (connectionInfo.connectionType.equals(ConnectionType.LOW_ENERGY)) {
                    if (connectionInfo.remoteDeviceAddress.toString().equals(address)) {
                        Log.d(TAG, address + " already connected");
                        mClientMap.addConnection(clientIf, clientIf, address);
                        sendConnectState(clientIf, address, true, BluetoothGatt.GATT_SUCCESS);
                        return;
                    }
                }
            }

            // No security at first, client should ask for encryption and authentication
            EnumSet<ConnectFlags> connectionFlags = EnumSet.noneOf(ConnectFlags.class);
            int result = mDeviceManager.connectWithRemoteLowEnergyDevice(new BluetoothAddress(
                    address),
                    connectionFlags);
            if (result != 0) {
                Log.e(TAG, "Cannot connect to the remote" + result);
                sendConnectState(clientIf, address, false, BluetoothGatt.GATT_FAILURE);
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
                    } else {
                        mClientMap.addConnection(clientIf, clientIf, address);
                        state = true;
                    }
                    sendConnectState(clientIf, address, state, status);
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
                result = mGattQueue.setGattClientCmd(clientIf, address, GattQueue.UPDATE_REMOTE_SERVICES);
                if (result == 0) {
                    Log.e(TAG, "No GattClient found");
                    return;
                }
                result = mDeviceManager.updateRemoteLowEnergyDeviceServices(bluetoothAddress);
                if (result < 0) {
                    Log.w(TAG, "services update fail result="+result);
                    if (result == -10047) { //Check BTPM_ERROR_CODE_DEVICE_DISCOVERY_IN_PROGRESS
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(BLE_DISCOVER_SERVICE, clientIf, 0, address), 500);
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
            app.callback.onSearchComplete(address, BluetoothGatt.GATT_SUCCESS);
        }

        private void sendNotify(AttributeData attData) {
            List<Integer> clientIfs = mGattQueue.getGattClientIdbyAddress(attData.address);
            if (clientIfs.isEmpty()) {
                if (DBG)
                    Log.d(TAG, "no clients for notification");
                return;
            }
            for (Integer clientIf : clientIfs) {
                ClientMap.App app = mClientMap.getById(clientIf);
                if (app != null) {
                    try {
                        app.callback.onNotify(attData.address, attData.attHandle, attData.values);
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

        private void sendWriteResponse(int clientIf, int status, AttributeData attData) {
            if (DBG)
                Log.d(TAG, "sendWriteResponse handle= " + attData.attHandle +
                        " address: " + attData.address + " status:" + status + " client:"
                        + clientIf);

            ClientMap.App app = mClientMap.getById(clientIf);
            if (app != null) {
                try {
                    app.callback.onWriteResponse(attData.address, attData.attHandle, status);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        private void sendReadResponse(int clientIf, int status, AttributeData attData) {
            if (DBG)
                Log.d(TAG, "sendReadResponse handle=" + attData.attHandle +
                        " address: " + attData.address + " status:" + status + " client:"
                        + clientIf);

            ClientMap.App app = mClientMap.getById(clientIf);
            if (app != null) {
                try {
                    app.callback.onReadResponse(attData.address, attData.attHandle, status,
                            attData.values);
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
                                BluetoothGatt.GATT_SUCCESS);
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

        private void unregister(int clientIf) {
            if (DBG) Log.d(TAG, "unregister app_id=" + clientIf);
            else Log.d(TAG, "unregister");
            mScanQueue.removeScanClient(clientIf);
            mGattQueue.removeGattClient(clientIf);
            mClientMap.remove(clientIf);
            //Check special client
            if (clientIf == RECON_REMOTE_ID) {
                synchronized (ScanStateChange_Lock) {
                    mIsRemoteScanning = false;
                    if (mRemoteAddress != null)
                        clientDisconnect(RECON_REMOTE_ID, mRemoteAddress);
                    mRemoteAddress = null;

                    if (mScanQueue.isEmpty()) {
                        mScanStatus = BLE_SCAN_STOP;
                        mScanCommand = BLE_SCAN_STOP;
                    }
                }
            }
            // If no more clients performing a scan or connect, then clear the client map
            if (mScanQueue.isEmpty() && mGattQueue.isEmpty()) {
                synchronized (ScanStateChange_Lock) {
                    mScanStatus = BLE_SCAN_STOP;
                    mScanCommand = BLE_SCAN_STOP;
                }

                mClientMap.clear();
                mClientID = 0;

                mHandler.removeMessages(BLE_PROFILE_DISABLE);
                mHandler.sendMessageDelayed(mHandler.obtainMessage(BLE_PROFILE_DISABLE),
                        PROFILE_DISABLE_TIME);
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
                        app.callback.onClientRegistered(BluetoothGatt.GATT_SUCCESS, app_id);
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
                            if (mScanStatus == BLE_SCAN_STOP) {// Restart the scan
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
                    sendScanResult(msg.arg1, msg.arg2, (BluetoothAddress) msg.obj);
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
                        Log.e(TAG, "sendGattDiscoveredService fail");
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
                    profileDisable(false);
                    break;
                case BLE_CLIENT_UNREGISTER:
                    unregister(msg.arg1);
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
            if (DBG) Log.d(TAG, "deviceScanStartedEvent");
            synchronized (ScanStateChange_Lock) {
                mScanStatus = BLE_SCANNING;
            }
        }

        @Override
        public void deviceScanStoppedEvent() {
            if (DBG) Log.d(TAG, "deviceScanStoppedEvent");
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
                mHandler.sendMessage(mHandler.obtainMessage(BLE_GATT_CONNECT_STATE, BluetoothGatt.GATT_FAILURE, 0, remoteDevice.toString()));
            }
        }

        @Override
        public void remoteDeviceDeletedEvent(BluetoothAddress arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void remoteDeviceEncryptionStatusEvent(BluetoothAddress remoteDevice, int status) {
            if (DBG) Log.d(TAG, "EncryptionStatusEvent");
            if (status < 0) {
                Log.e(TAG, "Encryption fail status=" + status);
            } else
                mHandler.sendMessage(mHandler.obtainMessage(BLE_GATT_CONNECT_STATE,
                    BluetoothGatt.GATT_SUCCESS, 1, remoteDevice.toString()));
        }

        @Override
        public void remoteDeviceFoundEvent(RemoteDeviceProperties deviceProperties) {
            // Check if BLE devices
            if (!deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY))
                return;
            if (DBG)
                Log.d(TAG, "remoteDeviceFoundEvent:");
            int reportReconRemote = 0;

            synchronized (ScanStateChange_Lock) {
                //Check if previous connected recon remote
                if (mIsRemoteScanning && (mRemoteAddress != null)
                    && (mRemoteAddress.equals(deviceProperties.deviceAddress.toString())) )
                {
                    if (DBG)
                        Log.d(TAG, "Recon  pre-connected remote");
                    reportReconRemote = 1;
                }
                // Check if new recon remote
                if (reportReconRemote==0 && isNewRemote(deviceProperties.deviceName)) {
                    if (DBG)
                        Log.d(TAG, "Recon new remote");
                    if (mIsRemoteScanning)
                        reportReconRemote=1;
                    else
                        return;
                }
            }

            if (DBG)
                Log.d(TAG, "Report=" + reportReconRemote + " Address: " +deviceProperties.deviceAddress);
            mHandler.sendMessage(mHandler.obtainMessage(BLE_ADV_DATA,
                    deviceProperties.lowEnergyRSSI, reportReconRemote, deviceProperties.deviceAddress));
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
                int status = BluetoothGatt.GATT_SUCCESS;
                String address = deviceProperties.deviceAddress.toString();

                if (deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.CURRENTLY_CONNECTED_OVER_LE)) {
                    if (DBG) Log.d(TAG, "Connected Over LE");
                    connection = 1;
                } else {
                    // connection being 0 is going to remove the connection from the mClientMap
                    if (DBG) Log.d(TAG, "Disconnected Over LE");
                }

                if (!deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.LE_LINK_CURRENTLY_ENCRYPTED)) {
                    if (DBG) Log.d(TAG, "But not encrypted");
                    status = BluetoothGatt.GATT_NO_ENCRYPTION;
                }
                mHandler.sendMessage(mHandler.obtainMessage(BLE_GATT_CONNECT_STATE, status, connection, address));
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
        public void remoteLowEnergyDeviceServicesStatusEvent(BluetoothAddress remoteDevice, boolean success) {
            if (success == false) {
                Log.e(TAG, "remoteLowEnergyDeviceServicesStatusEvent fail");
                return;
            }
            String address = remoteDevice.toString();
            int clientIf = mGattQueue.checkCmdandClear(address, GattQueue.UPDATE_REMOTE_SERVICES);
            if (clientIf > 0) {
                if (DBG) Log.d(TAG, "found client " + clientIf +" to update remote services");
                mHandler.sendMessage(mHandler.obtainMessage(BLE_DISCOVER_SERVICE, clientIf, 0, address));
            } else Log.e(TAG, "No client found for remote services update");
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
            if (type == null)
                return BluetoothGatt.GATT_FAILURE;
            switch (type) {
                case READ_NOT_PERMITTED:
                    status = BluetoothGatt.GATT_READ_NOT_PERMITTED;
                    break;
                case WRITE_NOT_PERMITTED:
                    status = BluetoothGatt.GATT_WRITE_NOT_PERMITTED;
                    break;
                case INSUFFICIENT_AUTHENTICATION:
                    status = BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION;
                    break;
                case REQUEST_NOT_SUPPORTED:
                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
                    break;
                case INSUFFICIENT_ENCRYPTION:
                    status = BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION;
                    break;
                case INVALID_OFFSET:
                    status = BluetoothGatt.GATT_INVALID_OFFSET;
                    break;
                case INVALID_ATTRIBUTE_VALUE_LENGTH:
                    status = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;
                    break;
                default:
                    status = BluetoothGatt.GATT_FAILURE;
                    break;
            }
            return status;
        }

        @Override
        public void connectedEvent(ConnectionType Type, BluetoothAddress remoteDeviceAddress,
                int MTU) {

        }

        @Override
        public void connectionMTUUpdateEvent(ConnectionType Type, BluetoothAddress arg1, int arg2) {
            // TODO Auto-generated method stub

        }

        @Override
        public void disconnectedEvent(ConnectionType Type, BluetoothAddress remoteDeviceAddress) {

        }

        @Override
        public void handleValueEvent(ConnectionType Type, BluetoothAddress remoteDeviceAddress,
                boolean handleValueIndication,
                int attHandle, byte[] attributeValue) {
            if (!Type.equals(ConnectionType.LOW_ENERGY))
                return;
            String address = remoteDeviceAddress.toString();
            AttributeData attData = new AttributeData(address, attHandle, attributeValue);

            mHandler.sendMessage(mHandler.obtainMessage(BLE_GATT_HANDLE_VALUE, attData));
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
            if (id == null) {
                Log.e(TAG, "No client for errorResponseEvent found");
                return;
            }
            removeAttReadWriteID(transactionID);

            String address = remoteDeviceAddress.toString();
            int status = getGattStatus(attributeProtoccolErrorType);
            if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
                Log.w(TAG, "Insufficent authentication");
                int result = mDeviceManager.authenticateRemoteLowEnergyDevice(remoteDeviceAddress);
                if (result < 0) {
                    Log.w(TAG, "Authenticate remote fail resuilt=" + result);
                }
            }
            AttributeData attData = new AttributeData(address, handle);
            if (id.readWrite) {
                mHandler.sendMessage(mHandler.obtainMessage(BLE_GATT_WRITE_RESPONSE, id.clientId,
                        status, attData));
            } else {
                mHandler.sendMessage(mHandler.obtainMessage(BLE_GATT_READ_RESPONSE, id.clientId,
                        status, attData));
            }
        }

        @Override
        public void readResponseEvent(ConnectionType Type, BluetoothAddress remoteDeviceAddress,
                int transactionID,
                int handle, boolean isFinal, byte[] value) {
            if (DBG) Log.d(TAG, "readResponseEvent id= " + transactionID);
            if (!Type.equals(ConnectionType.LOW_ENERGY))
                return;

            AttReadWriteID id = getAttReadWriteID(transactionID);
            if (id == null) {
                Log.e(TAG, "No client for readResponseEvent found");
                return;
            }
            removeAttReadWriteID(transactionID);

            String address = remoteDeviceAddress.toString();
            AttributeData attData = new AttributeData(address, handle, value);
            mHandler.sendMessage(mHandler.obtainMessage(BLE_GATT_READ_RESPONSE, id.clientId,
                    BluetoothGatt.GATT_SUCCESS, attData));
        }

        @Override
        public void writeResponseEvent(ConnectionType Type, BluetoothAddress remoteDeviceAddress,
                int transactionID, int handle) {
            if (DBG)
                Log.d(TAG, "writeResponseEvent id=" + transactionID);
            if (!Type.equals(ConnectionType.LOW_ENERGY))
                return;

            AttReadWriteID id = getAttReadWriteID(transactionID);
            if (id == null) {
                Log.e(TAG, "no client for writeResponseEvent found");
                return;
            }
            removeAttReadWriteID(transactionID);

            String address = remoteDeviceAddress.toString();
            AttributeData attData = new AttributeData(address, handle);
            mHandler.sendMessage(mHandler.obtainMessage(BLE_GATT_WRITE_RESPONSE, id.clientId,
                    BluetoothGatt.GATT_SUCCESS, attData));
        }

    }
}
