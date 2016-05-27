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

/** \file   navc_time.c 
 *  \brief  NAVC GPS coarse and fine time management funcitons implementation
 * 
 *  \see    navc_time.h
 */

#include "gpsc_data.h"
#include "mcpf_report.h"
#include "mcpf_msg.h"
#include "navc_defs.h"
#include "navc_api.h"
#include "pla_hw.h"
#include "mcpf_services.h"
#include "mcpf_time.h"

#include "nav_log_msg.h"
#include "nav_log_cust_codes.h"

/************************************************************************
 * Defines
 ************************************************************************/

#define SECONDS_JAN1_1970_TO_JAN6_1980   315964800
#define SECONDS_IN_WEEK					 604800

/************************************************************************
 * Types
 ************************************************************************/

/************************************************************************
 * Internal functions prototypes
 ************************************************************************/

static void populateCoarseTimeAssist (const TNAVCD_Ctrl *pNavc, T_GPSC_time_assist *pTime);

static void populateFineTimeAssist (const TNAVCD_Ctrl *pNavc, T_GPSC_time_assist *pTime);

static void setCoarseTime (TNAVCD_Ctrl *pNavc, const T_GPSC_time_assist *pTime);

static void requestPulse (const TNAVCD_Ctrl *pNavc);


/************************************************************************
 *
 *   Module functions implementation
 *
 ************************************************************************/

/** 
 * \fn     navcTime_init
 * \brief  Initialize GPS time using host system timer
 * 
 */
void navcTime_init (TNAVCD_Ctrl	*pNavc, McpU32 uTimeUnc, McpBool bPulseReqEnable)
{
	McpU32  uGpsSec;
	McpU32  uMs;
	McpU16  uWeek;
	McpU32 uCurrentTimeSec;
	
	/* Get current number of seconds since Jan 1, 1970 and 
	 * calculate GPS number of seconds since Jan 6, 1980
	 */
	uCurrentTimeSec = mcpf_getCurrentTime_inSec (pNavc->hMcpf);

	if (uCurrentTimeSec < SECONDS_JAN1_1970_TO_JAN6_1980)
	{
		/* Set uWeek to invalid value */
		uWeek = 0;
	}
	else
	{
		uGpsSec = uCurrentTimeSec - SECONDS_JAN1_1970_TO_JAN6_1980;
		/* Calculate weeks elapsed */
		uWeek = (McpU16) (uGpsSec / SECONDS_IN_WEEK);
	}
	
	/* Calculate milliseconds elapsed in week */
	uMs = (uGpsSec % SECONDS_IN_WEEK) * 1000;

	pNavc->tGpsTimeEst.tGpsTime[GPSC_TIME_ACCURACY_COARSE].tTime.gps_week = uWeek;
	pNavc->tGpsTimeEst.tGpsTime[GPSC_TIME_ACCURACY_COARSE].tTime.gps_msec = uMs;
	pNavc->tGpsTimeEst.tGpsTime[GPSC_TIME_ACCURACY_COARSE].tTime.sub_ms   = 0;
	pNavc->tGpsTimeEst.tGpsTime[GPSC_TIME_ACCURACY_COARSE].tTime.time_unc = uTimeUnc;
	pNavc->tGpsTimeEst.tGpsTime[GPSC_TIME_ACCURACY_COARSE].tTime.time_accuracy = GPSC_TIME_ACCURACY_COARSE;
	pNavc->tGpsTimeEst.tGpsTime[GPSC_TIME_ACCURACY_COARSE].uSystemTimeMs= mcpf_getCurrentTime_InMilliSec(pNavc->hMcpf);
	pNavc->tGpsTimeEst.tGpsTime[GPSC_TIME_ACCURACY_COARSE].bValid   	= MCP_TRUE;

	MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcTime_init: gpsweek=%d,gpsmsec = %u, GPSSec = %u\n", uWeek,uMs,uGpsSec));

	pNavc->tGpsTimeEst.bPulseReqEnable = bPulseReqEnable;
}

/** 
 * \fn     navcTime_setGpsTime
 * \brief  Set GPS Time of NAVC object
 * 
 */
void navcTime_setGpsTime (TNAVCD_Ctrl 		*pNavc, 
						  TNAVCD_GpsTime 	*pGpsTime,
						  ENAVCD_TimeMode 	eTimeMode)
{

	if (eTimeMode < NAVCD_TIME_MODE_MAX_NUM)
	{
		pNavc->tGpsTimeEst.tGpsTime[eTimeMode] = *pGpsTime;

		pNavc->tGpsTimeEst.tGpsTime[eTimeMode].bValid = MCP_TRUE;
		
		SESSLOGMSG("[%s]%s:%d, %d,%d %s \n",get_utc_time(),SESS_EPH_AIDING,nav_sess_getCount(0),pGpsTime->tTime.gps_msec,pGpsTime->tTime.gps_week,"#TOW Aiding to GPS ms,week");
	}
	else
	{
        MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("navcTime_setGpsTime: invalid time mode=%u\n", eTimeMode));
	}
}



/** 
 * \fn     gpsc_time_request_pulse_ind 
 * \brief  Request time pulse generation
 * 
 * \note    
 * \param	void
 * \return 	result of operation: success or failure
 * \sa     	
 */
T_GPSC_result gpsc_time_request_pulse_ind ()
{
	/* Add platform dependent support for pulse generation */

	return GPSC_TIME_PULSE_GENERATION_FAIL;
}

/** 
 * \fn     gpsc_time_request_tow_ind 
 * \brief  Request time of week
 * 
 */

T_GPSC_result gpsc_time_request_tow_ind (T_GPSC_time_assist   *pTime)
{
   	gpsc_ctrl_type	*p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl		*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl;
	T_GPSC_result	result = GPSC_TIME_PULSE_GENERATION_FAIL;

	switch (pTime->time_accuracy)
	{
	case GPSC_TIME_ACCURACY_COARSE:
		populateCoarseTimeAssist (pNavc, pTime);
		result = GPSC_SUCCESS;
		break;

	case GPSC_TIME_ACCURACY_FINE_LAST_PULSE:

		if (pNavc->tGpsTimeEst.tGpsTime[GPSC_TIME_ACCURACY_FINE_LAST_PULSE].bValid)
		{
			populateFineTimeAssist (pNavc, pTime);
			pNavc->tGpsTimeEst.tGpsTime[GPSC_TIME_ACCURACY_FINE_LAST_PULSE].bValid = MCP_FALSE;
			result = GPSC_SUCCESS;
		}
		else
		{
			if (pNavc->tGpsTimeEst.bPulseReqEnable)
			{
				requestPulse (pNavc);
				result = GPSC_TIME_PULSE_GENERATION_PENDING;
			}
			else
			{
				populateCoarseTimeAssist (pNavc, pTime);
			}
		}
		break;

	default:
        MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, 
						  ("gpsc_time_request_tow_ind: invalid time mode=%u\n", pTime->time_accuracy));
		break;
	}

	return result;
}


/** 
 * \fn     gpsc_time_report_tow_ind 
 * \brief  Indicate time of week
 * 
 */
T_GPSC_result gpsc_time_report_tow_ind (T_GPSC_time_accuracy time_accuracy, T_GPSC_time_assist *pTime)
{
    gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl	*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 

	if (time_accuracy == GPSC_TIME_ACCURACY_COARSE)
	{
		/* Set coarse time in NAVC if GPS time uncertainty is less than of NAVC one */
		if ((pTime->time_unc < pNavc->tGpsTimeEst.tGpsTime[GPSC_TIME_ACCURACY_COARSE].tTime.time_unc) ||
			!pNavc->tGpsTimeEst.tGpsTime[GPSC_TIME_ACCURACY_COARSE].bValid)
		{
			setCoarseTime (pNavc, pTime);
		}
	}

    return TRUE;
}


/** 
 * \fn     populateCoarseTimeAssist 
 * \brief  Populate GPS coarse time assistance structure
 * 
 * Sets GPS time assistance structure from NAVC coarse available time information
 * 
 * \note
 * \param	pNavc - pointer to NAVC control object
 * \param  	pTime - pointer to GPS time structure
 * \return 	void
 * \sa     	
 */ 

static void populateCoarseTimeAssist (const TNAVCD_Ctrl *pNavc, T_GPSC_time_assist *pTime)
{

	McpU32 	uCurrMs, deltaMs;
	
	uCurrMs = mcpf_getCurrentTime_InMilliSec(pNavc->hMcpf);
	deltaMs = uCurrMs - pNavc->tGpsTimeEst.tGpsTime[GPSC_TIME_ACCURACY_COARSE].uSystemTimeMs;

	*pTime = pNavc->tGpsTimeEst.tGpsTime[GPSC_TIME_ACCURACY_COARSE].tTime;

	pTime->gps_msec += deltaMs;
	pTime->sub_ms 	= 0;
	pTime->time_accuracy = GPSC_TIME_ACCURACY_COARSE;
}

/** 
 * \fn     populateFineTimeAssist 
 * \brief  Populate GPS fine (pulse) time assistance structure
 * 
 * Sets GPS time assistance structure from NAVC fine (pulse) available time information
 * 
 * \note
 * \param	pNavc - pointer to NAVC control object
 * \param  	pTime - pointer to GPS time structure
 * \return 	void
 * \sa     	
 */ 

static void populateFineTimeAssist (const TNAVCD_Ctrl *pNavc, T_GPSC_time_assist *pTime)
{
	*pTime = pNavc->tGpsTimeEst.tGpsTime[GPSC_TIME_ACCURACY_FINE_LAST_PULSE].tTime;
	pTime->time_accuracy = GPSC_TIME_ACCURACY_FINE_LAST_PULSE;	
}

/** 
 * \fn     setCoarseTime 
 * \brief  Populate NAVC coarse time structure
 * 
 * Sets NAVC coarse time from GPS time
 * 
 * \note
 * \param	pNavc - pointer to NAVC control object
 * \param  	pTime - pointer to GPS reported time
 * \return 	void
 * \sa     	
 */ 
static void setCoarseTime (TNAVCD_Ctrl *pNavc, const T_GPSC_time_assist *pTime)
{
	pNavc->tGpsTimeEst.tGpsTime[GPSC_TIME_ACCURACY_COARSE].tTime = *pTime;

	pNavc->tGpsTimeEst.tGpsTime[GPSC_TIME_ACCURACY_COARSE].tTime.sub_ms = 0;
	pNavc->tGpsTimeEst.tGpsTime[GPSC_TIME_ACCURACY_COARSE].tTime.time_accuracy = GPSC_TIME_ACCURACY_COARSE;
	pNavc->tGpsTimeEst.tGpsTime[GPSC_TIME_ACCURACY_COARSE].uSystemTimeMs = mcpf_getCurrentTime_InMilliSec(pNavc->hMcpf);
	pNavc->tGpsTimeEst.tGpsTime[GPSC_TIME_ACCURACY_COARSE].bValid   = MCP_TRUE;
}



/** 
 * \fn     requestPulse 
 * \brief  Send request pulse event to assistance task (modem)
 * 
 * Send request pulse event to assistance task (modem) if assistance task id is known,
 * meaning that any assistance message was received from this task
 * 
 * \note
 * \param	pNavc - pointer to NAVC control object
 * \return 	void
 * \sa     	
 */ 
static void requestPulse (const TNAVCD_Ctrl *pNavc)
{
	EMcpfRes  	eRes;

	/* Request pulse from assistance task, if assistance task id is known */

	if (pNavc->eAssistSrcTaskId)
	{
		eRes = mcpf_SendMsg (pNavc->hMcpf, 
							 pNavc->eAssistSrcTaskId,  
							 pNavc->uAssistSrcQId,  
							 TASK_NAV_ID,
							 NAVC_QUE_CMD_ID,
							 NAVC_EVT_REQUEST_PULSE,
							 0,                 	/* data len     */
							 0, 					/* user defined */
							 NULL);

		if (eRes != RES_OK)
		{
			MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("requestPulse: mcpf_SendMsg failed\n"));
		}
	}
}

