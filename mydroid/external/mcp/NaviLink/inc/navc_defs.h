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


/** \file   Navc_defs.h 
 *  \brief  NAVC internal definitions
 * 
 * 
 */

#ifndef __NAVC_DEFS_H__
#define __NAVC_DEFS_H__

#include "mcpf_defs.h"
#include "gpsc_data.h"

/************************************************************************
 * Defines
 ************************************************************************/

#define NAVCD_TIMER_MAX_NUM 	30

#define NAVCD_ACCURACY_UNKNOWN 0xFFFF
#define NAVCD_TIMEOUT_UNKNOWN 0xFFFF

/* No of Sec to accomodate the network delay in RRC/RRLP path in Ctrl Plane tests*/
#define NAVCD_NETWORK_DELAY		2

/* NAVC/GPSC session id is 4 bytes fields that is used as:
 * Lsb byte	0: user defined session reference number/session id 
 *     byte 1: protocol id: RRLP, RRC, proprietary  
 * 	   byte	2: MCPF queue id of the task requesting the service
 * Msb byte	3: MCPF task id requesting the NAVC/GPSC			 
 */ 

#define NAVCD_SESSION_REF_NUM_SET(sesId, id)		((sesId & ~0x00FF) | ((McpU32)(id) & 0xFF))
#define NAVCD_SESSION_REF_NUM_GET(sesId)			((sesId) & 0xFF)

#define NAVCD_SESSION_PROT_ID_SET(sesId, protId)	((sesId & ~0xFF00) | ((McpU32)((protId) & 0xFF) << 8))
#define NAVCD_SESSION_PROT_ID_GET(sesId)			(((sesId) >> 8) & 0xFF)

#define NAVCD_SESSION_QUE_ID_SET(sesId, qId)		((sesId & ~0xFF0000) | ((McpU32)((qId) & 0xFF) << 16))
#define NAVCD_SESSION_QUE_ID_GET(sesId)				(((sesId) >> 16) & 0xFF)

#define NAVCD_SESSION_TASK_ID_SET(sesId, taskId)	((sesId & ~0xFF000000) | ((McpU32)((taskId) & 0xFF) << 24))
#define NAVCD_SESSION_TASK_ID_GET(sesId)			(((sesId) >> 24) & 0xFF)

/************************************************************************
 * Macros
 ************************************************************************/


/************************************************************************
 * Types
 ************************************************************************/

/*  NAVC internal event's opcodes */
typedef enum
{
	NAVCD_EVT_RECV_RX_IND = 1,
	NAVCD_EVT_TX_COMPL_IND,
	NAVCD_EVT_CCM_COMPL_IND,
	NAVCD_EVT_DISCRETE_CTRL,
    NAVCD_EVT_TIMEPULSE_REFCLK_CTRL,
    NAVCD_EVT_TIMER_EVT,
    NAVCD_EVT_TIMEPULSE_REFCLK_SENSOR_REQ,
    NAVCD_EVT_INJECT_SENSORASSIST_MSG,
    NAVCD_EVT_INJECT_APMCONFIG_MSG 
} ENAVCD_InternalEvtOpcode;

/*  NAVC internal states */
typedef enum
{
	NAVCD_STATE_STOPPED,
	NAVCD_STATE_INITIALIZED,
	NAVCD_STATE_WAIT_FOR_CCM_START_COMPLETE,
	NAVCD_STATE_WAIT_FOR_GPSC_START_COMPLETE,
	NAVCD_STATE_RUNNING,
	NAVCD_STATE_WAIT_FOR_GPSC_STOP_COMPLETE,
	NAVCD_STATE_WAIT_FOR_CCM_STOP_COMPLETE

} ENAVCD_navcState;


/*  NAVC supported protocols IDs as stored in session ID */
typedef enum
{
	NAVCD_PROT_PRIVATE,
	NAVCD_PROT_RRLP,
	NAVCD_PROT_RRC

} ENAVCD_ProtocolId;


/* Time reference */

typedef struct
{
	T_GPSC_time_assist 	tTime;
	McpU32      		uSystemTimeMs; 		/* system time in msec related to GPS time */
	McpBool				bValid;

} TNAVCD_GpsTime;

typedef struct
{
	TNAVCD_GpsTime	tGpsTime[NAVCD_TIME_MODE_MAX_NUM];
	McpU32			initTimeUnc;
	McpBool			bPulseReqEnable;

} TNAVCD_TimeEstimation;

/* for keeping the assistance src type and priority */
typedef struct
{
	McpU8			uAssistSrcType;
	McpU8			uAssistSrcPriority;
	McpU32          uAssistProviderTaskId;
	McpU32          uAssistProviderQueueId;
} TNAVCD_AssistSrcDb;




/*  NAVC control structure */
typedef struct
{
	handle_t 			hMcpf;
	handle_t 			hEvtPool;
	handle_t 			hSessPool;
	handle_t			hNavcSm;

	TNAVCD_TimeEstimation tGpsTimeEst;	

	handle_t  			hGpsc;
	handle_t 			hAds;
		
	handle_t 			hInavc;

	handle_t 			hTimers[NAVCD_TIMER_MAX_NUM];
	McpBool 			bCmdQueStopped;
	handle_t			hCmdQueStopTimer;

	McpU16				eCmdInProcess;

	EmcpTaskId 			eSrcTaskId;
	McpU8				uSrcQId;
	McpBool				bAssistDataFromPgpsSagps;

	EmcpTaskId 			eAssistSrcTaskId;
	McpU8				uAssistSrcQId;
	McpU32	            uSagpsProviderTaskId;

	TNAVCD_AssistSrcDb tAssistSrcDb[MAX_ASSIST_PROVIDER];

	McpU8			uPathCtrlFile[50];
	McpU32	            uwplTaskId;	

} TNAVCD_Ctrl;

/* Location error indication type */
typedef struct
{
  McpU8		number_octets;
  McpU8 	*p_B;
  McpU16 	uAssistBitMapMandatory;
  McpU16 	uAssistBitMapOptional;
  T_GPSC_nav_model_req_params * pNavModel;

} TNAVCD_LocError;

#endif /*__NAVC_DEFS_H__*/

