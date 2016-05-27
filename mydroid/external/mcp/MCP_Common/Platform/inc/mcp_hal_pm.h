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
*   FILE NAME:      mcp_hal_pm.h
*
*   BRIEF:          This file defines the API of the HAL_PM module implementation.
*			    
*
*   DESCRIPTION:    General
*
*                   
*                   
*   AUTHOR:         Malovany Ram
*
\*******************************************************************************/

#ifndef __MCP_HAL_PM_H
#define __MCP_HAL_PM_H
 
/********************************************************************************
 *
 * Include files
 *
 *******************************************************************************/
#include "mcp_hal_defs.h"

/********************************************************************************
 *
 * Types
 *
 *******************************************************************************/
/*-------------------------------------------------------------------------------
 *  McpHalPmStatus
 */
typedef enum tagMcpHalPmStatus
{
	MCP_HAL_PM_STATUS_SUCCESS =              								MCP_HAL_STATUS_SUCCESS,	
	MCP_HAL_PM_STATUS_PENDING =              								MCP_HAL_STATUS_PENDING,
	MCP_HAL_PM_STATUS_FAILED =              								MCP_HAL_STATUS_FAILED

} McpHalPmStatus;


typedef struct _McpHalPmEventParms
{
	/*Operation Status */
	McpHalPmStatus 	 status;

	/* Chip Identification */
	McpHalChipId	chip_id;

	/* Transport Identification */
	McpHalTranId	tran_id;	

} McpHalPmEventParms;



/*-------------------------------------------------------------------------------
 * Halpmcallback type
 *
 *     A function of this type is called to indicate Mcp_Hal_pm events.
 */
typedef void (*McpHalPmCallback)(const McpHalPmEventParms* );

/*-------------------------------------------------------------------------------
 * McpHalPmHandler structure
 *
 *    	Represents Mcp_Hal_PM handler, which will receive notifications status
 *	From the Mcp_Hal_Pm module
 */
typedef struct _McpHalPmHandler
{	
	/* Pointer to callback function */
	McpHalPmCallback callback;	
} McpHalPmHandler;


/********************************************************************************
 *
 * Function declarations
 *
 *******************************************************************************/

/*-------------------------------------------------------------------------------
 * MCP_HAL_PM_Init()
 *
 * Brief: 
 *		Hal_Pm Power Management  module initialization.
 *		
 *
 * Description:
 *		
*		Hal_Pm Power Management  module initialization.
 *		
 *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *     	None.
 *
 * Returns:
 *		MCP_HAL_PM_STATUS_SUCCESS. - Operation is successful
 */
McpHalPmStatus MCP_HAL_PM_Init(void);

/*-------------------------------------------------------------------------------
 * MCP_HAL_PM_Deinit()
 *
 * Brief:  
 *		Hal_Pm Power Management  module deinitialization.
 *		
 *
 * Description:
 *		Hal_Pm Power Management  module deinitialization.
 *		
 *
 * Type:
 *		Synchronous 
 *
 * Parameters:
 *     	None.
 * Returns:
 *		MCP_HAL_PM_STATUS_SUCCESS - Operation is successful
 */
McpHalPmStatus MCP_HAL_PM_Deinit(void);

/*-------------------------------------------------------------------------------
 * MCP_HAL_PM_Register_Transport()
 *
 * Brief:  
 *		Register a transport client within Hal_Pm module according to the 
 *		chip_id and tran_id supplied by the user.
 *		
 *
 * Description:
 *		Register a transport client within Hal_Pm module according to the 
 *		chip_id and tran_id supplied by the user.
 *		
 *
 * Type:
 *		Synchronous or  Asynchronous
 *
 * Parameters:
 *     	chip_id [in] -Chip identification. 
 *     	tran_id [in] -Transport identification.
 *		handler [in] -handler struct containing a valid callback function.
 *						The 'callback' field must be set by the user.
 *
 * Returns:
 *		MCP_HAL_PM_STATUS_SUCCESS - if the initialization has been successfully
 *									   finished.
 * 		 
 *
 *		MCP_HAL_PM_STATUS_FAILED-  or any specific error defined in BthalStatus type,
 *								      if the initialization failed.
 */
McpHalPmStatus MCP_HAL_PM_RegisterTransport(McpHalChipId chip_id,McpHalTranId tran_id,McpHalPmHandler* handler);


/*-------------------------------------------------------------------------------
 * MCP_HAL_PM_Deregister_Transport()
 *
 * Brief:  
 *		Derigster the transport acorrding to the chip_id and tran_id supplied by
 *		 the user.
 *		
 *
 * Description:
 *		Derigster the transport acorrding to the chip_id and tran_id supplied by
 *		 the user.
 *		
 * 
 * Type:
 *		Synchronous.
 *
 * Parameters:
 *     	chip_id [in] -Chip identification. 
 *     	tran_id [in] -Transport identification.
 *
 * Returns:
 *		MCP_HAL_PM_STATUS_SUCCESS - if the deinitialization has been successfully
 *			finished.
 *
 *		MCP_HAL_PM_STATUS_FAILED or any specific error defined in BthalStatus type,
 *			if the deinitialization failed.
 */
McpHalPmStatus MCP_HAL_PM_DeregisterTransport(McpHalChipId chip_id,McpHalTranId tran_id);


/*-------------------------------------------------------------------------------
 * MCP_HAL_PM_Transport_Wakeup()
 *
 * Brief:  
 *		Chip and Transport  manager wakes up.
 *		
 *
 * Description:
 *		Chip and Transport  manager wakes up.
 *		
 * 
 * Type:
 *		Synchronous
 *
 * Parameters:
 *     	chip_id [in] -Chip identification. 
 *     	tran_id [in] -Transport identification.
 *
 * Returns:
 *		MCP_HAL_PM_STATUS_SUCCESS - if the wake up request has been
 *			succesfull.
 *
 *		MCP_HAL_PM_STATUS_FAILED - if the wake up request has been
 *			unsuccesfull.
 *
 *		MCP_HAL_PM_STATUS_PENDING - TBD need to implemnet Asynchronous behavior
 */
McpHalPmStatus MCP_HAL_PM_TransportWakeup(McpHalChipId chip_id,McpHalTranId tran_id);

/*-------------------------------------------------------------------------------
 * MCP_HAL_PM_Transport_Sleep()
 *
 * Brief:  
 *		Chip and Transport  manager go to sleep.
 *		
 *
 * Description:
 *		Chip and Transport  manager go to sleep.
 *		
 * 
 * Type:
 *		Synchronous.
 *
 * Parameters:
 *     	chip_id [in] -Chip identification. 
 *     	tran_id [in] -Transport identification.
 *
 * Returns:
 *		MCP_HAL_PM_STATUS_SUCCESS - if the sleep request has been
 *			succesfull.
 *
 *		MCP_HAL_PM_STATUS_FAILED or any specific error defined in BthalStatus type,
 *			if the deinitialization failed.
 *
 *		MCP_HAL_PM_STATUS_PENDING - TBD need to implemnet Asynchronous behavior
 */
McpHalPmStatus MCP_HAL_PM_TransportSleep(McpHalChipId chip_id,McpHalTranId tran_id);


#endif /* __MCP_HAL_PM_H */

