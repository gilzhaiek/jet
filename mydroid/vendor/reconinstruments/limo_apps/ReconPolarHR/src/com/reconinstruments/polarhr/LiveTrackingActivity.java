package com.reconinstruments.polarhr;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.reconinstruments.polarhr.service.*;

public class LiveTrackingActivity extends Activity {
	
	public static final String TAG = "LiveTrackingActivity";
	
	public static final int TOAST_MSG = 1;
	
	/** Messenger for communicating with service. */
	Messenger mService = null;
	/** Flag indicating whether we have called bind on the service. */
	boolean mIsBound;
	
	BroadcastReceiver mHRServiceReceiver;
	
	private TextView heartRateTextView;
	
	/**
	 * Handler of incoming messages from service.
	 */
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			//Log.v(TAG, "Received message");
			switch (msg.what) {
			case PolarHRService.MSG_POLAR_BUNDLE:
				Bundle hrBundle = (Bundle) msg.getData();
				
				if(hrBundle.getInt("ConnectionState") == PolarHRStatus.STATE_CONNECTED
						&& hrBundle.getInt("AvgHR") > 0)
					heartRateTextView.setText(Integer.toString(hrBundle.getInt("AvgHR")));
				break;
				
			default:
				super.handleMessage(msg);
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
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. We are communicating with our
			// service through an IDL interface, so get a client-side
			// representation of that from the raw service object.
			mService = new Messenger(service);
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			mService = null;

			// As part of the sample, tell the user what happened.
			//Toast.makeText(LiveTrackingActivity.this, "Remote Service Disconnected",
			//		Toast.LENGTH_SHORT).show();
		}
	};

	void doBindService() {
		// Establish a connection with the service. We use an explicit
		// class name because there is no reason to be able to let other
		// applications replace our component.
		startService(new Intent("POLAR_HR_SERVICE"));
		//bindService(
		//		new Intent("POLAR_HR_SERVICE"),	
		//		mConnection, Context.BIND_AUTO_CREATE);
		//mIsBound = true;
		// Toast.makeText(this,"Binding.",1000).show();
	}

	void doUnbindService() {
		//if (mIsBound) {
		//	// Detach our existing connection.
		//	unbindService(mConnection);
		//	mIsBound = false;
		//	Toast.makeText(LiveTrackingActivity.this, "Please wait 30 seconds before attempting to reconnect.", 1000).show();
		//}
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.live_tracking_simple_layout);
		
		heartRateTextView = (TextView) findViewById(R.id.bpm);
		
		Typeface tf = Typeface.createFromAsset(this.getResources().getAssets(), "fonts/Eurostib_1.TTF");
		heartRateTextView.setTypeface(tf);
		
		if(((PolarApplication) getApplicationContext()).isPolarMACSet()) {
			doBindService();
		} else {
			Toast.makeText(LiveTrackingActivity.this, "No device attached. Please pair and select a device first.", 1000).show();
			finish();
		}
		
		mHRServiceReceiver = new HeartRateBroadcastReceiver();
		registerReceiver(mHRServiceReceiver, new IntentFilter("POLAR_BROADCAST_HR"));
	}
	
	@Override
	public void onDestroy() {
		unregisterReceiver(mHRServiceReceiver);
		doUnbindService();
		super.onDestroy();
	}

	class HeartRateBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals("POLAR_BROADCAST_HR")) {
				Bundle hrBundle = intent.getExtras().getBundle("POLAR_BUNDLE");
				if(hrBundle.getInt("ConnectionState") == PolarHRStatus.STATE_CONNECTED
						&& hrBundle.getInt("AvgHR") > 0)
					heartRateTextView.setText(Integer.toString(hrBundle.getInt("AvgHR")));
				/*
				//request the latest HR bundle
				try {
					// Give it some value as an example.
					Message msg = Message.obtain(null,
							PolarHRService.MSG_GET_POLAR_BUNDLE, 0, 0);
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
					// In this case the service has crashed before we could even
					// do anything with it; we can count on soon being
					// disconnected (and then reconnected if it can be restarted)
					// so there is no need to do anything here.
				} catch (Exception e) {
					//Log.e("ReconDashboard", e.toString());
				}
				*/
			}
			
		}
		
	}
}
