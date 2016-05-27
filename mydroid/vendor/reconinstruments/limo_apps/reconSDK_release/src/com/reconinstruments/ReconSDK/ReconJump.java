package com.reconinstruments.ReconSDK;

import java.lang.reflect.Method;
import java.util.zip.DataFormatException;

import android.os.Bundle;
import android.util.Log;

/** Concrete Implementation of  {@link ReconEvent#TYPE_JUMP} Object <p>
 * 
 *  ReconJump is an aggregate Type. Jump start is potentially indicated by "FreeFall" Event --
 *  measured accelerometer value drop bellow certain threshold. Jump end is consequently
 *  determined when accelerometer value returns back to normal. Time delta between these events
 *  is time of the Jump, during which MOD Service accumulates statistics exposed by Jump Object <p>
 *  
 *  The Properties of Jump Object are:
 *  <ul>
 *     <li> Sequence ('serial number' -- unique ID)
 *     <li> Timestamp
 *     <li> Air Time (seconds): Time between start and end of FreeFall Event
 *     <li> Distance (meters) : Horizontal Distance achieved during jump Air Time
 *     <li> Drop     (meters) : Vertical Distance between highest and landing point
 *     <li> Height   (meters) : Vertical Distance between starting and highest pont
 *  </ul>
 *  <p>
 *  Occasionally 1 or more data values might not be available; in that case corresponding data field 
 *  will be set to ReconJump.INVALID_XXX value. <p>
 *  
 *  New Jump Events are always broadcasted across the Platform. The SDK Client can be notified by subscribing 
 *  to Jump Events {@link ReconSDKManager#registerListener(IReconEventListener, int)}. <p>
 *  Full Jump History can be retrieved by calling {@link ReconSDKManager#receiveData(IReconDataReceiver, int)}
 *  */
public class ReconJump extends ReconEvent 
{
	private static final String TAG = ReconJump.class.getSimpleName();
	
	// constants
//    public final static int    INVALID_VALUE      = -10000;
    
	/** Invalid Jump Sequence Value */
    public final static int    INVALID_SEQUENCE   = -10000;
    
    /** Invalid Jump Date Value */
    public final static long   INVALID_DATE       = -10000;
    
    /** Invalid Jump Air Time Value */
    public final static int    INVALID_AIR        = -10000;
    
    /** Invalid Jump Distance Value */
    public final static float  INVALID_DISTANCE   = -10000;
    
    /** Invalid Jump Drop Value */
    public final static float  INVALID_DROP       = -10000;
    
    /** Invalid Jump Height Value */
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
    
    /** Jump Sequence Accessor 
     * <p>
     * @return Jump Sequence Identification number
     */
    public int   GetSequence() {return mSequence;}
    
    /** Jump Date Accessor 
     * <p>
     * @return Jump Start Timestamp
     */
    public long  GetDate()     {return mDate;}
    
    /** Jump Air Time 
     * <p>
     * @return Jump Air Time in meters
     */
    public long  GetAir()      {return mAir;}
    
    /** Jump Distance 
     * <p>
     * @return Jump Distance in meters
     */
    public float GetDistance() {return mDistance;}
    
    /** Jump Drop 
     * <p>
     * @return Jump Drop in meters
     */
    public float GetDrop()     {return mDrop;}
    
    /** Jump Height 
     * <p>
     * @return Jump Height in meters
     */
    public float GetHeight()   {return mHeight;}
    
    // identification
    @Override
    public String toString()
    {
    	return ReconJump.TAG;
    }
    
    /** Public Constructor */
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
