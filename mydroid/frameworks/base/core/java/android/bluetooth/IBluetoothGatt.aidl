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
import android.os.ParcelUuid;

import android.bluetooth.IBluetoothGattCallback;
import android.bluetooth.IBluetoothGattServerCallback;

/**
 * API for interacting with BLE / GATT
 * @hide
 */
interface IBluetoothGatt {
    List<BluetoothDevice> getDevicesMatchingConnectionStates(in int[] states);

    void registerClient(in ParcelUuid appId, in IBluetoothGattCallback callback);
    boolean registerReconRemote(in IBluetoothGattCallback callback);
    void unregisterClient(in int clientIf);

    void startScan(in int appIf, in boolean isServer);
    void startScanWithUuids(in int appIf, in boolean isServer, in ParcelUuid[] ids);
    void stopScan(in int appIf, in boolean isServer);

    void clientConnect(in int clientIf, in String address, in boolean isDirect);
    void clientDisconnect(in int clientIf, in String address);
    void discoverServices(in int clientIf, in String address);
    void writeAttributeValue(in int clientIf,in String address, in int attHandle, in byte[] data, in boolean response);
    void readAttributeValue(in int clientIf,in String address, in int attHandle);

    void readRemoteRssi(in int clientIf, in String address);
    int getDeviceType(in String address);

    void encryptRemoteDevice(in int clientIf, in String address);
    void authenticateRemoteDevice(in int clientIf, in String address);

    /*GattServers*/
    void registerServer(in ParcelUuid appId, in IBluetoothGattServerCallback callback);
    void unregisterServer(in int serverIf);
    void serverConnect(in int servertIf, in String address, in boolean isDirect);
    void serverDisconnect(in int serverIf, in String address);
    void beginServiceDeclaration(in int serverIf, in int srvcType,
                            in int srvcInstanceId, in int minHandles,
                            in ParcelUuid srvcId);
    void addIncludedService(in int serverIf, in int srvcType,
                            in int srvcInstanceId, in ParcelUuid srvcId);
    void addCharacteristic(in int serverIf, in ParcelUuid charId,
                            in int properties, in int permissions);
    void addDescriptor(in int serverIf, in ParcelUuid descId,
                            in int permissions);
    void endServiceDeclaration(in int serverIf);
    void removeService(in int serverIf, in int srvcType,
                            in int srvcInstanceId, in ParcelUuid srvcId);
    void clearServices(in int serverIf);
    void sendResponse(in int serverIf, in String address, in int requestId,
                            in int status, in int offset, in byte[] value);
    void sendNotification(in int serverIf, in String address, in int srvcType,
                            in int srvcInstanceId, in ParcelUuid srvcId,
                            in int charInstanceId, in ParcelUuid charId,
                            in boolean confirm, in byte[] value);
}
