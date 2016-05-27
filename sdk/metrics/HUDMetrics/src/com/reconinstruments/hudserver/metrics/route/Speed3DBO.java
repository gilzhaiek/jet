package com.reconinstruments.hudserver.metrics.route;

import android.util.Log;

import com.reconinstruments.hudserver.metrics.BaseBO;
import com.reconinstruments.hudserver.metrics.MetricUtils;
import com.reconinstruments.hudserver.metrics.MetricsBOs;
import com.reconinstruments.os.metrics.BaseValue;
import com.reconinstruments.os.metrics.HUDMetricIDs;
import com.reconinstruments.os.metrics.MetricChangedListener;

public class Speed3DBO extends BaseBO implements MetricChangedListener {
    private static final boolean DEBUG = BASE_DEBUG | false;

    private BaseValue mHorizSpeed = new BaseValue();

    public Speed3DBO() {
        super(HUDMetricIDs.SPEED_3D);
    }

    @Override
    protected void reset() {
        super.reset();
    }

    @Override
    protected void enableMetrics() {
        MetricsBOs.get(HUDMetricIDs.SPEED_VERTICAL).registerListener(this);
        MetricsBOs.get(HUDMetricIDs.SPEED_HORIZONTAL).registerListener(this);
    }

    @Override
    protected void disableMetrics() {
        MetricsBOs.get(HUDMetricIDs.SPEED_VERTICAL).unregisterListener(this);
        MetricsBOs.get(HUDMetricIDs.SPEED_HORIZONTAL).unregisterListener(this);
    }

    @Override
    public void onValueChanged(int metricID, float value, long changeTime, boolean isValid) {
        if(metricID == HUDMetricIDs.SPEED_HORIZONTAL){
            mHorizSpeed.set(value, changeTime);
        }
        else if(metricID == HUDMetricIDs.SPEED_VERTICAL){
            // TODO - Add support for injection of 3rd party BLE/ANT device

            // We add an invalid Horizontal speed as it wasn't updated by the GPS speed
            if(mHorizSpeed.isValidFloat() && MetricUtils.isOutOfSync(mHorizSpeed)){
                mHorizSpeed.setInvalidValue();
            }

            //If Horizontal or Vertical speed is invalid, we set 3D speed to invalid
            if(!mHorizSpeed.isValidFloat() || !isValid) {
                getMetric().addInvalidValue();
            } else {
                float speed = (float)Math.sqrt((double)(mHorizSpeed.Value * mHorizSpeed.Value + value * value));
                getMetric().addValue(speed, changeTime);
            }

            if(DEBUG) Log.d(TAG,"3DSpeed: " + getMetric().getValue() + " , TimeStamp: " + getMetric().getTimeMillisLatestChange());
        }
    }
}

