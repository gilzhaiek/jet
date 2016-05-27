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
 * FileName			:	gpsc_app_api.c
 *
 * Description     	:
 * This file contains the functions that are related to the application interface
 * of the GPSC SAP.
 *
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */


#include <time.h>
#include "gpsc_types.h"
#include "gpsc_consts.h"
#include "gpsc_msg.h"
#include "gpsc_data.h"
#include "gpsc_mgp_cfg.h"
#include "gpsc_mgp_tx.h"
#include "gpsc_sess.h"
#include "gpsc_init.h"
#include "gpsc_comm.h"
#include "gpsc_state.h"
#include "gpsc_sequence.h"
#include "gpsc_sap.h"
#include "gpsc_ext_api.h"
#include "gpsc_utils.h"
#include "gpsc_app_api.h"
#include "gpsc_timer_api.h"
#include "gpsc_geo_fence.h"
#include "gpsc_cd.h"
#include "gpsc_database.h"
#include "gpsc_version.h"
#include "gpsc_me_api.h"
#include "mcpf_mem.h"
#include "mcpf_report.h"
#include "mcp_hal_fs.h"
#include "mcp_hal_log.h"
#include "mcp_hal_types.h"
#include "version.h"
#include "navc_api.h"
#include "navc_api_pvt.h"
#include "navc_defs.h"
#ifdef ENABLE_INAV_ASSIST
#include "inavc_ext.h"
#endif
#include <math.h>
#include "nt_adapt.h"
#ifdef GPSC_DEBUG
#include <stdio.h>
#endif
#ifdef WIFI_ENABLE
#include "navc_wplServices.h"
#include "navc_hybridPosition.h"

#endif
#include "nav_log_msg.h"
#include "nav_log_cust_codes.h"


#include <utils/Log.h>
#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG "NAVD.GPSC_APP_API"    // our identification for logcat (adb logcat NAVD.GPSC_APP_API:V *:S)

// this bypasses all ultra convoluted assistance request logic and sends it to recon control port
// for implementation take a look at gpsc_mgp.c
extern T_GPSC_result recon_request_assistance(U16 mask);


#define VERDIRECT           0x01
#define BEARING             0x02
#define HORSPEED            0x04
#define VERSPEED            0x08
#define HORUNCERTSPEED      0x10
#define VERUNCERTSPEED      0x20

#define ULTS_GPSM_LATENCY 0

#define UTC_TS_MIN_LENGTH 13

#define YY_FORMAT_BASE_YEAR 2000
#define MAX_HOR_ACC_VALUE 0xFFFF
#define MAX_LOC_AGE_VALUE 0xFF
#define MAX_RSP_TIME_VALUE 0xFFFF
#define MAX_BUF_LENGTH 128
#define RDONLY  "r"
#define HOR_ACC "hor_acc"
#define LOC_AGE "loc_age"
#define RSP_TIME "response_time"

#define GPS_CONFIG_FILE_PATH "/system/etc/gps/config/GpsConfigFile.txt"


extern TNAVCD_Ctrl* pNavcCtrl;

#define pi 3.14159265358979

#define GPSC_DEFAULT_LOG_PORT 5555
S16 MSAssisted;
static McpU8 g_repConfig=1;
static McpU8 g_apmConfig=0;
static McpU16 g_hor_acc =MAX_HOR_ACC_VALUE;
static McpU8 g_loc_age =MAX_LOC_AGE_VALUE;
static McpU32 g_rsp_time =MAX_RSP_TIME_VALUE;

/* Implemented in navc_outEvt, sends confirmation of begin assistance command */
extern void RECON_gpsc_app_assist_begin_req_ind ();

/* RECON add-on: Indicator whether we are in the middle of Assistance Transaction so that 
   NAV barf logic does not fire second request while first on is in progress */
static int RECON_ASSIST_IN_PROGRESS = 0;

// GET-er AND SET-er: Needs MUTEX
int RECON_get_assist_in_progress ()
{
   return RECON_ASSIST_IN_PROGRESS;
}

void RECON_set_assist_in_progress (int state)
{
   RECON_ASSIST_IN_PROGRESS = state;
}

typedef enum
{
	REQUEST_SUPL_PGPS,
	REQUEST_SUPL,
	REQUEST_CTRL_PLANE,
	REQUEST_MAX
} T_GPSC_assist_src;
T_GPSC_result readGpsConfigFile();

T_GPSC_result stringCompare(const char *const p_string_1,
                                   const char *const p_string_2);

T_GPSC_assist_src gpsc_get_request_assist_src(gpsc_sess_cfg_type*	p_zSessConfig);

T_GPSC_result gpsc_app_send_assist_request (TNAVCD_Ctrl		*pNavc,
											U16 assist_bitmap_mandatory, 
											U16	 assist_bitmap_optional, 
											T_GPSC_nav_model_req_params *nav_model_req_params,
											my_Position *pos_info,
											T_GPSC_supl_qop *gps_qop, 
											T_GPSC_current_gps_time *tGpsTime,
											U32 assist_src);

static U8 pseudoRMS_formatter ( FLT f_pr_rms_meters );
static U8 gpsc_populate_navmodel(gpsc_ctrl_type *p_zGPSCControl,T_GPSC_nav_model_req_params *p_zNavModel);
static T_GPSC_result gpsc_app_assist_ttl_req (T_GPSC_ttl_assist_rep *p_ttl_assist);
#ifdef WIFI_DEMO
 void gpsc_app_gps_wifi_blend_pos_ind(TNAVC_gpsWifiBlendReport *p_zGpsWifiBlendReport);
#endif
void gpsc_populate_raw_pos
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_raw_position* p_raw_position,
 gpsc_sess_specific_cfg_type* p_zSessSpecificCfg
);

void gpsc_populate_rrlp_pos
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_prot_position* p_prot_position
);


void gpsc_populate_raw_meas
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_raw_measurement* p_raw_measurement,
 gpsc_sess_specific_cfg_type* p_zSessSpecificCfg
);

void gpsc_populate_rrlp_meas
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_prot_measurement* p_prot_measurement
);
void gpsc_populate_aa_assist
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_acquisition_assist* p_acquisition_assist
);
void gpsc_populate_eph_assist
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_ephemeris_assist*   p_ephemeris_assist
);
void gpsc_populate_iono_assist
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_iono_assist*        p_iono_assist
);
void gpsc_populate_alm_assist
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_almanac_assist*     p_almanac_assist
);
void gpsc_populate_utc_assist
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_utc_assist*         p_utc_assist
);
void gpsc_populate_dgps_assist
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_dgps_assist*        p_dgps_assist
);
void gpsc_populate_tow_assist
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_tow_assist*         p_tow_assist
);
void gpsc_populate_rti_assist
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_rti_assist*         p_rti_assist
);

void gpsc_populate_pos_assist
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_position_assist*        p_pos_assist
);


/* added to check the time availability  */
void gpsc_check_newtime_assist
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_time_assist*         p_newtime_assist
);

DBL gpsc_boundary_check_pos_ok
(gpsc_inject_pos_est_type*   p_zInjectPosEst,
 gpsc_db_type *p_zGPSCDatabase
);

void gpsc_store_msa_pos_in_db(TNAVC_posIndData* posInd);

McpU32 gpsc_get_get_gps_secs_from_utctimestamp(McpU8* utcTs,McpU8 len);

McpU32 gpsc_app_convert_utc2gps(McpU16 UTC []);

McpU16 * gpsc_app_convert_gps2cal( McpU32 gpsTime);

McpDBL gpsc_app_convert_gps2julianday( McpU32 gpsTime);

McpU16 *gpsc_app_convert_julianday2caldate(McpDBL  JD);

McpU8 * gpsc_app_convert_gps2utc(McpU32 gpsTime, McpU8 *p_len );


McpU8 gpsc_get_free_region_number(McpU32 aBitmap){

  int i=0;

  for (i=0;i<GEOFENCE_MAX_REGIONS;i++)
  {
	if (aBitmap & (0x01 << i))
	{
		continue;
	}
	else
		return i+1;
  }
 return i+1;
}

McpU8 gpsc_get_gf_report_config(gpsc_sess_cfg_type*    p_zSessConfig,gpsc_sess_specific_cfg_type* p_zCurSessConfig)
{
	gpsc_sess_specific_cfg_type*  p_zSessSpecificCfg=NULL;
	McpU8 rep_config=0xFF;
	STATUSMSG("gpsc_get_gf_report_config: Entering \n");
	if(NULL==p_zSessConfig)
		return rep_config;
	p_zSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;
	if(NULL==p_zSessSpecificCfg || (p_zSessSpecificCfg==p_zCurSessConfig) )
		return rep_config;
	rep_config = 0;
	while(p_zSessSpecificCfg != NULL)
	{
		STATUSMSG("gpsc_get_gf_report_config : GeofenceReportConfigration 0x%X\n",p_zSessSpecificCfg->GeofenceReportConfigration);
		if(p_zCurSessConfig!=p_zSessSpecificCfg)
			rep_config |=(p_zSessSpecificCfg->GeofenceReportConfigration&0x01);
		p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;
		STATUSMSG("gpsc_get_gf_report_config : 0x%X\n",rep_config);
	}
	STATUSMSG("gpsc_get_gf_report_config : Current Report Configuration should be %d\n",rep_config);
	STATUSMSG("gpsc_get_gf_report_config: Exiting \n");
	return rep_config;
}


McpU8 gpsc_app_get_apm_config(gpsc_sess_cfg_type*    p_zSessConfig,gpsc_sess_specific_cfg_type* p_zCurSessConfig)
{
	gpsc_sess_specific_cfg_type*  p_zSessSpecificCfg=NULL;
	McpU8 apm_config=0xFF;
	STATUSMSG("gpsc_app_get_apm_config: Entering \n");
	if(NULL==p_zSessConfig)
		return apm_config;
	p_zSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;
	if(NULL==p_zSessSpecificCfg || (p_zSessSpecificCfg==p_zCurSessConfig) )
	{
		STATUSMSG("gpsc_app_get_apm_config : return APM config  0x%X\n",apm_config);
		return apm_config;
	}
	apm_config = 0;
	STATUSMSG("gpsc_app_get_apm_config : before while rep_config  0x%X\n",apm_config);
	while(p_zSessSpecificCfg != NULL)
	{
		STATUSMSG("gpsc_app_get_apm_config : APM Configuration 0x%X\n",p_zSessSpecificCfg->u_ApmConfig);
		if(p_zCurSessConfig!=p_zSessSpecificCfg)
			apm_config |=(p_zSessSpecificCfg->u_ApmConfig & 0x01);
		p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;
		STATUSMSG("gpsc_app_get_apm_config : 0x%X\n",apm_config);
	}
	STATUSMSG("gpsc_app_get_apm_config : Current APM Configuration should be 0x%X\n",apm_config);
	STATUSMSG("gpsc_app_get_apm_config: Exiting \n");
	return apm_config;
}
/*
 ******************************************************************************
 * gpsc_app_loc_fix_req
 *
 * Function description:
 *
 * This function is the external interface that the appliction uses to start a
 * fix.
 *
 * Parameters:
 *
 *  TNAVC_reqLocFix - Location fix parameters
 *
 * Return value:
 *  Result
 *
 ******************************************************************************
*/


extern T_GPSC_result gpsc_app_loc_fix_req
(
 TNAVC_reqLocFix* loc_fix_req_params,
 McpU8 blf_flg
)
{
	gpsc_ctrl_type*	p_zGPSCControl;
	gpsc_state_type*	p_zGPSCState;
	gpsc_sess_cfg_type*	p_zSessConfig;
	gpsc_cfg_type*	p_zGPSCConfig;

	gpsc_smlc_assist_type* p_zSmlcAssist;
	gpsc_loc_fix_qop* p_zQop;


    gpsc_sess_specific_cfg_type*  p_zSessSpecificCfg=NULL;

	gpsc_event_cfg_type * p_zEventCfg;
        T_GPSC_result gpscRes;
	McpU8 rep_config = 0xFF;
	McpU8 apm_config = 0xFF;

    U16 w_CurrRepPer = 1;


	SAPENTER(gpsc_app_loc_fix_req);


	p_zGPSCControl = gp_zGPSCControl;
	p_zGPSCState  = p_zGPSCControl->p_zGPSCState;
	p_zSessConfig = p_zGPSCControl->p_zSessConfig;
	p_zGPSCConfig = p_zGPSCControl->p_zGPSCConfig;
	p_zSmlcAssist = p_zGPSCControl->p_zSmlcAssist;
	p_zEventCfg   = p_zGPSCControl->p_zEventCfg;
	p_zQop = &p_zSmlcAssist->z_Qop;

	gpsc_timer_stop(p_zGPSCControl, C_TIMER_WAIT_LOCREQ);

  /* following added for issue 206 - nul report */
    w_CurrRepPer = p_zSessConfig->w_AvgReportPeriod;
  p_zSessConfig->w_OldAvgRepPeriod = p_zSessConfig->w_AvgReportPeriod;

	if(p_zGPSCConfig->recv_min_rep_period == 500)
	{
	/* Before modifying the session config variables, check if this is a valid request */
	if(loc_fix_req_params->loc_fix_num_reports < 0 && loc_fix_req_params->loc_fix_period < 0)
		SAPLEAVE(gpsc_app_loc_fix_req,GPSC_LOC_FIX_INVALID_PERIOD)
	}
	else
	{
		/* Before modifying the session config variables, check if this is a valid request */
	    if(loc_fix_req_params->loc_fix_num_reports < 0  && loc_fix_req_params->loc_fix_period == 0)
		{
		STATUSMSG("ERROR: Fast TTFF not configured so report period of %d not supported\n",loc_fix_req_params->loc_fix_period);
		SAPLEAVE(gpsc_app_loc_fix_req,GPSC_LOC_FIX_INVALID_PERIOD)
		}
	}

	if( (loc_fix_req_params->loc_fix_num_reports < 0 ) && (loc_fix_req_params->loc_fix_max_ttff == 0xFFFF))
		SAPLEAVE(gpsc_app_loc_fix_req,GPSC_LOC_FIX_INVALID_FIX_MODE)

	if(p_zGPSCControl->p_zGPSCState->u_LocationRequestPending == TRUE)
	{
		p_zGPSCState->u_ReturnPending = FALSE;
        STATUSMSG("Status: Multiple session initiated");
	}
	else
	{
		p_zGPSCState->u_ReturnPending = TRUE;
	}

       /* Check if BLF is enabled for this session
   	and reconfigure the BLF session to get position fix with the requested
   	periodicity, but no buffering */
	if((!blf_flg) && (p_zSessConfig->u_BLF_State == C_BLF_STATE_ENABLED))
	{
		/* Disable BLF in hardware */
	        gpscRes = gpsc_app_blf_config_req(0, 0);
        	if (gpscRes < GPSC_SUCCESS)
	        {
        		STATUSMSG("ERROR:Configuring BLF failed.\n");
        		return gpscRes;
	        }
		/* Put the state variable to C_BLF_STATE_DORMANT, so that BLF in hardware
		   can be re-enabled when all normals sessions goes away */
		p_zSessConfig->u_BLF_State = C_BLF_STATE_DORMANT;
		STATUSMSG("INFO: BLF moved to DORMANT state.\n");
		/* Enable extended reports */
		p_zEventCfg->u_Ext_reports = 1;
	}

  /* check if session is already exist with same session id or new session requested */
 	p_zSessSpecificCfg = gpsc_session_get_node (p_zSessConfig, loc_fix_req_params->loc_fix_session_id);
	if(p_zSessSpecificCfg == NULL)
	{
    /* add new session */
    p_zSessSpecificCfg = gpsc_session_add_node (p_zSessConfig, loc_fix_req_params->loc_fix_session_id);

		if(p_zSessSpecificCfg == NULL)
		{
			ERRORMSG("Error : No Memory for new session node");
                        /* If we had already enabled BLF, disbale it */
                        if(blf_flg && (p_zSessConfig->u_BLF_State == C_BLF_STATE_ENABLED))
                        {
                                gpscRes = gpsc_app_blf_config_req(0, 0);
                                if (gpscRes < GPSC_SUCCESS)
                                {
                                        STATUSMSG("ERROR: Disabling BLF failed.\n");
                                }
                                p_zSessConfig->u_BLF_State = C_BLF_STATE_DISABLED;
                                p_zEventCfg->u_Ext_reports = 1;
                        }
			SAPLEAVE(gpsc_app_loc_fix_req,GPSC_FIX_ALREADY_IN_PROGRESS_FAIL)
		}
		/* increase count by one for multiple session, this is to know how many
		     location request are handled at a time  */
		p_zSessConfig->w_TotalNumSession++;
		if(loc_fix_req_params->loc_sess_type==GPSC_SESSTYPE_NI || loc_fix_req_params->loc_sess_type==GPSC_SESSTYPE_LP )
		{
			p_zSessSpecificCfg->GeofenceReportConfigration=1;
			p_zSessSpecificCfg->u_ApmConfig=0;


		}
		else
		{
			p_zSessSpecificCfg->GeofenceReportConfigration=g_repConfig;
			p_zSessSpecificCfg->u_ApmConfig=g_apmConfig;
		}
    /* for new location request, first fix will always have report period = 1s */
    p_zSessSpecificCfg->u_WaitFirstFix = TRUE;
	}
	else /* session already exists */
	{
		/* u_ExistingSession should be set only when its NOT C_SESSION_LOCAL.
		   Dummy session may be also go through same loop when it receives the
		   first location request.
		*/

          /*Overwriting existing session with good one*/
           p_zSessSpecificCfg->w_SessionTagId = loc_fix_req_params->loc_fix_session_id;

		/* Check if the ReportingPeriod or RTB is changed in old session, required to update Event Configuration and
		   u_ExistingSession > 1 indicates that there is change in bitmap or Report Period.*/
		if((p_zSessSpecificCfg->w_ResultTypeBitmap != loc_fix_req_params->loc_fix_result_type_bitmap) ||
		   (p_zSessSpecificCfg->w_ReportPeriod != loc_fix_req_params->loc_fix_period)
		   )
		{
			p_zSessConfig->u_ExistingSession++;
		}
	}

	rep_config=gpsc_get_gf_report_config(p_zSessConfig,p_zSessSpecificCfg);
	if(0==rep_config)
	{
		STATUSMSG("gpsc_app_loc_fix_req: GF session currently running, Normal session request..Change Report Config .\n");
		TNAVC_SetMotionMask motion_mask;
		memset(&motion_mask,0x00,sizeof(motion_mask));
		motion_mask.region_number=0;
		motion_mask.report_configuration=1;
		gpsc_app_set_motion_mask(&motion_mask,0xFFFFFFFF);
	}
	else
	{
	}
	apm_config=gpsc_app_get_apm_config(p_zSessConfig,p_zSessSpecificCfg);
	if(1==apm_config)
	{
		if(p_zGPSCConfig->apm_control == 1) {
		TNAVC_SetAPMParams apm_param;
		apm_param.apm_control=p_zGPSCConfig->apm_control;
		apm_param.search_mode=p_zGPSCConfig->search_mode;
		apm_param.saving_options=p_zGPSCConfig->saving_options;
		apm_param.power_save_qc=p_zGPSCConfig->power_save_qc;
		gpsc_app_control_apm_params(&apm_param,0xFFFFFFFF);

		}
		else {
		STATUSMSG("gpsc_app_loc_fix_req: APM session currently running, Normal session request without apm..Change APM Config .\n");
		TNAVC_SetAPMParams apm_param;
		apm_param.apm_control=0;
		apm_param.search_mode=1;
		apm_param.saving_options=0;
		apm_param.power_save_qc=80;
		gpsc_app_control_apm_params(&apm_param,0xFFFFFFFF);
	}
	}
       /* The new session node is successfully added now. Set the p_zSessConfig->u_BLF_Session flag
          if  this is a BLF enabled session */
        if(blf_flg)
       {
                p_zSessSpecificCfg->u_BLF_Flag = 1;
        }
        else
        {
                p_zSessSpecificCfg->u_BLF_Flag = 0;
        }

	p_zSessSpecificCfg->w_typeofrequest = GPSC_NORMAL_REQUEST;


	/*Initialize q_PeriodElapsed for this session*/
	p_zSessSpecificCfg->q_PeriodElapsed = 0;


	/* check if any changes in parameters of existing session for event configuration or AI2 */

		/* VFT and TTFF check */
		if(loc_fix_req_params->loc_fix_max_ttff ==0)
       	p_zSessSpecificCfg->q_ValidFixTimeout  = (U32) p_zGPSCConfig->default_max_ttff;
		else if(loc_fix_req_params->loc_fix_max_ttff == 0xFFFF)
		p_zSessSpecificCfg->q_ValidFixTimeout = (U32)0; //Dont timeout
	else
	    p_zSessSpecificCfg->q_ValidFixTimeout = (U32)((loc_fix_req_params->loc_fix_max_ttff*1000) - ULTS_GPSM_LATENCY);

		/* report period */
		p_zSessSpecificCfg->w_ReportPeriod = loc_fix_req_params->loc_fix_period;
	/* removed session common result type bitmap p_zSessConfig->w_ResultTypeBitmap
	   and replced with session specific result type bitmap for better utilization and
	   also for raw measurement sent which is only for perticular session */
	p_zSessSpecificCfg->w_ResultTypeBitmap = loc_fix_req_params->loc_fix_result_type_bitmap;
p_zSessSpecificCfg->call_type = loc_fix_req_params->call_type;

	/* if w_TotalNumFixesPerSession = 0, we go for infinite reports */
	p_zSessSpecificCfg->w_TotalNumFixesPerSession = loc_fix_req_params->loc_fix_num_reports;

	if(p_zSessSpecificCfg->w_TotalNumFixesPerSession)
     	   p_zSessSpecificCfg->w_FixCounter = p_zSessSpecificCfg->w_TotalNumFixesPerSession;
	else  /*TNFS=0 is for infinite session. Set Fixcounter=1 and after meas/fix
		    restore this value to 1*/
           p_zSessSpecificCfg->w_FixCounter=1;



	/* 1sec delay only if NMEA is requested and autonomous mode cold start.*/
	if(p_zSessSpecificCfg->w_ResultTypeBitmap & NMEA_MASK)
	  p_zSessSpecificCfg->u_OneSecDelay = TRUE;
	else /* 1sec delay initialized to FALSE for nmea. */
	  p_zSessSpecificCfg->u_OneSecDelay = FALSE;

	/* populate time out and Accuracy information */
	if( loc_fix_req_params->loc_fix_qop.horizontal_accuracy != C_ACCURACY_UNKNOWN &&
		loc_fix_req_params->loc_fix_qop.horizontal_accuracy != 0)
	{

	  /* in case of Multiple session , keep the tougher accuracy comared to other session*/
	  if( loc_fix_req_params->loc_fix_qop.horizontal_accuracy <= p_zQop->u_HorizontalAccuracy)
	  {
	      p_zQop->u_HorizontalAccuracy = loc_fix_req_params->loc_fix_qop.horizontal_accuracy;
		  p_zQop->u_TimeoutCepInjected = FALSE;
	  }

	  STATUSMSG("Status:Received SinglePE Parmas: Accuracy1 : %d",p_zQop->u_HorizontalAccuracy);
	 }
	else
		p_zQop->u_HorizontalAccuracy = C_ACCURACY_UNKNOWN;

	if( loc_fix_req_params->loc_fix_qop.vertical_accuracy != C_ACCURACY_UNKNOWN &&
		loc_fix_req_params->loc_fix_qop.vertical_accuracy != 0)
	{
		p_zQop->w_VerticalAccuracy=loc_fix_req_params->loc_fix_qop.vertical_accuracy;
	}
	else
		p_zQop->w_VerticalAccuracy = C_ACCURACY_UNKNOWN;

	if(loc_fix_req_params->loc_fix_qop.max_response_time != C_TIMEOUT_UNKNOWN &&
		loc_fix_req_params->loc_fix_qop.max_response_time != 0)
	{
		/* in case of Multiple session , keep the tougher timeout compared to other session*/
		if(loc_fix_req_params->loc_fix_qop.max_response_time <= p_zQop->w_MaxResponseTime)
		{
		   p_zQop->w_MaxResponseTime = loc_fix_req_params->loc_fix_qop.max_response_time;
		   p_zQop->u_TimeoutCepInjected = FALSE;
		}

		STATUSMSG("Status:Received SinglePE Parmas: Timeout2:%ds",p_zQop->w_MaxResponseTime);
	}
	else
		p_zQop->w_MaxResponseTime = C_TIMEOUT_UNKNOWN;

	STATUSMSG("loc_fix_req_params->loc_sess_type = %d ",loc_fix_req_params->loc_sess_type);

	if(loc_fix_req_params->loc_fix_qop.max_loc_age!=C_LOC_AGE_UNKNOWN)
		p_zQop->u_MaxLocationAge=loc_fix_req_params->loc_fix_qop.max_loc_age;
	else
		p_zQop->u_MaxLocationAge;

if(loc_fix_req_params->loc_sess_type != GPSC_SESSTYPE_LP)
{

if(p_zGPSCControl->p_zGPSCState->u_LocationRequestPending == FALSE)
{
	SESSLOGMSG("[%s]%s:%d, %s \n",get_utc_time(),SESS_START,nav_sess_getCount(0) ,"# Session starts");
		 SESSLOGMSG("[%s]%s:%d, AGPS-%d.%d %s \n",get_utc_time(),SESS_AGPS_SW_VERNO,nav_sess_getCount(0),SUPL_MAJ_VER_NUM,SUPL_MIN_VER_NUM,"#Version numbers");
	if(loc_fix_req_params->loc_sess_type==GPSC_SESSTYPE_NI)
	{
		if(loc_fix_req_params->loc_fix_mode==GPSC_FIXMODE_MSBASED)
			SESSLOGMSG("[%s]%s:%d, %s %s\n",get_utc_time(),SESS_OTA_MSG_TYPE,nav_sess_getCount(0),"SUPL INIT","#NI MSB");
		else if(loc_fix_req_params->loc_fix_mode==GPSC_FIXMODE_MSASSISTED)
			SESSLOGMSG("[%s]%s:%d, %s %s\n",get_utc_time(),SESS_OTA_MSG_TYPE,nav_sess_getCount(0),"SUPL INIT","#NI MSA");

			SESSLOGMSG("[%s]%s:%d, %d,%d,%d,%d,%d %s \n",get_utc_time(),SESS_QOP,nav_sess_getCount(0),p_zQop->u_HorizontalAccuracy,p_zQop->w_VerticalAccuracy, p_zQop->w_MaxResponseTime,1,0,"#QOP hor_acc,ver_acc,max_res_time,num_fixes,time_btw_fixes ");
	    }


	p_zGPSCControl->p_zGPSCState->u_GpscLocFixStartSystemTime=mcpf_getCurrentTime_InMilliSec(p_zGPSCControl->p_zSysHandlers->hMcpf);
	}else
	{
			STATUSMSG("p_zGPSCControl->p_zGPSCState->u_LocationRequestPending != FALSE ");

	}


}
else
{
		STATUSMSG("loc_fix_req_params->loc_sess_type !=GPSC_SESSTYPE_NI ");

}
p_zSessSpecificCfg->w_typeofsession=loc_fix_req_params->loc_sess_type;


	/* At this point location request is valid */
	switch(loc_fix_req_params->loc_fix_mode)
	{

		case GPSC_FIXMODE_AUTONOMOUS:
			{
				p_zSessSpecificCfg->u_PosCompEntity = C_MS_COMPUTED;
				p_zSessConfig->u_SmlcAssisPermit = C_SMLC_ASSIS_NOT_PERMITTED;
				/*always keep MS based if its autonomous mode*/
				p_zSessConfig->u_PosCompEntity = C_MS_COMPUTED;


				break;
			}



		case GPSC_FIXMODE_MSBASED:
			{
				p_zSessSpecificCfg->u_PosCompEntity = C_MS_COMPUTED;
				p_zSessConfig->u_SmlcAssisPermit = C_SMLC_ASSIS_PERMITTED;
				/*always keep MS based if atleast one session is MSB*/
				p_zSessConfig->u_PosCompEntity = C_MS_COMPUTED;
				break;
			}
		case GPSC_FIXMODE_AFLT:
        case GPSC_FIXMODE_ECID:
        case GPSC_FIXMODE_EOTD:
        case GPSC_FIXMODE_OTDOA:
		case GPSC_FIXMODE_AUTOSUPL:
			{
				p_zSessSpecificCfg->u_PosCompEntity = C_MS_COMPUTED;
				p_zSessConfig->u_SmlcAssisPermit = C_SMLC_ASSIS_PERMITTED;
				p_zSessConfig->u_PosCompEntity = C_MS_COMPUTED;
				break;
			}

 		
		case GPSC_FIXMODE_MSASSISTED:
			{
				p_zSessSpecificCfg->u_PosCompEntity = C_SMLC_COMPUTED;
				p_zSessConfig->u_SmlcAssisPermit = C_SMLC_ASSIS_PERMITTED;
				/*always keep MS assisted if first session is MSA*/
                if(p_zSessConfig->w_TotalNumSession==1)
                  p_zSessConfig->u_PosCompEntity = C_SMLC_COMPUTED;
				break;
			}

		default :
			{
				ERRORMSG("Invalid Fixmode Requested");
				gpsc_session_del_node(p_zSessConfig, p_zSessSpecificCfg->w_SessionTagId);
				SAPLEAVE(gpsc_app_loc_fix_req,GPSC_LOC_FIX_INVALID_FIX_MODE);

			}
	} /* end of switch(loc_fix_req_params->loc_fix_mode) */

	STATUSMSG("Session Information: \nNode=0x%x\nTotal No of Reports=%d",
		p_zSessSpecificCfg, p_zSessSpecificCfg->w_TotalNumFixesPerSession);
	STATUSMSG("Valid Fix Timeout=%d\t Position Computing Entity=%d\n",
		p_zSessSpecificCfg->q_ValidFixTimeout,p_zSessSpecificCfg->u_PosCompEntity);



	if(p_zSessConfig->w_TotalNumSession <= 1)
	{
		if((p_zSessConfig->q_PeriodicExpTimerVal) < (p_zSessSpecificCfg->q_ValidFixTimeout))
			p_zSessConfig->q_PeriodicExpTimerVal = p_zSessSpecificCfg->q_ValidFixTimeout;
		}

	/* if p_zSessConfig->w_AvgReportPeriod = 0, sub second periodic update is enabled */
	p_zSessConfig->u_ReConfigFlag = FALSE;

	if (p_zGPSCControl->p_zGPSCState->u_LocationRequestPending == TRUE && loc_fix_req_params->loc_sess_type==GPSC_SESSTYPE_NI)
	{
		STATUSMSG("gpsc_app_loc_fix_req - Multiple Sessions -  Requesting Clk Report to send NI response ");
		gpsc_sess_req_clock_to_get_assist(p_zGPSCControl);
	}

	/* set location request pending */
	p_zGPSCState->u_LocationRequestPending = TRUE;

	/* process location fix request */
	GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_LOCATION_START);

	if (p_zGPSCState->u_ReturnPending)
	   SAPLEAVE(gpsc_app_loc_fix_req, GPSC_PENDING)
	else
		SAPLEAVE(gpsc_app_loc_fix_req, GPSC_SUCCESS)

}



/*
 ******************************************************************************
 * gpsc_app_inject_new_timeout_info
 *
 * Function description:
 *
 * This function is the external interface that the application uses to instructs
 * the GPS to with the new timeout info
 *
 * Parameters:
 *
 * None
 *
 * Return value:
 *  T_GPSC_result : Result
 *
 ******************************************************************************
*/

void gpsc_app_inject_new_timeout(
	TNAVC_reqLocFix* loc_fix_req_params)
{
	gpsc_ctrl_type*	p_zGPSCControl;
	gpsc_loc_fix_qop* p_zQop;
	gpsc_smlc_assist_type* p_zSmlcAssist;


	p_zGPSCControl = gp_zGPSCControl;
	gpsc_sess_cfg_type* p_zSessConfig;
	gpsc_cfg_type*  p_zGPSCConfig;
	gpsc_sess_specific_cfg_type*  p_zSessSpecificCfg=NULL;
	//gpsc_db_gps_clk_type*  p_DBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;  /* Klocwork Changes */
	//gpsc_state_type*		p_zGPSCState    = p_zGPSCControl->p_zGPSCState;  /* Klocwork Changes */


	p_zSmlcAssist = p_zGPSCControl->p_zSmlcAssist;
	p_zQop = &p_zSmlcAssist->z_Qop;

	p_zSessConfig = p_zGPSCControl->p_zSessConfig;
	p_zGPSCConfig = p_zGPSCControl->p_zGPSCConfig;


	/* populate Accuracy information */
	if( loc_fix_req_params->loc_fix_qop.horizontal_accuracy == C_ACCURACY_UNKNOWN ||
							loc_fix_req_params->loc_fix_qop.horizontal_accuracy == 0)
	{
		p_zQop->u_HorizontalAccuracy = C_ACCURACY_UNKNOWN;
	}
	else
	{
		/* in case of Multiple session , keep the tougher accuracy comared to other session*/
		if( loc_fix_req_params->loc_fix_qop.horizontal_accuracy <= p_zQop->u_HorizontalAccuracy)
		{
			p_zQop->u_HorizontalAccuracy = loc_fix_req_params->loc_fix_qop.horizontal_accuracy;
			p_zQop->u_TimeoutCepInjected = FALSE;
			STATUSMSG("gpsc_app_inject_new_timeout:Received new: Accuracy : %d",p_zQop->u_HorizontalAccuracy);
		}
		else
		{
			STATUSMSG("gpsc_app_inject_new_timeout: Using old: Accuracy : %d",p_zQop->u_HorizontalAccuracy);
		}
	}

	/* populate Time out information */
	if(loc_fix_req_params->loc_fix_qop.max_response_time == C_TIMEOUT_UNKNOWN ||
							loc_fix_req_params->loc_fix_qop.max_response_time == 0)
	{
			p_zQop->w_MaxResponseTime = C_TIMEOUT_UNKNOWN;
	}
	else
	{
		/* in case of Multiple session , keep the tougher timeout compared to other session*/
		if(loc_fix_req_params->loc_fix_qop.max_response_time <= p_zQop->w_MaxResponseTime)
		{
			p_zQop->w_MaxResponseTime = loc_fix_req_params->loc_fix_qop.max_response_time;
			p_zQop->u_TimeoutCepInjected = FALSE;
			STATUSMSG("gpsc_app_inject_new_timeout:Received new:  Timeout2:%ds",p_zQop->w_MaxResponseTime);
		}
		else
		{
			STATUSMSG("gpsc_app_inject_new_timeout: sing old: Timeout2:%ds",p_zQop->w_MaxResponseTime);
		}
	}

	p_zSessSpecificCfg = gpsc_session_get_node (p_zSessConfig, loc_fix_req_params->loc_fix_session_id);
	if(p_zSessSpecificCfg != NULL)
	{
		STATUSMSG("gpsc_app_inject_new_timeout: Valid Session Running, Updating Timeout:%ds",p_zSessSpecificCfg);
		/* VFT and TTFF check */
		if(loc_fix_req_params->loc_fix_qop.max_response_time ==0)
			p_zSessSpecificCfg->q_ValidFixTimeout  = (U32) p_zGPSCConfig->default_max_ttff;
		else if(loc_fix_req_params->loc_fix_qop.max_response_time == 0xFFFF)
			p_zSessSpecificCfg->q_ValidFixTimeout = (U32)0; //Dont timeout
		else
		{
			STATUSMSG("gpsc_app_inject_new_timeout: updating time in Session Config Timeout2:%ds",loc_fix_req_params->loc_fix_max_ttff*1000);
			p_zSessSpecificCfg->q_ValidFixTimeout = (U32)((loc_fix_req_params->loc_fix_qop.max_response_time*1000) - ULTS_GPSM_LATENCY);
		}
	}

	gpsc_mgp_tx_inject_timeout_cep_info(p_zGPSCControl, p_zQop);


	if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
	{
	  FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
	}
}


void gpsc_app_inject_fix_update_rate(	TNAVC_updateFixRate* loc_fix_update)
{
	gpsc_ctrl_type*	p_zGPSCControl;

	p_zGPSCControl = gp_zGPSCControl;
	gpsc_sess_cfg_type* p_zSessConfig;
	gpsc_sess_specific_cfg_type*  p_zSessSpecificCfg=NULL;

	p_zSessConfig = p_zGPSCControl->p_zSessConfig;

	STATUSMSG("gpsc_app_inject_fix_update_rate: Entering.");
	STATUSMSG("gpsc_app_inject_fix_update_rate: update_rate  [%d]",loc_fix_update->update_rate);
	STATUSMSG("gpsc_app_inject_fix_update_rate: loc_sessID   [0x%x]",loc_fix_update->loc_sessID);
	/* check if sessin is already exist with same session id or new session requested */
	p_zSessSpecificCfg = gpsc_session_get_node (p_zSessConfig, loc_fix_update->loc_sessID);
	if(p_zSessSpecificCfg != NULL)
	{
		STATUSMSG("gpsc_app_inject_fix_update_rate: Valid Session Running");
		p_zSessSpecificCfg->w_ReportPeriod = loc_fix_update->update_rate;
		p_zSessConfig->u_ReConfigFlag = TRUE;
		gpsc_sess_update_period(p_zGPSCControl, C_PERIOD_NOTSET);
		p_zSessConfig->u_ReConfigFlag = FALSE;
	}else
	{
		STATUSMSG("gpsc_app_inject_fix_update_rate: no valid session with loc_sessID [0x%X]",loc_fix_update->loc_sessID);
	}
	STATUSMSG("gpsc_app_inject_fix_update_rate: Exiting.");
}
/*
 ******************************************************************************
 * gpsc_app_gps_ready_req
 *
 * Function description:
 *
 * This function is the external interface that the application uses to instruct
 * the GPS to boot up and get ready for a fix. On completion the GPSC return a
 * gpsc_app_ready_req_cnf
 *
 * Parameters:
 *
 * None
 *
 * Return value:
 *  T_GPSC_result : Result
 *
 ******************************************************************************
*/
extern T_GPSC_result gpsc_app_gps_ready_req (void)
{
	gpsc_ctrl_type*	p_zGPSCControl;
	gpsc_state_type*	p_zGPSCState;
	gpsc_sv_steer_inject_type* p_zSvInject;


	SAPENTER(gpsc_app_gps_ready_req);

	p_zGPSCControl = gp_zGPSCControl;
	p_zGPSCState = p_zGPSCControl->p_zGPSCState;
	p_zSvInject = p_zGPSCControl->p_zSvInject;
	/* initialize sv steering inject count here */
	p_zSvInject->u_SvSteerInjectCount =0;

	p_zGPSCState->u_ReadyRequestPending = TRUE;

	/* start ready sequence */
	GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_READY_START);

	SAPLEAVE(gpsc_app_gps_ready_req,GPSC_PENDING)
}





/*
 ******************************************************************************
 * gpsc_parse_control_file
 *
 * Function description:
 *
 *This function parses the patch control file and stores the paths and filenames
 * in directories.
 *
 * Parameters:
 *
 * p_zGPSCControl : Pointer to GPSC Control structure.
 *
 * Return value:
 *   T_GPSC_result : Result from gpsc_app_assist_request_ind
 *
 ******************************************************************************
*/
T_GPSC_result gpsc_parse_control_file(TNavcSm *pNavcSm,gpsc_sys_handlers* p_GPSCSysHandlers, McpS8* s_PathFileName)
{

	//TNAVCD_Ctrl	*pNavc = (TNAVCD_Ctrl *)pNavcSm->hNavc;/* Klocwork Changes */
	handle_t 		*pMcpf = (handle_t)pNavcSm->hMcpf;
	McpS8 *pch, *pch1, *pch2;
	McpS8 inputBuffer[250] = {'\0'};
	McpFILE pFile = NULL;
	McpU8 res = GPSC_SUCCESS;

	MCPF_REPORT_INFORMATION(pMcpf, NAVC_MODULE_LOG, ("gpsc_parse_control_file"));


	// initilize the system handlers to defaults
	pFile = MCP_HAL_FS_fopen (s_PathFileName,"r");
	if (!pFile)
	{
		MCPF_REPORT_INFORMATION(pMcpf, NAVC_MODULE_LOG,("gpsc_parse_control_file: Failed to open Path Control File -%s",s_PathFileName));
		res =  GPSC_FAIL;
	}
	else
	{
		MCPF_REPORT_INFORMATION(pMcpf, NAVC_MODULE_LOG,("gpsc_parse_control_file: opened control file - %s",s_PathFileName));
	  	while(MCP_HAL_FS_fgets(inputBuffer, sizeof(inputBuffer), pFile)) // get the next line
		  {
				pch = MCP_HAL_STRING_strtok (inputBuffer," ,.-\n\t");
				  if ((pch != NULL)&&(*pch != '#'))	// ignore empty tokens and comments
				  {
						pch1 = MCP_HAL_STRING_strtok (NULL, " ,=)#(&^%$@!<>\n\r\t");  // get the  next token here, as all lines have more than one token

						if ((pch1 != NULL)&&(*pch1 != '#')) // ignore second tokens and commnets
						{
							//system files
							if (!MCP_HAL_STRING_StrCmp(pch, "GPSC_CONFIG_FILE"))
							{
								MCP_HAL_STRING_StrCpy(p_GPSCSysHandlers->uGpscConfigFile, (McpS8*)pch1);
								MCPF_REPORT_INFORMATION(pMcpf, NAVC_MODULE_LOG,("GPSC_CONFIG_FILE: %s",p_GPSCSysHandlers->uGpscConfigFile));
							}
							else if (!MCP_HAL_STRING_StrCmp(pch, "PATCH_FILE"))
							{
								MCP_HAL_STRING_StrCpy(p_GPSCSysHandlers->uPatchFile, (McpS8*)pch1);
								MCPF_REPORT_INFORMATION(pMcpf, NAVC_MODULE_LOG,("PATCH_FILE: %s",p_GPSCSysHandlers->uPatchFile));
							}
							else if (!MCP_HAL_STRING_StrCmp(pch, "AIDING_PATH"))
							{
								MCP_HAL_STRING_StrCpy(p_GPSCSysHandlers->uAidingPath, (McpS8*)pch1);
								MCPF_REPORT_INFORMATION(pMcpf, NAVC_MODULE_LOG,("AIDING_PATH: %s",p_GPSCSysHandlers->uAidingPath));
							}

							// Debug control
							else if (!MCP_HAL_STRING_StrCmp(pch, "LOGGER_CONTROL"))
							{
								McpU8 temp = MCP_HAL_STRING_AtoI((McpU8*)pch1);
								if (temp == 1)
									p_GPSCSysHandlers->uLogControl = NAVC_LOG_MODE_UDP;
								else if (temp == 2)
									p_GPSCSysHandlers->uLogControl = NAVC_LOG_MODE_NVM;
								else if (temp == 3)
									p_GPSCSysHandlers->uLogControl = NAVC_LOG_MODE_PLATFORM;
								else if (temp == 4)
									p_GPSCSysHandlers->uLogControl = NAVC_LOG_MODE_STDOUT;
								else
									p_GPSCSysHandlers->uLogControl = NAVC_LOG_MODE_NONE;

								MCPF_REPORT_INFORMATION(pMcpf, NAVC_MODULE_LOG,("LOGGER_CONTROL: 0x%x",temp));
							}

							// MAX_LOG_LINES
							else if (!MCP_HAL_STRING_StrCmp(pch, "MAX_LOG_LINES"))
							{
								p_GPSCSysHandlers->uMaxLogLines = (McpU32)MCP_HAL_STRING_strtol((McpU8*)pch1, NULL, 10);
								MCPF_REPORT_INFORMATION(pMcpf, NAVC_MODULE_LOG,("MAX_LOG_LINES:%d",p_GPSCSysHandlers->uMaxLogLines));
							}

							// MAX_NVM_FILES
							else if (!MCP_HAL_STRING_StrCmp(pch, "MAX_NVM_FILES"))
							{
								p_GPSCSysHandlers->uMaxNvmFiles = MCP_HAL_STRING_AtoI((McpU8*)pch1);
								MCPF_REPORT_INFORMATION(pMcpf, NAVC_MODULE_LOG,("MAX_NVM_FILES:%d",p_GPSCSysHandlers->uMaxNvmFiles));

							}

							else if (!MCP_HAL_STRING_StrCmp(pch, "SENSOR_CONTROL"))
							{
								McpU8 temp = MCP_HAL_STRING_AtoI((McpU8*)pch1);

								if (temp == 2)
									p_GPSCSysHandlers->uSensorDebugControl = NAVC_LOG_MODE_NVM;
								else
									p_GPSCSysHandlers->uSensorDebugControl = NAVC_LOG_MODE_NONE;
								MCPF_REPORT_INFORMATION(pMcpf, NAVC_MODULE_LOG,("SENSOR_CONTROL: 0x%x",temp));
							}
							// Session log control
							else if (!MCP_HAL_STRING_StrCmp(pch, "SESSION_LOG_CONTROL"))
							{
								McpU8 temp = MCP_HAL_STRING_AtoI((McpU8*)pch1);
								if (temp == 1)
									p_GPSCSysHandlers->uSessionLogControl = NAVC_LOG_MODE_NVM;
								else
									p_GPSCSysHandlers->uSessionLogControl = NAVC_LOG_MODE_NONE;

								MCPF_REPORT_INFORMATION(pMcpf, NAVC_MODULE_LOG,("sess_log:0x%x",temp));

							}

							// UDP port for debug
							else if (!MCP_HAL_STRING_StrCmp(pch, "DEBUG_UDP"))
							{
								pch2 = MCP_HAL_STRING_strtok (NULL, " ,=)#(&^%$@!<>\n\r\t");  // get the  next token, port number
								MCPF_REPORT_INFORMATION(pMcpf, NAVC_MODULE_LOG,("pch2: %s",pch2));

								if ((pch2 != NULL)&&(*pch2 != '#')) // ignore second tokens and commnets
								{
									MCP_HAL_STRING_StrCpy(p_GPSCSysHandlers->uUdpIp, (McpS8*)pch1);

									//MCP_HAL_STRING_StrCpy(p_GPSCSysHandlers->uUdpPort, (McpU8*)pch2);
									p_GPSCSysHandlers->uUdpPort = MCP_HAL_STRING_AtoI((McpU8*)pch2);

									if (0 == p_GPSCSysHandlers->uUdpPort)
									{
										MCPF_REPORT_INFORMATION(pMcpf, NAVC_MODULE_LOG,("CONFIGURE DEFAULT LOG PORT.."));
										p_GPSCSysHandlers->uUdpPort = GPSC_DEFAULT_LOG_PORT;
									}
									else if (5555 != p_GPSCSysHandlers->uUdpPort)
									{
										MCPF_REPORT_INFORMATION(pMcpf, NAVC_MODULE_LOG,("LOG PORT DIFFERENT FROM 5555"));
									}

									MCPF_REPORT_INFORMATION(pMcpf, NAVC_MODULE_LOG,("DEBUG_UDP: IP= %s, Port: %d",p_GPSCSysHandlers->uUdpIp, p_GPSCSysHandlers->uUdpPort ));
								}
								else
								{
									MCPF_REPORT_INFORMATION(pMcpf, NAVC_MODULE_LOG,("ERROR: pch2 = %s", pch2));
								}
							}

							// NVM files for debug
							else if (!MCP_HAL_STRING_StrCmp(pch, "LOGGER_NVM_FILE"))
							{
								MCP_HAL_STRING_StrCpy(p_GPSCSysHandlers->uLogNvmPath, (McpS8*)pch1);
								MCPF_REPORT_INFORMATION(pMcpf, NAVC_MODULE_LOG,("LOGGER_NVM_FILE: %s",p_GPSCSysHandlers->uLogNvmPath));
							}
							else if (!MCP_HAL_STRING_StrCmp(pch, "SENSOR_NVM_PATH"))
							{
								MCP_HAL_STRING_StrCpy(p_GPSCSysHandlers->uSensorDataNvmPath, (McpS8*)pch1);
								MCPF_REPORT_INFORMATION(pMcpf, NAVC_MODULE_LOG,("SENSOR_NVM_PATH: %s",p_GPSCSysHandlers->uSensorDataNvmPath));
							}
							else if (!MCP_HAL_STRING_StrCmp(pch, "SESSION_LOG_NVM_PATH"))
							{
								MCP_HAL_STRING_StrCpy(p_GPSCSysHandlers->uSessionLogNvmPath, (McpS8*)pch1);
								MCPF_REPORT_INFORMATION(pMcpf, NAVC_MODULE_LOG,("SESSION_LOG_NVM_PATH: %s",p_GPSCSysHandlers->uSessionLogNvmPath));
							}
							else
								MCPF_REPORT_INFORMATION(pMcpf, NAVC_MODULE_LOG,("Unidentified token; pch = %s\n", pch));
						}
						else
							MCPF_REPORT_INFORMATION(pMcpf, NAVC_MODULE_LOG,("ERROR: pch1 = %s", pch1));

				  }
		}

		MCP_HAL_FS_fclose (pFile);
		MCPF_REPORT_INFORMATION(pMcpf, NAVC_MODULE_LOG,("gpsc_parse_control_file: closing path config file"));

	  }


MCPF_REPORT_INFORMATION(pMcpf, NAVC_MODULE_LOG,("gpsc_parse_control_file: end"));
return res;
}


/*
 ******************************************************************************
 * gpsc_init_sensor_files
 *
 * Function description:
 *
 * Creates the ToSensor.dat and FromSensor.dat files
 *
 * Parameters:
 *
 *GPSC control structure
 *
 * Return value:
 *  T_GPSC_result : Result
 *
 ******************************************************************************
*/
T_GPSC_result gpsc_init_sensor_files (gpsc_ctrl_type* p_zGPSCControl)
{
	gpsc_sys_handlers* p_GPSCSysHandlers = p_zGPSCControl->p_zSysHandlers;


#ifdef ALL_AI2_COMM_IN_FILE
	McpU8 AllAI2FileName[150];
#else
	McpU8 ToFileName[150],FromFileName[150];
#endif

	McpU8 TimeTemp[5];
	McpU32 u_CurrentTime, u_TimeTemp;
	McpHalDateAndTime dateAndTimeStruct;
	McpU8 res = GPSC_SUCCESS;

	/* check if sensor file are already being used */
#ifdef ALL_AI2_COMM_IN_FILE
		if (p_zGPSCControl->p_zSysHandlers->uAllSensorAI2Fd != -1)
			return 0;
#else
	if ((p_zGPSCControl->p_zSysHandlers->uToSensorFd == -1) &&
		(p_zGPSCControl->p_zSysHandlers->uFromSensorFd == -1))
	{
   }
	else
		return 0;
#endif

#ifdef ALL_AI2_COMM_IN_FILE
		MCP_HAL_STRING_StrCpy (AllAI2FileName,(McpU8*)p_GPSCSysHandlers->uSensorDataNvmPath);
		MCP_HAL_STRING_StrCat (AllAI2FileName,"AllSensorAI2Comm_");
#else
			MCP_HAL_STRING_StrCpy (ToFileName,(McpU8*)p_GPSCSysHandlers->uSensorDataNvmPath);
			MCP_HAL_STRING_StrCpy (FromFileName,(McpU8*)p_GPSCSysHandlers->uSensorDataNvmPath );

			MCP_HAL_STRING_StrCat (ToFileName, "ToSensor_");
			MCP_HAL_STRING_StrCat (FromFileName, "FromSensor_");
#endif

			// u_CurrentTime = (McpU32)mcpf_getCurrentTime_inSec(pMcpf);
			u_CurrentTime = time(NULL);
		        mcpf_ExtractDateAndTime(u_CurrentTime,&dateAndTimeStruct );

			/*year*/
			u_TimeTemp = dateAndTimeStruct.year;
			MCP_HAL_STRING_ItoA(u_TimeTemp,TimeTemp);

#ifdef ALL_AI2_COMM_IN_FILE
		MCP_HAL_STRING_StrCat (AllAI2FileName, TimeTemp);
		MCP_HAL_STRING_StrCat (AllAI2FileName, "-");
#else
		MCP_HAL_STRING_StrCat (ToFileName, TimeTemp);
		MCP_HAL_STRING_StrCat (ToFileName, "-");
		MCP_HAL_STRING_StrCat (FromFileName, TimeTemp);
		MCP_HAL_STRING_StrCat (FromFileName, "-");
#endif

			/*month*/
			u_TimeTemp = dateAndTimeStruct.month;
			MCP_HAL_STRING_ItoA(u_TimeTemp,TimeTemp);
#ifdef ALL_AI2_COMM_IN_FILE
		MCP_HAL_STRING_StrCat (AllAI2FileName, TimeTemp);
		MCP_HAL_STRING_StrCat (AllAI2FileName, "-");
#else
		MCP_HAL_STRING_StrCat (ToFileName, TimeTemp);
		MCP_HAL_STRING_StrCat (ToFileName, "-");
		MCP_HAL_STRING_StrCat (FromFileName, TimeTemp);
		MCP_HAL_STRING_StrCat (FromFileName, "-");
#endif

			/*day*/
			u_TimeTemp = dateAndTimeStruct.day;
			MCP_HAL_STRING_ItoA(u_TimeTemp,TimeTemp);
#ifdef ALL_AI2_COMM_IN_FILE
		MCP_HAL_STRING_StrCat (AllAI2FileName, TimeTemp);
		MCP_HAL_STRING_StrCat (AllAI2FileName, "-");
#else
			MCP_HAL_STRING_StrCat (ToFileName, TimeTemp);
			MCP_HAL_STRING_StrCat (ToFileName, "_");
			MCP_HAL_STRING_StrCat (FromFileName, TimeTemp);
			MCP_HAL_STRING_StrCat (FromFileName, "_");
#endif

			/*hour*/
			u_TimeTemp = dateAndTimeStruct.hour;
			MCP_HAL_STRING_ItoA(u_TimeTemp,TimeTemp);
#ifdef ALL_AI2_COMM_IN_FILE
		MCP_HAL_STRING_StrCat (AllAI2FileName, TimeTemp);
		MCP_HAL_STRING_StrCat (AllAI2FileName, "-");
#else
		MCP_HAL_STRING_StrCat (ToFileName, TimeTemp);
		MCP_HAL_STRING_StrCat (ToFileName, "-");
		MCP_HAL_STRING_StrCat (FromFileName, TimeTemp);
		MCP_HAL_STRING_StrCat (FromFileName, "-");
#endif

			/*minute*/
			u_TimeTemp = dateAndTimeStruct.minute;
			MCP_HAL_STRING_ItoA(u_TimeTemp,TimeTemp);
#ifdef ALL_AI2_COMM_IN_FILE
		MCP_HAL_STRING_StrCat (AllAI2FileName, TimeTemp);
		MCP_HAL_STRING_StrCat (AllAI2FileName, "-");
#else
		MCP_HAL_STRING_StrCat (ToFileName, TimeTemp);
		MCP_HAL_STRING_StrCat (ToFileName, "-");
		MCP_HAL_STRING_StrCat (FromFileName, TimeTemp);
		MCP_HAL_STRING_StrCat (FromFileName, "-");
#endif

			/*second*/
			u_TimeTemp = dateAndTimeStruct.second;
			MCP_HAL_STRING_ItoA(u_TimeTemp,TimeTemp);
#ifdef ALL_AI2_COMM_IN_FILE
		MCP_HAL_STRING_StrCat (AllAI2FileName, TimeTemp);
		MCP_HAL_STRING_StrCat (AllAI2FileName, ".dat");

		if (mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *) AllAI2FileName,
			MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_WRONLY | MCP_HAL_FS_O_CREATE | MCP_HAL_FS_O_TRUNC,
			&p_zGPSCControl->p_zSysHandlers->uAllSensorAI2Fd) != RES_OK)
		{
			STATUSMSG("gpsc_app_init_req: Failed to open ToSensor File");
			p_zGPSCControl->p_zSysHandlers->uAllSensorAI2Fd = 0;
			MCP_StrCpyUtf8(p_zGPSCControl->p_zSysHandlers->uAllSensorAI2_path, AllAI2FileName);
		}
		else
		{
			STATUSMSG("Created '%s' to log sensor data",AllAI2FileName);
			res = GPSC_FAIL;
		}
#else
			MCP_HAL_STRING_StrCat (ToFileName, TimeTemp);
			MCP_HAL_STRING_StrCat (FromFileName, TimeTemp);

			MCP_HAL_STRING_StrCat (ToFileName, ".dat");
			MCP_HAL_STRING_StrCat (FromFileName, ".dat");


	if (mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *) ToFileName,
		MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_WRONLY | MCP_HAL_FS_O_CREATE | MCP_HAL_FS_O_TRUNC,
		&p_zGPSCControl->p_zSysHandlers->uToSensorFd) != RES_OK)
	{
		ALOGI("gpsc_app_init_req: Failed to open ToSensor File");
		p_zGPSCControl->p_zSysHandlers->uToSensorFd = 0;
		MCP_StrCpyUtf8(p_zGPSCControl->p_zSysHandlers->uToSensor_path, ToFileName);
	}
	else
	{
		/* Open up file permissions so that we can copy logs out of sdcard from MTP */
		fchmod(p_zGPSCControl->p_zSysHandlers->uToSensorFd, 0777);
		ALOGI("Created '%s' to log sensor data",ToFileName);
		res = GPSC_FAIL;
	}



	if (mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *) FromFileName,
		MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_WRONLY | MCP_HAL_FS_O_CREATE |MCP_HAL_FS_O_TRUNC,
		&p_zGPSCControl->p_zSysHandlers->uFromSensorFd) != RES_OK)
	{
		ALOGI("gpsc_app_init_req: Failed to open ToSensor File");
		p_zGPSCControl->p_zSysHandlers->uFromSensorFd = 0;
		MCP_StrCpyUtf8(p_zGPSCControl->p_zSysHandlers->uFromSensor_path, FromFileName);
	}
	else
	{
		/* Open up file permissions so that we can copy logs out of sdcard from MTP */
		fchmod(p_zGPSCControl->p_zSysHandlers->uFromSensorFd, 0777);
		ALOGI("Created '%s' to log sensor data",FromFileName);
		res = GPSC_FAIL;
	}
#endif

return res;
}



/*
 ******************************************************************************
 * gpsc_init_sensor_files
 *
 * Function description:
 *
 * Closes sensor files
 *
 * Parameters:
 *
 *GPSC control structure
 *
 * Return value:
 *  void
 *
 ******************************************************************************
*/
void gpsc_deinit_sensor_files (gpsc_ctrl_type *p_zGPSCControl)
{
	//gpsc_sys_handlers* p_GPSCSysHandlers = p_zGPSCControl->p_zSysHandlers;  /* Klocwork Changes */

	/* Close To/From Sensor */
#ifdef ALL_AI2_COMM_IN_FILE
	if(p_zGPSCControl->p_zSysHandlers->uAllSensorAI2Fd != -1)
		mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uAllSensorAI2Fd);
	p_zGPSCControl->p_zSysHandlers->uAllSensorAI2Fd = -1;
#else
	if(p_zGPSCControl->p_zSysHandlers->uToSensorFd != -1)
		mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uToSensorFd);

	if(p_zGPSCControl->p_zSysHandlers->uFromSensorFd != -1)
		mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uFromSensorFd);

	p_zGPSCControl->p_zSysHandlers->uToSensorFd = -1;
	p_zGPSCControl->p_zSysHandlers->uFromSensorFd = -1;
#endif
	return;
}


/*
 ******************************************************************************
 * gpsc_init_logging
 *
 * Function description:
 *
 * init the logging functionality
 *
 * Parameters:
 *
 *GPSC control structure
 *
 * Return value:
 *  void
 *
 ******************************************************************************
*/
void gpsc_init_logging (gpsc_ctrl_type *p_zGPSCControl, gpsc_sys_handlers* temp_sysHandler)
{
	//gpsc_sys_handlers* p_GPSCSysHandlers = p_zGPSCControl->p_zSysHandlers; /* Klocwork Changes */

	/*copy NVM paths */
	MCP_HAL_STRING_StrCpy (p_zGPSCControl->p_zSysHandlers->uPathCtrlFile,(McpU8*)temp_sysHandler->uPathCtrlFile);
	MCP_HAL_STRING_StrCpy (p_zGPSCControl->p_zSysHandlers->uGpscConfigFile,(McpU8*)temp_sysHandler->uGpscConfigFile);
	MCP_HAL_STRING_StrCpy (p_zGPSCControl->p_zSysHandlers->uPatchFile,(McpU8*)temp_sysHandler->uPatchFile);
	MCP_HAL_STRING_StrCpy (p_zGPSCControl->p_zSysHandlers->uAidingPath,(McpU8*)temp_sysHandler->uAidingPath);
	MCP_HAL_STRING_StrCpy (p_zGPSCControl->p_zSysHandlers->uSensorDataNvmPath,(McpU8*)temp_sysHandler->uSensorDataNvmPath);
	MCP_HAL_STRING_StrCpy (p_zGPSCControl->p_zSysHandlers->uSessionLogNvmPath,(McpU8*)temp_sysHandler->uSessionLogNvmPath);
	MCP_HAL_STRING_StrCpy (p_zGPSCControl->p_zSysHandlers->uLogNvmPath,(McpU8*)temp_sysHandler->uLogNvmPath);
	MCP_HAL_STRING_StrCpy (p_zGPSCControl->p_zSysHandlers->uUdpIp,(McpU8*)temp_sysHandler->uUdpIp);
	p_zGPSCControl->p_zSysHandlers->uUdpPort=temp_sysHandler->uUdpPort;
	p_zGPSCControl->p_zSysHandlers->uMaxLogLines=temp_sysHandler->uMaxLogLines;
	p_zGPSCControl->p_zSysHandlers->uMaxNvmFiles=temp_sysHandler->uMaxNvmFiles;
#ifdef ALL_AI2_COMM_IN_FILE
	p_zGPSCControl->p_zSysHandlers->uAllSensorAI2Fd = -1;
#else
	p_zGPSCControl->p_zSysHandlers->uToSensorFd = -1;
	p_zGPSCControl->p_zSysHandlers->uFromSensorFd = -1;
#endif
	/*   create path directories  */
	mcpf_create_dir_path(p_zGPSCControl->p_zSysHandlers->uAidingPath,        0777);
	mcpf_create_dir_path(p_zGPSCControl->p_zSysHandlers->uSensorDataNvmPath, 0777);
	mcpf_create_dir_path(p_zGPSCControl->p_zSysHandlers->uSessionLogNvmPath, 0777);
	mcpf_create_dir_path(p_zGPSCControl->p_zSysHandlers->uLogNvmPath,        0777);
	   if (chmod("/data/gps", 0777) < 0) {
	       STATUSMSG("Error changing permissions of /data/gps to 0777:");
    	}
	   if (chmod("/data/gps/aiding", 0777) < 0) {
	       STATUSMSG("Error changing permissions of /data/gps to 0777:");
    	}
	gpsc_log_ctrl (p_zGPSCControl,
					temp_sysHandler->uLogControl,
					temp_sysHandler->uSensorDebugControl,
					temp_sysHandler->uSessionLogControl);

	return;
}






/*
 ******************************************************************************
 * gpsc_log_ctrl
 *
 * Function description:
 *
 * init the logging functionality
 *
 * Parameters:
 *
 * GPSC control structure
 *
 * Return value:
 *  void
 *
 ******************************************************************************
*/
void gpsc_log_ctrl (gpsc_ctrl_type* p_zGPSCControl, McpU8 navc_log_ctrl, McpU8 sensor_log_ctrl, McpU8 session_log_ctrl)
{
/* This routine has bug as switches that govern AI2 To/From Sensor File are not updated properly
   I am not wasting time figuring where / how in this horror this should be done; instead
   I have simply commented out this feature in "gpsc_drv_api.c" */

		STATUSMSG("gpsc_log_ctrl: LogMode: %x, Sensor: %x Session: %x", navc_log_ctrl, sensor_log_ctrl,session_log_ctrl);

		/* disable logging */
		if (navc_log_ctrl == NAVC_LOG_MODE_NONE)
		{
			MCP_HAL_LOG_DisableLogging();
			MCP_HAL_LOG_DisableUdpLogging();
			MCP_HAL_LOG_DisableFileLogging();
			MCP_HAL_LOG_DisableLogToAndroid();
			MCP_HAL_LOG_DisableStdoutLogging();
		}

		/* UDP logging */
		if (navc_log_ctrl & NAVC_LOG_MODE_UDP)
			MCP_HAL_LOG_EnableUdpLogging(p_zGPSCControl->p_zSysHandlers->uUdpIp,
											p_zGPSCControl->p_zSysHandlers->uUdpPort);
		else
			MCP_HAL_LOG_DisableUdpLogging();


		/* NVM logging */
		if (navc_log_ctrl & NAVC_LOG_MODE_NVM)
			MCP_HAL_LOG_EnableFileLogging(p_zGPSCControl->p_zSysHandlers->uLogNvmPath,
											p_zGPSCControl->p_zSysHandlers->uMaxNvmFiles,
											p_zGPSCControl->p_zSysHandlers->uMaxLogLines);
		else
			MCP_HAL_LOG_DisableFileLogging();

		/* Platform logging */
		if (navc_log_ctrl & NAVC_LOG_MODE_PLATFORM)
			MCP_HAL_LOG_EnableLogToAndroid("NAVC");
		else
			MCP_HAL_LOG_DisableLogToAndroid();


		/* STD out logging */
		if (navc_log_ctrl & NAVC_LOG_MODE_STDOUT)
			MCP_HAL_LOG_EnableStdoutLogging();
		else
			MCP_HAL_LOG_DisableStdoutLogging();

		/* Session logs */
		if (session_log_ctrl & NAVC_LOG_MODE_NVM)
		{
			nav_sess_log_init( p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uSessionLogNvmPath );
		}
		else
		{
			nav_sess_log_deinit(p_zGPSCControl->p_zSysHandlers->hMcpf);
		}


		/* Sensor logs. We don't need this barf for RECON */
                /* Temporarily re-enable these logs for Alpha build.
                   Make sure we remove for production */
		if (sensor_log_ctrl & NAVC_LOG_MODE_NVM)
			gpsc_init_sensor_files (p_zGPSCControl);
		else
			gpsc_deinit_sensor_files (p_zGPSCControl);


	return;
}

/*
 ******************************************************************************
 * gpsc_app_init_req
 *
 * Function description:
 *
 * This function is the external interface needs to be called before any GPSC
 * function calls. This function causes the GPSC to request and initialise
 * its memory.
 *
 * Parameters:
 *
 * None
 *
 * Return value:
 *  T_GPSC_result : Result
 *
 ******************************************************************************
*/
extern T_GPSC_result gpsc_app_init_req (void *sysHandlers, void **hGpsc)
{
	gpsc_ctrl_type*    p_zGPSCControl;
	gpsc_sys_handlers* p_sysHandlers = (gpsc_sys_handlers *)sysHandlers;

	/* added for version display */
	gpsc_global_memory_type* p_GPSCMemory;

	MCPF_REPORT_INFORMATION(p_sysHandlers->hMcpf, NAVC_MODULE_LOG, ("Enter gpsc_app_init_req"));

	/* if memory is not allocated then request for memory */
	if (gp_zGPSCControl ==NULL && gpsc_memory_init(p_sysHandlers->hMcpf)!=TRUE)
	{
		MCPF_REPORT_ERROR(p_sysHandlers->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_init_req: Failed to init memory"));
		return GPSC_MEM_ALLOCATION_FAIL;
	}


	p_zGPSCControl = gp_zGPSCControl;
	if ( p_zGPSCControl == NULL)
    {
	   MCPF_REPORT_ERROR(p_sysHandlers->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_init_req: Failed to initp_GPSCMemory"));
	   return GPSC_MEM_ALLOCATION_FAIL;
	}

	if (hGpsc != NULL)
		*hGpsc = p_zGPSCControl;

	/* Init System Handlers */
	p_zGPSCControl->p_zSysHandlers->hMcpf = p_sysHandlers->hMcpf;
	p_zGPSCControl->p_zSysHandlers->hNavcCtrl = p_sysHandlers->hNavcCtrl;
	p_zGPSCControl->p_zSysHandlers->GpscConfigFile = p_sysHandlers->GpscConfigFile;

	/* Initilize logging */
	gpsc_init_logging(p_zGPSCControl, p_sysHandlers);

	/* Added to display the version */
	STATUSMSG("SW Ver: %s,   NaviLink Ver: %s", SW_VERSION_STR, NAVC_VERSION_STR);

	p_zGPSCControl->p_zGpscSm = GpscSmCreate(p_zGPSCControl, p_sysHandlers);
	if (NULL == p_zGPSCControl->p_zGpscSm)
		{
		/* release gpsc memory */
		if(mcpf_mem_free(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_GPSCMemory) != RES_OK )
		    gpsc_os_fatal_error_ind(GPSC_MEM_FREE_ERROR);

		gpsc_diagnostic(GPSC_DIAG_GPSM_INTERACTION,
			"SAP: gpsc_app_init_req GPSC State Machine Memory Allocation Fail %d", GPSC_MEM_ALLOCATION_FAIL);

		return GPSC_MEM_ALLOCATION_FAIL;
		}

	p_zGPSCControl->p_zGPSCState->u_InitRequestPending = TRUE;


	/* GPSC initialization - Send INIT event */
	GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_INIT);

	p_zGPSCControl->p_zGpscSm->hGpsc = p_zGPSCControl;

	/* GPSC initialized and send confirmation to NAVC and GPSM */
	if (E_GPSC_SEQ_INIT == p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq)
   {
		p_zGPSCControl->p_zGPSCState->u_InitRequestPending = FALSE;
	}
	else
	{
		SAPLEAVE(gpsc_app_init_req, GPSC_FAIL);
	}


#ifdef MCP_STK_ENABLE
		/* Baud Rate is not set from GPS, just indicate 115200 for notification */
		p_zGPSCControl->p_zGPSCConfig->comm_config.comm_config_union.comm_config_uart.uart_baud_rate = GPSC_BAUD_115200;
#endif

	SAPLEAVE(gpsc_app_init_req, GPSC_SUCCESS)
}

U16 gpsc_get_min_toe_diff(gpsc_ctrl_type*  p_zGPSCControl)
{
	U8 u_i, good_eph_count = 0;
	U16 toe_min = 0xFFFF; // default maximum value

	gpsc_db_type* p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_db_gps_clk_type*    p_zDBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;
	gpsc_db_eph_type* p_zDBEphemeris = 0;
	gpsc_db_sv_dir_type*     p_zDBSvDirection = &p_zGPSCDatabase->z_DBSvDirection;
	p_zDBEphemeris = &p_zGPSCDatabase->z_DBEphemeris[0];
	U16 gps_sec        = (p_zGPSCDatabase->z_DBGpsClock.q_GpsMs)/1000;
	me_SvDirection*        p_zRawSvDirection = 0;
	pe_RawSF1Param*        p_RawSF1Param = 0;
	DBL d_data_age_sec = 0.0;

	DBL d_CurrentAccGpsSec = FullGpsMs(p_zDBGpsClock->w_GpsWeek,
				p_zDBGpsClock->q_GpsMs) * 0.001;

	for (u_i = 0; u_i < N_SV; u_i++, p_zDBEphemeris++ )
	{
		p_zRawSvDirection = &p_zDBSvDirection->z_RawSvDirection[u_i];
		p_zDBEphemeris = &p_zGPSCDatabase->z_DBEphemeris[u_i];
		p_RawSF1Param = &p_zDBEphemeris->z_RawSF1Param;

		if ((p_zDBEphemeris->z_RawEphemeris.u_Prn != 0))
		{
			d_data_age_sec = d_CurrentAccGpsSec +
							p_zDBGpsClock->f_TimeUnc * 0.001f -
							p_RawSF1Param->d_FullToe;
			d_data_age_sec = (FLT)ceil (d_data_age_sec);
			
			STATUSMSG("+++ %s: SvId=%d, eph age=%f minutes +++\n", __FUNCTION__, u_i+1, d_data_age_sec/60.0f);

			if (( (d_data_age_sec > 0) && ( d_data_age_sec < C_GOOD_EPH_AGE_SEC ) ) ||
								( (d_data_age_sec < 0) && ( (0 - d_data_age_sec) < C_GOOD_EPH_AGE_SEC ) ) )
			{
					STATUSMSG("+++ %s: SvId = %d, has good eph +++\n", __FUNCTION__, u_i+1);
					good_eph_count++;

					if ((U16)((2*60*60 - d_data_age_sec) < toe_min))
						toe_min = (U16)(2*60*60 - d_data_age_sec);

					STATUSMSG("+++ %s: d_data_age_sec = %d toe_min %d +++\n", __FUNCTION__, (U16)(2*60*60 - d_data_age_sec), toe_min);
			}
			else
			{
				STATUSMSG("+++ %s: Ephemeris Required: SvId=%d, elev=%f, age =%f old +++\n", __FUNCTION__, u_i+1,
				   p_zRawSvDirection->b_Elev * C_LSB_ELEV_MASK, d_data_age_sec/60.0f);
			}
		}
	}

	ALOGI("+++ %s: good_eph_count %d, toe_min %d +++\n", __FUNCTION__, good_eph_count, toe_min);
	return (good_eph_count < 4) ? 0 : toe_min;

}

T_GPSC_assist_src gpsc_get_request_assist_src(gpsc_sess_cfg_type*	p_zSessConfig)
{
	gpsc_sess_specific_cfg_type*  p_zSessSpecificCfg=NULL;

	p_zSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;

	T_GPSC_assist_src assist_src = REQUEST_SUPL_PGPS;

	STATUSMSG("Enter gpsc_get_request_assist_src");

	while(p_zSessSpecificCfg != NULL) {

		if((p_zSessSpecificCfg->call_type == NAVC_E911) ||
			(p_zSessSpecificCfg->call_type == NAVC_CP_MTLR) ||
			(p_zSessSpecificCfg->call_type == NAVC_CP_NILR))
		{
			assist_src = REQUEST_CTRL_PLANE;
			break;
		}
		else 
		{
			if((p_zSessSpecificCfg->call_type == NAVC_SUPL_MT_POSITIONING) ||
				(p_zSessSpecificCfg->call_type == NAVC_SUPL_MT_NO_POSITIONING))
			{
				assist_src = REQUEST_SUPL;
			}
		}

		p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;
	}

	STATUSMSG("Exit gpsc_get_request_assist_src: Request Source %d", assist_src);
	return assist_src;
}


T_GPSC_result gpsc_app_send_assist_request (TNAVCD_Ctrl		*pNavc,
											U16 assist_bitmap_mandatory, 
											U16	 assist_bitmap_optional, 
											T_GPSC_nav_model_req_params *nav_model_req_params,
											my_Position *pos_info,
											T_GPSC_supl_qop *gps_qop, 
											T_GPSC_current_gps_time *tGpsTime,
											U32 assist_src)
{
    T_GPSC_result return_val = GPSC_FAIL;

    if (assist_src == CUSTOM_AGPS_PROVIDER1)
    {
            return_val = recon_request_assistance (assist_bitmap_mandatory);
    }
    else
    {
        if ((pNavc->tAssistSrcDb[assist_src].uAssistSrcType == assist_src) &&
	    (pNavc->tAssistSrcDb[assist_src].uAssistSrcPriority != 0))
        {
            return_val = gpsc_app_assist_request_ind (assist_bitmap_mandatory,
                                                      assist_bitmap_optional, nav_model_req_params,pos_info,gps_qop,tGpsTime,
                                                      pNavc->tAssistSrcDb[assist_src].uAssistProviderTaskId,
                                                      pNavc->tAssistSrcDb[assist_src].uAssistProviderQueueId);
        }
        else
        {
        	ALOGI("Assistance Provider %d not registered", assist_src);
        }
    }

    return return_val;    
}


// This is so unbelievably stupid, re-definition of standard C library
// function. But do NOT change it, everything will blow apart
T_GPSC_result stringCompare(const char* const p_string_1,
                            const char* const p_string_2)
{
    if ( strcmp(p_string_1, p_string_2) == 0)
    {
        return GPSC_SUCCESS;
    }

    return GPSC_FAIL;

}


T_GPSC_result readGpsConfigFile()
{
    T_GPSC_result retVal = GPSC_SUCCESS;
    FILE *fp = NULL;

    char a_inputBuffer[MAX_BUF_LENGTH] = {'\0'};
    char *p_token = NULL;

    LOGD(" readConfigFile: Entering \n");

    fp = fopen(GPS_CONFIG_FILE_PATH, RDONLY);
    if (NULL == fp)
    {
        LOGD(" readConfigFile: fopen FAILED !!! \n");
        return GPSC_FAIL;
    }

    while( (fgets(a_inputBuffer, sizeof(a_inputBuffer), fp) ) &&
           (stringCompare(a_inputBuffer, "\n") != GPSC_SUCCESS) )
    {
      // LOGD(" readConfigFile: a_inputBuffer = %s \n", a_inputBuffer);
       p_token = (char *)strtok(a_inputBuffer, ":");
       if ( NULL == p_token )
       {
           /* Continue with the next line. */
           continue;
       }
	   else if ((stringCompare(p_token, HOR_ACC) == GPSC_SUCCESS))
       {

            p_token = (char *) strtok(NULL, ":");
            if (NULL == p_token)
            {
                LOGD(" readConfigFile: strtok returned NULL !!! \n");
                fclose(fp);
                return GPSC_FAIL;
            }
            else
        	{
				g_hor_acc = atoi(p_token);
				if(g_hor_acc > MAX_HOR_ACC_VALUE)
					g_hor_acc = MAX_HOR_ACC_VALUE;
				LOGD(" readConfigFile: HOR_ACC : %d \n",g_hor_acc);
        	}

       }
	   else if ((stringCompare(p_token, LOC_AGE) == GPSC_SUCCESS))
       {

            p_token = (char *) strtok(NULL, ":");
            if (NULL == p_token)
            {
                LOGD(" readConfigFile: strtok returned NULL !!! \n");
                fclose(fp);
                return GPSC_FAIL;
            }
            else
        	{
				g_loc_age= atoi(p_token);
				if(g_loc_age>MAX_LOC_AGE_VALUE)
					g_loc_age = MAX_LOC_AGE_VALUE;
				LOGD(" readConfigFile: LOC_AGE : %d \n",g_loc_age);
        	}

       }
		else if ((stringCompare(p_token, RSP_TIME) == GPSC_SUCCESS))
       {

            p_token = (char *) strtok(NULL, ":");
            if (NULL == p_token)
            {
                LOGD(" readConfigFile: strtok returned NULL !!! \n");
                fclose(fp);
                return GPSC_FAIL;
            }
            else
        	{
				g_rsp_time = atoi(p_token);
				if(g_rsp_time > MAX_RSP_TIME_VALUE)
					g_rsp_time = MAX_RSP_TIME_VALUE;
				LOGD(" readConfigFile: RSP_TIME : %d \n",g_rsp_time);
        	}
       }
    }


    fclose(fp);
    LOGD(" readConfigFile: Exiting Successfully. \n");
    return retVal;
}

/*
 ******************************************************************************
 * gpsc_request_assistance
 *
 * Function description:
 *
 * This function is the internal function that makes a call to
 * gpsc_app_assist_request_ind and requests for more assistance data. This
 * assistance data is broken in two groups, optional and mandatory.
 *
 * Parameters:
 *
 * p_zGPSCControl : Pointer to GPSC Control structure.
 *
 * Return value:
 *   T_GPSC_result : Result from gpsc_app_assist_request_ind
 *
 ******************************************************************************
*/

T_GPSC_result gpsc_request_assistance(gpsc_ctrl_type* p_zGPSCControl)
{
  gpsc_assist_wishlist_type*  p_GpsAssisWishlist      = p_zGPSCControl->p_zGpsAssisWishlist;
  gpsc_cfg_type*	            p_zGPSCConfig           = p_zGPSCControl->p_zGPSCConfig;
  gpsc_sess_cfg_type*		   p_zSessConfig		      = p_zGPSCControl->p_zSessConfig;
  U16                         assist_bitmap_mandatory = 0;
  U16                         assist_type_optional    = 0;
  T_GPSC_current_gps_time     zGpsTime;
  T_GPSC_nav_model_req_params z_nav_model, *p_nav_model = NULL;
  gpsc_smlc_assist_type*	   p_zSmlcAssist	         = p_zGPSCControl->p_zSmlcAssist;
  gpsc_db_type*               p_zGPSCDatabase         = p_zGPSCControl->p_zGPSCDatabase;
  T_GPSC_result               return_val              = GPSC_SUCCESS;
  TNAVCD_Ctrl*                pNavc                   = (TNAVCD_Ctrl*)p_zGPSCControl->p_zSysHandlers->hNavcCtrl; 

  gpsc_db_pos_type*           p_DBPos                 = &p_zGPSCDatabase->z_DBPos;
  U16                         w_MandatoryMask         = 0x00;
  gpsc_loc_fix_qop*           p_zQop                  = &p_zSmlcAssist->z_Qop;

  // on top of this horror check if assistance is already in progress; this dumb thing sends 2nd request
  // in the middle of first one!
  if (RECON_get_assist_in_progress() == 1)
  {
     ALOGI("+++ %s: Assistance Transaction already in progress; you dumbheads can't ask for 2nd one until 1st finishes +++\n", __FUNCTION__);
     return GPSC_FAIL;
  }

  // This checks if # of assist attempts exceeded some arbitrary threshold, which in stupid; consider removing
  if (p_zSmlcAssist->u_NumAssistAttempt >= C_MAX_ASSIST_ATTEMPTS)
  {
	  ALOGI("+++ %s: Exceeded maximum number of Assistance Attempts (%d). Assist Request (%d) will not be sent +++\n",
         __FUNCTION__, C_MAX_ASSIST_ATTEMPTS, p_zSmlcAssist->u_NumAssistAttempt);

	  return GPSC_FAIL;
  } 

  if (p_zSessConfig->u_PosCompEntity == C_SMLC_COMPUTED)
	 w_MandatoryMask = p_zGPSCConfig->assist_bitmap_msassisted_mandatory_mask;
  else
    w_MandatoryMask = p_zGPSCConfig->assist_bitmap_msbased_mandatory_mask;

  assist_bitmap_mandatory = (U16)(p_GpsAssisWishlist->w_Wishlist & w_MandatoryMask);
  assist_type_optional = (U16)(0);

  if (p_GpsAssisWishlist->w_Wishlist & GPSC_REQ_NAV &&
	       gpsc_populate_navmodel(p_zGPSCControl, &z_nav_model) == TRUE )
  {
	  p_nav_model = &z_nav_model;
  }

  p_zSmlcAssist->q_AlmPresent = 0;
  p_zSmlcAssist->u_SvCounter = 0;
  p_zSmlcAssist->u_SvCounterSteer = 0;
  p_zSmlcAssist->u_nSvEph = 0;
  p_zSmlcAssist->u_InjVisible = FALSE;
  p_zSmlcAssist->q_VisiableBitmap = 0;
  p_zSmlcAssist->q_InjSvEph = 0;

  T_GPSC_supl_qop qop_to_supl;
  memset(&qop_to_supl, 0x00, sizeof(T_GPSC_supl_qop) );

  my_Position gpsPosInfo;
  memset (&gpsPosInfo, 0x00, sizeof(my_Position) );

  gpsPosInfo.pos_opt_bitmap = 0;

  T_GPSC_prot_position protPos;
  gpsc_populate_rrlp_pos(p_zGPSCControl, &protPos);

  if (readGpsConfigFile() != GPSC_SUCCESS)
     return GPSC_FAIL;

  if (protPos.prot_fix_result != GPSC_PROT_FIXRESULT_NOFIX)
  {
	   gpsc_report_pos_type  *p_zReportPos  = &p_zGPSCDatabase->z_DBPos.z_ReportPos;

	   U32 loc_age= gpsc_db_gps_time_in_secs(p_zGPSCControl)-p_zReportPos->u_timeofFix;
      U32 hor_error=(U32)sqrt(pow(protPos.ellip_alt_unc_ellip.unc_major, 2)+pow(protPos.ellip_alt_unc_ellip.unc_major, 2));

    	if (((p_zQop->u_HorizontalAccuracy!=C_ACCURACY_UNKNOWN)||p_zQop->u_HorizontalAccuracy>=hor_error)&&(((p_zQop->w_VerticalAccuracy!=C_ACCURACY_UNKNOWN))||(p_zQop->w_VerticalAccuracy>=protPos.ellip_alt_unc_ellip.unc_alt))&&((p_zQop->u_MaxLocationAge!=C_LOC_AGE_UNKNOWN)||((loc_age<=0)&&(loc_age<=p_zQop->u_MaxLocationAge))))
       {
	        // Check Qop. If position is Valid then Send it
           gpsPosInfo.pos_opt_bitmap=0x01;
		     gpsPosInfo.latitude=protPos.ellip_alt_unc_ellip.latitude;
		     gpsPosInfo.latitude_sign=protPos.ellip_alt_unc_ellip.latitude_sign;
		     gpsPosInfo.longtitude=protPos.ellip_alt_unc_ellip.longitude;
		     gpsPosInfo.altitude=protPos.ellip_alt_unc_ellip.altitude;
		     gpsPosInfo.altitudeDirection=protPos.ellip_alt_unc_ellip.altitude_sign;
		     gpsPosInfo.altUncertainty=protPos.ellip_alt_unc_ellip.unc_alt;
		     gpsPosInfo.uncertaintySemiMinor=protPos.ellip_alt_unc_ellip.unc_minor;
		     gpsPosInfo.uncertaintySemiMajor=protPos.ellip_alt_unc_ellip.unc_major;

		     // to do - UTC time to be added
	        memset(gpsPosInfo.UTCTimeStamp, 0x00, 20);
		     McpU8* p_utctime = gpsc_app_convert_gps2utc(p_zReportPos->u_timeofFix, &gpsPosInfo.UTCTimeStampNumByte);
           memcpy(gpsPosInfo.UTCTimeStamp, p_utctime, gpsPosInfo.UTCTimeStampNumByte);
	     }
	}
	else
	{
		ALOGI("+++ %s: Position Information Not Known +++\n", __FUNCTION__);
	}


   FLT f_float = (FLT)g_hor_acc;
	if ( f_float > 1800000) f_float = 1800000;

   qop_to_supl.horr_acc = (U8)ceil ( ext_log ( f_float * 0.1 + 1.0f ) / ext_log (1.1f));
	
   qop_to_supl.max_loc_age = g_loc_age;
   if (qop_to_supl.max_loc_age > 65535)
  	  qop_to_supl.max_loc_age = 65535;

   McpU16 max_response_time = g_rsp_time;
   if (max_response_time > 128)
  	  max_response_time = 128;

   qop_to_supl.max_response_time = (McpU8)(ext_log(max_response_time)/ext_log(2));
   qop_to_supl.delay= 	qop_to_supl.max_response_time;
   qop_to_supl.qop_optional_bitmap = QOP_MAX_LOC_AGE | QOP_DELAY;
   qop_to_supl.ver_acc = p_zGPSCControl->p_zSmlcAssist->z_Qop.w_VerticalAccuracy;


  if (p_zGPSCDatabase->z_DBGpsClock.u_Valid)
  {
	  zGpsTime.gps_week = p_zGPSCDatabase->z_DBGpsClock.w_GpsWeek;
	  zGpsTime.Msec = p_zGPSCDatabase->z_DBGpsClock.q_GpsMs;
	  zGpsTime.Uncertainty =p_zGPSCDatabase->z_DBGpsClock.f_TimeUnc;
  }
  else
  {
	  zGpsTime.gps_week = C_GPS_WEEK_UNKNOWN;
	  zGpsTime.Msec = 0xffffffff;
	  zGpsTime.Uncertainty = 0;
  }

  ALOGI("+++ %s: Assist Attempt [%d] +++\n", __FUNCTION__, p_zSmlcAssist->u_NumAssistAttempt);
  ALOGI("+++ %s: GPS Time - Valid [%d], Week [%d], MSec [%d], Unc [%f] +++\n", __FUNCTION__, 
				p_zGPSCDatabase->z_DBGpsClock.u_Valid,
				zGpsTime.gps_week,
				zGpsTime.Msec,
				zGpsTime.Uncertainty);

  /*** RECON changes: Send assistance request to both SAGPS (RX Networks) and Recon AGPS Provider. If this fails
       always send to last task ID (which is HAL), but treat as failure. ****/
  return_val = gpsc_app_send_assist_request(
           pNavc, 
           assist_bitmap_mandatory,
           assist_type_optional, 
           p_nav_model, 
           &gpsPosInfo, 
           &qop_to_supl, 
           &zGpsTime,
           SAGPS_PROVIDER);

  return_val |= gpsc_app_send_assist_request(
           pNavc, 
           assist_bitmap_mandatory,
           assist_type_optional, 
           p_nav_model, 
           &gpsPosInfo, 
           &qop_to_supl, 
           &zGpsTime,
           CUSTOM_AGPS_PROVIDER1);


  if (return_val != GPSC_SUCCESS)
  {
     gpsc_app_assist_request_ind (assist_bitmap_mandatory,
				  assist_type_optional, p_nav_model,
                                  &gpsPosInfo,
                                  &qop_to_supl,
                                  &zGpsTime,
                                  pNavc->eSrcTaskId,
                                  pNavc->uSrcQId);
  }
  else
  {
     p_zSmlcAssist->u_NumAssistAttempt++; 
     ALOGI("+++ %s: Successfully requested Assistance from Assistance Provider(s). Request number [%d] (maximum %d)  +++\n", 
        __FUNCTION__, p_zSmlcAssist->u_NumAssistAttempt, C_MAX_ASSIST_ATTEMPTS);

     /* Start 4 minutes RX Network timer. When this timer expires, NAVD will re-request
        ephemeris assistance data */
     /* check if the timer is already started. if already started then do not restart */
     if (GPSC_SUCCESS !=  gpsc_timer_status(p_zGPSCControl,C_TIMER_RXN_EPHE_VALID))
     {
         ALOGI("C_TIMER_RXN_EPHE_VALID Timer started");
         gpsc_timer_start(p_zGPSCControl,C_TIMER_RXN_EPHE_VALID,(4.0 * 60.0 * 1000.0));
     }
  }

  return return_val;
}


/*
 ******************************************************************************
 * gpsc_request_assistance
 *
 * Function description:
 *
 * This function is the internal function is used to populate the existing
 * navmodel information during request for assistance data.
 *
 *
 * Parameters:
 *
 * p_zGPSCControl : Pointer to GPSC Control structure.
 * p_zNavModel:		Pointer to structure that is populated with current Navmodel
 *					information that GPSC has.
 *
 * Return value:
 *   U8  :			Result from gpsc_app_assist_request_ind
 *
 ******************************************************************************
*/

#define C_T_TOE_LIMIT               0x0A

static U8 gpsc_populate_navmodel(gpsc_ctrl_type *p_zGPSCControl,T_GPSC_nav_model_req_params *p_zNavModel)
{
  gpsc_assist_wishlist_type*  p_GpsAssisWishlist =
                                   p_zGPSCControl->p_zGpsAssisWishlist;

  gpsc_db_type* p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
  gpsc_db_eph_type *p_zDBEphemeris;
  pe_RawEphemeris *p_RawEph;
  pe_RawSF1Param  *p_RawSF1Param;
  U8 u_nsat=0;
  if (p_GpsAssisWishlist->w_Wishlist & GPSC_REQ_NAV )
  {

    p_zNavModel->gps_week = 0;
	p_zNavModel->nav_toe = 0;
	p_zNavModel->nav_num_svs =0;
	p_zNavModel->nav_toe_limit =0;


    /* if the navigation model is requested, and if MS has a some, though
       not enough, SVs with good enough nav. model data, list them there */
    if ( p_GpsAssisWishlist->q_GoodEphList )
	{
	  U8 u_Prn;
      DBL d_FullToe_min;
      U8  u_toe_min;
      U8  u_SvId_newest = 0; /* ID of the SV with newest eph. */
      U16 w_EphWeek;
      U16 w_Iode;
	  U8 u_i;

      /* find from which "good eph." SV as defined by
         gpsc_sess_build_wishlist() as the youngest eph */
	  d_FullToe_min = 65535.0 * 604800.0 + 604800.0; /* max full toe supported */

      p_zDBEphemeris = &p_zGPSCDatabase->z_DBEphemeris[ 0 ];
	    for ( u_i = 0; u_i < N_SV; u_i++, p_zDBEphemeris++ )
	    {

	      if ( (p_GpsAssisWishlist->q_GoodEphList & (1L<<u_i) ) && p_zDBEphemeris->u_Valid )
		    {
		      /* found a good one as defined by gpsc_sess_build_wishlist() */
			  p_RawSF1Param = &p_zGPSCDatabase->z_DBEphemeris[ u_i ].z_RawSF1Param;
			  if (p_RawSF1Param->d_FullToe < d_FullToe_min )
		      {
		        d_FullToe_min = p_RawSF1Param->d_FullToe;
		        u_SvId_newest = (U8)(u_i+1);
		      }
		    }
	    }
      if (	u_SvId_newest == 0 || u_SvId_newest > N_SV )
	    {
	      u_nsat = 0; /* sanity check: just request all eph. */
	    }
      else
	    {
	      /* decide the week number to be reported */
		    w_EphWeek =  p_zGPSCDatabase->z_DBEphemeris[u_SvId_newest-1].w_EphWeek;

		    u_toe_min = (U8)(p_zGPSCDatabase->z_DBEphemeris[u_SvId_newest-1].
		                z_RawEphemeris.w_Toe / 225); /* toe in hours: w_toe unit is 16 sec. */

			p_zNavModel->gps_week = w_EphWeek;
			p_zNavModel->nav_toe = u_toe_min;
			p_zNavModel->nav_toe_limit = (C_T_TOE_LIMIT & 0x0F );

		    /* now put qualified SVs in the IODE list */
        for ( u_i = 0; u_i < N_SV; u_i++ )
		    {
	        if ( (p_GpsAssisWishlist->q_GoodEphList & (1L<<u_i) ) &&
	             p_zDBEphemeris->u_Valid )
		      {
				p_zDBEphemeris = &p_zGPSCDatabase->z_DBEphemeris[ u_i ];
				p_RawEph = &p_zGPSCDatabase->z_DBEphemeris[ u_i ].z_RawEphemeris;

				if ( p_zDBEphemeris->w_EphWeek == w_EphWeek )
				{

				  u_Prn = p_RawEph->u_Prn;

				  u_Prn--; /* format for the 6-bit SVId */

				  w_Iode = p_RawEph->u_Iode;
				  p_zNavModel->nav_data[u_nsat].iode = (U8)w_Iode;
				  p_zNavModel->nav_data[u_nsat].svid = u_Prn;
				  u_nsat++;
		  		}
		      }
			}
	    } /* if valid SVs to be included in the navmodel list */
	} /* if good-sv-list not empty */

   /* now fill in NSAT */
	p_zNavModel->nav_num_svs  = u_nsat;
    return TRUE;
  } /* if-wishlist-contains-navmodel-req */
  else
  {
	return FALSE;
  }
}


/* Function to send the NMEA report for the requested applicatio */

void gpsc_send_nmea_result(gpsc_ctrl_type*  p_zGPSCControl, gpsc_sess_specific_cfg_type* p_zSessSpecificCfg)
{
	T_GPSC_loc_fix_response zLocFixResponse;
	T_GPSC_loc_fix_response* p_zLocFixResponse = &zLocFixResponse;
	gpsc_result_type*	p_zResultBuffer = p_zGPSCControl->p_zResultBuffer;

	MCP_HAL_STRING_StrnCpy((McpS8*)p_zLocFixResponse->loc_fix_response_union.nmea_response.nmea_string,
         (McpS8*)p_zResultBuffer->s_ResultBuffer,
         sizeof(p_zLocFixResponse->loc_fix_response_union.nmea_response.nmea_string) );

	p_zLocFixResponse->loc_fix_session_id = p_zSessSpecificCfg->w_SessionTagId;
	p_zLocFixResponse->ctrl_loc_fix_response_union = GPSC_RESULT_NMEA;

	/* send NMEA information to application */
	gpsc_app_loc_fix_ind (p_zLocFixResponse);

	return;
}


void gpsc_send_position_result(gpsc_ctrl_type*  p_zGPSCControl, U8 u_FixNoFix, gpsc_sess_specific_cfg_type* p_zSessSpecificCfg)
{
//	U16 w_NmeaMask;
	T_GPSC_loc_fix_response * p_zLocFixResponse = NULL;
	T_GPSC_prot_position* p_zProtPos = NULL;
	//TNAVC_MotionMask_Status* p_zmotionmaskstatus = NULL; /* Klocwork Changes */
	static int FixNo;
//	gpsc_result_type*	p_zResultBuffer = p_zGPSCControl->p_zResultBuffer;
//#ifdef CLIENT_CUSTOM
	gpsc_db_type		  *p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_report_pos_type  *p_zReportPos  = &p_zGPSCDatabase->z_DBPos.z_ReportPos;
//#endif


	if(u_FixNoFix)
	{
		FixNo++;
		STATUSMSG("==>> Status: Got %d FIX <<==",FixNo);
#if DEBUG_MSG
		STATUSMSG(" Time tick when we got the position and before sent to ULTS = %d",GetLmTickCount());
#endif
	}
	else
	{
		STATUSMSG("Status: Timed Out");
		SESSLOGMSG("[%s]%s:%d, %s \n",get_utc_time(),SESS_WARN_GPS_POS,nav_sess_getCount(0),"#GPS cannot produce position within given response time");
	}

	p_zReportPos->u_timeofFix=(U32)gpsc_db_gps_time_in_secs(p_zGPSCControl);

#ifdef CLIENT_CUSTOM
	if (p_zGPSCCustom->custom_pos_rep_ext_req_flag)
	{
		T_GPSC_loc_fix_response zLocFixResponse;
        	T_GPSC_raw_position* p_zRawPos = NULL;
		p_zLocFixResponse = (T_GPSC_loc_fix_response*)&zLocFixResponse;
		p_zRawPos = &p_zLocFixResponse->loc_fix_response_union.raw_position;
		p_zLocFixResponse->ctrl_loc_fix_response_union = GPSC_RESULT_RAW_POSITION;
		p_zLocFixResponse->loc_fix_session_id = 254;  //HARD CODED
		p_zLocFixResponse->MeasorFix = 2;//POS =1, MES=0
#ifdef ENABLE_INAV_ASSIST
		p_zLocFixResponse->live_debug_flag = inavc_get_debug_ind(pNavcCtrl->hInavc);
		STATUSMSG("Live Debug Flag: 0x%x", p_zLocFixResponse->live_debug_flag);
#else
		p_zLocFixResponse->live_debug_flag = 0;
#endif
		gpsc_populate_raw_pos(p_zGPSCControl,p_zRawPos,p_zSessSpecificCfg );
		gpsc_app_loc_fix_ind (p_zLocFixResponse);
		p_zGPSCCustom->custom_pos_rep_ext_req_flag = FALSE;
		return;
	}
#endif

#ifdef WIFI_ENABLE

     /*Presently hybrid position is being called here- need to be check later
            hybrid position is being called only if pos bit map type is blended or wifi*/
     if((p_zSessSpecificCfg->u_LocFixPosBitmap & GPS_WIFI_BLEND)== GPS_WIFI_BLEND || (p_zSessSpecificCfg->u_LocFixPosBitmap & GPS_WIFI_BLEND)== WIFI_POS)
      {
         STATUSMSG("Request for Hybrid Position from gpsc_send_position_result()");
		 STATUSMSG("location type is = %d", p_zSessSpecificCfg->u_LocFixPosBitmap);
		 hybrid_gps_wifi_pos_blender(p_zGPSCControl,p_zSessSpecificCfg);
     }

    else
	{
		if(p_zSessSpecificCfg->w_ResultTypeBitmap & GPSC_RESULT_RAW )
		{
			T_GPSC_loc_fix_response zLocFixResponse;
	         	T_GPSC_raw_position* p_zRawPos = NULL;
			p_zLocFixResponse = (T_GPSC_loc_fix_response*)&zLocFixResponse;
			p_zRawPos = &p_zLocFixResponse->loc_fix_response_union.raw_position;
			p_zLocFixResponse->ctrl_loc_fix_response_union = GPSC_RESULT_RAW_POSITION;
			/* populate position fix type*/
	       p_zLocFixResponse->loc_fix_pos_type_bitmap = GPS_POS;

			p_zLocFixResponse->loc_fix_session_id = p_zSessSpecificCfg->w_SessionTagId;
			p_zLocFixResponse->MeasorFix = 2;
#ifdef ENABLE_INAV_ASSIST
			p_zLocFixResponse->live_debug_flag = inavc_get_debug_ind(pNavcCtrl->hInavc);
			STATUSMSG("Live Debug Flag: 0x%x", p_zLocFixResponse->live_debug_flag);
#else
			p_zLocFixResponse->live_debug_flag = 0;
#endif
			gpsc_populate_raw_pos(p_zGPSCControl,p_zRawPos,p_zSessSpecificCfg );
	//		SAPCALL(gpsc_app_loc_fix_ind);
			gpsc_app_loc_fix_ind (p_zLocFixResponse);
		}


		if(p_zSessSpecificCfg->w_ResultTypeBitmap & GPSC_RESULT_PROT )
		{
			T_GPSC_loc_fix_response zLocFixResponse;
		      	p_zLocFixResponse = (T_GPSC_loc_fix_response*)&zLocFixResponse;
			p_zLocFixResponse->loc_fix_session_id = p_zSessSpecificCfg->w_SessionTagId;
			p_zProtPos = &p_zLocFixResponse->loc_fix_response_union.prot_position;
			p_zLocFixResponse->ctrl_loc_fix_response_union = GPSC_RESULT_PROT_POSITION;
		if(p_zSessSpecificCfg->u_PosCompEntity == C_MS_COMPUTED||(p_zSessSpecificCfg->u_PosCompEntity == C_MS_COMPUTED_PREFERRED))
		{
			p_zLocFixResponse->loc_fix_req_mode=GPSC_FIXMODE_MSBASED;
		}
		else
		{
			p_zLocFixResponse->loc_fix_req_mode=GPSC_FIXMODE_MSASSISTED;
		}
#ifdef ENABLE_INAV_ASSIST
			p_zLocFixResponse->live_debug_flag = inavc_get_debug_ind(pNavcCtrl->hInavc);
			STATUSMSG("Live Debug Flag: 0x%x", p_zLocFixResponse->live_debug_flag);
#else
			p_zLocFixResponse->live_debug_flag = 0;
#endif
		STATUSMSG("==>> gpsc_send_position_result: Current Pos mode is %d  <<==",p_zLocFixResponse->loc_fix_req_mode);
			if(u_FixNoFix ==TRUE)
			{
				STATUSMSG("==>> gpsc_send_position_result: Calling gpsc_populate_rrlp_pos  <<==");
				p_zLocFixResponse->MeasorFix = 2;//POS =1, MES=0
				gpsc_populate_rrlp_pos(p_zGPSCControl,p_zProtPos);
			}
			else
			{
				p_zProtPos->gps_tow = 0;
				p_zProtPos->prot_fix_result = GPSC_PROT_FIXRESULT_NOFIX;
			}
	//		SAPCALL(gpsc_app_loc_fix_ind);
			gpsc_app_loc_fix_ind (p_zLocFixResponse);
			}
    	}
#else

	if(p_zSessSpecificCfg->w_ResultTypeBitmap & GPSC_RESULT_RAW )
		{
			T_GPSC_loc_fix_response zLocFixResponse;
	         	T_GPSC_raw_position* p_zRawPos = NULL;
			p_zLocFixResponse = (T_GPSC_loc_fix_response*)&zLocFixResponse;
			p_zRawPos = &p_zLocFixResponse->loc_fix_response_union.raw_position;
			p_zLocFixResponse->ctrl_loc_fix_response_union = GPSC_RESULT_RAW_POSITION;
			/* populate position fix type*/
	       p_zLocFixResponse->loc_fix_pos_type_bitmap = GPS_POS;

			p_zLocFixResponse->loc_fix_session_id = p_zSessSpecificCfg->w_SessionTagId;
			p_zLocFixResponse->MeasorFix = 2;
#ifdef ENABLE_INAV_ASSIST
			p_zLocFixResponse->live_debug_flag = inavc_get_debug_ind(pNavcCtrl->hInavc);
			STATUSMSG("Live Debug Flag: 0x%x", p_zLocFixResponse->live_debug_flag);
#else
			p_zLocFixResponse->live_debug_flag = 0;
#endif
			gpsc_populate_raw_pos(p_zGPSCControl,p_zRawPos,p_zSessSpecificCfg );
	//		SAPCALL(gpsc_app_loc_fix_ind);
			gpsc_app_loc_fix_ind (p_zLocFixResponse);
		}


		if(p_zSessSpecificCfg->w_ResultTypeBitmap & GPSC_RESULT_PROT )
		{
			T_GPSC_loc_fix_response zLocFixResponse;
		      	p_zLocFixResponse = (T_GPSC_loc_fix_response*)&zLocFixResponse;
			p_zLocFixResponse->loc_fix_session_id = p_zSessSpecificCfg->w_SessionTagId;
			p_zProtPos = &p_zLocFixResponse->loc_fix_response_union.prot_position;
			p_zLocFixResponse->ctrl_loc_fix_response_union = GPSC_RESULT_PROT_POSITION;
		if(p_zSessSpecificCfg->u_PosCompEntity == C_MS_COMPUTED||(p_zSessSpecificCfg->u_PosCompEntity == C_MS_COMPUTED_PREFERRED))
		{
			p_zLocFixResponse->loc_fix_req_mode=GPSC_FIXMODE_MSBASED;
		}
		else
		{
			p_zLocFixResponse->loc_fix_req_mode=GPSC_FIXMODE_MSASSISTED;
		}
#ifdef ENABLE_INAV_ASSIST
			p_zLocFixResponse->live_debug_flag = inavc_get_debug_ind(pNavcCtrl->hInavc);
			STATUSMSG("Live Debug Flag: 0x%x", p_zLocFixResponse->live_debug_flag);
#else
			p_zLocFixResponse->live_debug_flag = 0;
#endif
		STATUSMSG("==>> gpsc_send_position_result: Current Pos mode is %d  <<==",p_zLocFixResponse->loc_fix_req_mode);
			if(u_FixNoFix ==TRUE)
			{
				p_zLocFixResponse->MeasorFix = 2;//POS =1, MES=0
				gpsc_populate_rrlp_pos(p_zGPSCControl,p_zProtPos);
			}
			else
			{
				p_zProtPos->gps_tow = 0;
				p_zProtPos->prot_fix_result = GPSC_PROT_FIXRESULT_NOFIX;
			}
	//		SAPCALL(gpsc_app_loc_fix_ind);
                        gpsc_send_position_to_pirority_handler(p_zGPSCControl,p_zProtPos) ;
			gpsc_app_loc_fix_ind (p_zLocFixResponse);
			}
#endif // ifdef WIFI_ENABLE
}

U8 gpsc_send_measurement_result(gpsc_ctrl_type*  p_zGPSCControl, U8 u_MeasNoMeas, gpsc_sess_specific_cfg_type* p_zSessSpecificCfg)
{
	T_GPSC_loc_fix_response * p_zLocFixResponse;
	T_GPSC_raw_measurement* p_zRawMeasurement;

	if(u_MeasNoMeas)
	{
    STATUSMSG("App Loc fix Ind sent to GPSM");
		if(p_zSessSpecificCfg->w_ResultTypeBitmap & GPSC_RESULT_RAW )
	{
        T_GPSC_loc_fix_response zLocFixResponse;
	//	T_GPSC_raw_measurement raw_measurement;
		p_zLocFixResponse = (T_GPSC_loc_fix_response*)&zLocFixResponse;
         //	p_zRawMeasurement = &raw_measurement;
		p_zRawMeasurement = (T_GPSC_raw_measurement*) &p_zLocFixResponse->loc_fix_response_union.raw_measurement;
		/* assign session tag Id to location response */
	    p_zLocFixResponse->loc_fix_session_id = p_zSessSpecificCfg->w_SessionTagId;
		/* indicate that it is raw measurement result to location response */
		p_zLocFixResponse->ctrl_loc_fix_response_union = GPSC_RESULT_RAW_MEASUREMENT;
		gpsc_populate_raw_meas(p_zGPSCControl,(T_GPSC_raw_measurement*)p_zRawMeasurement,p_zSessSpecificCfg );
		SAPCALL(gpsc_app_loc_fix_ind);
		gpsc_app_loc_fix_ind (p_zLocFixResponse);

	  }
	  if(p_zSessSpecificCfg->w_ResultTypeBitmap & GPSC_RESULT_PROT )
	  {
		T_GPSC_loc_fix_response zLocFixResponse;
		T_GPSC_prot_measurement * p_zProtMeas = NULL;
        p_zLocFixResponse = (T_GPSC_loc_fix_response*)&zLocFixResponse;
		p_zLocFixResponse->loc_fix_session_id = p_zSessSpecificCfg->w_SessionTagId;
		p_zProtMeas = &p_zLocFixResponse->loc_fix_response_union.prot_measurement;

	    /* assign session tag Id to location response */
	    p_zLocFixResponse->loc_fix_session_id = p_zSessSpecificCfg->w_SessionTagId;

		if(p_zSessSpecificCfg->u_PosCompEntity == C_MS_COMPUTED||(p_zSessSpecificCfg->u_PosCompEntity == C_MS_COMPUTED_PREFERRED))
		{
			p_zLocFixResponse->loc_fix_req_mode=GPSC_FIXMODE_MSBASED;
		}
		else
		{
			p_zLocFixResponse->loc_fix_req_mode=GPSC_FIXMODE_MSASSISTED;
		}

		STATUSMSG("==>> gpsc_send_measurement_result: Current Pos mode is %d  <<==",p_zLocFixResponse->loc_fix_req_mode);

		p_zLocFixResponse->ctrl_loc_fix_response_union = GPSC_RESULT_PROT_MEASUREMENT;
		gpsc_populate_rrlp_meas(p_zGPSCControl,p_zProtMeas);
#if DEBUG_MSG
		STATUSMSG(" Time tick when we got the measurement and before sent to ULTS = %d",GetLmTickCount());
#endif
//		SAPCALL(gpsc_app_loc_fix_ind);
		gpsc_app_loc_fix_ind (p_zLocFixResponse);
	}
	}
	else
	{
		/* Sending RAW measument to application */
//		STATUSMSG("Sent RAW Measurement for Session ID: 0x%X",p_zSessSpecificCfg->w_SessionTagId);
		if(p_zSessSpecificCfg->w_ResultTypeBitmap & GPSC_RESULT_RAW )
		{
	    T_GPSC_loc_fix_response zLocFixResponse;
		p_zLocFixResponse = (T_GPSC_loc_fix_response*)&zLocFixResponse;
		p_zRawMeasurement = &p_zLocFixResponse->loc_fix_response_union.raw_measurement;

		/* assign session tag Id to location response */
	    p_zLocFixResponse->loc_fix_session_id = p_zSessSpecificCfg->w_SessionTagId;
		p_zLocFixResponse->ctrl_loc_fix_response_union = GPSC_RESULT_RAW_MEASUREMENT;
		gpsc_populate_raw_meas(p_zGPSCControl,(T_GPSC_raw_measurement*)p_zRawMeasurement,p_zSessSpecificCfg );
//			SAPCALL(gpsc_app_loc_fix_ind);
			gpsc_app_loc_fix_ind (p_zLocFixResponse);
		}

	} /* end of if(u_MeasNoMeas) */

	return TRUE;

}


void gpsc_populate_raw_pos
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_raw_position* p_raw_position,
 gpsc_sess_specific_cfg_type* p_zSessSpecificCfg
)
{
	U8 u_SvCount = 0;
	DBL d_Lat_Double;
	DBL d_Long_Double;
	T_GPSC_toa *p_toa = &p_raw_position->toa;
	T_GPSC_position *p_position = &p_raw_position->position;
	gpsc_db_type          *p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_report_pos_type  *p_zReportPos  = &p_zGPSCDatabase->z_DBPos.z_ReportPos;
	gpsc_db_gps_clk_type  *p_zDBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;
	T_GPSC_position_sv_info * p_zPosSvInfo = &p_position->position_sv_info[0];
	/*A lot of these are unpoplated as they need PG 1.1*/
	p_raw_position->report_num = (U16)((p_zSessSpecificCfg->w_TotalNumFixesPerSession
									- p_zSessSpecificCfg->w_FixCounter));
	p_raw_position->num_requested_reports =  p_zSessSpecificCfg->w_TotalNumFixesPerSession;
	p_toa->gps_week = p_zDBGpsClock->w_GpsWeek;
	p_toa->gps_msec = p_zDBGpsClock->q_GpsMs;
	p_toa->sub_ms 		= p_zDBGpsClock->f_TimeBias;
	p_toa->tUnc			= p_zDBGpsClock->f_TimeUnc;
	p_toa->fbias			= p_zDBGpsClock->f_FreqBias;
	p_toa->fUnc			= p_zDBGpsClock->f_FreqUnc;
	p_toa->TimerCount		= p_zReportPos->q_RefFCount;
	p_toa->u_valid		= p_zDBGpsClock->u_Valid;
	p_toa->sub_ms = 0;

	p_position->raw_fix_result_bitmap =p_zReportPos->w_PositionFlags ;
	p_position->latitude_radians =p_zReportPos->l_Lat ;
	p_position->longitude_radians =p_zReportPos->l_Long ;
	p_position->altitude_wgs84 =p_zReportPos->x_Height ;
	p_position->altitude_msl = p_zReportPos->x_HeightMSL;
	p_position->uncertainty_east =p_zReportPos->w_EastUnc ;
	p_position->uncertainty_north =p_zReportPos->w_NorthUnc ;
	p_position->uncertainty_vertical =p_zReportPos->w_VerticalUnc ;
	p_position->velocity_east =p_zReportPos->x_VelEast ;
	p_position->velocity_north =p_zReportPos->x_VelNorth ;
	p_position->velocity_vertical =p_zReportPos->x_VelVert ;
	p_position->velocity_uncertainty =p_zReportPos->w_VelUnc;
	p_position->PDOP =p_zReportPos->u_PDOP ;
	p_position->HDOP =p_zReportPos->u_HDOP ;
	p_position->VDOP =p_zReportPos->u_VDOP ;
	p_position->heading_true =p_zReportPos->w_HeadTrue ;
	p_position->heading_magnetic =p_zReportPos->w_HeadMagnet ;
	p_position->loc_angle_unc =p_zReportPos->w_LocAngleUnc ;
	p_position->loc_angle_unc_on_axis =p_zReportPos->w_LocUncOnAxis ;
	p_position->loc_angle_unc_perp_axis =p_zReportPos->w_LocUncPerAxis ;
	p_position->utc_hours =p_zReportPos->u_UtcHours ;
	p_position->utc_minutes =p_zReportPos->u_UtcMins ;
	p_position->utc_seconds =p_zReportPos->u_UtcSec ;
	p_position->utc_tenths =p_zReportPos->u_UtcTenthSec ;
	p_position->c_position_sv_info = p_zReportPos->u_nSVs ;

	d_Lat_Double = ((DBL)(p_position->latitude_radians))*  (DBL)C_LSB_LAT * (DBL)C_RAD_TO_DEG;
	d_Long_Double = ((DBL)(p_position->longitude_radians))*(DBL)C_LSB_LON * (DBL)C_RAD_TO_DEG ;
	STATUSMSG("gpsc_populate_raw_pos: Lat=%d, Lon=%d, Ht=%d",p_position->latitude_radians,p_position->longitude_radians,p_position->altitude_wgs84);
	if(p_zSessSpecificCfg->u_WaitFirstFix == TRUE) // To print only first time
	{
		U32 timetofix=mcpf_getCurrentTime_InMilliSec(p_zGPSCControl->p_zSysHandlers->hMcpf)-p_zGPSCControl->p_zGPSCState->u_GpscLocFixStartSystemTime;
			SESSLOGMSG("[%s]%s:%d, %f,%f,%f,%f,%f,%f,%f %s %d ms \n",get_utc_time(),SESS_POS_RESULT,nav_sess_getCount(0),d_Lat_Double,d_Long_Double ,(DBL)(p_position->altitude_wgs84 * 0.5),(DBL)((p_zReportPos->w_LocAngleUnc)*180.0/(pow(2,15))),(DBL)(p_zReportPos->w_VerticalUnc * 0.5),(DBL)(p_zReportPos->w_LocUncOnAxis * 0.1),(DBL)(p_zReportPos->w_LocUncPerAxis * 0.1),"#Position (Lat, Lang, Alt,orientation,height_unc,unc_maj,unc_min) time to fix : ",timetofix);
	}else
	{
		STATUSMSG("SessLog: [%s]%s:%d, %f,%f,%f,%f,%f,%f,%f %s \n",get_utc_time(),SESS_POS_RESULT,nav_sess_getCount(0),d_Lat_Double,d_Long_Double ,(DBL)(p_position->altitude_wgs84 * 0.5),(DBL)((p_zReportPos->w_LocAngleUnc)*180.0/(pow(2,15))),(DBL)(p_zReportPos->w_VerticalUnc * 0.5),(DBL)(p_zReportPos->w_LocUncOnAxis * 0.1),(DBL)(p_zReportPos->w_LocUncPerAxis * 0.1),"#Position Lat, Lang, Alt,orientation,height_unc,unc_maj,unc_min");
	}

	//STATUSMSG("SessLog: [%s]%s:%d, %f,%f,%f,%f,%f,%f,%f %s \n",get_utc_time(),SESS_POS_RESULT,nav_sess_getCount(0),d_Lat_Double,d_Long_Double ,(DBL)(p_position->altitude_wgs84*0.5),(DBL)((p_zReportPos->w_LocAngleUnc)*180.0/(pow(2,15))),(DBL)(p_zReportPos->w_VerticalUnc * 0.5),(DBL)(p_zReportPos->w_LocUncOnAxis * 0.1),(DBL)(p_zReportPos->w_LocUncPerAxis * 0.1),"#Position Lat, Lang, Alt,orientation,height_unc,unc_maj,unc_min");
	for (u_SvCount=0; u_SvCount < p_position->c_position_sv_info; u_SvCount++)
	{
	p_zPosSvInfo->svid = (U8)(p_zReportPos->u_SVs[u_SvCount]-1);
	p_zPosSvInfo->iode = p_zReportPos->u_IODE[u_SvCount];
	p_zPosSvInfo->residual = p_zReportPos->w_Residuals[u_SvCount];
	p_zPosSvInfo->weight = p_zReportPos->w_SvWeights[u_SvCount];
	p_zPosSvInfo++;
	}

}

void gpsc_populate_rrlp_pos
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_prot_position* p_prot_position
)
{
	T_GPSC_ellip_alt_unc_ellip *p_ellip_alt_unc_ellip = &p_prot_position->ellip_alt_unc_ellip;
	gpsc_db_type          *p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_report_pos_type  *p_zReportPos  = &p_zGPSCDatabase->z_DBPos.z_ReportPos;
	gpsc_db_gps_clk_type  *p_zDBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;

	U16 w_Word;
	FLT f_float;
	DBL d_Double;
	McpDBL   					  gps_secs_80;
	McpDBL   					  utc_secs_80;

	 p_prot_position->gps_tow = p_zDBGpsClock->q_GpsMs;
	 p_prot_position->gps_secs = gpsc_db_gps_time_in_secs(p_zGPSCControl);
	 gps_secs_80 = p_prot_position->gps_secs;
	 utc_secs_80 = (McpDBL)((p_zDBGpsClock->w_GpsWeek * WEEK_SECS) +
	 				(((U8)(p_zDBGpsClock->q_GpsMs/DAY_MSECS))*DAY_SEC) +
	 				(p_zReportPos->u_UtcHours * 3600) +
	 				(p_zReportPos->u_UtcMins * 60)+
	 				(p_zReportPos->u_UtcSec)+
	 				(p_zReportPos->u_UtcTenthSec/10));

	 STATUSMSG("Before compo: gpsc_populate_rrlp_pos: utc_secs_80 = %f, gps_secs_80 = %f",utc_secs_80,gps_secs_80);

	 if(gps_secs_80 < utc_secs_80 )
	 	utc_secs_80 -=  DAY_SEC;

	p_prot_position->ellip_alt_unc_ellip.utctime = utc_secs_80;

	  p_ellip_alt_unc_ellip->velocity_flag=0; //speed is currently filled in Raw Pos or MSA position

	/* 3D fix : 2D fix */;
	if(p_zReportPos->w_PositionFlags & 0x02)
	{
		p_prot_position->prot_fix_result =  GPSC_PROT_FIXRESULT_3D;
		p_ellip_alt_unc_ellip->shape_code =  C_POS_SHAPE_ELLIP_ALT_UNC_ELLIP; /*9*/
		p_ellip_alt_unc_ellip->v_altitude = 1;
		p_ellip_alt_unc_ellip->v_altitude_sign = 1;
		p_ellip_alt_unc_ellip->v_unc_alt = 1;
	}
	else if(p_zReportPos->w_PositionFlags & 0x01)
	{
		p_prot_position->prot_fix_result =  GPSC_PROT_FIXRESULT_2D;
		p_ellip_alt_unc_ellip->shape_code =  C_POS_SHAPE_ELLIP_UNC_ELLIP; /*4*/
		/*Set these to invalid, however we will populate the values
		  with the values GPS sends us. But the will be stale altitude values*/
		p_ellip_alt_unc_ellip->v_altitude = 0;
		p_ellip_alt_unc_ellip->v_altitude_sign = 0;
		p_ellip_alt_unc_ellip->v_unc_alt = 0;
	}
	else
	{
		//ERRORMSG("Error : Expected position flags to be either 2D or 3D");
		p_prot_position->prot_fix_result = GPSC_PROT_FIXRESULT_NOFIX;
		return;
	}

     /* shape*/


    /******** latitude ****************/

	d_Double = ((DBL)(p_zReportPos->l_Lat) * C_2_23_OVER_90)*  (DBL)C_LSB_LAT * (DBL)C_RAD_TO_DEG;

	 p_ellip_alt_unc_ellip->latitude_sign = 0x0;
    if (d_Double < 0 ) /* if latitude in the southern hemesphere */
    {
      p_ellip_alt_unc_ellip->latitude_sign = 0x80; /* sign bit high for southern
                                             hemesphere */

      /* then code the rest with the absolute value of the latitude */
      d_Double = -d_Double;
    }

    p_ellip_alt_unc_ellip->latitude = (U32)d_Double;

    /********** longitude ************/
    d_Double = ((DBL)(p_zReportPos->l_Long) * C_2_24_OVER_360)*(DBL)C_LSB_LON * (DBL)C_RAD_TO_DEG ;

    p_ellip_alt_unc_ellip->longitude = (S32)d_Double;

    /********* altitude ***************/

    if ( p_zReportPos->x_Height < 0 )
    {
      /*** depth into earth condition */
      w_Word = (U16)-p_zReportPos->x_Height;
      p_ellip_alt_unc_ellip->altitude_sign = (U8)0x01;
    }
    else
    {
      w_Word = p_zReportPos->x_Height;
	  p_ellip_alt_unc_ellip->altitude_sign = 0x0;
    }
    w_Word >>= 1; /* LSB of p_zReportPos->x_Height is 0.5 meters */
    p_ellip_alt_unc_ellip->altitude = w_Word;


	STATUSMSG("gpsc_populate_rrlp_pos: Lat=%d, Lon=%d, Ht=%d",p_ellip_alt_unc_ellip->latitude,p_ellip_alt_unc_ellip->longitude,p_ellip_alt_unc_ellip->altitude);

    /********* uncertainty  *************/

    /*** AI2 limits the representation of horizontal uncs to 6553.5m
         and RRLP's limit is 1800 km, so no need to cap uncs here */

    /* RRLP uses K to discribe altitude unc:
       Altitude (meters) = C * ( (1+x) ** K - 1 ),
       where C = 10, x = 0.1. */

	U8  u_UncMajorK, u_UncMinorK; /* K in the unc representation */

    if(/*p_zGPSCControl->hw_type!=GPSC_HW_NL5500*/0)
	{
	    u_UncMajorK = (U8)( ext_log ( (FLT)p_zReportPos->w_LocUncOnAxis*
	                           (FLT)C_LSB_EER * 0.1f + 1.0f
	                 ) /
	                 ext_log(1.1f)
	             );

	    u_UncMinorK = (U8)( ext_log ( (FLT)p_zReportPos->w_LocUncPerAxis*
	                            (FLT)C_LSB_NER * 0.1f + 1.0f
	                ) /
	              ext_log(1.1f)
	              );

		  FLT f_float;

		  f_float=(FLT)(((p_zReportPos->w_LocAngleUnc)*(FLT)C_RAD_TO_DEG)*0.5f);

	      p_ellip_alt_unc_ellip->orient_major = (McpU8)f_float; /* from North, clockwise */
	      p_ellip_alt_unc_ellip->unc_major= u_UncMajorK;
	      p_ellip_alt_unc_ellip->unc_minor = u_UncMinorK;


	    /*** AI2 limits the representation of vertical unc to 32767.5m
	         and RRLP's limit is 990.5m, so we need to cap it here */
	    f_float = (FLT)p_zReportPos->w_VerticalUnc * (FLT)C_LSB_VER;
	    if ( f_float > 990.5f) f_float = 990.5;

	    /* RRLP uses K to discribe altitude unc:
	       Altitude (meters) = C * ( (1+x) ** K - 1 ),
	       where C = 45, x = 0.025. Note 1/45 = 0.022222222 */
	    p_ellip_alt_unc_ellip->unc_alt =
	      (U8)ceil ( ext_log ( f_float * 0.02222f + 1.0f ) / ext_log (1.025f));
	    p_ellip_alt_unc_ellip->confidence = 0; /* we don't use it */
	}
	else //for backward compatibility with NL5500
	{

		U8  u_EastUncK, u_NorthUncK; /* K in the unc representation */

		u_EastUncK = (U8)( ext_log ( (FLT)p_zReportPos->w_EastUnc *
								   (FLT)C_LSB_EER * 0.1f + 1.0f
						 ) /
						 ext_log(1.1f)
					 );

			u_NorthUncK = (U8)( ext_log ( (FLT)p_zReportPos->w_NorthUnc *
									(FLT)C_LSB_NER * 0.1f + 1.0f
						) /
					  ext_log(1.1f)
					  );


			if (p_zReportPos->w_EastUnc > p_zReportPos->w_NorthUnc)
			{
			  /* East-West being semi-major */
			  p_ellip_alt_unc_ellip->orient_major = 89; /* from North, clockwise */
			  p_ellip_alt_unc_ellip->unc_major= u_EastUncK;
			  p_ellip_alt_unc_ellip->unc_minor = u_NorthUncK;
			}
			else
			{
			  /* North-South being semi-major */
			  p_ellip_alt_unc_ellip->orient_major = 0; /* from North, clockwise */
			  p_ellip_alt_unc_ellip->unc_major = u_NorthUncK;
			  p_ellip_alt_unc_ellip->unc_minor = u_EastUncK;
			}

			/*** AI2 limits the representation of vertical unc to 32767.5m
				 and RRLP's limit is 990.5m, so we need to cap it here */
			f_float = (FLT)p_zReportPos->w_VerticalUnc * (FLT)C_LSB_VER;
			if ( f_float > 990.5f) f_float = 990.5;

			/* RRLP uses K to discribe altitude unc:
			   Altitude (meters) = C * ( (1+x) ** K - 1 ),
			   where C = 45, x = 0.025. Note 1/45 = 0.022222222 */
			p_ellip_alt_unc_ellip->unc_alt =
			  (U8)ceil ( ext_log ( f_float * 0.02222f + 1.0f ) / ext_log (1.025f));
			p_ellip_alt_unc_ellip->confidence = 0; /* we don't use it */

	}
	STATUSMSG("uncertaintySemiMinor : %d",p_ellip_alt_unc_ellip->unc_minor);
	STATUSMSG("uncertaintySemiMajor : %d",p_ellip_alt_unc_ellip->unc_major);


}

#define C_SPEED_LIGHT_NSEC  (LIGHT_MSEC * 0.001 * 0.001) /* speed of light:
                                                            meters / nanosec */

T_GPSC_result gpsc_get_wishlist(T_GPSC_wishlist_params* p_gpsc_wishlist_params)
{
	U16 u_i;
	gpsc_custom_struct* p_zCustom = gp_zGPSCControl->p_zCustomStruct;
	gpsc_db_eph_type *p_DBEphemeris = &gp_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[0];
	gpsc_db_alm_type *p_DBAlmanac = &gp_zGPSCControl->p_zGPSCDatabase->z_DBAlmanac[0];

	p_gpsc_wishlist_params->assist_availability_flags = (McpU16)(p_zCustom->custom_assist_availability_flag);
	p_gpsc_wishlist_params->assist_availability_flags  &= ~C_RTI_AVAIL;
	p_gpsc_wishlist_params->assist_availability_flags = (McpU16)(~p_gpsc_wishlist_params->assist_availability_flags);

	// Ephimeries bitmask
	p_gpsc_wishlist_params->eph_availability_flags = 0;
	p_gpsc_wishlist_params->pred_eph_availability_flags = 0;
	for (u_i=0;u_i<N_SV;u_i++)
	{
		if (p_DBEphemeris[u_i].u_Valid)
			{
				/*if the ephemeris is a predicted one then update the predicted ephemeris flag else the normal ephemeris availability flag*/
				if (p_DBEphemeris[u_i].z_RawEphemeris.u_predicted)
					p_gpsc_wishlist_params->pred_eph_availability_flags |= (U32) pow(2,u_i);
				else
				p_gpsc_wishlist_params->eph_availability_flags |= (U32) pow(2,u_i);
			}
	}
	p_gpsc_wishlist_params->eph_availability_flags = ~p_gpsc_wishlist_params->eph_availability_flags;
	p_gpsc_wishlist_params->pred_eph_availability_flags = ~p_gpsc_wishlist_params->pred_eph_availability_flags;
	// Almanac bitmask
	p_gpsc_wishlist_params->alm_availability_flags = 0;
	for (u_i=0;u_i<N_SV;u_i++)
	{
		if (p_DBAlmanac[u_i].u_Valid)
			{
				p_gpsc_wishlist_params->alm_availability_flags |= (U32) pow(2,u_i);
			}
	}
	p_gpsc_wishlist_params->alm_availability_flags = ~p_gpsc_wishlist_params->alm_availability_flags;

	return GPSC_SUCCESS;
}


void gpsc_populate_raw_meas
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_raw_measurement* p_raw_measurement,
 gpsc_sess_specific_cfg_type* p_zSessSpecificCfg
)
{
	U8 u_i,u_j;
   	DBL d_AdjSubMsec;
	T_GPSC_measurement* p_measurement;
	gpsc_db_type          *p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
   	gpsc_db_gps_meas_type*       p_zDBGpsMeas  = &p_zGPSCDatabase->z_DBGpsMeas;
   	gpsc_meas_report_type*       p_zMeasReport = &p_zDBGpsMeas->z_MeasReport;
    	gpsc_meas_per_sv_type*       p_zMeasPerSv;
    	gpsc_db_gps_clk_type*        p_zDBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;
	T_GPSC_toa *p_toa = &p_raw_measurement->toa;

	gpsc_db_eph_type *p_DBEphemeris = &p_zGPSCDatabase->z_DBEphemeris[0];
	gpsc_db_alm_type *p_DBAlmanac = &p_zGPSCDatabase->z_DBAlmanac[0];

	gpsc_custom_struct* p_zCustom = p_zGPSCControl->p_zCustomStruct;
	gpsc_db_sv_dir_type  *p_DBSvDirection = &p_zGPSCControl->p_zGPSCDatabase->z_DBSvDirection;
	gpsc_db_gps_inj_clk_type* p_DBGpsInjClock =&p_zGPSCControl->p_zGPSCDatabase->z_DBGpsInjClock;
	gpsc_db_health_type *p_zDBHealth = &p_zGPSCDatabase->z_DBHealth;
	gpsc_smlc_assist_type* p_zSmlcAssist = p_zGPSCControl->p_zSmlcAssist;
	gpsc_event_cfg_type*   p_zEventCfg = p_zGPSCControl->p_zEventCfg;

	/*SV Debug variables*/
	McpU8   ephDbgStr[250] =" ";
	McpU8 *p_ephDbgStr = ephDbgStr;
	McpS8 svIdStr[5];


	/* raw measurement */
	/* assign session report info */
	p_raw_measurement->report_num = (U16)((p_zSessSpecificCfg->w_TotalNumFixesPerSession
									- p_zSessSpecificCfg->w_FixCounter));
	p_raw_measurement->num_requested_reports =  p_zSessSpecificCfg->w_TotalNumFixesPerSession;

	/* update toa from clock or meas rep ??? */
	p_toa->gps_week = p_zDBGpsClock->w_GpsWeek;
	p_toa->gps_msec = p_zDBGpsClock->q_GpsMs;
	p_toa->sub_ms 		= (McpFLT)p_zDBGpsClock->f_TimeBias;
	p_toa->tUnc			= p_zDBGpsClock->f_TimeUnc;
	p_toa->fbias		= p_zDBGpsClock->f_FreqBias;
	p_toa->fUnc			= p_zDBGpsClock->f_FreqUnc;
	p_toa->TimerCount		= p_zMeasReport->q_FCount;

	if (p_DBGpsInjClock->u_Valid == TRUE)
		p_raw_measurement->assist_availability_flags |= C_TCXO_AVAIL;

	p_raw_measurement->assist_availability_flags = (McpU16)(~p_zCustom->custom_assist_availability_flag);
	/* request for clock if uncertainty is greater than 3sec */
	if(p_zDBGpsClock->f_TimeUnc > 3000)
		p_raw_measurement->assist_availability_flags  |= C_REF_GPSTIME_AVAIL;

	// Ephimeries bitmask
	p_raw_measurement->eph_availability_flags = 0;
	p_raw_measurement->pred_eph_availability_flags = 0;

	for (u_i=0;u_i<N_SV;u_i++)
	{
		/*Check and update the ephemeris available in data base - set the bitmap of eph valid and toe not expired*/
		if ( (p_DBEphemeris[u_i].u_Valid) && (FALSE == p_DBEphemeris[u_i].z_RawEphemeris.toe_expired) )
		{
			/*if the ephemeris is a predicted one then update the predicted ephemeris flag else the normal ephemeris availability flag*/
			if (p_DBEphemeris[u_i].z_RawEphemeris.u_predicted)
			{
				p_raw_measurement->pred_eph_availability_flags |= (U32) pow(2,u_i);
			}
			else
			{
				p_raw_measurement->eph_availability_flags |= (U32) pow(2,u_i);
			}
		}

		/*If we have Injected ephemeris is smlc assistance and eph_toe expired then request for the same*/
		if((p_zCustom->custom_assist_availability_flag & C_EPH_AVAIL) && (FALSE == p_zSmlcAssist->z_SmlcRawEph[u_i].toe_expired ))
		{
			if(p_zSmlcAssist->z_SmlcRawEph[u_i].u_predicted)
			{
				p_raw_measurement->pred_eph_availability_flags |= (U32) pow(2, u_i);
			}
			else
			{
				p_raw_measurement->eph_availability_flags |= (U32) pow(2, u_i);
			}

		}

	}

	// Dont ask for ephimeris for SVs with poor health
	if(p_zCustom->custom_assist_availability_flag & C_RTI_AVAIL)
	{
		for(u_i=0;u_i<N_SV;u_i++)
		{
			if( p_zDBHealth->u_AlmHealth[u_i]==1 ||p_zDBHealth->u_AlmHealth[u_i]==3)
			{ // BAD or Does not exist
				p_raw_measurement->eph_availability_flags |= (U32) pow(2,u_i);

				MCP_HAL_STRING_Sprintf(svIdStr, " :%d", u_i );
				p_ephDbgStr = MCP_HAL_STRING_StrCat(p_ephDbgStr, svIdStr);
			}
		}
		STATUSMSG("SV Health N-OK: %s", p_ephDbgStr);
	}

	p_raw_measurement->eph_availability_flags = ~p_raw_measurement->eph_availability_flags;
	p_raw_measurement->pred_eph_availability_flags = ~p_raw_measurement->pred_eph_availability_flags;


	// Almanac bitmask
		p_raw_measurement->alm_availability_flags = 0;
		for (u_i=0;u_i<N_SV;u_i++)
			{
				if (p_DBAlmanac[u_i].u_Valid)
					{
						p_raw_measurement->alm_availability_flags |= (U32) pow(2,u_i);
					}

			}
	p_raw_measurement->alm_availability_flags = ~p_raw_measurement->alm_availability_flags;
	p_raw_measurement->d_PosUnc = p_zMeasReport->d_PosUnc;
	p_raw_measurement->q_GoodStatSvIdBitMap = p_zMeasReport->q_GoodStatSvIdBitMap;
	p_raw_measurement->u_IsValidMeas 	= p_zMeasReport->u_IsValidMeas;


	STATUSMSG("GpscPoplteRawMeas:PosUnc=%lf,AA=%x,EPH=%x,ALM=%x,GUDSVBITMAP:%x,IsValMeas:%d",
				p_raw_measurement->d_PosUnc,
				p_raw_measurement->assist_availability_flags,
				p_raw_measurement->eph_availability_flags,
				p_raw_measurement->alm_availability_flags,
		              p_raw_measurement->q_GoodStatSvIdBitMap,
              		p_raw_measurement->u_IsValidMeas);
	STATUSMSG("Cont..PRED_EPH= %x, p_bitmap = %x",p_raw_measurement->pred_eph_availability_flags, p_zCustom->custom_pred_eph_bitmap);


	/* fill out the measurement-dependent part of the message */
    /* find out how many SVs involved */
	p_raw_measurement->c_measurement = 0;
	p_raw_measurement->d_PosUnc =p_zMeasReport->d_PosUnc;

	STATUSMSG("PosUnc=%lf",p_raw_measurement->d_PosUnc );

	for (u_i=0,u_j=0; u_i < N_LCHAN; u_i++)
    {
      U8 u_SvId;
      p_zMeasPerSv = &p_zMeasReport->z_MeasPerSv[u_i];

      u_SvId = (U8)(p_zMeasPerSv->u_SvIdSvTimeFlag & 0x3F);

	  if(u_SvId && p_zMeasPerSv->u_SvIdSvTimeFlag)
      {
        /* this is a valid SV to report */
		p_raw_measurement->c_measurement++;

        /* SV id */
		p_measurement = &p_raw_measurement->measurement[u_j++];  /* N_LCHAN */
	    p_measurement->svid = (U8)(u_SvId-1);

		/* time tag info */
		p_measurement->time_tag_info = (p_zMeasPerSv->u_SvIdSvTimeFlag >> 6);


		/* snr */
		p_measurement->snr = p_zMeasPerSv->w_Snr;
		/* cno_tenths */
        /* Cno. Add in noise figure and implementation loss (then round and
           switch from 0.1 to 1 dB-Hz/bit units) in order to reference it to
           the antenna input. */
        /* Cno round and switch from 0.1 to 1 dB-Hz/bit units)
		   in order to reference it to the antenna input. */
        p_measurement->cno_tenths = p_zMeasPerSv->w_Cno;

		/* latency-ms */
		p_measurement->latency_ms = p_zMeasPerSv->x_LatencyMs;
		/* pre_int */
		p_measurement->pre_int = p_zMeasPerSv->u_PreInt;
		/* post_int */
		p_measurement->post_int = p_zMeasPerSv->u_PreInt;
		/* msec */
		p_measurement->msec = p_zMeasPerSv->q_Msec;

		/* sub_msec */
		/*** We tell SMLC that the reference point of the measurement is
           at TOW, which is now contained in p_zGpsMsrSetElement->q_GpsTow_R24,
           but the actually GPS moment of the measurement is instead at
           the FCOUNT of the measurement, whose GPS msec is
           p_zMeasGpsTime->q_Msec - p_zMeasGpsTime->f_ClkBias.
           We can adjust each SV's submsec value by adding
           p_zMeasGpsTime->f_ClkBias to it. */

        d_AdjSubMsec = (DBL)p_zMeasPerSv->q_SubMsec
                     * C_LSB_SV_SUBMS + p_zMeasReport->z_MeasToa.f_ClkTimeBias;

        if (d_AdjSubMsec > 1.0 )
        {

          /* if the adjustment results in the submsec value above one, at the
             reference moment (TOW) SV time's msec should be one msec more,
             thus we need to subtract 1 ms from the submsec variable */

          d_AdjSubMsec -= 1.0;
        }

        else if  ( d_AdjSubMsec < 0 )
        {

          /** or if the adjustment results in submsec value negative, at the
            reference moment (TOW) SV time's msec should be one msec less,
            thus we need to use one minus the resuting "submsec" value for
            the submsec value */

          d_AdjSubMsec = 1.0 + d_AdjSubMsec;
        }
		p_measurement->sub_msec = (U32) d_AdjSubMsec;

		/* sv_time_uncertainty */
		p_measurement->sv_time_uncertainty = p_zMeasPerSv->w_SvTimeUnc;
		/* sv_speed*/
		p_measurement->sv_speed = p_zMeasPerSv->l_SvSpeed;
		/* sv_speed_uncertainty */
		p_measurement->sv_speed_uncertainty = p_zMeasPerSv->q_SvSpeedUnc;
		/* meas_status_bitmap */
		p_measurement->meas_status_bitmap = p_zMeasPerSv->w_MeasStatus;

                if(p_zEventCfg->u_Ext_reports)
                {
		/* Extended measurement report - Support in NL5500 */
		/*Channel Measurement State*/
		p_measurement->channel_meas_state = p_zMeasPerSv->u_ChannelMeasState;
		/*Accumulated Carrier Phase*/
		p_measurement->accum_carrier_phase = p_zMeasPerSv->q_AccCarrierPhase;
		/*Carrier Velocity*/
		p_measurement->carrier_vel = p_zMeasPerSv->q_CarrierVelocity;
		/*Carrier Acceleration*/
		p_measurement->carrier_acc = p_zMeasPerSv->w_CarrierAcc;
		/*Loss of lock indicator*/
		p_measurement->loss_lock_ind = p_zMeasPerSv->u_LossLockInd;
		/* Good Observations Count */
		p_measurement->good_obv_cnt = p_zMeasPerSv->u_GoodObvCount;
		/* Total Observation Count */
		p_measurement->total_obv_cnt = p_zMeasPerSv->u_TotalObvCount;
		}
		else
		{
		/* Extended measurement report - Support in NL5500 */
		/*Channel Measurement State*/
		p_measurement->channel_meas_state = 0xcc;
		/*Accumulated Carrier Phase*/
		p_measurement->accum_carrier_phase = 0xcccccccc;
		/*Carrier Velocity*/
		p_measurement->carrier_vel = (S32)0xcccccccc;
		/*Carrier Acceleration*/
		p_measurement->carrier_acc = (U16)0xcccc;
		/*Loss of lock indicator*/
		p_measurement->loss_lock_ind = 0xcc;
		/* Good Observations Count */
		p_measurement->good_obv_cnt = 0xcc;
		/* Total Observation Count */
		p_measurement->total_obv_cnt = 0xcc;
		}

		/* Add Elevation and Azimuth for each SV as part of measurement report. */

		/* Elevation */
		p_measurement->elevation = p_DBSvDirection->z_RawSvDirection[u_SvId-1].b_Elev;

		/* Azimuth */
		p_measurement->azimuth = p_DBSvDirection->z_RawSvDirection[u_SvId-1].u_Azim;


      }
  }

  STATUSMSG("gpsc_populate_raw_meas: Sending %d SVs",p_raw_measurement->c_measurement);
}



void gpsc_populate_rrlp_meas
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_prot_measurement* p_prot_measurement
)
{
    T_GPSC_sv_measurement*       p_sv_measurement;
	gpsc_db_type          *p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
    gpsc_db_gps_meas_type*       p_zDBGpsMeas  = &p_zGPSCDatabase->z_DBGpsMeas;
    gpsc_meas_report_type*       p_zMeasReport = &p_zDBGpsMeas->z_MeasReport;
    //gpsc_meas_stat_report_type*  p_zMeasStatReport = &p_zDBGpsMeas->z_MeasStatReport;/* Klocwork Changes */
    gpsc_meas_per_sv_type*       p_zMeasPerSv;
    gpsc_db_sv_dir_type  *p_DBSvDirection = &p_zGPSCControl->p_zGPSCDatabase->z_DBSvDirection;

    gpsc_db_gps_clk_type*        p_zDBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;

    	gpsc_db_eph_type *p_DBEphemeris = &p_zGPSCDatabase->z_DBEphemeris[0];
	gpsc_db_alm_type *p_DBAlmanac = &p_zGPSCDatabase->z_DBAlmanac[0];
	gpsc_custom_struct* p_zCustom = p_zGPSCControl->p_zCustomStruct;
	gpsc_smlc_assist_type* p_zSmlcAssist = p_zGPSCControl->p_zSmlcAssist;
	gpsc_db_health_type *p_zDBHealth = &p_zGPSCDatabase->z_DBHealth;

    U8 u_i,u_j, u_exponent;
    U16  w_mantissa;
    S16 x_Word;
    FLT f_Num;
    DBL d_AdjSubMsec, d_clock_freq_bias, d_Num;
	U8 u_HasMultipathInd;
	U32  w_IntTime = 0;
	U16  w_Latency=0;

	/*SV Debug variables*/
	McpU8   ephDbgStr[250] =" ";
	McpU8 *p_ephDbgStr = ephDbgStr;
	McpS8 svIdStr[5];


    d_clock_freq_bias = (DBL)p_zDBGpsClock->f_FreqBias;

    /* Now, at p_zMeasReport->q_FCount,
       GpsTime = p_zMeasGpsTime->q_GpsMsec - p_zMeasGpsTime->f_ClkTimeBias **/

    /* we report TOW (msec into week) of the measurement moment */

    p_prot_measurement->gps_tow =
      p_zMeasReport->z_MeasToa.q_GpsMsec ;//% C_FOUR_HRS_MSEC -commented here and added only for rrlp;

    /* fill out the measurement-dependent part of the message */
    /* find out how many SVs involved */

    p_prot_measurement->c_sv_measurement = 0;


    // Ephimeries bitmask
	p_prot_measurement->eph_availability_flags = 0;
	p_prot_measurement->pred_eph_availability_flags = 0;


	for (u_i=0;u_i<N_SV;u_i++)
	{
		/*Check and update the ephemeris available in data base - set the bitmap of eph valid and toe not expired*/
		if ( (p_DBEphemeris[u_i].u_Valid) && (FALSE == p_DBEphemeris[u_i].z_RawEphemeris.toe_expired) )
		{
			/*if the ephemeris is a predicted one then update the predicted ephemeris flag else the normal ephemeris availability flag*/
			if (p_DBEphemeris[u_i].z_RawEphemeris.u_predicted)
			{
				p_prot_measurement->pred_eph_availability_flags |= (U32) pow(2,u_i);
			}
			else
			{
				p_prot_measurement->eph_availability_flags |= (U32) pow(2,u_i);
			}
		}

		/*If we have Injected ephemeris is smlc assistance and eph_toe expired then request for the same*/
		if((p_zCustom->custom_assist_availability_flag & C_EPH_AVAIL) && (FALSE == p_zSmlcAssist->z_SmlcRawEph[u_i].toe_expired ))
		{
			if(p_zSmlcAssist->z_SmlcRawEph[u_i].u_predicted)
			{
				p_prot_measurement->pred_eph_availability_flags |= (U32) pow(2, u_i);
			}
			else
			{
				p_prot_measurement->eph_availability_flags |= (U32) pow(2, u_i);
			}

		}

	}


	if(p_zCustom->custom_assist_availability_flag & C_RTI_AVAIL)
	{
		for(u_i=0;u_i<N_SV;u_i++)
		{
			if( p_zDBHealth->u_AlmHealth[u_i]==1 ||p_zDBHealth->u_AlmHealth[u_i]==3)
			{ // BAD or Does not exist
				p_prot_measurement->eph_availability_flags |= (U32) pow(2,u_i);

				MCP_HAL_STRING_Sprintf(svIdStr, " :%d", u_i );
				p_ephDbgStr = MCP_HAL_STRING_StrCat(p_ephDbgStr, svIdStr);
			}
		}
		STATUSMSG("SV Health N-OK: %s", p_ephDbgStr);
	}

	p_prot_measurement->eph_availability_flags = ~p_prot_measurement->eph_availability_flags;
	p_prot_measurement->pred_eph_availability_flags = ~p_prot_measurement->pred_eph_availability_flags;


	// Almanac bitmask
			p_prot_measurement->alm_availability_flags = 0;
			for (u_i=0;u_i<N_SV;u_i++)
				{
					if (p_DBAlmanac[u_i].u_Valid)
						{
							p_prot_measurement->alm_availability_flags |= (U32) pow(2,u_i);
						}

				}
		p_prot_measurement->alm_availability_flags = ~p_prot_measurement->alm_availability_flags;


    for (u_i=0,u_j=0; u_i < N_LCHAN; u_i++)
    {
      U8 u_SvId;
      p_zMeasPerSv     = &p_zMeasReport->z_MeasPerSv[u_i];

      u_SvId = (U8)(p_zMeasPerSv->u_SvIdSvTimeFlag & 0x3F);

      if (  u_SvId
         )
      {
        /* this is a good SV to report */


		w_IntTime = p_zMeasPerSv->w_PostInt * p_zMeasPerSv->u_PreInt;
		if(w_IntTime <= 4100)		w_Latency = 5001;
		else if  (w_IntTime <= 17000)			w_Latency = 17001; /* 16s integration */
		else			w_Latency = 32001;  /*int gr8 er than 16s*/

		/* check if it is good observation */
		// Unc check needs to be added
		if((p_zMeasPerSv->w_MeasStatus & MEAS_STATUS_SM_VALID)
			&& (~p_zMeasPerSv->w_MeasStatus & MEAS_STATUS_FROM_DIFF)
			&& (~p_zMeasPerSv->w_MeasStatus & MEAS_STATUS_DONT_USE)
			&& (~p_zMeasPerSv->w_MeasStatus & MEAS_STATUS_XCORR)
			&& (p_zMeasPerSv->x_LatencyMs < w_Latency)
			&&(p_zMeasPerSv->w_Cno >50) )
		{

			p_prot_measurement->c_sv_measurement++;


        /* SV id */

        if (u_j > GPSC_PROT_MAX_SV_MEASUREMENT) return;

		  p_sv_measurement = &p_prot_measurement->sv_measurement[u_j++]; 
	     p_sv_measurement->svid = (U8)(u_SvId-1);

        /* Cno. Add in noise figure and implementation loss (then round and
           switch from 0.1 to 1 dB-Hz/bit units) in order to reference it to
           the antenna input. */
        /* Cno round and switch from 0.1 to 1 dB-Hz/bit units)
		   in order to reference it to the antenna input. */
 //       p_sv_measurement->cno = (U8)((p_zMeasPerSv->w_Cno + 5) / 10);
 		p_sv_measurement->cno = p_zMeasPerSv->w_Cno;

            /* snr */
    	p_sv_measurement->snr = p_zMeasPerSv->w_Snr;

        /* doppler */
        /* f_Num = (FLT)p_zMeasPerSv->l_SvSpeed * (FLT)0.01
	         * (FLT)C_MsToL1Hz * (FLT)(-1.0); */ /* Doppler in Hz */
        /* Per Richard, adding frequency bias to calibrate doppler : */
        f_Num = (FLT)((DBL)p_zMeasPerSv->l_SvSpeed * 0.01 - d_clock_freq_bias)
              * (FLT)C_MsToL1Hz * (-1.0f);
        x_Word= (S16)(f_Num * 5.0f); /* resolution 0.2 Hz, use *5 for /0.2 */
		p_sv_measurement->doppler = x_Word;

         /*** We tell SMLC that the reference point of the measurement is
           at TOW, which is now contained in p_zGpsMsrSetElement->q_GpsTow_R24,
           but the actually GPS moment of the measurement is instead at
           the FCOUNT of the measurement, whose GPS msec is
           p_zMeasGpsTime->q_Msec - p_zMeasGpsTime->f_ClkBias.
           We can adjust each SV's submsec value by adding
           p_zMeasGpsTime->f_ClkBias to it. */

        d_AdjSubMsec = (DBL)p_zMeasPerSv->q_SubMsec
                     * C_LSB_SV_SUBMS + p_zMeasReport->z_MeasToa.f_ClkTimeBias;

        if (d_AdjSubMsec > 1.0 )
        {

          /* if the adjustment results in the submsec value above one, at the
             reference moment (TOW) SV time's msec should be one msec more,
             thus we need to subtract 1 ms from the submsec variable */

          d_AdjSubMsec -= 1.0;
        }

        else if  ( d_AdjSubMsec < 0 )
        {

          /** or if the adjustment results in submsec value negative, at the
            reference moment (TOW) SV time's msec should be one msec less,
            thus we need to use one minus the resuting "submsec" value for
            the submsec value */

          d_AdjSubMsec = 1.0 + d_AdjSubMsec;
        }

        /* both whole chip and fraction of a chip are based on "code phase", so
           convert submsec to code phase */
        d_AdjSubMsec = 1.0 - d_AdjSubMsec;

        /* whole chips: whole chip representation of the code phase */
        d_Num = d_AdjSubMsec / C_MSEC_PER_CHIP; /* whole chips in msec,
                                                   floating */
        p_sv_measurement->whole_chips = (U16)d_Num; /* whole chips in
                                                            msec, U10 */

        if ( p_sv_measurement->whole_chips > 1022 )
        {
          ERRORMSG("Error: WholeChip out of range" );
        }


        /* fraction of a chip: multiple of 1/1024 chips */
        d_Num = d_AdjSubMsec - (FLT)p_sv_measurement->whole_chips * (FLT)C_MSEC_PER_CHIP;
        p_sv_measurement->frac_chips = (U16)( d_Num / C_MSEC_FRAC_CHIP ) ;

        /* multipath indicator */
		u_HasMultipathInd = (U8)(((p_zMeasPerSv->w_MeasStatus) & 0x1000) >> 12);
		if (u_HasMultipathInd)
		{
			p_sv_measurement->multipath_indicator = (U8)(((p_zMeasPerSv->w_MeasStatus) & 0xC000) >> 14);
		}
		else
		{
			p_sv_measurement->multipath_indicator = 0;
		}

        /* PseudoRange RMS error: converted from SV time uncertainty */
        w_mantissa = (U16)(p_zMeasPerSv->w_SvTimeUnc >> 5);
        u_exponent = (U8)(p_zMeasPerSv->w_SvTimeUnc & 0x001F);
		    f_Num = (FLT)( ext_ldexp( (DBL)w_mantissa, u_exponent) ) *
			    (FLT)C_SPEED_LIGHT_NSEC;
        p_sv_measurement->pseudorange_rms_error = pseudoRMS_formatter ( f_Num );

        /* Elevation */
		p_sv_measurement->elevation = p_DBSvDirection->z_RawSvDirection[u_SvId-1].b_Elev;

		/* Azimuth */
		p_sv_measurement->azimuth = p_DBSvDirection->z_RawSvDirection[u_SvId-1].u_Azim;
      }
    }

}

  STATUSMSG("gpsc_populate_rrlp_meas:Sending: %d SVs",p_prot_measurement->c_sv_measurement);
}

/*
 ******************************************************************************
 *
 * psudoRMS_formatter
 *
 * Function description:
 *  This function converts the psedorange RMS ( in meters ) to an index-based
 *  representation defined by the RRLP protocol. The index-based representation
 *  uses a 6-bit index, in which the low 3 bits is the mantissa (x) and the
 *  high 3 bits is the exponent (y). And index of i states that the pseudo
 *  range RMS error (P) is within the range defined by Z (i-1) <= P < Z (i),
 *  where Z (i ) = 0.5 * (1 + x / 8 ) * 2 ** y.
 *
 * Parameters:
 *
 *  f_pr_rms_meters: psedorange RMS error in meters.
 *
 *
 * Return value:
 *
 *  The index (0..63) as defined by the RRLP protocol.
 *
 ******************************************************************************
*/

static U8 pseudoRMS_formatter ( FLT f_pr_rms_meters )
{
  FLT f_ValueGroup[64];
  U8 u_i, u_Mantissa_R3, u_Expo_R3;
  U16 w_Word;
  U8 u_ret = 0;

  for (u_i=0; u_i<64; u_i++)
  {

    u_Mantissa_R3 = (U8)(u_i & 0x07); /* Bit 0-2 of index: mantissa */
    u_Expo_R3 = (U8)((u_i >> 3) & 0x07); /* Bit 3-5 of index: exponent */

    w_Word = (U16)u_Mantissa_R3; /* creating a floating point number with
                      using U16, with its high byte
                    representing whole number and its low
                    byte reprenting fractional number */

    w_Word <<= 5; /* left-shift 8 for the high byte to represent whole
               number, and then right-shift 3 for dividing by 8,
                     up to here: x/8  */

    w_Word += 0x0100;  /* 1 + x / 8 */
    w_Word <<= u_Expo_R3; /* ( 1 + x/8 ) * 2 ** y  */
    w_Word >>= 1; /* 0.5 * ( 1 + x/8 ) * 2 ** y */


    /* LSB of the fractional part: 2 ** (-8 ) = 0.00390625 */
    f_ValueGroup[u_i] = (FLT)(w_Word >> 8) +
                      (FLT) (w_Word & 0xFF) * (FLT)0.00390625 ;
  }


  if ( f_pr_rms_meters < f_ValueGroup[0] )
  {
    u_ret = 0;
  }
  else if ( f_pr_rms_meters >= f_ValueGroup[62] )
  {
    u_ret = 63;
  }
  else
  {
    for (u_i=1; u_i < 63; u_i++)
    {
      if ( (f_pr_rms_meters < f_ValueGroup[u_i]) &&
         (f_pr_rms_meters >= f_ValueGroup[u_i - 1])
         )
      {
        u_ret = u_i;
        break;
      }
    }
  }
  return u_ret;
}


extern void handle_assistance_inject_req(T_GPSC_assistance_inject *assistance_inject)
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;

/* to avoid warning level 4: unreferenced formal parameter, assistance_inject is assigned to temp */
   UNREFERENCED_PARAMETER(assistance_inject);

   /* No location Request and only assistance has comes from network, process it */
   GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_ASSIST_INJECT);
}

extern T_GPSC_result gpsc_app_assist_inject_req (T_GPSC_assistance_inject* assistance_inject)
{
	gpsc_ctrl_type*	p_zGPSCControl =  gp_zGPSCControl;
	gpsc_state_type*	p_zGPSCState   =	p_zGPSCControl->p_zGPSCState;

	switch (assistance_inject->ctrl_assistance_inject_union)
	{
	    case GPSC_ASSIST_COMPL_SET:
			  p_zGPSCState->u_ReturnPending = TRUE;
			  ALOGI("+++ %s: Assistance set is complete, start the injection +++\n", __FUNCTION__);

			  handle_assistance_inject_req(assistance_inject);
			  p_zGPSCState->u_GPSCSubState = 0;

			  if (p_zGPSCState->u_ReturnPending)
			  {
			      SAPLEAVE(gpsc_app_assist_inject_req, GPSC_PENDING);
			  }
			  else
			  {
				   SAPLEAVE(gpsc_app_assist_inject_req, GPSC_SUCCESS);
			  }
			  break;

      case GPSC_ASSIST_ACQ :
		    gpsc_populate_aa_assist(p_zGPSCControl,
			   &assistance_inject->assistance_inject_union.acquisition_assist[0]);
		break;

		case GPSC_ASSIST_EPH :
		   gpsc_populate_eph_assist(p_zGPSCControl,
			   &assistance_inject->assistance_inject_union.ephemeris_assist[0]);
		break;

		case GPSC_ASSIST_IONO :
		   gpsc_populate_iono_assist(p_zGPSCControl,
			   &assistance_inject->assistance_inject_union.iono_assist);
		break;

		case GPSC_ASSIST_UTC :
			gpsc_populate_utc_assist(p_zGPSCControl,
			   &assistance_inject->assistance_inject_union.utc_assist);
		break;

		case GPSC_ASSIST_DGPS :
		   gpsc_populate_dgps_assist(p_zGPSCControl,
			   &assistance_inject->assistance_inject_union.dgps_assist);
		break;

		case GPSC_ASSIST_ALMANAC:
      {
		   gpsc_populate_alm_assist(p_zGPSCControl,
			   &assistance_inject->assistance_inject_union.almanac_assist[0]);
      }
		break;

		case GPSC_ASSIST_TOW :
		   gpsc_populate_tow_assist(p_zGPSCControl,
			   &assistance_inject->assistance_inject_union.tow_assist[0]);
		break;

		case GPSC_ASSIST_RTI :
		   gpsc_populate_rti_assist(p_zGPSCControl,
			   &assistance_inject->assistance_inject_union.rti_assist);
		break;

		case GPSC_ASSIST_POSITION :
		   gpsc_populate_pos_assist(p_zGPSCControl,
			   &assistance_inject->assistance_inject_union.position_assist);
		break;

		/* added to check the time availability  */
		case GPSC_ASSIST_TIME :
      {
		   gpsc_check_newtime_assist(p_zGPSCControl,
			   &assistance_inject->assistance_inject_union.time_assist);
      }
      break;
		/* added to check the time availability  */

	}

	SAPLEAVE(gpsc_app_assist_inject_req, GPSC_SUCCESS);
}

extern T_GPSC_result gpsc_app_assist_complete_req (void)
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	gpsc_state_type*	p_zGPSCState = p_zGPSCControl->p_zGPSCState;

   T_GPSC_result ret = GPSC_SUCCESS;

	switch (p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq)
	{
	case  E_GPSC_SEQ_SESSION_GET_ASSISTANCE:
	{
		STATUSMSG("Applying SMLC assist data: avail list = 0x%x",
						p_zGPSCControl->p_zSmlcAssist->w_AvailabilityFlags);
		p_zGPSCState->u_GPSCSubState = 0;

		p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE;

		gpsc_session_sequence(p_zGPSCControl, E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE);
	}
   break;

	case  E_GPSC_SEQ_SESSION_REQUEST_TIME:
	{
		STATUSMSG("Applying Requested Time assist data: avail list = 0x%x",
						p_zGPSCControl->p_zSmlcAssist->w_AvailabilityFlags);
		p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE;

		gpsc_session_sequence(p_zGPSCControl,E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE);
	}
   break;

	case  E_GPSC_SEQ_SESSION_WAIT_FOR_LOCATION_REQUEST:
	{
		STATUSMSG("Applying Wait SVD assist data: avail list = 0x%x",
						p_zGPSCControl->p_zSmlcAssist->w_AvailabilityFlags);
		p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE;

		gpsc_session_sequence(p_zGPSCControl,E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE);
	}
   break;

	case  E_GPSC_SEQ_SESSION_CONFIG:
	{
		STATUSMSG("Applying Assistance from Session Config assist data: avail list = 0x%x",
						p_zGPSCControl->p_zSmlcAssist->w_AvailabilityFlags);
		p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE;

		gpsc_session_sequence(p_zGPSCControl, E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE);
	}
   break;

	case  E_GPSC_SEQ_SESSION_ON:
	{
		STATUSMSG("Applying Assistance from Session ON assist data: avail list = 0x%x",
						p_zGPSCControl->p_zSmlcAssist->w_AvailabilityFlags);
		p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE;

		gpsc_session_sequence(p_zGPSCControl, E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE);
	}
   break;

	default:
		ALOGE("+++ %s: not expected in this sequence +++\n", __FUNCTION__);
		ret = GPSC_FAIL;
	}

   return ret;
}

extern T_GPSC_result gpsc_app_inject_ref_clock_parameters_req (U16 ref_clock_quality, U32 ref_clock_frequency)
{

	gpsc_ctrl_type*	p_zGPSCControl;
	gpsc_state_type*	p_zGPSCState;
	gpsc_cfg_type*		p_zGPSCConfig;

	SAPENTER(gpsc_app_inject_ref_clock_parameters_req);

	p_zGPSCControl = gp_zGPSCControl;
	p_zGPSCState =	p_zGPSCControl->p_zGPSCState;
	p_zGPSCConfig = p_zGPSCControl->p_zGPSCConfig;

	if(ref_clock_quality >  p_zGPSCConfig->ref_clock_quality)
		p_zGPSCState->u_ClockQualInjectPending  = TRUE;


	p_zGPSCConfig->ref_clock_quality = ref_clock_quality;
	p_zGPSCConfig->ref_clock_frequency = ref_clock_frequency;

	if(p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_SESSION_ON)
	{
		/*u_ClockQualInjectPending will be reset inside this function*/
		gpsc_mgp_inject_OscParams(p_zGPSCControl);
		if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
				SAPLEAVE(gpsc_app_inject_ref_clock_parameters_req, GPSC_TX_INITIATE_FAIL)
	}
	SAPLEAVE(gpsc_app_inject_ref_clock_parameters_req, GPSC_SUCCESS)
}

extern T_GPSC_result gpsc_app_inject_time_stamp_parameters_req (U16 timestamp_period, U32 timepulse_period_unc)
{

	gpsc_ctrl_type*	p_zGPSCControl;

	gpsc_cfg_type*		p_zGPSCConfig;

	SAPENTER(gpsc_app_inject_time_stamp_parameters_req);

	p_zGPSCControl = gp_zGPSCControl;
	p_zGPSCConfig = p_zGPSCControl->p_zGPSCConfig;

	p_zGPSCConfig->calib_period = timestamp_period;
	p_zGPSCConfig->period_uncertainity = (U16)timepulse_period_unc;

	if(p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_SESSION_ON)
	{
		gpsc_mgp_inject_CalibTimestamp_Period(p_zGPSCControl);
		if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
				SAPLEAVE(gpsc_app_inject_time_stamp_parameters_req, GPSC_TX_INITIATE_FAIL)
	}
	SAPLEAVE(gpsc_app_inject_time_stamp_parameters_req, GPSC_SUCCESS)
}

void gpsc_populate_aa_assist
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_acquisition_assist* p_acquisition_assist
)
{
	gpsc_smlc_assist_type* p_zSmlcAssist = p_zGPSCControl->p_zSmlcAssist;
	gpsc_acq_assist_type *p_zAcquisAssist = &p_zSmlcAssist->z_AcquisAssist;
    gpsc_assist_wishlist_type*  p_zGpsAssisWishlist = p_zGPSCControl->p_zGpsAssisWishlist;
	gpsc_custom_struct* p_zCustom = p_zGPSCControl->p_zCustomStruct;
	gpsc_acq_element_type   *p_zAcquisElement;
    gpsc_additional_angle_type *p_zAddAngle;
	T_GPSC_time_assist z_CurrentTime;
	//TNAVCD_Ctrl		*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl;  /* Klocwork Changes */
    U8 u_i;
	U8 u_j;
	McpU8   aaDbgStr[250] =" ";
	McpU8 *p_aaDbgStr = aaDbgStr;
	McpU8   aaRxCnt =0;
	McpS8 svIdStr[5];


	for(u_j=0;u_j<N_SV_15;u_j++,p_acquisition_assist++)
	{
		/* typically, the injected struct should contain an index which
		 *  indicates the number of AA in the  aray, but since this is
		 *  not there, we will need to check the svid of every array
		 *  element, to identify if a given element is populated with a valid AA
		 */
		if ((p_acquisition_assist->svid != 0xFF) &&
			((p_acquisition_assist->svid >= 0) &&
			(p_acquisition_assist->svid <= 31)))
	  {

			if (p_zAcquisAssist->u_NumSv < C_MAX_SVS_ACQ_ASSIST)
		u_i = p_zAcquisAssist->u_NumSv;
			else
				return; // the smlc DB is full

		p_zAcquisElement = &p_zAcquisAssist->z_AcquisElement[u_i];
		p_zAddAngle  = &p_zAcquisAssist->z_AddAngle[u_i];

	p_zAcquisAssist->u_NumSv++;

			/* SMLC gives multiple of 80 msec */
			p_zAcquisElement->q_GpsTow = p_acquisition_assist->gps_tow * 80;

	p_zAcquisElement->u_SvId = (U8)(p_acquisition_assist->svid+1);
	p_zAcquisElement->x_Doppler0 = p_acquisition_assist->aa_doppler0;

	if(p_acquisition_assist->v_aa_doppler1)
		p_zAcquisElement->u_Doppler1 = p_acquisition_assist->aa_doppler1;
	else
		p_zAcquisElement->u_Doppler1 = 0xFF;

	if(p_acquisition_assist->v_aa_doppler_uncertainty)
		p_zAcquisElement->u_DopplerUnc = p_acquisition_assist->aa_doppler_uncertainty;
	else
		p_zAcquisElement->u_DopplerUnc =0xFF;

	p_zAcquisElement->u_GpsBitNum = p_acquisition_assist->aa_gps_bit_number;
	p_zAcquisElement->u_IntCodePhase = p_acquisition_assist->aa_int_code_phase;
	p_zAcquisElement->w_CodePhase = p_acquisition_assist->aa_code_phase;
	p_zAcquisElement->u_SrchWin = p_acquisition_assist->aa_code_phase_search_window;

	aaRxCnt++;
	MCP_HAL_STRING_Sprintf(svIdStr, " :%d", p_zAcquisElement->u_SvId );
	p_aaDbgStr = MCP_HAL_STRING_StrCat(p_aaDbgStr, svIdStr);
	if(p_acquisition_assist->v_aa_azimuth)
		p_zAddAngle->u_Azimuth = p_acquisition_assist->aa_azimuth;
	else
		p_zAddAngle->u_Azimuth = 0xFF;

	if(p_acquisition_assist->v_aa_elevation)
	{
		p_zSmlcAssist->w_AvailabilityFlags |= C_ANGLE_AVAIL;
		p_zCustom->custom_assist_availability_flag |= C_ANGLE_AVAIL;
		p_zAddAngle->u_Elevation = p_acquisition_assist->aa_elevation;
	}
	else
		p_zAddAngle->u_Elevation = 0xFF;

	/*Add to list of visible SVs*/
	p_zGPSCControl->p_zGPSCState->z_SessionStatus.u_AcqAsstListedSVs
                |=1 <<(p_zAcquisElement->u_SvId-1);

			STATUSMSG("Status: Received Acquisition Assist for Sv %d",p_zAcquisElement->u_SvId);

	  }
	}

	p_zSmlcAssist->w_AvailabilityFlags |= C_ACQ_AVAIL;
	p_zGpsAssisWishlist->w_Wishlist &= ~GPSC_REQ_AA;

	p_zCustom->custom_assist_availability_flag |= C_ACQ_AVAIL;

#ifndef CLIENT_CUSTOM
	//custom comment time assist flags
	p_zSmlcAssist->w_AvailabilityFlags |= C_REF_GPSTIME_AVAIL;
	p_zGpsAssisWishlist->w_Wishlist &= ~GPSC_REQ_TIME;
#endif

z_CurrentTime.time_accuracy = GPSC_TIME_ACCURACY_COARSE;
	if(p_zGPSCControl->p_zGPSCDatabase->z_DBGpsClock.w_GpsWeek == C_GPS_WEEK_UNKNOWN)
	{
		if(gpsc_time_request_tow_ind(&z_CurrentTime)==GPSC_SUCCESS)
		{
			if(z_CurrentTime.gps_week != C_GPS_WEEK_UNKNOWN )
			{
				SESSLOGMSG("[%s]%s:%d, %d,%d,%s, %s \n",get_utc_time(),SESS_EPH_AIDING,nav_sess_getCount(0),aaRxCnt,z_CurrentTime.gps_week,p_aaDbgStr,"#AA Aiding num_sat,w_GpsWeek,SV IDs");
			}
			else
			{
				SESSLOGMSG("[%s]%s:%d, %d,%d,%s, %s \n",get_utc_time(),SESS_EPH_AIDING,nav_sess_getCount(0),aaRxCnt,-1,p_aaDbgStr,"#AA Aiding num_sat,w_GpsWeek,SV IDs");
			}
		}
		else
		{
			STATUSMSG("SessLog: gpsc_populate_aa_assist: gpsc_time_request_tow_ind Failed!!");

		}

	}
	else
	{
		SESSLOGMSG("[%s]%s:%d, %d,%d,%s, %s \n",get_utc_time(),SESS_EPH_AIDING,nav_sess_getCount(0),aaRxCnt,p_zGPSCControl->p_zGPSCDatabase->z_DBGpsClock.w_GpsWeek,p_aaDbgStr,"#AA Aiding num_sat,w_GpsWeek,SV IDs");
	}





}

gpsc_toe_validity_bounds get_toe_validity_bounds(U32 toe_val)
{
	gpsc_toe_validity_bounds toe_validity;

	if(toe_val < C_GOOD_EPH_AGE_SEC) {
		/* TOE validity lower bound wraps with previous week */
		STATUSMSG("get_toe_validity_bounds : TOE validity lower bound wraps with previous week");
		toe_validity.toe_lower_bound_min = (U32)(WEEK_SECS - (C_GOOD_EPH_AGE_SEC - toe_val));
		toe_validity.toe_lower_bound_max = WEEK_SECS;
		toe_validity.toe_upper_bound_min = 0;
		toe_validity.toe_upper_bound_max = (toe_val + C_GOOD_EPH_AGE_SEC);
	} else if(toe_val > (WEEK_SECS - C_GOOD_EPH_AGE_SEC)) {
		/* TOE validity upper bound wraps with next week */
		STATUSMSG("get_toe_validity_bounds : TOE validity upper bound wraps with next week");
		toe_validity.toe_lower_bound_min = (toe_val - C_GOOD_EPH_AGE_SEC);
		toe_validity.toe_lower_bound_max = WEEK_SECS;
		toe_validity.toe_upper_bound_min = 0;
		toe_validity.toe_upper_bound_max = (C_GOOD_EPH_AGE_SEC - (WEEK_SECS - toe_val));
	}else {
		/* TOE validity within current week */
		STATUSMSG("get_toe_validity_bounds : TOE validity within current week");
		toe_validity.toe_lower_bound_min = (toe_val - C_GOOD_EPH_AGE_SEC);
		toe_validity.toe_lower_bound_max = toe_val;
		toe_validity.toe_upper_bound_min = toe_val;
		toe_validity.toe_upper_bound_max = (toe_val + C_GOOD_EPH_AGE_SEC);
	}

	return toe_validity;
}

U8 is_eph_valid_for_gpssec(U32 eph_toe, U32 gpssec)
{
	gpsc_toe_validity_bounds curr_toe_validity;

	STATUSMSG("is_eph_valid_for_gpssec : TOE = %d, GPS Sec = %d", eph_toe, gpssec);
	/* Get the validity bounds for the TOE considering week wrap arounds */
	curr_toe_validity = get_toe_validity_bounds(eph_toe);

	STATUSMSG("TOE Validity: LowerBoundMin : %d, LowerBoundMax : %d, UpperBoundMin : %d, UpperBoundMax : %d",
				curr_toe_validity.toe_lower_bound_min,
				curr_toe_validity.toe_lower_bound_max,
				curr_toe_validity.toe_upper_bound_min,
				curr_toe_validity.toe_upper_bound_max);

	/* If the absolute value of current GPS Second is within the validity range,
	 * then the ephemeris is valid
	 */
	if(((gpssec >= curr_toe_validity.toe_lower_bound_min) &&
		 (gpssec <= curr_toe_validity.toe_lower_bound_max))
		 ||
	   ((gpssec >= curr_toe_validity.toe_upper_bound_min) &&
		 (gpssec <= curr_toe_validity.toe_upper_bound_max)))
	{
		STATUSMSG("Ephemeris Valid");
		return TRUE;
	}
	else
	{
		STATUSMSG("Ephemeris Expired");
		return FALSE;
	}
}


void gpsc_populate_eph_assist
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_ephemeris_assist* p_ephemeris_assist
)
{
	gpsc_smlc_assist_type* p_zSmlcAssist = p_zGPSCControl->p_zSmlcAssist;
	gpsc_assist_wishlist_type*  p_zGpsAssisWishlist = p_zGPSCControl->p_zGpsAssisWishlist;
	pe_RawEphemeris *p_zSmlcRawEph;
    	pe_RawSF1Param  *p_zSmlcRawSF1Param;
	gpsc_custom_struct* p_zCustom = p_zGPSCControl->p_zCustomStruct;
	gpsc_db_type *p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	U8 u_i, u_Svid, u_injectedpredephe = 0;

	//McpU16 latestToe = 0; /* Klocwork Changes */

	McpU8   ephDbgStr[250] =" ", ephRjEbgStr[250]=" ";
	McpU8 *p_ephDbgStr = ephDbgStr, *p_ephRjDbgStr = ephRjEbgStr;
	McpU8   ephRxCnt =0;
	McpU8  ephRjCnt = 0;
	McpU8  ephToReject = FALSE;
	McpS8 svIdStr[5], snIdRjStr[5];
	U32  ephFullRelToe =0, prevEphFullRelToe = 0;
	U32 currentAccGpsSec = 0;
	gpsc_db_gps_clk_type* p_zDBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;
	T_GPSC_time_assist z_CurrentTime;
	McpU8 isEphValid = FALSE;
    gpsc_db_eph_type *p_DBEphemeris;
	pe_RawEphemeris *p_RawEph;

	//TNAVCD_Ctrl		*pNavc = (TNAVCD_Ctrl *)p_zGPSCControl->p_zSysHandlers->hNavcCtrl;  /* Klocwork Changes */
		
	if (p_zDBGpsClock->u_Valid == TRUE)
		{
			currentAccGpsSec  = (U32)(p_zDBGpsClock->q_GpsMs / 1000);
		}

	for(u_i=0;u_i<N_SV_15;u_i++,p_ephemeris_assist++)
	{
		if (p_ephemeris_assist->svid != 0xFF)
		{
			ephToReject = FALSE;

					u_Svid = (U8)(p_ephemeris_assist->svid+1);

					p_zSmlcRawEph = &p_zSmlcAssist->z_SmlcRawEph[u_Svid-1];
					p_zSmlcRawSF1Param = &p_zSmlcAssist->z_SmlcRawSF1Param[u_Svid-1];

			if (p_zDBGpsClock->u_Valid == TRUE)
			{
				ephFullRelToe = (U32) (p_ephemeris_assist->ephem_toe * 16);

				/* Check the validity of the ephemeris for the current GPS second */
				isEphValid = is_eph_valid_for_gpssec(ephFullRelToe, currentAccGpsSec);

				if(isEphValid == TRUE)
				{
					ephToReject = TRUE;

					/* If GPS time is valid, 
					 * When new assistance is available (BCE/EE) with a greater time validity 
					 * than the existing one, the new one is populated, else rejected.
					 */

					/* Check if ephmeris in the database is valid */
					p_DBEphemeris = &p_zGPSCDatabase->z_DBEphemeris[ u_Svid - 1 ];
                    p_RawEph = &p_zGPSCDatabase->z_DBEphemeris[ u_Svid - 1 ].z_RawEphemeris;

					if(p_DBEphemeris->u_Valid == TRUE)
					{
						STATUSMSG("Compare current eph TOE with Database eph");
						prevEphFullRelToe = (U32)(p_RawEph->w_Toe * 16);
					}
					else
					{
						/* If Ephemeris is available previously, 
						 * Check if new ephemeris needs to be injected or not.
						 */
						if(p_zSmlcRawEph->u_available == TRUE)
						{
							STATUSMSG("Compare current eph TOE with previous available eph");
							prevEphFullRelToe = (U32)(p_zSmlcRawEph->w_Toe * 16);
						}
						else
						{
							STATUSMSG("Eph unavailable, Use as is");
							ephToReject = FALSE;
						}
					}

					if(ephToReject == TRUE)
					{
						/* Check the validity of the previous ephemeris for the current GPS second */
						isEphValid = is_eph_valid_for_gpssec(prevEphFullRelToe, currentAccGpsSec);

						if(isEphValid == TRUE)
						{
							if(ephFullRelToe <= prevEphFullRelToe)
							{
								/* Current ephemeris TOE may be lower as it may have wrapped to next week */
								if((ephFullRelToe < (2 * C_GOOD_EPH_AGE_SEC)) && (prevEphFullRelToe > (WEEK_SECS - (2 * C_GOOD_EPH_AGE_SEC))))
								{
									STATUSMSG("Current TOE has wrapped to next week");
									STATUSMSG("Previous TOE older. Inject current");
									ephToReject = FALSE;
								}
								else
								{
									STATUSMSG("Rejecting ephemeris as existing one has greater time validity");
									ephToReject = TRUE;
								}
							}
							else
							{
								/* Current ephemeris TOE may be higher although stale as previous TOE may have wrapped to next week */
								if((prevEphFullRelToe < (2 * C_GOOD_EPH_AGE_SEC)) && (ephFullRelToe > (WEEK_SECS - (2 * C_GOOD_EPH_AGE_SEC))))
								{
									STATUSMSG("Previous TOE has wrapped to next week. Hence current is stale");
									STATUSMSG("Rejecting ephemeris as existing one has greater time validity");
									ephToReject = TRUE;
								}
								else
								{
									STATUSMSG("Previous TOE older. Inject current");
									ephToReject = FALSE;
								}
							}
						}
						else
						{
							STATUSMSG("Previous TOE not valid. Inject current");
							ephToReject = FALSE;
						}
					}
				}
			}
			else
			{
				STATUSMSG("gpsc_populate_eph_assist : p_zDBGpsClock->u_Valid is FALSE, Populate ephemeris");

#if 0	/* Disabling the check. Need to revisit the condition to validate available eph. Use system time?? */
				/* If GPS time is not valid, 
				 * if previously a Broadcast ephemeris is injected and current is predicted do not replace
				 * Else replace with current ephemeris
				 */
				if(p_zSmlcRawEph->u_available == TRUE)
				{
					if(((p_zCustom->custom_pred_eph_bitmap >> u_Svid) & 0x1) == 0)
					{
						if(p_ephemeris_assist->ephem_predicted)
						{
							STATUSMSG("Rejecting ephemeris as existing one seems to be BCE and GpsClock is not valid");
							ephToReject = TRUE;
						}
					}
				}
#endif
			}

			if(ephToReject == TRUE)
			{
				/* Reject the current ephemeris */
				ephRjCnt++;
				//MCP_HAL_STRING_Sprintf(snIdRjStr, " :%d", p_ephemeris_assist->svid+1 );
				//p_ephRjDbgStr = MCP_HAL_STRING_StrCat(p_ephRjDbgStr, snIdRjStr);

				STATUSMSG("Rejected Eph Sv : %d ", p_ephemeris_assist->svid+1);

				/* Go onto check ephemeris for next SV */
				continue;
			}

				
			p_zSmlcAssist->u_nSvEph++;  /* increment the counter */

					p_zSmlcRawEph->u_Prn = u_Svid;
					p_zSmlcRawEph->q_E = p_ephemeris_assist->ephem_e;
					p_zSmlcRawEph->q_IZero = p_ephemeris_assist->ephem_i0;
					p_zSmlcRawEph->q_MZero = p_ephemeris_assist->ephem_m0 ;
					p_zSmlcRawEph->q_Omega = p_ephemeris_assist->ephem_w;
					p_zSmlcRawEph->q_OmegaDot = p_ephemeris_assist->ephem_omega_adot;
					p_zSmlcRawEph->q_OmegaZero = p_ephemeris_assist->ephem_omega_a0;
					p_zSmlcRawEph->q_SqrtA = p_ephemeris_assist->ephem_a_power_half;
					p_zSmlcRawEph->u_Iode = (U8)p_ephemeris_assist->ephem_iodc;
					p_zSmlcRawEph->w_Cic = p_ephemeris_assist->ephem_cic;
					p_zSmlcRawEph->w_Cis = p_ephemeris_assist->ephem_cis;
					p_zSmlcRawEph->w_Crc = p_ephemeris_assist->ephem_crc;
					p_zSmlcRawEph->w_Crs = p_ephemeris_assist->ephem_crs;
					p_zSmlcRawEph->w_Cuc = p_ephemeris_assist->ephem_cuc;
					p_zSmlcRawEph->w_Cus = p_ephemeris_assist->ephem_cus;
					p_zSmlcRawEph->w_DeltaN = p_ephemeris_assist->ephem_delta_n;
					p_zSmlcRawEph->w_IDot = p_ephemeris_assist->ephem_idot;
					p_zSmlcRawEph->w_Toe = p_ephemeris_assist->ephem_toe;
					p_zSmlcRawEph->u_predicted =  p_ephemeris_assist->ephem_predicted;
					p_zSmlcRawEph->u_predSeedAge =  p_ephemeris_assist->ephem_predSeedAge;

					p_zSmlcRawSF1Param->q_Af0 = p_ephemeris_assist->ephem_af0;
					p_zSmlcRawSF1Param->u_Accuracy = p_ephemeris_assist->ephem_ura;
					p_zSmlcRawSF1Param->u_Af2 = p_ephemeris_assist->ephem_af2;
					p_zSmlcRawSF1Param->u_CodeL2 = p_ephemeris_assist->ephem_code_on_l2;
					p_zSmlcRawSF1Param->u_Health = p_ephemeris_assist->ephem_svhealth;
					p_zSmlcRawSF1Param->u_Tgd = p_ephemeris_assist->ephem_tgd;
					p_zSmlcRawSF1Param->w_Af1 = p_ephemeris_assist->ephem_af1;
					p_zSmlcRawSF1Param->w_Iodc = p_ephemeris_assist->ephem_iodc;
					p_zSmlcRawSF1Param->w_Toc = p_ephemeris_assist->ephem_toc;
					/*set bitmap for predicted*/
					if (p_ephemeris_assist->ephem_predicted)
					{
						p_zCustom->custom_pred_eph_bitmap |= (U32) pow(2,u_Svid);
#ifdef CLIENT_CUSTOM
						// Add 7 to the ura
						// so that the receiver can identify it as a predicted EPH
						// applicable for SP81.XX
						p_zSmlcRawSF1Param->u_Accuracy += 7;
#else
						// set the first bit of the ura field as '1'
						// so that the receiver can identify it as a predicted EPH
						// applicable for SP11.XX
						p_zSmlcRawSF1Param->u_Accuracy |= (1 << 7);
						u_injectedpredephe ++;
#endif
					}
					else
					{
						p_zCustom->custom_pred_eph_bitmap &= ~((U32) pow(2,u_Svid));
					}

					/*Store time of Eph Rx*/
					p_zSmlcRawEph->q_sysTimeRx = mcpf_getCurrentTime_inSec(p_zGPSCControl->p_zSysHandlers->hMcpf);
					p_zSmlcRawEph->toe_expired = FALSE;

					p_zSmlcRawEph->u_available = TRUE;

					ephRxCnt++;
					MCP_HAL_STRING_Sprintf(svIdStr, " :%d", u_Svid );
					p_ephDbgStr = MCP_HAL_STRING_StrCat(p_ephDbgStr, svIdStr);

					p_zSmlcAssist->w_AvailabilityFlags |= C_EPH_AVAIL;
					p_zGpsAssisWishlist->w_Wishlist &= ~GPSC_REQ_NAV;
					p_zCustom->custom_assist_availability_flag |= C_EPH_AVAIL;

				}
	}

	STATUSMSG("Received Ephemeris for %d SVs IDs>> %s; Rejected: %d SVs IDs: ",
		ephRxCnt, p_ephDbgStr, ephRjCnt, p_ephRjDbgStr);

	z_CurrentTime.time_accuracy = GPSC_TIME_ACCURACY_COARSE;
	if(p_zDBGpsClock->w_GpsWeek == C_GPS_WEEK_UNKNOWN)
	{
		if(gpsc_time_request_tow_ind(&z_CurrentTime)==GPSC_SUCCESS)
		{
			if(z_CurrentTime.gps_week != C_GPS_WEEK_UNKNOWN )
			{
				SESSLOGMSG("[%s]%s:%d, %d,%d, %s, %s \n",get_utc_time(),SESS_EPH_AIDING,nav_sess_getCount(0),ephRxCnt,z_CurrentTime.gps_week,p_ephDbgStr,"#EPH Aiding num_sat,GpsWeek,toe,SV IDs");
			}
			else
			{
				SESSLOGMSG("[%s]%s:%d, %d,%d, %s, %s \n",get_utc_time(),SESS_EPH_AIDING,nav_sess_getCount(0),ephRxCnt,-1,p_ephDbgStr,"#EPH Aiding num_sat,GpsWeek,toe,SV IDs");
			}
		}
		else
		{
			STATUSMSG("SessLog: gpsc_populate_eph_assist: gpsc_time_request_tow_ind Failed!!");

		}

	}
	else
	{
		SESSLOGMSG("[%s]%s:%d, %d,%d, %s, %s \n",get_utc_time(),SESS_EPH_AIDING,nav_sess_getCount(0),ephRxCnt,p_zDBGpsClock->w_GpsWeek,p_ephDbgStr,"#EPH Aiding num_sat,GpsWeek,toe,SV IDs");
	}

    STATUSMSG("SessLog: [%s]%s:%d, %d,%d, %s, %s \n",get_utc_time(),SESS_EPH_AIDING,nav_sess_getCount(0),ephRxCnt,p_zDBGpsClock->w_GpsWeek,p_ephDbgStr,"#EPH Aiding num_sat,GpsWeek,toe,SV IDs");

}
void gpsc_populate_iono_assist
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_iono_assist*  p_iono_assist
)
{
	gpsc_smlc_assist_type* p_zSmlcAssist = p_zGPSCControl->p_zSmlcAssist;
	gpsc_assist_wishlist_type*  p_zGpsAssisWishlist = p_zGPSCControl->p_zGpsAssisWishlist;
	gpsc_raw_iono_type* p_zSmlcRawIono  = &p_zSmlcAssist->z_SmlcRawIono;
	gpsc_custom_struct* p_zCustom = p_zGPSCControl->p_zCustomStruct;

	STATUSMSG("Status: Received Iono");
	p_zSmlcRawIono->u_Alpha0 = p_iono_assist->iono_alfa0;
	p_zSmlcRawIono->u_Alpha1 = p_iono_assist->iono_alfa1;
	p_zSmlcRawIono->u_Alpha2 = p_iono_assist->iono_alfa2;
	p_zSmlcRawIono->u_Alpha3 = p_iono_assist->iono_alfa3;
	p_zSmlcRawIono->u_Beta0 = p_iono_assist->iono_beta0;
	p_zSmlcRawIono->u_Beta1 = p_iono_assist->iono_beta1;
	p_zSmlcRawIono->u_Beta2 = p_iono_assist->iono_beta2;
	p_zSmlcRawIono->u_Beta3 = p_iono_assist->iono_beta3;

	p_zSmlcAssist->w_AvailabilityFlags |= C_IONO_AVAIL;
	p_zGpsAssisWishlist->w_Wishlist &= ~GPSC_REQ_IONO;
	p_zCustom->custom_assist_availability_flag |= C_IONO_AVAIL;
}

void gpsc_populate_alm_assist
(
    gpsc_ctrl_type* p_zGPSCControl,
    T_GPSC_almanac_assist* p_almanac_assist
)
{

	gpsc_cfg_type*	p_zGPSCConfig	= p_zGPSCControl->p_zGPSCConfig;
	gpsc_smlc_assist_type* p_zSmlcAssist = p_zGPSCControl->p_zSmlcAssist;
	gpsc_assist_wishlist_type*  p_zGpsAssisWishlist = p_zGPSCControl->p_zGpsAssisWishlist;
	pe_RawAlmanac *p_zSmlcRawAlmanac;
	gpsc_custom_struct* p_zCustom = p_zGPSCControl->p_zCustomStruct;
	U8 u_i = 0;
	U8 u_Svid = 0;

	for (u_i = 0; u_i < N_SV; u_i++, p_almanac_assist++)
	{
		if (p_almanac_assist->svid != 0xFF)
		{
			u_Svid = (U8)(p_almanac_assist->svid + 1);
	      ALOGI("+++ %s: Received Almanac for Sv %d +++\n", __FUNCTION__, u_Svid);

			p_zSmlcRawAlmanac = &p_zSmlcAssist->z_SmlcRawAlmanac[u_Svid-1];

	      p_zSmlcRawAlmanac->u_Prn = u_Svid;
	      p_zSmlcRawAlmanac->w_Week = p_almanac_assist->almanac_week;
	      p_zSmlcRawAlmanac->q_MZero = p_almanac_assist->almanac_m0;
	      p_zSmlcRawAlmanac->q_Omega = p_almanac_assist->almanac_w;
	      p_zSmlcRawAlmanac->q_OmegaZero = p_almanac_assist->almanac_omega0;
	      p_zSmlcRawAlmanac->q_SqrtA = p_almanac_assist->almanac_a_power_half;
	      p_zSmlcRawAlmanac->u_Health = p_almanac_assist->almanac_svhealth;
	      p_zSmlcRawAlmanac->u_Toa = p_almanac_assist->almanac_toa;
	      p_zSmlcRawAlmanac->w_Af0 = p_almanac_assist->almanac_af0;
	      p_zSmlcRawAlmanac->w_Af1 = p_almanac_assist->almanac_af1;
	      p_zSmlcRawAlmanac->w_DeltaI = p_almanac_assist->almanac_ksii;
	      p_zSmlcRawAlmanac->w_E = p_almanac_assist->almanac_e;
	      p_zSmlcRawAlmanac->w_OmegaDot = p_almanac_assist->almanac_omega_dot;
		}
	}

	p_zSmlcAssist->w_AvailabilityFlags |= C_ALM_AVAIL;
	p_zGpsAssisWishlist->w_Wishlist &= ~GPSC_REQ_ALMANAC;

    /* to block almanac in 3gpp test */
	p_zSmlcAssist->w_AvailabilityFlags &=  ~(p_zGPSCConfig->block_almanac);
	p_zCustom->custom_assist_availability_flag |= C_ALM_AVAIL;

}
void gpsc_populate_utc_assist
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_utc_assist* p_utc_assist
)
{

	gpsc_smlc_assist_type* p_zSmlcAssist = p_zGPSCControl->p_zSmlcAssist;
  	gpsc_assist_wishlist_type*  p_zGpsAssisWishlist = p_zGPSCControl->p_zGpsAssisWishlist;
  	gpsc_raw_utc_type* p_zSmlcRawUtc  = &p_zSmlcAssist->z_SmlcRawUtc;
	gpsc_custom_struct* p_zCustom = p_zGPSCControl->p_zCustomStruct;

   	STATUSMSG("Status: Received Utc");
  	p_zSmlcRawUtc->utc_a1 = p_utc_assist->utc_a1;
  	p_zSmlcRawUtc->utc_a0 = p_utc_assist->utc_a0;
  	p_zSmlcRawUtc->utc_tot = p_utc_assist->utc_tot;
  	p_zSmlcRawUtc->utc_wnt = p_utc_assist->utc_wnt;
  	p_zSmlcRawUtc->utc_delta_tls = p_utc_assist->utc_delta_tls;
  	p_zSmlcRawUtc->utc_wnlsf = p_utc_assist->utc_wnlsf;
  	p_zSmlcRawUtc->utc_dn = p_utc_assist->utc_dn;
  	p_zSmlcRawUtc->utc_delta_tlsf = p_utc_assist->utc_delta_tlsf;

  	p_zSmlcAssist->w_AvailabilityFlags |= C_UTC_AVAIL;

	p_zGpsAssisWishlist->w_Wishlist &= ~GPSC_REQ_UTC;
	p_zCustom->custom_assist_availability_flag |= C_UTC_AVAIL;

}


/* This function is added for DGPS Support : 26/07/2006 --Raghavendra */
void gpsc_populate_dgps_assist
(
   gpsc_ctrl_type* p_zGPSCControl,
   T_GPSC_dgps_assist* p_dgps_assist
)
{
	 gpsc_smlc_assist_type* p_zSmlcAssist = p_zGPSCControl->p_zSmlcAssist;
	 gpsc_dgps_assist_type* p_zDgpsAssist  = &p_zSmlcAssist->z_DgpsAssist;
	 gpsc_assist_wishlist_type*  p_zGpsAssisWishlist = p_zGPSCControl->p_zGpsAssisWishlist;
	 gpsc_custom_struct* p_zCustom = p_zGPSCControl->p_zCustomStruct;

	 U8 u_Svid = (U8)(p_dgps_assist->dgps.svid);
	 dgps_assist* p_zdgps = &p_zSmlcAssist->z_DgpsAssist.z_dgps[u_Svid];

	 p_zDgpsAssist->q_gps_tow = p_dgps_assist->gps_tow;
	 p_zDgpsAssist->u_dgps_status = p_dgps_assist->dgps_status;
	 p_zDgpsAssist->u_dgps_Nsat = p_dgps_assist->dgps_Nsat;

    p_zdgps->u_svid = p_dgps_assist->dgps.svid;
    p_zdgps->u_iode = p_dgps_assist->dgps.iode;
	 p_zdgps->u_dgps_udre = p_dgps_assist->dgps.dgps_udre;
	 p_zdgps->x_dgps_pseudo_range_cor = p_dgps_assist->dgps.dgps_pseudo_range_cor;
	 p_zdgps->b_dgps_range_rate_cor =  p_dgps_assist->dgps.dgps_range_rate_cor;
	 p_zdgps->b_dgps_deltapseudo_range_cor2 = p_dgps_assist->dgps.dgps_deltapseudo_range_cor2;
	 p_zdgps->b_dgps_deltarange_rate_cor2 = p_dgps_assist->dgps.dgps_deltarange_rate_cor2;
	 p_zdgps->b_dgps_deltapseudo_range_cor3 = p_dgps_assist->dgps.dgps_deltapseudo_range_cor3;
	 p_zdgps->b_dgps_deltarange_rate_cor3 = p_dgps_assist->dgps.dgps_deltarange_rate_cor3;

	 p_zSmlcAssist->w_AvailabilityFlags |= C_DGPS_AVAIL;
	 p_zGpsAssisWishlist->w_Wishlist &= ~GPSC_REQ_DGPS;
	 p_zCustom->custom_assist_availability_flag |= C_DGPS_AVAIL;

}

void gpsc_populate_tow_assist
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_tow_assist* p_tow_assist
)
{
	gpsc_smlc_assist_type* p_zSmlcAssist = p_zGPSCControl->p_zSmlcAssist;
	gpsc_raw_tow_type *p_zTow;
	gpsc_custom_struct* p_zCustom = p_zGPSCControl->p_zCustomStruct;
	U8 index = 0, u_i =0;

	for (index = 0; index < N_SV_15; index++, p_tow_assist++)
	{
		p_zSmlcAssist->z_SmlcTow[index].u_SvId = 0;
		if (p_tow_assist->svid != 0xFF)
		{
			p_zTow 								= &p_zSmlcAssist->z_SmlcTow[u_i];
			p_zTow->u_SvId 					= (U8)(p_tow_assist->svid) + 1;

			STATUSMSG("Status: Received TOW Assist for Sv %d", p_zTow->u_SvId);

			p_zTow->u_AlertFlag 			= p_tow_assist->towa_alert_flag;
			p_zTow->u_AntiSpoofFlag 	= p_tow_assist->towa_anti_spoof_flag;
			p_zTow->u_TlmRsvBits 		= p_tow_assist->towa_tlm_reserved_bits;
			p_zTow->w_TlmWord 			= p_tow_assist->towa_tlm_word;

			u_i++;
		}
	}

	p_zSmlcAssist->w_AvailabilityFlags |= C_REF_TOW_AVAIL;
	p_zCustom->custom_assist_availability_flag |= C_REF_TOW_AVAIL;

}

/* check if this function needs to be put under GPSC_ASSIST 
 added to check the time availability -start 
 Just Set the time availability Flag 

 *** It seems that even if we provide this, during position assist Gps Week and Second are not set
     Good luck trying to figure out this logic. So RECON explicitly sets that here ****
*/
void gpsc_check_newtime_assist
(
   gpsc_ctrl_type*      p_zGPSCControl,   // this is global; why do they keep passing it is beyond me
   T_GPSC_time_assist*  p_newtime_assist
)
{
	gpsc_smlc_assist_type* p_zSmlcAssist = p_zGPSCControl->p_zSmlcAssist;
	gpsc_custom_struct*    p_zCustom     = p_zGPSCControl->p_zCustomStruct;

	p_zSmlcAssist->w_AvailabilityFlags |= C_REF_GPSTIME_AVAIL;
	p_zCustom->custom_assist_availability_flag |= C_REF_GPSTIME_AVAIL;

   // set the position Time Parameters
   p_zSmlcAssist->z_InjectPosEst.w_GpsWeek   = p_newtime_assist->gps_week; 
   p_zSmlcAssist->z_InjectPosEst.q_GpsMsec   = p_newtime_assist->gps_msec;
   p_zSmlcAssist->z_InjectPosEst.u_Mode      = 0;   // use this time, not receiver time
   p_zSmlcAssist->z_InjectPosEst.u_ForceFlag = 1;   // force

  /* Inject Time Information  */
  p_zSmlcAssist->z_InjectTimeEst.u_Mode = 1; /* Mode: TimeNow */
  p_zSmlcAssist->z_InjectTimeEst.q_Fcount = 0; /* irrelevant */
  p_zSmlcAssist->z_InjectTimeEst.z_meTime.w_GpsWeek = p_newtime_assist->gps_week;
  p_zSmlcAssist->z_InjectTimeEst.z_meTime.q_GpsMsec = p_newtime_assist->gps_msec;

  /* irrelevant as TimeNow mode is used */
  p_zSmlcAssist->z_InjectTimeEst.z_meTime.f_ClkTimeBias =0;

  p_zSmlcAssist->z_InjectTimeEst.z_meTime.f_ClkTimeUncMs = (FLT)(p_newtime_assist->time_unc / (1000.0));
  p_zSmlcAssist->z_InjectTimeEst.u_ForceFlag = 0;

}


void gpsc_populate_rti_assist
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_rti_assist* p_rti_assist
)
{
	gpsc_smlc_assist_type* p_zSmlcAssist = p_zGPSCControl->p_zSmlcAssist;
	gpsc_assist_wishlist_type*  p_zGpsAssisWishlist = p_zGPSCControl->p_zGpsAssisWishlist;
	gpsc_custom_struct* p_zCustom = p_zGPSCControl->p_zCustomStruct;

	STATUSMSG("Status: Received RTI Assist");
	p_zSmlcAssist->q_RealTimeIntegrity[0] = p_rti_assist->rti_bitmask[0];
	p_zSmlcAssist->q_RealTimeIntegrity[1] = p_rti_assist->rti_bitmask[1];

	p_zSmlcAssist->w_AvailabilityFlags |= C_RTI_AVAIL;
	p_zGpsAssisWishlist->w_Wishlist &= ~GPSC_REQ_RTI;
	p_zCustom->custom_assist_availability_flag |= C_RTI_AVAIL;

}


#define C_LSB_HT_REP           2                 /* reciprocal of C_LSB_HT */

void gpsc_populate_pos_assist
(
   gpsc_ctrl_type*         p_zGPSCControl,
   T_GPSC_position_assist* p_pos_assist
)
{
   U32 q_EastUnc = 0, q_NorthUnc = 0;
   U32 q_DatabaseUnc = 0;

	gpsc_smlc_assist_type* p_zSmlcAssist = p_zGPSCControl->p_zSmlcAssist;
	gpsc_assist_wishlist_type*  p_zGpsAssisWishlist = p_zGPSCControl->p_zGpsAssisWishlist;
	gpsc_inject_pos_est_type*   p_zInjectPosEst   = &p_zSmlcAssist->z_InjectPosEst;
   gpsc_db_type* p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_custom_struct* p_zCustom = p_zGPSCControl->p_zCustomStruct;

	STATUSMSG("Status: Received Position");
	if (p_pos_assist->latitude_sign)
	{
		p_zInjectPosEst->l_Lat = (S32)(0 - p_pos_assist->latitude* C_90_OVER_2_23 *
												C_DEG_TO_RAD * C_LSB_LAT_REP);
	}
	else
	{
		p_zInjectPosEst->l_Lat = (S32)(p_pos_assist->latitude* C_90_OVER_2_23 *
												C_DEG_TO_RAD * C_LSB_LAT_REP);
	}

	p_zInjectPosEst->l_Long = (S32)(p_pos_assist->longitude * C_360_OVER_2_24
													*C_DEG_TO_RAD * C_LSB_LON_REP);
	p_zInjectPosEst->q_Unc = p_pos_assist->position_uncertainty;

	if (p_pos_assist->altitude_sign)
		p_zInjectPosEst->x_Ht  = (S16)(-1* p_pos_assist->altitude * C_LSB_HT_REP);
	else
		p_zInjectPosEst->x_Ht  = (S16)(p_pos_assist->altitude * C_LSB_HT_REP);

	p_zInjectPosEst->u_Mode = 1; /*use Sensor time*/

    /* added for coarse position */
    q_EastUnc =  (U32)p_zGPSCDatabase->z_DBPos.z_ReportPos.w_EastUnc;
    q_NorthUnc = (U32)p_zGPSCDatabase->z_DBPos.z_ReportPos.w_NorthUnc;
    q_DatabaseUnc =(U32)ext_sqrt( q_EastUnc * q_EastUnc + q_NorthUnc * q_NorthUnc );

	p_zSmlcAssist->w_AvailabilityFlags |= C_REFLOC_AVAIL;
   p_zSmlcAssist->w_AvailabilityFlags |= C_ALT_AVAIL;
	p_zGpsAssisWishlist->w_Wishlist &= ~GPSC_REQ_LOC;
	p_zCustom->custom_assist_availability_flag |= C_REFLOC_AVAIL;

	STATUSMSG("DATA: RRLP Longitude %d, converted to %d",
			p_pos_assist->longitude,p_zInjectPosEst->l_Long);
	STATUSMSG("DATA: RRLP Latitude %d, converted to %d",
			p_pos_assist->latitude,p_zInjectPosEst->l_Lat);


  /* If the distance between the GPSC database location and Application injected location
     lies within the uncertainty of the Application injection then use the database position
	  else use the position that is injected from the Application. */

  if (!((p_zGPSCDatabase->z_DBPos.z_ReportPos.l_Lat)&& (p_zGPSCDatabase->z_DBPos.z_ReportPos.l_Long)))
  {
	  p_zInjectPosEst->u_ForceFlag = 1;
	  return;
  }

  if ((gpsc_boundary_check_pos_ok(p_zInjectPosEst,p_zGPSCDatabase))>(DBL)(p_zInjectPosEst->q_Unc ))
  {
	  p_zInjectPosEst->u_ForceFlag = 1;
	  STATUSMSG("BoundaryCheck: Force flag injected");

  }
  else
  {
	  p_zInjectPosEst->u_ForceFlag = 0;
	  STATUSMSG("BoundaryCheck: Force flag NOT injected");
  }



}




DBL gpsc_boundary_check_pos_ok(gpsc_inject_pos_est_type*   p_zInjectPosEst,
													gpsc_db_type *p_zGPSCDatabase)
{
   DBL d_F, d_G, d_L, d_sing, d_cosl, d_cosf, d_sinl, d_sinf, d_cosg;
   DBL d_S, d_C, d_W, d_R, d_H1, d_H2, d_D;
   DBL d_eFlattening = (DBL)(1.0/298.257223563);
   DBL d_eRad = (DBL)(6378135.0);
   DBL d_DatabaseLat,d_DatabaseLong,d_InjLat,d_InjLong;

   d_DatabaseLat = (p_zGPSCDatabase->z_DBPos.z_ReportPos.l_Lat / (C_LSB_LAT_REP));

   d_DatabaseLong = (p_zGPSCDatabase->z_DBPos.z_ReportPos.l_Long / (C_LSB_LON_REP));

   d_InjLat = (p_zInjectPosEst->l_Lat / (C_LSB_LAT_REP));

   d_InjLong = (p_zInjectPosEst->l_Long / (C_LSB_LON_REP));

   d_F = (d_DatabaseLat + d_InjLat) / 2.0;
   d_G = (d_DatabaseLat - d_InjLat) / 2.0;
   d_L = (d_DatabaseLong - d_InjLong) / 2.0;

   d_sing = sin(d_G);
   d_cosl = cos(d_L);
   d_cosf = cos(d_F);
   d_sinl = sin(d_L);
   d_sinf = sin(d_F);
   d_cosg = cos(d_G);

   d_S = (d_sing * d_sing * d_cosl * d_cosl) + (d_cosf * d_cosf * d_sinl * d_sinl);
   d_C = (d_cosg * d_cosg * d_cosl * d_cosl) + (d_sinf * d_sinf * d_sinl * d_sinl);
   d_W = atan2(sqrt(d_S),sqrt(d_C));
   d_R = sqrt((d_S * d_C))/d_W;
   d_H1 = (3 * d_R - 1.0) / (2.0 * d_C);
   d_H2 = (3 * d_R + 1.0) / (2.0 * d_S);
   d_D = 2 * d_W * d_eRad;
   d_D = d_D * (1 + (d_eFlattening * d_H1 * d_sinf * d_sinf * d_cosg * d_cosg) -
                    (d_eFlattening * d_H2 * d_cosf * d_cosf * d_sing * d_sing));
   return(d_D);

}

extern T_GPSC_result gpsc_app_shutdown_req (void)
{
    gpsc_ctrl_type*	p_zGPSCControl;


	SAPENTER(gpsc_app_shutdown_req);

	p_zGPSCControl = gp_zGPSCControl;

	/*Save NVS data to non volatile location */
	//gpsc_db_save_to_nvs(p_zGPSCControl->p_zGPSCDatabase);

	/*power down the GPS IP */
	gpsc_mgp_inject_gps_shutdown_ctrl(p_zGPSCControl);
	gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK);
	MCP_HAL_OS_Sleep(50);

	/*Reset All timers*/
	gpsc_timer_stop(p_zGPSCControl,C_TIMER_ALL);

	p_zGPSCControl->p_zGpscSm->e_GpscCurrState = E_GPSC_STATE_SHUTDOWN;

	p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SHUTDOWN;

	SAPLEAVE(gpsc_app_shutdown_req,GPSC_SUCCESS)

}

extern T_GPSC_result gpsc_app_gps_powersave_req (void)
{
  	gpsc_ctrl_type*	p_zGPSCControl;
	gpsc_state_type*	p_zGPSCState;

	SAPENTER(gpsc_app_gps_powersave_req);

	p_zGPSCControl = gp_zGPSCControl;
	p_zGPSCState =	p_zGPSCControl->p_zGPSCState;

	switch(p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq)
	{
	case E_GPSC_SEQ_READY_IDLE:
	case E_GPSC_SEQ_READY_SLEEP:
//		case  C_GPSC_STATE_READY:
			p_zGPSCState->u_PowerSaveRequestPending = TRUE;
//			SAPLEAVE(gpsc_app_gps_powersave_req, state_change(p_zGPSCControl,C_GPSC_STATE_GPS_POWER_RESET))
		default:
			ERRORMSG2("Powersave not valid in this sequence %s", GpscSmSequence(p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq));
		//		s_StateNames[p_zGPSCState->u_GPSCState]);
			SAPLEAVE(gpsc_app_gps_powersave_req, GPSC_FAIL)

	}
}

extern T_GPSC_result gpsc_app_assist_delete_req (U8 del_assist_bitmap, U32 sv_bitmap)
{
  gpsc_ctrl_type*	p_zGPSCControl;

  gpsc_db_type*     p_zGPSCDatabase;
  gpsc_sess_cfg_type*	p_zSessConfig;
  gpsc_db_sv_dir_type*     p_zDBSvDirection;
  me_SvDirection*        p_zRawSvDirection;
  gpsc_sys_handlers*	p_GPSCSysHandlers;
  U8 u_i=0;
  McpS32 fd;
  T_GPSC_result retVal = GPSC_SUCCESS;
  gpsc_custom_struct* p_zCustom;
  //klocwork
  McpU8 uAidingFile[C_MAX_AIDING_FILENAME_LEN+20];
  gpsc_smlc_assist_type* p_zSmlcAssist;

  SAPENTER(gpsc_app_assist_delete_req);

  p_zGPSCControl = gp_zGPSCControl;
  p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
  p_zSessConfig = p_zGPSCControl->p_zSessConfig;

  p_zDBSvDirection = &p_zGPSCDatabase->z_DBSvDirection;

  p_zCustom = p_zGPSCControl->p_zCustomStruct;

  p_GPSCSysHandlers = p_zGPSCControl->p_zSysHandlers;

  p_zSmlcAssist = p_zGPSCControl->p_zSmlcAssist;


  p_zSessConfig->q_PreSessionActionFlags = del_assist_bitmap;
/*
  if(p_zGPSCState->u_GPSCState != C_GPSC_STATE_INIT)
  {
	ERRORMSG2("Error: Assist Delete not valid in state %s",
		s_StateNames[p_zGPSCState->u_GPSCState]);
	SAPLEAVE(gpsc_app_assist_delete_req, GPSC_FAIL)
  } commented for fix for mvng scn and msb test transit */


  if(del_assist_bitmap&GPSC_DEL_AIDING_TIME)
  {
    STATUSMSG("Status: gpsc_app_assist_delete_req del aiding time");
	p_zGPSCDatabase->z_DBGpsClock.u_Valid = FALSE;
	p_zGPSCDatabase->z_DBGpsClock.q_FCount = GPSC_INVALID_FCOUNT;
    p_zGPSCDatabase->z_DBGpsClock.u_NumInvalids = 0;
	p_zGPSCDatabase->z_DBHealth.d_UpdateTimeSec = 0;
	p_zGPSCDatabase->z_DBGpsClock.w_GpsWeek = C_GPS_WEEK_UNKNOWN;
	p_zGPSCDatabase->z_DBGpsClock.q_GpsMs = 0;
	p_zSmlcAssist->w_InjectedFlags &= ~C_REF_GPSTIME_AVAIL;
	p_zSmlcAssist->w_InjectedFlags &= ~C_REF_TOW_AVAIL;

	/* Delete from receiver */
	gpsc_mgp_tx_rc_act (p_zGPSCControl, C_DELETE_TIME, 0);
	gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK);

	p_zCustom->custom_assist_availability_flag &= ~C_REF_GPSTIME_AVAIL;

	/* delete the sv direction DB struct also, as TIME is need for it computation */
	del_assist_bitmap |= GPSC_DEL_AIDING_SVDIR;
 }
  if(del_assist_bitmap&GPSC_DEL_AIDING_POSITION)
  {
    STATUSMSG("Status: gpsc_app_assist_delete_req del pos");
	p_zGPSCDatabase->z_DBPos.d_ToaSec = 0;
	p_zGPSCDatabase->z_NvsPos.d_ToaSec = 0;
	p_zSmlcAssist->w_InjectedFlags &= ~C_REFLOC_AVAIL;

	MCP_HAL_STRING_StrCpy (uAidingFile,(McpU8*)p_GPSCSysHandlers->uAidingPath);
	MCP_HAL_STRING_StrCat (uAidingFile,C_STR_AID_POSITION_FILE);
	if( mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *) uAidingFile,
						MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_WRONLY | MCP_HAL_FS_O_CREATE | MCP_HAL_FS_O_TRUNC, &fd) == RES_OK )
	{
		mcpf_file_empty(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
		mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
	}
	else
	{
		STATUSMSG("Status: POS deletion from NVM failed!");
		retVal = GPSC_FAIL;
	}

	/* Delete position from receiver */
	gpsc_mgp_tx_rc_act (p_zGPSCControl, C_DELETE_POS, 0);
	gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK);
	p_zCustom->custom_assist_availability_flag  &= ~C_REFLOC_AVAIL;

	/* delete the sv direction DB struct also, as POS is need for it computation */
	del_assist_bitmap |= GPSC_DEL_AIDING_SVDIR;

  }
  if(del_assist_bitmap&GPSC_DEL_AIDING_EPHEMERIS)
  {
	p_zGPSCDatabase->u_EphCounter=0;
	p_zSmlcAssist->w_InjectedFlags &= ~C_EPH_AVAIL;

	STATUSMSG("Status: gpsc_app_assist_delete_req del EPH");
	STATUSMSG("Status: gpsc_app_assist_delete_req del EPH: sv_bitmap=%d",sv_bitmap);

	if(sv_bitmap == 0 || sv_bitmap == 0xFFFFFFFF)
	{	// delete all
       		gpsc_mgp_tx_rc_act (p_zGPSCControl, C_DELETE_EPHEM, 0);
			p_zCustom->custom_pred_eph_bitmap = (U32) 0;
			for(u_i = 0; u_i < N_SV; u_i++)
			{
  				p_zSmlcAssist->w_AvailabilityFlags = 0;

  				p_zSmlcAssist->u_nSvEph = 0;
  				p_zSmlcAssist->u_InjVisible = FALSE;
  				p_zSmlcAssist->q_VisiableBitmap = 0;
  				p_zSmlcAssist->q_InjSvEph = 0;

				p_zSmlcAssist->z_SmlcRawEph[u_i].u_available = FALSE;
				p_zSmlcAssist->z_SmlcRawEph[u_i].u_Prn = 0;

				p_zSmlcAssist->z_SmlcRawEph[u_i].u_predicted = 0;
				p_zSmlcAssist->z_SmlcRawEph[u_i].u_predSeedAge = 0;
				p_zSmlcAssist->z_SmlcRawEph[u_i].toe_expired = TRUE;

				p_zSmlcAssist->z_SmlcRawEph[u_i].u_available = FALSE;
			}
	}
	else
	{	//delete selected
		for(u_i = 0; u_i < N_SV; u_i++)
		{
			if(sv_bitmap && ((U32) pow(2,u_i)))
			{
				gpsc_mgp_tx_rc_act (p_zGPSCControl, C_DELETE_EPHEM, u_i);
				p_zCustom->custom_pred_eph_bitmap &= ~((U32) pow(2, u_i));
				p_zSmlcAssist->z_SmlcRawEph[u_i].u_available = FALSE;
				p_zSmlcAssist->z_SmlcRawEph[u_i].u_Prn = 0;

				p_zSmlcAssist->z_SmlcRawEph[u_i].u_predicted = 0;
				p_zSmlcAssist->z_SmlcRawEph[u_i].u_predSeedAge = 0;
				p_zSmlcAssist->z_SmlcRawEph[u_i].toe_expired = TRUE;
				p_zSmlcAssist->z_SmlcRawEph[u_i].u_available	= FALSE;

			}
		}
	}
	gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK);

	MCP_HAL_STRING_StrCpy (uAidingFile,(McpU8*)p_GPSCSysHandlers->uAidingPath);
	MCP_HAL_STRING_StrCat (uAidingFile,C_STR_AID_EPH_FILE);
	if( mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *)uAidingFile,
						MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_WRONLY | MCP_HAL_FS_O_CREATE | MCP_HAL_FS_O_TRUNC, &fd) == RES_OK )
	{
		mcpf_file_empty(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
		mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
	}
	else
	{
		STATUSMSG("Status: gpsc_app_assist_delete_req del EPH Falied!");
		retVal = GPSC_FAIL;
	}

	p_zSmlcAssist->q_InjSvEph = 0;
	p_zCustom->custom_assist_availability_flag  &= ~C_EPH_AVAIL;

	/* delete the sv direction DB struct also, as EPH is need for it computation */
	del_assist_bitmap |= GPSC_DEL_AIDING_SVDIR;

  }
  if(del_assist_bitmap&GPSC_DEL_AIDING_ALMANAC)
  {
  	pe_RawAlmanac*              p_zSmlcRawAlm;
	p_zGPSCDatabase->u_AlmCounter=0;
	p_zSmlcAssist->w_InjectedFlags &= ~C_ALM_AVAIL;

	STATUSMSG("Status: gpsc_app_assist_delete_req del ALM: sv_bitmap=%d",sv_bitmap);

	if(sv_bitmap == 0 || sv_bitmap == 0xFFFFFFFF)
	{	// delete all
       		gpsc_mgp_tx_rc_act (p_zGPSCControl, C_DELETE_ALM, 0);
		for (u_i=0; u_i<N_SV; u_i++)
		{
		    p_zSmlcRawAlm = &p_zSmlcAssist->z_SmlcRawAlmanac[u_i];
		    p_zSmlcRawAlm->u_Prn = 0;
		}
	}
	else
	{	//delete selected
		for(u_i = 0; u_i < N_SV; u_i++)
		{
			if(sv_bitmap && ((U32) pow(2,u_i)))
			{
			    p_zSmlcRawAlm = &p_zSmlcAssist->z_SmlcRawAlmanac[u_i];
	     	    p_zSmlcRawAlm->u_Prn = 0;
				gpsc_mgp_tx_rc_act (p_zGPSCControl, C_DELETE_ALM, u_i);
		}
	}
	}
	gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK);

	MCP_HAL_STRING_StrCpy (uAidingFile,(McpU8*)p_GPSCSysHandlers->uAidingPath);
	MCP_HAL_STRING_StrCat (uAidingFile,C_STR_AID_ALMANAC_FILE);
	if ( mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *)uAidingFile,
						MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_WRONLY | MCP_HAL_FS_O_CREATE | MCP_HAL_FS_O_TRUNC, &fd) == RES_OK )
	{
		mcpf_file_empty(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
		mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
	}
	else
	{
		STATUSMSG("Status: gpsc_app_assist_delete_req del ALM Falied!");
		retVal = GPSC_FAIL;
	}

	p_zCustom->custom_assist_availability_flag &= ~ C_ALM_AVAIL;

	/* delete the sv direction DB struct also, as ALM is need for it computation */
	del_assist_bitmap |= GPSC_DEL_AIDING_SVDIR;

  }
  if(del_assist_bitmap&GPSC_DEL_AIDING_IONO_UTC)
  {
	  STATUSMSG("Status: gpsc_app_assist_delete_req del IONO");
	p_zGPSCDatabase->z_DBIono.d_UpdateTimeSec = 0;
	p_zSmlcAssist->w_InjectedFlags &= ~C_UTC_AVAIL;
	p_zSmlcAssist->w_InjectedFlags &= ~C_IONO_AVAIL;

	MCP_HAL_STRING_StrCpy (uAidingFile,(McpU8*)p_GPSCSysHandlers->uAidingPath);
	MCP_HAL_STRING_StrCat (uAidingFile,C_STR_AID_IONO_FILE);
	if ( mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *)uAidingFile,
						MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_WRONLY | MCP_HAL_FS_O_CREATE | MCP_HAL_FS_O_TRUNC, &fd) == RES_OK )
	{
		mcpf_file_empty(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
		mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
	}
	else
	{
		STATUSMSG("Status: gpsc_app_assist_delete_req del IONO Falied!");
		retVal = GPSC_FAIL;
	}

	MCP_HAL_STRING_StrCpy (uAidingFile,(McpU8*)p_GPSCSysHandlers->uAidingPath);
	MCP_HAL_STRING_StrCat (uAidingFile,C_STR_AID_UTC_FILE);
	if ( mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *)uAidingFile,
						MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_WRONLY | MCP_HAL_FS_O_CREATE | MCP_HAL_FS_O_TRUNC, &fd) == RES_OK )
	{
	mcpf_file_empty(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
	mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
	}
	else
	{
		STATUSMSG("Status: UTC deletion from NVM failed");
		retVal = GPSC_FAIL;
	}

	p_zCustom->custom_assist_availability_flag &= ~C_IONO_AVAIL;
	p_zCustom->custom_assist_availability_flag &= ~C_UTC_AVAIL;

    gpsc_mgp_tx_rc_act (p_zGPSCControl, C_DELETE_IONO_UTC, 0);
	gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK);

  }
  if(del_assist_bitmap&GPSC_DEL_AIDING_SVHEALTH)
  {  /* added for fix for mvng scn and msb test transit */
	  STATUSMSG("Status: gpsc_app_assist_delete_req del SVHEALTH");
	p_zSmlcAssist->w_InjectedFlags &= ~C_ANGLE_AVAIL;

     for ( u_i = 0; u_i < N_SV; u_i++)
	  {
	     p_zGPSCDatabase->z_DBHealth.u_AlmHealth[u_i] = C_HEALTH_UNKNOWN;
	 }

	MCP_HAL_STRING_StrCpy (uAidingFile,(McpU8*)p_GPSCSysHandlers->uAidingPath);
	MCP_HAL_STRING_StrCat (uAidingFile,C_STR_AID_HEALTH_FILE);
	 if ( mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *) uAidingFile,
						MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_WRONLY | MCP_HAL_FS_O_CREATE | MCP_HAL_FS_O_TRUNC, &fd) == RES_OK )
	 {
	 	mcpf_file_empty(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
		mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
	 }
	 else
	 {
	 	STATUSMSG("Status: gpsc_app_assist_delete_req del SVHEALTH Falied!");
		retVal = GPSC_FAIL;
	 }
	 gpsc_mgp_tx_rc_act (p_zGPSCControl, C_DELETE_SV_HEALTH, 0);
	 gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK);
	 p_zCustom->custom_assist_availability_flag &= ~C_RTI_AVAIL;

  }
  /* to delete SV direction */
  if(del_assist_bitmap&GPSC_DEL_AIDING_SVDIR)
  {
		  /* to delete SV direction */
  p_zDBSvDirection->u_num_of_sv = 0;
		  STATUSMSG("Status: Delete SV directions");
    for ( u_i = 0; u_i < N_SV; u_i++)
	  {
	     p_zRawSvDirection = &p_zDBSvDirection->z_RawSvDirection[u_i];
		 p_zRawSvDirection->b_Elev = -128 ;
			p_zRawSvDirection->u_Azim = 0;
		   }
		p_zCustom->custom_assist_availability_flag &= ~C_ANGLE_AVAIL;
  }
  if (del_assist_bitmap&GPSC_DEL_AIDING_ACQ)
	{
		gpsc_acq_assist_type*        p_zSmlcAcquisAssist = &p_zSmlcAssist->z_AcquisAssist;
		gpsc_db_gps_clk_type*   p_zAAGpsClock = &p_zSmlcAcquisAssist->p_zAAGpsClock;
		gpsc_acq_element_type*       p_zSmlcAcquisElement;
		gpsc_additional_angle_type*  p_zSmlcAdditionalAngle;
		STATUSMSG("Status: Delete Acq");
		p_zCustom->custom_assist_availability_flag &= ~C_ANGLE_AVAIL;
		p_zCustom->custom_assist_availability_flag &= ~C_ACQ_AVAIL;
		p_zSmlcAcquisAssist->u_NumSv = 0;
		p_zSmlcAssist->w_BestRef = 0;
		p_zSmlcAssist->w_NDiffs = 0;
		p_zAAGpsClock->u_Valid = FALSE;
		p_zSmlcAssist->w_InjectedFlags &= ~C_ACQ_AVAIL;

		for (u_i=0; u_i<C_MAX_SVS_ACQ_ASSIST; u_i++)
		{
			if (sv_bitmap && ((U32) pow(2,u_i)))
			{
			  p_zSmlcAcquisElement = &p_zSmlcAcquisAssist->z_AcquisElement[u_i];
			  p_zSmlcAdditionalAngle = &p_zSmlcAcquisAssist->z_AddAngle[u_i];

			  p_zSmlcAcquisElement->u_SvId = 0;
			  p_zSmlcAcquisElement->u_Doppler1 = 0xFF;
			  p_zSmlcAcquisElement->u_DopplerUnc = 0xFF;
			  p_zSmlcAdditionalAngle->u_Azimuth = 0xFF;
			  p_zSmlcAdditionalAngle->u_Elevation = 0xFF;
			}
		}
	}

  gpsc_app_assist_delete_ind();


  SAPLEAVE(gpsc_app_assist_delete_req, retVal)
}



/*********************************************************************/
extern T_GPSC_result gpsc_app_loc_fix_stop_req ( U32 q_SessionTagId	)
{
  gpsc_ctrl_type*	              p_zGPSCControl = gp_zGPSCControl;
  gpsc_sess_cfg_type*           p_zSessCfg = p_zGPSCControl->p_zSessConfig;
  gpsc_state_type*	           p_zGPSCState = p_zGPSCControl->p_zGPSCState;
  gpsc_sess_specific_cfg_type*  p_zSessSpecificCfg=NULL;

	U32             w_SessionTagId[25];
	S8              u_TotSessions = 0;
	S8              u_CurSession = 0;
	McpU8 rep_config=0xFF;
	McpU8 apm_config=0xFF;

   SAPENTER(gpsc_app_loc_fix_stop_req);

	gpsc_session_get_all_nodes(p_zSessCfg, &w_SessionTagId, &u_TotSessions);

	// For multi session scenario - like SUPL MSA
	if (u_TotSessions>1)
	{
		for(u_CurSession=0;u_CurSession<u_TotSessions;u_CurSession++)
		{
			p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type*)gpsc_session_get_node(p_zSessCfg,w_SessionTagId[u_CurSession]);
			if (p_zSessSpecificCfg == NULL)
			   return GPSC_FAIL;

			if (GPSC_NW_SESSION_LP == p_zSessSpecificCfg->w_typeofsession)
			{
				/* Mutiple stop requests sometimes cause application stop to be blocked. */
				gpsc_session_del_node(p_zGPSCControl->p_zSessConfig, w_SessionTagId[u_CurSession]);
			}
		}
	}
	if ( q_SessionTagId == C_SESSION_INVALID )
	{
		p_zSessSpecificCfg = p_zGPSCControl->p_zSessConfig->p_zSessSpecificCfg ;
	}
	else
	{
  p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type*)gpsc_session_get_node(p_zSessCfg,q_SessionTagId);
	}

	gpsc_smlc_assist_type*	p_zSmlcAssist	= p_zGPSCControl->p_zSmlcAssist;



  if(p_zSessSpecificCfg == NULL)
  {
		gpsc_app_stop_fix_ind(q_SessionTagId);
		SAPLEAVE(gpsc_app_loc_fix_stop_req, GPSC_SUCCESS)
	}
	rep_config=gpsc_get_gf_report_config(p_zGPSCControl->p_zSessConfig,p_zSessSpecificCfg);
	if(0==rep_config)
	{
		STATUSMSG("gpsc_app_loc_fix_stop_req: GF session currently running, Normal sessions Stopped..Change Report Config to report breach .\n");
		TNAVC_SetMotionMask motion_mask;
		memset(&motion_mask,0x00,sizeof(motion_mask));
		motion_mask.region_number=0;
		motion_mask.report_configuration=0;
		gpsc_app_set_motion_mask(&motion_mask,0xFFFFFFFF);
	}


	if(p_zSessSpecificCfg->w_typeofsession!=GPSC_NW_SESSION_NI || p_zSessSpecificCfg->w_typeofsession!=GPSC_NW_SESSION_LP )
	{
		g_repConfig=1;
		g_apmConfig=0;
	}
  	/* Stopping the wifi positioninh here
  	    Presently the wifi position will stop as and when get stop request from application without checking the session */
#ifdef WIFI_ENABLE
   p_zGPSCState->u_wifilocrequested = FALSE;
   if(p_zSessSpecificCfg->u_LocFixPosBitmap & WIFI_POS)
   {
      STATUSMSG("gpsc_app_loc_fix_stop_req : Stop wifi session");
  	  wpl_location_stop_request( q_SessionTagId);
      wifi_log_close(p_zGPSCControl);
   }
#endif
    p_zGPSCState->q_LocStopSessId = q_SessionTagId;

	//Stop C-Plane Session
	if(p_zGPSCState->u_CalibRequested == TRUE)
	{
		gpsc_app_calib_control_ind(NULL);
		p_zGPSCState->u_CalibRequested = FALSE;
	}

	if(E_GPSC_STATE_ACTIVE != p_zGPSCControl->p_zGpscSm->e_GpscCurrState)
	  {
		 gpsc_cmd_complete_cnf(GPSC_FAIL);
		 SAPLEAVE(gpsc_app_loc_fix_stop_req, GPSC_NO_FIX_IN_PROGRESS_FAIL);

	  }

	STATUSMSG("gpsc_app_loc_fix_stop_req : Sending stop command to all assistance data sources");
		if(gpsc_app_stop_nw_connection()==GPSC_SUCCESS)
		{
			p_zGPSCState->u_StopSessionReqPending = TRUE;
			p_zSmlcAssist->u_NumAssistAttempt=0;
		}
	gpsc_app_update_apm_config(p_zGPSCControl);

	GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_LOCATION_STOP);

	/* stop timer for EE request */
	if(gpsc_timer_status(p_zGPSCControl, C_TIMER_RXN_EPHE_VALID) == GPSC_SUCCESS)
	{
		STATUSMSG("EE Timer stopped after loc fix stop");
		gpsc_timer_stop(p_zGPSCControl, C_TIMER_RXN_EPHE_VALID);
	}

	SAPLEAVE(gpsc_app_loc_fix_stop_req, GPSC_PENDING);
}

void gpsc_app_stop_nw_session_indication()
{
		gpsc_ctrl_type*	p_zGPSCControl;
		gpsc_state_type*	p_zGPSCState;
		p_zGPSCControl = gp_zGPSCControl;
		p_zGPSCState  = p_zGPSCControl->p_zGPSCState;

		STATUSMSG("gpsc_app_stop_nw_session_indication : u_StopSessionReqPending = %d e_GpscCurrState = %d",p_zGPSCState->u_StopSessionReqPending, p_zGPSCControl->p_zGpscSm->e_GpscCurrState);

		if(p_zGPSCState->u_StopSessionReqPending == TRUE && E_GPSC_STATE_ACTIVE == p_zGPSCControl->p_zGpscSm->e_GpscCurrState)
		{
			GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_LOCATION_STOP);
		}

		p_zGPSCState->u_StopSessionReqPending = FALSE;
}


T_GPSC_result gpsc_app_stop_nw_connection()
{
	T_GPSC_result	res = GPSC_FAIL;
	STATUSMSG("gpsc_app_stop_nw_connection : entering");
	   res = gpsc_app_stop_request_ind();
	   if(res == GPSC_FAIL)
		{
			STATUSMSG("gpsc_app_stop_nw_connection : Stop NW session Failed");
		}
		else if(res == GPSC_SUCCESS)
			{
				STATUSMSG("gpsc_app_stop_nw_connection : Successfully Stopped NW session");
			}else
			{
				STATUSMSG("gpsc_app_stop_nw_connection : unknown return value");
			}

	STATUSMSG("gpsc_app_stop_nw_connection : exiting");
	return res;

}
void gpsc_app_create_default_config_req(U8 u_ConfigNum)
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
    gpsc_cfg_type z_GPSCConfig,*p_zGPSCConfig = &z_GPSCConfig ;

	U32 q_i;
	U8 *p_B = (U8*)p_zGPSCConfig;
	McpS32	fd;

	/*Set all bytes to 0, so we dont include fillers in checksum*/
	for(q_i = 0; q_i< sizeof(gpsc_cfg_type);q_i++)
		*p_B++ = 0;


	p_zGPSCConfig->patch_available = GPSC_TRUE;

	if(u_ConfigNum == 0)
	{
		T_GPSC_comm_config_uart *p_comm_config_uart =
							&p_zGPSCConfig->comm_config.comm_config_union.comm_config_uart;

		p_zGPSCConfig->comm_config.ctrl_comm_config_union = GPSC_COMM_MODE_UART;

        p_comm_config_uart->uart_baud_rate = GPSC_BAUD_115200;

	}
	if(u_ConfigNum == 1)
	{
		T_GPSC_comm_config_i2c *p_comm_config_i2c =
							&p_zGPSCConfig->comm_config.comm_config_union.comm_config_i2c;

		p_zGPSCConfig->comm_config.ctrl_comm_config_union = GPSC_COMM_MODE_I2C;
		p_comm_config_i2c->i2c_data_rate = GPSC_I2C_RATE_400KHZ;
		p_comm_config_i2c->i2c_logicalid = 0x14;
		p_comm_config_i2c->i2c_ce_address = 0x08;
		p_comm_config_i2c->i2c_ce_address_mode = GPSC_I2C_7BIT_ADDRESS;
		p_comm_config_i2c->i2c_gps_address_mode = GPSC_I2C_7BIT_ADDRESS;
		p_comm_config_i2c->i2c_gps_address = 0x14 ;

	}
	p_zGPSCConfig->compatible_gps_versions[0] = 0x00050A03;
	p_zGPSCConfig->compatible_gps_versions[1] = 0x01000001;
	p_zGPSCConfig->compatible_gps_versions[2] = 0x01090004;
	p_zGPSCConfig->compatible_gps_versions[3] = 0x10000004;

	p_zGPSCConfig->ref_clock_frequency = 38400000; // 19200000;
	p_zGPSCConfig->ref_clock_quality = 50;

	p_zGPSCConfig->driver_tx_response_required = GPSC_TRUE;
	p_zGPSCConfig->ai2_ack_required = GPSC_TRUE;

	p_zGPSCConfig->auto_power_save_enable = GPSC_TRUE;
	p_zGPSCConfig->auto_power_ready_enable = GPSC_TRUE;
	p_zGPSCConfig->auto_power_save_timeout = 10000; /*secs*/


    p_zGPSCConfig->altitude_hold_mode = GPSC_ALT_HOLD_MANUAL_3D; /* 0: PE default */
    p_zGPSCConfig->elevation_mask = (5 * 128)/90; /* 7: for 5 degrees, PE default */
    p_zGPSCConfig->pdop_mask = 12; /* typical 50, PE default */

	p_zGPSCConfig->assist_bitmap_msbased_mandatory_mask = GPSC_REQ_LOC|
										   GPSC_REQ_TIME|
										   GPSC_REQ_NAV;
	p_zGPSCConfig->assist_bitmap_msassisted_mandatory_mask = GPSC_REQ_AA|
										   GPSC_REQ_TIME;

	p_zGPSCConfig->smlc_comm_timeout = 40000; /*millisecs*/
	p_zGPSCConfig->ai2_comm_timeout = 1000;  /*millisecs*/
	p_zGPSCConfig->driver_tx_timeout = 1000;   /*millisecs*/
	p_zGPSCConfig->sleep_entry_delay_timeout = 1000;
	p_zGPSCConfig->utc_leap_seconds = 16;
	p_zGPSCConfig->pa_blank_enable = GPSC_FALSE;
	p_zGPSCConfig->pa_blank_polarity = GPSC_ACTIVE_HIGH;
	p_zGPSCConfig->gps_minimum_week = 0;
	p_zGPSCConfig->timestamp_edge = GPSC_POSITIVE_EDGE;
	p_zGPSCConfig->default_max_ttff = 90000;
	p_zGPSCConfig->max_clock_acceleration = 1000;
	p_zGPSCConfig->front_end_loss = 25;

	p_zGPSCConfig->checksum = CalculateCheckSum((U8*)&z_GPSCConfig, sizeof(gpsc_cfg_type));

	if (mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *)"/system/bin/GPSCConfigFile.cfg",
						MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_WRONLY, &fd) == RES_OK )
	{
		mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, fd,
						(McpU8 *)p_zGPSCConfig, sizeof(gpsc_cfg_type));
		mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
	}
	else
	{
		ALOGE("+++ %s: Failed to create default configuration file +++\n", __FUNCTION__);
	}
}

void gpsc_release_memory(gpsc_ctrl_type*  p_zGPSCControl)
{
	/* release gpsc sm */
	if(mcpf_mem_free(p_zGPSCControl->p_zSysHandlers->hMcpf,
					   p_zGPSCControl->p_zGpscSm) != RES_OK )
		gpsc_os_fatal_error_ind(GPSC_MEM_FREE_ERROR);

	/* Regardless of what it was, reset it as precaution for future runs*/
	p_zGPSCControl->p_zGPSCState->u_ShutDownRequestPending = FALSE;
	if (mcpf_mem_free(p_zGPSCControl->p_zSysHandlers->hMcpf,
					   p_zGPSCControl->p_GPSCMemory) != RES_OK )
		gpsc_os_fatal_error_ind(GPSC_MEM_FREE_ERROR);
}

/*
 ******************************************************************************
 * gpsc_app_prodlinetest_req
 *
 * Function description:
 *
 * This This SAP will be called by GPSM whenever product line test to
 * be performed
 *
 * Parameters:
 *
 *  T_GPSC_prodline_test_req_params - Product Line Test parameters
 *
 * Return value:
 *  Result
 *
 ******************************************************************************
*/
extern T_GPSC_result gpsc_app_prodlinetest_req
(
 TNAVC_plt* prodline_test_req_params
 )
{

    gpsc_ctrl_type*	p_zGPSCControl;
	gpsc_state_type*	p_zGPSCState;
    gpsc_productline_test_params*   p_zProductlineTest;
	T_GPSC_result t_Result = GPSC_PENDING;


	SAPENTER(gpsc_app_prodlinetest_req);
	p_zGPSCControl = gp_zGPSCControl;
	p_zGPSCState = p_zGPSCControl->p_zGPSCState;
    p_zProductlineTest =  p_zGPSCControl->p_zProductlineTest;


    mcpf_mem_copy(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zProductlineTest,prodline_test_req_params,sizeof(TNAVC_plt));

	GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_PLT);

	if(E_GPSC_EVENT_PLT == p_zGPSCControl->p_zGpscSm->e_GpscCurrEvent)
		{
		p_zGPSCState->u_ProductlinetestRequestPending = TRUE;
		}
	else
		{
		ERRORMSG2("Error: Request for PLT not valid in state %d\n",p_zGPSCControl->p_zGpscSm->e_GpscCurrState);
		t_Result = GPSC_FAIL;
		}
//	gpsc_cmd_complete_cnf(GPSC_PENDING);
	SAPLEAVE(gpsc_app_prodlinetest_req,t_Result)
}

/*
 ******************************************************************************
 * gpsc_app_inject_calib_control_req
 *
 * Function description:
 *
 * This function is the external interface that the application uses to instruct
 * the GPSC to inject the calibration control type.It can be called from the gpsm whenever
 * enable or disable calibration control type.
 *
 * Parameters:
 *  enum  T_GPSC_calib_type
 *
 *
 * Return value:
 *  T_GPSC_result : Result
 * Added for config 4 support-
 * Author: Raghavendra M R
 ******************************************************************************
*/

extern T_GPSC_result gpsc_app_inject_calib_control_req(T_GPSC_calib_type CalibrationType)
{

    gpsc_ctrl_type*	p_zGPSCControl;
	gpsc_state_type*	p_zGPSCState;

	SAPENTER(gpsc_app_inject_calib_control_req);

	p_zGPSCControl = gp_zGPSCControl;
	p_zGPSCState = p_zGPSCControl->p_zGPSCState;
	p_zGPSCState->u_CalibRequested = FALSE;


	STATUSMSG("Status: gpsc_app_inject_calib_control_req: Calib Params:%d, Currentseq:%d",
					p_zGPSCState->u_CalibType,
					p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq);
	if(p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq < E_GPSC_SEQ_INIT)
	{
		SAPLEAVE(gpsc_app_inject_calib_control_req,GPSC_FAIL);
	}
	else
		{
		   p_zGPSCState->u_CalibType = (U8) CalibrationType;
			if(CalibrationType == GPSC_UNDO_CALIB)
			{
				/* undo caliberation */
				maximize_frunc(p_zGPSCControl);
			}
			else
			{
 	    STATUSMSG("Status: gpsc_app_inject_calib_control_req: Calib Params:%d\n",p_zGPSCState->u_CalibType);
				//if(p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_SESSION_ON)
		{
		   if (p_zGPSCState->u_CalibType == GPSC_ENABLE_PERIODIC_CALIB_TIME_STAMP
				|| p_zGPSCState->u_CalibType == GPSC_ENABLE_ONESHOT_CALIB_TIME_STAMP)
		   {
			   gpsc_mgp_inject_CalibTimestamp_Period(p_zGPSCControl);
		   }
		   else if(p_zGPSCState->u_CalibType == GPSC_ENABLE_PERIODIC_CALIB_REF_CLK
				|| p_zGPSCState->u_CalibType == GPSC_ENABLE_ONESHOT_CALIB_REF_CLK)
		   {
			   gpsc_mgp_inject_OscParams(p_zGPSCControl);
		   }

		   gpsc_mgp_inject_calibrationcontrol(p_zGPSCControl,p_zGPSCState->u_CalibType);
		  if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
		   SAPLEAVE(gpsc_app_inject_calib_control_req, GPSC_TX_INITIATE_FAIL)
		    		}
	    }
		   SAPLEAVE(gpsc_app_inject_calib_control_req,GPSC_SUCCESS);
		}
}

/*
 ******************************************************************************
 * gpsc_app_set_sbas_params
 *
 * Function description:
 *
 * This function is the external interface that the application uses to instruct
 * the GPSC to set the SBAS parameters.
 *
 * Parameters:
 *  struct  TNAVC_SetSBASParams
 *
 *
 * Return value:
 *  T_GPSC_result : Result
 ******************************************************************************
*/
extern T_GPSC_result gpsc_app_set_sbas_params (TNAVC_SetSBASParams *sbas_params)
{
	gpsc_ctrl_type *p_zGPSCControl;

	gpsc_dyna_feature_cfg_type *p_zGPSCDynaFeatureConfig;

	p_zGPSCControl = gp_zGPSCControl;
	p_zGPSCDynaFeatureConfig = p_zGPSCControl->p_zGPSCDynaFeatureConfig;

	SAPENTER(gpsc_app_set_sbas_params);

		p_zGPSCDynaFeatureConfig->feature_flag |= GPSC_FEAT_SBAS;
	p_zGPSCDynaFeatureConfig->sbas_control = sbas_params->sbas_control;
	p_zGPSCDynaFeatureConfig->sbas_prn_mask = sbas_params->sbas_prn_mask;
	p_zGPSCDynaFeatureConfig->Mode = sbas_params->Mode;
	p_zGPSCDynaFeatureConfig->Flags = sbas_params->Flags;

		if((p_zGPSCControl->p_zGPSCState->u_SensorOpMode) == C_RC_ON)
		{
		    STATUSMSG("Injected SBAS=%d", p_zGPSCDynaFeatureConfig->sbas_control);
			gpsc_mgp_tx_inject_sbas(p_zGPSCControl);
			p_zGPSCDynaFeatureConfig->feature_flag &= ~GPSC_FEAT_SBAS;
		}

	return GPSC_SUCCESS;
}

/*
 ******************************************************************************
 * gpsc_app_enable_kalman_filter
 *
 * Function description:
 *
 * This function is the external interface that the application uses to instruct
 * the GPSC to enable/disable the kalman filter.
 *
 * Parameters:
 *  struct  TNAVC_EnableKalmanFilter
 *
 *
 * Return value:
 *  T_GPSC_result : Result
 ******************************************************************************
*/
extern T_GPSC_result gpsc_app_enable_kalman_filter (TNAVC_EnableKalmanFilter *enable_kalman_filter)
{
	gpsc_ctrl_type *p_zGPSCControl;

	gpsc_dyna_feature_cfg_type *p_zGPSCDynaFeatureConfig;

	p_zGPSCControl = gp_zGPSCControl;
	p_zGPSCDynaFeatureConfig = p_zGPSCControl->p_zGPSCDynaFeatureConfig;

	SAPENTER(gpsc_app_enable_kalman_filter);

		p_zGPSCDynaFeatureConfig->feature_flag |= GPSC_FEAT_KALMAN;
	p_zGPSCDynaFeatureConfig->kalman_control = enable_kalman_filter->kalman_control;

	return GPSC_SUCCESS;
}

/*
 ******************************************************************************
 * gpsc_app_set_host_wakeup_params
 *
 * Function description:
 *
 * This function is the external interface that the application uses to instruct
 * the GPSC to set the host wake-up parameters.
 *
 * Parameters:
 *  struct  TNAVC_SetHostWakeupParams
 *
 *
 * Return value:
 *  T_GPSC_result : Result
 ******************************************************************************
*/
extern T_GPSC_result gpsc_app_set_host_wakeup_params (TNAVC_SetHostWakeupParams *host_wakeup_params)
{
	gpsc_ctrl_type *p_zGPSCControl;

	gpsc_dyna_feature_cfg_type *p_zGPSCDynaFeatureConfig;

	p_zGPSCControl = gp_zGPSCControl;
	p_zGPSCDynaFeatureConfig = p_zGPSCControl->p_zGPSCDynaFeatureConfig;

	SAPENTER(gpsc_app_set_host_wakeup_params);

		p_zGPSCDynaFeatureConfig->feature_flag |= GPSC_FEAT_HOSTWAKEUP;
	p_zGPSCDynaFeatureConfig->host_req_opt = host_wakeup_params->host_req_opt;
	p_zGPSCDynaFeatureConfig->host_assert_delay = host_wakeup_params->host_assert_delay;
	p_zGPSCDynaFeatureConfig->host_reassert_delay = host_wakeup_params->host_reassert_delay;
	p_zGPSCDynaFeatureConfig->host_ref_clk_req_opt = host_wakeup_params->host_ref_clk_req_opt;
	p_zGPSCDynaFeatureConfig->host_ref_clk_req_sig_sel = host_wakeup_params->host_ref_clk_req_sig_sel;
	p_zGPSCDynaFeatureConfig->host_ref_clk_assert_dly = host_wakeup_params->host_ref_clk_assert_dly;
	p_zGPSCDynaFeatureConfig->host_ref_clk_reassert_dly = host_wakeup_params->host_ref_clk_reassert_dly;
	p_zGPSCDynaFeatureConfig->host_sigout_type_ctrl = host_wakeup_params->host_sigout_type_ctrl;

	return GPSC_SUCCESS;
}

/*
 ******************************************************************************
 * gpsc_app_set_motion_mask
 *
 * Function description:
 *
 * This function is the external interface that the application uses to instruct
 * the GPSC to set the motion mask (geo-fence) parameters.
 *
 * Parameters:
 *  struct  TNAVC_SetMotionMask

 *
 *
 * Return value:
 *  T_GPSC_result : Result
 ******************************************************************************
*/
#if 0 /*GF-Integ*/
extern T_GPSC_result gpsc_app_set_motion_mask (TNAVC_SetMotionMask *motion_mask)
{
	gpsc_ctrl_type *p_zGPSCControl;
	gpsc_cfg_type *p_zGPSCConfig;
	gpsc_set_motion_mask * p_zGPSCSetMotionMask;
		gpsc_geo_fence_control* p_zGeofenceControl;
		int i =0, regionFree= 0;
	//static McpU32  regionnumbercount; /* Klocwork Changes */

	p_zGPSCControl = gp_zGPSCControl;
	p_zGPSCConfig  = p_zGPSCControl->p_zGPSCConfig;
	p_zGPSCSetMotionMask = p_zGPSCControl->p_zSetMotionMask;
		p_zGeofenceControl = p_zGPSCControl->p_zGeoFenceControl;

	SAPENTER(gpsc_app_set_motion_mask);

		regionFree = gpsc_get_free_region_number(p_zGPSCControl->p_zGeoFenceControl->intRegNoBitmap);

	if(motion_mask->no_of_vertices > GEOFENCE_MAX_VERTICES) {
	STATUSMSG("ERROR: Geofence no of vertices exceeds 10");
		  SAPLEAVE(gpsc_app_set_motion_mask,GPSC_FAIL)
		}
		else if(regionFree	> GEOFENCE_MAX_REGIONS) {
			STATUSMSG("ERROR: Geofence MAX GeoFence Regions reached - %d", GEOFENCE_MAX_REGIONS);
			 SAPLEAVE(gpsc_app_set_motion_mask,GPSC_FAIL)
		 } else if (motion_mask->region_number > GEOFENCE_MAX_REGIONS) {
			STATUSMSG("ERROR: Geofence Region Number cannot be > %d", GEOFENCE_MAX_REGIONS);
			 SAPLEAVE(gpsc_app_set_motion_mask,GPSC_FAIL)
		}

		STATUSMSG("set_mm: InternalRegion#: %d, AppRegion# %d", regionFree, motion_mask->region_number);

		 p_zGPSCSetMotionMask->region_number = motion_mask->region_number;
		p_zGeofenceControl->internalRegionNumber =	regionFree;
		p_zGeofenceControl->feature_flag |= GPSC_FEAT_MOTION_MASK;
		p_zGPSCSetMotionMask->report_configuration = motion_mask->report_configuration;

	p_zGPSCSetMotionMask->region_type = motion_mask->region_type;
	p_zGPSCSetMotionMask->motion_mask_control = motion_mask->motion_mask_control;
	p_zGPSCSetMotionMask->no_of_vertices = motion_mask->no_of_vertices;
	p_zGPSCSetMotionMask->speed_limit = motion_mask->speed_limit;
	p_zGPSCSetMotionMask->altitude_limit = motion_mask->altitude_limit;
	p_zGPSCSetMotionMask->area_altitude= motion_mask->area_altitude;
	p_zGPSCSetMotionMask->radius_of_circle = motion_mask->radius_of_circle;



	 /* Klockwork changes */
	for(i ; ((i< motion_mask->no_of_vertices) && (i < MAX_VERTICES)) ; i++){
	p_zGPSCSetMotionMask->latitude[i]= motion_mask->latitude[i];
	p_zGPSCSetMotionMask->longitude[i] = motion_mask->longitude[i];

	}
	STATUSMSG("Status: app motion mask control %d \n",motion_mask->motion_mask_control);
	STATUSMSG("Status: app motion mask no of vertices %d \n",motion_mask->no_of_vertices);
	STATUSMSG("Status: app motion mask speed limit %d \n",motion_mask->speed_limit);
	STATUSMSG("Status: app motion mask altitude limit %d \n",motion_mask->altitude_limit);
	STATUSMSG("Status: app motion mask area altitude %d \n",motion_mask->area_altitude);
	STATUSMSG("Status: app motion mask radius of circle %d \n",motion_mask->radius_of_circle);
		STATUSMSG("Status: app motion mask region number %d \n",motion_mask->region_number);
		STATUSMSG("Status: app motion mask session number %d \n",motion_mask->uSessionId);

	SAPLEAVE(gpsc_app_set_motion_mask,GPSC_SUCCESS)

}
#endif
extern T_GPSC_result gpsc_app_set_motion_mask (TNAVC_SetMotionMask *motion_mask,McpU32 sessionid)
{
	gpsc_ctrl_type *p_zGPSCControl;

	gpsc_sess_cfg_type*	p_zSessConfig;
	gpsc_set_motion_mask * p_zGPSCSetMotionMask;
	gpsc_geo_fence_control* p_zGeofenceControl;
	int i =0, regionFree= 0;
	McpU8 rep_config=0xFF;
	//static McpU32  regionnumbercount; /* Klocwork Changes */

	p_zGPSCControl = gp_zGPSCControl;
	p_zGPSCSetMotionMask = p_zGPSCControl->p_zSetMotionMask;
	p_zSessConfig = p_zGPSCControl->p_zSessConfig;
	p_zGeofenceControl = p_zGPSCControl->p_zGeoFenceControl;
	gpsc_sess_specific_cfg_type*  p_zSessSpecificCfg=NULL;
	p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type*)gpsc_session_get_node(p_zSessConfig,sessionid);

	SAPENTER(gpsc_app_set_motion_mask);

	regionFree = gpsc_get_free_region_number(p_zGPSCControl->p_zGeoFenceControl->intRegNoBitmap);

	if(motion_mask->no_of_vertices > GEOFENCE_MAX_VERTICES) {
		STATUSMSG("ERROR: Geofence no of vertices exceeds 10");
		SAPLEAVE(gpsc_app_set_motion_mask,GPSC_FAIL)
	}
	else if(regionFree	> GEOFENCE_MAX_REGIONS) {
		STATUSMSG("ERROR: Geofence MAX GeoFence Regions reached - %d", GEOFENCE_MAX_REGIONS);
		SAPLEAVE(gpsc_app_set_motion_mask,GPSC_FAIL)
	} else if (motion_mask->region_number > GEOFENCE_MAX_REGIONS) {
		STATUSMSG("ERROR: Geofence Region Number cannot be > %d", GEOFENCE_MAX_REGIONS);
		SAPLEAVE(gpsc_app_set_motion_mask,GPSC_FAIL)
	}

	STATUSMSG("set_mm: InternalRegion#: %d, AppRegion# %d", regionFree, motion_mask->region_number);

	p_zGeofenceControl->feature_flag |= GPSC_FEAT_MOTION_MASK;
	STATUSMSG("gpsc_app_set_motion_mask: region_number: %d,  report_configuration: %d",
			motion_mask->region_number,
			motion_mask->report_configuration);

	if(NULL!=p_zSessSpecificCfg)
	{
		p_zSessSpecificCfg->GeofenceReportConfigration = motion_mask->report_configuration;
		STATUSMSG("gpsc_app_set_motion_mask: GeofenceReportConfigration %d", p_zSessSpecificCfg->GeofenceReportConfigration);
	}
	else
		STATUSMSG("gpsc_app_set_motion_mask:  NULL == p_zSessSpecificCfg");

	g_repConfig = motion_mask->report_configuration;

	rep_config=gpsc_get_gf_report_config(p_zSessConfig,p_zSessSpecificCfg);
	STATUSMSG("gpsc_app_set_motion_mask: rep_config without GF request is %d", rep_config);

	if(motion_mask->region_number != 0)
	{
		if(1==rep_config)
		{
			p_zGPSCSetMotionMask->report_configuration=1;
		}
		else
			p_zGPSCSetMotionMask->report_configuration = motion_mask->report_configuration;
	}

	else
	{
		if(1==rep_config)
		{
			SAPLEAVE(gpsc_app_set_motion_mask,GPSC_SUCCESS);
		}
		else
		{
			STATUSMSG("gpsc_app_set_motion_mask: updating breach reporting with %d", motion_mask->report_configuration);
			if (motion_mask->report_configuration == 1)
				gpsc_app_rep_on_gf_breach(FALSE);
			if (motion_mask->report_configuration == 0)
				gpsc_app_rep_on_gf_breach(TRUE);
			SAPLEAVE(gpsc_app_set_motion_mask,GPSC_SUCCESS);
		}
	}


	p_zGPSCSetMotionMask->region_number = motion_mask->region_number;

	p_zGPSCSetMotionMask->region_type = motion_mask->region_type;
	p_zGPSCSetMotionMask->motion_mask_control = motion_mask->motion_mask_control;
	p_zGPSCSetMotionMask->no_of_vertices = motion_mask->no_of_vertices;
	p_zGPSCSetMotionMask->speed_limit = motion_mask->speed_limit;
	p_zGPSCSetMotionMask->altitude_limit = motion_mask->altitude_limit;
	p_zGPSCSetMotionMask->area_altitude= motion_mask->area_altitude;
	p_zGPSCSetMotionMask->radius_of_circle = motion_mask->radius_of_circle;
	p_zGPSCSetMotionMask->region_number = motion_mask->region_number;


	/* Klockwork changes */
	for(i ; ((i< motion_mask->no_of_vertices) && (i < MAX_VERTICES)) ; i++){
		p_zGPSCSetMotionMask->latitude[i]= motion_mask->latitude[i];
		p_zGPSCSetMotionMask->longitude[i] = motion_mask->longitude[i];

	}
	gpsc_mgp_tx_inject_motion_mask(p_zGPSCControl);
	STATUSMSG("Status: app motion mask control %d \n",motion_mask->motion_mask_control);
	STATUSMSG("Status: app motion mask no of vertices %d \n",motion_mask->no_of_vertices);
	STATUSMSG("Status: app motion mask speed limit %d \n",motion_mask->speed_limit);
	STATUSMSG("Status: app motion mask altitude limit %d \n",motion_mask->altitude_limit);
	STATUSMSG("Status: app motion mask area altitude %d \n",motion_mask->area_altitude);
	STATUSMSG("Status: app motion mask radius of circle %d \n",motion_mask->radius_of_circle);
	STATUSMSG("Status: app motion mask region number %d \n",motion_mask->region_number);
	STATUSMSG("Status: app motion mask session number %d \n",motion_mask->uSessionId);

	SAPLEAVE(gpsc_app_set_motion_mask,GPSC_SUCCESS)

}

extern T_GPSC_result gpsc_app_get_motion_mask(McpU8  regionnumber)
{
   gpsc_ctrl_type *p_zGPSCControl;

   p_zGPSCControl = gp_zGPSCControl;
   STATUSMSG("Status: app get motion region %d \n",regionnumber);

   SAPENTER(gpsc_app_get_motion_mask);

  gpsc_mgp_tx_req_motion_mask_setting(p_zGPSCControl,regionnumber);
   if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
	SAPLEAVE(gpsc_app_get_motion_mask, GPSC_FAIL)

	SAPLEAVE(gpsc_app_get_motion_mask,GPSC_SUCCESS)
     }

/*
 ******************************************************************************
 * gpsc_app_clear_motion_mask
 *
 * Function description:
 *
 * This function is the external interface that the application uses to instruct
 * the GPSC to clear the motion mask (geo-fence) parameters.
 *
 * Parameters:
 *  struct  TNAVC_SetMotionMask

 *
 *
 * Return value:
 *  T_GPSC_result : Result
 ******************************************************************************
*/
extern T_GPSC_result gpsc_app_clear_motion_mask (TNAVC_SetMotionMask *motion_mask, McpU32 navcSessId)
{
	gpsc_ctrl_type *p_zGPSCControl;
	gpsc_cfg_type *p_zGPSCConfig;
	gpsc_sess_cfg_type*	p_zSessConfig;
	gpsc_set_motion_mask * p_zGPSCSetMotionMask;
	gpsc_geo_fence_control* p_zGeofenceControl;
	U16 i = 0, intRegionNum =0;  /* Klocwork Changes */

	  gpsc_sess_specific_cfg_type*  p_zSessSpecificCfg=NULL;
	 S16 sapResult = GPSC_SUCCESS;

	p_zGPSCControl = gp_zGPSCControl;
	p_zGPSCConfig  = p_zGPSCControl->p_zGPSCConfig;
	p_zGPSCSetMotionMask = p_zGPSCControl->p_zSetMotionMask;
	p_zGeofenceControl = p_zGPSCControl->p_zGeoFenceControl;
	p_zSessConfig = p_zGPSCControl->p_zSessConfig;
	p_zGPSCConfig = p_zGPSCControl->p_zGPSCConfig;

	SAPENTER(gpsc_app_clear_motion_mask);


	if ( navcSessId== C_SESSION_INVALID )
	{
		STATUSMSG("gpsc_app_clear_motion_mask: Invalid Session ID: %d",  navcSessId );
		SAPLEAVE(gpsc_app_clear_motion_mask, GPSC_FAIL)
	}
	else
	{
		 p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type*)gpsc_session_get_node(p_zSessConfig, navcSessId);
	}

	 if(p_zSessSpecificCfg == NULL)
	 {
		 STATUSMSG("gpsc_app_clear_motion_mask: No Session Present: %d",  navcSessId);
		 SAPLEAVE(gpsc_app_clear_motion_mask, GPSC_FAIL)
	 }

	/*Region range is from 1-25*/
	for(intRegionNum=1;intRegionNum<=GEOFENCE_MAX_REGIONS;intRegionNum++)
	{
		if ( motion_mask->region_number == p_zSessSpecificCfg->RegionNumberMap[intRegionNum])
		{
			p_zSessSpecificCfg->RegionNumberMap[intRegionNum] = 0; /*Clear the Region*/
			p_zSessSpecificCfg->RegionBitmap &= ~(0x01 << (intRegionNum-1)); /*Clear the internal region number per this session*/
			if (p_zSessSpecificCfg->RegionNumberMap[0]  >  0)
			   p_zSessSpecificCfg->RegionNumberMap[0] = p_zSessSpecificCfg->RegionNumberMap[0] -1; /* decrement the region counter*/
			break;
		}
		else
			continue;
	}

	if (intRegionNum  >= GEOFENCE_MAX_REGIONS)
	{
		STATUSMSG("gpsc_app_clear_motion_mask: NO Region ID found: %d",  motion_mask->region_number);
		SAPLEAVE(gpsc_app_clear_motion_mask, GPSC_FAIL)
	}

	p_zGeofenceControl->internalRegionNumber = 	intRegionNum;
	p_zGeofenceControl->feature_flag |= GPSC_FEAT_MOTION_MASK;
	p_zGPSCSetMotionMask->report_configuration = motion_mask->report_configuration;

	p_zGPSCSetMotionMask->region_type = motion_mask->region_type;
	p_zGPSCSetMotionMask->motion_mask_control = motion_mask->motion_mask_control;
	p_zGPSCSetMotionMask->no_of_vertices = motion_mask->no_of_vertices;
	p_zGPSCSetMotionMask->speed_limit = motion_mask->speed_limit;
	p_zGPSCSetMotionMask->altitude_limit = motion_mask->altitude_limit;
	p_zGPSCSetMotionMask->area_altitude= motion_mask->area_altitude;
	p_zGPSCSetMotionMask->radius_of_circle = motion_mask->radius_of_circle;


	for(i ; i<= motion_mask->no_of_vertices ; i++){
	p_zGPSCSetMotionMask->latitude[i]= motion_mask->latitude[i];
	p_zGPSCSetMotionMask->longitude[i] = motion_mask->longitude[i];

	}

	if ((gpsc_mgp_tx_inject_motion_mask(p_zGPSCControl)) == TRUE)
	{
		p_zGPSCControl->p_zGeoFenceControl->intRegNoBitmap &= ~(0x01 << (p_zGPSCControl->p_zGeoFenceControl->internalRegionNumber-1));
		sapResult = GPSC_SUCCESS;

				gpsc_cmd_success_cnf(GPSC_SUCCESS);
	}

	else
	{
			sapResult = GPSC_FAIL;

	}
	if (p_zSessSpecificCfg->RegionNumberMap[0] <=0 )
		gpsc_app_loc_fix_stop_req(navcSessId);

	p_zGPSCControl->p_zGeoFenceControl->feature_flag &= ~GPSC_FEAT_MOTION_MASK;
	p_zGPSCControl->p_zGeoFenceControl->internalRegionNumber = 0;

	SAPLEAVE(gpsc_app_clear_motion_mask,sapResult)

}

/*
 ******************************************************************************
 * gpsc_app_set_apm_params
 *
 * Function description:
 *
 * This function is the external interface that the application uses to instruct
 * the GPSC to set the APM parameters.
 *
 * Parameters:
 *  struct  TNAVC_SetAPMParams

 *
 *
 * Return value:
 *  T_GPSC_result : Result
 ******************************************************************************
*/
extern T_GPSC_result gpsc_app_set_apm_params (TNAVC_SetAPMParams *apm_params)
{
	gpsc_ctrl_type *p_zGPSCControl;

	gpsc_dyna_feature_cfg_type *p_zGPSCDynaFeatureConfig;

	p_zGPSCControl = gp_zGPSCControl;
	p_zGPSCDynaFeatureConfig = p_zGPSCControl->p_zGPSCDynaFeatureConfig;
	STATUSMSG("gpsc_app_set_apm_params: Entering.");
	SAPENTER(gpsc_app_set_apm_params);

		p_zGPSCDynaFeatureConfig->feature_flag |= GPSC_FEAT_APM;
	p_zGPSCDynaFeatureConfig->apm_control = apm_params->apm_control;
  	p_zGPSCDynaFeatureConfig->search_mode = apm_params->search_mode;
	p_zGPSCDynaFeatureConfig->saving_options = apm_params->saving_options;
	p_zGPSCDynaFeatureConfig->power_save_qc = apm_params->power_save_qc;
//	p_zGPSCConfig->apm_control = apm_params->apm_control;
	STATUSMSG("gpsc_app_set_apm_params: u_SensorOpMode %d\n",p_zGPSCControl->p_zGPSCState->u_SensorOpMode);
		/* inject advanced power management to sensor */
	STATUSMSG("Injected APM = %d\n", p_zGPSCDynaFeatureConfig->apm_control);
			gpsc_mgp_tx_inject_advanced_power_management (p_zGPSCControl);
//			p_zGPSCDynaFeatureConfig->feature_flag &= ~GPSC_FEAT_APM;
	return GPSC_SUCCESS;

}

/*
 ******************************************************************************
 * gpsc_app_control_apm_params
 *
 * Function description:
 *
 * This function is the external interface that the application uses to instruct
 * the GPSC to set the APM parameters.
 *
 * Parameters:
 *  struct  TNAVC_SetAPMParams

 *
 *
 * Return value:
 *  T_GPSC_result : Result
 ******************************************************************************
 */
extern T_GPSC_result gpsc_app_control_apm_params (TNAVC_SetAPMParams *apm_params, McpU32 sessionid)
{
	gpsc_ctrl_type *p_zGPSCControl;
	gpsc_cfg_type *p_zGPSCConfig;
	gpsc_sess_cfg_type*	p_zSessConfig;
	gpsc_state_type*	p_zGPSCState;
	McpU8 apm_config=0xFF;
	p_zGPSCControl = gp_zGPSCControl;
	p_zGPSCConfig  = p_zGPSCControl->p_zGPSCConfig;
	p_zSessConfig = p_zGPSCControl->p_zSessConfig;
	p_zGPSCState  = p_zGPSCControl->p_zGPSCState;
	p_zGPSCState->u_GeofenceAPMEnabled = apm_params->apm_control;
	gpsc_sess_specific_cfg_type*  p_zSessSpecificCfg=NULL;
	p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type*)gpsc_session_get_node(p_zSessConfig,sessionid);
	g_apmConfig=apm_params->apm_control;
	if(NULL!=p_zSessSpecificCfg)
		p_zSessSpecificCfg->u_ApmConfig=apm_params->apm_control;
	STATUSMSG("gpsc_app_control_apm_params: calling gpsc_app_get_apm_config .\n");
	apm_config = gpsc_app_get_apm_config(p_zSessConfig,p_zSessSpecificCfg);
	U32 rep_config=gpsc_get_gf_report_config(p_zSessConfig,NULL);
	if(0==apm_config)
	{
		SAPLEAVE(gpsc_app_set_motion_mask,GPSC_SUCCESS);
		STATUSMSG("gpsc_app_control_apm_params: apm_config = %d",apm_config);
	}
	else
	{
	if(rep_config !=0 && (apm_params->apm_control== 0)) {
		if(p_zGPSCConfig->apm_control == 1) {
		STATUSMSG("gpsc_app_control_apm_params: APM session currently running, Normal session request with apm enabled..Change APM Config .\n");
		STATUSMSG("gpsc_app_control_apm_params: p_zGPSCConfig->search_mode %d\n",p_zGPSCConfig->search_mode);
		STATUSMSG("gpsc_app_control_apm_params:p_zGPSCConfig->saving_options %d\n",p_zGPSCConfig->saving_options);
		STATUSMSG("gpsc_app_control_apm_params:p_zGPSCConfig->power_save_qc %d\n",p_zGPSCConfig->power_save_qc);
		TNAVC_SetAPMParams apm_param;
		apm_param.apm_control=p_zGPSCConfig->apm_control;
		apm_param.search_mode=p_zGPSCConfig->search_mode;
		apm_param.saving_options=p_zGPSCConfig->saving_options;
		apm_param.power_save_qc=p_zGPSCConfig->power_save_qc;
		gpsc_app_set_apm_params(&apm_param);
		}
		}
		else {
		STATUSMSG("gpsc_app_control_apm_params: Change APM config.");
		gpsc_app_set_apm_params(apm_params);
		}
	}
	STATUSMSG("gpsc_app_control_apm_params: Exiting.");
	return GPSC_SUCCESS;

}

/*
 ******************************************************************************
 * gpsc_app_gps_ver_req
 *
 * Function description:
 *
 * This function is the external interface that the application uses to instruct
 * the GPSC to request the software and Hardware version of the GPS chip.
 *
 * Parameters:
 *  None
 *
 *
 * Return value:
 *  T_GPSC_result : Result
 * Note: Added support to request the software and hardware information of GPS
 *       sensor
 ******************************************************************************
*/
T_GPSC_result gpsc_app_gps_ver_req (void)
{

	gpsc_ctrl_type*	p_zGPSCControl;
	gpsc_state_type*	p_zGPSCState;


	p_zGPSCControl = gp_zGPSCControl;

	p_zGPSCState  = p_zGPSCControl->p_zGPSCState;


	SAPENTER(gpsc_app_gps_ver_req);

	STATUSMSG(" HW and SW Version request in state = %d",p_zGPSCState->u_GPSCState);

	/* Check if GPSC is in Patch Control state */
//	if(p_zGPSCState->u_GPSCState < C_GPSC_STATE_WAIT_FOR_STATUS)
	if(p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq < E_GPSC_SEQ_READY_BEGIN_DOWNLOAD)
	{
		p_zGPSCState->u_GpsVersionPending = TRUE;
		ERRORMSG("Error : Request for HW and SW version is not valid in this state");
		SAPLEAVE(gpsc_app_gps_ver_req,GPSC_FAIL)
	}

	gpsc_mgp_ver_req(p_zGPSCControl);
	if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
				SAPLEAVE(gpsc_app_gps_ver_req, GPSC_TX_INITIATE_FAIL)

	SAPLEAVE(gpsc_app_gps_ver_req,GPSC_SUCCESS)
}

/*
 *  ******************************************************************************
 *  gpsc_app_blf_session_request
 *
 *  Function description:
 *  Initialise and create a BLF session
 *
 *  Parameters:
 *  None
 *
 *
 *  Return value:
 *  T_GPSC_result : Result
 ***********************************************************************************
 */
T_GPSC_result gpsc_app_blf_session_request(TNAVC_BLF_Session_Config *blf_cfg)
{
	gpsc_ctrl_type*	p_zGPSCControl;
	gpsc_sess_cfg_type*	p_zSessConfig;
	gpsc_dyna_feature_cfg_type* p_zGPSCDynaFeatureConfig;
    //gpsc_sess_specific_cfg_type*  p_zSessSpecificCfg=NULL;  /* Klocwork Changes */
	gpsc_event_cfg_type * p_zEventCfg;
        T_GPSC_result gpscRes;

	SAPENTER(gpsc_app_blf_session_request);

	p_zGPSCControl = gp_zGPSCControl;
	p_zSessConfig = p_zGPSCControl->p_zSessConfig;
	p_zEventCfg   = p_zGPSCControl->p_zEventCfg;
	p_zGPSCDynaFeatureConfig = p_zGPSCControl->p_zGPSCDynaFeatureConfig;

       /* Check if this is the first BLF session. We only allow only one
	   BLF session to run */
       if(p_zSessConfig->u_BLF_State != C_BLF_STATE_DISABLED)
       {
		STATUSMSG("ERROR: New BLF session cannot be started, as there is an existing BLF session.\n");
               	SAPLEAVE(gpsc_app_blf_session_request, GPSC_FAIL)
       }
       /* Validate the user provided buffer count */
       if(blf_cfg->blf_fix_count > C_MAX_BLF_BUFFER_COUNT)
       {
               	STATUSMSG("ERROR: BLF buffer count %d is > supported %d\n",blf_cfg->blf_fix_count,
                                                                                               C_MAX_BLF_BUFFER_COUNT);
               	SAPLEAVE(gpsc_app_blf_session_request, GPSC_GPS_INVALID_BLF_PARMS_FAIL)
       }

       /* Check whether normal sessions are already running */
       if(p_zSessConfig->w_TotalNumSession > 0)
       {
		/* As normal session is already running, we cannot enable the
		   hardware BLF feature. Insted, we run the BLF session as a normal
		   session with the requested periodicity untill all normal sesssions
		   are stopped. */
		STATUSMSG("INFO: Hardware BLF will not be enabled as normal sessions are already running.\n");
		p_zGPSCDynaFeatureConfig->blf_fix_count = blf_cfg->blf_fix_count;
		/* Update the state to C_BLF_STATE_DORMANT so that hardware blf feature
		   can be automatically enabled when all normal sessions goes away */
       		p_zSessConfig->u_BLF_State = C_BLF_STATE_DORMANT;
       		p_zSessConfig->u_BLF_Count = blf_cfg->blf_fix_count;
		STATUSMSG("INFO: BLF moved to DORMANT state.\n");
       }
       else
       {
              	/* There is no sessions currently running. Requested BLF session is the only one.
		   Configure BLF in hardware */
                gpscRes = gpsc_app_blf_config_req(1, blf_cfg->blf_fix_count);
                if (gpscRes < GPSC_SUCCESS)
                {
                	STATUSMSG("ERROR:Configuring BLF failed.\n");
                	return gpscRes;
                }
		/* Update the state to C_BLF_STATE_ENABLED */
       		p_zSessConfig->u_BLF_State = C_BLF_STATE_ENABLED;
       		p_zSessConfig->u_BLF_Count = blf_cfg->blf_fix_count;
       		/* Disable extended reports. BLF will not work with extended reports */
       		p_zEventCfg->u_Ext_reports = 0;
       }

       SAPLEAVE(gpsc_app_blf_session_request, GPSC_SUCCESS)
}

/*
 *  ******************************************************************************
 *  gpsc_app_blf_status_res
 *
 *  Function description:
 *  Send BLF status response to the application
 *
 *  Parameters:
 *  None
 *
 *
 *  Return value:
 *  T_GPSC_result : Result
 ***********************************************************************************
 */
T_GPSC_result gpsc_app_blf_status_res (void)
{
        gpsc_ctrl_type* p_zGPSCControl = gp_zGPSCControl;
        gpsc_state_type*        p_zGPSCState;
        TNAVC_BlfStatusReport blfStatusReport, *p_blfStatusReport = &blfStatusReport;

        p_zGPSCState  = p_zGPSCControl->p_zGPSCState;

        SAPENTER(gpsc_app_blf_status_res);

        p_blfStatusReport->blf_fix_count = p_zGPSCState->blf_fix_count;
        p_blfStatusReport->blf_fix_count_threshold = p_zGPSCState->blf_fix_count_threshold;
        p_blfStatusReport->blf_fix_count_rx = p_zGPSCState->blf_fix_count_rx;
        p_blfStatusReport->blf_sts = p_zGPSCState->blf_sts;
        p_blfStatusReport->blf_position_sts = p_zGPSCState->blf_position_sts;
        p_blfStatusReport->blf_velocity_sts = p_zGPSCState->blf_velocity_sts;
        p_blfStatusReport->blf_dop_sts = p_zGPSCState->blf_dop_sts;
        p_blfStatusReport->blf_sv_sts = p_zGPSCState->blf_sv_sts;

        gpsc_app_blf_status_ind (p_blfStatusReport);

        SAPLEAVE(gpsc_app_blf_status_res, GPSC_SUCCESS)
}

/*
 ******************************************************************************
 * gpsc_app_gps_ver_res
 *
 * Function description:
 * Send version response to the application
 *
 * Parameters:
 *  None
 *
 *
 * Return value:
 *  T_GPSC_result : Result
 ******************************************************************************
*/
T_GPSC_result gpsc_app_gps_ver_res (void)
{
	U8 u_len, uChipId;
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	gpsc_state_type*	p_zGPSCState;
	gps_version* p_zGPSVersion;
	handle_t	 pMcpf  = (handle_t *)p_zGPSCControl->p_zSysHandlers->hMcpf;
	T_GPSC_gps_ver_resp_params z_GPSVerControl,*p_zGPSVerControl = &z_GPSVerControl ;

#ifdef SIMULATE_DR
	static hit_count = 0;
#endif

	p_zGPSCState  = p_zGPSCControl->p_zGPSCState;
	p_zGPSVersion = p_zGPSCControl->p_zGPSVersion;

	SAPENTER(gpsc_app_gps_ver_res);

	STATUSMSG(" Version response in state = %d",p_zGPSCState->u_GPSCState);

	/* Chip ID */
	p_zGPSVerControl->ChipIDMajorVerNum=p_zGPSVersion->u_ChipIDMajorVerNum;
	p_zGPSVerControl->ChipIDMinorVerNum=p_zGPSVersion->u_ChipIDMinorVerNum;

	/* ROM ID */
	p_zGPSVerControl->ROMIDMajorVerNum=p_zGPSVersion->u_ROMIDMajorVerNum;
	p_zGPSVerControl->ROMIDMinorVerNum=p_zGPSVersion->u_ROMIDMinorVerNum;
	p_zGPSVerControl->ROMIDSubMinor1VerNum=p_zGPSVersion->u_ROMIDSubMinor1VerNum;
	p_zGPSVerControl->ROMIDSubMinor2VerNum=p_zGPSVersion->u_ROMIDSubMinor2VerNum;

	/* ROM DATE */
	p_zGPSVerControl->ROMDay=p_zGPSVersion->u_ROMDay;
	p_zGPSVerControl->ROMMonth=p_zGPSVersion->u_ROMMonth;
	p_zGPSVerControl->ROMYear=p_zGPSVersion->w_ROMYear;

	/* PATCH VERSION */
	p_zGPSVerControl->PATCHMajor=p_zGPSVersion->u_PATCHMajor;
	p_zGPSVerControl->PATCHMinor=p_zGPSVersion->u_PATCHMinor;

	/* PATCH DATE */
	p_zGPSVerControl->PATCHDay=p_zGPSVersion->u_PATCHDay;
	p_zGPSVerControl->PATCHMonth=p_zGPSVersion->u_PATCHMonth;
 	p_zGPSVerControl->PATCHYear=p_zGPSVersion->w_PATCHYear;

	/* GPSC VERSION */
	p_zGPSVerControl->gpsc_major = NULL;
	p_zGPSVerControl->gpsc_minor = NULL;
	p_zGPSVerControl->gpsc_subminor1 = NULL;
	p_zGPSVerControl->gpsc_subminor2 = NULL;

	/* GPSC DATE */
	p_zGPSVerControl->gpsc_day = NULL;
	p_zGPSVerControl->gpsc_month = NULL;
	p_zGPSVerControl->gpsc_year = NULL;

	/* CHIP ID str */
	uChipId = p_zGPSVersion->u_SysConfig & 0x70; //extract bits 6-4
	if(uChipId == NL_5500) // check if it is NL5500
		{
			mcpf_mem_copy(pMcpf, p_zGPSVerControl->chipID_str, "NL5500", 6);
			p_zGPSVerControl->chipID_str[6] = '\0';
		STATUSMSG("gpsc_app_gps_ver_res: NL5500");
	}
	else if(uChipId == WL_128x)
	{
		mcpf_mem_copy(pMcpf, p_zGPSVerControl->chipID_str, "WL128X", 6);
		p_zGPSVerControl->chipID_str[6] = '\0';
		STATUSMSG("gpsc_app_gps_ver_res: WL128X");
	}
	else
	{
		mcpf_mem_copy(pMcpf, p_zGPSVerControl->chipID_str, "UnKnown", 7);
		p_zGPSVerControl->chipID_str[7] = '\0';
		STATUSMSG("gpsc_app_gps_ver_res: Unknown: System config: 0x%X",p_zGPSVersion->u_SysConfig);
		}
	/* Navilink Version */
	u_len = (U8)MCP_HAL_STRING_StrLen(SW_VERSION_STR);
	mcpf_mem_copy(pMcpf, p_zGPSVerControl->navilink_ver, SW_VERSION_STR, u_len);
	p_zGPSVerControl->navilink_ver[u_len] = '\0';

	/* NAVC Version */
	u_len = (U8)MCP_HAL_STRING_StrLen(NAVC_VERSION_STR);
	mcpf_mem_copy(pMcpf, p_zGPSVerControl->navc_ver, NAVC_VERSION_STR, u_len);
	p_zGPSVerControl->navc_ver[u_len] = '\0';
#ifdef SIMULATE_DR
	hit_count++;

	if(hit_count > 2)
	{
        /*U8 u_Data[9] = {0xD0,0x10,0xFD,0xB3,0x0A,0x00,0x01,0x01,0x01};*/
		U8 u_Data[9] = {0xD0,0x10,0x2D,0xB1,0x0A,0x00,0x01,0x01,0x01};

	    u_Data[8] = (hit_count & 0x1);

		gpsc_mgp_dr_req(p_zGPSCControl, &u_Data[0], 9);
		if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
					STATUSMSG(" DR Transmission Failed");
	}
#endif

	gpsc_app_gps_ver_ind (p_zGPSVerControl);

	SAPLEAVE(gpsc_app_gps_ver_res,GPSC_SUCCESS)
}


/*
 ******************************************************************************
 * gpsc_app_simulate_dr
 *
 * Function description:
 *
 * This function is used to send the AI2 message to the Rx to simulate
 * dead reckoning situation.
 *
 * Parameters:
 *  None
 *
 *
 * Return value:
 *  T_GPSC_result : GPSC_FAIL/GPSC_SUCCESS
 *
 ******************************************************************************
*/
extern void gpsc_app_simulate_dr(U8 *p_buf, U32 length)
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;

	gpsc_mgp_dr_req(p_zGPSCControl, p_buf, length);
	if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
	{
	    STATUSMSG(" DR Transmission Failed");
	    SAPLEAVE(gpsc_app_simulate_dr,GPSC_FAIL)
	}
	else
	{
		SAPLEAVE(gpsc_app_simulate_dr,GPSC_SUCCESS)
	}
}


/*
 ******************************************************************************
 * gpsc_app_nvs_file_req
 *
 * Function description:
 *
 * This function is the external interface that the application uses store the
 * assistance from GPSC data base to NVS File unconditionally.
 *
 * Parameters:
 *  None
 *
 *
 * Return value:
 *  T_GPSC_result : GPSC_FAIL/GPSC_SUCCESS
 *
 ******************************************************************************
*/
extern T_GPSC_result gpsc_app_nvs_file_req (void)
{

	gpsc_ctrl_type*	p_zGPSCControl;
	gpsc_state_type*	p_zGPSCState;

	SAPENTER(gpsc_app_nvs_file_req);

	p_zGPSCControl = gp_zGPSCControl;
	p_zGPSCState  = p_zGPSCControl->p_zGPSCState;

	p_zGPSCState->u_UpdateNvsRequest = TRUE;

	if(p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq < E_GPSC_SEQ_INIT)
	{
	  p_zGPSCState->u_UpdateNvsRequest = FALSE;
	  ERRORMSG("Error : Request to update NVS is not allowed before init");
	  SAPLEAVE(gpsc_app_nvs_file_req,GPSC_FAIL)
	}
	else
	{
	  gpsc_db_save_to_nvs(p_zGPSCControl->p_zGPSCDatabase);
	  p_zGPSCState->u_UpdateNvsRequest = FALSE;
	  SAPLEAVE(gpsc_app_nvs_file_req,GPSC_SUCCESS)

	}


}

/*
 ******************************************************************************
 * gpsc_app_blf_buff_dump_req
 *
 * Function description:
 *
 * This function is the external interface that the application uses to dump
 * the location fix buffer
 *
 * Parameters: None
 *
 * Return value:
 *  T_GPSC_result : GPSC_FAIL/GPSC_SUCCESS
 *
 ******************************************************************************
*/
extern T_GPSC_result gpsc_app_blf_buff_dump_req()
{
  gpsc_ctrl_type*  p_zGPSCControl;

  SAPENTER(gpsc_app_blf_buff_dump_req);

  p_zGPSCControl = gp_zGPSCControl;

  if(p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq < E_GPSC_SEQ_READY_BEGIN_DOWNLOAD)
  {
	ERRORMSG("Error : BLF dump request is not valid in this state");
	SAPLEAVE(gpsc_app_blf_buff_dump_req, GPSC_FAIL)
  }

  gpsc_mgp_tx_blf_buff_dump(p_zGPSCControl);

  if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
  {
        SAPLEAVE(gpsc_app_blf_buff_dump_req, GPSC_TX_INITIATE_FAIL)
  }

  SAPLEAVE(gpsc_app_blf_buff_dump_req, GPSC_SUCCESS)
}


/*
 ******************************************************************************
 * gpsc_app_blf_sts_req
 *
 * Function description:
 *
 * This function is the external interface that the application uses to get the
 * status of the Buffered location FIx
 *
 * Parameters: None
 *
 * Return value:
 *  T_GPSC_result : GPSC_FAIL/GPSC_SUCCESS
 *
 ******************************************************************************
*/
extern T_GPSC_result gpsc_app_blf_sts_req()
{
  gpsc_ctrl_type*  p_zGPSCControl;

  SAPENTER(gpsc_app_blf_sts_req);

  p_zGPSCControl = gp_zGPSCControl;

  if(p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq < E_GPSC_SEQ_READY_BEGIN_DOWNLOAD)
  {
	ERRORMSG("Error : BLF status request is not valid in this state");
	SAPLEAVE(gpsc_app_blf_sts_req, GPSC_FAIL)
  }

  gpsc_mgp_tx_blf_sts(p_zGPSCControl);

  if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
  {
        SAPLEAVE(gpsc_app_blf_sts_req, GPSC_TX_INITIATE_FAIL)
  }

  SAPLEAVE(gpsc_app_blf_sts_req, GPSC_SUCCESS)
}


/*
 ******************************************************************************
 * gpsc_app_blf_config_req
 *
 * Function description:
 *
 * This function is the external interface that the application uses configure
 * the buffered location fix feature
 *
 * Parameters:
 *
 * U8 blf_en_flg - Enable / Disable flag, U32 blf_fix_count - Buffer count
 *
 * Return value:
 *  T_GPSC_result : GPSC_FAIL/GPSC_SUCCESS
 *
 ******************************************************************************
*/
extern T_GPSC_result gpsc_app_blf_config_req(U8 blf_en_flg, U32 blf_fix_count)
{
  gpsc_ctrl_type*  p_zGPSCControl;
  gpsc_dyna_feature_cfg_type* p_zGPSCDynaFeatureConfig;

  SAPENTER(gpsc_app_blf_config_req);

  p_zGPSCControl = gp_zGPSCControl;
  p_zGPSCDynaFeatureConfig = p_zGPSCControl->p_zGPSCDynaFeatureConfig;

  if(p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq < E_GPSC_SEQ_READY_BEGIN_DOWNLOAD)
  {
	ERRORMSG("Error : BLF config is not valid in this state");
	SAPLEAVE(gpsc_app_blf_config_req, GPSC_FAIL)
  }

  p_zGPSCDynaFeatureConfig->blf_en_flg = blf_en_flg;
  p_zGPSCDynaFeatureConfig->blf_fix_count = blf_fix_count;
  if(blf_en_flg)
  {
  	p_zGPSCDynaFeatureConfig->blf_en_position = 1; /* Only position buffering is supported */
  }
  else
  {
  	p_zGPSCDynaFeatureConfig->blf_en_position = 0;
  }
  p_zGPSCDynaFeatureConfig->blf_en_velocity = 0;
  p_zGPSCDynaFeatureConfig->blf_en_dop = 0;
  p_zGPSCDynaFeatureConfig->blf_en_sv = 0;

  gpsc_mgp_tx_blf_config(p_zGPSCControl);

  if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_ACK_REQ)==FALSE)
  {
        SAPLEAVE(gpsc_app_blf_config_req, GPSC_TX_INITIATE_FAIL)
  }

  SAPLEAVE(gpsc_app_blf_config_req, GPSC_SUCCESS)
}




extern T_GPSC_result gpsc_app_send_msa_supl_postion(TNAVC_posIndData* posInd)
{
    U32             w_SessionTagId[25];
	S8 			    u_TotSessions = 0;
	S8			    u_CurSession = 0;
	SAPCALL(gpsc_app_send_msa_supl_postion);
	#define GPS_SECS 604800

	T_GPSC_loc_fix_response zLocFixResponse;
	T_GPSC_loc_fix_response *p_zLocFixResponse = (T_GPSC_loc_fix_response*)&zLocFixResponse;
	T_GPSC_prot_position        *p_prot_position    = &p_zLocFixResponse->loc_fix_response_union.prot_position;
	p_zLocFixResponse->ctrl_loc_fix_response_union = GPSC_RESULT_PROT_POSITION;


   	T_GPSC_ellip_alt_unc_ellip  *pGpsPosInfo = &p_prot_position->ellip_alt_unc_ellip;
	gpsc_ctrl_type*	p_zGPSCControl=gp_zGPSCControl;
    gpsc_sess_cfg_type* pSessCfg = p_zGPSCControl->p_zSessConfig;

	gpsc_db_type          *p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_db_gps_clk_type  *p_zDBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;

    p_prot_position->gps_tow = 	p_zDBGpsClock->w_GpsWeek*GPS_SECS + (p_zDBGpsClock->q_GpsMs)/1000;

	p_prot_position->prot_fix_result =	GPSC_PROT_FIXRESULT_2D;
	pGpsPosInfo->shape_code =  C_POS_SHAPE_ELLIP_ALT_UNC_ELLIP; /*9*/
	pGpsPosInfo->v_altitude = 1;
	pGpsPosInfo->v_altitude_sign = 1;
	pGpsPosInfo->v_unc_alt = 1;

	if (NULL==posInd)
		return GPSC_FAIL;

		gpsc_store_msa_pos_in_db(posInd);

	pGpsPosInfo->latitude = posInd->position.latitude;
	pGpsPosInfo->longitude = posInd->position.longtitude;
	pGpsPosInfo->latitude_sign = posInd->position.latitude_sign;
	if(posInd->position.pos_opt_bitmap & 0x01)
        {
		STATUSMSG("pGpsPosInfo->pos_opt_bitmap :: ALTITUDE PRESENT.. 3D FIX");
	pGpsPosInfo->altitude = posInd->position.altitude;
          pGpsPosInfo->unc_alt = posInd->position.altUncertainty;
	pGpsPosInfo->altitude_sign = posInd->position.altitudeDirection;
          p_prot_position->prot_fix_result =      GPSC_PROT_FIXRESULT_3D;
        }
	if(posInd->position.pos_opt_bitmap & 0x02)
        {
		STATUSMSG("pGpsPosInfo->pos_opt_bitmap :: Uncertaintity Present");
	pGpsPosInfo->unc_major = posInd->position.uncertaintySemiMajor;
	pGpsPosInfo->unc_minor = posInd->position.uncertaintySemiMinor;
	pGpsPosInfo->orient_major = posInd->position.orientationMajorAxis;
	}
	if(posInd->position.pos_opt_bitmap & 0x04)
	pGpsPosInfo->confidence = posInd->position.confidence;
	if(posInd->position.pos_opt_bitmap & 0x08)
	{
      pGpsPosInfo->velocity_flag = posInd->position.velocity.velocity_flag;
		STATUSMSG("pGpsPosInfo->velocity_flag = %d", pGpsPosInfo->velocity_flag);
	  if(pGpsPosInfo->velocity_flag & (BEARING | HORSPEED))
	  {

			STATUSMSG("pGpsPosInfo->horuncertspeed = %d", pGpsPosInfo->horuncertspeed);
	   pGpsPosInfo->bearing = posInd->position.velocity.bearing;
           pGpsPosInfo->horspeed = posInd->position.velocity.horspeed;
          }
          if(pGpsPosInfo->velocity_flag & (VERDIRECT | VERSPEED))
          {
			STATUSMSG("pGpsPosInfo->veruncertspeed = %d", pGpsPosInfo->veruncertspeed);
	   pGpsPosInfo->verdirect = posInd->position.velocity.verdirect;
           pGpsPosInfo->verspeed = posInd->position.velocity.verspeed;
          }
	  if(pGpsPosInfo->velocity_flag & HORUNCERTSPEED)
          {
	    pGpsPosInfo->horuncertspeed = posInd->position.velocity.horuncertspeed;
          }
	  if(pGpsPosInfo->velocity_flag & VERUNCERTSPEED)
          {
	   pGpsPosInfo->veruncertspeed = posInd->position.velocity.veruncertspeed;
          }

        }
        else
    {
           pGpsPosInfo->velocity_flag = 0x0;
		STATUSMSG("pGpsPosInfo->velocity_flag = 0x0");
    }
		p_prot_position->gps_secs= (DBL) gp_zGPSCControl->p_zGPSCDatabase->z_DBPos.d_ToaSec;
	STATUSMSG("UTC: p_prot_position->gps_secs = %ld", p_prot_position->gps_secs);

		pGpsPosInfo->utctime = gp_zGPSCControl->p_zGPSCDatabase->z_DBPos.d_ToaSec;
	STATUSMSG("UTC: pGpsPosInfo->utctime = %ld", pGpsPosInfo->utctime);
		gpsc_db_gps_time_in_secs(p_zGPSCControl);
        gpsc_session_get_all_nodes(pSessCfg, &w_SessionTagId, &u_TotSessions);
        for(u_CurSession=0;u_CurSession<u_TotSessions;u_CurSession++)
        {

        p_zLocFixResponse->loc_fix_session_id = w_SessionTagId[u_CurSession];
		gpsc_app_loc_fix_ind (p_zLocFixResponse);
        }
      SAPLEAVE(gpsc_app_send_msa_supl_postion,GPSC_SUCCESS);
	return GPSC_SUCCESS;
}
/*
 ******************************************************************************
 * gpsc_app_inject_gps_clock_accuracy_req
 *
 * Function description:
 *
 * This function is the external interface that the application uses to instruct
 * the GPSC to inject the GPS_CLK accuracy
 *
 * Parameters:
 *  None
 *
 *
 * Return value:
 *  T_GPSC_result : GPSC_FAIL/GPSC_SUCCESS
 *
 ******************************************************************************
*/
extern T_GPSC_result gpsc_app_inject_gps_clock_accuracy_req(void)
{
	gpsc_ctrl_type*	p_zGPSCControl;

	p_zGPSCControl = gp_zGPSCControl;

	SAPCALL(gpsc_os_file_read_ind);

	p_zGPSCControl->p_zGPSCState->u_FreqInjectRequest = TRUE;

	SAPLEAVE(gpsc_os_file_read_ind,GPSC_SUCCESS);

	return GPSC_SUCCESS;
}

/*
 ******************************************************************************
 * gpsc_app_assist_database_req
 *
 * Function description:
 *
 * GPSM shall use this function to get the assistance data from GPSC database.
 * This may be acquisition assistance data, ephemeris data, ionospheric delay coefficients,
 * inititial position estimates, or almanac data according to the union type.The pointer
 * provided by the GPSM will be populated with assistance present in the GPSC Database
 * during the request made.
 *
 * Parameters:
 *  T_GPSC_assistance_database_report P: Pointer to structure to populate the assistance database
 *
 *
 * Return value:
 *  T_GPSC_result : GPSC_FAIL/GPSC_SUCCESS
 *
 ******************************************************************************
*/
T_GPSC_result gpsc_app_assist_database_req (T_GPSC_assistance_database_report *assistance_database_report)
{
	gpsc_ctrl_type*	p_zGPSCControl;
	U8 counter1,counter2;
	T_GPSC_assistance_inject_union_report *p_assistance_inject_union =  &assistance_database_report->assistance_inject_union_report;
	T_GPSC_ephemeris_assist_rep *p_ephemeris_assist = &assistance_database_report->assistance_inject_union_report.ephemeris_assist_rep;
	T_GPSC_almanac_assist_rep *p_almanac_assist = &assistance_database_report->assistance_inject_union_report.almanac_assist_rep;
	p_zGPSCControl = gp_zGPSCControl;

	gpsc_smlc_assist_type* p_zSmlcAssist = p_zGPSCControl->p_zSmlcAssist;

	SAPCALL(gpsc_app_assist_database_req);
	SAPENTER(gpsc_app_assist_database_req);

	switch (assistance_database_report->ctrl_assistance_inject_union_report)
	{
			  case GPSC_ASSIST_EPH_REP:  /*  Ephemeris assistance data  */
					STATUSMSG("gpsc_app_assist_database_req: GPSC_ASSIST_EPH");

					counter1=0; counter2=0;
					for (counter1=0;counter1<N_SV;counter1++)
					{
						counter2=0;

						if( (TRUE == p_zSmlcAssist->z_SmlcRawEph[counter1].u_available) && (FALSE == p_zSmlcAssist->z_SmlcRawEph[counter1].toe_expired ))
						{
								p_ephemeris_assist->ephemeris_assist[counter1].svid = (U8) (p_zSmlcAssist->z_SmlcRawEph[counter1].u_Prn) - 1;
								p_ephemeris_assist->ephemeris_assist[counter1].iode = (U8)p_zSmlcAssist->z_SmlcRawEph[counter1].u_Iode;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_crs = (S16)p_zSmlcAssist->z_SmlcRawEph[counter1].w_Crs;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_delta_n = (S16)p_zSmlcAssist->z_SmlcRawEph[counter1].w_DeltaN;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_cuc = (S16)p_zSmlcAssist->z_SmlcRawEph[counter1].w_Cuc;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_m0 = (S32)p_zSmlcAssist->z_SmlcRawEph[counter1].q_MZero;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_e = (U32)p_zSmlcAssist->z_SmlcRawEph[counter1].q_E;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_omega_a0 = (S32)p_zSmlcAssist->z_SmlcRawEph[counter1].q_OmegaZero;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_cus = (S16)p_zSmlcAssist->z_SmlcRawEph[counter1].w_Cus;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_toe = (U16)p_zSmlcAssist->z_SmlcRawEph[counter1].w_Toe;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_cic = (S16)p_zSmlcAssist->z_SmlcRawEph[counter1].w_Cic;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_cis = (S16)p_zSmlcAssist->z_SmlcRawEph[counter1].w_Cis;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_i0 = (S32)p_zSmlcAssist->z_SmlcRawEph[counter1].q_IZero;

								/**/
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_omega_adot = (S32)p_zSmlcAssist->z_SmlcRawEph[counter1].q_OmegaDot;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_idot = (S16)p_zSmlcAssist->z_SmlcRawEph[counter1].w_IDot;

								p_ephemeris_assist->ephemeris_assist[counter1].ephem_crc 					= (S16)p_zSmlcAssist->z_SmlcRawEph[counter1].w_Crc;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_code_on_l2 	= (U8)p_zSmlcAssist->z_SmlcRawSF1Param[counter1].u_CodeL2;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_svhealth 			= (U8)p_zSmlcAssist->z_SmlcRawSF1Param[counter1].u_Health;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_tgd 					= (S8)p_zSmlcAssist->z_SmlcRawSF1Param[counter1].u_Tgd;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_iodc 				= (U16)p_zSmlcAssist->z_SmlcRawSF1Param[counter1].w_Iodc;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_toc 					= (U16)p_zSmlcAssist->z_SmlcRawSF1Param[counter1].w_Toc;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_af2 					= (S8)p_zSmlcAssist->z_SmlcRawSF1Param[counter1].u_Af2;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_af1 					= (S16)p_zSmlcAssist->z_SmlcRawSF1Param[counter1].w_Af1;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_af0 					= (S32)p_zSmlcAssist->z_SmlcRawSF1Param[counter1].q_Af0;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_ura 					= (U8)p_zSmlcAssist->z_SmlcRawSF1Param[counter1].u_Accuracy;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_w 					= (S32)p_zSmlcAssist->z_SmlcRawEph[counter1].q_Omega;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_a_power_half 	= (U32)p_zSmlcAssist->z_SmlcRawEph[counter1].q_SqrtA;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_predicted 		= 	(U8)p_zSmlcAssist->z_SmlcRawEph[counter1].u_predicted;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_predSeedAge = (U8)p_zSmlcAssist->z_SmlcRawEph[counter1].u_predSeedAge;

								counter2 = 1;
						}

						/*Check the validity of the data base, if database has a valid one, update with the latest one available in database*/
						if(p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].u_Valid)
						{
								p_ephemeris_assist->ephemeris_assist[counter1].svid 								= (U8)(p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawEphemeris.u_Prn) - 1;
								p_ephemeris_assist->ephemeris_assist[counter1].iode = (U8)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawEphemeris.u_Iode;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_crs = (S16)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawEphemeris.w_Crs;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_delta_n = (S16)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawEphemeris.w_DeltaN;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_cuc = (S16)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawEphemeris.w_Cuc;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_m0 = (S32)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawEphemeris.q_MZero;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_e = (U32)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawEphemeris.q_E;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_omega_a0 = (S32)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawEphemeris.q_OmegaZero;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_cus = (S16)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawEphemeris.w_Cus;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_toe = (U16)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawEphemeris.w_Toe;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_cic = (S16)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawEphemeris.w_Cic;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_cis = (S16)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawEphemeris.w_Cis;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_i0 = (S32)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawEphemeris.q_IZero;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_omega_adot 	= (S32)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawEphemeris.q_OmegaDot;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_crc 					= (S16)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawEphemeris.w_Crc;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_idot					= (S16)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawEphemeris.w_IDot;
									p_ephemeris_assist->ephemeris_assist[counter1].ephem_code_on_l2 = (U8)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawSF1Param.u_CodeL2;
									p_ephemeris_assist->ephemeris_assist[counter1].ephem_svhealth = (U8)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawSF1Param.u_Health;
									p_ephemeris_assist->ephemeris_assist[counter1].ephem_tgd = (S8)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawSF1Param.u_Tgd;
									p_ephemeris_assist->ephemeris_assist[counter1].ephem_iodc = (U16)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawSF1Param.w_Iodc;
									p_ephemeris_assist->ephemeris_assist[counter1].ephem_toc = (U16)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawSF1Param.w_Toc;
									p_ephemeris_assist->ephemeris_assist[counter1].ephem_af2 = (S8)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawSF1Param.u_Af2;
									p_ephemeris_assist->ephemeris_assist[counter1].ephem_af1 = (S16)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawSF1Param.w_Af1;
									p_ephemeris_assist->ephemeris_assist[counter1].ephem_af0 = (S32)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawSF1Param.q_Af0;
									p_ephemeris_assist->ephemeris_assist[counter1].ephem_ura = (U8)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawSF1Param.u_Accuracy;
									p_ephemeris_assist->ephemeris_assist[counter1].ephem_w = (S32)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawEphemeris.q_Omega;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_a_power_half 	= (U32)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[counter1].z_RawEphemeris.q_SqrtA;
								p_ephemeris_assist->ephemeris_assist[counter1].ephem_predicted 		= 	(U8)0;
									p_ephemeris_assist->ephemeris_assist[counter1].ephem_predSeedAge = (U8)0;

									counter2 = 2;
							}

							if(0 == counter2) // make 'SVid = 0' if it is not available in smlc as well as GPSC DB
							{
								p_ephemeris_assist->ephemeris_assist[counter1].svid = 0xFF;
							}
					}

					break;
			  case GPSC_ASSIST_ALMANAC_REP:  /*  Almanac assistance data  */
					STATUSMSG("gpsc_app_assist_database_req: GPSC_ASSIST_ALMANAC");
					counter1=0; counter2=0;
					for (counter1=0;counter1<N_SV;counter1++)
						{
							if(p_zGPSCControl->p_zGPSCDatabase->z_DBAlmanac[counter1].u_Valid)
								{
									p_almanac_assist->almanac_assist[counter2].svid = (U8)(counter1);
									p_almanac_assist->almanac_assist[counter2].almanac_svhealth = (U8)p_zGPSCControl->p_zGPSCDatabase->z_DBAlmanac[counter1].z_RawAlmanac.u_Health;
									p_almanac_assist->almanac_assist[counter2].almanac_week = (U16)p_zGPSCControl->p_zGPSCDatabase->z_DBAlmanac[counter1].z_RawAlmanac.w_Week;
									p_almanac_assist->almanac_assist[counter2].almanac_toa = (U8)p_zGPSCControl->p_zGPSCDatabase->z_DBAlmanac[counter1].z_RawAlmanac.u_Toa;
									p_almanac_assist->almanac_assist[counter2].almanac_e = (U16)p_zGPSCControl->p_zGPSCDatabase->z_DBAlmanac[counter1].z_RawAlmanac.w_E;
									p_almanac_assist->almanac_assist[counter2].almanac_omega_dot = (S16)p_zGPSCControl->p_zGPSCDatabase->z_DBAlmanac[counter1].z_RawAlmanac.w_OmegaDot;
									p_almanac_assist->almanac_assist[counter2].almanac_omega0 = (S32)p_zGPSCControl->p_zGPSCDatabase->z_DBAlmanac[counter1].z_RawAlmanac.q_OmegaZero;
									p_almanac_assist->almanac_assist[counter2].almanac_m0 = (S32)p_zGPSCControl->p_zGPSCDatabase->z_DBAlmanac[counter1].z_RawAlmanac.q_MZero;
									p_almanac_assist->almanac_assist[counter2].almanac_af0 = (S16)p_zGPSCControl->p_zGPSCDatabase->z_DBAlmanac[counter1].z_RawAlmanac.w_Af0;
									p_almanac_assist->almanac_assist[counter2].almanac_af1 = (S16)p_zGPSCControl->p_zGPSCDatabase->z_DBAlmanac[counter1].z_RawAlmanac.w_Af1;
									p_almanac_assist->almanac_assist[counter2].almanac_ksii = (S16)p_zGPSCControl->p_zGPSCDatabase->z_DBAlmanac[counter1].z_RawAlmanac.w_DeltaI;
									p_almanac_assist->almanac_assist[counter2].almanac_a_power_half = (U32)p_zGPSCControl->p_zGPSCDatabase->z_DBAlmanac[counter1].z_RawAlmanac.q_SqrtA;
									p_almanac_assist->almanac_assist[counter2].almanac_w = (S32)p_zGPSCControl->p_zGPSCDatabase->z_DBAlmanac[counter1].z_RawAlmanac.q_Omega;
									counter2++;
								}
						}

					while(counter2<N_SV)  // make 'SVid = 0' for all invalid SVs
								{
									p_almanac_assist->almanac_assist[counter2].svid = 0xFF;
									counter2++;
								}

					break;

			  case GPSC_ASSIST_IONO_REP:  /*  Ionospheric assistance data  */
					STATUSMSG("gpsc_app_assist_database_req: GPSC_ASSIST_IONO");
					p_assistance_inject_union->iono_assist.iono_alfa0 =  (S8)p_zGPSCControl->p_zGPSCDatabase->z_DBIono.z_RawIono.u_Alpha0;
					p_assistance_inject_union->iono_assist.iono_alfa1 =  (S8)p_zGPSCControl->p_zGPSCDatabase->z_DBIono.z_RawIono.u_Alpha1;
					p_assistance_inject_union->iono_assist.iono_alfa2 =  (S8)p_zGPSCControl->p_zGPSCDatabase->z_DBIono.z_RawIono.u_Alpha2;
					p_assistance_inject_union->iono_assist.iono_alfa3 =  (S8)p_zGPSCControl->p_zGPSCDatabase->z_DBIono.z_RawIono.u_Alpha3;
					p_assistance_inject_union->iono_assist.iono_beta0 =  (S8)p_zGPSCControl->p_zGPSCDatabase->z_DBIono.z_RawIono.u_Beta0;
					p_assistance_inject_union->iono_assist.iono_beta1 =  (S8)p_zGPSCControl->p_zGPSCDatabase->z_DBIono.z_RawIono.u_Beta1;
					p_assistance_inject_union->iono_assist.iono_beta2 =  (S8)p_zGPSCControl->p_zGPSCDatabase->z_DBIono.z_RawIono.u_Beta2;
					p_assistance_inject_union->iono_assist.iono_beta3 =  (S8)p_zGPSCControl->p_zGPSCDatabase->z_DBIono.z_RawIono.u_Beta3;
					p_assistance_inject_union->iono_assist.v_iono_assist =  (U8)p_zGPSCControl->p_zGPSCDatabase->z_DBIono.v_iono_valid;
					STATUSMSG("iono_alfa0:%d,  iono_alfa1:%d,  iono_alfa2:%d,  iono_alfa3:%d",
					p_assistance_inject_union->iono_assist.iono_alfa0,
					p_assistance_inject_union->iono_assist.iono_alfa1,
					p_assistance_inject_union->iono_assist.iono_alfa2,
					p_assistance_inject_union->iono_assist.iono_alfa3);
					STATUSMSG("iono_beta0:%d,  iono_beta1:%d,  iono_beta2:%d,  iono_beta3:%d",
					p_assistance_inject_union->iono_assist.iono_beta0,
					p_assistance_inject_union->iono_assist.iono_beta1,
					p_assistance_inject_union->iono_assist.iono_beta2,
					p_assistance_inject_union->iono_assist.iono_beta3);

					STATUSMSG("v_iono_assist:%d", p_assistance_inject_union->iono_assist.v_iono_assist);

					break;
			  case GPSC_ASSIST_TIME_REP:  /*  Time of the week data  */

					if ((p_zGPSCControl->p_zGPSCState->u_SensorOpMode == C_RC_ON) || (p_zGPSCControl->p_zGPSCState->u_SensorOpMode == C_RC_OFF))
					{
			  		STATUSMSG("gpsc_app_assist_database_req: GPSC_ASSIST_TIME");
					p_assistance_inject_union->time_assist.gps_week = (U16)p_zGPSCControl->p_zGPSCDatabase->z_DBGpsClock.w_GpsWeek;
					p_assistance_inject_union->time_assist.gps_msec = (U32)p_zGPSCControl->p_zGPSCDatabase->z_DBGpsClock.q_GpsMs;
					p_assistance_inject_union->time_assist.time_unc = (U32)ceil(p_zGPSCControl->p_zGPSCDatabase->z_DBGpsClock.f_TimeUnc);

			  		  STATUSMSG("gps_week:%d,  gps_msec:%d,  time_unc:%d, u_GPSCState = %d",
					  p_assistance_inject_union->time_assist.gps_week,
					  p_assistance_inject_union->time_assist.gps_msec,
					  p_assistance_inject_union->time_assist.time_unc,
					  p_zGPSCControl->p_zGPSCState->u_SensorOpMode);

					}else if (p_zGPSCControl->p_zGPSCState->u_SensorOpMode == C_RC_IDLE)
					{
						/*If we are in IDLE state, request receiver for the current time*/
						STATUSMSG("gpsc_app_assist_database_req: GPSC_ASSIST_TIME -> Req. Device");
						p_zGPSCControl->p_zSmlcAssist->u_RequestedAssistTime = TRUE;
						gpsc_mgp_tx_req(p_zGPSCControl, AI_REQ_CLOCK_REP, 0);
						if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
						{
							STATUSMSG("gpsc_app_assist_database_req: GPSC_TX_INITIATE_FAIL - FATAL_ERROR");
							return GPSC_TX_INITIATE_FAIL;
						}
						return GPSC_TIME_PULSE_GENERATION_PENDING;

					}

					/* to be implemented later
					p_assistance_inject_union->time_assist->time_accuracy = p_zGPSCControl->p_zGPSCDatabase->z_DBGpsClock->
					p_assistance_inject_union->time_assist->sub_ms = p_zGPSCControl->p_zGPSCDatabase->z_DBGpsClock->
					*/
					break;


			  case GPSC_ASSIST_POSITION_REP:  /*  Position assistance data  */
			  		STATUSMSG("gpsc_app_assist_database_req: GPSC_ASSIST_POSITION");
					p_assistance_inject_union->position_assist.latitude = (U32)(p_zGPSCControl->p_zGPSCDatabase->z_NvsPos.l_PosLat) & 0x7FFFFFFF;
					if ((p_zGPSCControl->p_zGPSCDatabase->z_NvsPos.l_PosLat)<0)
						p_assistance_inject_union->position_assist.latitude_sign = 1;
					else
						p_assistance_inject_union->position_assist.latitude_sign = 0;
					p_assistance_inject_union->position_assist.longitude = (S32)(p_zGPSCControl->p_zGPSCDatabase->z_NvsPos.l_PosLong) & 0x7FFFFFFF;
					p_assistance_inject_union->position_assist.altitude = (U16)((p_zGPSCControl->p_zGPSCDatabase->z_NvsPos.x_PosHt) & 0x7FFF);
					if ((p_zGPSCControl->p_zGPSCDatabase->z_NvsPos.x_PosHt)<0)
						p_assistance_inject_union->position_assist.altitude_sign = 1;
					else
						p_assistance_inject_union->position_assist.altitude_sign = 0;
					p_assistance_inject_union->position_assist.position_uncertainty = (U32)p_zGPSCControl->p_zGPSCDatabase->z_NvsPos.f_PosUnc;
					p_assistance_inject_union->position_assist.d_assist_ToaSec = (DBL)p_zGPSCControl->p_zGPSCDatabase->z_NvsPos.d_ToaSec;
					break;

			  case GPSC_ASSIST_UTC_REP:  /*  UTC assistance data  */
			  		STATUSMSG("gpsc_app_assist_database_req: GPSC_ASSIST_UTC");
						p_assistance_inject_union->utc_assist.utc_valid = p_zGPSCControl->p_zGPSCDatabase->z_DBUtc.u_valid;
						p_assistance_inject_union->utc_assist.utc_a1 = (S32)p_zGPSCControl->p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_a1;
						p_assistance_inject_union->utc_assist.utc_a0 = (S32)p_zGPSCControl->p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_a0;
						p_assistance_inject_union->utc_assist.utc_tot = (U8)p_zGPSCControl->p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_tot;
						p_assistance_inject_union->utc_assist.utc_wnt = (U8) p_zGPSCControl->p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_wnt;
						p_assistance_inject_union->utc_assist.utc_delta_tls = (S8)p_zGPSCControl->p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_delta_tls;
						p_assistance_inject_union->utc_assist.utc_wnlsf = (U8)p_zGPSCControl->p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_wnlsf;
						p_assistance_inject_union->utc_assist.utc_dn = (S8)p_zGPSCControl->p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_dn;
						p_assistance_inject_union->utc_assist.utc_delta_tlsf =  (S8)p_zGPSCControl->p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_delta_tlsf;

						STATUSMSG("gpsc_app_assist_database_req: valid=%d,a1=%d,a0=%d,tot=%d,wnt=%d,tls=%d,wnlsf=%d,dn=%d,tlsf=%d",
							p_zGPSCControl->p_zGPSCDatabase->z_DBUtc.u_valid,
							p_zGPSCControl->p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_a1,
							p_zGPSCControl->p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_a0,
							p_zGPSCControl->p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_tot,
							p_zGPSCControl->p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_wnt,
							p_zGPSCControl->p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_delta_tls,
							p_zGPSCControl->p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_wnlsf,
							p_zGPSCControl->p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_dn,
							p_zGPSCControl->p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_delta_tlsf);

					break;

			  case GPSC_ASSIST_TTL_REP:  /*  TTL assistance data  */
			  		STATUSMSG("gpsc_app_assist_database_req: GPSC_ASSIST_TTL");
					if ( GPSC_SUCCESS != gpsc_app_assist_ttl_req(&p_assistance_inject_union->ttl_assist))
					{
						STATUSMSG("gpsc_app_assist_ttl_req - ERROR");
						return GPSC_FAIL;
					}
					break;


			  default:  /*  Unknown Case  */
					STATUSMSG("gpsc_app_assist_database_req: case default-Fail case");
					SAPLEAVE(gpsc_app_assist_database_req,GPSC_FAIL);
					return GPSC_FAIL;

	}
	SAPLEAVE(gpsc_app_assist_database_req,GPSC_SUCCESS);

	return GPSC_SUCCESS;
}


/*
 ******************************************************************************
 * gpsc_app_svstatus_req
 *
 * Function description:
 * GPSM shall use this function to get the SV Directions,PRN,SNR for the visible SVs and
 * also bit map indication for the SVs which has the almanac and ephemeris data from the GPSC.
 * The structure will be populated by the GPSC for the above information available during the request.
 * If there is no position estimation is available then GPSC shall not give this information.
 *
 * Parameters:
 *  T_GPSC_sv_status P: Pointer to structure to populate the sv information during the position calculation.
 *
 *
 * Return value:
 *  T_GPSC_result : GPSC_FAIL/GPSC_SUCCESS
 *
 ******************************************************************************
*/

T_GPSC_result gpsc_app_svstatus_req (T_GPSC_sv_status *sv_status)
{
		gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
		gpsc_db_sv_dir_type  *p_DBSvDirection = &p_zGPSCControl->p_zGPSCDatabase->z_DBSvDirection;
		gpsc_db_eph_type *p_DBEphemeris = &p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[0];
		gpsc_db_alm_type *p_DBAlmanac = &p_zGPSCControl->p_zGPSCDatabase->z_DBAlmanac[0];
		gpsc_db_pos_type *p_DBPos = &p_zGPSCControl->p_zGPSCDatabase->z_DBPos;
		U8 u_temp, u_SvPrn;

		SAPCALL(gpsc_app_assist_database_req);

		// Number of visible SVs (range from 1 to 32)
		sv_status->num_svs = p_DBSvDirection->u_num_of_sv;

		// SV parameters
		for (u_temp=0; u_temp<N_SV;u_temp++)
			{
				if(p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[u_temp].u_Valid)
					{
						sv_status->sv_status_param[u_temp].svprn = p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[u_temp].z_RawEphemeris.u_Prn;
						sv_status->sv_status_param[u_temp].iode = (U8)p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[u_temp].z_RawEphemeris.u_Iode;
						u_SvPrn = sv_status->sv_status_param[u_temp].svprn;
						sv_status->sv_status_param[u_temp].azimuth = p_DBSvDirection->z_RawSvDirection[u_SvPrn].u_Azim;
						sv_status->sv_status_param[u_temp].elevation = p_DBSvDirection->z_RawSvDirection[u_SvPrn].b_Elev;
					}
				else		// SV not available
					{
						sv_status->sv_status_param[u_temp].svprn = 0;
					}
			}

		// Ephimeries bitmask
		sv_status->ephemeris_bitmask = 0;
		for (u_temp=0;u_temp<N_SV;u_temp++)
			{
				if (p_DBEphemeris[u_temp].u_Valid)
					{
						sv_status->ephemeris_bitmask |= (U32) pow(2,u_temp);
					}

			}

		// Almanac bitmask
		sv_status->almanac_bitmask = 0;
		for (u_temp=0;u_temp<N_SV;u_temp++)
			{
				if (p_DBAlmanac[u_temp].u_Valid)
					{
						sv_status->almanac_bitmask |= (U32) pow(2,u_temp);
					}

			}

		// SVs used in Fix bitmap
		sv_status->sv_used_in_fix_bitmask = 0;
		for (u_temp=0;u_temp<p_DBPos->z_ReportPos.u_nSVs;u_temp++)
			{
				sv_status->sv_used_in_fix_bitmask |= (U32) pow(2,(p_DBPos->z_ReportPos.u_SVs[u_temp]-1));
			}


		SAPLEAVE(gpsc_app_assist_database_req,GPSC_SUCCESS);

}


extern T_GPSC_result gpsc_app_deinit_req (void)
{
	gpsc_ctrl_type*	p_zGPSCControl;

	SAPENTER(gpsc_app_deinit_req);

	p_zGPSCControl = gp_zGPSCControl;


	/*Save NVS data to non volatile location */
	//gpsc_db_save_to_nvs(p_zGPSCControl->p_zGPSCDatabase);

	/*close sensor files*/
	gpsc_deinit_sensor_files(p_zGPSCControl);

	/* Set global pointer to null so we can release memory */
	gp_zGPSCControl = NULL;

	/* release gpsc sm and gpsc control structure memory */
	gpsc_release_memory(p_zGPSCControl);

	return GPSC_SUCCESS;

}

/*
 ******************************************************************************
 * gpsc_populate_send_eph_data
 *
 * Function description:
 * Upon receive the ephemeris data from GPS device, gpsc shall populate and send this data
 * to requested client for example SAGPS.
 * Parameters:
 *  gpsc_db_eph_type.
 *
 *
 * Return value:
 *  Void
 *
 ******************************************************************************
*/

void gpsc_populate_send_eph_data(gpsc_db_eph_type *p_DBEphemeris)
{
	gpsc_ctrl_type*	p_zGPSCControl;
	gpsc_tasks_reg_for_assist * pRegAssist;
	T_GPSC_ephemeris_assist*   p_zEphemerisAssist = NULL;
    T_GPSC_ephemeris_assist    zEphemerisAssist;
	McpU8 i;
	p_zGPSCControl = gp_zGPSCControl;
	pRegAssist = &p_zGPSCControl->zRegAssist;
	p_zEphemerisAssist = (T_GPSC_ephemeris_assist*)&zEphemerisAssist;

	/* RawEphemeris*/
   STATUSMSG("Report ephemeris to NAVC for SV ID = %d",p_DBEphemeris->z_RawEphemeris.u_Prn);
   p_zEphemerisAssist->svid = (U8)(p_DBEphemeris->z_RawEphemeris.u_Prn-1); /* Zero based SVID*/
   p_zEphemerisAssist->ephem_a_power_half = p_DBEphemeris->z_RawEphemeris.q_SqrtA;
   p_zEphemerisAssist->ephem_cic = (S16)p_DBEphemeris->z_RawEphemeris.w_Cic;
   p_zEphemerisAssist->ephem_cis = (S16)p_DBEphemeris->z_RawEphemeris.w_Cis;
   p_zEphemerisAssist->ephem_crc =(S16) p_DBEphemeris->z_RawEphemeris.w_Crc;
   p_zEphemerisAssist->ephem_crs = (S16)p_DBEphemeris->z_RawEphemeris.w_Crs;
   p_zEphemerisAssist->ephem_cuc = (S16)p_DBEphemeris->z_RawEphemeris.w_Cuc;
   p_zEphemerisAssist->ephem_cus = (S16)p_DBEphemeris->z_RawEphemeris.w_Cus;
   p_zEphemerisAssist->ephem_delta_n = (S16)p_DBEphemeris->z_RawEphemeris.w_DeltaN;
   p_zEphemerisAssist->ephem_e = p_DBEphemeris->z_RawEphemeris.q_E;
   p_zEphemerisAssist->ephem_i0 = (S32)p_DBEphemeris->z_RawEphemeris.q_IZero;
   {
		/* w_IDot appears to be a 14-bit two's-complement number.
	       We have to extend it's sign before casting to S16. */
	   U16 temp = p_DBEphemeris->z_RawEphemeris.w_IDot;
	   if (temp & 0x2000)
	   {
		   /* negative */
		   temp |= 0xc000;
	   }
	   else
	   {
		   /* positive */
		   temp &= 0x3fff;
	   }
	   p_zEphemerisAssist->ephem_idot = (S16)temp;
   }
   p_zEphemerisAssist->ephem_m0 = (S32)p_DBEphemeris->z_RawEphemeris.q_MZero;
   p_zEphemerisAssist->ephem_omega_a0 = (S32)p_DBEphemeris->z_RawEphemeris.q_OmegaZero;
   {
		/* q_OmegaDot appears to be a 24-bit two's-complement number.
		   We have to extend it's sign before casting to S32. */
	   U32 temp = p_DBEphemeris->z_RawEphemeris.q_OmegaDot;
	   if (temp & 0x00800000)
	   {
		   /* negative */
		   temp |= 0xff000000;
	   }
	   else
	   {
		   /* positive */
		   temp &= 0x00ffffff;
	   }
	   p_zEphemerisAssist->ephem_omega_adot = (S32)temp;
   }
   p_zEphemerisAssist->ephem_toe = p_DBEphemeris->z_RawEphemeris.w_Toe;
   p_zEphemerisAssist->ephem_w = (S32)p_DBEphemeris->z_RawEphemeris.q_Omega;
   p_zEphemerisAssist->iode = p_DBEphemeris->z_RawEphemeris.u_Iode ;


   /* RawSF1params */
   p_zEphemerisAssist->ephem_code_on_l2 = p_DBEphemeris->z_RawSF1Param.u_CodeL2;
   p_zEphemerisAssist->ephem_af0 = (S32)p_DBEphemeris->z_RawSF1Param.q_Af0;
   p_zEphemerisAssist->ephem_af1 = (S16)p_DBEphemeris->z_RawSF1Param.w_Af1;
   p_zEphemerisAssist->ephem_af2 = (S8)p_DBEphemeris->z_RawSF1Param.u_Af2;
   p_zEphemerisAssist->ephem_iodc = p_DBEphemeris->z_RawSF1Param.w_Iodc;
   p_zEphemerisAssist->ephem_svhealth = p_DBEphemeris->z_RawSF1Param.u_Health;
   p_zEphemerisAssist->ephem_tgd = (S8)p_DBEphemeris->z_RawSF1Param.u_Tgd;
   p_zEphemerisAssist->ephem_toc = p_DBEphemeris->z_RawSF1Param.w_Toc;
   p_zEphemerisAssist->ephem_ura = p_DBEphemeris->z_RawSF1Param.u_Accuracy;

  /* Week */
  p_zEphemerisAssist->ephem_week = p_DBEphemeris->w_EphWeek;

   /* call the SAP to indicate the ephemeris data to NAVC */
   SAPCALL(gpsc_app_assist_eph_ind);

   for (i=0;i<pRegAssist->EPHcount;i++)
   {
   	gpsc_app_assist_eph_ind(p_zEphemerisAssist,
							pRegAssist->zEphList[i].taskID,
							pRegAssist->zEphList[i].queueID);
   }

}

/*
 ******************************************************************************
 * gpsc_populate_send_gpstime_data
 *
 * Function description:
 * Upon receive the GPS Time from GPS device, gpsc shall populate and send this data
 * to requested client for example SAGPS.
 *
 * Parameters:
 *  gpsc_db_gps_clk_type.
 *
 *
 * Return value:
 *  Void
 *
 ******************************************************************************
*/

void gpsc_populate_send_gpstime_data(gpsc_db_gps_clk_type*  p_DBGpsClock)
{
	gpsc_ctrl_type					*p_zGPSCControl;
	gpsc_tasks_reg_for_assist 		*pRegAssist;
	T_GPSC_time_assist 			*pAssistTime = NULL;
	T_GPSC_time_assist 			tAssistTime;
	McpU8 i;

	p_zGPSCControl = gp_zGPSCControl;
	pRegAssist = &p_zGPSCControl->zRegAssist;
	pAssistTime = &tAssistTime;

	STATUSMSG("gpsc_populate_send_gpstime_data");

	pAssistTime->gps_week = p_DBGpsClock->w_GpsWeek;
	pAssistTime->gps_msec = p_DBGpsClock->q_GpsMs;
	pAssistTime->time_unc = (McpU32)p_DBGpsClock->f_TimeUnc;
	pAssistTime->sub_ms = 0;
	pAssistTime->time_accuracy = 0;

	STATUSMSG("gpsc_populate_send_gpstime_data: Week:%d, Msec:%d",
				pAssistTime->gps_week,
				pAssistTime->gps_msec);


	for (i=0;i<pRegAssist->GpsTimeCount;i++)
	{
		gpsc_app_assist_gpstime_ind(pAssistTime,
								pRegAssist->zGpsTimeList[i].taskID,
								pRegAssist->zGpsTimeList[i].queueID);
	}

}
/*
 ******************************************************************************
 * gpsc_app_gps_sleep
 *
 * Function description:
 * Put the receiver in Sleep mode, only if the receiver is in idle mode.
 *
 * Parameters:
 *
 *
 * Return value:
 *  T_GPSC_result : GPSC_FAIL/GPSC_SUCCESS
 *
 ******************************************************************************
*/

T_GPSC_result gpsc_app_gps_sleep ()
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	gpsc_state_type*	p_zGPSCState = p_zGPSCControl->p_zGPSCState;
	gpsc_cfg_type* p_zGPSCConfig = p_zGPSCControl->p_zGPSCConfig;


	SAPCALL(gpsc_app_gps_sleep);
	SAPENTER(gpsc_app_gps_sleep);

	/* do not handle this command when config file is set for sleep */
	if (p_zGPSCConfig->low_power_state == LOW_POWER_MODE_SLEEP)
		SAPLEAVE(gpsc_app_gps_sleep,GPSC_SUCCESS);

	if(p_zGPSCState->u_SensorOpMode == C_RC_IDLE)
		{
		STATUSMSG("gpsc_app_gps_sleep: Receiver if ON/IDLE, begin sleep");

			p_zGPSCControl->p_zCustomStruct->custom_sleep_req_flag= TRUE;
			p_zGPSCControl->p_zCustomStruct->wakeup = FALSE;

			p_zGPSCControl->p_zGpscSm->e_GpscCurrState = E_GPSC_STATE_IDLE;
			GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_IDLE);

		}
	else if (p_zGPSCState->u_SensorOpMode == C_RC_ON )
	{
		STATUSMSG("gpsc_app_gps_sleep: Receiver is ON, ignore sleep request");
	}
	else
	{
		STATUSMSG("gpsc_app_gps_sleep: Receiver is OFF, ignore sleep request");
	}

	SAPLEAVE(gpsc_app_gps_sleep,GPSC_SUCCESS);
}

/*
 ******************************************************************************
 * gpsc_app_gps_wakeup
 *
 * Function description:
 * wake up the receiver from sleep mode
 *
 * Parameters:
 *
 *
 * Return value:
 *  T_GPSC_result : GPSC_FAIL/GPSC_SUCCESS
 *
 ******************************************************************************
*/

T_GPSC_result gpsc_app_gps_wakeup ()
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	gpsc_state_type*	p_zGPSCState = p_zGPSCControl->p_zGPSCState;
	gpsc_cfg_type* p_zGPSCConfig = p_zGPSCControl->p_zGPSCConfig;

	SAPCALL(gpsc_app_gps_wakeup);
	SAPENTER(gpsc_app_gps_wakeup);

	/* do not handle this command when config file is set for sleep */
	if (p_zGPSCConfig->low_power_state == LOW_POWER_MODE_SLEEP)
		SAPLEAVE(gpsc_app_gps_sleep,GPSC_SUCCESS);

	if( p_zGPSCState->u_SensorOpMode == C_RC_OFF)
		{
		STATUSMSG("gpsc_app_gps_wakeup: Receiver if OFF, begin wakeup");

			p_zGPSCControl->p_zCustomStruct->custom_wakeup_req_flag = TRUE;
			p_zGPSCControl->p_zCustomStruct->wakeup = FALSE;

			p_zGPSCControl->p_zGpscSm->e_GpscCurrState = E_GPSC_STATE_IDLE;
			GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_GPS_WAKEUP);
	}
	else
	{
		STATUSMSG("gpsc_app_gps_wakeup: Receiver is not OFF, ignore wakeup request");
	}

	SAPLEAVE(gpsc_app_gps_wakeup,GPSC_SUCCESS);

}


/*
 ******************************************************************************
 * gpsc_app_gps_wakeup_from_cmdhandler
 *
 * Function description:
 * wake up the receiver from sleep mode
 *
 * Parameters:
 *
 *
 * Return value:
 *  T_GPSC_result : GPSC_FAIL/GPSC_SUCCESS
 *
 ******************************************************************************
*/

T_GPSC_result gpsc_app_gps_wakeup_from_cmdhandler ()
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	gpsc_cfg_type* p_zGPSCConfig = p_zGPSCControl->p_zGPSCConfig;
	SAPCALL(gpsc_app_gps_wakeup_from_cmdhandler);
	SAPENTER(gpsc_app_gps_wakeup_from_cmdhandler);

	/* do not handle this command when config file is set for sleep */
	if (p_zGPSCConfig->low_power_state != LOW_POWER_MODE_SLEEP)
	{
		STATUSMSG("gpsc_app_gps_wakeup_from_cmdhandler: LP state not set to sleep");
		SAPLEAVE(gpsc_app_gps_wakeup_from_cmdhandler,GPSC_SUCCESS);
	}

    if(p_zGPSCControl->p_zGPSCState->u_SensorOpMode == C_RC_OFF)
	{
		gpsc_mgp_tx_rc_act (p_zGPSCControl, C_RECEIVER_IDLE, 0);
		gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK);
		STATUSMSG("[%s] gpsc_app_gps_wakeup_from_cmdhandler - Injected Idle Mode, Going to Sleep",get_utc_time());
		NavcCmdDisableQueue(p_zGPSCControl->p_zSysHandlers->hNavcCtrl);
		//wait until receiver is out of sleep
			MCP_HAL_OS_Sleep(980);
		STATUSMSG("[%s] gpsc_app_gps_wakeup_from_cmdhandler - Sleep Over, Send Response",get_utc_time());
	}
	else
	{
		STATUSMSG("gpsc_app_gps_wakeup_from_cmdhandler: Receiver is not OFF, ignore wakeup request");
	}

	SAPLEAVE(gpsc_app_gps_wakeup_from_cmdhandler,GPSC_SUCCESS);

}

/*
 ******************************************************************************
 * gpsc_app_inj_tcxo_req
 *
 * Function description:
 * inject tcxo paramters.
 *
 * Parameters:
 *
 *
 * Return value:
 *  T_GPSC_result : GPSC_FAIL/GPSC_SUCCESS
 *
 ******************************************************************************
*/
T_GPSC_result gpsc_app_inj_tcxo_req (gpsc_inject_freq_est_type*  p_zInjectFreqEst)
{
	 gpsc_sys_handlers*	p_GPSCSysHandlers;
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	gpsc_db_gps_inj_clk_type* p_DBGpsInjClock =&p_zGPSCControl->p_zGPSCDatabase->z_DBGpsInjClock;
	gpsc_custom_struct* p_zCustom = p_zGPSCControl->p_zCustomStruct;
	McpS32	fd;
  	//EMcpfRes	eRetVal; /* Klocwork Changes */
	//klocwork
  	McpU8 uAidingFile[C_MAX_AIDING_FILENAME_LEN+20];

	SAPCALL(gpsc_app_inj_tcxo_req);
	SAPENTER(gpsc_app_inj_tcxo_req);

	//Store to DB
  	p_DBGpsInjClock->l_FreqBiasRaw = p_zInjectFreqEst->l_FreqBiasRaw;
  	p_DBGpsInjClock->q_FreqUncRaw = p_zInjectFreqEst->q_FreqUncRaw;
	p_DBGpsInjClock->u_Valid= TRUE; /* set to valid */
	p_zCustom->custom_assist_availability_flag |= C_TCXO_AVAIL;


	//Store to NVM
  	p_GPSCSysHandlers = p_zGPSCControl->p_zSysHandlers;

	MCP_HAL_STRING_StrCpy (uAidingFile,(McpU8*)p_GPSCSysHandlers->uAidingPath);
	MCP_HAL_STRING_StrCat (uAidingFile,C_STR_AID_TCXO_FILE);

	if (mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *) uAidingFile,
					MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_WRONLY | MCP_HAL_FS_O_CREATE, &fd) == RES_OK)
	{
		mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, fd,
							(void *)p_DBGpsInjClock, sizeof(gpsc_db_gps_inj_clk_type));
		mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
	}
	else
	{
			STATUSMSG("gpsc_app_inj_tcxo_req: Failed to open file - %s",  uAidingFile);
	}


	//Inject
	gpsc_mgp_inject_freq_est(p_zGPSCControl, p_zInjectFreqEst);
	if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
				SAPLEAVE(gpsc_app_inj_tcxo_req, GPSC_TX_INITIATE_FAIL);

	gpsc_app_inj_tcxo_ind();

	SAPLEAVE(gpsc_app_inj_tcxo_req,GPSC_SUCCESS);


}

/*
 ******************************************************************************
 * gpsc_app_assist_ttl_req
 *
 * Function description:
 * GPSM shall use this function to get the TTL values of each of the assistance data available in the database
 * The assistance data time is checked against the current GPS time and if the value is within the specified limit
 * then the difference of current GPS time and assistance time is filled in the parameters.
 *
 * Parameters:
 *  T_GPSC_ttl_assist_rep P: Pointer to structure to populate the TTL information.
 *
 *
 * Return value:
 *  T_GPSC_result : GPSC_FAIL/GPSC_SUCCESS
 *
 ******************************************************************************
*/
static T_GPSC_result gpsc_app_assist_ttl_req (T_GPSC_ttl_assist_rep *p_ttl_assist)
{
		gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
		gpsc_db_type* p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
		gpsc_db_pos_type *p_DBPos = &p_zGPSCControl->p_zGPSCDatabase->z_DBPos;


		U8 u_temp;
		U32 q_curGPSTimeSecs =0;
		U32 q_tempTime =0;

	    FLT f_EastUnc, f_NorthUnc, f_PosUnc;
	    DBL d_PosAgeSec;


		SAPCALL(gpsc_app_assist_ttl_req);

		q_curGPSTimeSecs = gpsc_db_gps_time_in_secs(p_zGPSCControl);

		/* Klocwork Changes */
		mcpf_mem_set(p_zGPSCControl->p_zSysHandlers->hMcpf, &p_ttl_assist->iono_ttl, 0, sizeof(U32));

		p_ttl_assist->refTime_ttl = 0xFFFFFFFF;
		if ( q_curGPSTimeSecs != 0)
		{
			/* TTL - IONO - 2 Weeks */

			if(p_zGPSCDatabase->z_DBIono.d_UpdateTimeSec <= 0)
				p_ttl_assist->iono_ttl = 0;
			else
			{
				q_tempTime = (U32)(q_curGPSTimeSecs - p_zGPSCDatabase->z_DBIono.d_UpdateTimeSec);
				p_ttl_assist->iono_ttl = (q_tempTime < (WEEK_SECS * 2))? q_tempTime : 0;
			}

			/* TTL - refTime - TimeUnc is < 3sec */
			p_ttl_assist->refTime_ttl = (U32)(((U32)p_zGPSCDatabase->z_DBGpsClock.f_TimeUnc) < 3000.0)? (U32)((3000.0 - (p_zGPSCDatabase->z_DBGpsClock.f_TimeUnc))*200) : (U32)0xFFFFFFFF; // 5 PPM = 1/(5e-6) = 200000

			/* TTL - refPosition - ((150 km - posUnc km) * 1000/25)*/

		    if ( p_DBPos->d_ToaSec!= 0 )
			{
				d_PosAgeSec = q_curGPSTimeSecs - p_DBPos->d_ToaSec;

			    f_EastUnc = (FLT)p_DBPos->z_ReportPos.w_EastUnc * (FLT)C_LSB_EER;
				f_NorthUnc = (FLT)p_DBPos->z_ReportPos.w_NorthUnc * (FLT)C_LSB_NER;

				f_PosUnc = (FLT)ext_sqrt( (f_EastUnc * f_EastUnc) + (f_NorthUnc * f_NorthUnc) );

				/* increament f_Unc based on the age of the fix */
				f_PosUnc += (FLT)d_PosAgeSec * (FLT)C_POS_UNC_GROWTH;

				if (f_PosUnc < (FLT)C_GOOD_INIT_POS_UNC )
				{
					p_ttl_assist->refPosition_ttl = (U32)((FLT)C_GGOD_INIT_POS_UNC_EPH - f_PosUnc)/(C_POS_UNC_GROWTH); //15 Km/ (20m/s)
					//STATUSMSG("Status: Chk pos: OK");
				}
				else
				{
					p_ttl_assist->refPosition_ttl = 0;
				}
			} /*if (p_DBPos->d_ToaSec!= 0)*/


			/* TTL - UTC - 1 Yr - 54 Weeks */

			if (p_zGPSCControl->p_zGPSCDatabase->z_DBUtc.u_valid)
			{
				q_tempTime = (U32) (q_curGPSTimeSecs - p_zGPSCControl->p_zGPSCDatabase->z_DBUtc.d_UpdateTimeSec);
				p_ttl_assist->utc_ttl = (q_tempTime < (54U*WEEK_SECS))? q_tempTime: 0;
			}


			for (u_temp=0; u_temp<32;u_temp++)
			{
				/* TTL - Ephemeris - < 4hrs */
				if(p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[u_temp].u_Valid)
				{
						/* time w.r.t. Toe can be in 2 hrs ahead or 2 Hrs back
							Eph is considered valid for q_curGPSTimeSecs +- 2Hrs
							If Toe > q_curGPSTimeSecs then that ephemeris is valid for 2hrs + (Toe - q_curGPSTimeSecs)
						*/
						q_tempTime = abs(q_curGPSTimeSecs - ((U32)(p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[u_temp].z_RawSF1Param.d_FullToe)));
						if (q_tempTime < (2U*3600U))
						{
							if (((U32)(p_zGPSCControl->p_zGPSCDatabase->z_DBEphemeris[u_temp].z_RawSF1Param.d_FullToe)) > q_curGPSTimeSecs)
							{
								p_ttl_assist->eph_ttl[u_temp] = (2 * 3600) +
										((U32)q_tempTime);
							}
							else
							{
								p_ttl_assist->eph_ttl[u_temp] = (2 * 3600) - (U32)q_tempTime;
							}
						}
						else
							p_ttl_assist->eph_ttl[u_temp] = 0;
				}
				else
				{
						p_ttl_assist->eph_ttl[u_temp] = 0;
				}

				/* TTL - Almanac - 6 months - 27Weeks*/
				if(p_zGPSCControl->p_zGPSCDatabase->z_DBAlmanac[u_temp].u_Valid)
				{
					q_tempTime = abs(q_curGPSTimeSecs - (U32)p_zGPSCControl->p_zGPSCDatabase->z_DBAlmanac[u_temp].z_RawAlmanac.d_FullToa);
						p_ttl_assist->alm_ttl[u_temp] = (q_tempTime < (27U*WEEK_SECS))? q_tempTime : 0;
				}
				else
				{
						p_ttl_assist->alm_ttl[u_temp] = 0;
				}

				/* TTL - Acquisition Assistance - TBD Not in DB */

				/* TTL - RTI - TBD Not in DB */

			} /*for (u_temp ...)*/


		} /* if (q_curGPSTimeSecs != 0)*/

		return GPSC_SUCCESS;

		SAPLEAVE(gpsc_app_assist_ttl_req,GPSC_SUCCESS);

}

/*
 ******************************************************************************
 * gpsc_app_pos_rep_ext_req
 *
 * Function description:
 *
 * This function is the external interface that the application uses to instruct
 * the GPSC to request for one Position Rep Ext from the sensor.
 *
 * Parameters:
 *  None
 *
 *
 * Return value:
 *  T_GPSC_result : Result
 ******************************************************************************
*/
T_GPSC_result gpsc_app_pos_rep_ext_req (void)
{

	gpsc_ctrl_type*	p_zGPSCControl;
	gpsc_state_type*	p_zGPSCState;
	gpsc_custom_struct*	p_zGPSCCustom;
	p_zGPSCControl = gp_zGPSCControl;
	p_zGPSCState  = p_zGPSCControl->p_zGPSCState;
	p_zGPSCCustom = p_zGPSCControl->p_zCustomStruct;
	SAPENTER(gpsc_app_pos_rep_ext_req);

	STATUSMSG(" Request for Position Rep Ext in state = %d",p_zGPSCState->u_GPSCState);

	p_zGPSCCustom->custom_pos_rep_ext_req_flag = TRUE;

	AI2TX("Request Position",AI_CLAPP_REQ_POS_EXT);
	gpsc_mgp_tx_req( p_zGPSCControl, AI_CLAPP_REQ_POS_EXT, 0 );

	if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
				SAPLEAVE(gpsc_app_pos_rep_ext_req, GPSC_TX_INITIATE_FAIL);

	SAPLEAVE(gpsc_app_pos_rep_ext_req,GPSC_SUCCESS);
}
/*
 ******************************************************************************
 * gpsc_app_enter_fwd_req
 *
 * Function description:
 *
 * This function is the external interface that the application uses to instruct
 * the GPSC to enter fowarding mode
 *
 * Parameters:
 *  None
 *
 *
 * Return value:
 *  T_GPSC_result : Result
 ******************************************************************************
*/
T_GPSC_result gpsc_app_enter_fwd_req (void)
{

	gpsc_ctrl_type*	p_zGPSCControl;
	p_zGPSCControl = gp_zGPSCControl;
	SAPENTER(gpsc_app_enter_fwd_req);

 	if((E_GPSC_STATE_IDLE == p_zGPSCControl->p_zGpscSm->e_GpscCurrState) || (E_GPSC_STATE_SLEEP == p_zGPSCControl->p_zGpscSm->e_GpscCurrState))
  	{
  	   GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_ENTER_FWD);
  	}
	else
	{
		STATUSMSG("Error: Request for Fwd state in = %d",p_zGPSCControl->p_zGpscSm->e_GpscCurrState);
		SAPLEAVE(gpsc_app_enter_fwd_req,GPSC_FAIL);
	}

	SAPLEAVE(gpsc_app_enter_fwd_req,GPSC_SUCCESS);
}

/*
 ******************************************************************************
 * gpsc_app_sw_reset
 *
 * Function description:
 *
 * This function is the external interface that the application uses to instruct
 * the GPSC to reset the chip in sw manner....
 *
 * Parameters:
 *  None
 *
 *
 * Return value:
 *  T_GPSC_result : Result
 ******************************************************************************
*/
U8 resetFlag=0;
extern T_GPSC_result gpsc_app_sw_reset(void)
{
   gpsc_ctrl_type *p_zGPSCControl;
   gpsc_sess_cfg_type *p_zSessConfig;

   p_zGPSCControl = gp_zGPSCControl;
   p_zSessConfig = gp_zGPSCControl->p_zSessConfig;

   SAPENTER(gpsc_app_sw_reset);
   resetFlag =1;

   // now close all the sessions
  // gpsc_session_del_node(p_zSessConfig,0xFFFFFFFF);

    /* Turn off all async/periodic events */
//	gpsc_mgp_cfg_evt_rep_init ( p_zGPSCControl );

  // session end to clear all the session related params
   gpsc_sess_end(p_zGPSCControl);

   /* Turn off all async/periodic events */
	//gpsc_mgp_cfg_evt_rep_init ( p_zGPSCControl );

#ifdef LARGE_UPDATE_RATE
	/* q_SleepDuration = 0 - if in case session sleep when Rp > 30 seconds */
	p_zSessConfig->q_SleepDuration = 0;
#endif
	p_zSessConfig->u_ReConfigFlag = FALSE;
	p_zSessConfig->w_TotalNumSession = 0;

   //now send the reset command to the chip
   gpsc_mgp_tx_rc_act(p_zGPSCControl,C_RECEIVER_RESET,NULL);
   if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
   {
     FATAL_INIT(GPSC_TX_INITIATE_FAIL);
   }

   SAPLEAVE(gpsc_app_sw_reset,GPSC_SUCCESS);

}

/*
 ******************************************************************************
 * gpsc_app_exit_fwd_req
 *
 * Function description:
 *
 * This function is the external interface that the application uses to instruct
 * the GPSC to exit fowarding mode and enter ready state
 *
 * Parameters:
 *  None
 *
 *
 * Return value:
 *  T_GPSC_result : Result
 ******************************************************************************
*/
T_GPSC_result gpsc_app_exit_fwd_req (void)
{
	gpsc_ctrl_type*	p_zGPSCControl;
	p_zGPSCControl = gp_zGPSCControl;
	SAPENTER(gpsc_app_exit_fwd_req);


       STATUSMSG("GPSC State:%s, Event:%s \n",GpscSmState(p_zGPSCControl->p_zGpscSm->e_GpscCurrState), GpscSmEvent(E_GPSC_EVENT_EXIT_FWD));


 	if(E_GPSC_STATE_FOWARD == p_zGPSCControl->p_zGpscSm->e_GpscCurrState)
  	{
  	   GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_EXIT_FWD);
  	}
	else
	{
		STATUSMSG("Error: Request for exit of Fwd state in = %d",p_zGPSCControl->p_zGpscSm->e_GpscCurrState);
		SAPLEAVE(gpsc_app_exit_fwd_req,GPSC_FAIL);
	}

	SAPLEAVE(gpsc_app_exit_fwd_req,GPSC_SUCCESS);
}

/*
 ******************************************************************************
 * gpsc_app_fwd_tx_req
 *
 * Function description:
 *
 * This function is used to send raw data to MCPF
 *
 * Parameters:
 *  p_uDataBuffer -> Pointer to data buffer
 *  w_NumBytes   -> Number of bytes
 *
 * Return value:
 *  T_GPSC_result : Result
 ******************************************************************************
*/
T_GPSC_result gpsc_app_fwd_tx_req( U8 *p_uDataBuffer, U16 w_NumBytes)
{
        gpsc_ctrl_type* p_zGPSCControl;
        p_zGPSCControl = gp_zGPSCControl;

	SAPENTER(gpsc_app_fwd_tx_req);
 	if(E_GPSC_STATE_FOWARD == p_zGPSCControl->p_zGpscSm->e_GpscCurrState)
         {
	  if (gpsc_transmit_data(p_uDataBuffer, w_NumBytes) != GPSC_SUCCESS)
		{
			SAPLEAVE(gpsc_app_fwd_tx_req,GPSC_FAIL);
		}
         }
        else
         {
	  SAPLEAVE(gpsc_app_fwd_tx_req,GPSC_FAIL);
         }
	SAPLEAVE(gpsc_app_fwd_tx_req,GPSC_SUCCESS);
}

/*
 ******************************************************************************
 * gpsc_app_fwd_rx_res
 *
 * Function description:
 *
 * This function is used to indicate received data from MCPF.
 *
 * Parameters:
 *  p_uDataBuffer -> Pointer to data buffer
 *  w_NumBytes   -> Number of bytes
 *  NOTE: Data must be copied before returning
 *
 * Return value:
 *  T_GPSC_result : Result
 ******************************************************************************
*/
void gpsc_app_fwd_rx_res( U8 *p_uDataBuffer, U16 w_NumBytes)
{
        gpsc_ctrl_type* p_zGPSCControl;
        p_zGPSCControl = gp_zGPSCControl;

        SAPENTER(gpsc_app_fwd_rx_res);
        STATUSMSG("Info: received %d bytes",w_NumBytes);

      // pass the data to the requesting client through NAVC
      if(E_GPSC_STATE_FOWARD == p_zGPSCControl->p_zGpscSm->e_GpscCurrState)
        {
         gpsc_app_fwd_rx_ind(p_uDataBuffer, w_NumBytes);
        }
	  else
	  {
			STATUSMSG("ERROR: gpsc_app_fwd_rx_res called and GPSC State not in E_GPSC_STATE_FOWARD");
	  }
       SAPLEAVE(gpsc_app_fwd_rx_res,GPSC_SUCCESS);
}


/*
 ******************************************************************************
 * gpsc_app_get_agc
 *
 * Function description:
 *
 * This function to initiate an AGC request at with the receiver
 *
 * Parameters:
 * none
 * Return value:
 *  T_GPSC_result : Result
 ******************************************************************************
*/
T_GPSC_result gpsc_app_get_agc( )
{
	gpsc_ctrl_type 	*p_zGPSCControl	= gp_zGPSCControl;
	gpsc_state_type	*p_zGPSCState 	= p_zGPSCControl->p_zGPSCState;
	gpsc_custom_struct	*p_zCustom		= p_zGPSCControl->p_zCustomStruct;

	SAPENTER(gpsc_app_get_agc);

	if(p_zGPSCState->u_SensorOpMode == C_RC_ON)
	{
		/* sensor is on, request for AGC parameters */
		gpsc_mgp_tx_read_reg(p_zGPSCControl, C_RF_AGC_REG, REG_16BIT);

		if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
		{
			STATUSMSG("gpsc_app_get_agc: GPSC_TX_INITIATE_FAIL - FATAL_ERROR");
			return GPSC_TX_INITIATE_FAIL;
		}

		p_zCustom->custom_agc_req_flag = TRUE;
		p_zGPSCState->q_RegReadAddress = C_RF_AGC_REG;
	}
	else
	{
		/* sensor is in Idle/Off, reject the request */
		SAPLEAVE(gpsc_app_get_agc,GPSC_FAIL);
	}

	SAPLEAVE(gpsc_app_get_agc,GPSC_SUCCESS);
}

/*
 ******************************************************************************
 * gpsc_app_get_agc
 *
 * Function description:
 *
 * This function to initiate an AGC request at with the receiver
 *
 * Parameters:
 * none
 * Return value:
 *  T_GPSC_result : Result
 ******************************************************************************
*/
void gpsc_app_process_agc_res(gpsc_ctrl_type* p_zGPSCControl, U16 q_regValue)
{
	S32  RSSI_at_VGA_Input_dBm = 0;
	S32	RSSI_at_LNA_Input_dBm = 0;
	S32	sRegValue = 0;

   UNREFERENCED_PARAMETER(p_zGPSCControl);

	if (q_regValue > 4096)
		sRegValue = q_regValue - 8192;
	else
		sRegValue = q_regValue;

	RSSI_at_VGA_Input_dBm = (10*log10(pow(2,(sRegValue/64))*pow(0.2,2))) + 13;
	RSSI_at_LNA_Input_dBm = RSSI_at_VGA_Input_dBm - REFE_GAIN;

	gpsc_app_agc_ind(RSSI_at_VGA_Input_dBm, RSSI_at_LNA_Input_dBm);
}

/*
 ******************************************************************************
 * gpsc_store_msa_pos_in_db
 *
 * Function description:
 *
 * This function to store the position in database in the msa mode
 *
 * Parameters: TNAVC_posIndData - Pos Indication
 * Return value:
 *  none
 ******************************************************************************
*/
void gpsc_store_msa_pos_in_db(TNAVC_posIndData* posInd)
{

	gpsc_ctrl_type*	p_zGPSCControl=gp_zGPSCControl;
	gpsc_db_type          *p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	//gpsc_db_gps_clk_type *p_DBGpsClock = &p_zGPSCDatabase->z_DBGpsClock; /* Klocwork Changes */

	gpsc_report_pos_type *p_ReportPos = &p_zGPSCDatabase->z_DBPos.z_ReportPos;
  	gpsc_db_pos_type *p_DBPos = &p_zGPSCDatabase->z_DBPos;
  	//me_Time *p_meTime = &p_DBPos->z_PosTime; /* Klocwork Changes */
	DBL d_Lat_Double;
	DBL d_Long_Double;
	p_ReportPos->w_PositionFlags = 0x00;

	if(posInd->position.latitude_sign)
	{
		p_ReportPos->l_Lat = (S32)(0 - posInd->position.latitude* C_90_OVER_2_23 *
												C_DEG_TO_RAD * C_LSB_LAT_REP);
	}
	else
	{
		p_ReportPos->l_Lat = (S32)(posInd->position.latitude* C_90_OVER_2_23 *
												C_DEG_TO_RAD * C_LSB_LAT_REP);
	}

	p_ReportPos->l_Long = (S32)(posInd->position.longtitude * C_360_OVER_2_24
													*C_DEG_TO_RAD * C_LSB_LON_REP);

	if(posInd->position.altitudeDirection)
		p_ReportPos->x_Height  = (S16)(-1* posInd->position.altitude *C_LSB_HT_REP);
	else
		p_ReportPos->x_Height  = (S16)(posInd->position.altitude*C_LSB_HT_REP);


	p_ReportPos->w_PositionFlags |= 0x02;


	// to be done - conversion of unc to DB format
	FLT locUncPerAxis;
	FLT locUncOnAxis;
	locUncPerAxis=(((pow(1.1,posInd->position.uncertaintySemiMinor))-1)*10);
	if(locUncPerAxis>6553.5f)
		p_ReportPos->w_LocUncPerAxis=65535;
	else
		p_ReportPos->w_LocUncPerAxis=(U16)(locUncPerAxis*10);

	locUncOnAxis=(((pow(1.1,posInd->position.uncertaintySemiMajor))-1)*10);
	if(locUncOnAxis>6553.5f)
		p_ReportPos->w_LocUncOnAxis=65535;
	else
		p_ReportPos->w_LocUncOnAxis=(U16)(locUncOnAxis*10);

	p_ReportPos->w_LocAngleUnc=(U16)(((posInd->position.orientationMajorAxis)*C_LSB_HT_REP)*C_DEG_TO_RAD);
	p_ReportPos->w_VerticalUnc=(U16)(((pow(1.025,posInd->position.altUncertainty))-1)*45);

	//temporary - complete utctimestamp decoding is required.
	//memset(posInd->position.UTCTimeStamp,0x00,20);
	//utctimestampbytes=posInd->position.UTCTimeStampNumByte;
	//memcpy(utctimestamp,posInd->position.UTCTimeStamp,utctimestampbytes);

    //p_DBPos->d_ToaSec = gpsc_db_gps_time_in_secs(p_zGPSCControl);
    //zzz
	p_ReportPos->u_timeofFix=gpsc_get_get_gps_secs_from_utctimestamp(posInd->position.UTCTimeStamp,posInd->position.UTCTimeStampNumByte);

	STATUSMSG("##############DB POS##############");
	STATUSMSG("GPS Time of the Fix  : %f",p_DBPos->d_ToaSec);
	STATUSMSG("Latitude: %ld",p_ReportPos->l_Lat);
	STATUSMSG("Longitude: %ld",p_ReportPos->l_Long);
	STATUSMSG("Altitude: %ld",p_ReportPos->x_Height);
	STATUSMSG("w_LocUncPerAxis : %d",p_ReportPos->w_LocUncPerAxis);
	STATUSMSG("w_LocUncOnAxis : %d",p_ReportPos->w_LocUncOnAxis);
	STATUSMSG("utctimestampbytes : %d",posInd->position.UTCTimeStampNumByte);
	STATUSMSG("utctimestamp : %c%c%c%c%c%c",posInd->position.UTCTimeStamp[7],posInd->position.UTCTimeStamp[8],posInd->position.UTCTimeStamp[9],posInd->position.UTCTimeStamp[10],posInd->position.UTCTimeStamp[11],posInd->position.UTCTimeStamp[12]);
	STATUSMSG("#####################################");
	d_Lat_Double = ((DBL)(p_ReportPos->l_Lat) )*(DBL)C_LSB_LAT * (DBL)C_RAD_TO_DEG;
	d_Long_Double = ((DBL)(p_ReportPos->l_Long))*(DBL)C_LSB_LON * (DBL)C_RAD_TO_DEG ;
	U32 timetofix=mcpf_getCurrentTime_InMilliSec(p_zGPSCControl->p_zSysHandlers->hMcpf)-p_zGPSCControl->p_zGPSCState->u_GpscLocFixStartSystemTime;
	SESSLOGMSG("[%s]%s:%d, %f,%f,%d,%d,%d,%d,%d %s %d ms\n",get_utc_time(),SESS_POS_RESULT,nav_sess_getCount(0),d_Lat_Double,d_Long_Double,p_ReportPos->x_Height,p_ReportPos->w_LocAngleUnc,p_ReportPos->w_VerticalUnc,p_ReportPos->w_LocUncOnAxis,p_ReportPos->w_LocUncPerAxis,"#Position (Lat, Lang, Alt,orientation,height_unc,unc_maj,unc_min) time to fix : ",timetofix);

}

#ifdef WIFI_ENABLE  //below functions are used only if wifi enabled
/*
 ******************************************************************************
 * gpsc_app_wifi_pos_update
 *
 * Function description:
 *
 * This function update the wifi position from WPC Module to local database of NAVC
 *
 * Parameters:
 * TNAVC_wifiPosition
 * Return value:
 *  void
 ******************************************************************************
*/

void gpsc_app_wifi_pos_update(TNAVC_wifiPosition *p_wifiPosition)
{
    gpsc_ctrl_type *p_zGPSCControl= gp_zGPSCControl;
    gpsc_db_wifi_pos_type *p_z_WifiPos = &p_zGPSCControl->p_zGPSCDatabase->z_DBWifiPos;
    McpS8 tempbuff[256];
    McpU16 bufferlength;

    STATUSMSG("gpsc_app_wifi_pos_update: Update WiFi Pos in Databse: Lat=%.8lf, Lon=%.8lf, Unc =%.8lf, Noapused = %d",
		       p_wifiPosition->wifi_latitude,p_wifiPosition->wifi_longitude,p_wifiPosition->wifi_unc,p_wifiPosition->num_of_ap_used);
	if(p_wifiPosition->valid)
	{

        p_z_WifiPos->q_sytemtimeinmsec = os_get_current_time_inSec(p_zGPSCControl->p_zSysHandlers->hMcpf);

		p_z_WifiPos->d_WifiLatitude = p_wifiPosition->wifi_latitude;
		p_z_WifiPos->d_WifiLongitude= p_wifiPosition->wifi_longitude;

		/* Need to be check for the below parameter*/
		p_z_WifiPos->d_WifiAltitude = p_wifiPosition->wifi_altitude;
		/* WiFi Uncertainity is marked as *2 because GPS uncertainity is reported in 2sigma format instead of sigma,
		     We need to use 2sigma for WiFi as well so as to do appropriate comparision */
		p_z_WifiPos->d_WifiUnc = p_wifiPosition->wifi_unc;
		p_z_WifiPos->u_Valid = 1;
		p_z_WifiPos->q_RefFCount = p_zGPSCControl->p_zGPSCDatabase->z_DBGpsClock.q_FCount;
		p_z_WifiPos->d_WiFiEastUnc = p_wifiPosition->wifi_EastUnc;
		p_z_WifiPos->d_WiFiNorthUnc = p_wifiPosition->wifi_NorthUnc;
		p_z_WifiPos->d_WiFiEastVel = p_wifiPosition->wifi_EastVel;
		p_z_WifiPos->d_WiFiNorthVel = p_wifiPosition->wifi_NorthVel;
		p_z_WifiPos->d_WiFiEastVelUnc = p_wifiPosition->wifi_EastVelUnc;
		p_z_WifiPos->d_WiFiNorthVelUnc = p_wifiPosition->wifi_NorthVelUnc;

		p_z_WifiPos->d_VerticalUnc = 0;
		p_z_WifiPos->u_NumOfApUsed = p_wifiPosition->num_of_ap_used;
	   	p_z_WifiPos->q_timeOfWifiPosition = p_wifiPosition->time_of_wifiposition;
		p_z_WifiPos->FixPropagated = p_wifiPosition->FixPropagated;
		p_z_WifiPos->FixFromKalmanFilter = p_wifiPosition->FixFromKalmanFilter;
		p_z_WifiPos->KalmanEnable = p_wifiPosition->KalmanEnable; /* 4 lines change 20-04-11 */
		p_z_WifiPos->VBWSetting = p_wifiPosition->VBWSetting;
		p_z_WifiPos->AvgNumOfAPsUsed = p_wifiPosition->AvgNumOfAPsUsed;
		p_z_WifiPos->Filtered_Scale_Factor = p_wifiPosition->Filtered_Scale_Factor;
		STATUSMSG("gpsc_app_wifi_pos_update: Update WiFi Pos in Databse at TIME in Sec = %u, FCOUNT =%d", p_z_WifiPos->q_sytemtimeinmsec,p_z_WifiPos->q_RefFCount);
       	//u_CurrentTime = time(NULL);
       	//mcpf_ExtractDateAndTime(u_CurrentTime,&dateAndTimeStruct );
#if 0
		/* Write Date */
		MCP_HAL_STRING_Sprintf(tempbuff,"%d-%d-%d,",dateAndTimeStruct.year,dateAndTimeStruct.month,dateAndTimeStruct.day);
              bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
              mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uWifiFd,
						(McpU8 *)tempbuff, bufferlength);

		/* Write UTC Time */
		MCP_HAL_STRING_Sprintf(tempbuff,"%d:%d:%d,",dateAndTimeStruct.hour,dateAndTimeStruct.minute,dateAndTimeStruct.second);
              bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
              mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uWifiFd,
						(McpU8 *)tempbuff, bufferlength);

		/* Write LLA and Uncertainity */
             	MCP_HAL_STRING_Sprintf(tempbuff,"%.8lf,%.8lf,%.8lf,\n",p_z_WifiPos->d_wifi_lla[0],p_z_WifiPos->d_wifi_lla[1],p_z_WifiPos->w_Unc);
        	bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
     		mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uWifiFd,
						(McpU8 *)tempbuff, bufferlength);
#endif

	}
	else
	{
	  STATUSMSG("gpsc_app_wifi_pos_update:wifi pos is not valid");
	  p_z_WifiPos->u_Valid = 0;
	}
}


/*
 ******************************************************************************
 * gpsc_app_send_wifi_position_result
 *
 * Function description:
 *
 * This function send the wifi position
 *
 * Parameters:
 *
 * Return value:
 *  void
 ******************************************************************************
*/

void gpsc_app_send_wifi_position_result(gpsc_ctrl_type* p_zGPSCControl, gpsc_sess_specific_cfg_type* p_zSessSpecificCfg,U8 u_gps_pos_valid )
{
    T_GPSC_loc_fix_response * p_zLocFixResponse = NULL;
    gpsc_db_wifi_pos_type *p_z_WifiPos = &p_zGPSCControl->p_zGPSCDatabase->z_DBWifiPos;

    STATUSMSG("gpsc_app_send_wifi_position_result");



	if(p_zSessSpecificCfg->w_ResultTypeBitmap & GPSC_RESULT_RAW )
	{

        T_GPSC_loc_fix_response zLocFixResponse;
	    p_zLocFixResponse = (T_GPSC_loc_fix_response*)&zLocFixResponse;
		T_GPSC_raw_position* p_zRawPos = NULL;
		p_zRawPos = &p_zLocFixResponse->loc_fix_response_union.raw_position;
		/* populate position fix type*/
	    p_zLocFixResponse->loc_fix_pos_type_bitmap = WIFI_POS;

		p_zLocFixResponse->ctrl_loc_fix_response_union = GPSC_RESULT_RAW_POSITION;
		p_zLocFixResponse->loc_fix_session_id = p_zSessSpecificCfg->w_SessionTagId;
		gpsc_populate_raw_pos(p_zGPSCControl,p_zRawPos,p_zSessSpecificCfg );

			/* Overwrite the position data with Wifi Position, if that was selected */
			/* d_wifi_lla is used instead of d_out_lla because it is already in radians */

		p_zRawPos->position.latitude_radians = (U32)((p_z_WifiPos->d_WifiLatitude) / (DBL)C_LSB_LAT );
		p_zRawPos->position.longitude_radians = (S32)((p_z_WifiPos->d_WifiLongitude)/ (DBL)C_LSB_LON);
		/* for wifi position always 2D fix so altitude is unused parameter */
		//p_zRawPos->position.altitude_wgs84 = (U16)(p_z_WifiPos->d_WifiAltitude * 2);

		p_zRawPos->position.altitude_wgs84 = 0;

		p_zRawPos->position.uncertainty_east = (U16)(p_z_WifiPos->d_WiFiEastUnc * 10);
		p_zRawPos->position.uncertainty_north = (U16)(p_z_WifiPos->d_WiFiNorthUnc * 10);
		p_zRawPos->position.uncertainty_vertical = (U16)(p_z_WifiPos->d_VerticalUnc * 10);

		p_zRawPos->position.velocity_east = (S16)(p_z_WifiPos->d_WiFiEastVel);
		p_zRawPos->position.velocity_north = (S16)(p_z_WifiPos->d_WiFiNorthVel);


		STATUSMSG("gpsc_app_send_wifi_position_result: Output Latitude = %d, Longitude = %d, Altitude = %d",p_zRawPos->position.latitude_radians,p_zRawPos->position.longitude_radians,p_zRawPos->position.altitude_wgs84);
	//	SAPCALL(gpsc_app_loc_fix_ind);
		gpsc_app_loc_fix_ind (p_zLocFixResponse);
	}

	if(p_zSessSpecificCfg->w_ResultTypeBitmap & GPSC_RESULT_PROT )
	{
        T_GPSC_loc_fix_response zLocFixResponse;
	    p_zLocFixResponse = (T_GPSC_loc_fix_response*)&zLocFixResponse;
		T_GPSC_prot_position* p_zProtPos = NULL;
		p_zProtPos = &p_zLocFixResponse->loc_fix_response_union.prot_position;
		p_zLocFixResponse->loc_fix_session_id = p_zSessSpecificCfg->w_SessionTagId;
		/* populate position fix type*/
	    p_zLocFixResponse->loc_fix_pos_type_bitmap = WIFI_POS;
		p_zLocFixResponse->ctrl_loc_fix_response_union = GPSC_RESULT_PROT_POSITION;
		gpsc_populate_wifi_rrlp_pos(p_zGPSCControl,p_zProtPos,u_gps_pos_valid);
//		SAPCALL(gpsc_app_loc_fix_ind);
		gpsc_app_loc_fix_ind (p_zLocFixResponse);
	}

}

/*
 ******************************************************************************
 * gpsc_app_send_gps_position_result
 *
 * Function description:
 *
 * This function send the GPS fix
 *
 * Parameters:
 *
 * Return value:
 *  void
 ******************************************************************************
*/

void gpsc_app_send_gps_position_result(gpsc_ctrl_type* p_zGPSCControl, gpsc_sess_specific_cfg_type* p_zSessSpecificCfg)
{
	T_GPSC_loc_fix_response * p_zLocFixResponse = NULL;
	T_GPSC_prot_position* p_zProtPos = NULL;

	STATUSMSG("gpsc_app_send_gps_position_result -GPS Fixes");

	if(p_zSessSpecificCfg->w_ResultTypeBitmap & GPSC_RESULT_RAW )
	{

		T_GPSC_loc_fix_response zLocFixResponse;
		T_GPSC_raw_position* p_zRawPos = NULL;
		p_zLocFixResponse = (T_GPSC_loc_fix_response*)&zLocFixResponse;
		p_zRawPos = &p_zLocFixResponse->loc_fix_response_union.raw_position;
		p_zLocFixResponse->ctrl_loc_fix_response_union = GPSC_RESULT_RAW_POSITION;
		p_zLocFixResponse->loc_fix_session_id = p_zSessSpecificCfg->w_SessionTagId;
#ifdef ENABLE_INAV_ASSIST
		p_zLocFixResponse->live_debug_flag = inavc_get_debug_ind(pNavcCtrl->hInavc);
		STATUSMSG("Live Debug Flag: 0x%x", p_zLocFixResponse->live_debug_flag);
#else
		p_zLocFixResponse->live_debug_flag = 0;
#endif
		gpsc_populate_raw_pos(p_zGPSCControl,p_zRawPos,p_zSessSpecificCfg );
		/* populate position fix type*/
	    p_zLocFixResponse->loc_fix_pos_type_bitmap = GPS_POS;
		gpsc_app_loc_fix_ind (p_zLocFixResponse);

	}


	if(p_zSessSpecificCfg->w_ResultTypeBitmap & GPSC_RESULT_PROT )
	{
		T_GPSC_loc_fix_response zLocFixResponse;
			p_zLocFixResponse = (T_GPSC_loc_fix_response*)&zLocFixResponse;
		p_zLocFixResponse->loc_fix_session_id = p_zSessSpecificCfg->w_SessionTagId;
		p_zProtPos = &p_zLocFixResponse->loc_fix_response_union.prot_position;
		p_zLocFixResponse->ctrl_loc_fix_response_union = GPSC_RESULT_PROT_POSITION;
#ifdef ENABLE_INAV_ASSIST
		p_zLocFixResponse->live_debug_flag = inavc_get_debug_ind(pNavcCtrl->hInavc);
		STATUSMSG("Live Debug Flag: 0x%x", p_zLocFixResponse->live_debug_flag);
#else
		p_zLocFixResponse->live_debug_flag = 0;
#endif
		gpsc_populate_rrlp_pos(p_zGPSCControl,p_zProtPos);
		/* populate position fix type*/
	    p_zLocFixResponse->loc_fix_pos_type_bitmap = GPS_POS;
		gpsc_app_loc_fix_ind (p_zLocFixResponse);
	}

}


/*
 ******************************************************************************
 * gpsc_app_send_blended_position_result
 *
 * Function description:
 *
 * This function send the blended position
 *
 * Parameters:
 *
 * Return value:
 *  void
 ******************************************************************************
*/

void gpsc_app_send_blended_position_result(gpsc_ctrl_type* p_zGPSCControl, gpsc_sess_specific_cfg_type* p_zSessSpecificCfg,U8 u_gps_pos_valid )
{
    T_GPSC_loc_fix_response * p_zLocFixResponse = NULL;

    gpsc_db_blended_pos_type *p_z_BlendedPos = &p_zGPSCControl->p_zGPSCDatabase->z_DBBlendedPos;

    STATUSMSG("gpsc_app_send_Bleneded position");

	if(p_zSessSpecificCfg->w_ResultTypeBitmap & GPSC_RESULT_RAW )
	{

       	T_GPSC_loc_fix_response zLocFixResponse;
		T_GPSC_raw_position* p_zRawPos = NULL;
	    p_zLocFixResponse = (T_GPSC_loc_fix_response*)&zLocFixResponse;
		p_zRawPos = &p_zLocFixResponse->loc_fix_response_union.raw_position;
		/* populate position fix type*/
	    p_zLocFixResponse->loc_fix_pos_type_bitmap = GPS_WIFI_BLEND;

		p_zLocFixResponse->ctrl_loc_fix_response_union = GPSC_RESULT_RAW_POSITION;
		p_zLocFixResponse->loc_fix_session_id = p_zSessSpecificCfg->w_SessionTagId;
#ifdef ENABLE_INAV_ASSIST
		p_zLocFixResponse->live_debug_flag = inavc_get_debug_ind(pNavcCtrl->hInavc);
		STATUSMSG("Live Debug Flag: 0x%x", p_zLocFixResponse->live_debug_flag);
#else
		p_zLocFixResponse->live_debug_flag = 0;
#endif
		gpsc_populate_raw_pos(p_zGPSCControl,p_zRawPos,p_zSessSpecificCfg );

			/* Overwrite the position data with Wifi Position, if that was selected */
			/* d_wifi_lla is used instead of d_out_lla because it is already in radians */

		p_zRawPos->position.latitude_radians = (S32)((p_z_BlendedPos->d_BlendLatitude) / (DBL)C_LSB_LAT );
		p_zRawPos->position.longitude_radians = (S32)((p_z_BlendedPos->d_BlendLongitude)/ (DBL)C_LSB_LON);

		if(p_z_BlendedPos->u_2Dor3Dfix)
		  p_zRawPos->position.altitude_wgs84 = (S16)(p_z_BlendedPos->d_BlendAltitude * 2);
		else
			/* for 2D fix altitude is 0 */
		  p_zRawPos->position.altitude_wgs84 = 0;

		p_zRawPos->position.uncertainty_east = (U16)(p_z_BlendedPos->d_BlendEastUnc * 10);
		p_zRawPos->position.uncertainty_north = (U16)(p_z_BlendedPos->d_BlendNorthUnc * 10);
		p_zRawPos->position.uncertainty_vertical = (U16)(p_z_BlendedPos->d_VerticalUnc * 10);


		STATUSMSG(" RAW POS Latitude = %.8lf, Longitude = %.8lf, Altitude = %.8lf",p_z_BlendedPos->d_BlendLatitude,p_z_BlendedPos->d_BlendLongitude,p_z_BlendedPos->d_BlendAltitude);
	//	SAPCALL(gpsc_app_loc_fix_ind);
		gpsc_app_loc_fix_ind (p_zLocFixResponse);
	}

	if(p_zSessSpecificCfg->w_ResultTypeBitmap & GPSC_RESULT_PROT )
	{

        T_GPSC_loc_fix_response zLocFixResponse;
		T_GPSC_prot_position* p_zProtPos = NULL;
	    p_zLocFixResponse = (T_GPSC_loc_fix_response*)&zLocFixResponse;
		p_zProtPos = &p_zLocFixResponse->loc_fix_response_union.prot_position;
		/* populate position fix type*/
	    p_zLocFixResponse->loc_fix_pos_type_bitmap = GPS_WIFI_BLEND;

		p_zLocFixResponse->loc_fix_session_id = p_zSessSpecificCfg->w_SessionTagId;
		p_zLocFixResponse->ctrl_loc_fix_response_union = GPSC_RESULT_PROT_POSITION;
#ifdef ENABLE_INAV_ASSIST
		p_zLocFixResponse->live_debug_flag = inavc_get_debug_ind(pNavcCtrl->hInavc);
		STATUSMSG("Live Debug Flag: 0x%x", p_zLocFixResponse->live_debug_flag);
#else
		p_zLocFixResponse->live_debug_flag = 0;
#endif
		STATUSMSG(" BEFORE PROT Latitude = %.8lf, Longitude = %.8lf, Altitude = %.8lf, Uncertainity = %.8lf\n",p_z_BlendedPos->d_BlendLatitude,p_z_BlendedPos->d_BlendLongitude,p_z_BlendedPos->d_BlendAltitude,p_z_BlendedPos->d_BlendUnc);
		STATUSMSG(" BEFORE PROT Uncertainity = %.8lf EastUnc %.8lf, NorthUnc %.08lf\n",p_z_BlendedPos->d_BlendUnc,p_z_BlendedPos->d_BlendEastUnc,p_z_BlendedPos->d_BlendNorthUnc);
		gpsc_populate_gpswifi_blend_rrlp_pos(p_zGPSCControl,p_zProtPos,u_gps_pos_valid);

		STATUSMSG("gpsc_app_send_blended_position_result: PROT Output Latitude = %d, Longitude = %d, Altitude = %d",p_zProtPos->ellip_alt_unc_ellip.latitude,p_zProtPos->ellip_alt_unc_ellip.longitude,p_zProtPos->ellip_alt_unc_ellip.altitude);
//		SAPCALL(gpsc_app_loc_fix_ind);
		gpsc_app_loc_fix_ind (p_zLocFixResponse);
	}

}


#ifdef WIFI_DEMO

/*
 ******************************************************************************
 * gpsc_app_send_gps_wifi_blend_report
 *
 * Function description:
 *
 * This function send the wifi,gps and bleneded output for toolkit
 *
 * Parameters:
 *
 * Return value:
 *  void
 ******************************************************************************
*/

void gpsc_app_send_gps_wifi_blend_report(gpsc_ctrl_type* p_zGPSCControl, gpsc_sess_specific_cfg_type* p_zSessSpecificCfg,U8 u_gps_pos_valid, U8 u_wifi_pos_valid)
{
    TNAVC_gpsWifiBlendReport * p_zGpsWifiBlendReport = NULL;
	TNAVC_gpsWifiBlendReport  zGpsWifiBlendReport;
    U32 temptime;
	gpsc_db_type          *p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_report_pos_type  *p_zReportPos  = &p_zGPSCDatabase->z_DBPos.z_ReportPos;
	gpsc_db_gps_clk_type  *p_zDBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;
	gpsc_db_wifi_pos_type *p_z_WifiPos = &p_zGPSCControl->p_zGPSCDatabase->z_DBWifiPos;
    gpsc_db_blended_pos_type *p_z_BlendedPos = &p_zGPSCControl->p_zGPSCDatabase->z_DBBlendedPos;
	gpsc_db_gps_meas_type*   p_zDBGpsMeas  = &p_zGPSCControl->p_zGPSCDatabase->z_DBGpsMeas;
	gpsc_meas_report_type *p_zMeasReport = &p_zDBGpsMeas->z_MeasReport;

    p_zGpsWifiBlendReport = (TNAVC_gpsWifiBlendReport*)&zGpsWifiBlendReport;
    STATUSMSG("gpsc_app_send_gps_wifi_blend_report: Sending all the position to toolkit");

      p_zGpsWifiBlendReport->session_id= p_zSessSpecificCfg->w_SessionTagId;
    //p_zGpsWifiBlendReport->loc_fix_session_id = p_zSessSpecificCfg->w_SessionTagId;
    /*populate  GPS*/
	if(u_gps_pos_valid )
	{
		p_zGpsWifiBlendReport->gps_latitude = p_zReportPos->l_Lat;
		p_zGpsWifiBlendReport->gps_longitude =p_zReportPos->l_Long;
		p_zGpsWifiBlendReport->gps_altitude = p_zReportPos->x_Height;
		p_zGpsWifiBlendReport->gps_timeofpos = p_zMeasReport->z_MeasToa.q_GpsMsec;
		p_zGpsWifiBlendReport->gps_fix_valid = TRUE;

	}
	else
		p_zGpsWifiBlendReport->gps_fix_valid = FALSE;

   /*populate WIFI*/
	if(u_wifi_pos_valid )
	{

	   p_zGpsWifiBlendReport->wifi_latitude  = (S32)((p_z_WifiPos->d_WifiLatitude) / (DBL)C_LSB_LAT );
	   p_zGpsWifiBlendReport->wifi_longitude = (S32)((p_z_WifiPos->d_WifiLongitude)/ (DBL)C_LSB_LON);

	   temptime = p_zMeasReport->z_MeasToa.q_GpsMsec - ((p_z_WifiPos->q_sytemtimeinmsec - p_z_WifiPos->q_timeOfWifiPosition)*1000);
       p_zGpsWifiBlendReport->wifi_timeofpos = temptime;
	   p_zGpsWifiBlendReport->wifi_fix_valid = TRUE;
	}
	else
		p_zGpsWifiBlendReport->wifi_fix_valid = FALSE;


    /*populate BLENDED*/

     p_zGpsWifiBlendReport->blended_latitude  = (S32)((p_z_BlendedPos->d_BlendLatitude) / (DBL)C_LSB_LAT );
	 p_zGpsWifiBlendReport->blended_longitude = (S32)((p_z_BlendedPos->d_BlendLongitude)/ (DBL)C_LSB_LON);

	 if(p_z_BlendedPos->u_2Dor3Dfix)
		  p_zGpsWifiBlendReport->blended_altitude = (S16)(p_z_BlendedPos->d_BlendAltitude * 2);
	 else
			/* for 2D fix altitude is 0 */
	   p_zGpsWifiBlendReport->blended_altitude = 0;
	 p_zGpsWifiBlendReport->blended_timeofpos = p_zMeasReport->z_MeasToa.q_GpsMsec;

    /* Send the position repoprts to toolkit from here */

	STATUSMSG("gpsc_app_send_gps_wifi_blend_report: call gpsc_app_gps_wifi_blend_pos_ind ");
	 gpsc_app_gps_wifi_blend_pos_ind(p_zGpsWifiBlendReport);


}

#endif

/*
 ******************************************************************************
 * gpsc_populate_wifi_rrlp_pos
 *
 * Function description:
 *
 * This function populate the wifi fixes as per the protocol specific
 *
 * Parameters:
 *
 * Return value:
 *  void
 ******************************************************************************
*/

void gpsc_populate_wifi_rrlp_pos(gpsc_ctrl_type* p_zGPSCControl,T_GPSC_prot_position* p_prot_position,U8 u_gps_pos_valid)
{

	T_GPSC_ellip_alt_unc_ellip *p_ellip_alt_unc_ellip = &p_prot_position->ellip_alt_unc_ellip;
	DBL d_Double;
	U16 w_Word;
	U8	u_EastUncK, u_NorthUncK; /* K in the unc representation */
	FLT f_float;
	gpsc_db_gps_clk_type  *p_zDBGpsClock = &p_zGPSCControl->p_zGPSCDatabase->z_DBGpsClock;
		  gpsc_db_wifi_pos_type *p_z_WifiPos = &p_zGPSCControl->p_zGPSCDatabase->z_DBWifiPos;

	/* Overwrite the position data with Wifi Position, if that was selected */
	/* d_wifi_lla is used instead of d_out_lla because it is already in radians */
	 p_prot_position->gps_tow = p_zDBGpsClock->q_GpsMs;

	/*  Wifi is always  2D fix */;
#if 0
	if(u_gps_pos_valid && (p_zReportPos->w_PositionFlags & 0x02))
	{
		p_prot_position->prot_fix_result =	GPSC_PROT_FIXRESULT_3D;
		p_ellip_alt_unc_ellip->shape_code =  C_POS_SHAPE_ELLIP_ALT_UNC_ELLIP; /*9*/
		p_ellip_alt_unc_ellip->v_altitude = 1;
		p_ellip_alt_unc_ellip->v_altitude_sign = 1;
		p_ellip_alt_unc_ellip->v_unc_alt = 1;
	}
	else
	{
#endif
		p_prot_position->prot_fix_result =	GPSC_PROT_FIXRESULT_2D;
		p_ellip_alt_unc_ellip->shape_code =  C_POS_SHAPE_ELLIP_UNC_ELLIP; /*4*/
		/*Set these to invalid, however we will populate the values
		  with the values GPS sends us. But the will be stale altitude values*/
		p_ellip_alt_unc_ellip->v_altitude = 0;
		p_ellip_alt_unc_ellip->v_altitude_sign = 0;
		p_ellip_alt_unc_ellip->v_unc_alt = 0;
	//}

	/* shape*/


	/******** latitude ****************/

	//d_Double = (DBL)(p_z_WifiPos->d_WifiLatitude * C_2_23_OVER_90) * (DBL)C_LSB_LAT * (DBL)C_RAD_TO_DEG;
	d_Double = (DBL)(p_z_WifiPos->d_WifiLatitude * C_2_23_OVER_90)* (DBL)C_RAD_TO_DEG;

	p_ellip_alt_unc_ellip->latitude_sign = 0x0;
	if (d_Double < 0 ) /* if latitude in the southern hemesphere */
	{
		  p_ellip_alt_unc_ellip->latitude_sign = 0x80; /* sign bit high for southern
													 hemesphere */

		  /* then code the rest with the absolute value of the latitude */
		  d_Double = -d_Double;
	 }

			p_ellip_alt_unc_ellip->latitude = (U32)d_Double;

	 /********** longitude ************/
	// d_Double = (DBL)(p_z_WifiPos->d_WifiLongitude  * C_2_24_OVER_360)*(DBL)C_LSB_LON * (DBL)C_RAD_TO_DEG;
	 d_Double = (DBL)(p_z_WifiPos->d_WifiLongitude  * C_2_24_OVER_360)* (DBL)C_RAD_TO_DEG;

	 p_ellip_alt_unc_ellip->longitude = (S32)d_Double;

	 /********* altitude ***************/
#if 0
	 if ( p_z_WifiPos->d_WifiAltitude < 0 )
	 {
		  /*** depth into earth condition */
		  w_Word = (U16)-p_z_WifiPos->d_WifiAltitude * 2;
		  p_ellip_alt_unc_ellip->altitude_sign = (U8)0x01;
	  }
	  else
	  {
		  w_Word = p_z_WifiPos->d_WifiAltitude * 2;
		  p_ellip_alt_unc_ellip->altitude_sign = 0x0;
	   }
	   w_Word >>= 1; /* LSB of p_zReportPos->x_Height is 0.5 meters */
	   p_ellip_alt_unc_ellip->altitude = w_Word;
#endif
       p_ellip_alt_unc_ellip->altitude = 0;  //for wifi only case

	   STATUSMSG("gpsc_populate_wifi_rrlp_pos: Lat=%d, Lon=%d, Ht=%d",p_ellip_alt_unc_ellip->latitude,p_ellip_alt_unc_ellip->longitude,p_ellip_alt_unc_ellip->altitude);

	   /********* uncertainty  *************/

	   /*** AI2 limits the representation of horizontal uncs to 6553.5m
				 and RRLP's limit is 1800 km, so no need to cap uncs here */

	   /* RRLP uses K to discribe altitude unc:
			   Altitude (meters) = C * ( (1+x) ** K - 1 ),
			   where C = 10, x = 0.1. */


	   u_EastUncK = (U8)( ext_log ( (FLT)p_z_WifiPos->d_WiFiEastUnc *
								   (FLT)C_LSB_EER * 0.1f + 1.0f
						 ) /
						 ext_log(1.1f)
					 );

	   u_NorthUncK = (U8)( ext_log ( (FLT)p_z_WifiPos->d_WiFiNorthUnc *
									(FLT)C_LSB_NER * 0.1f + 1.0f
						) /
					  ext_log(1.1f)
					  );


		p_ellip_alt_unc_ellip->orient_major = 0; /* from North, clockwise */
		p_ellip_alt_unc_ellip->unc_major = u_NorthUncK;
		p_ellip_alt_unc_ellip->unc_minor = u_EastUncK;

		/*** AI2 limits the representation of vertical unc to 32767.5m
				 and RRLP's limit is 990.5m, so we need to cap it here */
		f_float = (FLT)p_z_WifiPos->d_VerticalUnc * (FLT)C_LSB_VER;
		if ( f_float > 990.5f) f_float = 990.5;

		/* RRLP uses K to discribe altitude unc:
			   Altitude (meters) = C * ( (1+x) ** K - 1 ),
			   where C = 45, x = 0.025. Note 1/45 = 0.022222222 */
		p_ellip_alt_unc_ellip->unc_alt =
			  (U8) ( ext_log ( f_float * 0.02222f + 1.0f ) / ext_log (1.025f));
		p_ellip_alt_unc_ellip->confidence = 0; /* we don't use it */

}


/*
 ******************************************************************************
 * gpsc_populate_gpswifi_blend_rrlp_pos
 *
 * Function description:
 *
 * This function populate the wifi fixes as per the protocol specific
 *
 * Parameters:
 *
 * Return value:
 *  void
 ******************************************************************************
*/

void gpsc_populate_gpswifi_blend_rrlp_pos(gpsc_ctrl_type* p_zGPSCControl,T_GPSC_prot_position* p_prot_position,U8 u_gps_pos_valid)
{

	T_GPSC_ellip_alt_unc_ellip *p_ellip_alt_unc_ellip = &p_prot_position->ellip_alt_unc_ellip;
	DBL d_Double;
	U16 w_Word;
	U32 timeRet=0;
	U8	u_EastUncK, u_NorthUncK; /* K in the unc representation */
	FLT f_float;
	gpsc_db_type          *p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_db_gps_clk_type  *p_zDBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;
	gpsc_db_blended_pos_type *p_z_BlendedPos = &p_zGPSCDatabase->z_DBBlendedPos;

	/* Overwrite the position data with Wifi Position, if that was selected */
	/* d_wifi_lla is used instead of d_out_lla because it is already in radians */
	 p_prot_position->gps_tow = p_zDBGpsClock->q_GpsMs;
	timeRet = gpsc_db_gps_time_in_secs(p_zGPSCControl);
	/*  use system time till  utc time is ready*/
	if(timeRet == 0){
		 p_prot_position->gps_secs = (McpDBL)MCP_HAL_OS_GetSystemTime();
	}else {
		p_prot_position->gps_secs = (McpDBL)timeRet;
	}

	STATUSMSG("gpsc_populate_gpswifi_blend_rrlp_pos: gps_secs = %lf", p_prot_position->gps_secs);
	/*added by considering wifipositioning */
	p_ellip_alt_unc_ellip->velocity_flag = 0;

	/* 3D fix : 2D fix */;
	if(p_z_BlendedPos->u_2Dor3Dfix)
	{
	    //p_zReportPos->w_PositionFlags =  p_zReportPos->w_PositionFlags | 0x02;
		p_prot_position->prot_fix_result =	GPSC_PROT_FIXRESULT_3D;
		p_ellip_alt_unc_ellip->shape_code =  C_POS_SHAPE_ELLIP_ALT_UNC_ELLIP; /*9*/
		p_ellip_alt_unc_ellip->v_altitude = 1;
		p_ellip_alt_unc_ellip->v_altitude_sign = 1;
		p_ellip_alt_unc_ellip->v_unc_alt = 1;
	}
	else
	{
	    //p_zReportPos->w_PositionFlags =  p_zReportPos->w_PositionFlags | 0x01;
		p_prot_position->prot_fix_result =	GPSC_PROT_FIXRESULT_2D;
		p_ellip_alt_unc_ellip->shape_code =  C_POS_SHAPE_ELLIP_UNC_ELLIP; /*4*/
		/*Set these to invalid, however we will populate the values
		  with the values GPS sends us. But the will be stale altitude values*/
		p_ellip_alt_unc_ellip->v_altitude = 0;
		p_ellip_alt_unc_ellip->v_altitude_sign = 0;
		p_ellip_alt_unc_ellip->v_unc_alt = 0;
	}
#if 0
	else
	{
		ERRORMSG("Error : Expected position flags to be either 2D or 3D");
		p_prot_position->prot_fix_result = GPSC_PROT_FIXRESULT_NOFIX;
		return;
	}
#endif

	/* shape*/


	/******** latitude ****************/

	//d_Double = (DBL)(p_z_BlendedPos->d_BlendLatitude * C_2_23_OVER_90) * (DBL)C_LSB_LAT * (DBL)C_RAD_TO_DEG;
    d_Double = (DBL)(p_z_BlendedPos->d_BlendLatitude * C_2_23_OVER_90) * (DBL)C_RAD_TO_DEG;
	p_ellip_alt_unc_ellip->latitude_sign = 0x0;
	if (d_Double < 0 ) /* if latitude in the southern hemesphere */
	{
		  p_ellip_alt_unc_ellip->latitude_sign = 0x80; /* sign bit high for southern
													 hemesphere */

		  /* then code the rest with the absolute value of the latitude */
		  d_Double = -d_Double;
	 }

			p_ellip_alt_unc_ellip->latitude = (U32)d_Double;

	 /********** longitude ************/
	 //d_Double = (DBL)(p_z_BlendedPos->d_BlendLongitude  * C_2_24_OVER_360)*(DBL)C_LSB_LON * (DBL)C_RAD_TO_DEG;
	 d_Double = (DBL)(p_z_BlendedPos->d_BlendLongitude  * C_2_24_OVER_360) * (DBL)C_RAD_TO_DEG;

	 p_ellip_alt_unc_ellip->longitude = (S32)d_Double;

	 /********* altitude ***************/

	 if ( p_z_BlendedPos->d_BlendAltitude < 0 )
	 {
		  /*** depth into earth condition */
		  w_Word = (U16)-p_z_BlendedPos->d_BlendAltitude * 2;
		  p_ellip_alt_unc_ellip->altitude_sign = (U8)0x01;
	  }
	  else
	  {
		  w_Word = p_z_BlendedPos->d_BlendAltitude * 2;
		  p_ellip_alt_unc_ellip->altitude_sign = 0x0;
	   }
	   w_Word >>= 1; /* LSB of p_zReportPos->x_Height is 0.5 meters */
	   p_ellip_alt_unc_ellip->altitude = w_Word;


	   STATUSMSG("gpsc_populate_gpswifi_blend_rrlp_pos: Lat=%d, Lon=%d, Ht=%d",p_ellip_alt_unc_ellip->latitude,p_ellip_alt_unc_ellip->longitude,p_ellip_alt_unc_ellip->altitude);
	   STATUSMSG(" gpsc_populate_gpswifi_blend_rrlp_pos :Blened Lat = %.8lf, Long = %.8lf, Alt = %.8lf",p_z_BlendedPos->d_BlendLatitude,p_z_BlendedPos->d_BlendLongitude,p_z_BlendedPos->d_BlendAltitude);
	   //STATUSMSG("gpsc_populate_gpswifi_blend_rrlp_pos: BLENDED Lat=%d, Lon=%d, Ht=%d",p_z_BlendedPos->d_BlendLatitude,p_z_BlendedPos->d_BlendLongitude,p_z_BlendedPos->d_BlendAltitude);

	   /********* uncertainty  *************/

	   /*** AI2 limits the representation of horizontal uncs to 6553.5m
				 and RRLP's limit is 1800 km, so no need to cap uncs here */

	   /* RRLP uses K to discribe altitude unc:
			   Altitude (meters) = C * ( (1+x) ** K - 1 ),
			   where C = 10, x = 0.1. */

	   u_EastUncK = (U8)( ext_log ( (FLT)p_z_BlendedPos->d_BlendEastUnc *
								   (FLT)C_LSB_EER * 0.1f + 1.0f
						 ) /
						 ext_log(1.1f)
					 );

	   u_NorthUncK = (U8)( ext_log ( (FLT)p_z_BlendedPos->d_BlendNorthUnc *
									(FLT)C_LSB_NER * 0.1f + 1.0f
						) /
					  ext_log(1.1f)
					  );

			if (p_z_BlendedPos->d_BlendEastUnc  > p_z_BlendedPos->d_BlendNorthUnc)
			{
			  /* East-West being semi-major */
			  p_ellip_alt_unc_ellip->orient_major = 89; /* from North, clockwise */
			  p_ellip_alt_unc_ellip->unc_major= u_EastUncK;
			  p_ellip_alt_unc_ellip->unc_minor = u_NorthUncK;
			}
			else
			{
			  /* North-South being semi-major */
			  p_ellip_alt_unc_ellip->orient_major = 0; /* from North, clockwise */
			  p_ellip_alt_unc_ellip->unc_major = u_NorthUncK;
			  p_ellip_alt_unc_ellip->unc_minor = u_EastUncK;
			}

#if 0
		p_ellip_alt_unc_ellip->orient_major = 0; /* from North, clockwise */
		p_ellip_alt_unc_ellip->unc_major = u_NorthUncK;
w_NorthUnc		p_ellip_alt_unc_ellip->unc_minor = u_EastUncK;
#endif
		/*** AI2 limits the representation of vertical unc to 32767.5m
				 and RRLP's limit is 990.5m, so we need to cap it here */
		f_float = (FLT)p_z_BlendedPos->d_VerticalUnc * (FLT)C_LSB_VER;
		if ( f_float > 990.5f) f_float = 990.5;

		/* RRLP uses K to discribe altitude unc:
			   Altitude (meters) = C * ( (1+x) ** K - 1 ),
			   where C = 45, x = 0.025. Note 1/45 = 0.022222222 */
		p_ellip_alt_unc_ellip->unc_alt =
			  (U8) ( ext_log ( f_float * 0.02222f + 1.0f ) / ext_log (1.025f));
		p_ellip_alt_unc_ellip->confidence = 0; /* we don't use it */

}

#endif  //#ifdef WIFI_ENABLE
McpU32 gpsc_get_get_gps_secs_from_utctimestamp(McpU8* utcTs,McpU8 len)
{

	McpU16 time_utc[6]={0,0,0,0,0,0}; //YYYY, MM, DD, HH, MM, SS
	McpU8 tz_str_len,y_len=0;
	char mm[3]= {'0','0','\0'}, dd[3]= {'0','0','\0'}, hh[3]= {'0','0','\0'}, mn[3]= {'0','0','\0'},ss[3]= {'0','0','\0'};
	McpS8 tz_hour=0,tz_min=0;


	if(len<UTC_TS_MIN_LENGTH||NULL==utcTs)
		return 0;

	if(utcTs[len-1]=='Z')
	{
		STATUSMSG("UTC TIME Present");
		tz_str_len=0;

	}
	else
	{
		STATUSMSG("Local TIME with TZ present");
		char tz_hh[3]= {'0','0','\0'},tz_mn[3]= {'0','0','\0'};
		strncpy(tz_hh,&utcTs[len-4],2);
		strncpy(tz_mn,&utcTs[len-2],2);
		tz_hour=atoi(tz_hh);
		tz_min=atoi(tz_mn);

		if(utcTs[len-5]=='-')
		{
			tz_hour=(-1)*tz_hour;
			tz_min=(-1)*tz_min;
		}

		tz_str_len=5;//+0530 IND
	}


	if(len-tz_str_len==UTC_TS_MIN_LENGTH)
	{
		STATUSMSG("UTC TIME in YYMMDDHHMNSSZ is Present");
		char yy[3] = {'0','0','\0'}; /* Klocwork changes */
		y_len=2;
		strncpy(yy,&utcTs[0],sizeof(yy)-1);
		time_utc[0] = YY_FORMAT_BASE_YEAR + atoi(yy);

	}
	else if (len-tz_str_len==UTC_TS_MIN_LENGTH+2)
	{
		STATUSMSG("UTC TIME in YYYYMMDDHHMNSSZ is Present");
		char yy[5] = {'0','0','0','0','\0'}; /* Klocwork changes */
		y_len=4;
		strncpy(yy,&utcTs[0],sizeof(yy)-1);
		time_utc[0]=atoi(yy);

	}
	else
	{
		STATUSMSG("Invalid UTC TIME Format");
		return 0;
	}


	strncpy(mm,&utcTs[y_len],2);
	strncpy(dd,&utcTs[y_len+2],2);
	strncpy(hh,&utcTs[y_len+4],2);
	strncpy(mn,&utcTs[y_len+6],2);
	strncpy(ss,&utcTs[y_len+8],2);
	time_utc[1]=atoi(mm);
	time_utc[2]=atoi(dd);
	//to do - Handle the chain of change in hour,day,month,year
	time_utc[3]=atoi(hh)+tz_hour;
	time_utc[4]=atoi(mn)+tz_min;
	time_utc[5]=atoi(ss);


	STATUSMSG("UTC TIME in YYYY%d-MM%d-DD%d-HH%d-MN%d-SS%d is",time_utc[0],time_utc[1],time_utc[2],time_utc[3],time_utc[4],time_utc[5]);


	return gpsc_app_convert_utc2gps(time_utc);

}


McpU32 gpsc_app_convert_utc2gps(McpU16 UTC[])
{
    //Input UTC is of the form UTC - YYYY MM DD HH MM SS.sss
    //convert UTC to julian date
    //from julian date convert it to week and ms taking care of J1980 offset

    //Formulas for conversion from Wellenhoff
    McpU16 month[] = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365};

    //McpDBL year = 365.25;  /* Klocwork Changes */
    McpU16 mjcent = 15019;
    //c1=1461.0;
	STATUSMSG("gpsc_app_convert_utc2gps: Entering");
    if (UTC[0]>1900)
        UTC[0]=UTC[0] - 1900;


    McpU16 mjd = mjcent+UTC[2]+month[UTC[1]-1]+floor((1461*UTC[0]-1)/4);
    if (((UTC[0] -(4*floor(UTC[0]/4))) == 0)&& UTC[1]>2)
        mjd=mjd+1;


    McpDBL julianday = mjd + 2400000.5;
    julianday = julianday + (UTC[3]+UTC[4]/60.0 + UTC[5]/3600.0)/24.0;

    McpDBL J1980 = 2444244.5;

    julianday = julianday - J1980;	//GPS is referred from 1980 Jan6th
    //printf("Inside Utc2Gps(int UTC[]) = %f \n",julianday * 86400);
	STATUSMSG("gpsc_app_convert_utc2gps: julianday*86400 = %ld",(McpU32)(julianday * 86400));
    return (McpU32)(julianday * 86400);

}


McpU16 * gpsc_app_convert_gps2cal( McpU32 gpsTime)
 {
     McpDBL JD = gpsc_app_convert_gps2julianday( gpsTime);
     return gpsc_app_convert_julianday2caldate(JD);

 }

McpDBL gpsc_app_convert_gps2julianday( McpU32 gpsTime)
{

    //Constants
    McpDBL SEC_PER_DAY     = 86400;
    //McpDBL NUM_GPS_WEEKS   = 1024;  //% Number of GPS weeks before a week rollover  /* Klocwork Changes */

   //Julian day number of the birthday of GPS (Midnight of January 5, 1980)
   //Can compute using January 6, 1980, HMS = 00:00:00
   McpDBL JD_GPS = 2444244.5;

   //Compute the (decimal) number of elapsed days since the start of GPS
   McpDBL gpsDays = (gpsTime / SEC_PER_DAY)  + 0.5;

    //Compute the current (decimal) Julian Day
    //JD = JD_GPS + gpsDays - 0.5
    return (JD_GPS + gpsDays - 0.5);
}



McpU16 *gpsc_app_convert_julianday2caldate(McpDBL  JD)
{

    static McpU16 cal_time[6] = {0,0,0,0,0,0};
    //Constants
    McpDBL SEC_PER_DAY  = 86400;

    // Most of the calculations do not need the decimal portion
    McpDBL JDN = floor(JD);

    //Compute Seconds After Midnight (SAM) - note that days in the Julian
    //date system start at 12:00pm, so must adjust the SAM accordingly
    McpDBL partialDay = JD - JDN;
    McpDBL sam = 0; /* Klocwork changes */

    if ( (partialDay >= 0) && (partialDay < 0.5) )
        sam = (partialDay + 0.5) * SEC_PER_DAY;

    else if( (partialDay >= 0.5) && (partialDay < 1.0) )
        sam = (partialDay - 0.5) * SEC_PER_DAY;

    else
		STATUSMSG("SAM requires partial days, i.e. less than 86400 seconds!\n");

    //Compute YMD
    McpDBL Z,W,X,A,B,C,D,E,F;

    Z = JD + 0.5 ;
    W = floor( (Z - 1867216.25) / 36524.25 );
    X = floor( W / 4 );
    A = Z + 1 + W - X;
    B = A + 1524;
    C = floor( (B - 122.1) / 365.25 );
    D = floor( 365.25 * C );
    E = floor( (B - D) / 30.6001 );
    F = floor( 30.6001 * E );

    //Compute day of the month
    //day = floor(B - D - F);
    cal_time[2] = floor(B - D - F);

    //Compute month of the year
    if( (E - 1) <= 12 )
        cal_time[1] =  E - 1;    //month = E - 1;
    else if( (E-13) <= 12 )
          cal_time[1] = E - 13;  //month = E - 13;
    else
		STATUSMSG("Can''t get number less than 12 in gpsc_app_convert_julianday2caldate \n");

    //Compute year
    if ( cal_time[1] < 3 )        // month < 3
        cal_time[0] = C - 4715;   // year = C - 4715;
    else
        cal_time[0] = C - 4716;   //year = C - 4716;


       McpDBL TOL = 0.000001;  //% floor function is sensitive to near-zero values
       cal_time[3] = floor ((sam / 3600) + TOL);
       cal_time[4] = floor( ((sam - (cal_time[3] * 3600) ) / 60 ) + TOL );
       cal_time[5] = ceil( sam - cal_time[3] * 3600 - cal_time[4] * 60);

    return cal_time;

}

McpU8* gpsc_app_convert_gps2utc(McpU32 gpsTime, McpU8* p_len )
{
    static McpU8 utcTs[50];
    McpU8 buff[3];
    McpU16* cal_time = gpsc_app_convert_gps2cal(gpsTime); //747437400

    if(cal_time[0] > 2000)
    {
        cal_time[0] = cal_time[0] - 2000;
        if (cal_time[0] < 10)
        {
            MCP_HAL_STRING_ItoA (0,utcTs);
            MCP_HAL_STRING_ItoA (cal_time[0],buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);

        }else
        {
            MCP_HAL_STRING_ItoA (cal_time[0],utcTs);
        }
    }
    else
    {
        MCP_HAL_STRING_ItoA (cal_time[0],utcTs);
    }

    if (cal_time[1] < 10)
    {
            MCP_HAL_STRING_ItoA (0,buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
            MCP_HAL_STRING_ItoA (cal_time[1],buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
    }else
    {
            MCP_HAL_STRING_ItoA (cal_time[1],buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
    }


    if (cal_time[2] < 10)
    {
            MCP_HAL_STRING_ItoA (0,buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
            MCP_HAL_STRING_ItoA (cal_time[2],buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);

    }else
    {
            MCP_HAL_STRING_ItoA (cal_time[2],buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
    }


    if (cal_time[3] < 10)
    {
            MCP_HAL_STRING_ItoA (0,buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
            MCP_HAL_STRING_ItoA (cal_time[3],buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
    }else
    {
            MCP_HAL_STRING_ItoA (cal_time[3],buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
    }


    if (cal_time[4] < 10)
    {
            MCP_HAL_STRING_ItoA (0,buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
            MCP_HAL_STRING_ItoA (cal_time[4],buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
    }else
    {
            MCP_HAL_STRING_ItoA (cal_time[4],buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
    }


    if (cal_time[5] < 10)
    {
            MCP_HAL_STRING_ItoA (0,buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
            MCP_HAL_STRING_ItoA (cal_time[5],buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
    }else
    {
            MCP_HAL_STRING_ItoA (cal_time[5],buff);
            MCP_HAL_STRING_StrCat(utcTs,buff);
    }

    MCP_HAL_STRING_StrCat(utcTs,"Z");
    *p_len = MCP_HAL_STRING_StrLen(utcTs);

    return utcTs;

}

/*
 ******************************************************************************
 * gpsc_app_rep_on_gf_breach
 *
 * Function description:
 *
 * This function can be used to enable/disable reporting of MEAS, POS, Violation reports
 * from the receiver, based on GF breach or alwasy
 *
 * Parameters:
 * value - FASLE - on breach reporting,
 *		TRUE - always reporting.
 *
 ******************************************************************************
*/

void	gpsc_app_rep_on_gf_breach(McpU8 value)
{

	gpsc_ctrl_type*	p_zGPSCControl= gp_zGPSCControl;

	if (value == FALSE)   // enable on breach reporting
	{
		McpU8 temp = 0x01;
		gpsc_mgp_tx_write_memory(p_zGPSCControl, 0x00097394, 0x01, 0x01, &temp);
	}
	else if (value == TRUE)
	{
		McpU8 temp = 0x00;
		gpsc_mgp_tx_write_memory(p_zGPSCControl, 0x00097394, 0x01, 0x01, &temp);
	}
	else
	{
		return;
	}

	gpsc_comm_transmit(p_zGPSCControl, C_AI2_ACK_REQ);

return;
}

void gpsc_app_update_gf_config(gpsc_ctrl_type* p_zGPSCControl,gpsc_sess_specific_cfg_type* p_zSessSpecificCfg)
{
	McpU8 rep_config = 0xFF;
	rep_config=gpsc_get_gf_report_config(p_zGPSCControl->p_zSessConfig,p_zSessSpecificCfg);
	if(0==rep_config)
	{
		STATUSMSG("gpsc_app_loc_fix_stop_req: GF session currently running, Normal session Stopped..Change Report Config to report breach .\n");
		TNAVC_SetMotionMask motion_mask;
		memset(&motion_mask,0x00,sizeof(motion_mask));
		motion_mask.region_number=0;
		motion_mask.report_configuration=0;
		gpsc_app_set_motion_mask(&motion_mask,0xFFFFFFFF);
	}
	else
	{
	}

}

void gpsc_app_update_apm_config(gpsc_ctrl_type* p_zGPSCControl)
{
	McpU8 apm_config = gpsc_app_get_apm_config(p_zGPSCControl->p_zSessConfig,NULL);
	if (1 == apm_config)
	{
		TNAVC_SetAPMParams apm_param;
		apm_param.apm_control=1;
		apm_param.search_mode=1;
		apm_param.saving_options=0;
		apm_param.power_save_qc=80;
		gpsc_app_control_apm_params(&apm_param, 0xFFFFFFFF);
	}
}

/*
 ******************************************************************************
 * gpsc_register_for_assistance
 *
 * Function description:
 *
 * This function to register for assistance updates
 *
 * Parameters:
 * Src task Id of the registering task
 * Src queue ID
 * Return value:
 *  T_GPSC_result : Result
 ******************************************************************************
*/
T_GPSC_result gpsc_register_for_assistance(McpU16 AssistFlags, McpU8 SrcTaskId, McpU8 uSrcQId)
{
	gpsc_ctrl_type*	p_zGPSCControl;
	gpsc_tasks_reg_for_assist * pRegAssist;
	p_zGPSCControl = gp_zGPSCControl;
	pRegAssist = &p_zGPSCControl->zRegAssist;

	STATUSMSG("gpsc_register_for_assistance");

	if (AssistFlags & NAVC_REG_EPH)
	{
		if(pRegAssist->EPHcount < EPH_LIST_CNT)
		{
			pRegAssist->zEphList[pRegAssist->EPHcount].taskID = SrcTaskId;
			pRegAssist->zEphList[pRegAssist->EPHcount].queueID = uSrcQId;
			pRegAssist->EPHcount++;

			STATUSMSG("gpsc_register_for_assistance: EPH: Cnt:%d, TaskID:%d, Q-ID:%d",
				pRegAssist->EPHcount,
				pRegAssist->zEphList[(pRegAssist->EPHcount - 1)].taskID,
				pRegAssist->zEphList[(pRegAssist->EPHcount - 1)].queueID);
		}
		else
			STATUSMSG("gpsc_register_for_assistance: EPH: Cnt:%d >= %d, Could not register",
				pRegAssist->EPHcount,
				EPH_LIST_CNT);
	}


	if (AssistFlags & NAVC_REG_GPSTIME)
	{
		if (pRegAssist->GpsTimeCount < GPSTIME_LIST_CNT)
		{
			pRegAssist->zGpsTimeList[pRegAssist->GpsTimeCount].taskID = SrcTaskId;
			pRegAssist->zGpsTimeList[pRegAssist->GpsTimeCount].queueID = uSrcQId;
			pRegAssist->GpsTimeCount++;

			STATUSMSG("gpsc_register_for_assistance: GPS Time: Cnt:%d, TaskID:%d, Q-ID:%d",
				pRegAssist->GpsTimeCount,
				pRegAssist->zGpsTimeList[(pRegAssist->GpsTimeCount - 1)].taskID,
				pRegAssist->zGpsTimeList[(pRegAssist->GpsTimeCount- 1)].queueID);
		}
		else
			STATUSMSG("gpsc_register_for_assistance: GPS Time: Cnt:%d >= %d, Could not register",
				pRegAssist->GpsTimeCount,
				GPSTIME_LIST_CNT);
	}


return GPSC_SUCCESS;
}

/* Recon add-on to indicate start of Assistance Transaction */
void RECON_gpsc_app_assist_begin_req ()
{
   // flip assist in progress indicator. (This needs mutex!)
   RECON_set_assist_in_progress(1);

   // this sends back confirmation async via cumbersome god knows how it really works mechanism
   RECON_gpsc_app_assist_begin_req_ind ();
}

#ifdef ENABLE_INAV_ASSIST

/*
 ******************************************************************************
 * gpsc_app_inject_sensor_assistance
 *
 * Function description:
 *
 * This function to Packetize the sensor assistance message and send it to Rx
 *
 * Parameters: Handle to the Sensor Engine Library output structure
 *
 * Return value:
 *  T_GPSC_result : Result
 ******************************************************************************
*/
T_GPSC_result gpsc_app_inject_sensor_assistance(void *pData)
{
	gpsc_ctrl_type*	p_zGPSCControl;

	p_zGPSCControl = gp_zGPSCControl;

	SAPCALL(gpsc_app_inject_sensor_assistance);

	if(E_GPSC_STATE_ACTIVE == p_zGPSCControl->p_zGpscSm->e_GpscCurrState)
	{
		gpsc_mgp_inject_sensor_assistance(p_zGPSCControl, pData);
	}
	else
	{
		STATUSMSG("gpsc_app_inject_sensor_assistance: Invalid State");
	}

	SAPLEAVE(gpsc_app_inject_sensor_assistance,GPSC_SUCCESS);

	return GPSC_SUCCESS;
}

/*
 ******************************************************************************
 * gpsc_app_inject_apm_config
 *
 * Function description:
 *
 * This function to Packetize the APM configuration message to Rx
 *
 * Parameters: Handle to the Sensor Engine Library output structure
 *
 * Return value:
 *  T_GPSC_result : Result
 ******************************************************************************
*/
T_GPSC_result gpsc_app_inject_apm_config(void *pData)
{
	gpsc_ctrl_type*	p_zGPSCControl;
	SEOutput *pSensorAssist = (SEOutput *)pData;
	U8 apm_onoff;

	p_zGPSCControl = gp_zGPSCControl;

	apm_onoff = (pSensorAssist->OpFlag & SMARTAPM_VALID);
	p_zGPSCControl->p_zGPSCDynaFeatureConfig->apm_control = apm_onoff;

	SAPCALL(gpsc_app_inject_apm_config);

	if(E_GPSC_STATE_ACTIVE == p_zGPSCControl->p_zGpscSm->e_GpscCurrState)
	{
		gpsc_mgp_tx_inject_advanced_power_management(p_zGPSCControl);
	}
	else
	{
		STATUSMSG("gpsc_app_inject_apm_config: Invalid State");
	}

	SAPLEAVE(gpsc_app_inject_apm_config,GPSC_SUCCESS);

	return GPSC_SUCCESS;
}

#endif
