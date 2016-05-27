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

package com.reconinstruments.os.hudremote;

/**
 * Used for receiving events from the HUDRemoteManager when a BLE HUD remote
 * event occurs.
 * @hide
 */
public interface HUDRemoteListener {
    /**
     * Called when a HUD remote has been registered.
     *
     * @param state The BLE address of the HUD remote.
     */
    public void onHUDRemoteRegistered(int state);

    /**
     * Called when a HUD remote has been found via a BLE scan.
     *
     * @param address The BLE address of the HUD remote.
     */
    public void onHUDRemoteScan(String address);

    /**
     * Called when a HUD remote is connected or disconnected
     *
     * @param state 1 for connected and 0 for disconnected
     */
    public void onHUDRemoteConnectionChange(int state);

    /**
     * Called when a HUD get keypress event from the remote
     *
     * @param value keypress value
     */
    public void onHUDRemoteNotify(int value);
}
