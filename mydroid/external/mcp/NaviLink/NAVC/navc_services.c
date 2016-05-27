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

/** \file   navc_services.c 
 *  \brief  NAVC stack service functions implementation:
 * 			- timer services
 * 			- error report
 * 
 *  \see    navc_cmdHandler.c, navc_outEvt.c, navc_platform.c
 */

#include "gpsc_data.h"
#include "mcpf_services.h"
#include "mcpf_time.h"
#include "mcpf_report.h"
#include "mcpf_msg.h"
#include "navc_defs.h"
#include "navc_api.h"

#include <utils/Log.h>
#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG "NAVD.NAVC_SERVICES"    // our identification for logcat (adb logcat NAVD.NAVC_SERVICES:V *:S)


/************************************************************************
 * Defines
 ************************************************************************/

/************************************************************************
 * Types
 ************************************************************************/

/************************************************************************
 * Internal functions prototypes
 ************************************************************************/

/************************************************************************
 *
 *   Module functions implementation
 *
 ************************************************************************/


/** 
 * \fn     gpsc_drv_apply_calib_timepulse_refclk_ind 
 * \brief  Request to provide the reference clock to sensor
 *         
 * \note    Used for config 4
 * \param	enable_disable_calib - enable (1) or disable (0) output of reference clock/time pulse
 * \return 	result of operation: success or failure
 * \sa     	
 */
T_GPSC_result gpsc_drv_apply_calib_timepulse_refclk_ind (U8 enable_disable_calib)
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl	*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 
	T_GPSC_result	res;
	EMcpfRes  		eRes;

	eRes = mcpf_hwRefClkSet(pNavc->hMcpf, enable_disable_calib);
	
	if (eRes == RES_OK)
	{
		eRes = mcpf_SendMsg (pNavc->hMcpf, 
			                 TASK_NAV_ID,  
				             NAVC_QUE_INTERNAL_EVT_ID,
					         TASK_NAV_ID,
						     NAVC_QUE_CMD_ID,
							 NAVCD_EVT_TIMEPULSE_REFCLK_CTRL,
							0,                               /* data size    */
							(McpU32) enable_disable_calib,  /* user defined */
							NULL);
		if (eRes == RES_OK)
		{
			res = GPSC_SUCCESS;
		}
		else
		{
			MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_drv_apply_calib_timepulse_refclk_ind: mcpf_SendMsg failed\n"));
			res = GPSC_FAIL;
		}
	}
	else
	{
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_drv_apply_calib_timepulse_refclk_ind: hw_refClk_set failed\n"));
		res = GPSC_FAIL;
	}
	return res;
}

 /** 
 * \fn     gpsc_calib_timepulse_refclk_sensor_req 
 * \brief  Request to provide the reference clock to sensor
 *         
 * \note    Used for config 4
 * \param	enable_disable_calib - enable (1) or disable (0) output of reference clock/time pulse
 * \return 	result of operation: success or failure
 * \sa     	
 */
T_GPSC_result gpsc_calib_timepulse_refclk_sensor_req (U8 enable_disable_calib)
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl	*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 
	T_GPSC_result	res;
	EMcpfRes  		eRes;

	MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_calib_timepulse_refclk_sensor_req: Req:%d", enable_disable_calib));

	eRes = mcpf_hwRefClkSet(pNavc->hMcpf, enable_disable_calib);
	
	if (eRes == RES_OK)
	{
		eRes = mcpf_SendMsg (pNavc->hMcpf, 
			                 TASK_NAV_ID,  
				             NAVC_QUE_INTERNAL_EVT_ID,
					         TASK_NAV_ID,
						     NAVC_QUE_CMD_ID,
							 NAVCD_EVT_TIMEPULSE_REFCLK_SENSOR_REQ,
							0,                               /* data size    */
							(McpU32) enable_disable_calib,  /* user defined */
							NULL);
		if (eRes == RES_OK)
		{
			res = GPSC_SUCCESS;
		}
		else
		{
			MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_calib_timepulse_refclk_sensor_req: mcpf_SendMsg failed\n"));
			res = GPSC_FAIL;
		}
	}
	else
	{
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("gpsc_calib_timepulse_refclk_sensor_req: hw_refClk_set failed\n"));
		res = GPSC_FAIL;
	}
	return res;
}
 

 /** 
 * \fn     gpsc_os_timer_expired_cb 
 * \brief  Start timer
 *         
 * \note    
 * \param	u_TimerId 	 - GPSC timer ID that expired
 * \return 	result of operation: success or failure
 * \sa     	gpsc_os_timer_expired_cb
*/
T_GPSC_result gpsc_os_timer_expired_cb (U32 u_TimerId, McpUint uUnused)
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl	*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 

	MCPF_UNUSED_PARAMETER(uUnused);
	if (pNavc->hTimers[u_TimerId] != NULL)
	{
		pNavc->hTimers[u_TimerId] = NULL;
		return gpsc_os_timer_expired_res(u_TimerId);
	}
	else
	{
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, 
			("gpsc_os_timer_expired_cb: invalid timer ID=%u expired\n", (McpU32) u_TimerId));
		return GPSC_FAIL;
	}
}

/** 
 * \fn     gpsc_os_timer_start_ind 
 * \brief  Start timer
 *         
 * \note    
 * \param	uTimerId 	 - GPSC timer ID to start
 * \param	uExpiryTime  - timer duration (ms)
 * \return 	result of operation: success or failure
 * \sa     	gpsc_os_timer_start_ind
 */
T_GPSC_result gpsc_os_timer_start_ind (U8 uTimerId, U32 uExpiryTime)
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl	*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 
	T_GPSC_result   res = GPSC_FAIL;

	if (uTimerId < NAVCD_TIMER_MAX_NUM)
	{
		pNavc->hTimers[uTimerId] = mcpf_timer_start (pNavc->hMcpf, 
													 uExpiryTime, 
													 TASK_NAV_ID,
													 (mcpf_timer_cb) gpsc_os_timer_expired_cb,
													 (handle_t) ((McpU32) uTimerId));

		if (pNavc->hTimers[uTimerId] != NULL)
        res = GPSC_SUCCESS;

      else
        ALOGE("+++ %s: Failed to start Timer [id = %d] +++\n", __FUNCTION__, uTimerId);
	}
	else
	   ALOGE("+++ %s: Invalid timer ID [%d] +++\n", __FUNCTION__, uTimerId);

    return res;
}

/** 
 * \fn     gpsc_os_timer_stop_ind 
 * \brief  Stop timer
 *         
 * \note    
 * \param	uTimerId 	 - GPSC timer ID or all timers (0) to stop 
 * \param	uExpiryTtime - timer duration (ms)
 * \return 	result of operation: success or failure
 * \sa     	gpsc_os_timer_stop_ind
 */
T_GPSC_result gpsc_os_timer_stop_ind (U8 uTimerId)
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl	*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 
	T_GPSC_result   res = GPSC_FAIL;
	EMcpfRes    	eRes;

	if (uTimerId < NAVCD_TIMER_MAX_NUM)
	{
		if (uTimerId != C_TIMER_ALL)
		{
			if (pNavc->hTimers[uTimerId])
			{
				eRes = mcpf_timer_stop (pNavc->hMcpf, pNavc->hTimers[uTimerId]);
				if (eRes == RES_OK)
				{
                pNavc->hTimers[uTimerId] = NULL;
					 res = GPSC_SUCCESS;
				}
				else
				{
					MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, 
									  ("gpsc_os_timer_stop_ind: error timerId=%u hTimer=%p\n",
									   uTimerId, pNavc->hTimers[uTimerId]));
				}
			}
			else
			{
				MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, 
								  ("gpsc_os_timer_stop_ind: timer handle NULL for ID=%u\n",uTimerId));
			}
		}
		else
		{
			McpU32 		uIndx;

			/* stop all active timers */
			for (uIndx=0; uIndx < NAVCD_TIMER_MAX_NUM; uIndx++)
			{
				if (pNavc->hTimers[uIndx])
				{
					eRes = mcpf_timer_stop (pNavc->hMcpf, pNavc->hTimers[uIndx]);
					if (eRes != RES_OK)
					{
						MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, 
										  ("gpsc_os_timer_stop_ind-all: error timerId=%u hTimer=%p\n",
										   uIndx, pNavc->hTimers[uIndx]));
						return GPSC_FAIL; 
					}
					else
					{
						pNavc->hTimers[uIndx] = NULL;
						res = GPSC_SUCCESS;
					}
				}
			}
			res = GPSC_SUCCESS;
		}
	}
	else
	{
       ALOGE("+++ %s: Invalid Timer ID [%d] +++\n", __FUNCTION__, uTimerId);
	}
    return res;
}

/** 
 * \fn     gpsc_os_fatal_error_ind 
 * \brief  Fatal error function
 *         
 * \note    
 * \param	eErrCode - error code 
 * \return 	void
 * \sa     	
 */
void gpsc_os_fatal_error_ind (T_GPSC_result eErrCode)
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	TNAVCD_Ctrl	*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 

	MCPF_REPORT_FATAL_ERROR( pNavc->hMcpf, NAVC_MODULE_LOG, ("Fatal error code=%d\n", eErrCode)); 
}



