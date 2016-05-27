package com.reconinstruments.os.hardware.glance;

import com.reconinstruments.os.hardware.glance.IGlanceDetectionListener;
import com.reconinstruments.os.hardware.glance.IGlanceCalibrationListener;

/**
 * System-private API for talking to the HUDGlanceService
 *
 * {@hide}
 */
interface IHUDGlanceService {
    int aheadCalibration();
    int displayCalibration();
    int registerGlanceDetection(in IGlanceDetectionListener listener);
    void unregisterGlanceDetection(in IGlanceDetectionListener listener);
    void registerGlanceCalibration(in IGlanceCalibrationListener listener);
    void unregisterGlanceCalibration(in IGlanceCalibrationListener listener);
}

