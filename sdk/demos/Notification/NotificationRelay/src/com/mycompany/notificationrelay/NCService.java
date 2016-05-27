package com.mycompany.notificationrelay;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityService;

public class NCService extends NotificationListenerService {
	private String TAG = this.getClass().getSimpleName();

	private Handler handler;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handler = new Handler();
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onNotificationPosted(StatusBarNotification sbn) {
		if(sbn.getNotification().tickerText == null)
			return;

		String tickerText = (String)sbn.getNotification().tickerText;

		Log.i(TAG,"onNotificationPosted from Package '" + sbn.getPackageName() + "'");
		Log.i(TAG,"ID:" + sbn.getId() + " TEXT:'" + tickerText + "'");

		Intent intent = new Intent();
		intent.setAction("com.mycompany.newsbn");
		intent.putExtra("tickerText",tickerText);

		BitmapDrawable icon = null;
		try {
			icon = (BitmapDrawable)getPackageManager().getApplicationIcon(sbn.getPackageName());
			intent.putExtra("icon", icon.getBitmap());
			intent.putExtra("hasIcon", true);
		} catch (NameNotFoundException e) {
			intent.putExtra("hasIcon", false);
			icon = null;
			e.printStackTrace();
		}

		sendNotificationToHUDViaEngage(tickerText, icon);
		sendBroadcast(intent);
	} 

	protected void sendNotificationToHUDViaEngage(String notifcationText, BitmapDrawable icon) {
		Intent intent = new Intent(HUDConnectivityService.INTENT_OBJECT); // Receiver on Engage (Fixed)
		HUDConnectivityMessage cMsg = new HUDConnectivityMessage();
		
		cMsg.setSender(this.getClass().getCanonicalName()); // Sender (me)
		cMsg.setIntentFilter("com.mycompany.newsbn"); // Receiver on the HUD
		cMsg.setData(notifcationText.getBytes()); // Extra Data to the HUD app
		intent.putExtra(HUDConnectivityMessage.TAG, cMsg.toByteArray());
		sendBroadcast(intent);
	}

	@Override
	public void onNotificationRemoved(StatusBarNotification sbn) {
	}
}
