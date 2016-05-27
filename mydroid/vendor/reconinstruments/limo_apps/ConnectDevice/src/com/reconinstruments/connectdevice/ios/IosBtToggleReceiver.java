package com.reconinstruments.connectdevice.ios;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class IosBtToggleReceiver extends BroadcastReceiver {
	private static final String TAG = "IosBtToggleReceiver";

	@Override
	public void onReceive(final Context context, Intent intent) {
		Log.d(TAG, "action: " + intent.getAction());
		if (intent.getAction().equals(
				"com.reconinstruments.connect.TOGGLE_IOS_BT")) {
			new Thread() {
				public void run() {
					Intent toggleIntent = new Intent(context,
							IosBtToggleActivity.class);
					toggleIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
							| Intent.FLAG_ACTIVITY_SINGLE_TOP);
					context.startActivity(toggleIntent);
				}
			}.start();
		}
	}
}
