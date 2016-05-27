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
 * FileName			:	gpsc_mgp_rx.c
 *
 * Description     	:
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#include "gpsc_types.h"
#include "gpsc_database.h"
#include "gpsc_data.h"
#include "gpsc_meas.h"
#include "gpsc_init.h"
#include "gpsc_mgp_tx.h"
#include "gpsc_sess.h"
#include "gpsc_msg.h"
#include "gpsc_sequence.h"
#include "gpsc_state.h"
#include "gpsc_comm.h"
#include "gpsc_app_api.h"
#include "gpsc_mgp_rx.h"
#include "gpsc_mgp_cfg.h"
#include "gpsc_timer_api.h"
#include "gpsc_geo_fence.h"
#include "mcpf_time.h"
#ifdef WIFI_ENABLE
#include "navc_hybridPosition.h"
#endif
#ifdef ENABLE_INAV_ASSIST
#include "inavc_ext.h"
#endif
#include <math.h>


#ifdef GPSC_DEBUG
#include <stdio.h>
#endif

#include <utils/Log.h>
#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG "NAVD.GPSC_MGP_RX"    // our identification for logcat (adb logcat NAVD.GPSC_MGP_RX:V *:S)

extern TNAVCD_Ctrl* pNavcCtrl;

#define C_FCOUNT_DIF_FACTOR 100


void gpsc_sess_update_timeout(gpsc_ctrl_type*  p_zGPSCControl)
{
	gpsc_cfg_type*	p_zGPSCConfig = p_zGPSCControl->p_zGPSCConfig;
	gpsc_sess_cfg_type *p_zSessConfig = p_zGPSCControl->p_zSessConfig;

    gpsc_sess_specific_cfg_type * p_zSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;

	while (p_zSessSpecificCfg != NULL)
	{
       /*Adding the sleep duration to IsiCount to offset the Isicount for sleep duration
		  This can also be done when timer expires
		*/
       if(p_zGPSCConfig->low_power_state == LOW_POWER_MODE_SLEEP)
	   {
#ifdef LARGE_UPDATE_RATE
        p_zSessSpecificCfg->q_PeriodElapsed = p_zSessSpecificCfg->q_PeriodElapsed + p_zSessConfig->q_SleepDuration;
#else
        p_zSessSpecificCfg->q_PeriodElapsed = p_zSessSpecificCfg->q_PeriodElapsed;
#endif
	   }

       if(p_zSessSpecificCfg->u_WaitFirstFix == FALSE)
		{
		   if((p_zGPSCConfig->recv_min_rep_period == 500) && (p_zSessSpecificCfg->w_ReportPeriod==0))
		   {
		       p_zSessSpecificCfg->q_ValidFixTimeout = (p_zGPSCConfig->recv_min_rep_period);
		   }
		   else
		   {
			   p_zSessSpecificCfg->q_ValidFixTimeout = (U32)(p_zSessSpecificCfg->w_ReportPeriod * 1000);
		   }
	   }

	   p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;
	}
    return;
}


#ifdef LARGE_UPDATE_RATE
U8 gpsc_sess_sleep_estimate(gpsc_ctrl_type*  p_zGPSCControl, U8 u_FixNoFix)
{
    U8 w_ReportPeriodMinDiff=0xFF;
    U8 w_ReportPeriodDiff=0;
    S32 x_SleepEstimate;
	gpsc_sess_cfg_type *p_zSessConfig = p_zGPSCControl->p_zSessConfig;
    gpsc_sess_specific_cfg_type * p_zSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;
	gpsc_sess_specific_cfg_type * p_zSessSpecificTempCfg= NULL;

	/* traversing through all sessions */
	while (p_zSessSpecificCfg != NULL)
	{
        if(p_zSessSpecificCfg->w_ReportPeriod < (U16)(p_zSessSpecificCfg->q_PeriodElapsed/1000))
		{
		    p_zSessSpecificCfg->q_PeriodElapsed = 0;
			w_ReportPeriodDiff = (U8) p_zSessSpecificCfg->w_ReportPeriod;
	    }
	    else
       	{
            w_ReportPeriodDiff= (U8)(p_zSessSpecificCfg->w_ReportPeriod - (U16)(p_zSessSpecificCfg->q_PeriodElapsed/1000));
       	}

		if(w_ReportPeriodDiff < w_ReportPeriodMinDiff)
		{
			w_ReportPeriodMinDiff= w_ReportPeriodDiff;
			p_zSessSpecificTempCfg = p_zSessSpecificCfg;
		}
		/* check for next session */
        p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;
	} /* while (p_zSessSpecificCfg != NULL) */
	if((w_ReportPeriodMinDiff*1000) < C_THRESHOLD_FIX_TIME)
	{
		p_zSessConfig->q_SleepDuration = 0;
		return FALSE;
	}

	x_SleepEstimate = (S32)((w_ReportPeriodMinDiff * 1000) - p_zSessConfig->w_SleepDelta);

	/* If the last report was a no-fix, decrease the sleep duration */
	if ( !u_FixNoFix )
		x_SleepEstimate = (S32)(x_SleepEstimate - p_zSessConfig->q_NoFixDelta);

	if(x_SleepEstimate <= 0)
		p_zSessConfig->q_SleepDuration = 0;
	else
		p_zSessConfig->q_SleepDuration = (U32)x_SleepEstimate;

	/* If the time remaining before starting the next fix is small, let receiver be ON,
	* else, If the time remaining before starting the next fix is medium, let receiver be IDLE,
	* else, If the time remaining before starting the next fix is large, let receiver be OFF,
	* TBD: Define small, medium, large!
	*/

	if(p_zSessConfig->q_SleepDuration != 0)
	{
		/*KlocWork Critical Issue:55 Resolved by Checking pointer for NULL*/
	   if(p_zSessSpecificTempCfg != NULL)
	   {
		p_zSessSpecificTempCfg->q_PeriodElapsed = 0;
	   }
       gpsc_sess_update_timeout(p_zGPSCControl);

	   if(p_zSessConfig->w_TotalNumSession > 1)
	   {
	   if(p_zSessSpecificTempCfg)
	   {
          /*Least time remaining session should be started as first session*/
		  p_zSessSpecificTempCfg->u_WaitFirstFix = TRUE;
		    p_zSessSpecificTempCfg->q_PeriodElapsed = 0;
	   }
	   }


       p_zSessConfig->q_SleepActionStart = mcpf_getCurrentTime_inSec (p_zGPSCControl->p_zSysHandlers->hMcpf);

	   /* Start walking the sleep path */
	   p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_SLEEP;

	   /* stop periodic timer */
	   gpsc_timer_stop(p_zGPSCControl,C_TIMER_PERIODIC_REPORTING);

	   /* send receiver idle to GPS receiver */
	   gpsc_inject_gps_idle(p_zGPSCControl);

	   return TRUE;
	}

	return FALSE;
}
#endif /* LARGE_UPDATE_RATE*/


void gpsc_check_session_for_fix(gpsc_ctrl_type*  p_zGPSCControl, U8 u_FixNoFix)
{
    gpsc_state_type*	p_zGPSCState=p_zGPSCControl->p_zGPSCState;
    gpsc_sess_cfg_type*	p_zSessConfig = p_zGPSCControl->p_zSessConfig;
    gpsc_session_status_type*  p_zSessionStatus = &p_zGPSCState->z_SessionStatus;
    gpsc_sess_specific_cfg_type* p_zSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;
    gpsc_report_pos_type *p_zReportPos = &p_zGPSCControl->p_zGPSCDatabase->z_DBPos.z_ReportPos;
    gpsc_event_cfg_type * p_zEventCfg = p_zGPSCControl->p_zEventCfg;

    gpsc_cfg_type*			p_zGPSCConfig	= p_zGPSCControl->p_zGPSCConfig;
	 gpsc_smlc_assist_type*  p_zSmlcAssist  = p_zGPSCControl->p_zSmlcAssist;

	 U8 u_NodeDeleted = FALSE;
    U8 u_WaitFirstFix= FALSE;
	 U8 sentResult=FALSE;
    S32 q_FCountDiff = 0;
	 int i = 0;
	 U8 u_UpdateConfig= FALSE;

    /* Check the difference in reports. There may be rollover of U32 value */
    if (p_zSessConfig->q_LastFCount == GPSC_INVALID_FCOUNT)
       q_FCountDiff = 1;
    else
       q_FCountDiff = (p_zSessConfig->q_FCount - p_zSessConfig->q_LastFCount);


	 /* Update Fcount only when report is not NULL reoprt */
    p_zSessConfig->q_LastFCount = p_zSessConfig->q_FCount;

    /* Process Position report for each valid session*/
    while (p_zSessSpecificCfg != NULL)
    {
        if (p_zSessSpecificCfg->u_PosCompEntity != C_MS_COMPUTED)
		 {
            p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;
            continue;
		 }

	    /* check if session needs to be deleted */
	    if (p_zSessSpecificCfg->w_FixCounter == 0)
		 {
		    /* May be this session is no more required, so delete the session */
 	        gpsc_sess_specific_cfg_type* p_zTempSessSpecificCfg= NULL;
		     p_zTempSessSpecificCfg =  p_zSessSpecificCfg;
           p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;

		    /* Node is getting deleted copy the next pointer*/
           u_UpdateConfig = TRUE;
			  gpsc_app_update_gf_config(p_zGPSCControl,p_zTempSessSpecificCfg);
		     gpsc_cmd_complete_cnf(GPSC_SUCCESS);
		     gpsc_session_del_node(p_zSessConfig, p_zTempSessSpecificCfg->w_SessionTagId);
                    
           /* Check whether deleting the node resulted in enabling the BLF */
           if (p_zSessConfig->u_BLF_State == C_BLF_STATE_ENABLED)
           {
        	      /* If BLF got enabled, disable Extednded reports */
               p_zEventCfg->u_Ext_reports = 0;
			      p_zSessConfig->u_ReConfigFlag = TRUE;
           }
			  u_NodeDeleted = TRUE;

#ifdef LARGE_UPDATE_RATE
			/* q_SleepDuration = 0 - if in case session sleep when Rp > 30 seconds */
			p_zSessConfig->q_SleepDuration = 0;
#endif
		    continue;
		}

	    /* update the IsiCount with valid time interval */
	    p_zSessSpecificCfg->q_PeriodElapsed = p_zSessSpecificCfg->q_PeriodElapsed + q_FCountDiff;

	    if ( u_FixNoFix ) /*Valid Fix*/
		 {
     	      /* for geo fence */
			   S16 velocity[3];

            velocity[0] = p_zReportPos->x_VelEast;
            velocity[1] = p_zReportPos->x_VelNorth;
            velocity[2] = p_zReportPos->x_VelVert;

		    /* initialize invalid fix count for re request assistance
		       if good position, then set it to zero */
	        if (p_zSessConfig->u_SmlcAssisPermit == C_SMLC_ASSIS_PERMITTED)
	        {
               /* initialize the counter to 0 when got good fix */
		         p_zGPSCState->u_CountInvalidFix = 0;
	        }

            /* added for geo fence */
	    if (p_zSessSpecificCfg->u_WaitFirstFix == TRUE)
		 {
			    /* GPSC: NMEA issue - GSV is not populated with any data till one sec after fix. */
                    /* delaying report sending by one iteration as report period is 1 second for first fix */

					/* checking for geofence setting: first fix*/
               if (p_zSessSpecificCfg->w_typeofrequest == GPSC_NORMAL_REQUEST)
                  gpsc_send_position_result(p_zGPSCControl, TRUE, p_zSessSpecificCfg);
                        
               if (p_zSessSpecificCfg->w_typeofrequest == GPSC_GEOFENCE_REQUEST)
               {
                  if (p_zSessSpecificCfg->GeofenceReportConfigration == 0)
                  {
                     if ((p_zSessSpecificCfg->RegionBitmap) & (p_zSessSpecificCfg->GeofenceStatus))
                         gpsc_send_position_result(p_zGPSCControl, TRUE, p_zSessSpecificCfg);
                  }

                  else if (p_zSessSpecificCfg->GeofenceReportConfigration == 1)
								      gpsc_send_position_result(p_zGPSCControl, TRUE, p_zSessSpecificCfg);
				   }
			      p_zSessSpecificCfg->w_FixCounter--;
	            p_zSessSpecificCfg->u_WaitFirstFix=FALSE;
			      u_WaitFirstFix = TRUE;
			} /* end of else if(p_zSessSpecificCfg->u_WaitFirstFix) */
         else if(p_zSessSpecificCfg->u_WaitFirstFix == FALSE)
         {
                if( (p_zSessSpecificCfg->q_ValidFixTimeout != 0) &&
                    (p_zSessSpecificCfg->q_PeriodElapsed >= (p_zSessSpecificCfg->q_ValidFixTimeout - C_FCOUNT_DIF_FACTOR)) )
                {
                    /* send position report as report periodicity is matched */
                    /* to support hybrid mode, here to add 3rd param as w_SessionTagId
                       to send pos report perticular session as periodicity matched */

                    if(p_zSessSpecificCfg->w_typeofrequest == GPSC_NORMAL_REQUEST)
                    {
                        gpsc_send_position_result(p_zGPSCControl, TRUE, p_zSessSpecificCfg);
                    }


                    if (p_zSessSpecificCfg->w_typeofrequest == GPSC_GEOFENCE_REQUEST)
                    {
                        if (p_zSessSpecificCfg->GeofenceReportConfigration == 0)
                        {
                            if ((p_zSessSpecificCfg->RegionBitmap) & (p_zSessSpecificCfg->GeofenceStatus))
                            {
                                gpsc_send_position_result(p_zGPSCControl, TRUE, p_zSessSpecificCfg);
                            }
                        }

                        if (p_zSessSpecificCfg->GeofenceReportConfigration == 1)
                            gpsc_send_position_result(p_zGPSCControl, TRUE, p_zSessSpecificCfg);
                    }

                    p_zSessSpecificCfg->w_FixCounter--;
                    p_zSessSpecificCfg->q_PeriodElapsed = 0;
                    p_zSessSpecificCfg->u_WaitFirstFix = FALSE;

                    sentResult=TRUE;
                }
                else
                {
                    if (p_zSessSpecificCfg->w_typeofrequest == GPSC_GEOFENCE_REQUEST)
                    {
                        if (p_zSessSpecificCfg->GeofenceReportConfigration == 0)
                        {
                            if ((p_zSessSpecificCfg->RegionBitmap) & (p_zSessSpecificCfg->GeofenceStatus))
                            {
                                gpsc_send_position_result(p_zGPSCControl, TRUE, p_zSessSpecificCfg);
                            }
                        }

                        if (p_zSessSpecificCfg->GeofenceReportConfigration == 1)
							      gpsc_send_position_result(p_zGPSCControl, TRUE, p_zSessSpecificCfg);
					     }
			      }
			}
			else
			{
			    p_zSessSpecificCfg->w_FixCounter--;
			}

		}
		else // if (u_FixNoFix)
		{
	      /*No Timeout if q_ValidFixTimeout ==0, this may be zero only for TTFF, not for ISI*/
	   	if( (p_zSessSpecificCfg->q_ValidFixTimeout != 0) &&
			   (p_zSessSpecificCfg->q_PeriodElapsed >= (p_zSessSpecificCfg->q_ValidFixTimeout - C_FCOUNT_DIF_FACTOR)))
		   {
		      /* send position report as report periodicity is matched 
			     to support hybrid mode, here to add 3rd param as w_SessionTagId to send pos report to
			     perticular session as periodicity matched 
			     sending Time out report to calculate the receiver time out cases for re acquisition */
			  if (p_zSessConfig->u_SmlcAssisPermit == C_SMLC_ASSIS_PERMITTED)
			  {
                 if (p_zGPSCState->u_CountInvalidFix != UNDEFINED_U8)
                 {
				        p_zGPSCState->u_CountInvalidFix++;
				        if (p_zGPSCState->u_CountInvalidFix == p_zGPSCConfig->count_invalid_fix)
				        {
					        ALOGI("+++ %s: Re-Request for MSB assistance. Number of consecutive Time-outs [%d] +++/n",
                         __FUNCTION__, p_zGPSCState->u_CountInvalidFix);

					        p_zSmlcAssist->u_NumAssistAttempt = 0;
					        if (gpsc_request_assistance(p_zGPSCControl) != GPSC_SUCCESS)
                          ALOGE("+++ %s: Assistance Request Failure +++\n", __FUNCTION__);
                       else
					           p_zGPSCState->u_CountInvalidFix = UNDEFINED_U8;
				        }
				     }
			  }

		if (p_zSessSpecificCfg->w_typeofrequest == GPSC_NORMAL_REQUEST)
		{
                        U32             w_SessionTagId[25];
                        S8              u_TotSessions = 0;
                        S8              u_CurSession = 0;

                        // Since this condition is satisfied only for 1 client. Need to send Error to RRLP Session
                        if (p_zSessSpecificCfg->w_typeofsession!=GPSC_NW_SESSION_LP)
                            gpsc_send_position_result(p_zGPSCControl, FALSE, p_zSessSpecificCfg);

                        gpsc_session_get_all_nodes(p_zSessConfig, &w_SessionTagId, &u_TotSessions);
                        gpsc_sess_specific_cfg_type* p_zTempSpecificCfg = NULL;

                        for (u_CurSession=0; u_CurSession < u_TotSessions; u_CurSession++)
                        {
                            p_zTempSpecificCfg=gpsc_session_get_node(p_zSessConfig,w_SessionTagId[u_CurSession]);

                            if (p_zTempSpecificCfg == NULL)
				                  return;
                            
                            if (p_zTempSpecificCfg->w_typeofsession == GPSC_NW_SESSION_LP)
                            {
                                gpsc_send_position_result(p_zGPSCControl, FALSE, p_zTempSpecificCfg);
                                gpsc_session_del_node (p_zSessConfig, w_SessionTagId[u_CurSession]);
                            }
                        }
                }

                if (p_zSessSpecificCfg->w_typeofrequest == GPSC_GEOFENCE_REQUEST)
                {
                    if(p_zSessSpecificCfg->GeofenceReportConfigration == 0)
                    {
                        if((p_zSessSpecificCfg->RegionBitmap) & (p_zSessSpecificCfg->GeofenceStatus))
                        {
                            gpsc_send_position_result(p_zGPSCControl, TRUE, p_zSessSpecificCfg);
                        }
                    }

                    if (p_zSessSpecificCfg->GeofenceReportConfigration == 1)
                    {
						     gpsc_send_position_result(p_zGPSCControl, TRUE, p_zSessSpecificCfg);
					     }

				    }
	              
                p_zSessSpecificCfg->q_PeriodElapsed=0;
                sentResult = TRUE;
			  
                if (p_zSessSpecificCfg->u_WaitFirstFix)
			       {
	       	       /* We are unable to get the fix within TTFF, so forcefully terminating session*/
		              p_zSessSpecificCfg->w_FixCounter--;
	
	    		        p_zSessSpecificCfg->u_WaitFirstFix=FALSE;
				        u_WaitFirstFix = TRUE;
			       }
			       else
			       {
	                 p_zSessSpecificCfg->w_FixCounter--;
			       }
		   }

		} /*end of if(u_FixNoFix)*/



	    /* are we supposed to terminate a session based on number of fix reports now ? */
        /* Number of fix reports does play a role in terminating a session.
	     But has it reached the specified number yet? */

	    /* Yes, enough fix reports sent already. End session now. */
	    if (p_zSessSpecificCfg->w_TotalNumFixesPerSession== 0)
		{
	       /*This is true in case of infinite reports*/
		   p_zSessSpecificCfg->w_FixCounter=1;
		}
	    else if(p_zSessSpecificCfg->w_FixCounter== 0)
		{
 	       gpsc_sess_specific_cfg_type* p_zTempSessSpecificCfg= NULL;
		   p_zTempSessSpecificCfg =  p_zSessSpecificCfg;

		   p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;

		   /*Node is getting deleted copy the next pointer*/
			gpsc_app_update_gf_config(p_zGPSCControl,p_zTempSessSpecificCfg);
			u_UpdateConfig = TRUE;
		   gpsc_session_del_node(p_zSessConfig, p_zTempSessSpecificCfg->w_SessionTagId);
                   if(p_zSessConfig->u_BLF_State == C_BLF_STATE_ENABLED)
                   {
                           /* If BLF got enabled, disable Extednded reports */
                           p_zEventCfg->u_Ext_reports = 0;
			   p_zSessConfig->u_ReConfigFlag = TRUE;
                   }
		   u_NodeDeleted=TRUE;
		   continue;
		}

	   /*traversing to next node*/
	    p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;

    } /* while(p_zSessSpecificCfg != NULL) */

    /* clears number of unreported PE event */
    p_zSessionStatus->w_NumUnRptFixEvt = 0;

#ifndef NO_TRACKING_SESSIONS
    if (p_zSessConfig->u_SessionMode == C_MULTIFIX_SESSION_MODE_2)
	 {
       if (p_zGPSCControl->p_zGPSCState->z_SessionStatus.u_TrackSubState == TRKSTATE_INITIAL)
          p_zGPSCControl->p_zGPSCState->z_SessionStatus.u_TrackSubState = TRKSTATE_NORMAL;
	 }
#endif

    /* check if there is no session exists */
    if(p_zSessConfig->w_TotalNumSession == 0)
    {
       gpsc_sess_end(p_zGPSCControl);
	   return;
    }
    else if((u_NodeDeleted) || (u_WaitFirstFix))
    {
       p_zSessConfig->u_ReConfigFlag = TRUE;
       /*Some of the Sessions are deleted check for change in
       ISI or Position method and update receiver if required*/
#ifdef LARGE_UPDATE_RATE
       if(gpsc_sess_sleep_estimate(p_zGPSCControl, u_FixNoFix)==TRUE)
	      return;
#endif
	}
    else if(sentResult)
    {
#ifdef LARGE_UPDATE_RATE
       if(gpsc_sess_sleep_estimate(p_zGPSCControl, u_FixNoFix)==TRUE)
	      return;
#endif
    }
	/* Check for normal session or geofence session report config 0 means Geofence session */
	U32 rep_config=gpsc_get_gf_report_config(p_zSessConfig,p_zSessSpecificCfg);
	if(rep_config == 0) 
   { /* Geofence session */
	if(p_zGPSCConfig->apm_control == 0  || u_UpdateConfig)
	{
		if (p_zSessConfig->u_ReConfigFlag == TRUE)
		{
			gpsc_sess_update_period(p_zGPSCControl, C_PERIOD_NOTSET);
			gpsc_sess_update_timeout(p_zGPSCControl);
			p_zSessConfig->u_NullRepFlag = TRUE;
			p_zSessConfig->u_ReConfigFlag = FALSE;
		}
		if (u_UpdateConfig)
		{
			gpsc_app_update_apm_config(p_zGPSCControl);
			u_UpdateConfig=FALSE;
		}
	}
}
else 
{
/* Normal session */
	if (p_zSessConfig->u_ReConfigFlag == TRUE)
   {
		gpsc_sess_update_period(p_zGPSCControl, C_PERIOD_NOTSET);
		gpsc_sess_update_timeout(p_zGPSCControl);
		p_zSessConfig->u_NullRepFlag = TRUE;
		p_zSessConfig->u_ReConfigFlag = FALSE;
    }
}
    /* update period expiry timer value */
    p_zSessConfig->q_PeriodicExpTimerVal = ((p_zSessConfig->w_AvgReportPeriod * 2000) + C_GPSC_TIMEOUT_PERIODIC_REPORT);

	/* Restart the Periodic timer if any session is MSB. */
	gpsc_timer_start(p_zGPSCControl, C_TIMER_PERIODIC_REPORTING, p_zSessConfig->q_PeriodicExpTimerVal);
}

void gpsc_mgp_rx_proc_pos
(
   gpsc_ctrl_type*  p_zGPSCControl,
   U8           u_FixNoFix
)
{
  gpsc_state_type*           p_zGPSCState         = p_zGPSCControl->p_zGPSCState;
  gpsc_sess_cfg_type*	     p_zSessConfig        = p_zGPSCControl->p_zSessConfig;
  gpsc_session_status_type*  p_zSessionStatus     = &p_zGPSCState->z_SessionStatus;

  gpsc_db_type*              p_zGPSCDatabase      = p_zGPSCControl->p_zGPSCDatabase;
  gpsc_db_pos_type*          p_DBPos              = &p_zGPSCDatabase->z_DBPos;
  gpsc_report_pos_type*      p_ReportPos          = &p_DBPos->z_ReportPos;
  gpsc_sess_specific_cfg_type* p_zSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;
  gpsc_custom_struct*		p_zGPSCCustom = p_zGPSCControl->p_zCustomStruct;

  /* Below variables helps holding position report till N sec after first fix, before
     reporting back to server. This is currently defaulted to 0, meaning report as soon
	 as fix is achieved. This could be set to some other number later.
  */
  if (p_zSessSpecificCfg == NULL)
	  return;

  if (p_zGPSCCustom->custom_pos_rep_ext_req_flag)
  { /* Send the pos rep directly if it has been requested */
	  gpsc_send_position_result(p_zGPSCControl, TRUE, p_zSessSpecificCfg);
  }

  if ( p_zGPSCControl->p_zGPSCState->u_SensorOpMode != C_RC_ON)
  {
     return; /* don't do anything if Receiver is not ON */
  }

  /* Will get cleared if this event results in a report to SIA */
  p_zSessionStatus->w_NumUnRptFixEvt++;

  /****************************** OK, session goes on ... ****/

  /* GPS 0.5.10.3 has a bug that causes it to report a fix with only
     velocity flag as set. Add a workaround for this */

  if (u_FixNoFix &&
	 ((p_ReportPos->w_PositionFlags & NF_FIX_2D) != NF_FIX_2D) &&
	  ((p_ReportPos->w_PositionFlags & NF_FIX_3D) != NF_FIX_3D)
	 )
  {
     u_FixNoFix = 0; /* treat as a no fix*/
  }


  /* AI2 reports from its periodic position report fixes that are externally
     injected, not-updated, or with ITAR violation, treat these fixes as
     no-fix for SIA reporting purposes **/
  if (u_FixNoFix &&
     (p_ReportPos->w_PositionFlags & (NF_NOT_UPDATED | NF_EXTERNAL_UPDATE | NF_ITAR_VIOLATION) ) )
  {
     u_FixNoFix = 0; /* treat as a no fix, even thought it was not reported as NoNewFix */
  }

  /* inject an expanded A2 when a first fix is made*/
  /* avoid cases where an external update is made*/
  if ((u_FixNoFix == TRUE)&&(p_zGPSCState->u_firstfix == FALSE)&&
	     !(p_ReportPos->w_PositionFlags & (NF_NOT_UPDATED | NF_EXTERNAL_UPDATE | NF_ITAR_VIOLATION))&&
	     	 (((p_ReportPos->w_PositionFlags & NF_FIX_2D) == NF_FIX_2D) || (p_ReportPos->w_PositionFlags & NF_FIX_3D) == NF_FIX_3D))
  	{
  		p_zGPSCState->u_firstfix = TRUE;

		gpsc_loc_fix_qop zQop;
		zQop.u_HorizontalAccuracy = C_ACCURACY_MAXOUT/2;
		zQop.w_MaxResponseTime = 1;

		/*inject*/
		gpsc_mgp_tx_inject_timeout_cep_info(p_zGPSCControl, &zQop);
  	}

   gpsc_check_session_for_fix(p_zGPSCControl, u_FixNoFix);
}

/*===========================================================================

FUNCTION
  GPSC_MGP_RX_PROC_NMEA

DESCRIPTION
  This function Ssends NMEA results to requestor.

===========================================================================*/
void gpsc_mgp_rx_proc_nmea(gpsc_ctrl_type*  p_zGPSCControl)
{
    gpsc_sess_cfg_type*		   p_zSessConfig	       = p_zGPSCControl->p_zSessConfig;
    gpsc_sess_specific_cfg_type* p_zSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;

	 if (p_zSessSpecificCfg == NULL) return;

	 if ( p_zGPSCControl->p_zGPSCState->u_SensorOpMode != C_RC_ON) return;

	 while (p_zSessSpecificCfg != NULL)
	 {
		 /* send nmea measurement if requested through bitmap */
		 if (p_zSessSpecificCfg->w_ResultTypeBitmap & NMEA_MASK)
       {
          gpsc_send_nmea_result(p_zGPSCControl, p_zSessSpecificCfg);
       }

		 p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type*) p_zSessSpecificCfg->p_nextSession;
	 }

}


/*===========================================================================

FUNCTION
  GPSC_MGP_RX_PROC_MEAS

DESCRIPTION
  This function determines if a measurement report is needed to send to
  SIA/SMLC.  If so,  calls the functions to carry out.  In either case,
  properly handles state variables.

PARAMETERS
  u_MeasNoMeas - 1: New measurement available; 0: No new measurement

===========================================================================*/
void gpsc_mgp_rx_proc_meas
(
  gpsc_ctrl_type*  p_zGPSCControl,
  U8              u_MeasNoMeas
)
{

	gpsc_sess_cfg_type*		   p_zSessConfig	   = p_zGPSCControl->p_zSessConfig;
	gpsc_state_type*             p_zGPSCState       = p_zGPSCControl->p_zGPSCState;
	gpsc_session_status_type*    p_zSessionStatus  = &p_zGPSCState->z_SessionStatus;

	gpsc_db_type*                p_zGPSCDatabase    = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_db_gps_clk_type*        p_zDBGpsClock     = &p_zGPSCDatabase->z_DBGpsClock;
	gpsc_db_gps_meas_type*       p_zDBGpsMeas      = &p_zGPSCDatabase->z_DBGpsMeas;
	gpsc_meas_report_type*       p_zMeasReport     = &p_zDBGpsMeas->z_MeasReport;
	gpsc_meas_stat_report_type*  p_zMeasStatReport =
	                            &p_zDBGpsMeas->z_MeasStatReport;


	gpsc_sess_specific_cfg_type* p_zSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;
        gpsc_event_cfg_type * p_zEventCfg = p_zGPSCControl->p_zEventCfg;


    gpsc_cfg_type*			p_zGPSCConfig	= p_zGPSCControl->p_zGPSCConfig;
	gpsc_smlc_assist_type*  p_zSmlcAssist  = p_zGPSCControl->p_zSmlcAssist;


	U8 u_NodeDeleted = FALSE;
	U8 u_WaitFirstFix= FALSE;
	U8 u_GoodMeasQuality  = FALSE;
	U8 sentResult=FALSE;
	S32 q_FCountDiff = 0;
   FLT  f_pdop = 0;


   if (p_zSessSpecificCfg == NULL)
   {
    	STATUSMSG("gpsc_mgp_rx_proc_meas:Status: p_zSessSpecificCfg == NULL");
		return;
   }

	if ( p_zGPSCControl->p_zGPSCState->u_SensorOpMode != C_RC_ON)
	{
	    STATUSMSG("ERROR: gpsc_mgp_rx_proc_meas Receiver not in C_RC_ON");
	    return; /* don't do anything if Receiver is not ON */
	}

	// Will get cleared if this event results in a report to SIA
	p_zSessionStatus->w_NumUnRptMsrEvt++;

	if ( !p_zMeasReport->u_Valid || !p_zMeasStatReport->u_Valid ||
		    (p_zMeasReport->q_FCount != p_zMeasStatReport->q_FCount) ||
		  (p_zDBGpsClock->q_FCount != p_zMeasReport->q_FCount)
		  )
		{
         		//return;
			 /*Dont return, keep processing so we can timeout*/

		}
#ifdef CLIENT_CUSTOM

	  p_zMeasReport->u_IsValidMeas = gpsc_meas_qualchk( p_zGPSCControl, &f_pdop);
	  u_GoodMeasQuality = TRUE;
#else //if the meas is good encode a response to rrlp, and if succesful send it to smlc
	  if (gpsc_meas_qualchk( p_zGPSCControl, &f_pdop))
	  {
		 u_GoodMeasQuality = TRUE;
	  }
#endif

	/* start null report handling */
	if(p_zSessConfig->q_LastFCountMeas == GPSC_INVALID_FCOUNT)
      q_FCountDiff = 1;
    else
      q_FCountDiff = (p_zSessConfig->q_FCountMeas - p_zSessConfig->q_LastFCountMeas);

	/* Checking for NULL Report. NULL/Unexpected report are reported by receiver immediately
	  when there is a change of reporting period and event configuration */

	/* check if measurement is of good quality, mark NULL report as FALSE */
	if (u_MeasNoMeas && u_GoodMeasQuality)
	{
		/* reset re-acquisition counter for MSA case if good measurement is received */
	    if( (p_zSessConfig->u_SmlcAssisPermit == C_SMLC_ASSIS_PERMITTED) &&
			(p_zSessSpecificCfg->u_PosCompEntity == C_SMLC_COMPUTED))
	    {
            /* initialize the counter to 0 when got good fix */
		    p_zGPSCState->u_CountInvalidFix = 0;
	    }

	}

#ifdef NULL_REPORT
	if(p_zSessConfig->u_NullRepFlag == TRUE)
	{

		if(p_zSessConfig->u_PosCompEntity == C_SMLC_COMPUTED)
		{
		   p_zSessConfig->u_NullRepFlag = FALSE;
		}

		if( p_zSessConfig->w_OldAvgRepPeriod != p_zSessConfig->w_AvgReportPeriod)
		{
			STATUSMSG("Session Information: Ignoring Measurement NULL REPORT at SessionFCount:%ld, LastFCount:%ld, new RP:%d\n",
			p_zSessConfig->q_FCountMeas,p_zSessConfig->q_LastFCountMeas,p_zSessConfig->w_AvgReportPeriod);
		  
         return; //return as no need to process NULL report
		}

	} /* end of if(p_zSessConfig->u_NullRepFlag == TRUE) */
	else
	{
        /*Update Fcount only when report is not NULL reoprt */
	    p_zSessConfig->q_LastFCountMeas = p_zSessConfig->q_FCountMeas;
	} /* end of else (p_zSessConfig->u_NullRepFlag == TRUE) */
#else
	/*Update Fcount only when report is not NULL reoprt */
	    p_zSessConfig->q_LastFCountMeas = p_zSessConfig->q_FCountMeas;
#endif

	while (p_zSessSpecificCfg != NULL)
	{
		/* send raw measurement if requested through bitmap */
		if (p_zSessSpecificCfg->w_ResultTypeBitmap & GPSC_RESULT_RAW)
		{
		   // sending RAW measurement only if there is no good measurement 
		   gpsc_send_measurement_result( p_zGPSCControl, TRUE, p_zSessSpecificCfg);
		}

		if (p_zSessSpecificCfg->u_PosCompEntity != C_SMLC_COMPUTED)
		{
		    p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;
		    continue;
		}

	  if (p_zSessSpecificCfg->w_FixCounter == 0)
	  {
		  // May be this session is no more required, so delete the session
 	      gpsc_sess_specific_cfg_type* p_zTempSessSpecificCfg= NULL;
		   p_zTempSessSpecificCfg =  p_zSessSpecificCfg;

		   p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;

		  // Node is getting deleted copy the next pointer
		  STATUSMSG("SessionInfo:TagId=0x%x No Reports required Deleting Session", p_zTempSessSpecificCfg->w_SessionTagId);

    	  gpsc_app_update_gf_config(p_zGPSCControl,p_zSessSpecificCfg);
		  gpsc_session_del_node(p_zSessConfig, p_zTempSessSpecificCfg->w_SessionTagId);
                  
        if (p_zSessConfig->u_BLF_State == C_BLF_STATE_ENABLED)
        {
           // If BLF got enabled, disable Extended reports
           p_zEventCfg->u_Ext_reports = 0;
			  p_zSessConfig->u_ReConfigFlag = TRUE;
        }
		  u_NodeDeleted=TRUE;

#ifdef LARGE_UPDATE_RATE
		  /* q_SleepDuration = 0 - if in case session sleep when Rp > 30 seconds */
		  p_zSessConfig->q_SleepDuration = 0;
#endif
		  continue;
	   }

		 // update the IsiCount with valid time interval
		 p_zSessSpecificCfg->q_PeriodElapsed = p_zSessSpecificCfg->q_PeriodElapsed + q_FCountDiff;

	    STATUSMSG("SessionInfo:MEAS SessTag=0x%x,TotSesion=%d, RP=%d,RPE=%d\n",
		 p_zSessSpecificCfg->w_SessionTagId,
		 p_zSessConfig->w_TotalNumSession,
		 p_zSessSpecificCfg->w_ReportPeriod,
		 p_zSessSpecificCfg->q_PeriodElapsed);

	    STATUSMSG("VFT=%d,FC=%d,TNFS=%d,WFF=%d, PTE=%d\n",
		 p_zSessSpecificCfg->q_ValidFixTimeout,
		 p_zSessSpecificCfg->w_FixCounter,
		 p_zSessSpecificCfg->w_TotalNumFixesPerSession,
		 p_zSessSpecificCfg->u_WaitFirstFix,
		 p_zSessConfig->q_PeriodicExpTimerVal);


		/** Hold the Meas report if Pdop is very high and still 5sec to timeout ***/
              if((u_GoodMeasQuality) && (f_pdop > 10.0f))
              {
				if ((  p_zSessSpecificCfg->q_ValidFixTimeout == 0) ||
				( p_zSessSpecificCfg->q_ValidFixTimeout > 5000  &&
	                      p_zSessSpecificCfg->q_PeriodElapsed<(p_zSessSpecificCfg->q_ValidFixTimeout-5000)))
				{
					STATUSMSG("------  f_pdop=%f  ----------", f_pdop);
	                      		u_GoodMeasQuality= FALSE;
					p_zMeasReport->u_IsValidMeas = FALSE;
					STATUSMSG("MGPRX_u_IsValidMeas=%d\n", p_zMeasReport->u_IsValidMeas);
				}

              }



		if (u_MeasNoMeas && u_GoodMeasQuality) /* for a NewMeasurement event */
		{
			if((p_zSessSpecificCfg->q_ValidFixTimeout != 0) &&
				(p_zSessSpecificCfg->q_PeriodElapsed >= (p_zSessSpecificCfg->q_ValidFixTimeout - C_FCOUNT_DIF_FACTOR)) )
			{
                                /* reset re-acquisition counter as good measurement is sent to NAVC */
                                if( (p_zSessConfig->u_SmlcAssisPermit == C_SMLC_ASSIS_PERMITTED) &&
                                        (p_zSessSpecificCfg->u_PosCompEntity == C_SMLC_COMPUTED))
                                {
                                        /* initialize the counter to 0 when got good fix */
                                        p_zGPSCState->u_CountInvalidFix = 0;
                                }
				/* send position report as report periodicity is matched */
				/* to support hybrid mode, here to add 3rd param as w_SessionTagId to send pos report to
				     perticular session as periodicity matched */
				STATUSMSG("Session Information: Session Tag Id=0x%x\t - Good Meas sent on Timeout",
					p_zSessSpecificCfg->w_SessionTagId);
				 if ( gpsc_send_measurement_result( p_zGPSCControl, TRUE, p_zSessSpecificCfg))
				  {


					 p_zSessSpecificCfg->w_FixCounter--;

				     STATUSMSG("Status: Good set of PRM sent.Fix time=%d", p_zSessSpecificCfg->q_PeriodElapsed);
		             		p_zSessSpecificCfg->q_PeriodElapsed=0;

		             /* This shall be used for first fix only */
					  p_zGPSCState->u_FcountDelta = 0;

					  p_zSessSpecificCfg->u_WaitFirstFix=FALSE;
					  sentResult=TRUE;
					  if (p_zSessConfig->u_PosCompEntity == C_SMLC_COMPUTED)
					  {
						   p_zSessConfig->w_OldAvgRepPeriod = p_zSessConfig->w_AvgReportPeriod;
					  }

				  }
			}
			else if (p_zSessSpecificCfg->u_WaitFirstFix)
			{
                                /* reset re-acquisition counter as good measurement is sent to NAVC */
                                if( (p_zSessConfig->u_SmlcAssisPermit == C_SMLC_ASSIS_PERMITTED) &&
                                        (p_zSessSpecificCfg->u_PosCompEntity == C_SMLC_COMPUTED))
                                {
                                        /* initialize the counter to 0 when got good fix */
                                        p_zGPSCState->u_CountInvalidFix = 0;
                                }

				if (gpsc_send_measurement_result( p_zGPSCControl, TRUE, p_zSessSpecificCfg))
				{
					  p_zSessSpecificCfg->w_FixCounter--;
				     p_zSessSpecificCfg->q_PeriodElapsed=0;

				     // This shall be used for first fix only
					  p_zGPSCState->u_FcountDelta = 0;
				}
			   p_zSessSpecificCfg->u_WaitFirstFix=FALSE;
				u_WaitFirstFix = TRUE;
				sentResult = TRUE;

				if (p_zSessConfig->u_PosCompEntity == C_SMLC_COMPUTED)
					p_zSessConfig->w_OldAvgRepPeriod = p_zSessConfig->w_AvgReportPeriod;
			}



		}
		else
		{
#ifdef LARGE_UPDATE_RATE
			/* in case of time out */
			if(p_zSessConfig->q_SleepDuration)
			{
				STATUSMSG("**** Session Info:No Good Measurement after Session Wakeup at this time ***** \n");

				STATUSMSG("RP=%d,RPE=%d,VFT=%d\n",p_zSessSpecificCfg->w_ReportPeriod,
					p_zSessSpecificCfg->q_PeriodElapsed,p_zSessSpecificCfg->q_ValidFixTimeout);

				/*traversing to next node*/
				p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;
				continue;
			}
#endif

			/* if no valid measurement report sent, regardless what caused that,
		 	    check if it's time to send to SMLC RRLP-LocationError **/

			/* if meas quality is not good and q_PeriodElapsed exceeds its reporting time, then send reporting error */
			if((p_zSessSpecificCfg->q_ValidFixTimeout != 0) &&
				(p_zSessSpecificCfg->q_PeriodElapsed >= (p_zSessSpecificCfg->q_ValidFixTimeout - C_FCOUNT_DIF_FACTOR)) )
			{
				p_zSessSpecificCfg->w_FixCounter--;
                STATUSMSG("Session Information: Session Tag Id=0x%x - Measurement is of Not Good Quality at TIMEOUT\n",
					p_zSessSpecificCfg->w_SessionTagId);
				/*The below function call is to check the good SVs without SV time
				  in case of time out so that allow the meas report to sent to n/w */
				gpsc_meas_qualchk_sft_timeout(p_zGPSCControl);
			/* It is time to send back a location error */
				gpsc_send_measurement_result( p_zGPSCControl, TRUE, p_zSessSpecificCfg);



				if (p_zSessSpecificCfg->u_WaitFirstFix)
				{
					p_zSessSpecificCfg->u_WaitFirstFix=FALSE;
					u_WaitFirstFix=1;
				}


				 /* to calculate the receiver time out cases for re acquisition */
			    if (p_zSessConfig->u_SmlcAssisPermit == C_SMLC_ASSIS_PERMITTED)
				 {
				    if (p_zGPSCState->u_CountInvalidFix != UNDEFINED_U8)
					 {
					    p_zGPSCState->u_CountInvalidFix++;
						 if (p_zGPSCState->u_CountInvalidFix == p_zGPSCConfig->count_invalid_fix)
						 {
						    ALOGI("+++ %s: Re-Request for MSB assistance. Number of consecutive Time-outs [%d] +++/n",
                         __FUNCTION__, p_zGPSCState->u_CountInvalidFix);

							 p_zSmlcAssist->u_NumAssistAttempt = 0;
							 if (gpsc_request_assistance(p_zGPSCControl) != GPSC_SUCCESS)
                         ALOGE("+++ %s: Assistance Request Failure +++\n", __FUNCTION__);
                      else
							    p_zGPSCState->u_CountInvalidFix = UNDEFINED_U8;
						}
					}
				}
			}

		 }


		if (p_zSessSpecificCfg->w_TotalNumFixesPerSession == 0)
		{
            /*This is true in case of infinite reports*/
			p_zSessSpecificCfg->w_FixCounter=1;

		}
		else if(p_zSessSpecificCfg->w_FixCounter== 0)
		{
 	        gpsc_sess_specific_cfg_type* p_zTempSessSpecificCfg= NULL;
		    p_zTempSessSpecificCfg =  p_zSessSpecificCfg;
            STATUSMSG("Session-0x%x, SessionNext-0x%x",p_zSessSpecificCfg,p_zSessSpecificCfg->p_nextSession );
		    p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;
		  /*Node is getting deleted copy the next pointer*/
//		    gpsc_app_loc_fix_stop_cnf(GPSC_SUCCESS, p_zTempSessSpecificCfg->w_SessionTagId);
			STATUSMSG("No Meas Reports required Deleting TagId: %d", p_zTempSessSpecificCfg->w_SessionTagId);
			  gpsc_app_update_gf_config(p_zGPSCControl,p_zSessSpecificCfg);
			gpsc_session_del_node(p_zSessConfig, p_zTempSessSpecificCfg->w_SessionTagId);
                        if(p_zSessConfig->u_BLF_State == C_BLF_STATE_ENABLED)
                        {
                        	/* If BLF got enabled, disable Extednded reports */
                        	p_zEventCfg->u_Ext_reports = 0;
				p_zSessConfig->u_ReConfigFlag = TRUE;
                    	}
			u_NodeDeleted=TRUE;

			continue;
		}

	p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;
	}

/* check if there is no session exists */
   if(p_zSessConfig->w_TotalNumSession == 0)
   {
      gpsc_sess_end(p_zGPSCControl);
	  return;
   }
   else  if((u_NodeDeleted) || (u_WaitFirstFix))
   {

 	  /* update re configuration only if all sessions are MS Assisted */
       if(p_zSessConfig->u_PosCompEntity == C_SMLC_COMPUTED)
		  {
		  p_zSessConfig->u_ReConfigFlag = TRUE;
		  }
   }
   else if(sentResult)
   {

      /*Some of the Sessions are deleted check for change in
      ISI or Position method and update receiver if required*/
#ifdef LARGE_UPDATE_RATE
      if(gpsc_sess_sleep_estimate(p_zGPSCControl, u_MeasNoMeas)==TRUE)
		  return;
#endif
   }

   /* update re configuration only if all sessions are MS Assisted */
   if(p_zSessConfig->u_PosCompEntity == C_SMLC_COMPUTED)
   {
	   if (p_zSessConfig->u_ReConfigFlag == TRUE)
	   {

		 gpsc_sess_update_period(p_zGPSCControl, C_PERIOD_NOTSET);
		 gpsc_sess_update_timeout(p_zGPSCControl);
		 p_zSessConfig->u_NullRepFlag = TRUE;
		 p_zSessConfig->u_ReConfigFlag = FALSE;

		 /* update period expiry timer value */
	     p_zSessConfig->q_PeriodicExpTimerVal = ((p_zSessConfig->w_AvgReportPeriod * 1000) + C_GPSC_TIMEOUT_PERIODIC_REPORT);
	   }
   }

   /* update period expiry timer value only if all sessions are MSA */
 //     if (p_zSessConfig->u_PosCompEntity == C_SMLC_COMPUTED)
  gpsc_timer_start(p_zGPSCControl, C_TIMER_PERIODIC_REPORTING, p_zSessConfig->q_PeriodicExpTimerVal);
	// Restart the Measurement Tracking upon valid measurement 

   /*  THIS CAUSES MEMORY CORRUPTION AND UNSTABLE STATE OF SOFTWARE. WHATEVER THIS CRAP IS 
       SUPPOSED TO DO, IT MUST BE COMMENTED OUT 
	if (gpsc_timer_start(p_zGPSCControl, C_TIMER_MEAS_TRACKING,
        (p_zSessConfig->w_AvgReportPeriod * 1000 + C_GPSC_TIMEOUT_MEAS_TRACKING)) != GPSC_SUCCESS)
   {
      ALOGE("+++ %s: Measurement Tracking Timer (%d) could not be re-started +++\n", __FUNCTION__, C_TIMER_MEAS_TRACKING);
   }*/

}
/*===========================================================================

FUNCTION
  GPSC_MGP_RX_PROC_ASYN_EVT

DESCRIPTION
  This function processes asynchronous event reports from Sensor

===========================================================================*/
#define C_MAX_NEW_ALM 28

void gpsc_mgp_rx_proc_asyn_evt
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*        p_zAi2Field
)
{
  U8                           u_EventType              = *p_zAi2Field->p_B;
  U8                           u_SvId                   = 0;
  gpsc_state_type*	          p_zGPSCState             = p_zGPSCControl->p_zGPSCState;
  gpsc_sess_cfg_type*          p_zSessConfig		        = p_zGPSCControl->p_zSessConfig;
  gpsc_cfg_type*               p_zGPSCConfig            = p_zGPSCControl->p_zGPSCConfig;
  gpsc_dyna_feature_cfg_type*  p_zGPSCDynaFeatureConfig = p_zGPSCControl->p_zGPSCDynaFeatureConfig;
  gpsc_sess_specific_cfg_type* p_zSessSpecificCfg       = NULL;
  U32                          rep_config               = gpsc_get_gf_report_config(p_zSessConfig,p_zSessSpecificCfg);
  U8*                          p_B                      = p_zAi2Field->p_B;


  if (u_EventType == AI_EVENT_EXT_HW)
	 gpsc_time_pulse_event_received(p_zGPSCControl,p_zAi2Field);

  switch (u_EventType)
  {
    case AI_EVENT_ENGINE_ON:
		{
         ALOGI("+++ %s: AI_EVENT_ENGINE_ON +++\n", __FUNCTION__);

			p_B += 2;
			p_zGPSCState->q_FCountRecOnFirstAssistToLocReq = NBytesGet (&p_B, sizeof(U32));

			p_zGPSCControl->p_zGPSCState->u_SensorOpMode = C_RC_ON;
			p_zGPSCControl->p_zGPSCState->u_SensorOpModeConfirmed = TRUE;
		}
		break;

    case AI_EVENT_ENGINE_OFF:
		{
			McpU32 temp = 0;

         ALOGI("+++ %s: AI_EVENT_ENGINE_OFF +++\n", __FUNCTION__);

			p_B += 2;
			temp = NBytesGet (&p_B, sizeof(U32));

			p_zGPSCControl->p_zGPSCState->u_SensorOpMode = C_RC_OFF;
			p_zGPSCControl->p_zGPSCState->u_SensorOpModeConfirmed = TRUE;

			gpsc_sleep_sequence(p_zGPSCControl,E_GPSC_SEQ_GPS_WAIT_SLEEP);
    	}
		break;

    case AI_EVENT_ENGINE_IDLE:
		{
			McpU32 temp = 0;
			p_B += 2;

         ALOGI("+++ %s: AI_EVENT_ENGINE_IDLE +++\n", __FUNCTION__);

			temp = NBytesGet (&p_B, sizeof(U32));
		   p_zGPSCState->q_FCountRecOnFirstAssistToLocReq = 0;

			p_zGPSCControl->p_zGPSCState->u_SensorOpMode = C_RC_IDLE;
			p_zGPSCControl->p_zGPSCState->u_SensorOpModeConfirmed = TRUE;

    	}

      break;

    case AI_EVENT_NEW_ALM:
    {
        p_zAi2Field->p_B++;
        u_SvId = *p_zAi2Field->p_B;
        p_zSessConfig->u_SvNewAlmCount++;

        /* Why in the world this magic numbers? Heaven knows
	     if ((u_SvId >= 193) && (u_SvId <= 202))
		     return; */

        ALOGV("+++ %s: New Almanac. SV ID: %d +++\n", __FUNCTION__, u_SvId);

        /* Check if we are in APM mode */
        if ((p_zGPSCDynaFeatureConfig->feature_flag & GPSC_FEAT_APM) && (p_zGPSCDynaFeatureConfig->apm_control == 1))
        {
            /* Chip might be sleeping. Send a wake up command */
            gpsc_wakeup_receiver(p_zGPSCControl);
        }

        // Logic here is presumably - when AI2 reports new almanac event for specific SV,
        // it requests actual data
	     if (rep_config != 0) /* normal session */
		  {
		     gpsc_mgp_tx_req( p_zGPSCControl, AI_REQ_ALM, u_SvId );
			  gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK);
		  }
	     else 
        {   /* Geofence Session */
	         if (p_zGPSCState->u_GeofenceAPMEnabled == 0)
	         {
			      gpsc_mgp_tx_req( p_zGPSCControl, AI_REQ_ALM, u_SvId );
			      gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK);
	         }
	     }

        /* once all almanac received, indicate to GPSM */
        if (p_zSessConfig->u_SvNewAlmCount >= C_MAX_NEW_ALM)
        {
           gpsc_app_alm_assist_complete_ind();
           p_zSessConfig->u_SvNewAlmCount = 0;
        }
    }
    break;

    case AI_EVENT_NEW_EPHEM:
    {
        p_zAi2Field->p_B++;
        u_SvId = *p_zAi2Field->p_B;
	  
        /* Why in the world these magic numbers???
        if ((u_SvId >= 193) && (u_SvId <= 202))
	        return; */

        ALOGV("+++ %s: New Ephemeris Event. SV ID: %d +++\n", __FUNCTION__, u_SvId);

        /* Check if we are in APM mode */
        if ((p_zGPSCDynaFeatureConfig->feature_flag & GPSC_FEAT_APM) && (p_zGPSCDynaFeatureConfig->apm_control == 1))
        {
            /* Chip might be sleeping. Send a wake up command */
            gpsc_wakeup_receiver(p_zGPSCControl);
        }
		
        // logic here is presumably: When AI2 chip asynchronously reports new Ephemeris for 
        // particular SV ID, it requests actual data -- and then upon receipt of AI_REP_EPHEM packet 
        // it somehow serializes it in /data/gps/aiding
        if (rep_config != 0) /* normal session */
		  {
		     gpsc_mgp_tx_req( p_zGPSCControl, AI_REQ_EPHEM, u_SvId );
			  gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK);
		  }
		  else 
        { /* Geofence Session */
			  if (p_zGPSCState->u_GeofenceAPMEnabled == 0)
			  {
			     gpsc_mgp_tx_req( p_zGPSCControl, AI_REQ_EPHEM, u_SvId );
			     gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK);
			  }
		}
    }
    break;

    case AI_EVENT_NEW_IONO_UTC:
    {
        ALOGI("+++ %s: AI_EVENT_NEW_IONO_UTC +++\n", __FUNCTION__);
        /* Check if we are in APM mode */
        if ((p_zGPSCDynaFeatureConfig->feature_flag & GPSC_FEAT_APM) && (p_zGPSCDynaFeatureConfig->apm_control == 1))
        {
            /* Chip might be sleeping. Send a wake up command */
            gpsc_wakeup_receiver(p_zGPSCControl);
        }
		
        if (rep_config != 0) /* normal session */
		  {
				  gpsc_mgp_tx_req( p_zGPSCControl, AI_REQ_IONO, 0 );
				  gpsc_mgp_tx_req( p_zGPSCControl, AI_REQ_UTC, 0 );
				  gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK);
		  }
		  else 
        { 
              /* Geofence Session */
		        if (p_zGPSCState->u_GeofenceAPMEnabled == 0)
		        {
			        gpsc_mgp_tx_req( p_zGPSCControl, AI_REQ_IONO, 0 );
			        gpsc_mgp_tx_req( p_zGPSCControl, AI_REQ_UTC, 0 );
			        gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK);
		        }
	     }
    }
    break;

    case AI_EVENT_NO_NEW_POS:
    // it is guaranteed that we get either this or NewPos at 1 Hz 
    {
      p_B++;
      /* Print out the reason for no new position */
      ALOGV("+++ %s: AI_EVENT_NO_NEW_POS +++ %d\n", __FUNCTION__, *p_B);
      // leave 1 byte for event data, and 1 byte for proper fCount value
      p_B++;

      // now get FCount even if there is no position, store in Session DB for NULL report 
      p_zSessConfig->q_FCount = NBytesGet (&p_B, sizeof(U32));

      gpsc_mgp_rx_proc_pos( p_zGPSCControl, 0 );

      break;
    } 

    case AI_EVENT_NO_NEW_MEAS:
    {
      ALOGI("+++ %s: AI_EVENT_NO_NEW_MEAS +++\n", __FUNCTION__);
      p_B++;

      gpsc_mgp_rx_proc_meas( p_zGPSCControl, 0 );

      break;
    }

     case AI_EVENT_GPS_AWAKE:
     {
         ALOGI("+++ %s: GPS Is Awake +++\n", __FUNCTION__);

	      if (p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_WAIT_GPS_WAKEUP)
	      {
		      /* Wait for Device Calibration time ~900ms to precisely sync-up with actual wake-up time */
		      /* possible wake up from loc fix req */
		      p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_GPS_WAKEUP;
		      gpsc_wakeup_sequence(p_zGPSCControl,E_GPSC_SEQ_GPS_WAKEUP);
	      }
	      else
	      {
				if (p_zGPSCControl->p_zGpscSm->e_GpscCurrState != E_GPSC_STATE_ACTIVE)
		         p_zGPSCControl->p_zGpscSm->e_GpscCurrState = E_GPSC_STATE_IDLE;

		      if (p_zGPSCConfig->low_power_state != LOW_POWER_MODE_SLEEP)
			      NavcCmdEnableQueue(p_zGPSCControl->p_zSysHandlers->hNavcCtrl);
	      }
      }
      break;


    default:
    //   ALOGI("+++ %s: Not Processing Asynchronous Event [0x%X] +++\n", __FUNCTION__, u_EventType );
       break;
    }
}


void gpsc_mgp_rx_proc_dwld_rec_res
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*        p_zAi2Field
)
{
	gpsc_state_type* p_zGPSCState = p_zGPSCControl->p_zGPSCState;
	gpsc_patch_status_type*	p_zPatchStatus    =  &p_zGPSCState->z_PatchStatus;
	U16 w_RecordNumber;
	U8  u_RecordResult;
	U8*  p_Buff;

	p_Buff = p_zAi2Field->p_B;
	w_RecordNumber = (U16)((p_Buff[0]<<8) + p_Buff[1]);
	u_RecordResult = p_Buff[2];

	if(w_RecordNumber == p_zPatchStatus->w_NextRecord)
	{
		switch(u_RecordResult)
		{
		case C_AI2_REC_RESULT_OK:
			p_zPatchStatus->w_NextRecord ++;
			STATUSMSG("Status: Acknowledgement for record number %d",w_RecordNumber);
#ifdef GPSC_DEBUG
			printf("\n Status: Acknowledgement for record number %d",w_RecordNumber);
#endif
			p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_READY_READ_INJECT_RECORD;
			gpsc_ready_sequence(p_zGPSCControl, E_GPSC_SEQ_READY_READ_INJECT_RECORD);
			// STATECHANGE_REPORT_FERROR(C_GPSC_STATE_READ_INJECT_RECORD);
			return;
		case C_AI2_REC_RESULT_BADCHKSUM:
#ifdef GPSC_DEBUG
			printf("\n Error: Result for record %d : Bad checksum",w_RecordNumber);
#endif
			ERRORMSG2("Error: Result for record %d : Bad checksum",w_RecordNumber);
			break;
		case C_AI2_REC_RESULT_BADADDR:
#ifdef GPSC_DEBUG
			printf("\nError: Result for record  %d : Bad Address",w_RecordNumber);
#endif
			ERRORMSG2("Error: Result for record  %d : Bad Address",w_RecordNumber);
			break;
		case C_AI2_REC_RESULT_INAPPLICABLE:
#ifdef GPSC_DEBUG
			printf("\nError: Result for record  %d : Inapplicable Patch",w_RecordNumber,p_zPatchStatus->w_NextRecord);
#endif
			ERRORMSG3("Error: Result for record  %d : Inapplicable Patch",w_RecordNumber,p_zPatchStatus->w_NextRecord);
			break;
		}
	}
	else
	{
#ifdef GPSC_DEBUG
		printf("\n Warning : Rx Acknowledgement message for record number %d instead of %d",w_RecordNumber);
#endif
		ERRORMSG2("Warning : Rx Acknowledgement message for record number %d instead of %d",w_RecordNumber);
	}

	/*If error occured, continue without downloading patch*/
#ifdef GPSC_DEBUG
	printf("\n Error in Downloading skip the download process");
#endif
	p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_READY_SKIP_DOWNLOAD;
	// STATECHANGE_REPORT_FERROR(C_GPSC_STATE_SKIP_DOWNLOAD);
}

U8 gpsc_mgp_rx_proc_dwld_complete
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*       p_zAi2Field
)
{
	U8 u_Result = FALSE;
	switch(*p_zAi2Field->p_B)
	{
	/* download successful */
	case C_AI2_DWLD_SUCCESS:
#ifdef GPSC_DEBUG
		printf("\n Status: Download completed succesfully");
#endif
		gpsc_timer_stop(p_zGPSCControl, C_TIMER_SEQUENCE);
		STATUSMSG("Status: Status: Download completed succesfully");
		u_Result = TRUE;
		break;

	/* download fail */
	case C_AI2_DWLD_FAIL:
#ifdef GPSC_DEBUG
		printf("\n Error: Download failed skip download");
#endif
		ERRORMSG("Error: Download failed");
		u_Result = FALSE;
		break;

	/* Bad Field received in Download Complete message */
	default :
#ifdef GPSC_DEBUG
		printf("\n Error: Bad Field received in Download Complete message,skip download");
#endif
		ERRORMSG("Error: Bad Field received in Download Complete message");
		u_Result = FALSE;
	}

	return u_Result;
}

char* gpsc_populate_nmea_buffer
(
   gpsc_ctrl_type*  p_zGPSCControl,
   Ai2Field*        p_zAi2Field
)
{
	gpsc_result_type*  p_zResultBuffer = p_zGPSCControl->p_zResultBuffer;
	U16 w_Count = 4;
   char* p = p_zResultBuffer->p_sNmeaPointer;

	if (p_zGPSCControl->p_zGpscSm->e_GpscCurrState != E_GPSC_STATE_ACTIVE)
	{
		ALOGE("+++ %s: Warning: Unexpected NMEA sentence +++\n",  __FUNCTION__);
		return 0;
	}
 
	for (w_Count; w_Count < p_zAi2Field->w_Length; w_Count++)
	{
		*(p_zResultBuffer->p_sNmeaPointer++) = (S8)p_zAi2Field->p_B[w_Count];
	}
    
   *p_zResultBuffer->p_sNmeaPointer = '\0';

 /*  if ( (strncmp(p, "$GPRMC", 6) != 0) &&
        (strncmp(p, "$GPGGA", 6) != 0) &&
        (strncmp(p, "$GPGSA", 6) != 0) &&
        (strncmp(p, "$GPGSV", 6) != 0) )   return 0; */


	p_zResultBuffer->u_NmeaStringCount++;
   return p;

}

#define C_AI2_GPS_STATUS_LENGTH 22
#define C_AI2_GPS_ERROR_STATUS_LENGTH 22
#define GPS_STATUS_MSG_CALIBHW_PASS (1U<<0)
#define GPS_STATUS_MSG_UART1_PASS (1U<<1)
#define GPS_STATUS_MSG_UART2_PASS (1U<<2)
#define GPS_STATUS_MSG_I2C_1_PASS (1U<<3)
#define GPS_STATUS_MSG_I2C_2_PASS (1U<<4)
#define GPS_STATUS_MSG_ROMCHK_PASS (1U<<5)
#define GPS_STATUS_MSG_PATCH_DETECTED (1U<<15)

#define GPS_STATUS_MSG_EXCEP_UNDEFINSTR (0)
#define GPS_STATUS_MSG_EXCEP_SOFTINTR 	(1U<<10)
#define GPS_STATUS_MSG_EXCEP_ABORTPREF 	(1U<<11)
#define GPS_STATUS_MSG_EXCEP_ABORTDATA 	(1U<<11 | 1U<<10)
#define GPS_STATUS_MSG_EXCEP_RESVINTR 	(1U<<12)
#define GPS_STATUS_MSG_EXCEP_FIQINTR 	(1U<<12 | 1U<<10)
#define GPS_STATUS_MSG_EXCEP_RESV 		(1U<<12 | 1U<<11 )
#define GPS_STATUS_MSG_EXCEP_NONE 		(1U<<12 | 1U<<11 | 1U<<10 )
#define GPS_STATUS_MSG_EXCEP_WATCHDOG 	(1U<<13)

#define GPS_STATUS_MSG_EXCEP_MASK (1U<<13 | 1U<<12 | 1U<<11 | 1U<<10 )

#define GPS_STATUS_MSG_RAMROM_DETECTED (1U<<14)
#define GPS_STATUS_MSG_PATCH_DETECTED (1U<<15)

#define GPS_STATUS_MSG_CLKCFG_MASK		(0x3)

#define GPS_STATUS_MSG_HW_MASK		(0x70)


#define GPS_STATUS_PASS_MASK ( GPS_STATUS_MSG_CALIBHW_PASS |\
								GPS_STATUS_MSG_UART1_PASS |\
								GPS_STATUS_MSG_UART2_PASS |\
								GPS_STATUS_MSG_I2C_1_PASS |\
								GPS_STATUS_MSG_I2C_2_PASS)

void gpsc_report_error_status
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*    p_zAi2Field
)
{
	U16 w_FileLineNumber;
	U8  u_FileName[20];
	U8* p_FileName = &u_FileName[0];
	U8* p_B = p_zAi2Field->p_B;

	UNREFERENCED_PARAMETER(p_zGPSCControl);

	if (p_zAi2Field->w_Length == C_AI2_GPS_ERROR_STATUS_LENGTH)
	{
		/* get line number of file */
		w_FileLineNumber = (U16)NBytesGet(&p_B, sizeof(U16));

		/* get file name */
		p_FileName = (U8 *) NBytesGet(&p_B, sizeof(u_FileName));

		ALOGE("+++ %s: GPS RECEIVER ERROR: File: %s. Line Number: %d ", __FUNCTION__, u_FileName, w_FileLineNumber);
	}
}

T_GPSC_result gpsc_rx_examine_gps_status
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*    p_zAi2Field
)
{
	U16 w_GPSStatusWord;
	U8 u_ChipID;
	U16 w_Year;
	U8	u_SystemConfig;
	U8 *p_B = p_zAi2Field->p_B;
    T_GPSC_gps_ver_resp_params z_GPSVerControl,*p_zGPSVerControl = &z_GPSVerControl ;
	gpsc_cfg_type* p_zGPSCConfig = p_zGPSCControl->p_zGPSCConfig;
	gps_version *p_zGPSVersion = p_zGPSCControl->p_zGPSVersion;
	gpsc_state_type* p_zGPSCState       = p_zGPSCControl->p_zGPSCState;

	if(p_zAi2Field->w_Length == C_AI2_GPS_STATUS_LENGTH)
	{
		w_GPSStatusWord = (U16)NBytesGet(&p_B, sizeof(U16));
		/* Check of all the bits pass */
		if (w_GPSStatusWord & GPS_STATUS_MSG_PATCH_DETECTED )
		{
			p_zGPSCState->u_ValidPatch = 1;
		}
		else
           	p_zGPSCState->u_ValidPatch = 0;
		if(w_GPSStatusWord & GPS_STATUS_PASS_MASK != GPS_STATUS_PASS_MASK)
		{
			ERRORMSG2("Error: GPS Status Word is bad : %x",w_GPSStatusWord);
			if((w_GPSStatusWord & GPS_STATUS_MSG_CALIBHW_PASS)==0)
			{
				ERRORMSG("Error: Calibration hardware failed - Shutting down");
				return GPSC_GPS_BIST_FAIL;
			}
			if((w_GPSStatusWord & GPS_STATUS_MSG_UART1_PASS)==0)
			{
				if(p_zGPSCConfig->comm_config.ctrl_comm_config_union == GPSC_COMM_MODE_UART)
				{
					ERRORMSG("Error: UART1 failed - Shutting down");
					return GPSC_GPS_BIST_FAIL;
				}
				else
				{
					ERRORMSG("Error: UART1 failed but using I2C - so moving on");
					return GPSC_SUCCESS;
				}

			}
			if((w_GPSStatusWord & GPS_STATUS_MSG_UART2_PASS)==0)
			{
				ERRORMSG("Error: UART2 failed - Ignoring and moving on");
				return GPSC_SUCCESS;
			}
			if((w_GPSStatusWord & GPS_STATUS_MSG_I2C_1_PASS)==0)
			{
				if(p_zGPSCConfig->comm_config.ctrl_comm_config_union == GPSC_COMM_MODE_I2C)
				{
					ERRORMSG("Error: I2c-1 hardware failed - Shutting down");
					return GPSC_GPS_BIST_FAIL;
				}
				else
				{
					ERRORMSG("Error: I2c-1 failed but using UART1 - so moving on");
					return GPSC_SUCCESS;
				}
			}
			if((w_GPSStatusWord & GPS_STATUS_MSG_I2C_2_PASS)==0)
			{
				if(p_zGPSCConfig->comm_config.ctrl_comm_config_union == GPSC_COMM_MODE_I2C)
				{
					ERRORMSG("Error: I2c-2 hardware failed - Shutting down");
					return GPSC_GPS_BIST_FAIL;
				}
				else
				{
					ERRORMSG("Error: I2c-2 failed but using UART1 - so moving on");
					return GPSC_SUCCESS;
				}
			}

		}

		if((w_GPSStatusWord & GPS_STATUS_MSG_EXCEP_MASK) != GPS_STATUS_MSG_EXCEP_NONE)
		{
			ERRORMSG2("Error: GPS Exception Occured : %x",w_GPSStatusWord)
				return GPSC_GPS_EXCEPTION_RESET_FAIL;
		}

		/* Extract Chip ID*/
		u_ChipID = (U8)NBytesGet(&p_B, sizeof(U8));
		p_zGPSVersion->u_ChipIDMajorVerNum = (U8)((u_ChipID >> 4) & 0x0F);
		p_zGPSVersion->u_ChipIDMinorVerNum = (U8)(u_ChipID & 0x0F);


		/* Extract clock configuration info */
		u_SystemConfig = (U8)NBytesGet(&p_B, sizeof(U8));

		p_zGPSCState->u_SensorClockConfig = (U8)(u_SystemConfig & GPS_STATUS_MSG_CLKCFG_MASK);
		STATUSMSG("gpsc_rx_examine_gps_status: CLOCK CONFIGURATION = %d", p_zGPSCState->u_SensorClockConfig+1);
		U8 u_HwType;

		u_HwType=(U8)(u_SystemConfig & GPS_STATUS_MSG_HW_MASK);
		p_zGPSCControl->hw_type=(U8)((u_HwType>>4) & 0x07);
		STATUSMSG("gpsc_rx_examine_gps_status: HW = %d", p_zGPSCControl->hw_type);

		/* GPS ROM ID*/

		p_zGPSVersion->u_ROMIDSubMinor2VerNum = (U8)NBytesGet(&p_B, sizeof(U8));

		p_zGPSVersion->u_ROMIDSubMinor1VerNum = (U8)NBytesGet(&p_B, sizeof(U8));

		p_zGPSVersion->u_ROMIDMinorVerNum = (U8)NBytesGet(&p_B, sizeof(U8));

		p_zGPSVersion->u_ROMIDMajorVerNum = (U8)NBytesGet(&p_B, sizeof(U8));

		/* Month Day Year */
		p_zGPSVersion->u_ROMMonth = (U8)NBytesGet(&p_B, sizeof(U8)); /* month */
		p_zGPSVersion->u_ROMDay = (U8)NBytesGet(&p_B, sizeof(U8)); /* day */


		w_Year = (U16)NBytesGet(&p_B, sizeof(U16)); /* year */
		p_zGPSVersion->w_ROMYear = w_Year;
        /* if the version request is pending send the version to GPSM*/
		if (p_zGPSCState->u_GpsVersionPending)
		{
			p_zGPSVerControl->ChipIDMajorVerNum = p_zGPSVersion->u_ChipIDMajorVerNum;
			p_zGPSVerControl->ChipIDMinorVerNum = p_zGPSVersion->u_ChipIDMinorVerNum;
			p_zGPSVerControl->ROMDay = p_zGPSVersion->u_ROMDay;
			p_zGPSVerControl->ROMIDMajorVerNum = p_zGPSVersion->u_ROMIDMajorVerNum;
			p_zGPSVerControl->ROMIDMinorVerNum = p_zGPSVersion->u_ROMIDMinorVerNum;
			p_zGPSVerControl->ROMMonth = p_zGPSVersion->u_ROMMonth;
			p_zGPSVerControl->ROMIDSubMinor1VerNum = p_zGPSVersion->u_ROMIDSubMinor1VerNum;
			p_zGPSVerControl->ROMIDSubMinor2VerNum = p_zGPSVersion->u_ROMIDSubMinor2VerNum;
			p_zGPSVerControl->ROMYear = p_zGPSVersion->w_ROMYear;

			/*
			SAPCALL(gpsc_app_gps_ver_ind);
			gpsc_app_gps_ver_ind (p_zGPSVerControl);
			*/
			p_zGPSCState->u_GpsVersionPending = FALSE;
		}

		STATUSMSG("Status: GPS Status Message OK");
		return GPSC_SUCCESS;
	}
	else //Size is not right
		return GPSC_GPS_STATUS_MSG_FAIL;

}


#define C_AI2_SELFTEST_PASS 1

U8 gpsc_mgp_rx_proc_selftest_report
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*    p_zAi2Field
)
{
	if(p_zGPSCControl==NULL){;} //To avoid a warning
	if(p_zAi2Field->w_Length != 1)
		return FALSE;
	if(p_zAi2Field->p_B[0] != C_AI2_SELFTEST_PASS)
		return FALSE;
	return TRUE;
}

void gpsc_mgp_rx_prodlinetest_report
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*    p_zAi2Field
)
{

    U8 i,*p_B = p_zAi2Field->p_B;
    T_GPSC_rtc_osctest* p_RtcOscTest;
    T_GPSC_gps_osctest* p_GpsOscTest;
    T_GPSC_sigacq_test* p_SigAcqTest;
    T_GPSC_cw_test*     p_CwTest;
	T_GPSC_sync_test*   p_SyncTest;
	T_GPSC_self_test*   p_SelfTest;
  	T_GPSC_gpio_test * p_GpioTest;
	T_GPSC_ram_checksum_test *p_RamChecksumTest;
    gpsc_state_type* p_GPSCState = p_zGPSCControl->p_zGPSCState;
    T_GPSC_prodtest_response *p_prodtest_response;
    gpsc_result_type*	p_zResultBuffer = p_zGPSCControl->p_zResultBuffer;
    U32 q_tmp =0;

    p_prodtest_response = (T_GPSC_prodtest_response*)&p_zResultBuffer->s_ResultBuffer[0];

    //p_B++; //ignore first byte, as it is the sub packet ID
	/* stop PLT timer gpsc_mgp_rx_prodlinetest_report */
	gpsc_timer_stop(p_zGPSCControl,C_TIMER_PLT_SEQUENCE);

    switch( p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq )
	{

		case E_GPSC_SEQ_PLT:
		{
			switch( p_zAi2Field->u_Id )
			  {

			   case AI_CLAPP_REP_RTC_OSC_TEST_RES:
					p_RtcOscTest =  &p_prodtest_response->prodtest_response_union.rtc_osctest;
	   				STATUSMSG("Status:AI2 Report RTC Offset Test Result");
					p_prodtest_response->ctrl_prodtest_response_union = GPSC_RESULT_RTC_OSCTEST;
					p_RtcOscTest->rtcoscoffset = (S16)NBytesGet(&p_B, sizeof(S16));
       				p_GPSCState->u_ProductlinetestRequestPending = FALSE;
					gpsc_app_prodlinetest_ind(p_prodtest_response);

					gpsc_inject_gps_idle(p_zGPSCControl);
					break;

			   case AI_CLAPP_REP_RAM_CHKSUM_RES:
					p_RamChecksumTest =  &p_prodtest_response->prodtest_response_union.ram_checksum_test;
	   				STATUSMSG("Status:AI2 Report RTC Offset Test Result");
					p_prodtest_response->ctrl_prodtest_response_union = GPSC_RESULT_RAM_CHECKSUM_TEST;
					p_RamChecksumTest->checksumresult = (U8)NBytesGet(&p_B, sizeof(S16));
       					p_GPSCState->u_ProductlinetestRequestPending = FALSE;
					gpsc_app_prodlinetest_ind(p_prodtest_response);
					gpsc_inject_gps_idle(p_zGPSCControl);
					break;

			   case AI_CLAPP_REP_GPS_OSC_TEST_RES:
					p_GpsOscTest = &p_prodtest_response->prodtest_response_union.gps_osctest;
					STATUSMSG("Status:AI2 Report GPS Oscillotor Offset Test result");
					p_prodtest_response->ctrl_prodtest_response_union = GPSC_RESULT_GPS_OSCTEST;
					p_GpsOscTest->gpsoscoffset = (S16)(NBytesGet(&p_B, sizeof(S16)) * 1);
					p_GPSCState->u_ProductlinetestRequestPending = FALSE;
					gpsc_app_prodlinetest_ind(p_prodtest_response);
					gpsc_inject_gps_idle(p_zGPSCControl);
					break;
			   case AI_CLAPP_REP_GPS_SIGNAL_ACQ_TEST_RES:
					p_SigAcqTest = &p_prodtest_response->prodtest_response_union.sigacq_test;
   					STATUSMSG("Status:AI2 Report GPS Signal Acq Test Result");
					p_prodtest_response->ctrl_prodtest_response_union = GPSC_RESULT_SIGACQ_TEST;
					p_SigAcqTest->numofsv = (U8)NBytesGet(&p_B, sizeof(U8));

					for (i=0;i<p_SigAcqTest->numofsv; i++)
					{
					   p_SigAcqTest->svprn[i] = (U8)NBytesGet(&p_B, sizeof(U8));
					   p_SigAcqTest->svcno[i] = (S16)NBytesGet(&p_B, sizeof(S16));
					}
					p_GPSCState->u_ProductlinetestRequestPending = FALSE;
					gpsc_app_prodlinetest_ind(p_prodtest_response);
					gpsc_inject_gps_idle(p_zGPSCControl);
					break;

			   case AI_CW_TEST_RESULTS:

					p_CwTest = &p_prodtest_response->prodtest_response_union.cw_test;
					STATUSMSG("Status:AI2 Report CW Test Result message");
					p_prodtest_response->ctrl_prodtest_response_union = GPSC_RESULT_CW_TEST;

					p_CwTest->cwresponsetype = (U8)NBytesGet(&p_B, sizeof(U8));
					p_CwTest->totalpacket = (U16)NBytesGet(&p_B, sizeof(U16));
					p_CwTest->packetnumber = (U16)NBytesGet(&p_B, sizeof(U16));

					p_CwTest->num_wideband_peak = (U8)NBytesGet(&p_B, sizeof(U8));

					if(p_CwTest->num_wideband_peak != 0)
					{
						int i;
						p_CwTest->wideband_centfreq=(U32)NBytesGet(&p_B, 3); //to do: convert to 2's complement
						for (i=0;i<p_CwTest->num_wideband_peak;i++)
						{
							p_CwTest->wideband_peakinfo[i].wideband_peak_index=(U16)NBytesGet(&p_B, sizeof(U16));
							p_CwTest->wideband_peakinfo[i].wideband_peak_snr=(U16)NBytesGet(&p_B, sizeof(U16));
						}

					}

					p_CwTest->num_narrowband_peak = (U8)NBytesGet(&p_B, sizeof(U8));

					if(p_CwTest->num_narrowband_peak != 0)
					{
					int i;
					p_CwTest->narrowband_centfreq = (U32)NBytesGet(&p_B, 3); //to do: convert to 2's complement
					for(i=0; i<p_CwTest->num_narrowband_peak;i++)
						{
						p_CwTest->narrowband_peakinfo[i].narrowband_peak_index=(U16)NBytesGet(&p_B, sizeof(U16));
						p_CwTest->narrowband_peakinfo[i].narrowband_peak_snr=(U16)NBytesGet(&p_B, sizeof(U16));
						}
					}

					// this data will not come for tcxo and nf cw test
					/*
					p_CwTest->can1bin_peak = (U16)NBytesGet(&p_B, sizeof(U16));
					p_CwTest->can2to3bin_peak = (U16)NBytesGet(&p_B, sizeof(U16));
					p_CwTest->can4to7bin_peak = (U16)NBytesGet(&p_B, sizeof(U16));
					p_CwTest->can8to15bin_peak = (U16)NBytesGet(&p_B, sizeof(U16));
					p_CwTest->can16to31bin_peak = (U16)NBytesGet(&p_B, sizeof(U16));
					p_CwTest->can32to63bin_peak = (U16)NBytesGet(&p_B, sizeof(U16));
					p_CwTest->can64to127bin_peak = (U16)NBytesGet(&p_B, sizeof(U16));
					p_CwTest->can128to255bin_peak = (U16)NBytesGet(&p_B, sizeof(U16));
					p_CwTest->can256to511bin_peak = (U16)NBytesGet(&p_B, sizeof(U16));
					*/
                                        /* In NL5500 there is a mismatch in the reported bytes to the actual message definition
                                           The mismatch is in P1 dB numbers getting reported after the NF. These bytes should be discarded
                                           before reading the TCXO offset
                                        */

					if(p_CwTest->cwresponsetype == CW_RES_TYPE_STAT_REPORT)
					{

						p_CwTest->noise_figure = (U8)NBytesGet(&p_B, sizeof(U8));
                                                *p_B ++;
                                                *p_B ++;
                                                q_tmp = NBytesGet(&p_B, sizeof(U32));
						p_CwTest->tcxo_offset = (S32)(q_tmp);

						STATUSMSG("Status:AI2 Report CW Test Result NOISE FIGURE %d", p_CwTest->noise_figure);
						STATUSMSG("Status:AI2 Report CW Test Result TCXO OFFSET %d", p_CwTest->tcxo_offset);

					}


    				gpsc_app_prodlinetest_ind(p_prodtest_response);
					p_GPSCState->u_ProductlinetestRequestPending = FALSE;
					gpsc_inject_gps_idle(p_zGPSCControl);

					/*

				  McpU8                        noise_figure;
  McpU8                        tcxo_offset;
  T_GPSC_cwtest_widebandpeakinfo wideband_peakinfo;
  T_GPSC_cwtest_narrowbandpeakinfo narrowband_peakinfo;

					CW_RES_TYPE_1MHZ_IQ_SAMPLES=0,
					CW_RES_TYPE_2KHZ_IQ_SAMPLES,
					CW_RES_TYPE_WIDEBAND_FFT,
					CW_RES_TYPE_NARROWBAND_FFT,
					CW_RES_TYPE_STAT_REPORT,
					CW_RES_TYPE_2MHZ_IQ_SAMPLES,
					CW_RES_TYPE_8MHZ_IQ_SAMPLES,
					CW_RES_TYPE_32MHZ_IQ_SAMPLES,
					CW_RES_TYPE_32MHZ_IQ_SAMPLES_AFTER_NOTCH



					if ( p_CwTest->cwresponsetype == 0)   //1MHz IQ Sample
					{
						p_CwTest->totalpacket = (U16)NBytesGet(&p_B, sizeof(U16));
						p_CwTest->packetnumber = (U16)NBytesGet(&p_B, sizeof(U16));

					}
    				if ( p_CwTest->cwresponsetype == 1)   //2KHz IQ Sample
					{
						p_CwTest->totalpacket = (U16)NBytesGet(&p_B, sizeof(U16));
						p_CwTest->packetnumber = (U16)NBytesGet(&p_B, sizeof(U16));

					}
					if ( p_CwTest->cwresponsetype == 2)   //Wideband FFT
					{
						p_CwTest->totalpacket = (U16)NBytesGet(&p_B, sizeof(U16));
						p_CwTest->packetnumber = (U16)NBytesGet(&p_B, sizeof(U16));

					}
					if ( p_CwTest->cwresponsetype == 3)   //Narrowband FFT
					{
						p_CwTest->totalpacket = (U16)NBytesGet(&p_B, sizeof(U16));
						p_CwTest->packetnumber = (U16)NBytesGet(&p_B, sizeof(U16));

					}

					if ( p_CwTest->cwresponsetype == 4)   // status report
					{
						p_CwTest->totalpacket = (U16)NBytesGet(&p_B, sizeof(U16));
						p_CwTest->packetnumber = (U16)NBytesGet(&p_B, sizeof(U16));
						p_CwTest->num_wideband_peak = (U8)NBytesGet(&p_B, sizeof(U8));
						if( p_CwTest->num_wideband_peak > 0)
						{
						   p_CwTest->wideband_centfreq = (S32)NBytesGet(&p_B,3);
						   for(i=0;i<p_CwTest->num_wideband_peak;i++)
						   {
							  p_CwTest->wideband_peak_index[i] = (U16)NBytesGet(&p_B, sizeof(U16));
   							  p_CwTest->wideband_peak_snr[i] = (U16)NBytesGet(&p_B, sizeof(U16));

						   }
						}
						//narrowband
						p_CwTest->num_narrowband_peak = (U8)NBytesGet(&p_B, sizeof(U8));
						if( p_CwTest->num_narrowband_peak > 0)
						{
						   p_CwTest->narrowband_centfreq = (S32)NBytesGet(&p_B,3);
						   for(i=0;i<p_CwTest->num_narrowband_peak;i++)
						   {
							  p_CwTest->narrowband_peak_index[i] = (U16)NBytesGet(&p_B, sizeof(U16));
   							  p_CwTest->narrowband_peak_snr[i] = (U16)NBytesGet(&p_B, sizeof(U16));

						   }
						   //peak profile
						   p_CwTest->can1bin_peak = (U16)NBytesGet(&p_B, sizeof(U16));
						   p_CwTest->can2to3bin_peak = (U16)NBytesGet(&p_B, sizeof(U16));
						   p_CwTest->can4to7bin_peak = (U16)NBytesGet(&p_B, sizeof(U16));
						   p_CwTest->can8to15bin_peak = (U16)NBytesGet(&p_B, sizeof(U16));
						   p_CwTest->can16to31bin_peak = (U16)NBytesGet(&p_B, sizeof(U16));
						   p_CwTest->can32to63bin_peak = (U16)NBytesGet(&p_B, sizeof(U16));
						   p_CwTest->can64to127bin_peak = (U16)NBytesGet(&p_B, sizeof(U16));
						   p_CwTest->can128to255bin_peak = (U16)NBytesGet(&p_B, sizeof(U16));
						   p_CwTest->can256to511bin_peak = (U16)NBytesGet(&p_B, sizeof(U16));
						}

					}



					if( p_CwTest->cwresponsetype == 4)   //status report
					{
						gpsc_app_prodlinetest_ind(p_prodtest_response);
					   p_GPSCState->u_ProductlinetestRequestPending = FALSE;
					   gpsc_inject_gps_idle(p_zGPSCControl);
					}
*/
					break;

			   case AI_REP_SYNCTEST_EVENT:
					p_SyncTest =  &p_prodtest_response->prodtest_response_union.sync_test;
	   				STATUSMSG("Status:AI2 Report Sync Test Result");
					p_prodtest_response->ctrl_prodtest_response_union = GPSC_RESULT_SYNC_TEST;
					p_SyncTest->state = (U8)NBytesGet(&p_B, sizeof(U8));
					p_SyncTest->on_time = (U32)NBytesGet(&p_B, sizeof(U32));
					p_SyncTest->start_time = (U32)NBytesGet(&p_B, sizeof(U32));
					p_SyncTest->code_sync_time = (U32)NBytesGet(&p_B, sizeof(U32));
					p_SyncTest->bit_sync_time = (U32)NBytesGet(&p_B, sizeof(U32));
					p_SyncTest->frame_sync_time = (U32)NBytesGet(&p_B, sizeof(U32));

					p_GPSCState->u_ProductlinetestRequestPending = FALSE;
					gpsc_app_prodlinetest_ind(p_prodtest_response);
					gpsc_inject_gps_idle(p_zGPSCControl);
					break;

				case AI_CLAPP_REP_RCVR_SELF_TEST_RES:
					p_SelfTest =  &p_prodtest_response->prodtest_response_union.self_test;
	   				STATUSMSG("Status:AI2 Report Self Test Result");
					p_prodtest_response->ctrl_prodtest_response_union = GPSC_RESULT_SELF_TEST;
					p_SelfTest->self_test_result = (U8)NBytesGet(&p_B, sizeof(U8));

					p_GPSCState->u_ProductlinetestRequestPending = FALSE;
					gpsc_app_prodlinetest_ind(p_prodtest_response);
					gpsc_inject_gps_idle(p_zGPSCControl);
					break;

			   case AI_CLAPP_REP_CUSTOM_PACKET: //AI_CLAPP_CUSTOM_SUB_PKT_REP_GPIO_STATUS
				   {
				   U8 u_SubPacketId = 0;
				   u_SubPacketId = (U8)NBytesGet(&p_B, sizeof(U8));

				   switch(u_SubPacketId)
				   {
				   case AI_CLAPP_CUSTOM_SUB_PKT_REP_GPIO_STATUS:
					   p_GpioTest = &p_prodtest_response->prodtest_response_union.gpio_test;
					   p_prodtest_response->ctrl_prodtest_response_union = GPSC_RESULT_GPIO_TEST;
   					   p_GpioTest->status_value = (U16)NBytesGet(&p_B, sizeof(U16));
					   STATUSMSG("Status:AI2 Report GPS GPIO Test Result, status value:0x%X",p_GpioTest->status_value);
					   gpsc_app_prodlinetest_ind(p_prodtest_response);
             		   gpsc_inject_gps_idle(p_zGPSCControl);
					   break;

				   default:
				  AI2RX("Unhandled Custom Packet Response ID",u_SubPacketId);
				  break;
				   }
				   break;
				   }
			   default:
				  AI2RX("Unhandled Message",p_zAi2Field->u_Id);
				  break;
			   } /* end of switch( p_zAi2Field->u_Id )*/
		} /* end of case C_GPSC_STATE_PRODUCTLINE_TEST: */
		break;

		/* following seq need to be check if required or not */
		case E_GPSC_SEQ_READY_IDLE:
		case E_GPSC_SEQ_READY_SLEEP:
			if(p_zAi2Field->u_Id == AI_CLAPP_REP_RAM_CHKSUM_RES)
			{
			p_RamChecksumTest =  &p_prodtest_response->prodtest_response_union.ram_checksum_test;
	   				STATUSMSG("Status:AI2 Report RTC Offset Test Result");
					p_prodtest_response->ctrl_prodtest_response_union = GPSC_RESULT_RAM_CHECKSUM_TEST;
					p_RamChecksumTest->checksumresult = (U8)NBytesGet(&p_B, sizeof(S16));
       					p_GPSCState->u_ProductlinetestRequestPending = FALSE;
					gpsc_app_prodlinetest_ind(p_prodtest_response);
					gpsc_inject_gps_idle(p_zGPSCControl);
			} /* end of case C_GPSC_STATE_GPS_PM_IDLE */
			break;


		default:
		 ERRORMSG("Error : Report for product line test is  not valid in this state");
	} /* end of switch( p_zGPSCState->u_GPSCState ) */
}


void gpsc_mgp_rx_calibcontrol_report
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*    p_zAi2Field
)
{

   U8 *p_B = p_zAi2Field->p_B;

   U8 u_SubPktId;
   gpsc_state_type* p_GPSCState = p_zGPSCControl->p_zGPSCState;
   T_GPSC_calib_response *p_calib_response;
   gpsc_result_type*	p_zResultBuffer = p_zGPSCControl->p_zResultBuffer;

   STATUSMSG("gpsc_mgp_rx_calibcontrol_report");

   p_calib_response = (T_GPSC_calib_response*)&p_zResultBuffer->s_ResultBuffer[0];

   u_SubPktId = (U8)NBytesGet(&p_B, sizeof(U8));

   p_calib_response->calib_result = (U8)NBytesGet(&p_B, sizeof(U8));

   STATUSMSG("calib_result:%d",p_calib_response->calib_result);

   if((p_calib_response->calib_result == 0) && p_GPSCState->u_CalibDisable == TRUE)
   {
	   gpsc_calib_timepulse_refclk_sensor_req (C_APPLY_TIMESTAMP_REFCLK_DISABLE);
   }

#ifdef CLIENT_CUSTOM
	p_GPSCState->u_CalibType = GPSC_DISABLE_CALIBRATION; // only externally triggered Calib allowed.
#else
	p_GPSCState->u_CalibType = p_zGPSCControl->p_zSysHandlers->GpscConfigFile.ref_clock_calib_type;
#endif

   gpsc_app_calib_control_ind ( p_calib_response);

}

void gpsc_mgp_rx_invalid_msg
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*    p_zAi2Field
)
{

	gpsc_comm_tx_buff_type * p_Ai2TxBuf = p_zGPSCControl->p_zGPSCCommTxBuff;

	STATUSMSG("Error : Recieved Invalid message AI2, ID: %d and Error: %d",p_zAi2Field->p_B[0],p_zAi2Field->p_B[1]  );

	switch(p_zAi2Field->p_B[1])
	{
		case INVALID_MSG_DES:
			STATUSMSG("Error: Invalid message descriptor");
			break;
		case INVALID_MSG_LEN:
			STATUSMSG("Error: Invalid message length");
			break;
		case INVALID_MSG_CHECKSUM:
			STATUSMSG("Error: Invalid message checksum");
			break;
		case INVALID_MSG_PID:
			STATUSMSG("Error: Invalid packet ID");
			break;
		case INVALID_MSG_PDATA:
			STATUSMSG("Error: Invalid packet data");
			break;
		case INVALID_MSG_PLEN:
			STATUSMSG("Error: Invalid packet length");
			break;
		case INVALID_MSG_SST:
			STATUSMSG("Error: Invalid software state transition commanded");
			break;
		default:
			STATUSMSG("Error: Unknown");
			break;
	}

	if(p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_READY_BAUD_CHANGE)
	  {
	  	// if we are in patch download state, the gps device might already have the patch downloaded
		// in this case move reset the receiver so that we get the version response and we can move to Ready.
		// This has been introduced so the that the phone can be reset without resetting the External NL5500 board.
	  	if(p_zAi2Field->p_B[0] == 0xF5 && p_zAi2Field->p_B[1] == 4)
	  	{
			STATUSMSG("Error received for Protocol Select command - sending receiver reset");
			gpsc_mgp_tx_rc_act(p_zGPSCControl, C_RECEIVER_RESET, NULL);
			if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
			{
				FATAL_INIT(GPSC_TX_INITIATE_FAIL);
			}
	  	}

	  }
	  else   // if not in Patch Control state
	  {
	  	if (p_Ai2TxBuf->u_AI2AckPending ==TRUE)
	  	{	// release  pending ACK
	  		STATUSMSG(" Releasing pending ACK ");
		 	p_Ai2TxBuf->u_AI2AckPending =FALSE;
			gpsc_timer_stop(p_zGPSCControl,C_TIMER_AI2_ACK_PENDING);
			if(p_Ai2TxBuf->u_DataPending == TRUE)
			{
				p_Ai2TxBuf->u_DataPending = FALSE;
				gpsc_comm_transmit(p_zGPSCControl, p_Ai2TxBuf->u_DataPendingAck);
			}
			gpsc_state_process_ack(p_zGPSCControl);
	  	}
	  }
}


void gpsc_geofence_pro_report(  gpsc_ctrl_type*  p_zGPSCControl,TNAVC_MotionMask_Status* p_zmotionmaskstatus)
{
	if(NULL!=p_zmotionmaskstatus)
	{
		
	    gpsc_sess_cfg_type* p_zSessConfig = p_zGPSCControl->p_zSessConfig;
	    gpsc_sess_specific_cfg_type* p_zSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;
	    McpU8 proximity_alert=0;
		proximity_alert = p_zmotionmaskstatus->status & 0xF0000000;
		if(0!=proximity_alert)
		{
			if(0==gpsc_get_gf_report_config(p_zSessConfig,NULL))
			{
				if(NULL!=p_zSessSpecificCfg)
					p_zSessSpecificCfg->w_ReportPeriod/=3;
				p_zSessConfig->u_ReConfigFlag = TRUE;
				gpsc_sess_update_period(p_zGPSCControl, C_PERIOD_NOTSET);
				p_zSessConfig->u_ReConfigFlag = FALSE;
			}
		}
		else
		{
			p_zmotionmaskstatus->status &= 0x01FFFFFF;
			
		   // p_zSessSpecificCfg->GeofenceStatus = p_zmotionmaskstatus->status;
		    while(p_zSessSpecificCfg != NULL)
		    {	
				p_zmotionmaskstatus->geofence_session_id = p_zSessSpecificCfg->w_SessionTagId;
				gpsc_app_motion_mask_ind(p_zmotionmaskstatus);
				/*traversing to next node*/
				p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;

		    } /* while(p_zSessSpecificCfg != NULL) */
		}
	}

}

void gpsc_mgp_rx_motion_mask_setting  
(
        gpsc_ctrl_type*  p_zGPSCControl,
        Ai2Field*    p_zAi2Field
)
{

	U8 *p_B = p_zAi2Field->p_B;
	int i;

	U8 u_SubPktId;

	TNAVC_GetMotionMask* geofence_setting = (TNAVC_GetMotionMask *)p_zGPSCControl->p_zMotionMaskSetting;

	u_SubPktId = (U8)NBytesGet(&p_B, sizeof(U8));

	geofence_setting->report_configuration= (U8)NBytesGet(&p_B, sizeof(U8));

	geofence_setting->region_number= (U8)NBytesGet(&p_B, sizeof(U8));

	geofence_setting->region_type= (U8)NBytesGet(&p_B, sizeof(U8));
	STATUSMSG("geofence_setting->region type = %d",geofence_setting->region_type);
	geofence_setting->motion_mask_control= (U8)NBytesGet(&p_B, sizeof(U8));
	STATUSMSG("geofence_setting->motion mask control = %d",geofence_setting->motion_mask_control);
	geofence_setting->no_of_vertices= (U8)NBytesGet(&p_B, sizeof(U8));
	geofence_setting->speed_limit= (U16)NBytesGet(&p_B, sizeof(U16));
	geofence_setting->altitude_limit= (U16)NBytesGet (&p_B, sizeof(U16));
	geofence_setting->area_altitude = (U16)NBytesGet (&p_B, sizeof(U16));
	geofence_setting->radius_of_circle = (U16)NBytesGet (&p_B, sizeof(U16));
	for(i=0; i<geofence_setting->no_of_vertices; i++) {
		geofence_setting->latitude[i] = NBytesGet (&p_B, sizeof(U32));
	}

	for(i=0; i<geofence_setting->no_of_vertices; i++) {
		geofence_setting->longitude[i] = NBytesGet (&p_B, sizeof(U32));
	}
	for(i=0; i<geofence_setting->no_of_vertices; i++)
	{
		/*5. Validate geofence configuration - validate latitude and longitude in range */
		/*TBD- Not Validated now*/
		if (!mapToXY(&geofence_setting->latitude[i],
		            &geofence_setting->longitude[i],
		            geofence_setting->latitude[i],
		            geofence_setting->longitude[i]))
		{
			return;
		}

	}

	gpsc_app_motion_mask_settings_ind(geofence_setting);


}

/*
 ******************************************************************************
 * gpsc_mgp_rx_reg_read_response
 *
 * Function description:
 *   Processes the - Report RF Register, Memory Contents AI2 message
 *
 *
 * Parameters:
 *  p_zGPSCControl : gpsc control struct
 *  p_zAi2Field : RX AI2 struct
 *
 * Return value:
 *   Void
 ******************************************************************************
*/
void gpsc_mgp_rx_reg_read_response(gpsc_ctrl_type *p_zGPSCControl, Ai2Field *p_zAi2Field)
{
	gpsc_state_type	*p_zGPSCState 	= p_zGPSCControl->p_zGPSCState;
	U8  *p_B = p_zAi2Field->p_B;
	U8 u_Len = p_zAi2Field->w_Length;
	//U8 u_SubPktId = (U8)NBytesGet(&p_B, sizeof(U8)); /* Klocwork Changes */

	STATUSMSG("gpsc_mgp_rx_reg_read_response: len=%d",u_Len);

	switch(p_zGPSCState->q_RegReadAddress)
	{
		case C_RF_AGC_REG:
		{
			U16 q_regValue;
			STATUSMSG("gpsc_mgp_rx_reg_read_response: received values for AGC parameters");
			if (u_Len == 3) // sub packet Id + no. of bytes in register
			{
				q_regValue = NBytesGet (&p_B, sizeof(U16));
				gpsc_app_process_agc_res(p_zGPSCControl, q_regValue);
			}
			else
				STATUSMSG("Error reading AGC params");
		}
		break;

		default:
			STATUSMSG("gpsc_mgp_rx_reg_read_response: Not handled");
	}

}

#ifdef ENABLE_INAV_ASSIST
/*
 ******************************************************************************
 * gpsc_mgp_rx_sensor_pe_data
 *
 * Function description:
 *   Receive the Position Engine information to be passed to the sensor engine
 *   library
 *
 * Parameters:
 *  p_B : pointer to the message data
 *
 * Return value:
 *   Void
 ******************************************************************************
*/
void gpsc_mgp_rx_sensor_pe_data(U8 *p_B)
{
	/*ReportSensorPEData*/
	infoFromGPS s_StoreGPSInfo = {0};
	U32 u_IntStore, u_ValidFlags;
	U16 u_ShortStore;
	U8 u_ByteStore;
	S32 s_IntStore;
	S16 s_ShortStore;
	S8 s_ByteStore;

	s_StoreGPSInfo.fcount = (U32)NBytesGet(&p_B, sizeof(U32));

	/* Flags : Should be same as position report */
	u_ShortStore = (U16)NBytesGet(&p_B, sizeof(U16));
    s_StoreGPSInfo.Flag = u_ShortStore;

	/* Latitude */
	s_IntStore = (S32)NBytesGet(&p_B, sizeof(S32));
	s_StoreGPSInfo.Latitude = (s_IntStore * 180)/pow(2,32);

	/* Longitude */
	s_IntStore = (S32)NBytesGet(&p_B, sizeof(S32));
	s_StoreGPSInfo.Longtitude = (s_IntStore * 180)/pow(2,31);

	/* Altitude */
	s_ShortStore = (S16)NBytesGet(&p_B, sizeof(S16));
	s_StoreGPSInfo.Altitude = s_ShortStore;
	s_StoreGPSInfo.Altitude *= 0.5;

	/* Latitude Unc */
	u_ShortStore = (U16)NBytesGet(&p_B, sizeof(U16));
	s_StoreGPSInfo.PosUncX = u_ShortStore;
	s_StoreGPSInfo.PosUncX *= 0.1;

	/* Longitude Unc */
	u_ShortStore = (U16)NBytesGet(&p_B, sizeof(U16));
	s_StoreGPSInfo.PosUncY = u_ShortStore;
	s_StoreGPSInfo.PosUncY *= 0.1;

	/* Altitude Unc */
	u_ShortStore = (U16)NBytesGet(&p_B, sizeof(U16));
	s_StoreGPSInfo.PosUncZ = u_ShortStore;
	s_StoreGPSInfo.PosUncZ *= 0.5;

	/* East Velocity */
	s_ShortStore = (S16)NBytesGet(&p_B, sizeof(S16));
	s_StoreGPSInfo.VelocityEast = s_ShortStore;
	s_StoreGPSInfo.VelocityEast *= (1000.0/65536.0);

	/* North Velocity */
	s_ShortStore = (S16)NBytesGet(&p_B, sizeof(S16));
	s_StoreGPSInfo.VelocityNorth = s_ShortStore;
	s_StoreGPSInfo.VelocityNorth *= (1000.0/65536.0);

	/* Vertical Velocity */
	s_ShortStore = (S16)NBytesGet(&p_B, sizeof(S16));
	s_StoreGPSInfo.VelocityVertical = s_ShortStore;
	s_StoreGPSInfo.VelocityVertical *= (1000.0/65536.0);

	/* East Velocity Unc */
	u_ShortStore = (U16)NBytesGet(&p_B, sizeof(U16));
	s_StoreGPSInfo.VelUncEast = u_ShortStore;
	s_StoreGPSInfo.VelUncEast *= (1000.0/65536.0);

	/* North Velocity Unc */
	u_ShortStore = (U16)NBytesGet(&p_B, sizeof(U16));
	s_StoreGPSInfo.VelUncNorth = u_ShortStore;
	s_StoreGPSInfo.VelUncNorth *= (1000.0/65536.0);

	/* Vertical Velocity Unc */
	u_ShortStore = (U16)NBytesGet(&p_B, sizeof(U16));
	s_StoreGPSInfo.VelUncVertical = u_ShortStore;
	s_StoreGPSInfo.VelUncVertical *= (1000.0/65536.0);

	/* Number of SVs used in Position Fix */
	u_ByteStore = (U8)NBytesGet(&p_B, sizeof(U8));
	s_StoreGPSInfo.NumPosSv = u_ByteStore;

	/* Number of SVs used in Velocity Fix */
	u_ByteStore = (U8)NBytesGet(&p_B, sizeof(U8));
	s_StoreGPSInfo.NumVelSv = u_ByteStore;

	/* NumSvPosFixPreRaim */
	u_ByteStore = (U8)NBytesGet(&p_B, sizeof(U8));

	/* NumSvVelFixPreRaim */
	u_ByteStore = (U8)NBytesGet(&p_B, sizeof(U8));

	/* MP Level Indicator */
	u_ByteStore = (U8)NBytesGet(&p_B, sizeof(U8));
	s_StoreGPSInfo.MPLevelIndicator = u_ByteStore;
/*    s_StoreGPSInfo.MPLevelIndicator *= (10.0/254.0);*/

	/* Max Pos Error */
	u_ByteStore = (U8)NBytesGet(&p_B, sizeof(U8));
	s_StoreGPSInfo.MaxPosErr = u_ByteStore;
/*    s_StoreGPSInfo.MaxPosErr *= (50.0/254.0);*/

	/* Max Vel Error */
	u_ByteStore = (U8)NBytesGet(&p_B, sizeof(U8));
	s_StoreGPSInfo.MaxVelErr = u_ByteStore;
/*    s_StoreGPSInfo.MaxVelErr *= (50.0/254.0);*/

	inavc_inject_GPSInfo(pNavcCtrl->hInavc, &s_StoreGPSInfo);
}
#endif


