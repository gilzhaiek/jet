package com.reconinstruments.symptomchecker.Tests;

import java.util.HashMap;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.reconinstruments.symptomchecker.Config;
import com.reconinstruments.symptomchecker.TestBase;

public class BluetoothTest extends TestBase{

	private static final String TAG = "SymptomChecker:BluetoothTest";
	
	private class BluetoothThread extends Thread {

		private boolean mRunning = true;

		public BluetoothThread(){
			//Set name for thread
			super("BluetoothThread");
		}

		@Override 
		public void run(){

			// Turn on BLUETOOTH
			boolean testResult = TurnOnBluetooth();

			//Check if BLUETOOTH is on
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

				// Turn off BLUETOOTH
				TurnOffBluetooth();

				//If user terminated the test 
				if(mRunning == false){
					testResult = false;
					SetTestComments("Stopped by user");
				}
				//If BLUETOOTH test within specification
				else if(IsBluetoothWithinSpec()){
					testResult = true;
				}
				else {
					testResult = false;
					SetTestComments("Did not find any bluetooth device near by enabled");
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
	
	/**
	 * Broadcast receiver for BLUETOOTH device detection
	 */
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				mBluetoothDataMap.put(device.getAddress(), device);
			}
		}
	};

	private BluetoothAdapter mBluetoothAdapter;
	private boolean mBluetoothIsEnabled = false;
	private boolean mBTBroadcastReceiverRegistered = false;
	private HashMap<String , BluetoothDevice> mBluetoothDataMap = new HashMap<String , BluetoothDevice>();

	public BluetoothTest(String testName, Activity activity) {
		super(testName, activity);
		//TODO Need to change later
		SetTestPeriod(15);
		SetTimeOutPeriod(30);
	}

	/**
	 * Check if BLUETOOTH test within specified configuration
	 * @return true if size of data is greater than or equal to BLUETOOTH Minimum size
	 */
	public boolean IsBluetoothWithinSpec() {
		for(String key: mBluetoothDataMap.keySet()){
			BluetoothDevice device = mBluetoothDataMap.get(key);
			String bt = device.getName() + ", " + device.getAddress();
			Log.d(TAG, "BT (name, address) : " + bt);
		}
		return mBluetoothDataMap.size() >= Config.BluetoothSize;
	}
	
	@Override
	public Thread GetNewTestThread(){
		return new BluetoothThread();
	}

	@Override
	public void StartTest(){
		super.StartTest();
	}

	@Override 
	public void EndTest(){
		TurnOffBluetooth();
		super.EndTest();
	}

	@Override
	public void ForceStop(){
		((BluetoothThread) GetTestThread()).Stop();
		SetTestResult(false);
		EndTest();
	}

	/**
	 * Turn Off BLUETOOTH and disable discovery mode 
	 */
	private void TurnOffBluetooth() {
		//Cancel Discovery
		mBluetoothAdapter.cancelDiscovery();
		
		// Unregister broadcast receiver for device
		if(mBTBroadcastReceiverRegistered){
			GetContext().unregisterReceiver(mReceiver);
			mBTBroadcastReceiverRegistered = false;
		}
		
		// Note: We don't want to turn off BLUETOOTH of the unit
		//if(mBluetoothIsEnabled){
			// turn off BLUETOOTH
		//	mBluetoothIsEnabled = mBluetoothAdapter.disable();
		//}
	}

	/**
	 * Turn On BLUETOOTH and Discovery for devices
	 * @return true if BLUETOOTH is turned on otherwise false
	 */
	private boolean TurnOnBluetooth(){
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		//Register broadcast receiver for discovery device
		
		if(mBTBroadcastReceiverRegistered == false){
			IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
			GetContext().registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
			mBTBroadcastReceiverRegistered = true;
		}
		
		//Turn on BLUETOOTH if it is not enabled
		if(mBluetoothAdapter.isEnabled() == false){
			mBluetoothIsEnabled = mBluetoothAdapter.enable();
		} else {
			mBluetoothIsEnabled = true;
		}
		
		//time for BLUETOOTH to enable
		for(int i= 0 ; i < 3 ; i ++){
			mBluetoothIsEnabled = mBluetoothAdapter.isEnabled();
			if(mBluetoothIsEnabled){
				break;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		//Start discovery
		mBluetoothAdapter.startDiscovery();
		
		return mBluetoothIsEnabled;
	}
}
