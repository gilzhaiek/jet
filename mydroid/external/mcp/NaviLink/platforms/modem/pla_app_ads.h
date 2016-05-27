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

/** \file   pla_app_ads.h 
 *  \brief  Platform Application Layer internal types and function APIs specification
 * 
 * 
 */

#ifndef PLA_APP_ADS
#define PLA_APP_ADS

#include "ads_api.h"
#include "mcp_hal_st.h"

#define MAX_ADS_PAYLOAD 	3000

/*  ADS supported protocol ID-s */
typedef enum
{
	ADS_PROT_PRIVATE,
	ADS_PROT_RRLP,
	ADS_PROT_RRC

} EADS_ProtocolId;

typedef union 
{
	struct 
	{
		AdsPrefix_t	Header;
		McpU8		Payload[MAX_ADS_PAYLOAD];
		McpU16		PayloadLength;
	}s;
	McpU8 Buffer[MAX_ADS_PAYLOAD+sizeof(AdsPrefix_t)];
} AdsMsg_u;

typedef struct _AdsPort_t 
{
	handle_t		hAds;
	handle_t		hUart;
	handle_t		hTxCritSec;
	handle_t		hTxQueue;
	handle_t		hTxCmdPool;
	handle_t		hRrlp;
	handle_t		hRrc;
	EADS_ProtocolId eProtocol;
	McpBool			bTxInProcess;
	McpU16			TaskId;
	THalSt_PortCfg	tPortCfg;
	enum { READ_ADS_HEADER, READ_ADS_BUFFER } eRxState;
	McpU16			readBytes;
	McpU16			reqBytes;
	AdsMsg_u		RxAdsMsg;
	AdsMsg_u		TxAdsMsg;
	
	struct _AdsPort_t	*next;
} AdsPort_t;


typedef struct  
{
	handle_t	hMcpf;
	AdsPort_t	*hPort;
	handle_t	hCmdPool;
} Ads_t;


#define APP_ID_FILTER			100
#define C_RECIP_LSB_POS_SIGMA	10.0  /* 1/C_LSB_POS_SIGMA */

/** 
 * \fn     ads_process_msg 
 * \brief  Process recieved ADS message
 * 
 * Process received ADS message according to ADS message class and type
 * 
 * \note
 * \param	pPort    - handle to ADS port
 * \param	TaskId   - ADS port registered MCPF task ID
 * \param   pMsg     - pointer to buffer containing ADS received message
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_SendMsg
 */
void ads_process_msg (AdsPort_t *pPort, McpU16 TaskId, AdsMsg_u *pMsg);

/** 
 * \fn     ads_process_msg 
 * \brief  Process recieved ADS message
 * 
 * Process received ADS message according to ADS message class and type
 * 
 * \note
 * \param	pPort    - handle to ADS port
 * \param	bAdsMsg  - boolean flag set to TRUE when ADS internal message is sent
 * \param   pInMsg   - pointer to input ADS message
 * \param   pOutMsg  - pointer to generated output ADS message
 * \return 	Result of operation: OK or ERROR
 * \sa     	
 */
McpU16 ads_generate_msg (AdsPort_t *pPort, McpBool bAdsMsg, TmcpfMsg *pInMsg, AdsMsg_u *pOutMsg);

#endif /*PLA_APP_ADS*/	
