package com.reconinstruments.hudserver.metrics;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class HUDService extends Service {
    private final String TAG = this.getClass().getSimpleName();

    IHUDMetricServiceImpl mIHUDMetricServiceImpl = null;

    @Override
    public void onCreate() {
        super.onCreate();
        mIHUDMetricServiceImpl = new IHUDMetricServiceImpl(this);
        Log.d(TAG, "HUDService onCreate");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "HUDService onDestroy");
        mIHUDMetricServiceImpl.onDestory();
        mIHUDMetricServiceImpl = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "HUDService onBind");
        return mIHUDMetricServiceImpl;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "HUDService onUnbind");
        return super.onUnbind(intent);
    }
}
