package com.reconinstruments.dashlauncher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.os.Handler;
import android.os.PowerManager;
import android.limopm.LimoPMNative;
import android.os.Build;

public class WarningActivity extends Activity {

    public static final String TAG = "WarningActivity";
     private static final long SCREEN_OFF_TIME = 60000; // 1 min
     private static final long POWER_OFF_TIME = 240000; // 4 min
    //    private static final long SCREEN_OFF_TIME = 10000; // 10 sec
    //    private static final long POWER_OFF_TIME = 30000; // 30 sec

    public static final int POWER_MODE_NORMAL = 1;
    public static final int POWER_MODE_DISPLAY_OFF = 2;
    private static boolean screenOn = true;

    
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
		
	this.setContentView(R.layout.warning_layout);
    }

    public void onResume() {
	super.onResume();
	// Set the timer for screen off
	Log.v(TAG,"screen off timer started");
	timerHandler.postDelayed(screenOffRunnable, SCREEN_OFF_TIME );
    }
    public void onPause() {
	resumeNormalOperation();
	super.onPause();
    }


    public boolean onTouchEvent (MotionEvent event) {
	if (!screenOn) {	// We don'do much with keys if screen is off
	    resumeNormalOperation();
	    Log.v(TAG,"screen off timer started");
	    timerHandler.postDelayed(screenOffRunnable, SCREEN_OFF_TIME );
	    return true;
	}
	
	startActivity(new Intent(getApplicationContext(), com.reconinstruments.dashlauncher.livestats.LiveStatsActivity.class));
	overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
	return true;

    }
    public boolean onKeyUp(int keyCode, KeyEvent event) {
	Log.v(TAG, "onKeyUp");
	if (!screenOn) {	// We don'do much with keys if screen is off
	    resumeNormalOperation();
	    Log.v(TAG,"screen off timer started");
	    timerHandler.postDelayed(screenOffRunnable, SCREEN_OFF_TIME );
	    return true;
	}
	if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||  keyCode == KeyEvent.KEYCODE_POWER) {
	    startActivity(new Intent(getApplicationContext(), com.reconinstruments.dashlauncher.livestats.LiveStatsActivity.class));
	    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
	    return true;
	}
		
	return super.onKeyUp(keyCode, event);
    }
	
    public void onBackPressed() {
	return;
    }

    Handler timerHandler = new Handler ();

    Runnable screenOffRunnable = new Runnable () {
	    public void run() {
		Log.v(TAG,"Screen off");
		// turn off screen;
		screenOn = false;
		if (DeviceUtils.isLimo()) {
		    LimoPMNative.SetPowerMode(POWER_MODE_DISPLAY_OFF);
		}
		// start powerOff timer:
		Log.v(TAG,"Power off timer started");
		timerHandler.postDelayed(powerOffRunnable, POWER_OFF_TIME);
	    }
	};

    Runnable powerOffRunnable = new Runnable () {
	    public void run() {
		Log.v(TAG,"Power off");
		// power off
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		pm.reboot("BegForShutdown"); // Magic reason that
					     // causes shutdown as
					     // opposed to reboot
	    }
	};

    private void resumeNormalOperation () {
	Log.v(TAG,"resume Normal operations");
	// Cancel screenOff timer (if any):
	timerHandler.removeCallbacks(screenOffRunnable);
	//Cancel PowerOff timer (if any):
	timerHandler.removeCallbacks(powerOffRunnable);
	//make sure the backlight is on
	screenOn = true;
	if (DeviceUtils.isLimo()) {
	    LimoPMNative.SetPowerMode(POWER_MODE_NORMAL);
	}
    }
}
