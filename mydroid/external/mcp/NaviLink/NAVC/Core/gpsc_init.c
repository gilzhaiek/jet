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
 * FileName			:	gpsc_init.c
 *
 * Description     	:
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#define _GPSC_DATA_DEFINE
#include "gpsc_data.h"
#undef _GPSC_DATA_DEFINE

#include "gpsc_sap.h"
#include "gpsc_init.h"
#include "gpsc_mgp_tx.h"
#include "gpsc_mgp_cfg.h"
#include "gpsc_mgp_tx.h"
#include "gpsc_msg.h"
#include "gpsc_mgp.h"
#include "gpsc_comm.h"
#include "gpsc_sess.h"
#include "gpsc_state.h"
#include "mcpf_mem.h"
#include "mcpf_services.h"
#include "mcp_hal_fs.h"
#include "navc_priority_handler.h"
#include <utils/Log.h>
void gpsc_init_event_config(gpsc_ctrl_type*	p_zGPSCControl);

/*
 ******************************************************************************
 * gpsc_init_database
 *
 * Function description:
 *
 * Initialize LM database by marking everything as invalid.
 *
 * Parameters:
 *  p_zGPSCDatabase - Pointer to the LM database
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/

void gpsc_init_database(gpsc_db_type *p_zGPSCDatabase, U8 u_Clean)
{

  U8 u_i;

  p_zGPSCDatabase->z_DBGpsClock.u_Valid = FALSE;
  p_zGPSCDatabase->z_DBGpsClock.f_TimeUnc = 999999.0f;

  p_zGPSCDatabase->u_EphCounter=0;
  p_zGPSCDatabase->u_AlmCounter=0;

  /*if u_Clean is set, clean ephemeris, almanac , pos and iono info too*/
  if(u_Clean == TRUE)
  {
	  p_zGPSCDatabase->z_NvsPos.d_ToaSec = 0;
	  for ( u_i = 0; u_i < N_SV; u_i++)
	  {
		p_zGPSCDatabase->z_DBEphemeris[ u_i ].u_Valid = FALSE;
		p_zGPSCDatabase->z_DBEphemeris[ u_i ].z_RawEphemeris.u_predicted = FALSE;
		p_zGPSCDatabase->z_DBEphemeris[ u_i ].z_RawEphemeris.u_predSeedAge = 0;
		p_zGPSCDatabase->z_DBEphemeris[ u_i ].z_RawEphemeris.toe_expired = TRUE;
		p_zGPSCDatabase->z_DBEphStat[ u_i ].u_SvId = 0;

		p_zGPSCDatabase->z_DBAlmanac[ u_i ].u_Valid = FALSE;
		p_zGPSCDatabase->z_DBAlmStat[ u_i ].u_SvId = 0;
		p_zGPSCDatabase->z_DBHealth.u_AlmHealth[u_i] = C_HEALTH_UNKNOWN;
	  }

	  p_zGPSCDatabase->z_DBIono.d_UpdateTimeSec = 0;
	  p_zGPSCDatabase->z_DBIono.v_iono_valid = 0;
	  p_zGPSCDatabase->z_DBHealth.d_UpdateTimeSec = 0;
	  p_zGPSCDatabase->z_DBPos.d_ToaSec = 0;
	  p_zGPSCDatabase->z_DBGpsInjClock.l_FreqBiasRaw=0;
	  p_zGPSCDatabase->z_DBGpsInjClock.q_FreqUncRaw=0;
	  p_zGPSCDatabase->z_DBUtc.u_valid = 0;
  }

  p_zGPSCDatabase->z_DBSvDirection.u_num_of_sv = 0;

  /* the measurement database, is only used to construct
     information for sending to SMLC. When its both z_MeasReport
     and z_MeasStatReport contain a same FCOUNT value, we know
     this structure contains a set of measurement data and
     measurement status of a same measurement. This lifetime
     of the "database" is only extended to when a measurement
     has been examined, at which point to invalid it. */
  p_zGPSCDatabase->z_DBGpsMeas.z_MeasReport.u_Valid = FALSE;
  p_zGPSCDatabase->z_DBGpsMeas.z_MeasStatReport.u_Valid = FALSE;
  p_zGPSCDatabase->z_DBGpsMeas.u_init_last = TRUE;
  for ( u_i = 0; u_i < N_LCHAN; u_i++)
  {
    p_zGPSCDatabase->
      z_DBGpsMeas.z_MeasReport.z_MeasPerSv[u_i].u_SvIdSvTimeFlag = 0;
    p_zGPSCDatabase->z_DBGpsMeas.z_MeasStatReport.z_MeasStatPerSv[u_i].u_Sv = 0;
    p_zGPSCDatabase->z_DBGpsMeas.z_LastMeasObsvd[u_i].u_Sv = 0;
  }


  p_zGPSCDatabase->z_DBHWEvent.u_Valid = FALSE;
  p_zGPSCDatabase->z_DBGpsInjClock.u_Valid = FALSE;
  p_zGPSCDatabase->z_DBGpsClock.w_GpsWeek=C_GPS_WEEK_UNKNOWN;

}


/*
 ******************************************************************************
 * gpsc_init_steering
 *
 * Function description:
 *
 * Initialize LM database by marking everything as invalid.
 *
 * Parameters:
 *  p_zGPSCDatabase - Pointer to the LM database
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/
void gpsc_init_steering(gpsc_db_type *p_zGPSCDatabase)
{

  gpsc_db_sv_steer_type *p_zDBSvSteer = &p_zGPSCDatabase->z_DBSvSteer;
  gpsc_sv_id_steer_type *p_zSvIdSteer;
  U8 u_i;

  p_zDBSvSteer->u_Valid = FALSE;

  for (u_i=0; u_i<N_LCHAN; u_i++)
  {
    p_zSvIdSteer = &p_zDBSvSteer->z_SvIdSteer[u_i];
    p_zSvIdSteer->u_SvId = 0;
  }
}


/*
 ******************************************************************************
 * gpsc_init_state
 *
 * Function description:
 *
 * This function initializes variables in the gpsc_state_type structure
 *
 * Parameters:
 *  p_zGPSCControl: pointer to the array of pointers that contains addresses of
 *  the variables that are accessed throughout the program.
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/

void gpsc_init_state(gpsc_ctrl_type* p_zGPSCControl)
{

  gpsc_state_type*             p_zGPSCState      =  p_zGPSCControl->p_zGPSCState;
  gpsc_session_status_type*    p_zSessionStatus  =  &p_zGPSCState->z_SessionStatus;
  gpsc_patch_status_type*	    p_zPatchStatus    =  &p_zGPSCState->z_PatchStatus;


  /* initialize GPSC state flags */
  p_zGPSCState->u_LocationRequestPending = FALSE;
  p_zGPSCState->u_PowerSaveRequestPending = FALSE;
  p_zGPSCState->u_ShutDownRequestPending = FALSE;
  p_zGPSCState->u_GPSCState = E_GPSC_STATE_SHUTDOWN;
  p_zGPSCState->u_SensorOpMode = C_RC_IDLE;

  p_zGPSCState->u_ReadyRequestPending = FALSE;
  p_zGPSCState->u_FixStopRequestPending = FALSE;

  p_zGPSCState->u_ClockQualInjectPending  = FALSE;
  p_zGPSCState->u_AsyncPulseEventPending  = FALSE;
  p_zGPSCState->u_FineTimeRespPending  = FALSE;

  p_zGPSCState->u_GpsVersionPending = FALSE;
  p_zGPSCState->u_UpdateNvsRequest = FALSE;

  p_zGPSCState->u_ProductlinetestRequestPending = FALSE;

  p_zGPSCState->u_TimeInjected =FALSE; /* added for RTI feature */

  p_zGPSCState->u_ValidPatch = FALSE; /* to avoid patch download if there is valid patch */

  p_zGPSCState->u_BaudRateConfigured = 0;
  p_zGPSCState->u_DummySession = 0; /* added for Hybrid feature */

  p_zGPSCState->u_WaitForClkReport = FALSE;
  p_zGPSCState->u_StopSessionReqPending = FALSE;
  p_zGPSCState->u_ClkRptforWlPending= FALSE;
  p_zGPSCState->u_CalibRequested = FALSE;

  p_zGPSCState->u_GpsSessStarted=FALSE; /* Added to Avoid Multiple parallel session */
  p_zGPSCState->z_nvm_inj_state = 0;

  p_zGPSCState->u_firstfix = FALSE;
  p_zGPSCState->u_GeofenceAPMEnabled = FALSE;

  /* The reference clock calibration type can be configured through configFile */
#ifdef CLIENT_CUSTOM		
	p_zGPSCState->u_CalibType = GPSC_DISABLE_CALIBRATION; // only externally triggered Calib allowed.
#else
  p_zGPSCState->u_CalibType = p_zGPSCControl->p_zSysHandlers->GpscConfigFile.ref_clock_calib_type;
#endif


  p_zGPSCState->u_CountInvalidFix = UNDEFINED_U8; /* to count the no of invalid fix or meas report to
                                                     decide the re acquisition of the assistance */
  p_zSessionStatus->u_WishlistBuilt = 0;
  p_zSessionStatus->u_BuildWishlistLater=0;
  p_zSessionStatus->w_NumUnRptFixEvt = 0;
  p_zSessionStatus->w_NumUnRptMsrEvt = 0;

#ifndef NO_TRACKING_SESSIONS
  p_zSessionStatus->u_TrackSubState = TRKSTATE_INITIAL;
#endif

#ifndef UNLIMITED_EPHEMERIS_STORAGE
  p_zSessionStatus->u_AcqAsstListedSVs = 0;
#endif

  p_zPatchStatus->u_PatchFileOpen=FALSE;
  p_zPatchStatus->q_TotalBytes=0;
  p_zPatchStatus->w_TotalRecords=0;
  p_zPatchStatus->w_NextRecord = 0;
  p_zPatchStatus->q_BytesSent= 0;
  p_zPatchStatus->pPatchFile = 0;
}


/*
 ******************************************************************************
 * Function description:
 *  Initialize the structure holding assistance data from SMLC
 *
 * Parameters:
 *  p_zSmlcAssist: pointer to the structure holding assistance data from SMLC
 *
 *
 * Return value:
 *    None
 *
 ******************************************************************************
*/

void gpsc_init_smlc(gpsc_ctrl_type *p_zGPSCControl)
{

  U8 u_i;
  U16 w_Temp;

  gpsc_smlc_assist_type*  p_zSmlcAssist  = p_zGPSCControl->p_zSmlcAssist;

  pe_RawEphemeris*            p_zSmlcRawEph;
  pe_RawAlmanac*              p_zSmlcRawAlm;

  gpsc_smlc_gps_time_type*     p_zSmlcGpsTime = &p_zSmlcAssist->z_SmlcGpsTime;
  gpsc_inject_time_est_type*   p_zSmlcInjectTimeEst =
                                &p_zSmlcGpsTime->z_InjectGpsTimeEst;

  gpsc_inject_pos_est_type*    p_zSmlcInjectPosEst =
                                &p_zSmlcAssist->z_InjectPosEst;
  gpsc_raw_tow_type*           p_zSmlcRawTow;
  gpsc_acq_assist_type*        p_zSmlcAcquisAssist =
                                &p_zSmlcAssist->z_AcquisAssist;
  gpsc_acq_element_type*       p_zSmlcAcquisElement;
  gpsc_additional_angle_type*  p_zSmlcAdditionalAngle;

  gpsc_loc_fix_qop* p_zQop = &p_zSmlcAssist->z_Qop;

  p_zSmlcAssist->w_InjectedFlags=0;
  p_zSmlcAssist->w_AvailabilityFlags = 0;
  p_zSmlcAssist->u_AssistPendingFlag = FALSE;
  p_zSmlcAssist->u_nSvEph = 0;
  p_zSmlcAssist->u_InjVisible = FALSE;
  p_zSmlcAssist->q_VisiableBitmap = 0;
  p_zSmlcAssist->q_InjSvEph = 0;

  p_zSmlcInjectTimeEst->z_meTime.w_GpsWeek = C_GPS_WEEK_UNKNOWN;


  w_Temp = 0xFFFF;
  p_zSmlcInjectPosEst->x_Ht = (S16)w_Temp;

  p_zSmlcAssist->q_RealTimeIntegrity[0]=0; /* default to all healthy */
  p_zSmlcAssist->q_RealTimeIntegrity[1]=0; /* default to all healthy */


  for (u_i=0; u_i<N_SV; u_i++)
  {

    p_zSmlcRawEph = &p_zSmlcAssist->z_SmlcRawEph[u_i];
    p_zSmlcRawAlm = &p_zSmlcAssist->z_SmlcRawAlmanac[u_i];
    p_zSmlcRawTow = &p_zSmlcAssist->z_SmlcTow[u_i];

    p_zSmlcRawEph->u_Prn = 0;
    p_zSmlcRawAlm->u_Prn = 0;
    p_zSmlcRawTow->u_SvId = 0;

    p_zSmlcRawEph->u_predicted = 0;
    p_zSmlcRawEph->u_predSeedAge = 0;
    p_zSmlcRawEph->toe_expired = TRUE;
    p_zSmlcRawEph->u_available	= FALSE;

  }

  p_zSmlcAcquisAssist->p_zAAGpsClock.u_Valid = FALSE;
  p_zSmlcAcquisAssist->u_NumSv = 0;
  for (u_i=0; u_i<C_MAX_SVS_ACQ_ASSIST; u_i++)
  {
    p_zSmlcAcquisElement = &p_zSmlcAcquisAssist->z_AcquisElement[u_i];
    p_zSmlcAdditionalAngle = &p_zSmlcAcquisAssist->z_AddAngle[u_i];

    p_zSmlcAcquisElement->u_SvId = 0;
    p_zSmlcAcquisElement->u_Doppler1 = 0xFF;
    p_zSmlcAcquisElement->u_DopplerUnc = 0xFF;
    p_zSmlcAdditionalAngle->u_Azimuth = 0xFF;
    p_zSmlcAdditionalAngle->u_Elevation = 0xFF;
  }

  /* initialize timeout and CEP info */

  p_zQop->u_HorizontalAccuracy = C_ACCURACY_UNKNOWN;
  p_zQop->w_MaxResponseTime =  C_TIMEOUT_UNKNOWN;
  p_zQop->u_TimeoutCepInjected = FALSE;

}

/*
 ******************************************************************************
 * gpsc_init_config
 *
 * Function description:
 *
 * If gpsc_cfg_type is not found in non-volatile memory, this function creates
 * such a file with default values.
 *
 * Parameters:
 *
 *  p_zGPSCConfig: pointer to configuration record
 *
 * u_UpdateNvs: TRUE - update NVS; FALSE: don't update NVS
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/

void gpsc_init_sess_config(gpsc_sess_cfg_type*	p_zSessConfig)
{
#ifdef LARGE_UPDATE_RATE
	gpsc_ctrl_type*	p_zGPSCControl;
	gpsc_cfg_type* p_zGPSCConfig;

	p_zGPSCControl = gp_zGPSCControl;
	p_zGPSCConfig	= p_zGPSCControl->p_zGPSCConfig;
#endif

	/*  It will set session common position measurement entity u_PosCompEntity as
	     C_MS_COMPUTED if any of the session in multiple session is C_MS_COMPUTED else if
	     all sessions are C_SMLC_COMPUTED, then it will set to C_SMLC_COMPUTED.
	      By default it is set to C_MS_COMPUTED
	   */
	p_zSessConfig->u_PosCompEntity = C_MS_COMPUTED;
	p_zSessConfig->u_SmlcAssisPermit = C_SMLC_ASSIS_PERMITTED;

	p_zSessConfig->w_TotalNumSession = 0;

	p_zSessConfig->w_AvgReportPeriod = 1;
//	p_zSessConfig->w_ValidFixTimeout = 0;
	p_zSessConfig->q_PreSessionActionFlags = 0;
	p_zSessConfig->x_ActionTimeOffset = 0;
	p_zSessConfig->w_ActionTimeUnc = (U16) -1;
	p_zSessConfig->x_ActionPosEOffset = 0;
	p_zSessConfig->x_ActionPosNOffset = 0;
	p_zSessConfig->w_ActionPosUnc = (U16) -1;
	p_zSessConfig->u_BLF_State = 0;

    p_zSessConfig->u_SessionMode = 1;  /* if both number of fixes per session and
                                        intersession interval are specified,
                                        sessions are terminated on number of
                                        sessions, and next session starts at no
                                        sooner than intersession interval. */

    p_zSessConfig->u_SensorDiagSettings = 0;

#ifdef LARGE_UPDATE_RATE
	/* Multi session variables */
	p_zSessConfig->q_SleepDuration = 0;				/* No Info on sleep duration */

	p_zSessConfig->w_SleepDelta = (C_SLEEP_DELTA);
													/* Convert to seconds */
	p_zSessConfig->q_NoFixDelta = (C_NO_FIX_CORRECTION);
#endif

	/* initialize session specific data structure */
	p_zSessConfig->p_zSessSpecificCfg = NULL;

	/* in miliseconds by default it is 20 % more than default value of w_AvgReportPeriod */
	p_zSessConfig->q_PeriodicExpTimerVal = C_GPSC_TIMEOUT_PERIODIC_REPORT;


  /* for NULL report */
  p_zSessConfig->u_NullRepFlag = FALSE;
  p_zSessConfig->u_ReConfigFlag = FALSE;
  p_zSessConfig->q_FCount = GPSC_INVALID_FCOUNT;
  p_zSessConfig->q_LastFCount = GPSC_INVALID_FCOUNT;

  p_zSessConfig->q_FCountMeas = GPSC_INVALID_FCOUNT;
  p_zSessConfig->q_LastFCountMeas = GPSC_INVALID_FCOUNT;

  p_zSessConfig->u_SvNewAlmCount = 0;
}


/*
 ******************************************************************************
 * gpsc_mmgps_init
 *
 * Function description:
 *
 * If gpsc_cfg_type is not found in non-volatile memory, this function creates
 * such a file with default values.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the array of pointers that contains addresses of
 *  the variables that are accessed throughout the program.
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/
void gpsc_mmgps_init(gpsc_ctrl_type *p_zGPSCControl)
{
  gpsc_db_type*           p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;


  /* Initialize GPSC database, at this point we must assume we know nothing
     about Sensor data */
  gpsc_init_database(p_zGPSCDatabase, FALSE);

  /* Initialize steering database */
  /* mark all steering invalid */

  gpsc_init_steering(p_zGPSCDatabase);

}



/*
 ******************************************************************************
 * gpsc_cfg_sanity
 *
 * Function description:
 *
 * Checks the sanity of the Lm Configuration settings, if insanity found,
 * replace the parameters with some sensible values.
 *
 * Parameters:
 *
 *  p_zGPSCConfig: pointer to the LmConfig structure.
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/

U8 gpsc_cfg_sanity(gpsc_cfg_type* p_zGPSCConfig)
{

  if (p_zGPSCConfig->pdop_mask == 0)
  {
    p_zGPSCConfig->pdop_mask = 12;
    STATUSMSG("Status: Force config to  PDOP to 12" );
  }
  return TRUE;

}

U8 gpsc_cfg_checksum(gpsc_cfg_type* p_zGPSCConfig)
{

	if(CalculateCheckSum((U8*)p_zGPSCConfig, sizeof(gpsc_cfg_type))==0)
		return TRUE;
	else
		return FALSE;
}
/*===========================================================================
FUNCTION   gpsc_init

DESCRIPTION
  Initialising GPSC operation parameters

RETURNS
  None
******************************************************************************
*/




U8 gpsc_memory_init(handle_t hMcpf)
{

  gpsc_global_memory_type *p_GPSCMemory;
  gpsc_ctrl_type*    p_zGPSCControl;  /*=&z_GPSCControl;*/
  U16 w_MemorySize;
  U8 i=0;
/********************** Request for memory for allocation control structures***/

  w_MemorySize = (U16)sizeof(gpsc_global_memory_type);
  p_GPSCMemory  = (gpsc_global_memory_type*) mcpf_mem_alloc(hMcpf, w_MemorySize);


  if(p_GPSCMemory == NULL)
  {
	gp_zGPSCControl = NULL;
	return FALSE;
  }
  mcpf_mem_set(hMcpf, (void*)  p_GPSCMemory,    0, (U32)sizeof(gpsc_global_memory_type));

  /*Initialise the global pointer*/
  gp_zGPSCControl = &(p_GPSCMemory->z_GPSCControl);
  p_zGPSCControl = gp_zGPSCControl;


  p_GPSCMemory->zGPSCVersion.u_Day  = NULL;
  p_GPSCMemory->zGPSCVersion.u_Month  = NULL;
  p_GPSCMemory->zGPSCVersion.w_Year = NULL;
  p_GPSCMemory->zGPSCVersion.u_SubMinor1VerNum  = NULL;
  p_GPSCMemory->zGPSCVersion.u_SubMinor2VerNum  = NULL;
  p_GPSCMemory->zGPSCVersion.w_MajorVerNum  = NULL;
  p_GPSCMemory->zGPSCVersion.w_MinorVerNum  = NULL;

/********************** Initialize the main LM control structure ***************/
  p_zGPSCControl->p_zGPSCState          = &(p_GPSCMemory->z_GPSCState);
  p_zGPSCControl->p_zGPSCDatabase      =  &(p_GPSCMemory->z_GPSCDatabase);
  p_zGPSCControl->p_zGPSCConfig          = &(p_GPSCMemory->z_GPSCConfig);
  p_zGPSCControl->p_zSessConfig          = &(p_GPSCMemory->z_SessConfig);
  p_zGPSCControl->p_zEventCfg          = &(p_GPSCMemory->z_EventCfg);
  p_zGPSCControl->p_zGpsAssisWishlist  = &(p_GPSCMemory->z_GpsAssisWishlist);
  p_zGPSCControl->p_zSmlcAssist       = &(p_GPSCMemory->z_SmlcAssist);
  p_zGPSCControl->p_zRcvrConfig       = &(p_GPSCMemory->z_RcvrConfig);
  p_zGPSCControl->p_zGPSCParms          = &(p_GPSCMemory->z_GPSCParms);
  p_zGPSCControl->p_zGPSCCommRxBuff	   = &(p_GPSCMemory->z_GPSCCommRxBuff);
  p_zGPSCControl->p_zGPSCCommTxBuff	   = &(p_GPSCMemory->z_GPSCCommTxBuff);
  p_zGPSCControl->p_GPSCMemory		   = (void*)  p_GPSCMemory;
  p_zGPSCControl->p_zResultBuffer		= &(p_GPSCMemory->z_ResultBuffer);
  p_zGPSCControl->p_zProductlineTest   = &(p_GPSCMemory->z_ProductlineTest);
  p_zGPSCControl->p_zGPSCDynaFeatureConfig = &(p_GPSCMemory->z_GPSCDynaFeatureConfig);
  p_zGPSCControl->p_zGPSVersion = &(p_GPSCMemory->z_GPSVersion);
  p_zGPSCControl->p_zSvInject	= &(p_GPSCMemory->z_SvInject);
  p_zGPSCControl->p_zSysHandlers = &(p_GPSCMemory->z_SysHandlers);
  p_zGPSCControl->p_zCustomStruct = &(p_GPSCMemory->z_CustomStruct);
  p_zGPSCControl->p_zMotionMaskStatus = &(p_GPSCMemory->z_motion_mask_status);
  p_zGPSCControl->p_zMotionMaskSetting = &(p_GPSCMemory->z_motion_mask_setting);
  p_zGPSCControl->p_zSetMotionMask = &(p_GPSCMemory->z_set_motion_mask);
  p_zGPSCControl->p_zGeoFenceControl = &(p_GPSCMemory->z_GeoFenceControl);  
  p_zGPSCControl->hw_type=GPSC_HW_NONE;

  p_zGPSCControl->zRegAssist.EPHcount = 0;

  for (i=0; i<EPH_LIST_CNT;i++)
  {
  	p_zGPSCControl->zRegAssist.zEphList[i].taskID = 0;
	p_zGPSCControl->zRegAssist.zEphList[i].queueID = 0;
  }

  p_zGPSCControl->zRegAssist.GpsTimeCount = 0;

  for (i=0; i<GPSTIME_LIST_CNT;i++)
  {
  	p_zGPSCControl->zRegAssist.zGpsTimeList[i].taskID = 0;
	p_zGPSCControl->zRegAssist.zGpsTimeList[i].queueID = 0;
  }
  	
  return TRUE;
}

gpsc_db_gps_clk_type znvs_GpsTime;

T_GPSC_result gpsc_load_from_nvs(gpsc_ctrl_type* p_zGPSCControl, gpsc_cfg_type* pGPSCConfig)
{
	gpsc_db_type*         p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_sys_handlers*	p_GPSCSysHandlers = p_zGPSCControl->p_zSysHandlers;
	gpsc_custom_struct* p_zCustom = p_zGPSCControl->p_zCustomStruct;
	McpS32	fd;
	EMcpfRes	eRetVal;
	McpS8 u_i;
	
	//klocwork
	McpU8 uAidingPath[C_MAX_AIDING_FILENAME_LEN+20];

	*p_zGPSCControl->p_zGPSCConfig = *pGPSCConfig;
	gpsc_cfg_sanity(p_zGPSCControl->p_zGPSCConfig);

	   //for nvs time injection
/****** To extract GPSC Clock File  ******/
    MCP_HAL_STRING_StrCpy (uAidingPath,(McpU8*)p_GPSCSysHandlers->uAidingPath);
    MCP_HAL_STRING_StrCat (uAidingPath,C_STR_AID_CLOCK_FILE);

    eRetVal = mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *)uAidingPath,
                        MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_RDONLY, &fd);
    if(eRetVal == RES_OK)
    {
        mcpf_file_read(p_zGPSCControl->p_zSysHandlers->hMcpf, fd,
            ( void* ) &znvs_GpsTime, sizeof( gpsc_db_gps_clk_type  )  );
        mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
    }
    else
    {
        STATUSMSG("Error: Cannot open aiding file-%s",uAidingPath);
		p_zGPSCDatabase->z_DBPos.d_ToaSec = 0;
    }
//end of extract GPSC Clock File

/****** GPSCPositionFile  ******/
	MCP_HAL_STRING_StrCpy (uAidingPath,(McpU8*)p_GPSCSysHandlers->uAidingPath);
	MCP_HAL_STRING_StrCat (uAidingPath,C_STR_AID_POSITION_FILE);

	eRetVal = mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *)uAidingPath,
						MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_RDONLY, &fd);
	if (eRetVal == RES_OK)
	{
		mcpf_file_read(p_zGPSCControl->p_zSysHandlers->hMcpf, fd,
			( void* ) &p_zGPSCDatabase->z_NvsPos, sizeof( gpsc_nv_pos_type )  );
		mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
	}
	else
	{
		p_zGPSCDatabase->z_NvsPos.d_ToaSec = 0;
		ALOGE("+++ %s: Cannot open aiding file-%s +++\n", __FUNCTION__, uAidingPath);
	}

/****** GPSCAlmanacFile  ******/
	MCP_HAL_STRING_StrCpy (uAidingPath,(McpU8*)p_GPSCSysHandlers->uAidingPath);
	MCP_HAL_STRING_StrCat (uAidingPath,C_STR_AID_ALMANAC_FILE);

	eRetVal = mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *)uAidingPath,
						MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_RDONLY, &fd);
	if(eRetVal == RES_OK)
	{
		mcpf_file_read(p_zGPSCControl->p_zSysHandlers->hMcpf, fd,
			( void* ) &p_zGPSCDatabase->z_DBAlmanac[0], sizeof( gpsc_db_alm_type) * N_SV  );
		mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
	}
	else
	{
		ALOGE("+++ %s: Cannot open aiding file-%s +++\n", __FUNCTION__, uAidingPath);
	}

/****** GPSCEphFile  ******/
	MCP_HAL_STRING_StrCpy (uAidingPath,(McpU8*)p_GPSCSysHandlers->uAidingPath);
	MCP_HAL_STRING_StrCat (uAidingPath,C_STR_AID_EPH_FILE);

	eRetVal = mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *)uAidingPath,
						MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_RDONLY, &fd);
	if(eRetVal == RES_OK)
	{
		mcpf_file_read(p_zGPSCControl->p_zSysHandlers->hMcpf, fd,
			( void* ) &p_zGPSCDatabase->z_DBEphemeris[0], sizeof( gpsc_db_eph_type) * N_SV  );
		mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
	}
	else
	{
		ALOGE("+++ %s: Cannot open aiding file-%s +++\n", __FUNCTION__, uAidingPath);
	}

/****** GPSCIonoFile  ******/
	MCP_HAL_STRING_StrCpy (uAidingPath,(McpU8*)p_GPSCSysHandlers->uAidingPath);
	MCP_HAL_STRING_StrCat (uAidingPath,C_STR_AID_IONO_FILE);

	eRetVal = mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *)uAidingPath,
						MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_RDONLY, &fd);
	if(eRetVal == RES_OK)
	{
		mcpf_file_read(p_zGPSCControl->p_zSysHandlers->hMcpf, fd,
			( void* ) &p_zGPSCDatabase->z_DBIono, sizeof( gpsc_db_iono_type )  );
		mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
	}
	else
	{
		p_zGPSCDatabase->z_DBIono.v_iono_valid = 0;
		ALOGE("+++ %s: Cannot open aiding file-%s +++\n", __FUNCTION__, uAidingPath);
	}

/****** GPSCUtcFile  ******/
	MCP_HAL_STRING_StrCpy (uAidingPath,(McpU8*)p_GPSCSysHandlers->uAidingPath);
	MCP_HAL_STRING_StrCat (uAidingPath,C_STR_AID_UTC_FILE);

	eRetVal = mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *)uAidingPath,
						MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_RDONLY, &fd);
	if (eRetVal == RES_OK)
	{
		mcpf_file_read(p_zGPSCControl->p_zSysHandlers->hMcpf, fd,
						( void* ) &p_zGPSCDatabase->z_DBUtc, sizeof( gpsc_db_utc_type )  );
		mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
	}
	else
	{
		p_zGPSCDatabase->z_DBUtc.u_valid = 0;
		ALOGE("+++ %s: Cannot open aiding file-%s +++\n", __FUNCTION__, uAidingPath);
	}

/****** GPSCHealthFile  ******/
	MCP_HAL_STRING_StrCpy (uAidingPath,(McpU8*)p_GPSCSysHandlers->uAidingPath);
	MCP_HAL_STRING_StrCat (uAidingPath,C_STR_AID_HEALTH_FILE);

	eRetVal = mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *)uAidingPath,
						MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_RDONLY, &fd);
	if(eRetVal == RES_OK)
	{
		mcpf_file_read(p_zGPSCControl->p_zSysHandlers->hMcpf, fd,
			( void* ) &p_zGPSCDatabase->z_DBHealth, sizeof( gpsc_db_health_type )  );
		mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
	}
	else
	{
		
		for ( u_i = 0; u_i < N_SV; u_i++)
		{   
			   p_zGPSCDatabase->z_DBHealth.u_AlmHealth[u_i] = C_HEALTH_UNKNOWN;
		}
		ALOGE("+++ %s: Cannot open aiding file-%s +++\n", __FUNCTION__, uAidingPath);
	}

/****** GPSCTcxoFile  ******/
	MCP_HAL_STRING_StrCpy (uAidingPath,(McpU8*)p_GPSCSysHandlers->uAidingPath);
	MCP_HAL_STRING_StrCat (uAidingPath,C_STR_AID_TCXO_FILE);

	eRetVal = mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *)uAidingPath,
						MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_RDONLY, &fd);
	if(eRetVal == RES_OK)
	{
		mcpf_file_read(p_zGPSCControl->p_zSysHandlers->hMcpf, fd,
						(void *)&p_zGPSCDatabase->z_DBGpsInjClock, sizeof(gpsc_db_gps_inj_clk_type));
		mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
	}
	else
	{
		ALOGE("+++ %s: Cannot open aiding file-%s +++\n", __FUNCTION__, uAidingPath);
	}

    /* We have a valid TCXO */
    if(p_zGPSCDatabase->z_DBGpsInjClock.u_Valid ==TRUE)
	{
		p_zCustom->custom_assist_availability_flag |= C_TCXO_AVAIL;
  	}

	
#if 0
{ 
	McpU8 i;


	STATUSMSG("****** gpsc_config file check ******\n");
	STATUSMSG("driver_tx_response_required %d\n", p_zGPSCControl->p_zGPSCConfig->driver_tx_response_required);
	STATUSMSG("ai2_ack_required %d\n", p_zGPSCControl->p_zGPSCConfig->ai2_ack_required);
	STATUSMSG("auto_power_save_enable %d\n", p_zGPSCControl->p_zGPSCConfig->auto_power_save_enable);
	STATUSMSG("auto_power_ready_enable %d\n", p_zGPSCControl->p_zGPSCConfig->auto_power_ready_enable);
	STATUSMSG("power_mgmt_enable %d\n", p_zGPSCControl->p_zGPSCConfig->power_mgmt_enable);
	STATUSMSG("driver_tx_timeout %d\n", p_zGPSCControl->p_zGPSCConfig->driver_tx_timeout);
	STATUSMSG("ai2_comm_timeout %d\n", p_zGPSCControl->p_zGPSCConfig->ai2_comm_timeout);
	STATUSMSG("auto_power_save_timeout %d\n", p_zGPSCControl->p_zGPSCConfig->auto_power_save_timeout);
	STATUSMSG("internal_1 %d\n", p_zGPSCControl->p_zGPSCConfig->internal_1);
	STATUSMSG("internal_2 %d\n", p_zGPSCControl->p_zGPSCConfig->internal_2);
	STATUSMSG("internal_3 %d\n", p_zGPSCControl->p_zGPSCConfig->internal_3);
	STATUSMSG("internal_4 %d\n", p_zGPSCControl->p_zGPSCConfig->internal_4);

	STATUSMSG("smlc_comm_timeout %d\n", p_zGPSCControl->p_zGPSCConfig->smlc_comm_timeout);
	STATUSMSG("sleep_entry_delay_timeout %d\n", p_zGPSCControl->p_zGPSCConfig->sleep_entry_delay_timeout);
	STATUSMSG("default_max_ttff %d\n", p_zGPSCControl->p_zGPSCConfig->default_max_ttff);
	STATUSMSG("patch_available %d\n", p_zGPSCControl->p_zGPSCConfig->patch_available);

	STATUSMSG("comm_config %d\n", p_zGPSCControl->p_zGPSCConfig->comm_config);
	//get_uart_baud_rate(p_zGPSCControl->p_zGPSCConfig->comm_config);

	STATUSMSG("assist_bitmap_msbased_mandatory_mask %d\n", p_zGPSCControl->p_zGPSCConfig->assist_bitmap_msbased_mandatory_mask);
	STATUSMSG("assist_bitmap_msassisted_mandatory_mask %d\n", p_zGPSCControl->p_zGPSCConfig->assist_bitmap_msassisted_mandatory_mask);

	for(i=0;i<GPSC_MAX_COMPATIBLE_VERSIONS;i++)
		STATUSMSG("compatible_gps_versions[i] 0x%x\n", p_zGPSCControl->p_zGPSCConfig->compatible_gps_versions[i]);

	STATUSMSG("ref_clock_frequency %d\n", p_zGPSCControl->p_zGPSCConfig->ref_clock_frequency);
	STATUSMSG("ref_clock_quality %d\n", p_zGPSCControl->p_zGPSCConfig->ref_clock_quality);
	STATUSMSG("max_clock_acceleration %d\n", p_zGPSCControl->p_zGPSCConfig->max_clock_acceleration);
	STATUSMSG("max_user_acceleration %d\n", p_zGPSCControl->p_zGPSCConfig->max_user_acceleration);
	STATUSMSG("zzz_align0 %d\n", p_zGPSCControl->p_zGPSCConfig->zzz_align0);
	STATUSMSG("zzz_align1 %d\n", p_zGPSCControl->p_zGPSCConfig->zzz_align1);
	STATUSMSG("altitude_hold_mode %d\n", p_zGPSCControl->p_zGPSCConfig->altitude_hold_mode);
	STATUSMSG("elevation_mask %d\n", p_zGPSCControl->p_zGPSCConfig->elevation_mask);
	STATUSMSG("pdop_mask %d\n", p_zGPSCControl->p_zGPSCConfig->pdop_mask);
	STATUSMSG("zzz_align2 %d\n", p_zGPSCControl->p_zGPSCConfig->zzz_align2);
	STATUSMSG("zzz_align3 %d\n", p_zGPSCControl->p_zGPSCConfig->zzz_align3);
	STATUSMSG("timestamp_edge %d\n", p_zGPSCControl->p_zGPSCConfig->timestamp_edge);
	STATUSMSG("pa_blank_enable %d\n", p_zGPSCControl->p_zGPSCConfig->pa_blank_enable);
	STATUSMSG("pa_blank_polarity %d\n", p_zGPSCControl->p_zGPSCConfig->pa_blank_polarity);
	STATUSMSG("gps_minimum_week %d\n", p_zGPSCControl->p_zGPSCConfig->gps_minimum_week);
	STATUSMSG("utc_leap_seconds %d\n", p_zGPSCControl->p_zGPSCConfig->utc_leap_seconds);
	STATUSMSG("zzz_align4 %d\n", p_zGPSCControl->p_zGPSCConfig->zzz_align4);
	STATUSMSG("diag_report_control %d\n", p_zGPSCControl->p_zGPSCConfig->diag_report_control);
	STATUSMSG("front_end_loss %d\n", p_zGPSCControl->p_zGPSCConfig->front_end_loss);
	STATUSMSG("kalman_feat_control %d\n", p_zGPSCControl->p_zGPSCConfig->kalman_feat_control);
	STATUSMSG("checksum %d\n", p_zGPSCControl->p_zGPSCConfig->checksum);
	STATUSMSG("zzz_align5 %d\n", p_zGPSCControl->p_zGPSCConfig->zzz_align5);
	STATUSMSG("zzz_align6 %d\n", p_zGPSCControl->p_zGPSCConfig->zzz_align6);
	STATUSMSG("zzz_align7 %d\n", p_zGPSCControl->p_zGPSCConfig->zzz_align7);
	STATUSMSG("apm_control %d\n", p_zGPSCControl->p_zGPSCConfig->apm_control);
//	STATUSMSG("power_mode %d\n", p_zGPSCControl->p_zGPSCConfig->power_mode);
	STATUSMSG("search_mode %d\n", p_zGPSCControl->p_zGPSCConfig->search_mode);
	STATUSMSG("saving_options %d\n", p_zGPSCControl->p_zGPSCConfig->saving_options);
	STATUSMSG("power_save_qc %d\n", p_zGPSCControl->p_zGPSCConfig->power_save_qc);

	STATUSMSG("sbas_control %d\n", p_zGPSCControl->p_zGPSCConfig->sbas_control);
	STATUSMSG("sbas_prn %d\n", p_zGPSCControl->p_zGPSCConfig->sbas_prn);
	STATUSMSG("sbas_mode %d\n", p_zGPSCControl->p_zGPSCConfig->sbas_mode);
	STATUSMSG("sbas_flags %d\n", p_zGPSCControl->p_zGPSCConfig->sbas_flags);
	STATUSMSG("block_almanac %d\n", p_zGPSCControl->p_zGPSCConfig->block_almanac);
	STATUSMSG("rx_opmode %d\n", p_zGPSCControl->p_zGPSCConfig->rx_opmode);
	STATUSMSG("lna_crystal %d\n", p_zGPSCControl->p_zGPSCConfig->lna_crystal);
	STATUSMSG("enable_timeout %d\n", p_zGPSCControl->p_zGPSCConfig->enable_timeout);



/*	STATUSMSG("timeout_info %d\n", p_zGPSCControl->p_zGPSCConfig->timeout_info);
	STATUSMSG("time_session1 %d\n", p_zGPSCControl->p_zGPSCConfig->time_session1);
	STATUSMSG("time_session2 %d\n", p_zGPSCControl->p_zGPSCConfig->time_session2);
	STATUSMSG("time_session3 %d\n", p_zGPSCControl->p_zGPSCConfig->time_session3);
	STATUSMSG("time_sigma1 %d\n", p_zGPSCControl->p_zGPSCConfig->time_sigma1);
	STATUSMSG("time_sigma2 %d\n", p_zGPSCControl->p_zGPSCConfig->time_sigma2);
	STATUSMSG("time_sigma3 %d\n", p_zGPSCControl->p_zGPSCConfig->time_sigma3);
	STATUSMSG("time_sigma4 %d\n", p_zGPSCControl->p_zGPSCConfig->time_sigma4);
	STATUSMSG("cep_info %d\n", p_zGPSCControl->p_zGPSCConfig->cep_info);
	STATUSMSG("time_sigma_control %d\n", p_zGPSCControl->p_zGPSCConfig->time_sigma_control);

*/

	STATUSMSG("timeout1 %d\n", p_zGPSCControl->p_zGPSCConfig->timeout1);
	STATUSMSG("timeout2 %d\n", p_zGPSCControl->p_zGPSCConfig->timeout2);
	STATUSMSG("accuracy1 %d\n", p_zGPSCControl->p_zGPSCConfig->accuracy1);
	STATUSMSG("accuracy2 %d\n", p_zGPSCControl->p_zGPSCConfig->accuracy2);

	STATUSMSG("autonomous_test_flag %d\n", p_zGPSCControl->p_zGPSCConfig->autonomous_test_flag);
	STATUSMSG("rti_enable %d\n", p_zGPSCControl->p_zGPSCConfig->rti_enable);
	STATUSMSG("data_wipe %d\n", p_zGPSCControl->p_zGPSCConfig->data_wipe);
	STATUSMSG("dll %d\n", p_zGPSCControl->p_zGPSCConfig->dll);
	STATUSMSG("carrier_phase %d\n", p_zGPSCControl->p_zGPSCConfig->carrier_phase);
	STATUSMSG("hw_req_opt %d\n", p_zGPSCControl->p_zGPSCConfig->hw_req_opt);
	STATUSMSG("zzz_align8 %d\n", p_zGPSCControl->p_zGPSCConfig->zzz_align8);
	STATUSMSG("hw_assert_delay %d\n", p_zGPSCControl->p_zGPSCConfig->hw_assert_delay);
	STATUSMSG("hw_reassert_delay %d\n", p_zGPSCControl->p_zGPSCConfig->hw_reassert_delay);
	STATUSMSG("hw_ref_clk_req_opt %d\n", p_zGPSCControl->p_zGPSCConfig->hw_ref_clk_req_opt);
	STATUSMSG("hw_ref_clk_req_sig_sel %d\n", p_zGPSCControl->p_zGPSCConfig->hw_ref_clk_req_sig_sel);
	STATUSMSG("hw_ref_clk_assert_dly %d\n", p_zGPSCControl->p_zGPSCConfig->hw_ref_clk_assert_dly);
	STATUSMSG("hw_ref_clk_reassert_dly %d\n", p_zGPSCControl->p_zGPSCConfig->hw_ref_clk_reassert_dly);
	STATUSMSG("hw_sigout_type_ctrl %d\n", p_zGPSCControl->p_zGPSCConfig->hw_sigout_type_ctrl);
	STATUSMSG("zzz_align9 %d\n", p_zGPSCControl->p_zGPSCConfig->zzz_align9);
	STATUSMSG("tcxo_startup_time %d\n", p_zGPSCControl->p_zGPSCConfig->tcxo_startup_time);
	STATUSMSG("gps_sleep_ctrl %d\n", p_zGPSCControl->p_zGPSCConfig->gps_sleep_ctrl);
	STATUSMSG("geofence_enable %d\n", p_zGPSCControl->p_zGPSCConfig->geofence_enable);
	STATUSMSG("recv_min_rep_period %d\n", p_zGPSCControl->p_zGPSCConfig->recv_min_rep_period);
	STATUSMSG("count_invalid_fix %d\n", p_zGPSCControl->p_zGPSCConfig->count_invalid_fix);
	STATUSMSG("zzz_align10 %d\n", p_zGPSCControl->p_zGPSCConfig->zzz_align10);


	STATUSMSG("low_power_state %d\n", p_zGPSCControl->p_zGPSCConfig->low_power_state);
	STATUSMSG("calib_period %d\n", p_zGPSCControl->p_zGPSCConfig->calib_period);
	STATUSMSG("period_uncertainity %d\n", p_zGPSCControl->p_zGPSCConfig->period_uncertainity);
	STATUSMSG("max_gps_clk_unc %d\n", p_zGPSCControl->p_zGPSCConfig->max_gps_clk_unc);
	STATUSMSG("shrt_term_gps_clk_unc %d\n", p_zGPSCControl->p_zGPSCConfig->shrt_term_gps_clk_unc);
	STATUSMSG("system_time_unc %d\n", p_zGPSCControl->p_zGPSCConfig->system_time_unc);
	STATUSMSG("enable_manualrefclk %d\n", p_zGPSCControl->p_zGPSCConfig->enable_manualrefclk);
	STATUSMSG("enable_finetime %d\n", p_zGPSCControl->p_zGPSCConfig->enable_finetime);
	STATUSMSG("altitude_estimate %d\n", p_zGPSCControl->p_zGPSCConfig->altitude_estimate);
	STATUSMSG("altitude_unc %d\n", p_zGPSCControl->p_zGPSCConfig->altitude_unc);
	STATUSMSG("ref_clock_calib_type %d\n", p_zGPSCControl->p_zGPSCConfig->ref_clock_calib_type);
	STATUSMSG("priority_sagps %d\n", p_zGPSCControl->p_zGPSCConfig->priority_sagps);

	STATUSMSG("priority_pgps %d\n", p_zGPSCControl->p_zGPSCConfig->priority_pgps);
	STATUSMSG("priority_supl %d\n", p_zGPSCControl->p_zGPSCConfig->priority_supl);
	STATUSMSG("priority_cplane %d\n", p_zGPSCControl->p_zGPSCConfig->priority_cplane);
	STATUSMSG("priority_custom_agps_provider1 %d\n", p_zGPSCControl->p_zGPSCConfig->priority_custom_agps_provider1);
	STATUSMSG("priority_custom_agps_provider2 %d\n", p_zGPSCControl->p_zGPSCConfig->priority_custom_agps_provider2);
	STATUSMSG("priority_custom_agps_provider3 %d\n", p_zGPSCControl->p_zGPSCConfig->priority_custom_agps_provider3);
	STATUSMSG("pgps_sagps_timeout1 %d\n", p_zGPSCControl->p_zGPSCConfig->pgps_sagps_timeout1);
	STATUSMSG("pgps_sagps_timeout2 %d\n", p_zGPSCControl->p_zGPSCConfig->pgps_sagps_timeout2);
	STATUSMSG("pgps_sagps_accuracy1 %d\n", p_zGPSCControl->p_zGPSCConfig->pgps_sagps_accuracy1);
	STATUSMSG("pgps_sagps_accuracy2 %d\n", p_zGPSCControl->p_zGPSCConfig->pgps_sagps_accuracy2);
	STATUSMSG("alt_2d_pdop %d\n", p_zGPSCControl->p_zGPSCConfig->alt_2d_pdop);
	STATUSMSG("tcxo_unc_longterm %d\n", p_zGPSCControl->p_zGPSCConfig->tcxo_unc_longterm);
	STATUSMSG("tcxo_aging %d\n", p_zGPSCControl->p_zGPSCConfig->tcxo_aging);



	/*New Variables Added for M4 Release*/
	STATUSMSG("sys_time_sync %d\n", p_zGPSCControl->p_zGPSCConfig->sys_time_sync);
	STATUSMSG("unc_threshold %d\n", p_zGPSCControl->p_zGPSCConfig->unc_threshold);
	STATUSMSG("pos_velocty %d\n", p_zGPSCControl->p_zGPSCConfig->pos_velocty);
	STATUSMSG("pos_unc_threshold %d\n", p_zGPSCControl->p_zGPSCConfig->pos_unc_threshold);
	STATUSMSG("hours_thrshold %d\n", p_zGPSCControl->p_zGPSCConfig->hours_thrshold);
	STATUSMSG("week_threshold %d\n", p_zGPSCControl->p_zGPSCConfig->week_threshold);
	STATUSMSG("rtc_quality %d\n", p_zGPSCControl->p_zGPSCConfig->rtc_quality);
	STATUSMSG("rtc_calibration %d\n", p_zGPSCControl->p_zGPSCConfig->rtc_calibration);
	STATUSMSG("prm_adjustment %d\n", p_zGPSCControl->p_zGPSCConfig->prm_adjustment);
	STATUSMSG("pdop_mask_time_out %d\n", p_zGPSCControl->p_zGPSCConfig->pdop_mask_time_out);
	STATUSMSG("pps_output %d\n", p_zGPSCControl->p_zGPSCConfig->pps_output);
	STATUSMSG("pps_polarity %d\n", p_zGPSCControl->p_zGPSCConfig->pps_polarity);
	STATUSMSG("time_injection %d\n", p_zGPSCControl->p_zGPSCConfig->time_injection);
	STATUSMSG("enable_rtc_time_injection %d\n", p_zGPSCControl->p_zGPSCConfig->enable_rtc_time_injection);
	STATUSMSG("time_inj_check %d\n", p_zGPSCControl->p_zGPSCConfig->time_inj_check);
	STATUSMSG("time_validate_check %d\n", p_zGPSCControl->p_zGPSCConfig->time_validate_check);
	STATUSMSG("pos_inject_check %d\n", p_zGPSCControl->p_zGPSCConfig->pos_inject_check);
	STATUSMSG("pos_validate_check %d\n", p_zGPSCControl->p_zGPSCConfig->pos_validate_check);
	STATUSMSG("eph_inject_check %d\n", p_zGPSCControl->p_zGPSCConfig->eph_inject_check);
	STATUSMSG("eph_validate_check %d\n", p_zGPSCControl->p_zGPSCConfig->eph_validate_check);
	STATUSMSG("alm_inject_check %d\n", p_zGPSCControl->p_zGPSCConfig->alm_inject_check);
	STATUSMSG("alm_validate_check %d\n", p_zGPSCControl->p_zGPSCConfig->alm_validate_check);
	STATUSMSG("tcxo_inject_check %d\n", p_zGPSCControl->p_zGPSCConfig->tcxo_inject_check);
	STATUSMSG("tcxo_validate_check %d\n", p_zGPSCControl->p_zGPSCConfig->tcxo_validate_check);
	STATUSMSG("utc_inject_check %d\n", p_zGPSCControl->p_zGPSCConfig->utc_inject_check);
	STATUSMSG("utc_validate_check %d\n", p_zGPSCControl->p_zGPSCConfig->utc_validate_check);
	STATUSMSG("ion_inject_check %d\n", p_zGPSCControl->p_zGPSCConfig->ion_inject_check);
	STATUSMSG("ion_validate_check %d\n", p_zGPSCControl->p_zGPSCConfig->ion_validate_check);
	STATUSMSG("health_inject_check %d\n", p_zGPSCControl->p_zGPSCConfig->health_inject_check);
	STATUSMSG("health_validate_check %d\n", p_zGPSCControl->p_zGPSCConfig->health_validate_check);
	STATUSMSG("systemtime_or_injectedtime %d\n", p_zGPSCControl->p_zGPSCConfig->systemtime_or_injectedtime);

	
	STATUSMSG("tone_power %d\n", p_zGPSCControl->p_zGPSCConfig->tone_power);
	STATUSMSG("nf_correction_factor %d\n", p_zGPSCControl->p_zGPSCConfig->nf_correction_factor);
	STATUSMSG("fft_average_number %d\n", p_zGPSCControl->p_zGPSCConfig->fft_average_number);


	}
#endif
	return GPSC_SUCCESS;
}


void gpsc_init_result_buffer(gpsc_ctrl_type*    p_zGPSCControl)
{
	  gpsc_result_type*	p_zResultBuffer = p_zGPSCControl->p_zResultBuffer;

	  /* Cast the buffer as Result type structure, get offset of NMEA buffer */
	  p_zResultBuffer->p_sNmeaPointer = &p_zResultBuffer->s_ResultBuffer[0];
	  *p_zResultBuffer->p_sNmeaPointer = '\0';
	  p_zResultBuffer->u_NmeaStringCount = 0;
}


void gpsc_init_flags(gpsc_ctrl_type*    p_zGPSCControl)
{
  gpsc_state_type*             p_zGPSCState       = p_zGPSCControl->p_zGPSCState;
  p_zGPSCState->u_LocationRequestPending = FALSE;
  p_zGPSCState->u_PowerSaveRequestPending = FALSE;
  p_zGPSCState->u_ShutDownRequestPending = FALSE;
}

/*
 ******************************************************************************
 * gpsc_init_event_config
 *
 * Function description:
 *
 * If gpsc_cfg_type is not found in non-volatile memory, this function creates
 * such a file with default values.
 *
 * Parameters:
 *
 *  p_zGPSCConfig: pointer to configuration record
 *
 * u_UpdateNvs: TRUE - update NVS; FALSE: don't update NVS
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/

void gpsc_init_event_config(gpsc_ctrl_type*	p_zGPSCControl)
{

	gpsc_event_cfg_type*    p_zEventCfg = p_zGPSCControl->p_zEventCfg;
	gpsc_cfg_type* p_zGPSCConfig = p_zGPSCControl->p_zGPSCConfig;
	gpsc_custom_struct*		p_zGPSCCustom = p_zGPSCControl->p_zCustomStruct;


	/* init event config - STATE_INIT */
	p_zEventCfg->q_PeriodicReportFlags = 0;
	p_zEventCfg->q_AsyncEventFlags = 0;
	p_zEventCfg->u_PortNum = 1;
	p_zEventCfg->u_Ext_reports = 1; /* Extended reports enabled by default */

	if(p_zGPSCConfig->recv_min_rep_period == 500)
	{
       p_zEventCfg->w_PeriodicReportRate = 0;
       p_zEventCfg->u_SubSecPeriodicReportRate = 1;
	}
	else /* if(p_zGPSCConfig->recv_min_rep_period = 500) */
	{
	/* By default, for NL5350 */
	p_zEventCfg->w_PeriodicReportRate = 1;
    p_zEventCfg->u_SubSecPeriodicReportRate = 0;
	}

	/* CUSTOM */
	p_zGPSCCustom->custom_pos_rep_ext_req_flag = FALSE;
	p_zGPSCCustom->custom_sleep_req_flag = FALSE;
	p_zGPSCCustom->custom_wakeup_req_flag = FALSE;
	p_zGPSCCustom->custom_ver_req_flag = FALSE;
	p_zGPSCCustom->custom_assist_availability_flag = 0;
	p_zGPSCCustom->custom_pred_eph_bitmap = 0;
	p_zGPSCCustom->wakeup = TRUE;
	p_zGPSCCustom->loc_req_timer_first_time_check_flag = FALSE;

	
}


void gpsc_init_syshandlers(gpsc_sys_handlers*    p_GPSCSysHandlers)
{

	// Initilize to default values.
	MCP_HAL_STRING_StrCpy (p_GPSCSysHandlers->uGpscConfigFile,"GPSCConfigFile.cfg");
	MCP_HAL_STRING_StrCpy (p_GPSCSysHandlers->uPatchFile,"patch-X.0.ce");
	MCP_HAL_STRING_StrCpy (p_GPSCSysHandlers->uAidingPath, "");
	MCP_HAL_STRING_StrCpy (p_GPSCSysHandlers->uUdpIp, "");
	MCP_HAL_STRING_StrCpy (p_GPSCSysHandlers->uLogNvmPath, "/data/location/TI/");
	MCP_HAL_STRING_StrCpy (p_GPSCSysHandlers->uSensorDataNvmPath, "/data/location/TI/");

	p_GPSCSysHandlers->uUdpPort = 0;
	p_GPSCSysHandlers->uLogControl = NAVC_LOG_MODE_UDP;
	p_GPSCSysHandlers->uSensorDebugControl = NAVC_LOG_MODE_NVM;
	p_GPSCSysHandlers->uMaxLogLines = 6500;
	p_GPSCSysHandlers->uMaxNvmFiles = 100;
}

void gpsc_init_priority()
{

	STATUSMSG("gpsc_init_priority: setting supl and control plane priority \n");
	//Navc_Set_Priority(NAVC_SUPL_MO,NAVC_SUPL_MO,NAVC_ACCEPTINCOMING_STOPONGOING);
//	Navc_Set_Priority(NAVC_SUPL_MO,NAVC_SUPL_MT_POSITIONING,NAVC_ACCEPTINCOMING_STOPONGOING);
	Navc_Set_Priority(NAVC_SUPL_MO,NAVC_CP_NILR,NAVC_ACCEPTINCOMING_STOPONGOING);
	Navc_Set_Priority(NAVC_SUPL_MT_POSITIONING,NAVC_CP_NILR,NAVC_ACCEPTINCOMING_STOPONGOING);
	Navc_Set_Priority(NAVC_CP_NILR,NAVC_SUPL_MO,NAVC_IGNOREINCOMING);
	Navc_Set_Priority(NAVC_CP_NILR,NAVC_SUPL_MT_POSITIONING,NAVC_HOLD);
	Navc_Set_Priority(NAVC_STANDLONE,NAVC_STANDLONE,NAVC_NOEFFECT);
	Navc_Set_Priority(NAVC_STANDLONE,NAVC_CP_NILR,NAVC_NOEFFECT);
	Navc_Set_Priority(NAVC_STANDLONE,NAVC_SUPL_MO,NAVC_NOEFFECT);
	Navc_Set_Priority(NAVC_STANDLONE,NAVC_SUPL_MT_POSITIONING,NAVC_NOEFFECT);
	Navc_Set_Priority(NAVC_CP_NILR,NAVC_STANDLONE,NAVC_NOEFFECT);
	Navc_Set_Priority(NAVC_SUPL_MO,NAVC_STANDLONE,NAVC_NOEFFECT);
	Navc_Set_Priority(NAVC_SUPL_MT_POSITIONING,NAVC_STANDLONE,NAVC_NOEFFECT);
	
	
}
