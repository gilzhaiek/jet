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
#include "RXN_MSL_Common.h"
#include "RXN_MSL_Platform.h"
#include "RXN_MSL_Time.h"

/* External functions */
extern BOOL MSL_WaitForEvent(EVENT* event);
extern void MSL_ThreadInit();
extern void MSL_ThreadUninit();
extern void MSL_HandleSendFailure();
extern void MSL_CreateSNTPRequest(U08* ReqPkt);
extern U32 MSL_DecodeSNTPResponse(U08* RespPkt);

extern U16 MSL_CmdRequest(EVENT event);
extern BOOL MSL_CommandReceived(EVENT event);
extern void MSL_StopThread(THREAD thread);
extern U08 MSL_GetConstelConfig();

#ifdef ANDROID
extern BOOL MSL_SendAndroidSNTPTimeRequest();
extern BOOL MSL_SendDeviceIdRequest();
extern BOOL MSL_SendTerminationRequest();
extern BOOL MSL_SendAndroidUpdateSeedOrClkRequest(U08 type); 
#endif

#ifdef XYBRID
extern BOOL MSL_SendXybridMessage();
#endif

/* External variables */
extern MSL_Config_t gConfig;

/* Internal prototypes */
static void RXN_MsgSocketThread(void* pArg);
static BOOL IsEndOfLifeSeed(BOOL result);
static BOOL SendSNTPTimeRequest();
static BOOL SendUpdateSeedOrClkRequest(U08 type); 
static BOOL HandleCommand(EVENT event);
#ifndef ANDROID
static BOOL SendNativeSNTPTimeRequest();
static U16 SendNativeUpdateSeedOrClkRequest(U08 type); 
#endif

#define SNTP_RECV_TIMEOUT 10

static THREAD mThread;

void MSL_StartMessageThread()
{
    mThread = MSL_StartThread((void*)&RXN_MsgSocketThread);
}

U16 CmdRequestStopMsgSocketThread()
{
    U16 ret = MSL_CmdRequest(MSL_EXIT_MSG_SOCKET_THREAD_EVENT);
    MSL_Sleep(5000);
    MSL_StopThread(mThread);
    return ret;
}

U16 CmdRequestSNTPTime()
{
    return MSL_CmdRequest(MSL_SNTP_TIME_REQUEST_EVENT);
}

U16 CmdRequestSeed()
{
    return MSL_CmdRequest(MSL_UPDATE_SEED_REQUEST_EVENT);
}

U16 CmdRequestClockUpdate()
{
    return MSL_CmdRequest(MSL_UPDATE_CLK_REQUEST_EVENT);
}

static BOOL IsEndOfLifeSeed(BOOL result)
{
    if (result == RXN_MSL_EOL_ERR)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE06, 
            "RXN_MsgSocketThread: EOL message detected. No more messages will be processed. Stop the MSL logger.");

        /* Shutdown the MSL logger. */
        RXN_MSL_Log_UnInit();

        return TRUE;
    }
    return FALSE;
}

static BOOL HandleCommand(EVENT event)
{
    BOOL bResult = TRUE;
    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08, 
            "HandleCommand: event: %1.0f", (float)event);
    switch (event)
    {
    case MSL_UPDATE_CLK_REQUEST_EVENT:
        bResult = SendUpdateSeedOrClkRequest(MSL_REQ_TYPE_CLOCK);
        break;
    case MSL_UPDATE_SEED_REQUEST_EVENT:
        bResult = SendUpdateSeedOrClkRequest(MSL_REQ_TYPE_SEED);
        break;
    case MSL_SNTP_TIME_REQUEST_EVENT:
        bResult = SendSNTPTimeRequest();
        break;
#ifdef ANDROID
    case MSL_SEND_DEVICE_ID_REQUEST_EVENT:
        bResult = MSL_SendDeviceIdRequest();
        break;
    case MSL_RXNSERVICES_TERMINATION_EVENT:
        bResult = MSL_SendTerminationRequest();
        break;
#endif
    case MSL_EXIT_MSG_SOCKET_THREAD_EVENT:
        return FALSE;	/* exit */
        break;
#ifdef XYBRID
    case MSL_XYBRID_MESSAGE_EVENT:
        bResult = MSL_SendXybridMessage();
        break;
#endif
    default:
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08, 
            "RXN_HandleCommand: Unknown event: %1.0f", (float)event);
        break;
    }

    if (IsEndOfLifeSeed(bResult))
    {
        return FALSE;	/* exit */
    }

    if (!bResult)
    {
        MSL_HandleSendFailure();
    }

    return TRUE;
}

static BOOL MSL_HandleEvent(EVENT event)
{
    BOOL ret = TRUE;

    /* If platform specific handling successful, process command */
    if (MSL_CommandReceived(event))
    {
        /* Handle the command */
        ret = HandleCommand(event);
    }
    return ret;
}

static void RXN_MsgSocketThread(void* pArg)
{
    EVENT event;
    BOOL exitThread = FALSE;

    MSL_ThreadInit();

    /* Wait for an event and then handle it, exiting the thread if required */
    while (!exitThread)
    {
        if (MSL_WaitForEvent(&event))
        {
            exitThread = !MSL_HandleEvent(event);
            MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE03,
                "RXN_MsgSocketThread: Handled event %1.0f. MSL_HandleEvent returned: %1.0f.", event, exitThread);
        }
    }

    MSL_ThreadUninit();

    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE03,
        "RXN_MsgSocketThread: Message thread exiting.");
}

static BOOL SendSNTPTimeRequest()
{
#ifdef ANDROID
    return MSL_SendAndroidSNTPTimeRequest();
#else
    return SendNativeSNTPTimeRequest();
#endif
}

#ifndef ANDROID
static BOOL SendNativeSNTPTimeRequest()
{
    SOCKET sckt = 0;
    U08 ReqPkt[48];
    U08 RespPkt[48];
    U32 SNTPTime = 0;
    struct timeval timeout = { SNTP_RECV_TIMEOUT, 0 };
    fd_set read_set;

    if (MSL_ScktOpenUDP(gConfig.SNTP.host, gConfig.SNTP.port, &sckt) != RXN_SUCCESS)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
            "SendSNTPTimeRequest: Error opening UDP port.");
        return FALSE;
    }

    /* Set socket non-blocking */
    if (MSL_SetSocketNonBlocking(sckt) == RXN_FAIL)
    {
        return FALSE;
    }

    MSL_CreateSNTPRequest(ReqPkt);

    if (MSL_ScktWriteBytes(sckt, ReqPkt, 48, 500, NULL, 0) != RXN_SUCCESS)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
            "SendSNTPTimeRequest: Error sending SNTP request to SNTP server.");
        return FALSE;
    }

    /* Setup a BYTE array to contain response packet data. */
    memset((void*) RespPkt, 0, 48);

    FD_ZERO(&read_set);
    FD_SET(sckt, &read_set);

    /* Wait for socket to be readable and if there is a timeout or some other error, return error */
    if (select((int)sckt + 1, &read_set, NULL, NULL, &timeout) <= 0)
    {
        MSL_ScktClose(sckt);
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_WARNING, RXN_LOG_ZONE04, "Timed out waiting for SNTP reply.");
        return FALSE;
    }

    /* Read a response. */
    if (recv(sckt, RespPkt, 48, 0) == SOCKET_ERROR)
    {
        MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
            "SendSNTPTimeRequest:Response receive error. Errno: %s.", MSL_GetOSErrorString(MSL_GetOSError()));
        return FALSE;
    }

    SNTPTime = MSL_DecodeSNTPResponse(RespPkt);
    MSL_SetSNTPTime(SNTPTime);

    /* Close the socket and free resources. */
    if (MSL_ScktClose(sckt) != RXN_SUCCESS)
    {
        /* Log an error. */
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
            "SendSNTPTimeRequest: Error closing a socket.");
    }

    MSL_SetNextSystemEvent(MSL_EVENT_TIME_UPDATED);
    return TRUE;
}
#endif

static BOOL SendUpdateSeedOrClkRequest(U08 type)
{
#ifdef ANDROID
    return MSL_SendAndroidUpdateSeedOrClkRequest(type);
#else
    return (SendNativeUpdateSeedOrClkRequest(type) == RXN_SUCCESS);
#endif
}

#ifndef ANDROID
static U16 SendNativeUpdateSeedOrClkRequest(U08 type) 
{
    CHAR fullHost[MSL_MAX_HOST_STR_LEN]; /* Full host string - including index. */
    CHAR IP[MSL_SKT_ADDR_MAX_LEN]; /* IP Address string. */
    CHAR msg[MSL_SKT_RESP_MSG_MAX_LEN]; /* Request msg store. */
    U32 msgLen = 0; /* Len of the request msg. */
    U16 result = RXN_SUCCESS;

    /* socket element for incoming AND outgoing msgs. */
    SOCKET skt = 0;

    MSL_CreateHostName(fullHost, gConfig.net.host, gConfig.net.hostMaxIdx);
    result = MSL_ScktGetHostIP(fullHost, IP);

    if (result != RXN_SUCCESS)
    {
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE04,
            "SendUpdateSeedOrClkRequest: Error resolving an IP address. Error: %1.0f",
            (double) result);
        return RXN_FAIL;
    }

    /* Initialize a socket for comms using the host name developed. */
    result = MSL_ScktOpenTCP(IP, MSL_GetHostPort(), 5000, &skt);

    /* Handle Errors. */
    if (result != RXN_SUCCESS) {
        /* Log an error. */
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
            "SendUpdateSeedOrClkRequest: Error opening a socket for PGPS update. Error: %1.0f",
            (double) result);
        return RXN_FAIL;
    }

    /* Generate a request msg. Note: IP is ignored. */
    MSL_GenReqMsg(type, IP, msg, (U16 *)&msgLen, MSL_GetConstelConfig());

    /* Send the request msg. The IPAdd and port can be null as we will use a
    * connected socket. */
    result = MSL_ScktWriteBytes(skt, (U08*)msg, msgLen, 500, NULL, 0);

    /* Handle Errors. */
    if (result != RXN_SUCCESS) {
        /* Log an error. */
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
            "SendUpdateSeedOrClkRequest: Error writing a PGPS request msg through a socket. Error: %1.0f",
            (double) result);
        return RXN_FAIL;
    }

    memset(msg, 0, MSL_SKT_RESP_MSG_MAX_LEN);

    /* Init respMsgLen to contain the capacity of respMsg. */
    msgLen = MSL_SKT_RESP_MSG_MAX_LEN;

    /* Read a response. respMsgLen will be set to the number of bytes read. */
    result = MSL_ScktReadBytes(skt, (U08*)msg, MSL_SKT_RESP_MSG_MAX_LEN, 15000, &msgLen);

    /* Handle Errors. */
    if (result != RXN_SUCCESS) {
        /* Log an error. */
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
            "SendUpdateSeedOrClkRequest: Error reading a PGPS response msg through a socket. Error: %1.0f",
            (double) result);
        return RXN_FAIL;
    }

    /* Close the socket and free resources. */
    result = MSL_ScktClose(skt);

    /* Handle Errors. */
    if (result != RXN_SUCCESS) {
        /* Log an error. */
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
            "SendUpdateSeedOrClkRequest: Error closing a socket. Error: %1.0f",
            (double) result);

        return RXN_FAIL;
    }

    if(type == MSL_REQ_TYPE_SEED)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "SendUpdateSeedOrClkRequest: Seed Data retrieved.");
    }
    else
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "SendUpdateSeedOrClkRequest: Clock Data retrieved.");
    }

    result = MSL_ProcessPGPSServerRespMsg(msg, msgLen);

    /* Handle Errors. */
    if (result != RXN_SUCCESS) {
        /* Log an error. */
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
            "SendUpdateSeedOrClkRequest: Error processing a PGPS seed response msg. Error: %1.0f",
            (double) result);
        msg[msgLen - 1] = '\0';
        MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "SendUpdateSeedOrClkRequest: Error processing PGPS seed response msg: %s", msg);

        return result;
    }

    /* If a seed was successfully processed, reset the clock drift
       in order to allow exactly one clock download per seed */
    if (type == MSL_REQ_TYPE_SEED)
    {
        MSL_ClockUpdateCountReset();
    }

    /* Download and Decode of seed successful. Continue on to propagate seeds. */
    MSL_SetNextSystemEvent(MSL_EVENT_SEED_DOWNLOADED);

    return result;
}
#endif

