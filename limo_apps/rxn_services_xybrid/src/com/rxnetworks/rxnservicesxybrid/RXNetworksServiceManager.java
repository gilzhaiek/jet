package com.rxnetworks.rxnservicesxybrid;

import com.rxnetworks.rxnserviceslib.RXNetworksService;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.util.Log;

public class RXNetworksServiceManager extends BroadcastReceiver {

	public static final String TAG = "RXNetworksServiceManager";

	@Override
	public void onReceive(Context context, Intent intent) 
	{
		  // just make sure we are getting the right intent (better safe than sorry)
		  if( "android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) 
		  {
			  ComponentName comp = new ComponentName(context.getPackageName(), RXNetworksService.class.getName());
			  ComponentName service = context.startService(new Intent().setComponent(comp));
			  
			  if (null == service)
			  {
				  // something really wrong here
				  Log.e(TAG, "Could not start service " + comp.toString());
			  }
		  } 
		  else 
		  {
			  Log.e(TAG, "Received unexpected intent " + intent.toString());   
		  }
	}

}
