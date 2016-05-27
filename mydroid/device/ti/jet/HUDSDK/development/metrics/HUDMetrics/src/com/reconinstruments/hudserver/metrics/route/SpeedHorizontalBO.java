package com.reconinstruments.hudserver.metrics.route;

import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.reconinstruments.hudserver.metrics.BaseBO;
import com.reconinstruments.hudserver.metrics.MetricLocationHandler;
import com.reconinstruments.hudserver.metrics.MetricLocationListener;
import com.reconinstruments.hudserver.metrics.MetricUtils;
import com.reconinstruments.os.metrics.HUDMetricIDs;

public class SpeedHorizontalBO extends BaseBO implements MetricLocationHandler {
    private static final boolean DEBUG = BASE_DEBUG | false;

    public SpeedHorizontalBO() {
        super(HUDMetricIDs.SPEED_HORIZONTAL);
    }

    @Override
    protected void enableMetrics() {
        MetricLocationListener.getInstance().register(this);
    }

    @Override
    protected void disableMetrics() {
        MetricLocationListener.getInstance().unregister(this);
    }

    @Override
    public void onLocationChanged(Location location, GpsStatus gpsStatus, int numOfSatsInFix) {
        long changeTime = location.getTime();		
        float horzSpeed = MetricUtils.MPSToKMPH(location.getSpeed());

        if((horzSpeed > MetricUtils.MAX_ALLOWED_HORIZONTAL_SPEED_KPH) || (!MetricUtils.signalStrongForSpeed(location))) {
            getMetric().addInvalidValue();
        }
        else {
            getMetric().addValue(horzSpeed, changeTime);
        }

        if(DEBUG) Log.d(TAG,"Horz Speed: " + getMetric().getValue() + " , TimeStamp: " + getMetric().getTimeMillisLatestChange());	}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
}
