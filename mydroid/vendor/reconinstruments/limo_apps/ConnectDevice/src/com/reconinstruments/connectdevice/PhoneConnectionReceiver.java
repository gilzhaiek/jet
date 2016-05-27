package com.reconinstruments.connectdevice;

import java.util.List;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.reconinstruments.connectdevice.ios.MfiReconnectActivity;

//JIRA: MODLIVE-772 Implement bluetooth connection wizard on MODLIVE
public class PhoneConnectionReceiver extends BroadcastReceiver {
	private static final String TAG = "PhoneConnectionReceiver";

	@Override
	public void onReceive(final Context context, Intent intent) {
		Log.d(TAG, "action: " + intent.getAction());
		if (intent.getAction().equals(
				"com.reconinstruments.connectdevice.RECONNECT") && !isActivityRunning(context, ConnectionActivity.class)) {
			new Thread() {
				public void run() {
					Intent reconnectIntent = new Intent(context,
							MfiReconnectActivity.class);
					reconnectIntent.putExtra("from", 0);
					reconnectIntent.putExtra("address",
							PreferencesUtils.getDeviceAddress(context));
					reconnectIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
							| Intent.FLAG_ACTIVITY_SINGLE_TOP);
					context.startActivity(reconnectIntent);
				}
			}.start();
		}
	}
	
	private Boolean isActivityRunning(Context context, Class activityClass)
	{
		Log.d(TAG, "isActivityRunning is called.");
	        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
	        List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(Integer.MAX_VALUE);
	        for (ActivityManager.RunningTaskInfo task : tasks) {
	            if (activityClass.getCanonicalName().equalsIgnoreCase(task.baseActivity.getClassName())){
	            	Log.d(TAG, activityClass + " is running.");
	                return true;
	            }
	        }
	        return false;
	}
}
// End of JIRA: MODLIVE-772