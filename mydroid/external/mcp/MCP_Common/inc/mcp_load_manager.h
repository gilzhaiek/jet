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
/******************************************************************************\
*
*   FILE NAME:      mcp_load_manager.c
*
*   BRIEF:          This file defines the API of the MCP Load Manager
*
*   DESCRIPTION:    General
*                   This file defines the API of the MCP Load Manager
*
*   AUTHOR:         Malovany Ram
*
\******************************************************************************/

#ifndef __MCP_LOAD_MANAGER_H__
#define __MCP_LOAD_MANAGER_H__

/*******************************************************************************
 *
 * Include files
 *
 ******************************************************************************/
#include "mcpf_defs.h"
#include "mcp_hal_defs.h"
#include "mcp_bts_script_processor.h"

/*******************************************************************************
 *
 * Types
 *
 ******************************************************************************/
/* Load Manager callback function */
typedef void (*TLoadMngrCB)(McpBtsSpStatus status, void *pUserData);


/*******************************************************************************
 *
 * Data Structures
 *
 ******************************************************************************/

/*******************************************************************************
 *
 * Function declarations
 *
 ******************************************************************************/
/*------------------------------------------------------------------------------
 * MCP_LoadMngr_Init()
 *
 * Brief:  
 *      Initialize Load Manager structs.
 *
 * Description:
 *      Initialize Load Manager structs.
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      N/A
 *
 * Returns:
 *      N/A
 */
void MCP_LoadMngr_Init(void);

/*------------------------------------------------------------------------------
 * MCP_LoadMngr_Deinit()
 *
 * Brief:  
 *      Deinitialize Load Manager structs.
 *
 * Description:
 *      Deinitialize Load Manager structs.
 *
 * Type:
 *      N/A
 *
 * Parameters:
 *      N/A
 *
 * Returns:
 *      N/A
 */
void MCP_LoadMngr_Deinit(void);

/*------------------------------------------------------------------------------
 * MCP_MCP_LoadMngr_Create()
 *
 * Brief:  
 *      Create the load manager state machine and register the BT_IF client.
 *
 * Description:
 *      Create the load manager state machine and register the BT_IF client.
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      ccmaObj [in] - pointer to CCMA object.
 *
 * Returns:
 *      N/A
 */
void MCP_LoadMngr_Create(handle_t ccmaObj);

/*------------------------------------------------------------------------------
 * MCP_LoadMngr_Destroy()
 *
 * Brief:  
 *      Deregister the BT_IF client.
 *
 * Description:
 *      Deregister the BT_IF client.
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      N/A
 *
 *
 * Returns:
 *      N/A
 */
void MCP_LoadMngr_Destroy(void);

/*------------------------------------------------------------------------------
 * MCP_LoadMngr_SetScript()
 *
 * Brief:  
 *      Set the script that need to be loaded.
 *
 * Description:
 *      Set the script that need to be loaded
 *      This function must be call before MCP_LoadMngr_LoadScript.
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      scriptName [in]     - Script file name (not including path)
 *      scriptLocation [in] - Script location (path)
 *
 * Returns:
 *      MCP_HAL_STATUS_SUCCESS - Operation success.
 *      MCP_HAL_STATUS_FAILED - Operation fail.
 */
McpHalStatus MCP_LoadMngr_SetScriptName (const char *scriptName,
                                         const McpUtf8 *scriptLocation);

/*-------------------------------------------------------------------------------
 * MCP_LoadMngr_LoadScript()
 *
 * Brief:  
 *      Load a script.
 *
 * Description:
 *      Load a script according to MCP_LoadMngr_SetScript function.
 *      This function must be call after calling MCP_LoadMngr_SetScript
 * Type:
 *      Synchronous / Asynchronous
 *
 * Parameters:
 *      fCB [in] - Client cllback function. 
 *      pUserData [in] - User data for the corrent command.
 *
 * Returns:
 *
 *      MCP_HAL_STATUS_PENDING - The load script operation was started 
 *          successfully.A MCP_BTS_SP_STATUS_SUCCESS event will be received 
 *          when the operation has been succesfully done.
 *          If the operation  failed McpBtsSpStatus other then
 *          MCP_BTS_SP_STATUS_SUCCESS and MCP_BTS_SP_STATUS_EXECUTION_ABORTED
 *          will be sent.
 *
 *      MCP_BTS_SP_STATUS_SUCCESS - The operation was successfuly
 *
 *      MCP_BTS_SP_STATUS_FAILED - The operation failed.
 */

McpHalStatus MCP_LoadMngr_LoadScript (TLoadMngrCB fCB , void *pUserData);

/*------------------------------------------------------------------------------
 * MCP_LoadMngr_StopLoadScript()
 *
 * Brief:  
 *      Abort the current script Load.
 *
 * Description:
 *      Abort the current script Load.
 *
 * Type:
 *      Synchronous / Asynchronous
 *
 * Parameters:
 *      fCB [in] - Client cllback function.
 *      pUserData [in] - User data for the corrent command.
 *
 * Returns:
 *
 *      MCP_HAL_STATUS_PENDING - The load script operation was started 
 *          successfully.A MCP_BTS_SP_STATUS_EXECUTION_ABORTED event will be
 *          received when the operation has been succesfully done.
 *          If the operation  failed  a diffrrent McpBtsSpStatus will be sent.
 *
 *      MCP_BTS_SP_STATUS_FAILED - There is no Client call back function.
 */
McpHalStatus MCP_LoadMngr_StopLoadScript (TLoadMngrCB fCB,void *pUserData);


/*------------------------------------------------------------------------------
 * MCP_LoadMngr_NotifyUnload()
 *
 * Brief:  
 *      Notify the Load manager that the script is unloaded
 *      du to radio off.
 *
 * Description:
 *      Notify the Load manager that the script is unloaded
 *      du to radio off.
 *
 * Type:
 *      Synchronous 
 *
 * Parameters:
 *      N/A
 *
 * Returns:
 *      N/A
 */
void MCP_LoadMngr_NotifyUnload (void);


#endif /* __MCP_LOAD_MANAGER_H__ */

