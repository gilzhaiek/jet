package com.reconinstruments.ReconSDK;

import java.lang.reflect.Method;
import java.util.zip.DataFormatException;

import android.os.Bundle;


/** Class representing a Recon Data Event <p>
 * This is a standard example of polymorphism; concrete Event Data types are always derivatives
 * from ReconEvent abstract base. Provides Run Time Type Information (RTTI) and enumerates
 * supported Event Types. Retrieved Data Results using services of {@link ReconSDKManager}
 * are always in terms of ReconEvent base class.
 * 
 */
public abstract class ReconEvent
{
	static final String TAG = ReconEvent.class.getSimpleName();
 
	/** A constant describing Recon Temperature Event Type. See {@link ReconTemperature} */
	public static final int TYPE_TEMPERATURE   = 1;
	
	/** A constant describing Recon Distance (Horizontal) Event Type. See {@link ReconJump} */
	public static final int TYPE_DISTANCE      = 2;
	
	/** A constant describing Recon Distance (Vertical) Event Type. See {@link ReconVertical} */
	public static final int TYPE_VERTICAL      = 4;
	
	/** A constant describing A constant describing Recon Location Event Type. See {@link ReconLocation} */
	public static final int TYPE_LOCATION      = 8;
	
	/** A constant describing Recon Speed Event Type. See {@link ReconSpeed} */
	public static final int TYPE_SPEED         = 16;
	
	/** A constant describing Recon Time Event Type. See {@link ReconTime} */
	public static final int TYPE_TIME          = 32;
	
	/** A constant describing Recon Altitude Event Type. See {@link ReconAltitude} */
	public static final int TYPE_ALTITUDE      = 64;
	
	// Aggregates
	
	/** A constant describing Recon Jump Event Type. Aggregate. See {@link ReconJump} */
    public static final int TYPE_JUMP          = 256;
    
    /** A constant describing Recon Run Event Type. Aggregate. See {@link ReconRun} */
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
    
    /**
     * Run Time Type Information of concrete Recon Event Object -- Recon  Data Type Enumeration.
     * Similar to Sensor.TYPE_XXX enumeration in <a href="http://developer.android.com/reference/android/hardware/Sensor.html">
	 * Android Sensor API</a>. Type Values are chosen so that broadcast registration can be made with bitmask
     * @return  Event Object ID (ReconEvent.TYPE_XXX enumeration)
     * 
     */
    public int getType() {return mType;}
    
    /* Methods */
    
    // RPC serialization; must be implemented in derived classes
    // Note: Due to release constraints, developers responsible for MOD service
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
