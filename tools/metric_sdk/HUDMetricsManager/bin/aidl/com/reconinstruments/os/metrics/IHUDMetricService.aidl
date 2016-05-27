package com.reconinstruments.os.metrics;

import com.reconinstruments.os.metrics.IHUDMetricListener;
import com.reconinstruments.os.metrics.BaseValue;

interface IHUDMetricService {
	BaseValue getMetricValue(int groupID, int metricID);
	oneway void registerMetricListener(in IHUDMetricListener listener, int listenerID, int groupID, int metricID);
	oneway void unregisterMetricListener(in IHUDMetricListener listener, int listenerID, int groupID, int metricID);
}