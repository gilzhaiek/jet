package com.reconinstruments.os.connectivity.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.util.SparseArray;

import com.reconinstruments.os.connectivity.IHUDConnectivity;
import com.reconinstruments.os.connectivity.IHUDConnectivity.ConnectionState;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Class that provides HUD Connectivity to phone and to the cloud via the phone
 * <p/>
 * The class serves both the HUD and the Phone
 * {@hide}
 */
public class HUDSPPService extends HUDBTBaseService {

    /* UUIDs reserved for connectivity */
    private static final String[] UUIDS = {
        "3007e231-e2af-4742-bcc4-70648bf22599",
        "798e999d-5fe8-4199-bc03-ab87f8545f1a",
        "5ed5a87f-15af-44c4-affc-9cbb686486e5",
        "c88436a2-0526-47c3-b365-c8519a5ea4e1"
    };

    private static final int MAX_SOCKET_CNT = UUIDS.length;
    /** This can be less if the developer wishes to using constructor {@link #HUDSPPService(IHUDConnectivity, int)} */
    private static int SOCKETS_CNT = MAX_SOCKET_CNT;

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
    private ConnectThread[] mConnectThreads;
    private SPPOutStreamWriter mOutStreamWriter;
    private SparseArray<InStreamThread> mInStreamThreads = new SparseArray<InStreamThread>();
    private AcceptThread[] mSecureAcceptThreads;

    private class SPPOutStreamWriter extends OutStreamWriter {

        private final Collection<BluetoothSocket> mBTSockets = new ArrayList<BluetoothSocket>();

        public SPPOutStreamWriter(int numSockets){
            super(new ArrayBlockingQueue<OutputStreamContainer>(numSockets));
        }

        public void addSocketToPool(BluetoothSocket socket) throws IOException{
            OutputStreamContainer outputStreamContainer = new OutputStreamContainer(socket.getOutputStream());
            mBTSockets.add(socket);
            mFreeOutputStreamPool.offer(outputStreamContainer);
        }

        public void removeSocketFromPool(BluetoothSocket socket){
            mBTSockets.remove(socket);
            mFreeOutputStreamPool.remove(socket);
        }

        @Override
        public void write(OutputStreamContainer osContainer, byte[] buffer) throws IOException {
            osContainer.getOutputStream().write(buffer);
        }

        public void cancel() {
            try {
                for (BluetoothSocket socket : mBTSockets) {
                    socket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
            mFreeOutputStreamPool.clear();
            mBTSockets.clear();
        }
    }

    /**
     * Construct with the default number of sockets - {@link #MAX_SOCKET_CNT}
     * @param hudConnectivity
     * @throws Exception
     */
    public HUDSPPService(IHUDConnectivity hudConnectivity) throws Exception {
        super(hudConnectivity);
        init(MAX_SOCKET_CNT);
    }

    /**
     * @param hudConnectivity
     * @param socketCount number of bluetooth sockets between 1 and {@link #MAX_SOCKET_CNT}
     * @throws Exception
     */
    public HUDSPPService(IHUDConnectivity hudConnectivity, int socketCount) throws Exception {
        super(hudConnectivity);
        init(socketCount);
    }

    /**
     * Initialize with the number of sockets to connect on. Default is {@link #MAX_SOCKET_CNT}
     * @param socketCount must be between 0 and {@link #MAX_SOCKET_CNT}
     */
    private void init(int socketCount) throws IllegalArgumentException{
        if (socketCount>MAX_SOCKET_CNT || socketCount<0){
            throw new IllegalArgumentException("Count must be between 0 and" + MAX_SOCKET_CNT);
        }else{
            SOCKETS_CNT = socketCount;
        }

        for (int i = 0; i < SOCKETS_CNT; i++) {
            mUUIDs[i] = UUID.fromString(UUIDS[i]);
        }

        mInStreamThreads = new SparseArray<InStreamThread>();
        mOutStreamWriter = new SPPOutStreamWriter(SOCKETS_CNT);
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
        cancelConnectThreads();

        // Cancel any thread currently running a connection
        cancelInStreamThreads();

        mOutStreamWriter.cancel();

        // Start the thread to listen on a BluetoothServerSocket
        cancelAcceptThreads();
        startAcceptThreads();

        setState("startListening", ConnectionState.LISTENING);
    }

    private void startAcceptThreads(){
        mSecureAcceptThreads = new AcceptThread[SOCKETS_CNT];
        for (int i=0; i<mSecureAcceptThreads.length; i++){
            try{
                mSecureAcceptThreads[i] = new AcceptThread(i);
                mSecureAcceptThreads[i].start();
            }catch (IOException e){
                Log.e(TAG, "Failed to create AcceptThread #" +i, e);
            }
        }
    }

    private void cancelInStreamThreads(){
        for (int i = 0; i < mInStreamThreads.size(); i++) {
            if (mInStreamThreads.get(i) != null) {
                mInStreamThreads.get(i).cancel();
            }
        }
        mInStreamThreads.clear();
    }

    private void cancelConnectThreads(){
        if (mConnectThreads != null) {
            for (ConnectThread connectThread : mConnectThreads) {
                if (connectThread != null) {
                    connectThread.cancel();
                }
            }
            mConnectThreads = null;
        }
    }

    private void cancelAcceptThreads(){
        if (mSecureAcceptThreads != null) {
            for (AcceptThread acceptThread : mSecureAcceptThreads) {
                if (acceptThread != null) {
                    acceptThread.cancel();
                }
            }
            mSecureAcceptThreads = null;
        }
    }

    @Override
    public synchronized void stopListening() throws IOException {
        if (DEBUG) Log.d(TAG, "stopListening");

        setState("stopListening", ConnectionState.STOPPED);

        // Cancel any thread attempting to make a connection
        cancelConnectThreads();

        // Cancel the accept thread
        cancelAcceptThreads();

        // Cancel any thread currently running a connection
        cancelInStreamThreads();

        mOutStreamWriter.cancel();
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
     * @param address The Address of the Bluetooth Device to connect
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
            cancelConnectThreads();
        }

        // Cancel any thread currently running a connection
        cancelInStreamThreads();

        // Start the thread to connect with the given device
        mConnectThreads = new ConnectThread[SOCKETS_CNT];
        for (int i=0; i<mConnectThreads.length; i++){
            mConnectThreads[i] = new ConnectThread(mBluetoothAdapter.getRemoteDevice(address), i);
            mConnectThreads[i].start();
        }
        setState("connect", ConnectionState.CONNECTING);
    }

    @Override
    public void disconnect() throws IOException {
        setState("disconnect()", ConnectionState.DISCONNECTED);

        // Cancel any thread attempting to make a connection
        cancelConnectThreads();

        // Cancel any thread currently running a connection
        cancelInStreamThreads();

        mOutStreamWriter.cancel();
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param device The BluetoothDevice that has been connected
     * @throws IOException
     * @throws InterruptedException
     */
    public synchronized void connected(BluetoothDevice device, BluetoothSocket btSocket, int sessionId) throws IOException, InterruptedException {
        if (DEBUG) Log.d(TAG, "connected");

        // Cancel any thread with the same id currently running a connection
        if (mInStreamThreads.get(sessionId) != null) {
            mInStreamThreads.get(sessionId).cancel();
            mInStreamThreads.remove(sessionId);
        }

        // Start the thread to manage the connection and perform transmissions
        InStreamThread inStreamThread = new InStreamThread(btSocket, sessionId);
        inStreamThread.start();
        mInStreamThreads.put(sessionId, inStreamThread);
        mOutStreamWriter.addSocketToPool(btSocket);

        // Send the name of the connected device back to the UI Activity
        mDeviceName = device.getName();
        mHUDConnectivity.onDeviceName(mDeviceName);

        // only broadcast connected state the first time
        if (mState != ConnectionState.CONNECTED){
            setState("connected", ConnectionState.CONNECTED);
        }

    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final static int MAX_RETRIES = 3;
        private final BluetoothSocket mmBTSocket;

        private final BluetoothDevice mmDevice;
        private int mIndex;

        public ConnectThread(BluetoothDevice device, int index) {
            mIndex = index;
            mmDevice = device;
            BluetoothSocket socket = null;
            try {
                // Get a BluetoothSocket for a connection with the given BluetoothDevice
                socket = device.createRfcommSocketToServiceRecord(mUUIDs[index]);
            } catch (IOException e) {
                Log.e(TAG, "Failed to createRfcommSocketToServiceRecord", e);
            }
            mmBTSocket = socket;
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
            setName("ConnectThread #" + mIndex);

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a successful connection or an exception
                if (DEBUG) Log.d(TAG, "connecting to socket #" + mIndex);
                connectSocket(mmBTSocket, 0);
            } catch (IOException e) {
                Log.e(TAG, "Connect Failed", e);
                // Close the socket
                try {
                        mmBTSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                btSocketFailed("ConnectThread::run");
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (HUDSPPService.this) {
                mConnectThreads = null;
            }

            // Start the connected thread
            try {
                connected(mmDevice, mmBTSocket, mIndex);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread::run connected thread failed IOException:", e);
            } catch (InterruptedException e) {
                Log.e(TAG, "ConnectThread::run connected thread failed InterruptedException:", e);
            }
        }

        public void cancel() {
            try {
                mmBTSocket.close();
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

        private final BluetoothServerSocket mmBTServerSocket;
        private final int mIndex;

        public AcceptThread(int index) throws IOException {
            mIndex = index;
            mmBTServerSocket = getBluetoothServerSocket("H_" + mSDPName + "_" + index, mUUIDs[index]);
            Log.v(TAG, "created accept socket # " + String.valueOf(index) + "/" + SOCKETS_CNT);
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
            setName("AcceptThread #"+mIndex);
            if (DEBUG) Log.d(TAG, "BEGIN AcceptThread" + this);

            BluetoothSocket socket;

            // Listen to the server socket if we're not connected
            while (mState != ConnectionState.CONNECTED) {
                setState("AcceptThread:run", ConnectionState.LISTENING);
                try {
                    // This is a blocking call and will only return on a successful connection or an exception
                    if (DEBUG) Log.d(TAG, "Waiting to accept socket #" + mIndex);
                    socket = mmBTServerSocket.accept();
                    if (DEBUG) Log.d(TAG, "Accepted socket #" + mIndex);
                } catch (IOException e) {
                    Log.e(TAG, "AcceptThread: accept() failed on accepting socket (OK if this device initated a connection)", e);
                    cancel();
                    break;
                }

                // If a connection was accepted Start the connected thread.
                synchronized (HUDSPPService.this) {
                    try {
                        connected(socket.getRemoteDevice(), socket, mIndex);
                    } catch (IOException e) {
                        Log.e(TAG, "Couldn't get socket to provide a stream IOException:", e);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Couldn't get socket to provide a stream InterruptedException:", e);
                    }
                    break;
                }
            }
            if (DEBUG) Log.i(TAG, "END AcceptThread" + this);
        }

        public void cancel() {
            if (DEBUG) Log.d(TAG, "CANCEL " + this);
            try {
                mmBTServerSocket.close();
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

        private BluetoothSocket mBtSocket;
        private final InputStream mmInStream;
        private final int mSessionID;

        public InStreamThread(BluetoothSocket btSocket, int sessionID) throws IOException {
            Log.d(TAG, "CREATE ConnectedThread with SessionID:" + sessionID);
            mBtSocket = btSocket;
            mmInStream = btSocket.getInputStream();
            mSessionID = sessionID;
        }

        @Override
        public void run() {
            setName("InStreamThread #" + mSessionID);
            Log.i(TAG, "BEGIN " + getName());

            byte[] data = new byte[1024];
            ExcessDataAgent excessDataAgent = new ExcessDataAgent();
            int bytes;

            // Keep listening to the InputStream while connected
            while (!isInterrupted()) {
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
                    cancel();
                    btSocketFailed("InStreamThread::run");
                    // Start the service over to restart listening mode
                    // BluetoothChatService.this.start();
                    break;
                }
            }
        }

        private void cancel(){
            interrupt();

            // this is a blocking call so call it on a background thread
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        mmInStream.close();
                        mBtSocket.close();
                    }catch (Exception e){
                        Log.e(TAG, getName()+ " failed to close socket", e);
                    }
                }
            }).start();

            // this thread is ending so remove it from the list
            mInStreamThreads.delete(mSessionID);
            mOutStreamWriter.removeSocketFromPool(mBtSocket);
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
     * Indicate that the socket connection failed and notify the UI Activity.
     */
    private synchronized void btSocketFailed(String parentFunction) {
        if (DEBUG) Log.d(TAG, "btSocketFailed (called from " + parentFunction + ")");
        if (mInStreamThreads.size()==0 && mState != ConnectionState.LISTENING && mState != ConnectionState.STOPPED) {
            setState(parentFunction, ConnectionState.DISCONNECTED);

            // Start the service over to restart listening mode
            try {
                HUDSPPService.this.startListening();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

}
