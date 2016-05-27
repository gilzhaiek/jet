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

/**
 * Helper class to track and hold response data from SS1 read/write attribute requests.
 */
public class AttributeData {
    /**
     * Bluetooth address of the remote device.
     */
    String address;

    /**
     * Attribute handle.
     */
    int attHandle;

    /**
     * Attribute value.
     */
    byte[] values;

    AttributeData(String address, int handle, byte[] values) {
        this.address = address;
        this.attHandle = handle;
        this.values = values;
    }
    
    AttributeData(String address, int handle) {
        this.address = address;
        this.attHandle = handle;
        this.values = null;
    }
}
