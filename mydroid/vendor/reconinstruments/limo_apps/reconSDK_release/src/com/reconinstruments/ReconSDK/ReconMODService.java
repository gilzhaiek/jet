package com.reconinstruments.ReconSDK;


/** Enumeration of Messaging Mechanism with RECON MOD Service. This class
 * is primarily intended for internal usage & should be regarded as an"undocumented feature"
 * SDK Clients should use services of {@link ReconSDKManager} class instead.
 * 
 * All constants are enumerated; this is the only shared source between
 *        MOD service and SDK client. This minimizes chances for errors &
 *        miscrepancies later when internals change -- just recompile both Server
 *        and SDK */
public class ReconMODService
{
	/** MOD Service Intent String */
	public static final String INTENT_STRING    = "RECON_MOD_SERVICE";  
	
	/** Broadcast Bundle Data Object Name */
	public static final String BROADCAST_BUNDLE = "Bundle";          
	
	/**  Key value of method that indicates changed Broadcast Field */
	public static final String BROADCAST_FIELD  = "WhichOne";          
	
	/** full update Bundle enumeration */
	public class FullUpdate
	{
		public static final String ALTITUDE    = "ALTITUDE_BUNDLE";
		public static final String SPEED       = "SPEED_BUNDLE";
		public static final String DISTANCE    = "DISTANCE_BUNDLE";
		public static final String JUMPS       = "JUMP_BUNDLE";
		public static final String RUNS        = "RUN_BUNDLE";
		public static final String TEMPERATURE = "TEMPERATURE_BUNDLE";
		public static final String VERTICAL    = "VERTICAL_BUNDLE";
		public static final String LOCATION    = "LOCATION_BUNDLE";
		public static final String TIME        = "TIME_BUNDLE";
	}
	
    /** MOD Message Enumeration */
	public class Message
	{
		public static final int MSG_RESULT                 = 1;
		public static final int MSG_RESULT_CHRONO          = 2;
		public static final int MSG_GET_ALTITUDE_BUNDLE    = 3;
		public static final int MSG_GET_DISTANCE_BUNDLE    = 4;
		public static final int MSG_GET_JUMP_BUNDLE        = 5;
		public static final int MSG_GET_LOCATION_BUNDLE    = 6;
		public static final int MSG_GET_RUN_BUNDLE         = 7;
		public static final int MSG_GET_SPEED_BUNDLE       = 8;
		public static final int MSG_GET_TEMPERATURE_BUNDLE = 9;
		public static final int MSG_GET_TIME_BUNDLE        = 10;
		public static final int MSG_GET_VERTICAL_BUNDLE    = 11;
		public static final int MSG_GET_CHRONO_BUNDLE      = 12;
		public static final int MSG_GET_FULL_INFO_BUNDLE   = 13;
		public static final int MSG_CHRONO_START_STOP      = 14;
		public static final int MSG_CHRONO_LAP_TRIAL       = 15;
		public static final int MSG_CHRONO_START_NEW_TRIAL = 16;
		public static final int MSG_CHRONO_STOP_TRIAL      = 17;
		public static final int MSG_RESET_STATS            = 18;
		public static final int MSG_RESET_ALLTIME_STATS    = 19;
	}

    /** Event Broadcast Strings */
    public class BroadcastString
    {
       public  static final String BROADCAST_JUMP_ACTION_STRING        = "RECON_MOD_BROADCAST_JUMP";
       public  static final String BROADCAST_TEMPERATURE_ACTION_STRING = "RECON_MOD_BROADCAST_TEMPERATURE";
       public  static final String BROADCAST_DISTANCE_ACTION_STRING    = "RECON_MOD_BROADCAST_DISTANCE";
       public  static final String BROADCAST_VERTICAL_ACTION_STRING    = "RECON_MOD_BROADCAST_VERTICAL";
       public  static final String BROADCAST_ADVJUMP_ACTION_STRING     = "RECON_MOD_BROADCAST_ADVJUMP";
       public  static final String BROADCAST_LOCATION_ACTION_STRING    = "RECON_MOD_BROADCAST_LOCATION";
       public  static final String BROADCAST_SPEED_ACTION_STRING       = "RECON_MOD_BROADCAST_SPEED";
       public  static final String BROADCAST_TIME_ACTION_STRING        = "RECON_MOD_BROADCAST_TIME";
       public  static final String BROADCAST_ALTITUDE_ACTION_STRING    = "RECON_MOD_BROADCAST_ALT";
       public  static final String BROADCAST_RUN_ACTION_STRING         = "RECON_MOD_BROADCAST_RUN";
       
    }
    
    /** Recon Event Field Names */
    protected class FieldName
    {
       /* ReconTemperature */
       public static final String TEMP_VALUE             = "Temperature";
       public static final String TEMP_MAX               = "MaxTemperature";
       public static final String TEMP_MIN               = "MinTemperature";
       public static final String TEMP_MAX_ALL_TIME      = "AllTimeMaxTemperature";
       public static final String TEMP_MIN_ALL_TIME      = "AllTimeMinTemperature";
       
	    
       /* ReconDistance */
       public static final String DISTANCE_VALUE         = "Distance";
       public static final String DISTANCE_VERTICAL      = "VertDistance";
       public static final String DISTANCE_HORIZONTAL    = "HorzDistance";
       public static final String DISTANCE_ALL_TIME      = "AllTimeDistance";
       
       
       /* ReconVertical */
       public static final String VERTICAL_VALUE         = "Vert";
       public static final String VERTICAL_PREVIOUS      = "PreviousVert";
       public static final String VERTICAL_ALL_TIME      = "AllTimeVert";
       
       /* ReconLocation */
       public static final String LOCATION_VALUE         = "Location";
       public static final String LOCATION_PREVIOUS      = "PreviousLocation";

       /* ReconSpeed */
       public static final String SPEED_VALUE            = "Speed";
       public static final String SPEED_HORIZONTAL       = "HorzSpeed";
       public static final String SPEED_VERTICAL         = "VertSpeed";
       public static final String SPEED_MAX              = "MaxSpeed";
       public static final String SPEED_AVERAGE          = "AverageSpeed";
       public static final String SPEED_ALL_TIME_MAX     = "AllTimeMaxSpeed";
  
       /* ReconTime */
       public static final String TIME_UTC               = "UTCTimems";
       public static final String TIME_LAST_UPDATE       = "LastUpdate";
       public static final String TIME_UPDATE_BEFORE     = "TheUpdateBefore";
 
       /* ReconAltitude */
       public static final String ALTITUDE_VALUE             = "Alt";     
       public static final String ALTITUDE_MAX               = "MaxAlt";
       public static final String ALTITUDE_MIN               = "MinAlt";
       
       public static final String ALTITUDE_PREV              = "PreviousAlt";
       public static final String ALTITUDE_PREV_MAX          = "PreviousMaxAlt";
       public static final String ALTITUDE_PREV_MIN          = "PreviousMinAlt";

       public static final String ALTITUDE_ALL_TIME_MAX      = "AllTimeMaxAlt";
       public static final String ALTITUDE_ALL_TIME_MIN      = "AllTimeMinAlt";
       
	   public static final String ALTITUDE_PRESSURE          = "Pressure";
	   public static final String ALTITUDE_PRESSURE_VALUE    = "PressureAlt";
	   public static final String ALTITUDE_PRESSURE_PREV     = "PreviousPressureAlt";
	   public static final String ALTITUDE_PRESSURE_MAX      = "MaxPressureAlt";
	   public static final String ALTITUDE_PRESSURE_MIN      = "MinPressureAlt";
	   public static final String ALTITUDE_PRESSURE_PREV_MAX = "PreviousMaxPressureAlt";
	   public static final String ALTITUDE_PRESSURE_PREV_MIN = "PreviousMinPressureAlt";
	
	   public static final String ALTITUDE_GPS_VALUE         = "GpsAlt";
	   public static final String ALTITUDE_GPS_PREV          = "PreviousGpsAlt";
	
	   public static final String ALTITUDE_IS_CALLIBRATING   = "IsCallibrating";
	   public static final String ALTITUDE_IS_INITIALIZED    = "IsInitialized";
	   public static final String ALTITUDE_HEIGHT_OFFSET     = "HeightOffsetN";
       
       /* ReconJump */
       public static final String JUMP_COUNTER           = "JumpCounter";      // Total # of Jumps recorded
       public static final String JUMP_LIST              = "Jumps";            // parcellable array list in result bundle
       
       public static final String JUMP_SEQUENCE          = "Number";           // Jump Object properties:
       public static final String JUMP_DATE              = "Date";
       public static final String JUMP_AIR               = "Air";
       public static final String JUMP_DISTANCE          = "Distance";
       public static final String JUMP_DROP              = "Drop";
       public static final String JUMP_HEIGHT            = "Height";
       
       /* ReconRun */
       public static final String RUN_SKI_TOTAL          = "AllTimeTotalNumberOfSkiRuns";
       public static final String RUN_LIST               = "Runs";
       
       public static final String RUN_SEQUENCE           = "Number";
       public static final String RUN_START              = "Start";
       public static final String RUN_AVG_SPEED          = "AverageSpeed";
       public static final String RUN_MAX_SPEED          = "MaxSpeed";
       public static final String RUN_DISTANCE           = "Distance";
       public static final String RUN_VERTICAL           = "Vertical";
       
 
    }
    
}
