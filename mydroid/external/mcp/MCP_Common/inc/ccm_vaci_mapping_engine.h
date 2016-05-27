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
*   FILE NAME:      ccm_vaci_mapping_engine.h
*
*   BRIEF:          This file defines the internal API of the Connectivity Chip
*                   Manager (CCM) Voice and Audio Control (VAC) mapping
*                   engine component.
*                  
*
*   DESCRIPTION:    The mapping engine is a CCM-VAC internal module storing
*                   possible and current mapping between operations and 
*                   audio resources.
*
*   AUTHOR:         Ronen Kalish
*
\*******************************************************************************/
#ifndef __CCM_VACI_MAPPING_ENGINE_H__
#define __CCM_VACI_MAPPING_ENGINE_H__

#include "ccm_vac.h"
#include "mcp_hal_defs.h"
#include "mcp_config_parser.h"
#include "ccm_audio_types.h"

/*-------------------------------------------------------------------------------
 * TCCM_VAC_MEResourceList type
 *
 *     List of resources
 */
typedef struct _TCCM_VAC_MEResourceList
{
    ECAL_Resource   eResources[ CCM_VAC_MAX_NUM_OF_RESOURCES_PER_OP ];  /* list of resources */
    McpU32              uNumOfResources;                                    /* number of resources on list */
} TCCM_VAC_MEResourceList;

/* forward declarations */
typedef struct _TCCM_VAC_MappingEngine TCCM_VAC_MappingEngine;

/*-------------------------------------------------------------------------------
 * _CCM_VAC_MappingEngine_Create()
 *
 * Brief:  
 *      Creates a mapping engine object.
 *      
 * Description:
 *      Creates the mapping engine object. Read configuration from ini file
 *      (including default values) and capabilities from CAL to form the initial
 *      operation to resource mapping.
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      chipId          [in]    - The chip ID for which this object is created
 *      pConfigParser   [in]    - A config parser object, including the VAC configuration file
 *      ptMappEngine    [out]   - The mapping engine handle
 *
 * Returns:
 *      CCM_VAC_STATUS_SUCCESS              - creation succeeded
 *      CCM_VAC_STATUS_FAILURE_UNSPECIFIED  - creation failed
 */
ECCM_VAC_Status _CCM_VAC_MappingEngine_Create (McpHalChipId chipId,
                                               McpConfigParser *pConfigParser,
                                               TCCM_VAC_MappingEngine **ptMappEngine);

/*-------------------------------------------------------------------------------
 * _CCM_VAC_MappingEngine_Configure()
 *
 * Brief:  
 *      Configures a mapping engine object.
 *      
 * Description:
 *      Configures the mapping engine object. Read configuration from ini file,
 *      match with chip capabilities and initializes resource to allocation mapping
 *      accordingly.
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      ptMappEngine        [in]    - the mapping engine handle
 *      ptAvailResources    [in]    - the chip available resources
 *      ptAvailOperations   [in]    - the chip available operations
 *
 * Returns:
 *      CCM_VAC_STATUS_SUCCESS              - configuration succeeded
 *      CCM_VAC_STATUS_FAILURE_UNSPECIFIED  - configuration failed
 */
ECCM_VAC_Status _CCM_VAC_MappingEngine_Configure (TCCM_VAC_MappingEngine *ptMappEngine, 
                                                  TCAL_ResourceSupport *ptAvailResources,
                                                  TCAL_OperationSupport *ptAvailOperations);

/*-------------------------------------------------------------------------------
 * _CCM_VAC_MappingEngine_Destroy()
 *
 * Brief:  
 *      Destroy a mapping engine object.
 *      
 * Description:
 *      Destroy the mapping engine object. 
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      ptMappEngine    [in]    - the mapping engine handle
 *
 * Returns:
 *      N/A
 */
void _CCM_VAC_MappingEngine_Destroy (TCCM_VAC_MappingEngine **ptMappEngine);

/*-------------------------------------------------------------------------------
 * _CCM_VAC_MappingEngine_OperationToResourceList()
 *
 * Brief:  
 *      Returns the current resources mapped to an operation
 *      
 * Description:
 *      Searches current mapping for all resources currently mapped to an operation
 *      (whether it is running or not) and return it 
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      ptMappEngine    [in]    - the mapping engine handle
 *      eOperation      [in]    - the operation for which resource list is required
 *      ptResourceList  [out]   - a pointer to a resource list to be filled with 
 *                                required resources
 *
 * Returns:
 *      N/A
 */
void _CCM_VAC_MappingEngine_OperationToResourceList (TCCM_VAC_MappingEngine *ptMappEngine, 
                                                     ECAL_Operation eOperation,
                                                     TCAL_ResourceList *ptResourceList);

/*-------------------------------------------------------------------------------
 * _CCM_VAC_MappingEngine_GetOptionalResourcesList()
 *
 * Brief:  
 *      Returns the current optional resources used by an operation
 *      
 * Description:
 *      Searches current mapping for all optional resources (w/o their derived resources)
 *      mapped to an operation 
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      ptMappEngine    [in]    - the mapping engine handle
 *      eOperation      [in]    - the operation for which resource list is required
 *      ptResourceList  [out]   - a pointer to a resource list to be filled with 
 *                                optional resources
 *
 * Returns:
 *      N/A
 */
void _CCM_VAC_MappingEngine_GetOptionalResourcesList (TCCM_VAC_MappingEngine *ptMappEngine,
                                                      ECAL_Operation eOperation,
                                                      TCAL_ResourceList *ptResourceList);

/*-------------------------------------------------------------------------------
 * _CCM_VAC_MappingEngine_SetOptionalResourcesList()
 *
 * Brief:  
 *      Sets optional resources used by an operation
 *      
 * Description:
 *      Sets the optional resources to be used by an operation. Resources that
 *      were in use but are not included in the new list will be removed, whereas
 *      resources that that were not in use but are include in the list will be added 
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      ptMappEngine    [in]    - the mapping engine handle
 *      eOperation      [in]    - the operation for which resource list is required
 *      ptResourceList  [out]   - a pointer to a resource list including new optional
 *                                resources
 *
 * Returns:
 *      N/A
 */
void _CCM_VAC_MappingEngine_SetOptionalResourcesList (TCCM_VAC_MappingEngine *ptMappEngine,
                                                      ECAL_Operation eOperation,
                                                      TCAL_ResourceList *ptResourceList);

/*-------------------------------------------------------------------------------
 * _CCM_VAC_MappingEngine_GetConfigData()
 *
 * Brief:  
 *      Retrieves the mapping engine configuration object
 *      
 * Description:
 *      Retrieves the mapping engine configuration object, to be updated with
 *      chip capabilities
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      ptMappEngine    [in]    - the mapping engine handlee
 *
 * Returns:
 *      A pointer to the mapping configuration object
 */
TCAL_MappingConfiguration *_CCM_VAC_MappingEngine_GetConfigData (TCCM_VAC_MappingEngine *ptMappEngine);

#endif /* __CCM_VACI_MAPPING_ENGINE_H__ */

