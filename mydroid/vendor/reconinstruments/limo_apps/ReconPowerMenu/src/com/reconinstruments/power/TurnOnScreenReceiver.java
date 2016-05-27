package com.reconinstruments.power;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import com.reconinstruments.utils.DeviceUtils;
import com.reconinstruments.utils.SettingsUtil;

public class TurnOnScreenReceiver extends BroadcastReceiver {
	static final String TAG = "TurnOnScreenReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		if (!ReconPowerMenuActivity.screenOn) {
			PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			if (!pm.isScreenOn()) {
				Log.d(TAG, "Turn on screen, wake the device up");
				pm.userActivity(System.currentTimeMillis(), false);
			}

            if (DeviceUtils.isSun() && SettingsUtil.getCachableSystemIntOrSet(context, "GlanceReEnable", 0) == 1) {
                // Enable Glance detection before doing display off
                Intent i = new Intent("com.reconinstruments.ACTION_REGISTER_GLANCE");
                context.sendBroadcast(i);

                // Indicate that glance detection is enabled
                SettingsUtil.setSystemInt(context, "GlanceEnabled", 1);

                // Reset flag
                SettingsUtil.setSystemInt(context, "GlanceReEnable", 0);
            }
		}
	}
};
