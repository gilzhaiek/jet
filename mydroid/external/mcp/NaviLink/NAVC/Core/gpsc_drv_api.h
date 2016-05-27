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
 * FileName			:	gpsc_drv_api.h
 *
 * Description     	:
 * This header file supports the api for the Driver portion of the GPSC
 * interface.
 *
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#ifndef _GPSC_DRV_API_H
#define _GPSC_DRV_API_H

enum
{
	C_GPSC_POWER_STATE_OFF,				/*Power_EN = 0, Reset = X, SleepX = X*/
	C_GPSC_POWER_STATE_RESET,			/*Power_EN = 1, Reset = 0, SleepX = 1*/
	C_GPSC_POWER_STATE_SLEEP,			/*Power_EN = 1, Reset = 1, SleepX = 0*/
	C_GPSC_POWER_STATE_ON				/*Power_EN = 1, Reset = 1, SleepX = 1*/
};

//U8 gpsc_power_control(gpsc_ctrl_type*	p_zGPSCControl, U8 u_PowerState);

T_GPSC_result gpsc_transmit_data(U8* p_uDataBuffer, U32 w_NumBytes);

#endif //#ifndef _GPSC_DRV_API_H
