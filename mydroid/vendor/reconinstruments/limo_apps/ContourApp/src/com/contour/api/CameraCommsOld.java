package com.contour.api;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.contour.api.CameraProtocolOld.AckPacket;
import com.contour.api.CameraProtocolOld.CmdPacket;
import com.contour.api.CameraProtocolOld.GPSAssistPayload;
import com.contour.api.CameraProtocolOld.Packet;
import com.contour.api.CameraProtocolOld.VideoPacket;
import com.contour.connect.SettingsFragment;
import com.contour.connect.debug.CLog;
import com.contour.utils.SharedPrefHelper;


public class CameraCommsOld extends BaseCameraComms {

    // private static final String NAME = "contour.android";
    // private static final UUID MY_UUID =
    // UUID.fromString("00000000-DECA-FADE-DECA-DEAFDECACAFF");
    private static final String    TAG                  = "CameraCommsOld";

    // State of the service
    public static final int        STATE_IDLE           = 0;
    public static final int        STATE_CONNECTING     = 1;
    public static final int        STATE_CONNECTED      = 2;

    // Camera Status
    CameraStatus                   mStatus;
    private int                    mMajorVersion;
    private int                    mMinorVersion;
    private int                    mBuildNumber;
    private int                    mModel;

    // Camera Settings
    private CameraProtocolOld.CameraSettingsOld         mSettings1 = null;
    private CameraProtocolOld.CameraSettingsOld         mSettings2 = null;
    private CameraSettings         mSettings;
    private int                    mCommitDelay         = 0;

    private int                    mState               = STATE_IDLE;
    private boolean                mStatusUpdates       = true;
    private Boolean                mWaitingForVFrame    = false;

//    private GPSUploadThread        mGPSUpload;
    private boolean                mFileIsPrepared      = false;
    private List<GPSAssistPayload> mGPSPayloads;

    private CommThread             mCommThread;

    private int                    mRetainCount;
    private CameraCommsCallback    mCallback;
    private final Runnable mStatusRunnable = new Runnable() {
        @Override
        public void run() {
            CameraProtocolOld.CmdPacket statusReq = new CameraProtocolOld.CmdPacket(
                    (byte) CameraProtocolOld.CMD_GET_CAMERA_STATUS, 0, null);
            sendPacket(statusReq);
            mHandler.postDelayed(this, 5000);
        }
    };
    private final Runnable mSendSwitch2SettingsRunnable = new Runnable() {
        @Override
        public void run() {
            commitSettings(2);
            broadcast(ACTION_SETTINGS, mSettings);        
        }
    };

    public CameraCommsOld(Context context,CameraCommsCallback callback) {
        super(context);

        mCommitDelay = 1500;
        mCallback = callback;
        
        this.mTriggerRequests.put(CameraProtocolOld.CMD_REQUEST_IDENTIFY, new RequestFunction() {
            @Override
            public void doRequest() {
                sendStartupUpdate();
                requestStatus();
            }
        });
        this.mTriggerRequests.put(CameraProtocolOld.CMD_RET_CAMERA_STATUS, new RequestFunction() {
            @Override
            public void doRequest() {
                requestSettings();
                startVideoStream();
            }
        });
    }

    public final int getState() {
        return mState;
    }

    public void retain() {
        if(mDebug) CLog.out(TAG,"retain");
        ++mRetainCount;
    }

    public void release() {
        if(mDebug) CLog.out(TAG,"release");

        if (mRetainCount > 0)
            --mRetainCount;
        else {
            if(mDebug) CLog.out(TAG,"Service was overreleased.");
            return;
        }

        if (mRetainCount == 0) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkRetainCount();
                }
            }, 5000);
        }
    }

    private void checkRetainCount() {
        if (mRetainCount == 0) {
            if(mDebug) CLog.out(TAG,"Disconnecting due to retain timeout.");
            disconnect();
        }
    }

    private void switchState(int state) {
        mState = state;
        broadcast(ACTION_STATECHANGE);
    }

    public boolean isConnected() {
        return mState == STATE_CONNECTED;
    }

    public boolean gpsFileIsReady() {
        return mFileIsPrepared;
    }

    public boolean sendGPSFile() {
        if (!mFileIsPrepared)
            return false;

//        if (mGPSUpload == null) {
//            pauseStatusUpdates();
//            stopVideoStream();
//
//            mGPSUpload = new GPSUploadThread();
//            mGPSUpload.start();
//        } else
//            return false;

        return true;
    }

    private void gpsFilePrepared() {
        File gpsFile = new File(mContext.getFilesDir(), GPSDownloadThread.ALMANAC_FILENAME);
        mGPSPayloads = GPSAssistPayload.createGPSAssistPayloads(gpsFile);

        mFileIsPrepared = true;
    }

    private void connectionFailed(Throwable reason) {
        broadcast(ACTION_CONNECTFAILED, reason);
    }

    public void connected() {
        if(mDebug) CLog.out(TAG,"Connected to device.");

        switchState(STATE_CONNECTED);
        broadcast(ACTION_CONNECTED);
    }

    @Override
    public void disconnected() {
        if(mDebug) CLog.out(TAG,"Disconnected from device.");

        mHandler.removeCallbacks(mStatusRunnable);

        
//        if (mGPSUpload != null)
//            mGPSUpload.cancel();

        switchState(STATE_IDLE);
        broadcast(ACTION_DISCONNECTED);
        mSettings = null;
        mSettings1 = null;
        mSettings2 = null;
    }

    public void receivedPacket(BasePacket packet) {
        // d("Recv: " + packet.toString());

        
//        if(D) CLog.out(TAG,Arrays.toString(packet.toByteArray()));

        switch (packet.getHeaderType()) {
        case CameraProtocolOld.PT_ACK: {
            if (mDebug)
                CLog.out(TAG, "received PT_ACK", packet.toString());
            doTriggerRequests(CameraProtocolOld.CMD_CUSTOM);
            break;
        }

        case CameraProtocolOld.PT_CMD: {
            // Handle the packet
            BaseCmdPacket cmdPkt = (BaseCmdPacket) packet;
            receivedCmd(cmdPkt);

            // Send an ack to the packet
            AckPacket ack = new AckPacket(cmdPkt.getPacketId());
            sendPacket(ack);
            break;
        }

        case CameraProtocolOld.PT_VIDEO: {
            

            // If we were waiting for this frame, mark it
//            if (mWaitingForVFrame) {
//                synchronized (mWaitingForVFrame) {
//                    mWaitingForVFrame = false;
//                }
//            }

            // Cast the packet
            VideoPacket vp = (VideoPacket) packet;
            mCallback.onVideoFrameReceived(vp.getBitmap());

            // Send an ack to the packet
            AckPacket ack = new AckPacket(vp.video_id);
            sendPacket(ack);

            // Broadcast the frame
            // broadcast(ACTION_VIDEOFRAME, vp.data);
            break;
        }
        }
    }

    private void receivedCmd(BaseCmdPacket packet) {
        if (mDebug)
            CLog.out(TAG, "receivedCmd", packet.getCmd(), Arrays.toString(packet.getData()));
        int cmdId = packet.getCmd();
        switch (cmdId) {
        case CameraProtocolOld.CMD_RESEND: {
            if (mDebug)
                CLog.out(TAG, "Recv CMD_RESEND");

            // if (mGPSUpload != null) {
            // mGPSUpload.resendReceived(packet.getParam());
            // }
            break;
        }

        case CameraProtocolOld.CMD_REQUEST_IDENTIFY: {
            int param = packet.getParam();
            mMajorVersion = (param >> 16) & 0xFF;
            mMinorVersion = (param >> 8) & 0xFF;
            mBuildNumber = param & 0xFF;
            if (mDebug)
                CLog.out(TAG, "Recv CMD_REQUEST_IDENTIFY", mMajorVersion, mMinorVersion, mBuildNumber);
            break;
        }

        case CameraProtocolOld.CMD_RET_CAMERA_STATUS: {
            if (mDebug)
                CLog.out(TAG, "Recv CMD_RET_CAMERA_STATUS", packet.getPacketId());

            handleReceivedStatus(packet.getData());
            break;
        }

        case CameraProtocolOld.CMD_RECORD_STREAM_STOPPED: {
            if (mDebug) CLog.out(TAG, "Recv CMD_RECORD_STREAM_STOPPED");
            startVideoStream();
            broadcast(ACTION_RECORDING_STOPPED);
            break;
        }

        case CameraProtocolOld.CMD_RECORD_STREAM_STARTED: {
            if (mDebug) CLog.out(TAG, "Recv CMD_RECORD_STREAM_STARTED");
            broadcast(ACTION_RECORDING_STARTED);
            break;
        }

        case CameraProtocolOld.CMD_RET_CAMERA_SETTINGS: {
            handleSettingsReceived(packet.getData());
            break;
        }
        case CameraProtocolOld.CMD_VIDEO_STREAM_STARTED:
            if (mDebug) CLog.out(TAG, "Recv CMD_VIDEO_STREAM_STARTED");
            break;
        case CameraProtocolOld.CMD_VIDEO_STREAM_STOPPED:
            if (mDebug) CLog.out(TAG, "Recv CMD_VIDEO_STREAM_STOPPED");
            break;
        }
        doTriggerRequests(cmdId);
    }
    
    private void doTriggerRequests(int cmdId) {
        RequestFunction f = mTriggerRequests.get(cmdId);
        mTriggerRequests.delete(cmdId);
        if(f != null) {
            f.doRequest();
        }
    }
    
    int mCurrentCameraState = CameraStatus.CAMERA_STATE_UNKNOWN;
    private void handleReceivedStatus(byte[] bytes) {
        mStatus = CameraStatus.decodeByteArrayOld(bytes);
        if (mDebug) CLog.out(TAG, "Recv ACTION_STATUS", mStatus);
        if(this.mSettings != null)
            mSettings.switchPosition = mStatus.switchPos;
        
        { //sync app to camera status if necssary
            int newCameraState = mStatus.cameraState;
            int oldCameraState = mCurrentCameraState;
            mCurrentCameraState = newCameraState;
            if(oldCameraState == CameraStatus.CAMERA_STATE_UNKNOWN) {
                switch(newCameraState) {
                case CameraStatus.CAMERA_STATE_PREVIEW: 
                    broadcast(ACTION_RECORDING_STOPPED);
                    break;
                case CameraStatus.CAMERA_STATE_PREVIEW_PAUSED: 
                    broadcast(ACTION_RECORDING_STOPPED);    
                    break;
                case CameraStatus.CAMERA_STATE_RECORDING:
                    Intent i = new Intent(ACTION_RECORDING_STARTED);
                    i.putExtra(EXTRA_INTEGER, mStatus.elapsedRecordSeconds);
                    mContext.sendBroadcast(i);
                    break;
                case CameraStatus.CAMERA_STATE_RECORDING_STOPPED: 
                    broadcast(ACTION_RECORDING_STOPPED);    
                    break;
                }
            }
        }
        broadcast(ACTION_STATUS, mStatus);
    }    
    private void handleSettingsReceived(byte[] data) {
        if (mDebug) CLog.out(TAG, "Recv CMD_RET_CAMERA_SETTINGS", Arrays.toString(data));

        CameraProtocolOld.CameraSettingsOld settings = CameraProtocolOld.CameraSettingsOld.decodeByteArray(data);
        if (settings.switchPosition == 0)
            mSettings1 = settings;
        else
            mSettings2 = settings;

        mModel = settings.cameraModel;

        if(mDebug) CLog.out(TAG,"Recv SETTINGS FOR SW "  + settings.switchPosition,settings);
        
        if(mSettings1 != null && mSettings2 != null) {
         
            mSettings = CameraSettings.convertFromOld(mSettings1, mSettings2,mStatus.switchPos);
            broadcast(ACTION_SETTINGS,mSettings);

//            broadcastOrdered(ACTION_CAMERA_CONFIGURE_ENABLED);
        }
    }

    public void connect(BluetoothDevice device) {
        disconnect();

        switchState(STATE_CONNECTING);
        broadcast(ACTION_CONNECTING);

        if(mDebug) CLog.out(TAG,"Connecting to device: " + device.getName().trim());

        // Start connect thread
//        (new ConnectThread(device)).start();
    }

    public void disconnect() {
        if (mCommThread != null) {
            mCommThread.cancel();
            mCommThread = null;
        }
    }

    public int getModel() {
        return mModel;
    }

    public CameraStatus getStatus() {
        return mStatus;
    }

    public CameraProtocolOld.CameraSettingsOld getSettings(int switchPosition) {
        return (switchPosition == 1) ? mSettings1 : mSettings2;
    }

    public void setCommitDelay(int msDelay) {
        mCommitDelay = msDelay;
    }

    public int getCommitDelay() {
        return mCommitDelay;
    }

    public void commitSettings(int switchPos) {
        
        if (switchPos == 1) {
            pauseStatusUpdates();

            if (mSettings1 != null) {
                final CmdPacket setSettings1 = new CmdPacket(CameraProtocolOld.CMD_SET_CAMERA_SETTINGS, 0, mSettings1.toByteArray());
                sendPacket(setSettings1);
            }
            if (mDebug)
                CLog.out(TAG, "Sending Switch 1 Settings: " + mSettings1.toString());

        } else {
            if (mSettings2 != null) {
                final CmdPacket setSettings2 = new CmdPacket(CameraProtocolOld.CMD_SET_CAMERA_SETTINGS, 1, mSettings2.toByteArray());
                sendPacket(setSettings2);
                mHandler.removeCallbacks(mStatusRunnable);
                mHandler.postDelayed(mStatusRunnable, 1000);
            }
            if (mDebug)
                CLog.out(TAG, "Sending Switch 2 Settings: " + mSettings2.toString());
        }
    }

    private void requestStatus() {
        if (mDebug) CLog.out(TAG, "Sending requestStatus.");
        
        mHandler.removeCallbacks(mStatusRunnable);
        
        if(isConnected() && mStatusUpdates)
        {
            CmdPacket statusReq = new CmdPacket(CameraProtocolOld.CMD_GET_CAMERA_STATUS, 0, null);
            sendPacket(statusReq);
            mHandler.postDelayed(mStatusRunnable, 5000);
        }


//        if (mState == STATE_CONNECTED && mStatusUpdates) {
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    requestStatus();
//                }
//            }, 5000);
//        }
    }

    private void requestSettings(int switchPosition) {
        // Check if we've already received this setting
//        CameraProtocolOld.CameraSettingsOld settings = getSettings(switchPosition);

        if (isConnected()) {
            CmdPacket settingsReq = new CmdPacket(CameraProtocolOld.CMD_GET_CAMERA_SETTINGS, switchPosition-1, null);
            sendPacket(settingsReq);

            // Make sure that this setting has been set 2s from now
//            final int switchPos = switchPosition;
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    requestSettings(switchPos);
//                }
//            }, 2000);
        }
    }

    public void pauseStatusUpdates() {
        mStatusUpdates = false;
        mHandler.removeCallbacks(mStatusRunnable);
    }

    public void resumeStatusUpdates() {
        mStatusUpdates = true;
        requestStatus();
    }

    public void startVideoStream() {
        if (!isConnected())
            return;

        CmdPacket startVideo = new CmdPacket(CameraProtocolOld.CMD_VIDEO_STREAM_ON, 0, null);
        sendPacket(startVideo);
    }

    public void stopVideoStream() {
        CmdPacket stopVideo = new CmdPacket(CameraProtocolOld.CMD_VIDEO_STREAM_OFF, 0, null);
        sendPacket(stopVideo);
    }

    @Override
    public void startRecording() {
        CmdPacket startRecord = new CmdPacket(CameraProtocolOld.CMD_RECORD_STREAM_START, 0, null);
        sendPacket(startRecord);
    }

    public void sendStartupUpdate() {
        if(mDebug) CLog.out(TAG,"SENDING UPDATE TIME");
        Calendar c = Calendar.getInstance(); 
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        TimeZone tz = c.getTimeZone();
        int dstoffsec =  (tz.getRawOffset() + (tz.inDaylightTime(c.getTime()) ? tz.getDSTSavings() : 0)) / 1000;
        int dstMinOffset = Math.abs(dstoffsec / 60) % 60;
        int dstHourOffset = (dstoffsec / (60*60)) + 12;
        if (mDebug)  CLog.out(TAG, "SEND STARTUP_UPDATE",dstHourOffset,dstMinOffset,dstoffsec);

        byte[] b = new byte[11];
        b[0] = (byte) (year & 0xFF);
        b[1] = (byte) (year >> 8);
        b[2] = (byte) (year >> 16);
        b[3] = (byte) (year >> 24);
        b[4] = (byte) month;
        b[5] = (byte) day;
        b[6] = (byte) c.get(Calendar.HOUR_OF_DAY);
        b[7] = (byte) c.get(Calendar.MINUTE);
        b[8] = (byte) c.get(Calendar.SECOND);
        b[9] = (byte)dstHourOffset;
        b[10] = (byte)dstMinOffset;
        
        CmdPacket cmdPacket = new CmdPacket(CameraProtocolOld.CMD_UPDATE_TIME, 0, b);
        sendPacket(cmdPacket);
    }

    @Override
    public void stopRecording() {
        CmdPacket stopRecord = new CmdPacket(CameraProtocolOld.CMD_RECORD_STREAM_STOP, 0, null);
        sendPacket(stopRecord);
    }

    private void sendPacket(Packet packet) {
//        if (mState == STATE_CONNECTED && mCommThread != null) {
//            // d("Sending: " + packet.toString());
//            mCommThread.write(packet.toByteArray());
//        }
        
        if (mState == STATE_CONNECTED) {
            mCallback.sendData(packet);
        }
    }

//    private class ConnectThread extends Thread {
//        private final BluetoothDevice mmDevice;
//        private BluetoothSocket       mmSocket;
//
//        ConnectThread(BluetoothDevice device) {
//            mmDevice = device;
//        }
//
//        @Override
//        public void run() {
//            // Establish the connection
//            try {
//                // Obtain a socket to the device
//
//                // Android >= 2.3.3 (API 10)
//                // mmSocket =
//                // mmDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);
//
//                // Android < 2.3.3
//                Method m = mmDevice.getClass().getMethod("createInsecureRfcommSocket", new Class[] { int.class });
//                mmSocket = (BluetoothSocket) m.invoke(mmDevice, 1);
//
//                // Attempt to connect
//                mmSocket.connect();
//
//                mCommThread = new CommThreadOld(mmSocket);
//                mCommThread.start();
//            } catch (Exception exc) {
//                e("Failed to connect to device.", exc);
//                connectionFailed(exc);
//                return;
//            }
//        }
//    }

//    private class CommThreadOld extends Thread {
//        private static final int MAX_PACKET_SIZE = 65535;
//
//        private BluetoothSocket  mmSocket;
//        private OutputStream     mmOutput;
//        private ByteBuffer       mmRecvBuffer    = ByteBuffer.allocateDirect(MAX_PACKET_SIZE);
//
//        CommThreadOld(BluetoothSocket socket) {
//            mmSocket = socket;
//            mmRecvBuffer.order(ByteOrder.LITTLE_ENDIAN);
//        }
//
//        @Override
//        public void run() {
//            // Obtain the streams and read from the input
//            try {
//                final int tenKb = 10 * 1024;
//                mmOutput = mmSocket.getOutputStream();
//                InputStream input = mmSocket.getInputStream();
//                byte[] buffer = new byte[tenKb];
//
//                AckPacket ack = new AckPacket();
//                write(ack.toByteArray());
//
//                CmdPacket cmd = new CmdPacket(CameraProtocolOld.CMD_PING, 0, null);
//                write(cmd.toByteArray());
//
//                // We're connected
//                connected();
//
//                for (;;) {
//                    int bytesRead = input.read(buffer);
//                    receivedData(buffer, bytesRead);
//                }
//            } catch (IOException e) {
//                disconnected();
//            }
//        }
//
//        public synchronized void write(byte[] bytes) {
//            try {
//                mmOutput.write(bytes);
//                mmOutput.flush();
//            } catch (Exception e) {
//                e("Unable to write to device: ", e);
//            }
//        }
//
//        public void cancel() {
//            try {
//                if (mmSocket != null) {
//                    mmSocket.close();
//                    mmSocket = null;
//                }
//            } catch (Exception e) {
//                e("Failed to close socket while canceling comms thread.", e);
//            }
//        }
//
//        private void receivedData(byte[] buffer, int len) {
//            if (mmRecvBuffer.remaining() < len) {
//                mmRecvBuffer.clear();
//                w("Buffer overflow would have occurred in receive buffer! Disconnecting to avoid crash.");
//
//                mHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        stop();
//                    }
//                });
//            }
//
//            // Copy over the received data
//            mmRecvBuffer.put(buffer, 0, len);
//
//            if (mmRecvBuffer.position() >= CameraProtocolOld.PacketHeader.LENGTH)
//                parsePackets();
//        }
//
//        private void parsePackets() {
//            int bufferEnd = mmRecvBuffer.position();
//            int dataRemaining = bufferEnd;
//
//            // Start at beginning of buffer
//            mmRecvBuffer.rewind();
//
//            // Check that enough data remains in the buffer to grab a packet
//            // header
//            while (dataRemaining >= CameraProtocolOld.PacketHeader.LENGTH) {
//                // Parse header
//                PacketHeader header = new PacketHeader(mmRecvBuffer);
//
//                // Check for header validity
//                if (!header.isValid()) {
//                    // Bogus header. Dump the buffer
//                    byte[] remaining = new byte[dataRemaining];
//                    mmRecvBuffer.get(remaining);
//
//                    StringBuilder sb = new StringBuilder();
//                    sb.append("Bogus packet header - " + header.toString());
//                    for (int i = 0; i < remaining.length; i++) {
//                        if (i % 10 == 0)
//                            sb.append("\n\t");
//                        sb.append(String.format("%02X ", remaining[i]));
//                    }
//
//                    e(sb.toString(), null);
//                    mmRecvBuffer.clear();
//                    disconnect();
//                    return;
//                }
//
//                // Check if there is enough data to deserialize the rest of the
//                // packet
//                if (dataRemaining < header.length)
//                    break;
//
//                // Deserialize the rest of the packet
//                Packet packet = CameraProtocolOld.deserializePacket(header, mmRecvBuffer);
//                if (packet != null)
//                    receivedPacket((BasePacket) packet);
//                else {
//                    e("Caught an exception while parsing a packet.  Forcing disconnection", null);
//                    disconnect();
//                    return;
//                }
//
//                // Adjust the data remaining (includes the header)
//                dataRemaining -= header.length;
//            }
//
//            // Remove the packet data from the buffer, and place the
//            // remaining data at the start of the buffer.
//            if (dataRemaining > 0 && dataRemaining != bufferEnd) {
//                mmRecvBuffer.position(bufferEnd - dataRemaining);
//
//                // Temporary swap buffer
//                byte[] swap = new byte[dataRemaining];
//
//                // Grab the remaining data in the buffer
//                mmRecvBuffer.get(swap);
//                // Clear out the buffer
//                mmRecvBuffer.clear();
//                // Place the remaining data at the beginning of the buffer
//                mmRecvBuffer.put(swap);
//
//                // Set the new end of the buffer
//                bufferEnd = dataRemaining;
//            } else if (dataRemaining == 0)
//                bufferEnd = 0;
//
//            // Move the buffer position to the end
//            mmRecvBuffer.position(bufferEnd);
//        }
//    }

    /*
     * private class ServerThread extends Thread { private BluetoothServerSocket
     * mmServerSocket;
     * 
     * public ServerThread() { mmServerSocket = null;
     * 
     * // Create a new listening server socket try { mmServerSocket =
     * BluetoothAdapter
     * .getDefaultAdapter().listenUsingRfcommWithServiceRecord(NAME, MY_UUID); }
     * catch(IOException e) { Log.e(TAG, "listen() failed", e); } }
     * 
     * public void run() { if(mmServerSocket == null) return;
     * 
     * setName("AcceptThread"); BluetoothSocket socket = null;
     * 
     * // Listen to the server socket if we're not connected while(true) { try {
     * i("Listening for a BT connection...");
     * 
     * // This is a blocking call and will only return on a // successful
     * connection or an exception socket = mmServerSocket.accept(); }
     * catch(IOException e) { w("accept() exception: " + e.toString()); break; }
     * 
     * // If a connection was accepted if(socket != null) {
     * i("Got a connection, disconnecting from any current connection.");
     * disconnect();
     * 
     * i("Spinning up comm thread..."); mCommThread = new CommThread(socket);
     * mCommThread.start(); } }
     * 
     * w("Exiting accept thread..."); }
     * 
     * public void cancel() { try { i("Shutting down server socket.");
     * mmServerSocket.close(); } catch(IOException e) { Log.e(TAG,
     * "close() of server failed", e); } } }
     */

    private class GPSDownloadThread extends Thread {
        private static final String ALMANAC_URL      = "http://alp.u-blox.com/current_7d.alp";
        public static final String  ALMANAC_FILENAME = "current_7d.alp";

        boolean                     mFileUpToDate    = false;

        public GPSDownloadThread() {
            final long oneDay = (1000 * 60) * 60 * 24;

            File targetFile = new File(mContext.getFilesDir(), ALMANAC_FILENAME);
            long lastModified = targetFile.lastModified();

            // lastModified will be 0 if file doesn't exist
            mFileUpToDate = (System.currentTimeMillis() - lastModified) < oneDay;
        }

        @Override
        public void run() {
            if (mFileUpToDate) {
                gpsFilePrepared();
                return;
            }

            try {

                URL url = new URL(ALMANAC_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.connect();

                BufferedInputStream input = new BufferedInputStream(connection.getInputStream());
                BufferedOutputStream output = new BufferedOutputStream(mContext.openFileOutput(ALMANAC_FILENAME + ".incomplete", 0));

                int contentLen = connection.getContentLength();
                int bytesRead = 0;

                byte[] buffer = new byte[1024];
                while (bytesRead < contentLen) {
                    int read = input.read(buffer);
                    bytesRead += read;
                    output.write(buffer, 0, read);
                }

                output.close();
                connection.disconnect();

                File file = new File(mContext.getFilesDir(), ALMANAC_FILENAME + ".incomplete");
                file.renameTo(new File(mContext.getFilesDir(), ALMANAC_FILENAME));

                gpsFilePrepared();
            } catch (Exception e) {
                CLog.e(e,"Error downloading GPSAssist file ");
            }
        }
    }

//    private class GPSUploadThread extends Thread {
//        private ProgressDialog             mmProgressDialog;
//        private Iterator<GPSAssistPayload> mmCurrentGPSPayload;
//        private GPSAssistPayload           mmPayload;
//        private boolean                    mmCanceled;
//        private int                        mmWaitForAck;
//
//        private Handler                    mmProgressUpdater;
//
//        public GPSUploadThread() {
//            mmCanceled = false;
//
//            mmProgressUpdater = new Handler() {
//                @Override
//                public void dispatchMessage(Message msg) {
//                    int progress = msg.arg1;
//                    mmProgressDialog.setProgress(progress);
//                }
//            };
//
//            mmCurrentGPSPayload = mGPSPayloads.iterator();
//            mmPayload = mmCurrentGPSPayload.next();
//
//            mmProgressDialog = new ProgressDialog(mContext);
//            mmProgressDialog.setTitle("Updating GPS");
//            mmProgressDialog.setMessage("This should only take a moment...");
//            mmProgressDialog.setMax(mGPSPayloads.size());
//            mmProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//            mmProgressDialog.setCancelable(true);
//            mmProgressDialog.setOnCancelListener(new OnCancelListener() {
//                @Override
//                public void onCancel(DialogInterface arg0) {
//                    GPSUploadThread.this.cancel();
//                    Toast.makeText(mContext, "GPSAssist Upload Canceled", Toast.LENGTH_SHORT).show();
//                }
//            });
//            mmProgressDialog.show();
//        }
//
//        @Override
//        public synchronized void run() {
//            i("Beginning GPSAssist Upload");
//
//            while (mmPayload != null && !mmCanceled) {
//                // Update the progress dialog
//                mmProgressUpdater.obtainMessage(0, mmPayload.getPacketNumber(), 0).sendToTarget();
//
//                i(String.format("Sending payload %d/%d", mmPayload.getPacketNumber(), mmPayload.getTotalPackets()));
//
//                // Send the payload
//                CmdPacket pkt = new CmdPacket(CameraProtocolOld.CMD_SET_GPSASSIST, 0, mmPayload.toByteArray());
//                mmWaitForAck = pkt.cmd_id;
//                sendPacket(pkt);
//
//                // Begin waiting for the ack, at most 1 second
//                // before resending packet, or the next one.
//                // Receiving an ack or resend req will interrupt here.
//                try {
//                    wait(1000);
//                } catch (InterruptedException e) {
//                }
//            }
//
//            try {
//                mmProgressDialog.dismiss();
//                resumeStatusUpdates();
//                startVideoStream();
//
//                mHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(mContext, "GPSAssist Successfully Updated", Toast.LENGTH_SHORT).show();
//                    }
//                });
//            } catch (Exception e) {
//                // We don't care if anything fails here
//            }
//
//            mGPSUpload = null;
//            i("Finished GPS Upload");
//        }
//
//        public synchronized void ackReceived(int ackID) {
//            if (ackID == mmWaitForAck) {
//                if (mmCurrentGPSPayload.hasNext())
//                    mmPayload = mmCurrentGPSPayload.next();
//                else
//                    mmPayload = null;
//
//                notifyAll();
//            }
//        }
//
//        public synchronized void resendReceived(int ackID) {
//            i("Received GPSAssist resend request.");
//
//            // Do not advance payload so that the current one is resent.
//            notifyAll();
//        }
//
//        public synchronized void cancel() {
//            mmCanceled = true;
//            mGPSUpload = null;
//
//            notifyAll();
//        }
//    }

    public int getMajorVersion() {
        return mMajorVersion;
    }

    public int getMinorVersion() {
        return mMinorVersion;
    }

    public int getBuildNumber() {
        return mBuildNumber;
    }

    public String getVersionString() {
        return String.format("%d.%d.%d", mMajorVersion, mMinorVersion, mBuildNumber);
    }

    @SuppressWarnings("unused")
    private void dumpBuffer(ByteBuffer bb, int size) {
        int position = bb.position();
        bb.rewind();

        byte[] buffer = new byte[size];
        bb.get(buffer);

        StringBuilder sb = new StringBuilder();
        sb.append("Dumping buffer:\n\t");
        for (int i = 0; i < size; i++) {
            sb.append(String.format("%02X ", buffer[i]));
        }
        Log.d(TAG, sb.toString());

        bb.position(position);
    }

    @SuppressWarnings("unused")
    private void dumpBuffer(byte[] buffer, int len) {
        if (len < 0)
            len = buffer.length;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (i % 32 == 0 && i > 0)
                sb.append("\n\t");
            sb.append(String.format("%02X ", buffer[i]));
        }
        Log.d(TAG, sb.toString());
    }

    @Override
    public void updateSettings(int switchPos, int categoryPosition, int itemPosition) {
        if(mSettings == null) {
            CLog.out(TAG,"CAMERA SETTINGS NULL");
            return;
        }
        mSettings.switchPosition = (byte) switchPos;

         switch(categoryPosition) {
         case SettingsFragment.SETTINGS_ITEM_MODE: 
            mSettings.setVideoResByIndex(itemPosition);
             break;
         case SettingsFragment.SETTINGS_ITEM_QUALITY:
             mSettings.setQuality(itemPosition);
             break;
         case SettingsFragment.SETTINGS_ITEM_PHOTO_FREQUENCY:
             mSettings.setPhotoModeByIndex(itemPosition);
             break;
         case SettingsFragment.SETTINGS_ITEM_METERING:
             mSettings.setMeteringMode(itemPosition);
             break;
         case SettingsFragment.SETTINGS_ITEM_WHITE_BALANCE:
             mSettings.setWhiteBalance((byte) itemPosition);
             break;
         case SettingsFragment.SETTINGS_ITEM_CAMERA_BEEPS:
             boolean currentBeep = mSettings.getBeep();
             mSettings.setBeep(!currentBeep);
             break;
         case SettingsFragment.SETTINGS_ITEM_GPS:
             boolean currentGps = mSettings.getGps();
             mSettings.setGps(!currentGps);
             break;
         case SettingsFragment.SETTINGS_ITEM_GPS_UPDATE_RATE:
             mSettings.setGpsRate((byte) itemPosition);
             break;
         case SettingsFragment.SETTINGS_ITEM_CONTRAST:  
             mSettings.setContrast(itemPosition);
             break;
         case SettingsFragment.SETTINGS_ITEM_EXPOSURE:
             mSettings.setExposure(itemPosition);
             break;
         case SettingsFragment.SETTINGS_ITEM_SHARPNESS:
             mSettings.setSharpness(itemPosition);
             break;
         case SettingsFragment.SETTINGS_ITEM_VIDEO_FORMAT:
             mSettings.setVideoFormat(itemPosition);
             break;
         case SettingsFragment.SETTINGS_ITEM_INTERNAL_MIC:
             mSettings.setInternalMic(itemPosition);
             break;
         case SettingsFragment.SETTINGS_ITEM_EXTERNAL_MIC:
             mSettings.setExternalMic((byte) itemPosition);
            break;
         }
         if(switchPos == 1) {
             mSettings1 = mSettings.convertToOld(switchPos);
             mSettings2.videoRefresh = mSettings1.videoRefresh;
         } else {
             mSettings2 = mSettings.convertToOld(switchPos);
             mSettings1.videoRefresh = mSettings2.videoRefresh;
         }
         commitSettings(1);
//         this.mTriggerRequests.put(CameraProtocolOld.CMD_CUSTOM, new RequestFunction() {
//             @Override
//             public void doRequest() {
//                 commitSettings(2);
//             }
//         });
         mHandler.removeCallbacks(mSendSwitch2SettingsRunnable);
         mHandler.postDelayed(this.mSendSwitch2SettingsRunnable, 1000);    
    }

    @Override
    public void sendDefaultSettings() {
        mSettings = CameraSettings.defaultSettings(mModel);
        mSettings1 = mSettings.convertToOld(1);
        mSettings2 = mSettings.convertToOld(2);
        this.commitSettings(1);
//        this.mTriggerRequests.put(CameraProtocolOld.CMD_CUSTOM, new RequestFunction() {
//            @Override
//            public void doRequest() {
//                commitSettings(2);
//            }
//        });
        mHandler.removeCallbacks(mSendSwitch2SettingsRunnable);
        mHandler.postDelayed(this.mSendSwitch2SettingsRunnable, 1000);
    }

    @Override
    public void sendSettings() {
        //updateSettings() handles this
//        this.commitSettings();
    }

    @Override
    public void sendSetCameraConfigure() {
//        this.requestSettings();        
    }

    @Override
    public void requestSettings() {
        this.requestSettings(1);
        this.requestSettings(2);
    }
}
