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
 * This is a nine axis sensor test
 */
public class NineAxisTest extends TestBase{

	private static final String TAG = "SymptomChecker:NineAxisTest";

	public class NineAxisData {
		public float X;
		public float Y;
		public float Z;

		public NineAxisData (float x, float y, float z){
			X = x;
			Y = y;
			Z = z;
		}
	}

	private class NineAxisThread extends Thread {

		private boolean mRunning = true;

		public NineAxisThread(){
			//Set name for thread
			super("NineAxisThread");
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

				boolean accResult, magResult, gyroResult;


				//If user terminated the test 
				if(mRunning == false){
					testResult = false;
					SetTestComments("Stopped by user");
				}
				else {
					accResult = CheckAccWithinRage(mAccDataList);
					magResult = CheckMagWithinRage(mMagDataList);
					gyroResult = CheckGyroWithinRage(mGyroDataList);

					//If NineAxis is within range then set test result to true
					if(accResult && magResult && gyroResult){
						testResult = true;
					}
					else {
						testResult = false;
						if(!accResult){
							SetTestComments("Accelerometer not normal");
						}
						if(!magResult){
							SetTestComments("Magnetometer not normal");
						}
						if(!gyroResult){
							SetTestComments("Gyroscope not normal");
						}
					}
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

	private SensorEventListener mNineAxisListener = new SensorEventListener() {

		@Override
		public void onSensorChanged(SensorEvent event) {
			NineAxisData naData = new NineAxisData(event.values[0], event.values[1], event.values[2]);
			switch(event.sensor.getType()){
			case Sensor.TYPE_ACCELEROMETER:
				mAccDataList.add(naData);
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				mMagDataList.add(naData);
				break;
			case Sensor.TYPE_GYROSCOPE:
				mGyroDataList.add(naData);
				break;
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// Not In Used For This Test			
		}
	};

	private int SAMPLEING_RATE = 500000;

	private boolean mAccSensorIsRegistered = false;
	private boolean mMagSensorIsRegistered = false;
	private boolean mGyroSensorIsRegistered = false;

	private SensorManager mSensorManager = null;

	private Sensor mAccSensor = null;
	private Sensor mMagSensor  = null;
	private Sensor mGyroSensor = null;

	private List<NineAxisData> mAccDataList = new ArrayList<NineAxisData>();
	private List<NineAxisData> mMagDataList = new ArrayList<NineAxisData>();
	private List<NineAxisData> mGyroDataList = new ArrayList<NineAxisData>();

	public NineAxisTest(String testName, Activity activity) {
		super(testName, activity);
		//TODO Need to change later
		SetSamplingRate(SAMPLEING_RATE);
		SetTestPeriod(5);
		SetTimeOutPeriod(10);
	}

	public boolean CheckAccWithinRage(List<NineAxisData> dataList) {
		boolean AccResult = false;
		//Check if the list is empty
		if (!dataList.isEmpty()) {
			//Initialise some variables needed for the checking purposes
			float VectorValue = 0.0f;
			float XValue = 0.0f, YValue = 0.0f, ZValue = 0.0f;
			float index = dataList.size();

			for(NineAxisData naData : dataList){
				float xValue = naData.X;
				float yValue = naData.Y;
				float zValue = naData.Z;

				XValue += xValue;
				YValue += yValue;
				ZValue += zValue;

				VectorValue += Math.sqrt(Math.pow(xValue, 2) + Math.pow(yValue, 2) + Math.pow(zValue, 2));
			}

			VectorValue = VectorValue / index;
			XValue = XValue / index;
			YValue = YValue / index;
			ZValue = ZValue / index;

			AccResult = Compare(XValue, Config.AccMinX, Config.AccMaxX)	
					&& Compare(YValue, Config.AccMinY, Config.AccMaxY) 
					&& Compare(ZValue, Config.AccMinZ, Config.AccMaxZ) 
					&& Compare(VectorValue, Config.AccMinMag, Config.AccMaxMag);
			Log.d(TAG,"Acc (x,y,z,magnitude) : " + XValue + " ," + YValue + " ," +  ZValue + " ," + VectorValue);
		}

		return AccResult;
	}

	public boolean CheckMagWithinRage(List<NineAxisData> dataList) {
		boolean MagResult = false;
		//Check if the list is empty
		if (!dataList.isEmpty()) {
			//Initialise some variables needed for the checking purposes
			float VectorValue = 0.0f;
			float XValue = 0.0f, YValue = 0.0f, ZValue = 0.0f;
			float index = dataList.size();

			for(NineAxisData naData : dataList){
				float xValue = naData.X;
				float yValue = naData.Y;
				float zValue = naData.Z;

				XValue += xValue;
				YValue += yValue;
				ZValue += zValue;

				VectorValue += Math.sqrt(Math.pow(xValue, 2) + Math.pow(yValue, 2) + Math.pow(zValue, 2));
			}

			VectorValue = VectorValue / index;
			XValue = XValue / index;
			YValue = YValue / index;
			ZValue = ZValue / index;

			MagResult = Compare(XValue, Config.MagMinX, Config.MagMaxX)
					&& Compare(YValue, Config.MagMinY, Config.MagMaxY)
					&& Compare(ZValue, Config.MagMinZ, Config.MagMaxZ)
					&& Compare(VectorValue, Config.MagMinMag, Config.MagMaxMag);
			Log.d(TAG,"Mag (x,y,z,magnitude) : " + XValue + " ," + YValue + " ," + ZValue + " ," + VectorValue);
		}

		return MagResult;
	}

	public boolean CheckGyroWithinRage(List<NineAxisData> dataList) {
		boolean GyroResult = false;
		//Check if the list is empty
		if (!dataList.isEmpty()) {
			//Initialise some variables needed for the checking purposes
			float XValue = 0.0f, YValue = 0.0f, ZValue = 0.0f;
			float index = dataList.size();

			for(NineAxisData naData : dataList){

				float xValue = naData.X;
				float yValue = naData.Y;
				float zValue = naData.Z;

				XValue += xValue;
				YValue += yValue;
				ZValue += zValue;
			}

			XValue = XValue / index;
			YValue = YValue / index;
			ZValue = ZValue / index;

			GyroResult = Compare(XValue, Config.GyroMinX, Config.GyroMaxX)
					&& Compare(YValue, Config.GyroMinY, Config.GyroMaxY)
					&& Compare(ZValue, Config.GyroMinZ, Config.GyroMaxZ);

			Log.d(TAG,"Gyro (x,y,z) : " + XValue + " ," + YValue + " ," + ZValue);
		}

		return GyroResult;
	}

	private boolean Compare (float value, float min, float max){
		return (min <= value && value <= max);
	}

	@Override
	public Thread GetNewTestThread(){
		return new NineAxisThread();
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
		((NineAxisThread) GetTestThread()).Stop();
		SetTestResult(false);
		EndTest();
	}

	/**
	 * Unregister the nine axis sensor if nine axis sensor was registered 
	 */
	private void UnregisterSensor() {
		// check if accelerometer sensor was registered
		if(mAccSensorIsRegistered){
			// unregistering the accelerometer sensor
			mSensorManager.unregisterListener(mNineAxisListener , mAccSensor);
			mAccSensorIsRegistered = false;
		}

		// check if magnetometer sensor was registered
		if(mMagSensorIsRegistered){
			// unregistering the magnetometer sensor
			mSensorManager.unregisterListener(mNineAxisListener , mMagSensor);
			mMagSensorIsRegistered = false;
		}

		// check if gyroscope sensor was registered
		if(mGyroSensorIsRegistered){
			// unregistering the gyroscope sensor
			mSensorManager.unregisterListener(mNineAxisListener , mGyroSensor);
			mGyroSensorIsRegistered = false;
		}
	}

	/**
	 * Register NineAxis Sensor
	 * @return true if NineAxis sensor was registered correctly otherwise false
	 */
	private boolean RegisterSensor(){
		// initialize the sensor manager
		mSensorManager 	= (SensorManager) GetContext().getSystemService(Context.SENSOR_SERVICE);
		mAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mMagSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

		// registering the nine axis sensor
		mAccSensorIsRegistered = mSensorManager.registerListener(mNineAxisListener, mAccSensor, GetSamplingRate());
		mMagSensorIsRegistered = mSensorManager.registerListener(mNineAxisListener, mMagSensor, GetSamplingRate());
		mGyroSensorIsRegistered = mSensorManager.registerListener(mNineAxisListener, mGyroSensor, GetSamplingRate());
		return mAccSensorIsRegistered && mMagSensorIsRegistered && mGyroSensorIsRegistered;
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
