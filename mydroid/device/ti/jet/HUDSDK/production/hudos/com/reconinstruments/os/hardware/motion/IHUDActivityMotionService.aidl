package com.reconinstruments.os.hardware.motion;

import com.reconinstruments.os.hardware.motion.IActivityMotionDetectionListener;

/**
 * System-private API for talking to the HUDActivityMotionService
 *
 * {@hide}
 */
interface IHUDActivityMotionService {
    int registerActivityMotionDetection(in IActivityMotionDetectionListener listener, in int type);
    void unregisterActivityMotionDetection(in IActivityMotionDetectionListener listener);
    int getActivityMotionDetectedEvent();
}

