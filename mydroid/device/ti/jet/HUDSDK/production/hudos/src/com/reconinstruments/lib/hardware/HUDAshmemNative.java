package com.reconinstruments.lib.hardware;

/**
 * Stub file.
 *
 * @hide
 */
public class HUDAshmemNative {
    public static native int allocate(int length);
    public static native int write(int handle, byte[] data, int length);
    public static native byte[] read(int handle, int length);
    public static native int free(int handle);

    public HUDAshmemNative() {
    }
}



