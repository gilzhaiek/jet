package com.reconinstruments.systemui.uplink;

import com.reconinstruments.systemui.R;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

public class UplinkActivity extends Activity {
    BroadcastReceiver mTheReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.uplink);
	mTheReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
		    finish();
		}
	    };
	IntentFilter infil =
	    new IntentFilter("com.reconinstruments.systemui.uplink.UPLINK_DONE");
	infil.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
        registerReceiver(mTheReceiver,infil );
    }
    public void onDestroy() {
	unregisterReceiver(mTheReceiver);
	super.onDestroy();
    }
    @Override
    public void onBackPressed() {
	return;
    }
}
