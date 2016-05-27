package com.reconinstruments.dashlauncher.notifications;

import java.util.Random;

import com.reconinstruments.dashnotification.NotificationsActivity;
import com.reconinstruments.dashnotification.R;
import com.reconinstruments.modlivemobile.dto.message.PhoneMessage;
import com.reconinstruments.modlivemobile.dto.message.XMLMessage;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.util.Log;

public class NotificationsService extends Service {

	private static final String TAG = "NotificationsService";
	
	public static final String REGISTER_NOTIFICATION = "RECON_POST_NOTIFICATION";
	public static final String DISMISS_NOTIFICATION = "RECON_DISMISS_NOTIFICATION";
	public static final String TEST_NOTIFICATION = "RECON_TEST_NOTIFICATION";
	
	private ContentResolver cr;
	private boolean isMetric = true;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(TAG, "onStartCommand() called");

		cr = getContentResolver();

		/*
		 * Intent actions for broadcast receivers registration
		 */
		IntentFilter notificationsIntentFilter = new IntentFilter(REGISTER_NOTIFICATION);
		notificationsIntentFilter.addAction(DISMISS_NOTIFICATION);
		notificationsIntentFilter.addAction(TEST_NOTIFICATION);
		notificationsIntentFilter.addAction("RECON_MOD_BROADCAST_TIME");

		registerReceiver(notificationsBroadcastReceiver, notificationsIntentFilter);
		Log.v(TAG, "registered transcendInfoReceiver");

		return START_STICKY;
	}
	
	BroadcastReceiver notificationsBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			isMetric = ReconSettingsUtil.getUnits(NotificationsService.this) == ReconSettingsUtil.RECON_UINTS_METRIC;
			
			// BROADCAST TO REGISTER A NOTIFICATION
			if(intent.getAction().equals(REGISTER_NOTIFICATION)) {
				Bundle bundle = intent.getExtras();
				Bundle notificationBundle = bundle.getBundle("NotificationBundle");
				Integer notif_id = bundle.getInt("NotificationID");
				boolean notif_persistant = bundle.getBoolean("Persistant");
				
				//Log.v(TAG, "Register Notification: " + notif_id);
				
				Parcel p = Parcel.obtain();
				notificationBundle.writeToParcel(p, 0);
				
				// Insert row
				ContentValues values = new ContentValues();

				values.put(NotificationsDatabase.KEY_DATA, p.marshall());
				values.put(NotificationsDatabase.KEY_DATE, System.currentTimeMillis());
				values.put(NotificationsDatabase.KEY_NOTIFICATION_ID, notif_id);
				values.put(NotificationsDatabase.KEY_SINGLETON_PERSISTANT, notif_persistant ? 1 : 0);
				
				cr.insert(NotificationsProvider.NOTIFICATION_URI, values);
				
				// Send notification to status bar
				boolean statusBarNotification =  notificationBundle.getBoolean("displayInStatusBar", true);
				if(statusBarNotification) {
					NotificationManager a = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					Notification n = new Notification(R.drawable.notification_icon_air, 
							notificationBundle.getString("title") + " - " + 
									NotificationsActivity.parseNotificationMessage(notificationBundle.getString("message"), isMetric), 
							System.currentTimeMillis());
					PendingIntent pi = PendingIntent.getBroadcast(NotificationsService.this, 0, new Intent(), 0);
					n.setLatestEventInfo(NotificationsService.this, notificationBundle.getString("title"), notificationBundle.getString("title"), pi);
					n.flags = Notification.FLAG_ONGOING_EVENT;
					a.notify(654654, n);
					a.cancel(654654);
				}
			}
			
			// BROADCAST TO DISMISS A NOTIFICATION
			else if(intent.getAction().equals(DISMISS_NOTIFICATION)) {
				Bundle extras = intent.getExtras();
				Integer notif_id = extras.getInt("NotificationID");
				
				Log.v(TAG, "Dismiss Notification: " + notif_id);
				
				cr.delete(NotificationsProvider.NOTIFICATION_URI, NotificationsDatabase.KEY_NOTIFICATION_ID+"="+notif_id, null);
			}
			
			// NEW DAY, DISMISS ALL NOTIFICATIONS
			else if(intent.getAction().equals("RECON_MOD_BROADCAST_TIME")) {
				Log.v(TAG, "New day, deleting all notifications");
				
				cr.delete(NotificationsProvider.NOTIFICATION_URI, null, null);
			}
			
			else if(intent.getAction().equals(TEST_NOTIFICATION)) {
				Log.v(TAG, TEST_NOTIFICATION);
				
				Random r = new Random(System.currentTimeMillis());
				int id = r.nextInt();
				
				NotificationManager a = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				Notification n = new Notification(R.drawable.notification_icon_air,	"Test - " + id, System.currentTimeMillis());
				PendingIntent pi = PendingIntent.getBroadcast(NotificationsService.this, 0, new Intent(), 0);
				n.setLatestEventInfo(NotificationsService.this, "Test - " + id, "Test - " + id, pi);
				n.flags = Notification.FLAG_ONGOING_EVENT;
				a.notify(id, n);
				//a.cancel(id);
			}
		}
		
	};
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	

}

