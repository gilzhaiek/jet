package com.reconinstruments.gazedetectionapp;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.reconinstruments.os.hardware.gaze.HUDGazeManager;
import com.reconinstruments.os.hardware.gaze.GazeCalibrationListener;
import com.reconinstruments.os.hardware.screen.HUDScreenManager;

import java.util.ArrayList;

public class GazeAppService extends Service implements GazeCalibrationListener{
    private static final String TAG = "GazeAppService";
    private static MediaPlayer mPlayer = null;

    private HUDGazeManager mGazeMgr = null;
    private HUDScreenManager mScreenMgr = null;

    private ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_REGISTER_GAZE_DETECT = 3;
    static final int MSG_UNREGISTER_GAZE_DETECT = 4;
    static final int MSG_REGISTER_GAZE_CALIBRATE = 5;
    static final int MSG_UNREGISTER_GAZE_CALIBRATE = 6;
    static final int MSG_AHEAD_CALIBRATE = 7;
    static final int MSG_DISPLAY_CALIBRATE = 8;

    // Added by ShaneJ
    static final int MSG_AHEAD_CAL_DONE = 9;
    static final int MSG_DISPLAY_CAL_DONE = 10;


    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    if (!mClients.contains(msg.replyTo)) {
                        mClients.add(msg.replyTo);
                    }
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_REGISTER_GAZE_DETECT:
                    Log.d(TAG,"Received message to start gaze detect");
                    if (mScreenMgr == null) {
                        mScreenMgr = HUDScreenManager.getInstance();
                        //mScreenMgr.setScreenOffDelay(5000);
                    }
                    mScreenMgr.registerToGaze();
                    break;
                case MSG_UNREGISTER_GAZE_DETECT:
                    Log.d(TAG,"Received message to stop gaze detect");
                    if (mScreenMgr == null) {
                        mScreenMgr = HUDScreenManager.getInstance();
                        //mScreenMgr.setScreenOffDelay(5000);
                    }
                    mScreenMgr.unregisterToGaze();
                    break;
                case MSG_REGISTER_GAZE_CALIBRATE:
                    mGazeMgr.registerGazeCalibration(GazeAppService.this);
                    break;
                case MSG_UNREGISTER_GAZE_CALIBRATE:
                    mGazeMgr.unregisterGazeCalibration(GazeAppService.this);
                    break;
                case MSG_AHEAD_CALIBRATE:
                    mGazeMgr.aheadCalibration();
                    break;
                case MSG_DISPLAY_CALIBRATE:
                    mGazeMgr.displayCalibration();
                    break;
                default:
                    Log.d(TAG, "Unknown message!");
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public void onCreate() {
        //registerReceiver(mCbEventReceiver, new IntentFilter("com.reconinstruments.hudserver.gaze.EVENT"));
        mPlayer = MediaPlayer.create(this, R.raw.bleep);
        if (mGazeMgr == null) {
            mGazeMgr = HUDGazeManager.getInstance();
        }
        mGazeMgr.registerGazeCalibration(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mMessenger.getBinder();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "GazeAppService stopping");
        //unregisterReceiver(mCbEventReceiver);
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        mGazeMgr.unregisterGazeCalibration(this);
    }

    @Override
    public void onCalibrationEvent(boolean atDisplay) {
        String text = (atDisplay)? "Display Calibration done" : "Ahead Calibration done";
        Message msg = null;
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        if (mPlayer != null) {
            mPlayer.start();
        }
        if (!atDisplay) {
            msg = Message.obtain(null, MSG_AHEAD_CAL_DONE);
        }
        else {
            msg = Message.obtain(null, MSG_DISPLAY_CAL_DONE);
        }
        try {
            mClients.get(0).send(msg);
        } catch (RemoteException e) {
            Log.d(TAG, "Client Activity is dead");
        }
    }
}
