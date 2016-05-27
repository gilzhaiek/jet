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


/** \file   mcp_msg.c 
 *  \brief  MCPF message module implementation
 *
 *  \see    msp_msg.h
 */

#include "mcpf_msg.h"
#include "mcpf_mem.h"
#include "mcpf_services.h"
#include "mcpf_report.h"
#include "pla_os.h"

#include <utils/Log.h>
#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG "MCPF.MCPF_MSG"    // our identification for logcat (adb logcat "MCPF.MCPF_MSG:V *:S)

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
 * \fn     mcpf_SendMsg
 * \brief  Send MCPF message
 * 
 */ 
EMcpfRes	mcpf_SendMsg (handle_t		hMcpf,
						  EmcpTaskId	eDestTaskId,
						  McpU8			uDestQId,
						  EmcpTaskId	eSrcTaskId,
						  McpU8			uSrcQId,
						  McpU16		uOpcode,
						  McpU32		uLen,
						  McpU32		uUserDefined,
						  void 			*pData)
{
	Tmcpf*    pMcpf = (Tmcpf*) hMcpf;
	TmcpfMsg* pMsg = (TmcpfMsg*)mcpf_mem_alloc_from_pool (hMcpf, pMcpf->hMsgPool);

	if (pMsg != NULL)
	{
		pMsg->eSrcTaskId = eSrcTaskId;
		pMsg->uSrcQId = uSrcQId;
		pMsg->uOpcode = uOpcode;
		pMsg->uLen 	  = uLen;
		pMsg->uUserDefined = uUserDefined;
		pMsg->pData   = pData;

		if (pMcpf->tClientsTable[eDestTaskId].fCb) 
		{
			pMcpf->tClientsTable[eDestTaskId].fCb (pMcpf->tClientsTable[eDestTaskId].hCb, 
													eDestTaskId, 
													uDestQId, 
													pMsg);
		}
		else
		{
			MCPF_REPORT_ERROR(hMcpf, MCPF_MODULE_LOG, 
								("mcpf_SendMsg: ERROR - Client's Cb is empty, freeing msg..."));
			mcpf_mem_free_from_pool(hMcpf, pMsg);
			return (RES_ERROR);
		}
	}
	else
	{
      ALOGE("+++ %s: mcpf_mem_alloc_from_pool failure. MCPF Hanle 0x%x, Pool Handle 0x%x,  +++\n",
       __FUNCTION__, hMcpf, pMcpf->hMsgPool);

		return (RES_MEM_ERROR);
	}

   //ALOGI("+++ %s: Message sent! +++\n", __FUNCTION__);
	return (RES_OK);
}


/** 
 * \fn     mcpf_SendPriorityMsg
 * \brief  Send MCPF message
 * 
 */ 
EMcpfRes	mcpf_SendPriorityMsg (handle_t		hMcpf,
						  EmcpTaskId	eDestTaskId,
						  McpU8			uDestQId,
						  EmcpTaskId	eSrcTaskId,
						  McpU8			uSrcQId,
						  McpU16		uOpcode,
						  McpU32		uLen,
						  McpU32		uUserDefined,
						  void 			*pData)
{
	Tmcpf		*pMcpf = (Tmcpf	*) hMcpf;
	TmcpfMsg	*pMsg;

	pMsg = (TmcpfMsg *)mcpf_mem_alloc_from_pool (hMcpf, pMcpf->hMsgPool);

	if (pMsg != NULL)
	{
		pMsg->eSrcTaskId = eSrcTaskId;
		pMsg->uSrcQId = uSrcQId;
		pMsg->uOpcode = uOpcode;
		pMsg->uLen 	  = uLen;
		pMsg->uUserDefined = uUserDefined;
		pMsg->pData   = pData;

		if (pMcpf->tClientsTable[eDestTaskId].fPriorityCb) 
		{
			pMcpf->tClientsTable[eDestTaskId].fPriorityCb (pMcpf->tClientsTable[eDestTaskId].hCb, 
													eDestTaskId, 
													uDestQId, 
													pMsg);
		}
		else
		{
			MCPF_REPORT_ERROR(hMcpf, MCPF_MODULE_LOG, 
								("mcpf_SendPriorityMsg: ERROR - Client's Priority Cb is empty, freeing msg..."));
			mcpf_mem_free_from_pool(hMcpf, pMsg);
			return (RES_ERROR);
		}
	}
	else
	{
		return (RES_MEM_ERROR);
	}

	return (RES_OK);
}



/** 
 * \fn     mcpf_MsgqEnable
 * \brief  Enable message queue
 * 
 */ 
EMcpfRes	mcpf_MsgqEnable (handle_t 	 hMcpf, 
							 EmcpTaskId eDestTaskId,
							 McpU8		 uDestQId)
{
	Tmcpf		*pMcpf = (Tmcpf	*) hMcpf;
	TMcpfTask   *pTask = pMcpf->tTask[ eDestTaskId ];
	McpBool		bQueFull = MCP_FALSE;
	EMcpfRes   res = RES_OK;
	
	mcpf_critSec_Enter (hMcpf, pTask->hCritSecObj, MCPF_INFINIT);

	pTask->bQueueFlag[ uDestQId ] = MCP_TRUE; 
	if (que_Size (pTask->hQueue[ uDestQId ]))
	{
		bQueFull = MCP_TRUE;
	}

	mcpf_critSec_Exit (hMcpf, pTask->hCritSecObj);

	if (bQueFull)
	{
		res = os_sigobj_set (pMcpf->hPla, pTask->hSignalObj);
	}

	return res;
}

/** 
 * \fn     mcpf_MsgqDisable
 * \brief  Enable message queue
 * 
 */ 
EMcpfRes	mcpf_MsgqDisable (handle_t 	 	hMcpf, 
							  EmcpTaskId 	eDestTaskId,
							  McpU8		 	uDestQId)
{
	Tmcpf		*pMcpf = (Tmcpf	*) hMcpf;
	TMcpfTask   *pTask = pMcpf->tTask[ eDestTaskId ];
	
	mcpf_critSec_Enter (hMcpf, pTask->hCritSecObj, MCPF_INFINIT);

	pTask->bQueueFlag[ uDestQId ] = MCP_FALSE; 

	mcpf_critSec_Exit (hMcpf, pTask->hCritSecObj);

	return RES_OK;
}

/** 
 * \fn     mcpf_SetEvent
 * \brief  Set event
 * 
 */ 
EMcpfRes	mcpf_SetEvent (handle_t		hMcpf, 
						   EmcpTaskId	eDestTaskId,
						   McpU32		uEvent)
{
	Tmcpf		*pMcpf = (Tmcpf	*) hMcpf;
	TMcpfTask   *pTask = pMcpf->tTask[ eDestTaskId ];
	McpBool		bEvtChange = MCP_FALSE;
	EMcpfRes  res = RES_OK;
	McpU32		uEventMask = 0x01 << uEvent;
	
	mcpf_critSec_Enter (hMcpf, pTask->hCritSecObj, MCPF_INFINIT);

	if (!(pTask->uEvntBitmap & uEventMask))
	{
		pTask->uEvntBitmap |= uEventMask;
		bEvtChange = MCP_TRUE;
	}

	mcpf_critSec_Exit (hMcpf, pTask->hCritSecObj);

	if (bEvtChange)
	{
		res = os_sigobj_set (pMcpf->hPla, pTask->hSignalObj);
	}

	return res;
}


/** 
 * \fn     mcpf_RegisterClientCb
 * \brief  Register external send message handler function
 *
 */ 
EMcpfRes	mcpf_RegisterClientCb (handle_t			hMcpf,
								   McpU32			*pDestTaskId,
								   tClientSendCb 		fCb,
								   handle_t		 	hCb)
{
	EMcpfRes res = RES_ERROR;
	Tmcpf		*pMcpf = (Tmcpf	*) hMcpf;
	McpU32		taskId;

	for(taskId=TASK_MAX_ID; taskId<TASK_MAX_ID+MAX_EXTERNAL_CLIENTS; taskId++)
	{
		if(pMcpf->tClientsTable[taskId].fCb == NULL)
		{
			pMcpf->tClientsTable[taskId].fCb = fCb;
			pMcpf->tClientsTable[taskId].hCb = hCb;
			*pDestTaskId = taskId; 
			res = RES_OK;
			break;
		}
	}
	return (res);
}

/** 
 * \fn     mcpf_UnRegisterClientCb
 * \brief  Unregister MCPF external send message handler
 * 
 */ 
EMcpfRes	mcpf_UnRegisterClientCb (handle_t	hMcpf,
									 McpU32		uDestTaskId)
{
	EMcpfRes res = RES_ERROR;
	Tmcpf		*pMcpf = (Tmcpf	*) hMcpf;

	if(pMcpf->tClientsTable[uDestTaskId].fCb)
	{
		pMcpf->tClientsTable[uDestTaskId].fCb = NULL;
		pMcpf->tClientsTable[uDestTaskId].hCb = NULL;
		res = RES_OK;
	}
	return res;
}

/** 
 * \fn     mcpf_EnqueueMsg 
 * \brief  Enqueue MCPF message
 * 
 */
EMcpfRes mcpf_EnqueueMsg (handle_t		hMcpf,
								  EmcpTaskId	eDestTaskId,
								  McpU8			uDestQId,
								  TmcpfMsg  	*pMsg)
{
	Tmcpf		*pMcpf = (Tmcpf	*) hMcpf;
	TMcpfTask   *pTask = pMcpf->tTask[ eDestTaskId ];
	EMcpfRes   res;

	if (uDestQId >= pMcpf->tTask[eDestTaskId]->uNumOfQueues)
	{
		/* Destination queue is not valid */
		mcpf_mem_free_from_pool (hMcpf, pMsg);
		return RES_ERROR;
	}
	
	mcpf_critSec_Enter (hMcpf, pTask->hCritSecObj, MCPF_INFINIT);

	res = que_Enqueue ( pTask->hQueue[ uDestQId ], (handle_t) pMsg);

	mcpf_critSec_Exit (hMcpf, pTask->hCritSecObj);

	if (res == RES_OK)
	{
#ifdef DEBUG
		 MCPF_REPORT_INFORMATION(hMcpf, MCPF_MODULE_LOG, 
								("ENQUEUED MSG FOR TASK #%d, TO QUEUE #%d", eDestTaskId, uDestQId));
#endif
		
        res = os_sigobj_set (pMcpf->hPla, pTask->hSignalObj);
	}
	else
	{
		return RES_ERROR;
	}
	return res;
}

/** 
 * \fn     mcpf_RequeueMsg 
 * \brief  Requeue MCPF message
 * 
 */
EMcpfRes mcpf_RequeueMsg (handle_t		hMcpf,
								  EmcpTaskId	eDestTaskId,
								  McpU8			uDestQId,
								  TmcpfMsg  	*pMsg)
{
	Tmcpf		*pMcpf = (Tmcpf	*) hMcpf;
	TMcpfTask   *pTask = pMcpf->tTask[ eDestTaskId ];
	EMcpfRes   res;

	if (uDestQId >= pMcpf->tTask[eDestTaskId]->uNumOfQueues)
	{
		/* Destination queue is not valid */
		mcpf_mem_free_from_pool (hMcpf, pMsg);
		return RES_ERROR;
	}
	
	mcpf_critSec_Enter (hMcpf, pTask->hCritSecObj, MCPF_INFINIT);

	res = que_Requeue ( pTask->hQueue[ uDestQId ], (handle_t) pMsg);

	mcpf_critSec_Exit (hMcpf, pTask->hCritSecObj);

	if (res == RES_OK)
	{
#ifdef DEBUG
		 MCPF_REPORT_INFORMATION(hMcpf, MCPF_MODULE_LOG, 
								("REQUEUED MSG FOR TASK #%d, TO QUEUE #%d", eDestTaskId, uDestQId));
#endif
		
        res = os_sigobj_set (pMcpf->hPla, pTask->hSignalObj);
	}
	else
	{
		return RES_ERROR;
	}
	return res;
}



/************************************************************************
 * Module Private Functions
 ************************************************************************/
