package com.reconinstruments.gazedetectionapp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.reconinstruments.os.hardware.gaze.HUDGazeManager;

public class GazeActivity extends Activity
{
    private static final String TAG = "GazeActivity";

    private Context mContext = null;

    private boolean menuCount, gazeStatus, calibrated, gazeOnResume, gazeOnConnect;
    private TextView gazeText, calText;
    private ImageView checkImg;

    public static final String REQUEST = "com.reconinstruments.gazedetectionapp.REQ";
    public static final String RESULT = "com.reconinstruments.gazedetectionapp.RES";
    public static final String EVENT = "com.reconinstruments.gazedetectionapp.EVT";

    private Messenger mService = null;
    private boolean mBound = false;

    private SharedPreferences sharedPref;
    private static final String GAZE_STATUS_STRING = "gazeStatus";
    private static final String CAL_STATUS_STRING = "calibrationStatus";

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            Log.d(TAG, "Connected to service");
            mService = new Messenger(obj);
            if (gazeOnConnect) {
                toggleGazeDetection();
                gazeOnConnect = false;
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            Log.d(TAG, "Disconnected from service");
            mService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean connService = false;
        mContext = this;
        sharedPref = getPreferences(Context.MODE_PRIVATE);

        /* Bind and Start the service for this app */
        setContentView(R.layout.main);
        bindService(new Intent(GazeActivity.this, GazeAppService.class), mConnection, Context.BIND_AUTO_CREATE);
        mBound = true;
        //Log.d(TAG, "Binding to GazeAppService");
        startService(new Intent(getBaseContext(), GazeAppService.class));

        gazeStatus = sharedPref.getBoolean(GAZE_STATUS_STRING, false);
        calibrated = sharedPref.getBoolean(CAL_STATUS_STRING, false);
        menuCount = gazeOnResume = gazeOnConnect = false;
        
        gazeText = (TextView) findViewById(R.id.GazeDetText);
        calText = (TextView) findViewById(R.id.CalText);
        checkImg = (ImageView) findViewById(R.id.CheckBox);

        /* Initialize interface and start gaze detection (if needed) */
        menuSelect();
        if (gazeStatus) {
            gazeOnConnect = true;
            gazeStatus = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        /* Re-enable gaze detection if required */
        if (gazeOnResume) {
            //Log.d(TAG,"Attempting to re-enable gaze detection");
            toggleGazeDetection();
            gazeOnResume = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(GAZE_STATUS_STRING, gazeStatus);
        editor.putBoolean(CAL_STATUS_STRING, calibrated);
        editor.apply();
        if (mBound) {
            if (mService != null) {
                unbindService(mConnection);
                mBound = false;
            }
        }
    }

    public void startGazeDetection() {
        Message msg = Message.obtain(null, GazeAppService.MSG_REGISTER_GAZE_DETECT);
        try {
            Log.d(TAG,"Sending message to service to start");
            mService.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to start detection");
        }

    }

    public void stopGazeDetection() {
        Message msg = Message.obtain(null, GazeAppService.MSG_UNREGISTER_GAZE_DETECT);
        try {
            Log.d(TAG,"Sending message to service to stop");
            mService.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to stop detection");
        }

    }

    public void menuSelect() {
        if (!menuCount) {
            calText.setTextColor(getResources().getColor(R.color.recon_grey));
            gazeText.setTextColor(getResources().getColor(R.color.recon_orange));
            setCheckBoxImage();
        } else {
            gazeText.setTextColor(getResources().getColor(R.color.recon_grey));
            calText.setTextColor(getResources().getColor(R.color.recon_orange));
            setCheckBoxImage();
        }
    }

    public void setCheckBoxImage(){
        if (!menuCount) {
            if (gazeStatus) {
                checkImg.setImageResource(R.drawable.checkbox_enabled_orange);
            } else {
                checkImg.setImageResource(R.drawable.checkbox_orange);
            }
        } else {
            if (gazeStatus) {
                checkImg.setImageResource(R.drawable.checkbox_enabled_grey);
            } else {
                checkImg.setImageResource(R.drawable.checkbox_grey);
            }
        }
    }

    public void toggleGazeDetection() {
        if (!gazeStatus) {
            Log.d(TAG,"Toggling ON gaze detection");
            startGazeDetection();
            gazeStatus = true;
        }
        else {
            Log.d(TAG,"Toggling OFF gaze detection");
            stopGazeDetection();
            gazeStatus = false;
        }
        setCheckBoxImage();
    }

    public void startCalibration() {
        if (gazeStatus) {
            gazeOnResume = true;
            toggleGazeDetection();
        }
        Intent calibrateIntent = new Intent(this, calibrate.class);
        startActivity(calibrateIntent);
        calibrated = true;
    }

    /* Detects release of button and increments according count */
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                /*  Gaze Detection Toggle  */
                if (!menuCount) {
                    if (!calibrated) {
                        gazeOnResume = true;
                        startCalibration();
                    } else {
                        toggleGazeDetection();
                    }
                }
                /* Calibrate */
                else {
                    startCalibration();
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP && menuCount) {
                menuCount = false;
                menuSelect();
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && !menuCount) {
                menuCount = true;
                menuSelect();
            }
        }
        return super.onKeyUp(keyCode, event);
    }
}