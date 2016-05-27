package com.reconinstruments.lib.hardware;

public class HUDPower {
    // Returns the Main Board Temperature in C Degrees
    public native int getBoardTemperature_C();

    // Returns the Average Current in micro amps hour for the past 10 seconds
    public native int getBatteryVoltage_uV();
    public native int getAverageCurrent_uA();
    public native int getCurrent_uA();
    public native int getBatteryPercentage();
    public native int getBatteryTemperature_C10th();
    public native int setCompassTemperature(boolean enable_disable);
    public native int getCompassTemperature();
    public native int setFreqScalingGovernor(int governor);
    public HUDPower() {
    }

    static {
        System.loadLibrary("reconinstruments_jni"); // libreconinstruments_jni.so
    }
}
