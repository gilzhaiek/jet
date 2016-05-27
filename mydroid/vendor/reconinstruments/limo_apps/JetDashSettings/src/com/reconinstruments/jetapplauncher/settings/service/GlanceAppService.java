package com.reconinstruments.jetapplauncher.settings.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.reconinstruments.jetapplauncher.R;
import com.reconinstruments.os.hardware.glance.HUDGlanceManager;
import com.reconinstruments.os.hardware.glance.GlanceCalibrationListener;
import com.reconinstruments.os.hardware.screen.HUDScreenManager;
import com.reconinstruments.utils.SettingsUtil;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class GlanceAppService extends Service implements GlanceCalibrationListener{
    private static final String TAG = "GlanceAppService";
    private static MediaPlayer mPlayer = null;
    private static MediaPlayer mTickPlayer = null;

    private HUDGlanceManager mGlanceMgr = null;
    private HUDScreenManager mScreenMgr = null;

    private HashMap<String, Messenger> mClients = new HashMap<String, Messenger>();
    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_UNREGISTER_CLIENT = 2;
    public static final int MSG_REGISTER_GLANCE_DETECT = 3;
    public static final int MSG_UNREGISTER_GLANCE_DETECT = 4;
    public static final int MSG_REGISTER_GLANCE_CALIBRATE = 5;
    public static final int MSG_UNREGISTER_GLANCE_CALIBRATE = 6;
    public static final int MSG_AHEAD_CALIBRATE = 7;
    public static final int MSG_DISPLAY_CALIBRATE = 8;
    public static final int MSG_SET_DELAY = 9;

    public static final int MSG_AHEAD_CAL_DONE = 9;
    public static final int MSG_DISPLAY_CAL_DONE = 10;
    public static final int MSG_REGISTERED_SUCCESS = 11;
    public static final int MSG_REGISTERED_FAILED = 12;
    public static final int MSG_UNREGISTERED_SUCCESS = 13;
    public static final int MSG_UNREGISTERED_FAILED = 14;

    private static final String ACTION_REGISTER_GLANCE = "com.reconinstruments.ACTION_REGISTER_GLANCE";
    private static final String ACTION_UNREGISTER_GLANCE = "com.reconinstruments.ACTION_UNREGISTER_GLANCE";

    private static final int CAL_DONE = 1;
    private static final int CAL_TICK = 2;
    private static final int CAL_TICK_INTERVAL = 500;
    private HashMap<Integer, Integer> mSoundPoolMap;
    private SoundPool mSoundPool;

    private Context mContext;
    private Timer mTimer;

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            int res, delay;
            Message response = null;
            Bundle b = msg.getData();

            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    Log.d(TAG, "Registering client: " + b.getString("key"));
                    if (!mClients.containsKey(b.getString("key"))) {
                        mClients.put(b.getString("key"), msg.replyTo);
                    }
                    break;
                case MSG_UNREGISTER_CLIENT:
                    Log.d(TAG, "Unregistering client: " + b.getString("key"));
                    mClients.remove(b.getString("key"));
                    break;
                case MSG_REGISTER_GLANCE_DETECT:
                    Log.d(TAG,"Received message to start glance detect");
                    res = registerToGlance();
                    if (res != -1) {
                        response = Message.obtain(null, MSG_REGISTERED_SUCCESS);
                    } else {
                        response = Message.obtain(null, MSG_REGISTERED_FAILED);
                    }
                    try {
                        Messenger m = mClients.get(b.getString("key"));
                        m.send(response);
                    } catch (RemoteException e) {
                        Log.d(TAG, "Client Activity is dead");
                    }
                    break;
                case MSG_UNREGISTER_GLANCE_DETECT:
                    Log.d(TAG,"Received message to stop glance detect");
                    res = unregisterToGlance();
                    if (res != -1) {
                        response = Message.obtain(null, MSG_UNREGISTERED_SUCCESS);
                    } else {
                        response = Message.obtain(null, MSG_UNREGISTERED_FAILED);
                    }
                    try {
                        mClients.get(b.getString("key")).send(response);
                    } catch (RemoteException e) {
                        Log.d(TAG, "Client Activity is dead");
                    }
                    break;
                case MSG_REGISTER_GLANCE_CALIBRATE:
                    if (mGlanceMgr != null) {
                        mGlanceMgr.registerGlanceCalibration(GlanceAppService.this);
                    }
                    break;
                case MSG_UNREGISTER_GLANCE_CALIBRATE:
                    if (mGlanceMgr != null) {
                        mGlanceMgr.unregisterGlanceCalibration(GlanceAppService.this);
                    }
                    break;
                case MSG_AHEAD_CALIBRATE:
                    if (mGlanceMgr != null) {
                        playInterval();
                        mGlanceMgr.aheadCalibration();
                    }
                    break;
                case MSG_DISPLAY_CALIBRATE:
                    if (mGlanceMgr != null) {
                        playInterval();
                        mGlanceMgr.displayCalibration();
                    }
                    break;
                case MSG_SET_DELAY:
                    delay = msg.arg1;
                    delay *= 1000;
                    Log.d(TAG, "Setting screen off delay to " + delay);
                    if (mScreenMgr != null) {
                        mScreenMgr.setScreenOffDelay(delay);
                    }
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
        Log.d(TAG, "Creating glance app service");
        if (mGlanceMgr == null) {
            mGlanceMgr = HUDGlanceManager.getInstance();
        }

        if (mGlanceMgr != null) {
            mGlanceMgr.registerGlanceCalibration(this);

            if ((SettingsUtil.getCachableSystemIntOrSet(this, "GlanceEnabled", 0) == 1) &&
                (SettingsUtil.getCachableSystemIntOrSet(this, "GlanceCalibrated", 0) == 1)) {
                Log.d(TAG, "Enabling glance detection!");
                if (registerToGlance() == 0) {
                    SettingsUtil.setSystemInt(this, "GlanceEnabled", 0);
                }
            } else {
                Log.d(TAG, "Haven't enabled glance detection");
            }
        } else {
            Log.d(TAG, "Could not retrieve HUD glance manager");
        }
        mContext = this;

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_REGISTER_GLANCE);
        filter.addAction(ACTION_UNREGISTER_GLANCE);
        registerReceiver(mReceiver, filter);

        initSounds();
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Received action: " + action);
            if (action.equals(ACTION_REGISTER_GLANCE)) {
                registerToGlance();
            } else if (action.equals(ACTION_UNREGISTER_GLANCE)) {
                unregisterToGlance();
            }
        }
    };

    private int registerToGlance() {
        if (mScreenMgr == null) {
            mScreenMgr = HUDScreenManager.getInstance();
        }

        // Retrieve the screen delay
        int delay = SettingsUtil.getCachableSystemIntOrSet(GlanceAppService.this, "ScreenDelay", 2);
        delay *= 1000;
        Log.d(TAG, "Setting screen off delay to " + delay);
        mScreenMgr.setScreenOffDelay(delay);

        return mScreenMgr.registerToGlance();
    }

    private int unregisterToGlance() {
        if (mScreenMgr == null) {
            mScreenMgr = HUDScreenManager.getInstance();
        }
        return mScreenMgr.unregisterToGlance();
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
        Log.d(TAG, "GlanceAppService stopping");
        if (mGlanceMgr != null) {
            mGlanceMgr.unregisterGlanceCalibration(this);
        }

        unregisterReceiver(mReceiver);
    }

    @Override
    public void onCalibrationEvent(boolean atDisplay) {
        String text = (atDisplay)? "Display Calibration done" : "Ahead Calibration done";
        Log.d(TAG, "onCalibrationEvent: " + text);

        // Stop playing the progress sound
        mTimer.cancel();
        mTimer = null;

        // Play the calibration done sound
        playSound(CAL_DONE);

        Message msg = null;
        if (!atDisplay) {
            msg = Message.obtain(null, MSG_AHEAD_CAL_DONE);
        }
        else {
            msg = Message.obtain(null, MSG_DISPLAY_CAL_DONE);
        }
        try {
            mClients.get("CalibrateActivity").send(msg);
        } catch (RemoteException e) {
            Log.d(TAG, "Client Activity is dead");
        }
    }

    private void initSounds() {
        mSoundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 100);
        mSoundPoolMap = new HashMap<Integer, Integer>();

        mSoundPoolMap.put(CAL_DONE, mSoundPool.load(mContext, R.raw.bleep, 1));
        mSoundPoolMap.put(CAL_TICK, mSoundPool.load(mContext, R.raw.tick, 1));
    }

    private void playSound(int sound) {
        if (mSoundPool == null || mSoundPoolMap == null) {
            initSounds();
        }
        mSoundPool.play(mSoundPoolMap.get(sound), 1, 1, 1, 0, 1f);
    }

    private void playInterval() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                playSound(CAL_TICK);
            }
        }, 0, 1000);
    }
}
