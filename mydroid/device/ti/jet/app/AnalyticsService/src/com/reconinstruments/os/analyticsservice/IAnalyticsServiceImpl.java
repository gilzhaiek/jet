package com.reconinstruments.os.analyticsservice;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.reconinstruments.os.analyticsagent.AnalyticsManager;
import com.reconinstruments.os.analyticsagent.IAnalyticsService;
import com.reconinstruments.os.analyticsagent.IAnalyticsServiceListener;
import com.reconinstruments.os.analyticsagent.ServiceRequestCodes;
import com.reconinstruments.os.analyticsagent.IAnalyticsServiceShutdownObserver;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

public class IAnalyticsServiceImpl extends IAnalyticsService.Stub {
    private final String TAG = this.getClass().getSimpleName();
    private final static boolean DEBUG = false;
//	private final static long MAX_TIME_FOR_AGENT_FLUSH_RESPONSE_MS = 60000;		// one minute

    private HashMap<IBinder, AgentListenerTracker> mAgentListeners = new HashMap<IBinder, AgentListenerTracker>();    // tracks all agent listeners
    private HashMap<IBinder, IAnalyticsServiceListener> mResponseTracker = null;

    private AnalyticsServiceApp mParentService = null;
    private Context mContext = null;
    private Handler mHandler;
    private boolean processingUpload = false;
    private IAnalyticsServiceShutdownObserver mShutdownObserver;

    @SuppressLint("WorldReadableFiles")
    public IAnalyticsServiceImpl(AnalyticsServiceApp parentService, Handler serviceHandler) {
        mParentService = parentService;
        mContext = (Context) parentService;
        mHandler = serviceHandler;    // get a handler for the service main thread
    }

    // ========================
    // service API

    public int harvestAllAgentData() {  // only called once at shutdown
        mResponseTracker = new HashMap<IBinder, IAnalyticsServiceListener>();
        int count = 0;

        for (AgentListenerTracker listenerTracker : mAgentListeners.values()) {
            if (DEBUG)
                Log.d(TAG, "adding listener with IBinder " + listenerTracker.listener.asBinder() + " to mResponseTracker");
            mResponseTracker.put(listenerTracker.listener.asBinder(), listenerTracker.listener);            // fill mResponseTracker first with all listeners to avoid race condition with data fetch
        }
        if (DEBUG) Log.i(TAG, "ready data harvest for " + mResponseTracker.size() + " agents");
        for (AgentListenerTracker listenerTracker : mAgentListeners.values()) {
            try {
                listenerTracker.listener.onServiceShutdown();
                count ++;
            } catch (Exception e) {
                // TODO how to handle error case??  - lose data
            }
        }
        return count;
    }

    private void removeListenerFromResponseTracker(IAnalyticsServiceListener listener) {
        if (DEBUG) Log.d(TAG, "Removing listener with IBinder " + listener.asBinder() + " from responseTracker");
        mResponseTracker.remove(listener.asBinder());
        if (mResponseTracker.size() == 0) {
            mResponseTracker = null;
            mParentService.onReceivedDataFromAllAgents();
        }
    }


    public void sendConfigToAgents(String newConfigStr) {
        for (AgentListenerTracker listenerTracker : mAgentListeners.values()) {
            try {
                listenerTracker.listener.onNewConfiguration(newConfigStr);
            } catch (Exception e) {
                // TODO how to handle error case??  - ignore config.. potentially get undesired data
            }
        }
    }

    // ========================
    // private support methods
    private class AgentListenerTracker implements IBinder.DeathRecipient {
        private final IAnalyticsServiceListener listener;
        private final String mAgentID;

        public AgentListenerTracker(IAnalyticsServiceListener agentListener, String agentID) {
            this.listener = agentListener;
            this.mAgentID = agentID;
        }

        public IAnalyticsServiceListener getListener() {
            return this.listener;
        }

        public void binderDied() {
            Log.i(TAG, "Client app finished without being unregistering listener " + listener);
            unregisterAgent(this.listener);
        }

    }

    //==================================
    // Analytics Agent API methods

    @Override
    public void saveAgentData(IAnalyticsServiceListener listener, Bundle data) throws RemoteException {
        if (DEBUG) Log.d(TAG, "AnalyticsService saving data from agent with listener " + listener);
        String newData = data.getString("data");
        // copy data to internal buffer, then return
        mParentService.onNewAgentData(newData);

        // check if end of a response pull
        if (mResponseTracker != null) {
            removeListenerFromResponseTracker(listener);
        }
    }

    @Override
    public String getConfiguration() throws RemoteException {
        return mParentService.mSystemConfigurationJSON;
    }

    @Override
    public String getDataFileRepoPath() throws RemoteException {
        String filePath = "";

        return filePath;
    }

    @Override
    public String getDiagnosticFileRepoPath() throws RemoteException {
        String filePath = "";

        return filePath;
    }

    @Override
    public void registerAgent(IAnalyticsServiceListener listener, String agentID) throws RemoteException {
        if (listener != null) {
            IBinder binder = listener.asBinder();
            synchronized (this.mAgentListeners) {
                if (this.mAgentListeners.containsKey(binder)) {
                    Log.w(TAG, "Ignoring duplicate listener: " + binder + " for analytics agent " + agentID);
                } else {
                    AgentListenerTracker listenerTracker = new AgentListenerTracker(listener, agentID);
                    binder.linkToDeath(listenerTracker, 0);
                    this.mAgentListeners.put(binder, listenerTracker);
                    if (DEBUG) {
                        Log.d(TAG, "Registered listener: " + binder + " for agent " + agentID);
                    }
                }
            }
        }
    }

    @Override
    public void unregisterAgent(IAnalyticsServiceListener listener) {
        if (this.mAgentListeners != null && listener != null) {
            IBinder binder = listener.asBinder();
            synchronized (this.mAgentListeners) {
                AgentListenerTracker listenerTracker = this.mAgentListeners.remove(binder);
                if (listenerTracker != null) {
                    if (DEBUG) {
                        Log.d(TAG, "Unregistered listener: " + binder);
                    }
                    binder.unlinkToDeath(listenerTracker, 0);
                }
                if (mResponseTracker != null) {
                    removeListenerFromResponseTracker(listener); // to handle (rare) race condition in case agent closes just before harvest request received
                }
            }
        }
    }

    public void shutdown(IAnalyticsServiceShutdownObserver observer) { // received from PackageManager::ShutdownThread during shutdown sequence
        mShutdownObserver = observer;

        Log.i(TAG, "Shutting down");

        mParentService.onShutdown();    // starts data harvest  and shuts down internal monitors/listeners - finished with call to endShutdown (after success or timeout)
    }

    public void endShutdown() {         // internal, called after all agent data has been harvested on shutdown
        try {
            if (DEBUG) Log.i(TAG, "  package manager callback - complete!");
            mShutdownObserver.onShutDownComplete(0); // tell PackageManager::ShutdownThread that we're done
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException when shutting down");
        }
    }

    public void diagnosticSnapshotOfSystemFiles(String key) {
        if (DEBUG) Log.e(TAG, "In diagnosticSnapshotOfSystemFiles: " + key);

        ZipParameters parameters = new ZipParameters();

        String zipFilePath = mParentService.mDiagnosticDropBox + key + ".zip";
        if (DEBUG) Log.e(TAG, " file path: " + zipFilePath);
        ZipFile zipFile = null;
        try {
            if (DEBUG) Log.i(TAG, "creating zip");
            zipFile = new ZipFile(zipFilePath);
            if (DEBUG) Log.i(TAG, "zipfile created");
        } catch (ZipException e) {
            Log.e(TAG, "Critical failure of Analytics Service diagnosticSnapshotOfSystemFiles. Cannot create zip " + zipFilePath + ". " + e.getMessage() );
            return;
        }
        if(zipFile == null) {
            Log.e(TAG, "Critical failure of Analytics Service diagnosticSnapshotOfSystemFiles. Cannot create zip " + zipFilePath + ". ");
            return;
        }

        ArrayList filesToAdd = new ArrayList();
        String logcatFilePath = mContext.getFilesDir() + "/logcatout.txt";
        File lcFile = new File(logcatFilePath);

        // snapshot logcat
        try {
            String execStr = "logcat -d -v threadtime -f " + lcFile.getAbsolutePath();
            if (DEBUG) Log.i(TAG, "writing logcat to file using " + execStr);
            Process logcatprocess = Runtime.getRuntime().exec(execStr);
            if (DEBUG) Log.i(TAG, "logcat written");

            if(lcFile.exists()) {
                filesToAdd.add(lcFile);
            }
        } catch (IOException e) {
            Log.e(TAG, "Critical failure of Analytics Service diagnosticSnapshotOfSystemFiles. Cannot create logcat image. " + e.getMessage() );
        }

        String dmesgFilePath = mContext.getFilesDir() + "/dmesgout.txt";
        File dmFile = new File(dmesgFilePath);

        // snapshot dmesg
        try {
            if (DEBUG) Log.i(TAG, "writing dmesg to file: " + dmFile.getAbsolutePath());

            Process dmesgprocess = Runtime.getRuntime().exec("dmesg");
            BufferedReader reader = new BufferedReader(new InputStreamReader(dmesgprocess.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new FileWriter(dmFile.getAbsolutePath()));
            String line;

            while ((line = reader.readLine()) != null) {
               writer.write(line);
               writer.newLine();
            }
            writer.close();
            reader.close();
            if (DEBUG) Log.i(TAG, "dmsg written to diagnostic zip file");

            if(dmFile.exists()) {
                filesToAdd.add(dmFile);
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Critical failure of Analytics Service diagnosticSnapshotOfSystemFiles. Cannot find dmesg output file. " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "Critical failure of Analytics Service diagnosticSnapshotOfSystemFiles. Cannot create dmesg output file. " + e.getMessage() );
        }

        if(filesToAdd.size() > 0) {
            try {
                parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE); // set compression method to deflate compression
                parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
                parameters.setEncryptFiles(true);
                parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES);
                parameters.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256);
                parameters.setPassword(mParentService.ZIP_ENCRYPTION_KEY);

                zipFile.addFiles(filesToAdd, parameters);
                if (DEBUG) Log.i(TAG, "logcat written to diagnostic dropbox");

            } catch (ZipException e) {
                Log.e(TAG, "Critical failure of Analytics Service diagnosticSnapshotOfSystemFiles. Cannot move diagnostics zip file to dropbox: " + e.getMessage());
            }
        }

    }


}
