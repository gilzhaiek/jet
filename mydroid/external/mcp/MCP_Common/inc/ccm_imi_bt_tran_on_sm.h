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
/*******************************************************************************\
*
*   FILE NAME:      ccm_imi_bt_tran_on_sm.h
*
*   BRIEF:          This file defines the internal interface of the CCM Init Manager BT Tranport On State Machine
*
*   DESCRIPTION:
*   This module is responsible for turning on the shared BT transport.
*   
*   To make it portable, it uses bt_hci_if 
*
*   AUTHOR:   Udi Ron
*
\*******************************************************************************/
#ifndef __CCM_IMI_BT_TRAN_ON_SM_H
#define __CCM_IMI_BT_TRAN_ON_SM_H

#include "mcp_hal_types.h"
#include "mcp_config.h"
#include "mcp_defs.h"

#include "ccm_defs.h"
#include "ccm_config.h"

#include "ccm_imi_common.h"
#include "mcp_bts_script_processor.h"

typedef enum _tagCcmIm_BtTranOnSm_Event
{
    _CCM_IM_BT_TRAN_ON_SM_EVENT_START,
    _CCM_IM_BT_TRAN_ON_SM_EVENT_HCI_IF_ON_COMPLETE,
    _CCM_IM_BT_TRAN_ON_SM_EVENT_HCI_IF_ON_FAILED,
    _CCM_IM_BT_TRAN_ON_SM_EVENT_SET_TRAN_PARMS_COMPLETE,
    _CCM_IM_BT_TRAN_ON_SM_EVENT_HCI_COMMMAND_COMPLETE,
    _CCM_IM_BT_TRAN_ON_SM_EVENT_SCRIPT_EXEC_COMPLETE,
    _CCM_IM_BT_TRAN_ON_SM_EVENT_ABORT,

    _CCM_IM_BT_TRAN_ON_SM_EVENT_NULL_EVENT,

    _CCM_IM_NUM_OF_BT_TRAN_ON_EVENTS,
    _CCM_IM_INVALID_BT_TRAN_ON_EVENT
} _CcmIm_BtTranOnSm_Event;

typedef enum _tagCcmIm_BtTranOnState {
    _CCM_IM_BT_TRAN_ON_SM_STATE_NONE,
    _CCM_IM_BT_TRAN_ON_SM_STATE_TURN_HCI_ON,
    _CCM_IM_BT_TRAN_ON_SM_STATE_ABORT_TURN_HCI_ON,
    _CCM_IM_BT_TRAN_ON_SM_STATE_READ_VERSION,
    _CCM_IM_BT_TRAN_ON_SM_STATE_ABORT_READ_VERSION,
    _CCM_IM_BT_TRAN_ON_SM_STATE_EXECUTING_SCRIPT,
    _CCM_IM_BT_TRAN_ON_SM_STATE_ABORT_EXECUTING_SCRIPT,

    _CCM_IM_NUM_OF_BT_TRAN_ON_STATES,
    _CCM_IM_INVALID_BT_TRAN_ON_STATE
} _CcmIm_BtTranOnSm_State;

typedef struct _tagCcmIm_BtTranOnSm_CompletionEvent {
    McpHalChipId        chipId;
    _CcmImStatus    completionStatus;
} _CcmIm_BtTranOnSm_CompletionEvent;


typedef void (*_CcmIm_BtTranOnSm_CompletionCb)(_CcmIm_BtTranOnSm_CompletionEvent *event);

typedef struct _tagCcmIm_BtTranOnSm_Obj _CcmIm_BtTranOnSm_Obj;

/*-------------------------------------------------------------------------------
 * _CCM_IM_BtTranOnSm_StaticInit()
 *
 * Brief:       
 *  Static initializations that are common to all CCM-IM instances
 *
 * Description:
 *  This function must be called once before creating CCM-IM instances
 *
 *  The function is called by the CCM when it is statically initialized
 *
 * Type:
 *      Synchronous
 *
 * Parameters:  void
 *
 * Returns:
 *      CCM_STATUS_SUCCESS - Operation is successful.
 *
 *      CCM_STATUS_INTERNAL_ERROR - A fatal error occurred
 */
_CcmImStatus _CCM_IM_BtTranOnSm_StaticInit(void);

/*-------------------------------------------------------------------------------
 * _CCM_IM_BtTranOnSm_Create()
 *
 * Brief:       
 *  Creates a CCM-IM BT Tran On SM instance
 *
 * Description:
 *  Creates a new instance of the CCM-IM for a chip.
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      chipId [in] - Identifies the chip that this instance is managing
 *      ccmaObj [in] - pointer to CCMA
 *      parentCb [in] - Parent's callback function for event notification
 *      ccmImMutexHandle [in] - CCM IM Mutex Handle
 *      thisObj [out] - pointer to the instance data
 *
 * Returns:
 *      CCM_STATUS_SUCCESS - Operation is successful.
 *
 *      CCM_STATUS_INTERNAL_ERROR - A fatal error occurred
 */
_CcmImStatus _CCM_IM_BtTranOnSm_Create( McpHalChipId                        chipId,
                                       handle_t ccmaObj,
                                                    _CcmIm_BtTranOnSm_CompletionCb  parentCb,
                                                    McpHalOsSemaphoreHandle         ccmImMutexHandle,
                                       _CcmIm_BtTranOnSm_Obj **thisObj);
                                            
/*-------------------------------------------------------------------------------
 * CCM_IMI_Destroy()
 *
 * Brief:       
 *  Destroys a CCM-IM BT Tran On SM instance
 *
 * Description:
 *  Destroys an existing CCM-IM BT Tran On SM instance.
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      thisObj [in / out] - pointer to the instance data. Set to null on exit
 *
 * Returns:
 *      CCM_STATUS_SUCCESS - Operation is successful.
 *
 *      CCM_STATUS_INTERNAL_ERROR - A fatal error occurred
 */
_CcmImStatus _CCM_IM_BtTranOnSm_Destroy(_CcmIm_BtTranOnSm_Obj **thisObj);

/*-------------------------------------------------------------------------------
 * _CCM_IM_BtTranOnSm_HandleEvent()
 *
 * Brief:       
 *  Sends an event to the state machine engine
 *
 * Description:
 *  This function is called to send a new event to the state machine. The event is handled according
 *  to the current state and the sent event.
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      smData [in] - instance pointer
 *      event [in] - Sent Event
 *      eventData [in] - Data that accompanies the event. NULL if not applicable
 *
 * Returns:
 *      CCM_STATUS_SUCCESS - Operation is successful.
 *
 *      CCM_STATUS_INTERNAL_ERROR - A fatal error occurred
 */
_CcmImStatus _CCM_IM_BtTranOnSm_HandleEvent(    _CcmIm_BtTranOnSm_Obj       *smData, 
                                                            _CcmIm_BtTranOnSm_Event     event,
                                                            void                            *eventData);

void _CCM_IM_BtTranOnSm_GetChipVersion(_CcmIm_BtTranOnSm_Obj *thisObj, 
                                       McpU16 *projectType,
                                       McpU16 *versionMajor,
                                       McpU16 *versionMinor);

const char *_CCM_IM_BtTranOnSm_DebugStatetStr(_CcmIm_BtTranOnSm_State state);

const char *_CCM_IM_BtTranOnSm_DebugEventStr(_CcmIm_BtTranOnSm_Event event);

#endif

