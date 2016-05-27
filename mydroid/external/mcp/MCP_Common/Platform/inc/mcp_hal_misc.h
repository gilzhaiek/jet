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
*   FILE NAME:      mcp_hal_misc.h
*
*   BRIEF:          This file defines Miscellaneous HAL utilities that are not
*                   part of any specific functionality (e.g, strings or memory).
*
*   DESCRIPTION:    General
*                   
*   AUTHOR:         Udi Ron
*
\******************************************************************************/

#ifndef __MCP_HAL_MISC_H
#define __MCP_HAL_MISC_H


/*******************************************************************************
 *
 * Include files
 *
 ******************************************************************************/
#include "mcp_hal_types.h"
#include "mcp_hal_config.h"
#include "mcp_hal_defs.h"


/*------------------------------------------------------------------------------
 * MCP_HAL_MISC_Srand()
 *
 * Brief:  
 *    Sets a random starting point
 *
 * Description:
 *	The function sets the starting point for generating a series of pseudorandom
 *  integers. To reinitialize the generator, use 1 as the seed argument. Any
 *  other value for seed sets the generator to a random starting point.
 *  MCP_HAL_MISC_Rand() retrieves the pseudorandom numbers that are generated. 
 *	Calling MCP_HAL_MISC_Rand() before any call to srand generates the same
 *  sequence as calling MCP_HAL_MISC_Srand() with seed passed as 1.
 *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *		seed [in] - Seed for random-number generation
 *
 * Returns:
 *     void
 */
void MCP_HAL_MISC_Srand(McpUint seed);

/*------------------------------------------------------------------------------
 * MCP_HAL_MISC_Rand()
 *
 * Brief:  
 *     Generates a pseudorandom number
 *
 * Description:
 *    Returns a pseudorandom integer in the range 0 to 65535. 
 *
 *	Use the MCP_HAL_MISC_Srand() function to seed the pseudorandom-number
 *  generator before calling rand.
 *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *		void.
 *
 * Returns:
 *     Returns a pseudorandom number, as described above.
 */
McpU16 MCP_HAL_MISC_Rand(void);


/*---------------------------------------------------------------------------
 * MCP_HAL_MISC_Assert()
 *
 * Brief: 
 *     Called by the stack to indicate that an assertion failed. 
 *
 * Description: 
 *     Called by the stack to indicate that an assertion failed. MCP_HAL_MISC_Assert
 *     should display the failed expression and the file and line number
 *     where the expression occurred.
 *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *     expression [in] - A string containing the failed expression.
 *
 *     file [in] - A string containing the file in which the expression occurred.
 *
 *     line [in] - The line number that tested the expression.
 *
 * Returns:
 *		void.
 */
void MCP_HAL_MISC_Assert(const char *expression, const char *file, McpU16 line);

#endif	/* __MCP_HAL_MISC_H */

