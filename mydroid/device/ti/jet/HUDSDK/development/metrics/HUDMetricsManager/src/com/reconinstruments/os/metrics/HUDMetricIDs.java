package com.reconinstruments.os.metrics;

public class HUDMetricIDs {
    /**
     * All Time Metric : All time max altitude
     */
    public static final int ALL_TIME_MAX_ALT = 1;

    /**
     * All Time Metric : All time min altitude	
     */
    public static final int ALL_TIME_MIN_ALT = 2;

    /**
     * Altitude Metric : Non Calibrated Raw Pressure converted Altitude  
     */
    public static final int ALTITUDE_PRESSURE = 3;

    /**
     * Altitude Metric : GPS Altitude
     */
    public static final int ALTITUDE_GPS = 4;

    /**
     * Altitude Metric : Altitude Calibrated Value 
     */
    public static final int ALTITUDE_CALIBRATED = 5;

    /**
     * Altitude Metric : Delta Altitude Value - based on the pressure altitude 
     */
    public static final int ALTITUDE_DELTA = 6;

    /**
     * Speed Metric : Horizontal Speed Metric in KMH - calculated from the GPS Speed
     */
    public static final int SPEED_HORIZONTAL = 7;

    /**
     * Speed Metric : Vertical Speed Metric in KMH - calculated from the pressure sensor change
     */
    public static final int SPEED_VERTICAL = 8;


    /**
     * Speed Metric : 3D Speed Metric in KMH - the vector from SPEED_HORIZONTAL and SPEED_VERTICAL
     */
    public static final int SPEED_3D = 9;

    /**
     * Speed Metric : Low Pass Filtered Speed in KMH - Hidden from the developer - this is used for Pace calculation
	/** @hide */
    public static final int SPEED_FILTERED = 10;

    /**
     * Speed Metric : Pace - SECOND per KM 
     */
    public static final int SPEED_PACE = 11;


    /**
     * Distance Metric : Horizontal Distance 
     */
    public static final int DISTANCE_HORIZONTAL = 12;

    /**
     * Distance Metric : Vertical Distance
     */
    public static final int DISTANCE_VERTICAL = 13;

    /**
     * Distance Metric : 3D Distance  
     */
    public static final int DISTANCE_3D = 14;

    /**
     * Grade metric : Grade
     */
    public static final int GRADE = 15;
}