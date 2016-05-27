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

/** \file   ads_api.h
 *  \brief  ULTS/ADS types specification file
 *
 *
 */

#ifndef ADS_API
#define ADS_API

#include "mcpf_defs.h"

/***** Internal Structures and Types defintions *****/
#define SYNC_WORD       0xF0
#define SYNC_END_WORD   0xF1

typedef struct
{
    McpU8  Sync;            // 0xf0
    McpU8  VersionDirFlag;
    McpU8  MsgClass;
    McpU8  MsgType;
    McpU8  ErrorStatus;
    McpU8  MsgLength[2]; //BE
    McpU8  reserved;
} AdsPrefix_t;

typedef struct
{
    McpU8  CS0;
    McpU8  CS1;
    McpU8  Sync;            // 0xf0
    McpU8  SyncEnd;         // 0xf1
} AdsSuffix_t;

// MsgClass
enum
{
    ADS_MC_GENERIC,
    ADS_MC_CONFIG,
    ADS_MC_SPECIFICATION,
    ADS_MC_ADD_FEATURE,
    ADS_MC_APP_MSG
};

// ADS_MC_GENERIC: MsgType
enum
{
  ADS_MC_GENERIC_MT_VERSION_REQ,
  ADS_MC_GENERIC_MT_VERSION_RESP,
  ADS_MC_GENERIC_MT_PING_REQ,
  ADS_MC_GENERIC_MT_PING_RESP
};

/* ADS_MC_CONFIG message types */
enum
{
  ADS_MC_CONFIG_MT_CONFIG_REQ,
  ADS_MC_CONFIG_MT_CONFIG_RESP,
  ADS_MC_CONFIG_MT_COLD_START_REQ,
  ADS_MC_CONFIG_MT_COLD_START_RESP,
  ADS_MC_CONFIG_MT_TRIG_PULSE_TIME,
  ADS_MC_CONFIG_MT_TRIG_PULSE_TIME_RESP,
  ADS_MC_CONFIG_MT_SESSION_END,
  ADS_MC_CONFIG_MT_SESSION_END_RESP
};

// ADS_MC_ADD_FEATURE:MsgType
enum
{
    ADS_MT_NO_REQUEST,
    ADS_MT_SBAS=0x01,
    ADS_MT_KALMAN=0x02,
    ADS_MT_HW=0x04,
    ADS_MT_MOTION_MASK=0x08,
    ADS_MT_APM=0x10
};

// ADS_MC_SPECIFICATION: MsgType
enum
{
    ADS_MC_SPECIFICATION_MT_RRLP,
    ADS_MC_SPECIFICATION_MT_RRLP_RESP,
    ADS_MC_SPECIFICATION_MT_RRC,
    ADS_MC_SPECIFICATION_MT_RRC_RESP
};

// ADS_MC_APP_MSG: MsgType
enum
{
    ADS_MT_APP_LOC_REQ,
    ADS_MT_APP_LOC_STOP_REQ,
    ADS_MT_APP_LOC_RESP,
    ADS_MT_APP_LOC_STOP_RESP,
    ADS_MT_SUPL_APP_LOC_REQ,
    ADS_MT_SUPL_APP_LOC_STOP_REQ,
    ADS_MT_APP_CALIB_CTRL_REQ,
    ADS_MT_APP_COARSE_POS_REQ,
    ADS_MT_APP_GPSC_INIT_REQ,
    ADS_MT_APP_MSSTAND_REQ,
    ADS_MT_APP_PRODUCT_LINE_TEST_REQ,
    ADS_MT_APP_GPSC_SHUTDOWN_REQ,
    ADS_MT_APP_PRODUCT_LINE_TEST_RESP,
    ADS_MT_APP_COARSE_TIME_REQ,
    ADS_MT_APP_MOTION_MASK_REQ,
    ADS_MT_APP_GPSC_VERSION_REQ,
    ADS_MT_APP_GPSC_VERSION_RESP,
    ADS_MT_APP_AUTO_FLAG_REQ
};



// ADS_MT_APP_LOC_REQ
typedef struct
{
    McpU8           SessionId;
    McpU8           LocFixMode; //0 - autonomous, 1 -  MS-BASED, 2- MS-ASSIST
    McpU16          LocFixBitmap;
    McpU16          nReports;   // number of reports
    McpU16          ReportPeriod;
    McpU16          MaxTtff;
} AdsLocFixReq_t;

// ADS_MT_APP_LOC_STOP_REQ
typedef struct
{
    McpU8           SessionId;
} AdsLocFixStop_t;




enum LCE_LOCREQ_STATUS_MESSAGES
{
    LCE_UNDEFINED,
    LCE_FAIL,
    LCE_SUCCESS
} LceLocReqStatusMsgs;



#endif /*ADS_API*/
