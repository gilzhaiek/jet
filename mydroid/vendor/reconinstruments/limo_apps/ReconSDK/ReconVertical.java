package com.reconinstruments.ReconSDK;

import java.lang.reflect.Method;
import java.util.zip.DataFormatException;

import android.os.Bundle;
import android.util.Log;

/* Concrete Implementation of ReconEvent.TYPE_VERTICAL Data Type */
public class ReconVertical extends ReconEvent
{
	// constants
	private static final String TAG = ReconVertical.class.getSimpleName();
    public static final int INVALID_VERTICAL = 0;
    
	// concrete type data fields
    protected float mPreviousVert = ReconVertical.INVALID_VERTICAL;
    protected float mVert = ReconVertical.INVALID_VERTICAL;
    protected float mAllTimeVert = ReconVertical.INVALID_VERTICAL;
    
    // public c-tor
    public ReconVertical ()
    {
    	mType = ReconEvent.TYPE_VERTICAL;
    	BROADCAST_ACTION_STRING = ReconMODService.BroadcastString.BROADCAST_VERTICAL_ACTION_STRING;
    }
    
    // public data accessors
    public    float GetVertical           ()   {return mVert;}
    public    float GetPreviousVertical   ()   {return mPreviousVert;}
    public    float GetAllTimeVertical    ()   {return mAllTimeVert;}
    
    // identification
    @Override
    public String toString()
    {
    	return ReconVertical.TAG;
    }
    
	// IPC Serialization
	@Override
	protected Bundle generateBundle() 
	{
		Bundle b = new Bundle();

		b.putFloat(ReconMODService.FieldName.VERTICAL_PREVIOUS, mPreviousVert);
		b.putFloat(ReconMODService.FieldName.VERTICAL_VALUE,    mVert);
		b.putFloat(ReconMODService.FieldName.VERTICAL_ALL_TIME, mAllTimeVert);
		
		return b;
	}

	@Override
	protected Method changedField(String strName) throws DataFormatException
	{
		 // get changed data. We allow null if BROADCAST_FIELD is not found,
		 // ohterwise it must point to valid bundle key
		 Method m = null;
		 Class<? extends ReconVertical> clazz = this.getClass();
			
		 try
		 {
		    if (strName.equals(ReconMODService.FieldName.VERTICAL_PREVIOUS))
		       m = clazz.getMethod("GetPreviousVertical", (Class<?>[])null);
				
		    else if  (strName.equals(ReconMODService.FieldName.VERTICAL_VALUE))
			   m = clazz.getMethod("GetVertical", (Class<?>[])null);
				
		    else if (strName.equals(ReconMODService.FieldName.VERTICAL_ALL_TIME))
			   m = clazz.getMethod("GetAllTimeVertical", (Class<?>[])null);
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
		 if (b.containsKey(ReconMODService.FieldName.VERTICAL_PREVIOUS) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.VERTICAL_PREVIOUS) );
		
		 if (b.containsKey(ReconMODService.FieldName.VERTICAL_VALUE) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.VERTICAL_VALUE) );

		 if (b.containsKey(ReconMODService.FieldName.VERTICAL_ALL_TIME) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.VERTICAL_ALL_TIME) );

		 // now just read them
		 mPreviousVert          = b.getFloat(ReconMODService.FieldName.VERTICAL_PREVIOUS);
		 mVert                  = b.getFloat(ReconMODService.FieldName.VERTICAL_VALUE);
		 mAllTimeVert           = b.getFloat(ReconMODService.FieldName.VERTICAL_ALL_TIME);
	}

	

}
