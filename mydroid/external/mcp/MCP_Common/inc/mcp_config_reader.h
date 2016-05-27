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
*   FILE NAME:      mcp_config_reader.h
*
*   BRIEF:          Init file reader
*
*   DESCRIPTION:    The init file reader utility handles ini configuration
*                   files and their memory replacement within a single object.
*                   It allows the file parser to receive one line at a time, 
*                   encapsulating file handling and differences between file
*                   and memory.
*
*   AUTHOR:         Ronen Kalish
*
\******************************************************************************/
#ifndef __MCP_CONFIG_READER_H__
#define __MCP_CONFIG_READER_H__

#include "mcp_hal_types.h"
#include "mcp_hal_fs.h"

/*-------------------------------------------------------------------------------
 * McpConfigParser type
 *
 *     The MCP config reader object
 */
typedef struct _McpConfigReader
{
    McpHalFsFileDesc        tFile;
    McpU32                  uFileSize;
    McpU32                  uFileBytesRead;
    McpU8                   *pMemory;
} McpConfigReader;

/*-------------------------------------------------------------------------------
 * MCP_CONFIG_READER_Open()
 *
 * Brief:  
 *      Opens a new configuration reader object, either opens a file or memory
 *      location
 *      
 * Description:
 *      This function will initialize a config reader object, and attempt to open 
 *      the supplied ini file. If it fails it will attempt to open the supplied
 *      memory ini alternative. 
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      pConfigParser   [in]    - The memory for the config reader object
 *      pFileName       [in]    - the file to open (NULL if no file exist)
 *      pMemeConfig     [in]    - The alternative memory configuration (NULL if 
 *                                it shouldn't be used).
 *
 * Returns:
 *      TRUE        - Configuration opened successfully
 *      FALSE       - Failed to open configuration object
 */
McpBool MCP_CONFIG_READER_Open (McpConfigReader *pConfigReader, 
                                McpUtf8 *pFileName, 
                                McpU8 *pMemConfig);
/*-------------------------------------------------------------------------------
 * MCP_CONFIG_READER_Close()
 *
 * Brief:  
 *		Closes the file indicated by pConfigReader->tFile if exists.
 *		
 * Description:
 *		Closes the file indicated by pConfigReader->tFile if exists.
 *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *		pConfigParser	[in]	- The memory for the config reader object
 */
McpBool MCP_CONFIG_READER_Close (McpConfigReader *pConfigReader);

/*-------------------------------------------------------------------------------
 * MCP_CONFIG_READER_getNextLine()
 *
 * Brief:  
 *      Retrieves the next line in a configuration storage object.
 *      
 * Description:
 *      Retrieves the next line in a configuration storage object.
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      pConfigParser   [in]    - The memory for the config reader object
 *      pFileName       [in]    - the file to open (NULL if no file exist)
 *      pMemeConfig     [in]    - The alternative memory configuration (NULL if 
 *                                it shouldn't be used).
 *
 * Returns:
 *      TRUE        - Configuration opened successfully
 *      FALSE       - Failed to open configuration object
 */
McpBool MCP_CONFIG_READER_getNextLine (McpConfigReader * pConfigReader,
                                       McpU8* pLine);

#endif /* __MCP_CONFIG_READER_H__ */
