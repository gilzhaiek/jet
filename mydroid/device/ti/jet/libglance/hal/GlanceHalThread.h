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
#ifndef GLANCEHALTHREAD_H
#define GLANCEHALTHREAD_H

#include <utils/Thread.h>
#include <utils/Mutex.h>
#include <utils/Condition.h>

#include "glance.h"

#define ONE_SEC_IN_NS               1000000000ll
#define ENABLE_REMOVAL_DETECTION    1

#define GLANCE_LTHRESHOLD             0       // Prox sensor lower threshold bound
#define GLANCE_UTHRESHOLD             1023    // Prox sensor upper threshold bound
#define GLANCE_REMOVAL_MULTI          12      // Used for removal detection threshold calculation
#define GLANCE_REMOVAL_ADD            21      // Used for removal detection threshold calculation

using namespace android;

class GlanceHalThread : public Thread {
public:
    GlanceHalThread(glance_event_callback cb);
#if THRESHOLD_ALGO
    GlanceHalThread(glance_event_callback cb, int ahead, int display);
#endif
    virtual ~GlanceHalThread();
    virtual bool threadLoop();
    void setDisplayState(bool state);
#if ON_CHIP
    int setThresholds(u16 low, u16 high, u8 pers);
#endif

private:
    glance_event_callback mEventCallback;
#if THRESHOLD_ALGO
    int mAhead;
    int mDisplay;
    int mAheadUThreshold;
    int mAheadLThreshold;
    int mDisplayUThreshold;
    int mDisplayLThreshold;
    bool mDisplayState;
#endif
};


#endif
