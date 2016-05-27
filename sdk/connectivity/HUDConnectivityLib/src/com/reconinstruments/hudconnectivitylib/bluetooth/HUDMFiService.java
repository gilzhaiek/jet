package com.reconinstruments.hudconnectivitylib.bluetooth;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;

import com.reconinstruments.hudconnectivitylib.IHUDConnectivity;
import com.reconinstruments.hudconnectivitylib.IHUDConnectivity.ConnectionState;
import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.BluetopiaPMException;
import com.stonestreetone.bluetopiapm.SPPM;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortClientManager;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortClientManager.ConnectionFlags;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortServerManager;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortServerManager.IncomingConnectionFlags;
import com.stonestreetone.bluetopiapm.ServerNotReachableException;

import java.io.IOException;
import java.util.EnumSet;
import java.util.concurrent.ArrayBlockingQueue;

public class HUDMFiService extends HUDBTBaseService {

    private static final int FLUSH_TIME_OUT = 5000;
    private static final int DEFAULT_PORT_NUMBER = 1;
    private static final int BUFFSIZE = 4096;
    private static final int DATA_PACKET_TIMEOUT = 800;
    private static final String BUNDLE_SEED_ID = "12345ABCDE";
    private static final String CURRENT_LANGUAGE = "en";

    private static int MAX_OUTPUT_STREAM_SIZE = 60000;
    private static int LOOP_BUFFER_SIZE = 10000;

    private static final EnumSet<IncomingConnectionFlags> INCOMING_CONNECTION_FLAGS = EnumSet.noneOf(IncomingConnectionFlags.class);

    private static final EnumSet<ConnectionFlags> CONNECTION_FLAGS = EnumSet.noneOf(ConnectionFlags.class);

    private static SparseArray<ExcessDataAgent> mExcessDataAgentSA = new SparseArray<ExcessDataAgent>();

    static {
        INCOMING_CONNECTION_FLAGS.add(IncomingConnectionFlags.REQUIRE_AUTHENTICATION);
        INCOMING_CONNECTION_FLAGS.add(IncomingConnectionFlags.REQUIRE_ENCRYPTION);
        INCOMING_CONNECTION_FLAGS.add(IncomingConnectionFlags.MFI_REQUIRED);
        CONNECTION_FLAGS.add(ConnectionFlags.REQUIRE_AUTHENTICATION);
        CONNECTION_FLAGS.add(ConnectionFlags.REQUIRE_ENCRYPTION);
        CONNECTION_FLAGS.add(ConnectionFlags.MFI_REQUIRED);
    }

    private BTMFiEventCallback mServerEventCallback = null;
    private BTMFiEventCallback mClientEventCallback = null;

    private SerialPortClientManager mSerialPortClientManager;
    private SerialPortServerManager mSerialPortServerManager;

    private ConnectingRoute mConnectingRoute;
    private String mAddress = null;

    public enum ConnectingRoute {
        NO_ROUTE,
        CLIENT,
        SERVER
    }

    private MFiOutStreamWriter mOutStreamWriter;

    private class MFiOutStreamWriter extends OutStreamWriter {

        private final int mNumberOfChannels;

        public MFiOutStreamWriter(int numberOfChannels) {
            super(new OutputStreamContainer[numberOfChannels], new ArrayBlockingQueue<OutputStreamContainer>(numberOfChannels));
            mNumberOfChannels = numberOfChannels;

            for (int i = 0; i < mNumberOfChannels; i++) {
                //protocol Index starts with 1
                mOutputStreamPool[i] = new OutputStreamContainer(mFreeOutputStreamPool, i + 1);
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
                    Log.e(TAG, "Error Puting TwoWayStream to FreeTwoWayStreams", e);
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
            if(DEBUG) Log.d(TAG,"Finish MFiOutStreamWriter reset");
        }

        @Override
        public void write(OutputStreamContainer osContainer, byte[] buffer) throws IOException {
            writeData(osContainer.getSessionID(), buffer);
        }
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch(msg.what){
                case BTMFiEventCallback.SESSION_DATA_RECEIVED_EVENT:
                    receivedData(msg.arg1, (byte[]) msg.obj);
                    break;
                case BTMFiEventCallback.FROM_HUD_CONNECTED_EVENT:
                    establishedConnectionFromHUDEvent(msg.arg1 == 1 ? true : false);
                    break;
                case BTMFiEventCallback.FROM_IPHONE_CONNECTED_EVENT:
                    establishedConnectionFromIphoneEvent((String) msg.obj);
                    break;
                case BTMFiEventCallback.DISCONNECTED_EVENT:
                    disconnectionEvent();
                    break;
                case BTMFiEventCallback.SESSION_OPEN_REQUEST_EVENT:
                    sessionOpenRequestEvent(msg.arg1, msg.arg2);
                    break;
                case BTMFiEventCallback.SESSION_CLOSE_EVENT:
                    sessionCloseEvent(msg.arg1);
                    break;
            }
        }
    };

    public HUDMFiService(IHUDConnectivity hudConnectivity) throws Exception {
        super(hudConnectivity);
        mConnectingRoute = ConnectingRoute.NO_ROUTE;
        mOutStreamWriter = new MFiOutStreamWriter(MFiInfo.NUMBER_OF_CHANNELS);

        mClientEventCallback = new BTMFiEventCallback("Client", mHandler);
        mServerEventCallback = new BTMFiEventCallback("Server", mHandler);
        openSerialPortManagers();
        Log.d(TAG, "HUDMFiService constructor success");
    }

    private void openSerialPortManagers() throws Exception {
        // Open the Client Manager
        if (DEBUG) Log.d(TAG, "Opening and configuring the SerialPortClientManager");
        if (mSerialPortClientManager == null) {
            try {
                mSerialPortClientManager = new SerialPortClientManager(mClientEventCallback);
            } catch (Exception e) {
                Log.e(TAG, "HUDMFiService: Could not connect to the BluetopiaPM service", e);
                throw e;
            }
        }
        configureMFiSettings(mSerialPortClientManager);
        if (DEBUG) Log.d(TAG, "SerialPortClientManager is now configured and open");


        // Open the Server Manager
        if (DEBUG) Log.d(TAG, "Opening and configuring the SerialPortServerManager");
        if (mSerialPortServerManager == null) {
            try {
                mSerialPortServerManager = new SerialPortServerManager(mServerEventCallback, INCOMING_CONNECTION_FLAGS);
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
        if (mState != ConnectionState.CONNECTED){
            throw new Exception("ConnectionState is not CONNECTED");
        }
        if(buffer.length <= MAX_OUTPUT_STREAM_SIZE){
            mOutStreamWriter.write(osContainer, buffer);
        }
        else {
            int bufferLength = buffer.length;
            int consumedLength = 0;
            byte[] loopBuffer = new byte[LOOP_BUFFER_SIZE];
            while((bufferLength - consumedLength) > LOOP_BUFFER_SIZE){
                System.arraycopy(buffer, consumedLength, loopBuffer, 0, LOOP_BUFFER_SIZE);
                consumedLength += LOOP_BUFFER_SIZE;
                mOutStreamWriter.write(osContainer, loopBuffer);
            }
            if((bufferLength - consumedLength) > 0){
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

    @Override
    public void connect(String address) throws IOException {
        Log.d(TAG, "connect to: " + mAddress);
        if (address == null) {
            throw new NullPointerException("Device address can't be null");
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
                return serialPortClientManager.connectRemoteDevice(bluetoothAddress, DEFAULT_PORT_NUMBER, CONNECTION_FLAGS, false);
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result != 0) {
                    Log.e(TAG, "connect(" + mAddress + ") failed, error #" + result);
                    setState("connect", ConnectionState.DISCONNECTED);
                    disconnect();
                }
                setDeviceName(mAddress);
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
    }

    private synchronized void setConnectingRoute(ConnectingRoute route) {
        if (DEBUG) Log.d(TAG, "Connecting Route : " + route.toString());
        mConnectingRoute = route;
    }

    public synchronized ConnectingRoute getConnectingRoute() {
        return mConnectingRoute;
    }

    protected void establishedConnectionFromHUDEvent(boolean successful) {
        //deviceName and bluetoothAddress was already set during connect(String address)
        if (successful) {
            setConnectingRoute(ConnectingRoute.CLIENT);
        } else {
            setConnectingRoute(ConnectingRoute.NO_ROUTE);
            setState("failed establishedConnectionFromHUD", ConnectionState.DISCONNECTED);
            Log.e(TAG, "establishedConnectionFromHUDEvent(" + mAddress + "), failed");
        }
    }

    protected void establishedConnectionFromIphoneEvent(String bluetoothAddress) {
        mAddress = bluetoothAddress;
        setConnectingRoute(ConnectingRoute.SERVER);
        setDeviceName(bluetoothAddress);
    }

    protected void disconnectionEvent() {
        //Disconnect event occurred
        disconnect();
    }

    protected void sessionOpenRequestEvent(int sessionID, int protocolIndex) {
        SPPM manager = getSerialPortManager();
        int result = -1;
        if (manager == null) {
            String errorMsg = "sessionOpenRequestEvent() sessionID:" + sessionID + "protocolIndex: " + protocolIndex + " serial port manager is null";
            Log.e(TAG, errorMsg);
            return;
        }
        result = manager.openSessionRequestResponse(sessionID, true);
        if (result >= 0) {
            if (!mOutStreamWriter.addSessionToStreamPool(sessionID, protocolIndex)) {
                //TODO is it possible to have same sessionId ?
                //why we cannot support multiple sessionID in one protocol Index is the session close eventcall back doesn't show which session is closed
                Log.e(TAG, "Duplicated  ProtocolIndex: " + protocolIndex + " Session ID:" + sessionID + " how is that possible");
            }
            else {
                addExcessDataAgent(sessionID);
                if(getState() != ConnectionState.CONNECTED){
                    setState("establishedConnectionFromHUD", ConnectionState.CONNECTED);
                }
            }
        } else {
            Log.e(TAG, "Session Open Request Event Failed");
            //TODO No way to closeSessionRequestResphone so we can only disconnect for now
            disconnect();
        }
    }

    protected void sessionCloseEvent(int sessionID) {
        closeSession();
    }

    private void closeSession(){
        resetExcessDataAgent();
        mOutStreamWriter.reset();
        if(getState() == ConnectionState.CONNECTED){
            setState("closeSession()", ConnectionState.CONNECTING);
        }
    }

    protected void receivedData(int sessionID, byte[] sessionData) {
        HUDBTMessage hudBTRequest = null;
        ExcessDataAgent excessDataAgent = getExcessDataAgent(sessionID);
        int bytes = sessionData.length;
        if (bytes > 0){
            hudBTRequest = HUDBTMessageCollectionManager.addData(sessionID, sessionData, bytes, excessDataAgent);
        }
        consume(hudBTRequest);

        while(excessDataAgent.hasValidData()){
            hudBTRequest = HUDBTMessageCollectionManager.addData(sessionID, excessDataAgent.getData(), excessDataAgent.getBytes(), excessDataAgent);
            consume(hudBTRequest);
        }
    }    

    protected void writeData(final int sessionID, final byte[] data) throws IOException {
        if (mState != ConnectionState.CONNECTED) return;
        SPPM manager = getSerialPortManager();
        if (manager == null) {
            String errorMsg = "writeData() State show connected but serial port manager is null";
            Log.e(TAG, errorMsg);
            throw new IOException(errorMsg);
        }

        if (data == null) {
            String errorMsg = "writeData() data tries to send is null";
            Log.e(TAG, errorMsg);
            throw new IOException(errorMsg);
        }

        Log.d(TAG, "Sending session data to iOS device, sessionID: " + sessionID + " data.length: " + data.length);
        int result = manager.sendSessionData(sessionID, data);
        if (result >= 0) {
            Log.d(TAG, "Sent session data to iOS device, sessionsID: " + sessionID + " packetId: " + result);
        } else {
            //TODO here need to catch for error code
            // we reconnect here since we can't send packet out.
            // if (sendingPacketId == -10087)
            // ?? why reconnect we reconnect here since we can't send packet out.
            Log.w(TAG, "Sent session data to iOS device, packetId: " + result);
        }
    }

    private void consume(HUDBTMessage hudBTRequest){
        if (hudBTRequest != null){
            synchronized (mHUDBTConsumers) {
                for (int i = 0; i < mHUDBTConsumers.size(); i++) {
                    if (mHUDBTConsumers.get(i).consumeBTData(hudBTRequest.getHeader(), hudBTRequest.getPayload(), hudBTRequest.getBody())) {
                        break;
                    }
                }
            }
        }
    }

    private ExcessDataAgent getExcessDataAgent(int sessionID){
        return mExcessDataAgentSA.get(sessionID);
    }

    private void addExcessDataAgent(int sessionID){
        mExcessDataAgentSA.append(sessionID, new ExcessDataAgent());
    }

    private void resetExcessDataAgent(){
        mExcessDataAgentSA.clear();
    }

    /**
     * Find the Name of the bluetooth unit and notify HUDConnectivity
     *
     * @param bluetoothAddress address of the paired bluetooth unit
     */
    private void setDeviceName(String bluetoothAddress) {
        mDeviceName = mBluetoothAdapter.getRemoteDevice(bluetoothAddress).getName();
        mHUDConnectivity.onDeviceName(mDeviceName);
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
}
