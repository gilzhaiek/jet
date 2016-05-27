package com.reconinstruments.dashboard;

import android.os.Bundle;

import com.reconinstruments.dashboard.MetricManager.MetricType;

/**
 * 
 * <code>BigWidget</code> is for the metrics object on the big view. 
 * the difference with small widget is the big widget has the icon.
 *
 */
public class BigWidget extends SmallWidget{

    private int mMetricTypeIcon; //res id for metric type icon

    public BigWidget(MetricType metricType, String value, String unit, int metricTypeIcon, String packageName, int disconnectedIcon) {
        super(metricType, value, unit, 0, packageName, disconnectedIcon); // slot = 0 for the big widget
        mMetricTypeIcon = metricTypeIcon;
    }

    public BigWidget(Bundle b) {
        super(MetricType.valueOf(b.getString("MetricType")), b.getString("Value"), b.getString("Unit"), 
                0, b.getString("PackageName"), b.getInt("DisconnectedIcon", 0));
        mMetricTypeIcon = b.getInt("MetricTypeIcon", 0);
    }

    public int getMetricTypeIcon() {
        return mMetricTypeIcon;
    }

    @Override
    public Bundle toBundle() {
        Bundle b = super.toBundle();
        b.putInt("MetricTypeIcon", mMetricTypeIcon);
        return b;
    }
}
