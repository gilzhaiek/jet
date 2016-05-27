package com.example.hudmetricsclient;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.reconinstruments.os.metrics.HUDMetricsID;

public class SpeedActivity extends Activity {
    MetricView mSpeedHorizontal = null;
    MetricView mSpeedVertical = null;
    MetricView mSpeed3D = null;
    MetricView mPace = null;

    SpeedModel mSpeedModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed);

        mPace = new MetricView((TextView) findViewById(R.id.PaceCurrent), (TextView) findViewById(R.id.PacePrevious));
        mSpeedHorizontal = new MetricView((TextView) findViewById(R.id.SpeedHorizontalCurrent), (TextView) findViewById(R.id.SpeedHorizontalPrevious));
        mSpeed3D = new MetricView((TextView) findViewById(R.id.Speed3DCurrent), (TextView) findViewById(R.id.Speed3DPrevious));
        mSpeedVertical = new MetricView((TextView) findViewById(R.id.SpeedVerticalCurrent), (TextView) findViewById(R.id.SpeedVerticalPrevious));

        mSpeedModel = new SpeedModel(this);
    }

    @Override
    public void onResume(){
        super.onResume();
        mSpeedModel.onResume();
    }

    @Override
    public void onPause(){
        super.onPause();
        mSpeedModel.onPause();
    }

    public void UpdateValueChangeText(int metricID, float value) {
        MetricView metricView = GetData(metricID);
        if(metricView != null) {
            metricView.addValue(value);	
        }
    }

    private MetricView GetData(int metricID) {
        if(metricID == HUDMetricsID.SPEED_PACE){
            return mPace;
        }
        else if(metricID == HUDMetricsID.SPEED_HORIZONTAL){
            return mSpeedHorizontal;
        }
        else if(metricID == HUDMetricsID.SPEED_VERTICAL){
            return mSpeedVertical;
        }
        else if(metricID == HUDMetricsID.SPEED_3D){
            return mSpeed3D;
        }
        return null;
    }
}
