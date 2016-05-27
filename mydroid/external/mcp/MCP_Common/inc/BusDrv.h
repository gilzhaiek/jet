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

/** \file   BusDrv.h 
 *  \brief  Bus-Driver module API definition                                  
 *
 *  \see    SdioBusDrv.c, WspiBusDrv.c, mcs_uartBusDrv.c
 */

#ifndef __BUS_DRV_API_H__
#define __BUS_DRV_API_H__


#include "TxnDefs.h"
#include "mcpf_queue.h"


/************************************************************************
 * Defines
 ************************************************************************/
#define FIXED_BUSY_LEN           4
#define FIXED_BUSY_LEN_READ      FIXED_BUSY_LEN + 4   

#ifdef TI_STAND_ALONE_UART_BUSDRV
#define MAX_XFER_BUFS            1
#else
#define MAX_XFER_BUFS            4
#endif

#define TXN_STATUS_OK            0
#define TXN_STATUS_ERROR         1
#define TXN_STATUS_RECOVERY      2

#define TXN_DIRECTION_WRITE      0
#define TXN_DIRECTION_READ       1

#define TXN_HIGH_PRIORITY        0
#define TXN_LOW_PRIORITY         1
#define TXN_INC_ADDR             0
#define TXN_FIXED_ADDR           1
#define TXN_NON_SLEEP_ELP        1
#define TXN_SLEEP_ELP            0

/************************************************************************
 * Macros
 ************************************************************************/
/* Get field from TTxnStruct->uTxnParams */
#define TXN_PARAM_GET_PRIORITY(pTxn)            ( (pTxn->uTxnParams & 0x00000003) >> 0 )
#define TXN_PARAM_GET_FUNC_ID(pTxn)             ( (pTxn->uTxnParams & 0x0000000C) >> 2 )
#define TXN_PARAM_GET_DIRECTION(pTxn)           ( (pTxn->uTxnParams & 0x00000010) >> 4 )
#define TXN_PARAM_GET_FIXED_ADDR(pTxn)          ( (pTxn->uTxnParams & 0x00000020) >> 5 )
#define TXN_PARAM_GET_MORE(pTxn)                ( (pTxn->uTxnParams & 0x00000040) >> 6 )
#define TXN_PARAM_GET_SINGLE_STEP(pTxn)         ( (pTxn->uTxnParams & 0x00000080) >> 7 )
#define TXN_PARAM_GET_STATUS(pTxn)              ( (pTxn->uTxnParams & 0x00000F00) >> 8 )


#define TXN_PARAM_TXN_FLAG_BIT					16
#define TXN_PARAM_IFSLPMNG_OP_BIT				17
#define TXN_PARAM_CHAN_NUM_BIT					24

#define TXN_PARAM_TXN_FLAG_MASK					(0x01 << TXN_PARAM_TXN_FLAG_BIT)
#define TXN_PARAM_IFSLPMNG_OP_MASK				(0x07 << TXN_PARAM_IFSLPMNG_OP_BIT)
#define TXN_PARAM_CHAN_NUM_MASK					(0xFF << TXN_PARAM_CHAN_NUM_BIT)


#define TXN_PARAM_GET_TXN_FLAG(pTxn)            ( (pTxn->uTxnParams & TXN_PARAM_TXN_FLAG_MASK) >> TXN_PARAM_TXN_FLAG_BIT )
#define TXN_PARAM_GET_IFSLPMNG_OP(pTxn)         ( (pTxn->uTxnParams & TXN_PARAM_IFSLPMNG_OP_MASK) >> TXN_PARAM_IFSLPMNG_OP_BIT )
#define TXN_PARAM_GET_CHAN_NUM(pTxn)          	( (pTxn->uTxnParams & TXN_PARAM_CHAN_NUM_MASK) >> TXN_PARAM_CHAN_NUM_BIT )

/* Set field in TTxnStruct->uTxnParams */
#define TXN_PARAM_SET_PRIORITY(pTxn, uValue)    ( pTxn->uTxnParams = (pTxn->uTxnParams & ~0x00000003) | (uValue << 0 ) )
#define TXN_PARAM_SET_FUNC_ID(pTxn, uValue)     ( pTxn->uTxnParams = (pTxn->uTxnParams & ~0x0000000C) | (uValue << 2 ) )
#define TXN_PARAM_SET_DIRECTION(pTxn, uValue)   ( pTxn->uTxnParams = (pTxn->uTxnParams & ~0x00000010) | (uValue << 4 ) )
#define TXN_PARAM_SET_FIXED_ADDR(pTxn, uValue)  ( pTxn->uTxnParams = (pTxn->uTxnParams & ~0x00000020) | (uValue << 5 ) )
#define TXN_PARAM_SET_MORE(pTxn, uValue)        ( pTxn->uTxnParams = (pTxn->uTxnParams & ~0x00000040) | (uValue << 6 ) )
#define TXN_PARAM_SET_SINGLE_STEP(pTxn, uValue) ( pTxn->uTxnParams = (pTxn->uTxnParams & ~0x00000080) | (uValue << 7 ) )
#define TXN_PARAM_SET_STATUS(pTxn, uValue)      ( pTxn->uTxnParams = (pTxn->uTxnParams & ~0x00000F00) | (uValue << 8 ) )

#define TXN_PARAM_SET_TXN_FLAG(pTxn,uValue)  	( (pTxn)->uTxnParams = ((pTxn)->uTxnParams & ~TXN_PARAM_TXN_FLAG_MASK) | \
												(((uValue) << TXN_PARAM_TXN_FLAG_BIT) & TXN_PARAM_TXN_FLAG_MASK) )

#define TXN_PARAM_SET_IFSLPMNG_OP(pTxn,uValue)  ( (pTxn)->uTxnParams = ((pTxn)->uTxnParams & ~TXN_PARAM_IFSLPMNG_OP_MASK) | \
												(((uValue) << TXN_PARAM_IFSLPMNG_OP_BIT) & TXN_PARAM_IFSLPMNG_OP_MASK) )

#define TXN_PARAM_SET_CHAN_NUM(pTxn,uValue)  	( (pTxn)->uTxnParams = ((pTxn)->uTxnParams & ~TXN_PARAM_CHAN_NUM_MASK) | \
												(((uValue) << TXN_PARAM_CHAN_NUM_BIT) & TXN_PARAM_CHAN_NUM_MASK) )


#define TXN_PARAM_SET(pTxn, uPriority, uId, uDirection, uAddr) \
        TXN_PARAM_SET_PRIORITY(pTxn, uPriority); \
        TXN_PARAM_SET_FUNC_ID(pTxn, uId); \
        TXN_PARAM_SET_DIRECTION(pTxn, uDirection); \
        TXN_PARAM_SET_FIXED_ADDR(pTxn, uAddr);

#define BUILD_TTxnStruct(pTxn, uAddr, pBuf, uLen, fCB, hCB) \
    (pTxn)->aBuf[0] = (McpU8*)pBuf; \
    (pTxn)->aLen[0] = (McpU16)uLen; \
    (pTxn)->uHwAddr = uAddr; \
    (pTxn)->hCbHandle = (void*)hCB; \
    (pTxn)->fTxnDoneCb = (void*)fCB;


/* Interface Sleep Management opcodes */
#define TXN_PARAM_IFSLPMNG_OP_NORM		0
#define TXN_PARAM_IFSLPMNG_OP_SLEEP		1     
#define TXN_PARAM_IFSLPMNG_OP_SLEEP_ACK	2     
#define TXN_PARAM_IFSLPMNG_OP_AWAKE		3     
#define TXN_PARAM_IFSLPMNG_OP_AWAKE_ACK	4

/* For Ai2 Parsing	*/
#define AI2_PKT_SYNCH_BYTE   0x10
#define AI2_PKT_TERM_BYTE    0x03

#define BUSDRV_NAME_MAX_SIZE			10


/************************************************************************
 * Types
 ************************************************************************/
/* The TxnDone CB called by the bus driver upon Async Txn completion */
typedef void (*TBusDrvTxnDoneCb)(handle_t hCbHandle, void *pTxn);

/* The Rx Indicataion CB called by the bus driver upon packet receive completion */
typedef ETxnStatus (*TI_BusDvrRxIndCb)(handle_t hCbHandle, void *pTxn);

/* The TxnDone CB called by the TxnQueue upon Async Txn completion */
typedef void (*TTxnQueueDoneCb)(handle_t hCbHandle, void *pTxn, McpBool bInExternalContext);

/* The TxnDone CB of the specific Txn originator (Xfer layer) called upon Async Txn completion */
typedef void (*TTxnDoneCb)(handle_t hCbHandle, void *pTxn);

/* HCI header definition */
#define TXN_HCI_HEADER_MAX_LEN	4

typedef struct
{
	McpU8 uPktType;
	McpU8 aHeader[TXN_HCI_HEADER_MAX_LEN];
	McpU8 uLen;                             /* header length including packet type byte */

} THciHeader;

typedef union 
{
	THciHeader tHciHeader;

} THeader;

/* Protocol Identifier */
typedef enum
{
	PROTOCOL_ID_UNDEFINED,
	PROTOCOL_ID_HCI

} Eprotocol_id;


/* The transactions structure */
typedef struct
{
    TQueNodeHdr tTxnQNode;                /* Header for queueing */
    McpU32    	uTxnParams;               /* Txn attributes (bit fields) - see macros above */
    McpU32    	uHwAddr;                  /* Physical (32 bits) HW Address */
    TTxnDoneCb  fTxnDoneCb;               /* CB called by TwIf upon Async Txn completion (may be NULL) */
    handle_t    hCbHandle;                /* The handle to use when calling fTxnDoneCb */
    McpU16    	aLen[MAX_XFER_BUFS];      /* Lengths of the following aBuf data buffers respectively.
                                              Zero length marks last used buffer, or MAX_XFER_BUFS of all are used. */
    McpU8		*aBuf[MAX_XFER_BUFS];     /* Host data buffers to be written to or read from the device */
    McpU32    	aWspiPad[FIXED_BUSY_LEN]; /* Padding used by WSPI bus driver for its header or fixed-busy bytes */
	McpU8   	*pData;
	McpU8	 	protocol;
	THeader		tHeader;

} TTxnStruct; 

/* Parameters for all bus types configuration in ConnectBus process */

typedef struct
{
    McpU32    uBlkSizeShift;
} TSdioCfg; 

typedef struct
{
    McpU32    uDummy;
} TWspiCfg; 

typedef struct
{
    McpU32    uBaudRate;
    McpU32    uFlowCtrl;
	McpU16    XonLimit;
	McpU16    XoffLimit;
	McpU16    portNum;

} TUartCfg; 

typedef union
{
    TSdioCfg    tSdioCfg;       
    TWspiCfg    tWspiCfg;       
    TUartCfg    tUartCfg;       

} TBusDrvCfg;


/* UART receiver state */
#ifndef TI_STAND_ALONE_UART_BUSDRV
typedef enum
{
    BUSDRV_STATE_RX_PKT_TYPE = 1,
    BUSDRV_STATE_RX_HEADER,
	BUSDRV_STATE_RX_DATA,
	BUSDRV_STATE_RX_CONGESTION,
    BUSDRV_STATE_RX_MAX
} EBusDrvRxState;
#else
typedef enum
{
    BUSDRV_STATE_RX_GET_SYNC = 1,
	BUSDRV_STATE_RX_GET_DATA,
	BUSDRV_STATE_RX_GET_DATA_DLE,
	BUSDRV_STATE_RX_CONGESTION,
	BUSDRV_STATE_RX_MAX
} EBusDrvRxState;
#endif

/* Statistics counters */
typedef struct
{

	McpU32       Tx;          
	McpU32       TxCompl;         
	McpU32       RxInd;               
	McpU32       BufAvail;
	McpU32       TxErr;               
	McpU32       RxErr;               
	McpU32       RxIndErr;               
	McpU32       AllocErr;               

} TBusDrvStat;

/************************************************************************
 * Functions
 ************************************************************************/
handle_t   	busDrv_Create     (const handle_t hMcpf);
EMcpfRes 	busDrv_Destroy    (handle_t hBusDrv);
void       	busDrv_Init       (handle_t hBusDrv);

EMcpfRes 	busDrv_ConnectBus (handle_t        			hBusDrv, 
                               const TBusDrvCfg       	*pBusDrvCfg,
                               const TBusDrvTxnDoneCb 	fCbFunc,
                               const handle_t        	hCbArg,
                               const TBusDrvTxnDoneCb 	fConnectCbFunc,
							   const TI_BusDvrRxIndCb   fRxIndCb,
							   const handle_t 			hRxIndHandle, 
							   const TI_TxnEventHandlerCb fEventHandlerCb,
							   const handle_t 			hEventHandle,
							   const handle_t			hIfSlpMng);

EMcpfRes 	busDrv_DisconnectBus (handle_t hBusDrv);
ETxnStatus 	busDrv_Transact   (handle_t hBusDrv, TTxnStruct *pTxn);
void 	   	busDrv_BufAvailable (handle_t hBusDrv);
void	   	busDrv_GetStat (const handle_t hBusDrv, TBusDrvStat  * pStat, EBusDrvRxState * pState);
void		busDrv_SetSpeed (const handle_t hBusDrv, const McpU16 baudrate);
EMcpfRes  busDrv_RxReset (handle_t hBusDrv);
EMcpfRes busDrv_Reset (handle_t hBusDrv, const McpU16 baudrate);

#endif /*__BUS_DRV_API_H__*/

