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
 * FileName			:	gpsc_comm.h
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
#ifndef _GPSC_COMM_H
#define _GPSC_COMM_H


#include "gpsc_sap.h"


void gpsc_comm_init(gpsc_ctrl_type *  p_zGPSCControl);


typedef struct			/* Message descriptor for the 'put message' function */
{
	U8 u_PacketId;		/* SBC. Packet ID to be transmitted */
	U8 *p_Buf;			  /* SBC. Pointer to caller's buffer where the packet data resides */
	U16 w_PacketLen;  /* SBC. Packet length */
	U8 u_AckType;		  /* Acknowledge type */
	U8 u_MsgSend;		  /* SBC. TRUE if message is ready to be transmitted, FALSE otherwise */
	U8 u_ForceFlag;		/* SBC. TRUE if message must go through, FALSE otherwise */
} gpsc_ai2_put_msg_desc_type;

T_GPSC_result gpsc_comm_rx_bytes
(
 gpsc_ctrl_type*    p_zGPSCControl,
 U8 *p_uDataBuffer,
 U16 w_NumBytes
);

void gpsc_comm_tx_success(gpsc_ctrl_type* p_zGPSCControl);

void gpsc_comm_write(gpsc_ctrl_type* p_zGPSCControl, gpsc_ai2_put_msg_desc_type *p_MsgDesc);

U8 gpsc_comm_transmit(gpsc_ctrl_type* p_zGPSCControl, U8 u_Ack);

U8 gpsc_comm_check_bufferspace(gpsc_ctrl_type* p_zGPSCControl, U16 w_MoreDataLen);

#endif //_GPSC_COMM_H
