package com.reconinstruments.os.analyticsagent;

public class AnalyticsSystemStamp {
    //	public String	mDeviceID;
    public String mTimeStamp;
    public String mSystemTemp;

    public AnalyticsSystemStamp(int sysTemp) {
        mTimeStamp = Long.toString(System.currentTimeMillis());
        mSystemTemp = String.format("%d", sysTemp);
    }
}
