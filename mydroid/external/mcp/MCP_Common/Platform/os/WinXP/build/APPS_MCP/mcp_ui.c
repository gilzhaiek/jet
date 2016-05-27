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


/** \file   mcp_ui.c 
 *  \brief  User Interface Thread                                  
 * 
  */


/************************************************************************
 * Includes
 ************************************************************************/
#include <time.h>
#include <stdio.h>
#include "mcp_ui.h"
#include "mcpf_services.h"
#include "mcpf_mem.h"
#include "mcpf_main.h"
#include "mcpf_time.h"
#include "mcpf_msg.h"
#include "mcpf_report.h"
#ifdef _NAVC_APP
#include "gpsc_types.h"
#include "navc_api.h"


/************************************************************************
 * External Functions Definitions
 ************************************************************************/
extern void _NAVC_APP_Report(const char *format,...);

extern void NAVCApp_UpdateState(char *stateMsg);

/************************************************************************
 * Definitions
 ************************************************************************/
#define UI_POOL_SIZE	10
#define AUTONOMOUS_SESSION  254

/************************************************************************
 * Global Variables
 ************************************************************************/
handle_t g_hMcpf;
handle_t g_hUiPool;

/************************************************************************
 * APIs
 ************************************************************************/

/** 
 * \fn     ui_init 
 * \brief  Init UI
 * 
 * Init UI global variable & Thread
 * 
 * \note
 * \param	hMcpf  - MCPF Handler
 * \return 	None
 * \sa     	ui_init
 */ 
void ui_init(handle_t hMcpf)
{
	g_hMcpf = hMcpf;
	mcpf_CreateTask(hMcpf, TASK_UI_ID, "UI", 1, NULL, NULL);
	mcpf_RegisterTaskQ(hMcpf, TASK_UI_ID, 1, ui_handleEvent, hMcpf);
	g_hUiPool = mcpf_memory_pool_create(hMcpf, sizeof(TNAVC_reqLocFix), UI_POOL_SIZE);
	mcpf_MsgqEnable(hMcpf, TASK_UI_ID, 1);
}

/** 
 * \fn     ui_destroy 
 * \brief  Destroy UI 
 * 
 * Destroy UI global variable & Thread
 * 
 * \note
 * \return 	None
 * \sa     	ui_destroy
 */ 
void ui_destroy()
{
	mcpf_memory_pool_destroy(g_hMcpf, g_hUiPool);
	mcpf_DestroyTask(g_hMcpf, TASK_UI_ID);
	mcpf_UnregisterTaskQ(g_hMcpf, TASK_UI_ID, 1);
}

/** 
 * \fn     ui_sendNavcCmd 
 * \brief  Build & Send command to NAVC
 * 
 * Build & Send command to NAVC
 * 
 * \note
 * \param	uOpCode  - Command's operation code
 * \return 	None
 * \sa     	ui_sendNavcCmd
 */ 
void ui_sendNavcCmd(McpU16 uOpCode)
{
	McpU16	uLen = 0;
	void	*pData = NULL;
	McpU32	current_time;
	McpU8	uDestQueId;
	TNAVC_reqLocFix	*pReqLocFix;
				

	if(uOpCode == NAVC_CMD_REQUEST_FIX)
	{
		/* Build Data to send */
		pReqLocFix = (TNAVC_reqLocFix	*)mcpf_mem_alloc_from_pool(g_hMcpf, g_hUiPool);

		pReqLocFix->loc_fix_session_id = AUTONOMOUS_SESSION;
		pReqLocFix->loc_fix_mode = GPSC_FIXMODE_AUTONOMOUS;
#ifdef GPSM_NMEA_FILE
		pReqLocFix->loc_fix_result_type_bitmap = (GPSC_RESULT_PROT | NMEA_MASK);
#else
		pReqLocFix->loc_fix_result_type_bitmap = GPSC_RESULT_PROT;
#endif /* #ifdef GPSM_NMEA_FILE */
		pReqLocFix->loc_fix_max_ttff = 0;
		 pReqLocFix->loc_fix_period = 0; 
	  	pReqLocFix->loc_fix_num_reports =1;

		pData = (void *)pReqLocFix;
		uLen = sizeof(TNAVC_reqLocFix);
		uDestQueId = NAVC_QUE_CMD_ID;

		current_time = mcpf_getCurrentTime_inSec(g_hMcpf);
		_NAVC_APP_Report("\n%s Sending Location Request\n",ctime((time_t *)&current_time));

	}
	else
	{
		uDestQueId = NAVC_QUE_ACTION_ID;
	}

	mcpf_SendMsg(g_hMcpf, TASK_NAV_ID, uDestQueId, TASK_UI_ID, 1, uOpCode, uLen, 0, pData);
}

/** 
 * \fn     ui_handleEvent 
 * \brief  Handle UI Events
 * 
 * Handle UI Events
 * 
 * \note
 * \param	hMcpf  - MCPF Handler
 * \param	pMsg  - pointer to MCPF message containing UI Event
 * \return 	status of operation: OK or Error
 * \sa     	ui_handleEvent
 */ 
EMcpfRes ui_handleEvent (handle_t hMcpf, TmcpfMsg *pMsg)
{
	EMcpfRes ret_val;

	switch (pMsg->uOpcode)
    {
		case NAVC_EVT_CMD_COMPLETE:
			{
				TNAVC_evt *pEvt = (TNAVC_evt *)(pMsg->pData);
				if(pEvt->eComplCmd == NAVC_CMD_START) 
				{
					if(pEvt->eResult == RES_OK)
						NAVCApp_UpdateState("ON");
					else
						_NAVC_APP_Report("NAVC_CMD_START FAILED!");
				}
				else if(pEvt->eComplCmd == NAVC_CMD_STOP) 
				{
					if(pEvt->eResult == RES_OK)
						NAVCApp_UpdateState("OFF");
					else
						_NAVC_APP_Report("NAVC_CMD_STOP FAILED!");
				}
			}
			break;

		case NAVC_EVT_POSITION_FIX_REPORT:
			{
				TNAVC_evt	*pEvt = pMsg->pData;
				T_GPSC_loc_fix_response *loc_fix_response = &pEvt->tParams.tLocFixReport;
				T_GPSC_loc_fix_response_union *p_LocFixRespUnion = &loc_fix_response->loc_fix_response_union;
				T_GPSC_prot_position *p_RrlpPosition = &p_LocFixRespUnion->prot_position;
				T_GPSC_ellip_alt_unc_ellip *p_RrlpEllipInfo = &p_RrlpPosition->ellip_alt_unc_ellip;
				McpU32	current_time;
				FLT f_Latitude;
				FLT f_Longitude;

				if(pEvt->eResult != RES_OK) 
				{
					ret_val = RES_ERROR;
					_NAVC_APP_Report("NAVC_EVT_POSITION_FIX_REPORT FAILED!");
					break;
				}
				
				current_time = mcpf_getCurrentTime_inSec(g_hMcpf);
				switch(p_RrlpPosition->prot_fix_result)
				{
					
#define TWO_TO_23	8388608.0F
#define TWO_TO_24	16777216.0F
#define LAT_SCALE (TWO_TO_23 / 90.0F)
#define LONG_SCALE (TWO_TO_24 / 360.0F)
					
				case GPSC_PROT_FIXRESULT_NOFIX:
					_NAVC_APP_Report("\n%s[LR FIX] -> Fix Timeout\n",ctime((time_t *)&current_time));
					break;
				case GPSC_PROT_FIXRESULT_2D:
					{
						_NAVC_APP_Report("\n%s[LR FIX] -> 2D Fix Obtained\n",ctime((time_t *)&current_time));
						
						f_Latitude = (FLT) ((DBL)p_RrlpEllipInfo->latitude / LAT_SCALE);
						f_Longitude = (FLT) ((DBL) p_RrlpEllipInfo->longitude / LONG_SCALE);
						
						_NAVC_APP_Report("[POSITION]");
						_NAVC_APP_Report("[Lat %3.06f%s Deg][Lon %3.06f Deg]\n",
							f_Latitude,
							p_RrlpEllipInfo->latitude_sign?"S":"N",
							f_Longitude);
					}
					break;
				case GPSC_PROT_FIXRESULT_3D:
					{	
						_NAVC_APP_Report("\n%s[LR FIX] -> 3D Fix Obtained\n",ctime((time_t *)&current_time));
						
						f_Latitude = (FLT) ((DBL)p_RrlpEllipInfo->latitude / LAT_SCALE);
						f_Longitude = (FLT) ((DBL) p_RrlpEllipInfo->longitude / LONG_SCALE);
						
						_NAVC_APP_Report("[POSITION]");
						_NAVC_APP_Report("[Lat %3.06f%s Deg][Lon %3.06f Deg]\n",
							f_Latitude,
							p_RrlpEllipInfo->latitude_sign?"S":"N",
							f_Longitude);
						_NAVC_APP_Report("[ALTITUDE]");
						_NAVC_APP_Report("[%s%d]\n",
							p_RrlpEllipInfo->altitude_sign?"-":"+",
							p_RrlpEllipInfo->altitude);
					}
					break;
				default :
					_NAVC_APP_Report("[LR FIX] -> Error! Invalid Standalone Fix specified\n");
					break;
				}
			}
			break;

		default:
			MCPF_REPORT_ERROR(hMcpf, UI_MODULE_LOG, ("ui_handleEvent: Unknown Event received\n"));
			ret_val = RES_ERROR;
			break;

	}

	/* Free received msg and data buffer */
	if (pMsg->pData)
	{
		mcpf_mem_free_from_pool (hMcpf, pMsg->pData);
	}
	mcpf_mem_free_from_pool (hMcpf, (void *) pMsg);

	return ret_val;
}
#endif