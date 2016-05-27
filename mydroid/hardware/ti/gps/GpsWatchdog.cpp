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
//#define LOG_NDEBUG 0
#define LOG_TAG "GpsWatchdog"
#include <utils/Log.h>

#include "GpsWatchdog.h"

using namespace android;

GpsWatchdog::GpsWatchdog(GPSResponseHandler *pResponseHandler, int mask) :
    mpResponseHandler(pResponseHandler), mErrorDetected(false), mFirstPingMask(mask) {
    ALOGI("First ping mask: 0x%x", mFirstPingMask);
}

GpsWatchdog::~GpsWatchdog() {
}

void GpsWatchdog::pingWatchdog(int ping) {
    Mutex::Autolock _l(mLock);
    mCondition.signal();

    // The mFirstPingMask keeps track of the first ping received of the enabled pings.
    // Once the first ping of a particular ping type is received, the corresponding bit
    // of the mFirstPingMask will be unset. When the mFirstPingMask is 0, then all enabled
    // pings have been received.
    if (mFirstPingMask & ping) {
        mFirstPingMask &= ~(ping);
        ALOGI("First ping 0x%x. Ping mask: 0x%x", ping, mFirstPingMask);
    }
}

bool GpsWatchdog::threadLoop() {
    ALOGI("GpsWatchdog main processing thread");
    while (!exitPending()) {
        mLock.lock();

        // Until we receive the first ping from all enabled ping types, we will wait for
        // GPS_WATCHDOG_INIT_TIMEOUT seconds. Once we have received the first pings from
        // all enabled ones, then we will revert to waiting for GPS_WATCHDOG_TIMEOUT seconds.
        long long timeout = (mFirstPingMask) ? GPS_WATCHDOG_INIT_TIMEOUT : GPS_WATCHDOG_TIMEOUT;

        // Wait for ping. If we timeout, that means something has gone wrong.
        if (mCondition.waitRelative(mLock, timeout)) {
            // Check to see if we've already detected an error or if we've been signaled to shutdown
            if (!mErrorDetected && !exitPending()) {
                ALOGE("GpsWatchdog detected error!");
                // Detected error, send error to Android for recovery.
                if (mpResponseHandler != NULL) {
                    mpResponseHandler->commandCompleted(GPSCommand::GPS_FATAL_ERROR, GPSStatusCode::GPS_SUCCESS);\
                }
                mErrorDetected = true;
            }
        }
        mLock.unlock();
    }
    ALOGV("Exiting GpsWatchdog threadLoop");
    return false;
}
