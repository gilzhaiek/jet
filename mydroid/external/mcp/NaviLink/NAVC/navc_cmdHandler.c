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

/** \file   navc_cmdHandler.c 
 *  \brief  NAVC stack command handler implementation
 * 
 *  \see    navc_cmdHandler.h, navc_main.c
 */

#include "gpsc_data.h"
#include "gpsc_sequence.h"
#include "gpsc_timer_api.h"

#include "mcpf_mem.h"
#include "mcpf_services.h"
#include "mcpf_time.h"
#include "mcpf_report.h"
#include "mcpf_msg.h"
#include "navc_api.h"
#include "navc_api_pvt.h"
#include "navc_defs.h"
#include "navc_time.h"
#include "navc_cmdHandler.h"
#include "navc_sm.h"
#include "gpsc_app_api.h"
#include "navc_inavc_if.h"
#ifdef WIFI_ENABLE
#include "wpl_app_api.h"
#endif
#include "nav_log_msg.h"
#include "nav_log_cust_codes.h"

#ifdef ENABLE_INAV_ASSIST
#include "inavc_ext.h"
#endif

#include <utils/Log.h>
#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG "NAVD.NAVC_CMD_HANDLER"    // our identification for logcat (adb logcat NAVD.NAVC_CMD_HANDLER:V *:S)

/************************************************************************
 * Defines
 ************************************************************************/

#define CMD_QUEUE_STOP_MAX_TIME 	1000	/* 1 sec */

/************************************************************************
 * Types
 ************************************************************************/
extern TNAVCD_Ctrl *pNavcCtrl;

/************************************************************************
 * Internal functions prototypes
 ************************************************************************/

static void sendErrorEvt (const TNAVCD_Ctrl *pNavc, 
						  const EmcpTaskId	eDestTaskId, 
						  const McpU8  		uDestQId, 
						  const McpU16 		uOpcode,
						  const McpU16 		uCmplOpcode);

static void cmdQueStopTimerExpired (handle_t hNavc, McpUint uTimerId);

static void sendAssistReport (const TNAVCD_Ctrl 		*pNavc, 
							  McpU8 					*pBuf, 
							  McpU32 					uLen,
							  const ENAVC_evtOpcode 	eRespOpcode,
							  const ENAVC_cmdOpcode 	eComplCmd);



static void sendAssistPriorityReport (const TNAVCD_Ctrl 		*pNavc, 
							          McpU8 					*pBuf, 
							          McpU32 					uLen,
							          const ENAVC_evtOpcode 	eRespOpcode,
							          const ENAVC_cmdOpcode 	eComplCmd);


/************************************************************************
 *
 *   Module functions implementation
 *
 ************************************************************************/

/** 
 * \fn     navcMsg_handleCommand 
 * \brief  Process received NAVC command
 * 
 */
void NavcCmdHandler (handle_t hNavc, TmcpfMsg * pMsg)
{
    TNAVCD_Ctrl*       pNavc = (TNAVCD_Ctrl*) hNavc;
	 McpU32 			     uSesId = 0;
    TNAVC_cmdParams*   pCmdParams = (TNAVC_cmdParams*) pMsg->pData;
	 T_GPSC_result	     gpscRes = GPSC_SUCCESS;

	 McpU16			uOpCode = pMsg->uOpcode;
	 TNAVC_reqLocFix* blfReqLocFix = NULL;
	
	gpsc_ctrl_type *p_zGPSCControl = gp_zGPSCControl;

	// Update SrcTaskId and SrcQId only if it is Not related to Caliberation else it will create problem for further responses
	if ( (pMsg->uOpcode != NAVC_CMD_SET_REFCLK_PARAMETER) && ( (pMsg->uOpcode!=NAVC_CMD_INJECT_CALIB_CTRL) ))
	{
		pNavc->eSrcTaskId = pMsg->eSrcTaskId; 	/* Store the source of message to return response to it */
		pNavc->uSrcQId 	  = pMsg->uSrcQId;
		pNavc->eCmdInProcess = pMsg->uOpcode;
	}

	MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("navc_cmd: 0x%x", pMsg->uOpcode));

	// under certain circumstances the device might need to be woken up from here.
	NavcCheckandWakeupdevice(hNavc, pMsg->uOpcode);
	
    switch (pMsg->uOpcode)
    {

    case NAVC_CMD_REQUEST_FIX:

		/* Currently there is no Ref Clock Injection Support, so can't enable calibration */
		/* gpsc_app_inject_calib_control_req(GPSC_ENABLE_PERIODIC_CALIB_REF_CLK); */

		NavcCmdDisableQueue (hNavc);

		/* Read user defined session reference number from LSB byte of input session ID */
		uSesId = NAVCD_SESSION_REF_NUM_SET(uSesId, pCmdParams->tReqLocFix.loc_fix_session_id);
      uSesId = NAVCD_SESSION_PROT_ID_SET(uSesId, (pCmdParams->tReqLocFix.loc_fix_session_id>>8));
      
		/* Add source task and queue IDs, and protocol ID to location fix session ID */ 
		uSesId = NAVCD_SESSION_TASK_ID_SET(uSesId, pMsg->eSrcTaskId);
		uSesId = NAVCD_SESSION_QUE_ID_SET (uSesId, pMsg->uSrcQId);
		pCmdParams->tReqLocFix.loc_fix_session_id = uSesId;

		gpscRes = Navc_Priority_Handler(&pCmdParams->tReqLocFix);
		 
        if (gpscRes < GPSC_SUCCESS)
        {
			sendErrorEvt (pNavc, 
						  pMsg->eSrcTaskId, 
						  pMsg->uSrcQId, 
						  (McpU16) NAVC_EVT_POSITION_FIX_REPORT,
						  NAVC_CMD_REQUEST_FIX);
        }

		if (p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq != E_GPSC_SEQ_WAIT_ACTIVE)
		{
			NavcCmdEnableQueue (hNavc);
		}


	/* Start 10 minutes NVS timer. When this timer expires, NAVD will save its state to NVS */
	/* check if the timer is already started just to be safe. if already started then do not restart */
	if (GPSC_SUCCESS !=  gpsc_timer_status(p_zGPSCControl, C_TIMER_NVS_SAVE))
	{
		ALOGI("C_TIMER_NVS_SAVE Timer started");
		gpsc_timer_start(p_zGPSCControl, C_TIMER_NVS_SAVE, C_TIMER_NVS_SAVE_TIMEOUT);
	}

      break;

    case NAVC_CMD_STOP_FIX:

		MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("NavcCmdHandler: Disabling CMD-Q for NAVC_CMD_STOP_FIX"));
		NavcCmdDisableQueue (hNavc);

		/* C_SESSION_INVALID session ID is used to stop all the current sessesions */
		if (pCmdParams->tStopLocFix.uSessionId != C_SESSION_INVALID )
		{
			/* Read user defined session reference number from LSB byte of input session ID */
			uSesId = NAVCD_SESSION_REF_NUM_SET(uSesId, pCmdParams->tStopLocFix.uSessionId);

            /* Add source task and queue IDs, and protocol ID to location fix session ID */
            uSesId = NAVCD_SESSION_TASK_ID_SET(uSesId, pMsg->eSrcTaskId);
            uSesId = NAVCD_SESSION_QUE_ID_SET (uSesId, pMsg->uSrcQId);
        //  uSesId = NAVCD_SESSION_PROT_ID_SET(uSesId, NAVCD_PROT_PRIVATE);
            uSesId = NAVCD_SESSION_PROT_ID_SET(uSesId, (pCmdParams->tStopLocFix.uSessionId>>8));

            MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("1:NavcCmdHandler: NAVC_CMD_STOP_FIX SessionId: 0x%x", pCmdParams->tStopLocFix.uSessionId));
            gpscRes = gpsc_app_loc_fix_stop_req (uSesId);
			if (gpscRes != GPSC_PENDING)
			{
				MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("NavcCmdHandler: Enabling CMD-Q for NAVC_CMD_STOP_FIX"));
				NavcCmdEnableQueue (hNavc);

				if (gpscRes != GPSC_SUCCESS)
				{
					MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("NavcCmdHandler: gpsc_app_loc_fix_stop_req failed\n"));
				}
			}
			else
			{
				/* The enabling is delayed as some operations against the command are pending */
				MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("NavcCmdHandler: Delaying enabling CMD-Q for NAVC_CMD_STOP_FIX"));
			}

			//For CMCC logging, Session End should not be printed  if it is triggered by RRC/RRLP.
			if (NAVCD_SESSION_PROT_ID_GET(uSesId)==NAVCD_PROT_PRIVATE)
			{
				SESSLOGMSG("[%s]%s:%d,  %s \n",get_utc_time(),SESS_END,nav_sess_getCount(0) ,"# Session end\n\n");
				nav_sess_getCount(1);
			}

		/* stop timer for NVS save */
		if(gpsc_timer_status(p_zGPSCControl, C_TIMER_NVS_SAVE) == GPSC_SUCCESS)
		{
			ALOGI("NVS save timer stopped after loc fix stop");
			gpsc_timer_stop(p_zGPSCControl, C_TIMER_NVS_SAVE);
		}
        }
        else
        {
            MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("NavcCmdHandler: Invalid session ID"));
			MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("NavcCmdHandler: Enabling CMD-Q for NAVC_CMD_STOP_FIX"));
			NavcCmdEnableQueue (hNavc);
        }

		break;

    case NAVC_CMD_INJECT_ASSISTANCE:

		/* to check the assistance from sagps or pgps clients, if so set the flag
		   and being used to send the time oot info accordingly */
		 if(((McpU32)pMsg->eSrcTaskId == pNavc->tAssistSrcDb[SAGPS_PROVIDER].uAssistProviderTaskId) ||
			 ((McpU32)pMsg->eSrcTaskId == pNavc->tAssistSrcDb[PGPS_PROVIDER].uAssistProviderTaskId))
		 {
           pNavc->bAssistDataFromPgpsSagps = TRUE;
			  MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("NavcCmdHandler: assistance from SAGPS or PGPS\n"));
			 
		 }
		 else
           pNavc->bAssistDataFromPgpsSagps = FALSE;
		 
		gpscRes = gpsc_app_assist_inject_req (&pCmdParams->tInjectAssist.tAssistance);

		if (gpscRes < GPSC_SUCCESS)
		{
            MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("NavcCmdHandler: gpsc_app_assist_inject_req failed\n"));
		}

		/* 
		 * Store the source of assistance (modem) to request the pulse generation 
		 * for fine time injection upon request from GPSC
		 */
		pNavc->eAssistSrcTaskId = pMsg->eSrcTaskId;			
		pNavc->uAssistSrcQId    = pMsg->uSrcQId;

		switch(pCmdParams->tInjectAssist.tAssistance.ctrl_assistance_inject_union)
		{
			case GPSC_ASSIST_EPH:
				sendAssistReport(pNavc, (McpU8*)NULL, 0, NAVC_EVT_ASSIST_REP_EPH, NAVC_EVT_ASSIST_REP_EPH);
				break;

			case GPSC_ASSIST_TIME:
				sendAssistReport(pNavc, (McpU8*)NULL, 0, NAVC_EVT_ASSIST_REP_TIME, NAVC_EVT_ASSIST_REP_TIME);
				break;

			case GPSC_ASSIST_TOW:
				sendAssistReport(pNavc, (McpU8*)NULL, 0, NAVC_EVT_ASSIST_REP_TOW, NAVC_EVT_ASSIST_REP_TOW);
				break;

			case GPSC_ASSIST_POSITION:
				sendAssistReport(pNavc, (McpU8*)NULL, 0, NAVC_EVT_ASSIST_REP_POSITION, NAVC_EVT_ASSIST_REP_POSITION);
				break;

			case GPSC_ASSIST_ACQ:
				sendAssistReport(pNavc, (McpU8*)NULL, 0, NAVC_EVT_ASSIST_REP_ACQ, NAVC_EVT_ASSIST_REP_ACQ);
				break;

			case GPSC_ASSIST_ALMANAC:
				sendAssistReport(pNavc, (McpU8*)NULL, 0, NAVC_EVT_ASSIST_REP_ALMANAC, NAVC_EVT_ASSIST_REP_ALMANAC); 
				break;

			case GPSC_ASSIST_UTC:
				sendAssistReport(pNavc, (McpU8*)NULL, 0, NAVC_EVT_ASSIST_REP_UTC, NAVC_EVT_ASSIST_REP_UTC);
				break;

			case GPSC_ASSIST_IONO:
				sendAssistReport(pNavc, (McpU8*)NULL, 0, NAVC_EVT_ASSIST_REP_IONO, NAVC_EVT_ASSIST_REP_IONO);
				break;

			case GPSC_ASSIST_RTI:
				sendAssistReport(pNavc, (McpU8*)NULL, 0, NAVC_EVT_ASSIST_REP_RTI, NAVC_EVT_ASSIST_REP_RTI);
				break;

			case GPSC_ASSIST_COMPL_SET:
				// do nothing
			break;

			default:
				MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcCmd_handleCommand: NAVC_CMD_INJECT_ASSISTANCE: INVALID PARAM: 0x%x\n", pCmdParams->tInjectAssist.tAssistance.ctrl_assistance_inject_union));
		}


        break;

   case NAVC_CMD_BEGIN_ASSISTANCE:
   {
      RECON_gpsc_app_assist_begin_req ();
   }
   break;

	case NAVC_CMD_COMPLETE_ASSISTANCE:
		gpscRes = gpsc_app_assist_complete_req();
		RECON_set_assist_in_progress (0);
		if (gpscRes < GPSC_SUCCESS)
		{
            MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("NavcCmdHandler: gpsc_app_assist_complete_req failed\n"));
		}
        break;

    case NAVC_CMD_DELETE_ASISTANCE:

		gpscRes = gpsc_app_assist_delete_req (pCmdParams->tDelAssist.uDelAssistBitmap,
									    pCmdParams->tDelAssist.uSvBitmap ); 
		if (gpscRes < GPSC_SUCCESS)
		{
            MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("NavcCmdHandler: gpsc_app_assist_delete_req failed\n"));
		}
		break;

		case NAVC_CMD_SAVE_ASSISTANCE:
		{
			gpscRes = gpsc_app_nvs_file_req (); 
			if (gpscRes < GPSC_SUCCESS)
			{
							MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("NavcCmdHandler: gpsc_app_assist_delete_req failed\n"));
			}

		}
		break;

		case NAVC_CMD_SUPLC_SESSION_RESULT:
		{
			if((pCmdParams->tSuplSessionSuccess == TRUE) &&
				(p_zGPSCControl->p_zSuplStatus.u_GetAssistSUPLRequestPending == FALSE))
			{
				MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("SUPL session successful"));
				/* Reset the flag to allow further requests. */			
				p_zGPSCControl->p_zSuplStatus.u_GetAssistSUPLRequest = FALSE;
				/* Reset the SUPL request count */
				p_zGPSCControl->p_zSuplStatus.u_SessRequestCount = 0;
			}
			else
			{
				MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("SUPL session failed"));

				if(E_GPSC_STATE_ACTIVE == p_zGPSCControl->p_zGpscSm->e_GpscCurrState)			
				{
					if(p_zGPSCControl->p_zSuplStatus.u_SessRequestCount < 
									p_zGPSCControl->p_zSessConfig->w_TotalNumSession)
					{
						MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, 
								("ReInitiate SUPL session to get assistance"));

						MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, 
								("Request Count = %d", p_zGPSCControl->p_zSuplStatus.u_SessRequestCount));
						gpsc_get_assistance_from_network(p_zGPSCControl);

						p_zGPSCControl->p_zSuplStatus.u_GetAssistSUPLRequest = TRUE;
						
						/* Increment the SUPL request count */
						p_zGPSCControl->p_zSuplStatus.u_SessRequestCount++;
					}
					else
					{
						/* Reset the flag to allow further requests. */ 		
						p_zGPSCControl->p_zSuplStatus.u_GetAssistSUPLRequest = FALSE;
						/* Reset the SUPL request count */
						p_zGPSCControl->p_zSuplStatus.u_SessRequestCount = 0;
					}					
				}
				else
				{
					/* Reset the flag to allow further requests. */ 		
					p_zGPSCControl->p_zSuplStatus.u_GetAssistSUPLRequest = FALSE;
					/* Reset the SUPL request count */
					p_zGPSCControl->p_zSuplStatus.u_SessRequestCount = 0;
				}

				/* Check the flag */ 		
				if(p_zGPSCControl->p_zSuplStatus.u_GetAssistSUPLRequest != TRUE)
				{
					/* Confirm if sequence is OK */
					MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("Recheck Sequence."));
				}				
			}
		}
		break;


		
#if 0 /* TODO, not implemented in the GPSC core */
	case NAVC_CMD_INJECT_CONFIGURATION:

		/* TODO, not implemented in the GPSC core */
		#if 0
		gpscRes = gpsc_app_inject_configuration_req (&pCmdParams->tInjectConfig.tConfigFile,
											    pCmdParams->tInjectConfig.tSaveToFile );
		if (gpscRes < GPSC_SUCCESS)
		{
            MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("NavcCmdHandler: gpsc_app_inject_configuration_req failed\n"));
		}
		#endif
        break;

	case NAVC_CMD_READ_CONFIGURATION:

		/* TODO, not implemented in the GPSC core */
		#if 0
		gpscRes = gpsc_app_request_configuration_req (&pCmdParams->tReadConfig.tConfigFile);
		if(gpscRes< GPSC_SUCCESS)
		{
            MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("NavcCmdHandler: gpsc_app_request_configuration_req failed\n"));
		}
		#endif
        break;
#endif

	case NAVC_CMD_SET_HOST_WAKEUP_PARAMS:

		gpscRes = gpsc_app_set_host_wakeup_params (&pCmdParams->tSetHostWakeupParams);
		if(gpscRes< GPSC_SUCCESS)
		{
			MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, 
				("NavcCmdHandler: gpsc_app_set_host_wakeup_params failed\n"));
		}
        break;

	case NAVC_CMD_SET_APM_PARAMS:

			uSesId = NAVCD_SESSION_REF_NUM_SET(uSesId, pCmdParams->tSetAPMParams.loc_sessID);
			uSesId = NAVCD_SESSION_PROT_ID_SET(uSesId, (pCmdParams->tSetAPMParams.loc_sessID >> 8));
			/* Add source task and queue IDs, and protocol ID to Geo fence session ID */
			uSesId = NAVCD_SESSION_TASK_ID_SET(uSesId, pMsg->eSrcTaskId);
			uSesId = NAVCD_SESSION_QUE_ID_SET (uSesId, pMsg->uSrcQId);

			gpscRes = gpsc_app_control_apm_params (&pCmdParams->tSetAPMParams,uSesId);
		if(gpscRes< GPSC_SUCCESS)
		{
			MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, 
				("NavcCmdHandler: gpsc_app_set_apm_params failed\n"));
		}
        break;

	case NAVC_CMD_SET_SBAS_PARAMS:

		gpscRes = gpsc_app_set_sbas_params (&pCmdParams->tSetSBASParams);
		if(gpscRes< GPSC_SUCCESS)
		{
			MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, 
				("NavcCmdHandler: gpsc_app_set_sbas_params failed\n"));
		}
        break;

	case NAVC_CMD_SET_MOTION_MASK:
		
	/* Read user defined session reference number from LSB byte of input session ID */
		uSesId = NAVCD_SESSION_REF_NUM_SET(uSesId, pCmdParams->tSetMotionMask.uSessionId);
       	        uSesId = NAVCD_SESSION_PROT_ID_SET(uSesId, (pCmdParams->tSetMotionMask.uSessionId>>8));      
		/* Add source task and queue IDs, and protocol ID to Geo fence session ID */ 
		uSesId = NAVCD_SESSION_TASK_ID_SET(uSesId, pMsg->eSrcTaskId);
		uSesId = NAVCD_SESSION_QUE_ID_SET (uSesId, pMsg->uSrcQId);
		pCmdParams->tSetMotionMask.uSessionId= uSesId;			
		MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("NavcCmdHandler: set motion mask task id %d \n",pMsg->eSrcTaskId));
		MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("NavcCmdHandler: set motion mask session id 0x%x \n",uSesId));

		gpscRes = gpsc_app_set_motion_mask (&pCmdParams->tSetMotionMask,uSesId);
		if(gpscRes< GPSC_SUCCESS)
		{
			MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG,
				("NavcCmdHandler: gpsc_app_set_motion_mask failed\n"));
		}


        break;

case NAVC_CMD_GET_MOTION_MASK:

		gpscRes = gpsc_app_get_motion_mask (pCmdParams->GeoFence_regionNumber);
		if(gpscRes< GPSC_SUCCESS)
		{
			MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, 
				("NavcCmdHandler: gpsc_app_get_motion_mask failed\n"));
		}
        break;

	case NAVC_CMD_ENABLE_KALMAN_FILTER:

		gpscRes = gpsc_app_enable_kalman_filter (&pCmdParams->tEnableKalmanFilter);
		if(gpscRes< GPSC_SUCCESS)
		{
			MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, 
				("NavcCmdHandler: gpsc_app_enable_kalman_filter failed\n"));
		}
        break;

	case NAVC_CMD_SET_REFCLK_PARAMETER:

		gpscRes = gpsc_app_inject_ref_clock_parameters_req (pCmdParams->tRefClock.uRefClockQuality,
											  pCmdParams->tRefClock.uRefClockFrequency);
		if(gpscRes< GPSC_SUCCESS)
		{
            MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("NavcCmdHandler: gpsc_app_inject_ref_clock_parameters_req failed\n"));
			gpscRes = GPSC_FAIL;					
		}

        break;

	case NAVC_CMD_INJECT_CALIB_CTRL:

		gpscRes = gpsc_app_inject_calib_control_req (pCmdParams->tInjectCalibCtrl.uCalibType);
		if(gpscRes< GPSC_SUCCESS)
		{
            MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("NavcCmdHandler: gpsc_app_inject_calib_control_req failed\n"));
			gpscRes = GPSC_FAIL;				
		}

        break;

	case NAVC_CMD_SET_TOW:
		 {	
			 TNAVCD_GpsTime tGpsTime;
			tGpsTime.tTime = pCmdParams->tSetTow.tTimeAssist;
			
			tGpsTime.uSystemTimeMs = mcpf_getCurrentTime_InMilliSec(pNavc->hMcpf);

			navcTime_setGpsTime (pNavc, &tGpsTime, pCmdParams->tSetTow.eTimeMode);
		 }
        break;

	case NAVC_CMD_PLT:
              {
		TNAVC_plt *tPltParams;
                tPltParams = (TNAVC_plt * ) pMsg;
		NavcCmdDisableQueue (hNavc);
                
		//gpscRes = gpsc_app_prodlinetest_req (&pCmdParams->tPltParams);
                gpscRes = gpsc_app_prodlinetest_req (tPltParams);
		if(gpscRes < GPSC_SUCCESS)
		{
			NavcCmdEnableQueue (hNavc);
            MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("NavcCmdHandler: gpsc_app_prodlinetest_req failed\n"));
		}
             }
        break;

	case NAVC_CMD_GET_VERSION:
		gpscRes = gpsc_app_gps_ver_req ();
		if(gpscRes < GPSC_SUCCESS)
		{
            MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("NavcCmdHandler: gpsc_app_gps_ver_req failed\n"));
		}
		break;

	case NAVC_CMD_BLF_CONFIG:
		/* Read user defined session reference number from LSB byte of input session ID */
		uSesId = NAVCD_SESSION_REF_NUM_SET(uSesId, pCmdParams->tBlfConfig.uSessionId);
       	        uSesId = NAVCD_SESSION_PROT_ID_SET(uSesId, (pCmdParams->tBlfConfig.uSessionId>>8));      
		/* Add source task and queue IDs, and protocol ID to Geo fence session ID */ 
		uSesId = NAVCD_SESSION_TASK_ID_SET(uSesId, pMsg->eSrcTaskId);
		uSesId = NAVCD_SESSION_QUE_ID_SET (uSesId, pMsg->uSrcQId);
		pCmdParams->tBlfConfig.uSessionId= uSesId;
                gpscRes = gpsc_app_blf_session_request(&pCmdParams->tBlfConfig); 
                if(gpscRes < GPSC_SUCCESS)
                {
                        MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcCmd_handleCommand: gpsc_app_blf_session_request failed\n"));
			break;
                }

		/* Allocate memory for blfReqLocFix structure */
		blfReqLocFix = (TNAVC_reqLocFix *)mcpf_mem_alloc(NULL,sizeof(TNAVC_reqLocFix)); 
		if(blfReqLocFix == NULL)
		{
                        MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcCmd_handleCommand: mcpf_mem_alloc() for NAVC_CMD_BLF_CONFIG  failed\n"));
			break;
		}
    	        blfReqLocFix->loc_fix_session_id = uSesId;
		blfReqLocFix->loc_fix_result_type_bitmap = pCmdParams->tBlfConfig.loc_fix_result_type_bitmap;
		blfReqLocFix->loc_fix_mode = pCmdParams->tBlfConfig.loc_fix_mode;
		blfReqLocFix->loc_fix_max_ttff = pCmdParams->tBlfConfig.loc_fix_max_ttff;
		blfReqLocFix->loc_fix_num_reports = pCmdParams->tBlfConfig.loc_fix_num_reports;
		blfReqLocFix->loc_fix_period = pCmdParams->tBlfConfig.loc_fix_period;

		gpscRes = gpsc_app_loc_fix_req(blfReqLocFix, 1);
		if (gpscRes < GPSC_SUCCESS)
		{

			

			sendErrorEvt (pNavc, 
					  pMsg->eSrcTaskId, 
					  pMsg->uSrcQId, 
					  (McpU16) NAVC_EVT_POSITION_FIX_REPORT,
					  NAVC_CMD_REQUEST_FIX);
		}

		/* blfReqLocFix is no longer used. Free it now */
		mcpf_mem_free(NULL, blfReqLocFix);
	break;
        case NAVC_CMD_BLF_STATUS:

                /* Any session can request the status of BLF */
                gpscRes = gpsc_app_blf_sts_req();
                if(gpscRes < GPSC_SUCCESS)
                {
                        MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcCmd_handleCommand: gpsc_app_blf_sts_req failed\n"));
                }
                
        break;

        case NAVC_CMD_BLF_BUFFER_DUMP:

                gpscRes = gpsc_app_blf_buff_dump_req();
                if(gpscRes < GPSC_SUCCESS)
                {
                        MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcCmd_handleCommand: gpsc_app_blf_buff_dump_req\n"));
                }
                
		break;

	case NAVC_CMD_GET_ASSIST_EPH:
		{
			T_GPSC_assistance_database_report tAssistance_database_report;

			mcpf_mem_zero(pNavc->hMcpf, &tAssistance_database_report, sizeof(T_GPSC_assistance_database_report));
			tAssistance_database_report.ctrl_assistance_inject_union_report = GPSC_ASSIST_EPH_REP;
			if(gpsc_app_assist_database_req(&tAssistance_database_report) != GPSC_SUCCESS)
			{
				MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcCmd_handleCommand: gpsc_app_assist_database_req failed\n"));
			}
			sendAssistReport(pNavc, (McpU8*)(&tAssistance_database_report.assistance_inject_union_report), 
							 sizeof(T_GPSC_assistance_inject_union_report), 
							 NAVC_EVT_ASSIST_REP_EPH ,NAVC_CMD_GET_ASSIST_EPH);
		}
		break;

	case NAVC_CMD_GET_ASSIST_IONO:
		{
			T_GPSC_assistance_database_report tAssistance_database_report;
			
			tAssistance_database_report.ctrl_assistance_inject_union_report = GPSC_ASSIST_IONO_REP;
			gpscRes = gpsc_app_assist_database_req(&tAssistance_database_report);
			if(gpscRes < GPSC_SUCCESS)
			{
				MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcCmd_handleCommand: gpsc_app_assist_database_req failed\n"));
			}
			sendAssistReport(pNavc, (McpU8*)(&tAssistance_database_report.assistance_inject_union_report), 
							 sizeof(T_GPSC_assistance_inject_union_report), 
							 NAVC_EVT_ASSIST_REP_IONO ,NAVC_CMD_GET_ASSIST_IONO);
		}
		break;

	case NAVC_CMD_GET_ASSIST_ALMANAC:
		{
			T_GPSC_assistance_database_report tAssistance_database_report;
			
			tAssistance_database_report.ctrl_assistance_inject_union_report = GPSC_ASSIST_ALMANAC_REP;
			gpscRes = gpsc_app_assist_database_req(&tAssistance_database_report);
			if (gpscRes < GPSC_SUCCESS)
			{
				MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcCmd_handleCommand: gpsc_app_assist_database_req failed\n"));
			}
			sendAssistReport(pNavc, (McpU8*)(&tAssistance_database_report.assistance_inject_union_report), 
							 sizeof(T_GPSC_assistance_inject_union_report), 
							 NAVC_EVT_ASSIST_REP_ALMANAC ,NAVC_CMD_GET_ASSIST_ALMANAC);
		}
		break;

	case NAVC_CMD_GET_ASSIST_TIME:
		{
			T_GPSC_assistance_database_report tAssistance_database_report;
			
			tAssistance_database_report.ctrl_assistance_inject_union_report = GPSC_ASSIST_TIME_REP;
			gpscRes = gpsc_app_assist_database_req(&tAssistance_database_report);
			if ( gpscRes != GPSC_TIME_PULSE_GENERATION_PENDING)
			{
			  if(gpscRes < GPSC_SUCCESS)
			  {
			  	MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcCmd_handleCommand: gpsc_app_assist_database_req TIME failed\n"));
			}
			sendAssistReport(pNavc, (McpU8*)(&tAssistance_database_report.assistance_inject_union_report), 
							 sizeof(T_GPSC_assistance_inject_union_report), 
							 NAVC_EVT_ASSIST_REP_TIME ,NAVC_CMD_GET_ASSIST_TIME);
			} else{
				/* Note that return value of GPSC_TIME_PULSE_GENERATION_PENDING is as an internal event in the current 
				** context to avoid sending respose to the TI-Drv immediately as the reseponse will be sent 
				** in gpsc_app_assist_time_ind
				*/
				gpscRes = GPSC_SUCCESS;
			}
		}
		break;


	case NAVC_CMD_GET_ASSIST_POSITION:
		{
			T_GPSC_assistance_database_report tAssistance_database_report;
			
			tAssistance_database_report.ctrl_assistance_inject_union_report = GPSC_ASSIST_POSITION_REP;
			gpscRes = gpsc_app_assist_database_req(&tAssistance_database_report);
			if(gpscRes < GPSC_SUCCESS)
			{
				MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcCmd_handleCommand: gpsc_app_assist_database_req failed\n"));
			}
			sendAssistReport(pNavc, (McpU8*)(&tAssistance_database_report.assistance_inject_union_report), 
							 sizeof(T_GPSC_assistance_inject_union_report), 
							 NAVC_EVT_ASSIST_REP_POSITION ,NAVC_CMD_GET_ASSIST_POSITION);
		}
		break;

	case NAVC_CMD_GET_SV_STATUS:
		{
			TNAVC_evt 	*pEvt;
			
			pEvt = (TNAVC_evt *) mcpf_mem_alloc_from_pool (pNavc->hMcpf, pNavc->hEvtPool);
			if (pEvt == NULL)
			{
				MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcCmd_handleCommand: mcpf_mem_alloc_from_pool failed\n"));
			}
			else
			{
				gpscRes = gpsc_app_svstatus_req(&pEvt->tParams.tSvStatus);
				if(gpscRes < GPSC_SUCCESS)
				{
					MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, 
						("navcCmd_handleCommand: gpsc_app_svstatus_req failed\n"));
				}
				else
				{
					pEvt->eResult = RES_OK;
					pEvt->eComplCmd = NAVC_CMD_GET_SV_STATUS;
				
					if(mcpf_SendMsg (pNavc->hMcpf, pNavc->eSrcTaskId,  pNavc->uSrcQId,  TASK_NAV_ID, NAVC_QUE_CMD_ID,
						(McpU16) NAVC_EVT_SV_STATUS_REPORT, sizeof(T_GPSC_sv_status)+NAVC_EVT_HEADER_SIZE, 0, pEvt) != RES_OK)
					{
						mcpf_mem_free_from_pool (pNavc->hMcpf, pEvt);
		            			MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcCmd_handleCommand: mcpf_SendMsg failed\n"));
					}
				}
			}
		}
		break;
		
	case NAVC_CMD_SAGPS_PROVIDER_REGISTER:

		{
		     /* Store the PGPS client task id for sending the reports */
			pNavc->tAssistSrcDb[pCmdParams->tAssistSrcType.eAssistSrcType].uAssistSrcType =(McpU8)SAGPS_PROVIDER;
		    pNavc->tAssistSrcDb[pCmdParams->tAssistSrcType.eAssistSrcType].uAssistProviderTaskId = pMsg->eSrcTaskId;
		    pNavc->tAssistSrcDb[pCmdParams->tAssistSrcType.eAssistSrcType].uAssistProviderQueueId = pMsg->uSrcQId;
			MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, 
						("navcCmd_handleCommand: Assistancesource PGPS/SAGPS registered %d\n",pCmdParams->tAssistSrcType.eAssistSrcType));

				pNavc->uSagpsProviderTaskId = pMsg->uUserDefined;
		}
	break;
#ifdef WIFI_ENABLE 

	case NAVC_CMD_WPL_REGISTER: 



		{

			 /* Store the wpl task id for sending the reports */

				MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, 

						("navcCmd_handleCommand: wpl registered "));

				pNavc->uwplTaskId = pMsg->uUserDefined;

		}

	break;

#endif

     /* for registering the assistance source
	    store the taskid and src type in our local database*/
	case NAVC_CMD_REGISTER_ASSIST_SRC:   
		{   
			if ( (McpU8)pCmdParams->tAssistSrcType.eAssistSrcType < MAX_ASSIST_PROVIDER)
			{
			    pNavc->tAssistSrcDb[pCmdParams->tAssistSrcType.eAssistSrcType].uAssistSrcType =(McpU8)pCmdParams->tAssistSrcType.eAssistSrcType;
		       pNavc->tAssistSrcDb[pCmdParams->tAssistSrcType.eAssistSrcType].uAssistProviderTaskId = pMsg->eSrcTaskId;
		       pNavc->tAssistSrcDb[pCmdParams->tAssistSrcType.eAssistSrcType].uAssistProviderQueueId = pMsg->uSrcQId;

			   if (pCmdParams->tAssistSrcType.eAssistSrcType == SAGPS_PROVIDER || pCmdParams->tAssistSrcType.eAssistSrcType == PGPS_PROVIDER)

			   {
               pNavc->uSagpsProviderTaskId = pMsg->eSrcTaskId;
			   }

            ALOGI("+++ %s: Assistance Source Registered. Type [%d], Task ID [%d] +++\n",
               __FUNCTION__, pCmdParams->tAssistSrcType.eAssistSrcType, pMsg->eSrcTaskId);

			}
			else
			{
				MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, 
						("navcCmd_handleCommand: Assistancesource type %d is not valid\n",pCmdParams->tAssistSrcType.eAssistSrcType));
			}

		}   
	break;

	/* application requested the assistance src priority send the response with the prority
	   of all the registered assistance source */
	case NAVC_CMD_GET_ASSIST_PRIORITY:
		{
			
			MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcCmd_handleCommand: Requested for assistance priority\n"));
			
			sendAssistPriorityReport(pNavc, NULL, 
							 0, 
							 NAVC_EVT_ASSIST_SRC_REP_PRIORITY ,NAVC_CMD_GET_ASSIST_PRIORITY);



		}
	break;
    /* here we update the database with the priority set by the application*/

	case NAVC_CMD_SET_ASSIST_PRIORITY:
		{
         McpU8 index,assistSrcTyp = 0xFF;
		   for (index = 0; index < MAX_ASSIST_PROVIDER; index++)
		   {
			  assistSrcTyp = (McpU8)pCmdParams->tAssistSrcPrioritySet[index].eAssistSrcType;
			  if (assistSrcTyp < MAX_ASSIST_PROVIDER)
			  {
			     pNavc->tAssistSrcDb[index].uAssistSrcType = assistSrcTyp;
			     pNavc->tAssistSrcDb[index].uAssistSrcPriority = pCmdParams->tAssistSrcPrioritySet[index].assist_src_priority;
			  }
		   }

		}
		break;

		
	case NAVC_CMD_GET_ASSIST_UTC:
		{
			T_GPSC_assistance_database_report tAssistance_database_report;
			
			tAssistance_database_report.ctrl_assistance_inject_union_report = GPSC_ASSIST_UTC_REP;
			gpscRes = gpsc_app_assist_database_req(&tAssistance_database_report);
			if (gpscRes < GPSC_SUCCESS)
			{
				MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcCmd_handleCommand: gpsc_app_assist_database_req UTC failed\n"));
			}
			sendAssistReport(pNavc, (McpU8*)(&tAssistance_database_report.assistance_inject_union_report), 
							 sizeof(T_GPSC_assistance_inject_union_report), 
							 NAVC_EVT_ASSIST_REP_UTC ,NAVC_CMD_GET_ASSIST_UTC);
		}
		break;

	case NAVC_CMD_GET_ASSIST_TTL:
		{
			T_GPSC_assistance_database_report tAssistance_database_report;
			
			tAssistance_database_report.ctrl_assistance_inject_union_report = GPSC_ASSIST_TTL_REP;
			gpscRes = gpsc_app_assist_database_req(&tAssistance_database_report);
			if(gpscRes < GPSC_SUCCESS)
			{
				MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcCmd_handleCommand: gpsc_app_assist_database_req TTL failed\n"));
			}
			sendAssistReport(pNavc, (McpU8*)(&tAssistance_database_report.assistance_inject_union_report), 
							 sizeof(T_GPSC_assistance_inject_union_report), 
							 NAVC_EVT_ASSIST_REP_TTL ,NAVC_CMD_GET_ASSIST_TTL);
		}
		break;


	case NAVC_CMD_GPS_SLEEP:
		{
			gpscRes = gpsc_app_gps_sleep();
			if(gpscRes < GPSC_SUCCESS)
			{
				MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcCmd_handleCommand: gpsc_app_gps_sleep failed\n"));
			}

		}
		break;

	case NAVC_CMD_GPS_WAKEUP:
		{
			gpscRes = gpsc_app_gps_wakeup();
			if(gpscRes < GPSC_SUCCESS)
			{
				MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcCmd_handleCommand: gpsc_app_gps_wakeup failed\n"));
			}

		}
		break;

	case NAVC_CMD_INJ_TCXO:
		{
			gpscRes = gpsc_app_inj_tcxo_req(&pCmdParams->tInjectFreqEst);
			if(gpscRes < GPSC_SUCCESS)
			{
				MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcCmd_handleCommand: gpsc_app_inj_tcxo_req failed\n"));
			}

		}
		break;

	case NAVC_CMD_GET_WISHLIST:
		{
			TNAVC_evt 	*pEvt;
			pEvt = (TNAVC_evt *) mcpf_mem_alloc_from_pool (pNavc->hMcpf, pNavc->hEvtPool);
			
			if (pEvt == NULL)
			{
				MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcCmd_handleCommand: mcpf_mem_alloc_from_pool failed\n"));
			}
			else
			{
				gpscRes = gpsc_get_wishlist(&pEvt->tParams.tWishlist);
				if(gpscRes < GPSC_SUCCESS)
				{
					MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, 
						("navcCmd_handleCommand: gpsc_app_svstatus_req failed\n"));
				}
				else
				{
					pEvt->eResult = RES_OK;
					pEvt->eComplCmd = NAVC_CMD_GET_WISHLIST;
				
					if(mcpf_SendMsg (pNavc->hMcpf, pNavc->eSrcTaskId,  pNavc->uSrcQId,  TASK_NAV_ID, NAVC_QUE_CMD_ID,
						(McpU16) NAVC_EVT_CMD_COMPLETE, sizeof(T_GPSC_sv_status)+NAVC_EVT_HEADER_SIZE, 0, pEvt) != RES_OK)
					{
						mcpf_mem_free_from_pool (pNavc->hMcpf, pEvt);
		            			MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcCmd_handleCommand: mcpf_SendMsg failed\n"));
					}
				}
			}
		}
		break;
	
    case NAVC_CMD_SUPL_PROVIDER_REGISTER:
    {
        /** This message is sent by assistance sources such as SUPLC.
        * Hitherto, NAVC directly communicates with registered assistance source if required. */
        pNavc->eAssistSrcTaskId = pMsg->eSrcTaskId;
        pNavc->uAssistSrcQId    = pMsg->uSrcQId;
    }	break;

	 case NAVC_CMD_QOP_TIMEOUT:
	 {
	 
		/* Read user defined session reference number from LSB byte of input session ID */
		uSesId = NAVCD_SESSION_REF_NUM_SET(uSesId, pCmdParams->tReqLocFix.loc_fix_session_id);
        uSesId = NAVCD_SESSION_PROT_ID_SET(uSesId, (pCmdParams->tReqLocFix.loc_fix_session_id>>8));      
		/* Add source task and queue IDs, and protocol ID to location fix session ID */ 
		uSesId = NAVCD_SESSION_TASK_ID_SET(uSesId, pMsg->eSrcTaskId);
		uSesId = NAVCD_SESSION_QUE_ID_SET (uSesId, pMsg->uSrcQId);
		pCmdParams->tReqLocFix.loc_fix_session_id = uSesId;
		gpsc_app_inject_new_timeout (&pCmdParams->tReqLocFix);	
		
	}break;

		case NAVC_CMD_QOP_UPDATE:
			{
				/* Read user defined session reference number from LSB byte of input session ID */
				uSesId = NAVCD_SESSION_REF_NUM_SET(uSesId, pCmdParams->tUpdate_rate.loc_sessID);
				uSesId = NAVCD_SESSION_PROT_ID_SET(uSesId, (pCmdParams->tUpdate_rate.loc_sessID >> 8));
				/* Add source task and queue IDs, and protocol ID to location fix session ID */
				uSesId = NAVCD_SESSION_TASK_ID_SET(uSesId, pMsg->eSrcTaskId);
				uSesId = NAVCD_SESSION_QUE_ID_SET (uSesId, pMsg->uSrcQId);
				pCmdParams->tUpdate_rate.loc_sessID = uSesId;
				MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG,("NavcCmdHandler: loc_sessID   [%d] \n",pCmdParams->tUpdate_rate.loc_sessID));
				
				gpsc_app_inject_fix_update_rate (&pCmdParams->tUpdate_rate);

			}
			break;
    case NAVC_CMD_SW_RESET:
        {
          gpsc_app_sw_reset();
        }
        break;

		
		case NAVC_CMD_ENTER_FWD_MODE:
		{
			gpscRes = gpsc_app_enter_fwd_req();
			if(gpscRes != GPSC_SUCCESS)
			{
				MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcCmd_handleCommand: gpsc_app_enter_fwd_req failed\n"));
			}
			
		}break;

		case NAVC_CMD_EXIT_FWD_MODE:
		{
			gpscRes = gpsc_app_exit_fwd_req();
			if(gpscRes != GPSC_SUCCESS)
			{
				MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcCmd_handleCommand: gpsc_app_exit_fwd_req failed\n"));
			}
			
		}break;

		case NAVC_CMD_FWD_TX_DATA:
		{
			McpU16 w_NumBytes = (McpU16)pCmdParams->tPropProtoMsg.numBytes;
			U8 *p_uDataBuffer = (U8 *)pCmdParams->tPropProtoMsg.ai2Msg;

			gpscRes = gpsc_app_fwd_tx_req(p_uDataBuffer, w_NumBytes);
			if(gpscRes != GPSC_SUCCESS)
			{
				MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcCmd_handleCommand: gpsc_app_fwd_tx_req failed\n"));
			}
			
		}
      break;
		
		case NAVC_CMD_SET_LOG_MODE:
		{

			MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("NAVC-CMD: LogMode Int: %x, Sensor: %x", 
																		pCmdParams->tLogMode.navc_log_mode,
																		pCmdParams->tLogMode.sensor_log_mode));
				gpsc_ctrl_type*    p_zGPSCControl = gp_zGPSCControl;

			gpsc_log_ctrl (p_zGPSCControl, 
						pCmdParams->tLogMode.navc_log_mode, 
						pCmdParams->tLogMode.sensor_log_mode,
                  pCmdParams->tLogMode.Session_log_mode);


		}break;

		case NAVC_CMD_REP_MEAS_ON_GF_BREACH:
		{

			MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("NAVC-CMD: NAVC_CMD_REP_MEAS_NO_GF_BREACH: %d",(McpU8)pCmdParams->uRepOnGfBreach));

			gpsc_app_rep_on_gf_breach((McpU8)pCmdParams->uRepOnGfBreach);
			

		}break;
		case NAVC_CMD_GET_AGC:
		{
			MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("NAVC-CMD: get AGC"));

			gpscRes = gpsc_app_get_agc();

			if (gpscRes == GPSC_SUCCESS)
			{
				/* handle Success */
			}
			else if (gpscRes == GPSC_FAIL)
			{
				/* hendle Fail */
			}
			else
			{
				/* handle unknown */
			}
			
		}break;
		case NAVC_CMD_POS_IND:
				{
					if(&pCmdParams->tPosInd != NULL)
						{
							if(gpsc_app_send_msa_supl_postion(&pCmdParams->tPosInd) != GPSC_SUCCESS)
							//LOGD("gpsc_app_send_msa_supl_position Failed!!");
							MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_send_msa_supl_position Failed!!"));
						}
					else
						MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("Error!! Position not not reported by SUPL_END!"));
				}
					break;
		case NAVC_CMD_REG_FOR_ASSISTANCE:
		{
			McpU16 AssistFlags = pCmdParams->tRegAssist.AssistBitmap;
		
			MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("NAVC-CMD: NAVC_CMD_REG_FOR_ASSISTANCE: 0x%X", AssistFlags));
			gpscRes = gpsc_register_for_assistance((McpU16)AssistFlags, (McpU8)pMsg->eSrcTaskId, (McpU8)pMsg->uSrcQId);
		}break;
					
#ifdef WIFI_ENABLE
		case NAVC_CMD_SET_WIFI_POSITION:
		{
			MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, 

					("navcCmd_handleCommand: fix from wpl "));

			TNAVC_wifiPosition*	pwifiPosition=(TNAVC_wifiPosition*)pMsg->pData;

			gpsc_app_wifi_pos_update(pwifiPosition);
						
		}break;
#endif

#ifdef ENABLE_INAV_ASSIST
		case NAVC_CMD_SIMULATE_DR:
		{
			McpU16 w_NumBytes = (McpU16)pCmdParams->tPropProtoMsg.numBytes;
			U8 *p_uDataBuffer = (U8 *)pCmdParams->tPropProtoMsg.ai2Msg;

			MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("NAVC-CMD: Simulate DR : %d bytes", w_NumBytes));

			gpsc_app_simulate_dr(p_uDataBuffer, w_NumBytes);

		}break;
		
		case NAVC_CMD_INAV_CTRL:
		{
			ENAVC_inav_ctrl inav_ctrl_sel = pCmdParams->tINAVCtrlSel.inav_ctrl;
			
			switch(inav_ctrl_sel)
			{
				case    INAV_DISABLE:
				{
					/* De-Initialize INAVC */
					MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("NAVC-CMD: De-Initialize INAVC"));
					inavc_deinit(pNavcCtrl->hInavc);
				}break;
				case	INAV_ENABLE_DR:
				{
					/* Initialize INAVC for sensor assisted support */
					MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("NAVC-CMD: Initialize INAVC"));
					e_result = inavc_init(pNavcCtrl->hInavc, INAV_ENABLE_DR);
					if(e_result != RES_OK)
					{
						MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG,
							("NAVC-CMD: initialize_inavc failed\n"));
					}
				}break;
				case	INAV_ENABLE_SMARTAPM:
				{
					MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("NAVC-CMD: SmartAPM Unsupported"));
				}break;
				default:
				{
					MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("NAVC-CMD: Unsupported INAVC Command"));
				}break;
			}
		}break;
#endif		
		
    default:
        MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("NavcCmdHandler: uknown cmd=%u\n", pMsg->uOpcode));
		
		gpscRes = GPSC_FAIL;
        break;
    }

	/* Free received msg and data buffer */
	if (pMsg->pData)
	{
		mcpf_mem_free_from_pool (pNavc->hMcpf, pMsg->pData);
	}
	mcpf_mem_free_from_pool (pNavc->hMcpf, (void *) pMsg);


        // Send RES_OK on GPSC_SUCCESS and GPSC_PENDING. Otherwise, send RES_ERROR.
		sendCmdCompleteEvt(pNavc, pNavc->eSrcTaskId, pNavc->uSrcQId, 
			(gpscRes >= GPSC_SUCCESS ? RES_OK : RES_ERROR), uOpCode);

//	return gpscRes;
}

/** 
 * \fn     NavcCheckandWakeupdevice 
 * \brief  Wake up the device if it is in sleep
 * 
 * \note
 * \param	pNavc - pointer to NAVC control object
 * \param	uOpcode  - current opcode being handled by the command handler
 * \return 	void
 * \sa     	
 */ 

void NavcCheckandWakeupdevice(handle_t hNavc, McpU8 uOpcode)
{
	gpsc_ctrl_type *p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl     *pNavc = (TNAVCD_Ctrl *) hNavc;
	switch(uOpcode)
	{
		case NAVC_CMD_INJECT_ASSISTANCE:
		case NAVC_CMD_COMPLETE_ASSISTANCE:
		case NAVC_CMD_DELETE_ASISTANCE:
		case NAVC_CMD_GET_VERSION:
		case NAVC_CMD_PLT:
			{
		gpsc_app_gps_wakeup_from_cmdhandler ();
			}

		case NAVC_CMD_REQUEST_FIX:
			{
				MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("NavcCheckandWakeupdevice: NAVC_CMD_REQUEST_FIX"));
				if(gpsc_timer_status(p_zGPSCControl, C_TIMER_AUTO_POWER_SLEEP) == GPSC_SUCCESS)
				{
					MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("NavcCheckandStopTimer: C_TIMER_AUTO_POWER_SLEEP is already running.stoping it..\n"));
					gpsc_timer_stop(p_zGPSCControl, C_TIMER_AUTO_POWER_SLEEP);
				}
			}
		break;
	}
}

/** 
 * \fn     sendCmdCompleteEvt 
 * \brief  Sends command complete indication to command requestor
 * 
 * Sends command complete event to specified destination task
 * 
 * \note
 * \param	pNavc - pointer to NAVC control object
 * \param	eResult  - result
 * \return 	void
 * \sa     	
 */ 
void NavcCmdCompleteHandler (const TNAVCD_Ctrl* pNavc, 
									  const EMcpfRes 		eResult)
{
	EMcpfRes  	eRes = RES_OK;
	TNAVC_evt* pEvt = (TNAVC_evt*) mcpf_mem_alloc_from_pool (pNavc->hMcpf, pNavc->hEvtPool);

	if (pEvt)
	{
		pEvt->eResult   = eResult;
		pEvt->eComplCmd = pNavc->eCmdInProcess;

		eRes = mcpf_SendMsg (pNavc->hMcpf, 
							 pNavc->eSrcTaskId,  
							 pNavc->uSrcQId,
							 TASK_NAV_ID,
							 NAVC_QUE_CMD_ID,
							 NAVC_EVT_CMD_COMPLETE,
							 NAVC_EVT_HEADER_SIZE,
							 0, 				/* user defined */
							 pEvt);

		if (eRes != RES_OK)
		{
			mcpf_mem_free_from_pool (pNavc->hMcpf, pEvt);
         MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("sendCmdCompleteEvt: mcpf_SendMsg failed\n"));
		}
	}
	else
	{
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("sendCmdCompleteEvt: mcpf_mem_alloc_from_pool failed\n"));
	}
}


/** 
 * \fn     navcCmd_stopCmdQueue 
 * \brief  Stop NAVC command queue
 * 
 */ 
void NavcCmdDisableQueue (handle_t hNavc)
{
	TNAVCD_Ctrl*pNavc = (TNAVCD_Ctrl*) hNavc;

	if (!pNavc->bCmdQueStopped)
	{
		mcpf_MsgqDisable (pNavc->hMcpf, TASK_NAV_ID, NAVC_QUE_CMD_ID);
		pNavc->bCmdQueStopped = MCP_TRUE;
		pNavc->hCmdQueStopTimer = mcpf_timer_start (pNavc->hMcpf, 
													CMD_QUEUE_STOP_MAX_TIME, 
													TASK_NAV_ID,
													(mcpf_timer_cb) cmdQueStopTimerExpired,
													(handle_t) pNavc);

	}
	else
	{
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("stopCmdQueue: NAVC cmd queue was already stopped\n"));
	}
}

/** 
 * \fn     navcCmd_enableCmdQueue 
 * \brief  Enable NAVC command queue
 * 
 */ 
void NavcCmdEnableQueue (handle_t hNavc)
{
	TNAVCD_Ctrl* pNavc = (TNAVCD_Ctrl*) hNavc;

	if (pNavc->bCmdQueStopped)
	{
		mcpf_timer_stop (pNavc->hMcpf, pNavc->hCmdQueStopTimer);
		pNavc->bCmdQueStopped = MCP_FALSE;
		mcpf_MsgqEnable (pNavc->hMcpf, TASK_NAV_ID, NAVC_QUE_CMD_ID);

	}
	else
	{
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("enableCmdQueue: NAVC cmd queue is not stopped\n"));
	}
}



/************************************************************************
 * Module Private Functions
 ************************************************************************/

/** 
 * \fn     sendErrorEvt 
 * \brief  Sends error indication
 * 
 * Sends error event message to specified destination task
 * 
 * \note
 * \param	pNavc - pointer to NAVC control object
 * \param  	eDestTaskId - destination task ID
 * \param  	eDestQId - destination queue ID
 * \param	uOpcode  - event opcode
 * \return 	void
 * \sa     	navcCmd_handleCommand
 */ 
static void sendErrorEvt (const TNAVCD_Ctrl *pNavc, 
						  const EmcpTaskId	eDestTaskId, 
						  const McpU8  		uDestQId, 
						  const McpU16 		uOpcode,
						  const McpU16 		uCmplOpcode)
{
	EMcpfRes  	eRes;
	TNAVC_evt * pEvt;

	pEvt = (TNAVC_evt *) mcpf_mem_alloc_from_pool (pNavc->hMcpf, pNavc->hEvtPool);

	if (pEvt)
	{
		pEvt->eResult = RES_ERROR;
		pEvt->eComplCmd = uCmplOpcode;

		eRes = mcpf_SendMsg (pNavc->hMcpf, 
							 eDestTaskId,  
							 uDestQId,
							 TASK_NAV_ID,
							 NAVC_QUE_CMD_ID,
							 uOpcode,
							 sizeof (TNAVC_evt),
							 0, 				/* user defined */
							 pEvt);
		if (eRes != RES_OK)
		{
			mcpf_mem_free_from_pool (pNavc->hMcpf, pEvt);
            MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("sendErrorEvt: mcpf_SendMsg failed\n"));
		}
	}
	else
	{
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("sendErrorEvt: mcpf_mem_alloc_from_pool failed\n"));
	}
}



/** 
 * \fn     cmdQueStopTimerExpired 
 * \brief  Queue stop time out callback function
 * 
 * Command queue timeout callback function re-enables NAVC command queue
 * 
 * \note
 * \param	hNavc - pointer to NAVC control object
 * \param	uTimerId - not used
 * \return 	void
 * \sa     	navcCmd_stopCmdQueue
 */ 
static void cmdQueStopTimerExpired (handle_t hNavc, McpUint uTimerId)
{
	TNAVCD_Ctrl *pNavc = (TNAVCD_Ctrl *) hNavc;

	MCPF_UNUSED_PARAMETER(uTimerId);
	
	if (pNavc->bCmdQueStopped)
	{
		pNavc->bCmdQueStopped = MCP_FALSE;
		mcpf_MsgqEnable (pNavc->hMcpf, TASK_NAV_ID, NAVC_QUE_CMD_ID);

		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("cmdQueStopTimerExpired: timeout error!, \n"));
	}
	else
	{
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("stopCmdQueue: timeout error, queue was not stopped!\n"));
	}
}

/** 
* \fn     sendAssistReport 
* \brief  Send Assistance Report message
* 
* Sends Assistance Report to specified destination task
* 
* \note
* \param	pNavc - pointer to NAVC control object
* \param  	pBuf  - pointer to buffer with Report
* \param  	uLen  - buffer length
* \param  	eRespOpcode - MCPF message response opcode
* \param  	eComplCmd   - MCPF command which response is sent for
* \return 	void
* \sa     	
*/ 
static void sendAssistReport (const TNAVCD_Ctrl 		*pNavc, 
							  McpU8 					*pBuf, 
							  McpU32 					uLen,
							  const ENAVC_evtOpcode 	eRespOpcode,
							  const ENAVC_cmdOpcode 	eComplCmd)
{
	EMcpfRes  	eRes = 0;
	
	TNAVC_evt* pEvt = (TNAVC_evt *) mcpf_mem_alloc_from_pool (pNavc->hMcpf, pNavc->hEvtPool);
	if (pEvt)
	{
		pEvt->eResult = RES_OK;
		pEvt->eComplCmd = eComplCmd;
		
		mcpf_mem_copy (pNavc->hMcpf, &pEvt->tParams, pBuf, uLen);
		
		eRes = mcpf_SendMsg (pNavc->hMcpf, 
			pNavc->eSrcTaskId,  
			pNavc->uSrcQId,  
			TASK_NAV_ID,
			NAVC_QUE_CMD_ID,
			(McpU16) eRespOpcode,
			uLen + NAVC_EVT_HEADER_SIZE,
			0, 				/* user defined */
			pEvt);
		
		if (eRes != RES_OK)
		{
			  mcpf_mem_free_from_pool (pNavc->hMcpf, pEvt);
           MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("sendAssistReport: mcpf_SendMsg failed\n"));
		}
	}
	else
	{
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("sendAssistReport: mcpf_mem_alloc_from_pool failed\n"));
	}
}

/** 
* \fn     sendCmdCompleteEvt 
* \brief  Sends command complete event
* 
* Sends command complete event to specified destination task
* 
* \note
* \param    pNavc - pointer to NAVC control object
* \param    eDestTaskId - destination task ID
* \param    eDestQId - destination queue ID
* \param    eResult  - result
* \return   void
* \sa
*/
void sendCmdCompleteEvt (const TNAVCD_Ctrl   *pNavc,
                                const EmcpTaskId    eDestTaskId,
                                const McpU8         uDestQId,
                                const EMcpfRes      eResult,
                                const ENAVC_cmdOpcode eComplCmd)
{
	EMcpfRes  	eRes;
	TNAVC_evt * pEvt;
	
	pEvt = (TNAVC_evt *) mcpf_mem_alloc_from_pool (pNavc->hMcpf, pNavc->hEvtPool);
	
	if (pEvt)
	{
		pEvt->eResult   = eResult;
		pEvt->eComplCmd = eComplCmd;
		
		eRes = mcpf_SendMsg (pNavc->hMcpf, 
			eDestTaskId,  
			uDestQId,
			TASK_NAV_ID,
			NAVC_QUE_CMD_ID,
			NAVC_EVT_CMD_COMPLETE,
			NAVC_EVT_HEADER_SIZE,
			0, 				/* user defined */
			pEvt);
		if (eRes != RES_OK)
		{
			mcpf_mem_free_from_pool (pNavc->hMcpf, pEvt);
            MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("sendCmdCompleteEvt: mcpf_SendMsg failed\n"));
		}
	}
	else
	{
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("sendCmdCompleteEvt: mcpf_mem_alloc_from_pool failed\n"));
	}
}

/* 
* \fn     sendAssistPriorityReport 
* \brief  Send Assistance source priority to the requested task
*
* \note
* \param	pNavc - pointer to NAVC control object
* \param  	pBuf  - pointer to buffer with Report
* \param  	uLen  - buffer length
* \param  	eRespOpcode - MCPF message response opcode
* \param  	eComplCmd   - MCPF command which response is sent for
* \return 	void
* \sa     	
*/ 
static void sendAssistPriorityReport (const TNAVCD_Ctrl 		*pNavc, 
							  McpU8 					*pBuf, 
							  McpU32 					uLen,
							  const ENAVC_evtOpcode 	eRespOpcode,
							  const ENAVC_cmdOpcode 	eComplCmd)
{
	TNAVC_evt 	*pEvt;
	EMcpfRes  	eRes;
	McpU8 index;

	UNREFERENCED_PARAMETER(*pBuf);
	UNREFERENCED_PARAMETER(uLen);
	pEvt = (TNAVC_evt *) mcpf_mem_alloc_from_pool (pNavc->hMcpf, pNavc->hEvtPool);
	if (pEvt)
	{
		pEvt->eResult = RES_OK;
		pEvt->eComplCmd = eComplCmd;
		
		//mcpf_mem_copy (pNavc->hMcpf, &pEvt->tParams, pBuf, uLen);
        
		for (index = 0; index< MAX_ASSIST_PROVIDER ; index++)
		{
		  pEvt->tParams.tAssistSrcPriorityRep[index].eAssistSrcType = pNavc->tAssistSrcDb[index].uAssistSrcType;

          pEvt->tParams.tAssistSrcPriorityRep[index].assist_src_priority = pNavc->tAssistSrcDb[index].uAssistSrcPriority;
		}
		eRes = mcpf_SendMsg (pNavc->hMcpf, 
			pNavc->eSrcTaskId,  
			pNavc->uSrcQId,  
			TASK_NAV_ID,
			NAVC_QUE_CMD_ID,
			(McpU16) eRespOpcode,
			sizeof(pEvt->tParams.tAssistSrcPriorityRep) + NAVC_EVT_HEADER_SIZE, //uLen+NAVC_EVT_HEADER_SIZE,
			0, 				/* user defined */
			pEvt);
		
		if (eRes != RES_OK)
		{
			mcpf_mem_free_from_pool (pNavc->hMcpf, pEvt);
            MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("sendAssistPriorityReport: mcpf_SendMsg failed\n"));
		}
	}
	else
	{
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("sendAssistPriorityReport: mcpf_mem_alloc_from_pool failed\n"));
	}
}
