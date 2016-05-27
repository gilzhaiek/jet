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


/** \file   nt_adapt.c 
 *  \brief  NAVC Transport Adapter module implementation                                  
 * 
 *  NAVC Transport Adapter provides:
 * - Registration of the NAVC channel to the shared transport layer
 * - TX Flow Control
 * - Callbacks for transport layer to indicate RX packets.
 * 
 * The NAVC Transport Adapter is OS and Bus independent module.  
 * 
 *
  */


/************************************************************************
 * Includes
 ************************************************************************/
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>
#include <errno.h>

#include "nt_adapt.h"
#include "mcpf_services.h"
#include "mcpf_mem.h"
#include "mcpf_report.h"
#include "mcp_defs.h"
#include "mcp_hal_defs.h"
#include "mcpf_msg.h"
#include "gpsc_data.h"
#include "navc_defs.h"
#include "navc_api.h"
#include "mcp_hal_os.h"
#include "mcp_hal_log.h"

#include <utils/Log.h>
#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG "NAVD.NT_ADAPT"    // our identification for logcat (adb logcat NAVD.NT_ADAPT:V *:S)

#ifdef UNIFIED_TOOLKIT_COMM
#include "clientsocket_comm.h"

extern McpS32 clientsockfd;
#endif

#define MAX_BAUD_RATES		12

/* For Ai2 Parsing	*/
#define AI2_PKT_SYNCH_BYTE   0x10
#define AI2_PKT_TERM_BYTE    0x03

#ifndef MCP_STK_ENABLE /* Replaced for ST in Kernel */

   #ifdef CLIENT_CUSTOM
      const char* NTA_DEVICE_NAME =                                   "/dev/ttyS0";    /* for custom client */
   #else
      const char* NTA_DEVICE_NAME =                                   "/dev/ttyS1";	   /* for Zoom 2 */
   #endif

#else
   const char* NTA_DEVICE_NAME    =                                   "/dev/tigps";    /* Shared Transport */
#endif


#define NTA_SEND_AND_MEM_RETRY_CNT		5

typedef struct nta_params_t
{
	handle_t		hMcpf;
	handle_t    hHalSt;
	handle_t		hRxPool;
	handle_t		hRxShortPool;
	McpU8*      pRxBuf;
	McpU8*      pInData;
	McpU16		uInDataLen;
	ENtaRxState	eRxState;
	McpU16		portNum;
} nta_params_t;

nta_params_t nta_params;
static McpU32 		u_BaudRates[MAX_BAUD_RATES];

static EMcpfRes sendMsgToNavc();
static EMcpfRes prepareNextPacket();
static EMcpfRes drvRxInd ();


extern int  open_recon_port (int type);
extern void close_recon_port (int type);

/************************************************************************
 * Functions
 ************************************************************************/

/** 
 * \fn     NTA_Init 
 * \brief  Init the NAVC Transport Adapter module
 * 
 * Allocates internal data and registers channels to the transport layer.
 * 
 * \note
 * \param	hMCpf - handle to MCPF
 * \param	uPortNum - Port Number
 * \return 	Returns the status of operation: OK or Error
 * \sa     	NTA_Init
 */ 
EMcpfRes NTA_Init (handle_t hMcpf, McpInt uPortNum)
{
	McpU8	i = 0;
	nta_params.hMcpf = hMcpf;
	
	nta_params.hRxPool = mcpf_memory_pool_create(hMcpf, NTA_RX_BUF_SIZE, 
													NTA_RX_POOL_SIZE);
	if (!nta_params.hRxPool)
	{
		MCPF_REPORT_ERROR(hMcpf, NAVC_MODULE_LOG, ("NTA_Init: Create Rx Pool falied\n"));
		return RES_ERROR;
	}
   ALOGI("+++ %s: Rx Pool created. Pool Size: [%d] +++\n", __FUNCTION__, NTA_RX_POOL_SIZE);

	nta_params.hRxShortPool = mcpf_memory_pool_create(hMcpf, NTA_RX_SHORT_BUF_SIZE,
													NTA_RX_SHORT_POOL_SIZE);
	if (!nta_params.hRxShortPool)
	{
		MCPF_REPORT_ERROR(hMcpf, NAVC_MODULE_LOG, ("NTA_Init: Create SHORT RX Pool failed\n"));

      mcpf_memory_pool_destroy(hMcpf, nta_params.hRxPool);
		return RES_ERROR;
	}

	ALOGI("+++ %s: Short Rx Pool created. Pool Size: [%d] +++\n", __FUNCTION__, NTA_RX_SHORT_POOL_SIZE);

	nta_params.hHalSt = HAL_ST_Create(hMcpf, "GPS TRANSPORT");
	if (!nta_params.hHalSt)
	{
		MCPF_REPORT_ERROR(hMcpf, NAVC_MODULE_LOG, ("NTA_Init: Create HAL ST failed\n"));

		mcpf_memory_pool_destroy(hMcpf, nta_params.hRxPool);
      mcpf_memory_pool_destroy(hMcpf, nta_params.hRxShortPool);

		return RES_ERROR;
	}

	/* Initialize parameters */
	nta_params.uInDataLen = 0;
	nta_params.eRxState = NTA_STATE_RX_GET_SYNC;
	nta_params.pInData = NULL;
	nta_params.pRxBuf = NULL;
	nta_params.portNum = (McpU16)uPortNum;
	
	// Set Baud Rate Array
	for (i = 0; i < MAX_BAUD_RATES; i++)
		u_BaudRates[i] = 0xFF;

	u_BaudRates[GPSC_BAUD_1200] = HAL_ST_SPEED_1200;
	u_BaudRates[GPSC_BAUD_2400] = HAL_ST_SPEED_2400;
	u_BaudRates[GPSC_BAUD_4800] = HAL_ST_SPEED_4800; 
	u_BaudRates[GPSC_BAUD_9600] = HAL_ST_SPEED_9600; 
	u_BaudRates[GPSC_BAUD_14400] = HAL_ST_SPEED_14400; 
	u_BaudRates[GPSC_BAUD_19600] = HAL_ST_SPEED_19200;
	u_BaudRates[GPSC_BAUD_38400] = HAL_ST_SPEED_38400; 
	u_BaudRates[GPSC_BAUD_57600] = HAL_ST_SPEED_57600; 
	u_BaudRates[GPSC_BAUD_115200] = HAL_ST_SPEED_115200; 
	u_BaudRates[GPSC_BAUD_230400] = HAL_ST_SPEED_230400;

  if (open_recon_port(RECON_PIPE_TYPE_DATA) >= 0 )
      ALOGI("+++ %s: Recon GPS Data Port [%s] open OK! +++\n", __FUNCTION__, RECON_DATA_PIPE);

  if (open_recon_port(RECON_PIPE_TYPE_CONTROL) >= 0 )
      ALOGI("+++ %s: Recon GPS Control Port [%s] open OK! +++\n", __FUNCTION__, RECON_CONTROL_PIPE);
	return RES_OK;
}

/** 
 * \fn     NTA_Open
 * \brief  Opens the 'tigps' char device
 * 
 * Opens the 'tigps' char device.
 * 
 * \note
 * \param	hMCpf - handle to MCPF
 * \return 	Returns the status of operation: OK or Error
 * \sa     	NTA_Open
 */ 
EMcpfRes NTA_Open ()
{
	EMcpfRes ret = 0;
	THalSt_PortCfg tConf;
	
	nta_params.pRxBuf = mcpf_mem_alloc_from_pool(nta_params.hMcpf, nta_params.hRxPool);
        MCPF_Assert(nta_params.pRxBuf);

	if (!nta_params.pRxBuf)
	{
		MCPF_REPORT_ERROR(nta_params.hMcpf, NAVC_MODULE_LOG, 
								("NTA_Open: Alloc buf from Rx Pool falied\n"));	
	}

	nta_params.pInData = nta_params.pRxBuf;

	mcpf_mem_zero(nta_params.hMcpf, &tConf, sizeof(THalSt_PortCfg));
	tConf.portNum = nta_params.portNum;
	 
	ret = HAL_ST_Init(nta_params.hHalSt, NTA_EvtHndl, NULL, &tConf, (const McpS8*)NTA_DEVICE_NAME, 
						nta_params.pRxBuf, 1, MCP_FALSE);

	if (ret == RES_ERROR)
	{
		MCPF_REPORT_ERROR(nta_params.hMcpf, NAVC_MODULE_LOG, 
								("NTA_Open: HAL_ST_Init falied\n"));	

		mcpf_mem_free_from_pool(nta_params.hMcpf, nta_params.pRxBuf);
		nta_params.pInData = NULL;
		nta_params.pRxBuf = NULL;

      return ret;
	}

   ALOGI("+++ %s: Connection with [%s] TI Shared Transport Device established +++\n", __FUNCTION__, NTA_DEVICE_NAME);
  
	return ret;
}

/** 
 * \fn     NTA_Close 
 * \brief  Closes the 'tigps' char device
 * 
 * Closes the 'tigps' char device
 * 
 * \note
 * \return 	Returns the status of operation: OK or Error
 * \sa     	NTA_Close
 */ 
EMcpfRes NTA_Close ()
{
	HAL_ST_Deinit(nta_params.hHalSt);

	if (nta_params.pRxBuf)
		mcpf_mem_free_from_pool(nta_params.hMcpf, nta_params.pRxBuf);

	nta_params.uInDataLen = 0;
	nta_params.eRxState = NTA_STATE_RX_GET_SYNC;
	nta_params.pInData = NULL;
	nta_params.pRxBuf = NULL;

	return RES_OK;
}

/** 
 * \fn     NTA_Destroy 
 * \brief  Destroy the NAVC Transport Adapter module
 * 
 * Frees internal data.
 * 
 * \note
 * \return 	Returns the status of operation: OK or Error
 * \sa     	NTA_Destroy
 */ 
EMcpfRes NTA_Destroy ()
{
	HAL_ST_Destroy(nta_params.hHalSt);

	mcpf_memory_pool_destroy(nta_params.hMcpf, nta_params.hRxShortPool);
	mcpf_memory_pool_destroy(nta_params.hMcpf, nta_params.hRxPool);

   // close RECON ports
   close_recon_port (RECON_PIPE_TYPE_DATA);
   close_recon_port (RECON_PIPE_TYPE_CONTROL);

	return RES_OK;
}

/** 
 * \fn     NTA_SendData
 * \brief  NAVC Transport Adapter transmit data function
 * 
 *  Transmit packet to char device
 * 
 * \note
 * \param	pBuf 	- pointer to the data to transmit
 * \param	len	 	- data length
 * \return 	Returns the status of operation: OK or Error
 * \sa      NTA_SendData
 */ 
EMcpfRes NTA_SendData (McpU8* pBuf, McpU16 len)
{
	McpU16 sentLen = 0;
	EMcpfRes ret = HAL_ST_Write(nta_params.hHalSt, pBuf, len, &sentLen);
	
   if (sentLen < len)
	  MCPF_REPORT_ERROR(nta_params.hMcpf, NAVC_MODULE_LOG, ("Less Bytes Tx'd: len: %d, sent: %d", len, sentLen));

	if (ret == RES_ERROR)
		return ret;
	else
		return RES_OK;
}

/** 
 * \fn     NTA_EvtHndl
 * \brief  NAVC Transport Adapter Event indication function 
 * 
 *  Indication that a packet is received from the char device or a Tx Complete
 * ***
 * *** THIS IS EXECUTING IN CONTEXT OF HAL_ST Thread that blocks on /dev/tigps, then calls back once data 
 * *** has been retrieved from Kernel
 * ***
 * 
 * \note
 * \param	hHandleCb 	- handler to the calling module handle (to fit RX Indication CB type - not used)
 * \param	eEvent 		-Indicates the event received (Rx Ind & Tx Cmplt are relevent here)
 * \return 	None
 * \sa     	NTA_EvtHndl
 */ 
void NTA_EvtHndl (handle_t hHandleCb, EHalStEvent eEvent)
{
	EMcpfRes ret;

	MCPF_UNUSED_PARAMETER(hHandleCb);
	switch (eEvent)
	{
		case HalStEvent_WriteComplInd:

			/* Send Tc Complete Ind to NAVC */
			ret = mcpf_SendMsg (nta_params.hMcpf, 
						 		TASK_NAV_ID,  
						 		NAVC_QUE_INTERNAL_EVT_ID,
						 		TASK_NAV_ID,
						 		NAVC_QUE_INTERNAL_EVT_ID,
						 		NAVCD_EVT_TX_COMPL_IND,
						 		0,               
						 		0, 			  /* user defined */
						 		NULL);

			if (ret != RES_OK)
			{
				MCPF_REPORT_ERROR(nta_params.hMcpf, NAVC_MODULE_LOG, 
					("NTA_EvtHndl: TxCmplt - mcpf_SendMsg failed\n"));
			}
		break;

		case HalStEvent_ReadReadylInd:
			/* Read data from the HAL_ST */
			ret = drvRxInd();

			if (ret != RES_OK)
			{
				MCPF_REPORT_ERROR(nta_params.hMcpf, NAVC_MODULE_LOG, 
					("NTA_EvtHndl: RxInd - drvRxInd failed\n"));
			}
		break;

		default:
			break;
	}
}

/** 
 * \fn     NTA_SetBaudRate
 * \brief  NAVC Transport Adapter Set Baud Rate function
 * 
 *  Set the UART Baud Rate
 * 
 * \note
 * \param	speed 	- the new baud rate speed
 * \return  None
 * \sa     	NTA_SetBaudRate
 */ 
void NTA_SetBaudRate (McpU16 speed)
{
	THalSt_PortCfg 	conf;

	conf.uBaudRate = u_BaudRates[speed];
	
	HAL_ST_Set(nta_params.hHalSt, &conf);
}


/***************************************************************************
*							Internal Functions							   *
***************************************************************************/

static EMcpfRes sendMsgToNavc()
{
   int tries = 0;

   while (1)
   {
       EMcpfRes ret = mcpf_SendMsg (nta_params.hMcpf, 
						 TASK_NAV_ID,  
						 NAVC_QUE_INTERNAL_EVT_ID,
						 TASK_NAV_ID,
						 NAVC_QUE_INTERNAL_EVT_ID,
						 NAVCD_EVT_RECV_RX_IND,
						 nta_params.uInDataLen,               
						 0, 			  /* user defined */
						 nta_params.pRxBuf);

        tries++; 
        if (ret == RES_OK) break;

        if (tries >= 10)
        {
    		    ALOGE("+++ %s: mcpf_SendMsg Failure. Freeing buffer +++\n", __FUNCTION__);
		       mcpf_mem_free_from_pool(nta_params.hMcpf, nta_params.pRxBuf);
             
             return ret;
        }
        MCP_HAL_OS_Sleep(200);
   }

   if (tries > 1) ALOGI("+++ %s: Message to Navc sent, but took [%d] retries +++\n", __FUNCTION__, tries);
	return RES_OK;
}

static EMcpfRes prepareNextPacket()
{
	// Init Params for next read 
	nta_params.uInDataLen = 0;
	nta_params.eRxState = NTA_STATE_RX_GET_SYNC;

	nta_params.pRxBuf = mcpf_mem_alloc_from_pool(nta_params.hMcpf, nta_params.hRxPool);
	if (!nta_params.pRxBuf)
	{
      ALOGE("+++ %s: Alloc buf from Rx Pool failed +++\n", __FUNCTION__);

		nta_params.pInData = NULL;
		nta_params.pRxBuf = NULL;

		return RES_ERROR;
	}
	
	nta_params.pInData = nta_params.pRxBuf;
	return RES_OK;
}

/** 
 * \fn     drvRxInd
 * \brief  Data receive indication
 * 
 * Called by event handler upon Rx event, indicating received data is ready.
 * 
 * \note   
 * \param  None
 * \return   Returns the status of operation: OK or Error
 * \sa     drvRxInd
 */ 
static EMcpfRes drvRxInd ()
{
    McpU8		byte = 0;              // a IO byte to be parsed
    McpU16	   uReadLen = 0; 
    McpU16	   pktLen = 0;            // pktLen received
    McpBool	   done = MCP_FALSE;      // flag to indicate a complete packet has been obtained
    EMcpfRes	eReadStatus = RES_OK;
	 McpU32	   drvRxIndCnt = 0;       // cmcm

	 /* If RxBuf/pInData is not available return error  */
    if (!nta_params.pRxBuf || !nta_params.pInData)
    {
        MCPF_REPORT_ERROR (nta_params.hMcpf, NAVC_MODULE_LOG,
                          ("drvRxInd: Rx buffer is NULL!\n"));
		  return RES_ERROR;
    }

    do
    {
		 /* Get the byte read */
		 byte = *nta_params.pInData;
		 drvRxIndCnt++;

		 /* get the number of bytes read */
       HAL_ST_ReadResult(nta_params.hHalSt, &pktLen);

	    /* Update read len */
	    if (pktLen || uReadLen )
	    {
		   nta_params.uInDataLen++;	// increment the length
		   nta_params.pInData++;      // byte is data byte, store it 

		   switch (nta_params.eRxState)
		   {
			   case NTA_STATE_RX_GET_SYNC:
			   {
               if (byte == AI2_PKT_SYNCH_BYTE)
					   nta_params.eRxState  = NTA_STATE_RX_GET_DATA_DLE;
				   else
				   {
					   // Reset the pointer and datalength and wait for a proper start (sync byte) of AI2 message
					   nta_params.pInData--;
					   nta_params.uInDataLen--;
				   }
            }
		  	   break;	
		
			   case NTA_STATE_RX_GET_DATA:
			   {
               if (byte == AI2_PKT_SYNCH_BYTE)
					   nta_params.eRxState  = NTA_STATE_RX_GET_DATA_DLE;
            }
		  	   break;

            case NTA_STATE_RX_GET_DATA_DLE:
            {
           		if (byte == AI2_PKT_TERM_BYTE)
               {
                    #ifdef UNIFIED_TOOLKIT_COMM	
					        sendto_clientsocket(clientsockfd, nta_params.pRxBuf, nta_params.uInDataLen, 1);
                    #endif

                    done = MCP_TRUE;
					     uReadLen = 0;

								/*
								 * If datalength is small, use a buffer from the short buf pool
								 * and free up the buffer from the long buf pool
								 */
	                      if (nta_params.uInDataLen < NTA_RX_SHORT_BUF_SIZE)
	                      {
									McpU8* pShortBufStore = mcpf_mem_alloc_from_pool(
																nta_params.hMcpf,
																nta_params.hRxShortPool);
									if (!pShortBufStore)
									{
										MCPF_REPORT_ERROR(nta_params.hMcpf,
														  NAVC_MODULE_LOG,
										  				  ("drvRxInd: Alloc buf from SHORT Rx Pool failed. \
										  				    Continue with previous allocation\n"));
									}
									else
									{
										mcpf_mem_copy(nta_params.hMcpf,
													  pShortBufStore,
													  nta_params.pRxBuf,
													  nta_params.uInDataLen);

										/* Free the previous allocation */
										mcpf_mem_free_from_pool(nta_params.hMcpf, nta_params.pRxBuf);

										/* Update the data pointer */
										nta_params.pRxBuf = pShortBufStore;
									}
								}

                        if (sendMsgToNavc () != RES_OK)
                        {
                            ALOGE("+++ %s: AI2 Packet could not be processed - message delivery failed +++\n", __FUNCTION__);
                        }

                        if (prepareNextPacket() != RES_OK)
                        {
                            ALOGE("+++ %s: Failed to allocate buffer for next Rx +++\n", __FUNCTION__);
                        }

					         resetDataPtr(nta_params.hHalSt, nta_params.pInData);

                }
				else
				{
				 	nta_params.eRxState = NTA_STATE_RX_GET_DATA;
				}

            }
			break;

           	default:
            {
				   MCPF_REPORT_ERROR(nta_params.hMcpf,NAVC_MODULE_LOG,("ERROR: INVALID STATE: %d", nta_params.eRxState));
			   }
			break;

		}/* end of switch (nta_params.eRxState) */

		/* cmcm read upto POOL_RX_MAX_BUF_SIZE */
		if ( (nta_params.uInDataLen >= NTA_RX_BUF_SIZE) && (!done) )
		{
			ENtaRxState	eRxState;
			uReadLen = 0;

         if (sendMsgToNavc() != RES_OK)
         {
             ALOGE("+++ %s: AI2 Packet could not be processed - message delivery failed +++\n", __FUNCTION__);
         }
		
			/* Save current Rx State, since 'prepareNextPacketRx' changes it */
			eRxState = nta_params.eRxState;
		
         if (prepareNextPacket() != RES_OK)
         {
             ALOGE("+++ %s: Failed to allocate buffer for next Rx +++\n", __FUNCTION__);
         }
							
			resetDataPtr(nta_params.hHalSt, nta_params.pInData);
			
			/* Set the Rx state, to be the state that was saved */
			nta_params.eRxState = eRxState;
		
		}
		else if (done)
			done = MCP_FALSE;


		if ( nta_params.pInData >= ( (McpU8*)nta_params.pRxBuf + NTA_RX_BUF_SIZE))
		{
			MCPF_REPORT_ERROR(nta_params.hMcpf, NAVC_MODULE_LOG,("drvRxInd: FEEL THERE IS MEMORY OVERWRITE"));
		}

	} //if ( pktLen || uReadLen )

	/* call read to get next data byte */
	eReadStatus = HAL_ST_Read (nta_params.hHalSt, nta_params.pInData, 1, &uReadLen);
		
	if (eReadStatus == RES_ERROR)
	{
		/* Transport Error */
		MCPF_REPORT_ERROR (nta_params.hMcpf, NAVC_MODULE_LOG, 
                              ("drvRxInd: Rx failed, status=%u len=%u!\n", eReadStatus, uReadLen));

		return RES_ERROR;
	}

    	/*  eReadStatus is added to verify if a deinit is initialized */
    } while ( uReadLen ); /* end of while */
    
	return RES_OK;
	
} /*End drvRxInd*/
