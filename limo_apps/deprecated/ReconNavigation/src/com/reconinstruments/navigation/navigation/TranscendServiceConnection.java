/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.navigation;

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
import android.widget.Toast;

import com.reconinstruments.modservice.ReconMODServiceMessage;

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
	private MapView mMapView = null;
	private Messenger mIncomingMessenger = null;							//the messenger for handling message coming from TranscendService
	private Messenger mServiceMessenger = null;								//the messenger for sending query message to TranscendService
	private Timer mQueryTimer = null;
	
	
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
			case  ReconMODServiceMessage.MSG_RESULT:
				if( msg.arg1 == ReconMODServiceMessage.MSG_GET_SPEED_BUNDLE )				
				{
					Bundle data = msg.getData();
					//query the Bundle for the speed
					float speed = data.getFloat("Speed");
					
					//now set the speed to the map view
					mMapView.setSppeed(speed);
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
			msg.what = ReconMODServiceMessage.MSG_GET_SPEED_BUNDLE;
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
			}
			
			
		}		
	}
	
	public TranscendServiceConnection( Context context, MapView mapView )
	{
		mContext = context;
		mMapView = mapView;
		
		mIncomingMessenger = new Messenger( new TranscendMessageHandler( ));
		
	}
	@Override
	public void onServiceConnected(ComponentName name, IBinder binder) 
	{		
		mConnectionStatus = CONNECTION_STATUS_BOUND;
		if (DEBUG_TOAST){
		    Toast.makeText(mContext, "Connected to Transcend Service", Toast.LENGTH_SHORT ).show();}
		
		//create the messenger to sending message to TranscendService
		//Note that we using the binder coming from the TranscendService for
		//creating this messenger
		mServiceMessenger = new Messenger( binder );
		
		//now we have connected to the Transcend service, let's schedule a repeatitive task
		//for querying the service for informations
		mQueryTimer = new Timer();
		
		//schedule a repetitive task for querying information 
		//at the interval of 1 seconds, start 1 seconds later on
		mQueryTimer.schedule( new QueryTask(), 10000, 1000 );
	}

	@Override
	public void onServiceDisconnected(ComponentName name) 
	{
	    if (DEBUG_TOAST){
		Toast.makeText(mContext, "Disconnect from Transcend Service", Toast.LENGTH_SHORT ).show();}
		
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
			if (DEBUG_TOAST){
			    Toast.makeText(mContext, "Binding to Transcend Service", Toast.LENGTH_SHORT ).show();}
						
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
	
}