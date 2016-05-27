/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 *This class defined a point-of-interest item for rendering in a
 *resort map
 */
package com.reconinstruments.navigation.navigation;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.os.Bundle;
import android.widget.Toast;

import com.reconinstruments.navigation.R;

public class GPSListener implements LocationListener
{
	public interface IGPSLocationUpdater
	{
		void onLocationChanged( double lat, double lng );
		void onBearingChanged( float newBearing );
	}
	
	private double mPrevLat = 0.f; 	//the last  position(latitude, longitude)
	private double mPrevLng = 0.f;	
	private Context mContext = null;
	private MapManager mGPSUpdater=null;
	private float[]  mBearings = null;
	static final int mBearingFilterLen = 20;				//the number of bearings to cached for filtering
	private int mNumBearings = 0;							//the numnber of bearings cached so far
	private int mBearingIdx=0;								//the current bearing index;
	
	public GPSListener( Context  context, MapManager gpsUpdater )
	{
		mContext = context;
		mGPSUpdater = gpsUpdater;
		mBearings = new float[mBearingFilterLen];
	}
	
	/**
     * Implemented LocationListener interfaces
     */
    @Override
    public void onLocationChanged(Location location)
    {

		double lat = location.getLatitude();
		double lng = location.getLongitude();
		
		//maker sure current location is far away enough from previous location
		double dist_in_meter = Util.distanceFromLngLats(lng, lat, mPrevLng, mPrevLat)*1000;
		
		//do not set the cached (lat, lng) and call the cal back 
		//unless the currenct GPS read has certain distance from the previous location
		if( dist_in_meter > Util.GPS_LOCATION_THRESHOLD )
		{
			mPrevLat = lat;
			mPrevLng = lng;	
			
			if( mGPSUpdater != null )
			{
				mGPSUpdater.onLocationChanged(lat, lng);
			}
		}
		else
		{
			//let the location change trigger the resort auto-loading
			//if there is no active resort being assigned to mapmanager yet
			if( mGPSUpdater != null  && mGPSUpdater.mActiveResort == null )
			{
				mGPSUpdater.onLocationChanged(lat, lng);
			}
		}
		
		
		//calcuate the new bearing by averaging the
		//historical bearing informations
		float bearing = location.getBearing();
		mBearings[mBearingIdx] = bearing;
		mBearingIdx = (mBearingIdx + 1)%mBearingFilterLen;
		if( mNumBearings < mBearingFilterLen )
		{
			++mNumBearings;
		}
		
		//average around the bearings
		float sum = 0;
		for( int i = 0; i < mNumBearings; ++i )
		{
			sum += mBearings[ i ];
		}
		
		sum /= mNumBearings;
		
		mGPSUpdater.onBearingChanged( sum );
		
		//Toast.makeText(mContext, "Bearing Updated: " + sum, Toast.LENGTH_SHORT ).show();
    }
    
    @Override 
    public void onProviderDisabled(String provider)
    {
    	//Toast.makeText( mContext.getApplicationContext(), mContext.getResources().getString(R.string.gps_disabled), Toast.LENGTH_SHORT);
    }
    
    @Override
    public void onProviderEnabled(String provider)
    {
    	//Toast.makeText(mContext.getApplicationContext(), mContext.getResources().getString(R.string.gps_enabled), Toast.LENGTH_SHORT);
    }
    
    @Override
    public void onStatusChanged (String provider, int status, Bundle extras)
    {
/*    	
    	switch( status )
    	{
	    	case LocationProvider.OUT_OF_SERVICE:
	    		Toast.makeText(mContext.getApplicationContext(), mContext.getResources().getString(R.string.gps_out_of_service), Toast.LENGTH_SHORT);
	    	break;
	    	
	    	case LocationProvider.AVAILABLE:
	    		Toast.makeText(mContext.getApplicationContext(), mContext.getResources().getString(R.string.gps_out_of_service), Toast.LENGTH_SHORT);
	    	break;
	    	
	    	case LocationProvider.TEMPORARILY_UNAVAILABLE:
	    		Toast.makeText(mContext.getApplicationContext(), mContext.getResources().getString(R.string.gps_unavailable), Toast.LENGTH_SHORT);
    	}
*/    	    	
    }   	
	
}