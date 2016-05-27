package com.reconinstruments.systempropertysetter;
import android.os.SystemProperties;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
public class SystemPropSetterReceiver extends BroadcastReceiver {
    public static final String SET_PROP = "com.reconinstruments.systempropertysetter.SET_PROP";
    public static final String GET_PROP = "com.reconinstruments.systempropertysetter.GET_PROP";
    @Override
    public void onReceive(Context context, Intent intent) {
	String action = intent.getAction();
	if (action.equals(SET_PROP)) {
	    String prop = intent.getStringExtra("prop");
	    String value = intent.getStringExtra("value");
	    Log.v("Systempropertysetter","prop "+prop+" value "+value);
	    SystemProperties.set(prop,value);
	}
    }
}
