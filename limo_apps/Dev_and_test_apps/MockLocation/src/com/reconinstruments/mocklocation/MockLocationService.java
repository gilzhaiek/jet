package com.reconinstruments.mocklocation;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;


public class MockLocationService extends Service
{
	private final static String TAG = "MockLocationService";
	private final static String RECON_FAKE_GPS_PROVIDER = "FakeGPS";
	public final static String CHANGE_LOCATION_ACTION = "com.reconinstruments.mocklocationclient.change_location";

	boolean mGPSEnabled	= true;
//	double mLatitude = 49.395556;   // Cypress
//	double mLongitude = -123.203333;
//	double mLatitude = 50.108333;   // Whistler
//	double mLongitude = -122.9425;
	double mLatitude = 49.276793;   // HQ
	double mLongitude = -123.121186;
//	double mLatitude = 51.511742;   // London
//	double mLongitude = -0.12341;
//	double mLatitude = 49.274389; // near HQ, at skytrain
//	double mLongitude = -123.121921;
//	double mLatitude = 33.683165; // Foothill Ranch, California, USA
//	double mLongitude = -117.666437;
	
	protected enum Resorts {
		NONE,
		CYPRESS,
		OUTSIDE_CYPRESS,
		WHISTLER
	}

	
	@Override
    public IBinder onBind (Intent intent)    {
		return null;
    }

    MockLocationProvider mock;
    float counter = 0;

    @Override
    public void onCreate() {
    	super.onCreate();
		Log.i(TAG,"service created");

    	mock = new MockLocationProvider(RECON_FAKE_GPS_PROVIDER, this);
//    	mock = new MockLocationProvider(LocationManager.GPS_PROVIDER, this);

    	//Set test location
    	sendLocation.run();

//    	LocationManager locMgr = (LocationManager)getSystemService(LOCATION_SERVICE);
    	
    }

    @Override
    public int onStartCommand(Intent i, int flag, int startId)    {
    	// Register mMessageReceiver to receive messages.
    	IntentFilter filter = new IntentFilter();
    	filter.addAction(CHANGE_LOCATION_ACTION);
    	filter.addCategory(Intent.CATEGORY_DEFAULT);
    	registerReceiver(mGpsChangeReceiver, filter);
		Log.i(TAG,"broadcast listener registered");
   	
    	return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
    	unregisterReceiver(mGpsChangeReceiver);
    	mock.shutdown();
    	super.onDestroy();

    }

    // handler for received Intents for the "my-event" event 
    private BroadcastReceiver mGpsChangeReceiver = new BroadcastReceiver() {
    	@Override
    	public void onReceive(Context context, Intent intent) {
    		// Extract data included in the Intent
    		Bundle extras = intent.getExtras();
    		if(extras != null) {
    			boolean enableService = extras.getBoolean("GPSstate");
       			if(!mGPSEnabled && enableService) {
    				mock.enableProvider();
    			}
       			if(mGPSEnabled && !enableService) {
    				mock.disableProvider();
    			}
    			mGPSEnabled = enableService;
    			double newLat = extras.getDouble("Latitude");		// change this to be an array of points instead of one point and change code to cycle through array on each call
    			double newLong = extras.getDouble("Longitude");
    			if(newLat != 0. || newLong != 0.) {
    				mLatitude = newLat;
    				mLongitude = newLong;
    			}
    		}  
    		Log.i(TAG,"New location: " + mLatitude + ", " + mLongitude);
    	}
    };
    

    private Handler mockHandler = new Handler();
    
    private final  Runnable sendLocation=new Runnable()	{
	    public void run()   {
	    	if(mGPSEnabled) {
		    	//Log.i("MockLocation", "location: " + mLatitude + ", " + mLongitude);
				mock.pushLocation(mLatitude, mLongitude );
				counter++;
				counter = counter % 5;
				
				mockHandler.postDelayed(sendLocation,1000);	// stage next call
	    	}
	    }
	};


}
