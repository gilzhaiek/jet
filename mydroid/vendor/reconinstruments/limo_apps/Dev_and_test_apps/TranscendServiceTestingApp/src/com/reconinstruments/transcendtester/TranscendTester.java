package com.reconinstruments.transcendtester;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Button;
import android.view.View;

public class TranscendTester extends Activity {
    public static final int MSG_START_SPORTS_ACTIVITY = 20;
    public static final int MSG_PAUSE_SPORTS_ACTIVITY = 21;
    public static final int MSG_RESTART_SPORTS_ACTIVITY = 22;
    public static final int MSG_STOP_SPORTS_ACTIVITY = 23;
    Messenger mService = null;
    boolean mIsBound;
    class IncomingHandler extends Handler {
	@Override
	public void handleMessage(Message msg) {
	    switch (msg.what) {
	    case 1:		// ReconTranscendService.MSG_RESULT
		break;
	    default:
		super.handleMessage(msg);
	    }
	}
    }
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
		mService = new Messenger(service);
	    }
	    public void onServiceDisconnected(ComponentName className) {
		mService = null;
	    }
	};
    void doBindService() {
	bindService(new Intent("RECON_MOD_SERVICE"),
		    mConnection, Context.BIND_AUTO_CREATE);
	mIsBound = true;
    }
    void doUnbindService() {
	if (mIsBound) {
	    unbindService(mConnection);
	    mIsBound = false;
	}
    }
    private void sendCommandToTranscendService(int i) {
	try {
	    Message msg = Message.obtain(null,i, 0, 0);
	    msg.replyTo = mMessenger;
	    mService.send(msg);
	} catch (RemoteException e) {
	} catch (Exception e) {
	}
    }
    private void startSportsActivity() {
	sendCommandToTranscendService(MSG_START_SPORTS_ACTIVITY);
    }
    private void pauseSportsActivity() {
	sendCommandToTranscendService(MSG_PAUSE_SPORTS_ACTIVITY);
    }
    private void resumeSportsActivity() {
	sendCommandToTranscendService(MSG_RESTART_SPORTS_ACTIVITY);
    }
    private void stopSportsActivity() {
	sendCommandToTranscendService(MSG_STOP_SPORTS_ACTIVITY);
    }
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
	final Button startActivity_b = (Button) findViewById(R.id.start_activity);
	startActivity_b.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    startSportsActivity();
		}
	    });
	final Button pauseActivity_b = (Button) findViewById(R.id.pause_activity);
	pauseActivity_b.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    pauseSportsActivity();
		}
	    });
	final Button resumeActivity_b = (Button) findViewById(R.id.resume_activity);
	resumeActivity_b.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    resumeSportsActivity();
		}
	    });
	final Button finishActivity_b = (Button) findViewById(R.id.finish_activity);
	finishActivity_b.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    stopSportsActivity();
		}
	    });
    }
    public void onResume() {
	super.onResume();
	doBindService();
    }
    public void onPause() {
	doUnbindService();
	super.onPause();
    }
}
