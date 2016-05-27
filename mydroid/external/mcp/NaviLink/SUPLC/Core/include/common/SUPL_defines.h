#ifndef _SUPL_DEFINES_H_
#define _SUPL_DEFINES_H_

//SUPL version
#define MAJ_VERSION 1
#define MIN_VERSION 0
#define SER_VERSION 0


#define TLS_ENABLED
#define NOTIFICATIONS_ENABLED
#define HASH_ENABLED
#define NOTIFICATION_TIME_OUT      15000  //15 seconds for notifications
#define UT1234_TIME_OUT_VALUE	   10     //10 seconds of waiting messages

#ifdef MCP_LOGD
#define LOGD ALOGD
#else
#define LOGD
#endif

#endif //_SUPL_DEFINES_H_
