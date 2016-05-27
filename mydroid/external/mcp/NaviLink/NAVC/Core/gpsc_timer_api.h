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
/*******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName			:	gpsc_timer_api.h
 *
 * Description     	:	API for gpsc_os_timer functions.
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#ifndef _GPSC_TIMER_H
#define _GPSC_TIMER_H
#include "gpsc_sap.h"

#define C_TIMER_NVS_SAVE_TIMEOUT (10.0 * 60.0 * 1000.0) /* 10 minutes */

T_GPSC_result gpsc_timer_start(gpsc_ctrl_type*  p_zGPSCControl,
							        U8 timerid, U32 expiry_time);
T_GPSC_result gpsc_timer_stop(gpsc_ctrl_type*  p_zGPSCControl, U8 timerid);
T_GPSC_result gpsc_timer_status(gpsc_ctrl_type*  p_zGPSCControl, U8 timerid);
void gpsc_time_pulse_event_received(gpsc_ctrl_type *p_zGPSCControl,Ai2Field* p_zAi2Field);

#endif //#ifndef _GPSC_TIMER_H
