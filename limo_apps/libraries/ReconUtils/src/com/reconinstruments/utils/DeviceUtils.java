//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.utils;
import android.os.Build;

/**
 *  <code>DeviceUtils</code> provides routines to infer information
 *  about the Recon Device that the system is running on.
 *
 */
public class DeviceUtils {
    /**
     * Returns true if it is sunglasses, marketing term JET
     *
     */
    static public boolean isSun() {
        return Build.MODEL.equalsIgnoreCase("JET");
    }
    /**
     * Returns true if it is a snow2
     *
     */
    static public boolean isSnow2() {
        return Build.MODEL.equalsIgnoreCase("Snow2");
    }
    /**
     * Returns true if we are on MODLive
     *
     */
    static public boolean isLimo() {
        return Build.PRODUCT.equalsIgnoreCase("limo");
    }
    /**
     * Returns true if on Jet platform. JET, Snow2, Omni
     *
     */
    static public boolean isJetPlatform() {
        return Build.PRODUCT.contains("jet");
    }
}