package com.reconinstruments.os.metrics;

import com.reconinstruments.os.metrics.IHUDMetricListener;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;

public abstract class BaseMetricGroup {
    private final String TAG = this.getClass().getSuperclass().getSimpleName() + "." + this.getClass().getSimpleName();

    @SuppressWarnings("unused")
    private static final boolean DEBUG = true; // change to true to enable debugging 

    protected SparseArray<BaseMetric> mBaseMetrics = new SparseArray<BaseMetric>(); 

    /** @hide */
    protected BaseMetricGroup() {
    }
    /** @hide */
    public SparseArray<BaseMetric> getBaseMetrics() {
        return mBaseMetrics;
    }

    /** @hide */
    protected void putMetric(BaseMetric baseMetric) {
        mBaseMetrics.put(baseMetric.getMetricID(), baseMetric);
    }

    public BaseMetric getMetric(int hudMetricID) {
        return getMetric(hudMetricID);
    }

    protected int getMetricID(int index) {
        return mBaseMetrics.keyAt(index);
    }

    /** @hide */
    public void register(IHUDMetricListener listener, int listenerID, int metricID) throws RemoteException {
        BaseMetric baseMetric = getMetric(metricID);
        if(baseMetric == null) {
            Log.e(TAG,"register listener, metricID is null - " + metricID);
            return;
        }
        baseMetric.register(listener, listenerID);
    }

    /** @hide */
    public void unregister(IHUDMetricListener listener, int metricID) {
        BaseMetric baseMetric = getMetric(metricID);
        if(baseMetric == null) {
            Log.e(TAG,"unregister listener, metricID is null - " + metricID);
            return;
        }
        baseMetric.unregister(listener);
    }



    /** @hide */
    public abstract void load(SharedPreferences persistantStats);

    /** @hide */
    public abstract void save(Editor persistantStatsEditor);

    /** @hide */
    public abstract void reset();
}
