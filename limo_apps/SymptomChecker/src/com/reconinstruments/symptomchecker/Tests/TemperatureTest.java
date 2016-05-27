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
 * This is a temperature sensor test
 */
public class TemperatureTest extends TestBase{

	private static final String TAG = "SymptomChecker:TemperatureTest";
	
	private class TemperatureThread extends Thread {

		private boolean mRunning = true;

		public TemperatureThread(){
			//Set name for thread
			super("TemperatureThread");
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
				
				//Get the average temperature during the test period
				float average = GetAverageTemperature();
				Log.d(TAG,"Average Temperature : " + average);

				//If user terminated the test 
				if(mRunning == false){
					testResult = false;
					SetTestComments("Stopped by user");
				}
				//If average temperature is within range then set test result to true
				else if(Config.TemperatureMin <= average && average <= Config.TemperatureMax){
					testResult = true;
					SetTestComments("Temperature Average " + average);
				}
				else {
					testResult = false;
					SetTestComments("Temperature Average not normal " + average);
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

	private SensorEventListener mTemperatureListener = new SensorEventListener() {

		@Override
		public void onSensorChanged(SensorEvent event) {
			if(event.sensor.getType() == Sensor.TYPE_TEMPERATURE){
				mTemperatureDataList.add(event.values[0]);
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// Not In Used For This Test			
		}
	};

	private int SAMPLEING_RATE = 1000000;

	private boolean mTemperatureSensorIsRegistered = false; 
	private SensorManager mSensorManager = null;

	private Sensor mTemperatureSensor = null;
	private List<Float> mTemperatureDataList = new ArrayList<Float>();

	public TemperatureTest(String testName, Activity activity) {
		super(testName, activity);
		//TODO Need to change later
		SetSamplingRate(SAMPLEING_RATE);
		SetTestPeriod(5);
		SetTimeOutPeriod(10);
	}

	@Override
	public Thread GetNewTestThread(){
		return new TemperatureThread();
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
		((TemperatureThread) GetTestThread()).Stop();
		SetTestResult(false);
		EndTest();
	}


	/**
	 * Calculate the Average Temperature
	 * @return average Temperature or -99999.0 no value to calculate
	 */
	private float GetAverageTemperature(){
		float sum = 0;
		//Check if DataList is empty, if empty return -99999.00f
		if(mTemperatureDataList.isEmpty()){
			return -99999.00f;
		}

		//sum of the data list
		for(Float f : mTemperatureDataList){
			sum += f;
		}

		//divide the sum by the data list size for average
		return sum / mTemperatureDataList.size();		
	}


	/**
	 * Unregister the temperature sensor if temperature sensor was registered 
	 */
	private void UnregisterSensor() {
		// check if pressure sensor was registered
		if(mTemperatureSensorIsRegistered){
			// unregistering the pressure sensor
			mSensorManager.unregisterListener(mTemperatureListener , mTemperatureSensor);
			mTemperatureSensorIsRegistered = false;
		}
	}

	/**
	 * Register Temperature Sensor
	 * @return true if temperature sensor was registered correctly otherwise false
	 */
	private boolean RegisterSensor(){
		// initialize the sensor manager
		mSensorManager 	= (SensorManager) GetContext().getSystemService(Context.SENSOR_SERVICE);
		mTemperatureSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_TEMPERATURE);

		// registering the temperature sensor
		mTemperatureSensorIsRegistered = mSensorManager.registerListener(mTemperatureListener, mTemperatureSensor, GetSamplingRate());
		return mTemperatureSensorIsRegistered;
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
