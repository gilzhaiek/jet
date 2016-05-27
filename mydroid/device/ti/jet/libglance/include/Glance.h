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
#ifndef GLANCE_H
#define GLANCE_H

#include "IGlanceClient.h"
#include "IGlanceService.h"

namespace android {

class GlanceListener: virtual public RefBase {
public:
    virtual void event(int32_t event) = 0;
};

class Glance: public BnGlanceClient {
public:
    ~Glance();
    static sp<Glance> connect();
    void setListener(const sp<GlanceListener> &listener);

    int aheadCalibration();
    int displayCalibration();
    int startGlanceDetection();
    int stopGlanceDetection();

    // IGlanceClient interface
    virtual void eventCallback(int32_t event);
private:
    Glance();
    static const sp<IGlanceService> &getGlanceService(sp<Glance> g);

    class DeathNotifier: public IBinder::DeathRecipient {
    public:
        DeathNotifier(sp<Glance> g) : mGlance(g) {
        }

        virtual void binderDied(const wp<IBinder> &who);
        sp<Glance> mGlance;
    };

    sp<GlanceListener> mListener;
    static sp<DeathNotifier> mDeathNotifier;
    static Mutex mLock;
    static sp<IGlanceService> mGlanceService;
    static bool mGlanceDetecting;
};

};

#endif
