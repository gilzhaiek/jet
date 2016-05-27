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
#define PE_HOLDING_ALT_INIT  100.0F    /* 100 meters altitude hold (m).      */
#define PE_ALT_ERR_EST_INIT  10000.0F  /* Huge 1-sigma estimate (m).         */
#define PE_ELEV_MASK_INIT    0.08727F  /* 5 degree mask.                     */
#define PE_DGPS_MAX_INT      60.0F     /* 60 second interval.                */
#define PE_PDOP_MASK_INIT    50        /* Good enough for position estimate. */
#define PE_PDOP_2D_TRESH     33         /* If altitude-hold is on, threshold  */
                                       /* to swith to a 2D mode.  */


#define  NF_NO_INFO      0x0000
#define  NF_FIX_2D      0x0001
#define NF_FIX_3D      0x0002
#define NF_FIX_OVERD    0x0004
#define NF_TOO_FEW_SVS    0x0008
#define NF_DIFFERENTIAL   0x0010
#define NF_NOT_UPDATED    0x0020
#define  NF_EXTERNAL_UPDATE  0x0040
#define NF_VELOCITY_FIX    0x0080
#define NF_ITAR_VIOLATION  0x0100

									   
									   /* enum pe_AltHoldModes determine the type of position fix.
 * Four or more satellite create a 3D position.  A 2D position would
 * use an altitude measurement in the fix.
*/

enum pe_AltHoldModes
{
  PE_ALT_MANUAL_3D,  /* No 2D positions; only 3D positions.               */
  PE_ALT_MANUAL_2D,  /* An external source provides an altitude estimate  */
                     /* that is used as a measurement in the solution.    */
  PE_ALT_HOLD_AUTO,  /* 2D or 3D fixes based on the # of measurements.    */
  PE_ALT_FILTERED,   /* In 3D mode the altitude estimate is filtered over */
                     /* time and if the PDOP exceeds u_2DPdopMask below,  */
                     /* the filtered altitude is used as a measurement.   */
  PE_ALT_ALLOW_2D    /* Allow fix when the first fix is a 2D fix */
};


/* Structure pe_RawSF1Param describes the "clock" data from NAV subframe 1.
 * The w_Week value will be extended from the 10-bit value that comes from the
 * SV into a full 16-bit value, i.e. we don't allow the GPS week to rollover
 * at 1023 weeks.
*/

typedef struct
{
  U8  u_CodeL2;        /* 2  bits */
  U8  u_Accuracy;        /* 4  bits */
  U8  u_Health;        /* 6  bits */
  U8  u_Tgd;          /* 8  bits */
  U16  w_Iodc;          /* 10 bits */
  U16  w_Toc;          /* 16 bits */
  U8  u_Af2;          /* 8  bits */
  U16  w_Af1;          /* 16 bits */
  U32  q_Af0;          /* 22 bits */

  DBL d_FullToe;        /* Cumulative GPS time of the TOE. */

} pe_RawSF1Param;


/* Structure pe_RawEphemeris describes the raw ephemeris data for one SV.
 * Items represent the values as defined for the nav message.
 * Note: If the size of this structure exceeds 64 bytes, the queue size for
 * CD must be increased.
*/

typedef struct
{
  U8  u_Prn;          /* 6 bits  */
  U8  u_Iode;          /* 8 bits  */
  U16  w_Crs;          /* 16 bits */
  U16  w_DeltaN;        /* 16 bits */
  U16  w_Cuc;          /* 16 bits */
  U32  q_MZero;        /* 32 bits */
  U32  q_E;          /* 32 bits */
  U32  q_SqrtA;        /* 32 bits */
  U32  q_OmegaZero;      /* 32 bits */
  U16  w_Cus;          /* 16 bits */
  U16  w_Toe;          /* 16 bits */
  U16  w_Cic;          /* 16 bits */
  U16  w_Cis;          /* 16 bits */
  U32  q_IZero;        /* 32 bits */
  U32  q_Omega;        /* 32 bits */
  U32  q_OmegaDot;        /* 24 bits */
  U16  w_Crc;          /* 16 bits */
  U16  w_IDot;          /* 14 bits */
  DBL d_UpdateTime;      /* Cumulative GPS time of the update. */
  U8	  u_predicted;		/*Set to TRUE if the ephemeris is predicted, FALSE if it is decoded from sky */
  U8    u_predSeedAge;	/*Used for storing the Ephemeris seed age incase of Prediceted Aphemeris*/

  /*The following parameters are used only for SMLC assistance store and not in GPSC DB */
  U8    toe_expired;               /* The ephemeris is old hence need to re-request for new one  */
  U8    u_available;               /* availability is set when eph is received from network                 */
  U32  q_sysTimeRx;		/* system timestamp (in seconds) while the ephemeris was received in NAVC */
  
} pe_RawEphemeris;


/* This pe_RawAlmanac structure describes raw almanac data as broadcast from
 * the satellites.  The members below w_Week are not apart of the broadcast
 * data, but derived from another source.
*/

typedef struct
{
  U8  u_Prn;          /* 6 bits  */
  U8  u_Health;        /* 8 bits  */
  U8  u_Toa;          /* 8 bits  */
  U16  w_E;          /* 16 bits */
  U16  w_DeltaI;        /* 16 bits */
  U16  w_OmegaDot;        /* 16 bits */
  U32  q_SqrtA;        /* 24 bits */
  U32  q_OmegaZero;      /* 24 bits */
  U32  q_Omega;        /* 24 bits */
  U32 q_MZero;        /* 24 bits */
  U16  w_Af0;          /* 11 bits */
  U16  w_Af1;          /* 11 bits */

  U16  w_Week;          /* 10 bits */
  DBL d_FullToa;        /* Cumulative GPS time of the TOA. */
  DBL d_UpdateTime;      /* Cumulative GPS time of the update. */

} pe_RawAlmanac, * const RAW_ALMANAC;
