package com.reconinstruments.agps;
import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

/**
 * class <code>MockLocationProvider</code> is responsible for pushing
 * location to system via mock location mechanism
 *
 */
public class MockLocationProvider {
    public static String PROVIDER_NAME = "RECON_AGPS";
    ReconAGpsContext mOwner;
    LocationManager mLocManager;

    /**
     * Creates a new <code>MockLocationProvider</code> instance.
     *
     * @param owner a <code>ReconAGpsContext</code> value
     */
    public MockLocationProvider(ReconAGpsContext owner) {
	mOwner = owner;
	setupLocationManager();
	enableMockLocation();
    }

    private void setupLocationManager() {
	mLocManager = (LocationManager) mOwner.getContext()
	    .getSystemService(Context.LOCATION_SERVICE);
	//	mLocManager.removeTestProvider(PROVIDER_NAME);
	if (mLocManager.getProvider(PROVIDER_NAME) == null) {
	    mLocManager.addTestProvider(PROVIDER_NAME, false, false, false, false, false, 
					true, true, 0, 5);
	}
	mLocManager.setTestProviderEnabled(PROVIDER_NAME, true);
    }

    public void pushLocation(Location loc) {
	mLocManager.setTestProviderLocation(PROVIDER_NAME, loc);
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