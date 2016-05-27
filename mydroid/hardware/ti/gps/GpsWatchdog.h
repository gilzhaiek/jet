/*
 * Copyright (C) 2015 Recon Instruments
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
#ifndef GPSWATCHDOG_H
#define GPSWATCHDOG_H

#include <utils/Thread.h>
#include <utils/Condition.h>

#include "gpsresponsehandler.h"

#define ONE_SEC 1000000000ll
#define GPS_WATCHDOG_INIT_TIMEOUT (ONE_SEC * 10)
#define GPS_WATCHDOG_TIMEOUT (ONE_SEC * 10)

using namespace android;

/* The GpsWatchdog is a thread that runs and receives "pings" from other
** GPS HAL processing threads. The GpsWatchdog waits for these pings. The
** GpsWatchdog detects an error when it does not receive a ping within
** GPS_WATCHDOG_TIMEOUT seconds. When it detects an error, it reports a fatal
** error to Android.
*/
class GpsWatchdog : public Thread {
public:
    GpsWatchdog(GPSResponseHandler *pResponseHandler, int mask);
    virtual ~GpsWatchdog();
    virtual bool threadLoop();
    void pingWatchdog(int ping = 0);
private:
    Condition           mCondition;
    Mutex               mLock;
    GPSResponseHandler  *mpResponseHandler;
    bool                mErrorDetected;
    int                 mFirstPingMask;
};

#endif
