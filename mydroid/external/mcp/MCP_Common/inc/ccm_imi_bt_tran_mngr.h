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

#ifndef __CCM_IMI_BT_TRAN_MNGR_H
#define __CCM_IMI_BT_TRAN_MNGR_H

#include "mcp_hal_types.h"
#include "mcp_hal_os.h"
#include "mcp_config.h"
#include "mcp_defs.h"

#include "ccm_defs.h"
#include "ccm_config.h"

#include "ccm_imi_common.h"

typedef enum _tagCcmIm_BtTranMngr_Event
{
    _CCM_IM_BT_TRAN_MNGR_EVENT_TRAN_OFF,
    _CCM_IM_BT_TRAN_MNGR_EVENT_TRAN_ON_ABORT,
    _CCM_IM_BT_TRAN_MNGR_EVENT_TRAN_ON,

    _CCM_IM_NUM_OF_BT_TRAN_MNGR_EVENTS,
    _CCM_IM_INVALID_BT_TRAN_MNGR_EVENT
} _CcmIm_BtTranMngr_Event;

typedef enum _tagCcmIm_BtTranMngr_CompletionEventType
{
    _CCM_IM_BT_TRAN_MNGR_COMPLETED_EVENT_TRAN_OFF_COMPLETED,
    _CCM_IM_BT_TRAN_MNGR_COMPLETED_EVENT_TRAN_ON_COMPLETED,
    _CCM_IM_BT_TRAN_MNGR_COMPLETED_EVENT_TRAN_ON_ABORT_COMPLETED,
} _CcmIm_BtTranMngr_CompletionEventType;

typedef struct _tagCcmIm_BtTranMngr_CompletionEvent
{
    McpHalChipId                                chipId;
    CcmImStackId                                stackId;
    _CcmIm_BtTranMngr_CompletionEventType       eventType;
    _CcmImStatus                            completionStatus;
} _CcmIm_BtTranMngr_CompletionEvent;


typedef void (*_CcmIm_BtTranMngr_CompletionCb)(_CcmIm_BtTranMngr_CompletionEvent *event);

typedef struct _tagCcmIm_BtTranMngr_Obj _CcmIm_BtTranMngr_Obj;

_CcmImStatus _CCM_IM_BtTranMngr_StaticInit(void);

_CcmImStatus _CCM_IM_BtTranMngr_Create(void *hMcpf,
									   McpHalChipId chipId,
                                                    _CcmIm_BtTranMngr_CompletionCb  parentCb,
                                                    McpHalOsSemaphoreHandle         ccmImMutexHandle,
                                       _CcmIm_BtTranMngr_Obj **thisObj);
                                            
_CcmImStatus _CCM_IM_BtTranMngr_Destroy(_CcmIm_BtTranMngr_Obj **thisObj);

_CcmImStatus _CCM_IM_BtTranMngr_HandleEvent(    _CcmIm_BtTranMngr_Obj   *smData,
                                                            CcmImStackId                stackId,
                                                            _CcmIm_BtTranMngr_Event event);

void _CCM_IM_BtTranMngr_GetChipVersion(_CcmIm_BtTranMngr_Obj *thisObj,
                                       McpU16 *projectType,
                                       McpU16 *versionMajor,
                                       McpU16 *versionMinor);

handle_t *CCM_IM_BtTranMngr_GetCcmaObj(_CcmIm_BtTranMngr_Obj *smData);

const char *_CCM_IM_BtTranMngr_DebugEventStr(_CcmIm_BtTranMngr_Event event);

#endif

