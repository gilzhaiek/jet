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


/** \file   navc_main.c 
 *  \brief  NAVC Init module implementation                                  
 * 
  */


/************************************************************************
 * Includes
 ************************************************************************/
#include "pla_defs.h"
#include "pla_cmdParse.h"
#include "navc_init.h"
#include "gpsc_types.h"
#include "mcpf_main.h"
#include "mcpf_report.h"
#include "mcpf_mem.h"
#include "mcpf_msg.h"
#include "navc_api.h"
#include "gpsc_data.h"
#include "gpsc_state.h"
#include "navc_internalEvtHandler.h"
#include "navc_defs.h"
#include "navc_cmdHandler.h"
#include "pla_app_ads_api.h"
#include "mcp_hal_st.h"
#include "navc_time.h"
#include "bmtrace.h"
#include "navc_sm.h"
#include "nt_adapt.h"

#include "navc_api.h"
#include "suplc_api.h"
#include <utils/Log.h>
#include "nav_log_msg.h"

#define INIT_TIME_UNCERTAINTY	120000000	/* usec */




TNAVCD_Ctrl *pNavcCtrl;

/************************************************************************
 * Functions
 ************************************************************************/

/** 
 * \fn     NAVC_Create 
 * \brief  Init the NAVC
 * 
 * Allocates internal data and starts NAVC main thread
 * 
 * \note
  * \param	hMCpf - handle to MCPF
 * \param	*CmdLine - Command Line parameters
 * \return 	Returns the status of operation: OK or Error
 */ 
EMcpfRes NAVC_Create(handle_t hMcpf, char *CmdLine)
{
	TcmdLineParams  tCmdLineParams;
	McpU16			 i = 0;
	EMcpfRes		    rc = RES_OK;
	gpsc_ctrl_type* pGpsc = 0;	
 	TNavcSm*        pNavcSm = 0;
/* Command Line Argument Scan
 *
 * -pN          		Set Sensor Comm Port to COM<N>
 * -lN          		Set NMEA Log Comm Port to COM<N>
 * -adsX,Y,Z,...		Set ADS Comm Ports to COM<X>, COM<Y>, etc.
 * -b					Set to 1 to Restrict the Almanac data injection during 3GPP test
 */

	tCmdLineParams.x_SensorPcPort = -1;
	tCmdLineParams.x_NmeaPcPort = -1;

	for (i = 0; i < MAX_ADS_PORTS; i++)
		tCmdLineParams.x_AdsPort[i] = -1;

	tCmdLineParams.x_AdsPortsCounter = 0;
	
/* Command Line Argument Scan
 *
 * -pN          		Set Sensor Comm Port to COM<N>
 * -lN          		Set NMEA Log Comm Port to COM<N>
 * -adsX,Y,Z,...		Set ADS Comm Ports to COM<X>, COM<Y>, etc.
 * -b					Set to 1 to Restrict the Almanac data injection during 3GPP test
 */

	pla_cmdParse_NavcCmdLine(CmdLine, &tCmdLineParams);

	/* Allocate NAVC ctrl structure */
	pNavcCtrl = mcpf_mem_alloc(hMcpf, sizeof(TNAVCD_Ctrl));
	if (pNavcCtrl == NULL)
	{
		MCPF_REPORT_ERROR(hMcpf, NAVC_MODULE_LOG, ("Failed to allocate NAVC ctrl structure!"));
		return RES_ERROR;
	}
	mcpf_mem_zero(hMcpf, pNavcCtrl, sizeof(TNAVCD_Ctrl));
  
	/* Init NAVC ctrl structure */
	pNavcCtrl->hMcpf = hMcpf;

	pNavcCtrl->hEvtPool = mcpf_memory_pool_create(hMcpf, sizeof(TNAVC_evt), 20); 
	if(pNavcCtrl->hEvtPool == NULL)
	{
		MCPF_REPORT_ERROR(hMcpf, NAVC_MODULE_LOG, ("Failed to create events pool!"));
		mcpf_mem_free(hMcpf, pNavcCtrl);

		return RES_ERROR;
	}

	pNavcCtrl->hSessPool = mcpf_memory_pool_create(hMcpf, sizeof(gpsc_sess_specific_cfg_type), 20); 
	if (pNavcCtrl->hSessPool == NULL)
	{
		MCPF_REPORT_ERROR(hMcpf, NAVC_MODULE_LOG, ("Failed to create Sessions pool!"));
		mcpf_memory_pool_destroy(hMcpf, pNavcCtrl->hEvtPool);
		mcpf_mem_free(hMcpf, pNavcCtrl);

		return RES_ERROR;
	}

	/* Create NAVC Task */
	rc = mcpf_CreateTask(hMcpf, TASK_NAV_ID, "NAVC", NAVC_QUE_MAX_ID - 1, NULL, NULL);
	if (rc != RES_OK)
	{
		MCPF_REPORT_ERROR(hMcpf, NAVC_MODULE_LOG, ("Failed to create NAVC Task!"));
		mcpf_memory_pool_destroy(hMcpf, pNavcCtrl->hSessPool);
		mcpf_memory_pool_destroy(hMcpf, pNavcCtrl->hEvtPool);
		mcpf_mem_free(hMcpf, pNavcCtrl);

		return RES_ERROR;
	}

	/* Create NAVC Queues */
	rc = mcpf_RegisterTaskQ(hMcpf, TASK_NAV_ID, NAVC_QUE_INTERNAL_EVT_ID, NavcEvtHandler, pNavcCtrl);
	if (rc != RES_OK)
	{
		MCPF_REPORT_ERROR(hMcpf, NAVC_MODULE_LOG, ("Failed to register NAVC Internal Event Queue!"));
		mcpf_DestroyTask(hMcpf, TASK_NAV_ID);
		mcpf_memory_pool_destroy(hMcpf, pNavcCtrl->hSessPool);
		mcpf_memory_pool_destroy(hMcpf, pNavcCtrl->hEvtPool);
		mcpf_mem_free(hMcpf, pNavcCtrl);

		return RES_ERROR;
	}
	
	rc = mcpf_RegisterTaskQ(hMcpf, TASK_NAV_ID, NAVC_QUE_ACTION_ID, NavcActionHandler, pNavcCtrl);
	if (rc != RES_OK)
	{
		MCPF_REPORT_ERROR(hMcpf, NAVC_MODULE_LOG, ("Failed to register NAVC Action Queue!"));
		mcpf_UnregisterTaskQ(hMcpf, TASK_NAV_ID, NAVC_QUE_INTERNAL_EVT_ID);
		mcpf_DestroyTask(hMcpf, TASK_NAV_ID);
		mcpf_memory_pool_destroy(hMcpf, pNavcCtrl->hSessPool);
		mcpf_memory_pool_destroy(hMcpf, pNavcCtrl->hEvtPool);
		mcpf_mem_free(hMcpf, pNavcCtrl);

		return RES_ERROR;
	}
	
	rc = mcpf_RegisterTaskQ(hMcpf, TASK_NAV_ID, NAVC_QUE_CMD_ID, NavcCmdHandler, pNavcCtrl);
	if (rc != RES_OK)
	{
		MCPF_REPORT_ERROR(hMcpf, NAVC_MODULE_LOG, ("Failed to register NAVC Command Queue!"));
		mcpf_UnregisterTaskQ(hMcpf, TASK_NAV_ID, NAVC_QUE_ACTION_ID);
		mcpf_UnregisterTaskQ(hMcpf, TASK_NAV_ID, NAVC_QUE_INTERNAL_EVT_ID);
		mcpf_DestroyTask(hMcpf, TASK_NAV_ID);
		mcpf_memory_pool_destroy(hMcpf, pNavcCtrl->hSessPool);
		mcpf_memory_pool_destroy(hMcpf, pNavcCtrl->hEvtPool);
		mcpf_mem_free(hMcpf, pNavcCtrl);

		return RES_ERROR;
	}

	/* Create NAVC SM */
	pNavcCtrl->hNavcSm = NavcSmCreate(hMcpf, pNavcCtrl);
	if (pNavcCtrl->hNavcSm == NULL)
	{
		MCPF_REPORT_ERROR(hMcpf, NAVC_MODULE_LOG, ("Failed to create NAVC SM!"));
		mcpf_UnregisterTaskQ(hMcpf, TASK_NAV_ID, NAVC_QUE_CMD_ID);
		mcpf_UnregisterTaskQ(hMcpf, TASK_NAV_ID, NAVC_QUE_ACTION_ID);
		mcpf_UnregisterTaskQ(hMcpf, TASK_NAV_ID, NAVC_QUE_INTERNAL_EVT_ID);
		mcpf_DestroyTask(hMcpf, TASK_NAV_ID);
		mcpf_memory_pool_destroy(hMcpf, pNavcCtrl->hSessPool);
		mcpf_memory_pool_destroy(hMcpf, pNavcCtrl->hEvtPool);
		mcpf_mem_free(hMcpf, pNavcCtrl);

		return RES_ERROR;
	}
	
	/* Init NTA */
	rc = NTA_Init(hMcpf, tCmdLineParams.x_SensorPcPort);
	if (rc != RES_OK)
	{
		MCPF_REPORT_ERROR(hMcpf, NAVC_MODULE_LOG, ("NTA_Init failed"));
		NavcSmDestroy(pNavcCtrl->hNavcSm);
		mcpf_UnregisterTaskQ(hMcpf, TASK_NAV_ID, NAVC_QUE_CMD_ID);
		mcpf_UnregisterTaskQ(hMcpf, TASK_NAV_ID, NAVC_QUE_ACTION_ID);
		mcpf_UnregisterTaskQ(hMcpf, TASK_NAV_ID, NAVC_QUE_INTERNAL_EVT_ID);
		mcpf_DestroyTask(hMcpf, TASK_NAV_ID);
		mcpf_memory_pool_destroy(hMcpf, pNavcCtrl->hSessPool);
		mcpf_memory_pool_destroy(hMcpf, pNavcCtrl->hEvtPool);
		mcpf_mem_free(hMcpf, pNavcCtrl);

		return RES_ERROR;
	}

	/* ADS Setup */
	pNavcCtrl->hAds = MCP_ADS_Create(hMcpf);
	if (pNavcCtrl->hAds == NULL)
	{
		MCPF_REPORT_ERROR(hMcpf, NAVC_MODULE_LOG, ("MCP_ADS_Create failed"));
		NTA_Destroy();
		NavcSmDestroy(pNavcCtrl->hNavcSm);
		mcpf_UnregisterTaskQ(hMcpf, TASK_NAV_ID, NAVC_QUE_CMD_ID);
		mcpf_UnregisterTaskQ(hMcpf, TASK_NAV_ID, NAVC_QUE_ACTION_ID);
		mcpf_UnregisterTaskQ(hMcpf, TASK_NAV_ID, NAVC_QUE_INTERNAL_EVT_ID);
		mcpf_DestroyTask(hMcpf, TASK_NAV_ID);
		mcpf_memory_pool_destroy(hMcpf, pNavcCtrl->hSessPool);
		mcpf_memory_pool_destroy(hMcpf, pNavcCtrl->hEvtPool);
		mcpf_mem_free(hMcpf, pNavcCtrl);

		return RES_ERROR;
	}

	for (i = 0; i < tCmdLineParams.x_AdsPortsCounter; i++)
	{
		if (MCP_ADS_AddPort(pNavcCtrl->hAds,  (McpU16) tCmdLineParams.x_AdsPort[i], HAL_ST_SPEED_57600) == NULL)
		{
			MCPF_REPORT_ERROR(hMcpf, NAVC_MODULE_LOG, ("MCP_ADS_AddPort failed"));
			rc = RES_ERROR;
		}
	}

	navcTime_init (pNavcCtrl, INIT_TIME_UNCERTAINTY, MCP_FALSE /* pulse request is disabled */);

	/* initalize assistance source database to 0xff initially*/
	mcpf_mem_set(hMcpf, &pNavcCtrl->tAssistSrcDb, 0xFF, sizeof(TNAVCD_AssistSrcDb[MAX_ASSIST_PROVIDER]));

	MCP_HAL_STRING_StrCpy (pNavcCtrl->uPathCtrlFile,(McpU8*)tCmdLineParams.s_PathCtrlFile);

	NavcSm(pNavcCtrl->hNavcSm, E_NAVC_EV_CREATE);
	 pNavcSm = (TNavcSm *) pNavcCtrl->hNavcSm;
	 pGpsc =(gpsc_ctrl_type *) pNavcSm->hGpsc;
	
	return rc;
};



/** 
 * \fn     NAVC_Destroy 
 * \brief  De-initialize NAVC stack
 * 
 * Delete NAVC object
 * 
 * \note
 * \param	void
 * \return 	status of operation: OK or Error
 */ 
EMcpfRes NAVC_Destroy(void)
{
	handle_t hMcpf = pNavcCtrl->hMcpf;
	

	NavcSm(pNavcCtrl->hNavcSm, E_NAVC_EV_DESTROY);

	/* De-initialize ADS objects */
	if (pNavcCtrl->hAds)
	{
		MCP_ADS_Destroy (pNavcCtrl->hAds);
	}

	/* Destroy NTA */
	
	NTA_Destroy();

	/* Destroy NAVC SM */
	NavcSmDestroy(pNavcCtrl->hNavcSm);
		
	/* Destory NAVC Task & Queues */
	mcpf_UnregisterTaskQ(hMcpf, TASK_NAV_ID, NAVC_QUE_INTERNAL_EVT_ID);
	mcpf_UnregisterTaskQ(hMcpf, TASK_NAV_ID, NAVC_QUE_ACTION_ID);
	mcpf_UnregisterTaskQ(hMcpf, TASK_NAV_ID, NAVC_QUE_CMD_ID);

	mcpf_memory_pool_destroy(hMcpf, pNavcCtrl->hEvtPool);
	mcpf_memory_pool_destroy(hMcpf, pNavcCtrl->hSessPool);
	mcpf_mem_free(hMcpf, pNavcCtrl);

	mcpf_DestroyTask(hMcpf, TASK_NAV_ID);
	return RES_OK;
}


/** 
 * \fn     NavcActionHandler 
 * \brief  Perform NAVC Actions
 * 
 * Start NAVC Actions execution - START / STOP
 * 
 * \note
 * \param	hNavc  - pointer to NAVC control object
 * \param	pMsg   - pointer to MCPF message containing NAVC Action
 * \return 	status of operation: OK or Error
 * \sa     	NavcActionHandler
 */ 
void NavcActionHandler (handle_t hNavc, TmcpfMsg * pMsg)
{
    TNAVCD_Ctrl *pNavc;
       TNAVC_evt    *pEvt;
        static McpS8     bInitialised=0;
        McpS8*           pState;
//  EMcpfRes    retVal = RES_OK;

	pNavc = (TNAVCD_Ctrl *)hNavc;
	pNavc->eSrcTaskId = pMsg->eSrcTaskId;
	pNavc->uSrcQId = pMsg->uSrcQId;
	pNavc->eCmdInProcess = pMsg->uOpcode;

    switch (pMsg->uOpcode)
    {
    case NAVC_CMD_START:
		if(bInitialised==0)
		{
       		NavcSm(pNavc->hNavcSm, E_NAVC_EV_START);
			bInitialised = 1;
		}
		else
		{
			//Send Command complete
			sendCmdCompleteEvt(pNavc, pNavc->eSrcTaskId, pNavc->uSrcQId, RES_OK, NAVC_CMD_START);
		}
		
                
        break;

       case NAVC_SUPLC_CMD_START:                  
       	{
              LOGD("begin NAVC_SUPLC_CMD_START \n");
		pEvt = (TNAVC_evt *) mcpf_mem_alloc_from_pool (pNavc->hMcpf, pNavc->hEvtPool);
		if (pEvt)
		{
			if(mcpf_SendMsg (pNavc->hMcpf, pNavc->eSrcTaskId,  pNavc->uSrcQId,  TASK_NAV_ID, NAVC_QUE_ACTION_ID,
						(McpU16) SUPLC_CMD_INIT_CORE, NAVC_EVT_HEADER_SIZE, 0, pEvt) != RES_OK)
				{
					mcpf_mem_free_from_pool (pNavc->hMcpf, pEvt);
					LOGD( "sendCmdCompleteEvt: NAVC_CMD_START mcpf_SendMsg failed\n");
				}
		}
         }  
		break;
    case NAVC_CMD_STOP:
		NavcSm(pNavc->hNavcSm, E_NAVC_EV_STOP);
                bInitialised = 0;
     break;
       case NAVC_SUPLC_CMD_STOP:        
 		{
               LOGD("begin NAVC_SUPLC_CMD_STOP \n");
		pEvt = (TNAVC_evt *) mcpf_mem_alloc_from_pool (pNavc->hMcpf, pNavc->hEvtPool);
		if (pEvt)
    		      {
			if(mcpf_SendMsg (pNavc->hMcpf, pNavc->eSrcTaskId,  pNavc->uSrcQId,	TASK_NAV_ID, NAVC_QUE_ACTION_ID,
						(McpU16) SUPLC_CMD_DEINIT_CORE, NAVC_EVT_HEADER_SIZE, 0, pEvt) != RES_OK)
			  {
				mcpf_mem_free_from_pool (pNavc->hMcpf, pEvt);
			      LOGD("sendCmdCompleteEvt: NAVC_CMD_STOP mcpf_SendMsg failed\n");
			}
   
                }
             }
		break;
        case NAVC_CMD_CHECK_INIT_STATE:
              {
                pState = (McpS8 *) mcpf_mem_alloc_from_pool (pNavc->hMcpf, pNavc->hEvtPool);
                if (pState)
                {
                   memcpy(pState,&bInitialised,sizeof(McpS8));
// qid 2 below is SUPLC_QUEUE_ID
                   if(mcpf_SendMsg (pNavc->hMcpf,TASK_SUPL_ID,2,  TASK_NAV_ID, NAVC_QUE_ACTION_ID,
                                                       (McpU16) SUPLC_RSP_NAVC_STATE, NAVC_EVT_HEADER_SIZE, 0,(void *)pState) != RES_OK)
                   {
                       LOGD("sendCmdCompleteEvt: NAVC_CMD_CHECK_INIT_STATE mcpf_SendMsg failed\n");
                   }
	       }
             } 
               break;

	default:
		MCPF_REPORT_ERROR(pNavcCtrl->hMcpf, NAVC_MODULE_LOG, ("NavcActionHandler: unknown cmd=%d", pMsg->uOpcode));
		break;
	}
    
	/* Free received msg and data buffer */
	if (pMsg->pData)
	{
		mcpf_mem_free_from_pool (pNavc->hMcpf, pMsg->pData);
	}
	mcpf_mem_free_from_pool (pNavc->hMcpf, (void *) pMsg);

	
}





















