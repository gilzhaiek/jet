package com.reconinstruments.ReconSDK;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;


/* Internal class that encapsulates publish/subscribe mechanism of (broadcast) Recon Events
 * Not intended for outside intantation -- do NOT compile in ReconSDK.jar 
 * 
 * If internal architecture changes, all that has to be done is subclass this class
 * Rest of SDK Public Interface remains the same 
 * */
class EventManager
{
	// constants
	private static final String TAG = EventManager.class.getSimpleName();
	 
	// Map of Event Listeners
	private Map<Integer, EventNotifier> mListeners = new HashMap<Integer, EventNotifier>();
	
	// Listener Registration
	protected void registerListener (Context c, IReconEventListener listener, int type) throws InstantiationException
	{
	    // unregister all old listeners first
	    this.unregisterListener(c, type);
	
	    // parse type which can be bitmask  
	    for (int i = 0; i < 32; i++)
	    {
	    	if (CHECK_BIT(type, i) != 0)
	    	{
	    		Integer key = new Integer ( 1 << i );
	    		ReconEvent evt =  ReconEvent.Factory(key.intValue() );
	    		IntentFilter intentFilter = new IntentFilter
	    				  (evt.BROADCAST_ACTION_STRING);
	    		
	    		EventNotifier not = new EventNotifier(evt, listener );
	    	    c.registerReceiver(not, intentFilter);
	    			
	    		mListeners.put(evt.getType(), not );
	    	}
	    }

	}
	
	// Listener unregistration
	protected void unregisterListener (Context c, int type)
	{
		for (int i = 0; i < 32; i++)
		{ 
		   	if (CHECK_BIT(type, i) != 0)
	    	{ 
		   		Integer key = new Integer (1 << i);
		        if (mListeners.containsKey(key) )
		        {
		   		   EventNotifier not = mListeners.get(key);
		   		   c.unregisterReceiver(not);
		   		
		   		   mListeners.remove(key);
		        }
	    	}
		}
	}
	
	// internal class -- Broadcast Receiver
	private class EventNotifier extends BroadcastReceiver
	{
		// Recon Event this notifier is bound to
		private ReconEvent mEvent;    
		private IReconEventListener mListener;
		
		// initializing c-tor
        public EventNotifier (ReconEvent evt, IReconEventListener listener) 
        {
        	mEvent = evt;
        	mListener = listener;
        }
        
		@Override
		public void onReceive(Context context, Intent intent)
		{
			Method m = null;
			
			Log.d(TAG, "Event Broadcast Received!");
			
			// Event Data is in extras bundle. Construct appropriate object based on action string
			// If server sent crap, object deserializer will throw; we will log, and not give
			// client anything
			try
			{
			   Bundle b = intent.getExtras();
			   String s = intent.getStringExtra(ReconMODService.BROADCAST_FIELD);
			   
			   // first construct data bundle
		       mEvent.fromBundle(b.getBundle(ReconMODService.BROADCAST_BUNDLE) );
		       
		       // now get changed field
		       if (s != null)
		         m = mEvent.changedField(s);
			}
			
			catch (DataFormatException dfe)
			{
				Log.e(TAG, dfe.getMessage() );
				return;
			}
			
			catch (Exception ex)   // generic exception
			{
				if (ex.getMessage() == null)
				   Log.e(TAG, "Generic Communication Failure with Transcend Server" );
				else
				   Log.e(TAG, ex.getMessage() );
				
				return;
			}
		    
			// Here we have valid object, so give it to the client
			mListener.onDataChanged(mEvent, m);
		}

	} 
	
	// internal helpers
	private int CHECK_BIT(int var, int pos)
	{
		return ( (var) & ( 1 << (pos) ) );
	}
	 
}
