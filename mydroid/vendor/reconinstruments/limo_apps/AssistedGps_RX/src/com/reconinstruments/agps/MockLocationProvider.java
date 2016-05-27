package com.reconinstruments.agps;
import android.content.Context;
import android.content.Intent;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

/**
 * class <code>MockLocationProvider</code> is responsible for pushing
 * location to system via mock location mechanism
 *
 */
public class MockLocationProvider {
    private static final String TAG = MockLocationProvider.class.getSimpleName();
    public static String PROVIDER_NAME = "RECON_AGPS";
    private static final String AGPS_REGISTERED = "com.reconinstruments.agps.AGPS_REGISTERED";
    ReconAGpsContext mOwner;
    LocationManager mLocManager;

    /**
     * Creates a new <code>MockLocationProvider</code> instance.
     *
     * @param owner a <code>ReconAGpsContext</code> value
     */
    public MockLocationProvider(ReconAGpsContext owner) {
	mOwner = owner;
	enableMockLocation();
	setupLocationManager();
    }

    private void setupLocationManager() {
	mLocManager = (LocationManager) mOwner.getContext()
	    .getSystemService(Context.LOCATION_SERVICE);
	//	mLocManager.removeTestProvider(PROVIDER_NAME);
	if (mLocManager.getProvider(PROVIDER_NAME) == null) {
	    mLocManager.addTestProvider(PROVIDER_NAME, false, false, false, false, false,
                true, true, 0, 5);
	}
        else {
        Log.e(TAG, "Provider " + PROVIDER_NAME + " already exists");
    }
	mLocManager.setTestProviderEnabled(PROVIDER_NAME, true);
	Intent registeredAGPS = new Intent(AGPS_REGISTERED);
	mOwner.getContext().sendBroadcast(registeredAGPS);
    }

    public void pushLocation(Location loc) {
        // for some reason, just passing in loc doesn't work, so create
        // a new instance of Location with the current time and pass that in
        Location agpsLocation = new Location(PROVIDER_NAME);
        agpsLocation.setLatitude(loc.getLatitude());
        agpsLocation.setLongitude(loc.getLongitude());
        agpsLocation.setTime(System.currentTimeMillis());
        agpsLocation.setAltitude(0);
	mLocManager.setTestProviderLocation(PROVIDER_NAME, agpsLocation);
    }

    public void pushLocation() {
	pushLocation(mOwner.mAgpsLocListener.getLatestLocation());
    }
 
    public void shutdown() {
	if (mLocManager.getProvider(PROVIDER_NAME) != null) {
	    mLocManager.removeTestProvider(PROVIDER_NAME);
	}
    }
    private void enableMockLocation() {
	Settings.Secure.putInt(mOwner.getContext().getContentResolver(),
			       Settings.Secure.ALLOW_MOCK_LOCATION,1);
    }

}