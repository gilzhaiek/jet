/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.server;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.RemoteException;
import android.os.UEventObserver;
import android.util.Slog;

/**
 * <p>PcbtemperatureService monitors the high PCB temperature alert.
 * When the high temperature happens, this service broadcasts the hot temperature event
 * to all {IntentReceivers} that are watching the {HIGH_TEMPERATURE} action.
</p>
 */
class PcbtemperatureService {
    private static final String TAG = PcbtemperatureService.class.getSimpleName();
    private static final int mWarningTemperatureLevel=70;
    private static final int mCriticalTemperatureLevel=85;

    private static final int SYSTEM_STATE_COLD = 0;
    private static final int SYSTEM_STATE_WARM = 1;
    private static final int SYSTEM_STATE_HOT = 2;

    private int mTemperatureValue;
    private final Context mContext;
    private boolean mNotification;
    public static final String mHotPCB="PCB_HOT_TEMPERATURE";
    
    public PcbtemperatureService(Context context) {
        mContext = context;
        mNotification=true;
        mPcbTemperatureObserver.startObserving(
        "NAME=PCB_TEMPERATURE");
        Slog.v(TAG,"Service registered!");
    }

    private UEventObserver mPcbTemperatureObserver = new UEventObserver() {
        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            update(Integer.parseInt(event.get("TEMPERATURE")));
        }
    };

    private synchronized final void update(int newValue) {
        mTemperatureValue=newValue;
        Slog.v(TAG,"Temperature value="+mTemperatureValue);
        processValues();
    }

    private void processValues() {
        if(mTemperatureValue>=mWarningTemperatureLevel){
            // shut down gracefully if temperature is too high
            // wait until the system has booted before attempting to display the shutdown dialog.
            if(mTemperatureValue>=mCriticalTemperatureLevel && ActivityManagerNative.isSystemReady()){
                Slog.v(TAG,"Shut down intent due to crytical pcb temperature");
                Intent intent = new Intent(Intent.ACTION_REQUEST_SHUTDOWN);
                intent.putExtra(Intent.EXTRA_KEY_CONFIRM, false);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            }else {
                if(mNotification && ActivityManagerNative.isSystemReady()){
                    mNotification=false;
                    sendIntent();
                }
            }

        }
        else{
	    if(!mNotification) {
		Slog.v(TAG,"Temperature back to normal, set mNotification to true and send intent");
		mNotification=true;
		sendIntent();
	    }
        }
    }

    private final void sendIntent() {
        Slog.v(TAG,"High PCB temperature intent");

	int currentState = SYSTEM_STATE_COLD;

	if (mTemperatureValue>=mWarningTemperatureLevel) {
	    currentState = SYSTEM_STATE_WARM;
	}

        Intent intent = new Intent(mHotPCB);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
	intent.putExtra("systemState", currentState);
	intent.putExtra("systemTemperatue", mTemperatureValue);
	try {
	    ActivityManagerNative.getDefault().broadcastIntent(
							       null, intent, null, null, Activity.RESULT_OK, null, null,
							       null, false, false, Binder.getOrigCallingUser()
							       );
	} catch (RemoteException ex) {
	}

    }
}