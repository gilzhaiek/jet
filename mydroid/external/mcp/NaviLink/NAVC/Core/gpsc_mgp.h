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
 * FileName			:	gpsc_mgp.h
 *
 * Description     	:
 * This file contains private definitions and interfaces to the Sensor
 * module of the GPSC
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#ifndef _GPSC_MGP_H
#define _GPSC_MGP_H

#include "gpsc_mgp_rx.h"
#include "gpsc_cd.h"

/*===========================================================================

FUNCTION
  GPSC_MGP_PROCESS_MSG

DESCRIPTION
  This function processes AI2 message coming from the Sensor

===========================================================================*/
extern void gpsc_mgp_process_msg
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*       p_zAi2Field
);

U8 mgp_custom_ver_resp
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*    p_zAi2Field
);

U8 mgp_send_blf_sts
(
  gpsc_ctrl_type* p_zGPSCControl,
  Ai2Field* p_zAi2Field
);
#endif /* _GPSC_MGP_H */
