package com.reconinstruments.os.metrics;

/**
 * Used for receiving notifications from the HUDMetricManager when 
 * metric values have updated.
 */
public interface MetricChangedListener {
    /**
     * Called when metric values have changed.
     * <p>See {@link com.reconinstruments.os.metrics.HUDMetricIDs HUDMetricIDs}
     * for details on possible metric types.
     * 
     * @param metricID
     * @param value
     * @param changeTime
     * @param isValid
     */
    public void onValueChanged(int metricID, float value, long changeTime, boolean isValid);
}