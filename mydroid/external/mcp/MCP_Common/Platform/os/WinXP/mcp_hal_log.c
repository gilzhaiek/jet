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
*   FILE NAME:      mcp_hal_log.c
*
*   DESCRIPTION:    This file implements the API of the MCP HAL log utilities.
*
*   AUTHOR:         Udi Ron
*
\*******************************************************************************/


/********************************************************************************
 *
 * Include files
 *
 *******************************************************************************/

#include <windows.h>
#include "logger.h"
#include "mcp_hal_log.h"

/****************************************************************************
 *
 * Constants
 *
 ****************************************************************************/

#define MCP_HAL_MAX_FORMATTED_MSG_LEN 			(200)
#define UNUSED_PARAMETER(_PARM) 	((_PARM) = (_PARM))

static char _mcpLog_FormattedMsg[MCP_HAL_MAX_FORMATTED_MSG_LEN + 1];
DWORD g_dwTlsIndex;

void MCP_HAL_LOG_Init(void)
{
    /* allocate thread local storage */
    g_dwTlsIndex = TlsAlloc();
    if (TLS_OUT_OF_INDEXES == g_dwTlsIndex)
    {
        C_Logger_Debug(3,
                       __FILE__,
                       __LINE__,
                       MCP_HAL_LOG_SEVERITY_FATAL,
                       "%s: MCP_HAL_LOG_Init: Unable to allocate TLS index, err %d",
                       MCP_HAL_LOG_Modules[0].name,
                       GetLastError());
        Sleep(5);
        exit(1);
    }
}

void MCP_HAL_LOG_Deinit(void)
{
    BOOL ret = TlsFree(g_dwTlsIndex);
    if (0 == ret)
    {
        C_Logger_Debug(3,
                       __FILE__,
                       __LINE__,
                       MCP_HAL_LOG_SEVERITY_FATAL,
                       "%s: MCP_HAL_LOG_Deinit: Unable to free TLS index, err %d",
                       MCP_HAL_LOG_Modules[0].name,
                       GetLastError());
        Sleep(5);
        exit(1);
    }
}

void MCP_HAL_LOG_SetThreadName(const char* name)
{
    if (0 == TlsSetValue(g_dwTlsIndex, (LPVOID)name))
    {
        C_Logger_Debug(3,
                       __FILE__,
                       __LINE__,
                       MCP_HAL_LOG_SEVERITY_FATAL,
                       "%s: MCP_HAL_LOG_SetThreadName: Unable to set TLS value, err %d",
                       MCP_HAL_LOG_Modules[0].name,
                       GetLastError());
        Sleep(5);
        exit(1);
    }
}

static char *MCP_HAL_LOG_GetThreadName(void)
{
    return TlsGetValue(g_dwTlsIndex);
}

/*-------------------------------------------------------------------------------
 * MCP_HAL_LOG_FormatMsg()
 *
 *		sprintf-like string formatting. the formatted string is allocated by the function.
 *
 * Type:
 *		Synchronous, non-reentrant 
 *
 * Parameters:
 *
 *		format [in]- format string
 *
 *		...     [in]- additional format arguments
 *
 * Returns:
 *     Returns pointer to the formatted string
 *
 */
char *MCP_HAL_LOG_FormatMsg(const char *format, ...)
{
	va_list     args;

	_mcpLog_FormattedMsg[MCP_HAL_MAX_FORMATTED_MSG_LEN] = '\0';
	
	va_start(args, format);

	_vsnprintf(_mcpLog_FormattedMsg, MCP_HAL_MAX_FORMATTED_MSG_LEN, format, args);

	va_end(args);

	return _mcpLog_FormattedMsg;
}


/*-------------------------------------------------------------------------------
 * LogMsg()
 *
 *		Sends a log message to the local logging tool.
 *		Not all parameters are necessarily supported in all platforms.
 *     
 * Type:
 *		Synchronous
 *
 * Parameters:
 *
 *		fileName [in] - name of file originating the message
 *
 *		line [in] - line number in the file
 *
 *		moduleType [in] - e.g. "BTL_BMG", "BTL_SPP", "BTL_OPP", 
 *
 *		severity [in] - debug, error...
 *
 *		msg [in] - message in already formatted string
 *
 * 	Returns:
 *		void
 * 
 */
void MCP_HAL_LOG_LogMsg(	const char*		fileName, 
								McpU32			line, 
                            McpHalLogModuleId_e moduleId, 
								McpHalLogSeverity severity,  
								const char* 		msg)
{
    char *threadName = MCP_HAL_LOG_GetThreadName();

    C_Logger_Debug(3,
                   fileName,
                   line,
                   severity,
                   "%s (%s): %s",
                   MCP_HAL_LOG_Modules[moduleId].name,
                   (threadName == NULL ? "UNKNOWN" : threadName),
				   msg);
}


/*-------------------------------------------------------------------------------
 * DumpBuf()
 *
 *		Sends a log message to the local logging tool.
 *		Not all parameters are necessarily supported in all platforms.
 *     
 * Type:
 *		Synchronous
 *
 * Parameters:
 *
 *		fileName [in] - name of file originating the message
 *
 *		line [in] - line number in the file
 *
 *		severity [in] - debug, error...
 *
 *		pTitle [in] - title, already formatted string, limited to 40 bytes length
 *
 *		pBuf [in] - pointer to buffer to dump contents
 *
 *		len [in]  - buffer size to dump
 * 	Returns:
 *		void
 * 
 */
void MCP_HAL_LOG_DumpBuf (const char 		*fileName, 
						 McpU32				line, 
						 McpHalLogSeverity 	severity,  
						 const char			*pTitle,
						 const McpU8		*pBuf,
						 const McpU32		len)
{

#define D_LINE_SIZE	16

	McpU32 i,j;
	char  * pOut, outBuf[(D_LINE_SIZE * 3)+50];
    char *threadName = MCP_HAL_LOG_GetThreadName();

	pOut = outBuf;
	
	for (i=0; i<len && pOut<(outBuf + sizeof(outBuf)); i += D_LINE_SIZE)
	{
		pOut += sprintf(pOut, "%s %u:", pTitle, i);
		for (j=0; j<D_LINE_SIZE && len>i+j && (pOut<(outBuf + sizeof(outBuf))); j++  )
		{
			pOut += sprintf(pOut, " %02x", *(pBuf+i+j));
		}
		C_Logger_Debug(3, fileName, line, severity, "(%s) : %s",
                   (threadName == NULL ? "UNKNOWN" : threadName), outBuf);
		pOut = outBuf;
	}
}



void MCP_HAL_LOG_Set_Mode(McpU8 udpMode)
{
	UNUSED_PARAMETER(udpMode);

    C_Logger_Debug(3, "NA", 100, 1, "%s", "SetMode - Not APPL");
}

void MCP_HAL_LOG_LogBinBuf( const McpU8	logMode,
									McpU8	tagId,
						 			const McpU8	*pBuf,
						 			const McpU32	len)
{
   McpU8 u_i;
    u_i = logMode;

	UNUSED_PARAMETER(tagId);

    
MCP_HAL_LOG_DumpBuf ("gpsc_drv_api.c",  
						11, 
				         2,  
						"To/From Sens",
						 pBuf,
						 len);

}