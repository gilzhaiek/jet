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
*   FILE NAME:      ccm_vaci_allocation_engine.h
*
*   BRIEF:          This file defines the internal API of the Connectivity Chip
*                   Manager (CCM) Voice and Audio Control (VAC) allocation 
*                   engine component.
*                  
*
*   DESCRIPTION:    The Allocation engine is a CCM-VAC internal module storing
*                   audio resources allocations to operations.
*
*   AUTHOR:         Ronen Kalish
*
\*******************************************************************************/
#ifndef __CCM_VACI_ALLOC_ENGINE_H__
#define __CCM_VACI_ALLOC_ENGINE_H__

#include "ccm_vac.h"
#include "mcp_config_parser.h"
#include "ccm_audio_types.h"

/* forward declarations */
typedef struct _TCCM_VAC_AllocationEngine TCCM_VAC_AllocationEngine;

/*-------------------------------------------------------------------------------
 * _CCM_VAC_AllocationEngine_Create()
 *
 * Brief:  
 *      Creates an allocation engine object.
 *      
 * Description:
 *      Creates the allocation engine object.
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      chipId          [in]    - the chip ID for which this object is created
 *      pConfigParser   [in]    - a config parser object, including the VAC configuration file
 *      ptAllocEngine   [out]   - the allocation engine handle
 *
 * Returns:
 *      CCM_VAC_STATUS_SUCCESS              - creation succeeded
 *      CCM_VAC_STATUS_FAILURE_UNSPECIFIED  - creation failed
 */
ECCM_VAC_Status _CCM_VAC_AllocationEngine_Create (McpHalChipId chipId,
                                                  McpConfigParser *pConfigParser,
                                                  TCCM_VAC_AllocationEngine **ptAllocEngine);

/*-------------------------------------------------------------------------------
 * _CCM_VAC_AllocationEngine_Configure()
 *
 * Brief:  
 *      Configures an allocation engine object.
 *      
 * Description:
 *      Configures the allocation engine object. Read configuration from ini file,
 *      match with chip capabilities and initializes all resources accordingly.
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      ptAllocEngine       [in]    - the allocation engine handle
 *      ptAvailResources    [in]    - the chip available resources
 *      ptAvailOperations   [in]    - the chip available operations
 *
 * Returns:
 *      CCM_VAC_STATUS_SUCCESS              - configuration succeeded
 *      CCM_VAC_STATUS_FAILURE_UNSPECIFIED  - configuration failed
 */
ECCM_VAC_Status _CCM_VAC_AllocationEngine_Configure (TCCM_VAC_AllocationEngine *ptAllocEngine, 
                                                     TCAL_ResourceSupport *ptAvailResources,
                                                     TCAL_OperationSupport *ptAvailOperations);

/*-------------------------------------------------------------------------------
 * _CCM_VAC_AllocationEngine_Destroy()
 *
 * Brief:  
 *      Destroy an allocation engine object.
 *      
 * Description:
 *      Destroy an allocation engine object. 
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      ptAllocEngine    [in]    - the allocation engine handle
 *
 * Returns:
 *      N/A
 */
void _CCM_VAC_AllocationEngine_Destroy (TCCM_VAC_AllocationEngine **ptAllocEngine);

/*-------------------------------------------------------------------------------
 * _CCM_VAC_AllocationEngine_TryAllocate()
 *
 * Brief:  
 *      Attempts to allocate a resource.
 *      
 * Description:
 *      Attempts to allocate a resource for the specified operation. If the 
 *      resource is already allocated for an operation that cannot mutually allocate
 *      this resource, a negative reply is returned 
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      ptAllocEngine   [in]     - the allocation object handle
 *      eResource       [in]     - the resource to allocate
 *      peOperation     [in/out] - the operation requesting the resource / 
 *                                 the operation currently owning the resource (if any)
 *
 * Returns:
 *      MCP_TRUE    - the resource was allocated
 *      MCP_FALSE   - the resource is already allocated. Current owner is indicated 
 *                    by peOperation
 */
McpBool _CCM_VAC_AllocationEngine_TryAllocate (TCCM_VAC_AllocationEngine *ptAllocEngine,
                                               ECAL_Resource eResource, 
                                               ECAL_Operation *peOperation);

/*-------------------------------------------------------------------------------
 * _CCM_VAC_AllocationEngine_Release()
 *
 * Brief:  
 *      Release a resource.
 *      
 * Description:
 *      Release a resource that was previously allocated.
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      ptAllocEngine   [in]    - the allocation object handle
 *      eResource       [in]    - the resource to release
 *      eOperation      [in]    - the operation releasing the resource
 *
 * Returns:
 *      N/A
 */
void _CCM_VAC_AllocationEngine_Release (TCCM_VAC_AllocationEngine *ptAllocEngine,
                                        ECAL_Resource eResource,
                                        ECAL_Operation eOperation);

/*-------------------------------------------------------------------------------
 * _CCM_VAC_AllocationEngine_SetResourceProperties()
 *
 * Brief:  
 *      Set properties for a resource
 *      
 * Description:
 *      Set unique properties per resource, e.g. SCO handle, that may be used
 *      later for resource configuration
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      ptAllocEngine   [in]    - the allocation object handle
 *      eResource       [in]    - the resource to release
 *      pProperties     [in]    - new properties to keep
 *
 * Returns:
 *      N/A
 */
void _CCM_VAC_AllocationEngine_SetResourceProperties (TCCM_VAC_AllocationEngine *ptAllocEngine,
                                                      ECAL_Resource eResource,
                                                      TCAL_ResourceProperties *pProperties);

/*-------------------------------------------------------------------------------
 * _CCM_VAC_AllocationEngine_GetResourceProperties()
 *
 * Brief:  
 *      Get properties for a resource
 *      
 * Description:
 *      Get unique properties per resource, e.g. SCO handle, that may be used
 *      later for resource configuration
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      ptAllocEngine   [in]    - the allocation object handle
 *      eResource       [in]    - the resource to release
 *
 * Returns:
 *      The resource properties structure
 */
TCAL_ResourceProperties *_CCM_VAC_AllocationEngine_GetResourceProperties (TCCM_VAC_AllocationEngine *ptAllocEngine,
                                                                          ECAL_Resource eResource);

/*-------------------------------------------------------------------------------
 * _CCM_VAC_AllocationEngine_GetConfigData()
 *
 * Brief:  
 *      Retrieves the allocation engine configuration object
 *      
 * Description:
 *      Retrieves the allocation engine configuration object, to be updated with
 *      allowed opeartion pairs
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      ptAllocEngine   [in]    - the allocation object handle
 *
 * Returns:
 *      The allocation engine configuration object
 */
TCAL_OpPairConfig *_CCM_VAC_AllocationEngine_GetConfigData (TCCM_VAC_AllocationEngine *ptAllocEngine);

#endif /* __CCM_VACI_ALLOC_ENGINE_H__ */
