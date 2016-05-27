#include <fcntl.h>
#include <errno.h>
#include <string.h>
#include <poll.h>
#include <unistd.h>
#include <dirent.h>
#include <stdio.h>
#include <stdlib.h>
#include <linux/input.h>
#include <linux/netlink.h>
#include <sys/socket.h>

#define LOG_TAG "mtp_monitor"
#include <cutils/properties.h>
#include <cutils/log.h>

/*button event*/
#define BUTTON_INPUT    "twl6030_pwrbutton"
#define MAX_LENGTH      1024
#define MAX_EVENTS      4

#define TIME_DELAY      20000//20 s
#define BUTTON_INDEX    0

#define SYSRQ           "/proc/sys/kernel/sysrq"
#define SYSRQ_TRIGGER   "/proc/sysrq-trigger"

#define KOPIN_CTRL      "/proc/kopinctrl"

/* The following constant is the IOCTL number defined in /linux/i2c-dev.h*/
#define I2C_SLAVE       0x0703
#define I2C_SLAVE_FORCE 0x0706
#define I2C_TENBIT      0x0704

#define LED_I2C_ADDRESS 0x49
#define BITS_7          0
#define PMC_PWM2ON      0xBD
#define PMC_PWM2OFF     0xBE
#define PMC_TOGGLE3     0x92

#define PWM2ON_PEROID       0x60
#define PWM2OFF_PEROID      0x7F
#define PWM2EN_CLOCK_INPUT  (1 << 5)
#define PWM2EN_SIGNAL       (1 << 4)


static int acquire_powerbutton_state(int fd)
{
    int i;
    struct input_event ev[MAX_EVENTS];
    int rb;
    unsigned char button_press=0;
    rb=read(fd,ev, sizeof(ev));
    rb=rb/sizeof(ev[0]);
    if(rb < 0){
        ALOGE("read error\n");
        return -1;
    }

    for(i=0; i<rb; i++) {
        if (ev[i].type == EV_KEY) {
            ALOGD("power button code=%d,value=%d\n",ev[i].code,ev[i].value);
            button_press=ev[i].value;
            if(button_press)
                break;
        }
        if (ev[i].type == EV_SYN) {
            continue;
        }
    }
    return button_press;
}

static int openInput(const char* inputName){
    int fd = -1;
    const char *dirname = "/dev/input";
    char devname[MAX_LENGTH]={0};
    char *filename;
    DIR *dir;
    struct dirent *de;
    char name[80];

    dir = opendir(dirname);
    if(dir == NULL)
    {
        ALOGE("cannot open input directory!\n");
        return -1;
    }
    strcpy(devname, dirname);
    filename = devname + strlen(devname);
    *filename++ = '/';
    /*Check all files here*/
     while((de = readdir(dir))) {
        /*find ".", continue*/
        if(de->d_name[0] == '.' && de->d_name[1] == '\0')
            continue;
        /*find "." continue*/
        if(de->d_name[0] == '.' && de->d_name[1] == '.' && de->d_name[2] == '\0')
            continue;

        strcpy(filename, de->d_name);
        fd = open(devname, O_RDWR);//O_RDONLY
        if (fd>=0) {

            if (ioctl(fd, EVIOCGNAME(sizeof(name) - 1), &name) < 1) {
                name[0] = '\0';
            }

            if(inputName){
                if (!strcmp(name, inputName)) {
                    ALOGD("Input Device File: %s\n",devname);
                    break;
                } else {
                    close(fd);
                    fd = -1;
                }
            }
            else
                ALOGD("%s found\n", name);
        }
    }
    closedir(dir);

    return fd;
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
        ALOGE("write_int failed to open %s\n", path);
        return -errno;
    }
}

/*void android_shutdown(void)
{
    ALOGD("shut down\n");
#ifdef  TEST_DEBUG
    return;
#endif
    system("am broadcast -a com.reconinstruments.SHUTDOWN");

    // Force Shutdown after 20 seconds
    usleep(20*1000*1000);
    write_str(SYSRQ, "1", 1);//enable SysRq completely
    write_str(SYSRQ_TRIGGER, "o", 1);//shut down
    return;
}*/

int turn_on_blue_led(void)
{
    int ret,fd;
    unsigned char buf[3];

    fd = open("/dev/i2c-1", O_RDWR);
    if(fd < 0){
        ALOGE("%s failed to open\n", __func__);
        return -1;
    }

    ret = (ioctl(fd, I2C_TENBIT, BITS_7) >= 0) ? ((ioctl(fd, I2C_SLAVE_FORCE, LED_I2C_ADDRESS) >= 0) ? 0 : -3) : -2;

    if(ret==0)
    {
        buf[0] = PMC_PWM2ON;
        buf[1] = PWM2ON_PEROID;
        buf[2] = PWM2OFF_PEROID;
        write(fd, buf, 3);

        buf[0] = PMC_TOGGLE3;
        buf[1] = (PWM2EN_CLOCK_INPUT | PWM2EN_SIGNAL);
        write(fd, buf, 2);
    }

    close(fd);
    return ret;
}

static void boot_to_android() {
    if(turn_on_blue_led() < 0)
        ALOGE("[%s:%u] Couldn't turn on led\n",__FUNCTION__,__LINE__);

    ALOGI("[%s:%u] BOOTMODE_NOR\n",__FUNCTION__,__LINE__);
    write_str(KOPIN_CTRL, "BOOTMODE_NOR", 12);
    property_set("sys.boot_to_android", "1");
}

int main(int argc, char *argv[])
{
    int fd_button;
    int ret;
    struct pollfd fds;

    /* Power button handle */
    fd_button                 = openInput(BUTTON_INPUT);
    fds.fd      = fd_button;
    fds.events  = POLLIN;
    fds.revents = 0;

    ALOGD("[%s:%u]\n",__FUNCTION__,__LINE__);

    /*Enter sleep and wakeup loop*/
    while(1){
        ret = poll (&fds, 1, TIME_DELAY) ;
        if(ret < 0)
        {
            ALOGE("Couldn't poll %d\n",ret);
            continue;
        }

        if((ret > 0) && (fds.revents == POLLIN)){
            ALOGD("[%s:%u] Detected Power Button Press\n",__FUNCTION__,__LINE__);
            if(acquire_powerbutton_state(fd_button)) {
                boot_to_android();
                break; // Exit
            }
        }
    }
    ALOGD("[%s:%u] Exit\n",__FUNCTION__,__LINE__);
    close(fd_button);
    return 0;
}
