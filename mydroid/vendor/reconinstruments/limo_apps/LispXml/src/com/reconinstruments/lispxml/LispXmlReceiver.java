package com.reconinstruments.lispxml;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
public class LispXmlReceiver extends BroadcastReceiver {
    public static final String TAG = "LispXmlReceiver";
    public static final String sBT_RETURN = "com.reconinstruments.lispxml.BluetoothReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
	String action = intent.getAction();
	if (action.equals("android.intent.action.ACTION_POWER_CONNECTED")) {
	    // start the service
	    context.startService(new Intent (LispXmlService.LONG_TERM_ACTION));
	    Log.v(TAG,"connected");
	}
	else if (action.equals("android.intent.action.ACTION_POWER_DISCONNECTED")){
	    // stop the service
	    context.stopService(new Intent (LispXmlService.LONG_TERM_ACTION));
	    Log.v(TAG,"disconnected");
	}
	else if (action.equals(sBT_RETURN)) {
	    Intent si = new Intent(LispXmlService.BT_ACTION);
	    si.putExtras(intent);
	    context.startService(si);
	    Log.v(TAG,"bt lispxml");
	}
    }
}
