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


/** \file   mcp_hal_socket.h 
 *  \brief  Hardware/OS Adaptation Layer for Socket Interface API-s
 * 
 * 
 * The HAL Socket is OS and Bus dependent module.  
 *
 *  \see    mcp_hal_socket.c
 */

#ifndef __MCP_HAL_SOCKET_H
#define __MCP_HAL_SOCKET_H

#include "mcpf_defs.h"

/************************************************************************/
/*							 Definitions                                */
/************************************************************************/
#define	SOCKET_CLIENT_NUM 12

typedef handle_t	(*tHalSockOnAcceptCb) (handle_t, McpU32, McpU8 *);
typedef McpU16		(*tHalSockOnRecvCb) (handle_t, McpS8 *, McpS16);
typedef void		(*tHalSockOnCloseCb) (handle_t);


/************************************************************************/
/*							Structure                                   */
/************************************************************************/


/************************************************************************/
/*						APIs Definitions                                */
/************************************************************************/
/** 
 * \fn     mcp_hal_socket_CreateServerSocket
 * \brief  Socket Initializing and Binding at the Server side
 * 
 */
handle_t mcp_hal_socket_CreateServerSocket(handle_t hMcpf, handle_t hHostSocket, 
										   McpS16 port, tHalSockOnAcceptCb onAcceptCb, 
										   tHalSockOnRecvCb onRecvCb, tHalSockOnCloseCb onCloseCb);

/** 
 * \fn     mcp_hal_socket_Accept
 * \brief  Socket Accept at the Server side
 * 
 */
void mcp_hal_socket_Accept(handle_t hHalSocket);


/** 
 * \fn     mcp_hal_socket_DestroyServerSocket
 * \brief  Socket Initializing and Binding at the Server side
 * 
 */
void mcp_hal_socket_DestroyServerSocket(handle_t hHalSocket);


/** 
 * \fn     mcp_hal_socket_Send 
 * \brief  Send data back to the client
 * 
 */
EMcpfRes mcp_hal_socket_Send(handle_t hHalSocket, McpU32 clientSockId, McpS8 *buf, McpU16 bufLen);


#endif /* __MCP_HAL_SOCKET_H */
