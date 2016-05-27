package com.reconinstruments.bletest;
import android.util.Log;
public class BLELog {
    public static final void d (String t, String s) {
	if (BLEDebugSettings.shouldLog)
	    android.util.Log.d(t,s);
    }
    public static final void v (String t, String s) {
    if (BLEDebugSettings.shouldLog)
    	android.util.Log.v(t,s);
    }
}
