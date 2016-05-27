package com.reconinstruments.hudserver.metrics.route;

import android.util.Log;

import com.reconinstruments.hudserver.metrics.BaseBO;
import com.reconinstruments.hudserver.metrics.MetricUtils;
import com.reconinstruments.hudserver.metrics.MetricsBOs;
import com.reconinstruments.os.metrics.HUDMetricIDs;
import com.reconinstruments.os.metrics.MetricChangedListener;

public class DistanceHorizontalBO extends BaseBO implements MetricChangedListener {
    private static final boolean DEBUG = BASE_DEBUG | false;

    public DistanceHorizontalBO() {
        super(HUDMetricIDs.DISTANCE_HORIZONTAL);
        reset();
    }

    @Override
    protected void reset() {
        getMetric().addValue(0);
    }

    @Override
    protected void enableMetrics() {
        MetricsBOs.get(HUDMetricIDs.SPEED_HORIZONTAL).registerListener(this);
    }

    @Override
    protected void disableMetrics() {
        MetricsBOs.get(HUDMetricIDs.SPEED_HORIZONTAL).unregisterListener(this);
    }

    @Override
    public void onValueChanged(int metricID, float value, long changeTime, boolean isValid) {
        if(metricID == HUDMetricIDs.SPEED_HORIZONTAL){
            if(!isValid){
                return;
            }

            float speedMPS = MetricUtils.KMPHToMPS(value); 
            float horzDistance = getMetric().getValue() + speedMPS;
            getMetric().addValue(horzDistance, changeTime);

            if(DEBUG)Log.d(TAG,"DistanceHorz: " + getMetric().getValue() + " , TimeStamp: " + getMetric().getTimeMillisLatestChange());
        }
    }
}
