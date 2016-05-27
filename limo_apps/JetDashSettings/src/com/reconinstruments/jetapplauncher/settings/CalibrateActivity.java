package com.reconinstruments.jetapplauncher.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.reconinstruments.commonwidgets.FeedbackDialog;
import com.reconinstruments.jetapplauncher.R;
import com.reconinstruments.jetapplauncher.settings.service.GlanceAppService;
import com.reconinstruments.os.hardware.glance.HUDGlanceManager;
import com.reconinstruments.utils.SettingsUtil;

public class CalibrateActivity extends FragmentActivity {

    private static final String TAG = "CalibrateActivity";
    private ImageView mAheadImg, mDisplayImg;
    private boolean mCalStatus;
    private boolean mGlanceEnabled = false;

    private Context mContext;
    private Messenger mService = null;

    private Handler mHandler = new Handler();
    private static final int CALIBRATED_FEEDBACK_DURATION = 2500; // 2.5 seconds

    private Runnable finishActivity = new Runnable() {
        @Override
        public void run() {
            finish();
        }
    };

    /**
    * Handler of incoming messages from service.
    */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GlanceAppService.MSG_AHEAD_CAL_DONE:
                    Log.d(TAG, "Ahead calibration complete");
                    SettingsUtil.setSystemInt(mContext, "GlanceCalibrated", 1);
                    FeedbackDialog.showDialog(CalibrateActivity.this, "Calibrated", "This feature may not work<br />for everyone", FeedbackDialog.ICONTYPE.CHECKMARK, FeedbackDialog.HIDE_SPINNER);
                    mHandler.postDelayed(finishActivity, CALIBRATED_FEEDBACK_DURATION);
                    break;
                case GlanceAppService.MSG_DISPLAY_CAL_DONE:
                    Log.d(TAG, "Display calibration complete");
                    mAheadImg.setVisibility(View.VISIBLE);
                    mDisplayImg.setVisibility(View.INVISIBLE);
                    mCalStatus = true;
                    break;
                default:
                    Log.d(TAG, "Unknown message!");
                    super.handleMessage(msg);
            }
        }
    }

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            mService = new Messenger(obj);

            try {
                Message msg = Message.obtain(null, GlanceAppService.MSG_REGISTER_CLIENT);
                Bundle b = new Bundle();
                b.putString("key", TAG);
                msg.setData(b);
                msg.replyTo = mMessenger;
                mService.send(msg);

                // Stop glance detection if already enabled
                if (mGlanceEnabled) {
                    msg = Message.obtain(null, GlanceAppService.MSG_UNREGISTER_GLANCE_DETECT);
                    msg.setData(b);
                    mService.send(msg);
                }
            } catch (RemoteException e) {
                Log.d(TAG, "GlanceAppService has crashed");
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            Log.d(TAG, "Disconnected from service");
            mService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibrate);

        // Check to see if glance detection is currently enabled
        if (SettingsUtil.getCachableSystemIntOrSet(this, "GlanceEnabled", 0) == 1) {
            Log.d(TAG, "Glance detection currently enabled");
            mGlanceEnabled = true;
        }

        bindService(new Intent(CalibrateActivity.this, GlanceAppService.class), mConnection, 0);
        mContext = this;

        mCalStatus = false;

        mAheadImg = (ImageView) findViewById(R.id.calAheadImg);
        mDisplayImg = (ImageView) findViewById(R.id.calDisplayImg);

        mAheadImg.setVisibility(View.INVISIBLE);
        mDisplayImg.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            try {
                Bundle b = new Bundle();
                b.putString("key", TAG);
                if (mGlanceEnabled) {
                    Message msg = Message.obtain(null, GlanceAppService.MSG_REGISTER_GLANCE_DETECT);
                    msg.setData(b);
                    mService.send(msg);
                }

                Message msg = Message.obtain(null, GlanceAppService.MSG_UNREGISTER_CLIENT);
                msg.setData(b);
                mService.send(msg);
            } catch (RemoteException e) {
                // There is nothing to do if the service has crashed.
            }
            unbindService(mConnection);
        }

        FeedbackDialog.dismissDialog(this);
    }

    public void aheadCalibration() {
        Message msg = Message.obtain(null, GlanceAppService.MSG_AHEAD_CALIBRATE);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to start ahead calibration");
        }
    }

    public void displayCalibration() {
        Message msg = Message.obtain(null, GlanceAppService.MSG_DISPLAY_CALIBRATE);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to start display calibration");
        }
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
            // Wait for a second before we do calibration.
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                Log.e(TAG, "Failed to sleep");
            }

            if (!mCalStatus) {
                displayCalibration();
            }
            else {
                aheadCalibration();
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_slide_in_left,0);

    }
}
