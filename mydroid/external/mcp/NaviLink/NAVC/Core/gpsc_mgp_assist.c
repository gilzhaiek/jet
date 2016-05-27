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
 * FileName			:	gpsc_mgp_assist.c
 *
 * Description     	:
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */
#include "gpsc_data.h"
#include "gpsc_mgp_tx.h"
#include "gpsc_sess.h"

#include "gpsc_pm_assist.h"
#include "gpsc_mgp_cfg.h"
#include "gpsc_msg.h"
#include "gpsc_state.h"
#include "gpsc_ext_api.h"
#include "gpsc_comm.h"
#include "mcpf_mem.h"
#include "gpsc_mgp_assist.h"
#include "gpsc_cd.h"

#include "mcpf_services.h"

#include <utils/Log.h>
#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG "NAVD.GPSC_MGP_ASSIST"    // our identification for logcat (adb logcat NAVD.GPSC_MGP_ASSIST:V *:S)

typedef struct
{
   U8 u_SvId;
   U8 u_Elev;
}SvElev; /*  */


#define C_STEER_RUN_SEL  0x01
#define C_STEER_FORCE    0x02
enum
{
   APPLY_ASSIST_INIT,
   APPLY_ASSIST_INJ_TIME,
   APPLY_ASSIST_INJ_POS,
   APPLY_ASSIST_INJ_ALT,
   APPLY_ASSIST_INJ_IONO,
   APPLY_ASSIST_INJ_EPH,
   APPLY_ASSIST_INJ_ALM,
   APPLY_ASSIST_SORT_STEERING,
   APPLY_ASSIST_INJ_STEERING,
   APPLY_ASSIST_INJ_PMATCH,
   APPLY_ASSIST_INJ_SVDIFF,
   APPLY_ASSIST_INJ_SVDIR,
   APPLY_ASSIST_INJ_RTI,
   APPLY_ASSIST_INJ_DGPS,    // added for DGPS Support 27/07/06-Raghavendra
   APPLY_ASSIST_INJ_UTC,     // added for UTC support
   APPLY_ASSIST_END
};

/* GPS Acquisition Assistance message Code Phase Search Window parameters.
   The parameters are assumed to be one sided search window beginning with
   SMLC version 4.0.0.13, so there is no need to half the values any more.
   Conversion is made from units of chips to ms.
   (1023 chips = 1ms) */

static const FLT SvTimeUnc[16] = 
{
    1.0f,
    (FLT)C_MSEC_PER_CHIP,
    2.0f * (FLT)C_MSEC_PER_CHIP,
    3.0f * (FLT)C_MSEC_PER_CHIP,
    4.0f * (FLT)C_MSEC_PER_CHIP,
    6.0f * (FLT)C_MSEC_PER_CHIP,
    8.0f * (FLT)C_MSEC_PER_CHIP,
    12.0f * (FLT)C_MSEC_PER_CHIP,
    16.0f * (FLT)C_MSEC_PER_CHIP,
    24.0f * (FLT)C_MSEC_PER_CHIP,
    32.0f * (FLT)C_MSEC_PER_CHIP,
    48.0f * (FLT)C_MSEC_PER_CHIP,
    64.0f * (FLT)C_MSEC_PER_CHIP,
    96.0f * (FLT)C_MSEC_PER_CHIP,
    128.0f * (FLT)C_MSEC_PER_CHIP,
    192.0f * (FLT)C_MSEC_PER_CHIP };

/* Local function protocol ***/

/********************************************************************
 *
 * gpsc_mgp_assist_apply
 *
 * Function description:
 *
 * Applies SMLC provided assistance data to Sensor.
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
 *********************************************************************/

U8 gpsc_mgp_assist_apply (gpsc_ctrl_type* p_zGPSCControl)
{
  gpsc_smlc_assist_type*  p_zSmlcAssist        = p_zGPSCControl->p_zSmlcAssist;
  gpsc_db_type*           p_zGPSCDatabase      = p_zGPSCControl->p_zGPSCDatabase;
  gpsc_db_gps_clk_type*   p_zDBGpsClock        = &p_zGPSCDatabase->z_DBGpsClock;
  U16                     w_AvailabilityFlags  = p_zSmlcAssist->w_AvailabilityFlags;
  gpsc_state_type*	     p_zGPSCState         = p_zGPSCControl->p_zGPSCState;
  gpsc_comm_tx_buff_type* p_Ai2TxBuf           = p_zGPSCControl->p_zGPSCCommTxBuff;
  gpsc_cfg_type*          p_zGPSCConfig        = p_zGPSCControl->p_zGPSCConfig;

  if (w_AvailabilityFlags == 0)
	  return FALSE;

  /* Stay in while loop till there is space to add more 100 bytes to buffer
   So that we dont exceed the max limit  specified by GPSM 8 */

  while (gpsc_comm_check_bufferspace(p_zGPSCControl, 0))
  {
  switch(p_zGPSCState->u_GPSCSubState)
  {
 /******************************************************************************/
    case APPLY_ASSIST_INIT:
  	  	p_zSmlcAssist->w_CurrentWeek = p_zDBGpsClock->w_GpsWeek;
		p_zSmlcAssist->q_CurrentMsec = p_zDBGpsClock->q_GpsMs;
/*		p_zSmlcAssist->f_MinUnc = 999999.0f;
		p_zSmlcAssist->w_BestRef = 0;
		p_zSmlcAssist->w_NDiffs = 0; */
		p_zSmlcAssist->u_SvCounter = 0;
		//p_zSmlcAssist->u_InjVisible = FALSE;
		p_zSmlcAssist->q_VisiableBitmap = 0;
		//p_zSmlcAssist->q_InjSvEph = 0;

		p_zGPSCState->u_GPSCSubState= APPLY_ASSIST_INJ_TIME;
	break;
/******************************************************************************/
    case APPLY_ASSIST_INJ_TIME:
        /*** Reference time provided by SMLC ***/
        if (p_zSmlcAssist->w_AvailabilityFlags & C_REF_GPSTIME_AVAIL )
        {
            ALOGV("+++ %s: APPLY_ASSIST_INJ_TIME +++\n", __FUNCTION__);
            gpsc_mgp_inject_gps_time(p_zGPSCControl, &p_zSmlcAssist->z_InjectTimeEst);

            p_zSmlcAssist->w_InjectedFlags |= C_REF_GPSTIME_AVAIL;
            p_zSmlcAssist->w_AvailabilityFlags &=  ~C_REF_GPSTIME_AVAIL;
            
            /* Injecting the time will trigger an assistance request */
            p_zSmlcAssist->u_NumAssistAttempt = 0;
        }


        p_zGPSCState->u_GPSCSubState= APPLY_ASSIST_INJ_POS;
        break;
/******************************************************************************/
    case APPLY_ASSIST_INJ_POS:
	/*** Reference position provided by SMLC ***/
	if (p_zSmlcAssist->w_AvailabilityFlags & C_REFLOC_AVAIL )
	{
		ALOGV("+++ %s: APPLY_ASSIST_INJ_POS +++\n", __FUNCTION__);
		gpsc_mgp_inject_pos_est(p_zGPSCControl, &p_zSmlcAssist->z_InjectPosEst, TRUE);

        p_zSmlcAssist->w_InjectedFlags |= C_REFLOC_AVAIL;
		  p_zSmlcAssist->w_AvailabilityFlags &=  ~C_REFLOC_AVAIL;
	  }
  	  p_zGPSCState->u_GPSCSubState= APPLY_ASSIST_INJ_ALT;
	break;

 /******************************************************************************/
    case APPLY_ASSIST_INJ_ALT:

	  /*** Inject Altitude: in order of preference & availability, 1. smlc 2. last known 3. default ***/
	  if((w_AvailabilityFlags & C_ALT_AVAIL) &&
		((p_zGPSCConfig->altitude_hold_mode == GPSC_ALT_HOLD_ALLOW_2D) ||
		 (p_zGPSCConfig->altitude_hold_mode == GPSC_ALT_HOLD_MANUAL_2D)))
		{	
         // allow for altitude injection when 2D is selected
			gpsc_inject_altitude_type zInjectAltitude;
			if (p_zSmlcAssist->w_InjectedFlags & C_REFLOC_AVAIL)
				{	// ref position is available
					zInjectAltitude.x_Altitude = p_zSmlcAssist->z_InjectPosEst.x_Ht/2.0; //KNS; To take care of the correct alt injection
					zInjectAltitude.w_AltitudeUnc = p_zGPSCConfig->altitude_unc;  //99meters
					zInjectAltitude.u_ForceFlag = 1;

					ALOGV("+++ %s: Injecting SMLC Altitude +++\n", __FUNCTION__);
				}
			else if (p_zGPSCDatabase->z_DBPos.d_ToaSec > 0)
				{	// database contains last known position, use this to reinject the altitude
					zInjectAltitude.x_Altitude =p_zGPSCDatabase->z_DBPos.z_ReportPos.x_Height/2.0;
					zInjectAltitude.w_AltitudeUnc = p_zGPSCConfig->altitude_unc;  //99meters
					zInjectAltitude.u_ForceFlag = 1;

					ALOGV("+++ %s: Injecting last known Altitude +++\n", __FUNCTION__);
				}
			else
				{	// no position info is available, inject a default altitude of 100m with unc of 99m
					zInjectAltitude.x_Altitude = p_zGPSCConfig->altitude_estimate;		//100meters
					zInjectAltitude.w_AltitudeUnc = p_zGPSCConfig->altitude_unc;  //99meters
					zInjectAltitude.u_ForceFlag = 1;

					ALOGV("+++ %s: Injecting Default Altitude +++\n", __FUNCTION__);
				}

			gpsc_mgp_inject_altitude(p_zGPSCControl, &zInjectAltitude);

			p_zSmlcAssist->w_InjectedFlags |= C_ALT_AVAIL;
			p_zSmlcAssist->w_AvailabilityFlags &=  ~C_ALT_AVAIL;

		}
  	  p_zGPSCState->u_GPSCSubState= APPLY_ASSIST_INJ_IONO;
	break;


 /******************************************************************************/
	case APPLY_ASSIST_INJ_IONO:
	 /*** If Iono is provided by SMLC ****************/
	  if ( p_zSmlcAssist->w_AvailabilityFlags & C_IONO_AVAIL )
	  {
        ALOGV("+++ %s: Injected Iono to target +++\n", __FUNCTION__);
		  gpsc_mgp_inject_iono(p_zGPSCControl, &p_zSmlcAssist->z_SmlcRawIono );
		
		  p_zSmlcAssist->w_AvailabilityFlags &=  ~C_IONO_AVAIL;
	  }
	  p_zGPSCState->u_GPSCSubState = APPLY_ASSIST_INJ_UTC;
	break;
	/******************************************************************************/
	case APPLY_ASSIST_INJ_UTC:
	 /*** If UTC is provided by SMLC ****************/
	  if ( p_zSmlcAssist->w_AvailabilityFlags & C_UTC_AVAIL )
	  {
        ALOGV("+++ %s: Injected UTC to target +++\n", __FUNCTION__);
		  gpsc_mgp_inject_utc(p_zGPSCControl, &p_zSmlcAssist->z_SmlcRawUtc );
		  p_zSmlcAssist->w_AvailabilityFlags &=  ~C_UTC_AVAIL;
	  }

	  p_zGPSCState->u_GPSCSubState = APPLY_ASSIST_INJ_EPH;
	break;
 /******************************************************************************/
	case APPLY_ASSIST_INJ_EPH:
	  if ( (p_zSmlcAssist->w_AvailabilityFlags & C_EPH_AVAIL ))
	  {

      ALOGV("+++ %s: Injected Ephemeris to target +++\n", __FUNCTION__);
		if (gpsc_mgp_assist_eph_apply(p_zGPSCControl) == TRUE)
		{
			p_zSmlcAssist->w_InjectedFlags |= C_EPH_AVAIL;
			break; // Stay in same state till there are more eph
		}
		else
		{
			ALOGV("+++ %s: Ephemeris Injection Completed +++\n", __FUNCTION__);
			p_zSmlcAssist->w_AvailabilityFlags &= ~C_EPH_AVAIL;
		}

		if (p_zSmlcAssist->w_InjectedFlags & C_EPH_AVAIL)
		{
			gpsc_inject_cepinfo_start_mode(p_zGPSCControl,9,18,100,150);
		}
		
	  }
      /* Reset persistant counter for almanac injection*/
	  p_zGPSCState->u_GPSCSubState = APPLY_ASSIST_INJ_ALM;
	break;
/******************************************************************************/
	case APPLY_ASSIST_INJ_ALM:
	  /*If almanac is provided by SMLC*/

	  if (p_zSmlcAssist->w_AvailabilityFlags & C_ALM_AVAIL )
	  {
		U8 u_i = p_zSmlcAssist->u_SvCounter;
		pe_RawAlmanac *p_zSmlcRawAlm;

		while(u_i<N_SV && (p_zSmlcAssist->z_SmlcRawAlmanac[u_i].u_Prn)==0)
			u_i++;

	
		if (u_i < N_SV)
		{
			p_zSmlcRawAlm = &p_zSmlcAssist->z_SmlcRawAlmanac[u_i];

			/* need to reconcile alm. week number only if gps week is present else send the
			   almanac week as it is*/
		    if ( p_zSmlcAssist->w_CurrentWeek != C_GPS_WEEK_UNKNOWN  )
			    alm_week_reconcile(p_zSmlcRawAlm,p_zSmlcAssist->w_CurrentWeek);
			else
				 gpsc_diagnostic(GPSC_DIAG_GPSM_INTERACTION, "Can't reconcile alm. week number");

			p_zSmlcAssist->q_AlmPresent |= (U32)1L<<u_i;

         ALOGV("+++ %s: Injecting Almanac for SV [%d] to target +++\n", __FUNCTION__, p_zSmlcRawAlm->u_Prn);
			gpsc_mgp_inject_alm( p_zGPSCControl, p_zSmlcRawAlm );

			p_zSmlcRawAlm->u_Prn = 0;

			// Advance counter to the next sv found
			p_zSmlcAssist->u_SvCounter = u_i;
			break; // break out of for loop
		}
	  }
	  p_zSmlcAssist->w_AvailabilityFlags &=  ~C_ALM_AVAIL;
	  p_zGPSCState->u_GPSCSubState = APPLY_ASSIST_SORT_STEERING;
	break;

 /******************************************************************************/
	case APPLY_ASSIST_SORT_STEERING:
	 /*** If Acquistion Assistance is provided by SMLC ****/
	  if ( p_zSmlcAssist->w_AvailabilityFlags & C_ACQ_AVAIL )
	  {
		  gpsc_mgp_assist_steering_sort(p_zGPSCControl);
		  p_zSmlcAssist->u_SvCounter = 0;
		  p_zGPSCState->u_GPSCSubState = APPLY_ASSIST_INJ_STEERING;
		  break;
	  }
	  p_zGPSCState->u_GPSCSubState = APPLY_ASSIST_INJ_PMATCH;
	break;
 /******************************************************************************/
	case APPLY_ASSIST_INJ_STEERING:

	  /* Remain in this state till there is a steering to apply */
	  if (gpsc_mgp_assist_steering_apply(p_zGPSCControl) != TRUE)
	  {
     	  gpsc_smlc_assist_type*  p_zSmlcAssist  = p_zGPSCControl->p_zSmlcAssist;
		  p_zGPSCState->u_GPSCSubState = APPLY_ASSIST_INJ_SVDIFF;
		  p_zSmlcAssist->w_AvailabilityFlags &=  ~C_ACQ_AVAIL;
		  p_zSmlcAssist->w_InjectedFlags |= C_ACQ_AVAIL;
		  p_zSmlcAssist->u_SvCounterSteer = 0;
        p_zSmlcAssist->u_NumSvToInj = 0;
	  	  p_zGPSCControl->p_zSvInject->u_SvSteerInjectCount = 0;

		STATUSMSG("gpsc_mgp_assist_apply: Injected all steering, BestRef:%d, Ndiffs: %d",
				p_zSmlcAssist->w_BestRef, p_zSmlcAssist->w_NDiffs);

	  }

	break;
 /******************************************************************************/
	case APPLY_ASSIST_INJ_SVDIFF:
	  p_zSmlcAssist->u_SvCounter = 0;
	  gpsc_mgp_assist_svdiff_apply(p_zGPSCControl);
	  p_zGPSCState->u_GPSCSubState = APPLY_ASSIST_INJ_SVDIR;
	break;
 /******************************************************************************/
	case APPLY_ASSIST_INJ_SVDIR:
		{
		gpsc_smlc_assist_type*  p_zSmlcAssist  = p_zGPSCControl->p_zSmlcAssist;
		gpsc_acq_assist_type   *p_zAcquisAssist = &p_zSmlcAssist->z_AcquisAssist;
		gpsc_mgp_assist_svdir_apply(p_zGPSCControl);
		gpsc_mgp_assist_svdir_save(p_zGPSCControl);
		p_zAcquisAssist->u_NumSv=0;
		p_zGPSCState->u_GPSCSubState = APPLY_ASSIST_INJ_PMATCH;
		}
	break;
 /******************************************************************************/
	case APPLY_ASSIST_INJ_PMATCH:
	  /*** Do pattern match ******/
 	  if ( p_zSmlcAssist->w_AvailabilityFlags & C_REF_TOW_AVAIL )
	  {
		 gpsc_mgp_assist_pmatch_apply(p_zGPSCControl);
		  p_zSmlcAssist->w_AvailabilityFlags &=  ~C_REF_TOW_AVAIL;
	  } /* if ( w_AvailabilityFlags & C_REF_TOW_AVAIL )  */

	  p_zGPSCState->u_GPSCSubState = APPLY_ASSIST_INJ_RTI;
	break;
 /******************************************************************************/
	case APPLY_ASSIST_INJ_RTI:
	  /*** If RTI is provided but alamanac is not provided by SMLC, i.e.,
		   RTI has not been processed, we then rely on Sensor's knowledge
		   about non-existing SVs, and for the existing ones, we can use
		   RTI to modify its GOOD/BAD status */
	  gpsc_mgp_assist_rti_apply(p_zGPSCControl);
	  p_zGPSCState->u_GPSCSubState = APPLY_ASSIST_INJ_DGPS;   /* changed for DGPS support 27/07/06 --Raghavendra*/
     	  break;

 /******************************************************************************/
    case APPLY_ASSIST_INJ_DGPS:   /* added for DGPS support 27/07/06 --Raghavendra*/

	  if ( p_zSmlcAssist->w_AvailabilityFlags & C_DGPS_AVAIL )
	  {
		gpsc_mgp_inject_dgps(p_zGPSCControl, &p_zSmlcAssist->z_DgpsAssist );
		p_zSmlcAssist->w_InjectedFlags |= C_DGPS_AVAIL;

		p_zSmlcAssist->w_AvailabilityFlags &=  ~C_DGPS_AVAIL;
	  }
	  p_zGPSCState->u_GPSCSubState = APPLY_ASSIST_END;
	break;
 /******************************************************************************/
	case APPLY_ASSIST_END:
	// All injection complete clear database 
	if (p_Ai2TxBuf->u_FirstPacket !=1 )
		return TRUE;

	return FALSE;
 /******************************************************************************/
  }
  }/* End of while loop*/
  return TRUE;

}

/********************************************************************
 *
 * acq_assist_propagate
 *
 * Function description:
 *
 * Recover SvTimeMsec from the modulo 80ms bits information,
 * propagates the SV time to the FCOUNT of GpsTime database,
 * and populate the structure suitable for AI2 steering injection.
 *
 * Parameters:
 *
 * p_zAcquisElement: pointer to the structure holding SMLC acq. assis
 *                   data.
 *
 * p_zInjSteer: pointer to the structure to be populated with data
 *              suitable for AI2 steering injection.
 *
 * q_AssistToaMs: Time of applicability of the SMLC assistance data
 *                in terms of Gps msec.
 *
 * p_zDGGpsClock: pointer to the Gps clock time database.
 *
 * Return:
 *   None
 *
 *********************************************************************/
static void acq_assist_propagate(gpsc_acq_element_type *p_zAcquisElement,
              me_SvSteering *p_zSvSteer, me_SvSteering   *p_zSaveSvSteer,
              U32 q_AssistToaMs, gpsc_db_gps_clk_type *p_zDBGpsClock)
{

  me_SvTime z_MeSvTime, *p_zMeSvTime = &z_MeSvTime;
  DBL d_PropMs;
  U8  u_YmodR;
  U16 w_Word;
  S32 l_MsecCorrOn70ms; /* correction to SV msec on top of the minus 70ms
                 from TOA **/
  FLT f_SvSpeed;
  FLT f_SvSpeedUnc;
  DBL d_TimeDelta;


  /* propagate assistance data to the FCOUNT associated with local Gps time */
  p_zSvSteer->q_FCount = p_zDBGpsClock->q_FCount;

  /* from time-of-applicability of acq. assistance data to the above FCOUNT,
     propagated time in msec is : WHAT IF WEEK BOUNDARY HAS BEEN CROSSED ???  */
  d_PropMs = (DBL) ((S32)(p_zDBGpsClock->q_GpsMs - q_AssistToaMs))
           - p_zDBGpsClock->f_TimeBias;

  /* Need to construct the whole SV Time Msec received at toa of acq.
     assistance, the nominal transit delay for the GPS signal is around
     73 msec. */

  /* the mod 80 ms value of the SvTimeMsec is */
  u_YmodR =(U8)( p_zAcquisElement->u_GpsBitNum * 20
          + p_zAcquisElement->u_IntCodePhase);

  /* the true SvTimeMsec is within (q_AssistToaMs - 73) - 5.51 to
     (q_AssistToaMs - 73) + 13.17, which means it's either within the same
     mod80 window, or less than half into the previous or the next window,
     a correction can be caluculated using the function minXMinusY **/
  l_MsecCorrOn70ms = minXMinusY( (U32)(q_AssistToaMs - 73),
                                 (U32)u_YmodR , (S32)80);

  /* SvTimeMsec at TOA established **/
  p_zMeSvTime->q_Ms = q_AssistToaMs - 73 + l_MsecCorrOn70ms;

  /* SvTimeSubMsec at TOA: */
  p_zMeSvTime->f_SubMs = (FLT)(1022 - p_zAcquisElement->w_CodePhase)
                       / (FLT)1023.0;

  /* Sv speed: meters/sec **/
  f_SvSpeed = (FLT)p_zAcquisElement->x_Doppler0 * (FLT)2.5 *
            (FLT)C_L1HzToMs * (FLT)(-1.0);

  /* resolution 0.01 m/s, value limited so that 24 bits enough,
     Bit 23 sign bit */
  p_zSvSteer->f_SvSpeed = f_SvSpeed;


  p_zSvSteer->f_SvSpeedUnc = 0; /* these two may or may not have been
                                   provided */
  p_zSvSteer->f_SvAccel = 0;

  if (p_zAcquisElement->u_DopplerUnc != 255 ) /* doppler unc. provided */
  {
    /* Sv doppler unc represented as
       200 * 2 ** (-p_zAcquisElement->u_DopplerUnc) */
    w_Word = (U16)200;
    w_Word <<= 8; /* so that later less than 0.5 Hz can be retained */
    w_Word >>= p_zAcquisElement->u_DopplerUnc;
    f_SvSpeedUnc = (FLT)(w_Word >> 8);
    if ( w_Word & 0x08 )
    {
      f_SvSpeedUnc += 0.5;
    }
    f_SvSpeedUnc *= (FLT)C_L1HzToMs; /* convert from Hz to m/s */
    p_zSvSteer->f_SvSpeedUnc = f_SvSpeedUnc;
  }

  p_zSvSteer->f_SvTimeUncMs = (FLT)SvTimeUnc[p_zAcquisElement->u_SrchWin]
                            + p_zDBGpsClock->f_TimeUnc
                            + (FLT)ext_fabs(d_PropMs) *
                                p_zSvSteer->f_SvSpeedUnc *
                                  (FLT)(1.0f/(FLT)LIGHT_SEC);

  if ( p_zAcquisElement->u_Doppler1 != 255) /* doppler acceleration provided */
  {
    p_zSvSteer->f_SvAccel =
      ( (FLT)p_zAcquisElement->u_Doppler1 * 1.5f/63.0f - 1.0f ) *
      (FLT)C_L1HzToMs;
  }


  /* save speed and msec, subms for possible time-alignment use, note
     msec and subms are the ones before being propagated to the current
	 FCOUNT **/
  p_zSaveSvSteer->f_SvSpeed = p_zSvSteer->f_SvSpeed;
  p_zSaveSvSteer->z_SvTime.q_Ms = p_zMeSvTime->q_Ms;
  p_zSaveSvSteer->z_SvTime.f_SubMs = p_zMeSvTime->f_SubMs;

  /* Now propagate SvTimeSubMsec and SvTimeMsec to p_zInjSteer->q_FCount */
  d_TimeDelta = d_PropMs * (1.0f - f_SvSpeed * (1.0f/(FLT)LIGHT_SEC) );
  sv_time_delta( p_zMeSvTime, d_TimeDelta, TRUE);

  /* Now populate the AI2 InjSteer structure with SvTime Msec and SubMsec */
  p_zSvSteer->z_SvTime.q_Ms = p_zMeSvTime->q_Ms;
  p_zSvSteer->z_SvTime.f_SubMs = p_zMeSvTime->f_SubMs;

}


/********************************************************************
 *
 * gpsc_mgp_assist_eph_apply
 *
 * Function description:
 *
 * Applies ephemeris assistance data to the GPS.
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
 *   Mesage to transmit ? TRUE/FALSE
 *
 *********************************************************************/

U8 gpsc_mgp_assist_eph_apply (gpsc_ctrl_type *p_zGPSCControl)
{
	U8 u_i;
	pe_RawSF1Param  *p_zSmlcRawSF1Param;
	pe_RawEphemeris *p_zSmlcRawEph;
	gpsc_smlc_assist_type*  p_zSmlcAssist  = p_zGPSCControl->p_zSmlcAssist;

	U32 q_ToeMs; /* Toe converted to Msec into week */
	S32 l_MsDifference;

	if(gpsc_update_sv_visibility(p_zGPSCControl))
	{
		for (u_i=p_zSmlcAssist->u_SvCounter; u_i<N_SV; u_i++)
		{
			p_zSmlcRawSF1Param = (pe_RawSF1Param  *)&p_zSmlcAssist->z_SmlcRawSF1Param[u_i];
			p_zSmlcRawEph = (pe_RawEphemeris *)&p_zSmlcAssist->z_SmlcRawEph[u_i];

			if ((p_zSmlcRawEph->u_Prn) && (p_zSmlcAssist->q_VisiableBitmap&(McpU32)mcpf_Pow(2,u_i)))
			{
				gpsc_inject_eph_type z_InjectEph, *p_zInjectEph = &z_InjectEph;
				if ((p_zSmlcRawEph->u_Prn >= 193) && (p_zSmlcRawEph->u_Prn <= 202))
				{
					STATUSMSG("---QZSS SVIDs suppressed in Inject Ephemeris--- %d ",p_zSmlcRawEph->u_Prn);
					continue;
				}

				p_zInjectEph->p_zRawEphemeris = p_zSmlcRawEph;
				p_zInjectEph->p_zRawSF1Param  = p_zSmlcRawSF1Param;

				/* handles the special condition of the last 2 hours of a week:
				Gps time in one week while Toe is referenced to the next week */
				if (( p_zSmlcAssist->w_CurrentWeek != C_GPS_WEEK_UNKNOWN) && (p_zSmlcAssist->w_CurrentWeek != 0))
				{
					/* assume that the Toe is in the current week */
					p_zSmlcAssist->w_EphWeek = p_zSmlcAssist->w_CurrentWeek;
					q_ToeMs = (U32)p_zSmlcRawEph->w_Toe * (U32)(16 * 1000); /* unit 16 sec */
					l_MsDifference = (S32)p_zSmlcAssist->q_CurrentMsec - (S32)q_ToeMs;
					if (l_MsDifference > (S32)(WEEK_MSECS/2.0) )
					{
						/* handles the special condition of the last 2 hours of a week */
						p_zSmlcAssist->w_EphWeek++;
					}
					else if (l_MsDifference < ( 0 - (S32)(WEEK_MSECS/2.0) ) )
					{
						/* should never get here, as SMLC never gives eph.
						whose toe is in the past */
						p_zSmlcAssist->w_EphWeek--;
					}

					p_zInjectEph->w_EphWeek  = p_zSmlcAssist->w_EphWeek;
				  }
				  else
	           			p_zInjectEph->w_EphWeek = p_zSmlcAssist->w_CurrentWeek;


				gpsc_mgp_inject_eph(p_zGPSCControl, p_zInjectEph,1);
				STATUSMSG("gpsc_mgp_assist_eph_apply: Injected Visible Ephemeris (to target) for Sv %u ", p_zSmlcRawEph->u_Prn);
				/*Clear satellite from records*/
				p_zSmlcRawEph->u_Prn =0;
				/*Set counter to next injected ephemeris*/
				p_zSmlcAssist->u_SvCounter =(U8)( u_i + 1);
				return TRUE;
			}
		}

		/* Injection is complete, set the counter to zero */
		p_zSmlcAssist->u_SvCounter = 0;

	}
	else
	{
		STATUSMSG("gpsc_mgp_assist_eph_apply: unable to compute visibility");
	}

	return FALSE;
}

/********************************************************************
 *
 * gpsc_mgp_assist_rti_apply
 *
 * Function description:
 *
 * Applies Real time Integrity assistance data to the GPS.
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
 *   Mesage to transmit ? TRUE/FALSE
 *
 *********************************************************************/
U8 gpsc_mgp_assist_rti_apply (gpsc_ctrl_type *p_zGPSCControl)
{
  gpsc_smlc_assist_type*  p_zSmlcAssist  = p_zGPSCControl->p_zSmlcAssist;
  gpsc_db_type*           p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
  gpsc_db_health_type*    p_zDBHealth     = &p_zGPSCDatabase->z_DBHealth;
  gpsc_db_gps_clk_type*   p_zDBGpsClock  = &p_zGPSCDatabase->z_DBGpsClock;

  U16   w_AvailabilityFlags  = p_zSmlcAssist->w_AvailabilityFlags;
  if (w_AvailabilityFlags & C_ALM_AVAIL )
  {
	/* from the almanac list, we know which SVs are not in existence,
	   so if we don't get real time integrity, at leaset we should inform
	   Sensor what SVs are not in existence if not already known. We must
	   have Sensor's knowledge of health before we can modify it, because
	   this action can only identify non-existing ones, and we must not
	   corrupt other SVs whose health info cannot be inferred from SMLC
	   almanac assistance */
	U8 u_i;
	U8 u_SvHealth[N_SV];
	if ( !(w_AvailabilityFlags & C_RTI_AVAIL) &&
		( p_zDBHealth->d_UpdateTimeSec > 0 )
	   )
	{

	  mcpf_mem_copy(p_zGPSCControl->p_zSysHandlers->hMcpf, &u_SvHealth[0], &p_zDBHealth->u_AlmHealth[0], N_SV);

	  for (u_i=0; u_i<N_SV; u_i++)
	  {
		if ( p_zSmlcAssist->q_AlmPresent & (U32)(1L<<u_i))
		{
		  u_SvHealth[u_i] = C_HEALTH_NOEXIST;
		}
	  }

	  if (MemDiff(&u_SvHealth[0], &p_zDBHealth->u_AlmHealth[0], N_SV) )
	  {
		/* if Sensor didn't know before some of the SVs were not in
		   existence and we now know that due to the alamanc assistance
		   data from SMLC, tell Sensor so */
		gpsc_mgp_inject_sv_health(p_zGPSCControl, &u_SvHealth[0]);
		return TRUE;
	  }

	}

	/* if SMLC provides both almanac and RTI, we can uniquely identify the
	   health GOOD/BAD/NotExist status without the help of Sensor's current
	   health knowledge */
	else if (w_AvailabilityFlags & C_RTI_AVAIL)
	{
	  /* note: GSM spec. allows SvId of 0 - 63, here we assume only 1-32
		 are of interest */

	  for (u_i=0; u_i< N_SV; u_i++)
	  {

		if ( ( p_zSmlcAssist->q_RealTimeIntegrity[0] &
				  (U32)(1L << u_i)
			  ) == 0  )
		{
		  /* if RTI says "healthy", it has to be GOOD */
		  u_SvHealth[ u_i ] = C_HEALTH_GOOD;
		}
		else if (p_zSmlcAssist->q_AlmPresent&
				  (U32)(1L << u_i))
		{
		  /* if RTI says "unhealthy", and the SV is not in alamanc,
			 it must be non-existing */
		  u_SvHealth[ u_i ] = C_HEALTH_NOEXIST;
		}
		else
		{
		  /* if RTI says "unhealthy", and the SV is in alamanc,
			 it must be BAD */
		  u_SvHealth[ u_i ] = C_HEALTH_BAD;
		}

	  }

	  gpsc_mgp_inject_sv_health(p_zGPSCControl, &u_SvHealth[0]);
	  STATUSMSG("Status: Injected health");

	  /* p_zSmlcAssist->w_AvailabilityFlags &= ~C_RTI_AVAIL;  so that the part of the code that
							processes only RTI will not redo this part */
	  return TRUE;
	}

  }
  if ( w_AvailabilityFlags & C_RTI_AVAIL )
  {

	/*** Frist, if we do have health information, we need to modify it
		 using RIT ***/
	if (p_zDBHealth->d_UpdateTimeSec > 0.0)
	{
	  U8 u_SvHealth[N_SV], u_i;


	  mcpf_mem_copy(p_zGPSCControl->p_zSysHandlers->hMcpf, &u_SvHealth[0], &p_zDBHealth->u_AlmHealth[0], N_SV);

	  for (u_i=0; u_i < N_SV; u_i++)
	  {
		if ( !(p_zSmlcAssist->q_RealTimeIntegrity[0] & (U32)(1L<< u_i)) )
		{
		  u_SvHealth[u_i] = C_HEALTH_GOOD;
		}
	  }

	  if (MemDiff(&u_SvHealth[0], &p_zDBHealth->u_AlmHealth[0], N_SV) )
	  {
		gpsc_mgp_inject_sv_health(p_zGPSCControl, &u_SvHealth[0]);
     	STATUSMSG("Status: Injected health");
		return TRUE;
	  }

	  else /* if same, at least update the UpdateTime so that we'll know
			  how fresh the health info is */
	  {
		if ( (p_zDBGpsClock->w_GpsWeek != C_GPS_WEEK_UNKNOWN ) &&
			 (p_zDBGpsClock->f_TimeUnc < C_GOOD_HEALTH_AGE_SEC * 0.1f )
		   )
		{
          /* if we have time, and Tunc less than 10% of the defined good
             health age, reset time stamp of the SV health database */
          p_zDBHealth->d_UpdateTimeSec = FullGpsMs(p_zDBGpsClock->w_GpsWeek,
                                        p_zDBGpsClock->q_GpsMs) * 0.001;
		}
	  }

	}

	else
	{
	  /* if we don't have health info., but we do have almanac,
		 construct health info. here using the combination of almanac
		 and RIT */
	  gpsc_db_alm_type*        p_zDBAlmanac;
	  U8 u_SvHealth[N_SV], u_i;

	  /** find out how many and which SVs existing */
	  for (u_i = 0; u_i < N_SV; u_i++)
	  {
		p_zDBAlmanac = &p_zGPSCDatabase->z_DBAlmanac[u_i];

		if ( ( p_zSmlcAssist->q_RealTimeIntegrity[0] &
				  (U32)(1L << u_i)
			  ) == 0  )
		{
		  /* if RTI says "healthy", it has to be GOOD */
		  u_SvHealth[u_i] = C_HEALTH_GOOD;
		}

		else if ( p_zDBAlmanac->u_Valid)
		{
		  u_SvHealth[u_i] = C_HEALTH_BAD;
		}
		else
		{
		  u_SvHealth[u_i] = C_HEALTH_NOEXIST;
		}

	  }

	  gpsc_mgp_inject_sv_health(p_zGPSCControl, &u_SvHealth[0]);
	  STATUSMSG("Status: Injected health");
	  return TRUE;

	}
  } /* end bracket of if-RTI-available **/
  return FALSE;
}

/********************************************************************
 *
 * gpsc_mgp_assist_steering_sort
 *
 * Function description:
 *
 * If more than.16 svs are provided, sort them in order of highest elev
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
 *   Mesage to transmit ? TRUE/FALSE
 *
 *********************************************************************/
U8 gpsc_mgp_assist_steering_sort(gpsc_ctrl_type *p_zGPSCControl)
{
	gpsc_smlc_assist_type*  p_zSmlcAssist  = p_zGPSCControl->p_zSmlcAssist;
	gpsc_acq_assist_type   *p_zAcquisAssist = &p_zSmlcAssist->z_AcquisAssist;
	gpsc_db_type*           p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_db_sv_steer_type     *p_zDBSvSteer = &p_zGPSCDatabase->z_DBSvSteer;
	gpsc_acq_element_type   *p_zAcquisElement;
	gpsc_additional_angle_type  *p_zAddAngle;


	U8 u_i, u_j, u_NumSv=0;


	p_zDBSvSteer->u_Valid = TRUE;
	u_NumSv = p_zAcquisAssist->u_NumSv;  /* number of SVs provided */
	if(u_NumSv == 0)
	{
		STATUSMSG("gpsc_mgp_assist_steering_sort: Error SMLC acq. assist data has NumSv = %d", u_NumSv);
	   return FALSE;
	}
	p_zSmlcAssist->u_NumSvToInj = u_NumSv;

	/* if more than N_LCHAN SVs, sort them with respect to the elevation */

	if (u_NumSv > N_LCHAN )
	{
		U8 u_HighestElev, u_HighestElevSvId, u_HighestElevIndx;
		SvElev z_SvElevOrig[16];  /* a copy of the original Sv elevation
								list from SMLC, clear elevation to
								255 after selected */
		p_zSmlcAssist->u_NumSvToInj = N_LCHAN;

		for (u_i=0; u_i<u_NumSv; u_i++)
		{
		  p_zAcquisElement = &p_zAcquisAssist->z_AcquisElement[u_i];
		  p_zAddAngle = &p_zAcquisAssist->z_AddAngle[u_i];
		  z_SvElevOrig[u_i].u_SvId = p_zAcquisElement->u_SvId;
		  z_SvElevOrig[u_i].u_Elev = p_zAddAngle->u_Elevation;
		}

		for (u_j = 0; u_j < N_LCHAN; u_j++)
		{
		  /* pick up the highest N_LCHAN SVs to z_SvElevHighest[] */
		  u_HighestElev = 0;
		  u_HighestElevSvId = 0;
		  u_HighestElevIndx = 0;
		  for (u_i = 0; u_i < u_NumSv; u_i++)
		  {
			if ( z_SvElevOrig[u_i].u_SvId )
			{
			  if ( z_SvElevOrig[u_i].u_Elev > u_HighestElev )
			  {
				u_HighestElev = z_SvElevOrig[u_i].u_Elev;
				u_HighestElevSvId = z_SvElevOrig[u_i].u_SvId;
				u_HighestElevIndx = u_i;
			  }
			}
		  }
		  /* stuff the highest Sv' ID in u_HighestSvs[] */
		  p_zSmlcAssist->u_HighestSvIndex[u_j] = u_HighestElevIndx;
		  /* removed the already-picked Sv from z_SvElevOrig[] */
		  z_SvElevOrig[u_HighestElevIndx].u_SvId = 0;
		  z_SvElevOrig[u_HighestElevIndx].u_Elev = 255;
		}
	}
	return FALSE;
}
/********************************************************************
 *
 * gpsc_mgp_assist_steering_apply
 *
 * Function description:
 *
 * Applies Sv Steering assistance data to the GPS.
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
 *   Mesage to transmit ? TRUE/FALSE
 *
 *********************************************************************/
U8 gpsc_mgp_assist_steering_apply (gpsc_ctrl_type *p_zGPSCControl)
{
	gpsc_smlc_assist_type*  p_zSmlcAssist  = p_zGPSCControl->p_zSmlcAssist;
	gpsc_acq_assist_type   *p_zAcquisAssist = &p_zSmlcAssist->z_AcquisAssist;
	gpsc_db_type*           p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_db_sv_steer_type     *p_zDBSvSteer = &p_zGPSCDatabase->z_DBSvSteer;
	gpsc_db_gps_clk_type*   p_zDBGpsClock  = &p_zGPSCDatabase->z_DBGpsClock;
	gpsc_db_gps_clk_type*   p_zAAGpsClock = &p_zAcquisAssist->p_zAAGpsClock;
	gpsc_sv_steer_inject_type* p_zSvInject = p_zGPSCControl->p_zSvInject;
	gpsc_steer_inject_per_sv_type* p_zPerSvSteerInject = NULL;
	gpsc_acq_element_type   *p_zAcquisElement;
	gpsc_sv_id_steer_type     *p_zSaveSvIdSteer;
	me_SvSteering   *p_zSaveSvSteer;
	me_SvSteering   z_SvSteer, *p_zSvSteer = &z_SvSteer;

	U8 u_i,u_SteerFlag;

	/* Store all Injected Svs in database as zSvInjectedReoprt */
	p_zPerSvSteerInject = &p_zSvInject->p_zPerSvSteerInject[p_zSvInject->u_SvSteerInjectCount];

	/*Initially set index of sv to inject as sv counter*/
	u_i = p_zSmlcAssist->u_SvCounterSteer;

	/*If either all steering injected or N_LCHAN number reached return*/
	if(u_i >= p_zSmlcAssist->u_NumSvToInj)
		return FALSE;

    /*If there are more than N_LCHAN satellites with steering, set index to next highest*/
	if( p_zAcquisAssist->u_NumSv > N_LCHAN )
	{
		/*Look up table for next highest sv*/
		u_i = p_zSmlcAssist->u_HighestSvIndex[u_i];
	}

	/*At this point u_i points to next sv to inject*/
	p_zAcquisElement = &p_zAcquisAssist->z_AcquisElement[u_i];
	p_zSaveSvIdSteer = &p_zDBSvSteer->z_SvIdSteer[u_i];
	p_zSaveSvSteer   = &p_zSaveSvIdSteer->z_SvSteer;

	/* recover SvTimeMsec, propagate SV time to FCOUNT of database clock,
	   but save the pre-propagated steering for possible time-alignment
	   use. This is because at this point the FCOUNT may not be referenced
	   to an accurate GPS time. */

	p_zSaveSvIdSteer->u_SvId = (U8)(p_zAcquisElement->u_SvId);

	if ( p_zSmlcAssist->w_NDiffs == 0 &&
		p_zDBGpsClock->u_Valid == TRUE)
	{
		p_zAAGpsClock->q_FCount = p_zDBGpsClock->q_FCount;
		p_zAAGpsClock->w_GpsWeek = p_zDBGpsClock->w_GpsWeek;
		p_zAAGpsClock->q_GpsMs = p_zDBGpsClock->q_GpsMs;
		p_zAAGpsClock->f_TimeBias = p_zDBGpsClock->f_TimeBias;
		p_zAAGpsClock->f_TimeUnc = p_zDBGpsClock->f_TimeUnc;
		p_zAAGpsClock->b_UTCDiff = p_zDBGpsClock->b_UTCDiff;
		p_zAAGpsClock->f_FreqBias = p_zDBGpsClock->f_FreqBias;
		p_zAAGpsClock->f_FreqUnc = p_zDBGpsClock->f_FreqUnc;
		p_zAAGpsClock->u_Valid = TRUE;

		STATUSMSG("gpsc_mgp_assist_steering_apply: FC: %d, %d", p_zAcquisAssist->p_zAAGpsClock.q_FCount, p_zAAGpsClock->q_FCount);
	}
	else
	{
		STATUSMSG("gpsc_mgp_assist_steering_apply: GPS Time is invalid");
	}


	if ( p_zAAGpsClock->u_Valid == FALSE)
		return FALSE;

	acq_assist_propagate(p_zAcquisElement, p_zSvSteer, p_zSaveSvSteer,
						 p_zAcquisElement->q_GpsTow, p_zAAGpsClock);

	/* for SV time diff., only up to N_LCHAN SV can be involved */
	if ( p_zSmlcAssist->w_NDiffs < N_LCHAN )
	{
	  p_zSmlcAssist->u_RefSv[p_zSmlcAssist->w_NDiffs] = p_zAcquisElement->u_SvId;
	  p_zSmlcAssist->z_RefSvSteer[p_zSmlcAssist->w_NDiffs] = z_SvSteer;

	  if ( p_zSvSteer->f_SvTimeUncMs < p_zSmlcAssist->f_MinUnc )
	  {
		p_zSmlcAssist->f_MinUnc = p_zSvSteer->f_SvTimeUncMs;
		p_zSmlcAssist->w_BestRef = p_zSmlcAssist->w_NDiffs;
		STATUSMSG("gpsc_mgp_assist_steering_apply: updated BestRef to SV%d", p_zSmlcAssist->w_BestRef);
	  }
	  p_zSmlcAssist->w_NDiffs++;
	}

	u_SteerFlag = C_STEER_FORCE;
	/*Increment counter*/
	p_zSmlcAssist->u_SvCounterSteer++;
	if (p_zSmlcAssist->u_SvCounterSteer  == p_zSmlcAssist->u_NumSvToInj )
	{
	  u_SteerFlag |= C_STEER_RUN_SEL;
	}
	if ((p_zAcquisElement->u_SvId >= 193) && (p_zAcquisElement->u_SvId <= 202))
	{
		STATUSMSG("---QZSS SVIDs suppressed in SV Steering Injection--- %d ",p_zAcquisElement->u_SvId);
		return FALSE;
	}

	/* Store Injected Svs in steering database, ref: gpsc_meas_filter */
	p_zPerSvSteerInject->u_SvSteerId = p_zAcquisElement->u_SvId;
	p_zSvInject->u_SvSteerInjectCount++;

	gpsc_mgp_inject_sv_steering(p_zGPSCControl, p_zSvSteer,
					 p_zAcquisElement->u_SvId, u_SteerFlag);


	return TRUE;
}
/********************************************************************
 *
 * gpsc_mgp_assist_svdiff_apply
 *
 * Function description:
 *
 * Applies Sv Difference assistance data to the GPS.
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
 *   Mesage to transmit ? TRUE/FALSE
 *
 *********************************************************************/
U8 gpsc_mgp_assist_svdiff_apply (gpsc_ctrl_type *p_zGPSCControl)
{
	gpsc_smlc_assist_type*  p_zSmlcAssist  = p_zGPSCControl->p_zSmlcAssist;
	gpsc_acq_assist_type   *p_zAcquisAssist = &p_zSmlcAssist->z_AcquisAssist;
	gpsc_db_type*           p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_db_sv_steer_type     *p_zDBSvSteer = &p_zGPSCDatabase->z_DBSvSteer;
	gpsc_db_gps_clk_type*   p_zAAGpsClock = &p_zAcquisAssist->p_zAAGpsClock;
	me_SvSteering   z_SvSteer, *p_zSvSteer = &z_SvSteer;
	gpsc_db_gps_meas_type*       p_zDBGpsMeas =
								   &p_zGPSCControl->p_zGPSCDatabase->z_DBGpsMeas;
	U16 w_NDiffs, w_BestRef;
	U8 u_i,u_NumSv;
	me_SvSteering *p_zRefSvSteer;

	u_NumSv = p_zAcquisAssist->u_NumSv;  /* number of SVs provided */

	for (u_i = u_NumSv; u_i < N_LCHAN; u_i++)
	{
		p_zDBSvSteer->z_SvIdSteer[u_i].u_SvId = 0; /* mark unused slots with id=0 */
	}
	w_NDiffs  = p_zSmlcAssist->w_NDiffs;
	w_BestRef = p_zSmlcAssist->w_BestRef;
	/** calculate diff. from the best SV (with least time unc ) */

	if (w_NDiffs > 1) /* there is at least more than 1 Svs, do diff */
	{
		FLT f_ClockTimeUncMs = p_zAAGpsClock->f_TimeUnc;
		U8 u_j;
		me_SvDiff z_SvDiff[N_LCHAN -1], *p_zSvDiff;

		p_zRefSvSteer = &p_zSmlcAssist->z_RefSvSteer[w_BestRef];

		for (u_j=0, u_i=0; u_i<w_NDiffs; u_i++)
		{
		  if (u_i == w_BestRef )
		  {
			continue; /* skip the ref. SV */
		  }

		  p_zSvSteer = &p_zSmlcAssist->z_RefSvSteer[u_i];

		  if (u_j < (N_LCHAN -1))
        {
		     p_zSvDiff = &z_SvDiff[u_j];

		     p_zSvDiff->u_Sv = p_zSmlcAssist->u_RefSv[u_i];
		     p_zSvDiff->u_DTimeStatus = SV_DTIME_STATUS_MS_VALID |
						    SV_DTIME_STATUS_SB_VALID |
						    SV_DTIME_STATUS_SM_VALID;
		     p_zSvDiff->f_DTimeMsecs = (FLT)((S32)(p_zSvSteer->z_SvTime.q_Ms -
											     p_zRefSvSteer->z_SvTime.q_Ms) ) +
						    p_zSvSteer->z_SvTime.f_SubMs -
						    p_zRefSvSteer->z_SvTime.f_SubMs;

			   /* Each Sv's f_SvTimeUncMs component has f_ClockTimeUncMs included in it.
			   * The whole point of using differences is that this uncertainty is removed.
			   * The subtract result can't be negative since the same clock time unc
			   * was included in the setting of the steering time unc up above.
			   */

		     p_zSvDiff->f_NDTimeUncMs = p_zSvSteer->f_SvTimeUncMs - f_ClockTimeUncMs;
		     p_zSvDiff->f_DSpeed = p_zSvSteer->f_SvSpeed - p_zRefSvSteer->f_SvSpeed;
		     p_zSvDiff->f_NDSpeedUnc = p_zSvSteer->f_SvSpeedUnc;
        }
		  u_j++;
		}

		/** inject SV Diff to Sensor ***/
		gpsc_mgp_inject_sv_diff(p_zGPSCControl,  p_zAAGpsClock->q_FCount,
					  p_zSmlcAssist->u_RefSv[w_BestRef], p_zRefSvSteer->f_SvTimeUncMs - f_ClockTimeUncMs,
					  p_zRefSvSteer->f_SvSpeedUnc, u_j, z_SvDiff);

			/** now request MeasStatus */
		AI2TX("Request Meas Status",AI_REQ_MEAS_STAT);
		gpsc_mgp_tx_req( p_zGPSCControl, AI_REQ_MEAS_STAT, 0 );

			/** indicate the need for init. "last observation" history */
		p_zDBGpsMeas->u_init_last = TRUE;
		return TRUE;
	} /* if (w_NDiffs > 1) */
  return FALSE;
}
/********************************************************************
 *
 * gpsc_mgp_assist_svdir_apply
 *
 * Function description:
 *
 * Applies Sv Direction assistance data to the GPS.
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
 *   Mesage to transmit ? TRUE/FALSE
 *
 *********************************************************************/
U8 gpsc_mgp_assist_svdir_apply (gpsc_ctrl_type *p_zGPSCControl)
{
	gpsc_smlc_assist_type*  p_zSmlcAssist  = p_zGPSCControl->p_zSmlcAssist;
	gpsc_acq_assist_type   *p_zAcquisAssist = &p_zSmlcAssist->z_AcquisAssist;
	gpsc_acq_element_type   *p_zAcquisElement;
	gpsc_additional_angle_type  *p_zAddAngle;
	U8 u_i;
	U16   w_AvailabilityFlags  = p_zSmlcAssist->w_AvailabilityFlags;

	if (w_AvailabilityFlags & C_ANGLE_AVAIL )
	{
		U8 u_cnt = 0,u_NumSv;


		gpsc_inj_sv_dir z_InjectSvDir[C_MAX_SVS_ACQ_ASSIST];
		gpsc_inj_sv_dir* p_zInjectSvDir;

		u_NumSv = p_zAcquisAssist->u_NumSv;
		for (u_i=0; u_i<u_NumSv; u_i++)
		{
			p_zAcquisElement = &p_zAcquisAssist->z_AcquisElement[u_i];
			if((p_zAcquisElement->u_SvId >= 193) && (p_zAcquisElement->u_SvId <= 202))
			{
				STATUSMSG("---QZSS SVIDs suppressed in SV Direction injection--- %d ",p_zAcquisElement->u_SvId);
				u_i--;
				u_NumSv--;

				continue;
			}

			p_zAddAngle = &p_zAcquisAssist->z_AddAngle[u_i];
			/*KlocWork Critical Issue:54 Resolved by adding boundary check*/
			if(u_cnt < C_MAX_SVS_ACQ_ASSIST)
			{
			 p_zInjectSvDir = &z_InjectSvDir[u_cnt];

			 p_zInjectSvDir->u_SvId = 0; /* init. to no-SvDir for this SV */

			if (p_zAddAngle->u_Elevation !=0xFF )
			{
				p_zInjectSvDir->u_SvId = p_zAcquisElement->u_SvId;

				p_zInjectSvDir->b_Elev =
				  (S8)((FLT)p_zAddAngle->u_Elevation * 16 );
				/* 16 = (90.0f/8.0f) / (180/256): RRLP and AI2 units */

				p_zInjectSvDir->u_Azim =
				  (U8)((FLT)p_zAddAngle->u_Azimuth * 8 );
				/* 8 = (360.0f/32.0f) / (360/256): RRLP and AI2 units */

				u_cnt++;
			}
			}

		}

		/* make sure the rest slots in SvDir represent no SVs */
		for (u_i=u_NumSv; u_i < C_MAX_SVS_ACQ_ASSIST; u_i++)
		{
			p_zInjectSvDir = &z_InjectSvDir[u_i];
			p_zInjectSvDir->u_SvId = 0;
		}
		if(((z_InjectSvDir->u_SvId) >= 193) && ((z_InjectSvDir->u_SvId) <= 202))
		{
		  STATUSMSG("---QZSS SVIDs suppressed in SV Direction injection--- %d ",(z_InjectSvDir->u_SvId));
		  return FALSE;
		}
		gpsc_mgp_inject_sv_dir(p_zGPSCControl, &z_InjectSvDir[0], u_cnt);
		return TRUE;
	} /* if (w_AvailabilityFlags & C_ANGLE_AVAIL ) */
	return FALSE;
}
/********************************************************************
 *
 * gpsc_mgp_assist_svdir_save
 *
 * Function description:
 *
 * Saves Sv Direction assistance data to the database.
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
 *   Mesage to transmit ? TRUE/FALSE
 *
 *********************************************************************/
U8 gpsc_mgp_assist_svdir_save (gpsc_ctrl_type *p_zGPSCControl)
{
	gpsc_smlc_assist_type*  p_zSmlcAssist  = p_zGPSCControl->p_zSmlcAssist;
	gpsc_acq_assist_type   *p_zAcquisAssist = &p_zSmlcAssist->z_AcquisAssist;
	gpsc_db_type*           p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_acq_element_type   *p_zAcquisElement;
	gpsc_additional_angle_type  *p_zAddAngle;

	U8      u_i, u_NumSv;
	u_NumSv = p_zAcquisAssist->u_NumSv;
	/* In MS-Assisted mode, LM needs to retain SV directions so that a PDOP
	   test can be performed to qualify measurements for return to the server.
	   At present, the AI2 protocol does not provide a reliable way to ensure
	   that the sensor will report the injected directions back to LM so that
	   LM can retain them in the LM database.  As a temporary expedient, the
	   directions that may arrive from the server with the acquisition
	   assistance data are now stored directly in the LM database.  It must be
	   understood that this violates the intent the of LM design, in which
	   the database is conceived as a helper for MS-based operation. */

#ifdef AUDIT_SVDIR_AND_PDOP
sm_LogMsgf("SERVER Injects %d SV Directions\n", u_NumSv);
#endif
	for (u_i=0; u_i<u_NumSv; u_i++)
	{
		U8	u_SvId;
		gpsc_db_sv_dir_type  *p_DBSvDirection = &p_zGPSCDatabase->z_DBSvDirection;
		me_SvDirection	   *p_RawSvDirection;

		p_zAcquisElement = &p_zAcquisAssist->z_AcquisElement[u_i];
		p_zAddAngle = &p_zAcquisAssist->z_AddAngle[u_i];
		u_SvId = p_zAcquisElement->u_SvId;
		if( (u_SvId) && (u_SvId <= 32) )
		{
			p_RawSvDirection = &p_DBSvDirection->z_RawSvDirection[u_SvId - 1];
			if (p_zAddAngle->u_Elevation !=0xFF )
			{
				/* Elev units in RRLP 90/8, in database 90/128 */
				/* Round, assuming that the most likely angle is the midpoint
				   of the coarse quantization interval */
				p_RawSvDirection->b_Elev = (S8)((p_zAddAngle->u_Elevation << 4) + (1<<3));
				/* Azim units in RRLP 360/32, in database 360/256 */
				p_RawSvDirection->u_Azim = (U8)((p_zAddAngle->u_Azimuth << 3) + (1<<2));
#ifdef AUDIT_SVDIR_AND_PDOP
	  sm_LogMsgf("SV%02d EL%03d AZ%03d\n", u_SvId,
					(S16) ((90.0/128.0)*(FLT)p_RawSvDirection->b_Elev),
					(U16) ((360.0/256.0)*(FLT)p_RawSvDirection->u_Azim));
#endif
			}
			else
			{
				p_RawSvDirection->b_Elev = -128;
				p_RawSvDirection->u_Azim = 0;
			}
		}
	} /* for (u_i=0; u_i<u_NumSv; u_i++) */
	return FALSE;
}
/********************************************************************
 *
 * gpsc_mgp_assist_pmatch_apply
 *
 * Function description:
 *
 * Applies Pattern Match assistance data to the GPS from time of week
 * assistance data.
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
 *   Mesage to transmit ? TRUE/FALSE
 *
 *********************************************************************/
U8 gpsc_mgp_assist_pmatch_apply (gpsc_ctrl_type *p_zGPSCControl)
{
	/* If GPS TOW Assist is provided by SMLC, then pattern match can be
	   enhanced by the predicted bits in this field. */
	gpsc_smlc_assist_type*  p_zSmlcAssist  = p_zGPSCControl->p_zSmlcAssist;
	gpsc_tlm_how_type     z_Tgpsc_How,  *p_zTlmHow = &z_Tgpsc_How;
	gpsc_pred_db_type      z_PredDb[12], *p_zPredDb;
	gpsc_raw_tow_type      *p_zRawTow;
	U8      u_i;
	U32         q_Sfow; /* subframe of week */
	U8 u_Injected = FALSE;
    U8 u_j = 0;

	for (u_i=0; u_i<N_SV; u_i++) /* RRLP provides max. 12 SVs for ToW Assistance */
	{
	  p_zRawTow = &p_zSmlcAssist->z_SmlcTow[u_i];

	  if (p_zRawTow->u_SvId == 0)
	  {
		  continue;
	  }

      if (u_j > 11)
         break;

      p_zPredDb = &z_PredDb[u_j++];

	  p_zPredDb->u_SvId = 0;

	  if ( p_zRawTow->u_SvId != 0 )
	  {
		if((p_zRawTow->u_SvId >= 193) && (p_zRawTow->u_SvId <= 202))
		{
			STATUSMSG("---QZSS SVIDs suppressed in Inject Predicted SV Data Bits--- %d ",p_zRawTow->u_SvId);
			continue;
		}

		STATUSMSG("Status: SvId: %d, Alert + Anti-Spoof bits: %d%d",
			p_zRawTow->u_SvId,p_zRawTow->u_AlertFlag,p_zRawTow->u_AntiSpoofFlag);
		p_zTlmHow->u_ReservedBits = (U8)(p_zRawTow->u_TlmRsvBits);
		p_zTlmHow->u_FlagBits = (U8)(p_zRawTow->u_AlertFlag << 1);
		p_zTlmHow->u_FlagBits |= p_zRawTow->u_AntiSpoofFlag;
		p_zTlmHow->w_TlmMessage = p_zRawTow->w_TlmWord;

		q_Sfow = p_zSmlcAssist->z_SmlcGpsTime.z_InjectGpsTimeEst.z_meTime.q_GpsMsec / 6000L + 1L;

		if (q_Sfow >= WEEK_ZCOUNTS )
		  q_Sfow = 0;

		p_zTlmHow->q_SubFrameOfWeek = q_Sfow;

		gpsc_pm_assist_tlm_how(p_zTlmHow);

		p_zPredDb->u_SvId = p_zRawTow->u_SvId;
		p_zPredDb->u_NumBits = 62; /* Sparse Pattern Match uses 62 bits */
		p_zPredDb->q_BitOfWeek = p_zTlmHow->q_BitOfWeek;
		p_zPredDb->q_PredictedBits[0] = p_zTlmHow->q_PredictedBits[0];
		p_zPredDb->q_PredictedBits[1] = p_zTlmHow->q_PredictedBits[1];
		u_Injected = TRUE;
		gpsc_mgps_inject_pred_data_bits(p_zGPSCControl, p_zPredDb);
	  }
	}
	return u_Injected;
}


void alm_week_reconcile(pe_RawAlmanac *p_zSmlcRawAlm, U16 w_CurrentWeek)
{
	U16 w_projected_alm_week = C_GPS_WEEK_UNKNOWN;
	S16  s_delta_week = 0;
	w_projected_alm_week = (U16)(p_zSmlcRawAlm->w_Week & 0x00FF); /* only 8 bits here  */
	w_projected_alm_week |= w_CurrentWeek & 0xFF00; /* add the high byte from clock info. */
	s_delta_week = (S8)((S16)w_CurrentWeek - (S16)w_projected_alm_week);

	/* assuming the true intended alm. week can't be 2.5 years away from the current time,
	a difference larger than 2.5 years can be attributed to the rollover of the 8-bit boundary
	RRLP/RRC alm. week employs */

	if ( s_delta_week > 128 )
	{
		w_projected_alm_week += 256;
	}
	else if ( s_delta_week < -128 )
	{
		w_projected_alm_week -= 256;
	}
	p_zSmlcRawAlm->w_Week = w_projected_alm_week;
}
