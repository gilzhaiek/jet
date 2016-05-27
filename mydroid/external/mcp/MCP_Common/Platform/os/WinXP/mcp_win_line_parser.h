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
*   FILE NAME:      lineparser.h
*
*   BRIEF:          This file defines the API of the line parser.
*
*   AUTHOR:         Udi Ron
*
\*******************************************************************************/
#ifndef __MCP_WIN_LINE_PARSER_H_
#define __MCP_WIN_LINE_PARSER_H_

#include "mcp_hal_types.h"

#define MCP_WIN_LINE_PARSER_MAX_NUM_OF_ARGUMENTS		10
#define MCP_WIN_LINE_PARSER_MAX_LINE_LEN					200
#define MCP_WIN_LINE_PARSER_MAX_MODULE_NAME_LEN		10
#define MCP_WIN_LINE_PARSER_MAX_STR_LEN			    		80

typedef enum
{
	MCP_WIN_LINE_PARSER_STATUS_SUCCESS,
	MCP_WIN_LINE_PARSER_STATUS_FAILED,
	MCP_WIN_LINE_PARSER_STATUS_ARGUMENT_TOO_LONG,
	MCP_WIN_LINE_PARSER_STATUS_NO_MORE_ARGUMENTS
} MCP_WIN_LINE_PARSER_STATUS;

MCP_WIN_LINE_PARSER_STATUS MCP_WIN_LINE_PARSER_ParseLine(McpU8 *line, const char* delimiters);
McpU32 MCP_WIN_LINE_PARSER_GetNumOfArgs(void);
McpBool MCP_WIN_LINE_PARSER_AreThereMoreArgs(void);

void MCP_WIN_LINE_PARSER_ToLower(McpU8 *str);

MCP_WIN_LINE_PARSER_STATUS MCP_WIN_LINE_PARSER_GetNextChar(McpU8 *c);
MCP_WIN_LINE_PARSER_STATUS MCP_WIN_LINE_PARSER_GetNextStr(McpU8 *str, McpU8 len);
MCP_WIN_LINE_PARSER_STATUS MCP_WIN_LINE_PARSER_GetNextU8(McpU8 *value, McpBool hex);
MCP_WIN_LINE_PARSER_STATUS MCP_WIN_LINE_PARSER_GetNextU16(McpU16 *value, McpBool hex);
MCP_WIN_LINE_PARSER_STATUS MCP_WIN_LINE_PARSER_GetNextU32(McpU32 *value, McpBool hex);
MCP_WIN_LINE_PARSER_STATUS MCP_WIN_LINE_PARSER_GetNextS8(McpS8 *value);
MCP_WIN_LINE_PARSER_STATUS MCP_WIN_LINE_PARSER_GetNextS16(McpS16 *value);
MCP_WIN_LINE_PARSER_STATUS MCP_WIN_LINE_PARSER_GetNextS32(McpS32 *value);
MCP_WIN_LINE_PARSER_STATUS MCP_WIN_LINE_PARSER_GetNextBool(McpBool *value);

#endif	/* __MCP_WIN_LINE_PARSER_H_ */

