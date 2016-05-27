/*
* Copyright (c) 2011 Rx Networks, Inc. All rights reserved.
*
* Property of Rx Networks
* Proprietary and Confidential
* Do NOT distribute
* 
* Any use, distribution, or copying of this document requires a 
* license agreement with Rx Networks. 
* Any product development based on the contents of this document 
* requires a license agreement with Rx Networks. 
* If you have received this document in error, please notify the 
* sender immediately by telephone and email, delete the original 
* document from your electronic files, and destroy any printed 
* versions.
*
* This file contains sample code only and the code carries no 
* warranty whatsoever.
* Sample code is used at your own risk and may not be functional. 
* It is not intended for commercial use.   
*
* Example code to illustrate how to integrate Rx Networks PGPS 
* System into a client application.
*
* The emphasis is in trying to explain what goes on in the software,
* and how to the various API functions relate to each other, 
* rather than providing a fully optimized implementation.
*
*************************************************************************
* $LastChangedDate: 2009-04-22 17:33:16 -0700 (Wed, 22 Apr 2009) $
* $Revision: 10292 $
*************************************************************************
*
*/

#include <time.h>
#include <assert.h>
#include "RXN_MSL_Time.h"
#include "RXN_MSL_Platform.h"

/* Maximum allowable uncertainty for GPS clock for SNTP requests (in seconds)*/
static U32 mTimeUncertThresh = 5 * 1000;

/* Offset from internal clock - to be used when calculating GPS time.
* Supports targets having an invalid clock. */
static S32 mSystime_ClockOffset = MSL_INVALID_CLK_OFFSET;

/* Accurate GPS time from outside. To set up this time it must either from
a decoded GPS signal or a secured RTC based on decoded GPS signal.
When set up time value, the gMSL_ClockOffset will always cleared to 0.
We'll discard this clock if the Uncertainty higher than the threshold*/
static U32 mAccGPSTime = MSL_INVALID_GPS_TIME;
static U32 mAccGPSTime_Uncertainty = MSL_INVALID_GPS_TIME_UNCERTAINTY;
static U32 mAccGPSTime_Start_Point = MSL_INVALID_GPS_TIME;

/* Accurate GPS time from outside. To set up this time it must either from
a decoded GPS signal or a secured RTC based on decoded GPS signal.
When set up time value, the gMSL_ClockOffset will always cleared to 0.*/
static U32 mSNTPTime = MSL_INVALID_GPS_TIME;
static U32 mSNTPTime_Start_Point = MSL_INVALID_GPS_TIME;

static U32 mGPSLeapSec = MSL_DEFAULT_GPS_LEAP_SECOND_OFFSET;

static enum MSL_Current_Time_Source mCurrentTimeSource = MSL_CURRENT_TIME_SOURCE_NO;

/* internal prototypes */
static U32 GetSNTPTimeUncert(void);
static U32 GetAccGPSTimeUncert(void);
static U32 LeapSecRolloverCheck(U32 curGPSTime);

U16 MSL_SetGPSTime(RXN_RefTime_t* pRefTime, U16 gps_wn)
{
    U32 oldAccGPSTimeUncert;

    MSL_EnterBlock(RXN_MSL_CS_Time);
    oldAccGPSTimeUncert = mAccGPSTime_Uncertainty;
    if(pRefTime->TAccmSec > mTimeUncertThresh)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_WARNING, RXN_LOG_ZONE08,
            "RXN_MSL_SetGPSTime: Uncertainty of time is not acceptable.");
        MSL_LeaveBlock(RXN_MSL_CS_Time);
        return RXN_SUCCESS;
    }
    else
    {
        mAccGPSTime = gps_wn * 604800 + pRefTime->TOWmSec / 1000;
        mAccGPSTime_Uncertainty = pRefTime->TAccmSec;
        mAccGPSTime_Start_Point = MSL_GetTickSecond(); /* TickCount */

        MSL_LogGPSTime("RXN_MSL_SetGPSTime: GPS time set to:", mAccGPSTime);
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "RXN_MSL_SetGPSTime: Time Uncertainty: %1.0f", GetAccGPSTimeUncert());
    }
    MSL_LeaveBlock(RXN_MSL_CS_Time);

    /* Reset data fetch counters */
    MSL_SM_ResetCounters();

    if(oldAccGPSTimeUncert > mTimeUncertThresh)
    {
        /* Only if the send system event if previous time was not useful
        * as this will trigger based logic to operate correctly, otherwise
        * if previous time was usuable, the new time doesn't effect the operating
        * logic too much and prevents continuous exits of system idle timeout.
        */
        MSL_SetNextSystemEvent(MSL_EVENT_TIME_UPDATED);
    }
    return RXN_SUCCESS;
}

U32 MSL_GetBestGPSTime(enum MSL_Current_Time_Source* pTimeSource)
{
    extern MSL_Config_t gConfig;
    extern U32 MSL_GetSystemTime(void);

    U32 bestTime = 0;

    MSL_EnterBlock(RXN_MSL_CS_Time);
    /* Utilize the internal function (target - specific). */
    if ((mAccGPSTime != MSL_INVALID_GPS_TIME) &&
        (GetAccGPSTimeUncert() < mTimeUncertThresh))
    {
        /* Good to have an accurate GPS time, just return it with the
        * propagation justification. */
        mCurrentTimeSource = MSL_CURRENT_TIME_SOURCE_GPS;
        bestTime = mAccGPSTime + MSL_GetTickSecond() - mAccGPSTime_Start_Point;
    }
    else if ((mSNTPTime != MSL_INVALID_GPS_TIME) &&
        (GetSNTPTimeUncert() < mTimeUncertThresh))
    {
        /* Don't have GPS time. Let's try SNTP time. If we have SNTP time
        * return it with the propagation justification.  */
        mCurrentTimeSource = MSL_CURRENT_TIME_SOURCE_SNTP;
        bestTime = MSL_ConvertUTCToGPSTime((time_t)(mSNTPTime + MSL_GetTickSecond() - mSNTPTime_Start_Point));
    }
    /* we may not get any time update from GPS or SNTP 
    * fall back to most recent valid time */
    else if ((mAccGPSTime != MSL_INVALID_GPS_TIME) &&
        (mAccGPSTime_Start_Point > mSNTPTime_Start_Point) )
    {
        /* Good to have an accuarte GPS time, just return it with the
        * propagation justification. */
        mCurrentTimeSource = MSL_CURRENT_TIME_SOURCE_GPS;
        bestTime = mAccGPSTime + MSL_GetTickSecond() - mAccGPSTime_Start_Point;
    }
    /* old GPS Time isn't good to use, then fall back to old SNTP time */
    else if (mSNTPTime != MSL_INVALID_GPS_TIME)
    {
        /* Don't have GPS time. Let's try SNTP time. If we have SNTP time
        * return it with the propagation justification.  */
        mCurrentTimeSource = MSL_CURRENT_TIME_SOURCE_SNTP;
        bestTime = MSL_ConvertUTCToGPSTime((time_t)(mSNTPTime + MSL_GetTickSecond() - mSNTPTime_Start_Point));
    }
    else
    {
        if((gConfig.flags & MSL_CONFIG_FLAG_USE_SYSTEM_TIME) == 1)
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
                    "MSL_GetBestGPSTime: Fallback to using system time.");
            /* we don't have accurate time, use system time instead
            Please note, the system time offset has been added to avoid
            outside change and condition code. */
            mCurrentTimeSource = MSL_CURRENT_TIME_SOURCE_SYSTEM;
            bestTime = MSL_GetSystemTime() + mSystime_ClockOffset;
        }
        else
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
                    "MSL_GetBestGPSTime: No valid timesource, not using system time.");
            mCurrentTimeSource = MSL_CURRENT_TIME_SOURCE_NO;
            MSL_LeaveBlock(RXN_MSL_CS_Time);
            return 0;
        }
    }

    if (pTimeSource)
    {
        *pTimeSource = mCurrentTimeSource;
    }

    /* Check if a leap second has occurred leap second information. If so
     * adjust the time reported and also the local variable mGPSLeapSec
     */
    bestTime = LeapSecRolloverCheck(bestTime);

    MSL_LeaveBlock(RXN_MSL_CS_Time);
    return bestTime;
}


U16 MSL_GetSNTPTime(RXN_RefTime_t* pRefTime)
{
    U16 ret = RXN_FAIL;

    MSL_EnterBlock(RXN_MSL_CS_Time);
    
    if (mSNTPTime != MSL_INVALID_GPS_TIME)
    {
        U32 SNTPGPSTime = MSL_ConvertUTCToGPSTime((time_t)(mSNTPTime + MSL_GetTickSecond() - mSNTPTime_Start_Point));
        pRefTime->weekNum = (SNTPGPSTime / 604800) % 1024; /* take the 1024 week roll over into account. */
        pRefTime->TOWmSec = (SNTPGPSTime % 604800) * 1000;
        pRefTime->TAccmSec = GetSNTPTimeUncert(); 
        pRefTime->TAccnSec = 0;
        pRefTime->TOWnSec = 0;
        ret = RXN_SUCCESS;
    }
    MSL_LeaveBlock(RXN_MSL_CS_Time);
    return ret;
}

void MSL_SetSNTPTime(U32 SNTPTime)
{
    MSL_EnterBlock(RXN_MSL_CS_Time);
    mSNTPTime = SNTPTime;
    mSNTPTime_Start_Point = MSL_GetTickSecond();
    MSL_LogGPSTime("MSL_SetSNTPTime: GPS time set to:", MSL_ConvertUTCToGPSTime((time_t)mSNTPTime)); 
    MSL_LeaveBlock(RXN_MSL_CS_Time);
}

BOOL MSL_IsGPSTimeAcceptable()
{
    BOOL ret = FALSE;
    MSL_EnterBlock(RXN_MSL_CS_Time);
    ret = (mAccGPSTime != MSL_INVALID_GPS_TIME) && (GetAccGPSTimeUncert() <= mTimeUncertThresh);
    MSL_LeaveBlock(RXN_MSL_CS_Time);
    return ret;
}

BOOL MSL_IsSNTPTimeAcceptable()
{
    BOOL ret = FALSE;
    MSL_EnterBlock(RXN_MSL_CS_Time);
    ret = (mSNTPTime != MSL_INVALID_GPS_TIME) && (GetSNTPTimeUncert() <= mTimeUncertThresh);
    MSL_LeaveBlock(RXN_MSL_CS_Time);
    return ret;
}

void MSL_Time_Reset()
{
    MSL_EnterBlock(RXN_MSL_CS_Time);
    mSystime_ClockOffset = MSL_INVALID_CLK_OFFSET;

    mAccGPSTime = MSL_INVALID_GPS_TIME;
    mAccGPSTime_Uncertainty = MSL_INVALID_GPS_TIME_UNCERTAINTY;
    mAccGPSTime_Start_Point = MSL_INVALID_GPS_TIME;
    mSNTPTime = MSL_INVALID_GPS_TIME;
    mSNTPTime_Start_Point = MSL_INVALID_GPS_TIME;

    mCurrentTimeSource = MSL_CURRENT_TIME_SOURCE_NO;
    MSL_LeaveBlock(RXN_MSL_CS_Time);
}

static U32 LeapSecRolloverCheck(U32 curGPSTime)
{
    RXN_config_t config;  

    RXN_Get_Configuration(&config);

    if(config.leap_secs != 0)
    {
        mGPSLeapSec = config.leap_secs;
    }

    /* GPS leap second rollover condition */
    if((config.gps_time_next_leap != 0)&&(config.gps_time_next_leap < curGPSTime))
    {
        /* Currently this function does not match the capabilities of RXN_MSL_SetLeapSecondsInfo()
         * due to inherent behaviour of the Core 6.x to only support positive leap seconds */
        mGPSLeapSec = config.leap_secs + 1;
        curGPSTime = curGPSTime + 1;
    }
    return curGPSTime;
}

U32 MSL_GetGPSLeapSec()
{
    return mGPSLeapSec;
}

static U32 GetSNTPTimeUncert(void)
{
    U32 uncert = (MSL_GetTickSecond() - mSNTPTime_Start_Point )*1000 / (8*SYSTEM_HOUR); /* 3 seconds per day */
    if(uncert == 0)
    {
        uncert = 1;
    }
    return uncert;
}

static U32 GetAccGPSTimeUncert(void)
{
    return mAccGPSTime_Uncertainty + ((MSL_GetTickSecond() - mAccGPSTime_Start_Point)*1000 / (8*SYSTEM_HOUR)); /* 3 seconds per day */
}

void MSL_SetTimeUncertThresh(U32 thresh)
{
    mTimeUncertThresh = thresh * 1000;
}

U32 MSL_GetTimeUncertThresh()
{
    return mTimeUncertThresh;
}

BOOL MSL_UseBoundedGLOSec()
{
    extern MSL_Config_t gConfig;

    return (gConfig.constel[RXN_GLO_CONSTEL].flags & MSL_CONFIG_FLAG_GLO_USE_BOUNDED_TIME) == MSL_CONFIG_FLAG_GLO_USE_BOUNDED_TIME;
}

U32 MSL_ConvertUTCToGPSTime(time_t tTime)
{
    return (U32)tTime - SYSTEM_GPS_OFFSET + MSL_GetGPSLeapSec();
}

U32 MSL_ConvertUTCToGLOTime(time_t tTime)
{
    return (U32)tTime - SYSTEM_GPS_OFFSET - GPS_GLO_OFFSET;
}

U32 MSL_ConvertGPSTimeToGLOTime(U32 currentGPSTime)
{
    return currentGPSTime - GPS_GLO_OFFSET - MSL_GetGPSLeapSec();  
}

U32 MSL_ConvertGLOTimeToGPSTime(U32 gloSec)
{
    return gloSec + GPS_GLO_OFFSET + MSL_GetGPSLeapSec();
}

U32 MSL_ConvertGLOSecToBoundedGLOSec(U32 gloSec)
{
    /* Since currently GLONASS BCE has only tb values of :15 and :45 for maximum compatibility with
       chipset the MSL will round to the same intervals */

    U32 gloHourRemainder;

    gloHourRemainder = gloSec % 3600;

    /* If current time is between :00 and :30 */
    if(gloHourRemainder <  1800)
    {
        /* Return :15 value */
        return (gloSec - gloHourRemainder + 900);
    }
    else /* Current time is between :30 and :60 */
    {
        /* Return :45 value */
        return (gloSec - gloHourRemainder + 2700);
    }

}

void MSL_ConvertGLOSecToComponents(U32 gloSec, U08* N4, U16* NT, U08* tb)
{
    U32 days = 0;
    assert(N4 != NULL && NT != NULL && tb != NULL);

    /* Break gloSec up into component parts.  gloSec=[(N4-1)*1461 + (NT-1)]*86400 + tb*900 */
    *tb = gloSec % 86400 / 900;
    days = gloSec / 86400;
    *NT = days % 1461 + 1;
    *N4 = days / 1461 + 1;

    /* Check bounds of N4. Note: NT and tb are bounded by the equation so don't need
       to be checked. */
    if (*N4 > 31)
    {
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
            "MSL_ConvertGLOSecToComponents: N4 out of bounds: %1.0f", (float)*N4);
    }
}

U32 MSL_ConvertGLOComponentsToGLOSec(U08 N4, U16 NT, U08 tb)
{    
    /* gloSec=[(N4-1)*1461 + (NT-1)]*86400 + tb*900 */
    return ((N4-1)*1461 + (NT-1)) * 86400 + tb * 900;
}

U08 RXN_GetTauGPS(R32 *tauGPS)
{
    const R32 TWO_POW_30 = 1073741824;
    const R32 TWO_POW_49 = 562949953421312;

    U32 curGPSTime = 0;
    RXN_config_t config;
    enum MSL_Current_Time_Source curTimeSource = MSL_CURRENT_TIME_SOURCE_NO;

    RXN_Get_Configuration(&config);

    /* Get the current GPS time from the system. */
    curGPSTime = MSL_GetBestGPSTime(&curTimeSource);
    
    if (( curTimeSource != MSL_CURRENT_TIME_SOURCE_GPS ) &&
        ( curTimeSource != MSL_CURRENT_TIME_SOURCE_SNTP ))
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
            "RXN_GetTauGPS: No accurate GPS time source.");
        return RXN_FAIL;
    }
    else
    {
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
            "RXN_GetTauGPS: GPS Time: %1.0f.", curGPSTime);
    }

    /* Display info */
    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
        "RXN_GetTauGPS: config.tt_tau_gps: %1.0f.", config.tt_tau_gps);
    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
        "RXN_GetTauGPS: config.tau_gps: %1.0f.", config.tau_gps);
    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
        "RXN_GetTauGPS: config.tau_gps_dot: %1.0f.", config.tau_gps_dot);

    if(config.tt_tau_gps != 0)
    {
        *tauGPS = (config.tau_gps_dot/TWO_POW_49)*(curGPSTime - config.tt_tau_gps)+(config.tau_gps/TWO_POW_30);
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
            "RXN_GetTauGPS: predicted tauGPS: %e.", *tauGPS);
        return RXN_SUCCESS;
    }
    else
    {
        *tauGPS = 0;
        return RXN_FAIL;
    }
}