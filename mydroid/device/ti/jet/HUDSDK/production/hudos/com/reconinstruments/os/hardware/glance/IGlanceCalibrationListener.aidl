package com.reconinstruments.os.hardware.glance;

/**
 * Listener for glance calibration events.
 *
 * {@hide}
 */
oneway interface IGlanceCalibrationListener {
    void onCalibrationEvent(boolean atDisplay);
}
