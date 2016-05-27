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


/** \file   mcp_msg.h 
 *  \brief  MCPF message structure and API specification
 *
 *  \see    mcpf_msg.c
 */

#ifndef __MCPF_MSG_H
#define __MCPF_MSG_H

#ifdef __cplusplus
extern "C" {
#endif 

#include "mcpf_defs.h"

/** 
 * \fn     mcpf_SendMsg
 * \brief  Send MCPF message
 * 
 * Allocate MCPF message, populate it's fields and send to specified target queue of 
 * the destination task
 * 
 * \note
 * \param	hMcpf     - handle to OS Framework
 * \param	eDestTaskId - destination task ID
 * \param   uDestQId  - destination queue ID 
 * \param   eSrcTaskId- source task ID
 * \param   eSrcQId   - source queue ID to return response if any
 * \param   uOpcode   - message opcode
 * \param   uLen      - number of bytes in data buffer pointed by pData
 * \param   uUserDefined - user defined parameter
 * \param	pData		 - message data buffer
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_MsgqEnable, mcpf_MsgqDisable
 */ 
EMcpfRes	mcpf_SendMsg (handle_t		hMcpf,
						  EmcpTaskId	eDestTaskId,
						  McpU8			uDestQId,
						  EmcpTaskId	eSrcTaskId,
						  McpU8			uSrcQId,
						  McpU16		uOpcode,
						  McpU32		uLen,
						  McpU32		uUserDefined,
						  void 			*pData);

/** 
 * \fn     mcpf_SendPriorityMsg
 * \brief  Send MCPF High Priority message
 * 
 * Allocate MCPF message, populate it's fields and send to specified target queue of 
 * the destination task
 * 
 * \note
 * \param	hMcpf     - handle to OS Framework
 * \param	eDestTaskId - destination task ID
 * \param   uDestQId  - destination queue ID 
 * \param   eSrcTaskId- source task ID
 * \param   eSrcQId   - source queue ID to return response if any
 * \param   uOpcode   - message opcode
 * \param   uLen      - number of bytes in data buffer pointed by pData
 * \param   uUserDefined - user defined parameter
 * \param	pData		 - message data buffer
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_MsgqEnable, mcpf_MsgqDisable
 */ 
EMcpfRes	mcpf_SendPriorityMsg (handle_t		hMcpf,
						  EmcpTaskId	eDestTaskId,
						  McpU8			uDestQId,
						  EmcpTaskId	eSrcTaskId,
						  McpU8			uSrcQId,
						  McpU16		uOpcode,
						  McpU32		uLen,
						  McpU32		uUserDefined,
						  void 			*pData);
						  
/** 
 * \fn     mcpf_MsgqEnable
 * \brief  Enable message queue
 * 
 * Enable the destination queue, meaning that messages in this queue 
 * will be processed by destination task after this function call
 * 
 * \note
 * \param	hMcpf     - handle to OS Framework
 * \param	eDestTaskId - destination task ID
 * \param   uDestQId  - destination queue ID 
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_MsgqDisable
 */ 
EMcpfRes	mcpf_MsgqEnable (handle_t 	 hMcpf, 
							 EmcpTaskId eDestTaskId,
							 McpU8		 uDestQId);

/** 
 * \fn     mcpf_MsgqDisable
 * \brief  Enable message queue
 * 
 * Disable the destination queue, meaning that messages in this queue 
 * will not be processed by destination task after this function call
 * 
 * \note
 * \param	hMcpf     - handle to OS Framework
 * \param	eDestTaskId - destination task ID
 * \param   uDestQId  - destination queue ID 
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_MsgqEnable
 */ 
EMcpfRes	mcpf_MsgqDisable (handle_t 	 	hMcpf, 
							  EmcpTaskId 	eDestTaskId,
							  McpU8		 	uDestQId);

/** 
 * \fn     mcpf_SetEvent
 * \brief  Set event
 * 
 * Set the bit in the Events bitmap of the destination task
 * 
 * \note
 * \param	hMcpf	- handle to OS Framework
 * \param	eDestTaskId - destination task ID
 * \param	uEvent	- event index
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_MsgqEnable
 */ 
EMcpfRes	mcpf_SetEvent (handle_t		hMcpf, 
						   EmcpTaskId	eDestTaskId,
						   McpU32		uEvent);

/** 
 * \fn     mcpf_RegisterClientCb
 * \brief  Register external send message handler function
 * 
 * Register external send message handler function and allocates external Task ID
 * 
 * \note
 * \param	hMcpf	- handle to OS Framework
 * \param	pDestTaskId - pointer to destination task ID (return value)
 * \param	fCb			- function handler
 * \param	hCb			- function callback handler
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_UnRegisterClientCb
 */ 
EMcpfRes	mcpf_RegisterClientCb (handle_t			hMcpf,
								   McpU32			*pDestTaskId,
								   tClientSendCb	fCb,
								   handle_t		 	hCb);

/** 
 * \fn     mcpf_RegisterClientCb
 * \brief  Unregister MCPF external send message handler 
 * 
 * Unregister MCPF external send message handler for specified external destination task id 
 * and free the task id
 * 
 * \note
 * \param	hMcpf	- handle to OS Framework
 * \param	uDestTaskId - destination task ID
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_RegisterClientCb
 */ 

EMcpfRes	mcpf_UnRegisterClientCb (handle_t	hMcpf,
									 McpU32		uDestTaskId);


/** 
 * \fn     mcpf_EnqueueMsg 
 * \brief  Enqueue MCPF message
 * 
 * Enqueue MCPF message to destination queue of specified target task and 
 * set task's signal object
 * 
 * \note
 * \param	hMcpf     - handle to OS Framework
 * \param	eDestTaskId - destination task ID
 * \param   uDestQId  - destination queue ID 
 * \param	pMsg 	  - pointer to MCPF message
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_SendMsg
 */
EMcpfRes mcpf_EnqueueMsg (handle_t		hMcpf,
						  EmcpTaskId	eDestTaskId,
						  McpU8			uDestQId,
								  TmcpfMsg  	*pMsg);
/** 
 * \fn     mcpf_RequeueMsg 
 * \brief  Requeue MCPF message
 * 
 * Requeue MCPF message to the head of destination queue of specified target task and 
 * set task's signal object
 * 
 * \note
 * \param	hMcpf     - handle to OS Framework
 * \param	eDestTaskId - destination task ID
 * \param   uDestQId  - destination queue ID 
 * \param	pMsg 	  - pointer to MCPF message
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_SendMsg
 */
EMcpfRes mcpf_RequeueMsg (handle_t		hMcpf,
						  EmcpTaskId	eDestTaskId,
						  McpU8			uDestQId,
								  TmcpfMsg  	*pMsg);

#ifdef __cplusplus
}
#endif 

#endif /* __MCPF_MSG_H */

