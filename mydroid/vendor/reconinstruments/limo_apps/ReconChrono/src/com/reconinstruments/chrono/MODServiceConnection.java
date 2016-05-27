package com.reconinstruments.chrono;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.reconinstruments.modservice.*;

public class MODServiceConnection implements ServiceConnection {

	private static MODServiceConnection mInstance = null;
	
	// connection status
	static final int CONNECTION_STATUS_NOTSTARTED = 0;
	static final int CONNECTION_STATUS_BINDING = 1;
	static final int CONNECTION_STATUS_BOUND = 2;
	public static final boolean DEBUG_TOAST = false;

	private Context mContext;
	private int mConnectionStatus = CONNECTION_STATUS_NOTSTARTED;

	// the messenger for handling message coming from MODService
	private Messenger mIncomingMessenger = null;
	// the messenger for sending query message to MODService
	private Messenger mServiceMessenger = null;
	
	private ArrayList<Messenger> messengerForwards = null;

	private Timer mQueryTimer = null;

	public static MODServiceConnection getInstance(Context context) {
		if(mInstance == null)
			mInstance = new MODServiceConnection(context);
		return mInstance;
	}
	
	private MODServiceConnection(Context context) {
		mContext = context;
		mIncomingMessenger = new Messenger(new MODServiceMessageHandler());
		messengerForwards = new ArrayList<Messenger>(2);
	}

	@Override
	public void onServiceConnected(ComponentName arg0, IBinder binder) {
		mConnectionStatus = CONNECTION_STATUS_BOUND;

		// create the messenger to sending message to MODService
		// Note that we using the binder coming from the MODService for
		// creating this messenger
		mServiceMessenger = new Messenger(binder);

		// schedule a repetitive task for querying information
		// at the interval of 1 seconds, start 0.1 seconds later on
		//mQueryTimer.schedule(new QueryTask(), 100, 1000);
		restartTimer();
	}

	@Override
	public void onServiceDisconnected(ComponentName arg0) {
		mConnectionStatus = CONNECTION_STATUS_NOTSTARTED;
		mServiceMessenger = null;

		// cancel the timer if it is set
		if (mQueryTimer != null) {
			mQueryTimer.cancel();
			mQueryTimer = null;
		}
	}
	
	public void doBindService() {
		mContext.bindService( new Intent( "RECON_MOD_SERVICE" ), this, Context.BIND_AUTO_CREATE );
	}

	public void doUnBindService() {
		if (mConnectionStatus == CONNECTION_STATUS_BOUND) {
			mContext.unbindService(this);
			mConnectionStatus = CONNECTION_STATUS_NOTSTARTED;

			// cancel the timer if it is set
			if (mQueryTimer != null) {
				mQueryTimer.cancel();
				mQueryTimer = null;
			}
		}
	}
	
	public void requestUpdate() {
		try {
			Message msg = Message.obtain();
			msg.what = ReconMODServiceMessage.MSG_GET_CHRONO_BUNDLE;
			msg.replyTo = mIncomingMessenger;
			mServiceMessenger.send(msg);
		} catch (Exception e) {
			
		}
	}
	
	public void issueLapTrialCommand() {
		try {
			Message forwardMsg = Message.obtain(null, ReconMODServiceMessage.MSG_CHRONO_LAP_TRIAL, 0, 0);
			forwardMsg.replyTo = mIncomingMessenger;
			mServiceMessenger.send(forwardMsg);
			restartTimer();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void issueStartStopCommand() {
		try {
			Message forwardMsg = Message.obtain(null, ReconMODServiceMessage.MSG_CHRONO_START_STOP, 0, 0);
			forwardMsg.replyTo = mIncomingMessenger;
			mServiceMessenger.send(forwardMsg);
			restartTimer();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void addReceiver(Messenger messenger) {
		messengerForwards.add(messenger);
	}
	
	private void restartTimer() {
		if(mQueryTimer != null) {
			mQueryTimer.cancel();
		}
		mQueryTimer = new Timer();
		mQueryTimer.schedule(new QueryTask(), 100, 1000);
	}

	class MODServiceMessageHandler extends Handler {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ReconMODServiceMessage.MSG_RESULT:
				if (msg.arg1 == ReconMODServiceMessage.MSG_GET_CHRONO_BUNDLE) {
					Bundle data = msg.getData();

					// Do something with result
					for(Messenger m : messengerForwards) {
						try {
							Message message = Message.obtain();
							message.what = ReconMODServiceMessage.MSG_RESULT;
							message.arg1 = ReconMODServiceMessage.MSG_GET_CHRONO_BUNDLE;
							message.setData(data);
							m.send(message);
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				break;

			default:
				super.handleMessage(msg);
			}
		}
	}

	/**
	 * The periodic running task for sending message to MODService
	 * querying for information
	 */
	private class QueryTask extends TimerTask {

		@Override
		public void run() {
			Message msg = Message.obtain();
			msg.what = ReconMODServiceMessage.MSG_GET_CHRONO_BUNDLE;
			msg.replyTo = mIncomingMessenger;

			try {
				mServiceMessenger.send(msg);
			} catch (RemoteException e) {
				// Catching the RemoteException means the Service was crashed
				// remotely
				// let's cancel this query task by stopping the timer
				this.cancel();
				mQueryTimer = null;
			}

		}
	}
}
