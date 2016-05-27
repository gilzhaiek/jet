package com.reconinstruments.interdevicetest;
import com.reconinstruments.interdevice.InterDeviceIntent;
import android.view.View;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

public class InterDeviceTestActivity extends Activity
{
    public static final String TAG = "InterDeviceTestActivity";
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

	//Enable system-wide RECEIVING of intents sent by the remote
	//device. In the future this line will be called by the system
	//and hence always be enabled. So in the future the app
	//developer may not need to explicitly call this. This line
	//can safely be called several times by different apps.
	startService(new Intent("RECON_REMOTE_INTENT_SERVICE"));

	final Button btngetprior = (Button) findViewById(R.id.btn);
        btngetprior.setOnClickListener(new View.OnClickListener() 
	    {
		public void onClick(View v)
		{
		    Intent i = new Intent("lalalala");
		    i.putExtra("ExtraStuff","Extra Stuff");
		    //Special Recon API to send the remote intent. A
		    //regular broadcast receiver will receive this
		    //intent provided that
		    //"RECON_REMOTE_INTENT_SERVICE" is started on the
		    //receiving end
		    InterDeviceIntent.sendInterDeviceIntent(getApplicationContext(), i);
		}
	    });
    }

    @Override
    public void onResume() {
	super.onResume();
	//Register for the same event in case it comes from the other side
	registerReceiver(bri, new IntentFilter("lalalala"));
    }
    @Override
    public void onPause() {
	unregisterReceiver(bri);
	super.onPause();
    }

    public BroadcastReceiver bri = new BroadcastReceiver() {
	    public void onReceive(Context c, Intent i ) {
		Log.d(TAG,"Received remote intent lalalala");
		Toast.makeText(c, "Received remote intent lalalala", 1000).show();
		Toast.makeText(c, "Extra Stuff is" + i.getStringExtra("ExtraStuff"), 1000).show();
	    }
	};
}
