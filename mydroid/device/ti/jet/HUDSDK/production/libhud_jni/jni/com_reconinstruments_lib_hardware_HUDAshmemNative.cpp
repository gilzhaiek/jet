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
//#define LOG_NDEBUG 0
#define LOG_TAG "HUDAshmemNative_JNI"

#include <jni.h>
#include "JNIHelp.h"
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/mman.h>

#include <utils/Log.h>
#include <utils/Errors.h>

#include <cutils/ashmem.h>
#include <cutils/atomic.h>

namespace android
{

static const char *mClassName = "com/reconinstruments/lib/hardware/HUDAshmemNative";
extern "C" {
    JNIEXPORT jint JNICALL Java_com_reconinstruments_lib_hardware_HUDAshmemNative_allocate(JNIEnv *env, jobject obj, jint length);
    JNIEXPORT jint JNICALL Java_com_reconinstruments_lib_hardware_HUDAshmemNative_write(JNIEnv *env, jobject obj, jint handle, jbyteArray data, jint length);
    JNIEXPORT jbyteArray JNICALL Java_com_reconinstruments_lib_hardware_HUDAshmemNative_read(JNIEnv *env, jobject obj, jint handle, jint length);
    JNIEXPORT jint JNICALL Java_com_reconinstruments_lib_hardware_HUDAshmemNative_free(JNIEnv *env, jobject obj, jint handle);
};

JNIEXPORT jint JNICALL Java_com_reconinstruments_lib_hardware_HUDAshmemNative_allocate(JNIEnv *env, jobject obj, jint length) {
    ALOGV("native_allocate");
    int fd = ashmem_create_region("HUDAshmemNative", length);
    if (fd < 0) return NO_MEMORY;

    int result = ashmem_set_prot_region(fd, PROT_READ|PROT_WRITE);
    if (result < 0) {
        return result;
    }
    ALOGV("native_allocate X - fd: %d", fd);
    return fd;
}

JNIEXPORT jint JNICALL Java_com_reconinstruments_lib_hardware_HUDAshmemNative_write(JNIEnv *env, jobject obj, jint handle, jbyteArray data, jint length) {
    ALOGV("native_write - handle: %d, length: %d", handle, length);
    int rc = 1;
    void *ptr = mmap(NULL, length, (PROT_READ|PROT_WRITE), MAP_SHARED, handle, 0);
    if (ptr == MAP_FAILED) {
        rc = -errno;
    } else {
        void *bytes = (void *)env->GetByteArrayElements(data, NULL);
        const int len = env->GetArrayLength(data);

        // Make sure the lengths match
        if (len == length) {
            // Write to the shared memory
            memcpy(ptr, bytes, len);
        } else {
            ALOGE("native_write: length of data and length must be the same (%d, %d)", len, length);
            rc = -1;
        }

        // Clean up
        munmap(ptr, len);
        env->ReleaseByteArrayElements(data, (jbyte *)bytes, 0);
    }

    ALOGV("native_write X");
    return rc;
}

JNIEXPORT jbyteArray JNICALL Java_com_reconinstruments_lib_hardware_HUDAshmemNative_read(JNIEnv *env, jobject obj, jint handle, jint length) {
    ALOGV("native_read - handle: %d, length: %d", handle, length);
    //int fd = dup(handle);
    void *ptr = mmap(NULL, length, PROT_READ, MAP_SHARED, handle, 0);
    if (ptr == MAP_FAILED) {
        ALOGE("native_read: mmap failed with errno: %d", -errno);
        return NULL;
    } else {
        // Create a byte array to return back to caller
        jbyteArray bytes = env->NewByteArray(length);

        // Copy the contents of the read data to the byte array
        env->SetByteArrayRegion(bytes, 0, length, (jbyte *)ptr);

        // Clean up
        munmap(ptr, length);
        close(handle);

        ALOGV("native_read X");
        return bytes;
    }
}

JNIEXPORT jint JNICALL Java_com_reconinstruments_lib_hardware_HUDAshmemNative_free(JNIEnv *env, jobject obj, jint handle) {
    ALOGV("native_free - handle: %d", handle);
    int rc = -1;
    int fd = android_atomic_or(-1, &handle);

    if (fd >= 0) {
        close(fd);
        rc = 1;
    }
    ALOGV("native_free X - rc: %d", rc);
    return rc;
}

static JNINativeMethod method_table[] = {
    { "allocate", "(I)I", (void *)Java_com_reconinstruments_lib_hardware_HUDAshmemNative_allocate },
    { "write", "(I[BI)I", (void *)Java_com_reconinstruments_lib_hardware_HUDAshmemNative_write },
    { "read", "(II)[B", (void *)Java_com_reconinstruments_lib_hardware_HUDAshmemNative_read },
    { "free", "(I)I", (void *)Java_com_reconinstruments_lib_hardware_HUDAshmemNative_free },
};

int register_hud_HUDAshmemNative(JNIEnv *env) {
    ALOGI("Registering JNINativeMethod for HUDAshmemNative");
    return jniRegisterNativeMethods(env, mClassName, method_table, NELEM(method_table));
}

};
