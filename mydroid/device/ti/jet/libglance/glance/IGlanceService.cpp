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
#include <IGlanceService.h>

namespace android {

enum {
    CONNECT = IBinder::FIRST_CALL_TRANSACTION,
    AHEAD_CALIBRATION,
    DISPLAY_CALIBRATION,
    START_GLANCE_DETECTION,
    STOP_GLANCE_DETECTION
};

class BpGlanceService: public BpInterface<IGlanceService> {
public:
    BpGlanceService(const sp<IBinder>& impl)
        : BpInterface<IGlanceService>(impl) {
    }

    void connect(const sp<IGlanceClient> &glanceClient) {
        Parcel data, reply;
        data.writeInterfaceToken(IGlanceService::getInterfaceDescriptor());
        data.writeStrongBinder(glanceClient->asBinder());
        remote()->transact(CONNECT, data, &reply);
    }

    int aheadCalibration() {
        Parcel data, reply;
        data.writeInterfaceToken(IGlanceService::getInterfaceDescriptor());
        remote()->transact(AHEAD_CALIBRATION, data, &reply);
        return reply.readInt32();
    }

    int displayCalibration() {
        Parcel data, reply;
        data.writeInterfaceToken(IGlanceService::getInterfaceDescriptor());
        remote()->transact(DISPLAY_CALIBRATION, data, &reply);
        return reply.readInt32();
    }

    int startGlanceDetection() {
        Parcel data, reply;
        data.writeInterfaceToken(IGlanceService::getInterfaceDescriptor());
        remote()->transact(START_GLANCE_DETECTION, data, &reply);
        return reply.readInt32();
    }

    int stopGlanceDetection() {
        Parcel data, reply;
        data.writeInterfaceToken(IGlanceService::getInterfaceDescriptor());
        remote()->transact(STOP_GLANCE_DETECTION, data, &reply);
        return reply.readInt32();
    }

};

IMPLEMENT_META_INTERFACE(GlanceService, "IGlanceService");

status_t BnGlanceService::onTransact(
    uint32_t code, const Parcel& data, Parcel *reply, uint32_t flags) {
    switch(code) {
        case CONNECT: {
            CHECK_INTERFACE(IGlanceService, data, reply);
            sp<IGlanceClient> glanceClient = interface_cast<IGlanceClient>(data.readStrongBinder());
            connect(glanceClient);
            return NO_ERROR;
        } break;
        case AHEAD_CALIBRATION: {
            CHECK_INTERFACE(IGlanceService, data, reply);
            int result = aheadCalibration();
            reply->writeInt32(result);
            return NO_ERROR;
        } break;
        case DISPLAY_CALIBRATION: {
            CHECK_INTERFACE(IGlanceService, data, reply);
            int result = displayCalibration();
            reply->writeInt32(result);
            return NO_ERROR;
        } break;
        case START_GLANCE_DETECTION: {
            CHECK_INTERFACE(IGlanceService, data, reply);
            int result = startGlanceDetection();
            reply->writeInt32(result);
            return NO_ERROR;
        } break;
        case STOP_GLANCE_DETECTION: {
            CHECK_INTERFACE(IGlanceService, data, reply);
            int result = stopGlanceDetection();
            reply->writeInt32(result);
            return NO_ERROR;
        } break;
        default:
            return BBinder::onTransact(code, data, reply, flags);
    }
}

};
