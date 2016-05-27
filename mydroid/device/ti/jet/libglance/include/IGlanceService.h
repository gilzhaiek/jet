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
#ifndef IGLANCESERVICE_H
#define IGLANCESERVICE_H

#include <binder/IInterface.h>
#include <binder/Parcel.h>
#include "IGlanceClient.h"

namespace android {

class IGlanceService: public IInterface
{
public:
    DECLARE_META_INTERFACE(GlanceService);

    virtual void connect(const sp<IGlanceClient>& glanceClient) = 0;
    virtual int aheadCalibration() = 0;
    virtual int displayCalibration() = 0;
    virtual int startGlanceDetection() = 0;
    virtual int stopGlanceDetection() = 0;
};

class BnGlanceService: public BnInterface<IGlanceService>
{
public:
    virtual status_t onTransact(uint32_t code,
                                const Parcel& data,
                                Parcel* reply,
                                uint32_t flags = 0);
};

};

#endif
