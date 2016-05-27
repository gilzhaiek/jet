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

#ifndef __MCPF_TIME_H
#define __MCPF_TIME_H

#ifdef __cplusplus
extern "C" {
#endif 

#include "mcpf_defs.h"
#include "mcpf_sll.h"

/** Definitions **/
typedef enum
{	
	TMR_STATE_ACTIVE,
	TMR_STATE_EXPIRED,
	TMR_STATE_DELETED
} EMcpfTimerState;


typedef void (*mcpf_timer_cb)(handle_t hCaller, McpUint uTimerId);


/** Structures **/
typedef struct
{	
	mcpf_timer_cb 	fCb;
	handle_t		hCaller;
} TMcpfTimerCb;

typedef struct
{
	TSllNodeHdr		tTimerListNode; /* Sorted linked List */
	McpU32      		uEexpiryTime;
	EmcpTaskId		eSource;
	EMcpfTimerState	eState;	
	TMcpfTimerCb		tCbData;
} TMcpfTimer;


/** APIs **/

/** 
 * \fn     mcpf_timer_start
 * \brief  Timer Start
 * 
 * This function will put the 'timer start' request in a sorted list, 
 * and will start an OS timer when necessary.
 * 
 * \note
 * \param	hMcpf     - MCPF handler.
 * \param	uExpiryTime - Requested expiry time in milliseconds (duration time).
 * \param	eTaskId - Source task id.
 * \param	fCb - Timer expiration Cb.
 * \param	hCaller - Cb's handler.
 * \return 	Pointer to the timer handler.
 * \sa     	mcpf_timer_start
 */ 
handle_t  	mcpf_timer_start (handle_t hMcpf, McpU32 uExpiryTime, EmcpTaskId eTaskId, 
								mcpf_timer_cb fCb, handle_t hCaller);

/** 
 * \fn     mcpf_timer_stop
 * \brief  Timer Stop
 * 
 * This function will remove the tiemr fro mteh list
 * and will stop the OS timer when necessary.
 * 
 * \note
 * \param	hMcpf     - MCPF handler.
 * \param	hTimer - Timer's handler.
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_timer_stop
 */  
EMcpfRes	 	mcpf_timer_stop (handle_t hMcpf, handle_t hTimer);

/** 
 * \fn     mcpf_getCurrentTime_inSec
 * \brief  Return time in seconds
 * 
 * This function will return the time in seconds from 1.1.1970.
 * 
 * \note
 * \param	hMcpf     - MCPF handler.
 * \return 	The time in seconds from 1.1.1970.
 * \sa     	mcpf_getCurrentTime_inSec
 */  
McpU32		mcpf_getCurrentTime_inSec(handle_t hMcpf);

/** 
 * \fn     mcpf_getCurrentTime_InMilliSec
 * \brief  Return time in milliseconds
 * 
 * This function will return the system time in milliseconds.
 * 
 * \note
 * \param	hMcpf     - MCPF handler.
 * \return 	The time in milliseconds from system's start-up (system time).
 * \sa     	mcpf_getCurrentTime_InMilliSec
 */  
McpU32		mcpf_getCurrentTime_InMilliSec(handle_t hMcpf);

/** 
 * \fn     mcpf_getSystemUpTime_InMilliSec
 * \brief  Return time in milliseconds
 * 
 * This function will return the system time in milliseconds.
 * 
 * \note
 * \param	hMcpf     - MCPF handler.
 * \return 	The time in milliseconds from system's start-up (system time).
 * \sa     	mcpf_getSystemUpTime_InMilliSec
 */  
McpU32		mcpf_getSystemUpTime_InMilliSec(handle_t hMcpf);

/** 
 * \fn     mcpf_handleTimer
 * \brief  Timer message handler
 * 
 * This function will be called upon de-queuing a message from the timer queue.
 * 
 * \note
 * \param	hMcpf - MCPF handler.
 * \param	*tMsg - The message containing the timer. 
 * \return 	None
 * \sa     	mcpf_handleTimer
 */  
void 	mcpf_handleTimer(handle_t hCaller, TmcpfMsg *tMsg);

#ifdef __cplusplus
}
#endif 

#endif

