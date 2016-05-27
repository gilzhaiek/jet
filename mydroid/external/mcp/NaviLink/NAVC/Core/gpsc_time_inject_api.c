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
 * FileName			:	gpsc_time_api.c
 *
 * Description     	:
 * This file is provides an api for the Time Pulse portion of the GPSC
 * interface.
 *
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#include "gpsc_types.h"
#include "gpsc_sap.h"
#include "gpsc_data.h"
#include "gpsc_drv_api.h"
#include "gpsc_msg.h"
#include "gpsc_comm.h"
#include "gpsc_sequence.h"
#include "gpsc_state.h"
#include "gpsc_app_api.h"
#include "gpsc_timer_api.h"

void gpsc_populate_time_assist
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_time_assist*        p_time_assist
);

void gpsc_send_time_report
(
  gpsc_ctrl_type* p_zGPSCControl,
  T_GPSC_time_accuracy time_accuracy
);



void gpsc_time_pulse_event_received
(
 gpsc_ctrl_type *p_zGPSCControl,
 Ai2Field*    p_zAi2Field
)
{
  gpsc_state_type*		p_zGPSCState    = p_zGPSCControl->p_zGPSCState;
  gpsc_db_type *p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
  gpsc_db_hw_event_type  *p_zDBHWEvent = &p_zGPSCDatabase->z_DBHWEvent;
  U8 *p_B = p_zAi2Field->p_B;
  U16 w_RawSubMs;
  p_B +=2; /*Point to Fcount*/
  
  if(p_zGPSCState->u_AsyncPulseEventPending !=TRUE)
  {
	ERRORMSG("Error: Not expected Time Pulse Event at this time");
	return;
  }

  p_zGPSCState->u_AsyncPulseEventPending  = FALSE;
  p_zDBHWEvent->u_Valid = TRUE;
  p_zDBHWEvent->q_FCount = NBytesGet( &p_B, sizeof(U32) );
  w_RawSubMs = (U16)NBytesGet( &p_B, sizeof(U16) );
  p_zDBHWEvent->f_SubMs = (FLT)w_RawSubMs / 16368;

  if(p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_SESSION_REQUEST_TIME)
  	{
  	gpsc_session_sequence(p_zGPSCControl,E_GPSC_SEQ_SESSION_REQUEST_TIME);
  	}
#if 0

  if(p_zGPSCState->u_FineTimeRespPending  == FALSE)
  {
	if(p_zGPSCState->u_GPSCState == C_GPSC_STATE_REQUEST_TIME)
	{
	  /*Time from GPSM already received, send time to GPS now*/
		STATECHANGE_REPORT_FERROR(C_GPSC_STATE_INJECT_TIME);
	}
	else if(p_zGPSCState->u_GPSCState == C_GPSC_STATE_REQUEST_PULSE)
	{
	  gpsc_send_time_report(p_zGPSCControl,GPSC_TIME_ACCURACY_FINE);
	  STATECHANGE_REPORT_FERROR(C_GPSC_STATE_GPS_PM_SLEEP);
	}
  }
#endif
}

extern T_GPSC_result gpsc_time_request_pulse_res (T_GPSC_result result)
{
  gpsc_ctrl_type *p_zGPSCControl;
  gpsc_state_type*		p_zGPSCState; 
  gpsc_cfg_type*		p_zGPSCConfig;
  T_GPSC_time_accuracy time_accuracy;
  SAPENTER(gpsc_time_request_pulse_res);
	
  p_zGPSCControl = gp_zGPSCControl;
  p_zGPSCState    = p_zGPSCControl->p_zGPSCState;
  p_zGPSCConfig	= p_zGPSCControl->p_zGPSCConfig;

#if 0
  if(p_zGPSCState->u_GPSCState != C_GPSC_STATE_REQUEST_PULSE)
  {
	ERRORMSG2("Error: Pulse Response not expected in state %s",
				s_StateNames[p_zGPSCState->u_GPSCState]);
	SAPLEAVE(gpsc_time_request_pulse_res,GPSC_INTERNAL_FAIL);
  }  
#endif
  if(p_zGPSCState->u_FineTimeRespPending !=TRUE)
  {
	ERRORMSG("Error: Not expected Time Response at this time");
	SAPLEAVE(gpsc_time_request_pulse_res,GPSC_INTERNAL_FAIL);
  }
  
  p_zGPSCState->u_FineTimeRespPending = FALSE;
  if(result == GPSC_SUCCESS)
  {
	if(p_zGPSCState->u_AsyncPulseEventPending== TRUE)
	{
		STATUSMSG("Status: Pulse generated successfully, waiting for async event");
		gpsc_timer_start(p_zGPSCControl,C_TIMER_SEQUENCE, p_zGPSCConfig->ai2_comm_timeout );
		return GPSC_SUCCESS;
	}
	else
	{
		STATUSMSG("Status: Pulse generated successfully, sending fine time");
		time_accuracy = GPSC_TIME_ACCURACY_FINE_LAST_PULSE ;
	}
  }
  else if(result == GPSC_TIME_PULSE_GENERATION_FAIL)
  {
	STATUSMSG("Status: Pulse NOT genrated successfully, reporting coarse time");
	p_zGPSCState->u_AsyncPulseEventPending= FALSE;
	time_accuracy = GPSC_TIME_ACCURACY_COARSE;
  }
  else
  {
	STATUSMSG("Status: Unknown result value, reporting fine time");
	p_zGPSCState->u_AsyncPulseEventPending= FALSE;
	time_accuracy = GPSC_TIME_ACCURACY_COARSE;
  }
  gpsc_send_time_report(p_zGPSCControl,time_accuracy);
  //STATECHANGE_REPORT_FERROR(C_GPSC_STATE_GPS_PM_SLEEP);
  return GPSC_SUCCESS;
}


void gpsc_populate_time_assist
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_time_assist* p_time_assist
)
{
	gpsc_smlc_assist_type* p_zSmlcAssist = p_zGPSCControl->p_zSmlcAssist;
	gpsc_assist_wishlist_type*  p_zGpsAssisWishlist = p_zGPSCControl->p_zGpsAssisWishlist;
	gpsc_smlc_gps_time_type*    p_zSmlcGpsTime = &p_zSmlcAssist->z_SmlcGpsTime;
    gpsc_inject_time_est_type*  p_zInjectTimeEst = &p_zSmlcGpsTime->z_InjectGpsTimeEst;

	p_zInjectTimeEst->z_meTime.w_GpsWeek = p_time_assist->gps_week;
	p_zInjectTimeEst->z_meTime.q_GpsMsec = p_time_assist->gps_msec;
	p_zInjectTimeEst->z_meTime.f_ClkTimeUncMs = (FLT)(p_time_assist->time_unc / (1000.0));
	p_zInjectTimeEst->z_meTime.f_ClkTimeBias =(FLT) p_time_assist->sub_ms;
    if(p_time_assist->time_accuracy == GPSC_TIME_ACCURACY_FINE_LAST_PULSE)
		p_zInjectTimeEst->u_Mode = 2; /* Last Pulse */
	else
		p_zInjectTimeEst->u_Mode = 1; /* TimeNow */
    p_zInjectTimeEst->q_Fcount = 0; /* irrelevant in both modes */
    p_zInjectTimeEst->u_ForceFlag = 0; /* don't inject if Sensor has better
                                                time */
	p_zGpsAssisWishlist->w_Wishlist &= ~GPSC_REQ_TIME;
	STATUSMSG("Status: Received Time Assistance : Week %d, Msec: %d, Mode : %s",
		p_zInjectTimeEst->z_meTime.w_GpsWeek, p_zInjectTimeEst->z_meTime.q_GpsMsec,
		(p_zInjectTimeEst->u_Mode == 1 ? "Coarse":"Fine"));
}

void gpsc_send_time_report
(
  gpsc_ctrl_type* p_zGPSCControl,
  T_GPSC_time_accuracy time_accuracy
)
{
	gpsc_db_type*			p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_db_gps_clk_type*  p_DBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;
    gpsc_db_hw_event_type  *p_zDBHWEvent = &p_zGPSCDatabase->z_DBHWEvent;
	FLT f_EventTimeMs,f_GPSTime,f_FCOUNTDiff;

	T_GPSC_time_assist  z_time_report,*p_time_report = &z_time_report;
	if(time_accuracy == GPSC_TIME_ACCURACY_FINE_LAST_PULSE)
	{
     f_GPSTime = p_DBGpsClock->q_GpsMs + p_DBGpsClock->f_TimeBias;
     f_FCOUNTDiff = (FLT)p_zDBHWEvent->q_FCount - p_DBGpsClock->q_FCount;
	 f_EventTimeMs = f_GPSTime + f_FCOUNTDiff + p_zDBHWEvent->f_SubMs;
     z_time_report.gps_week = p_DBGpsClock->w_GpsWeek;
	 z_time_report.gps_msec  = (U32)(f_EventTimeMs);
	 z_time_report.sub_ms = (U32)((f_EventTimeMs - z_time_report.gps_msec)*1000);
	 z_time_report.time_unc = (U32)(p_DBGpsClock->f_TimeUnc * 1000) ; //TBD

	}
	else
	{
		z_time_report.gps_week = p_DBGpsClock->w_GpsWeek;
		z_time_report.gps_msec = p_DBGpsClock->q_GpsMs;
		z_time_report.time_unc = (U32)(p_DBGpsClock->f_TimeUnc * 1000);  //TBD
		if(p_DBGpsClock->f_TimeBias < 0)
		{
			z_time_report.gps_msec--;
			p_DBGpsClock->f_TimeBias += 1.0;
		}
		z_time_report.sub_ms = (U32)(p_DBGpsClock->f_TimeBias*1000);
	}
	STATUSMSG("Status: Reporting Time: Week %d, Msec %d, Unc %fms",
		z_time_report.gps_week,z_time_report.gps_msec, z_time_report.time_unc/1000.0f);
	gpsc_time_report_tow_ind (time_accuracy, p_time_report);
}