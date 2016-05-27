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
package com.reconinstruments.os.hardware.glance;

/**
 * Used for receiving notifications from the {@link com.reconinstruments.os.hardware.glance.HUDGlanceManager}
 * when a glance detection event occurs.
 * {@hide}
 */
public interface GlanceDetectionListener {

    /**
     * Called when a detection event occurs.
     *
     * @param atDisplay True to indicate display glance detected, otherwise,
     *                  to indicate ahead glance detected.
     */
    public void onDetectEvent(boolean atDisplay);

    /**
     * Called when a removal event occurs.
     *
     * @param removed True to indicate removal detected, otherwise,
     *                to indicate put on detected.
     */
    public void onRemovalEvent(boolean removed);
}
