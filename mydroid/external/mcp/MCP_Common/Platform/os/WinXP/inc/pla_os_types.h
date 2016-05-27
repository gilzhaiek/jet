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

/** 
 *  \file   pla_os_types.h 
 *  \brief  PLA Win OS dependent types and includes file
 * 
 * This file contains Win OS specific types, definitions and H files
 * 
 *  \see    pla_os.c, mcp_hal_types.h
 */

#ifndef PLA_OS_TYPES_H
#define PLA_OS_TYPES_H

/* -------------------------------------------------------------
 *					Platform-Depndent Part							
 *																
 * 		SET THE VALUES OF THE FOLLOWING PRE-PROCESSOR 			
 *		DEFINITIONS 	TO THE VALUES THAT APPLY TO THE 				
 *		TARGET PLATFORM											
 *																
 */

/* Size of type (char) in the target platform, in bytes	*/
#define MCP_CHAR_SIZE		1

/* Size of type (short) in the target platform, in bytes */
#define MCP_SHORT_SIZE 		2

/* Size of type (long) in the target platform, in bytes	*/
#define MCP_LONG_SIZE 		4

/* Size of type (int) in the target platform, in bytes	*/
#define MCP_INT_SIZE 		4

#define likely
#define unlikely

#ifndef __FUNCTION__
#define __FUNCTION__		""		/* macro is not implemented in MVC */
#endif

#define SNPRINTF	_snprintf

/* Task's Priority */
#define	PLA_THREAD_PRIORITY_ABOVE_NORMAL		(1)
#define	PLA_THREAD_PRIORITY_BELOW_NORMAL		(-1)
#define	PLA_THREAD_PRIORITY_HIGHEST				(2)
#define	PLA_THREAD_PRIORITY_IDLE					(-15)
#define	PLA_THREAD_PRIORITY_LOWEST				(-2)
#define	PLA_THREAD_PRIORITY_NORMAL				(0)
#define	PLA_THREAD_PRIORITY_TIME_CRITICAL		(15)


#endif /* PLA_OS_TYPES_H */


