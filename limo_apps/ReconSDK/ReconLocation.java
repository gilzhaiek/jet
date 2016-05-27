package com.reconinstruments.ReconSDK;

import java.lang.reflect.Method;
import java.util.zip.DataFormatException;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;

/* Concrete Implementation of ReconEvent.TYPE_LOCATION Data Type */
public class ReconLocation extends ReconEvent
{
	// constants
	private static final String TAG = ReconLocation.class.getSimpleName();
    public  static final Location INVALID_LOCATION = null; 

    // concrete type data fields
    protected Location mLocation = ReconLocation.INVALID_LOCATION;
    protected Location mPreviousLocation = ReconLocation.INVALID_LOCATION;
    
    // public c-tor
    public ReconLocation ()
    {
    	mType = ReconEvent.TYPE_LOCATION; 
    	BROADCAST_ACTION_STRING = ReconMODService.BroadcastString.BROADCAST_LOCATION_ACTION_STRING;
    }
    
    // public data accessors
    public Location GetLocation() {return mLocation;}
    public Location GetPreviousLocation() {return mPreviousLocation;}
    
    // identification
    @Override
    public String toString()
    {
    	return ReconLocation.TAG;
    }
    
    // IPC Serialization
	@Override
	protected Bundle generateBundle() 
	{
		Bundle b = new Bundle();
		
		b.putParcelable(ReconMODService.FieldName.LOCATION_VALUE, mLocation);
		b.putParcelable(ReconMODService.FieldName.LOCATION_PREVIOUS, mPreviousLocation );
		
		return b;
	}

	@Override
	protected Method changedField(String strName) throws DataFormatException
	{
		 // get changed data. We allow null if BROADCAST_FIELD is not found,
		 // ohterwise it must point to valid bundle key
		 Method m = null;
		 Class<? extends ReconLocation> clazz = this.getClass();

		 try
		 {
		    if (strName.equals(ReconMODService.FieldName.LOCATION_VALUE))
		       m = clazz.getMethod("GetLocation", (Class<?>[])null);
				
		    else if  (strName.equals(ReconMODService.FieldName.LOCATION_PREVIOUS))
			   m = clazz.getMethod("GetPreviousLocation", (Class<?>[])null);

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
		// this one a bit different as Ali has messed up Location handling on Transend server
		// depending on whether device had GPS fix or not
		 mLocation = mPreviousLocation = null;
		 
		 if (b != null)
		 {
			 if (b.containsKey(ReconMODService.FieldName.LOCATION_VALUE) == false)
				 mLocation         = (Location)b.getParcelable(ReconMODService.FieldName.LOCATION_VALUE);
			
			 if (b.containsKey(ReconMODService.FieldName.LOCATION_PREVIOUS) == false)
				 mPreviousLocation = (Location)b.getParcelable(ReconMODService.FieldName.LOCATION_PREVIOUS);

		 }
		 
		 if ( (mLocation == ReconLocation.INVALID_LOCATION) || (mPreviousLocation == ReconLocation.INVALID_LOCATION) )
		 {
			 // transend sends null objects if there is no GPS fix yet. We'll log this
			 // documented SDK feature so that values will be set to INVALID_LOCATION (null)
			 // if there was no GPS fix
			 Log.i(TAG, "GPS Fix not resolved on Transcend Server");
			 
		 }
		
	}

}
