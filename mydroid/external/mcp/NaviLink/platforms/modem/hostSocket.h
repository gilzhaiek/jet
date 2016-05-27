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

/** \file   hostSocket.h 
 *  \brief  Host Socket internal definitions
 * 
 * This file contains Host Socket internal definitions
 * 
 *  see hostSocket.c
 */

#ifndef __HOST_SOCKET_H
#define __HOST_SOCKET_H

#include "hostSocket_api.h"

/************************************************************************/
/*						Definitions                                     */
/************************************************************************/
#define HOSTSOCKET_MESSAGE_HEADER_SIZE	8
#define	HOSTSOCKET_MESSAGE_SYNC_START	0xF3F2F1F0


/************************************************************************/
/*						Enums & Structures                              */
/************************************************************************/
typedef enum
{
	HOSTSOCK_WAIT_HEADER,
	HOSTSOCK_WAIT_PAYLOAD
} hostSocket_clinetState_e;

typedef struct  
{
	McpU32	syncSrart;
	McpU8	msgClass;
	McpU8	opCode;
	McpU16	payloadLen;
} hostSocket_header_t;

typedef struct  
{
	handle_t	hMcpf;
	handle_t	hNavl;
	handle_t	hHalSock;
	handle_t	hClientsPool;
	handle_t	hNavlMsgPool;
} hostSocket_t;

typedef struct  
{
	handle_t					hCritSec;
	handle_t					hHostSock;
	handle_t					hNavlClient;
	McpU32						uClientSockId;
	hostSocket_clinetState_e	clientState;
	hostSocket_header_t			currentMsgHdr;
} hostSocket_client_t;

#endif /* __HOST_SOCKET_H */
