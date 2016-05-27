package com.reconinstruments.modliveliveserver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ToggleButton;

public class ModLiveLiveServerActivity extends Activity {
	
	private final static String TAG = "ModLiveLiveServerActivity";
	
	private boolean mIsBound = false;
    private ToggleButton button;    
    private ScreenshotCaptureService mBoundService;
    private CaptureServiceReceiver mReceiver;
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mReceiver = new CaptureServiceReceiver();
        registerReceiver(mReceiver, new IntentFilter("MOD_LIVE_LIVE_RUNNING"));
        registerReceiver(mReceiver, new IntentFilter("MOD_LIVE_LIVE_STOPPED"));
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	Log.d(TAG, "startService");
    	startService(new Intent("MOD_LIVE_LIVE_SERVICE"));
    	doBindService();
    	
    	button = (ToggleButton) findViewById(R.id.serviceToggle);
        
        button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(button.isChecked()) {
					// Start service
					mBoundService.startRecording();
				} else {
					// Stop service
					mBoundService.stopRecording();
				}
			}
        	
        });
        
        Intent myi = new Intent();
		myi.setAction("MOD_LIVE_LIVE_IS_RUNNING");
		Context c = getApplicationContext();
		c.sendBroadcast(myi);
    }

    @Override
    public void onPause() {
    	doUnbindService();
    	super.onPause();
    }
    
    @Override
    public void onDestroy() {
    	unregisterReceiver(mReceiver);
    	super.onDestroy();
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((ScreenshotCaptureService.LocalBinder)service).getService();
            //button.setChecked(mBoundService.isRunning());
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
        }
    };
    
    
    public void doBindService() {
    	Log.d(TAG, "doBindService");
    	bindService(new Intent(ModLiveLiveServerActivity.this, 
                ScreenshotCaptureService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }
    
    public void doUnbindService() {
    	Log.d(TAG, "doUnbindService");
    	if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }
    
    class CaptureServiceReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(action.equals("MOD_LIVE_LIVE_RUNNING")) {
				button.setChecked(true);
			}
			
			else if(action.equals("MOD_LIVE_LIVE_STOPPED")) {
				button.setChecked(false);
			}
		}
    	
    }
}