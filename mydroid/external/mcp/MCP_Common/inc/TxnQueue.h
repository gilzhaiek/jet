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

/** \file   TxnQueue.h 
 *  \brief  TxnQueue module API definition                                  
 *
 *  \see    TxnQueue.c
 */

#ifndef __TXN_QUEUE_API_H__
#define __TXN_QUEUE_API_H__


#include "TxnDefs.h"
#include "BusDrv.h"



/************************************************************************
 * Defines
 ************************************************************************/


/************************************************************************
 * Macros
 ************************************************************************/


/************************************************************************
 * Types
 ************************************************************************/


/************************************************************************
 * Functions
 ************************************************************************/
handle_t   	txnQ_Create (const handle_t hMcpf);

EMcpfRes  txnQ_Destroy (handle_t hTxnQ);

void        txnQ_Init (handle_t hTxnQ, const handle_t hMcpf);

EMcpfRes  txnQ_ConnectBus (handle_t 				hTxnQ, 
							 const TBusDrvCfg 		*pBusDrvCfg,
							 const TTxnDoneCb 		fConnectCb,
							 const handle_t 		hConnectCb, 
							 const TI_BusDvrRxIndCb fRxIndCb,
							 const handle_t 		hRxIndHandle, 
							 const TI_TxnEventHandlerCb fEventHandlerCb,
							 const handle_t 		hEventHandle,
							 const handle_t 		hIfSlpMng);

EMcpfRes  txnQ_DisconnectBus (handle_t hTxnQ);

EMcpfRes  txnQ_Open (handle_t       		hTxnQ, 
                       const McpU32       	uFuncId, 
                       const McpU32       	uNumPrios, 
                       const TTxnQueueDoneCb fTxnQueueDoneCb,
                       const handle_t       hCbHandle);

void        txnQ_Close (handle_t  hTxnQ, const McpU32 uFuncId);

ETxnStatus  txnQ_Restart (handle_t hTxnQ, const McpU32 uFuncId);

void        txnQ_Run (handle_t hTxnQ, const McpU32 uFuncId);

void        txnQ_Stop (handle_t hTxnQ, const McpU32 uFuncId);

ETxnStatus  txnQ_Transact (handle_t hTxnQ, TTxnStruct *pTxn);

handle_t	txnQ_GetBusDrvHandle (const handle_t hTxnQ);

McpBool 	txnQ_IsQueueEmpty (const handle_t hTxnQ, const McpU32 uFuncId);

EMcpfRes    txnQ_ResetBus (handle_t hTxnQ, const McpU16 baudrate);

#ifdef TRAN_DBG
    void txnQ_PrintQueues (handle_t hTxnQ);
	void txnQ_testTx (handle_t hTxnQ, TTxnStruct *pTxn);
#endif



#endif /*__TXN_QUEUE_API_H__*/
