package com.rxnetworks.rxnservicesxybrid;

import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.HandlerThread;
import android.util.Log;

import com.rxnetworks.xybridcommonlibrary.location.RXN_RefLocation_t;
import com.rxnetworks.xybridcommonlibrary.location.RXN_RefLocation_t.RXN_PositionType;

public class GpsLocationAgent
{
	public interface GpsConsumer
	{
		public void onLocationAvailable(final RXN_RefLocation_t location);
	};
	
	private final CopyOnWriteArrayList<GpsConsumer> mClients;
	private final LocationManager mLocationManager;
	private final HandlerThread mHandlerThread = new HandlerThread("com.rxnetworks.rxnservicesxybrid.handlerthread");

    private static final String TAG = "GpsReceiverWrapper";
    
	private LocationListener mLocationListener = new LocationListener() 
	{
	    public void onLocationChanged(Location location) 
	    {    	
	    	Log.d(TAG, "Getting location fix data from GPS");
	    	
	    	RXN_RefLocation_t rxLocation = null;
	    	
	    	if (location != null && (location.getLatitude() != 0 || location.getLongitude() != 0))
	    	{
		    	rxLocation = new RXN_RefLocation_t();
		    	
		    	rxLocation.setFixPositionType(RXN_PositionType.GPS);
		    	rxLocation.setLat(location.getLatitude());
		    	rxLocation.setLon(location.getLongitude());
		    	rxLocation.setAlt(location.getAltitude());
		    	rxLocation.setUncertainty(location.getAccuracy());
		    	rxLocation.setTime(location.getTime());
	    	}

	    	sendLocationToClients(rxLocation);
	    }
	    
	    public void onStatusChanged(String provider, int status, Bundle extras)	{}
	    public void onProviderEnabled(String provider) {}
	    public void onProviderDisabled(String provider)
	    {
	    	sendLocationToClients(null);
	    }
	};
    
	public GpsLocationAgent(Context context) 
	{
		mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		mClients = new CopyOnWriteArrayList<GpsConsumer>();
		mHandlerThread.start();
	}
	
	public void addObserver(GpsConsumer consumer)
	{
		if (!mClients.contains(consumer))
		{
			mClients.add(consumer);
		}
	}
	
	public void removeObserver(GpsConsumer consumer)
	{
		mClients.remove(consumer);
	}
	
	public void requestGpsFix()
	{
		Log.d(TAG, "Requesting single update");
		mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, mLocationListener, mHandlerThread.getLooper());
	}
	
	private void sendLocationToClients(final RXN_RefLocation_t location)
	{
		new Thread(new Runnable()
		{
			public void run()
			{	
	            for (GpsConsumer client : mClients)
	            {
	            	client.onLocationAvailable(location);
	            }
			}
		}).start();
	}
}
