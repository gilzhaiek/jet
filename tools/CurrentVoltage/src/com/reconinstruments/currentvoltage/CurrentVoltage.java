package com.reconinstruments.currentvoltage;

import android.app.Application;

public class CurrentVoltage extends Application {
	private static final String TAG = CurrentVoltage.class.getSimpleName();
	
	public static final boolean DEBUG = true;
	
	public static final String PREF_KEY_REFRESH_RATE = "refresh_rate";
	
	public static int refreshRate = 2;
	
	public static boolean averageMode = false;
	
	public static enum GraphType {
		VOLTAGE, CURRENT
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
	}

}
