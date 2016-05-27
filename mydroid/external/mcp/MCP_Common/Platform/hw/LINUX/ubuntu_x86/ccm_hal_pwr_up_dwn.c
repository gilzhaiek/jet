/*
 * TI's FM Stack
 *
 * Copyright 2001-2008 Texas Instruments, Inc. - http://www.ti.com/
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and  
 * limitations under the License.
 */

/*******************************************************************************\
*
*   FILE NAME:      ccm_hal_pwr_up_dwn.c
*
*   DESCRIPTION:    Implementation of reset and shutdown sequences of CCM chips
*
*   AUTHOR:         Udi Ron
*
\*******************************************************************************/

/********************************************************************************
 *
 * Include files
 *
 *******************************************************************************/
#include <stdio.h>
#include "ccm_hal_pwr_up_dwn.h"
#include "mcp_defs.h"
#include "osapi.h"
#include "ccm_hal_pwr_up_dwn.h"
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <linux/ppdev.h>
#include <sys/io.h>

#include <linux/parport.h>
#include "mcp_hal_os.h"

#define CCM_HAL_PWR_UP_DWN_DISABLE_NSHUTDOWN 

#define PAR_PORT_NAME     ("/dev/parport0")

#define _CCM_HAL_PWR_UP_DWN_LPT1_ADDRESS      0x378

/********************************************************************************
 *
 * Function definitions
 *
 *******************************************************************************/
McpBool _ccmHalPwrUpDwn_NshutdownControlAvailable = MCP_FALSE;

static McpUint _ccmHalPwrUpDwn_PinRefCount = 0;

static void _CcmHalPwrUpDwn_SetPin();
static void _CcmHalPwrUpDwn_ClearPin();



CcmHalPwrUpDwnStatus CCM_HAL_PWR_UP_DWN_Init()
{
#ifndef CCM_HAL_PWR_UP_DWN_DISABLE_NSHUTDOWN
    static int g_parportfd;

    g_parportfd = open(PAR_PORT_NAME,O_RDWR);

    if (g_parportfd == 0)
        return CCM_HAL_PWR_UP_DWN_STATUS_FAILED;

    if (ioctl(g_parportfd,PPCLAIM))
        return CCM_HAL_PWR_UP_DWN_STATUS_FAILED;

    if (ioperm(_CCM_HAL_PWR_UP_DWN_LPT1_ADDRESS,5,1))
        return CCM_HAL_PWR_UP_DWN_STATUS_FAILED;

    _ccmHalPwrUpDwn_NshutdownControlAvailable = MCP_TRUE;
#endif    
	return CCM_HAL_PWR_UP_DWN_STATUS_SUCCESS;
}

/*-------------------------------------------------------------------------------
 * CCM_HAL_PWR_UP_DWN_Reset()
 *
 *		Resets BT Host Controller chip.
 */
CcmHalPwrUpDwnStatus CCM_HAL_PWR_UP_DWN_Reset(McpHalChipId	chipId, McpHalCoreId coreId, McpBool bForceReset)
{
	MCP_UNUSED_PARAMETER(chipId);
	MCP_UNUSED_PARAMETER(coreId);
	
	if (_ccmHalPwrUpDwn_NshutdownControlAvailable == MCP_TRUE)
	{
		/* Actually reset only when the first core requests reset */
		if (_ccmHalPwrUpDwn_PinRefCount == 0)
		{
			_CcmHalPwrUpDwn_ClearPin();

			/* Delaying to let reset value stay long enough on the line */
			MCP_HAL_OS_Sleep(20);

			_CcmHalPwrUpDwn_SetPin();

            MCP_HAL_OS_Sleep(20);
		}
	}

	++_ccmHalPwrUpDwn_PinRefCount;
	return CCM_HAL_PWR_UP_DWN_STATUS_SUCCESS;
}

/*-------------------------------------------------------------------------------
 * CCM_HAL_PWR_UP_DWN_Shutdown()
 *
 *		Shutdowns BT Host Controller chip.
 */
CcmHalPwrUpDwnStatus CCM_HAL_PWR_UP_DWN_Shutdown(McpHalChipId	chipId, McpHalCoreId coreId)
{	
	MCP_UNUSED_PARAMETER(chipId);
	MCP_UNUSED_PARAMETER(coreId);

	--_ccmHalPwrUpDwn_PinRefCount;

	if (_ccmHalPwrUpDwn_NshutdownControlAvailable == MCP_TRUE)
	{
		/* Actually Shut Down only when the last core requests shutdown */
		if (_ccmHalPwrUpDwn_PinRefCount == 0)
		{
			_CcmHalPwrUpDwn_ClearPin();
		}
	}
	return CCM_HAL_PWR_UP_DWN_STATUS_SUCCESS;
}

short  _CcmHalPwrUpDwn_Inp32 (short portaddr)
{
    short in_data = inw(portaddr);
    MCP_HAL_LOG_INFO(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_PM, ("_CcmHalPwrUpDwn_Out32:port:0x%08x data:0x%08x", portaddr,in_data));
    return in_data;
}

void  _CcmHalPwrUpDwn_Out32 (short portaddr, short datum)
{
    outw(datum,portaddr);
    MCP_HAL_LOG_INFO(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_PM, ("_CcmHalPwrUpDwn_Out32:port:0x%08x data:0x%08x", portaddr,datum));
}

void _CcmHalPwrUpDwn_SetPin()
{
	_CcmHalPwrUpDwn_Out32(_CCM_HAL_PWR_UP_DWN_LPT1_ADDRESS, (short)0xFFFF);
}

void _CcmHalPwrUpDwn_ClearPin()
{
	_CcmHalPwrUpDwn_Out32(_CCM_HAL_PWR_UP_DWN_LPT1_ADDRESS, 0);
}
