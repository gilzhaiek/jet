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
*   FILE NAME:      mcp_ver_defs.h
*
*   DESCRIPTION:    This file defines common macros that should be used for message logging, 
*					and exception checking, handling and reporting
*
*					In addition, it contains miscellaneous other related definitions.
*
*   AUTHOR:         Udi Ron
*
\*******************************************************************************/


#ifndef __MCP_VER_DEFS_H
#define __MCP_VER_DEFS_H


/********************************************************************************
 *
 * Include files
 *
 *******************************************************************************/
#include "mcp_hal_types.h"
#include "mcp_hal_log.h"
#include "mcp_hal_misc.h"

#if (MCP_HAL_LOG_REPORT_API_FUNCTION_ENTRY_EXIT == MCP_HAL_CONFIG_ENABLED)

#define MCP_LOG_FUNCTION_ENTRY				MCP_LOG_FUNCTION(("Entered %s", mcpDbgFuncName))								
#define MCP_LOG_FUNCTION_EXIT					MCP_LOG_FUNCTION(("Exiting %s", mcpDbgFuncName))											
#define MCP_DEFINE_FUNC_NAME(funcName)	const char* mcpDbgFuncName = funcName

#else

#define MCP_LOG_FUNCTION_ENTRY
#define MCP_DEFINE_FUNC_NAME(funcName)
#define MCP_LOG_FUNCTION_EXIT

#endif

#define MCP_FUNC_START(funcName)			\
	MCP_DEFINE_FUNC_NAME(funcName);	\
	MCP_LOG_FUNCTION_ENTRY

#define MCP_FUNC_END()						\
	goto CLEANUP;							\
	CLEANUP:								\
	MCP_LOG_FUNCTION_EXIT

#define MCP_ASSERT(condition)	\
		MCP_HAL_MISC_Assert(#condition, __FILE__, (McpU16)__LINE__)

#define MCP_VERIFY_ERR_NORET(condition, msg)	\
		if ((condition) == 0)						\
		{										\
			MCP_LOG_ERROR(msg);				\
		}
		
#define MCP_VERIFY_FATAL_NORET(condition, msg)		\
		if ((condition) == 0)							\
		{											\
			MCP_LOG_FATAL(msg);					\
			MCP_ASSERT(condition);					\
		}

#define MCP_VERIFY_ERR_NO_RETVAR(condition, msg)	\
		if ((condition) == 0)						\
		{										\
			MCP_LOG_ERROR(msg);				\
			goto CLEANUP;						\
		}
		
#define MCP_VERIFY_FATAL_NO_RETVAR(condition, msg)		\
		if ((condition) == 0)							\
		{											\
			MCP_LOG_FATAL(msg);					\
			MCP_ASSERT(condition);					\
			goto CLEANUP;							\
		}

#define MCP_ERR_NORET(msg)							\
			MCP_LOG_ERROR(msg);
		
#define MCP_FATAL_NORET(msg)						\
			MCP_LOG_FATAL(msg);					\
			MCP_ASSERT(0);

#define MCP_RET_SET_RETVAR(setRetVarExp)			\
	(setRetVarExp);									\
	goto CLEANUP
	
#define MCP_VERIFY_ERR_SET_RETVAR(condition, setRetVarExp, msg)		\
		if ((condition) == 0)											\
		{															\
			MCP_LOG_ERROR(msg);									\
			(setRetVarExp);											\
			goto CLEANUP;											\
		}			

#define MCP_VERIFY_FATAL_SET_RETVAR(condition, setRetVarExp, msg)	\
		if ((condition) == 0)						\
		{										\
			MCP_LOG_FATAL(msg);				\
			(setRetVarExp);						\
			MCP_ASSERT(condition);				\
			goto CLEANUP;						\
		}

#define MCP_ERR_SET_RETVAR(setRetVarExp, msg)		\
		MCP_LOG_ERROR(msg);						\
		(setRetVarExp);								\
		goto CLEANUP;

#define MCP_FATAL_SET_RETVAR(setRetVarExp, msg)	\
			MCP_LOG_FATAL(msg);					\
			(setRetVarExp);							\
			MCP_ASSERT(0);							\
			goto CLEANUP;


#define MCP_RET(returnCode)								\
		MCP_RET_SET_RETVAR(status = returnCode)

#define MCP_RET_NO_RETVAR()							\
		MCP_RET_SET_RETVAR(status = status)

#define MCP_VERIFY_ERR(condition, returnCode, msg)		\
		MCP_VERIFY_ERR_SET_RETVAR(condition, (status = (returnCode)), msg)

 #define MCP_VERIFY_FATAL(condition, returnCode, msg)	\
		MCP_VERIFY_FATAL_SET_RETVAR(condition, (status = (returnCode)),  msg)

#define MCP_ERR(returnCode, msg)		\
		MCP_ERR_SET_RETVAR((status = (returnCode)), msg)

 #define MCP_FATAL(returnCode, msg)	\
		MCP_FATAL_SET_RETVAR((status = (returnCode)),  msg)

#define MCP_ERR_NO_RETVAR(msg)					\
			MCP_LOG_ERROR(msg);				\
			goto CLEANUP;

#define MCP_FATAL_NO_RETVAR(msg)				\
			MCP_LOG_FATAL(msg);				\
			MCP_ASSERT(0);						\
			goto CLEANUP;

#endif /* __MCP_VER_DEFS_H */


