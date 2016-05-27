package com.reconinstruments.intro.instore;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class InStoreDemoService extends Service {

	boolean isOakley = false;
	
	private static final String TAG = "ReplayService";
	Handler mHandler = new Handler();
	private static Runnable mPlayDemoRunnable;
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	public void onCreate() {
		super.onCreate();
		
		Log.v(TAG, "onCreate()");
		
		mPlayDemoRunnable = new DemoRunnable();
		
		try {
			ApplicationInfo ai = getPackageManager().getApplicationInfo(this.getPackageName(), PackageManager.GET_META_DATA);
			isOakley = ai.metaData.getBoolean("isOakley");
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		if(!isOakley) this.stopSelf();
		
		registerReceiver(keyEventsReceiver, new IntentFilter("com.reconinstruments.system.keyintercept"));
		
		// post a runnable for 30s from now 
		// and cancel it if there is user activity
		mHandler.postDelayed(new DemoRunnable(), 30000);
		
		Toast.makeText(this, "Store Demo Mode Enabled", Toast.LENGTH_SHORT).show();
	}
	
	public void onDestroy() {
		super.onDestroy();
		
		unregisterReceiver(keyEventsReceiver);
		mHandler.removeCallbacks(mPlayDemoRunnable);
		
		Toast.makeText(this, "Store Demo Mode Disabled", Toast.LENGTH_SHORT).show();
	}
	
	BroadcastReceiver keyEventsReceiver = new BroadcastReceiver() {
		
		// Received user activity, restart delay for runnable
		@Override
		public void onReceive(Context context, Intent arg1) {
			Log.v(TAG, "keyEvent");
			mHandler.removeCallbacks(mPlayDemoRunnable);
			mHandler.postDelayed(mPlayDemoRunnable, 30000);
		}
		
	};
	
	class DemoRunnable implements Runnable {

		@Override
		public void run() {
			// start activity
			Log.v(TAG, "starting demo screensaver");
			Intent i = new Intent(getApplicationContext(), com.reconinstruments.intro.instore.InStoreDemoActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
		}
		
	}
}
