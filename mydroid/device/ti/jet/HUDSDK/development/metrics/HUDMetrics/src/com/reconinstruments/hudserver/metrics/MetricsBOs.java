package com.reconinstruments.hudserver.metrics;

import android.util.SparseArray;

public class MetricsBOs {

    protected static SparseArray<BaseBO> mBaseBOs = new SparseArray<BaseBO>();

    public static BaseBO get(int metricID) {
        return mBaseBOs.get(metricID);
    }

    public static void put(BaseBO baseBO) {
        mBaseBOs.put(baseBO.getMetricID(), baseBO);
    }
}
