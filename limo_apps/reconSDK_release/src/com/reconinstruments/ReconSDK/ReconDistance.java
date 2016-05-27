package com.reconinstruments.ReconSDK;

import java.lang.reflect.Method;
import java.util.zip.DataFormatException;

import android.os.Bundle;
import android.util.Log;

/** Concrete Implementation of {@link ReconEvent#TYPE_DISTANCE} Data Type <p>
 * 
 *  ReconDistance Data Object encapsulates distance covered during various *  Events such as Ski Runs etc. All Values are in meters. MOD Server keeps track of:
 *  <ul>
 *     <li> Horizontal Distance
 *     <li> Vertical Distance
 *     <li> Overall Distance
 *     <li> All Time Accumulated Distance
 *  </ul> 
 *  <p>
 *  Distance Events are not broadcasted by MOD Server.  Client must explicitly
 *  request an update {@link ReconSDKManager#receiveData(IReconDataReceiver, int)}
 *  */
public class ReconDistance extends ReconEvent
{
	// constants
	private static final String TAG = ReconDistance.class.getSimpleName();
	
	/** Invalid Distance Value */
    public static final int INVALID_DISTANCE = 0;
    
	// concrete type data fields
    protected float mHorzDistance     = ReconDistance.INVALID_DISTANCE;
    protected float mVertDistance     = ReconDistance.INVALID_DISTANCE;
    protected float mDistance         = ReconDistance.INVALID_DISTANCE;
    protected float mAllTimeDistance  = ReconDistance.INVALID_DISTANCE;
    
    /** Public Constructor */
    public ReconDistance ()
    {
    	mType = ReconEvent.TYPE_DISTANCE; 
    	BROADCAST_ACTION_STRING = ReconMODService.BroadcastString.BROADCAST_DISTANCE_ACTION_STRING;
    }
    
    // public data accessors
    
    /** Horizontal Distance Accessor 
     * <p>
     * @return Current Horizontal Distance
     */
    public    float GetHorizontalDistance ()   {return mHorzDistance;}
    
    /** Vertical Distance Accessor 
     * <p>
     * @return Current Vertical Distance
     */
    public    float GetVerticalDistance   ()   {return mVertDistance;}
    
    /** Overall Distance Accessor 
     * <p>
     * @return Current Overall Distance
     */
    public    float GetDistance           ()   {return mDistance;}
    
    /**All Time Distance Accessor 
     * <p>
     * @return All Time Accumulated Distance Value
     */
    public    float GetAllTimeDistance    ()   {return mAllTimeDistance;}

    // identification
    @Override
    public String toString()
    {
    	return ReconDistance.TAG;
    }
    
    // IPC Serialization
	@Override
	protected Bundle generateBundle() 
	{
	    Bundle b = new Bundle();
	    
	    b.putFloat(ReconMODService.FieldName.DISTANCE_HORIZONTAL, mHorzDistance);
	    b.putFloat(ReconMODService.FieldName.DISTANCE_VERTICAL,   mVertDistance);
	    b.putFloat(ReconMODService.FieldName.DISTANCE_VALUE,      mDistance);
	    b.putFloat(ReconMODService.FieldName.DISTANCE_ALL_TIME,   mAllTimeDistance);
		
		return b;
	}

	@Override
	protected Method changedField(String strName) throws DataFormatException
	{
		 // get changed data. We allow null if BROADCAST_FIELD is not found,
		 // ohterwise it must point to valid bundle key
		 Method m = null;
		 Class<? extends ReconDistance> clazz = this.getClass();

		 try
		 {
			 if (strName.equals(ReconMODService.FieldName.DISTANCE_HORIZONTAL))
				m = clazz.getMethod("GetHorizontalDistance", (Class<?>[])null);
				
			 else if  (strName.equals(ReconMODService.FieldName.DISTANCE_VERTICAL))
				m = clazz.getMethod("GetVerticalDistance", (Class<?>[])null);
				
			 else if (strName.equals(ReconMODService.FieldName.DISTANCE_VALUE))
				m = clazz.getMethod("GetDistance", (Class<?>[])null);
				
			 else if (strName.equals(ReconMODService.FieldName.DISTANCE_ALL_TIME))
				m = clazz.getMethod("GetAllTimeDistance", (Class<?>[])null);
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
		 if (b.containsKey(ReconMODService.FieldName.DISTANCE_HORIZONTAL) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.DISTANCE_HORIZONTAL) );
		
		 if (b.containsKey(ReconMODService.FieldName.DISTANCE_VERTICAL) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.DISTANCE_VERTICAL) );

		 if (b.containsKey(ReconMODService.FieldName.DISTANCE_VALUE) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.DISTANCE_VALUE) );

		 if (b.containsKey(ReconMODService.FieldName.DISTANCE_ALL_TIME) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.DISTANCE_ALL_TIME) );

		 // now just read them
		 mHorzDistance          = b.getFloat(ReconMODService.FieldName.DISTANCE_HORIZONTAL);
		 mVertDistance          = b.getFloat(ReconMODService.FieldName.DISTANCE_VERTICAL);
		 mDistance              = b.getFloat(ReconMODService.FieldName.DISTANCE_VALUE);
		 mAllTimeDistance       = b.getFloat(ReconMODService.FieldName.DISTANCE_ALL_TIME);
		 
	}

}
