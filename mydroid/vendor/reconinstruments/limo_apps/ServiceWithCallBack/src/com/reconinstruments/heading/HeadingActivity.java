package com.reconinstruments.heading;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.location.Location;

public class HeadingActivity extends Activity implements HeadingListener
{
	public static final String TAG = "Heading Activity";
	private HeadingManager mHeadingManager;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mHeadingManager = new HeadingManager(getApplicationContext(),this);
	}

	@Override
	public void onStart() {
		super.onStart();
		mHeadingManager.initService();
	}

	@Override
	public void onStop() {
		mHeadingManager.releaseService();
		super.onStop();
	}

	@Override
	public void onHeadingChanged(HeadingEvent he) {
		Log.v(TAG,"Yaw is "+he.mYaw);
		// while(true) {
		//     try {
		// 	long numMillisecondsToSleep = 100; // 1 seconds
		// 	Thread.sleep(numMillisecondsToSleep);
		//     } catch (InterruptedException e) {
		//     }
		// }
	}

}
