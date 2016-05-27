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

#ifndef __CCM_IMI_COMMON_H
#define __CCM_IMI_COMMON_H

#include "mcp_hal_types.h"
#include "mcp_config.h"
#include "mcp_defs.h"

#include "ccm_defs.h"
#include "ccm_config.h"

#include "ccm_imi.h"

typedef McpUint _CcmImStatus;

#define _CCM_IM_STATUS_SUCCESS			((_CcmImStatus)CCM_IM_STATUS_SUCCESS)
#define _CCM_IM_STATUS_FAILED				((_CcmImStatus)CCM_IM_STATUS_FAILED)
#define _CCM_IM_STATUS_PENDING			((_CcmImStatus)CCM_IM_STATUS_PENDING)
#define _CCM_IM_STATUS_NO_RESOURCES		((_CcmImStatus)CCM_IM_STATUS_NO_RESOURCES)
#define _CCM_IM_STATUS_IN_PROGRESS		((_CcmImStatus)CCM_IM_STATUS_IN_PROGRESS)
#define _CCM_IM_STATUS_INVALID_PARM		((_CcmImStatus)CCM_IM_STATUS_INVALID_PARM)
#define _CCM_IM_STATUS_INTERNAL_ERROR	((_CcmImStatus)CCM_IM_STATUS_INTERNAL_ERROR)
#define _CCM_IM_STATUS_IMPROPER_STATE	((_CcmImStatus)CCM_IM_STATUS_IMPROPER_STATE)

#define _CCM_IM_FIRST_INTERNAL_STATUS		((_CcmImStatus)1000)

#define _CCM_IM_STATUS_NULL_SM_ENTRY		((_CcmImStatus)_CCM_IM_FIRST_INTERNAL_STATUS)

#define _CCM_IM_STATUS_INVALID_BT_HCI_VERSION_INFO		((_CcmImStatus)_CCM_IM_FIRST_INTERNAL_STATUS + 1)
#define _CCM_IM_STATUS_TRAN_ON_ABORTED					((_CcmImStatus)_CCM_IM_FIRST_INTERNAL_STATUS + 2)

/*
	Index of core (0 - 2) in State Machine
*/
typedef  McpUint _CcmIm_SmStackIdx;

#define _CCM_IM_NUM_OF_SM_STACK_IDXS	((_CcmIm_SmStackIdx)CCM_IM_MAX_NUM_OF_STACKS)
#define _CCM_IM_INVALID_SM_STACK_IDX		((_CcmIm_SmStackIdx)CCM_IM_INVALID_STACK_ID)

#endif

