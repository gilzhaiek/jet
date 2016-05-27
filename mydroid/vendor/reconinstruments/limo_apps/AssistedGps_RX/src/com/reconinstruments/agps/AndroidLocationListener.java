package com.reconinstruments.agps;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.os.Bundle;
import android.content.Context;
import android.util.Log;
/**
 * Onboard location listener. Listens for the event that Hud has its
 * own independant 3D gps fix.
 * 
 */
class AndroidLocationListener implements LocationListener {
    private static final String TAG = "AndroidLocationListener";
    private LocationManager mlocManager;//Android location manager
    ReconAGpsContext mOwner;
    private Location mLatestLocation = null;
    private int streakOfGood = 0;
    private static int GOOD_STREAK = 10;
    private static final float GOOD_ACCURACY = 30;
    /**
     * Get the latest location object received through our own gps
     * system. It is null if no location has been received.
     *
     * @return a <code>Location</code> value
     */
    public Location getLatestLocation () {
	return mLatestLocation;
    }
    /**
     * Creates a new <code>AndroidLocationListener</code> instance.
     *
     * @param owner a <code>ReconAGpsContext</code> object
     */
    public AndroidLocationListener (ReconAGpsContext owner) {
	mOwner = owner;
	mlocManager = (LocationManager)(mOwner.getContext().getSystemService(Context.LOCATION_SERVICE));
    }
    @Override
    public void onProviderDisabled(String provider) {
    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
    @Override
    public void onProviderEnabled(String provider) {
    }
    /**
     * Gets called by the system whenever there is a location
     * change. If the <code>Location</code> has speed info in it it is
     * interpreted as a good fix. At that point the hudGpsReceived()
     * is fired to the <code>StateMachine</code>
     */
    @Override
    public void onLocationChanged(Location location) {
	if (location.hasSpeed()) { // meanswe have 3D fix
	    mLatestLocation = location;
	    if (location.getAccuracy() < GOOD_ACCURACY)  { // good data
		streakOfGood++;
	    }
	    else if (streakOfGood < GOOD_STREAK) {
		    streakOfGood = 0;
	    }
	    if (streakOfGood == GOOD_STREAK) {
		mOwner.mStateMachine.haveGoodOwnGps();
		Log.v(TAG,"Good Streak");
	    }
	}
    }
    /**
     * Starts listening to incoming Gps locations from Android (onboard)
     *
     */
    public void initialize() {
	mlocManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, this);
    }
}	

