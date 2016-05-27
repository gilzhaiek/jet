
package com.reconinstruments.dashboard;

import android.os.Bundle;

import com.reconinstruments.dashboard.MetricManager.MetricType;

/**
 * <code>SmallWidget</code> is for the metrics object on small view.
 */
public class SmallWidget {

    private MetricType mMetricType;
    private String mValue;
    private String mUnit;
    private int mSlot; // 0 for the big widget, small widget start from 1
    private int mDisconnectedIcon = 0; // res id for the disconnected icon, 0 means working fine currently
    private String mPackageName = "com.reconinstruments.dashboard"; // the apk package name, default value is itself.

    /**
     * Creates a new <code>SmallWidget</code> instance.
     *
     * @param metricType a <code>MetricType</code> value
     * @param value a <code>String</code> value
     * @param unit a <code>String</code> value
     * @param slot an <code>int</code> value
     * @param packageName a <code>String</code> value
     * @param disconnectedIcon an <code>int</code> value
     */
    public SmallWidget(MetricType metricType, String value, String unit, int slot,
            String packageName, int disconnectedIcon) {
        mMetricType = metricType;
        mValue = value;
        mUnit = unit;
        mSlot = slot;
        mDisconnectedIcon = disconnectedIcon;
        if (packageName != null && !packageName.isEmpty()) {
            mPackageName = packageName;
        }
    }
    /**
     * Creates a new <code>SmallWidget</code> instance.
     *
     * @param b a <code>Bundle</code> value
     */
    public SmallWidget(Bundle b) {
        mMetricType = MetricType.valueOf(b.getString("MetricType"));
        mValue = b.getString("Value");
        mUnit = b.getString("Unit");
        mSlot = b.getInt("Slot", 0);
        mDisconnectedIcon = b.getInt("DisconnectedIcon", 0);
        String packageName = b.getString("PackageName");
        if (packageName != null && !packageName.isEmpty()) {
            mPackageName = packageName;
        }
    }

    public MetricType getMetricType() {
        return mMetricType;
    }

    public String getValue() {
        return mValue;
    }

    public String getUnit() {
        return mUnit;
    }

    public int getSlot() {
        return mSlot;
    }

    public int getDisconnectedIcon() {
        return mDisconnectedIcon;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public Bundle toBundle() {
        Bundle b = new Bundle();
        b.putString("MetricType", mMetricType.name());
        b.putString("Value", mValue);
        b.putString("Unit", mUnit);
        b.putInt("Slot", mSlot);
        b.putInt("DisconnectedIcon", mDisconnectedIcon);
        b.putString("PackageName", mPackageName);
        return b;
    }

}
