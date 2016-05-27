package com.reconinstruments.os.hardware.sensors;

/**
 * An interface to receive heading location events.
 */
public interface HeadLocationListener {

    /**
     * Callback function to report heading location events.
     *
     * @param yaw Rotation around the vertical axis (in degrees).
     * @param pitch Rotation around the side-to-side axis (in degrees).
     * @param roll Rotation around the front-to-back axis (in degrees).
     */
    void onHeadLocation(float yaw, float pitch, float roll);
}
