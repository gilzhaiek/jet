package com.reconinstruments.hudserver.metrics.route;

import android.util.Log;

import com.reconinstruments.hudserver.metrics.BaseBO;
import com.reconinstruments.hudserver.metrics.MetricsBOs;
import com.reconinstruments.os.metrics.HUDMetricIDs;
import com.reconinstruments.os.metrics.MetricChangedListener;

public class DistanceVerticalBO extends BaseBO implements MetricChangedListener{

    private static final boolean DEBUG = true;
    private final String TAG = this.getClass().getSimpleName();

    public DistanceVerticalBO() {
        super(HUDMetricIDs.DISTANCE_VERTICAL);
        reset();
    }

    @Override
    protected void reset() {
        getMetric().addValue(0);
    }

    @Override
    protected void enableMetrics() {
        MetricsBOs.get(HUDMetricIDs.ALTITUDE_DELTA).registerListener(this);
    }

    @Override
    protected void disableMetrics() {
        MetricsBOs.get(HUDMetricIDs.ALTITUDE_DELTA).unregisterListener(this);
    }

    @Override
    public void onValueChanged(int metricID, float value, long changeTime, boolean isValid) {
        if(metricID == HUDMetricIDs.ALTITUDE_DELTA){
            if(!isValid){
                return;
            }

            float deltaAlt = Math.abs(value);
            float vertDist = getMetric().getValue() + deltaAlt;
            getMetric().addValue(vertDist, changeTime);
            if(DEBUG)Log.d(TAG,"DistanceVert: " + getMetric().getValue() + " , TimeStamp: " + getMetric().getTimeMillisLatestChange());
        }
    }

}
