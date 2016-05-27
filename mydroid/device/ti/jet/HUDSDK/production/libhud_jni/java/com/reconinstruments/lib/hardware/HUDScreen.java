package com.reconinstruments.lib.hardware;

public class HUDScreen {

    // Turns on the screen on/off
    public native int screenOn(boolean onOff);
    // Sets the screen off delay
    public native int setScreenOffDelay(int delay);
    // Retrieve the screen state
    public native int getScreenState();
    // Force the screen to be on. If stayOn is true, this puts the screen in a forced-on state
    // in which the screen will be on for at least delay number of seconds before it can be
    // turned off again. If stayOn is false, then screen may turn on/off depending on other
    // screen events.
    public native int forceScreenOn(int delay, boolean stayOn);
    // Cancel force screen timer
    public native int cancelForceScreen();
    // Sets the screen state (internal use only)
    public native int setScreenState(int state);

    public HUDScreen() {
    }
}
