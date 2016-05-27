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

/** \file   queue.c 
 *  \brief  This module provides generic queueing services, including enqueue, dequeue
 *            and requeue of any object that contains TQueNodeHdr in its structure.                                  
 *
 *  \see    queue.h
 */

#include "mcpf_queue.h"
#include "mcpf_report.h"
#include "mcpf_mem.h"
#include "mcp_utils_dl_list.h"



/* Queue structure */
typedef struct 
{
    MCP_DL_LIST_Node tHead;          /* The queue first node */
    McpU32   		 uCount;         /* Current number of nodes in queue */
    McpU32   		 uLimit;         /* Upper limit of nodes in queue */
    McpU32   		 uMaxCount;      /* Maximum uCount value (for debug) */
    McpU32   		 uOverflow;      /* Number of overflow occurrences - couldn't insert node (for debug) */
    McpU32   		 uNodeHeaderOffset; /* Offset of NodeHeader field from the entry of the queued item */
	handle_t   		 hMcpf;
} TQueue;	

/*
 *              EXTERNAL  FUNCTIONS 
 *        =============================== 
 */

/** 
 * \fn     que_Create 
 * \brief  Create a queue. 
 * 
 * Allocate and init a queue object.
 * 
 * \note    
 * \param  hOs               - Handle to Os Abstraction Layer
 * \param  hReport           - Handle to report module
 * \param  uLimit            - Maximum items to store in queue
 * \param  uNodeHeaderOffset - Offset of NodeHeader field from the entry of the queued item.
 * \return Handle to the allocated queue 
 * \sa     que_Destroy
 */ 
handle_t que_Create (handle_t hMcpf, McpU32 uLimit, McpU32 uNodeHeaderOffset)
{
	TQueue *pQue;

	if(!hMcpf)
	{
		MCPF_OS_REPORT (("Mcpf handler is NULL\n"));
		return NULL;
	}

	/* allocate queue module */
	pQue = mcpf_mem_alloc (hMcpf, sizeof(TQueue));
	
	if (!pQue)
	{
		MCPF_OS_REPORT (("Error allocating the Queue Module\n"));
		return NULL;
	}
	
    mcpf_mem_zero (hMcpf, pQue, sizeof(TQueue));

	MCP_DL_LIST_InitializeHead (&pQue->tHead);

	/* Set the Queue parameters */
    pQue->hMcpf           = hMcpf;
	pQue->uLimit            = uLimit;
	pQue->uNodeHeaderOffset = uNodeHeaderOffset;

	return (handle_t)pQue;
}


/** 
 * \fn     que_Destroy
 * \brief  Destroy the queue. 
 * 
 * Free the queue memory.
 * 
 * \note   The queue's owner should first free the queued items!
 * \param  hQue - The queue object
 * \return RES_COMPLETE on success or RES_ERROR on failure 
 * \sa     que_Create
 */ 
EMcpfRes que_Destroy (handle_t hQue)
{
    TQueue *pQue = (TQueue *)hQue;

    /* Alert if the queue is unloaded before it was cleared from items */
    if (pQue->uCount)
    {
        MCPF_REPORT_ERROR(pQue->hMcpf, QUEUE_MODULE_LOG, ("que_Destroy() Queue Not Empty!!"));
    }
    /* free Queue object */
	mcpf_mem_free (pQue->hMcpf, pQue);
	
    return RES_COMPLETE;
}

/** 
 * \fn     que_Enqueue
 * \brief  Enqueue an item 
 * 
 * Enqueue an item at the queue's tail
 * 
 * \note   
 * \param  hQue   - The queue object
 * \param  hItem  - Handle to queued item
 * \return RES_COMPLETE if item was queued, or RES_ERROR if not queued due to overflow
 * \sa     que_Dequeue, que_Requeue
 */ 
EMcpfRes que_Enqueue (handle_t hQue, handle_t hItem)
{
	TQueue      	 *pQue = (TQueue *)hQue;
    MCP_DL_LIST_Node *pQueNodeHdr;  /* the Node-Header in the given item */

	/* Check queue limit */
	if(pQue->uCount < pQue->uLimit)
	{
        /* Find NodeHeader in the given item */
        pQueNodeHdr = (MCP_DL_LIST_Node *)((McpU8*)hItem + pQue->uNodeHeaderOffset);

        /* Enqueue item to the queue tail and increment items counter */
		MCP_DL_LIST_InsertTail (&pQue->tHead, pQueNodeHdr);

		pQue->uCount++;

#ifdef TRAN_DBG
		if (pQue->uCount > pQue->uMaxCount)
        {
			pQue->uMaxCount = pQue->uCount;
        }
        MCPF_REPORT_INFORMATION (pQue->hMcpf, QUEUE_MODULE_LOG,
            ("que_Enqueue(): enqueued successfully to queue (handle 0x%08x), count %d\n",
            (McpU32)hQue, pQue->uCount));
#endif
		
		return RES_COMPLETE;
	}
	
	/* 
	 *  Queue is overflowed, return RES_ERROR.
	 */
#ifdef TRAN_DBG
	pQue->uOverflow++;
	MCPF_REPORT_WARNING (pQue->hMcpf, QUEUE_MODULE_LOG, ("que_Enqueue(): Queue Overflow\n"));
#endif
	
	return RES_ERROR;
}


/** 
 * \fn     que_Dequeue
 * \brief  Dequeue an item 
 * 
 * Dequeue an item from the queue's head
 * 
 * \note   
 * \param  hQue - The queue object
 * \return pointer to dequeued item or NULL if queue is empty
 * \sa     que_Enqueue, que_Requeue
 */ 
handle_t que_Dequeue (handle_t hQue)
{
    TQueue   			*pQue = (TQueue *)hQue;
	handle_t 			hItem;
	MCP_DL_LIST_Node 	*pNode;
 
    if (pQue->uCount)
    {
		/* Queue is not empty, take packet from the queue head and 
		 * find pointer to the node entry 
		 */
		pNode = MCP_DL_LIST_RemoveHead (&pQue->tHead);
		hItem = (handle_t)((McpU8 *)pNode - pQue->uNodeHeaderOffset); 
        pQue->uCount--;
#ifdef TRAN_DBG
		MCPF_REPORT_INFORMATION (pQue->hMcpf, QUEUE_MODULE_LOG,
                    ("que_Dequeue(): dequeued successfully from queue (handle 0x%08x), remain %d\n",
                    (McpU32)hQue, pQue->uCount));
#endif
        return (hItem);
    }
    
	/* Queue is empty */
#ifdef DEBUG
    MCPF_REPORT_INFORMATION (pQue->hMcpf, QUEUE_MODULE_LOG, ("que_Dequeue(): Queue is empty\n"));
#endif
    return NULL;
}


/** 
 * \fn     que_Requeue
 * \brief  Requeue an item 
 * 
 * Requeue an item at the queue's head
 * 
 * \note   
 * \param  hQue   - The queue object
 * \param  hItem  - Handle to queued item
 * \return RES_COMPLETE if item was queued, or RES_ERROR if not queued due to overflow
 * \sa     que_Enqueue, que_Dequeue
 */ 
EMcpfRes que_Requeue (handle_t hQue, handle_t hItem)
{
    TQueue      		*pQue = (TQueue *)hQue;
    MCP_DL_LIST_Node 	*pQueNodeHdr;  /* the NodeHeader in the given item */

    /* 
	 *  If queue's limits not exceeded add the packet to queue's tail and return RES_COMPLETE 
	 */
    if (pQue->uCount < pQue->uLimit)
	{
        /* Find NodeHeader in the given item */
        pQueNodeHdr = (MCP_DL_LIST_Node *)((McpU8*)hItem + pQue->uNodeHeaderOffset);

        /* Enqueue item and increment items counter */
		MCP_DL_LIST_InsertHead (&pQue->tHead, pQueNodeHdr);
		pQue->uCount++;

#ifdef TRAN_DBG
		if (pQue->uCount > pQue->uMaxCount)
			pQue->uMaxCount = pQue->uCount;
		MCPF_REPORT_INFORMATION (pQue->hMcpf, QUEUE_MODULE_LOG, ("que_Requeue(): Requeued successfully\n"));
#endif

		return RES_COMPLETE;
    }
	/* 
	 *  Queue is overflowed, return RES_ERROR.
	 *  Note: This is not expected in the current design, since Tx packet may be requeued
	 *          only right after it was dequeued in the same context so the queue can't be full.
	 */
#ifdef TRAN_DBG
    pQue->uOverflow++;
	MCPF_REPORT_ERROR (pQue->hMcpf, QUEUE_MODULE_LOG, ("que_Requeue(): Queue Overflow\n"));
#endif
    
    return RES_ERROR;
}


/** 
 * \fn     que_Size
 * \brief  Return queue size 
 * 
 * Return number of items in queue.
 * 
 * \note   
 * \param  hQue - The queue object
 * \return McpU32 - the items count
 * \sa     
 */ 
McpU32 que_Size (handle_t hQue)
{
    TQueue *pQue = (TQueue *)hQue;
 
    return (pQue->uCount);
}

	
/** 
 * \fn     que_Print
 * \brief  Print queue status
 * 
 * Print the queue's parameters (not the content).
 * 
 * \note   
 * \param  hQue - The queue object
 * \return void
 * \sa     
 */ 

#ifdef TRAN_DBG

void que_Print(handle_t hQue)
{
	TQueue *pQue = (TQueue *)hQue;

    MCPF_OS_REPORT(("que_Print: Count=%u MaxCount=%u Limit=%u Overflow=%u NodeHeaderOffset=%u Next=0x%x Prev=0x%x\n",
                    pQue->uCount, pQue->uMaxCount, pQue->uLimit, pQue->uOverflow, 
                    pQue->uNodeHeaderOffset, (McpU32)pQue->tHead.next, (McpU32)pQue->tHead.prev));
}

#endif /* TRAN_DBG */



