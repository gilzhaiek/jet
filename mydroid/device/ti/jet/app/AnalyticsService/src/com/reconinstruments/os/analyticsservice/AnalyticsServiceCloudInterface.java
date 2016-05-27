package com.reconinstruments.os.analyticsservice;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class AnalyticsServiceCloudInterface {
    private final String TAG = this.getClass().getSimpleName();

    protected AnalyticsServiceQueue mEventsQueue = null;
    protected Context mContext;

    public AnalyticsServiceCloudInterface(Context context, SharedPreferences sharedPref, long staleTime, long maxFileSize, String outputDropBox,
                                          int qSize, boolean resetStoredData, String configVer, String deviceSVN, String zipKey) {
        mContext = context;
        mEventsQueue = new AnalyticsServiceQueue(this, sharedPref, staleTime, maxFileSize, outputDropBox, qSize, resetStoredData, configVer, deviceSVN, zipKey);
    }

    public void storeData(String strData) {
        mEventsQueue.add(strData);        // store data locally for more efficient, periodic uploads.
    }

    public boolean netAvailable() {
        return false;
    }

    public void flushQueueToZipOnShutdown() {
        mEventsQueue.flushQueueToZipOnShutdown();
    }

}
