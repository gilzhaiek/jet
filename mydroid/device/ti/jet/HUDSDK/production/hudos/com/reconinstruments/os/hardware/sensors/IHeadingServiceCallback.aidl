package com.reconinstruments.os.hardware.sensors;

/**
 * {@hide}
 */
oneway interface IHeadingServiceCallback {
    void onHeadLocation(float yaw, float pitch, float roll);
}
