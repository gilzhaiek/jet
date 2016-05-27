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
 * FileName			:	gpsc_sess.h
 *
 * Description     	:
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#ifndef _GPSC_PL_SESS_H
#define _GPSC_PL_SESS_H



#include "gpsc_data.h"




/***  Pre-session action constants  ****/
#define C_DEL_AIDING_TIME           0x1            /* Delete time                    */
#define C_DEL_AIDING_POSITION       0x2           /* Delete position                */
#define C_DEL_AIDING_EPHEMERIS      0x4            /* Delete ephemeris               */
#define C_DEL_AIDING_ALMANAC        0x8            /* Delete almanac                 */
#define C_DEL_AIDING_IONO_UTC       0x10          /* Delete Iono/UTC                */
#define C_DEL_AIDING_SVHEALTH       0x20           /* Delete SV health               */
#define C_DEL_AIDING_DGPS		    0x40		   /* Delete DGPS					 */
#define C_DEL_AIDING_STEERING		0x80		   /* Delete Steering				 */


enum
{
	C_ABORT_CAUSE_RESET,
	C_ABORT_CAUSE_FIX_TO,
	C_ABORT_CAUSE_GPS_TO,
	C_ABORT_CAUSE_SMLC_TO,
	C_ABORT_CAUSE_TERMINATED
};

void gpsc_sess_prepare(gpsc_ctrl_type *p_zGPSCControl);

void gpsc_sess_get_sv_dir(gpsc_ctrl_type *p_zGPSCControl);

void gpsc_sess_end_presession
(
  gpsc_ctrl_type *p_zGPSCControl
);
void gpsc_sess_start(gpsc_ctrl_type *p_zGPSCControl);
void gpsc_sess_end(gpsc_ctrl_type *p_zGPSCControl);
void gpsc_sess_collect_end(gpsc_ctrl_type *p_zGPSCControl);
void gpsc_sess_coll_begin(gpsc_ctrl_type *p_zGPSCControl);
void gpsc_sess_coll_abort(gpsc_ctrl_type *p_zGPSCControl, S16 x_abort_reason);
T_GPSC_result gpsc_sess_req_clock_to_get_assist(gpsc_ctrl_type*  p_zGPSCControl);
U8 gpsc_session_is_nw_session_running (gpsc_sess_cfg_type*    p_zSessConfig);

U8   gpsc_sess_build_wishlist(gpsc_ctrl_type *p_zGPSCControl);

U8 gpsc_sess_pre_act(gpsc_ctrl_type *p_zGPSCControl);
void gpsc_sess_init_counters(gpsc_ctrl_type *p_zGPSCControl);
gpsc_sess_specific_cfg_type* gpsc_session_get_node (gpsc_sess_cfg_type*	p_zSessConfig, 
													U32 				w_SessionTagId);
gpsc_sess_specific_cfg_type* gpsc_session_add_node (gpsc_sess_cfg_type*	p_zSessConfig,
													U32 				w_SessionTagId);
U8 gpsc_session_del_node (gpsc_sess_cfg_type*	p_zSessConfig, U32 w_SessionTagId);
S8 gpsc_sess_update_period(gpsc_ctrl_type *p_zGPSCControl, U8 u_IsiPreset);
U16 gpsc_sess_calc_periodicity (gpsc_ctrl_type *p_zGPSCControl);
U8 gpsc_session_get_all_nodes (gpsc_sess_cfg_type*   p_zSessConfig,U32 *w_SessionTagId, S8 *s_NoSessions);
#ifndef NO_TRACKING_SESSIONS
U8 gpsc_build_ephemeris_wishlist(gpsc_ctrl_type * p_zGPSCControl);
#endif

U16 gpsc_sess_get_nmea_bitmap(gpsc_sess_cfg_type *p_zSessConfig);
void gpsc_inject_cepinfo_start_mode(gpsc_ctrl_type *p_zGPSCControl,U16 timeout1,U16 timeout2,U16 accuracy1,U16 accuracy2);
#endif /* _GPSC_PL_SESS_H */
