package com.reconinstruments.os.analyticsagent;

oneway interface IAnalyticsServiceListener {
	void onNewConfiguration(String jsonConfigString);
	void onServiceShutdown();
}