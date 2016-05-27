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
 * FileName			:	gpsc_database.h
 *
 * Description     	:
 * This file contains functions that manage the LM database and non-volatile
 * memory that keeps information about knowledge the companion processor has,
 * in terms of GPS time, initial position, almanac, ephemeris, Iono, health,
 * measurement, etc.
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#ifndef _GPSC_DATABASE_H
#define _GPSC_DATABASE_H


#include "gpsc_data.h"
#include "gpsc_mgp_rx.h"

void gpsc_db_update_clock(Ai2Field *p_zAi2Field, gpsc_db_type *p_zGPSCDatabase );
U8	 gpsc_db_update_pos(Ai2Field *p_zAi2Field, gpsc_db_type *p_zGPSCDatabase);
U8	 gpsc_db_update_ext_pos(Ai2Field *p_zAi2Field, gpsc_db_type *p_zGPSCDatabase);
U8   gpsc_db_update_pos_posloc(Ai2Field *p_zAi2Field, gpsc_ctrl_type*  p_zGPSCControl);
void gpsc_db_update_alm (Ai2Field *p_Field, gpsc_db_type *p_zGPSCDatabase);
void gpsc_db_update_eph (Ai2Field *p_Field, gpsc_db_type *p_zGPSCDatabase);
void gpsc_db_update_iono(Ai2Field *p_zAi2Field, gpsc_db_type *p_zGPSCDatabase);
void gpsc_db_update_utc(Ai2Field *p_zAi2Field, gpsc_db_type *p_zGPSCDatabase); //-->UTC
void gpsc_db_update_health( Ai2Field *p_Field, gpsc_db_type *p_zGPSCDatabase );

void gpsc_db_update_sv_meas(Ai2Field *p_zAi2Field, gpsc_db_type *p_zGPSCDatabase);

void gpsc_db_update_sv_ext_meas(Ai2Field *p_zAi2Field, gpsc_db_type *p_zGPSCDatabase);

U8 gpsc_db_save_to_nvs(gpsc_db_type *p_zGPSCDatabase);
void gpsc_nvs_update_pos (gpsc_db_type *p_zGPSCDatabase);
void gpsc_nvs_update_eph (gpsc_db_type *p_zGPSCDatabase);
void gpsc_nvs_update_iono(gpsc_db_type *p_zGPSCDatabase);
void gpsc_nvs_update_utc(gpsc_db_type *p_zGPSCDatabase); //--> UTC
void gpsc_nvs_update_alm (gpsc_db_type *p_zGPSCDatabase);
void gpsc_nvs_update_health (gpsc_db_type *p_zGPSCDatabase);
void gpsc_nvs_update_clock_uncertanity (gpsc_db_type *p_zGPSCDatabase);
void gpsc_nvs_update_tcxo (gpsc_db_type *p_zGPSCDatabase);


void gpsc_db_update_sv_dir(Ai2Field* p_zAi2Field,gpsc_db_type*  p_zGPSCDatabase);
void gpsc_db_update_sv_meas_stat(Ai2Field* p_zAi2Field,gpsc_db_type*  p_zGPSCDatabase);

U8 gpsc_db_is_pos_ok(gpsc_db_pos_type*  p_zDBPos,DBL d_CurrentAccGpsSec,U32 q_threshold);
U8 gpsc_db_get_num_of_eph(gpsc_ctrl_type* p_zGPSCControl);

void gpsc_db_clr_meas_stat(  gpsc_meas_stat_report_type*  p_zMeasStatReport);
void gpsc_db_upd_meas_hist( gpsc_db_gps_meas_type*  p_zDBGpsMeas );

void gpsc_db_clear_clock(gpsc_ctrl_type*  p_zGPSCControl);

U32 gpsc_db_gps_time_in_secs(gpsc_ctrl_type*  p_zGPSCControl);
U8 gpsc_db_update_geofence_status(Ai2Field *p_Field, gpsc_db_type *p_zGPSCDatabase);

#endif /* _GPSC_DATABASE_H */
