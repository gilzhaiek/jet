package com.reconinstruments.webapi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.reconinstruments.modlivemobile.bluetooth.BTCommon;
import com.reconinstruments.webapi.WebRequestMessage.WebRequestBundle;
import com.reconinstruments.webapi.WebResponseMessage.WebResponseBundle;


/**
 * 
 * This class will provides API for making http request
 * With only very Basic Functionalities.
 * 
 * @author Patrick Cho
 *
 */
public class SDKWebService {
	private static String TAG = "SDKWebService";
	
	private static ResponseReceiver mReceiver;
	// gets the broadcast that has Response Information
	// TODO if it returns no response, it won't unregister receiver.
	
	private static Context mContext;

	private static boolean DEBUG = false;
	
	
	/**
	 * 
	 * @param context <em>Context you register</em>
	 * @param debug <em> If you set this to true, then you can generate logcat of messages at DEBUG level</em>
	 * @param timeOutSecs <em> set the timeout for the request. Put 0 for default, which is 10000 ms
	 * @param wrb <em>Web Request Bundle that includes essential data</em>
	 * @param listener
	 * There are 2 Callback functions,<br>
	 * 1. one that returns byte array<br>
	 * 2. one that returns string<br>
	 * <em>You can use either one or both</em>
	 */
	public static void httpRequest(Context context, boolean debug, int timeOutSecs, WebRequestBundle wrb , WebResponseListener listener){

		mContext = context;
		
		DEBUG = debug;
		
		if (timeOutSecs == 0)
			timeOutSecs = 10000;
		
		mReceiver = new ResponseReceiver(listener);
		
		BTCommon.broadcastMessage(mContext, WebRequestMessage.compose(wrb));
		
		mContext.registerReceiver(mReceiver, new IntentFilter(wrb.callbackIntent));
		
		new UnRegisterReceiver(mReceiver, timeOutSecs).start();
		
	}
	
	
	static class UnRegisterReceiver extends Thread{
		
		int timeOutSecs;
		ResponseReceiver myReceiver;
		
		public UnRegisterReceiver(ResponseReceiver receiver, int timeOutSecs){
			this.myReceiver = receiver;
			this.timeOutSecs = timeOutSecs;
		}
		
		@Override
		public void run(){
			try {
				sleep(timeOutSecs);
			} catch (InterruptedException e) {
				Log.e(TAG, e.getLocalizedMessage());
				try {
					mContext.unregisterReceiver(myReceiver);
				} catch (IllegalArgumentException e1){
					Log.d(TAG, "destroying Activity auto unregisters BroadcastReceiver");
				}
			}
			try {
				mContext.unregisterReceiver(myReceiver);
			} catch (IllegalArgumentException e){
				Log.d(TAG, "Checking BroadcastReceiver");
			}
			
		}
	}
		
	
	
	/**
	 * 
	 * Implement this interface and register it as callback function for httpRequest 
	 *
	 */
	public static interface WebResponseListener{
		
		public void onComplete(byte[] response , String statusCode, String statusId, String requestId );
		
		public void onComplete(String response , String statusCode, String statusId, String requestId );
		
	}
	
	
	private static class ResponseReceiver extends BroadcastReceiver{

		WebResponseListener listener;
		
		public ResponseReceiver(WebResponseListener listener){
			this.listener = listener;
		}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			
			Bundle bundle = intent.getExtras();
			
			WebResponseBundle wrb = WebResponseMessage.parse(bundle.getString("message"), intent.getAction());
			
			if (DEBUG) Log.d(TAG, intent.getAction() + " statusCode = " + wrb.statusCode + ", statusLine = " + wrb.statusLine + ", requestId = "+ wrb.requestId +
					" : content = " + WebAPIUtils.decodeBase64String(wrb.contentInBase64));	
			
			listener.onComplete(
					WebAPIUtils.decodeBase64String(wrb.contentInBase64),
					wrb.statusCode, wrb.statusLine , wrb.requestId
					);
			
			listener.onComplete(
					WebAPIUtils.decodeBase64ByteArray(wrb.contentInBase64),
					wrb.statusCode, wrb.statusLine , wrb.requestId
					);
			
			try{
				context.unregisterReceiver(this);
			}catch(IllegalArgumentException e){
				Log.d(TAG, "Checking BroadcastReceiver");
			}
			
				
		}
		
	}
	
	

}
