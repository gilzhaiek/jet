package com.reconinstruments.symptomchecker.Tests;

import android.app.Activity;
import android.util.Log;

import com.reconinstruments.symptomchecker.AutomatedTestActivity;
import com.reconinstruments.symptomchecker.Config;
import com.reconinstruments.symptomchecker.TestBase;

public class CameraTest extends TestBase{

	private static final String TAG = "SymptomChecker:CameraTest";
	
	private class CameraThread extends Thread {

		private boolean mRunning = true;

		public CameraThread(){
			//Set name for thread
			super("CameraThread");
		}

		@Override 
		public void run(){

			// GetCamera
			Log.d(TAG,"Get Camera");
			boolean testResult = GetCamera();
			
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			//Check if Camera accessible
			if(testResult){
				// Take Photo
				Log.d(TAG,"Take Photo");
				testResult = TakePhoto();
			}			

			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			//Release Camera
			Log.d(TAG,"Release Camera");
			if(testResult){
				testResult = ReleaseCamera();
			}
			
			//If user terminated the test 
			if(mRunning == false){
				testResult = false;
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

	public CameraTest(String testName, Activity activity) {
		super(testName, activity);
		//TODO Need to change later
		SetTestPeriod(10);
		SetTimeOutPeriod(10);
	}

	@Override
	public Thread GetNewTestThread(){
		return new CameraThread();
	}

	@Override
	public void StartTest(){
		super.StartTest();
	}

	@Override 
	public void EndTest(){
		ReleaseCamera();
		super.EndTest();
	}

	@Override
	public void ForceStop(){
		((CameraThread) GetTestThread()).Stop();
		SetTestResult(false);
		EndTest();
	}

	/**
	 * Release the camera
	 * @return true if camera released correctly otherwise false
	 */
	private boolean ReleaseCamera() {
		AutomatedTestActivity activity = (AutomatedTestActivity) GetParentActivity();
		return activity.TurnOffCamera();
	}
	
	/**
	 * Take a photo
	 * @return true if photo taken correctly otherwise false
	 */
	private boolean TakePhoto() {
		AutomatedTestActivity activity = (AutomatedTestActivity) GetParentActivity();
		String filePath = Config.Directory + "/" + Config.CameraFile;
		return activity.TakePicture(filePath);
	}

	/**
	 * Get Camera
	 * @return true if acquired camera successfully otherwise false
	 */
	private boolean GetCamera(){
		AutomatedTestActivity activity = (AutomatedTestActivity) GetParentActivity();
		return activity.TurnOnCamera();
	}
		
}
