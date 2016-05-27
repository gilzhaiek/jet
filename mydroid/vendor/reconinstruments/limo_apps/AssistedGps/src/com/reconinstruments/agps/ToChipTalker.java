package com.reconinstruments.agps;
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

    public ToChipTalker(ReconAGpsContext owner) {
	mOwner = owner;
    }
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
	// setup events we are interested in
	int flags = ReconAGPS.GPS_COMMAND_FLAG | 
	    ReconAGPS.GPS_STATUS_FLAG |
	    ReconAGPS.GPS_NMEA_FLAG |
	    ReconAGPS.GPS_LOCATION_FLAG;
	int result = m_assistant.Start(this, flags);
	if (result != 0) {
	    Log.e(TAG, "Failed to start GPS Session. Reason: " + Integer.toString(result) );
	    return false;
	}
	bAssisted = false;
	return true;
    }
    public void cleanUp() {
	if (m_assistant != null) m_assistant.Stop();
    }
    @Override
    public void onCommandCompleted(int command, int result) {
    }
    @Override
    public void onStatusReceived(int status) {
	mLastStatus = status;
	// Notify the state machine
	mOwner.mStateMachine.gpsChipStateChanged(status);
    }
    @Override
    public void onNmeaData(String strNmea)  {
    }
    @Override
    public void onPositionReport(ReconAGPS.LocationFix fix)   {
	// mGpsLAT.setText(Double.toString(fix.latitude) );
	// mGpsLONG.setText(Double.toString(fix.longitude) );
	// mGpsAlt.setText(Double.toString(fix.altitude) ); 
	// mGpsSpeed.setText(Float.toString(fix.speed) );

    }

    private ReconAGPS.AssistanceData prepareAssistanceData(Location loc) {
	ReconAGPS.AssistanceData agps = m_assistant.new AssistanceData();
	agps.flags =  ReconAGPS.AssistanceData.ASSIST_POSITION |
	    ReconAGPS.AssistanceData.ASSIST_TIME |
	    ReconAGPS.AssistanceData.ASSIST_ALMANAC;
	// fill time
	agps.UtcTime = loc.getTime();
	agps.uncertainty = (int)loc.getAccuracy();
	// fill almanac
	agps.almanac_file = AlmanacFetcher.ALMANAC_FILE;
	agps.almanac_format = ReconAGPS.AssistanceData.ALMANAC_FORMAT_YUMA;
	// fill location
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