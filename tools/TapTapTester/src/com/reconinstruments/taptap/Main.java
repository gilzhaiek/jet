package com.reconinstruments.taptap;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

//<T
import android.view.KeyEvent;
//T>

public class Main extends Activity implements OnClickListener{
    public static final String TAG = "TapTap_Main";

    private final static String STARTTEXT = "Start Recording";
    private final static String STOPTEXT = "Stop Recording";

    /*This is the thread get data from sensor framework*/    
    protected static SensorsThread mSensorThread = null;
    public static boolean mIsSensorThRunning = false;
    /*This is the thread get data from HUD OS*/    
    protected static HudOsThread mHudOsThread = null;
    public static boolean mIsHudOsThRunning = false;

    private static TextView mHertzTextView = null;
    private static TextView mSensorLogTextView = null;
//<T
    private static TextView mTapCounterTextView = null;
    private static int mTapCounter = 0;
//T>
    //private static TextView mTemperatureLogTextView = null;
    //private static int mHertz = 20;
    private static int mHertz = 200;
    //private static int mTimeinterval = 10; //10second by default

    //private Button mUpButton = null;
    //private Button mDownButton = null;
    private Button mStartButton = null;
//<T
    private Button mResetTapCounterButton = null;
//T>
    /*
    private Button mTimeIntervalButton = null;
    private static CheckBox mAccCB = null;
    private static CheckBox mMagCB = null;
    private static CheckBox mGyroCB = null;
    private static CheckBox mPreCB = null;
    private static CheckBox mTempCB = null;
    private static CheckBox mMainTemp = null;
    private static CheckBox mCompassTemp = null;
    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mHertzTextView = (TextView) findViewById(R.id.Hertz);
        mSensorLogTextView = (TextView) findViewById(R.id.WritingOk);
//<T
        mTapCounterTextView = (TextView) findViewById(R.id.Taps);
//T>
        //mTemperatureLogTextView = (TextView) findViewById(R.id.TemperatureStatus);

        //mUpButton = (Button) findViewById(R.id.UpButton);
        //mDownButton = (Button) findViewById(R.id.DownButton);
        mStartButton = (Button) findViewById(R.id.StartStopButton);
//<T
        mResetTapCounterButton = (Button) findViewById(R.id.ResetTapCounterButton);
//T>
        /*
        mTimeIntervalButton = (Button) findViewById(R.id.TimeIntervalSendbutton);
        mAccCB = (CheckBox) findViewById(R.id.AccelerometerCheckBox);
        mMagCB = (CheckBox) findViewById(R.id.MagnetometerCheckBox);
        mGyroCB = (CheckBox) findViewById(R.id.GyroScopeCheckBox);
        mPreCB = (CheckBox) findViewById(R.id.PressureCheckBox);
        mTempCB = (CheckBox) findViewById(R.id.TemperatureCheckBox);
        mMainTemp = (CheckBox) findViewById(R.id.MainBoardTempCheckBox);
        mCompassTemp = (CheckBox) findViewById(R.id.CompassTempCheckBox);
        mUpButton.setOnClickListener(this);
        mDownButton.setOnClickListener(this);
        */
        mStartButton.setOnClickListener(this);
//<T
        mResetTapCounterButton.setOnClickListener(this);
//T>
        //mTimeIntervalButton.setOnClickListener(this);
        //UpdateHertz(mHertz);
//<T
        mHertzTextView.setText("Samp Freq: " + mHertz + " Hz");
	mTapCounterTextView.setText("Total 2-Taps: " + mTapCounter);
//T>
    }

    @Override
    public void onBackPressed(){
        if(mSensorThread != null){
            mSensorThread.StopSensor();
        }
        //if(mHudOsThread != null)
        //    mHudOsThread.StopHudOsThread();
        super.onBackPressed();
    }
/*
    protected static boolean IsAccChecked(){
        return mAccCB.isChecked();
    }
    protected static boolean IsMagChecked(){
        return mMagCB.isChecked();
    }
    protected static boolean IsGyroChecked(){
        return mGyroCB.isChecked();
    }
    protected static boolean IsPreChecked(){
        return mPreCB.isChecked();
    }
    protected static boolean IsTempChecked(){
        return mTempCB.isChecked();
    }
    protected static boolean IsMainTempChecked(){
        return mMainTemp.isChecked();
    }
    protected static boolean IsCompassTempChecked(){
        return mCompassTemp.isChecked();
    }
*/
    protected static int GetHertz(){
        return mHertz;
    }
    public static void SetSensorsText(String text){
        mSensorLogTextView.setText(text);
    }

/*
    public static void SetTemperatureText(String text){
        mTemperatureLogTextView.setText(text);
    }
*/
    
//<T
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_MEDIA_RECORD:
				Log.d(TAG, "TAP_TAP detected (KEY_MEDIA_RECORD): Z00");
				if (mIsSensorThRunning) {
					mTapCounter++;
					mTapCounterTextView.setText("Total 2-Taps: " + mTapCounter);
					mSensorThread.logTapTapInterrupt(1,0,0);
				}
				return true;
			/*
			case KeyEvent.KEYCODE_A:
				Log.d(TAG, "TAP_TAP detected: 00X");
				if (mIsSensorThRunning) {
					mTapCounter++;
					mTapCounterTextView.setText("Total 2-Taps: " + mTapCounter);
					mSensorThread.logTapTapInterrupt(0,0,1);
				}
				return true;
			case KeyEvent.KEYCODE_B:
				Log.d(TAG, "TAP_TAP detected: 0Y0");
				if (mIsSensorThRunning) {
					mTapCounter++;
					mTapCounterTextView.setText("Total 2-Taps: " + mTapCounter);
					mSensorThread.logTapTapInterrupt(0,1,0);
				}
				return true;
			case KeyEvent.KEYCODE_C:
				Log.d(TAG, "TAP_TAP detected: 0YX");
				if (mIsSensorThRunning) {
					mTapCounter++;
					mTapCounterTextView.setText("Total 2-Taps: " + mTapCounter);
					mSensorThread.logTapTapInterrupt(0,1,1);
				}
				return true;
			case KeyEvent.KEYCODE_D:
				Log.d(TAG, "TAP_TAP detected: Z00");
				if (mIsSensorThRunning) {
					mTapCounter++;
					mTapCounterTextView.setText("Total 2-Taps: " + mTapCounter);
					mSensorThread.logTapTapInterrupt(1,0,0);
				}
				return true;
			case KeyEvent.KEYCODE_E:
				Log.d(TAG, "TAP_TAP detected: Z0X");
				if (mIsSensorThRunning) {
					mTapCounter++;
					mTapCounterTextView.setText("Total 2-Taps: " + mTapCounter);
					mSensorThread.logTapTapInterrupt(1,0,1);
				}
				return true;
			case KeyEvent.KEYCODE_F:
				Log.d(TAG, "TAP_TAP detected: ZY0");
				if (mIsSensorThRunning) {
					mTapCounter++;
					mTapCounterTextView.setText("Total 2-Taps: " + mTapCounter);
					mSensorThread.logTapTapInterrupt(1,1,0);
				}
				return true;
			case KeyEvent.KEYCODE_G:
				Log.d(TAG, "TAP_TAP detected: ZYX");
				if (mIsSensorThRunning) {
					mTapCounter++;
					mTapCounterTextView.setText("Total 2-Taps: " + mTapCounter);
					mSensorThread.logTapTapInterrupt(1,1,1);
				}
				return true;
			*/
			default:
				return super.onKeyUp(keyCode, event);
		}
	}
//T>
    
    @Override
    public void onClick(View view) {
        switch(view.getId()){
        /*
            case R.id.UpButton:
                if(mHertz == 1){
                    mHertz = 0;
                }
                //mHertz += 10;
                mHertz += 50;
                //if(mHertz > 200){
                if(mHertz > 800){
                    //mHertz = 200;
                    mHertz = 800;
                }
                UpdateHertz(mHertz);
                break;
            case R.id.DownButton:
                mHertz -= 10;
                if(mHertz <= 0){
                    mHertz = 1;
                }
                UpdateHertz(mHertz);
                break;
        */
            case R.id.StartStopButton:
                mStartButton.setEnabled(false);
                //if(IsAccChecked() || IsMagChecked() || IsGyroChecked() || IsPreChecked() || IsTempChecked())
                    StartorStopSensorThread();
                //if(IsMainTempChecked() || IsCompassTempChecked())
                    //StartorStopHudOsThread();
                mStartButton.setEnabled(true);
                break;
//<T
            case R.id.ResetTapCounterButton:
                mTapCounterTextView.setEnabled(false);
                mTapCounter = 0;
                mTapCounterTextView.setText("Total 2-Taps: " + mTapCounter);
                mTapCounterTextView.setEnabled(true);
                break;
//T>
        /*
            case R.id.TimeIntervalSendbutton:
                String message;
                int num = 10;
                EditText timeintervalText;

                timeintervalText = (EditText) findViewById(R.id.TimeIntervaleditText);
                message = timeintervalText.getText().toString();
                if(!message.matches("")){
                    try {
                        num = Integer.parseInt(message);
                    } catch(NumberFormatException e)
                    {
                        Log.e(TAG, message + "is not a number");
                    }
                }
                mTimeinterval = num;
                break;
        */
        }
    }

    private void StartorStopSensorThread() {

        if(!mIsSensorThRunning){
            DisableView();
            mSensorThread = new SensorsThread(this);
            mSensorThread.setName("SensorsListenerThread");
            mSensorThread.start();
            if(mSensorThread.IsRunning()){
                SetSensorsText("Logging status: logging Data");
                mIsSensorThRunning = true;
            }
            else {
                SetSensorsText("Logging status: Error");
                EnableView();
            }

        }else{
            mSensorThread.StopSensor();
            while(mSensorThread.getState().equals(Thread.State.RUNNABLE)){
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
            }
            mSensorThread = null;
            EnableView();
            SetSensorsText("Logging status: idle");
            mIsSensorThRunning = false;
        }
    }
/*
    private void StartorStopHudOsThread() {
        if(!mIsHudOsThRunning) {

            DisableTimeIntervalView();

            mHudOsThread = new HudOsThread(mTimeinterval);
            mHudOsThread.setName("TemperatureThread");

            mHudOsThread.start();
            if(mHudOsThread.IsRunning()){
                SetTemperatureText("Begin");
                mIsHudOsThRunning = true;
            }
            else {
                SetTemperatureText("Error");
                EnableTimeIntervalView();
            }
        } else {
            mHudOsThread.StopHudOsThread();
            while(mHudOsThread.getState().equals(Thread.State.RUNNABLE)){
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
            }
            mHudOsThread = null;
            mIsHudOsThRunning = false;
            SetTemperatureText("Stop");
            EnableTimeIntervalView();
        }
    }
    private void UpdateHertz(int hertz) {
        mHertzTextView.setText("Sampling Frequency" + hertz + " Hz");	
    }
*/


    private void EnableView() {
	/*
        SetStatus(mAccCB, true, Button.VISIBLE);
        SetStatus(mMagCB, true, Button.VISIBLE);
        SetStatus(mGyroCB, true, Button.VISIBLE);
        SetStatus(mPreCB, true, Button.VISIBLE);
        SetStatus(mTempCB, true, Button.VISIBLE);
        SetStatus(mUpButton, true, Button.VISIBLE);
        SetStatus(mDownButton, true, Button.VISIBLE);	
	*/	
        mStartButton.setText(STARTTEXT);
        //mHertzTextView.setVisibility(TextView.VISIBLE);
    }
/*
    private void EnableTimeIntervalView(){
        SetStatus(mTimeIntervalButton, true, Button.VISIBLE);
    }

    private void DisableTimeIntervalView(){
        SetStatus(mTimeIntervalButton, false, Button.INVISIBLE);
    }

    private void SetStatus(Button button, boolean condition, int view) {
        button.setClickable(condition);
        button.setVisibility(view);
    }
*/

    private void DisableView() {
	/*
        SetStatus(mAccCB, false, Button.INVISIBLE);
        SetStatus(mMagCB, false, Button.INVISIBLE);
        SetStatus(mGyroCB, false, Button.INVISIBLE);
        SetStatus(mPreCB, false, Button.INVISIBLE);
        SetStatus(mTempCB, false, Button.INVISIBLE);
        SetStatus(mUpButton, false, Button.INVISIBLE);
        SetStatus(mDownButton, false, Button.INVISIBLE);
        */
        mStartButton.setText(STOPTEXT);
       // mHertzTextView.setVisibility(TextView.INVISIBLE);
    }

}
