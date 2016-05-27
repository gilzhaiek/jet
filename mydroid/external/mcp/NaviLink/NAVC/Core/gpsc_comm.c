/*
 * $Header: /FoxProjects/FoxSource/win32/LocationManager 1/10/04 7:53p Lleirer $
 ******************************************************************************
 *  Copyright (C) 1999 SnapTrack, Inc.

 *

 *                  SnapTrack, Inc.

 *                  4040 Moorpark Ave, Suite 250

 *                  San Jose, CA  95117

 *

 *     This program is confidential and a trade secret of SnapTrack, Inc. The

 * receipt or possession of this program does not convey any rights to

 * reproduce or disclose its contents or to manufacture, use or sell anything

 * that this program describes in whole or in part, without the express written

 * consent of SnapTrack, Inc.  The recipient and/or possessor of this program

 * shall not reproduce or adapt or disclose or use this program except as

 * expressly allowed by a written authorization from SnapTrack, Inc.

 *

 *

 ******************************************************************************/


 /*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*


   L O C A T I O N   S E R V I C E S   M A N A G E R   M O D U L E


  Copyright (c) 2002 by QUALCOMM INCORPORATED. All Rights Reserved.



 Export of this technology or software is regulated by the U.S. Government.

 Diversion contrary to U.S. law prohibited.

 *====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*/


/*********************************************************************************

                                   TI GPS Confidential

*********************************************************************************/

/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName			:	gpsc_comm.c
 *
 * Description     	:
 * This File contains the functions related to the communication abstraction layer
 * the communication is achieved via various calls to and fro the GPS Driver.
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#include "gpsc_types.h"
#include "gpsc_sap.h"
#include "gpsc_data.h"
#include "gpsc_drv_api.h"
#include "gpsc_comm.h"
#include "gpsc_mgp.h"
#include "gpsc_msg.h"
#include "gpsc_state.h"
#include "gpsc_timer_api.h"
#include "gpsc_app_api.h"
#include <stdio.h>

#include <utils/Log.h>
#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG "NAVD.GPSC_COMM"    // our identification for logcat (adb logcat NAVD.GPSC_COMM:V *:S)


/**** THIS PROCESSES MESSAGE READ OFF /dev/tigps. IT IS STILL IN CONTEXT OF SAME THREAD that blocks on Kernel driver
      mcp_hal_st::stThreadFun *****/
T_GPSC_result gpsc_comm_rx_bytes
(
    gpsc_ctrl_type*    p_zGPSCControl,
    U8*                p_uDataBuffer,
    U16                w_NumBytes
)
{
	U32 w_count        = 0;
	U8 u_char          = 0;
	U8 u_BuilderResult = 0;

	gpsc_comm_rx_buff_type* p_Buf         = p_zGPSCControl->p_zGPSCCommRxBuff;
	gpsc_comm_tx_buff_type* p_Ai2TxBuf    = p_zGPSCControl->p_zGPSCCommTxBuff;
	gpsc_state_type*        p_zGPSCState  = p_zGPSCControl->p_zGPSCState;
	Ai2Rx*                  p_RxDesc      = &p_Buf->z_RxDesc;
	Ai2Field*               p_Ai2RxField  = &p_Buf->z_Ai2RxField;

	if (p_zGPSCState->u_FowardMode == TRUE)
	{ 
		gpsc_app_fwd_rx_res(p_uDataBuffer, w_NumBytes);
		return TRUE;
	}

	if (w_NumBytes > C_DL_RX_BUF_LEN)
		return GPSC_FAIL;

	if (p_zGPSCState->u_IgnoreFirstByte == TRUE &&
		p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_INIT)
	{
		p_zGPSCState->u_IgnoreFirstByte = FALSE;
		return GPSC_SUCCESS;
	}

	if (p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_INIT)
	{
		ALOGE("+++ %s: Error : GPS is expected to be off +++\n", __FUNCTION__);
		return GPSC_FAIL;
	}

	for (w_count = 0; w_count < w_NumBytes; w_count++)
	{
		u_char = p_uDataBuffer[w_count];

		/* Build up the AI2 message */
		if ( (u_BuilderResult = Ai2RxBuild( p_RxDesc, (U8) u_char )) == C_AI2BUILDER_MSG_READY)
		{
			/* A complete AI2 message is available in the buffer. */
			/* Get the true fields from the message. */
         int i = 0;
			while (Ai2RxFieldGet( p_RxDesc, p_Ai2RxField ))
			{
            i++;
				gpsc_mgp_process_msg( p_zGPSCControl, p_Ai2RxField );
			}

         if (i > 1) ALOGI("+++ %s: Processed [%d] GPS Messages from single IO buffer +++\n", __FUNCTION__, i);
		}
		else
		{
	 		if (u_BuilderResult == C_AI2BUILDER_ACK_DETECTED)
			{
				// Detected an Ack
				if (p_Ai2TxBuf->u_AI2AckPending == FALSE)
				{
					ALOGI("+++ %s: Warning: Unexpected Acks recieved +++\n", __FUNCTION__);
				}

				p_Ai2TxBuf->u_AI2AckPending = FALSE;
				gpsc_timer_stop(p_zGPSCControl, C_TIMER_AI2_ACK_PENDING);
					
            if (p_Ai2TxBuf->u_DataPending == TRUE)
				{
					p_Ai2TxBuf->u_DataPending = FALSE;
					gpsc_comm_transmit(p_zGPSCControl, p_Ai2TxBuf->u_DataPendingAck);
				}
				gpsc_state_process_ack(p_zGPSCControl);
			}
		}
	}

	return GPSC_SUCCESS;
}

void gpsc_comm_init (gpsc_ctrl_type*    p_zGPSCControl)
{

	gpsc_comm_rx_buff_type* p_RxBuf = p_zGPSCControl->p_zGPSCCommRxBuff;
	gpsc_comm_tx_buff_type* p_TxBuf = p_zGPSCControl->p_zGPSCCommTxBuff;

	Ai2RxInstall ( &p_RxBuf->z_RxDesc, &p_RxBuf->u_Buff[0], C_DL_RX_BUF_LEN );
	Ai2RxInit( &p_RxBuf->z_RxDesc );

	p_TxBuf->u_FirstPacket    = 1;
	p_TxBuf->u_AI2AckPending  = FALSE;
	p_TxBuf->u_DataPending    = FALSE;
	p_TxBuf->u_AddAckShared   = FALSE;

	p_TxBuf->u_DataPendingAck = C_AI2_NO_ACK;
	Ai2TxInstall (&p_TxBuf->z_TxDesc, &p_TxBuf->u_Buff[0], C_DL_TX_BUF_LEN);

}

void gpsc_comm_write(gpsc_ctrl_type* p_zGPSCControl, gpsc_ai2_put_msg_desc_type* p_MsgDesc)
{
	Ai2Field z_Ai2Field;

	gpsc_comm_tx_buff_type* p_Ai2TxBuf = p_zGPSCControl->p_zGPSCCommTxBuff;
	Ai2Tx* p_Tx = &p_Ai2TxBuf->z_TxDesc;

	if (p_Ai2TxBuf->u_FirstPacket)
	{
		Ai2TxInit( p_Tx, C_AI2_NO_ACK );
		p_Ai2TxBuf->u_FirstPacket = 0;
	}

	// Add the next packet to the buffer
	z_Ai2Field.u_Id = p_MsgDesc->u_PacketId;
	z_Ai2Field.w_Length = p_MsgDesc->w_PacketLen;
	z_Ai2Field.p_B = p_MsgDesc->p_Buf;

	Ai2TxFieldAdd (p_Tx, &z_Ai2Field);
}

U8 gpsc_comm_check_bufferspace(gpsc_ctrl_type* p_zGPSCControl, U16 w_MoreDataLen)
{
  gpsc_comm_tx_buff_type * p_Ai2TxBuf = p_zGPSCControl->p_zGPSCCommTxBuff;
  Ai2Tx *p_Tx = &p_Ai2TxBuf->z_TxDesc;

  if (p_Ai2TxBuf->u_FirstPacket == 1)
	  return TRUE;

  if (p_Tx->w_ByteCount < C_MAX_DRIVER_BUF - w_MoreDataLen)
	  return TRUE;

  return FALSE;
}

U8 gpsc_comm_transmit(gpsc_ctrl_type* p_zGPSCControl, U8 u_Ack)
{
	S16                     u_BytestoDownload = 0;
	gpsc_comm_tx_buff_type* p_Ai2TxBuf = p_zGPSCControl->p_zGPSCCommTxBuff;
	gpsc_cfg_type*          p_zGPSCConfig = p_zGPSCControl->p_zGPSCConfig;
	Ai2Tx*                  p_Tx = &p_Ai2TxBuf->z_TxDesc;
	gpsc_state_type*        p_zGPSCState = p_zGPSCControl->p_zGPSCState;

	if (p_zGPSCState->u_FowardMode == TRUE)
	{
		ALOGI("+++ %s Warning: In Fowarding mode, cannot tx now +++\n", __FUNCTION__);
		return TRUE;
	}

	MCPF_UNUSED_PARAMETER(p_zGPSCConfig);
	if (p_Ai2TxBuf->u_AI2AckPending == TRUE)
	{
		p_Ai2TxBuf->u_DataPending = TRUE;
		p_Ai2TxBuf->u_DataPendingAck = (U8)((p_Ai2TxBuf->u_DataPendingAck == C_AI2_ACK_REQ)?
										C_AI2_ACK_REQ:u_Ack);
		return TRUE;
	}

	if (p_Ai2TxBuf->u_FirstPacket == 1)
	{
		// Check the case when we transmit without populating the buffer
		ERRORMSG("Warning: transmitting on an empty buffer");
		return FALSE;
	}

	if (!(p_Ai2TxBuf->u_AddAckShared))
	{
		if (u_Ack  == C_AI2_ACK_REQ)
		{
			/* Set AI2 Ack Pending to true */
			p_Ai2TxBuf->u_AI2AckPending = TRUE;

			/* Only if the config file parameter is set to true shall we request an ack
			  if the parameter is set to false we shall simulate an ack when we receive
			  a tx response, this is to speed up the communication in a reliable connection*/
			if (p_zGPSCControl->p_zGPSCConfig->ai2_ack_required == GPSC_TRUE)
				Ai2SetAck(p_Tx, C_AI2_ACK_REQ);
			else
				Ai2SetAck(p_Tx, C_AI2_NO_ACK);
		}
		else
		{
			p_Ai2TxBuf->u_AI2AckPending = FALSE;
			Ai2SetAck(p_Tx, C_AI2_NO_ACK);
		}

	}

	if (p_Tx->w_ByteCount <= MAX_CHUNK_DATA)
	{
		// Finish up the buffer 
		Ai2TxEnd( p_Tx );
	}

		u_BytestoDownload = p_Tx->w_ByteCount;

		/* Send the message */
		if (p_Tx->w_ByteCount > MAX_CHUNK_DATA)
		{
				p_Tx->w_ByteCount = MAX_CHUNK_DATA;

				if (gpsc_transmit_data(&p_Tx->p_Buff[0], p_Tx->w_ByteCount) != GPSC_SUCCESS)
				{
					/* When working with Shared Transport the 'u_TxResPending' flag
						is not needed, because flow control is done in the NAVC Adapter */
					gpsc_os_fatal_error_ind(GPSC_TX_INITIATE_FAIL);
					GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
				}
				else
				{
					p_Tx->w_ByteCount = (U16)(u_BytestoDownload - MAX_CHUNK_DATA);
					p_Tx->p_Buff =  ((p_Tx->p_Buff) + MAX_CHUNK_DATA);
					p_Ai2TxBuf->u_AddAckShared = TRUE;
					p_Ai2TxBuf->u_DataPending = TRUE;
					p_Ai2TxBuf->u_DataPendingAck = C_AI2_NO_ACK;
				}
				p_Ai2TxBuf->u_FirstPacket = 0;
				return TRUE;
		}

		if (gpsc_transmit_data(&p_Tx->p_Buff[0], p_Tx->w_ByteCount) != GPSC_SUCCESS)
		{
			FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
			GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
		}
		else
		{
			p_Tx->p_Buff = &p_zGPSCControl->p_zGPSCCommTxBuff->u_Buff[0];
			p_Ai2TxBuf->u_AddAckShared = FALSE;
			p_Ai2TxBuf->u_DataPending = FALSE;
			p_Ai2TxBuf->u_DataPendingAck = C_AI2_NO_ACK;
		}

		p_Ai2TxBuf->u_FirstPacket = 1;
		return TRUE;
}


void gpsc_comm_tx_success(gpsc_ctrl_type* p_zGPSCControl)
{
	gpsc_comm_tx_buff_type * p_Ai2TxBuf  = p_zGPSCControl->p_zGPSCCommTxBuff;
	gpsc_cfg_type *p_zGPSCConfig = p_zGPSCControl->p_zGPSCConfig;

	if (p_Ai2TxBuf->u_AI2AckPending == TRUE)
	{
		// Tx completed
		if (p_zGPSCControl->p_zGPSCConfig->ai2_ack_required == GPSC_TRUE)
		{
			/*If we had requested for  an ack start timer and wait for AI2 ack*/
			gpsc_timer_start(p_zGPSCControl, C_TIMER_AI2_ACK_PENDING,
							p_zGPSCConfig->ai2_comm_timeout);
		}
		else
		{
           /*Simulate an AI2 ack*/
			STATUSMSG("Status : Simulating an AI2 ack");
			p_Ai2TxBuf->u_AI2AckPending =FALSE;
			if(p_Ai2TxBuf->u_DataPending == TRUE)
			{
				p_Ai2TxBuf->u_DataPending = FALSE;
				gpsc_comm_transmit(p_zGPSCControl, p_Ai2TxBuf->u_DataPendingAck);
			}
			gpsc_state_process_ack(p_zGPSCControl);
		}
	}
	else if(p_Ai2TxBuf->u_DataPending == TRUE)
	{
		p_Ai2TxBuf->u_DataPending = FALSE;
		gpsc_comm_transmit(p_zGPSCControl, p_Ai2TxBuf->u_DataPendingAck);
	}
}


