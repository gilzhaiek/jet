package com.reconinstruments.hudserver.metrics.route;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.reconinstruments.hudserver.metrics.BaseBO;
import com.reconinstruments.hudserver.metrics.MetricUtils;
import com.reconinstruments.os.metrics.HUDMetricIDs;

public class AltitudePressureBO extends BaseBO implements SensorEventListener {
    private static final boolean DEBUG = BASE_DEBUG | false;

    // Absolute range given from pressure sensor spec sheet is 260 - 1260mBar
    // Here we set the min/max pressure range wider than what the spec provides
    public final static float MAX_VALID_PRESSURE_VALUE = 2000.0f;
    public final static float MIN_VALID_PRESSURE_VALUE = 200.0f;
    private static int PRESSURE_UPDATE_RATE = 1000000; // 1 second in nanoseconds

    private Context mContext;
    private SensorManager mSensorManager;

    float mLastPressure = Float.NaN;

    public AltitudePressureBO(Context context) {
        super(HUDMetricIDs.ALTITUDE_PRESSURE);
        mContext = context;
        mSensorManager = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_PRESSURE){
            updatePressureAltitude(event.values[0], event.timestamp);
        }
    }

    private void updatePressureAltitude(float pressure, long changeTime){
        if((pressure < MIN_VALID_PRESSURE_VALUE) || (pressure > MAX_VALID_PRESSURE_VALUE)){
            Log.w(TAG, "updatePressureAltitude: unable to handle pressure value:" + pressure + " which exceed the range" + MIN_VALID_PRESSURE_VALUE + "-" + MAX_VALID_PRESSURE_VALUE);
            getMetric().addInvalidValue();
        }
        else {
            if(!MetricUtils.isValidFloat(mLastPressure)){
                mLastPressure = pressure;
            }
            else {
                mLastPressure = MetricUtils.applyLowPassFilter(mLastPressure, pressure);
            }

            float pressureAltitude = MetricUtils.convertToAltitudeInMeters(mLastPressure);
            getMetric().addValue(pressureAltitude, changeTime);
        }
        if(DEBUG)Log.d(TAG,"updatePressureAltitude: " + getMetric().getValue() + " , TimeStamp: " + getMetric().getTimeMillisLatestChange());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void enableMetrics() {
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE), PRESSURE_UPDATE_RATE);
    }

    @Override
    protected void disableMetrics() {
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void reset() {
        getMetric().reset();
        mLastPressure = Float.NaN;
    }
}
