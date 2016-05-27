package com.reconinstruments.os.hardware.motion;

/**
 * Listener for activity motion detection events.
 *
 * {@hide}
 */
oneway interface IActivityMotionDetectionListener {
    void onDetectEvent(boolean inMotion, int type);
}
