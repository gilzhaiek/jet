package com.example.hudmetricsclient;

import com.reconinstruments.os.metrics.HUDMetricIDs;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class DistanceActivity extends Activity {

    MetricView mDistanceHorizontal = null;
    MetricView mDistanceVertical = null;
    MetricView mDistance3D = null;

    DistanceModel mDistanceModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_distance);

        mDistance3D = new MetricView((TextView) findViewById(R.id.Distance3DCurrent), (TextView) findViewById(R.id.Distance3DPrevious));
        mDistanceHorizontal = new MetricView((TextView) findViewById(R.id.DistanceHorizontalCurrent), (TextView) findViewById(R.id.DistanceHorizontalPrevious));
        mDistanceVertical = new MetricView((TextView) findViewById(R.id.DistanceVerticalCurrent), (TextView) findViewById(R.id.DistanceVerticalPrevious));
        mDistanceModel = new DistanceModel(this);
    }

    @Override
    public void onResume(){
        super.onResume();
        mDistanceModel.onResume();
    }

    @Override
    public void onPause(){
        super.onPause();
        mDistanceModel.onPause();
    }

    public void UpdateValueChangeText(int metricID, float value) {
        MetricView metricView = GetData(metricID);
        if(metricView != null) {
            metricView.addValue(value);
        }
    }

    private MetricView GetData(int metricID) {
        if(metricID == HUDMetricIDs.DISTANCE_3D){
            return mDistance3D;
        }
        else if(metricID == HUDMetricIDs.DISTANCE_HORIZONTAL){
            return mDistanceHorizontal;
        }
        else if(metricID == HUDMetricIDs.DISTANCE_VERTICAL){
            return mDistanceVertical;
        }
        return null;
    }
}
