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
*   FILE NAME:      mcp_hal_log_udp.h
*
*   BRIEF:          This file defines internal structures and data types required
*                   for UDP logging.
*
*   DESCRIPTION:  
*
*   AUTHOR:         Chen Ganir
*
\*******************************************************************************/
#ifndef __MCP_HAL_LOG_UDP_H
#define __MCP_HAL_LOG_UDP_H


#define MCPHAL_LOG_MAX_FILENAME_LENGTH_UDP   150 
#define MCPHAL_LOG_MAX_MESSAGE_LENGTH_UDP    150
#define MCPHAL_LOG_MAX_THREADNAME_LENGTH  30
typedef struct _udp_log_msg_t
{
    McpU32 line;
    char threadName[MCPHAL_LOG_MAX_THREADNAME_LENGTH];
    McpHalLogSeverity severity;
    char fileName[MCPHAL_LOG_MAX_FILENAME_LENGTH_UDP];
    char message[MCPHAL_LOG_MAX_MESSAGE_LENGTH_UDP];
} udp_log_msg_t;



#define MCPHAL_LOG_MAX_FILENAME_LENGTH_NVM   4 
#define MCPHAL_LOG_MAX_MESSAGE_LENGTH_NVM    150
#define MCPHAL_LOG_MAX_MODULENAME_LENGTH_NVM  4
typedef struct _log_msg_NVM_t
{
    McpU32 line;
    char moduleName[MCPHAL_LOG_MAX_MODULENAME_LENGTH_NVM];
    McpHalLogSeverity severity;
    char fileName[MCPHAL_LOG_MAX_FILENAME_LENGTH_NVM];
    char message[MCPHAL_LOG_MAX_MESSAGE_LENGTH_NVM];
} log_msg_NVM_t;


#endif /* __MCP_HAL_LOG_UDP_H */



