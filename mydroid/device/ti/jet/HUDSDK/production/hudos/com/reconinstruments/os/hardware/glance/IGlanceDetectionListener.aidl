package com.reconinstruments.os.hardware.glance;

/**
 * Listener for glance detection events.
 *
 * {@hide}
 */
oneway interface IGlanceDetectionListener {
    void onDetectEvent(boolean atDisplay);
    void onRemovalEvent(boolean removed);
}
