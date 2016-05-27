package com.reconinstruments.os.metrics;

import android.os.Bundle;

public class Bundler {
    public static final int BUNDLE_TYPE_FLOAT = 1;
    public static final int BUNDLE_TYPE_INT = 2;
    public static final int BUNDLE_TYPE_BUNDLE = 3;

    private static final String MetricIDName = "MetricID";
    private static final String ChangeTimeName = "ChangeTime";
    private static final String ValueName = "Value";
    private static final String ValueValid = "ValueValid";	

    public static void addToBundleF(Bundle bundle, BaseMetric baseMetric) {
        BaseValue metricValue = baseMetric.getBaseValue();

        bundle.putInt(MetricIDName, baseMetric.getMetricID());
        bundle.putFloat(ValueName, metricValue.Value);
        bundle.putLong(ChangeTimeName, metricValue.ChangeTime);
        bundle.putBoolean(ValueValid, metricValue.isValidFloat());
    }

    public static void onBundleReceiverF(MetricChangedListener listener, Bundle bundle) {
        listener.onValueChanged(
                bundle.getInt(MetricIDName),
                bundle.getFloat(ValueName),
                bundle.getLong(ChangeTimeName),
                bundle.getBoolean(ValueValid));
    }
}
