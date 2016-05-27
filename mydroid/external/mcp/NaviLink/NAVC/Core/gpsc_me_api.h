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
#ifndef GPSC_ME_API_H
#define GPSC_ME_API_H

#define  C_GPS_WEEK_UNKNOWN  0xFFFF
#define C_RTC_TIME_DRIFT_RATE_PER_DAY_IN_S (10.0/(24.0 *60.0 * 60.0)) /* 10s/day */
#define C_MIN_TIME_UNC_THRESHOLD           (60.0 * 1000.0 * 1000.0)    /* 60s */

   /*The C_HEALTH_NOEXIST field really only applies to the Almanac.
   The Almanac is assumed to be complete when every SV has an almanac
   health entry that is not C_HEALTH_UNKNOWN */

#define  C_HEALTH_UNKNOWN  (0<<0)  /* No health information */
#define  C_HEALTH_BAD    (1<<0)  /* Satellite is sick */
#define  C_HEALTH_GOOD    (2<<0)  /* Satellite is known good */
#define  C_HEALTH_NOEXIST  (3<<0)  /* Satellite does not exist */

/* The MEAS_STATUS defines identify the bits store in the w_MeasStatus field
   of the me_Meas data structure. */


#define  MEAS_STATUS_NULL       0       /* The don't know anything state */
#define  MEAS_STATUS_SM_VALID   (1<<0)  /* TRUE -> f_SatSubMs is valid */
#define  MEAS_STATUS_SB_VALID   (1<<1)  /* TRUE -> sub bit time known */
#define  MEAS_STATUS_MS_VALID   (1<<2)  /* TRUE -> satellite time is known */
#define  MEAS_STATUS_BE_CONFIRM (1<<3)  /* TRUE -> bit edge confirmed from
                                                   signal */
#define  MEAS_STATUS_VE_VALID   (1<<4)  /* TRUE -> measured velocity */
#define  MEAS_STATUS_VE_FINE    (1<<5)  /* TRUE/FALSE -> fine/coarse velocity */
#define  MEAS_STATUS_CONT_LOCK  (1<<6)  /* TRUE -> Channel has continous lock */
#define  MEAS_STATUS_HEALTHY    (1<<7)  /* TRUE -> Satellite is healthy */
#define  MEAS_STATUS_GOOD_DATA  (1<<8)  /* TRUE -> Good satellite data parity */
#define  MEAS_STATUS_FROM_DIFF  (1<<9)  /* TRUE -> Last update from difference,
                                                   not meas */
#define  MEAS_STATUS_UNCERTAIN  (1<<10) /* TRUE -> Weak SV but can't do definite
                                                   XCORR test */
#define  MEAS_STATUS_XCORR      (1<<11) /* TRUE -> Strong indication of cross
                                                   correlation */
#define  MEAS_STATUS_MULTIPATH  (1<<12) /* TRUE -> Had multipath indicators */
#define  MEAS_STATUS_DONT_USE   (1<<13) /* TRUE -> Dubious measurement, wait
                                                    for another dwell */
                                                  

typedef struct
{
  U16  w_GpsWeek;      /* GPS week number at reference tick [weeks] */
  U32  q_GpsMsec;      /* GPS msec at reference tick [msecs] */
  FLT f_ClkTimeBias;    /* Clock bias [msecs] */
  FLT  f_ClkTimeUncMs;    /* 1 sided max time bias uncertainty [msecs] */
  FLT  f_ClkFreqBias;
  FLT  f_ClkFreqUnc; /* added for NL5500 */
} me_Time;

/* The me_SvTime structure is a fundamental component of measurement time and
   steering information. It is defined in units of msecs to facilitate easy
   independent manipulation of the epoch and sub epoch components of time. */


typedef struct
{
  FLT  f_SubMs;    /* Range of 0 thru 0.99999 [msecs] */
  U32  q_Ms;      /* Range of 0 thru (WEEK_MSECS-1) [msecs] */
} me_SvTime;

/* me_SvDirection contains the pointing information for a particular
   satellite. This is useful for DOP calculations and for satellite
   selection. Note that a me_Sv_Direction is deemed invalid if
   the b_Elev element is set to C_SV_DIRECTION_UNKNOWN */

typedef struct
{
  S8  b_Elev;    /* Elevation. (-128 == Unknown). 180/256 degs/bit */
  U8  u_Azim;    /* Azimuth. 360/256 degs/bit. */
} me_SvDirection;


/*
 * me_SvSteering conveys line of sight ranging information about a satellite.
 * The source of this data may be derived locally from a knowledge of time,
 * position, ephemeris and almanac or from a remote server.
 *
 * Most importantly note that the speed estimate does not include the
 * effects of the local oscillator. Note that positive speed increases
 * pseudo range and is therefore equivalent to negative Doppler.
 *
*/

typedef struct
{
  U32  q_FCount;    /* FCount when structure is valid */
  me_SvTime z_SvTime; /* Satellite signal time at user time tick [msecs] */
  FLT  f_SvTimeUncMs;  /* 1 sided max time uncertainty [msecs] */
  FLT  f_SvSpeed;    /* Pseudo range speed [m/s] */
  FLT  f_SvSpeedUnc;  /* 1 sided max speed uncertainty [m/s] */
  FLT f_SvAccel;    /* Pseudo range acceleration [m/s^2] */
} me_SvSteering;


/* Certain systems don't know local time particularly well, but do
   have a good idea of varous SV range differences. To provide a consistent
   and simple interface, the time differences are supplied in a single
   function, me_SvTimeDiffPut(), that differences the satellites to a
   single reference Sv. The uncertainties are NOT differenced. Instead, they
   are summed at the appropriate time to get SV differenced uncertainties.
   The DTIME_STATUS flags are arranged to have the
   same bit positions as w_MeasStatus. */


#define SV_DTIME_STATUS_SM_VALID MEAS_STATUS_SM_VALID /* TRUE -> Accept sub-ms
                                                         difference only */
#define SV_DTIME_STATUS_SB_VALID MEAS_STATUS_SB_VALID /* TRUE -> Accept mod 20
                                                         difference */
#define SV_DTIME_STATUS_MS_VALID MEAS_STATUS_MS_VALID /* TRUE -> Accept abs
                                                         msec difference */

typedef struct
{
  U8   u_Sv;           /* Satellite ID */
  U8   u_DTimeStatus;  /* Describes time difference ambiguity */
  FLT  f_DTimeMsecs;   /* SV Time - Reference SV Time */
  FLT  f_NDTimeUncMs;   /* 1 sided non-differenced SV only time unc [msecs];
							does NOT include clock time unc */	
  FLT  f_DSpeed;       /* Speed difference [m/s]. f_DTimeMsecs -= Msecs *
                          f_DSpeed/c */
  FLT  f_NDSpeedUnc;    /* 1 sided non-differenced SV only speed unc [m/s];
							does NOT include clock speed unc */	
} me_SvDiff;

#endif /* GPSC_ME_API_H */
