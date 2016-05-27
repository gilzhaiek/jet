package com.reconinstruments.os.connectivity.bluetooth;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;

import com.reconinstruments.mfi.MFiServiceManager;
import com.reconinstruments.mfi.MFiServiceListener;
import com.reconinstruments.mfi.MFiInfo;
import com.reconinstruments.os.connectivity.IHUDConnectivity;
import com.reconinstruments.os.connectivity.IHUDConnectivity.ConnectionState;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

/** {@hide}*/
public class HUDMFiService extends HUDBTBaseService implements MFiServiceListener {

    private static final String LISTENER_UUID = "f4085309-5b87-49f7-ba2c-dd3a3a4ef4de";
    private static int MAX_OUTPUT_STREAM_SIZE = 60000;
    private static SparseArray<ExcessDataAgent> mExcessDataAgentSA = new SparseArray<ExcessDataAgent>();
    private MFiServiceManager mMFiService;
    private MFiOutStreamWriter mOutStreamWriter;
    private String mAddress;

    private class MFiOutStreamWriter extends OutStreamWriter {

        private OutputStreamContainer[] mOutputStreamPool;
        private final int mNumberOfChannels;

        public MFiOutStreamWriter(int numberOfChannels) {
            super(new ArrayBlockingQueue<OutputStreamContainer>(numberOfChannels));
            mNumberOfChannels = numberOfChannels;
            mOutputStreamPool = new OutputStreamContainer[numberOfChannels];
            for (int i = 0; i < mNumberOfChannels; i++) {
                //protocol Index starts with 1
                mOutputStreamPool[i] = new OutputStreamContainer(i + 1);
            }
        }

        public boolean addSessionToStreamPool(int sessionID, int protocolIndex) {
            if (DEBUG) Log.d(TAG, "addSessionToStreamPool(protocolIndex: " + protocolIndex + " sessionID: " + sessionID + ")");
                //ProtocolIndex starts from 1
            OutputStreamContainer outputStreamPool = mOutputStreamPool[protocolIndex - 1];
            boolean addSuccessful = outputStreamPool.setSessionID(sessionID);
            if (addSuccessful) {
                try {
                    mFreeOutputStreamPool.put(outputStreamPool);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Error Puting outputStreamPool to FreeOutputStreamPool", e);
                    return false;
                }
            }
            return addSuccessful;
        }

        public void reset() {
            for (OutputStreamContainer outputStreamPool : mOutputStreamPool) {
                outputStreamPool.reset();
                if (DEBUG) Log.d(TAG, "reset() " + outputStreamPool.printMFiStatus());
            }
            mFreeOutputStreamPool.clear();
            if (DEBUG) Log.d(TAG, "Finish MFiOutStreamWriter reset");
        }

        @Override
        public void write(OutputStreamContainer osContainer, byte[] buffer) throws IOException {
            writeData(osContainer.getSessionID(), buffer);
        }
    }

    public HUDMFiService(IHUDConnectivity hudConnectivity) throws Exception {
        super(hudConnectivity);
        mMFiService = new MFiServiceManager();
        mOutStreamWriter = new MFiOutStreamWriter(MFiInfo.NUMBER_OF_HUD_CONNECTIVITY_CHANNELS);
        Log.d(TAG, "HUDMFiService constructor success");

        //Need to Initialize for first time because we already missed the listener callback
        onConnected(mMFiService.getDeviceName(), mMFiService.getLastConnectedBTAddress(), mMFiService.isConnectingOrConnected());
        //This is assuming that our Channels are lined up in the beginning of the MFiInfo protocol arraylist
        for (int i = 0; i < MFiInfo.NUMBER_OF_HUD_CONNECTIVITY_CHANNELS; i++) {
            int sessionID = mMFiService.getSessionID(i + 1);
            onSessionOpened(sessionID, i + 1, sessionID > 0);
        }
        try {
            mMFiService.start(LISTENER_UUID, this);
            Log.d(TAG, "Successfully Start IMFiService");
        } catch (Exception e) {
            Log.e(TAG, "Couldn't Start IMFiService", e);
        }
        //TODO when to stop mMFiService.stop
    }

    @Override
    public void addConsumer(IHUDBTConsumer hudBTConsumer) {
        super.addConsumer(hudBTConsumer);
    }

    @Override
    public void removeConsumer(IHUDBTConsumer hudBTConsumer) {
        super.removeConsumer(hudBTConsumer);
    }

    @Override
    public void startListening() throws IOException {
    }

    @Override
    public void stopListening() throws IOException {
    }

    @Override
    public void write(OutputStreamContainer osContainer, byte[] buffer) throws Exception {
        if (mState != ConnectionState.CONNECTED) {
            throw new Exception("ConnectionState is not CONNECTED");
        }
        if (buffer.length <= MAX_OUTPUT_STREAM_SIZE) {
            mOutStreamWriter.write(osContainer, buffer);
        } else {
            int bufferLength = buffer.length;
            int consumedLength = 0;
            byte[] loopBuffer = new byte[MAX_OUTPUT_STREAM_SIZE];
            while ((bufferLength - consumedLength) > MAX_OUTPUT_STREAM_SIZE) {
                System.arraycopy(buffer, consumedLength, loopBuffer, 0, MAX_OUTPUT_STREAM_SIZE);
                consumedLength += MAX_OUTPUT_STREAM_SIZE;
                mOutStreamWriter.write(osContainer, loopBuffer);
            }
            if ((bufferLength - consumedLength) > 0) {
                loopBuffer = new byte[bufferLength - consumedLength];
                System.arraycopy(buffer, consumedLength, loopBuffer, 0, loopBuffer.length);
                mOutStreamWriter.write(osContainer, loopBuffer);
            }
        }
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

    private void setDeviceName(String bluetoothAddress) {
        mDeviceName = mBluetoothAdapter.getRemoteDevice(bluetoothAddress).getName();
        mHUDConnectivity.onDeviceName(mDeviceName);
    }

    @Override
    public void connect(String address) throws IOException {
        setDeviceName(address);
        if (getState() != ConnectionState.CONNECTED) {
            setState("connect(" + mAddress + ")", ConnectionState.CONNECTED);
        }
    }

    @Override
    public void disconnect() throws IOException {
        //Should not worried about set state to disconnected
        //MFiServiceListener will take care of it
    }

    private static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    private void receivedData(int sessionID, byte[] sessionData) {
        /*if(DEBUG) {
                String dataString = byteArrayToHex(sessionData);
                Log.d(TAG, "receivedData: " + dataString);
        }*/
        HUDBTMessage hudBTRequest = null;
        ExcessDataAgent excessDataAgent = getExcessDataAgent(sessionID);
        int bytes = sessionData.length;
        if (bytes > 0) {
            hudBTRequest = HUDBTMessageCollectionManager.addData(sessionID, sessionData, bytes, excessDataAgent);
        }
        consume(hudBTRequest);

        while (excessDataAgent.hasValidData()) {
            hudBTRequest = HUDBTMessageCollectionManager.addData(sessionID, excessDataAgent.getData(), excessDataAgent.getBytes(), excessDataAgent);
            consume(hudBTRequest);
        }
    }

    private void consume(HUDBTMessage hudBTRequest) {
        if (hudBTRequest != null) {
            synchronized (mHUDBTConsumers) {
                for (int i = 0; i < mHUDBTConsumers.size(); i++) {
                    Log.d(TAG, "Consume Message");
                    if (mHUDBTConsumers.get(i).consumeBTData(hudBTRequest.getHeader(), hudBTRequest.getPayload(), hudBTRequest.getBody())) {
                        break;
                    }
                }
            }
        }
    }

    protected void writeData(final int sessionID, final byte[] data) throws IOException {
        if (mState != ConnectionState.CONNECTED) return;

        if (data == null) {
            String errorMsg = "writeData() data tries to send is null";
            Log.e(TAG, errorMsg);
            throw new IOException(errorMsg);
        }

        Log.d(TAG, "Sending session data to iOS device, sessionID: " + sessionID + " data.length: " + data.length);
        try {
            mMFiService.writeData(sessionID, data);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to write Data into the MFiService", e);
        }
    }

    protected void sessionOpen(int sessionID, int protocolIndex, boolean success) {
        if (success) {
            if (mOutStreamWriter.addSessionToStreamPool(sessionID, protocolIndex)) {
                addExcessDataAgent(sessionID);
            } else {
                //TODO it is possible to get duplicated protocolIndex because of the order of initialization now ? Need to check
                //why we cannot support multiple sessionID in one protocol Index is the session close eventcall back doesn't show which session is closed
                Log.e(TAG, "Duplicated  ProtocolIndex: " + protocolIndex + " Session ID:" + sessionID + " how is that possible");
            }
        } else {
            Log.e(TAG, "sessionOpen(sessionID:" + sessionID + ", protocolIndex:" + protocolIndex + ", success:" + success + ")");
        }
    }

    private void closeSession() {
        resetExcessDataAgent();
        mOutStreamWriter.reset();
        if (getState() == ConnectionState.CONNECTED) {
            setState("closeSession()", ConnectionState.CONNECTING);
        }
    }

    private ExcessDataAgent getExcessDataAgent(int sessionID) {
        return mExcessDataAgentSA.get(sessionID);
    }

    private void addExcessDataAgent(int sessionID) {
        mExcessDataAgentSA.append(sessionID, new ExcessDataAgent());
    }

    private void resetExcessDataAgent() {
        mExcessDataAgentSA.clear();
    }

    @Override
    public void onConnected(String deviceName, String address, boolean success) {
        if (DEBUG)
            Log.d(TAG, "onConnected() DeviceName: " + deviceName + " Address: " + address + " Success: " + success);
        if (success) {
            mHUDConnectivity.onDeviceName(mDeviceName);
            setState("onConnected()", ConnectionState.CONNECTING);
        }
    }

    @Override
    public void onDisconnected() {
        closeSession();
        setState("disconnect()", ConnectionState.DISCONNECTED);
    }

    @Override
    public void onSessionOpened(int sessionID, int protocolIndex, boolean success) {
        sessionOpen(sessionID, protocolIndex, success);
    }

    @Override
    public void onSessionClosed() {
        closeSession();
    }

    @Override
    public void onReceivedData(int sessionID, byte[] sessionData) {
        receivedData(sessionID, sessionData);
    }

    @Override
    public void onSessionDataConfirmationEvent(int sessionID, int packetID, int dataConfirmationStatus) {
        //Do Nothing, this is not used over here
    }

    @Override
    public void onPacketID(int packetID) {
        //Do Nothing, as it will be handle by BTMfiSessionManager
    }
}
