package com.reconinstruments.jetapplauncher.settings.service;

import java.util.TimeZone;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityService;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.hud_phone_status_exchange.TimesyncRequestMessage;
import com.reconinstruments.hudservice.helper.HUDConnectivityHelper;
import com.reconinstruments.hud_phone_status_exchange.TimesyncResponseMessage;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import com.reconinstruments.utils.SettingsUtil;

public class SyncTimeReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent.getAction().equals("HUD_STATE_CHANGED")){
			Bundle bundle = intent.getExtras();
			int b = bundle.getInt("state"); // connectionstate
			if(b == 2){ //connected
				syncTime(context);
			}
		}else if(intent.getAction().equals(TimesyncResponseMessage.INTENT)){
			if (intent.hasExtra("message")) {
				String xml = new String(new HUDConnectivityMessage(intent.getByteArrayExtra("message")).getData());
				TimesyncResponseMessage msg = new TimesyncResponseMessage(xml);
				int phoneOffset = msg.getUtcOffset();
				long phoneTime = msg.getUtcTime();
				if(SettingsUtil.getSyncTimeWithSmartPhone(context)){
					AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
					alarm.setTime(phoneTime);
					if(TimeZone.getDefault().getRawOffset() != phoneOffset){
						String[] tzStrs = TimeZone.getAvailableIDs(phoneOffset);
						if(tzStrs.length > 0){
							alarm.setTimeZone(tzStrs[0]);
						}
					}
					Intent timeChanged = new Intent(Intent.ACTION_TIME_CHANGED);
					context.sendBroadcast(timeChanged);
				}
			}
		}
	}

	private void syncTime(final Context context) {
		if(SettingsUtil.getSyncTimeWithSmartPhone(context)){
			new CountDownTimer(1 * 1000, 1000) {
				public void onTick(long millisUntilFinished) {
				}
				public void onFinish() {
					HUDConnectivityMessage cMsg = new HUDConnectivityMessage();
					TimesyncRequestMessage msg = new TimesyncRequestMessage();
					cMsg.setSender(SyncTimeReceiver.class.getCanonicalName());
					cMsg.setIntentFilter(TimesyncRequestMessage.INTENT);
					cMsg.setData(msg.serialize().getBytes());
					HUDConnectivityHelper.getInstance(context).push(cMsg, HUDConnectivityService.Channel.OBJECT_CHANNEL);
				}
			}.start();
		}
	}
}
