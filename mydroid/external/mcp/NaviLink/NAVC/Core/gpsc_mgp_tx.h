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
 * FileName			:	gpsc_mgp_tx.h
 *
 * Description     	:
 * This file contains the Sensor transmit module  of the GPSC
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#ifndef _GPSC_MGP_TX_H
#define _GPSC_MGP_TX_H



#include "gpsc_data.h"
#include "gpsc_mgp_assist.h"
#define	C_LSB_CE_OSC_QUAL		(0.01) 	/* ppm */

enum
{
	C_GPSC_PATCH_SYNC_DWLD=0,
	C_GPSC_PATCH_CUR_LINK_DWLD,
	C_GPSC_PATCH_STACKRAM_DWLD = 3,
	C_GPSC_PATCH_NO_DOWNLOAD=5
};

void gpsc_mgp_send_patch_dwld_ctrl(
  gpsc_ctrl_type*  p_zGPSCControl,
  U8           u_DwldMode
);

void gpsc_mgp_ver_req(  gpsc_ctrl_type*  p_zGPSCControl);

U8 gpsc_process_patch_download( gpsc_ctrl_type*  p_zGPSCControl);

void gpsc_read_next_dwld_record(gpsc_ctrl_type*  p_zGPSCControl);

void gpsc_tx_download_record(gpsc_ctrl_type* p_zGPSCControl);

void gpsc_send_end_of_download(gpsc_ctrl_type* p_zGPSCControl);

void gpsc_mgp_inject_init_data
(
  gpsc_ctrl_type*  p_zGPSCControl
);

U8 gpsc_mgp_inject_nvs_pos(gpsc_ctrl_type * p_zGPSCControl);

U8 gpsc_mgp_inject_nvs_iono (gpsc_ctrl_type*  p_zGPSCControl);

U8 gpsc_mgp_inject_nvs_health(gpsc_ctrl_type*  p_zGPSCControl);

U8 gpsc_mgp_inject_nvs_alm(gpsc_ctrl_type* p_zGPSCControl);

U8 gpsc_mgp_inject_nvs_eph (gpsc_ctrl_type*  p_zGPSCControl);

void  gpsc_mgp_inject_nvs_suplhot(gpsc_ctrl_type* p_zGPSCControl);

U8 gpsc_mgp_inject_init_time
(
  gpsc_ctrl_type*  p_zGPSCControl
);

U8 gpsc_mgp_inject_init_utc
(
  gpsc_ctrl_type*  p_zGPSCControl
);

void gpsc_mgp_inject_gps_time
(
  gpsc_ctrl_type*             p_zGPSCControl,
  gpsc_inject_time_est_type*  p_zInjectTimeEst
);

void gpsc_mgp_inject_freq_est
(
  gpsc_ctrl_type*             p_zGPSCControl,
  gpsc_inject_freq_est_type*  p_zInjectFreqEst
);

void gpsc_mgp_inject_pos_est
(
  gpsc_ctrl_type *p_zGPSCControl,
  gpsc_inject_pos_est_type *p_InjectPosEst,
  U8 u_reRequest
);

U8 gpsc_mgp_inject_alm
(
  gpsc_ctrl_type*  p_zGPSCControl,
  pe_RawAlmanac*  p_RawAlmanac
);

U8 gpsc_mgp_inject_eph
(
  gpsc_ctrl_type*        p_zGPSCControl,
  gpsc_inject_eph_type*  p_zInjectEph,
  McpU8 u_zAckReq
);

void gpsc_mgp_inject_iono
(
  gpsc_ctrl_type*      p_zGPSCControl,
  gpsc_raw_iono_type*  p_Iono
);

void gpsc_mgp_inject_utc //--> added for UTC
(
  gpsc_ctrl_type*      p_zGPSCControl,
  gpsc_raw_utc_type*  p_Utc
);

void gpsc_mgp_inject_dgps
(
  gpsc_ctrl_type*  p_zGPSCControl,
  gpsc_dgps_assist_type*     p_Dgps
);


void gpsc_mgp_inject_sv_health
(
  gpsc_ctrl_type*  p_zGPSCControl,
  U8*             p_SvHealth
);


void gpsc_mgp_inject_sv_dir
(
  gpsc_ctrl_type*   p_zGPSCControl,
  gpsc_inj_sv_dir*  p_zInjectSvDir,
  U8              u_Count
);

void gpsc_mgp_inject_OscParams
(
  gpsc_ctrl_type*  p_zGPSCControl
);

void gpsc_mgp_inject_CalibTimestamp_Period
(
  gpsc_ctrl_type*  p_zGPSCControl
);

void gpsc_mgp_inject_calibrationcontrol
(
  gpsc_ctrl_type*  p_zGPSCControl,
  U8 u_CalibrationType
);


void gpsc_mgp_inject_sv_steering
(
  gpsc_ctrl_type*  p_zGPSCControl,
  me_SvSteering*  p_zSvSteer,
  U8              u_SvId,
  U8              u_SteerFlag
);

void gpsc_mgp_inject_sv_diff
(
  gpsc_ctrl_type*   p_zGPSCControl,
  U32             q_FCount,
  U8              u_RefSv,
  FLT             f_RefSvTimeUncMs,
  FLT             f_RefSvSpeedUnc,
  U8              u_NDiffs,
  me_SvDiff*      p_zSvDiff
);

void gpsc_mgps_inject_pred_data_bits
(
  gpsc_ctrl_type*     p_zGPSCControl,
  gpsc_pred_db_type*  p_zPredDb
);

void gpsc_mgp_tx_req
(
  gpsc_ctrl_type*  p_zGPSCControl,
  U8              u_PacketId,
  U8              u_SvId
);
/* For product line Test */
S8 gpsc_mgp_tx_prodlinetest_req
(
  gpsc_ctrl_type*  p_zGPSCControl

);


void gpsc_mgp_tx_rc_act
(
  gpsc_ctrl_type*  p_zGPSCControl,
  U8              u_ActionType,
  U8              u_ActionModifier
);

void gpsc_mgp_tx_nmea_ctrl(gpsc_ctrl_type* p_zGPSCControl, U16 w_NMEABitmap);

void gpsc_mgp_tx_i2c_config(gpsc_ctrl_type* p_zGPSCControl);

void gpsc_mgp_tx_custom_packet(gpsc_ctrl_type* p_zGPSCControl,U8 *p_Body, U8 u_length);
void gpsc_mgp_tx_customer_config(gpsc_ctrl_type* p_zGPSCControl);
void gpsc_mgp_tx_customer_config_extended(gpsc_ctrl_type* p_zGPSCControl);
void gpsc_mpg_init_dynamic_feature_config (gpsc_ctrl_type* p_zGPSCControl);
void gpsc_mpg_inject_dynamic_feature_config (gpsc_ctrl_type* p_zGPSCControl);
void gpsc_mgp_tx_inject_hostwakeup(gpsc_ctrl_type* p_zGPSCControl);
void gpsc_mgp_tx_inject_sbas(gpsc_ctrl_type* p_zGPSCControl);
U8 gpsc_mgp_tx_inject_motion_mask(gpsc_ctrl_type* p_zGPSCControl);
U8  gpsc_mgp_tx_req_sv_meas_del(gpsc_ctrl_type* p_zGPSCControl, U8 sv_ID);
void gpsc_mgp_tx_inject_advanced_power_management (gpsc_ctrl_type* p_zGPSCControl);

void gpsc_mgp_tx_inject_timeout_cep_info(gpsc_ctrl_type* p_zGPSCControl, gpsc_loc_fix_qop *p_zQop);
void gpsc_mgp_tx_inject_baud_rate (gpsc_ctrl_type* p_zGPSCControl);
void maximize_frunc(gpsc_ctrl_type *p_zGPSCControl);
void gpsc_mgp_inject_comm_prot(gpsc_ctrl_type* p_zGPSCControl);

U8 gpsc_tx_req_injected_alm(gpsc_ctrl_type *p_zGPSCControl);

U8 gpsc_tx_req_injected_eph(gpsc_ctrl_type *p_zGPSCControl);

U8 gpsc_mgp_inject_init_health
(
  gpsc_ctrl_type*  p_zGPSCControl
);


void gpsc_mgp_inject_timepulse_ref_clk_req_res (gpsc_ctrl_type*  p_zGPSCControl,U8 u_TimestampRefClkResponse);
void gpsc_mgp_inject_gps_shutdown_ctrl(gpsc_ctrl_type* p_zGPSCControl);
void gpsc_mgp_inject_wakeup_sequence(gpsc_ctrl_type* p_zGPSCControl);

void gpsc_mgp_inject_altitude(  gpsc_ctrl_type*    p_zGPSCControl,  gpsc_inject_altitude_type*  p_InjectAltitude);

void gpsc_mgp_skip_download (gpsc_ctrl_type* p_zGPSCControl);

void gpsc_mgp_tx_visible_sv_eph(gpsc_ctrl_type* p_zGPSCControl);

void gpsc_mgp_dr_req(gpsc_ctrl_type*  p_zGPSCControl, U8 *p_Data, U32 len);

void gpsc_mgp_ver_req(  gpsc_ctrl_type*  p_zGPSCControl);

void gpsc_mgp_tx_rtc_control(gpsc_ctrl_type* p_zGPSCControl);

U8 gpsc_mgp_inj_alm_on_wakeup(gpsc_ctrl_type*  p_zGPSCControl);
void gpsc_mgp_tx_blf_config(gpsc_ctrl_type* p_zGPSCControl);
void gpsc_mgp_tx_blf_sts(gpsc_ctrl_type* p_zGPSCControl);
void gpsc_mgp_tx_blf_buff_dump(gpsc_ctrl_type* p_zGPSCControl);

#ifdef WIFI_ENABLE
void gpsc_mgp_tx_inject_wlan_assist_info(gpsc_ctrl_type* p_zGPSCControl,gpsc_db_blended_pos_type *p_z_BlendedPos,gpsc_report_pos_type  *p_zReportPos);
#endif //#ifdef WIFI_ENABLE

void gpsc_mgp_inject_sensor_assistance(gpsc_ctrl_type* p_zGPSCControl, void *pSEOut);
void gpsc_mgp_tx_write_reg(gpsc_ctrl_type* p_zGPSCControl, U32 q_WriteReg, U32 q_WriteMask, U32 q_WriteValue);
void gpsc_mgp_tx_write_memory(gpsc_ctrl_type* p_zGPSCControl, U32 q_WriteReg, U8 uDatatype, U8 uDataLen, U8 *uData);
U8 gpsc_mgp_inject_nvs_data ( gpsc_ctrl_type*  p_zGPSCControl );
void gpsc_mgp_inject_pos_est_wakeup(gpsc_ctrl_type*    p_zGPSCControl,gpsc_inject_pos_est_type*  p_InjectPosEst,U8 u_reRequest);
#endif /* _GPSC_MGP_TX_H */
