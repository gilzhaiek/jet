//#define LOG_NDEBUG 0
#define LOG_TAG "POWER_JNI"

#include <jni.h>
#include "JNIHelp.h"
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>

#include <utils/Log.h>

namespace android
{

#define BATTERY_PATH "/sys/class/power_supply/twl6030_battery"
#define MAINBOARD_TEMP "/sys/bus/i2c/drivers/tmp102_temp_sensor/4-0049/temperature"
#define COMPASS_PATH "/sys/class/misc/jet_sensors"
#define FREQ_SCALING_GOV_PATH "/sys/devices/system/cpu/cpu0/cpufreq"

#define NUM_GOVERNORS 7
static const char *governor_map[NUM_GOVERNORS] = { "hotplug", "interactive", "conservative",
                                                   "userspace", "powersave", "ondemand", "performance" };

//Assume file read didn't give maximum number
#define OPEN_FILE_ERROR INT_MIN//0x80000000 for our current compiler
#define READ_FILE_ERROR (INT_MIN+1)
static const char * class_name = "com/reconinstruments/lib/hardware/HUDPower";

static int readFromFile(char* path)
{
    char value[20];
    int length;

    int fd = open(path, O_RDONLY);
    if (fd < 0) {
        return OPEN_FILE_ERROR;
    }

    length=read(fd, value, 20);

    close(fd);
    if(length<=0)
        return READ_FILE_ERROR;
    return atoi(value);
}

/**
 * \return INT_MIN or (INT_MIN+1) for failure, value of size for success
 */
static int writeToFile(char* path, char* data, int size)
{
    int length;
    int fd = open(path, O_WRONLY);
    if (fd < 0) {
        return OPEN_FILE_ERROR;
    }

    length=write(fd,data,size);
    if(length!=size)
        length=0;

    close(fd);
    return length;
}

static jint getBoardTemperature_C(JNIEnv* env, jobject obj)
{
    return readFromFile((char*)MAINBOARD_TEMP);
}

static jint getBatteryVoltage_uV(JNIEnv* env, jobject obj)
{
    char path[50];

    sprintf(path,"%s/%s", BATTERY_PATH, "voltage_now");
    return readFromFile(path);
}

static jint getAverageCurrent_uA(JNIEnv* env, jobject obj)
{
    char path[50];

    sprintf(path,"%s/%s", BATTERY_PATH, "current_avg");
    return readFromFile(path);
}

static jint getCurrent_uA(JNIEnv* env, jobject obj)
{
    char path[50];

    sprintf(path,"%s/%s", BATTERY_PATH, "current_now");
    return readFromFile(path);
}

static jint getBatteryPercentage(JNIEnv* env, jobject obj)
{
    char path[50];

    sprintf(path,"%s/%s", BATTERY_PATH, "capacity");
    return readFromFile(path);
}

static jint getBatteryTemperature_C10th(JNIEnv* env, jobject obj)
{
    char path[50];

    sprintf(path,"%s/%s", BATTERY_PATH, "temp");
    return readFromFile(path);
}

/**
 * \return INT_MIN or (INT_MIN+1) for failure, 1 for success
 */
static jint setCompassTemperature(JNIEnv* env, jobject obj, jboolean isenable)
{
    char path[50];
    char enable;
    sprintf(path,"%s/%s", COMPASS_PATH, "mag_temp_enable");

    if(isenable)
        enable='1';
    else
        enable='0';
    return writeToFile(path,&enable,1);
}

static jint getCompassTemperature(JNIEnv* env, jobject obj)
{
    char path[50];

    sprintf(path,"%s/%s", COMPASS_PATH, "mag_temp");
    return readFromFile(path);
}

static jint setFreqScalingGovernor(JNIEnv* env, jobject obj, jint governor)
{
    int rc = -1;
    // Make sure the index is within range
    if (governor >= 0 && governor < NUM_GOVERNORS) {
        char path[128];
        char gov[12];
        sprintf(path, "%s/%s", FREQ_SCALING_GOV_PATH, "scaling_governor");

        strncpy(gov, governor_map[governor], sizeof(gov));
        ALOGD("Setting %s to %s", gov, path);
        rc = writeToFile(path, gov, sizeof(gov));
    }
    return rc;
}

static JNINativeMethod method_table[] = {
    { "getBoardTemperature_C", "()I", (void *) getBoardTemperature_C },
    { "getBatteryVoltage_uV", "()I", (void *) getBatteryVoltage_uV },
    { "getAverageCurrent_uA", "()I", (void *) getAverageCurrent_uA },
    { "getCurrent_uA", "()I", (void *) getCurrent_uA },
    { "getBatteryPercentage", "()I", (void *) getBatteryPercentage },
    { "getBatteryTemperature_C10th", "()I", (void *) getBatteryTemperature_C10th },
    { "setCompassTemperature", "(Z)I", (void *) setCompassTemperature },
    { "getCompassTemperature", "()I", (void *) getCompassTemperature },
    { "setFreqScalingGovernor", "(I)I", (void *)setFreqScalingGovernor },
};

int register_hud_service_HUDPower(JNIEnv *env)
{
    return jniRegisterNativeMethods(env, class_name, method_table, NELEM(method_table));
}

};
