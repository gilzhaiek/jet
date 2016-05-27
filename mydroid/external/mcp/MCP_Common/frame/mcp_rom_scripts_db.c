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

#include "mcp_hal_string.h"
#include "mcp_rom_scripts_db.h"
#include "mcp_defs.h"
#include "mcp_hal_log.h"

MCP_HAL_LOG_SET_MODULE(MCP_HAL_LOG_MODULE_TYPE_FRAME);

extern const McpRomScripts_Data mcpRomScripts_Data[];
extern const McpUint mcpRomScripts_NumOfScripts;

McpBool MCP_RomScriptsGetMemInitScriptData(	const char 	*scriptFileName,
											McpUint		*scriptSize,
											const McpU8 **scriptAddress)
{
	McpUint	idx;
	McpBool	scriptFound;

	scriptFound = MCP_FALSE;
	*scriptSize = 0;
	*scriptAddress = NULL;
	
	for (idx = 0; idx < mcpRomScripts_NumOfScripts; ++idx)
	{
		if (MCP_HAL_STRING_StriCmp(scriptFileName, mcpRomScripts_Data[idx].fileName) == 0)
		{
			scriptFound = MCP_TRUE;
			*scriptSize = mcpRomScripts_Data[idx].size;
			*scriptAddress = mcpRomScripts_Data[idx].address;
		}
	}

	MCP_LOG_INFO(("MCP_RomScriptsGetMemInitScriptData: %s file \"%s\" in ROM!", 
		((scriptFound == MCP_TRUE) ? ("found") : ("failed to find")),
		scriptFileName));

	return scriptFound;
}


