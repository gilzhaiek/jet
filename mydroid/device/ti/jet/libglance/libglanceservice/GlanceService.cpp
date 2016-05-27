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
#define LOG_TAG "GlanceService"

#include <cutils/log.h>
#include "GlanceService.h"

#define APDS9900 "apds9900"
#define ERROR -1

namespace android {

static GlanceService *gGlanceService;

void GlanceService::instantiate() {
    GlanceService *gs = new GlanceService();
    if (gs->isInitialized()) {
        ALOGI("Adding GlanceService as a system service!");
        defaultServiceManager()->addService(String16("glanceserver"), new GlanceService());
    } else {
        ALOGE("Failed to initialize GlanceService");
    }
}

GlanceService::GlanceService() :
    mModule(NULL),
    mDevice(NULL),
    mInitialized(false),
    mClient(NULL) {
    gGlanceService = this;
    mInitialized = (init() == 1);
}

void GlanceService::onFirstRef() {
    BnGlanceService::onFirstRef();
}

GlanceService::~GlanceService() {
    release();
    free(mClient);
}

void GlanceService::connect(const sp<IGlanceClient> &glanceClient) {
    if (mClient) {
        free(mClient);
    }
    mClient = new Client(glanceClient);
}

int GlanceService::aheadCalibration() {
    if (mInitialized) {
        return mDevice->ops->ahead_calibration();
    }
    return ERROR;
}

int GlanceService::displayCalibration() {
    if (mInitialized) {
        return mDevice->ops->display_calibration();
    }
    return ERROR;
}

int GlanceService::startGlanceDetection() {
    if (mInitialized) {
        return mDevice->ops->start_glance_detection();
    }
    return ERROR;
}

int GlanceService::stopGlanceDetection() {
    if (mInitialized) {
        return mDevice->ops->stop_glance_detection();
    }
    return ERROR;
}

int GlanceService::init() {
    if (hw_get_module(GLANCE_HARDWARE_MODULE_ID, (const hw_module_t **)&mModule) < 0) {
        ALOGE("Could not load glance HAL module");
        return ERROR;
    }

    if (mModule->methods->open(mModule, APDS9900, (hw_device_t **)&mDevice) < 0) {
        ALOGE("Could not open glance device");
        return ERROR;
    }

    mDevice->ops->set_event_callback(glanceHalCallback);

    return 1;
}

int GlanceService::release() {
    free(mClient);
    return 1;
}

bool GlanceService::isInitialized() {
    return mInitialized;
}

void GlanceService::glanceHalCallback(int event) {
    ALOGV("GlanceService::glanceHalCallback: %d", event);
    Client *client = NULL;
    if (gGlanceService != NULL) {
        client = gGlanceService->getClient();
    } else {
        ALOGE("glance service null");
    }

    if (client != NULL) {
        client->handleEvent(event);
    } else {
        ALOGE("client null");
    }
}

GlanceService::Client::Client(const sp<IGlanceClient> &glanceClient) {
    mGlanceClient = glanceClient;
}

GlanceService::Client::~Client() {
    mGlanceClient.clear();
}

void GlanceService::Client::handleEvent(int event) {
    ALOGV("GlanceService::Client::handleEvent %d", event);
    sp<IGlanceClient> gc = mGlanceClient;
    if (gc != 0) {
        gc->eventCallback(event);
    }
}

};
