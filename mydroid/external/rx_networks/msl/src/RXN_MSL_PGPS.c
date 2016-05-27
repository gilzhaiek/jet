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

/************************************
* External global functions below. *
************************************/

/* Used to issue commands to command pipe */
extern U16 CmdRequestStopMsgSocketThread();
extern U16 CmdRequestSNTPTime();
extern U16 CmdRequestSeed();
extern U16 CmdRequestClockUpdate();

#ifdef XYBRID
extern void MSL_ProcessXybridCoarsePostionRsp(R32 lat, R32 lon, R32 alt, U32 uncert);
extern void MSL_ProcessXybridErrorRsp();
extern void MSL_ProcessXybridBCEResponse(const char* data, U16 dataSize);
static void HandleXybridPositionReport(const CHAR* msg, U32 msgSize);
static void HandleXybridResponse(const CHAR* msg, U32 msgSize);
#endif

/*******************************
* External global vars below. *
*******************************/
extern MSL_Config_t gConfig;

extern U16 CmdRequestDeviceId(char* deviceId);

/**********************
* Global vars below. *
**********************/
/* Password store. */
static RXN_password_t mPassword;
/* Client info store. */
static RXN_client_info_t mClientInfo;
/*Client IMEI */
static char mDeviceIMEI[50] = "\0";

#define MSL_MAX_INT_LENGTH 11

#ifdef ANDROID
static U08 mDataAccessIsEnabled = MSL_DATA_ACCESS_UNDEFINED;
#else
static U08 mDataAccessIsEnabled = MSL_DATA_ACCESS_ENABLED;
#endif

extern U08 MSL_GetConstelConfig();

void RXN_MSL_SetDataAccess(BOOL isEnabled)
{
    if (isEnabled)
	{
        MSL_SetNextSystemEvent(MSL_EVENT_NETWORK_AVAILABLE);
		mDataAccessIsEnabled = MSL_DATA_ACCESS_ENABLED;
	}
    else
	{
		mDataAccessIsEnabled = MSL_DATA_ACCESS_DISABLED;
	}
}

U08 RXN_MSL_GetDataAccess()
{
    /* If gConfig.net.respect_Data_Settings is set to true then we will smartly determine if
     * the system allows for download of data, if gConfig.net.respect_Data_Settings is set to
     * false we will blindly allow for data. */
    if(gConfig.net.respect_Data_Settings)
    {
        return mDataAccessIsEnabled;
    }
    else
    {
        return MSL_DATA_ACCESS_ENABLED;
    }
}
U16 MSL_PGPS_Work(void)
{
    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,

        "MSL_PGPS_Work: Running state machine...");
    MSL_RunStateMachine();
    return RXN_SUCCESS;
}

U16 MSL_UpdateSeedOrClk(U08 type)
{
    U16 result = RXN_SUCCESS;                 /* Result store. */
    char deviceID[MSL_MAX_DEVICE_STR_LEN];    /* Device ID store. */

    /* Get a device ID. */
    RXN_MSL_GetDeviceID(deviceID);

    /* Generate a password. */
    if (MSL_GenPW(gConfig.sec.vendorId, gConfig.sec.modelId, deviceID) != NULL)
    {
        if (type == MSL_REQ_TYPE_SEED)
        {
            result = CmdRequestSeed();
			if (result == RXN_SUCCESS)
			{
				MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE04,
					"MSL_UpdateSeedOrClk: Seed request attempt made.");
			}			
			else
			{
				MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
					"MSL_UpdateSeedOrClk: CmdRequestSeed failed. Error: %1.0f", (double) result);
			}
        }
        else
        {
            result = CmdRequestClockUpdate();
			if (result == RXN_SUCCESS)
			{
				MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE04,
					"MSL_UpdateSeedOrClk: Clock update request attempt made.");
			}
			else
			{
				MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
					"MSL_UpdateSeedOrClk: CmdRequestClockUpdate failed. Error: %1.0f", (double) result);
			}
        }
    }
    else
    {
        /* Log an error. */
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE06,
            "MSL_UpdateSeedOrClk: Error generating a password for PGPS update. Error: %1.0f",
            (double) result);

        return MSL_PGPS_PW_GEN_ERROR;
    }   
    return result;
}

const U08* MSL_GenPW(char vendorID[MSL_MAX_VENDOR_STR_LEN],
              char modelID[MSL_MAX_MODEL_STR_LEN],
              char deviceID[MSL_MAX_DEVICE_STR_LEN])
{
  /* Setup clientInfo. */
  strncpy(mClientInfo.vendor_id, vendorID, MSL_MAX_VENDOR_STR_LEN);
  strncpy(mClientInfo.model_id, modelID, MSL_MAX_MODEL_STR_LEN);
  strncpy(mClientInfo.device_id, deviceID, MSL_MAX_DEVICE_STR_LEN);
  mClientInfo.gps_seconds = RXN_MSL_GetBestGPSTime();

    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE06,
        "MSL_GenPW: Will generate pass using sys time %1.0f and no offset.",
         (double)  mClientInfo.gps_seconds);

  /* Generate a password using clientInfo. */
  if(RXN_Generate_Password(&mClientInfo, &mPassword) != RXN_SUCCESS)
  {
    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE06,
        "MSL_GenPW: Error generating a password. Ensure that a valid security key file is used.");

        return NULL;
    }

    return mPassword.password;
}

U16 MSL_GenReqMsg(U08 type, CHAR IP[MSL_SKT_ADDR_MAX_LEN], 
                  CHAR reqMsg[MSL_SKT_REQ_MSG_MAX_LEN], U16* pMsgLen, U08 constelConfig)
{
    CHAR mask[32];
    CHAR auxInfo[32];
    U08  seedType = 7;
    

    /*  version = 2 for PGPS 3.0
        version = 3 for PGPS 6.1
        version = 4 for PGPS 6.2 */
#ifdef RXN_CORE_LIBRARY_7
    U08 ver = 5;
    seedType = 14;
#elif defined RXN_CORE_LIBRARY_6_2
    U08 ver = 4;
#else
    U08 ver = 3;
#endif

    /* Setup the mask according to type. */
    if(type == MSL_REQ_TYPE_SEED)
    {
        snprintf(mask, sizeof(mask), "1073741824"); /* 0x40000000 */
    }
    else // Clk update.
    {
        snprintf(mask, sizeof(mask), "64"); /* 0x40 */
    }

    switch(constelConfig)
    {

        case RXN_GPS_CONSTEL:
        {
            snprintf(auxInfo, sizeof(auxInfo), "GPS:BLT");
            break;
        }
        case RXN_GLO_CONSTEL:
        {
            snprintf(auxInfo, sizeof(auxInfo), "GLO:BLT&msgs=GLO:2AUX");
            break;
        }
        case RXN_ALL_CONSTEL:
        {
            snprintf(auxInfo, sizeof(auxInfo), "GPS:BLT;GLO:BLT&msgs=GLO:2AUX");
            break;
        }
    }

    /* Build the request msg for either a seed or clock update. */
#if defined (WIN32) || (WINCE) || (USE_NATIVE_SOCKET)
	snprintf((char*) reqMsg, MSL_SKT_REQ_MSG_MAX_LEN, "GET /grnserver/RXNPredictedData?seedAge=%d&version=%d&mask=%s&cId=%s&mId=%s&un=%s&pw=%s&seedType=%d&constType=%d&info=%s HTTP/1.1\r\nUser-Agent: _REQUEST\r\nHost: %s\r\n\r\n",
        gConfig.seedAge, ver, mask, mClientInfo.vendor_id, mClientInfo.model_id, mClientInfo.device_id, mPassword.password, seedType, constelConfig + 1, auxInfo, IP);
#else
    snprintf((char*) reqMsg, MSL_SKT_REQ_MSG_MAX_LEN, "grnserver/RXNPredictedData?seedAge=%d&version=%d&mask=%s&cId=%s&mId=%s&un=%s&pw=%s&seedType=%d&constType=%d&info=%s",
        gConfig.seedAge, ver, mask, mClientInfo.vendor_id, mClientInfo.model_id, mClientInfo.device_id, mPassword.password, seedType, constelConfig + 1, auxInfo);
#endif

    /* Set the msg len (include '\0' char). */
    *pMsgLen = (U16)(strlen(reqMsg) + 1);
    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08, reqMsg);
    return RXN_SUCCESS;
}

U16 MSL_ProcessPGPSServerRespMsg(CHAR respMsg[MSL_SKT_RESP_MSG_MAX_LEN], U32 respLen)
{
    U16 result = RXN_SUCCESS;                 /* Result store. */
    U08 respData[MSL_SKT_RESP_MSG_MAX_LEN];   /* Seed or clock data within the response. */
    CHAR* pStrHeadStart = NULL;               /* Pointer to header start within msg. */
    CHAR* pStrFootEnd = NULL;                 /* Pointer to footer start within msg. */
    U32 respDataLen = 0;                      /* Seed or clock data length. */
    RXN_seed_information_t curPRNseedInfo;    /* For logging of seed data. */
    U16 x = 0;                                /* Counter. */
    U08 y = 0; 
    RXN_constel_t constel;
    RXN_config_t config;

    const CHAR failAuthMsg[] = "HTTP Status 401";

    /* Data length is too short. Don't parse the message. 
    * Just return error and also avoid infinite loop when finding the "<\RXN>" tag. */
    if(respLen < 5)
    {			
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
            "MSL_ProcessPGPSServerRespMsg: data length error.");
        return MSL_PGPS_RESP_PROC_ERROR;
    }

    if (respMsg == NULL)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
            "MSL_ProcessPGPSServerRespMsg: Message is NULL.");
        return MSL_PGPS_RESP_PROC_ERROR;
    }

    /* Clear respData. */
    memset((void*) respData, 0, sizeof(char) * MSL_SKT_RESP_MSG_MAX_LEN);

    MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE04,
        "MSL_ProcessPGPSServerRespMsg: http response: %s", respMsg);
    // Find the start of the HTTP header (includes "<RXN...>" tag).
    pStrHeadStart = strstr(respMsg, failAuthMsg);
    if(pStrHeadStart != NULL)
    {
        return MSL_PGPS_RESP_PROC_ERROR;
    }

    pStrHeadStart = strstr(respMsg, "<RXN") ;

    // If the start could not be found, return an error.

    if(pStrHeadStart == NULL)
    {

        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
            "MSL_ProcessPGPSServerRespMsg: http response has error in header.");
        return MSL_PGPS_RESP_PROC_ERROR;
    }
    // Find a pointer to the char at the START of the "<\RXN>" tag.
    // Note that string fcns such as strstr can't be used as they may hit
    // a string within seed data and be thrown off.

    for(x=0; x<(respLen - 5); x++)
    {
        if(	(respMsg[x] == '<') && (respMsg[x+1] == '/') &&
            (respMsg[x+2] == 'R') && (respMsg[x+3] == 'X') &&
            (respMsg[x+4] == 'N') && (respMsg[x+5] == '>'))
        {
            pStrFootEnd = respMsg + x;
            break;
        }
    }

    // If the "<\RXN>" could not be found, return null.
    if(pStrFootEnd == NULL)
    {

        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
            "MSL_ProcessPGPSServerRespMsg: http response has error in footer.");
        return MSL_PGPS_RESP_PROC_ERROR;
    }

    // Get the seed or clock data length.
    respDataLen = (U32)((pStrFootEnd + 6) - pStrHeadStart);

    // Copy the data to respData for subsequent processing.
    memcpy(respData, pStrHeadStart, respDataLen);

    /* Check to see if it is a EOL seed*/
    if((strstr((char*)respData, "EOL") != NULL)&&(respDataLen == 14))
    {
        char eolFile[MSL_MAX_PATH];

        strncpy(eolFile, gConfig.pantryPath, MSL_MAX_PATH);
        strncat(eolFile, "/EOL.eol", MSL_MAX_FOLDER_NAME);

        MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE05,
            "MSL_ProcessPGPSServerRespMsg: EOL message detected, Creating: %s file", eolFile );
        MSL_OpenFile(MSL_EOL_RSRC_ID, eolFile, "w+");

        /* Disable downloads and makes the application idle for a year,
        if the user reboots the phone, the MSL_Initialize function should prevent the app from starting.*/
        gConfig.SNTP.requestRetryMax = 0;   
        gConfig.net.downloadRetryMax = 0;
        gConfig.SNTP.requestRetryTimer = 31104000;
        gConfig.net.downloadRetryTimer = 31104000;   
        gConfig.systemCheckFreq  = 31104000;

        MSL_CloseFile(MSL_EOL_RSRC_ID);

        return RXN_MSL_EOL_ERR;
    }
    
    for(y = 0; y < gConfig.numConstels; y++)
    {
        constel = gConfig.constelList[y];

        /* Decode the seed or clock data. */
        result = RXN_Decode(respData, respDataLen, constel);

        /* Handle errors. */
        if(result != RXN_SUCCESS)
        {
            MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
                "RXN_Decode: result: %1.0f. ...", (double) result);
            return MSL_PGPS_RESP_PROC_ERROR;
        }
    
        /* Log seed times. */

        for(x=1; x<=numOfPRNs[constel]; x++)
        {
            /* Get seed info for the currently indexed PRN seed. */
            result = RXN_Get_Seed_Information((U08)x, &curPRNseedInfo, constel);

            /* Don't worry about PRNs for which no info is available. */
            if(result != RXN_SUCCESS)
            {
                continue;
            }

            /* Log trace info. */
            MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
                "MSL_ProcessPGPSServerRespMsg: Decoded seed with data for PRN: %1.0f. Constel: %1.0f ...", (double) x, (double) constel);
            MSL_LogGPSTime("MSL_ProcessPGPSServerRespMsg: Seed creation time:", curPRNseedInfo.seed_creation_time);
        }
    }

    RXN_Get_Configuration(&config);

    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "MSL_ProcessPGPSServerRespMsg: Current leap second value indicated by PGPS seed: %1.0f.", (double)config.leap_secs);
    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "MSL_ProcessPGPSServerRespMsg: The next leap seconds occurrence indicated by the PGPS seed (in GPS seconds): %1.0f.", (double)config.gps_time_next_leap);

    return RXN_SUCCESS;
}

U16 MSL_PropagateAll(RXN_constel_t constel)
{
    U16 result = RXN_SUCCESS;               /* Result store. */
    U08 PRN = 0;                            /* PRN count reference. */
    RXN_seed_information_t curPRNseedInfo;  /* Info store for each PRN's seed. */

    U32 currentTime = 0;
	U32 targetTime = 0;
	RXN_state_t state;
	BOOL bPropagateAll_Complete = FALSE;
	U08 numOfSeedPropagated = 0;

    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "MSL_PropagateAll: Propagate Constel: %1.0f ...", (float)constel);

#ifndef RXN_CONFIG_INCLUDE_GLONASS
    if(constel == RXN_GLO_CONSTEL)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
            "MSL_PropagateAll: GLONASS support has not been enabled.");
        return RXN_FAIL;
    }
#endif

 	memset((void *)&state, 0, sizeof(state));
	state.buffer_ptr = &state.buffer[0]; 

    currentTime = RXN_MSL_GetBestGPSTime();

    MSL_LogGPSTime("MSL_PropagateAll: Current GPS time:", currentTime);

	/* Loop through each PRN.
	* If a PRN is un-available - the seed start time will be 0.*/
	for(PRN=1; PRN<=numOfPRNs[constel] ; PRN++)
	{
        U32 count = 0;

        MSL_Sleep(1);

        memset(&curPRNseedInfo, 0, sizeof(RXN_seed_information_t));

		/* Get seed info for the currently indexed PRN seed. */
		result = RXN_Get_Seed_Information(PRN, &curPRNseedInfo, constel);

        /* Propagate the PRN seed forward by 4 hours. If no seed information, or the seed data is stale, use current time + 4 hours*/
        if(curPRNseedInfo.poly_curr_time > currentTime)
        {
            targetTime = curPRNseedInfo.poly_curr_time + (4 * SYSTEM_HOUR);
        }
        else
        {
            targetTime = currentTime + (4 * SYSTEM_HOUR);
        }

        /* Propagate all PRNs forward by 4 hours. */        
			MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "MSL_PropagateAll: Propagating PRN: %1.0f", (double) PRN);
        MSL_LogGPSTime("MSL_PropagateAll: Propagation target GPS time:", targetTime);
        
        state.status = RXN_STATUS_START;
        do {
            if (MSL_CheckAbort()) {
                state.status = RXN_STATUS_ABORT;
            }
            result = RXN_Generate_Assistance_NB(PRN, targetTime, constel, &state);
        }while (state.status == RXN_STATUS_PENDING && ++count < 10000000);

        /* If it looks like propagation was in an infinite loop, exit the loop early */
        if (count == 10000000)
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
                "MSL_PropagateAll: Propagation taking too long.  Terminated early.");
        }

        if(result != RXN_SUCCESS)
        {
            if(result == RXN_DEBUG_CODE_ERROR_EPHEMERIS_TOO_OLD)
            {
                MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
                    "MSL_PropagateAll: Broadcast ephemeris is too old, unable to generate SAGPS seed for PRN: %1.0f.", (double)PRN); 
            }
            else if(result == RXN_DEBUG_CODE_ERROR_EPHEMERIS_UNAVAILABLE)
            {
                MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
                    "MSL_PropagateAll: No broadcast ephemeris available, unable to generate SAGPS seed for PRN: %1.0f.", (double)PRN); 
            }
            else if(result == RXN_DEBUG_CODE_ERROR_EVALUATION_LICENSE_EXPIRED)
            {
                MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
                    "MSL_PropagateAll: Propagate data past the validity period of the license key is not possible. No further propagation will occur.");
                return MSL_LICENSE_KEY_EXPIRED;
            }
            else
            {
                MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
                    "MSL_PropagateAll: Could not RXN_Generate_Assistance for PRN: %1.0f. Debug Code: %1.0f", (double)PRN, (double)result); 
            }
        }
    }

    bPropagateAll_Complete = TRUE;

    /* Loop through each PRN.
    * If a PRN is un-available - the seed start time will be 0.*/
    for(PRN=1; PRN<=numOfPRNs[constel] ; PRN++)
	{
        MSL_Sleep(1);
        /* Get seed info for the currently indexed PRN seed. */
        result = RXN_Get_Seed_Information(PRN, &curPRNseedInfo, constel);

        /* Don't worry about PRNs for which no info is available. */
        if(result != RXN_SUCCESS)
        {
            MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_WARNING, RXN_LOG_ZONE08,
                "MSL_PropagateAll: Could not RXN_Get_Seed_Information for Constel: %1.0f PRN: %1.0f",constel, PRN);
            continue;
        }
        else
            numOfSeedPropagated++;

        /* If the PRN is still not propagated out completely break out of this loop */

        /* Continually ensure the pantry has been propagated far enough in the future, but does not exceed the expiry of the seed */
        /* This exact logic can be found in the propagation state machine (MSL_CheckSeedStatus)*/
        if((curPRNseedInfo.number_of_ephemeris_used == 0 && (((currentTime + gConfig.constel[constel].prop.PGPSFwd) - curPRNseedInfo.poly_curr_time) >= 4*SYSTEM_HOUR) && curPRNseedInfo.poly_curr_time < curPRNseedInfo.poly_expire_time) ||		// PGPS seeds
            (curPRNseedInfo.number_of_ephemeris_used != 0 && (((currentTime + gConfig.constel[constel].prop.SAGPSFwd) - curPRNseedInfo.poly_curr_time) >= 4*SYSTEM_HOUR) && curPRNseedInfo.poly_curr_time < curPRNseedInfo.poly_expire_time && currentTime < curPRNseedInfo.poly_expire_time))		// SAGPS seeds
        {
            /* Log trace info. */
            MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
                "MSL_PropagateAll: Prop must continue as req'd for PRN: %1.0f...",
                (double) PRN);
            MSL_LogGPSTime("MSL_PropagateAll: PRN seed create time: -->", curPRNseedInfo.seed_creation_time);
            MSL_LogGPSTime("MSL_PropagateAll: PRN poly start time: --->", curPRNseedInfo.poly_start_time);
            MSL_LogGPSTime("MSL_PropagateAll: PRN poly current time: ->", curPRNseedInfo.poly_curr_time);
            MSL_LogGPSTime("MSL_PropagateAll: PRN poly expire time: -->", curPRNseedInfo.poly_expire_time);

            /* We can continue with propagation. */
            bPropagateAll_Complete = FALSE;
            break;
        }
    }

    if(numOfSeedPropagated == 0)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_WARNING, RXN_LOG_ZONE08, "MSL_PropagateAll: No PGPS or SAGPS seeds are available.");
        return MSL_PROPAGATION_COMPLETE;
    }
    else if(bPropagateAll_Complete)
    {
        /* Finished propagating. */
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08, "MSL_PropagateAll: Prop complete. %1.0f seeds are propagated.", numOfSeedPropagated);
        return MSL_PROPAGATION_COMPLETE;
    }
    return RXN_SUCCESS;
}

#ifdef ANDROID
U16 MSL_ParseXML(CHAR* msg, U32 msgSize)
{
  CHAR * pStart;
  CHAR * pEnd;
  U16 result;
    BOOL foundSeed = FALSE;

    U32 tagLength = 0;

    enum MSL_Message_Response_Type messageType;

    const CHAR pgpsSeedTag[] = "<name>PGPS_Seed</name>";
    const CHAR sntpTimeTag[] = "<name>SNTP_Time</name>";
    const CHAR deviceIdTag[] = "<name>Device_ID</name>";
    const CHAR dataAccessTag[] = "<name>Data_Access_Enabled</name>";

    MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "RXN_MSL_ParseXML: Message received: %s", msg);
    /* will devise better way*/
    pStart = strstr(msg, pgpsSeedTag);
    if(pStart != NULL)
    {
        messageType = MSL_SEED_DATA;
    }
    pStart = strstr(msg, sntpTimeTag);
    if(pStart != NULL)
    {
        messageType = MSL_SNTP_TIME;
    }
    pStart = strstr(msg, deviceIdTag);
    if(pStart != NULL)
    {
        messageType = MSL_DEVICE_ID;
    }
    pStart = strstr(msg, dataAccessTag);
    if(pStart != NULL)
    {
        messageType = MSL_DATA_ACCESS_STATUS;
    }

#ifdef XYBRID
    const CHAR xybridResponseTag[] = "<name>xybrid_response</name>";
    const CHAR xybridErrorReportTag[] = "<name>xybrid_position_error</name>";

    if(strstr(msg, xybridResponseTag) != NULL)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE09, "RXN_MSL_ParseXML: Xybrid response");
        messageType = MSL_XYBRID_RESPONSE;
    }
    else if(strstr(msg, xybridErrorReportTag) != NULL)
    {        
        MSL_ProcessXybridErrorRsp();
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE09, "RXN_MSL_ParseXML: Xybrid sever error report: Server connection failed or timeout.");
        return MSL_XYBRID_RESPONSE; 
    }
#endif

    switch(messageType)
    {
    case MSL_XYBRID_RESPONSE:
#ifdef XYBRID
        {
            HandleXybridResponse(msg, msgSize);
        }
#endif 
		break;

    case MSL_SEED_DATA:
        {
            CHAR const seedStartTag[] = "<seed><![CDATA[";
            CHAR const clockDataMaskTag[] = "mask=\"64\"";

            /*size*/
            pStart = strstr(msg, seedStartTag) + sizeof(seedStartTag) - 1;
                if(pStart == NULL)
                {
                    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
                            "RXN_MSL_ParseXML: No Seed Data.");
                    return messageType;
                }

            U32 remainder = msgSize - (U32)(pStart - msg);

            if(remainder > 0)
            {
                if(strstr(msg, clockDataMaskTag) != NULL)
                {
                    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
                        "RXN_MSL_ParseXML: Clock Data retrieved.");
                }
                else
                {
                    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
                        "RXN_MSL_ParseXML: Seed Data retrieved.");
                    foundSeed = TRUE;
                }

                result = MSL_ProcessPGPSServerRespMsg(pStart, remainder);

                /* Handle Errors. */
                if(result != RXN_SUCCESS)
                {
                    /* Log an error. */
                    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
                        "RXN_MSL_ParseXML: Error processing a PGPS seed response msg. Error: %1.0f",
                        (double) result);

                    /* Truncate string to pass to logging function*/
                    pStart[remainder - 1] = '\0';
                    MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
                        "RXN_MSL_ParseXML: Error processing PGPS seed response msg: %s", pStart);

                    return messageType;
                }

                /* If a seed was successfully processed, reset the clock drift
                   in order to allow exactly one clock download per seed */
                if (foundSeed)
                {
                    MSL_ClockUpdateCountReset();
                }

                /* Download and Decode of seed successful. Continue on to propagate seeds. */
                MSL_SetNextSystemEvent(MSL_EVENT_SEED_DOWNLOADED);
            }
        }
        break;

    case MSL_SNTP_TIME:
        {
            CHAR const sntpTimeStartTag[] = "<time>";
            CHAR const sntpTimeEndTag[] = "</time>";

            CHAR const clockOffsetStartTag[] = "<clock offset>";
            CHAR const clockOffsetEndTag[] = "</clock offset>";

            CHAR buffer[MSL_SKT_RESP_MSG_MAX_LEN];
            U32 time = 0;

            /*size*/
            pStart = strstr(msg, sntpTimeStartTag) + sizeof(sntpTimeStartTag) -1;
            pEnd = strstr(msg, sntpTimeEndTag);

            if((pStart != NULL)&&(pEnd != NULL))
            {
                tagLength = (U32)(pEnd - pStart);
                memset(buffer, 0, sizeof(buffer));
                strncpy(buffer, pStart, tagLength);
                time = strtoul(buffer,NULL,0);
            }

            /*size*/
            pStart = strstr(msg, clockOffsetStartTag) + sizeof(clockOffsetStartTag) -1;
            pEnd = strstr(msg, clockOffsetEndTag);

            if((pStart != NULL)&&(pEnd != NULL))
            {
                tagLength = (U32)(pEnd - pStart);
                memset(buffer, 0, sizeof(buffer));
                strncpy(buffer, pStart, tagLength);
            }

            if(time != 0)
            {
		MSL_SetSNTPTime(time);
                MSL_SetNextSystemEvent(MSL_EVENT_TIME_UPDATED);
	    }
        }
        break;

    case MSL_DEVICE_ID:
        {
            CHAR const imeiStartTag[] = "<IMEI>";
            CHAR const imeiEndTag[] = "</IMEI>";

            /*size*/
            pStart = strstr(msg, imeiStartTag) + sizeof(imeiStartTag) -1;
            pEnd = strstr(msg, imeiEndTag);

            if((pStart != NULL)&&(pEnd != NULL))
            {
                tagLength = (U32)(pEnd - pStart);
                memset(mDeviceIMEI, 0, sizeof(mDeviceIMEI));
                strncpy(mDeviceIMEI, pStart, tagLength);
                MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
                    "RXN_MSL_ParseXML: Device IMEI: %s.", mDeviceIMEI);
            }
        }
        break;

    case MSL_DATA_ACCESS_STATUS:
        {
            CHAR const StartTag[] = "<data>";
            CHAR const EndTag[] = "</data>";
            CHAR buffer[MSL_SKT_RESP_MSG_MAX_LEN];
            int dataEnabled = 0;

            /*size*/
            pStart = strstr(msg, StartTag) + sizeof(StartTag) -1;
            pEnd = strstr(msg, EndTag);

            if((pStart != NULL)&&(pEnd != NULL))
            {
                tagLength = (U32)(pEnd - pStart);
                memset(buffer, 0, sizeof(buffer));
                strncpy(buffer, pStart, tagLength);
                dataEnabled = atoi(buffer);
                MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
                    "RXN_MSL_ParseXML: Data Access is: %1.0f.", dataEnabled);

                if(dataEnabled > 0)
                {
                    RXN_MSL_SetDataAccess(TRUE);
                }
                else
                {
                    RXN_MSL_SetDataAccess(FALSE);
                }
            }
        }
        break;
  }
  return messageType;
}
#endif

void RXN_MSL_GetDeviceID(char device[MSL_MAX_DEVICE_STR_LEN])
{
	/* Vendors must implement this fcn call to provide a device
	* ID that is unique from device to device. 
	*/
	if(strcmp(mDeviceIMEI, "\0") == 0)
        CmdRequestDeviceId(mDeviceIMEI);

	if(strcmp(mDeviceIMEI, "\0") == 0)
		strcpy(device, "DefaultId");
	else
		strcpy(device, mDeviceIMEI);

    MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "RXN_MSL_GetDeviceID: Device ID: %s.", device);

	return;
}


#ifdef XYBRID
static void HandleXybridPositionReport(const CHAR* msg, U32 msgSize)
{
    const CHAR * pStart;
    const CHAR * pEnd;
    U32 tagLength = 0;

    R32 lat = 0;
    R32 lon = 0;
    R32 alt = 0;
    U32 uncert = 0;

    CHAR const ErrorStartTag[] = "<RxnAssist><Error format=\"XML\">";
    CHAR const ErrorEndTag[] = "</Error>";
    CHAR const LatStartTag[] = "<Lat>";
    CHAR const LatEndTag[] = "</Lat>";
    CHAR const LonStartTag[] = "<Lon>";
    CHAR const LonEndTag[] = "</Lon>";

    CHAR const AltStartTag[] = "<Alt>";
    CHAR const AltEndTag[] = "</Alt>";

    CHAR const UncertaintyStartTag[] = "<Uncertainty>";
    CHAR const UncertaintyEndTag[] = "</Uncertainty>";

    CHAR buffer[MSL_SKT_RESP_MSG_MAX_LEN];
    memset(buffer, 0, sizeof(buffer));

    pStart = strstr(msg, ErrorStartTag);
    if (pStart != NULL)
    {
        pStart = strstr(msg, ErrorStartTag) + sizeof (ErrorStartTag) - 1;
        pEnd = strstr(msg, ErrorEndTag);

        tagLength = pEnd - pStart;

        strncpy(buffer, pStart, tagLength);
        MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE09, "RXN_MSL_ParseXML: Xybrid sever error report: %s", buffer);
        MSL_ProcessXybridErrorRsp();
        return;
    }

    /* Lat */
    pStart = strstr(msg, LatStartTag);
    if (pStart != NULL)
    { 
        pStart = strstr(msg, LatStartTag) + sizeof (LatStartTag) - 1;
        pEnd = strstr(msg, LatEndTag);

        tagLength = pEnd - pStart;

        strncpy(buffer, pStart, tagLength);
        lat = atof(buffer);
    }

    /* Lon */
    pStart = strstr(msg, LonStartTag);
    if (pStart != NULL)
    {
        pStart = strstr(msg, LonStartTag) + sizeof (LonStartTag) - 1;
        pEnd = strstr(msg, LonEndTag);

        tagLength = pEnd - pStart;

        memset(buffer, 0, sizeof(buffer));
        strncpy(buffer, pStart, tagLength);
 
        lon = atof(buffer);
    }

    /* Alt */
    pStart = strstr(msg, AltStartTag);
    if (pStart != NULL)
    {
        pStart = strstr(msg, AltStartTag) + sizeof (AltStartTag) - 1;
        pEnd = strstr(msg, AltEndTag);

        tagLength = pEnd - pStart;

        memset(buffer, 0, sizeof(buffer));
        strncpy(buffer, pStart, tagLength);

        alt = atof(buffer);
    }

    /* Uncertainty */
    pStart = strstr(msg, UncertaintyStartTag);
    if (pStart != NULL)
    {
        pStart = strstr(msg, UncertaintyStartTag) + sizeof (UncertaintyStartTag) - 1;
        pEnd = strstr(msg, UncertaintyEndTag);

        tagLength = pEnd - pStart;
        
        memset(buffer, 0, sizeof(buffer));
        strncpy(buffer, pStart, tagLength);

        uncert = atoi(buffer);
    }

    MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE03, 
            "Xybrid result lat: %f lon: %f", lat, lon);
    MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE03, 
            "Xybrid result alt: %1.0f uncertainty: %1.0f", alt, uncert);

    MSL_ProcessXybridCoarsePostionRsp(lat, lon, alt, uncert);
}

static U32 ParseU32(const CHAR* msg, U32 msgSize, const char* start, const char* end)
{
    const char* pStart = strstr(msg, start) + strlen(start);
    const char* pEnd = strstr(pStart, end);
    char buffer[MSL_MAX_INT_LENGTH];

    U32 tagLength = (U32)(pEnd - pStart);

    if (tagLength > MSL_MAX_INT_LENGTH)
        tagLength = MSL_MAX_INT_LENGTH;

    memset(buffer, 0, sizeof(buffer));
    strncpy(buffer, pStart, tagLength);
    return atoi(buffer);
}

/* Note: only works if haystack is larger than needle. Needle must be null terminated, but haytack
 * does not need to be. */
static char* rstrnstr(const char* haystack, U32 size, const char* needle)
{
    const char* min = haystack - size;
    const char* ptr = haystack - strlen(needle);
    while (--ptr && ptr >= min)
    {
        char* ret = strstr(ptr, needle);
        if (ret)
            return ret;
    }
    return NULL;
}

static void HandleXybridResponse(const CHAR* msg, U32 msgSize)
{
    CHAR const sizeStartTag[] = "<data_size>";
    CHAR const sizeEndTag[] = "</data_size>";
    CHAR const dataStartTag[] = "<RXN ";
    const char* pStart = NULL;

    U32 dataSize = ParseU32(msg, msgSize, sizeStartTag, sizeEndTag);

    if (dataSize <= 0)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
            "RXN_MSL_ParseXML: No Data.");
        return;
    }
    
    pStart = strstr(msg, dataStartTag);
    if (pStart)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08, "BCE received");

        pStart += strlen(dataStartTag);

        /* Look for the end of the "<RXN" tag */
        while (*pStart++ != '>' && (U32)(pStart - msg) < msgSize);

        MSL_ProcessXybridBCEResponse(pStart, dataSize);
    }

    /* Because the message contains binary data, a strstr() search for the RefLocation
     * tag will fail since we are likely to come across a NULL character before the
     * message ends.  Search backwards from the end of the message instead. */
    const char* pLocationStart = rstrnstr(msg + msgSize, msgSize, "<RefLocation");
    if (pLocationStart)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08, "Xybrid location received");
        HandleXybridPositionReport(pLocationStart, strlen(pLocationStart));
    }
}

#endif
