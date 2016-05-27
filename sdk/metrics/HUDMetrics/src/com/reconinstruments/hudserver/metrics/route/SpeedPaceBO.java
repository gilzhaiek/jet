package com.reconinstruments.hudserver.metrics.route;

import android.util.Log;

import com.reconinstruments.hudserver.metrics.BaseBO;
import com.reconinstruments.hudserver.metrics.MetricUtils;
import com.reconinstruments.hudserver.metrics.MetricsBOs;
import com.reconinstruments.os.metrics.BaseValue;
import com.reconinstruments.os.metrics.HUDMetricIDs;
import com.reconinstruments.os.metrics.MetricChangedListener;

public class SpeedPaceBO extends BaseBO implements MetricChangedListener{
    private static final boolean DEBUG = BASE_DEBUG | false;

    private static final float LOW_PASS_FILTER_FACTOR = 0.033f;
    private static final int PACE_UPDATE_SAMPLE_SIZE = 4;
    private int mNumberOfSampleSinceLastUpdate = 0;

    private BaseValue mFilteredSpeed = new BaseValue();

    public SpeedPaceBO() {
        super(HUDMetricIDs.SPEED_PACE);
        reset();
    }

    @Override
    protected void reset() {
        getMetric().addValue(0);
        mFilteredSpeed.set(0, 0);
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
                if(DEBUG) Log.d(TAG,"onValueChanged: 3D Speed is invalid");
                return;
            }

            mFilteredSpeed.set(MetricUtils.applyLowPassFilter(mFilteredSpeed.Value, value, LOW_PASS_FILTER_FACTOR), changeTime);

            if(mNumberOfSampleSinceLastUpdate <= PACE_UPDATE_SAMPLE_SIZE){
                mNumberOfSampleSinceLastUpdate++;
                return;
            }

            mNumberOfSampleSinceLastUpdate = 0;
            float paceValue  = MetricUtils.KMPHToSecPKM(mFilteredSpeed.Value);
            getMetric().addValue(paceValue, changeTime);

            if(DEBUG) Log.d(TAG,"Pace: " + getMetric().getValue() + " , TimeStamp: " + getMetric().getTimeMillisLatestChange());
        }
    }
}
