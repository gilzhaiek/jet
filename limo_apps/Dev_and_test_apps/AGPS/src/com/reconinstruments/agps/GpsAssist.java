package com.reconinstruments.agps;

import java.io.File;
import java.io.FilenameFilter;

import android.app.Activity;
import android.content.Context;
import android.location.GpsStatus.NmeaListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class GpsAssist extends Activity implements IReconGpsEventListener, NmeaListener, LocationListener
{
	// standard Ident tag for logcat filtering
	private static final String TAG = "RECON.GpsAssist"; 

	// instance of ReconAGPS. We talk to this object instead of Native Layer
	// directly as it simplifies threading and other details
	ReconAGPS m_assistant;

	// hardcoding path where in development I will adb push; in Production
	// Ali team needs to manage this via Web updates
	public static final String INJECTION_FOLDER = "/data/data/";
	public static final String ALMANAC_FILE = "alamanc_YUMA.alm";
	public static final String EPHEMERIS_EXT = "raw";

	public static final String RINEX_EPHEMERIS_FILE = "data/data/ephemeris_RINEX.txt";

	public static final double HARDCODED_LOCATION_LAT = 49.2767265;
	public static final double HARDCODED_LOCATION_LON = -123.1208365;
	public static final double HARDCODED_LOCATION_ALT = 8;

	// text controls for diagnostics
	private TextView mGpsStatus;
	private TextView mGpsCommand;
	private TextView mGpsResult; 
	private TextView mGpsNmea;

	private TextView mGpsLAT;
	private TextView mGpsLONG;
	private TextView mGpsAlt;
	private TextView mGpsSpeed;


	// assist service will not want/need this; in this example I am using UI 
	// app to get Fix / Nmea Android way
	private LocationManager mLocationManager = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gps_assist);

		// get text controls
		mGpsStatus   = (TextView) findViewById (R.id.gpsstatus);
		mGpsCommand  = (TextView) findViewById (R.id.gpscommand);
		mGpsResult   = (TextView) findViewById (R.id.gpsresult);
		mGpsNmea     = (TextView) findViewById (R.id.gpsnmea);

		mGpsLAT      = (TextView) findViewById (R.id.gpsLAT);
		mGpsLONG     = (TextView) findViewById (R.id.gpsLONG);
		mGpsAlt      = (TextView) findViewById (R.id.gpsalt);
		mGpsSpeed    = (TextView) findViewById (R.id.gpsspeed);

		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// load native layer
		try
		{
			m_assistant = ReconAGPS.Initialize();
		}
		catch (Exception e)
		{
			Log.e(TAG, "Native Layer failed to load. Reason: " + e.getMessage() );
			return;
		}

		Log.i(TAG, "AGPS Native Layer initialized successfully!");

		int result = m_assistant.Start(this);
		if (result != 0)
		{
			Log.e(TAG, "Failed to start GPS Session. Reason: " + Integer.toString(result) );
			return;
		}

		Log.i(TAG, "GPS Assistant started... listening for assistance requests");

		// also register for Location and NMEA Updates Android way (Assistant service will not need to do this)
		try
		{
			if (mLocationManager.addNmeaListener(this) == false)
			{
				Log.e(TAG, "Failed to add Nmea Listener");
			}
		}
		catch (Exception ex)
		{
			Log.e(TAG, "Exception thrown while trying to add Nmea Listener. Reason: " + ex.getMessage() );
		}


		try
		{
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		}
		catch (Exception ex)
		{
			Log.e(TAG, "Exception thrown while trying to register Location Listener with Android. Reason: " + ex.getMessage() );
		}

	}

	@Override
	protected void onStart ()
	{
		super.onStart();
	}

	@Override
	protected void onStop ()
	{
		super.onStop();
	}

	@Override
	protected void onDestroy ()
	{
		m_assistant.Stop ();
		super.onDestroy();
	}

	@Override
	public void onCommandCompleted(int command, int result) 
	{
		mGpsCommand.setText(Integer.toString(command) );
		mGpsResult.setText(Integer.toString(result) );
	}
	@Override
	public void onStatusReceived(int status, int extra) 
	{
		mGpsStatus.setText(Integer.toString(status));

		// assistance plug: In this example I am using:
		//  -- UtcTime as System.currentTimeMillis
		//  -- Position: Lat, Long, Alt of Granville Island, Vancouver, BC Canada
		//  -- Almanac: YUMA format, downloaded from http://www.navcen.uscg.gov/?pageName=gpsAlmanacs
		//  -- Ephemeris: Not supported right now 
		//
		// When assistance is completed, command callback with ReconAGPS.COMMAND_GPS_END_ASSIST
		// will be triggered. Examine result to determine if it was successful
		if (status == ReconAGPS.STATUS_GPS_REQUEST_ASSIST)
		{
			Log.i(TAG, "Assistance Request status received! Type: " + Integer.toHexString(extra) );
			ReconAGPS.AssistanceData agps = m_assistant.new AssistanceData();

			// extra contains bitmask of assistance types that are required; Send what you have!
			// In this example I am assuming I have everything
			if ( (extra & ReconAGPS.AssistanceData.ASSIST_POSITION) == ReconAGPS.AssistanceData.ASSIST_POSITION)
			{
				Log.i(TAG, "Position Assistance Requested ... ");
				agps.flags |= ReconAGPS.AssistanceData.ASSIST_POSITION;

				agps.fix = m_assistant.new LocationFix();
				agps.fix.flags = ReconAGPS.LocationFix.LOCATION_HAS_LAT_LONG | 
						ReconAGPS.LocationFix.LOCATION_HAS_ALTITUDE;

				// 1050 Homer Street Vancouver BC 2nd Floor
				agps.fix.latitude  = HARDCODED_LOCATION_LAT;
				agps.fix.longitude = HARDCODED_LOCATION_LON;
				agps.fix.altitude  = HARDCODED_LOCATION_ALT;   
			}

			if ( (extra & ReconAGPS.AssistanceData.ASSIST_TIME) == ReconAGPS.AssistanceData.ASSIST_TIME)
			{
				Log.i(TAG, "Time Assistance Requested ... ");
				agps.flags |= ReconAGPS.AssistanceData.ASSIST_TIME;

				agps.UtcTime = System.currentTimeMillis();
				agps.uncertainty = 0;
			}

			if ( (extra & ReconAGPS.AssistanceData.ASSIST_ALMANAC) == ReconAGPS.AssistanceData.ASSIST_ALMANAC)
			{
				Log.i(TAG, "Almanac Assistance Requested ... ");
				agps.flags |= ReconAGPS.AssistanceData.ASSIST_ALMANAC;

				agps.almanac_file = INJECTION_FOLDER + ALMANAC_FILE;
				agps.almanac_format = ReconAGPS.AssistanceData.ALMANAC_FORMAT_YUMA;
			}

			// for ephemeris we can assist either RINEX file, which contains number of satelites
			// stored in ASCII format, or single RAW Ephemeris
			if ( (extra & ReconAGPS.AssistanceData.ASSIST_EPHEMERIS) == ReconAGPS.AssistanceData.ASSIST_EPHEMERIS)
			{
				Log.i(TAG, "Ephemeris Assistance Requested ... ");
				agps.flags |= ReconAGPS.AssistanceData.ASSIST_EPHEMERIS;

				// Get the Epheremis files from injection folder
				File aiding = new File(INJECTION_FOLDER);
				File [] files = aiding.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith(EPHEMERIS_EXT);
					}
				});
				

				// Rinex example
				// agps.rinex_file = GpsAssist.RINEX_EPHEMERIS_FILE;
				// agps.ephemeris_format = ReconAGPS.AssistanceData.EPHEMERIS_FORMAT_RINEX;

				// Raw example - can pass multiple files
				agps.raw_ephemeris = new String[files.length];
				
				for (int i = 0 ; i < files.length ; i++) {
					File raw = files[i];
					Log.d(TAG,"Injecting : " + raw);
					agps.raw_ephemeris[i] = raw.getPath();
				}

				agps.ephemeris_format = ReconAGPS.AssistanceData.EPHEMERIS_FORMAT_RAW;

			}

			m_assistant.Assist(agps);
		}

	}

	@Override
	public void onLocationChanged(Location location)
	{
		mGpsLAT.setText(Double.toString(location.getAltitude() ));
		mGpsLONG.setText(Double.toString(location.getLongitude() ));
		mGpsAlt.setText(Double.toString(location.getAltitude() ));
		mGpsSpeed.setText(Float.toString(location.getSpeed() ));
	}

	@Override
	public void onProviderDisabled(String provider)
	{
		// Google dummy, we don't care
	}

	@Override
	public void onProviderEnabled(String provider)
	{
		// Google dummy, we don't care
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras)
	{
		// Google dummy; we get assistant status updates Recon route
	}

	@Override
	public void onNmeaReceived(long timestamp, String nmea)
	{
		mGpsNmea.setText(nmea);
	}

}
