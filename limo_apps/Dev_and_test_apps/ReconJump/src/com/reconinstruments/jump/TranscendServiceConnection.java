/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.jump;

import java.util.Timer;
import java.util.TimerTask;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.reconinstruments.applauncher.transcend.ReconTranscendService;

/**
 *This class implement a connection to Transcend
 *for querying information 
 */

public  class TranscendServiceConnection implements ServiceConnection
{

	static final int CONNECTION_STATUS_NOTSTARTED = 0;
	static final int CONNECTION_STATUS_BINDING = 1;
	static final int CONNECTION_STATUS_BOUND = 2;
    public static final boolean DEBUG_TOAST = false;
	
	private Context mContext;
	private int mConnectionStatus = CONNECTION_STATUS_NOTSTARTED;
	private Messenger mIncomingMessenger = null;	//the messenger for handling message coming from TranscendService
	private Messenger mServiceMessenger = null;		//the messenger for sending query message to TranscendService
	private Timer mQueryTimer = null;
	
	
	private JumpTabView mJumpView;
	
	//handle the incoming message from the Transcend service
	//for getting the speed and chrono, and pass them
	//to mapview for rendering
	private class TranscendMessageHandler extends Handler
	{
		@Override
		public void handleMessage( Message msg )
		{
			switch( msg.what )
			{
			case  ReconTranscendService.MSG_RESULT:
				if( msg.arg1 == ReconTranscendService.MSG_GET_FULL_INFO_BUNDLE )				
				{
					Bundle data = msg.getData();
					
					mJumpView.setViewData(data);
					
				}
			break;
			
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	/**
	 * The periodly running task for sending message to TranscendService querying for information
	 */
	private class QueryTask extends TimerTask
	{

		@Override
		public void run() 
		{
			Message msg = Message.obtain( );
			msg.what = ReconTranscendService.MSG_GET_FULL_INFO_BUNDLE;
			msg.replyTo = mIncomingMessenger;
			
			try
			{
				mServiceMessenger.send(msg);
			}
			catch( RemoteException e )
			{
				//Catching the RemoteException means the Service was crashed remotely
				// let's cancel this query task by stopping the timer
				this.cancel();
				mQueryTimer = null;
				
				if( DEBUG_TOAST )
				{
					Toast.makeText(mContext, "Remote Service Crashed", Toast.LENGTH_SHORT ).show();
				}
			}
			
			
		}		
	}
	
	public TranscendServiceConnection( Context context, JumpTabView jumpView  )
	{
		mContext = context;
		mJumpView = jumpView;
		mIncomingMessenger = new Messenger( new TranscendMessageHandler( ));
		
	}
	
	public void onServiceConnected(ComponentName name, IBinder binder) 
	{		
		mConnectionStatus = CONNECTION_STATUS_BOUND;
		
		if( DEBUG_TOAST )
		{
			Toast.makeText(mContext, "Connected to Transcend Service", Toast.LENGTH_SHORT ).show();
		}
		
		//create the messenger to sending message to TranscendService
		//Note that we using the binder coming from the TranscendService for
		//creating this messenger
		mServiceMessenger = new Messenger( binder );
		
		
		//now we have connected to the Transcend service, let's schedule a repeatitive task
		//for querying the service for informations
		mQueryTimer = new Timer();
		
		//schedule a repetitive task for querying information 
		//at the interval of 1 seconds, start 0.1 seconds later on
		mQueryTimer.schedule( new QueryTask(), 100, 1000 );		
	}

	public void onServiceDisconnected(ComponentName name) 
	{
		if( DEBUG_TOAST )
		{
			Toast.makeText(mContext, "Disconnect from Transcend Service", Toast.LENGTH_SHORT ).show();
		}
		
		mConnectionStatus = CONNECTION_STATUS_NOTSTARTED;
		mServiceMessenger = null;
		
		//cancel the timer if it is set
		if( mQueryTimer != null )
		{
			mQueryTimer.cancel();
			mQueryTimer = null;
		}
	}
	
	/**
	 * Bind to Transcend service
	 */
	public void doBindService( )
	{
		if( mConnectionStatus == CONNECTION_STATUS_NOTSTARTED )
		{
			//ask the Context to bind to Transcend service
			//mContext.bindService( new Intent( TranscendServiceConnection.this, TranscendService.class ), this, Context.BIND_AUTO_CREATE );
			mConnectionStatus = CONNECTION_STATUS_BINDING;
			
			if( DEBUG_TOAST )
			{
				Toast.makeText(mContext, "Binding to Transcend Service", Toast.LENGTH_SHORT ).show();
			}
						
		}		
	}
	
	/**
	 * Un-bind to Transcend service
	 */
	public void doUnBindService()
	{
		if( mConnectionStatus == CONNECTION_STATUS_BOUND )
		{
			mContext.unbindService( this );
			mConnectionStatus = CONNECTION_STATUS_NOTSTARTED;
			
			//cancel the timer if it is set
			if( mQueryTimer != null )
			{
				mQueryTimer.cancel();
				mQueryTimer = null;
			}
		}
	}
	
	/*
	//called by client to ask Transcend service to reset the stats
	public void resetStats( )
	{
		if(mConnectionStatus == CONNECTION_STATUS_BOUND )
		{
			Message msg = Message.obtain( );
			msg.what = ReconTranscendService.MSG_RESET_STATS;
			msg.replyTo = null;
			
			try
			{
				mServiceMessenger.send(msg);
			}
			catch( RemoteException e )
			{
				//Catching the RemoteException means the Service was crashed remotely
				// let's cancel this query task by stopping the timer						
			}
		}
		else
		{
			//service not connected:
			Log.e("ReconStats:", "Try to reset stats but the transcend service is not connected yet");
		}
	}
	
	//called by client to ask Transcend service to reset the stats
	public void resetAllTimeStats( )
	{
		if(mConnectionStatus == CONNECTION_STATUS_BOUND )
		{
			Message msg = Message.obtain( );
			msg.what = ReconTranscendService.MSG_RESET_STATS;
			msg.replyTo = null;
			
			try
			{
				mServiceMessenger.send(msg);
			}
			catch( RemoteException e )
			{
				//Catching the RemoteException means the Service was crashed remotely
				// let's cancel this query task by stopping the timer						
			}
		}
		else
		{
			//service not connected:
			Log.e("ReconStats:", "Try to reset stats but the transcend service is not connected yet");
		}
	}
	*/
}