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
 * $LastChangedDate: 2009-03-02 15:53:19 -0800 (Mon, 02 Mar 2009) $
 * $Revision: 9396 $
 *************************************************************************
 *
 */

#ifndef RXN_MSL_FILE_PGPS_H
#define RXN_MSL_FILE_PGPS_H

#ifdef __cplusplus
extern "C" {
#endif


/* Temporary location to be moved to API header set*/
typedef char CHAR;

enum MSL_Message_Response_Type
{
    MSL_SEED_DATA,
    MSL_SNTP_TIME,
    MSL_DEVICE_ID,
    MSL_XYBRID_RESPONSE,
    MSL_DATA_ACCESS_STATUS,
};

/*************
 * Constants *
 *************/

#define MSL_PGPS_CACHE_SIZE    RXN_CONSTANT_MIN_CACHE_SIZE


U16 MSL_PGPS_Work(void);

/*********************
 * Utility Functions *
 *********************/

/* Update all seeds or clocks. */
U16 MSL_UpdateSeedOrClk(U08 type);

/* Generate a password. */
const U08* MSL_GenPW(CHAR vendorID[MSL_MAX_VENDOR_STR_LEN],
              CHAR modelID[MSL_MAX_MODEL_STR_LEN],
              CHAR deviceID[MSL_MAX_DEVICE_STR_LEN]);

/* Generate a msg that can be used to request a seed or clock update. */
U16 MSL_GenReqMsg(U08 type, 
                  CHAR IP[MSL_SKT_ADDR_MAX_LEN],
                  CHAR reqMsg[MSL_SKT_REQ_MSG_MAX_LEN],
                  U16* pMsgLen,
                  U08 constelConfig);

/* Process the response to a seed or clock request. */
U16 MSL_ProcessPGPSServerRespMsg(CHAR respMsg[MSL_SKT_RESP_MSG_MAX_LEN], U32 respLen);

/* Propagate all PRNs forward by 4 hours. Non-blocking */
U16 MSL_PropagateAll(RXN_constel_t constel);


/* Get the device specific ID that is used to generate a required PGPS
 * seed password. Each vendor must determine how to get an ID that is
 * unique on a per-device basis and implement this function accordingly.
 * Note that the implementation of this function is to reside within 
 * RXN_MSL_AbsCalls.c */
void RXN_MSL_GetDeviceID(CHAR device[MSL_MAX_DEVICE_STR_LEN]);

/* Used to parse incoming XML messages from RXN Services */
U16 MSL_ParseXML(CHAR* msg, U32 msgSize);

/* Sends an XML request to RXN Services to request SNTP Time */
U32 MSL_RequestSNTPTime();


/********************************
 * System Maintenance Functions *
 ********************************/

#ifdef ANDROID
/* Sends a message to gWakePipe to ensure the system obtains a wakeLock. */
U16 MSL_WakeUpWork();

/* Closes gWakePipe */
void MSL_CloseWakePipe();
#endif

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* RXN_MSL_FILE_PGPS_H */
