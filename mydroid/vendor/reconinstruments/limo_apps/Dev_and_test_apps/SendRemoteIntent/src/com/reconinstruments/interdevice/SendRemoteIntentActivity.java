package com.reconinstruments.interdevice;
import android.view.View;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
public class SendRemoteIntentActivity extends Activity {
    public static final String TAG = "SendRemoteIntentActivity";
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
	startService(new Intent("RECON_REMOTE_INTENT_SERVICE"));
	final Button btngetprior = (Button) findViewById(R.id.btn);
        btngetprior.setOnClickListener(new View.OnClickListener() 
	    {
		public void onClick(View v)
		{
		    Intent i = new Intent("lalalala");
		    InterDeviceIntent.sendInterDeviceIntent(getApplicationContext(), i);
		}
	    });

	//Register for the same event in case it comes from the other side
	registerReceiver(bri, new IntentFilter("lalalala"));
    }
    @Override
    public void onResume() {
	super.onResume();
    }

    public BroadcastReceiver bri = new BroadcastReceiver() {
	    public void onReceive(Context c, Intent i ) {
		Log.w(TAG,"YAY");
	    }
	};
}
