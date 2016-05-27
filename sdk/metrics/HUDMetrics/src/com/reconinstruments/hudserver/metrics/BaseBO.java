package com.reconinstruments.hudserver.metrics;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.RemoteException;

import com.reconinstruments.os.metrics.BaseMetric;
import com.reconinstruments.os.metrics.IHUDMetricListener;
import com.reconinstruments.os.metrics.ListenerChangedHandler;
import com.reconinstruments.os.metrics.MetricChangedListener;

/** @hide */
public abstract class BaseBO implements ListenerChangedHandler {
    protected static final boolean BASE_DEBUG = true;

    protected final String TAG = this.getClass().getSuperclass().getSimpleName();

    protected BaseMetric mBaseMetric;

    protected BaseBO(int hudMetricID) {
        mBaseMetric = new BaseMetric(hudMetricID);
        mBaseMetric.setMetricChangedListener(this);

        MetricsBOs.put(this);
    }

    public void registerListener(MetricChangedListener listener){
        synchronized (listener) {
            getMetric().registerListener(listener);
        }
    }

    public void unregisterListener(MetricChangedListener listener){
        synchronized (listener) {
            getMetric().unregisterListener(listener);
        }
    }

    public void registerMetricListener(IHUDMetricListener listener,	int listenerID) throws RemoteException {
        synchronized (listener) {
            getMetric().register(listener, listenerID);
        }
    }

    public void unregisterMetricListener(IHUDMetricListener listener){
        synchronized (listener) {
            getMetric().unregister(listener);
        }
    }

    public BaseMetric getMetric(){
        return mBaseMetric;
    }

    public int getMetricID(){
        return getMetric().getMetricID();
    }

    @Override
    public void onServiceStatusChange(int status){
        switch (status) {
            case ListenerChangedHandler.STATUS_ENABLE_SERVICE:
                enableMetrics();
                break;
            case ListenerChangedHandler.STATUS_DISABLE_SERVICE:
                disableMetrics();
                break;
        }
    }

    /**
     * To Load call getMetric().load(persistantStats);
     * @param persistantStats
     */
    protected void load1(SharedPreferences persistantStats) {
    }

    /**
     * To Save call getMetric().save(persistantStatsEditor);
     * @param persistantStatsEditor
     */
    protected void save1(Editor persistantStatsEditor) {

    }

    /**
     * To Reset call getMetric().reset() or getMetric().addValue(0) accordingly
     */
    protected void reset() {
        getMetric().reset();
    }

    protected abstract void enableMetrics();

    protected abstract void disableMetrics();


}
