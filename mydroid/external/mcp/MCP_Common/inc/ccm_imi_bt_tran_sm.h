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

#ifndef __CCM_IMI_BT_TRAN_SM_H
#define __CCM_IMI_BT_TRAN_SM_H

#include "mcp_hal_types.h"
#include "mcp_hal_os.h"
#include "mcp_config.h"
#include "mcp_defs.h"

#include "ccm_defs.h"
#include "ccm_config.h"

#include "ccm_imi_common.h"

typedef enum _tagCcmIm_BtTranSm_Event
{
    _CCM_IM_BT_TRAN_SM_EVENT_TRAN_OFF,
    _CCM_IM_BT_TRAN_SM_EVENT_TRAN_ON_ABORT,
    _CCM_IM_BT_TRAN_SM_EVENT_TRAN_ON,
    
    _CCM_IM_BT_TRAN_SM_EVENT_TRAN_OFF_COMPLETE,
    _CCM_IM_BT_TRAN_SM_EVENT_TRAN_ON_COMPLETE,
    _CCM_IM_BT_TRAN_SM_EVENT_TRAN_ON_ABORT_COMPLETE,

    _CCM_IM_BT_TRAN_SM_EVENT_TRAN_ON_FAILED,
    
    _CCM_IM_BT_TRAN_SM_EVENT_NULL_EVENT,

    _CCM_IM_NUM_OF_BT_TRAN_SM_EVENTS,
    _CCM_IM_INVALID_BT_TRAN_SM_EVENT
} _CcmIm_BtTranSm_Event;

typedef enum _tagCcmIm_BtTranSm_State {
    _CCM_IM_BT_TRAN_SM_STATE_OFF,
    _CCM_IM_BT_TRAN_SM_STATE_ON,
    _CCM_IM_BT_TRAN_SM_STATE_OFF_IN_PROGRESS,
    _CCM_IM_BT_TRAN_SM_STATE_ON_IN_PROGRESS,
    _CCM_IM_BT_TRAN_SM_STATE_ON_ABORT_IN_PROGRESS,
    _CCM_IM_BT_TRAN_SM_STATE_ON_ABORTED_OFF_IN_PROGRESS,

    _CCM_IM_BT_TRAN_SM_STATE_ON_FAILED,
    
    _CCM_IM_NUM_OF_BT_TRAN_SM_STATES,
    _CCM_IM_INVALID_BT_TRAN_SM_STATE
} _CcmIm_BtTranSm_State;

typedef enum _tagCcmIm_BtTranSm_CompletionEventType
{
    _CCM_IM_BT_TRAN_SM_COMPLETED_EVENT_TRAN_OFF_COMPLETED,
    _CCM_IM_BT_TRAN_SM_COMPLETED_EVENT_TRAN_ON_COMPLETED,
    _CCM_IM_BT_TRAN_SM_COMPLETED_EVENT_TRAN_ON_ABORT_COMPLETED,
} _CcmIm_BtTranSm_CompletionEventType;

typedef struct _tagCcmIm_BtTranSm_CompletionEvent {
    McpHalChipId                            chipId;
    _CcmIm_BtTranSm_CompletionEventType eventType;
    _CcmImStatus                        completionStatus;
} _CcmIm_BtTranSm_CompletionEvent;


typedef void (*_CcmIm_BtTranSm_CompletionCb)(_CcmIm_BtTranSm_CompletionEvent *event);

typedef struct _tagCcmIm_BtTranSm_Obj _CcmIm_BtTranSm_Obj;

_CcmImStatus _CCM_IM_BtTranSm_StaticInit(void);

_CcmImStatus _CCM_IM_BtTranSm_Create(void *hMcpf,
									 McpHalChipId chipId,
                                     _CcmIm_BtTranSm_CompletionCb parentCb,
                                     McpHalOsSemaphoreHandle ccmImMutexHandle,
                                     _CcmIm_BtTranSm_Obj **thisObj);

_CcmImStatus _CCM_IM_BtTranSm_Destroy(_CcmIm_BtTranSm_Obj **thisObj);

_CcmImStatus _CCM_IM_BtTranSm_HandleEvent(_CcmIm_BtTranSm_Obj *smData, 
                                          _CcmIm_BtTranSm_Event event,
                                          void *eventData);

McpBool _CCM_IM_BtTranSm_IsInProgress(_CcmIm_BtTranSm_Obj *smData);

handle_t *CCM_IM_BtTranSm_GetCcmaObj(_CcmIm_BtTranSm_Obj *smData);

void _CCM_IM_BtTranSm_GetChipVersion(_CcmIm_BtTranSm_Obj *thisObj,
                                     McpU16 *projectType,
                                     McpU16 *versionMajor,
                                     McpU16 *versionMinor);

const char *_CCM_IM_BtTranSm_DebugStatetStr(_CcmIm_BtTranSm_State state);

const char *_CCM_IM_BtTranSm_DebugEventStr(_CcmIm_BtTranSm_Event event);

#endif

