package com.example.hudmetricsclient;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.reconinstruments.os.metrics.HUDMetricsID;

public class GradeActivity extends Activity {

    MetricView mGrade = null;

    GradeModel mGradeModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grade);

        mGrade = new MetricView((TextView)findViewById(R.id.GradeCurrent), (TextView)findViewById(R.id.GradePrevious));

        mGradeModel = new GradeModel(this);
    }

    @Override
    public void onResume(){
        super.onResume();
        mGradeModel.onResume();
    }

    @Override
    public void onPause(){
        super.onPause();
        mGradeModel.onPause();
    }

    public void UpdateValueChangeText(int metricID, float value) {
        MetricView metricView = GetData(metricID);
        if(metricView != null) {
            metricView.addValue(value);
        }
    }

    private MetricView GetData(int metricID) {
        if(metricID == HUDMetricsID.GRADE){
            return mGrade;
        }
        return null;
    }
}
