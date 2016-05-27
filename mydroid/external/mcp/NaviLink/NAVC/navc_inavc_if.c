/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   navc_inav_if.c
 *
 * Description      :   All interface functions between NAVC<->INAVC are
 *						implemented here. For ease of integration.
 *
 * Author           :   A.N.Naveen
 * Date             :   16 Dec 2010
 *
 ******************************************************************************
 */

/************************************************************************
 * Header Files
 ************************************************************************/
#include <math.h>
#include <utils/Log.h>
#include <sys/time.h>
#ifdef ENABLE_INAV_ASSIST
#include "inavc_ext.h"
#endif
#include "navc_inavc_if.h"
#include "mcpf_msg.h"
#include "navc_api.h"
#include "navc_defs.h"

#ifdef ENABLE_INAV_ASSIST
/************************************************************************
 * Defines
 ************************************************************************/
#define TRACE_LOG(...) LOGD(__VA_ARGS__)

#define C_PACKET_SIZE_INJ_SEDATA 40
#define C_PACKET_SIZE_INJ_APMCONFIG 8

/************************************************************************
 * Types
 ************************************************************************/
extern TNAVCD_Ctrl *pNavcCtrl;

#ifdef UNIFIED_TOOLKIT_COMM
McpS32 clientsockfd = 0;
#endif


/************************************************************************
 * Internal functions prototypes
 ************************************************************************/

/**
 * \fn     navc_notify_cb
 * \brief  This routine is registered with INAVC to be invoked to send
 *         msg to the Rx.
 */
McpS32 navc_notify_cb(handle_t h_reg_cb, McpU32 event, void *param)
{
	EMcpfRes result;
	struct timeval curr_time;

	gettimeofday(&curr_time, NULL);

	switch(event)
	{
		case EVENT_UPDATE_SEDATA:
		{
			TRACE_LOG("navc_notify_cb (%ld:%ld): EVENT_UPDATE_SEDATA",
									curr_time.tv_sec, curr_time.tv_usec);

			result = mcpf_SendPriorityMsg (h_reg_cb,
						 		TASK_NAV_ID,
						 		NAVC_QUE_INTERNAL_EVT_ID,
						 		TASK_NAV_ID,
						 		NAVC_QUE_INTERNAL_EVT_ID,
						 		NAVCD_EVT_INJECT_SENSORASSIST_MSG,
						 		0,
						 		0, 			  /* user defined */
						 		param);
			if (result != RES_OK)
			{
				TRACE_LOG("navc_notify_cb: mcpf_SendPriorityMsg failed\n");
			}
		}
		break;

		case EVENT_PROPOGATE_FIX:
		{
			TRACE_LOG("navc_notify_cb (%ld:%ld): EVENT_PROPOGATE_FIX",
			                        curr_time.tv_sec, curr_time.tv_usec);
		}
		break;

		case EVENT_SET_APMCONFIG:
		{
			TRACE_LOG("navc_notify_cb (%ld:%ld): EVENT_SET_APMCONFIG",
			                        curr_time.tv_sec, curr_time.tv_usec);

			result = mcpf_SendPriorityMsg (h_reg_cb,
						 		TASK_NAV_ID,
						 		NAVC_QUE_INTERNAL_EVT_ID,
						 		TASK_NAV_ID,
						 		NAVC_QUE_INTERNAL_EVT_ID,
						 		NAVCD_EVT_INJECT_APMCONFIG_MSG,
						 		0,
						 		0, 			  /* user defined */
						 		param);
			if (result != RES_OK)
			{
				TRACE_LOG("navc_notify_cb: mcpf_SendPriorityMsg failed\n");
			}
		}
		break;

		case EVENT_RESET_APMCONFIG:
		{
			TRACE_LOG("navc_notify_cb (%ld:%ld): EVENT_RESET_APMCONFIG",
			                        curr_time.tv_sec, curr_time.tv_usec);

			result = mcpf_SendPriorityMsg (h_reg_cb,
						 		TASK_NAV_ID,
						 		NAVC_QUE_INTERNAL_EVT_ID,
						 		TASK_NAV_ID,
						 		NAVC_QUE_INTERNAL_EVT_ID,
						 		NAVCD_EVT_INJECT_APMCONFIG_MSG,
						 		0,
						 		0, 			  /* user defined */
						 		param);
			if (result != RES_OK)
			{
				TRACE_LOG("navc_notify_cb: mcpf_SendPriorityMsg failed\n");
			}
		}
		break;

		default:
		{
			TRACE_LOG("navc_notify_cb (%ld:%ld): Unexpected event received",
			                        curr_time.tv_sec, curr_time.tv_usec);
		}
		break;
	}

	return 0;

}

/**
 * \fn     navc_set_inavc_hndl
 * \brief  This routine used to store the INAVC handle.
 */
 void navc_set_inavc_hndl(handle_t h_inavc)
 {
	pNavcCtrl->hInavc = h_inavc;

#ifdef UNIFIED_TOOLKIT_COMM
	clientsockfd = inavc_get_clientsockfd(pNavcCtrl->hInavc);
#endif
 }
#endif