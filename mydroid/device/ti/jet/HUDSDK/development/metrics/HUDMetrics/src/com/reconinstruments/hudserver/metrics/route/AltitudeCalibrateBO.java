package com.reconinstruments.hudserver.metrics.route;

import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.reconinstruments.hudserver.metrics.BaseBO;
import com.reconinstruments.hudserver.metrics.MetricLocationHandler;
import com.reconinstruments.hudserver.metrics.MetricLocationListener;
import com.reconinstruments.hudserver.metrics.MetricUtils;
import com.reconinstruments.hudserver.metrics.MetricsBOs;
import com.reconinstruments.hudserver.metrics.SmartAveragerF;
import com.reconinstruments.os.metrics.BaseValue;
import com.reconinstruments.os.metrics.HUDMetricIDs;
import com.reconinstruments.os.metrics.MetricChangedListener;

public class AltitudeCalibrateBO extends BaseBO implements MetricChangedListener, MetricLocationHandler{
    private static final boolean DEBUG = BASE_DEBUG | false;

    private BaseValue mPressurAlt = new BaseValue();
    private SmartAveragerF mAvgAltOffset = new SmartAveragerF(MetricUtils.AVG_OFFSET_SIZE);

    public AltitudeCalibrateBO() {
        super(HUDMetricIDs.ALTITUDE_CALIBRATED);
    }

    @Override
    protected void reset() {
        mPressurAlt.reset();
        mAvgAltOffset.reset();
    }

    /**
     * Inject Mock Altitude - only works in MetricUtils.USE_MOCK_METRICS is compiled with true
     * @param altitude
     */
    public void injectMockAltitude(float altitude) {
        if(MetricUtils.USE_MOCK_METRICS){
            getMetric().addValue(altitude);
        }
    }

    @Override
    protected void enableMetrics() {
        if(!MetricUtils.USE_MOCK_METRICS) {
            MetricsBOs.get(HUDMetricIDs.ALTITUDE_PRESSURE).registerListener(this);
            MetricLocationListener.getInstance().register(this);
        }
    }

    @Override
    protected void disableMetrics() {
        if(!MetricUtils.USE_MOCK_METRICS) {
            MetricsBOs.get(HUDMetricIDs.ALTITUDE_PRESSURE).unregisterListener(this);
            MetricLocationListener.getInstance().unregister(this);
        }
    }

    @Override
    public void onValueChanged(int metricID, float value, long changeTime, boolean isValid) {
        if(metricID == HUDMetricIDs.ALTITUDE_PRESSURE){
            if(isValid){
                mPressurAlt.set(value, changeTime);
            }
            if(mPressurAlt.isValidFloat()){
                getMetric().addValue(mPressurAlt.Value - mAvgAltOffset.getAverage(), changeTime);
            } else {
                getMetric().addValue(0 - mAvgAltOffset.getAverage(), changeTime);
            }
            if(DEBUG)Log.d(TAG,"Calibrated Alt: " + getMetric().getValue() + " , TimeStamp: " + getMetric().getTimeMillisLatestChange());
        }
    }

    @Override
    public void onLocationChanged(Location location, GpsStatus gpsStatus, int numOfSatsInFix) {
        if(!location.hasAltitude()){
            return; //No Altitude - nolocation is needed
        }
        if(numOfSatsInFix >= MetricUtils.MIN_SATS_CALIBRATE_PRESSURE){
            int offsetAlt = 0;
            if(mPressurAlt.isValidFloat()){
                offsetAlt = (int)(mPressurAlt.Value - (float)location.getAltitude());
            }
            else {
                offsetAlt = 0 - (int) location.getAltitude();
            }
            int repeatInAvg = numOfSatsInFix - MetricUtils.MIN_SATS_CALIBRATE_PRESSURE + 1;

            // Will repeat based on the strength	
            /*for(int i = MetricUtils.MIN_SATS_CALIBRATE_PRESSURE; i < numOfSatsInFix; i++) {
				repeatInAvg *= 2;
			}*/
            mAvgAltOffset.push(offsetAlt, repeatInAvg);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
}
