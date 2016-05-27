
package com.reconinstruments.gatt;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class identifying a client that has requested GATT communication.
 */
public class GattQueue {
    static final int UPDATE_REMOTE_SERVICES = 1;

    public class GattClient {
        int appIf;
        // Bluetooth address, e.g. "90:D7:EB:B2:66:DE"
        String address;
        // command begin to send to the remote
        int cmd;

        GattClient(int appIf, String address) {
            this.appIf = appIf;
            this.address = address;
            this.cmd = 0;
        }

        void setCmd(int cmd) {
            this.cmd = cmd;
        }

        int getCmd() {
            return cmd;
        }
    }

    private List<GattClient> mGattQueue = new ArrayList<GattClient>();

    void add(int clientIf, String address) {
        synchronized (mGattQueue) {
            mGattQueue.add(new GattClient(clientIf, address));
        }
    }

    void clear() {
        synchronized (mGattQueue) {
            mGattQueue.clear();
        }
    }

    void removeGattClient(int appIf) {
        synchronized (mGattQueue) {
            for (GattClient client : mGattQueue) {
                if (client.appIf == appIf) {
                    mGattQueue.remove(client);
                    continue;
                }
            }
        }
    }

    GattClient getGattClient(int appIf, String address) {
        for (GattClient client : mGattQueue) {
            if (client.appIf == appIf && client.address.equals(address))
                return client;
        }
        return null;
    }

    /**
     * Alow multiple clients to connect to one remote
     * 
     * @param address
     * @return
     */
    List<Integer> getGattClientIdbyAddress(String address) {
        List<Integer> appids = new ArrayList<Integer>();
        for (GattClient client : mGattQueue) {
            if (client.address.equals(address))
                appids.add(client.appIf);
        }

        return appids;
    }

    int setGattClientCmd(int appIf, String address, int cmd) {
        GattClient client = getGattClient(appIf, address);
        if (client == null) {
            return 0;
        }
        synchronized (client) {
            client.setCmd(UPDATE_REMOTE_SERVICES);
        }
        return 1;
    }

    int checkCmdandClear(String address, int cmd) {
        for (GattClient client : mGattQueue) {
            if (client.address.equals(address) && client.cmd == cmd) {
                synchronized (client) {
                    client.setCmd(0);
                }
                return client.appIf;
            }
        }
        return 0;
    }
    
    boolean isEmpty() {
        return mGattQueue.isEmpty();
    }
}
