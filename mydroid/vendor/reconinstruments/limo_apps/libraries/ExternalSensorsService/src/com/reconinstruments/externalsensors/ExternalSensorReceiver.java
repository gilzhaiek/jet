package com.reconinstruments.externalsensors;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

abstract public class ExternalSensorReceiver extends BroadcastReceiver {
    Context mContext;			// owner
    IExternalSensorListener mExternalSensorListener;
    public ExternalSensorReceiver (Context c) {
	mContext = c;
    }
    @Override
    public void onReceive(Context c, Intent i) {
	// Do something
    }
    abstract public void  start();
    public void start(String str) {
	mContext.registerReceiver(this, new IntentFilter(str));
    }
    public void stop() {
	mContext.unregisterReceiver(this);
    }
}
    