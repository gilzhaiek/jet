package com.reconinstruments.mfiservice;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.BluetopiaPMException;
import com.stonestreetone.bluetopiapm.SPPM;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortClientManager;
import com.stonestreetone.bluetopiapm.ServerNotReachableException;
import com.reconinstruments.mfi.*;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class MFiServiceImpl extends IMFiService.Stub {
    private static final String TAG = "MFiServiceImpl";

    private boolean DEBUG = true;

    private static final int FLUSH_TIME_OUT = 5000;
    private static final int DEFAULT_PORT_NUMBER = 1;
    private static final int BUFFSIZE = 8192;
    private static final int DATA_PACKET_TIMEOUT = 800;
    private static final String BUNDLE_SEED_ID = "12345ABCDE";
    private static final String CURRENT_LANGUAGE = "en";

    private static int MAX_OUTPUT_STREAM_SIZE = 60000;
    private static int LOOP_BUFFER_SIZE = 10000;

    private String HUD_CONNECTIVITY_UUID = "f4085309-5b87-49f7-ba2c-dd3a3a4ef4de"; // 4 Channels
    private String HUD_SERVICE_UUID = "48b0b236-547d-4b41-98bc-fcacaab31d00"; // 3 Channels Obj, Cmd, File

    private EnumSet<SPPM.SerialPortServerManager.IncomingConnectionFlags> mIncomingConnectionFlags = EnumSet.noneOf(SPPM.SerialPortServerManager.IncomingConnectionFlags.class);

    private EnumSet<SPPM.SerialPortClientManager.ConnectionFlags> mConnectionFlags = EnumSet.noneOf(SPPM.SerialPortClientManager.ConnectionFlags.class);

    private boolean mServiceStarted = false;

    private BTMFiEventCallback mServerEventCallback = null;
    private BTMFiEventCallback mClientEventCallback = null;

    private SPPM.SerialPortClientManager mSerialPortClientManager;
    private SPPM.SerialPortServerManager mSerialPortServerManager;

    private final Map<IBinder, MFiListenerTracker> mMFiListeners = new HashMap<IBinder, MFiListenerTracker>();

    private Object mStartStopSync = new Object();

    private Context mContext;

    public enum ConnectionState {
        LISTENING,
        CONNECTED,
        CONNECTING,
        DISCONNECTED,
    }

    public enum ConnectingRoute {
        NO_ROUTE,
        CLIENT,
        SERVER
    }

    protected String mDeviceName = "NULL";
    private String mAddress = null;
    private ConnectionState mState = ConnectionState.DISCONNECTED;
    private ConnectingRoute mConnectingRoute = ConnectingRoute.NO_ROUTE;

    public MFiServiceImpl(Context context) throws Exception {
        mContext = context;
    }

    public void start(String listenerUUID, IMFiServiceListener listener) throws RemoteException {
        synchronized (mStartStopSync) {
            if (mServiceStarted) {
                Log.w(TAG, "Service already started..., not starting again");
                registerMFiListener(listenerUUID, listener);
                return;
            }
            mClientEventCallback = new BTMFiEventCallback("Client", mHandler);
            mServerEventCallback = new BTMFiEventCallback("Server", mHandler);

            try {
                openSerialPortManagers();
            } catch (Exception e) {
                Log.e(TAG, "Couldn't openSerialPortManagers", e);
                throw new RemoteException("start: Couldn't openSerialPortManagers");
            }

            Log.d(TAG, "MFiServiceImpl started success");
            mServiceStarted = true;
            registerMFiListener(listenerUUID, listener);
        }
    }

    public void stop(String listenerUUID, IMFiServiceListener listener) {
        synchronized (mStartStopSync) {
            Log.d(TAG, "Stopping Serial Port Managers...");

            if (mSerialPortServerManager != null) {
                mSerialPortServerManager.dispose();
                mSerialPortServerManager = null;
                Log.d(TAG, "Close Server result: Success");
            } else {
                Log.w(TAG, "Close Server result: No server is currently active.");
            }

            if (mSerialPortClientManager != null) {
                mSerialPortClientManager.dispose();
                mSerialPortClientManager = null;
                Log.d(TAG, "Close Client result: Success");
            } else {
                Log.w(TAG, "Close Client result: No client is currently active.");
            }

            unregisterMFiListener(listenerUUID, listener);
            mServiceStarted = false;
        }
    }

    /**
     * @return the current connection state
     * <br>ConnectionState.LISTENING
     * <br>ConnectionState.CONNECTED
     * <br>ConnectionState.CONNECTING
     * <br>ConnectionState.DISCONNECTED
     */
    public synchronized ConnectionState getState() {
        return mState;
    }

    protected synchronized void setState(String parentFunction, ConnectionState state) {
        if (DEBUG) Log.d(TAG, "setState(" + parentFunction + ") " + mState + " -> " + state);
        mState = state;
    }

    private synchronized void setConnectingRoute(ConnectingRoute route) {
        if (DEBUG) Log.d(TAG, "Connecting Route : " + route.toString());
        mConnectingRoute = route;
    }

    private synchronized ConnectingRoute getConnectingRoute() {
        return mConnectingRoute;
    }

    public String getDeviceName() throws RemoteException {
        return mDeviceName;
    }

    public String getLastConnectedBTAddress() throws RemoteException {
        return mAddress;
    }

    public boolean isConnectingOrConnected() throws RemoteException {
        return (getState() == ConnectionState.CONNECTING) || (getState() == ConnectionState.CONNECTED);
    }

    public int getSessionID(int protocolIndex) throws RemoteException {
        return MFiInfo.getSessionID(protocolIndex);
    }

    private void openSerialPortManagers() throws Exception {
        // Open the Client Manager
        if (DEBUG) Log.d(TAG, "Opening and configuring the SerialPortClientManager");
        if (mSerialPortClientManager == null) {
            try {
                mSerialPortClientManager = new SPPM.SerialPortClientManager(mClientEventCallback);
            } catch (Exception e) {
                Log.e(TAG, "MFiServiceImpl: Could not connect to the BluetopiaPM service", e);
                throw e;
            }
        }
        configureMFiSettings(mSerialPortClientManager);
        if (DEBUG) Log.d(TAG, "SerialPortClientManager is now configured and open");

        // Open the Server Manager
        if (DEBUG) Log.d(TAG, "Opening and configuring the SerialPortServerManager");
        if (mSerialPortServerManager == null) {
            try {
                mIncomingConnectionFlags.add(SPPM.SerialPortServerManager.IncomingConnectionFlags.REQUIRE_AUTHENTICATION);
                mIncomingConnectionFlags.add(SPPM.SerialPortServerManager.IncomingConnectionFlags.REQUIRE_ENCRYPTION);
                mIncomingConnectionFlags.add(SPPM.SerialPortServerManager.IncomingConnectionFlags.MFI_REQUIRED);

                mSerialPortServerManager = new SPPM.SerialPortServerManager(mServerEventCallback, mIncomingConnectionFlags);
            } catch (ServerNotReachableException e) {
                Log.e(TAG, "Open Server result: Unable to communicate with Platform Manager service", e);
                throw e;
            } catch (BluetopiaPMException e) {
                Log.e(TAG, "Open Server result: Unable to register server port (already in use?)", e);
                throw e;
            }
        }
        configureMFiSettings(mSerialPortServerManager);
        if (DEBUG) Log.d(TAG, "mSerialPortServerManager is now configured and open");
    }

    private void configureMFiSettings(SPPM serialPortProfileManager) throws Exception {
        int result = serialPortProfileManager.configureMFiSettings(
                BUFFSIZE,
                DATA_PACKET_TIMEOUT,
                null,
                MFiInfo.mMFiAccessoryInfo,
                MFiInfo.mMFiProtocols,
                BUNDLE_SEED_ID,
                null,
                CURRENT_LANGUAGE,
                null);
        if (result != 0) {
            Log.e(TAG, "configureMFiSettings failed, error #" + result);
            throw new Exception("configureMFiSettings failed, error #" + result);
        }
    }

    public void connect(String address) throws RemoteException {
        Log.d(TAG, "connect to: " + address);
        if (address == null) {
            throw new NullPointerException("Device address can't be null");
        }

        if (getState() == ConnectionState.CONNECTED) {
            if (mAddress != address) { // Need to disconnect
                disconnect();
            } else {
                Log.d(TAG, "MFi is already connected to " + address);
                return;
            }
        }

        mAddress = address;

        if (getState() == ConnectionState.CONNECTING) {
            Log.w(TAG, "Already trying to connect, ignoring request");
            return;
        }

        setState("connect(" + mAddress + ")", ConnectionState.CONNECTING);
        new AsyncTask<Object, Void, Integer>() {
            @Override
            protected Integer doInBackground(Object... params) {
                BluetoothAddress bluetoothAddress = new BluetoothAddress((String) params[0]);
                SerialPortClientManager serialPortClientManager = (SerialPortClientManager) params[1];

                mConnectionFlags.add(SerialPortClientManager.ConnectionFlags.REQUIRE_AUTHENTICATION);
                mConnectionFlags.add(SerialPortClientManager.ConnectionFlags.REQUIRE_ENCRYPTION);
                mConnectionFlags.add(SerialPortClientManager.ConnectionFlags.MFI_REQUIRED);

                return serialPortClientManager.connectRemoteDevice(bluetoothAddress, DEFAULT_PORT_NUMBER, mConnectionFlags, false);
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result != 0) {
                    onConnected(mAddress, false);
                    Log.e(TAG, "connect(" + mAddress + ") failed, error #" + result);
                    setState("connect", ConnectionState.DISCONNECTED);
                    disconnect();
                }
            }
        }.execute(address, mSerialPortClientManager);
    }

    /**
     * Close bluetooth connection
     */
    public void disconnect() {
        closeSession();
        int result = -1;

        //It's normal to have negative result because only one manager is connected
        //result = 0 only if SerialPortManager was connected, otherwise negative value
        result = mSerialPortClientManager.disconnectRemoteDevice(FLUSH_TIME_OUT);
        Log.d(TAG, "disconnectRemoteDevice() SerialPortClientManager result: " + result);

        result = mSerialPortServerManager.disconnectRemoteDevice(FLUSH_TIME_OUT);
        Log.d(TAG, "disconnectRemoteDevice() SerialPortServerManager result: " + result);

        setState("disconnect()", ConnectionState.DISCONNECTED);
        setConnectingRoute(ConnectingRoute.NO_ROUTE);

        synchronized (this.mMFiListeners) {
            // Go through all registered listeners and report the event.
            for (Map.Entry<IBinder, MFiListenerTracker> entry : this.mMFiListeners.entrySet()) {
                IMFiServiceListener listener = entry.getValue().getListener();
                try {
                    if (DEBUG) Log.d(TAG, "disconnect: Notifying MFi listener: " + entry.getKey());
                    listener.onDisconnected();
                } catch (RemoteException e) {
                    Log.e(TAG, "disconnect: Failed to update MFi listener: " + entry.getKey(), e);
                    this.unregisterMFiListener(entry.getValue().getUUID(), listener);
                }
            }
        }
    }

    /**
     * Find the Name of the bluetooth unit and notify HUDConnectivity
     *
     * @param bluetoothAddress address of the paired bluetooth unit
     * @param success          if onConnected success or failed
     */
    private void onConnected(String bluetoothAddress, boolean success) {
        mDeviceName = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(bluetoothAddress).getName();
        setState("onConnected()", ConnectionState.CONNECTING);
        synchronized (this.mMFiListeners) {
            // Go through all registered listeners and report the event.
            for (Map.Entry<IBinder, MFiListenerTracker> entry : this.mMFiListeners.entrySet()) {
                IMFiServiceListener listener = entry.getValue().getListener();
                try {
                    if (DEBUG)
                        Log.d(TAG, "onDeviceName: Notifying MFi listener: " + entry.getKey());
                    listener.onConnected(mDeviceName, bluetoothAddress, success);
                } catch (RemoteException e) {
                    Log.e(TAG, "onDeviceName: Failed to update MFi listener: " + entry.getKey(), e);
                    this.unregisterMFiListener(entry.getValue().getUUID(), listener);
                }
            }
        }
    }

    private void closeSession() {
        MFiInfo.clearSessionIDs();

        synchronized (this.mMFiListeners) {
            // Go through all registered listeners and report the event.
            for (Map.Entry<IBinder, MFiListenerTracker> entry : this.mMFiListeners.entrySet()) {
                IMFiServiceListener listener = entry.getValue().getListener();
                try {
                    if (DEBUG)
                        Log.d(TAG, "onSessionClosed: Notifying MFi listener: " + entry.getKey());
                    listener.onSessionClosed();
                } catch (RemoteException e) {
                    Log.e(TAG, "onSessionClosed: Failed to update MFi listener: " + entry.getKey(), e);
                    this.unregisterMFiListener(entry.getValue().getUUID(), listener);
                }
            }
        }

        if (getState() == ConnectionState.CONNECTED) {
            setState("onSessionClosed()", ConnectionState.CONNECTING);
        }
    }

    private IMFiServiceListener getMFiListener(String listenerUUID) {
        synchronized (this.mMFiListeners) {
            // Go through all registered listeners and report the event.
            for (Map.Entry<IBinder, MFiListenerTracker> entry : this.mMFiListeners.entrySet()) {
                if (listenerUUID.equals(entry.getValue().getUUID())) {
                    return entry.getValue().getListener();
                }
            }
        }
        return null;
    }


    private void registerMFiListener(String listenerUUID, IMFiServiceListener listener) throws RemoteException {
        if (listener == null) {
            return;
        }

        if (!listenerUUID.equals(HUD_SERVICE_UUID) && !listenerUUID.equals(HUD_CONNECTIVITY_UUID)) {
            Log.e(TAG, "Unknown UUID " + listenerUUID);
        }


        IBinder binder = listener.asBinder();
        synchronized (this.mMFiListeners) {
            if (this.mMFiListeners.containsKey(binder)) {
                Log.w(TAG, "Ignoring duplicate MFi listener: " + listener);
            } else {
                MFiListenerTracker listenerTracker = new MFiListenerTracker(listenerUUID, listener);
                binder.linkToDeath(listenerTracker, 0);
                this.mMFiListeners.put(binder, listenerTracker);
                if (DEBUG) Log.d(TAG, "Adding MFi listener: " + listener);
            }
        }
    }

    private void unregisterMFiListener(String listenerUUID, IMFiServiceListener listener) {
        if (listener == null) {
            return;
        }

        if (!listenerUUID.equals(HUD_SERVICE_UUID) && !listenerUUID.equals(HUD_CONNECTIVITY_UUID)) {
            Log.e(TAG, "Unknown UUID " + listenerUUID);
            return;
        }

        if (listener != null) {
            IBinder binder = listener.asBinder();
            synchronized (this.mMFiListeners) {
                MFiListenerTracker listenerTracker = this.mMFiListeners.remove(binder);
                if (listenerTracker == null) {
                    Log.w(TAG, "Ignoring remove MFi listener: " + binder);
                } else {
                    if (DEBUG) Log.d(TAG, "Remove listener: " + binder);
                    binder.unlinkToDeath(listenerTracker, 0);
                }
            }
        }
    }

    private final class MFiListenerTracker implements IBinder.DeathRecipient {
        private final String listenerUUID;
        private final IMFiServiceListener listener;

        public MFiListenerTracker(String listenerUUID, IMFiServiceListener listener) {
            this.listenerUUID = listenerUUID;
            this.listener = listener;
        }

        public String getUUID() {
            return this.listenerUUID;
        }

        public IMFiServiceListener getListener() {
            return this.listener;
        }

        public void binderDied() {
            MFiServiceImpl.this.unregisterMFiListener(this.listenerUUID, this.listener);
        }
    }

    public void writeData(final int sessionID, byte[] data) throws RemoteException {
        if (getState() != ConnectionState.CONNECTED) {
            throw new RemoteException("writeData: MFi is not connected");
        }

        SPPM manager = getSerialPortManager();
        if (manager == null) {
            String errorMsg = "writeData() State show connected but serial port manager is null";
            Log.e(TAG, errorMsg);
            throw new RemoteException(errorMsg);
        }

        if (data == null) {
            String errorMsg = "writeData() data tries to send is null";
            Log.e(TAG, errorMsg);
            throw new RemoteException(errorMsg);
        }

        Log.d(TAG, "Sending session data to iOS device, sessionID: " + sessionID + " data.length: " + data.length);
        int packetID = manager.sendSessionData(sessionID, data);
        if (packetID >= 0) {
            Log.d(TAG, "Sent session data to iOS device, sessionsID: " + sessionID + " packetID: " + packetID);
        } else {
            //TODO here need to catch for error code
            // we reconnect here since we can't send packet out.
            // if (sendingPacketId == -10087)
            // ?? why reconnect we reconnect here since we can't send packet out.
            Log.w(TAG, "Sent session data to iOS device, packetId: " + packetID);
        }

        int protocolIndex = MFiInfo.getProtocolIndex(sessionID);
        if (protocolIndex < 0) {
            Log.e(TAG, "Unknown SessionID " + sessionID);
            return;
        }

        IMFiServiceListener listener = null;
        String hudUUID = "";
        if (MFiInfo.isHUDConnectivityProtocol(protocolIndex)) {
            hudUUID = HUD_CONNECTIVITY_UUID;
        } else {
            hudUUID = HUD_SERVICE_UUID;
        }
        listener = getMFiListener(hudUUID);

        if (listener != null) {
            try {
                listener.onPacketID(packetID);
            } catch (RemoteException e) {
                Log.e(TAG, "writeData: Failed to update  MFi listener", e);
                this.unregisterMFiListener(hudUUID, listener);
            }
        }
    }

    protected void sessionOpenRequestEvent(int sessionID, int protocolIndex) {
        SPPM manager = getSerialPortManager();
        int result = -1;
        if (manager == null) {
            String errorMsg = "sessionOpenRequestEvent() sessionID:" + sessionID + "protocolIndex: " + protocolIndex + " serial port manager is null";
            Log.e(TAG, errorMsg);
            return;
        }
        result = manager.openSessionRequestResponse(sessionID, true); // Returns Zero for success
        MFiInfo.setSessionID(protocolIndex, sessionID);

        IMFiServiceListener listener = null;
        String hudUUID = "";
        if (MFiInfo.isHUDConnectivityProtocol(protocolIndex)) {
            hudUUID = HUD_CONNECTIVITY_UUID;
        } else {
            hudUUID = HUD_SERVICE_UUID;
        }
        listener = getMFiListener(hudUUID);

        if ((result == 0) && (getState() == ConnectionState.CONNECTING)) {
            setState("sessionOpenRequestEvent()", ConnectionState.CONNECTED);
        }

        if (listener != null) {
            try {
                listener.onSessionOpened(sessionID, protocolIndex, (result == 0));
            } catch (RemoteException e) {
                Log.e(TAG, "sessionOpenRequestEvent: Failed to update MFi listener", e);
                this.unregisterMFiListener(hudUUID, listener);
            }
        }
    }

    /**
     * Get the serial port manager we are using to communicate to iphone
     *
     * @return the SPPM which we are using to communicate, null if there is none
     */
    private SPPM getSerialPortManager() {
        switch (mConnectingRoute) {
            case CLIENT:
                return mSerialPortClientManager;
            case SERVER:
                return mSerialPortServerManager;
            default:
                return null;
        }
    }

    protected void receivedData(int sessionID, byte[] sessionData) {
        int protocolIndex = MFiInfo.getProtocolIndex(sessionID);
        if (protocolIndex < 0) {
            Log.e(TAG, "Unknown SessionID " + sessionID);
            return;
        }
        if (MFiInfo.isHUDConnectivityProtocol(protocolIndex)) {
            IMFiServiceListener listener = getMFiListener(HUD_CONNECTIVITY_UUID);
            if (listener != null) {
                try {
                    listener.onReceivedData(sessionID, sessionData);
                } catch (RemoteException e) {
                    Log.e(TAG, "receivedData: Failed to update HUD_CONNECTIVITY_UUID MFi listener", e);
                    this.unregisterMFiListener(HUD_CONNECTIVITY_UUID, listener);
                }
            }
        } else {
            IMFiServiceListener listener = getMFiListener(HUD_SERVICE_UUID);
            if (listener != null) {
                try {
                    listener.onReceivedData(sessionID, sessionData);
                } catch (RemoteException e) {
                    Log.e(TAG, "receivedData: Failed to update HUD_SERVICE_UUID MFi listener", e);
                    this.unregisterMFiListener(HUD_SERVICE_UUID, listener);
                }
            }
        }
    }

    protected void sessionDataConfirmationEvent(int sessionID, int packetID, SPPM.DataConfirmationStatus status) {
        MFiInfo.getProtocolIndex(sessionID);

        int protocolIndex = MFiInfo.getProtocolIndex(sessionID);
        if (protocolIndex < 0) {
            Log.e(TAG, "Unknown SessionID " + sessionID);
            return;
        }
        if (MFiInfo.isHUDConnectivityProtocol(protocolIndex)) {
            IMFiServiceListener listener = getMFiListener(HUD_CONNECTIVITY_UUID);
            if (listener != null) {
                try {
                    listener.onSessionDataConfirmationEvent(sessionID, packetID, status.ordinal());
                } catch (RemoteException e) {
                    Log.e(TAG, "receivedData: Failed to update HUD_CONNECTIVITY_UUID MFi listener", e);
                    this.unregisterMFiListener(HUD_CONNECTIVITY_UUID, listener);
                }
            }
        } else {
            IMFiServiceListener listener = getMFiListener(HUD_SERVICE_UUID);
            if (listener != null) {
                try {
                    listener.onSessionDataConfirmationEvent(sessionID, packetID, status.ordinal());
                } catch (RemoteException e) {
                    Log.e(TAG, "receivedData: Failed to update HUD_SERVICE_UUID MFi listener", e);
                    this.unregisterMFiListener(HUD_SERVICE_UUID, listener);
                }
            }
        }
    }

    protected void establishedConnectionFromHUDEvent(boolean successful) {
        //deviceName and bluetoothAddress was already set during connect(String address)
        if (successful) {
            setConnectingRoute(ConnectingRoute.CLIENT);
            onConnected(mAddress, true);
        } else {
            onConnected(mAddress, false);
            setConnectingRoute(ConnectingRoute.NO_ROUTE);
            setState("failed establishedConnectionFromHUD", ConnectionState.DISCONNECTED);
            Log.e(TAG, "establishedConnectionFromHUDEvent(" + mAddress + "), failed");
        }
    }

    protected void establishedConnectionFromIphoneEvent(String bluetoothAddress) {
        mAddress = bluetoothAddress;
        setConnectingRoute(ConnectingRoute.SERVER);
        onConnected(bluetoothAddress, true);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BTMFiEventCallback.SESSION_DATA_RECEIVED_EVENT:
                    receivedData(msg.arg1, (byte[]) msg.obj);
                    break;
                case BTMFiEventCallback.SESSION_DATA_CONFIRMATION_EVENT:
                    sessionDataConfirmationEvent(msg.arg1, msg.arg2, (SPPM.DataConfirmationStatus) msg.obj);
                    break;
                case BTMFiEventCallback.FROM_HUD_CONNECTED_EVENT:
                    establishedConnectionFromHUDEvent(msg.arg1 == 1 ? true : false);
                    break;
                case BTMFiEventCallback.FROM_IPHONE_CONNECTED_EVENT:
                    establishedConnectionFromIphoneEvent((String) msg.obj);
                    break;
                case BTMFiEventCallback.DISCONNECTED_EVENT:
                    disconnect();
                    break;
                case BTMFiEventCallback.SESSION_OPEN_REQUEST_EVENT:
                    sessionOpenRequestEvent(msg.arg1, msg.arg2);
                    break;
                case BTMFiEventCallback.SESSION_CLOSE_EVENT:
                    closeSession();
                    break;
            }
        }
    };
}
