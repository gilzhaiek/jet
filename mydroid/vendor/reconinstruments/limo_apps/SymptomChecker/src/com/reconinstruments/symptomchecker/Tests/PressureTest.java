package com.reconinstruments.symptomchecker.Tests;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.reconinstruments.symptomchecker.Config;
import com.reconinstruments.symptomchecker.TestBase;

/**
 * @author tomaslai
 * This is a pressure sensor test
 */
public class PressureTest extends TestBase{

	private static final String TAG = "SymptomChecker:PressureTest";
	
	private class PressureThread extends Thread {

		private boolean mRunning = true;

		public PressureThread(){
			//Set name for thread
			super("PressureThread");
		}

		@Override 
		public void run(){

			// Registering Pressure Sensor
			boolean testResult = RegisterSensor();

			//Check if sensor register successfully
			if(testResult){

				//Wait for period the test was set 				
				for(int i = 0; i < GetTestPeriod(); i ++){
					//If user terminated the test
					if(mRunning == false){
						break;
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				//Unregister sensor
				UnregisterSensor();

				//Get the average pressure during the test period
				float average = GetAveragePressure();
				Log.d(TAG,"Average Pressure : " + average);
				
				//If user terminated the test 
				if(mRunning == false){
					testResult = false;
					SetTestComments("Stopped by user");
				}
				//If average pressure is within range then set test result to true
				else if(Config.PressureMin <= average && average <= Config.PressureMax){
					testResult = true;
					SetTestComments("Pressure Average " + average);
				}
				else {
					testResult = false;
					SetTestComments("Pressure Average not normal " + average);
				}
			}

			// store test result
			SetTestResult(testResult);

			//finish test
			EndTest();	
		}

		public void Stop(){
			mRunning = false;
		}
	}

	private SensorEventListener mPressureListener = new SensorEventListener() {

		@Override
		public void onSensorChanged(SensorEvent event) {
			if(event.sensor.getType() == Sensor.TYPE_PRESSURE){
				mPressureDataList.add(event.values[0]);
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// Not In Used For This Test			
		}
	};

	private int SAMPLEING_RATE = 1000000;

	private boolean mPressureSensorIsRegistered = false; 
	private SensorManager mSensorManager = null;

	private Sensor mPressureSensor = null;
	private List<Float> mPressureDataList = new ArrayList<Float>();



	public PressureTest(String testName, Activity activity) {
		super(testName, activity);
		//TODO Need to change later
		SetSamplingRate(SAMPLEING_RATE);
		SetTestPeriod(5);
		SetTimeOutPeriod(10);
	}

	@Override
	public Thread GetNewTestThread(){
		return new PressureThread();
	}

	@Override
	public void StartTest(){
		super.StartTest();
	}

	@Override 
	public void EndTest(){
		UnregisterSensor();
		super.EndTest();
	}

	@Override
	public void ForceStop(){
		((PressureThread) GetTestThread()).Stop();
		SetTestResult(false);
		EndTest();
	}


	/**
	 * Calculate the Average Pressure
	 * @return average Pressure or -99999.0 no value to calculate
	 */
	private float GetAveragePressure(){
		float sum = 0;
		//Check if DataList is empty, if empty return -99999.00f
		if(mPressureDataList.isEmpty()){
			return -99999.00f;
		}

		//sum of the data list
		for(Float f : mPressureDataList){
			sum += f;
		}

		//divide the sum by the data list size for average
		return sum / mPressureDataList.size();		
	}


	/**
	 * Unregister the pressure sensor if pressure sensor was registered 
	 */
	private void UnregisterSensor() {
		// check if pressure sensor was registered
		if(mPressureSensorIsRegistered){
			// unregistering the pressure sensor
			mSensorManager.unregisterListener(mPressureListener , mPressureSensor);
			mPressureSensorIsRegistered = false;
		}
	}

	/**
	 * Register Pressure Sensor
	 * @return true if pressure sensor was registered correctly otherwise false
	 */
	private boolean RegisterSensor(){
		// initialize the sensor manager
		mSensorManager 	= (SensorManager) GetContext().getSystemService(Context.SENSOR_SERVICE);
		mPressureSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

		// registering the pressure sensor
		mPressureSensorIsRegistered = mSensorManager.registerListener(mPressureListener, mPressureSensor, GetSamplingRate());
		return mPressureSensorIsRegistered;
	}

	/**
	 * Set the sampling rate for pressure sensor 
	 * @param microSeconds refresh rate of the sensor in microSeconds
	 */
	private void SetSamplingRate(int microSeconds){
		SAMPLEING_RATE = microSeconds;
	}

	/**
	 * Get the sampling rate for pressure sensor
	 * @return sampling rate in microSeconds
	 */
	private int GetSamplingRate (){
		return SAMPLEING_RATE;
	}
}
