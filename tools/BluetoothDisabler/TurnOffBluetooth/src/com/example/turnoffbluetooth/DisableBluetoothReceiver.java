package com.example.turnoffbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DisableBluetoothReceiver extends BroadcastReceiver {
	private static final String TAG = DisableBluetoothReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "disable Bluetooth");
		
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter.disable()) {
        	Log.d(TAG, "disable Bluetooth successful");
        } else {
        	Log.d(TAG, "disable Bluetooth failed");
        }
		
	}

}
