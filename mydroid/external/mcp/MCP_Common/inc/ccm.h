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

#ifndef __CCM_H
#define __CCM_H

#include "mcpf_defs.h"
#include "mcp_hal_types.h"
#include "mcp_hal_defs.h"
#include "mcp_defs.h"
#include "ccm_defs.h"
#include "ccm_im.h"
#include "ccm_vac.h"


typedef struct tagCcmObj CcmObj;

CcmStatus CCM_StaticInit(void);

/*-------------------------------------------------------------------------------
 * CCM_Create()
 *
 * Brief:       
 *  Creates a CCM instance
 *
 * Description:
 *  Creates a new instance of the CCM. 
 *
 *  
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      chipId [in] - Identifies the chip that this instance is managing
 *      this [out] - pointer to the instance data
 *
 * Returns:
 *      CCM_STATUS_SUCCESS - Operation is successful.
 */
CcmStatus CCM_Create(void * hMcpf, McpHalChipId chipId, handle_t *thisObj);

CcmStatus CCM_Destroy(handle_t *thisObj);

#ifndef MCP_STU_ENABLE
CcmStatus CCM_Configure(CcmObj *thisObj);
#endif

CcmImObj *CCM_GetIm(CcmObj *thisObj);

TCCM_VAC_Object *CCM_GetVac(CcmObj *thisObj);

Cal_Config_ID *CCM_GetCAL(CcmObj *thisObj);

handle_t *CCM_GetCcmaObj(CcmObj *thisObj);

#endif  /* __CCM_H */

