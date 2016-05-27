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
#define LOG_TAG "glance_HAL"
#include <utils/Log.h>
#include <stdio.h>

#include "glance.h"
#include "apds.h"
#include "GlanceHalThread.h"

#include <hardware/hardware.h>

#define APDS9900_FILE "/dev/apds9900"
#define MIN(x,y) ((x)<(y)? (x):(y))
#define ABS(x) ((x)<0? (x*-1):(x))
#define ERROR -1

// Static global variables
static glance_event_callback event_callback = NULL;
static struct pollfd Pfd;
static GlanceHalThread *glance_thread = NULL;
static int ahead_avg[CALIBRATION_ARRAY_SIZE];
static int display_avg[CALIBRATION_ARRAY_SIZE];
static int ahead_avg_val = 0;
static int display_avg_val = 0;
static int ahead_cur_size = 0;
static int display_cur_size = 0;
static int ppcount_val = 0;
#if (CONTROL_BL && !(KOPIN))
static int bl_resume_val = 0;
#endif
static GLANCE_STATE state = GLANCE_STATE_INIT;

int write_bytes(u8 *val, int size) {
    int ret;
    ret = write(Pfd.fd, val, size);
    if (ret == size)
        return 0;
    else {
        ALOGE("Write failed");
        return -EIO;
    }
}

int write_register(u8 reg, u8 val) {
    int ret;
    u8 buf[2];
    buf[0] = reg;
    buf[1] = val;
    return write_bytes(buf, 2);
}

int read_2bytes(u8 reg, u8 *val) {
    int ret;
    *val = reg;
    ret = ioctl(Pfd.fd, APDS_READ_2BYTES, val);
    if (ret) {
        ALOGE("Read failed");
        return -EIO;
    }
    return 1;
}

int read_register(u8 reg, u8 *val) {
    int ret;
    u8 buf = reg;
    ret = ioctl(Pfd.fd, APDS_READ_BYTE, &buf);
    if (ret){
        ALOGE("Read failed");
        return -EIO;
    }
    *val = buf;
    return 1;
}

int clear_interrupt(u8 value) {
    int rc = ioctl(Pfd.fd, APDS_CLEAR_IT, &value);
    if (rc < 0) {
        ALOGE("Read fail");
        return -EIO;
    }
    return 0;
}

int wait_for_interrupt(int timeout) {
    int ret = poll(&Pfd, 1, timeout);
    if (ret > 0) {
        ret = (Pfd.revents == POLLIN) ? 1 : 0;
    }
    return ret;
}

#if CONTROL_BL
#if !(KOPIN)
static int get_bl() {
    int fd, bl_val;
    char value[8];

    fd = open(PANEL_BACKLIGHT, O_RDWR);
    if (fd < 0) {
        ALOGE("Failed to open panel backlight");
        return errno;
    }

    read(fd, value, 8);
    bl_val = atoi(value);

    close(fd);

    return bl_val;
}
#endif

int set_bl(int bl_enable) {
    int nwr, fd, ret;
    char value[16];

#if KOPIN
    fd = open(KOPIN_CTRL, O_RDWR);
#else
    fd = open(PANEL_BACKLIGHT, O_RDWR);
#endif
    if (fd < 0) {
        ALOGE("Failed to open panel backlight");
        return errno;
    }

#if KOPIN
    if (bl_enable) {
        nwr = sprintf(value, "%s\n", "KOPIN_RESUME");
    } else {
        nwr = sprintf(value, "%s\n", "KOPIN_SUSPEND");
    }
#else
    nwr = sprintf(value, "%d\n", bl_enable);
#endif
    ret = write(fd, value, nwr);

    close(fd);

    return (ret == nwr) ? 0 : -1;
}

int enable_bl() {
#if !(KOPIN)
    return set_bl(bl_resume_val);
#else
    return set_bl(1);
#endif
}

int disable_bl() {
#if !(KOPIN)
    bl_resume_val = get_bl();
#endif
    return set_bl(0);
}

#endif

static int APDS9900_base_config() {
    u8 buf[9];
    int ret;

    buf[0] = (COMMAND_MODE|AUTO_INCREMENT|APDS_ENABLE_REG);

    /********************************************************************************/
    /* APDS ENABLE REGISTER R0                                                      */
    /* [7-6] = 00b      Reserved, must be 00                                        */
    /* [5-0] = 000000b  Clear all - DISABLE                                         */
    /********************************************************************************/
    buf[1] = 0x00; //Disable and power down

    /********************************************************************************/
    /* APDS ATIME REGISTER R1                                                       */
    /* [7:0] = 0xFF     ALS Integration Time    2.72ms  (minimum)                   */
    /********************************************************************************/
    buf[2] = APDS_ATIME_MIN;

    /********************************************************************************/
    /* APDS PTIME REGISTER R2                                                       */
    /* [7:0] = 0xFF     Proximity Integration Time  2.72ms  (minimum)               */
    /********************************************************************************/
    buf[3] = APDS_PTIME;

    /********************************************************************************/
    /* APDS WTIME REGISTER R3                                                       */
    /* [7:0] = 0xFF     Wait Time   2.72ms  (minimum)                               */
    /********************************************************************************/
    buf[4] = APDS_WTIME_25MS;

    ret = write_bytes(buf,5);
    if (ret < 0) {
        ALOGE("Write failed");
        return -EIO;
    }

    buf[0] = (COMMAND_MODE|AUTO_INCREMENT|APDS_PILTL_REG);

    // Interupt Config Initialization

    /********************************************************************************/
    /* APDS ENABLE Proximity Registers                                              */
    /*  APDS_PILTL_REG                  0x08                                        */
    /*  APDS_PILTH_REG                  0x09                                        */
    /*  APDS_PIHTL_REG                  0x0A                                        */
    /*  APDS_PIHTH_REG                  0x0B                                        */
    /*  APDS_PERS_REG                   0x0C                                        */
    /********************************************************************************/
    buf[1] = APDS_PILTL_INIT;
    buf[2] = APDS_PILTH_INIT;
    buf[3] = APDS_PIHTL_INIT;
    buf[4] = APDS_PIHTH_INIT;
    buf[5] = APDS_PERS_INIT;

    /********************************************************************************/
    /* APDS CONFIG REGISTER RD                                                      */
    /* [1] = 0  Wait Config WLONG = 0                                               */
    /* [1] = 1  Wait Config WLONG = 1 12x Wait cycle                                */
    /********************************************************************************/
    buf[6] = APDS_WSHORT;

    /********************************************************************************/
    /* APDS PPCOUNT REGISTER RE                                                     */
    /* [7:0] = 0x01     Proximity Pulse count       (minimum)                       */
    /********************************************************************************/
    buf[7] = APDS_PPCOUNT_MIN;

    /********************************************************************************/
    /* APDS CONTROL REGISTER RF                                                     */
    /* [7:6] = 0x01     LED drive           50 mA                                   */
    /* [5:4] = 0x20     Proximity Diode Select      RESERVED                        */
    /* [3:2] = 0x0      Proximity Gain Control      RESERVED                        */
    /* [1:0] = 0x0      ALS Gain Control        RESERVED                            */
    /********************************************************************************/
    buf[8] = (APDS_PDRIVE_50 | APDS_PDIODE | APDS_PGAIN | APDS_AGAIN_1X);

    ret = write_bytes(buf,9);
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

    /********************************************************************************/
    /* APDS ENABLE REGISTER R0                                                      */
    /* [7-6] = 00b      Reserved, must be 00                                        */
    /* [5-0] = 101111b  Enable all but ALS interupts                                */
    /********************************************************************************/
    ret = write_register(COMMAND_MODE | APDS_ENABLE_REG, (APDS_PIEN_OFF | APDS_AIEN_OFF | APDS_WEN_ON | APDS_PEN_ON | APDS_AEN_OFF | APDS_PON));
    if (ret < 0) {
        ALOGE("Write failed");
        return -EIO;
    }

    return ret;
}

static int initialize_apds() {
    int ret = 0;
    Pfd.fd = open(APDS9900_FILE, O_RDWR);
    if (Pfd.fd < 0) {
        ALOGE("Failed to open %s", APDS9900_FILE);
        ret = Pfd.fd;
        return ret;
    }
    Pfd.events = POLLIN;
    Pfd.revents = 0;

    // Clear interrupt bit in case it is still set previously
    ret = clear_interrupt(APDS_PS_ALS_CLR);
    if (ret < 0) {
        ALOGE("Failed to clear interrupt");
        close(Pfd.fd);
        return ret;
    }

    // Initialize APDS chip with base configuration
    ret = APDS9900_base_config();
    if (ret < 0) {
        ALOGE("Failed to initialize APDS chip");
        close(Pfd.fd);
        return ret;
    }

    ALOGV("Successfully initialized APDS");

    return ret;
}

static int glance_device_close(hw_device_t *device) {
    ALOGV("glance_device_close");
    int ret = 0;
    glance_device_t *dev = NULL;

    if (device == NULL) {
        ret = -EINVAL;
        return ret;
    }

    dev = (glance_device_t *)device;
    if (dev->ops) {
        free(dev->ops);
    }
    free(dev);

    return ret;
}

static void glance_set_callback(glance_event_callback event_cb) {
    if (!event_callback) {
        event_callback = event_cb;
    }
}

static void write_calibration() {
    FILE *fp;
    char buf[12];
    fp = fopen(GLANCE_CALIBRATION_PATH, "w");
    if (fp != NULL) {
        sprintf(buf, "%d %d %d", ahead_avg_val, display_avg_val, ppcount_val);
        fputs(buf, fp);
        fclose(fp);
    }
}

static void read_calibration() {
    FILE *fp;
    char buf[12];
    size_t len;
    fp = fopen(GLANCE_CALIBRATION_PATH, "r");
    if (fp != NULL) {
        len = fread(buf, 1, sizeof(buf), fp);
        fclose(fp);
    }
    ALOGV("Read calibration: %s", buf);

    if (strlen(buf) > 0) {
        // Parse the char buffer
        char *tok = strtok(buf, " ");
        ahead_avg_val = (tok == NULL) ? 0 : atoi(tok);
        tok = strtok(NULL, " ");
        display_avg_val = (tok == NULL) ? 0 : atoi(tok);
        tok = strtok(NULL, " ");
        ppcount_val = (tok == NULL) ? 0 : atoi(tok);
        ALOGV("Parsed: %d %d %d", ahead_avg_val, display_avg_val, ppcount_val);
    }
}

static int glance_start_glance_detection();
static int glance_stop_glance_detection();

/*
 * Glance Calibration Algorithm
 * --------------------------
 *
 * The Glance calibration algorithm is composed of two steps:
 * 1) Ahead calibration (The user is looking ahead, that is, not at the display)
 *      a) PPCOUNT = 1
 *      b) Do some readings, take the average of the readings, and store it in an array (ahead_avg[PPCOUNT-1])
 *      c) PPCOUNT++
 *      d) Repeat b) and c) until average of the readings == 1023
 *
 * 2) Display calibration (The user is looking at the display)
 *      a) PPCOUNT = 1
 *      b) Do some readings, take the average of the readings, and store it in an array (display_avg[PPCOUNT-1])
 *      c) PPCOUNT++
 *      d) Repeat b) and c) until average of the readings == 1023
 *
 * 3) Find the max difference
 *      diff1 = | ahead_avg[0] - display_avg[0] |
 *      diff2 = | ahead_avg[1] - display_avg[1] |
 *      ...
 *      diffX = max(diff1, diff2, ...)
 *
 * 4) Set PPCOUNT to X
 *
 */
static int glance_calibration(bool ahead) {
    ALOGV("glance_calibration");
    int ppcount = 1;
    int cur_size = 0;
    int ret, avg_count;
    int *avg = (ahead) ? ahead_avg : display_avg;
    u16 sum, average;
    u8 buf[2];
    GLANCE_STATE prev_state = state;

    if (state == GLANCE_STATE_DETECTING) {
        /* If it is currently in the GLANCE_STATE_DETECTING state, then
        ** we need to stop the glance detection and restart it after calibration
        ** is done.
        */
        if (glance_stop_glance_detection() == ERROR) {
            ALOGE("Cannot perform calibration!");
            return ERROR;
        }

        // Wait one sec to let the chip finish up
        usleep(1000000);
    }

    // Configure filtering interrupt capabilities so every cycle generates an interrupt.
    write_register(COMMAND_MODE | APDS_PERS_REG, 0x00);

    cur_size = 0;

    // Start with PPCOUNT of 1
    ret = write_register(COMMAND_MODE | APDS_PPCOUNT_REG, ppcount);
    if (ret < 0) {
        ALOGE("Write failed!");
        return ret;
    }

    while (true) {
        sum = 0;

        // Power on the APDS with Proximity and Proximity interrupt enabled
        ret = write_register(COMMAND_MODE | APDS_ENABLE_REG, (APDS_PIEN_ON | APDS_AIEN_OFF | APDS_WEN_ON | APDS_PEN_ON | APDS_AEN_OFF | APDS_PON));
        if (ret < 0) {
            ALOGE("Write failed!");
            return ret;
        }

        // Read three values of the proximity ADC and sum them
        for (avg_count = 0; avg_count <= 2; avg_count++) {
            ret=wait_for_interrupt(ONE_SEC);
            if (ret > 0) {
                ret=APDS_ReadProxRegister(buf);
                if (ret > 0) {
                    sum += ((buf[1]<<8) + buf[0]);
                    ALOGV("sum: %d", sum);
                }
                // Clear interrupt bit
                ret = clear_interrupt(APDS_PS_ALS_CLR);
                if (ret < 0) {
                    ALOGE("Write failed!");
                    return ret;
                }
            }
        }

        // Turn off the proximity interupt for calculation phase
        ret = write_register(COMMAND_MODE | APDS_ENABLE_REG, (APDS_PIEN_OFF | APDS_AIEN_OFF | APDS_WEN_ON | APDS_PEN_ON | APDS_AEN_OFF | APDS_PON));
        if (ret < 0) {
            ALOGE("Write failed!");
            return ret;
        }

        // Average the three values
        average = sum / avg_count;
        avg[cur_size++] = average;
        ALOGV("PPCOUNT: %d [%.u]", ppcount, average);

        ppcount++;

        /************************************************************/
        /* APDS PPCOUNT REGISTER RE                                 */
        /* [7:0] = 0x01     Proximity Pulse count       (minimum)   */
        /************************************************************/
        ret = write_register(COMMAND_MODE | APDS_PPCOUNT_REG, ppcount);
        if (ret < 0) {
            ALOGE("Write failed!");
            return ret;
        }

        if (average >= 1023 || ppcount > 0xFF)
            break;

        if (cur_size >= CALIBRATION_ARRAY_SIZE)
            break;
    }

    ppcount = 1;
    if (ahead) {
        ahead_cur_size = cur_size;
    } else {
        display_cur_size = cur_size;
    }

    // If both arrays are not empty, then find the best PPCOUNT
    if (display_cur_size != 0 && ahead_cur_size != 0) {
        int size = MIN(ahead_cur_size, display_cur_size);
        ALOGV("ahead_cur_size: %d, display_cur_size: %d SIZE: %d", ahead_cur_size, display_cur_size, size);
        u16 max_diff = 0;
        for (int i = 0; i < size; i++) {
            u16 diff = ABS((ahead_avg[i] - display_avg[i]));
            if (diff > max_diff) {
                max_diff = diff;
                ahead_avg_val = ahead_avg[i];
                display_avg_val = display_avg[i];
                ppcount = i+1;
            }
        }
        ppcount_val = ppcount;
        ALOGV("MAXDIFF: %.u at PPCOUNT: %d, AHEAD_VAL:%d DISPLAY_VAL:%d\n", max_diff, ppcount, ahead_avg_val, display_avg_val);
        write_calibration();
        state = GLANCE_STATE_CALIBRATED;
    } else {
        // Reset the PPCOUNT to 1
        ppcount = 1;
    }

    /************************************************************/
    /* APDS PPCOUNT REGISTER RE                                 */
    /* [7:0] = 0x01     Proximity Pulse count       (minimum)   */
    /************************************************************/
    ret = write_register(COMMAND_MODE | APDS_PPCOUNT_REG, ppcount);
    if (ret < 0) {
        ALOGE("Write failed!");
        return ret;
    }

    // Report to upper layer that calibration is complete
    if (event_callback) {
        event_callback((ahead) ? GLANCE_EVENT_AHEAD_CALIBRATED : GLANCE_EVENT_DISPLAY_CALIBRATED);
    }

    // Check to see if our previous state is glance detecting
    if (prev_state == GLANCE_STATE_DETECTING) {
        // Restart glance detection
        ret = glance_start_glance_detection();
    }

    return ret;
}

static int glance_ahead_calibration() {
    return glance_calibration(true);
}

static int glance_display_calibration() {
    return glance_calibration(false);
}

static int glance_start_glance_detection() {
    ALOGV("glance_start_glance_detection - state:%d", state);
    int ret = ERROR;

    // If we are already glance detecting, just return a success
    if (state == GLANCE_STATE_DETECTING) {
        return 0;
    }

    // Check to see if there is persistent calibration data
    if (state == GLANCE_STATE_INIT && (ahead_avg_val == 0 || display_avg_val == 0 || ppcount_val == 0)) {
        ALOGV("Reading calibration file...");
        read_calibration();
        // Validate the read values. Ensure ahead and display values are within 0 < x <= 1023 and ahead > display. Also ensure ppcount > 0.
        if (ahead_avg_val > 0 && ahead_avg_val <= 1023 &&
            display_avg_val > 0 && display_avg_val <= 1023 &&
            ppcount_val > 0 && ahead_avg_val > display_avg_val) {
            ALOGV("Valid persistent calibration data: %d %d %d", ahead_avg_val, display_avg_val, ppcount_val);

            // Set the PPCOUNT to read value
            ret = write_register(COMMAND_MODE | APDS_PPCOUNT_REG, ppcount_val);
            if (ret < 0) {
                ALOGE("Write failed!");
                return -EIO;
            }

            state = GLANCE_STATE_CALIBRATED;
        }
    }

    // Make sure we're in the correct state. We cannot perform glance detection if it's not calibrated!
    if (state == GLANCE_STATE_CALIBRATED) {
        if (glance_thread != NULL) {
            ALOGE("glance_thread not null!");
            return ERROR;
        }

#if CONTROL_BL
        bl_resume_val = get_bl();
#endif
        // Start a thread to do glance detection
        glance_thread = new GlanceHalThread(event_callback, ahead_avg_val, display_avg_val);
        glance_thread->run();
        state = GLANCE_STATE_DETECTING;
        ret = 0;
    }
    ALOGV("glance_start_glance_detection X");
    return ret;
}

static int glance_stop_glance_detection() {
    ALOGV("glance_stop_glance_detection - state:%d", state);
    int ret = ERROR;

    // Make sure glance detection has started
    if (state == GLANCE_STATE_DETECTING && glance_thread) {
        glance_thread->requestExit();
        glance_thread = NULL;

        // Assume we're still calibrated
        state = GLANCE_STATE_CALIBRATED;
        ret = 0;
    } else {
        event_callback(GLANCE_EVENT_GLANCE_STOPPED);
    }
    ALOGV("glance_stop_glance_detection X");
    return ret;
}

static int glance_device_open(const hw_module_t *module, const char *id, hw_device_t **device) {
    ALOGV("glance_device_open: %s", id);
    int ret = 0;
    glance_device_t *glance_device = NULL;
    glance_device_ops_t *glance_ops = NULL;

    glance_device = (glance_device_t *)malloc(sizeof(*glance_device));
    if (!glance_device) {
        ALOGE("glance_device allocation fail");
        ret = -ENOMEM;
        goto failure;
    }

    glance_ops = (glance_device_ops_t *)malloc(sizeof(*glance_ops));
    if (!glance_ops) {
        ALOGE("glance_device_ops allocation fail");
        ret = -ENOMEM;
        goto failure;
    }

    if (initialize_apds() < 0) {
        ALOGE("Failed to initialize glance HAL");
        ret = -EIO;
        goto failure;
    }

    memset(glance_device, 0, sizeof(*glance_device));
    memset(glance_ops, 0, sizeof(*glance_ops));

    glance_device->common.tag = HARDWARE_DEVICE_TAG;
    glance_device->common.version = 0;
    glance_device->common.module = (hw_module_t *)(module);
    glance_device->common.close = glance_device_close;
    glance_device->ops = glance_ops;

    glance_ops->set_event_callback = glance_set_callback;
    glance_ops->ahead_calibration = glance_ahead_calibration;
    glance_ops->display_calibration = glance_display_calibration;
    glance_ops->start_glance_detection = glance_start_glance_detection;
    glance_ops->stop_glance_detection = glance_stop_glance_detection;

    *device = &glance_device->common;

    return ret;

failure:
    if (glance_device) {
        free(glance_device);
        glance_device = NULL;
    }

    if (glance_ops) {
        free(glance_ops);
        glance_ops = NULL;
    }

    *device = NULL;
    return ret;
}

static struct hw_module_methods_t glance_module_methods = {
    open : glance_device_open
};

struct hw_module_t HAL_MODULE_INFO_SYM = {
    tag : HARDWARE_MODULE_TAG,
    version_major : 1,
    version_minor : 0,
    id : GLANCE_HARDWARE_MODULE_ID,
    name : "Glance HAL",
    author : "Recon GlanceHAL Module",
    methods : &glance_module_methods,
    dso : 0,
    reserved : {0},
};

