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
 * FileName			:	gpsc_mgp_rx.h
 *
 * Description     	:
 * This file contains the Sensor receive module  of the GPSC
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#ifndef _GPSC_MGP_RX_H
#define _GPSC_MGP_RX_H

#include "gpsc_sap.h"
#include "nav_log_msg.h"
#include "gpsc_app_api.h"
void gpsc_mgp_rx_proc_pos
(
  gpsc_ctrl_type*  p_zGPSCControl,
  U8              u_FixNoFix
);

void gpsc_mgp_rx_proc_nmea
(
gpsc_ctrl_type*  p_zGPSCControl
);

void gpsc_mgp_rx_proc_meas
(
  gpsc_ctrl_type*  p_zGPSCControl,
  U8              u_MeasNoMeas
);

void gpsc_mgp_rx_proc_asyn_evt
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*       p_zAi2Field
);

void gpsc_mgp_rx_proc_dwld_rec_res
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*       p_zAi2Field
);

U8 gpsc_mgp_rx_proc_dwld_complete
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*       p_zAi2Field
);

char* gpsc_populate_nmea_buffer
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*        p_zAi2Field
);

T_GPSC_result gpsc_rx_examine_gps_status
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*    p_zAi2Field
);

U8 gpsc_mgp_rx_proc_selftest_report
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*    p_zAi2Field
);

void gpsc_mgp_rx_prodlinetest_report
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*    p_zAi2Field
);
void gpsc_mgp_rx_calibcontrol_report
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*    p_zAi2Field
);

void gpsc_report_error_status
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*    p_zAi2Field
);

void gpsc_mgp_rx_invalid_msg
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*    p_zAi2Field
);

void gpsc_mgp_rx_sensor_pe_data(U8 *p_B);

void gpsc_mgp_rx_sensor_calib_data_for_blend(U8 *p_B);
void gpsc_mgp_rx_reg_read_response(gpsc_ctrl_type *p_zGPSCControl, Ai2Field *p_zAi2Field);
void gpsc_geofence_pro_report(  gpsc_ctrl_type*  p_zGPSCControl,TNAVC_MotionMask_Status* p_zmotionmaskstatus);
void gpsc_mgp_rx_motion_mask_setting ( gpsc_ctrl_type*  p_zGPSCControl,Ai2Field*    p_zAi2Field);


#endif /* _GPSC_MGP_RX_H */
