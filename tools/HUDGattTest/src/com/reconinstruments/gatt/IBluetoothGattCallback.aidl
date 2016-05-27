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

/**
 * Callback definitions for interacting with recon BLE / GATT
 * @hide
 */
interface IBluetoothGattCallback {
    void onClientRegistered(in int status, in int clientIf);
    void onScanResult(in String address, in int rssi, in byte[] advData);
	void onClientConnectionState(in int status, in int clientIf,
	                         in boolean connected, in String address);
	                         
 	void onGetService(in String address, in int srvcInstId,
	                  in ParcelUuid srvcUuid, in int startHandle, in int endHandle);
    void onGetIncludedService(in String address, in int inclSrvcInstId,
                              in ParcelUuid inclSrvcUuid, in int startHandle, in int endHandle);	
 	void onGetCharacteristic(in String address, in int AttHandle,
	                         in ParcelUuid charUuid, in int charProps); 
 	void onGetDescriptor(in String address,in int AttHandle,
                              in ParcelUuid descUuid);
	void onSearchComplete(in String address, in int status);
	void onNotify(in String address, in int AttHandle, in byte[] values);    
	void onWriteResponse(in String address, in int AttHandle, in int status);
	void onReadResponse(in String address, in int AttHandle, in int status, in byte[] values); 
	void onReadRemoteRssi(in String address, in int rssi, in int status);        
}
