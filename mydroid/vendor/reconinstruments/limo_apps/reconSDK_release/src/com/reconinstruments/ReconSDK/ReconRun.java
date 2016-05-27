package com.reconinstruments.ReconSDK;

import java.lang.reflect.Method;
import java.util.zip.DataFormatException;

import android.os.Bundle;
import android.util.Log;

/** Concrete Implementation of  {@link ReconEvent#TYPE_RUN} Object <p>
 * 
 *  ReconRun is an aggregate Type. Typically the start of the Run is determined by the rate of vertical descent.
 *  The threshold is descending 10 metres or more in altitude in 10 seconds or less. The end of the Run occurs
 *  when there is a vertical ascent of 10 metres or more in 10 seconds or less. Occasionally 1 or more data values
 *  might not be available; in that case corresponding data field will be set to ReconRun.INVALID_XXX value. <p>
 *  
 *  New Run Events are always broadcasted across the Platform. The SDK Client can be notified by subscribing 
 *  to Run Events {@link ReconSDKManager#registerListener(IReconEventListener, int)} <p>
 *  Full Run History can be retrieved by calling {@link ReconSDKManager#receiveData(IReconDataReceiver, int)}
 *  */
public class ReconRun extends ReconEvent
{
	// constants
	private static final String TAG = ReconRun.class.getSimpleName();
	
	/** Invalid Run Sequence Value */
	public static final int   INVALID_SEQUENCE = -1;
	
	/** Invalid Run Start Value */
	public static final int   INVALID_START    = -1;
	
	/** Invalid Run Speed Value */
    public static final float INVALID_SPEED = -1;
    
    /** Invalid Run Distance Value */
    public static final float INVALID_DISTANCE = -1;
    
    /** Invalid Run Vertical Value */
    public static final float INVALID_VERTICAL = -1;
    
	// concrete type data fields
    protected int   mNumber       = ReconRun.INVALID_SEQUENCE;
    protected int   mStart        = ReconRun.INVALID_START;
    protected float mAverageSpeed = ReconRun.INVALID_SPEED;
    protected float mMaxSpeed     = ReconRun.INVALID_SPEED;
    protected float mDistance     = ReconRun.INVALID_DISTANCE;
    protected float mVertical     = ReconRun.INVALID_VERTICAL;
    
    /** Public Constructor */
    public ReconRun ()
    {
    	mType = ReconEvent.TYPE_RUN;
    	BROADCAST_ACTION_STRING = ReconMODService.BroadcastString.BROADCAST_RUN_ACTION_STRING;
    }
    
    // public data accessors
    
    /** Run Sequence Accessor 
     * <p>
     * @return Run Sequence Identification number
     */
    public    int   GetSequence           ()   {return mNumber;}
    
    /** Run Start Timestamp Accessor
     * <p>
     * @return Timestamp of Run Start Event
     */
    public    int   GetStart              ()   {return mStart;}
    
    /** Run Average Speed Accessor
     * <p>
     * @return Average Speed achieved during the Run
     */
    public    float GetAverageSpeed       ()   {return mAverageSpeed;}
    
    /** Run Maximum Speed Accessor
     * <p>
     * @return Maximum speed (km/h) achieved during the Run
     */
    public    float GetMaximumSpeed       ()   {return mMaxSpeed;}
    
    /** Run Distance Accessor
     * <p>
     * @return Horizontal Distance in meters achieved during the Run
     */
    public    float GetDistance           ()   {return mDistance;}
    
    /** Run Vertical Distance Accessor
     * <p>
     * @return Vertical Distance in meters achieved during the Run
     */
    public    float GetVertical           ()   {return mVertical;}
    
    @Override
    public String toString()
    {
    	return ReconRun.TAG;
    }
    
	@Override
	protected Bundle generateBundle() 
	{
   	    Bundle b = new Bundle();
    	
    	b.putInt   (ReconMODService.FieldName.RUN_SEQUENCE,   mNumber);
    	b.putInt   (ReconMODService.FieldName.RUN_START,      mStart);
    	b.putFloat (ReconMODService.FieldName.RUN_AVG_SPEED,  mAverageSpeed);
    	b.putFloat (ReconMODService.FieldName.RUN_MAX_SPEED,  mMaxSpeed);
    	b.putFloat (ReconMODService.FieldName.RUN_DISTANCE,   mDistance);
    	b.putFloat (ReconMODService.FieldName.RUN_VERTICAL,   mVertical);
    	
    	return b;
	}

	@Override
	protected Method changedField(String strName) throws DataFormatException
	{
		 // get changed data. We allow null if BROADCAST_FIELD is not found,
		 // ohterwise it must point to valid bundle key
		 Method m = null;
		 Class<? extends ReconRun> clazz = this.getClass();
			
		 try
		 {
		    if (strName.equals(ReconMODService.FieldName.RUN_SEQUENCE))
		       m = clazz.getMethod("GetSequence", (Class<?>[])null);
				
		    else if  (strName.equals(ReconMODService.FieldName.RUN_START))
			   m = clazz.getMethod("GetStart", (Class<?>[])null);
				
		    else if (strName.equals(ReconMODService.FieldName.RUN_AVG_SPEED))
			   m = clazz.getMethod("GetAverageSpeed", (Class<?>[])null);
		    
		    else if (strName.equals(ReconMODService.FieldName.RUN_MAX_SPEED))
			   m = clazz.getMethod("GetMaximumSpeed", (Class<?>[])null);
		    
		    else if (strName.equals(ReconMODService.FieldName.RUN_DISTANCE))
			   m = clazz.getMethod("GetDistance", (Class<?>[])null);
		    
		    else if (strName.equals(ReconMODService.FieldName.RUN_VERTICAL))
			   m = clazz.getMethod("GetVertical", (Class<?>[])null);
		 }	
		 
		 catch (Exception ex)
		 {
			Log.e(TAG, ex.getMessage() );
		 }
				
		 if (m == null) throw new DataFormatException(String.format("%s: Invalid Broadcast Changed Value", TAG) );
		 
		 return m;
	}

	
	@Override
	protected void fromBundle(Bundle b) throws DataFormatException 
	{
		 // validate bundle
		 if (b.containsKey(ReconMODService.FieldName.RUN_SEQUENCE) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.RUN_SEQUENCE) );
		
		 if (b.containsKey(ReconMODService.FieldName.RUN_START) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.RUN_START) );

		 if (b.containsKey(ReconMODService.FieldName.RUN_AVG_SPEED) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.RUN_AVG_SPEED) );
		 
		 if (b.containsKey(ReconMODService.FieldName.RUN_MAX_SPEED) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.RUN_MAX_SPEED) );
		 
		 if (b.containsKey(ReconMODService.FieldName.RUN_DISTANCE) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.RUN_DISTANCE) );
		 
		 if (b.containsKey(ReconMODService.FieldName.RUN_VERTICAL) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.RUN_VERTICAL) );

		 // now just read them
		 mNumber          = b.getInt   (ReconMODService.FieldName.RUN_SEQUENCE);
		 mStart           = b.getInt   (ReconMODService.FieldName.RUN_START);
		 mAverageSpeed    = b.getFloat (ReconMODService.FieldName.RUN_AVG_SPEED);
		 mMaxSpeed        = b.getFloat (ReconMODService.FieldName.RUN_MAX_SPEED);
		 mDistance        = b.getFloat (ReconMODService.FieldName.RUN_DISTANCE);
		 mVertical        = b.getFloat (ReconMODService.FieldName.RUN_VERTICAL);
		    
	}


}
