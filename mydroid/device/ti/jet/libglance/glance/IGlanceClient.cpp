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
#include <IGlanceClient.h>

namespace android {

enum {
    EVENT_CALLBACK= IBinder::FIRST_CALL_TRANSACTION,
};

class BpGlanceClient: public BpInterface<IGlanceClient> {
public:
    BpGlanceClient(const sp<IBinder> &impl)
        : BpInterface<IGlanceClient>(impl) {
    }

    void eventCallback(int32_t event) {
        Parcel data, reply;
        data.writeInterfaceToken(IGlanceClient::getInterfaceDescriptor());
        data.writeInt32(event);
        remote()->transact(EVENT_CALLBACK, data, &reply, IBinder::FLAG_ONEWAY);
    }
};

IMPLEMENT_META_INTERFACE(GlanceClient, "GlanceClient");

status_t BnGlanceClient::onTransact(
    uint32_t code, const Parcel &data, Parcel *reply, uint32_t flags) {
    switch(code) {
        case EVENT_CALLBACK: {
            CHECK_INTERFACE(IGlanceClient, data, reply);
            int32_t event = data.readInt32();
            eventCallback(event);
            return NO_ERROR;
        } break;
        default:
            return BBinder::onTransact(code, data, reply, flags);
    }
}

};
