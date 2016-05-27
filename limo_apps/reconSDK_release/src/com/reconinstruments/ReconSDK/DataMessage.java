package com.reconinstruments.ReconSDK;

/* Derivative of android.os.handler; processing of IPC Messages from MOD Service
 * SDK Internal -- do NOT compile in ReconSDK.jar */

import java.util.ArrayList;
import java.util.zip.DataFormatException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class DataMessage extends Handler
{
	// constants
	private static final String TAG = DataMessage.class.getSimpleName();
	 
	// internal data members
	private DataManager mParent = null;        // Parent class -- data manager - we will signal once we are done
	private IReconDataReceiver mClient = null; // Client callback
	private Messenger   mMessenger = null;     // Service connection: At this level we want it established
	private boolean     mSent = false;         // message sent (vs queued) indicator
	private int         mDataID = 0;           // Data ID
	
	// hide default c-tor
	@SuppressWarnings("unused")
	private DataMessage(){;}
	
	// initializing c-tor
	protected DataMessage (DataManager parent, IReconDataReceiver clientCallback, int dataID) 
	{
		mParent = parent;
		mClient = clientCallback;
		mDataID = dataID;
		
		mMessenger = new Messenger(this);
	}
	
	// internal export to send ourselves to MOD
	// TODO: Thread safety -- lock?
	protected void Send (Messenger service) 
	{
   	     if (mSent == false)
   	     {
   	    	 // map message to IPC Type
   	    	 int messageID = 0;
   	    	 
			 try 
			 {
				messageID = DataManager.Type2ID(mDataID);
			 } 
			 catch (InstantiationException e)
			 {
		    	 Log.e(TAG, e.getMessage() );  // we throw this one, so it is formatted nicely
				 
				 // invoke client callback telling we crapped out
				 mClient.onReceiveCompleted(ReconSDKManager.GENERIC_FAILURE, null);
				 
				 // clear up the request
				 mParent.RequestCompleted (mDataID );
				 
				 return;
			 }
   	    	 
	   	     Message ipc = Message.obtain(null,
						 messageID, 0, 0);
		       
		     ipc.replyTo = mMessenger;
		     
		     try
		     {
		        service.send(ipc);
		     }
		     catch (RemoteException ex)
		     {
		    	 Log.e(TAG, String.format("Server Communication Failure. Message ID [%d], Reason: %s", 
						 messageID, ex.getMessage() ) );
				 
				 // invoke client callback telling we crapped out
				 mClient.onReceiveCompleted(ReconSDKManager.GENERIC_FAILURE, null);
				 
				 // clear up the request
				 mParent.RequestCompleted (mDataID );
		     }
		     
		     mSent = true;
   	     }
	}
	
	@Override
	public void handleMessage(Message msg)
	{
	    Log.d(TAG, String.format("Received message: [%d]", msg.what) );
	    switch (msg.what)
	    {
	       // Data Object deserializer
	       case ReconMODService.Message.MSG_RESULT:
	    	   
	    	 try
	    	 {
		        if (this.mDataID == ReconEvent.TYPE_FULL)
		        {
		    		ArrayList<ReconDataResult> results = new ArrayList<ReconDataResult>();
		        	this.DecomposeFull((Bundle)msg.getData(), results);
		        	
		        	// invoke client callback
		        	mClient.onFullUpdateCompleted(ReconSDKManager.STATUS_OK, results);
		        }
		        else
		        {
	    		    ReconDataResult result = new ReconDataResult();
	    			this.Decompose ((Bundle)msg.getData(), mDataID, result);
	    			
	    			// invoke client callback
	    			mClient.onReceiveCompleted(ReconSDKManager.STATUS_OK, result);
		        }
		        	 
	    	 }
	    	 catch (DataFormatException dfe)
	    	 {
	    		 Log.e(TAG, dfe.getMessage() );
	    		 
	    		 if (this.mDataID == ReconEvent.TYPE_FULL)
	    			 mClient.onFullUpdateCompleted(ReconSDKManager.ERR_DATA_FORMAT,  null);
	    		 else
	    		     mClient.onReceiveCompleted(ReconSDKManager.ERR_DATA_FORMAT, null); 
	    	 }
	    	 
	    	 catch (UnsupportedOperationException uoe)
	    	 {
	    		 Log.e(TAG, uoe.getMessage() );
	    		 
	    		 if (this.mDataID == ReconEvent.TYPE_FULL)
	    			 mClient.onFullUpdateCompleted(ReconSDKManager.ERR_NOT_SUPPORTED,  null);
	    		 else
	    		     mClient.onReceiveCompleted(ReconSDKManager.ERR_NOT_SUPPORTED, null);
	    	 }
	    	 
	    	 catch (Exception ex)  // catch all -- IPC failure (invalid bundles -- nulls -- etc etc)
	    	                       // beware stupid Java will throw with getMessage() null for null pointer exception
	    	 {
			     if (ex.getMessage() == null)
				    Log.e(TAG, "Generic Communication Failure with MOD Server" );
				 else
				    Log.e(TAG, ex.getMessage() );
	    		 
	    		 if (this.mDataID == ReconEvent.TYPE_FULL)
	    			 mClient.onFullUpdateCompleted(ReconSDKManager.GENERIC_FAILURE,  null);
	    		 else
	    		     mClient.onReceiveCompleted(ReconSDKManager.GENERIC_FAILURE, null);
	    	 }
		      
	    	 // at any rate tell parent request has completed
	 		 mParent.RequestCompleted(mDataID);
	    	 break;
	    }

	}
	
	// IPC Decomposer of Full Bundle Update
	private void DecomposeFull (Bundle bfull, ArrayList<ReconDataResult> results) throws DataFormatException, UnsupportedOperationException
	{
		// Altitude
		ReconDataResult alt = new ReconDataResult();
		Bundle bdata = bfull.getBundle(ReconMODService.FullUpdate.ALTITUDE);
		
		if (bdata != null)
		{
	       this.Decompose(bdata, ReconEvent.TYPE_ALTITUDE, alt);
	    
	       if (alt.arrItems.size() > 0)
	          results.add(alt);
		}
	    
	    // Speed
	    ReconDataResult speed = new ReconDataResult();
	    bdata = bfull.getBundle(ReconMODService.FullUpdate.SPEED);
	    
	    if (bdata != null)
	    {
		    this.Decompose(bdata, ReconEvent.TYPE_SPEED, speed);
		    
		    if (speed.arrItems.size() > 0)
		       results.add(speed);
	    }
	
	    // Distance
	    ReconDataResult distance = new ReconDataResult();
	    bdata = bfull.getBundle(ReconMODService.FullUpdate.DISTANCE);
	    
	    if (bdata != null)
	    {
		    this.Decompose(bdata, ReconEvent.TYPE_DISTANCE, distance);
		    
		    if (distance.arrItems.size() > 0)
		       results.add(distance);
	    }
	    
	    // Jump
	    ReconDataResult jumps = new ReconDataResult();
	    bdata = bfull.getBundle(ReconMODService.FullUpdate.JUMPS);
	    
	    if (bdata != null)
	    {
		    this.Decompose(bdata, ReconEvent.TYPE_JUMP, jumps);
		    
		    if (jumps.arrItems.size() > 0)
		       results.add(jumps);
	    }
	    
	    // Run
	    ReconDataResult runs = new ReconDataResult();
	    bdata = bfull.getBundle(ReconMODService.FullUpdate.RUNS);
	    
	    if (bdata != null)
	    {
		    this.Decompose(bdata, ReconEvent.TYPE_RUN, runs);
		    
		    if (runs.arrItems.size() > 0)
		       results.add(runs);
	    }
	    
	    // Temperature
	    ReconDataResult temperature = new ReconDataResult();
	    bdata = bfull.getBundle(ReconMODService.FullUpdate.TEMPERATURE);
	    
	    if (bdata != null)
	    {
		    this.Decompose(bdata, ReconEvent.TYPE_TEMPERATURE, temperature);
		    
		    if (temperature.arrItems.size() > 0)
		       results.add(temperature);
	    }
	    
	    // Vertical
	    ReconDataResult vertical = new ReconDataResult();
	    bdata = bfull.getBundle(ReconMODService.FullUpdate.VERTICAL);
	    
	    if (bdata != null)
	    {
		    this.Decompose(bdata, ReconEvent.TYPE_VERTICAL, vertical);
		    
		    if (vertical.arrItems.size() > 0)
		       results.add(vertical);
	    }
	    
	    // Location
	    ReconDataResult location = new ReconDataResult();
	    bdata = bfull.getBundle(ReconMODService.FullUpdate.LOCATION);
	    
	    if (bdata != null)
	    {
		    this.Decompose(bdata, ReconEvent.TYPE_LOCATION, location);
		    
		    if (location.arrItems.size() > 0)
		       results.add(location);
	    }
	    
	    // Time
	    ReconDataResult time = new ReconDataResult();
	    bdata = bfull.getBundle(ReconMODService.FullUpdate.TIME);
	    
	    if (bdata != null)
	    {
		    this.Decompose(bdata, ReconEvent.TYPE_TIME, time);
		    
		    if (time.arrItems.size() > 0) 
		       results.add(time);
	    }
	}
	
	// IPC Decomposer of ReconEvent objects
	private void Decompose (Bundle bdata, int dataID, ReconDataResult result) throws DataFormatException, UnsupportedOperationException
	{
		boolean bSingleton = true;
		
		ReconEvent evt = null;
		result.itemCounter = 1;   // start with single object
		
		switch (dataID)
		{
		   // temperature, distance, vertical, location send only single object
		   case (ReconEvent.TYPE_TEMPERATURE):
		   {
		       evt = new ReconTemperature();
		   }
		   break;
		   
		   // distance sends only single object
		   case (ReconEvent.TYPE_DISTANCE):
		   {
		       evt = new ReconDistance();
		   }
		   break;
		       
		   // vertical sends only single object
		   case (ReconEvent.TYPE_VERTICAL):
		   {
		       evt = new ReconVertical();
		   }
		   break;
		       
		   // location sends only single object
		   case (ReconEvent.TYPE_LOCATION):
		   {
			   evt = new ReconLocation();
		   }
		   break;
		   
		       
		   // speed sends only single object
		   case (ReconEvent.TYPE_SPEED):
		   {
			   evt = new ReconSpeed();
		   }
		   break;
		       
		   // time sends only single object
		   case (ReconEvent.TYPE_TIME):
		   {
			   evt = new ReconTime();
		   }
		   break;
		      
		   // altitude sends only single object
		   case (ReconEvent.TYPE_ALTITUDE):
		   {
			   evt = new ReconAltitude();
		   }
		   break;
		   
		   // jump is aggregate
		   case (ReconEvent.TYPE_JUMP):
		   {
		       // jump counter
		       result.itemCounter = bdata.getInt(ReconMODService.FieldName.JUMP_COUNTER);
		     
		       // Bundle array
		       ArrayList<Bundle> jumps =  bdata.getParcelableArrayList(ReconMODService.FieldName.JUMP_LIST);
		     
		       for (int i = 0; i < jumps.size(); i++)
		       {
		    	   evt = new ReconJump();
		    	   evt.fromBundle(jumps.get(i) );
		    	
		    	   result.arrItems.add(evt);
		       }
		       
		       // flag as non-singleton
		       bSingleton = false;
		   }
		   break;
		   
		   // run is aggregage
		   case (ReconEvent.TYPE_RUN):
		   {
		       // total number of ski runs
		       result.itemCounter = bdata.getInt(ReconMODService.FieldName.RUN_SKI_TOTAL);
		     
		       // Bundle array
		       ArrayList<Bundle> runs =  bdata.getParcelableArrayList(ReconMODService.FieldName.RUN_LIST);
		     
		       for (int i = 0; i < runs.size(); i++)
		       {
		    	   evt = new ReconRun();
		    	   evt.fromBundle(runs.get(i) );
		    	
		    	   result.arrItems.add(evt);
		       }
		       
		       // flag as non-singleton
		       bSingleton = false;
		   }
		   break;
		   
		/*   case (ReconEvent.TYPE_ADVANCED_JUMP):
			   break; */
	
		   default:
			   break;
		}
		
		// common post-process, if singleton
		if (bSingleton == true)
		{
		   if (evt == null)  throw new UnsupportedOperationException("This feature is not supported yet");
		   
	       evt.fromBundle(bdata);
	       result.arrItems.add(evt);
		}

	}
}
