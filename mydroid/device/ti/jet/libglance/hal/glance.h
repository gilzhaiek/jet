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
#ifndef GLANCEHAL_H
#define GLANCEHAL_H

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <poll.h>
#include <errno.h>

#include <hardware/hardware.h>

#define GLANCE_HARDWARE_MODULE_ID "glance"

// Defines for kernel access
#define APDS_IOCTL_BASE 'a'
#define APDS_READ_BYTE _IOWR(APDS_IOCTL_BASE, 0, int)
#define APDS_READ_2BYTES _IOWR(APDS_IOCTL_BASE, 1, int)
#define APDS_CLEAR_IT  _IOW(APDS_IOCTL_BASE, 0, int)

// CMD bit of command register
#define COMMAND_MODE 0x80
// Auto-increment protocol transaction
#define AUTO_INCREMENT 0x20

// Size of calibration arrays
#define CALIBRATION_ARRAY_SIZE 100

/* Persistence register value (8 bits). This controls the filtering interrupt capabilities of the device.
 *  PPERS: Bits 7:4
 *  APERS: Bits 3:0
 * Since we're only using proximity, we should modify PPERS.
 */
#define PERS_VAL 0x70

/* File path for storing calibration data. This is so calibrated values are persistent
 * across power cycles. The file consists of three values, delimited by space:
 *  <Ahead glance average> <Display glance average> <PPCOUNT>
 */
#define GLANCE_CALIBRATION_PATH "/data/system/glance.cal"

/* Glance detection algorithm.
 *  Set to 1 for Theshold Algorithm. Set to 0 for Two Array Comparison Algorithm.
 */
#define THRESHOLD_ALGO 1

#if THRESHOLD_ALGO
/* Threshold algorithm.
 *
 *  Take AVG_COUNT number of readings from the proximity sensor. The average of the readings
 *  are calculated. Decision of ahead glance or display glance is derived by whether the average
 *  falls within the ahead glance threshold window or the display glance threshold window.
 *
 *  The threshold windows are calculated based on the ahead + glance calibration steps. The
 *  calibration steps yield two values: the average sensor value for ahead glance (mAhead) and
 *  the average sensor value for display glance (mDisplay) for the user. The difference of the
 *  two values are obtained and divided into 10 equal parts (div). Then the threshold windows
 *  are calculated based on the following:
 *
 *  Ahead upper threshold = mAhead + (div * AHEADU_PCT/10)
 *  Ahead lower threshold = mAhead - (div * AHEADL_PCT/10)
 *  Display upper threshold = mDisplay + (div * DISPLAYU_PCT/10)
 *  Display lower threshold = mDisplay - (div * DISPLAYL_PCT/10)
 *
 *  The threshold windows must be in the range of [0, 1023].
 *
 *  -----------------------------------------------------------------------------------------
 *
 *  Threshold ON_CHIP algorithm.
 *
 *  The threshold algorithm can also be run on the chip. All averaging and comparisons are
 *  done on the chip. An interrupt is only triggered if the sensor reading exceeds either
 *  the upper or lower threshold. That is, when trying to detect for a ahead glance, the
 *  chip thresholds are set to be 0 and "Ahead lower threshold". When trying to detect for a
 *  display glance, the chip thresholds are set to be "Display upper threshold" and 1023.
 */
#define ON_CHIP         1       // Enable ON_CHIP algorithm

// Threshold window percentage.
#define AHEADU_PCT      20
#define AHEADL_PCT      50
#define DISPLAYU_PCT    40
#define DISPLAYL_PCT    20

// Number of average readings to take during glance detection
#define AVG_COUNT 5

#else
/* Two array comparison algorithm.
 *
 *  Compare two arrays of different size (poll_array and newest_array) with newest_array being
 *  smaller than poll_array. Both arrays are FIFO with newest_array feeding poll_array it's
 *  oldest value when a new value is given to newest_array. At each new value from the sensor
 *  the average of each array is calculated and the two compared. If difference between the two
 *  is greater than a threshold value then there is a state change of the display.
 */

// Larger array of old values
#define POLL_ARRAY_SIZE     100

// Small array of most recent values
#define NEWEST_ARRAY_SIZE   10

// Difference threshold for glance detection. Increase value to reduce false positives.
#define DISPLAY_THRESHOLD   85
#define AHEAD_THRESHOLD     60
#endif

#define u8 unsigned char
#define u16 unsigned int
#define ONE_SEC 1000

typedef void (*glance_event_callback)(int event);

typedef struct glance_device_ops {
    /** Set the callback to GlanceService */
    void (*set_event_callback)(glance_event_callback event_cb);

    /** Start ahead calibration */
    int (*ahead_calibration)();

    /** Start display calibration */
    int (*display_calibration)();

    /** Start glance detection */
    int (*start_glance_detection)();

    /** Stop glance detection */
    int (*stop_glance_detection)();

} glance_device_ops_t;

typedef struct glance_device {
    hw_device_t common;
    glance_device_ops_t *ops;
} glance_device_t;

typedef enum GLANCE_EVENT {
    GLANCE_EVENT_UNKNOWN = -1,
    GLANCE_EVENT_AHEAD_CALIBRATED,    // Ahead calibration complete
    GLANCE_EVENT_DISPLAY_CALIBRATED,  // Display calibration complete
    GLANCE_EVENT_GLANCE_AHEAD,          // Ahead glance detected
    GLANCE_EVENT_GLANCE_DISPLAY,        // Display glance detected
    GLANCE_EVENT_GLANCE_STOPPED,        // Glance detection stopped
    GLANCE_EVENT_REMOVED              // Removal detected
} GLANCE_EVENT;

typedef enum GLANCE_STATE {
    GLANCE_STATE_INIT,                // Initial state. Requires calibration values
    GLANCE_STATE_CALIBRATED,          // Calibration values obtained
    GLANCE_STATE_DETECTING,           // Glance detection underway
} GLANCE_STATE;

int write_bytes(u8 *val, int size);
int write_register(u8 reg, u8 val);
int read_2bytes(u8 reg, u8 *val);
int read_register(u8 reg, u8 *val);
int clear_interrupt(u8 value);
int wait_for_interrupt(int timeout);

// Compile flag to enable/disable turning on/off the display backlight during glance detection
#define CONTROL_BL      0

#if CONTROL_BL
#define KOPIN           0           // Control Backlight by suspending/resuming Kopin chip

#if KOPIN
#define KOPIN_CTRL      "/proc/kopinctrl"
#else
#define PANEL_BACKLIGHT "/sys/devices/platform/display_led/leds/lcd-backlight/brightness"
#endif
int enable_bl();
int disable_bl();
int set_bl(int bl_enable);
#endif

#define APDS_ReadProxRegister(x) read_2bytes(APDS_PDATAL_REG, x)

#endif
