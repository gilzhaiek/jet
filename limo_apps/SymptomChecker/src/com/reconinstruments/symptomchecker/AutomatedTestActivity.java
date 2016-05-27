package com.reconinstruments.symptomchecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.reconinstruments.symptomchecker.Camera.CameraView;
import com.reconinstruments.symptomchecker.Tests.BluetoothTest;
import com.reconinstruments.symptomchecker.Tests.CameraTest;
import com.reconinstruments.symptomchecker.Tests.GPSTest;
import com.reconinstruments.symptomchecker.Tests.NineAxisTest;
import com.reconinstruments.symptomchecker.Tests.PressureTest;
import com.reconinstruments.symptomchecker.Tests.TemperatureTest;
import com.reconinstruments.symptomchecker.Tests.WifiTest;
import com.reconinstruments.symptomchecker.Tools.ReportGenerator;
import com.reconinstruments.symptomchecker.Tools.TimeOut;

public class AutomatedTestActivity extends Activity{

	private static String TAG = "AutomatedTestActivity";

	List<TestBase> mTestList = new ArrayList<TestBase>();
	private static TextView mTextView;
	private static CameraView mCameraView;
	private static SurfaceView mCameraSurfaceView;
	private ProgressBar mProgressBar;
	private static boolean IsJet = Build.MODEL.equalsIgnoreCase("JET");

	private static BlinkingTitle mBlinkingTitle = null;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event){
		//TODO Warning Screen For User Test In Progress 
		return false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.automated_test_activity);
		mTextView = (TextView) findViewById(R.id.TITLE_TEXT_VIEW);
		mProgressBar = (ProgressBar) findViewById(R.id.PROGRESS_BAR);

		mTestList.clear();

		//Creating Tests
		TestBase btTest = new BluetoothTest(TestTypes.Bluetooth.GetName(), this);
		TestBase cameraTest = new CameraTest(TestTypes.Camera.GetName(), this);
		TestBase gpsTest = new GPSTest(TestTypes.GPS.GetName(), this);
		TestBase naTest = new NineAxisTest(TestTypes.NineAxis.GetName(), this);
		TestBase pressureTest = new PressureTest(TestTypes.Pressure.GetName(), this);
		TestBase tempTest = new TemperatureTest(TestTypes.Temperature.GetName(), this);
		TestBase wifiTest = new WifiTest(TestTypes.Wifi.GetName(), this);

		if(!IsJet){ 		//is Snow we want to skip some tests 
			cameraTest.SetIsSelectedFlag(false);
			tempTest.SetIsSelectedFlag(false);
			wifiTest.SetIsSelectedFlag(false);
		}

		//adding Tests to List
		mTestList.add(btTest);
		mTestList.add(naTest);
		mTestList.add(pressureTest);
		mTestList.add(tempTest);
		mTestList.add(wifiTest);
		mTestList.add(gpsTest);
		mTestList.add(cameraTest);

		mProgressBar.setMax(mTestList.size() + 1); //add one extra for writing report
		mProgressBar.setProgress(0);

		mCameraView = null;

		if(IsJet){
			TurnOnCamera();
		}

		if(mBlinkingTitle == null){
			mBlinkingTitle = new BlinkingTitle();
			mBlinkingTitle.Start(this);
		}else {
			mBlinkingTitle.Finish();
			mBlinkingTitle = new BlinkingTitle();
			mBlinkingTitle.Start(this);
		}

		DeleteAllFiles();

		new Thread("StartProcessThread") {
			@Override
			public void run(){
				StartProcess();
				mBlinkingTitle.Finish();
				DestroyActivity();
			}

		}.start();
	}

	private void DestroyActivity() {
		finish();
	}

	@Override
	protected void onStart(){
		super.onStart();
	}

	private void StartProcess() {
		int index = 0;
		for(TestBase test: mTestList){
			index ++;
			mProgressBar.setProgress(index);

			if(test.IsSelected()){
				test.StartTest();
				TimeOut timeOut = new TimeOut(test.GetTimeOutPeriod());
				timeOut.Start();
				while(test.IsInProgress() && !timeOut.IsTimeOut()){
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				if(timeOut.IsTimeOut()){
					test.ForceStop();
				}

				if (test.GetTestResult()){
					//TODO Test Passed What to Do
				} else {
					if(timeOut.IsTimeOut()) {
						//TODO Test Time Out For This Category What to DO
					}
					else {
						//TODO Test Failed What To Do	
					}
				}
			}
			else {
				test.SetTestComments("Test Was not Selected");
			}
		}		

		ReportGenerator.Generate(mTestList, GetSoftwareVersionName(), GetFormattedKernelVersion());
		mProgressBar.setProgress(mProgressBar.getMax());
	}

	public boolean TurnOnCamera(){
		mCameraSurfaceView = (SurfaceView) findViewById(R.id.cameraSurfaceView);
		if(mCameraView == null){
			mCameraView = new CameraView(this, mCameraSurfaceView);
		}
		return mCameraView != null;
	}

	public boolean TakePicture(String filePath){
		if(mCameraView != null){
			return mCameraView.TakePicture(filePath);
		}
		return false;
	}

	public boolean TurnOffCamera(){
		if(mCameraView != null){
			mCameraView.releaseCamera();
		}
		return true;
	}

	private void DeleteAllFiles() {
		String filePath = Config.Directory;
		File file = new File(filePath);
		String[] files;
		files = file.list();
		if(files != null){
			for(String f: files){
				File myFile = new File(file,f);
				myFile.delete();
			}
		}
	}

	public class BlinkingTitle extends Thread{

		private Activity mParentActivity;
		private boolean mRunning = false;

		@Override
		public void run(){
			while(mRunning){
				try {
					sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(mTextView.isEnabled()){
					mParentActivity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mTextView.setEnabled(false);
						}
					});

				}
				else {
					mParentActivity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mTextView.setEnabled(true);
						}
					});
				}
			}
		}

		public void Start(Activity activity){
			mParentActivity = activity;
			mRunning = true;
			mTextView.setText(R.string.test_in_progress);
			this.start();
		}

		public void Finish(){
			mRunning = false;
			mParentActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mTextView.setText(R.string.finish_testing);
				}
			});

		}
	}

	private String GetSoftwareVersionName(){

		String versionName="unknown";
		try {
			PackageInfo pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.GET_META_DATA);
			versionName = pInfo.versionName;
		} catch (NameNotFoundException e1) {
			Log.e(this.getClass().getSimpleName(), "Name not found", e1);
		}
		return versionName;
	}

	private String GetFormattedKernelVersion() {
		String procVersionStr;

		try {
			BufferedReader reader = new BufferedReader(new FileReader("/proc/version"), 256);
			try {
				procVersionStr = reader.readLine();
			} finally {
				reader.close();
			}

			final String PROC_VERSION_REGEX =
					"\\w+\\s+" + /* ignore: Linux */
							"\\w+\\s+" + /* ignore: version */
							"([^\\s]+)\\s+" + /* group 1: 2.6.22-omap1 */
							"\\(([^\\s@]+(?:@[^\\s.]+)?)[^)]*\\)\\s+" + /* group 2: (xxxxxx@xxxxx.constant) */
							"\\((?:[^(]*\\([^)]*\\))?[^)]*\\)\\s+" + /* ignore: (gcc ..) */
							"([^\\s]+)\\s+" + /* group 3: #26 */
							"(?:PREEMPT\\s+)?" + /* ignore: PREEMPT (optional) */
							"(.+)"; /* group 4: date */

			Pattern p = Pattern.compile(PROC_VERSION_REGEX);
			Matcher m = p.matcher(procVersionStr);

			if (!m.matches()) {
				Log.e(TAG, "Regex did not match on /proc/version: " + procVersionStr);
				return "Unavailable";
			} else if (m.groupCount() < 4) {
				Log.e(TAG, "Regex match on /proc/version only returned " + m.groupCount()
						+ " groups");
				return "Unavailable";
			} else {
				return (new StringBuilder(m.group(1)).append("\n").append(
						m.group(2)).append(" ").append(m.group(3)).append("\n")
						.append(m.group(4))).toString();
			}
		} catch (IOException e) {  
			Log.e(TAG,
					"IO Exception when getting kernel version for Device Info screen",
					e);

			return "Unavailable";
		}
	}
}
