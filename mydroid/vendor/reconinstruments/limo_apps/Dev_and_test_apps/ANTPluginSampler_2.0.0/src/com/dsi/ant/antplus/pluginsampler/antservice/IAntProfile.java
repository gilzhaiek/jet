package com.dsi.ant.antplus.pluginsampler.antservice;
public interface IAntProfile {
    public void requestAccessToPcc();
    public void handleReset(int antDeviceId);
    public void releaseAccess();
}    