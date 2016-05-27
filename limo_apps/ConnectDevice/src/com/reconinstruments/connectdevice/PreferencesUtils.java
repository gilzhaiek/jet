package com.reconinstruments.connectdevice;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import com.reconinstruments.utils.DeviceUtils;

// JIRA: MODLIVE-772 Implement bluetooth connection wizard on MODLIVE
public class PreferencesUtils {
	private static final String TAG = "PreferencesUtils";
	private static boolean nameChanged = false;

	public static boolean isNameChanged() {
		return nameChanged;
	}
	
	public static void setBTDeviceName(Context context, String deviceName) {
		if (deviceName != null && !"unknown".equals(deviceName)) {
			SharedPreferences prefs = context.getApplicationContext().getSharedPreferences("last_device", Context.MODE_WORLD_WRITEABLE | Context.MODE_WORLD_READABLE);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("latest_bt_paird_device", deviceName);
			editor.commit();
			Log.d(TAG, "set latest_bt_paird_device to " + deviceName);
		}
	}
	
	public static String getBTDeviceName(Context context) {
		SharedPreferences prefs = context.getApplicationContext().getSharedPreferences("last_device", Context.MODE_WORLD_WRITEABLE | Context.MODE_WORLD_READABLE);
		String name = prefs.getString("latest_bt_paird_device", "unknown");
		Log.d(TAG, "latest_bt_paird_device is " + name);
		return name;
	}
	
	public static void setDeviceName(Context context, String deviceName) {
		if (deviceName != null && !"unknown".equals(deviceName)) {
			if(!deviceName.equals(getDeviceName(context))){
				if(!"unknown".equals(getDeviceName(context))){
						nameChanged = true;
				}
			}else{
				nameChanged = false;
			}
			SharedPreferences prefs = context.getApplicationContext().getSharedPreferences("last_device", Context.MODE_WORLD_WRITEABLE | Context.MODE_WORLD_READABLE);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("latest_paird_device", deviceName);
			editor.commit();
			Log.d(TAG, "set latest_paird_device to " + deviceName);
		}
	}

        public static String getDeviceName(Context context) {
	    String deviceName;
	    if(DeviceUtils.isLimo()){
		deviceName = PreferencesUtils.getDeviceNameLegacy(context);
	    }else{
		deviceName = BTPropertyReader.getBTConnectedDeviceName(context);
		if (deviceName == null) {
		    deviceName = "unknown"; // Default value
		}
	    }
	    return deviceName;
	}

	public static String getDeviceNameLegacy(Context context) {
		SharedPreferences prefs = context.getApplicationContext().getSharedPreferences("last_device", Context.MODE_WORLD_WRITEABLE | Context.MODE_WORLD_READABLE);
		String name = prefs.getString("latest_paird_device", "unknown");
		Log.d(TAG, "latest_paird_device is " + name);
		return name;
	}

	public static void setDeviceAddress(Context context, String deviceAddress) {
		if (deviceAddress != null && !"unknown".equals(deviceAddress)) {
			SharedPreferences prefs = context.getApplicationContext().getSharedPreferences("last_device", Context.MODE_WORLD_WRITEABLE | Context.MODE_WORLD_READABLE);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("latest_paird_device_address", deviceAddress);
			editor.commit();
			Log.d(TAG, "set latest_paird_device_address to " + deviceAddress);
		}
	}

	public static String getDeviceAddress(Context context) {
		SharedPreferences prefs = context.getApplicationContext().getSharedPreferences("last_device", Context.MODE_WORLD_WRITEABLE | Context.MODE_WORLD_READABLE);
		return prefs.getString("latest_paird_device_address", "unknown");
	}

	//0 for android , 1 for ios
	public static void setLastPairedDeviceType(Context context, int deviceType) {
	    Log.d(TAG, "Settings.System: set LastPairedDeviceType to " + deviceType);
	    Settings.System.putInt(context.getContentResolver(), "LastPairedDeviceType", deviceType);

	}

	public static int getLastPairedDeviceType(Context context) {
		Log.d(TAG, "LastPairedDeviceType from Settings.System");
		int res = 0;
		try {
			res = Settings.System.getInt(context.getContentResolver(), "LastPairedDeviceType");
		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}
		return res;
	}

	public static void setLastPairedDeviceName(Context context, String deviceName) {
			Log.d(TAG, "Settings.System: set LastPairedDeviceName to " + deviceName);
			Settings.System.putString(context.getContentResolver(), "LastPairedDeviceName", deviceName);
	}

	public static String getLastPairedDeviceName(Context context) {
		Log.d(TAG, "LastPairedDeviceName from Settings.System");
		String deviceName = Settings.System.getString(context.getContentResolver(), "LastPairedDeviceName");
		if(deviceName == null)
			deviceName = "";
		return deviceName;
	}

	public static void setLastPairedDeviceAddress(Context context, String deviceAddress) {
			Log.d(TAG, "Settings.System: set LastPairedDeviceAddress to " + deviceAddress);
			Settings.System.putString(context.getContentResolver(), "LastPairedDeviceAddress", deviceAddress);
	}

	public static String getLastPairedDeviceAddress(Context context) {
		Log.d(TAG, "LastPairedDeviceAddress from Settings.System");
		String deviceAddress = Settings.System.getString(context.getContentResolver(), "LastPairedDeviceAddress");
		if(deviceAddress == null){
			deviceAddress = "";
		}
		return deviceAddress;
	}

	public static void setReconnect(Context context, boolean reconnnect) {
			int reconnectInt = 0;
			if(reconnnect)
				reconnectInt = 1;
			Log.d(TAG, "Settings.System: set Reconnect to " + reconnectInt);
			Settings.System.putInt(context.getContentResolver(), "Reconnect", reconnectInt);
	}

	public static boolean isReconnect(Context context) {
		Log.d(TAG, "Reconnect from Settings.System");
		int res = 0;
		try {
			res = Settings.System.getInt(context.getContentResolver(), "Reconnect");
		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}
		if(res == 1)
			return true;
		return false;
	}
}
// End of JIRA: MODLIVE-772
