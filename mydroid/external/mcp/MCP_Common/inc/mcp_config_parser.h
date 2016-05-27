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
*   FILE NAME:      mcp_config_parser.h
*
*   BRIEF:          Init file parser
*
*   DESCRIPTION:    The init file parser utility reads an init file,
*                   parse its content and supply values according to section and
*                   key name. It supports ASCII and integer key values
*
*   AUTHOR:         Ronen Kalish
*
\******************************************************************************/
#ifndef __MCP_CONFIG_PARSER_H__
#define __MCP_CONFIG_PARSER_H__

#include "mcp_hal_types.h"
#include "mcp_config_reader.h"
#include "mcp_config.h"

/*-------------------------------------------------------------------------------
 * McpConfigParserStatus type
 *
 *     MCP config parser statuses
 */
typedef enum _McpConfigParserStatus
{
    MCP_CONFIG_PARSER_STATUS_SUCCESS = 0,
    MCP_CONFIG_PARSER_STATUS_FAILURE_FILE_ERROR,
    MCP_CONFIG_PARSER_STATUS_FAILURE_KEY_DOESNT_EXIST
} McpConfigParserStatus;

/*-------------------------------------------------------------------------------
 * McpConfigParserSection type
 *
 *     MCP config parser section related ifnormation
 */
typedef struct _McpConfigParserSection
{
    McpU8       pSectionName[ MCP_CONFIG_PASRER_MAX_SECTION_LEN ];
    McpU32      uStartingKeyIndex;
    McpU32      uNumberOfKeys;
    McpU32      uLastFoundKeyIndex;
} McpConfigParserSection;

/*-------------------------------------------------------------------------------
 * McpConfigParser type
 *
 *     The MCP config parser object
 */
typedef struct _McpConfigParser
{
    
    McpU8                   pKeyNames[ MCP_CONFIG_PARSER_KEY_NUMBER ][ MCP_CONFIG_PASRER_MAX_KEY_NAME_LEN ];
    McpU8                   pKeyValues[ MCP_CONFIG_PARSER_KEY_NUMBER ][ MCP_CONFIG_PASRER_MAX_KEY_VALUE_LEN ];
    McpU32                  uKeysNum;
    McpConfigParserSection  tSections[ MCP_CONFIG_PARSER_SECTION_NUMBER ];
    McpU32                  uSectionNum;
} McpConfigParser;

/*-------------------------------------------------------------------------------
 * MCP_CONFIG_PARSER_Open()
 *
 * Brief:  
 *      Opens a new configuration parser object, read the file and parse it
 *      
 * Description:
 *      This function will initialize a config parser object, and attempt to read 
 *      the supplied ini file. If it fails it will attempt to read the supplied
 *      memory ini alternative. If any of these is available it will parse the 
 *      configuration object and save the parsed data.
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      pFileName       [in]    - the file to open (NULL if no file exist)
 *      pMemeConfig     [in]    - The alternative memory configuration (NULL if 
 *                                it shouldn't be used).
 *      pConfigParser   [in]    - The memory for the config parser object
 *
 * Returns:
 *      MCP_CONFIG_PARSER_STATUS_SUCCESS            - Configuration parsed successfully
 *      MCP_CONFIG_PARSER_STATUS_FAILURE_FILE_ERROR - File operation failed (or no 
 *                                                    configuration is available)
 */
McpConfigParserStatus MCP_CONFIG_PARSER_Open (McpUtf8 *pFileName, 
                                              McpU8 *pMemConfig, 
                                              McpConfigParser *pConfigParser);

/*-------------------------------------------------------------------------------
 * MCP_CONFIG_PARSER_GetAsciiKey()
 *
 * Brief:  
 *      Retrieves the ASCII value of a given key in a given section
 *      
 * Description:
 *      Retrieves the ASCII value of a given key in a given section. If more than
 *      one key with the same name exist in the section, the first call to this
 *      function will retrieve the value of the first key and any immediate calls
 *      with the same names will retrieve the following keys' values
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      pConfigParser   [in]    - Config parser object
 *      pSectionName    [in]    - Section in which the key is found
 *      pKeyName        [in]    - Key name
 *      pKeyAsciiValue  [out]   - The key value
 *
 * Returns:
 *      MCP_CONFIG_PARSER_STATUS_SUCCESS                    - Key retrieved
 *      MCP_CONFIG_PARSER_STATUS_FAILURE_KEY_DOESNT_EXIST   - Key was not found
 */
McpConfigParserStatus MCP_CONFIG_PARSER_GetAsciiKey (McpConfigParser *pConfigParser,
                                                     McpU8 *pSectionName,
                                                     McpU8 *pKeyName,
                                                     McpU8 **pKeyAsciiValue);

/*-------------------------------------------------------------------------------
 * MCP_CONFIG_PARSER_GetIntegerKey()
 *
 * Brief:  
 *      Retrieves the integer (32 bit signed) value of a given key in a given section
 *      
 * Description:
 *      Retrieves the integer value of a given key in a given section. If more than
 *      one key with the same name exist in the section, the first call to this
 *      function will retrieve the value of the first key and any immediate calls
 *      with the same names will retrieve the following keys' values
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      pConfigParser   [in]    - Config parser object
 *      pSectionName    [in]    - Section in which the key is found
 *      pKeyName        [in]    - Key name
 *      pKeyIntValue    [out]   - The key value
 *
 * Returns:
 *      MCP_CONFIG_PARSER_STATUS_SUCCESS                    - Key retrieved
 *      MCP_CONFIG_PARSER_STATUS_FAILURE_KEY_DOESNT_EXIST   - Key was not found
 */
McpConfigParserStatus MCP_CONFIG_PARSER_GetIntegerKey (McpConfigParser *pConfigParser,
                                                       McpU8 *pSectionName,
                                                       McpU8 *pKeyName,
                                                       McpS32 *pKeyIntValue);

#endif /* __MCP_CONFIG_PARSER_H__ */
