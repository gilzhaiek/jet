#ifndef AMPLIFIER_LOG_H
#define AMPLIFIER_LOG_H

#ifdef JET_SYSTEM
#define LOG_NDEBUG 0
#define LOG_TAG "amplifier_Tfa98xx"
#include <cutils/log.h>
#define print_verbose(fmt, args...) ALOGV(fmt, ##args)
#define print_err(fmt, args...) ALOGE(fmt, ##args)

#else
#define print_verbose(fmt, args...) fprintf(stdout,fmt, ##args)
#define print_err(fmt, args...) fprintf(stderr, fmt, ##args)

#endif

#define __print_err_line print_err( "%s %d err!-----\n",__FUNCTION__,__LINE__)
#define print_if_false(e) if (!(e)) print_err( "%s %d err!-----\n",__FUNCTION__,__LINE__)
#endif