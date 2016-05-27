package com.reconinstruments.symptomchecker.Tests;

import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.reconinstruments.symptomchecker.Config;
import com.reconinstruments.symptomchecker.TestBase;

/**
 * @author tomaslai
 * This is a WIFI test
 */
public class WifiTest extends TestBase{

	private static final String TAG = "SymptomChecker:WifiTest";
	
	private class WifiThread extends Thread {

		private boolean mRunning = true;

		public WifiThread(){
			//Set name for thread
			super("WifiThread");
		}

		@Override 
		public void run(){

			// Turn on WIFI
			boolean testResult = TurnOnWifi();
			
			//Check if WIFI turned on successfully
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
					ScanWifi();
				}


				//Turn off WIFI
				TurnOffWifi();

				//If user terminated the test 
				if(mRunning == false){
					testResult = false;
					SetTestComments("Stopped by user");
				}
				//If WIFI test within specification
				else if(IsWifiWithinSpec(mWifiDataMap)){
					testResult = true;
				}
				else {
					testResult = false;
					SetTestComments("Did not find any Wifi hotspot near by");
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

	private WifiManager mWifiManager;
	private HashMap<String, ScanResult> mWifiDataMap = new HashMap<String, ScanResult>();

	public WifiTest(String testName, Activity activity) {
		super(testName, activity);
		//TODO Need to change later
		SetTestPeriod(10);
		SetTimeOutPeriod(20); // Timout need to set longer because enable and disable wifi takes time
	}

	@Override
	public Thread GetNewTestThread(){
		return new WifiThread();
	}

	@Override
	public void StartTest(){
		super.StartTest();
	}

	@Override 
	public void EndTest(){
		TurnOffWifi();
		super.EndTest();
	}

	@Override
	public void ForceStop(){
		((WifiThread) GetTestThread()).Stop();
		SetTestResult(false);
		EndTest();
	}

	/**
	 * Turn off WIFI is it was turned on
	 */
	private void TurnOffWifi() {
		// check if WIFI was on
		if(IsWifiOn()){
			//Turn Off WIFI
			mWifiManager.setWifiEnabled(false);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Check if WIFI is turned on
	 * @return true if WIFI is on otherwise false
	 */
	private boolean IsWifiOn(){
		return mWifiManager.isWifiEnabled();
	}

	/**
	 * Turn on WIFI if WIFI was not enabled
	 * @return true if WIFI turned on successfully, otherwise false
	 */
	private boolean TurnOnWifi(){
		//Get WIFI Manager
		mWifiManager = (WifiManager) GetContext().getSystemService(Context.WIFI_SERVICE);

		//If WIFI was already on then return true
		if(IsWifiOn()){
			return true;
		}
		else {
			//Turn on WIFI
			mWifiManager.setWifiEnabled(true);
		}

		// WIFI needs time to be enable, so here set a roughly 4 second waiting time
		for(int i = 0 ; i< 8 ; i ++){
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(IsWifiOn()){
				break;
			}
		}
		return IsWifiOn();
	}

	/**
	 * Put all scanned WIFI result into WIFI data map
	 */
	private void ScanWifi(){
		// Scan for WIFI signal
		List<ScanResult> scanResults = mWifiManager.getScanResults();
		// If result is not null
		if(scanResults != null){
			// Put into dataMap
			for(ScanResult sr: scanResults){
				mWifiDataMap.put(sr.SSID, sr);
			}
		}
	}

	/**
	 * Check if WIFI test within specified configuration
	 * @param dataMap HashMap of the Scan Result from WIFI
	 * @return true if size of data is greater than or equal to WIFI Minimum size
	 */
	private boolean IsWifiWithinSpec(HashMap<String, ScanResult> dataMap) {
		for(String key: dataMap.keySet()){
			ScanResult hotspot = dataMap.get(key);
			String st = hotspot.SSID + ", " + hotspot.frequency;
			Log.d(TAG, "Wifi (name, frequency) : " + st);
		}
		return dataMap.size() >= Config.WifiSize;
	}
}