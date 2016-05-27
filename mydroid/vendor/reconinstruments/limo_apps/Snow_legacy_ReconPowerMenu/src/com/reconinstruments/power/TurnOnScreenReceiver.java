package com.reconinstruments.power;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

public class TurnOnScreenReceiver extends BroadcastReceiver {
	static final String TAG = "TurnOnScreenReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		if (!ReconPowerMenuActivity.screenOn) {
			PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			if (!pm.isScreenOn()) {
				Log.d(TAG, "Turn on screen, wake the device up");
				pm.userActivity(System.currentTimeMillis(), true);
			}
		}
	}
};