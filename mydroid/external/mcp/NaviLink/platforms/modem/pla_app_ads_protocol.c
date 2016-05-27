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

/** \file   pla_app_protocol.c 
 *  \brief  Platform Application Layer ADS/ULTS protocol implementation
 * 
 * 
 */

#include "mcpf_defs.h"
#include "mcpf_mem.h"
#include "mcpf_msg.h"
#include "mcpf_services.h"
#include "mcpf_time.h"
#include "mcpf_report.h"
#include "navc_api.h"
#include "navc_api_pvt.h"
#include "navc_rrlp.h"
#include "navc_rrc.h"
#include "pla_app_ads.h" 
#include "math.h"
#include "mcp_hal_os.h"


/************************************************************************
 * Defines
 ************************************************************************/
#define DEL_ALL_AIDING_DATA 0xFF

/************************************************************************
 * Types
 ************************************************************************/


/************************************************************************
 * Internal functions prototypes
 ************************************************************************/

static McpU8 get_byte(McpU8 **buf);
static McpU16 get_short(McpU8 **buf);
static McpU32 get_long(McpU8 **buf);
static void ads_process_generic_msg (AdsPort_t *pPort, McpU16 TaskId, AdsMsg_u *pMsg);
static void ads_process_add_feature (AdsPort_t *pPort, McpU16 TaskId, AdsMsg_u *pMsg);
static void ads_process_rr (AdsPort_t *pPort, McpU16 TaskId, AdsMsg_u *pMsg);
static void ads_process_app_msg (AdsPort_t *pPort, McpU16 TaskId, AdsMsg_u *pMsg);
static void ads_process_config_msg (AdsPort_t *pPort, McpU16 TaskId, AdsMsg_u *pMsg);

/************************************************************************
 *
 *   Module functions implementation
 *
 ************************************************************************/

/** 
 * \fn     ads_process_msg
 * \brief  Process recieved ADS message
 * 
 */
void ads_process_msg (AdsPort_t *pPort, McpU16 TaskId, AdsMsg_u *pMsg)
{
	switch (pMsg->s.Header.MsgClass)
	{
		case ADS_MC_GENERIC:
			ads_process_generic_msg (pPort, TaskId, pMsg);
		break;
		
	    case ADS_MC_CONFIG:
			ads_process_config_msg (pPort, TaskId, pMsg);
		break;
		
	    case ADS_MC_SPECIFICATION:
			ads_process_rr (pPort, TaskId, pMsg);
		break;

		case ADS_MC_ADD_FEATURE:
			ads_process_add_feature (pPort, TaskId, pMsg);
		break;
		
		/* message from application for LOC_REQ, LOC_STOP_REQ */
		case ADS_MC_APP_MSG:
			ads_process_app_msg (pPort, TaskId, pMsg);
		break;

	}
}
	       
/** 
 * \fn     ads_generate_msg
 * \brief  Generate ADS message for tranmitting over ADS port
 * 
 */
McpU16 ads_generate_msg (AdsPort_t *pPort, McpBool bAdsMsg, TmcpfMsg *pInMsg, AdsMsg_u *pOutMsg)
{
	Ads_t *hAds = pPort->hAds;
	McpU16 uLen = (McpU16) pInMsg->uLen;
	TNAVC_evt 	*pEvt = (TNAVC_evt *) pInMsg->pData;

	pOutMsg->s.Header.VersionDirFlag = (McpU8) 3; /* DBG, was 1 */;
	pOutMsg->s.Header.ErrorStatus = 0;
	if(bAdsMsg) /* ads generic messages (ping, version, etc */
	{
		pOutMsg->s.Header.MsgClass = (McpU8)pInMsg->uOpcode;
		pOutMsg->s.Header.MsgType = (McpU8)pInMsg->uUserDefined;
		pOutMsg->s.PayloadLength = 0;
	}
	else
	{
		switch(pInMsg->uOpcode)
		{
		case NAVC_EVT_POSITION_FIX_REPORT:


			switch (pPort->eProtocol)
			{
			case ADS_PROT_PRIVATE:
				pOutMsg->s.Header.MsgClass = (McpU8)ADS_MC_APP_MSG;
				pOutMsg->s.Header.MsgType  = (McpU8)ADS_MT_APP_LOC_RESP;
				mcpf_mem_copy(hAds->hMcpf, pOutMsg->s.Payload, &pEvt->tParams, pInMsg->uLen); 
				break;

			case ADS_PROT_RRLP:
				{
					TNAVC_evt *pEvt = (TNAVC_evt *) pInMsg->pData;

					pOutMsg->s.Header.MsgClass = (McpU8)ADS_MC_SPECIFICATION;
					pOutMsg->s.Header.MsgType  = (McpU8)ADS_MC_SPECIFICATION_MT_RRLP;
	
					/* Encode RRLP Response message */
					uLen = RRLP_encodeResponse (pPort->hRrlp, 
												(T_GPSC_loc_fix_response *) &pEvt->tParams.tLocFixReport,
												NULL, 
												C_RRLP_REP_POS, 
												C_RRLP_LOC_REASON_GOOD, 
												pOutMsg->s.Payload);
				}
				break;

			case ADS_PROT_RRC:
				{
					TNAVC_evt *pEvt = (TNAVC_evt *) pInMsg->pData;

					pOutMsg->s.Header.MsgClass = (McpU8)ADS_MC_SPECIFICATION;
					pOutMsg->s.Header.MsgType  = (McpU8)ADS_MC_SPECIFICATION_MT_RRC;

					/* Encode RRC Response message */
					uLen = RRC_encodeResponse (pPort->hRrc, 
											   (T_GPSC_loc_fix_response *) &pEvt->tParams.tLocFixReport,
											   NULL, 
											   RRC_REP_POS, 
											   C_RRC_LOC_REASON_GOOD, 
											   pOutMsg->s.Payload);
				}
				break;

			default:
				MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG, 
								  ("ads_generate_msg: bad protocol id=%u\n",pPort->eProtocol));
				return 0;
				break;
			}

			break;  

		case NAVC_EVT_ASSISTANCE_REQUEST:

			switch (pPort->eProtocol)
			{
			case ADS_PROT_PRIVATE:
				/* Not supported */
				return 0;
				break;

			case ADS_PROT_RRLP:
				{
					TNAVC_evt *pEvt = (TNAVC_evt *) pInMsg->pData;
					TNAVCD_LocError	tLocError;

					tLocError.uAssistBitMapMandatory = pEvt->tParams.tAssistReq.uAssistBitmapMandatory;
					tLocError.uAssistBitMapOptional  = pEvt->tParams.tAssistReq.uAssistBitmapOptional;
					tLocError.pNavModel = &pEvt->tParams.tAssistReq.tNavModelReqParams;

					pOutMsg->s.Header.MsgClass = (McpU8)ADS_MC_SPECIFICATION;
					pOutMsg->s.Header.MsgType  = (McpU8)ADS_MC_SPECIFICATION_MT_RRLP_RESP;
	
					/* Encode RRLP Response message */
					uLen = RRLP_encodeResponse (pPort->hRrlp, 
												NULL,
												&tLocError, 
												C_RRLP_REP_LOCERR, 
												C_RRLP_LOC_REASON_GPS_ASS_MIS, 
												pOutMsg->s.Payload);
				}
				break;

			case ADS_PROT_RRC:
				{
					TNAVC_evt *pEvt = (TNAVC_evt *) pInMsg->pData;
					TNAVCD_LocError	tLocError;


					tLocError.uAssistBitMapMandatory = pEvt->tParams.tAssistReq.uAssistBitmapMandatory;
					tLocError.uAssistBitMapOptional  = pEvt->tParams.tAssistReq.uAssistBitmapOptional;
					tLocError.pNavModel = &pEvt->tParams.tAssistReq.tNavModelReqParams;

					pOutMsg->s.Header.MsgClass = (McpU8)ADS_MC_SPECIFICATION;
					pOutMsg->s.Header.MsgType  = (McpU8)ADS_MC_SPECIFICATION_MT_RRC_RESP;

					/* Encode RRC Response message */
					uLen = RRC_encodeResponse (pPort->hRrc, 
											   NULL,
											   &tLocError, 
											   RRC_REP_LOCERR, 
											   C_RRC_LOC_REASON_ASSIS_MIS, 
											   pOutMsg->s.Payload);
				}
				break;

			default:
				MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG, 
								  ("ads_generate_msg: bad protocol id=%u\n",pPort->eProtocol));
				return 0;
				break;
			}

			break;  

 		case NAVC_EVT_RRLP_ACK:
			pOutMsg->s.Header.MsgClass = (McpU8)ADS_MC_SPECIFICATION;
			pOutMsg->s.Header.MsgType  = (McpU8)ADS_MC_SPECIFICATION_MT_RRLP_RESP;
			mcpf_mem_copy(hAds->hMcpf, pOutMsg->s.Payload, &pEvt->tParams, uLen); 
			break;  

		case NAVC_EVT_RRC_ACK:
			pOutMsg->s.Header.MsgClass = (McpU8)ADS_MC_SPECIFICATION;
			pOutMsg->s.Header.MsgType  = (McpU8)ADS_MC_SPECIFICATION_MT_RRC_RESP;
			mcpf_mem_copy(hAds->hMcpf, pOutMsg->s.Payload, &pEvt->tParams, uLen); 
			break;  

		case NAVC_EVT_RRLP_RESP:
			pOutMsg->s.Header.MsgClass = (McpU8)ADS_MC_SPECIFICATION;
			pOutMsg->s.Header.MsgType  = (McpU8)ADS_MC_SPECIFICATION_MT_RRLP_RESP;
			mcpf_mem_copy(hAds->hMcpf, pOutMsg->s.Payload, &pEvt->tParams, uLen); 
			break;  

		case NAVC_EVT_RRC_RESP:
			pOutMsg->s.Header.MsgClass = (McpU8)ADS_MC_SPECIFICATION;
			pOutMsg->s.Header.MsgType  = (McpU8)ADS_MC_SPECIFICATION_MT_RRC_RESP;
			mcpf_mem_copy(hAds->hMcpf, pOutMsg->s.Payload, &pEvt->tParams, uLen); 
			break;

		case NAVC_EVT_PLT_RESPONSE:
			pOutMsg->s.Header.MsgClass = (McpU8)ADS_MC_APP_MSG;
			pOutMsg->s.Header.MsgType  = (McpU8)ADS_MT_APP_PRODUCT_LINE_TEST_RESP;
			mcpf_mem_copy(hAds->hMcpf, pOutMsg->s.Payload, &pEvt->tParams, uLen); 
			break;  

		case NAVC_EVT_VERSION_RESPONSE:
			pOutMsg->s.Header.MsgClass = (McpU8)ADS_MC_APP_MSG;
			pOutMsg->s.Header.MsgType  = (McpU8)ADS_MT_APP_GPSC_VERSION_RESP;
			mcpf_mem_copy(hAds->hMcpf, pOutMsg->s.Payload, &pEvt->tParams, uLen); 
			break;

		case NAVC_EVT_REQUEST_PULSE:
			MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG, 
							  ("ads_generate_msg: request pulse.\n"));
			/* TBD */
			break;

		default:
			MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG, 
							  ("ads_generate_msg: usupported msg opcode=%u\n", pInMsg->uOpcode));
			return 0;
			break;  
		}

		pOutMsg->s.PayloadLength = (McpU16) uLen;
		pOutMsg->s.Header.MsgLength[0] = (McpU8) ((uLen & 0xff00) >> 8);
		pOutMsg->s.Header.MsgLength[1] = (McpU8) (uLen & 0xff);
	}
	return (McpU16)(uLen + sizeof(AdsPrefix_t));
}

/** 
 * \fn     get_byte 
 * \brief  Get byte value from the buffer
 * 
 * Get byte value from the buffer and increment buffer pointer
 * 
 * \note
 * \param	buf - address of buffer pointer
 * \return 	byte value
 * \sa     	get_short, get_long
 */
static McpU8 get_byte(McpU8 **buf)
{
	McpU8 res = **buf;
	(*buf)++;
	return res;
}

/** 
 * \fn     get_short 
 * \brief  Get short (two bytes) value from the buffer
 * 
 * Get short (two bytes) value from the buffer and increment buffer pointer
 * 
 * \note
 * \param	buf - address of buffer pointer
 * \return 	byte value
 * \sa     	get_byte, get_long
 */
static McpU16 get_short(McpU8 **buf)
{
	McpU16 res = mcpf_endian_BEtoHost16(*buf);
	(*buf)+=2;
	return res;
}

/** 
 * \fn     get_long 
 * \brief  Get long (4 bytes) value from the buffer
 * 
 * Get long (4 bytes) value from the buffer and increment buffer pointer
 * 
 * \note
 * \param	buf - address of buffer pointer
 * \return 	byte value
 * \sa     	get_byte, get_long
 */
static McpU32 get_long(McpU8 **buf)
{
	McpU32 res = mcpf_endian_BEtoHost32(*buf);
	(*buf)+=4;
	return res;
}

/** 
 * \fn     ads_process_generic_msg 
 * \brief  Process ADS generic class message
 * 
 * Process ADS generic class message types
 * 
 * \note
 * \param	pPort  - ADS port handler
 * \param	TaskId - ADS registered MCPF task id
 * \param	pMsg   - received ADS  message
 * \return 	void         
 * \sa     	ads_process_msg
 */
static void ads_process_generic_msg (AdsPort_t *pPort, McpU16 TaskId, AdsMsg_u *pMsg)
{
	Ads_t *hAds = pPort->hAds;

	switch (pMsg->s.Header.MsgType)
	{
		case ADS_MC_GENERIC_MT_VERSION_REQ:
			
			MCPF_REPORT_INFORMATION(hAds->hMcpf, ADS_MODULE_LOG, ("From ULTS:Received Version request\n"));
			mcpf_SendMsg(hAds->hMcpf, TaskId, 0, TaskId, 0, ADS_MC_GENERIC, 0, ADS_MC_GENERIC_MT_VERSION_RESP, NULL);
			MCPF_REPORT_INFORMATION(hAds->hMcpf, ADS_MODULE_LOG, ("To ULTS : Sent Version response\n"));
			break;
			
		case ADS_MC_GENERIC_MT_PING_REQ:
			MCPF_REPORT_INFORMATION(hAds->hMcpf, ADS_MODULE_LOG, ("\nFrom ULTS:Received Ping request\n"));
			mcpf_SendMsg(hAds->hMcpf, TaskId, 0, TaskId, 0, ADS_MC_GENERIC, 0, ADS_MC_GENERIC_MT_PING_RESP, NULL);
			MCPF_REPORT_INFORMATION(hAds->hMcpf, ADS_MODULE_LOG, ("To ULTS : Sent Ping response\n"));
			break;
			
		default:
			MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG, ( "LM ERR: Unsupported Message Type %d\n", pMsg->s.Header.MsgType));
		break;
	}
	
}

/** 
 * \fn     ads_process_config_msg 
 * \brief  Process ADS generic class message
 * 
 * Process ADS generic class message types
 * 
 * \note
 * \param	pPort  - ADS port handler
 * \param	TaskId - ADS registered MCPF task id
 * \param	pMsg   - received ADS  message
 * \return 	void         
 * \sa     	ads_process_msg
 */
static void ads_process_config_msg (AdsPort_t *pPort, McpU16 TaskId, AdsMsg_u *pMsg)
{
	Ads_t *hAds = pPort->hAds;
	McpU8 *cmd  = (McpU8 *) &pMsg->s.Payload;
	TNAVC_cmdParams *pNavcCmd;
	EMcpfRes		 eRes;

	switch (pMsg->s.Header.MsgType)
	{
	case ADS_MC_CONFIG_MT_CONFIG_REQ:
		{
			McpU8 timeMode, sesMode;

			mcpf_SendMsg(hAds->hMcpf, TaskId, 0, TaskId, 0, ADS_MC_CONFIG, 0, ADS_MC_CONFIG_MT_CONFIG_RESP, NULL);

			timeMode = get_byte(&cmd);
			sesMode  = get_byte(&cmd);

			MCPF_REPORT_INFORMATION(hAds->hMcpf, ADS_MODULE_LOG, 
									("ULTS Config request: timeMode=%u sesMode=%u\n", timeMode, sesMode));
		}
		break;

	case ADS_MC_CONFIG_MT_COLD_START_REQ:

		MCPF_REPORT_INFORMATION(hAds->hMcpf, ADS_MODULE_LOG, ("ULTS Cold Start request\n"));

		mcpf_SendMsg(hAds->hMcpf, TaskId, 0, TaskId, 0, ADS_MC_CONFIG, 0, ADS_MC_CONFIG_MT_COLD_START_RESP, NULL);

		/* Delete assistance */
		pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (hAds->hMcpf, hAds->hCmdPool);

		if (pNavcCmd)
		{
			pNavcCmd->tDelAssist.uDelAssistBitmap = DEL_ALL_AIDING_DATA;
			pNavcCmd->tDelAssist.uSvBitmap = 0;

			eRes = mcpf_SendMsg (hAds->hMcpf, 
								 TASK_NAV_ID, 
								 NAVC_QUE_CMD_ID , 
								 TaskId, 
								 0, 
								 NAVC_CMD_DELETE_ASISTANCE, 
								 sizeof(pNavcCmd->tDelAssist), 
								 0, 
								 pNavcCmd);

			if (eRes != RES_OK)
			{
				mcpf_mem_free_from_pool (hAds->hMcpf, pNavcCmd);
				MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG, ("ads_process_config_msg: mcpf_SendMsg failed!\n"));
			}
		}
		else
		{
			MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG, ("ads_process_config_msg: allocation failed!\n"));
		}
		
		/* TBD */
		break;

	case ADS_MC_CONFIG_MT_TRIG_PULSE_TIME:

		mcpf_SendMsg(hAds->hMcpf, TaskId, 0, TaskId, 0, ADS_MC_CONFIG, 0, ADS_MC_CONFIG_MT_TRIG_PULSE_TIME_RESP, NULL);

		pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool(hAds->hMcpf, hAds->hCmdPool);

		if (pNavcCmd)
		{
			McpU32 uBias;

			pNavcCmd->tSetTow.tTimeAssist.gps_week = (McpU16) (get_short(&cmd) + 1024);
			pNavcCmd->tSetTow.tTimeAssist.gps_msec = get_long(&cmd);

			uBias = (McpU32) (get_byte(&cmd) << 16);			/* Read 3 bytes clock bias */
			uBias = (McpU32) (uBias + (get_byte(&cmd) << 8));
			uBias = (McpU32) (uBias + get_byte(&cmd));
			pNavcCmd->tSetTow.tTimeAssist.sub_ms   = uBias;
			pNavcCmd->tSetTow.tTimeAssist.time_unc = get_short(&cmd) / 1000; 		/* convert to microseconds */
			pNavcCmd->tSetTow.tTimeAssist.time_accuracy = GPSC_TIME_ACCURACY_FINE_LAST_PULSE;
			pNavcCmd->tSetTow.eTimeMode = NAVCD_TIME_PULSE;
			pNavcCmd->tSetTow.uSystemTimeInMsec = mcpf_getCurrentTime_InMilliSec(hAds->hMcpf);

			eRes = mcpf_SendMsg (hAds->hMcpf, 
								 TASK_NAV_ID, 
								 NAVC_QUE_CMD_ID , 
								 TaskId, 
								 0,
								 NAVC_CMD_SET_TOW, 
								 sizeof(pNavcCmd->tSetTow), 
								 0, 
								 pNavcCmd);

			if (eRes != RES_OK)
			{
				mcpf_mem_free_from_pool (hAds->hMcpf, pNavcCmd);
				MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG, ("ads_process_config_msg: mcpf_SendMsg failed!\n"));
			}

			MCPF_REPORT_INFORMATION(hAds->hMcpf, ADS_MODULE_LOG, 
									("ULTS Pulse Time: week=%u msec=%u bias=%u unc=%u\n", 
									 (McpU32) pNavcCmd->tSetTow.tTimeAssist.gps_week,
									 (McpU32) pNavcCmd->tSetTow.tTimeAssist.gps_msec,
									 (McpU32) pNavcCmd->tSetTow.tTimeAssist.sub_ms,
									 (McpU32) pNavcCmd->tSetTow.tTimeAssist.time_unc));
		}
		else
		{
			MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG, ("ads_process_config_msg: allocation failed!\n"));
		}
		break;

	case ADS_MC_CONFIG_MT_SESSION_END:

		MCPF_REPORT_INFORMATION(hAds->hMcpf, ADS_MODULE_LOG, ("ULTS Session End request\n"));

		mcpf_SendMsg(hAds->hMcpf, TaskId, 0, TaskId, 0, ADS_MC_CONFIG, 0, ADS_MC_CONFIG_MT_SESSION_END_RESP, NULL);

		/* Delete assistance */
		pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (hAds->hMcpf, hAds->hCmdPool);

		if (pNavcCmd)
		{
			pNavcCmd->tDelAssist.uDelAssistBitmap = 0x3F;
			pNavcCmd->tDelAssist.uSvBitmap = 0;

			eRes = mcpf_SendMsg (hAds->hMcpf, 
								 TASK_NAV_ID, 
								 NAVC_QUE_CMD_ID , 
								 TaskId, 
								 0, 
								 NAVC_CMD_DELETE_ASISTANCE, 
								 sizeof(pNavcCmd->tDelAssist), 
								 0, 
								 pNavcCmd);

			if (eRes != RES_OK)
			{
				mcpf_mem_free_from_pool (hAds->hMcpf, pNavcCmd);
				MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG, ("ads_process_config_msg: mcpf_SendMsg failed!\n"));
			}
		}
		else
		{
			MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG, ("ads_process_config_msg: allocation failed!\n"));
		}

		/* Stop current session */
		pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (hAds->hMcpf, hAds->hCmdPool);

		if (pNavcCmd)
		{
			pNavcCmd->tStopLocFix.uSessionId = C_SESSION_INVALID;

			eRes = mcpf_SendMsg (hAds->hMcpf, 
								 TASK_NAV_ID, 
								 NAVC_QUE_CMD_ID , 
								 TaskId, 
								 0, 
								 NAVC_CMD_STOP_FIX, 
								 sizeof(pNavcCmd->tStopLocFix), 
								 0, 
								 pNavcCmd);

			if (eRes != RES_OK)
			{
				mcpf_mem_free_from_pool (hAds->hMcpf, pNavcCmd);
				MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG, ("ads_process_config_msg: mcpf_SendMsg failed!\n"));
			}
		}
		else
		{
			MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG, ("ads_process_config_msg: allocation failed!\n"));
		}
		break;
			
	default:
		MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG, 
						  ("ads_process_config_msg: unknown msg type=%u\n", pMsg->s.Header.MsgType));
		break;
	}
}


/** 
 * \fn     ads_process_add_feature 
 * \brief  Process ADS ADD_FEATURE class message
 * 
 * Process ADS ADD_FEATURE class message types
 * 
 * \note
 * \param	pPort - ADS port handler
 * \param	TaskId - ADS registered MCPF task id
 * \param	pMsg   - received ADS  message
 * \return 	void         
 * \sa     	ads_process_msg
 */
static void ads_process_add_feature (AdsPort_t *pPort, McpU16 TaskId, AdsMsg_u *pMsg)
{
	Ads_t *hAds = pPort->hAds;
	TNAVC_cmdParams *pNavcCmd = (TNAVC_cmdParams*)mcpf_mem_alloc_from_pool(hAds->hMcpf, hAds->hCmdPool);
	McpU8 *cmd = (McpU8 *)&pMsg->s.Payload;
	McpU16		uOpcode = 0;
	McpU32		uLen = 0;

	if(pNavcCmd == NULL)
		return;

	/* Need to ignore first 3 bytes of message (2 bytes Message Len & 1 byte Reserved) */
	get_short(&cmd);
	get_byte(&cmd);

	switch (pMsg->s.Header.MsgType)
	{
	   case ADS_MT_HW:
	   		uOpcode = NAVC_CMD_SET_HOST_WAKEUP_PARAMS;
			uLen = sizeof(TNAVC_SetHostWakeupParams);
             /* for NL5500 */
			pNavcCmd->tSetHostWakeupParams.host_req_opt = get_byte(&cmd);
			pNavcCmd->tSetHostWakeupParams.host_assert_delay = get_short(&cmd);
			pNavcCmd->tSetHostWakeupParams.host_reassert_delay = get_short(&cmd);
			pNavcCmd->tSetHostWakeupParams.host_ref_clk_req_opt = get_byte(&cmd);
			pNavcCmd->tSetHostWakeupParams.host_ref_clk_req_sig_sel = get_byte(&cmd);
			pNavcCmd->tSetHostWakeupParams.host_ref_clk_assert_dly = get_short(&cmd);
			pNavcCmd->tSetHostWakeupParams.host_ref_clk_reassert_dly = get_short(&cmd);
			pNavcCmd->tSetHostWakeupParams.host_sigout_type_ctrl = get_byte(&cmd);

	   break;

	   case ADS_MT_SBAS:
	   		uOpcode = NAVC_CMD_SET_SBAS_PARAMS;
			uLen = sizeof(TNAVC_SetSBASParams);
			
			pNavcCmd->tSetSBASParams.sbas_control = get_byte(&cmd);
			pNavcCmd->tSetSBASParams.sbas_prn_mask = get_long(&cmd);
			pNavcCmd->tSetSBASParams.Mode = get_byte(&cmd);
			pNavcCmd->tSetSBASParams.Flags = get_byte(&cmd);
			pNavcCmd->tSetSBASParams.rsvd_sbas1 = get_long(&cmd);
			pNavcCmd->tSetSBASParams.rsvd_sbas2 = get_long(&cmd);
	   break;

	   case ADS_MT_MOTION_MASK:
		   /*	uOpcode = NAVC_CMD_SET_MOTION_MASK;
			uLen = sizeof(TNAVC_SetMotionMask);
			
			pNavcCmd->tSetMotionMask.motion_mask_control = get_byte(&cmd);
			pNavcCmd->tSetMotionMask.area_origin_latitude = get_long(&cmd);
			pNavcCmd->tSetMotionMask.area_origin_longitude = get_long(&cmd);
			pNavcCmd->tSetMotionMask.area_origin_altitude = get_short(&cmd);
			pNavcCmd->tSetMotionMask.radius_of_circle = get_short(&cmd);
			pNavcCmd->tSetMotionMask.altitude_limit = get_short(&cmd);
			pNavcCmd->tSetMotionMask.speed_limit = get_short(&cmd); */
		break;

		case ADS_MT_APM:
			uOpcode = NAVC_CMD_SET_APM_PARAMS;
			uLen = sizeof(TNAVC_SetAPMParams);
			
			pNavcCmd->tSetAPMParams.apm_control = get_byte(&cmd);
			pNavcCmd->tSetAPMParams.Rsvd_pwr_mgmt1 = get_byte(&cmd);
			pNavcCmd->tSetAPMParams.search_mode = get_byte(&cmd);
			pNavcCmd->tSetAPMParams.saving_options = get_byte(&cmd);
			pNavcCmd->tSetAPMParams.power_save_qc = get_byte(&cmd);
			pNavcCmd->tSetAPMParams.Rsvd_pwr_mgmt2 = get_byte(&cmd);
			pNavcCmd->tSetAPMParams.Rsvd_pwr_mgmt3 = get_byte(&cmd);
		break;
	}

	mcpf_SendMsg(hAds->hMcpf, TASK_NAV_ID, NAVC_QUE_CMD_ID , TaskId, 0, uOpcode, uLen, 0, pNavcCmd);
}

/** 
 * \fn     ads_process_rr 
 * \brief  Process ADS RRC/RRLP class message
 * 
 * Process ADS RRC and RRLP message types
 * 
 * \note
 * \param	pPort  - ADS port handler
 * \param	TaskId - ADS registered MCPF task id
 * \param	pMsg   - received ADS  message
 * \return 	void         
 * \sa     	ads_process_msg
 */
static void ads_process_rr (AdsPort_t *pPort, McpU16 TaskId, AdsMsg_u *pMsg)
{
	Ads_t 	 *hAds = pPort->hAds;
	McpU32	 uSystemTimeInMsec = mcpf_getCurrentTime_InMilliSec(hAds->hMcpf);
	EMcpfRes eRes;


	switch (pMsg->s.Header.MsgType)
	{
		case ADS_MC_SPECIFICATION_MT_RRLP:

			MCPF_REPORT_INFORMATION(hAds->hMcpf, ADS_MODULE_LOG, 
									("ads_process_rr: received RRLP Msg from ULTS, len=%u\n", pMsg->s.PayloadLength));

			/* Send Ack Response for the RRLP msg */
			mcpf_SendMsg (hAds->hMcpf, 
						  TaskId, 
						  0, 
						  TASK_NAV_ID,
						  0,
						  NAVC_EVT_RRLP_ACK, 
						  0, 0, NULL);

			pPort->eProtocol = NAVCD_PROT_RRLP;

			eRes = RRLP_processRxMsg (pPort->hRrlp, 
									  pMsg->s.Payload,
									  pMsg->s.PayloadLength,
									  uSystemTimeInMsec);
			if (eRes != RES_OK)
			{
				MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG, ("ads_process_rr: RRLP msg processing failed\n"));
			}
		break;
		
		case ADS_MC_SPECIFICATION_MT_RRC:

			MCPF_REPORT_INFORMATION(hAds->hMcpf, ADS_MODULE_LOG, 
									("ads_process_rr: received RRC Msg from ULTS, len=%u\n", pMsg->s.PayloadLength));

			/* Send Ack Response for the RRC msg */
			mcpf_SendMsg (hAds->hMcpf, 
						  TaskId, 
						  0, 
						  TASK_NAV_ID,
						  0,
						  NAVC_EVT_RRC_ACK, 
						  0, 0, NULL);
			
			pPort->eProtocol = ADS_PROT_RRC;

			eRes = RRC_processRxMsg (pPort->hRrc, 
									 pMsg->s.Payload,
									 pMsg->s.PayloadLength,
									 uSystemTimeInMsec);
			if (eRes != RES_OK)
			{
				MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG, ("ads_process_rr: RRC msg processing failed\n"));
			}
		break;
		
		default:
			MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG, ("Unsupported Message Type %d\n", pMsg->s.Header.MsgType));
		break;
	}

}

/** 
 * \fn     ads_process_app_msg 
 * \brief  Process ADS application class message
 * 
 * Process ADS application message types
 * 
 * \note
 * \param	hAds - ADS protocol handler
 * \param	TaskId - ADS registered MCPF task id
 * \param	pMsg   - received ADS  message
 * \return 	void         
 * \sa     	ads_process_msg
 */
static void ads_process_app_msg (AdsPort_t *pPort, McpU16 TaskId, AdsMsg_u *pMsg)
{
	Ads_t  *hAds = pPort->hAds;
	TNAVC_cmdParams *pNavcCmd = (TNAVC_cmdParams*)mcpf_mem_alloc_from_pool(hAds->hMcpf, hAds->hCmdPool);
	McpU16 uOpcode = 0;
	McpU32 uLen = 0;
	McpU8 *cmd = (McpU8 *)pMsg->s.Payload;
	McpS32 status = 0;

	if(pNavcCmd == NULL)
		return ;

	switch (pMsg->s.Header.MsgType)
	{
		case ADS_MT_APP_LOC_REQ:
		{
			pNavcCmd->tReqLocFix.loc_fix_session_id = (U16)(((get_byte(&cmd))&0xff) | ((TaskId&0xff)<<8));
			pNavcCmd->tReqLocFix.loc_fix_mode = (T_GPSC_loc_fix_mode)get_byte(&cmd);
			pNavcCmd->tReqLocFix.loc_fix_result_type_bitmap = get_short(&cmd);
			pNavcCmd->tReqLocFix.loc_fix_num_reports = get_short(&cmd);
			pNavcCmd->tReqLocFix.loc_fix_period = get_short(&cmd);
			pNavcCmd->tReqLocFix.loc_fix_max_ttff = get_short(&cmd);
			uOpcode = NAVC_CMD_REQUEST_FIX;
			uLen = sizeof(pNavcCmd->tReqLocFix);
			pPort->eProtocol = ADS_PROT_PRIVATE;
		}
		break;
				
		case ADS_MT_APP_LOC_STOP_REQ:
		{				
			pNavcCmd->tStopLocFix.uSessionId = (U16)(((get_byte(&cmd))&0xff) | ((TaskId&0xff)<<8));
			uOpcode = NAVC_CMD_STOP_FIX;
			uLen = sizeof(pNavcCmd->tStopLocFix);
				
		}
		break;

		case ADS_MT_APP_GPSC_INIT_REQ:
		{
			uOpcode = NAVC_CMD_START;
		}
		break;
				
		case ADS_MT_APP_GPSC_SHUTDOWN_REQ: 
		{
			uOpcode = NAVC_CMD_STOP;
		}
		break;

		case ADS_MT_APP_PRODUCT_LINE_TEST_REQ:
		{
			 
			McpU8 TestType = get_byte(&cmd);
                        TNAVC_pltUnion *pNavcPlt;
			uOpcode = NAVC_CMD_PLT;
pNavcPlt = (TNAVC_pltUnion*)&pNavcCmd;
			//uLen = sizeof(pNavcCmd->tPltParams);
 
                       uLen = sizeof(TNAVC_plt);
					
			switch(TestType)
			{
				case 0:
				case 1:
				case 2:
					pNavcPlt->tPltParams.req_type = get_long(&cmd);
					pNavcPlt->tPltParams.timeout = get_long(&cmd);
					pNavcPlt->tPltParams.svid = get_byte(&cmd);
				break;

				case 3:
					pNavcPlt->tPltParams.req_type = get_long(&cmd);
					pNavcPlt->tPltParams.timeout = get_long(&cmd);
					pNavcPlt->tPltParams.cw_test_ver = get_byte(&cmd);
				break;
				
				case 4:
					pNavcPlt->tPltParams.req_type = get_long(&cmd);
					pNavcPlt->tPltParams.timeout = get_long(&cmd);
					pNavcPlt->tPltParams.svid = get_byte(&cmd);
					pNavcPlt->tPltParams.cw_test_ver = get_byte(&cmd);
					pNavcPlt->tPltParams.termination_event = get_byte(&cmd);
					pNavcPlt->tPltParams.cw_test_params.test_request = get_byte(&cmd);
					pNavcPlt->tPltParams.cw_test_params.start_delay = get_byte(&cmd);
					pNavcPlt->tPltParams.cw_test_params.wideband_centfreq = get_long(&cmd);
					pNavcPlt->tPltParams.cw_test_params.narrowband_centfreq = get_long(&cmd);
					pNavcPlt->tPltParams.cw_test_params.wideband_peaks = get_byte(&cmd);
					pNavcPlt->tPltParams.cw_test_params.wideband_adj_samples = get_byte(&cmd);
					pNavcPlt->tPltParams.cw_test_params.narrowband_peaks = get_byte(&cmd);
					pNavcPlt->tPltParams.cw_test_params.narrowband_adj_samples = get_byte(&cmd);
				break;
				
				case 5:
					pNavcPlt->tPltParams.req_type = get_long(&cmd);
					pNavcPlt->tPltParams.timeout = get_long(&cmd);
					pNavcPlt->tPltParams.svid = get_byte(&cmd);
					pNavcPlt->tPltParams.gpio_test_params.write_value = get_short(&cmd);
					pNavcPlt->tPltParams.gpio_test_params.write_mask = get_short(&cmd);
					pNavcPlt->tPltParams.gpio_test_params.status_mask = get_short(&cmd);
				break;
					
				case 6:
				case 7:
					pNavcPlt->tPltParams.req_type = get_long(&cmd);
					pNavcPlt->tPltParams.timeout = get_long(&cmd);
					pNavcPlt->tPltParams.svid = get_byte(&cmd);
					pNavcPlt->tPltParams.termination_event = get_byte(&cmd);
				break;

				case 8:
					pNavcPlt->tPltParams.req_type = get_long(&cmd);
					pNavcPlt->tPltParams.timeout = 5;
					break;
					
				default:
					status = -1; // un-defined parameter
				}
		}
		break;

		
		case ADS_MT_APP_COARSE_TIME_REQ:
		{
			pNavcCmd->tInjectAssist.uSessionId = (U16)(((get_byte(&cmd))&0xff) | ((TaskId&0xff)<<8)); 
			pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_TIME;
			/*----------------------------Time Info------------------*/
			pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.time_assist.gps_week = (McpU16)(get_short(&cmd)+1024);
			pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.time_assist.gps_msec = get_long(&cmd);
			get_long(&cmd); // sub_ms;
			pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.time_assist.time_unc = get_long(&cmd);
			get_byte(&cmd); // time_accuracy;
			pNavcCmd->tInjectAssist.uSystemTimeInMsec = mcpf_getCurrentTime_InMilliSec(hAds->hMcpf);
			pNavcCmd->tInjectAssist.bValidGsmTime = MCP_FALSE;

			uOpcode = NAVC_CMD_INJECT_ASSISTANCE;
			uLen = sizeof(pNavcCmd->tInjectAssist);
		}
		break;

		case ADS_MT_APP_COARSE_POS_REQ:
		{
			McpU32 temp;
			T_GPSC_position_assist *pAssist;
		   
			pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_POSITION;
			pNavcCmd->tInjectAssist.uSessionId = (U16)(((get_byte(&cmd))&0xff) | ((TaskId&0xff)<<8));
			
			if((pNavcCmd->tInjectAssist.uSessionId&0x00ff) < APP_ID_FILTER)
			{
				//   m_Diag("APP ID not correct...");
				return;
			}
		   
			/*----------------------------Position Info------------------*/
			/* Latitude */
			pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.position_assist;
			
			pAssist->latitude_sign= get_byte(&cmd);
			pAssist->latitude = (pAssist->latitude_sign)? (0 - get_short(&cmd)) : (get_short(&cmd));
			pAssist->latitude = (McpU32)( pAssist->latitude /(DBL)((DBL)90/(DBL)8388608) );
			
		   
			/* Longitude */
			temp= get_byte(&cmd);
			pAssist->longitude = (temp)? (0 - get_short(&cmd)) : (get_short(&cmd));
			pAssist->longitude = (McpS32)( pAssist->longitude /(DBL)((DBL)360/(DBL)16777216) );
					

			/* retrieve altitude */
			pAssist->altitude = get_short(&cmd);
			/* TODO: Once sign is sent from HST --> read it. */
			pAssist->altitude_sign = 1;
			
			/* Position Uncertainity uncertaintySemiMajor */
			pAssist->position_uncertainty = get_long(&cmd);
			//p_zInjectPosEst->q_Unc = p_zGpscPositionAssist->position_uncertainty;

			uOpcode = NAVC_CMD_INJECT_ASSISTANCE;
			uLen = sizeof(TNAVC_injectAssist);
		}
		break;

		/* added to send calibration control request */
       case ADS_MT_APP_CALIB_CTRL_REQ:
	   {
		   pNavcCmd->tInjectCalibCtrl.uCalibType = get_byte(&cmd);

		   uOpcode = NAVC_CMD_INJECT_CALIB_CTRL;
		   uLen = 1;
		}
		break;
	
		case ADS_MT_APP_GPSC_VERSION_REQ:
		{
			uOpcode = NAVC_CMD_GET_VERSION;
		}
		break;
	   
		   /* added for geo fence */
		case ADS_MT_APP_MOTION_MASK_REQ:
		{
		//   McpU32 i;

		  // pNavcCmd->tGeoFence.uSessionId = (U16)(((get_short(&cmd)) & 0x00ff) | ((TaskId & 0xff)<<8));; /* session id */
		  // pNavcCmd->tGeoFence.tGeoFenceCfg.geo_fence_control = get_byte(&cmd); /* geo-fence control*/
		  // pNavcCmd->tGeoFence.tGeoFenceCfg.speed_limit = get_short(&cmd); /* speed limit*/
		  // pNavcCmd->tGeoFence.tGeoFenceCfg.altitude_limit = get_short(&cmd); /* altitude limit */
		   
		   /* vertices number */
		  // pNavcCmd->tGeoFence.tGeoFenceCfg.vertices_number = get_byte(&cmd);
		   
		   /* vertices of polygon in x,y, z format */
		  // for(i=0;i<pNavcCmd->tGeoFence.tGeoFenceCfg.vertices_number;i++)
		  // {
		//	   pNavcCmd->tGeoFence.tGeoFenceCfg.geo_fence_vertices[i].geo_fence_latitude = get_long(&cmd);
		//	   pNavcCmd->tGeoFence.tGeoFenceCfg.geo_fence_vertices[i].geo_fence_longitude = get_long(&cmd);
		 //  }
		   
		   /* radius of circle */
		  // pNavcCmd->tGeoFence.tGeoFenceCfg.radius = get_short(&cmd);
		  // uOpcode = NAVC_CMD_SET_GEOFENCE;
		 //  uLen = sizeof(TNAVC_geoFence);
		}
		break; /* end of case ADS_MT_APP_MOTION_MASK_REQ */

		case ADS_MT_APP_AUTO_FLAG_REQ:
		{
			pNavcCmd->tDelAssist.uDelAssistBitmap = get_byte(&cmd);
			pNavcCmd->tDelAssist.uSvBitmap = 0;
			uOpcode = NAVC_CMD_DELETE_ASISTANCE;
		   	uLen = sizeof(TNAVC_delAssist);
		}
		break;
			
		default:
			status = -2; /* Unknown Opcode */
		break;
	}

	if(uLen == 0)
	{
		mcpf_mem_free_from_pool(hAds->hMcpf, pNavcCmd);
		pNavcCmd = NULL;
	}

	if(mcpf_SendMsg(hAds->hMcpf, TASK_NAV_ID, NAVC_QUE_CMD_ID, TaskId, 0, uOpcode, uLen, 0, pNavcCmd)
		!= RES_OK)							
	{
		/* mcpf_SendMsg(hAds->hMcpf, TaskId, 0, TaskId, 0, uOpcode, sizeof(LCE_FAIL), LCE_FAIL, 0 ); */
		return;
    }
}






















