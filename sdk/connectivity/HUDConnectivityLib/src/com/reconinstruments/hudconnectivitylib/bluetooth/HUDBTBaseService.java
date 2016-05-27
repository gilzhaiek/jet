package com.reconinstruments.hudconnectivitylib.bluetooth;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.bluetooth.BluetoothAdapter;
import android.util.Log;

import com.reconinstruments.hudconnectivitylib.IHUDConnectivity;
import com.reconinstruments.hudconnectivitylib.IHUDConnectivity.ConnectionState;

public abstract class HUDBTBaseService implements IHUDBTService {

    protected final String TAG = this.getClass().getSuperclass().getSimpleName();

    protected static final boolean DEBUG = true;

    protected static final boolean FORCE_BT_ENABLE = true;

    protected BluetoothAdapter mBluetoothAdapter = null;
    protected final IHUDConnectivity mHUDConnectivity;
    protected ConnectionState mState;

    protected String mDeviceName = "NULL";
    protected final ArrayList<IHUDBTConsumer> mHUDBTConsumers = new ArrayList<IHUDBTConsumer>();

    public HUDBTBaseService(IHUDConnectivity hudConnectivity) throws Exception {
        if (hudConnectivity == null) {
            throw new NullPointerException("HUDSPPService Constructor can't have null values");
        }
        mHUDConnectivity = hudConnectivity;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            throw new Exception("HUDSPPService: BluetoothAdapter.getDefaultAdapter() is null, is your Bluetooth Off?");
        }

        if (!mBluetoothAdapter.isEnabled() && FORCE_BT_ENABLE) {
            mBluetoothAdapter.enable();
        }

        mState = ConnectionState.DISCONNECTED;
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    protected synchronized void setState(String parentFunction, ConnectionState state) {
        if (DEBUG) Log.d(TAG, "setState(" + parentFunction + ") " + mState + " -> " + state);
        mState = state;

        mHUDConnectivity.onConnectionStateChanged(state);
    }

    /**
     * @return the current connection state
     * <br>ConnectionState.LISTENING
     * <br>ConnectionState.CONNECTED
     * <br>ConnectionState.CONNECTING
     * <br>ConnectionState.DISCONNECTED
     */
    @Override
    public synchronized ConnectionState getState() {
        return mState;
    }

    @Override
    public String getDeviceName() {
        return mDeviceName;
    }

    public void addConsumer(IHUDBTConsumer hudBTConsumer){
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

    public class OutputStreamContainer {
        private final static int EMPTY_SESSION_ID = -1;
        private int mSessionID = EMPTY_SESSION_ID;
        private int mProtocolChannel = -1;

        protected OutputStream mmOutStream;
        private final BlockingQueue<OutputStreamContainer> mBlockingQueue;

        public OutputStreamContainer(BlockingQueue<OutputStreamContainer> blockingQueue, int protocolChannel) {
            mBlockingQueue = blockingQueue;
            mProtocolChannel = protocolChannel;
        }

        public OutputStreamContainer(BlockingQueue<OutputStreamContainer> blockingQueue, OutputStream outputStream) {
            mBlockingQueue = blockingQueue;
            mmOutStream = outputStream;
        }

        public boolean setSessionID(int sessionID) {
            if (hasSessionID()) return false;
            mSessionID = sessionID;
            return true;
        }

        public boolean hasSessionID() {
            return mSessionID != EMPTY_SESSION_ID;
        }

        public void reset() {
            mSessionID = EMPTY_SESSION_ID;
        }

        public int getProtocolChannel() {
            return mProtocolChannel;
        }

        public int getSessionID() {
            return mSessionID;
        }

        public String printMFiStatus() {
            return "ProtocolIndex: " + getProtocolChannel() + " SessionID: " + getSessionID();
        }

        public OutputStream getOutputStream() {
            return mmOutStream;
        }

        public void release() throws InterruptedException {
            mBlockingQueue.put(this);
        }
    }

    protected abstract class OutStreamWriter {

        protected final OutputStreamContainer mOutputStreamPool[];
        protected final BlockingQueue<OutputStreamContainer> mFreeOutputStreamPool;

        public OutStreamWriter(OutputStreamContainer[] outputStreamPool, ArrayBlockingQueue<OutputStreamContainer> freeOutputStreamPool){
            Log.d(TAG, "CREATE OutgoingStreamThread");
            mOutputStreamPool = outputStreamPool;
            mFreeOutputStreamPool = freeOutputStreamPool;
        }

        public OutputStreamContainer obtain() throws InterruptedException {
            if (DEBUG) Log.d(TAG, "OutputStreamPool obtain: " + mFreeOutputStreamPool.size());
            return mFreeOutputStreamPool.take();
        }

        public void release(OutputStreamContainer osContainer) throws InterruptedException {
            mFreeOutputStreamPool.put(osContainer);
            if (DEBUG) Log.d(TAG, "OutputStreamPool release: " + mFreeOutputStreamPool.size());
        }

        /**
         * Write to connected OutStream
         * @param osContainer 
         * @param buffer The bytes to write
         * @throws IOException
         */
        abstract void write(OutputStreamContainer osContainer, byte[] buffer) throws IOException;
    }
}
