package com.reconinstruments.gatt;

import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Helper class that keeps track of registered GATT applications.
 * This class manages application callbacks and keeps track of GATT connections.
 * @hide
 */
/*package*/ class ContextMap<T> {
    private static final String TAG = "GattTestContextMap";

    /**
     * Connection class helps map connection IDs to device addresses.
     */
    class Connection {
        int connId;
        String address;
        int appId;

        Connection(int connId, String address,int appId) {
            this.connId = connId;
            this.address = address;
            this.appId = appId;
        }
    }

    /**
     * Application entry mapping appIDs and callbacks.
     */
    class App {
        /** The id of the application */
        int id;

        /** Application callbacks */
        T callback;

        /** Death receipient */
        private IBinder.DeathRecipient mDeathRecipient;

        /**
         * Creates a new app context.
         */
        App(int id, T callback) {
            this.id = id;
            this.callback = callback;
        }

        /**
         * Link death recipient
         */
        void linkToDeath(IBinder.DeathRecipient deathRecipient) {
            try {
                IBinder binder = ((IInterface)callback).asBinder();
                binder.linkToDeath(deathRecipient, 0);
                mDeathRecipient = deathRecipient;
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to link deathRecipient for app id " + id);
            }
        }

        /**
         * Unlink death recipient
         */
        void unlinkToDeath() {
            if (mDeathRecipient != null) {
                try {
                    IBinder binder = ((IInterface)callback).asBinder();
                    binder.unlinkToDeath(mDeathRecipient,0);
                } catch (NoSuchElementException e) {
                    Log.e(TAG, "Unable to unlink deathRecipient for app id " + id);
                }
            }
        }
    }

    /** Our internal application list */
    List<App> mApps = new ArrayList<App>();

    /** Internal list of connected devices **/
    Set<Connection> mConnections = new HashSet<Connection>();

    /**
     * Add an entry to the application context list.
     */
    void add(int id, T callback) {
        synchronized (mApps) {
            mApps.add(new App(id, callback));
        }
    }

    /**
     * Remove the context for a given application ID.
     */
    void remove(int id) {
        synchronized (mApps) {
            Iterator<App> i = mApps.iterator();
            while(i.hasNext()) {
                App entry = i.next();
                if (entry.id == id) {
                    entry.unlinkToDeath();
                    i.remove();
                    break;
                }
            }
        }
    }

    /**
     * Add a new connection for a given application ID.
     */
    void addConnection(int id, int connId, String address) {
        synchronized (mConnections) {
            App entry = getById(id);
            if (entry != null){
                mConnections.add(new Connection(connId, address, id));
            }
        }
    }

    /**
     * Remove a connection with the given ID.
     */
    void removeConnection(int connId, String address) {
        synchronized (mConnections) {
            Iterator<Connection> i = mConnections.iterator();
            while(i.hasNext()) {
                Connection connection = i.next();
                if (connection.connId == connId && connection.address.equals(address)) {
                    i.remove();
                    break;
                }
            }
        }
    }

    /**
     * Get an application context by ID.
     */
    App getById(int id) {
        Iterator<App> i = mApps.iterator();
        while(i.hasNext()) {
            App entry = i.next();
            if (entry.id == id) return entry;
        }
        Log.d(TAG, "Empty Context ID " + id);
        return null;
    }

    /**
     * Get the device addresses for all connected devices
     */
    Set<String> getConnectedDevices() {
        Set<String> addresses = new HashSet<String>();
        Iterator<Connection> i = mConnections.iterator();
        while(i.hasNext()) {
            Connection connection = i.next();
            addresses.add(connection.address);
        }
        return addresses;
    }

    /**
     * Get an application context by a connection ID.
     */
    App getByConnId(int connId) {
        Iterator<Connection> ii = mConnections.iterator();
        while(ii.hasNext()) {
            Connection connection = ii.next();
            if (connection.connId == connId){
                return getById(connection.appId);
            }
        }
        return null;
    }

    /**
     * Returns a connection ID for a given device address.
     */
    Integer connIdByAddress(int id, String address) {
        App entry = getById(id);
        if (entry == null) return null;

        Iterator<Connection> i = mConnections.iterator();
        while(i.hasNext()) {
            Connection connection = i.next();
            if (connection.address.equals(address) && connection.appId == id)
                return connection.connId;
        }
        return null;
    }

    /**
     * Returns the device address for a given connection ID.
     */
    String addressByConnId(int connId) {
        Iterator<Connection> i = mConnections.iterator();
        while(i.hasNext()) {
            Connection connection = i.next();
            if (connection.connId == connId) return connection.address;
        }
        return null;
    }

    List<Connection> getConnectionByApp(int appId) {
        List<Connection> currentConnections = new ArrayList<Connection>();
        Iterator<Connection> i = mConnections.iterator();
        while(i.hasNext()) {
            Connection connection = i.next();
            if (connection.appId == appId)
                currentConnections.add(connection);
        }
        return currentConnections;
    }

    /**
     * Erases all application context entries.
     */
    void clear() {
        synchronized (mApps) {
            Iterator<App> i = mApps.iterator();
            while(i.hasNext()) {
                App entry = i.next();
                entry.unlinkToDeath();
                i.remove();
            }
        }

        synchronized (mConnections) {
            mConnections.clear();
        }
    }
}

