package com.reconinstruments.phone.service;

//import android.bluetooth.BluetoothHandsfreeUnit;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

class IncomingCallReceiver extends BroadcastReceiver {

	private static final String TAG = "IncomingCallReceiver";
	public PhoneRelayService mTheService;
	public IncomingCallReceiver(PhoneRelayService theService) {
		mTheService = theService;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v(TAG,"onReceive");
		if (mTheService.alreadyInACall) {
			Log.v(TAG,"already in the call");
			return;
		}
		else {mTheService.alreadyInACall = true;}

		Log.d(TAG, "Received " + intent.getAction());
		String callerId = "";
		mTheService.isiOS = false;
		mTheService.isHfp = true;
		callerId = intent.getStringExtra("EXTRA_CALLER_ID");
		Log.d(TAG, "Number: " + callerId);

		// ALI FIXME put me back TODO:
		mTheService.mActiveCallerId = callerId;
		Log.v(TAG,"Force iPhone resolution");
		mTheService.requestCallerNameResolutionFromiPhone(callerId);



		// // // HACK
		// // mTheService.mActiveCallerName = callerId; // FIXME

		// if (!callerId.equals(mTheService.mActiveCallerId)) {
		//     Log.v(TAG,"Resolving the name");
		//     mTheService.mActiveCallerId = callerId;
		// } else {
		//     if (mTheService == null) {
		// 	Log.w(TAG,"service is null");
		// 	return;
		//     } 
		//     // // If call from same callerId as last one - use cached data
		//     String contact = mTheService.mActiveCallerName != null ? mTheService.mActiveCallerName : mTheService.mActiveCallerId.length() > 0 ? mTheService.mActiveCallerId : "Unknown";
		//     mTheService.mEvents.add(0, String.format("Incoming call from %s", contact));
		//     if (mTheService.mActiveCallerId == null) {
		// 	mTheService.mActiveCallerId = "unknown";

		//     }
		//     mTheService.gotCall(contact, mTheService.mActiveCallerId);
		// }
	}

}