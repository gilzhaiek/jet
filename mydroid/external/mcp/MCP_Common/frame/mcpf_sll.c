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
#include "mcpf_sll.h"
#include "mcpf_report.h"
#include "mcpf_mem.h"


/** APIs **/

/** 
 * \fn     mcpf_SLL_Create
 * \brief  Create a sorted linked list
 * 
 * This function creates a sorted linked list
 * 
 * \note
 * \param	hMcpf - MCPF handler.
 * \param	uLimit - max list length.
 * \param	uNodeHeaderOffset - Offset of node's header field in stored structure.
 * \param	uSortByFeildOffset - Offset of the field to sort by, in stored structure.
 * \param	tSortType - sort type, UP or DOWN.
 * \return 	List handler.
 * \sa     	mcpf_SLL_Create
 */ 
handle_t mcpf_SLL_Create (handle_t hMcpf, McpU32 uLimit, McpU32 uNodeHeaderOffset, 
							McpU32 uSortByFeildOffset, TSllSortType	tSortType)
{
    mcpf_SLL   *pSortedList;
    if (!hMcpf) 
	{
		MCPF_OS_REPORT(("Mcpf handler is NULL\n"));
		return NULL;	
	}
        
    /* Allocate memory for the sorted Linked List */
    pSortedList = mcpf_mem_alloc(hMcpf, sizeof(mcpf_SLL));

    if (NULL == pSortedList)
	{
        MCPF_REPORT_ERROR(hMcpf, MCPF_MODULE_LOG, ("mcpf_mem_alloc returned NULL\n"));
		return NULL;
	}
    
    /* Initialize the memory block to zero */
    mcpf_mem_zero(hMcpf, pSortedList, sizeof(mcpf_SLL));

    /* Initialize the list header */
	MCP_DL_LIST_InitializeHead (&pSortedList->tHead);

    /* Set the List paramaters */
	pSortedList->hMcpf = hMcpf;                  
	pSortedList->tSortType = tSortType;
	pSortedList->uSortByFieldOffset = (McpU16)uSortByFeildOffset;
	pSortedList->uNodeHeaderOffset  = (McpU16)uNodeHeaderOffset;
	pSortedList->uOverflow = 0;
	pSortedList->uLimit = (McpU16)uLimit;
	pSortedList->uCount = 0;
        
    return (handle_t)pSortedList;
}

/** 
 * \fn     mcpf_SLL_Destroy
 * \brief  Destroy the sorted linked list
 * 
 * This function frees the resources of the SLL.
 * 
 * \note
 * \param	hList - List handler.
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_SLL_Destroy
 */ 
EMcpfRes mcpf_SLL_Destroy (handle_t hList)
{
    mcpf_SLL   *pSortedList;    
    handle_t     *hMcpf;

    /* Get the list handler from hList */
    pSortedList = (mcpf_SLL*) hList;    
    
	hMcpf = pSortedList->hMcpf;
    
    if (pSortedList-> uCount >=1) /* List is not Empty */
	{
       MCPF_REPORT_ERROR(pSortedList->hMcpf, MCPF_MODULE_LOG, ("mcpf_SLL_Destroy: List not empty !!!, before calling me empty the list\n"));
       mcpf_mem_free(hMcpf, pSortedList);
       return RES_ERROR;
	}
	else
	{
		mcpf_mem_free(hMcpf, pSortedList);
	}

    return RES_OK;  
}

/** 
 * \fn     mcpf_SLL_Insert
 * \brief  Insert item to list
 * 
 * This function inserts the item to the list in its correct place.
 * 
 * \note
 * \param	hList - List handler.
 * \param	hItem - item to put in the List.
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_SLL_Insert
 */ 
EMcpfRes mcpf_SLL_Insert (handle_t hList, handle_t hItem)
{
    mcpf_SLL     		*pSortedList = (mcpf_SLL*) hList;
    MCP_DL_LIST_Node  	*pItemNodeHeader, *pListNodeHeader;
    McpU32       		uItemSortBy_field, uListSortBy_field;
    MCP_DL_LIST_Node 	*pNode;

    pSortedList = (mcpf_SLL*) hList;

	/* If list is not full */
    if (pSortedList->uCount < pSortedList->uLimit)
	{
		/* Get the Node Header in the given Item */
		pItemNodeHeader = (MCP_DL_LIST_Node*) ((McpU8 *)hItem + pSortedList->uNodeHeaderOffset);

		/* Get content of field, to sort by, of the new Item */
		uItemSortBy_field = * ( (McpU32*)((McpU8*)hItem + pSortedList->uSortByFieldOffset));

		/* Get the address of the list header */
		pListNodeHeader = &pSortedList->tHead;      

		MCP_DL_LIST_ITERATE(pListNodeHeader, pNode)
		{
			uListSortBy_field = *((McpU32*)((McpU8*)pNode - 
											pSortedList->uNodeHeaderOffset + 
											pSortedList->uSortByFieldOffset));

			if ( ((SLL_UP   == pSortedList->tSortType) && (uItemSortBy_field < uListSortBy_field)) ||
				 ((SLL_DOWN == pSortedList->tSortType) && (uItemSortBy_field > uListSortBy_field)) )
			{
				break;
			}
		}

		MCP_DL_LIST_InsertNode (pItemNodeHeader, pNode->prev, pNode);

		pSortedList->uCount++;

	#ifdef _DEBUG
		if (pSortedList->uCount > pSortedList->uMaxCount)
		{
			pSortedList->uMaxCount = pSortedList->uCount;
		}		
	#endif
	}
	else
	{
		MCPF_REPORT_ERROR(pSortedList->hMcpf, MCPF_MODULE_LOG, ("mcpf_SLL_Insert: List is FULL \n"));
		return RES_ERROR;
	}

    return RES_OK;
}

/** 
 * \fn     mcpf_SLL_Remove
 * \brief  Remove item from list
 * 
 * This function delete requested item from list.
 * 
 * \note
 * \param	hList - List handler.
 * \param	hItem - item to remove from the List.
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_SLL_Remove
 */ 
EMcpfRes mcpf_SLL_Remove (handle_t hList, handle_t hItem)
{
    mcpf_SLL     		*pSortedList = (mcpf_SLL *)hList;
    MCP_DL_LIST_Node  	*pItemHdr, *pListHdr, *pNode;
    
    pListHdr = &(pSortedList->tHead);

	pItemHdr = (MCP_DL_LIST_Node*) ((McpU8 *)hItem + pSortedList->uNodeHeaderOffset);

    /* Check if the list is not empty */
    if(pSortedList->uCount)
    {
		/* loop list item until item is found & list is traversed till end */
		MCP_DL_LIST_ITERATE(pListHdr, pNode)
		{
			if (pNode == pItemHdr)
			{
				break;
			}
		}
		if (pNode == pItemHdr )  /* Verify if the item was found */
		{
			MCP_DL_LIST_RemoveNode (pNode);

			/* Reduce value of count in the list header structure */
			pSortedList->uCount = (McpU16)((pSortedList->uCount) - 1);
			return RES_OK;
		}
		else /* Item was not found */
		{
			MCPF_REPORT_ERROR(pSortedList->hMcpf, MCPF_MODULE_LOG, ("mcpf_SLL_Remove: ITEM WAS NOT FOUND\n"));
			return RES_ERROR;
		}
	}
	else
	{
		MCPF_REPORT_ERROR(pSortedList->hMcpf, MCPF_MODULE_LOG, ("mcpf_SLL_Remove: List is EMPTY \n"));
		return RES_ERROR;
	}
}

/** 
 * \fn     mcpf_SLL_Retrieve
 * \brief  Retrieve item from list
 * 
 * This function retrieves item from head of the list and return it.
 * 
 * \note
 * \param	hList - List handler.
 * \return 	Handler of the item from the head of the list.
 * \sa     	mcpf_SLL_Retrieve
 */ 
handle_t mcpf_SLL_Retrieve (handle_t hList)
{
   mcpf_SLL     	*pSortedList = (mcpf_SLL*) hList;
   MCP_DL_LIST_Node *pNode, *pListHdr;
   McpU8			*pItem;

   pListHdr = &(pSortedList->tHead);

   if(pSortedList->uCount)
   {
	   pNode = MCP_DL_LIST_RemoveHead (pListHdr);
	   pItem = (McpU8 *)pNode - pSortedList->uNodeHeaderOffset;

	   /* Reduce value of count in the list header structure */
       pSortedList->uCount = (McpU16)((pSortedList->uCount) - 1);
	   return ((handle_t)pItem);
   }
   else /* List is empty */
   {
	   MCPF_REPORT_ERROR(pSortedList->hMcpf, MCPF_MODULE_LOG, ("mcpf_SLL_Retrieve: List is EMPTY\n"));
       return NULL;
   }
}

/** 
 * \fn     mcpf_SLL_Get
 * \brief  Get item from list
 * 
 * This function return's the first item from head of the list.
 * 
 * \note
 * \param	hList - List handler.
  * \return 	Handler of the item from the head of the list.
 * \sa     	mcpf_SLL_Get
 */ 
handle_t mcpf_SLL_Get (handle_t hList)
{
   mcpf_SLL     	*pSortedList = (mcpf_SLL*) hList;     
   MCP_DL_LIST_Node *pNode, *pListHdr;
   McpU8			*pItem;
   
   pListHdr = &(pSortedList->tHead);
            
   if(((mcpf_SLL*)hList)->uCount)
   {
	   pNode = MCP_DL_LIST_GetHead (pListHdr);
	   pItem = (McpU8 *)pNode - pSortedList->uNodeHeaderOffset;
	   return ((handle_t)pItem);
   }
   else
   {
	   MCPF_REPORT_ERROR(pSortedList->hMcpf, MCPF_MODULE_LOG, ("mcpf_SLL_Get: List is EMPTY\n"));
	   return NULL;
   }   
}

/** 
 * \fn     mcpf_SLL_Size
 * \brief  Return number of items in the list
 * 
 * This function return's the the number of item in the list.
 * 
 * \note
 * \param	hList - List handler.
  * \return 	Number of item in the list.
 * \sa     	mcpf_SLL_Size
 */ 
McpU32 mcpf_SLL_Size (handle_t hList)
{
   return(((mcpf_SLL*)hList)->uCount);	
}
