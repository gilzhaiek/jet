/*****< SS1UTILI.h >***********************************************************/
/*      Copyright 2000 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  SS1UTILI - Definitions, Constants, and Macros, for use with the           */
/*             utility routines used by the Stonestreet One Bluetopia PM stack*/
/*             for Android.                                                   */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   07/18/10  G. Hensley     Initial creation.                               */
/******************************************************************************/

#ifndef __SS1UTILIH__
#define __SS1UTILIH__

#include "SS1CFG.h"

   /* Prefix for use with definitions of functions which should be      */
   /* exported when built into a shared libraries.                      */
#define SS1API __attribute__(( visibility("default") ))

   /* Definitions for logging macros for use in the Bluetooth JNI Bridge*/
   /* modules. These definitions are intented to allow the logging      */
   /* facilities to be disabled for release builds.                     */
#if ENABLE_SS1_LOGGING == 0

#define SS1_LOGV(_fmt, ...) ((void)0)
#define SS1_LOGD(_fmt, ...) ((void)0)
#define SS1_LOGI(_fmt, ...) ((void)0)
#define SS1_LOGW(_fmt, ...) ((void)0)
#define SS1_LOGE(_fmt, ...) ((void)0)

#else /* ENABLE_SS1_LOGGING */

#if SS1_PLATFORM_SDK_VERSION >= 16

#if LOG_FUNCTION_NAMES == 0
#define _SS1_LOG(_lvl, _fmt, ...) ALOG##_lvl(_fmt , ##__VA_ARGS__)
#else
#define _SS1_LOG(_lvl, _fmt, ...) ALOG##_lvl("%s: " _fmt, __FUNCTION__ , ##__VA_ARGS__)
#endif

#else

#if LOG_FUNCTION_NAMES == 0
#define _SS1_LOG(_lvl, _fmt, ...) LOG##_lvl(_fmt , ##__VA_ARGS__)
#else
#define _SS1_LOG(_lvl, _fmt, ...) LOG##_lvl("%s: " _fmt, __FUNCTION__ , ##__VA_ARGS__)
#endif

#endif

#define SS1_LOGV(_fmt, ...) _SS1_LOG(V, _fmt , ##__VA_ARGS__)
#define SS1_LOGD(_fmt, ...) _SS1_LOG(D, _fmt , ##__VA_ARGS__)
#define SS1_LOGI(_fmt, ...) _SS1_LOG(I, _fmt , ##__VA_ARGS__)
#define SS1_LOGW(_fmt, ...) _SS1_LOG(W, _fmt , ##__VA_ARGS__)
#define SS1_LOGE(_fmt, ...) _SS1_LOG(E, _fmt , ##__VA_ARGS__)

#endif /* ENABLE_SS1_LOGGING */

   /* The following macro confirms that a mutex is currently available  */
   /* (unlocked). It is intended to be used at the end of a routine to  */
   /* confirm that the mutex was properly released. The parameter M     */
   /* should be given as an address of a pthread_mutex_t variable.      */
#if 0
#define CHECK_MUTEX(M) do \
{ \
   if(pthread_mutex_trylock(M) == 0) \
      pthread_mutex_unlock(M); \
   else \
      SS1_LOGE("%s: STILL HOLDING MUTEX ON RETURN (" #M ")", __FUNCTION__); \
} while(0)
#else
#define CHECK_MUTEX(M) ((void)0)
#endif


   /* Disable unsupported profiles.                                     */
#if (!SS1_MODULE_ENABLED_AUDM)
   #if (SS1_SUPPORT_A2DP == TRUE)
      #warning "A2DP not supported by the distribution - Disabling"
   #endif
   #undef  SS1_SUPPORT_A2DP
   #define SS1_SUPPORT_A2DP     FALSE
#endif
#if (!SS1_MODULE_ENABLED_HIDM)
   #if (SS1_SUPPORT_HID == TRUE)
      #warning "HID not supported by the distribution - Disabling"
   #endif
   #undef  SS1_SUPPORT_HID
   #define SS1_SUPPORT_HID      FALSE
#endif
#if (!SS1_MODULE_ENABLED_PANM)
   #if (SS1_SUPPORT_PAN == TRUE)
      #warning "PAN not supported by the distribution - Disabling"
   #endif
   #undef  SS1_SUPPORT_PAN
   #define SS1_SUPPORT_PAN      FALSE
#endif
#if ((!SS1_MODULE_ENABLED_HDSM) || (!SS1_MODULE_ENABLED_HFRM))
   #if (SS1_SUPPORT_HFRE_HSP == TRUE)
      #warning "HFP/HSP not supported by the distribution - Disabling"
   #endif
   #undef  SS1_SUPPORT_HFRE_HSP
   #define SS1_SUPPORT_HFRE_HSP FALSE
#endif
#if (!SS1_MODULE_ENABLED_HDPM)
   #if (SS1_SUPPORT_HEALTH == TRUE)
      #warning "HDP not supported by the distribution - Disabling"
   #endif
   #undef  SS1_SUPPORT_HEALTH
   #define SS1_SUPPORT_HEALTH   FALSE
#endif

#endif
