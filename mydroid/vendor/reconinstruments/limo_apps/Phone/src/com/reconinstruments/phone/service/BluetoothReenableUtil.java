package com.reconinstruments.phone.service;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class BluetoothReenableUtil {
    public PhoneRelayService mTheService;
    public BluetoothReenableUtil (PhoneRelayService prs) {
	mTheService = prs;
    }
    private static final String TAG = "BluetoothReenableUtil";

    public void enableBTIfNecessary () {
	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mTheService);

	boolean state = prefs.getBoolean("bt_state", false);
	//In the above line False means it was unchecked (disabled) by
	//user or app.  NOT system.

	boolean lock = prefs.getBoolean("state_lock", false);
	//In the above line True means it was reboot by user or app.

	Log.d(TAG, "mBluetoothAdapter.isEnabled()=" + mTheService.mBluetoothAdapter.isEnabled() + " state=" + state + " lock=" + lock);
	SharedPreferences.Editor editor = prefs.edit();
	editor.putBoolean("state_lock", false);
	editor.commit();
	Log.d(TAG, "set lock to " + false);
	if (!mTheService.mBluetoothAdapter.isEnabled() && state ) {
	    Intent enableBtIntent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE);
	    enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    mTheService.startActivity(enableBtIntent);
	    //		if(mBluetoothAdapter.enable()){
	    //			editor.putBoolean("bt_state", true);
	    //			editor.commit();
	    //		}
	} else if(mTheService.mBluetoothAdapter.isEnabled()){
	    editor.putBoolean("bt_state", true);
	    editor.commit();
	}
    }

    
}