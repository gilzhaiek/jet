package com.reconinstruments.ReconSDK;

import java.lang.reflect.Method;
import java.util.zip.DataFormatException;

import android.os.Bundle;
import android.util.Log;

/** Concrete Implementation of {@link ReconEvent#TYPE_ALTITUDE} Data Type 
 * 
 *  ReconAltitude is one of the most important Data Object on MOD Live Platform.
 *  Values are derived based on both GPS and Pressure data. Client code thus has access to:
 *  <p>
 *  <ul>
 *     <li> GPS Based only Altitude
 *     <li> Barometric based only Altitude
 *     <li> "Derived" (Composite) - based on both GPS and Barometric Values
 *  </ul>
 *  <p>
 *  Altitude Record values (maximum, minimum, all-time) are always broadcast by Transend Server. 
 *  SDK Client can be notified by subscribing 
 *  to Altitude Events {@link ReconSDKManager#registerListener(IReconEventListener, int)} <p>
 *  Full Data Results bundle can be retrieved on demand by calling {@link ReconSDKManager#receiveData(IReconDataReceiver, int)}
 * */
public class ReconAltitude extends ReconEvent
{
	// constants
	private static final String TAG = ReconAltitude.class.getSimpleName();
	
	/** Invalid Altitude Value */
    public static  final int    INVALID_ALTITUDE = -10000;
    
    /** Invalid Pressure Value */
    public static  final float  INVALID_PRESSURE = 0;
    
    /** GPS Derived Altitude Data Structure */
    public final class GPSData
    {
    	 protected float mGpsAlt         = ReconAltitude.INVALID_ALTITUDE;
    	 protected float mPreviousGpsAlt = ReconAltitude.INVALID_ALTITUDE;
    	 
    	 // accessors
    	 
    	 /** GPS Altitude Accessor
           * <p>
           * @return GPS based Altitude
           */
    	 public float    GetAltitude()         {return mGpsAlt;}
    	 
      	 /** GPS previous Altitude Accessor
          * <p>
          * @return GPS based previous Altitude
          */
    	 public float    GetPreviousAltitude() {return mPreviousGpsAlt;}
    }
    
    /** Pressure Derived Altitude Data Structure */
    public final class PressureData
    {
    	 // Pressure value itself: in mbar
    	 protected float mPressure = ReconAltitude.INVALID_PRESSURE; 
    	 
    	 protected float mPressureAlt            = ReconAltitude.INVALID_ALTITUDE;
    	 protected float mMaxPressureAlt         = ReconAltitude.INVALID_ALTITUDE;
    	 protected float mMinPressureAlt         = -ReconAltitude.INVALID_ALTITUDE;
    	 
    	 protected float mPreviousPressureAlt    = ReconAltitude.INVALID_ALTITUDE;
    	 protected float mPreviousMaxPressureAlt = ReconAltitude.INVALID_ALTITUDE;
    	 protected float mPreviousMinPressureAlt = -ReconAltitude.INVALID_ALTITUDE;

    	 // accessors
    	 
       	 /** Pressure Accessor
          * <p>
          * @return Barometric Pressure in milibars
          */
    	 public float    GetPressure() {return mPressure;}
    	 
       	 /** Pressure based Altitude Accessor
          * <p>
          * @return Pressure based Altitude
          */
    	 public float    GetAltitude()    {return mPressureAlt;}
    	 
       	 /** Pressure based maximum Altitude Accessor
          * <p>
          * @return Pressure based maximum Altitude
          */
    	 public float    GetMaxAltitude() {return mMaxPressureAlt;}
    	 
      	 /** Pressure based minimum Altitude Accessor
          * <p>
          * @return Pressure based minimum Altitude
          */
    	 public float    GetMinAltitude() {return mMinPressureAlt;}
    	 
      	 /** Pressure based previous Altitude Accessor
          * <p>
          * @return Pressure based previous Altitude
          */
    	 public float    GetPreviousAltitude   () {return mPreviousPressureAlt;}
    	 
      	 /** Pressure based previous maximum Altitude Accessor
          * <p>
          * @return Pressure based previous maximum Altitude
          */
    	 public float    GetPreviousMaxAltitude() {return mPreviousMaxPressureAlt;}
    	 
      	 /** Pressure based previous minimum Altitude Accessor
          * <p>
          * @return Pressure based previous minimum Altitude
          */
    	 public float    GetPreviousMinAltitude() {return mPreviousMinPressureAlt;}
    }
    

    // concrete type data fields
    protected float mAlt            = ReconAltitude.INVALID_ALTITUDE;
    protected float mMaxAlt         = ReconAltitude.INVALID_ALTITUDE;
    protected float mMinAlt         = -ReconAltitude.INVALID_ALTITUDE;
    protected float mPreviousAlt    = ReconAltitude.INVALID_ALTITUDE;
    protected float mPreviousMaxAlt = ReconAltitude.INVALID_ALTITUDE;
    protected float mPreviousMinAlt = -ReconAltitude.INVALID_ALTITUDE;
    protected float mAllTimeMaxAlt  = ReconAltitude.INVALID_ALTITUDE;
    protected float mAllTimeMinAlt  = ReconAltitude.INVALID_ALTITUDE; 

    protected int     mHeightOffsetN  = 0;     // number of data points used for callibrating barometer using GPS altitude
    protected boolean mIsCallibrating = true;
    protected boolean mIsInitialized  = false;

	protected GPSData      mGPSData      = new GPSData();
    protected PressureData mPressureData = new PressureData();
	
 
    // accessors
    
	/** Derived Altitude Accessor
     * <p>
     * @return Derived Altitude value
     */
    public float   GetAltitude()             {return mAlt;}
    
	/** Maximum Derived Altitude Accessor
     * <p>
     * @return Maximum Derived Altitude
     */
    public float   GetMaxAltitude()          {return mMaxAlt;}
    
	/** Minimum Derived Altitude Accessor
     * <p>
     * @return Minimum Derived Altitude
     */
    public float   GetMinAltitude()          {return mMinAlt;}
    
	/** Previous Derived Altitude Accessor
     * <p>
     * @return Previous Derived Altitude
     */
    public float   GetPreviousAltitude()     {return mPreviousAlt;}
    
	/** Maximum Previous Derived Altitude Accessor
     * <p>
     * @return Maximum Previous Derived Altitude
     */
    public float   GetPreviousMaxAltitude()  {return mPreviousMaxAlt;}
    
	/** Minimum Previous Derived Altitude Accessor
     * <p>
     * @return Minimum Previous Derived Altitude
     */
    public float   GetPreviousMinAltitude()  {return mPreviousMinAlt;}
    
	/** All-time Maximum Derived Altitude Accessor
     * <p>
     * @return All-time Maximum Derived Altitude
     */
    public float   GetAllTimeMaxAltitude()   {return mAllTimeMaxAlt;}
    
	/** All-time Minimum Derived Altitude Accessor
     * <p>
     * @return All-time Minimum Derived Altitude
     */
    public float   GetAllTimeMinAltitude()   {return mAllTimeMinAlt;}
    
    /** Internal Calibration Flag Accessor
     * <p>
     * @return boolean flag indicating if Altitude measurement system is currently calibrating 
     */
    public boolean IsCallibrating()  {return mIsCallibrating;}
    
    /** Internal Initialized Flag Accessor
     * <p>
     * @return boolean flag indicating if Altitude measurement system has been successfully initialized 
     */
    public boolean IsInitialized ()  {return mIsInitialized;}
    
    /** Height Offset Accessor
     * <p>
     * @return number of data points used for callibrating barometer using GPS altitude
     */
    public int     GetHeightOffset() {return mHeightOffsetN;}
    
    /** GPS aggregate Object Accessor 
     * <p>
     * @return GPS aggregate object
     */
    public GPSData      GetGPSAltitude()       {return mGPSData;}
    
    /** Pressure aggregate Object Accessor 
     * <p>
     * @return PressureData aggregate object
     */
    public PressureData GetPressureAltitude()  {return mPressureData;}
    
    // identification
    @Override
    public String toString()
    {
    	return ReconAltitude.TAG;
    }
    
    /** Public Constructor */
    public ReconAltitude ()
    {
    	mType = ReconEvent.TYPE_ALTITUDE;
    	BROADCAST_ACTION_STRING = ReconMODService.BroadcastString.BROADCAST_ALTITUDE_ACTION_STRING;
    }

	@Override
	protected Bundle generateBundle() 
	{
		Bundle b = new Bundle();
		
		b.putFloat(ReconMODService.FieldName.ALTITUDE_VALUE,          mAlt);
		b.putFloat(ReconMODService.FieldName.ALTITUDE_MAX,            mMaxAlt);
		b.putFloat(ReconMODService.FieldName.ALTITUDE_MIN,            mMinAlt);
		
		b.putFloat(ReconMODService.FieldName.ALTITUDE_PREV,           mPreviousAlt);
		b.putFloat(ReconMODService.FieldName.ALTITUDE_PREV_MAX,       mPreviousMaxAlt);
		b.putFloat(ReconMODService.FieldName.ALTITUDE_PREV_MIN,       mPreviousMinAlt);
		
		b.putFloat(ReconMODService.FieldName.ALTITUDE_ALL_TIME_MAX,   mAllTimeMaxAlt);
		b.putFloat(ReconMODService.FieldName.ALTITUDE_ALL_TIME_MIN,   mAllTimeMinAlt);
		
		b.putFloat(ReconMODService.FieldName.ALTITUDE_PRESSURE,          mPressureData.mPressure);
		
		b.putFloat(ReconMODService.FieldName.ALTITUDE_PRESSURE_VALUE,    mPressureData.mPressureAlt);
		b.putFloat(ReconMODService.FieldName.ALTITUDE_PRESSURE_PREV,     mPressureData.mPreviousPressureAlt);
		b.putFloat(ReconMODService.FieldName.ALTITUDE_PRESSURE_MAX,      mPressureData.mMaxPressureAlt);
		b.putFloat(ReconMODService.FieldName.ALTITUDE_PRESSURE_MIN,      mPressureData.mMinPressureAlt);
		b.putFloat(ReconMODService.FieldName.ALTITUDE_PRESSURE_PREV_MAX, mPressureData.mPreviousMaxPressureAlt);
		b.putFloat(ReconMODService.FieldName.ALTITUDE_PRESSURE_PREV_MIN, mPressureData.mPreviousMinPressureAlt);
		
		
		b.putFloat(ReconMODService.FieldName.ALTITUDE_GPS_VALUE, mGPSData.mGpsAlt);
		b.putFloat(ReconMODService.FieldName.ALTITUDE_GPS_PREV,  mGPSData.mPreviousGpsAlt);
		
		b.putInt     (ReconMODService.FieldName.ALTITUDE_HEIGHT_OFFSET,   mHeightOffsetN);
		b.putBoolean (ReconMODService.FieldName.ALTITUDE_IS_CALLIBRATING, mIsCallibrating);
		b.putBoolean (ReconMODService.FieldName.ALTITUDE_IS_INITIALIZED,  mIsInitialized);
		
			
		return b;
	}

	@Override
	protected Method changedField(String strName) throws DataFormatException
	{
		 // get changed data. We allow null if BROADCAST_FIELD is not found,
		 // ohterwise it must point to valid bundle key
		 // For Altitude
		 Method m = null;
		 Class<? extends ReconAltitude> clazz = this.getClass();
		
		 try
		 {
		    if (strName.equals(ReconMODService.FieldName.ALTITUDE_VALUE))
		       m = clazz.getMethod("GetAltitude", (Class<?>[])null);
			
		    else if (strName.equals(ReconMODService.FieldName.ALTITUDE_MAX))
			   m = clazz.getMethod("GetMaxAltitude", (Class<?>[])null);
		    
		    else if (strName.equals(ReconMODService.FieldName.ALTITUDE_MIN))
			   m = clazz.getMethod("GetMinAltitude", (Class<?>[])null);
		    
		    else if (strName.equals(ReconMODService.FieldName.ALTITUDE_PREV))
			   m = clazz.getMethod("GetPreviousAltitude", (Class<?>[])null);
		    
		    else if (strName.equals(ReconMODService.FieldName.ALTITUDE_PREV_MAX))
			   m = clazz.getMethod("GetPreviousMaxAltitude", (Class<?>[])null);
		    
		    else if (strName.equals(ReconMODService.FieldName.ALTITUDE_PREV_MIN))
			   m = clazz.getMethod("GetPreviousMinAltitude", (Class<?>[])null);
		    
		    else if (strName.equals(ReconMODService.FieldName.ALTITUDE_ALL_TIME_MAX))
			   m = clazz.getMethod("GetAllTimeMaxAltitude", (Class<?>[])null);
	
		    else if (strName.equals(ReconMODService.FieldName.ALTITUDE_ALL_TIME_MIN))
			   m = clazz.getMethod("GetAllTimeMinAltitude", (Class<?>[])null);
		    
		    else if (strName.equals(ReconMODService.FieldName.ALTITUDE_HEIGHT_OFFSET) )
		    	m = clazz.getMethod("GetHeightOffset", (Class<?>[])null);
		    
		    else if (strName.equals(ReconMODService.FieldName.ALTITUDE_IS_CALLIBRATING))
			   m = clazz.getMethod("IsCallibrating", (Class<?>[])null);
		    
		    else if (strName.equals(ReconMODService.FieldName.ALTITUDE_IS_INITIALIZED))
			   m = clazz.getMethod("IsInitialized", (Class<?>[])null);
		    
		    else if (strName.equals(ReconMODService.FieldName.ALTITUDE_PRESSURE))
			   m = clazz.getMethod("GetPressureAltitude", (Class<?>[])null);
		    
		    else if (strName.equals(ReconMODService.FieldName.ALTITUDE_PRESSURE_VALUE))
			   m = clazz.getMethod("GetPressureAltitude", (Class<?>[])null);
		    
		    else if (strName.equals(ReconMODService.FieldName.ALTITUDE_PRESSURE_MAX))
			   m = clazz.getMethod("GetPressureAltitude", (Class<?>[])null);
		    
		    else if (strName.equals(ReconMODService.FieldName.ALTITUDE_PRESSURE_MIN))
			   m = clazz.getMethod("GetPressureAltitude", (Class<?>[])null);
		    
		    else if (strName.equals(ReconMODService.FieldName.ALTITUDE_PRESSURE_PREV))
			   m = clazz.getMethod("GetPressureAltitude", (Class<?>[])null);
		    
		    else if (strName.equals(ReconMODService.FieldName.ALTITUDE_PRESSURE_PREV_MAX))
			   m = clazz.getMethod("GetPressureAltitude", (Class<?>[])null);
		    
		    else if (strName.equals(ReconMODService.FieldName.ALTITUDE_PRESSURE_PREV_MIN))
			   m = clazz.getMethod("GetPressureAltitude", (Class<?>[])null);
		    
		    else if (strName.equals(ReconMODService.FieldName.ALTITUDE_GPS_VALUE))
			   m = clazz.getMethod("GetGPSAltitude", (Class<?>[])null);
		    
		    else if (strName.equals(ReconMODService.FieldName.ALTITUDE_GPS_PREV))
			   m = clazz.getMethod("GetGPSAltitude", (Class<?>[])null);

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
		 if (b.containsKey(ReconMODService.FieldName.ALTITUDE_VALUE) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.ALTITUDE_VALUE) );
	
		 if (b.containsKey(ReconMODService.FieldName.ALTITUDE_MAX) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.ALTITUDE_MAX) );
		 
		 if (b.containsKey(ReconMODService.FieldName.ALTITUDE_MIN) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.ALTITUDE_MIN) );
		 
		 if (b.containsKey(ReconMODService.FieldName.ALTITUDE_PREV) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.ALTITUDE_PREV) );
		 
		 if (b.containsKey(ReconMODService.FieldName.ALTITUDE_PREV_MAX) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.ALTITUDE_PREV_MAX) );
		 
		 if (b.containsKey(ReconMODService.FieldName.ALTITUDE_PREV_MIN) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.ALTITUDE_PREV_MIN) );
		 
		 if (b.containsKey(ReconMODService.FieldName.ALTITUDE_ALL_TIME_MAX) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.ALTITUDE_ALL_TIME_MAX) );
		 
		 if (b.containsKey(ReconMODService.FieldName.ALTITUDE_ALL_TIME_MIN) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.ALTITUDE_ALL_TIME_MIN) );

		 if (b.containsKey(ReconMODService.FieldName.ALTITUDE_IS_CALLIBRATING) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.ALTITUDE_IS_CALLIBRATING) );

		 if (b.containsKey(ReconMODService.FieldName.ALTITUDE_IS_INITIALIZED) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.ALTITUDE_IS_INITIALIZED) );

		 if (b.containsKey(ReconMODService.FieldName.ALTITUDE_HEIGHT_OFFSET) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.ALTITUDE_HEIGHT_OFFSET) );

		 if (b.containsKey(ReconMODService.FieldName.ALTITUDE_GPS_VALUE) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.ALTITUDE_GPS_VALUE) );

		 if (b.containsKey(ReconMODService.FieldName.ALTITUDE_GPS_PREV) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.ALTITUDE_GPS_PREV) );
		 
		 if (b.containsKey(ReconMODService.FieldName.ALTITUDE_PRESSURE) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.ALTITUDE_PRESSURE) );

		 if (b.containsKey(ReconMODService.FieldName.ALTITUDE_PRESSURE_VALUE) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.ALTITUDE_PRESSURE_VALUE) );
		 
		 if (b.containsKey(ReconMODService.FieldName.ALTITUDE_PRESSURE_PREV) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.ALTITUDE_PRESSURE_PREV) );
	
		 if (b.containsKey(ReconMODService.FieldName.ALTITUDE_PRESSURE_MAX) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.ALTITUDE_PRESSURE_MAX) );

		 if (b.containsKey(ReconMODService.FieldName.ALTITUDE_PRESSURE_MIN) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.ALTITUDE_PRESSURE_MIN) );
		 
		 if (b.containsKey(ReconMODService.FieldName.ALTITUDE_PRESSURE_PREV_MAX) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.ALTITUDE_PRESSURE_PREV_MAX) );
		 
		 if (b.containsKey(ReconMODService.FieldName.ALTITUDE_PRESSURE_PREV_MIN) == false)
			 throw new DataFormatException(String.format("%s: Field %s not found", TAG, ReconMODService.FieldName.ALTITUDE_PRESSURE_PREV_MIN) );

		 
		 // now just read them
		 mAlt             = b.getFloat   (ReconMODService.FieldName.ALTITUDE_VALUE);
		 mMaxAlt          = b.getFloat   (ReconMODService.FieldName.ALTITUDE_MAX);
		 mMinAlt          = b.getFloat   (ReconMODService.FieldName.ALTITUDE_MIN);
		 mPreviousAlt     = b.getFloat   (ReconMODService.FieldName.ALTITUDE_PREV);
		 mPreviousMaxAlt  = b.getFloat   (ReconMODService.FieldName.ALTITUDE_PREV_MAX);
		 mPreviousMinAlt  = b.getFloat   (ReconMODService.FieldName.ALTITUDE_PREV_MIN);
		 mAllTimeMaxAlt   = b.getFloat   (ReconMODService.FieldName.ALTITUDE_ALL_TIME_MAX);
		 mAllTimeMinAlt   = b.getFloat   (ReconMODService.FieldName.ALTITUDE_ALL_TIME_MIN);
		 mIsCallibrating  = b.getBoolean (ReconMODService.FieldName.ALTITUDE_IS_CALLIBRATING);
		 mIsInitialized   = b.getBoolean (ReconMODService.FieldName.ALTITUDE_IS_INITIALIZED);
		 mHeightOffsetN   = b.getInt     (ReconMODService.FieldName.ALTITUDE_HEIGHT_OFFSET);
	
		 mGPSData.mGpsAlt         = b.getFloat (ReconMODService.FieldName.ALTITUDE_GPS_VALUE);
		 mGPSData.mPreviousGpsAlt = b.getFloat (ReconMODService.FieldName.ALTITUDE_GPS_PREV);
		 
		 mPressureData.mPressure                = b.getFloat (ReconMODService.FieldName.ALTITUDE_PRESSURE);
		 mPressureData.mPressureAlt             = b.getFloat (ReconMODService.FieldName.ALTITUDE_PRESSURE_VALUE);
		 mPressureData.mPreviousPressureAlt     = b.getFloat (ReconMODService.FieldName.ALTITUDE_PRESSURE_PREV);
		 mPressureData.mMaxPressureAlt          = b.getFloat (ReconMODService.FieldName.ALTITUDE_PRESSURE_MAX);
		 mPressureData.mMinPressureAlt          = b.getFloat (ReconMODService.FieldName.ALTITUDE_PRESSURE_MIN);
		 mPressureData.mPreviousMaxPressureAlt  = b.getFloat (ReconMODService.FieldName.ALTITUDE_PRESSURE_PREV_MAX);
		 mPressureData.mPreviousMinPressureAlt  = b.getFloat (ReconMODService.FieldName.ALTITUDE_PRESSURE_PREV_MIN);

	
	}

}
