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


/** \file   navc_cmdHandler.h 
 *  \brief  NAVC stack command handler interface
 * 
 *   The file contains the NAVC command function interface.
 * NAVC command is passed from NAVC thread as MCPF message where
 * MCPF message opcode is set to required NAVC command opcode and data buffer
 * contains NAVC command parameters.
 * 
 * 
 */

#ifndef __NAVC_CMDHANDLER_H__
#define __NAVC_CMDHANDLER_H__

/************************************************************************
 * Defines
 ************************************************************************/


/************************************************************************
 * Macros
 ************************************************************************/


/************************************************************************
 * Types
 ************************************************************************/

/** 
 * \fn     NavcCmdHandler
 * \brief  Process NAVC command message
 * 
 * This function analyzes NAVC message command opcode, parses command 
 * parameters and executes the command
 * 
 * \note
 * \param	hNavc  - pointer to NAVC control object
 * \param	pMsg   - pointer to MCPF message containing NAVC command
 * \return 	Result of operation: OK or ERROR
 * \sa     	NavcEvtHandler
 */  
void NavcCmdHandler (handle_t hNavc, TmcpfMsg * pMsg);


/** 
 * \fn     navcCmd_stopCmdQueue 
 * \brief  Stop NAVC command queue
 * 
 * Stop NAVC commad queue and start guarding timer to re-enable queue.
 * The function is used to protect GPSC state machine from inconsistency
 * of asyncronously issued NAVC commands. The NAVC command may be stopped
 * when NAVC command requiring protection is received.
 * 
 * \note
 * \param	pNavc - handle to NAVC control object
 * \return 	void
 * \sa     	navcCmd_enableCmdQueue
 */ 
void NavcCmdDisableQueue (handle_t hNavc);

/** 
 * \fn     navcCmd_enableCmdQueue 
 * \brief  Enable NAVC command queue
 * 
 * Enable NAVC commad queue and stop guarding timer.
 * The function is used to protect GPSC state machine from inconsistency
 * of asyncronously issued NAVC commands. The NAVC re-enables command queue
 * after GPSC response/confirmation for the current command is received.
 * 
 * \note
 * \param	pNavc - handle to NAVC control object
 * \return 	void
 * \sa     	navcCmd_stopCmdQueue
 */ 
void NavcCmdEnableQueue (handle_t hNavc);

/** 
 * \fn     NavcCmdCompleteHandler 
 * \brief  Sends command complete indication to command requestor
 * 
 * Sends command complete event to specified destination task
 * 
 * \note
 * \param	pNavc - pointer to NAVC control object
 * \param	eResult  - result
 * \return 	void
 * \sa     	
 */ 
void NavcCmdCompleteHandler (const TNAVCD_Ctrl 	*pNavc, 
							 const EMcpfRes 		eResult);

void sendCmdCompleteEvt (const TNAVCD_Ctrl   *pNavc,
                                const EmcpTaskId    eDestTaskId,
                                const McpU8         uDestQId,
                                const EMcpfRes      eResult,
                                const ENAVC_cmdOpcode eComplCmd);

void NavcCheckandWakeupdevice(handle_t hNavc, McpU8 uOpcode);

#endif /*__NAVC_CMDHANDLER_H__*/

