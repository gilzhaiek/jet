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

/** \file   navc_internalEvtHandler.c 
 *  \brief  NAVC stack internal events handler implementation
 * 
 *  \see    navc_internalEvtHandler.h, navc_main.c
 */

#include "mcpf_mem.h"
#include "mcpf_services.h"
#include "mcpf_report.h"
#include "mcpf_msg.h"
#include "mcp_hal_os.h"
#include "navc_defs.h"
#include "gpsc_data.h"
#include "navc_api.h"
#include "navc_internalEvtHandler.h"
#include "gpsc_app_api.h"
#include "navc_sm.h"
#include "navc_inavc_if.h"
#include "navc_cmdHandler.h"

#include "gpsc_sequence.h"

#include <utils/Log.h>
#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG "NAVD.NAVC_INTERNALEVTHANDLER"    // our identification for logcat (adb logcat NAVD.NAVC_INTERNALEVTHANDLER:V *:S)

/************************************************************************
 * Defines
 ************************************************************************/


/************************************************************************
 * Types
 ************************************************************************/



/************************************************************************
 * Internal functions prototypes
 ************************************************************************/

#ifdef TIMER_HACK
static void evTimerExprHndlr (const TNAVCD_Ctrl* pNavc, const TmcpfMsg* pMsg);
#endif



/************************************************************************
 *
 *   Module functions implementation
 *
 ************************************************************************/

/** 
 * \fn     NavcEvtHandler 
 * \brief  Process received NAVC internal event
 * 
 */
void NavcEvtHandler (handle_t hNavc, TmcpfMsg* pMsg)
{
    TNAVCD_Ctrl* pNavc = (TNAVCD_Ctrl*) hNavc;
	 gpsc_ctrl_type* p_zGPSCControl = gp_zGPSCControl;

    switch (pMsg->uOpcode)
    {
	case NAVC_EVT_GPSC_IN_ACTIVE_STATE:
		{
			NavcCmdEnableQueue (pNavc);
		}
		break;

   // Process Recieved AI2 Packet. 
	case NAVCD_EVT_RECV_RX_IND:
		{
			gpsc_drv_receive_data_req(pMsg->pData, (McpU16)pMsg->uLen);
		}
		break;
	
	case NAVCD_EVT_TX_COMPL_IND:
		{
			gpsc_drv_transmit_data_res (NULL, NULL);
		}
		break;

	case NAVCD_EVT_CCM_COMPL_IND:
		NavcSm(pNavc->hNavcSm, E_NAVC_EV_COMPLETE);
		break;

	case NAVCD_EVT_DISCRETE_CTRL: /* TODO - delete this case */
		gpsc_drv_discrete_control_res(RES_OK); 


	case NAVCD_EVT_TIMEPULSE_REFCLK_CTRL:
	{
		/* Apply the refclock to GPS receiver */
		gpsc_drv_apply_calib_timepulse_refclk_res (GPSC_SUCCESS, (McpU8) (pMsg->uUserDefined));

	}		
	break;

	case NAVCD_EVT_TIMEPULSE_REFCLK_SENSOR_REQ:
	{
		gpsc_drv_sensor_calib_req_timepulse_refclk_res(pMsg->uUserDefined);
	}
	break;

	case NAVC_EVT_SUPLC_SESSION_RESULT:
	{
		TNAVC_cmdParams* pCmdParams = (TNAVC_cmdParams*) pMsg->pData;
		gpsc_app_stop_nw_session_indication();
		
		if ((pCmdParams->tSuplSessionSuccess == TRUE) &&
			(p_zGPSCControl->p_zSuplStatus.u_GetAssistSUPLRequestPending == FALSE))
		{
			/* Reset the flag to allow further requests. */			
			p_zGPSCControl->p_zSuplStatus.u_GetAssistSUPLRequest = FALSE;

			/* Reset the SUPL request count */
			p_zGPSCControl->p_zSuplStatus.u_SessRequestCount = 0;
		}
		else
		{
			if (E_GPSC_STATE_ACTIVE == p_zGPSCControl->p_zGpscSm->e_GpscCurrState)			
			{
				if (p_zGPSCControl->p_zSuplStatus.u_SessRequestCount < 
								p_zGPSCControl->p_zSessConfig->w_TotalNumSession)
				{
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
		}
	}
	break;	

#ifdef ENABLE_INAV_ASSIST
	case NAVCD_EVT_INJECT_SENSORASSIST_MSG:
	{
		gpsc_app_inject_sensor_assistance(pMsg->pData);
	}
	break;


	case NAVCD_EVT_INJECT_APMCONFIG_MSG:
	{
		gpsc_app_inject_apm_config(pMsg->pData);
	}
	break;

#endif

#ifdef TIMER_HACK
    case NAVCD_EVT_TIMER_EVT:
        evTimerExprHndlr (pNavc, pMsg);
        break;
#endif

	default:
		/* Error unknown event id */
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG,("ERROR: UNKNOWN NAVC EVENT #: %d", pMsg->uOpcode));
      break;
    }

	/* Free received msg and data buffer */
	if (pMsg->pData)
	{
		mcpf_mem_free_from_pool (pNavc->hMcpf, pMsg->pData);
      pMsg->pData = NULL;
	}
	mcpf_mem_free_from_pool (pNavc->hMcpf, (void*)pMsg);
}

/************************************************************************
 * Module Private Functions
 ************************************************************************/
#ifdef TIMER_HACK
static void evTimerExprHndlr (const TNAVCD_Ctrl * pNavc, const TmcpfMsg * pMsg)
{
    UNREFERENCED_PARAMETER(pNavc);
    UNREFERENCED_PARAMETER(pMsg);
    gpsc_os_timer_expired_res(C_TIMER_SEQUENCE);
}
#endif

























