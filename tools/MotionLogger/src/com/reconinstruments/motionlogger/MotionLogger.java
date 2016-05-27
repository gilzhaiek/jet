package com.reconinstruments.motionlogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SoundEffectConstants;
import android.widget.LinearLayout;

import com.reconinstruments.motionlogger.containers.SensorValue;
import com.reconinstruments.motionlogger.sensors.SensorFusionManager;
import com.reconinstruments.motionlogger.sensors.SensorFusionManager.KFListener;
import com.reconinstruments.motionlogger.utils.RawDataLogger;

public class MotionLogger extends Activity implements KFListener {
    private static final String TAG = "MotionLogger";

    /*Sensor Managers*/
    private SensorFusionManager mKFManager;

    /*Sensor Controls*/
    private static boolean mIsReceivingRaw;

    /*Logger*/
    private RawDataLogger mRawDataLogger;

    /*Data Containers*/
    private int mPmicTemp, mXMTemp;

    /*Timers*/
    private Timer mRawLogTimer;

    /*The declared Movement State*/
	private boolean mWalkingMotionLogger;
    private enum MovementState {
        STOP(0), WALK(1), RUN(2), WALKUP(3), WALKDOWN(4), SEATPEDAL(5), STANDPEDAL(6), COAST(7), CLIPSTOP(8);

        private final int id;
        MovementState(int id) {this.id = id;}
        public int getValue() {return id;}
    };
    private MovementState mMovementState;

    /*The layout for background changing*/
    private LinearLayout mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		mWalkingMotionLogger = getIntent().getBooleanExtra("Walking?", true);
		
		if (mWalkingMotionLogger) {
			setContentView(R.layout.walking_motion_logger);
			mLayout = (LinearLayout) findViewById(R.id.walking_motion_logger);
		}
		else {
			setContentView(R.layout.biking_motion_logger);
			mLayout = (LinearLayout) findViewById(R.id.biking_motion_logger);
		}
        mKFManager = SensorFusionManager.Initialize(this);

        mRawDataLogger = new RawDataLogger();

		if (mWalkingMotionLogger)
			mRawDataLogger.isWritingBipedal(true);
		else
			mRawDataLogger.isWritingBipedal(false);
        mMovementState = MovementState.STOP;

        startRaw();
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopRaw();
        stopReceivingRawData();
        switchXMTempState(false);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode) {
        case KeyEvent.KEYCODE_DPAD_CENTER:
            mKFManager.getRawValues().movementState = MovementState.STOP.getValue();
			if (mWalkingMotionLogger)
				mLayout.setBackgroundResource(R.drawable.bipedal_stop);
			else
				mLayout.setBackgroundResource(R.drawable.cycling_stopped);
			mLayout.playSoundEffect(SoundEffectConstants.CLICK);
            break;
        case KeyEvent.KEYCODE_DPAD_LEFT:
			if (mWalkingMotionLogger) {
				mKFManager.getRawValues().movementState = MovementState.WALK.getValue();
				mLayout.setBackgroundResource(R.drawable.bipedal_walk);
			}
			else {
				mKFManager.getRawValues().movementState = MovementState.CLIPSTOP.getValue();
				mLayout.setBackgroundResource(R.drawable.cycling_stopped_clipped_in);
			}
			mLayout.playSoundEffect(SoundEffectConstants.CLICK);
            break;
        case KeyEvent.KEYCODE_DPAD_RIGHT:
            if (mWalkingMotionLogger) {
				mKFManager.getRawValues().movementState = MovementState.RUN.getValue();
				mLayout.setBackgroundResource(R.drawable.bipedal_run);
			}
			else {
				mKFManager.getRawValues().movementState = MovementState.SEATPEDAL.getValue();
				mLayout.setBackgroundResource(R.drawable.cycling_seated_pedaling);
			}
			mLayout.playSoundEffect(SoundEffectConstants.CLICK);
            break;
        case KeyEvent.KEYCODE_DPAD_UP:
			if (mWalkingMotionLogger) {
				mKFManager.getRawValues().movementState = MovementState.WALKUP.getValue();
				mLayout.setBackgroundResource(R.drawable.bipedal_up_stairs);
			}
			else {
				mKFManager.getRawValues().movementState = MovementState.STANDPEDAL.getValue();
				mLayout.setBackgroundResource(R.drawable.cycling_standing_pedaling);
			}
			mLayout.playSoundEffect(SoundEffectConstants.CLICK);
            break;
        case KeyEvent.KEYCODE_DPAD_DOWN:
			if (mWalkingMotionLogger) {
				mKFManager.getRawValues().movementState = MovementState.WALKDOWN.getValue();
				mLayout.setBackgroundResource(R.drawable.bipedal_down_stairs);
			}
			else {
				mKFManager.getRawValues().movementState = MovementState.COAST.getValue();
				mLayout.setBackgroundResource(R.drawable.cycling_coasting);
			}
			mLayout.playSoundEffect(SoundEffectConstants.CLICK);
			break;
        }

        super.onKeyDown(keyCode, event);
        return true;
    }


    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
            .setTitle("Really quit?")
            .setMessage("Are you sure you wish to stop?")
            .setNegativeButton(android.R.string.yes, new OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        MotionLogger.super.onBackPressed();
                        finish();
                    }
                })
            .setPositiveButton(android.R.string.no, null).create().show();
    }

    private int readPmicTemp() {

        File file = new File("/sys/devices/platform/omap/omap_i2c.4/i2c-4/4-0049/temperature");
        //Read text from file
        StringBuilder text = new StringBuilder();
        int pmicTemp = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        pmicTemp = (int) Integer.parseInt(text.toString());
        Log.d(TAG, "PMIC TEMP: " + pmicTemp);
        return pmicTemp;
    }

    private int readXMTemp() {

        File file = new File("/sys/class/misc/jet_sensors/mag_temp");
        //Read text from file
        StringBuilder text = new StringBuilder();
        int xmTemp = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        xmTemp = (int) Integer.parseInt(text.toString());
        Log.d(TAG, "XM TEMP: " + xmTemp);
        return xmTemp;

    }

    //Turn XMTemp readings on or off
    private void switchXMTempState(boolean state) {

        //Determine state to write
        String strFilePath = "/sys/class/misc/jet_sensors/mag_temp_enable";
        String content = null;
        if (state) {
            Log.d(TAG, "enableXMTemp");
            content = "1";
        }
        else {
            Log.d(TAG, "disableXMTemp");
            content = "0";
        }

        //create FileOutputStream object
        try {
            FileOutputStream fos = new FileOutputStream(strFilePath);

            byte[] contentInBytes = content.getBytes();
            fos.write(contentInBytes);

            fos.flush();
            fos.close();

        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void startReceivingRawData() {
        if (mIsReceivingRaw == false) {
            mKFManager.registerListener(this);
            mIsReceivingRaw = true;
        }
    }

    protected void stopReceivingRawData() {
        if (mIsReceivingRaw == true) {
            mKFManager.unregisterListener();
            mIsReceivingRaw = false;
        }
    }

    private void startRaw() {
        try {
            switchXMTempState(true);
            startReceivingRawData();
            mRawDataLogger.startWriting();
            mRawLogTimer = new Timer();
            startTimer(mRawLogTimer);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopRaw() {
        stopReceivingRawData();
        mRawDataLogger.stopWriting();
        mRawLogTimer.cancel();
    }

    private void startTimer(Timer timer) {
        TimerTask printTimeElapsed = new TimerTask() {
                long elapsedTimeMS = 0;
                public void run() {
                    Date dateSince = new Date(elapsedTimeMS);

                    final String time = new SimpleDateFormat("mm:ss", Locale.getDefault()).format(dateSince);

                    elapsedTimeMS += 1000;
                    mPmicTemp = readPmicTemp();
                    mXMTemp = readXMTemp();
					mKFManager.getRawValues().pmic_temp = mPmicTemp;
					mKFManager.getRawValues().xm_temp = mXMTemp;

                }
            };

        timer.schedule(printTimeElapsed, 0, 1000);
    }

    @Override
    public void onRawValuesChanged(SensorValue result) {
        mRawDataLogger.println(mKFManager.getRawValues());
    }
}