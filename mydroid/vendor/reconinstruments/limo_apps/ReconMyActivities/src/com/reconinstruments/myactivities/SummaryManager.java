//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.myactivities;

import java.util.HashMap;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.reconinstruments.utils.BundleUtils;
import com.reconinstruments.utils.stats.StatFormatUtils;
import com.reconinstruments.utils.stats.StatsUtil.StatDescription;
import com.reconinstruments.utils.stats.StatsUtil.FormattedStat;
import com.reconinstruments.utils.stats.StatsUtil.MetricType;
import com.reconinstruments.utils.stats.StatsUtil.StatType;
import com.reconinstruments.utils.stats.StatsUtil;
import com.reconinstruments.utils.stats.ActivityUtil;
import com.reconinstruments.utils.SettingsUtil;
import com.reconinstruments.utils.stats.TranscendUtils;

/**
 * <code>TranscendServiceManager</code> accepts all of the sensor bundle coming
 * from transcend service and provides the proper format to UI layer.
 */
public class SummaryManager {

    private static final String TAG = SummaryManager.class.getSimpleName();

    private static HashMap<StatType,StatDescription> statDescs = new HashMap<StatType,StatDescription>();
    static {
        statDescs.put(StatType.ALL_TIME_ACTIVITIES, new StatDescription("SPORTS_ACTIVITY_BUNDLE", MetricType.ACTIVITIES, "TotalNumberOfRides"));
        statDescs.put(StatType.LAST_ACTIVITY_DATE, new StatDescription("SPORTS_ACTIVITY_BUNDLE", MetricType.DATE, "StartTime"));
        statDescs.put(StatType.ALL_TIME_BEST_DURATION, new StatDescription("SPORTS_ACTIVITY_BUNDLE", MetricType.MOVINGDURATION, "BestDuration"));
        statDescs.put(StatType.ALL_TIME_TOTAL_DURATION, new StatDescription("SPORTS_ACTIVITY_BUNDLE", MetricType.MOVINGDURATION, "AllTimeDuration"));
        statDescs.put(StatType.LAST_ACTIVITY_DURATION, new StatDescription("SPORTS_ACTIVITY_BUNDLE", MetricType.MOVINGDURATION, "Durations"));
        statDescs.put(StatType.LAST_ACTIVITY_DISTANCE, new StatDescription("DISTANCE_BUNDLE", MetricType.DISTANCE, "Distance"));
        statDescs.put(StatType.ALL_TIME_BEST_DISTANCE, new StatDescription("DISTANCE_BUNDLE", MetricType.DISTANCE, "BestDistance"));
        statDescs.put(StatType.ALL_TIME_TOTAL_DISTANCE, new StatDescription("DISTANCE_BUNDLE", MetricType.DISTANCE, "AllTimeDistance"));
        statDescs.put(StatType.ALL_TIME_TOTAL_ELEVATION_GAIN, new StatDescription("VERTICAL_BUNDLE", MetricType.ELEVATIONGAIN, "AllTimeElevGain"));
        statDescs.put(StatType.ALL_TIME_BEST_ELEVATION_GAIN, new StatDescription("VERTICAL_BUNDLE", MetricType.ELEVATIONGAIN, "BestElevGain"));
        statDescs.put(StatType.LAST_ACTIVITY_ELEVATION_GAIN, new StatDescription("VERTICAL_BUNDLE", MetricType.ELEVATIONGAIN, "PreviousElevGain"));
        statDescs.put(StatType.LAST_ACTIVITY_AVERAGE_SPEED, new StatDescription("SPEED_BUNDLE", MetricType.SPEED, "AverageSpeed"));
        statDescs.put(StatType.ALL_TIME_BEST_AVERAGE_SPEED, new StatDescription("SPEED_BUNDLE", MetricType.SPEED, "BestAverageSpeed"));
        statDescs.put(StatType.LAST_ACTIVITY_AVERAGE_PACE, new StatDescription("SPEED_BUNDLE", MetricType.PACE, "AverageSpeed"));
        statDescs.put(StatType.ALL_TIME_BEST_AVERAGE_PACE, new StatDescription("SPEED_BUNDLE", MetricType.PACE, "BestAverageSpeed"));
        statDescs.put(StatType.LAST_ACTIVITY_CALORIES_BURNED, new StatDescription("CALORIE_BUNDLE", MetricType.CALORIE, "TotalCalories"));
        statDescs.put(StatType.ALL_TIME_TOTAL_CALORIES_BURNED, new StatDescription("CALORIE_BUNDLE", MetricType.CALORIE, "AllTimeTotalCalories"));
        statDescs.put(StatType.ALL_TIME_BEST_CALORIES_BURNED, new StatDescription("CALORIE_BUNDLE", MetricType.CALORIE, "BestCalorieBurn"));
        statDescs.put(StatType.LAST_ACTIVITY_AVERAGE_HEART_RATE, new StatDescription("HR_BUNDLE", MetricType.HEARTRATE, "AverageHeartRate"));
        statDescs.put(StatType.ALL_TIME_BEST_JUMP, new StatDescription("JUMP_BUNDLE", MetricType.JUMP, "AllTimeBestJump"));
        statDescs.put(StatType.ALL_TIME_MAX_ALTITUDE, new StatDescription("ALTITUDE_BUNDLE", MetricType.ELEVATIONGAIN, "AllTimeMaxAlt"));
        statDescs.put(StatType.ALL_TIME_VERTICAL, new StatDescription("VERTICAL_BUNDLE", MetricType.ELEVATIONGAIN, "AllTimeVert"));
        statDescs.put(StatType.ALL_TIME_TOTAL_RUNS, new StatDescription("RUN_BUNDLE", MetricType.SKIRUN, "AllTimeTotalNumberOfSkiRuns"));
        statDescs.put(StatType.ALL_TIME_MAX_SPEED, new StatDescription("SPEED_BUNDLE", MetricType.SPEED, "AllTimeMaxSpeed"));
        statDescs.put(StatType.LAST_ACTIVITY_JUMP, new StatDescription("JUMP_BUNDLE", MetricType.JUMP, "BestJump"));
        statDescs.put(StatType.LAST_ACTIVITY_ALTITUDE, new StatDescription("ALTITUDE_BUNDLE", MetricType.ELEVATIONGAIN, "MaxAlt"));
        statDescs.put(StatType.LAST_ACTIVITY_VERTICAL, new StatDescription("VERTICAL_BUNDLE", MetricType.ELEVATIONGAIN, "Vert"));
        statDescs.put(StatType.LAST_ACTIVITY_RUNS, new StatDescription("RUN_BUNDLE", MetricType.SKIRUN, "Runs"));
        statDescs.put(StatType.LAST_ACTIVITY_MAX_SPEED, new StatDescription("SPEED_BUNDLE", MetricType.SPEED, "MaxSpeed"));
        statDescs.put(StatType.LAST_ACTIVITY_AVERAGE_CADENCE, new StatDescription("CADENCE_BUNDLE", MetricType.CADENCE, "AverageCadence"));

        for(StatType type:statDescs.keySet()) {
            statDescs.get(type).type = type;
        }
    }

    public static StatDescription getStatDesc(StatType type){
        return statDescs.get(type);
    }

    private HashMap<StatType,FormattedStat> stats;
    private Context mContext;
    private int activityType = ActivityUtil.SPORTS_TYPE_OTHER;


    public SummaryManager(Context context, int activityType) {
        mContext = context;
        stats = new HashMap<StatType,FormattedStat>();
        this.activityType = activityType; 

        Bundle data = TranscendUtils.readSummaryDumpIntoBundle(activityType);
        if(data == null) //if there is no activity data, do nothing.
            return;

        boolean isMetric = SettingsUtil.getUnits(mContext) == SettingsUtil.RECON_UINTS_METRIC;
        for(StatDescription desc: StatsUtil.getStatDescs().values()) {
            stats.put(desc.type, StatFormatUtils.formatData(context, data, desc, isMetric, false));
        }
    }

    public HashMap<StatType,FormattedStat> getStats(){
        return stats;
    }
    public FormattedStat getFormattedStat(StatType type){
        return stats.get(type);
    }
    public String getStatValue(StatType type){
        FormattedStat stat = stats.get(type);
        if(stat!=null)
            return stat.value;
        else
            return "";
    }


    public int getActivityType() {
        return activityType;
    }

    public boolean hasActivities() {
        return !stats.isEmpty();
    }
}