package com.reconinstruments.os.analyticsagent;

import org.json.JSONException;
import org.json.JSONObject;

public class AnalyticsEventRecord {
    private final String TAG = this.getClass().getSimpleName();
    private final static String JSON_ERROR = "{ Bad JSON values }";

    public String mJSONString;

    private String mTimeStamp;
    private String mAgentID;
    private String mEventType;
    private String mData1;
    private String mData2;
    private String mData3;

    public AnalyticsEventRecord(String timeStampStr, String agentID, String eventType, String data1, String data2, String data3) {
        mTimeStamp = timeStampStr;
        mAgentID = agentID;
        mEventType = eventType;
        mData1 = data1;
        mData2 = data2;
        mData3 = data3;

        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("time_stamp", mTimeStamp);
            jsonObj.put("component", mAgentID);
            jsonObj.put("event_type", mEventType);
            jsonObj.put("data1", mData1);
            jsonObj.put("data2", mData2);
            jsonObj.put("data3", mData3);
            mJSONString = jsonObj.toString();
        } catch (JSONException e) {
            mJSONString = JSON_ERROR;
            e.printStackTrace();
        }
    }
}
