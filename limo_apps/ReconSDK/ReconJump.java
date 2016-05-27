package com.reconinstruments.ReconSDK;

import java.lang.reflect.Method;
import java.util.zip.DataFormatException;

import android.os.Bundle;
import android.util.Log;

/* Concrete Implementation of ReconEvent.TYPE_JUMP Data Type */
public class ReconJump extends ReconEvent 
{
	private static final String TAG = ReconJump.class.getSimpleName();
	
	// constants
    public final static int    INVALID_VALUE      = -10000;
    
    public final static int    INVALID_SEQUENCE   = -10000;
    public final static long   INVALID_DATE       = -10000;
    public final static int    INVALID_AIR        = -10000;
    public final static float  INVALID_DISTANCE   = -10000;
    public final static float  INVALID_DROP       = -10000;
    public final static float  INVALID_HEIGHT     = -10000;
    
    // concrete type data fields
    // jump properties (making protected, don't want to expose outside package
    protected int     mSequence = ReconJump.INVALID_SEQUENCE;
    protected long    mDate     = ReconJump.INVALID_DATE;
    protected long    mAir      = ReconJump.INVALID_AIR;
    protected float   mDistance = ReconJump.INVALID_DISTANCE;
    protected float   mDrop     = ReconJump.INVALID_DROP;
    protected float   mHeight   = ReconJump.INVALID_HEIGHT;

    /* Public Accessors */
    public int   GetSequence() {return mSequence;}
    public long  GetDate()     {return mDate;}
    public long  GetAir()      {return mAir;}
    public float GetDistance() {return mDistance;}
    public float GetDrop()     {return mDrop;}
    public float GetHeight()   {return mHeight;}
    
    // identification
    @Override
    public String toString()
    {
    	return ReconJump.TAG;
    }
    
    // public constructor
    public ReconJump() 
    {
    	mType = ReconEvent.TYPE_JUMP;
    	BROADCAST_ACTION_STRING = ReconMODService.BroadcastString.BROADCAST_JUMP_ACTION_STRING;
    }
    
  
	@Override
	protected Bundle generateBundle() 
	{
		Bundle b = new Bundle();
		
		b.putInt   (ReconMODService.FieldName.JUMP_SEQUENCE,    mSequence);
		b.putLong  (ReconMODService.FieldName.JUMP_DATE,        mDate);
		b.putLong  (ReconMODService.FieldName.JUMP_AIR,         mAir);
		b.putFloat (ReconMODService.FieldName.JUMP_DISTANCE,    mDistance);
		b.putFloat (ReconMODService.FieldName.JUMP_DROP,        mDrop);
		b.putFloat (ReconMODService.FieldName.JUMP_HEIGHT,      mHeight);
		
		return b;
	}

	@Override
	protected Method changedField(String strName) throws DataFormatException
	{
		 // get changed data. We allow null if BROADCAST_FIELD is not found,
		 // ohterwise it must point to valid bundle key
		 Method m = null;
		 Class<? extends ReconJump> clazz = this.getClass();

		 try
		 {
			 if (strName.equals(ReconMODService.FieldName.JUMP_SEQUENCE))
			    m = clazz.getMethod("GetSequence", (Class<?>[])null);
				
			 else if  (strName.equals(ReconMODService.FieldName.JUMP_DATE))
				m = clazz.getMethod("GetDate", (Class<?>[])null);
				
			 else if (strName.equals(ReconMODService.FieldName.JUMP_AIR))
				m = clazz.getMethod("GetAir", (Class<?>[])null);
				
			 else if (strName.equals(ReconMODService.FieldName.JUMP_DISTANCE))
				m = clazz.getMethod("GetDistance", (Class<?>[])null);
				
			 else if (strName.equals(ReconMODService.FieldName.JUMP_DROP))
				m = clazz.getMethod("GetDrop", (Class<?>[])null);
				
			 else if (strName.equals(ReconMODService.FieldName.JUMP_HEIGHT))
				m = clazz.getMethod("GetHeight", (Class<?>[])null);
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
	     if (b.containsKey(ReconMODService.FieldName.JUMP_SEQUENCE) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.JUMP_SEQUENCE) );
	
		 if (b.containsKey(ReconMODService.FieldName.JUMP_DATE) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.JUMP_DATE) );

		 if (b.containsKey(ReconMODService.FieldName.JUMP_AIR) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.JUMP_AIR) );

		 if (b.containsKey(ReconMODService.FieldName.JUMP_DISTANCE) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.JUMP_DISTANCE) );

		 if (b.containsKey(ReconMODService.FieldName.JUMP_DROP) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.JUMP_DROP) );

		 if (b.containsKey(ReconMODService.FieldName.JUMP_HEIGHT) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.JUMP_HEIGHT) );

		 
		 // now just read them
		 mSequence   = b.getInt   (ReconMODService.FieldName.JUMP_SEQUENCE);
		 mDate       = b.getLong  (ReconMODService.FieldName.JUMP_DATE);
		 mAir        = b.getLong  (ReconMODService.FieldName.JUMP_AIR);
	     mDistance   = b.getFloat (ReconMODService.FieldName.JUMP_DISTANCE);
	     mDrop       = b.getFloat (ReconMODService.FieldName.JUMP_DROP);
	     mHeight     = b.getFloat (ReconMODService.FieldName.JUMP_HEIGHT);
	     
	}

}
