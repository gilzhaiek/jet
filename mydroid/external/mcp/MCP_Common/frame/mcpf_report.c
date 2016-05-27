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

/** \file report.c
 *  \brief report module implementation
 *
 *  \see report.h
 */

#include "mcpf_report.h"
#include "mcp_hal_os.h"
#include "mcpf_mem.h"
#include "mcp_defs.h"

char cReportModuleDesc[MCP_MAX_LOG_MODULES][MAX_STRING_LEN];
/************************************************************************
 *                        report_create                                 *
 ************************************************************************/
handle_t report_Create (handle_t hMcpf)
{
    TReport *pReport;
	McpU8	i;

    	pReport = mcpf_mem_alloc(hMcpf, sizeof(TReport));
    if (!pReport)
    {
        return NULL;
    }

    	pReport->hMcpf = hMcpf;

	/* To disable all reports */
	/*
	mcpf_mem_zero(hMcpf, pReport->aSeverityTable, sizeof(pReport->aSeverityTable));
	mcpf_mem_zero(hMcpf, pReport->aModuleTable, sizeof(pReport->aModuleTable));
	*/

	/* By Default, all reports are open */
	for(i = 0; i < REPORT_SEVERITY_MAX; i++)
    {
		pReport->aSeverityTable[i] = 1;
    }

	pReport->aSeverityTable[REPORT_SEVERITY_DEBUG_RX] = 0;
	pReport->aSeverityTable[REPORT_SEVERITY_DEBUG_TX] = 0;

	for(i = 0; i < MCP_MAX_LOG_MODULES; i++)
	{
        pReport->aModuleTable[i] = 1;
    }

    /* TBD - save report configuration in persistent storage (file).
     * Meanwhile, close "noisy" traces by default in case TRAN_DBG is not defined */
#ifndef TRAN_DBG
    pReport->aModuleTable[HAL_UART_MODULE_LOG] = 0;
    pReport->aModuleTable[HAL_ST_MODULE_LOG] = 0;
    pReport->aModuleTable[BUS_DRV_MODULE_LOG] = 0;
    pReport->aModuleTable[QUEUE_MODULE_LOG] = 0;
#endif    
        
    return (handle_t)pReport;
}

/************************************************************************
 *                        report_SetDefaults                            *
 ************************************************************************/
EMcpfRes report_SetDefaults (handle_t hReport, TReportInitParams *pInitParams)
{
    report_SetReportModuleTable (hReport, (McpU8 *)pInitParams->aModuleTable);
    report_SetReportSeverityTable (hReport, (McpU8 *)pInitParams->aSeverityTable);
    
    return RES_COMPLETE;
}

/************************************************************************
 *                        report_unLoad                                 *
 ************************************************************************/
EMcpfRes report_Unload (handle_t hReport)
{
    TReport *pReport = (TReport *)hReport;
    /* Added for Android Logger Support */
	MCP_HAL_LOG_Deinit();
    mcpf_mem_free(pReport->hMcpf, pReport);
    return RES_COMPLETE;
}

EMcpfRes report_SetReportModule(handle_t hReport, McpU8 module_index)
{
    EMcpfRes res = RES_COMPLETE;

    if (module_index < MCP_MAX_LOG_MODULES)
    {
        ((TReport *)hReport)->aModuleTable[module_index] = 1;
    }
    else
    {
        MCPF_OS_REPORT(("Invalid module index %d", module_index));
        res = RES_ERROR;
    }
    return res;
}


EMcpfRes report_ClearReportModule(handle_t hReport, McpU8 module_index)
{
    EMcpfRes res = RES_COMPLETE;
    
    if (module_index < MCP_MAX_LOG_MODULES)
{
    ((TReport *)hReport)->aModuleTable[module_index] = 0;
    }
    else
    {
        MCPF_OS_REPORT(("Invalid module index %d", module_index));
        res = RES_ERROR;
    }
    return res;
}

EMcpfRes report_SetReportSeverity(handle_t hReport, McpU8 uSeverity)
{
    EMcpfRes res = RES_COMPLETE;

    if (uSeverity < REPORT_SEVERITY_MAX)
    { 
          ((TReport *)hReport)->aSeverityTable[uSeverity] = 1;
    }
    else
    {
        MCPF_OS_REPORT(("Invalid severity %d", uSeverity));
        res = RES_ERROR;
    }
    return res;
}


EMcpfRes report_ClearReportSeverity(handle_t hReport, McpU8 uSeverity)
{
     EMcpfRes res = RES_COMPLETE;
    
    if (uSeverity < REPORT_SEVERITY_MAX)
    { 
          ((TReport *)hReport)->aSeverityTable[uSeverity] = 0;
    }
    else
    {
        MCPF_OS_REPORT(("Invalid severity %d", uSeverity));
        res = RES_ERROR;
    }
    return res;
}
EMcpfRes report_GetReportModuleTable(handle_t hReport, McpU8 *pModules)
{
    McpU8 index;

    mcpf_mem_copy(((TReport *)hReport)->hMcpf, 
                  (void *)pModules, 
                  (void *)(((TReport *)hReport)->aModuleTable), 
                  sizeof(((TReport *)hReport)->aModuleTable));

    for (index = 0; index < sizeof(((TReport *)hReport)->aModuleTable); index++)
    {
        pModules[index] += '0';
    }

    return RES_COMPLETE;
}


EMcpfRes report_SetReportModuleTable(handle_t hReport, McpU8 *pModules)
{
    McpU8 index;

    for (index = 0; index < sizeof(((TReport *)hReport)->aModuleTable); index++)
    {
        pModules[index] -= '0';
    }

    mcpf_mem_copy(((TReport *)hReport)->hMcpf, 
                  (void *)(((TReport *)hReport)->aModuleTable), 
                  (void *)pModules, 
                  sizeof(((TReport *)hReport)->aModuleTable));

    return RES_COMPLETE;
}


EMcpfRes report_GetReportSeverityTable(handle_t hReport, McpU8 *pSeverities)
{
    McpU8 index;

    mcpf_mem_copy (((TReport *)hReport)->hMcpf, 
                   (void *)pSeverities, 
                   (void *)(((TReport *)hReport)->aSeverityTable), 
                   sizeof(((TReport *)hReport)->aSeverityTable));

    for (index = 0; index < sizeof(((TReport *)hReport)->aSeverityTable); index++)
    {
        pSeverities[index] += '0';
    }

    return RES_COMPLETE;
}


EMcpfRes report_SetReportSeverityTable(handle_t hReport, McpU8 *pSeverities)
{
    McpU8 index;

    for (index = 0; index < sizeof(((TReport *)hReport)->aSeverityTable); index++)
    {
        pSeverities[index] -= '0';
    }

    mcpf_mem_copy(((TReport *)hReport)->hMcpf, 
                  (void *)(((TReport *)hReport)->aSeverityTable), 
                  (void *)pSeverities, 
                  sizeof(((TReport *)hReport)->aSeverityTable));

    return RES_COMPLETE;
}

/************************************************************************
 *                        hex_to_string                                 *
 ************************************************************************/
EMcpfRes report_Dump (McpU8 *pBuffer, char *pString, McpU32 size)
{
    McpU32 index;
    McpU8  temp_nibble;

    /* Go over pBuffer and convert it to chars */ 
    for (index = 0; index < size; index++)
    {
        /* First nibble */
        temp_nibble = (McpU8)(pBuffer[index] & 0x0F);
        if (temp_nibble <= 9)
        {
            pString[(index << 1) + 1] = (char)(temp_nibble + '0');
        }
        else
        {
            pString[(index << 1) + 1] = (char)(temp_nibble - 10 + 'A');
        }

        /* Second nibble */
        temp_nibble = (McpU8)((pBuffer[index] & 0xF0) >> 4);
        if (temp_nibble <= 9)
        {
            pString[(index << 1)] = (char)(temp_nibble + '0');
        }
        else
        {
            pString[(index << 1)] = (char)(temp_nibble - 10 + 'A');
        }
    }

    /* Put string terminator */
    pString[(size * 2)] = 0;

    return RES_COMPLETE;
}


/* HEX DUMP for BDs !!! Debug code only !!! */
EMcpfRes report_PrintDump (McpU8 *pData, McpU32 datalen)
{
#ifdef TRAN_DBG
    McpS32  dbuflen=0;
    McpU32 j;
    char   dbuf[50];
    static const char hexdigits[16] = "0123456789ABCDEF";

    for(j=0; j < datalen;)
    {
        /* Add a byte to the line*/
        dbuf[dbuflen] =  hexdigits[(pData[j] >> 4)&0x0f];
        dbuf[dbuflen+1] = hexdigits[pData[j] & 0x0f];
        dbuf[dbuflen+2] = ' ';
        dbuf[dbuflen+3] = '\0';
        dbuflen += 3;
        j++;
        if((j % 16) == 0)
        {
            /* Dump a line every 16 hex digits*/
            MCPF_OS_REPORT(("%04x  %s\n", j-16, dbuf));
            dbuflen = 0;
        }
    }
    /* Flush if something has left in the line*/
    if(dbuflen)
        MCPF_OS_REPORT(("%04x  %s\n", j & 0xfff0, dbuf));
#else
    MCP_UNUSED_PARAMETER(pData);
    MCP_UNUSED_PARAMETER(datalen);
#endif
    return RES_COMPLETE;
}

