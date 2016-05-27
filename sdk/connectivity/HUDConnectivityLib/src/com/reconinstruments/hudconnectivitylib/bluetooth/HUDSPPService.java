package com.reconinstruments.hudconnectivitylib.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.reconinstruments.hudconnectivitylib.IHUDConnectivity;
import com.reconinstruments.hudconnectivitylib.IHUDConnectivity.ConnectionState;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Class that provides HUD Connectivity to phone and to the cloud via the phone
 * <p/>
 * The class serves both the HUD and the Phone
 */
public class HUDSPPService extends HUDBTBaseService {

    private static final String[] UUIDS = {
        "3007e231-e2af-4742-bcc4-70648bf22599",
        "798e999d-5fe8-4199-bc03-ab87f8545f1a",
        "5ed5a87f-15af-44c4-affc-9cbb686486e5",
        "c88436a2-0526-47c3-b365-c8519a5ea4e1"
    };

    private final static int SOCKETS_CNT = UUIDS.length; // This can be less if the developer wishes to

    private final UUID[] mUUIDs = new UUID[SOCKETS_CNT];

    private final String mSDPName = "HUDSPPService";

    public enum TransactionType {
        REQUEST,
        RESPONSE
    }

    /*
     * startListening -> mSecureAcceptThread -> connected -> ConnectedThread
     * connect -> mConnectThread -> connected -> ConnectedThread
     */
    private ConnectThread mConnectThread;
    private SPPOutStreamWriter mOutStreamWriter;
    private final InStreamThread[] mInStreamThreads;
    private AcceptThread mSecureAcceptThread;

    private class SPPOutStreamWriter extends OutStreamWriter {

        private final BluetoothSocket mmBTSockets[];

        public SPPOutStreamWriter(BluetoothSocket btSockets[]) throws IOException, InterruptedException {
            super(new OutputStreamContainer[btSockets.length], new ArrayBlockingQueue<OutputStreamContainer>(btSockets.length));
            mmBTSockets = btSockets;

            for (int i = 0; i < mmBTSockets.length; i++) {
                mOutputStreamPool[i] = new OutputStreamContainer(mFreeOutputStreamPool, mmBTSockets[i].getOutputStream());
                mFreeOutputStreamPool.put(mOutputStreamPool[i]);
            }
        }

        @Override
        public void write(OutputStreamContainer osContainer, byte[] buffer) throws IOException {
            osContainer.getOutputStream().write(buffer);
        }

        public void cancel() {
            try {
                for (int i = 0; i < mmBTSockets.length; i++) {
                    mmBTSockets[i].close();
                }
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    public HUDSPPService(IHUDConnectivity hudConnectivity) throws Exception {
        super(hudConnectivity);

        for (int i = 0; i < SOCKETS_CNT; i++) {
            mUUIDs[i] = UUID.fromString(UUIDS[i]);
        }

        mInStreamThreads = new InStreamThread[SOCKETS_CNT];
    }

    @Override
    public void addConsumer(IHUDBTConsumer hudBTConsumer) {
        super.addConsumer(hudBTConsumer);
    }

    @Override
    public void removeConsumer(IHUDBTConsumer hudBTConsumer) {
        super.removeConsumer(hudBTConsumer);
    }

    /**
     * Starts AcceptThread to begin a session in listening (server) mode.
     * <br>Usually called by the Activity onStart()
     *
     * @throws IOException
     */
    @Override
    public synchronized void startListening() throws IOException {
        if (DEBUG) Log.d(TAG, "startListening");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        for (int i = 0; i < mInStreamThreads.length; i++) {
            if (mInStreamThreads[i] != null) {
                mInStreamThreads[i] = null;
            }
        }

        if (mOutStreamWriter != null) {
            mOutStreamWriter.cancel();
            mOutStreamWriter = null;
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread();
            mSecureAcceptThread.start();
        } else {
            setState("startListening", ConnectionState.LISTENING);
        }
    }

    @Override
    public synchronized void stopListening() throws IOException {
        if (DEBUG) Log.d(TAG, "stopListening");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        for (int i = 0; i < mInStreamThreads.length; i++) {
            if (mInStreamThreads[i] != null) {
                mInStreamThreads[i] = null;
            }
        }

        if (mOutStreamWriter != null) {
            mOutStreamWriter.cancel();
            mOutStreamWriter = null;
        }
    }

    @Override
    public synchronized void write(OutputStreamContainer osContainer, byte[] buffer) throws Exception {
        if (mState != ConnectionState.CONNECTED) {
            throw new Exception("ConnectionState is not CONNECTED");
        }

        mOutStreamWriter.write(osContainer, buffer);
    }

    @Override
    public OutputStreamContainer obtainOutputStreamContainer() throws InterruptedException {
        return mOutStreamWriter.obtain();
    }

    @Override
    public void releaseOutputStreamContainer(OutputStreamContainer osContainer) {
        try {
            mOutStreamWriter.release(osContainer);
        } catch (InterruptedException e) {
            Log.wtf(TAG, "Couldn't release OutputStreamContainer", e);
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param uuid
     * @param address The Address of the Bluetooth Device to connect
     * @param handler A Handler to send messages back to the UI Activity
     * @throws IOException
     */
    @Override
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
        for (int i = 0; i < mInStreamThreads.length; i++) {
            if (mInStreamThreads[i] != null) {
                mInStreamThreads[i] = null;
            }
        }

        if (mOutStreamWriter != null) {
            mOutStreamWriter.cancel();
            mOutStreamWriter = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(mBluetoothAdapter.getRemoteDevice(address));
        mConnectThread.start();
        setState("connect", ConnectionState.CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     * @throws IOException
     * @throws InterruptedException
     */
    public synchronized void connected(BluetoothDevice device, BluetoothSocket btSockets[]) throws IOException, InterruptedException {
        if (DEBUG) Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        for (int i = 0; i < mInStreamThreads.length; i++) {
            if (mInStreamThreads[i] != null) {
                mInStreamThreads[i] = null;
            }
        }

        if (mOutStreamWriter != null) {
            mOutStreamWriter.cancel();
            mOutStreamWriter = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            Log.d(TAG, "connected will close the accept thread as a connection was performed from this device");
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        for (int i = 0; i < SOCKETS_CNT; i++) {
            mInStreamThreads[i] = new InStreamThread(btSockets[i].getInputStream(), i);
            mInStreamThreads[i].start();
        }
        mOutStreamWriter = new SPPOutStreamWriter(btSockets);

        // Send the name of the connected device back to the UI Activity
        mDeviceName = device.getName();
        mHUDConnectivity.onDeviceName(mDeviceName);

        setState("connected", ConnectionState.CONNECTED);
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final static int MAX_RETRIES = 3;
        private final BluetoothSocket mmBTSockets[] = new BluetoothSocket[SOCKETS_CNT];

        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;

            try {
                // Get a BluetoothSocket for a connection with the given BluetoothDevice
                for (int i = 0; i < SOCKETS_CNT; i++) {
                    mmBTSockets[i] = device.createRfcommSocketToServiceRecord(mUUIDs[i]);
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to createRfcommSocketToServiceRecord", e);
            }
        }

        public void connectSocket(BluetoothSocket aSocket, int attemptCnt) throws IOException {
            attemptCnt++;
            try {
                if (DEBUG) Log.d(TAG, "connectSocket attempt #" + attemptCnt);
                aSocket.connect();
                return;
            } catch (IOException e) {
                if (attemptCnt > MAX_RETRIES) {
                    throw e;
                }
                connectSocket(aSocket, attemptCnt);
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e1) {
                }
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
                for (int i = 0; i < SOCKETS_CNT; i++) {
                    if (DEBUG) Log.d(TAG, "connecting to socket #" + i);
                    connectSocket(mmBTSockets[i], 0);
                }
            } catch (IOException e) {
                Log.e(TAG, "Connect Failed", e);
                // Close the socket
                try {
                    for (int i = 0; i < SOCKETS_CNT; i++) {
                        mmBTSockets[i].close();
                    }
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed("ConnectThread::run");
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (HUDSPPService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            try {
                connected(mmDevice, mmBTSockets);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread::run connected thread failed IOException:", e);
            } catch (InterruptedException e) {
                Log.e(TAG, "ConnectThread::run connected thread failed InterruptedException:", e);
            }
        }

        public void cancel() {
            try {
                for (int i = 0; i < SOCKETS_CNT; i++) {
                    mmBTSockets[i].close();
                }
            } catch (IOException e) {
                Log.e(TAG, "close() of connect HUD socket failed", e);
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

        private final BluetoothServerSocket mmBTServerSocket[] = new BluetoothServerSocket[SOCKETS_CNT];

        public AcceptThread() throws IOException {
            for (int i = 0; i < SOCKETS_CNT; i++) {
                mmBTServerSocket[i] = getBluetoothServerSocket("H_" + mSDPName + "_" + i, mUUIDs[i]);
            }
        }

        private BluetoothServerSocket getBluetoothServerSocket(String sdpName, UUID uuid) throws IOException {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(sdpName, uuid);
            } catch (IOException e) {
                if (FORCE_BT_ENABLE) {
                    mBluetoothAdapter.enable();
                } else {
                    throw new IOException("AcceptThread listenUsingRfcommWithServiceRecord() failed", e);
                }
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(sdpName, uuid);
            }
            return tmp;
        }

        @Override
        public void run() {
            if (DEBUG) Log.d(TAG, "BEGIN mAcceptThread");
            setName("AcceptThread");

            BluetoothSocket sockets[] = new BluetoothSocket[SOCKETS_CNT];

            // Listen to the server socket if we're not connected
            while (mState != ConnectionState.CONNECTED) {
                setState("AcceptThread:run", ConnectionState.LISTENING);
                try {
                    // This is a blocking call and will only return on a successful connection or an exception
                    for (int i = 0; i < SOCKETS_CNT; i++) {
                        if (DEBUG) Log.d(TAG, "Waiting to accept socket #" + i);
                        sockets[i] = mmBTServerSocket[i].accept();
                        if (DEBUG) Log.d(TAG, "Accepted socket #" + i);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "AcceptThread: accept() failed on accepting socket (OK if this device initated a connection)", e);
                    cancel();
                    break;
                }

                // If a connection was accepted
                synchronized (HUDSPPService.this) {
                    switch (mState) {
                        case LISTENING:
                        case DISCONNECTED:
                        case CONNECTING:
                            // Situation normal. Start the connected thread.
                            try {
                                connected(sockets[0].getRemoteDevice(), sockets);
                            } catch (IOException e) {
                                Log.e(TAG, "Couldn't get socket to provide a stream IOException:", e);
                            } catch (InterruptedException e) {
                                Log.e(TAG, "Couldn't get socket to provide a stream InterruptedException:", e);
                            }
                            break;
                        case CONNECTED:
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                for (int i = 0; i < SOCKETS_CNT; i++) {
                                    sockets[i].close();
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                    }
                }
            }
            if (DEBUG) Log.i(TAG, "END mAcceptThread" + this);
        }

        public void cancel() {
            if (DEBUG) Log.d(TAG, "CANCEL " + this);
            try {
                for (int i = 0; i < SOCKETS_CNT; i++) {
                    mmBTServerSocket[i].close();
                }
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }

            if (mState == ConnectionState.LISTENING) {
                setState("AcceptThread:cancel", ConnectionState.DISCONNECTED);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class InStreamThread extends Thread {
        private final InputStream mmInStream;
        private final int mSessionID;

        public InStreamThread(InputStream inStream, int sessionID) throws IOException {
            Log.d(TAG, "CREATE ConnectedThread with SessionID:" + sessionID);

            mmInStream = inStream;
            mSessionID = sessionID;
        }

        @Override
        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");

            byte[] data = new byte[1024];
            ExcessDataAgent excessDataAgent = new ExcessDataAgent();
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    HUDBTMessage hudBTRequest = null;
                    bytes = mmInStream.read(data);
                    if (bytes > 0) {
                        hudBTRequest = HUDBTMessageCollectionManager.addData(mSessionID, data, bytes, excessDataAgent);
                    }
                    consume(hudBTRequest);

                    while(excessDataAgent.hasValidData()){
                        hudBTRequest = HUDBTMessageCollectionManager.addData(mSessionID, excessDataAgent.getData(), excessDataAgent.getBytes(), excessDataAgent);
                        consume(hudBTRequest);
                    }
                    
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost("InStreamThread::run");
                    // Start the service over to restart listening mode
                    // BluetoothChatService.this.start();
                    break;
                }
            }
        }

        private void consume(HUDBTMessage hudBTRequest){
            if (hudBTRequest != null) {
                if(DEBUG) Log.d(TAG,"Consume HUDBTMessage");
                synchronized (mHUDBTConsumers) {
                    for (int i = 0; i < mHUDBTConsumers.size(); i++) {
                        if (mHUDBTConsumers.get(i).consumeBTData(hudBTRequest.getHeader(), hudBTRequest.getPayload(), hudBTRequest.getBody())) {
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private synchronized void connectionFailed(String parentFunction) {
        if (DEBUG) Log.d(TAG, "connectionFailed (called from " + parentFunction + ")");
        if (mState != ConnectionState.LISTENING) {
            setState(parentFunction, ConnectionState.DISCONNECTED);

            // Start the service over to restart listening mode
            try {
                HUDSPPService.this.startListening();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private synchronized void connectionLost(String parentFunction) {
        if (DEBUG) Log.d(TAG, "connectionLost (called from " + parentFunction + ")");
        if (mState != ConnectionState.LISTENING) {
            setState(parentFunction, ConnectionState.DISCONNECTED);

            // Start the service over to restart listening mode
            try {
                HUDSPPService.this.startListening();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
