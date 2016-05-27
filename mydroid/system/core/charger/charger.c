/*
 * Copyright (C) 2011 The Android Open Source Project
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

//#define DEBUG_UEVENTS
#define CHARGER_KLOG_LEVEL 6

#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <linux/input.h>
#include <linux/netlink.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <string.h>
#include <sys/poll.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/un.h>
#include <time.h>
#include <unistd.h>
#include <linux/i2c.h>


#include <cutils/android_reboot.h>
#include <cutils/klog.h>
#include <cutils/list.h>
#include <cutils/misc.h>
#include <cutils/uevent.h>
#include <cutils/properties.h>

#include "minui/minui.h"
//Set 1 below to check if need to enable MTP service fo PC connection (Which is so called "Partial Android")
#define MTP_ENABLE 0

#ifndef max
#define max(a,b) ((a) > (b) ? (a) : (b))
#endif

#ifndef min
#define min(a,b) ((a) < (b) ? (a) : (b))
#endif

#define ARRAY_SIZE(x)           (sizeof(x)/sizeof(x[0]))

#define MSEC_PER_SEC            (1000LL)
#define NSEC_PER_MSEC           (1000000LL)

#define BATTERY_UNKNOWN_TIME    (2 * MSEC_PER_SEC)
#define POWER_ON_KEY_TIME       (1 * MSEC_PER_SEC)
#define UNPLUGGED_SHUTDOWN_TIME (2 * MSEC_PER_SEC)
#define BATT_FULL_SHUTDOWN_TIME (20 * MSEC_PER_SEC)
#define BATT_NONCHARGE_SHUTDOWN_TIME (20 * MSEC_PER_SEC)

#define BATTERY_FULL_THRESH     95
#define BATTERY_LOW_THRESH      5

#define BAT_USB_EVENT_NAME      "twl6030_usb"
#define POWER_SUPPLY_TYPE       "POWER_SUPPLY_ONLINE"
#define POWER_SUPPLY_TYPE_PATH  "/sys/class/power_supply/twl6030_usb/online"
#define BAT_STATUS_PATH         "/sys/class/power_supply/twl6030_battery/status"

enum power_supply_type {
    POWER_SUPPLY_TYPE_BATTERY = 0,
    POWER_SUPPLY_TYPE_UPS,
    POWER_SUPPLY_TYPE_MAINS,
    POWER_SUPPLY_TYPE_USB,          /* Standard Downstream Port */
    POWER_SUPPLY_TYPE_USB_DCP,      /* Dedicated Charging Port */
    POWER_SUPPLY_TYPE_USB_CDP,      /* Charging Downstream Port */
    POWER_SUPPLY_TYPE_USB_ACA,      /* Accessory Charger Adapters */
    POWER_SUPPLY_TYPE_USB_UNKNOWN,
};

enum {
	POWER_SUPPLY_STATUS_UNKNOWN = 0,
	POWER_SUPPLY_STATUS_CHARGING,
	POWER_SUPPLY_STATUS_DISCHARGING,
	POWER_SUPPLY_STATUS_NOT_CHARGING,
	POWER_SUPPLY_STATUS_FULL,
};

#define KOPIN_CTRL      "/proc/kopinctrl"

/* The following constant is the IOCTL number defined in /linux/i2c-dev.h*/
#define I2C_SLAVE               0x0703
#define I2C_SLAVE_FORCE         0x0706
#define I2C_TENBIT              0x0704

/* This is the structure as used in the I2C_RDWR ioctl call */
struct i2c_rdwr_ioctl_data {
        struct i2c_msg __user *msgs;    /* pointers to i2c_msgs */
        __u32 nmsgs;                    /* number of i2c_msgs */
};

#define LED_I2C_ADDRESS         0x49
#define BITS_7                  0
#define PMC_PWM2ON              0xBD
#define PMC_PWM2OFF             0xBE
#define PMC_TOGGLE3             0x92

#define PWM2ON_PEROID           0x60
#define PWM2OFF_PEROID          0x7F
#define PWM2EN_CLOCK_INPUT      (1 << 5)
#define PWM2EN_SIGNAL           (1 << 4)
#define PWM2DISABLE_SIGNAL      (1 << 3)

#define PMIC_LED_PWM_CTRL2	0xF5
#define PMIC_LED_PWM_ON		0x01
#define PMIC_LED_PWM_OFF	0x02
#define PMIC_LED_PWM_AUTO	0x03

enum led_states {
    LED_STATE_NOP = -1,
    LED_STATE_OFF = 0,
    LED_STATE_ON = 1,
    LED_STATE_BLINK = 2,
};

#define LED_BLINK_CNT           3
#define LED_BLINK_DELAY_MS      250

#define LAST_KMSG_PATH          "/proc/last_kmsg"
#define LAST_KMSG_MAX_SZ        (32 * 1024)

#define LOGE(x...) do { KLOG_ERROR("charger", x); } while (0)
#define LOGI(x...) do { KLOG_INFO("charger", x); } while (0)
#define LOGV(x...) do { KLOG_DEBUG("charger", x); } while (0)

struct key_state {
    bool pending;
    bool down;
    int64_t timestamp;
};

struct power_supply {
    struct listnode list;
    char name[256];
    char type[32];
    bool online;
    bool valid;
    char cap_path[PATH_MAX];
};

struct charger {
    int64_t next_key_check;
    int64_t next_pwr_check;
    int64_t next_full_check;
    int64_t next_blink;

    struct key_state keys[KEY_MAX + 1];
    int uevent_fd;

    struct listnode supplies;
    int num_supplies;
    int num_supplies_online;

    struct power_supply *battery;
    int cur_led_state;
    int cur_blink_cnt;
    int cur_charger;
#if MTP_ENABLE
    bool mtp_only;
#endif
    int charge_status;
};

struct uevent {
    const char *action;
    const char *path;
    const char *subsystem;
    const char *ps_name;
    const char *ps_type;
    const char *ps_online;
    const char *ps_status;
};

static struct charger charger_state = {
    .cur_led_state = LED_STATE_NOP,
    .cur_charger = POWER_SUPPLY_TYPE_BATTERY,
#if MTP_ENABLE
    .mtp_only = true,
#endif
    .next_key_check = -1,
    .next_pwr_check = -1,
    .next_full_check = -1,
    .next_blink = -1,
    .charge_status = POWER_SUPPLY_STATUS_CHARGING,
};

static int char_width;
static int char_height;
//If the system is shut down but still connecting charger, we need the system on but without boot to Android MTP
static bool shutdown_charge_enable;
#define SHUTDOWN_REASON_PROPERTY "persist.sys.shutdown.reason"
#define MANUFACTURE_PROPERTY "ro.manufacture"
#define CHARGE_SHUTDOWN_REASON "ChargeShutdown"

static bool is_shutdown_charge(void)
{
    char value[PROPERTY_VALUE_MAX];

    property_get(SHUTDOWN_REASON_PROPERTY, value, "");

    if(!strcmp(value, CHARGE_SHUTDOWN_REASON)) {
        //The real shutdown reason can return back to shutdown now 
        property_set(SHUTDOWN_REASON_PROPERTY, "Shutdown");
        return true;
    }
    return false;
}

/* current time in milliseconds */
static int64_t curr_time_ms(void)
{
    struct timespec tm;
    clock_gettime(CLOCK_MONOTONIC, &tm);
    return tm.tv_sec * MSEC_PER_SEC + (tm.tv_nsec / NSEC_PER_MSEC);
}

static void clear_screen(void)
{
    gr_color(0, 0, 0, 255);
    gr_fill(0, 0, gr_fb_width(), gr_fb_height());
};

#ifdef DEBUG_DUMP_KMSG
#define MAX_KLOG_WRITE_BUF_SZ 256

static void dump_last_kmsg(void)
{
    char *buf;
    char *ptr;
    unsigned sz = 0;
    int len;

    LOGI("\n");
    LOGI("*************** LAST KMSG ***************\n");
    LOGI("\n");
    buf = load_file(LAST_KMSG_PATH, &sz);
    if (!buf || !sz) {
        LOGI("last_kmsg not found. Cold reset?\n");
        goto out;
    }

    len = min(sz, LAST_KMSG_MAX_SZ);
    ptr = buf + (sz - len);

    while (len > 0) {
        int cnt = min(len, MAX_KLOG_WRITE_BUF_SZ);
        char yoink;
        char *nl;

        nl = memrchr(ptr, '\n', cnt - 1);
        if (nl)
            cnt = nl - ptr + 1;

        yoink = ptr[cnt];
        ptr[cnt] = '\0';
        klog_write(6, "<6>%s", ptr);
        ptr[cnt] = yoink;

        len -= cnt;
        ptr += cnt;
    }

    free(buf);

out:
    LOGI("\n");
    LOGI("************* END LAST KMSG *************\n");
    LOGI("\n");
}
#endif

static int read_file(const char *path, char *buf, size_t sz)
{
    int fd;
    size_t cnt;

    fd = open(path, O_RDONLY, 0);
    if (fd < 0)
        goto err;

    cnt = read(fd, buf, sz - 1);
    if (cnt <= 0)
        goto err;
    buf[cnt] = '\0';
    if (buf[cnt - 1] == '\n') {
        cnt--;
        buf[cnt] = '\0';
    }

    close(fd);
    return cnt;

err:
    if (fd >= 0)
        close(fd);
    return -1;
}

static int read_file_int(const char *path, int *val)
{
    char buf[32];
    int ret;
    int tmp;
    char *end;

    ret = read_file(path, buf, sizeof(buf));
    if (ret < 0)
        return -1;

    tmp = strtol(buf, &end, 0);
    if (end == buf ||
        ((end < buf+sizeof(buf)) && (*end != '\n' && *end != '\0')))
        goto err;

    *val = tmp;
    return 0;

err:
    return -1;
}

static int write_str(char const *path, char *buffer, int bytes)
{
    int fd;
    int ret;

    fd = open(path, O_RDWR);
    if (fd >= 0) {
        ret= write(fd, buffer, bytes);
        close(fd);
        return ret == -1 ? -errno : 0;
    } else {
        LOGE("write_int failed to open %s\n", path);
        return -errno;
    }
}

static int set_i2c_register(int file,
                            unsigned char addr,
                            unsigned char reg,
                            unsigned char value) {

    unsigned char outbuf[2];
    struct i2c_rdwr_ioctl_data packets;
    struct i2c_msg messages[1];

    messages[0].addr  = addr;
    messages[0].flags = 0;
    messages[0].len   = sizeof(outbuf);
    messages[0].buf   = outbuf;

    /* The first byte indicates which register we'll write */
    outbuf[0] = reg;

    /* The second byte indicates the value to write.  Note that for many
       devices, we can write multiple, sequential registers at once by
       simply making outbuf bigger. */
    outbuf[1] = value;

    /* Transfer the i2c packets to the kernel and verify it worked */
    packets.msgs  = messages;
    packets.nmsgs = 1;
    if(ioctl(file, I2C_RDWR, &packets) < 0) {
        perror("Unable to send data");
        return -1;
    }

    return 0;
}

static int get_i2c_register(int file,
                            unsigned char addr,
                            unsigned char reg,
                            unsigned char *val) {
    unsigned char inbuf, outbuf;
    struct i2c_rdwr_ioctl_data packets;
    struct i2c_msg messages[2];

    /* In order to read a register, we first do a "dummy write" by writing
       0 bytes to the register we want to read from.  This is similar to
       the packet in set_i2c_register, except it's 1 byte rather than 2. */
    outbuf = reg;
    messages[0].addr  = addr;
    messages[0].flags = 0;
    messages[0].len   = sizeof(outbuf);
    messages[0].buf   = &outbuf;

    /* The data will get returned in this structure */
    messages[1].addr  = addr;
    messages[1].flags = I2C_M_RD;
    messages[1].len   = sizeof(inbuf);
    messages[1].buf   = &inbuf;

    /* Send the request to the kernel and get the result back */
    packets.msgs      = messages;
    packets.nmsgs     = 2;
    if(ioctl(file, I2C_RDWR, &packets) < 0) {
        perror("Unable to send data");
        return -1;
    }
    *val = inbuf;

    return 0;
}


int set_charge_led(struct charger *charger, int64_t now)
{
    int led_on = 0;
    int fd = 0;
    unsigned char reg = 0;

    LOGV("[%lld] cur_led_state=%d cur_blink_cnt=%d next_blink=[%lld]\n",now,charger->cur_led_state,charger->cur_blink_cnt,charger->next_blink);

    switch(charger->cur_led_state)
    {
        case LED_STATE_BLINK:
            if (now < charger->next_blink)
            {
                /* It's not time to blink yet */
                return 0;
            }
            else
            {
                /* Normally at this point of the boot up process the charge LED is already ON so
                    when we blink the charge led we want to start with the off state */
                led_on = (charger->cur_blink_cnt % 2);
            }
            break;
        case LED_STATE_ON:
            led_on = 1;
            break;
        case LED_STATE_OFF:
            led_on = 0;
            break;
        default:
            LOGE("Invalid LED state\n");
            return -1;
    }

    fd = open("/dev/i2c-1", O_RDWR);
    if(fd < 0){
        LOGE("%s failed to open\n", __func__);
        return -1;
    }

    if(led_on) {
        /* Turn on charge LED */
        get_i2c_register(fd, LED_I2C_ADDRESS, PMIC_LED_PWM_CTRL2, &reg);
        reg = (reg & ~(0x3)) | PMIC_LED_PWM_ON;
        set_i2c_register(fd, LED_I2C_ADDRESS, PMIC_LED_PWM_CTRL2, reg);

    } else {
        /* Turn off charge LED */
        get_i2c_register(fd, LED_I2C_ADDRESS, PMIC_LED_PWM_CTRL2, &reg);
        reg = (reg & ~(0x3)) | PMIC_LED_PWM_OFF;
        set_i2c_register(fd, LED_I2C_ADDRESS, PMIC_LED_PWM_CTRL2, reg);
    }

    if(charger->cur_led_state == LED_STATE_BLINK) {
        if(--charger->cur_blink_cnt <= 0) {
            /* Done with blinking */
            charger->cur_led_state = LED_STATE_NOP;
            charger->next_blink = -1;
            /* Return control of charge LED to HW */
            get_i2c_register(fd, LED_I2C_ADDRESS, PMIC_LED_PWM_CTRL2, &reg);
            reg = (reg & ~(0x3)) | PMIC_LED_PWM_AUTO;
            set_i2c_register(fd, LED_I2C_ADDRESS, PMIC_LED_PWM_CTRL2, reg);
        } else {
            charger->next_blink = now + LED_BLINK_DELAY_MS;
        }
    }

    close(fd);

    return 0;
}


int set_power_led(bool led_on)
{
    int ret, fd;
    unsigned char buf[3];

    fd = open("/dev/i2c-1", O_RDWR);
    if(fd < 0){
        LOGE("%s failed to open\n", __func__);
        return -1;
    }

    // Set to 7Bit Address
    // And if success - now attempt to set the Slave Address
    ret = (ioctl(fd, I2C_TENBIT, BITS_7) >= 0) ? ((ioctl(fd, I2C_SLAVE_FORCE, LED_I2C_ADDRESS) >= 0) ? 0 : -3) : -2;
    LOGV("ret=%d led_on=%d\n",ret,led_on);
    if(ret == 0)
    {
        if(led_on) {
            buf[0] = PMC_PWM2ON;
            buf[1] = PWM2ON_PEROID;
            buf[2] = PWM2OFF_PEROID;
            write(fd, buf, 3);

            buf[0] = PMC_TOGGLE3;
            buf[1] = (PWM2EN_CLOCK_INPUT | PWM2EN_SIGNAL);
            write(fd, buf, 2);
        } else {
            buf[0] = PMC_TOGGLE3;
            buf[1] = (PWM2EN_CLOCK_INPUT | PWM2DISABLE_SIGNAL);
            write(fd, buf, 2);
        }
    }

    close(fd);

    return ret;
}

static int check_charge_status(char data)
{
    int value;
    switch(data){
        case 'C':
            value=POWER_SUPPLY_STATUS_CHARGING;
        break;
        case 'D':
            value=POWER_SUPPLY_STATUS_DISCHARGING;
        break;
        case 'F':
            value=POWER_SUPPLY_STATUS_FULL;
        break;
        case 'N':
            value=POWER_SUPPLY_STATUS_NOT_CHARGING;
        break;
        default:
            value=POWER_SUPPLY_STATUS_UNKNOWN;
        break;
    }
    return value;
}
#if 0
static int get_charging_status()
{
    char buf[32];
    int ret;

    if(read_file(BAT_STATUS_PATH, buf, sizeof(buf)) < 0) {
        LOGI("[%s:%d] Failed to get Status, assuming charging\n",__FUNCTION__,__LINE__);
        return POWER_SUPPLY_STATUS_CHARGING;
    } else {
        return check_charge_status(buf[0]);
    }
}
#endif

static int get_battery_capacity(struct charger *charger)
{
    int ret;
    int batt_cap = -1;

    if (!charger->battery)
        return -1;

    ret = read_file_int(charger->battery->cap_path, &batt_cap);
    if (ret < 0 || batt_cap > 100) {
        batt_cap = -1;
    }

//    LOGI("[%s:%d] batt_cap=%d\n",__FUNCTION__,__LINE__,batt_cap);

    return batt_cap;
}

static void boot_to_android(struct charger *charger) {
#if MTP_ENABLE
    LOGI("boot_to_android mtp_only=%d\n",charger->mtp_only);
    if(charger->mtp_only) {
        property_set("sys.boot_to_android", "0");
        write_str(KOPIN_CTRL, "BOOTMODE_CHA", 12);
    } else 
#endif
    {
        set_power_led(1);
        write_str(KOPIN_CTRL, "BOOTMODE_NOR", 12);
    }

    exit (1);
}

static bool is_battery_low(struct charger *charger) {
    return (get_battery_capacity(charger) < BATTERY_LOW_THRESH);
}
#if MTP_ENABLE
static void check_if_boot_to_mtp(struct charger *charger) {
    if( (charger->cur_charger == POWER_SUPPLY_TYPE_USB) ||
        (charger->cur_charger == POWER_SUPPLY_TYPE_USB_CDP)) {
        LOGV("[%s:%u]: PC Charger\n",__FUNCTION__,__LINE__);

        if(shutdown_charge_enable)
            return;
        if(!is_battery_low(charger)) {
            boot_to_android(charger);
        }
    } else {
        LOGV("[%s:%u]: Other Charger\n",__FUNCTION__,__LINE__);
    }
}
#endif
static struct power_supply *find_supply(struct charger *charger,
                                        const char *name)
{
    struct listnode *node;
    struct power_supply *supply;

    list_for_each(node, &charger->supplies) {
        supply = node_to_item(node, struct power_supply, list);
        if (!strncmp(name, supply->name, sizeof(supply->name)))
            return supply;
    }
    return NULL;
}

static struct power_supply *add_supply(struct charger *charger,
                                       const char *name, const char *type,
                                       const char *path, bool online)
{
    struct power_supply *supply;

    supply = calloc(1, sizeof(struct power_supply));
    if (!supply)
        return NULL;

    strlcpy(supply->name, name, sizeof(supply->name));
    strlcpy(supply->type, type, sizeof(supply->type));
    snprintf(supply->cap_path, sizeof(supply->cap_path),
             "/sys/%s/capacity", path);
    supply->online = online;
    list_add_tail(&charger->supplies, &supply->list);
    charger->num_supplies++;
    LOGV("... added %s %s %d\n", supply->name, supply->type, online);
    return supply;
}

static void remove_supply(struct charger *charger, struct power_supply *supply)
{
    if (!supply)
        return;
    list_remove(&supply->list);
    charger->num_supplies--;
    free(supply);
}

static void parse_uevent(struct charger *charger, const char *msg, struct uevent *uevent)
{
    uevent->action = "";
    uevent->path = "";
    uevent->subsystem = "";
    uevent->ps_name = "";
    uevent->ps_online = "";
    uevent->ps_type = "";
    uevent->ps_status = "";
    /* currently ignoring SEQNUM */
    while (*msg) {
#ifdef DEBUG_UEVENTS
        LOGI("uevent str: %s\n", msg);
#endif
        if (!strncmp(msg, "ACTION=", 7)) {
            msg += 7;
            uevent->action = msg;
        } else if (!strncmp(msg, "DEVPATH=", 8)) {
            msg += 8;
            uevent->path = msg;
        } else if (!strncmp(msg, "SUBSYSTEM=", 10)) {
            msg += 10;
            uevent->subsystem = msg;
        } else if (!strncmp(msg, "POWER_SUPPLY_NAME=", 18)) {
            msg += 18;
            uevent->ps_name = msg;
        } else if (!strncmp(msg, "POWER_SUPPLY_ONLINE=", 20)) {
            msg += 20;
            uevent->ps_online = msg;
        } else if (!strncmp(msg, "POWER_SUPPLY_TYPE=", 18)) {
            msg += 18;
            uevent->ps_type = msg;
        } else if (!strncmp(msg, "POWER_SUPPLY_STATUS=", 20)) {
            msg += 20;
            uevent->ps_status = msg;
        }
        /* advance to after the next \0 */
        while (*msg++)
            ;
    }
#ifdef DEBUG_UEVENTS
    LOGI("event { %s\n %s\n %s\n %s\n %s\n %s\n %s\n}",
         uevent->action, uevent->path, uevent->subsystem,
         uevent->ps_name, uevent->ps_type, uevent->ps_online, uevent->ps_status);
#endif
    /*Check USB host type*/
    if(!strncmp(uevent->ps_name, "twl6030_usb", 11)) {
        charger->cur_charger = uevent->ps_online[0]-48;
#if MTP_ENABLE
        check_if_boot_to_mtp(charger);
#endif
    }

    /*Check if still charge*/
    if(uevent->ps_status[0] != '\0')
        charger->charge_status=check_charge_status(uevent->ps_status[0]);
}

static void handle_battery_full_state(struct charger *charger, int64_t now)
{
    if((charger->next_full_check != -1) && (now >= charger->next_full_check)) {
        LOGI("[%lld] Charging is done shutting down...\n", now);
        android_reboot(ANDROID_RB_POWEROFF, 0, 0);
    }
}

static void process_ps_uevent(struct charger *charger, struct uevent *uevent)
{
    int online;
    char ps_type[32];
    struct power_supply *supply = NULL;
    int i;
    bool was_online = false;
    bool battery = false;

    if (uevent->ps_type[0] == '\0') {
        char *path;
        int ret;

        if (uevent->path[0] == '\0')
            return;
        ret = asprintf(&path, "/sys/%s/type", uevent->path);
        if (ret <= 0)
            return;
        ret = read_file(path, ps_type, sizeof(ps_type));
        free(path);
        if (ret < 0)
            return;
    } else {
        strlcpy(ps_type, uevent->ps_type, sizeof(ps_type));
    }

    if (!strncmp(ps_type, "Battery", 7))
        battery = true;

    online = atoi(uevent->ps_online);
    supply = find_supply(charger, uevent->ps_name);
    if (supply) {
        was_online = supply->online;
        supply->online = online;
    }

    if (!strcmp(uevent->action, "add")) {
        if (!supply) {
            supply = add_supply(charger, uevent->ps_name, ps_type, uevent->path,
                                online);
            if (!supply) {
                LOGE("cannot add supply '%s' (%s %d)\n", uevent->ps_name,
                     uevent->ps_type, online);
                return;
            }
            /* only pick up the first battery for now */
            if (battery && !charger->battery)
                charger->battery = supply;
        } else {
            LOGE("supply '%s' already exists..\n", uevent->ps_name);
        }
    } else if (!strcmp(uevent->action, "remove")) {
        if (supply) {
            if (charger->battery == supply)
                charger->battery = NULL;
            remove_supply(charger, supply);
            supply = NULL;
        }
    } else if (!strcmp(uevent->action, "change")) {
        if (!supply) {
            LOGE("power supply '%s' not found ('%s' %d)\n",
                 uevent->ps_name, ps_type, online);
            return;
        }
    } else {
        return;
    }

    /* allow battery to be managed in the supply list but make it not
     * contribute to online power supplies. */
    if (!battery) {
        if (was_online && !online)
            charger->num_supplies_online--;
        else if (supply && !was_online && online)
            charger->num_supplies_online++;
    }

    LOGI("cap=%d charging=%d power supply %s (%s) %s (action=%s num_online=%d num_supplies=%d)\n",
         get_battery_capacity(charger), charger->charge_status,
         uevent->ps_name, ps_type, battery ? "" : online ? "online" : "offline",
         uevent->action, charger->num_supplies_online, charger->num_supplies);

    if(charger->charge_status == POWER_SUPPLY_STATUS_CHARGING) {
        charger->next_full_check = -1;
    } else {
        int64_t now = curr_time_ms();
        if (charger->next_full_check == -1) {
            if(charger->charge_status == POWER_SUPPLY_STATUS_FULL) {
                charger->next_full_check = now + BATT_FULL_SHUTDOWN_TIME;
                LOGI("[%lld] Battery is FULL: shutting down in %lld (@ %lld)\n", now, BATT_FULL_SHUTDOWN_TIME, charger->next_full_check);
            }
            else{
                charger->next_full_check = now + BATT_NONCHARGE_SHUTDOWN_TIME;
                LOGI("[%lld] Battery not charge: shutting down in %lld (@ %lld)\n", now, BATT_NONCHARGE_SHUTDOWN_TIME, charger->next_full_check);
            }
        } else {
            handle_battery_full_state(charger, now);
        }
    }
}

static void process_uevent(struct charger *charger, struct uevent *uevent)
{
    if (!strcmp(uevent->subsystem, "power_supply"))
        process_ps_uevent(charger, uevent);
}

#define UEVENT_MSG_LEN  1024
static int handle_uevent_fd(struct charger *charger, int fd)
{
    char msg[UEVENT_MSG_LEN+2];
    int n;

    if (fd < 0)
        return -1;

    while (true) {
        struct uevent uevent;

        n = uevent_kernel_multicast_recv(fd, msg, UEVENT_MSG_LEN);
        if (n <= 0)
            break;
        if (n >= UEVENT_MSG_LEN)   /* overflow -- discard */
            continue;

        msg[n] = '\0';
        msg[n+1] = '\0';

        parse_uevent(charger, msg, &uevent);
        process_uevent(charger, &uevent);
    }

    return 0;
}

static int uevent_callback(int fd, short revents, void *data)
{
    struct charger *charger = data;

    if (!(revents & POLLIN))
        return -1;
    return handle_uevent_fd(charger, fd);
}

/* force the kernel to regenerate the change events for the existing
 * devices, if valid */
static void do_coldboot(struct charger *charger, DIR *d, const char *event,
                        bool follow_links, int max_depth)
{
    struct dirent *de;
    int dfd, fd;

    dfd = dirfd(d);

    fd = openat(dfd, "uevent", O_WRONLY);
    if (fd >= 0) {
        write(fd, event, strlen(event));
        close(fd);
        handle_uevent_fd(charger, charger->uevent_fd);
    }

    while ((de = readdir(d)) && max_depth > 0) {
        DIR *d2;

        LOGV("looking at '%s'\n", de->d_name);

        if ((de->d_type != DT_DIR && !(de->d_type == DT_LNK && follow_links)) ||
           de->d_name[0] == '.') {
            LOGV("skipping '%s' type %d (depth=%d follow=%d)\n",
                 de->d_name, de->d_type, max_depth, follow_links);
            continue;
        }
        LOGV("can descend into '%s'\n", de->d_name);

        fd = openat(dfd, de->d_name, O_RDONLY | O_DIRECTORY);
        if (fd < 0) {
            LOGE("cannot openat %d '%s' (%d: %s)\n", dfd, de->d_name,
                 errno, strerror(errno));
            continue;
        }

        d2 = fdopendir(fd);
        if (d2 == 0)
            close(fd);
        else {
            LOGV("opened '%s'\n", de->d_name);
            do_coldboot(charger, d2, event, follow_links, max_depth - 1);
            closedir(d2);
        }
    }
}

static void coldboot(struct charger *charger, const char *path,
                     const char *event)
{
    char str[256];

    LOGV("doing coldboot '%s' in '%s'\n", event, path);
    DIR *d = opendir(path);
    if (d) {
        snprintf(str, sizeof(str), "%s\n", event);
        do_coldboot(charger, d, str, true, 1);
        closedir(d);
    }
}

static int set_key_callback(int code, int value, void *data)
{
    if(code == KEY_END)
        code = KEY_POWER;

    struct charger *charger = data;
    int64_t now = curr_time_ms();
    int down = !!value;

    if (code > KEY_MAX)
        return -1;

    /* ignore events that don't modify our state */
    if (charger->keys[code].down == down)
        return 0;

    /* only record the down even timestamp, as the amount
     * of time the key spent not being pressed is not useful */
    if (down)
        charger->keys[code].timestamp = now;
    charger->keys[code].down = down;
    charger->keys[code].pending = true;
    if (down) {
        LOGV("[%lld] key[%d] down\n", now, code);
    } else {
        int64_t duration = now - charger->keys[code].timestamp;
        int64_t secs = duration / 1000;
        int64_t msecs = duration - secs * 1000;
        LOGV("[%lld] key[%d] up (was down for %lld.%lldsec)\n", now,
            code, secs, msecs);
    }

    return 0;
}

static void update_input_state(struct charger *charger,
                               struct input_event *ev)
{
    if (ev->type != EV_KEY)
        return;
    set_key_callback(ev->code, ev->value, charger);
}

static void set_next_key_check(struct charger *charger,
                               struct key_state *key,
                               int64_t timeout)
{
    int64_t then = key->timestamp + timeout;

    if (charger->next_key_check == -1 || then < charger->next_key_check)
        charger->next_key_check = then;
}

static void process_key(struct charger *charger, int code, int64_t now)
{
    int batt_cap = -1;
    struct key_state *key = &charger->keys[code];
    int64_t next_key_check;

    if (code == KEY_POWER) {
        if (key->down) {
        } else if (key->pending) {
            LOGI("[%s:%u] code == KEY_POWER key->down\n",__FUNCTION__,__LINE__);
#if MTP_ENABLE
            charger->mtp_only = false;
#endif
            if(is_battery_low(charger)) {
                charger->cur_led_state = LED_STATE_BLINK;
                charger->cur_blink_cnt = LED_BLINK_CNT*2; // One for ON and another one for OFF
            } else {
                boot_to_android(charger); // Should only be called if charger didn't exit due to low battery
            }
        }
    }

    if(charger->cur_led_state == LED_STATE_BLINK) {
        set_charge_led(charger, now);
    } else {
        charger->next_blink = -1;
    }

    key->pending = false;
}

static void handle_input_state(struct charger *charger, int64_t now)
{
    process_key(charger, KEY_POWER, now);

    if (charger->next_key_check != -1 && now > charger->next_key_check)
        charger->next_key_check = -1;
}

static void handle_power_supply_state(struct charger *charger, int64_t now)
{
    if (charger->num_supplies_online == 0) {
        if (charger->next_pwr_check == -1) {
            charger->next_pwr_check = now + UNPLUGGED_SHUTDOWN_TIME;
            LOGI("[%lld] device unplugged: shutting down in %lld (@ %lld)\n",
                 now, UNPLUGGED_SHUTDOWN_TIME, charger->next_pwr_check);
        } else if (now >= charger->next_pwr_check) {
            LOGI("[%lld] shutting down\n", now);
            android_reboot(ANDROID_RB_POWEROFF, 0, 0);
        } else {
            /* otherwise we already have a shutdown timer scheduled */
        }
    } else {
        /* online supply present, reset shutdown timer if set */
        if (charger->next_pwr_check != -1) {
            LOGI("[%lld] device plugged in: shutdown cancelled\n", now);
        }
        charger->next_pwr_check = -1;
    }
}

static void wait_next_event(struct charger *charger, int64_t now)
{
    int64_t next_event = INT64_MAX;
    int64_t timeout;
    struct input_event ev;
    int ret;

    LOGV("[%lld] next key: %lld next pwr: %lld next blink: %lld next full:%lld\n", now,
         charger->next_key_check,
         charger->next_pwr_check,charger->next_blink, charger->next_full_check);

    if (charger->next_blink != -1 && charger->next_blink < next_event)
        next_event = charger->next_blink;
    if (charger->next_key_check != -1 && charger->next_key_check < next_event)
        next_event = charger->next_key_check;
    if (charger->next_pwr_check != -1 && charger->next_pwr_check < next_event)
        next_event = charger->next_pwr_check;
    if (charger->next_full_check != -1 && charger->next_full_check < next_event)
        next_event = charger->next_full_check;

    if (next_event != -1 && next_event != INT64_MAX)
        timeout = max(0, next_event - now);
    else
        timeout = -1;
    LOGV("[%lld] blocking (%lld)\n", now, timeout);
    ret = ev_wait((int)timeout);
    if (!ret)
        ev_dispatch();
}

static int input_callback(int fd, short revents, void *data)
{
    struct charger *charger = data;
    struct input_event ev;
    int ret;

    ret = ev_get_input(fd, revents, &ev);
    if (ret)
        return -1;
    update_input_state(charger, &ev);
    return 0;
}

static void event_loop(struct charger *charger)
{
    int ret;

    while (true) {
        int64_t now = curr_time_ms();
#ifdef DEBUG_UEVENTS
        LOGI("[%lld] event_loop()\n", now);
#endif
        handle_input_state(charger, now);
        handle_power_supply_state(charger, now);
        handle_battery_full_state(charger, now);
#if MTP_ENABLE
        check_if_boot_to_mtp(charger);
#endif
        wait_next_event(charger, now);
    }
}

static bool is_manufacture_version(void)
{
    char value[PROPERTY_VALUE_MAX];

    property_get(MANUFACTURE_PROPERTY, value, "");

    if(!strcmp(value, "1")) {
        return true;
    }
    return false;
}

int main(int argc, char **argv)
{
    int ret;
    struct charger *charger = &charger_state;
    int64_t now = curr_time_ms() - 1;
    int fd;
    int i;

    list_init(&charger->supplies);

    klog_init();
    klog_set_level(CHARGER_KLOG_LEVEL);
#ifdef DEBUG_DUMP_KMSG
    dump_last_kmsg();
#endif
    if(is_manufacture_version()){
        LOGI("%s set\n", MANUFACTURE_PROPERTY);
        boot_to_android(charger);
    }

    shutdown_charge_enable = is_shutdown_charge();
    LOGI("START CHARGER; shutdown_charge_enable=%d\n",shutdown_charge_enable);

    ev_init(input_callback, charger);

    fd = uevent_open_socket(64*1024, true);
    if (fd >= 0) {
        fcntl(fd, F_SETFL, O_NONBLOCK);
        ev_add_fd(fd, uevent_callback, charger);
    }
    charger->uevent_fd = fd;
    coldboot(charger, "/sys/class/power_supply", "add");

    ev_sync_key_state(set_key_callback, charger);

    event_loop(charger);

    return 0;
}
