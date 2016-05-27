package com.reconinstruments.hudserver.metrics.route;

import android.util.Log;

import com.reconinstruments.hudserver.metrics.BaseBO;
import com.reconinstruments.hudserver.metrics.MetricUtils;
import com.reconinstruments.hudserver.metrics.MetricsBOs;
import com.reconinstruments.os.metrics.HUDMetricIDs;
import com.reconinstruments.os.metrics.MetricChangedListener;

public class SpeedVerticalBO extends BaseBO implements MetricChangedListener{
    private static final boolean DEBUG = BASE_DEBUG | false;

    public SpeedVerticalBO() {
        super(HUDMetricIDs.SPEED_VERTICAL);
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
    public void onValueChanged(int metricID, float value, long changeTime,  boolean isValid) {
        if(metricID == HUDMetricIDs.ALTITUDE_DELTA){

            /**
             *  We assume that the sensor driver is more accurate in providing the data every 1 per second
             *  We provide a time stamp in Java level - but latency is added and fluctuated
             *  So we will assume that if the data is less than 1.5 seconds apart - it is about 1 second apart in the driver
             */

            if(isValid) {
                float deltaAlt = Math.abs(value);
                getMetric().addValue(MetricUtils.MPSToKMPH(deltaAlt), changeTime);
            }
            else {
                getMetric().addInvalidValue();
            }
            if(DEBUG) Log.d(TAG,"Vert Speed: " + getMetric().getValue() + " , TimeStamp: " + getMetric().getTimeMillisLatestChange());
        }
    }
}
