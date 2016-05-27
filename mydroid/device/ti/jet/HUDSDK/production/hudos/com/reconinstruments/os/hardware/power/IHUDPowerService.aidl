package com.reconinstruments.os.hardware.power;

/**
 * System-private API for talking to the HUDPowerService
 *
 * {@hide}
 */
interface IHUDPowerService {
    int getBatteryVoltage();
    int getAverageCurrent();
    int getCurrent();
    int getBatteryPercentage();
    int getBatteryTemperature_C();
    int getLastShutdownReason();
    int setCompassTemperature(boolean enable_disable);
    int getCompassTemperature();
    int getBoardTemperature();
    int setFreqScalingGovernor(int governor);
}

 
