package com.reconinstruments.hudserver.metrics.route;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.reconinstruments.os.metrics.BaseMetric;
import com.reconinstruments.os.metrics.BaseMetricGroup;
import com.reconinstruments.os.metrics.HUDMetricIDs;

public class HUDAllTimeRouteMetrics extends BaseMetricGroup {
    private static final String TAG = "HUDAllTimeRouteMetrics";


    private static HUDAllTimeRouteMetrics instance = null; 

    public static HUDAllTimeRouteMetrics getInstance() {
        if(instance == null) {
            Log.d(TAG, "init: HUDAllTimeRouteMetrics is initalized ");
            instance = new HUDAllTimeRouteMetrics();
        } 
        return instance;
    }

    private HUDAllTimeRouteMetrics() {
        super();
        putMetric(new BaseMetric(HUDMetricIDs.ALL_TIME_MAX_ALT));
        putMetric(new BaseMetric(HUDMetricIDs.ALL_TIME_MIN_ALT));
    }

    @Override
    public void load(SharedPreferences persistantStats) {
        getMetric(HUDMetricIDs.ALL_TIME_MAX_ALT).load(persistantStats);
        getMetric(HUDMetricIDs.ALL_TIME_MIN_ALT).load(persistantStats);
    }

    @Override
    public void save(Editor persistantStatsEditor) {
        getMetric(HUDMetricIDs.ALL_TIME_MAX_ALT).save(persistantStatsEditor);
        getMetric(HUDMetricIDs.ALL_TIME_MIN_ALT).save(persistantStatsEditor);
    }

    @Override
    public void reset(){
        getMetric(HUDMetricIDs.ALL_TIME_MAX_ALT).reset();
        getMetric(HUDMetricIDs.ALL_TIME_MIN_ALT).reset();
    }
}
