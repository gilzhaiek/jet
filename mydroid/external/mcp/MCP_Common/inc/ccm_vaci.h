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
*   FILE NAME:      ccm_vac.h
*
*   BRIEF:          This file defines the internal API of the Connectivity Chip 
*                   Manager (CCM) Voice and Audio Control (VAC) component.
*                  
*
*   DESCRIPTION:    The CCM-VAC is used by different stacks audio and voice use-cases
*                   for synchronization and configuration of baseband resources, such
*                   as PCM and I2S bus and internal resources.
*
*   AUTHOR:         Ronen Kalish
*
\*******************************************************************************/
#ifndef __CCM_VACI_H__
#define __CCM_VACI_H__

#include "ccm_vac.h"
#include "ccm_vaci_configuration_engine.h"
#include "mcp_hal_defs.h"
#include "ccm_vaci_chip_abstration.h"

/*-------------------------------------------------------------------------------
 * CCM_VAC_StaticInit()
 *
 * Brief:  
 *      Initialize static data (related to all chips)
 *      
 * Description:
 *      Initialize data that is not unique pre chip
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      N/A
 *
 * Returns:
 *      CCM_VAC_STATUS_SUCCESS              - Initialization succeeded
 */
ECCM_VAC_Status CCM_VAC_StaticInit (void);
#define CCM_VAC_StaticInit() _CCM_VAC_ConfigurationEngine_StaticInit()

/*-------------------------------------------------------------------------------
 * CCM_VAC_Create()
 *
 * Brief:  
 *      Creates a VAC instance for a specific chip
 *      
 * Description:
 *      Initializes all VAC data relating to a specific chip
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      chipId      [in]     - the chip id for which the VAC object is created
 *      pCAL        [in]     - a pointer to the CAL object
 *	  pConfigParser	[in]	- A pointer to config parser object, including the VAC configuration file
 *      this        [out]    - the VAC object
 *
 * Returns:
 *      CCM_VAC_STATUS_SUCCESS              - Creation succeeded
 *      CCM_VAC_STATUS_FAILURE_UNSPECIFIED  - Creation failed
 */
ECCM_VAC_Status CCM_VAC_Create (McpHalChipId chipId,
                                Cal_Config_ID *pCal,
                                TCCM_VAC_Object **thisObj,
                                McpConfigParser 			*pConfigParser);
#define CCM_VAC_Create(_chipId, _pCAL, _thisObj,_pConfigParser) _CCM_VAC_ConfigurationEngine_Create((_chipId), (_pCAL), (_thisObj),(_pConfigParser))

/*-------------------------------------------------------------------------------
 * CCM_VAC_Configure()
 *
 * Brief:  
 *      Configures a VAC instance for a specific chip
 *      
 * Description:
 *      Match ini file information with chip capabilities
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      this        [in]     - the VAC object
 *
 * Returns:
 *      CCM_VAC_STATUS_SUCCESS                          - Configuration succeeded
 *      CCM_VAC_STATUS_FAILURE_INVALID_CONFIGURATION    - Configuration failed
 */
ECCM_VAC_Status CCM_VAC_Configure (TCCM_VAC_Object *thisObj);
#define CCM_VAC_Configure(_thisObj) _CCM_VAC_ConfigurationEngine_Configure(_thisObj);

/*-------------------------------------------------------------------------------
 * CCM_VAC_Destroy()
 *
 * Brief:  
 *      Destroies a VAC instance for a specific chip
 *      
 * Description:
 *      de-initializes all VAC data relating to a specific chip
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      this        [in]     - the VAC object
 *
 * Returns:
 *      N/A
 */
void CCM_VAC_Destroy (TCCM_VAC_Object **thisObj);
#define CCM_VAC_Destroy(_thisObj) _CCM_VAC_ConfigurationEngine_Destroy((_thisObj));

#endif /* __CCM_VACI_H__ */
