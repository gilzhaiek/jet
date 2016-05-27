package com.reconinstruments.mocklocation;
import android.content.Intent;
import android.app.Activity;
import android.os.Bundle;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;


public class MockLocationActivity extends Activity
{
    MockLocationProvider mock;
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.main);
 
	mock = new MockLocationProvider(LocationManager.GPS_PROVIDER, this);
 
	//Set test location
	mock.pushLocation(49.276793,-123.121186 );
//	mock.pushLocation(51.511742,-0.12341 );
//	mock.pushLocation(50.108333,-122.9425 );
	startService(new Intent("RECON_MOCK_LOCATION_SERVICE"));
 
	LocationManager locMgr = (LocationManager)
	    getSystemService(LOCATION_SERVICE);
	// LocationListener lis = new LocationListener() {
	// 	public void onLocationChanged(Location location) {
	// 	    //You will get the mock location
	// 	}
	// 	//...
	//     };
 
	// locMgr.requestLocationUpdates(
	// 			      LocationManager.GPS_PROVIDER, 1000, 1, lis);
    }
 
    protected void onDestroy() {
	mock.shutdown();
	super.onDestroy();
    }
}
