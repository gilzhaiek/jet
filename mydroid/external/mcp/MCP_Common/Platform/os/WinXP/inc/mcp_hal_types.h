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
*   FILE NAME:      mcp_hal_types.h
*
*   BRIEF:    		Definitions for basic basic HAL Types.
*
*   DESCRIPTION:	This file defines the BASIC hal types. These would be used
*					as base types for upper layers
*
*   AUTHOR:         Udi Ron
*
\*******************************************************************************/

#ifndef __MCP_HAL_TYPES_H
#define __MCP_HAL_TYPES_H

/* -------------------------------------------------------------
 *					Platform-Depndent Part							
 *																
 * 		SET THE VALUES OF THE FOLLOWING PRE-PROCESSOR 			
 *		DEFINITIONS 	TO THE VALUES THAT APPLY TO THE 				
 *		TARGET PLATFORM											
 *																
 */
#include "pla_os_types.h"

/* -------------------------------------------------------------
 *					8 Bits Types
 */
#if MCP_CHAR_SIZE == 1

typedef unsigned char	 	McpU8;
typedef signed char 		McpS8;

#elif MCP_SHORT_SIZE == 1

typedef unsigned short 	McpU8;
typedef          short 		McpS8;

#elif MCP_INT_SIZE == 1

typedef unsigned int 		McpU8;
typedef          int 		McpS8;

#else

#error Unable to define 8-bits basic types!

#endif

/* -------------------------------------------------------------
 *					16 Bits Types
 */
#if MCP_SHORT_SIZE == 2

typedef unsigned short 	McpU16;
typedef          short 		McpS16;

#elif MCP_INT_SIZE == 2

typedef unsigned int 		McpU16;
typedef          int 		McpS16;

#else

#error Unable to define 16-bits basic types!

#endif

/* -------------------------------------------------------------
 *					32 Bits Types
 */
#if MCP_LONG_SIZE == 4

typedef unsigned long 	McpU32;
typedef          long 	McpS32;

#elif MCP_INT_SIZE == 4

typedef unsigned int 	McpU32;
typedef          int 	McpS32;

#else

#error Unable to define 32-bits basic types!

#endif

/* -------------------------------------------------------------
 *			Native Float and Double Types (# of bits irrelevant)
 */

typedef float			McpFLT;
typedef double			McpDBL;
/* -------------------------------------------------------------
 *			Native Integer Types (# of bits irrelevant)
 */
typedef int			McpInt;
typedef unsigned int	McpUint;


/* -------------------------------------------------------------
 *					UTF8,16 types
 */
typedef McpU8 	McpUtf8;
typedef McpU16 	McpUtf16;
	
/* --------------------------------------------------------------
 *					Boolean Definitions							 
 */
typedef McpInt McpBool;

#define MCP_TRUE  (1 == 1)
#define MCP_FALSE (0==1) 

/* --------------------------------------------------------------
 *					Null Definition							 
 */
#ifndef NULL
#define NULL    0
#endif

/* -------------------------------------------------------------
 *					FILE						
 */
typedef void*	McpFILE;

/* -------------------------------------------------------------
 *					LIMITS						
 */
 
#define	MCP_U8_MAX			((McpU8)0xFF)
#define	MCP_U16_MAX			((McpU16)0xFFFF)
#define	MCP_U32_MAX			((McpU32)0xFFFFFFFF)

#if MCP_INT_SIZE == 4

#define MCP_UINT_MAX			(MCP_U32_MAX)

#elif MCP_INT_SIZE == 2

#define MCP_UINT_MAX			(MCP_U16_MAX)

#endif


#endif /* __MCP_HAL_TYPES_H */

