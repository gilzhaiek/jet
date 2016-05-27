/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.reconinstruments.gatt;

import android.os.ParcelUuid;
import com.reconinstruments.gatt.IBluetoothGattCallback;

/**
 * API for interacting with recon BLE / GATT
 * @hide
 */
interface IBluetoothGatt {
    List<BluetoothDevice> getDevicesMatchingConnectionStates(in int[] states);

    void registerClient(in ParcelUuid appId, in IBluetoothGattCallback callback);
    void unregisterClient(in int clientIf);

    void startScan(in int appIf, in boolean isServer);
    void startScanWithUuids(in int appIf, in boolean isServer, in ParcelUuid[] ids);
    void stopScan(in int appIf, in boolean isServer);

    void clientConnect(in int clientIf, in String address, in boolean isDirect);
    void clientDisconnect(in int clientIf, in String address);
    void discoverServices(in int clientIf, in String address);
    void writeAttValue(in int clientIf,in String address, in int AttHandle, in byte[] data, in boolean response);
    void readAttValue(in int clientIf,in String address, in int AttHandle);
    
    void readRemoteRssi(in int clientIf, in String address);
    int getDeviceType(in String address);
    void encryptRemoteDevice(in int clientIf, in String address);
    void authenticateRemoteDevice(in int clientIf, in String address);
}
