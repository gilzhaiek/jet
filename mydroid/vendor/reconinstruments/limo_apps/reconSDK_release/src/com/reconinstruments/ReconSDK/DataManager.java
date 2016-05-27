package com.reconinstruments.ReconSDK;

import java.util.LinkedHashMap;
import java.util.Map;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Messenger;
import android.util.Log;

/* Internal class that encapsulates client initiated IPC Requests
 * Not intended for outside intantation -- do NOT compile in ReconSDK.jar
 * 
 * If internal architecture changes, all that has to be done is subclass this class
 * Rest of SDK Public Interface remains the same
 *  */
public class DataManager
{
	// constants
	private static final String TAG = DataManager.class.getSimpleName();
	 
	// Map of IPC Reqeusts; we implement as linked map to guarantee order in case
	// of service disconnect
    Map<Integer, DataMessage> mRequests = new LinkedHashMap<Integer, DataMessage> ();
	  
    // Messenger for communicating with MOD Service
    private Messenger    mService = null;     
    
    // internal export -- MOD Service  Link Up/Down
    private void Connect (Context c)
    {
     	if (mService == null)
    	{
    		   c.bindService
    		   (
    			  new Intent(ReconMODService.INTENT_STRING),				
    			  mConnection, 
    			  Context.BIND_AUTO_CREATE
    		   );
    	}
    }
    
    @SuppressWarnings("unused")
	private void Disconnect (Context c)
    {
    	if (mService != null)
    	{
    		c.unbindService(mConnection);
    	}
    }
    
    // internal export -- Queue Data Request
    protected void QueueRequest (Context c, IReconDataReceiver completion, int dataType)
    {
    	// if 1st time, must connect service
    	this.Connect(c);
    	
    	// check if we have this request already
    	if (mRequests.containsKey(dataType) == true)
        {
    		Log.i(TAG, String.format("Request for Data Type [%d] already pending; second attempt ignored", dataType) );
    		return;
        }
    	
    	// no -- allocate new Message
    	mRequests.put(dataType, new DataMessage (this, completion, dataType) );
    	
    	// queue processing if service is connected; if not
    	// it will be invoked when it connects
    	this.ProcessRequests();
    }
    
    /**
     * Class for interacting with the main interface MOD Service
     */
    private ServiceConnection mConnection = new ServiceConnection()
    {
	    public void onServiceConnected(ComponentName className, IBinder service)
	    {
		   Log.d(TAG, "MOD Service connected");
		   mService = new Messenger(service);
		   
		   // queue all outstanding requests
		   DataManager.this.ProcessRequests();
	    }

	    public void onServiceDisconnected(ComponentName className)
	    {
		   // This is called when the connection with the service has been
		   // unexpectedly disconnected -- that is, its process crashed.
		   mService = null;
	    }
	};
	
	// internal implementation
	private   void ProcessRequests()
	{
		if (mService != null)
		{
			// iterate map and send all queued messages
			for (Integer msgID : mRequests.keySet() )
			{
				DataMessage msg = mRequests.get(msgID);
				msg.Send(mService);
		    }
		}
	}

	protected void RequestCompleted (int dataType)
	{
		// we only clear request id map
		mRequests.remove(dataType);
	}
	
    // data id to message type resolver
	protected static final int Type2ID (int type) throws InstantiationException
	{
		int messageID = 0;
		
		switch (type)
		{
		   case ReconEvent.TYPE_JUMP:
			   messageID = ReconMODService.Message.MSG_GET_JUMP_BUNDLE;
			   break;
			   
		  /* case ReconEvent.TYPE_ADVANCED_JUMP:
			   break;*/
			   
		   case ReconEvent.TYPE_TEMPERATURE:
			   messageID = ReconMODService.Message.MSG_GET_TEMPERATURE_BUNDLE;
			   break;
			   
		   case ReconEvent.TYPE_DISTANCE:
			   messageID = ReconMODService.Message.MSG_GET_DISTANCE_BUNDLE;
			   break;
			   
		   case ReconEvent.TYPE_VERTICAL:
			   messageID = ReconMODService.Message.MSG_GET_VERTICAL_BUNDLE;
			   break;
			   
		   case ReconEvent.TYPE_LOCATION:
			   messageID = ReconMODService.Message.MSG_GET_LOCATION_BUNDLE;
			   break;

		   case ReconEvent.TYPE_SPEED:
			   messageID = ReconMODService.Message.MSG_GET_SPEED_BUNDLE;
			   break;
			   
		   case ReconEvent.TYPE_TIME:
			   messageID = ReconMODService.Message.MSG_GET_TIME_BUNDLE;
			   break;
			   
		   case ReconEvent.TYPE_ALTITUDE:
			   messageID = ReconMODService.Message.MSG_GET_ALTITUDE_BUNDLE;
			   break;
			   
		   case ReconEvent.TYPE_RUN:
			   messageID = ReconMODService.Message.MSG_GET_RUN_BUNDLE;
			   break;
			   
		   // full update
		   case ReconEvent.TYPE_FULL:
			   messageID = ReconMODService.Message.MSG_GET_FULL_INFO_BUNDLE;
			   break;
			   
		   default:
			   break;
		}
		
		if (messageID == 0)
		{
			throw new InstantiationException
   	          (String.format("%s: Invalid/unsupported Event Data Type: [%d]", TAG, type) );
		}
		return messageID;
	}
	
}
