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


/** \file   msp_txnpool.h 
 *  \brief  Transaction structure and buffer pool handling
 *
 *  \see    msp_txnpool.c
 */


#ifndef _MCP_POOL_API_H_
#define _MCP_POOL_API_H_

#define POOL_MAX_BUF_SIZE 		1028

#define POOL_RX_MAX_BUF_SIZE 	POOL_MAX_BUF_SIZE


/** 
 * \fn     mcpf_txnPool_Create
 * \brief  Create pool
 * 
 * Allocate memory for pool
 * 
 * \note
 * \param	hMcpf   - pointer to OS Framework
 * \return 	handle_t hPool
 * \sa     	mcpf_txnPool_Create
 */ 
handle_t 	mcpf_txnPool_Create (handle_t  hMcpf);


/** 
 * \fn     mcpf_txnPool_Destroy
 * \brief  Destroy pool
 * 
 * Allocate memory for pool according to specified configuration
 * 
 * \note
 * \param	hMcpf   - pointer to OS Framework
 * \return 	status of operation OK or Error
 * \sa     	mcpf_txnPool_Destroy
 */ 
EMcpfRes 	mcpf_txnPool_Destroy (handle_t hMcpf);


/** 
 * \fn     mcpf_txnPool_Alloc
 * \brief  Allocate transaction structure
 * 
 * Allocate transaction structure
 * 
 * \note
 * \param	hMcpf   - pointer to OS Framework
 * \return 	pointer to allocated TTxnStruct or NULL in the case of error
 * \sa     	mcpf_txnPool_Alloc 
 */ 
TTxnStruct * mcpf_txnPool_Alloc (handle_t hMcpf);

/** 
 * \fn     mcpf_txnPool_AllocBuf
 * \brief  Allocate transaction structure and buffer
 * 
 * Allocate transaction structure and buffer of specified size
 * 
 * \note
 * \param	hMcpf   - pointer to OS Framework
 * \param	size   - buffer size
 * \return 	pointer to allocated TTxnStruct or NULL in the case of error
 * \sa     	mcpf_txnPool_AllocBuf 
 */ 
TTxnStruct * mcpf_txnPool_AllocBuf (handle_t hMcpf, McpU16 size);


/** 
 * \fn     mcpf_txnPool_AllocNBuf
 * \brief  Allocate transaction structure and buffer
 * 
 * Allocate transaction structure and buffers of specified size
 * 
 * \note
 * \param	hMcpf   - pointer to OS Framework
 * \param	uBufN  - number of required buffers, is to be less or equal to MAX_XFER_BUFS
 * \param	aSizeN - array of buffer sizes, number of items is to be equal to uBufN
 * \return 	pointer to allocated TTxnStruct or NULL in the case of error
 * \sa     	mcpf_txnPool_AllocNBuf 
 */ 
TTxnStruct * mcpf_txnPool_AllocNBuf (handle_t hMcpf, McpU32 uBufN, McpU16 aSizeN[]);

/** 
 * \fn     mcpf_txnPool_FreeBuf
 * \brief  Free transaction structure and buffer[s]
 * 
 * Free transaction structure and buffer or buffers associated with txn structure
 * 
 * \note
 * \param	hMcpf   - pointer to OS Framework
 * \param	pTxn  - pointer to TTxnStruct 
 * \return 	void
 * \sa     	mcpf_txnPool_FreeBuf 
 */ 
void mcpf_txnPool_FreeBuf (handle_t hMcpf, TTxnStruct * pTxn);

/** 
 * \fn     mcpf_txnPool_Free
 * \brief  Free transaction structure
 * 
 * Free transaction structure only without buffer or buffers associated with txn structure
 * 
 * \note
 * \param	hMcpf   - pointer to OS Framework
 * \param	pTxn  - pointer to TTxnStruct 
 * \return 	void
 * \sa     	mcpf_txnPool_Free 
 */ 
void mcpf_txnPool_Free (handle_t hMcpf, TTxnStruct * pTxn);


#endif  /* _MCP_POOL_API_H_ */
