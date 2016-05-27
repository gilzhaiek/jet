//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.phone.service;

import com.reconinstruments.commonwidgets.ReconToast;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

//import com.reconinstruments.modlivemobile.dto.message.PhoneMessage;
import com.reconinstruments.connect.messages.PhoneMessage;


public class PhoneRelayReceiver extends BroadcastReceiver {

    public final static String TAG = "PhoneRelayReceiver";

    public PhoneRelayService mPhoneRelayService;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG,"onReceive");
        if (intent.getAction().equals("HFP_CALL_STATUS_CHANGED")) { // update is coming from gogglen
            Log.v(TAG,"HFP_CALL_STATUS_CHANGED");
            String event = intent.getStringExtra("event");
            if (event.equals("CALL_STARTED")) {
                Log.v(TAG,"received CALL_STARTED");
                mPhoneRelayService.doWhenCallStarted();
            }
            else if (event.equals("CALL_ENDED")) {
                Log.v(TAG,"received CALL_ENDED");
                mPhoneRelayService.doWhenCallEnded();
            }
        }
        else {                  // update is coming from the phone
            PhoneMessage msg = new PhoneMessage(intent.getStringExtra("message"));
            Log.d(TAG,"control: "+msg.isControl()+" type: "+msg.type.name());
            if(!msg.isControl()){
                if(msg.type==PhoneMessage.Status.RINGING){
                    Log.d(TAG,"CallRelayReceiver received incoming call!");
                    // Set the values to mean Android phone.
                    mPhoneRelayService.isiOS = false;
                    mPhoneRelayService.isHfp = true;
                    mPhoneRelayService.gotCall(msg.name, msg.number);
                } else if(msg.type==PhoneMessage.Status.GOTSMS){
                    Log.d("SMS_RECEIVER", "SMS received by receiver");
                    mPhoneRelayService.gotSMS(msg.name, msg.number, msg.body);
                }
                else if(msg.type==PhoneMessage.Status.ENDED){
                    mPhoneRelayService.doWhenCallEnded();
                }
                else if(msg.type==PhoneMessage.Status.STARTED){
                    mPhoneRelayService.doWhenCallStarted();
                }
                else if (msg.type==PhoneMessage.Status.SENTSMS) {
                    if (msg.title.contains("success")) {
                        Log.v(TAG,"Message Successfully sent");
                        new ReconToast(context, com.reconinstruments.commonwidgets.R.drawable.checkbox_icon, "Message Sent!").show();
                    } else {
                        Log.v(TAG,"Message Delivery failed");
                        new ReconToast(context, "Message was NOT Sent!").show();

                    }
                }
            }
            else if (msg.type==PhoneMessage.Status.REFRESH_NEEDED) {
                mPhoneRelayService.doWhenRefreshNeeded();
            }
        }
    }
}

