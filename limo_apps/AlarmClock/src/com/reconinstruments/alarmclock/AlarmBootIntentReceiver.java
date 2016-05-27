package com.reconinstruments.alarmclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmBootIntentReceiver extends BroadcastReceiver{
	public static final String PREFS_NAME = "AlarmPrefsFile";
	@Override
	public void onReceive(Context context, Intent intent) {

		Intent serviceIntent = new Intent(context, AlarmStartupService.class);
	    context.startService(serviceIntent);

	}
}