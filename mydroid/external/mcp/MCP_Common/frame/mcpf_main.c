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

/** Include Files **/
#include "mcpf_defs.h"
#include "mcpf_main.h"
#include "mcpf_mem.h"
#include "mcpf_services.h"
#include "mcpf_report.h"
#include "pla_os.h"
#include "mcp_hal_misc.h"
#include "mcp_hal_pm.h"
#include "mcp_hal_string.h"
#include "mcpf_msg.h"
#include "bmtrace.h"
#ifdef MCP_STU_ENABLE
#include "mcp_txnpool.h"
#include "mcp_hal_uart.h"
#include "mcp_hciDefs.h"
#include "mcp_hci_adapt.h"
#include "ccm.h"
#else
#include "mcp_hal_st.h"
#endif

#include <utils/Log.h>
#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG "MCP.MCPF_MAIN"    // our identification for logcat (adb logcat MCP.MCPF_MAIN:V *:S)


/************************************************************************/
/*							Internal Function							*/
/************************************************************************/
EMcpfRes		mcpf_TaskProc (handle_t	hMcpf, EmcpTaskId	eTaskId);

#ifdef MCP_STU_ENABLE	
/* FUTURE USE */
/*void			mcpf_ccmCmdHandler(handle_t hMcpf,  TmcpfMsg *tMsg);*/

/** 
 * \fn     eventHandlerFun
 * \brief  Transaction layer Cb function
 * 
 * This function is the Transaction layer Cb function. 
 * It's responsibility is to handle system errors.
 * In current implementation every stack handles 
 * it's own errors, so this function is left empty.
 * 
 * \note
 * \param	hHandleCb     		- handle to MCPF
 * \param	pEvent     	- Event received
 * \return 	None
 * \sa     	eventHandlerFun
 */ 
static void eventHandlerFun (handle_t hHandleCb, TTxnEvent * pEvent)
{
	MCPF_UNUSED_PARAMETER(hHandleCb);

	switch (pEvent->eEventType)
        {
	case Txn_InitComplInd:
		break;
		
	case Txn_DestroyComplInd:
		break;
		
	case Txn_ErrorInd:
		MCPF_REPORT_ERROR(hHandleCb, MCPF_MODULE_LOG, ("eventHandlerFun: Txn_ErrorInd, errId=%u modId=%u\n", 
							pEvent->tEventParams.err.eErrId,
							pEvent->tEventParams.err.eModuleId));
		break;
		
	default:
		MCPF_REPORT_ERROR(hHandleCb, MCPF_MODULE_LOG, ("eventHandlerFun: unknown event received\n"));
		break;
		
        }
}
#endif /* ifdef MCP_STU_ENABLE */


/** MCPF APIs - Initialization **/

/** 
 * \fn     mcpf_create
 * \brief  Create MCPF
 * 
 * This function is used to allocate and initiate the context of the MCP framework.
 * 
 * \note
 * \param	hPla     		- handle to Platform Adaptation
 * \param	uPortNum     	- Port Number
 * \return 	MCPF Handler or NULL on failure
 * \sa     	mcpf_create
 */ 
handle_t mcpf_create(handle_t hPla, McpU16 uPortNum)
{
	Tmcpf* pMcpf = 0;

   MCPF_UNUSED_PARAMETER(uPortNum);

	// allocate MCPF module
	pMcpf = (Tmcpf*)os_malloc(hPla, sizeof(Tmcpf));
	
	if (!pMcpf)
	{
	    MCPF_OS_REPORT (("Error allocating the MCPF Module\n"));
	    return NULL;
	}
   os_memory_zero (hPla, pMcpf, sizeof(Tmcpf));

	/* Register to PLA */
	pMcpf->hPla = hPla;
	pla_registerClient(hPla, (handle_t)pMcpf);
	
	/* Initiate report module. */
	pMcpf->hReport = report_Create((handle_t)pMcpf);

	/* Create Critical Section Object. */
	mcpf_critSec_CreateObj((handle_t)pMcpf, "McpfCritSecObj", &pMcpf->hMcpfCritSecObj);

	/* Allocate memory pools (for Msg, Timer). */
	pMcpf->hMsgPool = mcpf_memory_pool_create((handle_t)pMcpf, (McpU16)sizeof(TmcpfMsg), 
						  (McpU16)MCPF_MSG_POOL_SIZE); 
 //  ALOGI("+++ %s: Create Message Pool [0x%x]. Element size: [%d], Number of Elements: [%d] +++\n",
 //      __FUNCTION__, pMcpf->hMsgPool, sizeof(TmcpfMsg), MCPF_MSG_POOL_SIZE);
	
	pMcpf->hTimerPool = mcpf_memory_pool_create((handle_t)pMcpf, (McpU16)sizeof(TMcpfTimer), 
															(McpU16)MCPF_TIMER_POOL_SIZE); 

 //  ALOGI("+++ %s: Create Timer Pool [0x%x]. Element size: [%d], Number of Elements: [%d] +++\n",
 //      __FUNCTION__, pMcpf->hTimerPool, sizeof(TMcpfTimer), MCPF_TIMER_POOL_SIZE);


	/* Create Timer's linked list. */
	pMcpf->hTimerList = mcpf_SLL_Create((handle_t)pMcpf, 10, MCPF_FIELD_OFFSET(TMcpfTimer, tTimerListNode), 
										MCPF_FIELD_OFFSET(TMcpfTimer, uEexpiryTime), SLL_UP);

	CL_TRACE_INIT(pMcpf);
	CL_TRACE_ENABLE();

	return (handle_t)pMcpf;
}

/** 
 * \fn     mcpf_destroy
 * \brief  Destroy MCPF
 * 
 * This function is used to free all the resources allocated in the MCPF creation.
 * 
 * \note
 * \param	hMcpf     - handle to MCPF
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_destroy
 */ 
EMcpfRes 	mcpf_destroy(handle_t hMcpf)
{
	Tmcpf *pMcpf = (Tmcpf *)hMcpf;


#ifdef MCP_STU_ENABLE	
	/* FUTURE USE: Destroy MCPF/CCM Thread */
	/*
	mcpf_UnregisterTaskQ(hMcpf, TASK_CCM_ID, 0);
	mcpf_DestroyTask(hMcpf, TASK_CCM_ID);
	*/
#endif /* ifdef MCP_STU_ENABLE */

	CL_TRACE_DISABLE();
	CL_TRACE_DEINIT();

#ifdef MCP_STU_ENABLE	
#ifndef TI_STAND_ALONE_UART_BUSDRV
	/* Destroy CCM */
	CCM_Destroy(&pMcpf->hCcmObj);
#endif

	HCIA_Destroy (pMcpf->hHcia);

	/* Destroy Transport Module */
	trans_Destroy(pMcpf->hTrans);
#endif /* ifdef MCP_STU_ENABLE */

	/* Destroy Timer's linked list. */
	mcpf_SLL_Destroy(pMcpf->hTimerList);


	/* Free memory pools (for Msg, Timer, Txn & 2 Txn Buf). */
#ifdef MCP_STU_ENABLE	
	mcpf_txnPool_Destroy(hMcpf);
#endif /* ifdef MCP_STU_ENABLE */
	mcpf_memory_pool_destroy(hMcpf, pMcpf->hMsgPool);
	mcpf_memory_pool_destroy(hMcpf, pMcpf->hTimerPool);
#ifdef MCP_STU_ENABLE	
	mcpf_memory_pool_destroy(hMcpf, pMcpf->hTxnBufPool_long);
	mcpf_memory_pool_destroy(hMcpf, pMcpf->hTxnBufPool_short);
#endif /* ifdef MCP_STU_ENABLE */


	/* Destroy Critical Section Object. */
	mcpf_critSec_DestroyObj(hMcpf, &pMcpf->hMcpfCritSecObj);


	/* Unload report module. */
	report_Unload(pMcpf->hReport);


	/* Deallocate MCPF module */
	os_free(pMcpf->hPla, hMcpf);


	return RES_COMPLETE;
}

/** 
 * \fn     mcpf_CreateTask
 * \brief  Create an MCPF Task
 * 
 * This function opens a session with the MCPF, 
 * with the calling driver's specific requirements.
 * 
 * \note
 * \param	hMcpf - MCPF handler.
 * \param	eTaskId - a static (constant) identification of the driver.
 * \param	pTaskName - pointer to null terminated task name string
 * \param	uNum_of_queues - number of priority queues required by the driver.
 * \param	fEvntCb - Event's callback function.
 * \param	hEvtCbHandle - Event's callback handler.
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_CreateTask
 */ 
EMcpfRes	mcpf_CreateTask (handle_t 		hMcpf, 
							 EmcpTaskId 	eTaskId, 
							 char 			*pTaskName, 
							 McpU8 			uNum_of_queues, 
							 mcpf_event_cb 	fEvntCb, 
							 handle_t 		hEvtCbHandle)
{
	Tmcpf *pMcpf = (Tmcpf *)hMcpf;
	char critSecObjName[20];
	McpU32 i;
	
	/* If a context to the specified 'eTaskId' is already created --> Return Fail */
	if (pMcpf->tTask[eTaskId])
		return RES_ERROR;
	

	/* Allocate task handler */
	pMcpf->tTask[eTaskId] = (TMcpfTask *) mcpf_mem_alloc(hMcpf, sizeof(TMcpfTask));
	MCPF_Assert(pMcpf->tTask[eTaskId]);


	/** Allocate Queues according to 'uNum_of_queues' + 1 queue for timer handling **/
	pMcpf->tTask[eTaskId]->uNumOfQueues  = uNum_of_queues + 1;
	pMcpf->tTask[eTaskId]->hQueue	     = mcpf_mem_alloc(hMcpf, (McpU16)(sizeof(handle_t) * (uNum_of_queues + 1)));
	pMcpf->tTask[eTaskId]->fQueueCb	     = mcpf_mem_alloc(hMcpf, (McpU16)(sizeof(mcpf_msg_handler_cb) * (uNum_of_queues + 1)));
    pMcpf->tTask[eTaskId]->hQueCbHandler = mcpf_mem_alloc(hMcpf, (McpU16)(sizeof(handle_t) * (uNum_of_queues + 1)));
	pMcpf->tTask[eTaskId]->bQueueFlag    = mcpf_mem_alloc(hMcpf, (McpU16)(sizeof(McpBool) * (uNum_of_queues + 1)));
	/* Set all 'bQueueFlag' fields to be disabled.	*/
	for(i = 0; i <= uNum_of_queues; i++)
	    pMcpf->tTask[eTaskId]->bQueueFlag[i] = MCP_FALSE;

	/* Create Timer's Queue */
	pMcpf->tTask[eTaskId]->hQueue[0] = mcpf_que_Create(hMcpf, QUE_MAX_LIMIT, 
														MCPF_FIELD_OFFSET(TmcpfMsg, tMsgQNode));
	pMcpf->tTask[eTaskId]->fQueueCb[0] = mcpf_handleTimer;
	pMcpf->tTask[eTaskId]->hQueCbHandler[0] = hMcpf;
	pMcpf->tTask[eTaskId]->bQueueFlag[0] = MCP_TRUE;
	

	/* Allocate Critical section & signaling object */
	snprintf(critSecObjName, (sizeof(critSecObjName)) - 1, "Task_%d_critSecObj", eTaskId); /* KlockWork Changes - changed sprintf to snprintf */
	mcpf_critSec_CreateObj(hMcpf, critSecObjName, &(pMcpf->tTask[eTaskId]->hCritSecObj));
    pMcpf->tTask[eTaskId]->hSignalObj = os_sigobj_create(pMcpf->hPla);

        /* Set Event Handler */
	pMcpf->tTask[eTaskId]->uEvntBitmap = 0;
	pMcpf->tTask[eTaskId]->fEvntCb = fEvntCb;
    pMcpf->tTask[eTaskId]->hEvtCbHandler = hEvtCbHandle;

	/* Set Destroy Flag to FALSE */
	pMcpf->tTask[eTaskId]->bDestroyTask = MCP_FALSE;

	/* Set task's Send Cb function */
	pMcpf->tClientsTable[eTaskId].fCb = (tClientSendCb)mcpf_EnqueueMsg;
	pMcpf->tClientsTable[eTaskId].fPriorityCb = (tClientSendCb)mcpf_RequeueMsg;
	pMcpf->tClientsTable[eTaskId].hCb = hMcpf;

	if (MCP_HAL_STRING_StrLen(pTaskName) < MCPF_TASK_NAME_MAX_LEN)
	{
	    MCP_HAL_STRING_StrCpy (pMcpf->tTask[eTaskId]->cName, pTaskName);
	}
	else
	{
	    /* task name string is too long, truncate it */
	    mcpf_mem_copy (hMcpf, pMcpf->tTask[eTaskId]->cName, (McpU8 *) pTaskName, MCPF_TASK_NAME_MAX_LEN - 1);
	    pMcpf->tTask[eTaskId]->cName[MCPF_TASK_NAME_MAX_LEN - 1] = 0; /* line termination */
	}

	/* Create Thread */
	pMcpf->tTask[eTaskId]->hOsTaskHandle = os_createThread(NULL, 0, mcpf_TaskProc, &eTaskId, 0, NULL);

	return RES_COMPLETE;
}

/** 
 * \fn     mcpf_DestroyTask
 * \brief  Destroy MCPF
 * 
 * This function will be used to free all the driver resources 
 * and to Unregister from MCPF.
 * 
 * \note
 * \param	hMcpf     - MCPF handler.
 * \param	eTask_id - a static (constant) identification of the driver.
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_DestroyTask
 */ 
EMcpfRes	mcpf_DestroyTask (handle_t hMcpf, EmcpTaskId eTask_id)
{
	Tmcpf *pMcpf = (Tmcpf *)hMcpf;

	/* Set Destroy Flag to TRUE and set signal */
	pMcpf->tTask[eTask_id]->bDestroyTask = MCP_TRUE;
	os_sigobj_set(pMcpf->hPla, pMcpf->tTask[eTask_id]->hSignalObj);

	return RES_COMPLETE;
}

/** 
 * \fn     mcpf_RegisterTaskQ
 * \brief  Destroy MCPF
 * 
 * In order for the driver to be able to receive messages, 
 * it should register its message handler callbacks to the MCPF.
 * 
 * \note
 * \param	hMcpf - MCPF handler. 
 * \param	eTaskId - Task ID.
 * \param	uQueueId - Queue Index that the callback is related to.
 * \param	fCb - the message handler callback function.
 * \param	hCb - The callback function's handler.
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_RegisterTaskQ
 */
EMcpfRes	mcpf_RegisterTaskQ (handle_t hMcpf, EmcpTaskId eTaskId, McpU8 uQueueId, 
								mcpf_msg_handler_cb	fCb, handle_t hCb)
{
	Tmcpf *pMcpf = (Tmcpf *)hMcpf;

	/* Check if 'uQueueId' is valid */
	if( (uQueueId == 0) || (uQueueId >= pMcpf->tTask[eTaskId]->uNumOfQueues) )
	{
		MCPF_REPORT_ERROR(hMcpf, MCPF_MODULE_LOG, ("mcpf_RegisterTaskQ: Queue ID is invalid!"));
		return RES_ERROR;
	}

	/* Register Queue */
	pMcpf->tTask[eTaskId]->hQueue[uQueueId] = mcpf_que_Create(hMcpf, QUE_MAX_LIMIT, 
																MCPF_FIELD_OFFSET(TmcpfMsg, tMsgQNode));
	pMcpf->tTask[eTaskId]->fQueueCb[uQueueId] = fCb;
	pMcpf->tTask[eTaskId]->hQueCbHandler[uQueueId] = hCb;

	return RES_COMPLETE;
}

/** 
 * \fn     mcpf_UnregisterTaskQ
 * \brief  Destroy MCPF
 * 
 * This function un-registers the message handler Cb for the specified queue.
 * 
 * \note
 * \param	hMcpf - MCPF handler. 
 * \param	eTaskId - Task ID.
 * \param	uQueueId - Queue Index that the callback is related to.
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_UnregisterTaskQ
 */ 
EMcpfRes	mcpf_UnregisterTaskQ (handle_t hMcpf, EmcpTaskId eTaskId, McpU8 uQueueId)
{
	Tmcpf	*pMcpf = (Tmcpf *)hMcpf;

	/* Check if 'uQueueId' is valid */
	if( (uQueueId == 0) || (uQueueId >= pMcpf->tTask[eTaskId]->uNumOfQueues) )
	{
	    MCPF_REPORT_ERROR(hMcpf, MCPF_MODULE_LOG, ("mcpf_UnregisterTaskQ: Queue ID is invalid!"));
	    return RES_ERROR;
	}

	/* Unregister Queue */
	mcpf_que_Destroy(pMcpf->tTask[eTaskId]->hQueue[uQueueId]);

	return RES_COMPLETE;
}

/** 
 * \fn     mcpf_SetTaskPriority
 * \brief  Set the task's priority
 * 
 * This function will set the priority of the given task.
 * 
 * \note
 * \param	hMcpf     - MCPF handler.
 * \param	eTask_id - Task ID.
 * \param	sPriority - Priority to set.
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_SetTaskPriority
 */ 
EMcpfRes	mcpf_SetTaskPriority (handle_t hMcpf, EmcpTaskId eTask_id, McpInt sPriority)
{
	Tmcpf		*pMcpf = (Tmcpf *)hMcpf;
	TMcpfTask   	*pTask = pMcpf->tTask[eTask_id];
	
	return os_SetTaskPriority(pTask->hOsTaskHandle, sPriority);
}

/** Internal Functions **/

#ifdef MCP_STU_ENABLE
/* TODO - FUTURE USE: Implement Function */
/** 
 * \fn     mcpf_ccmCmdHandler 
 * \brief  CCM command Handler
 * 
 * Handler On/Off complete commands sent to the CCM
 * 
 * \note
 * \param	hMcpf     - handle to MCPF
 * \param	tMsg   	- Massage to handle
 * \return 	None
 * \sa     	mcpf_ccmCmdHandler
 
void			mcpf_ccmCmdHandler(handle_t hMcpf,  TmcpfMsg *tMsg)
{
	MCPF_UNUSED_PARAMETER(hMcpf);
	MCPF_UNUSED_PARAMETER(tMsg);
}
*/
#endif /* ifdef MCP_STU_ENABLE */

/** 
 * \fn     mcpf_TaskProc 
 * \brief  Task main loop
 * 
 * Wait on task signaling object and having received it process event bitmask and 
 * task queues
 * 
 * \note
 * \param	hMcpf     - handle to OS Framework
 * \param	eTaskId   - task ID
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_SendMsg, mcpf_SetEvent
 */
EMcpfRes	mcpf_TaskProc (handle_t	hMcpf, EmcpTaskId	eTaskId)
{
	Tmcpf		*pMcpf = (Tmcpf	*) hMcpf;
	TMcpfTask   *pTask = pMcpf->tTask[ eTaskId ];
	McpU32		uEvent = 0;
	EMcpfRes  	res;
	McpU32		uQindx;
	TmcpfMsg	*pMsg;

	CL_TRACE_TASK_DEF();

#ifndef MCP_STU_ENABLE
	MCP_HAL_LOG_SetThreadName(pMcpf->tTask[eTaskId]->cName);
#endif /* ifndef MCP_STU_ENABLE */

	while (pTask->bDestroyTask != MCP_TRUE)
	{
        res = os_sigobj_wait (pMcpf->hPla, pTask->hSignalObj);

	    CL_TRACE_TASK_START();

	    if (res == RES_OK)
	    {
#ifdef DEBUG
		    MCPF_REPORT_INFORMATION(hMcpf, NAVC_MODULE_LOG, ("mcpf_TaskProc: Task #%d was invoked", eTaskId));
#endif


			/* KlockWork Changes - while condition check for False, so this cannot be True here */
			/*
			if(pTask->bDestroyTask == MCP_TRUE)
		    {
			    MCPF_REPORT_INFORMATION(hMcpf, NAVC_MODULE_LOG, ("mcpf_TaskProc: Task #%d about to destroy...", eTaskId));
			    break;
		    }
		    */

		    /* Check event bitmap and invoke task event handler if event bitmap is set */
		    mcpf_critSec_Enter (hMcpf, pTask->hCritSecObj, MCPF_INFINIT);
		    if (pTask->uEvntBitmap && pTask->fEvntCb)
		    {
			    uEvent = pTask->uEvntBitmap;
			    pTask->uEvntBitmap = 0;
		    }
		    mcpf_critSec_Exit (hMcpf, pTask->hCritSecObj);

		    if (uEvent)
		    {
			    pTask->fEvntCb (pTask->hEvtCbHandler, uEvent);
		    }

		    /* Check event queues and invoke task queue event handler if there is a message */
		    for (uQindx = 0; uQindx < pTask->uNumOfQueues; uQindx++)
		    {
			    do
			    {
				    mcpf_critSec_Enter (hMcpf, pTask->hCritSecObj, MCPF_INFINIT);
				    if (pTask->bQueueFlag[ uQindx ])
				    {
					    pMsg = (TmcpfMsg *) que_Dequeue (pTask->hQueue[ uQindx ]);
				    }
				    else
				    {
					    mcpf_critSec_Exit (hMcpf, pTask->hCritSecObj);
					    break;
				    }
				    mcpf_critSec_Exit (hMcpf, pTask->hCritSecObj);

				    if (pMsg)
				    {
#ifdef DEBUG
					    MCPF_REPORT_INFORMATION(hMcpf, MCPF_MODULE_LOG, 
								    ("DEQUEUED MSG FOR TASK #%d, FROM QUEUE #%d", eTaskId, uQindx));
#endif
						
					    pTask->fQueueCb[ uQindx ] (pTask->hQueCbHandler[ uQindx ], pMsg);
				    }
				    else
				    {
					    break;
				    }
			    } while (pMsg);
            }
		}
    	else
	    {
		    MCPF_REPORT_ERROR(hMcpf, NAVC_MODULE_LOG, ("mcpf_TaskProc: fail on return from 'os_sigobj_wait'"));
		    return RES_ERROR;
	    }

	    CL_TRACE_TASK_END("mcpf_TaskProc", CL_TRACE_CONTEXT_TASK, pTask->cName, "MCPF");
    }

	/* Unregister task's Send Cb function */
	pMcpf->tClientsTable[eTaskId].fCb = NULL;
	pMcpf->tClientsTable[eTaskId].hCb = NULL;

	/*  Free Critical section & signaling object */
	mcpf_critSec_DestroyObj(hMcpf, &pTask->hCritSecObj);
	os_sigobj_destroy(pMcpf->hPla, pTask->hSignalObj);

	/* Destroy Timer's Queue */
	mcpf_que_Destroy(pTask->hQueue[0]);

	/* Free Queues */
	mcpf_mem_free(hMcpf, pTask->bQueueFlag);
	mcpf_mem_free(hMcpf, pTask->fQueueCb);
	mcpf_mem_free(hMcpf, pTask->hQueue);
	mcpf_mem_free(hMcpf, pTask->hQueCbHandler);			

	/*  Free task handler */
	mcpf_mem_free(hMcpf, pTask);
				
	/* Exit Thread */
	os_exitThread(0);
				
	return RES_OK;
}

