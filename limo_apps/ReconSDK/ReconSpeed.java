package com.reconinstruments.ReconSDK;

import java.lang.reflect.Method;
import java.util.zip.DataFormatException;

import android.os.Bundle;
import android.util.Log;

/* Concrete Implementation of ReconEvent.TYPE_SPEED Data Type */
public class ReconSpeed extends ReconEvent 
{
	// constants
	private static final String TAG = ReconSpeed.class.getSimpleName();
    public  static final float INVALID_SPEED = -1; 

    // concrete type data fields
	protected float mSpeed            = ReconSpeed.INVALID_SPEED;
    protected float mHorzSpeed        = ReconSpeed.INVALID_SPEED;
    protected float mVertSpeed        = ReconSpeed.INVALID_SPEED;
    protected float mMaxSpeed         = ReconSpeed.INVALID_SPEED;
    protected float mAverageSpeed     = ReconSpeed.INVALID_SPEED;
    protected float mAllTimeMaxSpeed  = ReconSpeed.INVALID_SPEED;
    
    // public c-tor
    public ReconSpeed ()
    {
    	mType = ReconEvent.TYPE_SPEED; 
    	BROADCAST_ACTION_STRING = ReconMODService.BroadcastString.BROADCAST_SPEED_ACTION_STRING;
    }
    
    // public data accessors
    public float GetSpeed()             {return mSpeed;}
    public float GetHorizontalSpeed()   {return mHorzSpeed;}
    public float GetVerticalSpeed()     {return mVertSpeed;}
    public float GetMaximumSpeed()      {return mMaxSpeed;}
    public float GetAverageSpeed()      {return mAverageSpeed;}
    public float GetAllTimeMaxSpeed()   {return mAllTimeMaxSpeed;}
    
    // identification
    @Override
    public String toString()
    {
    	return ReconSpeed.TAG;
    }
    
	@Override
	protected Bundle generateBundle() 
	{
    	Bundle b = new Bundle();
    	
    	b.putFloat(ReconMODService.FieldName.SPEED_HORIZONTAL,   mHorzSpeed);
    	b.putFloat(ReconMODService.FieldName.SPEED_VERTICAL,     mVertSpeed);
    	b.putFloat(ReconMODService.FieldName.SPEED_VALUE,        mSpeed);
    	b.putFloat(ReconMODService.FieldName.SPEED_MAX,          mMaxSpeed);
	    b.putFloat(ReconMODService.FieldName.SPEED_ALL_TIME_MAX, mAllTimeMaxSpeed);
    	b.putFloat(ReconMODService.FieldName.SPEED_AVERAGE,      mAverageSpeed);
    	
    	return b;
	}

	@Override
	protected Method changedField(String strName) throws DataFormatException
	{
		 // get changed data. We allow null if BROADCAST_FIELD is not found,
		 // ohterwise it must point to valid bundle key
		 Method m = null;
		 Class<? extends ReconSpeed> clazz = this.getClass();

		 try
		 {
			 if (strName.equals(ReconMODService.FieldName.SPEED_VALUE))
			    m = clazz.getMethod("GetSpeed", (Class<?>[])null);
				
			 else if  (strName.equals(ReconMODService.FieldName.SPEED_HORIZONTAL))
				m = clazz.getMethod("GetHorizontalSpeed", (Class<?>[])null);
				
			 else if (strName.equals(ReconMODService.FieldName.SPEED_VERTICAL))
				m = clazz.getMethod("GetVerticalSpeed", (Class<?>[])null);
				
			 else if (strName.equals(ReconMODService.FieldName.SPEED_MAX))
				m = clazz.getMethod("GetMaximumSpeed", (Class<?>[])null);
				
			 else if (strName.equals(ReconMODService.FieldName.SPEED_AVERAGE))
				m = clazz.getMethod("GetAverageSpeed", (Class<?>[])null);
				
			 else if (strName.equals(ReconMODService.FieldName.SPEED_ALL_TIME_MAX))
				m = clazz.getMethod("GetAllTimeMaxSpeed", (Class<?>[])null);
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
		 if (b.containsKey(ReconMODService.FieldName.SPEED_VALUE) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.SPEED_VALUE) );
	
		 if (b.containsKey(ReconMODService.FieldName.SPEED_HORIZONTAL) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.SPEED_HORIZONTAL) );
	
		 if (b.containsKey(ReconMODService.FieldName.SPEED_VERTICAL) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.SPEED_VERTICAL) );

		 if (b.containsKey(ReconMODService.FieldName.SPEED_MAX) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.SPEED_MAX) );

		 if (b.containsKey(ReconMODService.FieldName.SPEED_AVERAGE) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.SPEED_AVERAGE) );

		 if (b.containsKey(ReconMODService.FieldName.SPEED_ALL_TIME_MAX) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.SPEED_ALL_TIME_MAX) );

		 // now just read them
		 mSpeed           = b.getFloat(ReconMODService.FieldName.SPEED_VALUE);
		 mHorzSpeed       = b.getFloat(ReconMODService.FieldName.SPEED_HORIZONTAL);
		 mVertSpeed       = b.getFloat(ReconMODService.FieldName.SPEED_VERTICAL);
		 mMaxSpeed        = b.getFloat(ReconMODService.FieldName.SPEED_MAX);
		 mAverageSpeed    = b.getFloat(ReconMODService.FieldName.SPEED_AVERAGE);
		 mAllTimeMaxSpeed = b.getFloat(ReconMODService.FieldName.SPEED_ALL_TIME_MAX);
		 
	}

}
