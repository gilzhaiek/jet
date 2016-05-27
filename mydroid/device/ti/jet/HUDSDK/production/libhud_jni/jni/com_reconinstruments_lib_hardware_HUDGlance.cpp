/*
 * Copyright (C) 2014 Recon Instruments
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
#define LOG_TAG "GLANCE_JNI"

#include <jni.h>
#include <JNIHelp.h>
#include <android_runtime/AndroidRuntime.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>

#include <utils/Log.h>

#include "Glance.h"

namespace android
{

struct fields_t {
    jclass    glanceClass;
    jmethodID postEvent;
    jfieldID  context;
};

static fields_t fields;
static Mutex sLock;

static const char* const classPathName = "com/reconinstruments/lib/hardware/HUDGlance";

class JNIGlanceContext: public GlanceListener
{
public:
    JNIGlanceContext(JNIEnv *env, jobject weak_this, jclass clazz, const sp<Glance> &glance);
    ~JNIGlanceContext() { release(); }
	sp<Glance> &getGlance() { Mutex::Autolock _l(mLock); return mGlance; }
	void release();

    virtual void event(int32_t event);

private:
    jobject mGlanceJObjectWeak;
    jclass mGlanceJClass;
    sp<Glance> mGlance;

    Mutex mLock;
};

sp<Glance> get_native_glance(JNIEnv *env, jobject thiz, JNIGlanceContext **pContext)
{
    sp<Glance> glance;
    Mutex::Autolock _l(sLock);
    JNIGlanceContext *context = reinterpret_cast<JNIGlanceContext*>(env->GetIntField(thiz, fields.context));
    if (context != NULL) {
        glance = context->getGlance();
    }
    ALOGV("get_native_glance: context=%p", context);

    if (pContext != NULL) *pContext = context;
    return glance;
}

JNIGlanceContext::JNIGlanceContext(JNIEnv *env, jobject weak_this, jclass clazz, const sp<Glance> &glance)
{
    ALOGV("Creating JNIGlanceContext %p, %p!", env, weak_this);
    mGlanceJObjectWeak = env->NewGlobalRef(weak_this);
    mGlanceJClass = (jclass)env->NewGlobalRef(clazz);
    mGlance = glance;
}

void JNIGlanceContext::release()
{
    ALOGV("release");
    Mutex::Autolock _l(mLock);
    JNIEnv *env = AndroidRuntime::getJNIEnv();

    if (mGlanceJObjectWeak != NULL) {
        env->DeleteGlobalRef(mGlanceJObjectWeak);
        mGlanceJObjectWeak = NULL;
    }
    if (mGlanceJClass != NULL) {
        env->DeleteGlobalRef(mGlanceJClass);
        mGlanceJClass = NULL;
    }
    mGlance.clear();
}

void JNIGlanceContext::event(int32_t event)
{
    ALOGV("JNIGlanceContext::event");
    Mutex::Autolock _l(mLock);
    if (mGlanceJObjectWeak == NULL) {
        ALOGE("Cannot do callback on null glance object");
        return;
    }
    JNIEnv *env = AndroidRuntime::getJNIEnv();
    if (env) {
        ALOGV("calling callback!");
        env->CallStaticVoidMethod(
            mGlanceJClass, fields.postEvent, mGlanceJObjectWeak, event);
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
    } else {
        ALOGE("No JNI env for callback, can't post event");
        return;
    }
}

static int glanceService_setup(JNIEnv *env, jobject thiz, jobject weak_this)
{
    ALOGV("setup");
    sp<Glance> g = Glance::connect();
    if (g == NULL) {
        ALOGE("Fail to create Glance service!");
        return -1;
    }

    jclass clazz = env->GetObjectClass(thiz);
    if (clazz == NULL) {
        ALOGE("Can't get glance class");
        return -1;
    }

    sp<JNIGlanceContext> context = new JNIGlanceContext(env, weak_this, clazz, g);
    context->incStrong(thiz);
    g->setListener(context);

    // Save context in field
    env->SetIntField(thiz, fields.context, (int)context.get());
    return 0;
}

static int glanceService_aheadCalibration(JNIEnv *env, jobject thiz)
{
    ALOGV("aheadCalibration");
    JNIGlanceContext *context;
    sp<Glance> g = get_native_glance(env, thiz, &context);
    if (g == 0) {
        ALOGE("Cannot find Glance");
        return -1;
    }

    return g->aheadCalibration();
}

static int glanceService_displayCalibration(JNIEnv *env, jobject thiz)
{
    ALOGV("displayCalibration");
    JNIGlanceContext *context;
    sp<Glance> g = get_native_glance(env, thiz, &context);
    if (g == 0) {
        ALOGE("Cannot find Glance");
        return -1;
    }

    return g->displayCalibration();
}

static int glanceService_startGlanceDetection(JNIEnv *env, jobject thiz)
{
    ALOGV("startGlanceDetection");
    JNIGlanceContext *context;
    sp<Glance> g = get_native_glance(env, thiz, &context);
    if (g == 0) {
        ALOGE("Cannot find Glance");
        return -1;
    }

    return g->startGlanceDetection();
}

static int glanceService_stopGlanceDetection(JNIEnv *env, jobject thiz)
{
    ALOGV("stopGlanceDetection");
    JNIGlanceContext *context;
    sp<Glance> g = get_native_glance(env, thiz, &context);
    if (g == 0) {
        ALOGE("Cannot find Glance");
        return -1;
    }

    return g->stopGlanceDetection();
}

static JNINativeMethod gMethods[] = {
    { "native_setup", "(Ljava/lang/Object;)I", (void *)glanceService_setup },
    { "native_aheadCalibration", "()I", (void *)glanceService_aheadCalibration },
    { "native_displayCalibration", "()I", (void *)glanceService_displayCalibration },
    { "native_startGlanceDetection", "()I", (void *)glanceService_startGlanceDetection },
    { "native_stopGlanceDetection", "()I", (void *)glanceService_stopGlanceDetection },
};

#define JAVA_NATIVEGLANCE_POSTEVENT_NAME "postEventFromNative"
#define JAVA_NATIVEGLANCE_CONTEXT_NAME "mNativeContext"

int register_hud_service_HUDGlance(JNIEnv *env)
{
    ALOGV("registering HUDGlance service for JNI");
    jclass glanceClass = NULL;
    fields.glanceClass = NULL;
    fields.postEvent = NULL;

    // Get the HUDGlance class
    glanceClass = env->FindClass(classPathName);
    if (glanceClass == NULL) {
        ALOGE("Can't find %s", classPathName);
        return -1;
    }
    fields.glanceClass = (jclass)env->NewGlobalRef(glanceClass);

    // Get the postEvent method
    fields.postEvent = env->GetStaticMethodID(fields.glanceClass, JAVA_NATIVEGLANCE_POSTEVENT_NAME, "(Ljava/lang/Object;I)V");
    if (fields.postEvent == NULL) {
        ALOGE("Can't find HUDGlance.%s", JAVA_NATIVEGLANCE_POSTEVENT_NAME);
        return -1;
    }

    // Get the context field
    fields.context = env->GetFieldID(fields.glanceClass, JAVA_NATIVEGLANCE_CONTEXT_NAME, "I");
    if (fields.context == NULL) {
        ALOGE("Can't find HUDGlance.%s", JAVA_NATIVEGLANCE_CONTEXT_NAME);
        return -1;
    }

    return jniRegisterNativeMethods(env, classPathName, gMethods, NELEM(gMethods));
}

};
