
package com.reconinstruments.gatt;

import java.util.ArrayList;
import java.util.List;
/**
 * Helper class identifying a client that has requested LE scan results.
 * @hide
 */
import java.util.UUID;

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
            for (ScanClient client : mScanQueue) {
                if (client.appIf == appIf) {
                    mScanQueue.remove(client);
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
}
