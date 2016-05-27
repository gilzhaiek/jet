/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.reconinstruments.camera.app;

import android.app.Application;
import android.content.Intent;

import com.reconinstruments.camera.util.CameraUtil;
import com.reconinstruments.camera.util.UsageStatistics;

public class CameraApp extends Application {
	private static final String TAG = CameraApp.class.getSimpleName();
	public static final boolean DEBUG = true;
	
	/**
	 * static final class for providing on access point for all constants.
	 * 
	 * @author patrickcho
	 *
	 */
	public static final class Constants {
		
	}

    @Override
    public void onCreate() {
        super.onCreate();
        UsageStatistics.initialize(this);
        CameraUtil.initialize(this);
        this.sendBroadcast(new Intent("com.reconinstruments.camera.action.CAMERA_STARTED"));
    }
}

