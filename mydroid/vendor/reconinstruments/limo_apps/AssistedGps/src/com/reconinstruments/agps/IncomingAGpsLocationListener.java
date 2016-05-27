package com.reconinstruments.agps;
import android.content.BroadcastReceiver;
import com.reconinstruments.mobilesdk.hudconnectivity.Constants;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityService.Channel;
import android.location.Location;
import android.content.Context;
import android.content.Intent;
import com.reconinstruments.mobilesdk.agps.ReconAGps;

/**
 * class <code>IncomingAGpsLocationListener</code> is responsible for
 * fetching the location data that comes from the phone
 *
 */
public class IncomingAGpsLocationListener extends BroadcastReceiver {
    ReconAGpsContext mOwner;
    private Location mLatestLocation = null;
    /**
     * Get the latest location received from the phone. If no location
     * has been received null is retained.
     *
     * @return a <code>Location</code> value
     */
    public Location getLatestLocation() {
	return mLatestLocation;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
	byte[] hcmba = intent.getByteArrayExtra("message");
	if(hcmba == null) return; // message is empy
	HUDConnectivityMessage hcm = new HUDConnectivityMessage(hcmba);
	if (hcm == null) return; // no hud connectivity message
	try {
	    Location loc = ReconAGps.getLocation(hcm);
	    mLatestLocation = loc;
	    // Notify the state machine
	    mOwner.mStateMachine.phoneGpsReceived(loc);
	}
	catch (ReconAGps.InvalidLocationXml e) {
	}
    }
    /**
     * Creates a new <code>IncomingAGpsLocationListener</code> instance.
     *
     * @param agpsa an <code>ReconAGpsContext</code> object as owner
     */
    public IncomingAGpsLocationListener(ReconAGpsContext agpsa) {
	super();
	mOwner = agpsa;
    }
}