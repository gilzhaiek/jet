package com.reconinstruments.connectdevice.ios;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import com.reconinstruments.commonwidgets.ReconToast;

public class BtTelephonyReceiver extends BroadcastReceiver {
	private static final String TAG = "BtTelephonyReceiver";
	private static int priorState = 0;

	@Override
	public void onReceive(Context context, Intent intent) {
		// state 2 connected, 1 connecting, 0 disconnected
		int hfpstate = intent.getIntExtra("HfpState", 0);
		int mapstate = intent.getIntExtra("MapState", 0);
		String result = "hfp state: " + hfpstate + "\n" + "mapstate: "
				+ mapstate;
    	(new ReconToast(context, com.reconinstruments.commonwidgets.R.drawable.checkbox_icon, "" + result)).show();
		if (mapstate == 2 && priorState == 1) {
			Log.d(TAG, "Success to disconnect and connect to the phone, done.");
		} else if (mapstate == 0 && priorState == 1) {
			Log.d(TAG,
					"Fail to disconnect and connect to the phone, ask the user try again.");
		}
		priorState = mapstate;
	}
}