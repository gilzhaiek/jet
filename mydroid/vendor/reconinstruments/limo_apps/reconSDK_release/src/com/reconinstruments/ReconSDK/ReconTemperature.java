package com.reconinstruments.ReconSDK;

import java.lang.reflect.Method;
import java.util.zip.DataFormatException;
import android.os.Bundle;
import android.util.Log;

/** Concrete Implementation of {@link ReconEvent#TYPE_TEMPERATURE} Data Type <p>
 *  ReconTemperature Data Object encapsulates measured ambient Temperature. MOD
 *  Server keep track of current temperature and records Min/Max values. 
 *  All data is reported in degrees Celsius.
 *  <p>
 *  SDK Client can be notified by subscribing 
 *  to Temperature Events {@link ReconSDKManager#registerListener(IReconEventListener, int)}. 
 *  Only minimum/maximum values will be broadcast, but "ordinary" temperature changes that
 *  do not set record will not.
 *  */
public class ReconTemperature extends ReconEvent 
{
	// constants
	private static final String TAG = ReconTemperature.class.getSimpleName();
	
	/** Invalid Temperature Value (-1 Kelvin!) */
    public  static final int INVALID_TEMPERATURE = -274; // -1 K!

    // concrete type data fields
    protected int mMaxTemperature        = ReconTemperature.INVALID_TEMPERATURE;
    protected int mMinTemperature        = ReconTemperature.INVALID_TEMPERATURE;
    protected int mTemperature           = ReconTemperature.INVALID_TEMPERATURE;
    protected int mAllTimeMaxTemperature = ReconTemperature.INVALID_TEMPERATURE;
    protected int mAllTimeMinTemperature = ReconTemperature.INVALID_TEMPERATURE;
    
    /** Public Constructor */
    public ReconTemperature ()
    {
    	mType = ReconEvent.TYPE_TEMPERATURE; 
    	BROADCAST_ACTION_STRING = ReconMODService.BroadcastString.BROADCAST_TEMPERATURE_ACTION_STRING;
    }
    
    // public data accessors
    
    /** Maximum Temperature Accessor
     * <p>
     * @return Maximum Recorded Temperature
     */
    public    int GetMaxTemperature()        {return mMaxTemperature;}
    
    /** Minimum Temperature Accessor
     * <p>
     * @return Minimum Recorded Temperature
     */
    public    int GetMinTemperature()        {return mMinTemperature;}
    
    /** Current Temprature Accessor
     * <p>
     * @return Current Temperature
     */
    public    int GetTemperature   ()        {return mTemperature;}
    
    /** All Time Maximum Temperature Accessor
     * <p>
     * @return All Time Maximum Temperature
     */
    public    int GetAllTimeMaxTemperature() {return mAllTimeMaxTemperature;}
    
    /** All Time Minimum Temperature Accessor
     * <p>
     * @return All Time Minimum Temperature
     */
    public    int GetAllTimeMinTemperature() {return mAllTimeMinTemperature;}
    
    // identification
    @Override
    public String toString()
    {
    	return ReconTemperature.TAG;
    }
    
    // IPC Serialization
	@Override
	protected Bundle generateBundle() 
	{
		Bundle b = new Bundle();
		
		b.putInt(ReconMODService.FieldName.TEMP_MAX,          mMaxTemperature);
		b.putInt(ReconMODService.FieldName.TEMP_MIN,          mMinTemperature);
		b.putInt(ReconMODService.FieldName.TEMP_MAX_ALL_TIME, mAllTimeMaxTemperature);
		b.putInt(ReconMODService.FieldName.TEMP_MIN_ALL_TIME, mAllTimeMinTemperature);
		b.putInt(ReconMODService.FieldName.TEMP_VALUE,        mTemperature); 
		
		return b;
	}

	@Override
	protected Method changedField(String strName) throws DataFormatException
	{
		 // get changed data. We allow null if BROADCAST_FIELD is not found,
		 // ohterwise it must point to valid bundle key
		 Method m = null;
		 Class<? extends ReconTemperature> clazz = this.getClass();
		
		 try
		 {
			  if (strName.equals(ReconMODService.FieldName.TEMP_MAX))
			     m = clazz.getMethod("GetMaxTemperature", (Class<?>[])null);
				
			  else if  (strName.equals(ReconMODService.FieldName.TEMP_MIN))
				 m = clazz.getMethod("GetMinTemperature", (Class<?>[])null);
				
			  else if (strName.equals(ReconMODService.FieldName.TEMP_MAX_ALL_TIME))
				 m = clazz.getMethod("GetAllTimeMaxTemperature", (Class<?>[])null);
				
			  else if (strName.equals(ReconMODService.FieldName.TEMP_MIN_ALL_TIME))
				 m = clazz.getMethod("GetAllTimeMinTemperature", (Class<?>[])null);
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
		 if (b.containsKey(ReconMODService.FieldName.TEMP_MAX) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.TEMP_MAX) );
		
		 if (b.containsKey(ReconMODService.FieldName.TEMP_MIN) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.TEMP_MIN) );

		 if (b.containsKey(ReconMODService.FieldName.TEMP_MAX_ALL_TIME) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.TEMP_MAX_ALL_TIME) );

		 if (b.containsKey(ReconMODService.FieldName.TEMP_MIN_ALL_TIME) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.TEMP_MIN_ALL_TIME) );

		 if (b.containsKey(ReconMODService.FieldName.TEMP_VALUE) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.TEMP_VALUE) );

		 // now just read them
		 mMaxTemperature        = b.getInt(ReconMODService.FieldName.TEMP_MAX);
		 mMinTemperature        = b.getInt(ReconMODService.FieldName.TEMP_MIN);
		 mAllTimeMaxTemperature = b.getInt(ReconMODService.FieldName.TEMP_MAX_ALL_TIME);
		 mAllTimeMinTemperature = b.getInt(ReconMODService.FieldName.TEMP_MIN_ALL_TIME);
		 mTemperature           = b.getInt(ReconMODService.FieldName.TEMP_VALUE);
		
	}

}
