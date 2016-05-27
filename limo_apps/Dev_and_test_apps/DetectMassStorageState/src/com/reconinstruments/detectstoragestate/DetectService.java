package com.reconinstruments.detectstoragestate;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;


public class DetectService extends Service {

	private static final String TAG = "DetectService";
	
	BroadcastReceiver mReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			
			Log.e(TAG, "Intent Received");

			if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)){
				Log.v(TAG, Intent.ACTION_MEDIA_UNMOUNTED+"Broadcast Action: External media is present, but not mounted at its mount point." +
						" The path to the mount point for the removed media is contained in the Intent.mData field.");
			}
			else if (intent.getAction().equals(Intent.ACTION_MEDIA_EJECT)){
				Log.v(TAG, Intent.ACTION_MEDIA_EJECT+": User has expressed the desire to remove the external storage media. " +
						"Applications should close all files they have open within the mount point when they receive this intent." +
						" The path to the mount point for the media to be ejected is contained in the Intent.mData field.");
			}
			else if (intent.getAction().equals(Intent.ACTION_MEDIA_REMOVED)){
				Log.v(TAG, Intent.ACTION_MEDIA_REMOVED+"Broadcast Action: External media has been removed." +
						" The path to the mount point for the removed media is contained in the Intent.mData field." );
			}
			else if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTABLE)){
				Log.v(TAG, Intent.ACTION_MEDIA_UNMOUNTABLE+": External media is present but cannot be mounted." +
						" The path to the mount point for the removed media is contained in the Intent.mData field.");
			}
			else if (intent.getAction().equals(Intent.ACTION_MEDIA_SHARED)){
				Log.v(TAG, Intent.ACTION_MEDIA_SHARED+": External media is unmounted because it is being shared via USB mass storage." +
						" The path to the mount point for the shared media is contained in the Intent.mData field.");
			}
			else if (intent.getAction().equals(Intent.ACTION_MEDIA_BAD_REMOVAL)){
				Log.v(TAG, Intent.ACTION_MEDIA_BAD_REMOVAL+": External media was removed from SD card slot, but mount point was not unmounted." +
						" The path to the mount point for the removed media is contained in the Intent.mData field.");
			}
			else if (intent.getAction().equals(Intent.ACTION_MEDIA_CHECKING)){
				Log.v(TAG, Intent.ACTION_MEDIA_CHECKING+": External media is present, and being disk-checked The path to the mount point" +
						" for the checking media is contained in the Intent.mData field.");
			}

		}

	};
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		
		Log.v(TAG,"Service Started");
		
		IntentFilter mFilter = new IntentFilter(Intent.ACTION_MEDIA_UNMOUNTED);
		mFilter.addAction(Intent.ACTION_MEDIA_EJECT);
		mFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
		mFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTABLE);
		mFilter.addAction(Intent.ACTION_MEDIA_SHARED);
		mFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
		mFilter.addAction(Intent.ACTION_MEDIA_CHECKING);
		mFilter.addDataScheme("file");
		
		registerReceiver(mReceiver, mFilter);
		
		return START_STICKY;
	}

	@Override
	public void onDestroy(){
		
		unregisterReceiver(mReceiver);
		
	}
	
}