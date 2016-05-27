/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.dashlauncher.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.reconinstruments.applauncher.transcend.ReconTranscendService;

/**
 *This class implement a connection to Transcend
 *for querying information about Speed and Chrono
 */

public  class TranscendServiceConnection implements ServiceConnection
{

	static final int CONNECTION_STATUS_NOTSTARTED = 0;
	static final int CONNECTION_STATUS_BINDING = 1;
	static final int CONNECTION_STATUS_BOUND = 2;

    public static final boolean DEBUG_TOAST = false;    

	private Context mContext;
	private int mConnectionStatus = CONNECTION_STATUS_NOTSTARTED;	
	private Messenger mServiceMessenger = null;								//the messenger for sending query message to TranscendService	
	
	
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
			Log.e("Setting:", "Try to reset stats but the transcend service is not connected yet");
		}
	}
	
	//called by client to ask Transcend service to reset the stats
		public void resetAllTimeStats( )
		{
			if(mConnectionStatus == CONNECTION_STATUS_BOUND )
			{
				Message msg = Message.obtain( );
				msg.what = ReconTranscendService.MSG_RESET_ALLTIME_STATS;
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
				Log.e("Setting:", "Try to reset stats but the transcend service is not connected yet");
			}
		}
	
	public TranscendServiceConnection( Context context )
	{
		mContext = context;							
	}
	
	public void onServiceConnected(ComponentName name, IBinder binder) 
	{		
		mConnectionStatus = CONNECTION_STATUS_BOUND;
		if (DEBUG_TOAST)
		{
		    Toast.makeText(mContext, "Connected to Transcend Service", Toast.LENGTH_SHORT ).show();
		}
		
		//create the messenger to sending message to TranscendService
		//Note that we using the binder coming from the TranscendService for
		//creating this messenger
		mServiceMessenger = new Messenger( binder );	
	}

	public void onServiceDisconnected(ComponentName name) 
	{
	    if (DEBUG_TOAST)
	    {
	    	Toast.makeText(mContext, "Disconnect from Transcend Service", Toast.LENGTH_SHORT ).show();
	    }
		
		mConnectionStatus = CONNECTION_STATUS_NOTSTARTED;
		mServiceMessenger = null;
		
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
			if (DEBUG_TOAST)
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
			
		}
	}
	
}