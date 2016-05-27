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

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
/**
 * Helper class that contains a queue of clients that have requested GATT communication.
 */
public class GattQueue {
    static final int UPDATE_REMOTE_SERVICES = 1;

    /**
     * Helper class identifying a client that has requested GATT communication.
     */
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

    /**
     * Add a client to the queue.
     */
    void add(int clientIf, String address) {
        synchronized (mGattQueue) {
            mGattQueue.add(new GattClient(clientIf, address));
        }
    }

    /**
     * Remove all clients in the queue.
     */
    void clear() {
        synchronized (mGattQueue) {
            mGattQueue.clear();
        }
    }

    /**
     * Remove a client from the queue.one client ID may have multiple GattClients
     */
    void removeGattClient(int appIf) {
        synchronized (mGattQueue) {
            Iterator<GattClient> iter = mGattQueue.iterator();
            while (iter.hasNext()) {
                GattClient client = iter.next();
                if (client.appIf == appIf) {
                    iter.remove();
                    continue;
                }
            }
        }
    }

    /**
     * Retrieve GattClient.
     */
    GattClient getGattClient(int appIf, String address) {
        for (GattClient client : mGattQueue) {
            if (client.appIf == appIf && client.address.equals(address))
                return client;
        }
        return null;
    }

    /**
     * Alow multiple clients to connect to one remote
     */
    List<Integer> getGattClientIdbyAddress(String address) {
        List<Integer> appids = new ArrayList<Integer>();
        for (GattClient client : mGattQueue) {
            if (client.address.equals(address))
                appids.add(client.appIf);
        }

        return appids;
    }

    /**
     * Set a command for a particular client.
     */
    int setGattClientCmd(int appIf, String address, int cmd) {
        GattClient client = getGattClient(appIf, address);
        if (client == null) {
            return 0;
        }
        synchronized (client) {
            client.setCmd(cmd);
        }
        return 1;
    }

    /**
     * Check to see if a client with the address has the command set. Also clears the command.
     */
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

    /**
     * Check to see if the queue is empty.
     */
    boolean isEmpty() {
        return mGattQueue.isEmpty();
    }
}
