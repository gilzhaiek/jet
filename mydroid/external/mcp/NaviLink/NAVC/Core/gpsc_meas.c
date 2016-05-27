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
 * FileName			:	gpsc_meas.c
 *
 * Description     	:
 *  This file contains functions that handle GPS measurement sent from Sensor.
 *  It checks the quality of such a measurement, if good enough, it will call
 *  a function to report such measurement to SMLC if configuration calls for
 *  MS-Assisted; if not good enouch, it either simply wait for better ones, or
 *  will do time alignment aimed at improving measurement time uncertainty.
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#include "gpsc_database.h"
#include "gpsc_init.h"
#include "gpsc_mgp_cfg.h"
#include "gpsc_mgp_tx.h"
#include "gpsc_msg.h"
#include "gpsc_ext_api.h"
#include "gpsc_meas.h"
#include "navc_defs.h"


/* Structure to define the time uncertainty table with respect to SNR */
typedef struct
{
 U16 w_SnrDB;   /* Background SNR */
 FLT f_SvTimeUncMs; /* Observed Time Error on Simulated Data */
 FLT f_Slope;   /* Observed Slope of the Error curve */
} mc_TimeUnc;


const FLT mcf_SqrtBeqLookUp[11] =
{
 1.0f,
 0.41623f,
 0.31838f,
 0.26818f,
 0.23619f,
 0.21349f,
 0.19631f,
 0.18271f,
 0.17159f,
 0.16229f,
 0.15436f
};

#define C_TIMEUNC_TABLE_SIZE  6

FLT mc_TimeUncertainty ( U16 w_SnrDB, U8 u_FilterN );



/* max. SV time unc. representable in RRLP: 112 meters -> 0.00037359 msec */
#define C_MAX_PRM_RMS_ERR       0.00037359


#define C_NO_THRESH_TIMEUNC    150   /* Threshold for relaxing the SV time unc check in PE */
#define GOOD_MEAS				MEAS_STATUS_MS_VALID
#define C_LIGHT_SPEED_SEC		LIGHT_SEC /* light speed second - meters/seconds */

#define C_SUB_MS_SCALE_FACTOR 0.000000059604644775390625

#define  SV_DIRECTION_UNKNOWN  (-128)


static U8 costab[65] =
{
255,255,255,254,254,253,252,251,
250,249,247,246,244,242,240,238,
236,233,231,228,225,222,219,215,
212,208,205,201,197,193,189,185,
180,176,171,167,162,157,152,147,
142,136,131,126,120,115,109,103,
 98, 92, 86, 80, 74, 68, 62, 56,
 50, 44, 37, 31, 25, 19, 13,  6,
0
};


typedef struct
{
  S8  b_Elev;    /* Elevation. (-128 == Unknown). 180/256 degs/bit */
  U8  u_Azim;    /* Azimuth. 360/256 degs/bit. */
} pdop_SvDirection;

/*** local function prototype ****/
static FLT PdopApprox( pdop_SvDirection *p_AzEl, U32 q_N );
static U32 UnitVector( pdop_SvDirection *p_AzEl, S16 *p_Vector );
static S32 RowMult( S16 *p_RowIn, S16 *p_MatIn, U32 q_N );
static S32 Determinant( S32 *p_M );
static S32 Cos( U8 arg );


/*
 ******************************************************************************
 * function description:
 *
 *  gpsc_meas_qualchk()
 *
 *	1. Check for Good Measurement
 *     Criteria:
 *     (a) Status Bits are Sub-millisecond valid, Sub-bit time known, SV time known
 *     (b) Time Uncertainty thresh hold is
 *     (c) Latency of measurement < 5 sec (or on dwell length)
 *  2. Compute average C/No of good measurements
 *  3. Compute weighted DOP and position uncertainty of good measurements
 *  4. GPSC should maintain list of SVs with
 *     (a) steering injected from network & having elevation >mask angle
 *	   (b) Check if SV number is between 1 and 32
 *  5. Valid Fix Criteria:
 *     Conditions for accepting and sending measurement block to network
 *     (a) Number of good measurements >= 4, and
 *     (b) If averageC/No<30dB-Hz, check if every SV that has steering has a
 *         Observation Count >0 (rather than total count) or
 *		   if Good Observation Count>0 for atleast 8 SVs
 *     (c) If PosUnc < Accuracy
 *            where Accuracy = accuracy 1 if period elapsed <= timeout1,
 *            where Accuracy = accuracy 2 if period elapsed > timeout1
 *     then accept measurement block.
 *  6. When at T2=timeout-2, no conditions, send out measurement block.
 *          where T2 = period elapsed > timeout2.
 *
 * parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the
 *  pointers of many structures accessed from different modules and
 *  functions.
 *
 * return value:
 *
 *    TRUE if measurement is valid
 *
 ******************************************************************************
*/

#define PRN_OK(x)               ( ((x)>0) && ((x)<(N_SV+1)) )
#define GOOD_MEAS				MEAS_STATUS_MS_VALID

U8 gpsc_meas_qualchk(gpsc_ctrl_type *p_zGPSCControl, FLT *f_Pdop)
{
  U8 u_i,chan,u_Sv,u_CountObs;
  U8 b_IsAllSvSearched = 0;
  U8 u_IsValidMeas = FALSE;
  U8 u_NumSvsWithGoodMeas = 0;
  gpsc_state_type*             p_zGPSCState       = p_zGPSCControl->p_zGPSCState;
  gpsc_session_status_type*    p_zSessionStatus  = &p_zGPSCState->z_SessionStatus;
  gpsc_db_type*                p_zGPSCDatabase    = p_zGPSCControl->p_zGPSCDatabase;
  gpsc_sv_steer_inject_type* p_zSvInject = p_zGPSCControl->p_zSvInject;
  gpsc_sess_cfg_type*           p_zSessConfig           = p_zGPSCControl->p_zSessConfig;
  gpsc_dyna_feature_cfg_type *p_zGPSCDynaFeatureConfig = p_zGPSCControl->p_zGPSCDynaFeatureConfig;

  gpsc_db_gps_clk_type*        p_zDBGpsClock     = &p_zGPSCDatabase->z_DBGpsClock;
  gpsc_db_gps_meas_type*       p_zDBGpsMeas      = &p_zGPSCDatabase->z_DBGpsMeas;
  gpsc_meas_report_type*       p_zMeasReport     = &p_zDBGpsMeas->z_MeasReport;
  me_Time*                    p_zMeasToa        = &p_zMeasReport->z_MeasToa;
  gpsc_loc_fix_qop* p_zQop = &p_zGPSCControl->p_zSmlcAssist->z_Qop;
  gpsc_meas_per_sv_type*       p_zMeasPerSv;
  gpsc_steer_inject_per_sv_type* p_zPerSvSteerInject = NULL;

  gpsc_meas_per_sv_type*       p_zMeasPerSvDisplay;
  gpsc_cfg_type* p_zGPSCConfig = p_zGPSCControl->p_zGPSCConfig;
  gpsc_sys_handlers 		*p_zSysHandlers = p_zGPSCControl->p_zSysHandlers;
  TNAVCD_Ctrl				*pNavcCtrl = (TNAVCD_Ctrl *)p_zSysHandlers->hNavcCtrl;
  FLT f_SvTimeUncMsec, f_SvTimeUncMsecThreshold;
  U16 w_mantissa;
  U8  u_exponent;
  U32  q_AvgCNo =0;
  U16	w_Latency=0;
  U16 TimeInSession = 0;
  U8 IsGoodDop = FALSE;
  int w_IntTime;
  U16 timeout1,timeout2,accuracy,accuracy1,accuracy2;
  /* following for pos unc calculation */
  double d_PsuedoRangeError = 0.0;
  double d_AvgPsuedoRangeError = 0.0;
  U8 u_CountGoodObs = 0;
  p_zMeasReport->q_GoodStatSvIdBitMap = 0;

  /** make sure we've got sync. clock and measurement database */
  if ( p_zDBGpsMeas->z_MeasReport.q_FCount != p_zDBGpsClock->q_FCount )
  {
      STATUSMSG("gpsc_meas_qualchk : sync fail in clock and meas database FC= %lu",p_zDBGpsMeas->z_MeasReport.q_FCount);
	  return FALSE;
  }

  {
    STATUSMSG("Status: Checking meas. block with FC=%lu",
		        p_zDBGpsMeas->z_MeasReport.q_FCount);
  }

  /* Do not start checking until at least 1s passed from rcvr turn-on. ==>
	   TTFF will never be shorter than 1s. */
  if( p_zSessionStatus->w_NumUnRptMsrEvt < 1 )
  {
    STATUSMSG("gpsc_meas_qualchk : TTFF shorte than 1 sec FC= %lu",p_zDBGpsMeas->z_MeasReport.q_FCount);
	return FALSE;
  }
  else if(p_zSessionStatus->w_NumUnRptMsrEvt > 1)
  {
      TimeInSession = (p_zSessionStatus->w_NumUnRptMsrEvt - 1) * (p_zSessConfig->w_AvgReportPeriod);
  }
  else
  {
      TimeInSession = 1;
  }

 /*  Now assign clock information from the clock database to the meas.
      database, which could later be adjusted by time-alignment */
  p_zMeasToa->w_GpsWeek = p_zDBGpsClock->w_GpsWeek;
  p_zMeasToa->q_GpsMsec = p_zDBGpsClock->q_GpsMs;
#if GPSC_DEBUG
  STATUSMSG("Tow in Msec ==%ld",p_zMeasToa->q_GpsMsec);
#endif

  p_zMeasToa->f_ClkTimeBias = p_zDBGpsClock->f_TimeBias;
  p_zMeasToa->f_ClkTimeUncMs = p_zDBGpsClock->f_TimeUnc;

  /* check if good observation of Sv */
  p_zMeasPerSv = &p_zMeasReport->z_MeasPerSv[0];

  STATUSMSG("\nCurrent Meas statistics: \nSv, C, ST, \tSnr, CNo, \tLatency, Pre, Post, \tMsec, \tSubMsec, \tTUnc, Speed, SUnc, OC, GOC\n");
  for( u_i = 0; u_i < N_LCHAN; u_i++, p_zMeasPerSv++)
  {
    u_Sv = (U8)(p_zMeasPerSv->u_SvIdSvTimeFlag & 0x3F);
	p_zMeasPerSvDisplay =  &p_zMeasReport->z_MeasPerSv[u_i];

	w_mantissa =(U16) (p_zMeasPerSv->w_SvTimeUnc >> 5);
	u_exponent = (U8)(p_zMeasPerSv->w_SvTimeUnc & 0x001F);
//	f_SvTimeUncMsec = (FLT)( ext_ldexp( (DBL)w_mantissa, u_exponent) ) * 1.0e-6f;



	f_SvTimeUncMsec = mc_TimeUncertainty(p_zMeasPerSv->w_Snr, (U8)0);

	STATUSMSG("%02d, %02x, %04x, \t%02d, %02d, \t%06d, %2d, %4d, \t%ld, \t%ld, \t%06f, %04f, %02f, %02d, %02d @\n", \
                (p_zMeasPerSvDisplay->u_SvIdSvTimeFlag & 0x3F),p_zMeasPerSvDisplay->u_ChannelMeasState,p_zMeasPerSvDisplay->w_MeasStatus, \
				p_zMeasPerSvDisplay->w_Snr,p_zMeasPerSvDisplay->w_Cno,p_zMeasPerSvDisplay->x_LatencyMs,p_zMeasPerSvDisplay->u_PreInt, \
				p_zMeasPerSvDisplay->w_PostInt,p_zMeasPerSvDisplay->q_Msec,p_zMeasPerSvDisplay->q_SubMsec,f_SvTimeUncMsec,\
                (FLT)(p_zMeasPerSvDisplay->l_SvSpeed * (FLT)0.01), (FLT)(p_zMeasPerSvDisplay->q_SvSpeedUnc *0.1),p_zMeasPerSvDisplay->u_TotalObvCount,p_zMeasPerSvDisplay->u_GoodObvCount);

    /* Base latency on Integration time */
    w_IntTime = p_zMeasPerSv->u_PreInt * p_zMeasPerSv->w_PostInt;
    if(w_IntTime <= 4100)
      w_Latency = 5001;
	else if  (w_IntTime <= 17000)
	  w_Latency = 17001; /* 16s integration */
	else
	  w_Latency = 32001;/*int gr8 er than 16s*/

	/* check if it is good observation */
    if( PRN_OK(u_Sv) &&
        (p_zMeasPerSv->w_MeasStatus & GOOD_MEAS) &&
        (~p_zMeasPerSv->w_MeasStatus & MEAS_STATUS_FROM_DIFF)&&
	    (~p_zMeasPerSv->w_MeasStatus & MEAS_STATUS_DONT_USE) &&
	    (~p_zMeasPerSv->w_MeasStatus & MEAS_STATUS_XCORR)    &&
	    (p_zMeasPerSv->x_LatencyMs < w_Latency)
	  )
	  {
        /* summing of CNo */
		q_AvgCNo += p_zMeasPerSv->w_Cno;
		/* increment good observation count */
		u_NumSvsWithGoodMeas ++;
      }
    } /* end of good measurement loop */

    /* Compute average cno */
    if (u_NumSvsWithGoodMeas)
	{
	  q_AvgCNo = q_AvgCNo/u_NumSvsWithGoodMeas;
    }

	/* Calculate Sv Time Unc */
	if(q_AvgCNo > (U32)C_NO_THRESH_TIMEUNC)
	  f_SvTimeUncMsecThreshold = (FLT)(150.0/LIGHT_MSEC);
    else
	  f_SvTimeUncMsecThreshold = (FLT)(200.0/LIGHT_MSEC);

	 STATUSMSG("GPSC_MEAS_FILTER_LOG with latency=%d,q_AvgCNo=%ld,u_NumSvsWithGoodMeas=%d\n",w_Latency,q_AvgCNo,u_NumSvsWithGoodMeas);

	/* check only good SV's based on sv Time Unc */
	p_zMeasPerSv = &p_zMeasReport->z_MeasPerSv[0];
   	q_AvgCNo = 0;
	u_NumSvsWithGoodMeas =0;


	for (chan=0; chan < N_LCHAN; chan++, p_zMeasPerSv++)
 	{
      u_Sv = (U8)(p_zMeasPerSv->u_SvIdSvTimeFlag & 0x3F);
      /* Base latency on Integration time */
      w_IntTime = p_zMeasPerSv->u_PreInt * p_zMeasPerSv->w_PostInt;
      if(w_IntTime <= 4100)
        w_Latency = 5001;
	  else if  (w_IntTime <= 17000) /* 16s integration */
	    w_Latency = 17001;
	  else
	    w_Latency = 32001;/*int gr8 er than 16s*/

	  w_mantissa =(U16) (p_zMeasPerSv->w_SvTimeUnc >> 5);
	  u_exponent = (U8)(p_zMeasPerSv->w_SvTimeUnc & 0x001F);
//	  f_SvTimeUncMsec = (FLT)( ext_ldexp( (DBL)w_mantissa, u_exponent) ) * 1.0e-6f;

	  f_SvTimeUncMsec = mc_TimeUncertainty(p_zMeasPerSv->w_Snr, (U8)0);

	  /* check if it is good observation */
      if( PRN_OK(u_Sv) &&
         (p_zMeasPerSv->w_MeasStatus & GOOD_MEAS) &&
         (~p_zMeasPerSv->w_MeasStatus & MEAS_STATUS_FROM_DIFF)&&
	     (~p_zMeasPerSv->w_MeasStatus & MEAS_STATUS_DONT_USE) &&
	     (~p_zMeasPerSv->w_MeasStatus & MEAS_STATUS_XCORR)    &&
	     (f_SvTimeUncMsec < (FLT)(f_SvTimeUncMsecThreshold))
	   )
	   {
	     /* summing of CNo */
	     q_AvgCNo += p_zMeasPerSv->w_Cno;

		 /* calculate psuedo range error */
		 d_PsuedoRangeError = (double) (f_SvTimeUncMsec * LIGHT_MSEC); /* f_SvTimeUncMsec should convert to second*/
		 d_AvgPsuedoRangeError += d_PsuedoRangeError;

	     /* increment good observation count */
		  u_NumSvsWithGoodMeas ++;
		   p_zMeasReport->q_GoodStatSvIdBitMap |= ((U32)1<<(u_Sv - 1));
	   }
    } /* end of loop check only good SV's based on sv Time Unc */

	/* Compute average cno */
    if (u_NumSvsWithGoodMeas)
	{
	  q_AvgCNo = q_AvgCNo/u_NumSvsWithGoodMeas;
    }

	/* compute average psuedo range */
	if (u_NumSvsWithGoodMeas)
	{
	  d_AvgPsuedoRangeError = (double) d_AvgPsuedoRangeError/u_NumSvsWithGoodMeas;
    }


	STATUSMSG("GPSC_MEAS_FILTER_LOG with TimeUncThreshold=%f,q_AvgCNo=%ld,u_NumSvsWithGoodMeas=%d\n",f_SvTimeUncMsecThreshold,q_AvgCNo,u_NumSvsWithGoodMeas);

    /*this checks whether all sv with steering have been searched once*/
    u_CountObs=0;
	b_IsAllSvSearched = FALSE;

	p_zMeasPerSv = &p_zMeasReport->z_MeasPerSv[0];

    for (chan=0; chan < N_LCHAN; chan++, p_zMeasPerSv++)
	{
      u_Sv = (U8)(p_zMeasPerSv->u_SvIdSvTimeFlag & 0x3F);

	  /* Store all Injected Svs in database as zSvInjectedReoprt */
	  p_zPerSvSteerInject = &p_zSvInject->p_zPerSvSteerInject[0];

	  for(u_i=0; u_i<p_zSvInject->u_SvSteerInjectCount; u_i++, p_zPerSvSteerInject++)
	  {
		/*check whehther all SV that had assistance and above elevation mask has one observation count*/

	    /*the measurement observation count is got from p_Meas.z_Meas[u_Chan].u_Observe */
		if(u_Sv == p_zPerSvSteerInject->u_SvSteerId)
		{
			if(p_zMeasPerSv->u_TotalObvCount > 0)
			u_CountObs++;

			if(p_zMeasPerSv->u_GoodObvCount > 0)
			u_CountGoodObs++;
		}

	  } /*  for(i=0, i<p_zSvInjectedReoprt->NumSvInjected, i++, p_SvInjected++) */
	} /* endo of all sv with steering have been searched once */

	/* check if all sv's searched for which steering injected */
	if(u_CountObs >= p_zSvInject->u_SvSteerInjectCount)
	{
        b_IsAllSvSearched = TRUE;
	}

	STATUSMSG("GPSC_MEAS_FILTER_LOG u_NoOfSvStrInj=%d,u_CountObs=%d,u_CountGoodObs=%d, b_IsAllSvSearched=%d\n",p_zSvInject->u_SvSteerInjectCount,u_CountObs,u_CountGoodObs,b_IsAllSvSearched);

	/*we know the above condition is true or false, b_IsAllSvSearched = 1 or 0*/

	/*call function that finds uncertainty in the solution*/
	/*pos unc - take from hemanth_function()  */

	{
	/*********************************************************/
		/* PDOP Test */
		/*********************************************************/

		/* If test is valid so far and there are at least 4 good measurements,
		   do a final test: Positional Dilution of Precision (PDOP) test.
		   PDOP may allow reporting only 4 SVs provided they have good
		   geometry.  If not good geometry,  continue to wait for more
		   good measurements. */

		U8	u_Sv;
		U32 q_PdopN;
		me_SvDirection *p_D;
		pdop_SvDirection z_PdopD[N_LCHAN], *p_PdopD;

			q_PdopN = 0;
			p_PdopD = &z_PdopD[0];
			p_zMeasPerSv = &p_zMeasReport->z_MeasPerSv[0];


#ifdef AUDIT_SVDIR_AND_PDOP
      sm_LogMsg("** BEGIN: Select 'Good' SvDirs for PDOP **\n");
#endif
			for (u_i = N_LCHAN; u_i; u_i--, p_zMeasPerSv++)
			{
				u_Sv = (U8)(p_zMeasPerSv->u_SvIdSvTimeFlag & 0x3F);
				/* For SVs with good measurement only */
				if( (u_Sv) && (u_Sv <= 32) && ( p_zMeasReport->q_GoodStatSvIdBitMap & ((U32)1<<(u_Sv - 1))) )
				{
					/* Get Az/El for PDOP filter below */
					p_D = &p_zGPSCDatabase->z_DBSvDirection.z_RawSvDirection[u_Sv - 1];
					if( p_D->b_Elev != -128 )
					{
						p_PdopD->b_Elev = p_D->b_Elev;
						p_PdopD->u_Azim = p_D->u_Azim;
#ifdef AUDIT_SVDIR_AND_PDOP
            sm_LogMsgf("SV%02d EL%03d AZ%03d\n", u_Sv,
                            (S16) ((90.0/128.0)*(FLT)p_PdopD->b_Elev),
                            (U16) ((360.0/256.0)*(FLT)p_PdopD->u_Azim) );
#endif
						p_PdopD++;
						q_PdopN++;
					}
				}
			}

#ifdef AUDIT_SVDIR_AND_PDOP
      sm_LogMsgf("** END: Select %d SvDirs for PDOP **\n", q_PdopN);
      sm_DiagMsgf("Selected %d SvDirs for PDOP\n", q_PdopN);
#endif

			/* Earlier tests tell that at least 4, but not all, SVs have
			   good measurements. Check PDOP for the SVs that seem good.
			   Favorable PDOP means that the 4 or more good SVs are likely
			   to form a valid fix.  If unfavorable PDOP,
			   block the fix in hope that finding another SV will improve PDOP.
			   Negative PDOP can mean singularity or AzEl not available,
			   usually the latter, so don't block fix on that account. */
      /* DEF1988:
         Perform PDOP Filter test until
         there are 5 seconds left before TIMEOUT.
         Note that the Filter will not be used if the TIMEOUT
         setting is 10 seconds, or less */

		  *f_Pdop = PdopApprox( z_PdopD, q_PdopN );

		if (*f_Pdop > 0)
		{
			/* check pdop and pos unc */
			p_zMeasReport->d_PosUnc = (double) (3* d_AvgPsuedoRangeError* (*f_Pdop));
		}
		else
		{
		            p_zMeasReport->d_PosUnc = 3.0 * 25.0 * d_AvgPsuedoRangeError;
		}

		if (p_zMeasReport->d_PosUnc == 0)
		{ 
		            p_zMeasReport->d_PosUnc = 65535; 
		}


#if defined(AUDIT_SVDIR_AND_PDOP)
      sm_LogMsgf("PDOP Approx. = %f\n", (float) *f_Pdop);
      sm_DiagMsgf("PDOP Approx. = %f\n", (float) *f_Pdop);
#endif

      STATUSMSG("PDOP Approx. = %f\n", (float) *f_Pdop);
	} /* end PDOP test */

	/* initialize timeout and accuracy */
 
	if(p_zQop->u_HorizontalAccuracy !=  C_ACCURACY_UNKNOWN )
	{
		accuracy1 = (U16)(p_zQop->u_HorizontalAccuracy);
		accuracy2 = (U16)((p_zQop->u_HorizontalAccuracy *2) - 10) ;
		if( accuracy2 <= 10)
		{
	      accuracy2 = (U16)(p_zQop->u_HorizontalAccuracy *2);
		}
	}
	else
	{
	   accuracy1 = (U16)p_zGPSCConfig->accuracy1;
	   accuracy2 = (U16)p_zGPSCConfig->accuracy2;
	   	  
	   if(pNavcCtrl->bAssistDataFromPgpsSagps == TRUE)  /* for Sagps and PGPS Session*/
	   {
		   accuracy1 = (U16)p_zGPSCConfig->pgps_sagps_accuracy1;
	       accuracy2 = (U16)p_zGPSCConfig->pgps_sagps_accuracy2;
		    
	   }
	}

	
	if(p_zQop->w_MaxResponseTime != C_TIMEOUT_UNKNOWN )
	{
	   timeout1 = (U16)(p_zQop->w_MaxResponseTime /2); /* resolution 1 sec/bit */
	   timeout2 = (U16)(p_zQop->w_MaxResponseTime ); /* resolution 1 sec/bit */
       
	}
	else
	{

	   timeout1 = (U16)(p_zGPSCConfig->timeout1); /* resolution 1 sec/bit */
	   timeout2 = (U16)(p_zGPSCConfig->timeout2); /* resolution 1 sec/bit */

	   if(pNavcCtrl->bAssistDataFromPgpsSagps == TRUE)  /* for Sagps and PGPS Session*/
	   {
		   timeout1 = (U16)(p_zGPSCConfig->pgps_sagps_timeout1); /* resolution 1 sec/bit */
	       timeout2 = (U16)(p_zGPSCConfig->pgps_sagps_timeout2); /* resolution 1 sec/bit */
		  
	   }

	}

	/* at timeout, send measurement report with whatever status */
	if((abs((p_zGPSCState->u_FcountDelta + timeout2)- TimeInSession)) < (p_zSessConfig->w_AvgReportPeriod))
	{
		u_IsValidMeas = TRUE;
		STATUSMSG("GPSC_MEAS_FILTER: timeout case = %d sec, u_IsValidMeas=%d\n", TimeInSession, u_IsValidMeas);
		/*The below function call is to check the good SVs without SV time
		  in case of time out so that allow the meas report to sent to n/w */
		gpsc_meas_qualchk_sft_timeout(p_zGPSCControl);
		return u_IsValidMeas;
	}
	else if (TimeInSession == ((p_zGPSCState->u_FcountDelta + timeout2)-2))
	{
            /* Check if we are in APM mode */
            if((p_zGPSCDynaFeatureConfig->feature_flag & GPSC_FEAT_APM) && (p_zGPSCDynaFeatureConfig->apm_control == 1))
            {
	        STATUSMSG("T2-2sec, Not increasing the PDOP mask in the sensor, while in APM mode.");
            }
            else
            {
                /* at T2 - 2 seconds increase the PDOP mask in the receiver */
	        STATUSMSG("T2-2sec, Increasing the PDOP mask in the sensor");
	        gpsc_mgp_maximize_pdop(p_zGPSCControl);
            }
	}
	
	accuracy = accuracy1;
	if(TimeInSession > timeout1) /* time in session > timeout1 */
	{
		accuracy = accuracy2;
	}

	/* for pdop calculation, either take sqrt of f_Pdop or take
	   power^2 of p_zGPSCConfig->pdop_mask */
	if((*f_Pdop <= p_zGPSCConfig->pdop_mask) && (*f_Pdop > 0.00))
	{
		IsGoodDop = TRUE;
	}

	if(q_AvgCNo < 290)
	{
		if( (b_IsAllSvSearched) || (u_CountGoodObs >= 8))
		{
		    u_IsValidMeas = TRUE;
		}
		else
		{
		    return u_IsValidMeas;
		}
	}

	/* pos unc and pdop check */
	if((u_NumSvsWithGoodMeas >=4) && (p_zMeasReport->d_PosUnc < accuracy))
	{
		u_IsValidMeas = TRUE;
	}
	else
	{
        u_IsValidMeas = FALSE;
	}
	STATUSMSG("GPSC_MEAS_FILTER: PosUnc=%lf, Accuracy=%d,TimeInSession=%d, u_IsValidMeas=%d\n",p_zMeasReport->d_PosUnc,accuracy, TimeInSession, u_IsValidMeas);
/* Start: Change by Manav
 * Date: 03/11/2010
 * Purpose: to work around a premature release of measurements to the server
 * in MSA cases
 */
        {
                U8 u_numOfGoodSVs = 0, u_numOfSVsWithTimeSet = 0;	
                p_zMeasPerSv = &p_zMeasReport->z_MeasPerSv[0];
	        u_numOfGoodSVs = 0;
	        u_numOfSVsWithTimeSet = 0;
	        for (chan=0; chan < N_LCHAN; chan++, p_zMeasPerSv++)
	        {
		        /* Check for OLT and DLL */
		        if ((p_zMeasPerSv->u_ChannelMeasState == 3) || (p_zMeasPerSv->u_ChannelMeasState == 4))
			        u_numOfGoodSVs ++;
		        /* Check for Measurments with time set */
		        if ((p_zMeasPerSv->w_MeasStatus & GOOD_MEAS))
			        u_numOfSVsWithTimeSet++;

	        }

	        /* if the number of measurements with time set are less than the number of SVs in track.
	         * There is no need to push these to Server */

	        if (u_numOfSVsWithTimeSet < u_numOfGoodSVs)
	        {
			        /* we need to hold the measurements */
			        u_IsValidMeas = FALSE;
	        }
        }

/* End: Change by Manav
 * Date: 03/11/2010
 * Purpose: to work around a premature release of measurements to the server
 * in MSA cases
 */

	
	return u_IsValidMeas;
}

/******************************************************************************
 * function description:
 *
 *  gpsc_meas_qualchk_sft_timeout()
 *
 *
 * parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the
 *  pointers of many structures accessed from different modules and
 *  functions.
 *
 * return value:
 *
 *    void
 *
 ******************************************************************************
*/

void gpsc_meas_qualchk_sft_timeout(gpsc_ctrl_type *p_zGPSCControl)
{
  U8 u_i,u_Sv,u_NumSvsWithGoodData = 0;
  gpsc_db_type*                p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
  gpsc_db_gps_meas_type*       p_zDBGpsMeas = &p_zGPSCDatabase->z_DBGpsMeas;
  gpsc_meas_report_type*       p_zMeasReport     = &p_zDBGpsMeas->z_MeasReport;
  gpsc_meas_per_sv_type*       p_zMeasPerSv;

  STATUSMSG("Status: SFT - Sv Stat bit check during timeout");
  p_zMeasPerSv = &p_zMeasReport->z_MeasPerSv[0];

  for( u_i = 0; u_i < N_LCHAN; u_i++, p_zMeasPerSv++ )
  {
	u_Sv = (U8)(p_zMeasPerSv->u_SvIdSvTimeFlag & 0x3F);
	 if((p_zMeasPerSv->w_MeasStatus & MEAS_STATUS_SM_VALID) &&
		 !(p_zMeasPerSv->w_MeasStatus & MEAS_STATUS_FROM_DIFF) &&
		 !(p_zMeasPerSv->w_MeasStatus & MEAS_STATUS_DONT_USE) &&
		 !(p_zMeasPerSv->w_MeasStatus & MEAS_STATUS_XCORR)
	   )
	 {
        u_NumSvsWithGoodData++;
    	p_zMeasReport->q_GoodStatSvIdBitMap |= ((U32)1<<(u_Sv - 1));
//        STATUSMSG(" Good Svs without check the SVtime = %d",p_zMeasStatPerSv->u_Sv);
     }
  }
  STATUSMSG("Session Information: SFT q_GoodStatSvIdBitMap=0x%x",p_zMeasReport->q_GoodStatSvIdBitMap);
}

#if MEAS_STATEBITS_CKHECK_FN
/*
 ******************************************************************************
 * function description:
 *  ai_AI2MeasStatusCheck() Checks the measurement block status field.
 *            Valid status: - subms (f_SatSubMs) valid (.._SM_VALID set)
 *                  - sub-bit time known (.._SB_VALID set)
 *                  - satellite time is known (.._MS_VALID set)
 *                  - last update from measurement (..FROM_DIFF not set)
 *                  - not a 'dubious' measurement (..MS_DONT_USE not set)
 *
 * parameters:
 *    p_Meas - pointer to an ME measurement
 *
 * return value:
 *    TRUE if measurement status is valid
 *
 ******************************************************************************
*/

static U8 meas_statbits_check( gpsc_meas_stat_per_sv_type *p_zMeasStatPerSv )
{
  if ( ((p_zMeasStatPerSv->w_MeasStatus  &
       (MEAS_STATUS_SM_VALID | MEAS_STATUS_SB_VALID | MEAS_STATUS_MS_VALID |
      MEAS_STATUS_FROM_DIFF | MEAS_STATUS_DONT_USE | MEAS_STATUS_XCORR)) ==
       (MEAS_STATUS_SM_VALID | MEAS_STATUS_SB_VALID | MEAS_STATUS_MS_VALID)))
    return TRUE;
  else
    return FALSE;
}

#endif /* #if MEAS_STATEBITS_CKHECK_FN */

/*
 ******************************************************************************
 * Cos
 *
 * Function description:
 *
 *	Approximate cosine from table with 8-bit resolution
 *
 * Parameters:
 *
 *	arg	- degrees * 256/360, range 0 - 255
 *
 * Return value:
 *
 *	cosine approximation * 255, e.g., cos(0) = 0xFF.  This is approximate b8 scaling.
 *
 ******************************************************************************
*/

S32 Cos( U8 arg )
{
	if( arg <= 64 )
		return( costab[arg] );
	else if( arg <= 128 )
		return( (S32)0 - costab[128 - arg] );
	else if( arg <= 192 )
		return( (S32)0 - costab[arg - 128] );
	else
		return( costab[256 - arg] );
}

/*
 ******************************************************************************
 * pdop_UnitVectorPdop
 *
 * Function description:
 *
 *	UnitVector is used to compute the unit vector scaled b15 for a given Sv using
 *	the Azimuth and Elevation estimates that are provided.
 *
 *	If no direction information is known (indicated by b_Elev == SV_DIRECTION_UNKNOWN),
 *	then no conversion occurs and a FALSE return code is generated.
 *
 * Parameters:
 *
 *	p_AzEl - point to an AzEl pair
 *	p_Vector - Pointer to the 3 element x, y, z vector (output)
 *
 * Return value:
 *
 *	TRUE if a valid UnitVector is written to *p_Vector.
 *
 ******************************************************************************
*/
static U32 UnitVector( pdop_SvDirection *p_AzEl, S16 *p_Vector )
{
U8  u_Azim;
S8	b_Elev;
S32 l_CosElev;

	/* Can't compute the direction vector if the direction is unknown */
	if( p_AzEl->b_Elev == SV_DIRECTION_UNKNOWN )
		return( FALSE );

	u_Azim = p_AzEl->u_Azim;
	b_Elev = (S8)((p_AzEl->b_Elev + 1) >> 1);


	/* Look up the cos of the elevation. Note that recasting
	   the (S8) b_Elev to (U8) handles the task of dealing
	   with negative elevations. */

	l_CosElev = Cos( (U8)b_Elev );

	/* x = cos( Elev ) * sin( Azim ) */
	*p_Vector++	= (S16)(l_CosElev * Cos( (U8)(u_Azim - 64) ) >> 1);

	/* y = cos( Elev ) * cos( Azim ) */
	*p_Vector++	= (S16)(l_CosElev * Cos( u_Azim ) >> 1);

	/* z = sin( Elev ) */
	*p_Vector = (S16)(Cos( (U8)(b_Elev - 64) ) << 7);

	/* Indicate a successful translation */
	return( TRUE );
}


/*
 ******************************************************************************
 * RowMult - RowMult_ind()
 *
 * Function description:
 *
 * 	RowMult multiplies a single row of the input matrix against a column
 *	of the transposed input matrix.  We actually form only the transpose.
 *	A row of the virtual matrix is effectively accessed by a stride of 3
 *	in the transpose.
 *
 *	(x0, x1 .... xn) * ( x0   y0   z0 )
 *  (y0, y1 .... yn)   ( x1   y1   z1 )
 *	(z0, z1 .... zn)   ( ..   ..   .. )
 *                     ( xn   yn   zn )
 *
 *	On input, each element has a maximum magnitude of 1 (= 32767).
 *
 *	Scale down by 3 leaving enough headroom for summing 16 maximum product
 *	values then scale down by 16 so that later processes can make products in
 *	a 32-bit register.
 *
 * Parameters:
 *
 *	p_RowIn - Points to the column that corresponds to a row of the
 *				virtual input matrix
 *	p_MatIn - Points to a column of the transposed input matrix
 *	q_N - Number of elements in each row/column
 *
 * Return value:
 *
 *	Sum of products
 *
 ******************************************************************************
*/

static S32 RowMult( S16 *p_RowIn, S16 *p_MatIn, U32 q_N )
{
S32 l_Sum = 0;

	for( ; q_N; q_N-- )
	{
		l_Sum += (S32) *p_RowIn * *p_MatIn >> 3;
		p_RowIn += 3;
		p_MatIn += 3;
	}

	return( l_Sum >> 16 );
}


/*
 ******************************************************************************
 * Determinant_ind
 *
 * Function description:
 *
 * 	Returns the determinant of a 3X3 matrix
 *
 *
 * Parameters:
 *
 *	p_Mat - Points to the input matrix.  Values are preconditioned to have
 *			at most 15 significant bits.
 *
 * Return value:
 *
 *	Determinant
 *
 ******************************************************************************
*/
static S32 Determinant ( S32 *p_M )
{
#if 1
S32 l_Sum;

l_Sum = 0;

l_Sum = p_M[0]*p_M[5]*p_M[10]*p_M[15] - p_M[0]*p_M[5]*p_M[11]*p_M[14] - p_M[0]*p_M[9]*p_M[6]*p_M[15] + p_M[0]*p_M[9]*p_M[7]*p_M[14] + p_M[0]*p_M[13]*p_M[6]*p_M[11] - p_M[0]*p_M[13]*p_M[7]*p_M[10] - p_M[4]*p_M[1]*p_M[10]*p_M[15]
      + p_M[4]*p_M[1]*p_M[11]*p_M[14] + p_M[4]*p_M[9]*p_M[2]*p_M[15] - p_M[4]*p_M[9]*p_M[3]*p_M[14] - p_M[4]*p_M[13]*p_M[2]*p_M[11] + p_M[4]*p_M[13]*p_M[3]*p_M[10] + p_M[8]*p_M[1]*p_M[6]*p_M[15] - p_M[8]*p_M[1]*p_M[7]*p_M[14]
      - p_M[8]*p_M[5]*p_M[2]*p_M[15] + p_M[8]*p_M[5]*p_M[3]*p_M[14] + p_M[8]*p_M[13]*p_M[2]*p_M[7] - p_M[8]*p_M[13]*p_M[3]*p_M[6] - p_M[12]*p_M[1]*p_M[6]*p_M[11] + p_M[12]*p_M[1]*p_M[7]*p_M[10] + p_M[12]*p_M[5]*p_M[2]*p_M[11]
      - p_M[12]*p_M[5]*p_M[3]*p_M[10] - p_M[12]*p_M[9]*p_M[2]*p_M[7] + p_M[12]*p_M[9]*p_M[3]*p_M[6];

l_Sum = l_Sum >> 11;

	return( l_Sum );
#else

S32 l_Prod, l_Sum;
U32 i;
S32 *p_End;

	p_End = &p_M[16];
	l_Sum = 0;

	/* Sum the products of the forward diagonals.  Form products
		with double precision. Each element has at most 15 significant
		bits.  Triple product would have at most 45 bits, but shifted
		right 16, so 29 bits.  This leaves headroom to sum 4 products
		in the worst case.  In any practical case elements will have
		less than 15 bits because RowMult makes space to sum up to
		16 worst case SVs. Worst case would occur when all angles are
		near some critical multiple of PI/2 */

	for( i = 4; i; i-- )
	{
		l_Prod = *p_M;
		if( (p_M += 5) >= p_End )
			p_M -= 16;
		l_Prod *= *p_M;
		if( (p_M += 5) >= p_End )
			p_M -= 16;
		l_Sum += (l_Prod >> 16) * *p_M +
			((S32)((U32)(l_Prod << 16) >> 16) * *p_M >> 16);
		if( (p_M += 5) >= p_End )
			p_M -= 16;
		l_Sum += (l_Prod >> 16) * *p_M +
			((S32)((U32)(l_Prod << 16) >> 16) * *p_M >> 16);
		if( (p_M += 5) >= p_End )
			p_M -= 16;
	}

	/* Subtract the reverse diagonal products */

	for( i = 4; i; i-- )
	{
		if( (p_M += 3) >= p_End )
			p_M -= 16;
		l_Prod = *p_M;
		if( (p_M += 3) >= p_End )
			p_M -= 16;
		l_Prod *= *p_M;
		if( (p_M += 3) >= p_End )
			p_M -= 16;
		l_Sum -= (l_Prod >> 16) * *p_M +
			((S32)((U32)(l_Prod << 16) >> 16) * *p_M >> 16);
		if( (p_M += 3) >= p_End )
			p_M -= 16;
		l_Sum -= (l_Prod >> 16) * *p_M +
			((S32)((U32)(l_Prod << 16) >> 16) * *p_M >> 16);
		if( (p_M += 3) >= p_End )
			p_M -= 16;

	}
	return( l_Sum );
#endif
}



/*
 ******************************************************************************
 * PdopApprox - FLT PdopApprox_ind
 *
 * Function description:
 *
 * 	PdopApprox is a helper function for the application.  It uses an
 *	optimized integer approximation and the Sv Direction
 *	information to calculate the PDOP for a given set of satellites. The caller
 *	provides the direction angles for selected satellites.
 *
 * Parameters:
 *
 *	p_AzEl - point to array of SV directions
 *	q_N - number of SVs represented in the array.  Minimum 4.
 *
 * Return value:
 *
 *	PDOP. (Negative result indicates that PDOP is incalculable).
 *
 ******************************************************************************
*/
static FLT PdopApprox( pdop_SvDirection *p_AzEl, U32 q_N )
{
U32 q_i;
S32 l_Det;
FLT f_DetInv;
FLT f_Result;
S16 *p_UnitVector;		/* x,y,z of SV */
S16 x_XYZ[3*N_CHAN];	/* x,y,z of all SVs */
S32 l_Mat[16],C11,C22,C33;			/* 3 by 3 intermediate matrix */
//me_SvDirection *p_D;
pdop_SvDirection *p_D;
S16 x_Ones[3*N_CHAN];

	/* Construct the pointing vectors. If any of the SVs does
	   not have a valid Elevation / Azimuth then we can't compute
	   a PDOP.  Unit vectors are scaled b15. */

	/* The maximum number of SVs permitted for this calculation is N_LCHAN. (Space limited).
	   The minimum number of SVs permitted is 3. (To solve a 3x3 matrix).
	   But limit to 4 for practicality. */

	if( q_N < 4 )
		return( -1.0f );

	/* Use an arbitrary subset if too many SVs */
	q_N = q_N <= N_CHAN ? q_N : N_CHAN;

	p_UnitVector = &x_XYZ[0];
	for( q_i = q_N, p_D = p_AzEl; q_i; q_i--, p_D++ )
	{
		if( ! UnitVector( p_D, p_UnitVector ) )
			return( -1.0f );		/* Missing Az/El */

		p_UnitVector += 3;
	}


	for(q_i=0;q_i<3*N_CHAN;q_i++)
	{
		x_Ones[q_i] = (S16) (1<<8);
	}

	/* Multiply input matrix by transpose.  Actually, the matrix we have formed
		is the transpose. The input matrix is virtual.  Do product of two b15
		values then scale 19 to fit sum into S16.  Thus values are scaled b11. */

	l_Mat[0] = 		RowMult( &x_XYZ[0], &x_XYZ[0], q_N );	/* x x */
	l_Mat[1] = l_Mat[4] =	RowMult( &x_XYZ[0], &x_XYZ[1], q_N );	/* x y */
	l_Mat[2] = l_Mat[8] =	RowMult( &x_XYZ[0], &x_XYZ[2], q_N );	/* x z */

	l_Mat[5] = 		RowMult( &x_XYZ[1], &x_XYZ[1], q_N );	/* y y */
	l_Mat[6] = l_Mat[9] =	RowMult( &x_XYZ[1], &x_XYZ[2], q_N );	/* y z */
	l_Mat[10] = 		RowMult( &x_XYZ[2], &x_XYZ[2], q_N );	/* z z */


	l_Mat[3] = l_Mat[12] = RowMult( &x_XYZ[0], &x_Ones[0], q_N );	/* sum(x) */
	l_Mat[7] = l_Mat[13] = RowMult( &x_XYZ[1], &x_Ones[0], q_N );	/* sum(y) */
	l_Mat[11] = l_Mat[14]= RowMult( &x_XYZ[2], &x_Ones[0], q_N );	/* sum(z) */
	l_Mat[15] = (S16)(q_N)*(S16)(1<<8);

	l_Det = 0;

	/* Scale down l_Mat by b5 to ensure that the determinant and the co-factors dont overflow */

	for(q_i=0;q_i<16;q_i++)
		{
			l_Mat[q_i] = (S32)(l_Mat[q_i] >> 5);
		}

	/* The typical task is to invert l_Mat[]. We are only interested in the
	   diagonal elements of the inverted matrix EX*EX, EY*EY and EZ*EZ. */

	l_Det = 0;

	if( (l_Det = Determinant( l_Mat )) == 0 )
		return ( -2.0f );		/* Singular */

	/* Account for determinant made of four-factor products but cofactor uses
		three-factor products.  l_Mat is scaled b6.  Determinant result is b28 (4mul + 16additions)
		but scaled down by 11 to b17.  Cofactor product is b21 (3mul(b18) + 9 additions(b3)).  Hence scaling
		differs by four bits. */

	f_DetInv = (FLT)(1./(1<<4)) / (FLT)l_Det;

	/* Term of inverse = cofactor of term / determinant.  We need only
		the main diagonal terms. */

	C11 =  l_Mat[5] * (l_Mat[10]*l_Mat[15] - l_Mat[11]*l_Mat[14]) - l_Mat[9] * (l_Mat[6]*l_Mat[15] + l_Mat[7]*l_Mat[14])
		+ l_Mat[13]*(l_Mat[6]*l_Mat[11] - l_Mat[7]*l_Mat[10]);

	C22 = l_Mat[0]*(l_Mat[10]*l_Mat[15] - l_Mat[11]*l_Mat[14]) - l_Mat[8]*(l_Mat[2]*l_Mat[15] + l_Mat[3]*l_Mat[14])
		+ l_Mat[12]*(l_Mat[2]*l_Mat[11] - l_Mat[3]*l_Mat[10]);

	C33 = l_Mat[0]*(l_Mat[5]*l_Mat[15] - l_Mat[7]*l_Mat[13]) - l_Mat[4]*(l_Mat[1]*l_Mat[15] + l_Mat[3]*l_Mat[13])
		+ l_Mat[12]*(l_Mat[1]*l_Mat[7] - l_Mat[3]*l_Mat[5]);


	f_Result = (FLT)(C11 + C22 + C33) * f_DetInv;

	return f_Result;
}



/*
 * The following table is based on Norm's theoretical predictions
 * using the approximation sigma_error = 0.42 * SNR^(-0.41) chips
*/
const mc_TimeUnc mcz_TimeUnc[C_TIMEUNC_TABLE_SIZE]

=
{
 {
  (U16)150,   /* >= 15 dB */
  (FLT)(0.1019/CA_CHIPS_MSEC),
  (FLT)( (0.0635-0.1019)/(50*CA_CHIPS_MSEC) )
 },
 {
  (U16)200,   /* >= 20 dB */
  (FLT)(0.0635/CA_CHIPS_MSEC),
  (FLT)( (0.0396-0.0635)/(50*CA_CHIPS_MSEC) )
 },
 {
  (U16)250,   /* >= 25 dB */
  (FLT)(0.0396/CA_CHIPS_MSEC),
  (FLT)( (0.0247-0.0396)/(50*CA_CHIPS_MSEC) )
 },
 {
  (U16)300,   /* >= 30 dB */
  (FLT)(0.0247/CA_CHIPS_MSEC),
  (FLT)( (0.0154-0.0247)/(50*CA_CHIPS_MSEC) )
 },
 {
  (U16)350,   /* >= 35 dB */
  (FLT)(0.0154/CA_CHIPS_MSEC),
  (FLT)( (0.0096-0.0154)/(50*CA_CHIPS_MSEC) )
 },
 {
  (U16)400,   /* >= 40 dB */
  (FLT)(0.0096/CA_CHIPS_MSEC),
  (FLT)0.0
 }
}

;


/*
 ******************************************************************************
 * mc_TimeUncertainty
 *
 * Function description:
 *
 *  This function calculates the TimeUncertainty of a given measurement based
 *  on the background SNR of the measurement (from a lookup table)
 *
 * Parameters:
 *
 *  w_SnrDB   - Background SNR ( * 10 dB )
 *  u_FilterN - Code Phase Filter coefficient
 *
 * Return value:
 *
 *  FLT   f_SvTimeUncMs
 *
 ******************************************************************************
*/

FLT mc_TimeUncertainty ( U16 w_SnrDB, U8 u_FilterN )
{
const mc_TimeUnc *p_TimeUnc = &mcz_TimeUnc[0];
FLT f_SvTimeUncMs;
U32 i;

 /*
  * p_TimeUnc is the table of time uncertainties at a few discrete SNRs
  * For all the SNRs inbetween we perform a simple two-point interpolation
  * To avoid any divisions we also stored the slope of the uncertainty curve
  * at each discrete SNR.
  *   Interpolated value Y = Slope * (X - X1) + Y1;
  *   where   X  = Measured SNR
  *       X1 = SNR from table which is just below the Measured SNR
  *       Y1 = Time Uncertainty from table correponding to SNR X1.
 */


 /* Find the table entry corresponding to an SNR just below the measurement SNR */
 for( i = 0; i < C_TIMEUNC_TABLE_SIZE; i++ )
 {
  if( p_TimeUnc[i].w_SnrDB > w_SnrDB )
    break;
 }
 if( i )
  i--;

 /* Y = Slope * ( X - X1 ) + Y1 */
 f_SvTimeUncMs = p_TimeUnc[i].f_Slope * (FLT)((S32)(w_SnrDB - p_TimeUnc[i].w_SnrDB)) +
        p_TimeUnc[i].f_SvTimeUncMs;

 /*
  * Code Phase Filter used is based on p_Meas->u_FilterN (N)
  *   Filter Equation is (for a filter co-efficient of N)
  *      Yn = Yn-1 + (Xn-Yn-1)/(N+1) ;
  *   i.e. Yn = Xn/(N+1) + N*Yn-1/(N+1);
  *   i.e. Yn = (1-a)Xn + aYn-1;    where a = N/(N+1)
  *           n
  *           ---
  *   i.e.   Yn = \   (1- a^k) Xk
  *           /
  *           ---
  *           k=0
  *
  *   Relating this to a standard low-pass filter we have
  *       a^n = exp(-t/tou)
  *       n * ln(a) = - n*Ts/tou;
  *
  *   So the time constant of the filter is obtained by
  *       tou = -Ts/ln(a);
  *
  *   Equivalent bandwidth of this Filter is
  *       Beq = 1/(4*tou)
  *         = -ln(a)/(4*Ts)
  *
  *   Since our measurements are 1sec apart Ts=1sec (or 1Hz Bandwidth)
  *       Beq = -ln(a)/4;
  *
  *   Now Sigma of the Filtered Error estimate is Sigma(filter) = Sigma(no_filter) * Sqrt(Beq)
  *
  *   mcf_SqrtBeqLookUp contains the sqrt(Beq) for values of N ranging from 0 to 10;
 */
 /* Restrict the u_FilterN to a max of 10 */
 if( u_FilterN > 10 )
  u_FilterN = 10;

 f_SvTimeUncMs *= mcf_SqrtBeqLookUp[u_FilterN];

 return ( f_SvTimeUncMs );
}

