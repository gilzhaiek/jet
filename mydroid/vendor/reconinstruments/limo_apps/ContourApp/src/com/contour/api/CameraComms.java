 package com.contour.api;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import android.R;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;

import com.contour.api.CameraProtocol.AckPacket;
import com.contour.api.CameraProtocol.CmdPacket;
import com.contour.api.CameraProtocol.GPSAssistPayload;
import com.contour.api.CameraProtocol.Packet;
import com.contour.connect.SettingsFragment;
import com.contour.connect.debug.CLog;
import com.contour.utils.SharedPrefHelper;

public class CameraComms extends BaseCameraComms {

    static final String TAG = "CameraComms";
    

    // private static final String NAME = "contour.android";
    // private static final UUID MY_UUID =
    // UUID.fromString("00000000-DECA-FADE-DECA-DEAFDECACAFF");


    // State of the service
    public static final int        STATE_IDLE               = 0;
    public static final int        STATE_CONNECTING         = 1;
    public static final int        STATE_CONNECTED          = 2;

    // Messages passed via handler


    // Camera Status
    CameraStatus                   mStatus;
    private int                    mMajorVersion;
    private int                    mMinorVersion;
    private int                    mBuildNumber;
    private int                    mModel = BaseCameraComms.MODEL_PLUS_2;

    // Camera Settings
    private CameraSettings         mSettings;
    
    private int                    mCommitDelay             = 0;

    private int                    mState                   = STATE_IDLE;
    private boolean                mStatusUpdates           = true;
//    private Boolean                mWaitingForVFrame        = false;
    private Boolean                mRecordStarting          = false;
    private Boolean                mRecordStopping          = false;

    private GPSUploadThread        mGPSUpload;
    private boolean                mFileIsPrepared          = false;
    private List<GPSAssistPayload> mGPSPayloads;

    private int                    mRetainCount;
    private CameraCommsCallback    mCallback;

	private final Runnable mStatusRunnable = new Runnable() {
		@Override
		public void run() {
			CmdPacket statusReq = new CmdPacket(
					(byte) CameraProtocol.CMD_GET_CAMERA_STATUS, 0, null);
			sendPacket(statusReq);
			mHandler.postDelayed(this, 5000);
		}
	};

    public CameraComms(Context context, CameraCommsCallback callback) {
        super(context);
        mCommitDelay = 1500;
        mCallback = callback;
        this.mTriggerRequests.put(CameraProtocol.CMD_REQUEST_IDENTIFY, new RequestFunction() {
            @Override
            public void doRequest() {
                sendStartupUpdate();
                requestStatus();
            }
        });
        this.mTriggerRequests.put(CameraProtocol.CMD_RET_CAMERA_STATUS_NEW, new RequestFunction() {
            @Override
            public void doRequest() {
                requestSettings();
                startVideoStream();
            }
        });
        // Begin the download process
        // (new GPSDownloadThread()).start();
    }

    public final int getState() {
        return mState;
    }

    public void retain() {
        ++mRetainCount;
    }

    public void release() {
        if (mRetainCount > 0)
            --mRetainCount;
        else {
            if (mDebug) CLog.out(TAG, "Service was overreleased.");
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
            if (mDebug) CLog.out(TAG, "Disconnecting due to retain timeout.");
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

        if (mGPSUpload == null) {
            pauseStatusUpdates();
            stopVideoStream();

            mGPSUpload = new GPSUploadThread();
            mGPSUpload.start();
        } else
            return false;

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
        if (mDebug) CLog.out(TAG, "Connected to device.");

        switchState(STATE_CONNECTED);
        broadcast(ACTION_CONNECTED);

        // requestStatus();
        // requestSettings(0);
        // requestSettings(1);
    }   

    public void disconnected() {
        switchState(STATE_IDLE);

        mHandler.removeCallbacks(mStatusRunnable);
        if (mDebug) CLog.out(TAG, "Disconnected from device.");

        if (mGPSUpload != null)
            mGPSUpload.cancel();

        broadcast(ACTION_DISCONNECTED);

        mSettings = null;

        mRecordStarting = false;
        mRecordStopping = false;
    }

    public void receivedPacket(BasePacket packet) {
//        if (D) CLog.out(TAG, "receivedPacket", Arrays.toString(packet.toByteArray()));

        switch (packet.getHeaderType()) {
        case CameraProtocol.PT_ACK: {
            AckPacket ack = (AckPacket) packet;

            if (mGPSUpload != null)
                mGPSUpload.ackReceived(ack.header.pkt_id);

            break;
        }

        case CameraProtocol.PT_CMD: {
            // Handle the packet
            CmdPacket cmdPkt = (CmdPacket) packet;
            receivedCmd(cmdPkt);

            // Send an ack to the packet
            AckPacket ack = new AckPacket(cmdPkt.getPacketId());            
            sendPacket(ack);
            break;
        }
        }
    }

    private void receivedCmd(CmdPacket packet) {
        int cmdId = packet.cmd & 0xff;
        switch (cmdId) {
        case CameraProtocol.CMD_REQUEST_IDENTIFY: {
            
            mMajorVersion = ((packet.data[0] << 8) & 0xFF) | (packet.data[1] & 0xFF);
             mMinorVersion =  packet.data[2] & 0xFF;
             mBuildNumber = packet.data[3] & 0xFF;
             if (mDebug) CLog.out(TAG, "Recv CMD_REQUEST_IDENTIFY",mMajorVersion,mMinorVersion,mBuildNumber,Arrays.toString(packet.data));

           
        } break;
        case CameraProtocol.CMD_RET_CAMERA_STATUS_NEW:
        case CameraProtocol.CMD_RET_CAMERA_STATUS: {
            this.handleReceivedStatus(packet.data);
            break;
        }

        case CameraProtocol.CMD_RET_CAMERA_SETTINGS: {
            if (mDebug) CLog.out(TAG, "PRE RECV CMD_RET_CAMERA_SETTINGS", packet.header);
            handleReceivedSettings(packet.data);
            break;
        }

        case CameraProtocol.CMD_RECORD_STREAM_STOPPED: {
        	handleRecordStopped();
            break;
        }

        case CameraProtocol.CMD_RECORD_STREAM_STARTED: {
        	handleRecordStarted();
            break;
        }
        case CameraProtocol.CMD_JPEG: {
            this.handleJpegReceived(packet);
            break;
        }
        case CameraProtocol.CMD_RET_CAMERA_CONFIGURE: {
            if (mDebug) CLog.out(TAG, "Recv CMD_RET_CAMERA_CONFIGURE",packet);
            handleCameraConfigureReceived(packet.data[0]);
            break;
        }
        case CameraProtocol.CMD_VIDEO_STREAM_STARTED: {
            if (mDebug) CLog.out(TAG, "Recv CMD_VIDEO_STREAM_STARTED");
            break;
        }
        case CameraProtocol.CMD_VIDEO_STREAM_STOPPED: {
            if (mDebug) CLog.out(TAG, "Recv CMD_VIDEO_STREAM_STOPPED");

            break;
        } 
        }
        RequestFunction f = mTriggerRequests.get(cmdId);
        mTriggerRequests.delete(cmdId);
        if(f != null) {
            f.doRequest();
        }
    }
    
    private void handleRecordStopped() {
        if (mDebug) CLog.out(TAG, "CMD_RECORD_STREAM_STOPPED.");
        broadcast(ACTION_RECORDING_STOPPED);
        //TODO
      	/*
		 * if([CTRSettingsUtil sharedInstance].cameraNeedsConfigureMessage) {
		 * 	[self sendCameraConfigure]; [CTRSettingsUtil
		 * 	sharedInstance].cameraNeedsConfigureMessage = NO; 
		 * } else {
		 * 	_settingsEnabled = YES; 
		 * [self liveVideoStart]; 
		 * }
		 */
    }
    
    int mCurrentCameraState = CameraStatus.CAMERA_STATE_UNKNOWN;
    private void handleReceivedStatus(byte[] bytes) {
        mStatus = CameraStatus.decodeByteArray(bytes);
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
    
    private void handleReceivedSettings(byte[] bytes) {
        CameraSettings settings = CameraSettings.decodeByteArray(bytes);
        mModel = settings.cameraModel;
        if(this.mStatus != null) {
            settings.switchPosition = this.mStatus.switchPos;
            mSettings = settings;
            if (mDebug) CLog.out(TAG, "RECV CMD_RET_CAMERA_SETTINGS", settings);
            broadcast(ACTION_SETTINGS, settings);
        } else {
            if (mDebug) CLog.out(TAG, "ERROR RECV CMD_RET_CAMERA_SETTINGS BEFORE STATUS", settings);
            this.requestSettings();
        }
        
//        mCallback.onSettngsReceived(settings, 0);
    }
    
    private void handleRecordStarted() {
        if (mDebug) CLog.out(TAG, "CMD_RECORD_STREAM_STARTED.");
      broadcast(ACTION_RECORDING_STARTED);
    }
    
//    BitmapFactory.Options options = new BitmapFactory.Options();
    private void handleJpegReceived(CmdPacket cp) {
        Bitmap bmp = BitmapFactory.decodeByteArray(cp.data, 0, cp.mDataLength);
        if(bmp!=null) {
            this.mCallback.onVideoFrameReceived(bmp);
//            bmp.recycle();
        }
    }
    
    private void handleCameraConfigureReceived(int state) {
        if (mDebug) CLog.out(TAG, "CMD_RET_CAMERA_CONFIGURE",state);

    	if(state == 1) {
    		this.resumeStatusUpdates();
    		this.requestSettings();
    		this.startVideoStream();

    		//enable settings video
            broadcast(ACTION_CAMERA_CONFIGURE_ENABLED);

    	} else {
    		this.pauseStatusUpdates();
    		//disable settings, video
            broadcast(ACTION_CAMERA_CONFIGURE_DISABLED);
    	}
    }
    
    public void connect(BluetoothDevice device) {
        disconnect();

        switchState(STATE_CONNECTING);
        broadcast(ACTION_CONNECTING);

        if (mDebug) CLog.out(TAG, "Connecting to device: " + device.getName().trim());

        // Start connect thread
    }

    public void disconnect() {

    }

    public int getModel() {
        return mModel;
    }

    public CameraSettings getSettings(int switchPosition) {
        return mSettings;
    }

    public void updateSettings(int switchPos, int categoryPosition, int itemPosition) {
        //TODO 
        //Update Switch Position on Settings
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
         broadcast(ACTION_SETTINGS, mSettings);
    }
    
    public void sendDefaultSettings() {
        mSettings = CameraSettings.defaultSettings(mModel);
        final CmdPacket setSettings = new CmdPacket((byte) CameraProtocol.CMD_SET_CAMERA_SETTINGS, 0, mSettings.toByteArray());
        sendPacket(setSettings);
        if (mDebug) CLog.out(TAG, "Sending Default Settings: " + mSettings.toString());
    }
    
    public void setCommitDelay(int msDelay) {
        mCommitDelay = msDelay;
    }

    public int getCommitDelay() {
        return mCommitDelay;
    }

    public void sendSettings() {
        if (mSettings != null) {
        	final CmdPacket setSettings = new CmdPacket((byte) CameraProtocol.CMD_SET_CAMERA_SETTINGS, 0, mSettings.toByteArray());
            sendPacket(setSettings);

            // d("Settings 1 data: ");
            // dumpBuffer(setSettings1.toByteArray(), -1);
        }

        if (mDebug) CLog.out(TAG, "Sending Settings: " + mSettings.toString());

        // Clear the settings and request a new set
//        mSettings = null;
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                requestSettings();
//            }
//        }, mCommitDelay + 1000);
    }

    private void requestStatus() {
   	  	 mHandler.removeCallbacks(mStatusRunnable);
   	 
         if(isConnected() && mStatusUpdates)
         {
             CmdPacket statusReq = new CmdPacket((byte) CameraProtocol.CMD_GET_CAMERA_STATUS, 0, null);
             sendPacket(statusReq);
             mHandler.postDelayed(mStatusRunnable, 5000);
             if (mDebug) CLog.out(TAG, "Sending request status command.");
         }
    }

    public void requestSettings() {
        if (isConnected()) {
            CmdPacket settingsReq = new CmdPacket((byte) CameraProtocol.CMD_GET_CAMERA_SETTINGS, 0, null);
            sendPacket(settingsReq);
            if (mDebug) CLog.out(TAG, "Sending request settings command.");


            // Make sure that this setting has been set 2s from now
            // final int switchPos = switchPosition;
            // mHandler.postDelayed(new Runnable() {
            // @Override
            // public void run()
            // {
            // requestSettings(switchPos);
            // }
            // }, 2000);
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

    public void startRecording() {
        if (mDebug) CLog.out(TAG, "Sending record start command.");

        CmdPacket startRecCmd = new CmdPacket((byte) CameraProtocol.CMD_RECORD_STREAM_START);
        sendPacket(startRecCmd);

//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (mRecordStarting)
//                    startRecording();
//            }
//        }, 2000);
    }

    public void stopRecording() {
        if (mDebug) CLog.out(TAG, "Sending record stop command.");

//        synchronized (mRecordStopping) {
//            mRecordStopping = true;
//        }

        CmdPacket stopRecCmd = new CmdPacket((byte) CameraProtocol.CMD_RECORD_STREAM_STOP);
        sendPacket(stopRecCmd);

//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (mRecordStopping)
//                    stopRecording();
//            }
//        }, 2000);
    }

    public void startVideoStream() {
        if (!isConnected())
            return;

        if (mDebug) CLog.out(TAG, "SEND VIDEO_STREAM_START");

//        synchronized (mWaitingForVFrame) {
//            mWaitingForVFrame = true;
//        }

        CmdPacket startVideo = new CmdPacket((byte) CameraProtocol.CMD_VIDEO_STREAM_ON, 0, null);
        sendPacket(startVideo);

//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (mWaitingForVFrame)
//                    startVideoStream();
//            }
//        }, 1500);
    }

    public void stopVideoStream() {
        if (mDebug) CLog.out(TAG, "SEND VIDEO_STREAM_STOP");

        CmdPacket stopVideo = new CmdPacket((byte) CameraProtocol.CMD_VIDEO_STREAM_OFF, 0, null);
        sendPacket(stopVideo);
    }
    
    public void sendStartupUpdate() {

        Calendar c = Calendar.getInstance(); 
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        TimeZone tz = c.getTimeZone();
        int dstoffsec =  (tz.getRawOffset() + (tz.inDaylightTime(c.getTime()) ? tz.getDSTSavings() : 0)) / 1000;
        int dstMinOffset = Math.abs(dstoffsec / 60) % 60;
        int dstHourOffset = (dstoffsec / (60*60)) + 12;
        int hour =  c.get(Calendar.HOUR_OF_DAY);
        int min = c.get(Calendar.MINUTE);
        int sec =  c.get(Calendar.SECOND);
        if (mDebug)  CLog.out(TAG, "SEND STARTUP_UPDATE",year,month,day,hour,min,sec,dstHourOffset,dstMinOffset,dstoffsec);

        byte[] b = new byte[11];
        b[0] = (byte) (year & 0xFF);
        b[1] = (byte) (year >> 8);
        b[2] = (byte) (year >> 16);
        b[3] = (byte) (year >> 24);
        b[4] = (byte) month;
        b[5] = (byte) day;
        b[6] = (byte) hour;
        b[7] = (byte) min;
        b[8] = (byte) sec;
        b[9] = (byte)dstHourOffset;
        b[10] = (byte)dstMinOffset;
        
        CmdPacket cmdPacket = new CmdPacket((byte) CameraProtocol.CMD_STARTUP_UPDATE, 0, b);
        sendPacket(cmdPacket);
    }
    
    public void sendSetCameraConfigure() {
        if (mDebug)  CLog.out(TAG, "SEND SET_CAMERA_CONFIGURE");
        CmdPacket cmdPacket = new CmdPacket((byte) CameraProtocol.CMD_SET_CAMERA_CONFIGURE);
        sendPacket(cmdPacket);
    }

    private void sendPacket(Packet packet) {
        if (mState == STATE_CONNECTED) {
            mCallback.sendData(packet);
        }
    }

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
                if (mDebug) CLog.out(TAG, "Local GPSAssist file is up to date.");
                gpsFilePrepared();
                return;
            }

            try {
                if (mDebug) CLog.out(TAG, "Local GPSAssist file not present or out of date, downloading new file...");

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
                if (mDebug) CLog.out(TAG, "Error downloading GPSAssist file: ", e);
            }
        }
    }

    class GPSUploadThread extends Thread {
        private ProgressDialog             mmProgressDialog;
        private Iterator<GPSAssistPayload> mmCurrentGPSPayload;
        private GPSAssistPayload           mmPayload;
        private boolean                    mmCanceled;
        private int                        mmWaitForAck;

//        private Handler                    mmProgressUpdater;

        public GPSUploadThread() {
            mmCanceled = false;

//            mmProgressUpdater = new Handler() {
//                @Override
//                public void dispatchMessage(Message msg) {
//                    int progress = msg.arg1;
//                    mmProgressDialog.setProgress(progress);
//                }
//            };

            mmCurrentGPSPayload = mGPSPayloads.iterator();
            mmPayload = mmCurrentGPSPayload.next();

            mmProgressDialog = new ProgressDialog(mContext);
            mmProgressDialog.setTitle("Updating GPS");
            mmProgressDialog.setMessage("This should only take a moment...");
            mmProgressDialog.setMax(mGPSPayloads.size());
            mmProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mmProgressDialog.setCancelable(true);
            mmProgressDialog.setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface arg0) {
                    GPSUploadThread.this.cancel();
                    Toast.makeText(mContext, "GPSAssist Upload Canceled", Toast.LENGTH_SHORT).show();
                }
            });
            mmProgressDialog.show();
        }

        @Override
        public synchronized void run() {
            if (mDebug) CLog.out(TAG, "Beginning GPSAssist Upload");

            while (mmPayload != null && !mmCanceled) {
                // Update the progress dialog
//                mmProgressUpdater.obtainMessage(0, mmPayload.getPacketNumber(), 0).sendToTarget();

                if (mDebug) CLog.out(TAG, String.format("Sending payload %d/%d", mmPayload.getPacketNumber(), mmPayload.getTotalPackets()));

                // Send the payload
                CmdPacket pkt = new CmdPacket((byte) CameraProtocol.CMD_SET_GPSASSIST, 0, mmPayload.toByteArray());
                mmWaitForAck = pkt.header.pkt_id;
                sendPacket(pkt);

                // Begin waiting for the ack, at most 1 second
                // before resending packet, or the next one.
                // Receiving an ack or resend req will interrupt here.
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                }
            }

            try {
                mmProgressDialog.dismiss();
                resumeStatusUpdates();
                startVideoStream();
            } catch (Exception e) {
                // We don't care if anything fails here
            }

            mGPSUpload = null;
            if (mDebug) CLog.out(TAG, "Finished GPS Upload");
        }

        public synchronized void ackReceived(int ackID) {
            if (ackID == mmWaitForAck) {
                if (mmCurrentGPSPayload.hasNext())
                    mmPayload = mmCurrentGPSPayload.next();
                else
                    mmPayload = null;

                notifyAll();
            }
        }

        public synchronized void resendReceived(int ackID) {
            if (mDebug) CLog.out(TAG, "Received GPSAssist resend request.");

            // Do not advance payload so that the current one is resent.
            notifyAll();
        }

        public synchronized void cancel() {
            mmCanceled = true;
            mGPSUpload = null;

            notifyAll();
        }
    }

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
}
