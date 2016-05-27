#include <string.h>
#include <jni.h>
#include <asm/ioctl.h>
#include <sys/types.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include "risensors_def.h"
#include "SensorNative.h"

#define LOG_TAG  "ReconSensor.Native"

#define STATUS_DELAY_FIELD       "mDelay"
#define STATUS_MIN_DELAY_FIELD   "mMinDelay"
#define STATUS_MODE_FIELD        "mMode"
#define STATUS_ENABLED_FIELD     "mEnabled"

// logging
#if defined NDK_BUILD
	#include <android/log.h>

	#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR,   LOG_TAG, __VA_ARGS__)
	#define ALOGD(...) __android_log_print(ANDROID_LOG_DEBUG,   LOG_TAG, __VA_ARGS__)
#else
	#include <utils/Log.h>
#endif


/* Opens MUX Device */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconsensor_SensorNative_openDevice
(
   JNIEnv* env,    // environment ponter
   jobject jthis   // parent stub java class
)
{
	  char node[100];
	  int mux = 0;

      ALOGD("SensorNative_openDevice: Opening MUX Device...");

      // open MUX device
      sprintf (node, "/dev/%s", PROXMUX_DRIVER_NAME);
      mux = open (node, O_RDONLY);

	  if (mux == -1)
      {
         ALOGE("SensorNative_openDevice: Failed to open MUX Device (%s)", node);
         return 0;
      }


      // return cookie. It will be cast back to object every time
      ALOGD("SensorNative_openDevice: Successfully opened MUX Device!");

      return (jint)mux;
}



/* Closes MUX Device. context is invalidated */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconsensor_SensorNative_closeDevice
(
    JNIEnv* env,     // environment ponter
    jobject jthis,   // parent stub java class
    jint    context  // session pointer

)
{
    int mux = (int)context;
    close (mux);

    return 0;
}


/** Start Recon Addition Implementations **/


// get sensor reporting mode
JNIEXPORT jint Java_com_reconinstruments_reconsensor_SensorNative_getSensorStatus
(
   JNIEnv* env, jclass clazz,    // JNI Plumbing
   jint context,                 // open mux device handle
   jint handle,                  // Recon Sensor Handle: Java has converted from Type
   jobject statusObj             // custom data structure: Sensor status
)
{
    risensorcmd cmd;
    int iRet = 0;

    int mux = (int)context;

    ALOGD("Native recon_sensor_get_mode. Handle: [0x%x]", handle);

    // simple IOCTL
    memset(&cmd, 0, sizeof(risensorcmd) );
    cmd.handle = handle;

    iRet = ioctl (mux, PROXMUX_IOCTL_GET_STATUS, &cmd);
    if (iRet < 0)
    {
        ALOGE("PROXMUX_IOCTL_GET_STATUS error (%d)", errno);
        return -errno;
    }
    else
    {
        // Protocol: long1: current rate, long2: fastest rate, short1: enabled, short4: current mode
        jclass statusClass = (*env)->GetObjectClass(env, statusObj);

        jfieldID fld = (*env)->GetFieldID(env, statusClass, STATUS_DELAY_FIELD, "I");
        (*env)->SetIntField(env, statusObj, fld, cmd.long1);

        fld = (*env)->GetFieldID(env, statusClass, STATUS_MIN_DELAY_FIELD, "I");
        (*env)->SetIntField(env, statusObj, fld, cmd.long2);

        fld = (*env)->GetFieldID (env, statusClass, STATUS_ENABLED_FIELD, "Z");
        (*env)->SetBooleanField(env, statusObj, fld, (jboolean)cmd.short1);

        fld = (*env)->GetFieldID(env, statusClass, STATUS_MODE_FIELD, "I");
        (*env)->SetIntField(env, statusObj, fld, cmd.short4);
    }

    return 0;

}


// set sensor reporting mode
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconsensor_SensorNative_setReportingMode
(
    JNIEnv* env, jclass clazz,    // JNI Plumbing
    jint context,                 // open mux device
    jint handle,                  // Recon Sensor Handle: Java has converted from Type
    jint mode                     // mode: FIFO or CR (or others?)
)
{
    risensorcmd cmd;
    int iRet = 0;

    int mux = (int)context;

    ALOGD("Native recon_sensor_set_mode. Handle: [0x%x], mode: [%d]", handle, mode);

    // simple IOCTL
    memset(&cmd, 0, sizeof(risensorcmd) );
    cmd.handle = handle;
    cmd.short1 = (unsigned short)mode;

    iRet = ioctl (mux, PROXMUX_IOCTL_SET_MODE, &cmd);
    if (iRet < 0) ALOGE("PROXMUX_IOCTL_SET_MODE error (%d)", errno);

    return iRet;
}

// start custom kernel sensor reporting workqueue
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconsensor_SensorNative_startQueue
(
    JNIEnv* env, jclass clazz,    // JNI Plumbing
    jint context,                 // open mux device
    jint mask,                    // Recon Sensor Handle Mask
    jint delay_ms                 // queue reporting rate [ms]
)
{
    risensorcmd cmd;
    int iRet = 0;

    int mux = (int)context;

    ALOGD("Native recon_sensor_start_queue. Mask [%d], delay [%d] ms", mask, delay_ms);

    // simple IOCTL
    memset(&cmd, 0, sizeof(risensorcmd) );
    cmd.handle = mask;
    cmd.long1 = delay_ms;

    iRet = ioctl (mux, PROXMUX_IOCTL_START_QUEUE, &cmd);
    if (iRet < 0) ALOGE("PROXMUX_IOCTL_START_QUEUE error (%d)", errno);

    return iRet;
}


// stop custom kernel sensor reporting workqueue
// containing sensors are not disabled, just transferred to default queue
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconsensor_SensorNative_stopQueue
(
    JNIEnv* env, jclass clazz,   // JNI Plumbing
    jint context,                // open mux device
    jint delay_ms                // queue identifier: reporting rate [ms]
)
{
    risensorcmd cmd;
    int iRet = 0;

    int mux = (int)context;

    ALOGD("native recon_sensor_stop_queue. Reporting Rate [%d] ms", delay_ms);

    // simple IOCTL
    memset(&cmd, 0, sizeof(risensorcmd) );
    cmd.long1 = delay_ms;
    iRet = ioctl (mux, PROXMUX_IOCTL_STOP_QUEUE, &cmd);

    if (iRet < 0) ALOGE("PROXMUX_IOCTL_STOP_QUEUE error (%d)", errno);
    return iRet;
}

