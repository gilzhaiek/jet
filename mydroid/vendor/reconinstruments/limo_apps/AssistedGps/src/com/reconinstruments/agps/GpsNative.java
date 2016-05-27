package com.reconinstruments.agps;
/**
 * This class is just a placeholder for Native stuff defined in JNI.
 *
 */
class GpsNative {
    public static native int agps_query_assist();
    public static native int agps_init(String a, String b,String c,String d, String e);
    public static native void agps_cleanup();
    public static native int agps_begin_assist(int a);
    public static native int agps_end_assist();
    public static native int agps_inject_location(double a ,double b, double c, int d);
    public static native int agps_inject_time(long a,int b);
    public static native int agps_inject_almanac(String a,int b);
    public static native int agps_inject_ephemeris(String a);
}
