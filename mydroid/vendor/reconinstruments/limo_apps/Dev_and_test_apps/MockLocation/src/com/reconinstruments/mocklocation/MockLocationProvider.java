package com.reconinstruments.mocklocation;
import android.content.Context;
import android.content.Intent;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.util.Log;


public class MockLocationProvider {
	public final static String FAKE_LOCATION_PROVIDER_REGISTERED = "com.reconinstruments.mocklocation.fake_location_registered";
	public final static String FAKE_LOCATION_PROVIDER_UNREGISTERED = "com.reconinstruments.mocklocation.fake_location_unregistered";
  String providerName;
  Context ctx;
  boolean mRegistered=false;

  double deltaLatitude = 0;
  double deltaLongitude = 0;

  float counter = 0;
 
  public MockLocationProvider(String name, Context ctx) {
    this.providerName = name;
    this.ctx = ctx;
    
 
    registerProvider();
  }
  
  public void  registerProvider() {
	  
	LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
	try{ 
		lm.removeTestProvider(providerName);
	} 
	catch (Exception e) {};

    lm.addTestProvider(providerName, false, false, false, false, false, true, true, 0, 5);
    lm.setTestProviderEnabled(providerName, true);

    this.mRegistered = true;
    
	Intent intent = new Intent();
	intent.setAction(FAKE_LOCATION_PROVIDER_REGISTERED);
	intent.addCategory(Intent.CATEGORY_DEFAULT);
	intent.putExtra("GPSstate", true);
	intent.putExtra("Latitude", 0.);
	intent.putExtra("Longitude", 0.);
	ctx.sendBroadcast(intent); 		// force GPS on in case it was disabled...

  }
 
  public void shutdown() {
    unregisterProvider();
  }
	  
  public void unregisterProvider() {
	  LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
	  lm.removeTestProvider(providerName);

      this.mRegistered = false;

	  Intent intent = new Intent();
	  intent.setAction(FAKE_LOCATION_PROVIDER_UNREGISTERED);
	  intent.addCategory(Intent.CATEGORY_DEFAULT);
	  intent.putExtra("GPSstate", true);
	  intent.putExtra("Latitude", 0.);
	  intent.putExtra("Longitude", 0.);
	  ctx.sendBroadcast(intent); 		// force GPS on in case it was disabled...

  }

  public void enableProvider() {
	  if(!mRegistered) registerProvider();
  }
  public void disableProvider() {
	  if(mRegistered) unregisterProvider();
  }

  public void pushLocation(double lat, double lon) {
    LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
	double period = 1f;
	double speedX = 0,
		  speedY = 0;
	
	counter += 5f;
    counter = counter % 360;

	//this line adds noise to the speed, to simulate fluctuating speed
//	speedX = 7.5*Math.cos(counter%360f * (Math.PI / 180.f));
//	speedY = 7.5*Math.sin(counter%360f * (Math.PI / 180.f));

	//the following code adds regular zeroes, to simulate abrupt stops
	/*if(counter%3 <= TOLERANCE){
		speed = 0;
	}*/	

	//the following code simulates negative/wrong sensor values by just
	//subtracting the halfSpeed offset from the speed
	//speed -= halfSpeed;

//	deltaLatitude = 0.001 * (speedY);
//	deltaLongitude = 0.001 * (speedX); 
 
    Location mockLocation = new Location(providerName);
    mockLocation.setLatitude(lat);
    mockLocation.setLongitude(lon); 
    mockLocation.setAltitude(0);
//    mockLocation.setSpeed(22+(2*((System.currentTimeMillis()/1000)%2)-1)*
//			                     (System.currentTimeMillis()/1000)%20); 
    mockLocation.setTime(System.currentTimeMillis()); 
    lm.setTestProviderLocation(providerName, mockLocation);
	Log.i("MockLocation", "location: " + mockLocation.getLatitude() + ", " + mockLocation.getLongitude());
  }
 
}
