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

/** \file   mcp_hal_socket.c 
 *  \brief  Win OS Adaptation Layer for Socket Interface implementation
 * 
 *  \see    mcp_hal_socket.h
 */

#include <stdio.h>
#include <winsock2.h>

#include "mcpf_services.h"
#include "mcp_hal_os.h"
#include "mcp_hal_fs.h"
#include "mcp_hal_socket.h"
#include "mcp_hal_types.h"
#include "mcpf_mem.h"
#include "mcpf_report.h"

/************************************************************************/
/* Internal Structures						    		                                          */
/************************************************************************/
typedef struct {
	handle_t				hMcpf;
	handle_t				hHostSocket;
	handle_t				hClientsPool;
	tHalSockOnAcceptCb	onAcceptCb;
	tHalSockOnRecvCb		onRecvCb;
	tHalSockOnCloseCb	onCloseCb;
	SOCKET				sockId;
} hal_socket_t;

typedef struct  
{
	SOCKET		clientSockId;
	handle_t	hHalSock;
	handle_t	hCaller;
	McpU8		uNumOfBytesToRead;
} client_handle_t;


/************************************************************************/
/* Internal Functions Definitions                                       */
/************************************************************************/
DWORD WINAPI serverSocket_ThreadFunc(LPVOID hSocket);
DWORD WINAPI clientSocket_ThreadFunc(LPVOID hSocket);


/************************************************************************/
/* APIs Functions Implementation                                        */
/************************************************************************/
/** 
 * \fn     mcp_hal_socket_CreateServerSocket
 * \brief  Socket Initializing and Binding at the Server side
 * 
 */
handle_t mcp_hal_socket_CreateServerSocket(handle_t hMcpf, handle_t hHostSocket, 
										   McpS16 port, tHalSockOnAcceptCb onAcceptCb, 
										   tHalSockOnRecvCb onRecvCb, tHalSockOnCloseCb onCloseCb) 
{
	SOCKADDR_IN lSockAddr;
	WSADATA wsaData;
	hal_socket_t *pHalSocket;

	pHalSocket = mcpf_mem_alloc(hMcpf, sizeof(hal_socket_t));
	if(pHalSocket == NULL)
	{
		MCPF_REPORT_ERROR(hMcpf, HAL_SOCKET_MODULE_LOG, 
			("mcp_hal_socket_CreateServerSocket: Create Hal Socket Failed!"));
		return NULL;
	}

	pHalSocket->hClientsPool = mcpf_memory_pool_create(hMcpf, sizeof(client_handle_t), SOCKET_CLIENT_NUM);
	if(pHalSocket->hClientsPool == NULL)
	{
		MCPF_REPORT_ERROR(hMcpf, HAL_SOCKET_MODULE_LOG, 
			("mcp_hal_socket_CreateServerSocket: Create Pool Failed!"));
		mcpf_mem_free(hMcpf, pHalSocket);
		return NULL;
	}

	pHalSocket->hMcpf = hMcpf;
	pHalSocket->hHostSocket = hHostSocket;
	pHalSocket->onAcceptCb = onAcceptCb;
	pHalSocket->onRecvCb = onRecvCb;
	pHalSocket->onCloseCb = onCloseCb;

	if(WSAStartup(MAKEWORD(2,0),&wsaData) != 0)
	{
		MCPF_REPORT_ERROR(hMcpf, HAL_SOCKET_MODULE_LOG, 
			("mcp_hal_socket_CreateServerSocket: WSAStartup() Failed!"));
		mcpf_memory_pool_destroy(hMcpf, pHalSocket->hClientsPool);
		mcpf_mem_free(hMcpf, pHalSocket);
		return NULL;
	}

	pHalSocket->sockId = socket(AF_INET,SOCK_STREAM,IPPROTO_TCP);
	if(pHalSocket->sockId == INVALID_SOCKET)
	{
		MCPF_REPORT_ERROR(hMcpf, HAL_SOCKET_MODULE_LOG, 
			("mcp_hal_socket_CreateServerSocket: socket() Failed!"));
		WSACleanup();
		mcpf_memory_pool_destroy(hMcpf, pHalSocket->hClientsPool);
		mcpf_mem_free(hMcpf, pHalSocket);
		return NULL;	
	}

	memset(&lSockAddr,0, sizeof(lSockAddr));
	lSockAddr.sin_family = AF_INET;
	lSockAddr.sin_port = htons(port);
	lSockAddr.sin_addr.s_addr = INADDR_ANY;

    /* Bind - links the socket we just created with the sockaddr_in 
       structure. Basically it connects the socket with 
       the local address and a specified port.
       If it returns non-zero - quit, as this indicates error. */
    if(bind(pHalSocket->sockId,(SOCKADDR*)&lSockAddr,sizeof(lSockAddr)) != 0)
    {
		MCPF_REPORT_ERROR(hMcpf, HAL_SOCKET_MODULE_LOG, 
			("mcp_hal_socket_CreateServerSocket: bind() Failed!"));
		closesocket(pHalSocket->sockId);
		WSACleanup();
		mcpf_memory_pool_destroy(hMcpf, pHalSocket->hClientsPool);
		mcpf_mem_free(hMcpf, pHalSocket);
        return NULL;
    }

    /* Listen - instructs the socket to listen for incoming 
       connections from clients. The second arg is the backlog. */
    if(listen(pHalSocket->sockId,10) != 0)
    {
		MCPF_REPORT_ERROR(hMcpf, HAL_SOCKET_MODULE_LOG, 
			("mcp_hal_socket_CreateServerSocket: listen() Failed!"));
		closesocket(pHalSocket->sockId);
		WSACleanup();
		mcpf_memory_pool_destroy(hMcpf, pHalSocket->hClientsPool);
		mcpf_mem_free(hMcpf, pHalSocket);
        return NULL;
    }
	
	return (handle_t)pHalSocket;
}

/** 
 * \fn     mcp_hal_socket_Accept
 * \brief  Socket Accept at the Server side
 * 
 */
void mcp_hal_socket_Accept(handle_t hHalSocket) 
{
	hal_socket_t	*pHalSock = (hal_socket_t *)hHalSocket;
	unsigned long q_Handle;
	HANDLE hThread;
	
	hThread = CreateThread(NULL,0,serverSocket_ThreadFunc,pHalSock,0,(LPDWORD)&q_Handle);
}

/** 
 * \fn     mcp_hal_socket_DestroyServerSocket
 * \brief  Socket Initializing and Binding at the Server side
 * 
 */
void mcp_hal_socket_DestroyServerSocket(handle_t hHalSocket) 
{
	hal_socket_t	*pHalSock = (hal_socket_t *)hHalSocket;
	
	/* TODO: Need to destroy Thread */
	
	closesocket(pHalSock->sockId);
	WSACleanup();
}

/** 
 * \fn     mcp_hal_socket_Send 
 * \brief  Send data back to the client
 * 
 */
EMcpfRes mcp_hal_socket_Send(handle_t hHalSocket, McpU32 clientSockId, McpS8 *buf, McpU16 bufLen)
{
	hal_socket_t *pHalSocket = (hal_socket_t *)hHalSocket;
	SOCKET	clientSock = (SOCKET)clientSockId;
	McpU32	bytesSent = 0;
	
	do
	{
		bytesSent += send(clientSock, &buf[bytesSent], bufLen-bytesSent, 0);
	} while ( (bytesSent > 0) && (bytesSent < bufLen) );

	if(bytesSent == 0)
	{
		MCPF_REPORT_ERROR(pHalSocket->hMcpf, HAL_SOCKET_MODULE_LOG, 
			("mcp_hal_socket_Send: Send Failed!"));
		return RES_ERROR;
	}
	else
	{
		MCPF_REPORT_INFORMATION(pHalSocket->hMcpf, HAL_SOCKET_MODULE_LOG, 
			("mcp_hal_socket_Send: Send was succesful! bytesSent = %d", bytesSent));
		return RES_OK;
	}
}


/************************************************************************/
/* Internal Functions Implementation                                    */
/************************************************************************/
/** 
 * \fn     serverSocket_ThreadFuncThreadFunc 
 * \brief  Server Socket Thread Function, where the 
 *		   Incoming connections From Socket is handled
 * 
 **/
DWORD WINAPI serverSocket_ThreadFunc(LPVOID hSocket)
{
	hal_socket_t	*pHalSock = (hal_socket_t *)hSocket;
	SOCKADDR_IN		from;
	int				fromlen = sizeof(from);
	HANDLE			clientThread;
	unsigned long	client_Handle;
	client_handle_t	*hClientHandle;
	SOCKET			clienSockId;
	McpBool			bInLoop = MCP_TRUE;

	while(bInLoop)
	{
		clienSockId = accept(pHalSock->sockId,(SOCKADDR*)&from,&fromlen);

		if(clienSockId == INVALID_SOCKET)
		{
			MCPF_REPORT_INFORMATION(pHalSock->hMcpf, HAL_SOCKET_MODULE_LOG, 
				("serverSocket_ThreadFunc: accept() returned INVALID_SOCKET!"));
			return 0;
		}
		
		hClientHandle = (client_handle_t *)mcpf_mem_alloc_from_pool(pHalSock->hMcpf, pHalSock->hClientsPool);
		if(hClientHandle == NULL)
		{
			MCPF_REPORT_ERROR(pHalSock->hMcpf, HAL_SOCKET_MODULE_LOG, 
				("serverSocket_ThreadFunc: Failed to allocate client structure!"));
			break;
		}

		hClientHandle->hHalSock = hSocket;
		hClientHandle->clientSockId = clienSockId;

		hClientHandle->hCaller = pHalSock->onAcceptCb(pHalSock->hHostSocket, hClientHandle->clientSockId, 
														&hClientHandle->uNumOfBytesToRead);
		
		if(hClientHandle->hCaller == NULL)
		{
			MCPF_REPORT_ERROR(pHalSock->hMcpf, HAL_SOCKET_MODULE_LOG, 
				("serverSocket_ThreadFunc: OnAcceptCb() Failed!"));
			mcpf_mem_free_from_pool(pHalSock->hMcpf, hClientHandle);
			break;
		}
		
		clientThread = CreateThread(NULL,0,clientSocket_ThreadFunc,hClientHandle,0,(LPDWORD)&client_Handle);
	}
	return 0;
}

/** 
 * \fn     clientSocket_ThreadFunc 
 * \brief  Call Back Function Where the Multiple Clients are handled
 * 
 * 
 */
DWORD WINAPI clientSocket_ThreadFunc(LPVOID hClientHandle)
{
	client_handle_t	*pClientHandle = (client_handle_t *)hClientHandle;
	hal_socket_t	*pHalSock = (hal_socket_t *)pClientHandle->hHalSock;
	McpS8		IncData[1000];
	McpS32		recvLen = 0;
	McpU32		uMoreBytesToRead;
	McpBool			bInLoop = MCP_TRUE;
	
	while(bInLoop)
	{
		recvLen = 0;
		do
		{
			recvLen += recv(pClientHandle->clientSockId, &IncData[recvLen], 
				pClientHandle->uNumOfBytesToRead-recvLen, 0);
		} while ( (recvLen > 0) && (recvLen < pClientHandle->uNumOfBytesToRead) );

		if(recvLen == 0)
		{
			MCPF_REPORT_ERROR(pHalSock->hMcpf, HAL_SOCKET_MODULE_LOG, 
				("clientSocket_ThreadFunc: Error on recv()"));
			break;
		}
		else
		{
			uMoreBytesToRead = pHalSock->onRecvCb(pClientHandle->hCaller, IncData, (McpS16)recvLen);
			

			memset((void*)&IncData, 0, 1000);
			
			if(uMoreBytesToRead != 0)
			{
				recvLen = 0;
				do
				{
					recvLen += recv(pClientHandle->clientSockId, &IncData[recvLen], 
						uMoreBytesToRead-recvLen, 0);
					
				} while ( (recvLen > 0) && ((McpU32)recvLen < uMoreBytesToRead) );
				
				if(recvLen == 0)
				{
					MCPF_REPORT_ERROR(pHalSock->hMcpf, HAL_SOCKET_MODULE_LOG, 
						("clientSocket_ThreadFunc: Error on recv()"));
					break;
				}
				else
				{
					uMoreBytesToRead = pHalSock->onRecvCb(pClientHandle->hCaller, IncData, (McpS16)recvLen);
				
					memset((void*)&IncData, 0, 1000);
				}
			}
		}
	}

	pHalSock->onCloseCb(pClientHandle->hCaller);
	mcpf_mem_free_from_pool(pHalSock->hMcpf, hClientHandle);
	return 0;
}
