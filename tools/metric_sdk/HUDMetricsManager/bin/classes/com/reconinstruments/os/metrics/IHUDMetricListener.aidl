package com.reconinstruments.os.metrics;

oneway interface IHUDMetricListener {
	void onMetricDataChanged(int listenerID, int type, in Bundle metricData);
}