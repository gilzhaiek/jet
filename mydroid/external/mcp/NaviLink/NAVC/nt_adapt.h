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


/** \file   nt_adapt.h 
 *  \brief  NAVC Transport Adapter module API definition                                  
 * 
 *  
 * 
 * The NAVC Transport Adapter is OS and Bus independent module.  
 * 
 *
 *  \see    nt_adapt.c
 */

#ifndef __NTA_API_H__
#define __NTA_API_H__


/************************************************************************
 * Includes
 ************************************************************************/
#include "mcpf_defs.h"
#include "mcp_hal_st.h"

/************************************************************************
 * Defines
 ************************************************************************/
#define NTA_RX_BUF_SIZE	4096	
#define NTA_RX_POOL_SIZE	80

#define NTA_RX_SHORT_BUF_SIZE	200
#define NTA_RX_SHORT_POOL_SIZE	100

typedef enum
{
	NTA_STATE_RX_GET_SYNC = 1,
	NTA_STATE_RX_GET_DATA,
	NTA_STATE_RX_GET_DATA_DLE,
	NTA_STATE_RX_CONGESTION,
	NTA_STATE_RX_MAX
} ENtaRxState;

/************************************************************************
 * Functions
 ************************************************************************/

/** 
 * \fn     NTA_Init 
 * \brief  Init the NAVC Transport Adapter module
 * 
 * Allocates internal data and registers channels to the transport layer.
 * 
 * \note
 * \param	hMCpf - handle to MCPF
 * \param	uPortNum - Port Number
 * \return 	Returns the status of operation: OK or Error
 * \sa     	NTA_Init
 */ 
EMcpfRes NTA_Init (handle_t hMcpf, McpInt uPortNum);

/** 
 * \fn     NTA_Open
 * \brief  Opens the 'tigps' char device
 * 
 * Opens the 'tigps' char device.
 * 
 * \note
 * \param	hMCpf - handle to MCPF
 * \return 	Returns the status of operation: OK or Error
 * \sa     	NTA_Open
 */ 
EMcpfRes NTA_Open ();

/** 
 * \fn     NTA_Close 
 * \brief  Closes the 'tigps' char device
 * 
 * Closes the 'tigps' char device
 * 
 * \note
 * \return 	Returns the status of operation: OK or Error
 * \sa     	NTA_Close
 */ 
EMcpfRes NTA_Close ();

/** 
 * \fn     NTA_Destroy 
 * \brief  Destroy the NAVC Transport Adapter module
 * 
 * Frees internal data.
 * 
 * \note
 * \return 	Returns the status of operation: OK or Error
 * \sa     	NTA_Destroy
 */ 
EMcpfRes NTA_Destroy ();


/** 
 * \fn     NTA_SendData
 * \brief  NAVC Transport Adapter transmit data function
 * 
 *  Transmit packet to char device
 * 
 * \note
 * \param	pBuf 	- pointer to the data to transmit
 * \param	len	 	- data length
 * \return 	Returns the status of operation: OK or Error
 * \sa      NTA_SendData
 */ 
EMcpfRes NTA_SendData (McpU8  *pBuf, McpU16 len);


/** 
 * \fn     NTA_EvtHndl
 * \brief  NAVC Transport Adapter Event indication function 
 * 
 *  Indication that a packet is received from the char device or a Tx Complete
 * 
 * \note
 * \param	hHandleCb 	- handler to the calling module handle (to fit RX Indication CB type - not used)
 * \param	eEvent 		-Indicates the event received (Rx Ind & Tx Cmplt are relevent here)
 * \return 	None
 * \sa     	NTA_EvtHndl
 */ 
void NTA_EvtHndl (handle_t hHandleCb, EHalStEvent eEvent);

/** 
 * \fn     NTA_SetBaudRate
 * \brief  NAVC Transport Adapter Set Baud Rate function
 * 
 *  Set the UART Baud Rate
 * 
 * \note
 * \param	speed 	- the new baud rate speed
 * \return  None
 * \sa     	NTA_SetBaudRate
 */ 
void NTA_SetBaudRate (McpU16 speed);

#endif /*__NTA_API_H__*/
