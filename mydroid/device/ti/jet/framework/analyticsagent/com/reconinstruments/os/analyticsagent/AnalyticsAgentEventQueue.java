package com.reconinstruments.os.analyticsagent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

public class AnalyticsAgentEventQueue {
    // constants
    private final String TAG = this.getClass().getSimpleName();

    private final static int DEFAULT_Q_SIZE = 50;
    private final boolean DEBUG = false;

    // members
    private String mAgentID;
    private int mQSize = DEFAULT_Q_SIZE;
    private AnalyticsManager mAnalyticsManager = null;
    private SendEventsTask mSendEventsTask = null;
    private Context mContext;
    private String mEventRecordQueueStr = null;
    private int mEventRecordQueueSize = 0;

    public AnalyticsAgentEventQueue(String agentID, AnalyticsManager analyticsManager, Context context) {
        mAgentID = agentID;
        mAnalyticsManager = analyticsManager;
        mContext = context;

        mEventRecordQueueStr = "";
        mEventRecordQueueSize = 0;
    }

    public void setQSize(int newQueueSize) {
        if (newQueueSize > 0) {
            mQSize = newQueueSize;
        }
    }

    public synchronized void add(String newEventRecordJSONString) throws Exception {
        if (!mEventRecordQueueStr.isEmpty()) {
            mEventRecordQueueStr += ",";    // concatinate string so as to produce valid JSON
        }
        mEventRecordQueueStr += newEventRecordJSONString;
        mEventRecordQueueSize++;

        if (DEBUG) Log.d(TAG, "" + mEventRecordQueueSize + " of " + mQSize + " - " + (long) (System.currentTimeMillis() / 1000) + ":" + String.format("%03d", (int) (System.currentTimeMillis() % 1000)) + "  Analytic saving event: " + newEventRecordJSONString);

        if (mEventRecordQueueSize >= mQSize && !mEventRecordQueueStr.isEmpty() && mSendEventsTask == null) {  // if not in process of dumping a queue to the local zip file, dump queue
            mSendEventsTask = new SendEventsTask(mEventRecordQueueStr, mEventRecordQueueSize);

            mEventRecordQueueStr = "";  // reset eventQueue
            mEventRecordQueueSize = 0;

            mSendEventsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);// required to allow parallel execution of Asynch tasks in map layer		    }
        }
    }

    private class SendEventsTask extends AsyncTask<Void, Void, String> {
        String mJsonOutput;
        int mEventCount;

        public SendEventsTask(String output, int count) {
            mJsonOutput = output;
            mEventCount = count;
        }

        protected String doInBackground(Void... voids) {
            try {
                sendEventData(Integer.toString(mEventCount).concat(":" + mJsonOutput));  // preface data with numberOfEvents:
            } catch (Exception e) {
                Log.e(TAG, "Critical failure of Analytics Agent, " + mAgentID + ". Cannot save data to service.");
            }

            return "DONE";
        }

        @Override
        protected void onCancelled(String endString) {    // shouldn't reach here as task is never cancelled (at least not by us)
            Log.d(TAG, "finished LoadNewDrawingSetTask - cancelled");
            mSendEventsTask = null;
        }

        protected void onPostExecute(String endString) {
            mSendEventsTask = null;
            mAnalyticsManager.onDataSentToService();
        }
    }

    public void sendAllContentsToServiceOnShutdown() {
        try {
            sendEventData(Integer.toString(mEventRecordQueueSize).concat(":" + mEventRecordQueueStr)); // preface data with numberOfEvents:
        } catch (Exception e) {
            Log.e(TAG, "Critical failure of Analytics Agent, " + mAgentID + ". Cannot save data to service on shutdown.");
        }
    }

    public synchronized void sendEventData(String jsonOutput) throws Exception {
        if (mAnalyticsManager != null && !jsonOutput.isEmpty()) {
            Bundle b = new Bundle();
            b.putString("data", jsonOutput);    // pass over string of concatenated json-formatted events
            if (DEBUG) Log.d(TAG, "agent " + mAgentID + " sending events to server");
            mAnalyticsManager.saveAgentData(b);
        }
    }

}
