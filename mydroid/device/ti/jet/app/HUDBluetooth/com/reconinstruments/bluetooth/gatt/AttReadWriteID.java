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
 * Helper class to keep track of reading/writing attributes on the remote device.
 */
public class AttReadWriteID {
    /**
     * SS1 transaction ID used to track the response of a read/write value request.
     */
    public int transactionID;

    /**
     * ContextMap's connection ID.
     */
    public int clientId;

    /**
     * Flag to indicate write or read. True for write; false for read.
     */
    public boolean readWrite;

    AttReadWriteID(int transactionID, int clientId, boolean rw) {
        this.transactionID = transactionID;
        this.clientId = clientId;
        this.readWrite = rw;
    }
}
