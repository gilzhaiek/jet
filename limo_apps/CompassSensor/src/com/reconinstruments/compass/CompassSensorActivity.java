package com.reconinstruments.compass;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.reconinstruments.os.hardware.led.HUDLedManager;
import com.reconinstruments.utils.DeviceUtils;
import com.reconinstruments.commonwidgets.CommonUtils;
import android.os.AsyncTask;
import com.reconinstruments.commonwidgets.FeedbackDialog;
import android.support.v4.app.FragmentActivity;

/**
 * 
 * This is main activity for compass calibration process
 * The state transition is as following
 * 
 *	User navigate to Compass Dashboard:
 *
 *	Compass Dashboard State:
 *		Check hasWrittenMagOffsetsV2 and /data/system/senors.conf conf contains “conv_B” without not used.
 *			If (1) we go to “Compass Screen State”
 *			if (0) we go “Calibration Pre Start State”
 *  
 *  <Then Start this Activity to Calibrate>
 *	Calibration Pre Start State:
 *		if “retryCalibration” is 1
 *			Set retryCalibration to 0
 *			go to “Calibration Retry State”
 *
 *		if “retryCalibrationHW” is 1
 *			Set retryCalibrationHW to 0
 *			go to “Calibration Retry State HW”
 *
 *		Show a screen that asks the user to Calibrate
 *		wait for any button press (except back)
 *		Shows a screen that explains how to calibrate
 *		Wait for any button press (except back)
 *		go to “Calibration Start State”
 *
 *	Calibration Retry State:
 *		Shows a screen that iterates how to calibrate
 *		Wait for any button press (except back)
 *		go to “Calibration Start State”
 *
 *	Calibration Retry State HW:
 *		Shows a screen that iterates how to calibrate (with a different wording)
 *		Wait for any button press (except back)
 *		go to “Calibration Start State”
 *
 *	Calibration Start State:
 *		Starts an intent to call CompassSensorActivity
 *		When starting the CompassSensorActivity, we will copy the default sensors.conf to the data partition before registering to the sensors
 *		Sleep X seconds.
 *		Set hasWrittenMagOffsetsV2 to 0.
 *		A 15 seconds countdown collects data CompassSensorListener::onSensorChanged
 *		CompassSensorListener::calibrationComplete call ErrFunction::isHardwareFaulty() before calculateOffsets();
 *
 *		if(true)
 *			go to “Hardware Faulty State”
 *
 *		An ErrFunction::CalculateOffsetAndScale checks for valid values and returns true/false
 *
 *			if(true) call writeMagOffsetAndScale:
 *				Settings.Secure.putInt(context.getContentResolver(), "hasWrittenMagOffsetsV2", 1);
 *
 *			if (false) set “retryCalibration” to 1.
 *				Go to “Compass Dashboard State”
 *
 *	Hardware Faulty State
 *		set “retryCalibrationHW” to 1.
 *
 *	Compass Screen State
 *		Show compass
 *  
 * @author Re-factored by Patrick Cho
 *
 */
public class CompassSensorActivity extends FragmentActivity {
	private static final String TAG = CompassSensorActivity.class.getSimpleName();

    private HUDLedManager mLedMgr = null;

	public static final boolean DEBUG = true;
	public static final boolean LOG_DIAGNOSTIC_DATA = false;
	public static final boolean LOAD_ACC_OFFSET = false;
	public static final boolean SHOW_TEMPERATURES = false;
	public static final int TEMPERATURE_UPDATE_INTERVAL_MS = 2000;
	
	public static final int SLEEP_TIME_AFTER_CONF_REWRITE = 1000;
	
	public static final int CALIBRATE_INTERVAL = 1000;
	public static final int CALIBRATE_COMPLETE_DELAY = 5000; // Calculation of data takes 2~3 seconds so make it 5 seconds to show 2~3 seconds
	public static final int CALIBRATE_COUNTDOWN_STEPS = 15; // ACTUAL COUNTDOWN TIME FOR DATA COLLECTION
	
	public static final int MESSAGE_WHAT_CALIBRATE_PROGRESS	= 1;
	public static final int MESSAGE_WHAT_CALIBRATE_COMPLETE	= 2;
	public static final int MESSAGE_WHAT_CALIBRATE_EXIT		= 3;
	public static final int MESSAGE_WHAT_CALIBRATE_SHOW_INSTRUCTIONS = 4;
	
	public static final String SYSTEM_PREF_COMPASS_STATE_KEY = "compassState";
	public static final String SYSTEM_PREF_COMPASS_STATE_VALUE_CALIBRATED = "calibrated";
	
	/**
	 * Calibration Status Enumerations
	 */
	public static enum CALIBRATION_STATE {
		/**
		 * Calibration Pre Start State:
		 */
		READY,
		/**
		 * Show instructions
		 */
		SHOW_INSTRUCTIONS,
		/**
		 * Calibration Start State:
		 */
		CALIBRATING,
		/**
		 * Calibration Retry State HW:<br/>
		 * hardware failure occured
		 */
		FAILURE_FIRST,
		/**
		 * Calibration Retry State HW Second Screen <br/>
		 * hardware failure occured
		 */
		FAILURE_SECOND,
		/**
		 * Calibration Retry State:
		 */
		INCOMPLETE_FIRST,
		/**
		 * Calibration Retry State Second Screen:
		 */
		INCOMPLETE_SECOND,
		/**
		 * Calibration Finished, stay a few seconds and continue
		 * TODO check whether press key to continue is needed since user might miss this screen a lot
		 */
		FINISHED
	}
	public static CALIBRATION_STATE mCurrentState = CALIBRATION_STATE.READY;
	
	/**
	 * Calibration Result Status Enumerations
	 */
	public static enum CALIBRATION_RESULT {
		/**
		 * Calibration Successful
		 */
		SUCCESSFUL,
		/**
		 * Calibration Incomplete (eg. Due to not enough data)
		 */
		INCOMPLETE,
		/**
		 * Calibration FAILURE (eg. Due to hardware fault)
		 */
		FAILURE
	}

	/**
	 * The listener which computes the correction of sensor after data collected
	 */
	private CompassSensorListener mCompassSensorListener;
	
	/*
	 * View References
	 */
	private TextView mTempTextView;
	private int mTempBoard = 0;
	private int mTempSensorChip = 0;
	private TextView mTopTextView;
	private TextView mBottomTextView;
	private LinearLayout mWrapperView;
	private FrameLayout mRootView;
	
	private Handler customHandler = new Handler();
	
	/**
	 * Indicates if the calibration process is actively calibrating,
	 * therefore, all the keystrokes including back button should not trigger
	 * anything
	 */
	private static boolean isCalibrating = false;
	
	private static boolean CALLED_FROM_INITIAL_SETUP = false;

	/**
	 * Thread instance;
	 */
	private CalibrationTimerThread mCalibrationTimerThread;
	
	/**
	 * This is the handler that works as Facade and handles all of the UI changes
	 */
	private Handler uiHandler = new Handler(){

		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what) {
			case MESSAGE_WHAT_CALIBRATE_SHOW_INSTRUCTIONS:
				setInstructionView();
				mCurrentState = CALIBRATION_STATE.SHOW_INSTRUCTIONS;
				break;
			case MESSAGE_WHAT_CALIBRATE_PROGRESS:
				mTopTextView.setText(msg.arg1+"");
				break;
			case MESSAGE_WHAT_CALIBRATE_COMPLETE:
			     new ReallyCompleteTheCalibrationTask().execute();
			     break;
			case MESSAGE_WHAT_CALIBRATE_EXIT:
				if (mCurrentState == CALIBRATION_STATE.FINISHED) {
					if(CALLED_FROM_INITIAL_SETUP){
						Log.d(TAG, "Launching bluetooth pairing!");
						Intent goToNext = new Intent("com.reconinstruments.connectdevice.CONNECT"); 
						goToNext.putExtra("com.reconinstruments.QuickstartGuide.CALLED_FROM_INITIAL_SETUP", true);
						startActivity(goToNext);
					}
					finish(); // Only Finish when finished state
				}
				break;
			}
		}
	};
	
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (DEBUG) Log.d(TAG ,"onCreate");
		
		Intent received = getIntent();
		CALLED_FROM_INITIAL_SETUP = received.getBooleanExtra("com.reconinstruments.QuickstartGuide.CALLED_FROM_INITIAL_SETUP", false);
		
		setContentView(R.layout.activity_compass_calibrate);
		
		mCompassSensorListener = new CompassSensorListener(this);
		
		initViewReference();
		ready();
		
		if(SHOW_TEMPERATURES) {
			enableXMTemp();
			updateTemperatureValues();
		}
	}
	private Runnable updateTimerThread = new Runnable() {

		public void run() {
			updateTemperatureValues();
		}
	};

	private void updateTemperatureValues() {
		mTempBoard = readPmicTemp();
		mTempSensorChip = readXMTemp();
		mTempTextView.setText("Tb: " + mTempBoard + ", Ts: " + mTempSensorChip);
		customHandler.postDelayed(updateTimerThread, TEMPERATURE_UPDATE_INTERVAL_MS);
	}
	
 
    private int readPmicTemp(){

		File file = new File("/sys/devices/platform/omap/omap_i2c.4/i2c-4/4-0049/temperature");
		//Read text from file
		StringBuilder text = new StringBuilder();
		int pmicTemp = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;

			while ((line = br.readLine()) != null) {
				text.append(line);
				//		        text.append('\n');
				//		    	Log.d("DARRELL","PMIC TEMP: "+ line);
			}
		}
		catch (IOException e) {
			//You'll need to add proper error handling here
		}


		pmicTemp = (int) Integer.parseInt(text.toString());
//		Log.d(TAG,"PMIC TEMP: "+ pmicTemp);
		return pmicTemp;
	}
    
	private void enableXMTemp(){
		Log.d(TAG,"enableXMTemp!");
		String strFilePath = "/sys/class/misc/jet_sensors/mag_temp_enable";
		String content = "1";
		//create FileOutputStream object
		try {
			FileOutputStream fos = new FileOutputStream(strFilePath);

			byte[] contentInBytes = content.getBytes();
			fos.write(contentInBytes);

			fos.flush();
			fos.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}		
	}
	private void disableXMTemp(){
		String strFilePath = "/sys/class/misc/jet_sensors/mag_temp_enable";

		//create FileOutputStream object
		try {
			FileOutputStream fos = new FileOutputStream(strFilePath);
			DataOutputStream dos = new DataOutputStream(fos);

			int ch = 0;

			dos.write(ch);
			dos.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}		
	}
	
	
	private int readXMTemp(){
		
		File file = new File("/sys/class/misc/jet_sensors/mag_temp");
		//Read text from file
		StringBuilder text = new StringBuilder();
		int xmTemp = 0;
		try {
		    BufferedReader br = new BufferedReader(new FileReader(file));
		    String line;
		    
		    while ((line = br.readLine()) != null) {
		        text.append(line);
//		        text.append('\n');
//		    	Log.d("DARRELL","PMIC TEMP: "+ line);
		    	
		
		    }
		}
		catch (IOException e) {
		    //You'll need to add proper error handling here
		}
		
		
		xmTemp = (int) Integer.parseInt(text.toString());
//		Log.d(TAG,"XM TEMP: "+ xmTemp);
		return xmTemp;
		
	}
	
	
	////////////////////////////////////////////////////////////////////////////////////
	// State Transition Helper Methods
	////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * transition to ready state
	 */
	private void ready() {
		Intent intent = this.getIntent();
		boolean startFromBeginning = intent.getBooleanExtra("startFromBeginning", false);
		//Log.d(TAG,"datavalue "+ startFromBeginning);
		if(startFromBeginning){
			mCurrentState = CALIBRATION_STATE.READY;
			setReadyView();
		}else{//start from second screen of calib process
			mWrapperView.setVisibility(View.GONE); //from setreadyview
			uiHandler.sendEmptyMessage(MESSAGE_WHAT_CALIBRATE_SHOW_INSTRUCTIONS);
		}

	}
	
	/**
	 * Transition to calibrating state
	 */
	private void calibrating() {
		mCurrentState = CALIBRATION_STATE.CALIBRATING;
		setCalibratingView();
		calibrate();
	}
	
	/**
	 * Transition to failed (HW FAULTY) state
	 */
	private void fail(boolean first) {
		mCurrentState = first?
				CALIBRATION_STATE.FAILURE_FIRST:
					CALIBRATION_STATE.FAILURE_SECOND;
		setFailView(first); // Brings up first screen
	}
	
	/**
	 * Transition to incomplete state
	 */
	private void incomplete(boolean first) {
		mCurrentState = first?
				CALIBRATION_STATE.INCOMPLETE_FIRST:
					CALIBRATION_STATE.INCOMPLETE_SECOND;
		setIncompleteView(first); // Brings up first screen
	}



    private class ReallyCompleteTheCalibrationTask
	extends AsyncTask<Void,Void, CALIBRATION_RESULT>{

	@Override
	protected void onPreExecute() {
		FeedbackDialog.showDialog(CompassSensorActivity.this, "Calibrating", null, null, FeedbackDialog.SHOW_SPINNER);
	}
	@Override
	protected CALIBRATION_RESULT doInBackground(Void...params) {
	    CALIBRATION_RESULT result = calibrationComplete();
	    return result;
	}
	@Override
        protected void onPostExecute(CALIBRATION_RESULT result) {
		FeedbackDialog.dismissDialog(CompassSensorActivity.this);
	    finished(result); 
       }
    }
	
	/**
	 * Transition to finished state
	 */
	private void finished(CALIBRATION_RESULT result) {
		if (DEBUG) Log.d(TAG, "finished(result), check if error happend");
		if (DEBUG) Log.d(TAG, "Result : " + result.name());
		switch (result) {
		case FAILURE:
			fail(true);
			return;
		case INCOMPLETE:
			incomplete(true);
			return;
		case SUCCESSFUL:
			mCurrentState = CALIBRATION_STATE.FINISHED;

			// SET FINISHED VIEW
			setFinishedView();
			break;
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////
	// OnKey Listener Methods
	////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * When Key is pressed, check whether it is calibrating actively or just waiting for calibration
	 * if actively calibrating, ignore any key event until calibration
	 */
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (isCalibrating)
			return true;
		
		return super.onKeyUp(keyCode, event);
	}
	
	/**
	 * When Key is pressed, check whether it is calibrating actively or just waiting for calibration
	 * if actively calibrating, ignore any key event until calibration
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (isCalibrating)
			return true;

		switch(keyCode) {
		case KeyEvent.KEYCODE_DPAD_DOWN:
			switch (mCurrentState) {
			case FAILURE_FIRST:
				fail(false);
				return true;
			case FAILURE_SECOND:
				fail(true);
				return true;
			case INCOMPLETE_FIRST:
				incomplete(false);
				return true;
			case INCOMPLETE_SECOND:
				incomplete(true);
				return true;
			default:
				break;
			}
			break;
		case KeyEvent.KEYCODE_DPAD_CENTER:
		case KeyEvent.KEYCODE_ENTER:
			switch (mCurrentState) {
			case READY:
				uiHandler.sendEmptyMessage(MESSAGE_WHAT_CALIBRATE_SHOW_INSTRUCTIONS);
				break;
			case SHOW_INSTRUCTIONS:
			case FAILURE_FIRST:
			case FAILURE_SECOND:
			case INCOMPLETE_FIRST:
			case INCOMPLETE_SECOND:
				// Go back to calibrating state
				calibrating();
				return true;
			case FINISHED:
				uiHandler.sendEmptyMessage(MESSAGE_WHAT_CALIBRATE_EXIT);// Finish after 2 seconds
				break;
			default:
				break;
			}
			
			break;
		}
		
		return super.onKeyDown(keyCode, event);
	}
	
	/**
	 * When Back Button is pressed, check whether it is calibrating actively or just waiting for calibration
	 * if actively calibrating, ignore any key event until calibration
	 */
	@Override
	public void onBackPressed() {
		if (isCalibrating)
			/**
			 * Sorry, but you already triggered calibration, so you better not do anything but waiting!
			 */
			return;
		else {
			/**
			 * let it propagate
			 */
			super.onBackPressed();
			showTutorialOnItemHost();
            CommonUtils.launchParent(this,null,false);
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////
	// Other Instance Methods
	////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Inits the Ready View References and sets all text
	 */
	private void initViewReference() {
		mTopTextView = (TextView)findViewById(R.id.calibrate_top_textview);
		mBottomTextView = (TextView)findViewById(R.id.calibrate_bottom_textview);
		mWrapperView = (LinearLayout)findViewById(R.id.calibrate_ui_wrapper);
		mRootView = (FrameLayout)findViewById(R.id.calibrate_root);
		
		mTempTextView = (TextView) findViewById(R.id.calibrate_temperatures);
		if(SHOW_TEMPERATURES) {
			mTempTextView.setVisibility(View.VISIBLE);
		}
		else {
			mTempTextView.setVisibility(View.GONE);
		}
	}
	/**
	 * Sets Ready View with correct String with arguments
	 */
	private void setReadyView() {
		mWrapperView.setVisibility(View.GONE);
		if(DeviceUtils.isSun()){
			mRootView.setBackgroundResource(R.drawable.compass_calib_default);
		}
		else {
			mRootView.setBackgroundResource(R.drawable.snow_compass_calib_default);
		}
	}
	
	/**
	 * Sets Instruction View.
	 */
	private void setInstructionView() {
		if(DeviceUtils.isSun()) {
			mRootView.setBackgroundResource(R.drawable.compass_calib_instructions);
		} else {
			mRootView.setBackgroundResource(R.drawable.snow_compass_calib_instructions);
		}
	}
	
	/**
	 * Sets the Calibrating State View with Correct Image and Hide Ready View
	 */
	private void setCalibratingView() {
		mWrapperView.setVisibility(View.VISIBLE);
		mTopTextView.setText(CALIBRATE_COUNTDOWN_STEPS+"");
		String text = "Continue to <font color='#FF9933'><strong>spin</strong></font>, "
				+ "<font color='#FF9933'><strong>roll</strong></font> and "
				+ "<font color='#FF9933'><strong>flip</strong></font> the device until the timer expires.";
		mBottomTextView.setText(Html.fromHtml(text));
	}
	
	/**
	 * Sets the Calibrating State View with Correct Image and Hide Ready View
	 */
	private void setFailView(boolean first) {
		mWrapperView.setVisibility(View.GONE);
		if(DeviceUtils.isSun()){
			mRootView.setBackgroundResource(first ? R.drawable.compass_calib_failed_a : R.drawable.compass_calib_failed_b);
		}
		else {
			mRootView.setBackgroundResource(first ? R.drawable.snow_compass_calib_failed_a : R.drawable.snow_compass_calib_failed_b);
		}
	}
	
	/**
	 * Sets the Calibrating State View with Correct Image and Hide Ready View
	 */
	private void setIncompleteView(boolean first) {
		mWrapperView.setVisibility(View.GONE);
		if(DeviceUtils.isSun()){
			mRootView.setBackgroundResource(first ? R.drawable.compass_calib_incomplete_a : R.drawable.compass_calib_incomplete_b);
		}
		else {
			mRootView.setBackgroundResource(first ? R.drawable.snow_compass_calib_incomplete_a : R.drawable.snow_compass_calib_incomplete_b);
		}
	}
	
	/**
	 * Sets the Calibrating State View with Correct Image and Hide Ready View
	 */
	private void setFinishedView() {
		mWrapperView.setVisibility(View.GONE);
		if(DeviceUtils.isSun()){
			mRootView.setBackgroundResource(R.drawable.compass_calib_success);
		}else{
			mRootView.setBackgroundResource(R.drawable.snow_compass_calib_success);
		}

	}
	
	/**
	 * Start of the calibration process
	 */
	private synchronized void calibrate() {
		if (DEBUG) Log.d(TAG,"calibrate()");
		if(!isCalibrating){
			if (DEBUG) Log.d(TAG,"Calibration Started");
			isCalibrating = true;
			mCalibrationTimerThread = new CalibrationTimerThread();
			mCalibrationTimerThread.start();
		}
	}


	/**
	 * All of the calibration process is finished, tell the listener 
	 */
	private synchronized CALIBRATION_RESULT calibrationComplete()   {
		if (DEBUG) Log.d(TAG,"calibartionComplete()");
		isCalibrating = false;

		try {
			/**
			 * Check whether it is hw failure first
			 */
			if (mCompassSensorListener.isHardwareFaulty()) {
				Log.e(TAG,"Calibration Failed");
				return CALIBRATION_RESULT.FAILURE;
			}
			/**
			 * and then if it is just other issue
			 */
			if (!mCompassSensorListener.isCalibrationSuccesful()) {
				Log.e(TAG,"calibration Incomplete with error");
				return CALIBRATION_RESULT.INCOMPLETE;
			}
		} catch (Exception e) {
			Log.e(TAG,"calibration Incomplete with Exception");
			return CALIBRATION_RESULT.INCOMPLETE;
		}

		Settings.System.putString(getContentResolver(),
				SYSTEM_PREF_COMPASS_STATE_KEY,
				SYSTEM_PREF_COMPASS_STATE_VALUE_CALIBRATED);
				
		// EVERYTHING IS FINE!
		return CALIBRATION_RESULT.SUCCESSFUL;
	}
	
	/**
	 * Thread that waits and sends message to the handler for UI update
	 */
	class CalibrationTimerThread extends Thread {
		public void setLEDBlinking(boolean enabled) {
            if (mLedMgr == null) {
                Log.d(TAG, "Retrieving HUDLedManager");
                mLedMgr = HUDLedManager.getInstance();
            }

            if (enabled) {
                mLedMgr.contBlinkPowerLED(HUDLedManager.BRIGHTNESS_NORMAL, 50, 250, true);
            } else {
                mLedMgr.contBlinkPowerLED(0, 0, 0, false);
            }
		}

		@Override
		public void run() {

			CompassUtil.copy(CompassUtil.SYSTEM_SENSORS_CONF, CompassUtil.DATA_SENSORS_CONF);
			Settings.Secure.putInt(getContentResolver(), "hasWrittenMagOffsetsV2", 0);

			try {
				Thread.sleep(SLEEP_TIME_AFTER_CONF_REWRITE);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			setLEDBlinking(true);
			mCompassSensorListener.startCalibration();

			int countdown = CALIBRATE_COUNTDOWN_STEPS;

			while(countdown > 0) {
				Message msg = Message.obtain(uiHandler, MESSAGE_WHAT_CALIBRATE_PROGRESS);
				msg.arg1 = countdown--;
				uiHandler.sendMessage(msg);
				try{
					Thread.sleep(CALIBRATE_INTERVAL);
				} catch (InterruptedException e) {
					Log.e(TAG, "calibrationTimerThread got interrupted");
				}
			}

			mCompassSensorListener.finishCalibration();
			setLEDBlinking(false);

			uiHandler.sendEmptyMessage(MESSAGE_WHAT_CALIBRATE_COMPLETE);

		}
	};
	
	private void showTutorialOnItemHost(){
    	if(CALLED_FROM_INITIAL_SETUP){
    		Settings.System.putInt(getContentResolver(), "com.reconinstruments.itemhost.SHOULD_SHOW_COACHMARKS", 1);
    	}
    }
}

