package com.reconinstruments.hudserver.metrics;

import android.location.Location;

import com.reconinstruments.os.metrics.BaseValue;

public class MetricUtils {

    public static final float HDOP_CUTOFF_FOR_VALID_SPEED = 2;

    private static final int ACCURACY_IN_HDOP = 10; 

    /**
     * Low pass filter factor - the weight given to the new value
     */
    private static final float LOW_PASS_FILTER_NEW_VALUE_WEIGHT = 0.1f;

    /**
     * Using the SmartAverager to average the difference between the GPS Altitude to the Pressure Calculated Altitude
     */
    public static final int AVG_OFFSET_SIZE = 500;

    /**
     * Min number of sats to calibrate pressure
     */
    public static final int MIN_SATS_CALIBRATE_PRESSURE = 5;

    /**
     * Max value for change between metrics in milliseconds
     */
    public static final int MAX_TIME_MILLIS_SENSOR_OUT_OF_SYNC = 1500; // 1.5 Seconds

    /**
     * Decimeters In Meters
     */
    public static final float DECIMETERS_IN_METERS = 10.0f;

    /**
     * Max Horizontal Speed allowed
     */
    public static final float MAX_ALLOWED_HORIZONTAL_SPEED_KPH = 300.0f;

    /**
     * Multiplier for conversion, from m/s to km/h
     */
    private static final float MPS_TO_KMPH = 3.6f;

    /**
     * Various Flags
     */
    public static final boolean USE_MOCK_METRICS = false;

    private static final short[] PRESSURE_DELIMITER = {
        1000, 1130, 1300, 1500, 1730, 2000, 2300,  2650,      //above the Mount Everest
        3000, 3350, 3700, 4100, 4500, 5000, 5500,  6000,
        6500, 7100, 7800, 8500, 9200, 9700, 10300, 11000
    }; // 24 members

    private static final short[] ALT_COEFFICIENT_I = {
        12256, 10758, 9329, 8085, 7001, 6069, 5360, 4816,
        4371,  4020,  3702, 3420, 3158, 2908, 2699, 2523,
        2359,  2188,  2033, 1905, 1802, 1720, 1638
    }; // 23 members

    private static final short[] ALT_COEFFICIENT_J =  {
        16212, 15434, 14541, 13630, 12722, 11799, 10910, 9994,
        9171,  8424,  7737,  7014,  6346,  5575,  4865,  4206,
        3590,  2899,  2151,  1456,  805,   365,   -139
    }; // 23 members 0.1mbar


    /**
     * @param timeMillis
     * @param baseValue
     * @return the delta time between timeMillis to baseValue - a positive time if timeMillis is newer than baseValue
     */
    public static long deltaTimeMillis(long timeMillis, BaseValue baseValue) {
        return timeMillis - baseValue.ChangeTime;
    }
    /**
     * @param baseValue1 
     * @param baseValue2 
     * @return the delta time between baseValue1 to baseValue2 - a positive time if baseValue1 is newer than baseValue2
     */
    public static long deltaTimeMillis(BaseValue baseValue1, BaseValue baseValue2) {
        return baseValue1.ChangeTime - baseValue2.ChangeTime;
    }

    public static long timeSinceLastChangeMillis(BaseValue baseValue) {
        return timeSinceLastChangeMillis(baseValue.ChangeTime);
    }

    public static long timeSinceLastChangeMillis(long changeTime) {
        return System.currentTimeMillis() - changeTime;
    }

    public static float applyLowPassFilter(float oldValue, float newValue) {
        return applyLowPassFilter(oldValue, newValue, LOW_PASS_FILTER_NEW_VALUE_WEIGHT);
    }

    public static float applyLowPassFilter(float oldValue, float newValue, float newValueWeight) {
        return oldValue*(1.0f-newValueWeight) + newValue * newValueWeight;
    }

    /**
     * @param location
     * @return true if the GPS Signal is strong enough to know that the speed is correct
     */
    public static boolean signalStrongForSpeed(Location location){
        return signalStrongForSpeed(getHDOP(location));
    }

    /**
     * @param hdop
     * @return true if the HDOP is strong enough to know that the speed is correct
     */
    public static boolean signalStrongForSpeed(float hdop){
        return hdop < HDOP_CUTOFF_FOR_VALID_SPEED;
    }

    /**
     * @param value
     * @return true if the float is not equal to NaN, false if it is
     */
    public static boolean isValidFloat(float value) {
        return !Float.isNaN(value);
    }

    /**
     * Convert value from km/h to second/km
     * @param value in km/h
     * @return sec/km
     */
    public static float KMPHToSecPKM(float value) {
        return 1f / value * 3600f;
    }

    /**
     * Convert value from m/s to km/h
     * @param value in m/s
     * @return km/h
     */
    public static float MPSToKMPH(float value) {
        return MPS_TO_KMPH*value;
    }

    /**
     * Convert value from km/h to m/s
     * @param value in km/h
     * @return m/s
     */
    public static float KMPHToMPS(float value) {
        return value/MPS_TO_KMPH;
    }

    /**
     * @param location
     * @return the HDOP from location's accuracy
     */
    public static float getHDOP(Location location){
        return location.getAccuracy() / ACCURACY_IN_HDOP; // TODO: Check with Nicolas
    }

    /**
     * Helper to calculate altitude from sensor barometric pressure [hPa]
     * @param pressure
     * @return altitude in meters
     */
    public static float convertToAltitudeInMeters(float pressure){
        return convertToAltitude(pressure)/DECIMETERS_IN_METERS;
    }

    /**
     * Helper to calculate altitude from sensor barometric pressure [hPa]
     * @param pressure
     * @return altitude in 0.1meters i.e. 10 is 1 meter.
     */
    public static int convertToAltitude(float pressure)  {
        int idx, tmp, altituydeDecimalBits;

        int p = (int)(pressure*100);

        for (idx = 22; idx > 0; idx--) //22 ,Pressure_Delimiter[i]=10300
        {
            tmp = (int)(PRESSURE_DELIMITER[idx] * 10);
            if (p >= tmp)
                break;
        }

        tmp = (int)(ALT_COEFFICIENT_J[idx] * 10);
        altituydeDecimalBits = (p - (int)PRESSURE_DELIMITER[idx] * 10) * (int)ALT_COEFFICIENT_I[idx];
        tmp = tmp - (altituydeDecimalBits >> 11);

        return tmp;//0.1m
    }

    public static boolean isOutOfSync(BaseValue baseValue) {
        return (System.currentTimeMillis() - baseValue.ChangeTime) > MetricUtils.MAX_TIME_MILLIS_SENSOR_OUT_OF_SYNC;
    }

    public static boolean isOutOfSync(long timeMillis) {
        return (System.currentTimeMillis() - timeMillis) > MetricUtils.MAX_TIME_MILLIS_SENSOR_OUT_OF_SYNC;
    }

}
