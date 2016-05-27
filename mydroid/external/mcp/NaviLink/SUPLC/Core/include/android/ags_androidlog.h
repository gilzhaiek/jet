/*
******************************************************************
* COPYRIGHT © Teleca AB                                          *
*----------------------------------------------------------------*
* MODULE     : androidlog.h                                      *
*                                                                *
* PROGRAMMER : Dmitriy Kardakov <Dmitriy.Kardakov@teleca.com>    *
* DATE       : 28 Feb 2009                                       *
* VERSION    : 1.0                                               *
*                                                                *
*----------------------------------------------------------------*
* MODULE SUMMARY : Defines macros for writing to the Android     *
* logcat stream.                                                 *
*----------------------------------------------------------------*
*                                                                *
* MODIFICATION RECORDS                                           *
* Dmitriy Kardakov - Initial version                             *
******************************************************************
*/
#ifndef __AGS_ANDROIDLOG_H__
#define __AGS_ANDROIDLOG_H__

#define ANDROID_LOG

#define LOG_TAG CURR_TAG
#include "utils/Log.h"
#define MAX_LOG_TAG_SIZE (64)
static char CURR_TAG[MAX_LOG_TAG_SIZE];

#ifdef ANDROID_LOG
/* KW Changes by Naresh */
#define errorMessage(fmt, args...) \
        do {\
            snprintf(CURR_TAG,MAX_LOG_TAG_SIZE, "%s:%d %s:", __FILE__, __LINE__, "ERROR");\
            LOGD(fmt, ##args);\
        } while(0)
        
/* KW Changes by Naresh */
#define debugMessage(fmt, args...) \
        do {\
            snprintf(CURR_TAG,MAX_LOG_TAG_SIZE, "%s:%d %s:", __FILE__, __LINE__, "DEBUG");\
            LOGD(fmt, ##args);\
        } while(0)
#else
#define errorMessage(fmt, args...)
#define debugMessage(fmt, args...)
#endif 

#endif /* __AGS_ANDROIDLOG_H__ */
