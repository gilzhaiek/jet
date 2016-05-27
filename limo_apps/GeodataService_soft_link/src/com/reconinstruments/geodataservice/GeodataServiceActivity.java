package com.reconinstruments.geodataservice;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;


public class GeodataServiceActivity extends Activity
{
	private final static String TAG = "GeodataServiceActivity";
	public final static String GEODATASERVICE_START = "com.reconinstruments.geodataservice.start";
	public final static String GEODATASERVICE_BROADCAST_SHUTTDOWN_REQUEST = "com.reconinstruments.geodataservice.shutdown";
	public final static String GEODATASERVICE_BROADCAST_RESTART_REQUEST = "com.reconinstruments.geodataservice.restart";

	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG,"service activity created");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	 
		startService(new Intent(GEODATASERVICE_START));
		
//    	IntentFilter filter = new IntentFilter();
//    	filter.addAction(GEODATASERVICE_BROADCAST_SHUTTDOWN_REQUEST);		
//    	filter.addCategory(Intent.CATEGORY_DEFAULT);
//    	registerReceiver(mGeodataServiceAPIReceiver, filter);
//
//    	filter.addAction(GEODATASERVICE_BROADCAST_RESTART_REQUEST);		
//    	filter.addCategory(Intent.CATEGORY_DEFAULT);
//    	registerReceiver(mGeodataServiceAPIReceiver, filter);
//		Log.i(TAG,"shutdown/restart broadcast listener registered");

    }
 
    protected void onDestroy() {
		Log.i(TAG,"service activity destroyed");
//		mMessageView.setText("Geodata Service stopped...");
//		unregisterReceiver(mGeodataServiceAPIReceiver);
    	super.onDestroy();
    }
 
 //============================================
 // handler for pushed (broadcast) Intents (service API calls) 
//     private BroadcastReceiver mGeodataServiceAPIReceiver = new BroadcastReceiver() {
//     	@Override
//     	public void onReceive(Context context, Intent intent) {
//     		
//     		Log.d(TAG, "Requesting Service Stop:" + intent.getAction().toString());
//    		stopService(new Intent(GEODATASERVICE_START));
//    		
//    		if(intent.getAction().toString().compareTo(GEODATASERVICE_BROADCAST_RESTART_REQUEST) == 0) {
////					Thread.sleep(500);
//         		Log.d(TAG, "Requesting Service Start");
//    			startService(new Intent(GEODATASERVICE_START));
//    		}
//     	}
//     };

}
