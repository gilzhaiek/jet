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
*   FILE NAME:      mcp_config_reader.c
*
*   BRIEF:          This file defines the implementation of the MCP configuration
*                   reader
*
*   DESCRIPTION:    The MCP configuration reader is a utility reading
*                   ini files, used by the MCP configuration parser.
*
*   AUTHOR:         Ronen Kalish
*
\*******************************************************************************/

#include "mcp_config_reader.h"
#include "mcp_hal_fs.h"
#include "mcp_defs.h"
#include "mcp_hal_log.h"

MCP_HAL_LOG_SET_MODULE(MCP_HAL_LOG_MODULE_TYPE_FRAME);

McpBool MCP_CONFIG_READER_Open (McpConfigReader *pConfigReader, 
                                McpUtf8 *pFileName, 
                                McpU8 *pMemConfig)
{
    McpHalFsStatus  eFsStatus;
    McpHalFsStat    tFsStat;

    MCP_FUNC_START ("MCP_CONFIG_READER_Open");

    MCP_LOG_INFO(("MCP_CONFIG_READER_Open: attempting to open config file %s or memory location 0x%p",
                  (NULL == pFileName ? (McpU8*)"NULL" : pFileName), pMemConfig));

    /* initialize config reader */
    pConfigReader->uFileSize = 0;
    pConfigReader->uFileBytesRead = 0;
    pConfigReader->pMemory = NULL;

    /* verify file name is not NULL */
    if (NULL != pFileName)
    {
        /* attempt to open the file */
        eFsStatus = MCP_HAL_FS_Open (pFileName, (MCP_HAL_FS_O_RDONLY | MCP_HAL_FS_O_TEXT), 
                                     &(pConfigReader->tFile));
        if (MCP_HAL_FS_STATUS_SUCCESS == eFsStatus)
        {
            /* file open succeeded, proceed to read file size */
            eFsStatus = MCP_HAL_FS_Stat (pFileName, &tFsStat);
            if (MCP_HAL_FS_STATUS_SUCCESS == eFsStatus)
            {
                pConfigReader->uFileSize = tFsStat.size;
                return MCP_TRUE;
            }
            else
            {
                MCP_LOG_ERROR (("MCP_CONFIG_READER_Open: failed get file information, status :%d", 
                                eFsStatus));
            }
        }
        else
        {
            MCP_LOG_ERROR (("MCP_CONFIG_READER_Open: failed opening file from FS, status :%d", 
                            eFsStatus));
        }
    }

    /* 
     * we got here either because file name is NULL or file open failed - in both cases
     * attempt to use memory configuration
     */
    if (NULL != pMemConfig)
    {
        pConfigReader->pMemory = pMemConfig;
         MCP_LOG_INFO(("MCP_CONFIG_READER_Open: opening file from memory, location:0x%p", 
                            pMemConfig));
        return MCP_TRUE;
    }

    MCP_FUNC_END ();

    /* both file and memory configurations failed to open */
    return MCP_FALSE;
}

McpBool MCP_CONFIG_READER_Close (McpConfigReader *pConfigReader)
{
    McpHalFsStatus  eFsStatus;

    /* if configuration is from a file */
    if (0 != pConfigReader->uFileSize)
    {
        eFsStatus = MCP_HAL_FS_Close (pConfigReader->tFile);
        if(eFsStatus == MCP_HAL_FS_STATUS_SUCCESS)
        {
            return MCP_TRUE;
        }
    }
    else
    {
        return MCP_TRUE;
    }

    return MCP_FALSE;
}

McpBool MCP_CONFIG_READER_getNextLine (McpConfigReader *pConfigReader,
                                       McpU8* pLine)
{
    McpU32              uNumRead, uIndex = 0;
    McpHalFsStatus      eFsStatus;
    McpBool             status;

    MCP_FUNC_START ("MCP_CONFIG_READER_getNextLine");

    pLine[ uIndex ] = '\0';

    /* if configuration is from a file */
    if (0 != pConfigReader->uFileSize)
    {
        /* verify EOF was not reached */
        if (pConfigReader->uFileBytesRead >= pConfigReader->uFileSize)
        {
            return MCP_FALSE;
        }

        /* read char by char until EOF or EOL */
        do
        {
            eFsStatus = MCP_HAL_FS_Read (pConfigReader->tFile, (void *)&(pLine[ uIndex ]), 1, &uNumRead);
            MCP_VERIFY_ERR ((MCP_HAL_FS_STATUS_SUCCESS == eFsStatus), MCP_FALSE,
                            ("MCP_CONFIG_READER_getNextLine: reading char returned status %d", eFsStatus));
            uIndex++;
            pConfigReader->uFileBytesRead++;
        } while ((pConfigReader->uFileBytesRead < pConfigReader->uFileSize) && /* EOF */
                 (0xA != pLine[ uIndex - 1 ])); /* EOL */

        /* if EOL was read */
        if (0xA == pLine[ uIndex - 1 ])
        {
            /*
             * DOS file format would end a line in CR + LF (0xD 0xA). Unix only with LF (0xA).
             * We read until LF is found, and than check for CR before that, and eliminate it as well
             * if it's found
             */
            /* replace EOL with string terminator */
            if ((uIndex > 1) && (0xD == pLine[ uIndex - 2]))
            {
                /* DOS file - replace the CR with null */
                pLine[ uIndex - 2 ] = '\0';
            }
            else
            {
                /* LINUX file - replace the LF with null */
            pLine[ uIndex - 1 ] = '\0';
        }
        }
        else
        {
            /* EOF was probably reached - last char read is valid, set string terminator to next char */
            pLine[ uIndex ] = '\0';
        }

        return MCP_TRUE;
    }
    /* configuration is from memory */
    else if (NULL != pConfigReader->pMemory)
    {
        /* read memory until EOL or string end is reached */
        while (('\n' != pConfigReader->pMemory[ uIndex ]) && /* EOL */
               ('\0' != pConfigReader->pMemory[ uIndex ]))   /* string end */
        {
            pLine[ uIndex ] = pConfigReader->pMemory[ uIndex ];
            uIndex++;
        }

        /* terminate the read string */
        pLine[ uIndex ] = '\0';

        /* if the configuration string was exhusted */
        if ('\0' == pConfigReader->pMemory[ uIndex ])
        {
            pConfigReader->pMemory = NULL;
        }
        else
        {
            /* advance pointer after the read section (+1 for the \n char itself) */
            pConfigReader->pMemory += (uIndex + 1);
        }

        return MCP_TRUE;
    }
    else
    {
        MCP_LOG_INFO (("MCP_CONFIG_READER_getNextLine: configuration was not opened successfully, or exhusted"));
    }
 
    MCP_FUNC_END ();

    return MCP_FALSE;
}

