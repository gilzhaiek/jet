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
 * FileName			:	gpsc_database.c
 *
 * Description     	:
 * This file contains functions that manage the GPSC database and non-volatile
 * memory that keeps information about knowledge the companion processor has,
 * in terms of GPS time, initial position, almanac, ephemeris, Iono, health,
 * measurement, etc.
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */


#include "gpsc_types.h"
#include "gpsc_sap.h"
#include "gpsc_data.h"
#include "gpsc_database.h"
#include "gpsc_msg.h"
#include "gpsc_app_api.h"
#include "gpsc_ext_api.h"
#include "mcpf_services.h"
#include "mcpf_mem.h"
#include "mcp_hal_fs.h"
#include "gpsc_hdgfilter.h"

#include <stdio.h>
#include <math.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>

#include <utils/Log.h>
#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG "NAVD.GPSC_DATABASE"    // our identification for logcat (adb logcat NAVD.GPSC_DATABASE:V *:S)

//#define RECON_EPHEMERIS_SERVER          // comment out for production, uncomment for in-house server

void gpsc_print_ephemeris(gpsc_db_type* p_zGPSCDatabase);
void gpsc_print_almanac(gpsc_db_type*   p_zGPSCDatabase);

// recon add-on; store raw ephemeris as downloaded from AI2 chip
#if defined RECON_EPHEMERIS_SERVER
void recon_save_ephemeris (U16 gpsweek, pe_RawEphemeris* p_RawEph, pe_RawSF1Param* p_RawFS1Param);
#endif

/*
 ******************************************************************************
 * gpsc_db_update_clock
 *
 * Function description:
 *
 * This function fills LM's GPS Clock database structure with the scaled
 * Clock Report data.
 *
 * Parameters:
 *
 *  p_Field - Pointer to received AI2 Field.
 *  p_zGPSCDatabase - pointer to the database structure
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/
void gpsc_db_update_clock( Ai2Field *p_zAi2Field, gpsc_db_type *p_zGPSCDatabase )
{
  U8*  p_B = p_zAi2Field->p_B;
  U32  q_Word;
  S32  l_Word;
  U16  w_Word;
  U16  w_Mantissa;
  U8   u_Exponent;

gpsc_db_gps_meas_type    *p_zDBGpsMeas;
gpsc_meas_report_type   *p_zMeasReport;
me_Time   *p_meTime;
gpsc_db_gps_clk_type*  p_DBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;
  gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
  gpsc_custom_struct* p_zCustom = p_zGPSCControl->p_zCustomStruct;
  U8 p_SubPacket;
  gpsc_db_pos_type *p_DBPos = &p_zGPSCDatabase->z_DBPos;

  if (p_zAi2Field->u_Id == AI_REP_CLOCK)
{	// AI 2 packet is a clock report
	  STATUSMSG("gpsc_db_update_clock: Updating DB using CLK report");
	  p_DBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;
	  p_DBGpsClock->q_FCount = NBytesGet( &p_B, sizeof(U32) );
	  w_Word = (U16) NBytesGet ( &p_B, sizeof (U16) );


	  p_DBGpsClock->w_GpsWeek = w_Word;
	  p_DBGpsClock->q_GpsMs = NBytesGet ( &p_B, sizeof (U32) );

	  q_Word = NBytesGet ( &p_B, 3 );
	  l_Word = S32Convert ( q_Word, 24 );  /* Sign extended */
	  p_DBGpsClock->f_TimeBias = l_Word * (FLT)C_LSB_TIME_BIAS;

	  w_Word = (U16) NBytesGet ( &p_B, sizeof (U16) );
	  w_Mantissa = (U16)(w_Word >> 5);
	  u_Exponent = (U8)(w_Word & 0x001F);

	  /* convert from mantissa/exponent to float, from ns to ms. */
	  p_DBGpsClock->f_TimeUnc = (FLT)ext_ldexp( (DBL) w_Mantissa, u_Exponent )
	                          * (FLT)0.000001;
	STATUSMSG("gpsc_db_update_clock: Updating DB using CLK report time uncertinaty %f",p_DBGpsClock->f_TimeUnc);

	  p_DBGpsClock->b_UTCDiff = (S8) *p_B++;

	  l_Word = (S32) NBytesGet( &p_B, sizeof(U32) );
	  p_DBGpsClock->f_FreqBias = l_Word * (FLT) C_LSB_FRQ_BIAS;

	  q_Word = NBytesGet( &p_B, sizeof(U32) );
	  p_DBGpsClock->f_FreqUnc = q_Word * (FLT) C_LSB_FRQ_UNC;

	if((p_DBGpsClock->w_GpsWeek != C_GPS_WEEK_UNKNOWN)&&(p_DBGpsClock->f_TimeUnc<= C_MAX_VALID_TIME_UNC_MS))
	  p_DBGpsClock->u_Valid = TRUE;
	else
	  p_DBGpsClock->u_Valid = FALSE;
          if(p_DBPos->d_ToaSec==0)
          p_DBPos->d_ToaSec = gpsc_db_gps_time_in_secs(gp_zGPSCControl);
}
else if (p_zAi2Field->u_Id == AI_REP_CUSTOM_PACKET)
{
   p_SubPacket = *p_B++;
   if (p_SubPacket == AI_REP_MEAS_EXT)
   {	// AI2 packet is an extended meas report
	  STATUSMSG("gpsc_db_update_clock: Updating DB using Ext Meas report");
	  p_zDBGpsMeas = &p_zGPSCDatabase->z_DBGpsMeas;
	  p_zMeasReport = &p_zDBGpsMeas->z_MeasReport;
	  p_meTime = &p_zMeasReport->z_MeasToa;
	  p_DBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;

	  p_DBGpsClock->q_FCount = (U32) NBytesGet(&p_B, sizeof(U32));
	  p_DBGpsClock->w_GpsWeek = (U16) NBytesGet(&p_B, sizeof(U16));
	  p_DBGpsClock->q_GpsMs = (U32) NBytesGet(&p_B, sizeof(U32));

	  q_Word = (U32)(NBytesGet(&p_B, 3) & 0x00FFFFFF);
	  l_Word = S32Convert ( q_Word, 24 );  /* Sign extended */
	  p_DBGpsClock->f_TimeBias = l_Word * (FLT)C_LSB_TIME_BIAS;

	  w_Word = (U16) NBytesGet(&p_B, sizeof(U16));
		if (w_Word != 4395899)
			p_zCustom->custom_assist_availability_flag |= C_REF_GPSTIME_AVAIL;

	  w_Mantissa = (U16)(w_Word >> 5);
	  u_Exponent = (U8)(w_Word & 0x001F);

	  /* convert from mantissa/exponent to float, from ns to ms. */
	  p_DBGpsClock->f_TimeUnc = (FLT)ext_ldexp( (DBL) w_Mantissa, u_Exponent )
	                          * (FLT)0.000001;
	  l_Word = (S32) NBytesGet( &p_B, sizeof(S32) );
	  p_DBGpsClock->f_FreqBias =  l_Word *  (FLT) C_LSB_FRQ_BIAS;

	  p_DBGpsClock->f_FreqUnc = (FLT) (NBytesGet(&p_B, sizeof(U32)) * (FLT) C_LSB_FRQ_BIAS);

	if((p_DBGpsClock->w_GpsWeek != C_GPS_WEEK_UNKNOWN)&&(p_DBGpsClock->f_TimeUnc<= C_MAX_VALID_TIME_UNC_MS))
	  p_DBGpsClock->u_Valid = TRUE;
	else {
	  p_DBGpsClock->u_Valid = FALSE;
      p_DBGpsClock->u_NumInvalids++;
    }

          if(p_DBPos->d_ToaSec==0)
          p_DBPos->d_ToaSec = gpsc_db_gps_time_in_secs(gp_zGPSCControl);
   }
   else
   {
	STATUSMSG("gpsc_db_update_clock: unable to decode AI2 custom packet: %d", p_SubPacket);
	return;
   }
}
else
{
	STATUSMSG("gpsc_db_update_clock: unable to decode AI2 packet: %d", p_zAi2Field->u_Id);
	return;
}


  STATUSMSG("Data: CLK FC:%lu  FBias:%f  FUnc:%f UTCDiff:%d",
          p_DBGpsClock->q_FCount,
          p_DBGpsClock->f_FreqBias,
          p_DBGpsClock->f_FreqUnc,
          p_DBGpsClock->b_UTCDiff);
  if (p_DBGpsClock->w_GpsWeek == C_GPS_WEEK_UNKNOWN)
  {

  STATUSMSG("Data: CLK contd.. Wk=n.k.,Ms=%d, TUnc=%f, ",
		  p_DBGpsClock->q_GpsMs,
		  p_zGPSCDatabase->z_DBGpsClock.f_TimeUnc);

  }
  else
  {
  STATUSMSG("Data: CLK contd.. Wk=%u,Ms=%d, TUnc=%f",
		  p_DBGpsClock->w_GpsWeek,
		  p_DBGpsClock->q_GpsMs,
		  p_zGPSCDatabase->z_DBGpsClock.f_TimeUnc);

  }

  if(p_DBGpsClock->u_Valid == TRUE)
  {
              /*Set Time as available*/
		p_zCustom->custom_assist_availability_flag |= C_REF_GPSTIME_AVAIL;

		gpsc_populate_send_gpstime_data(p_DBGpsClock);

  }
  else{
		/*Set Time as NOT available*/
		p_zCustom->custom_assist_availability_flag &= ~C_REF_GPSTIME_AVAIL;

  }
}

/*
 ******************************************************************************
 * gpsc_db_update_pos
 *
 * Function description:
 *
 * This function fills the Position database structure with the scaled
 * Position Report data
 *
 * Parameters:
 *  p_Field - Pointer to received packet field
 *  p_zGPSCDatabase - pointer to the database structure
 *
 * Return value:
 *  TRUE: updated; FALSE: not updated
 *
 ******************************************************************************
*/

U8 gpsc_db_update_pos (Ai2Field *p_Field, gpsc_db_type *p_zGPSCDatabase)
{

  gpsc_ctrl_type*	p_zGPSCControl;
  gpsc_sess_cfg_type      *p_zSessConfig;


  U8  *p_B=p_Field->p_B;
  U16 w_PacketLen = p_Field->w_Length;
  U8  u_SvCnt;
  U8  u_i;
  U8 u_SvID;
  gpsc_db_pos_type *p_DBPos = &p_zGPSCDatabase->z_DBPos;
  gpsc_report_pos_type *p_ReportPos = &p_zGPSCDatabase->z_DBPos.z_ReportPos;
  gpsc_db_gps_clk_type *p_DBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;
  gpsc_custom_struct* p_zCustom;
  me_Time *p_meTime = &p_DBPos->z_PosTime;
  p_zGPSCControl = gp_zGPSCControl;
  p_zSessConfig = p_zGPSCControl->p_zSessConfig;

  STATUSMSG("status: inside gpsc_db_update_pos");

  p_zCustom = p_zGPSCControl->p_zCustomStruct;

  if (w_PacketLen == 4 || w_PacketLen == 6) //To support all AI2 versions
  {
    p_DBPos->d_ToaSec = 0;
    return FALSE; /* no position information in Sensor */
  }

  p_ReportPos->q_RefFCount = NBytesGet (&p_B, sizeof(U32));
  p_ReportPos->w_PositionFlags = (U16) NBytesGet(&p_B, sizeof(U16));
  p_ReportPos->l_Lat = (S32)NBytesGet(&p_B, sizeof(S32));
//  STATUSMSG("Reported Latitude = %ld\n",p_ReportPos->l_Lat);
  p_ReportPos->l_Long = (S32)NBytesGet(&p_B, sizeof(S32));
//  STATUSMSG("Reported Longitude = %ld\n",p_ReportPos->l_Long);
  p_ReportPos->x_Height = (S16)NBytesGet(&p_B,sizeof(S16));
//  STATUSMSG("Reported Height = %d\n",p_ReportPos->x_Height);
  p_ReportPos->w_EastUnc = (U16) NBytesGet (&p_B,sizeof(U16));
  p_ReportPos->w_NorthUnc = (U16) NBytesGet (&p_B,sizeof(U16));
  p_ReportPos->w_VerticalUnc = (U16) NBytesGet (&p_B,sizeof(U16));
  p_ReportPos->x_VelEast = (U16) NBytesGet (&p_B,sizeof(U16));
  p_ReportPos->x_VelNorth = (U16) NBytesGet (&p_B,sizeof(U16));
  p_ReportPos->x_VelVert = (U16) NBytesGet(&p_B,sizeof(U16));
  p_ReportPos->u_PDOP = *p_B++;
  p_ReportPos->u_HDOP = *p_B++;
  p_ReportPos->u_VDOP = *p_B++;

  u_SvCnt = (U8)(( p_Field->w_Length - 31 ) / 6);

  /* Make sure no more than N_LCHAN SVs are being reported otherwise we
     could get an out of bounds array write below */
  if ( u_SvCnt > N_LCHAN )
  {
    u_SvCnt = N_LCHAN;
  }

  for (u_i=0; u_i < u_SvCnt; u_i++)
  {
	u_SvID = (U8)(*p_B & 0xFF);

	if((u_SvID >= 193) && (u_SvID <= 202))
	{
	  *p_B++;
      STATUSMSG("---QZSS SVIDs suppressed in position report--- %d ",u_SvID);
      u_i--;
      u_SvCnt--;

      continue;
    }
    p_ReportPos->u_SVs[ u_i ] = *p_B++;
    p_ReportPos->u_IODE[ u_i ] = *p_B++;
    p_ReportPos->w_Residuals[ u_i ] = (U16) NBytesGet (&p_B,sizeof(U16));
    p_ReportPos->w_SvWeights[ u_i ] = (U16) NBytesGet (&p_B,sizeof(U16));
  }

  p_ReportPos->u_nSVs = u_SvCnt;

  p_meTime->w_GpsWeek = p_DBGpsClock->w_GpsWeek;
  p_meTime->q_GpsMsec = p_DBGpsClock->q_GpsMs;

  p_DBPos->d_ToaSec = gpsc_db_gps_time_in_secs(gp_zGPSCControl);

  p_zCustom->custom_assist_availability_flag |= C_REFLOC_AVAIL;

  /* check to see if this is an externally updated position or a part of the fix */
  /* set ACQ Assistance as available if the receiver has valied computed position */
  if(!(p_ReportPos->w_PositionFlags & 0x40))
  {
  		p_zCustom->custom_assist_availability_flag |= C_ACQ_AVAIL;
		p_zCustom->custom_assist_availability_flag |= C_TCXO_AVAIL;
  }

  /* update Ref FCount in session DB as well */
  p_zSessConfig->q_FCount = p_ReportPos->q_RefFCount;
  return TRUE;
}

/*
 ******************************************************************************
 * gpsc_db_update_ext_pos
 *
 * Function description:
 *
 * This function fills the Position database structure with the scaled
 * Extended Position Report data
 *
 * Parameters:
 *  p_Field - Pointer to received packet field
 *  p_zGPSCDatabase - pointer to the database structure
 *
 * Return value:
 *  TRUE: updated; FALSE: not updated
 *
 ******************************************************************************
*/

U8 gpsc_db_update_ext_pos (Ai2Field *p_Field, gpsc_db_type *p_zGPSCDatabase)
{
  gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
  gpsc_sess_cfg_type      *p_zSessConfig  = p_zGPSCControl->p_zSessConfig;

  U8  *p_B=p_Field->p_B;
  U16 w_PacketLen = p_Field->w_Length;
  U8  u_SvCnt;
  U8  u_i;
  U8 u_SvID = 0;
  gpsc_db_pos_type *p_DBPos = &p_zGPSCDatabase->z_DBPos;
  gpsc_report_pos_type *p_ReportPos = &p_zGPSCDatabase->z_DBPos.z_ReportPos;
  gpsc_db_gps_clk_type *p_DBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;
  me_Time *p_meTime = &p_DBPos->z_PosTime;
  gpsc_custom_struct* p_zCustom = p_zGPSCControl->p_zCustomStruct;
	VelocityVar z_CurrentVelocityVar, z_FilteredVelocityVar;

  STATUSMSG("status: inside gpsc_db_update_ext_pos");

  if (w_PacketLen == 4 || w_PacketLen == 6) //To support all AI2 versions
  {
    p_DBPos->d_ToaSec = 0;
    return FALSE; /* no position information in Sensor */
  }

  p_ReportPos->q_RefFCount = NBytesGet (&p_B, sizeof(U32));
  p_ReportPos->w_PositionFlags = (U16) NBytesGet(&p_B, sizeof(U16));
  p_ReportPos->l_Lat = (S32)NBytesGet(&p_B, sizeof(S32));
  p_ReportPos->l_Long = (S32)NBytesGet(&p_B, sizeof(S32));
  p_ReportPos->x_Height = (S16)NBytesGet(&p_B,sizeof(S16));
  p_ReportPos->x_HeightMSL = (S16)NBytesGet(&p_B,sizeof(S16));
  p_ReportPos->w_EastUnc = (U16) NBytesGet (&p_B,sizeof(U16));
  p_ReportPos->w_NorthUnc = (U16) NBytesGet (&p_B,sizeof(U16));
  p_ReportPos->w_VerticalUnc = (U16) NBytesGet (&p_B,sizeof(U16));
  p_ReportPos->x_VelEast = (U16) NBytesGet (&p_B,sizeof(U16));
  p_ReportPos->x_VelNorth = (U16) NBytesGet (&p_B,sizeof(U16));
  p_ReportPos->x_VelVert = (U16) NBytesGet(&p_B,sizeof(U16));
  p_ReportPos->w_VelUnc = (U16) NBytesGet (&p_B,sizeof(U16));
  p_ReportPos->u_PDOP = *p_B++;
  p_ReportPos->u_HDOP = *p_B++;
  p_ReportPos->u_VDOP = *p_B++;
  p_ReportPos->w_HeadTrue = (U16) NBytesGet (&p_B,sizeof(U16));
  p_ReportPos->w_HeadMagnet = (U16) NBytesGet (&p_B,sizeof(U16));
  p_ReportPos->w_LocAngleUnc = (U16) NBytesGet (&p_B,sizeof(U16));
  p_ReportPos->w_LocUncOnAxis = (U16) NBytesGet (&p_B,sizeof(U16));
  p_ReportPos->w_LocUncPerAxis = (U16) NBytesGet (&p_B,sizeof(U16));
  p_ReportPos->u_UtcHours = *p_B++;
  p_ReportPos->u_UtcMins = *p_B++;
  p_ReportPos->u_UtcSec = *p_B++;
  p_ReportPos->u_UtcTenthSec = *p_B++;
  p_ReportPos->q_Rsvd1 = (U32)NBytesGet (&p_B, sizeof(U32));
  p_ReportPos->q_Rsvd2 = (U32)NBytesGet (&p_B, sizeof(U32));
  p_ReportPos->q_Rsvd3 = (U32)NBytesGet (&p_B, sizeof(U32));

  u_SvCnt = (U8)(( p_Field->w_Length - 61 ) / 6);

	STATUSMSG("gpsc_db_update_ext_pos: INPUT(Before Conversion): %d  %d  %d\n",p_ReportPos->x_VelEast,p_ReportPos->x_VelNorth,p_ReportPos->w_HeadTrue);
	z_CurrentVelocityVar.f_Heading	 = (FLT) (p_ReportPos->w_HeadTrue * 180.0/((FLT)(pow(2.0,15))));
	z_CurrentVelocityVar.f_VelEast	 = (FLT) (p_ReportPos->x_VelEast * 1000.0/65535.0);
	z_CurrentVelocityVar.f_VelNorth  = (FLT) (p_ReportPos->x_VelNorth * 1000.0/ 65535.0);
	z_CurrentVelocityVar.q_FCount	 = p_ReportPos->q_RefFCount;
	STATUSMSG("gpsc_db_update_ext_pos: INPUT(After Conversion):  %f  %f  %f\n",z_CurrentVelocityVar.f_VelEast,z_CurrentVelocityVar.f_VelNorth,z_CurrentVelocityVar.f_Heading);
	gpsc_HdgFilter(&z_CurrentVelocityVar, &z_FilteredVelocityVar);
	STATUSMSG("gpsc_db_update_ext_pos: AFTER FILTER(Before Conversion): %f  %f  %f\n",z_FilteredVelocityVar.f_VelEast,z_FilteredVelocityVar.f_VelNorth,z_FilteredVelocityVar.f_Heading);
	p_ReportPos->x_VelEast    = (S16)(z_FilteredVelocityVar.f_VelEast * 65535.0/1000.0);
	p_ReportPos->x_VelNorth   = (S16)(z_FilteredVelocityVar.f_VelNorth * 65535.0/1000.0);
	p_ReportPos->w_HeadTrue   = (U16)(z_FilteredVelocityVar.f_Heading * pow(2.0,15)/180.0);
	STATUSMSG("gpsc_db_update_ext_pos: AFTER FILTER(After Conversion): %d   %d  %d\n",p_ReportPos->x_VelEast,p_ReportPos->x_VelNorth,p_ReportPos->w_HeadTrue);
  /* Make sure no more than N_LCHAN SVs are being reported otherwise we
     could get an out of bounds array write below */
  if ( u_SvCnt > N_LCHAN )
  {
    u_SvCnt = N_LCHAN;
  }



  for (u_i=0; u_i < u_SvCnt; u_i++)
  {
    u_SvID = (U8)(*p_B & 0xFF);

    if((u_SvID >= 193) && (u_SvID <= 202))
    {
      *p_B++;
      STATUSMSG("---QZSS SVIDs suppressed in ext position--- %d ",u_SvID);
      u_i--;
      u_SvCnt--;

      continue;
    }
    p_ReportPos->u_SVs[ u_i ] = *p_B++;
    p_ReportPos->u_IODE[ u_i ] = *p_B++;
    p_ReportPos->w_Residuals[ u_i ] = (U16) NBytesGet (&p_B,sizeof(U16));
    p_ReportPos->w_SvWeights[ u_i ] = (U16) NBytesGet (&p_B,sizeof(U16));
  }

  p_ReportPos->u_nSVs = u_SvCnt;

  p_meTime->w_GpsWeek = p_DBGpsClock->w_GpsWeek;
  p_meTime->q_GpsMsec = p_DBGpsClock->q_GpsMs;

  p_DBPos->d_ToaSec = gpsc_db_gps_time_in_secs(gp_zGPSCControl);

  p_zCustom->custom_assist_availability_flag |= C_REFLOC_AVAIL;

  /* check to see if this is an externally updated position or a part of the fix */
  /* set ACQ Assistance as available if the receiver has valied computed position */
  if(!(p_ReportPos->w_PositionFlags & 0x40))
  {
  		p_zCustom->custom_assist_availability_flag |= C_ACQ_AVAIL;
		p_zCustom->custom_assist_availability_flag |= C_TCXO_AVAIL;
  }

  /* update Ref FCount in session DB as well */
  p_zSessConfig->q_FCount = p_ReportPos->q_RefFCount;
  return TRUE;

}


U8 gpsc_db_update_geofence_status(Ai2Field *p_Field, gpsc_db_type *p_zGPSCDatabase)
{
  gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
  TNAVC_MotionMask_Status* p_zmotionmaskstatus = NULL;
  TNAVC_MotionMask_Status zGeofenceReport;
  p_zmotionmaskstatus = (TNAVC_MotionMask_Status*)&zGeofenceReport;

  U8  *p_B=p_Field->p_B;
  U16 w_PacketLen = p_Field->w_Length;
  U8 u_SubPktId;

  gpsc_geofence_status *p_geofence = &p_zGPSCDatabase->z_GeofenceStatus;

  STATUSMSG("status: inside gpsc_db_update_geofence_status");
  	u_SubPktId = (U8)NBytesGet(&p_B, sizeof(U8));
	  p_zmotionmaskstatus->TimerCount = (U32)NBytesGet (&p_B, sizeof(U32));
	  p_zmotionmaskstatus->status = (U32)NBytesGet(&p_B, sizeof(U32));

	    STATUSMSG("status: inside gpsc_db_update_geofence_status %d\n ",p_zmotionmaskstatus->status);
	    STATUSMSG("status: inside gpsc_db_update_geofence_fcount %d\n ",p_zmotionmaskstatus->TimerCount);

		p_geofence->status = p_zmotionmaskstatus->status;
		p_geofence->FCount = p_zmotionmaskstatus->TimerCount;

		gpsc_geofence_pro_report(p_zGPSCControl,p_zmotionmaskstatus);

	  return TRUE;

}
/*
 ******************************************************************************
 * gpsc_db_update_alm
 *
 * Function description:
 *
 * This function fills the Almanac database structure with the scaled
 * Almanac Report data
 *
 * Parameters:
 *  p_Field - Pointer to received packet field
 *  p_zGPSCDatabase - Pointer to the LM database
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/

void gpsc_db_update_alm (Ai2Field *p_Field, gpsc_db_type *p_zGPSCDatabase)
{
  U8 *p_B = p_Field->p_B;
  U8  u_Sv;
  U16  w_Word;
  U32 q_Word;
  gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
  gpsc_custom_struct* p_zCustom = p_zGPSCControl->p_zCustomStruct;

  gpsc_db_alm_type *p_DBAlmanac;
  pe_RawAlmanac *p_RawAlmanac;

  U16 w_PacketLen = p_Field->w_Length;

  if (w_PacketLen == 1 ) //if reported SV does not have any data
    	return ; /* Do nothing */

  STATUSMSG("Status: gpsc_db_update_alm");

  u_Sv = *p_B++;
  if((u_Sv >= 193) && (u_Sv <= 202))
  {
      STATUSMSG("---QZSS SVIDs suppressed in report almanac--- %d ",u_Sv);

      return;
   }

  /*KlocWork Critical Issue:37 Resolved by adding boundary check*/
  if((u_Sv >0) && (u_Sv <= 32))
  {

    p_DBAlmanac = &p_zGPSCDatabase->z_DBAlmanac[ u_Sv - 1 ];
  p_RawAlmanac = &p_zGPSCDatabase->z_DBAlmanac[ u_Sv - 1 ].z_RawAlmanac;

  p_RawAlmanac->u_Prn = u_Sv;
  p_RawAlmanac->u_Health = *p_B++;
  p_RawAlmanac->u_Toa = *p_B++;

  w_Word = (U16) NBytesGet(&p_B,sizeof(U16));
  p_RawAlmanac->w_E = w_Word;

  w_Word = (U16) NBytesGet(&p_B,sizeof(U16));
  p_RawAlmanac->w_DeltaI = w_Word;

  w_Word = (U16) NBytesGet(&p_B,sizeof(U16));
  p_RawAlmanac->w_OmegaDot = w_Word;

  q_Word = NBytesGet(&p_B,3);
  p_RawAlmanac->q_SqrtA = q_Word;

  q_Word = NBytesGet(&p_B,3);
  p_RawAlmanac->q_OmegaZero = q_Word;

  q_Word = NBytesGet(&p_B,3);
  p_RawAlmanac->q_Omega = q_Word;

  q_Word = NBytesGet(&p_B,3);
  p_RawAlmanac->q_MZero = q_Word;

  w_Word = (U16) NBytesGet(&p_B,sizeof(U16));
  p_RawAlmanac->w_Af0 = w_Word;

  w_Word = (U16) NBytesGet(&p_B,sizeof(U16));
  p_RawAlmanac->w_Af1 = w_Word;

  w_Word = (U16) NBytesGet(&p_B,sizeof(U16));
  p_RawAlmanac->w_Week = w_Word;

  w_Word = (U16) NBytesGet(&p_B,sizeof(U16));

  q_Word = NBytesGet( &p_B, sizeof(U32) );

  /* Compute Update Time: note this time stamp comes from Sensor. */
  if ( w_Word == C_GPS_WEEK_UNKNOWN )
    p_RawAlmanac->d_UpdateTime = -1.0;
  else
    p_RawAlmanac->d_UpdateTime = (DBL) w_Word * (DBL) WEEK_SECS +
      (DBL) q_Word * 0.001;

  /* Compute Full Toa */
  p_RawAlmanac->d_FullToa = (DBL) p_RawAlmanac->w_Week * (DBL) WEEK_SECS +
    (DBL) p_RawAlmanac->u_Toa * 4096.0;

  p_DBAlmanac->u_Valid = TRUE;

  p_zCustom->custom_assist_availability_flag |= C_ALM_AVAIL;
}
  else
  {
	STATUSMSG("uSv value out of bound");
  }

}
/*
 ******************************************************************************
 * gpsc_db_update_iono
 *
 * Function description:
 *
 * This function fills the Iono database structure with the scaled
 * Iono Report data.
 *
 * Parameters:
 *
 *  p_Field - Pointer to received AI2 Field.
 *  p_zGPSCDatabase - Pointer to the GPSC database
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/
void gpsc_db_update_iono( Ai2Field *p_Field, gpsc_db_type *p_zGPSCDatabase )
{
  U8 *p_B = p_Field->p_B;
  gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
  gpsc_custom_struct* p_zCustom = p_zGPSCControl->p_zCustomStruct;

  STATUSMSG("Status: gpsc_db_update_iono");

  /* Prevent from injecting incorrect Iono data */
  if ( (p_zGPSCDatabase->z_DBGpsClock.u_Valid) &&
       (p_zGPSCDatabase->z_DBGpsClock.w_GpsWeek != C_GPS_WEEK_UNKNOWN) &&
       (p_Field->w_Length != 0)
     )
  {
    p_zGPSCDatabase->z_DBIono.z_RawIono.u_Alpha0 = *p_B++;
    p_zGPSCDatabase->z_DBIono.z_RawIono.u_Alpha1 = *p_B++;
    p_zGPSCDatabase->z_DBIono.z_RawIono.u_Alpha2 = *p_B++;
    p_zGPSCDatabase->z_DBIono.z_RawIono.u_Alpha3 = *p_B++;
    p_zGPSCDatabase->z_DBIono.z_RawIono.u_Beta0 = *p_B++;
    p_zGPSCDatabase->z_DBIono.z_RawIono.u_Beta1 = *p_B++;
    p_zGPSCDatabase->z_DBIono.z_RawIono.u_Beta2 = *p_B++;
    p_zGPSCDatabase->z_DBIono.z_RawIono.u_Beta3 = *p_B++;
    p_zGPSCDatabase->z_DBIono.d_UpdateTimeSec =
	gpsc_db_gps_time_in_secs(gp_zGPSCControl);

	p_zGPSCDatabase->z_DBIono.v_iono_valid = TRUE;
	p_zCustom->custom_assist_availability_flag |= C_IONO_AVAIL;
  }
}

/*
 ******************************************************************************
 * gpsc_db_update_utc
 *
 * Function description:
 *
 * This function fills the utc database structure with the scaled
 * utc Report data.
 *
 * Parameters:
 *
 *  p_Field - Pointer to received AI2 Field.
 *  p_zGPSCDatabase - Pointer to the GPSC database
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/
void gpsc_db_update_utc( Ai2Field *p_Field, gpsc_db_type *p_zGPSCDatabase )
{
	U8 *p_B = p_Field->p_B;
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	gpsc_custom_struct* p_zCustom = p_zGPSCControl->p_zCustomStruct;

	STATUSMSG("Status: gpsc_db_update_utc");

	if(p_Field->w_Length)
	{
		STATUSMSG("gpsc_db_update_utc: received UTC with valid length");
		// valid UTC data is available
		p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_a0            = NBytesGet( &p_B, sizeof(U32) );
		p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_a1            = NBytesGet( &p_B, sizeof(U32) );
		p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_delta_tls     = *p_B++;
		p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_tot           = *p_B++;
		p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_wnt           = *p_B++;
		p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_wnlsf         = *p_B++;
		p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_dn            = *p_B++;
		p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_delta_tlsf    = *p_B++;
		p_zGPSCDatabase->z_DBUtc.u_valid = 	1;

		STATUSMSG("gpsc_db_update_utc: a1=%d,a0=%d,tot=%d,wnt=%d,tls=%d,wnlsf=%d,dn=%d,tlsf=%d",
			p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_a1,
			p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_a0,
			p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_tot,
			p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_wnt,
			p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_delta_tls,
			p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_wnlsf,
			p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_dn,
			p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_delta_tlsf);

		//set the availability flag
		p_zCustom->custom_assist_availability_flag |= C_UTC_AVAIL;

		if (  p_zGPSCDatabase->z_DBGpsClock.u_Valid &&
		     (p_zGPSCDatabase->z_DBGpsClock.w_GpsWeek != C_GPS_WEEK_UNKNOWN))
		  {
			    p_zGPSCDatabase->z_DBUtc.d_UpdateTimeSec = 	gpsc_db_gps_time_in_secs(gp_zGPSCControl);
		  }

  	}
	else
	{
		STATUSMSG("gpsc_db_update_utc: UTC with Invalid length");
		p_zGPSCDatabase->z_DBUtc.u_valid = 0;
	}

}


/*
 ******************************************************************************
 * gpsc_db_update_eph
 *
 * Function description:
 *
 * This function fills the Ephemeris database structure with the scaled
 * Ephemeris Report data
 *
 * Parameters:
 *  p_Field - Pointer to received packet field
 *  p_zGPSCDatabase - Pointer to the LM database
 *
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/

void gpsc_db_update_eph (Ai2Field* p_Field, gpsc_db_type* p_zGPSCDatabase)
{
  U8* p_B = p_Field->p_B;
  U16 w_Word = 0;  
  U32  q_Word = 0;
  U8  u_Sv = 0;
  gpsc_db_eph_type* p_DBEphemeris = 0;
  pe_RawEphemeris*  p_RawEph = 0;
  pe_RawSF1Param*   p_RawSF1Param = 0;
  pe_RawEphemeris*  p_zSmlcRawEph = 0;
  pe_RawSF1Param*   p_zSmlcRawSF1Param = 0;
  U8 u_Sv_health = 0;
  gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
  gpsc_custom_struct* p_zCustom = p_zGPSCControl->p_zCustomStruct;
  U16 w_PacketLen = p_Field->w_Length;
  gpsc_smlc_assist_type* p_zSmlcAssist = p_zGPSCControl->p_zSmlcAssist;
  gpsc_db_health_type*  p_zDBHealth = &p_zGPSCDatabase->z_DBHealth;

  U16 gpsweek = C_GPS_WEEK_UNKNOWN;  // unknown

  // if reported SV does not have any data
  if (w_PacketLen == 1 )
  {
      ALOGI("+++ %s: New Ephemeris, but SV has no data - exiting +++\n", __FUNCTION__);
    	return; 
  }

  u_Sv = *p_B++;
  u_Sv_health = *(p_B + 2);

  /*<health check> - do not update the database for the satellites whose health is not good */
  if (u_Sv_health == 1)
  {
  	  if (p_zCustom->custom_assist_availability_flag & C_RTI_AVAIL)
  	  {
  		  if (p_zDBHealth->u_AlmHealth[u_Sv-1] != C_HEALTH_GOOD)
  		  {
  			  ALOGI("+++ %s: Health NOK for Sv: %d, RTI Available. Quitting +++\n", __FUNCTION__, u_Sv);
			  return;
  		  }
     }
     else
     {
		  ALOGI("+++ %s: Health not OK for SV: %d. Quitting +++\n", __FUNCTION__, u_Sv);
		  return;
     }
  }
  /*<\health check>*/

  p_DBEphemeris = &p_zGPSCDatabase->z_DBEphemeris[ u_Sv - 1 ];
  p_RawEph = &p_zGPSCDatabase->z_DBEphemeris[ u_Sv - 1 ].z_RawEphemeris;
  p_RawSF1Param = &p_zGPSCDatabase->z_DBEphemeris[ u_Sv - 1 ].z_RawSF1Param;

  p_RawEph->u_Prn = u_Sv;

  p_RawSF1Param->u_CodeL2 = *p_B++;
  p_RawSF1Param->u_Accuracy = *p_B++;
  p_RawSF1Param->u_Health = *p_B++;
  p_RawSF1Param->u_Tgd = *p_B++;

  w_Word = (U16) NBytesGet(&p_B, sizeof(U16));
  p_RawSF1Param->w_Iodc = w_Word;

  w_Word = (U16) NBytesGet(&p_B, sizeof(U16));
  p_RawSF1Param->w_Toc = w_Word;

  p_RawSF1Param->u_Af2 = *p_B++;

  w_Word = (U16) NBytesGet(&p_B,sizeof(U16));
  p_RawSF1Param->w_Af1 = w_Word;

  q_Word = NBytesGet(&p_B,3);
  p_RawSF1Param->q_Af0 = q_Word;

  p_RawEph->u_Iode = *p_B++;

  w_Word = (U16) NBytesGet(&p_B,sizeof(U16));
  p_RawEph->w_Crs = w_Word;

  w_Word = (U16) NBytesGet(&p_B,sizeof(U16));
  p_RawEph->w_DeltaN = w_Word;

  q_Word = NBytesGet(&p_B,sizeof(U32));
  p_RawEph->q_MZero = q_Word;

  w_Word = (U16) NBytesGet(&p_B,sizeof(U16));
  p_RawEph->w_Cuc = w_Word;

  q_Word = NBytesGet(&p_B,sizeof(U32));
  p_RawEph->q_E = q_Word;

  w_Word = (U16) NBytesGet(&p_B,sizeof(U16));
  p_RawEph->w_Cus = w_Word;

  q_Word = NBytesGet(&p_B,sizeof(U32));
  p_RawEph->q_SqrtA = q_Word;

  w_Word = (U16) NBytesGet(&p_B,sizeof(U16));
  p_RawEph->w_Toe = w_Word;

  w_Word = (U16) NBytesGet(&p_B,sizeof(U16));
  p_RawEph->w_Cic = w_Word;

  q_Word = NBytesGet(&p_B,sizeof(U32));
  p_RawEph->q_OmegaZero = q_Word;

  w_Word = (U16) NBytesGet(&p_B,sizeof(U16));
  p_RawEph->w_Cis = w_Word;

  q_Word = NBytesGet(&p_B,sizeof(U32));
  p_RawEph->q_IZero = q_Word;

  w_Word = (U16) NBytesGet(&p_B,sizeof(U16));
  p_RawEph->w_Crc = w_Word;

  q_Word = NBytesGet(&p_B,sizeof(U32));
  p_RawEph->q_Omega = q_Word;

  q_Word = NBytesGet(&p_B,3);
  p_RawEph->q_OmegaDot = q_Word;

  w_Word = (U16) NBytesGet(&p_B,sizeof(U16));
  p_RawEph->w_IDot = w_Word;

  w_Word = (U16) NBytesGet(&p_B,sizeof(U16));
  p_zGPSCDatabase->z_DBEphemeris[ u_Sv - 1 ].w_EphWeek = w_Word;
    /* week of ephemeris */

  w_Word = (U16) NBytesGet( &p_B, sizeof(U16) ); /* update week */
  gpsweek = w_Word;

  q_Word = NBytesGet( &p_B, sizeof(U32) );  /* msec into week of last update */

  /* Compute Update Time: note that the time stamp comes from Sensor */
  if ( w_Word == C_GPS_WEEK_UNKNOWN )
    p_RawEph->d_UpdateTime = -1.0;
  else
    p_RawEph->d_UpdateTime = (DBL) w_Word * (DBL) WEEK_SECS +
      (DBL) q_Word * 0.001;

  p_RawSF1Param->d_FullToe = (DBL)p_DBEphemeris->w_EphWeek
                           * (DBL) WEEK_SECS
                           + (DBL)p_RawEph->w_Toe * 16.0;

  p_DBEphemeris->u_Valid = TRUE;

  /*Update the DB with proper Predicted parameters, predicted flag and predicted Seed Age from the injected data*/
  p_zSmlcRawEph = &p_zSmlcAssist->z_SmlcRawEph[u_Sv-1];
  p_zSmlcRawSF1Param = &p_zSmlcAssist->z_SmlcRawSF1Param[u_Sv-1];

  /*if the IODE and the User Range Accuracy of the injected SV is same as that the receiver is sending then we think
  ** this ephemeris is a predicted ephemeris not the decoded one from sky
  */
  if ( (p_RawEph->u_Iode ==  p_zSmlcRawEph->u_Iode) && (p_RawSF1Param->u_Accuracy >= 7) )
  {
  	  p_RawEph->u_predicted = TRUE;
	  p_RawEph->u_predSeedAge = p_zSmlcRawEph->u_predSeedAge;
  }
  else
  {
  	  p_RawEph->u_predicted = FALSE;
	  p_RawEph->u_predSeedAge = 0;
  }

  /* set the toe as valid */
  p_RawEph->toe_expired = FALSE;

  p_zCustom->custom_assist_availability_flag |= C_EPH_AVAIL;

  /**** RECON ADD-ON: Serialize this in /data/gps/aiding/reconephemeris in binary format for pick-up/broadcast via Web Server ***/
#if defined RECON_EPHEMERIS_SERVER
  recon_save_ephemeris (gpsweek, p_RawEph, p_RawSF1Param);
#endif

  /* Populate Eph data for sagps and indicate*/
  gpsc_populate_send_eph_data(p_DBEphemeris);

}
/*
 ******************************************************************************
 * gpsc_db_update_health
 *
 * Function description:
 *
 * This function fills the health database structure with data from Sensor.
 *
 * Parameters:
 *
 *  p_Field - Pointer to received AI2 Field.
 *  p_zGPSCDatabase - Pointer to the LM database
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/
void gpsc_db_update_health( Ai2Field *p_Field, gpsc_db_type *p_zGPSCDatabase )
{
  U8 u_i;
  U8 *p_B = p_Field->p_B;
  gpsc_db_health_type *p_zDBHealth = &p_zGPSCDatabase->z_DBHealth;
  U16 w_UpdateWeek;
  U32 q_UpdateMsec;
  gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
  gpsc_custom_struct* p_zCustom = p_zGPSCControl->p_zCustomStruct;

  STATUSMSG("gpsc_db_update_health: Received RTI from Receiver");


  w_UpdateWeek = (U16) NBytesGet(&p_B,sizeof(U16));
  q_UpdateMsec = NBytesGet(&p_B,sizeof(U32));

  for (u_i=0; u_i<N_SV; u_i++)
  {
    p_zDBHealth->u_AlmHealth[u_i] = *p_B++;
  }
  if (w_UpdateWeek != C_GPS_WEEK_UNKNOWN)
  {
    p_zGPSCDatabase->z_DBHealth.d_UpdateTimeSec =
      FullGpsMs (w_UpdateWeek, q_UpdateMsec) * 0.001;

	p_zCustom->custom_assist_availability_flag |= C_RTI_AVAIL;
  }
  else
  {
    p_zGPSCDatabase->z_DBHealth.d_UpdateTimeSec = -1.0;
  }
}

/*
 ******************************************************************************
 * gpsc_db_update_sv_dir
 *
 * Function description:
 *
 * This function updates the Sv Direction database
 *
 * Parameters:
 *  p_Field - Pointer to an Ai2Field structure
 *  p_zGPSCDatabase - Pointer to the LM database
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/

void gpsc_db_update_sv_dir
(
  Ai2Field*     p_zAi2Field,
  gpsc_db_type*  p_zGPSCDatabase
)
{

  U8  *p_B = p_zAi2Field->p_B;
  U8  u_SvId, u_i;
  U8 u_SvID;
  U16 w_Length;
  U8  u_NumSvs;
  gpsc_db_sv_dir_type  *p_DBSvDirection = &p_zGPSCDatabase->z_DBSvDirection;
  me_SvDirection *p_RawSvDirection;
  w_Length = p_zAi2Field->w_Length;
  u_NumSvs = (U8)(w_Length/3);

  if (w_Length != 0)
  {

      U8 u_VisibleSVs = 0;
//      STATUSMSG("Status: SENSOR Injects %d SV Directions", u_NumSvs);


    for (u_i=0; u_i < u_NumSvs; u_i++)
    {
		u_SvID = (U8)(*p_B & 0xFF);

		if((u_SvID >= 193) && (u_SvID <= 202))
		{
			*p_B++;
		//	STATUSMSG("---QZSS SVIDs suppressed in SV direction Report--- %d ",u_SvID);
			u_i--;
			u_NumSvs--;

			continue;

		}
      u_SvId = *p_B++;

	/*KlocWork Critical Issue:42 Resolved by adding boundary check*/
  if((u_SvId >0) && (u_SvId <= 32))
  {

      p_RawSvDirection = &p_DBSvDirection->z_RawSvDirection[u_SvId - 1];
      p_RawSvDirection->b_Elev = *p_B++;
      p_RawSvDirection->u_Azim = *p_B++;

	  STATUSMSG("gpsc_db_update_sv_dir: svid [%d]->b_Elev = %d ",u_SvId,p_RawSvDirection->b_Elev);

      if (p_RawSvDirection->b_Elev != -128 )
      {
        p_DBSvDirection->u_num_of_sv++;
        {
          FLT f_Elev = (FLT)(C_LSB_ELEV_MASK *(FLT)p_RawSvDirection->b_Elev);
//          S16 s_Elev = (S16) f_Elev;
//          U16 u_Azim = (U16) (C_LSB_AZIM_MASK *(FLT)p_RawSvDirection->u_Azim);

//          STATUSMSG("Data: SV%02d EL%03d AZ%03d", u_SvId, s_Elev, u_Azim);

          if (f_Elev > 5.0) u_VisibleSVs++;
        }
	  }
      }
    }

		STATUSMSG("Status: SENSOR Injected %d Valid, %d Visible SV Directions",p_DBSvDirection->u_num_of_sv, u_VisibleSVs);
  }

}



/*
 ******************************************************************************
 * gpsc_db_update_sv_meas
 *
 * Function description:
 *
 * This function updates the Sv Measurement database
 *
 * Parameters:
 *  p_Field - Pointer to an Ai2Field structure
 *  p_zGPSCDatabase - Pointer to the LM database
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/
void gpsc_db_update_sv_meas(Ai2Field *p_zAi2Field, gpsc_db_type *p_zGPSCDatabase)
{

  gpsc_ctrl_type*	p_zGPSCControl;
  gpsc_sess_cfg_type      *p_zSessConfig;

  gpsc_meas_per_sv_type    *p_zMeasPerSv;

  gpsc_db_gps_meas_type    *p_zDBGpsMeas = &p_zGPSCDatabase->z_DBGpsMeas;
  gpsc_meas_report_type   *p_zMeasReport = &p_zDBGpsMeas->z_MeasReport;

  U8  *p_B = p_zAi2Field->p_B;
  U16 w_Length;
  U8 u_SvIdSvTimeFlag, u_i;
  U16 w_Snr = 0;				/* added to support SBAS control for GPS5350 */
  U8 u_Index = 0;
  U8 u_SvID = 0 ;
  w_Length = p_zAi2Field->w_Length;
  w_Length -=4; /* 4-Byte FCOUNT is not repeated for different SVs */

  p_zGPSCControl = gp_zGPSCControl;
  p_zSessConfig = p_zGPSCControl->p_zSessConfig;

  if (w_Length == 0)
  {
    /* this means no SV, set this database invalid */
    for (u_i = (U8)w_Length; u_i < N_LCHAN; u_i++)
	{
      p_zMeasPerSv = &p_zMeasReport->z_MeasPerSv[u_i];
      p_zMeasPerSv->u_SvIdSvTimeFlag = 0;
	}

    /* mark measurement "database" as invalid */
    p_zMeasReport->u_Valid = FALSE;
	return;
  }

  if (w_Length / 28 * 28 != w_Length)
  {
    return; /* length wrong, not multiple of bytes per Sv */
  }
  w_Length /= 28; /* w_Length now represents number of SVs */

  p_zMeasReport->q_FCount = (U32) NBytesGet(&p_B, sizeof(U32));

  for (u_i=0; u_i < w_Length; u_i++)
  {
	u_SvID = (U8)(*p_B & 0x3F);

	if(((u_SvID >= 120) && (u_SvID <= 138)) ||((u_SvID >= 193) && (u_SvID <= 202)))
    {
	  STATUSMSG("---SVIDs suppressed in measurement--- %d ",u_SvID);
    	/* increment p_B to next sv data, ignoring remaining data of current sv's */
      u_i--; /* Decrement if the SvID that is reported is between 120 - 138 so that
				it doesn`t create any void for the SvID that is suppressed*/
	  w_Length--; /* Decrement so that the total number of Sv`s are counted
					 other then the one that is suppressed for SvID`s from 120 - 138*/
	  continue;
    }


    u_SvIdSvTimeFlag = *p_B++;
    /* ignore measurement reports from sv 120 to 138 - SBAS control */
    w_Snr = (U16)NBytesGet(&p_B, sizeof(U16));

	/*KlocWork Critical Issue:125 Resolved by adding boundary check*/
	if (u_Index < N_LCHAN)
    p_zMeasPerSv = &p_zMeasReport->z_MeasPerSv[u_Index++];

    p_zMeasPerSv->u_SvIdSvTimeFlag = u_SvIdSvTimeFlag;
    /* following added and modified for SBAS control */
    p_zMeasPerSv->w_Snr =	w_Snr;
    p_zMeasPerSv->w_Cno = (U16)NBytesGet(&p_B, sizeof(U16));
    p_zMeasPerSv->x_LatencyMs = (S16)NBytesGet(&p_B, sizeof(S16));
    p_zMeasPerSv->u_PreInt = *p_B++;
    p_zMeasPerSv->w_PostInt = (U16)NBytesGet(&p_B, sizeof(U16));
    p_zMeasPerSv->q_Msec = (U32)NBytesGet(&p_B, sizeof(U32));
    p_zMeasPerSv->q_SubMsec = (U32)NBytesGet(&p_B, 3) & 0x00FFFFFF;
    p_zMeasPerSv->w_SvTimeUnc = (U16)NBytesGet(&p_B, sizeof(U16));
    p_zMeasPerSv->l_SvSpeed = NBytesGet(&p_B, 3);
    if (p_zMeasPerSv->l_SvSpeed &  0x00800000 )
    {
      p_zMeasPerSv->l_SvSpeed |= 0xFF000000; /* sign ext. */
    }

    p_zMeasPerSv->q_SvSpeedUnc = (U16)NBytesGet(&p_B, sizeof(U16));

    p_zMeasPerSv->w_MeasStatus = (U16)NBytesGet(&p_B, sizeof(U16));

    /* disregard the reserved 2 bytes */
    p_B++;
    p_B++;
  }

  /* mark the unused slot with SvId = 0 */
  for (u_i = (U8)w_Length; u_i < N_LCHAN; u_i++)
  {
    p_zMeasPerSv = &p_zMeasReport->z_MeasPerSv[u_i];
    p_zMeasPerSv->u_SvIdSvTimeFlag = 0;
  }

  /* mark measurement "database" as valid */
  p_zMeasReport->u_Valid = TRUE;

  /* update Ref FCount in session DB as well */
  p_zSessConfig->q_FCountMeas = p_zMeasReport->q_FCount;


}
/*
 ******************************************************************************
 * gpsc_db_update_sv_ext_meas
 *
 * Function description:
 *
 * This function updates the Sv Extended Measurement database
 *
 * Parameters:
 *  p_Field - Pointer to an Ai2Field structure
 *  p_zGPSCDatabase - Pointer to the LM database
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/

void gpsc_db_update_sv_ext_meas(Ai2Field *p_zAi2Field, gpsc_db_type *p_zGPSCDatabase)
{
  gpsc_ctrl_type*	p_zGPSCControl;
  gpsc_sess_cfg_type      *p_zSessConfig;

  gpsc_db_gps_meas_type    *p_zDBGpsMeas = &p_zGPSCDatabase->z_DBGpsMeas;
  gpsc_meas_report_type   *p_zMeasReport = &p_zDBGpsMeas->z_MeasReport;
  me_Time   *p_meTime = &p_zMeasReport->z_MeasToa;
  gpsc_meas_per_sv_type    *p_zMeasPerSv;


  U8  *p_B = p_zAi2Field->p_B;
  U16 w_Length,i = 0;
  U8 u_SvID;
  U8 u_SvIdSvTimeFlag, u_i;
  S32 l_Word;
  p_zGPSCControl = gp_zGPSCControl;
  p_zSessConfig = p_zGPSCControl->p_zSessConfig;

STATUSMSG("Status: gpsc_db_update_sv_ext_meas");
p_B++;

if(p_zAi2Field->w_Length > 26)
{
  p_zMeasReport->q_FCount = (U32) NBytesGet(&p_B, sizeof(U32));
  p_meTime->w_GpsWeek = (U16) NBytesGet(&p_B, sizeof(U16));
  p_meTime->q_GpsMsec = (U32) NBytesGet(&p_B, sizeof(U32));
  p_meTime->f_ClkTimeBias = (FLT)(NBytesGet(&p_B, 3) & 0x00FFFFFF);
  p_meTime->f_ClkTimeUncMs = (U16) NBytesGet(&p_B, sizeof(U16));
    l_Word = (S32) NBytesGet( &p_B, sizeof(S32) );
  p_meTime->f_ClkFreqBias =  l_Word * (FLT) 1.0;
  p_meTime->f_ClkFreqUnc = (FLT) NBytesGet(&p_B, sizeof(U32));
  p_zMeasReport->w_SvBlockLen = (U16) NBytesGet(&p_B, sizeof(U16));
}

  w_Length = p_zAi2Field->w_Length;
  w_Length -=26; /* 26-Byte Sub packet ID ,FCOUNT,GPS Week,GPS Millisecond
  					Clock Time Bias, Clock Time Uncertainty,Clock Frequency Bias,
                    Clock Frequency Uncertainty, SV Block Length
 					is not repeated for different SVs */

  STATUSMSG("Status:inside gpsc_db_update_sv_ext_meas");
  if (w_Length == 0)
  {
    /* this means no SV, set this database invalid */
    for (u_i = (U8)w_Length; u_i < N_LCHAN; u_i++)
	{
      p_zMeasPerSv = &p_zMeasReport->z_MeasPerSv[u_i];
      p_zMeasPerSv->u_SvIdSvTimeFlag = 0;
	}

    /* mark measurement "database" as invalid */
    p_zMeasReport->u_Valid = FALSE;
	return;
  }

  if (w_Length / 50 * 50 != w_Length)
  {
    return; /* length wrong, not multiple of bytes per Sv */
  }
  w_Length /= 50; /* w_Length now represents number of SVs */

  for (u_i=0; u_i < w_Length; u_i++)
  {
	u_SvID = (U8)(*p_B & 0x3F);

	if(((u_SvID >= 120) && (u_SvID <= 138)) ||((u_SvID >= 193) && (u_SvID <= 202)))
    {
	  STATUSMSG("---SVIDs suppressed in measurement report ext--- %d ",u_SvID);
    	/* increment p_B to next sv data, ignoring remaining data of current sv's */
      u_i--; /* Decrement if the SvID that is reported is between 120 - 138 so that
				it doesn`t create any void for the SvID that is suppressed*/
	  w_Length--; /* Decrement so that the total number of Sv`s are counted
					 other then the one that is suppressed for SvID`s from 120 - 138*/
	  continue;
    }

    u_SvIdSvTimeFlag = *p_B++;

    p_zMeasPerSv = &p_zMeasReport->z_MeasPerSv[u_i];

    p_zMeasPerSv->u_SvIdSvTimeFlag = u_SvIdSvTimeFlag;

    p_zMeasPerSv->w_Cno = (U16)NBytesGet(&p_B, sizeof(U16));

    p_zMeasPerSv->x_LatencyMs = (S16)NBytesGet(&p_B, sizeof(S16));

    p_zMeasPerSv->u_PreInt = *p_B++;

    p_zMeasPerSv->w_PostInt = (U16)NBytesGet(&p_B, sizeof(U16));

    p_zMeasPerSv->q_Msec = (U32)NBytesGet(&p_B, sizeof(U32));

    p_zMeasPerSv->q_SubMsec = (U32)NBytesGet(&p_B, 3) & 0x00FFFFFF;

    p_zMeasPerSv->w_SvTimeUnc = (U16)NBytesGet(&p_B, sizeof(U16));

    p_zMeasPerSv->l_SvSpeed = NBytesGet(&p_B, 3);
    if (p_zMeasPerSv->l_SvSpeed &  0x00800000 )
    {
      p_zMeasPerSv->l_SvSpeed |= 0xFF000000; /* sign ext. */
    }

    p_zMeasPerSv->q_SvSpeedUnc = (U32) (NBytesGet(&p_B, 3) & 0x00FFFFFF);

    p_zMeasPerSv->w_MeasStatus = (U16)NBytesGet(&p_B, sizeof(U16));

    p_zMeasPerSv->u_ChannelMeasState = *p_B++;

    p_zMeasPerSv->q_AccCarrierPhase = (S32) NBytesGet(&p_B, sizeof(S32));

    p_zMeasPerSv->q_CarrierVelocity = (S32) NBytesGet(&p_B, sizeof(S32));

    p_zMeasPerSv->w_CarrierAcc = (S16)NBytesGet(&p_B, sizeof(S16));
	if (p_zMeasPerSv->w_CarrierAcc &  0x00008000 )
    {
		p_zMeasPerSv->w_CarrierAcc |= 0xFFFF0000; /* sign ext. */
	}

    p_zMeasPerSv->u_LossLockInd = *p_B++;

    p_zMeasPerSv->w_Snr = (U16)NBytesGet(&p_B, sizeof(U16));

    p_zMeasPerSv->u_GoodObvCount = *p_B++;

    p_zMeasPerSv->u_TotalObvCount = *p_B++;

    /* disregard the reserved 13 bytes */

    for (i=0; i<9 ;i++)
    {
      p_B++;
    }

  }

  /* mark the unused slot with SvId = 0 */
  for (u_i = (U8)w_Length; u_i < N_LCHAN; u_i++)
  {
    p_zMeasPerSv = &p_zMeasReport->z_MeasPerSv[u_i];
    p_zMeasPerSv->u_SvIdSvTimeFlag = 0;
  }

  /* mark measurement "database" as valid */
  p_zMeasReport->u_Valid = TRUE;


  /* update Ref FCount in session DB as well */
  p_zSessConfig->q_FCountMeas = p_zMeasReport->q_FCount;



}


/*
 ******************************************************************************
 * gpsc_db_update_sv_meas_stat
 *
 * Function description:
 *
 * This function updates the Sv Measurement Status database
 *
 * Parameters:
 *  p_Field - Pointer to an Ai2Field structure
 *  p_zGPSCDatabase - Pointer to the LM database
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/

void gpsc_db_update_sv_meas_stat
(
  Ai2Field*     p_zAi2Field,
  gpsc_db_type*  p_zGPSCDatabase
)
{
  gpsc_db_gps_meas_type*       p_zDBGpsMeas = &p_zGPSCDatabase->z_DBGpsMeas;

  gpsc_meas_stat_report_type*  p_zMeasStatReport =
                                &p_zDBGpsMeas->z_MeasStatReport;

  gpsc_meas_stat_per_sv_type*  p_zMeasStatPerSv;
  gpsc_last_meas_obsvd_type*   p_zLastMeasObsvd;


  U8 u_i;
  U8 u_SvID;
  U8 *p_B = p_zAi2Field->p_B;
  U16 w_Length;
  U16 w_ReportCounter = 0;

  char buffer[120],*p_b = &buffer[0];

  w_Length = p_zAi2Field->w_Length;
  w_Length -= 4; /* 4-byte FCOUNT does not repeat for each SV */

  if (w_Length == 0 )
  {
    gpsc_db_clr_meas_stat(p_zMeasStatReport);
	return;
  }


  if (w_Length / 10 * 10 != w_Length)
  {
    return; /* length wrong, not multiple of bytes per Sv */
  }
  w_Length /= 10; /* w_Length now represents number of SVs */


  p_zMeasStatReport->q_FCount = NBytesGet(&p_B, sizeof(U32) );

  p_b+= sprintf(p_b,"Data: MeasStat: Sv,CNO,Chan,Status,OC,GOC|");
  w_ReportCounter = 2;
  for ( u_i = 0; u_i < w_Length; u_i++)
  {
	u_SvID = (U8)(*p_B & 0xFF);
	if(((u_SvID >= 120) && (u_SvID <= 138)) ||((u_SvID >= 193) && (u_SvID <= 202)))
    {
	  *p_B++;
	  STATUSMSG("---SVIDs suppressed in measurement status--- %d ",u_SvID);
    	/* increment p_B to next sv data, ignoring remaining data of current sv's */
      u_i--; /* Decrement if the SvID that is reported is between 120 - 138 so that
				it doesn`t create any void for the SvID that is suppressed*/
	  w_Length--; /* Decrement so that the total number of Sv`s are counted
					 other then the one that is suppressed for SvID`s from 120 - 138*/
	  continue;
    }
    p_zMeasStatPerSv = &p_zMeasStatReport->z_MeasStatPerSv[u_i];

    p_zMeasStatPerSv->u_Sv = *p_B++;

    p_zMeasStatPerSv->x_LatencyMs = (S16)NBytesGet(&p_B, sizeof(S16));
    p_zMeasStatPerSv->w_CnoDB = (U16)NBytesGet(&p_B, sizeof(U16));
    p_zMeasStatPerSv->u_ChanState = *p_B++;
    p_zMeasStatPerSv->u_GoodObs = *p_B++;
    p_zMeasStatPerSv->u_Observe = *p_B++;
    p_zMeasStatPerSv->w_MeasStatus = (U16)NBytesGet(&p_B, sizeof(U16));

	if(w_ReportCounter==5)
	{
		STATUSMSG(buffer);
		p_b = &buffer[0];
		p_b+= sprintf(p_b,"MeasStat : ");
		w_ReportCounter  =0;
	}

	p_b+= sprintf(p_b,"%u,%u,%u,%X,%u,%u|",
					p_zMeasStatPerSv->u_Sv,
					p_zMeasStatPerSv->w_CnoDB,
					p_zMeasStatPerSv->u_ChanState,
					p_zMeasStatPerSv->w_MeasStatus,
					p_zMeasStatPerSv->u_Observe,
					p_zMeasStatPerSv->u_GoodObs);

	w_ReportCounter++;
  }

  /* mark the unused slot with SvId = 0 */
  for ( u_i = (U8)w_Length; u_i < N_LCHAN; u_i++)
  {
    p_zMeasStatPerSv = &p_zMeasStatReport->z_MeasStatPerSv[u_i];
    p_zMeasStatPerSv->u_Sv = 0;
  }


  /* if first valid meas. status since steering injection */
  if (p_zDBGpsMeas->u_init_last)
  {
    p_zLastMeasObsvd = &p_zDBGpsMeas->z_LastMeasObsvd[0];
    p_zMeasStatPerSv = &p_zMeasStatReport->z_MeasStatPerSv[0];

    for ( u_i=0; u_i<w_Length; u_i++, p_zLastMeasObsvd++, p_zMeasStatPerSv++)
    {
      p_zLastMeasObsvd->u_Sv      = p_zMeasStatPerSv->u_Sv;
      p_zLastMeasObsvd->u_Observe = p_zMeasStatPerSv->u_Observe;
      p_zLastMeasObsvd->u_GoodObs = p_zMeasStatPerSv->u_GoodObs;
    }

    p_zDBGpsMeas->u_init_last = FALSE;

  }
  //STATUSMSG(buffer);
  /* mark measurement status "database" as valid */
  p_zMeasStatReport->u_Valid = TRUE;
}


/*
 ******************************************************************************
 * gpsc_db_is_pos_ok
 *
 * Function description:
 *
 * This function check if position uncertainty is below a threshold
 *
 * Parameters:
 *  p_zDBPos: pointer to postion database
 *  d_CurrentAccGpsSec: current GPS second
 *  u_threshold: position uncertainty threshold
 *
 * Return value:
 *  TRUE: position uncertainty less than the threshold
 *  FALSE: position uncertainty greather than or equal to the threshold
 *
 ******************************************************************************
*/

U8 gpsc_db_is_pos_ok
(
  gpsc_db_pos_type*  p_zDBPos,
  DBL               d_CurrentAccGpsSec,
  U32               q_threshold
)

{

  U8 u_ret = FALSE;
  FLT f_EastUnc, f_NorthUnc, f_PosUnc;
  DBL d_PosAgeSec;

  if ( p_zDBPos->d_ToaSec!= 0 )
  {
    d_PosAgeSec = d_CurrentAccGpsSec - p_zDBPos->d_ToaSec;

    f_EastUnc = (FLT)p_zDBPos->z_ReportPos.w_EastUnc * (FLT)C_LSB_EER;
    f_NorthUnc = (FLT)p_zDBPos->z_ReportPos.w_NorthUnc * (FLT)C_LSB_NER;

    f_PosUnc = (FLT)ext_sqrt( f_EastUnc * f_EastUnc + f_NorthUnc * f_NorthUnc );

    /* increament f_Unc based on the age of the fix */
    f_PosUnc += (FLT)d_PosAgeSec * (FLT)C_POS_UNC_GROWTH;
    STATUSMSG("Status: Chk pos: %f d_PosAgeSec %f f_EastUnc %f f_NorthUnc %f",p_zDBPos->d_ToaSec,d_PosAgeSec,f_EastUnc,f_NorthUnc,f_PosUnc);

    if (f_PosUnc < (FLT)q_threshold )
    {
      u_ret = TRUE; /* position is good enough for the purpose */

      STATUSMSG("Status: Chk pos: OK");

    }

    else
	{

	  STATUSMSG("Data: Chk pos BAD: CurrRTCSec=%lf, posDBTimestamp=%lf, posAgeSec=%lf, posUnc=%f threshold=%f",
	          d_CurrentAccGpsSec,  p_zDBPos->d_ToaSec, d_PosAgeSec, f_PosUnc, (FLT)q_threshold);

    }
  }

  else
  {
	  STATUSMSG("Status: Chk pos: no pos db");
  }

  return u_ret;

}


/*
 ******************************************************************************
 * gpsc_db_get_num_of_eph
 *
 * Function description:
 *
 * This function count number SVs with ephemeris
 *
 * Parameters:
 *  p_zGPSCControl - Pointer to structure that hold data used throughout LM
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/

U8 gpsc_db_get_num_of_eph(gpsc_ctrl_type* p_zGPSCControl)
{
  gpsc_db_type* p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
  gpsc_db_eph_type* p_zDBEphemeris;

  U8 u_num_of_eph = 0;
  U8 u_i;

  for ( u_i=0; u_i < N_SV; u_i++)
  {
    p_zDBEphemeris = &p_zGPSCDatabase->z_DBEphemeris[u_i];

  if ( p_zDBEphemeris->u_Valid)
  {
    u_num_of_eph++;
  }
  }

  return u_num_of_eph;

}


/*
 ******************************************************************************
 * gpsc_db_clr_meas_stat
 *
 * Function description:
 *
 * This function clears measurement status database.
 *
 * Parameters:
 *
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/

void gpsc_db_clr_meas_stat(  gpsc_meas_stat_report_type*  p_zMeasStatReport)
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
  gpsc_meas_stat_per_sv_type*  p_zMeasStatPerSv;
  U8 u_i;

  for ( u_i = 0; u_i < N_LCHAN; u_i++)
  {
    p_zMeasStatPerSv = &p_zMeasStatReport->z_MeasStatPerSv[u_i];
    mcpf_mem_set(p_zGPSCControl->p_zSysHandlers->hMcpf, (void *)p_zMeasStatPerSv,0, sizeof(*p_zMeasStatPerSv));
  }
   p_zMeasStatReport->u_Valid = FALSE;
}


/*
 ******************************************************************************
 * gpsc_db_upd_meas_hist
 *
 * Function description:
 *
 * This function updates measurement status history list
 *
 * Parameters:
 *
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/
void gpsc_db_upd_meas_hist( gpsc_db_gps_meas_type*  p_zDBGpsMeas )
{

  gpsc_meas_stat_report_type*  p_zMeasStatReport =
                                &p_zDBGpsMeas->z_MeasStatReport;

  gpsc_meas_stat_per_sv_type*  p_zMeasStatPerSv;
  gpsc_last_meas_obsvd_type*   p_zLastMeasObsvd;
  U8 u_i;

  for ( u_i = 0; u_i < N_LCHAN; u_i++ )
  {
      p_zLastMeasObsvd = &p_zDBGpsMeas->z_LastMeasObsvd[u_i];
      p_zMeasStatPerSv = &p_zMeasStatReport->z_MeasStatPerSv[u_i];
	  p_zLastMeasObsvd->u_Sv = p_zMeasStatPerSv->u_Sv;
	  p_zLastMeasObsvd->u_Observe = p_zMeasStatPerSv->u_Observe;
	  p_zLastMeasObsvd->u_GoodObs = p_zMeasStatPerSv->u_GoodObs;
  }

}


/*
 ******************************************************************************
 * gpsc_db_save_to_nvs
 *
 * Function description:
 *
 * This function saved the Database to flash for retrival later
 *
 * Parameters:
 *  p_zGPSCDatabase - Pointer to the GPSC database
 *
 * Return value:
 *  Boolean
 *
 ******************************************************************************
*/
U8 gpsc_db_save_to_nvs(gpsc_db_type *p_zGPSCDatabase)
{
  gpsc_nvs_update_pos(p_zGPSCDatabase);
  gpsc_nvs_update_eph(p_zGPSCDatabase);
  gpsc_nvs_update_iono(p_zGPSCDatabase);
  gpsc_nvs_update_utc(p_zGPSCDatabase);
  gpsc_nvs_update_alm(p_zGPSCDatabase);
  gpsc_nvs_update_health(p_zGPSCDatabase);
  gpsc_nvs_update_clock_uncertanity(p_zGPSCDatabase);
  gpsc_nvs_update_tcxo(p_zGPSCDatabase);
  return TRUE;
}

/*
 ******************************************************************************
 * gpsc_nvs_update_pos
 *
 * Function description:
 *
 * This function updates the NVS record of position,
 *
 * Parameters:
 *  p_zGPSCDatabase - Pointer to the GPSC database
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/
void gpsc_nvs_update_pos (gpsc_db_type *p_zGPSCDatabase)
{
	gpsc_db_pos_type *p_DBPos = &p_zGPSCDatabase->z_DBPos;
	FLT  f_PosUnc;
	FLT  f_EastUnc;
	FLT  f_NorthUnc;
	 gpsc_sys_handlers*	p_GPSCSysHandlers;
	gpsc_nv_pos_type  z_NvPos;
	gpsc_nv_pos_type  *p_DBNvPos  = &p_zGPSCDatabase->z_NvsPos;
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	McpS32	fd;
	//klocwork - ABR
	  McpU8 uAidingFile[C_MAX_AIDING_FILENAME_LEN+20];

	p_GPSCSysHandlers = p_zGPSCControl->p_zSysHandlers;

	/* Make sure we only store a good fix into NVS file. 
 	   See Response Packet for Report Position AI2 command for
           position flag description */
	if (p_DBPos->z_ReportPos.w_PositionFlags & 0x1C87)
	{
		z_NvPos.l_PosLat = p_DBPos->z_ReportPos.l_Lat;
		z_NvPos.l_PosLong = p_DBPos->z_ReportPos.l_Long;
		z_NvPos.x_PosHt = p_DBPos->z_ReportPos.x_Height;

		f_EastUnc = p_DBPos->z_ReportPos.w_EastUnc * (FLT) C_LSB_EER;
		f_NorthUnc = p_DBPos->z_ReportPos.w_NorthUnc * (FLT) C_LSB_NER;
		f_PosUnc = (FLT) ext_sqrt((f_EastUnc * f_EastUnc) +
							  (f_NorthUnc * f_NorthUnc));
		z_NvPos.f_PosUnc = f_PosUnc;
		z_NvPos.d_ToaSec = p_DBPos->d_ToaSec;

		// Don't save position aiding data if uncertanty more than 100m or position is 0 0 0
		if(f_PosUnc > 100 )
			return;

		MCP_HAL_STRING_StrCpy (uAidingFile,(McpU8*)p_GPSCSysHandlers->uAidingPath);
		MCP_HAL_STRING_StrCat (uAidingFile,C_STR_AID_POSITION_FILE);
		if (mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *) uAidingFile,
							MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_WRONLY | MCP_HAL_FS_O_CREATE, &fd) == RES_OK)
		{
			if ( mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, fd,
								(void *)&z_NvPos, sizeof(z_NvPos)) != sizeof(z_NvPos) )
			{
				 /* keep a RAM copy of what gets written to NVS */
				mcpf_mem_copy(p_zGPSCControl->p_zSysHandlers->hMcpf, p_DBNvPos, &z_NvPos, sizeof(z_NvPos) );
			}
			mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
			ALOGI("+++ Updated [%s] Binary Position File. Data: +++\n", uAidingFile);
		}
		else
		{
			STATUSMSG("gpsc_nvs_update_pos: Failed to open file!");
		}
	}
}
/*
 ******************************************************************************
 * gpsc_nvs_update_eph
 *
 * Function description:
 *
 * This function updates the NVS record of ephemeris.
 *
 * Parameters:
 *  p_zGPSCDatabase - Pointer to the GPSC database
 *
 * Return value:
 *  None
 *
 *
 ******************************************************************************
*/
void gpsc_nvs_update_eph (gpsc_db_type* p_zGPSCDatabase)
{
	gpsc_sys_handlers*	p_GPSCSysHandlers = 0;
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	McpS32	fd = 0;
   McpU8 uAidingFile[C_MAX_AIDING_FILENAME_LEN+20];

	p_GPSCSysHandlers = p_zGPSCControl->p_zSysHandlers;

	MCP_HAL_STRING_StrCpy (uAidingFile,(McpU8*)p_GPSCSysHandlers->uAidingPath);
	MCP_HAL_STRING_StrCat (uAidingFile,C_STR_AID_EPH_FILE);

	if (mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *)uAidingFile,
						MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_WRONLY | MCP_HAL_FS_O_CREATE, &fd) == RES_OK)
	{
		mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, fd,
							(void *)&p_zGPSCDatabase->z_DBEphemeris[0], (sizeof(gpsc_db_eph_type) * N_SV) );
		mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);

		ALOGI("+++ Updated [%s] Binary Ephemeris File. Data: +++\n", uAidingFile);
      gpsc_print_ephemeris(p_zGPSCDatabase);
	}
	else
	{
		ALOGE("gpsc_nvs_update_eph: Failed to open file!");
	}
}

/*
 ******************************************************************************
 * gpsc_nvs_update_iono
 *
 * Function description:
 *
 * This function logs incoming Iono data to a file for use when injecting aiding
 * information into the receiver at startup
 *
 * Parameters:
 *  p_zGPSCDatabase - Pointer to the LM database
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/
void  gpsc_nvs_update_iono(gpsc_db_type *p_GPSCDatabase)
{
	gpsc_sys_handlers*	p_GPSCSysHandlers;
	gpsc_db_iono_type    *p_DBIono = &p_GPSCDatabase->z_DBIono;
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	McpS32	fd;
//klocwork
	McpU8 uAidingFile[C_MAX_AIDING_FILENAME_LEN+20];

	p_GPSCSysHandlers = p_zGPSCControl->p_zSysHandlers;

	MCP_HAL_STRING_StrCpy (uAidingFile,(McpU8*)p_GPSCSysHandlers->uAidingPath);
	MCP_HAL_STRING_StrCat (uAidingFile,C_STR_AID_IONO_FILE);
	if (mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *)uAidingFile,
						MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_WRONLY | MCP_HAL_FS_O_CREATE, &fd) == RES_OK)
	{
		mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, fd,
							(void *)p_DBIono, sizeof(gpsc_db_iono_type) );
		mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
		ALOGI("+++ Updated [%s] Binary Iono  File. Data: +++\n", uAidingFile);
	}
	else
	{
		STATUSMSG("gpsc_nvs_update_iono: Failed to open file!");
	}
}

/*
 ******************************************************************************
 * gpsc_nvs_update_utc
 *
 * Function description:
 *
 * This function logs incoming UTC data to a file for use when injecting aiding
 * information into the receiver at startup
 *
 * Parameters:
 *  p_zGPSCDatabase - Pointer to the LM database
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/

 void gpsc_nvs_update_utc(gpsc_db_type *p_zGPSCDatabase)
 {

	 gpsc_sys_handlers*	p_GPSCSysHandlers;
	 gpsc_db_utc_type *p_DBUtc = &p_zGPSCDatabase->z_DBUtc;
	 gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	 McpS32	fd;
 	 McpU8 uAidingFile[C_MAX_AIDING_FILENAME_LEN+20];

  	 p_GPSCSysHandlers = p_zGPSCControl->p_zSysHandlers;

	MCP_HAL_STRING_StrCpy (uAidingFile,(McpU8*)p_GPSCSysHandlers->uAidingPath);
	MCP_HAL_STRING_StrCat (uAidingFile,C_STR_AID_UTC_FILE);
	 if (mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *)uAidingFile,
	 						MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_WRONLY | MCP_HAL_FS_O_CREATE, &fd) == RES_OK)
	 {
	 	mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, fd,
	 						(void *)p_DBUtc, sizeof(gpsc_db_utc_type) );
	 	mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
		ALOGI("+++ Updated [%s] Binary UTC File. Data: +++\n", uAidingFile);
	 }
	 else
	 {
	 		STATUSMSG("gpsc_nvs_update_UTC: Failed to open file!");
	 }


 }


/*
 ******************************************************************************
 * gpsc_nvs_update_alm
 *
 * Function description:
 *
 * This function update the NVS copy of the almanac
 *
 * Parameters:
 *  p_zGPSCDatabase: pointer to the database structure.
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/

void gpsc_nvs_update_alm (gpsc_db_type *p_zGPSCDatabase)
{
	gpsc_sys_handlers*	p_GPSCSysHandlers;
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	McpS32	fd;
	//klocwork
 	 McpU8 uAidingFile[C_MAX_AIDING_FILENAME_LEN+20];

  	 p_GPSCSysHandlers = p_zGPSCControl->p_zSysHandlers;

	MCP_HAL_STRING_StrCpy (uAidingFile,(McpU8*)p_GPSCSysHandlers->uAidingPath);
	MCP_HAL_STRING_StrCat (uAidingFile,C_STR_AID_ALMANAC_FILE);

	/* print almanacv data */
	// gpsc_print_almanac(p_zGPSCDatabase);

	if (mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *) uAidingFile,
						MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_WRONLY | MCP_HAL_FS_O_CREATE, &fd) == RES_OK)
	{
		mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, fd,
							(void *)&p_zGPSCDatabase->z_DBAlmanac[0], (sizeof(gpsc_db_alm_type)*N_SV) );
		mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
		ALOGI("+++ Updated [%s] Binary Almanac File. Data: +++\n", uAidingFile);
	}
	else
	{
		STATUSMSG("gpsc_nvs_update_alm: Failed to open file!");
	}
}

/*
 ******************************************************************************
 * gpsc_nvs_update_health
 *
 * Function description:
 *
 * This function update the NVS copy of the health
 *
 * Parameters:
 *  p_zGPSCDatabase: pointer to the database structure.
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/
void gpsc_nvs_update_health (gpsc_db_type *p_zGPSCDatabase)
{

	gpsc_sys_handlers*	p_GPSCSysHandlers;
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	McpS32	fd;
	//klocwork - ABR overflow
 	McpU8 uAidingFile[C_MAX_AIDING_FILENAME_LEN+20];

  	 p_GPSCSysHandlers = p_zGPSCControl->p_zSysHandlers;

	MCP_HAL_STRING_StrCpy (uAidingFile,(McpU8*)p_GPSCSysHandlers->uAidingPath);
	MCP_HAL_STRING_StrCat (uAidingFile,C_STR_AID_HEALTH_FILE);
	if(mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *) uAidingFile,
						MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_WRONLY | MCP_HAL_FS_O_CREATE, &fd) == RES_OK)
	{
		mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, fd,
							(void *)&p_zGPSCDatabase->z_DBHealth, sizeof(gpsc_db_health_type)); /* Klocwork Changes */
		mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
		ALOGI("+++ Updated [%s] Binary Health File. Data: +++\n", uAidingFile);
	}
	else
	{
		STATUSMSG("gpsc_nvs_update_health : Failed to open file!");
	}
}

/*
 ******************************************************************************
 * gpsc_nvs_update_tcxo
 *
 * Function description:
 *
 * This function updates the NVS record of tcxo param.
 *
 * Parameters:
 *  p_zGPSCDatabase - Pointer to the GPSC database
 *
 * Return value:
 *  None
 *
 *
 ******************************************************************************
*/
void gpsc_nvs_update_tcxo (gpsc_db_type *p_zGPSCDatabase)
{
	gpsc_sys_handlers*	p_GPSCSysHandlers;
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	gpsc_db_pos_type*          p_DBPos          = &p_zGPSCDatabase->z_DBPos;
    gpsc_report_pos_type*      p_ReportPos      = &p_DBPos->z_ReportPos;
	McpS32	fd, shrt_term_frUnc;
	//klocwork - adding 20 more as size
 	McpU8 uAidingFile[C_MAX_AIDING_FILENAME_LEN+20];
	gpsc_cfg_type*   p_zGPSCConfig  = p_zGPSCControl->p_zGPSCConfig;
	gpsc_db_gps_inj_clk_type z_DBGpsInjClock;


  	 p_GPSCSysHandlers = p_zGPSCControl->p_zSysHandlers;

	shrt_term_frUnc = (U32) (p_zGPSCConfig->shrt_term_gps_clk_unc * LIGHT_SEC * 1e-6);

	MCP_HAL_STRING_StrCpy (uAidingFile,(McpU8*)p_GPSCSysHandlers->uAidingPath);
	MCP_HAL_STRING_StrCat (uAidingFile,C_STR_AID_TCXO_FILE);

	/* The TCXOFile is updated only when clock is valid and
	     * the fr_unc reported is better than the short term clock uncertainity stored in configuration file
	     *
	     * If receiver has given out a 3D-Fix, only then,
	     *            write bias along with short term unc to NVS file for use in next fix attempt.
	     * If bias determined by receiver is outside,
	     *            don't update NVS.
	     * For MSA cases - As of now we only depend on modem clock for callibration
     */
	if (  (p_zGPSCDatabase->z_DBGpsClock.u_Valid) &&
	      (p_zGPSCDatabase->z_DBGpsClock.f_FreqUnc  < shrt_term_frUnc) &&
	      (0 != p_DBPos->d_ToaSec) &&
		  (NF_FIX_3D == (p_ReportPos->w_PositionFlags & NF_FIX_3D)) &&
          (NF_VELOCITY_FIX == (p_ReportPos->w_PositionFlags & NF_VELOCITY_FIX)) )
	{
		z_DBGpsInjClock.u_Valid = TRUE;
		z_DBGpsInjClock.l_FreqBiasRaw = p_zGPSCDatabase->z_DBGpsClock.f_FreqBias;
		z_DBGpsInjClock.q_FreqUncRaw = p_zGPSCDatabase->z_DBGpsClock.f_FreqUnc;

		if (mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *) uAidingFile,
							MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_WRONLY | MCP_HAL_FS_O_CREATE, &fd) == RES_OK)
		{
			mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, fd,
							(void *)&z_DBGpsInjClock, sizeof(gpsc_db_gps_inj_clk_type));
			mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
			ALOGI("+++ Updated [%s] Binary TCXO File. Data: +++\n", uAidingFile);
		}
		else
		{
			STATUSMSG("gpsc_nvs_update_tcxo: Failed to open file!");
		}

		STATUSMSG(" gpsc_nvs_update_tcxo: Updating NVM - u_Valid = %d, l_FreqBiasRaw = %d, q_FreqUncRaw = %d",
		                                                  z_DBGpsInjClock.u_Valid,
		                                                  z_DBGpsInjClock.l_FreqBiasRaw,
		                                                  z_DBGpsInjClock.q_FreqUncRaw);

	    STATUSMSG(" gpsc_nvs_update_tcxo: shrt_term_frUnc = %ld, CLK-FC = %ld, POS_REPORT_FC = %ld",
		                                                  shrt_term_frUnc,
		                                                  p_zGPSCDatabase->z_DBGpsClock.q_FCount,
                                                          p_ReportPos->q_RefFCount);
	}
	else
	{
		STATUSMSG("gpsc_nvs_update_tcxo: Not updated NVM CLK Valid: %d, as f_FreqUnc is not better - %ld, Shrt_Term_Unc: %ld",
				p_zGPSCDatabase->z_DBGpsClock.u_Valid,
				p_zGPSCDatabase->z_DBGpsClock.f_FreqUnc,
				shrt_term_frUnc );
	}
}


void gpsc_db_clear_clock(gpsc_ctrl_type*  p_zGPSCControl)
{
   gpsc_db_type*  p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
   gpsc_db_gps_clk_type*  p_DBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;

    p_DBGpsClock->w_GpsWeek = C_GPS_WEEK_UNKNOWN;
    p_DBGpsClock->u_Valid = FALSE;
    p_DBGpsClock->u_NumInvalids = 0;
}

U32 gpsc_db_gps_time_in_secs(gpsc_ctrl_type*  p_zGPSCControl)
{
   gpsc_db_type*  p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
   gpsc_db_gps_clk_type*  p_DBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;
   U32 q_GPSTimeSecs;

   if(p_DBGpsClock->u_Valid == FALSE || p_DBGpsClock->w_GpsWeek == C_GPS_WEEK_UNKNOWN||
	   p_DBGpsClock->f_TimeUnc >  C_MAX_VALID_TIME_UNC_MS)
	   return FALSE;

  q_GPSTimeSecs = (U32)((p_DBGpsClock->w_GpsWeek* WEEK_SECS) + (p_DBGpsClock->q_GpsMs/1000));
  return q_GPSTimeSecs;
}

U16 gpsc_db_gps_week(gpsc_ctrl_type*  p_zGPSCControl)
{
   gpsc_db_type*  p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
   gpsc_db_gps_clk_type*  p_DBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;

   if(p_DBGpsClock->u_Valid == FALSE || p_DBGpsClock->w_GpsWeek == C_GPS_WEEK_UNKNOWN||
	   p_DBGpsClock->f_TimeUnc >  C_MAX_VALID_TIME_UNC_MS)
	   return FALSE;

  return p_DBGpsClock->w_GpsWeek;
}

/*
 ******************************************************************************
 * gpsc_nvs_update_clock_uncertanity
 *
 * Function description:
 *
 * This function updates the NVS record of clock uncertanity,
 *
 * Parameters:
 *  p_zGPSCDatabase - Pointer to the GPSC database
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/
void gpsc_nvs_update_clock_uncertanity (gpsc_db_type *p_zGPSCDatabase)
{
	gpsc_sys_handlers*	p_GPSCSysHandlers;
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	McpS32	fd;
	McpS32   fd1;
	//klocwork - Array Buffer overflow
 	McpU8 uAidingFile[C_MAX_AIDING_FILENAME_LEN+20];
 	McpU8 uAidingFile1[C_MAX_AIDING_FILENAME_LEN+20];

  	 p_GPSCSysHandlers = p_zGPSCControl->p_zSysHandlers;

	MCP_HAL_STRING_StrCpy (uAidingFile,(McpU8*)p_GPSCSysHandlers->uAidingPath);
	MCP_HAL_STRING_StrCat (uAidingFile,C_STR_AID_CLOCK_FILE);


	if (mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *) uAidingFile,
		MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_WRONLY | MCP_HAL_FS_O_CREATE, &fd) == RES_OK)
	{
		  //for updating gps clock info and system time
	        if(p_zGPSCDatabase->z_DBGpsClock.u_Valid != 0)
	        {
			mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, fd,
				(void *)&p_zGPSCDatabase->z_DBGpsClock, sizeof(gpsc_db_gps_clk_type) );

			MCP_HAL_STRING_StrCpy (uAidingFile1,(McpU8*)p_GPSCSysHandlers->uAidingPath);
			MCP_HAL_STRING_StrCat (uAidingFile1,C_STR_AID_SYSCLOCK_FILE);

			McpU32 systime = os_get_current_time_inSec(NULL);
			if (mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *) uAidingFile1,
					MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_WRONLY | MCP_HAL_FS_O_CREATE, &fd1) == RES_OK)
			{
				mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, fd1,
						(void *)&systime, sizeof(systime));

				STATUSMSG("gpsc_nvs_update_clock_uncertanity: systime: %d",systime);
				mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd1);
			}
			else
			{
				STATUSMSG("gpsc_nvs_update_clock_uncertanity: SysClock file");
			}
	        }
	        mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);
	        //for updating gps clock info and system time
	}
	else
	{
		STATUSMSG("gpsc_nvs_update_clock_uncertanity: Failed to open file!");
	}

}


void gpsc_print_ephemeris(gpsc_db_type* p_zGPSCDatabase)
{
   gpsc_db_eph_type* p_zDBEphemeris;

   U8 u_num_of_eph = 0;
   U8 u_i = 0;
   U8 u_SvCnt = p_zGPSCDatabase->u_EphCounter;

   for (u_i = 0; u_i < N_SV; u_i++)
   {
      p_zDBEphemeris = &p_zGPSCDatabase->z_DBEphemeris[u_i];

      if (p_zDBEphemeris->u_Valid)
      {
         u_num_of_eph++;

         STATUSMSG("Sv: %d Storing Epheremis Data.", u_i);

         STATUSMSG("p_zRawEphemeris update time %f ", p_zDBEphemeris->z_RawEphemeris.d_UpdateTime);
         STATUSMSG("p_zRawEphemeris q_E %d ", p_zDBEphemeris->z_RawEphemeris.q_E);
         STATUSMSG("p_zRawEphemeris q_IZero %d ", p_zDBEphemeris->z_RawEphemeris.q_IZero);
         STATUSMSG("p_zRawEphemeris q_MZero %d ", p_zDBEphemeris->z_RawEphemeris.q_MZero);
         STATUSMSG("p_zRawEphemeris q_Omega %d ", p_zDBEphemeris->z_RawEphemeris.q_Omega);
         STATUSMSG("p_zRawEphemeris q_OmegaDot %d ", p_zDBEphemeris->z_RawEphemeris.q_OmegaDot);
         STATUSMSG("p_zRawEphemeris q_OmegaZero %d ", p_zDBEphemeris->z_RawEphemeris.q_OmegaZero);
         STATUSMSG("p_zRawEphemeris q_SqrtA %d ", p_zDBEphemeris->z_RawEphemeris.q_SqrtA);
         STATUSMSG("p_zRawEphemeris u_Iode %d ", p_zDBEphemeris->z_RawEphemeris.u_Iode);
         STATUSMSG("p_zRawEphemeris u_Prn %d ", p_zDBEphemeris->z_RawEphemeris.u_Prn);
         STATUSMSG("p_zRawEphemeris w_Cic %d ", p_zDBEphemeris->z_RawEphemeris.w_Cic);
         STATUSMSG("p_zRawEphemeris w_Cis %d ", p_zDBEphemeris->z_RawEphemeris.w_Cis);
         STATUSMSG("p_zRawEphemeris w_Crc %d ", p_zDBEphemeris->z_RawEphemeris.w_Crc);
         STATUSMSG("p_zRawEphemeris w_Crs %d ", p_zDBEphemeris->z_RawEphemeris.w_Crs);
         STATUSMSG("p_zRawEphemeris w_Cuc %d ", p_zDBEphemeris->z_RawEphemeris.w_Cuc);
         STATUSMSG("p_zRawEphemeris w_Cus %d ", p_zDBEphemeris->z_RawEphemeris.w_Cus);
         STATUSMSG("p_zRawEphemeris w_DeltaN %d ", p_zDBEphemeris->z_RawEphemeris.w_DeltaN);
         STATUSMSG("p_zRawEphemeris w_IDot %d ", p_zDBEphemeris->z_RawEphemeris.w_IDot);
         STATUSMSG("p_zRawEphemeris w_Toe %d ", p_zDBEphemeris->z_RawEphemeris.w_Toe);
         STATUSMSG("p_zRawSF1Param  d_FullToe %f", p_zDBEphemeris->z_RawSF1Param.d_FullToe);
         STATUSMSG("p_zRawSF1Param q_Af0 %d", p_zDBEphemeris->z_RawSF1Param.q_Af0);
         STATUSMSG("p_zRawSF1Param u_Accuracy %d", p_zDBEphemeris->z_RawSF1Param.u_Accuracy);
         STATUSMSG("p_zRawSF1Param u_Af2 %d", p_zDBEphemeris->z_RawSF1Param.u_Af2);
         STATUSMSG("p_zRawSF1Param u_CodeL2 %d", p_zDBEphemeris->z_RawSF1Param.u_CodeL2);
         STATUSMSG("p_zRawSF1Param u_Health %d", p_zDBEphemeris->z_RawSF1Param.u_Health);
         STATUSMSG("p_zRawSF1Param u_Tgd %d", p_zDBEphemeris->z_RawSF1Param.u_Tgd);
         STATUSMSG("p_zRawSF1Param w_Af1 %d", p_zDBEphemeris->z_RawSF1Param.w_Af1);
         STATUSMSG("p_zRawSF1Param w_Iodc %d", p_zDBEphemeris->z_RawSF1Param.w_Iodc);
         STATUSMSG("p_zRawSF1Param w_Toc %d", p_zDBEphemeris->z_RawSF1Param.w_Toc);
         STATUSMSG("w_EphWeek %d", p_zDBEphemeris->w_EphWeek);

     }
     else
     {
         STATUSMSG("Status: Storing Ignored for Epheremis for Sv %d to NVS", u_i);
     }
   }

   STATUSMSG("Status: Storing Total Valid Epheremis=%d to NVS, EPh Counter=%d",u_num_of_eph,u_SvCnt);


}

void gpsc_print_almanac(gpsc_db_type *p_zGPSCDatabase)
{
  gpsc_db_alm_type*        p_zDBAlmanac;

  U8 u_num_of_alm = 0;
  U8 u_i;
  U8 u_SvCnt = p_zGPSCDatabase->u_AlmCounter;

  for ( u_i = 0; u_i < N_SV; u_i++)
  {
     p_zDBAlmanac = &p_zGPSCDatabase->z_DBAlmanac[u_i];
     if ( p_zDBAlmanac->u_Valid)
	  {
	     u_num_of_alm++;

	     ALOGI("Sv:%d, Storing Almanac Data:", u_i);
	     ALOGI("p_zDBAlmanac d_FullToa %d ", p_zDBAlmanac->z_RawAlmanac.d_FullToa);
	     ALOGI("p_zDBAlmanac d_UpdateTime %d ", p_zDBAlmanac->z_RawAlmanac.d_UpdateTime);
	     ALOGI("p_zDBAlmanac q_MZero %d ", p_zDBAlmanac->z_RawAlmanac.q_MZero);
	     ALOGI("p_zDBAlmanac q_Omega %d ", p_zDBAlmanac->z_RawAlmanac.q_Omega);
	     ALOGI("q_OmegaZero %d ", p_zDBAlmanac->z_RawAlmanac.q_OmegaZero);
	     ALOGI("q_SqrtA %d ", p_zDBAlmanac->z_RawAlmanac.q_SqrtA);
	     ALOGI("u_Health %d ", p_zDBAlmanac->z_RawAlmanac.u_Health);
	     ALOGI("u_Prn %d ", p_zDBAlmanac->z_RawAlmanac.u_Prn);
	     ALOGI("u_Toa %d ", p_zDBAlmanac->z_RawAlmanac.u_Toa);
	     ALOGI("w_Af0 %d ", p_zDBAlmanac->z_RawAlmanac.w_Af0);
	     ALOGI("w_Af1 %d ", p_zDBAlmanac->z_RawAlmanac.w_Af1);
	     ALOGI("w_DeltaI %d ", p_zDBAlmanac->z_RawAlmanac.w_DeltaI);
	     ALOGI("w_E %d ", p_zDBAlmanac->z_RawAlmanac.w_E);
	     ALOGI("w_OmegaDot %d ", p_zDBAlmanac->z_RawAlmanac.w_OmegaDot);
	     ALOGI("w_Week %d ", p_zDBAlmanac->z_RawAlmanac.w_Week);
	  }
     else
	  {
        ALOGI("Status: Storing Ignored for Almanac for Sv %d to NVS", u_i);
	  }
  }
  ALOGI("Status: Storing Total Valid Almanac=%d to NVS, Almanac Counter=%d",u_num_of_alm,u_SvCnt);
}

/* Recon Add-on: Store RAW Ephemeris (ICD-200) as downloaded from AI2 chip.
   Recon Ephemeris Server will then adb pull it from device and make available across the Internet 

   Since we don't want to do this in production, call is guarded with pre-processor. Server device
   will simply need in house specific compile

   We serialize in following format:  (this is same as struct gpsrawephemeris in recongps.h)

  Data         Bytes       Description
  =====================================
  svid         1           Satellite PRN Number
  iode         1           IODE Issue of Data, Ephemeris                  
  crs          2           Amplitude of the Sine Harmonic Correction Term to the Orbit Radius    
  delta_n      2           Mean Motion Difference from Computed Value                              
  cuc          2           Amplitude of the Cosine Harmonic Correction Term to the Argument of Latitude
  m0           4           Mean Anomaly at Reference Time      
  e            4           Eccentricity
  sqrta        4           Square Root of the Semi-Major Axis                       
  omega0       4           Longitude of Ascending Node of Orbit Plane at Weekly Epoch
  cus          2           Amplitude of the Sine Harmonic Correction Term to the Argument of Latitudude                     
  toe          2           Reference Time Ephemeris
  cic          2           Amplitude of the Cosine Harmonic Correction Term to the Angle of Inclination      
  cis          2           Amplitude of the Cosine Harmonic Correction Term to the Angle of Inclination
  i0           4           Inclination Angle at Reference Time 
  w (omega)    4           Argument of Perigee
  omegadot     4           Rate of Right Ascension                            
  crc          2           Amplitude of the Cosine Harmonic Correction Term to the Orbit Radius
  idot         2           Rate of Inclination Angle        
  l2code       1           Codes on L2 channel     
  accuracy     1           SV Accuracy                         
  health       1           SV Health                      
  tgd          1           TGD  (Estimate Group Delay Differential)                          
  iodc         2           Issue of Data, Clock (IODC)                         
  toc          2           Sattelite Clock Correction               
  af2          1           SV clock drift rate in sec/sec2                   
  <empty>      1           <alignment - so it is easier to read in>
  af1          2           SV clock drift in sec/sec
  af0          4           SV clock bias in sec            
  predicted    1           Set to TRUE if the ephemeris is predicted, FALSE if it is decoded from sky                
  seedage      1           Used for storing the Ephemeris seed age incase of Predicted Aphemeris
  week         2           GPS Week number (to go with toe) - 65535 if unknown 

*/


#if defined RECON_EPHEMERIS_SERVER
void recon_save_ephemeris (U16 gpsweek, pe_RawEphemeris* p_RawEph, pe_RawSF1Param* p_RawFS1Param)
{
   int fd = 0;
   char path[80];
   char cmd[100];

   unsigned char filler = ' ';

   sprintf(path, "%s/%d", "data/gps/aiding/reconephemeris", p_RawEph->u_Prn);

   // if it exists, it will be replaced
   fd = open(path, O_RDWR | O_CREAT);
   if (fd == -1) {ALOGE("+++ %s: Could not create [%s] Raw Ephemeris. Error %d Quitting +++\n", __FUNCTION__, path, -errno); return;}
  
   // now dump data
#define SEPH(file, value) \
   if (write(file, &value, sizeof(value) ) != sizeof(value) ) goto failed;

   SEPH(fd, p_RawEph->u_Prn)
   SEPH(fd, p_RawEph->u_Iode)
   SEPH(fd, p_RawEph->w_Crs)
   SEPH(fd, p_RawEph->w_DeltaN)
   SEPH(fd, p_RawEph->w_Cuc)
   SEPH(fd, p_RawEph->q_MZero)
   SEPH(fd, p_RawEph->q_E)
   SEPH(fd, p_RawEph->q_SqrtA)
   SEPH(fd, p_RawEph->q_OmegaZero)
   SEPH(fd, p_RawEph->w_Cus)
   SEPH(fd, p_RawEph->w_Toe)
   SEPH(fd, p_RawEph->w_Cic)
   SEPH(fd, p_RawEph->w_Cis)
   SEPH(fd, p_RawEph->q_IZero)
   SEPH(fd, p_RawEph->q_Omega)
   SEPH(fd, p_RawEph->q_OmegaDot)
   SEPH(fd, p_RawEph->w_Crc)
   SEPH(fd, p_RawEph->w_IDot)

   SEPH(fd, p_RawFS1Param->u_CodeL2)
   SEPH(fd, p_RawFS1Param->u_Accuracy)
   SEPH(fd, p_RawFS1Param->u_Health)
   SEPH(fd, p_RawFS1Param->u_Tgd)
   SEPH(fd, p_RawFS1Param->w_Iodc)
   SEPH(fd, p_RawFS1Param->w_Toc)
   SEPH(fd, p_RawFS1Param->u_Af2)
   SEPH(fd, filler)
   SEPH(fd, p_RawFS1Param->w_Af1)
   SEPH(fd, p_RawFS1Param->q_Af0)

   SEPH(fd, p_RawEph->u_predicted)
   SEPH(fd, p_RawEph->u_predSeedAge)
 
   SEPH(fd, gpsweek)

   // close the file, change permission and we are done!
   close(fd);
   sprintf(cmd, "chmod 666 %s", path);
   system(cmd);

   ALOGI("+++ %s: Successfully saved [%s] Raw Ephemeris +++\n", __FUNCTION__, path);
  
   return;

failed:
    ALOGE("+++ %s: Failed to save [%s] Raw Ephemeris (Error %d) +++\n", __FUNCTION__, path, -errno);
    if (fd != -1)  // file was open; make sure we close and delete it
    {
       close(fd);
       remove(path);
    }
}
#endif
