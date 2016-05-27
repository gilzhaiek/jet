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

/** \file   navl_api.h 
 *  \brief  NAVL external api header definations
 * 
 * 
 */

#ifndef __NAVL_API_H
#define __NAVL_API_H

#define MAX_PAYLOAD 	9000

/* MsgClass */
enum
{
	NAVL_CLASS_INTERNAL,
	NAVL_CLASS_GPS,
	NAVL_CLASS_RR,
	NAVL_CLASS_SUPL,
	NAVL_CLASS_SUPL_ADS,
	NAVL_CLASS_WPL,
};


/* NAVL_MC_SPECIFICATION: MsgType */
enum 
{
	NAVL_MC_SPECIFICATION_MT_RRLP,
	NAVL_MC_SPECIFICATION_MT_RRLP_RESP,
	NAVL_MC_SPECIFICATION_MT_RRC,
	NAVL_MC_SPECIFICATION_MT_RRC_RESP
};

typedef struct 
{
	McpU8		Payload[MAX_PAYLOAD];
	
} NavlMsg_t;

typedef EMcpfRes (*tNavlSendCb) (handle_t, McpU8, McpU8, McpU16, McpU8 *);

/** 
 * \fn     NAVL_Create
 * \brief  Initialization of the NAVL layer
 * 
 *  Create memory pool, init structures, init semaphores
 * 
 * \note
 * \param	hMcpf	- handle to OS Framework
 * \return 	handle to initialized NAVL structure
 * \sa     	NAVL_Create
**/ 

handle_t NAVL_Create(handle_t hMcpf);


/** 
 * \fn     NAVL_Destroy
 * \brief  Destroys NAVL Module
 * 
 *  Free memory pool & structures
 * 
 * \note
 * \param	hNavl	- handle to NAVL
 * \return 	response codes
 * \sa     	NAVL_Destroy
**/ 

EMcpfRes NAVL_Destroy(handle_t hNavl);


/** 
 * \fn     NAVL_Open
 * \brief  Open communication channel
 * 
 *  register the task and callback
 * 
 * \note
 * \param	hNAVL	- handle to NAVL
 * \param 	hCaller - handle to calling task
 * \param	fCb - Callback function to be registered
 * \return	handle to NAVL client structure
 * \sa     	NAVL_Open
**/ 

handle_t NAVL_Open(handle_t hNAVL, handle_t hCaller, tNavlSendCb fCb);


/** 
 * \fn     NAVL_Close
 * \brief  Close NAVL Client Module
 * 
 *  Free memory & structures
 * 
 * \note
 * \param	hNavl_Client	- handle to NAVL Client
 * \return 	response codes
 * \sa     	NAVL_Close
**/ 

EMcpfRes NAVL_Close(handle_t hNavl_Client);


/** 
 * \fn     NAVL_Command
 * \brief  Send command to GPS task
 * 
 *  Sends command to GPS task.
 * 
 * \note
 * \param	hNavl_Client	- initialized client structure
 * \param	MsgClass	- Message class
 * \param	Opcode	- Opcode of message
 * \param	Param	- Message
 * \return 	response codes 
 * \
 * \sa     	NAVL_Command
**/ 

EMcpfRes NAVL_Command(handle_t hNavl_Client, 
					  McpU8 MsgClass, 
					  McpU8 Opcode, 
					  McpU16 Length,
					  NavlMsg_t *Param);


#endif /* __NAVL_API_H */	
