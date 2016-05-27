/*
* Copyright (c) 2007-2012 Rx Networks, Inc. All rights reserved.
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
* This file contains implementation of XYBRID functionality.
* 
*/

#include <stdio.h>              /* Required for string manipulation. */
#include <stdlib.h>             /* Required for some string manipulation functions. */
#include <assert.h>

#include "RXN_MSL_Xybrid.h"     /* Contains declarations for fcns within. */
#include "RXN_MSL_Common.h"     /* Internal common declarations. */
#include "RXN_MSL_PGPS.h"

static MSL_SYNC mCommandSync;
static RXN_FullEph_t* mBCEArr;
static U08 mBCEArrSize;

/* Internal prototypes */
static U08 WaitForXybridResponse(RXN_RefLocation_t* pCoarsePosition, U32 seconds);

/* Externals */

extern void MSL_SyncCreate(MSL_SYNC* sync);
extern void MSL_SyncDestroy(MSL_SYNC* sync);
extern U08 MSL_SyncStart(MSL_SYNC* sync, U32 seconds);
extern void MSL_SyncEnd(MSL_SYNC* sync);


void MSL_Xybrid_Initialize()
{
    MSL_SyncCreate(&mCommandSync);
}

void MSL_Xybrid_Uninitialize()
{
    MSL_SyncDestroy(&mCommandSync);
}

static void FinishCommand(RXN_RefLocation_t* location)
{
    MSL_SyncEnd(&mCommandSync);
}

static void StoreBCE(RXN_MSL_NavDataList_t* ephemeris, RXN_constel_t constel)
{
    RXN_MSL_RemoveBCEObserver(StoreBCE);

    if (mBCEArr && ephemeris)
    {
        memcpy(ephemeris->ephList.gps, mBCEArr, sizeof(RXN_FullEph_t) * ephemeris->numEntries);
        mBCEArrSize = ephemeris->numEntries;
    }
    else
    {
        mBCEArrSize = 0;
    }
}

static U08 WaitForXybridResponse(RXN_RefLocation_t* pCoarsePosition, U32 seconds)
{
    RXN_MSL_AddLocationObserver(FinishCommand);

    /* Wait until the request completes, then return  */
    if (MSL_SyncStart(&mCommandSync, seconds) == RXN_SUCCESS)
    {
        RXN_MSL_RemoveLocationObserver(FinishCommand);
        if(pCoarsePosition != NULL)
        {
            MSL_GetXybridPosition(pCoarsePosition);

            MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE09,
                "Xybrid location: Lat = %1f, Lon = %1f", 
                pCoarsePosition->LLA.Lat, pCoarsePosition->LLA.Lon);
        }
        return RXN_SUCCESS;
    }

    return RXN_FAIL;
}

U08 RXN_MSL_SynchroGetPosition(RXN_RefLocation_t* pCoarsePosition, U32 seconds)
{
    assert(pCoarsePosition);   

    if (MSL_RequestXybridAssistance(RXN_MSL_ASSISTANCE_SYNCHRO) != RXN_SUCCESS)
    {
        return RXN_FAIL;
    }

    return WaitForXybridResponse(pCoarsePosition, seconds);
}

U08 RXN_MSL_GetXybridCoarsePosition(RXN_RefLocation_t* pCoarsePosition, U32 seconds)
{
    assert(pCoarsePosition);   

    if (MSL_RequestXybridAssistance(RXN_MSL_ASSISTANCE_XBD_RT) != RXN_SUCCESS)
    {
        return RXN_FAIL;
    }

    return WaitForXybridResponse(pCoarsePosition, seconds);
}

U08 RXN_MSL_XybridGetBCEAndPosition(BOOL filtered, RXN_FullEph_t* PRNArr, U08* ArrSize, RXN_RefLocation_t* pCoarsePosition, U32 seconds)
{
    assert(pCoarsePosition);   
    assert(PRNArr);
    assert(ArrSize);

    MSL_SetBCEFiltered(filtered);

    MSL_EnterBlock(RXN_MSL_CS_Xybrid);
    mBCEArr = PRNArr;
    MSL_LeaveBlock(RXN_MSL_CS_Xybrid);

    U32 mask = RXN_MSL_ASSISTANCE_XBD_BCE;

    if (pCoarsePosition != NULL)
    {
        mask |= RXN_MSL_ASSISTANCE_XBD_RT;
    }

    RXN_MSL_AddBCEObserver(StoreBCE);

    if (MSL_RequestXybridAssistance(mask) != RXN_SUCCESS)
    {
        return RXN_FAIL;
    }

    U08 result = WaitForXybridResponse(pCoarsePosition, seconds);

    if (result == RXN_SUCCESS)
    {
        *ArrSize = mBCEArrSize;
    }
    else
    {
        *ArrSize = 0;
    }

    mBCEArr = NULL;

    return result;
}

U08 RXN_MSL_XybridGetBCE(BOOL filtered, RXN_FullEph_t* PRNArr, U08* ArrSize, U32 seconds)
{
    return RXN_MSL_XybridGetBCEAndPosition(filtered, PRNArr, ArrSize, NULL, seconds);
}

