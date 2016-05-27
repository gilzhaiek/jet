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
/******************************************************************************\
*
*   FILE NAME:      mcp_hal_config.h
*
*   BRIEF:          MCP HAL Configuration Parameters
*
*   DESCRIPTION:    
*
*   The constants in this file configure the MCP HAL layer for a specific
*   platform and project.
*
*   Some constants are numeric, and others indicate whether a feature is enabled
*   (defined as MCP_HAL_CONFIG_ENABLED) or disabled (defined as
*     MCP_HAL_CONFIG_DISABLED).
*
*   The values in this specific file are tailored for a Windows distribution.
*   To change a constant, simply change its value in this file and recompile the
*   entire BTIPS package.
*
*   AUTHOR:         Udi Ron
*
*******************************************************************************/

#ifndef __MCP_HAL_CONFIG_H
#define __MCP_HAL_CONFIG_H

#include <stdio.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <stdlib.h>
#include <string.h>
#include <netinet/in.h>

#include "mcp_hal_types.h"


/*------------------------------------------------------------------------------
 * Common
 *
 *     Represents common configuration parameters.
 */

/*
 *	Indicates that a feature is enabled
 */
#define MCP_HAL_CONFIG_ENABLED									(MCP_TRUE)

/*
 *	Indicates that a feature is disabled
 */
#define MCP_HAL_CONFIG_DISABLED									(MCP_FALSE)

/*
 * Defines the number of chips that are in actual use in the system.
 *
 * Values may be 1 or 2
 */
#define MCP_HAL_CONFIG_NUM_OF_CHIPS								((McpUint)1)

/*
 *  The maximum number of bytes in one UFF-8 character
 */
#define MCP_HAL_CONFIG_MAX_BYTES_IN_UTF8_CHAR                   (2)

/*------------------------------------------------------------------------------
 * FS
 *
 *     Represents configuration parameters for FS module.
 */

/*
 *  Specifies the absolute path of the folder where MCP scripts should be
 *  located including the last delimiter
 */
#define MCP_HAL_CONFIG_FS_SCRIPT_FOLDER		                    ("/data/btips/TI/BTInitScript/")

/*
 *  The maximum length of a file system path, including file name component
 *  (in characters) and 0-terminator
*/
#define MCP_HAL_CONFIG_FS_MAX_PATH_LEN_CHARS                    (256)

/*
 *  The maximum name of the file name part on the local file system
 *  (in characters) and 0-terminator
*/
#define MCP_HAL_CONFIG_FS_MAX_FILE_NAME_LEN_CHARS               (256)

/*
 *  The maximum number of directories that may be opened simultaneously via the
 *  CP_HAL_FS_OpenDir()
*/
#define MCP_HAL_CONFIG_FS_MAX_NUM_OF_OPEN_DIRS				    (10)
/*
*   The folder separator character of files on the file system
 *	[ToDo] - What to do when the path is non-ASCII?
*/
#define MCP_HAL_CONFIG_FS_PATH_DELIMITER                        ('/')

/*
 *  Define if the file system is case sensitive
*/
#define MCP_HAL_CONFIG_FS_CASE_SENSITIVE                        (MCP_TRUE)

/*------------------------------------------------------------------------------
 * OS
 *
 *     Represents configuration parameters for OS module.
 */
                            
/* 	
 *	The total number of semaphores that are in use in the system
 *	
 *	BT: 5
 *	FM: 1 
 *
 *	[ToDo] - Mechainsm to adapt the number to the actual clients and their needs
 */
#define MCP_HAL_CONFIG_OS_MAX_NUM_OF_SEMAPHORES                 (6)

#define MCP_HAL_OS_MAX_ENTITY_NAME_LEN							(20)

#define LOG_FILE                                                "/mnt/tmp/btips_log.txt"

/* 
 * Sets log file maximum size in bytes. When this size is exceeded, the logfile
 *  is renamed to .prev and a new log file is created. Setting this value to
 * zero will result in ignoring size limitation.
 */
#define LOG_FILE_MAX_SIZE                                       0
#define CORE_DUMP_LOCATION                                      "/data/btips/"

#endif /* __MCP_HAL_CONFIG_CONFIG_H */

