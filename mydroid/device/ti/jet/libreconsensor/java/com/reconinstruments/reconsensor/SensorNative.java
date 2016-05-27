package com.reconinstruments.reconsensor;

/* Native Proxy; bridge between Java world and JNI
 * This is PURE INTERFACE with no logic at this level
 * Essentially a "C header file" */

class SensorNative 
{	 
	 // Session built / teardown (c-tor / d-tor)
    static native int openDevice   ();               // called at init: Opens device file ("/dev/proxmux)
    static native int closeDevice  (int session);    // close MUX device
     
    // Recon API Sensor extensions
    static native int getSensorStatus   (int session, int handle, SensorStatus status);
    static native int setReportingMode  (int session, int handle, int mode);
    
    static native int startQueue  (int session, int mask, int delay);
    static native int stopQueue   (int session, int delay);
}
