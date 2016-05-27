package com.reconinstruments.agps;
import android.content.BroadcastReceiver;
import com.reconinstruments.mobilesdk.hudconnectivity.Constants;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityService.Channel;
import android.location.Location;
import android.content.Context;
import android.content.Intent;
import com.reconinstruments.mobilesdk.agps.ReconAGps;
import android.util.Log;
/**
 * class <code>IncomingAGpsLocationListener</code> is responsible for
 * fetching the location data that comes from the phone
 *
 */
public class IncomingAGpsLocationListener extends BroadcastReceiver {
    ReconAGpsContext mOwner;
    private Location mLatestLocation = null;
    private static final String TAG = "IncomingAGpsLocationListener";
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
	Log.v(TAG,"Got message");
	byte[] hcmba = intent.getByteArrayExtra("message");
	Log.v(TAG,"Check if message is empty");
	if(hcmba == null) return; // message is empy
	Log.v(TAG,"hcmba not empty");
	HUDConnectivityMessage hcm = new HUDConnectivityMessage(hcmba);
	if (hcm == null) return; // no hud connectivity message
	Log.v(TAG,"hcm not empty");
	try {
	    Location loc = ReconAGps.getLocation(hcm);
	    mLatestLocation = loc;
	    // Notify the state machine
	    mOwner.mStateMachine.phoneGpsReceived(loc);
	}
	catch (ReconAGps.InvalidLocationXml e) {
	    Log.e(TAG,"InvalidLocationXml");
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