package com.reconinstruments.ReconSDK;

import java.lang.reflect.Method;
import java.util.zip.DataFormatException;

import android.os.Bundle;


/* Recon Event Base Class. Recon Events are things of Interest in Recon Platform.
 * They are direct aggregates built on top of Android Sensor Data.
 * 
 * SDK Clients have read-only access to Events; Data is managed in Recon Transcend Service
 * and broadcast across the platform when things of interest happen.
 * 
 * Clients interact with Recon Events in 2 different ways:
 * 
 * 1) Subscribing to one or more Broadcasts of Interest; upon receipt they get latest ReconEvent
 * 2) Issuing data reader requests; entire history is then marshalled from the server
 */
public abstract class ReconEvent
{
	static final String TAG = ReconEvent.class.getSimpleName();
	
   /* Recon Data Type Enumeration. Similar to Sensor.TYPE_XXX in android.hardware.Sensor
    * Values are chosen so that broadcast registration can be made with bitmask
    * 
    * Singletons are listed before aggregate events
    *  */
	
	public static final int TYPE_TEMPERATURE   = 1;
	public static final int TYPE_DISTANCE      = 2;
	public static final int TYPE_VERTICAL      = 4;
	public static final int TYPE_LOCATION      = 8;
	public static final int TYPE_SPEED         = 16;
	public static final int TYPE_TIME          = 32;
	public static final int TYPE_ALTITUDE      = 64;
	
	// Aggregates
    public static final int TYPE_JUMP          = 256;
    public static final int TYPE_RUN           = 512;
    
    //public static final int TYPE_ADVANCED_JUMP = 1024;
    
    // Full Bundle
    protected static final int TYPE_FULL          = 2048;
    
    /* Properties
     * At base class we define only type RTTI. Derived concrete Events
     * will define their own set of data properties
     *  */
    protected int mType;
    protected String BROADCAST_ACTION_STRING;
    
    public int getType() {return mType;}
    
    /* Methods */
    
    // RPC serialization; must be implemented in derived classes
    // Note: Due to release constraints, developers responsible for Transcend service
    //       want to minimize code changes. Eventually when transcend implementation
    //       is cleaned up, it should start using SDK Published ReconEvent; this is
    //       why I am implementing both in/out Bundle serialization
    protected abstract Bundle  generateBundle();
    
    protected abstract void    fromBundle(Bundle b)   throws DataFormatException;
    protected abstract Method  changedField(String s) throws DataFormatException;
    
    // Virtual constructor -- factory dispatch
    static ReconEvent Factory (int type) throws InstantiationException
    {
    	ReconEvent ret = null;
    	
    	switch (type)
    	{
    	   case ReconEvent.TYPE_TEMPERATURE:
    		   ret = new ReconTemperature();
    		   break;
    		   
    	   case ReconEvent.TYPE_DISTANCE:
    		   ret = new ReconDistance();
    		   break;
    		   
    	   case ReconEvent.TYPE_VERTICAL:
    		   ret = new ReconVertical();
    		   break;
    		   
    	   case ReconEvent.TYPE_LOCATION:
    		   ret = new ReconLocation();
    		   break;
    		   
    	   case ReconEvent.TYPE_SPEED:
    		   ret = new ReconSpeed();
    		   break;
    		
    	   case ReconEvent.TYPE_TIME:
    		   ret = new ReconTime();
    		   break;
    		   
    	   case ReconEvent.TYPE_JUMP:
    		   ret = new ReconJump();
    		   break;
    		 
    	   case ReconEvent.TYPE_RUN:
    		   ret = new ReconRun();
    		   break;
    		   
    	   case ReconEvent.TYPE_ALTITUDE:
    		   ret = new ReconAltitude();
    		   break;
    		   
    	   /*case ReconEvent.TYPE_ADVANCED_JUMP:
    		   ret = new ReconAdvancedJump();
    		   break;*/
    		   
    	   default:
    		  break; 
    	}
    	
    	if (ret == null) 
        {
    		throw new InstantiationException 
    	     (String.format("%s: Invalid/unsupported Event Data Type: [%d]", TAG, type) );
        }
    	
    	return ret;
    }
      
}
