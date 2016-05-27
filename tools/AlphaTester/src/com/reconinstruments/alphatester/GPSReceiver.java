package com.reconinstruments.alphatester;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

public class GPSReceiver extends BroadcastReceiver {


	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
        final String action = intent.getAction();

        int iconId, textResId;

//        if (action.equals(LocationManager.GPS_FIX_CHANGE_ACTION)) {
//            // GPS is getting fixes
//        } else if (action.equals(LocationManager.GPS_ENABLED_CHANGE_ACTION)) {
//            // GPS is off
//            iconId = textResId = 0;
//        } else {
//            // GPS is on, but not receiving fixes
//
//        }   
		
	}

}
