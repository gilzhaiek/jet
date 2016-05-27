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
 * FileName			:	gpsc_mgp_cfg.h
 *
 * Description     	:
 * This file contains the Sensor configuration module of the GPSC
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#ifndef _GPSC_MGP_CFG_H
#define _GPSC_MGP_CFG_H

enum /* Altitude Hold Mode */
{
  C_ALTHOLDMODE_MANUAL_2D,
  C_ALTHOLDMODE_MANUAL_3D,
  C_ALTHOLDMODE_AUTO,
  C_ALTHOLDMODE_FILTERED,
  C_ALTHOLDMODE_ALLOW_2D
};

/* Receiver Operational Mode */

enum 
{
  C_GPSC_RECV_OP_MODE_ACCURACY, 					 /*                */
  C_GPSC_RECV_OP_MODE_ACCURACY_SPEED_BOTH,  		/*   */
  C_GPSC_RECV_OP_MODE_SPEED						/*     */
};


enum
{
  C_RC_ON,
  C_RC_OFF,
  C_RC_IDLE,
  C_RC_UNKNOWN=255
}; /* receiver operation modes */

enum
{
  AI_SERVER_SMART,
  AI_SERVER_SIMPLE
}; /* ServerMode used in RcvrConfig of Sensor */

/* Event Configuration Definitions */

#define C_EVENT_PERIODIC_TIME       0x01
#define C_EVENT_PERIODIC_POS        0x02
#define C_EVENT_PERIODIC_POSSTAT    0x04
#define C_EVENT_PERIODIC_MEAS       0x08
#define C_EVENT_PERIODIC_MEASSTAT   0x10
#define C_EVENT_PERIODIC_NONEWPOS   0x20 /* AI2 reports that as async */
#define C_EVENT_PERIODIC_NONEWMEAS  0x40 /* AI2 reports that as async */
#define C_EVENT_PERIODIC_POS_EXT    0x80000000
#define C_EVENT_PERIODIC_MEAS_EXT   0x40000000

#define C_EVENT_ASYNC_NEWEPH        0x01
#define C_EVENT_ASYNC_NEWALM        0x02
#define C_EVENT_ASYNC_NEWIONOUTC    0x04
#define C_EVENT_ASYNC_NEWSVHEALTH   0x08
#define C_EVENT_ASYNC_EXT_HW        0x10
#define C_EVENT_ASYNC_DIAG          0x20  /* rcvr ON, OFF, IDLE */
#define C_EVENT_ASYNC_ENGINESTATE     0x40  /* AI2 Async Event Reports */
#define C_EVENT_ASYNC_POWERSAVESTATE  0x80  /* AI2 Async Event Reports */
#define C_EVENT_ASYNC_FIRSTFIXMEAS    0x100  /* AI2 Async Event Reports */

#define C_EVENT_ASYNC_ALL (\
							C_EVENT_ASYNC_NEWEPH  |  \
							C_EVENT_ASYNC_NEWALM  |  \
							C_EVENT_ASYNC_NEWIONOUTC  |  \
							C_EVENT_ASYNC_NEWSVHEALTH  |  \
							C_EVENT_ASYNC_EXT_HW  |  \
							C_EVENT_ASYNC_DIAG  |  \
							C_EVENT_ASYNC_ENGINESTATE  |  \
							C_EVENT_ASYNC_POWERSAVESTATE  |  \
							C_EVENT_ASYNC_FIRSTFIXMEAS)

#define C_EVENT_ASYNC_APM (\
                                                        C_EVENT_ASYNC_NEWEPH  |  \
                                                        C_EVENT_ASYNC_NEWALM  |  \
                                                        C_EVENT_ASYNC_NEWIONOUTC  |  \
							C_EVENT_ASYNC_ENGINESTATE | \
                                                        C_EVENT_ASYNC_NEWSVHEALTH)

void gpsc_mgp_cfg_set_evt
(
  gpsc_ctrl_type*  p_zGPSCControl,
  U8           u_PeriodicAsync,
  U32          q_OnEvents,
  U16         w_PeriodicReportRate,
  U8		  u_SubSecPeriodicReportRate
);

void gpsc_mgp_cfg_evt_rep_init
(
  gpsc_ctrl_type*  p_zGPSCControl
);


void gpsc_mgp_rcvr_cfg
(
  gpsc_ctrl_type*  p_zGPSCControl
);

T_GPSC_result gpsc_mgp_maximize_pdop	(  gpsc_ctrl_type*  p_zGPSCControl);

#endif /* _GPSC_MGP_CFG_H */
