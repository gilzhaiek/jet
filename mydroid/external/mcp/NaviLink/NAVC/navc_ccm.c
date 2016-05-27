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


/** \file   navc_sm.c 
 *  \brief  NAVC main state machine implementation                                  
 * 
  */


/************************************************************************
 * Includes
 ************************************************************************/
#include "gpsc_data.h"
#include "gpsc_state.h"

#include "navc_ccm.h"
#include "ccm.h"
#include "ccm_im.h"
#include "navc_init.h"
#include "navc_api.h"
#include "mcpf_main.h"
#include "mcpf_report.h"
#include "mcpf_mem.h"
#include "mcpf_services.h"
#include "mcpf_msg.h"
#include "bmtrace.h"
#include "navc_sm.h"
#include "mcp_hal_os.h"
#include "mcp_hciDefs.h"
#include "mcp_hci_adapt.h"

static void navcCcmImHandlerFun(CcmImEvent *pEvent);
static void NavcCmdComplCb (handle_t hCbHandle, THciaCmdComplEvent *pTxn);
static EMcpfRes NavcCcmSendData (handle_t hNavcCcm);

typedef struct  
{
	handle_t			hMcpf;
	handle_t			hNavcSm;
	CcmObj				*pCcmObj;
 	CCM_IM_StackHandle	hCcmIm;
}TNavcCcm;
static 	TNavcCcm *g_pNavcCcm;
static T_GPSC_result NavcSaPowerCtrl(TNavcCcm *pNavcCcm, McpBool bPowerOn);

/** 
 * \fn     NavcCCmRegisterStack 
 * \brief  Register NAVC to CCM
 * 
 * Register NAVC stack to CCM
 * 
 * \note
 * \param	void
 * \return 	status of operation: OK or Error
 * \sa     	navcCcmImHandlerFun
 */ 
EMcpfRes NavcCcmRegisterStack(TNavcCcm *pNavcCcm)
{
	CcmImStatus	ccmImStatus;
	CcmImObj	*pCcmImObj;

	pNavcCcm->pCcmObj = MCPF_GET_CCM_OBJ(pNavcCcm->hMcpf);
	pCcmImObj  = CCM_GetIm (pNavcCcm->pCcmObj);
	
	ccmImStatus = CCM_IM_RegisterStack (pCcmImObj, CCM_IM_STACK_ID_GPS, navcCcmImHandlerFun, &pNavcCcm->hCcmIm);
	if (ccmImStatus != CCM_IM_STATUS_SUCCESS)
	{
		MCPF_REPORT_ERROR(pNavcCcm->hMcpf, NAVC_MODULE_LOG, ("registerStackToCcm: CCM_IM_RegisterStack failed!"));
		return RES_ERROR;
	}
	return RES_OK;
}

/** 
 * \fn     NavcCCmUnRegisterStack 
 * \brief  UnRegister NAVC from CCM
 * 
 * Register NAVC stack to CCM
 * 
 * \note
 * \param	void
 * \return 	status of operation: OK or Error
 * \sa     	navcCcmImHandlerFun
 */ 
EMcpfRes NavcCCmUnRegisterStack(TNavcCcm *pNavcCcm)
{
	CcmImStatus	ccmImStatus;
	if (CCM_IM_GetStackState (pNavcCcm->hCcmIm) != CCM_IM_STACK_STATE_OFF)
	{
		MCPF_REPORT_ERROR(pNavcCcm->hMcpf, NAVC_MODULE_LOG, 
			("NAVC_Deinit: CCM_IM_GetStackState - Stack is still ON, need to turn it OFF first."));
		return RES_ERROR;
	}
	else
	{
		ccmImStatus = CCM_IM_DeregisterStack(&pNavcCcm->hCcmIm);
		if (ccmImStatus != CCM_IM_STATUS_SUCCESS)
		{
			MCPF_REPORT_ERROR(pNavcCcm->hMcpf, NAVC_MODULE_LOG, 
				("NAVC_Deinit: CCM_IM_DeregisterStack failed"));
			return RES_ERROR;
		}
		pNavcCcm->hCcmIm = NULL;
		
		pNavcCcm->pCcmObj = NULL;
	}
	return RES_OK;
}

void NavcCcmStackOn(handle_t hNavcCcm)
{
	TNavcCcm *pNavcCcm = (TNavcCcm*)hNavcCcm;
	CcmImStatus	ccmImStatus;

	if(CCM_IM_GetStackState(pNavcCcm->hCcmIm) == CCM_IM_STACK_STATE_ON)
	{
		MCPF_REPORT_INFORMATION(pNavcCcm->hMcpf, NAVC_MODULE_LOG, ("NavcCcmStackOn: NAVC stack already ON"));
		mcpf_SendMsg(pNavcCcm->hMcpf, TASK_NAV_ID, NAVC_QUE_INTERNAL_EVT_ID, 
						TASK_CCM_ID, 1, NAVCD_EVT_CCM_COMPL_IND, 0, 0, NULL);
		return;
	}
    ccmImStatus = CCM_IM_StackOn(pNavcCcm->hCcmIm);
	switch (ccmImStatus)
	{
		case CCM_IM_STATUS_SUCCESS:
			MCPF_REPORT_INFORMATION(pNavcCcm->hMcpf, NAVC_MODULE_LOG, ("NavcCcmStackOn: NAVC stackOn succeeded"));
			mcpf_SendMsg(pNavcCcm->hMcpf, TASK_NAV_ID, NAVC_QUE_INTERNAL_EVT_ID, 
							TASK_CCM_ID, 1, NAVCD_EVT_CCM_COMPL_IND, 0, 0, NULL);
			break;

		case CCM_IM_STATUS_PENDING:
			MCPF_REPORT_INFORMATION(pNavcCcm->hMcpf, NAVC_MODULE_LOG, ("NavcCcmStackOn: NAVC stackOn is pending"));
			break;

		default:
			MCPF_REPORT_ERROR(pNavcCcm->hMcpf, NAVC_MODULE_LOG, ("NavcCcmStackOn: CCM_IM_StackOn failed!\n"));
            MCP_ASSERT(0);
			break;
		}
}



/** 
 * \fn     NavcCcmStackOff 
 * \brief  Turning Off stack in CCM
 * 
 * Turning Off stack in CCM
 * 
 * \note
 * \param	pNavc  - pointer to NAVC control object
 * \return 	status of operation: OK or Error
 * \sa     	NAVC_ccmStackOff
 */ 
void NavcCcmStackOff(handle_t hNavcCcm)
{
	TNavcCcm *pNavcCcm = (TNavcCcm*)hNavcCcm;

	CcmImStatus	ccmImStatus;
	CcmImStackState tCcmImStackState;

	if(pNavcCcm->hCcmIm == NULL)
	{
		mcpf_SendMsg(pNavcCcm->hMcpf, TASK_NAV_ID, NAVC_QUE_INTERNAL_EVT_ID, 
				TASK_CCM_ID, 1, NAVCD_EVT_CCM_COMPL_IND, 0, 0, NULL);
		return;
	}
	
	tCcmImStackState = CCM_IM_GetStackState(pNavcCcm->hCcmIm);
	if(tCcmImStackState == CCM_IM_STACK_STATE_OFF)
	{
		mcpf_SendMsg(pNavcCcm->hMcpf, TASK_NAV_ID, NAVC_QUE_INTERNAL_EVT_ID, 
				TASK_CCM_ID, 1, NAVCD_EVT_CCM_COMPL_IND, 0, 0, NULL);
		return;
	}

	if (tCcmImStackState != CCM_IM_STACK_STATE_ON_IN_PROGRESS)
	{
		if(NavcCcmSendData(hNavcCcm) == RES_ERROR)
		{
			MCPF_REPORT_ERROR(pNavcCcm->hMcpf, NAVC_MODULE_LOG, ("NavcCcmStackOff: NavcCcmSendData failed!\n"));
			MCP_ASSERT(0);
		}
	}
	else
	{
		ccmImStatus = CCM_IM_StackOnAbort(pNavcCcm->hCcmIm);
		if ((ccmImStatus != CCM_IM_STATUS_SUCCESS) && (ccmImStatus != CCM_IM_STATUS_PENDING)) 
		{
			MCPF_REPORT_INFORMATION(pNavcCcm->hMcpf, NAVC_MODULE_LOG, ("NavcCcmStackOff: CCM_IM_StackOnAbort failed!"));
			MCP_ASSERT(0);
		}
	}
}

/** 
 * \fn     navcCcmImHandlerFun 
 * \brief  NAVC callback from CCM
 * 
 * Process CCM event
 * 
 * \note
 * \param	pEvent - pointer to CCM event object
 * \return 	void
 * \sa     	navcCcmImHandlerFun
 */ 
static void navcCcmImHandlerFun(CcmImEvent *pEvent)
{
	TNavcCcm *pNavcCcm = g_pNavcCcm;
    MCPF_REPORT_INFORMATION(pNavcCcm->hMcpf, NAVC_MODULE_LOG, 
		("navcCcmImHandlerFun: eventType=%d status=%d", pEvent->type, pEvent->status));

    switch (pEvent->type)
	{
		case CCM_IM_EVENT_TYPE_ON_COMPLETE:
			if (pEvent->status == CCM_IM_STATUS_SUCCESS)
			{
				MCPF_REPORT_INFORMATION(pNavcCcm->hMcpf, NAVC_MODULE_LOG, 
					("navcCcmImHandlerFun: NAVC stackOn is completed"));

				mcpf_SendMsg(pNavcCcm->hMcpf, TASK_NAV_ID, NAVC_QUE_INTERNAL_EVT_ID, 
								TASK_CCM_ID, 1, NAVCD_EVT_CCM_COMPL_IND, 0, 0, NULL);
			}
			else
			{
				MCPF_REPORT_ERROR(pNavcCcm->hMcpf, NAVC_MODULE_LOG, 
					("navcCcmImHandlerFun: NAVC stackOn failed!"));
			}
		break;

		case CCM_IM_EVENT_TYPE_ON_ABORT_COMPLETE:
			if (pEvent->status == CCM_IM_STATUS_SUCCESS)
			{
				MCPF_REPORT_INFORMATION(pNavcCcm->hMcpf, NAVC_MODULE_LOG, 
					("navcCcmImHandlerFun: NAVC abort is completed"));
			}
			else
			{
				MCPF_REPORT_ERROR(pNavcCcm->hMcpf, NAVC_MODULE_LOG, 
					("navcCcmImHandlerFun: NAVC abort is failed!"));
			}
		break;

		case CCM_IM_EVENT_TYPE_OFF_COMPLETE:
			if (pEvent->status == CCM_IM_STATUS_SUCCESS)
			{
				MCPF_REPORT_INFORMATION(pNavcCcm->hMcpf, NAVC_MODULE_LOG, 
					("navcCcmImHandlerFun: NAVC stackOff is completed"));

                mcpf_SendMsg(pNavcCcm->hMcpf, TASK_NAV_ID, NAVC_QUE_INTERNAL_EVT_ID, 
								TASK_CCM_ID, 1, NAVCD_EVT_CCM_COMPL_IND, 0, 0, NULL);
			}
			else
			{
				MCPF_REPORT_ERROR(pNavcCcm->hMcpf, NAVC_MODULE_LOG, 
					("navcCcmImHandlerFun: NAVC stackOff failed!"));
			}
		break;

		default:
			MCPF_REPORT_ERROR(pNavcCcm->hMcpf, NAVC_MODULE_LOG, 
				("navcCcmImHandlerFun: unknown event=%d!", pEvent->type));
			break;
	}
}

handle_t NavcCcmCreate(handle_t hMcpf, handle_t hNavcSm)
{
	TNavcCcm *pNavcCcm = mcpf_mem_alloc(hMcpf, sizeof(TNavcCcm));
	if(pNavcCcm)
	{
		g_pNavcCcm = pNavcCcm;
		pNavcCcm->hMcpf = hMcpf;
		pNavcCcm->hNavcSm = hNavcSm;

		/* Register Stack to CCM */
		if (NavcCcmRegisterStack(pNavcCcm) != RES_OK)
		{
			MCP_ASSERT(0);
		}
	}
	return pNavcCcm;
}

void NavcCcmDestroy(handle_t hNavcCcm)
{
	TNavcCcm *pNavcCcm = (TNavcCcm*)hNavcCcm;

	NavcCCmUnRegisterStack(pNavcCcm);
	mcpf_mem_free(pNavcCcm->hMcpf, hNavcCcm);
}

static void NavcCmdComplCb (handle_t hCbHandle, THciaCmdComplEvent *pTxn)
{
	TNavcCcm *pNavcCcm = (TNavcCcm*)hCbHandle;
	CcmImStatus	ccmImStatus;
	
	//mcpf_txnPool_FreeBuf(pNavcCcm->hMcpf, (TTxnStruct*)pTxn);

	//MCP_HAL_OS_Sleep(5000);
	
	ccmImStatus = CCM_IM_StackOff(pNavcCcm->hCcmIm);
		
		switch (ccmImStatus)
		{
		case CCM_IM_STATUS_SUCCESS:
			MCPF_REPORT_INFORMATION(pNavcCcm->hMcpf, NAVC_MODULE_LOG, ("NavcCcmStackOff: NAVC stackOff succeeded"));
			mcpf_SendMsg(pNavcCcm->hMcpf, TASK_NAV_ID, NAVC_QUE_INTERNAL_EVT_ID, 
				TASK_CCM_ID, 1, NAVCD_EVT_CCM_COMPL_IND, 0, 0, NULL);
			break;
			
		case CCM_IM_STATUS_PENDING:
			MCPF_REPORT_INFORMATION(pNavcCcm->hMcpf, NAVC_MODULE_LOG, ("NavcCcmStackOff: NAVC stackOff is pending"));
			break;
			
		default:
			MCPF_REPORT_ERROR(pNavcCcm->hMcpf, NAVC_MODULE_LOG, ("NavcCcmStackOff: CCM_IM_StackOff failed!\n"));
			MCP_ASSERT(0);
			break;
		}
}

static EMcpfRes NavcCcmSendData (handle_t hNavcCcm)
{
	TNavcCcm *pNavcCcm = (TNavcCcm*)hNavcCcm;
	TTxnStruct *pTxn;
	static McpU8	data = 0x00;

	return HCIA_SendCommand(((Tmcpf *)pNavcCcm->hMcpf)->hHcia, /*0xC0FF*/ 0xFFC0, &data, 1, 
								NavcCmdComplCb, hNavcCcm);
#if 0
	/* Allocate transaction Structure and a Buffer */
	pTxn = mcpf_txnPool_AllocBuf(pNavcCcm->hMcpf, 8);

	if(!pTxn)
	{
		MCPF_REPORT_ERROR(pNavcCcm->hMcpf, NAVC_MODULE_LOG, 
			("NavcCcmSendData: Failed allocating transaction structure and a buffer..."));
		return RES_ERROR;
	}

	pTxn->pData = pTxn->aBuf[0];
	/* Build and Add HCI Header, including packet type */
	pTxn->pData[0] = (McpU8)HCI_PKT_TYPE_COMMAND;

	 /* GPS Power Mode: 0xffc0 */
	pTxn->pData[1] = (McpU8)0xc0; 
	pTxn->pData[2] = (McpU8)0xFF; 
	 
	pTxn->pData[3] = (McpU8)0x01;	/* len =1 */ 
	pTxn->pData[4] = (McpU8)0x00;  /* value = 0 (Power Off) */

	pTxn->aLen[0] = 5;

	/* Transaction Protocol */
	pTxn->protocol = PROTOCOL_ID_HCI;

	/* Set TX complete CB */
	pTxn->fTxnDoneCb = NavcCcmTxCmplt;
	pTxn->hCbHandle = hNavcCcm;

	TXN_PARAM_SET_CHAN_NUM(pTxn, HCI_PKT_TYPE_COMMAND); /* sets channel number */

	/* Send Txn to Transport */
	return mcpf_trans_TxData(pNavcCcm->hMcpf, pTxn);
#endif
}
 