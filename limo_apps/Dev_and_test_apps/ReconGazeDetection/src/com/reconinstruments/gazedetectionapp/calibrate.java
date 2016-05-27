package com.reconinstruments.gazedetectionapp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.reconinstruments.os.hardware.gaze.HUDGazeManager;

public class calibrate extends Activity {

    private static final String TAG = "CalibrateActivity";
    private ImageView AheadImg, DisplayImg;
    private boolean calStatus;

    private Messenger mService = null;
    private boolean mBound = false;

    /**
    * Handler of incoming messages from service.
    */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GazeAppService.MSG_AHEAD_CAL_DONE:
                    Log.d(TAG, "Ahead calibration complete");
                    AheadImg.setVisibility(View.INVISIBLE);
                    DisplayImg.setVisibility(View.VISIBLE);
                    calStatus = true;
                    break;
                case GazeAppService.MSG_DISPLAY_CAL_DONE:
                    Log.d(TAG, "Display calibration complete");
                    finish();
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
                Message msg = Message.obtain(null, GazeAppService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                Log.d(TAG, "GazeAppService has crashed");
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

        bindService(new Intent(calibrate.this, GazeAppService.class), mConnection, 0);
        mBound = true;
        //Log.d(TAG, "Binding to GazeAppService");

        calStatus = false;

        AheadImg = (ImageView) findViewById(R.id.CalAheadImg);
        DisplayImg = (ImageView) findViewById(R.id.CalDisplayImg);
        AheadImg.setVisibility(View.VISIBLE);
        DisplayImg.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, GazeAppService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing to do if the service has crashed.
                }
                unbindService(mConnection);
                mBound = false;
            }
        }
    }

    public void aheadCalibration() {
        Message msg = Message.obtain(null, GazeAppService.MSG_AHEAD_CALIBRATE);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to start ahead calibration");
        }
    }

    public void displayCalibration() {
        Message msg = Message.obtain(null, GazeAppService.MSG_DISPLAY_CALIBRATE);
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

            if (!calStatus) {
                aheadCalibration();
            }
            else {
                displayCalibration();
            }
        }
        return super.onKeyUp(keyCode, event);
    }
}
