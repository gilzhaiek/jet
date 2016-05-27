#include "JNIHelp.h"
#include "jni.h"
#include "utils/Log.h"

namespace android {
int register_hud_service_HUDPower(JNIEnv* env);
int register_hud_service_HUDGlance(JNIEnv* env);
int register_hud_service_HUDScreen(JNIEnv* env);
int register_hud_HUDAshmemNative(JNIEnv* env);
};

using namespace android;

extern "C" jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jint result = -1;

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        ALOGE("GetEnv failed!");
        return result;
    }
    ALOG_ASSERT(env, "Could not retrieve the env!");

    register_hud_service_HUDPower(env);
    register_hud_service_HUDGlance(env);
    register_hud_service_HUDScreen(env);
    register_hud_HUDAshmemNative(env);

    return JNI_VERSION_1_6;
}
