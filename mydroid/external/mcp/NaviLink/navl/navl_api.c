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

/** \file   Navl_api.c
 *  \brief  Navigation Liberary
 *
 *
 */

#include "mcpf_defs.h"
#include "mcpf_mem.h"
#include "mcpf_msg.h"
#include "mcpf_services.h"
#include "mcpf_time.h"
#include "mcpf_report.h"
#include "navc_api.h"
#include "navc_rrlp.h"
#include "navc_rrc.h"
#include "math.h"
#include "mcp_hal_os.h"
#include "mcp_hal_types.h"
#include "navl_api.h"
#include "navl.h"
#include "suplc_defs.h"
#include "suplc_api.h"
#include "gpsc_msg.h"

#include <sys/wait.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <netdb.h>
#include <netinet/in.h>

#include <utils/Log.h>
#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG "NAVD.NAVL_API"    // our identification for logcat (adb logcat NAVD.NAVL_API:V *:S)

#define MCPF_SOCKET_PORT_ADS        4122
#define MCPF_HOST_NAME_ADS          "localhost"



/************************************************************************
 * Defines
 ************************************************************************/
/************************************************************************
 * Types
 ************************************************************************/

/************************************************************************
 * Internal functions prototypes
 ************************************************************************/
EMcpfRes navl_process_rr (handle_t hNavl_Client, McpU8 Opcode, McpU16 Length, NavlMsg_t *Param);
EMcpfRes navl_process_gps (handle_t hNavl_Client, McpU8 Opcode, McpU16 Length, NavlMsg_t *Param);
EMcpfRes navl_process_internal (handle_t hNavl_Client, McpU8 Opcode, McpU16 Length, NavlMsg_t *Param);
EMcpfRes navl_process_supl (handle_t hNavl_Client, McpU8 Opcode, McpU16 Length, NavlMsg_t *Param);
EMcpfRes navl_generate_msg (handle_t hNavl_Client, McpU32 uUnused1, McpU8 uUnused2, TmcpfMsg *mcpfMsg);
EMcpfRes navl_process_wpl(handle_t hNavl_Client, McpU8 Opcode, McpU16 Length, NavlMsg_t *Param);



/************************************************************************
 *
 *   Module functions implementation
 *
 ************************************************************************/

/**
 * \fn     NAVL_Create
 * \brief  Initialization of the NAVL layer
 *
 *  Create memory pool, init structures, init semaphores
 *
 * \note
 * \param	hMcpf	- handle to OS Framework
 * \return 	handle to initialized NAVL structure
 * \sa     	NAVL_Create
 **/
handle_t NAVL_Create(handle_t hMcpf)
{

	EMcpfRes retcode = 0;
	NAVL_t* pNavl = NULL;

	pNavl = (NAVL_t*)mcpf_mem_alloc(hMcpf, (McpU32)(sizeof(NAVL_t)));
	if (pNavl == NULL)
	{
		MCPF_REPORT_ERROR(hMcpf, NAVL_MODULE_LOG, ("NAVL_Create(): failed to allocate NAVL structure"));
		return NULL;
	}

	pNavl->hMcpf = hMcpf;
	/* below pool create has been changed after supl integration, change is being made
	   wrt to element number against 16 and 4 respectively*/
	pNavl->hRRCmdPool = mcpf_memory_pool_create(hMcpf, sizeof(TNAVC_cmdParams), 24);
	pNavl->hRREvtPool = mcpf_memory_pool_create(hMcpf, sizeof(NavlMsg_t), 8);

	retcode = mcpf_critSec_CreateObj(hMcpf, "NAVL", &pNavl->pCritSecHandle);
	if (retcode != RES_OK)
	{
		MCPF_REPORT_ERROR(hMcpf, NAVL_MODULE_LOG, ("NAVL_Create(): mcpf_critSec_CreateObj failed"));
		mcpf_memory_pool_destroy(hMcpf, pNavl->hRREvtPool);
		mcpf_memory_pool_destroy(hMcpf, pNavl->hRRCmdPool);
		mcpf_mem_free(hMcpf, pNavl);

		return NULL;
	}

	return (handle_t)pNavl;

}


/**
 * \fn     NAVL_Destroy
 * \brief  Destroys NAVL Module
 *
 *  Free memory pool & structures
 *
 * \note
 * \param	hNavl	- handle to NAVL
 * \return 	response codes
 * \sa     	NAVL_Destroy
 **/
EMcpfRes NAVL_Destroy(handle_t hNavl)
{
	NAVL_t* pNavl = (NAVL_t*)hNavl;

	mcpf_critSec_DestroyObj(pNavl->hMcpf, &pNavl->pCritSecHandle);
	mcpf_memory_pool_destroy(pNavl->hMcpf, pNavl->hRREvtPool);
	mcpf_memory_pool_destroy(pNavl->hMcpf, pNavl->hRRCmdPool);
	mcpf_mem_free(pNavl->hMcpf, pNavl);

	return RES_OK;
}


/**
 * \fn     NAVL_Open
 * \brief  Open communication channel
 *
 *  Allocates client structures, initializes RRLP and RRC libs
 *  and registers the task and callback.
 *
 * \note
 * \param	hNAVL	- handle to NAVL
 * \param 	hCaller - handle to calling task
 * \param	fCb - Callback function to be registered
 * \return	handle to NAVL client structure
 * \sa     	NAVL_Open
 **/
handle_t NAVL_Open(handle_t hNAVL, handle_t hCaller, tNavlSendCb fCb)
{
	NAVL_t *pNavl = (NAVL_t *)hNAVL;
	NAVL_Client_t *pNavl_Client;
	EMcpfRes response;

	pNavl_Client = (NAVL_Client_t*)mcpf_mem_alloc(pNavl->hMcpf, (McpU32)(sizeof(NAVL_Client_t)));

	if (pNavl_Client == NULL)
		return NULL;

	pNavl_Client->hNavl = hNAVL;
	pNavl_Client->hCaller = hCaller;
	pNavl_Client->fCb = fCb;
	pNavl_Client->eProtocol = NAVL_PROT_PRIVATE;

	response = mcpf_RegisterClientCb(pNavl->hMcpf, (McpU32*)&pNavl_Client->TaskId,
			navl_generate_msg, (handle_t)pNavl_Client);

	if (response != RES_OK)
	{
		MCPF_REPORT_ERROR(pNavl->hMcpf, NAVL_MODULE_LOG, ("NAVL_Open(): mcpf_RegisterClientCb failed"));
		mcpf_mem_free(pNavl->hMcpf, pNavl_Client);
		return NULL;
	}

	/* Init RRLP */
	pNavl_Client->hRrlp = RRLP_Init (pNavl->hMcpf, pNavl->hRRCmdPool, pNavl->hRREvtPool, pNavl_Client->TaskId, 0);
	if (pNavl_Client->hRrlp == NULL)
	{
		MCPF_REPORT_ERROR(pNavl->hMcpf, NAVL_MODULE_LOG, ("NAVL_Open: RRLP_Init failed"));
		mcpf_UnRegisterClientCb(pNavl->hMcpf, pNavl_Client->TaskId);
		mcpf_mem_free(pNavl->hMcpf, pNavl_Client);

		return NULL;
	}

	/* Init RRC */
	pNavl_Client->hRrc = RRC_Init (pNavl->hMcpf, pNavl->hRRCmdPool, pNavl->hRREvtPool, pNavl_Client->TaskId, 0);
	if (pNavl_Client->hRrc == NULL)
	{
		MCPF_REPORT_ERROR(pNavl->hMcpf, NAVL_MODULE_LOG, ("NAVL_Open: RRC_Init failed"));
		RRLP_Destroy(pNavl_Client->hRrlp);
		mcpf_UnRegisterClientCb(pNavl->hMcpf, pNavl_Client->TaskId);
		mcpf_mem_free(pNavl->hMcpf, pNavl_Client);

		return NULL;
	}

	return (handle_t)pNavl_Client;
}


/**
 * \fn     NAVL_Close
 * \brief  Close NAVL Client Module
 *
 *  Free memory & structures
 *
 * \note
 * \param	hNavl_Client	- handle to NAVL Client
 * \return 	response codes
 * \sa     	NAVL_Close
 **/
EMcpfRes NAVL_Close(handle_t hNavl_Client)
{
	NAVL_Client_t* pNavl_Client = 0;
	NAVL_t* pNavl = 0;

	if (hNavl_Client == NULL)
		return RES_OK;

	pNavl_Client = (NAVL_Client_t*)hNavl_Client;
	pNavl = (NAVL_t*)pNavl_Client->hNavl;

	RRC_Destroy(pNavl_Client->hRrc);
	RRLP_Destroy(pNavl_Client->hRrlp);

	mcpf_UnRegisterClientCb(pNavl->hMcpf, pNavl_Client->TaskId);
	mcpf_mem_free(pNavl->hMcpf, pNavl_Client);

	return RES_OK;
}

/**
 * Function:        connectWithMcpfSocket
 * Brief:           Creates a socket and connects with it.
 * Description:     A socket is exposed by MCPF for external applications to communicate
 with it. Helper Service writes the requests on this socket.
 * Note:            Internal function.
 * Params:          u16_inPortNumber - Port Number.
 p_inHostName - Host Name.
 * Return:          Success: HSJNI_SUCCESS.
Failure: HSJNI_FAILURE_XXX.
 */
static McpU16 connectWithMcpfSocket(const McpU16 u16_inPortNumber,
		const McpU8 *const p_inHostName,
		McpS16 *const p_sockDescriptor)
{
	struct sockaddr_in serverAddress;       /* Internet Address of Server. */
	struct hostent *p_host = NULL;          /* The host (server) info. */
	McpS16 u16_sockDescriptor = -1;
	McpS8 retVal = 0;

	/* Obtain host information. */
	p_host = gethostbyname((char *)p_inHostName);
	if (p_host == (struct hostent*) NULL )
		return 1;

	/* Clear the structure. */
	memset( &serverAddress, 0, sizeof(serverAddress) );

	/* Set address type. */
	serverAddress.sin_family = AF_INET;
	memcpy(&serverAddress.sin_addr, p_host->h_addr, p_host->h_length);
	serverAddress.sin_port = htons(u16_inPortNumber);

	/* Create a new socket. */
	u16_sockDescriptor = socket(AF_INET, SOCK_STREAM, 0);
	if (u16_sockDescriptor < 0)
		return -1;

	/* Connect with MCPF. */
	retVal = connect(u16_sockDescriptor,
			(struct sockaddr *)&serverAddress,
			sizeof(serverAddress) );
	if (retVal < 0)
		return -1;

	/* Maintain a global variable for keeping the socket descriptor. */
	*p_sockDescriptor = u16_sockDescriptor;

	return 0;
}

static void closeConnectionWithMcpf(const McpS16 *const p_socketId)
{
	shutdown(*p_socketId, SHUT_RDWR);
	close(*p_socketId);
}


static McpU16 sendRequestToMcpf(const void *const p_inData,	McpU32 u32_msgSize)
{
	McpU16 retVal = 0;
	static McpS32 g_mcpfsocketId = 0;

	if (g_mcpfsocketId == 0) // create connection only first time
	{
		retVal = connectWithMcpfSocket(MCPF_SOCKET_PORT_ADS, (McpU8 *)MCPF_HOST_NAME_ADS, &g_mcpfsocketId);
		if (retVal != 0)
		{
			//MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG,(" Java_PostSLPMessageToQueue: Socket Connection FAILED !!! \n"));
			STATUSMSG("sendRequestToMcpf: Socket Connection FAILED !\n");
			return retVal;
		}
	}
	else
	{

		STATUSMSG("sendRequestToMcpf: g_mcpfsocketId = %u\n",g_mcpfsocketId);
	}
	/* Send payload if any. */
	if (u32_msgSize)
	{
		if ( write(g_mcpfsocketId, (void *)p_inData, u32_msgSize ) < 0)
		{
			//MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG,(" sendRequestToMcpf: Payload Sending FAILED !!! \n"));
			STATUSMSG("sendRequestToMcpf: Payload Sending FAILED !\n");
			/* If server is not running/reset, we have to connect first and then write data */
			retVal = connectWithMcpfSocket(MCPF_SOCKET_PORT_ADS, (McpU8 *)MCPF_HOST_NAME_ADS, &g_mcpfsocketId);
			if (retVal != 0)
			{
				//MCPF_REPORT_ERROR(hAds->hMcpf, ADS_MODULE_LOG,(" Java_PostSLPMessageToQueue: Socket Connection FAILED !!! \n"));
				STATUSMSG("sendRequestToMcpf: Socket Connection FAILED !\n");
				return retVal;
			}
			if ( write(g_mcpfsocketId, (void *)p_inData, u32_msgSize ) < 0)
			{
				STATUSMSG("sendRequestToMcpf: Payload Sending FAILED !\n");
				closeConnectionWithMcpf(&g_mcpfsocketId);
				g_mcpfsocketId = 0;
				return -1;
			}

		}
	}

	//closeConnectionWithMcpf(&g_mcpfsocketId);
	STATUSMSG("sendRequestToMcpf: bytes written = %u\n",u32_msgSize);
	STATUSMSG("sendRequestToMcpf: success = %u\n",retVal);

	STATUSMSG("sendRequestToMcpf: Exiting Successfully!!");

	return retVal;
}


/**
 * \fn     NAVL_Command
 * \brief  Send command to GPS task
 *
 *  Sends command to GPS task.
 *
 * \note
 * \param	hNavl_Client	- initialized client structure
 * \param	MsgClass	- Message class
 * \param	Opcode	- Opcode of message
 * \param	Param	- Message
 * \return 	response codes
 * \
 * \sa     	NAVL_Command
 **/
EMcpfRes NAVL_Command(handle_t hNavl_Client, McpU8 MsgClass, McpU8 Opcode, McpU16 Length, NavlMsg_t *Param)
{
	NAVL_Client_t *pNavl_Client = (NAVL_Client_t *)hNavl_Client;
	NAVL_t *pNavl = (NAVL_t *)pNavl_Client->hNavl;
	EMcpfRes ret_code = RES_OK;

	switch (MsgClass)
	{
		case NAVL_CLASS_INTERNAL:  /* future use */
			if(Param != NULL)
			{
				ret_code = navl_process_internal (hNavl_Client, Opcode, Length, Param);
			}
			else
			{
				ret_code = navl_process_internal (hNavl_Client, Opcode, Length, NULL);
			}
			break;

		case NAVL_CLASS_GPS:  /* All access to NAVC */
			if(Param != NULL)
			{
				ret_code = navl_process_gps (hNavl_Client, Opcode, Length, Param);
			}
			else
			{
				ret_code = navl_process_gps (hNavl_Client, Opcode, Length, NULL);
			}
			break;

		case NAVL_CLASS_RR:  /* RRC and RRLP */
			if(Param != NULL)
			{
				ret_code = navl_process_rr (hNavl_Client, Opcode, Length, Param);
				mcpf_mem_free_from_pool(pNavl->hMcpf, Param);
			}

			break;

		case NAVL_CLASS_SUPL: /* All access to SUPLC */
			if(Param != NULL)
			{
				ret_code = navl_process_supl (hNavl_Client, Opcode, Length, Param);
			}
			else
			{
				ret_code = navl_process_supl (hNavl_Client, Opcode, Length, NULL);
			}
			break;

		case NAVL_CLASS_SUPL_ADS:
			{

				STATUSMSG("NAVL_Command: NAVL_CLASS_SUPL_ADS\n");
				sendRequestToMcpf(Param->Payload,Length);
				mcpf_mem_free_from_pool(pNavl->hMcpf, Param);

			}
			break;
#ifdef WIFI_ENABLE
		case NAVL_CLASS_WPL:
			if(Param != NULL)
			{
				ret_code = navl_process_wpl (hNavl_Client, Opcode, Length, Param);
			}
			else
			{
				ret_code = navl_process_wpl (hNavl_Client, Opcode, Length, NULL);
			}
			break;
#endif
		default:
			ret_code = RES_UNKNOWN_OPCODE;
			break;

	}

	return ret_code;
}


/************************************************************************
 * Internal functions
 ************************************************************************/
/**
 * \fn     navl_process_rr
 * \brief  Process NAVL RRC/RRLP class message
 *
 * Process NAVL RRC and RRLP message types
 *
 **/
EMcpfRes navl_process_rr (handle_t hNavl_Client, McpU8 Opcode, McpU16 Length, NavlMsg_t *Param)
{
	NAVL_Client_t *pNavl_Client = (NAVL_Client_t *)hNavl_Client;
	NAVL_t *pNavl = (NAVL_t *) pNavl_Client->hNavl;
	McpU32	 uSystemTimeInMsec = mcpf_getCurrentTime_InMilliSec(pNavl->hMcpf);
	EMcpfRes eRes;
	EMcpfRes ret_code;


	switch (Opcode)
	{
		case NAVL_MC_SPECIFICATION_MT_RRLP:

			MCPF_REPORT_INFORMATION(pNavl->hMcpf, NAVL_MODULE_LOG,
					("navl_process_rr: received RRLP Msg from client, len=%u\n", Length));

			/* Send Ack Response for the RRLP msg */
			ret_code = mcpf_SendMsg (pNavl->hMcpf,
					pNavl_Client->TaskId,
					0,
					TASK_NAV_ID,
					0,
					NAVC_EVT_RRLP_RESP,
					0, Opcode, NULL);

			pNavl_Client->eProtocol = NAVL_PROT_RRLP;

			eRes = RRLP_processRxMsg (pNavl_Client->hRrlp,
					Param->Payload,
					Length,
					uSystemTimeInMsec);
			if (eRes != RES_OK)
			{
				MCPF_REPORT_ERROR(pNavl->hMcpf, NAVL_MODULE_LOG,
						("navl_process_rr: RRLP msg processing failed\n"));
				ret_code = RES_ERROR;
			}


			break;

		case NAVL_MC_SPECIFICATION_MT_RRC:

			MCPF_REPORT_INFORMATION(pNavl->hMcpf, NAVL_MODULE_LOG,
					("navl_process_rr: received RRC Msg from client, len=%u\n", Length));

			/* Send Ack Response for the RRC msg */
			ret_code = mcpf_SendMsg (pNavl->hMcpf,
					pNavl_Client->TaskId,
					0,
					TASK_NAV_ID,
					0,
					NAVC_EVT_RRC_RESP,
					0, 0, NULL);

			pNavl_Client->eProtocol = NAVL_PROT_RRC;

			eRes = RRC_processRxMsg (pNavl_Client->hRrc,
					Param->Payload,
					Length,
					uSystemTimeInMsec);
			if (eRes != RES_OK)
			{
				MCPF_REPORT_ERROR(pNavl->hMcpf, NAVL_MODULE_LOG,
						("navl_process_rr: RRC msg processing failed\n"));
				ret_code = RES_ERROR;
			}

			break;

		default:
			MCPF_REPORT_ERROR(pNavl->hMcpf, NAVL_MODULE_LOG,
					("navl_process_rr: Unsupported Opcode %d\n", Opcode));
			ret_code = RES_UNKNOWN_OPCODE;
			break;
	}

	return ret_code;
}


/**
 * \fn     navl_process_internal
 * \brief  Process internal messages
 *
 * Process internal  message types
 *
 */
EMcpfRes navl_process_internal (handle_t hNavl_Client, McpU8 Opcode, McpU16 Length, NavlMsg_t *Param)
{
	NAVL_Client_t *pNavl_Client = (NAVL_Client_t *)hNavl_Client;
	NAVL_t *pNavl = (NAVL_t *) pNavl_Client->hNavl;
	McpU32 uUserDefined = 0;

	return mcpf_SendMsg(pNavl->hMcpf,
			TASK_NAV_ID,
			NAVC_QUE_ACTION_ID,
			pNavl_Client->TaskId,
			0,
			Opcode,
			Length,
			uUserDefined,
			(void *)Param);
}

/**
 * \fn     navl_process_gps
 * \brief  Process NAVC class message
 *
 * Process NAVC  message types
 *
 */
EMcpfRes navl_process_gps (handle_t hNavl_Client, McpU8 Opcode, McpU16 Length, NavlMsg_t *Param)
{
	NAVL_Client_t *pNavl_Client = (NAVL_Client_t *)hNavl_Client;
	NAVL_t *pNavl = (NAVL_t *) pNavl_Client->hNavl;
	McpU32 uUserDefined;

	if(Opcode == NAVC_CMD_SAGPS_PROVIDER_REGISTER)
		uUserDefined = pNavl_Client->TaskId;
	else
		uUserDefined = 0;

	return mcpf_SendMsg(pNavl->hMcpf,
			TASK_NAV_ID,
			NAVC_QUE_CMD_ID,
			pNavl_Client->TaskId,
			0,
			Opcode,
			Length,
			uUserDefined,
			(void *)Param);
}

/**
 *  \fn     navl_process_supl
 *  \brief  Process SUPLC class message
 *
 *  Process SUPLC  message types
 *
 */
EMcpfRes navl_process_supl (handle_t hNavl_Client, McpU8 Opcode, McpU16 Length, NavlMsg_t *Param)
{
	NAVL_Client_t *pNavl_Client = (NAVL_Client_t *)hNavl_Client;
	NAVL_t *pNavl = (NAVL_t *) pNavl_Client->hNavl;
	McpU32 uUserDefined = 0;


	return mcpf_SendMsg(pNavl->hMcpf,
			TASK_SUPL_ID,
			SUPLC_CMD_QUEUE_ID,
			pNavl_Client->TaskId,
			0,
			Opcode,
			Length,
			uUserDefined,
			(void *)Param);

}


#ifdef WIFI_ENABLE

EMcpfRes navl_process_wpl(handle_t hNavl_Client, McpU8 Opcode, McpU16 Length, NavlMsg_t *Param)
{

	NAVL_Client_t *pNavl_Client = (NAVL_Client_t *)hNavl_Client;
	NAVL_t *pNavl = (NAVL_t *) pNavl_Client->hNavl;
	McpU32 uUserDefined;

	if(Opcode == NAVC_CMD_WPL_REGISTER){
		uUserDefined = pNavl_Client->TaskId;
	}
	else
		uUserDefined = 0;
	MCPF_REPORT_ERROR(pNavl->hMcpf, NAVL_MODULE_LOG,
			("navl_process_wpl: TaskId %d\n", pNavl_Client->TaskId));
	return mcpf_SendMsg(pNavl->hMcpf,
			TASK_NAV_ID,
			NAVC_QUE_CMD_ID,
			pNavl_Client->TaskId,
			0,
			Opcode,
			Length,
			uUserDefined,
			(void *)Param);


}
#endif //#ifdef WIFI_ENABLE


/**
 * \fn     navl_generate_msg
 * \brief  Generate message for transmitting to fCb of client
 *
 */

EMcpfRes navl_generate_msg (handle_t hNavl_Client, McpU32 uUnused1, McpU8 uUnused2, TmcpfMsg* mcpfMsg)
{
	NAVL_Client_t *pNavl_Client = (NAVL_Client_t*)hNavl_Client;
	NAVL_t *pNavl = (NAVL_t *)pNavl_Client->hNavl;
	McpU8 MsgClass = NAVL_CLASS_GPS;
	McpU8 Opcode = (McpU8) mcpfMsg->uOpcode;
	McpU16 Length = 0;
	McpU8 *Payload = mcpfMsg->pData;
	McpU16 PayloadLength= (McpU16) mcpfMsg->uLen;
	NavlMsg_t	*pMsg;
	EMcpfRes retVal = RES_OK;

	MCPF_UNUSED_PARAMETER(uUnused1);
	MCPF_UNUSED_PARAMETER(uUnused2);

	switch (Opcode)
	{
		case NAVC_EVT_POSITION_FIX_REPORT:
			{
				switch (pNavl_Client->eProtocol)
				{
					case NAVL_PROT_PRIVATE:
						{
							MsgClass = (McpU8)NAVL_CLASS_GPS;
							Length = PayloadLength;
						}
						break;

					case NAVL_PROT_RRLP:
						{
							TNAVC_evt* pEvt = (TNAVC_evt*)Payload;

							pMsg = (NavlMsg_t*)mcpf_mem_alloc_from_pool(pNavl->hMcpf, pNavl->hRREvtPool);
							MsgClass = (McpU8)NAVL_CLASS_RR;
							Opcode = (McpU8)NAVL_MC_SPECIFICATION_MT_RRLP;

							/* Encode RRLP Response message */
							Length = RRLP_encodeResponse (pNavl_Client->hRrlp,
									(T_GPSC_loc_fix_response *) &pEvt->tParams.tLocFixReport,
									NULL,
									C_RRLP_REP_POS,
									C_RRLP_LOC_REASON_GOOD,
									pMsg->Payload);

							Payload = (McpU8 *)pMsg;
							mcpf_mem_free_from_pool(pNavl->hMcpf, mcpfMsg->pData);
						}
						break;

					case NAVL_PROT_RRC:
						{
							TNAVC_evt *pEvt = (TNAVC_evt *) Payload;

							pMsg = (NavlMsg_t *)mcpf_mem_alloc_from_pool(pNavl->hMcpf, pNavl->hRREvtPool);
							MsgClass = (McpU8)NAVL_CLASS_RR;
							Opcode = (McpU8)NAVL_MC_SPECIFICATION_MT_RRC;

							/* Encode RRC Response message */
							Length = RRC_encodeResponse (pNavl_Client->hRrc,
									(T_GPSC_loc_fix_response *) &pEvt->tParams.tLocFixReport,
									NULL,
									RRC_REP_POS,
									C_RRC_LOC_REASON_GOOD,
									pMsg->Payload);

							Payload = (McpU8 *)pMsg;
							mcpf_mem_free_from_pool(pNavl->hMcpf, mcpfMsg->pData);
						}
						break;

					default:
						{
							MCPF_REPORT_ERROR(pNavl->hMcpf, NAVL_MODULE_LOG,
									("navl_generate_msg: bad protocol id=%u\n",pNavl_Client->eProtocol));
							mcpf_mem_free_from_pool(pNavl->hMcpf, mcpfMsg->pData);
							retVal = RES_ERROR;
						}
						break;
				}
			}
			break;

		case NAVC_EVT_ASSISTANCE_REQUEST:
			{
				switch (pNavl_Client->eProtocol)
				{
					case NAVL_PROT_PRIVATE:
						MsgClass = (McpU8)NAVL_CLASS_GPS;
						Length = PayloadLength;
						break;

					case NAVL_PROT_RRLP:
						{
							TNAVC_evt *pEvt = (TNAVC_evt *) Payload;
							TNAVCD_LocError	tLocError;

							pMsg = (NavlMsg_t *)mcpf_mem_alloc_from_pool(pNavl->hMcpf, pNavl->hRREvtPool);

							tLocError.uAssistBitMapMandatory = pEvt->tParams.tAssistReq.uAssistBitmapMandatory;
							tLocError.uAssistBitMapOptional  = pEvt->tParams.tAssistReq.uAssistBitmapOptional;
							tLocError.pNavModel = &pEvt->tParams.tAssistReq.tNavModelReqParams;

							MsgClass = (McpU8)NAVL_CLASS_RR;
							Opcode = (McpU8)NAVL_MC_SPECIFICATION_MT_RRLP;

							/* Encode RRLP Response message */
							Length = RRLP_encodeResponse (pNavl_Client->hRrlp,
									NULL,
									&tLocError,
									C_RRLP_REP_LOCERR,
									C_RRLP_LOC_REASON_GPS_ASS_MIS,
									pMsg->Payload);

							Payload = (McpU8*)pMsg;
							mcpf_mem_free_from_pool(pNavl->hMcpf, mcpfMsg->pData);
						}
						break;

					case NAVL_PROT_RRC:
						{
							TNAVC_evt *pEvt = (TNAVC_evt *) Payload;
							TNAVCD_LocError	tLocError;

							pMsg = (NavlMsg_t *)mcpf_mem_alloc_from_pool(pNavl->hMcpf, pNavl->hRREvtPool);

							tLocError.uAssistBitMapMandatory = pEvt->tParams.tAssistReq.uAssistBitmapMandatory;
							tLocError.uAssistBitMapOptional  = pEvt->tParams.tAssistReq.uAssistBitmapOptional;
							tLocError.pNavModel = &pEvt->tParams.tAssistReq.tNavModelReqParams;

							MsgClass = (McpU8)NAVL_CLASS_RR;
							Opcode = (McpU8)NAVL_MC_SPECIFICATION_MT_RRC;

							/* Encode RRC Response message */
							Length = RRC_encodeResponse (pNavl_Client->hRrc,
									NULL,
									&tLocError,
									RRC_REP_LOCERR,
									C_RRC_LOC_REASON_ASSIS_MIS,
									pMsg->Payload);

							Payload = (McpU8 *)pMsg;
							mcpf_mem_free_from_pool(pNavl->hMcpf, mcpfMsg->pData);
						}
						break;

					default:
						MCPF_REPORT_ERROR(pNavl->hMcpf, NAVL_MODULE_LOG,
								("navl_generate_msg: bad protocol id=%u\n",pNavl_Client->eProtocol));
						mcpf_mem_free_from_pool(pNavl->hMcpf, mcpfMsg->pData);
						retVal = RES_ERROR;
						break;
				}
			}
			break;

		case NAVC_EVT_RRLP_RESP:
			MsgClass = (McpU8)NAVL_CLASS_RR;
			Length = PayloadLength;

			break;

		case NAVC_EVT_RRC_RESP:
			MsgClass = (McpU8)NAVL_CLASS_RR;
			Length = PayloadLength;
			break;

		case NAVC_EVT_RRLP_ACK:
			{
				TNAVC_evt *pEvt = (TNAVC_evt *) Payload;
				pMsg = (NavlMsg_t *)mcpf_mem_alloc_from_pool(pNavl->hMcpf, pNavl->hRREvtPool);
				MsgClass = (McpU8)NAVL_CLASS_RR;
				Length = PayloadLength;
				Opcode = (McpU8)NAVL_MC_SPECIFICATION_MT_RRLP;
				mcpf_mem_copy (pNavl->hMcpf, pMsg->Payload, pEvt->tParams.tRrFrame.uRRpayload, Length);
				Payload = (McpU8*)pMsg;
				mcpf_mem_free_from_pool(pNavl->hMcpf, mcpfMsg->pData);
			}
			break;

		default:
			MsgClass = (McpU8)NAVL_CLASS_GPS;
			Length = PayloadLength;
			break;
	}


	if (retVal != RES_ERROR)
   {
		pNavl_Client->fCb(pNavl_Client->hCaller, MsgClass, Opcode, Length, Payload);
   }

	mcpf_mem_free_from_pool(pNavl->hMcpf, (void*)mcpfMsg);

	return retVal;
}
