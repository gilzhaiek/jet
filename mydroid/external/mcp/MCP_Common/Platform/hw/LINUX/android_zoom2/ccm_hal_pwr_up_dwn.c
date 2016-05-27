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
##                                                                            */

/*******************************************************************************\
*
*   FILE NAME:      ccm_hal_pwr_up_dwn.c
*
*   DESCRIPTION:    Implementation of reset and shutdown sequences of CCM chips
*
*   AUTHOR:         Chen Ganir
*
\*******************************************************************************/
#include "ccm_hal_pwr_up_dwn.h"

/********************************************************************************
 *
 * Include files
 *
 *******************************************************************************/
#include <errno.h>
#include <string.h>
#include <stdio.h>
#include "mcp_defs.h"
#include "ccm_hal_pwr_up_dwn.h"
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/ppdev.h>

/* This is the nshutdown rfkill file on zoom2.
     On other platforms this may change. To be sure, the file /sys/class/rfkill/rfkillx/name should
     be checked on every rfkillx that present in the system (on zoom2 this equals
     "Bluetooth on OMAP3430 Zoom2"
*/

#define RFKILL_TYPE_BTENABLE        "bluetooth"
#define RFKILL_TYPE_BTENABLE_LEN    9
#define RFKILL_TYPE_FMENABLE        "fm"
#define RFKILL_TYPE_FMENABLE_LEN    2

static char *BTENABLE_FILE_NAME = NULL;
static char *FMENABLE_FILE_NAME = NULL;

static int CCM_HAL_PWR_UP_DWN_GetRfkillPath(char **rfkill_path,char* rfkill_type,int rfkill_type_len)
{
	char path[64];
	char buf[16];
	int fd;
	int sz;
	int id;
    int found = 0;
	for (id = 0;; id++) {
		snprintf(path, sizeof(path), "/sys/class/rfkill/rfkill%d/type",id);
		fd = open(path, O_RDONLY);
		if (fd < 0) {
			MCP_HAL_LOG_ERROR(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_PM,("open(%s) failed: %s (%d)", path,strerror(errno), errno));
			return CCM_HAL_PWR_UP_DWN_STATUS_FAILED;
		}
		sz = read(fd, &buf, sizeof(buf));
		close(fd);
		if (sz >= rfkill_type_len && memcmp(buf, rfkill_type, rfkill_type_len) == 0) {
            found = 1;
			break;
		}
	}
    if (found == 0)
    {
		MCP_HAL_LOG_ERROR(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_PM,("Failed to find rfkill of type %s", rfkill_type));
		return CCM_HAL_PWR_UP_DWN_STATUS_FAILED;
    }
	asprintf(rfkill_path, "/sys/class/rfkill/rfkill%d/state", id);
	return CCM_HAL_PWR_UP_DWN_STATUS_SUCCESS;
}

/********************************************************************************
 *
 * Function definitions
 *
 *******************************************************************************/
McpBool _ccmHalPwrUpDwn_NshutdownControlAvailable = MCP_FALSE;

static char disable_pin = '0';
static char enable_pin = '1';

static McpUint _ccmHalPwrUpDwn_PinRefCount = 0;

CcmHalPwrUpDwnStatus CCM_HAL_PWR_UP_DWN_WritePin(const char* pin_name, char *data, unsigned long data_len)
{
    int pin_fd = -1;

	pin_fd = open(pin_name, O_WRONLY);
	if (pin_fd == -1) {
		MCP_HAL_LOG_ERROR(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_PM, ("CCM_HAL_PWR_UP_DWN_Reset: open %s failed: %s\n", pin_name, strerror(errno)));
		return CCM_HAL_PWR_UP_DWN_STATUS_FAILED;
	}

	if (1 != write(pin_fd, data, data_len)) {
		MCP_HAL_LOG_ERROR(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_PM, ("CCM_HAL_PWR_UP_DWN_Init: write failed: %s\n", strerror(errno)));
		return CCM_HAL_PWR_UP_DWN_STATUS_FAILED;
	}

	if (-1 == close(pin_fd))
		MCP_HAL_LOG_ERROR(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_PM, ("CCM_HAL_PWR_UP_DWN_Shutdown: close failed: %s\n", strerror(errno)));

    return CCM_HAL_PWR_UP_DWN_STATUS_SUCCESS;
}

CcmHalPwrUpDwnStatus CCM_HAL_PWR_UP_DWN_SetPin(const char* pin_name)
{
    return CCM_HAL_PWR_UP_DWN_WritePin(pin_name,&enable_pin,1);
}

CcmHalPwrUpDwnStatus CCM_HAL_PWR_UP_DWN_ResetPin(const char* pin_name)
{
    return CCM_HAL_PWR_UP_DWN_WritePin(pin_name,&disable_pin,1);
}


CcmHalPwrUpDwnStatus CCM_HAL_PWR_UP_DWN_Init()
{
    /* Find rfkill path assignments for FM and bluetooth" */
    if (CCM_HAL_PWR_UP_DWN_GetRfkillPath(&BTENABLE_FILE_NAME,RFKILL_TYPE_BTENABLE,RFKILL_TYPE_BTENABLE_LEN)!=CCM_HAL_PWR_UP_DWN_STATUS_SUCCESS)
		return CCM_HAL_PWR_UP_DWN_STATUS_FAILED;

    if (CCM_HAL_PWR_UP_DWN_GetRfkillPath(&FMENABLE_FILE_NAME,RFKILL_TYPE_FMENABLE,RFKILL_TYPE_FMENABLE_LEN)!=CCM_HAL_PWR_UP_DWN_STATUS_SUCCESS)
		return CCM_HAL_PWR_UP_DWN_STATUS_FAILED;
        
    /* Write initial values to pins */
	if (CCM_HAL_PWR_UP_DWN_STATUS_SUCCESS != CCM_HAL_PWR_UP_DWN_ResetPin(FMENABLE_FILE_NAME))
		return CCM_HAL_PWR_UP_DWN_STATUS_FAILED;
    
	if (CCM_HAL_PWR_UP_DWN_STATUS_SUCCESS != CCM_HAL_PWR_UP_DWN_ResetPin(BTENABLE_FILE_NAME))
		return CCM_HAL_PWR_UP_DWN_STATUS_FAILED;

	return CCM_HAL_PWR_UP_DWN_STATUS_SUCCESS;
}

/*-------------------------------------------------------------------------------
 * CCM_HAL_PWR_UP_DWN_Reset()
 *
 *		Resets BT Host Controller chip.
 */
CcmHalPwrUpDwnStatus CCM_HAL_PWR_UP_DWN_Reset(McpHalChipId chipId, McpHalCoreId coreId, McpBool bForceReset)
{
    MCP_UNUSED_PARAMETER(chipId);
    MCP_UNUSED_PARAMETER(coreId);
    
    if ((_ccmHalPwrUpDwn_PinRefCount == 0) || bForceReset)
    {
    	if (CCM_HAL_PWR_UP_DWN_STATUS_SUCCESS != CCM_HAL_PWR_UP_DWN_ResetPin(BTENABLE_FILE_NAME))
	    return CCM_HAL_PWR_UP_DWN_STATUS_FAILED;

       	usleep(5000);

    	if (CCM_HAL_PWR_UP_DWN_STATUS_SUCCESS != CCM_HAL_PWR_UP_DWN_SetPin(BTENABLE_FILE_NAME))
	    	return CCM_HAL_PWR_UP_DWN_STATUS_FAILED;
    		
       	usleep(10000);
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

    if (_ccmHalPwrUpDwn_PinRefCount > 0)
    	--_ccmHalPwrUpDwn_PinRefCount;

	/* Actually Shut Down only when the last core requests shutdown */
	if (_ccmHalPwrUpDwn_PinRefCount == 0)
	{
    	if (CCM_HAL_PWR_UP_DWN_STATUS_SUCCESS != CCM_HAL_PWR_UP_DWN_ResetPin(BTENABLE_FILE_NAME))
	    	return CCM_HAL_PWR_UP_DWN_STATUS_FAILED;
	}
    
	return CCM_HAL_PWR_UP_DWN_STATUS_SUCCESS;
}


