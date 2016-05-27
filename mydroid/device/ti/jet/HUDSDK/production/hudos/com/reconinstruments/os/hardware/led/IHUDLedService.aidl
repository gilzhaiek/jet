package com.reconinstruments.os.hardware.led;

/**
 * System-private API for talking to the HUDLedService
 *
 * {@hide}
 */
interface IHUDLedService {
    int blinkPowerLED(int intensity, in int[] pattern);
    int contBlinkPowerLED(int intensity, int onMs, int offMs, boolean onOff);
    int setPowerLEDBrightness(int intensity);
}
 
