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

/** \file   navc_outEvt.c 
 *  \brief  NAVC stack outgoing events from GPSC implementation. 
 * 			NAVC outgoing events are:
 * 			- response for performed command;
 * 			- confirmation for commnad completion;
 * 			- unsolicited GPSC indications and reports;
 * 
 *  \see    navc_cmdHandler.c, navc_services.c, navc_platform.c
 */

#include <math.h>

#include "gpsc_data.h"
#include "mcpf_mem.h"
#include "mcpf_services.h"
#include "mcpf_report.h"
#include "mcpf_msg.h"
#include "navc_defs.h"
#include "navc_api.h"
#include "navc_api_pvt.h"
#include "navc_init.h"
#include "navc_cmdHandler.h"
#include "navc_sm.h"
#include <utils/Log.h>
#include "suplc_cmdHandler.h"
#include "navc_sm.h"
#include "suplc_api.h"
#include "suplc_defs.h"

#include <utils/Log.h>
#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG "NAVD.NAVC_OUTEVT"    // our identification for logcat (adb logcat NAVD.NAVC_OUTEVT:V *:S)


/************************************************************************
 * Defines
 ************************************************************************/

/************************************************************************
 * Types
 ************************************************************************/

/************************************************************************
 * Internal functions prototypes
 ************************************************************************/


static void sendResponse (const TNAVCD_Ctrl 		*pNavc, 
						  McpU8 					*pBuf, 
						  McpU32 					uLen,
						  const ENAVC_evtOpcode 	eRespOpcode,
						  const ENAVC_cmdOpcode 	eComplCmd);

static McpBool refClkSrcSelection(const TNAVCD_Ctrl 	*pNavc, McpU32 *SrcTaskId, McpU32 *SrcQId);


void gpsc_app_fatal_err_ind (McpU32 uSesId,McpU32 errCode);

/************************************************************************
 *
 *   Module functions implementation
 *
 ************************************************************************/



/** 
 * \fn     gpsc_cmd_complete_cnf 
 * \brief  generic command complete confirmation
 * 
 */
void gpsc_cmd_complete_cnf(T_GPSC_result result)
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl	*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 

	NavcSm(pNavc->hNavcSm, E_NAVC_EV_COMPLETE);
}


void gpsc_cmd_success_cnf(T_GPSC_result result)
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl	*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 

	NavcCmdCompleteHandler (pNavc, result);
	
}


/** 
 * \fn     gpsc_app_loc_fix_ind 
 * \brief  Send location fix response to requested application
 * 
 */
void gpsc_app_loc_fix_ind (T_GPSC_loc_fix_response* loc_fix_response)
{
	EmcpTaskId 		eDestTaskId = 0;
	McpU8		 	   uDestQId = 0;
	gpsc_ctrl_type	*p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl		*pNavc = (TNAVCD_Ctrl*)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 

	TNAVC_evt 		*pEvt = 0;

	McpU32 			uSesId = 0;
	McpU32			uLen=0;
	McpU16			uOpcode=0;
	EMcpfRes  		eRes = 0;

	pEvt = (TNAVC_evt*) mcpf_mem_alloc_from_pool (pNavc->hMcpf, pNavc->hEvtPool);
	if (pEvt)
	{
		uSesId = loc_fix_response->loc_fix_session_id;

		eDestTaskId = NAVCD_SESSION_TASK_ID_GET(uSesId);
		uDestQId    = (McpU8) NAVCD_SESSION_QUE_ID_GET(uSesId);

		loc_fix_response->loc_fix_session_id = NAVCD_SESSION_REF_NUM_GET(uSesId);

		pEvt->eResult = RES_OK;
		uOpcode = NAVC_EVT_POSITION_FIX_REPORT;

		mcpf_mem_copy (pNavc->hMcpf, 
					   &pEvt->tParams.tLocFixReport,  
					   loc_fix_response, 
					   sizeof(pEvt->tParams.tLocFixReport));

		uLen = sizeof (TNAVC_locFixReport) + NAVC_EVT_HEADER_SIZE;
		
		eRes = mcpf_SendMsg (pNavc->hMcpf, 
							 eDestTaskId,  
							 uDestQId, 
							 TASK_NAV_ID,
							 NAVC_QUE_CMD_ID,
							 uOpcode,
							 uLen,
							 0, 				/* user defined */
							 pEvt);
		
		if (eRes != RES_OK)
		{
			 mcpf_mem_free_from_pool (pNavc->hMcpf, pEvt);
          MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_loc_fix_ind: mcpf_SendMsg failed\n"));
		}
	}
	else
	{
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_loc_fix_ind: mcpf_mem_alloc_from_pool failed\n"));
	}

}

/** 
 * \fn     gpsc_app_assist_request_ind 
 * \brief  Send assistance request event
 * 
 */
T_GPSC_result gpsc_app_assist_request_ind (U16 assist_bitmap_mandatory, 
												U16	 assist_bitmap_optional, 
												T_GPSC_nav_model_req_params *nav_model_req_params,
												my_Position *pos_info,
												T_GPSC_supl_qop *gps_qop, 
												T_GPSC_current_gps_time *tGpsTime,
												U32 SrcTaskId,
												U32 SrcQueueId)
{
	gpsc_ctrl_type	*p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl		*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 
	TNAVC_evt 		*pEvt = 0;
	T_GPSC_result	res = GPSC_FAIL;
	EMcpfRes  		eRes = 0;
	McpU16			uLen, uOpcode=0;
	
	MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG,
		("gpsc_app_assist_request_ind: SrcTaskId %d, SrcQueueId = %d", \
		  SrcTaskId, SrcQueueId));

	pEvt = (TNAVC_evt *) mcpf_mem_alloc_from_pool (pNavc->hMcpf, pNavc->hEvtPool);
	if (pEvt)
	{
		uOpcode = NAVC_EVT_ASSISTANCE_REQUEST;

		pEvt->eResult = RES_OK;
		pEvt->tParams.tAssistReq.uAssistBitmapMandatory = assist_bitmap_mandatory;
		pEvt->tParams.tAssistReq.uAssistBitmapOptional  = assist_bitmap_optional;
		memcpy((void *)&pEvt->tParams.tAssistReq.position, (void *)pos_info, sizeof(my_Position));
		memcpy((void *)&pEvt->tParams.tAssistReq.qop, (void *)gps_qop, sizeof(T_GPSC_supl_qop));
		memcpy((void *)&pEvt->tParams.tAssistReq.tGpsTime, (void *)tGpsTime, sizeof(T_GPSC_current_gps_time));

		if (nav_model_req_params)
		{
			mcpf_mem_copy (pNavc->hMcpf, 
						   &pEvt->tParams.tAssistReq.tNavModelReqParams,  
						   nav_model_req_params, 
						   sizeof(pEvt->tParams.tAssistReq.tNavModelReqParams));
		}
		uLen = sizeof (TNAVC_assistReq) + NAVC_EVT_HEADER_SIZE;

		if(SrcTaskId == TASK_SUPL_ID)
		{
			MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG,
										("gpsc_app_assist_request_ind: Request SUPL"));

			if(p_zGPSCControl->p_zSuplStatus.u_GetAssistSUPLRequest == FALSE)
			{
				p_zGPSCControl->p_zSuplStatus.u_GetAssistSUPLRequest = TRUE;
				p_zGPSCControl->p_zSuplStatus.u_SessRequestCount++;
			}
			else
			{
				MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG,
					("gpsc_app_assist_request_ind: SUPL session in progress"));
                                mcpf_mem_free_from_pool(pNavc->hMcpf, pEvt);
				p_zGPSCControl->p_zSuplStatus.u_GetAssistSUPLRequestPending = TRUE;
				res = GPSC_FAIL;
				return res;

			}	
		}

		eRes = mcpf_SendMsg (pNavc->hMcpf, 
							 SrcTaskId,                  //pNavc->eSrcTaskId, 
							 SrcQueueId,		// pNavc->uSrcQId, 
							 TASK_NAV_ID,
							 NAVC_QUE_CMD_ID,
							 uOpcode,
							 uLen,
							 0, 				/* user defined */
							 pEvt);

		if (eRes == RES_OK)
		{
			res = GPSC_SUCCESS;
		}
		else
		{
			mcpf_mem_free_from_pool (pNavc->hMcpf, pEvt);
            MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_assist_request_ind: mcpf_SendMsg failed\n"));
			res = GPSC_FAIL;
			return res;
		}
	}
	else
	{
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, \
			("gpsc_app_assist_request_ind: mcpf_mem_alloc_from_pool failed\n"));
		res = GPSC_FAIL;
		return res;
	}

	/* Indicate successful assistance data request */
	return res;
}


T_GPSC_result gpsc_app_stop_request_ind ()
{
	gpsc_ctrl_type	*p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl		*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 
	TNAVC_evt 		*pEvt;
//	TNAVC_evt 		*pEvtSaGps;
	T_GPSC_result	res = GPSC_FAIL;
	EMcpfRes  		eRes;
	McpU16			uLen, uOpcode=0;
	McpU32         SrcTaskId = 0;
	McpU32         SrcQueueId = 0;
	McpU8		      assist_indx = 0;

    for(assist_indx = 0; assist_indx < MAX_ASSIST_PROVIDER; assist_indx++)
	{
	    if((pNavc->tAssistSrcDb[assist_indx].uAssistSrcPriority != 0) && \
			(pNavc->tAssistSrcDb[assist_indx].uAssistSrcType !=0xFF))
		{	
			MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG,
				("gpsc_app_stop_request_ind: Assistance provider %d, Priority = %d, Type = %d", \
				  assist_indx,  pNavc->tAssistSrcDb[assist_indx].uAssistSrcPriority, pNavc->tAssistSrcDb[assist_indx].uAssistSrcType));

	pEvt = (TNAVC_evt *) mcpf_mem_alloc_from_pool (pNavc->hMcpf, pNavc->hEvtPool);
	if (pEvt)
	{
		uOpcode = NAVC_EVT_STOP_REQUEST;
				
		uLen = sizeof (TNAVC_stopReq) + NAVC_EVT_HEADER_SIZE;
		SrcTaskId = pNavc->tAssistSrcDb[assist_indx].uAssistProviderTaskId;
		SrcQueueId = pNavc->tAssistSrcDb[assist_indx].uAssistProviderQueueId;

		eRes = mcpf_SendMsg (pNavc->hMcpf, 
							 SrcTaskId,                  //pNavc->eSrcTaskId, 
							 SrcQueueId,		// pNavc->uSrcQId, 
							 TASK_NAV_ID,
							 NAVC_QUE_CMD_ID,
							 uOpcode,
							 uLen,
							 0, 				/* user defined */
							 pEvt);

		if (eRes == RES_OK)
		{
			res = GPSC_SUCCESS;
		}
		else
		{
			mcpf_mem_free_from_pool (pNavc->hMcpf, pEvt);
            MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_stop_request_ind: mcpf_SendMsg failed\n"));
			res = GPSC_FAIL;
			return res;
		}

			}
			else
			{
				MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_stop_request_ind: mcpf_mem_alloc_from_pool failed\n"));
				res = GPSC_FAIL;
				return res;
			}
		}
	}
	/* Indicate successful assistance data request */
	return res;
}







/** 
 * \fn     gpsc_app_prodlinetest_ind 
 * \brief  
 * 
 */
void gpsc_app_prodlinetest_ind (T_GPSC_prodtest_response *prodtest_response)
{
	gpsc_ctrl_type 	*p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl		*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 
	McpU32			uLen = sizeof(T_GPSC_ctrl_prodtest_response_union);
	T_GPSC_ctrl_prodtest_response_union eRespUnion = prodtest_response->ctrl_prodtest_response_union;
	

	switch(eRespUnion)
	{
	case GPSC_RESULT_RTC_OSCTEST:
		uLen += sizeof(T_GPSC_rtc_osctest);
		break;

	case GPSC_RESULT_GPS_OSCTEST:
		uLen += sizeof(T_GPSC_gps_osctest);
		break;

	case GPSC_RESULT_SIGACQ_TEST:
		uLen += sizeof(T_GPSC_sigacq_test);
		break;

	case GPSC_RESULT_CW_TEST:
		uLen += sizeof(T_GPSC_cw_test);
		break;

	case GPSC_RESULT_GPIO_TEST:
		uLen += sizeof(T_GPSC_gpio_test);
		break;

	case GPSC_RESULT_SYNC_TEST:
		uLen += sizeof(T_GPSC_sync_test);
		break;

	case GPSC_RESULT_SELF_TEST:
		uLen += sizeof(T_GPSC_self_test);
		break;

	case GPSC_RESULT_RAM_CHECKSUM_TEST:
		uLen += sizeof(T_GPSC_ram_checksum_test);
        break;
	default:
        MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_prodlinetest_ind: bad ctrl_prodtest_response_union value\n"));
		return;
	}

	sendResponse (pNavc, 
				  (McpU8 *) prodtest_response, 
				  uLen,  
				  NAVC_EVT_PLT_RESPONSE,
				  NAVC_CMD_PLT);
}

void RECON_gpsc_app_assist_begin_req_ind ()
{
   gpsc_ctrl_type*   p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl*      pNavc = (TNAVCD_Ctrl*)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 
    
	sendResponse (pNavc, 
				  NULL, 
				  0,  
				  NAVC_EVT_CMD_COMPLETE,
				  NAVC_CMD_BEGIN_ASSISTANCE);
}

/** 
 * \fn     gpsc_app_blf_status_ind
 * \brief  Indicate BLF status report
 * 
 */
T_GPSC_result gpsc_app_blf_status_ind (TNAVC_BlfStatusReport *p_blfStatusReport)
{
    gpsc_ctrl_type*     p_zGPSCControl = gp_zGPSCControl;
    TNAVCD_Ctrl *pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl;

    MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG,
                                                        ("gpsc_app_blf_status_ind"));
    sendResponse (pNavc,
                                  (McpU8 *) p_blfStatusReport,
                                  sizeof (TNAVC_BlfStatusReport),
                                  NAVC_EVT_BLF_REP_STATUS,
                                  NAVC_CMD_BLF_STATUS);

    return GPSC_SUCCESS;
}
/** 
 * \fn     gpsc_app_gps_ver_ind 
 * \brief  Indicate hardware and software version
 * 
 */
T_GPSC_result gpsc_app_gps_ver_ind (T_GPSC_gps_ver_resp_params *gps_ver_resp_params)
{
    gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl	*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 

	MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, 
							("gpsc_app_gps_ver_ind"));

	sendResponse (pNavc, 
				  (McpU8 *) gps_ver_resp_params, 
				  sizeof (T_GPSC_gps_ver_resp_params),  
				  NAVC_EVT_VERSION_RESPONSE,
				  NAVC_CMD_GET_VERSION);

	return GPSC_SUCCESS;
}

/** 
 * \fn     gpsc_app_alm_assist_complete_ind 
 * \brief  Indicate all almanac was received
 * 
 */
T_GPSC_result gpsc_app_alm_assist_complete_ind (void)
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl	*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 

	MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, 
							("GPSC assistance is complete, all almanac was received\n"));
  return GPSC_SUCCESS;
}

/** 
 * \fn     gpsc_app_assist_time_ind 
 * \brief  asnchronous response to the NAVC_CMD_GET_ASSIST_TIME 
 * 
 */
T_GPSC_result gpsc_app_assist_time_ind (void)
{
    gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl	*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 
	T_GPSC_assistance_database_report tAssistance_database_report;

	MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, 
							("GPS Assist Time Ind:"));
    tAssistance_database_report.ctrl_assistance_inject_union_report = GPSC_ASSIST_TIME_REP;

	tAssistance_database_report.assistance_inject_union_report.time_assist.gps_week = (U16)p_zGPSCControl->p_zGPSCDatabase->z_DBGpsClock.w_GpsWeek;
	tAssistance_database_report.assistance_inject_union_report.time_assist.gps_msec = (U32)p_zGPSCControl->p_zGPSCDatabase->z_DBGpsClock.q_GpsMs;
	tAssistance_database_report.assistance_inject_union_report.time_assist.time_unc = (U32)ceil(p_zGPSCControl->p_zGPSCDatabase->z_DBGpsClock.f_TimeUnc);

	sendResponse (pNavc, 
				  (McpU8 *) &tAssistance_database_report, 
				  sizeof(T_GPSC_assistance_inject_union_report),  
				  NAVC_EVT_ASSIST_REP_TIME,
				  NAVC_CMD_GET_ASSIST_TIME);

	return GPSC_SUCCESS;
}

/** 
 * \fn     gpsc_app_assist_eph_ind 
 * \brief  Send Ephemeris data to requested application ex:Sagps
 * 
 */
void gpsc_app_assist_eph_ind(T_GPSC_ephemeris_assist  *ephemeris_assist, EmcpTaskId TaskID, McpU8* QueueID)
{
	
	gpsc_ctrl_type	*p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl		*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 
	TNAVC_evt 		*pEvt;
	McpU32			uLen=0;
	McpU16			uOpcode=0;
	EMcpfRes  		eRes;

	pEvt = (TNAVC_evt *) mcpf_mem_alloc_from_pool (pNavc->hMcpf, pNavc->hEvtPool);
	if (pEvt)
	{
		pEvt->eResult = RES_OK;

		uOpcode = NAVC_EVT_ASSIST_REP_EPH;

		mcpf_mem_copy (pNavc->hMcpf, 
					   &pEvt->tParams.tAssistSingleEphemeris,  
					   ephemeris_assist, 
					   sizeof(pEvt->tParams.tAssistSingleEphemeris));
		uLen = sizeof (TNAVC_assistEphemeris) + NAVC_EVT_HEADER_SIZE;

		eRes = mcpf_SendMsg (pNavc->hMcpf, 
							 TaskID,  /* Src Task Id*/
							 QueueID,  /* Src Qid */
							 TASK_NAV_ID,
							 NAVC_QUE_CMD_ID,
							 uOpcode,
							 uLen,
							 0, 				/* user defined */
							 pEvt);

		if (eRes != RES_OK)
		{
			mcpf_mem_free_from_pool (pNavc->hMcpf, pEvt);
            MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_assist_eph_ind: mcpf_SendMsg failed\n"));
		}
	}
	else
	{
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_assist_eph_ind: mcpf_mem_alloc_from_pool failed\n"));
	}
}


/** 
 * \fn     gpsc_app_assist_gpstime_ind 
 * \brief  Send GPS time data to requested application ex:Sagps
 * 
 */
void gpsc_app_assist_gpstime_ind(T_GPSC_time_assist  *gpstime_assist, EmcpTaskId TaskID, McpU8* QueueID)
{
	
	gpsc_ctrl_type		*p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl		*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 
	TNAVC_evt 		*pEvt;
	McpU32			uLen=0;
	McpU16			uOpcode=0;
	EMcpfRes  		eRes;

	pEvt = (TNAVC_evt *) mcpf_mem_alloc_from_pool (pNavc->hMcpf, pNavc->hEvtPool);
	if (pEvt)
	{
		pEvt->eResult = RES_OK;

		uOpcode = NAVC_EVT_ASSIST_REP_TIME;

		mcpf_mem_copy (pNavc->hMcpf, 
					   	&pEvt->tParams.tAssistTime,  
					   	gpstime_assist, 
					   	sizeof(pEvt->tParams.tAssistTime));

		uLen = sizeof (T_GPSC_time_assist) + NAVC_EVT_HEADER_SIZE;

		eRes = mcpf_SendMsg (pNavc->hMcpf, 
							 TaskID,  /* Src Task Id*/
							 QueueID,  /* Src Qid */
							 TASK_NAV_ID,
							 NAVC_QUE_CMD_ID,
							 uOpcode,
							 uLen,
							 0, 				/* user defined */
							 pEvt);

		if (eRes != RES_OK)
		{
			mcpf_mem_free_from_pool (pNavc->hMcpf, pEvt);
            		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_assist_gpstime_ind: mcpf_SendMsg failed\n"));
		}
		
	}
	else
	{
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_assist_gpstime_ind: mcpf_mem_alloc_from_pool failed\n"));
	}
}

/** 
 * \fn     gpsc_app_gps_sleep_res 
 * \brief  asnchronous response to the NAVC_CMD_GPS_SLEEP 
 * 
 */
T_GPSC_result gpsc_app_gps_sleep_res(void)
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl		*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 

	MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("GPS Sleep Res:"));

	if (p_zGPSCControl->p_zCustomStruct->custom_sleep_req_flag == TRUE)
		{
			p_zGPSCControl->p_zCustomStruct->custom_sleep_req_flag= FALSE;
			sendResponse(pNavc, NULL, 0, NAVC_EVT_ASSIST_REP_SLEEP, NAVC_CMD_GPS_SLEEP);
		}

	return GPSC_SUCCESS;
}

/** 
 * \fn     gpsc_app_gps_wakeup_res 
 * \brief  asnchronous response to the NAVC_CMD_GPS_WAKEUP 
 * 
 */
T_GPSC_result gpsc_app_gps_wakeup_res(void)
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl		*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 

	MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("GPS Wakeup Res:"));

	if (p_zGPSCControl->p_zCustomStruct->custom_wakeup_req_flag == TRUE)
		{
			p_zGPSCControl->p_zCustomStruct->custom_wakeup_req_flag= FALSE;
			sendResponse(pNavc, NULL, 0, NAVC_EVT_ASSIST_REP_WAKEUP, NAVC_CMD_GPS_WAKEUP);
		}

	return GPSC_SUCCESS;
}

/** 
 * \fn     gpsc_app_assist_delete_ind 
 * \brief  synchronous response to the NAVC_CMD_DELETE_ASISTANCE 
 * 
 */
T_GPSC_result gpsc_app_assist_delete_ind(void)
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl		*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 

	MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("GPS Assist Delete Ind:"));

	sendResponse(pNavc, NULL, 0, NAVC_EVT_ASSIST_DEL_RES, NAVC_CMD_DELETE_ASISTANCE);

	return GPSC_SUCCESS;
}

/** 
 * \fn     gpsc_app_agc_ind
 * \brief  asynchronous response to the NAVC_CMD_GET_AGC 
 * 
 */
void  gpsc_app_agc_ind(McpS32 vgaInput, McpS32 lnaInput)
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl		*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 
	T_GPSC_agc_params	tAgc;
	
	MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("GPS AGC Ind:"));

	tAgc.vgaInput = vgaInput;
	tAgc.lnaInput = lnaInput;
	
	sendResponse(pNavc, (McpU8*)&tAgc, sizeof(T_GPSC_agc_params), NAVC_EVT_AGC_RESP, NAVC_CMD_GET_AGC);
}


/** 
 * \fn     gpsc_app_inj_tcxo_ind 
 * \brief  synchronous response to the NAVC_CMD_INJ_TCXO 
 * 
 */
T_GPSC_result gpsc_app_inj_tcxo_ind(void)
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl		*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 

	MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("GPS Inject TCXO Ind:"));

	sendResponse(pNavc, NULL, 0, NAVC_EVT_INJECT_FREQ_EST_RES, NAVC_CMD_INJ_TCXO);

	return GPSC_SUCCESS;
}

/** 
 * \fn     gpsc_app_inj_tcxo_ind 
 * \brief  synchronous response to the NAVC_CMD_INJ_TCXO 
 * 
 */
T_GPSC_result gpsc_app_stop_fix_ind(void)
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl		*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 

	MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_stop_fix_ind: send stop confirmation"));

	sendResponse(pNavc, NULL, 0, NAVC_EVT_CMD_COMPLETE, NAVC_CMD_STOP_FIX);

	MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_stop_fix_ind: Enabling CMD-Q for NAVC_CMD_STOP_FIX"));
	NavcCmdEnableQueue (p_zGPSCControl->p_zSysHandlers->hNavcCtrl);
	return GPSC_SUCCESS;
}

/** 
 * \fn     gpsc_app_refclk_req
 * \brief  request for refclock
 * 
 */
T_GPSC_result gpsc_app_refclk_req(T_GPSC_refclk_req refclkreq)
{
	McpU32         SrcTaskId = 0;
	McpU32         SrcQueueId = 0;
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl		*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 
	TNAVC_evt 	*pEvt;
	EMcpfRes  	eRes;

	MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("Request Ref Clock for Calib"));

	if (TRUE == refClkSrcSelection(pNavc,&SrcTaskId, &SrcQueueId))
	{

		pEvt = (TNAVC_evt *) mcpf_mem_alloc_from_pool (pNavc->hMcpf, pNavc->hEvtPool);
		if (pEvt)
		{
			pEvt->eResult = RES_OK;
			pEvt->eComplCmd = 0;

			mcpf_mem_copy (pNavc->hMcpf, &pEvt->tParams, (McpU8*) &refclkreq, sizeof(T_GPSC_refclk_req));
			
			eRes = mcpf_SendMsg (pNavc->hMcpf, 
								 SrcTaskId,  
								 SrcQueueId,  
								 TASK_NAV_ID,
								 NAVC_QUE_CMD_ID,
								 (McpU16) NAVC_EVT_REFCLK_REQ,
								 sizeof(T_GPSC_refclk_req)+NAVC_EVT_HEADER_SIZE,
								 0, 				/* user defined */
								 pEvt);

			if (eRes != RES_OK)
			{
				mcpf_mem_free_from_pool (pNavc->hMcpf, pEvt);
				MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_refclk_req: mcpf_SendMsg failed\n"));
			}
		}
		else
		{
			MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_refclk_req: mcpf_mem_alloc_from_pool failed\n"));
		}
	
	}


	return GPSC_SUCCESS;
}



/** 
 * \fn     gpsc_app_calib_control_ind 
 * \brief  Indicate calibration control
 * 
 */
void gpsc_app_calib_control_ind (T_GPSC_calib_response* calib_response)
{
	T_GPSC_refclk_req refclkreq;
	MCPF_UNUSED_PARAMETER(calib_response);
	refclkreq.refclk_req = GPSC_REFCLK_DISABLE;
	gpsc_app_refclk_req(refclkreq);
}


/*
 ******************************************************************************
 * gpsc_app_fwd_rx_ind
 *
 * Function description:
 *
 * This function is used to pass the received data from GPSC to NAVC client
 *
 * Parameters:
 *  p_uDataBuffer -> Pointer to data buffer
 *  w_NumBytes   -> Number of bytes 
 *  NOTE: Data must be copied before returning
 *
 * Return value:
 *  T_GPSC_result : Result
 ******************************************************************************
*/
void gpsc_app_fwd_rx_ind( U8 *p_uDataBuffer, U16 w_NumBytes)
{

 gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
 TNAVCD_Ctrl	*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 

TNAVC_PROP_PROTO_MSG tAi2RawMsg;
 int i;

 // copy incoming AI2 data
 tAi2RawMsg.numBytes = w_NumBytes;
 for(i =0; (i<w_NumBytes && i<C_DL_RX_BUF_LEN); i++)
   tAi2RawMsg.ai2Msg[i] = *p_uDataBuffer++;

 MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, 
 							("GPS AI2 MSG RX Ind:"));
 sendResponse (pNavc, 
 	(McpU8 *) &tAi2RawMsg,
	w_NumBytes + 7,  // to take care of headers
	NAVC_EVT_FWD_RX_DATA,
	NAVC_CMD_FWD_TX_DATA);
      
 //return GPSC_SUCCESS;

}

/************************************************************************
 *
 *   Private functions implementation
 *
 ************************************************************************/


/** 
 * \fn     sendResponse 
 * \brief  Send response message
 * 
 * Sends command complete event to specified destination task
 * 
 * \note
 * \param	pNavc - pointer to NAVC control object
 * \param  	pBuf  - pointer to buffer with response
 * \param  	uLen  - buffer length
 * \param  	eRespOpcode - MCPF message response opcode
 * \param  	eComplCmd   - MCPF command which response is sent for
 * \return 	void
 * \sa     	
 */ 
static void sendResponse (const TNAVCD_Ctrl 		*pNavc, 
						  McpU8 					*pBuf, 
						  McpU32 					uLen,
						  const ENAVC_evtOpcode 	eRespOpcode,
						  const ENAVC_cmdOpcode 	eComplCmd)
{
	EMcpfRes  	eRes = RES_OK;
	TNAVC_evt* pEvt = (TNAVC_evt*) mcpf_mem_alloc_from_pool (pNavc->hMcpf, pNavc->hEvtPool);
	if (pEvt)
	{
		pEvt->eResult = RES_OK;
		pEvt->eComplCmd = eComplCmd;

		mcpf_mem_copy (pNavc->hMcpf, &pEvt->tParams, pBuf, uLen);
		uLen += NAVC_EVT_HEADER_SIZE;

		eRes = mcpf_SendMsg (pNavc->hMcpf, 
							 pNavc->eSrcTaskId,  
							 pNavc->uSrcQId,  
							 TASK_NAV_ID,
							 NAVC_QUE_CMD_ID,
							 (McpU16) eRespOpcode,
							 uLen,
							 0, 				/* user defined */
							 pEvt);

		if (eRes != RES_OK)
		{
			mcpf_mem_free_from_pool (pNavc->hMcpf, pEvt);
            MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("sendResponse: mcpf_SendMsg failed\n"));
		}
	}
	else
	{
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("sendResponse: mcpf_mem_alloc_from_pool failed\n"));
	}
}


/** 
 * \fn     gpsc_app_fatal_err_ind 
 * \brief  Send fatal err indication to requested application
 * 
 */
static McpBool refClkSrcSelection(const TNAVCD_Ctrl 	*pNavc, McpU32 *SrcTaskId, McpU32 *SrcQId)
{
   McpU8 index = 0;

   /* check the request task id is matches with any of the assistance src client which is registered*/
   for (index = 0; index< MAX_ASSIST_PROVIDER; index++)
   {
	   if (pNavc->tAssistSrcDb[index].uAssistProviderTaskId && pNavc->tAssistSrcDb[index].uAssistSrcType==REFCLK_PROVIDER)
	   {       
	       MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("Requested RefClk source to %d and priority %d  and TaskID  %d\n",pNavc->tAssistSrcDb[index].uAssistSrcType,
		                     pNavc->tAssistSrcDb[index].uAssistSrcPriority,pNavc->tAssistSrcDb[index].uAssistProviderTaskId));

           *SrcTaskId = pNavc->tAssistSrcDb[index].uAssistProviderTaskId;
           *SrcQId = pNavc->tAssistSrcDb[index].uAssistProviderQueueId;
     	    return TRUE;
	   }

   }

    MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("NO Ref CLK Src Found, default request to Task: %d", pNavc->eSrcTaskId));
    return FALSE;
 }

void gpsc_app_fatal_err_ind (McpU32 uSesId,McpU32 errCode)
{
	EmcpTaskId 		eDestTaskId = 0;
	McpU8		 	   uDestQId    = 0; 
	gpsc_ctrl_type	*p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl		*pNavc  = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 
	TNAVC_evt 		*pEvt   = 0;
	McpU32			uLen    = 0;
	McpU16			uOpcode = 0;
	EMcpfRes  		eRes    = 0;

	pEvt = (TNAVC_evt *) mcpf_mem_alloc_from_pool (pNavc->hMcpf, pNavc->hEvtPool);
	if (pEvt)
	{
		//uSesId = loc_fix_session_id;

		eDestTaskId = NAVCD_SESSION_TASK_ID_GET(uSesId);
		uDestQId    = (McpU8) NAVCD_SESSION_QUE_ID_GET(uSesId);

		//loc_fix_response->loc_fix_session_id = NAVCD_SESSION_REF_NUM_GET(uSesId);

		pEvt->eResult = RES_ERROR;

		uOpcode = NAVC_EVT_FATAL_ERR;
#if 0
		mcpf_mem_copy (pNavc->hMcpf, 
					   //&pEvt->tParams.tLocFixReport, 
                       &pEvt->tParams.tErrCode,
					   //loc_fix_response, 
                       errCode,
					   //sizeof(pEvt->tParams.tErrCode));
                       sizeof(McpU32));
#endif 
        pEvt->tParams.tErrCode = errCode;
		uLen = sizeof (McpU32) + NAVC_EVT_HEADER_SIZE;

		eRes = mcpf_SendMsg (pNavc->hMcpf, 
							 eDestTaskId,  
							 uDestQId, 
							 TASK_NAV_ID,
							 NAVC_QUE_CMD_ID,
							 uOpcode,
							 uLen,
							 0, 				/* user defined */
							 pEvt);

		if (eRes != RES_OK)
		{
			mcpf_mem_free_from_pool (pNavc->hMcpf, pEvt);
            MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_loc_fix_ind: mcpf_SendMsg failed\n"));
		}
			 
	}
	else
	{
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_loc_fix_ind: mcpf_mem_alloc_from_pool failed\n"));
	}
	
	/* TODO support ULTS/ADS RRC/RRPL and ToolKit */
}

void gpsc_app_motion_mask_ind (TNAVC_MotionMask_Status* p_motionmaskstatus)
{

	EmcpTaskId 		eDestTaskId = 0;
	McpU8		 	   uDestQId = 0;
	gpsc_ctrl_type	*p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl		*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 
	TNAVC_evt 		*pEvt = 0;
	McpU32 			uSesId = 0;
	McpU32			uLen=0;
	McpU16			uOpcode=0;
	EMcpfRes  		eRes = 0;


	pEvt = (TNAVC_evt *) mcpf_mem_alloc_from_pool (pNavc->hMcpf, pNavc->hEvtPool);


	if (pEvt)
	{	

            	MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_motion_mask_ind:\n"));
		uSesId = p_motionmaskstatus->geofence_session_id;

		eDestTaskId = NAVCD_SESSION_TASK_ID_GET(uSesId);
		uDestQId    = (McpU8) NAVCD_SESSION_QUE_ID_GET(uSesId);

		p_motionmaskstatus->geofence_session_id = NAVCD_SESSION_REF_NUM_GET(uSesId);
		//p_motionmaskstatus->status = p_Geofencestatus->status;
		//p_motionmaskstatus->FCount = p_Geofencestatus->FCount;

		pEvt->eResult = RES_OK;

		uOpcode = NAVC_EVT_MOTION_MASK_STATUS;

		mcpf_mem_copy (pNavc->hMcpf, 
					   &pEvt->tParams.tMotionMaskStatus,  
					   p_motionmaskstatus, 
					   sizeof(pEvt->tParams.tMotionMaskStatus));
		uLen = sizeof (TNAVC_MotionMask_Status) + NAVC_EVT_HEADER_SIZE;

		eRes = mcpf_SendMsg (pNavc->hMcpf, 
							 eDestTaskId,  
							 uDestQId, 
							 TASK_NAV_ID,
							 NAVC_QUE_CMD_ID,
							 uOpcode,
							 uLen,
							 0, 				/* user defined */
							 pEvt);

		if (eRes != RES_OK)
		{
			mcpf_mem_free_from_pool (pNavc->hMcpf, pEvt);
            MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_motion_mask_ind: mcpf_SendMsg failed\n"));
		}

	
		/* for reporting raw position or Raw measure ment to sagps client */
		 
	}

	else
	{
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_motion_mask_ind: mcpf_mem_alloc_from_pool failed\n"));
	}

}

void gpsc_app_motion_mask_settings_ind (TNAVC_GetMotionMask* p_motionmasksetting)
{
	gpsc_ctrl_type	*p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl		*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 
	McpU16			uLen, uOpcode=0;

		uOpcode = NAVC_EVT_MOTION_MASK_SETTINGS;

		uLen = sizeof (TNAVC_GetMotionMask);

		sendResponse (pNavc, 
				  (McpU8 *)p_motionmasksetting, 
				  uLen,  
				  NAVC_EVT_MOTION_MASK_SETTINGS,
				  NAVC_CMD_GET_MOTION_MASK);
}
void gpsc_app_priority_override_ind()
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl*      pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 

	MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("Priority override so stopping or igorning the location request"));

	sendResponse(pNavc,NULL, 0, NAVC_EVT_PRIORITY_OVERRIDE_INDICATION, 0);
}


#ifdef WIFI_DEMO
 /** 
  * \fn 	gpsc_app_gps_wifi_blend_pos_ind 
  * \brief	Send wifi,gps and blended location fix response to toolkit
  * 
  */

 void gpsc_app_gps_wifi_blend_pos_ind(TNAVC_gpsWifiBlendReport *p_zGpsWifiBlendReport)
{
    EmcpTaskId 		eDestTaskId;
	McpU8		 	uDestQId;
	gpsc_ctrl_type	*p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl		*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 
    gpsc_db_type*           p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
    gpsc_db_gps_meas_type*  p_zDBGpsMeas = &p_zGPSCDatabase->z_DBGpsMeas; 
	TNAVC_evt 		*pEvt;
	TNAVC_evt 		*pEvtSaGps;
	//TNAVC_evt 		*pEvtSuplPos;
	McpU32 			uSesId;
	McpU32			uLen=0;
	McpU16			uOpcode=0;
	EMcpfRes  		eRes;

	pEvt = (TNAVC_evt *) mcpf_mem_alloc_from_pool (pNavc->hMcpf, pNavc->hEvtPool);
	if (pEvt)
	{
		uSesId = p_zGpsWifiBlendReport->session_id;

		eDestTaskId = NAVCD_SESSION_TASK_ID_GET(uSesId);
		uDestQId    = (McpU8) NAVCD_SESSION_QUE_ID_GET(uSesId);

		p_zGpsWifiBlendReport->session_id = NAVCD_SESSION_REF_NUM_GET(uSesId);

		pEvt->eResult = RES_OK;

		//uOpcode = NAVC_EVT_GPS_WIFI_BLEND_REPORT;
		uOpcode = 34; /* this is hardcoded because corresponding enum is always changing */ 

		mcpf_mem_copy (pNavc->hMcpf, 
					   &pEvt->tParams.tGpsWifiBlendReport,  
					   p_zGpsWifiBlendReport, 
					   sizeof(pEvt->tParams.tGpsWifiBlendReport));
		uLen = sizeof (TNAVC_gpsWifiBlendReport) + NAVC_EVT_HEADER_SIZE;
		
		eRes = mcpf_SendMsg (pNavc->hMcpf, 
							 eDestTaskId,  
							 uDestQId, 
							 TASK_NAV_ID,
							 NAVC_QUE_CMD_ID,
							 uOpcode,
							 uLen,
							 0, 				/* user defined */
							 pEvt);
#if 1

		McpU16 ii = 0;
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("sizeof tGpsWifiBlendReport %d", uLen));
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG,(" cmdOpcode %d", pEvt->eComplCmd));
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG,(" eResult %d", pEvt->eResult));
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG,("session_id  %d", pEvt->tParams.tGpsWifiBlendReport.session_id));
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG,("gps_latitude  %d", pEvt->tParams.tGpsWifiBlendReport.gps_latitude));
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG,(" gps_longitude %d", pEvt->tParams.tGpsWifiBlendReport.gps_longitude));
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG,(" gps_altitude %d", pEvt->tParams.tGpsWifiBlendReport.gps_altitude));
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG,(" gps_timeofpos %d", pEvt->tParams.tGpsWifiBlendReport.gps_timeofpos));
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG,(" gps_fix_valid %d", pEvt->tParams.tGpsWifiBlendReport.gps_fix_valid));
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG,(" wifi_latitude %d", pEvt->tParams.tGpsWifiBlendReport.wifi_latitude));
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG,(" wifi_longitude %d", pEvt->tParams.tGpsWifiBlendReport.wifi_longitude));
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG,(" wifi_timeofpos %d", pEvt->tParams.tGpsWifiBlendReport.wifi_timeofpos));
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG,(" wifi_fix_valid %d", pEvt->tParams.tGpsWifiBlendReport.wifi_fix_valid));
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG,(" blended_latitude %d", pEvt->tParams.tGpsWifiBlendReport.blended_latitude));
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG,(" blended_longitude %d", pEvt->tParams.tGpsWifiBlendReport.blended_longitude));
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG,(" blended_altitude %d", pEvt->tParams.tGpsWifiBlendReport.blended_altitude));
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG,(" blended_timeofpos %d", pEvt->tParams.tGpsWifiBlendReport.blended_timeofpos));

#endif

		if (eRes != RES_OK)
		{
			mcpf_mem_free_from_pool (pNavc->hMcpf, pEvt);
            MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_gps_wifi_blend_pos_ind: mcpf_SendMsg failed\n"));
		}
	}
	else
	{
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_gps_wifi_blend_pos_ind: mcpf_mem_alloc_from_pool failed\n"));
	}
}

#endif

