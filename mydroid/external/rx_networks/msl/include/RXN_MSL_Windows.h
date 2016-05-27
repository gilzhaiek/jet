/*
 * Copyright (c) 2013 Rx Networks, Inc. All rights reserved.
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

#ifndef RXN_MSL_WINDOWS_H
#define RXN_MSL_WINDOWS_H

#include <windows.h>
#include <winsock.h>

#define MSL_OS_WOULDBLOCK 	WSAEWOULDBLOCK
#define MSL_OS_INPROGRESS 	WSAEWOULDBLOCK
#define EVENT			U32
#define THREAD 			HANDLE

/* Address snprintf issue */
#ifdef _MSC_VER  /* all MS compilers define this (version) */
     #define snprintf _snprintf
#endif


enum MSL_REQUEST_EVENTS {
	MSL_UPDATE_CLK_REQUEST_EVENT = 0,
	MSL_UPDATE_SEED_REQUEST_EVENT,	
	MSL_SNTP_TIME_REQUEST_EVENT,
	//MSL_SEND_DEVICE_ID_REQUEST_EVENT,
	MSL_EXIT_MSG_SOCKET_THREAD_EVENT,
	MSL_REQUEST_EVENT_MAX
};
	
extern BOOL MSL_StartWinsock();
extern U16 MSL_SetSocketReadTimeout(SOCKET sckt, void* timeout);

#ifdef RXN_MSL_WIFI_SCAN

#define RXN_MSL_MAX_WIFI_ARR_SIZE	16
#define RXN_MSL_MAC_LENGTH			18
#define RXN_MSL_MAX_SSID_LENGTH		64
#define RXN_MSL_MAX_TYPE_LENGTH		8

/**
 *\brief
 * The Wi-Fi information available for a Wi-Fi AP.
 */
typedef struct RXN_MSL_WifiInfo
{
	char MAC[RXN_MSL_MAC_LENGTH];
	S32 RSSI;
	U32 LinkSpeed;
	char SSID[RXN_MSL_MAX_SSID_LENGTH];
	char type[RXN_MSL_MAX_TYPE_LENGTH];
} RXN_MSL_WifiInfo_t;

/**
 *\brief
 * A list of Wi-Fi AP's and their associated information.
 */
typedef struct RXN_MSL_WifiList
{
	U32 count;
	RXN_MSL_WifiInfo_t list[RXN_MSL_MAX_WIFI_ARR_SIZE];
} RXN_MSL_WifiList_t;

/** 
* \brief 
* Performs a Wi-Fi scan and returns the result.
* 
* \param wifiList
* [Out] The result of the Wi-Fi scan
*
* \return RXN_SUCCESS if the scan completed successfully.
* \return RXN_FAIL for any errors.
*
* \details
* <b>Description</b>\n
* This function performs a Wi-Fi scan, logging the result.  The SSID, MAC address and signal strength
* for each AP is returned.  The network type field in the results is not currently available and the
* result is always empty.
*
* Note:  This function is non-reentrant and should not be called by multiple threads concurrently.
*
*/ 
U08 RXN_MSL_GetWifiScan(RXN_MSL_WifiList_t* wifiList);

#endif

#endif
