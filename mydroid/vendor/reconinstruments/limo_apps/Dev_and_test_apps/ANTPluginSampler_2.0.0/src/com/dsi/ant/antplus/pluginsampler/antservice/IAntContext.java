package com.dsi.ant.antplus.pluginsampler.antservice;
/**
 * Describe interface <code>IAntContext</code> here.
 *
 * Interfaced to be used by any service or activity that does ant
 * stuff.
 *
 */
import android.content.Context;
public interface IAntContext {
    public Context getContext();
    public void requestAccessToPcc();
}