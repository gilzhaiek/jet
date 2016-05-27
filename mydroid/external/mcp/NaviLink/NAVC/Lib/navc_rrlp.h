/******************************************************************************\
##                                                                            *
## Unpublished Proprietary and Confidential Information of Texas Instruments  *
## Israel Ltd. Do Not Disclose.                                               *
## Copyright 2008 Texas Instruments Israel Ltd.                               *
## All rights reserved. All unpublished rights reserved.                      *
##                                                                            *
## No part of this work may be used or reproduced in any form or by any       *
## means, or stored in a database or retrieval system, without prior written  *
## permission of Texas Instruments Israel Ltd. or its parent company Texas    *
## Instruments Incorporated.                                                  *
## Use of this work is subject to a license from Texas Instruments Israel     *
## Ltd. or its parent company Texas Instruments Incorporated.                 *
##                                                                            *
## This work contains Texas Instruments Israel Ltd. confidential and          *
## proprietary information which is protected by copyright, trade secret,     *
## trademark and other intellectual property rights.                          *
##                                                                            *
## The United States, Israel  and other countries maintain controls on the    *
## export and/or import of cryptographic items and technology. Unless prior   *
## authorization is obtained from the U.S. Department of Commerce and the     *
## Israeli Government, you shall not export, reexport, or release, directly   *
## or indirectly, any technology, software, or software source code received  *
## from Texas Instruments Incorporated (TI) or Texas Instruments Israel,      *
## or export, directly or indirectly, any direct product of such technology,  *
## software, or software source code to any destination or country to which   *
## the export, reexport or release of the technology, software, software      *
## source code, or direct product is prohibited by the EAR. The subject items *
## are classified as encryption items under Part 740.17 of the Commerce       *
## Control List (“CCL”). The assurances provided for herein are furnished in  *
## compliance with the specific encryption controls set forth in Part 740.17  *
## of the EAR -Encryption Commodities and Software (ENC).                     *
##                                                                            *
## NOTE: THE TRANSFER OF THE TECHNICAL INFORMATION IS BEING MADE UNDER AN     *
## EXPORT LICENSE ISSUED BY THE ISRAELI GOVERNMENT AND THE APPLICABLE EXPORT  *
## LICENSE DOES NOT ALLOW THE TECHNICAL INFORMATION TO BE USED FOR THE        *
## MODIFICATION OF THE BT ENCRYPTION OR THE DEVELOPMENT OF ANY NEW ENCRYPTION.*
## UNDER THE ISRAELI GOVERNMENT'S EXPORT LICENSE, THE INFORMATION CAN BE USED *
## FOR THE INTERNAL DESIGN AND MANUFACTURE OF TI PRODUCTS THAT WILL CONTAIN   *
## THE BT IC.                                                                 *
##                                                                            *
\******************************************************************************/

/** \file   navc_rrlp.h 
 *  \brief  RRLP (Radio Resource LCS Protocol) module interface specification
 * 
 * This file contains functions to decode RRLP messages received from the SMLC 
 * (Serving Mobile Location Centre) and to encode RRLP messages tranmitted to SMLC.
 * 
 * 
 *  \see    navc_rrlp.c
 */

#ifndef _NAVC_RRLP_H
#define _NAVC_RRLP_H

#include "gpsc_data.h"
#include "mcpf_defs.h"
#include "navc_defs.h"
// #include "gpsc_me_api.h"


/************************************************************************
 * Defines
 ************************************************************************/

#define RRLP_MAX_SVS_ACQ_ASSIST 16
#define C_FOUR_HRS_MSEC     14400000L /* 4 hours worth of msec */

/************************************************************************
 * Types
 ************************************************************************/


/* RRLP Measurement Types */
typedef enum
{
	C_RRLP_REP_POS,
	C_RRLP_REP_MEAS,
	C_RRLP_REP_LOCERR

} ERrlp_MeasrType;


/* RRLP MsrPositionReponse LocationError Error Reasons */
typedef enum
{
  C_RRLP_LOC_REASON_UNDEF,        /* undefined */
  C_RRLP_LOC_REASON_NO_ENF_BTS,   /* not enough BTSs for E-OTD */
  C_RRLP_LOC_REASON_NO_ENF_SVS,   /* not enough SVs for GPS */
  C_RRLP_LOC_REASON_EOTD_LCA_MIS, /* EOTD location calculation assistance
                                     data missing */
  C_RRLP_LOC_REASON_EOTD_ASS_MIS, /* EOTD assistance data missing */
  C_RRLP_LOC_REASON_GPS_LCA_MIS,  /* GPS location calculation assistance
                                     data missing */
  C_RRLP_LOC_REASON_GPS_ASS_MIS,  /* GPS assistance data misssing */
  C_RRLP_LOC_REASON_REQ_MTD_US,   /* requested method unsupported */
  C_RRLP_LOC_REASON_LOC_REQ_US,   /* location request not supported */
  C_RRLP_LOC_REASON_BTS_GPS_NS,   /* reference BTS for GPS not the serving
                                     BTS */
  C_RRLP_LOC_REASON_BTS_EOTD_NS,  /* reference BTS for EOTD not the serving
                                     BTS */

  C_RRLP_LOC_REASON_GOOD = 255    /* NOT a LocationError code, used to
                                     indicate to our internal function that
                                     the mrsLocationResponse contains valid
                                     position or meas. */
} ERrlp_LocReason;

/** 
 * \fn     RRLP_Init
 * \brief  Initialize NAVC RRLP module object
 * 
 * Allocate NAVC RRLP object and initialize it
 * 
 * \note
 * \param	hMcpf - handle to OS Framework
 * \return	pointer to RRLP created object or NULL in case of memory allocation error
 * \sa      RRLP_Destroy
 */ 
handle_t RRLP_Init (handle_t 	hMcpf, 
					handle_t 	hCmdPool,
					handle_t 	hEvtPool,
					EmcpTaskId  eSrcTaskId,
					McpU8		uSrcQId);


/** 
 * \fn     RRLP_processRxMsg
 * \brief  Process incoming RRLP message
 * 
 *  The function decodes the RRLP message and do the necessary functionality. 
 *  If the incoming RRLP message is Measure Position Request then the 
 *  MS sends Location info, or GPS-Measureinfo or Location error accordingly. 
 *  If the incoming RRLP message is Protocol error then the current location 
 *  fix is stopped.
 * 
 * \note
 * \param	hRrlp - handle to RRLP object
 * \param	pMsg  - pointer to RRLP message buffer
 * \param	uLen  - RRLP message length
 * \param	uSystemTimeInMsec - system time of RRLP message reception
 * \return	result of RRLP decode: OK or Error  
 * \sa
 */
EMcpfRes RRLP_processRxMsg (handle_t 	hRrlp, 
							McpU8 		*pMsg,
							McpU16		uLen,
							McpU32 		uSystemTimeInMsec);

/** 
 * \fn     RRLP_encodeResponse
 * \brief  Encode RRLP response message
 * 
 *  The function encodes the RRLP message by specified message and error type
 * 
 * \note
 * \param	hRrlp - handle to RRLP object
 * \param	p_zLocFixResponse  - pointer to structure containing location response
 * \param	pLocError  		   - pointer to structure containing error information
 * \param	uMeasType  		   - measurement type
 * \param	eLocReason  	   - location error reason
 * \param	pMsgBuf  		   - pointer to output data buffer containing RRLP response
 * \param	pLen  		   - pointer to output data buffer containing RRLP response
 * \return	length of encoded RRLP message
 * \sa
 */
McpU16 RRLP_encodeResponse (handle_t				hRrlp,
							T_GPSC_loc_fix_response	*p_zLocFixResponse,
							TNAVCD_LocError			*pLocError,
							ERrlp_MeasrType			eMeasType,
							ERrlp_LocReason 		eLocReason,
							McpU8 					*pMsgBuf);



/** 
 * \fn     RRLP_StopLocationFixRequest 
 * \brief  Destroy NAVC RRLP module object
 * 
 *  The function Stops the Ongoing Location Request
 * 
 * \note
 * \param	hRrlp - handle to RRLP object
 * \return	void
 * \sa      
 */
void RRLP_StopLocationFixRequest(handle_t hRrlp);


/** 
 * \fn     RRLP_Destroy 
 * \brief  Destroy NAVC RRLP module object
 * 
 *  The function deletes the RRLP module object
 * 
 * \note
 * \param	hRrlp - handle to RRLP object
 * \return	void
 * \sa      RRLP_Init
 */
void RRLP_Destroy(handle_t hRrlp);

#endif /* _NAVC_RRLP_H */


