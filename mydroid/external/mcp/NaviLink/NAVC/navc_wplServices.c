/*********************************************************************************

                                   TI GPS Confidential

*********************************************************************************/

/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName			:	navc_wplServices.c
 *
 * Description     	:
 * Implementation of WPL Service apis
 *
 *
 * Author         	: Raghavendra MR
 *
 *
 ******************************************************************************
 */
#include "gpsc_data.h"
#include "mcpf_services.h"
#include "mcpf_mem.h"
#include "mcpf_time.h"
#include "mcpf_report.h"
#include "mcpf_msg.h"
#include "navc_defs.h"
#include "navc_api.h"
#include "navc_wplServices.h"
#include "wpl_app_api.h"
/**
 * \fn     wpl_location_request
 * \brief  Request for wifi postion
 *
 * \note
 * \param	location request parameter
 * \return 	void
 * \sa
 */
//manohar
void wpl_location_request( TNAVC_reqLocFix* loc_fix_req_params)
{
	TWPL_WifiLocReqParams* p_zWifiLocReq;

   gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
   TNAVCD_Ctrl		*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl;
  	TWPL_cmdParams 		*pWpl = NULL;
	TWPL_WifiLocReqParams *pLocReqParams = NULL;
	McpU32			uLen=0;
	McpU16			uOpcode=0;
	EMcpfRes  		eRes;
    EmcpTaskId 		eDestTaskId;
	McpU8		 	uDestQId;
	McpU32 			uSesId;


  	if(!pNavc->uwplTaskId)  {
		    MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("wpl_location_request() failed\n"));
		    return RES_ERROR;
	}
	p_zWifiLocReq = (TWPL_WifiLocReqParams*)mcpf_mem_alloc_from_pool(pNavc->hMcpf, pNavc->hEvtPool);

	if(p_zWifiLocReq) {


		p_zWifiLocReq->loc_fix_num_reports = loc_fix_req_params->loc_fix_num_reports;
		p_zWifiLocReq->loc_fix_period = loc_fix_req_params->loc_fix_period;
		uOpcode = WPL_CMD_REQUEST_WIFI_FIX;
		uLen = sizeof (TWPL_WifiLocReqParams);
		MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("wpl_location_request: sending loc req\n"));
		eRes = mcpf_SendMsg (pNavc->hMcpf,
				 pNavc->uwplTaskId,
				 0,
				 TASK_WPC_ID,
				 WPC_QUE_CMD_ID,
				 uOpcode,
				 uLen,
				 0, 				/* user defined */
				 (void*)p_zWifiLocReq);

		if (eRes != RES_OK)
		{
			mcpf_mem_free_from_pool (pNavc->hMcpf, pWpl);
	                MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("wpl_location_request: mcpf_SendMsg failed\n"));
		}
	} else
		MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("wpl_location_request: mcpf_mem_alloc_from_pool failed\n"));

}

/**
 * \fn     wpl_location_stop_request
 * \brief  Request to stop wifi postion
 *
 * \note
 * \param	session id
 * \return 	void
 * \sa
 */

 T_GPSC_result wpl_location_stop_request(  U32 q_SessionTagId)
{
    gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;


    TNAVCD_Ctrl	   *pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl;
    EmcpTaskId 		eDestTaskId;
	McpU8		 	uDestQId;
	McpU32		   uLen=0;
	McpU16		   uOpcode=0;
	EMcpfRes 	   eRes;
	McpU32 			uSesId;

  	if(!pNavc->uwplTaskId)  {
		    MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("wpl_location_stop_request() failed\n"));
		    return RES_ERROR;
	}

    MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("request for wpl_location_stop_request\n"));
	uOpcode = WPL_CMD_STOP_WIFI_FIX;
    uLen = 0;
		eRes = mcpf_SendMsg (pNavc->hMcpf,
				 pNavc->uwplTaskId,
				 0,
				 TASK_WPC_ID,
				 WPC_QUE_CMD_ID,
				 uOpcode,
				 uLen,
				 0, 				/* user defined */
				 NULL);
	   if (eRes != RES_OK)
	   {
		  MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("wpl_location_stop_request: mcpf_SendMsg failed\n"));
	   }

   return GPSC_SUCCESS;
}


/**
 * \fn     wpl_gps_info_ind
 * \brief  send the GPS information needed by wifi position engine
 *
 * \note
 * \param	GPS position report parameters
 * \return 	void
 * \sa
 */


void wpl_gps_info_ind(  gpsc_db_type *p_zGPSCDatabase)
{
   gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
  // gpsc_db_pos_type *p_DBPos = &p_zGPSCDatabase->z_DBPos;
   gpsc_report_pos_type *p_ReportPos = &p_zGPSCDatabase->z_DBPos.z_ReportPos;
   TNAVCD_Ctrl	   *pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl;
   TWPL_cmdParams	   *pWpl = NULL;
   TWPL_GpsInfo *pGpsInfo = NULL;
   McpU32		   uLen=0;
   McpU16		   uOpcode=0;
   EMcpfRes 	   eRes;


   MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("sending wpl_gps_info_ind\n"));
   pWpl = (TWPL_cmdParams *) mcpf_mem_alloc_from_pool (pNavc->hMcpf, pNavc->hEvtPool);
   if (pWpl)
   {


	   pGpsInfo = (TWPL_GpsInfo*)pWpl;

	    pGpsInfo->EastUnc = p_ReportPos->w_EastUnc;
	   	pGpsInfo->Multipathindicator = 0 ; // need to check on this
	   	pGpsInfo->NorthUnc = p_ReportPos->w_NorthUnc;
	   	pGpsInfo->NSVs = p_ReportPos->u_nSVs;
	   	pGpsInfo->RefFCount = p_ReportPos->q_RefFCount;
	   	pGpsInfo->VelEast = p_ReportPos->x_VelEast;
	   	pGpsInfo->VelNorth = p_ReportPos->x_VelNorth;
	   	pGpsInfo->VelUnc = p_ReportPos->w_VelUnc;
	   	pGpsInfo->VelVert = p_ReportPos->x_VelVert;
	   	pGpsInfo->VerticalUnc = p_ReportPos->w_VerticalUnc;

	   uOpcode = WPC_GPS_INFO_IND;

	   uLen = sizeof (TWPL_GpsInfo);

	   eRes = mcpf_SendMsg (pNavc->hMcpf,
							TASK_WPC_ID,
							WPC_QUE_CMD_ID,
							TASK_NAV_ID,
							NAVC_QUE_CMD_ID,
							uOpcode,
							uLen,
							0,				   /* user defined */
							(void*)pGpsInfo);

	   if (eRes != RES_OK)
	   {
		   mcpf_mem_free_from_pool (pNavc->hMcpf, pWpl);
		   MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("wpl_gps_info_ind: mcpf_SendMsg failed\n"));
	   }
	   }
   else
	   MCPF_REPORT_ERROR(pNavc->hMcpf, NAVC_MODULE_LOG, ("wpl_gps_info_ind: mcpf_mem_alloc_from_pool failed\n"));

}

