package com.reconinstruments.agps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.util.Log;

/**
 * This class is responsible for talking to the Gps chip and pushing
 * assisted Gps Stuff.
 *
 */
public class ToChipTalker implements IReconGpsEventListener{
    private static final String TAG = "ToChipTalker";
    ReconAGpsContext mOwner;
    // instance of ReconAGPS. We talk to this object instead of Native Layer
    // directly as it simplifies threading and other details
    ReconAGPS m_assistant;
    int mLastStatus;
    private boolean bAssisted = false;
    private boolean m_stopping = false;
    private boolean m_restarting = false;
    private boolean mIsRegistered = false;

    public ToChipTalker(ReconAGpsContext owner) {
        mOwner = owner;
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.reconinstruments.restartAGPS");
        mOwner.getContext().registerReceiver(mIntentReceiver, filter);
        mIsRegistered = true;
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received intent to restart AGPS");
            if (!m_stopping) {
                if (!m_restarting) {
                    m_restarting = true;
                    new Thread() {
                        @Override
                        public void run() {
                            cleanUp();
                            try {
                                Thread.sleep(1000);
                            } catch (Exception e) {
                                Log.d(TAG, "Interrupted sleep");
                            }
                            mOwner.initialize();
                            Log.d(TAG, "Successfully restarted AGPS");
                            m_restarting = false;
                        }
                    }.start();
                } else {
                    Log.d(TAG, "Currently restarting ReconAGPS");
                }
            }
        }
    };

    public boolean chipNeedsAssistance() {
	return mLastStatus == ReconAGPS.STATUS_GPS_REQUEST_ASSIST;
    }
    public boolean initialize() {
	// load native layer
	try {
	    m_assistant = ReconAGPS.Initialize();
	} catch (Exception e) {
	    Log.e(TAG, "Native Layer failed to load. Reason: " + e.getMessage() );
	    return false;
	}
	Log.i(TAG, "AGPS Native Layer initialized successfully!");

	int result = m_assistant.Start(this);
	if (result != 0) {
	    Log.e(TAG, "Failed to start GPS Session. Reason: " + Integer.toString(result) );
	    return false;
	}
	bAssisted = false;
	return true;
    }
    public void cleanUp() {
        Log.d(TAG, "cleanUp() - " + mIsRegistered);
        if (mIsRegistered) {
            mOwner.getContext().unregisterReceiver(mIntentReceiver);
            mIsRegistered = false;
        }
        if (m_stopping) {
            Log.i(TAG, "Currently stopping ReconAGPS");
            return;
        }
        m_stopping = true;
        Log.i(TAG, "Stopping up ReconAGPS");
        if (m_assistant != null) m_assistant.Stop();
        m_stopping = false;
    }
    @Override
    public void onCommandCompleted(int command, int result) {
    }
    @Override
    public void onStatusReceived(int status, int extra) {
	mLastStatus = status;
	// Notify the state machine
	mOwner.mStateMachine.gpsChipStateChanged(status);
    }
    

    private ReconAGPS.AssistanceData prepareAssistanceData(Location loc) {
	ReconAGPS.AssistanceData agps = m_assistant.new AssistanceData();
	agps.flags =  ReconAGPS.AssistanceData.ASSIST_POSITION |
	    ReconAGPS.AssistanceData.ASSIST_TIME;
	// fill time
	agps.UtcTime = loc.getTime();
	agps.uncertainty = 7000000; // FIXME hardcoded for now
	// The time comes from the Gps on the phone. So it should be
	// pretty accurate. However the xml has to travel some. And we
	// just choose the arbitrary number 7 seconds as the
	// accuracy. It is a bit too high but doesn't really affect
	// anything.

	//fill location
	agps.fix = m_assistant.new LocationFix();
	agps.fix.flags = ReconAGPS.LocationFix.LOCATION_HAS_LAT_LONG | 
	    ReconAGPS.LocationFix.LOCATION_HAS_ALTITUDE;
	agps.fix.latitude  = loc.getLatitude();
	agps.fix.longitude = loc.getLongitude();
	agps.fix.altitude  = loc.getAltitude();   
	return agps;
    }

    public void prepareAndPushDataToChip(Location loc) {
	ReconAGPS.AssistanceData agps = prepareAssistanceData(loc);
	m_assistant.Assist(agps);
    }

}
