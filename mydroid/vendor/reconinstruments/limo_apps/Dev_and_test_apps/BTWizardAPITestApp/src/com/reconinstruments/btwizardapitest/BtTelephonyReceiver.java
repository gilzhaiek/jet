package com.reconinstruments.btwizardapitest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class BtTelephonyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
	int hfpstate = intent.getIntExtra("HfpState",0); 
	int mapstate = intent.getIntExtra("MapState",0);
	String result = "hfp state: "+hfpstate+"\n"+"mapstate: "+mapstate;
	Toast.makeText(context, ""+result, Toast.LENGTH_LONG).show();
    }
}