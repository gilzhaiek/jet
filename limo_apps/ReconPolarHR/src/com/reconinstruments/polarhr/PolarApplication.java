package com.reconinstruments.polarhr;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PolarApplication extends Application {
	
	private String polarMAC = "";
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		polarMAC = mPrefs.getString("POLAR_MAC_ADDRESS", "");
	}
	
	public void setPolarMAC(String address) {
		polarMAC = address;
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putString("POLAR_MAC_ADDRESS", polarMAC); // value to store
		editor.commit();
	}
	
	public String getPolarMAC() {
		return polarMAC;
	}
	
	public boolean isPolarMACSet() {
		return !(polarMAC == "");
	}
}
