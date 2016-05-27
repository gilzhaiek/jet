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
 * FileName			:	gpsc_data.c
 *
 * Description     	:
 * This file contains defincation of the various data structures and constants
 * used throughout the GPSC.
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#ifndef _GPSC_DATA_H
#define _GPSC_DATA_H

#include "mcp_hal_types.h"
#include "mcpf_defs.h"
#include "gpsc_types.h"
#include "gpsc_sap.h"
#include "gpsc_ai2_api.h"
#include "gpsc_pe_api.h"
#include "gpsc_me_api.h"
#include "gpsc_utils.h"
#ifdef ALL_AI2_COMM_IN_FILE
#include <sys/time.h>
#endif


#ifndef UNREFERENCED_PARAMETER
#define UNREFERENCED_PARAMETER(P) ((P)=(P))
#endif

/* NMEA values */
#define NMEA_MASK 0x003F
        /* 0x003F = (GPSC_RESULT_NMEA_GGA|GPSC_RESULT_NMEA_GLL|GPSC_RESULT_NMEA_GSA|\
				  GPSC_RESULT_NMEA_GSV|GPSC_RESULT_NMEA_RMC|GPSC_RESULT_NMEA_VTG)*/

#define UNDEFINED_U8 0xFF

#define C_SMLC_COMPUTED_EVENT  ( C_EVENT_PERIODIC_MEAS)
#define C_MS_COMPUTED_EVENT  ( C_EVENT_PERIODIC_POS      | \
                                                         C_EVENT_PERIODIC_NONEWPOS |C_EVENT_PERIODIC_MEAS)
#define C_SMLC_COMPUTED_EVENT_EXT  ( C_EVENT_PERIODIC_MEAS_EXT)
#define C_MS_COMPUTED_EVENT_EXT  (C_EVENT_PERIODIC_POS_EXT       | \
	                                                 C_EVENT_PERIODIC_NONEWPOS |C_EVENT_PERIODIC_MEAS_EXT )
#ifdef CLIENT_CUSTOM
/*#define C_CUSTOM_COMPUTED_EVENT (C_EVENT_PERIODIC_TIME|C_EVENT_PERIODIC_POS_EXT|	C_EVENT_PERIODIC_NONEWPOS|C_EVENT_PERIODIC_MEAS_EXT|	C_EVENT_PERIODIC_MEASSTAT)*/

#define C_CUSTOM_COMPUTED_EVENT (C_EVENT_PERIODIC_POS_EXT|C_EVENT_PERIODIC_MEAS_EXT)

#endif


#define C_MAX_AIDING_FILENAME_LEN	100

/*All Aiding Filenames*/
#define C_STR_AID_POSITION_FILE		"GPSCPositionFile"
#define C_STR_AID_ALMANAC_FILE		"GPSCAlmanacFile"
#define C_STR_AID_EPH_FILE				"GPSCEphFile"
#define C_STR_AID_IONO_FILE			"GPSCIonoFile"
#define C_STR_AID_UTC_FILE				"GPSCUtcFile"
#define C_STR_AID_HEALTH_FILE			"GPSCHealthFile"
#define C_STR_AID_TCXO_FILE			"GPSCTcxoFile"
#define C_STR_AID_CLOCK_FILE			"GPSCClockFile"
#define C_STR_AID_SYSCLOCK_FILE			"GPSCSysClockFile"

/* assistance request attempts */
#define C_MAX_ASSIST_ATTEMPTS 1

#define SUPL_MAJ_VER_NUM 1
#define SUPL_MIN_VER_NUM 0

enum  /* Position Fix Tye */
{
  C_2D_FIX,
  C_3D_FIX
};


/* Configuration related constants */
enum  /* Control - User Plane selection */
{
  C_UP_ACTIVE,
  C_CP_ACTIVE
};

enum  /* Position Computing Entity selection */
{
  C_SMLC_COMPUTED,
  C_MS_COMPUTED,
  C_MS_COMPUTED_PREFERRED,
  C_SMLC_COMPUTED_PREFERRED
};

enum /* Server Access Permission selection: Privacy switch */
{
  C_SMLC_ASSIS_NOT_PERMITTED,
  C_SMLC_ASSIS_PERMITTED
};


enum
{
  C_LOC_REQ,
  C_ASSIS_DATA_REQ
}; /* Service Type */

enum
{
  C_GPSC_SENSOR_CONFIG1,
  C_GPSC_SENSOR_CONFIG2,
  C_GPSC_SENSOR_CONFIG3,
  C_GPSC_SENSOR_CONFIG4
};

/* Autonomous Mode Tests */
enum {
	  AUTONOMOUS_COLD_START,
	  AUTONOMOUS_HOT_START,
	  AUTONOMOUS_WARM_START
     };

enum {
	INVALID_MSG_DES = 0,		//Invalid message descriptor
	INVALID_MSG_LEN,			//Invalid message length
	INVALID_MSG_CHECKSUM,		//Invalid message checksum
	INVALID_MSG_PID,			//Invalid packet ID
	INVALID_MSG_PDATA,			//Invalid packet data
	INVALID_MSG_PLEN,			//Invalid packet length
	INVALID_MSG_SST			//Invalid software state transition commanded
};

/* System Handlers */
typedef struct  
{
	void * hMcpf;
	void * hNavcCtrl;
	T_GPSC_config_file GpscConfigFile;
#ifdef ALL_AI2_COMM_IN_FILE
	McpS32	uAllSensorAI2Fd;
	McpS8	uAllSensorAI2_path[C_MAX_AIDING_FILENAME_LEN];
#else
	McpS32	uToSensorFd;
	McpS8	uToSensor_path[C_MAX_AIDING_FILENAME_LEN];
	McpS32	uFromSensorFd;
	McpS8	uFromSensor_path[C_MAX_AIDING_FILENAME_LEN];
#endif
	McpS32	LogFileFd;
	McpS8	uPathCtrlFile[C_MAX_AIDING_FILENAME_LEN];
	McpS8	uGpscConfigFile[C_MAX_AIDING_FILENAME_LEN];
	McpS8	uPatchFile[C_MAX_AIDING_FILENAME_LEN];
	McpS8	uAidingPath[C_MAX_AIDING_FILENAME_LEN];
	McpS8	uUdpIp[50];
	McpU16	uUdpPort;
	McpS8	uLogNvmPath[50];
	McpS8	uSensorDataNvmPath[50];
	McpU8	uLogControl;
	McpU8	uSensorDebugControl;
	McpU32 uMaxLogLines;
	McpU32 uMaxNvmFiles;
	McpS32	uGPSWifiSelectorFd;
	McpS32	uWifiFd;
	McpS8	uSessionLogNvmPath[50];
	McpU8	uSessionLogControl;
} gpsc_sys_handlers;

typedef struct
{
	U8		custom_ver_req_flag;
	U8		custom_sleep_req_flag;
	U8		custom_wakeup_req_flag;
	U8		custom_pos_rep_ext_req_flag;
	U16		custom_assist_availability_flag;
	U8		wakeup;
	U8		custom_position_available_flag;
	U32		custom_pred_eph_bitmap;
	U8		loc_req_timer_first_time_check_flag;
	U8		custom_agc_req_flag;

}gpsc_custom_struct;

#ifdef ALL_AI2_COMM_IN_FILE

#define ALL_AI2_MAGIC_WORD 0xFFFFAABB
#define AI2_FROM_SENSOR	0
#define AI2_TO_SENSOR 1

typedef struct {
	U32		magic_word;
	struct timeval currtime;
	U32		length;
	U8		to_or_from;
	U8		resv[3];
} gpsc_all_ai2_hdr;
#endif

/*  constants used for data conversion */

#define C_LSB_TIME_BIAS        (1.0/16777216.0)  /* -0.5 to +0.5 range, 24
                                                    bits: approx. 15ns */
#define C_LSB_TIME_UNC_FINE    (0.655/65536.0)   /* 10ns, translates to 3
                                                    meters */
#define C_LSB_TIME_UNC_COARSE  (1000.0/65536.0)  /* 15.2us, translates to
                                                    4577 meters */
#define C_LSB_FRQ_BIAS         (0.1)             /* meters/sec */
#define C_LSB_FRQ_UNC          (0.1)             /* meters/sec */

#define C_LSB_SV_SUBMS         (1.0/16777216.0)  /* 24 bits */

#define C_LSB_SV_SPEED         (0.01)            /* meters/sec */
#define C_LSB_SV_SPEED_UNC     (0.1)             /* meters/sec. */
#define C_LSB_SV_TIME_UNC      (.000010)         /* 10ns per bit */
#define C_LSB_SV_ACCEL         (2.0/128.0)       /* cm/sec^2 */
#define C_LSB_LAT              (C_PI/65536.0/65536.0)      /* pi / (2^32) */
#define C_LSB_LAT_REP          (65536.0 * 65536.0 / C_PI)  /* (2^32)/pi */
#define C_EARTH_RADIUS         6371.04           /* Km. Assume this instead of
                                                    6378.00 Km, 0.1 % difference */
#define C_LSB_LON              (C_PI/32768.0/65536.0)      /* pi / (2^31) */
#define C_LSB_LON_REP          (65536.0 * 32768.0 / C_PI)  /* (2^31) / pi */
#define C_LSB_HT               (0.5)
#define C_LSB_HT_REP           2                 /* reciprocal of C_LSB_HT */
#define C_LSB_EER              (0.1)
#define C_LSB_NER              (0.1)
#define C_LSB_VER              (0.5)
#define C_LSB_EVEL             (1000.0/65535.0)  /* -500 to 500 range, 16
                                                    bits: LSB = 0.0152590
                                                    meters/sec */
#define C_LSB_NVEL             (1000.0/65535.0)  /* -500 to 500 range, 16
                                                    bits: LSB = 0.0152590
                                                    meters/sec */
#define C_LSB_VVEL             (1000.0/65535.0)  /* -500 to 500 range, 16
                                                    bits: LSB = 0.0152590
                                                    meters/sec */
#define C_LSB_RSDL             (0.1)
#define C_LSB_SV_WT            (0.01)
#define C_LSB_POS_SIGMA        (0.1)             /* 10cm per bit */
#define C_RECIP_LSB_POS_SIGMA  10.0              /* 1/C_LSB_POS_SIGMA */
#define C_LSB_DIFF_TIME        (40.0/8388608.0)  /* ms */
#define C_LSB_DIFF_TIME_UNC    (1.0/65536.0)     /* ms */
#define C_LSB_DIFF_SPEED       (2000.0/32768.0)  /* meters/sec */
#define C_LSB_DIFF_SPEED_UNC   (0.1)             /* meters/sec */

#define C_LSB_ELEV_MASK        (90.0/128.0)      /* Elevation degrees */
#define C_LSB_AZIM_MASK        (360.0/256)       /* Azimuth degrees */


  /* elevation represented by S8, LSB = 180/256 or 90/128, representable
   * range -90 to 90-LSB
   * azimuth represented by U8, LSB =  360/256, representable range
   * from 0 to 360 degrees.
   */
#define C_LSB_MAX_CLK_FREQ_UNC (1.0)             /* m/s */
#define C_LSB_MAX_CLK_ACCEL    (0.001)           /* m/s/s */
#define C_LSB_MAX_USER_ACCEL   (0.001)           /* m/s/s */

#define C_RAD_TO_DEG           (180.0 / C_PI )   /* factor converting from
                                                    radius to degree */
#define C_DEG_TO_RAD           (C_PI / 180.0 )   /* factor converting from
                                                    degree to radius */

#define C_POS_UNC_GROWTH       (FLT)20.0         /* position uncertainty
                                                    grows at 20
                                                    meters/second */

#define C_POS_SHAPE_ELLIP_ALT_UNC_ELLIP 9   /* ellipsoid point with altitude
                                               and unc. ellipsoid */
#define C_POS_SHAPE_ELLIP_UNC_ELLIP 3		/* ellipsoid point unc. ellipse */

/**** max. 16 bits can be used for this group of constants: the low 9 bits
      follow the same order as the AssistanceWishlist ***/

#define C_ALM_AVAIL          0x0001

#define C_UTC_AVAIL          0x0002

#define C_IONO_AVAIL         0x0004
#define C_EPH_AVAIL          0x0008
#define C_DGPS_AVAIL         0x0010
#define C_REFLOC_AVAIL       0x0020

#define C_REF_GPSTIME_AVAIL  0x0040 /* mandatory for ref time */

#define C_ACQ_AVAIL          0x0080
#define C_RTI_AVAIL          0x0100
#define C_ANGLE_AVAIL        0x0400

#define C_REF_GSMTIME_AVAIL  0x0800 /* optional for ref time */

#define C_REF_TOW_AVAIL      0x1000 /* optional for ref time */

#define C_TCXO_AVAIL		 0x2000 /* optional for ref time */

#define C_ALT_AVAIL			0x4000  /* Altitude */


#define C_FOUR_HRS_MSEC     14400000L /* 4 hours worth of msec */


#define C_90_OVER_2_23    (DBL)((DBL)90/(DBL)8388608 )
  /* 0.0000107288360595703125: used to convert lat. octet to DBL degrees */

#define C_360_OVER_2_24   (DBL)((DBL)360/(DBL)16777216 )
  /* 0.000021457672119140625: used to convert long. octets to DBL degrees */


#define C_2_23_OVER_90    (DBL)((DBL)8388608 / (DBL)90 )
  /* 93206.75555555555555555556: used to convert lat. degrees to the octet */

#define C_2_24_OVER_360   (DBL)((DBL)16777216 / (DBL)360 )
  /* 46603.37777777777777777778: used to convert long. degrees to octets */

#define C_CELL_RADIUS  10000 /* assume 10km cell radius, arbitrarily for now */

#define C_SV_DIRECTION_UPDATE_INTERVAL_SEC  120
                                        /* the interval at which
                                           SV-Direction database is
                                           updated, in seconds */
#define C_MAX_VALID_TIME_UNC_MS  60*1000  /*Max time unc in Ms beyond which time
										   is considered invalid*/

#define C_GOOD_IONO_AGE_SEC (84 * 3600) /* age in second for IONO to be
                                           considered good enough with no
                                           need to get updated from SMLC:
                                           half week */

/** for now lets treat the UTC in similar lines to IONO for aging **/
#define C_GOOD_UTC_AGE_SEC (84 * 3600)  /* age in second for UTC to be
                                           considered good enough with no
                                           need to get updated from SMLC:
                                           half week */

#define C_GOOD_ALM_AGE_SEC (84 * 3600 ) /* age in second for almanac data to
                                           be considered good enough with no
                                           need to get updated from SMLC:
                                           half week */
#define C_GOOD_EPH_AGE_SEC   (2 * 3600) /* age in second for ephemeris data
                                           to be considered good enough
                                           with no need to get updated from
                                           SMLC: 2 hours */

#define C_GOOD_HEALTH_AGE_SEC ( 2 * 3600 ) /* age in second for health data to
									   	                        be considered good:  2 hours**/


#define C_GOOD_INIT_POS_UNC      15000  /* maximum initial position
                                           uncertainty beyond which MS
                                           should request reference
                                           location from SMLC */
#define C_GGOD_INIT_POS_UNC_EPH  15000  /* maximum initial position
                                           uncertainty beyond which MS
                                           should request ephermis
                                           regardless of ephemeris in
                                           database */

#define C_MIN_EPH_SV  6                 /* minimum SVs with "good" ephemeris,
                                           below which LM will request eph
                                           assistance data from SMLC. */

#define C_MAX_PACKET_DATA_SIZE   1024

#define C_HALF_U32               (U32)2147483648  /* 2 ^ 31 */

#define C_SECS_PER_WEEK          (7 * 24 * 60 * 60)  /* seconds in a week */

#define C_RRLP_DOP0_RES          2.5           /* resolution of Doppler0 in
                                                  RRLP */
#define C_RRLP_DOP1_RES          (1.5 / 63.0)  /* resolution of Doppler1 in
                                                  RRLP */
#define C_RRLP_ANGL_RES          11.25         /* resolution of angle
                                                  (azimuth and elevation)
                                                  in RRLP */

/*
 * Each data bit, (D30* thru d24) will exhibit an effect on the 6 parity
 * bits in d26 .. d30. Build a table that identifies how each of the data
 * bits affects the 6 bit parity result. This will be used to derive the
 * parity sum. ---- For Scarse Pattern Match use
*/
#define C_N_PARITY  26  /* Number of bits that affect parity */



#define C_MSEC_PER_CHIP   (1.0 / 1023.0 )            /* msec per chip */
#define C_MSEC_FRAC_CHIP  (C_MSEC_PER_CHIP / 1024 )  /* msec per 1024th of
                                                        a chip */

#define C_MAX_RRLP_SIZE_BYTE 244 /* maximum RRLP message size: 244 bytes */

#define RESULT_BUFFER_LENGTH 3000

/* clock config support */
#define C_APPLY_TIMESTAMP_REFCLK_ENABLE  1
#define C_APPLY_TIMESTAMP_REFCLK_DISABLE 0

#define GPSC_INVALID_FCOUNT 0

/* for accuracy and Timeout*/
#define C_ACCURACY_UNKNOWN 0xFFFF
#define C_TIMEOUT_UNKNOWN 0xFFFF
#define C_ACC_VARIATION 10
#define C_FIRSTMEAS_INVALID 0xFFFFFFFF
#define C_T2DELTA	250 /* in ms */
#define C_ACCURACY_MAXOUT 400  /*meters*/
#define C_LOC_AGE_UNKNOWN 0xFF

/* RF Registers */

#define C_RF_AGC_REG  0x001CC0D6
#define REFE_GAIN 	54

/* geofence */
/*#define GEOFENCE_MOTION_DISABLE                 0
#define GEOFENCE_LOCATION_MASK                  0x2 // Bit-1 set
#define GEOFENCE_SPEED_MASK                        0x4  // Bit-2 Set
#define GEOFENCE_LOCATION_LIMIT_MASK       0x8 // bit-3 set
#define GEOFENCE_SPEED_LIMIT_MASK             0x10 // bit-4 set
#define GEOFENCE_SPECIFIED_CENTER_MASK      0x20   //bit-5 set
#define GEOFENCE_ALTITUDE_MASK            0x40  // Bit-6 set
#define GEOFENCE_MOTION_ENABLE_MASK       0x01
#define GEOFENCE_MOTION_MASK                      0x06 // (GEOFENCE_LOCATION_MASK | GEOFENCE_SPEED_MASK)
#define GEOFENCE_MOTION_DISABLE                 0
#define GEOFENCE_MAX_VERTICES                     10 */

/*
 * Mutil Session Management Constants
 */
/*Below w_SessionTagId Values are defined as special cases*/
#define C_SESSION_INVALID 	0xFFFFFFFF  /*to deal with w_SessionTagId which is a request for all,
                                          eg deleting all nodes, else is invalid*/
#define C_SESSION_LOCAL    	0xFFFFFFFE  /*to define w_SessionTagId when is generated for local location fix request
                                          used in case of RTI feature*/
#define SESSION_OWNER_MASK 0xC000
#define SESSION_ULTS 0x8000  /*first 2 bit =2*/
#define SESSION_SUPL 0x7000 /*first 2 bit =1*/
#define SESSION_APPL 0x0000 /*first 2 bit =0*/


#define C_PERIOD_PRESET 1 /*ReportPeriod must be set as 1 for new session*/
#define C_PERIOD_NOTSET 0  /*ReportPeriod  must be calculated based on HCF*/
/* low power mode */
#define LOW_POWER_MODE_SLEEP 0
#define LOW_POWER_MODE_IDLE	 1


#define GPSC_VISIBLE_EPH_INJ_THOLD 70 /* indicates 7.0dbm, scalled by 10 to compare with C/no value in measurement report */

#define C_BLF_STATE_DISABLED	0
#define C_BLF_STATE_ENABLED	1
#define C_BLF_STATE_DORMANT	2
#define C_MAX_BLF_BUFFER_COUNT 100

/* Can have up to 15 different mode definitions.

   These modes define what kind of roles "Total Number of Fixes Per
   Session" (TNFS) and "Inter Session Interval" (ISI) play, when they both
   are set to non-zero, OR BOTH SET TO ZERO.

   Note that if TNFS is set 0, number of fixes will
   no long be a factor in deciding when to end a session and/or to start
   the next; Also if ISI is set to 0, it means to start the next session
   ASAP after the previous one has ended. In the case where both are
   non-zero, the follow modes dictate the behaviors.

   Note that these definitions are only applicable to muti session
   cases. For a single session case where TotalNumberOfSessions = 1, since
   there is no such an issue as when the next session starts, if both TNFS
   and ISI are non-zero, it simply means terminate the session when either
   of the two expires. */

enum
{
  C_MULTIFIX_SESSION_MODE_0,
    /* Session terminated by TNFS or ISI whichever expires first, but
       always starts next session at ISI */
  C_MULTIFIX_SESSION_MODE_1,
    /* Session terminated by TNFS, but next session won't start until
       ISI has expired */
#ifndef NO_TRACKING_SESSIONS
  C_MULTIFIX_SESSION_MODE_2
    /* Session of indefinite length. Session terminated by User Command. */
#endif
};

/* During C_MULTIFIX_SESSION_MODE_2, Check for outdated ephemeris
 * and emergent SV requiring ephemeris. Make this check once every
 * N Ticks (Seconds).
 */

#define C_TRACKMODE_EPHCHECK_PERIOD (600)   /* 10 Minutes */
#define C_TRACKMODE_SVCHECK_PERIOD  (300)   /*  5 Minutes */
#define C_EPHEMERIS_RE_REQUEST_DELAY (300)  /*  5 Minutes */

#ifdef LARGE_UPDATE_RATE
#define C_PERIOD_THRESHOLD		(U32)(180)	/* 3 minutes */
#define	C_THRESHOLD_FIX_TIME	(U32)(30000)/* 30 seconds */
#define C_NO_FIX_CORRECTION		(U32)(30000)/* 30 seconds */
#define C_SLEEP_DELTA			(U16)(2000)	/* 2 seconds */
#else
#define C_PERIOD_THRESHOLD		(U32)(65535)/* Max 16-bit unsigned value */
#endif


/* added for patchdownload for the record byte 1120 */
#define C_PATCH_RECORD_BYTES 1120

#define C_TIMER_AI2_ACK_WAIT_PERIOD 30000 /*30 seconds*/
#define C_TIMER_TX_RES_WAIT_PERIOD 30000 /*30 seconds*/
#define C_TIMER_CLOCK_CALIB_WAIT_PERIOD 600 /*600 milliseconds*/
#define C_GPSC_TIMEOUT_PERIODIC_REPORT 8000 //Wait for at elast 8 secs for periodic reports
#define C_GPSC_TIMEOUT_NO_LOCREQ 60000 /*1min wait for location request*/
#define C_GPSC_TIMEOUT_NO_LOCREQ_CUSTOM		20000 	/* 20sec  */
#define C_GPSC_TIMEOUT_NO_LOCREQ_CUSTOM_FIRST		5000 	/* 5sec for the first timout */
#define C_GPSC_TIMER_SEQUENCE_TIMEOUT 10
#define C_GPSC_TIMEOUT_MEAS_TRACKING 	3000   	/*measurement tracking timer 3 seconds*/
#define C_GPSC_TIMEOUT_EPH_RE_REQUEST 	10000	/* 10 sec Timer to validate ephemeris age*/ 
#define C_GPSC_TIMEOUT_ALMANAC_NO_ASYNC 	2000 /*2sec timeout for no aync report for almanac */


enum
{
	  C_TIMER_ALL,
	//  C_TIMER_STATE,
	/* 1 */  C_TIMER_AI2_ACK_PENDING,
	/* 2 */  C_TIMER_TX_RES_PENDING,
	/* 3 */  C_TIMER_TX_DATA_PENDING,
	/* 4 */  C_TIMER_DISCRETE_RES_PENDING,
	/* 5 */  C_TIMER_CLOCK_CALIB_WAIT,
	/* 6 */  C_TIMER_REFCLKREQ_RES_PENDING,   //added for config 4 support
	/* 7 */  C_TIMER_PERIODIC_REPORTING,  // added for Periodic Reporting
	/* 8 */  C_TIMER_WAIT_LOCREQ,       ////added for timeout for assistance and location req
	/* 9 */  C_TIMER_SEQUENCE,
	/* 10 */  C_TIMER_WAIT_LOC_REQ_SEQ,
	/* 11 */  C_TIMER_PLT_SEQUENCE,
	/* 12 */  C_TIMER_SESSION_SLEEP,
	/* 13 */  C_TIMER_AUTO_POWER_SLEEP,
	/* 14 */  C_TIMER_MEAS_TRACKING,
	/* 15 */  C_TIMERS_PLUS_ONE,
	/* 16 */  C_TIMER_SLEEP,
	/* 17 */  C_TIMER_EXP_REQ_CLOCK,
	/* 18 */  C_TIMER_ALMANAC_AYNC,
	/* 19 */  C_TIMER_RXN_EPHE_VALID,
        /* 20 */ C_TIMER_NVS_SAVE
};

#define C_TIMER_EXP (2*60*1000) //10 minuts
#define C_MAX_COMPAT_VERS 5

#define C_MAX_DRIVER_BUF 400

#define C_CLOCK_DEGRADE 0xDE
#define C_CLOCK_CALIB_WAIT 0xCC

#define C_MIN_NVS_POS_UNC_1KM   (1000 * C_RECIP_LSB_POS_SIGMA)     /* Minimum acceptable NVS position uncertainty to inject */
#define C_MAX_NVS_POS_UNC_500KM (500000 * C_RECIP_LSB_POS_SIGMA) /* Maximum accpetable NVS position uncertainty to inject */
 /*
 * Structure Definitions
 */

typedef struct
{
	U8	u_CommMode;
	U8  u_DataRate;
	U8  u_I2cLogicalId;
	U8	u_I2cCEAddressMode;
	U16	w_I2cCEAddress;
	U8	u_I2cGPSAddressMode;
	U16 u_I2cGPSAddress;
}gpsd_comm_config;


#define gpsc_cfg_type T_GPSC_config_file

/*
* enum to Variable feature_flag
* Dynamic Configuration Feature types. This feature type decribes the dynamic feature requested by application to the GPSC.
*/
typedef enum
{
	GPSC_FEAT_NO_REQUEST           = 0x0,           /* No Request                     */
		GPSC_FEAT_SBAS                 = 0x1,           /* SBAS Feature Type              */
		GPSC_FEAT_KALMAN               = 0x2,           /* Kalman Feature Type            */
		GPSC_FEAT_HOSTWAKEUP           = 0x4,           /* Host Wakeup Feature Type       */
		GPSC_FEAT_MOTION_MASK          = 0x8,           /* Motion Mask Feature Type       */
		GPSC_FEAT_APM                  = 0x10,           /* APM Feature Type               */
                GPSC_FEAT_BLF                  = 0x20           /* Buffered location Fix Feature type */
}gpsc_feature_flag;

/*
* GPSC dynamic HW Flag for backward compatibility
*/

typedef enum
{
		GPSC_HW_NONE           = 0x0,           /* No HW                     */
		GPSC_HW_TRF5101                 = 0x1,           /* TRF5101              */
		GPSC_HW_GPS5200               = 0x2,           /* GPS5200            */
		GPSC_HW_WL128X               = 0x3, 		/* WL128X            */
		GPSC_HW_GPS53X0           = 0x4,           /* GPS53X0       */
		GPSC_HW_NL5500           = 0x5,           /* GPS53X0       */
        GPSC_HW_MAX                  = 0x20           /* MAX Number */
}gpsc_hw_type;



typedef enum
{
	GPSC_NORMAL_REQUEST,           /* Normal request                     */
	GPSC_GEOFENCE_REQUEST,           /* Geofence request              */
	GPSC_BUFFEREDLOCATION_REQUEST           /* buffered location request            */
}gpsc_type_of_request;


typedef enum
{
    GPSC_NW_SESSION_UNKNOWN   = 0x0,           /* Unknown Session Type  */
    GPSC_NW_SESSION_SI        = 0x1,           /* Set Initiated Request  */
    GPSC_NW_SESSION_NI        = 0x2 ,          /* Net Initiated Request */
    GPSC_NW_SESSION_LP        = 0x3 ,          /* LP Initiated Request */
    GPSC_NW_SESSION_MAX       = 0xFF ,
}gpsc_type_of_nw_session;




/*
* GPSC dynamic feature configuration.The configuration is injected into the receiver by calling the SAP function or can be injected in the init state using the config file.
*/
typedef struct
{
	gpsc_feature_flag	         feature_flag;             /*<  0:  4> (enum=32bit)<->T_GPSC_feature_flag Dynamic Configuration Feature types. This feature type decribes the dynamic feature requested by application to the GPSC. */
	McpU8                        kalman_control;           /*<  4:  1> Kalman feature control enable or disable bit.0 : Enable Kalman filter feature, 1: Disable Kalman filter feature. */
	McpU8                        zzz_align0;               /*<  5:  1> alignment                                          */
	McpU8                        zzz_align1;               /*<  6:  1> alignment                                          */
	McpU8                        zzz_align2;               /*<  7:  1> alignment                                          */
	McpU32				         apm_control;              /*<  8:  4> (enum=32bit)<->T_GPSC_apm_control Advance Power Management Feature Activation */
	McpU8                        Rsvd_pwr_mgmt1;           /*< 12:  1> Advance Power Management reserved bits. 3 bytes are reserved for Advanced Power Management. */
	McpU8                        zzz_align3;               /*< 13:  1> alignment                                          */
	McpU8                        zzz_align4;               /*< 14:  1> alignment                                          */
	McpU8                        zzz_align5;               /*< 15:  1> alignment                                          */
	McpU32				         search_mode;              /*< 16:  4> (enum=32bit)<->T_GPSC_search_mode Advance Power Management search mode Activation */
	McpU32					     saving_options;           /*< 20:  4> (enum=32bit)<->T_GPSC_saving_options Advance Power Management saving option. Power save state to enter while no active acquisition is ongoing. */
	McpU8                        power_save_qc;            /*< 24:  1> Unsigned: 10 msec per bit, Range: 100ms default to 900ms maximum */
	McpU8                        Rsvd_pwr_mgmt2;           /*< 25:  1> Advance Power Management reserved bits. 3 bytes are reserved for Advanced Power Management. */
	McpU8                        Rsvd_pwr_mgmt3;           /*< 26:  1> Advance Power Management reserved bits. 3 bytes are reserved for Advanced Power Management. */
	McpU8                        zzz_align6;               /*< 27:  1> alignment                                          */
	McpU32				         host_req_opt;             /*< 28:  4> (enum=32bit)<->T_GPSC_host_req_opt Host Wakeup Activation. Supported both in NL5500 and NL5350. */
	McpU16                       host_assert_delay;        /*< 32:  2> Minimum delay between host request signal (GPS_IRQ) assertion to commencement of transmission from GE. Unsigned, 30.5 usec (RTC clock period) per bit Range: 0 to 1 sec(Default : 5 ms).Supported only in NL5500. */
	McpU16                       host_reassert_delay;      /*< 34:  2> Minimum delay between host request (GPS_IRQ) signal de-assertion to re-assertion. Unsigned, 30.5 usec (RTC clock period) per bit Range: 0 to 1 sec(Default : 5 ms). Supported only in NL5500. */
	McpU8                        host_ref_clk_req_opt;     /*< 36:  1> Specifies host reference clock request signaling options for calibration purposes. 0: Disable,1: GPS HW signal controlled calibration,2: GPS SW message controlled calibration. Supported only in NL5500. */
	McpU8                        host_ref_clk_req_sig_sel; /*< 37:  1> Selects signal to be used for REF_CLK request. Applicable only for GPS HW signal controlled calibration option.0: Use GPS_IRQ pin for making reference clock requests,1: Use REF_CLK_REQ pin for making reference clock request. Supported both in NL5500 and NL5350. */
	McpU16                       host_ref_clk_assert_dly;  /*< 38:  2> Minimum delay between host reference clock request signal assertion to availability of clock. Unsigned, 30.5 usec (RTC clock period) per bit Range: 0 to 1 ssec(Default : 5 ms). Supported both in NL5500 and NL5350. */
	McpU16                       host_ref_clk_reassert_dly; /*< 40:  2> Minimum delay between host reference clock request signal de-assertion to re-assertion for next clock request. Unsigned, 30.5 usec (RTC clock period) per bit Range: 0 to 1 sec (Default : 91.5 usec). Supported both in NL5500 and NL5350. */
	McpU8                        host_sigout_type_ctrl;    /*< 42:  1> Bit 1: Output type control for Host REF_CLK Request0: Open source signal (Default)1: Push/Pull signal.Bit2: Output type control for TCXO_CLK_REQ signal0: Open source signal (Default)1: Push/Pull signal. Supported only in NL5500. */
	McpU8                        zzz_align7;               /*< 43:  1> alignment                                          */
	McpU32				         sbas_control;             /*< 44:  4> (enum=32bit)<->T_GPSC_sbas_control SBAS Activation. */
	McpU32                       sbas_prn_mask;            /*< 48:  4> SBAS prn mask.Bit field 0 - 18 corresponds to SBAS SVs 120 to 138. Bit field 19-31 is reserved and set to 0. By default all bits are set to 0 which corresponds to searching all PRN. */
	McpU8                        Mode;                     /*< 52:  1> SBAS mode. Set to 0.                               */
	McpU8                        Flags;                    /*< 53:  1> SBAS flags. Set to 0.                              */
	McpU8                        zzz_align8;               /*< 54:  1> alignment                                          */
	McpU8                        zzz_align9;               /*< 55:  1> alignment                                          */
	McpU32                       rsvd_sbas1;               /*< 56:  4> SBAS reserved bits. 8 bytes are reserved for SBAS. */
	McpU32                       rsvd_sbas2;               /*< 60:  4> SBAS reserved bits. 8 bytes are reserved for SBAS. */
	McpU32						 motion_mask_control;     /*< 64:  4> (enum=32bit)<->T_GPSC_motion_mask_control Motion Mask Activation. */
	McpS32                       altitude_limit;           /*< 80:  2> Motion Maskaltitude limit..Signed, 1 meters per bit.Range: -32768 to 32767 meters. */
	McpS32						 area_altitude; 			/*< 68:  4> Motion Mask area origin latitude.Signed, pi/32 radians per bit.Range: -pi/2 to pi/2 - pi/232 radians.Used only if Motion Mask Control Bit 5 is '1'. */
	McpS32						 latitude[MAX_VERTICES]; 		 
	McpS32						 longitude[MAX_VERTICES];		 
	McpU16                       speed_limit;              /*< 82:  2> Motion Mask speed limit.Unsigned, 0.01 meters/sec per bit.Range: 0 to 655.35 meters/sec */
	McpU16                       radius_of_circle;         /*< 78:  2> Motion Mask radius of circle.Unsigned, 1 meters per bit.Range: 1 to 65535 meters. */
	McpU8                        uSessionId;      
    McpU8                        report_configuration;      
	McpU8                        region_number;             
	McpU8                        region_type;               
	McpU8                        no_of_vertices;            
	McpU8		   				 z_Alignment1;		 
	McpU8		 				 z_Alignment2;
	McpU8		  				 z_Alignment3;	
        /* Buffered location fix parameters */
        McpU8           blf_en_flg;
        McpU16          blf_fix_count;
        McpU8           blf_en_position;
        McpU8           blf_en_velocity;
        McpU8           blf_en_dop;
        McpU8           blf_en_sv;
} gpsc_dyna_feature_cfg_type;

/* The structure gpsc_sess_specific_cfg_type needs to be defined for the multiple session
   specific information to support the Hybrid mode in the GPSC for GPS5350 */
struct gpsc_sess_specific_cfg_type
{
  U16   w_FixCounter;       /* counter used to count down fix reports in a
                              session
                              -0 : session need to be deleted
                              -other values: are valid session numbers
                              -1: In case of infinite reports this number is always set to 1*/
  U8 u_PosCompEntity;     					/*	Position Computing Entity -
  												0: ServerComputed;
  												1: MsComputed;
  												2-255 reserved. */
  U16 w_TotalNumFixesPerSession; 			/* 	Total number of sessions.
  												0: unlimited;
                               					1: 65535 valid numbers. */

  U32 q_ValidFixTimeout;					/*  	Wait period before a no fix is reported
                                                                              This value will be set to TTFF
													if TTFF = 0 , not timeout is required 
													to support NL5500 0.5 sec reporting, w_ValidFixTimeout (U16) 
													changed to q_ValidFixTimeout (U32)
											*/
													
  U16 w_ReportPeriod;							/*	Periodicity for each session.
  													(ISI) For multi sessions: 1-65535 interval
  													between the start of a seesion to the start of
  													the next (second).

  													For single sessions:
  													1:65535 end session time
  													0:	time not a limiting factor in ending or
  														starting sessions, for multi session it
													  	implies ASAP
													Session behavior needs to take
													w_TotalNumberFixesPerSession and u_SessionMode
										    		into account */
  U32 w_SessionTagId;							/*	Id used to map the location fix reports and request
  													for different session at GPSM */

  U32 q_PeriodElapsed;						/* to track loc_fix_ind with Reporting Period of the session
												to support NL5500 0.5 sec reporting, w_PeriodElapsed (U16) changed to 
												q_PeriodElapsed (U32)*/
  U8  u_WaitFirstFix;                                            /*waiting for first fix*/
  U8  u_OneSecDelay;          /* added to fix GPSC: NMEA issue - GSV is not populated with any data till one sec after fix.*/
  U16 w_ResultTypeBitmap;	 /*	Location fix result type bitmap, removed from session common and added
  								as session specific result type bitmap */
									
  U8 u_LocFixPosBitmap;     /* Added foe Wifi Position Development for specifying the type of position is requested by the application*/									
   gpsc_type_of_request w_typeofrequest;     /* 0 normal request 1 geofence request 2 buffered location */
	McpU32 RegionBitmap;	
	McpU32 GeofenceStatus;
	McpU32  GeofenceReportConfigration;
	McpU8  RegionNumberMap[GEOFENCE_MAX_REGIONS+1];
	gpsc_type_of_nw_session w_typeofsession;
  U8 u_BLF_Flag;		/* This flag denotes whether this specific session is BLF enabled */
//  T_GPSC_geo_fence_config_params z_GeoFenceConfig;
T_GPSC_call_type call_type; /*added for priotization*/
  McpU8 u_ApmConfig;
  struct gpsc_sess_specific_cfg_type* p_nextSession;	/*	Link list for the next node. */
};  /* An instance of this structure keeps all the listed
                       information in LM's non-volatile memeory */
typedef struct gpsc_sess_specific_cfg_type gpsc_sess_specific_cfg_type;

/* to support hybrid mode exclusively for GPS5350, gpsc_sess_cfg_type structure is
   redefined with compilatin flag GPS5350 */

typedef struct
{
	U8	u_PosCompEntity;			/*	Position Computing Entity
										0: ServerComputed
										1: MsComputed;
										2-255 reserved. */
	U16	w_TotalNumSession;			/*	Total number of sessions */
	U8	u_SmlcAssisPermit;			/*	Permission to access SMLC -
										0: NotPermitted;
										1: Permitted;
										2-255 reserved. */
	U16	w_AvgReportPeriod;	        	/*	Periodicity for all session. (HCF of report period of each session)*/
	U32	q_PreSessionActionFlags;	/*	Bit map indicating actions need to take before a session. */
	S16	x_ActionTimeOffset;			/* 	Used to alter clock time offset. */
	U16	w_ActionTimeUnc;				/* 	Used to alter clock time uncertainty */
	S16	x_ActionPosEOffset;			/* 	Used to alter east postition */
	S16	x_ActionPosNOffset;			/* 	Used to alter north position */
	U16	w_ActionPosUnc;				/*  Used to alter position uncertainty */
	U8	u_SessionMode;				/*  if
										0: session always starts at ISI;
										1: session starts at ISI */
	U8	u_SensorDiagSettings;		/*  Controls the Sensor's diag. output settings */

#ifdef LARGE_UPDATE_RATE
	U32 q_SleepDuration;	/* The sleep duration in mseconds, to sleep
							   between sessions */
	U16 w_SleepDelta;		/* Contains the approximation for time to sleep/awaken.
							   Constant value */
	U32 q_NoFixDelta;		/* Used to reduce the sleep duration, if a no-fix
							   report is generated after waking up. This is to
							   allow a robust way to get a fix if something
							   went wrong after waking up */
	U32 q_SleepActionStart;
	U32 q_SleepActionSuccessful;
							/* Used to determine the time taken for discrete action */
#endif
	U32 q_PeriodicExpTimerVal;	/* contains Periodic Expiry timer value in milisecond */

	
  U8 u_NullRepFlag; /* to use with NULL report */
  U8 u_ReConfigFlag; /* To use with NULL Report */
  U16 w_OldAvgRepPeriod;
  U32 q_LastFCount;        /* to use with NULL report */
  U32 q_FCount;				/* to use with NULL report */

  U32 q_FCountMeas;			/* to use with NULL report for measurement */
  U32 q_LastFCountMeas;     /* to use with NULL report for measurement */

  U8  u_SvNewAlmCount;
  U8  u_ExistingSession;	/* to support any change in existing session parameters */
  U8  u_BLF_State;            /* Flag to denote that BLF session is running */
  U16  u_BLF_Count;
	gpsc_sess_specific_cfg_type*  p_zSessSpecificCfg;

} gpsc_sess_cfg_type;  /* An instance of this structure keeps all the listed
                       information in LM's non-volatile memeory */




typedef struct
{

  U16 w_Wishlist;    /* Bit map: LSB up - alm, utc, iono, navmodel, dgps,
                        refloc, reftime, acq-assist., real time integrity,
                        reserved. */

  U32 q_GoodEphList; /* Bit map: SLB up - SV1, SV2, ..., a 1 means eph. good,
                        otherwise 0 */
  U32 q_NeedEphList; /* Bit map: LSB up - SV1, SV2, ..., a 1 means needed
                        otherwise 0 */

  U32 q_LastNeedEphList;
} gpsc_assist_wishlist_type;


typedef struct
{

  U16  w_NumUnRptFixEvt;   /* Number of PE Fix/NoFix events that were not
                              reported to SIA (due to MinValidFixInterval
                              or ValidFixTimeout). Incremented everytime a
                              PE Fix / NoFix is received, cleared everytime
                              a fix report is sent to SIA. Used to count
                              toward MinValidFixInterval or ValidFixTimeout */

  U16  w_NumUnRptMsrEvt;   /* Number of measurement events ( Measurement or
                              NoNewMeasurement ) that were not reported to
                              SIA (due to the quality of measurement or the
                              fact of being NoNewMeasurement, AND that
                              ValidFixTimeout not yet expired). Incremented
                              everytime a Meas. or NoNewMeas. event is
                              received, gets cleared when an RRLP
                              GpsMeasurement is sent. Used to count toward
                              ValidFixTimeout */


#ifndef NO_TRACKING_SESSIONS
  U32  q_LastEphChkLmTick; /* App. LmTick when ephemeris inventory was last checked */
  U8   u_TrackSubState;    /* Sub-States of Tracking Mode Session */
#endif
#ifndef UNLIMITED_EPHEMERIS_STORAGE

  U32  u_AcqAsstListedSVs;   /* SV List (bitmap) for Visible Satellites */

#endif
  U8   u_WishlistBuilt;    /* Set when the wishlist is built, cleared at end of session. */
  U8   *p_RrlpBuf;         /* Saved RRLP buffer pointer */
  U16  w_RrlpBufLen;       /* Saved length */
  U8   u_BuildWishlistLater;
} gpsc_session_status_type;

enum {
  TRKSTATE_INITIAL,
  TRKSTATE_NORMAL,
  TRKSTATE_WAIT_SV_DIR,
  TRKSTATE_SV_DIR_RDY,
  TRKSTATE_WAIT_EPH_ASSIST,
  TRKSTATE_EPH_RDY
};
typedef enum{ 
NVM_INJ_EPH = 0,
NVM_INJ_ALM,
NVM_INJ_POS,
NVM_INJ_UTC,
NVM_INJ_IONO,
NVM_INJ_HEALTH,
NVM_INJ_DONE}gpsc_nvm_inj_state;

typedef struct
{
  U32	q_TotalBytes;
  U32	q_BytesSent;
  U16	w_TotalRecords;
  U16	w_NextRecord;
  U16   w_LastRecordLength;
  U16   w_RecordLength;
  U8	u_RecordBuffer[C_PATCH_RECORD_BYTES +2];
  U8	u_PatchFileOpen;
  U8    u_FastDownload;
  S32	pPatchFile;

}gpsc_patch_status_type;

typedef struct
{
  U8   u_GPSCState;
  U8   u_GPSCSubState;
  U8   u_GPSCStateSeq;
  U8   u_SVCounter;
  U8   u_IgnoreFirstByte;	/*Set to true when power is switched off*/
  U8   u_SensorOpMode;          /* 0:ON; 1:OFF; 2:IDLE; 255: UNKOWN; */
  U8   u_SensorOpModeConfirmed; /* TRUE when confirmed by PING Report */
  U8   u_LocationRequestPending;/* TRUE when there is a location request pending*/
  U8   u_AssistInjectPending;   /* TRUE when assistance inject pending and FALSE when assistance inject completes */
  U8   u_ReadyRequestPending;/* TRUE when there is a location request pending*/
  U8   u_ShutDownRequestPending;/* TRUE when there is a shutdown request pending*/
  U8   u_PowerSaveRequestPending;/* TRUE when there is a shutdown request pending*/
  U8   u_InitRequestPending;/* TRUE when there is a shutdown request pending*/
  U8   u_FixStopRequestPending;/* TRUE when there is a loc fix stop request pending*/
  U8   u_ClockQualInjectPending;
  U8   u_AsyncPulseEventPending;  /*TRUE when Asynch Pulse event is pending*/
  U8   u_FineTimeRespPending;  /*True when response from GPSM containing
								  fine time response is pending*/
  U8   u_ProductlinetestRequestPending;  /* pending flag for product line test*/

  U32  w_TimerTrackBitmap; /* Bitmap containing information about which timer is set*/
  U8   u_SensorClockConfig; /* Holds the clock configuration as reported in the GPS Status */
  U8   u_CalibType;        /* Holds the calibration type */
  U8   u_CalibRequestPending; /* used to set when calibration control requested */
  U8   u_CalibDisable;
  U8   u_CalibEnable;
  U8   u_SkipCalib;			/* this flag is used to set calib timer if set as FALSE means calibration required */
  U8   u_TimeInjected; /* used to check the injection of time before position,added for RTI feature */
  U8   u_FreqInjectRequest; /* used to set when frequency estimate are to be injected */
  U8   u_ValidPatch;  /* used to deside the GPS receiver has the valid patch or not */
  U8   u_DummySession; /* used to check the dummy session */
  U8   u_BaudRateConfigured; /* to store the value of configured baud rate */ 
  U8   u_GpsVersionPending;
  U8   u_UpdateNvsRequest;
  U8   u_CountInvalidFix; /* to count the no of invalid fix or meas report to 
                             deside the re acquisition of the assitance */
  U32   q_FCountRecOnFirstAssistToLocReq; /* to account time elapsed between RC_ON to actual location request */
  U32   q_FCountRecOnFirstMeas;
  U32   u_FcountDelta; //diff b/w q_FCountRecOnFirstMeas and q_FCountRecOnFirstAssistToLocReq
  U32  u_DeltaFirstAssistToLocReq;
  U8   u_WaitForClkReport ;	/* waiting for the clk report from the receiver before inject steering */
  U8   u_ClkRptforWlPending ;	/* waiting for the clk report from the receiver before building wishlist */
  U8   u_GpsSessStarted; /* Gps Session Start Status*/
  gpsc_session_status_type    z_SessionStatus;
  gpsc_patch_status_type    z_PatchStatus;
  U32 q_LocStopSessId;
  U8  u_ReturnPending;
  U8		u_FowardMode;
  gpsc_nvm_inj_state z_nvm_inj_state;
  U32	q_RegReadAddress;
  U8  u_firstfix; /* TRUE: first fix has been recieved, FALSE: first fix has not yet been received*/
  /* Variable to keep status of BLF state */
  U16         blf_fix_count;
  U16         blf_fix_count_threshold;
  U16         blf_fix_count_rx;
  U8          blf_sts;
  U8          blf_position_sts;
  U8          blf_velocity_sts;
  U8          blf_dop_sts;
  U8          blf_sv_sts;
#ifdef WIFI_ENABLE
  U8   u_blenderfeedback; /* variable to control the wifi+Gps blender Feed back*/
  U8   u_wifilocrequested; /* to handle single wifi location */
#endif
  U32 u_GpscLocFixStartSystemTime;
	U8  u_StopSessionReqPending;
	U8  u_CalibRequested;
  U32			injectedCepinfoCount;
  U8				injectSvdirectionColdstart;
  U8 	u_GeofenceAPMEnabled;
} gpsc_state_type;


typedef struct
{
  U8       u_Mode;
  U32      q_Fcount;
  U8       u_ForceFlag;
  me_Time  z_meTime;
} gpsc_inject_time_est_type;  /* AI2 Command Packet 10: InjectTimeEst */

#if 0
/* Definition moved to navc_api.h */
typedef struct
{
  S32      l_FreqBiasRaw;
  U32      q_FreqUncRaw;
  U8       u_ForceFlag;
} gpsc_inject_freq_est_type;  /* AI2 Command Packet 32: InjectFreqEst */
#endif

typedef struct
{
  U32  q_FCount;  /* FCount on Sensor */
  U16  w_GpsWeek;
  U32  q_GpsMs;
  FLT  f_TimeBias;
  FLT  f_TimeUnc;
  S8   b_UTCDiff;
  FLT  f_FreqBias;
  FLT  f_FreqUnc;

  U8   u_Valid;
  U8   u_NumInvalids; // Number of invalid times we get from AI_REP_CUSTOM_PACKET
} gpsc_db_gps_clk_type;  /* GPS clock database */


typedef struct
{
  S32  l_Lat;        /* LSB: Pi/2^32; Range: -Pi/2 to Pi/2 */
  S32  l_Long;       /* LSB: Pi/2^31; Range: -Pi to Pi */
  S16  x_Ht;         /* LSB: 0.5 meter */
  U32  q_Unc;        /* LSB: 0.1 meter */
  U16  w_GpsWeek;
  U32  q_GpsMsec;
  U8   u_Mode;       /* 0: use time in this command;
                        1: use current receiver time */
  U8   u_ForceFlag;  /* 0: don't force; 1: force; */
} gpsc_inject_pos_est_type;  /* AI2 CommandPacket 12: InjectPositionEstimate */

typedef struct
{
  S16  x_Altitude;			/* 0.5 meters per bit; Range: -16384 to 16383.5 meters */
  U16  w_AltitudeUnc;		/* 0.5 meters per bit; Range: 0 to 32767.5 meters */
  U8   u_ForceFlag;  		/* 0: don't force; 1: force; */
} gpsc_inject_altitude_type;  /* AI2 CommandPacket 12: AI_INJ_ALTITUDE */

typedef struct
{
  U32  q_RefFCount;           /* Receiver's ms counter value. */
  U16  w_PositionFlags;       /* Flags as defined above. */
  S32  l_Lat;                 /* Latitude: LSB = Pi/2^32, Range -Pi/2 to Pi/2 */
  S32  l_Long;                /* Longitude: LSB = Pi/2^31, Range -Pi to Pi */
  S16  x_Height;              /* Height: LSB = 0.5 meter */
  S16  x_HeightMSL;			  /* MSL Height LSB = 0.5 meter */
  U16  w_EastUnc;             /* East Uncertainty: LSB = 0.1 meter */
  U16  w_NorthUnc;            /* North Uncertainty: LSB = 0.1 meter */
  U16  w_VerticalUnc;         /* Vertical Uncertainty: LSB = 0.5 meter */

  S16  x_VelEast;             /* East Velocity */
  S16  x_VelNorth;            /* North Velocity */
  S16  x_VelVert;             /* Vertical Velocity */
  U16  w_VelUnc;			  /* Velocity Uncertainity */
  U8  u_PDOP;                 /* Position Dilution of Precision (unitless). */
  U8  u_HDOP;                 /* Horizontal DOP (unitless) */
  U8  u_VDOP;                 /* Vertial DOP (unitless) */
  U16 w_HeadTrue;			  /* Heading, True*/
  U16 w_HeadMagnet;			  /* Heading, Magnetic*/
  U16 w_LocAngleUnc;		  /* Location Angle Uncertainty (Axis)*/
  U16 w_LocUncOnAxis;		  /* Location Uncertainty, on Axis*/
  U16 w_LocUncPerAxis;		  /* Location Uncertainty, Perpendicular to Axis*/
  U8  u_UtcHours;			  /* UTC Hours */
  U8  u_UtcMins;			  /* UTC Minutes */
  U8  u_UtcSec;				  /* UTC Seconds */
  U8  u_UtcTenthSec;		  /* UTC Tenths of Seconds */
  U32 q_Rsvd1;				  /* Reserved 1 */
  U32 q_Rsvd2;				  /* Reserved 2 */
  U32 q_Rsvd3;				  /* Reserved 3 */
  U8  u_nSVs;                 /* Number of SVs used in the position fix. */
  U8  u_SVs[ N_LCHAN ];       /* Satellites used in position fix. */
  U8  u_IODE[N_LCHAN];        /* IODEs used in fix. */
  U16  w_Residuals[N_LCHAN];  /* Measurement residuals. */
  U16  w_SvWeights[N_LCHAN];  /* Weights used in fix. */
  U32 u_timeofFix;  		  /* Gps time of Fix in seconds */

} gpsc_report_pos_type;  /* AI2 ReportPacket 6: ReportPosition  */


typedef struct
{
  gpsc_report_pos_type  z_ReportPos;  /* Position database contains the entire
                                        PositionReport packet from the Sensor */
  me_Time	z_PosTime;

  DBL        d_ToaSec; /* the position's RTC time stamp */
} gpsc_db_pos_type;  /* Position database structure */


typedef struct
{
  S32  l_PosLat;
  S32  l_PosLong;
  S16  x_PosHt;
  FLT  f_PosUnc;
  DBL  d_ToaSec;  /* RTC Timestamp, in seconds */
} gpsc_nv_pos_type;  /* Non-volatile memory position struture */

typedef struct
{
   McpU32		   FCount;
   McpU32            status; /* bit 0-24 correspond to  status of the region 1 to 25 bit value : 1 geo fence voilation has occured, bit value 0 : user is within geofence*/
   McpU32           geofence_session_id;
} gpsc_geofence_status;

typedef struct
{
  U8             u_Valid;
  pe_RawAlmanac  z_RawAlmanac;  /* note that pe_RawAlmanac contains
                                   d_UpdateTime (accumulative GPS
                                   seconds). */
} gpsc_db_alm_type;  /* Almanac Database Structure */


typedef struct
{
  U8   u_SvId;  /* 0 to indicate unknown status */
  U8   u_Toa;
  U16  w_AlmWeek;
} gpsc_db_alm_stat_type;  /* almanac status structure */


typedef struct
{
  U8               u_Valid;
  pe_RawSF1Param   z_RawSF1Param;
  pe_RawEphemeris  z_RawEphemeris;
  U16              w_EphWeek;       /* note DBAlm doesn't need this one
                                       because RawAlmanac contains week */
} gpsc_db_eph_type;  /* Ephemeris Database Structure */


typedef struct
{
  U8   u_SvId;
  U8   u_Iode;
  U16  w_Toe;
  U16  w_EphWeek;
} gpsc_db_eph_stat_type;  /* eph. status structure */

typedef struct
{
  pe_RawSF1Param*   p_zRawSF1Param;
  pe_RawEphemeris*  p_zRawEphemeris;
  U16               w_EphWeek;
} gpsc_inject_eph_type;

typedef struct
{
  me_SvDirection  z_RawSvDirection[N_SV];
  U8              u_num_of_sv; /* number of SVs with known direction */

} gpsc_db_sv_dir_type;


typedef struct
{
	U8 u_SvId;
	S8 b_Elev; /* -90 to 90 degrees, LSB = 180/256 degrees */
	U8 u_Azim; /* 0 to 360 degrees, LSB = 360/256 degrees */
}gpsc_inj_sv_dir;


typedef struct
{
  U8  u_Alpha0;
  U8  u_Alpha1;
  U8  u_Alpha2;
  U8  u_Alpha3;
  U8  u_Beta0;
  U8  u_Beta1;
  U8  u_Beta2;
  U8  u_Beta3;
} gpsc_raw_iono_type;  /* Raw IONO structure: Note in PE, both IONO and UTC
                         are in pe_RawIonoUtc */


typedef struct
{
  gpsc_raw_iono_type  z_RawIono;
  DBL                d_UpdateTimeSec;
                       /* in seconds, -1 for invalid. As the Sensor doesn't
                          give time stamp, this is accumulative GPS
                          second when LM gets it. */
  U8   v_iono_valid;   /* 0: invalid, 1: valid, holds the validity of iono data */
} gpsc_db_iono_type;  /* Iono Database Structure */

typedef struct
{
  S32                       utc_a1;                   /*<  0:  4> ICD-200                                            */
  S32                       utc_a0;                   /*<  4:  4> ICD-200                                            */
  U8                        utc_tot;                  /*<  8:  1> ICD-200                                            */
  U8                        utc_wnt;                  /*<  9:  1> ICD-200                                            */
  S8                        utc_delta_tls;            /*< 10:  1> ICD-200                                            */
  U8                        utc_wnlsf;                /*< 11:  1> ICD-200                                            */
  S8                        utc_dn;                   /*< 12:  1> ICD-200                                            */
  S8                        utc_delta_tlsf;           /*< 13:  1> ICD-200                                            */
  U8                        zzz_align0;               /*< 14:  1> alignment                                          */
  U8                        zzz_align1;               /*< 15:  1> alignment                                          */
} gpsc_raw_utc_type;  /* Raw utc structure:*/


typedef struct
{
  gpsc_raw_utc_type  z_RawUtc;
  U8		u_valid;
  DBL                d_UpdateTimeSec; // need this to validate UTC data later
  							/* in seconds, -1 for invalid. As the Sensor doesn't
                            give time stamp, this is accumulative GPS
                          	second when LM gets it. */
} gpsc_db_utc_type;  /* Iono Database Structure */

typedef struct
{
  U8                u_svid;
  U8                u_iode;
  U8                u_dgps_udre;
  S32               x_dgps_pseudo_range_cor;
  S16               b_dgps_range_rate_cor;
  S8			    b_dgps_deltapseudo_range_cor2;
  S8			    b_dgps_deltarange_rate_cor2;
  S8			    b_dgps_deltapseudo_range_cor3;
  S8		        b_dgps_deltarange_rate_cor3;

} dgps_assist;              /* Added for DGPS support 26/07/2006-Raghavendra*/

typedef struct
{
  U32               q_gps_tow;
  U8			    u_dgps_status;
  U8			    u_dgps_Nsat;
  dgps_assist       z_dgps[32];
} gpsc_dgps_assist_type;    /* Added for DGPS support 26/07/2006-Raghavendra*/


typedef struct
{
  U8   u_AlmHealth[N_SV];
  DBL  d_UpdateTimeSec;
} gpsc_db_health_type;



typedef struct
{
  U8  u_SvIdSvTimeFlag;  /* Bits 0-5: ID; (ID = 0 indicating no valid data
                            Bits 6 and 7:
                              Time flag -
                              0: no time known;
                              1: sub mesc known;
                              2: sub-bit and sub-ms known;
                              3: msec, sub-bit and sub-ms known;
                          */

  U16  w_Snr;            /* LSB = 0.1dB */
  U16  w_Cno;            /* LSB = 0.1dB */
  S16  x_LatencyMs;      /* LSB = 1 ms */
  U8   u_PreInt;         /* LSB = 1 ms */
  U16  w_PostInt;        /* LSB = 1 ms */
  U32  q_Msec;           /* LSB = 1 ms */
  U32  q_SubMsec;        /* LSB = 1/2^24 */
  U16  w_SvTimeUnc;      /* 11-5 mantissa-exponent, unit 1 ns */
  S32  l_SvSpeed;        /* LSB = 0.01 m/s */
  U32  q_SvSpeedUnc;     /* LSB = 0.01 m/s */
  U16  w_MeasStatus;     /* Bit 0 - sub-msec valid
                            Bit 1 - sub-bit time known
                            Bit 2 - sv time known
                            Bit 3 - Bit edge confirmed from signal
                            Bit 4 - measured velocity
                            Bit 5 - fine / coarse velocity
                            Bit 6 - channel has continuous lock
                            Bit 7 - rsvd
                            Bit 8 - rsvd
                            Bit 9 - Last update from difference, not meas
                            Bit 10 - week SV but can't do define XCORR test
                            Bit 11 - strong indication of croos-correlation
                            Bit 12 - had multipath indicators
                            Bit 13 - Don't use measurement
							Bit 14 - Multipath level bit-0
							Bit 15 - Multipath level bit-1
                          */
  /* added for NL5500 */
  U8   u_ChannelMeasState;    /*Channel Measurement State*/
 
  S32  q_AccCarrierPhase;  /*Accumulated Carrier Phase*/

  S32  q_CarrierVelocity;  /*Carrier Velocity*/

  S16  w_CarrierAcc;       /*Carrier Acceleration*/

  U8   u_LossLockInd;      /*Loss of lock indicator*/

  U8   u_GoodObvCount;     /* Good Observations Count */

  U8   u_TotalObvCount;    /* Total Observation Count */

  U16  w_Reserved;
} gpsc_meas_per_sv_type;


typedef struct
{
  U8         u_Valid;
  U32        q_FCount;

  me_Time    z_MeasToa;  /* meas. TimeOfApplicability: usually same as
                            clock database, adjusted by time-alignment if
                            called */
  U16		 w_SvBlockLen;  /*SV Block Length*/
  DBL		d_PosUnc;
  U32 		q_GoodStatSvIdBitMap; /* bit map to hold the list of good SVs */
  U8 		u_IsValidMeas; /* indicates if the measurement quality check is passed based on all parameters and
                              measurements indicated in q_GoodStatSvIdBitMap is valid to be sent out to the server */

  gpsc_meas_per_sv_type  z_MeasPerSv[N_LCHAN];
} gpsc_meas_report_type;


typedef struct
{
  U8   u_Sv;  /* u_Sv being zero indicating no valid data */
  S16  x_LatencyMs;
  U16  w_CnoDB;
  U8   u_ChanState;
  U8   u_GoodObs;
  U8   u_Observe;
  U16  w_MeasStatus;
} gpsc_meas_stat_per_sv_type;

typedef struct
{
  U8             u_Valid;
  U32            q_FCount;
  U32            q_GoodSvs;  /* bit map: not given by AI2, but by
                                gpsc_meas_qualchk() */
  gpsc_meas_stat_per_sv_type  z_MeasStatPerSv[N_LCHAN];
} gpsc_meas_stat_report_type;


typedef struct
{
  U8  u_Sv;
  U8  u_Observe;
  U8  u_GoodObs;
} gpsc_last_meas_obsvd_type;


typedef struct
{
  gpsc_meas_report_type       z_MeasReport;
  gpsc_meas_stat_report_type  z_MeasStatReport;
  gpsc_last_meas_obsvd_type   z_LastMeasObsvd[N_LCHAN];
  U8                         u_init_last; /* 0: dont need init; 1:need init */
  
} gpsc_db_gps_meas_type;


typedef struct
{
  U8             u_SvId;
  me_SvSteering  z_SvSteer;
} gpsc_sv_id_steer_type;

typedef struct
{
  U8                    u_Valid;
  U32                   q_GpsTow;
  gpsc_sv_id_steer_type  z_SvIdSteer[N_LCHAN];
} gpsc_db_sv_steer_type; /* only used for SmartServer time alignment */

#ifdef WIFI_ENABLE
typedef struct
{

    U8   u_Valid;
	DBL  d_WifiLatitude;
	DBL	 d_WifiLongitude;
	DBL	 d_WifiAltitude; //need to remove
	DBL	 d_WifiUnc;     //need to remove
	U32  q_RefFCount;           /* GPS Receiver's ms counter value, when wifi position received. */
    DBL  d_VerticalUnc;  // Need to remove
	DBL  d_WiFiEastUnc;
	DBL  d_WiFiNorthUnc;
	DBL  d_WiFiEastVel;
	DBL  d_WiFiNorthVel;
	DBL  d_WiFiEastVelUnc;
	DBL  d_WiFiNorthVelUnc;
	McpU8   u_NumOfApUsed;  /* number of APs used for WPC positioning */
    McpU32  q_timeOfWifiPosition; /* time tag when the fix obtained */
	McpU8   FixPropagated;  /* Indicate 0: Actual fix  1: Propagated */
    McpU8  FixFromKalmanFilter; /* Indicate 0: Centroid Fix 1: KalmanFilter*/
	McpU32 q_sytemtimeinmsec;
	
	McpU8	 KalmanEnable;	 /* 0: Kalman is disabled  1: Kalman is enabled*/ //new
	McpU8   VBWSetting ;	   /* 0: Low bandwidth Setting	1: High bandwidth Setting*/ //new
	McpFLT  AvgNumOfAPsUsed; /*Average number of AP's Used*/	//new
	McpDBL  Filtered_Scale_Factor;  //new
}
gpsc_db_wifi_pos_type;

typedef struct
{

    DBL  d_BlendLatitude;
	DBL	 d_BlendLongitude;
	DBL	 d_BlendAltitude;
	DBL	 d_BlendUnc;     // need to remove
	DBL	 d_BlendEastUnc;
	DBL	 d_BlendNorthUnc;
	DBL  d_VerticalUnc;
	U8   u_2Dor3Dfix;    /* 1-3D fix, 0-2Dfix*/

	/* Below one are for Future use */
//  DBL	 d_BlendEastVel;
//  DBL	 d_BlendNorthVel;
//	DBL  d_VerticalVel;

//  DBL	 d_BlendEastVelUnc;
//  DBL	 d_BlendNorthVelUnc;
//	DBL  d_VerticalVelUnc;
}
gpsc_db_blended_pos_type;

#endif // #ifdef WIFI_ENABLE

typedef struct
{
	 U32 q_RefFCount ;			/* Fcount									*/
	 U16 w_StatusFlags; 		/* Flags									*/
								/*Bit Status								*/
								/*0 2D Fix									*/
								/*1 3D Fix									*/
								/*3 Velocity Fix							*/
								/*4-15 Reserved - Set to 0					*/

	 FLT f_Latitude;			/* KF Latitude								*/
	 FLT f_Longitude;			/* KF Longitude 							*/
	 FLT f_Altitude;			/* KF Altitude								*/
	 FLT f_EastUncertainty; 	/* KF East Uncertainty						*/
	 FLT f_NorthUncertainty;	/* KF North Uncertainty 					*/
	 FLT f_VerticalUncertainty; /* KF Vertical Uncertainty					*/
	 FLT f_EastVelocity;		/* WLS East Velocity						*/
	 FLT f_NorthVelocity;		/* WLS North Velocity						*/
	 FLT f_VerticalVelocity;	/* WLS Vertical Velocity					*/
	 FLT f_VelEastUnc;			/* WLS Velocity East Uncertainty			*/
	 FLT f_VelNorthUnc; 		/* WLS Velocity North Uncertainty			*/
	 FLT f_VelVerticalUnc;		/* WLS Velocity Vertical Uncertainty		*/
	 U8  u_NumSvPosFix; 		/* KF:Number of SVs used in Position Fix	*/
	 U8  u_NumSvVelFix; 		/* KF: Number of SVs used inVelocity Fix	*/
	 U8  u_NumSvPosFixPreRaim;	/* KF:Number of SVs used in Position Fix	*/
	 U8  u_NumSvVelFixPreRaim;	/* KF: Number of SVs used inVelocity Fix	*/
	 U8  u_MpLevelIndicator;	/* KF: MP Level Indicator					*/
	 U8  u_MaxPosError; 		/* WLS Max Pos Error						*/
	 U8  u_MaxVelError; 		/* WLS Max Velocity Error					*/
	 U8  Reserved[10];			/* Reserved 				*/
}gpsc_db_sensor_calib_report_type;


typedef struct
{
	U32 toe_lower_bound_min;	/* Min TOE considering (TOE - 2 hour) validity */
	U32 toe_lower_bound_max;	/* To handle wrap around cases */
	U32 toe_upper_bound_min;	/* To handle wrap around cases */
	U32 toe_upper_bound_max;	/* Max TOE considering (TOE + 2 hour) validity */
} gpsc_toe_validity_bounds;


typedef struct
{
  U32  q_FCount;  /* FCount on Sensor */
  FLT  f_SubMs;
  U8   u_Valid;
}gpsc_db_hw_event_type;

typedef struct
{
  S32      l_FreqBiasRaw;
  U32      q_FreqUncRaw;
  U8		u_Valid;
}
gpsc_db_gps_inj_clk_type;

typedef struct
{
  gpsc_db_alm_type       z_DBAlmanac[N_SV];
  gpsc_db_eph_type       z_DBEphemeris[N_SV];

  gpsc_db_sv_dir_type    z_DBSvDirection;

  gpsc_db_alm_stat_type  z_DBAlmStat[N_SV];
  gpsc_db_eph_stat_type  z_DBEphStat[N_SV];
  gpsc_db_iono_type      z_DBIono;
  gpsc_db_utc_type      z_DBUtc; /* Database already has UTC */

  gpsc_db_health_type    z_DBHealth;
  gpsc_db_gps_clk_type   z_DBGpsClock;
  gpsc_db_gps_inj_clk_type	z_DBGpsInjClock;	/* Injected clock parameters */
  gpsc_db_hw_event_type  z_DBHWEvent;
  gpsc_db_pos_type       z_DBPos;
#ifdef WIFI_ENABLE
  gpsc_db_wifi_pos_type  z_DBWifiPos;
  gpsc_db_blended_pos_type z_DBBlendedPos;
#endif
  gpsc_db_sensor_calib_report_type z_DBSensorCalibReport;

  gpsc_db_gps_meas_type  z_DBGpsMeas;
  gpsc_db_sv_steer_type  z_DBSvSteer;

  gpsc_nv_pos_type       z_NvsPos; /* an image of the current NVS pos. rec. */
  gpsc_geofence_status z_GeofenceStatus;
  U8					 u_EphCounter;
  U8					 u_AlmCounter;
  U32					 q_InjEphList;
  U32					 q_InjAlmList;
} gpsc_db_type;



typedef struct
{
  U8   u_PortNum;
  U32  q_PeriodicReportFlags;
  U32  q_AsyncEventFlags;
  U16  w_PeriodicReportRate;
  U8   u_SubSecPeriodicReportRate;
  U8   u_Ext_reports;          /* Flag to denote that extended reports are enabled */
} gpsc_event_cfg_type;


typedef struct
{
  U8   u_SvId;
  U16  w_TlmWord;
  U8   u_AntiSpoofFlag;
  U8   u_AlertFlag;
  U8   u_TlmRsvBits;
} gpsc_raw_tow_type;


typedef struct
{
  gpsc_inject_time_est_type  z_InjectGpsTimeEst;
} gpsc_smlc_gps_time_type;



typedef struct
{
  U8   u_ReservedBits;  /* Argument in 2 low-order bits.  Default value 00 */
  U8   u_FlagBits;      /* Argument in 2 low-order bits.  Default value 01 */
  U16  w_TlmMessage;    /* Argument in 14 low-order bits.  Default value 0 */
  U32  q_SubFrameOfWeek;    /* Argument.  Range 0  to WEEK_ZCOUNTS-1 */
  U32  q_BitOfWeek;         /* Result. BOW of the first predicted bit. This
                               BOW is in the preceeding week when
                               q_SubFrameOfWeek == 0 */
  U32  q_PredictedBits[2];  /* 62 predicted bits, left justified */
} gpsc_tlm_how_type;


typedef struct
{
  U8   u_SvId;
  U8   u_NumBits;           /* Out of the maximum 64 bits, num. of bits
                               used in the PredictedBits[]. */
  U32  q_BitOfWeek;         /* Result. BOW of the first predicted bit. This
                               BOW is in the preceeding week when
                               q_SubFrameOfWeek == 0 */
  U32  q_PredictedBits[2];  /* Sparse Pattern match uses 62 predicted bits,
                               left justified */

} gpsc_pred_db_type;  /* predicted data bits */


typedef struct
{
  U8  u_SvId;          /* 1-64; 0 used to indicate invalid */
  S16 x_Doppler0;      /* 12 bit data, sign extended. 0'th order doppler:
                          -2048 to 2047, LSB for 2.5Hz */
  U8  u_Doppler1;      /* 6 bit data. 1'st order doppler: 0-63, representing
                          (-1 to 0.5 Hz/sec ). Optional (0xFF indicates not
                          present)  */
  U8  u_DopplerUnc;    /* 3 bit data. representing doppler unc of
                          [ 2**(-u_DopplerUnc) * 200 ].   Optional (0xFF
                          indicates not present)  */
  U16 w_CodePhase;     /* 10 bit data */
  U8  u_IntCodePhase;  /* 5 bit data */
  U8  u_GpsBitNum;     /* 2 bit data */
  U8  u_SrchWin;       /* 4 bit data, Code phase search window */
  U32              q_GpsTow;  /* GPS TimeOfWeek: in msec */
} gpsc_acq_element_type;  /* structure to keep raw RRLP acquisition assistance's
                            AquisElmenet data, except that all elements are in
                            2's complement */

typedef struct
{
  U8  u_Azimuth;    /* 5 bit data. Resolution = 11.25 degrees.  */
  U8  u_Elevation;  /* 3 bit data, Resolution = 11.25 degrees. 0xFF to
                       indicate invalid */
} gpsc_additional_angle_type;  /* structure to keep raw RRLP acquisition
                                 assistance's AdditionalAngel data */


#define C_MAX_SVS_ACQ_ASSIST 16
typedef struct
{

  U8               u_NumSv;   /* Number of SVs in the RRLP message */
  gpsc_acq_element_type       z_AcquisElement[C_MAX_SVS_ACQ_ASSIST];
                              /* max. 16 SVs in the message. Init. all
                                 AcquisElmenet->u_SvId = 0, and fill from
                                 z_AcquisElement[0] */
  gpsc_additional_angle_type  z_AddAngle[C_MAX_SVS_ACQ_ASSIST];
                              /* max. 16 SVs in the message. Init. all
                                 AdditionalAngel->u_Elevation = 0xFF, and
                                 fill from z_AddAngel[0]; */
   gpsc_db_gps_clk_type 	p_zAAGpsClock; /* used as a ref clock for propogation of AA SVs*/
} gpsc_acq_assist_type;

typedef struct
{
	U8 u_Prn;
	McpDBL xyz_x, xyz_y, xyz_z;
}gpsc_SV_xyz_type;


typedef struct
{
  U8                        u_QopFlag;                 /*<  0:  1> Not used presently,Quality of position flag.This flag indicates if a set of attributes associated with a request for the geographic position of SET is made. */
  U16                        u_HorizontalAccuracy;      /*<  2:  2> 0xFF if not set,Horizontal Accuracy. This describes the uncertainty for latitude and longitude */
  U16                       w_VerticalAccuracy;        /*<  4:  2> Not used presently,Vertical Accuracy.This describes the uncertainity in the altitude. */
  U16                       w_MaxResponseTime;         /*<  6:  2> Maximum Location age. Units in seconds from 0 to 65535.if not set the value as 0xFFFF */
  U8                        u_Delay;                  /*<  8:  1> Not used presently,Delay N. 2^N, N from (0..7), unit is seconds       */
  U8                        u_TimeoutCepInjected;     /* to inject the timeout and cep info only if there is a changes */
  U8                        u_MaxLocationAge;     /* Location Age..Sent by Network in Qop field */
  
                                  
} gpsc_loc_fix_qop;




typedef struct
{

  U8                       u_NumAssistAttempt;  /* increment by 1 every time an
                                                   attempt to request assistance
                                                   from SMLC is made for a same
                                                   request */

  U16                      w_AvailabilityFlags;  /* Bitmap: 1 - avaialable;
                                                            0: not avalaible
                                                    Bit 0 up: TimeTow, Position,
                                                    Ephemeris, Almanac, Iono */

  U16                      w_InjectedFlags;       /* Bitmap: 1 - injected;
                                                            0: not avalaible
                                                    Bit 0 up: TimeTow, Position,
                                                    Ephemeris, Almanac, Iono */

  U8                       u_MoreToCome;         /* FALSE: No more SMLC data on
                                                    the way; TRUE: More SMLC
                                                    data on the way */

   U8					   u_RequestedAssistTime;  /* FALSE: NO GPSC_ASSIST_TIME_REP requested
												     TRUE: GPSC_ASSIST_TIME_REP requested.
												  */
  U8				u_AssistPendingFlag; /* FALSE: no assistance pending
  										TRUE: new assistance was received during the assist injection seq*/

  gpsc_smlc_gps_time_type   z_SmlcGpsTime;        /* time is injected as soon as
                                                    received from SMLC */

  gpsc_raw_tow_type         z_SmlcTow[N_SV];

  gpsc_inject_pos_est_type  z_InjectPosEst;
  gpsc_inject_time_est_type z_InjectTimeEst;

  pe_RawEphemeris          z_SmlcRawEph[N_SV];
  pe_RawSF1Param           z_SmlcRawSF1Param[N_SV];

  U8				u_nSvEph; /* counter for keeping track of the number of SV EPH injected */
  U8				u_InjVisible; /* Flag for enabling of injection for visible SVx only */
  U32			q_VisiableBitmap; /* Bitmap of visible SV */
  U32			q_InjSvEph; 	/* Bitmap for keeping track of injected Eph for SVs*/
  
  gpsc_SV_xyz_type	z_SVxyz[N_SV]; /*XYZ position array of SVs*/

  
  U16                      w_EphWeek;            /* since the "raw" structures
                                                    for eph. don't have a simple
                                                    week number, use this to
                                                    avoid having to convert from
                                                    FullToe */

  pe_RawAlmanac            z_SmlcRawAlmanac[N_SV];

  gpsc_raw_iono_type       z_SmlcRawIono;

  gpsc_acq_assist_type     z_AcquisAssist;

  gpsc_dgps_assist_type    z_DgpsAssist;  /* added for DGPS support 26/07/2006-Raghavendra*/

  U32                      q_RealTimeIntegrity[2];
                                                 /* bit map: bit 0 not used;
                                                    bit 1: SVId 1 not healthy;
                                                    bit 2: SVId 2 not healthy;
                                                    ...
                                                 */
  U32						q_AlmPresent;
  U8						u_SvCounter;
  U8						u_SvCounterSteer;
  U16						w_CurrentWeek;
  U32						q_CurrentMsec;
  U8						u_NumSvToInj;
  U8						u_HighestSvIndex[N_LCHAN];
							/* this keeps the Indecies of the hightest
							N_LCHAN worth of SVs in the acquis.
						   assistance data, if SMLC has provide
						   more than N_LCHAN Svs, u_HighestSvIndex[0]
						   contains the Index of the highest sv from steering
							table*/
  U16					   w_NDiffs;
  U16					   w_BestRef;
  FLT						   f_MinUnc;
  U8						u_RefSv[N_LCHAN];
  gpsc_loc_fix_qop          z_Qop;  /* for populate accuracy and time out params */
  me_SvSteering				z_RefSvSteer[N_LCHAN];

  /**** Added at the end to take prevent any logic that might already be existing
   **** that might be addressing the elements of structure threough offset
   **** on the structure ***/

    gpsc_raw_utc_type			z_SmlcRawUtc;

    McpS32						injected_almanac;
} gpsc_smlc_assist_type;  /* this keeps a copy of assistance data from SMLC */


typedef struct
{

  U8   u_SlaveModeEnabled;
  U8   u_DgpsCorrAllowed;
  U8   u_DgpsMaxInterval;
  U8   u_ServerMode;
  U8   u_PatternMatchType;
  U8   u_FdicMode;
  U8   u_AltHoldMode;
  U16  w_MaxClkFrqUnc;
  U16  w_MaxClkAcc;
  U16  w_MaxUserAcc;
  U8   u_ElevMask;
  U8   u_PdopMask;
  U8   u_FeatureControl;
  U8   u_2DPdopMask;
  U8   u_ExtEventEdge;
  U8   u_Test0;
  U8   u_Test1;
  U8   u_Test2;
  U8   u_DiagSetting;  /* Bits 0,1: Diag Port;
                          Bits 2,3: ME diag level;
                          Bits 4,5: PE diag level;
                          Bits 6,7: Not used */
  U8   u_DiagSetting_1;/* Bits 0-3: Speed Optimization
                          Bits 4-7: Not used */
  U8   u_Rsvd;
} gpsc_rcvr_cfg_type;  /* 24 bytes total */

typedef struct
{
  U8 b_PosUncertainty_Defined;
  U8 b_PosOffset_Defined;
  U8 b_TestByte_Defined[3];
  U8 b_SendSessionStartStopTestMsgs;
  FLT f_PosUncertainty;
  FLT f_PosOffset;
  U16 u_TestByte[3];
} gpsc_parms;


#define C_DL_RX_BUF_LEN 2048

#define C_DL_TX_BUF_LEN 2048

#define MAX_CHUNK_DATA 1021

typedef struct
{
	U8 u_Buff[C_DL_RX_BUF_LEN];

	Ai2Rx z_RxDesc;
	Ai2Field z_Ai2RxField;
} gpsc_comm_rx_buff_type;

typedef struct
{
	U8 u_Buff[C_DL_TX_BUF_LEN];

	Ai2Tx z_TxDesc;
	U8 u_FirstPacket;
	U8 u_AI2AckPending; /* Is AI2 ack pending, TRUE/FALSE*/
	U8 u_DataPending;   /* Is there data in buffer?*/
	U8 u_DataPendingAck;/* Ack for pending data*/
	U8 u_AddAckShared; /* Adding Acknowledgement data in Shared transport. */
} gpsc_comm_tx_buff_type;


typedef struct
{
	U8	u_NmeaStringCount;
	S8*	p_sNmeaPointer;
	S8	s_ResultBuffer[RESULT_BUFFER_LENGTH];
}gpsc_result_type;

typedef struct
{
  U16					   w_test_request;             /*Bit set request */
  U16                       w_start_delay;               /* */
  S32                       l_wideband_center_freq;      /*               */
  S32                       l_narrowband_center_freq;           /*               */
  U8                        u_wideband_peaks;         /* */
  U8						u_wideband_adj_samples;
  U8						u_narrowband_peaks;
  U8						u_narrowband_adj_samples;
} gpsc_cw_test_params;

typedef struct
{
  U16                       write_value;
  U16                       write_mask;
  U16                       status_mask;
} gpsc_gpio_test_params;

typedef struct 
{
  gpsc_feature_flag 	feature_flag;
  McpU32				intRegNoBitmap;
  McpU8				internalRegionNumber;

}gpsc_geo_fence_control;




typedef TNAVC_plt gpsc_productline_test_params;
typedef TNAVC_MotionMask_Status gpsc_motion_mask_status;
typedef TNAVC_GetMotionMask gpsc_motion_mask_setting;
typedef TNAVC_SetMotionMask gpsc_set_motion_mask;

typedef struct
{
	U8  w_MajorVerNum;
	U8  w_MinorVerNum;
	U8  u_SubMinor1VerNum;
	U8  u_SubMinor2VerNum;
	U8  u_Day;
	U8  u_Month;
	U16 w_Year;
} gpsc_version;

typedef struct
{
	U8  u_ChipIDMajorVerNum;
	U8  u_ChipIDMinorVerNum;
	U8  u_SysConfig;
	U8  u_ROMIDMajorVerNum;
	U8  u_ROMIDMinorVerNum;
	U8  u_ROMIDSubMinor1VerNum;
	U8  u_ROMIDSubMinor2VerNum;
	U8  u_ROMDay;
	U8  u_ROMMonth;
	U16 w_ROMYear;
	U8  u_PATCHMajor;
	U8  u_PATCHMinor;
	U8  u_PATCHDay;
	U8  u_PATCHMonth;
	U16 w_PATCHYear;
} gps_version;

typedef struct
{
  U8   u_SvSteerId;
//  U32  q_FCount;    /* FCount when structure is valid */
//  FLT  f_SubMs;    /* Range of 0 thru 0.99999 [msecs] */
//  U32  q_Ms;      /* Range of 0 thru (WEEK_MSECS-1) [msecs] */
//  FLT  f_SvTimeUncMs;  /* 1 sided max time uncertainty [msecs] */
//  FLT  f_SvSpeed;    /* Pseudo range speed [m/s] */
//  FLT  f_SvSpeedUnc;  /* 1 sided max speed uncertainty [m/s] */
//  FLT f_SvAccel;    /* Pseudo range acceleration [m/s^2] */
}gpsc_steer_inject_per_sv_type;

typedef struct 
{
	U8 u_SvSteerInjectCount;
	gpsc_steer_inject_per_sv_type p_zPerSvSteerInject[32];
	
}gpsc_sv_steer_inject_type;

/* System Handlers */
typedef enum  
{
	E_GPSC_EVENT_SHUTDOWN,
	E_GPSC_EVENT_INIT,
	E_GPSC_EVENT_READY_START,
	E_GPSC_EVENT_NVS_INJECTION,
	E_GPSC_EVENT_READY_COMPLETE,
	E_GPSC_EVENT_IDLE,
	E_GPSC_EVENT_GPS_WAKEUP,
	E_GPSC_EVENT_LOCATION_START,
	E_GPSC_EVENT_LOCATION_STOP,
	E_GPSC_EVENT_LOCATION_COMPLETE,
	E_GPSC_EVENT_ASSIST_INJECT,
	E_GPSC_EVENT_APPLY_ASSISTANCE,
	E_GPSC_EVENT_ACTIVE,
	E_GPSC_EVENT_SESSION_SLEEP,
	E_GPSC_EVENT_SESSION_WAKEUP,
	E_GPSC_EVENT_PLT,
	E_GPSC_EVENT_ENTER_FWD,
	E_GPSC_EVENT_EXIT_FWD,
	E_GPSC_EVENT_NUM,
	E_GPSC_EVENT_RESET
} E_GPSC_SM_EVENT;

typedef enum  
{
	E_GPSC_STATE_SHUTDOWN,
	E_GPSC_STATE_IDLE,
	E_GPSC_STATE_SLEEP,
	E_GPSC_STATE_ACTIVE,
	E_GPSC_STATE_FOWARD,
	E_GPSC_STATE_NUM
} E_GPSC_SM_STATE;

/* GPSC State Sequence - u_GPSCStateSeq */
typedef enum
{
  E_GPSC_SEQ_SHUTDOWN,
  E_GPSC_SEQ_INIT,
  E_GPSC_SEQ_READY_START,
  E_GPSC_SEQ_READY_WAIT_FOR_WAKEUP,
  E_GPSC_SEQ_READY_BAUD_CHANGE,
  E_GPSC_SEQ_READY_BEGIN_DOWNLOAD,
  E_GPSC_SEQ_READY_READ_INJECT_RECORD,
  E_GPSC_SEQ_READY_SKIP_DOWNLOAD,
  E_GPSC_SEQ_READY_DOWNLOAD_COMPLETE,
  E_GPSC_SEQ_READY_INJECT_INIT_CONFIG,
  E_GPSC_SEQ_READY_GPS_INJECT_NVS,
  E_GPSC_SEQ_READY_GPS_INJECT_NVS_COMPLETE,
  E_GPSC_SEQ_READY_GPS_WAIT_IDLE,
  E_GPSC_SEQ_READY_GPS_WAIT_SLEEP,
  E_GPSC_SEQ_READY_IDLE,
  E_GPSC_SEQ_READY_SLEEP,
  E_GPSC_SEQ_IDLE,
  E_GPSC_SEQ_GPS_SLEEP,
  E_GPSC_SEQ_GPS_WAIT_SLEEP,
  E_GPSC_SEQ_GPS_IDLE,
  E_GPSC_SEQ_WAIT_GPS_WAKEUP, /* E_GPSC_SEQ_GPS_WAKEUP_START */
  E_GPSC_SEQ_GPS_WAKEUP,
  E_GPSC_SEQ_GPS_WAKEUP_REQ_TIME,
  E_GPSC_SEQ_GPS_WAKEUP_INJ_ASSISTANCE,
  E_GPSC_SEQ_GPS_WAKEUP_COMPLETE,
  E_GPSC_SEQ_ACTIVE_APPLY_REF_CLOCK,
  E_GPSC_SEQ_ACTIVE_WAIT_CALIB,
  E_GPSC_SEQ_ACTIVE_INJECT_ASSIST_SLEEP_FIX,
  E_GPSC_SEQ_WAIT_ACTIVE,
  E_GPSC_SEQ_ACTIVE,
  E_GPSC_SEQ_SESSION_REQUEST_TIME,
  E_GPSC_SEQ_SESSION_INJECT_TIME,
  E_GPSC_SEQ_SESSION_INJ_FIRST_TIME_NVS_DATA,
  E_GPSC_SEQ_SESSION_LOC_START,
  E_GPSC_SEQ_SESSION_LOC_STOP,
  E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE,
  E_GPSC_SEQ_SESSION_GET_ASSISTANCE,
  E_GPSC_SEQ_SESSION_WAIT_FOR_LOCATION_REQUEST,
  E_GPSC_SEQ_SESSION_LOCATION_COMPLETE,
  E_GPSC_SEQ_SESSION_CONFIG,
  E_GPSC_SEQ_SESSION_ON,
  E_GPSC_SEQ_SESSION_SLEEP,
  E_GPSC_SEQ_PLT,
  E_GPSC_SEQ_SHUTDOWN_COMPLETE,
  E_GPSC_SEQ_NUM
}E_GPSC_SM_SEQ;

char* GpscSmState(E_GPSC_SM_STATE eGpscState);
char* GpscSmEvent(E_GPSC_SM_EVENT eGpscEvent);
char* GpscSmSequence(E_GPSC_SM_SEQ eGpscSequence);

struct _gpsc_sm;
//typedef FGpscSmStateHnd (void *) ;
typedef E_GPSC_SM_STATE (*FGpscSmStateHnd)(struct _gpsc_sm*, E_GPSC_SM_EVENT); 

typedef struct _gpsc_sm
{
	void* hGpsc;
	E_GPSC_SM_STATE e_GpscCurrState;
	E_GPSC_SM_SEQ	e_GpscCurrSeq;
	E_GPSC_SM_EVENT e_GpscCurrEvent;
	FGpscSmStateHnd stateHnd[E_GPSC_STATE_NUM];
	U8 				u_ErrorStatus;	
} gpsc_sm;

void GpscSm(void* hGpscSm, E_GPSC_SM_EVENT event);


typedef struct
{
	U8 u_GetAssistSUPLRequest;
	U8 u_GetAssistSUPLRequestPending;
	U16 u_SessRequestCount;
} gpsc_supl_status;

typedef struct
{
	McpU8	taskID;
	McpU8	queueID;
}tRegAssistList;

#define EPH_LIST_CNT 1
#define GPSTIME_LIST_CNT 1

typedef struct
{
	tRegAssistList  zEphList[EPH_LIST_CNT];
	McpU8 EPHcount;

	tRegAssistList  zGpsTimeList[GPSTIME_LIST_CNT];
	McpU8	GpsTimeCount;
}gpsc_tasks_reg_for_assist;

/* a structure of this type is defined once and the pointer to this
   structrue is passed to functions accross this task */
typedef struct
{
  gpsc_comm_rx_buff_type*			p_zGPSCCommRxBuff;
  gpsc_comm_tx_buff_type*			p_zGPSCCommTxBuff;

  gpsc_db_type*                   p_zGPSCDatabase;
  gpsc_state_type*                p_zGPSCState;
  gpsc_cfg_type*                  p_zGPSCConfig;
  gpsc_sess_cfg_type*			  p_zSessConfig;
  gpsc_event_cfg_type*            p_zEventCfg;
  gpsc_assist_wishlist_type*      p_zGpsAssisWishlist;
  gpsc_smlc_assist_type*          p_zSmlcAssist;
  gpsc_rcvr_cfg_type*             p_zRcvrConfig;
  gpsc_parms *                    p_zGPSCParms;
  gpsc_result_type*				  p_zResultBuffer;
  gpsc_productline_test_params*   p_zProductlineTest; 
  void*							  p_GPSCMemory;
  gpsc_dyna_feature_cfg_type*	  p_zGPSCDynaFeatureConfig;
  gps_version*					  p_zGPSVersion;
  gpsc_sv_steer_inject_type*	  p_zSvInject;
  gpsc_sys_handlers*			  p_zSysHandlers;
  gpsc_sm*						  p_zGpscSm;
  gpsc_custom_struct*			  p_zCustomStruct;
 gpsc_motion_mask_status*        p_zMotionMaskStatus;
  gpsc_motion_mask_setting*       p_zMotionMaskSetting;
  gpsc_set_motion_mask*             p_zSetMotionMask;
  gpsc_geo_fence_control*		   p_zGeoFenceControl;		
  gpsc_feature_flag	                   feature_flag;
  gpsc_hw_type						hw_type;
McpU32  			regionnumbercount;
  gpsc_supl_status					p_zSuplStatus;
  gpsc_tasks_reg_for_assist			zRegAssist;
  U8 	u_reqAssistPending;
  U8 u_dummy1;
  U8 u_dummy2;
  U8 u_dummy3;
} gpsc_ctrl_type;



typedef struct
{
 gpsc_version					zGPSCVersion;
 gpsc_ctrl_type                 z_GPSCControl;
 gpsc_db_type                   z_GPSCDatabase;
 gpsc_state_type                z_GPSCState;
 gpsc_cfg_type                  z_GPSCConfig;
 gpsc_sess_cfg_type				z_SessConfig;
 gpsc_event_cfg_type            z_EventCfg;
 gpsc_assist_wishlist_type		z_GpsAssisWishlist;
 gpsc_smlc_assist_type          z_SmlcAssist;
 gpsc_rcvr_cfg_type             z_RcvrConfig;
 gpsc_parms						z_GPSCParms;
 gpsc_comm_rx_buff_type			z_GPSCCommRxBuff;
 gpsc_comm_tx_buff_type			z_GPSCCommTxBuff;
 gpsc_result_type				z_ResultBuffer;
 gpsc_productline_test_params   z_ProductlineTest;
 gpsc_dyna_feature_cfg_type		z_GPSCDynaFeatureConfig;
 gps_version					z_GPSVersion;					
 gpsc_sys_handlers				z_SysHandlers;
 gpsc_sv_steer_inject_type		z_SvInject;
 gpsc_custom_struct				z_CustomStruct;
gpsc_motion_mask_setting		z_motion_mask_setting;
 gpsc_motion_mask_status		z_motion_mask_status;
 gpsc_set_motion_mask			z_set_motion_mask;
 gpsc_geo_fence_control		z_GeoFenceControl;
}gpsc_global_memory_type;


#ifdef _GPSC_DATA_DEFINE
gpsc_ctrl_type *               gp_zGPSCControl=NULL;
#else
extern gpsc_ctrl_type *        gp_zGPSCControl;
#endif

/* Minimum valid GPS week. Any week less than this will be considered
 * invalid and rejected from time injection.
 */
#define RECON_MIN_GPS_WEEK 1700 /* Equivalent to begining of 2013 */

// Recon additions to facilitate direct data transfer and assistance requests
#define RECON_DATA_PIPE "/data/gps/recongps"        // data fifo
#define RECON_CONTROL_PIPE "/data/gps/reconcontrol" // control fifo

#define RECON_PIPE_TYPE_DATA 1
#define RECON_PIPE_TYPE_CONTROL 2

// recon gps message
#define RECON_DATA_NMEA           0x10  // NMEA Data Indicator
#define RECON_DATA_SV             0x11  // Space Vehicle Data Indicator
#define RECON_DATA_LOCATION       0x12  // GpsLocation Data Indicator

#define RECON_CONTROL_STATUS_REQUEST_ASSIST 0x05; 

typedef struct _recongpsmessage
{
   int            opcode;      // 4 bytes: message code
   unsigned int   i_msglen;    // 4 bytes: payload size
   unsigned char* pPayload;    // 4 bytes: pointer to block of payload memory, managed within processing function
}recongpsmessage;

#endif /* _GPSC_DATA_H */
