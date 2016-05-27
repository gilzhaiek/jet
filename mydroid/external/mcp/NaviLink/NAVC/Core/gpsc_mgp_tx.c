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
 * FileName			:	gpsc_mgp_tx.c
 *
 * Description     	:
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */
#include "pla_defs.h"
#include "gpsc_types.h"
#include "gpsc_sap.h"
#include "gpsc_mgp_tx.h"
#include "gpsc_mgp_cfg.h"
#include "gpsc_msg.h"
#include "gpsc_comm.h"
#include "gpsc_state.h"
#include "gpsc_database.h"
#include "gpsc_app_api.h"
#include "mcpf_services.h"
#include "mcp_hal_fs.h"
#include "mcp_unicode.h"
#include "gpsc_sequence.h"
#include "gpsc_timer_api.h"
#include "navc_defs.h"
#include <stdio.h>
#include "mcpf_time.h"
#include "navc_inavc_if.h"

#include <utils/Log.h>
#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG "NAVD.GPSC_MGP_TX"    // our identification for logcat (adb logcat NAVD.GPSC_MGP_TX:V *:S)

#ifdef ENABLE_INAV_ASSIST
#include "inavc_ext.h"
#endif

#include "math.h"

#ifdef UNIFIED_TOOLKIT_COMM
#include "clientsocket_comm.h"
extern McpS32 clientsockfd;
#endif

#define C_PACKET_SIZE_INJ_SEDATA 40
#define C_PACKET_SIZE_INJ_APMCONFIG 8

#define C_AI2_CUSTOMER_CONFIG_EXTENDED_BODY 81
#define C_AI2_HOST_WAKEUP_BODY 				20
#define C_AI2_RTC_CTRL_BODY					5
#define C_AI2_RF_READ_REG_BODY				8



#define C_AI2_CUSTOMER_CONFIG_BODY 24
#define C_AI2_SBAS_CTRL_BODY 				16
#define C_AI2_USER_MOTION_MASK_BODY 		42
#define C_AI2_ADV_POWER_MGMT_BODY			8
#define C_AI2_TIMEOUT_BODY					22
#define C_AI2_BAUD_RATE_BODY				2
#define C_BAUD_19200 5
/* Define noise figure and implementation loss in .1 dB-Hz units
   for C/No referencing. */
#define C_CNO_NF 25
#define C_PACKET_SIZE_INJ_TIME  17
#define C_PACKET_SIZE_INJ_POS   22
#define C_PATCH_HEADER_SIZE 9
#define C_GPSC_USE_GPS_ADDR 1
#define C_GPSC_MAX_MSG_LEN  64
#define C_AI2_PROT_SEL 1
#define GPSC_AI2 1
#define GPSC_SHUTDOWN_CTRL  0
#define GPSC_REF_CLK_STABLE  1
#define C_AI2_REF_CLK_RESP 3
#define C_AI2_ALTITUDE_BODY 5
#define C_AI2_BLF_CONFIG 7

U32 q_DownloadCurrTick=0,q_DownloadEndTick=0;

typedef struct
{

  U32 q_FCount;           /* FCOUNT when steering is valid */
  U16 w_AssistFlag;       /* Bit field. ‘1’ indicates status is true
							  Bit Status
							0 Position
							1 Velocity
							2  clock bias
							3  clock velocity
							4  Controlled N/W
							5-15 Reserved */
  U8 u_PositionFlag;           /* Bit Field '1' indicates status is true
									0: 2D
									1: 3D */
  S32 l_Latitude ;        /* Signed, Pi/2^32radians per bit  , Range: Pi/2 to Pi/2 - Pi/2^32 radians */
  S32 l_Longitude;      /* Signed, Pi/2^31radians per bit  , Range: Pi/2 to Pi/2 - Pi/2^31 radians */
  S16 x_Altitude;       /* Signed, 0.5 meters per bit Range: -16384 to 16383.5 meters*/
  U16 w_EastUnc;          /* Unsigned, 0.1 meters per bit  Range: 0 to 6553.5 meters (1 ?)*/
  U16 w_NorthUnc;          /* Unsigned, 0.1 meters per bit  Range: 0 to 6553.5 meters (1 ?)*/
  U16 w_VerticalUnc;          /* Unsigned, 0.5 meters per bit Range: 0 to 32767.5 meters (1 ?)*/
  S32 l_ClockBias;          /* Signed, 0.1 meters per bit. Range from -838860.8 to 838860.7 */
  U16 l_ClockBiasUnc;       /* Unsigned, 0.1 metres per bit. Range from 0 to 6553.5 */
  S16 x_EastVelocity;       /* Signed, 0.01 metres/sec per bit. Range from -327.68 to 327.67 m/sec */
  S16 x_NorthVelocity;     /* Signed, 0.01 metres/sec per bit. Range from -327.68 to 327.67 m/sec */
  S16 x_UpVelocity;        /* Signed, 0.01 metres/sec per bit. Range from -327.68 to 327.67 m/sec */
  S16 x_ClockVelocity;    /* Signed, 0.01 metres/sec per bit. Range from -327.68 to 327.67 m/sec */
  U16 w_EastVelUnc;        /* Unsigned, 0.01 meters per bit, Range: 0 to 655.35 meters (1 ?) */
  U16 w_NorthVelUnc;        /* Unsigned, 0.01 meters per bit, Range: 0 to 655.35 meters (1 ?) */
  U16 w_UpVelUnc;        /* Unsigned, 0.01 meters per bit, Range: 0 to 655.35 meters (1 ?) */
  U16 w_ClockVelUnc;        /* Unsigned, 0.01 meters per bit, Range: 0 to 655.35 meters (1 ?) */

}InjWlanAssist; /* structure for inject SV steering */





typedef struct
{
  U8  u_SvId;             /* 1..32 */
  U32 q_FCount;           /* FCOUNT when steering is valid */
  U32 q_SvSubMsec_R24;    /* Sv time sub msec: resolution = 1/2**24 msec */
  U32 q_SvMsec;           /* SV time msec: resolution = 1 msec */
  U16 w_SvTimeUnc;        /* 11-5 mantissa-exponent, unit 1 ns */
  S32 l_SvSpeed_R24;      /* Sv speed: resolution = 0.01 m/s.
                             Bit 23 is sign bit */
  U16 w_SvSpeedUnc;       /* SV speed unc: resolution = 0.1 m/s */
  S16 x_SvAccel;          /* Sv acceleration: resolution = 2/128 cm/sec2 */
  U8  u_SteeringFlag;     /* 0: run selection; 1: force unconditinally */
}InjSteer; /* structure for inject SV steering */

typedef struct
{
  U8 u_SvId;              /* 1..32 */

  U8 u_TimeStat;          /* 0: accept time diff only;
                             1: accept Mod20 diff.
                             2: accept abs ms diff. */

  S32 q_DiffTime_R24;     /* signed, resolution 40/(2^23) msec */

  U16 w_NonDiffTimeUnc;   /* unsigned, resolution 1/(2^16) msec */

  S16 x_DiffSpeed;        /* signed, resolution 2000/(2^15) m/s */

  U16 w_NonDiffSpeedUnc;  /* unsigned, resolution 0.1 m/s */

}InjSvDiff;

/*===========================================================================

FUNCTION  GPSC_MGP_TX_RC_ACT

DESCRIPTION
  This function forms and sends a ReceiverAction packect to the Sensor,
  and properly mark the relevant database elements "invalid" if the
  action taken with this command involve deletion of data that is
  maintained in LM database.

PARAMETERS:

  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the
  pointers of many structures accessed from different modules and
  functions.

  u_ActionType - Type of receiver action to be taken
  u_ActionModifier - required for some actions, like delete-eph and
                     delete-alm, 0 means for all Svs

===========================================================================*/

void gpsc_mgp_tx_rc_act (gpsc_ctrl_type*  p_zGPSCControl,
                        U8 u_ActionType,
                        U8 u_ActionModifier)
{

  gpsc_db_type*          p_zGPSCDatabase  = p_zGPSCControl->p_zGPSCDatabase;
  gpsc_db_gps_clk_type*  p_DBGpsClock    = &p_zGPSCDatabase->z_DBGpsClock;
  gpsc_db_pos_type*      p_DBPos         = &p_zGPSCDatabase->z_DBPos;
  gpsc_db_iono_type*     p_DBIono        = &p_zGPSCDatabase->z_DBIono;
  gpsc_db_health_type*   p_DBHealth      = &p_zGPSCDatabase->z_DBHealth;

  gpsc_ai2_put_msg_desc_type    z_PutMsgDesc;
  U8          u_ActionTypeArray[2];
  U8          u_Cnt;

  /* sanity check */
  if ( u_ActionType > 10 || u_ActionModifier > N_SV )
    return; /* undefined action type or invalid modifier, don't act */

  u_ActionTypeArray[0] = u_ActionType;
  u_ActionTypeArray[1] = u_ActionModifier;

  z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ;
  z_PutMsgDesc.u_PacketId = AI_REC_ACTION;
  z_PutMsgDesc.p_Buf = &u_ActionTypeArray[0];

  /* In most cases, packet length of an AI2 AI_REC_ACTION packet is 1.
     But if ActionType involves deleting ephemeris and/or deleting
     almanac, packet length becomes 2 as an optional ActionModifier
     which specifies SV IDs is added. Note when that happens,
     ActionModifier = 0 means action is for all SVs */
  z_PutMsgDesc.w_PacketLen = 1;
  if ( ( u_ActionType == C_DELETE_EPHEM ) ||
       ( u_ActionType == C_DELETE_ALM   ) )
  {
    z_PutMsgDesc.w_PacketLen = 2;
  }

  z_PutMsgDesc.u_ForceFlag = 0;
  z_PutMsgDesc.u_MsgSend = FALSE;

  AI2TX("Receiver Action",AI_REC_ACTION);
  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );

  switch (u_ActionType)
  {

  case C_RECEIVER_ON:
    p_zGPSCControl->p_zGPSCState->u_SensorOpMode = C_RC_ON;
    p_zGPSCControl->p_zGPSCState->u_SensorOpModeConfirmed = FALSE;
    break;

  case C_RECEIVER_OFF:
    p_zGPSCControl->p_zGPSCState->u_SensorOpMode = C_RC_OFF;
    p_zGPSCControl->p_zGPSCState->u_SensorOpModeConfirmed = FALSE;
    p_zGPSCControl->p_zGPSCState->injectedCepinfoCount = 0;
    break;

  case C_RECEIVER_IDLE:
    p_zGPSCControl->p_zGPSCState->u_SensorOpMode = C_RC_IDLE;
    p_zGPSCControl->p_zGPSCState->u_SensorOpModeConfirmed = FALSE;
    p_zGPSCControl->p_zGPSCState->injectedCepinfoCount = 0;
    break;
  }

  /* Once something is deleted, make sure mark relevant LM database
     elements "invalid" */
  if ( u_ActionType == C_DELETE_TIME )
  {
    p_DBGpsClock->u_Valid = FALSE;
    /*  The sensor time uncertainty will be set to 1 year (in ms). */
    p_DBGpsClock->f_TimeUnc = (FLT)31536* 1000000;
  }

  else if ( u_ActionType == C_DELETE_POS )
  {
    p_DBPos->d_ToaSec = 0;
  }

  else if ( u_ActionType == C_DELETE_SV_HEALTH )
  {
    p_DBHealth->d_UpdateTimeSec = 0;
  }

  else if ( u_ActionType == C_DELETE_EPHEM )
  {
    if (u_ActionModifier == 0 )
    {
      for ( u_Cnt = 0; u_Cnt < N_SV; u_Cnt++)
      {
        p_zGPSCDatabase->z_DBEphemeris[ u_Cnt ].u_Valid = FALSE;
		p_zGPSCDatabase->z_DBEphemeris[ u_Cnt ].z_RawEphemeris.u_Prn = 0;
        p_zGPSCDatabase->z_DBEphStat[ u_Cnt ].u_SvId = 0;
        /* use SvId = 0 to indicate status unknown */
      }
    }
    else
    {
      p_zGPSCDatabase->z_DBEphemeris[ u_ActionModifier - 1 ].u_Valid = FALSE;
	  p_zGPSCDatabase->z_DBEphemeris[ u_ActionModifier - 1 ].z_RawEphemeris.u_Prn = 0;
      p_zGPSCDatabase->z_DBEphStat[ u_ActionModifier - 1 ].u_SvId = 0;
    }
  }

  else if ( u_ActionType == C_DELETE_ALM )
  {
    if ( u_ActionModifier == 0)
    {
      for ( u_Cnt = 0; u_Cnt < N_SV; u_Cnt++)
      {
        p_zGPSCDatabase->z_DBAlmanac[ u_Cnt ].u_Valid = FALSE;
		p_zGPSCDatabase->z_DBAlmanac[ u_Cnt ].z_RawAlmanac.u_Prn = 0;
        p_zGPSCDatabase->z_DBAlmStat[ u_Cnt ].u_SvId = 0;
        /* use SvId = 0 to indicate status unknown */
      }
    }
    else
    {
      p_zGPSCDatabase->z_DBAlmanac[ u_ActionModifier - 1 ].u_Valid = FALSE;
	  p_zGPSCDatabase->z_DBAlmanac[ u_ActionModifier - 1 ].z_RawAlmanac.u_Prn = 0;
      p_zGPSCDatabase->z_DBAlmStat[ u_ActionModifier - 1 ].u_SvId = 0;
      /* use SvId = 0 to indicate status unknown */
    }
  }

  else if ( u_ActionType == C_DELETE_IONO_UTC )
  {
    p_DBIono->d_UpdateTimeSec = 0;
  }
}

//sanjay added

void gpsc_mgp_inject_nvs_suplhot(gpsc_ctrl_type* p_zGPSCControl)
{
        gpsc_smlc_assist_type*  p_zSmlcAssist  = p_zGPSCControl->p_zSmlcAssist;
        //gpsc_db_sv_dir_type *p_zDBSvDirection = &p_zGPSCDatabase->z_DBSvDirection; /* Klocwork Changes */
		gpsc_acq_assist_type   *p_zAcquisAssist = &p_zSmlcAssist->z_AcquisAssist;
        //U8 u_SvCnt = 0; /* Klocwork Changes */
		U8	i;
        //U32 q_CurGPSTime;  /* Klocwork Changes */
		//me_SvSteering   z_SvSteer, *p_zSvSteer = &z_SvSteer;  /* Klocwork Changes */

		if(p_zAcquisAssist->u_NumSv == 0)
		  return;

		for(i=0;i<17;i++)
		{
			if (p_zGPSCControl->p_zGPSCDatabase->z_DBGpsClock.f_TimeUnc > (160.0 * 1000.0) )
			{
		  		STATUSMSG("Status: Local time too coarse: wk=%u unc=%f, not to use Acq Assist",
				 p_zGPSCControl->p_zGPSCDatabase->z_DBGpsClock.w_GpsWeek,p_zGPSCControl->p_zGPSCDatabase->z_DBGpsClock.f_TimeUnc);
		  		return;
			}
			else  /* local time reasonably good, apply acq. assistance data */
          		gpsc_mgp_assist_steering_sort(p_zGPSCControl);/* ( p_zDBGpsClock->f_TimeUnc <= (160.0 * 1000.0) ) */

      		if(gpsc_mgp_assist_steering_apply(p_zGPSCControl) != TRUE)
	  		{
 		  		STATUSMSG("Status:No Injection steering clear the availlist");
				break;
	  		}
	  		else
	  		{
		  		STATUSMSG("Status: Injected all steering clear the availlist");
 	  		}
		}

		p_zSmlcAssist->u_SvCounter = 0;
		gpsc_mgp_assist_svdiff_apply(p_zGPSCControl);
        gpsc_mgp_assist_svdir_apply(p_zGPSCControl);
        //gpsc_mgp_assist_svdir_save(p_zGPSCControl);
		p_zAcquisAssist->u_NumSv=0;
        //gpsc_mgp_assist_pmatch_apply(p_zGPSCControl);
		gpsc_mgp_assist_rti_apply(p_zGPSCControl);
}

/*
 ******************************************************************************
 * gpsc_mgp_dr_req
 *
 * Function description:
 *
 *  Form and send Dead Reckoning Request to the Receiver
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *
 * Return value:
 *  None
 *
 ******************************************************************************
 */

void gpsc_mgp_dr_req(gpsc_ctrl_type*  p_zGPSCControl, U8 *p_Data, U32 len)
{
	  gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;

	  z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
	  z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_CUSTOM_PACKET;

	  z_PutMsgDesc.p_Buf = p_Data;

	  z_PutMsgDesc.w_PacketLen = len;

	  z_PutMsgDesc.u_ForceFlag = 1;
	  z_PutMsgDesc.u_MsgSend = TRUE;

	  AI2TX("Dead Reckoning Request",AI_CLAPP_INJ_CUSTOM_PACKET);
	  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}



/*
 ******************************************************************************
 * gpsc_mgp_ver_req
 *
 * Function description:
 *
 *  Form and send Version Request to the Receiver
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *
 * Return value:
 *  None
 *
 ******************************************************************************
 */

void gpsc_mgp_ver_req(  gpsc_ctrl_type*  p_zGPSCControl)
{
	  gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;

	  z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ;
	  z_PutMsgDesc.u_PacketId = AI_CLAPP_REQ_VERSION;
	  z_PutMsgDesc.p_Buf = NULL;

	  z_PutMsgDesc.w_PacketLen = 0;

	  z_PutMsgDesc.u_ForceFlag = 1;
	  z_PutMsgDesc.u_MsgSend = TRUE;

	  AI2TX("Version Request",AI_CLAPP_REQ_VERSION);
	  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}


/*
 ******************************************************************************
 * gpsc_mgp_inject_gps_time
 *
 * Function description:
 *
 *  Form and send InjectGpsTime command to Sensor and set GPS time database to
 *  invalid.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *  p_zInjectTimeEst: pointer to the zInjectTimeEst structure
 *
 * Return value:
 *  None
 *
 ******************************************************************************
 */

void gpsc_mgp_inject_gps_time
(
  gpsc_ctrl_type*     p_zGPSCControl,
  gpsc_inject_time_est_type*  p_zInjectTimeEst
)
{

  gpsc_db_type*  p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
  gpsc_ai2_put_msg_desc_type    z_PutMsgDesc;
  U8            u_Data[ C_PACKET_SIZE_INJ_TIME ], *p_B = u_Data;
  S32           l_LongWord;
  U16           w_Word;

	/* Make sure we do not inject invalid time, i.e. reject everything
	* less than 2013
	*/
 	if (p_zInjectTimeEst->z_meTime.w_GpsWeek < RECON_MIN_GPS_WEEK)
 	{
 		ALOGI("+++ %s: Time is not valid, rejecting time +++\n", __FUNCTION__);
		return;
	}

	/* Logic for week injection if Sensor has better TUnc */
	if ((p_zGPSCDatabase->z_DBGpsClock.w_GpsWeek == 65535) &&
		(p_zGPSCDatabase->z_DBGpsClock.f_TimeUnc < 20.0)) /* Signifies ms is not yet decoded */
	{

		*p_B++ = 0; /* Arbitrary Reference */
		p_B = NBytesPut((U32)p_zGPSCDatabase->z_DBGpsClock.q_FCount,p_B,sizeof(U32));
 		p_B = NBytesPut(p_zInjectTimeEst->z_meTime.w_GpsWeek, p_B, sizeof(U16));
  		p_B = NBytesPut(p_zGPSCDatabase->z_DBGpsClock.q_GpsMs,p_B,sizeof(U32));
		/* Byte 11,12,13: TimeBias: Limit to -+0.5ms, and quantitize to 24 bits
     			signed */
	  	if ( p_zGPSCDatabase->z_DBGpsClock.f_TimeBias > 0.5 )
	    	p_zGPSCDatabase->z_DBGpsClock.f_TimeBias = 0.5;
	  	else if ( p_zGPSCDatabase->z_DBGpsClock.f_TimeBias < -0.5 )
	   	 	p_zGPSCDatabase->z_DBGpsClock.f_TimeBias = -0.5;
	  	l_LongWord = (S32) ( p_zGPSCDatabase->z_DBGpsClock.f_TimeBias / (FLT)C_LSB_TIME_BIAS );

		p_B = NBytesPut(l_LongWord,p_B,3);

		 /*  encode time unc. mantissa and exponent, note w_Word is unc. in
      nanosecond */
 		w_Word = EncodeM11E5U16( p_zGPSCDatabase->z_DBGpsClock.f_TimeUnc *
                           ( FLT ) 1000.0 * ( FLT ) 1000.0 );
  		p_B = NBytesPut(w_Word, p_B, sizeof(U16));

		*p_B++ = 1; // force time injection in Arbitrary mode

	}
	else
	{
  *p_B++ = p_zInjectTimeEst->u_Mode;

  p_B = NBytesPut((U32)p_zInjectTimeEst->q_Fcount,p_B,sizeof(U32));
  p_B = NBytesPut(p_zInjectTimeEst->z_meTime.w_GpsWeek, p_B, sizeof(U16));
  p_B = NBytesPut(p_zInjectTimeEst->z_meTime.q_GpsMsec,p_B,sizeof(U32));

  /* Byte 11,12,13: TimeBias: Limit to -+0.5ms, and quantitize to 24 bits
     signed */
  if ( p_zInjectTimeEst->z_meTime.f_ClkTimeBias > 0.5 )
    p_zInjectTimeEst->z_meTime.f_ClkTimeBias = 0.5;
  else if ( p_zInjectTimeEst->z_meTime.f_ClkTimeBias < -0.5 )
    p_zInjectTimeEst->z_meTime.f_ClkTimeBias = -0.5;
	  	l_LongWord = (S32) ( p_zInjectTimeEst->z_meTime.f_ClkTimeBias / (FLT)C_LSB_TIME_BIAS );

  p_B = NBytesPut(l_LongWord,p_B,3);

  /*  encode time unc. mantissa and exponent, note w_Word is unc. in
      nanosecond */
  w_Word = EncodeM11E5U16( p_zInjectTimeEst->z_meTime.f_ClkTimeUncMs *
                           ( FLT ) 1000.0 * ( FLT ) 1000.0 );
  p_B = NBytesPut(w_Word, p_B, sizeof(U16));

  *p_B++ = p_zInjectTimeEst->u_ForceFlag;

	}

  z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ;
  z_PutMsgDesc.u_PacketId = AI_INJ_TIME_EST;
  z_PutMsgDesc.p_Buf = u_Data;
  z_PutMsgDesc.w_PacketLen = C_PACKET_SIZE_INJ_TIME;
  z_PutMsgDesc.u_ForceFlag = 0;
  z_PutMsgDesc.u_MsgSend = FALSE /*Add request back in same message*/;


  ALOGI("+++ %s: Injecting Time. GPS Week: [%d], Milisecond: [%d] +++\n", 
      __FUNCTION__, p_zInjectTimeEst->z_meTime.w_GpsWeek, p_zInjectTimeEst->z_meTime.q_GpsMsec);

  AI2TX("Inject Time", AI_INJ_TIME_EST);
  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );

  /* once inject command is sent, mark LM database for this item "not-valid"
     until Sensor later reports this item and then update the database */
  /*p_zGPSCDatabase->z_DBGpsClock.u_Valid = FALSE;*/

  /*** try this ***/
  if (  p_zInjectTimeEst->u_ForceFlag ||
       ( ( p_zGPSCDatabase->z_DBGpsClock.f_TimeUnc >
         p_zInjectTimeEst->z_meTime.f_ClkTimeUncMs
         ) && !p_zInjectTimeEst->u_ForceFlag
       )
   )
  {
	/* Leave the database FCount alone. The only known beneficiary of setting
	   the database to the injected values is the acquisition assistance
	   process.  It needs an FCount that is consistent with the GpsMsec.
	   The FCount should be roughly in the neighborhood of the sensor's
	   current FCount.  Within 1 sec is adequate */
	  /* p_zGPSCDatabase->z_DBGpsClock.q_FCount = */

    p_zGPSCDatabase->z_DBGpsClock.w_GpsWeek =
      p_zInjectTimeEst->z_meTime.w_GpsWeek;

    p_zGPSCDatabase->z_DBGpsClock.q_GpsMs =
      p_zInjectTimeEst->z_meTime.q_GpsMsec;

    p_zGPSCDatabase->z_DBGpsClock.f_TimeBias =
      p_zInjectTimeEst->z_meTime.f_ClkTimeBias;

    p_zGPSCDatabase->z_DBGpsClock.f_TimeUnc =
      p_zInjectTimeEst->z_meTime.f_ClkTimeUncMs;

    p_zGPSCDatabase->z_DBGpsClock.u_Valid = TRUE;

  }

  AI2TX("Request Clock", AI_REQ_CLOCK_REP);
  gpsc_mgp_tx_req(p_zGPSCControl, AI_REQ_CLOCK_REP, 0);
}

/*
 ******************************************************************************
 *
 * gpsc_mgp_inject_freq_est
 *
 * Function description: Inject frequency estimate to the sensor
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

void gpsc_mgp_inject_freq_est(
  gpsc_ctrl_type*             p_zGPSCControl,
  gpsc_inject_freq_est_type*  p_zInjectFreqEst
)
{
  U8            u_Data[9], *p_B = u_Data;

  gpsc_ai2_put_msg_desc_type z_PutMsgDesc;

  p_B = NBytesPut (p_zInjectFreqEst->l_FreqBiasRaw,p_B,sizeof(S32));
  p_B = NBytesPut (p_zInjectFreqEst->q_FreqUncRaw,p_B,sizeof(U32));
  *p_B = p_zInjectFreqEst->applyFlag;

  z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
  z_PutMsgDesc.u_PacketId = AI_INJ_FREQ_EST;
  z_PutMsgDesc.p_Buf = u_Data;
  z_PutMsgDesc.w_PacketLen = 9;
  z_PutMsgDesc.u_ForceFlag = 0;
  z_PutMsgDesc.u_MsgSend = FALSE;

  AI2TX("Inject Frequency",AI_INJ_FREQ_EST);
  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}

/*
 ******************************************************************************
 * gpsc_mgp_inject_pos_est
 *
 * Function description:
 *
 * Form and send gpsc_mgp_inject_pos_est command to Sensor, and set the
 * initial position database to invalid.
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

void gpsc_mgp_inject_pos_est
(
   gpsc_ctrl_type*            p_zGPSCControl,
   gpsc_inject_pos_est_type*  p_InjectPosEst,
   U8                         u_reRequest
)
{
   gpsc_db_type*  p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
   U8  u_Data[C_PACKET_SIZE_INJ_POS], *p_B = u_Data;
   gpsc_ai2_put_msg_desc_type z_PutMsgDesc;

   p_B = NBytesPut (p_InjectPosEst->l_Lat,p_B, sizeof(S32));
   p_B = NBytesPut (p_InjectPosEst->l_Long,p_B, sizeof(S32));
   p_B = NBytesPut (p_InjectPosEst->x_Ht,p_B, sizeof(S16));
   p_B = NBytesPut (p_InjectPosEst->q_Unc,p_B, sizeof(U32));
   p_B = NBytesPut (p_InjectPosEst->w_GpsWeek, p_B, sizeof(U16));
   p_B = NBytesPut (p_InjectPosEst->q_GpsMsec, p_B, sizeof(U32));

   *p_B++ = p_InjectPosEst->u_Mode;
   *p_B = p_InjectPosEst->u_ForceFlag;

   z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ;
   z_PutMsgDesc.u_PacketId = AI_INJ_POS_EST;
   z_PutMsgDesc.p_Buf = u_Data;
   z_PutMsgDesc.w_PacketLen = C_PACKET_SIZE_INJ_POS;
   z_PutMsgDesc.u_ForceFlag = 0;
   z_PutMsgDesc.u_MsgSend = FALSE;

   ALOGI("+++ %s: Injecting Position. LAT [%d], LONG [%d], GPS Week: [%d], GPS Msec: [%d] +++\n",
      __FUNCTION__, p_InjectPosEst->l_Lat, p_InjectPosEst->l_Long, p_InjectPosEst->w_GpsWeek, p_InjectPosEst->q_GpsMsec);

   AI2TX("Inject Position",AI_INJ_POS_EST);
   gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );

   /* once inject command is sent, mark database for this item "not-valid"
     until Sensor later reports this item and then update the database */

   p_zGPSCDatabase->z_DBPos.d_ToaSec = 0.0f;

   if (u_reRequest == TRUE)
   {
		AI2TX("Request Position", AI_CLAPP_REQ_POS_EXT);
      gpsc_mgp_tx_req( p_zGPSCControl, AI_CLAPP_REQ_POS_EXT, 0 );
   }

}


/*
 ******************************************************************************
 * gpsc_mgp_inject_pos_est_wakeup
 *
 * Function description:
 *
 * Form and send gpsc_mgp_inject_pos_est command to Sensor, and set the
 * initial position database to invalid.
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

void gpsc_mgp_inject_pos_est_wakeup
(
  gpsc_ctrl_type*    p_zGPSCControl,
  gpsc_inject_pos_est_type*  p_InjectPosEst,
   U8 u_reRequest
)
{
  U8  u_Data[C_PACKET_SIZE_INJ_POS], *p_B = u_Data;
  gpsc_ai2_put_msg_desc_type z_PutMsgDesc;

  p_B = NBytesPut (p_InjectPosEst->l_Lat,p_B,sizeof(S32));
  p_B = NBytesPut (p_InjectPosEst->l_Long,p_B,sizeof(S32));
  p_B = NBytesPut(p_InjectPosEst->x_Ht,p_B,sizeof(S16));
  p_B = NBytesPut(p_InjectPosEst->q_Unc,p_B,sizeof(U32));
  p_B = NBytesPut(p_InjectPosEst->w_GpsWeek, p_B, sizeof(U16));
  p_B = NBytesPut(p_InjectPosEst->q_GpsMsec, p_B, sizeof(U32));
  *p_B++ = p_InjectPosEst->u_Mode;
  *p_B = p_InjectPosEst->u_ForceFlag;

  z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ;
  z_PutMsgDesc.u_PacketId = AI_INJ_POS_EST;
  z_PutMsgDesc.p_Buf = u_Data;
  z_PutMsgDesc.w_PacketLen = C_PACKET_SIZE_INJ_POS;
  z_PutMsgDesc.u_ForceFlag = 0;
  z_PutMsgDesc.u_MsgSend = FALSE;

  AI2TX("Inject Position Wakeup",AI_INJ_POS_EST);
  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );

	if (u_reRequest == TRUE)
	{
		AI2TX("Request Position",AI_CLAPP_REQ_POS_EXT);
		gpsc_mgp_tx_req( p_zGPSCControl, AI_CLAPP_REQ_POS_EXT, 0 );
	}

}

/*
 ******************************************************************************
 * InjectAlamanc
 *
 * Function description:
 *
 *  Form and send InjectAlamanc command to Sensor and set almanac database to
 *  invalid.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *  p_RawAlamanc: pointer to a raw almanac structure
 *
 *
 * Return value:
 *  None
 *
 ******************************************************************************
 */

U8 gpsc_mgp_inject_alm
(
  gpsc_ctrl_type*     p_zGPSCControl,
  pe_RawAlmanac*  p_RawAlmanac
)
{
  gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;
  U8               u_Data[27], *p_B = u_Data;

  /* make sure if NVS claims to have valid record but somehow corrupted
     almanac in such a way that u_Prn would be cause access violation,
     exit the function without doing anything. */
  if ( ( p_RawAlmanac->u_Prn > N_SV ) || ( p_RawAlmanac->u_Prn == 0 ) )
  {
    return FALSE;
  }


  *p_B++ = p_RawAlmanac->u_Prn;
  *p_B++ = p_RawAlmanac->u_Health;
  *p_B++ = p_RawAlmanac->u_Toa;
  p_B = NBytesPut(p_RawAlmanac->w_E,p_B,sizeof(U16));
  p_B = NBytesPut(p_RawAlmanac->w_DeltaI,p_B,sizeof(U16));
  p_B = NBytesPut(p_RawAlmanac->w_OmegaDot,p_B,sizeof(U16));
  p_B = NBytesPut(p_RawAlmanac->q_SqrtA,p_B,3);
  p_B = NBytesPut(p_RawAlmanac->q_OmegaZero,p_B,3);
  p_B = NBytesPut(p_RawAlmanac->q_Omega,p_B,3);
  p_B = NBytesPut(p_RawAlmanac->q_MZero,p_B,3);
  p_B = NBytesPut(p_RawAlmanac->w_Af0,p_B,sizeof(U16));
  p_B = NBytesPut(p_RawAlmanac->w_Af1, p_B,sizeof(U16));
  p_B = NBytesPut(p_RawAlmanac->w_Week, p_B,sizeof(U16));

  z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ;
  z_PutMsgDesc.u_PacketId = AI_INJ_ALM;
  z_PutMsgDesc.p_Buf = u_Data;
  z_PutMsgDesc.w_PacketLen = 27;
  z_PutMsgDesc.u_ForceFlag = 0;
  z_PutMsgDesc.u_MsgSend = FALSE;

  // AI2TX are diagnostic macros that eventually boil down to MCPF_REPORT_*** crap, which is on Android ALOG - if turned on 
  ALOGV("+++ %s: Injecting Almanac PRN=%d +++\n", __FUNCTION__, p_RawAlmanac->u_Prn);

  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );

  return TRUE;
}



/*
 ******************************************************************************
 * gpsc_mgp_inject_eph
 *
 * Function description:
 *
 *  Form and send InjectAlamanc command to Sensor and set almanac database to
 *  invalid.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *    of many structures accessed from different modules and functions.
 *
 *  p_zInjectEph: pointer to the structure that contains pointers to SF1 and
 *    Eph structures and the ephemeris week number.
 *
 *  u_zAckReq: variable to selecte if ACK is required with this AI2, 1:ACK, 0:NO_ACK
 *
 * Return value:
 *  None
 *
 ******************************************************************************
 */

U8 gpsc_mgp_inject_eph
(
  gpsc_ctrl_type*  p_zGPSCControl,
  gpsc_inject_eph_type*   p_zInjectEph,
  McpU8 u_zAckReq
)
{

  gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;
  pe_RawEphemeris*           p_RawEphemeris   = p_zInjectEph->p_zRawEphemeris;
  pe_RawSF1Param*            p_RawSF1Param    = p_zInjectEph->p_zRawSF1Param;
  U16                        w_EphWeek        = p_zInjectEph->w_EphWeek;
  U8                         u_Data[63], *p_B = u_Data;

  /* make sure if NVS claims to have valid record but somehow corrupted
     ephemeris in such a way that u_Prn would be cause access violation,
     exit the function without doing anything. */
  if ( (p_RawEphemeris->u_Prn > N_SV) || (p_RawEphemeris->u_Prn == 0 ) )
  {
    return FALSE;
  }


  *p_B++ = p_RawEphemeris->u_Prn;
  *p_B++ = p_RawSF1Param->u_CodeL2;
  *p_B++ = p_RawSF1Param->u_Accuracy;
  *p_B++ = p_RawSF1Param->u_Health;
  *p_B++ = p_RawSF1Param->u_Tgd;
  p_B    = NBytesPut( p_RawSF1Param->w_Iodc, p_B, sizeof(U16) );
  p_B    = NBytesPut( p_RawSF1Param->w_Toc, p_B, sizeof(U16) );
  *p_B++ = p_RawSF1Param->u_Af2;
  p_B    = NBytesPut( p_RawSF1Param->w_Af1, p_B, sizeof(U16) );
  p_B    = NBytesPut( p_RawSF1Param->q_Af0, p_B, 3 ); /* 24 Bits */
  *p_B++ = p_RawEphemeris->u_Iode;
  p_B    = NBytesPut( p_RawEphemeris->w_Crs, p_B, sizeof(U16) );
  p_B    = NBytesPut( p_RawEphemeris->w_DeltaN, p_B, sizeof(U16) );
  p_B    = NBytesPut( p_RawEphemeris->q_MZero, p_B, sizeof(U32) );
  p_B    = NBytesPut( p_RawEphemeris->w_Cuc, p_B, sizeof(U16) );
  p_B    = NBytesPut( p_RawEphemeris->q_E, p_B, sizeof(U32) );
  p_B    = NBytesPut( p_RawEphemeris->w_Cus, p_B, sizeof(U16) );
  p_B    = NBytesPut( p_RawEphemeris->q_SqrtA, p_B, sizeof(U32) );
  p_B    = NBytesPut( p_RawEphemeris->w_Toe, p_B, sizeof(U16) );
  p_B    = NBytesPut( p_RawEphemeris->w_Cic, p_B, sizeof(U16) );
  p_B    = NBytesPut( p_RawEphemeris->q_OmegaZero, p_B, sizeof(U32) );
  p_B    = NBytesPut( p_RawEphemeris->w_Cis, p_B, sizeof(U16) );
  p_B    = NBytesPut( p_RawEphemeris->q_IZero, p_B, sizeof(U32) );
  p_B    = NBytesPut( p_RawEphemeris->w_Crc, p_B, sizeof(U16) );
  p_B    = NBytesPut( p_RawEphemeris->q_Omega, p_B, sizeof(U32) );
  p_B    = NBytesPut( p_RawEphemeris->q_OmegaDot, p_B, 3 ); /* 24 Bits */
  p_B    = NBytesPut( p_RawEphemeris->w_IDot, p_B, sizeof(U16) );

  p_B    = NBytesPut( w_EphWeek, p_B, sizeof(U16) );

  if (u_zAckReq == 1)
  z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ;
  else
  z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;

  z_PutMsgDesc.u_PacketId = AI_INJ_EPHEM;
  z_PutMsgDesc.p_Buf = u_Data;
  z_PutMsgDesc.w_PacketLen = 63;
  z_PutMsgDesc.u_ForceFlag = 0;
  z_PutMsgDesc.u_MsgSend = FALSE;

  AI2TX("Inject Ephemeris",AI_INJ_EPHEM);
  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
  return TRUE;
}


/*
 ******************************************************************************
 * gpsc_mgp_inject_iono
 *
 * Function description:
 *
 *  Form and send InjectIono command to Sensor and set almanac database to
 *  invalid.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *  p_Iono: pointer to an Iono structure.
 *
 * Return value:
 *  None
 *
 ******************************************************************************
 */

void gpsc_mgp_inject_iono
(
  gpsc_ctrl_type*  p_zGPSCControl,
  gpsc_raw_iono_type*     p_Iono
)
{
  gpsc_db_type*     p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
/*   gpsc_state_type*  p_zGPSCState = p_zGPSCControl->p_zGPSCState; */
  gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;
  U8               u_Data[8], *p_B = u_Data;

  *p_B++ = p_Iono->u_Alpha0;
  *p_B++ = p_Iono->u_Alpha1;
  *p_B++ = p_Iono->u_Alpha2;
  *p_B++ = p_Iono->u_Alpha3;
  *p_B++ = p_Iono->u_Beta0;
  *p_B++ = p_Iono->u_Beta1;
  *p_B++ = p_Iono->u_Beta2;
  *p_B++ = p_Iono->u_Beta3;

  z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ;
  z_PutMsgDesc.u_PacketId = AI_INJ_IONO;
  z_PutMsgDesc.p_Buf = u_Data;
  z_PutMsgDesc.w_PacketLen = 8;
  z_PutMsgDesc.u_ForceFlag = 0;
  z_PutMsgDesc.u_MsgSend = FALSE;

  AI2TX("Inject Iono",AI_INJ_IONO);
  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );

  /* once inject command is sent, mark LM database for this item "not-valid"
     until Sensor later reports this item and then update the database */
  p_zGPSCDatabase->z_DBIono.d_UpdateTimeSec = 0;

}

/*
 ******************************************************************************
 * gpsc_mgp_inject_init_utc
 *
 * Function description:
 *
 *  Form and send InjectUtc command to Sensor
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *
 * Return value:
 *  None
 *
 ******************************************************************************
 */
U8 gpsc_mgp_inject_init_utc
(
  gpsc_ctrl_type*  p_zGPSCControl
)
{
	gpsc_db_type* p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;

	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;
	  U8 u_Data[14], *p_B = u_Data;
	if(p_zGPSCDatabase->z_DBUtc.u_valid != TRUE)
	{
		STATUSMSG("gpsc_mgp_inject_init_utc: UTC time is invalid!");
		/* Values as on 05-07-2012 */
		p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_a0         = 0;
		p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_a1         = 512;
		p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_delta_tls  = 16;
		p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_tot        = 144;
		p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_wnt        = 159;
		p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_wnlsf      = 158;
		p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_dn         = 7;
		p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_delta_tlsf = 16;


	}


	  /* Bytes 0,1,2,3 are A0 */
	  p_B = NBytesPut(p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_a0,  p_B, sizeof (U32) );


	/* Bytes 4,5,6,7 are A1 */
	p_B = NBytesPut( p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_a1, p_B, sizeof(U32) );

	/* Byte 8 is DeltaTls */
	*p_B++ = p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_delta_tls;

	/* Byte 9 is Tot */
	*p_B++ = p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_tot;

	/* Byte 10 is WNt */
	*p_B++ = p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_wnt;

	/* Byte 11 is WNlsf */
	*p_B++ = p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_wnlsf;

	/* Byte 12 is DN */
	*p_B++ = p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_dn;

	/* Byte 13 is DeltaTlsf */
	*p_B++ = p_zGPSCDatabase->z_DBUtc.z_RawUtc.utc_delta_tlsf;


	  z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ;
	  z_PutMsgDesc.u_PacketId = AI_INJ_UTC;
	  z_PutMsgDesc.p_Buf = u_Data;
	  z_PutMsgDesc.w_PacketLen = 14;
	  z_PutMsgDesc.u_ForceFlag = 0;
	  z_PutMsgDesc.u_MsgSend = FALSE;

	  AI2TX("Inject UTC",AI_INJ_UTC);
	  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );

	  /* once inject command is sent, mark LM database for this item "not-valid"
	     until Sensor later reports this item and then update the database */
	  p_zGPSCDatabase->z_DBUtc.d_UpdateTimeSec = 0;
  p_zGPSCDatabase->z_DBUtc.u_valid = 0;

  return TRUE;

}


/*
 ******************************************************************************
 * gpsc_mgp_inject_dgps
 *
 * Function description:
 *
 *  Form and send InjectDgps command to Sensor
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *  p_dgps: pointer to an dgps structure.
 *
 * Return value:
 *  None
 * Date:  26/07/2006
 * Author : Raghavendra
 ******************************************************************************
 */

void gpsc_mgp_inject_dgps
(
  gpsc_ctrl_type*  p_zGPSCControl,
  gpsc_dgps_assist_type*     p_Dgps
)
{

  gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;
  U8  u_Data[151], *p_B = u_Data;   /* total packet length=16*9+ 7 */
  U8 u_i;
  U8 u_Count=0;
  dgps_assist * p_zdgps;

  p_B = NBytesPut( p_Dgps->q_gps_tow, p_B, sizeof(U32) );
  p_B = NBytesPut (0,p_B,sizeof(U16));  /* For Station ID */
  p_B = NBytesPut (0,p_B,sizeof(U8));  /* For Sequence Number */
  /* Not using at Present */
  //*p_B++ = p_Dgps->u_dgps_status; /* DGPS Status */
  //*p_B++ = p_Dgps->u_dgps_Nsat;   /* Number of dgps correction */


  for (u_i=0; u_i < 32; u_i++)
  {
     p_zdgps = (dgps_assist *)&p_Dgps->z_dgps[u_i];

	 if(p_zdgps ->u_svid)
	 {
	    *p_B++ = (U8) (p_zdgps->u_svid +1);
	    *p_B++ = p_zdgps->u_iode;
	    *p_B++ = p_zdgps->u_dgps_udre;
	    p_B = NBytesPut (0,p_B,sizeof(U8));  /* For Do not Use Flag  */
	    p_B = NBytesPut( p_zdgps->x_dgps_pseudo_range_cor, p_B, 3 );
	    p_B = NBytesPut( p_zdgps->b_dgps_range_rate_cor, p_B, sizeof(S16) );
		STATUSMSG("DGPS data for svid= %d  PRN = %d    PRC = %d    RRC = %d  injected to sensor",
			       p_zdgps ->u_svid,p_zdgps ->u_svid+1,p_zdgps->x_dgps_pseudo_range_cor,
				   p_zdgps->b_dgps_range_rate_cor);

		p_zdgps ->u_svid =0 ; // to clear the data
		u_Count++;
	  }

  }

  z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ;
  z_PutMsgDesc.u_PacketId = AI_INJ_DGPS_CORR;
  z_PutMsgDesc.p_Buf = u_Data;
  z_PutMsgDesc.w_PacketLen = (U16) (7 + (u_Count * 9)) ;
  z_PutMsgDesc.u_ForceFlag = 0;
  z_PutMsgDesc.u_MsgSend = FALSE;

  AI2TX("Inject Dgps",AI_INJ_DGPS_CORR);
  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );

}

/*
 ******************************************************************************
 * gpsc_mgp_inject_sv_health
 *
 * Function description:
 *
 *  Form and send InjectSvHealth command to Sensor and set almanac database to
 *  invalid.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *  p_SvHealth: pointer to the first element of an array holding Sv health
 *
 * Return value:
 *  None
 *
 ******************************************************************************
 */

void gpsc_mgp_inject_sv_health
(
  gpsc_ctrl_type*  p_zGPSCControl,
  U8*          p_SvHealth
)
{
/*   gpsc_db_type*     p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase; */
/*   gpsc_state_type*  p_zGPSCState    = p_zGPSCControl->p_zGPSCState; */
  gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;
  U8               u_Data[32], *p_B = u_Data;
  U8               u_i;

  for ( u_i = 0; u_i < N_SV; u_i++ )
  {
    *p_B++ = *p_SvHealth++;
  }

  z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ;
  z_PutMsgDesc.u_PacketId = AI_INJ_SV_HEALTH;
  z_PutMsgDesc.p_Buf = u_Data;
  z_PutMsgDesc.w_PacketLen = 32;
  z_PutMsgDesc.u_ForceFlag = 0;
  z_PutMsgDesc.u_MsgSend = FALSE;

  AI2TX("Inject Health",AI_INJ_SV_HEALTH);
  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );

  /* once inject command is sent, mark LM database for this item "not-valid"
     until Sensor later reports this item and then update the database */
  /* p_zGPSCDatabase->z_DBHealth.u_Valid = FALSE;*/

}



/*
 ******************************************************************************
 * InjectSvSteer
 *
 * Function description:
 *
 *  Form and send gpsc_mgp_inject_sv_steering command to Sensor.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *  p_zSvSteer: pointer to structure holding steering data.
 *
 *  u_SvId: PRN of the SV whose steering is to be injected.
 *
 *  u_SteerFlag: Bit map: Bit 0 - Run selection immediately.
 *                        Bit 1 - Force unconditionally.
 *
 * Return value:
 *  None
 *
 *
 ******************************************************************************
 */
void gpsc_mgp_inject_sv_steering
(
  gpsc_ctrl_type*  p_zGPSCControl,
  me_SvSteering*  p_zSvSteer,
  U8              u_SvId,
  U8              u_SteerFlag
)
{
  gpsc_ai2_put_msg_desc_type z_PutMsgDesc;
  InjSteer z_InjSteer, *p_zInjSteer = &z_InjSteer;

  U8  u_Data[22], *p_B = u_Data;
  FLT f_SvSpeed;


  STATUSMSG("Steer: Sv=%u, FC=%lu, Ms=%lu, SubMs=%f, Tunc=%f,SvSp=%f, SpUnc=%f, Acc=%f",
          u_SvId, p_zSvSteer->q_FCount, p_zSvSteer->z_SvTime.q_Ms,
          p_zSvSteer->z_SvTime.f_SubMs, p_zSvSteer->f_SvTimeUncMs,
          p_zSvSteer->f_SvSpeed, p_zSvSteer->f_SvSpeedUnc,
          p_zSvSteer->f_SvAccel);


  p_zInjSteer->u_SvId = u_SvId;
  p_zInjSteer->q_FCount = p_zSvSteer->q_FCount;

  p_zInjSteer->q_SvSubMsec_R24 = (U32) ( p_zSvSteer->z_SvTime.f_SubMs *
                       (FLT)((U32)1 << 24)
                     ); /* resolution: 1/2**24 msec */

  p_zInjSteer->q_SvMsec = p_zSvSteer->z_SvTime.q_Ms;


  /* SvTimeUnc: mantissa/exponent expression 11-5 */
  p_zInjSteer->w_SvTimeUnc = EncodeM11E5U16( p_zSvSteer->f_SvTimeUncMs * 1.0e6f );


  /* SvSpeed resolution 0.01 m/s */
    f_SvSpeed = p_zSvSteer->f_SvSpeed;
  if (p_zSvSteer->f_SvSpeed > 83886.07f )
  {
    f_SvSpeed = 83886.07f;
  }
  else if (p_zSvSteer->f_SvSpeed < -83886.07f )
  {
    f_SvSpeed = -83886.07f;
  }
  p_zInjSteer->l_SvSpeed_R24 = (S32)( f_SvSpeed * 100.0f );

  /* SvSpeedUnc resolution 0.1 m/s */
  p_zInjSteer->w_SvSpeedUnc = (U16)( p_zSvSteer->f_SvSpeedUnc * 10.0f);

  /* SvSpeedAcceleration resolution: 2/128 cm/s/s */
  p_zInjSteer->x_SvAccel = (S16) ( p_zSvSteer->f_SvAccel * 6400.0f );

  /* set steering flag to "force unconditionally " */
  p_zInjSteer->u_SteeringFlag = u_SteerFlag;

  *p_B++  = p_zInjSteer->u_SvId;
  p_B    = NBytesPut( p_zInjSteer->q_FCount, p_B, sizeof(U32) );
  p_B    = NBytesPut( p_zInjSteer->q_SvSubMsec_R24, p_B, 3 );
  p_B    = NBytesPut( p_zInjSteer->q_SvMsec, p_B, sizeof(U32) );
  p_B    = NBytesPut( p_zInjSteer->w_SvTimeUnc, p_B, sizeof(U16) );
  p_B    = NBytesPut( p_zInjSteer->l_SvSpeed_R24, p_B, 3 );
  p_B    = NBytesPut( p_zInjSteer->w_SvSpeedUnc, p_B, sizeof(U16) );
  p_B    = NBytesPut( p_zInjSteer->x_SvAccel, p_B, sizeof(S16) );
  *p_B  = p_zInjSteer->u_SteeringFlag;

  z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ;
  z_PutMsgDesc.u_PacketId = AI_INJ_SV_STEERING;
  z_PutMsgDesc.p_Buf = u_Data;
  z_PutMsgDesc.w_PacketLen = 22;
  z_PutMsgDesc.u_ForceFlag = 0;
  z_PutMsgDesc.u_MsgSend = FALSE;

  AI2TX("Inject Steering",AI_INJ_SV_STEERING);
  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}

/*
 ******************************************************************************
 * gpsc_mgp_inject_sv_diff
 *
 * Function description:
 *
 *  gpsc_mgp_inject_sv_diff allows the application to specify the time
 *  differences between satellites even though the absolute time is not
 *  particularly well known. This allows the time uncertainties in the
 *  individual SvSteering information to be high, but the time uncertainties
 *  on the differences to be small. The rate information in the SvSteering
 *  structure is used to propagate the differences.
 *
 *  Note - If the absolute time is well known (ie- The uncertainties in the
 *  SvSteering structure are small) then there is no value in using this
 *  function.
 *
 *  Note - Only N_LCHAN-1 SV time differences are stored.
 *
 *  Note - The SvSteering information for each satellite should be loaded
 *  using ms_SvSteeringPut before calling me_SvTimeDiffPut. The Measurement
 *  Engine will disregard difference information for Sv which do not have
 *  valid Steering information.
 *
 *  Note - The uncertainties are not differenced.  They contain the respective
 *  uncertainties (time, speed) for each SV only.  The uncertainties for a
 *  source and target SV are summed at the point they are used for SV differences.
 *  The SV time unc does NOT include the clock time uncertainty, and the SV
 *  speed unc does not include the clock speed uncertainty.
 *
 *
 * Parameters:
 *
 *  q_RefFCount - FCount where differences are refered to
 *  u_RefSv - Reference satellite
 *	f_RefSvTimeUncMs - Time Unc of reference SV only without clock time unc
 *	f_RefSvSpeedUnc - Speed Unc of reference SV only without clock speed unc
 *  u_NDiffs - Number of entries in p_Diff array
 *  p_Diff - Pointer to an array of SvDiffs
 *
 * Return value:
 *
 *  void
 *
 ******************************************************************************
*/
void gpsc_mgp_inject_sv_diff
(
  gpsc_ctrl_type*   p_zGPSCControl,
  U32             q_FCount,
  U8              u_RefSv,
  FLT             f_RefSvTimeUncMs,
  FLT             f_RefSvSpeedUnc,
  U8              u_NDiffs,
  me_SvDiff*      p_zSvDiff
)
{

  gpsc_ai2_put_msg_desc_type   z_PutMsgDesc;
  InjSvDiff    z_InjSvDiff[N_LCHAN], *p_zInjSvDiff;
  FLT          f_Float;
  U8           u_i;
  U16          w_PacketLen;
  U16          w_RefSvTimeUnc, w_RefSvSpeedUnc;

  U8           u_Data[9+N_LCHAN*11], *p_B = u_Data;

  /** assuming f_RefSvTimeUncMs < 1 ms **/
  f_Float = f_RefSvTimeUncMs * 65536.0f;
  if ( f_Float > 65535.0f ) f_Float = 65535.0f;
  w_RefSvTimeUnc = (U16)f_Float;

  /** assuming f_RefSvSpeedUnc < 6553.5 m/s */
  f_Float = f_RefSvSpeedUnc * 10.0f;
  if (f_Float > 65535.0f) f_Float = 65535.0f;
  w_RefSvSpeedUnc = (U16)f_Float;


  p_zInjSvDiff = &z_InjSvDiff[0];


  for (u_i = 0; u_i < u_NDiffs; u_i++)
  {

    p_zInjSvDiff->u_SvId = p_zSvDiff->u_Sv;
    p_zInjSvDiff->u_TimeStat = p_zSvDiff->u_DTimeStatus;
    /** assuming fabs(f_DTimeMsec) < 40 ms */
    f_Float = p_zSvDiff->f_DTimeMsecs / 40.0f * 8388608.0f;
    if (f_Float > 8388607.0f ) f_Float = 8388607.0f;
    else if (f_Float < -8388608.0f) f_Float = -8388608.0f;
    p_zInjSvDiff->q_DiffTime_R24 = (S32)f_Float;

    /** assuming f_NDTimeUncMs < 1 ms **/
    f_Float = p_zSvDiff->f_NDTimeUncMs * 65536.0f;
    if ( f_Float > 65535.0f ) f_Float = 65535.0f;
    p_zInjSvDiff->w_NonDiffTimeUnc = (U16)f_Float;

    /** assuming fabs(f_DSpeed) < 2000 m/s */
    f_Float = p_zSvDiff->f_DSpeed * 16.384f; /* 1/(2000/32768) = 16.384 */
    if (f_Float > 32767.0f ) f_Float = 32767.0f;
    else if (f_Float < -32768.0f) f_Float = -32768.0f;
    p_zInjSvDiff->x_DiffSpeed = (S16)f_Float;

    /** assuming f_NDSpeedUnc < 6553.5 m/s */
    f_Float = p_zSvDiff->f_NDSpeedUnc * 10.0f;
    if (f_Float > 65535.0f) f_Float = 65535.0f;
    p_zInjSvDiff->w_NonDiffSpeedUnc = (U16)f_Float;

    p_zSvDiff++;
    p_zInjSvDiff++;
  }

  /* compute size of the packet: RefSvId(1) + RefSvTUnc(2) + RefSvSpdUnc(2)
   *    + FCOUNT(4) + NDiffs * 11 */
  w_PacketLen = (U16)(9 + u_NDiffs * 11);

  *p_B++ = u_RefSv;
   p_B   = NBytesPut( w_RefSvTimeUnc, p_B, sizeof(U16) );
   p_B   = NBytesPut( w_RefSvSpeedUnc, p_B, sizeof(U16) );
   p_B   = NBytesPut(q_FCount, p_B, sizeof(U32) );

  p_zInjSvDiff = &z_InjSvDiff[0];
  for (u_i = 0; u_i < u_NDiffs; u_i++)
  {
    *p_B++ = p_zInjSvDiff->u_SvId;
    *p_B++ = p_zInjSvDiff->u_TimeStat;
     p_B   = NBytesPut( p_zInjSvDiff->q_DiffTime_R24, p_B, 3 );
     p_B   = NBytesPut( p_zInjSvDiff->w_NonDiffTimeUnc,  p_B, sizeof(U16) );
     p_B   = NBytesPut( p_zInjSvDiff->x_DiffSpeed,    p_B, sizeof(S16) );
     p_B   = NBytesPut( p_zInjSvDiff->w_NonDiffSpeedUnc, p_B, sizeof(U16) );
     p_zInjSvDiff++;
  }

  z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ;
  z_PutMsgDesc.u_PacketId = AI_INJ_SV_TIME_DIFF;
  z_PutMsgDesc.p_Buf = u_Data;
  z_PutMsgDesc.w_PacketLen = w_PacketLen;
  z_PutMsgDesc.u_ForceFlag = 0;
  z_PutMsgDesc.u_MsgSend = FALSE;

  AI2TX("Inject Time Diff",AI_INJ_SV_TIME_DIFF);
  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}

/*
 ******************************************************************************
 * gpsc_mgps_inject_pred_data_bits
 *
 * Function description:
 *
 *  Form and send InjectPredictedSvDatabits command to Sensor.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *  p_zPredDb: pointer to structure holding predicted data bits
 *
 * Return value:
 *  None
 *
 * Note:
 *  This function limits the maximum number of bits to be injected to 64.
 *
 ******************************************************************************
 */

void gpsc_mgps_inject_pred_data_bits
(
  gpsc_ctrl_type*  p_zGPSCControl,
  gpsc_pred_db_type*      p_zPredDb
)
{

  gpsc_ai2_put_msg_desc_type    z_PutMsgDesc;

  U8  u_Data[15], *p_B = u_Data;  /* 7 + max. 4 * 2 */
  U8  u_PacketLen;

  u_PacketLen = (U8)(7 + p_zPredDb->u_NumBits / 8);

  if (p_zPredDb->u_NumBits % 8)
  {
    u_PacketLen += 1;
  }

  *p_B++ = p_zPredDb->u_SvId;
  p_B = NBytesPut(p_zPredDb->q_BitOfWeek, p_B, sizeof (U32) );
  p_B = NBytesPut((U16)p_zPredDb->u_NumBits, p_B, sizeof (U16));

  *p_B++ = (U8)(p_zPredDb->q_PredictedBits[0] >> 24);
  *p_B++ = (U8)(p_zPredDb->q_PredictedBits[0] >> 16);
  *p_B++ = (U8)(p_zPredDb->q_PredictedBits[0] >> 8);
  *p_B++ = (U8)(p_zPredDb->q_PredictedBits[0]);
  *p_B++ = (U8)(p_zPredDb->q_PredictedBits[1] >> 24);
  *p_B++ = (U8)(p_zPredDb->q_PredictedBits[1] >> 16);
  *p_B++ = (U8)(p_zPredDb->q_PredictedBits[1] >> 8);
  *p_B++ = (U8)(p_zPredDb->q_PredictedBits[1]);

  z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ;
  z_PutMsgDesc.u_PacketId = AI_INJ_PRED_SV_DATA;
  z_PutMsgDesc.p_Buf = u_Data;
  z_PutMsgDesc.w_PacketLen = u_PacketLen;
  z_PutMsgDesc.u_ForceFlag = 0;
  z_PutMsgDesc.u_MsgSend = FALSE;

  AI2TX("Inject Pred Data",AI_INJ_PRED_SV_DATA);
  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}

/*
 ******************************************************************************
 * gpsc_mgp_inject_sv_dir
 *
 * Function description:
 *
 *  Form and send InjectSvDirections command to Sensor.
 *
 * Parameters:
 *
 *	p_zGPSCControl: pointer to the LmControl structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *  p_zInjectSvDir: pointer to array of direction headings
 *  count:   count of directions headings in array
 *
 * Return value:
 *	None
 *
 * Note:
 *
 ******************************************************************************
 */
void gpsc_mgp_inject_sv_dir
(
  gpsc_ctrl_type*   p_zGPSCControl,
  gpsc_inj_sv_dir*  p_zInjectSvDir,
  U8             u_Count
)
{

  gpsc_ai2_put_msg_desc_type    z_PutMsgDesc;

  U8  u_Data[49], *p_B = u_Data;  /* 1 + max. 16 * 3 */
  U8  u_PacketLen;
  U8  u_i;
  *p_B++ = 0; /* Run Selection = 0 */

  for (u_i=0; u_i<u_Count/*C_MAX_SVS_ACQ_ASSIST*/; u_i++)
  {
    if (p_zInjectSvDir->u_SvId != 0 )
        {
          *p_B++ = p_zInjectSvDir->u_SvId;
          *p_B++ = p_zInjectSvDir->b_Elev;
          *p_B++ = p_zInjectSvDir->u_Azim;

      //u_cnt++;
        }
        p_zInjectSvDir++;
  }

  u_PacketLen = (U8)(/*7 +*/ 1 + (u_Count /*u_cnt*/ * 3));

  z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ;
  z_PutMsgDesc.u_PacketId = AI_INJ_SV_DIR;
  z_PutMsgDesc.p_Buf = u_Data;
  z_PutMsgDesc.w_PacketLen = u_PacketLen;
  z_PutMsgDesc.u_ForceFlag = 0;
  z_PutMsgDesc.u_MsgSend = FALSE;

  AI2TX("Inject Sv Directions",AI_INJ_SV_DIR);
  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}

void gpsc_mgp_inject_sv_dir_almanac
(
  gpsc_ctrl_type*   p_zGPSCControl,
  gpsc_inj_sv_dir*  p_zInjectSvDir,
  U8             u_Count
)
{

  gpsc_ai2_put_msg_desc_type    z_PutMsgDesc;

  U8  u_Data[49], *p_B = u_Data;  /* 1 + max. 16 * 3 */
  U8  u_PacketLen;
  U8  u_i;
  *p_B++ = 1; /* Run Selection = 1 */

  for (u_i=0; u_i<u_Count/*C_MAX_SVS_ACQ_ASSIST*/; u_i++)
  {
    if (p_zInjectSvDir->u_SvId != 0 )
        {
          *p_B++ = p_zInjectSvDir->u_SvId;
          *p_B++ = p_zInjectSvDir->b_Elev;
          *p_B++ = p_zInjectSvDir->u_Azim;

      //u_cnt++;
        }
        p_zInjectSvDir++;
  }

  u_PacketLen = (U8)(/*7 +*/ 1 + (u_Count /*u_cnt*/ * 3));

  z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ;
  z_PutMsgDesc.u_PacketId = AI_INJ_SV_DIR;
  z_PutMsgDesc.p_Buf = u_Data;
  z_PutMsgDesc.w_PacketLen = u_PacketLen;
  z_PutMsgDesc.u_ForceFlag = 0;
  z_PutMsgDesc.u_MsgSend = FALSE;

  AI2TX("Inject Sv Directions",AI_INJ_SV_DIR);
  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}
/*
 ******************************************************************************
 * gpsc_mgp_inject_OscParams
 *
 * Function description:
 *
 *  Form and send InjectOscParams command to Sensor.
 *  This will send the external oscillator Frequency & Quality to
 *  Sensor to start frequency calibration
 *
 * Parameters:
 *
 *	p_zGPSCControl: None
 *  f_OscQual:   PPM uncertainty of Oscillator Frequency
 *
 *
 * Return value:
 *	None
 *
 * Note:
 *
 ******************************************************************************
 */

void gpsc_mgp_inject_OscParams
(
  gpsc_ctrl_type*   p_zGPSCControl
)
{
   gpsc_cfg_type		*p_zGPSCConfig		= p_zGPSCControl->p_zGPSCConfig;
   gpsc_state_type*	p_zGPSCState =	p_zGPSCControl->p_zGPSCState;
   gpsc_ai2_put_msg_desc_type    z_PutMsgDesc;
   U8	u_Data[7],*p_B = u_Data;

	/* Byte 0 is Locked to Network Flag. This flag is not used by the sensor. */
	*p_B++ = 0;

	STATUSMSG("gpsc_mgp_inject_OscParams: Freq: %d, Q: %d",
		p_zGPSCConfig->ref_clock_frequency, p_zGPSCConfig->ref_clock_quality);

	p_B = NBytesPut(p_zGPSCConfig->ref_clock_quality , p_B, sizeof( U16 ) );

	/* Bytes 3,4,5,6 are Oscillator Frequency */
	p_B = NBytesPut( p_zGPSCConfig->ref_clock_frequency, p_B, sizeof (U32) );


	z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ;
	z_PutMsgDesc.u_PacketId = AI_INJ_CE_OSC_PARAMS;
	z_PutMsgDesc.p_Buf = u_Data;
	z_PutMsgDesc.w_PacketLen = 7;
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;

	AI2TX("Inject CE Osc Params",AI_INJ_CE_OSC_PARAMS);
	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
	if(p_zGPSCState->u_ClockQualInjectPending == TRUE)
		maximize_frunc(p_zGPSCControl);

	p_zGPSCState->u_ClockQualInjectPending =  FALSE;


}

/*
 ******************************************************************************
 * gpsc_mgp_inject_CalibTimestamp_Period
 *
 * Function description:
 *
 *  Form and send Calibration Timestamp Period command to Sensor.
 *  This will send the calibration period and period uncertainity to
 *  Sensor to start frequency calibration
 *
 * Parameters:
 *
 *	p_zGPSCControl: None
 *
 *
 * Return value:
 *	None
 *
 * Note:
 *
 ******************************************************************************
 */

void gpsc_mgp_inject_CalibTimestamp_Period
(
  gpsc_ctrl_type*   p_zGPSCControl
)
{
	gpsc_cfg_type		*p_zGPSCConfig		= p_zGPSCControl->p_zGPSCConfig;
	gpsc_ai2_put_msg_desc_type    z_PutMsgDesc;
	U8	u_Data[5],*p_B = u_Data;

	/* Sub Packet 7 */
	p_B = NBytesPut(AI_CLAPP_CUSTOM_SUB_PKT_INJ_CALIB_TIME_STAMP_PERIOD, p_B,sizeof(U8));
	/* Bytes 1 for calibration period */
	p_B = NBytesPut(p_zGPSCConfig->calib_period , p_B, sizeof( U16 ) );

	/* Bytes 2 for period uncertainity */
	p_B = NBytesPut( p_zGPSCConfig->period_uncertainity, p_B, sizeof (U16) );


	z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ;
	z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_CUSTOM_PACKET;
	z_PutMsgDesc.p_Buf = u_Data;
	z_PutMsgDesc.w_PacketLen = 5;
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;

	AI2TX("Inject Calib Timestamp Period",AI_CLAPP_CUSTOM_SUB_PKT_INJ_CALIB_TIME_STAMP_PERIOD);
	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}


/*
 ******************************************************************************
 * gpsc_mgp_inject_nvs_data
 *
 * Function description:
 *
 *  The function inject all the aiding stored in the NVS, from any previous session,
 *  based on the selection made in the configuration file
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *
 * Return value:
 *  None
 *
 ******************************************************************************
 */

U8 gpsc_mgp_inject_nvs_data ( gpsc_ctrl_type*  p_zGPSCControl )
{
	gpsc_cfg_type* p_zGPSCConfig	= p_zGPSCControl->p_zGPSCConfig;
	gpsc_state_type*		p_zGPSCState    = p_zGPSCControl->p_zGPSCState;
	gpsc_db_type* p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_nv_pos_type *p_zDbNvsPos = &p_zGPSCDatabase->z_NvsPos;

	STATUSMSG("\nIn gpsc_mgp_inject_nvs_data\n");
 	/***** EPH *****/
	if(p_zGPSCState->z_nvm_inj_state == NVM_INJ_EPH)
	{
		if (p_zGPSCConfig->eph_inject_check == TRUE)
		{
			STATUSMSG("gpsc_mgp_inject_nvs_data: Inject NVS EPH\n");
			if(gpsc_mgp_inject_nvs_eph(p_zGPSCControl))
			{
				/* request for ephmeris */
				p_zGPSCState->z_nvm_inj_state++;
			}
		}
		else
			p_zGPSCState->z_nvm_inj_state++;
	}

 	/***** ALM *****/
	if(p_zGPSCState->z_nvm_inj_state == NVM_INJ_ALM)
	{
		if (p_zGPSCConfig->alm_inject_check == TRUE)
		{
			STATUSMSG("gpsc_mgp_inject_nvs_data: Inject NVS ALM");
			if(gpsc_mgp_inject_nvs_alm(p_zGPSCControl))
			{
				/* request for almanac */
				p_zGPSCState->z_nvm_inj_state++;
			}
		}
		else
			p_zGPSCState->z_nvm_inj_state++;
	}

 	/***** POS *****/
	if(p_zGPSCState->z_nvm_inj_state == NVM_INJ_POS)
	{
		if (p_zGPSCConfig->pos_inject_check == TRUE)
		{
			STATUSMSG("gpsc_mgp_inject_nvs_data: Inject NVS Position\n");

	/* If d_ToaSec is 0 then it is a cold start or we don't have the nvs position file */
		      if((U32)p_zDbNvsPos->d_ToaSec != 0)
		      {

			if(gpsc_mgp_inject_nvs_pos(p_zGPSCControl))
			{
				/* Request for position */
				gpsc_mgp_tx_req( p_zGPSCControl, AI_CLAPP_REQ_POS_EXT, 0 );
				if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
				{
					FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
					GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
				}
			}
		}
		}
		p_zGPSCState->z_nvm_inj_state++;
	}

 	/***** UTC *****/
	if(p_zGPSCState->z_nvm_inj_state == NVM_INJ_UTC)
	{
		if (p_zGPSCConfig->utc_inject_check == TRUE)
		{
			STATUSMSG("gpsc_mgp_inject_nvs_data: Inject NVS UTC");
			if(gpsc_mgp_inject_init_utc(p_zGPSCControl))
			{
				/* Request for UTC */
				//gpsc_mgp_tx_req( p_zGPSCControl, AI_REQ_UTC, 0 );
				if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
				{
					FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
					GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
				}
			}
		}
		p_zGPSCState->z_nvm_inj_state++;
	}

 	/***** IONO *****/
	if(p_zGPSCState->z_nvm_inj_state == NVM_INJ_IONO)
	{
		if (p_zGPSCConfig->ion_inject_check == TRUE)
		{
			STATUSMSG("gpsc_mgp_inject_nvs_data: Inject NVS Iono");
			if(gpsc_mgp_inject_nvs_iono(p_zGPSCControl))
			{
				/* Request for IONO */
				//gpsc_mgp_tx_req( p_zGPSCControl, AI_REQ_IONO, 0 );
				if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
				{
					FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
					GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
				}
			}
		}
		p_zGPSCState->z_nvm_inj_state++;
	}

 	/***** HEALTH *****/
	if(p_zGPSCState->z_nvm_inj_state == NVM_INJ_HEALTH)
	{
		if (p_zGPSCConfig->health_inject_check == TRUE)
		{
			STATUSMSG("gpsc_mgp_inject_nvs_data: Inject NVS Health");
			if(gpsc_mgp_inject_nvs_health(p_zGPSCControl))
			{
				/* Request for health */
				//gpsc_mgp_tx_req( p_zGPSCControl, AI_REQ_SV_HEALTH, 0 );
				if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
				{
					FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
					GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
				}
			}
		}
		p_zGPSCState->z_nvm_inj_state++;
	}

 	/***** DONE *****/
	if(p_zGPSCState->z_nvm_inj_state == NVM_INJ_DONE)
	{
		return TRUE;
	}

	return FALSE;
}

/*
 ******************************************************************************
 * gpsc_mgp_inject_init_data
 *
 * Function description:
 *
 *  At reset, after verifying Sensor running the Sensor program, inject initial
 *  data into Sensor, mainly from non-volatile memory.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *
 * Return value:
 *  None
 *
 ******************************************************************************
 */

extern gpsc_db_gps_clk_type znvs_GpsTime;

U8 gpsc_mgp_inject_init_time
(
  gpsc_ctrl_type*  p_zGPSCControl
)
{
   gpsc_db_type*  p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
   gpsc_db_gps_clk_type*  p_DBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;
  gpsc_inject_time_est_type  z_InjectTimeEst;
  T_GPSC_time_assist z_CurrentTime;
  	gpsc_sys_handlers*	p_GPSCSysHandlers;
	McpS32   fd1;
 	McpU8 uAidingFile1[C_MAX_AIDING_FILENAME_LEN+20];

  	 p_GPSCSysHandlers = p_zGPSCControl->p_zSysHandlers;


  z_CurrentTime.gps_week = C_GPS_WEEK_UNKNOWN;
  /*Clear clock info in database*/
  gpsc_db_clear_clock(p_zGPSCControl);
 z_CurrentTime.time_accuracy = GPSC_TIME_ACCURACY_COARSE;
 #if 0 //comented to use the system time
  result = gpsc_time_request_tow_ind(&z_CurrentTime);
 #endif
 //Modification for NVS stored GPS Time iniection
   McpU32 pre_systime = 0;
   McpU32 cur_systime = 0;
   McpU32 time_diff = 0;

			MCP_HAL_STRING_StrCpy (uAidingFile1,(McpU8*)p_GPSCSysHandlers->uAidingPath);
			MCP_HAL_STRING_StrCat (uAidingFile1,C_STR_AID_SYSCLOCK_FILE);

			if (mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *) uAidingFile1,
					MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_RDONLY, &fd1) == RES_OK)
			{
				mcpf_file_read(p_zGPSCControl->p_zSysHandlers->hMcpf, fd1,
						(void *)&pre_systime, sizeof(pre_systime));

				STATUSMSG("gpsc_mgp_inject_init_time: pre_systime: %d",pre_systime);

				mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd1);
			}
			else
			{
				STATUSMSG("gpsc_mgp_inject_init_time: Error opening SysClock file");
			}
   cur_systime = os_get_current_time_inSec(NULL);
   time_diff = cur_systime - pre_systime;

   STATUSMSG("[GPS]nvs_GpsTime->u_Valid=%x	cur_systime=%u pre_systime=%u diff=%u,gpsmsec=%d",
	 znvs_GpsTime.u_Valid, cur_systime, pre_systime, time_diff,znvs_GpsTime.q_GpsMs);
   if(cur_systime < pre_systime)
   {
	   STATUSMSG("[GPS] ERR:gpsc_mgp_inject_init_time : cur_systime=%d < pre_systime=%d", cur_systime, pre_systime);
   }
   else if(time_diff > 14400) // 4 hours
   {
	   STATUSMSG("[GPS] ERR: gpsc_mgp_inject_init_time:time_diff is too large %d\n", time_diff);
   }
   else if(znvs_GpsTime.u_Valid != 0)
   {
	   z_CurrentTime.gps_week = znvs_GpsTime.w_GpsWeek;
	   z_CurrentTime.gps_msec = znvs_GpsTime.q_GpsMs + (time_diff * 1000);
   }

   if (z_CurrentTime.gps_week == C_GPS_WEEK_UNKNOWN)
   {
      /* If the GPS week is still unknown, the GPS time 
       * saved in NVS was not valid. Inject the system time instead */
      ALOGI("%s: No valid NVS GPS time found, injecting system time instead.", __FUNCTION__);
      gpsc_time_request_tow_ind(&z_CurrentTime);
       
   }
   STATUSMSG("[GPS]: gpsc_mgp_inject_init_time:  gps_week=%d gps_msec=%d",
	 z_CurrentTime.gps_week, z_CurrentTime.gps_msec);
   ////Modification for NVS stored GPS Time iniection

   /*CUSTOM*/
   /* compute time unc
   	  Assumption is that RTC time will degrade at a rate of 10s/day.
   	  Limit the min uncertainty to 60s */
   z_CurrentTime.time_unc = (U32) ((FLT)time_diff * (FLT)C_RTC_TIME_DRIFT_RATE_PER_DAY_IN_S)/(1000.0*1000.0);
   if (z_CurrentTime.time_unc < (C_MIN_TIME_UNC_THRESHOLD))
   {
      z_CurrentTime.time_unc = (U32) (C_MIN_TIME_UNC_THRESHOLD) ;
   }

  /*If cannot establish knowledge of time at all, don't inject
	any data */

  if(z_CurrentTime.gps_week == C_GPS_WEEK_UNKNOWN)
  {
	STATUSMSG("Status: Week unknown");
	return FALSE;
  }
  if(z_CurrentTime.time_unc > C_MAX_VALID_TIME_UNC_MS * 1000)
  {
	STATUSMSG("Status: Bad Unc: %.2f secs",z_CurrentTime.time_unc/(1000*1000));
	return FALSE;
  }

  /* update the DB */
    p_DBGpsClock->w_GpsWeek = z_CurrentTime.gps_week;
    p_DBGpsClock->q_GpsMs = z_CurrentTime.gps_msec;
    p_DBGpsClock->f_TimeUnc = z_CurrentTime.time_unc;
    p_DBGpsClock->u_Valid = TRUE;

  /* Inject Time Information:  */
  z_InjectTimeEst.u_Mode = 1; /* Mode: TimeNow */
  z_InjectTimeEst.q_Fcount = 0; /* irrelevant */
  z_InjectTimeEst.z_meTime.w_GpsWeek = z_CurrentTime.gps_week;
  z_InjectTimeEst.z_meTime.q_GpsMsec = z_CurrentTime.gps_msec;

  /* irrelevant as TimeNow mode is used */
  z_InjectTimeEst.z_meTime.f_ClkTimeBias =0;

  z_InjectTimeEst.z_meTime.f_ClkTimeUncMs = (FLT)z_CurrentTime.time_unc/1000;
  z_InjectTimeEst.u_ForceFlag = 0;

  gpsc_mgp_inject_gps_time( p_zGPSCControl, &z_InjectTimeEst );
  /*Now the database has new clock info*/
  STATUSMSG("Status: RTC time injected");
  return TRUE;
}


U8 gpsc_mgp_inject_nvs_pos(gpsc_ctrl_type * p_zGPSCControl)
{
	gpsc_cfg_type* p_zGPSCConfig	 = p_zGPSCControl->p_zGPSCConfig;
	gpsc_db_type* p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_nv_pos_type *p_zDbNvsPos = &p_zGPSCDatabase->z_NvsPos;
	gpsc_inject_pos_est_type z_InjectPosEst;
	gpsc_inject_altitude_type  p_InjectAltitude;
    	S32 l_PosAgeSec = 0;
	U32 q_PosUnc = 0;
	U32 q_CurrentTime = 0;

	if (!p_zDbNvsPos->d_ToaSec)
	{
		ALOGI("gpsc_mgp_inject_nvs_pos: invalid d_ToaSec: %d", p_zDbNvsPos->d_ToaSec);
		return FALSE;
	}

	q_CurrentTime = gpsc_db_gps_time_in_secs(p_zGPSCControl);
	l_PosAgeSec =  q_CurrentTime  - (U32)p_zDbNvsPos->d_ToaSec;
	q_PosUnc = p_zGPSCConfig->pos_velocty * l_PosAgeSec;

	ALOGI("gpsc_mgp_inject_nvs_pos: dbSec: %d, Cur Time: %d, Age:%d, Velo: %d, Unc:%d, Threshold:%d",
	       (U32)p_zDbNvsPos->d_ToaSec, q_CurrentTime, l_PosAgeSec,
	       p_zGPSCConfig->pos_velocty, q_PosUnc, p_zGPSCConfig->pos_unc_threshold);
	
	if (p_zGPSCConfig->pos_validate_check == TRUE)
	{
		if(q_CurrentTime == FALSE)
		{
			ALOGI("gpsc_mgp_inject_nvs_pos: Time not useful");
			return FALSE;
		}

		if (l_PosAgeSec <= 0)
		{
			ALOGI("gpsc_mgp_inject_nvs_pos: l_PosAgeSec is -ve, reject position");
			return FALSE;
		}

		if(q_PosUnc > p_zGPSCConfig->pos_unc_threshold)
		{
			ALOGI("gpsc_mgp_inject_nvs_pos: Position Unc too large: %d", q_PosUnc);
			return FALSE;
		}

	}
	else
		ALOGI("gpsc_mgp_inject_nvs_pos: No Validity Check");

	/* Never want to inject [0,0] lat/long */
	if (p_zDbNvsPos->l_PosLat == 0 && p_zDbNvsPos->l_PosLong == 0)
	{
		ALOGI("gpsc_mgp_inject_nvs_pos: Position is invalid [0,0], reject position");
		return FALSE;
	}        

	/* inject position */
	z_InjectPosEst.l_Lat = p_zDbNvsPos->l_PosLat;
	z_InjectPosEst.l_Long = p_zDbNvsPos->l_PosLong;
	z_InjectPosEst.x_Ht = p_zDbNvsPos->x_PosHt;

	/* add the computed position uncertain */
	p_zDbNvsPos->f_PosUnc += q_PosUnc;
	z_InjectPosEst.q_Unc = (U32) (p_zDbNvsPos->f_PosUnc * C_RECIP_LSB_POS_SIGMA);

	if (z_InjectPosEst.q_Unc < C_MIN_NVS_POS_UNC_1KM)
	{
                ALOGI("gpsc_mgp_inject_nvs_pos: Position uncertainty too low. Capping to 1 KM.");
		z_InjectPosEst.q_Unc = C_MIN_NVS_POS_UNC_1KM;
	}
	else if (z_InjectPosEst.q_Unc > C_MAX_NVS_POS_UNC_500KM)
	{
                ALOGI("gpsc_mgp_inject_nvs_pos: Position uncertainty too high. Capping to 500 KM.");
		z_InjectPosEst.q_Unc =  C_MAX_NVS_POS_UNC_500KM;
		
	}

	/* use Mode = 1: use the time in receiver */
	z_InjectPosEst.u_Mode = 1;
	z_InjectPosEst.w_GpsWeek = 0;
	z_InjectPosEst.q_GpsMsec = 0;

	 /* force injection */
	z_InjectPosEst.u_ForceFlag = 0;
	/* inject */
	gpsc_mgp_inject_pos_est(p_zGPSCControl, &z_InjectPosEst, TRUE);


	/* populate altiude */
	p_InjectAltitude.x_Altitude = p_zDbNvsPos->x_PosHt/2;
	p_InjectAltitude.w_AltitudeUnc = p_zGPSCConfig->altitude_unc;
	p_InjectAltitude.u_ForceFlag = 0;

	/* inject altitude */
	gpsc_mgp_inject_altitude(p_zGPSCControl, &p_InjectAltitude);

	ALOGI("gpsc_mgp_inject_nvs_pos: Injected Lat: %d, Lon: %d, Ht:%d, PosUnc: %d, AltUnc: %d",
	      z_InjectPosEst.l_Lat, z_InjectPosEst.l_Long, z_InjectPosEst.x_Ht, z_InjectPosEst.q_Unc, p_InjectAltitude.w_AltitudeUnc);

	return TRUE;
}

U8 gpsc_mgp_inject_nvs_alm(gpsc_ctrl_type* p_zGPSCControl)
{
	gpsc_cfg_type* p_zGPSCConfig	 = p_zGPSCControl->p_zGPSCConfig;
	gpsc_db_type* p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_db_alm_type	*p_zDBAlmanac	= &p_zGPSCDatabase->z_DBAlmanac[0];
	U8 u_SvCnt = p_zGPSCDatabase->u_AlmCounter;
	U8 u_TxAlm = FALSE;
	U8 u_AlmFound = FALSE;
	U16 w_CurrentWeek = 0;
	U8 u_MaxSvsPerMsgCnt=0; /* To transmit only 4 alm at a time */

	if (p_zGPSCConfig->alm_validate_check == TRUE)
	{
		w_CurrentWeek = gpsc_db_gps_week(p_zGPSCControl);
		if(w_CurrentWeek == FALSE)
		{
			ALOGI("gpsc_mgp_inject_nvs_alm: Time not useful");
			return TRUE;
		}
	}

	ALOGI("gpsc_mgp_inject_nvs_alm: Validate %d, Threshold %dweeks",
		p_zGPSCConfig->alm_validate_check, p_zGPSCConfig->week_threshold);

        /* Only inject 1 almanac at a time */
	while(gpsc_comm_check_bufferspace(p_zGPSCControl,100) && (u_SvCnt < N_SV) && (u_MaxSvsPerMsgCnt < 1))
	{
		/*Search for valid almanac*/
		if(p_zDBAlmanac[u_SvCnt].u_Valid ==TRUE)
		{
			/* Check if the ALM is withing the Threshold */
			if (p_zGPSCConfig->alm_validate_check == TRUE &&
				(w_CurrentWeek - p_zDBAlmanac[u_SvCnt].z_RawAlmanac.w_Week) > p_zGPSCConfig->week_threshold)
			{
				ALOGI("gpsc_mgp_inject_nvs_alm: Rejected ALM for Sv%d with Week %d",
					u_SvCnt+1, p_zDBAlmanac[u_SvCnt].z_RawAlmanac.w_Week);
				p_zDBAlmanac[u_SvCnt].u_Valid = FALSE;
			}

            if(u_SvCnt < N_SV)
			{
				u_TxAlm = gpsc_mgp_inject_alm( p_zGPSCControl, &p_zDBAlmanac[u_SvCnt].z_RawAlmanac );

				ALOGI("gpsc_mgp_inject_nvs_alm: Injected ALM for Sv %d ",u_SvCnt +1);
				p_zGPSCDatabase->q_InjAlmList |= (1<<u_SvCnt);
				u_AlmFound = TRUE;
				u_MaxSvsPerMsgCnt++;
						#if 0
						/*Make Invalid, it will be set to invalid when it is received from GPS*/
						STATUSMSG("d_FullToa %d ", p_zDBAlmanac->z_RawAlmanac.d_FullToa);
						STATUSMSG("d_UpdateTime %d ", p_zDBAlmanac->z_RawAlmanac.d_UpdateTime);
						STATUSMSG("q_MZero %d ", p_zDBAlmanac->z_RawAlmanac.q_MZero);
						STATUSMSG("q_Omega %d ", p_zDBAlmanac->z_RawAlmanac.q_Omega);
						STATUSMSG("q_OmegaZero %d ", p_zDBAlmanac->z_RawAlmanac.q_OmegaZero);
						STATUSMSG("q_SqrtA %d ", p_zDBAlmanac->z_RawAlmanac.q_SqrtA);
						STATUSMSG("u_Health %d ", p_zDBAlmanac->z_RawAlmanac.u_Health);
						STATUSMSG("u_Prn %d ", p_zDBAlmanac->z_RawAlmanac.u_Prn);
						STATUSMSG("u_Toa %d ", p_zDBAlmanac->z_RawAlmanac.u_Toa);
						STATUSMSG("w_Af0 %d ", p_zDBAlmanac->z_RawAlmanac.w_Af0);
						STATUSMSG("w_Af1 %d ", p_zDBAlmanac->z_RawAlmanac.w_Af1);
						STATUSMSG("w_DeltaI %d ", p_zDBAlmanac->z_RawAlmanac.w_DeltaI);
						STATUSMSG("w_E %d ", p_zDBAlmanac->z_RawAlmanac.w_E);
						STATUSMSG("w_OmegaDot %d ", p_zDBAlmanac->z_RawAlmanac.w_OmegaDot);
						STATUSMSG("w_Week %d ", p_zDBAlmanac->z_RawAlmanac.w_Week);
						#endif
			}
		}
		u_SvCnt++;
		p_zGPSCDatabase->u_AlmCounter = u_SvCnt;
	}

	if(u_TxAlm)
	{
		if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_ACK_REQ)==FALSE)
		{
			FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
			GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
		}
	}

	if (u_SvCnt >=N_SV)
		return TRUE; /* ALM injection is complete */

	return FALSE;
}

U8 gpsc_mgp_inject_nvs_eph (gpsc_ctrl_type*  p_zGPSCControl)
{
	gpsc_cfg_type* p_zGPSCConfig	 = p_zGPSCControl->p_zGPSCConfig;
	gpsc_db_type* p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_db_eph_type	*p_zDBEphemeris		= &p_zGPSCDatabase->z_DBEphemeris[0];
	U8 u_SvCnt = p_zGPSCDatabase->u_EphCounter;
	U8 u_TxEph = FALSE;
	U16 w_CurrentWeek = 0;
	U32 q_CurrentSec = 0, q_CurrentTowSec=0;
	U16 w_WeekDiff = 0;
	S32 q_SecDiff=0;
	FLT f_HrsDiff=0;
        U8 u_MaxSvsPerMsgCnt=0; /* To transmit only 1 eph at a time */

	/*If no time was injected, this will return 0*/
	if (p_zGPSCConfig->eph_validate_check == TRUE)
	{
		q_CurrentSec = gpsc_db_gps_time_in_secs(p_zGPSCControl);
		w_CurrentWeek = gpsc_db_gps_week(p_zGPSCControl);
		if(q_CurrentSec == FALSE || w_CurrentWeek == FALSE)
		{
			ALOGI("gpsc_mgp_inject_nvs_eph: Time not useful");
			return TRUE;
		}
	}

	ALOGI("gpsc_mgp_inject_nvs_eph: Validate %d, Threshold %dhrs",
		p_zGPSCConfig->eph_validate_check, p_zGPSCConfig->hours_thrshold);

	while(gpsc_comm_check_bufferspace(p_zGPSCControl,100) && u_SvCnt < N_SV  && (u_MaxSvsPerMsgCnt < 1))
	{
		if((p_zDBEphemeris[u_SvCnt].u_Valid ==TRUE) && (p_zDBEphemeris[u_SvCnt].w_EphWeek != C_GPS_WEEK_UNKNOWN) &&
		   (p_zDBEphemeris[u_SvCnt].z_RawEphemeris.u_predicted != TRUE))
		{

			/* Check if the EPH is withing the Threshold */
			if (p_zGPSCConfig->eph_validate_check == TRUE)
			{
				ALOGI("w_CurrentWeek: %d, q_CurrentSec:%d, EphWeek:%d, EphToe:%d",
					w_CurrentWeek,q_CurrentSec,p_zDBEphemeris[u_SvCnt].w_EphWeek,p_zDBEphemeris[u_SvCnt].z_RawEphemeris.w_Toe);

				q_CurrentTowSec = (q_CurrentSec - (w_CurrentWeek * WEEK_SECS));
				w_WeekDiff = w_CurrentWeek - p_zDBEphemeris[u_SvCnt].w_EphWeek;

				if (w_WeekDiff > 0)
				{
					/* account for any week roll overs */
					q_SecDiff =  (q_CurrentTowSec + (w_WeekDiff * WEEK_SECS) ) - (p_zDBEphemeris[u_SvCnt].z_RawEphemeris.w_Toe * 16);

				}
				else
				{
					q_SecDiff =  (q_CurrentTowSec  - (p_zDBEphemeris[u_SvCnt].z_RawEphemeris.w_Toe * 16));
				}

				f_HrsDiff = ((w_WeekDiff * WEEK_HRS) + ((FLT)q_SecDiff /3600.0));


				ALOGI("q_SecDiff: %d, w_WeekDiff:%d, f_HrsDiff:%f,abso f_Hrs%f\n",q_SecDiff,w_WeekDiff,f_HrsDiff,mcpf_Fabs(f_HrsDiff));

				if (mcpf_Fabs(f_HrsDiff) > p_zGPSCConfig->hours_thrshold)
				{
					p_zDBEphemeris[u_SvCnt].u_Valid= FALSE;
					ALOGI("gpsc_mgp_inject_nvs_eph: Rejected Epheremis for Sv %f with age %fhrs",
						u_SvCnt+1, f_HrsDiff);
				}
			}

			if(p_zDBEphemeris[u_SvCnt].u_Valid != FALSE)
			{
				/*  Inject Ephemeris to Sensor if LM has valid record. Eph. week and TOE
				are included in the message so it's up to the PE to decide whether to
				accept it */
				gpsc_inject_eph_type z_InjectEph, *p_zInjectEph = &z_InjectEph;

				/*  Inject Ephemeris to Sensor if LM has valid record. Eph. week and TOE
				are included in the message so it's up to the PE to decide whether to
				accept it */
				p_zInjectEph->p_zRawEphemeris = &p_zDBEphemeris[u_SvCnt].z_RawEphemeris;
				p_zInjectEph->p_zRawSF1Param  = &p_zDBEphemeris[u_SvCnt].z_RawSF1Param;
				p_zInjectEph->w_EphWeek       = p_zDBEphemeris[u_SvCnt].w_EphWeek;

				u_TxEph = gpsc_mgp_inject_eph(p_zGPSCControl, p_zInjectEph,1);
                                u_MaxSvsPerMsgCnt++;
				ALOGI("gpsc_mgp_inject_nvs_eph: Injected Epheremis for Sv %d with age %fhrs",u_SvCnt +1,f_HrsDiff);

				/*Make Invalid, it will be set to invalid when it is received from GPS*/
				//p_zDBEphemeris[u_SvCnt].u_Valid = FALSE;
				p_zGPSCDatabase->q_InjEphList |= (1 <<u_SvCnt);

						#if 0
						STATUSMSG("p_zRawEphemeris update time %d ", p_zInjectEph->p_zRawEphemeris->d_UpdateTime);
						STATUSMSG("p_zRawEphemeris q_E %d ", p_zInjectEph->p_zRawEphemeris->q_E);
						STATUSMSG("p_zRawEphemeris q_IZero %d ", p_zInjectEph->p_zRawEphemeris->q_IZero);
						STATUSMSG("p_zRawEphemeris q_MZero %d ", p_zInjectEph->p_zRawEphemeris->q_MZero);
						STATUSMSG("p_zRawEphemeris q_Omega %d ", p_zInjectEph->p_zRawEphemeris->q_Omega);
						STATUSMSG("p_zRawEphemeris q_OmegaDot %d ", p_zInjectEph->p_zRawEphemeris->q_OmegaDot);
						STATUSMSG("p_zRawEphemeris q_OmegaZero %d ", p_zInjectEph->p_zRawEphemeris->q_OmegaZero);
						STATUSMSG("p_zRawEphemeris q_SqrtA %d ", p_zInjectEph->p_zRawEphemeris->q_SqrtA);
						STATUSMSG("p_zRawEphemeris u_Iode %d ", p_zInjectEph->p_zRawEphemeris->u_Iode);
						STATUSMSG("p_zRawEphemeris u_Prn %d ", p_zInjectEph->p_zRawEphemeris->u_Prn);
			           		STATUSMSG("p_zRawEphemeris w_Cic %d ", p_zInjectEph->p_zRawEphemeris->w_Cic);
						STATUSMSG("p_zRawEphemeris w_Cis %d ", p_zInjectEph->p_zRawEphemeris->w_Cis);
						STATUSMSG("p_zRawEphemeris w_Crc %d ", p_zInjectEph->p_zRawEphemeris->w_Crc);
						STATUSMSG("p_zRawEphemeris w_Crs %d ", p_zInjectEph->p_zRawEphemeris->w_Crs);
						STATUSMSG("p_zRawEphemeris w_Cuc %d ", p_zInjectEph->p_zRawEphemeris->w_Cuc);
						STATUSMSG("p_zRawEphemeris w_Cus %d ", p_zInjectEph->p_zRawEphemeris->w_Cus);
						STATUSMSG("p_zRawEphemeris w_DeltaN %d ", p_zInjectEph->p_zRawEphemeris->w_DeltaN);
						STATUSMSG("p_zRawEphemeris w_IDot %d ", p_zInjectEph->p_zRawEphemeris->w_IDot);
						STATUSMSG("p_zRawEphemeris w_Toe %d ", p_zInjectEph->p_zRawEphemeris->w_Toe);
						STATUSMSG("p_zRawSF1Param  d_FullToe %d", p_zInjectEph->p_zRawSF1Param->d_FullToe);
						STATUSMSG("p_zRawSF1Param q_Af0 %d", p_zInjectEph->p_zRawSF1Param->q_Af0);
						STATUSMSG("p_zRawSF1Param u_Accuracy %d", p_zInjectEph->p_zRawSF1Param->u_Accuracy);
						STATUSMSG("p_zRawSF1Param u_Af2 %d", p_zInjectEph->p_zRawSF1Param->u_Af2);
						STATUSMSG("p_zRawSF1Param u_CodeL2 %d", p_zInjectEph->p_zRawSF1Param->u_CodeL2);
						STATUSMSG("p_zRawSF1Param u_Health %d", p_zInjectEph->p_zRawSF1Param->u_Health);
						STATUSMSG("p_zRawSF1Param u_Tgd %d", p_zInjectEph->p_zRawSF1Param->u_Tgd);
						STATUSMSG("p_zRawSF1Param w_Af1 %d", p_zInjectEph->p_zRawSF1Param->w_Af1);
						STATUSMSG("p_zRawSF1Param w_Iodc %d", p_zInjectEph->p_zRawSF1Param->w_Iodc);
						STATUSMSG("p_zRawSF1Param w_Toc %d", p_zInjectEph->p_zRawSF1Param->w_Toc);
						STATUSMSG("w_EphWeek %d", p_zInjectEph->w_EphWeek);
						#endif
			}

		}
		u_SvCnt++;
		p_zGPSCDatabase->u_EphCounter = u_SvCnt;
	}

	if(u_TxEph)
	{
		if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_ACK_REQ)==FALSE)
		{
			FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
			GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
		}
	}

	if (u_SvCnt >=N_SV)
			return TRUE; /* EPH injection is complete */

	return FALSE;
}

U8 gpsc_mgp_inject_nvs_iono (gpsc_ctrl_type*  p_zGPSCControl)
{
	gpsc_cfg_type* p_zGPSCConfig	 = p_zGPSCControl->p_zGPSCConfig;
	gpsc_db_type* p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_db_iono_type   *p_zDBIono			= &p_zGPSCDatabase-> z_DBIono;
	S32 l_IonoAgeSec = 0;
	U32 q_CurrentAccMs = 0;

	if(p_zGPSCDatabase->z_DBIono.v_iono_valid != TRUE)
	{
		STATUSMSG("gpsc_mgp_inject_nvs_iono: IONO is invalid!");
		return FALSE;
	}

	if(p_zGPSCConfig->ion_validate_check)
	{
		q_CurrentAccMs = gpsc_db_gps_time_in_secs(p_zGPSCControl);
		if(q_CurrentAccMs == FALSE)
		{
			STATUSMSG("gpsc_mgp_inject_nvs_iono: Time not useful");
			return FALSE;
		}

		l_IonoAgeSec = q_CurrentAccMs  - (U32)p_zDBIono->d_UpdateTimeSec;
		STATUSMSG("NVS IONO: TimeS: %d, Cur Time: %d, Age:%d",(U32)p_zDBIono->d_UpdateTimeSec,
			q_CurrentAccMs,l_IonoAgeSec);

		if ( l_IonoAgeSec > 0 && l_IonoAgeSec < C_SECS_PER_WEEK )
		{
			STATUSMSG("Status: Nvs Iono validated & injected" );
			gpsc_mgp_inject_iono(p_zGPSCControl, &p_zDBIono->z_RawIono );
			return TRUE;
		}
		else
		{
			STATUSMSG("Status: NVS Iono too old" );
			return FALSE;

		}
	}
	else
	{
		STATUSMSG("Status: Nvs Iono Injected" );
		gpsc_mgp_inject_iono(p_zGPSCControl, &p_zDBIono->z_RawIono );
		return TRUE;
	}
}


U8 gpsc_mgp_inject_nvs_health(gpsc_ctrl_type*  p_zGPSCControl)
{
	gpsc_cfg_type* p_zGPSCConfig	 = p_zGPSCControl->p_zGPSCConfig;
	gpsc_db_type*           p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_db_health_type*    p_zDBHealth     = &p_zGPSCDatabase->z_DBHealth;
	S32 l_HealthAgeSec = 0;
	U32 q_CurrentAccMs = 0;

	if(p_zGPSCConfig->health_validate_check)
	{
		q_CurrentAccMs = gpsc_db_gps_time_in_secs(p_zGPSCControl);
		if(q_CurrentAccMs == FALSE)
		{
			STATUSMSG("gpsc_mgp_inject_nvs_health: Time not useful");
			return FALSE;
		}

		l_HealthAgeSec = q_CurrentAccMs  - (U32)p_zDBHealth->d_UpdateTimeSec;
		STATUSMSG("NVS Health: UpdateTime: %d, Cur Time: %d, Age:%d",(U32)p_zDBHealth->d_UpdateTimeSec,
			q_CurrentAccMs,l_HealthAgeSec);

		if ( l_HealthAgeSec > 0 && l_HealthAgeSec < (C_GOOD_HEALTH_AGE_SEC) )
		{
			STATUSMSG("Status: Nvs Health validated and injected" );
			gpsc_mgp_inject_sv_health(p_zGPSCControl, &p_zDBHealth->u_AlmHealth[0]);
			return TRUE;
		}
		else
		{
			STATUSMSG("Status: NVS Health too old" );
			return FALSE;
		}
	}
	else
	{
		STATUSMSG("Status: Nvs Health Injected" );
		gpsc_mgp_inject_sv_health(p_zGPSCControl, &p_zDBHealth->u_AlmHealth[0]);
		return TRUE;
	}
}

/*
 ******************************************************************************
 * gpsc_mgp_inj_alm_on_wakeup
 *
 * Function description:
 *
 * Injects ALM for SV17 thru SV32
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *           of many structures accessed from different modules and functions.
 *
 * Return value:
 *  TRUE: if an Almanac was found and Injected
 *  FALSE: if no Almanac was found or Injected
 ******************************************************************************
*/

U8 gpsc_mgp_inj_alm_on_wakeup(gpsc_ctrl_type*  p_zGPSCControl)
{
	gpsc_db_type*     p_zGPSCDatabase   = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_db_alm_type* p_zDBAlmanac	   = &p_zGPSCDatabase->z_DBAlmanac[0];
	U8                u_Ret             = FALSE;
	U8                u_MaxSvsPerMsgCnt = 0; /* To transmit only 4 alm at a time */

	if (p_zGPSCDatabase->u_AlmCounter > N_SV)
		return u_Ret;

   while (p_zGPSCDatabase->u_AlmCounter < N_SV)
	{
		if (p_zDBAlmanac[p_zGPSCDatabase->u_AlmCounter].u_Valid == TRUE)
		{
			if (gpsc_comm_check_bufferspace(p_zGPSCControl, 100) && (u_MaxSvsPerMsgCnt < 4))
			{
				u_Ret = gpsc_mgp_inject_alm( p_zGPSCControl, &p_zDBAlmanac[p_zGPSCDatabase->u_AlmCounter].z_RawAlmanac );
				p_zGPSCDatabase->q_InjAlmList |= (1<<p_zDBAlmanac[p_zGPSCDatabase->u_AlmCounter].z_RawAlmanac.u_Prn);
				u_MaxSvsPerMsgCnt++;

			}
			else
				return(TRUE); /* no more space in the transmit buffer, return and transmit before injecting more Almanac*/
		}

      p_zGPSCDatabase->u_AlmCounter++;
	}

	return u_Ret;
}

/*
 ******************************************************************************
 * gpsc_message_mmgps_req
 *
 * Function description:
 *
 * Send a packet to mmgps requesting information
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *           of many structures accessed from different modules and functions.
 *
 *  u_PacketId: packet ID of the zero-byte packet to be sent
 *
 *  u_SvId: SV ID for the commands that are SV specific, 0 for the ones
 *          not SV specific. Note for RequestSvDirection, u_SvId has
 *          different meaning: 0 directions only for SVs in measurement block,
 *          1: directions for all SVs.
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/

void gpsc_mgp_tx_req
(
  gpsc_ctrl_type*  p_zGPSCControl,
  U8           u_PacketId,
  U8           u_SvId
)
{
  gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;

  z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
  z_PutMsgDesc.u_PacketId = u_PacketId;
  z_PutMsgDesc.p_Buf = &u_SvId;

  z_PutMsgDesc.w_PacketLen = 0;

  if (u_SvId != 0)
  {
    z_PutMsgDesc.w_PacketLen = 1;
  }

  if (u_PacketId == AI_REQ_SV_DIR) //HACk: special case, for SV_DIR, use minimal mode
  {
        u_SvId =1;
		z_PutMsgDesc.w_PacketLen = 1;
  }

  z_PutMsgDesc.u_ForceFlag = 0;
  z_PutMsgDesc.u_MsgSend = FALSE;

  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}

/*
 ******************************************************************************
 * gpsc_mgp_tx_prodlinetest_req
 *
 * Function description:
 *
 * Send a packet to gps to request product line test
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *           of many structures accessed from different modules and functions.
 *
 *
 *
 * Return value:
 *  None
 ******************************************************************************
*/

S8 gpsc_mgp_tx_prodlinetest_req
(
  gpsc_ctrl_type*  p_zGPSCControl

 )
{
  gpsc_productline_test_params*   p_zProductlineTest = p_zGPSCControl->p_zProductlineTest;
  gpsc_state_type*		p_zGPSCState    = p_zGPSCControl->p_zGPSCState;
  gpsc_cfg_type		*p_zGPSCConfig		= p_zGPSCControl->p_zGPSCConfig;

  gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;

  switch(p_zProductlineTest->req_type)
  {

    case AI_CLAPP_REQ_RTC_OSC_TEST:
	{
      z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
      z_PutMsgDesc.u_PacketId = AI_CLAPP_REQ_RTC_OSC_TEST;
      z_PutMsgDesc.p_Buf = &(p_zProductlineTest->svid);
      z_PutMsgDesc.w_PacketLen = 0;
      z_PutMsgDesc.u_ForceFlag = 0;
      z_PutMsgDesc.u_MsgSend = FALSE;

      gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
	}
    break;

   case AI_CLAPP_REQ_RAM_CHKSUM_TEST:
	{
		z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
		z_PutMsgDesc.u_PacketId = AI_CLAPP_REQ_RAM_CHKSUM_TEST;
		z_PutMsgDesc.p_Buf = &(p_zProductlineTest->svid);
		z_PutMsgDesc.w_PacketLen = 0;
		z_PutMsgDesc.u_ForceFlag = 0;
		z_PutMsgDesc.u_MsgSend = FALSE;

		gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
	}
    break;

	case AI_CLAPP_REQ_USERDEF_CW_TEST:
	{
	  U8 u_Body[22],*p_B = &u_Body[0];
	  //*p_B++ = 30; /*timeout*/
	  p_B = NBytesPut(p_zProductlineTest->cw_test_params.test_request, p_B,sizeof(U16));
	  p_B = NBytesPut(p_zProductlineTest->cw_test_params.start_delay, p_B,sizeof(U16));
      p_B = NBytesPut(p_zProductlineTest->cw_test_params.wideband_centfreq, p_B,3);  /* 24 bits*/
	  p_B = NBytesPut(p_zProductlineTest->cw_test_params.narrowband_centfreq, p_B,3);
	  p_B = NBytesPut(p_zProductlineTest->cw_test_params.wideband_peaks ,p_B,sizeof(U8));
	  p_B = NBytesPut(p_zProductlineTest->cw_test_params.wideband_adj_samples ,p_B,sizeof(U8));
	  p_B = NBytesPut(p_zProductlineTest->cw_test_params.narrowband_peaks ,p_B,sizeof(U8));
	  p_B = NBytesPut(p_zProductlineTest->cw_test_params.narrowband_adj_samples ,p_B,sizeof(U8));
	  p_B = NBytesPut(0 ,p_B,8);
	  z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
      z_PutMsgDesc.u_PacketId = AI_CLAPP_REQ_USERDEF_CW_TEST;
      z_PutMsgDesc.p_Buf = &u_Body[0];
      z_PutMsgDesc.w_PacketLen = sizeof(u_Body);
      z_PutMsgDesc.u_ForceFlag = 0;
      z_PutMsgDesc.u_MsgSend = FALSE;

      gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );


	}
    break;

	case AI_CLAPP_REQ_PREDEF_CW_TEST:
	{
	  if((p_zProductlineTest->cw_test_ver == 1) || (p_zProductlineTest->cw_test_ver == 2))
	  	{
	  U8 u_Body[1],*p_B = &u_Body[0];
	  p_B = NBytesPut(p_zProductlineTest->cw_test_ver, p_B,sizeof(U8));
	  z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
      z_PutMsgDesc.u_PacketId = AI_CLAPP_REQ_PREDEF_CW_TEST;
      z_PutMsgDesc.p_Buf = &u_Body[0];
      z_PutMsgDesc.w_PacketLen = sizeof(u_Body);
      z_PutMsgDesc.u_ForceFlag = 0;
      z_PutMsgDesc.u_MsgSend = FALSE;
	  	}
	  else if(p_zProductlineTest->cw_test_ver == 3)
	  	{
		  U8 u_Body[22],*p_B = &u_Body[0];
		  z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
		  z_PutMsgDesc.u_PacketId = AI_CLAPP_REQ_USERDEF_CW_TEST;
		  z_PutMsgDesc.p_Buf = &u_Body[0];
		  z_PutMsgDesc.w_PacketLen = sizeof(u_Body);
		  z_PutMsgDesc.u_ForceFlag = 0;
		  z_PutMsgDesc.u_MsgSend = FALSE;

		  p_zProductlineTest->cw_test_params.test_request = 0x0011;
		  p_zProductlineTest->cw_test_params.test_request |=  ((p_zGPSCConfig->fft_average_number - 1)<< 9); /* 14:10 bits for no. fft avg */
		  p_zProductlineTest->cw_test_params.start_delay = 0x0000;
		  p_zProductlineTest->cw_test_params.wideband_centfreq = 0x00000000;
		  p_zProductlineTest->cw_test_params.narrowband_centfreq = 0x00800000;
		  p_zProductlineTest->cw_test_params.wideband_peaks = 0x05;
		  p_zProductlineTest->cw_test_params.wideband_adj_samples = 0x05;
		  p_zProductlineTest->cw_test_params.narrowband_peaks = 0x05;
		  p_zProductlineTest->cw_test_params.narrowband_adj_samples = 0x19;
		  p_zProductlineTest->cw_test_params.rawiqsample_rawfft_capture = 0x00;
		  /* nf correction factor is already scalled by 10 in the config file */
		  p_zProductlineTest->cw_test_params.nf_correction_factor = (p_zGPSCConfig->nf_correction_factor); /* Signed, scaled by 10, -12.8 to 12.7db, 0 = none */
		  p_zProductlineTest->cw_test_params.input_tone = p_zGPSCConfig->tone_power; /* Input tone power in dBm, scaled by 10, -3276.8 to 3276.7 dBm, 0=none */
		  p_zProductlineTest->cw_test_params.test_mode = 0x01;
		  p_zProductlineTest->cw_test_params.narrowband_decimation = 0x00;
		  p_zProductlineTest->cw_test_params.reserved = 0x0000;
		  p_B = NBytesPut(p_zProductlineTest->cw_test_params.test_request, p_B,sizeof(U16));
		  p_B = NBytesPut(p_zProductlineTest->cw_test_params.start_delay, p_B,sizeof(U16));
		  p_B = NBytesPut(p_zProductlineTest->cw_test_params.wideband_centfreq, p_B,3);  /* 24 bits*/
		  p_B = NBytesPut(p_zProductlineTest->cw_test_params.narrowband_centfreq, p_B,3);
		  p_B = NBytesPut(p_zProductlineTest->cw_test_params.wideband_peaks ,p_B,sizeof(U8));
		  p_B = NBytesPut(p_zProductlineTest->cw_test_params.wideband_adj_samples ,p_B,sizeof(U8));
		  p_B = NBytesPut(p_zProductlineTest->cw_test_params.narrowband_peaks ,p_B,sizeof(U8));
		  p_B = NBytesPut(p_zProductlineTest->cw_test_params.narrowband_adj_samples ,p_B,sizeof(U8));
		  p_B = NBytesPut(p_zProductlineTest->cw_test_params.rawiqsample_rawfft_capture ,p_B,sizeof(U8));
		  p_B = NBytesPut(p_zProductlineTest->cw_test_params.nf_correction_factor ,p_B,sizeof(U8));
		  p_B = NBytesPut(p_zProductlineTest->cw_test_params.input_tone ,p_B,sizeof(U16));
		  p_B = NBytesPut(p_zProductlineTest->cw_test_params.test_mode ,p_B,sizeof(U8));
		  p_B = NBytesPut(p_zProductlineTest->cw_test_params.narrowband_decimation ,p_B,sizeof(U8));
		  p_B = NBytesPut(p_zProductlineTest->cw_test_params.reserved ,p_B,sizeof(U16));
		  z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
		  z_PutMsgDesc.u_PacketId = AI_CLAPP_REQ_USERDEF_CW_TEST;
		  z_PutMsgDesc.p_Buf = &u_Body[0];
		  z_PutMsgDesc.w_PacketLen = sizeof(u_Body);
		  z_PutMsgDesc.u_ForceFlag = 0;
		  z_PutMsgDesc.u_MsgSend = FALSE;
      gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
	  	}
	}
    break;

	case AI_CLAPP_REQ_ACQ_TEST:
	{
	  U8 u_Body[2],*p_B = &u_Body[0];
	  p_B = NBytesPut(p_zProductlineTest->timeout/1000, p_B,sizeof(U8));
	  p_B = NBytesPut(p_zProductlineTest->svid ,p_B,sizeof(U8));
	  z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
      z_PutMsgDesc.u_PacketId = AI_CLAPP_REQ_ACQ_TEST;
      z_PutMsgDesc.p_Buf = &u_Body[0];
      z_PutMsgDesc.w_PacketLen = sizeof(u_Body);
      z_PutMsgDesc.u_ForceFlag = 0;
      z_PutMsgDesc.u_MsgSend = FALSE;

      gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
	}
	break;

	case AI_CLAPP_REQ_GPS_OSC_TEST :
	{
		/* if the GPS is In config 4 inject the oscillator frequency*/
      if(p_zGPSCState->u_SensorClockConfig == C_GPSC_SENSOR_CONFIG4)
	  {
		 gpsc_mgp_inject_OscParams(p_zGPSCControl);
	     gpsc_mgp_inject_calibrationcontrol(p_zGPSCControl,GPSC_ENABLE_PERIODIC_CALIB_REF_CLK);
		 p_zGPSCState->u_CalibRequestPending = FALSE;
	  }
	  z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
      z_PutMsgDesc.u_PacketId = AI_CLAPP_REQ_GPS_OSC_TEST;
      z_PutMsgDesc.p_Buf = &(p_zProductlineTest->svid);
      z_PutMsgDesc.w_PacketLen = 0;
      z_PutMsgDesc.u_ForceFlag = 0;
      z_PutMsgDesc.u_MsgSend = FALSE;

      gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
	}
    break;


	case AI_CLAPP_REQ_SYNCTEST:
	{
	  U8 u_Body[10],*p_B = &u_Body[0];
	  /* sv id */
	  p_B = NBytesPut(p_zProductlineTest->svid, p_B,sizeof(U8));
	  /* termination event */
	  p_B = NBytesPut(p_zProductlineTest->termination_event, p_B,sizeof(U8));
	  /* Enable Diagnostics */
	  p_B = NBytesPut(0 ,p_B,sizeof(U8));

	  /* 7 bytes reserved */
	  p_B = NBytesPut(0 ,p_B,sizeof(U32)); /* 4 bytes */
	  p_B = NBytesPut(0 ,p_B,sizeof(U16)); /* 2 bytes */
	  p_B = NBytesPut(0 ,p_B,sizeof(U8)); /* 1 bytes */

	  z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
      z_PutMsgDesc.u_PacketId = AI_CLAPP_REQ_SYNCTEST;
      z_PutMsgDesc.p_Buf = &u_Body[0];
      z_PutMsgDesc.w_PacketLen = sizeof(u_Body);
      z_PutMsgDesc.u_ForceFlag = 0;
      z_PutMsgDesc.u_MsgSend = FALSE;

      gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
	}
    break;

	case AI_CLAPP_REQ_SELF_TEST:
	{
	    gpsc_mgp_tx_req(p_zGPSCControl,AI_CLAPP_REQ_SELF_TEST,0);
	}
    break;



	case AI_CLAPP_INJ_CUSTOM_PACKET:
	{
    U8 u_Body[1],*p_B = &u_Body[0];		/* for resetting */
	U8 u_Body1[5],*p_B1 = &u_Body1[0]; /* for injecting */
	U8 u_Body2[3],*p_B2 = &u_Body2[0]; /* for requesting */

	/* Resetting GPIO control */
    p_B = NBytesPut(AI_CLAPP_CUSTOM_SUB_PKT_RESET_GPIO_CTRL, p_B,sizeof(U8));
    z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
    z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_CUSTOM_PACKET;
    z_PutMsgDesc.p_Buf = &u_Body[0];
    z_PutMsgDesc.w_PacketLen = sizeof(u_Body);
    z_PutMsgDesc.u_ForceFlag = 0;
    z_PutMsgDesc.u_MsgSend = FALSE;

    gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );

    if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
	{
		//   return GPSC_TX_INITIATE_FAIL;
		FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
		GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
	}
    STATUSMSG("GPIO Production Line Test: Resetting GPIO control, Packet ID: %d", AI_CLAPP_CUSTOM_SUB_PKT_RESET_GPIO_CTRL);

	/* injecting GPIO control */
   	p_B1 = NBytesPut(AI_CLAPP_CUSTOM_SUB_PKT_INJ_GPIO_CTRL, p_B1,sizeof(U8));
	p_B1 = NBytesPut(p_zProductlineTest->gpio_test_params.write_value, p_B1,sizeof(U16));
	p_B1 = NBytesPut(p_zProductlineTest->gpio_test_params.write_mask, p_B1,sizeof(U16));
    z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
    z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_CUSTOM_PACKET;
    z_PutMsgDesc.p_Buf = &u_Body1[0];
    z_PutMsgDesc.w_PacketLen = sizeof(u_Body1);
    z_PutMsgDesc.u_ForceFlag = 0;
    z_PutMsgDesc.u_MsgSend = FALSE;

    gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );

    if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
	{
	//   return GPSC_TX_INITIATE_FAIL;
	FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
	GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
	}
    STATUSMSG("GPIO PLT: Inject, WriteValue:0x%X, WriteMask:0x%X", p_zProductlineTest->gpio_test_params.write_value,p_zProductlineTest->gpio_test_params.write_mask);

    /* requesting GPIO control */
	p_B2 = NBytesPut(AI_CLAPP_CUSTOM_SUB_PKT_REQ_GPIO_STATUS, p_B2,sizeof(U8));
    p_B2 = NBytesPut(p_zProductlineTest->gpio_test_params.status_mask, p_B2,sizeof(U16));
	z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
    z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_CUSTOM_PACKET;
    z_PutMsgDesc.p_Buf = &u_Body2[0];
    z_PutMsgDesc.w_PacketLen = sizeof(u_Body2);
    z_PutMsgDesc.u_ForceFlag = 0;
    z_PutMsgDesc.u_MsgSend = FALSE;

    gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
    if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
	  {
		//   return GPSC_TX_INITIATE_FAIL;
		FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
		GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
	}

    STATUSMSG("GPIO PLT: Requested, Mask: 0x%0X", p_zProductlineTest->gpio_test_params.status_mask);

  }


    break;

	default:
		ERRORMSG("Error : no such request");
  }
  return GPSC_SUCCESS;
}


void gpsc_mgp_send_patch_dwld_ctrl(
  gpsc_ctrl_type*  p_zGPSCControl,
  U8           u_DwldMode
)
{
  gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;

  z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
  z_PutMsgDesc.u_PacketId = AI_CLAPP_PATCH_DWLD_CONTROL;
  z_PutMsgDesc.p_Buf = &u_DwldMode;

  z_PutMsgDesc.w_PacketLen = 1;

  z_PutMsgDesc.u_ForceFlag = 0;
  z_PutMsgDesc.u_MsgSend = FALSE;

  AI2TX("Patch Download Control",AI_CLAPP_PATCH_DWLD_CONTROL);
#ifdef GPSC_DEBUG
  printf("\nStatus : Sent Patch Download Control Msg\n");
  q_DownloadCurrTick = mcpf_getCurrentTime_InMilliSec(p_zGPSCControl->p_zSysHandlers->hMcpf);
#endif
  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}



U8 gpsc_process_patch_download( gpsc_ctrl_type*  p_zGPSCControl)
{
   gpsc_state_type* p_zGPSCState = p_zGPSCControl->p_zGPSCState;
   gpsc_patch_status_type*	p_zPatchStatus    =  &p_zGPSCState->z_PatchStatus;
   gpsc_sys_handlers*	p_GPSCSysHandlers = p_zGPSCControl->p_zSysHandlers;
   U8  u_Header[C_PATCH_HEADER_SIZE];
   U8  u_FullFileName[MCP_HAL_CONFIG_FS_MAX_PATH_LEN_CHARS];
   p_zPatchStatus->u_FastDownload =FALSE;


   if( (p_zGPSCControl->p_zGPSCConfig->patch_available != GPSC_FALSE) && (p_zGPSCState->u_ValidPatch==0 ))
   {
	   McpU32 l_PatchOpenResult;
	   McpU32 l_PatchReadResult;
	   EMcpfRes	eRetVal;

	   p_zPatchStatus->w_RecordLength =128;

	MCP_HAL_STRING_StrCpy (u_FullFileName,(McpU8*)p_GPSCSysHandlers->uPatchFile);

	   eRetVal = mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *)u_FullFileName,
	   				 MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_RDONLY, &p_zPatchStatus->pPatchFile);
	   if(eRetVal != RES_OK)
	   {
		   ERRORMSG2("Error: Cannot open patch file-%s",u_FullFileName);
		   FATAL_ERROR (GPSC_PATCH_FILE_OPEN_FAIL);
		   return FALSE;
	   }

	   l_PatchOpenResult = mcpf_getFileSize(p_zGPSCControl->p_zSysHandlers->hMcpf,
	   									p_zPatchStatus->pPatchFile);

	   if(l_PatchOpenResult == 0 )
	   {
		   ERRORMSG2("Bad size of Patch file %d bytes",l_PatchOpenResult);
		   FATAL_ERROR(GPSC_PATCH_FILE_OPEN_FAIL);
		   return FALSE;
	   }

	   p_zPatchStatus->u_PatchFileOpen=TRUE;

	   l_PatchReadResult = mcpf_file_read(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zPatchStatus->pPatchFile,
	   									(void*)&u_Header[0], C_PATCH_HEADER_SIZE);
	   if(l_PatchReadResult != C_PATCH_HEADER_SIZE)
	   {
	   	   mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zPatchStatus->pPatchFile);
		   ERRORMSG2("Error reading magic number of Patch file [%d] bytes read", l_PatchReadResult);
		   FATAL_ERROR(GPSC_PATCH_FILE_SIZE_FAIL);
		   return FALSE;
	   }

	   if((u_Header[0] == 0x07) && (u_Header[1] == 0xAB) && (u_Header[2] == 0xCD))
	   {
		   p_zPatchStatus->u_FastDownload =TRUE;
		   /* reading no of records*/
		   p_zPatchStatus->w_TotalRecords = (U16)u_Header[3];
		   p_zPatchStatus->w_TotalRecords =  (U16)((p_zPatchStatus->w_TotalRecords<<8) + (U16)u_Header[4]);
		   STATUSMSG(" NO of Records to download = %d ",p_zPatchStatus->w_TotalRecords);

		   /* reading user defined record length  */
		   p_zPatchStatus->w_RecordLength = (U16)u_Header[5];
		   p_zPatchStatus->w_RecordLength = (U16)((p_zPatchStatus->w_RecordLength<<8) + (U16)u_Header[6]) ;
		   STATUSMSG(" user defined record length = %d ",p_zPatchStatus->w_RecordLength);
		   /* reading last record length */
		   p_zPatchStatus->w_LastRecordLength = (U16)u_Header[7];
		   p_zPatchStatus->w_LastRecordLength = (U16)((p_zPatchStatus->w_LastRecordLength<<8) + (U16)u_Header[8]);
		   STATUSMSG(" last record length = %d ",p_zPatchStatus->w_LastRecordLength);
	   }
	   else /* for lagacy download */
	   {
		   if( (l_PatchOpenResult % p_zPatchStatus->w_RecordLength) != 0)
		   {
			   ERRORMSG2("Bad size of Patch file %d bytes",l_PatchOpenResult);
			   FATAL_ERROR(GPSC_PATCH_FILE_SIZE_FAIL);
			   return FALSE;

		   //  FATAL_INIT(GPSC_PATCH_FILE_SIZE_FAIL);
		   }

		   p_zPatchStatus->q_TotalBytes=(U32)l_PatchOpenResult;
		   p_zPatchStatus->w_TotalRecords = (U16)(l_PatchOpenResult/p_zPatchStatus->w_RecordLength);
		   /* If total num of bytes is not multiple of patch record byte send the remaing bytes
		   at the end */
	   }

	   p_zPatchStatus->w_NextRecord = 0;
	   p_zPatchStatus->q_BytesSent= 0;

	   	   return TRUE;
   }
   else
   {
   	return FALSE;
   }
}


void gpsc_read_next_dwld_record(gpsc_ctrl_type*  p_zGPSCControl)
{
   McpU32 l_PatchReadResult;
   gpsc_state_type* p_zGPSCState = p_zGPSCControl->p_zGPSCState;
   gpsc_patch_status_type*	p_zPatchStatus    =  &p_zGPSCState->z_PatchStatus;
   U32 q_numBytesDownload=0;

   /*If more records are left, send next record*/
   if(p_zPatchStatus->w_NextRecord < p_zPatchStatus->w_TotalRecords )
   {

	   if( p_zPatchStatus->u_FastDownload == TRUE )
	   {
		   if((p_zPatchStatus->w_NextRecord ==0) && (p_zGPSCControl->p_zGPSCConfig->patch_available != GPSC_STACKRAM_PATCH))
		   q_numBytesDownload = 128 ;  /* for the first special record in case of fast download*/
		   else if(p_zPatchStatus->w_NextRecord == p_zPatchStatus->w_TotalRecords-1)
		   q_numBytesDownload = p_zPatchStatus->w_LastRecordLength; /* for the last record */
		   else
		   q_numBytesDownload = p_zPatchStatus->w_RecordLength;
	   }

	   else
	   	q_numBytesDownload = p_zPatchStatus->w_RecordLength;  /* for the legacy case  128*/

	   l_PatchReadResult = mcpf_file_read(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zPatchStatus->pPatchFile,
	   								(void*)&p_zPatchStatus->u_RecordBuffer[2], (McpU16)q_numBytesDownload);

	   if(l_PatchReadResult == q_numBytesDownload)
	   {
	   	p_zPatchStatus->u_RecordBuffer[0] = (U8)((p_zPatchStatus->w_NextRecord>>8) & 0xFF);
		p_zPatchStatus->u_RecordBuffer[1] = (U8)(p_zPatchStatus->w_NextRecord & 0xFF);

				/* inject record to GPS receiver */
				gpsc_tx_download_record(p_zGPSCControl);
			    if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
				{
					//   return GPSC_TX_INITIATE_FAIL;
					FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
					GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
				}
				return;

	   }
	   else
	   {
	   	mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zPatchStatus->pPatchFile);
		ERRORMSG("Error: Patch read failed");
		FATAL_ERROR(GPSC_PATCH_FILE_READ_FAIL);
		p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_READY_SKIP_DOWNLOAD;
		gpsc_ready_sequence(p_zGPSCControl, E_GPSC_SEQ_READY_SKIP_DOWNLOAD);
//		FATAL_INIT(GPSC_PATCH_FILE_READ_FAIL);
	   }
   }
   else
   {
		/*If code reached here then  no more records to send. Close patch file ,
		 Send End of Download and await download result from GPS*/
	    mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zPatchStatus->pPatchFile);
	    p_zPatchStatus->u_PatchFileOpen = FALSE;
        p_zPatchStatus->pPatchFile = 0;

		/* at end of record, if baud rate is not 115200 (default), make it default before download complete Ai2 response comes */
		if(p_zGPSCControl->p_zGPSCConfig->comm_config.comm_config_union.comm_config_uart.uart_baud_rate != GPSC_BAUD_115200)
		{
		   /* set to default baud rate - 115200 */
		   p_zGPSCControl->p_zGPSCConfig->comm_config.comm_config_union.comm_config_uart.uart_baud_rate = GPSC_BAUD_115200;

		   /* This baud rate change request for E_GPSC_SEQ_READY_READ_INJECT_RECORD, no timer */
		   gpsc_mgp_tx_inject_baud_rate (p_zGPSCControl);
		   if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
		   {
			   FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
			   GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
			   //FATAL_INIT(GPSC_TX_INITIATE_FAIL);
		   }
		}
		else
		{
		   /* send end download message to GPS receiver */
		   gpsc_send_end_of_download(p_zGPSCControl);
		   if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
		     {
				//   return GPSC_TX_INITIATE_FAIL;
				FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
				GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
			}
           /*Wait for download result from GPS, stop timer when AI_CLAPP_REP_DWNLD_COMPLETE msg received */
		   gpsc_timer_start(p_zGPSCControl,C_TIMER_SEQUENCE,p_zGPSCControl->p_zGPSCConfig->ai2_comm_timeout);
		}
   }
}


void gpsc_tx_download_record(gpsc_ctrl_type* p_zGPSCControl)
{
	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;
	gpsc_state_type* p_zGPSCState = p_zGPSCControl->p_zGPSCState;
    gpsc_patch_status_type*	p_zPatchStatus    =  &p_zGPSCState->z_PatchStatus;
    U32	q_numBytesDownload=0;
	STATUSMSG("Status: Sending record Number %d",p_zPatchStatus->w_NextRecord);
#ifdef GPSC_DEBUG
	printf("\n Status: Sending record Number %d",p_zPatchStatus->w_NextRecord);
#endif

	z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
	z_PutMsgDesc.u_PacketId = AI_CLAPP_SET_DOWNLOAD_RECORD;
	z_PutMsgDesc.p_Buf = &p_zPatchStatus->u_RecordBuffer[0];

	if( p_zPatchStatus->u_FastDownload == TRUE )
	{
		   if((p_zPatchStatus->w_NextRecord ==0) && (p_zGPSCControl->p_zGPSCConfig->patch_available != GPSC_STACKRAM_PATCH))
		   q_numBytesDownload = 128 ;  /* for the first special record in case of fast download*/
		   else if(p_zPatchStatus->w_NextRecord == p_zPatchStatus->w_TotalRecords-1)
		   q_numBytesDownload = p_zPatchStatus->w_LastRecordLength; /* for the last record */
		   else
		   q_numBytesDownload = p_zPatchStatus->w_RecordLength;
	}

	else
		q_numBytesDownload = p_zPatchStatus->w_RecordLength;   /* legacy case 128*/

	STATUSMSG("Status: Packet Length +2 = %d",q_numBytesDownload);

	z_PutMsgDesc.w_PacketLen = (U16) (q_numBytesDownload +2);//sizeof(p_zPatchStatus->u_RecordBuffer);
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;
    AI2TX("Download Record",AI_CLAPP_SET_DOWNLOAD_RECORD);
	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}

void gpsc_send_end_of_download(gpsc_ctrl_type* p_zGPSCControl)
{
	U8 u_EODBytes[3] = "\xFF\xFF\xFF";
	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;

	z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
	z_PutMsgDesc.u_PacketId = AI_CLAPP_SET_DOWNLOAD_RECORD;
	z_PutMsgDesc.p_Buf = &u_EODBytes[0];
	z_PutMsgDesc.w_PacketLen = sizeof(u_EODBytes);
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;

	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}


void gpsc_mgp_tx_nmea_ctrl(gpsc_ctrl_type* p_zGPSCControl, U16 w_NMEABitmap)
{
	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;
	U16 w_Body[2];
	if ((w_NMEABitmap & ~NMEA_MASK) != 0)
	{
		ALOGE("+++ %s: Invalid NMEA Mask [0x%x] +++\n", __FUNCTION__, w_NMEABitmap);
		return;
	}

	w_Body[0] = w_NMEABitmap;
	w_Body[1] = 0;
	z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
	z_PutMsgDesc.u_PacketId = AI_CLAPP_NMEA_OUTPUT_CONTROL;
	z_PutMsgDesc.p_Buf = (U8*) &w_Body[0];
	z_PutMsgDesc.w_PacketLen = sizeof(w_Body);
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;

	AI2TX("NMEA control", AI_CLAPP_NMEA_OUTPUT_CONTROL);
	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc);
}

void gpsc_mgp_tx_i2c_config(gpsc_ctrl_type* p_zGPSCControl)
{
	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;
	gpsc_cfg_type* p_zGPSCConfig	= p_zGPSCControl->p_zGPSCConfig;
	T_GPSC_comm_config_i2c* p_comm_config_i2c =
		&p_zGPSCConfig->comm_config.comm_config_union.comm_config_i2c;
	U8 u_Body[11],*p_B = &u_Body[0];

	*p_B++ = 0x14; /*GPS Identifier*/
	p_B = NBytesPut(p_comm_config_i2c->i2c_ce_address_mode ,p_B,sizeof(U8));
	p_B = NBytesPut(p_comm_config_i2c->i2c_ce_address,p_B,sizeof(U16));
	p_B = NBytesPut(p_comm_config_i2c->i2c_data_rate,p_B,sizeof(U8));
	p_B = NBytesPut(p_comm_config_i2c->i2c_logicalid,p_B,sizeof(U8));

  /* This needs to updated as per the inputs provided in the config file.
     Changes needs to be done after SAP update and config file update. */
  p_B = NBytesPut(0 ,p_B,sizeof(U8));/*GPS Address control*/

	p_B = NBytesPut(p_comm_config_i2c->i2c_gps_address_mode,p_B,sizeof(U8));
	p_B = NBytesPut(p_comm_config_i2c->i2c_gps_address,p_B,sizeof(U16));

    /* This needs to updated as per the inputs provided in the config file.
     Changes needs to be done after SAP update and config file update. */
  p_B = NBytesPut(C_GPSC_MAX_MSG_LEN ,p_B,sizeof(U8));


	z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
	z_PutMsgDesc.u_PacketId = AI_CLAPP_SET_I2C_CONFIG;
	z_PutMsgDesc.p_Buf = &u_Body[0];
	z_PutMsgDesc.w_PacketLen = sizeof(u_Body);
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;
    AI2TX("I2C Config",AI_CLAPP_SET_I2C_CONFIG);
	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}



void gpsc_mgp_tx_customer_config(gpsc_ctrl_type* p_zGPSCControl)
{
	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;
	gpsc_cfg_type* p_zGPSCConfig	= p_zGPSCControl->p_zGPSCConfig;
	gpsc_state_type* p_zGPSCState = p_zGPSCControl->p_zGPSCState;
	U8 u_Body[C_AI2_CUSTOMER_CONFIG_BODY],*p_B = &u_Body[0];

	//CE Osc params
	*p_B++ = 0;

	p_B = NBytesPut(p_zGPSCConfig->ref_clock_quality , p_B, sizeof( U16 ) );

	/* Bytes 3,4,5,6 are Oscillator Frequency */
	p_B = NBytesPut( p_zGPSCConfig->ref_clock_frequency, p_B, sizeof (U32) );

	//timestamp_edge
	p_B = NBytesPut( p_zGPSCConfig->timestamp_edge, p_B, sizeof (U8) );

	//PA Blank control
	p_B = NBytesPut( p_zGPSCConfig->pa_blank_enable, p_B, sizeof (U8) );
	p_B = NBytesPut( p_zGPSCConfig->pa_blank_polarity, p_B, sizeof (U8) );

	//max_clock_acceleration
	p_B = NBytesPut(p_zGPSCConfig->max_clock_acceleration, p_B, sizeof (U16) );

	//C/NO front end loss
	p_B = NBytesPut( p_zGPSCConfig->front_end_loss, p_B, sizeof (U8) );

	//WatchDog control
	p_B = NBytesPut( GPSC_FALSE, p_B, sizeof (U8) );

	//AutoSleep control
	p_B = NBytesPut( GPSC_FALSE, p_B, sizeof (U8) );

	//UART2 Baudrate
	p_B = NBytesPut( C_BAUD_19200, p_B, sizeof (U8) );

	//PRM adjustment
	p_B = NBytesPut( 0, p_B, sizeof (U32) );

	//UTC leap seconds
	p_B = NBytesPut( p_zGPSCConfig->utc_leap_seconds, p_B, sizeof (U8) );

	//Calibration control -modification for config 4
	if((p_zGPSCState->u_SensorClockConfig == (C_GPSC_SENSOR_CONFIG4)) ||
       (p_zGPSCState->u_SensorClockConfig == (C_GPSC_SENSOR_CONFIG3))
    )
	{
		p_B = NBytesPut(GPSC_DISABLE_CALIBRATION, p_B, sizeof (U8) ); //disable the calibration for config4,3,2
	}
	else
	p_B = NBytesPut( GPSC_TRUE, p_B, sizeof (U8) );  //enable the periodic calibration for other config

	//PPS control
	p_B = NBytesPut( GPSC_FALSE, p_B, sizeof (U8) );
	p_B = NBytesPut( 0, p_B, sizeof (U8) ); // ignored

	z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
	z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_CUSTOMER_CONFIG;
	z_PutMsgDesc.p_Buf = &u_Body[0];
	z_PutMsgDesc.w_PacketLen = (U16)(p_B - &u_Body[0]);

	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;
    AI2TX("Customer Config",AI_CLAPP_INJ_CUSTOMER_CONFIG);
	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}

void gpsc_mgp_tx_custom_packet(gpsc_ctrl_type* p_zGPSCControl,U8 *p_Body, U8 u_length)
{
	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;

	if(p_zGPSCControl !=NULL) /* Klocwork Changes */
	{
	z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
	z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_CUSTOM_PACKET;
	z_PutMsgDesc.p_Buf = p_Body;
	z_PutMsgDesc.w_PacketLen = u_length;
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;
    AI2TX("Custom Packet",AI_CLAPP_INJ_CUSTOM_PACKET);
	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}

}

/*
 ******************************************************************************
 * gpsc_mgp_tx_inject_timeout_cep_info
 *
 * Function description:
 *
 *  Form and send timeout and CEP info command to Sensor.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *
 * Return value:
 *  None
 * Note: added support to inject timeout info and CEP info to sensor
 ******************************************************************************
 */
void gpsc_mgp_tx_inject_timeout_cep_info(gpsc_ctrl_type* p_zGPSCControl, gpsc_loc_fix_qop *p_zQop)
{
    U8 u_Body[C_AI2_TIMEOUT_BODY];
	U16 timeout1,timeout2,accuracy1,accuracy2;
	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;

	gpsc_db_gps_clk_type*  	p_DBGpsClock = &p_zGPSCControl->p_zGPSCDatabase->z_DBGpsClock;
	gpsc_cfg_type*	p_zGPSCConfig	= p_zGPSCControl->p_zGPSCConfig;
	gpsc_state_type* p_zGPSCState   = p_zGPSCControl->p_zGPSCState;
	U8 *p_B = &u_Body[0];

	/* Time delay when RC_ON from first set of assistance to actual location request for MSB and MSA case
	Delay = 0 while injecting timeout and cep info when first set of assistance comes*/
	S32 u_Delay; /* delay in seconds */

        if (p_zGPSCState->q_FCountRecOnFirstAssistToLocReq==0 ||
        p_DBGpsClock->q_FCount == GPSC_INVALID_FCOUNT)
        {
            u_Delay = 0;
        }
        else
        {
	    u_Delay = (S32) (p_DBGpsClock->q_FCount - p_zGPSCState->q_FCountRecOnFirstAssistToLocReq);
            u_Delay = u_Delay / 500;
	    if(u_Delay < 0)
	         u_Delay = 0;
            p_zGPSCState->u_FcountDelta = u_Delay;
            p_zGPSCState->u_FcountDelta /= 2;
	}

	if((p_zQop->u_HorizontalAccuracy == C_ACCURACY_UNKNOWN ) ||
		(p_zQop->u_HorizontalAccuracy == 0 ))
	{
		/* pick the config file values*/
       STATUSMSG(" ===>>> Acc: Picking from Config file");
	   accuracy1 = (U16)p_zGPSCConfig->accuracy1;
	   accuracy2 = (U16)p_zGPSCConfig->accuracy2;
	}
	else
	   {
		/* pick the injected values */
       STATUSMSG(" ===>>> Acc: Picking the injected value");
		accuracy1 = (U16)(p_zQop->u_HorizontalAccuracy);
        accuracy2 = (U16)((p_zQop->u_HorizontalAccuracy *2) - C_ACC_VARIATION) ;
        if( accuracy2 <= C_ACC_VARIATION)
        {
	      accuracy2 = (U16)(p_zQop->u_HorizontalAccuracy *2);
		if(accuracy2 > (C_ACC_VARIATION*2))
		{
	      accuracy2 -= C_ACC_VARIATION;
		}
        }
	}


	if((p_zQop->w_MaxResponseTime == C_TIMEOUT_UNKNOWN) ||
		(p_zQop->w_MaxResponseTime == 0))
	{
		/* pick the values from the config file*/
       STATUSMSG(" ===>>> RespTime: Picking from Config file");
		timeout1 = (U16)((p_zGPSCConfig->timeout1 * 2) + u_Delay); /* resolution 0.5 sec/bit */
		timeout2 = (U16)((p_zGPSCConfig->timeout2 * 2) + u_Delay); /* resolution 0.5 sec/bit */
         }
	else
	{
       STATUSMSG(" ===>>> RespTime: Picking the injected value");
		/* pick the injected values*/
		/* timeout 1 needs to be MaxResponseTime/2*/
		timeout1 = (U16)((p_zQop->w_MaxResponseTime) + u_Delay); /* resolution 0.5 sec/bit */
		/*timeout2 needs to be equal to MaxResponseTime*/
		timeout2 = (U16)((p_zQop->w_MaxResponseTime * 2) + u_Delay); /* resolution 0.5 sec/bit */
	}



	STATUSMSG("Status:SinglePE Injection: Delay:%ds, Accu1:%d,Accu2:%d,timeout1:%ds,timeout2:%ds",
		       									u_Delay/2,accuracy1,accuracy2,timeout1/2,timeout2/2);

	/* Single PE sub packet id */
	p_B = NBytesPut(AI_CLAPP_CUSTOM_SUB_PKT_INJ_TIMEOUT_CEP_INFO, p_B, sizeof(U8) ); /* 1 byte */

	/* constructing single PE parameters -  accuracy and timeout */
	p_B = NBytesPut(0, p_B, sizeof(U8) ); /* first 1 byte reserved */
	p_B = NBytesPut(timeout1, p_B, sizeof(U16) ); /* 2 byte */
	p_B = NBytesPut(accuracy1, p_B, sizeof(U16) ); /* 2 byte */
	p_B = NBytesPut(timeout2, p_B, sizeof(U16) ); /* 2 byte */
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

}


/*
 ******************************************************************************
 * gpsc_mgp_tx_customer_config_extended
 *
 * Function description:
 *
 *  Form and send extended customer configuration (only for GPS5350) command
 *  to Sensor.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *
 * Return value:
 *  None
 *
 ******************************************************************************
 */

void gpsc_mgp_tx_customer_config_extended(gpsc_ctrl_type* p_zGPSCControl)
{
	gpsc_ctrl_type*	p_GPSCControl;
    U8 u_Body[C_AI2_CUSTOMER_CONFIG_EXTENDED_BODY];

	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;
	gpsc_cfg_type *p_GPSCConfig,* p_zGPSCConfig	= p_zGPSCControl->p_zGPSCConfig;
	gpsc_dyna_feature_cfg_type *p_zGPSCDynaFeatureConfig = p_zGPSCControl->p_zGPSCDynaFeatureConfig;
	gpsc_state_type* p_zGPSCState = p_zGPSCControl->p_zGPSCState;
	U8 *p_B = &u_Body[0];

	p_GPSCControl = gp_zGPSCControl;

	p_GPSCConfig = p_GPSCControl->p_zGPSCConfig;

	/* CE Osc params */
	/* Osc control (1 byte) */
	*p_B++ = 0;
	/* Osc Quality (2 byte) */
	p_B = NBytesPut(p_zGPSCConfig->ref_clock_quality , p_B, sizeof( U16 ) );
	p_GPSCConfig->ref_clock_quality = p_zGPSCConfig->ref_clock_quality;

	/* Bytes 3,4,5,6 are Oscillator Frequency (4 byte) */
	p_B = NBytesPut( p_zGPSCConfig->ref_clock_frequency, p_B, sizeof (U32) );
	p_GPSCConfig->ref_clock_frequency = p_zGPSCConfig->ref_clock_frequency;

	/* timestamp_edge (1 Byte) */
	p_B = NBytesPut( p_zGPSCConfig->timestamp_edge, p_B, sizeof (U8) );
	p_GPSCConfig->timestamp_edge = p_zGPSCConfig->timestamp_edge;

	/* PA Blank control (2 bytes) */
	p_B = NBytesPut( p_zGPSCConfig->pa_blank_enable, p_B, sizeof (U8) );
	p_GPSCConfig->pa_blank_enable = p_zGPSCConfig->pa_blank_enable;
	p_B = NBytesPut( p_zGPSCConfig->pa_blank_polarity, p_B, sizeof (U8) );
	p_GPSCConfig->pa_blank_polarity = p_zGPSCConfig->pa_blank_polarity;

	/* max_clock_acceleration (2 bytes) */
	p_B = NBytesPut(p_zGPSCConfig->max_clock_acceleration, p_B, sizeof (U16) );
	p_GPSCConfig->max_clock_acceleration = p_zGPSCConfig->max_clock_acceleration;

	/* C/NO front end loss (1 bytes) */
	p_B = NBytesPut( p_zGPSCConfig->front_end_loss, p_B, sizeof (U8) );
	p_GPSCConfig->front_end_loss = p_zGPSCConfig->front_end_loss;

	/* WatchDog control (1 bytes) */
	p_B = NBytesPut( GPSC_FALSE, p_B, sizeof (U8) );

	/* AutoSleep control (1 byte) */
	/* This needs to be changed as per config file change.Present value
	   includes default values:
		Auto sleep disabled (Default) = 0
		Wakeup on communication = 00
	*/

	p_B = NBytesPut( p_zGPSCConfig->gps_sleep_ctrl, p_B, sizeof (U8) );
	p_GPSCConfig->gps_sleep_ctrl = p_zGPSCConfig->gps_sleep_ctrl;



	/* UART2 Baudrate (1 byte) */
	p_B = NBytesPut( C_BAUD_19200, p_B, sizeof (U8) );

	/* PRM adjustment (4 bytes) */
	p_B = NBytesPut( p_zGPSCConfig->prm_adjustment, p_B, sizeof (U32) ); /* Signed, (1/4294967296) per bit, Range: -0.5 to 0.5 seconds */

	/* UTC leap seconds (1 byte) */
	p_B = NBytesPut( p_zGPSCConfig->utc_leap_seconds, p_B, sizeof (U8) );



	/* Disable or enable Calibration control based on clock config */
	if((p_zGPSCState->u_SensorClockConfig == C_GPSC_SENSOR_CONFIG4 ) ||
       (p_zGPSCState->u_SensorClockConfig == C_GPSC_SENSOR_CONFIG3 ))
		{
		   p_B = NBytesPut(GPSC_DISABLE_CALIBRATION, p_B, sizeof (U8) );
		}
	else
		{
		   p_B = NBytesPut( GPSC_TRUE, p_B, sizeof (U8) );
		}

	/* PPS control (2 bytes) */
	p_B = NBytesPut( p_zGPSCConfig->pps_output, p_B, sizeof (U8) ); /* 0: disable, 1:enable */
	p_B = NBytesPut( p_zGPSCConfig->pps_polarity, p_B, sizeof (U8) ); /* 0:Rising edge, 1:Falling Edge */

	/* Advance Power Management (7 bytes): Disabled at startup */
	p_B = NBytesPut( 0, p_B, sizeof (U8) );
	p_B = NBytesPut( 0, p_B, sizeof (U8) );
	p_B = NBytesPut( 0, p_B, sizeof (U8) );
	p_B = NBytesPut( 0, p_B, sizeof (U8) );
	p_B = NBytesPut( 0, p_B, sizeof (U8) ); // ignored
	p_B = NBytesPut( 0, p_B, sizeof (U8) ); // ignored
	p_B = NBytesPut( 0, p_B, sizeof (U8) ); // ignored

	/* This needs to be changed as per config file change.Present value
	   includes default values:
	*/
	/* Host Wakeup Control (19 bytes - fields after sub_packet_id) */
	if(p_zGPSCDynaFeatureConfig->feature_flag & GPSC_FEAT_HOSTWAKEUP)
	{
		/* Host Wakeup Control */
		p_B = NBytesPut(p_zGPSCDynaFeatureConfig->host_req_opt, p_B, sizeof (U8) );
		p_B = NBytesPut( p_zGPSCDynaFeatureConfig->host_assert_delay, p_B, sizeof (U16) );
		p_B = NBytesPut( p_zGPSCDynaFeatureConfig->host_reassert_delay, p_B, sizeof (U16) );
		p_B = NBytesPut( p_zGPSCDynaFeatureConfig->host_ref_clk_req_opt, p_B, sizeof (U8) );
		p_B = NBytesPut( p_zGPSCDynaFeatureConfig->host_ref_clk_req_sig_sel, p_B, sizeof (U8) );
		p_B = NBytesPut( p_zGPSCDynaFeatureConfig->host_ref_clk_assert_dly, p_B, sizeof (U16) );
		p_B = NBytesPut( p_zGPSCDynaFeatureConfig->host_ref_clk_reassert_dly, p_B, sizeof (U16) );
		p_B = NBytesPut(p_zGPSCDynaFeatureConfig->host_sigout_type_ctrl, p_B, sizeof (U8) );
		p_B = NBytesPut( 0, p_B, sizeof (U8) ); // ignored
		p_B = NBytesPut( 0, p_B, sizeof (U16) ); // ignored
		p_B = NBytesPut( 0, p_B, sizeof (U32) ); // ignored

	}
	else /* initialize to zero */
	{
		/* Host Wakeup Control */
		p_B = NBytesPut( 0, p_B, sizeof (U8) );
		p_B = NBytesPut( 0, p_B, sizeof (U16) );
		p_B = NBytesPut( 0, p_B, sizeof (U16) );
		p_B = NBytesPut( 0, p_B, sizeof (U8) );
		p_B = NBytesPut( 0, p_B, sizeof (U8) );
		p_B = NBytesPut( 0, p_B, sizeof (U16) );
		p_B = NBytesPut( 0, p_B, sizeof (U16) );
		p_B = NBytesPut( 0, p_B, sizeof (U8) );
		p_B = NBytesPut( 0, p_B, sizeof (U8) ); // ignored
		p_B = NBytesPut( 0, p_B, sizeof (U16) ); // ignored
		p_B = NBytesPut( 0, p_B, sizeof (U32) ); // ignored

	}


	/* External LNS GPS Crystal control (1 byte) */
	p_B = NBytesPut( p_zGPSCConfig->lna_crystal, p_B, sizeof (U8) );
	p_GPSCConfig->lna_crystal = p_zGPSCConfig->lna_crystal;

	/* SBAS Control (15 bytes - fields after sub_packet_id) */
	if((p_zGPSCDynaFeatureConfig->feature_flag & GPSC_FEAT_SBAS) &&
		(p_zGPSCControl->p_zGPSCState->u_SensorOpMode == C_RC_ON) &&
			!(p_zGPSCDynaFeatureConfig->feature_flag & GPSC_FEAT_APM))
	{
		/* SBAS control */
		p_B = NBytesPut( p_zGPSCDynaFeatureConfig->sbas_control, p_B, sizeof (U8) );
		p_B = NBytesPut( p_zGPSCDynaFeatureConfig->sbas_prn_mask, p_B, sizeof (U32) );
		p_B = NBytesPut( p_zGPSCDynaFeatureConfig->Mode, p_B, sizeof (U8) );
		p_B = NBytesPut( p_zGPSCDynaFeatureConfig->Flags, p_B, sizeof (U8) );
		p_B = NBytesPut( 0, p_B, sizeof (U32) ); // ignored
		p_B = NBytesPut( 0, p_B, sizeof (U32) ); // ignored
	}
	else
	{
		/* SBAS control */
		p_B = NBytesPut( 0, p_B, sizeof (U8) );
		p_B = NBytesPut( 0, p_B, sizeof (U32) );
		p_B = NBytesPut( 0, p_B, sizeof (U8) );
		p_B = NBytesPut( 0, p_B, sizeof (U8) );
		p_B = NBytesPut( 0, p_B, sizeof (U32) ); // ignored
		p_B = NBytesPut( 0, p_B, sizeof (U32) ); // ignored
	}

	/* Reserved (15 bytes) */
	p_B = NBytesPut( 0, p_B, sizeof (U32) ); /* reserved */
	p_B = NBytesPut( 0, p_B, sizeof (U32) ); /* reserved */
	p_B = NBytesPut( 0, p_B, sizeof (U32) ); /* reserved */
	p_B = NBytesPut( 0, p_B, sizeof (U16) ); /* reserved */
	p_B = NBytesPut( 0, p_B, sizeof (U8) ); /* reserved */

	/* after initial dynamic configuration, change flag to no request */
//	p_zGPSCDynaFeatureConfig->feature_flag = GPSC_FEAT_NO_REQUEST; // is it really needed?

	/* preparing AI2 packet and send to sensor */
	z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
	z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_CUSTOMER_CONFIG_EXTENDED;
	z_PutMsgDesc.p_Buf = &u_Body[0];
	z_PutMsgDesc.w_PacketLen = (U16)(p_B - &u_Body[0]);
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;
    AI2TX("Customer Config Extended",AI_CLAPP_INJ_CUSTOMER_CONFIG_EXTENDED);
	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}


/*
 ******************************************************************************
 * gpsc_mpg_init_dynamic_feature_config
 *
 * Function description:
 *
 * Initialize and disable all dynamic feature configuration (only for GPS5350) command
 * to Sensor.
 * Dynamic feature supported in GPS5350 are:
 * HOST WAKEUP
 * SBAS
 * USER MOTION MASK (GEO FENSING)
 * APM (Advanced Power Management)
 *
 *  Note: dynamic configuration feature kalman filter is supported in recv cfg.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *
 * Return value:
 *  None
 * Note: added API support to inject dynamic features to sensor
 ******************************************************************************
 */
void gpsc_mpg_init_dynamic_feature_config (gpsc_ctrl_type* p_zGPSCControl)
{
	gpsc_cfg_type * p_zGPSCConfig = p_zGPSCControl->p_zGPSCConfig;
	gpsc_dyna_feature_cfg_type *p_zGPSCDynaFeatureConfig = p_zGPSCControl->p_zGPSCDynaFeatureConfig;

	p_zGPSCDynaFeatureConfig->feature_flag = GPSC_FEAT_NO_REQUEST;

	/* initialize advanced power management (APM) */
	p_zGPSCDynaFeatureConfig->apm_control = 0;
	
	p_zGPSCDynaFeatureConfig->power_save_qc = p_zGPSCConfig->power_save_qc;

	p_zGPSCDynaFeatureConfig->search_mode = p_zGPSCConfig->search_mode;

	p_zGPSCDynaFeatureConfig->saving_options = p_zGPSCConfig->saving_options;

	if(p_zGPSCConfig->apm_control & GPSC_APM_ENABLE)
	{
	    p_zGPSCDynaFeatureConfig->feature_flag |= GPSC_FEAT_APM;
	}

	/* initialize host wakeup control */

	/* This needs to be defined when defining SAP changes for Config File */
	p_zGPSCDynaFeatureConfig->host_req_opt = p_zGPSCConfig->hw_req_opt;
	p_zGPSCDynaFeatureConfig->host_assert_delay = p_zGPSCConfig->hw_assert_delay;
	p_zGPSCDynaFeatureConfig->host_reassert_delay = p_zGPSCConfig->hw_reassert_delay;
	p_zGPSCDynaFeatureConfig->host_ref_clk_req_opt = p_zGPSCConfig->hw_ref_clk_req_opt;
    p_zGPSCDynaFeatureConfig->host_ref_clk_req_sig_sel = p_zGPSCConfig->hw_ref_clk_req_sig_sel;
    p_zGPSCDynaFeatureConfig->host_ref_clk_assert_dly = p_zGPSCConfig->hw_ref_clk_assert_dly;
    p_zGPSCDynaFeatureConfig->host_ref_clk_reassert_dly = p_zGPSCConfig->hw_ref_clk_reassert_dly;
	p_zGPSCDynaFeatureConfig->host_sigout_type_ctrl = p_zGPSCConfig->hw_sigout_type_ctrl;

	if(p_zGPSCDynaFeatureConfig->host_req_opt & GPSC_HW_ENABLE)
	{
	p_zGPSCDynaFeatureConfig->feature_flag |= GPSC_FEAT_HOSTWAKEUP;
	}


//	gpsc_mgp_tx_inject_hostwakeup(p_zGPSCControl);

	/* initialize SBAS configuration */
	p_zGPSCDynaFeatureConfig->sbas_control = p_zGPSCConfig->sbas_control;
	p_zGPSCDynaFeatureConfig->sbas_prn_mask = p_zGPSCConfig->sbas_prn;
	p_zGPSCDynaFeatureConfig->Mode = 0;
	p_zGPSCDynaFeatureConfig->Flags = 0;

	if(p_zGPSCDynaFeatureConfig->sbas_control == GPSC_SBAS_ENABLE)
	{
	p_zGPSCDynaFeatureConfig->feature_flag |= GPSC_FEAT_SBAS;
	}
//	gpsc_mgp_tx_inject_sbas(p_zGPSCControl);

	/* initialize user motion mask settings (GEO FENSING) */
	p_zGPSCDynaFeatureConfig->motion_mask_control = GEOFENCE_MOTION_DISABLE;
//	gpsc_mgp_tx_inject_motion_mask(p_zGPSCControl);

}

/*
 ******************************************************************************
 * gpsc_mpg_inject_dynamic_feature_config
 *
 * Function description:
 *
 *  Select and send dynamic feature configuration (only for GPS5350) command
 *  to Sensor. Dynamic feature supported are host wakeup, SBAS,
 *  user motion mask setting
 *
 *  Note: dynamic configuration feature kalman filter is supported in recv cfg.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *
 * Return value:
 *  None
 * Note: added API support to inject dynamic features to sensor
 ******************************************************************************
 */
void gpsc_mpg_inject_dynamic_feature_config (gpsc_ctrl_type* p_zGPSCControl)
{

	gpsc_dyna_feature_cfg_type *p_zGPSCDynaFeatureConfig = p_zGPSCControl->p_zGPSCDynaFeatureConfig;

	/* inject host wakeup if requested */
	if((p_zGPSCDynaFeatureConfig->feature_flag & GPSC_FEAT_HOSTWAKEUP) &&
	   (p_zGPSCControl->p_zGPSCState->u_SensorOpMode == C_RC_IDLE)
	  )
	{
		gpsc_mgp_tx_inject_hostwakeup(p_zGPSCControl);
		p_zGPSCDynaFeatureConfig->feature_flag &= ~GPSC_FEAT_HOSTWAKEUP;
	}

	/* Check if SBAS is injected through SAP call. If not then inject SBAS
	   configuration */
	/* inject SBAS if requested */

	if((p_zGPSCDynaFeatureConfig->feature_flag & GPSC_FEAT_SBAS) &&
			!(p_zGPSCDynaFeatureConfig->feature_flag & GPSC_FEAT_APM))
	{
             STATUSMSG("Injected SBAS Idle=%d", p_zGPSCDynaFeatureConfig->sbas_control);
		 gpsc_mgp_tx_inject_sbas(p_zGPSCControl);
		 p_zGPSCDynaFeatureConfig->feature_flag &= ~GPSC_FEAT_SBAS;
	}
	/* Check if GEO FENSING (USER_MOTION_MASK) is injected through SAP call. If not then inject
	     Motion Mask Setting configuration */
	/* inject motion mask if requested */
/*	if(p_zGPSCDynaFeatureConfig->feature_flag & GPSC_FEAT_MOTION_MASK)
		{
		gpsc_mgp_tx_inject_motion_mask(p_zGPSCControl);
		p_zGPSCDynaFeatureConfig->feature_flag &= ~GPSC_FEAT_MOTION_MASK;
		} */

}

/*
 ******************************************************************************
 * gpsc_mgp_tx_inject_baud_rate
 *
 * Function description:
 *
 *  This function is used to inject baud rate for USART1 com port.
 *
 *	This message should be sent to inject baud rate of USART1,
 *  the default baud rate for USART1 is 57600
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the
 *  pointers of many structures accessed from different modules and functions.
 *
 *
 * Return value:
 *  None
 ******************************************************************************
 */
void gpsc_mgp_tx_inject_baud_rate (gpsc_ctrl_type* p_zGPSCControl)
{
#ifdef TI_STAND_ALONE_UART_BUSDRV
    U8 u_Body[C_AI2_BAUD_RATE_BODY];
	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;

	gpsc_cfg_type * p_zGPSCConfig = p_zGPSCControl->p_zGPSCConfig;
	U8 *p_B = &u_Body[0];

	STATUSMSG("Status: Baud rate Injected : %d",p_zGPSCConfig->comm_config.comm_config_union.comm_config_uart.uart_baud_rate);

	/* constructing baud rate packet buffer */
	p_B = NBytesPut(p_zGPSCConfig->comm_config.comm_config_union.comm_config_uart.uart_baud_rate, p_B, sizeof(U8) );

	/* preparing AI2 packet and send to sensor */
	z_PutMsgDesc.u_AckType = C_AI2_NO_ACK; //C_AI2_ACK_REQ;
	z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_BAUD_RATE;
	z_PutMsgDesc.p_Buf = &u_Body[0];
	z_PutMsgDesc.w_PacketLen = (U16)(p_B - &u_Body[0]);
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;
    AI2TX("Custom Packet 250 - Inject Baud Rate",AI_CLAPP_INJ_BAUD_RATE);
	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
#else
	UNREFERENCED_PARAMETER(p_zGPSCControl);
#endif
}

/*
 ******************************************************************************
 * gpsc_mgp_tx_inject_advanced_power_management
 *
 * Function description:
 *
 *  This function is used to inject settings for advanced power management (APM)
 *	feature to Sensor in GPS5350.
 *  This message shall be sent while Receiver is in Idle mode.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the
 *  pointers of many structures accessed from different modules and functions.
 *
 *
 * Return value:
 *  None
 * Note: added support to inject settings for advanced power management (APM)
 *       to sensor
 ******************************************************************************
 */
void gpsc_mgp_tx_inject_advanced_power_management (gpsc_ctrl_type* p_zGPSCControl)
{
    U8 u_Body[C_AI2_ADV_POWER_MGMT_BODY];
	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;
	gpsc_dyna_feature_cfg_type *p_zGPSCDynaFeatureConfig = p_zGPSCControl->p_zGPSCDynaFeatureConfig;

	U8 *p_B = &u_Body[0];

	/* motion mask sub packet id */
	p_B = NBytesPut(AI_CLAPP_CUSTOM_SUB_PKT_INJ_ADV_POWER_MGMT_CONF, p_B, sizeof(U8) ); /* Custom - Sub Packet */

	/* to check for initialization, by default feature must be disabled */
	/* user motion mask setting injection as per user request */
	p_B = NBytesPut( p_zGPSCDynaFeatureConfig->apm_control, p_B, sizeof (U8) );
	p_B = NBytesPut( 0, p_B, sizeof (U8) ); /*reserved: Shall be set to 0*/
	p_B = NBytesPut( 0, p_B, sizeof (U8) ); /* Search mode: Shall be set to 0*/
	p_B = NBytesPut( 0, p_B, sizeof (U8) ); /* Saving options: Shall be set to 0*/
	p_B = NBytesPut( 0, p_B, sizeof (U8) ); /* Power save QC: Shall be set to 0*/
	p_B = NBytesPut( 0, p_B, sizeof (U8) ); /* reserved */
	p_B = NBytesPut( 0, p_B, sizeof (U8) ); /* reserved */

	/* preparing AI2 packet and send to sensor */
	z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
	z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_CUSTOM_PACKET;
	z_PutMsgDesc.p_Buf = &u_Body[0];
	z_PutMsgDesc.w_PacketLen = (U16)(p_B - &u_Body[0]);
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;
    	AI2TX("Custom Packet - Injest Advanced Power Management",AI_CLAPP_CUSTOM_SUB_PKT_INJ_ADV_POWER_MGMT_CONF);
	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}




/*
 ******************************************************************************
 * gpsc_mgp_tx_inject_hostwakeup
 *
 * Function description:
 *
 *  Form and send host wakeup configuration (only for GPS5350) command
 *  to Sensor.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *
 * Return value:
 *  None
 * Note: added support to inject Host WakeUp configuration to sensor
 ******************************************************************************
 */
void gpsc_mgp_tx_inject_hostwakeup(gpsc_ctrl_type* p_zGPSCControl)
{
	U8 u_Body[C_AI2_HOST_WAKEUP_BODY];
	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;

	gpsc_dyna_feature_cfg_type *p_zGPSCDynaFeatureConfig = p_zGPSCControl->p_zGPSCDynaFeatureConfig;
	U8 *p_B = &u_Body[0];

	/* host wakeup sub packet id */
	p_B = NBytesPut(AI_CLAPP_CUSTOM_SUB_PKT_INJ_HOST_WAKEUP_CONF, p_B, sizeof(U8) ); /* Custom - Sub Packet */

	/* to check for initialization, by default feature must be disabled */

	/* This needs to be changed as per config file change.Present value
	   includes default values:
	*/
			/* Host Wakeup Control */
		p_B = NBytesPut(p_zGPSCDynaFeatureConfig->host_req_opt, p_B, sizeof (U8) );
		p_B = NBytesPut( p_zGPSCDynaFeatureConfig->host_assert_delay, p_B, sizeof (U16) );
		p_B = NBytesPut( p_zGPSCDynaFeatureConfig->host_reassert_delay, p_B, sizeof (U16) );
		p_B = NBytesPut( p_zGPSCDynaFeatureConfig->host_ref_clk_req_opt, p_B, sizeof (U8) );
		p_B = NBytesPut( p_zGPSCDynaFeatureConfig->host_ref_clk_req_sig_sel, p_B, sizeof (U8) );
		p_B = NBytesPut( p_zGPSCDynaFeatureConfig->host_ref_clk_assert_dly, p_B, sizeof (U16) );
		p_B = NBytesPut( p_zGPSCDynaFeatureConfig->host_ref_clk_reassert_dly, p_B, sizeof (U16) );
		p_B = NBytesPut(p_zGPSCDynaFeatureConfig->host_sigout_type_ctrl, p_B, sizeof (U8) );
		p_B = NBytesPut( 0, p_B, sizeof (U8) ); // ignored
		p_B = NBytesPut( 0, p_B, sizeof (U16) ); // ignored
		p_B = NBytesPut( 0, p_B, sizeof (U32) ); // ignored


	/* preparing AI2 packet and send to sensor */
	z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
	z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_CUSTOM_PACKET;
	z_PutMsgDesc.p_Buf = &u_Body[0];
	z_PutMsgDesc.w_PacketLen = (U16)(p_B - &u_Body[0]);
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;
    AI2TX("Custom Packet - Injest Host Wakeup Config",AI_CLAPP_INJ_CUSTOMER_CONFIG_EXTENDED);
	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}

/*
 ******************************************************************************
 * gpsc_mgp_tx_inject_sbas
 *
 * Function description:
 *
 *  Form and send SBAS control configuration (only for GPS5350) command
 *  to Sensor.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *
 * Return value:
 *  None
 * Note: added support to inject SBAB control to sensor
 ******************************************************************************
 */
void gpsc_mgp_tx_inject_sbas(gpsc_ctrl_type* p_zGPSCControl)
{

	U8 u_Body[C_AI2_SBAS_CTRL_BODY];
	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;

	gpsc_dyna_feature_cfg_type *p_zGPSCDynaFeatureConfig = p_zGPSCControl->p_zGPSCDynaFeatureConfig;
	U8 *p_B = &u_Body[0];

	/* SBAS ctrl sub packet id */
	p_B = NBytesPut(AI_CLAPP_CUSTOM_SUB_PKT_INJ_SBAS_CTRL, p_B, sizeof(U8) );

	/* Custom - Sub Packet */

	/* to check for initialization, by default feature must be disabled */
	/* SBAS control feature injection */
	/* SBAS control */
	p_B = NBytesPut( p_zGPSCDynaFeatureConfig->sbas_control, p_B, sizeof (U8) );
	p_B = NBytesPut( p_zGPSCDynaFeatureConfig->sbas_prn_mask, p_B, sizeof (U32) );
	p_B = NBytesPut( p_zGPSCDynaFeatureConfig->Mode, p_B, sizeof (U8) );
	p_B = NBytesPut( p_zGPSCDynaFeatureConfig->Flags, p_B, sizeof (U8) );
	p_B = NBytesPut( 0, p_B, sizeof (U32) ); /* reserved */
	p_B = NBytesPut( 0, p_B, sizeof (U32) ); /* reserved */

	/* preparing AI2 packet and send to sensor */
	z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
	z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_CUSTOM_PACKET;
	z_PutMsgDesc.p_Buf = &u_Body[0];
	z_PutMsgDesc.w_PacketLen = (U16)(p_B - &u_Body[0]);
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;
    AI2TX("Custom Packet - Injest SBAS Ctrl",AI_CLAPP_INJ_CUSTOMER_CONFIG_EXTENDED);
	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}
/*
 ******************************************************************************
 * gpsc_mgp_tx_blf_buff_dump
 *
 * Function description:
 *
 *  Form and send BLF dump buffer command
 *  to Sensor.
 *
 * Parameters: None
 *
 * Return value:
 *  None
 ******************************************************************************
 */
void gpsc_mgp_tx_blf_buff_dump(gpsc_ctrl_type* p_zGPSCControl)
{
    gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;

    z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
    z_PutMsgDesc.u_PacketId = AI_REQ_BLF_DUMP;
    z_PutMsgDesc.p_Buf = NULL;

    z_PutMsgDesc.w_PacketLen = 0;

    z_PutMsgDesc.u_ForceFlag = 0;
    z_PutMsgDesc.u_MsgSend = FALSE;

    AI2TX("BLF Dump request",AI_REQ_BLF_DUMP);
    gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc);
}


/*
 ******************************************************************************
 * gpsc_mgp_tx_blf_sts
 *
 * Function description:
 *
 *  Form and send BLF configuration status command
 *  to Sensor.
 *
 * Parameters: None
 *
 *
 * Return value:
 *  None
 ******************************************************************************
 */
void gpsc_mgp_tx_blf_sts(gpsc_ctrl_type* p_zGPSCControl)
{
    gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;

    z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
    z_PutMsgDesc.u_PacketId = AI_REQ_BLF_STS;
    z_PutMsgDesc.p_Buf = NULL;

    z_PutMsgDesc.w_PacketLen = 0;

    z_PutMsgDesc.u_ForceFlag = 0;
    z_PutMsgDesc.u_MsgSend = FALSE;

    AI2TX("BLF Status request",AI_REQ_BLF_STS);
    gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}

/*
 ******************************************************************************
 * gpsc_mgp_tx_blf_config
 *
 * Function description:
 *
 *  Form and send SBAS user BLF configuration setting command
 *  to Sensor.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *
 * Return value:
 *  None
 ******************************************************************************
 */
void gpsc_mgp_tx_blf_config(gpsc_ctrl_type* p_zGPSCControl)
{
  //gpsc_db_type*  p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;  /* Klocwork Changes */
  U8  u_Data[C_AI2_BLF_CONFIG], *p_B = u_Data;
  gpsc_ai2_put_msg_desc_type z_PutMsgDesc;
  gpsc_dyna_feature_cfg_type *p_zGPSCDynaFeatureConfig = p_zGPSCControl->p_zGPSCDynaFeatureConfig;

  /* user motion mask setting injection */
  p_B = NBytesPut( p_zGPSCDynaFeatureConfig->blf_en_flg, p_B, sizeof (U8) );
  p_B = NBytesPut( p_zGPSCDynaFeatureConfig->blf_fix_count, p_B, sizeof (U16) );
  p_B = NBytesPut( p_zGPSCDynaFeatureConfig->blf_en_position, p_B, sizeof (U8) );
  p_B = NBytesPut( p_zGPSCDynaFeatureConfig->blf_en_velocity, p_B, sizeof (U8) );
  p_B = NBytesPut( p_zGPSCDynaFeatureConfig->blf_en_dop, p_B, sizeof (U8) );
  p_B = NBytesPut( p_zGPSCDynaFeatureConfig->blf_en_sv, p_B, sizeof (U8) );

  z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
  z_PutMsgDesc.u_PacketId = AI_REQ_BLF_CFG;
  z_PutMsgDesc.p_Buf = u_Data;
  z_PutMsgDesc.w_PacketLen = C_AI2_BLF_CONFIG;
  z_PutMsgDesc.u_ForceFlag = 0;
  z_PutMsgDesc.u_MsgSend = FALSE;

  AI2TX("Config Buffer Location Fix ", AI_REQ_BLF_CFG);
  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}
/*
 ******************************************************************************
 * gpsc_mgp_tx_inject_motion_mask
 *
 * Function description:
 *
 *  Form and send SBAS user motion mask setting (geo fensing, only for GPS5350) command
 *  to Sensor.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *
 * Return value:
 *  None
 * Note: added support to injectuser motion mask setting to sensor
 ******************************************************************************
 */
    U8  gpsc_mgp_tx_inject_motion_mask(gpsc_ctrl_type* p_zGPSCControl)
{
    gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;

    gpsc_set_motion_mask* p_zGPSCsetMotionMask = p_zGPSCControl->p_zSetMotionMask;
    U8  u_Data[94];  /* 14 + max. 10* 8 */
        U8  u_PacketLen;
    U8 *p_B = &u_Data[0];
    int i =0;

    /* motion mask sub packet id */
    p_B = NBytesPut(AI_CLAPP_CUSTOM_SUB_PKT_INJ_MOTION_MASK_SETTING, p_B, sizeof(U8) ); /* Custom - Sub Packet */

    /* user motion mask setting injection */
        *p_B++ = p_zGPSCsetMotionMask->report_configuration;
		*p_B++ = p_zGPSCsetMotionMask->region_number;
        *p_B++ = p_zGPSCsetMotionMask->region_type;
    p_B = NBytesPut( p_zGPSCsetMotionMask->motion_mask_control, p_B, sizeof (U8) );
        *p_B++ = p_zGPSCsetMotionMask->no_of_vertices;
    p_B = NBytesPut( p_zGPSCsetMotionMask->speed_limit, p_B, sizeof (U16) );
    p_B = NBytesPut( p_zGPSCsetMotionMask->altitude_limit, p_B, sizeof (S16) );
    p_B = NBytesPut( p_zGPSCsetMotionMask->area_altitude, p_B, sizeof (S16) );
    p_B = NBytesPut( p_zGPSCsetMotionMask->radius_of_circle, p_B, sizeof (U16) );

    while (i< p_zGPSCsetMotionMask->no_of_vertices)
    {
        p_B = NBytesPut( p_zGPSCsetMotionMask->latitude[i], p_B, sizeof (S32) );
        p_B = NBytesPut( p_zGPSCsetMotionMask->longitude[i], p_B, sizeof (S32) );

        i++;
    }

    u_PacketLen = (U8)(14 + 8 * (p_zGPSCsetMotionMask->no_of_vertices));

    z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
    z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_CUSTOM_PACKET;
    z_PutMsgDesc.p_Buf = &u_Data[0];
    z_PutMsgDesc.w_PacketLen = u_PacketLen;
    z_PutMsgDesc.u_ForceFlag = 0;
    z_PutMsgDesc.u_MsgSend = TRUE;

    gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
    return gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK);
}


U8  gpsc_mgp_tx_req_sv_meas_del(gpsc_ctrl_type* p_zGPSCControl, U8 sv_ID)
{
	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;
	U8  u_Data[2];  /*2 */
	U8 *p_B = &u_Data[0];

	STATUSMSG("gpsc_mgp_tx_req_sv_meas_del: for svID: %d",sv_ID);
	/* Request Sv measurement delete sub packet id */
	p_B = NBytesPut(AI_CLAPP_CUSTOM_SUB_PKT_REQ_SV_MEAS_DEL, p_B, sizeof(U8) ); /* Custom - Sub Packet */
	*p_B++ = sv_ID;

	/* preparing AI2 packet and send to sensor */
	//z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ;
	z_PutMsgDesc.u_AckType   = C_AI2_NO_ACK;
	z_PutMsgDesc.u_PacketId  = AI_CLAPP_INJ_CUSTOM_PACKET;
	z_PutMsgDesc.p_Buf       = &u_Data[0];
	z_PutMsgDesc.w_PacketLen = (p_B - &u_Data[0]);
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend   = TRUE;
	AI2TX("Custom Packet - Inject request sv measurement delete",AI_CLAPP_CUSTOM_SUB_PKT_REQ_SV_MEAS_DEL);

	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
	if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
	{
		STATUSMSG("gpsc_mgp_tx_req_sv_meas_del: meas delete failed!! for sv %d \n",sv_ID);
		return FALSE;
	}
	return TRUE;
}

void gpsc_mgp_tx_req_motion_mask_setting(gpsc_ctrl_type* p_zGPSCControl,U8 regionnumber)
{

	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;
	U8 u_Body[2];
	U8 *p_B = &u_Body[0];
	STATUSMSG("region number %d \n",regionnumber);
	p_B = NBytesPut(AI_CLAPP_CUSTOM_SUB_PKT_REQ_MOTION_MASK_SETTING, p_B, sizeof(U8) ); /* 1 byte */

	p_B = NBytesPut(regionnumber, p_B, sizeof(U8) ); /* 1 byte */

	/* preparing AI2 packet and send to sensor */
	z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ; //C_AI2_NO_ACK; //C_AI2_ACK_REQ;
	z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_CUSTOM_PACKET;
	z_PutMsgDesc.p_Buf = &u_Body[0];
	z_PutMsgDesc.w_PacketLen = (U16)(p_B - &u_Body[0]);
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = TRUE;
    AI2TX("Custom Packet 251 - request motion mask settings",AI_CLAPP_CUSTOM_SUB_PKT_REQ_MOTION_MASK_SETTING);
	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}


/*
 ******************************************************************************
 * maximize_frunc
 *
 * Function description:
 *
 * maximize frequency uncertainty in sensor. Note: sensor will cap freq. unc. to either
 * its defined maximum, or calibration will provide a frequency uncertainty.
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
void maximize_frunc(gpsc_ctrl_type *p_zGPSCControl)
{
  gpsc_sys_handlers*	p_GPSCSysHandlers;
  gpsc_cfg_type*   p_zGPSCConfig  = p_zGPSCControl->p_zGPSCConfig;
  //gpsc_state_type*			p_zGPSCStateType = p_zGPSCControl->p_zGPSCState;  /* Klocwork Changes */
  //gpsc_db_gps_clk_type*      p_DBGpsClock =     &p_zGPSCDatabase->z_DBGpsClock;  /* Klocwork Changes */
  gpsc_inject_freq_est_type  z_InjectFreqEst;
  gpsc_inject_freq_est_type* p_zInjectFreqEst = &z_InjectFreqEst;
  gpsc_db_gps_inj_clk_type z_DBGpsInjClock;
  McpS32	fd;
  EMcpfRes	eRetVal;
  //klockwork
  McpU8 uAidingFile[C_MAX_AIDING_FILENAME_LEN+20];

  	p_GPSCSysHandlers = p_zGPSCControl->p_zSysHandlers;

	MCP_HAL_STRING_StrCpy (uAidingFile,(McpU8*)p_GPSCSysHandlers->uAidingPath);
	MCP_HAL_STRING_StrCat (uAidingFile,C_STR_AID_TCXO_FILE);

	eRetVal = mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *) uAidingFile,
			MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_RDONLY, &fd);

	if(eRetVal == RES_OK)
	{
			McpU32 uNumRead = 0;
			uNumRead = mcpf_file_read(p_zGPSCControl->p_zSysHandlers->hMcpf, fd,
				     ( void* ) &z_DBGpsInjClock, sizeof( gpsc_db_gps_inj_clk_type )  );
			mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);

			if (sizeof( gpsc_db_gps_inj_clk_type ) != uNumRead)
			{
				STATUSMSG(" maximize_frunc: Read failed");
				memset(&z_DBGpsInjClock, 0, sizeof(gpsc_db_gps_inj_clk_type));
				z_DBGpsInjClock.u_Valid = FALSE;
				z_DBGpsInjClock.l_FreqBiasRaw=0;
				z_DBGpsInjClock.q_FreqUncRaw=0;

				p_zInjectFreqEst->l_FreqBiasRaw = (S32)(0);
				p_zInjectFreqEst->q_FreqUncRaw =  (U32) ((p_zGPSCConfig->max_gps_clk_unc * LIGHT_SEC * 1e-6) );
				p_zInjectFreqEst->applyFlag = TRUE;
            }
            else
            {
				STATUSMSG(" maximize_frunc: Read pass. z_DBGpsInjClock.u_Valid = %d", z_DBGpsInjClock.u_Valid);
				if (z_DBGpsInjClock.u_Valid)
				{
					p_zInjectFreqEst->l_FreqBiasRaw = (S32)(z_DBGpsInjClock.l_FreqBiasRaw/(FLT) C_LSB_FRQ_BIAS);
					p_zInjectFreqEst->q_FreqUncRaw =  (U32) ((p_zGPSCConfig->shrt_term_gps_clk_unc * LIGHT_SEC * 1e-6) * (10.0f));
					p_zInjectFreqEst->applyFlag = TRUE;
				}
				else
				{
					p_zInjectFreqEst->l_FreqBiasRaw = (S32)(0);
					p_zInjectFreqEst->q_FreqUncRaw =  (U32) ((p_zGPSCConfig->tcxo_unc_longterm * LIGHT_SEC * 1e-6) * 10.0f);
					p_zInjectFreqEst->applyFlag = FALSE;
				}
			}

	}
	else
	{
			STATUSMSG(" maximize_frunc: Open failed");
			p_zInjectFreqEst->l_FreqBiasRaw = (S32)(0);
			p_zInjectFreqEst->q_FreqUncRaw =  (U32) ((p_zGPSCConfig->max_gps_clk_unc * LIGHT_SEC * 1e-6));
			p_zInjectFreqEst->applyFlag = TRUE;
	}

  STATUSMSG(" maximize_frunc: l_FreqBiasRaw = %d, q_FreqUncRaw = %d, applyFlag = %d", p_zInjectFreqEst->l_FreqBiasRaw,
                                                                                            p_zInjectFreqEst->q_FreqUncRaw,
                                                                                          p_zInjectFreqEst->applyFlag);
  gpsc_mgp_inject_freq_est(p_zGPSCControl, p_zInjectFreqEst);
}




U8 gpsc_tx_req_injected_alm(gpsc_ctrl_type *p_zGPSCControl)
{
  gpsc_db_type*              p_zGPSCDatabase =   p_zGPSCControl->p_zGPSCDatabase;
  int u_SvId;
  char SVList[100],*p_List = &SVList[0];
  /*If no Svs to request for return false*/
  if(p_zGPSCDatabase->q_InjAlmList ==0)
	  return FALSE;


  AI2TX("Request Almanac",AI_REQ_ALM);
  for(u_SvId = 0; u_SvId < N_SV ;u_SvId++)
	{
		if(p_zGPSCDatabase->q_InjAlmList & (1<< u_SvId))
		{
			gpsc_mgp_tx_req( p_zGPSCControl, AI_REQ_ALM, (U8)(u_SvId+1));
			p_List+= sprintf(p_List,"%d,",u_SvId+1);
		}
	}
  STATUSMSG("Status, Requested Almanac for Svs:%s",SVList);
  p_zGPSCDatabase->q_InjAlmList =0;
  return TRUE;
}

U8 gpsc_tx_req_injected_eph(gpsc_ctrl_type *p_zGPSCControl)
{
  gpsc_db_type*              p_zGPSCDatabase =   p_zGPSCControl->p_zGPSCDatabase;
  int u_SvId;
  char SVList[100],*p_List = &SVList[0];
  /*If no Svs to request for return false*/
  if(p_zGPSCDatabase->q_InjEphList ==0)
	  return FALSE;


  AI2TX("Request Ephemeris",AI_REQ_EPHEM);
  for(u_SvId = 0; u_SvId < N_SV ;u_SvId++)
	{
		if(p_zGPSCDatabase->q_InjEphList & (1<< u_SvId))
		{
			gpsc_mgp_tx_req( p_zGPSCControl, AI_REQ_EPHEM, (U8)(u_SvId+1));
			p_List+= sprintf(p_List,"%d,",u_SvId+1);
		}
	}
  STATUSMSG("Status, Requested ephemeris for Svs:%s",SVList);
  p_zGPSCDatabase->q_InjEphList=0;
  return TRUE;
}


/*
 ******************************************************************************
 * gpsc_mgp_inject_calibrationcontrol
 *
 * Function description:
 *
 *  Form and send calibration control message to Sensor.
 *
 *
 * Parameters:
 *
 * For NL53xx - Type of calibration -> uCalibParams = u_CalibType
 *    0: Calibration disabled
 *    2: One shot calibration disabled in config-4
 *    3: One shot calibration enabled in config-4
 *    Any other value: Periodic calibration enabled
 *
 *
 * For NL5500
 *
 *	Calibration Parameters - uCalibParams
 *    bit 0:   0 - Calibration disabled.
 *             1 - Calibration Enabled.
 *    bit 1:   0 - Periodic calibration.
 *             1 - One shot calibration.
 *    bit 3-2: 0 - Ref Clock based calibration.
 *             1 - Time Stamp based calibration.
 *    bit 4-7: reserved.
 *
 *
 * Return value:
 *	None
 *
 *
 ******************************************************************************
 */
void gpsc_mgp_inject_calibrationcontrol
(
  gpsc_ctrl_type*  p_zGPSCControl,U8 uCalibParams
)
{

   gpsc_ai2_put_msg_desc_type    z_PutMsgDesc;
   U8	u_Data[1],*p_B = u_Data;

	STATUSMSG("gpsc_mgp_inject_calibrationcontrol: %d", uCalibParams);
    //calibration type
	p_B = NBytesPut( uCalibParams, p_B, sizeof (U8) );

	/* added for not to handle ack during active state */
	if(E_GPSC_STATE_ACTIVE == p_zGPSCControl->p_zGpscSm->e_GpscCurrState)
		z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
	else
		z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ;

	z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_CALIB_CONTROL;
	z_PutMsgDesc.p_Buf = u_Data;
	z_PutMsgDesc.w_PacketLen = 1;
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;

	AI2TX("Inject calibration control",AI_CLAPP_INJ_CALIB_CONTROL);
	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );

}

/*
 ******************************************************************************
 * gpsc_mgp_inject_comm_prot
 *
 * Function description:
 *
 *  This function is used to select communication protocol
 *  of the GPS chip. At power-up of device this packet shall
 *  be sent as the first message.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the
 *  pointers of many structures accessed from different modules and functions.
 *
 *
 * Return value:
 *  None
 ******************************************************************************
 */

void gpsc_mgp_inject_comm_prot(gpsc_ctrl_type* p_zGPSCControl)
{
  U8 u_Body[C_AI2_PROT_SEL];
	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;

	U8 *p_B = &u_Body[0];

	STATUSMSG("Status: Protocol Selection - Default is AI2");

	/* constructing the protocol selection packet */
	p_B = NBytesPut(GPSC_AI2, p_B, sizeof(U8) );

	/* preparing AI2 packet and send to sensor */
	z_PutMsgDesc.u_AckType = C_AI2_NO_ACK; //C_AI2_ACK_REQ;
	z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_COMM_PROT;
	z_PutMsgDesc.p_Buf = &u_Body[0];
	z_PutMsgDesc.w_PacketLen = (U16)(p_B - &u_Body[0]);
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;
    AI2TX("Inject Communication Protocol",AI_CLAPP_INJ_COMM_PROT);
	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}
/*
 ******************************************************************************
 *
 * gpsc_mgp_inject_gps_shutdown_ctrl
 *
 * Function description: This packet can be used to shutdown the GPS IP
 * in shared interface mode of operation. This command is not supported
 * during direct interface mode over UART and I2C.
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

void gpsc_mgp_inject_gps_shutdown_ctrl(gpsc_ctrl_type* p_zGPSCControl)
{
  U8 u_Body[3];
	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;
#ifdef GPSC_NL5500
//	gpsc_cfg_type * p_zGPSCConfig = p_zGPSCControl->p_zGPSCConfig;
#endif /* #ifdef GPSC_NL5500 */

	U8 *p_B = &u_Body[0];

  /* Sub PacketID value 23 */
  p_B = NBytesPut(AI_CLAPP_CUSTOM_SUB_PKT_GPS_SHUTDOWN_CTRL, p_B,sizeof(U8));

	/* Reserved Value - 0 */
	p_B = NBytesPut( 0, p_B, sizeof (U8) );

  /* GPS Shutdown Control - 0 */
	p_B = NBytesPut( GPSC_SHUTDOWN_CTRL, p_B, sizeof (U8) );


	/* preparing AI2 packet and send to sensor */
	z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
	z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_CUSTOM_PACKET;
	z_PutMsgDesc.p_Buf = &u_Body[0];
	z_PutMsgDesc.w_PacketLen = (U16)(p_B - &u_Body[0]);
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;
    AI2TX("Custom Packet - Inject GPS Shutdown Control",AI_CLAPP_CUSTOM_SUB_PKT_GPS_SHUTDOWN_CTRL);
	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}

/*
 ******************************************************************************
 *
 * gpsc_mgp_inject_wakeup_sequence
 *
 * Function description: This packet shall be used by host to wakeup
 * device from deep sleep.
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

void gpsc_mgp_inject_wakeup_sequence(gpsc_ctrl_type* p_zGPSCControl)
{
  U8 u_Body[3];
	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;

	U8 *p_B = &u_Body[0];

  /* Sub PacketID value 23 */
  p_B = NBytesPut(AI_CLAPP_CUSTOM_SUB_PKT_INJ_WAKEUP_SEQ, p_B,sizeof(U8));

	/* Reserved Value - 0 */
	p_B = NBytesPut( 0, p_B, sizeof (U16) );


	/* preparing AI2 packet and send to sensor */
	z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
	z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_CUSTOM_PACKET;
	z_PutMsgDesc.p_Buf = &u_Body[0];
	z_PutMsgDesc.w_PacketLen = (U16)(p_B - &u_Body[0]);
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;
    AI2TX("Custom Packet - Inject Wakeup Sequence",AI_CLAPP_CUSTOM_SUB_PKT_INJ_WAKEUP_SEQ);
	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}


/*
 ******************************************************************************
 * gpsc_mgp_inject_timepulse_ref_clk_req_res
 *
* Function description: This packet shall be used by host in response
 * to REF_CLK request from GPS, while operating in host REF_CLK request
 * option set to "GPS SW message controlled calibration".Host shall
 * send this message after ensuring stable REF_CLK is supplied to NL5500.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *  u_TimestampRefClkResponse - Response Option - Enable/Disable
 *
 * Return value:
 *  None
 *
 ******************************************************************************
 */
void gpsc_mgp_inject_timepulse_ref_clk_req_res
(
  gpsc_ctrl_type*  p_zGPSCControl,U8 u_TimestampRefClkResponse
)
{
    U8 u_Body[C_AI2_REF_CLK_RESP];
	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;

	U8 *p_B = &u_Body[0];

	/* to avoid level 4 warning */
	UNREFERENCED_PARAMETER(u_TimestampRefClkResponse);

	/* Ref Clock Response sub packet id */
	p_B = NBytesPut(AI_CLAPP_CUSTOM_SUB_PKT_REF_CLK_REQ_RESP, p_B, sizeof(U8) );

	/* 1 reserve byte */
	p_B = NBytesPut( 0, p_B, sizeof (U8) );

	/* Ref clock response value */
	p_B = NBytesPut(GPSC_REF_CLK_STABLE, p_B, sizeof (U8) );


	/* preparing AI2 packet and send to sensor */
	z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
	z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_CUSTOM_PACKET;
	z_PutMsgDesc.p_Buf = &u_Body[0];
	z_PutMsgDesc.w_PacketLen = (U16)(p_B - &u_Body[0]);
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;
    AI2TX("Custom Packet - Inject Ref Clock Request Response",AI_CLAPP_CUSTOM_SUB_PKT_REF_CLK_REQ_RESP);
	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}


/*
 ******************************************************************************
 *
 * gpsc_mgp_inject_altitude
 *
 * Function description: This packet shall be used by host to inject altitude if
 * altitude is configured in gpsc config file as Allow 2D/ Manual 2D.
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


void gpsc_mgp_inject_altitude(
  gpsc_ctrl_type*    p_zGPSCControl,
  gpsc_inject_altitude_type*  p_InjectAltitude
)
{
  U8  u_Data[5], *p_B = u_Data;
  gpsc_ai2_put_msg_desc_type z_PutMsgDesc;

  p_B = NBytesPut (p_InjectAltitude->x_Altitude * 2,p_B,sizeof(S16));
  p_B = NBytesPut (p_InjectAltitude->w_AltitudeUnc * 2,p_B,sizeof(U16));

  *p_B = p_InjectAltitude->u_ForceFlag;

	STATUSMSG("Status:Altitude Injected, Est:%d, Unc:%d, Force:%d",
		p_InjectAltitude->x_Altitude,
		p_InjectAltitude->w_AltitudeUnc,
		p_InjectAltitude->u_ForceFlag);

  z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ;
  z_PutMsgDesc.u_PacketId = AI_INJ_ALTITUDE;
  z_PutMsgDesc.p_Buf = u_Data;
  z_PutMsgDesc.w_PacketLen = 5;
  z_PutMsgDesc.u_ForceFlag = 0;
  z_PutMsgDesc.u_MsgSend = FALSE;

  AI2TX("Inject Altitude",AI_INJ_ALTITUDE);
  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}
/*
 ******************************************************************************
 *
 * gpsc_mgp_skip_download
 *
 * Function description: This function injects no patch download message to GPS receiver.
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

void gpsc_mgp_skip_download (gpsc_ctrl_type* p_zGPSCControl)
{
            STATUSMSG("Status: Sending Download Control with No Download Selected");
	        gpsc_mgp_send_patch_dwld_ctrl(p_zGPSCControl,C_GPSC_PATCH_NO_DOWNLOAD);
	        if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
			{
				//   return GPSC_TX_INITIATE_FAIL;
				FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
				GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
			}
#ifdef GPSC_DEBUG
	        printf("\nStatus: Skip The Download process\n");
#endif
	        //STATECHANGE_REPORT_FERROR(C_GPSC_STATE_DOWNLOAD_COMPLETE);

}


/*
 ******************************************************************************
 *
 * gpsc_mgp_tx_visible_SV_eph
 *
 * Function description: This function injects EPH for SVs that are visiable in received measurement
 *	report.
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
void gpsc_mgp_tx_visible_sv_eph(gpsc_ctrl_type* p_zGPSCControl)
{

  gpsc_db_type *p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
  gpsc_smlc_assist_type*  p_zSmlcAssist  = p_zGPSCControl->p_zSmlcAssist;
  gpsc_db_gps_meas_type    *p_zDBGpsMeas = &p_zGPSCDatabase->z_DBGpsMeas;
  gpsc_meas_report_type   *p_zMeasReport = &p_zDBGpsMeas->z_MeasReport;
  gpsc_meas_per_sv_type    *p_zMeasPerSv;
  gpsc_inject_eph_type z_InjectEph, *p_zInjectEph = &z_InjectEph;
  gpsc_db_gps_clk_type*   p_zDBGpsClock  = &p_zGPSCDatabase->z_DBGpsClock;

  U8 u_i;
  U8 u_SvId;

	for (u_i=0;u_i<16;u_i++)
	{
		p_zMeasPerSv = &p_zMeasReport->z_MeasPerSv[u_i];
		u_SvId = (U8)(p_zMeasPerSv->u_SvIdSvTimeFlag&0x3F);
		if((u_SvId) &&(p_zMeasPerSv->w_Cno>GPSC_VISIBLE_EPH_INJ_THOLD))
		{
			/* SV is visible in the measurement report */
			p_zInjectEph->p_zRawEphemeris = &p_zSmlcAssist->z_SmlcRawEph[u_SvId-1];
			p_zInjectEph->p_zRawSF1Param  = &p_zSmlcAssist->z_SmlcRawSF1Param[u_SvId-1];
			p_zInjectEph->w_EphWeek  =  p_zDBGpsClock->w_GpsWeek;

			if((!(p_zSmlcAssist->q_InjSvEph&(McpU32)mcpf_Pow(2,u_SvId-1)))&&p_zInjectEph->p_zRawEphemeris->u_Prn !=0)
			{
				/* EPH for SV has not been injected, inject it */
				gpsc_mgp_inject_eph(p_zGPSCControl, p_zInjectEph, 0);

				if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
				{
					FATAL_INIT(GPSC_TX_INITIATE_FAIL);
				}

				STATUSMSG("gpsc_mgp_tx_visible_sv_eph: Injected Visible Eph from Measure report for Sv %u ", p_zInjectEph->p_zRawEphemeris->u_Prn);

				/*Clear satellite from records*/
				p_zInjectEph->p_zRawEphemeris->u_Prn =0;

				/* set the bit */
				p_zSmlcAssist->q_InjSvEph = p_zSmlcAssist->q_InjSvEph | (McpU8)mcpf_Pow(2,u_SvId-1);

			}

		}

	}
}

/*
 ******************************************************************************
 * gpsc_mgp_inject_utc
 *
 * Function description: This packet injects the UTC assistance data received from
 * the network to the device as per AI2 protocol
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *  gpsc_raw_utc_type* - pointer to UTC struct to be injected.
 *
 * Return value:
 *  None
 *
 ******************************************************************************
 */

void gpsc_mgp_inject_utc (  gpsc_ctrl_type*      p_zGPSCControl,  gpsc_raw_utc_type*  p_Utc)
{

gpsc_db_type*     p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
/*   gpsc_state_type*  p_zGPSCState = p_zGPSCControl->p_zGPSCState; */
  gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;
  U8 u_Data[14], *p_B = u_Data;


  /* Bytes 0,1,2,3 are A0 */
  p_B = NBytesPut(p_Utc->utc_a0,  p_B, sizeof (U32) );


/* Bytes 4,5,6,7 are A1 */
p_B = NBytesPut( p_Utc->utc_a1, p_B, sizeof(U32) );

/* Byte 8 is DeltaTls */
*p_B++ = p_Utc->utc_delta_tls;

/* Byte 9 is Tot */
*p_B++ = p_Utc->utc_tot;

/* Byte 10 is WNt */
*p_B++ = p_Utc->utc_wnt;

/* Byte 11 is WNlsf */
*p_B++ = p_Utc->utc_wnlsf;

/* Byte 12 is DN */
*p_B++ = p_Utc->utc_dn;

/* Byte 13 is DeltaTlsf */
*p_B++ = p_Utc->utc_delta_tlsf;


  z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ;
  z_PutMsgDesc.u_PacketId = AI_INJ_UTC;
  z_PutMsgDesc.p_Buf = u_Data;
  z_PutMsgDesc.w_PacketLen = 14;
  z_PutMsgDesc.u_ForceFlag = 0;
  z_PutMsgDesc.u_MsgSend = FALSE;

  AI2TX("Inject UTC",AI_INJ_UTC);
  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );

  /* once inject command is sent, mark LM database for this item "not-valid"
     until Sensor later reports this item and then update the database */
  p_zGPSCDatabase->z_DBUtc.d_UpdateTimeSec = 0;
  p_zGPSCDatabase->z_DBUtc.u_valid = 0;

}
/*
 ******************************************************************************
 * gpsc_mgp_tx_rtc_control
 *
 * Function description:
 *
 *  Form and send rtc time control information to the sensor.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *
 * Return value:
 *  None
 * Note: added support to inject timeout info and CEP info to sensor
 ******************************************************************************
 */
void gpsc_mgp_tx_rtc_control(gpsc_ctrl_type* p_zGPSCControl)
{
	gpsc_cfg_type		*p_zGPSCConfig		= p_zGPSCControl->p_zGPSCConfig;
	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;
	U8 u_Body[C_AI2_RTC_CTRL_BODY];
	U8 *p_B = &u_Body[0];

	/* RTC time crtl packet id */
	p_B = NBytesPut(AI_CLAPP_CUSTOM_SUB_PKT_INJ_RTC_TIME_INJECT_CTRL, p_B, sizeof(U8) ); /* 1 byte */

	p_B = NBytesPut(p_zGPSCConfig->enable_rtc_time_injection, p_B, sizeof(U8) ); /* 1byte: RTC: Injection Control, 0:Disable, 1:Enable*/
	p_B = NBytesPut(p_zGPSCConfig->rtc_calibration, p_B, sizeof(U8)); /* 1byte: RTC: Time Quality Source, 0:Calib, 1:Msg*/
	p_B = NBytesPut(p_zGPSCConfig->rtc_quality, p_B, sizeof(U16) ); /* 2byte: RTC: Quality, Unsigned 1ppm per bit */

	STATUSMSG("gpsc_mgp_tx_rtc_control: CTRL: %d, SRC: %d, QUL: %d",
		p_zGPSCConfig->enable_rtc_time_injection,
		p_zGPSCConfig->rtc_calibration,
		p_zGPSCConfig->rtc_quality);

	/* preparing AI2 packet and send to sensor */
	z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
	z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_CUSTOM_PACKET;
	z_PutMsgDesc.p_Buf = &u_Body[0];
	z_PutMsgDesc.w_PacketLen = (U16)(p_B - &u_Body[0]);
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;
	AI2TX("Custom Packet 251 - RTC time control",AI_CLAPP_CUSTOM_SUB_PKT_INJ_RTC_TIME_INJECT_CTRL);
	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}


/*
 ******************************************************************************
 * gpsc_mgp_tx_read_reg
 *
 * Function description:
 *
 *  Function used to send the read register AI2 to the sensor
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 * regaddress: register address
 *
 * Return value:
 *  void
 ******************************************************************************
 */
void gpsc_mgp_tx_read_reg(gpsc_ctrl_type* p_zGPSCControl, U32 q_ReadReg, U8 u_Reglen)
{
	//gpsc_cfg_type		*p_zGPSCConfig		= p_zGPSCControl->p_zGPSCConfig;  /* Klocwork Changes */
	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;
	U8 u_Body[C_AI2_RF_READ_REG_BODY];
	U8 *p_B = &u_Body[0];

	/* RF Registor, Memory Read/Write msg contents */
	p_B = NBytesPut(AI_CLAPP_CUSTOM_SUB_PKT_MEM_RW, p_B, sizeof(U8)); /* 1byte: sub packet id*/
	p_B = NBytesPut(0x0F, p_B, sizeof(U8)); /* 1byte: 0x0F:Read, 0x10:Write*/
	p_B = NBytesPut(q_ReadReg, p_B, sizeof(U32) ); /* 4byte: Read reg address*/
	p_B = NBytesPut(u_Reglen, p_B, sizeof(U8)); /* 1byte: len of register*/
	p_B = NBytesPut(0x01, p_B, sizeof(U8)); /* 1byte: number of bytes of be read*/

	STATUSMSG("gpsc_mgp_tx_read_reg: Read Register 0x%X", q_ReadReg);

	/* preparing AI2 packet and send to sensor */
	z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
	z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_CUSTOM_PACKET;
	z_PutMsgDesc.p_Buf = &u_Body[0];
	z_PutMsgDesc.w_PacketLen = (U16)(p_B - &u_Body[0]);
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;

	AI2TX("Reg Read",AI_CLAPP_SET_RF_REG_CONTENT);

	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}



/*
 ******************************************************************************
 * gpsc_mgp_tx_write_memory
 *
 * Function description:
 *
 *  Function used to send the write into a memory register AI2 to the sensor
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *  uDatatype:
 * 				01 - 8-bit
 *				02 - 16-bit
 *				04 - 32-bit
 * regaddress: register address
 *
 * Return value:
 *  void
 ******************************************************************************
 */
void gpsc_mgp_tx_write_memory(gpsc_ctrl_type* p_zGPSCControl, U32 q_WriteReg, U8 uDatatype, U8 uDataLen, U8 *uData)
{
	//gpsc_cfg_type		*p_zGPSCConfig		= p_zGPSCControl->p_zGPSCConfig;  /* Klocwork Changes */
	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;
	U8 u_Body[C_AI2_RF_READ_REG_BODY + 255];
	U8 *p_B = &u_Body[0];
	U16 i;
	/* RF Registor, Memory Read/Write msg contents */
	p_B = NBytesPut(AI_CLAPP_CUSTOM_SUB_PKT_MEM_RW, p_B, sizeof(U8)); /* 1byte: sub packet id*/
	p_B = NBytesPut(0x10, p_B, sizeof(U8)); /* 1byte: 0x0F:Read, 0x10:Write*/
	p_B = NBytesPut(q_WriteReg, p_B, sizeof(U32) ); /* 4byte: write reg address*/
	p_B = NBytesPut(uDatatype, p_B, sizeof(U8)); /* 1byte: tyep of register*/
	p_B = NBytesPut(uDataLen, p_B, sizeof(U8)); /* 1byte: number of bytes of be written*/

	for (i=0; i<(uDataLen*uDatatype);i=i+uDatatype)
	{
		if(uDatatype == 1)
		{
			p_B = NBytesPut(uData[i], p_B, sizeof(U8));
		}
		else if(uDatatype == 2)
		{
			p_B = NBytesPut(uData[i], p_B, sizeof(U16));
		}
		else if(uDatatype == 3)
		{
			p_B = NBytesPut(uData[i], p_B, sizeof(U32));
		}
		else
		{
			STATUSMSG("gpsc_mgp_tx_write_memory: Unknown data typ received");
			return;
		}
	}

	/* preparing AI2 packet and send to sensor */
	z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
	z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_CUSTOM_PACKET;
	z_PutMsgDesc.p_Buf = &u_Body[0];
	z_PutMsgDesc.w_PacketLen = (U16)(p_B - &u_Body[0]);
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;

	AI2TX("write memory",AI_CLAPP_INJ_CUSTOM_PACKET);

	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}


#ifdef WIFI_ENABLE
/*
 ******************************************************************************
 * gpsc_mgp_tx_write_reg
 *
 * Function description:
 *
 *  Function used to send the write register AI2 to the sensor
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 * WriteReg: address of register to write to
 * WriteMask: wirte maks
 * WriteValue: value to be written.
 *
 * Return value:
 *  void
 ******************************************************************************
 */
 #if 0
void gpsc_mgp_tx_write_reg(gpsc_ctrl_type* p_zGPSCControl, U32 q_WriteReg, U32 q_WriteMask, U32 q_WriteValue)
{
	gpsc_cfg_type		*p_zGPSCConfig		= p_zGPSCControl->p_zGPSCConfig;
	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;
	U8 u_Body[14];
	U8 *p_B = &u_Body[0];


	STATUSMSG("gpsc_mgp_tx_write_reg: reg:0x%X, mask:0x%X, value:0x%X",
					q_WriteReg, q_WriteMask, q_WriteValue);

	/* RF Registor, Memory Write msg contents */
	p_B = NBytesPut(q_WriteReg, p_B, sizeof(U32));
	p_B = NBytesPut(0x01, p_B, sizeof(U8)); /* 0x01:write */
	p_B = NBytesPut(q_WriteMask, p_B, sizeof(U32) );
	p_B = NBytesPut(q_WriteValue, p_B, sizeof(U32));
	p_B = NBytesPut(0x00, p_B, sizeof(U8)); /* reserved byte*/


	/* preparing AI2 packet and send to sensor */
	z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
	z_PutMsgDesc.u_PacketId = AI_CLAPP_SET_RF_REG_CONTENT;
	z_PutMsgDesc.p_Buf = &u_Body[0];
	z_PutMsgDesc.w_PacketLen = (U16)(p_B - &u_Body[0]);
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;

	AI2TX("Reg write",AI_CLAPP_SET_RF_REG_CONTENT);

	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}

#endif

/*
 ******************************************************************************
 * gpsc_mgp_tx_inject_wlan_assist_info
 *
 * Function description:
 *
 *  Form and send WLAN Assist Info for wifi position blending
 *  to Sensor.
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *  p_z_BlendedPos : pointer to blended position result
 * p_zReportPos : ponter to present position report
 *
 * Return value:
 *  None
 * Note:
 ******************************************************************************
 */
void gpsc_mgp_tx_inject_wlan_assist_info(gpsc_ctrl_type* p_zGPSCControl,gpsc_db_blended_pos_type *p_z_BlendedPos,gpsc_report_pos_type  *p_zReportPos)
{
	U8 u_Body[60]; //max no of wlan assist info length
	U8 i;
	gpsc_db_gps_meas_type*       p_zDBGpsMeas  = &p_zGPSCControl->p_zGPSCDatabase->z_DBGpsMeas;
	gpsc_meas_report_type *p_zMeasReport = &p_zDBGpsMeas->z_MeasReport;
	gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;
	InjWlanAssist z_InjWlanAssist, *p_zInjWlanAssist = &z_InjWlanAssist;

	U8 *p_B = &u_Body[0];

	/* populate AI2 as per the scale factors */
	p_zInjWlanAssist->q_FCount = p_zMeasReport->q_FCount;
	p_zInjWlanAssist->w_AssistFlag = 0x0001;  //only position Flag is enabled

	if(p_z_BlendedPos->u_2Dor3Dfix)
	 p_zInjWlanAssist->u_PositionFlag = 0x02;  //C_3D_FIX;
	else
	 p_zInjWlanAssist->u_PositionFlag = 0x01; //C_2D_FIX;

	p_zInjWlanAssist->l_Latitude = (S32)(p_z_BlendedPos->d_BlendLatitude /(DBL) C_LSB_LAT);
    p_zInjWlanAssist->l_Longitude =(S32)(p_z_BlendedPos->d_BlendLongitude / (DBL)C_LSB_LON);

	if(p_z_BlendedPos->u_2Dor3Dfix)
	 p_zInjWlanAssist->x_Altitude =  (S16)(p_z_BlendedPos->d_BlendAltitude * 2);
	else
	 p_zInjWlanAssist->x_Altitude = 0;
	p_zInjWlanAssist->w_EastUnc = (U16)(p_z_BlendedPos->d_BlendEastUnc * 10);
	p_zInjWlanAssist->w_NorthUnc =  (U16)(p_z_BlendedPos->d_BlendNorthUnc * 10);
	p_zInjWlanAssist->w_VerticalUnc = (U16)(p_z_BlendedPos->d_VerticalUnc * 2);
	/* Wlan assist info AI2 */
	p_B = NBytesPut(p_zInjWlanAssist->q_FCount, p_B, sizeof (U32) ); /* 4 byte Fcount value */

	p_B = NBytesPut( p_zInjWlanAssist->w_AssistFlag, p_B, sizeof (U16) );          /* 2 Byte Assistance Flag */

	p_B = NBytesPut( p_zInjWlanAssist->u_PositionFlag, p_B, sizeof (U8) );  /* 1 Byte Position Flag  */

	p_B = NBytesPut( p_zInjWlanAssist->l_Latitude, p_B, sizeof (S32) ); /* 4 byte signed -Lattitude */
	p_B = NBytesPut( p_zInjWlanAssist->l_Longitude, p_B, sizeof (S32) ); /* 4 byte signed -Longitude */
    p_B = NBytesPut( p_zInjWlanAssist->x_Altitude, p_B, sizeof (S16) ); /* 2 byte signed -Altitude */


	p_B = NBytesPut( p_zInjWlanAssist->w_EastUnc, p_B, sizeof (U16) ); /* 2 byte  -East Unc */
	p_B = NBytesPut( p_zInjWlanAssist->w_NorthUnc, p_B, sizeof (U16) ); /* 2 byte  -North Unc */
	p_B = NBytesPut( p_zInjWlanAssist->w_VerticalUnc, p_B, sizeof (U16) ); /* 2 byte  -Vertical Unc */

	p_B = NBytesPut( 0x000000, p_B, sizeof (S32) ); /* 4 byte Signed  -Clock Bias */

	p_B = NBytesPut( 0x0000, p_B, sizeof (S16) ); /* 2 byte  -Clock Bias Unc*/

	p_B = NBytesPut( 0x0000, p_B, sizeof (S16) ); /* 2 byte signed -East Velocity */

	p_B = NBytesPut( 0x0000, p_B, sizeof (S16) ); /* 2 byte signed -North Velocity */

	p_B = NBytesPut( 0x0000, p_B, sizeof (S16) ); /* 2 byte signed -Up Velocity */

	p_B = NBytesPut( 0x0000, p_B, sizeof (S16) ); /* 2 byte signed -Clock Velocity */

	p_B = NBytesPut( 0x0000, p_B, sizeof (U16) ); /* 2 byte  -East Velocity Unc */

    p_B = NBytesPut( 0x0000, p_B, sizeof (U16) ); /* 2 byte -North  Velocity Unc */

	p_B = NBytesPut( 0x0000, p_B, sizeof (U16) ); /* 2 byte -Up Velocity Unc */

	p_B = NBytesPut( 0x0000, p_B, sizeof (U16) ); /* 2 byte -Clock Velocity Unc */

	/*to put the reserved bytes */
	for(i=0;i<13;i++)
	p_B = NBytesPut( 0x00, p_B, sizeof (U8) );



	/* preparing AI2 packet and send to sensor */
	z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
	z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_WLAN_ASSIST_INFO;
	z_PutMsgDesc.p_Buf = &u_Body[0];
	z_PutMsgDesc.w_PacketLen = (U16)(p_B - &u_Body[0]);
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;
    AI2TX("Injest Wlan Assist Info",AI_CLAPP_INJ_WLAN_ASSIST_INFO);
	STATUSMSG("gpsc_mgp_tx_inject_wlan_assist_info: packet length = %d  FCOUNT = %d",z_PutMsgDesc.w_PacketLen,p_zInjWlanAssist->q_FCount);
	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
	if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
	  STATUSMSG("WLAN Assist AI2 Injection Fails");
}

#endif //#ifdef WIFI_ENABLE

#ifdef ENABLE_INAV_ASSIST

/**
 * \fn     convert_flt2fix_unsigned
 * \brief  Helper function to convert from float to unsigned fixed short
 *
 */
unsigned short convert_flt2fix_unsigned(double value,double MAX_VAL,unsigned int bits)
{
	double NUM_LEVELS;
	unsigned short PEAK_VAL;
	NUM_LEVELS = pow(2.0,(double)bits);
	PEAK_VAL = (unsigned short)(NUM_LEVELS -1);
	if(value>=MAX_VAL*(1-(1.0/NUM_LEVELS)))
		return(PEAK_VAL);
	else if (value<=0)
	     return ((unsigned short)0);
    else
	    return((unsigned short)(value*NUM_LEVELS/MAX_VAL));
}

/**
 * \fn     convert_flt2fix_signed
 * \brief  Helper function to convert from float to signed fixed short
 *
 */
signed short convert_flt2fix_signed(double value,double MAX_VAL,unsigned int bits)
{
	double NUM_LEVELS;
	signed short POS_PEAK_VAL,NEG_PEAK_VAL;

	NUM_LEVELS = pow(2.0,(double)bits);
	POS_PEAK_VAL = (signed short)(NUM_LEVELS/2 -1);
	NEG_PEAK_VAL = (signed short)(-NUM_LEVELS/2);
	if(value>=MAX_VAL*(1-(1.0/NUM_LEVELS)))
		return((signed short)POS_PEAK_VAL);
	else if(value<=-MAX_VAL*(1-(1.0/NUM_LEVELS)))
			return(NEG_PEAK_VAL);
    else
        return((signed short)(value*NUM_LEVELS/(2*MAX_VAL)));
}

#ifdef UNIFIED_TOOLKIT_COMM
/**
 * \fn     build_AI2_packet
 * \brief  Build the AI2 packet from the raw message
 */
int build_AI2_packet(U8 *p_AI2packetdata, U8 *p_data, U16 length, U8 msgid)
{
	int buffer_indx = 0;
	int checksum = 0;
	U8 tempbyte;
	int index;
	/* +1 for packet id, +2 forlength, +2 for escape U8 for len
	* +2 for checksum, +2 for DLE/ETS, +2 for escape U8 for checksum
	*/
	if ((length + 11) < 256)
	{
		//adding message header
		p_AI2packetdata[buffer_indx] = C_DLE;
		checksum = checksum + p_AI2packetdata[buffer_indx];
		buffer_indx = buffer_indx + 1;
		p_AI2packetdata[buffer_indx] = (U8)0;
		checksum = checksum + p_AI2packetdata[buffer_indx];
		buffer_indx = buffer_indx + 1;

		//message id
		p_AI2packetdata[buffer_indx] = msgid;
		checksum = checksum + p_AI2packetdata[buffer_indx];
		buffer_indx = buffer_indx + 1;
		if (p_AI2packetdata[buffer_indx - 1] == C_DLE)
		{
			p_AI2packetdata[buffer_indx] = C_DLE;
			buffer_indx = buffer_indx + 1;
		}

		//message length
		tempbyte = (U8)(length & 0xFF);
		p_AI2packetdata[buffer_indx] = tempbyte;
		checksum = checksum + p_AI2packetdata[buffer_indx];
		buffer_indx = buffer_indx + 1;
		if (p_AI2packetdata[buffer_indx - 1] == C_DLE)
		{
			p_AI2packetdata[buffer_indx] = C_DLE;
			buffer_indx = buffer_indx + 1;
		}
		tempbyte = (U8)(length >> 8);
		p_AI2packetdata[buffer_indx] = tempbyte;
		checksum = checksum + p_AI2packetdata[buffer_indx];
		buffer_indx = buffer_indx + 1;
		if (p_AI2packetdata[buffer_indx - 1] == C_DLE)
		{
			p_AI2packetdata[buffer_indx] = C_DLE;
			buffer_indx = buffer_indx + 1;
		}

		//packet data
		for (index = 0; index < length; index++)
		{
			p_AI2packetdata[buffer_indx] = p_data[index];
			checksum = checksum + p_AI2packetdata[buffer_indx];
			buffer_indx = buffer_indx + 1;
			if (p_AI2packetdata[buffer_indx - 1] == C_DLE)
			{
				p_AI2packetdata[buffer_indx] = C_DLE;
				buffer_indx = buffer_indx + 1;
			}
		}

		//checksum
		tempbyte = (U8)(checksum & 0xFF);
		p_AI2packetdata[buffer_indx] = tempbyte;
		buffer_indx = buffer_indx + 1;
		if (p_AI2packetdata[buffer_indx - 1] == C_DLE)
		{
			p_AI2packetdata[buffer_indx] = C_DLE;
			buffer_indx = buffer_indx + 1;
		}
		tempbyte = (U8)(checksum >> 8);
		p_AI2packetdata[buffer_indx] = tempbyte;
		buffer_indx = buffer_indx + 1;
		if (p_AI2packetdata[buffer_indx - 1] == C_DLE)
		{
			p_AI2packetdata[buffer_indx] = C_DLE;
			buffer_indx = buffer_indx + 1;
		}

		//packet tail
		p_AI2packetdata[buffer_indx] = C_DLE;
		buffer_indx = buffer_indx + 1;
		p_AI2packetdata[buffer_indx] = C_ETX;
		buffer_indx = buffer_indx + 1;

	}

	return buffer_indx;
}
#endif

/*
 ******************************************************************************
 * gpsc_mgp_inject_sensor_assistance
 *
 * Function description:
 *
 *  Function used to send the sensor engine library assistance to the Rx
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *  pSEOut: pointer to the sensor engine output structure
 *
 * Return value:
 *  void
 ******************************************************************************
 */
void gpsc_mgp_inject_sensor_assistance(gpsc_ctrl_type* p_zGPSCControl, void *pSEOut)
{
	U8 a_MsgBody[40], *p_B = a_MsgBody;
	U8 a_AI2packet[256], *p_AI2packet = a_AI2packet;
	U32 u_ai2len;
	U32 u_intVal;
	U16 u_shortVal;
	U8 u_byteval;
	EMcpfRes e_result;
	struct timeval curr_time;
	gpsc_ai2_put_msg_desc_type z_PutMsgDesc;
	SEOutput *pSensorAssist = (SEOutput *)pSEOut;

	U8 rest_flag, acc_valid, speed_valid, heading_valid, altitude_valid;
	U8 blend_type;
	U16 valid_flags;

#ifdef UNIFIED_TOOLKIT_COMM
	S32 retval;
#endif

	/**** Actual Message Block ****/
	u_byteval = AI_CLAPP_CUSTOM_SUB_PKT_INJ_SENSOR_ASSIST;  /* Sub-Packet ID: 32 */
	p_B = NBytesPut (u_byteval,p_B,sizeof(U8));

	u_intVal = pSensorAssist->Fcount; /* Fcount ---- TBD */
	p_B = NBytesPut (u_intVal,p_B,sizeof(U32));

	acc_valid = (pSensorAssist->OpFlag & 0x1);
	speed_valid = (pSensorAssist->OpFlag & 0x2) >> 1;
	heading_valid = (pSensorAssist->OpFlag & 0x4) >> 2;
	rest_flag = (pSensorAssist->OpFlag & 0x20) >> 5;
	blend_type = (pSensorAssist->OpFlag >> 6) & 0x3;

	STATUSMSG("gpsc_mgp_inject_sensor_assistance: Fcount : %d, rest Flag : %d\n",
					pSensorAssist->Fcount, rest_flag);
	STATUSMSG("gpsc_mgp_inject_sensor_assistance: Pitch : %f, Roll : %f, Yaw : %f\n",
		pSensorAssist->Pitch, pSensorAssist->Roll, pSensorAssist->RelYaw);

	altitude_valid = 0;

	valid_flags = 0;

	valid_flags = (acc_valid) |
			(speed_valid << 1) |
			(heading_valid << 2) |
			(altitude_valid << 3) |
			(blend_type << 4) |
			(rest_flag << 15);

	p_B = NBytesPut (valid_flags,p_B,sizeof(U16));

	STATUSMSG("gpsc_mgp_inject_sensor_assistance: Fcount : %d, Valid Flags : %d, rest_flag : %d ",
			pSensorAssist->Fcount, valid_flags, rest_flag);

	u_shortVal = (unsigned short)convert_flt2fix_signed(pSensorAssist->AccX,10.0,16);
	p_B = NBytesPut (u_shortVal,p_B,sizeof(U16));
	u_shortVal = (unsigned short)convert_flt2fix_signed(pSensorAssist->AccY,10.0,16);
	p_B = NBytesPut (u_shortVal,p_B,sizeof(U16));
	u_shortVal = (unsigned short)convert_flt2fix_signed(pSensorAssist->AccZ,10.0,16);
	p_B = NBytesPut (u_shortVal,p_B,sizeof(U16));

	u_shortVal = convert_flt2fix_unsigned(pSensorAssist->QmatX,10.0,16);
	p_B = NBytesPut (u_shortVal,p_B,sizeof(U16));
	u_shortVal = convert_flt2fix_unsigned(pSensorAssist->QmatY,10.0,16);
	p_B = NBytesPut (u_shortVal,p_B,sizeof(U16));
	u_shortVal = convert_flt2fix_unsigned(pSensorAssist->QmatZ,10.0,16);
	p_B = NBytesPut (u_shortVal,p_B,sizeof(U16));

	u_shortVal = (unsigned short)convert_flt2fix_signed(pSensorAssist->Speed,200,16);
	p_B = NBytesPut (u_shortVal,p_B,sizeof(U16));
	u_shortVal = convert_flt2fix_unsigned(pSensorAssist->Speed_Unc,200,16);
	p_B = NBytesPut (u_shortVal,p_B,sizeof(U16));

	u_shortVal = convert_flt2fix_unsigned(pSensorAssist->Heading,2*M_PI,16);
	p_B = NBytesPut (u_shortVal,p_B,sizeof(U16));
	u_shortVal = convert_flt2fix_unsigned(pSensorAssist->HeadingUnc,2*M_PI,16);
	p_B = NBytesPut (u_shortVal,p_B,sizeof(U16));

	u_shortVal = 0; // Altitude
	p_B = NBytesPut (u_shortVal,p_B,sizeof(U16));
	u_shortVal = 0; // Altitude unc
	p_B = NBytesPut (u_shortVal,p_B,sizeof(U16));

	/**** Additional Debug Info ****/
	u_shortVal = convert_flt2fix_unsigned(pSensorAssist->AddQmat_Pos,10.0,16);
	p_B = NBytesPut (u_shortVal,p_B,sizeof(U16));
	u_shortVal = convert_flt2fix_unsigned(pSensorAssist->AddQmat_Vel,10.0,16);
	p_B = NBytesPut (u_shortVal,p_B,sizeof(U16));

	/* TI Debug Flags */
	STATUSMSG("gpsc_mgp_inject_sensor_assistance: DebugFlags : %d", pSensorAssist->DebugFlags);
	p_B = NBytesPut (pSensorAssist->DebugFlags,p_B,sizeof(U8));

	gettimeofday(&curr_time, NULL);

	STATUSMSG("gpsc_mgp_inject_sensor_assistance (%ld:%ld): Send Message",
								curr_time.tv_sec, curr_time.tv_usec);

	z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
	z_PutMsgDesc.u_PacketId = AI_CLAPP_INJ_CUSTOM_PACKET;
	z_PutMsgDesc.p_Buf = &a_MsgBody[0];
	z_PutMsgDesc.w_PacketLen = C_PACKET_SIZE_INJ_SEDATA;
	z_PutMsgDesc.u_ForceFlag = 0;
	z_PutMsgDesc.u_MsgSend = FALSE;

	gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );

	if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
	{
		STATUSMSG("gpsc_mgp_inject_sensor_assistance: gpsc_comm_transmit Failed ");
		FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
		GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
	}

#ifdef UNIFIED_TOOLKIT_COMM
	{
		U8 a_string[64];

		u_ai2len = build_AI2_packet(p_AI2packet, &a_MsgBody[0], 40, AI_CLAPP_INJ_CUSTOM_PACKET); /* Packet ID: 251 */
		retval = sendto_clientsocket(clientsockfd, p_AI2packet, u_ai2len, 2);
		MCP_HAL_LOG_DumpBuf("gpsc_mgp_tx",
							4832,
							NAVC_MODULE_LOG,
							"Tx",
							p_AI2packet,
							u_ai2len);
		sprintf(a_string, "$SEOP,%f,%f,%f,%d",pSensorAssist->Pitch,pSensorAssist->Roll,pSensorAssist->RelYaw,((pSensorAssist->DebugFlags & 0x8)>>3));
		retval = sendto_clientsocket(clientsockfd, &a_string[0], strlen(a_string) + 1, 4);
		STATUSMSG(a_string);
	}
#endif

	gettimeofday(&curr_time, NULL);

	STATUSMSG("gpsc_mgp_inject_sensor_assistance (%ld:%ld): Send Message complete",
								curr_time.tv_sec, curr_time.tv_usec);
}
#endif
/*
 ******************************************************************************
 * gpsc_mgp_tx_write_reg
 *
 * Function description:
 *
 *  Function used to send the write register AI2 to the sensor
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 * WriteReg: address of register to write to
 * WriteMask: wirte maks
 * WriteValue: value to be written.
 *
 * Return value:
 *  void
 ******************************************************************************
 */

void gpsc_mgp_tx_write_reg(gpsc_ctrl_type* p_zGPSCControl, U32 q_WriteReg, U32 q_WriteMask, U32 q_WriteValue)
{
    gpsc_ai2_put_msg_desc_type  z_PutMsgDesc;
    U8 u_Body[14];
    U8 *p_B = &u_Body[0];

    STATUSMSG("gpsc_mgp_tx_write_reg: reg:0x%X, mask:0x%X, value:0x%X",
                                                    q_WriteReg, q_WriteMask, q_WriteValue);

     /* RF Registor, Memory Write msg contents */
    p_B = NBytesPut(q_WriteReg, p_B, sizeof(U32));
    p_B = NBytesPut(0x01, p_B, sizeof(U8)); /* 0x01:write */
    p_B = NBytesPut(q_WriteMask, p_B, sizeof(U32) );
    p_B = NBytesPut(q_WriteValue, p_B, sizeof(U32));
    p_B = NBytesPut(0x00, p_B, sizeof(U8)); /* reserved byte*/


    /* preparing AI2 packet and send to sensor */
    z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
    z_PutMsgDesc.u_PacketId = AI_CLAPP_SET_RF_REG_CONTENT;
    z_PutMsgDesc.p_Buf = &u_Body[0];
    z_PutMsgDesc.w_PacketLen = (U16)(p_B - &u_Body[0]);
    z_PutMsgDesc.u_ForceFlag = 0;
    z_PutMsgDesc.u_MsgSend = FALSE;

    AI2TX("Reg write",AI_CLAPP_SET_RF_REG_CONTENT);
    gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );
}
