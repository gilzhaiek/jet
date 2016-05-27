package com.reconinstruments.utils.stats;

import java.util.HashMap;


/**
 * Created by jinkim on 25/02/15.
 * <p/>
 * This class contains constants to be used in calculations involving Activity Stats
 */
public class StatsUtil {

    // invalid stats values
    public static final float       INVALID_ALT = -10000;
    public static final int         INVALID_AIR = -10000;
    public static final int         INVALID_CADENCE = 0XFFFF;
    public static final int         INVALID_CALORIES = -1;
    public static final long        INVALID_DATE = -10000;
    public static final float       INVALID_DISTANCE = -10000;
    public static final float       INVALID_DROP = -10000;
    public static final float       INVALID_GRADE = -10000;
    public static final float       INVALID_HEIGHT = -10000;
    public static final short       INVALID_HR = 255;
    public static final int         INVALID_NUMBER = -10000;
    public static final float       INVALID_PACE = -1;
    public static final int         INVALID_POWER = 0XFFF;
    public static final float       INVALID_SPEED = -1;
    public static final int         INVALID_TEMPERATURE = -274; // -1 K
    public static final int         INVALID_VALUE = -10000;
    
    public enum StatType {
        ALL_TIME_ACTIVITIES,

        ALL_TIME_TOTAL_DURATION,
        ALL_TIME_TOTAL_DISTANCE,
        ALL_TIME_TOTAL_ELEVATION_GAIN,
        ALL_TIME_TOTAL_CALORIES_BURNED,
        ALL_TIME_BEST_DURATION,
        ALL_TIME_BEST_DISTANCE,
        ALL_TIME_BEST_ELEVATION_GAIN,
        ALL_TIME_BEST_AVERAGE_SPEED,
        ALL_TIME_BEST_AVERAGE_PACE,
        ALL_TIME_BEST_CALORIES_BURNED,
        LAST_ACTIVITY_DATE,
        LAST_ACTIVITY_PLACE,
        LAST_ACTIVITY_DURATION,
        LAST_ACTIVITY_DISTANCE,
        LAST_ACTIVITY_ELEVATION_GAIN,
        LAST_ACTIVITY_AVERAGE_SPEED,
        LAST_ACTIVITY_AVERAGE_PACE,
        LAST_ACTIVITY_AVERAGE_WATT,
        LAST_ACTIVITY_HEART_RATE,
        LAST_ACTIVITY_AVERAGE_HEART_RATE,
        LAST_ACTIVITY_CALORIES_BURNED,
        LAST_ACTIVITY_CADENCE,
        LAST_ACTIVITY_AVERAGE_CADENCE,

        LAST_ACTIVITY_AVERAGE_POWER,
        LAST_ACTIVITY_MAX_POWER,
        LAST_ACTIVITY_GRADE,
        //snow
        ALL_TIME_TOTAL_RUNS,
        ALL_TIME_BEST_JUMP,
        ALL_TIME_MAX_ALTITUDE,
        ALL_TIME_VERTICAL,
        LAST_ACTIVITY_RUNS,
        LAST_ACTIVITY_JUMP,
        LAST_ACTIVITY_ALTITUDE,
        LAST_ACTIVITY_VERTICAL,
        ALL_TIME_MAX_SPEED,
        LAST_ACTIVITY_MAX_SPEED
    }
    public enum MetricType {
        POWER, 
        HEARTRATE, 
        CADENCE, 
        SPEED, 
        PACE, 
        DISTANCE, 
        MOVINGDURATION, 
        TERRAINGRADE, 
        ELEVATIONGAIN, 
        CALORIE, 
        JUMP, 
        SKIRUN,
        DATE,
        ACTIVITIES
    };
    
    public static class StatDescription {
        public StatDescription(String bundleName, MetricType metricType, String bundleParam) {
            this.bundleName = bundleName;
            this.bundleParam = bundleParam;
            this.metricType = metricType;
        }
        public StatType type;
        public String bundleName;
        public String bundleParam;
        public MetricType metricType;
    };

    public static class FormattedStat {
        public FormattedStat(String value, String unit) {
            this.value = value;
            this.unit = unit;
        }
        public String value;
        public String unit;
    }

    private static final HashMap<StatType,StatDescription> STAT_DESCS = new HashMap<StatType,StatDescription>();
    static {
        STAT_DESCS.put(StatType.ALL_TIME_ACTIVITIES, new StatDescription("SPORTS_ACTIVITY_BUNDLE", MetricType.ACTIVITIES, "TotalNumberOfRides"));
        STAT_DESCS.put(StatType.LAST_ACTIVITY_DATE, new StatDescription("SPORTS_ACTIVITY_BUNDLE", MetricType.DATE, "StartTime"));
        STAT_DESCS.put(StatType.ALL_TIME_BEST_DURATION, new StatDescription("SPORTS_ACTIVITY_BUNDLE", MetricType.MOVINGDURATION, "BestDuration"));
        STAT_DESCS.put(StatType.ALL_TIME_TOTAL_DURATION, new StatDescription("SPORTS_ACTIVITY_BUNDLE", MetricType.MOVINGDURATION, "AllTimeDuration"));
        STAT_DESCS.put(StatType.LAST_ACTIVITY_DURATION, new StatDescription("SPORTS_ACTIVITY_BUNDLE", MetricType.MOVINGDURATION, "Durations"));
        STAT_DESCS.put(StatType.LAST_ACTIVITY_DISTANCE, new StatDescription("DISTANCE_BUNDLE", MetricType.DISTANCE, "Distance"));
        STAT_DESCS.put(StatType.ALL_TIME_BEST_DISTANCE, new StatDescription("DISTANCE_BUNDLE", MetricType.DISTANCE, "BestDistance"));
        STAT_DESCS.put(StatType.ALL_TIME_TOTAL_DISTANCE, new StatDescription("DISTANCE_BUNDLE", MetricType.DISTANCE, "AllTimeDistance"));
        STAT_DESCS.put(StatType.ALL_TIME_TOTAL_ELEVATION_GAIN, new StatDescription("VERTICAL_BUNDLE", MetricType.ELEVATIONGAIN, "AllTimeElevGain"));
        STAT_DESCS.put(StatType.ALL_TIME_BEST_ELEVATION_GAIN, new StatDescription("VERTICAL_BUNDLE", MetricType.ELEVATIONGAIN, "BestElevGain"));
        STAT_DESCS.put(StatType.LAST_ACTIVITY_ELEVATION_GAIN, new StatDescription("VERTICAL_BUNDLE", MetricType.ELEVATIONGAIN, "ElevGain"));
        STAT_DESCS.put(StatType.LAST_ACTIVITY_AVERAGE_SPEED, new StatDescription("SPEED_BUNDLE", MetricType.SPEED, "AverageSpeed"));
        STAT_DESCS.put(StatType.ALL_TIME_BEST_AVERAGE_SPEED, new StatDescription("SPEED_BUNDLE", MetricType.SPEED, "BestAverageSpeed"));
        STAT_DESCS.put(StatType.LAST_ACTIVITY_AVERAGE_PACE, new StatDescription("SPEED_BUNDLE", MetricType.PACE, "AverageSpeed"));
        STAT_DESCS.put(StatType.ALL_TIME_BEST_AVERAGE_PACE, new StatDescription("SPEED_BUNDLE", MetricType.PACE, "BestAverageSpeed"));
        STAT_DESCS.put(StatType.LAST_ACTIVITY_CALORIES_BURNED, new StatDescription("CALORIE_BUNDLE", MetricType.CALORIE, "TotalCalories"));
        STAT_DESCS.put(StatType.ALL_TIME_TOTAL_CALORIES_BURNED, new StatDescription("CALORIE_BUNDLE", MetricType.CALORIE, "AllTimeTotalCalories"));
        STAT_DESCS.put(StatType.ALL_TIME_BEST_CALORIES_BURNED, new StatDescription("CALORIE_BUNDLE", MetricType.CALORIE, "BestCalorieBurn"));
        STAT_DESCS.put(StatType.LAST_ACTIVITY_HEART_RATE, new StatDescription("HR_BUNDLE", MetricType.HEARTRATE, "HeartRate"));
        STAT_DESCS.put(StatType.LAST_ACTIVITY_AVERAGE_HEART_RATE, new StatDescription("HR_BUNDLE", MetricType.HEARTRATE, "AverageHeartRate"));
        STAT_DESCS.put(StatType.ALL_TIME_BEST_JUMP, new StatDescription("JUMP_BUNDLE", MetricType.JUMP, "AllTimeBestJump"));
        STAT_DESCS.put(StatType.ALL_TIME_MAX_ALTITUDE, new StatDescription("ALTITUDE_BUNDLE", MetricType.ELEVATIONGAIN, "AllTimeMaxAlt"));
        STAT_DESCS.put(StatType.ALL_TIME_VERTICAL, new StatDescription("VERTICAL_BUNDLE", MetricType.ELEVATIONGAIN, "AllTimeVert"));
        STAT_DESCS.put(StatType.ALL_TIME_TOTAL_RUNS, new StatDescription("RUN_BUNDLE", MetricType.SKIRUN, "AllTimeTotalNumberOfSkiRuns"));
        STAT_DESCS.put(StatType.ALL_TIME_MAX_SPEED, new StatDescription("SPEED_BUNDLE", MetricType.SPEED, "AllTimeMaxSpeed"));
        STAT_DESCS.put(StatType.LAST_ACTIVITY_JUMP, new StatDescription("JUMP_BUNDLE", MetricType.JUMP, "BestJump"));
        STAT_DESCS.put(StatType.LAST_ACTIVITY_ALTITUDE, new StatDescription("ALTITUDE_BUNDLE", MetricType.ELEVATIONGAIN, "MaxAlt"));
        STAT_DESCS.put(StatType.LAST_ACTIVITY_VERTICAL, new StatDescription("VERTICAL_BUNDLE", MetricType.ELEVATIONGAIN, "Vert"));
        STAT_DESCS.put(StatType.LAST_ACTIVITY_RUNS, new StatDescription("RUN_BUNDLE", MetricType.SKIRUN, "Runs"));
        STAT_DESCS.put(StatType.LAST_ACTIVITY_MAX_SPEED, new StatDescription("SPEED_BUNDLE", MetricType.SPEED, "MaxSpeed"));
        STAT_DESCS.put(StatType.LAST_ACTIVITY_CADENCE, new StatDescription("CADENCE_BUNDLE", MetricType.CADENCE, "Cadence"));
        STAT_DESCS.put(StatType.LAST_ACTIVITY_AVERAGE_CADENCE, new StatDescription("CADENCE_BUNDLE", MetricType.CADENCE, "AverageCadence"));
        STAT_DESCS.put(StatType.LAST_ACTIVITY_AVERAGE_POWER, new StatDescription("POWER_BUNDLE", MetricType.POWER, "Power"));
        STAT_DESCS.put(StatType.LAST_ACTIVITY_MAX_POWER, new StatDescription("POWER_BUNDLE", MetricType.POWER, "MaxPower"));
        STAT_DESCS.put(StatType.LAST_ACTIVITY_GRADE, new StatDescription("GRADE_BUNDLE", MetricType.TERRAINGRADE, "TerrainGrade"));

        for(StatType type:STAT_DESCS.keySet()) {
            STAT_DESCS.get(type).type = type;
        }
    }
    
    public static HashMap<StatType,StatDescription> getStatDescs() {
        return STAT_DESCS;
    }
    public static StatDescription getStatDesc(StatType type) {
        return STAT_DESCS.get(type);
    }
}
