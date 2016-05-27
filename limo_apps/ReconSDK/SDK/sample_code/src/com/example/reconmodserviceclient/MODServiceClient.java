/**
   This sample app binds to MODService and retrieves the FullInfoBudle
   (for description see the documents provided) Extracts some altitude
   and some vertical information fields

*/
package com.example.reconmodserviceclient;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import android.view.View.OnClickListener;
import android.view.View;
import com.reconinstruments.modservice.ReconMODServiceMessage;

public class MODServiceClient extends Activity
{
    public static final String TAG = "MODServiceClient";
    private Bundle mFullInfoBundle;
    private TextView mAltField;
    private TextView mVrtField;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
	doBindService();
	Button button = (Button) findViewById(R.id.update_info_button);
	mAltField = (TextView)findViewById(R.id.alt_field);
	mVrtField = (TextView)findViewById(R.id.vrt_field);
	
	button.setOnClickListener(new OnClickListener(){
		public void onClick (View v) {
		    Log.d(TAG,"Button pressed");
		    requestUpdateFullInfo();
		}
	    });
	

    }

    /**
     * This part pertains to stuff needed to connect to MOD service
     */
    /** Messenger for communicating with service. */
    Messenger mService = null;
    /** Flag indicating whether we have called bind on the service. */
    boolean mIsBound;

    /**
     * Handler of incoming messages from service.
     */

    class IncomingHandler extends Handler {
	@Override
	public void handleMessage(Message msg) {
	    Log.d(TAG,"Received message");
	    switch (msg.what) {
	    case ReconMODServiceMessage.MSG_RESULT: 
		mFullInfoBundle = (Bundle) msg.getData();
		updateGui();
		break;
	    }
	}
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
		Log.d(TAG,"Service connected");
		mService = new Messenger(service);
	    }

	    public void onServiceDisconnected(ComponentName className) {
		// This is called when the connection with the service has been
		// unexpectedly disconnected -- that is, its process crashed.
		mService = null;
	    }
	};

    void doBindService() {
	Log.d(TAG,"Trying to bind to service");
	bindService(
		    new Intent("RECON_MOD_SERVICE"),				
		    mConnection, Context.BIND_AUTO_CREATE);
	mIsBound = true;
    }

    void doUnbindService() {
	if (mIsBound) {
	    // Detach our existing connection.
	    unbindService(mConnection);
	    mIsBound = false;

	}
    }

    public void requestUpdateFullInfo() {
	try {

	    Log.d(TAG,"request update full_info");
	    // Give it some value as an example.
	    Message msg = Message.obtain(null,
					 ReconMODServiceMessage.MSG_GET_FULL_INFO_BUNDLE, 0, 0);
	    msg.replyTo = mMessenger;
	    mService.send(msg);
	} catch (RemoteException e) {
	    // In this case the service has crashed before we could even
	    // do anything with it; we can count on soon being
	    // disconnected (and then reconnected if it can be restarted)
	    // so there is no need to do anything here.
	} catch (Exception e) {
	    Log.e("MODServiceClient", e.toString());
	}
    }

    // This function is just some sampel gui update:
     public void updateGui() {
	 if (mFullInfoBundle != null){
	     // Altitude information:
	     Bundle AltBundle = mFullInfoBundle.getBundle("ALTITUDE_BUNDLE");
	     // update the text field
	     mAltField.setText("Current Alt: "+AltBundle.getFloat("Alt")+
			      " Max: "+AltBundle.getFloat("MaxAlt"));

	     // Vertical information:
	     Bundle VertBundle = mFullInfoBundle.getBundle("VERTICAL_BUNDLE");
	     mVrtField.setText("Current Vert: "+VertBundle.getFloat("Vert"));
	 }
     }

    @Override
    public void onPause(){
	doUnbindService();
	super.onPause();
    }

    @Override
    public void onResume(){
	super.onResume();
	doBindService();
    }
}
