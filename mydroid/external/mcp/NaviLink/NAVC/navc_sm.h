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

/** \file   navc_time.h 
 *  \brief  NAVC time module interface specification
 * 
 * This file contains functions to manage GPS coarse and fine time
 * 
 *  \see    navc_time.c
 */

#ifndef _NAVC_SM_H
#define _NAVC_SM_H

#include "navc_defs.h"


/************************************************************************
 * Defines
 ************************************************************************/
typedef enum  
{
	E_NAVC_EV_CREATE,
	E_NAVC_EV_DESTROY,
	E_NAVC_EV_START,
	E_NAVC_EV_STOP,
	E_NAVC_EV_COMPLETE,
	E_NAVC_EV_FAILURE,
	E_NAVC_EV_NUM
} ENavcSMEvent;
typedef enum  
{
	E_NAVC_ST_NULL,
	E_NAVC_ST_IDLE,
	E_NAVC_ST_HW_INIT,
	E_NAVC_ST_SW_INIT,
	E_NAVC_ST_READY,
	E_NAVC_ST_RUNNING,
	E_NAVC_ST_HW_DEINIT,
	E_NAVC_ST_ERROR,
	E_NAVC_ST_NUM
} ENavcSMState;

struct _TNavcSm;
typedef ENavcSMState (*FNavcSmStateHnd)(struct _TNavcSm*, ENavcSMEvent);

typedef struct _TNavcSm
{
	ENavcSMState currState;
	FNavcSmStateHnd stateHnd[E_NAVC_ST_NUM];
	handle_t hMcpf;
	handle_t hNavc;
	handle_t hGpsc;
	handle_t hNavcCcm;
	//CcmObj	*pNavcCcmObj;
	McpBool	bDestroy;
	McpBool	bInitialized;
} TNavcSm;

/************************************************************************
 * Types
 ************************************************************************/

/** 
 * \fn     NavcSmCreate
 * \brief  Initialize NAVC state machine and issue Create event
 * 
 * the function initialize the SM data structures and call the Create
 * Event handler. Upon complete the state will be NAVC_ST_IDLE. 
 * 
 * \note
 * \param	hNavc 	  - pointer to NAVC object
 * \param	hMcpf	  - pointer to MCPF object
 * \return	handle_t  - pointer to allocated SM context (NULL upon failure)
 * \sa
 */
handle_t NavcSmCreate(handle_t hMcpf, handle_t hNavc);

/** 
 * \fn     NavcSmDestroy
 * \brief  Destroy NAVC state machine
 * 
 * the function frees the SM data structure.
 * 
 * \note
 * \param	hNavcSm   - pointer to NAVC SM object
 * \return	void
 * \sa
 */
void NavcSmDestroy(handle_t hNavcSm);

/** 
 * \fn     navcSm
 * \brief  Send Event to NAVC state machine
 * 
 * The function is used to issue event to NAVC SM. It will process the relevant 
 * action and move to the next state.
 *
 * \note
 * \param	hNavcSm   - pointer to NAVC SM object
 * \param	event     - event to be issued
 * \return	void
 * \sa
 */
void NavcSm(handle_t hNavcSm, ENavcSMEvent event);



#endif /* _NAVC_TIME_H */


