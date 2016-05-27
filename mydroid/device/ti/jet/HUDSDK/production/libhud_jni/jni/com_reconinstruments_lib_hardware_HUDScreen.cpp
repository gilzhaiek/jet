//#define LOG_NDEBUG 0
#define LOG_TAG "SCREEN_JNI"

#include <jni.h>
#include "JNIHelp.h"
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>

#include <utils/Condition.h>
#include <utils/Log.h>
#include <utils/Mutex.h>

namespace android
{

// Assume file read didn't give maximum number
#define OPEN_FILE_ERROR     INT_MIN         //0x80000000 for our current compiler
#define READ_FILE_ERROR     (INT_MIN+1)

#define POWER_STATE_PATH    "/sys/power/state"
#define BL_PATH             "/sys/devices/platform/display_led/leds/lcd-backlight/brightness"

#define ONE_SEC             1000000000ll
#define ONE_MSEC            1000000ll
#define BL_TIMEOUT          (ONE_SEC * 5)
#define BL_ON_TIMEOUT_DFT   (ONE_SEC * 2)
#define FADE_OFF_DELAY      (ONE_MSEC * 300)
#define FADE_STEPS          10ll
#define FADE_STEP_DELAY     (FADE_OFF_DELAY / FADE_STEPS)
#define SCREEN_BL_DFT_VAL   102

typedef enum SCREEN_STATE {
    SCREEN_STATE_BL_OFF,
    SCREEN_STATE_POWER_OFF,
    SCREEN_STATE_PENDING_OFF,
    SCREEN_STATE_FADING_OFF,
    SCREEN_STATE_ON,
    SCREEN_STATE_FORCED_ON,
    SCREEN_STATE_FORCED_STAY_ON
} SCREEN_STATE;

static const char *class_name = "com/reconinstruments/lib/hardware/HUDScreen";

static Mutex mLock;
static Condition mCondition;
static SCREEN_STATE mState = SCREEN_STATE_ON;
static int mBlResumeVal = SCREEN_BL_DFT_VAL;
static long long mScreenOffDelay = BL_ON_TIMEOUT_DFT;
static pthread_t mTimerOnThread;
static pthread_t mTimerOffThread;
static pthread_t mFadeOffThread;
static pthread_t mTimerForceOnThread;
static bool mOnOff = false;

static int readIntFromFile(char *path) {
    int bl_val;
    FILE *fp = fopen(path, "r");

    if (fp == NULL) {
        ALOGE("Failed to open %s", path);
        return OPEN_FILE_ERROR;
    }

    fscanf(fp, "%d", &bl_val);
    fclose(fp);
    return bl_val;
}

/**
 * return INT_MIN or (INT_MIN+1) for failure, value of size for success
 */
static int writeToFile(char *path, char *data, int size)
{
    int length;
    FILE *fp = fopen(path, "w");

    if (fp == NULL) {
        ALOGE("Failed to open %s", path);
        return OPEN_FILE_ERROR;
    }

    length = fprintf(fp, "%s", data);

    if (length != size) {
        ALOGV("Write may have failed: %d, %d", length, size);
        length = 0;
    }

    fclose(fp);
    return length;
}

static void *timerOffStart(void *args) {
    Mutex::Autolock _l(mLock);
    ALOGV("Pending to turn off display system");
    status_t res = mCondition.waitRelative(mLock, BL_TIMEOUT);

    if (res == -ETIMEDOUT) {
        // Condition timed out, continue on shutting down more of the display system.
        if (mState == SCREEN_STATE_BL_OFF) {
            writeToFile((char *)POWER_STATE_PATH, (char *)"mem", strlen("mem"));
            mState = SCREEN_STATE_POWER_OFF;
        }
    }

    return 0;
}

static void *fadeOffStart(void *args) {
    Mutex::Autolock _l(mLock);
    if (mState == SCREEN_STATE_FADING_OFF) {
        // Retrieve the BL value prior to fading it off
        mBlResumeVal = readIntFromFile((char *)BL_PATH);

        // In case the BL value we read is invalid (ie. 0), use the default value
        // so at least we can turn on the screen.
        if (mBlResumeVal == 0) {
            ALOGV("Using default screen resume value");
            mBlResumeVal = SCREEN_BL_DFT_VAL;
        }

        ALOGV("Turning off BL. Resuming at: %d", mBlResumeVal);

        int cur = mBlResumeVal;
        // Calculate how much to decrement the backlight based on number of FADE_STEPS
        int dec = cur / FADE_STEPS;
        while (cur) {
            status_t res = mCondition.waitRelative(mLock, FADE_STEP_DELAY);
            if (res == -ETIMEDOUT) {
                char val[8];
                cur = (cur < dec) ? 0 : cur - dec;
                sprintf(val, "%d", cur);
                ALOGV("Setting display to %s", val);
                writeToFile((char *)BL_PATH, val, strlen(val));
            } else {
                ALOGV("Interrupted while fading!");
                return 0;
            }
        }
        mState = SCREEN_STATE_BL_OFF;

        // Start the timer off thread
        if (pthread_create(&mTimerOffThread, NULL, &timerOffStart, NULL) != 0) {
            ALOGE("Failed to create timer off thread!");
        }
    }

    return 0;
}

static void *timerOnStart(void *args) {
    Mutex::Autolock _l(mLock);
    ALOGV("Pending to turn off back light");
    status_t res = mCondition.waitRelative(mLock, mScreenOffDelay);

    if (res == -ETIMEDOUT) {
        // Condition timed out, fade off the backlight
        if (mState == SCREEN_STATE_PENDING_OFF) {
            mState = SCREEN_STATE_FADING_OFF;
            if (pthread_create(&mFadeOffThread, NULL, &fadeOffStart, NULL) != 0) {
                ALOGE("Failed to create fade off thread!");
            }
        }
    }
    return 0;
}

static void *timerForceOnStart(void *args) {
    Mutex::Autolock _l(mLock);
    int delay = *((int *) args);
    ALOGV("Force on start: %d", delay);

    status_t res = mCondition.waitRelative(mLock, (delay * ONE_SEC));

    if (res == -ETIMEDOUT) {
        if (mState == SCREEN_STATE_FORCED_ON || mState == SCREEN_STATE_FORCED_STAY_ON) {
            // Depending on last on-off event, might need to turn off the display
            if (!mOnOff) {
                mState = SCREEN_STATE_PENDING_OFF;
                // Start the timer thread
                if (pthread_create(&mTimerOnThread, NULL, &timerOnStart, NULL) != 0) {
                    ALOGE("Failed to create timer on thread");
                }
            } else {
                mState = SCREEN_STATE_ON;
            }
            ALOGV("Force screen on completed. Resuming normal operation");
        }
    }

    free(args);
    return 0;
}

static int screenService_screenOn(JNIEnv *env, jobject obj, jboolean on_off)
{
    ALOGV("screenService_screenOn(%d) - current state: %d", on_off, mState);
    mOnOff = on_off;
    if (on_off) {
        Mutex::Autolock _l(mLock);
        // Depending on state of the screen, we need to turn on the correct device
        if (mState == SCREEN_STATE_BL_OFF) {
            char val[8];

            sprintf(val, "%d", mBlResumeVal);
            writeToFile((char *)BL_PATH, (char *)val, strlen(val));

            // Don't need the timer thread anymore, kill it
            mCondition.signal();

            mState = SCREEN_STATE_ON;
            mLock.unlock();
            if (pthread_join(mTimerOffThread, NULL) != 0) {
                ALOGE("Failed to join timer off thread!");
            }
        } else if (mState == SCREEN_STATE_POWER_OFF) {
            char val[8];
            sprintf(val, "%d", mBlResumeVal);
            writeToFile((char *)BL_PATH, (char *)val, strlen(val));
            writeToFile((char *)POWER_STATE_PATH, (char *)"on", strlen("on"));

            mState = SCREEN_STATE_ON;
        } else if (mState == SCREEN_STATE_PENDING_OFF) {
            // No need to modify screen. Just kill the mTimerOnThread
            mCondition.signal();
            mState = SCREEN_STATE_ON;

            mLock.unlock();
            if (pthread_join(mTimerOnThread, NULL) != 0) {
                ALOGE("Failed to join timer on thread!");
            }
        } else if (mState == SCREEN_STATE_FADING_OFF) {
            // Stop the fade off thread.
            mCondition.signal();

            char val[8];
            sprintf(val, "%d", mBlResumeVal);
            writeToFile((char *)BL_PATH, (char *)val, strlen(val));

            mState = SCREEN_STATE_ON;
            mLock.unlock();
            if (pthread_join(mFadeOffThread, NULL) != 0) {
                ALOGE("Failed to join fade off thread!");
            }
        } else if (mState == SCREEN_STATE_FORCED_ON) {
            // Nothing to do, just reset the state if it's not already set
            mState = SCREEN_STATE_ON;
        }
    } else {
        Mutex::Autolock _l(mLock);
        if (mState == SCREEN_STATE_ON) {
            mState = SCREEN_STATE_PENDING_OFF;
            // Start the timer thread
            if (pthread_create(&mTimerOnThread, NULL, &timerOnStart, NULL) != 0) {
                ALOGE("Failed to create timer on thread");
            }
        } else if (mState == SCREEN_STATE_FORCED_STAY_ON) {
            ALOGD("Skipping screen off event");
        } else if (mState == SCREEN_STATE_FORCED_ON) {
            mCondition.signal();

            mState = SCREEN_STATE_PENDING_OFF;
            mLock.unlock();
            if (pthread_join(mTimerForceOnThread, NULL) != 0) {
                // May not be a failure since mTimerForceOnThread may not even be running.
                ALOGV("Failed to join timer force on thread!");
            }

            // Start the timer thread
            if (pthread_create(&mTimerOnThread, NULL, &timerOnStart, NULL) != 0) {
                ALOGE("Failed to create timer on thread");
            }
        }
    }
    ALOGV("screenService_screenOn - X state: %d", mState);
    return 1;
}

static int screenService_setScreenOffDelay(JNIEnv *env, jobject obj, jint delay) {
    ALOGV("setScreenOffDelay to %d ms", delay);
    mScreenOffDelay = (long long)delay * ONE_MSEC;
    return 1;
}

static int screenService_getScreenState(JNIEnv *env, jobject obj) {
    Mutex::Autolock _l(mLock);
    ALOGV("Retrieving screen state: %d", mState);
    return mState;
}

static int screenService_cancelForceScreen(JNIEnv *env, jobject obj) {
    Mutex::Autolock _l(mLock);
    if (mState == SCREEN_STATE_FORCED_STAY_ON || mState == SCREEN_STATE_FORCED_ON) {
        ALOGV("Cancel force screen on. Current state: %d", mState);
        mCondition.signal();

        mLock.unlock();
        if (pthread_join(mTimerForceOnThread, NULL) != 0) {
            ALOGE("Failed to join timer force on thread!");
        }
    }
    return 1;
}

static int screenService_forceScreenOn(JNIEnv *env, jobject obj, jint delay, jboolean stayOn) {
    ALOGV("Force screen on: %d", delay);
    {
        Mutex::Autolock _l(mLock);
        SCREEN_STATE prevState = mState;
        ALOGV("Previous state: %d", prevState);
        if (prevState == SCREEN_STATE_FORCED_STAY_ON || prevState == SCREEN_STATE_FORCED_ON) {
            ALOGV("Cancelling previous timer first");
            mCondition.signal();

            mLock.unlock();
            if (pthread_join(mTimerForceOnThread, NULL) != 0) {
                ALOGE("Failed to join timer force on thread!");
            }
        }
    }
    screenService_screenOn(env, obj, true);
    {
        Mutex::Autolock _l(mLock);
        mState = (stayOn)? SCREEN_STATE_FORCED_STAY_ON : SCREEN_STATE_FORCED_ON;
    }

    // Forcing screen on, so set the on-off flag to false so we can turn off the display
    // if we don't ever get another call to screenService_screenOn().
    mOnOff = false;

    int *arg = (int *)malloc(sizeof(*arg));
    if (arg == NULL) {
        ALOGE("Failed to allocate memory.");
        return 0;
    }
    *arg = delay;

    // Start the timer thread
    if (pthread_create(&mTimerForceOnThread, NULL, &timerForceOnStart, arg) != 0) {
        ALOGE("Failed to create timer force screen on thread");
    }
    return 1;
}

static int screenService_setScreenState(JNIEnv *env, jobject obj, jint state) {
    Mutex::Autolock _l(mLock);
    ALOGV("Setting screen state from: %d to %d", mState, state);
    mState = (SCREEN_STATE)state;
    return 1;
}

static JNINativeMethod method_table[] = {
    { "screenOn", "(Z)I", (void *) screenService_screenOn },
    { "setScreenOffDelay", "(I)I", (void *) screenService_setScreenOffDelay },
    { "getScreenState", "()I", (void *) screenService_getScreenState },
    { "forceScreenOn", "(IZ)I", (void *) screenService_forceScreenOn },
    { "cancelForceScreen", "()I", (void *) screenService_cancelForceScreen },
    { "setScreenState", "(I)I", (void *) screenService_setScreenState },
};

int register_hud_service_HUDScreen(JNIEnv *env)
{
    return jniRegisterNativeMethods(env, class_name, method_table, NELEM(method_table));
}

};
