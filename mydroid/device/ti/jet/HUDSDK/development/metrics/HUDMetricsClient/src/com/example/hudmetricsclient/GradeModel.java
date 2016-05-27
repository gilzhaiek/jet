package com.example.hudmetricsclient;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.reconinstruments.os.metrics.HUDMetricIDs;
import com.reconinstruments.os.metrics.HUDMetricManager;
import com.reconinstruments.os.metrics.MetricChangedListener;

public class GradeModel implements ServiceConnection , MetricChangedListener{
    private final static String TAG = GradeModel.class.getName();
    GradeActivity mActivity;

    HUDMetricManager mHUDMetricManager = null;
    //TODO Remove Later Because No service Connection is required later on
    Boolean mServiceConnected = false;

    public GradeModel(GradeActivity gradeActivity){
        mActivity = gradeActivity;
        mHUDMetricManager = new HUDMetricManager(mActivity, this);
    }

    @Override
    public void onValueChanged(int metricID, float value, long changeTime, boolean isValid) {
        Log.d(TAG,"onValueChanged: metricID="+metricID+" value="+value+" changeTime="+changeTime);
        mActivity.UpdateValueChangeText(metricID, value);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mServiceConnected = true;
        onResume();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        onPause();
    }

    public void onResume() {
        if(!mServiceConnected)return;
        try {
            mHUDMetricManager.registerMetricListener(this, HUDMetricIDs.GRADE);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void onPause() {
        try {
            mHUDMetricManager.unregisterMetricListener(this, HUDMetricIDs.GRADE);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}