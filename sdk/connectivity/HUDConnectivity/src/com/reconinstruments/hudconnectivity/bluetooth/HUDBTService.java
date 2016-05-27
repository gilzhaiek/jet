package com.reconinstruments.hudconnectivity.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.reconinstruments.hudconnectivity.IHUDConnectivity;

/**
 * Class that provides HUD Connectivity to phone and to the cloud via the phone
 * <p/>
 * The class serves both the HUD and the Phone
 */
public class HUDBTService {
    private final String TAG = this.getClass().getSimpleName();

    private static final boolean DEBUG = true;

    private BluetoothAdapter mBluetoothAdapter = null;
    private final IHUDConnectivity mHUDConnectivity;
    private final ArrayList<IHUDBTConsumer> mHUDBTConsumers = new ArrayList<IHUDBTConsumer>();

    private boolean mIsHUD = true;
    private boolean mForceBTEnable = false;
    private UUID mHUD2PhoneUUID = null;
    private UUID mPhone2HUDUUID = null;
    private String mSDPName = "NULL";

    public enum ConnectionState {
        CONNECTED,
        CONNECTING,
        DISCONNECTED,
    }

    public enum TransactionType {
        REQUEST,
        RESPONSE
    }

    private ConnectionState mState;

    /*
     * startListening -> mSecureAcceptThread -> connected -> ConnectedThread
     * connect -> mConnectThread -> connected -> ConnectedThread
     */
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private AcceptThread mSecureAcceptThread;

    /**
     * @param handler    A handler for notification from the HUDBTService
     * @param uniqueName A Unique Name for your application, for example: com.mycompany.myapp
     * @param uuid       Unique UUID for the application that uses this service
     * @param uuid       Unique UUID for the application that uses this service
     * @throws Exception
     */
    public HUDBTService(IHUDConnectivity hudConnectivity, boolean isHUD, boolean forceBTEnable, String uniqueName, UUID hud2PhoneUUID, UUID phone2hudUUID) throws Exception {
        if (hudConnectivity == null || uniqueName == null || hud2PhoneUUID == null || phone2hudUUID == null) {
            throw new NullPointerException("HUDBTService Constructor can't have null values");
        }

        mHUDConnectivity = hudConnectivity;
        mIsHUD = isHUD;
        mForceBTEnable = forceBTEnable;
        mSDPName = uniqueName;
        mHUD2PhoneUUID = hud2PhoneUUID;
        mPhone2HUDUUID = phone2hudUUID;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            throw new Exception("startListening: BluetoothAdapter.getDefaultAdapter() is null, is your Bluetooth Off?");
        }

        if (!mBluetoothAdapter.isEnabled() && mForceBTEnable) {
            mBluetoothAdapter.enable();
        }

        mState = ConnectionState.DISCONNECTED;
    }

    public void addConsumer(IHUDBTConsumer hudBTConsumer) {
        synchronized (mHUDBTConsumers) {
            if (mHUDBTConsumers.contains(hudBTConsumer)) {
                Log.w(TAG, "addConsumer - consumer exists already");
                return;
            }
            mHUDBTConsumers.add(hudBTConsumer);
        }
    }

    public void removeConsumer(IHUDBTConsumer hudBTConsumer) {
        synchronized (mHUDBTConsumers) {
            if (!mHUDBTConsumers.contains(hudBTConsumer)) {
                Log.w(TAG, "addConsumer - consumer does not exists");
                return;
            }
            mHUDBTConsumers.remove(hudBTConsumer);
        }
    }

    /**
     * @return the current connection state
     * <br>ConnectionState.CONNECTING
     * <br>ConnectionState.CONNECTED
     * <br>ConnectionState.DISCONNECTED
     */
    public synchronized ConnectionState getState() {
        return mState;
    }

    /**
     * Starts AcceptThread to begin a session in listening (server) mode.
     * <br>Usually called by the Activity onStart()
     *
     * @throws IOException
     */
    public synchronized void startListening() throws IOException {
        if (DEBUG) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread();
            mSecureAcceptThread.start();
        }
    }

    /**
     * Writes data to the ConnectedThread
     *
     * @param data The bytes to write
     * @throws IOException
     * @see ConnectedThread#write(byte[])
     */
    public synchronized void write(byte[] data, TransactionType transactionType) throws IOException {
        if (mState != ConnectionState.CONNECTED) {
            return;
        }
        mConnectedThread.write(data, transactionType);
    }

    public synchronized byte[] read(int length) throws IOException {
        if (mState != ConnectionState.CONNECTED) {
            return null;
        }
        return mConnectedThread.read(length);
    }

    /**
     * Writes a header to the ConnectedThread in an unsynchronized manner
     *
     * @param header The bytes to write
     * @return a response header if this is a sync message
     * @throws IOException
     * @see ConnectedThread#writeHeader(byte[])
     */
    public synchronized byte[] writeHeader(byte[] header, TransactionType transactionType) throws IOException {
        if (mState != ConnectionState.CONNECTED) {
            return null;
        }
        return mConnectedThread.writeHeader(header, transactionType);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param uuid
     * @param address The Address of the Bluetooth Device to connect
     * @param handler A Handler to send messages back to the UI Activity
     * @throws IOException
     */
    public synchronized void connect(String address) throws IOException {
        if (address == null) {
            throw new NullPointerException("Device address can't be null");
        }

        Log.d(TAG, "connect to: " + address);

        // Cancel any thread attempting to make a connection
        if (mState == ConnectionState.CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(mBluetoothAdapter.getRemoteDevice(address));
        mConnectThread.start();
        setState(ConnectionState.CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     * @throws IOException
     */
    public synchronized void connected(BluetoothDevice device, BluetoothSocket requestSocket, BluetoothSocket responseSocket) throws IOException {
        if (DEBUG) Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(requestSocket, responseSocket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        mHUDConnectivity.onDeviceName(device.getName());

        setState(ConnectionState.CONNECTED);
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(ConnectionState state) {
        if (DEBUG) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        mHUDConnectivity.onConnectionStateChanged(state);
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmRequestSocket;
        private final BluetoothSocket mmResponseSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) throws IOException {
            mmDevice = device;

            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            if (mIsHUD) {
                mmRequestSocket = device.createRfcommSocketToServiceRecord(mHUD2PhoneUUID);
                mmResponseSocket = device.createRfcommSocketToServiceRecord(mPhone2HUDUUID);
            } else {
                mmRequestSocket = device.createRfcommSocketToServiceRecord(mPhone2HUDUUID);
                mmResponseSocket = device.createRfcommSocketToServiceRecord(mHUD2PhoneUUID);
            }
        }

        @Override
        public void run() {
            if (DEBUG) Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a successful connection or an exception
                if (mIsHUD) {
                    mmRequestSocket.connect();
                    mmResponseSocket.connect();
                } else {
                    mmResponseSocket.connect();
                    mmRequestSocket.connect();
                }
            } catch (IOException e) {
                // Close the socket
                try {
                    mmRequestSocket.close();
                    mmResponseSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (HUDBTService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            try {
                connected(mmDevice, mmRequestSocket, mmResponseSocket);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread::run connected thread failed", e);
            }
        }

        public void cancel() {
            try {
                mmRequestSocket.close();
                mmResponseSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmRequestServerSocket;
        private final BluetoothServerSocket mmResponseServerSocket;

        public AcceptThread() throws IOException {
            if (mIsHUD) {
                mmRequestServerSocket = getBluetoothServerSocket(mHUD2PhoneUUID);
                mmResponseServerSocket = getBluetoothServerSocket(mPhone2HUDUUID);
            } else {
                mmRequestServerSocket = getBluetoothServerSocket(mPhone2HUDUUID);
                mmResponseServerSocket = getBluetoothServerSocket(mHUD2PhoneUUID);
            }
        }

        private BluetoothServerSocket getBluetoothServerSocket(UUID uuid) throws IOException {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(mSDPName, uuid);
            } catch (IOException e) {
                if (mForceBTEnable) {
                    mBluetoothAdapter.enable();
                } else {
                    throw new IOException("AcceptThread listenUsingRfcommWithServiceRecord() failed", e);
                }
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(mSDPName, uuid);
            }
            return tmp;
        }

        @Override
        public void run() {
            if (DEBUG) Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");

            BluetoothSocket reqSocket = null;
            BluetoothSocket respSocket = null;

            // Listen to the server socket if we're not connected
            while (mState != ConnectionState.CONNECTED) {
                try {
                    // This is a blocking call and will only return on a successful connection or an exception
                    if (mIsHUD) {
                        reqSocket = mmRequestServerSocket.accept();
                    } else {
                        respSocket = mmResponseServerSocket.accept();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "AcceptThread: accept() failed on " + (mIsHUD ? "reqSocket" : "respSocket"), e);
                    break;
                }

                try {
                    // This is a blocking call and will only return on a successful connection or an exception
                    if (mIsHUD) {
                        respSocket = mmResponseServerSocket.accept();
                    } else {
                        reqSocket = mmRequestServerSocket.accept();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "AcceptThread: accept() failed on " + (mIsHUD ? "respSocket" : "reqSocket"), e);
                    break;
                }

                // If a connection was accepted
                if (reqSocket != null && respSocket != null) {
                    synchronized (HUDBTService.this) {
                        switch (mState) {
                            case DISCONNECTED:
                            case CONNECTING:
                                // Situation normal. Start the connected thread.
                                try {
                                    connected(reqSocket.getRemoteDevice(), reqSocket, respSocket);
                                } catch (IOException e) {
                                    Log.e(TAG, "Couldn't get socket to provide a stream", e);
                                }
                                break;
                            case CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    reqSocket.close();
                                    respSocket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                } else {
                    Log.e(TAG, "AcceptThread::run one of the sockets is null");
                }
            }
            if (DEBUG) Log.i(TAG, "END mAcceptThread" + this);

        }

        public void cancel() {
            if (DEBUG) Log.d(TAG, "CANCEL " + this);
            try {
                mmRequestServerSocket.close();
                mmResponseServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmRequestSocket;
        private final BluetoothSocket mmResponseSocket;

        private final InputStream mmInRequestStream;
        private final OutputStream mmOutRequestStream;

        private final InputStream mmInResponseStream;
        private final OutputStream mmOutResponseStream;

        public ConnectedThread(BluetoothSocket requestSocket, BluetoothSocket responseSocket) throws IOException {
            Log.d(TAG, "CREATE ConnectedThread");

            mmRequestSocket = requestSocket;
            mmResponseSocket = responseSocket;

            InputStream tmpReqIn = null;
            OutputStream tmpReqOut = null;

            InputStream tmpResIn = null;
            OutputStream tmpResOut = null;

            // Get the BluetoothSocket input and output streams
            tmpReqIn = mmRequestSocket.getInputStream();
            tmpReqOut = mmRequestSocket.getOutputStream();

            tmpResIn = mmResponseSocket.getInputStream();
            tmpResOut = mmResponseSocket.getOutputStream();

            mmInRequestStream = tmpReqIn;
            mmOutRequestStream = tmpReqOut;

            mmInResponseStream = tmpResIn;
            mmOutResponseStream = tmpResOut;
        }

        private byte[] readStream(InputStream inStream, int length) throws IOException {
            byte[] buffer = new byte[length];

            int readLength = inStream.read(buffer, 0, length);
            int pos = 0;
            while (readLength != length) {
                length -= readLength;
                pos += readLength;
                readLength = inStream.read(buffer, pos, length);
            }

            return buffer;
        }

        @Override
        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            //int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    byte[] header = null;
                    byte[] payload = null;
                    byte[] body = null;

                    // Read from the InputStream
                    header = readStream(mmInResponseStream, HUDBTHeaderFactory.HEADER_LENGTH);
                    if (header.length != HUDBTHeaderFactory.HEADER_LENGTH) {
                        Log.e(TAG, "ConnectedThread::run Header expected to have " + HUDBTHeaderFactory.HEADER_LENGTH + " bytes, but got " + header.length + " bytes");
                        writeHeader(HUDBTHeaderFactory.getErrorHeader(), TransactionType.RESPONSE);
                        continue;
                    }

                    if (HUDBTHeaderFactory.hasPayload(header)) {
                        payload = readStream(mmInResponseStream, HUDBTHeaderFactory.getPayloadLength(header));
                        if (payload.length != HUDBTHeaderFactory.getPayloadLength(header)) {
                            Log.e(TAG, "Expected Payload to have " + HUDBTHeaderFactory.getPayloadLength(header) + " bytes but got " + payload.length + " bytes");
                        }

                    }

                    if (HUDBTHeaderFactory.hasBody(header)) {
                        body = readStream(mmInResponseStream, HUDBTHeaderFactory.getBodyLength(header));
                        if (body.length != HUDBTHeaderFactory.getBodyLength(header)) {
                            Log.e(TAG, "Expected Body to have " + HUDBTHeaderFactory.getBodyLength(header) + " bytes but got " + body.length + " bytes");
                        }
                    }

                    synchronized (mHUDBTConsumers) {
                        for (int i = 0; i < mHUDBTConsumers.size(); i++) {
                            if (mHUDBTConsumers.get(i).consumeBTData(header, payload, body)) {
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    // Start the service over to restart listening mode
                    // BluetoothChatService.this.start();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         * @throws IOException
         */
        public byte[] writeHeader(byte[] header, TransactionType transactionType) throws IOException {
            write(header, transactionType);

            if ((HUDBTHeaderFactory.getRequestHeaderType(header) == HUDBTHeaderFactory.REQUEST_HDR__ONEWAY) ||
                    (HUDBTHeaderFactory.getApplication(header) == HUDBTHeaderFactory.APPLICATION__WEB)) {
                return null;
            }

            try {
                if (transactionType == TransactionType.REQUEST) {
                    header = readStream(mmInRequestStream, HUDBTHeaderFactory.HEADER_LENGTH);
                } else {
                    header = readStream(mmInResponseStream, HUDBTHeaderFactory.HEADER_LENGTH);
                }

                if (header.length != HUDBTHeaderFactory.HEADER_LENGTH) {
                    throw new IllegalArgumentException("Header expected to be of length " + HUDBTHeaderFactory.HEADER_LENGTH + " bytes, got " + header.length + "bytes");
                }

                return header;
            } catch (IOException e) {
                e.printStackTrace();
                connectionLost();
            }
            return null;
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         * @throws IOException
         */
        public void write(byte[] buffer, TransactionType transactionType) throws IOException {
            if (transactionType == TransactionType.REQUEST) {
                mmOutRequestStream.write(buffer);
            } else {
                mmOutResponseStream.write(buffer);
            }
        }

        public byte[] read(int length) throws IOException {
            return readStream(mmInRequestStream, length);
        }

        public void cancel() {
            try {
                mmRequestSocket.close();
                mmResponseSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        setState(ConnectionState.DISCONNECTED);

        // Start the service over to restart listening mode
        try {
            HUDBTService.this.startListening();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        setState(ConnectionState.DISCONNECTED);

        // Start the service over to restart listening mode
        try {
            HUDBTService.this.startListening();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
