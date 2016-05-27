package com.reconinstruments.ReconSDK;

import java.lang.reflect.Method;
import java.util.zip.DataFormatException;

import android.os.Bundle;
import android.util.Log;

/** Concrete Implementation of {@link ReconEvent#TYPE_TIME} Data Type <p>
 * 
 *  ReconTime Data Object encapsulates Time on MOD Live Platform. <p>
 *  Time Events are not broadcasted by MOD Server. App Clients must explicitly
 *  request an update {@link ReconSDKManager#receiveData(IReconDataReceiver, int)}
 *  */
public class ReconTime extends ReconEvent 
{
	// constants
	private static final String TAG = ReconTime.class.getSimpleName();
	
	/** Invalid Time Value */
    public  static final long INVALID_TIME = -1; 

    // concrete type data fields
	protected long mLastUpdate      = ReconTime.INVALID_TIME;         // Time in miliseconds
	protected long mTheUpdateBefore = ReconTime.INVALID_TIME;         // Time in milisecods
	protected long mUTCTimems       = ReconTime.INVALID_TIME;
	
	/** Public Constructor */
    public ReconTime ()
    {
    	mType = ReconEvent.TYPE_TIME; 
    	BROADCAST_ACTION_STRING = ReconMODService.BroadcastString.BROADCAST_TIME_ACTION_STRING;
    }
    
    // public data accessors
    
    /** Last Update Accessor 
     *  <p>
     * @return Time of last Update
     */
    public long  GetLastUpdate()    {return mLastUpdate;}
    
    /** Update Before Accessor
     * <p>
     * @return Time of previous Update
     */
    public long  GetUpdateBefore()  {return mTheUpdateBefore;}
    
    /** Current UTC Time Accessor (miliseconds) 
     * <p>
     * @return Current UTC Time
     */
    public long  GetUTCTime()       {return mUTCTimems;}
    
    // identification
    @Override
    public String toString()
    {
    	return ReconTime.TAG;
    }
    
    // IPC Serialization
	@Override
	protected Bundle generateBundle() 
	{
		Bundle b = new Bundle();
		
		b.putLong(ReconMODService.FieldName.TIME_LAST_UPDATE,     mLastUpdate);
		b.putLong(ReconMODService.FieldName.TIME_UPDATE_BEFORE,   mTheUpdateBefore);
		b.putLong(ReconMODService.FieldName.TIME_UTC,             mUTCTimems);
		
		return b;
	}

	@Override
	protected Method changedField(String strName) throws DataFormatException
	{
		 // get changed data. We allow null if BROADCAST_FIELD is not found,
		 // ohterwise it must point to valid bundle key
		 Method m = null;
		 Class<? extends ReconTime> clazz = this.getClass();
	
		 try
		 {
			 if (strName.equals(ReconMODService.FieldName.TIME_UTC))
				 m = clazz.getMethod("GetUTCTime", (Class<?>[])null);
				
			  else if  (strName.equals(ReconMODService.FieldName.TIME_LAST_UPDATE))
			     m = clazz.getMethod("GetLastUpdate", (Class<?>[])null);
				
			  else if (strName.equals(ReconMODService.FieldName.TIME_UPDATE_BEFORE))
				 m = clazz.getMethod("GetUpdateBefore", (Class<?>[])null);
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
		 if (b.containsKey(ReconMODService.FieldName.TIME_UTC) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.TIME_UTC) );

		 if (b.containsKey(ReconMODService.FieldName.TIME_UPDATE_BEFORE) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.TIME_UPDATE_BEFORE) );

		 if (b.containsKey(ReconMODService.FieldName.TIME_LAST_UPDATE) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.TIME_LAST_UPDATE) );

		 // now just read them
		 mUTCTimems           = b.getLong(ReconMODService.FieldName.TIME_UTC);
		 mLastUpdate          = b.getLong(ReconMODService.FieldName.TIME_LAST_UPDATE);
		 mTheUpdateBefore     = b.getLong(ReconMODService.FieldName.TIME_UPDATE_BEFORE);
		 
	}

}
