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
*   FILE NAME:      mcp_hal_endian.h
*
*   BRIEF:          This file defines Endian-related HAL utilities.
*
*   DESCRIPTION:    General
*
*   AUTHOR:         Udi Ron
*
\*******************************************************************************/

#ifndef __MCP_HAL_ENDIAN_H
#define __MCP_HAL_ENDIAN_H


/********************************************************************************
 *
 * Include files
 *
 *******************************************************************************/

#include "mcp_hal_types.h"
#include "mcp_hal_config.h"
#include "mcp_hal_defs.h"


/*---------------------------------------------------------------------------
 * MCP_ENDIAN_BEtoHost16()
 *
 * Brief:
 *	Convert a 16 bits number from Big Endian to Host 
 *
 * Description:  .
 *	Retrieve a 16-bit number from the given buffer. The number is in Big-Endian format.
 *	Converts it to host format and returns it to the caller.
 *
 * Parameters:
 *	
 * Return:
 *	16-bit number in Host's format
 */
McpU16 MCP_ENDIAN_BEtoHost16(const McpU8 *beBuff16);

/*---------------------------------------------------------------------------
 * MCP_ENDIAN_BEtoHost32()
 *
 * Brief:
 *	Convert a 32 bits number from Big Endian to Host 
 *
 * Description: 
 *	Retrieve a 32-bit number from the given buffer. The number is in Big-Endian format.
 *	Converts it to host format and returns it to the caller.
 *
 * Parameters:
 *	
 * Return:
 *	32-bit number in Host's format
 */
McpU32 MCP_ENDIAN_BEtoHost32(const McpU8 *beBuff32);

/*---------------------------------------------------------------------------
 * MCP_ENDIAN_LEtoHost16()
 *
 * Brief:
 *	Convert a 16 bits number from Little Endian to Host 
 *
 * Description: 
 *	Retrieve a 16-bit number from the given buffer. The number is in Little-Endian format.
 *	Converts it to host format and returns it to the caller.
 *
 * Parameters:
 *	
 * Return:
 *	16-bit number in Host's format
 */
McpU16 MCP_ENDIAN_LEtoHost16(const McpU8 *leBuff16);

/*---------------------------------------------------------------------------
 * MCP_ENDIAN_LEtoHost32()
 *
 * Brief:
 *	Convert a 32 bits number from Little Endian to Host 
 *
 * Description:  
 *	Retrieve a 32-bit number from the given buffer. The number is in Little-Endian format.
 *	Converts it to host format and returns it to the caller.
 *
 * Parameters:
 *	
 * Return:
 *	32-bit number in Host's format
 */
McpU32 MCP_ENDIAN_LEtoHost32(const McpU8 *leBuff32);

/*---------------------------------------------------------------------------
 * MCP_ENDIAN_HostToLE16()
 *
 * Brief:
 *	Convert a 16 bits number from Host to Little Endian
 *
 * Description: 
 *	Retrieve a 16-bit number from the given host value. The number is in host format.
 *	Converts it to Little-Endian format and stores it in the specified buffer in Little-Endian format.
 *
 * Parameters:
 *	
 * Return:
 *	void
 */
void MCP_ENDIAN_HostToLE16(McpU16 hostValue16, McpU8 *leBuff16);

/*---------------------------------------------------------------------------
 * MCP_ENDIAN_HostToLE32()
 *
 * Brief:
 *	Convert a 32 bits number from Host to Little Endian
 *
 * Description: 
 *	Retrieve a 32-bit number from the given host value. The number is in host format.
 *	Converts it to Little-Endian format and stores it in the specified buffer in Little-Endian format.
 *
 * Parameters:
 *	
 * Return:
 *	void
 */
void MCP_ENDIAN_HostToLE32(McpU32 hostValue32, McpU8 *leBuff32);

/*---------------------------------------------------------------------------
 * MCP_ENDIAN_HostToBE16()
 *
 * Brief:
 *	Convert a 16 bits number from Host to Little Endian
 *
 * Description: 
 *	Retrieve a 16-bit number from the given host value. The number is in host format.
 *	Converts it to Big-Endian format and stores it in the specified buffer in Big-Endian format.
 *
 * Parameters:
 *	
 * Return:
 *	void
 */
void MCP_ENDIAN_HostToBE16(McpU16 hostValue16, McpU8 *beBuff16);

/*---------------------------------------------------------------------------
 * MCP_ENDIAN_HostToBE32()
 *
 * Brief:
 *	Convert a 32 bits number from Host to Little Endian
 *
 * Description: 
 *	Retrieve a 32-bit number from the given host value. The number is in host format.
 *	Converts it to Big-Endian format and stores it in the specified buffer in Big-Endian format.
 *
 * Parameters:
 *	
 * Return:
 *	void
 */
void MCP_ENDIAN_HostToBE32(McpU32 hostValue32, McpU8 *beBuff32);

#endif	/* __MCP_HAL_ENDIAN_H */


