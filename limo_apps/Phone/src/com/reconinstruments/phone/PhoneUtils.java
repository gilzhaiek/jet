package com.reconinstruments.phone;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.reconinstruments.connect.apps.ConnectHelper;
import com.reconinstruments.connect.messages.PhoneMessage;
import com.reconinstruments.phone.dialogs.CallProgressActivity;
import com.reconinstruments.phone.dialogs.IncomingCallActivity;

public class PhoneUtils {

	private static final String TAG = "PhoneUtils";

	public static void startCall(String source,String contact,Activity activity){

		PhoneMessage msg = new PhoneMessage(PhoneMessage.Control.START,source);

		Intent myi = new Intent();
		myi.setAction(ConnectHelper.GEN_MSG);
		myi.putExtra("message", msg.toXML());
		activity.sendBroadcast(myi);

		Intent showCall = new Intent(activity,CallProgressActivity.class).
				putExtra("calling", true).
				putExtra("source", source).
				putExtra("contact", contact);

		activity.startActivity(showCall);
	}

	public static void answerCall(Context context,boolean isiOS,boolean isHfp,String source,String contact){

		if (isiOS) {// Answer in iPhone mode

			Log.v(TAG,"ios ble mode");
			//Send the bluetoothheadset command to pickup
			Intent i = new Intent("RECON_IOS_BLUETOOTH_HEADSET_COMMAND");
			i.putExtra("command", 1);//1 = answer call: FIXME use static names and etc.
			context.sendBroadcast(i); 
		}
		else {
			Log.v(TAG,"none BLE mode");
			// Tell phone to answer call
			PhoneMessage msg = new PhoneMessage(PhoneMessage.Control.ANSWER);

			// TODO
			if (!isHfp) {	// What it really means is Mfi
				Log.v(TAG,"non Hfp mode");
				Intent i = new Intent();
				i.setAction(ConnectHelper.GEN_MSG);
				i.putExtra("message", msg.toXML());
				context.sendBroadcast(i);
			}
			else {		// uinsg Hfp or Mfi
				Log.v(TAG,"Hfp mode");
				Intent i = new Intent();
				i.setAction("RECON_SS1_HFP_COMMAND"); // FIXME use static var
				i.putExtra("command", 1); // means answer
				context.sendBroadcast(i);
			}
		}
		// show the call progress activity in 'answering..' mode
		Intent showCall = new Intent(context,CallProgressActivity.class).
				putExtra("calling", false).
				putExtra("source", source).
				putExtra("contact", contact).
				putExtra("isiOS", isiOS).
				putExtra("isHfp", isHfp);

		context.startActivity(showCall);
	}
	
	public static void endCall(Context context,boolean isiOS,boolean isHfp, boolean reject){
		if (isiOS) {
			Intent i = new Intent("RECON_IOS_BLUETOOTH_HEADSET_COMMAND");
			i.putExtra("command", 2);//2 = hangup: FIXME use static names and etc
			context.sendBroadcast(i);
			Toast.makeText(context,"You need to hang up via iPhone",
					Toast.LENGTH_LONG).show();
		}
		else if (isHfp) {
			// Send the command locally
			Intent i = new Intent("RECON_SS1_HFP_COMMAND");
			i.putExtra("command", 2);//2 = hangup: FIXME use static names and etc
			context.sendBroadcast(i);
		}
		else { //android only
			PhoneMessage msg = reject?new PhoneMessage(PhoneMessage.Control.REJECT):
									  new PhoneMessage(PhoneMessage.Control.END);
			context.sendBroadcast(new Intent(ConnectHelper.GEN_MSG).putExtra("message",msg.toXML()));
		}
	}
}
