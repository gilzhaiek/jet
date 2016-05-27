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
*   FILE NAME:     mcp_hal_defs.h
*
*   BRIEF:          This file defines the common types, defines, and prototypes
*                   for the MCP HAL component.
*
*   DESCRIPTION:    General
*   
*                   The file holds common types , defines and prototypes, 
*                   used by the MCP HAL layer.
*
*   AUTHOR:         Udi Ron
*
\*******************************************************************************/

#ifndef __MCP_HAL_DEFS_H
#define __MCP_HAL_DEFS_H


/********************************************************************************
 *
 * Include files
 *
 *******************************************************************************/
#include "mcp_hal_types.h"

/********************************************************************************
 *
 * Types
 *
 *******************************************************************************/

/*-------------------------------------------------------------------------------
 * McpHalStatus Type
 *
 *  This is the common status type. Its values are generic status codes that apply to any
 *  MCP core stack (BT, FM, GPS).
 *
 *  Individual modules should DUPLICATE these values in their own definition files.
 *
 *  Values that are specific to a certain module should start from MCP_HAL_STATUS_OPEN.
 *  
 *  For consistency, the common codes should be DUPLICATED in specific modules. These duplicates
 *  must have the same value and the same meaning as the corresponding common code. That way,
 *  when a status code 0 (for example) is returned form any MCP / Core stack function (e.g., BT, or GPS),
 *  it always means SUCCESS.
 */
typedef McpUint McpHalStatus;

#define MCP_HAL_STATUS_SUCCESS          ((McpHalStatus)0)
#define MCP_HAL_STATUS_FAILED               ((McpHalStatus)1)
#define MCP_HAL_STATUS_PENDING          ((McpHalStatus)2)
#define MCP_HAL_STATUS_IN_PROGRESS      ((McpHalStatus)3)
#define MCP_HAL_STATUS_NO_RESOURCES     ((McpHalStatus)4)
#define MCP_HAL_STATUS_INVALID_PARM         ((McpHalStatus)5)
#define MCP_HAL_STATUS_NOT_SUPPORTED        ((McpHalStatus)6)
#define MCP_HAL_STATUS_TIMEOUT                  ((McpHalStatus)7)
#define MCP_HAL_STATUS_INTERNAL_ERROR   ((McpHalStatus)8)
#define MCP_HAL_STATUS_IMPROPER_STATE   ((McpHalStatus)9)

#define MCP_HAL_STATUS_OPEN             ((McpHalStatus)100)
                                                                  
/*-------------------------------------------------------------------------------
 * McpHalDateAndTime structure
 *
 *     Represents the date and time as a structure
 */
typedef struct tagMcpHalDateAndTime
{
    McpU16         year;    /* YYYY: e.g  2007 */
    McpU16         month;   /* MM: [1..12]     */
    McpU16         day;     /* DD: [1..31]     */
    McpU16         hour;    /* HH: [0..23]     */
    McpU16         minute;  /* MM: [0..59]     */
    McpU16         second;  /* SS: [0..59]     */

    /* UTC Time Zone Flag (TRUE = UTC time zone , FALSE = local time zone) */
    McpBool        utcTime; 
} McpHalDateAndTime;

/*
 *  McpHalChipId type
 *
 *  A logical identifier of a chip (e.g., 6450, 1273, 5500, etc).
*/
typedef enum tagMcpHalChipId {
    MCP_HAL_CHIP_ID_0,
    MCP_HAL_CHIP_ID_1,

    MCP_HAL_MAX_NUM_OF_CHIPS = 1,
    MCP_HAL_INVALID_CHIP_ID
} McpHalChipId;

/*
 *  McpHalTranId type
 *
 *  A logical identifier of a Transoprt of a  chip.
*/
typedef enum tagMcpHalTranId {
    MCP_HAL_TRAN_ID_0,
    MCP_HAL_TRAN_ID_1,
    MCP_HAL_TRAN_ID_2,

    MCP_HAL_MAX_NUM_OF_TRANS,
    MCP_HAL_INVALID_TRAN_ID
} McpHalTranId;


/*
 *  McpHalCoreId type
 *
 *  A logical identifier of a core in a chip
*/
typedef enum tagMcpHalCoreId {
    MCP_HAL_CORE_ID_BT,
    MCP_HAL_CORE_ID_FM,
    MCP_HAL_CORE_ID_GPS,

    MCP_HAL_MAX_NUM_OF_CORES,
    MCP_HAL_INVALID_CORE_ID
} McpHalCoreId;

#define DLL_EXPORT
#define DLL_IMPORT

#endif /* __MCP_HAL_DEFS_H */


