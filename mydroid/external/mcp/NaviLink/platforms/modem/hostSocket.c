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


/** \file   hostsocket.c 
 *  \brief  Host socket Thread implementation                               
 * 
  */

/** Include Files **/
#include "mcpf_services.h"
#include "mcp_hal_os.h"
#include "mcp_hal_fs.h"
#include "pla_hw.h"
#include "mcp_hal_socket.h"
#include "navc_api.h"
#include "mcpf_msg.h"
#include "mcpf_mem.h"
#include "mcpf_report.h"
#include "pla_os.h"
#include "navl_api.h"
#include "hostSocket.h"

#include <utils/Log.h>
#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG "NAVD.HOST_SOCKET"    // our identification for logcat (adb logcat NAVD.HOST_SOCKET:V *:S)

/************************************************************************/
/* Internal Functions Definitions                                       */
/************************************************************************/
handle_t	OnAcceptCb(handle_t hCaller, McpU32 uSockId, McpU8 *numOfBytesToRead);
McpU16		OnRecvCb(handle_t hCaller, McpS8 *buf, McpS16 bufLen);
void		OnCloseCb(handle_t hCaller);
EMcpfRes	ClientSendCb(handle_t hCaller, McpU8 msgClass, McpU8 opcode, McpU16 length, McpU8 *msg);


/************************************************************************/
/* APIs Functions Implementation                                        */
/************************************************************************/
/** 
 * \fn     hostSocket_Create
 * \brief  Create the Host Socket
 *  
 *  Create the Socket and Listen on the Thread
 * 
 * \note
 * \param	hMcpf	- handle to MCPF
 * \param 	hCaller - handle to NAVL
 * \return	handle to Host Socket
 * \sa     	hostSocket_Create
**/ 
handle_t hostSocket_Create(handle_t hMcpf, handle_t hNavl)
{
	hostSocket_t	*pHostSocket;

	pHostSocket = (hostSocket_t *)mcpf_mem_alloc(hMcpf, sizeof(hostSocket_t));
	if(pHostSocket == NULL)
	{
		MCPF_REPORT_ERROR(hMcpf, HOSTSOCKET_MODULE_LOG, ("hostSocket_Create: failed to allocate memeory"));
		return NULL;
	}

	pHostSocket->hClientsPool = mcpf_memory_pool_create(hMcpf, sizeof(hostSocket_client_t), SOCKET_CLIENT_NUM);
	if(pHostSocket->hClientsPool == NULL)
	{
		MCPF_REPORT_ERROR(hMcpf, HOSTSOCKET_MODULE_LOG, ("hostSocket_Create: failed to allocate client pool"));
		mcpf_mem_free(hMcpf, pHostSocket);
		return NULL;
	}

	pHostSocket->hNavlMsgPool = mcpf_memory_pool_create(hMcpf, sizeof(NavlMsg_t), 50);
	if(pHostSocket->hNavlMsgPool == NULL)
	{
		MCPF_REPORT_ERROR(hMcpf, HOSTSOCKET_MODULE_LOG, ("hostSocket_Create: failed to allocate msg pool"));
		mcpf_memory_pool_destroy(hMcpf, pHostSocket->hClientsPool);
		mcpf_mem_free(hMcpf, pHostSocket);
		return NULL;
	}

	pHostSocket->hMcpf = hMcpf;
	pHostSocket->hNavl = hNavl;

	pHostSocket->hHalSock = mcp_hal_socket_CreateServerSocket(hMcpf, (handle_t)pHostSocket, 4121, 
																OnAcceptCb, OnRecvCb, OnCloseCb);
	if(pHostSocket->hHalSock == NULL)
	{
		MCPF_REPORT_ERROR(hMcpf, HOSTSOCKET_MODULE_LOG, 
			("hostSocket_Create: FAILURE to create Server Socket\n"));
		mcpf_memory_pool_destroy(hMcpf, pHostSocket->hNavlMsgPool);
		mcpf_memory_pool_destroy(hMcpf, pHostSocket->hClientsPool);
		mcpf_mem_free(hMcpf, pHostSocket);
		return NULL;
	}
       
    return (handle_t)pHostSocket;
}

/** 
 * \fn     hostSocket_Destroy
 * \brief  Destroy the Host Socket Module
 *  
 *  Free memory & structures.
 * 
 * \note
 * \param	hHostSocket	- handle to Host Socket
 * \return	SUCESS or FAIL
 * \sa     	hostSocket_Destroy
**/ 
EMcpfRes hostSocket_Destroy(handle_t hHostSocket)
{
	hostSocket_t	*pHostSocket = (hostSocket_t *)hHostSocket;
	
	mcp_hal_socket_DestroyServerSocket(pHostSocket->hHalSock);
	mcpf_memory_pool_destroy(pHostSocket->hMcpf, pHostSocket->hNavlMsgPool);
	mcpf_memory_pool_destroy(pHostSocket->hMcpf, pHostSocket->hClientsPool);
	mcpf_mem_free(pHostSocket->hMcpf, hHostSocket);

	return RES_OK;
}


/************************************************************************/
/* Internal Functions Implementation                                    */
/************************************************************************/
/** 
 * \fn     OnAcceptCb 
 * \brief  This Callback will be called when a connection request  
 *		   has been received on the listening socket
 * 
 **/
handle_t OnAcceptCb(handle_t hCaller, McpU32 uSockId, McpU8 *numOfBytesToRead)
{
	hostSocket_t	*pHostSocket = (hostSocket_t *)hCaller;
	hostSocket_client_t	*pHostSockClient;
	
	pHostSockClient = (hostSocket_client_t	*)mcpf_mem_alloc_from_pool(pHostSocket->hMcpf, 
																		pHostSocket->hClientsPool);
	mcpf_critSec_CreateObj(pHostSocket->hMcpf, "Socket", &pHostSockClient->hCritSec);
	if(pHostSockClient == NULL)
	{
		MCPF_REPORT_ERROR(pHostSocket->hMcpf, HOSTSOCKET_MODULE_LOG, 
			("OnAcceptCb: Failed to allocate client from pool"));
		return NULL;
	}
	mcpf_critSec_Enter (pHostSocket->hMcpf, pHostSockClient->hCritSec , MCPF_INFINIT);

	pHostSockClient->hHostSock = hCaller;
	pHostSockClient->uClientSockId = uSockId;
	pHostSockClient->clientState = HOSTSOCK_WAIT_HEADER;

	mcpf_critSec_Exit (pHostSocket->hMcpf, pHostSockClient->hCritSec);

	pHostSockClient->hNavlClient = NAVL_Open(pHostSocket->hNavl, (handle_t)pHostSockClient, ClientSendCb);
	if(pHostSockClient->hNavlClient == NULL)
	{
		MCPF_REPORT_ERROR(pHostSocket->hMcpf, HOSTSOCKET_MODULE_LOG, 
			("OnAcceptCb: NAVL_Open() failed!"));
		mcpf_mem_free_from_pool(pHostSocket->hMcpf, pHostSockClient);
		return NULL;
	}
	
	*numOfBytesToRead = HOSTSOCKET_MESSAGE_HEADER_SIZE;

	return (handle_t)pHostSockClient;
}

/** 
 * \fn     OnRecvCb 
 * \brief  This Callback will be called when data  
 *		   has been received on the client socket
 * 
 **/
McpU16 OnRecvCb(handle_t hCaller, McpS8 *buf, McpS16 bufLen)
{
	hostSocket_client_t	*pHostSockClient = (hostSocket_client_t	*)hCaller;
	hostSocket_t *pHostSock = (hostSocket_t *)pHostSockClient->hHostSock;
	NavlMsg_t *pNavlMsg;

	MCPF_UNUSED_PARAMETER(bufLen);

	switch(pHostSockClient->clientState)
	{
		case HOSTSOCK_WAIT_HEADER:
		{
			if (*(McpU32*)buf == HOSTSOCKET_MESSAGE_SYNC_START)
			{
				MCPF_REPORT_INFORMATION(pHostSock->hMcpf, HOSTSOCKET_MODULE_LOG, 
					("OnRecvCb: Recevied correct SYNC START, processing packet..."));
				
				pHostSockClient->currentMsgHdr.msgClass = buf[4];
				pHostSockClient->currentMsgHdr.opCode = buf[5];
				//pHostSockClient->currentMsgHdr.payloadLen = mcpf_endian_BEtoHost16((McpU8 *)&buf[6]);
				pHostSockClient->currentMsgHdr.payloadLen = *((McpU16 *)&buf[6]);


				if(pHostSockClient->currentMsgHdr.payloadLen == 0)
				{
					MCPF_REPORT_INFORMATION(pHostSock->hMcpf, HOSTSOCKET_MODULE_LOG, 
					("OnRecvCb: payloadLen == 0, Processing Command from WAIT_HEADER..."));
					
					NAVL_Command(pHostSockClient->hNavlClient, pHostSockClient->currentMsgHdr.msgClass, 
						pHostSockClient->currentMsgHdr.opCode, pHostSockClient->currentMsgHdr.payloadLen,NULL); /* Klocwork Changes */
					pHostSockClient->clientState = HOSTSOCK_WAIT_HEADER;
				}
				else
				{
					MCPF_REPORT_INFORMATION(pHostSock->hMcpf, HOSTSOCKET_MODULE_LOG, 
					("OnRecvCb: Changed state to WAIT_PAYLOAD"));
					pHostSockClient->clientState = HOSTSOCK_WAIT_PAYLOAD;
				}

				return pHostSockClient->currentMsgHdr.payloadLen;
			}
			else
			{
				MCPF_REPORT_ERROR(pHostSock->hMcpf, HOSTSOCKET_MODULE_LOG, 
					("OnRecvCb: Recevied wrong preamble, ignoring packet... buf = %x%x%x%x\n", buf[0], buf[1], buf[2], buf[3]));
			}
		}
		break;	

		case HOSTSOCK_WAIT_PAYLOAD:
		{
			MCPF_REPORT_INFORMATION(pHostSock->hMcpf, HOSTSOCKET_MODULE_LOG, 
					("OnRecvCb: Received Payload!"));
			pNavlMsg = (NavlMsg_t *)mcpf_mem_alloc_from_pool(pHostSock->hMcpf, pHostSock->hNavlMsgPool);
			if(pNavlMsg == NULL)
			{
				MCPF_REPORT_ERROR(pHostSock->hMcpf, HOSTSOCKET_MODULE_LOG, 
									("OnRecvCb: fialed to allocate msg"));
                        //klocwork
                        MCPF_Assert(pNavlMsg);
			}
			
			mcpf_mem_copy(pHostSock->hMcpf, pNavlMsg->Payload, buf, pHostSockClient->currentMsgHdr.payloadLen);

			MCPF_REPORT_INFORMATION(pHostSock->hMcpf, HOSTSOCKET_MODULE_LOG, 
					("OnRecvCb: Processing Command from WAIT_PAYLOAD, payload length is %d", pHostSockClient->currentMsgHdr.payloadLen));
			
			NAVL_Command(pHostSockClient->hNavlClient, pHostSockClient->currentMsgHdr.msgClass, 
					pHostSockClient->currentMsgHdr.opCode, pHostSockClient->currentMsgHdr.payloadLen, pNavlMsg);

			pHostSockClient->clientState = HOSTSOCK_WAIT_HEADER;
		}
		break;

		default:
		{
			MCPF_REPORT_ERROR(pHostSock->hMcpf, HOSTSOCKET_MODULE_LOG, 
			("OnRecvCb: Client in wrong state\n"));
		}
		break;
	}
	return 0;
}

/** 
 * \fn     OnCloseCb 
 * \brief  This Callback will be called when the  
 *		   client socket has been closed
 * 
 **/
void OnCloseCb(handle_t hCaller)
{
	hostSocket_client_t	*pHostSockClient = (hostSocket_client_t	*)hCaller;
	hostSocket_t *pHostSock = (hostSocket_t *)pHostSockClient->hHostSock;

	NAVL_Close(pHostSockClient->hNavlClient);
	mcpf_critSec_DestroyObj(pHostSock->hMcpf, &pHostSockClient->hCritSec);
	mcpf_mem_free_from_pool(pHostSock->hMcpf, hCaller);
}

/** 
 * \fn     OnCloseCb 
 * \brief  This Callback will be called to   
 *		   send data over the client socket
 * 
 **/
EMcpfRes ClientSendCb(handle_t hCaller, McpU8 msgClass, McpU8 opcode, McpU16 length, McpU8* msg)
{
	hostSocket_client_t	*pHostSockClient = (hostSocket_client_t*)hCaller;
	hostSocket_t *pHostSocket = (hostSocket_t *)pHostSockClient->hHostSock;
	hostSocket_header_t msgHdr;
	EMcpfRes retVal = RES_OK;

	msgHdr.syncSrart = HOSTSOCKET_MESSAGE_SYNC_START;
	msgHdr.msgClass = msgClass;
	msgHdr.opCode = opcode;
	msgHdr.payloadLen = length;
	
	mcpf_critSec_Enter (pHostSocket->hMcpf, pHostSockClient->hCritSec , MCPF_INFINIT);

	retVal = mcp_hal_socket_Send(pHostSocket->hHalSock, pHostSockClient->uClientSockId, 
									(McpS8 *)&msgHdr, HOSTSOCKET_MESSAGE_HEADER_SIZE);
	
	if (retVal == RES_ERROR)
	{
		mcpf_mem_free_from_pool(pHostSocket->hMcpf, msg);
		mcpf_critSec_Exit (pHostSocket->hMcpf, pHostSockClient->hCritSec);

		MCPF_REPORT_ERROR(pHostSocket->hMcpf, HOSTSOCKET_MODULE_LOG, 
			("ClientSendCb: Sending header failed!\n"));

		return retVal;
	}

	if (length != 0)
	{
		retVal = mcp_hal_socket_Send(pHostSocket->hHalSock, pHostSockClient->uClientSockId, 
									(McpS8 *)msg, length);
	}

	if (msg != NULL)
	{
		mcpf_mem_free_from_pool(pHostSocket->hMcpf, msg);
	}

	mcpf_critSec_Exit (pHostSocket->hMcpf, pHostSockClient->hCritSec);
	return retVal;
	
}
