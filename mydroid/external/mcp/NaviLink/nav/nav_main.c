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


#include "navc_api.h"
#include "navc_defs.h"
#include "navc_init.h"
//#include "nt_adapt.h"

#include "mcpf_defs.h"
#include "mcpf_report.h"
#include "mcp_hal_os.h"
#include "mcpf_mem.h"
#include "mcpf_msg.h"
#include "mcpf_main.h"

#include "navl_api.h"
//#include "hostSocket.h"
#include "hostSocket_api.h"

#include "mcp_hal_string.h"
#include "pla_cmdParse.h"

#include "suplc_main.h"

#ifdef ENABLE_INAV_ASSIST
#include "inavc_ext.h"
#endif
#include "navc_inavc_if.h"

#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <string.h>
#include <errno.h>

#include <utils/Log.h>
#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG "NAVL_SERVER"


#define MAX_SIZE 5
#define FIFO_PATH "/data/gps/navlfifo"

#ifdef _NAVC_APP
static EMcpfRes NL_Create(handle_t hMcpf, char *CmdLine);

typedef struct  
{
	handle_t hNavl;
	handle_t hHostSocket;
	handle_t hInavc;
} NL_t;

#endif

NL_t	nl;
handle_t hMcpf = NULL;


extern char cReportModuleDesc[MCP_MAX_LOG_MODULES][MAX_STRING_LEN];
extern McpU32 uMcpfDynamicMemCnt;


handle_t MCP_Init (char *cmdLine)
{	
	handle_t 	hMcpf, hPla;
	char* bt_str = 0;
	char* nav_str = 0;
	McpU32	uPortNum = 0;

	/* Initialize logging module */
	MCP_HAL_LOG_Init();
	MCP_HAL_LOG_SetThreadName("NAVD_MAIN");
	
	/* Allocate BT & NAVC command Lines Strings */
	bt_str = mcpf_mem_alloc(NULL, 
		(McpU32)(MCP_HAL_STRING_StrLen((const char *)cmdLine) + 1));

	nav_str = mcpf_mem_alloc(NULL, 
		(McpU32)(MCP_HAL_STRING_StrLen((const char*)cmdLine) + 1));

	uPortNum = pla_cmdParse_GeneralCmdLine(cmdLine, &bt_str, &nav_str);

	/* Create PLA */
	hPla = pla_create();

    /* Create the Framework */
	hMcpf = mcpf_create(hPla, (McpU16)uPortNum);

	mcpf_mem_free(NULL, bt_str);

#ifdef _NAVC_APP
	/* Initialize NL */
   if (hMcpf != NULL)
	   NL_Create(hMcpf, nav_str);
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
    int fd = 0 ;
    int res = 0 ;

    char buf[MAX_SIZE] = {'\0'};

     unlink(FIFO_PATH);
     res =  mkfifo(FIFO_PATH, 0666);
     if (res == 0)
     {
        char buf[100];
        sprintf(buf, "chmod 666 %s", FIFO_PATH);
        system(buf);

        sprintf(buf, "chown system.sytem %s", FIFO_PATH);
        system(buf);
     }
     else
     {
         mcpf_destroy(hMcpf);
         return;
     }

	 /* Open the pipe for reading */
	 fd = open(FIFO_PATH, O_CREAT |O_RDWR| O_TRUNC );
	 if ( fd < 0)
	 {
        mcpf_destroy(hMcpf);
	     return;
	 }

	 /* Read from the pipe */
	 while (strcmp(buf, "exit") !=0 )
	 {
	    if ( read(fd, buf, MAX_SIZE) < 0)
	    {
	       if (errno == EINTR) continue;
          mcpf_destroy(hMcpf);

	       return;
	    }
	 }

    MCP_HAL_LOG_Deinit();

	 hostSocket_Destroy(nl.hHostSocket);
    NAVL_Destroy(nl.hNavl);
    SUPL_Destroy();
    NAVC_Destroy();

    mcpf_destroy(hMcpf);

	 close(fd);
	 remove(FIFO_PATH);

	 memset(buf, 0x0, MAX_SIZE);
}


#ifdef _NAVC_APP

static EMcpfRes NL_Create(handle_t hMcpf, char* CmdLine)
{
   char nav_str[8];
    
   snprintf(nav_str, 8, "-p%d", atoi((const char*)CmdLine) ); 

	/* Initiate NAVC */
	if (NAVC_Create(hMcpf, CmdLine) != RES_OK)
	{
		mcpf_mem_free(NULL, CmdLine);
		return RES_ERROR;
	}

	mcpf_mem_free(NULL, CmdLine);

	/* Initiate SUPL */
	if (SUPL_Create(hMcpf) != RES_OK)
		return RES_ERROR;
	
	/* Initiate NAVL */
	nl.hNavl = NAVL_Create(hMcpf);
	if (nl.hNavl == NULL)
		return RES_ERROR;

	/* Initiate Host Socket */
	nl.hHostSocket = hostSocket_Create(hMcpf, nl.hNavl);

	return RES_OK;
}


#endif /* _NAVC_APP */

