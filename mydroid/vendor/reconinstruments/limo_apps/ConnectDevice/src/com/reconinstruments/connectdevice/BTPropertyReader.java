package com.reconinstruments.connectdevice;

import android.content.Context;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

public class BTPropertyReader {
	private static final String TAG = "BTPropertyReader";

	// 0 disconnected, 1 connecting, 2 connected
	public static int getBTConnectionState(Context context){
		Log.d(TAG, "getBTConnectionState from Settings.System");
		int res = 0;
		try {
			res = Settings.System.getInt(context.getContentResolver(), "BTConnectionState");
		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}
		Log.d(TAG, "BTConnectionState = " + res);
		return res;
	}
	
	// 0 android, 1 ios
	public static int getBTConnectedDeviceType(Context context){
		Log.d(TAG, "BTConnectedDeviceType from Settings.System");
		int res = 0;
		try {
			res = Settings.System.getInt(context.getContentResolver(), "BTConnectedDeviceType");
		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}
		return res;
	}
	
	public static String getBTConnectedDeviceName(Context context){
		Log.d(TAG, "BTConnectedDeviceName from Settings.System");
		return Settings.System.getString(context.getContentResolver(), "BTConnectedDeviceName");
	}
	
	public static String getBTConnectedDeviceAddress(Context context){
		Log.d(TAG, "BTConnectedDeviceAddress from Settings.System");
		return Settings.System.getString(context.getContentResolver(), "BTConnectedDeviceAddress");
	}
}
