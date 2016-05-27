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
*   FILE NAME:      mcp_hal_memory.h
*
*   BRIEF:          This file defines Memory-related HAL utilities.
*
*   DESCRIPTION:    General
*
*   AUTHOR:         Udi Ron
*
\*******************************************************************************/

#ifndef __MCP_HAL_MEMORY_H
#define __MCP_HAL_MEMORY_H


/********************************************************************************
 *
 * Include files
 *
 *******************************************************************************/

#include "mcp_hal_types.h"
#include "mcp_hal_config.h"
#include "mcp_hal_defs.h"

/*-------------------------------------------------------------------------------
 * MCP_HAL_MEMORY_MemCopy()
 *
 * Brief:  
 *		Called by the stack to copy memory from one buffer to another.
 *
 * Description:
 *		Called by the stack to copy memory from one buffer to another.
 *     
 *		This function's implementation could use the ANSI memcpy function.
 *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *		dest [out] - Destination buffer for data.
 *
 *		source [in] - Source buffer for data. "dest" and "source" must not overlap.
 *
 *		numBytes [in] - Number of bytes to copy from "source" to "dest".
 *
 * Returns:
 *		void.
 * 
 */
void MCP_HAL_MEMORY_MemCopy(void *dest, const void *source, McpU32 numBytes);


/*-------------------------------------------------------------------------------
 * MCP_HAL_MEMORY_MemCmp()
 *
 * Brief:  
 *      Compare characters in two buffers.
 *
 * Description:
 *      Called by the stack to compare the bytes in two different buffers.
 *      If the buffers lengths or contents differ, this function returns FALSE.
 *
 *      This function's implementation could use the ANSI memcmp
 *      routine as shown:
 *
 *      return (len1 != len2) ? FALSE : (0 == memcmp(buffer1, buffer2, len2));
 *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *      buffer1 [in] - First buffer to compare.
 *
 *      len1 [in] - Length of first buffer to compare.
 *
 *      buffer2 [in] - Second buffer to compare.
 *
 *      len2 [in] - Length of second buffer to compare.
 *
 * Returns:
 *      TRUE - The lengths and contents of both buffers match exactly.
 *
 *      FALSE - Either the lengths or the contents of the buffers do not match.
 */
McpBool MCP_HAL_MEMORY_MemCmp(const void *buffer1, McpU16 len1, const void *buffer2, McpU16 len2);


/*-------------------------------------------------------------------------------
 * MCP_HAL_MEMORY_MemSet()
 *
 * Brief: 
 *      Sets buffers to a specified character.
 *
 * Description:
 *     Fills the destination buffer with the specified byte.
 *
 *     This function's implementation could use the ANSI memset
 *     function.
 *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *     dest [out] - Buffer to fill.
 *
 *     byte [in] - Byte to fill with.
 *
 *     len [in] - Length of the destination buffer.
 *
 * Returns:
 *		void.
 */
void MCP_HAL_MEMORY_MemSet(void *dest, McpU8 byte, McpU32 len);


/*-------------------------------------------------------------------------------
 * MCP_HAL_MEMORY_MemMalloc()
 *
 * Brief:  
 *	    Allocated memory according to a given size.
 *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *		size - the size (in bytes) to allocate in the memory.
 *
 * Returns:
 *		pointer to the allocated memory space.
 */
void *MCP_HAL_MEMORY_MemMalloc(int size);

/*-------------------------------------------------------------------------------
 * MCP_HAL_MEMORY_MemFree()
 *
 * Brief:  
 *	    Frees a memory space that was previously allocated using BTHAL_OS_Malloc.
 *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *		pointer to memory space to be freed.
 *
 */
void MCP_HAL_MEMORY_MemFree(void *ptr);


/*-------------------------------------------------------------------------------
 * MCP_HAL_MEMORY_MemMalloc()
 *
 * Brief:  
 *	    Allocated memory according to a given size.
 *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *		size - the size (in bytes) to allocate in the memory.
 *
 * Returns:
 *		pointer to the allocated memory space.
 */
void *MCP_HAL_MEMORY_MemMalloc(int size);

/*-------------------------------------------------------------------------------
 * MCP_HAL_MEMORY_MemFree()
 *
 * Brief:  
 *	    Frees a memory space that was previously allocated using BTHAL_OS_Malloc.
 *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *		pointer to memory space to be freed.
 *
 */
void MCP_HAL_MEMORY_MemFree(void *ptr);


#endif	/* __MCP_HAL_MEMORY_H */

