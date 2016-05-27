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
#ifndef GLANCESERVICE_H
#define GLANCESERVICE_H

#include <binder/BinderService.h>
#include <hardware/hardware.h>

#include "IGlanceService.h"
#include "IGlanceClient.h"
#include "glance.h"

namespace android {

class GlanceService: public BinderService<GlanceService>, public BnGlanceService
{
    friend class BinderService<GlanceService>;
public:
    static char const* getServiceName() { return "glanceservice"; };

    static const int GLANCE_CALICOMPLETE_AHEAD    = 1;
    static const int GLANCE_CALICOMPLETE_DISPLAY  = 2;
    static const int GLANCE_DETECT_AHEAD          = 3;
    static const int GLANCE_DETECT_DISPLAY        = 4;

    GlanceService();
    ~GlanceService();

    virtual void connect(const sp<IGlanceClient> &glanceClient);
    virtual int aheadCalibration();
    virtual int displayCalibration();
    virtual int startGlanceDetection();
    virtual int stopGlanceDetection();

    int release();
    bool isInitialized();
    static void instantiate();

private:
    int init();
    virtual void onFirstRef();
    static void glanceHalCallback(int event);

    /**
     * Helper class for passing events to other modules.
     */
    class Client
    {
    private:
        friend class GlanceService;
        Client(const sp<IGlanceClient> &glanceClient);
        ~Client();
        const sp<IGlanceClient> &getGlanceClient() { return mGlanceClient; }
        void handleEvent(int event);

        sp<IGlanceClient> mGlanceClient;
    };
    Client *getClient() { return mClient; }

    // HAL structures
    hw_module_t *mModule;
    glance_device_t *mDevice;

    bool mInitialized;
    Client *mClient;
};

};

#endif /* GLANCESERVICE_H */
