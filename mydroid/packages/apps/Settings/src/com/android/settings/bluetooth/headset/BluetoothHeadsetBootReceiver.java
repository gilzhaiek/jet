package com.android.settings.bluetooth.headset;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BluetoothHeadsetBootReceiver extends BroadcastReceiver {
	private static final String TAG = BluetoothHeadsetBootReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "Starting BluetoothHeadsetService on Boot...");
		context.startService(new Intent(context, BluetoothHeadsetService.class));
	}

}
