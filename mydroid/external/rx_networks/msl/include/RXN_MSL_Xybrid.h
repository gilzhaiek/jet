/*
* Copyright (c) 2012 Rx Networks, Inc. All rights reserved.
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
* $LastChangedDate: 2009-03-23 14:19:48 -0700 (Mon, 23 Mar 2009) $
* $Revision: 9903 $
*************************************************************************
*/

#ifndef RXN_XYBRID_H
#define	RXN_XYBRID_H

#ifdef	__cplusplus
extern "C" {
#endif

#include "RXN_MSL.h"

/**
 *\brief
 * Enumeration of xybrid configuration.  These are bitmasks and can be OR'd together.
 */
enum RXN_MSL_XBD_Config {
    RXN_MSL_XBD_CONFIG_NONE = 0,                        /*!< Uninitialized */
    RXN_MSL_XBD_CONFIG_WIFI_LOOKUP_ENABLED = 1,         /*!< Enable wifi lookup */
    RXN_MSL_XBD_CONFIG_DONATE_ENABLED = 1 << 1,         /*!< Enable donation */
    RXN_MSL_XBD_CONFIG_CELL_LOOKUP_ENABLED = 1 << 2,    /*!< Enable cell data in lookup */
};

/**
  *\brief
  * Calculates a Xybrid coarse position.  This function blocks until a position
  * is retrieved or until the specified number of seconds expires.
  *
  * \param pCoarsePosition The returned coarse position.
  * \param seconds The number of seconds to wait for a position before timing out.
  */
U08 RXN_MSL_GetXybridCoarsePosition(RXN_RefLocation_t* pCoarsePosition, U32 seconds);

/**
  *\brief
  * Downloads BCE from the Xybrid RT server and loads it into the MSL.  Propagation
  * will occur the next time RXN_MSL_Work() is called.  This function blocks until
  * the BCE has been downloaded from the server.
  *
  * \param filtered If TRUE, only the BCE relevant to the current area will be downloaded. Otherwise
  * BCE for all sv's is downloaded.
  *
  * \param pCoarsePosition The returned coarse position.  Set to NULL if not needed.
  * \param seconds The number of seconds to wait for a position before timing out.
  */
U08 RXN_MSL_XybridGetBCEAndPosition(BOOL filtered, RXN_FullEph_t* PRNArr, U08* ArrSize, RXN_RefLocation_t* pCoarsePosition, U32 seconds);

/**
  *\brief
  * Downloads BCE from the Xybrid RT server and loads it into the MSL.  Propagation
  * will occur the next time RXN_MSL_Work() is called.  This function blocks until
  * BCE has been downloaded from the server.
  *
  * \param filtered If TRUE, only the BCE relevant to the current area will be downloaded. Otherwise
  * BCE for all sv's is downloaded.
  * \param seconds The number of seconds to wait for a position before timing out.
  */
U08 RXN_MSL_XybridGetBCE(BOOL filtered, RXN_FullEph_t* PRNArr, U08* ArrSize, U32 seconds);

/**
  * \brief
  * Calculates a Synchro coarse position.  This function blocks until a position
  * is retrieved. 
  *
  * \param pCoarsePosition The returned coarse position.
  * \param seconds The number of seconds to wait for a position before timing out.
  */
U08 RXN_MSL_SynchroGetPosition(RXN_RefLocation_t* pCoarsePosition, U32 seconds);


/* Initialize Xybrid */
void MSL_Xybrid_Initialize();

/* Uninitialize Xybrid */
void MSL_Xybrid_Uninitialize();

/* Request Xybrid assistance from RXN Services */
U08 MSL_RequestXybridAssistance(U32 assistanceMask);

/* Set whether BCE requests should be filtered or composite */
void MSL_SetBCEFiltered(BOOL filtered);

/* Gets the current Xybrid position */
void MSL_GetXybridPosition(RXN_RefLocation_t* pPosition);

#ifdef	__cplusplus
}
#endif

#endif	/* RXN_XYBRID_H */

