package com.reconinstruments.utils.stats;

import com.reconinstruments.utils.stats.StatsUtil.StatType;

import android.graphics.Color;

// Code for getting UI Strings/UI data for stats

public class StatTextUtils {

    
    public static String getLastActivityName(int activityType) {
        switch(activityType) {
        case ActivityUtil.SPORTS_TYPE_CYCLING:
            return "Last Ride";
        case ActivityUtil.SPORTS_TYPE_RUNNING:
            return "Last Run";
        case ActivityUtil.SPORTS_TYPE_SKI:
            return "Last Day";
        default:
            return "Last Activity";
        }
    }
    public static String getActivityTitle(int activityType) {
        switch(activityType) {
        case ActivityUtil.SPORTS_TYPE_CYCLING:
            return "CYCLING";
        case ActivityUtil.SPORTS_TYPE_RUNNING:
            return "RUNNING";
        case ActivityUtil.SPORTS_TYPE_SKI:
        default:
            return "MY RECORDS";
        }
    }
    public static String getActivityUnit(int activityType) {
        switch(activityType) {
        case ActivityUtil.SPORTS_TYPE_CYCLING:
            return "RIDES";
        case ActivityUtil.SPORTS_TYPE_RUNNING:
            return "RUNS";
        case ActivityUtil.SPORTS_TYPE_SKI:
            return "DAYS";
        default:
            return "ACTIVITIES";
        }   
    }
    
    public static String getTitleForStat(StatType type, boolean isSnow2) {
        if(isSnow2) {
            switch(type) {
            case ALL_TIME_TOTAL_RUNS:
                return "Total Runs";
            case ALL_TIME_MAX_SPEED:
            case LAST_ACTIVITY_MAX_SPEED:
                return "Max Speed";
            case ALL_TIME_VERTICAL:
                return "Total Vertical";
            case ALL_TIME_TOTAL_DISTANCE:
                return "Total Distance";
            case ALL_TIME_MAX_ALTITUDE:
            case LAST_ACTIVITY_ALTITUDE:
                return "Max Altitude";
            case ALL_TIME_BEST_JUMP:
            case LAST_ACTIVITY_JUMP:
                return "Best Jump";
            case LAST_ACTIVITY_RUNS:
                return "Runs";
            case LAST_ACTIVITY_VERTICAL:
                return "Vertical";
            case LAST_ACTIVITY_DISTANCE:
                return "Distance";
            default:
                return "";
            } 
        } else {
            switch(type) {
            case ALL_TIME_TOTAL_DURATION:
            case ALL_TIME_BEST_DURATION:
            case LAST_ACTIVITY_DURATION:
                return "DURATION";
                
            case ALL_TIME_BEST_DISTANCE:
            case LAST_ACTIVITY_DISTANCE:
            case ALL_TIME_TOTAL_DISTANCE:
                return "DISTANCE";
            case ALL_TIME_BEST_ELEVATION_GAIN:
            case LAST_ACTIVITY_ELEVATION_GAIN:
            case ALL_TIME_TOTAL_ELEVATION_GAIN:
                return "ELEV GAIN";
            case ALL_TIME_BEST_AVERAGE_PACE:
            case LAST_ACTIVITY_AVERAGE_PACE:
                return "AVG PACE";
                
            case ALL_TIME_BEST_CALORIES_BURNED:
            case LAST_ACTIVITY_CALORIES_BURNED:
            case ALL_TIME_TOTAL_CALORIES_BURNED:
                return "CALORIES";

            case ALL_TIME_BEST_AVERAGE_SPEED:
            case LAST_ACTIVITY_AVERAGE_SPEED:
                return "AVG SPEED";
            case LAST_ACTIVITY_GRADE: 
                return "GRADE";
            case LAST_ACTIVITY_MAX_POWER: 
                return "MAX POWER";
            case LAST_ACTIVITY_AVERAGE_POWER:
                return "AVG POWER";
            case LAST_ACTIVITY_HEART_RATE: 
                return "HEART RATE";
            case LAST_ACTIVITY_AVERAGE_HEART_RATE:
                return "AVG HEART RATE";
                
            case LAST_ACTIVITY_MAX_SPEED: 
                return "MAX SPEED";
            case LAST_ACTIVITY_CADENCE: 
                return "CADENCE";
            case LAST_ACTIVITY_AVERAGE_CADENCE:
                return "AVG CADENCE";
            case LAST_ACTIVITY_RUNS: 
                return "TOTAL RUNS";
            default:
                return "";
            }
        }
    }
    
    public static int getColorForStat(StatType type, boolean isSnow2) {
        if(!isSnow2)
            return -1;

        switch(type) {
        case ALL_TIME_TOTAL_RUNS:
        case LAST_ACTIVITY_RUNS:
            return -1;
        case ALL_TIME_MAX_SPEED:
        case LAST_ACTIVITY_MAX_SPEED:
            return Color.rgb(108, 209, 35);
        case ALL_TIME_VERTICAL:
        case LAST_ACTIVITY_VERTICAL:
            return Color.rgb(0, 186, 255);
        case ALL_TIME_TOTAL_DISTANCE:
        case LAST_ACTIVITY_DISTANCE:
            return Color.rgb(242, 226, 44);
        case ALL_TIME_MAX_ALTITUDE:
        case LAST_ACTIVITY_ALTITUDE:
            return Color.rgb(230, 122, 0);
        case ALL_TIME_BEST_JUMP:
        case LAST_ACTIVITY_JUMP:
            return Color.rgb(255, 51, 255);
        }
        return -1;
    }
}
