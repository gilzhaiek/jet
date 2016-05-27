package com.reconinstruments.agps;

/* Native Proxy; bridge between Java world and JNI
 * This is PURE INTERFACE with no logic at this level
 * Essentially a "C header file" */

class GpsNative 
{	 
    // initialization - cleanup of Native Layer
    // Handler Class name and callback function names are passed in init.
    // in particular event. Class name must be fully qualified (package/name). 
    // 
    // Handler method names MUST BE STATIC; set to empty strings for data you are not interested in.
    // For instance if interested only in Status but not Command status
    // 
    // String handler = "com/reconinstruments/agps/GpsAssist";
    // String status = "statuscallback";
    // String command = "";
    //
    //  agps_init(handler, status, command);
    //
    static native int  agps_init (String Class, String Status, String Command);
    static native void agps_cleanup ();
    
    // begin assistance
    static native int  agps_begin_assist (int flags);
    
    // end assistance  
    static native int  agps_end_assist ();
    
    // location assist
    static native int  agps_inject_location (double lat, double lon, double alt, int flags);
    
    // time assist
    static native int  agps_inject_time (long utctime, int uncertainty);
    
    // almanac assist
    static native int  agps_inject_almanac (String path, int format);
    
    // emphemeris assist
    static native int  agps_inject_ephemeris (String path, int format);
    
}