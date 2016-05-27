package com.reconinstruments.os.analyticsagent;

oneway interface IAnalyticsServiceShutdownObserver {
	oneway void onShutDownComplete(in int statusCode);
}