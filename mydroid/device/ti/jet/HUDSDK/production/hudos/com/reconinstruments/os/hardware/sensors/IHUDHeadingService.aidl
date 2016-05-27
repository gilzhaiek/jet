package com.reconinstruments.os.hardware.sensors;
import com.reconinstruments.os.hardware.sensors.IHeadingServiceCallback;

/**
 * {@hide}
 */
interface IHUDHeadingService {
    void register(IHeadingServiceCallback callback);
    void unregister(IHeadingServiceCallback callback);
}
