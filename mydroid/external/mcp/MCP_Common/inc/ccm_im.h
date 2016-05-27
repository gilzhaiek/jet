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
#ifndef __CCM_IM_H
#define __CCM_IM_H

#include "mcp_hal_types.h"
#include "mcp_config.h"
#include "mcp_defs.h"

#include "ccm_defs.h"
#include "ccm_config.h"

typedef enum
{
    CCM_IM_STATUS_SUCCESS           = CCM_STATUS_SUCCESS,
    CCM_IM_STATUS_FAILED                = CCM_STATUS_FAILED,
    CCM_IM_STATUS_PENDING           = CCM_STATUS_PENDING,
    CCM_IM_STATUS_NO_RESOURCES      = CCM_STATUS_NO_RESOURCES,
    CCM_IM_STATUS_IN_PROGRESS       = CCM_STATUS_IN_PROGRESS,
    CCM_IM_STATUS_INVALID_PARM      = CCM_STATUS_INVALID_PARM,
    CCM_IM_STATUS_INTERNAL_ERROR    = CCM_STATUS_INTERNAL_ERROR,
    CCM_IM_STATUS_IMPROPER_STATE    = CCM_STATUS_IMPROPER_STATE
} CcmImStatus;

/*
 *  CcmImStackId type
 *
 *  A logical identifier of a stack in the CCM IM
 */
typedef enum tagCcmImStackId {
    CCM_IM_STACK_ID_BT,
    CCM_IM_STACK_ID_FM,
    CCM_IM_STACK_ID_GPS,

    CCM_IM_MAX_NUM_OF_STACKS,
    CCM_IM_INVALID_STACK_ID
} CcmImStackId;

typedef enum tagCcmImStackState {
    CCM_IM_STACK_STATE_OFF,
    CCM_IM_STACK_STATE_OFF_IN_PROGRESS,
    CCM_IM_STACK_STATE_ON_IN_PROGRESS,
    CCM_IM_STACK_STATE_ON_ABORT_IN_PROGRESS,
    CCM_IM_STACK_STATE_ON,
    CCM_IM_STACK_FATAL_ERROR,

    CCM_IM_NUM_OF_STACK_STATES,
    CCM_IM_INVALID_STACK_STATE
} CcmImStackState;

typedef enum
{   
    CCM_IM_EVENT_TYPE_ON_COMPLETE,
    CCM_IM_EVENT_TYPE_ON_ABORT_COMPLETE,
    CCM_IM_EVENT_TYPE_OFF_COMPLETE
} CcmImEventType;

typedef struct tagCcmEvent
{
    CcmImStackId        stackId;
    CcmImEventType  type;
    CcmImStatus     status;
} CcmImEvent;

typedef void (*CcmImEventCb)(CcmImEvent *event);

/*
    Forward Decleration
*/
typedef struct tagCcmImObj CcmImObj;

typedef void* CCM_IM_StackHandle;

CcmImStatus CCM_IM_RegisterStack(CcmImObj           *thisObj, 
                                        CcmImStackId        stackId, 
                                        CcmImEventCb        callback, 
                                        CCM_IM_StackHandle  *stackHandle);

CcmImStatus CCM_IM_DeregisterStack(CCM_IM_StackHandle *stackHandle);

CcmImStatus CCM_IM_StackOn(CCM_IM_StackHandle stackHandle);
CcmImStatus CCM_IM_StackOnAbort(CCM_IM_StackHandle stackHandle);
CcmImStatus CCM_IM_StackOff(CCM_IM_StackHandle stackHandle);

CcmImObj *CCM_IM_GetImObj(CCM_IM_StackHandle stackHandle);

CcmImStackState CCM_IM_GetStackState(CCM_IM_StackHandle     *stackHandle);

void CCM_IM_GetChipVersion(CCM_IM_StackHandle *stackHandle,
                           McpU16 *projectType,
                           McpU16 *versionMajor,
                           McpU16 *versionMinor);

#endif  /* __CCM_IM_H */

