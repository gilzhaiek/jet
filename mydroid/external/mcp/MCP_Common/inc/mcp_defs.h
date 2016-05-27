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

#ifndef __MCP_DEFS_H
#define __MCP_DEFS_H

#include "mcp_hal_types.h"
#include "mcp_hal_config.h"
#include "mcp_hal_log.h"
#include "mcp_ver_defs.h"

/*-------------------------------------------------------------------------------
 * MCP_STATIC Macro
 * 
 * This macro should be used instead of the 'static' keyword.
 *
 * The reason is that static symbols are usually not part of the debugger's symbol table, 
 * thus preventing source-level debugging of static functions and variables.
 *
 * In addition, they hamper footprint calculations since they do not show up in the map
 * as global symbols do.
 *
 */
#if (MCP_CONFIG_USE_STATIC_KEYWORD == MCP_CONFIG_ENABLED)

#define MCP_STATIC		static

#else

#define MCP_STATIC		/* Nothing */

#endif

#define MAX_STRING_LEN              128

#ifdef MCP_LOGD
#define LOGD ALOGD
#else
#define LOGD
#endif

#ifdef MCP_LOGV
#define LOGV ALOGV
#else
#define LOGV
#endif

#ifdef MCP_LOGI
#define LOGI ALOGI
#else
#define LOGI
#endif

#define LOGE ALOGE

/*
 *	McpStackType type
 *
 *	A logical identifier of a core stack type
*/
typedef enum tagMcpStackType {
	MCP_STACK_TYPE_BT	= 0,
	MCP_STACK_TYPE_FM_RX,
	MCP_STACK_TYPE_FM_TX,
	MCP_STACK_TYPE_GPS,

	MCP_NUM_OF_STACK_TYPES,
	MCP_INVALID_STACK_TYPE
} McpStackType;

/* MCP Tasks Definitions */
typedef enum 
{
	TASK_BT_ID,	
	TASK_FM_ID,		
	TASK_NAV_ID,	
	TASK_CCM_ID,	
	TASK_UI_ID,
	TASK_GPSAL_ID,
	TASK_CUSTOM_APP_ID,
    TASK_SUPL_ID,
    TASK_WPC_ID,
	TASK_MAX_ID,
	TASK_EXT_ID = TASK_MAX_ID
		
} EmcpTaskId;	 

/* Report Modules values */
#ifdef MCP_STU_ENABLE
#define	NAVC_MODULE_LOG (MCP_HAL_LOG_MODULE_TYPE_NAVC_MODULE)
#define	BT_MODULE_LOG (MCP_HAL_LOG_MODULE_TYPE_BT_ADAPT_MODULE)
#define	FM_MODULE_LOG (MCP_HAL_LOG_MODULE_TYPE_FM_ADAPT_MODULE)        
#define	MCPF_MODULE_LOG (MCP_HAL_LOG_MODULE_TYPE_MCPF_MODULE)
#define	TRANS_MODULE_LOG (MCP_HAL_LOG_MODULE_TYPE_TRANS_MODULE)     
#define	QUEUE_MODULE_LOG (MCP_HAL_LOG_MODULE_TYPE_QUEUE_MODULE)     
#define	REPORT_MODULE_LOG (MCP_HAL_LOG_MODULE_TYPE_REPORT_MODULE)
#define	IFSLPMNG_MODULE_LOG (MCP_HAL_LOG_MODULE_TYPE_IFSLPMNG_MODULE)
#define	BUS_DRV_MODULE_LOG (MCP_HAL_LOG_MODULE_TYPE_BUS_DRV_MODULE)    
#define	HAL_ST_MODULE_LOG (MCP_HAL_LOG_MODULE_TYPE_ST_MODULE)    
#define	UI_MODULE_LOG (MCP_HAL_LOG_MODULE_TYPE_NAVCP_UI_MODULE)
#define	ADS_MODULE_LOG (MCP_HAL_LOG_MODULE_TYPE_ADS_MODULE)
#define	RR_MODULE_LOG (MCP_HAL_LOG_MODULE_TYPE_RR_MODULE)
#define	HAL_SOCKET_MODULE_LOG (MCP_HAL_LOG_MODULE_TYPE_HAL_SOCKET_MODULE)
#define HAL_UART_MODULE_LOG (MCP_HAL_LOG_MODULE_TYPE_UART_MODULE)

#define MCP_MAX_LOG_MODULES  (MCP_HAL_LOG_MODULE_TYPE_LAST)
#else
typedef enum
{
	NAVC_MODULE_LOG,
	BT_MODULE_LOG,
	FM_MODULE_LOG,        
	MCPF_MODULE_LOG,
	TRANS_MODULE_LOG,     
	QUEUE_MODULE_LOG,     
	REPORT_MODULE_LOG,
	IFSLPMNG_MODULE_LOG,
	BUS_DRV_MODULE_LOG, 
	HAL_ST_MODULE_LOG,          /* Shared Transport */
	HAL_UART_MODULE_LOG,
	HAL_HCI_MODULE_LOG,
	UI_MODULE_LOG,
	ADS_MODULE_LOG,
	RR_MODULE_LOG,
	NAVL_MODULE_LOG,
	HOSTSOCKET_MODULE_LOG,
	HAL_SOCKET_MODULE_LOG,
	WPC_MODULE_LOG,
	MCP_MAX_LOG_MODULES
		
} EReportModule;
#endif /* ifdef MCP_STU_ENABLE */

/*---------------------------------------------------------------------------
 *
 * Used to define unused function parameters. Some compilers warn if this
 * is not done.
 */
#define MCP_UNUSED_PARAMETER(_PARM)     ((_PARM) = (_PARM))
#define MCP_UNUSED_CONST_PARAMETER(_PARM)   if (_PARM==_PARM) {};

#ifdef EBTIPS_RELEASE
#define MCP_LOG_DEBUG(msg)  
#define MCP_LOG_FUNCTION(msg)  
#else
#define MCP_LOG_DEBUG(msg)		MCP_HAL_LOG_DEBUG(__FILE__, __LINE__, mcpHalLogModuleId, msg)
#define MCP_LOG_FUNCTION(msg) 		MCP_HAL_LOG_FUNCTION(__FILE__, __LINE__,mcpHalLogModuleId, msg)
#endif /* EBTIPS_RELEASE */

#define MCP_LOG_INFO(msg)			MCP_HAL_LOG_INFO(__FILE__, __LINE__,mcpHalLogModuleId, msg)				
#define MCP_LOG_ERROR(msg)			MCP_HAL_LOG_ERROR(__FILE__, __LINE__, mcpHalLogModuleId, msg)			
#define MCP_LOG_FATAL(msg)			MCP_HAL_LOG_FATAL(__FILE__, __LINE__, mcpHalLogModuleId, msg)	
#define MCP_LOG_DUMPBUF(msg,pBuf,len)	MCP_HAL_LOG_DUMPBUF(__FILE__, __LINE__, msg, pBuf, len)	



#endif

