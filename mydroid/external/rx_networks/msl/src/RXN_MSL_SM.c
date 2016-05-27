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
* $LastChangedDate: 2009-04-17 13:53:14 -0700 (Fri, 17 Apr 2009) $
* $Revision: 10192 $
*************************************************************************
*
* This file contains implementation of PGPS functionality.
*
*/

#include <stdlib.h>
#include "RXN_MSL_Common.h"  /* Internal common declarations. */
#include "RXN_MSL_Time.h"

static enum MSL_system_states mCurrentSystemState = MSL_STATE_INITIALIZE;
static enum MSL_system_states mNextSystemState = MSL_STATE_INITIALIZE;
static enum MSL_system_states mPendingState = MSL_STATE_INITIALIZE;
static enum MSL_system_events mNextSystemEvent = MSL_EVENT_IDLE;

static U08 mCurConstelIndex = 0;

/* Retry counters */
static U08 mSeedDownloadCount = 0;
static U08 mSNTPRequestCount = 0;
static U08 mClockUpdateCount = 0;

static S08 mNeedSAGPSPropCount = MSL_MAX_CONSTEL;

extern MSL_Config_t gConfig;

extern U16 CmdRequestSNTPTime();
extern U16 CmdRequestTermination();
extern U08 RXN_MSL_GetDataAccess();
extern void MSL_DisconnectRXNServices();

/* prototypes */
static BOOL HandleEvent(enum MSL_system_events event);
static U16 SeedCheckAction(void);
static U16 DownloadSeed();
static U16 TimeCheckAction(void);
static void SNTPRetryReset();
static void SeedRetryReset();

typedef U16 (*ActionFunctionPointer)(void);

static ActionFunctionPointer mActionTable[MSL_STATE_MAX] =
{
    MSL_SystemInitializeAction,		/* While in MSL_STATE_INITIALIZE */
    MSL_SystemIdleAction,		/* While in MSL_STATE_IDLE */
    TimeCheckAction,			/* While in MSL_STATE_TIME_CHECK */
    MSL_SNTPRequestAction,		/* While in MSL_STATE_SNTP_REQUEST_PENDING */
    SeedCheckAction,			/* While in MSL_STATE_SEED_CHECK */
    SeedCheckAction,			/* While in MSL_STATE_SEED_DOWNLOAD_PENDING */
    MSL_ClockUpdateDownloadAction,	/* While in MSL_STATE_CLOCK_UPDATE_DOWNLOAD_PENDING */
    MSL_SeedPropagationAction,		/* While in MSL_STATE_SEED_PROPAGATING */
};

void MSL_SM_Initialize()
{
    MSL_CreateTimer();

    /* Initialize module variables */
    mCurrentSystemState = MSL_STATE_INITIALIZE;
    mNextSystemState = MSL_STATE_INITIALIZE;
    mPendingState = MSL_STATE_INITIALIZE;
    mNextSystemEvent = MSL_EVENT_IDLE;
    mCurConstelIndex = 0;
    mSeedDownloadCount = 0;
    mSNTPRequestCount = 0;
    mClockUpdateCount = 0;
    mNeedSAGPSPropCount = gConfig.numConstels;
}

void MSL_SM_Uninitialize()
{
    MSL_CloseTimer();
}

void MSL_RunStateMachine()
{
    BOOL useTimer = FALSE;

    /* Guarantees the process is running */
#ifdef ANDROID
    MSL_AcquireMainWakeLock();
#endif

    MSL_EnterBlock(RXN_MSL_CS_SM);

    /* A pending state may have been set by an event that was triggered while
    the state machine was running */
    if (mPendingState != MSL_STATE_INITIALIZE)
    {
        mNextSystemState = mPendingState;
        mPendingState = MSL_STATE_INITIALIZE;
    }

    mCurrentSystemState = mNextSystemState;
    mNextSystemEvent = MSL_EVENT_IDLE;

    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "MSL_RunStateMachine: state %1.0f",mCurrentSystemState);

    if(mCurrentSystemState < MSL_STATE_MAX)
    {
        MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
            "Received event %1.0f in state %1.0f", (float)MSL_EVENT_IDLE, (float)mCurrentSystemState);

        /* Call the appropriate idle action */
        useTimer = mActionTable[mCurrentSystemState]();
        MSL_LeaveBlock(RXN_MSL_CS_SM);
        if (useTimer)
        {
            MSL_TimeoutAction();
        }
    }
    else
    {
        /* invalid event/state - handle appropriately */
        MSL_LeaveBlock(RXN_MSL_CS_SM);
    }

#ifdef ANDROID
    MSL_ReleaseMainWakeLock();
#endif
}

static void SetNextSystemState(enum MSL_system_states state)
{
    /* If the state machine isn't running, just set the state.  Otherwise, set 
    the pending state and it will be handled the next time MSL_Work() is called. */
    if (MSL_TryEnterBlock(RXN_MSL_CS_SM))
    {
        mNextSystemState = state;
        mPendingState = MSL_STATE_INITIALIZE;
        MSL_LeaveBlock(RXN_MSL_CS_SM);
    } 
    else
    {
        mPendingState = state;
    }
}

void MSL_GetSystemState(enum MSL_system_states * pState)
{
    *pState = mCurrentSystemState;
}

void MSL_GetNextSystemState(enum MSL_system_states * pState)
{
    if (mPendingState == MSL_STATE_INITIALIZE)
    {
        *pState = mNextSystemState;
    }
    else
    {
        *pState = mPendingState;
    }
}

void MSL_GetNextSystemEvent(enum MSL_system_events * pEvent)
{
    *pEvent = mNextSystemEvent;
}

U16 MSL_SystemInitializeAction() 
{
    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "MSL_SystemInitializeAction: System base GPS week: %1.0f", gConfig.baseGPSWeekNum);

    SetNextSystemState(MSL_STATE_TIME_CHECK);
    return TRUE;
}

U16 MSL_Timeout(U32 timeoutSeconds)
{
    S32 result = 0;
    U32 wakeTickSecond = 0;

    /* wakeTickSecond is the tick second if MSL_Timeout is not interrupted */
    wakeTickSecond = MSL_GetTickSecond() + timeoutSeconds;

    MSL_WaitTimer(timeoutSeconds);

    result = wakeTickSecond - MSL_GetTickSecond();

    /* return the number of seconds left in the timeout period, ensuring 
    that 0 is returned if we've waited too long. */
    return (result < 0) ? 0 : result;
}

U16 MSL_WakeUpWork()
{
    return MSL_ExitTimer();
}

U16 MSL_SystemIdleAction()
{
#ifdef ANDROID
    /* If the system does not allow for PGPS seeds to be downloaded
       disable RXN Services */
    if((mSeedDownloadCount >= gConfig.net.downloadRetryMax)||(mSNTPRequestCount >= gConfig.SNTP.requestRetryMax))
    {
        /* Shut down RXN Services */
        CmdRequestTermination();
    }
#endif
    SetNextSystemState(MSL_STATE_TIME_CHECK);
    return TRUE;
}

static U16 TimeCheckAction(void)
{
    U16 result;
    enum MSL_system_states state;

    /* Verify time sources */
    if(MSL_IsGPSTimeAcceptable())
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
            "TimeCheckAction: Time set by SetGPSTime is acceptable no need to perform a SNTP request.");
        SetNextSystemState(MSL_STATE_SEED_CHECK);
        return FALSE;
    }
    else if(MSL_IsSNTPTimeAcceptable())
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
            "TimeCheckAction: Current SNTP time is acceptable no need to perform a SNTP request.");
        SetNextSystemState(MSL_STATE_SEED_CHECK);
        return FALSE;
    }

    mCurrentSystemState = MSL_STATE_SNTP_REQUEST_PENDING;

    result = MSL_SNTPRequestAction();

    if((gConfig.flags & MSL_CONFIG_FLAG_USE_SYSTEM_TIME) == 1)
    {
        MSL_GetNextSystemState(&state);

        if(state == MSL_STATE_IDLE)
        {
            /* Even though we were unable to obtain SNTP time we will fallback to system time */
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
                "TimeCheckAction: Using device system time as fallback time source.");
            SetNextSystemState(MSL_STATE_SEED_CHECK);
            return FALSE;
        }
    }
        return result;
}

U16 MSL_SNTPRequestAction()
{
    static U32 retryTickSecond = 0;

    if(RXN_MSL_GetDataAccess() != MSL_DATA_ACCESS_ENABLED)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
            "MSL_SNTPRequestAction: Network access not available or allowed.");
        /* Since we have no usable time we go to idle */
        SetNextSystemState(MSL_STATE_IDLE);
        return FALSE;
    }
    if(mSNTPRequestCount < gConfig.SNTP.requestRetryMax)
    {
        if(CmdRequestSNTPTime() == RXN_SUCCESS)
        {
            mSNTPRequestCount++;
            MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
                "MSL_SNTPRequestAction: SNTP request made. Attempt: %1.0f", mSNTPRequestCount);
        }
        return TRUE;
    }
    else if(mSNTPRequestCount == gConfig.SNTP.requestRetryMax)
    {
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
            "MSL_SNTPRequestAction: Maximum SNTP attempts made. No further attempts will be made. Must wait for %1.0f seconds.", gConfig.SNTP.requestRetryTimer);
        retryTickSecond = MSL_GetTickSecond() + gConfig.SNTP.requestRetryTimer;

        /* Increment count once more so we only set the retryTickSecond once */
        mSNTPRequestCount++;
        SetNextSystemState(MSL_STATE_IDLE);
    }
    else /* mSNTPRequestCount > gConfig.SNTP.requestRetryMax */
    {
        MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08, "MSL_SNTPRequestAction: Current time: %1.0f, Retry allowed time: %1.0f.", MSL_GetTickSecond(), retryTickSecond);
        if(MSL_GetTickSecond() < retryTickSecond)
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
                "MSL_SNTPRequestAction: Must wait till maximum try timeout has elapsed or a reset condition.");
            SetNextSystemState(MSL_STATE_IDLE);
        }
        else
        {
            SNTPRetryReset();
        }
    }
    return FALSE;
}

static U16 SeedCheckAction(void)
{
    enum MSL_seed_states seedState = MSL_SEED_OKAY;

    /* Note:  mCurConstelIndex will always reflect the index of the constellation
    * currently being processed.  It is incremented by HandleEvent() if the
    * propagation for a constellation completes. */

    while (mCurConstelIndex < gConfig.numConstels)
    {
        /* If a SAGPS propagation is needed and either a seed download has failed or there
         * is no network access, then do a round of propagation before trying to download again.
         * Otherwise, check the seed status. */
        if (mNeedSAGPSPropCount > 0
            && (mSeedDownloadCount > 0 || RXN_MSL_GetDataAccess() != MSL_DATA_ACCESS_ENABLED))
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
                "SeedCheckAction: Need to propagate SAGPS seed.");
            seedState = MSL_SEED_PROPAGATE;
        }
        else
        {
            MSL_CheckSeedStatus(&seedState, gConfig.constelList[mCurConstelIndex]);
        }

        MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "SeedCheckAction: seedState: %1.0f constel: %1.0f.", 
            seedState, gConfig.constelList[mCurConstelIndex]);

        switch(seedState)
        {
        case MSL_SEED_DOWNLOAD:
            {
                /* Download seed immediately and exit loop */
                return DownloadSeed();
            }
        case MSL_SEED_DOWNLOAD_AND_PROPAGATE_REMAINING:
            {
                DownloadSeed();
                SetNextSystemState(MSL_STATE_SEED_PROPAGATING);
                return FALSE;
            }
        case MSL_SEED_PROPAGATE:
            {
                SetNextSystemState(MSL_STATE_SEED_PROPAGATING);
                return FALSE;
            }

        case MSL_SEED_CLOCK_UPDATE:
            {
                SetNextSystemState(MSL_STATE_CLOCK_UPDATE_DOWNLOAD_PENDING);
                return FALSE;
            }
        case MSL_SEED_MSL_GPS_TIME_SOURCE_INVALID:
            {
                /* Mark the GPS Time invalid */
                MSL_Time_Reset();
                SetNextSystemState(MSL_STATE_SNTP_REQUEST_PENDING);
                return FALSE;
            }
        case MSL_SEED_OKAY:
        default:
            {
                break;
            }
        }

        /* This seed is ok, so check the next one */
        ++mCurConstelIndex;
    } 

    /* All constellations processed, so reset index and set state to idle */
    mCurConstelIndex = 0;
    SetNextSystemState(MSL_STATE_IDLE);

#if defined(ANDROID)
    /* Shut down RXN Services */
    CmdRequestTermination();
#endif
    return FALSE;
}

static U16 DownloadSeed()
{
    static U32 retryTickSecond = 0;

    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
        "DownloadSeed: Initiate seed download.");

    mCurrentSystemState = MSL_STATE_SEED_DOWNLOAD_PENDING;

    /* If the gMSL_SeedDownloadRetryTimer has elapsed since the last download we reset the download counter again */
    if(retryTickSecond < MSL_GetTickSecond())
    {
        SeedRetryReset();
    }

    /* Before we can reach this state MSL_SNTPRequestAction() must have been called
    * and verified time sources are okay */
    if(RXN_MSL_GetDataAccess() != MSL_DATA_ACCESS_ENABLED)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
            "DownloadSeed: Network access not available or allowed.");
        SetNextSystemState(MSL_STATE_IDLE);
        return FALSE;
    }

    /* If we've already tried to download but there is still no seed, try another port */
    if (mSeedDownloadCount != 0)
    {
        U16 oldPort = MSL_GetHostPort();
        if (MSL_SetNextHostPort() == RXN_SUCCESS) 
        {
            MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
                "DownloadSeed: Could not connect to port %1.0f. Next port is %1.0f.", oldPort, MSL_GetHostPort());
        }
    }

    if(mSeedDownloadCount < gConfig.net.downloadRetryMax)
    {
        if(MSL_UpdateSeedOrClk(MSL_REQ_TYPE_SEED) == RXN_SUCCESS)
        {
            /* Capture initial download time */
            if(mSeedDownloadCount == 0)
            {
                retryTickSecond = MSL_GetTickSecond() + gConfig.net.downloadRetryTimer;
            }
            mSeedDownloadCount++;
            MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
                "DownloadSeed: Seed download request made. Attempt: %1.0f.", mSeedDownloadCount);
        }
        return TRUE;
    }
    else if(mSeedDownloadCount == gConfig.net.downloadRetryMax)
    {
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
            "DownloadSeed: Maximum seed download attempts made. No further attempts will be made.Must wait for %1.0f seconds.", gConfig.net.downloadRetryTimer);
        retryTickSecond = MSL_GetTickSecond() + gConfig.net.downloadRetryTimer;

        /* Increment count once more so we only set the retryTickSecond once */
        mSeedDownloadCount++;
        /* Proceed to progatae SAGPS seed */
        SetNextSystemState(MSL_STATE_SEED_PROPAGATING);
    }
    else /* mSeedDownloadCount > gConfig.net.downloadRetryMax */
    {
        MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08, "Current time: %1.0f, Retry allowed time: %1.0f.", MSL_GetTickSecond(), retryTickSecond);

        if(MSL_GetTickSecond() < retryTickSecond)
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
                "DownloadSeed: Must wait till maximum try timeout has elapsed or a reset condition.");
            /* Proceed to progatae SAGPS seed */
            SetNextSystemState(MSL_STATE_SEED_PROPAGATING);
        }
        else
        {
            SeedRetryReset();
        }
    }
    return FALSE;
}

U16 MSL_ClockUpdateDownloadAction()
{
    if(RXN_MSL_GetDataAccess() != MSL_DATA_ACCESS_ENABLED)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
            "MSL_ClockUpdateDownloadAction: Network access not available or allowed.");
        SetNextSystemState(MSL_STATE_IDLE);
        return FALSE;
    }
    if(mClockUpdateCount < 1)
    {
        if(MSL_UpdateSeedOrClk(MSL_REQ_TYPE_CLOCK) == RXN_SUCCESS)
        {
            mClockUpdateCount++;
        }
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
            "MSL_ClockUpdateDownloadAction: Clock update request made.");
    }
    else
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
            "MSL_ClockUpdateDownloadAction: Clock update needed but already made an attempt, no further attempts allowed.");
    }

    SetNextSystemState(MSL_STATE_IDLE);
    return TRUE;
}

U16 MSL_CheckSeedStatus(enum MSL_seed_states * pSeedState, RXN_constel_t constel)
{
    U16 result = 0;
    U32 curGPSTime = 0;
    U08 PRN = 0;
    RXN_seed_information_t curPRNseedInfo;
    RXN_debug_information_t curPRNdebugInfo;
    U08 needSeedPRNCnt = 0;
    U08 needPropPRNCnt = 0;
    U08 needTimeCnt = 0;
    U08 needClkPRNCnt = 0;
    U08 noPantryPRNCnt = 0;
    U32 mslPropFwd = 0;

    /* gConfig.seed.updateAge + random portion of time for load balancing*/
    U32 randomizedSeedUpdateAge = 0;
    /* gMSL_ClockUpdateAge + random portion of time for load balancing*/
    U32 randomizedClockUpdateAge = 0;

    /* Get the current GPS time. */
    curGPSTime = RXN_MSL_GetBestGPSTime();

    randomizedSeedUpdateAge = (U32)(gConfig.constel[constel].seed.updateAge + (((R32)(rand()%1000)/1000) * (R32)gConfig.constel[constel].seed.updateAgeOffset));
    randomizedClockUpdateAge = (U32)(gConfig.constel[constel].seed.clockUpdateAge + (((R32)(rand()%1000)/1000) * (R32)gConfig.constel[constel].seed.clockUpdateAgeOffset));

    /* Determine how far past the seed start time it is for each seed. */
    for(PRN =1; PRN<=numOfPRNs[constel]; PRN++)
    {
        /* Get seed info for the PRN. */
        result = RXN_Get_Seed_Information(PRN, &curPRNseedInfo, constel);

        /* If there is a problem getting info for the PRN, assume
        * that it is out of service and don't go further with the PRN. */
        if(result != RXN_SUCCESS)
        {
            /* Log trace info. */
            MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_WARNING, RXN_LOG_ZONE08,
                "MSL_CheckSeedStatus: No seed info for PRN: %1.0f. Constel: %1.0f", (double) PRN, (double)constel);
            /* Increment noPantryPRNCnt. */
            noPantryPRNCnt++;
            continue;
        }

        /* Trace logging for debug. */
        MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "MSL_CheckSeedStatus: Check for required downloads or propagation for PRN %1.0f. Constel: %1.0f ...",
            (double) PRN, (double)constel);

        if(curPRNseedInfo.number_of_ephemeris_used == 0)
        {
            mslPropFwd = gConfig.constel[constel].prop.PGPSFwd;
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
                "MSL_CheckSeedStatus: PGPS seed ...");
        }
        else if(curPRNseedInfo.number_of_ephemeris_used == 1)
        {
            mslPropFwd = gConfig.constel[constel].prop.SAGPSFwd;
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
                "MSL_CheckSeedStatus: SAGPS seed with 1 BCE record ...");  
        }
        else
        {
            mslPropFwd = gConfig.constel[constel].prop.SAGPSFwd;
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
                "MSL_CheckSeedStatus: SAGPS seed with 2 BCE records ...");  

            /* Get seed debug info for the PRN. */
            if(RXN_Debug_Information(PRN, constel, &curPRNdebugInfo) == RXN_SUCCESS)
            {
                if(curPRNdebugInfo.first_seed_data.seed_creation_time == curPRNseedInfo.seed_creation_time)
                {
                    MSL_LogGPSTime("MSL_CheckSeedStatus: Seed Buffer 1 - TOE of first BCE record: -->", curPRNdebugInfo.first_seed_last_toe);
                    MSL_LogGPSTime("MSL_CheckSeedStatus: Seed Buffer 1 - TOE of second BCE record: ->", curPRNdebugInfo.first_seed_first_toe);
                }
                else
                {
                    MSL_LogGPSTime("MSL_CheckSeedStatus: Seed Buffer 2 - TOE of first BCE record: -->", curPRNdebugInfo.second_seed_last_toe);
                    MSL_LogGPSTime("MSL_CheckSeedStatus: Seed Buffer 2 - TOE of second BCE record: ->", curPRNdebugInfo.second_seed_first_toe);
                }
            }
        }
        MSL_LogGPSTime("MSL_CheckSeedStatus: Current GPS time: ------>", curGPSTime);
        MSL_LogGPSTime("MSL_CheckSeedStatus: PRN seed create time: -->", curPRNseedInfo.seed_creation_time);
        MSL_LogGPSTime("MSL_CheckSeedStatus: PRN poly start time: --->", curPRNseedInfo.poly_start_time);
        MSL_LogGPSTime("MSL_CheckSeedStatus: PRN poly current time: ->", curPRNseedInfo.poly_curr_time);
        MSL_LogGPSTime("MSL_CheckSeedStatus: PRN poly expire time: -->", curPRNseedInfo.poly_expire_time);
        MSL_LogGPSTime("MSL_CheckSeedStatus: PRN clock drift time: -->", curPRNseedInfo.clock_ref_time);

        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "MSL_CheckSeedStatus: Prop seed to %1.0f seconds from seed creation time ...", (double) mslPropFwd);
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "MSL_CheckSeedStatus: Update seed %1.0f seconds from seed creation time ...", (double) gConfig.constel[constel].seed.updateAge);
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "MSL_CheckSeedStatus: Update seed including random offset %1.0f seconds from seed creation time ...", (double) randomizedSeedUpdateAge);
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "MSL_CheckSeedStatus: Update clock %1.0f seconds from seed clock time.", (double) gConfig.constel[constel].seed.clockUpdateAge);
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "MSL_CheckSeedStatus: Update clock including random offset %1.0f seconds from seed clock time.", (double) randomizedClockUpdateAge);

        /* PGPS -1557 */
        if(curGPSTime < curPRNseedInfo.seed_creation_time)
        {
            needTimeCnt++;
        }

        /* If the current GPS time is more than gConfig.seed.updateAge seconds past the
        * seed's creation time, update all seeds. */
        if(curPRNseedInfo.number_of_ephemeris_used == 0 && (curGPSTime - curPRNseedInfo.seed_creation_time) > randomizedSeedUpdateAge)	// PGPS seeds
        {
            /* Increment the needSeedPRNCnt. */
            needSeedPRNCnt++;
        }

        /* If the seed is SAGPS seed, try to download PGPS seed anyway */
        if(curPRNseedInfo.number_of_ephemeris_used != 0)
        {
            needSeedPRNCnt++;
        }
        /* Continually ensure the pantry has been propagated far enough in the future, but does not exceed the expiry of the seed */
        /* This exact logic can be found in the propagation state machine (MSL_PropagateAll)*/
        if(((curGPSTime + mslPropFwd) - curPRNseedInfo.poly_curr_time) >= 4*SYSTEM_HOUR)
        {
            if(curPRNseedInfo.poly_curr_time < curPRNseedInfo.poly_expire_time)
            {
                /* Increment the needPropPRNCnt. */
                needPropPRNCnt++;
            }
        }

        if(gConfig.constel[constel].seed.clockUpdateAge != 0)
        {
            /* If the current GPS time is more that gMSL_ClockUpdateAge hours past the
            * seed's clock time, update clock data for all seeds. 
            * Check for PGPS seeds only. SAGPS seeds should be ignored. */			 
            if((curPRNseedInfo.number_of_ephemeris_used == 0) &&
                (curGPSTime > curPRNseedInfo.clock_ref_time) &&
                ((curGPSTime - curPRNseedInfo.clock_ref_time) > randomizedClockUpdateAge))
            {
                /* Increment the needClkPRNCnt. */
                needClkPRNCnt++;
            }
        }

    } /* for(PRN =1; PRN<=RXN_CONSTANT_NUM_PRNS; PRN++) */

    /* First check for the situation where no pantry data exists and
    * force a seed update. */
    if(noPantryPRNCnt >= numOfPRNs[constel])
    {
        /* Log trace info. */
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
            "MSL_CheckSeedStatus: No pantry data found for any PRN. New seed needs to be downloaded.");
        *pSeedState = MSL_SEED_DOWNLOAD;
        return RXN_SUCCESS;
    }

    if(needTimeCnt >= (RXN_CONSTANT_NUM_PRNS - noPantryPRNCnt))
    {
        /* Log trace info. */
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
                "MSL_CheckSeedStatus: The current time does not appear to be correct based on a comparison with the creation time of the PGPS seed in the pantry.");

        /* The GPSTime cannot be correct as the create time of our seed is
         * in the future with respects to the GPSTime we are using */
        *pSeedState = MSL_SEED_MSL_GPS_TIME_SOURCE_INVALID;
        return RXN_SUCCESS;
    }

    /* If any PRN requires propagation - propagate. */
    if(needPropPRNCnt > 0)
    {
        /* Log trace info. */
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
            "MSL_CheckSeedStatus: Pantry needs to be propagated.");

        *pSeedState = MSL_SEED_PROPAGATE;
    }

    if(needSeedPRNCnt >= (numOfPRNs[constel] - noPantryPRNCnt))
    {
        /* Log trace info. */
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
            "MSL_CheckSeedStatus: Pantry data exceeds seed update age. New seeds need to be downloaded.");

        if(*pSeedState == MSL_SEED_PROPAGATE)
        {
            *pSeedState = MSL_SEED_DOWNLOAD_AND_PROPAGATE_REMAINING;
        }
        else
        {
            *pSeedState = MSL_SEED_DOWNLOAD;
        }
        return RXN_SUCCESS;
    }
    /* If we do not need to download a new seed, propagation has priority over clock update */
    if(*pSeedState == MSL_SEED_PROPAGATE)
    {
        return RXN_SUCCESS;
    }

    /* Last, look for a required clock update...
    * If any PRN needs an update, update all PRNs. An update will only occur
    * once after a seed update (while the MSL is running).*/
    if(needClkPRNCnt > 0)
    {
        /* Log trace info. */
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
            "MSL_CheckSeedStatus: Clock update needs to be downloaded.");

        *pSeedState = MSL_SEED_CLOCK_UPDATE;
        return RXN_SUCCESS;
    }
    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
        "MSL_CheckSeedStatus: Seed is okay, pantry is propagated. No actions required.");
    *pSeedState = MSL_SEED_OKAY;
    return RXN_SUCCESS;
}

U16 MSL_SeedPropagationAction()
{
    U16 result = 0;

    result = MSL_PropagateAll(gConfig.constelList[mCurConstelIndex]);
    
    if (result == MSL_PROPAGATION_COMPLETE
        || result == MSL_LICENSE_KEY_EXPIRED)
    {
        /* If there are more constellations that need to be processed, go to seed check,
           otherwise we're finished, so go to idle. */
        if (++mCurConstelIndex < gConfig.numConstels)
        {
            SetNextSystemState(MSL_STATE_SEED_CHECK);
        }
        else
        {
            mCurConstelIndex = 0;
            SetNextSystemState(MSL_STATE_IDLE);
            if (result == MSL_LICENSE_KEY_EXPIRED)
            {
#ifdef ANDROID
                /* Shut down RXN Services */
                CmdRequestTermination();
#endif
                return RXN_SUCCESS;
            }
        }

        if (mNeedSAGPSPropCount > 0) 
        {
            --mNeedSAGPSPropCount;
        }

        /* If there are other constellations needing SAGPS propagation or if there
         * are download retries remaining, go to seed check instead of idle. */
        if (mNeedSAGPSPropCount > 0 || 
                (mSeedDownloadCount > 0 && mSeedDownloadCount < gConfig.net.downloadRetryMax))
        {
            SetNextSystemState(MSL_STATE_SEED_CHECK);
        }
    }
    else
    {
        return RXN_FAIL;
    }

    return RXN_SUCCESS;
}

static U32 GetTimeout(enum MSL_system_states state)
{
    switch(state)
    {
    case MSL_STATE_INITIALIZE:
        {
            return gConfig.startupDataWaitDuration;
            break;
        }
    case MSL_STATE_IDLE:
        {
            return gConfig.systemCheckFreq;
            break;
        }
    case MSL_STATE_SNTP_REQUEST_PENDING:
        {
            return gConfig.SNTP.requestRetryPeriod;
            break;
        }
    case MSL_STATE_SEED_DOWNLOAD_PENDING:
    case MSL_STATE_CLOCK_UPDATE_DOWNLOAD_PENDING:
        {
            return gConfig.net.downloadRetryPeriod; 
            break;
        }
    case MSL_STATE_SEED_PROPAGATING:
    default:
        {
            return 0;
            break;
        }
    }
}

U16 MSL_TimeoutAction()
{
    U32 timeoutSeconds = 0;
    U32 secondsRemaining = 0;

    enum MSL_system_states state;
    MSL_GetSystemState(&state);

    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "MSL_TimeoutAction: Timeout during state %1.0f.", (float)state);

    timeoutSeconds = GetTimeout(state);

    /* Enter timeout */
    secondsRemaining = MSL_Timeout(timeoutSeconds);

    if(secondsRemaining == 0)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "MSL_TimeoutAction: Timeout Elapsed.");
    }
    else
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "MSL_TimeoutAction: Exited timeout because event occurred.");
    }

    return RXN_SUCCESS;
}

static BOOL HandleEvent(enum MSL_system_events event)
{
    BOOL runImmediately = FALSE;

    enum MSL_system_states state;
    MSL_GetSystemState(&state);

    MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
        "Received event %1.0f in state %1.0f", (float)event, (float)state);

    MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
        "HandleEvent: Received state %1.0f in event %1.0f", (float)state, (float)event);

    switch(event)
    {
    case MSL_EVENT_TIME_UPDATED:
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08, "HandleEvent: Valid time obtained.");

            if(state == MSL_STATE_SNTP_REQUEST_PENDING)
            {
                SNTPRetryReset();
                SetNextSystemState(MSL_STATE_SEED_CHECK);
                runImmediately = TRUE;
            }
            else if((state == MSL_STATE_INITIALIZE)&&(RXN_MSL_GetDataAccess() != MSL_DATA_ACCESS_UNDEFINED))
            {
                MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
                    "HandleEvent: Valid time and data access setting obtained.");
                SetNextSystemState(MSL_STATE_TIME_CHECK);
                runImmediately = TRUE;
            }
            else
            {
                SetNextSystemState(MSL_STATE_TIME_CHECK);
                runImmediately = (state == MSL_STATE_IDLE);
            }
        }
        break;
    case MSL_EVENT_SEED_DOWNLOADED:
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08, "HandleEvent: New seed obtained.");

            /* PGPS-1557 - Do not reset the seed download retry counter
            SeedRetryReset();
            */
            SetNextSystemState(MSL_STATE_SEED_PROPAGATING);
            /* Force seed propagation to run immediately since we are pushing the seed file */
            runImmediately = TRUE;
        }
        break;
    case MSL_EVENT_EXIT_TIMEOUT:
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08, "HandleEvent: Exit timeout event.");
            runImmediately = TRUE;
        }
        break;
    case MSL_EVENT_SYSTEM_CHECK:
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08, "HandleEvent: Scheduling system check.");
            if(state == MSL_STATE_IDLE)
            {
                SetNextSystemState(MSL_STATE_TIME_CHECK);
                runImmediately = TRUE;
            }
        }
        break;
    case MSL_EVENT_FACTORY_RESET:
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
                "HandleEvent: Factory reset called. Starting system check.");
            SetNextSystemState(MSL_STATE_TIME_CHECK);
            runImmediately = TRUE;
        }
        break;
    case MSL_EVENT_WRITE_EPHEMERIS:
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08, "HandleEvent: Ephemeris received.");
            SetNextSystemState(MSL_STATE_TIME_CHECK);

            MSL_EnterBlock(RXN_MSL_CS_SM);
            mNeedSAGPSPropCount = gConfig.numConstels;
            MSL_LeaveBlock(RXN_MSL_CS_SM);

            runImmediately = (state == MSL_STATE_IDLE);
        }
        break;
    case MSL_EVENT_NETWORK_AVAILABLE:
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08, "HandleEvent: Network access available.");

            /* Make sure that the constellations are examined in priority sequence */
            MSL_EnterBlock(RXN_MSL_CS_SM);
            mCurConstelIndex = 0;
            MSL_LeaveBlock(RXN_MSL_CS_SM);

            SetNextSystemState(MSL_STATE_TIME_CHECK);
            runImmediately = TRUE;
        }
        break;
    case MSL_EVENT_IDLE:
    default:
        {
            break;
        }
    }   
    return runImmediately;
}

void MSL_ClockUpdateCountReset()
{
    mClockUpdateCount = 0;
}

static void SeedRetryReset()
{
    gConfig.net.hostPortIndex = 0;
    mSeedDownloadCount = 0;
    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "SeedRetryReset: Has been called.");
}
static void SNTPRetryReset()
{
    mSNTPRequestCount = 0;
    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "SNTPRetryReset: Has been called.");
}


void MSL_SetNextSystemEvent(enum MSL_system_events event)
{
    mNextSystemEvent = event;
    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "MSL_SetNextSystemEvent: Event %1.0f occurred.", (float)event);

    /* Handle the event, waking up the work thread if necessary */
    if (HandleEvent(event))
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
            "HandleEvent: Exiting timeout early.");
        MSL_WakeUpWork();
    }
}

void MSL_SM_FactoryReset()
{
    MSL_EnterBlock(RXN_MSL_CS_SM);
    SeedRetryReset();
    SNTPRetryReset();
    MSL_LeaveBlock(RXN_MSL_CS_SM);
}

void MSL_SM_ResetCounters()
{
    /* Reset data fetch counters */
    if (mSeedDownloadCount >= gConfig.net.downloadRetryMax)
    {
        SeedRetryReset();
    }
    if (mSNTPRequestCount >= gConfig.SNTP.requestRetryMax)
    {
        SNTPRetryReset();
    }
}

RXN_constel_t MSL_GetCurrentConstel()
{
#ifdef RXN_CONFIG_INCLUDE_GLONASS
    return gConfig.constelList[mCurConstelIndex];
#else
    /* Since GLONASS is disabled, this will prevent the SM from downloading GLONASS. */
    return RXN_GPS_CONSTEL;
#endif
}

U08 MSL_GetConstelConfig()
{
    /* Determine what constellations to download */
    if(gConfig.numConstels > 1)
    {
        /* Currently only GPS and GLONASS is supported */
        return RXN_ALL_CONSTEL;
    }
    else
    {
        return MSL_GetCurrentConstel();
    }
}
