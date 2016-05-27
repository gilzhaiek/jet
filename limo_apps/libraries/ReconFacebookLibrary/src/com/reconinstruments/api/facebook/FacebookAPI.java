package com.reconinstruments.api.facebook;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.reconinstruments.api.facebook.FacebookRequestMessage.FacebookRequestBundle;
import com.reconinstruments.api.facebook.FacebookResponseMessage.FacebookResponseBundle;
import com.reconinstruments.modlivemobile.bluetooth.BTCommon;


/**
 * 
 * 
 * @author Patrick Cho
 *
 */
public class FacebookAPI {
	
	private static ResponseReceiver mReceiver;
	// gets the broadcast that has Response Information
	private static Context mContext;
	
	/**
	 * 
	 * @param context
	 * @param wrb
	 * Facebook Request Bundle that includes data
	 * @param listener
	 * There are 2 Callback functions,
	 * 1. one that returns byte array
	 * 2. one that returns string
	 * You can use either one or both 
	 */
	public static void facebookRequest(Context context, FacebookRequestBundle wrb , FacebookResponseListener listener){

		mContext = context;
		
		mReceiver = new ResponseReceiver(listener);
		
		BTCommon.broadcastMessage(mContext, FacebookRequestMessage.compose(wrb));
		
		mContext.registerReceiver(mReceiver, new IntentFilter(wrb.callbackIntent));
		
	}
	
	
	/**
	 * 
	 * Implement this interface and register it as callback function for httpRequest 
	 *
	 */
	public static interface FacebookResponseListener{
		
		public void onComplete(byte[] response);
		
		public void onComplete(String response);
		
	}
	
	
	private static class ResponseReceiver extends BroadcastReceiver{

		FacebookResponseListener listener;
		
		public ResponseReceiver(FacebookResponseListener listener){
			this.listener = listener;
		}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			
			Bundle bundle = intent.getExtras();
			
			FacebookResponseBundle wrb = FacebookResponseMessage.parse(bundle.getString("message"), intent.getAction());
			
			listener.onComplete(FacebookResponseMessage.decodeBase64String(wrb.contentInBase64));
			
			listener.onComplete(FacebookResponseMessage.decodeBase64ByteArray(wrb.contentInBase64));
			
			mContext.unregisterReceiver(this);
				
		}
		
	}
	
	

}
