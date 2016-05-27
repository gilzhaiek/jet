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
 * Helper class identifying a client that has requested LE scan results.
 * @hide
 */
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
/*package*/
public class ScanQueue {
    class ScanClient {
        int appIf;
        UUID[] uuids;

        ScanClient(int appIf) {
            this.appIf = appIf;
            this.uuids = new UUID[0];
        }

        ScanClient(int appIf, UUID[] uuids) {
            this.appIf = appIf;
            this.uuids = uuids;
        }
    }

    List<ScanClient> mScanQueue = new ArrayList<ScanClient>();

    ScanClient getScanClient(int appIf) {
        for (ScanClient client : mScanQueue) {
            if (client.appIf == appIf)
                return client;
        }
        return null;
    }

    void removeScanClient(int appIf) {
        synchronized (mScanQueue) {
            Iterator <ScanClient> iter = mScanQueue.iterator();
            while (iter.hasNext()) {
                ScanClient client = iter.next();
                if (client.appIf == appIf) {
                    iter.remove();
                    break;
                }
            }
        }
    }

    boolean isEmpty() {
        return mScanQueue.isEmpty();
    }

    void add(int clientIf) {
        synchronized (mScanQueue) {
            mScanQueue.add(new ScanClient(clientIf));
        }
    }

    void add(int clientIf, UUID[] uuids) {
        synchronized (mScanQueue) {
            mScanQueue.add(new ScanClient(clientIf, uuids));
        }
    }

    void clear() {
        synchronized (mScanQueue) {
            mScanQueue.clear();
        }
    }

    /**
     * Alow multiple clients to connect to one remote
     *
     * @param address
     * @return
     */
    List<Integer> getScanClientIdbyUuids(List<UUID>  remoteUuids) {
        List<Integer> appids = new ArrayList<Integer>();
        for (ScanClient client : mScanQueue) {

            if (client.uuids.length > 0) { // Check if client only want specific GATT service UUID
                int matches = 0;
                for (UUID search : client.uuids) {
                    // Compare with each UUID founded in the remote
                    for (UUID remote : remoteUuids) {
                        if (remote.equals(search)) {
                            ++matches;
                            break; // Only count 1st match in case of
                                   // duplicates
                        }
                    }
                }

                if (matches < client.uuids.length) {
                    //matched uuids didn't find, keep searching...
                    continue;
                }
            }

            appids.add(client.appIf);
        }
        return appids;
    }

    /**
     * Return all appids
     * @return
     */
    List<Integer> getScanClientIds() {
        List<Integer> appids = new ArrayList<Integer>();
        for (ScanClient client : mScanQueue) {
            appids.add(client.appIf);
        }
        return appids;
    }
}
