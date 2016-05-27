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

/** \file   navc_rrc.h 
 *  \brief  RRC (Radio Resource Contorl Protocol) module interface specification
 * 
 * This file contains functions to decode RRC protcol messages related to position
 * measurement and assistance data received from the SMLC 
 * (Serving Mobile Location Centre) and to encode RRC messages tranmitted to SMLC.
 * 
 * 
 *  \see    navc_rrc.c
 */

#ifndef _NAVC_RRC_H
#define _NAVC_RRC_H

#include "gpsc_data.h"
#include "mcpf_defs.h"


/************************************************************************
 * Defines
 ************************************************************************/

#define RRC_MAX_SVS_ACQ_ASSIST 16

/************************************************************************
 * Types
 ************************************************************************/

/* RRC Measurement Types */
typedef enum
{
	RRC_REP_POS,
	RRC_REP_MEAS,
	RRC_REP_LOCERR

} ERrc_MeasrType;


/*** RRC MsrControl LocationError Error Reasons, UE-Positioning-ErrorCause ***/
typedef enum
{
  C_RRC_LOC_REASON_NO_ENF_OTDOA,     	/* notEnoughOTDOA-Cells 	*/
  C_RRC_LOC_REASON_NO_ENF_GPS_SAT,   	/* notEnoughGPS-Satellites	*/
  C_RRC_LOC_REASON_ASSIS_MIS,   		/* assistanceDataMissing	*/
  C_RRC_LOC_REASON_NO_ACC_GPS,       	/* notAccomplishedGPS-TimingOfCellFrames */ 
  C_RRC_LOC_REASON_UN_ERROR, 			/* undefinedError 			*/
  C_RRC_LOC_REASON_REQ_DENIED,  		/* requestDeniedByUser		*/
  C_RRC_LOC_REASON_NOT_PROCESSED,   	/* notProcessedAndTimeout- Unsupported Request method, location request.*/
  C_RRC_LOC_REASON_REF_CELL_NO_SERVE,   /* referenceCellNotServingCell */ 
  
  C_RRC_LOC_REASON_GOOD = 255    		/* NOT a LocationError code, used to indicate to NAVC 
										 * internal function that the mrsLocationResponse contains 
										 * valid position or measurement 
										 */
} ERrc_PosErrorCause;


/** 
 * \fn     RRC_Init
 * \brief  Initialize NAVC RRC module object
 * 
 * Allocate NAVC RRC object and initialize it
 * 
 * \note
 * \param	hMcpf     - handle to OS Framework
 * \param	hCmdPool  - handle to command message pool
 * \param	EmcpTaskId- source (parent) task ID
 * \param	uSrcQId   - source (parent) queue ID
 * \return	pointer to RRC created object or NULL in case of memory allocation error
 * \sa      RRC_Destroy
 */ 
handle_t RRC_Init (handle_t 	hMcpf,
				   handle_t 	hCmdPool,
				   handle_t	hEvtPool,
				   EmcpTaskId  	eSrcTaskId,
				   McpU8		uSrcQId);


/** 
 * \fn     RRC_processRxMsg
 * \brief  Process incoming RRC message
 * 
 *  The function decodes the RRC message and do the necessary functionality. 
 *  If the incoming RRC message is Measure Position Request then the MS sends Location info, 
 *  or GPS-Measureinfo or Location error accordingly. If the incoming RRC message is 
 *  Protocol error then the current location fix is stopped.
 * 
 * \note
 * \param	hRrc - handle to RRC object
 * \param	pMsg - pointer to RRC message buffer
 * \param	uLen - RRC message length
 * \param	uSystemTimeInMsec - system time of RRC message reception
 * \return	result of RRC decode: OK or Error  
 * \sa
 */
EMcpfRes RRC_processRxMsg (handle_t 	hRrc, 
						   McpU8 		*pMsg,
						   McpU16		uLen,
						   McpU32 		uSystemTimeInMsec);

/** 
 * \fn     RRC_encodeResponse
 * \brief  Encode RRC response message
 * 
 *  The function encodes the RRC message by specified message and error type
 * 
 * \note
 * \param	hRrc 			   - handle to RRC object
 * \param	p_zLocFixResponse  - pointer to structure containing location response
 * \param	pLocError  		   - pointer to structure containing error information
 * \param	uMeasType  		   - measurement type
 * \param	eLocReason  	   - location error reason
 * \param	pMsgBuf  		   - pointer to output data buffer containing RRC response
 * \param	pLen  		   - pointer to output data buffer containing RRC response
 * \return	length of encoded RRC message
 * \sa
 */
McpU16 RRC_encodeResponse (handle_t					hRrc,
						   T_GPSC_loc_fix_response	*p_zLocFixResponse, 
						   TNAVCD_LocError 			*pLocError, 
						   ERrc_MeasrType			eMeasType, 
						   ERrc_PosErrorCause		eLocReason, 
						   McpU8 					*pMsgBuf); 

/** 
 * \fn     RRC_StopLocationFixRequest 
 * \brief  Destroy NAVC RRC module object
 * 
 *  The function Stops the Ongoing Location Request
 * 
 * \note
 * \param	hRrc - handle to RRC object
 * \return	void
 * \sa      
 */
void RRC_StopLocationFixRequest(handle_t hRrc);


/** 
 * \fn     RRC_Destroy 
 * \brief  Destroy NAVC RRC module object
 * 
 *  The function deletes the RRC module object
 * 
 * \note
 * \param	hRrc - handle to RRC object
 * \return	void
 * \sa      RRC_Init
 */
void RRC_Destroy(handle_t hRrc);

#endif /* _NAVC_RRC_H */


