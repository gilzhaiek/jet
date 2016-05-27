package com.reconinstruments.hudserver.metrics.route;

import android.util.Log;

import com.reconinstruments.hudserver.metrics.BaseBO;
import com.reconinstruments.hudserver.metrics.MetricUtils;
import com.reconinstruments.hudserver.metrics.MetricsBOs;
import com.reconinstruments.os.metrics.HUDMetricIDs;
import com.reconinstruments.os.metrics.MetricChangedListener;

public class Distance3DBO extends BaseBO implements MetricChangedListener{
    private static final boolean DEBUG = BASE_DEBUG | false;

    public Distance3DBO() {
        super(HUDMetricIDs.DISTANCE_3D);
        reset();
    }

    @Override
    protected void reset() {
        getMetric().addValue(0);
    }

    @Override
    protected void enableMetrics() {
        MetricsBOs.get(HUDMetricIDs.SPEED_3D).registerListener(this);
    }

    @Override
    protected void disableMetrics() {
        MetricsBOs.get(HUDMetricIDs.SPEED_3D).unregisterListener(this);
    }

    @Override
    public void onValueChanged(int metricID, float value, long changeTime, boolean isValid) {
        if(metricID == HUDMetricIDs.SPEED_3D){
            if(!isValid) {
                return;
            }

            float speedMPS = MetricUtils.KMPHToMPS(value);
            float distance3D = getMetric().getValue() + speedMPS;
            getMetric().addValue(distance3D, changeTime);

            if(DEBUG)Log.d(TAG,"Distance3D: " + getMetric().getValue() + " , TimeStamp: " + getMetric().getTimeMillisLatestChange());
        }
    }
}
