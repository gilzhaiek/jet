package com.reconinstruments.ReconSDK;

import java.lang.reflect.Method;
import java.util.zip.DataFormatException;

import android.os.Bundle;
import android.util.Log;

/* Concrete Implementation of ReconEvent.TYPE_DISTANCE Data Type */
public class ReconDistance extends ReconEvent
{
	// constants
	private static final String TAG = ReconDistance.class.getSimpleName();
    public static final int INVALID_DISTANCE = 0;
    
	// concrete type data fields
    protected float mHorzDistance     = ReconDistance.INVALID_DISTANCE;
    protected float mVertDistance     = ReconDistance.INVALID_DISTANCE;
    protected float mDistance         = ReconDistance.INVALID_DISTANCE;
    protected float mAllTimeDistance  = ReconDistance.INVALID_DISTANCE;
    
    // public c-tor
    public ReconDistance ()
    {
    	mType = ReconEvent.TYPE_DISTANCE; 
    	BROADCAST_ACTION_STRING = ReconMODService.BroadcastString.BROADCAST_DISTANCE_ACTION_STRING;
    }
    
    // public data accessors
    public    float GetHorizontalDistance ()   {return mHorzDistance;}
    public    float GetVerticalDistance   ()   {return mVertDistance;}
    public    float GetDistance           ()   {return mDistance;}
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
