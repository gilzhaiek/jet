package com.reconinstruments.os.analyticsagent;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class AnalyticsEventFilter {
    // constants
    private final String TAG = this.getClass().getSimpleName();

    // members
    private String mEventType;                    // defined by clients...
    private long mTimeBetweenEventsInMS;        // minimum time between event cycles... cycles may have multiple events if mForNMilliSeconds or mForMEvents is used
    private int mForNMilliSeconds;
    private int mForMEvents;
    private long mEventCycleStartTime;        // set at beginning of each event cycle in MS
    private long mEventLastRecordedTime;            // time of last event in MS
    private int mNumEventsSeenThisCycle;

    // constructor/destroy
    public AnalyticsEventFilter(String eventType, int timeBetweenInSec, int periodTime, int periodNumEvents) {
        mEventType = eventType;
        mTimeBetweenEventsInMS = timeBetweenInSec * 1000;
        mForNMilliSeconds = periodTime * 1000;
        mForMEvents = periodNumEvents;

        mEventLastRecordedTime = 0;        // set with System.currentTimeMillis()
        mEventCycleStartTime = 0;
        mNumEventsSeenThisCycle = 0;
    }

//methods

    public boolean passesConditions() {
        long curTime = System.currentTimeMillis();
        long timeSinceCycleBeganInterval = curTime - mEventCycleStartTime;

        if (timeSinceCycleBeganInterval > mTimeBetweenEventsInMS) {    // if beyond cycle time, start new cycle - handles first time and no defined timeBetweenEvents (both default mTimeBetweenEventsInMS = 0);
            mEventCycleStartTime = curTime;
            mNumEventsSeenThisCycle = 0;
        } else {
            // check for multiple events per cycle (time limited AND number limited)
            if (mForNMilliSeconds > 0 && timeSinceCycleBeganInterval > mForNMilliSeconds) { // if beyond time limit, fail
                return false;
            }
            if (mForMEvents > 0 && mNumEventsSeenThisCycle >= mForMEvents) {                        // if beyond number limit, fail
                return false;
            }
        }

        mEventLastRecordedTime = curTime;        // record last event time
        mNumEventsSeenThisCycle++;
        return true;
    }


}
