package com.reconinstruments.symptomchecker.Tests;

import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.LocationManager;
import android.util.Log;

import com.reconinstruments.symptomchecker.Config;
import com.reconinstruments.symptomchecker.TestBase;

public class GPSTest extends TestBase{

	private static final String TAG = "SymptomChecker:GPSTest";
	
	private class GPSThread extends Thread {

		private boolean mRunning = true;

		public GPSThread(){
			//Set name for thread
			super("GPSThread");
		}

		@Override 
		public void run(){

			// Register Location Listener
			boolean testResult = RegisterLocationListener();

			//Check if GPS turned on successfully
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

				// Unregister Location Listener
				UnregisterLocationListener();

				//If user terminated the test 
				if(mRunning == false){
					testResult = false;
					SetTestComments("Stopped by user");
				}
				//If GPS test within specification
				else if(IsGPSWithinSpec()){
					testResult = true;
				}
				else {
					testResult = false;
					SetTestComments("Not able to receive any gps signal");
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

	private GpsStatus.Listener mGPSStatusListener = new GpsStatus.Listener() {
		final int GPS_EVENT_STARTED = 1;
		final int GPS_EVENT_STOPPED = 2;
		final int GPS_EVENT_FIRST_FIX = 3;
		final int GPS_EVENT_SATELLITE_STATUS = 4;
		@Override
		public void onGpsStatusChanged(int event) {
			switch(event){
			case GPS_EVENT_STARTED:
				// Not Tracked
				break;
			case GPS_EVENT_STOPPED:
				// Not Tracked
				break;
			case GPS_EVENT_FIRST_FIX:
				// Not Tracked
				break;
			case GPS_EVENT_SATELLITE_STATUS:
				UpdateSatelliteStatus();
				break;
			}
		}

	};

	private boolean mGPSStatusListenerIsRegistered = false; 
	private LocationManager mLocationManager = null;
	private HashMap<String , GpsSatellite> mGPSDataMap = new HashMap<String , GpsSatellite>();

	public GPSTest(String testName, Activity activity) {
		super(testName, activity);
		//TODO Need to change later
		SetTestPeriod(15);
		SetTimeOutPeriod(30);
	}

	/**
	 * Check if GPS test within specified configuration
	 * @return true if size of data is greater than or equal to GPS Minimum size
	 */
	public boolean IsGPSWithinSpec() {
		for(String key: mGPSDataMap.keySet()){
			GpsSatellite sat = mGPSDataMap.get(key);
			String st = sat.getPrn() + ", " + sat.getSnr();
			Log.d(TAG, "GPS (ID, SNR) : " + st);
		}
		return mGPSDataMap.size() >= Config.GPSSize;
	}

	private void UpdateSatelliteStatus() {
		GpsStatus status = mLocationManager.getGpsStatus(null);
		Iterator<GpsSatellite> SatItr = status.getSatellites().iterator();
		while(SatItr.hasNext()){
			GpsSatellite gpsSatellite = SatItr.next();
			mGPSDataMap.put(String.valueOf(gpsSatellite.getPrn()), gpsSatellite);
		}
	}

	@Override
	public Thread GetNewTestThread(){
		return new GPSThread();
	}

	@Override
	public void StartTest(){
		super.StartTest();
	}

	@Override 
	public void EndTest(){
		UnregisterLocationListener();
		super.EndTest();
	}

	@Override
	public void ForceStop(){
		((GPSThread) GetTestThread()).Stop();
		SetTestResult(false);
		EndTest();
	}

	/**
	 * Unregister the GPS Status Listener if it was registered 
	 */
	private void UnregisterLocationListener() {
		// check if gps status listener was registered
		if(mGPSStatusListenerIsRegistered){
			// unregistering the gps status listener
			mLocationManager.removeGpsStatusListener(mGPSStatusListener);
			mGPSStatusListenerIsRegistered = false;
		}
	}

	/**
	 * Register GPS Status Listener
	 * @return true if GPS status listener was registered correctly otherwise false
	 */
	private boolean RegisterLocationListener(){
		
		GetParentActivity().runOnUiThread(new Runnable(){
			@Override
			public void run() {
				// Initialize location manager
				mLocationManager = (LocationManager) GetContext().getSystemService(Context.LOCATION_SERVICE);
				// registering GPS Status Listener
				mGPSStatusListenerIsRegistered = mLocationManager.addGpsStatusListener(mGPSStatusListener);
			}
		});
		
		//time for gps status listener to register
		for(int i= 0 ; i < 3 ; i ++){
			if(mGPSStatusListenerIsRegistered){
				break;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return mGPSStatusListenerIsRegistered;
	}
}
