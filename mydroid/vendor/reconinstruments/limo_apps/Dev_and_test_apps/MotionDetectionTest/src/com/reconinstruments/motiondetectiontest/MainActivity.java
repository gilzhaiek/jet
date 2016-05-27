package com.reconinstruments.motiondetectiontest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.reconinstruments.os.hardware.motion.HUDActivityMotionManager;
import com.reconinstruments.os.hardware.motion.ActivityMotionDetectionListener;
import com.reconinstruments.utils.stats.ActivityUtil;

public class MainActivity extends Activity implements ActivityMotionDetectionListener
{
    private static final boolean PAUSE_RECON_ACTIVITY = false;
    private static final String TAG = "MainActivity";
    private Context mContext = null;

    private HUDActivityMotionManager mMgr = null;
    private static boolean mStarted = false;

    private int mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.main);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mStarted && mMgr != null) {
            mMgr.unregisterActivityMotionDetection(this);
            mStarted = false;
            TextView tv = (TextView)findViewById(R.id.motion);
            tv.setText("---");
        }
    }

    public void setCycling(View v) {
        if (mStarted && mMgr != null) {
            mMgr.unregisterActivityMotionDetection(this);
        }
        mActivity = HUDActivityMotionManager.MOTION_DETECT_CYCLING;
        Log.d(TAG, "HUDActivityMotionManager.MOTION_DETECT_CYCLING = " + HUDActivityMotionManager.MOTION_DETECT_CYCLING);
        TextView tv = (TextView)findViewById(R.id.activity);
        tv.setText("CYCLING");
    }

    public void setRunning(View v) {
        if (mStarted && mMgr != null) {
            mMgr.unregisterActivityMotionDetection(this);
        }
        mActivity = HUDActivityMotionManager.MOTION_DETECT_RUNNING;
        Log.d(TAG, "HUDActivityMotionManager.MOTION_DETECT_RUNNING = " + HUDActivityMotionManager.MOTION_DETECT_RUNNING);
        TextView tv = (TextView)findViewById(R.id.activity);
        tv.setText("RUNNING");
    }

    public void startTest(View v) {
        if (mStarted) {
            Log.e(TAG, "Already started test");
        } else {
            Log.d(TAG, "startTest");
            if (mMgr == null) {
                mMgr = HUDActivityMotionManager.getInstance();
            }
            if (mMgr != null) {
                int rc = mMgr.registerActivityMotionDetection(this, mActivity);
                Log.d(TAG, "Registered activity motion detection: " + rc);
                mStarted = (rc == 1);
            }
        }
    }

    public void stopTest(View v) {
        Log.d(TAG, "stopTest");
        if (mMgr == null) {
            mMgr = HUDActivityMotionManager.getInstance();
        }
        if (mMgr != null) {
            mMgr.unregisterActivityMotionDetection(this);
            mStarted = false;
            TextView tv = (TextView)findViewById(R.id.motion);
            tv.setText("---");
        }
    }

    public void getEvent(View v) {
        Log.d(TAG, "getEvent");
        if (mMgr == null) {
            mMgr = HUDActivityMotionManager.getInstance();

        }
        if (mMgr != null) {
            Log.d(TAG, "Last event: " + mMgr.getActivityMotionDetectedEvent());
        }
    }

    public void onDetectEvent(boolean inMotion, int type) {
        Log.d(TAG, "inMotion: " + inMotion + " type: " + type);
        TextView tv = (TextView)findViewById(R.id.motion);
        String text = (inMotion) ? "MOVING" : "STATIONARY";

        if (PAUSE_RECON_ACTIVITY) {
            if (inMotion)
                ActivityUtil.resumeActivity(this);
            else
                ActivityUtil.pauseActivity(this);
        }

        tv.setText(text);
    }
}
