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
 * FileName			:	navc_hybridPosition.h
 *
 * Description     	:
 * function and parameter definition for hybrid position
 *
 *
 * Author         	: 	Raghavendra MR
 *
 *
 ******************************************************************************
 */

#ifndef _NAVC_HYBRIDPOSITION_H
#define _NAVC_HYBRIDPOSITION_H

void hybrid_gps_wifi_pos_blender
(
  gpsc_ctrl_type* p_zGPSCControl,
  gpsc_sess_specific_cfg_type* p_zSessSpecificCfg
);

void wifi_log_init(gpsc_ctrl_type* p_zGPSCControl);
void wifi_log_close(gpsc_ctrl_type* p_zGPSCControl);

void gps_wifi_pos_blender(U8 u_gps_pos_valid,U8 u_wifi_pos_valid,gpsc_db_sensor_calib_report_type  *p_zSensorReport,gpsc_report_pos_type  *p_zReportPos,
 gpsc_db_wifi_pos_type *p_z_WifiPos,gpsc_db_blended_pos_type *p_z_BlendedPos);

U8 check_any_gps_session( gpsc_ctrl_type* p_zGPSCControl);

#endif //_NAVC_HYBRIDPOSITION_H