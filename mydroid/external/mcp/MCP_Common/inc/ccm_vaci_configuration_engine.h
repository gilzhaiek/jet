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
*   FILE NAME:      ccm_vaci_configuration_engine.h
*
*   BRIEF:          This file defines the API of the Connectivity Chip Manager (CCM)
*                   Voice and Audio Control (VAC) component.
*                  
*
*   DESCRIPTION:    The CCM-VAC is used by different stacks audio and voice use-cases
*                   for synchronization and configuration of baseband resources, such
*                   as PCM and I2S bus and internal resources.
*
*   AUTHOR:         Ronen Kalish
*
\*******************************************************************************/
#ifndef __CCM_VAC_CONFIG_ENGINE_H__
#define __CCM_VAC_CONFIG_ENGINE_H__

#include "ccm_vac.h"
#include "mcp_hal_defs.h"
#include "ccm_audio_types.h"
#include "ccm_vaci_chip_abstration.h"

/* forward declarations */
typedef struct _TCCM_VAC_ConfigurationEngine TCCM_VAC_ConfigurationEngine;

/*-------------------------------------------------------------------------------
 * _CCM_VAC_ConfigurationEngine_StaticInit()
 *
 * Brief:  
 *      Initialize configuration engine static data (related to all chips)
 *      
 * Description:
 *      Initialize data that is not unique per chip
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
ECCM_VAC_Status _CCM_VAC_ConfigurationEngine_StaticInit (void);

/*-------------------------------------------------------------------------------
 * _CCM_VAC_ConfigurationEngine_Create()
 *
 * Brief:  
 *      Creates a configuration engine instance for a specific chip
 *      
 * Description:
 *      Initializes all configuration engine data relating to a specific chip
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      chipId      [in]     - the chip id for which the VAC object is created
 *      pCAL        [in]     - a pointer to the CAL object
 *	  pConfigParser [in]	- A pointer to config parser object, including the VAC configuration file
 *      this        [out]    - the configuration engine object
 *
 * Returns:
 *      CCM_VAC_STATUS_SUCCESS              - Creation succeeded
 *      CCM_VAC_STATUS_FAILURE_UNSPECIFIED  - Creation failed
 */
ECCM_VAC_Status _CCM_VAC_ConfigurationEngine_Create(McpHalChipId chipId, 
                                                    Cal_Config_ID *pCAL,
                                                    TCCM_VAC_ConfigurationEngine **thisObj,
								McpConfigParser 			*pConfigParser);

/*-------------------------------------------------------------------------------
 * _CCM_VAC_ConfigurationEngine_Configure()
 *
 * Brief:  
 *      Configures the configuration engine instance for a specific chip
 *      
 * Description:
 *      Match ini file content with capabilities read from chip
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      this        [in]     - the configuration engine object
 *
 * Returns:
 *      CCM_VAC_STATUS_SUCCESS                          - Configuration succeeded
 *      CCM_VAC_STATUS_FAILURE_INVALID_CONFIGURATION    - Configuration failed
 */
ECCM_VAC_Status _CCM_VAC_ConfigurationEngine_Configure (TCCM_VAC_ConfigurationEngine *thisObj);

/*-------------------------------------------------------------------------------
 * _CCM_VAC_ConfigurationEngine_Destroy()
 *
 * Brief:  
 *      Destroys a configuration engine instance for a specific chip
 *      
 * Description:
 *      de-initializes all configuration engine data relating to a specific chip
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      this        [in]     - the configuration engine object
 *
 * Returns:
 *      N/A
 */
void _CCM_VAC_ConfigurationEngine_Destroy (TCCM_VAC_ConfigurationEngine **thisObj);

/*-------------------------------------------------------------------------------
 * _CCM_VAC_ConfigurationEngine_RegisterCallback()
 *
 * Brief:  
 *      Registers an operation callback with the VAC configuration engine
 *      
 * Description:
 *      Registers a callback function to be used for an operation. Should be called
 *      for every operation before it is requested from the VAC.
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      ptConfigEngine  [in]    - the configuration engine handle
 *      eOperation      [in]    - the operation for which this callback should be used
 *      fCB             [in]    - the callback function
 *
 * Returns:
 *      N/A
 */
void _CCM_VAC_ConfigurationEngine_RegisterCallback (TCCM_VAC_ConfigurationEngine *ptConfigEngine,
                                                    ECAL_Operation eOperation, 
                                                    TCCM_VAC_Callback fCB);

/*-------------------------------------------------------------------------------
 * _CCM_VAC_ConfigurationEngine_StartOperation()
 *
 * Brief:  
 *      Request an operation to be started
 *      
 * Description:
 *      Request the VAC configuration engine to start an operation with a specific
 *      configuration. The VAC will attempt to allocate the required resources for 
 *      the operation and config them. If not all resources are available, the list
 *      of unavailable resources will be returned in ptUnavailResources.
 *
 * Type:
 *      Synchronous / A-Synchronous
 *
 * Parameters:
 *      ptConfigEngine      [in]    - the configuration engine handle
 *      eOperation          [in]    - the operation to start
 *      ptConfig            [in]    - configuration for this operation
 *      ptUnavailResources  [out]   - list of unavailable resources
 *
 * Returns:
 *      CCM_VAC_STATUS_SUCCESS                         - The operation has started successfully,
 *                                                       or was already running
 *      CCM_VAC_STATUS_PENDING                         - Operation start is pending and will be 
 *                                                       indicated by an event
 *      CCM_VAC_STATUS_FAILURE_UNAVAILABLE_RESOURCES   - Operation could not be started due to
 *                                                       resource(s) unavailability. Unavailable
 *                                                       resource(s) and their current owner(s) are
 *                                                       indicated by ptUnavailResources.
 *      CCM_VAC_STATUS_FAILURE_OPERATION_NOT_SUPPORTED - The requested operation is not supported
 *                                                       by the chip or the host
 *      CCM_VAC_STATUS_FAILURE_BUSY                    - A call to _CCM_VAC_ConfigurationEngine_StopOperation
 *                                                       to stop the opeartion is still pending
 *      CCM_VAC_STATUS_FAILURE_UNSPECIFIED             - Operation could not be started due to 
 *                                                       unspecified error (bug)
 */
ECCM_VAC_Status _CCM_VAC_ConfigurationEngine_StartOperation (TCCM_VAC_ConfigurationEngine *ptConfigEngine,
                                                             ECAL_Operation eOperation,
                                                             TCAL_DigitalConfig *ptConfig,
                                                             TCCM_VAC_UnavailResourceList *ptUnavailResources);

/*-------------------------------------------------------------------------------
 * _CCM_VAC_ConfigurationEngine_StopOperation()
 *
 * Brief:  
 *      Request an operation to be stopped
 *      
 * Description:
 *      Request the VAC configuration engine to stop an operation. The VAC
 *      will release all resources that were allocated to this operation.
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      ptConfigEngine      [in]    - the configuration engine handle
 *      eOperation          [in]    - the operation to stop
 *
 * Returns:
 *      CCM_VAC_STATUS_SUCCESS                         - The operation has stopped successfully
 *      CCM_VAC_STATUS_PENDING                         - Operation stop is pending and will be 
 *                                                       indicated by an event
 *      CCM_VAC_STATUS_FAILURE_OPERATION_NOT_SUPPORTED - The requested operation is not supported
 *                                                       by the chip or the host
 *      CCM_VAC_STATUS_FAILURE_UNSPECIFIED             - Operation could not be stopped due to 
 *                                                       unspecified error
 */
ECCM_VAC_Status _CCM_VAC_ConfigurationEngine_StopOperation (TCCM_VAC_ConfigurationEngine *ptConfigEngine,
                                                            ECAL_Operation eOperation);

/*-------------------------------------------------------------------------------
 * _CCM_VAC_ConfigurationEngine_ChangeResource()
 *
 * Brief:  
 *      Request to change resources used by an operation
 *      
 * Description:
 *      Request the VAC configuration engine to change current resources used by 
 *      an operation. If the operation is currently running, this will cause the 
 *      configuration engine to attempt to allocate the new resources and config them.
 *      If the new resources are available, mapping will be switched to the new 
 *      resources and they will be configured. If not all resource(s) are available, 
 *      the list of unavailable resources is returned in ptUnavailResources, and the 
 *      operation continue to run using the old resources. If the operation is not 
 *      running, mapping will be changed for next operation runs.
 *
 * Type:
 *      Synchronous / A-Synchronous
 *
 * Parameters:
 *      ptConfigEngine      [in]    - the configuration engine handle
 *      eOperation          [in]    - the operation to start
 *      eResourceMask       [in]    - new resources to use
 *      ptConfig            [in]    - configuration for this operation
 *      ptUnavailResources  [out]   - list of unavailable resources
 *
 * Returns:
 *      CCM_VAC_STATUS_SUCCESS                      - resources had been changed successfully
 *      CCM_VAC_STATUS_PENDING                      - resource change is pending and will be 
 *                                                    indicated by an event
 *      CCM_VAC_STATUS_FAILURE_UNAVAILABLE_RESOURCES- resources could not be changed due to
 *                                                    resource(s) unavailability. Unavailable
 *                                                    resource(s) and their current owner(s) are
 *                                                    indicated by ptUnavailResources.
 *      CCM_VAC_STATUS_FAILURE_BUSY -                 The operation is still being configured,
 *                                                    either due to a call to
 *                                                    _CCM_VAC_ConfigurationEngine_StartOperation,
 *                                                    _CCM_VAC_ConfigurationEngine_StopOperation,
 *                                                    _CCM_VAC_ConfigurationEngine_ChangeConfiguration
 *                                                    or this function
 *      CCM_VAC_STATUS_FAILURE_OPERATION_NOT_SUPPORTED - 
 *                                                    operation is not supported by chip or host
 *      CCM_VAC_STATUS_FAILURE_UNSPECIFIED          - resources could not be changed due to 
 *                                                    unspecified error (bug)
 */
ECCM_VAC_Status _CCM_VAC_ConfigurationEngine_ChangeResource (TCCM_VAC_ConfigurationEngine *ptConfigEngine,
                                                             ECAL_Operation eOperation, 
                                                             ECAL_ResourceMask eResourceMask, 
                                                             TCAL_DigitalConfig *ptConfig, 
                                                             TCCM_VAC_UnavailResourceList *ptUnavailResources);

/*-------------------------------------------------------------------------------
 * _CCM_VAC_ConfigurationEngine_ChangeConfiguration()
 *
 * Brief:  
 *      Request to change resources configuration for an operation
 *      
 * Description:
 *      Request the VAC configuration engine to change current resources' 
 *      configuration for a specific operation. 
 *
 * Type:
 *      Synchronous / A-Synchronous
 *
 * Parameters:
 *      ptConfigEngine      [in]    - the configuration engine handle
 *      eOperation          [in]    - the operation to configure
 *      ptConfig            [in]    - new configuration for this operation
 *
 * Returns:
 *      CCM_VAC_STATUS_SUCCESS                      - configuration has been changed successfully
 *      CCM_VAC_STATUS_PENDING                      - configuration change is pending and will be 
 *                                                    indicated by an event
 *      CCM_VAC_STATUS_FAILURE_BUSY -                 The operation is still being configured,
 *                                                    either due to a call to 
 *                                                    _CCM_VAC_ConfigurationEngine_StartOperation,
 *                                                    _CCM_VAC_ConfigurationEngine_StopOperation,
 *                                                    _CCM_VAC_ConfigurationEngine_ChangeResource
 *                                                    or this function
 *      CCM_VAC_STATUS_FAILURE_OPERATION_NOT_SUPPORTED - 
 *                                                    operation is not supported by chip or host
 *      CCM_VAC_STATUS_FAILURE_UNSPECIFIED          - resources configuration could not be 
 *                                                    changed due to unspecified error 
 *                                                    (input values)
 */
ECCM_VAC_Status _CCM_VAC_ConfigurationEngine_ChangeConfiguration (TCCM_VAC_ConfigurationEngine *ptConfigEngine,
                                                                  ECAL_Operation eOperation, 
                                                                  TCAL_DigitalConfig *ptConfig);

/*-------------------------------------------------------------------------------
 * _CCM_VAC_ConfigurationEngine_CalCb()
 *
 * Brief:  
 *      Callback function for Chip Abstraction Layer (CAL) events
 *      
 * Description:
 *      Called by the CAL to indicate configuration complete for a resource 
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      pUserData           [in]    - user data (configuration object handle and operation)
 *                                    passed in configuration request
 *      eRetValue           [in]    - the configuration status
 *
 * Returns:
 *      N/A
 */
void _CCM_VAC_ConfigurationEngine_CalCb (void *pUserData, ECAL_RetValue eRetValue);


/*-------------------------------------------------------------------------------
 * _CCM_VAC_ConfigurationEngine_SetResourceProperties()
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
 *      ptConfigEngine  [in]    - the configuration engine object handle
 *      eResource       [in]    - the resource to release
 *      pProperties     [in]    - new properties to keep
 *
 * Returns:
 *      N/A
 */

void _CCM_VAC_ConfigurationEngine_SetResourceProperties (TCCM_VAC_ConfigurationEngine *ptConfigEngine,
                                    ECAL_Resource eResource,
                                    TCAL_ResourceProperties *pProperties);

#endif /* __CCM_VAC_CONFIG_ENGINE_H__ */
