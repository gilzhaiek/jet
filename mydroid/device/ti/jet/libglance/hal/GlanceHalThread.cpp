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
//#define LOG_NDEBUG 0
#define LOG_TAG "GlanceHalThread"

#include <unistd.h>

#include <utils/Log.h>
#include <utils/Thread.h>

#include "GlanceHalThread.h"
#include "apds.h"
#include "glance.h"

using namespace android;

GlanceHalThread::GlanceHalThread(glance_event_callback cb) :
    mEventCallback(cb) {
}

#if THRESHOLD_ALGO
GlanceHalThread::GlanceHalThread(glance_event_callback cb, int ahead, int display) :
    mEventCallback(cb),
    mAhead(ahead),
    mDisplay(display),
    mDisplayState(false) {
    // The sensor readings from ahead calibration should be larger than the readings
    // from display calibration.
    if (mAhead < mDisplay) {
        ALOGV("Swapping mAhead(%d) and mDisplay(%d)", mAhead, mDisplay);
        int tmp_ahead = mAhead;
        mAhead = mDisplay;
        mDisplay = tmp_ahead;
    }

    // Calculate ahead and display thresholds
    int diff = mAhead - mDisplay;
    int div = diff / 10;
    mAheadUThreshold = mAhead + (div * AHEADU_PCT/10);
    mAheadLThreshold = mAhead - (div * AHEADL_PCT/10);
    mDisplayUThreshold = mDisplay + (div * DISPLAYU_PCT/10);
    mDisplayLThreshold = mDisplay - (div * DISPLAYL_PCT/10);

    // Ensure the threshold windows are within [GLANCE_LTHRESHOLD, GLANCE_UTHRESHOLD]
    if (mDisplayLThreshold < GLANCE_LTHRESHOLD) {
        mDisplayLThreshold = GLANCE_LTHRESHOLD;
    }
    if (mAheadUThreshold > GLANCE_UTHRESHOLD) {
        mAheadUThreshold = GLANCE_UTHRESHOLD;
    }

    ALOGV("mAhead: %d -> %d, mDisplay: %d -> %d", mAheadLThreshold, mAheadUThreshold, mDisplayLThreshold, mDisplayUThreshold);
}
#endif

GlanceHalThread::~GlanceHalThread() {
    ALOGV("GlanceHalThread destructor");
    mEventCallback(GLANCE_EVENT_GLANCE_STOPPED);
}

void GlanceHalThread::setDisplayState(bool state) {
    mDisplayState = state;
}

#if ON_CHIP
int GlanceHalThread::setThresholds(u16 low, u16 high, u8 pers) {
    int ret;

    // Set thresholds on chip
    u8 buf[6];

    // Set conditions of the enable register, disable the interupts if they have been enabled so an interrupt does not occur during the reset
    ret = write_register(COMMAND_MODE | APDS_ENABLE_REG, (APDS_PIEN_OFF | APDS_AIEN_OFF | APDS_WEN_ON | APDS_PEN_ON | APDS_AEN_OFF | APDS_PON));
    if (ret < 0){
        ALOGE("Write failed");
        return -EIO;
    }

    // Clear Interupt Bit
    ret = clear_interrupt(APDS_PS_ALS_CLR);
    if (ret < 0) {
        ALOGE("Write failed");
        return -EIO;
    }

    buf[0] = (COMMAND_MODE|AUTO_INCREMENT|APDS_PILTL_REG);
    // Convert decimal number given for low and high to two hex values each
    buf[1] = ((unsigned char)(low));
    buf[2] = ((unsigned char)(((unsigned int)(low) >> 8)));
    buf[3] = ((unsigned char)(high));
    buf[4] = ((unsigned char)(((unsigned int)(high)) >> 8));
    buf[5] = pers;

    ret = write_bytes(buf, 6);
    if (ret < 0) {
        ALOGE("Write failed");
        return -EIO;
    }

    // Re-enable interupts
    ret = write_register(COMMAND_MODE | APDS_ENABLE_REG, (APDS_PIEN_ON | APDS_AIEN_OFF | APDS_WEN_ON | APDS_PEN_ON | APDS_AEN_OFF | APDS_PON));
    if (ret < 0) {
        ALOGE("Write failed");
        return -EIO;
    }

    // Clear Interupt Bit
    ret = clear_interrupt(APDS_PS_ALS_CLR);
    if (ret < 0) {
        ALOGE("Write failed");
        return -EIO;
    }

    return 0;
}
#endif

bool GlanceHalThread::threadLoop() {
    int ret, old_val;
    int removal_threshold = 0;
    u8 buf[3];

#if !(THRESHOLD_ALGO)
    // Larger array of old values for comparing to in order to determine glance
    int i = 0;
    int poll_array_sum = 0;
    int poll_array_avg = 0;
    int poll_array[POLL_ARRAY_SIZE];

    // Small array of most recent values to compare against large array to determine glance
    int j = 0;
    int newest_array_sum = 0;
    int newest_array_avg = 0;
    int newest_array[NEWEST_ARRAY_SIZE];
#endif
    GLANCE_EVENT event = GLANCE_EVENT_UNKNOWN;

    // Setup sensor for continuous polling at a specified rate
    ret = write_register(COMMAND_MODE | APDS_PERS_REG, 0x00);
    if (ret < 0) {
        ALOGE("Write failed");
        return false;
    }

    // Turn on sensor with proximity and proximity interrupt enabled
    ret = write_register(COMMAND_MODE | APDS_ENABLE_REG, (APDS_PIEN_ON | APDS_AIEN_OFF | APDS_WEN_ON | APDS_PEN_ON | APDS_AEN_OFF | APDS_PON));
    if (ret < 0) {
        ALOGE("Write failed");
        return false;
    }

    ret = read_register(APDS_PPCOUNT_REG, buf);
    removal_threshold = buf[0] * GLANCE_REMOVAL_MULTI + GLANCE_REMOVAL_ADD;
    ALOGV("removal threshold: %d", removal_threshold);

#if THRESHOLD_ALGO
#if ON_CHIP
    setThresholds(GLANCE_LTHRESHOLD, mAheadLThreshold, PERS_VAL);
#endif
#else
    // Initialization fill of FIFO first
    while (i < POLL_ARRAY_SIZE) {
        ret = wait_for_interrupt(ONE_SEC);
        if (ret > 0) {
            ret = APDS_ReadProxRegister(buf);
            if (ret > 0) {
                poll_array[i] = ((buf[1] << 8) + buf[0]);
                poll_array_sum += poll_array[i];
                ALOGV("poll_array_sum = %d", poll_array_sum);
                i++;
            }

            // Clear interrupt bit
            ret = clear_interrupt(APDS_PS_ALS_CLR);
            if (ret < 0) {
                ALOGV("Write failed");
                return false;
            }
        }
    }

    while (j < NEWEST_ARRAY_SIZE) {
        ret = wait_for_interrupt(ONE_SEC);
        if (ret > 0) {
            ret = APDS_ReadProxRegister(buf);
            if (ret > 0) {
                newest_array[j] = ((buf[1] << 8) + buf[0]);
                newest_array_sum += newest_array[j];
                ALOGV("newest_array_sum: %d", newest_array_sum);
                j++;
            }

            // Clear interrupt bit
            ret = clear_interrupt(APDS_PS_ALS_CLR);
            if (ret < 0) {
                ALOGV("Write failed");
                return false;
            }
        }
    }

    // Get average of the large array
    poll_array_avg = poll_array_sum / POLL_ARRAY_SIZE;

    // Reset counters
    i = 0;
    j = 0;
#endif

    ALOGV("Begin main glance detection loop!");

    // Main glance detection loop. Continously poll sensor until we're told to stop.
    while (!exitPending()) {
#if THRESHOLD_ALGO
#if ON_CHIP
        ret = wait_for_interrupt(ONE_SEC);
        if (ret > 0) {
            ret = APDS_ReadProxRegister(buf);
            if (ret > 0) {
                int sensor_val = ((buf[1] << 8) + buf[0]);
                ALOGV("Sensor_val: %d, mDisplayState: %d", sensor_val, mDisplayState);
                // Check to see if user was previously looking at the display or not (as indicated by mDisplayState).
                // mDisplayState true means the user was looking at the display. mDisplayState false means
                // the user was looking ahead.
                if (mDisplayState == true) {
                    mDisplayState = false;
#if CONTROL_BL
                    disable_bl();
#endif
                    // Check the value of the sensor. See if it's below the removal threshold. This only occurs
                    // when we were previously looking at the display and a removal occurred.
                    if ((sensor_val < removal_threshold) && ENABLE_REMOVAL_DETECTION) {
                        ALOGV("Detected removal");
                        event = GLANCE_EVENT_REMOVED;

                        // Set new thresholds so that the next time we're interrupted,
                        // it is a put on event. Set the display state to indicate that the display
                        // was turned off. This is so that the next time we're interrupted, the
                        // mDisplayState is set so that the display gets turned on.
                        setThresholds(GLANCE_LTHRESHOLD, removal_threshold, PERS_VAL);
                    } else {
                        event = GLANCE_EVENT_GLANCE_AHEAD;
                        // The user is currently looking ahead, so look for a display glance
                        setThresholds(mDisplayUThreshold, GLANCE_UTHRESHOLD, PERS_VAL);
                    }
                } else if ((sensor_val < removal_threshold) && ENABLE_REMOVAL_DETECTION) {
                    // Check the value of the sensor. See if it's below the removal threshold. This only occurs
                    // when we were previously looking ahead and a removal occurred.
                    ALOGV("Detected removal");
#if CONTROL_BL
                    disable_bl();
#endif
                    event = GLANCE_EVENT_REMOVED;

                    // Set new thresholds so that the next time we're interrupted,
                    // it is a put on event. Set the display state to indicate that the display
                    // was turned off. This is so that the next time we're interrupted, the
                    // mDisplayState is set so that the display gets turned on.
                    setThresholds(GLANCE_LTHRESHOLD, removal_threshold, PERS_VAL);
                    mDisplayState = false;
                } else {
                    mDisplayState = true;
#if CONTROL_BL
                    enable_bl();
#endif
                    event = GLANCE_EVENT_GLANCE_DISPLAY;
                    // The user is currently looking at display, so look for a ahead glance
                    setThresholds(removal_threshold, mAheadLThreshold, PERS_VAL);
                }

                // Report event to upper layer
                if (mEventCallback && event != GLANCE_EVENT_UNKNOWN) {
                    mEventCallback(event);
                }
            }
            // Clear interrupt bit
            ret = clear_interrupt(APDS_PS_ALS_CLR);
            if (ret < 0) {
                ALOGE("Write failed");
                return false;
            }
        }
#else // ON_CHIP
        int avg_count, sensor_val;
        int sum = 0;

        // Read three values of the proximity ADC and sum them
        for (avg_count = 0; avg_count < AVG_COUNT; avg_count++) {
            ret = wait_for_interrupt(ONE_SEC);
            if (ret > 0) {
                ret = APDS_ReadProxRegister(buf);
                if (ret > 0) {
                    sum += ((buf[1] << 8) + buf[0]);
                }
                // Clear interrupt bit
                ret = clear_interrupt(APDS_PS_ALS_CLR);
                if (ret < 0) {
                    ALOGE("Write failed!");
                    return false;
                }
            }
        }
        // Average the three values
        sensor_val = sum / avg_count;
        ALOGV("mAhead: %d, mDisplay: %d, sensor_val: %d", mAhead, mDisplay, sensor_val);

        if (sensor_val >= mAheadLThreshold && sensor_val <= mAheadUThreshold && mDisplayState == true) {
            mDisplayState = false;
#if CONTROL_BL
            disable_bl();
#endif
            event = GLANCE_EVENT_GLANCE_AHEAD;
        } else if (sensor_val >= mDisplayLThreshold && sensor_val <= mDisplayUThreshold && mDisplayState == false) {
            mDisplayState = true;
#if CONTROL_BL
            enable_bl();
#endif
            event = GLANCE_EVENT_GLANCE_DISPLAY;
        } else if (sensor_val < removal_threshold) {
#if CONTROL_BL
            disable_bl();
#endif
        }

        // Report event to upper layer
        if (mEventCallback && event != GLANCE_EVENT_UNKNOWN) {
            mEventCallback(event);
        }
#endif
#else // THRESHOLD_ALGO
        ret = wait_for_interrupt(ONE_SEC);
        if (ret > 0) {
            ret = APDS_ReadProxRegister(buf);
            if (ret > 0) {
                // Pull the oldest value out of the large array, store for cheap sum calculation
                old_val = poll_array[i];
                // Move the oldest value from small array into newest slot of the large array
                poll_array[i] = newest_array[j];
                // Put the latest value into the newest slot of the small array
                newest_array[j] = ((buf[1] << 8) + buf[0]);
                // Cheap calculation of the sum of the small array
                newest_array_sum = newest_array_sum - poll_array[i] + newest_array[j];
                // Average of the small array
                newest_array_avg = newest_array_sum / NEWEST_ARRAY_SIZE;

                ALOGV("i:%d, j:%d, sensor_val: %d, poll_array_avg:%d, newest_array_avg:%d\n", i, j, newest_array[j], poll_array_avg, newest_array_avg);

                event = GLANCE_EVENT_UNKNOWN;

                // Check to see if a significant event has occurred and deal with it
                if (newest_array_avg < removal_threshold) {
                    ALOGV("SIG EVENT");
                }
                // Look for changes of +/- DIFF_THRESHOLD from the larger average array for detection.
                else if (newest_array_avg > (poll_array_avg + AHEAD_THRESHOLD) && mDisplayState == true) {
                    mDisplayState = false;
                    ALOGV("AHEAD GLANCE!");
#if CONTROL_BL
                    disable_bl();
#endif
                    event = GLANCE_EVENT_GLANCE_AHEAD;
                } else if (newest_array_avg < (poll_array_avg - DISPLAY_THRESHOLD) && mDisplayState == false) {
                    mDisplayState = true;
                    ALOGV("DISPLAY GLANCE!");
#if CONTROL_BL
                    enable_bl();
#endif
                    event = GLANCE_EVENT_GLANCE_DISPLAY;
                }

                // Update sum and average of large array
                poll_array_sum = poll_array_sum - old_val + poll_array[i];
                poll_array_avg = poll_array_sum / POLL_ARRAY_SIZE;

                if (++i == POLL_ARRAY_SIZE) i = 0;
                if (++j == NEWEST_ARRAY_SIZE) j = 0;

                // Report event to upper layer
                if (mEventCallback && event != GLANCE_EVENT_UNKNOWN) {
                    mEventCallback(event);
                }

            }

            // Clear interrupt bit
            ret = clear_interrupt(APDS_PS_ALS_CLR);
            if (ret < 0) {
                ALOGE("Write failed");
                return false;
            }
        }
#endif // THRESHOLD_ALGO
    }

    return false;
}

