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

#ifndef __PLA_CMDPARSE_H
#define __PLA_CMDPARSE_H

#include "mcpf_defs.h"
#include "mcp_hal_types.h"

#define	MAX_ADS_PORTS		10

typedef struct
{
	McpInt	x_SensorPcPort; 
	McpInt	x_NmeaPcPort;
	McpInt	x_AdsPort[MAX_ADS_PORTS];
	McpU16	x_AdsPortsCounter;
	McpU16	w_AlmanacDataBlockFlag;
	McpS8  s_PathCtrlFile[50];
} TcmdLineParams;

/** 
 * \fn     pla_cmdParse_GeneralCmdLine
 * \brief  Parses the initial command line
 * 
 * This function parses the initial command line, 
 * and returns the BT & NAVC strings, 
 * along with the common parameter.
 * 
 * \note
 * \param	CmdLine     - Received command Line.
 * \param	**bt_str - returned BT cmd line.
 * \param	**nav_str - returned NAVC cmd line.
 * \return 	Port Number
 * \sa     	pla_cmdParse_GeneralCmdLine
 */
McpU32 pla_cmdParse_GeneralCmdLine(char *cmdLine, char **bt_str, char **nav_str);

/** 
 * \fn     pla_cmdParse_NavcCmdLine
 * \brief  Parses the NAVC command line
 * 
 * This function parses the NAVC command line.
 * 
 * \note
 * \param	*nav_str - NAVC cmd line.
 * \param	*tCmdLineParams - Params to fill.
 * \return 	None
 * \sa     	pla_cmdParse_NavcCmdLine
 */
void pla_cmdParse_NavcCmdLine(char *nav_str, TcmdLineParams *tCmdLineParams);


#endif

