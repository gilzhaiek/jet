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
 * FileName			:	gpsc_app_api.h
 *
 * Description     	:
 * This header file supports the api for the application portion of the GPSC
 * interface.
 *
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#ifndef _GPSC_APP_API_H
#define _GPSC_APP_API_H

#include "navc_api.h"
#include "navc_sm.h"
#include "pla_os.h"
/* added for RTI support in config file */
#define C_RTI_ENABLE 1
#define C_RTI_DISABLE 0
#define MAX_NO_GEOFENCE_REGIONS 24 


/* Recon add-on to indicate start of Assistance Transaction */
void RECON_gpsc_app_assist_begin_req ();
int  RECON_get_assist_in_progress ();
void RECON_set_assist_in_progress (int state);

T_GPSC_result gpsc_request_assistance(gpsc_ctrl_type *p_zGPSCControl);

void gpsc_send_position_result(gpsc_ctrl_type*  p_zGPSCControl, U8 u_FixNoFix, gpsc_sess_specific_cfg_type* p_zSessSpecificCfg);

U8 gpsc_send_measurement_result(gpsc_ctrl_type*  p_zGPSCControl, U8 u_MeasNoMeas, gpsc_sess_specific_cfg_type* p_zSessSpecificCfg);

void gpsc_release_memory(gpsc_ctrl_type*  p_zGPSCControl);

void gpsc_send_nmea_result(gpsc_ctrl_type*  p_zGPSCControl,gpsc_sess_specific_cfg_type* p_zSessSpecificCfg);

/* Populate eph data for Sagps update */
void gpsc_populate_send_eph_data(gpsc_db_eph_type *p_DBEphemeris);

//extern T_GPSC_result gpsc_app_inject_dynamic_feature_config (T_GPSC_dyn_feature_config *dyna_feature_cfg);

T_GPSC_result gpsc_app_gps_ver_res (void);
T_GPSC_result gpsc_app_blf_status_res (void);
extern T_GPSC_result gpsc_app_sw_reset(void);
T_GPSC_result gpsc_parse_control_file(TNavcSm *pNavcSm, gpsc_sys_handlers* p_GPSCSysHandlers, McpS8* s_PathFileName);
void gpsc_app_stop_nw_session_indication();

#ifdef WIFI_ENABLE 
void gpsc_app_send_wifi_position_result( gpsc_ctrl_type* p_zGPSCControl, gpsc_sess_specific_cfg_type* p_zSessSpecificCfg, U8 u_gps_pos_valid  );	

void gpsc_app_send_gps_position_result( gpsc_ctrl_type* p_zGPSCControl, gpsc_sess_specific_cfg_type* p_zSessSpecificCfg );
void gpsc_app_send_blended_position_result( gpsc_ctrl_type* p_zGPSCControl, gpsc_sess_specific_cfg_type* p_zSessSpecificCfg, U8 u_gps_pos_valid  );	
void gpsc_populate_wifi_rrlp_pos(gpsc_ctrl_type* p_zGPSCControl,T_GPSC_prot_position* p_prot_position, U8 u_gps_pos_valid);


void gpsc_populate_gpswifi_blend_rrlp_pos(gpsc_ctrl_type* p_zGPSCControl,T_GPSC_prot_position* p_prot_position, U8 u_gps_pos_valid);
#endif
#if 1//def WIFI_DEMO
void gpsc_app_send_gps_wifi_blend_report(gpsc_ctrl_type* p_zGPSCControl, gpsc_sess_specific_cfg_type* p_zSessSpecificCfg,U8 u_gps_pos_valid, U8 u_wifi_pos_valid);
#endif


T_GPSC_result gpsc_app_inject_sensor_assistance(void *pData);

T_GPSC_result gpsc_app_inject_apm_config(void *pData);
void gpsc_app_update_gf_config(gpsc_ctrl_type* p_zGPSCControl,gpsc_sess_specific_cfg_type* p_zSessSpecificCfg);
T_GPSC_result gpsc_register_for_assistance(McpU16 AssistFlags, McpU8 SrcTaskId, McpU8 uSrcQId);
void gpsc_app_update_apm_config(gpsc_ctrl_type* p_zGPSCControl);
void gpsc_populate_send_gpstime_data(gpsc_db_gps_clk_type*  p_DBGpsClock);
T_GPSC_result gpsc_app_control_apm_params (TNAVC_SetAPMParams *apm_params,McpU32 sessionid);
gpsc_toe_validity_bounds get_toe_validity_bounds(U32 toe_val);

U8 is_eph_valid_for_gpssec(U32 eph_toe, U32 gpssec);
extern T_GPSC_result gpsc_app_get_motion_mask(McpU8  regionnumber);
void gpsc_log_ctrl (gpsc_ctrl_type *p_zGPSCControl, McpU8 navc_log_ctrl, McpU8 sensor_log_ctrl,McpU8 session_log_ctrl);
T_GPSC_result gpsc_app_blf_session_request(TNAVC_BLF_Session_Config *blf_cfg);
void gpsc_app_inject_new_timeout(TNAVC_reqLocFix* loc_fix_req_params);
void gpsc_app_inject_fix_update_rate(TNAVC_updateFixRate* loc_fix_update);
T_GPSC_result gpsc_app_gps_wakeup_from_cmdhandler ();
T_GPSC_result gpsc_app_enter_fwd_req (void);
T_GPSC_result gpsc_app_exit_fwd_req (void);
T_GPSC_result gpsc_app_fwd_tx_req( U8 *p_uDataBuffer, U16 w_NumBytes);
void	gpsc_app_rep_on_gf_breach(McpU8 value);
void gpsc_init_logging (gpsc_ctrl_type *p_zGPSCControl, gpsc_sys_handlers* temp_sysHandler);
T_GPSC_result gpsc_app_get_agc();
extern T_GPSC_result gpsc_app_send_msa_supl_postion(TNAVC_posIndData* posInd);
T_GPSC_result gpsc_app_gps_wakeup_from_cmdhandler ();
void gpsc_app_fwd_rx_res( U8 *p_uDataBuffer, U16 w_NumBytes);
McpU8 gpsc_get_gf_report_config(gpsc_sess_cfg_type*    p_zSessConfig,gpsc_sess_specific_cfg_type* p_zCurSessConfig);
U16 gpsc_get_min_toe_diff(gpsc_ctrl_type*  p_zGPSCControl);
#endif //#ifndef _GPSC_APP_API_H
