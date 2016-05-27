package com.reconinstruments.hudserver.metrics.route;

import android.util.Log;

import com.reconinstruments.hudserver.metrics.BaseBO;
import com.reconinstruments.hudserver.metrics.MetricUtils;
import com.reconinstruments.hudserver.metrics.MetricsBOs;
import com.reconinstruments.os.metrics.HUDMetricIDs;
import com.reconinstruments.os.metrics.MetricChangedListener;

public class AltitudeDeltaBO extends BaseBO implements MetricChangedListener {
    private static final boolean DEBUG = BASE_DEBUG | false;

    float mLastPressureAltitude = Float.NaN;

    public AltitudeDeltaBO() {
        super(HUDMetricIDs.ALTITUDE_DELTA);
    }

    @Override
    public void onValueChanged(int metricID, float value, long changeTime, boolean isValid) {
        if(metricID == HUDMetricIDs.ALTITUDE_PRESSURE){
            if(!MetricUtils.isValidFloat(mLastPressureAltitude) || !isValid){
                getMetric().addInvalidValue();
            }
            else {
                getMetric().addValue(value - mLastPressureAltitude, changeTime);
            }
            mLastPressureAltitude = value;
            if(DEBUG)Log.d(TAG,"Delta Alt: " + getMetric().getValue() + " , TimeStamp: " + getMetric().getTimeMillisLatestChange());
        }
    }

    @Override
    protected void reset() {
        super.reset();
        mLastPressureAltitude = Float.NaN;
    }

    @Override
    protected void enableMetrics() {
        MetricsBOs.get(HUDMetricIDs.ALTITUDE_PRESSURE).registerListener(this);
    }

    @Override
    protected void disableMetrics() {
        MetricsBOs.get(HUDMetricIDs.ALTITUDE_PRESSURE).unregisterListener(this);
    }
}
