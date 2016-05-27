/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.bluetooth.IBluetooth;
import android.bluetooth.IBluetoothStateChangeCallback;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BluetoothGatt implements BluetoothProfile {
    private static final String TAG = "BluetoothGatt";
    private static final boolean DBG = false;
    private static final boolean VDBG = false;

    private final Context mContext;
    private IBluetoothGatt mService;
    private BluetoothGattCallback mCallback;
    private int mClientIf;
    private boolean mAuthRetry = false;
    private BluetoothDevice mDevice;
    private boolean mAutoConnect;
    private int mConnState;
    private final Object mStateLock = new Object();

    private static final int CONN_STATE_IDLE = 0;
    private static final int CONN_STATE_CONNECTING = 1;
    private static final int CONN_STATE_CONNECTED = 2;
    private static final int CONN_STATE_DISCONNECTING = 3;
    private static final int CONN_STATE_CLOSED = 4;
    private static final int CONN_STATE_ENCRYPTING = 5;

    private List<BluetoothGattService> mServices;

    /** A GATT operation completed successfully */
    public static final int GATT_SUCCESS = 0;

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

    /** @hide Indication that the channel is not encrypted, not exposed to developers */
    public static final int GATT_NO_ENCRYPTION = 0x100;

    /** A GATT operation failed, errors other than the above */
    public static final int GATT_FAILURE = 0x101;

    /**
     * No authentication required.
     * @hide
     */
    /*package*/ static final int AUTHENTICATION_NONE = 0;

    /**
     * Authentication requested; no man-in-the-middle protection required.
     * @hide
     */
    /*package*/ static final int AUTHENTICATION_NO_MITM = 1;

    /**
     * Authentication with man-in-the-middle protection requested.
     * @hide
     */
    /*package*/ static final int AUTHENTICATION_MITM = 2;

    private final UUID CLIENT_CONFIGURATION_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    /**
     * Bluetooth GATT interface callbacks
     */
    private final IBluetoothGattCallback mBluetoothGattCallback =
        new IBluetoothGattCallback.Stub() {
            /**
             * Application interface registered - app is ready to go
             * @hide
             */
            public void onClientRegistered(int status, int clientIf) {
                Log.v(TAG, "onClientRegistered() - status=" + status
                    + " clientIf=" + clientIf);
                if (VDBG) {
                    synchronized(mStateLock) {
                        if (mConnState != CONN_STATE_CONNECTING) {
                            Log.e(TAG, "Bad connection state: " + mConnState);
                        }
                    }
                }
                mClientIf = clientIf;
                if (status != GATT_SUCCESS) {
                    mCallback.onConnectionStateChange(BluetoothGatt.this, GATT_FAILURE,
                                                      BluetoothProfile.STATE_DISCONNECTED);
                    synchronized(mStateLock) {
                        mConnState = CONN_STATE_IDLE;
                    }
                    return;
                }
                try {
                    mService.clientConnect(mClientIf, mDevice.getAddress(),
                                           !mAutoConnect); // autoConnect is inverse of "isDirect"
                } catch (RemoteException e) {
                    Log.e(TAG,"",e);
                }
            }

            /**
             * Client connection state changed
             * @hide
             */
            public void onClientConnectionState(int status, int clientIf,
                                                boolean connected, String address) {
                if (DBG) Log.d(TAG, "onClientConnectionState() - status=" + status
                                 + " clientIf=" + clientIf + " device=" + address + " connected=" + connected);
                if (!address.equals(mDevice.getAddress())) {
                    return;
                }

                // Deal with mConnState
                boolean report = true;
                synchronized(mStateLock) {
                    if (DBG) Log.d(TAG, "mConnState=" + mConnState);
                    switch (mConnState) {
                        case CONN_STATE_CONNECTING:
                            if (connected) {
                                if (status == GATT_NO_ENCRYPTION) {
                                    mConnState = CONN_STATE_ENCRYPTING;
                                    report = false;
                                    try {
                                        mService.encryptRemoteDevice(clientIf, address);
                                    } catch (RemoteException e) {
                                        Log.e(TAG, "", e);
                                    }
                                } else {
                                    mConnState = CONN_STATE_CONNECTED;
                                }
                            } else {
                                mConnState = CONN_STATE_IDLE;
                            }
                            break;
                        case CONN_STATE_ENCRYPTING:
                            if (connected) {
                                mConnState = CONN_STATE_CONNECTED;
                            } else {
                                report = false;
                                mConnState = CONN_STATE_DISCONNECTING;
                                try {
                                    mService.clientConnect(clientIf, address, false);
                                } catch (RemoteException e) {
                                    Log.e(TAG, "", e);
                                }
                            }
                            break;
                        default:
                            if (connected) {
                                mConnState = CONN_STATE_CONNECTED;
                            } else {
                                mConnState = CONN_STATE_IDLE;
                            }
                            break;
                    }
                    if (DBG) Log.d(TAG, "change mConnState=" + mConnState);
                }

                if (report) {
                    int profileState = connected ? BluetoothProfile.STATE_CONNECTED :
                                                   BluetoothProfile.STATE_DISCONNECTED;
                    if (status != GATT_FAILURE) {
                        status = GATT_SUCCESS;
                    }

                    try {
                        mCallback.onConnectionStateChange(BluetoothGatt.this, status, profileState);
                    } catch (Exception ex) {
                        Log.w(TAG, "Unhandled exception: " + ex);
                    }
                }
            }

            /**
             * Callback reporting an LE scan result.
             * @hide
             */
            public void onScanResult(String address, int rssi, byte[] advData) {
                // no op
            }

            /**
             * A new GATT service has been discovered.
             * The service is added to the internal list and the search
             * continues.
             * @hide
             */
            public void onGetService(String address, int srvcInstId, ParcelUuid srvcUuid,
                                    int startHandle, int endHandle) {
                if (DBG) Log.d(TAG, "onGetService() - Device=" + address + " UUID=" + srvcUuid);
                if (!address.equals(mDevice.getAddress())) {
                    return;
                }
                // The main service is always primary service from SS1 stack
                BluetoothGattService service = new BluetoothGattService(mDevice, srvcUuid.getUuid(),
                        srvcInstId, BluetoothGattService.SERVICE_TYPE_PRIMARY, startHandle, endHandle);

                mServices.add(service);
            }

            /**
             * An included service has been found durig GATT discovery.
             * The included service is added to the respective parent.
             * @hide
             */
            public void onGetIncludedService(String address, int inclSrvcInstId,
                                            ParcelUuid inclSrvcUuid, 
                                            int startHandle, int endHandle) {
                if (DBG) Log.d(TAG, "onGetIncludedService() - Device=" + address
                    + " Included=" + inclSrvcUuid);

                if (!address.equals(mDevice.getAddress())) {
                    return;
                }
                BluetoothGattService service = getGattService(mDevice,
                        startHandle, endHandle);
                if (service == null) {
                    Log.e(TAG, "Main Service not found!");
                    return;
                }

                // The included service is always secondary service from SS1 stack
                BluetoothGattService includedService = new BluetoothGattService(mDevice, inclSrvcUuid.getUuid(),
                        inclSrvcInstId, BluetoothGattService.SERVICE_TYPE_SECONDARY, startHandle, endHandle);
                service.addIncludedService(includedService);
            }

            /**
             * A new GATT characteristic has been discovered.
             * Add the new characteristic to the relevant service and continue
             * the remote device inspection.
             * @hide
             */
            public void onGetCharacteristic(String address, int attHandle,
                                        ParcelUuid charUuid, int charProps)  {
                if (DBG) Log.d(TAG, "onGetCharacteristic() - Device=" + address + " UUID=" +
                               charUuid);

                if (!address.equals(mDevice.getAddress())) {
                    return;
                }

                BluetoothGattService service = getGattService(mDevice, attHandle, attHandle);
                if (service == null) {
                    Log.e(TAG, "Main Service for Characteristic not found!");
                    return;
                }

                UUID uuid = charUuid.getUuid();
                BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                                                        service, uuid, attHandle, charProps, 0);
                service.addCharacteristic(characteristic);
            }

            /**
             * A new GATT descriptor has been discovered.
             * Finally, add the descriptor to the related characteristic.
             * This should conclude the remote device update.
             * @hide
             */
            public void onGetDescriptor(String address, int attHandle, ParcelUuid descUuid)  {
                if (DBG) Log.d(TAG, "onGetDescriptor() - Device=" + address + " UUID=" + descUuid);

                if (!address.equals(mDevice.getAddress())) {
                    return;
                }

                BluetoothGattService service = getGattService(mDevice, attHandle, attHandle);
                if (service == null) return; 

                BluetoothGattCharacteristic characteristic =
                    service.getCharacteristicFromDescHandle(attHandle);

                if (characteristic == null) {
                    Log.e(TAG, "Characteristic for the Descriptor not found!");
                    return;
                }

                UUID uuid = descUuid.getUuid();
                BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                        characteristic, uuid, attHandle, 0);
                characteristic.addDescriptor(descriptor);
            }

            /**
             * Remote search has been completed.
             * The internal object structure should now reflect the state
             * of the remote device database. Let the application know that
             * we are done at this point.
             * @hide
             */
            public void onSearchComplete(String address, int status) {
                if (DBG) Log.d(TAG, "onSearchComplete() = Device=" + address + " Status=" + status);
                if (!address.equals(mDevice.getAddress())) {
                    return;
                }
                try {
                    mCallback.onServicesDiscovered(BluetoothGatt.this, status);
                } catch (Exception ex) {
                    Log.w(TAG, "Unhandled exception: " + ex);
                }
            }

            /**
             * Remote characteristic has been updated.
             * Updates the internal value.
             * @hide
             */
            public void onNotify(String address, int attHandle, byte[] values) {
                if (DBG) Log.d(TAG, "onNotify() - Device=" + address + " handle=" + attHandle);

                if (!address.equals(mDevice.getAddress())) {
                    return;
                }

                BluetoothGattService service = getGattService(mDevice, attHandle, attHandle);
                if (service == null) {
                    Log.e(TAG, "Main Service for Characteristic not found!");
                    return;
                }

                BluetoothGattCharacteristic characteristic = service.getCharacteristic(attHandle);
                if (characteristic == null) {
                    Log.e(TAG, "Characteristic not found!");
                    return;
                }

                characteristic.setValue(values);

                try {
                    mCallback.onCharacteristicChanged(BluetoothGatt.this, characteristic);
                } catch (Exception ex) {
                    Log.w(TAG, "Unhandled exception: " + ex);
                }
            }

            /**
             * Values have been written to the remote device.
             * Let the app know how we did.
             * @hide
             */
            public void onWriteResponse(String address, int attHandle, int status) {
                if (DBG) Log.d(TAG, "onWriteResponse() - Device=" + address + " handle=" + attHandle);

                if (!address.equals(mDevice.getAddress())) {
                    return;
                }

                BluetoothGattService service = getGattService(mDevice, attHandle, attHandle);
                if (service == null) {
                    Log.e(TAG, "Main Service for Characteristic not found!");
                    return;
                }
                
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(attHandle);
                if (characteristic == null) { // Check if it's descriptors
                    characteristic = service.getCharacteristicFromDescHandle(attHandle);
                    if (characteristic == null) {
                        Log.e(TAG, "Characteristic for the Descriptor not found!");
                        return;
                    }
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(attHandle);
                    if (descriptor == null) return;
                    if (DBG) Log.d(TAG, "onDescriptorWrite status=" + status);
                    try {
                        mCallback.onDescriptorWrite(BluetoothGatt.this, descriptor, status);
                    } catch (Exception ex) {
                        Log.w(TAG, "Unhandled exception: " + ex);
                    }
                } else { // Characteristic found
                    if (DBG) Log.d(TAG, "onCharacteristicWrite status=" + status);
                    try {
                        mCallback.onCharacteristicWrite(BluetoothGatt.this, characteristic, status);
                    } catch (Exception ex) {
                        Log.w(TAG, "Unhandled exception: " + ex);
                    }
                }
            }

            /**
             * Values have been read.
             * @hide
             */
            public void onReadResponse(String address, int attHandle, int status, byte[] values) {
                if (DBG) {
                    Log.d(TAG, "onReadResponse() - Device=" + address + " handle=" + attHandle);
                }

                if (!address.equals(mDevice.getAddress())) {
                    return;
                }

                BluetoothGattService service = getGattService(mDevice, attHandle, attHandle);
                if (service == null) {
                    Log.e(TAG, "Main Service for Characteristic not found!");
                    return;
                }

                BluetoothGattCharacteristic characteristic = service.getCharacteristic(attHandle);
                if (characteristic == null ) { // Check if it's descriptors
                    characteristic = service.getCharacteristicFromDescHandle(attHandle);
                    if (characteristic == null) {
                        Log.e(TAG, "Characteristic for the Descriptor not found!");
                        return;
                    }
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(attHandle);
                    if (descriptor == null) return;
                    if (DBG) Log.d(TAG, "onDescriptorRead status=" + status);
                    if (status == 0) descriptor.setValue(values);

                    try {
                        mCallback.onDescriptorRead(BluetoothGatt.this, descriptor, status);
                    } catch (Exception ex) {
                        Log.w(TAG, "Unhandled exception: " + ex);
                    }
                } else { // Characteristic found
                    if (DBG) Log.d(TAG, "onCharacteristicRead status=" + status);
                    if (status == 0) characteristic.setValue(values);
                    try {
                        mCallback.onCharacteristicRead(BluetoothGatt.this, characteristic, status);
                    } catch (Exception ex) {
                        Log.w(TAG, "Unhandled exception: " + ex);
                    }
                }
            }

            /**
             * Remote device RSSI has been read
             * @hide
             */
            public void onReadRemoteRssi(String address, int rssi, int status) {
                if (DBG) Log.d(TAG, "onReadRemoteRssi() - Device=" + address +
                            " rssi=" + rssi + " status=" + status);
                if (!address.equals(mDevice.getAddress())) {
                    return;
                }
                try {
                    mCallback.onReadRemoteRssi(BluetoothGatt.this, rssi, status);
                } catch (Exception ex) {
                    Log.w(TAG, "Unhandled exception: " + ex);
                }
            } 

        };

    /*package*/ BluetoothGatt(Context context, IBluetoothGatt iGatt, BluetoothDevice device) {
        mContext = context;
        mService = iGatt;
        mDevice = device;
        mServices = new ArrayList<BluetoothGattService>();

        mConnState = CONN_STATE_IDLE;
    }

    /**
     * Close this Bluetooth GATT client.
     *
     * Application should call this method as early as possible after it is done with
     * this GATT client.
     */
    public void close() {
        if (DBG) Log.d(TAG, "close()");

        unregisterApp();
        mConnState = CONN_STATE_CLOSED;
    }

    /**
     * Register an application callback to start using GATT.
     *
     * <p>This is an asynchronous call. The callback {@link BluetoothGattCallback#onAppRegistered}
     * is used to notify success or failure if the function returns true.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param callback GATT callback handler that will receive asynchronous callbacks.
     * @return If true, the callback will be called to notify success or failure,
     *         false on immediate error
     */
    private boolean registerApp(BluetoothGattCallback callback) {
        if (DBG) Log.d(TAG, "registerApp()");
        if (mService == null) return false;

        mCallback = callback;
        UUID uuid = UUID.randomUUID();
        if (DBG) Log.d(TAG, "registerApp() - UUID=" + uuid);

        try {
            mService.registerClient(new ParcelUuid(uuid), mBluetoothGattCallback);
        } catch (RemoteException e) {
            Log.e(TAG,"",e);
            return false;
        }

        return true;
    }

    /**
     * Unregister the current application and callbacks.
     */
    private void unregisterApp() {
        if (DBG) Log.d(TAG, "unregisterApp() - mClientIf=" + mClientIf);
        if (mService == null || mClientIf == 0) return;

        try {
            mCallback = null;
            mService.unregisterClient(mClientIf);
            mClientIf = 0;
        } catch (RemoteException e) {
            Log.e(TAG,"",e);
        }
    }

    /**
     * Initiate a connection to a Bluetooth GATT capable device.
     *
     * <p>The connection may not be established right away, but will be
     * completed when the remote device is available. A
     * {@link BluetoothGattCallback#onConnectionStateChange} callback will be
     * invoked when the connection state changes as a result of this function.
     *
     * <p>The autoConnect paramter determines whether to actively connect to
     * the remote device, or rather passively scan and finalize the connection
     * when the remote device is in range/available. Generally, the first ever
     * connection to a device should be direct (autoConnect set to false) and
     * subsequent connections to known devices should be invoked with the
     * autoConnect parameter set to true.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param device Remote device to connect to
     * @param autoConnect Whether to directly connect to the remote device (false)
     *                    or to automatically connect as soon as the remote
     *                    device becomes available (true).
     * @return true, if the connection attempt was initiated successfully
     */
    /*package*/ boolean connect(Boolean autoConnect, BluetoothGattCallback callback) {
        if (DBG) Log.d(TAG, "connect() - device: " + mDevice.getAddress() + ", auto: " + autoConnect);
        synchronized(mStateLock) {
            if (mConnState != CONN_STATE_IDLE) {
                throw new IllegalStateException("Not idle");
            }
            mConnState = CONN_STATE_CONNECTING;
        }
        if (!registerApp(callback)) {
            synchronized(mStateLock) {
                mConnState = CONN_STATE_IDLE;
            }
            Log.e(TAG, "Failed to register callback");
            return false;
        }

        // the connection will continue after successful callback registration
        mAutoConnect = autoConnect;
        return true;
    }

    /**
     * Disconnects an established connection, or cancels a connection attempt
     * currently in progress.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     */
    public void disconnect() {
        if (DBG) Log.d(TAG, "cancelOpen() - device: " + mDevice.getAddress());
        if (mService == null || mClientIf == 0) return;

        try {
            mService.clientDisconnect(mClientIf, mDevice.getAddress());
        } catch (RemoteException e) {
            Log.e(TAG,"",e);
        }
    }

    /**
     * Connect back to remote device.
     *
     * <p>This method is used to re-connect to a remote device after the
     * connection has been dropped. If the device is not in range, the
     * re-connection will be triggered once the device is back in range.
     *
     * @return true, if the connection attempt was initiated successfully
     */
    public boolean connect() {
        try {
            mService.clientConnect(mClientIf, mDevice.getAddress(),
                                   false); // autoConnect is inverse of "isDirect"
            return true;
        } catch (RemoteException e) {
            Log.e(TAG,"",e);
            return false;
        }
    }

    /**
     * Return the remote bluetooth device this GATT client targets to
     *
     * @return remote bluetooth device
     */
    public BluetoothDevice getDevice() {
        return mDevice;
    }

    /**
     * Discovers services offered by a remote device as well as their
     * characteristics and descriptors.
     *
     * <p>This is an asynchronous operation. Once service discovery is completed,
     * the {@link BluetoothGattCallback#onServicesDiscovered} callback is
     * triggered. If the discovery was successful, the remote services can be
     * retrieved using the {@link #getServices} function.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @return true, if the remote service discovery has been started
     */
    public boolean discoverServices() {
        if (DBG) Log.d(TAG, "discoverServices() - device: " + mDevice.getAddress());
        if (mService == null || mClientIf == 0) return false;

        mServices.clear();

        try {
            mService.discoverServices(mClientIf, mDevice.getAddress());
        } catch (RemoteException e) {
            Log.e(TAG,"",e);
            return false;
        }

        return true;
    }

    /**
     * Recon: Returns a service by start/endhandle range
     */
    private BluetoothGattService getServiceFromList(BluetoothDevice device, int startHandle,
        int endHandle, List<BluetoothGattService> services) {
        if (services.isEmpty()) {
            return null;
        }

        for (BluetoothGattService svc : services) {
            if (svc.getDevice().equals(device) &&
                svc.getStartHandle() <= startHandle &&
                svc.getEndHandle() >= endHandle) {
                    return svc;
            }
        }
        return null;
    }

    /**
     * Recon: Returns a service by UUID, instance and start/endhandle
     */
    private BluetoothGattService getServiceFromList(BluetoothDevice device, UUID uuid,
            int instanceId, int startHandle, int endHandle,
            List<BluetoothGattService> services) {
        if (services.isEmpty()) {
            return null;
        }

        for (BluetoothGattService svc : services) {
            if (svc.getDevice().equals(device) &&
                svc.getInstanceId() == instanceId &&
                svc.getUuid().equals(uuid) &&
                svc.getStartHandle() == startHandle &&
                svc.getEndHandle() == endHandle) {
                    return svc;
            }
        }
        return null;
    }

    /**
     * Mainly used to get BluetoothGattService based on the attHandle ranges
     * @param device
     * @param startHandle
     * @param endHandle
     * @return
     */
    private BluetoothGattService getGattService(BluetoothDevice device, int startHandle,
        int endHandle) {
        return getServiceFromList(device, startHandle,
               endHandle, mServices);
    }
    
    /**
     * Mainly used to get  BluetoothGattService to check if it's the same service
     * @param device
     * @param uuid
     * @param instanceId
     * @param startHandle
     * @param endHandle
     * @return
     */
    private BluetoothGattService getGattService(BluetoothDevice device, UUID uuid,
            int instanceId, int startHandle, int endHandle) {
        return getServiceFromList(device, uuid, instanceId,
                startHandle, endHandle, mServices);
    }

    /**
     * Returns a list of GATT services offered by the remote device.
     *
     * <p>This function requires that service discovery has been completed
     * for the given device.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @return List of services on the remote device. Returns an empty list
     *         if service discovery has not yet been performed.
     */
    public List<BluetoothGattService> getServices() {
        List<BluetoothGattService> result =
                new ArrayList<BluetoothGattService>();

        for (BluetoothGattService service : mServices) {
            if (service.getDevice().equals(mDevice)) {
                result.add(service);
            }
        }

        return result;
    }

    /**
     * Returns a {@link BluetoothGattService}, if the requested UUID is
     * supported by the remote device.
     *
     * <p>This function requires that service discovery has been completed
     * for the given device.
     *
     * <p>If multiple instances of the same service (as identified by UUID)
     * exist, the first instance of the service is returned.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param uuid UUID of the requested service
     * @return BluetoothGattService if supported, or null if the requested
     *         service is not offered by the remote device.
     */
    public BluetoothGattService getService(UUID uuid) {
        for (BluetoothGattService service : mServices) {
            if (service.getDevice().equals(mDevice) &&
                service.getUuid().equals(uuid)) {
                return service;
            }
        }

        return null;
    }

    /**
     * Reads the requested characteristic from the associated remote device.
     *
     * <p>This is an asynchronous operation. The result of the read operation
     * is reported by the {@link BluetoothGattCallback#onCharacteristicRead}
     * callback.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param characteristic Characteristic to read from the remote device
     * @return true, if the read operation was initiated successfully
     */
    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if ((characteristic.getProperties() &
                BluetoothGattCharacteristic.PROPERTY_READ) == 0) return false;

        if (DBG) Log.d(TAG, "readCharacteristic() - uuid: " + characteristic.getUuid());
        if (mService == null || mClientIf == 0) return false;

        BluetoothGattService service = characteristic.getService();
        if (service == null) return false;

        BluetoothDevice device = service.getDevice();
        if (device == null) return false;

        try {
            mService.readAttributeValue(mClientIf, device.getAddress(), characteristic.getInstanceId());
        } catch (RemoteException e) {
            Log.e(TAG,"",e);
            return false;
        }
        return true;
    }

    /**
     * Writes a given characteristic and its values to the associated remote device.
     *
     * <p>Once the write operation has been completed, the
     * {@link BluetoothGattCallback#onCharacteristicWrite} callback is invoked,
     * reporting the result of the operation.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param characteristic Characteristic to write on the remote device
     * @return true, if the write operation was initiated successfully
     */
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0
            && (characteristic.getProperties() &
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0) return false;

        if (DBG) Log.d(TAG, "writeCharacteristic() - uuid: " + characteristic.getUuid());
        if (mService == null || mClientIf == 0) return false;

        BluetoothGattService service = characteristic.getService();
        if (service == null) return false;

        BluetoothDevice device = service.getDevice();
        if (device == null) return false;

        boolean response = true;
        if (characteristic.getWriteType() == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
            response = false;
        }

        try {
            mService.writeAttributeValue(mClientIf, device.getAddress(), 
                    characteristic.getInstanceId(), characteristic.getValue(), response);
        } catch (RemoteException e) {
            Log.e(TAG,"",e);
            return false;
        }
        return true;
    }

    /**
     * Reads the value for a given descriptor from the associated remote device.
     *
     * <p>Once the read operation has been completed, the
     * {@link BluetoothGattCallback#onDescriptorRead} callback is
     * triggered, signaling the result of the operation.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param descriptor Descriptor value to read from the remote device
     * @return true, if the read operation was initiated successfully
     */
    public boolean readDescriptor(BluetoothGattDescriptor descriptor) {
        if (DBG) Log.d(TAG, "readDescriptor() - uuid: " + descriptor.getUuid());
        if (mService == null || mClientIf == 0) return false;

        BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
        if (characteristic == null) return false;

        BluetoothGattService service = characteristic.getService();
        if (service == null) return false;

        BluetoothDevice device = service.getDevice();
        if (device == null) return false;

        try {
            mService.readAttributeValue(mClientIf, device.getAddress(), descriptor.getHandle());
        } catch (RemoteException e) {
            Log.e(TAG,"",e);
            return false;
        }
        return true;
    }

    /**
     * Write the value of a given descriptor to the associated remote device.
     *
     * <p>A {@link BluetoothGattCallback#onDescriptorWrite} callback is
     * triggered to report the result of the write operation.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param descriptor Descriptor to write to the associated remote device
     * @return true, if the write operation was initiated successfully
     */
    public boolean writeDescriptor(BluetoothGattDescriptor descriptor) {
        if (DBG) Log.d(TAG, "writeDescriptor() - uuid: " + descriptor.getUuid());
        if (mService == null || mClientIf == 0) return false;

        BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
        if (characteristic == null) return false;

        BluetoothGattService service = characteristic.getService();
        if (service == null) return false;

        BluetoothDevice device = service.getDevice();
        if (device == null) return false;

        boolean response = true;
        if (characteristic.getWriteType() == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
            response = false;
        }

        try {
            mService.writeAttributeValue(mClientIf, device.getAddress(),
                    descriptor.getHandle(), descriptor.getValue(), response);
        } catch (RemoteException e) {
            Log.e(TAG,"",e);
            return false;
        }
        return true;
    }

    /**
     * Initiates a reliable write transaction for a given remote device.
     *
     * <p>Once a reliable write transaction has been initiated, all calls
     * to {@link #writeCharacteristic} are sent to the remote device for
     * verification and queued up for atomic execution. The application will
     * receive an {@link BluetoothGattCallback#onCharacteristicWrite} callback
     * in response to every {@link #writeCharacteristic} call and is responsible
     * for verifying if the value has been transmitted accurately.
     *
     * <p>After all characteristics have been queued up and verified,
     * {@link #executeReliableWrite} will execute all writes. If a characteristic
     * was not written correctly, calling {@link #abortReliableWrite} will
     * cancel the current transaction without commiting any values on the
     * remote device.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @return true, if the reliable write transaction has been initiated
     */
    public boolean beginReliableWrite() {
        if (DBG) Log.d(TAG, "beginReliableWrite() - device: " + mDevice.getAddress());
        if (mService == null || mClientIf == 0) return false;
        //Recon: SS1 stack no need such feature
        return true;
    }

    /**
     * Executes a reliable write transaction for a given remote device.
     *
     * <p>This function will commit all queued up characteristic write
     * operations for a given remote device.
     *
     * <p>A {@link BluetoothGattCallback#onReliableWriteCompleted} callback is
     * invoked to indicate whether the transaction has been executed correctly.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @return true, if the request to execute the transaction has been sent
     */
    public boolean executeReliableWrite() {
        if (DBG) Log.d(TAG, "executeReliableWrite() - device: " + mDevice.getAddress());
        if (mService == null || mClientIf == 0) return false;

        //Recon: SS1 stack no need such feature
        return true;
    }

    /**
     * Cancels a reliable write transaction for a given device.
     *
     * <p>Calling this function will discard all queued characteristic write
     * operations for a given remote device.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     */
    public void abortReliableWrite(BluetoothDevice mDevice) {
        //Recon: SS1 stack no need such feature
        return;
    }

    /**
     * Enable or disable notifications/indications for a given characteristic.
     *
     * <p>Once notifications are enabled for a characteristic, a
     * {@link BluetoothGattCallback#onCharacteristicChanged} callback will be
     * triggered if the remote device indicates that the given characteristic
     * has changed.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param characteristic The characteristic for which to enable notifications
     * @param enable Set to true to enable notifications/indications
     * @return true, if the requested notification status was set successfully
     */
    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enable) {
        if (DBG) Log.d(TAG, "setCharacteristicNotification() - uuid: " + characteristic.getUuid()
                         + " enable: " + enable);
        if (mService == null || mClientIf == 0) return false;

        BluetoothGattService service = characteristic.getService();
        if (service == null) return false;

        BluetoothDevice device = service.getDevice();
        if (device == null) return false;

        boolean isNotification;
        int property = characteristic.getProperties();

        if ((property & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
            isNotification = true;
        } else if ((property & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
            isNotification = false;
        } else {
            Log.e(TAG, "characteristic don't support notifications/indications");
            return false;
        }

        boolean response = true;
        if (characteristic.getWriteType() == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
            response = false;
        }

        // Find the Descriptor
        List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
        for (BluetoothGattDescriptor descriptor : descriptors) {
            if (descriptor.getUuid().equals(CLIENT_CONFIGURATION_UUID)) {
                if (enable) {
                    if (isNotification) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    } else {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    }
                } else {
                    descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                }

                try {
                    mService.writeAttributeValue(mClientIf, device.getAddress(), 
                            descriptor.getHandle(), descriptor.getValue(), response);
                    return true;
                } catch (RemoteException e) {
                    Log.e(TAG,"",e);
                    return false;
                }
            }
        }

        Log.e(TAG, "Didn't find CLIENT_CONFIGURATION_UUID!");
        return false;
    }

    /**
     * Clears the internal cache and forces a refresh of the services from the
     * remote device.
     * @hide
     */
    public boolean refresh() {
        if (DBG) Log.d(TAG, "refresh() - device: " + mDevice.getAddress());
        if (mService == null || mClientIf == 0) return false;
        //Recon: SS1 stack no need such feature
        return true;
    }

    /**
     * Read the RSSI for a connected remote device.
     *
     * <p>The {@link BluetoothGattCallback#onReadRemoteRssi} callback will be
     * invoked when the RSSI value has been read.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @return true, if the RSSI value has been requested successfully
     */
    public boolean readRemoteRssi() {
        if (DBG) Log.d(TAG, "readRssi() - device: " + mDevice.getAddress());
        if (mService == null || mClientIf == 0) return false;

        try {
            mService.readRemoteRssi(mClientIf, mDevice.getAddress());
        } catch (RemoteException e) {
            Log.e(TAG,"",e);
            return false;
        }

        return true;
    }

    @Override
    public int getConnectionState(BluetoothDevice device) {
        throw new UnsupportedOperationException("Use BluetoothManager#getConnectionState instead.");
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        throw new UnsupportedOperationException
            ("Use BluetoothManager#getConnectedDevices instead.");
    }

    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        throw new UnsupportedOperationException
            ("Use BluetoothManager#getDevicesMatchingConnectionStates instead.");
    }
}
