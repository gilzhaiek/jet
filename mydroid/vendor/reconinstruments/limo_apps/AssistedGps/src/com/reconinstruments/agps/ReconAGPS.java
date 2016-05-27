package com.reconinstruments.agps;

import java.io.IOException;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ReconAGPS
{   
	/* Nested sub-classes; data structure for easier argument passing */
	 public class LocationFix
	 {
		 double latitude = 0;
		 double longitude = 0;
		 double altitude = 0;
		 float  accuracy = 0;
		 float  speed = 0;
		 float  bearing = 0;
		 
		 int flags = 0;            // location flags bitmask; values must match GpsLocationProvider.java
		 
		 public static final int LOCATION_HAS_LAT_LONG = 1;
         public static final int LOCATION_HAS_ALTITUDE = 2;
         public static final int LOCATION_HAS_SPEED = 4;
         public static final int LOCATION_HAS_BEARING = 8;
         public static final int LOCATION_HAS_ACCURACY = 16;
	 }
	 
	 public class AssistanceData
	 {
		 LocationFix fix;
		 long        UtcTime = 0;
		 int         uncertainty = 0;
		 String      almanac_file;
		 int         almanac_format = 0;
		 String      ephemeris_file;
		 
		 int flags = 0;         // bitmask of assistance types client is providing
		 
		 /* almanac formats */
         public static final int ALMANAC_FORMAT_YUMA = 1;
         public static final int ALMANAC_FORMAT_SEM  = 2;
         
         /* assistance flags. Need to match GpsAidingData enumeration */
         public static final int ASSIST_EPHEMERIS = 0x01;
         public static final int ASSIST_ALMANAC   = 0x02;
         public static final int ASSIST_POSITION  = 0x04;
         public static final int ASSIST_TIME      = 0x08;
	 }
	 
	// standard Ident tag for logcat filtering
	private static final String TAG = "+++ " + ReconAGPS.class.getSimpleName() + " (from Java) +++";
	
	/* Handler and Method Signatures that will be asynchronously invoked through JNI layer */
	private static final String HANDLER_CLASS       = "com/reconinstruments/agps/ReconAGPS";
	private static final String STATUS_CALLBACK     = "statusReport";
	private static final String COMMAND_CALLBACK    = "cmdComplete";
	private static final String NMEA_CALLBACK       = "nmeaReport";
	private static final String POSITION_CALLBACK   = "positionReport";
    

    
    /* status codes (need to match with recongps.h), reported in
     * IReconGpsEventListener.onStatusReceived  */
    public static final int STATUS_GPS_SESSION_BEGIN   = 0x01;   
    public static final int STATUS_GPS_SESSION_END     = 0x02;
    public static final int STATUS_GPS_ENGINE_ON       = 0x03;
    public static final int STATUS_GPS_ENGINE_OFF      = 0x04;
    public static final int STATUS_GPS_REQUEST_ASSIST  = 0x04;
    public static final int STATUS_GPS_DEL_ASSIST      = 0x05;
    
    /* Gps Command enumeration
     * Values must match with "recongps.h */
    public static final int COMMAND_GPS_INJECT_POSITION  = 0x08;
    public static final int COMMAND_GPS_INJECT_TIME      = 0x09;
    public static final int COMMAND_GPS_INJECT_ALMANAC   = 0xA;
    public static final int COMMAND_GPS_INJECT_EPHEMERIS = 0xB;
    public static final int COMMAND_GPS_BEGIN_ASSIST     = 0xD;
    public static final int COMMAND_GPS_END_ASSIST       = 0xE;

    /* Flags indicating Gps Event subscription. Use bitmask
     * when staring GPS Session to indicate callbacks of interest */
    public static final int GPS_COMMAND_FLAG  = 0x01;
    public static final int GPS_STATUS_FLAG   = 0x02;
    public static final int GPS_NMEA_FLAG     = 0x04;
    public static final int GPS_LOCATION_FLAG = 0x08;
    
     // native so file name
	 private static final String SHARED_LIB = "reconagps";    // "libreconagps.so"
	 
	 // internal data
	 private int     mCbkFlags;                      // client callback flags
	 private int     mAssistCommand;                 // flag indicating if we are in the middle off assistance
	                                                 // when set, indicates last command issued
	 
	 private IReconGpsEventListener m_client;	     // client callback interface
	 private ReconAGPS.AssistanceData mAssistData;   // client assistance data
	 
    /* Singleton Pattern */
	 private ReconAGPS () {;}
	 protected static ReconAGPS mInstance = null;
	 
	 // **** PUBLIC INTERFACE ****
	 
	 /* Constructor -- Singleton Pattern
	  * All we do here is load shared native library, and can be safely done during Activity
	  * or Service startup. If this fails, there is no purpose continuing.
	  * 
	  * For session management, use Start/Stop methods
	  *  */
	 public  static ReconAGPS Initialize () throws UnsatisfiedLinkError, IOException
	 {
		 if (ReconAGPS.mInstance == null)
		 {
		     // load native shared library. This will automatically throw if libRECON-native.so can not be found
			 System.loadLibrary(ReconAGPS.SHARED_LIB);
			 
			 // now allocate us
			 ReconAGPS.mInstance = new ReconAGPS();
			 ReconAGPS.mInstance.m_client = null;     // passed during startup
			 
			 ReconAGPS.mInstance.mAssistData = null;
			 ReconAGPS.mInstance.mAssistCommand = 0;
			 ReconAGPS.mInstance.mCbkFlags = 0;
			 
		 }
		 
		 return mInstance;
	 }

     /* Session Startup. Callbacks are registerd. Client will typically want to call
      * this during Activity Restart/Resume */
	 public int Start (IReconGpsEventListener cbk, int flags)
	 {
		 // remember client callback
		 m_client = cbk;
		 
		 String strNmea = "";
		 String strLocation = "";
		 
	/*	 
	 * Assistant always needs command and status callbacks
	 * 
	 * if ( (flags & ReconAGPS.GPS_COMMAND_FLAG) == ReconAGPS.GPS_COMMAND_FLAG)
			 strCommand = ReconAGPS.COMMAND_CALLBACK; */
		
		 if ( (flags & ReconAGPS.GPS_NMEA_FLAG) == ReconAGPS.GPS_NMEA_FLAG)
			 strNmea = ReconAGPS.NMEA_CALLBACK;
		 
		 if ( (flags & ReconAGPS.GPS_LOCATION_FLAG) == ReconAGPS.GPS_LOCATION_FLAG)
			 strLocation = ReconAGPS.POSITION_CALLBACK;
		 
		 int result = GpsNative.agps_init(
				 ReconAGPS.HANDLER_CLASS, ReconAGPS.STATUS_CALLBACK, ReconAGPS.COMMAND_CALLBACK, strNmea, strLocation);
		 
		 // if it succeeded, save flags
		 if (result == 0) mCbkFlags = flags;
		 return result;
		 
	 }
	 
	 /* Session Cleanup. Callbacks are deregistered. Client will typically want to call
	  * this either during Acitivy Pause/Stop */
	 public void Stop ()
	 {
		 m_client = null;  // not need to sync as it is always executing in main thread (?)
		 
		 mAssistData = null;
		 mAssistCommand = 0;
		 mCbkFlags = 0;
		 
		 GpsNative.agps_cleanup ();
		 
		 
	 }

	 /* Inject Assistance
	  * We encapuslate this in single request to facilitate client code.
	  * 
	  * Command status will still be passed to client, but we will take care of sequence
	  * (begin -> time -> location -> almanac -> ephemeris -> end)
	  * 
	  * Final status that indicates to client if assistance succeeded will be 
	  * result of COMMAND_GPS_END_ASSIST  */
	 public int Assist (ReconAGPS.AssistanceData data)
	 {
		 if (mAssistCommand != 0)
		 {
			 Log.e(TAG, "Assistance Injection already in progress. Last command: " + Integer.toHexString(mAssistCommand) );
			 return -1;
		 }

	     // command callbacks are always registered internally. Check if he
		 // provided any data!
		 if (( (data.flags & ReconAGPS.AssistanceData.ASSIST_POSITION) != ReconAGPS.AssistanceData.ASSIST_POSITION) &&
			 ( (data.flags & ReconAGPS.AssistanceData.ASSIST_TIME) != ReconAGPS.AssistanceData.ASSIST_TIME) &&
			 ( (data.flags & ReconAGPS.AssistanceData.ASSIST_ALMANAC) != ReconAGPS.AssistanceData.ASSIST_ALMANAC) &&
			 ( (data.flags & ReconAGPS.AssistanceData.ASSIST_EPHEMERIS) != ReconAGPS.AssistanceData.ASSIST_EPHEMERIS) )
		 {
			 Log.e(TAG, "Invalid assistance. At least one of (Position|Time|Almanac|Ephemeris) must be provided!");
			 return -1;
		 }
		 
		 Log.i(TAG, "Assistance Request Starting. Flags: " + Integer.toHexString(data.flags) );
		 
		 // start assistance. If it succeeds, results are passed async so
		 // save client data
		 int result = GpsNative.agps_begin_assist (data.flags);
		 if (result == 0) 
	     {
			 mAssistCommand = ReconAGPS.COMMAND_GPS_BEGIN_ASSIST;
			 mAssistData = data;
	     }
		 
		 Log.d(TAG, "Assistance started. Type: " + Integer.toHexString(data.flags) );
		 return result;
	 }
	 
	 /* JNI Callbacks. Never call directly from Java. 
	  * THIS IS EXECUTING IN CONTEXT OF SEPARATE THREAD. Invoking client interface from here
	  * would cause Core Dump; thus these methods are made Static. We simply send message to ourselves
	  * and invoke client callbacks from context of main thread */
	 
	 /* Command Completion */
	 static void cmdComplete (int command, int status)
	 {
		String strLog = String.format("+++ GPS Command Complete callback. Command: [0x%x], Status: [0x%x] +++\n", command, status);
		Log.d(TAG, strLog);
		
		// send message to ourselves
		Message msg = ReconAGPS.mInstance.mGpsHandler.obtainMessage();
		
		msg.what = ReconAGPS.GPS_COMMAND_FLAG;
		msg.arg1 = command;
		msg.arg2 = status;
		
		ReconAGPS.mInstance.mGpsHandler.sendMessage(msg);
		
	 }
	 
	 /* Status Report */
	 static void statusReport (int status)
	 {
		String strLog = String.format("+++ GPS Status callback. Status: [0x%x] +++\n", status);
		Log.d(TAG, strLog);
		
		// send message to ourselves
		Message msg = ReconAGPS.mInstance.mGpsHandler.obtainMessage();
		
		msg.what = ReconAGPS.GPS_STATUS_FLAG;
		msg.arg1 = status;
		
		ReconAGPS.mInstance.mGpsHandler.sendMessage(msg);
	 }
	
	 /* Location Fix */
	 static void positionReport
	 (
		double latitude,
		double longitude,
		double altitude,
		float  accuracy,
		float  speed,
		float  bearing
	 )
	 {
	//	String strLog = String.format("### GPS Position Report Callback ####\n");
	//	Log.i(TAG, strLog);
		
		// allocate data object
		LocationFix fix = ReconAGPS.mInstance.new LocationFix();
		
		fix.latitude  = latitude;
		fix.longitude = longitude;
		fix.altitude  = altitude;
		fix.accuracy  = accuracy;
		fix.speed     = speed;
		fix.bearing   = bearing;
		
		// TODO: should add "flags" passed from Native; hard-coding to all right now
		//       as for AGPS it is not important
		fix.flags = ReconAGPS.LocationFix.LOCATION_HAS_ACCURACY |
				    ReconAGPS.LocationFix.LOCATION_HAS_ALTITUDE |
				    ReconAGPS.LocationFix.LOCATION_HAS_BEARING |
				    ReconAGPS.LocationFix.LOCATION_HAS_LAT_LONG |
				    ReconAGPS.LocationFix.LOCATION_HAS_SPEED;
		
		// send message to ourselves
		Message msg = ReconAGPS.mInstance.mGpsHandler.obtainMessage();
		
		msg.what = ReconAGPS.GPS_LOCATION_FLAG;
		msg.obj = fix;
		
		ReconAGPS.mInstance.mGpsHandler.sendMessage(msg);
	 }
	
	 /* NMEA Sentence */
	 static void nmeaReport (String sequence)
	 {
	//	String strLog = String.format("### NMEA Report Callback. Sequence: [%s],####\n", sequence);
	//	Log.d(TAG, strLog);
		
		// send message to ourselves
		Message msg = ReconAGPS.mInstance.mGpsHandler.obtainMessage();
		
		msg.what = ReconAGPS.GPS_NMEA_FLAG;
		msg.obj = sequence;
		
		ReconAGPS.mInstance.mGpsHandler.sendMessage(msg);
	 }


     /* Internal Message Handler of callbacks from JNI context thread */
	 @SuppressLint("HandlerLeak")
	 private class GpsMessageHandler extends Handler
	 {
	    
	    @Override
	    public void handleMessage(Message msg)
	    {
	    	// determine message type (callback type) from what
	    	switch (msg.what)
	    	{
		    	// for status event, value is in arg1
	    	    case ReconAGPS.GPS_STATUS_FLAG:
	    	    {
	    	    	if ( (mCbkFlags & ReconAGPS.GPS_STATUS_FLAG) == ReconAGPS.GPS_STATUS_FLAG)
	    	    	   m_client.onStatusReceived(msg.arg1);
	    	    }
	    	    break;
		    	
		    	// for command completion command is in arg1, result is in arg2
	    	    case ReconAGPS.GPS_COMMAND_FLAG:
	    	    {
	    	        // inform client always first regardless of command
	    	        if ( (mCbkFlags & ReconAGPS.GPS_COMMAND_FLAG) == ReconAGPS.GPS_COMMAND_FLAG)
	    	           m_client.onCommandCompleted(msg.arg1, msg.arg2);
	    	        
	    	       // check if we are in the middle of assistance process to continue
	    	       if (msg.arg1 == mAssistCommand)
	    	    	   ReconAGPS.this.continueAssistance(msg.arg2);
	    	    }
	    	    break;
		    	
		    	// for nmea sentence, data is String in Object
		        case ReconAGPS.GPS_NMEA_FLAG:
	    	    {
	    	    	m_client.onNmeaData((String) msg.obj);
	    	    }
	    	    break;
	    	    
		    	// for position report, data is LocationFix in object
	    	    case ReconAGPS.GPS_LOCATION_FLAG:
	    	    {
	    	    	LocationFix fix = (LocationFix) msg.obj;
	    	    	m_client.onPositionReport(fix);
	    	    }
	    	    break;
	    	}
	    }

		
	 }

     private GpsMessageHandler mGpsHandler = new GpsMessageHandler();
     
     // internal handler for assistance process when flag is set at Assist
     private void continueAssistance(int status)
     {
    	 int result = 0;
    	 
    	 // first check status. If this failed send assistance end
    	 if (status != 0 )
    	 {
    		 Log.e(TAG, "Error during assistance transaction. Command: " + Integer.toHexString(mAssistCommand) +
    				 " Status: " + Integer.toHexString(status) );
    		 
    		 if (mAssistCommand != ReconAGPS.COMMAND_GPS_END_ASSIST)
                mAssistCommand = ReconAGPS.COMMAND_GPS_END_ASSIST;
    		 else
    			result = status;   // don't send twice!!
    	 }
    	 
    	 else
    	 {
    		 // determine next command
	    	 switch (mAssistCommand)
	    	 {
	    	     case ReconAGPS.COMMAND_GPS_BEGIN_ASSIST:
	    	     {
	    	    	 if ( (mAssistData.flags & ReconAGPS.AssistanceData.ASSIST_TIME) == (ReconAGPS.AssistanceData.ASSIST_TIME) )
	    	    		 mAssistCommand = ReconAGPS.COMMAND_GPS_INJECT_TIME;
	    	    	 
	    	    	 else  if ( (mAssistData.flags & ReconAGPS.AssistanceData.ASSIST_POSITION) == (ReconAGPS.AssistanceData.ASSIST_POSITION) )
	    	    		 mAssistCommand = ReconAGPS.COMMAND_GPS_INJECT_POSITION;
	    	    	 
	    	    	 else  if ( (mAssistData.flags & ReconAGPS.AssistanceData.ASSIST_ALMANAC) == (ReconAGPS.AssistanceData.ASSIST_ALMANAC) )
	    	    		 mAssistCommand = ReconAGPS.COMMAND_GPS_INJECT_ALMANAC;
	    	    	 
	    	    	 else  if ( (mAssistData.flags & ReconAGPS.AssistanceData.ASSIST_EPHEMERIS) == (ReconAGPS.AssistanceData.ASSIST_EPHEMERIS) )
	    	    		 mAssistCommand = ReconAGPS.COMMAND_GPS_INJECT_EPHEMERIS;
	    	    	 
	    	    	 else
	    	    		 mAssistCommand = ReconAGPS.COMMAND_GPS_END_ASSIST;
	    	    	 
	    	    	 
	    	     }
	    	     break;
	    	     
	    	     case ReconAGPS.COMMAND_GPS_INJECT_TIME:
	    	     {
	    	    	  if ( (mAssistData.flags & ReconAGPS.AssistanceData.ASSIST_POSITION) == (ReconAGPS.AssistanceData.ASSIST_POSITION) )
	    	    		 mAssistCommand = ReconAGPS.COMMAND_GPS_INJECT_POSITION;
	    	    	 
	    	    	 else  if ( (mAssistData.flags & ReconAGPS.AssistanceData.ASSIST_ALMANAC) == (ReconAGPS.AssistanceData.ASSIST_ALMANAC) )
	    	    		 mAssistCommand = ReconAGPS.COMMAND_GPS_INJECT_ALMANAC;
	    	    	 
	    	    	 else  if ( (mAssistData.flags & ReconAGPS.AssistanceData.ASSIST_EPHEMERIS) == (ReconAGPS.AssistanceData.ASSIST_EPHEMERIS) )
	    	    		 mAssistCommand = ReconAGPS.COMMAND_GPS_INJECT_EPHEMERIS;
	    	    	 
	    	    	 else
	    	    		 mAssistCommand = ReconAGPS.COMMAND_GPS_END_ASSIST;
	    	     }
	    	     break;
	    	     
	    	     case ReconAGPS.COMMAND_GPS_INJECT_POSITION:
	    	     {
	    	    	 if ( (mAssistData.flags & ReconAGPS.AssistanceData.ASSIST_ALMANAC) == (ReconAGPS.AssistanceData.ASSIST_ALMANAC) )
	    	    		 mAssistCommand = ReconAGPS.COMMAND_GPS_INJECT_ALMANAC;
	    	    	 
	    	    	 else  if ( (mAssistData.flags & ReconAGPS.AssistanceData.ASSIST_EPHEMERIS) == (ReconAGPS.AssistanceData.ASSIST_EPHEMERIS) )
	    	    		 mAssistCommand = ReconAGPS.COMMAND_GPS_INJECT_EPHEMERIS;
	    	    	 
	    	    	 else
	    	    		 mAssistCommand = ReconAGPS.COMMAND_GPS_END_ASSIST;
	    	     }
	    	     break;
	    	     
	    	     case ReconAGPS.COMMAND_GPS_INJECT_ALMANAC:
	    	     {
                     if ( (mAssistData.flags & ReconAGPS.AssistanceData.ASSIST_EPHEMERIS) == (ReconAGPS.AssistanceData.ASSIST_EPHEMERIS) )
	    	    		 mAssistCommand = ReconAGPS.COMMAND_GPS_INJECT_EPHEMERIS;
	    	    	 
	    	    	 else
	    	    		 mAssistCommand = ReconAGPS.COMMAND_GPS_END_ASSIST;
	    	     }
	    	     break;
	    	     
	    	     case ReconAGPS.COMMAND_GPS_INJECT_EPHEMERIS:
	    	     {
	    	    	mAssistCommand = ReconAGPS.COMMAND_GPS_END_ASSIST;
	    	     }
	    	     break;
	    	     
	    	     case ReconAGPS.COMMAND_GPS_END_ASSIST:
	    	     {
	                  // nothing to do now, this was last command!
	    	    	  Log.i(TAG, "Assistance Request Finished");
	    	    	  
	    	    	  // TODO: Send event to client? He should get FIX_START!
	    	    	  mAssistData = null;
    		          mAssistCommand = 0;
    		          
    		          return;
	    	     }
	    	
	    	     default:
	    	     {
	    	    	 Log.e(TAG, "Internal Error - this can NEVER happen!!!");
	    	    	 return;
	    	     }
	    	 }  // switch
    	 }// else
    	 
    	 // now send next command
    	 if (mAssistCommand == ReconAGPS.COMMAND_GPS_INJECT_POSITION)
    	 {
    		 Log.i(TAG, "Injecting Location Assistance ... Latitude: " +
    	          Double.toString(mAssistData.fix.latitude) + " Longitude: " +
    			  Double.toString(mAssistData.fix.longitude) + " Altitude: " +
    	          Double.toString(mAssistData.fix.altitude) + " Flags: " + 
    			  Integer.toHexString(mAssistData.fix.flags) );
    		 try
    		 {
    		 result = GpsNative.agps_inject_location(
    				 mAssistData.fix.latitude,
    				 mAssistData.fix.longitude,
    				 mAssistData.fix.altitude,
    				 mAssistData.fix.flags);
    		 }
    		 catch (Exception ex)
    		 {
    			 result = -1;
    			 Log.e(TAG, "Exception occured injecting Location: " + ex.getMessage() );
    		 }
    		 
    	 }
    	 else if (mAssistCommand == ReconAGPS.COMMAND_GPS_INJECT_TIME)
    	 {
    		 Log.i(TAG, "Injecting Time Assistance ...");
    		 result = GpsNative.agps_inject_time(mAssistData.UtcTime, mAssistData.uncertainty);
    	 }
    	 else if (mAssistCommand == ReconAGPS.COMMAND_GPS_INJECT_ALMANAC)
    	 {
    		 Log.i(TAG, "Injecting Almanac Assistance ...");
    		 result = GpsNative.agps_inject_almanac(mAssistData.almanac_file, mAssistData.almanac_format);
    	 }
    	 else if (mAssistCommand == ReconAGPS.COMMAND_GPS_INJECT_EPHEMERIS)
    	 {
    		 Log.i(TAG, "Injecting Ephmeris Assistance ...");
    		 result = GpsNative.agps_inject_ephemeris(mAssistData.ephemeris_file);
    	 }
    	 else if (mAssistCommand == ReconAGPS.COMMAND_GPS_END_ASSIST)
    	 {
    		 Log.i(TAG, "Ending Assistance ...");
    		 result = GpsNative.agps_end_assist();
    	 }
    	 
    	 // in case of failure, abort everything
    	 if (result != 0)
    	 {
    		 Log.e(TAG, "Assistance command " + Integer.toHexString(mAssistCommand) + 
    				 " Error " + Integer.toHexString(result) );
    	     
    		 mAssistData = null;
    		 mAssistCommand = 0;
    	 }
    	 
     } // continueAssistance
}
