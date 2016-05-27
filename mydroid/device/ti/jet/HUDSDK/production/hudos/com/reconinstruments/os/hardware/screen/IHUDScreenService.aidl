package com.reconinstruments.os.hardware.screen;

/**
 * System-private API for talking to the HUDScreenService
 *
 * {@hide}
 */
interface IHUDScreenService {
    void screenOn(boolean onOff);
    void setScreenOffDelay(int delay);
    int getScreenState();
    void forceScreenOn(int delay, boolean stayOn);
    void cancelForceScreen();
    void setScreenState(int state);

}
