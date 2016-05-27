package com.reconinstruments.os.analyticsagent;

import com.reconinstruments.os.analyticsagent.IAnalyticsService;
import com.reconinstruments.os.analyticsagent.IAnalyticsServiceListener;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.SparseArray;

public class AnalyticsManager {
    private final String TAG = this.getClass().getSimpleName();

    private static final String REMOTE_SERVICE_NAME = IAnalyticsService.class.getName();
    private static final boolean DEBUG = false; // change to true to enable debugging


    private String mAgentID;
    private Handler mAgentHandler = null;
    private static IAnalyticsService mIAnalyticsService = null;
    private AnalyticsServiceListener mServiceListener = new AnalyticsServiceListener();
    private AnalyticsAgentEventQueue mEventQueue = null;
    private boolean mShuttingDown = false;

    public AnalyticsManager(String agentID, Handler handler, Context context) throws Exception {
        mAgentID = agentID;
        mAgentHandler = handler;
        mEventQueue = new AnalyticsAgentEventQueue(mAgentID, this, context);

        if (DEBUG) Log.d(TAG, "Connecting to Analytics Service by name [" + REMOTE_SERVICE_NAME + "]");
        mIAnalyticsService = IAnalyticsService.Stub.asInterface(ServiceManager.getService(REMOTE_SERVICE_NAME));

        if (mIAnalyticsService == null) {
            throw new IllegalStateException("Failed to find Analytics Service by name [" + REMOTE_SERVICE_NAME + "]");
        }

        mIAnalyticsService.registerAgent(mServiceListener, mAgentID); // register callback listener
    }

    // ============================================================
    // analytics agent client API

    public void destroy() {
        if (mIAnalyticsService != null) {
            try {
                mIAnalyticsService.unregisterAgent(mServiceListener);
                if (DEBUG) Log.d(TAG, "destroying agent " + mAgentID);
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to unregister analytics agent " + mAgentID + " with Analytics Service on destroy");
                e.printStackTrace();
            }
        }
    }

    public boolean isServiceConnected() {
        return (mIAnalyticsService != null);
    }

    public void setQSize(int newQueueSize) {
        mEventQueue.setQSize(newQueueSize);
    }

    public void sendEventToService(String eventString) throws Exception {
        synchronized (mEventQueue) {
            if (!mShuttingDown) {
                mEventQueue.add(eventString);    // store events in Q to avoid too many IPC connections with service
            }
        }
    }

    public void handleServiceShutdown() {
        if (mShuttingDown) return;   // block multiple calls to shutdown
        mShuttingDown = true;       // block further event additions

        synchronized (mEventQueue) {    // let any previous calls to sendEventToService spawn their task
            mEventQueue.sendAllContentsToServiceOnShutdown();
        }
    }

    public void onDataSentToService() {
    }


    // ============================================================
    // API methods to talk to directly to service

    public String getConfiguration() {
        if (mIAnalyticsService == null) {
            Log.w(TAG, "Analytics Service is not available.");
            return null;
        }
        try {
            return mIAnalyticsService.getConfiguration();
        } catch (Exception e) {
            return "";
        }
    }

    public void saveAgentData(Bundle data) {
        if (mIAnalyticsService == null) {
            Log.w(TAG, "Analytics Service is not available.");
            return;
        }
        try {
            mIAnalyticsService.saveAgentData(mServiceListener, data);
            if (DEBUG) Log.d(TAG, "data sent to Analytics Service.");
        } catch (Exception e) {
            Log.e(TAG, "Could not send data to AnalyticsService: " + e.getMessage());
        }
    }

    public void diagnosticSnapshotOfSystemFiles(String key) {
        if (mIAnalyticsService == null) {
            Log.w(TAG, "Analytics Service is not available.");
            return;
        }
        try {
            mIAnalyticsService.diagnosticSnapshotOfSystemFiles(key);
            if (DEBUG) Log.d(TAG, "diagnostics shapshot request to Analytics Service.");
        } catch (Exception e) {
            Log.e(TAG, "Could not send diagnostics shapshot request to AnalyticsService: " + e.getMessage());
        }

    }
    // ============================================================
    // Used for receiving configurations and data pull requests from Analytics Server.

    public class AnalyticsServiceListener extends IAnalyticsServiceListener.Stub {
        private final String TAG = this.getClass().getSimpleName();

        @Override
        public void onNewConfiguration(String jsonConfigString) {
            if (DEBUG) {
                Log.d(TAG, "analytics agent " + mAgentID + "received onNewConfiguration from service");
            }
            Message message = mAgentHandler.obtainMessage();
            message.arg1 = ServiceRequestCodes.NEW_CONFIG.ordinal();
            Bundle b = new Bundle();
            b.putString("message", jsonConfigString);
            message.setData(b);
            mAgentHandler.sendMessage(message);
        }

        @Override
        public void onServiceShutdown() {
            if (DEBUG) {
                Log.d(TAG, "analytics agent " + mAgentID + " received onServiceShutdown from service");
            }
            Message message = mAgentHandler.obtainMessage();
            message.arg1 = ServiceRequestCodes.SERVICE_SHUTDOWN.ordinal();
            mAgentHandler.sendMessage(message);
        }

    }
}
