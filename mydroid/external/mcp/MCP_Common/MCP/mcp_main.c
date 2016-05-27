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

#ifdef _BT_APP
#include "btl_common.h"
#include "btl_bmg.h"
#include "app_config.h"
#endif
#ifdef _FM_APP
#include "fm_rx.h"
#include "fm_tx.h"
#endif

#include "mcpf_defs.h"
#include "mcp_hal_defs.h"
#include "mcp_hal_types.h"
#include "mcp_hal_string.h"
#include "mcp_hal_pm.h"
#ifdef MCP_STU_ENABLE
#include "BusDrv.h"
#include "mcp_hal_uart.h"
#include "mcp_IfSlpMng.h"
#include "mcp_txnpool.h"
#include "mcp_transport.h"
#endif
#include "mcpf_main.h"
#include "mcpf_mem.h"
#include "pla_defs.h"
#include "pla_cmdParse.h"
#include "pla_os.h"
#include "mcp_hal_log.h"
#include "mcp_main.h"
#include "mcpf_report.h"

#ifdef _NAVC_APP
#include "navc_init.h"
#include "navl_api.h"
#include "hostSocket_api.h"
//#include "suplc_main.h"


static EMcpfRes NL_Create(handle_t hMcpf, char *CmdLine);
static void		NL_Destory();

typedef struct  
{
	handle_t hNavl;
	handle_t hHostSocket;
} NL_t;

NL_t	nl;
#endif

extern char cReportModuleDesc[MCP_MAX_LOG_MODULES][MAX_STRING_LEN];
extern McpU32 uMcpfDynamicMemCnt;


/*---------------------------------------------------------------------------
 *            MCP_Init()
 *---------------------------------------------------------------------------
 *
 * Synopsis:  Main entry point for our application
 *
 * Return:	MCPF Handler
 *
 */
handle_t MCP_Init (char *cmdLine)
{	
	handle_t hMcpf, hPla;
	char *bt_str;
	char *nav_str;
	McpU32 uPortNum;
#ifdef _BT_APP
	BtlCallBack btCallbackRoutine;
#endif /* _BT_APP */	

	/* Initialize logging module */
	MCP_HAL_LOG_Init();
	MCP_HAL_LOG_SetThreadName("MAIN");
	
	/* Allocate BT & NAVC command Lines Strings */
	bt_str = mcpf_mem_alloc(NULL, 
		(McpU32)(MCP_HAL_STRING_StrLen((const char *)cmdLine) + 1));
	MCPF_Assert(bt_str);
	nav_str = mcpf_mem_alloc(NULL, 
		(McpU32)(MCP_HAL_STRING_StrLen((const char *)cmdLine) + 1));
	MCPF_Assert(nav_str);

	uPortNum = pla_cmdParse_GeneralCmdLine(cmdLine, &bt_str, &nav_str);
	MCPF_Assert(uPortNum);

	/* Create PLA */
	hPla = pla_create();

    /* Create the Framework */
	hMcpf = mcpf_create(hPla, (McpU16)uPortNum);
	MCPF_Assert(hMcpf != NULL);
	
	MCPF_REPORT_INFORMATION(hMcpf, MCPF_MODULE_LOG, 
		("***** MCP_Init: mcpf_create() SUCCESS! nav_str = %s *****", nav_str));
	
#ifdef _BT_APP
	btCallbackRoutine = (BtlCallBack)APP_CONFIG_GET_BTL_CALLBACK;
	/* Initialize EBTIPS basic components: BT stack, BTL layer... */
	MCPF_Assert (BT_STATUS_SUCCESS == BTL_Init(btCallbackRoutine, hMcpf, bt_str));  
#endif /* _BT_APP */	

#ifdef _FM_APP
	/*Initialize FM Stack */
	MCPF_Assert(FMC_STATUS_SUCCESS == FM_TX_Init(hMcpf));
	MCPF_Assert(FMC_STATUS_SUCCESS == FM_RX_Init(hMcpf));
#endif /* _FM_APP */

	mcpf_mem_free(NULL, bt_str);

#ifdef _NAVC_APP
#ifdef MCP_STU_ENABLE
	/* Initiate NAVC */
	NAVC_Create(hMcpf, nav_str);
#else
	/* Initialize NL */
	NL_Create(hMcpf, nav_str);
#endif
	
	MCPF_REPORT_INFORMATION(hMcpf, MCPF_MODULE_LOG, 
		("***** MCP_Init: NL_Create() SUCCESS! *****"));
#endif /* _NAVC_APP */
	
	return hMcpf;
}

/*---------------------------------------------------------------------------
 *            MCP_Deinit()
 *---------------------------------------------------------------------------
 *
 * Synopsis:  De-initializes the MCP Package
 *
 * Return:    
 *
 */
void  MCP_Deinit(handle_t hMcpf)
{
#ifdef _NAVC_APP
#ifdef MCP_STU_ENABLE
	NAVC_Destroy();
#else
	NL_Destory();
#endif
#endif

#ifdef _FM_APP
	FM_RX_Deinit();
	FM_TX_Deinit();
#endif

#ifdef _BT_APP
	BTL_Deinit();
#endif

        /* de-initialize logging module */
	MCP_HAL_LOG_Deinit();

	mcpf_destroy(hMcpf);

}


#ifdef _NAVC_APP
#ifndef MCP_STU_ENABLE
/*---------------------------------------------------------------------------
 *            NL_Create()
 *---------------------------------------------------------------------------
 *
 * Synopsis:  Initializes all GPS related Modules
 *
 * Return:    RES_OK on Success, RES_ERROR on failure. 
 *
 */
static EMcpfRes NL_Create(handle_t hMcpf, char *CmdLine)
{
	/* Initiate NAVC */
	if (NAVC_Create(hMcpf, CmdLine) != RES_OK)
	{
		mcpf_mem_free(NULL, CmdLine);
		return RES_ERROR;
	}

	mcpf_mem_free(NULL, CmdLine);

	/* Initiate SUPL */
	/*if (SUPL_Create(hMcpf) != RES_OK)
		return RES_ERROR;*/
	
	MCPF_REPORT_INFORMATION(hMcpf, MCPF_MODULE_LOG, 
		("***** NL_Create: NAVC_Create() SUCCESS! *****"));
	
	/* Initiate NAVL */
	nl.hNavl = NAVL_Create(hMcpf);
	if(nl.hNavl == NULL)
		return RES_ERROR;
	
	MCPF_REPORT_INFORMATION(hMcpf, MCPF_MODULE_LOG, 
		("***** NL_Create: NAVL_Create() SUCCESS! *****"));
	
	MCPF_REPORT_INFORMATION(hMcpf, MCPF_MODULE_LOG, 
		("***** uMcpfDynamicMemCnt = %d (Excluding Host Socket mem alloc) *****", 
														uMcpfDynamicMemCnt));
	
	/* Initiate Host Socket */
	nl.hHostSocket = hostSocket_Create(hMcpf, nl.hNavl);

	MCPF_REPORT_INFORMATION(hMcpf, MCPF_MODULE_LOG, 
		("***** NL_Create: hostSocket_Create() SUCCESS! *****"));

	return RES_OK;
}

/*---------------------------------------------------------------------------
 *            NL_Destory()
 *---------------------------------------------------------------------------
 *
 * Synopsis:  Destroy all GPS related Modules
 *
 * Return:    
 *
 */
static void NL_Destory()
{
	hostSocket_Destroy(nl.hHostSocket);

	NAVL_Destroy(nl.hNavl);

	NAVC_Destroy();
}
#endif /* ifndef MCP_STU_ENABLE */
#endif /* _NAVC_APP */
