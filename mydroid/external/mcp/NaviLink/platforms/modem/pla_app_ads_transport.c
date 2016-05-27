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

/** \file   pla_app_transport.c 
 *  \brief  Platform Application Layer ADS/ULTS transport implementation
 * 
 * 
 */

#include "mcpf_defs.h"
#include "mcpf_mem.h"
#include "mcpf_msg.h"
#include "mcpf_services.h"
#include "mcpf_time.h"
#include "mcpf_report.h"
#include "mcp_hal_st.h"
#include "navc_api.h"
#include "navc_rrlp.h"
#include "navc_rrc.h"
#include "pla_app_ads.h" 
#include "mcp_hal_os.h"
#include "mcp_hal_string.h"
#include "pla_app_ads_api.h" 


/************************************************************************
 * Defines
 ************************************************************************/


/************************************************************************
 * Types
 ************************************************************************/
/* Debug stuff */
#ifdef _DEBUG
#define dumpBuf(pTitle, pBuf,len) MCP_LOG_DUMPBUF(pTitle,pBuf,len)
#else
#define dumpBuf(pTitle, pBuf,len)
#endif

/************************************************************************
 * Internal functions prototypes
 ************************************************************************/

static McpBool AdsCheckData(AdsPort_t *hPort, McpU16 *len);
static void AdsPrepareData(AdsPort_t *hPort, AdsMsg_u *iTxAdsMsg, McpU16 *len);
static void AdsUartRxCb(handle_t handle);
static void AdsUartTxCompleteCb(handle_t handle);
static EMcpfRes AdsSendCB (handle_t handle, McpU32 DestTaskId, McpU8 uUnused, TmcpfMsg *tmpMsg);
static void AdsUartEventCb(handle_t handle, EHalStEvent eEvent);

/************************************************************************
 *
 *   Module functions implementation
 *
 ************************************************************************/

/** 
 * \fn     MCP_ADS_Create
 * \brief  Create ADS object
 * 
 *  Create ADS object, link it to MCPF and create ADS commands pool
 */
handle_t MCP_ADS_Create (handle_t hMcpf)
{
	Ads_t *hAds = (Ads_t *)mcpf_mem_alloc(hMcpf, sizeof(Ads_t));
	if(hAds)
	{
		hAds->hMcpf = hMcpf;
		hAds->hPort = NULL;
		hAds->hCmdPool = mcpf_memory_pool_create(hMcpf, sizeof(TNAVC_cmdParams), 16);
	}
	return (handle_t)hAds;
}

/** 
 * \fn     MCP_ADS_Destroy
 * \brief  Destroy ADS object
 * 
 *  Destroy ADS object and it's pool
 */
void MCP_ADS_Destroy (handle_t hAds)
{
	Ads_t 		*pAds = (Ads_t *) hAds;
	AdsPort_t  	*pPort= pAds->hPort;
	AdsPort_t  	*pNextPort;

	while (pPort)
	{
		mcpf_UnRegisterClientCb(pAds->hMcpf, pPort->TaskId);
		HAL_ST_Deinit (pPort->hUart);
		HAL_ST_Destroy (pPort->hUart);
		mcpf_memory_pool_destroy (pAds->hMcpf, pPort->hTxCmdPool);
		mcpf_critSec_DestroyObj (pAds->hMcpf, &pPort->hTxCritSec);
		mcpf_que_Destroy (pPort->hTxQueue);

		RRC_Destroy (pPort->hRrc);
		RRLP_Destroy (pPort->hRrlp);

		pNextPort = pPort->next;
		mcpf_mem_free (pAds->hMcpf, pPort);
		pPort = pNextPort;
	}
	mcpf_memory_pool_destroy (pAds->hMcpf, pAds->hCmdPool);
	mcpf_mem_free (pAds->hMcpf, pAds);
}


/** 
 * \fn     MCP_ADS_AddPort
 * \brief  Create ADS server on specified serial port
 * 
 * Initiate an ADS server on specified comm port, allocate MCPF' task id and 
 * initialize serial port to receive/send ULTS/ADS messages
 * 
 */
handle_t *MCP_ADS_AddPort (handle_t handle, McpU16 portNum, McpU32 baudrate)
{
	McpS8 comFileName[10];
	McpS8 moduleName[10]; /* Klocwork Changes : #define HALST_MODULE_NAME_MAX_SIZE 10*/
	Ads_t *hAds = (Ads_t *)handle;
	AdsPort_t *hPort = (AdsPort_t *)mcpf_mem_alloc(hAds->hMcpf, (McpU32)(sizeof(AdsPort_t)));
	
	if(hPort)
	{	
		// link to head of ports linked list
		hPort->next = hAds->hPort;
		hAds->hPort = hPort;
		
		// Add internal (MCPF) task index
		mcpf_RegisterClientCb(hAds->hMcpf, (McpU32*)&hPort->TaskId, AdsSendCB, hPort);
		
		// configure comm port params
		MCP_HAL_STRING_Sprintf(comFileName, "\\\\.\\COM%d", portNum);
		hPort->tPortCfg.portNum = portNum;
		hPort->tPortCfg.uFlowCtrl = MCP_TRUE;
		hPort->tPortCfg.uBaudRate = baudrate;
		hPort->tPortCfg.XoffLimit = sizeof(AdsMsg_u) - 3;
		hPort->tPortCfg.XonLimit  = sizeof(AdsMsg_u) - 3;
		mcpf_critSec_CreateObj(hAds->hMcpf, "ADS-CritSec", &hPort->hTxCritSec);
		hPort->hTxQueue = mcpf_que_Create(hAds->hMcpf, 0xffffffff, 0);
		MCP_HAL_STRING_StrCpy(moduleName, "ADS");
		hPort->hUart = HAL_ST_Create(hAds->hMcpf, moduleName);
                 //klock work error resolution
                 if (hPort->hUart == NULL)
                 {
                   MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG, ("MCP_ADS_AddPort: HAL_ST_Create returned NULL"));
                   //Assert(hPort->hUart !=NULL );
                   return NULL;
                  }
		hPort->bTxInProcess = MCP_FALSE;
		hPort->reqBytes = sizeof(AdsPrefix_t);
		hPort->readBytes = 0;
		hPort->eRxState = READ_ADS_HEADER;
		hPort->hAds =hAds;
		hPort->hTxCmdPool = mcpf_memory_pool_create(hAds->hMcpf, sizeof(TmcpfMsg), 10);
		HAL_ST_Init(hPort->hUart, AdsUartEventCb, hPort, &hPort->tPortCfg, 
					comFileName, hPort->RxAdsMsg.Buffer, hPort->reqBytes, MCP_FALSE);

		hPort->hRrlp = RRLP_Init (hAds->hMcpf, hAds->hCmdPool, hAds->hCmdPool, hPort->TaskId, 0);
		if (hPort->hRrlp == NULL)
		{
			MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG, ("MCP_ADS_Create: RRLP_Init failed"));
		}

		hPort->hRrc = RRC_Init (hAds->hMcpf, hAds->hCmdPool, hAds->hCmdPool, hPort->TaskId, 0);
		if (hPort->hRrc == NULL)
		{
			MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG, ("MCP_ADS_Create: RRC_Init failed"));
		}
	}
	return (handle_t)hPort;
} 


/************************************************************************
 * Module Private Functions
 ************************************************************************/

/** 
 * \fn     AdsCheckData 
 * \brief  ADS frame check sum
 * 
 * Calculate ADS frame check sum and compare it with check sum contained in the frame 
 * 
 * \note
 * \param	hPort  - handle to ADS port control structure
 * \param	len    - buffer size to check
 * \return 	Result of operation: TRUE - check sum is OK or FALSE - check sum is erroneous
 * \sa     	AdsPrepareData
 */
static McpBool AdsCheckData(AdsPort_t *hPort, McpU16 *len)
{
	McpU16 in=hPort->readBytes, out=hPort->readBytes, nRcvBytes = (McpU16)(hPort->readBytes + *len);
	McpU8 *pBuffer = hPort->RxAdsMsg.Buffer;
	static McpU32 csum = 0; 
	McpBool res = MCP_TRUE;

	while(out == 0 && in<nRcvBytes)
	{
		if(pBuffer[in++] == SYNC_WORD) // looking for preamble
		{
			csum = SYNC_WORD;
			out++;
		}
	}

	while(in<nRcvBytes)
	{
		if(pBuffer[in] == SYNC_WORD)
		{
			if(pBuffer[in+1] == SYNC_END_WORD) // suffix SYNC 
			{
				csum -= (pBuffer[in-1] + pBuffer[in-2]);
				if(((csum & 0xff) != pBuffer[in-1]) ||
					(((csum & 0xff00)>>8) != pBuffer[in-2]))
				res = MCP_FALSE;
			}
			else
				in++;
		}
		csum += (McpU32)pBuffer[in];
		pBuffer[out++] = pBuffer[in++];
	}
	*len = (McpU16)(out - hPort->readBytes);
	return res;
}

/** 
 * \fn     AdsPrepareData 
 * \brief  Prepare ADS frame
 * 
 * Build ADS frame by adding start byte, calculating and appending check sum, append end byte.
 * 
 * \note
 * \param	hPort     - handle to ADS port control structure
 * \param	iTxAdsMsg - pointer to input buffer containing ADS frame to prepare
 * \param	len       - the size of input ADS buffer to prepare
 * \return 	void
 * \sa     	AdsCheckData
 */
static void AdsPrepareData(AdsPort_t *hPort, AdsMsg_u *iTxAdsMsg, McpU16 *len)
{
	McpU32 csum = SYNC_WORD;
	McpU16 iLen = *len, i, o;

	hPort->TxAdsMsg.s.Header.Sync = SYNC_WORD;
	for (i=1,o=1; i<iLen; o++, i++) // while(i<iLen)
	{
		/*KlocWork Critical Issue:110 Resolved by adding boundary check*/
       if((i < (MAX_ADS_PAYLOAD+sizeof(AdsPrefix_t))) && (o < (MAX_ADS_PAYLOAD+sizeof(AdsPrefix_t))))
	   {
		if(iTxAdsMsg->Buffer[i] == SYNC_WORD)
		{
			hPort->TxAdsMsg.Buffer[o++] = SYNC_WORD;

			/* Need to check if needed for Host Toolkit - If so --> fix host toolkit */
			/*csum += SYNC_WORD;*/
		}
		/*KlocWork Critical Issue:110 Resolved by adding boundary check*/
		if(o < (MAX_ADS_PAYLOAD+sizeof(AdsPrefix_t)))
		{
		hPort->TxAdsMsg.Buffer[o] = iTxAdsMsg->Buffer[i];
		csum += hPort->TxAdsMsg.Buffer[o];		
		}
       }
	}
	
	

	// prepare ADS Suffix
	/*KlocWork Critical Issue:138 Resolved by adding boundary check*/
	if(o < (MAX_ADS_PAYLOAD+sizeof(AdsPrefix_t)))
	hPort->TxAdsMsg.Buffer[o++] = (McpU8)((csum& 0xff00)>>8);
	/*KlocWork Critical Issue:138 Resolved by adding boundary check*/
	if(o < (MAX_ADS_PAYLOAD+sizeof(AdsPrefix_t)))
	hPort->TxAdsMsg.Buffer[o++] = (McpU8)(csum & 0x00ff);
	/*KlocWork Critical Issue:138 Resolved by adding boundary check*/
	if(o < (MAX_ADS_PAYLOAD+sizeof(AdsPrefix_t)))
	hPort->TxAdsMsg.Buffer[o++] = SYNC_WORD;
	/*KlocWork Critical Issue:138 Resolved by adding boundary check*/
	if(o < (MAX_ADS_PAYLOAD+sizeof(AdsPrefix_t)))
	hPort->TxAdsMsg.Buffer[o++] = SYNC_END_WORD;

	/*KlocWork Critical Issue:138 Resolved by adding boundary check*/
	if(o < (MAX_ADS_PAYLOAD+sizeof(AdsPrefix_t)))
	*len = (McpU16)(*len + (o-i));
}



/** 
 * \fn     AdsUartRxCb 
 * \brief  Port Rx event processing
 * 
 * Process Rx event from ADS port, read available data from the port and
 * invoke ADS protocol message processing function if whole ADS message is received
 * 
 * \note
 * \param	handle    - handle to ADS port control structure
 * \return 	void
 * \sa     	AdsCheckData, ads_process_msg
 */
static void AdsUartRxCb(handle_t handle)
{
	AdsPort_t *hPort = (AdsPort_t*)handle;
	Ads_t *hAds = (Ads_t*)hPort->hAds;
	McpU16 uRxLen;

	HAL_ST_ReadResult (hPort->hUart, &uRxLen);
	MCPF_REPORT_INFORMATION(hAds->hMcpf, ADS_MODULE_LOG, ("AdsUartRxCb: reqBytes = %d, uRxLen = %d", hPort->reqBytes, uRxLen));
	while(uRxLen)
	{
		if(AdsCheckData(hPort, &uRxLen) == MCP_FALSE)
		{
			// TODO - chcksum error handling
			MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG, ("checksum error on port %d\n", hPort->tPortCfg.portNum));
		}
		dumpBuf ("RxAds", hPort->RxAdsMsg.Buffer, uRxLen);
		hPort->readBytes = (McpU16)(hPort->readBytes + uRxLen);
		if(uRxLen < hPort->reqBytes)
		{
			hPort->reqBytes = (McpU16)(hPort->reqBytes - uRxLen);
		}
		else
		{
			switch (hPort->eRxState)
			{
				case READ_ADS_HEADER:
				{
					//Assert (hAdsDrv->readBytes == sizeof(AdsPrefix));
					hPort->RxAdsMsg.s.PayloadLength = mcpf_endian_BEtoHost16(hPort->RxAdsMsg.s.Header.MsgLength);
					hPort->reqBytes = (McpU16)(hPort->RxAdsMsg.s.PayloadLength + (McpU16)sizeof(AdsSuffix_t));
					hPort->eRxState = READ_ADS_BUFFER;
					MCPF_REPORT_INFORMATION(hAds->hMcpf, ADS_MODULE_LOG, ("AdsUartRxCb: In READ_ADS_HEADER, reqBytes = %d", hPort->reqBytes));
				}
				break;
				case READ_ADS_BUFFER:
				{
					MCPF_REPORT_INFORMATION(hAds->hMcpf, ADS_MODULE_LOG, ("AdsUartRxCb: In READ_ADS_BUFFER"));
					ads_process_msg(hPort, hPort->TaskId, &hPort->RxAdsMsg);
					hPort->reqBytes = sizeof(AdsPrefix_t);
					hPort->readBytes = 0;
					hPort->eRxState = READ_ADS_HEADER;
				}
				break;
			}
		}
		HAL_ST_Read(hPort->hUart, 
			&hPort->RxAdsMsg.Buffer[hPort->readBytes],
			hPort->reqBytes, 
			&uRxLen);
	} 
}


/** 
 * \fn     AdsUartTxCompleteCb 
 * \brief  Port Tx complete event processing
 * 
 * Process Tx complete event from ADS port, deque message from ADS port queue, builds ADS frame
 * and send ADS frame to ADS port
 * 
 * \note
 * \param	handle    - handle to ADS port control structure
 * \return 	void
 * \sa     	ads_generate_msg
 */
static void AdsUartTxCompleteCb(handle_t handle)
{
	AdsPort_t *hPort = (AdsPort_t*)handle;
	Ads_t *hAds = (Ads_t*)hPort->hAds;
	TmcpfMsg *Msg;
	McpBool bAdsMsg; // intrenal message indication
	McpU16 sentLen, reqLen;
	AdsMsg_u TmpMsg;
	EMcpfRes  res;

	mcpf_critSec_Enter(hAds->hMcpf, hPort->hTxCritSec, MCP_HAL_OS_TIME_INFINITE);
	hPort->bTxInProcess = MCP_FALSE;
	Msg = (TmcpfMsg *)mcpf_que_Dequeue(hPort->hTxQueue);
	MCPF_REPORT_INFORMATION(hAds->hMcpf, HAL_ST_MODULE_LOG, ("AdsUartTxCompleteCb: mcpf_que_Dequeue (outside while) size of queue:%ld", mcpf_que_Size(hPort->hTxQueue)));
	while(Msg)
	{
		bAdsMsg = (hPort->TaskId == Msg->eSrcTaskId)?MCP_TRUE:MCP_FALSE;
		hPort->bTxInProcess = MCP_TRUE;

		reqLen = ads_generate_msg(hPort, bAdsMsg, Msg, &TmpMsg);

		if(reqLen != 0)
			AdsPrepareData(hPort, &TmpMsg, &reqLen);
		
		if(Msg->pData)
			mcpf_mem_free_from_pool(hAds->hMcpf, Msg->pData);

		mcpf_mem_free_from_pool(hAds->hMcpf, Msg);
		Msg = 0;

		if(reqLen != 0)
		{
			dumpBuf ("TxAds", hPort->TxAdsMsg.Buffer, reqLen);
			
			res = HAL_ST_Write(hPort->hUart, hPort->TxAdsMsg.Buffer, reqLen, &sentLen);
			if(res == RES_PENDING)
			{
				MCPF_REPORT_INFORMATION(hAds->hMcpf, HAL_ST_MODULE_LOG, ("AdsUartTxCompleteCb: RES_PENDING"));
			}
			else
			{
			if(sentLen == reqLen)
			{
				hPort->bTxInProcess = MCP_FALSE;
				Msg = (TmcpfMsg *)mcpf_que_Dequeue(hPort->hTxQueue);
				MCPF_REPORT_INFORMATION(hAds->hMcpf, HAL_ST_MODULE_LOG, ("AdsUartTxCompleteCb: mcpf_que_Dequeue (inside while) size of queue:%ld", mcpf_que_Size(hPort->hTxQueue)));
				}
			}
		}
		else
		{
			MCPF_REPORT_INFORMATION(hAds->hMcpf, HAL_ST_MODULE_LOG, ("AdsUartTxCompleteCb: No need to send anything..."));
			res = RES_COMPLETE;
			hPort->bTxInProcess = MCP_FALSE;
			Msg = (TmcpfMsg *)mcpf_que_Dequeue(hPort->hTxQueue);
			MCPF_REPORT_INFORMATION(hAds->hMcpf, HAL_ST_MODULE_LOG, ("AdsUartTxCompleteCb: mcpf_que_Dequeue (inside while) size of queue:%ld", mcpf_que_Size(hPort->hTxQueue)));
		}
	}
	mcpf_critSec_Exit(hAds->hMcpf, hPort->hTxCritSec);
}

/** 
 * \fn     AdsSendCB 
 * \brief  Send message to ADS port
 * 
 * Initiate sending of the message to ADS port, if port TX is idle or add the message to the port TX queue,
 * if port TX is in progress.
 * 
 * \note
 * \param	handle     - handle to ADS port control structure
 * \param	DestTaskId - destination task ID
 * \param	uUnused - Added to fit Cb prototype
 * \param	tmpMsg     - message to send
 * \return 	void
 * \sa     	ads_generate_msg, AdsPrepareData
 */
static EMcpfRes AdsSendCB (handle_t handle, McpU32 DestTaskId, McpU8 uUnused, TmcpfMsg *tmpMsg)
{
	AdsPort_t *hPort = (AdsPort_t *)handle;
	Ads_t *hAds = (Ads_t*)hPort->hAds;
	McpU16 sentLen, reqLen;
	AdsMsg_u TmpMsg;
	EMcpfRes res = RES_PENDING;
	TmcpfMsg *Msg;
	EMcpfRes status;

	MCPF_UNUSED_PARAMETER(uUnused);

	Msg = (TmcpfMsg*)mcpf_mem_alloc_from_pool(hAds->hMcpf, hPort->hTxCmdPool); 
        if (Msg==NULL)
        {
              MCPF_REPORT_ERROR( hAds->hMcpf, HAL_ST_MODULE_LOG, ("mem alloc failed, returning "));
              return RES_MEM_ERROR;
        }
	*Msg = *tmpMsg; // copy mcpf message descriptor to internal buffer

	mcpf_critSec_Enter(hAds->hMcpf, hPort->hTxCritSec, MCP_HAL_OS_TIME_INFINITE);
	/* If port is busy enqueue message till Tx-Complete else write the message immediately */
	if(hPort->bTxInProcess) 
	{  
		mcpf_que_Enqueue(hPort->hTxQueue, Msg);
		MCPF_REPORT_INFORMATION(hAds->hMcpf, HAL_ST_MODULE_LOG, ("AdsSendCB: mcpf_que_Enqueue size of queue:%ld", mcpf_que_Size(hPort->hTxQueue)));
	
	}
	else
	{
		McpBool bAdsMsg; // intrenal message indication
		
		bAdsMsg = ((McpU16)DestTaskId == Msg->eSrcTaskId)?MCP_TRUE:MCP_FALSE;
		hPort->bTxInProcess = MCP_TRUE;
		mcpf_mem_zero(hAds->hMcpf, (void *)&TmpMsg, sizeof(AdsMsg_u));

		reqLen = ads_generate_msg(hPort, bAdsMsg, Msg, &TmpMsg);

		if(reqLen != 0)
			AdsPrepareData(hPort, &TmpMsg, &reqLen);

		if(Msg->pData)
			mcpf_mem_free_from_pool(hAds->hMcpf, Msg->pData);

		mcpf_mem_free_from_pool(hAds->hMcpf, Msg);

		if(reqLen != 0)
		{
			dumpBuf ("TxAds", hPort->TxAdsMsg.Buffer, reqLen);

			status = HAL_ST_Write(hPort->hUart, hPort->TxAdsMsg.Buffer, reqLen, &sentLen);
			if(status == RES_PENDING)
			{
				MCPF_REPORT_INFORMATION(hAds->hMcpf, HAL_ST_MODULE_LOG, ("AdsSendCb: RES_PENDING"));
			}
			else
			{
			if(sentLen == reqLen)
			{
				// sync tx
				res = RES_COMPLETE;
				hPort->bTxInProcess = MCP_FALSE;
				}
			}
		}
		else
		{
			MCPF_REPORT_INFORMATION(hAds->hMcpf, HAL_ST_MODULE_LOG, ("AdsSendCb: No need to send anything..."));
			res = RES_COMPLETE;
			hPort->bTxInProcess = MCP_FALSE;
		}
	}
	mcpf_critSec_Exit(hAds->hMcpf, hPort->hTxCritSec);

	mcpf_mem_free_from_pool(hAds->hMcpf, tmpMsg);   /* free MCPF input message */

	return res;
}


/** 
 * \fn     AdsUartEventCb 
 * \brief  Process TX and RX ADS port events
 * 
 * Analyze event type received from ADS port and invoke Rx and Tx processing functions
 * accordingly
 * 
 * \note
 * \param	handle - handle to ADS port control structure
 * \param	pEvent - port event
 * \return 	void
 * \sa     	ads_generate_msg, AdsPrepareData
 */
static void AdsUartEventCb(handle_t handle, EHalStEvent eEvent)
{
    AdsPort_t *pPort = (AdsPort_t *) handle;
    Ads_t       *pAds = (Ads_t *)  pPort->hAds;

	switch(eEvent)
	{
    case HalStEvent_ReadReadylInd:
        AdsUartRxCb(handle);
        break;
    case HalStEvent_WriteComplInd:
        AdsUartTxCompleteCb(handle);
        break;
    default:
        MCPF_REPORT_ERROR( pAds->hMcpf, HAL_ST_MODULE_LOG, ("AdsUartEventCb: unexpected event"));
        break;
	}
}

