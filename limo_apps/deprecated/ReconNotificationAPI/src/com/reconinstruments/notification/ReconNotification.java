package com.reconinstruments.notification;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

public class ReconNotification {

	/**
     * Send a notification to Recon's Livefeed
     * @param title 				the notification title
     * @param message 				the notification message
     * @param id 					a unique id that refers to this message instance
     * @param persistant 			should it be dismissed if clicked on
     * @param icon 					the Bitmap that should be displayed with this notification
     * @param intent 				the intent string to call when clicked
     * @param statusBarDisplay 		should a status bar notification also be created
     * @param intentExtras 			extras to be included in your intent
     */
	public static void postNotification(Context context, String title, String message, int id, boolean persistant, boolean statusBarDisplay, 
			Bitmap icon, String intent, Bundle intentExtras)
	{
		
    	Bundle notifBundle = new Bundle();
    	notifBundle.putString("title", title);
    	notifBundle.putString("message", message);
    	notifBundle.putParcelable("icon", icon);
    	notifBundle.putString("intentString", intent);
    	notifBundle.putBundle("extrasBundle", intentExtras);
    	notifBundle.putBoolean("displayInStatusBar", statusBarDisplay);
    	
    	Intent i = new Intent("RECON_POST_NOTIFICATION");
    	i.putExtra("NotificationID", id);
    	i.putExtra("NotificationBundle", notifBundle);
    	i.putExtra("Persistant", persistant);
    	
    	context.sendBroadcast(i);
    }
    
	/**
	 * Remove a notification from Recon's system
	 * @param context
	 * @param notificationID 	the id of the notification to remove 
	 */
    public static void dismissNotification(Context context, int notificationID) {
    	Intent i = new Intent("RECON_DISMISS_NOTIFICATION");
    	i.putExtra("NotificationID", notificationID);
    	
    	context.sendBroadcast(i);
    }
}
