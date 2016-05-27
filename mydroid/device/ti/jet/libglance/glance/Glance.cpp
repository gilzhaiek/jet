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
#define LOG_TAG "Glance"
#include <utils/Log.h>
#include <binder/IPCThreadState.h>
#include <binder/IServiceManager.h>
#include <binder/IMemory.h>

#include <Glance.h>

#define MAX_NUM_CNX_RETRIES 3
#define ERROR -1

namespace android {


Mutex Glance::mLock;
sp<IGlanceService> Glance::mGlanceService;
sp<Glance::DeathNotifier> Glance::mDeathNotifier;
bool Glance::mGlanceDetecting;

const sp<IGlanceService>& Glance::getGlanceService(sp<Glance> g) {
    Mutex::Autolock _t(mLock);
    if (mGlanceService.get() == 0) {
        sp<IServiceManager> sm = defaultServiceManager();
        sp<IBinder> binder;

        int num = 0;
        // Continuously wait for glanceserver
        while (num < MAX_NUM_CNX_RETRIES) {
            binder = sm->getService(String16("glanceserver"));
            if (binder != 0) {
                if (mDeathNotifier == NULL && g != NULL) {
                    mDeathNotifier = new DeathNotifier(g);
                }
                binder->linkToDeath(mDeathNotifier);
                mGlanceService = interface_cast<IGlanceService>(binder);
                break;
            }
            ALOGW("GlanceService not published, waiting...try: %d", num);
            num++;
        }
    }

    ALOGE_IF(mGlanceService == 0, "No GlanceService!");
    return mGlanceService;
}

Glance::Glance() {
}

Glance::~Glance() {
}

sp<Glance> Glance::connect() {
    sp<Glance> g = new Glance();
    const sp<IGlanceService> &gs = getGlanceService(g);

    if (gs == 0) {
        ALOGE("Failed to get glance service");
        g.clear();
        return 0;
    }

    gs->connect(g);

    return g;
}

void Glance::setListener(const sp<GlanceListener> &listener) {
    Mutex::Autolock _l(mLock);
    mListener = listener;
}

int Glance::aheadCalibration() {
    const sp<IGlanceService> &gs = getGlanceService(NULL);
    if (gs == 0) {
        ALOGE("Failed to get glance service");
        return ERROR;
    }
    return gs->aheadCalibration();
}

int Glance::displayCalibration() {
    const sp<IGlanceService> &gs = getGlanceService(NULL);
    if (gs == 0) {
        ALOGE("Failed to get glance service");
        return ERROR;
    }
    return gs->displayCalibration();
}

int Glance::startGlanceDetection() {
    const sp<IGlanceService> &gs = getGlanceService(NULL);
    if (gs == 0) {
        ALOGE("Failed to get glance service");
        return ERROR;
    }
    mGlanceDetecting = true;
    return gs->startGlanceDetection();
}

int Glance::stopGlanceDetection() {
    const sp<IGlanceService> &gs = getGlanceService(NULL);
    if (gs == 0) {
        ALOGE("Failed to get glance service");
        return ERROR;
    }
    mGlanceDetecting = false;
    return gs->stopGlanceDetection();
}

void Glance::eventCallback(int32_t event) {
    sp<GlanceListener> listener;
    {
        Mutex::Autolock _l(mLock);
        listener = mListener;
    }
    if (listener != NULL) {
        listener->event(event);
    }
}

void Glance::DeathNotifier::binderDied(const wp<IBinder> &who) {
    ALOGE("Binder connection with glance server died!");
    {
        Mutex::Autolock _l(Glance::mLock);
        Glance::mGlanceService.clear();
    }

    // Try again to connect to the server. Hope that it restarts
    const sp<IGlanceService> &gs = getGlanceService(NULL);
    if (gs != 0) {
        ALOGV("Reconnecting to glance server");
        gs->connect(mGlance);

        // Check to see if we're currently glance detecting. If so, restart it.
        if (Glance::mGlanceDetecting) {
            gs->startGlanceDetection();
        }
    }
}

};
