package com.example.hudmetricsclient;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.reconinstruments.os.HUDOS;
import com.reconinstruments.os.metrics.HUDMetricsID;
import com.reconinstruments.os.metrics.HUDMetricsManager;
import com.reconinstruments.os.metrics.MetricsValueChangedListener;

public class SpeedModel implements MetricsValueChangedListener {
    private final static String TAG = SpeedModel.class.getName();
    SpeedActivity mActivity;

    HUDMetricsManager mHUDMetricsManager = null;

    public SpeedModel(SpeedActivity mainActivity){
        mActivity = mainActivity;
        mHUDMetricsManager = (HUDMetricsManager)HUDOS.getHUDService(HUDOS.HUD_METRICS_SERVICE);
    }

    @Override
    public void onMetricsValueChanged(int metricID, float value, long changeTime, boolean isValid) {
        Log.d(TAG,"onMetricsValueChanged: metricID="+metricID+" value="+value+" changeTime="+changeTime);
        mActivity.UpdateValueChangeText(metricID, value);
    }

    public void onResume(){
        try {
            mHUDMetricsManager.registerMetricsListener(this, HUDMetricsID.SPEED_HORIZONTAL);
            mHUDMetricsManager.registerMetricsListener(this, HUDMetricsID.SPEED_VERTICAL);
            mHUDMetricsManager.registerMetricsListener(this, HUDMetricsID.SPEED_3D);
            mHUDMetricsManager.registerMetricsListener(this, HUDMetricsID.SPEED_PACE);			
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void onPause(){
        try {
            mHUDMetricsManager.unregisterMetricsListener(this, HUDMetricsID.SPEED_HORIZONTAL);
            mHUDMetricsManager.unregisterMetricsListener(this, HUDMetricsID.SPEED_VERTICAL);
            mHUDMetricsManager.unregisterMetricsListener(this, HUDMetricsID.SPEED_3D);
            mHUDMetricsManager.unregisterMetricsListener(this, HUDMetricsID.SPEED_PACE);			
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
