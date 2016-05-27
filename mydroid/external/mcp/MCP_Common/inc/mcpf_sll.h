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

#ifndef __MCPF_SLL_H
#define __MCPF_SLL_H


#include "mcpf_defs.h"
#include "mcp_utils_dl_list.h"


/** Definitions **/
typedef enum
{
	SLL_UP,
	SLL_DOWN
} TSllSortType;


/** Structures **/

/* A List node header structure */                        
typedef MCP_DL_LIST_Node TSllNodeHdr; 

typedef struct 
{
    MCP_DL_LIST_Node 	tHead;          /* The List first node */
    McpU16  	 		uCount;         /* Current number of nodes in list */
    McpU16   			uLimit;         /* Upper limit of nodes in list */
    McpU16  		 	uMaxCount;      /* Maximum uCount value (for debug) */
    McpU16  		 	uOverflow;      /* Number of overflow occurrences - couldn't insert node (for debug) */
    McpU16  		 	uNodeHeaderOffset; /* Offset of NodeHeader field from the entry of the listed item */
    McpU16  		 	uSortByFieldOffset; /* Offset of the field you sort by from the entry of the listed item */
    TSllSortType		tSortType;
    handle_t   			hMcpf;
} mcpf_SLL;


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
handle_t mcpf_SLL_Create (handle_t 		hMcpf, 
						  McpU32 		uLimit, 
						  McpU32 		uNodeHeaderOffset, 
						  McpU32 		uSortByFeildOffset, 
						  TSllSortType	tSortType);

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
EMcpfRes mcpf_SLL_Destroy (handle_t hList);

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
EMcpfRes mcpf_SLL_Insert (handle_t hList, handle_t hItem);

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
EMcpfRes mcpf_SLL_Remove (handle_t hList, handle_t hItem);

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
handle_t mcpf_SLL_Retrieve (handle_t hList);

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
handle_t mcpf_SLL_Get (handle_t hList);

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
McpU32 mcpf_SLL_Size (handle_t hList);


#endif
