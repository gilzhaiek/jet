package com.reconinstruments.autocadence;

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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

import com.reconinstruments.autocadence.containers.SensorValue;
import com.reconinstruments.autocadence.sensors.SensorFusionManager;
import com.reconinstruments.autocadence.sensors.SensorFusionManager.KFListener;
import com.reconinstruments.autocadence.utils.RawDataLogger;

public class AutoCadence extends Activity implements KFListener {
    private static final String TAG = "AutoCadence";

    private static final String TRANS_INTENT = "com.reconinstruments.applauncher.transcend.FULL_INFO_UPDATED";

    /*Sensor Managers*/
    private SensorFusionManager mKFManager;
    private Bundle sFullInfo;
    private BroadcastReceiver mTranscendReceiver;

    /*Sensor Controls*/
    private static boolean mIsReceivingRaw;

    /*Calculators*/
    private CadenceDetector mCadenceDetector;
    private int mAccelTicker;
    private float[] mRawAccelValues;
    private double[] mAccelTimestamp;

    /*Logger*/
    private RawDataLogger mRawDataLogger;

    /*Data Containers*/
    private int mPmicTemp, mXMTemp, mCadence;
    private float mWheelSpeed;

    /*Timers*/
    private Timer mRawLogTimer;
    private long mTimestamp;

    /*The declared Movement State*/
    private enum MovementState {
        STOP(0), WALK(1), RUN(2), WALKUP(3), WALKDOWN(4), SEATPEDAL(5), STANDPEDAL(6), COAST(7), CLIPSTOP(8);

        private final int id;
        MovementState(int id) {this.id = id;}
        public int getValue() {return id;}
    };
    private MovementState mMovementState;

    /*TextViews for displaying data*/
    private static TextView mRealCadenceText;
    private static TextView mCalcCadenceText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTimestamp = System.currentTimeMillis();

        mRawAccelValues = new float[100];
        mAccelTimestamp = new double[100];

        setContentView(R.layout.cadence_screen);

        mRealCadenceText = (TextView) findViewById(R.id.realcadence);
        mCalcCadenceText = (TextView) findViewById(R.id.calccadence);

        mKFManager = SensorFusionManager.Initialize(this);
        mTranscendReceiver = new BroadcastReceiver () {
                public void onReceive(Context context, Intent intent) {
                    sFullInfo = intent.getBundleExtra("FullInfo");
                    Log.v(TAG, "Updated sFullInfo");
                }
            };
        registerReceiver(mTranscendReceiver, new IntentFilter(TRANS_INTENT));

        mCadenceDetector = new CadenceDetector(mTimestamp);
        mRawDataLogger = new RawDataLogger();
        mRawDataLogger.isWritingBipedal(false);

        mMovementState = MovementState.STOP;

        startRaw();
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopRaw();
        stopReceivingRawData();
        unregisterReceiver(mTranscendReceiver);
        switchXMTempState(false);
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
            .setTitle("Really quit?")
            .setMessage("Are you sure you wish to stop?")
            .setNegativeButton(android.R.string.yes, new OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        AutoCadence.super.onBackPressed();
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
            mRawDataLogger.startWriting(mTimestamp);
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

    private int getCadence() {
        int value = -1;
        if (sFullInfo != null) {
            Bundle tempBundle = (Bundle) sFullInfo.get("CADENCE_BUNDLE");
            value = tempBundle.getInt("Cadence");
            if (value == 65535) {
                value = -1;
            }
        }
        return value;
    }

    private float getWheelSpeed() {
        float value = -1;
        if (sFullInfo != null) {
            Bundle tempBundle = (Bundle) sFullInfo.get("SPEED_BUNDLE");
            value = tempBundle.getFloat("SensorSpeed");
        }
        return value;
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
                    mCadence = getCadence();
                    mWheelSpeed = getWheelSpeed();
                    mKFManager.getRawValues().pmic_temp = mPmicTemp;
                    mKFManager.getRawValues().xm_temp = mXMTemp;
                    mKFManager.getRawValues().trueCadence = mCadence;
                    mKFManager.getRawValues().wheelSpeed = mWheelSpeed;

                }
            };

        timer.schedule(printTimeElapsed, 0, 1000);
    }

    @Override
    public void onRawValuesChanged(SensorValue result) {
        if (mAccelTicker > 0)
            mAccelTicker--;
        mRawAccelValues[mAccelTicker] = result.accX;
        mAccelTimestamp[mAccelTicker] = result.timestamp;
        if (mAccelTicker <= 0) {
            mAccelTicker = 100;

            mCadenceDetector.receiveAccelDump(mRawAccelValues, mAccelTimestamp);

            mKFManager.getRawValues().calcCadence = mCadenceDetector.getCadence();

            runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mRealCadenceText.setText("" + mKFManager.getRawValues().trueCadence);
                        mCalcCadenceText.setText("" + mKFManager.getRawValues().calcCadence);
                    }
                });
        }
        mRawDataLogger.println(mKFManager.getRawValues());
    }
}