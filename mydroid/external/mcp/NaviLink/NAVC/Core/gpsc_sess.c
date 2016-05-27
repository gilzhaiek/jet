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
 * FileName			:	gpsc_sess.c
 *
 * Description     	:
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#include "gpsc_data.h"
#include "gpsc_mgp_cfg.h"
#include "gpsc_mgp_tx.h"
#include "gpsc_sess.h"
#include "gpsc_database.h"
#include "gpsc_msg.h"
#include "gpsc_state.h"
#include "gpsc_ext_api.h"
#include "gpsc_timer_api.h"
#include "gpsc_comm.h"
#include "navc_defs.h"
#include "mcpf_mem.h"
#include "gpsc_sequence.h"
#include "gpsc_sap.h"
#include "gpsc_hdgfilter.h"

#include "nav_log_msg.h"
#include "nav_log_cust_codes.h"
#include <utils/Log.h>


#define CONTINUOUS_TRACKING_TIME 10 /*10 seconds*/





#ifdef GPSC_ULTS_DEBUG
extern U32 q_ULTSCurrTick;
#endif

extern U8 resetFlag;

U8 db_pos_ok(gpsc_db_pos_type* p_zDBPos, DBL d_CurrentAccGpsSec, U8 u_threshold);
U8 num_of_eph(gpsc_ctrl_type* p_zGPSCControl);




/********************************************************************
 *
 * gpsc_sess_prepare
 *
 * Function description:
 *  Prepare for the new session.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the
 *  pointers of many structures accessed from different modules and
 *  functions.
 *
 *
 * Return:
 *   None
 *
 *********************************************************************
 */

void gpsc_sess_prepare(gpsc_ctrl_type *p_zGPSCControl)
{
  gpsc_state_type      *p_zGPSCState       = p_zGPSCControl->p_zGPSCState;
  gpsc_smlc_assist_type    *p_zSmlcAssist     = p_zGPSCControl->p_zSmlcAssist;

  p_zSmlcAssist->u_NumAssistAttempt = 0;
  //p_zSmlcAssist->w_InjectedFlags=0; /*MCS00100136*/
  /* Required for injecting the assistance data which has been supplied
   * before requesting for fix */
  p_zGPSCState->u_GPSCSubState = 0;
}

/********************************************************************
 *
 * gpsc_sess_pre_act
 *
 * Function description:
 *  Carry out the pre-session actions spelled out in Lm
 *  configuration.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the
 *  pointers of many structures accessed from different modules and
 *  functions.
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the
 *  pointers of many structures accessed from different modules and
 *  functions.
 *
 *
 * Return:
 *   None
 *
 *********************************************************************
 */

U8 gpsc_sess_pre_act(gpsc_ctrl_type *p_zGPSCControl)
{
  gpsc_sess_cfg_type *p_zSessConfig = p_zGPSCControl->p_zSessConfig;
  U8 u_TxRequired = FALSE;


    /* first check to see if we operate in the user plane and the pre-session actions
       requested by gpsc_cfg_type have been taken */
    if ( p_zSessConfig->q_PreSessionActionFlags )
    {


	  u_TxRequired = TRUE;

	  /* Do not delete the assistance here */
#if 0

      if (p_zSessConfig->q_PreSessionActionFlags & C_DEL_AIDING_ALMANAC)
      {
        gpsc_mgp_tx_rc_act (p_zGPSCControl, C_DELETE_ALM, 0);
      }

      if (p_zSessConfig->q_PreSessionActionFlags & C_DEL_AIDING_SVHEALTH)
      {
        /* we don't currently use SvHealth */
        gpsc_mgp_tx_rc_act(p_zGPSCControl, C_DELETE_SV_HEALTH, 0);
      }

      if (p_zSessConfig->q_PreSessionActionFlags & C_DEL_AIDING_EPHEMERIS)
      {
        /* delete all eph. */
        gpsc_mgp_tx_rc_act (p_zGPSCControl, C_DELETE_EPHEM, 0);
      }

      if (p_zSessConfig->q_PreSessionActionFlags & C_DEL_AIDING_IONO_UTC)
      {
        /* delete IONO */
        gpsc_mgp_tx_rc_act (p_zGPSCControl, C_DELETE_IONO_UTC, 0);
      }

      /* delete DGPS */
      if (p_zSessConfig->q_PreSessionActionFlags & C_DEL_AIDING_DGPS)
      {
        /*TBD*/
      }

      /* delete steering */
      if (p_zSessConfig->q_PreSessionActionFlags & C_DEL_AIDING_STEERING)
      {
        /*TBD*/
      }

      /* Delete Time */
      if (p_zSessConfig->q_PreSessionActionFlags & C_DEL_AIDING_TIME)
      {
            gpsc_mgp_tx_rc_act (p_zGPSCControl, C_DELETE_TIME, 0);
            //maximize_frunc(p_zGPSCControl);
          }

	   /* Delete Position */
      if (p_zSessConfig->q_PreSessionActionFlags & C_DEL_AIDING_POSITION)
      {
            gpsc_mgp_tx_rc_act (p_zGPSCControl, C_DELETE_POS, 0);
          }

#endif
        }

	/* Actions are taken --> Clear flag */
	p_zSessConfig->q_PreSessionActionFlags = 0;

	return u_TxRequired;
}


/********************************************************************
 *
 * gpsc_sess_get_sv_dir
 *
 * Function description:
 *  Get SV direction info.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the
 *  pointers of many structures accessed from different modules and
 *  functions.
 *
 *
 * Return:
 *   None
 *
 *********************************************************************
 */
void gpsc_sess_get_sv_dir(gpsc_ctrl_type *p_zGPSCControl)
{
#if 0 /*CUSTOM*/
  U8 u_i;
  gpsc_db_type*               p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
  gpsc_db_sv_dir_type*        p_DBSvDirection = &p_zGPSCDatabase->z_DBSvDirection;
  me_SvDirction*           p_RawSvDirection;
#endif

  /* in the future we may develop a local function to
     calculate the SV directions, but for now we'll ask
     the sensor to provide it */

  /* Clear the SV direction DB to make sure we'll be in sync with the
     sensor when the directions arrive from there. */
#if 0 /*CUSTOM*/
  p_DBSvDirection->u_num_of_sv = 0;
  for (u_i = 0; u_i < N_SV; u_i++)
  {
    p_RawSvDirection = &p_DBSvDirection->z_RawSvDirection[u_i];
	p_RawSvDirection->b_Elev = -128;
  }
#endif

  AI2TX("Request Sv Directions",AI_REQ_SV_DIR);
  gpsc_mgp_tx_req(p_zGPSCControl, AI_REQ_SV_DIR, 1);

  if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
  {
	FATAL_INIT(GPSC_TX_INITIATE_FAIL);
  }

}

/********************************************************************
 *
 * gpsc_sess_start
 *
 * Function description:
 *  Start a new session.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the
 *  pointers of many structures accessed from different modules and
 *  functions.
 *
 *
 *
 * Return:
 *   None
 *
 *********************************************************************
 */

void gpsc_sess_start(gpsc_ctrl_type *p_zGPSCControl)
{
	U16 w_NMEABitmap = 0;
	gpsc_state_type *p_zGPSCState = p_zGPSCControl->p_zGPSCState;
  	gpsc_sess_cfg_type*	p_zSessConfig = p_zGPSCControl->p_zSessConfig;
   gpsc_cfg_type*                  p_zGPSCConfig   = p_zGPSCControl->p_zGPSCConfig;
	gpsc_smlc_assist_type* p_zSmlcAssist;
	gpsc_loc_fix_qop* p_zQop;
	p_zGPSCState->u_GpsSessStarted=TRUE;

	p_zSmlcAssist = p_zGPSCControl->p_zSmlcAssist;
	p_zQop = &p_zSmlcAssist->z_Qop;


	// Update NMEA control 
	w_NMEABitmap = gpsc_sess_get_nmea_bitmap(p_zSessConfig);
   // ALOGI("+++ %s: NMEA Bitmap: [0x%x] +++\n", __FUNCTION__, w_NMEABitmap); 

  	gpsc_mgp_tx_nmea_ctrl(p_zGPSCControl, w_NMEABitmap);

   if ((p_zGPSCConfig->altitude_hold_mode == GPSC_ALT_HOLD_ALLOW_2D) ||
                (p_zGPSCConfig->altitude_hold_mode == GPSC_ALT_HOLD_MANUAL_2D))
   {       
           gpsc_inject_altitude_type zInjectAltitude;

           //inject default altitude from config file
           zInjectAltitude.x_Altitude = p_zGPSCConfig->altitude_estimate;
           zInjectAltitude.w_AltitudeUnc = p_zGPSCConfig->altitude_unc;
           zInjectAltitude.u_ForceFlag = 1;
           
           gpsc_mgp_inject_altitude(p_zGPSCControl, &zInjectAltitude);
   }

	// GPS Receiver ON
	gpsc_mgp_tx_rc_act (p_zGPSCControl, C_RECEIVER_ON, 0);

}

/********************************************************************
 *
 * gpsc_sess_end
 *
 * Function description:
 *  Properly set gpsc_state_type variables to reflect the fact a session
 *  has come to an end, and check if this is also the end of the
 *  entire collect, if so properly set gpsc_state_type variables to reflect
 *  that.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the
 *  pointers of many structures accessed from different modules and
 *  functions.
 *
 *
 * Return:
 *   None
 *
 *********************************************************************
 */

void gpsc_sess_end(gpsc_ctrl_type *p_zGPSCControl)
{
	gpsc_state_type *p_zGPSCState = p_zGPSCControl->p_zGPSCState;
	gpsc_session_status_type *p_zSessionStatus = &p_zGPSCState->z_SessionStatus;
	gpsc_sess_cfg_type*	p_zSessConfig = p_zGPSCControl->p_zSessConfig;
	gpsc_assist_wishlist_type* p_zGpsAssisWishlist = p_zGPSCControl->p_zGpsAssisWishlist;

    gpsc_smlc_assist_type*	p_zSmlcAssist	= p_zGPSCControl->p_zSmlcAssist;

    STATUSMSG("Deleting All TagId");
	gpsc_timer_stop(p_zGPSCControl, C_TIMER_PERIODIC_REPORTING);
	gpsc_timer_stop(p_zGPSCControl, C_TIMER_WAIT_LOCREQ);
	gpsc_session_del_node(p_zSessConfig, C_SESSION_INVALID);


	STATUSMSG("Status: In session_end");
    /* start Just to stop injecteing assistance before time */
	p_zGPSCState->u_TimeInjected = FALSE;
	p_zGPSCState->u_GpsSessStarted=FALSE;
	/* end */
    /*End Added to reset time between session*/
    p_zSmlcAssist->u_SvCounterSteer=0;
    p_zSmlcAssist->u_NumSvToInj=0;
	p_zSessionStatus->u_WishlistBuilt = 0;
	p_zSessionStatus->u_BuildWishlistLater=0;
	/* clear some history */
	p_zGpsAssisWishlist->q_LastNeedEphList = 0;

	/* Check if SUPL_END needs to be issued here */
	if(p_zGPSCControl->p_zSuplStatus.u_GetAssistSUPLRequest == TRUE)
	{
		STATUSMSG("Status: SUPL session is still active or SUPL session failed !!!!");
	}
	/* Reinitilize the session fix counter */
	gpsc_sess_init_counters(p_zGPSCControl);

	gpsc_sess_collect_end(p_zGPSCControl);

}


/********************************************************************
 *
 * gpsc_sess_collect_end
 *
 * Function description:
 *  Take proper actions to end an entire collect
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the
 *  pointers of many structures accessed from different modules and
 *  functions.
 *
 *
 * Return:
 *   None
 *
 *********************************************************************
 */

void gpsc_sess_collect_end(gpsc_ctrl_type *p_zGPSCControl)
{
  gpsc_state_type  *p_zGPSCState = p_zGPSCControl->p_zGPSCState;
  gpsc_sess_cfg_type*	p_zSessConfig = p_zGPSCControl->p_zSessConfig;
  gpsc_smlc_assist_type*  p_zSmlcAssist  = p_zGPSCControl->p_zSmlcAssist;
  gpsc_db_type *p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
  gpsc_db_gps_clk_type*  p_DBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;
  gpsc_custom_struct*		p_zGPSCCustom = p_zGPSCControl->p_zCustomStruct;
  gpsc_acq_assist_type*        p_zSmlcAcquisAssist = &p_zSmlcAssist->z_AcquisAssist;
  gpsc_db_gps_clk_type*   p_zAAGpsClock = &p_zSmlcAcquisAssist->p_zAAGpsClock;
  gpsc_loc_fix_qop* p_zQop = &p_zGPSCControl->p_zSmlcAssist->z_Qop;
  T_GPSC_result result = 0;

  // Added this to write GPS data to NVS files immediately after first fix
  result = gpsc_app_nvs_file_req();
  if (result == GPSC_FAIL)
	  ALOGI("+++ %s: Failed to save GPS data to NVS files +++\n", __FUNCTION__);

  p_zGPSCState->z_SessionStatus.u_WishlistBuilt = 0;

  /* Location request serviced */
  p_zGPSCState->u_LocationRequestPending = FALSE;
  p_zGPSCState->u_firstfix = FALSE;
  p_zSessConfig->q_FCount = GPSC_INVALID_FCOUNT;

  p_zSessConfig->q_LastFCount = GPSC_INVALID_FCOUNT;
  p_zSessConfig->q_LastFCountMeas = GPSC_INVALID_FCOUNT;
  p_zSessConfig->q_FCountMeas = GPSC_INVALID_FCOUNT;

  p_zGPSCCustom->custom_position_available_flag = 0;

  p_zSmlcAssist->w_BestRef = 0;
  p_zSmlcAssist->w_NDiffs = 0;
  p_zAAGpsClock->u_Valid = FALSE;
  p_DBGpsClock->u_NumInvalids = 0;

  p_zQop->u_TimeoutCepInjected = FALSE;

  p_zGPSCState->u_CountInvalidFix = UNDEFINED_U8; /* to reset the count for invalid fix
                                                     to re request assistance */

  p_zGPSCState->u_ClkRptforWlPending = FALSE;    
  p_zSmlcAssist->w_AvailabilityFlags &=  ~C_REF_GPSTIME_AVAIL;

  p_zSmlcAssist->z_Qop.u_HorizontalAccuracy = C_ACCURACY_UNKNOWN;
  p_zSmlcAssist->z_Qop.w_MaxResponseTime = C_TIMEOUT_UNKNOWN;
  if (p_zSessConfig->u_PosCompEntity == C_SMLC_COMPUTED)
  {
     p_zGPSCDatabase->z_DBGpsMeas.z_MeasStatReport.u_Valid = FALSE;
  }
  gpsc_init_result_buffer(p_zGPSCControl);
  gpsc_HdgFiltInit();

#ifdef LARGE_UPDATE_RATE
  /* This controls the transition to READY state */
  p_zSessConfig->q_SleepDuration = 0;
#endif
	p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_LOCATION_COMPLETE;
	if (!resetFlag)
	{
	  // put receiver to idle 
	  gpsc_inject_gps_idle(p_zGPSCControl);
	}

	gpsc_timer_stop(p_zGPSCControl, C_TIMER_MEAS_TRACKING);
	gpsc_timer_stop(p_zGPSCControl, C_TIMER_EXP_REQ_CLOCK);
	gpsc_timer_stop(p_zGPSCControl, C_TIMER_ALMANAC_AYNC);
}

/********************************************************************
 *
 * gpsc_sess_coll_abort
 *
 * Function description:
 *
 *  Reset all the collect related parameters.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the
 *  pointers of many structures accessed from different modules and
 *  functions.
 *
 *  x_abort_reason: LSB defined as in GSM 09.31
 *                  with 00001100 to 1111111 partially defined as
 *                  11111101: fix time out -- used only by PosLocResult
 *                  x1111110: MGP position event report time out.
 *                  x1111111: SMLC comm. timeout.
 *                    where x is 0 for aborting a session, 1 for a collect
 *                  -1: don't send POSLOC-ABORT to SIA
 *
 * Return:
 *  None
 *
 *********************************************************************
 */

void gpsc_sess_coll_abort(gpsc_ctrl_type *p_zGPSCControl,
                           S16 x_abort_reason)
{
   if (x_abort_reason == C_ABORT_CAUSE_GPS_TO)
      STATUSMSG("Status: Timeout waiting for GPS");
   else if (x_abort_reason == C_ABORT_CAUSE_SMLC_TO )
      STATUSMSG("Status: Timeout waiting for SMLC. ");
   else if (x_abort_reason == C_ABORT_CAUSE_TERMINATED )
      STATUSMSG("Status: Session aborted by GPSM");
//  gpsc_sess_collect_end(p_zGPSCControl);
    gpsc_sess_end(p_zGPSCControl);

}

#ifndef UNLIMITED_EPHEMERIS_STORAGE
/*
 ******************************************************************************
 * gpsc_delete_invisible_sv_ephemeris()
 *
 * Function description:
 *
 * Delete the ephemeris of SV's that are below the Horizon (or Horizon Mask)
 * from the Sensor Memory and invalidate them in the Database.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/
void gpsc_delete_invisible_sv_ephemeris(gpsc_ctrl_type * p_zGPSCControl)
{
  gpsc_session_status_type * p_zSessionStatus = &p_zGPSCControl->p_zGPSCState->z_SessionStatus;
  gpsc_db_sv_dir_type * p_zDBSvDirection = &p_zGPSCControl->p_zGPSCDatabase->z_DBSvDirection;
  gpsc_db_type * p_zGPSCDatabase =  p_zGPSCControl->p_zGPSCDatabase;
  U8 u_i;


      if (p_zSessionStatus->u_AcqAsstListedSVs || p_zDBSvDirection->u_num_of_sv)
      { /* Delete Unnecessary Ephemeris */

        for ( u_i=0; u_i < N_SV; u_i++)
        {
          me_SvDirection * p_zRawSvDirection = &p_zDBSvDirection->z_RawSvDirection[u_i];

          /* Only consider deletion of Ephemeris that are in the Sensor */
          if (!p_zGPSCDatabase->z_DBEphemeris[u_i].u_Valid) continue;

          if (p_zDBSvDirection->u_num_of_sv)
          {/* Check the SV Elevation */
            if (p_zRawSvDirection->b_Elev != -128
              && p_zRawSvDirection->b_Elev * C_LSB_ELEV_MASK > 5.0)
            {/* This SV is above the Horizon, do not delete it */
              continue;
            }
          }
          else
          {
            if ((p_zSessionStatus->u_AcqAsstListedSVs & (1<<u_i)) != 0)
            { /* This SV is on the Acquisition Assist List, do not delete it */
              continue;
            }
          }

          /* At this point, this SV is not Visible and is wasting Storage Space*/
          gpsc_mgp_tx_rc_act(p_zGPSCControl, C_DELETE_EPHEM, (U8)(u_i+1));

        } /* End Loop */
      }
}
#endif
T_GPSC_result gpsc_sess_req_clock_to_get_assist(gpsc_ctrl_type*  p_zGPSCControl)
{
	gpsc_state_type*    p_zGPSCState =  p_zGPSCControl->p_zGPSCState;
	STATUSMSG("gpsc_sess_req_clock_to_get_assistance: Entering \n");
	gpsc_mgp_tx_req(p_zGPSCControl, AI_REQ_CLOCK_REP, 0);
	if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
	{
		STATUSMSG("gpsc_sess_req_clock_to_get_assistance: GPSC_TX_INITIATE_FAIL - FATAL_ERROR");
		return GPSC_TX_INITIATE_FAIL;
	}
	p_zGPSCState->u_ClkRptforWlPending = TRUE;
	STATUSMSG("gpsc_sess_req_clock_to_get_assistance: Exiting \n");
	return GPSC_SUCCESS;
}

/********************************************************************
 *
 * GetGspAssisWishlist
 *
 * Function description:
 *   Determine what GPS assistance data is needed from SMLC.
 *
 *   Steps:
 *
 *     1. If LM has no good knowledge of time (i.e. time database
 *        invalid, database not updated for 5 seconds, week number
 *        unknown, or uncertainty greater than 2 seconds), request
 *        everything that is supported. Otherwise, go throught the
 *        following steps.
 *
 *     2. Request reference location unless database contains
 *        position whose uncertainty is less than 15 kilometers.
 *
 *     3. Request almanac and real time integrity unless: in
 *        database, almanac health for all 32 SVs is known, and all
 *        the existing SVs have almanac that is young enough
 *        (TOA less than one week).
 *
 *     4. Request ephemeris unless: database position uncertainty
 *        is less than 15 kilometers AND, there are at least 6 SVs
 *        higher than 5 degrees in elevation, and their ephemeris TOE
 *        are younger than 2 hours.
 *
 *     5. Request IONO unless: database IONO's update time is more
 *        than one week ago.
 *
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the
 *  pointers of many structures accessed from different modules and
 *  functions.
 *
 *
 * Return:
 *   0: Nothing to request
 *   1: Request with LocReq
 *   2: Request with AssistanceData
 *
 *********************************************************************
 */
U8 gpsc_sess_build_wishlist(gpsc_ctrl_type *p_zGPSCControl)
{
  gpsc_assist_wishlist_type*  p_zGpsAssisWishlist =
                                   p_zGPSCControl->p_zGpsAssisWishlist;
  gpsc_sess_cfg_type*	p_zSessConfig = p_zGPSCControl->p_zSessConfig;
  gpsc_db_type*            p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
  gpsc_db_gps_clk_type*    p_zDBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;
  gpsc_db_alm_type*        p_zDBAlmanac;
  pe_RawAlmanac*         p_zDBRawAlmanac;
  gpsc_db_sv_dir_type*     p_zDBSvDirection = &p_zGPSCDatabase->z_DBSvDirection;
  me_SvDirection*        p_zRawSvDirection;
  gpsc_db_eph_type*        p_zDBEphemeris;
  pe_RawSF1Param*        p_RawSF1Param;
  gpsc_db_iono_type*       p_zDBIono = &p_zGPSCDatabase->z_DBIono;
  gpsc_db_health_type*     p_zDBHealth = &p_zGPSCDatabase->z_DBHealth;
  gpsc_db_pos_type*        p_zDBPos = &p_zGPSCDatabase->z_DBPos;
  gpsc_state_type*		  p_zGPSCState    = p_zGPSCControl->p_zGPSCState;
  gpsc_cfg_type*			p_zGPSCConfig	= p_zGPSCControl->p_zGPSCConfig;


  gpsc_smlc_assist_type*  p_zSmlcAssist  = p_zGPSCControl->p_zSmlcAssist;
    gpsc_loc_fix_qop* p_zQop = &p_zGPSCControl->p_zSmlcAssist->z_Qop;

     gpsc_loc_fix_qop zQop;
gpsc_custom_struct* p_zCustom;
p_zCustom = p_zGPSCControl->p_zCustomStruct;
  FLT f_data_age_sec;
  DBL d_CurrentAccGpsSec = 0.0;
  DBL d_data_age_sec = 0.0;
  U8 u_i = 0;
  U8 u_NumSvHealthKnown = 0, u_NumSvHealthBad = 0, u_NumSvAlmYoung = 0,
     u_NumSvNonExist = 0, u_ReqTime = FALSE, u_ReqAlm = FALSE, u_ReqEph = FALSE,
     u_ReqIono = FALSE, u_ReqRefLoc = FALSE, u_SvHealth = 0;
  U8 u_GoodEphCount =0;



  STATUSMSG("Status: In build_wishlist");

  p_zGPSCControl->p_zGPSCState->z_SessionStatus.u_WishlistBuilt = TRUE;
  p_zGpsAssisWishlist->w_Wishlist = 0; /* clear all wishlist bits */
  p_zGpsAssisWishlist->q_GoodEphList = 0; /* clear all good eph. index */
	p_zGpsAssisWishlist->q_NeedEphList = 0xFFFFFFFF; /* clear all need eph. index */
  p_zGPSCControl->p_zGPSCState->injectSvdirectionColdstart = FALSE;

  if(p_zGPSCControl->p_zGPSCState->u_SensorOpMode == C_RC_ON) {
  p_zGPSCControl->p_zGPSCState->injectedCepinfoCount++;
  STATUSMSG("Status: In build_wishlist p_zGPSCControl->p_zGPSCState->injectedCepinfoCount  %d",p_zGPSCControl->p_zGPSCState->injectedCepinfoCount);
  }
  STATUSMSG("Status: In build_wishlist p_zGPSCControl->p_zGPSCState  %d",p_zGPSCControl->p_zGPSCState->injectedCepinfoCount);


  /*************** Handle the MS-COMPUTED mode ****************************/

  if ( p_zSessConfig->u_PosCompEntity == C_MS_COMPUTED )
  {
    /* if time is totally unknown */

    if ( !p_zDBGpsClock->u_Valid ||
       (p_zDBGpsClock->w_GpsWeek == C_GPS_WEEK_UNKNOWN)
       )
    {

      /*p_zGpsAssisWishlist->w_Wishlist = GPSC_REQ_TIME;
       *return 2;
       */ /** 2: request time with AssistanceData */

      p_zGpsAssisWishlist->w_Wishlist =  GPSC_REQ_TIME |
                                        GPSC_REQ_LOC |
                                        GPSC_REQ_NAV |
                                        GPSC_REQ_ALMANAC |
                                        GPSC_REQ_IONO |
                                        GPSC_REQ_RTI |
                                        GPSC_REQ_DGPS|
                                        GPSC_REQ_AA;
#ifdef REPORT_EPHEMERIS_REQUEST_DECISION
      gpsc_test_txt_disp_unfiltered(p_zGPSCControl, "Requesting Ephemeris");
#endif

      /* to clear the almanac from the wishlist */
	  p_zGpsAssisWishlist->w_Wishlist &=  ~(p_zGPSCConfig->block_almanac);
	  STATUSMSG("Status: COLD START");
	  zQop.u_HorizontalAccuracy = 30;
          zQop.w_MaxResponseTime = 40;
	//  gpsc_mgp_tx_inject_timeout_cep_info(p_zGPSCControl, &zQop);
    STATUSMSG(" ===>>> %s - %d: u_TimeoutCepInjected = %d", __func__, __LINE__, p_zQop->u_TimeoutCepInjected);
    if ( p_zQop->u_TimeoutCepInjected != TRUE){
	if(p_zGPSCControl->p_zGPSCState->injectedCepinfoCount == 1) {
	p_zGPSCControl->p_zGPSCState->injectSvdirectionColdstart = TRUE;
	gpsc_inject_cepinfo_start_mode(p_zGPSCControl,38,80,30,150);
	//gpsc_inject_cepinfo_start_mode(p_zGPSCControl,40,80,1,1);
    }}
        else
              STATUSMSG("gpsc_sess_build_wishlist: ingnoring cep injection");
	  return 1;

    }
/*
    if ( (p_zDBPos->d_ToaSec== 0) && (p_zSmlcAssist->w_InjectedFlags & C_REFLOC_AVAIL) )
	{
     STATUSMSG("Positon not yet received");
     p_zSmlcAssist->w_InjectedFlags &= ~C_REFLOC_AVAIL;
	 p_zSessionStatus->u_BuildWishlistLater=1;
     return FALSE;
	}
*/
    /* if it ever reaches here, it means time is not totally unknown */
    /* our time esitimate is : */
    d_CurrentAccGpsSec = FullGpsMs(p_zDBGpsClock->w_GpsWeek,
                                 p_zDBGpsClock->q_GpsMs) * 0.001;


	/* Check if LM has knowledge of GPS time unc. is lesser
       than 2 seconds, dont request time then (Dont expect it to ever happen  */
    u_ReqTime = TRUE;
	if ( p_zDBGpsClock->f_TimeUnc < 2000.0f )
    {
      STATUSMSG("Good Time with unc %f",p_zDBGpsClock->f_TimeUnc);
		u_ReqTime = FALSE;
    }
	else
	{
		p_zGPSCState->u_TimeInjected = FALSE; /* to inject the time */
		STATUSMSG("Bad Time with unc %f",p_zDBGpsClock->f_TimeUnc);
	}

    /********  Check to see if initial position should be requested *****/
    if ( gpsc_db_is_pos_ok( p_zDBPos, d_CurrentAccGpsSec, (U32)C_GOOD_INIT_POS_UNC) && (p_zCustom->custom_assist_availability_flag & C_REFLOC_AVAIL))
    {
      u_ReqRefLoc = FALSE; /* request reference location, unless condition
                             for not to request is met below */
    }

    else{
		u_ReqRefLoc = TRUE;
    }

    /****** Check to see if almanac needs to be requested *****************/

    u_ReqAlm = TRUE; /* request almanac, unless the condition for not
                        to request is met below */

    if (p_zDBHealth->d_UpdateTimeSec > 0.0)  /* valid almanac health
                                                available */
    {
      u_NumSvHealthKnown = 0; /* num of SVs whose health is known */
      u_NumSvAlmYoung = 0;    /* num of SVs whose alm is young enough */
      u_NumSvNonExist = 0;    /* num of SVs that don't exist */
      u_NumSvHealthBad = 0;   /* num of SVs that exist but bad */

      /* check to see how many SVs whose almanac health is known */
      for (u_i=0; u_i<N_SV; u_i++)
      {
        if ( p_zDBHealth->u_AlmHealth[u_i] != C_HEALTH_UNKNOWN )
        {
          u_NumSvHealthKnown++;
        }
      }

      /* almanac health for all 32 SVs known, check to see the age of almanac
         data */
      if (u_NumSvHealthKnown == N_SV )
      {
        /* check almanac list: see how many SVs have almanac are
           young enough (less than a week), and how many not existing */
        for (u_i = 0; u_i < N_SV; u_i++)
        {
          p_zDBAlmanac = &p_zGPSCDatabase->z_DBAlmanac[u_i];
          p_zDBRawAlmanac = &p_zDBAlmanac->z_RawAlmanac;

          if ( p_zDBHealth->u_AlmHealth[u_i] == C_HEALTH_GOOD )
          {
            if (p_zDBAlmanac->u_Valid)
            {
              /* Take into account TUnc, the youngest possible time minus Toa */
              d_data_age_sec = d_CurrentAccGpsSec +
                               p_zDBGpsClock->f_TimeUnc * 0.001f -
					                     p_zDBRawAlmanac->d_FullToa;

              if ( ( (d_data_age_sec > 0) && ( d_data_age_sec < C_GOOD_ALM_AGE_SEC) ) ||
			             ( (d_data_age_sec < 0) && ( (0 - d_data_age_sec) < C_GOOD_ALM_AGE_SEC) ) )
			        {
						u_NumSvAlmYoung++;
			        }
			        else
			        {
				        f_data_age_sec = (FLT)d_data_age_sec;
						STATUSMSG("Status: Disq alm Sv%d: age=%f", u_i+1, f_data_age_sec);
			        }
     	      }
          }

          else if ( p_zDBHealth->u_AlmHealth[u_i] == C_HEALTH_BAD )
          {
	          u_NumSvHealthBad++;
          }
            else if ( p_zDBHealth->u_AlmHealth[u_i] == C_HEALTH_NOEXIST )
          {
	          u_NumSvNonExist++;
          }
		}
      }
      else
        STATUSMSG("Status: ReqAlm: some SV health unknown");

      if (u_NumSvAlmYoung == (N_SV - u_NumSvNonExist - u_NumSvHealthBad ) )
      {
        STATUSMSG("Status: NOT Requesting Alm: GY=%d, NE=%d, Bd=%d",
		                u_NumSvAlmYoung, u_NumSvNonExist, u_NumSvHealthBad);
		u_ReqAlm = FALSE; /* All existing SVs have young enough
                             almanac, don't request almanac */
      }
      else
      {
        STATUSMSG("Status: ReqAlm: GY=%d, NE=%d, Bd=%d",
		                u_NumSvAlmYoung, u_NumSvNonExist, u_NumSvHealthBad);
      }
    }
    else
      STATUSMSG("Status: ReqAlm: no health data");

    /** Check to see if ephemeris assistance is required: ************/

    u_ReqEph = TRUE; /* request ephemeris, unless condition for not to
                        request is met */

    if ( gpsc_db_is_pos_ok( p_zDBPos, d_CurrentAccGpsSec,(U32)C_GGOD_INIT_POS_UNC_EPH))
	{
      /* Only when initial location info. is good and there are SV directions,
         exists the chance that we may not have to request ephemeris */
      /* clear injected flag */
      p_zSmlcAssist->w_InjectedFlags &= ~C_REFLOC_AVAIL;

      for ( u_i=0; u_i < N_SV; u_i++)
	  {
        p_zRawSvDirection = &p_zDBSvDirection->z_RawSvDirection[u_i];
        p_zDBEphemeris = &p_zGPSCDatabase->z_DBEphemeris[u_i];
        p_RawSF1Param = &p_zDBEphemeris->z_RawSF1Param;
		//u_SvHealth = p_zDBHealth->u_AlmHealth[u_i]; //wrong value
		u_SvHealth         = p_RawSF1Param->u_Health;

				STATUSMSG("Status: svid = %d u_SvHealth = %d \n",u_i+1,u_SvHealth);
        /* On behalf of non-existing or non-healthy SVs don't trigger ephemeris request */
        if (( u_SvHealth == C_HEALTH_NOEXIST ) || ( u_SvHealth == C_HEALTH_BAD ))
          continue;

				STATUSMSG("Status: svid [%d] -> b_Elev = %d \n",u_i+1,p_zRawSvDirection->b_Elev);
				STATUSMSG("Status: b_Elev = %d \n",p_zRawSvDirection->b_Elev);
          /* found Sv with PRN=u_i+1 having elevation above 5 degrees,
             check to see
             (1) if we have a valid eph. in database for this SV
             (2) if this data possibly too old
             (3) if this data definately too young, sanity check  */

          STATUSMSG("Status: SvId=%d, elev=%f", u_i+1, p_zRawSvDirection->b_Elev*C_LSB_ELEV_MASK);
			STATUSMSG("Status: p_zDBEphemeris->z_RawEphemeris.u_Prn = %d", p_zDBEphemeris->z_RawEphemeris.u_Prn);
			//if (p_zDBEphemeris->u_Valid)
			if (p_zDBEphemeris->z_RawEphemeris.u_Prn > 0)
		  {
            /* taken into account TUnc, the youngest possibletime minus Toe */
            d_data_age_sec = d_CurrentAccGpsSec +
                             p_zDBGpsClock->f_TimeUnc * 0.001f -
                             p_RawSF1Param->d_FullToe;
	        f_data_age_sec = (FLT)d_data_age_sec;
			STATUSMSG("Status: SvId=%d, eph age=%f minutes", u_i+1, f_data_age_sec/60.0f);

            if (( (d_data_age_sec > 0) && ( d_data_age_sec < C_GOOD_EPH_AGE_SEC ) ) ||
			      ( (d_data_age_sec < 0) && ( (0 - d_data_age_sec) < C_GOOD_EPH_AGE_SEC ) ) )
			{
			  STATUSMSG("Status: SvId = %d, has good eph \n", u_i+1);
              p_zGpsAssisWishlist->q_GoodEphList |= 1L << u_i;
			  p_zGpsAssisWishlist->q_NeedEphList &= 0L << u_i;
			  u_GoodEphCount++;
			}
			else
			{
			  f_data_age_sec = (FLT)d_data_age_sec;
		      p_zGpsAssisWishlist->q_NeedEphList |= 1L << u_i;

              STATUSMSG("Status: Need eph: SvId=%d, elev=%f, age =%f old", u_i+1,
                          p_zRawSvDirection->b_Elev * C_LSB_ELEV_MASK, f_data_age_sec/60.0f);
			}
     	   }
		   else
          /* this in-view high-enough SV doesn't have eph. in database,
		         reason to trigger request for assistance */
		   {
		     p_zGpsAssisWishlist->q_NeedEphList |= 1L << u_i;
             STATUSMSG("Status: Need eph: Sv %d good dir., no eph in DB", u_i+1);
		   }

	  }
      STATUSMSG("Status: NeedList : %X, GoodList:%X, GoodCount:%d",
		  p_zGpsAssisWishlist->q_NeedEphList,p_zGpsAssisWishlist->q_GoodEphList,u_GoodEphCount);

	}
	else
	{
		STATUSMSG("Status: Req. Eph.: Pos. Bad or available SV dirs %d < C_MIN_EPH_SV or no Eph"
			,p_zDBSvDirection->u_num_of_sv);
	}

    /* check if we need to request IONO from SMLC */

    u_ReqIono = TRUE; /* request IONO, unless the condition is met */

    d_data_age_sec = d_CurrentAccGpsSec + p_zDBGpsClock->f_TimeUnc * 0.001f -
	                 p_zDBIono->d_UpdateTimeSec;
    f_data_age_sec = (FLT)d_data_age_sec;

    if ( (p_zDBIono->d_UpdateTimeSec != 0) && ( d_data_age_sec < C_GOOD_IONO_AGE_SEC ) )
    {
		STATUSMSG("Status: Valid Iono Record");
		u_ReqIono = FALSE; /* Iono updated less than C_GOOD_IONO_AGE_HOUR
                            (7 days) ago, no need to request new ones */
    }
	else if ( p_zDBIono->d_UpdateTimeSec == 0)
	{
		STATUSMSG("Status: ReqIono - no Record");
 	}
	else
	{
		STATUSMSG("Status: ReqIono: age=%f", f_data_age_sec);
	}

    /* Set up appropriate wishlist flags */
    if (u_ReqTime)
    {
      p_zGpsAssisWishlist->w_Wishlist = GPSC_REQ_TIME;
    }

    if (u_ReqRefLoc)
    {
      p_zGpsAssisWishlist->w_Wishlist |= GPSC_REQ_LOC;
    }

    if (u_ReqEph)
    {
      STATUSMSG("Status: Requesting Ephemeris");
      p_zGpsAssisWishlist->w_Wishlist |= GPSC_REQ_NAV;
      STATUSMSG("Status: WARM START");
       zQop.u_HorizontalAccuracy = 30;
          zQop.w_MaxResponseTime = 40;
	//  gpsc_mgp_tx_inject_timeout_cep_info(p_zGPSCControl, &zQop);
    STATUSMSG(" ===>>> %s - %d: u_TimeoutCepInjected = %d", __func__, __LINE__, p_zQop->u_TimeoutCepInjected);
    if ( p_zQop->u_TimeoutCepInjected != TRUE){
     if(p_zGPSCControl->p_zGPSCState->injectedCepinfoCount == 1) {
     gpsc_inject_cepinfo_start_mode(p_zGPSCControl,35,65,40,70);
     }}
    else
              STATUSMSG("gpsc_sess_build_wishlist: ingnoring cep injection");
    }
    else
    {
      STATUSMSG("Status: NOT Requesting Ephemeris");
      STATUSMSG("Status: HOT START");
      STATUSMSG(" ===>>> %s - %d: u_TimeoutCepInjected = %d", __func__, __LINE__, p_zQop->u_TimeoutCepInjected);
      if ( p_zQop->u_TimeoutCepInjected != TRUE){

      if(p_zGPSCControl->p_zGPSCState->injectedCepinfoCount == 1) {
        gpsc_inject_cepinfo_start_mode(p_zGPSCControl,3,7,40,70);
	  }
	  else {
	  STATUSMSG("Status: HOT START else");
	 zQop.u_HorizontalAccuracy = 20;
         zQop.w_MaxResponseTime = 8;
	gpsc_mgp_tx_inject_timeout_cep_info(p_zGPSCControl, &zQop);
        if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
        {
          FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
          GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
          return GPSC_TX_INITIATE_FAIL;
        }
     }}
    else
        STATUSMSG("gpsc_sess_build_wishlist: ingnoring cep injection");
    }

    if (u_ReqAlm)
    {
      /* whenever requesting almanac, also request RTI */
      p_zGpsAssisWishlist->w_Wishlist |= GPSC_REQ_ALMANAC | GPSC_REQ_RTI;
    }

    if (u_ReqIono)
    {
        /* no Iono or Iono older than C_GOOD_IONO_AGE_HOUR (7 days), request
           new ones */
        p_zGpsAssisWishlist->w_Wishlist |= GPSC_REQ_IONO;
    }

		/* for dgps support add dgps into wishlist with out any condition 11/09/06 --Raghavendra*/
     p_zGpsAssisWishlist->w_Wishlist |=  GPSC_REQ_DGPS;

	  /**** if we are not requesting almanac (which would force us to request
	        RTI), let's see if there is reason to request RTI */
    d_data_age_sec = d_CurrentAccGpsSec + p_zDBGpsClock->f_TimeUnc * 0.001f -
	                   p_zDBHealth->d_UpdateTimeSec;
    f_data_age_sec = (FLT)d_data_age_sec;

    if ( !u_ReqAlm && (p_zDBHealth->d_UpdateTimeSec != 0) &&
	     ( d_data_age_sec  > C_GOOD_HEALTH_AGE_SEC ) )
    {
	  p_zGpsAssisWishlist->w_Wishlist |= GPSC_REQ_RTI;
      STATUSMSG("Status: ReqRti-NotReqAlm-HlthOld: %f", f_data_age_sec);
    }

    /* Request Acquisition Assistance in msbased mode.
       Azimuth and Elevation will be extracted to help
       decide which Ephemeris to send down to the Sensor.
       every mode */

		p_zGpsAssisWishlist->w_Wishlist |=  GPSC_REQ_AA;

  } /* end if-MS-Computed */

  else if ( p_zSessConfig->u_PosCompEntity == C_SMLC_COMPUTED)
  {
   	/* Check if LM has knowledge of GPS time unc. is lesser
       than 2 seconds, dont request time then (Dont expect it to ever happen  */
    if ( p_zDBGpsClock->f_TimeUnc > 2000.0f )
        p_zGpsAssisWishlist->w_Wishlist |=GPSC_REQ_TIME;

		p_zGpsAssisWishlist->w_Wishlist |=  GPSC_REQ_AA;
        p_zGpsAssisWishlist->w_Wishlist |=  GPSC_REQ_LOC;
        p_zGpsAssisWishlist->w_Wishlist |=  GPSC_REQ_NAV;
  }

#ifndef UNLIMITED_EPHEMERIS_STORAGE
  /* Delete the Ephemeris that is "Inivisible" before requesting
   * and Ephemeris update. This will prevent Ephemeris Storage overflow.
   */
//  gpsc_delete_invisible_sv_ephemeris(p_zGPSCControl);
#endif

  if (p_zGpsAssisWishlist->w_Wishlist)
  {
	/* For 3GPP test condition, almanac needs to be cleared from wishlist */
 	p_zGpsAssisWishlist->w_Wishlist &=  ~(p_zGPSCConfig->block_almanac);
    return TRUE;
  }
  else
  {
    return FALSE;
  }

}


/*
 ******************************************************************************
 * gpsc_init_sess_counters
 *
 * Function description:
 *
 * Initialize the counters used in the session.
 *
 * Parameters:
 *	None
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/
void gpsc_sess_init_counters(gpsc_ctrl_type *p_zGPSCControl)
{
	gpsc_state_type      *p_zGPSCState       = p_zGPSCControl->p_zGPSCState;
	gpsc_session_status_type *p_zSessionStatus  = &p_zGPSCState->z_SessionStatus;

	gpsc_sess_cfg_type*	p_zSessConfig = p_zGPSCControl->p_zSessConfig;

	p_zSessionStatus->w_NumUnRptFixEvt = 0;

	p_zSessionStatus->w_NumUnRptMsrEvt = 0;

	p_zSessConfig->w_AvgReportPeriod = 1;
	p_zSessConfig->w_ActionTimeUnc = (U16)-1;
	p_zSessConfig->w_ActionPosUnc = (U16) -1;

	/* Added initialization of SUPL status parameters */
	p_zGPSCControl->p_zSuplStatus.u_GetAssistSUPLRequest = FALSE;
	p_zGPSCControl->p_zSuplStatus.u_GetAssistSUPLRequestPending = FALSE;
	p_zGPSCControl->p_zSuplStatus.u_SessRequestCount = 0;

}
U8 gpsc_session_get_all_nodes (gpsc_sess_cfg_type*    p_zSessConfig,U32 *w_SessionTagId, S8 *s_NoSessions)
{
	gpsc_sess_specific_cfg_type *p_zSessSpecificCfg, *p_zPrevSessSpecificCfg;
	gpsc_ctrl_type*	p_zGPSCControl;
	p_zGPSCControl = gp_zGPSCControl;
	p_zPrevSessSpecificCfg = NULL;
	p_zSessSpecificCfg = NULL;
        S8 i= 0;
	*s_NoSessions = 0;
	p_zSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;
	while(p_zSessSpecificCfg != NULL)
	{
		  w_SessionTagId[i++] = p_zSessSpecificCfg->w_SessionTagId;
        STATUSMSG("w_SessionTagId = %d", p_zSessSpecificCfg->w_SessionTagId);
		  p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;
	}
        *s_NoSessions = i;
    STATUSMSG("s_NoSessions = %d", i);
	return 0;
}

U8 gpsc_session_is_nw_session_running (gpsc_sess_cfg_type*    p_zSessConfig)
{
    gpsc_sess_specific_cfg_type *p_zSessSpecificCfg;
    p_zSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;
    while(p_zSessSpecificCfg != NULL)
    {
        STATUSMSG("w_typeofsession = %d", p_zSessSpecificCfg->w_typeofsession);
        if(p_zSessSpecificCfg->w_typeofsession==GPSC_NW_SESSION_NI)
            return TRUE;
        p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;
    }
    return FALSE;
}
/* adding support API's for hybrid mode in GPS5350 */

/********************************************************************
 *
 * gpsc_session_get_node
 *
 * Function description:
 *  Get a node with TagId. Check for Local or existing tag_id. In both cases we are
 *  overwriting node.
 *
 * Parameters:
 *
 *  gpsc_sess_specific_cfg_type: pointer to session specific information
 *  used for multi session hybrid mode in GPS5350
 *
 * Return:
 *   None
 *
 *********************************************************************
 */

gpsc_sess_specific_cfg_type* gpsc_session_get_node (gpsc_sess_cfg_type*	p_zSessConfig, U32 w_SessionTagId)
{
	gpsc_sess_specific_cfg_type *p_zSessSpecificCfg, *p_zPrevSessSpecificCfg;
	gpsc_ctrl_type*	p_zGPSCControl;
	p_zGPSCControl = gp_zGPSCControl;
	p_zPrevSessSpecificCfg = NULL;
	p_zSessSpecificCfg = NULL;

	/* get node */

	p_zSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;
	/* check if session with this session Id already exists, if so return */
	while(p_zSessSpecificCfg != NULL)
	{
		if((p_zSessSpecificCfg->w_SessionTagId == w_SessionTagId) ||
		   (p_zSessSpecificCfg->w_SessionTagId == C_SESSION_LOCAL))
		{
		  STATUSMSG("Status: Already present Node with TagId %x or got a Local", w_SessionTagId);
		  return p_zSessSpecificCfg;
		}
	p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;
	} /* while(p_zSessSpecificCfg != NULL) */
	return NULL;
}

/********************************************************************
 *
 * gpsc_session_add_node
 *
 * Function description:
 *  Add new session to session specific list.
 *
 * Parameters:
 *
 *  gpsc_sess_specific_cfg_type: pointer to session specific information
 *  used for multi session hybrid mode in GPS5350
 *
 * Return:
 *   None
 *
 *********************************************************************
 */

gpsc_sess_specific_cfg_type* gpsc_session_add_node (gpsc_sess_cfg_type*	p_zSessConfig, U32 w_SessionTagId)
{
	gpsc_sess_specific_cfg_type *p_zSessSpecificCfg, *p_zPrevSessSpecificCfg;
	gpsc_ctrl_type				*p_zGPSCControl = gp_zGPSCControl;
	gpsc_sys_handlers 		*p_zSysHandlers = p_zGPSCControl->p_zSysHandlers;
	TNAVCD_Ctrl				*pNavcCtrl = (TNAVCD_Ctrl *)p_zSysHandlers->hNavcCtrl;
	U8 i =0;

	p_zPrevSessSpecificCfg = NULL;
	p_zSessSpecificCfg = NULL;

	/* add first node */

	/* point to session specific pointer of session config */
	p_zSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;

	/* add first node */
	if(p_zSessSpecificCfg == NULL)
	{
		p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) mcpf_mem_alloc_from_pool(p_zSysHandlers->hMcpf, pNavcCtrl->hSessPool);
	  	if (p_zSessSpecificCfg == NULL)
	  	{
	  		ERRORMSG("Error: Failed to Acquire Memory for GPSC Session Specific Config Structure");
			p_zSessConfig->p_zSessSpecificCfg = NULL;
			return NULL;
	  	}

  	    p_zSessSpecificCfg->w_SessionTagId = w_SessionTagId;
		//p_zSessSpecificCfg->z_GeoFenceConfig.geo_fence_control = 0;
		p_zSessSpecificCfg->p_nextSession = NULL;
		p_zSessConfig->p_zSessSpecificCfg = p_zSessSpecificCfg;

	}
	else
	{

		/* traverse all session nodes till reach last node */
		if(p_zSessSpecificCfg->p_nextSession == NULL)
		{
			p_zPrevSessSpecificCfg = p_zSessSpecificCfg;
		}
		else
		{
			while(p_zSessSpecificCfg != NULL)
			{
				p_zPrevSessSpecificCfg = p_zSessSpecificCfg;
				p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *)p_zSessSpecificCfg->p_nextSession;
			}
		}


		/* create node */
		p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) mcpf_mem_alloc_from_pool(p_zSysHandlers->hMcpf, pNavcCtrl->hSessPool);
	  	if (p_zSessSpecificCfg == NULL)
	  	{
	  		ERRORMSG("Error: Failed to Acquire Memory for GPSC Session Specific Config Structure");
			p_zSessConfig->p_zSessSpecificCfg = NULL;
			return NULL;
	  	}


        p_zSessSpecificCfg->w_SessionTagId   = w_SessionTagId;


		/*Initialize per session GoeFence Parameters*/
		for(i=0;i<=GEOFENCE_MAX_REGIONS;i++)
		  p_zSessSpecificCfg->RegionNumberMap[i] = 0;
		p_zSessSpecificCfg->RegionBitmap = 0;
		//p_zSessSpecificCfg->z_GeoFenceConfig.geo_fence_control = 0;
		p_zSessSpecificCfg->p_nextSession = NULL;
		p_zPrevSessSpecificCfg->p_nextSession = (struct gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg;

	}
	return p_zSessSpecificCfg;
}

/********************************************************************
 *
 * gpsc_session_del_node
 *
 * Function description:
 *  Delete session specific node for given session tag ID.
 *
 * Parameters:
 *
 *  gpsc_sess_specific_cfg_type: pointer to session specific information
 *  used for multi session hybrid mode in GPS5350
 *
 * Return:
 *   None
 *
 *********************************************************************
 */
U8 gpsc_session_del_node (gpsc_sess_cfg_type*	p_zSessConfig, U32 w_SessionTagId)
{
	gpsc_sess_specific_cfg_type *p_zSessSpecificCfg, *p_zPrevSessSpecificCfg;
	U8 bFoundToDel = FALSE;
	gpsc_ctrl_type				*p_zGPSCControl = gp_zGPSCControl;
	gpsc_sys_handlers 		*p_zSysHandlers = p_zGPSCControl->p_zSysHandlers;

	p_zPrevSessSpecificCfg = NULL;
	p_zSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;
	T_GPSC_result gpscRes;


	/* if no node present - no session running */
	if(p_zSessSpecificCfg == NULL)
	{
			STATUSMSG("SessionInfo: All Nodes deleted, no running session");
			return FALSE;
	}

	/* if request for all nodes need to be deleted */
	if(w_SessionTagId == C_SESSION_INVALID)
	{
	     gpsc_sess_specific_cfg_type * p_zNextCfg = p_zSessConfig->p_zSessSpecificCfg;

            while(p_zNextCfg != NULL)
            {
            	  p_zNextCfg = (gpsc_sess_specific_cfg_type *) p_zNextCfg->p_nextSession;
		          mcpf_mem_free_from_pool(p_zSysHandlers->hMcpf, (U8 *)p_zSessSpecificCfg);
		          p_zSessSpecificCfg = p_zNextCfg;
             }

	      p_zSessConfig->p_zSessSpecificCfg= NULL;
             p_zSessConfig->w_TotalNumSession=0;

              /* If BLF was enabled for session, disable it in hardware */
              if(p_zSessConfig->u_BLF_State != C_BLF_STATE_DISABLED)
              {
			if(p_zSessConfig->u_BLF_State == C_BLF_STATE_ENABLED)
			{
                        gpscRes = gpsc_app_blf_config_req(0, 0);
                        if (gpscRes < GPSC_SUCCESS)
                        {
                                STATUSMSG("ERROR: Disabling BLF failed.\n");
                        }
			}
                        /* Update the BLF session indicator in session config */
                        p_zSessConfig->u_BLF_State = C_BLF_STATE_DISABLED;
              }
	      return TRUE;

	}

	/* traverse till the node where session Tag ID matches */
	while(p_zSessSpecificCfg != NULL)
	{
		if ( p_zSessSpecificCfg->w_SessionTagId == w_SessionTagId)
		{
			bFoundToDel = TRUE;
			//p_zPrevSessSpecificCfg = p_zSessSpecificCfg;
			break;
		}
		p_zPrevSessSpecificCfg = p_zSessSpecificCfg;
		p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;
	}

	/* delete node */
	if (bFoundToDel)
	{
		/* move and map to next node */
		if(p_zPrevSessSpecificCfg)
		{
			p_zPrevSessSpecificCfg->p_nextSession = p_zSessSpecificCfg->p_nextSession;
		}
		else
		{
			p_zSessConfig->p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;
		}

                /* If BLF was enabled for this session, disable it in hardware */
                if((p_zSessSpecificCfg->u_BLF_Flag) && (p_zSessConfig->u_BLF_State != C_BLF_STATE_DISABLED))
                {
			if(p_zSessConfig->u_BLF_State == C_BLF_STATE_ENABLED)
			{
                        gpscRes = gpsc_app_blf_config_req(0, 0);
                        if (gpscRes < GPSC_SUCCESS)
                        {
                                STATUSMSG("ERROR: Disabling BLF failed.\n");
                        }
			}
                        /* Update the BLF session indicator in session config */
                        p_zSessConfig->u_BLF_State = C_BLF_STATE_DISABLED;
                       	STATUSMSG("STATUS: BLF  Disabled while deleting the node.\n");
                }
		/* free memory - delete node */
		mcpf_mem_free_from_pool(p_zSysHandlers->hMcpf, (U8 *) p_zSessSpecificCfg);
		/* decrement session node count */
		p_zSessConfig->w_TotalNumSession--;

		/* If the only remaining session is BLF enabled, enable BLF in hardware */
		if(( p_zSessConfig->w_TotalNumSession == 1) && (p_zSessConfig->p_zSessSpecificCfg->u_BLF_Flag) &&
			(p_zSessConfig->u_BLF_State == C_BLF_STATE_DORMANT))
		{
                       	STATUSMSG("STATUS: Enabling BLF back.\n");
                        gpscRes = gpsc_app_blf_config_req(1, p_zSessConfig->u_BLF_Count);
                        if (gpscRes < GPSC_SUCCESS)
                        {
                                STATUSMSG("ERROR: Enabling BLF failed.\n");
                        }
			else
			{
                        	/* Update the BLF session indicator in session config */
                        	p_zSessConfig->u_BLF_State = C_BLF_STATE_ENABLED;
			}
                }

	    return TRUE;
	}
	else
	{
		ERRORMSG("Error: SessionInfo: Failed to delete node as SessionID mismatch");
	}

	return FALSE;
}



/********************************************************************
 *
 * gpsc_sess_calc_hcf
 *
 * Function description:
 * Calculate Highest Common Factor (HCF) for Periodicity.
 *
 * Parameters:
 *
 *  number 1, number 2 - two number of which HCF to be calculated.
 *
 * Return:
 *   U16 - HCF of two numbers.
 *
 *********************************************************************
 */
U16 gpsc_sess_calc_hcf (U16 num1, U16 num2)
{
	if (num1 == 0) return num2;
	if (num2== 0) return num1;
	return(gpsc_sess_calc_hcf ((U16)num2, (U16) (num1%num2)));
}

/********************************************************************
 *
 * gpsc_sess_calc_periodicity
 *
 * Function description:
 *  Calculate Periodicity for hybrid mode (multi session) specially for GPS5350.
 *
 * Parameters:
 *
 *  gpsc_sess_specific_cfg_type: pointer to session specific information
 *  used for multi session hybrid mode in GPS5350
 *
 * Return:
 *   None
 *
 * Note: ForNL5500, to support 0.5 second sub second report rate, if
 *       report period is set to zero, it means sub second periodic
 *       reporting is enabled.
 *********************************************************************
 */
U16 gpsc_sess_calc_periodicity (gpsc_ctrl_type *p_zGPSCControl)
{
	U16 u_HCF;
	gpsc_sess_cfg_type*	p_zSessConfig = p_zGPSCControl->p_zSessConfig;
	gpsc_event_cfg_type*    p_zEventCfg = p_zGPSCControl->p_zEventCfg;
	gpsc_sess_specific_cfg_type *p_zCurrSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;

	STATUSMSG("gpsc_sess_calc_periodicity: Entering.");
	/* for NL5500 if sub_sec periodic is disabled, initialize u_HCF to 1 else to ZERO */
	if(p_zEventCfg->w_PeriodicReportRate)
	{
		u_HCF = 1;
	}
	else /* sub_sec periodic is enabled */
	{
		/* sub second periodic report enabled, return periodicity as 0 */
		u_HCF = 0;
//		return u_HCF;
	}

//	p_zCurrSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;

	if(p_zCurrSessSpecificCfg == NULL)
	{
		ERRORMSG("Error: gpsc_sess_calc_periodicity: No session exist");
		return u_HCF; /* for review: or return ERROR  */
	}

	/* return periodicity if there is only one node present */
	if(p_zCurrSessSpecificCfg->p_nextSession == NULL)
	{
		STATUSMSG("gpsc_sess_calc_periodicity: p_zCurrSessSpecificCfg->p_nextSession == NULL");
		return p_zCurrSessSpecificCfg->w_ReportPeriod;
	}

	/* initialize u_HCF to periodicity of first node */
	u_HCF = p_zCurrSessSpecificCfg->w_ReportPeriod;
	STATUSMSG("gpsc_sess_calc_periodicity: Periodicity of first node  [%d]\n",u_HCF);

	/* traverse all node to find HCF value for periodicity */
    p_zCurrSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zCurrSessSpecificCfg->p_nextSession;
	while(p_zCurrSessSpecificCfg != NULL)
	{

		/*Set the HCF as 1 sec, SMLC should be reported as soon as first fix is received. Later this number
		    shall be set based on the periodicity of the expected reports*/

		/* check if any of one session has w_ReportPeriod = 0, means sub second enabled */
		STATUSMSG("gpsc_sess_calc_periodicity: Next session w_ReportPeriod [%d]\n",p_zCurrSessSpecificCfg->w_ReportPeriod);
		if((0 == u_HCF) || (0 == p_zCurrSessSpecificCfg->w_ReportPeriod))
		{
			return 0; /* sub second periodic report enabled, return periodicity as 0 */
		}
		else /* sub second periodic disabled */
		{
			/*if(p_zCurrSessSpecificCfg->u_WaitFirstFix == TRUE)
			  {
			  return 1; // sub second periodic report disabled
			  }
			 */
			STATUSMSG("gpsc_sess_calc_periodicity: Get HCF(%d, %d)\n",u_HCF,p_zCurrSessSpecificCfg->w_ReportPeriod);
		/* calculate HCF of two Node */
		u_HCF = gpsc_sess_calc_hcf(u_HCF, p_zCurrSessSpecificCfg->w_ReportPeriod);
			STATUSMSG("gpsc_sess_calc_periodicity: HCF  [%d]\n",u_HCF);
		/* if HCF is 1, break and return immediately */
			if (u_HCF == 1)
		{
			break;
		}
		}
    	/* traverse to next node */
		p_zCurrSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zCurrSessSpecificCfg->p_nextSession;

	} /* end of while(p_zCurrSessSpecificCfg != NULL) */
	STATUSMSG("gpsc_sess_calc_periodicity: u_HCF  [%d]\n",u_HCF);
	STATUSMSG("gpsc_sess_calc_periodicity: Entering.");
	return u_HCF;
}


/********************************************************************
 *
 * gpsc_sess_update_posComp
 *
 * Function description:
 *  This function sets position computing meas entity in session common config.
 *  It will set u_PosCompEntity == C_MS_COMPUTED if any of the session in
 *  multiple session is C_MS_COMPUTED.
 *  Else if all sessions are C_SMLC_COMPUTED, then it will set session common
 *  position measurement entity as C_SMLC_COMPUTED.
 *
 * Parameters:
 *  gpsc_sess_cfg_type: pointer to session information
 *
 * Return:
 *   None
 *
 *********************************************************************
 */
void gpsc_sess_update_posComp(gpsc_sess_cfg_type *p_zSessConfig)
{

     gpsc_sess_specific_cfg_type *p_zSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;
	 p_zSessConfig->u_PosCompEntity=0;
	STATUSMSG("gpsc_sess_update_posComp: Entering.");
     while(p_zSessSpecificCfg != NULL)
     	{

           if(p_zSessSpecificCfg->u_PosCompEntity == C_MS_COMPUTED)
           {
               p_zSessConfig->u_PosCompEntity = C_MS_COMPUTED;
   	        return;
           }

           p_zSessSpecificCfg= (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;
     	}
       p_zSessConfig->u_PosCompEntity = C_SMLC_COMPUTED;
	STATUSMSG("gpsc_sess_update_posComp: Exiting.");
	return;

}

/********************************************************************
 *
 * gpsc_sess_get_min_report_period
 *
 * Function description:
 *  This function return the smallest report period from all sessions
 *  present
 *
 * Parameters:
 *  gpsc_sess_cfg_type: pointer to session information
 *
 * Return:
 *   U16 - smallest report period
 *
 *********************************************************************
 */
U16 gpsc_sess_get_min_report_period(gpsc_sess_cfg_type *p_zSessConfig)
{
	  U16 w_Min=0;
    gpsc_sess_specific_cfg_type *p_zSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;
	  w_Min = p_zSessSpecificCfg->w_ReportPeriod;
    while(p_zSessSpecificCfg != NULL)
		{
		if(p_zSessSpecificCfg->w_ReportPeriod < w_Min)
			{
			w_Min = p_zSessSpecificCfg->w_ReportPeriod;
			}
      p_zSessSpecificCfg= (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;
    }
    return w_Min;
}
/********************************************************************
 *
 * gpsc_sess_get_nmea_bitmap
 *
 * Function description:
 *  This function retrieves nmea bitmap if any one of the session has
 *  activated nmea bitmap.
 *
 * Parameters:
 *  gpsc_sess_cfg_type: pointer to session information
 *
 * Return:
 *   U16 NmeaBitmap
 *
 *********************************************************************
 */
U16 gpsc_sess_get_nmea_bitmap(gpsc_sess_cfg_type* p_zSessConfig)
{
	 U16 w_ResultTypeBitmap=0;
    gpsc_sess_specific_cfg_type *p_zSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;

    while (p_zSessSpecificCfg != NULL)
    {
       if ((p_zSessSpecificCfg->w_ResultTypeBitmap && NMEA_MASK) == TRUE)
       {
          w_ResultTypeBitmap |= (p_zSessSpecificCfg->w_ResultTypeBitmap & NMEA_MASK);
       }
       p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type*) p_zSessSpecificCfg->p_nextSession;
    }
    return w_ResultTypeBitmap; 
  
    // For Rec(k)on we hardcode to what we need, as there is no UI is GPSCConfig tool for it
    /*UNREFERENCED_PARAMETER(p_zSessConfig);
    return (GPSC_RESULT_NMEA_GGA | GPSC_RESULT_NMEA_GSA | GPSC_RESULT_NMEA_GSV | GPSC_RESULT_NMEA_RMC);*/
}



/********************************************************************
 *
 * gpsc_sess_apm
 *
 * Function description:
 *  Configure the Advanced Power Management dynamically
 *
 * Parameters:
 *  w_AvgReportPeriod: Average Reporting Period
 *
 * Return:
 *   None
 *
 *********************************************************************
 */
void gpsc_sess_apm(gpsc_ctrl_type *p_zGPSCControl,U16 w_AvgReportPeriod)
{

	gpsc_cfg_type*        p_zGPSCConfig =  p_zGPSCControl->p_zGPSCConfig ;

    gpsc_dyna_feature_cfg_type *p_zGPSCDynaFeatureConfig = p_zGPSCControl->p_zGPSCDynaFeatureConfig;

	if(p_zGPSCDynaFeatureConfig->feature_flag & GPSC_FEAT_APM)
	{

		STATUSMSG("SessionInfo: Dynamic APM - ApmCtrl=%d, SM=%d, AvgPd=%d",
			p_zGPSCDynaFeatureConfig->apm_control,
			p_zGPSCDynaFeatureConfig->search_mode,
			w_AvgReportPeriod);
	}
	else
	{
		p_zGPSCDynaFeatureConfig->apm_control = p_zGPSCConfig->apm_control;
		p_zGPSCDynaFeatureConfig->search_mode = p_zGPSCConfig->search_mode;
		p_zGPSCDynaFeatureConfig->saving_options = p_zGPSCConfig->saving_options;
		p_zGPSCDynaFeatureConfig->power_save_qc = p_zGPSCConfig->power_save_qc;

		STATUSMSG("SessionInfo: Configured APM - ApmCtrl=%d, SM=%d, AvgPd=%d",
			p_zGPSCDynaFeatureConfig->apm_control,
			p_zGPSCDynaFeatureConfig->search_mode,
			w_AvgReportPeriod);
	}

	if(p_zGPSCDynaFeatureConfig->apm_control != GPSC_APM_ENABLE)
		return;

	/* In case of AUTO mode (AUTO check box selected) in config file,
	 * no need to inject APM as automode is injected at user command by default
	 */
	if(p_zGPSCDynaFeatureConfig->search_mode == GPSC_APM_SEARCH_MODE_AUTO)
	{
		/* if search mode is AUTO (TRUE), exit as by default AUTO mode injected to receiver */
		return;
	}
	else /* non AUTO mode, means in config file, AUTO check box is not selected */
	{

		if(w_AvgReportPeriod <= CONTINUOUS_TRACKING_TIME)
		{
			p_zGPSCDynaFeatureConfig->search_mode=GPSC_APM_SEARCH_MODE_CONT_TRACKING;
			STATUSMSG("SessionInfo: APM CT - SM=%d\n",p_zGPSCDynaFeatureConfig->search_mode);
		}
		else
		{
			p_zGPSCDynaFeatureConfig->search_mode=GPSC_APM_SEARCH_MODE_RAPID_REACQ;
			STATUSMSG("SessionInfo: APM RR - SM=%d\n",p_zGPSCDynaFeatureConfig->search_mode);
		}
	}

    STATUSMSG("SessionInfo: APM- AC=%d, SM=%d, AvgPd=%d",
			p_zGPSCDynaFeatureConfig->apm_control,
			p_zGPSCDynaFeatureConfig->search_mode,
			w_AvgReportPeriod);

	/* inject APM package as ai2 msg to sensor */
	gpsc_mgp_tx_inject_advanced_power_management (p_zGPSCControl);

}
/********************************************************************
 *
 * gpsc_sess_update_period
 *
 * Function description:
 *
 * Parameters:
 *
 *  gpsc_ctrl_type: pointer to session information
 * u_PeriodPreset: Used to prset ReprtingPeriod
 *
 * Return:
 *   None
 *
 ********************************************************************* */
S8 gpsc_sess_update_period(gpsc_ctrl_type *p_zGPSCControl, U8 u_PeriodPreset)
{
	gpsc_sess_cfg_type *p_zSessConfig = p_zGPSCControl->p_zSessConfig;
	gpsc_event_cfg_type*    p_zEventCfg = p_zGPSCControl->p_zEventCfg;
	U16 w_OldAvgRepPeriod = p_zSessConfig->w_AvgReportPeriod;
	U8 old_posComp=p_zGPSCControl->p_zSessConfig->u_PosCompEntity;

	STATUSMSG("gpsc_sess_update_period: Entering.");
	STATUSMSG("gpsc_sess_update_period: u_PeriodPreset  [%d]\n",u_PeriodPreset);
	if(u_PeriodPreset==C_PERIOD_PRESET)
	{
		if(FALSE == p_zEventCfg->u_SubSecPeriodicReportRate)
		{
			p_zSessConfig->w_AvgReportPeriod = 1;
		}
		else
		{
		p_zSessConfig->w_AvgReportPeriod = 0;
		}
	}
	else
	{
		p_zSessConfig->w_AvgReportPeriod = gpsc_sess_calc_periodicity (p_zGPSCControl);
		STATUSMSG("gpsc_sess_update_period: p_zSessConfig->w_AvgReportPeriod [%d]\n",p_zSessConfig->w_AvgReportPeriod);
	}

	gpsc_sess_update_posComp(p_zSessConfig);

	STATUSMSG("gpsc_sess_update_period: u_PeriodPreset                   [%d]\n",u_PeriodPreset);
	STATUSMSG("gpsc_sess_update_period: p_zSessConfig ->u_PosCompEntity  [%d]\n",p_zSessConfig ->u_PosCompEntity);
	STATUSMSG("gpsc_sess_update_period: old_posComp                      [%d]\n",old_posComp);
	STATUSMSG("gpsc_sess_update_period: w_OldAvgRepPeriod                [%d]\n", w_OldAvgRepPeriod);
	STATUSMSG("gpsc_sess_update_period: p_zSessConfig->w_AvgReportPeriod [%d]\n", p_zSessConfig->w_AvgReportPeriod);
	STATUSMSG("gpsc_sess_update_period: p_zSessConfig->u_ReConfigFlag    [%d]\n", p_zSessConfig->u_ReConfigFlag);
	/*Either Intersystem Interval or the Position Method is changed*/
	if(u_PeriodPreset || (p_zSessConfig ->u_PosCompEntity != old_posComp) ||
	(w_OldAvgRepPeriod != p_zSessConfig->w_AvgReportPeriod) || (p_zSessConfig->u_ReConfigFlag == TRUE) )
	{
		STATUSMSG("gpsc_sess_update_period: before gpsc_sess_apm().");
		//gpsc_sess_apm(p_zGPSCControl,p_zSessConfig->w_AvgReportPeriod);

#ifdef CLIENT_CUSTOM
		gpsc_mgp_cfg_set_evt(p_zGPSCControl, 0, (U32) C_CUSTOM_COMPUTED_EVENT,
				p_zSessConfig->w_AvgReportPeriod,p_zEventCfg->u_SubSecPeriodicReportRate);
#else
		STATUSMSG("gpsc_sess_update_period: p_zSessConfig->u_PosCompEntity [%d]\n",p_zSessConfig->u_PosCompEntity );
		if (p_zSessConfig->u_PosCompEntity == C_SMLC_COMPUTED)
		{
			if(p_zEventCfg->u_Ext_reports)
                        {
				STATUSMSG("Injected Event Config: %x", C_SMLC_COMPUTED_EVENT_EXT);
				STATUSMSG("gpsc_sess_update_period: C_SMLC_COMPUTED_EVENT_EXT w_AvgReportPeriod [%d]\n",p_zSessConfig->w_AvgReportPeriod);
				gpsc_mgp_cfg_set_evt(p_zGPSCControl, 0,
						 (U32) C_SMLC_COMPUTED_EVENT_EXT,
						  p_zSessConfig->w_AvgReportPeriod,
						   p_zEventCfg->u_SubSecPeriodicReportRate);
			}
			else
			{
				STATUSMSG("Injected Event Config: %x", C_SMLC_COMPUTED_EVENT);
				STATUSMSG("gpsc_sess_update_period: C_SMLC_COMPUTED_EVENT w_AvgReportPeriod [%d]\n",p_zSessConfig->w_AvgReportPeriod);
				gpsc_mgp_cfg_set_evt(p_zGPSCControl, 0,
						 (U32) C_SMLC_COMPUTED_EVENT,
						   p_zSessConfig->w_AvgReportPeriod,
						    p_zEventCfg->u_SubSecPeriodicReportRate);
			}
		}
		else
		{
			if(p_zEventCfg->u_Ext_reports)
                        {
				STATUSMSG("Injected Event Config: %x", C_MS_COMPUTED_EVENT_EXT);
				STATUSMSG("gpsc_sess_update_period: C_MS_COMPUTED_EVENT_EXT w_AvgReportPeriod [%d]\n",p_zSessConfig->w_AvgReportPeriod);
				gpsc_mgp_cfg_set_evt(p_zGPSCControl, 0,
							(U32) C_MS_COMPUTED_EVENT_EXT,
							 p_zSessConfig->w_AvgReportPeriod,
							  p_zEventCfg->u_SubSecPeriodicReportRate );
			}
			else
			{
				STATUSMSG("Injected Event Config: %x", C_MS_COMPUTED_EVENT);
				STATUSMSG("gpsc_sess_update_period: C_MS_COMPUTED_EVENT w_AvgReportPeriod [%d]\n",p_zSessConfig->w_AvgReportPeriod);
				gpsc_mgp_cfg_set_evt(p_zGPSCControl, 0, (U32) C_MS_COMPUTED_EVENT, p_zSessConfig->w_AvgReportPeriod, p_zEventCfg->u_SubSecPeriodicReportRate );
			}
	    }
#endif
		if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
		{
			//   return GPSC_TX_INITIATE_FAIL;
			FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
			GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
			return GPSC_TX_INITIATE_FAIL;
		}

		STATUSMSG("Injected Event Config: AvgPd = %d,PM = %d\n", p_zSessConfig->w_AvgReportPeriod, p_zSessConfig->u_PosCompEntity);
	}
	STATUSMSG("gpsc_sess_update_period: Exiting.");
 return 0;
}

void gpsc_inject_cepinfo_start_mode(gpsc_ctrl_type *p_zGPSCControl,U16 timeout1,U16 timeout2,U16 accuracy1,U16 accuracy2) {

    U8 u_Body[22];
    gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;
    U8 *p_B = &u_Body[0];

    STATUSMSG("Status:gpsc_inject_cepinfo_start_mode: Accu1:%d,Accu2:%d,timeout1:%ds,timeout2:%ds",
                                                accuracy1,accuracy2,timeout1,timeout2);

    /* Single PE sub packet id */
    p_B = NBytesPut(AI_CLAPP_CUSTOM_SUB_PKT_INJ_TIMEOUT_CEP_INFO, p_B, sizeof(U8) ); /* 1 byte */

    /* constructing single PE parameters -  accuracy and timeout */
    p_B = NBytesPut(0, p_B, sizeof(U8) ); /* first 1 byte reserved */
    p_B = NBytesPut((timeout1*2), p_B, sizeof(U16) ); /* 2 byte */
    p_B = NBytesPut(accuracy1, p_B, sizeof(U16) ); /* 2 byte */
    p_B = NBytesPut((timeout2*2), p_B, sizeof(U16) ); /* 2 byte */
    p_B = NBytesPut(accuracy2, p_B, sizeof(U16) ); /* 2 byte */

    /* rest 12 bytes are reserved */
    p_B = NBytesPut(0, p_B, sizeof(U32) ); /* 4 byte */
    p_B = NBytesPut(0, p_B, sizeof(U32) ); /* 4 byte */
    p_B = NBytesPut(0, p_B, sizeof(U32) ); /* 4 byte */


    /* preparing AI2 packet and send to sensor */
    z_PutMsgDesc.u_AckType = C_AI2_NO_ACK; //C_AI2_ACK_REQ;
    z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_CUSTOM_PACKET;
    z_PutMsgDesc.p_Buf = &u_Body[0];
    z_PutMsgDesc.w_PacketLen = (U16)(p_B - &u_Body[0]);
    z_PutMsgDesc.u_ForceFlag = 0;
    z_PutMsgDesc.u_MsgSend = FALSE;

    AI2TX("Custom Packet 251 - Inject Single PE",AI_CLAPP_CUSTOM_SUB_PKT_INJ_TIMEOUT_CEP_INFO);

    gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
    gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK);

}
