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

#ifndef __CCM_IMI_BT_TRAN_OFF_SM_H
#define __CCM_IMI_BT_TRAN_OFF_SM_H

#include "mcp_hal_types.h"
#include "mcp_config.h"
#include "mcp_defs.h"

#include "ccm_defs.h"
#include "ccm_config.h"

#include "ccm_imi_common.h"
#include "mcp_bts_script_processor.h"

typedef enum _tagCcmIm_BtTranOffSm_Event
{
    _CCM_IM_BT_TRAN_OFF_SM_EVENT_START,
    _CCM_IM_BT_TRAN_OFF_SM_EVENT_HCI_IF_OFF_COMPLETE,
    _CCM_IM_BT_TRAN_OFF_SM_EVENT_NULL_EVENT,
    _CCM_IM_NUM_OF_BT_TRAN_OFF_EVENTS,
    _CCM_IM_INVALID_BT_TRAN_OFF_EVENT
} _CcmIm_BtTranOffSm_Event;

typedef enum _tagCcmIm_BtTranOffState
{
    _CCM_IM_BT_TRAN_OFF_SM_STATE_NONE,
    _CCM_IM_BT_TRAN_OFF_SM_STATE_TURN_HCI_OFF,
    _CCM_IM_NUM_OF_BT_TRAN_OFF_STATES,
    _CCM_IM_INVALID_BT_TRAN_OFF_STATE
} _CcmIm_BtTranOffSm_State;

typedef struct _tagCcmIm_BtTranOffSm_CompletionEvent
{
    McpHalChipId        chipId;
    _CcmImStatus    completionStatus;
} _CcmIm_BtTranOffSm_CompletionEvent;


typedef void (*_CcmIm_BtTranOffSm_CompletionCb)(_CcmIm_BtTranOffSm_CompletionEvent *event);

typedef struct _tagCcmIm_BtTranOffSm_Obj _CcmIm_BtTranOffSm_Obj;

_CcmImStatus _CCM_IM_BtTranOffSm_StaticInit(void);

_CcmImStatus _CCM_IM_BtTranOffSm_Create(    McpHalChipId                        chipId,
                                        handle_t ccmaObj,
                                                    _CcmIm_BtTranOffSm_CompletionCb parentCb,
                                                    McpHalOsSemaphoreHandle         ccmImMutexHandle,
                                        _CcmIm_BtTranOffSm_Obj **thisObj);
                                            
_CcmImStatus _CCM_IM_BtTranOffSm_Destroy(_CcmIm_BtTranOffSm_Obj **thisObj);

_CcmImStatus _CCM_IM_BtTranOffSm_HandleEvent(   _CcmIm_BtTranOffSm_Obj      *smData, 
                                                            _CcmIm_BtTranOffSm_Event        event,
                                                            void                            *eventData);

const char *_CCM_IM_BtTranOffSm_DebugStatetStr(_CcmIm_BtTranOffSm_State state);

const char *_CCM_IM_BtTranOffSm_DebugEventStr(_CcmIm_BtTranOffSm_Event event);

#endif

