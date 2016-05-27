/*
 * Copyright (C) 2015 Recon Instruments
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reconinstruments.os.hardware.ashmem;

import com.reconinstruments.lib.hardware.HUDAshmemNative;

/**
 * Class that controls the Ashmem on the HUD. This class contains static functions
 * that allow a process to create a region of ashmem to share with another process.
 * {@hide}
 */
public class HUDAshmem {

    private static final String TAG = "HUDAshmem";

    /**
     * Constructor to create HUDAshmem to send from one process to another.
     * @hide
     */
    public HUDAshmem() {
    }

    /**
     * Allocate a new region of ashmem.
     *
     * @param length The size of the region to allocate in bytes.
     *
     * @return fd The file descriptor that this process can use to access the ashmem
     *            region. To pass the fd to another process, the fd needs to be wrapped
     *            in a ParcelFileDescriptor and a Bundle and sent to the other process using
     *            an Intent. On error, negative value is returned.
     */
    public static int allocate(int length) {
        return HUDAshmemNative.allocate(length);
    }

    /**
     * Write some data to ashmem region.
     *
     * @param handle The fd of the ashmem region.
     * @param data An array of data to write to the ashmem region.
     * @param length The size of data to write to the ashmem region. The caller must
     *               ensure that this does NOT exceed the size of the allocated ashmem
     *               region as pointed to by the handle. Otherwise, the behaviour of
     *               the function is undefined.
     *
     * @return 1 on success. Negative value on error.
     */
    public static int write(int handle, byte[] data, int length) {
        return HUDAshmemNative.write(handle, data, length);
    }

    /**
     * Read some data from ashmem region.
     *
     * @param handle The fd of the ashmem region.
     * @param length The size of the data to read from the ashmem region. The caller must
     *               ensure that this does NOT exceed the size of the allocated ashmem
     *               region as pointed to by the handle. To do so, for example, pass the
     *               size of the region as part of the Intent.
     *
     * @return byte[] A byte array containing the data read from the ashmem region. On error,
     *                null is returned.
     */
    public static byte[] read(int handle, int length) {
        return HUDAshmemNative.read(handle, length);
    }

    /**
     * Remove a reference to the ashmem region. Note that this may not immediately free the
     * physical memory on the file system as sending the fd via an Intent increments the fd
     * reference count. The physical memory will get freed when Android decides to perform
     * garbage collection on it.
     *
     * @param handle The fd of the ashmem region.
     *
     * @return 1 on success. Negative value on error.
     */
    public static int free(int handle) {
        return HUDAshmemNative.free(handle);
    }
}

