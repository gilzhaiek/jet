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
 *  \brief  Linux OS Adaptation Layer for Socket Interface implementation
 *
 *  \see    mcp_hal_socket.h
 */
#include <stdio.h>
#include <sys/types.h>
#include <sys/socket.h>

/*TI_PATCH start*/
#include <sys/un.h>
#include <errno.h>
/*TI_PATCH end*/

#include <netinet/in.h>
#include <string.h>
#include <stdlib.h>
#include <pthread.h>
#include <errno.h>
#include <fcntl.h>
#include "mcpf_services.h"
#include "mcp_hal_os.h"
#include "mcp_hal_fs.h"
#include "mcp_hal_socket.h"
#include "mcp_hal_types.h"
#include "mcpf_mem.h"
#include "mcpf_report.h"
#include "utils/Log.h"
/************************************************************************/
/* Internal Structures			                                        */
/************************************************************************/
typedef struct 
{
	handle_t				hMcpf;
	handle_t				hHostSocket;
	handle_t				hClientsPool;
	tHalSockOnAcceptCb		onAcceptCb;
	tHalSockOnRecvCb		onRecvCb;
	tHalSockOnCloseCb		onCloseCb;
	McpU32					sockId;

} hal_socket_t;

typedef struct
{
	McpU32		clientSockId;
	handle_t	hHalSock;
	handle_t	hCaller;
	McpU8		uNumOfBytesToRead;
} client_handle_t;


/*TI_PATCH start*/
#define SOC_NAME_4121		  "/data/gps/gps4121"
//#define SOC_NAME_4121		  "/system/etc/gps/gps4121"
/*TI_PATCH start*/

#ifndef DISABLE_ADS_ROUTER
McpU32	adsSockId;
void adsSocket_ThreadFunc();
#endif


/************************************************************************/
/* Internal Functions Definitions                                       */
/************************************************************************/
void serverSocket_ThreadFunc();
void clientSocket_ThreadFunc();


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
	/*TI_PATCH start*/
	//  struct sockaddr_in  lSockAddr;
	    struct sockaddr_un  lSockAddr;
	/*TI_PATCH end*/

#ifndef DISABLE_ADS_ROUTER
	  struct sockaddr_in  adsSockAddr;
	  pthread_t     threadAds;
	  McpU32 hThreadAds;

#endif

	//WSADATA wsaData;
	//unsigned long q_Handle;
	//HANDLE hThread;
	McpU32 hThread;
	hal_socket_t *pHalSocket;
	pthread_t     thread1;
	int flags;

	/* TI_PATCH start */
	    int tmp_errno;
	    int iret;
	/* TI_PATCH end */


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

	/*TI_PATCH start*/
	//  pHalSocket->sockId = socket(AF_INET,SOCK_STREAM,IPPROTO_TCP);
	    pHalSocket->sockId = socket(AF_UNIX,SOCK_STREAM,0);
	/*TI_PATCH end*/

	if(pHalSocket->sockId < 0)
	{
		MCPF_REPORT_ERROR(hMcpf, HAL_SOCKET_MODULE_LOG,
			("mcp_hal_socket_CreateServerSocket: socket() Failed!"));
		mcpf_memory_pool_destroy(hMcpf, pHalSocket->hClientsPool);
		mcpf_mem_free(hMcpf, pHalSocket);
		return NULL;
	}
    if (setsockopt(pHalSocket->sockId,SOL_SOCKET,SO_REUSEADDR,&flags,sizeof(int)) == -1)
    {
        perror("setsockopt");
		LOGD("perror");
    }

	memset(&lSockAddr,0, sizeof(lSockAddr));



	/*TI_PATCH start*/
	#if 0
	    lSockAddr.sin_family = AF_INET;
	    lSockAddr.sin_port = htons(port);
	    lSockAddr.sin_addr.s_addr = INADDR_ANY;
	#else
	    lSockAddr.sun_family = AF_UNIX;
	    strcpy(lSockAddr.sun_path, SOC_NAME_4121);
	#endif
	/*TI_PATCH end*/


	/*TI_PATCH start*/
	LOGD("mcp_hal_socket_CreateServerSocket: unlink()\n\r");
	    iret = unlink(SOC_NAME_4121);
	    tmp_errno = errno;

	    if( ( iret != 0 ) && ( tmp_errno != ENOENT ) )
	    {
	LOGD("mcp_hal_socket_CreateServerSocket: unlink() Failed!\n\r");
	        close(pHalSocket->sockId);
	        mcpf_memory_pool_destroy(hMcpf, pHalSocket->hClientsPool);
	        mcpf_mem_free(hMcpf, pHalSocket);
	        return NULL;
	    }
	/*TI_PATCH end*/

	/*  Ignore It on KW */
	/* Bind - links the socket we just created with the sockaddr_in
	   structure. Basically it connects the socket with
	   the local address and a specified port.
	   If it returns non-zero - quit, as this indicates error. */
   if(bind(pHalSocket->sockId,(struct sockaddr *)&lSockAddr,sizeof(lSockAddr)) != 0)
    {
		MCPF_REPORT_ERROR(hMcpf, HAL_SOCKET_MODULE_LOG,
			("mcp_hal_socket_CreateServerSocket: bind() Failed!"));
		close(pHalSocket->sockId);
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
		close(pHalSocket->sockId);
		mcpf_memory_pool_destroy(hMcpf, pHalSocket->hClientsPool);
		mcpf_mem_free(hMcpf, pHalSocket);
        return NULL;
    }
	hThread = pthread_create( &thread1, NULL, serverSocket_ThreadFunc, pHalSocket);

	      if (chmod(SOC_NAME_4121, 0777) < 0) {

	      LOGE("Error changing permissions of %s to 0777: %s",SOC_NAME_4121, strerror(errno));

	     }
	      else {
		LOGD("permission successful on socket");
		     }

#ifndef DISABLE_ADS_ROUTER
	  adsSockId = socket(AF_INET,SOCK_STREAM,IPPROTO_TCP);

	if(adsSockId < 0)
	{
		MCPF_REPORT_ERROR(hMcpf, HAL_SOCKET_MODULE_LOG,
			("mcp_hal_socket_CreateServerSocket: ADS socket() Failed!"));
		goto end_create_socket;
	}

    if (setsockopt(adsSockId,SOL_SOCKET,SO_REUSEADDR,&flags,sizeof(int)) == -1)
    {
        perror("setsockopt");
		LOGD("perror");
		goto end_create_socket;
    }

	memset(&adsSockAddr,0, sizeof(adsSockAddr));

    adsSockAddr.sin_family = AF_INET;
    adsSockAddr.sin_port = htons(port);
    adsSockAddr.sin_addr.s_addr = INADDR_ANY;

   if(bind(adsSockId,(struct sockaddr *)&adsSockAddr,sizeof(adsSockAddr)) != 0)
    {
		MCPF_REPORT_ERROR(hMcpf, HAL_SOCKET_MODULE_LOG,
			("mcp_hal_socket_CreateServerSocket: ADS bind() Failed!"));
		close(adsSockId);
		goto end_create_socket;
    }

    /* Listen - instructs the socket to listen for incoming
       connections from clients. The second arg is the backlog. */
    if(listen(adsSockId,5) != 0)
    {
		MCPF_REPORT_ERROR(hMcpf, HAL_SOCKET_MODULE_LOG,
			("mcp_hal_socket_CreateServerSocket: ADS listen() Failed!"));
		close(adsSockId);
        goto end_create_socket;
    }

	hThreadAds = pthread_create( &threadAds, NULL, adsSocket_ThreadFunc, pHalSocket);

#endif

end_create_socket:
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
	McpU32			hThread;
	pthread_t		thread1;

	/* This function is blocking, and is running in the application main thread */
	serverSocket_ThreadFunc(pHalSock);
}

/**
 * \fn     mcp_hal_socket_DestroyServerSocket
 * \brief  Socket Initializing and Binding at the Server side
 *
 */
void mcp_hal_socket_DestroyServerSocket(handle_t hHalSocket)
{
	hal_socket_t	*pHalSock = (hal_socket_t *)hHalSocket;
    int flags;
		/* TODO: Need to destroy Thread */

    flags = fcntl(pHalSock->sockId, F_GETFL, 0);
    if(fcntl(pHalSock->sockId, F_SETFL, flags | O_NONBLOCK) == -1)
    {
		LOGD("Error setting the socket to Non-Blocking mode\n\r");
        return NULL;
    }
    shutdown(pHalSock->sockId, SHUT_RDWR);
    close(pHalSock->sockId);
    pHalSock->sockId = -1;
}

/**
 * \fn     mcp_hal_socket_Send
 * \brief  Send data back to the client
 *
 */
EMcpfRes mcp_hal_socket_Send(handle_t hHalSocket, McpU32 clientSockId, McpS8* buf, McpU16 bufLen)
{
	hal_socket_t* pHalSocket = (hal_socket_t*)hHalSocket;
	McpU32	clientSock = (McpU32)clientSockId;
	McpU32	bytesSent = 0;

	do
	{
		bytesSent += send(clientSock, &buf[bytesSent], bufLen-bytesSent, 0);
	} 
   while ( (bytesSent > 0) && (bytesSent < bufLen) );

	if (bytesSent == 0)
	{
		MCPF_REPORT_ERROR(pHalSocket->hMcpf, HAL_SOCKET_MODULE_LOG,
			("mcp_hal_socket_Send: Send Failed!"));
		return RES_ERROR;
	}

	return RES_OK;
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
void serverSocket_ThreadFunc(void* hSocket)
{
	hal_socket_t	*pHalSock = (hal_socket_t *)hSocket;
	struct sockaddr_un 	from;
	int				fromlen = sizeof(from);
	McpU32			clientThread;
	unsigned long	client_Handle;
	client_handle_t	*hClientHandle;
	McpS32			clienSockId;
	McpBool			bInLoop = MCP_TRUE;
	pthread_t       thread2;

	MCPF_REPORT_INFORMATION(pHalSock->hMcpf, HAL_SOCKET_MODULE_LOG,
				("serverSocket_ThreadFunc: Entering..."));

	while(bInLoop)
	{
		clienSockId = accept(pHalSock->sockId,(struct sockaddr *)&from,&fromlen);
		MCPF_REPORT_INFORMATION(pHalSock->hMcpf, HAL_SOCKET_MODULE_LOG,
			("serverSocket_ThreadFunc: accept() for sockId %d, socket descriptor is %d", pHalSock->sockId, clienSockId ));

		if(clienSockId < 0)
		{
			MCPF_REPORT_INFORMATION(pHalSock->hMcpf, HAL_SOCKET_MODULE_LOG,
				("serverSocket_ThreadFunc: accept() returned INVALID_SOCKET! clienSockId is %d", clienSockId ));
			//continue;
		}
		else
		{

			hClientHandle = (client_handle_t *)mcpf_mem_alloc_from_pool(pHalSock->hMcpf, pHalSock->hClientsPool);
			if(hClientHandle == NULL)
			{
				MCPF_REPORT_ERROR(pHalSock->hMcpf, HAL_SOCKET_MODULE_LOG,
					("serverSocket_ThreadFunc: Failed to allocate client structure!"));
				break;
			}

			hClientHandle->hHalSock = hSocket;
			hClientHandle->clientSockId = (McpU32)clienSockId;

			hClientHandle->hCaller = pHalSock->onAcceptCb(pHalSock->hHostSocket, hClientHandle->clientSockId,
															&hClientHandle->uNumOfBytesToRead);

			if(hClientHandle->hCaller == NULL)
			{
				MCPF_REPORT_ERROR(pHalSock->hMcpf, HAL_SOCKET_MODULE_LOG,
					("serverSocket_ThreadFunc: OnAcceptCb() Failed!"));
				mcpf_mem_free_from_pool(pHalSock->hMcpf, hClientHandle);
				break;
			}
			clientThread = pthread_create( &thread2, NULL, clientSocket_ThreadFunc, hClientHandle);
	 }
	}
}

#ifndef DISABLE_ADS_ROUTER
/************************************************************************/
/* Internal Functions Implementation                                    */
/************************************************************************/
/**
 * \fn     serverSocket_ThreadFuncThreadFunc
 * \brief  Server Socket Thread Function, where the
 *		   Incoming connections From Socket is handled
 *
 **/
void adsSocket_ThreadFunc(void* hSocket)
{
	hal_socket_t	*pHalSock = (hal_socket_t *)hSocket;
	struct sockaddr_in 	from;
	int				fromlen = sizeof(from);
	McpU32			clientThread;
	unsigned long	client_Handle;
	client_handle_t	*hClientHandle;
	McpS32			clienSockId;
	McpBool			bInLoop = MCP_TRUE;
	pthread_t       thread2;

	MCPF_REPORT_INFORMATION(pHalSock->hMcpf, HAL_SOCKET_MODULE_LOG,
				("serverSocket_ThreadFunc: Entering..."));

	while(bInLoop)
	{
		clienSockId = accept(adsSockId,(struct sockaddr *)&from,&fromlen);
		MCPF_REPORT_INFORMATION(pHalSock->hMcpf, HAL_SOCKET_MODULE_LOG,
			("adsSocket_ThreadFunc: accept() for sockId %d, socket descriptor is %d", pHalSock->sockId, clienSockId ));

		if(clienSockId < 0)
		{
			MCPF_REPORT_INFORMATION(pHalSock->hMcpf, HAL_SOCKET_MODULE_LOG,
				("adsSocket_ThreadFunc: ADS accept() returned INVALID_SOCKET! clienSockId is %d", clienSockId ));
			//continue;
		}
		else
		{

			hClientHandle = (client_handle_t *)mcpf_mem_alloc_from_pool(pHalSock->hMcpf, pHalSock->hClientsPool);
			if(hClientHandle == NULL)
			{
				MCPF_REPORT_ERROR(pHalSock->hMcpf, HAL_SOCKET_MODULE_LOG,
					("adsSocket_ThreadFunc: Failed to allocate client structure!"));
				break;
			}

			hClientHandle->hHalSock = hSocket;
			hClientHandle->clientSockId = (McpU32)clienSockId;

			hClientHandle->hCaller = pHalSock->onAcceptCb(pHalSock->hHostSocket, hClientHandle->clientSockId,
															&hClientHandle->uNumOfBytesToRead);

			if(hClientHandle->hCaller == NULL)
			{
				MCPF_REPORT_ERROR(pHalSock->hMcpf, HAL_SOCKET_MODULE_LOG,
					("adsSocket_ThreadFunc: OnAcceptCb() Failed!"));
				mcpf_mem_free_from_pool(pHalSock->hMcpf, hClientHandle);
				break;
			}
			clientThread = pthread_create( &thread2, NULL, clientSocket_ThreadFunc, hClientHandle);
	 }
	}
}
#endif


/**
 * \fn     clientSocket_ThreadFunc
 * \brief  Call Back Function Where the Multiple Clients are handled
 *
 *
 */
void clientSocket_ThreadFunc(void* hClientHandle)
{
	client_handle_t	*pClientHandle = (client_handle_t *)hClientHandle;
	hal_socket_t	*pHalSock = (hal_socket_t *)pClientHandle->hHalSock;
	McpS8		IncData[9000];
	McpS32		recvLen = 0;
	McpU32		uMoreBytesToRead;
	McpBool			bInLoop = MCP_TRUE;

	while (bInLoop)
	{
		recvLen = 0;
		do
		{
			recvLen += recv(pClientHandle->clientSockId, &IncData[recvLen],
				pClientHandle->uNumOfBytesToRead-recvLen, 0);

			MCPF_REPORT_INFORMATION(pHalSock->hMcpf, HAL_SOCKET_MODULE_LOG,
				("serverSocket_ThreadFunc: recv() is passed the clienSockId of %d", pClientHandle->clientSockId));

		} while ( (recvLen > 0) && (recvLen < pClientHandle->uNumOfBytesToRead) );

		if(recvLen < 0)
		{
			MCPF_REPORT_ERROR(pHalSock->hMcpf, HAL_SOCKET_MODULE_LOG,
				("clientSocket_ThreadFunc: Error on recv() 1,Error code returned is %s",strerror(errno)));
			//break;
		}
		else if(recvLen == 0)
		{
			MCPF_REPORT_ERROR(pHalSock->hMcpf, HAL_SOCKET_MODULE_LOG,
				("clientSocket_ThreadFunc: sokcet was closed by peer 1"));
			close(pClientHandle->clientSockId);
			break;
		}
		else
		{
			uMoreBytesToRead = pHalSock->onRecvCb(pClientHandle->hCaller, IncData, (McpS16)recvLen);


			memset((void*)&IncData, 0, 9000);

			if(uMoreBytesToRead != 0)
			{
				recvLen = 0;
				do
				{
					recvLen += recv(pClientHandle->clientSockId, &IncData[recvLen],
						uMoreBytesToRead-recvLen, 0);

				} while ( (recvLen > 0) && ((McpU32)recvLen < uMoreBytesToRead) );

				if(recvLen < 0)
				{
					MCPF_REPORT_ERROR(pHalSock->hMcpf, HAL_SOCKET_MODULE_LOG,
						("clientSocket_ThreadFunc: Error on recv() 2, Error code returned is %s",strerror(errno) ));
					break;
				}
				else if(recvLen == 0)
				{
					MCPF_REPORT_ERROR(pHalSock->hMcpf, HAL_SOCKET_MODULE_LOG,
						("clientSocket_ThreadFunc: sokcet was closed by peer 2"));
				    close(pClientHandle->clientSockId);
					break;
				}
				else
				{
					uMoreBytesToRead = pHalSock->onRecvCb(pClientHandle->hCaller, IncData, (McpS16)recvLen);

					memset((void*)&IncData, 0, 9000);
				}
			}
		}
	}

	pHalSock->onCloseCb(pClientHandle->hCaller);
	mcpf_mem_free_from_pool(pHalSock->hMcpf, hClientHandle);
}
