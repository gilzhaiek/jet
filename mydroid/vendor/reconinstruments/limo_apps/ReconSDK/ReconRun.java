package com.reconinstruments.ReconSDK;

import java.lang.reflect.Method;
import java.util.zip.DataFormatException;

import android.os.Bundle;
import android.util.Log;

/* Concrete Implementation of ReconEvent.TYPE_RUN Object */
public class ReconRun extends ReconEvent
{
	// constants
	private static final String TAG = ReconRun.class.getSimpleName();
	
	public static final int   INVALID_SEQUENCE = -1;
	public static final int   INVALID_START    = -1;
    public static final float INVALID_SPEED = -1;
    public static final float INVALID_DISTANCE = -1;
    public static final float INVALID_VERTICAL = -1;
    
	// concrete type data fields
    protected int   mNumber       = ReconRun.INVALID_SEQUENCE;
    protected int   mStart        = ReconRun.INVALID_START;
    protected float mAverageSpeed = ReconRun.INVALID_SPEED;
    protected float mMaxSpeed     = ReconRun.INVALID_SPEED;
    protected float mDistance     = ReconRun.INVALID_DISTANCE;
    protected float mVertical     = ReconRun.INVALID_VERTICAL;
    
    // public c-tor
    public ReconRun ()
    {
    	mType = ReconEvent.TYPE_RUN;
    	BROADCAST_ACTION_STRING = ReconMODService.BroadcastString.BROADCAST_RUN_ACTION_STRING;
    }
    
    // public data accessors
    public    int   GetSequence           ()   {return mNumber;}
    public    int   GetStart              ()   {return mStart;}
    public    float GetAverageSpeed       ()   {return mAverageSpeed;}
    public    float GetMaximumSpeed       ()   {return mMaxSpeed;}
    public    float GetDistance           ()   {return mDistance;}
    public    float GetVertical           ()   {return mVertical;}
    
    // identification
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
