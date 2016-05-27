/*****< SS1CFG.h >*************************************************************/
/*      Copyright 2000 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  SS1CFG - Configuration Definitions and Constants, used by the Stonestreet */
/*           One Bluetopia PM stack for Android.                              */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   07/20/13  G. Hensley     Initial creation.                               */
/******************************************************************************/

#ifndef __SS1CFGH__
#define __SS1CFGH__

#include "utils/Log.h"

#include <pthread.h>
#include <stdio.h>

#include "SS1BTPM.h"

   /* Enable logging in the Bluetooth JNI Bridge modules.               */
#define ENABLE_SS1_LOGGING       FALSE
   /* Include function names in log output.                             */
#define LOG_FUNCTION_NAMES       TRUE

/* Enable support for profiles supported natively by Android.           */
#define SS1_SUPPORT_A2DP         TRUE
#define SS1_SUPPORT_HID          TRUE
#define SS1_SUPPORT_PAN          TRUE
#define SS1_SUPPORT_GATT         TRUE
#define SS1_SUPPORT_HFRE_HSP     TRUE
#define SS1_SUPPORT_HEALTH       FALSE

#define BTPM_CLIENT_INIT_DEFAULT_RETRY_COUNT   0
#define BTPM_CLIENT_INIT_DEFAULT_DELAY_MS    200

   /* Debug zones and levels to enable for the Bluetopia PM service     */
   /* (SS1BTPM). See the headers, DBGAPI.h and BTPMDBGZ.h for a full    */
   /* list of available Debug Zones and Levels.                         */
#define BTPM_SERVICE_DEBUG_ZONES (                                       \
                               /* BTPM_DEBUG_ZONE_INIT              | */ \
                               /* BTPM_DEBUG_ZONE_SETTINGS          | */ \
                               /* BTPM_DEBUG_ZONE_TIMER             | */ \
                               /* BTPM_DEBUG_ZONE_MODULE_MANAGER    | */ \
                               /* BTPM_DEBUG_ZONE_IPC               | */ \
                               /* BTPM_DEBUG_ZONE_MESSAGE           | */ \
                               /* BTPM_DEBUG_ZONE_MAIN              | */ \
                               /* BTPM_DEBUG_ZONE_LOCAL_DEVICE      | */ \
                               /* BTPM_DEBUG_ZONE_SERIAL_PORT       | */ \
                               /* BTPM_DEBUG_ZONE_SCO               | */ \
                               /* BTPM_DEBUG_ZONE_GENERIC_ATTRIBUTE | */ \
                               /* BTPM_DEBUG_ZONE_AUDIO             | */ \
                               /* BTPM_DEBUG_ZONE_HID               | */ \
                               /* BTPM_DEBUG_ZONE_HANDS_FREE        | */ \
                               /* BTPM_DEBUG_ZONE_PHONE_BOOK_ACCESS | */ \
                               /* BTPM_DEBUG_ZONE_PAN               | */ \
                               /* BTPM_DEBUG_ZONE_MESSAGE_ACCESS    | */ \
                               /* BTPM_DEBUG_ZONE_FTP               | */ \
                               /* BTPM_DEBUG_ZONE_HEADSET           | */ \
                               /* BTPM_DEBUG_ZONE_HEALTH_DEVICE     | */ \
                                  0)

#define BTPM_SERVICE_DEBUG_ZONES_PAGE_1 (                          \
                               /* BTPM_DEBUG_ZONE_OBJECT_PUSH | */ \
                               /* BTPM_DEBUG_ZONE_ANT_PLUS    | */ \
                               /* BTPM_DEBUG_ZONE_HID_DEVICE  | */ \
                                  0x40000000)

#define BTPM_SERVICE_DEBUG_ZONES_PAGE_2 (                                    \
                               /* BTPM_DEBUG_ZONE_LE_ALERT_NOTIFICATION | */ \
                               /* BTPM_DEBUG_ZONE_LE_HEART_RATE         | */ \
                               /* BTPM_DEBUG_ZONE_LE_PROXIMITY          | */ \
                               /* BTPM_DEBUG_ZONE_LE_FIND_ME            | */ \
                               /* BTPM_DEBUG_ZONE_LE_TIME               | */ \
                               /* BTPM_DEBUG_ZONE_LE_PHONE_ALERT        | */ \
                               /* BTPM_DEBUG_ZONE_LE_BATTERY            | */ \
                               /* BTPM_DEBUG_ZONE_LE_BLOOD_PRESSURE     | */ \
                               /* BTPM_DEBUG_ZONE_LE_HID_OVER_GATT      | */ \
                               /* BTPM_DEBUG_ZONE_LE_HEALTH_THERMOMETER | */ \
                               /* BTPM_DEBUG_ZONE_LE_GLUCOSE            | */ \
                               /* BTPM_DEBUG_ZONE_LE_ANCS               | */ \
                                  0x80000000)

#define BTPM_SERVICE_DEBUG_LEVELS (                               \
                                  BTPM_DEBUG_LEVEL_FUNCTION  |    \
                               /* BTPM_DEBUG_LEVEL_DATA_DUMP | */ \
                                  BTPM_DEBUG_LEVEL_VERBOSE   |    \
                                  BTPM_DEBUG_LEVEL_TIMEOUT   |    \
                                  BTPM_DEBUG_LEVEL_WARNING   |    \
                                  BTPM_DEBUG_LEVEL_FAILURE   |    \
                                  BTPM_DEBUG_LEVEL_CRITICAL  |    \
                                  0)


   /* Debug zones and levels to enable for the Bluetopia PM clients. See*/
   /* the headers, DBGAPI.h and BTPMDBGZ.h for a full list of available */
   /* Debug Zones and Levels.                                           */
#define BTPM_CLIENT_DEBUG_ZONES (                                        \
                               /* BTPM_DEBUG_ZONE_INIT              | */ \
                               /* BTPM_DEBUG_ZONE_SETTINGS          | */ \
                               /* BTPM_DEBUG_ZONE_TIMER             | */ \
                               /* BTPM_DEBUG_ZONE_MODULE_MANAGER    | */ \
                               /* BTPM_DEBUG_ZONE_IPC               | */ \
                               /* BTPM_DEBUG_ZONE_MESSAGE           | */ \
                               /* BTPM_DEBUG_ZONE_MAIN              | */ \
                               /* BTPM_DEBUG_ZONE_LOCAL_DEVICE      | */ \
                               /* BTPM_DEBUG_ZONE_SERIAL_PORT       | */ \
                               /* BTPM_DEBUG_ZONE_SCO               | */ \
                               /* BTPM_DEBUG_ZONE_GENERIC_ATTRIBUTE | */ \
                               /* BTPM_DEBUG_ZONE_AUDIO             | */ \
                               /* BTPM_DEBUG_ZONE_HID               | */ \
                               /* BTPM_DEBUG_ZONE_HANDS_FREE        | */ \
                               /* BTPM_DEBUG_ZONE_PHONE_BOOK_ACCESS | */ \
                               /* BTPM_DEBUG_ZONE_PAN               | */ \
                               /* BTPM_DEBUG_ZONE_MESSAGE_ACCESS    | */ \
                               /* BTPM_DEBUG_ZONE_FTP               | */ \
                               /* BTPM_DEBUG_ZONE_HEADSET           | */ \
                               /* BTPM_DEBUG_ZONE_HEALTH_DEVICE     | */ \
                                  0)

#define BTPM_CLIENT_DEBUG_ZONES_PAGE_1 (                           \
                               /* BTPM_DEBUG_ZONE_OBJECT_PUSH | */ \
                               /* BTPM_DEBUG_ZONE_ANT_PLUS    | */ \
                               /* BTPM_DEBUG_ZONE_HID_DEVICE  | */ \
                                  0x40000000)

#define BTPM_CLIENT_DEBUG_ZONES_PAGE_2 (                                     \
                               /* BTPM_DEBUG_ZONE_LE_ALERT_NOTIFICATION | */ \
                               /* BTPM_DEBUG_ZONE_LE_HEART_RATE         | */ \
                               /* BTPM_DEBUG_ZONE_LE_PROXIMITY          | */ \
                               /* BTPM_DEBUG_ZONE_LE_FIND_ME            | */ \
                               /* BTPM_DEBUG_ZONE_LE_TIME               | */ \
                               /* BTPM_DEBUG_ZONE_LE_PHONE_ALERT        | */ \
                               /* BTPM_DEBUG_ZONE_LE_BATTERY            | */ \
                               /* BTPM_DEBUG_ZONE_LE_BLOOD_PRESSURE     | */ \
                               /* BTPM_DEBUG_ZONE_LE_HID_OVER_GATT      | */ \
                               /* BTPM_DEBUG_ZONE_LE_HEALTH_THERMOMETER | */ \
                               /* BTPM_DEBUG_ZONE_LE_GLUCOSE            | */ \
                               /* BTPM_DEBUG_ZONE_LE_ANCS               | */ \
                                  0x80000000)

#define BTPM_CLIENT_DEBUG_LEVELS (                                \
                                  BTPM_DEBUG_LEVEL_FUNCTION  |    \
                               /* BTPM_DEBUG_LEVEL_DATA_DUMP | */ \
                                  BTPM_DEBUG_LEVEL_VERBOSE   |    \
                                  BTPM_DEBUG_LEVEL_TIMEOUT   |    \
                                  BTPM_DEBUG_LEVEL_WARNING   |    \
                                  BTPM_DEBUG_LEVEL_FAILURE   |    \
                                  BTPM_DEBUG_LEVEL_CRITICAL  |    \
                                  0)


#include "SS1UTILI.h"

#endif /* __SS1CFGH__ */
