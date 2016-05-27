package com.reconinstruments.detectstoragestate;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";
	
	Intent serviceIntent;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		serviceIntent = new Intent(this, DetectService.class);
		
		startService(serviceIntent);
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		stopService(serviceIntent);
		
	}
}