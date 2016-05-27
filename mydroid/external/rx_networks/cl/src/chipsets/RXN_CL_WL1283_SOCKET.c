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
* $LastChangedDate$
* $Revision$
*************************************************************************
* 
*/
#include "RXN_CL_Common.h"          /* Includes declarations for common elements within. */
#include "RXN_CL_Platform.h"        /* Includes platform dependent declarations */
#include "NavLinkCustom.h"          /* Includes declarations for NavLink */

extern S32 gCL_ClockOffset;

#ifndef RXN_CL_ERROR_CODE
/* This typedef makes the code more readable. */
typedef U16 RXN_CL_ERROR_CODE;
#endif

/******************************************************
* Define the chipset label or name.                  *
* Where appropriate - include a req'd firmware rev.  *
******************************************************/
#define CHIP_LABEL      "WL1283-socket"

#define MCPF_SOCKET_PORT    4121
#define MCPF_UNIX_PATH      "/data/gps/gps4121"

#define MAX_PAYLOAD 	3000
#define INVALID_SV_ID   0xff /* Used by TI stack to mark an SV ID as invalid. */

typedef struct
{
    U08 syncStart[4];
    U08 msgClass;
    U08 opCode;
    U16 payloadLen;
    /* payload follows */
} MESSAGE_HEADER_T;

typedef struct
{
    BOOL msBased;       /* Use MS-Based fix requests rather than MS-Autonomous. */
    BOOL updateBCE;     /* If TRUE, NAVC will report BCE. */
    BOOL regGPSTime;    /* If TRUE, NAVC will report GPS time. */
    BOOL useUNIX;       /* If TRUE, connect to NAVC using UNIX domain sockets */
    char serverIpAddress[20];
} CL_CONFIG_T;

/*
These are the synchronous requests that our app will make to the 
MCPF stack.
*/
typedef enum
{
    eRequestNone = 0,
    eRequestStart,
    eRequestStop,
    eRequestInjectAssistance,
    eRequestGetVersion,
    eRequestStopFixData,
    eRequestGetEphemeris,
    eRequestGetTime,
    eRequestDeleteAssistance,
} REQUEST_TYPE;

typedef enum
{
    eUninitialized,
    eInitialized,
    eStarted,
    eFixReporting,
} CHIPSET_STATE;

typedef enum
{
    RECV_AWAITING_SYNC_1,
    RECV_AWAITING_SYNC_2,
    RECV_AWAITING_SYNC_3,
    RECV_AWAITING_SYNC_4,
    RECV_COLLECTING_HEADER,
    RECV_COLLECTING_PAYLOAD,
} RECEIVE_STATE;

/* 
This struct represents an instance of the chipset-dependent part of the
generic chipset instance.

All instance variables go here. We avoid global and static variables.
*/
typedef struct _CHIPSET_DEP
{
    LOCK hLock; /* For threadsafe access. */
    RXN_CL_FixData_t fixData; /* Current location fix data. Value is updated asynchronously. */
    BOOL fixError; /* TRUE if chipset reports a fix error. */
    RXN_CL_Restarts_t restartType; /* Current restart type. */
    CHIPSET_STATE state; /* Records current state of chipset. */
    REQUEST_TYPE outstandingRequest; /* Synchronous request that is currently outstanding. */
    EVENT hResponseEvent; /* Event signaled when response arrives. */
    U08 pResponse[sizeof (MESSAGE_HEADER_T) + MAX_PAYLOAD]; /* Message from lower stack layer which contains response data. */
    THREAD hWorkerThread; /* handle to worker thread which reads GPSC messages. */
    BOOL exitNow;
    SOCKET hSocket;
    struct sockaddr peer;
    U08 SendMessage[sizeof (MESSAGE_HEADER_T) + MAX_PAYLOAD];
    MESSAGE_HEADER_T* SendHeader; /* Pointer to header in message about to be sent. */
    U08* SendPayload; /* Pointer to payload in message about to be sent. */
    RECEIVE_STATE ReceiveState;
    U08* ReceivePtr; /* Running pointer for destination of next received byte. */
    U08 ReceiveMessage[sizeof (MESSAGE_HEADER_T) + MAX_PAYLOAD];
    MESSAGE_HEADER_T* ReceiveHeader; /* Pointer to NAVL header in received message. */
    U08* ReceivePayload; /* Pointer to payload in received message. */
    U08 RawReadBuffer[sizeof (MESSAGE_HEADER_T) + MAX_PAYLOAD]; /* Raw buffer for socket reads. */
    CL_CONFIG_T ClConfig;
} CHIPSET_DEP;

CHIPSET_DEP g_ChipsetDep;

#define TWO_TO_23	8388608.0
#define TWO_TO_24	16777216.0
#define LAT_SCALE (TWO_TO_23 / 90.0)
#define LONG_SCALE (TWO_TO_24 / 360.0)

#define TWO_TO_32   4294967296.0
#define RAW_LAT_SCALE (180.0 / TWO_TO_32)
#define TWO_TO_31   2147483648.0
#define RAW_LONG_SCALE (180.0 / TWO_TO_31)

#define AUTONOMOUS_SESSION  254

/* Redefined here so we don't have to include a bunch of TI headers. */
#define  NF_FIX_2D      0x0001
#define  NF_FIX_3D      0x0002

static U16 gLastPRNTOEs[RXN_CONSTANT_NUM_PRNS] = {0};
static U16 gLastPRNIODCs[RXN_CONSTANT_NUM_PRNS] = {0};

static struct timeval* mpTimeout;
static struct timeval mTimeout;
static BOOL mConnected = FALSE;

#define NAVC_CONNECT_RETRY_TIMEOUT_S        10      /* value in seconds */
#define HOSTSOCKET_MESSAGE_SYNC_START_1		0xF0
#define HOSTSOCKET_MESSAGE_SYNC_START_2		0xF1
#define HOSTSOCKET_MESSAGE_SYNC_START_3		0xF2
#define HOSTSOCKET_MESSAGE_SYNC_START_4		0xF3

static void NavcEvtAssistRepTime(CHIPSET_DEP* pCD);

static void NavcEventCmdComplete(CHIPSET_DEP* pCD);
static void NavcEvtPositionFixReport(CHIPSET_DEP* pCD);
static void NavcEvtVersionResponse(CHIPSET_DEP* pCD);
static void NavcEvtAssistRepEph(CHIPSET_DEP* pCD);
static void NavcEvtAssistanceRequest(CHIPSET_DEP* pCD);

static BOOL NavcCmdInject15Ephemeris(CHIPSET_DEP* pCD, RXN_FullEph_t** ppGroup, U08 groupCount);
static BOOL NavcCmdInjectPosition(CHIPSET_DEP* pCD, RXN_RefLocation_t* pRefLoc);
static BOOL NavcCmdInjectTime(CHIPSET_DEP* pCD, RXN_RefTime_t* pRefTime);
static BOOL NavcCmdInjectAssistanceComplete(CHIPSET_DEP* pCD);
static BOOL NavcCmdSetTow(CHIPSET_DEP* pCD, RXN_RefTime_t* pRefTime);
static BOOL NavcCmdGetVersion(CHIPSET_DEP* pCD);
static U08 UREfromURA(U08 URAIdx);
static void* WorkerThread(void* pArg);
static int SetSockaddr(CHIPSET_DEP* pCD);
static BOOL ConnectToNavc(CHIPSET_DEP* pCD);
static void DisconnectNavc(SOCKET sckt);
static void ManageConnection(CHIPSET_DEP* pCD);
static int SetFdSet(CHIPSET_DEP* pCD, fd_set* readSet);
static void SetTimeout(U08 seconds);
static U16 InitializeConnection(CHIPSET_DEP* pCD);

typedef void (*NAVC_EVT_HANDLER_T)(CHIPSET_DEP*);

typedef struct
{
    U08 opCode;
    NAVC_EVT_HANDLER_T handler;
} NAVC_EVT_TABLE_ENTRY_T;

static NAVC_EVT_TABLE_ENTRY_T gGpsEvtTable[] =
{
    {NAVC_EVT_CMD_COMPLETE, NavcEventCmdComplete},
    {NAVC_EVT_POSITION_FIX_REPORT, NavcEvtPositionFixReport},
    {NAVC_EVT_VERSION_RESPONSE, NavcEvtVersionResponse},
    {NAVC_EVT_ASSIST_REP_EPH, NavcEvtAssistRepEph},
    {NAVC_EVT_ASSIST_REP_TIME, NavcEvtAssistRepTime},
    {NAVC_EVT_ASSISTANCE_REQUEST, NavcEvtAssistanceRequest},
};

#ifdef USE_MSL         /* USE_MSL is defined in makefile for Linux and project file for Windows. */
/* Set GPS time to MSL */
static void SetGPSTime(McpU16 gpsWeek, McpU32 mSec)
{
    RXN_RefTime_t refTime;
    /* GPS week number, if '0' or maxed out, time is unknown.           
    * GPS msec, if maxed out, time is unknown. */
    if((gpsWeek != 0) && (gpsWeek != 65535) && (mSec < 604800000))
    {
        refTime.weekNum = gpsWeek; 
        refTime.TOWmSec = mSec;
        refTime.TOWnSec = 0;
        refTime.TAccmSec = 0; 
        refTime.TAccnSec = 0;

        RXN_MSL_SetGPSTime(&refTime); 
    }
}
#endif

/* 
* \brief 
* Updates local current fix data with value reported by chipset.
* 
* \param pCD 
* [In] Pointer to chipset-dependent instance.
* \param pRawPosition 
* [In] Pointer to chipset position response.
*
* \returns 
*/
static void UpdateFixData(CHIPSET_DEP* pCD, T_GPSC_raw_position* pRawPosition)
{
    T_GPSC_position* pPosition = &pRawPosition->position;
    //double PI = 3.14159265358979324;
    int i;

    /* Firstly we should update the offset from system-derived GPS time.. */
    U32 gpsRcvrTime = (pRawPosition->toa.gps_week * 86400 * 7) + pRawPosition->toa.gps_msec / 1000;

    /* Return the difference from system-derived GPS time. */
    gCL_ClockOffset = gpsRcvrTime - CL_GetGPSTime();

    CL_EnterBlock(&pCD->hLock);

    pCD->fixData.numPRN = pPosition->c_position_sv_info;
    for (i = 0; i < pPosition->c_position_sv_info; i++)
    {
        pCD->fixData.PRNs[i] = pPosition->position_sv_info[i].svid + 1;
    }

    pCD->fixData.HDOP = (float) pPosition->HDOP / 10.0F;

    if ((pPosition->raw_fix_result_bitmap & NF_FIX_2D) == NF_FIX_2D)
    {
        pCD->fixData.fixDimensions = 2;
    }
    else if ((pPosition->raw_fix_result_bitmap & NF_FIX_3D) == NF_FIX_3D)
    {
        pCD->fixData.fixDimensions = 3;
    }
    else
    {
        pCD->fixData.fixDimensions = 0;
    }

    CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
        "UpdateFixData: Fix dimensions: %1.0f.", pCD->fixData.fixDimensions);

    pCD->fixData.LLA.Alt = (float) (pPosition->altitude_wgs84 / 2.0);
    pCD->fixData.LLA.Lat = (float) (pPosition->latitude_radians * RAW_LAT_SCALE);
    pCD->fixData.LLA.Lon = (float) (pPosition->longitude_radians * RAW_LONG_SCALE);

    CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
        "UpdateFixData: Lat: %1.6f.", pCD->fixData.LLA.Lat);
    CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
        "UpdateFixData: Lon: %1.6f.", pCD->fixData.LLA.Lon);
    CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
        "UpdateFixData: Alt: %1.0f.", pCD->fixData.LLA.Alt);

    CL_ConvertLLAToECEF(pCD->fixData.LLA, &pCD->fixData.ECEF);

    pCD->fixError = FALSE;

    CL_LeaveBlock(&pCD->hLock);
}

/* 
* \brief 
* Records that chipset reported an error.
* 
* \param pCD 
* [In] Pointer to chipset-dependent instance.

* \returns 
*/
static void SetFixError(CHIPSET_DEP* pCD)
{
    CL_EnterBlock(&pCD->hLock);
    /* Invalidate fix so that EVK will only get the latest from chipset. */
    pCD->fixData.fixDimensions = 0;
    pCD->fixData.numPRN = 0;
    pCD->fixData.HDOP = 1000.0;
    pCD->fixError = TRUE;
    CL_LeaveBlock(&pCD->hLock);
}

static BOOL SendBytes(SOCKET hSocket, U08* pData, int bytesToSend)
{
    int bytesSent = 0;

    while (bytesToSend)
    {
        bytesSent = send(hSocket, pData + bytesSent, bytesToSend, 0);
        if (bytesSent < 0)
        {
            return FALSE;
        }
        else if (bytesSent == 0)
        {
            /* Peer shutdown has occurred. */
            return FALSE;
        }
        else
        {
            bytesToSend -= bytesSent;
        }
    }
    return TRUE;
}

static BOOL SendCmd(CHIPSET_DEP* pCD)
{
    if (!SendBytes(pCD->hSocket, pCD->SendMessage, sizeof (MESSAGE_HEADER_T) + pCD->SendHeader->payloadLen))
    {
        CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
            "Send cmdOpcode %1.0f failed!", (double) pCD->SendHeader->opCode);
        return FALSE;
    }

    return TRUE;
}

static void LogEphemeris(T_GPSC_ephemeris_assist* pAssist)
{
    char ephList[CL_MAX_LOG_ENTRY];

    if(pAssist == NULL)
    {
        return;
    }

    snprintf(ephList, sizeof(ephList), "N,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d.",
        pAssist->svid,
        pAssist->ephem_week,
        pAssist->iode,
        pAssist->ephem_crs,
        pAssist->ephem_delta_n,
        pAssist->ephem_cuc,
        pAssist->ephem_m0,
        pAssist->ephem_e,
        pAssist->ephem_a_power_half,
        pAssist->ephem_omega_a0,
        pAssist->ephem_cus,
        pAssist->ephem_toe,
        pAssist->ephem_cic,
        pAssist->ephem_cis,
        pAssist->ephem_i0,
        pAssist->ephem_w,
        pAssist->ephem_omega_adot,
        pAssist->ephem_crc,
        pAssist->ephem_idot,
        pAssist->ephem_code_on_l2,
        pAssist->ephem_ura,
        pAssist->ephem_svhealth,
        pAssist->ephem_tgd,
        pAssist->ephem_iodc,
        pAssist->ephem_toc,
        pAssist->ephem_af2,
        pAssist->zzz_align0,
        pAssist->ephem_af1,
        pAssist->ephem_af0);

    CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
        "NavcCmdInject15Ephemeris: %s", ephList);    
}

static void SetMessageSync(CHIPSET_DEP* pCD)
{
    pCD->SendHeader->syncStart[0] = HOSTSOCKET_MESSAGE_SYNC_START_1;
    pCD->SendHeader->syncStart[1] = HOSTSOCKET_MESSAGE_SYNC_START_2;
    pCD->SendHeader->syncStart[2] = HOSTSOCKET_MESSAGE_SYNC_START_3;
    pCD->SendHeader->syncStart[3] = HOSTSOCKET_MESSAGE_SYNC_START_4;
}

BOOL NavcCmdPgpsProviderRegister(CHIPSET_DEP* pCD)
{
    /* Create request. */
    TNAVC_assist_src_type* pAssistSrcType = (TNAVC_assist_src_type*) pCD->SendPayload;
    pAssistSrcType->eAssistSrcType = PGPS_PROVIDER;

    SetMessageSync(pCD);
    pCD->SendHeader->msgClass = NAVL_CLASS_GPS;
    pCD->SendHeader->opCode = NAVC_CMD_REGISTER_ASSIST_SRC;
    pCD->SendHeader->payloadLen = sizeof (TNAVC_assist_src_type);

    return SendCmd(pCD);
}

BOOL NavcCmdSagpsProviderRegister(CHIPSET_DEP* pCD)
{
    /* Create request. */
    TNAVC_assist_src_type* pAssistSrcType = (TNAVC_assist_src_type*) pCD->SendPayload;
    pAssistSrcType->eAssistSrcType = SAGPS_PROVIDER;

    SetMessageSync(pCD);
    pCD->SendHeader->msgClass = NAVL_CLASS_GPS;
    pCD->SendHeader->opCode = NAVC_CMD_SAGPS_PROVIDER_REGISTER;
    pCD->SendHeader->payloadLen = sizeof (TNAVC_assist_src_type);

    return SendCmd(pCD);
}

/* Don't send fix request. NAVC will send fix data automatically */
static BOOL NavcCmdRequestFix(CHIPSET_DEP* pCD)
{
    /* Create request. */
    TNAVC_reqLocFix* pReqLocFix = (TNAVC_reqLocFix*) pCD->SendPayload;

    SetMessageSync(pCD);
    pCD->SendHeader->msgClass = NAVL_CLASS_GPS;
    pCD->SendHeader->opCode = NAVC_CMD_REQUEST_FIX;
    pCD->SendHeader->payloadLen = sizeof (TNAVC_reqLocFix);

    pReqLocFix->loc_fix_session_id = AUTONOMOUS_SESSION;
    if(pCD->ClConfig.msBased)
    {
        pReqLocFix->loc_fix_mode = GPSC_FIXMODE_MSBASED;
    }
    else
    {
        pReqLocFix->loc_fix_mode = GPSC_FIXMODE_AUTONOMOUS;
    }
    pReqLocFix->loc_fix_result_type_bitmap = GPSC_RESULT_RAW;
    pReqLocFix->loc_fix_max_ttff = 75;
    pReqLocFix->loc_fix_period = 1;
    pReqLocFix->loc_fix_num_reports = 0; /* 0 = infinite */
    /* pReqLocFix->loc_fix_qop = ???; TODO: seem to be ignored. */
    /* pReqLocFix->loc_fix_velocity = ???; TODO: seem to be ignored. */

    return SendCmd(pCD);
}

static BOOL NavcCmdStopFix(CHIPSET_DEP* pCD)
{
    /* Create request. */
    TNAVC_stopLocFix* pStopLocFix = (TNAVC_stopLocFix*) pCD->SendPayload;

    SetMessageSync(pCD);
    pCD->SendHeader->msgClass = NAVL_CLASS_GPS;
    pCD->SendHeader->opCode = NAVC_CMD_STOP_FIX;
    pCD->SendHeader->payloadLen = sizeof (TNAVC_stopLocFix);

    pStopLocFix->uSessionId = (U16) AUTONOMOUS_SESSION;

    return SendCmd(pCD);
}

static BOOL NavcCmdInject15Ephemeris(CHIPSET_DEP* pCD, RXN_FullEph_t** ppGroup, U08 groupCount)
{
    /* Create request. */
    TNAVC_injectAssist* pInjectAssist = (TNAVC_injectAssist*) pCD->SendPayload;
    T_GPSC_ephemeris_assist* pAssist = pInjectAssist->tAssistance.assistance_inject_union.ephemeris_assist;
    U08 i = 0;

    SetMessageSync(pCD);
    pCD->SendHeader->msgClass = NAVL_CLASS_GPS;
    pCD->SendHeader->opCode = NAVC_CMD_INJECT_ASSISTANCE;
    pCD->SendHeader->payloadLen = sizeof (TNAVC_injectAssist);

    pInjectAssist->uSessionId = 0; /* Ignored. */
    pInjectAssist->tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_EPH;
    pInjectAssist->uSystemTimeInMsec = (McpU32)time(NULL) * 1000; /* Taken from implementation of os_get_current_time_inMilliSec() */
    pInjectAssist->bValidGsmTime = MCP_FALSE;

    for (i = 0; i < groupCount; i++)
    {
        RXN_FullEph_t* pEphemeris = ppGroup[i];

        pAssist->svid = pEphemeris->prn - 1; /* svid is zero-based */
        pAssist->iode = (U08) pEphemeris->iode;
        pAssist->ephem_crs = pEphemeris->crs;
        pAssist->ephem_delta_n = pEphemeris->delta_n;
        pAssist->ephem_cuc = pEphemeris->cuc;
        pAssist->ephem_m0 = pEphemeris->m0;
        pAssist->ephem_e = pEphemeris->e;
        pAssist->ephem_a_power_half = pEphemeris->sqrt_a;
        pAssist->ephem_omega_a0 = pEphemeris->omega0;
        pAssist->ephem_cus = pEphemeris->cus;
        pAssist->ephem_toe = pEphemeris->toe;
        pAssist->ephem_cic = pEphemeris->cic;
        pAssist->ephem_cis = pEphemeris->cis;
        pAssist->ephem_i0 = pEphemeris->i0;
        pAssist->ephem_w = pEphemeris->w;
        pAssist->ephem_omega_adot = pEphemeris->omega_dot;
        pAssist->ephem_crc = pEphemeris->crc;
        pAssist->ephem_idot = pEphemeris->i_dot;
        pAssist->ephem_code_on_l2 = pEphemeris->CAOrPOnL2; /* TODO: check! */
        pAssist->ephem_ura = pEphemeris->ura;
        pAssist->ephem_svhealth = pEphemeris->health;
        pAssist->ephem_tgd = pEphemeris->TGD;
        pAssist->ephem_iodc = pEphemeris->iodc;
        pAssist->ephem_toc = pEphemeris->toc;
        pAssist->ephem_af2 = pEphemeris->af2;
        pAssist->zzz_align0 = 0; /* For completeness! */
        pAssist->ephem_af1 = pEphemeris->af1;
        pAssist->ephem_af0 = pEphemeris->af0;

        /* following are new added items comparing with NaviLink 6.1 */
        pAssist->ephem_predicted = (pEphemeris->iodc == 0 && pEphemeris->TGD == 0); /*Set to TRUE if the ephemeris is predicted, FALSE if it is decoded from sky */
        pAssist->ephem_predSeedAge = 0; /*Used for storing the Ephemeris seed age incase of Prediceted Ephemeris*/
        pAssist->ephem_week = pEphemeris->gps_week;
        /* TODO: what to do with pEphemeris->ure. */

        /* DEBUG */
        LogEphemeris(pAssist);

        pAssist++;
    }

    /* We need to mark remaining empty elements as invalid. */
    for (; i < N_SV_15; i++)
    {
        //RXN_FullEph_t* pEphemeris = ppGroup[i];
        pAssist->svid = INVALID_SV_ID;

        pAssist++;
    }

    return SendCmd(pCD);
}

static BOOL NavcCmdInjectPosition(CHIPSET_DEP* pCD, RXN_RefLocation_t* pRefLoc)
{
    /* Create request. */
    TNAVC_injectAssist* pInjectAssist = (TNAVC_injectAssist*) pCD->SendPayload;

    SetMessageSync(pCD);
    pCD->SendHeader->msgClass = NAVL_CLASS_GPS;
    pCD->SendHeader->opCode = NAVC_CMD_INJECT_ASSISTANCE;
    pCD->SendHeader->payloadLen = sizeof (TNAVC_injectAssist);

    pInjectAssist->uSessionId = 0; /* Ignored. */
    pInjectAssist->tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_POSITION;
    pInjectAssist->uSystemTimeInMsec = (McpU32)(time(NULL) * 1000); /* Taken from implementation of os_get_current_time_inMilliSec() */
    pInjectAssist->bValidGsmTime = MCP_FALSE;

    {
        T_GPSC_position_assist* pAssist = &pInjectAssist->tAssistance.assistance_inject_union.position_assist;
        RXN_LLA_LOC_t LLA;
        RXN_LLA_LOC_t* pLLA;

        switch (pRefLoc->type)
        {
        case RXN_LOC_ECEF:
            CL_ConvertECEFToLLA(pRefLoc->ECEF, &LLA);
            pLLA = &LLA;
            break;
        case RXN_LOC_LLA:
            pLLA = &pRefLoc->LLA;
            break;
        case RXN_LOC_BOTH:
            pLLA = &pRefLoc->LLA;
            break;
        default:
            /* Illegal type. */
            return FALSE; /* TODO: RXN_CL_INVALID_REF_LOC_FORMAT_ERR; */
        }

        /* In the following conversions I had to avoid using fabs() or
        * casting floats to unsigned ints or linker would complain.
        */

        if (pLLA->Lat < 0.0F)
        {
            pAssist->latitude = (U32) (pLLA->Lat * LAT_SCALE * -1.0F);
            pAssist->latitude_sign = 1;
        }
        else
        {
            pAssist->latitude = (U32) (pLLA->Lat * LAT_SCALE);
            pAssist->latitude_sign = 0;
        }

        pAssist->longitude = (S32) (pLLA->Lon * LONG_SCALE);

        if (pLLA->Alt < 1.0F)
        {
            S16 alt = (S16) (pLLA->Alt * -1.0F);
            pAssist->altitude = (U16) alt;
            pAssist->altitude_sign = 1;
        }
        else
        {
            S16 alt = (S16) (pLLA->Alt);
            pAssist->altitude = (U16) alt;
            pAssist->altitude_sign = 0;
        }

        if (pRefLoc->uncertSemiMajor != 0)
            pAssist->position_uncertainty = pRefLoc->uncertSemiMajor * 10;
        else
            pAssist->position_uncertainty = 10000;

        /* TODO: Do we need to set the new struct member. It is currently ignored by MCP.
        pAssist->d_assist_ToaSec = ?????;
        */
    }

    return SendCmd(pCD);
}

static BOOL NavcCmdInjectTime(CHIPSET_DEP* pCD, RXN_RefTime_t* pRefTime)
{
    /* Create request. */
    char logMsg[CL_MAX_LOG_ENTRY];
    TNAVC_injectAssist* pInjectAssist = (TNAVC_injectAssist*) pCD->SendPayload;

    SetMessageSync(pCD);
    pCD->SendHeader->msgClass = NAVL_CLASS_GPS;
    pCD->SendHeader->opCode = NAVC_CMD_INJECT_ASSISTANCE;
    pCD->SendHeader->payloadLen = sizeof (TNAVC_injectAssist);

    pInjectAssist->uSessionId = 0; /* Ignored. */
    pInjectAssist->tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_TIME;
    pInjectAssist->uSystemTimeInMsec = (McpU32)(time(NULL) * 1000); /* Taken from implementation of os_get_current_time_inMilliSec() */
    pInjectAssist->bValidGsmTime = MCP_FALSE;

    {
        T_GPSC_time_assist* pAssist = &pInjectAssist->tAssistance.assistance_inject_union.time_assist;

        pAssist->gps_week = pRefTime->weekNum;
        pAssist->zzz_align[0] = 0; /* For completeness. */
        pAssist->zzz_align[1] = 0; /* For completeness. */
        pAssist->gps_msec = pRefTime->TOWmSec;
        pAssist->sub_ms = pRefTime->TOWnSec / 1000; /* Convert to microseconds. */
        pAssist->time_unc = 0; /* TODO: set this properly */
        pAssist->time_accuracy = GPSC_TIME_ACCURACY_COARSE; /* Coarse time mode */

        sprintf(logMsg, "gps_week = %d, gps_ms = %d, sub_ms = %d, time_unc = %d, time_accuracy = %d",
            pAssist->gps_week, pAssist->gps_msec, pAssist->sub_ms, pAssist->time_unc, pAssist->time_accuracy);

        CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
            "NavcCmdInjectTime: %s", logMsg);
    }

    return SendCmd(pCD);
}

static BOOL NavcCmdInjectAssistanceComplete(CHIPSET_DEP* pCD)
{
    /* Create request. */
    TNAVC_injectAssist* pInjectAssist = (TNAVC_injectAssist*) pCD->SendPayload;

    SetMessageSync(pCD);
    pCD->SendHeader->msgClass = NAVL_CLASS_GPS;
    pCD->SendHeader->opCode = NAVC_CMD_INJECT_ASSISTANCE;
    pCD->SendHeader->payloadLen = sizeof (TNAVC_injectAssist);

    pInjectAssist->uSessionId = 0; /* Ignored. */
    pInjectAssist->tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_COMPL_SET;
    pInjectAssist->uSystemTimeInMsec = (McpU32)(time(NULL) * 1000); /* Taken from implementation of os_get_current_time_inMilliSec() */
    pInjectAssist->bValidGsmTime = MCP_FALSE;
    pInjectAssist->tAssistance.assistance_inject_union.assist_comp = 0x01; /* Ignored. */

    return SendCmd(pCD);
}

static BOOL NavcCmdDeleteAssistance(CHIPSET_DEP* pCD, U08 delAssistBitmap)
{
    /* Create request. */
    TNAVC_delAssist* pDelAssist = (TNAVC_delAssist*) pCD->SendPayload;

    SetMessageSync(pCD);
    pCD->SendHeader->msgClass = NAVL_CLASS_GPS;
    pCD->SendHeader->opCode = NAVC_CMD_DELETE_ASISTANCE;
    pCD->SendHeader->payloadLen = sizeof (TNAVC_delAssist);

    pDelAssist->uDelAssistBitmap = delAssistBitmap;
    pDelAssist->uSvBitmap = 0xffffffff; /* For all SNVs */

    return SendCmd(pCD);
}

static BOOL NavcCmdGetAssistEph(CHIPSET_DEP* pCD)
{
    /* Create request. */
    SetMessageSync(pCD);
    pCD->SendHeader->msgClass = NAVL_CLASS_GPS;
    pCD->SendHeader->opCode = NAVC_CMD_GET_ASSIST_EPH;
    pCD->SendHeader->payloadLen = 0;

    return SendCmd(pCD);
}

static BOOL NavcCmdGetAssistTime(CHIPSET_DEP* pCD)
{
    /* Create request. */
    SetMessageSync(pCD);
    pCD->SendHeader->msgClass = NAVL_CLASS_GPS;
    pCD->SendHeader->opCode = NAVC_CMD_GET_ASSIST_TIME;
    pCD->SendHeader->payloadLen = 0;

    return SendCmd(pCD);
}

static BOOL NavcCmdSetTow(CHIPSET_DEP* pCD, RXN_RefTime_t* pRefTime)
{
    /* Create request. */
    /* TODO: for efficiency, payloadLen should be actual size of data sent, not the union size! */
    TNAVC_setTow* pSetTow = (TNAVC_setTow*) pCD->SendPayload;

    SetMessageSync(pCD);
    pCD->SendHeader->msgClass = NAVL_CLASS_GPS;
    pCD->SendHeader->opCode = NAVC_CMD_SET_TOW;
    pCD->SendHeader->payloadLen = sizeof (TNAVC_setTow);

    pSetTow->eTimeMode = GPSC_TIME_ACCURACY_COARSE; /* Saves us including navc_defs.h for NAVCD_TIME_NOW */
    pSetTow->uSystemTimeInMsec = (McpU32)(time(NULL) * 1000); /* Taken from implementation of os_get_current_time_inMilliSec() */

    pSetTow->tTimeAssist.gps_week = pRefTime->weekNum;
    pSetTow->tTimeAssist.zzz_align[0] = 0; /* For completeness. */
    pSetTow->tTimeAssist.zzz_align[1] = 0; /* For completeness. */
    pSetTow->tTimeAssist.gps_msec = pRefTime->TOWmSec;
    pSetTow->tTimeAssist.sub_ms = pRefTime->TOWnSec / 1000; /* Convert to microseconds. */
    pSetTow->tTimeAssist.time_unc = 2000000; /* Nanoseconds. Previously EVK set this value. Now we do it here.
                                             If the value is zero, injected ephemeris has a negative effect
                                             on TTFFs i.e. TTFFs will be always greater than 16s and regularly
                                             greater than 100s.
                                             */
    pSetTow->tTimeAssist.time_accuracy = GPSC_TIME_ACCURACY_COARSE;

    return SendCmd(pCD);
}

static BOOL NavcCmdGetVersion(CHIPSET_DEP* pCD)
{
    /* Create request. */
    SetMessageSync(pCD);
    pCD->SendHeader->msgClass = NAVL_CLASS_GPS;
    pCD->SendHeader->opCode = NAVC_CMD_GET_VERSION;
    pCD->SendHeader->payloadLen = 0;

    return SendCmd(pCD);
}

/* This function is used to register for BCE and GPS time updates from NAVC. */
static BOOL NavcCmdRegForAssist(CHIPSET_DEP* pCD, U16 assistBitmap)
{
    /* Create request. */
    TNAVC_Reg_For_Assist* pRegAssist = (TNAVC_Reg_For_Assist*) pCD->SendPayload;

    SetMessageSync(pCD);
    pCD->SendHeader->msgClass = NAVL_CLASS_GPS;
    pCD->SendHeader->opCode = NAVC_CMD_REG_FOR_ASSISTANCE;  /* register for BCE updates and/or GPS time update */
    pCD->SendHeader->payloadLen = sizeof(TNAVC_Reg_For_Assist); 

    pRegAssist->AssistBitmap = assistBitmap;

    return SendCmd(pCD);
}

static void NavcEventCmdComplete(CHIPSET_DEP* pCD)
{
    /* This handler is just for informational purposes. */
    TNAVC_evt* pEvt = (TNAVC_evt*) pCD->ReceivePayload;

    CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
        "NavcEventCmdComplete: pEvt->eComplCmd = %1.0f", (float) pEvt->eComplCmd);

    switch (pEvt->eComplCmd)
    {
    case NAVC_CMD_START:
        CL_EnterBlock(&pCD->hLock);
        if(pCD->outstandingRequest == eRequestStart)
        {
            memcpy(pCD->pResponse, pCD->ReceivePayload, pCD->ReceiveHeader->payloadLen);
            CL_SignalEvent(&pCD->hResponseEvent);
        }
        CL_LeaveBlock(&pCD->hLock);
        break;
    case NAVC_CMD_STOP:
        CL_EnterBlock(&pCD->hLock);
        if(pCD->outstandingRequest == eRequestStop)
        {
            memcpy(pCD->pResponse, pCD->ReceivePayload, pCD->ReceiveHeader->payloadLen);
            CL_SignalEvent(&pCD->hResponseEvent);
        }
        CL_LeaveBlock(&pCD->hLock);
        break;

    default:
        CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
            "NavcEventCmdComplete: cmdOpcode %1.0f is not handled.", (float) pEvt->eComplCmd);
    }
}

static void NavcEvtPositionFixReport(CHIPSET_DEP* pCD)
{
    TNAVC_evt* pEvt = (TNAVC_evt*) pCD->ReceivePayload;
    T_GPSC_loc_fix_response* pLocFixResponse;

    if (pEvt->eResult != RES_OK)
    {
        CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
            "NavcEvtPositionFixReport: SetFixError.");
        SetFixError(pCD);
        return;
    }

    pLocFixResponse = &pEvt->tParams.tLocFixReport;

    if (pLocFixResponse->ctrl_loc_fix_response_union == GPSC_RESULT_RAW_POSITION)
    {
        CL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
            "NavcEvtPositionFixReport:Raw position report.");
        UpdateFixData(pCD, &pLocFixResponse->loc_fix_response_union.raw_position);
    }
    //else if(pLocFixResponse->ctrl_loc_fix_response_union == GPSC_RESULT_RAW_MEASUREMENT)
    //{
    //    CL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
    //        "NavcEvtPositionFixReport:Raw measurements.");
    //    /* We don't care about raw measurements. */
    //}
}

static void NavcEvtVersionResponse(CHIPSET_DEP* pCD)
{
    TNAVC_evt* pEvt = (TNAVC_evt*) pCD->ReceivePayload;
    char navcEvtVersion[RXN_CL_CHIP_MAX_STR_LEN];

    if (pEvt->eResult != RES_OK)
    {
        CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
            "NavcEvtVersionResponse failed: %1.0f.", (double) pEvt->eResult);
        return;
    }

    CL_EnterBlock(&pCD->hLock);
    memcpy(pCD->pResponse, pCD->ReceivePayload, pCD->ReceiveHeader->payloadLen);

    /* Format the return string. */
    sprintf(navcEvtVersion, "HW %d.%d SW %d.%d.%d.%d %d-%d-%d",
        pEvt->tParams.tVerResponse.hw_major,
        pEvt->tParams.tVerResponse.hw_minor,
        pEvt->tParams.tVerResponse.sw_major,
        pEvt->tParams.tVerResponse.sw_minor,
        pEvt->tParams.tVerResponse.sw_subminor1,
        pEvt->tParams.tVerResponse.sw_subminor2,
        pEvt->tParams.tVerResponse.sw_year,
        pEvt->tParams.tVerResponse.sw_month,
        pEvt->tParams.tVerResponse.sw_day
        );

    CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
        "NavcEvtVersionResponse: %s.", navcEvtVersion);

    CL_SignalEvent(&pCD->hResponseEvent);
    CL_LeaveBlock(&pCD->hLock);
}

static void NavcEvtAssistRepEph(CHIPSET_DEP* pCD)
{
    TNAVC_evt* pEvt = (TNAVC_evt *) pCD->ReceivePayload;

    CL_EnterBlock(&pCD->hLock);
#ifndef USE_MSL
    if (pCD->outstandingRequest == eRequestGetEphemeris)
    {
        memcpy(pCD->pResponse, pCD->ReceivePayload, pCD->ReceiveHeader->payloadLen);
        CL_SignalEvent(&pCD->hResponseEvent);
    }
#else
    {
        T_GPSC_ephemeris_assist* pAssist;
        RXN_FullEph_t fullEphemeris;
        RXN_FullEphem_u writeEphemeris;
        int index;
        char logMsg[RXN_CL_MAX_LOG_PATH];

        memset(&fullEphemeris, 0, sizeof(fullEphemeris));

        if(pEvt->eResult != RES_OK)
        {
            CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
                "NavcEvtAssistRepEph failed: %1.0f.", (double) pEvt->eResult);
            CL_LeaveBlock(&pCD->hLock);
            return;
        }

        pAssist = pEvt->tParams.tAssistEphemeris.ephemeris_assist;

        fullEphemeris.prn = pAssist->svid + 1; /* zero-based. */
        fullEphemeris.iode = (U16) pAssist->iode;
        fullEphemeris.crs = pAssist->ephem_crs;
        fullEphemeris.delta_n = pAssist->ephem_delta_n;
        fullEphemeris.cuc = pAssist->ephem_cuc;
        fullEphemeris.m0 = pAssist->ephem_m0;
        fullEphemeris.e = pAssist->ephem_e;
        fullEphemeris.sqrt_a = pAssist->ephem_a_power_half;
        fullEphemeris.omega0 = pAssist->ephem_omega_a0;
        fullEphemeris.cus = pAssist->ephem_cus;
        fullEphemeris.toe = pAssist->ephem_toe;
        fullEphemeris.cic = pAssist->ephem_cic;
        fullEphemeris.cis = pAssist->ephem_cis;
        fullEphemeris.i0 = pAssist->ephem_i0;
        fullEphemeris.w = pAssist->ephem_w;
        fullEphemeris.omega_dot = pAssist->ephem_omega_adot;
        fullEphemeris.crc = pAssist->ephem_crc;
        fullEphemeris.i_dot = pAssist->ephem_idot;
        fullEphemeris.CAOrPOnL2 = pAssist->ephem_code_on_l2; /* TODO: check! */
        fullEphemeris.ura = pAssist->ephem_ura;
        fullEphemeris.health = pAssist->ephem_svhealth;
        fullEphemeris.TGD = pAssist->ephem_tgd;
        fullEphemeris.iodc = pAssist->ephem_iodc;
        fullEphemeris.toc = pAssist->ephem_toc;
        fullEphemeris.af2 = pAssist->ephem_af2;
        fullEphemeris.af1 = pAssist->ephem_af1;
        fullEphemeris.af0 = pAssist->ephem_af0;
        fullEphemeris.gps_week = pAssist->ephem_week;
        fullEphemeris.AODO = 0; /* TODO: Not in pAssist. */
        fullEphemeris.L2PData = 0; /* TODO: Not in pAssist. */
        fullEphemeris.ephem_fit = 0; /* TODO: Not in pAssist. Set to 0 hours. */
        fullEphemeris.ure = UREfromURA(pAssist->ephem_ura); /* TODO: Is this ok? */

        CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE01,
            "NavcEvtAssistRepEph: prn: %1.0f.", (double) fullEphemeris.prn);

        /* Check if there is any data available for the PRN.*/
        if(fullEphemeris.prn == 0 || fullEphemeris.prn > RXN_CONSTANT_NUM_PRNS)
        {
            /* No data for the PRN.*/
            CL_LeaveBlock(&pCD->hLock);  
            return;
        }

        /* Check if the TOE or IODC value has changed.*/

        /* The arrays are zero-based i.e. PRN 1 is the first element. */
        index = fullEphemeris.prn - 1;

        sprintf(logMsg, "cic %d, crc %d, cuc %d, cis %d, crs %d, cus %d",
            fullEphemeris.cic, fullEphemeris.crc, fullEphemeris.cuc, fullEphemeris.cis, fullEphemeris.crs, fullEphemeris.cus);
        CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
            "NavcEvtAssistRepEph: %s.", logMsg);

        if(fullEphemeris.cic == 0 && fullEphemeris.crc == 0 && fullEphemeris.cuc == 0 &&
            fullEphemeris.cis == 0 && fullEphemeris.crs == 0 && fullEphemeris.cus == 0)
        {
            // This is the chipset reflecting back our own injected ephemeris. We ignore it.
            CL_LeaveBlock(&pCD->hLock);
            return;
        }

        sprintf(logMsg, "last TOE %d, new TOE %d, last IODC %d, new IODC %d",
            gLastPRNTOEs[index], fullEphemeris.toe, gLastPRNIODCs[index], fullEphemeris.iodc);
        CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
            "NavcEvtAssistRepEph: %s.", logMsg);

        if(fullEphemeris.toe != gLastPRNTOEs[index] || fullEphemeris.iodc != gLastPRNIODCs[index])
        {
            /* Update both lastPRNTOEs[] and lastPRNIODCs[] for the PRN. */
            gLastPRNTOEs[index] = fullEphemeris.toe;
            gLastPRNIODCs[index] = fullEphemeris.iodc;

            /* Write changed BCE record. Monitor the return of
            * RXN_MSL_WriteEphemeris() if additional logging (in addition to
            * that supported within the MSL) is required.
            */
            CL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
                "NavcEvtAssistRepEph: Call RXN_MSL_WriteEphemeris."); 

            writeEphemeris.gpsPRNArr = &fullEphemeris;
            RXN_MSL_WriteEphemeris(writeEphemeris, 1, RXN_GPS_CONSTEL);
        }
    }
#endif  //#ifdef USE_MSL
    CL_LeaveBlock(&pCD->hLock);
}

static void NavcEvtAssistRepTime(CHIPSET_DEP* pCD)
{
    TNAVC_evt* pEvt = (TNAVC_evt*) pCD->ReceivePayload;

    CL_EnterBlock(&pCD->hLock);
#ifndef USE_MSL
    if(pCD->outstandingRequest == eRequestGetTime)
    {
        memcpy(pCD->pResponse, pCD->ReceivePayload, pCD->ReceiveHeader->payloadLen);
        CL_SignalEvent(&pCD->hResponseEvent); 
    }
#else
    {
        /* Parse message. */
        if(pEvt->eResult == RES_OK)
        {
            T_GPSC_time_assist* pGpsTime = &pEvt->tParams.tAssistTime;

            CL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
                "NavcEvtAssistRepTime: gps_week = %1.0f, Msec = %1.0f", pGpsTime->gps_week, pGpsTime->gps_msec);

            CL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
                "NavcEvtAssistRepTime: sub_ms = %1.0f, time_unc = %1.0f", pGpsTime->sub_ms, pGpsTime->time_unc);

            CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
                "NavcEvtAssistRepTime: time_accuracy = %1.0f", pGpsTime->time_accuracy);

            SetGPSTime(pGpsTime->gps_week, pGpsTime->gps_msec);
        }
    }
#endif  //#ifdef USE_MSL

    CL_LeaveBlock(&pCD->hLock);
}

static void NavcEvtAssistanceRequest(CHIPSET_DEP* pCD)
{
    T_GPSC_current_gps_time* pGpsTime;
    TNAVC_evt* pEvt = (TNAVC_evt*) pCD->ReceivePayload;

    CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
        "NavcEvtAssistanceRequest: NAVC_EVT_ASSISTANCE_REQUEST: %1.0f.", pEvt->eResult);

    if(pEvt->eResult != RES_OK)
    {
        /* TODO: log. */
        CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
            "NavcEvtAssistanceRequest: NAVC_EVT_ASSISTANCE_REQUEST: %1.0f discarded.", pEvt->eResult);
        return;
    }

    pGpsTime = &pEvt->tParams.tAssistReq.tGpsTime;

    CL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
        "NavcEvtAssistanceRequest: gps_week = %1.0f, Msec = %1.0f", pGpsTime->gps_week , pGpsTime->Msec);
    CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
        "NavcEvtAssistanceRequest: Uncertainty = %1.0f", pGpsTime->Uncertainty);

#ifdef USE_MSL
    SetGPSTime(pGpsTime->gps_week, pGpsTime->Msec);
#endif

    /* Call RXN_CL_GetAssistance to refresh available EE. */
    RXN_CL_GetAssistance((U08)(-1), (U32)(-1), 0);
    CL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
        "NavcEvtAssistanceRequest: Call RXN_CL_GetAssistance.");

    /* Send Assistance complete. */
    NavcCmdInjectAssistanceComplete(pCD);
}

static void ProcessMessage(CHIPSET_DEP* pCD)
{
    U08 i;

    if (pCD->ReceiveHeader->msgClass != NAVL_CLASS_GPS)
    {
        /* Currently unhandled. */
        CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
            "ProcessMessage: Message currently unhandled.: %1.0f.", pCD->ReceiveHeader->msgClass);
        return;
    }

    for (i = 0; i < sizeof (gGpsEvtTable) / sizeof (gGpsEvtTable[0]); i++)
    {
        if (gGpsEvtTable[i].opCode == pCD->ReceiveHeader->opCode)
        {
            CL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
                "ProcessMessage: Handler %1.0f found. evtOpcode: %1.0f.", i, pCD->ReceiveHeader->opCode);
            break;
        }
    }

    CL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
        "ProcessMessage  pEvt->eComplCmd = %1.0f found evtopcode ", i, pCD->ReceiveHeader->opCode);

    if (i == sizeof (gGpsEvtTable) / sizeof (gGpsEvtTable[0]))
    {
        CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
            "ProcessMessage: No handler found. evtOpcode: %1.0f.", pCD->ReceiveHeader->opCode);
        return; /* No handler found. */
    }
    gGpsEvtTable[i].handler(pCD);
}

static void SetState(CHIPSET_DEP* pCD, RECEIVE_STATE newState)
{
    pCD->ReceiveState = newState;
}

static void ProcessFragment(CHIPSET_DEP* pCD, U08* pData, int bytesRead)
{
    int outstanding = 0; /* Number of bytes to complete payload. */

    while (bytesRead)
    {
        switch (pCD->ReceiveState)
        {
        case RECV_AWAITING_SYNC_1:
            switch (*pData)
            {
            case HOSTSOCKET_MESSAGE_SYNC_START_1:
                *pCD->ReceivePtr++ = *pData;
                SetState(pCD, RECV_AWAITING_SYNC_2);
                break;
            default:
                pCD->ReceivePtr = pCD->ReceiveMessage;
                break;
            }
            break;
        case RECV_AWAITING_SYNC_2:
            switch (*pData)
            {
            case HOSTSOCKET_MESSAGE_SYNC_START_2:
                *pCD->ReceivePtr++ = *pData;
                SetState(pCD, RECV_AWAITING_SYNC_3);
                break;
            default:
                pCD->ReceivePtr = pCD->ReceiveMessage;
                SetState(pCD, RECV_AWAITING_SYNC_1);
                break;
            }
            break;
        case RECV_AWAITING_SYNC_3:
            switch (*pData)
            {
            case HOSTSOCKET_MESSAGE_SYNC_START_3:
                *pCD->ReceivePtr++ = *pData;
                SetState(pCD, RECV_AWAITING_SYNC_4);
                break;
            default:
                pCD->ReceivePtr = pCD->ReceiveMessage;
                SetState(pCD, RECV_AWAITING_SYNC_1);
                break;
            }
            break;
        case RECV_AWAITING_SYNC_4:
            switch (*pData)
            {
            case HOSTSOCKET_MESSAGE_SYNC_START_4:
                *pCD->ReceivePtr++ = *pData;
                SetState(pCD, RECV_COLLECTING_HEADER);
                break;
            default:
                pCD->ReceivePtr = pCD->ReceiveMessage;
                SetState(pCD, RECV_AWAITING_SYNC_1);
                break;
            }
            break;
        case RECV_COLLECTING_HEADER:
            *pCD->ReceivePtr++ = *pData;
            if (pCD->ReceivePtr == pCD->ReceivePayload) /* Complete header? */
            {
                if (pCD->ReceiveHeader->payloadLen == 0)
                {
                    ProcessMessage(pCD);
                    pCD->ReceivePtr = pCD->ReceiveMessage;
                    SetState(pCD, RECV_AWAITING_SYNC_1);
                }
                else if (pCD->ReceiveHeader->payloadLen > MAX_PAYLOAD) /* Invalid header? */
                {
                    pCD->ReceivePtr = pCD->ReceiveMessage;
                    SetState(pCD, RECV_AWAITING_SYNC_1);
                }
                else
                {
                    SetState(pCD, RECV_COLLECTING_PAYLOAD);
                }
            }
            break;
        case RECV_COLLECTING_PAYLOAD:
            /* Optimize here to read rest of fragment up to payload end in one
            * memcpy rather than 1 byte at a time.
            */
            outstanding = (int) (pCD->ReceivePayload + pCD->ReceiveHeader->payloadLen - pCD->ReceivePtr);

            if (bytesRead >= outstanding)
            {
                memcpy(pCD->ReceivePtr, pData, outstanding);
                pData += outstanding;
                bytesRead -= outstanding;
                pCD->ReceivePtr = pCD->ReceiveMessage;
                SetState(pCD, RECV_AWAITING_SYNC_1);
                ProcessMessage(pCD);
                continue;
            }
            else
            {
                memcpy(pCD->ReceivePtr, pData, bytesRead);
                pCD->ReceivePtr += bytesRead;
                pData += bytesRead;
                bytesRead = 0;
                continue;
            }
            break;
        }

        pData++;
        bytesRead--;
    }
}

static void* WorkerThread(void* pArg)
{
    CHIPSET_DEP* pCD = (CHIPSET_DEP*) pArg;
    fd_set readSet;
    int rc;
    int maxFds;

    while (!pCD->exitNow)
    {
        if(!mConnected)
        {
            ManageConnection(pCD);
        }

        maxFds = SetFdSet(pCD, &readSet);
        rc = select(maxFds, &readSet, NULL, NULL, mpTimeout);

        if (rc < 0)
        {
            CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
                "select() failed, rc < 0! %s.", CL_GetOSErrorString(CL_GetOSError()));

            DisconnectNavc(pCD->hSocket);   
            /* Wait for 1 second to avoid a tight loop when any socket errors occur. */
            CL_Sleep(1000);
            continue;
        }
        else if (rc == 0)
        {
            CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
                "select() timeout reached!");
            continue;
        }
        else
        {
            /* Message fragment received from GPS server. */
            if (FD_ISSET(pCD->hSocket, &readSet))
            {
                int bytesRead = recv(pCD->hSocket, pCD->RawReadBuffer, sizeof (pCD->RawReadBuffer), 0);
                if (bytesRead < 0)
                {
#ifdef _MSC_VER
                    CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01, 
                        "recv() failed, bytesRead < 0! %s.", CL_GetOSErrorString(CL_GetOSError()));	
#else
                    /* Error. Function probably interrupted by a caught signal. */
                    if (errno == EINTR)
                    {
                        //printf("recv() returned %d after receiving interrupt\n", bytesRead);
                        CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
                            "WorkerThread: recv() returned %1.0f after receiving interrupt.", bytesRead);
                    }
                    else
                    {
                        //printf("recv() returned %d, errno = %d\n", bytesRead, errno);
                        CL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
                            "WorkerThread: recv() returned %1.0f, errno = %1.0f.", bytesRead, errno);
                    }
#endif
                    DisconnectNavc(pCD->hSocket);
                    /* Wait for 1 second to avoid a tight loop when any socket errors occur. */
                    CL_Sleep(1000);
                    continue;
                }
                else if (bytesRead == 0)
                {
                    CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
                        "WorkerThread: recv() failed, rc = 0, server has performed orderly shutdown!");   

                    DisconnectNavc(pCD->hSocket);
                    continue;
                }
                else
                {
                    //CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
                    //    "WorkerThread: ProccessFragment.");
                    ProcessFragment(pCD, pCD->RawReadBuffer, bytesRead);
                }
            }
            else
            {
                /* Should never get here. */
                CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
                    "WorkerThread: FDD_ISSET() failed!");
                break;
            } //  if (FD_ISSET(pCD->hSocket, &readSet))
        } // if (rc < 0)
    } // while (...)

    CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
        "WorkerThread: exiting!");
    return NULL;
}

void StopWorkerThread(CHIPSET_DEP* pCD)
{
    CL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE01,
            "Stopping worker thread.");
    pCD->exitNow = TRUE;
    DisconnectNavc(pCD->hSocket);
}

/* 
* \brief 
* Aborts an outstanding fix request.
* 
* \param pCD 
* [In] Pointer to chipset-dependent instance.
*
* RXN_SUCCESS on success.
* RXN_ALLOCATION_ERROR on failure to allocate message request.
* RXN_FAIL otherwise.
*/
static RXN_CL_ERROR_CODE StopFixData(CHIPSET_DEP* pCD)
{
    if (!NavcCmdStopFix(pCD))
    {
        return RXN_FAIL;
    }

    return RXN_SUCCESS;
}

/* 
* \brief 
* Writes ephemeris for UP TO 15 PRNS to the chipset.
* 
* \param pCD 
* [In] Pointer to chipset-dependent instance.
* \param ppGroup 
* [In] Pointer to array of ephemeris data pointers to be written.
* \param groupCount 
* [In] number of valid ephemeris in group.
*
* \returns 
* RXN_SUCCESS on success.
* RXN_ALLOCATION_ERROR on failure to allocate message request.
* RXN_CL_BUSY_ERROR if a request is currently outstanding.
* RXN_FAIL otherwise.
*/
static RXN_CL_ERROR_CODE Write15Ephemeris(CHIPSET_DEP* pCD, RXN_FullEph_t** ppGroup, U08 groupCount)
{
    if (!NavcCmdInject15Ephemeris(pCD, ppGroup, groupCount))
    {
        {
            CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
                "Write15Ephemeris: NavcCmdInject15Ephemeris failed.");
        }
        return RXN_FAIL;
    }
    else
    {
        CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
            "Write15Ephemeris: NavcCmdInject15Ephemeris success.");
    }
    return RXN_SUCCESS;
}

/* 
* \brief 
* Sets reference position assistance within the chipset.
* 
* \param pCD 
* [In] Pointer to chipset-dependent instance.
* \param pRefLoc 
* [In] Pointer to reference location.
*
* \returns 
* RXN_SUCCESS on success.
* RXN_ALLOCATION_ERROR on failure to allocate message request.
* RXN_CL_BUSY_ERROR if a request is currently outstanding.
* RXN_FAIL otherwise.
*/
static RXN_CL_ERROR_CODE WriteReferencePosition(CHIPSET_DEP* pCD, RXN_RefLocation_t* pRefLoc)
{
    if (!NavcCmdInjectPosition(pCD, pRefLoc))
    {
        return RXN_FAIL;
    }

    return RXN_SUCCESS;
}

/* 
* \brief 
* Sets reference time assistance within the chipset.
* 
* \param pCD 
* [In] Pointer to chipset-dependent instance.
* \param pRefTime 
* [In] Pointer to reference time.
*
* \returns 
* RXN_SUCCESS on success.
* RXN_ALLOCATION_ERROR on failure to allocate message request.
* RXN_CL_BUSY_ERROR if a request is currently outstanding.
* RXN_FAIL otherwise.
*/
static RXN_CL_ERROR_CODE WriteReferenceTime(CHIPSET_DEP* pCD, RXN_RefTime_t* pRefTime)
{
    if (!NavcCmdInjectTime(pCD, pRefTime))
    {
        CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
            "WriteReferenceTime: NavcCmdInjectTime failed.");
        return RXN_FAIL;
    }
    else
    {
        CL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE01,
            "WriteReferenceTime: NavcCmdInjectTime success.");
    }


    return RXN_SUCCESS;
}

/* 
* \brief 
* Tells the chipset all assistance has been injected.
* 
* \param pCD 
* [In] Pointer to chipset-dependent instance.
*
* \returns 
* RXN_SUCCESS on success.
* RXN_ALLOCATION_ERROR on failure to allocate message request.
* RXN_CL_BUSY_ERROR if a request is currently outstanding.
* RXN_FAIL otherwise.
*/
static RXN_CL_ERROR_CODE WriteAssistanceComplete(CHIPSET_DEP* pCD)
{
    if (!NavcCmdInjectAssistanceComplete(pCD))
    {
        CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
            "WriteAssistanceComplete: NavcCmdInjectAssistanceComplete failed..");
        return RXN_FAIL;
    }

    return RXN_SUCCESS;
}

/* 
* \brief 
*  Delete the given assistance from chipset.
* 
* \param pCD 
* [In] Pointer to chipset-dependent instance.
* \param delAssistBitmap 
* [In] bitmap indicates which assistance to delete.
*
* \returns 
* RXN_SUCCESS on success.
* RXN_ALLOCATION_ERROR on failure to allocate message request.
* RXN_CL_BUSY_ERROR if a request is currently outstanding.
* RXN_FAIL otherwise.
*/
static RXN_CL_ERROR_CODE DeleteAssistance(CHIPSET_DEP* pCD, U08 delAssistBitmap)
{
    if (!NavcCmdDeleteAssistance(pCD, delAssistBitmap))
    {
        return RXN_FAIL;
    }

    return RXN_SUCCESS;
}

/* CL_Get_Chipset must be implemented to provide a label to CL consumers
* even before the CL is initiated. */
U16 CL_GetChipset(char chipset[RXN_CL_CHIP_MAX_STR_LEN])
{
    strcpy(chipset, CHIP_LABEL);

    return RXN_SUCCESS;
}

U16 CL_SetFcns(CL_instance_t* pInstance)
{
    /* Set pointers only to those fcns that will be supported by the chipset. */
    pInstance->CL_Fcns.Initialize = CL_Initialize;
    pInstance->CL_Fcns.Uninitialize = CL_Uninitialize;
    pInstance->CL_Fcns.GetChipsetSupportVersion = CL_GetChipsetSupportVersion;
    pInstance->CL_Fcns.Work = CL_Work;
    pInstance->CL_Fcns.Restart = CL_Restart;
    pInstance->CL_Fcns.SetRefPosTime = CL_SetRefLocTime;
    pInstance->CL_Fcns.ReadEphemeris = CL_ReadEphemeris;
    pInstance->CL_Fcns.WriteEphemeris = CL_WriteEphemeris;
    pInstance->CL_Fcns.GetFixData = CL_GetFixData;
    pInstance->CL_Fcns.GetGPSRcvrTime = CL_GetGPSRcvrTime;
    return RXN_SUCCESS;
}

U16 CL_Initialize(CL_Chipset_Attrib_t* pAttrib, U16 chipsetVer, char config[RXN_CL_CONFIG_MAX_STR_LEN])
{
    CHIPSET_DEP* pCD = NULL;

    /* We'll just use the global instance for now. */
    pCD = &g_ChipsetDep;
    pAttrib->CL_Port_Hdl = (void*) pCD;

    /* Get the server IP address as a dotted decimal string. */
    if (CL_GetParamString(config, "ServerIP:", pCD->ClConfig.serverIpAddress) != RXN_SUCCESS)
    {
        CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE03,
            "CL_Initialize: Unable to get server IP adress within the config string provided.");

        return RXN_CL_CONFIG_FORMAT_ERR;
    }

    {
        /* Get optional MsBased parameter. If present and non-zero then we request fixes MS-Based fixes 
        * rather than MS-Autonomous. MS-Based requests mean that the TI stack will request assistance from
        * a previously registered assistance server e.g. our integration app. This is really
        * a test option which allows the EVK running on PC to verify the effectiveness of the integration
        * app on the TI stack, both of which are running on the Blaze. In this case the EVK would inject no assistance
        * but simply record TTFFs.
        */
        S64 param;
        pCD->ClConfig.msBased = FALSE;
        if (CL_GetParamInt(config, "MsBased:", 10, &param) == RXN_SUCCESS)
        {
            if (param != 0)
            {
                pCD->ClConfig.msBased = TRUE;
            }
        }
    }

    {
        S64 param;
        pCD->ClConfig.updateBCE = TRUE;
        /* Get optional UpdateBCE parameter. If present and non-zero, we will register for BCE updates. */
        if (CL_GetParamInt(config, "UpdateBCE:", 10, &param) == RXN_SUCCESS)
        {
            if (param == 0)
            {
                pCD->ClConfig.updateBCE = FALSE;
            }
        }
    }

    {
        S64 param;
        pCD->ClConfig.regGPSTime = TRUE;
        /* Get optional regGPSTime parameter. If present and non-zero, we will register for GPS time updates. 
        * NAVC will provide GPS time regularly.
        */
        if (CL_GetParamInt(config, "RegGPSTime:", 10, &param) == RXN_SUCCESS)
        {
            if (param == 0)
            {
                pCD->ClConfig.regGPSTime = FALSE;
            }
        }
    }

    {
        S64 param;

        pCD->ClConfig.useUNIX = TRUE;
		
		//pCD->ClConfig.useUNIX = FALSE;
        /* Get optional useUNIX parameter. If present and non-zero, we will use UNIX domain sockets
        *  to connect to NAVC
        */
        /*if (CL_GetParamInt(config, "useUNIX:", 10, &param) == RXN_SUCCESS)
        {
            if (param != 0)
            {
                pCD->ClConfig.useUNIX = TRUE;
            }
        }*/
    }

    if (pCD->state != eUninitialized)
    {
        return RXN_FAIL;
    }

    if(InitializeConnection(pCD) != RXN_SUCCESS)
    {
        /* Failed to initialize. Cleanup. */
        pCD->state = eUninitialized;

        if (pCD->hWorkerThread)
        {
            StopWorkerThread(pCD);
        }

#ifdef _MSC_VER  
        WSACleanup();        
#endif

        pCD->outstandingRequest = eRequestNone;

        CL_UninitBlock(&pCD->hLock);

        return RXN_FAIL;
    }

    return RXN_SUCCESS;
}

U16 CL_Uninitialize(CL_Chipset_Attrib_t* pAttrib)
{
    CHIPSET_DEP* pCD = (CHIPSET_DEP*) pAttrib->CL_Port_Hdl;

    if(pCD->state == eFixReporting)
    {
        /* Stop any outstanding fixes. */
        if (RXN_SUCCESS != StopFixData(pCD))
        {
            /* TODO: log. */
        }

        /* In any case. */
        /* Clear the current fix data. These values cause it to be rejected by EVK. */
        CL_EnterBlock(&pCD->hLock);
        pCD->fixData.fixDimensions = 0;
        pCD->fixData.numPRN = 0;
        pCD->fixData.HDOP = 1000.0;

        pCD->fixError = FALSE;

        pCD->state = eStarted;

        CL_LeaveBlock(&pCD->hLock);

        /* Must wait for MCP FSM to get to correct state. */
        CL_Sleep(2000);
    }

    pCD->state = eUninitialized;

    if (pCD->hWorkerThread)
    {
        StopWorkerThread(pCD);
    }

    CL_UninitEvent(&pCD->hResponseEvent);
    CL_UninitBlock(&pCD->hLock);    
    pCD->outstandingRequest = eRequestNone; 
    pCD->restartType = RXN_CL_NO_RESTART;

#ifdef _MSC_VER
    WSACleanup();
#endif

    return RXN_SUCCESS;
}

U16 CL_GetChipsetSupportVersion(CL_Chipset_Attrib_t* pAttrib, char version[RXN_CONSTANT_VERSION_STRING_LENGTH])
{
    CHIPSET_DEP* pCD = (CHIPSET_DEP*) pAttrib->CL_Port_Hdl;
    RXN_CL_ERROR_CODE rCode = RXN_FAIL;

    CL_EnterBlock(&pCD->hLock);

    if (pCD->outstandingRequest != eRequestNone)
    {
        /* Should never occur if all chiplib APIs are invoked from same thread. */
        CL_LeaveBlock(&pCD->hLock);
        return RXN_CL_BUSY_ERROR;
    }

    pCD->outstandingRequest = eRequestGetVersion;

    if (!NavcCmdGetVersion(pCD))
    {
        CL_LeaveBlock(&pCD->hLock);
        return RXN_FAIL;
    }

    CL_LeaveBlock(&pCD->hLock);

    /* Now wait for a response. */
    if(!CL_WaitForEvent(&pCD->hResponseEvent))
    {
        /* Should never happen */
        return RXN_FAIL;
    }

    /* Response received. */

    CL_EnterBlock(&pCD->hLock);

    /* Parse message. */
    {
        TNAVC_evt *pEvt = (TNAVC_evt *) (pCD->pResponse);
        if (pEvt->eResult == RES_OK)
        {
            /* Format the return string. */

            snprintf(
                version,
                RXN_CONSTANT_VERSION_STRING_LENGTH,
                "HW %d.%d SW %d.%d.%d.%d %d-%d-%d",
                pEvt->tParams.tVerResponse.hw_major,
                pEvt->tParams.tVerResponse.hw_minor,
                pEvt->tParams.tVerResponse.sw_major,
                pEvt->tParams.tVerResponse.sw_minor,
                pEvt->tParams.tVerResponse.sw_subminor1,
                pEvt->tParams.tVerResponse.sw_subminor2,
                pEvt->tParams.tVerResponse.sw_year,
                pEvt->tParams.tVerResponse.sw_month,
                pEvt->tParams.tVerResponse.sw_day
                );

            version[RXN_CONSTANT_VERSION_STRING_LENGTH - 1] = '\0'; /* Just in case we're on the limit. */

            rCode = RXN_SUCCESS;
        }

        /* Done with response so discard it. */
        pCD->outstandingRequest = eRequestNone;

        CL_LeaveBlock(&pCD->hLock);
    }

    return rCode;
}

U16 CL_Work(CL_Chipset_Attrib_t* pAttrib)
{
    return RXN_SUCCESS;
}

U16 CL_Restart(CL_Chipset_Attrib_t* pAttrib, RXN_CL_Restarts_t restartType,
               RXN_RefLocation_t* pRefLoc, RXN_RefTime_t* pRefTime)
{
    CHIPSET_DEP* pCD = (CHIPSET_DEP*) pAttrib->CL_Port_Hdl;
    RXN_CL_ERROR_CODE rCode = RXN_FAIL;

    U08 delAssistBitmap = 0x0;

    CL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
        "CL_Restart is called.");

    if (restartType == RXN_CL_COLD_RESTART)
    {
        /* Delete all assistance. */
        delAssistBitmap = /* GPSC_DEL_AIDING_TIME | */
            GPSC_DEL_AIDING_POSITION |
            GPSC_DEL_AIDING_EPHEMERIS |
            GPSC_DEL_AIDING_ALMANAC |
            GPSC_DEL_AIDING_IONO_UTC |
            GPSC_DEL_AIDING_SVHEALTH;

        /* Best effort request. No response expected. */
        rCode = DeleteAssistance(pCD, delAssistBitmap);
        CL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
            "CL_Restart: restartType == RXN_CL_COLD_RESTART.");
    }
    else if (restartType == RXN_CL_WARM_RESTART)
    {
        /* Delete ephemeris only. */
        delAssistBitmap = GPSC_DEL_AIDING_EPHEMERIS;

        /* Best effort request. No response expected. */
        rCode = DeleteAssistance(pCD, delAssistBitmap);
        CL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
            "CL_Restart: restartType == RXN_CL_WARM_RESTART.");
    }
    else if (restartType == RXN_CL_HOT_RESTART)
    {
        /* Delete nothing. */
        rCode = RXN_SUCCESS;
        CL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
            "CL_Restart: restartType == RXN_CL_HOT_RESTART.");
    }
    else
    {
        /* Illegal restart type. */
        rCode = RXN_CL_INVALID_RESTART_TYPE_ERR;
        CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
            "CL_Restart: restartType == RXN_CL_INVALID_RESTART_TYPE_ERR.");
    }

    CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
        "CL_Restart is called. rCode = %1.0f.", rCode);

    if (RXN_SUCCESS != rCode)
    {
        CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
            "CL_Restart: rCode = %1.0f", rCode);
        return rCode;
    }

    if (pRefLoc)
    {
        CL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
            "CL_Restart WriteReferencePosition.");

        rCode = WriteReferencePosition(pCD, pRefLoc);
        if (RXN_SUCCESS != rCode)
        {
            CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
                "CL_Restart WriteReferencePosition failed. rCode = %1.0f.", rCode);
            return rCode;
        }
    }

    if (pRefTime)
    {
        CL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
            "CL_Restart WriteReferenceTime.");
        rCode = WriteReferenceTime(pCD, pRefTime);
        if (RXN_SUCCESS != rCode)
        {
            CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
                "CL_Restart: WriteReferenceTime failed. rCode = %1.0f", rCode);
            return rCode;
        }
        else
        {
            CL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
                "CL_Restart: WriteReferenceTime success.");
        }
    }

    if (pRefLoc || pRefTime)
    {
        CL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
            "CL_Restart WriteAssistanceComplete.");
        rCode = WriteAssistanceComplete(pCD);
        CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
            "CL_Restart: WriteAssistanceComplete. rCode = %1.0f", rCode);

    }

    /* All went well! */
    return rCode;
}

U16 CL_SetRefLocTime(CL_Chipset_Attrib_t* pAttrib, RXN_RefLocation_t* pRefLoc, RXN_RefTime_t* pRefTime)
{
    CHIPSET_DEP* pCD = (CHIPSET_DEP*) pAttrib->CL_Port_Hdl;
    RXN_CL_ERROR_CODE rCode = RXN_SUCCESS;

    CL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE01,
        "CL_SetRefLocTime is called.");

    if (pRefLoc)
    {
        rCode = WriteReferencePosition(pCD, pRefLoc);
        if (RXN_SUCCESS != rCode)
        {
            return rCode;
        }
    }

    if (pRefTime)
    {
        rCode = WriteReferenceTime(pCD, pRefTime);
        if (RXN_SUCCESS != rCode)
        {
            CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
                "CL_SetRefLocTime: WriteReferenceTime failed. rCode = %1.0f.", rCode);
            return rCode;
        }
        else
        {
            CL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
                "CL_SetRefLocTime: WriteReferenceTime success.");
        }
    }

    if (pRefTime)
    {
        /*
        As part of time assistance injection, we also need to set TOW.
        TODO: there is a lack of determinism here. pRefTime was passed in, one or more
        synchronous calls are made which take some time, then we use pRefTime. However, pRefTime
        is now slightly stale. Does this matter?
        */
        rCode = NavcCmdSetTow(pCD, pRefTime) ? RXN_SUCCESS : RXN_FAIL;
    }

    rCode = WriteAssistanceComplete(pCD);

    return rCode;
}

U16 CL_ReadEphemeris(CL_Chipset_Attrib_t* pAttrib, RXN_MSL_NavDataList_t* pNavDataList, RXN_constel_t constel)
{
    CHIPSET_DEP* pCD = (CHIPSET_DEP*) pAttrib->CL_Port_Hdl;
    RXN_CL_ERROR_CODE rCode = RXN_FAIL;

    if (constel != RXN_GPS_CONSTEL)
    {
        CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
            "CL_ReadEphemeris: Constellation %d not supported.", constel);
    }

    CL_EnterBlock(&pCD->hLock);

    if (pCD->outstandingRequest != eRequestNone)
    {
        /* Should never occur if all chiplib APIs are invoked from same thread. */
        CL_LeaveBlock(&pCD->hLock);
        return RXN_CL_BUSY_ERROR;
    }

    pCD->outstandingRequest = eRequestGetEphemeris;

    if (!NavcCmdGetAssistEph(pCD))
    {
        CL_LeaveBlock(&pCD->hLock);
        return RXN_FAIL;
    }

    CL_LeaveBlock(&pCD->hLock);

    /* Now wait for a response. */
    if(!CL_WaitForEvent(&pCD->hResponseEvent))
    {
        /* Should never happen */
        return RXN_FAIL;
    }

    /* Response received. */
    CL_EnterBlock(&pCD->hLock);

    /* Parse message. */
    {
        TNAVC_evt* pEvt = (TNAVC_evt *) pCD->pResponse;

        if (pEvt->eResult == RES_OK)
        {
            T_GPSC_assistance_inject_union_report* pUnionReport = (T_GPSC_assistance_inject_union_report*) & pEvt->tParams;
            T_GPSC_ephemeris_assist_rep* pReport = &pUnionReport->ephemeris_assist_rep;

            U08 i = 0;

            for (i = 0; i < pNavDataList->numEntries; i++)
            {
                T_GPSC_ephemeris_assist* pAssist = &pReport->ephemeris_assist[i];
                RXN_FullEph_t* pEphemeris = &pNavDataList->ephList.gps[i];

                pEphemeris->prn = pAssist->svid + 1; /* svid is zero-based */
                pEphemeris->iode = (U16) pAssist->iode;
                pEphemeris->crs = pAssist->ephem_crs;
                pEphemeris->delta_n = pAssist->ephem_delta_n;
                pEphemeris->cuc = pAssist->ephem_cuc;
                pEphemeris->m0 = pAssist->ephem_m0;
                pEphemeris->e = pAssist->ephem_e;
                pEphemeris->sqrt_a = pAssist->ephem_a_power_half;
                pEphemeris->omega0 = pAssist->ephem_omega_a0;
                pEphemeris->cus = pAssist->ephem_cus;
                pEphemeris->toe = pAssist->ephem_toe;
                pEphemeris->cic = pAssist->ephem_cic;
                pEphemeris->cis = pAssist->ephem_cis;
                pEphemeris->i0 = pAssist->ephem_i0;
                pEphemeris->w = pAssist->ephem_w;
                pEphemeris->omega_dot = pAssist->ephem_omega_adot;
                pEphemeris->crc = pAssist->ephem_crc;
                pEphemeris->i_dot = pAssist->ephem_idot;
                pEphemeris->CAOrPOnL2 = pAssist->ephem_code_on_l2; /* TODO: check! */
                pEphemeris->ura = pAssist->ephem_ura;
                pEphemeris->health = pAssist->ephem_svhealth;
                pEphemeris->TGD = pAssist->ephem_tgd;
                pEphemeris->iodc = pAssist->ephem_iodc;
                pEphemeris->toc = pAssist->ephem_toc;
                pEphemeris->af2 = pAssist->ephem_af2;
                pEphemeris->af1 = pAssist->ephem_af1;
                pEphemeris->af0 = pAssist->ephem_af0;
                pEphemeris->gps_week = pAssist->ephem_week;
                pEphemeris->AODO = 0; /* TODO: Not in pAssist. */
                pEphemeris->L2PData = 0; /* TODO: Not in pAssist. */
                pEphemeris->ephem_fit = 0; /* TODO: Not in pAssist. Set to 0 hours. */
                pEphemeris->ure = CL_UREfromURA(pAssist->ephem_ura); /* TODO: Is this ok? */
            }
            rCode = RXN_SUCCESS;
        }

        pCD->outstandingRequest = eRequestNone;

        CL_LeaveBlock(&pCD->hLock);
    }

    return rCode;
}

U16 CL_GetGPSRcvrTime(CL_Chipset_Attrib_t* pAttrib, U16* pRcvrWeekNum, U32* pRcvrTOW)
{
    CHIPSET_DEP* pCD = (CHIPSET_DEP*) pAttrib->CL_Port_Hdl;
    RXN_CL_ERROR_CODE rCode = RXN_FAIL;

    CL_EnterBlock(&pCD->hLock);

    if (pCD->outstandingRequest != eRequestNone)
    {
        /* Should never occur if all chiplib APIs are invoked from same thread. */
        CL_LeaveBlock(&pCD->hLock);
        return RXN_CL_BUSY_ERROR;
    }

    pCD->outstandingRequest = eRequestGetTime;

    if (!NavcCmdGetAssistTime(pCD))
    {
        CL_LeaveBlock(&pCD->hLock);
        return RXN_FAIL;
    }

    CL_LeaveBlock(&pCD->hLock);

    /* Now wait for a response. */
    if(!CL_WaitForEvent(&pCD->hResponseEvent))
    {
        /* Should never happen */
        return RXN_FAIL;
    }

    /* Response received. */
    CL_EnterBlock(&pCD->hLock);

    /* Parse message. */
    {
        TNAVC_evt* pEvt = (TNAVC_evt *) pCD->pResponse;

        if (pEvt->eResult == RES_OK)
        {
            T_GPSC_assistance_inject_union_report* pUnionReport = (T_GPSC_assistance_inject_union_report*) & pEvt->tParams;
            T_GPSC_time_assist* pReport = &pUnionReport->time_assist;

            *pRcvrWeekNum = pReport->gps_week;
            *pRcvrTOW = pReport->gps_msec / 1000;

            CL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
                "CL_GetGPSRcvrTime gps_week = %1.0f, gps_msec = %1.0f.", pReport->gps_week, pReport->gps_msec); 

            rCode = RXN_SUCCESS;
        }

        /* Done with response so discard it. */
        pCD->outstandingRequest = eRequestNone;

        CL_LeaveBlock(&pCD->hLock);
    }

    return rCode;
}

U16 CL_WriteEphemeris(CL_Chipset_Attrib_t* pAttrib, RXN_MSL_NavDataList_t* pNavDataList, RXN_constel_t constel)
{
    CHIPSET_DEP* pCD = (CHIPSET_DEP*) pAttrib->CL_Port_Hdl;
    U08 i = 0;
    RXN_CL_ERROR_CODE rCode = RXN_SUCCESS;

    RXN_FullEph_t * group1[N_SV_15] = {NULL};
    RXN_FullEph_t * group2[N_SV_15] = {NULL};
    RXN_FullEph_t * group3[N_SV_15] = {NULL};
    U08 group1Count = 0;
    U08 group2Count = 0;
    U08 group3Count = 0;

    /* Split valid PRNs in groups of up to 15. */
    CL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
        "CL_WriteEphemeris is called.");

    if (constel != RXN_GPS_CONSTEL)
    {
        CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01,
            "CL_WriteEphemeris: Constellation %d not supported.", constel);
    }

    for (i = 0; i < pNavDataList->numEntries; i++)
    {
        if (pNavDataList->ephList.gps[i].prn == 0)
        {
            // Do not inject for this PRN.
            continue;
        }

        if (group1Count < N_SV_15)
        {
            group1[group1Count++] = &pNavDataList->ephList.gps[i];
        }
        else if (group2Count < N_SV_15)
        {
            group2[group2Count++] = &pNavDataList->ephList.gps[i];
        }
        else
        {
            group3[group3Count++] = &pNavDataList->ephList.gps[i];
        }
    }

    if (group1Count > 0)
    {
        rCode = Write15Ephemeris(pCD, group1, group1Count);
        if (RXN_SUCCESS != rCode)
        {
            /* We don't expect a failure so abandon remaining PRNs as well. */
            CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
                "CL_WriteEphemeris: group1Count Write15Ephemeris failed. rCode = %1.0f", rCode);
            return rCode;
        }
        else
        {
            CL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE01,
                "CL_WriteEphemeris: group1Count Write15Ephemeris success.");
        }
    }

    if (group2Count > 0)
    {
        rCode = Write15Ephemeris(pCD, group2, group2Count);
        if (RXN_SUCCESS != rCode)
        {
            /* We don't expect a failure so abandon remaining PRNs as well. */
            CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
                "CL_WriteEphemeris: group2Count Write15Ephemeris failed. rCode = %1.0f", rCode);
            return rCode;
        }
        else
        {
            CL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE01,
                "CL_WriteEphemeris: group2Count Write15Ephemeris success.");
        }
    }

    if (group3Count > 0)
    {
        rCode = Write15Ephemeris(pCD, group3, group3Count);
        if (RXN_SUCCESS != rCode)
        {
            /* We don't expect a failure so abandon remaining PRNs as well. */
            CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
                "CL_WriteEphemeris: group3Count Write15Ephemeris failed. rCode = %1.0f", rCode);
            return rCode;
        }
        else
        {
            CL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE01,
                "CL_WriteEphemeris: group2Count Write15Ephemeris success.");
        }
    }

    /* All went well! */
    rCode = WriteAssistanceComplete(pCD);

    CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
        "CL_WriteEphemeris: WriteAssistanceComplete. rCode = %1.0f", rCode);

    return rCode;
}

U16 CL_GetFixData(CL_Chipset_Attrib_t* pAttrib, RXN_CL_FixData_t* pFixData)
{
    CHIPSET_DEP* pCD = (CHIPSET_DEP*) pAttrib->CL_Port_Hdl;
    BOOL fixError;

    if (pCD->state == eStarted)
    {
        /* We have not initiated fix reporting yet. */
        pCD->state = eFixReporting;

        NavcCmdRequestFix(pCD); 
    }

    /* Respond immediately to request with current data.
    We may not have valid data yet. That's ok. EVK will poll us again.
    */
    CL_EnterBlock(&pCD->hLock);
    *pFixData = pCD->fixData;
    fixError = pCD->fixError;

    /* Invalidate fix so that EVK will not reread same value. Chipset will rewrite value asynchronously. */
    pCD->fixData.fixDimensions = 0;
    pCD->fixData.numPRN = 0;
    pCD->fixData.HDOP = 1000.0;
    pCD->fixError = FALSE;

    CL_LeaveBlock(&pCD->hLock);

    if (fixError)
    {
        //return RXN_FAIL;      // Don't care about fixError
        CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
            "CL_GetFixData: fixError");
    }

    if (pFixData->fixDimensions == 0)
    {
        /* No valid data yet. We have to sleep a little because EVK goes into a
        tight GetFixData() loop for first fix. After that it polls every 1 second.
        */
        CL_Sleep(200);
    }

    return RXN_SUCCESS;
}

static U08 UREfromURA(U08 URAIdx)
{
    switch(URAIdx)
    {
    case 0:
        return 2;
    case 1:
        return 3;
    case 2:
        return 4;
    case 3:
        return 6;
    case 4:
        return 9;
    case 5:
        return 13;
    case 6:
        return 24;
    case 7:
        return 48;
    case 8:
        return 96;
    case 9:
        return 192;
    default:
        return 255;
    }
}

static int SetSockaddr(CHIPSET_DEP* pCD)
{
    memset(&pCD->peer, 0, sizeof(pCD->peer));

    if (pCD->ClConfig.useUNIX)
    {
        CL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01, "Using domain sockets");
        struct sockaddr_un* addr = (struct sockaddr_un*)&pCD->peer;
        addr->sun_family = AF_UNIX;
        strcpy(addr->sun_path, MCPF_UNIX_PATH);
        return strlen(addr->sun_path) + sizeof(addr->sun_family);
    }
    else
    {
        CL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01, "Using tcp/ip sockets");
        struct sockaddr_in* addr = (struct sockaddr_in*)&pCD->peer;
        addr->sin_family = AF_INET;
        addr->sin_addr.s_addr = inet_addr(pCD->ClConfig.serverIpAddress);
        addr->sin_port = htons(MCPF_SOCKET_PORT);
        return sizeof(struct sockaddr_in);
    }
}

static BOOL ConnectToNavc(CHIPSET_DEP* pCD)
{
    pCD->hSocket = socket(pCD->ClConfig.useUNIX ? AF_UNIX : AF_INET, SOCK_STREAM, 0);

    if(pCD->hSocket == INVALID_SOCKET)
    {
        CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
            "socket() failed! Error: %s.", CL_GetOSErrorString(CL_GetOSError()));
        return FALSE;
    }

    int len = SetSockaddr(pCD);

    if(connect(pCD->hSocket, (struct sockaddr*) &pCD->peer, len) != 0)
    {
        CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
            "CL_Initialize: connect() failed: %s.", CL_GetOSErrorString(CL_GetOSError()));
        CL_CloseSckt(pCD->hSocket);
        return FALSE;
    }

    return TRUE;
}

static void ManageConnection(CHIPSET_DEP* pCD)
{
    U16 assistBitmap = 0;

    if(ConnectToNavc(pCD))
    {
        SetTimeout(0);	    /* Infinite wait required. */
        mConnected = TRUE;
        CL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE01,
            "CL_Initialize: NAVC connected.");
    }
    else
    {
        SetTimeout(NAVC_CONNECT_RETRY_TIMEOUT_S);
        mConnected = FALSE;
        CL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE01,
            "CL_Initialize: NAVC not connected!");

        return;
    }

    /* Register SAGPS as the service provider. */
    if (!NavcCmdSagpsProviderRegister(pCD)) 
    {
        CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
            "CL_Initialize: NavcCmdSagpsProviderRegister call failed.");
    }
    else
    {
        CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
            "CL_Initialize: NavcCmdSagpsProviderRegister success.");
    }

    /* This really just a ping to evoke a response from the GPSC server. */          
    NavcCmdGetVersion(pCD);

    assistBitmap = (pCD->ClConfig.updateBCE? NAVC_REG_EPH : 0) | (pCD->ClConfig.regGPSTime? NAVC_REG_GPSTIME : 0);
    if(assistBitmap > 0)
    {
        NavcCmdRegForAssist(pCD, assistBitmap);       
    }

    /* From now on we simply wait for messages from GPS server. */
    SetState(pCD, RECV_AWAITING_SYNC_1);
    pCD->ReceivePtr = pCD->ReceiveMessage;

    /* Give worker thread a chance to get underway before we send out 1s command. 
    * TODO: is there a better way.
    */
    CL_Sleep(200);

    pCD->state = eStarted;

    /* Clear the current fix data. These values cause it to be rejected by EVK. */
    pCD->fixData.fixDimensions = 0;
    pCD->fixData.numPRN = 0;
    pCD->fixData.HDOP = 1000.0;

    pCD->fixError = FALSE;
}

void SetTimeout(U08 seconds)
{
    if (seconds == 0)
    {
        mpTimeout = NULL;
    }
    else
    {
        mTimeout.tv_sec  = seconds;
        mTimeout.tv_usec = 0;        
        mpTimeout = &mTimeout;
    }
}

static int SetFdSet(CHIPSET_DEP* pCD, fd_set* readSet)
{
    int maxFds = 0;

    FD_ZERO(readSet);

    if(mConnected)
    {
        FD_SET(pCD->hSocket,readSet);
        maxFds = (int)(pCD->hSocket) + 1;
    }

    return maxFds;
}

static void DisconnectNavc(SOCKET sckt)
{
    mConnected = FALSE;                    
    SetTimeout(NAVC_CONNECT_RETRY_TIMEOUT_S);
    if (sckt != INVALID_SOCKET)
    {
        CL_CloseSckt(sckt);
        sckt = INVALID_SOCKET;
    }
}

static U16 InitializeConnection(CHIPSET_DEP* pCD)
{
    pCD->exitNow = FALSE;
    pCD->SendHeader = (MESSAGE_HEADER_T*) pCD->SendMessage;
    pCD->SendPayload = pCD->SendMessage + sizeof (MESSAGE_HEADER_T);
    pCD->ReceiveHeader = (MESSAGE_HEADER_T*) pCD->ReceiveMessage;
    pCD->ReceivePayload = pCD->ReceiveMessage + sizeof (MESSAGE_HEADER_T);
    pCD->restartType = RXN_CL_NO_RESTART;
    CL_InitBlock(&pCD->hLock);
    CL_InitEvent(&pCD->hResponseEvent);
    pCD->outstandingRequest = eRequestNone;

#ifdef _MSC_VER            
    if(!CL_StartWinsock())
    {
        return RXN_FAIL;
    }
#endif

    /* Connect to NAVC. If failed, we'll reconnect in WorkerThread. */
    ManageConnection(pCD);  

    if((pCD->hWorkerThread = CL_StartThread((void*) &WorkerThread, (void*) pCD)) == 0)
    {
        CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
            "Failed to start WorkerThread.");
        return RXN_FAIL;
    }

    return RXN_SUCCESS;
}
