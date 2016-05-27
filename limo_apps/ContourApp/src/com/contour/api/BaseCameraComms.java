package com.contour.api;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.SparseArray;


public abstract class BaseCameraComms {
    
    interface RequestFunction {
        public void doRequest();
    }
    
    // Device Models
    public static final int        MODEL_GPS                = 0;
    public static final int        MODEL_PLUS               = 1;
    public static final int        MODEL_PLUS_2             = 2;
    
    public static final String     ACTION_LOG               = "com.contour.actions.log";
    public static final String     ACTION_STATECHANGE       = "com.contour.actions.stateChange";
    public static final String     ACTION_DISCONNECTED      = "com.contour.actions.disconnected";
    public static final String     ACTION_CONNECTFAILED     = "com.contour.actions.connectFailed";
    public static final String     ACTION_CONNECTING        = "com.contour.actions.connecting";
    public static final String     ACTION_CONNECTED         = "com.contour.actions.connected";
    public static final String     ACTION_VIDEOFRAME        = "com.contour.actions.videoFrame";
    public static final String     ACTION_SETTINGS          = "com.contour.actions.settings";

    public static final String     ACTION_STATUS            = "com.contour.actions.status";
    public static final String     ACTION_INCOMPAT_FW       = "com.contour.actions.incompatibleFirmware";
    public static final String     ACTION_RECORDING_STARTED = "com.contour.actions.recordingStarted";
    public static final String     ACTION_RECORDING_STOPPED = "com.contour.actions.recordingStopped";
    
    public static final String     ACTION_CAMERA_CONFIGURE_ENABLED = "com.contour.actions.configureEnabled";
    public static final String     ACTION_CAMERA_CONFIGURE_DISABLED = "com.contour.actions.configureDisabled";

    // Extras in the broadcast
    public static final String     EXTRA_BYTEARRAY          = "extra.byteArray";
    public static final String     EXTRA_PARCELABLE         = "extra.parcelable";
    public static final String     EXTRA_INTEGER            = "extra.integer";
    
    final boolean mDebug;

    protected final SparseArray<RequestFunction> mTriggerRequests = new SparseArray<RequestFunction>(10);
    public static Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {

        }
    };
    
    protected final Context          mContext;
    protected BaseCameraComms(Context context) {
        mContext = context;
        mDebug = context.getResources().getBoolean(com.contour.connect.R.bool.debug_enabled);
    }

   
    public abstract void startVideoStream();
    public abstract void stopVideoStream();
    public abstract void startRecording();
    public abstract void stopRecording();
    public abstract void updateSettings(int switchPos, int categoryPosition, int itemPosition);
    public abstract void sendDefaultSettings();
    public abstract void sendSettings();
    public abstract void sendSetCameraConfigure();
    public abstract void disconnected();
    public abstract void connected();
    public abstract void receivedPacket(BasePacket packet);
    public abstract void requestSettings();
    public abstract void pauseStatusUpdates();

    
    protected void broadcast(String action) {
        Intent i = new Intent(action);
        mContext.sendBroadcast(i);
    }

    protected void broadcastOrdered(String action) {
        Intent i = new Intent(action);
        mContext.sendOrderedBroadcast(i, null);
    }
    
    protected void broadcast(String action, Parcelable extra) {
        Intent i = new Intent(action);
        i.putExtra(EXTRA_PARCELABLE, extra);
        mContext.sendBroadcast(i);
    }
    
    protected void broadcastOrdered(String action, Parcelable extra) {
        Intent i = new Intent(action);
        i.putExtra(EXTRA_PARCELABLE, extra);
        mContext.sendOrderedBroadcast(i, null);
    }

    protected void broadcast(String action, byte[] extra) {
        Intent i = new Intent(action);
        i.putExtra(EXTRA_BYTEARRAY, extra);
        mContext.sendBroadcast(i);
    }

    protected void broadcast(String action, String extra) {
        Intent i = new Intent(action);
        i.putExtra(Intent.EXTRA_TEXT, extra);
        mContext.sendBroadcast(i);
    }

    protected void broadcast(String action, Throwable extra) {
        broadcast(action, extra.getMessage());
    }
}
