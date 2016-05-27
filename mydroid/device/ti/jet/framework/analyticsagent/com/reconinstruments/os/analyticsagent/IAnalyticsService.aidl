package com.reconinstruments.os.analyticsagent;

import com.reconinstruments.os.analyticsagent.IAnalyticsServiceListener;
import android.os.Bundle;
import com.reconinstruments.os.analyticsagent.IAnalyticsServiceShutdownObserver;

interface IAnalyticsService {
    String getConfiguration();
    String getDataFileRepoPath();
    String getDiagnosticFileRepoPath();
    oneway void registerAgent(in IAnalyticsServiceListener listener, in String agentID);
    oneway void unregisterAgent(in IAnalyticsServiceListener listener);
    oneway void saveAgentData(in IAnalyticsServiceListener listener, in Bundle data);
    oneway void shutdown(in IAnalyticsServiceShutdownObserver observer);
    oneway void diagnosticSnapshotOfSystemFiles(in String key);
}
