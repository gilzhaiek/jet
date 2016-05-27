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
#include "navc_api.h"
#include "navc_api_pvt.h"
#include "navc_priority_handler.h"
#ifndef _GPSC_SAP_H_
#define _GPSC_SAP_H_

#include "mcpf_defs.h"

typedef enum
{
	GPSC_PENDING                   = 0x1,           /* OK, will be completed asynchronously */
	GPSC_SUCCESS                   = 0x0,           /* OK, successful completed.      */
	GPSC_FAIL                      = -0x1,          /* ERROR, failed                  */
	GPSC_UNITIALIZED_FAIL          = -0x2,          /* ERROR, GPSC not initialized    */
	GPSC_INCOMPATIBLE_ROM_VERSION_FAIL = -0x3,      /* ERROR, GPS ROM version not compatible with GPSC */
	GPSC_PERIODIC_TIMER_EXPIRED    = -0x4,          /* STATUS, Periodic Timer Expired due to report not received */
	GPSC_GPS_ACK_NOT_RECEIVED      = -0x5,          /* Ack from GPS not received      */
	GPSC_APP_LOC_FIX_REQ_NOT_RECEIVED = -0x6,       /* Location Fix request not received from application */
	GPSC_DISCRETE_CONTROL_INITIATE_FAIL = -0xb,     /* ERROR, discrete control initiation fail */
	GPSC_DISCRETE_CONTROL_FAIL     = -0xc,          /* ERROR, discrete control fail   */
	GPSC_DISCRETE_CONTROL_TIMEOUT_FAIL = -0xd,      /* ERROR, discrete control timeout fail */
	GPSC_TIME_PULSE_GENERATION_FAIL = -0xe,         /* ERROR, Pulse generation not available */
	GPSC_TIME_REQUEST_FAIL         = -0xf,          /* ERROR, Time request did not return with time */
	GPSC_TIME_RESPONSE_TIMEOUT_FAIL = -0x10,        /* ERROR, Timeout while waiting for response for time request from GPSM */
	GPSC_TIME_PULSE_EVENT_TIMEOUT_FAIL = -0x11,     /* ERROR, Timeout while waiting for time pulse event from GPS */
	GPSC_TIME_PULSE_GENERATION_PENDING = -0x12,     /* STATUS, time pulse generation was requested and is pending for completion */
	GPSC_TX_INITIATE_FAIL          = -0x14,         /* ERROR, transmission initiation fail */
	GPSC_TX_FAIL                   = -0x15,         /* ERROR, transmission fail       */
	GPSC_TX_TIMEOUT_FAIL           = -0x16,         /* ERROR, transmission timeout fail */
	GPSC_COMM_INIT_FAIL            = -0x17,         /* ERROR, communication channel initialization fail */
	GPSC_RX_BUFFER_FULL_FAIL       = -0x18,         /* ERROR, receive serial buffer full */
	GPSC_MEM_ALLOCATION_FAIL       = -0x1e,         /* ERROR, memory allocation error */
	GPSC_MEM_FREE_ERROR            = -0x1f,         /* ERROR, memory release error    */
	GPSC_TIMER_START_FAIL          = -0x28,         /* ERROR, timer start fail        */
	GPSC_TIMER_STOP_FAIL           = -0x29,         /* ERROR, timer stop fail         */
	GPSC_UNKNOWN_TIMER_EXPIRED_FAIL = -0x2a,        /* ERROR, unknown or invalid timer expired */
	GPSC_FILE_WRITE_FAIL           = -0x32,         /* ERROR, file write error        */
	GPSC_FILE_DELETE_FAIL          = -0x33,         /* ERROR, file delete error       */
	GPSC_CONFIG_FILE_READ_FAIL     = -0x34,         /* ERROR, configuration file read error */
	GPSC_CONFIG_FILE_CHECKSUM_FAIL = -0x35,         /* ERROR, configuration file checksum error */
	GPSC_CONFIG_FILE_CONTENT_FAIL  = -0x36,         /* ERROR, configuration file content error */
	GPSC_CONFIG_FILE_NOT_PRESENT_FAIL = -0x37,      /* ERROR, configuration file not present */
	GPSC_PATCH_FILE_OPEN_FAIL      = -0x38,         /* ERROR, patch file open error   */
	GPSC_PATCH_FILE_READ_FAIL      = -0x39,         /* ERROR, patch file read error   */
	GPSC_PATCH_FILE_SIZE_FAIL      = -0x3a,         /* ERROR, patch file size error   */
	GPSC_PATCH_FILE_RECORD_FAIL    = -0x3b,         /* ERROR, patch file record contents error */
	GPSC_NO_FIX_IN_PROGRESS_FAIL   = -0x46,         /* ERROR, bad command - fix not in progress */
	GPSC_FIX_ALREADY_IN_PROGRESS_FAIL = -0x47,      /* ERROR, bad command - fix already in progress */
	GPSC_UNKNOWN_AIDING_TYPE_FAIL  = -0x48,         /* ERROR, invalid aiding injection type */
	GPSC_ASSISTANCE_TIMEOUT_FAIL   = -0x49,         /* ERROR, timeout waiting for assistance data */
	GPSC_LOC_FIX_INVALID_FIX_MODE  = -0x4a,         /* ERROR, Invalid fix mode selected */
	GPSC_LOC_FIX_INVALID_RESULT_TYPE = -0x4b,       /* ERROR, Invalid result type selected */
	GPSC_LOC_FIX_RESULT_MISMATCH   = -0x4c,         /* ERROR, Mismatch between fix mode and result type selected */
	GPSC_LOC_FIX_INVALID_PERIOD    = -0x4d,         /* ERROR, Invalid period (0 seconds) selected */
	GPSC_LOC_FIX_INVALID_MAX_TTFF  = -0x4e,         /* ERROR, Invalid time to first fix (0 seconds) selected */
	GPSC_INTERNAL_FAIL             = -0x5a,         /* ERROR, GPSC internal error     */
	GPSC_GPS_BIST_FAIL             = -0x64,         /* ERROR, GPS built-in self test fail, GPS not available */
	GPSC_GPS_STATUS_MSG_FAIL       = -0x65,         /* ERROR, Invalid GPS status message received */
	GPSC_GPS_SELF_DETECTED_FATAL_FAIL = -0x66,      /* ERROR, GPS software detected fatal error */
	GPSC_GPS_EXCEPTION_RESET_FAIL  = -0x67,          /* ERROR, GPS exception and self reset */
	GPSC_GPS_INVALID_BLF_PARMS_FAIL  = -0x68
}T_GPSC_result;

typedef enum
{
	GPSC_FILE_SUCCESS              = 0x0,           /* OK, successful - requested number of bytes successfully read from or written to file, or file successfully deleted */
	GPSC_FILE_FAIL                 = -0x1,          /* ERROR, failed - requested number of bytes could not be read from or written to file, or file could not be deleted */
	GPSC_FILE_NOT_PRESENT          = -0x2           /* File not found                 */
}T_GPSC_file_result;

typedef enum
{
	GPSC_DIAG_ERROR_WARNING        = 0x1,           /* Error and warning diagnostics  */
		GPSC_DIAG_GPSC_INTERNAL_STATUS = 0x2,           /* GPSC internal status information */
		GPSC_DIAG_GPSM_INTERACTION     = 0x4,           /* GPSC/GPSM interaction information */
		GPSC_DIAG_AI2_INTERACTION      = 0x8            /* Information on AI2 interaction with GPS baseband */
}T_GPSC_diag_level;

typedef struct
{
	U8                        calib_result;             /*<  0:  1> Bit 0 :  0 Calibration disable complete            */
	U8                        zzz_align0;               /*<  1:  1> alignment                                          */
	U8                        zzz_align1;               /*<  2:  1> alignment                                          */
	U8                        zzz_align2;               /*<  3:  1> alignment                                          */
} T_GPSC_calib_response;

#if 0
typedef struct
{
	U8                        hw_major;                 /*<  0:  1> Bits 7 - 4: major version of byte value 1          */
	U8                        hw_minor;                 /*<  1:  1> Bits 3 - 0: minor version of byte value 1          */
	U8                        sw_major;                 /*<  2:  1> Bits 31- 24: major version of byte value 4         */
	U8                        sw_minor;                 /*<  3:  1> Bits 23 - 16: minor version of byte value 4        */
	U8                        sw_subminor1;             /*<  4:  1> Bits 15 - 8: sub-minor 1 of byte value 4           */
	U8                        sw_subminor2;             /*<  5:  1> Bits 7 - 0: sub-minor 2 of byte value 4            */
	U8                        sw_day;                   /*<  6:  1> Unsigned. Range 1 to 31.                           */
	U8                        sw_month;                 /*<  7:  1> Unsigned. Range 1 to 12.                           */
	U16                       sw_year;                  /*<  8:  2> Unsigned. 2003 to 2999.                            */
	U8                        zzz_align0;               /*< 10:  1> alignment                                          */
	U8                        zzz_align1;               /*< 11:  1> alignment                                          */
} T_GPSC_gps_ver_resp_params;
#endif
extern T_GPSC_result gpsc_app_shutdown_req (void);
extern T_GPSC_result gpsc_app_gps_ready_req (void);
extern T_GPSC_result gpsc_app_init_req (void *sysHandlers, void **hGpsc);
extern T_GPSC_result gpsc_app_gps_powersave_req (void);
extern void gpsc_cmd_complete_cnf (T_GPSC_result result);
extern T_GPSC_result gpsc_app_loc_fix_req (TNAVC_reqLocFix *loc_fix_req_params, McpU8 blf_flg);
extern void gpsc_app_loc_fix_start_cnf (T_GPSC_result result, U32 w_SessionTagId);
extern T_GPSC_result gpsc_app_loc_fix_stop_req ( U32 loc_fix_session_id);
extern void gpsc_app_loc_fix_ind (T_GPSC_loc_fix_response *loc_fix_response);
extern T_GPSC_result gpsc_app_assist_inject_req (T_GPSC_assistance_inject *assistance_inject);
extern T_GPSC_result gpsc_app_assist_complete_req (void);
extern T_GPSC_result gpsc_app_assist_delete_req (U8 del_assist_bitmap, U32 sv_bitmap);
T_GPSC_result gpsc_app_assist_request_ind (U16 assist_bitmap_mandatory, 
												U16	 assist_bitmap_optional, 
												T_GPSC_nav_model_req_params *nav_model_req_params,
												my_Position *pos_info,
												T_GPSC_supl_qop *gps_qop, 
												T_GPSC_current_gps_time *tGpsTime,
												U32 SrcTaskId,
												U32 SrcQueueId);
extern T_GPSC_result gpsc_app_stop_request_ind ();
extern T_GPSC_result gpsc_app_stop_nw_connection();

extern T_GPSC_result gpsc_app_inject_ref_clock_parameters_req (U16 ref_clock_quality, U32 ref_clock_frequency);
extern T_GPSC_result gpsc_app_inject_configuration_req (T_GPSC_config_file *config_file, T_GPSC_patch_available save_to_file );
extern T_GPSC_result gpsc_app_request_configuration_req (T_GPSC_config_file *config_file);
extern T_GPSC_result gpsc_time_request_tow_ind (T_GPSC_time_assist *time);
extern T_GPSC_result gpsc_time_request_pulse_ind ();
extern T_GPSC_result  gpsc_time_request_pulse_res (T_GPSC_result result);
extern T_GPSC_result gpsc_time_report_tow_ind (T_GPSC_time_accuracy time_accuracy, T_GPSC_time_assist *time);
extern T_GPSC_result gpsc_drv_transmit_data_res (handle_t hCbHandle, void *pTxn);
extern T_GPSC_result gpsc_drv_init_comm_ind (T_GPSC_comm_config *comm_config);
extern T_GPSC_result gpsc_drv_transmit_data_ind (U8 *Mem, U16 num_bytes);
extern T_GPSC_result gpsc_drv_receive_data_req (U8 *Mem, U16 num_bytes);
extern T_GPSC_result gpsc_drv_discrete_control_ind (U8 discrete_control_bitmap);
extern T_GPSC_result gpsc_drv_discrete_control_res (T_GPSC_result result);
extern T_GPSC_result gpsc_drv_interrupt_received_req (void);
extern T_GPSC_result gpsc_os_timer_expired_res (U32 timerid);
extern T_GPSC_file_result gpsc_os_file_read_ind (U8 fileid, U8 *Mem, U16 num_bytes);
extern T_GPSC_file_result gpsc_os_file_write_ind (U8 fileid, U8 *Mem, U16 num_bytes);
extern T_GPSC_file_result gpsc_os_file_delete_ind (U8 fileid);
extern S32 gpsc_os_patch_open_ind (void);
extern S32 gpsc_os_patch_read_ind (U8 *Mem, U16 num_bytes);
extern void gpsc_os_patch_close_ind (void);
extern void gpsc_os_diagnostic_ind (T_GPSC_diag_level diag_level, S8 *diag_string);
extern void gpsc_os_fatal_error_ind (T_GPSC_result result);
extern T_GPSC_result gpsc_os_timer_start_ind (U8 timerid, U32 expiry_time);
extern T_GPSC_result gpsc_os_timer_stop_ind (U8 timerid);
extern U8 *gpsc_os_malloc_ind (U16 mem_size);
extern T_GPSC_result gpsc_os_free_ind (U8 *Mem);
extern U32 gpsc_os_get_current_time_ind (void);
extern T_GPSC_result gpsc_app_prodlinetest_req (TNAVC_plt *prodline_test_req_params);
extern void gpsc_app_prodlinetest_ind (T_GPSC_prodtest_response *prodtest_response);
extern T_GPSC_result gpsc_app_inject_calib_control_req(T_GPSC_calib_type CalibrationType);
extern void gpsc_app_calib_control_ind (T_GPSC_calib_response *calib_response);
extern T_GPSC_result gpsc_drv_apply_calib_timepulse_refclk_ind (U8 enable_disable_calib);
extern T_GPSC_result gpsc_drv_apply_calib_timepulse_refclk_res (T_GPSC_result result,U8 u_OperType);
extern T_GPSC_result gpsc_app_set_host_wakeup_params (TNAVC_SetHostWakeupParams *host_wakeup_params);
extern T_GPSC_result gpsc_app_set_apm_params (TNAVC_SetAPMParams *apm_params);
extern T_GPSC_result gpsc_app_set_sbas_params (TNAVC_SetSBASParams *sbas_params);
extern T_GPSC_result gpsc_app_set_motion_mask (TNAVC_SetMotionMask *motion_mask,McpU32 sessionid);
extern T_GPSC_result gpsc_app_enable_kalman_filter (TNAVC_EnableKalmanFilter *enable_kalman_filter);
extern T_GPSC_result gpsc_app_gps_ver_req (void);
extern T_GPSC_result gpsc_app_nvs_file_req (void);
extern T_GPSC_result gpsc_app_gps_ver_ind (T_GPSC_gps_ver_resp_params *gps_ver_resp_params);
extern T_GPSC_result gpsc_app_blf_status_ind (TNAVC_BlfStatusReport *p_blfStatusReport);
extern T_GPSC_result gpsc_app_alm_assist_complete_ind (void);
extern T_GPSC_result gpsc_app_motion_mask_req (U32 geo_fence_session_id, T_GPSC_geo_fence_config_params *geo_fence_config_params );
extern T_GPSC_result gpsc_app_assist_database_req (T_GPSC_assistance_database_report *assistance_database_report);
extern T_GPSC_result gpsc_app_svstatus_req (T_GPSC_sv_status *sv_status);
extern T_GPSC_result gpsc_app_deinit_req (void);
extern void gpsc_app_assist_eph_ind(T_GPSC_ephemeris_assist  *ephemeris_assist, EmcpTaskId TaskID, McpU8* QueueID);
extern void gpsc_app_assist_gpstime_ind(T_GPSC_time_assist  *gpstime_assist, EmcpTaskId TaskID, McpU8* QueueID);
extern T_GPSC_result gpsc_app_assist_time_ind (void);
extern T_GPSC_result gpsc_get_wishlist(T_GPSC_wishlist_params* p_gpsc_wishlist_params);
extern T_GPSC_result gpsc_app_gps_sleep (void);
extern T_GPSC_result gpsc_app_gps_wakeup (void);
extern T_GPSC_result gpsc_app_inj_tcxo_req (gpsc_inject_freq_est_type*  p_zInjectFreqEst);
extern T_GPSC_result gpsc_app_pos_rep_ext_req (void);
extern T_GPSC_result gpsc_app_gps_sleep_res(void);
extern T_GPSC_result gpsc_app_gps_wakeup_res(void);
extern T_GPSC_result gpsc_app_assist_delete_ind(void);
extern T_GPSC_result gpsc_app_inj_tcxo_ind(void);
extern T_GPSC_result gpsc_app_blf_config_req(U8 blf_en_flg, U32 blf_fix_count);
extern T_GPSC_result gpsc_app_blf_sts_req();
extern T_GPSC_result gpsc_app_blf_buff_dump_req();
extern void gpsc_app_wifi_pos_update(TNAVC_wifiPosition *p_wifiPosition);
extern void gpsc_app_gps_wifi_blend_pos_ind(TNAVC_gpsWifiBlendReport *p_zGpsWifiBlendReport);
T_GPSC_result Navc_Priority_Handler(TNAVC_reqLocFix* loc_fix_req_params);
#endif
