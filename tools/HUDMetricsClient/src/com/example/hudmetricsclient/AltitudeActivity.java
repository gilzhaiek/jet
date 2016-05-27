package com.example.hudmetricsclient;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.reconinstruments.os.metrics.HUDMetricsID;

public class AltitudeActivity extends Activity {
    MetricView mPressureAlt = null;
    MetricView mCalAlt = null;
    MetricView mDeltaAlt = null;

    AltitudeModel mAltitudeModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_altitude);

        mPressureAlt = new MetricView((TextView)findViewById(R.id.AltitudePressureCurrent), (TextView)findViewById(R.id.AltitutdePressurePrevious));
        mCalAlt = new MetricView((TextView)findViewById(R.id.AltitudeCalibratedCurrent), (TextView)findViewById(R.id.AltitudeCalibratedPrevious));
        mDeltaAlt = new MetricView((TextView)findViewById(R.id.AltitudeDeltaCurrent), (TextView)findViewById(R.id.AltitudeDeltaPrevious));

        mAltitudeModel = new AltitudeModel(this);
    }

    @Override
    public void onResume(){
        super.onResume();
        mAltitudeModel.onResume();
    }

    @Override
    public void onPause(){
        super.onPause();
        mAltitudeModel.onPause();
    }

    public void UpdateValueChangeText(int metricID, float value) {
        MetricView metricView = GetData(metricID);
        if(metricView != null) {
            metricView.addValue(value);
        }
    }

    private MetricView GetData(int metricID) {
        if(metricID == HUDMetricsID.ALTITUDE_CALIBRATED){
            return mCalAlt;
        }
        else if(metricID == HUDMetricsID.ALTITUDE_DELTA){
            return mDeltaAlt;
        }
        else if(metricID == HUDMetricsID.ALTITUDE_PRESSURE){
            return mPressureAlt;
        }
        return null;
    }
}
