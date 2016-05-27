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
package com.reconinstruments.os.hardware.motion;

/**
 * Used for receiving notifications from the HUDActivityMotionManager when an activity motion
 * detection event occurs.
 *
 * {@hide}
 */
public interface ActivityMotionDetectionListener {

    /**
     * Called when a detection event occurs within the context of the current activity.
     *
     * @param inMotion Indicates if the HUD to be detected is in motion. If true, the HUD is
     *                 in motion; otherwise, the HUD is detected to be stationary.
     * @param type The type of activity.
     */
    public void onDetectEvent(boolean inMotion, int type);
}
