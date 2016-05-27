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
 * FileName			:	gpsc_mgp_cfg.c
 *
 * Description     	:
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#include "gpsc_data.h"
#include "gpsc_mgp_rx.h"
#include "gpsc_mgp_tx.h"
#include "gpsc_comm.h"
#include "gpsc_mgp_cfg.h"
#include "gpsc_msg.h"
#include "gpsc_app_api.h"
#include "mcp_hal_fs.h"

#define MAX_GPS_CLK_UNC 30 /* 3 PPM */

/********************************************************************
 *
 * gpsc_mgp_cfg_set_evt
 *
 * Function description:
 *
 *  Select events to be reported by Sensor
 *
 *
 * Parameters:
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the pointers
 *  of many structures accessed from different modules and functions.
 *
 *  u_PeriodicAsync: 0: set periodic event configuration.
 *                   1: set async. event configuration.
 *
 *  q_OnEvents - Bit map seleting events, a 1 means on, a 0 means no change
 *
 *    for periodic event configuration:
 *        Bit 0: Time
 *        Bit 1: Position
 *        Bit 2: Position Status
 *        Bit 3: Measurement
 *        Bit 4: Measurement Status
 *        Bit 5: No New Position
 *        Bit 6: No New Measurement
 *        Bit 5-31: Reserved
 *
 *    for async. event configuration
 *        Bit 0: New Ephemeris
 *      Bit 1: New Almanac
 *        Bit 2: New Iono/UtC
 *        Bit 3: New SV Health
 *        Bit 4: New H/W Event
 *        Bit 5: Diag. Event
 *        Bit 6-31: Reserved
 *
 * Return:
 *   None
 *
 *********************************************************************/
void gpsc_mgp_cfg_set_evt
(
  gpsc_ctrl_type*  p_zGPSCControl,
  U8           u_PeriodicAsync,
  U32          q_OnEvents,
  U16         w_PeriodicReportRate,
  U8		  u_SubSecPeriodicReportRate
)
{
  U8           u_Data[13], *p_Data, *p_Data_Start;
  gpsc_event_cfg_type*    p_zEventCfg = p_zGPSCControl->p_zEventCfg;
  gpsc_ai2_put_msg_desc_type   z_PutMsgDesc;

  p_Data = &u_Data[0];
  p_Data_Start = p_Data;

  *p_Data++ = 1; /* Use serial port 1*/

//  p_zEventCfg->w_PeriodicReportRate = w_PeriodicReportRate;
//  p_zEventCfg->u_SubSecPeriodicReportRate = u_SubSecPeriodicReportRate;

  if (u_PeriodicAsync == 0) /* set periodic events */
  {
    p_zEventCfg->q_PeriodicReportFlags = q_OnEvents;
    p_Data = NBytesPut( p_zEventCfg->q_PeriodicReportFlags,
                        p_Data,
                        sizeof( U32 ) );

    p_Data = NBytesPut( p_zEventCfg->q_AsyncEventFlags,
                        p_Data,
                        sizeof( U32 ) );

  }
  else /* set async events */
  {
    p_Data = NBytesPut( p_zEventCfg->q_PeriodicReportFlags,
                        p_Data,
                        sizeof( U32 ) );


    p_zEventCfg->q_AsyncEventFlags = q_OnEvents;
    p_Data = NBytesPut( p_zEventCfg->q_AsyncEventFlags,
                        p_Data,
                        sizeof( U32 ) );



  }

  /* check for sub second periodic report enabled (0.5 sec update rate) */
  if(FALSE == w_PeriodicReportRate)
  {
	/* check for sub second periodic report (0.5 sec update rate)
	   if u_SubSecPeriodicReportRate = 0, means 1 second update rate */
	if(FALSE == u_SubSecPeriodicReportRate)
	{
	  w_PeriodicReportRate = 1;
	  p_Data = NBytesPut(w_PeriodicReportRate,p_Data,sizeof(U16));
	  p_Data = NBytesPut(u_SubSecPeriodicReportRate,p_Data,sizeof(U8));
	}
	/* if u_SubSecPeriodicReportRate = 0, means 1 second update rate */
	else /* sub second periodic report enabled (0.5 sec update rate) */
	{
	  p_Data = NBytesPut(w_PeriodicReportRate,p_Data,sizeof(U16));

	  /* NL5500 - at present sub second periodic report is supported only for 0.5 second */
	  u_SubSecPeriodicReportRate = 5;
	  p_Data = NBytesPut(u_SubSecPeriodicReportRate,p_Data,sizeof(U8));
	}
  }
  else /* sub second periodic report disabled */
  {
	p_Data = NBytesPut(w_PeriodicReportRate,p_Data,sizeof(U16));

	/* NL5500 - if w_PeriodicReportRate in range of 1 to 65535,
	   sub second periodic report is disabled by default,
	   if sub second periodic report desired, then w_PeriodicReportRate must
	   be set to zero - 0.
	*/
	if(u_SubSecPeriodicReportRate)
	{
	u_SubSecPeriodicReportRate = 0;
	}
	p_Data = NBytesPut(u_SubSecPeriodicReportRate,p_Data,sizeof(U8));
  }

  /*Byte 13 is reserved */
  p_Data = NBytesPut( 0, p_Data, sizeof(U8));

  z_PutMsgDesc.u_AckType = C_AI2_NO_ACK;
  z_PutMsgDesc.u_PacketId = AI_INJ_EVENT_CFG;
  z_PutMsgDesc.p_Buf = p_Data_Start;
  z_PutMsgDesc.w_PacketLen = 13;
  z_PutMsgDesc.u_MsgSend = FALSE;

  AI2TX("Event Config",AI_INJ_EVENT_CFG);
  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );

}

/*
 ******************************************************************************
 * gpsc_mgp_cfg_evt_rep_init
 *
 * Function description:
 *
 * This function initializes the default Sensor event report setting. Used at
 * startup and everytime a collect has ended.
 *
 * Parameters:
 *  p_Field - Pointer to an Ai2Field structure.
 *
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the
 *  pointers of many structures accessed from different modules and
 *  functions.
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/
void gpsc_mgp_cfg_evt_rep_init (gpsc_ctrl_type *p_zGPSCControl)
{
	gpsc_event_cfg_type*    p_zEventCfg = p_zGPSCControl->p_zEventCfg;

/* for GPS5350, periodic report rate is passed as event configuration (4th parameter),
     by default it is initialized to 1,
	   for NL5500sub periodic report rate is passed as 5th parameter by default it is
	   initialized to 0 means periodic report for 1 second update rate is enabled.
 */
  
  /* Clear periodic events flag */
  gpsc_mgp_cfg_set_evt( p_zGPSCControl, 0,0,p_zEventCfg->w_PeriodicReportRate,p_zEventCfg->u_SubSecPeriodicReportRate);
  gpsc_mgp_cfg_set_evt( p_zGPSCControl, 1,0,p_zEventCfg->w_PeriodicReportRate,p_zEventCfg->u_SubSecPeriodicReportRate);

}

/*
 ******************************************************************************
 * gpsc_mgp_maximize_pdop
 *
 * Function description:
 *
 * this function is used to maximize the 2D and 3D PDOPS
 *
 * Parameters:
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the
 *  pointers of many structures accessed from different modules and
 *  functions.
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/

T_GPSC_result gpsc_mgp_maximize_pdop	(  gpsc_ctrl_type*  p_zGPSCControl)
{
	gpsc_cfg_type*   p_zGPSCConfig  = p_zGPSCControl->p_zGPSCConfig;
	gpsc_rcvr_cfg_type*  p_zRcvrConfig = p_zGPSCControl->p_zRcvrConfig;
	McpU8 temp_Pdop, temp_2DPdop;
	McpU8 res = GPSC_SUCCESS;

	temp_Pdop = p_zGPSCConfig->pdop_mask;
	temp_2DPdop = p_zGPSCConfig->alt_2d_pdop;

	p_zGPSCConfig->pdop_mask = p_zGPSCConfig->pdop_mask_time_out;
	p_zGPSCConfig->alt_2d_pdop = p_zGPSCConfig->pdop_mask_time_out;

	STATUSMSG("gpsc_mgp_maximize_pdop: Injecting PDOP:%d", p_zRcvrConfig->u_PdopMask);

	gpsc_mgp_rcvr_cfg( p_zGPSCControl);
	if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
	{
		STATUSMSG("gpsc_mgp_maximize_pdop: Comm Transmit Error");
		res = GPSC_FAIL;
	}
	p_zGPSCConfig->pdop_mask = temp_Pdop;
	p_zGPSCConfig->alt_2d_pdop= temp_2DPdop;

	return res;
}

/*
 ******************************************************************************
 * gpsc_mgp_rcvr_cfg
 *
 * Function description:
 *
 * This function accepts a copy of receiver configuration from Sensor and may
 * decide to alter it based on gpsc_cfg_type information.
 *
 * Parameters:
 *  p_zGPSCControl: pointer to the gpsc_ctrl_type structure that keeps the
 *  pointers of many structures accessed from different modules and
 *  functions.
 *
 * Return value:
 *  None
 *
 ******************************************************************************
*/

void gpsc_mgp_rcvr_cfg
(
  gpsc_ctrl_type*  p_zGPSCControl
)
{
  gpsc_ai2_put_msg_desc_type   z_PutMsgDesc;
  U8           u_DataBuff[24];
  U8 *p_DataBuff = u_DataBuff;


  gpsc_rcvr_cfg_type*  p_zRcvrConfig = p_zGPSCControl->p_zRcvrConfig;
  gpsc_cfg_type*   p_zGPSCConfig  = p_zGPSCControl->p_zGPSCConfig;
  gpsc_sess_cfg_type*	p_zSessConfig = p_zGPSCControl->p_zSessConfig;

  gpsc_sys_handlers*    p_GPSCSysHandlers = NULL;
  McpU8 uAidingFile[C_MAX_AIDING_FILENAME_LEN+20] = {'\0'};
  EMcpfRes  eRetVal = RES_OK;
  McpS32    fd = 0;

  p_zRcvrConfig->u_SlaveModeEnabled = 0;
  p_zRcvrConfig->u_DgpsCorrAllowed = TRUE;
  p_zRcvrConfig->u_DgpsMaxInterval = (U8)PE_DGPS_MAX_INT;

/*  if ( p_zSessConfig->u_PosCompEntity == C_SMLC_COMPUTED )
  {
    p_zRcvrConfig->u_ServerMode = AI_SERVER_SMART;
  }

  else if ( p_zSessConfig->u_PosCompEntity == C_MS_COMPUTED )
*/
  {
    /*GPS5350 always define ServerMode as AI_SERVER_SIMPLE, this will help handling multiple
	session with different PosCompEntity. */
    p_zRcvrConfig->u_ServerMode = AI_SERVER_SIMPLE;
  }

  p_zRcvrConfig->u_PatternMatchType = 0; /* not yet used */

  p_zRcvrConfig->u_FdicMode = TRUE;

  p_zRcvrConfig->u_AltHoldMode = (U8)p_zGPSCConfig->altitude_hold_mode;


  p_GPSCSysHandlers = p_zGPSCControl->p_zSysHandlers;
  MCP_HAL_STRING_StrCpy (uAidingFile,(McpU8*)p_GPSCSysHandlers->uAidingPath);
  MCP_HAL_STRING_StrCat (uAidingFile,C_STR_AID_TCXO_FILE);

  eRetVal = mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf,
                           (McpU8 *) uAidingFile,
                            MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_RDONLY,
                            &fd);
  if(RES_OK == eRetVal)
   {
       gpsc_db_gps_inj_clk_type z_DBGpsInjClock;
       McpU32 uNumRead = 0;

       uNumRead = mcpf_file_read(p_zGPSCControl->p_zSysHandlers->hMcpf,
                                  fd,
                                  ( void* ) &z_DBGpsInjClock,
                                  sizeof( gpsc_db_gps_inj_clk_type));

       mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, fd);

        if (sizeof( gpsc_db_gps_inj_clk_type ) != uNumRead)
        {
          /**/
          STATUSMSG(" gpsc_mgp_rcvr_cfg: C_STR_AID_TCXO_FILE Reading FAILED..>!!");
          p_zRcvrConfig->w_MaxClkFrqUnc = (U16)(MAX_GPS_CLK_UNC * LIGHT_SEC * 1e-6 * 0.1f);
        }
        else
        {
          p_zRcvrConfig->w_MaxClkFrqUnc = (U16)(p_zGPSCConfig->max_gps_clk_unc * LIGHT_SEC * 1e-6);
        }
    }
   else
    {
        /**/
        STATUSMSG(" gpsc_mgp_rcvr_cfg: C_STR_AID_TCXO_FILE opening FAILED..>!!");
        p_zRcvrConfig->w_MaxClkFrqUnc = (U16)(MAX_GPS_CLK_UNC * LIGHT_SEC * 1e-6 * 0.1f);
    }


  p_zRcvrConfig->w_MaxClkAcc = p_zGPSCConfig->max_clock_acceleration;

  p_zRcvrConfig->w_MaxUserAcc = p_zGPSCConfig->max_user_acceleration;

  p_zRcvrConfig->u_ElevMask = (U8) p_zGPSCConfig->elevation_mask;

  p_zRcvrConfig->u_PdopMask = p_zGPSCConfig->pdop_mask;

#ifdef ENABLE_INAV_ASSIST
  /* TBD: Add specific flag to GPSCConfig or some other means of conveying
   * sensor enabling
   */
  p_zRcvrConfig->u_FeatureControl = (U8)((p_zGPSCConfig->data_wipe)|((p_zGPSCConfig->kalman_feat_control)<<1)
								|((p_zGPSCConfig->dll)<<2)|((p_zGPSCConfig->carrier_phase)<<3)
                                                                |(1 << 4)) ; /* Enable Sensor PE Data */
#else
  p_zRcvrConfig->u_FeatureControl = (U8)((p_zGPSCConfig->data_wipe)|((p_zGPSCConfig->kalman_feat_control)<<1)
								|((p_zGPSCConfig->dll)<<2)|((p_zGPSCConfig->carrier_phase)<<3));
#endif

  p_zRcvrConfig->u_2DPdopMask = p_zGPSCConfig->alt_2d_pdop;


  p_zRcvrConfig->u_ExtEventEdge = (U8) p_zGPSCConfig->timestamp_edge;

  p_zRcvrConfig->u_Test0 = (U8)(p_zGPSCControl->p_zGPSCParms->b_TestByte_Defined[0] ?
    p_zGPSCControl->p_zGPSCParms->u_TestByte[0] : 0);
  p_zRcvrConfig->u_Test1 = (U8)(p_zGPSCControl->p_zGPSCParms->b_TestByte_Defined[1] ?
    p_zGPSCControl->p_zGPSCParms->u_TestByte[1] : 0);
  p_zRcvrConfig->u_Test2 = (U8)(p_zGPSCControl->p_zGPSCParms->b_TestByte_Defined[2] ?
    p_zGPSCControl->p_zGPSCParms->u_TestByte[2] : 0);

  p_zRcvrConfig->u_DiagSetting = p_zSessConfig->u_SensorDiagSettings;

  /* to set the receiver op mode 0-Accuracy 1-Combo(Acc and speed) */
  p_zRcvrConfig->u_DiagSetting_1 = p_zGPSCConfig->rx_opmode;

  p_zRcvrConfig->u_Rsvd = 0;

  *p_DataBuff++ = p_zRcvrConfig->u_SlaveModeEnabled;
  *p_DataBuff++ = p_zRcvrConfig->u_DgpsCorrAllowed;
  *p_DataBuff++ = p_zRcvrConfig->u_DgpsMaxInterval;
  *p_DataBuff++ = p_zRcvrConfig->u_ServerMode;
  *p_DataBuff++ = p_zRcvrConfig->u_PatternMatchType;
  *p_DataBuff++ = p_zRcvrConfig->u_FdicMode;
  *p_DataBuff++ = p_zRcvrConfig->u_AltHoldMode;

  p_DataBuff = NBytesPut( p_zRcvrConfig->w_MaxClkFrqUnc,
                          p_DataBuff,
                          sizeof( U16 ) );
  p_DataBuff = NBytesPut( p_zRcvrConfig->w_MaxClkAcc,
                          p_DataBuff,
                          sizeof( U16 ) );
  p_DataBuff = NBytesPut( p_zRcvrConfig->w_MaxUserAcc,
                          p_DataBuff,
                          sizeof( U16 ) );

  *p_DataBuff++ = p_zRcvrConfig->u_ElevMask;
  *p_DataBuff++ = p_zRcvrConfig->u_PdopMask;
  *p_DataBuff++ = p_zRcvrConfig->u_FeatureControl;

  *p_DataBuff++ = p_zRcvrConfig->u_2DPdopMask;
  *p_DataBuff++ = p_zRcvrConfig->u_ExtEventEdge;
  *p_DataBuff++ = p_zRcvrConfig->u_Test0;
  *p_DataBuff++ = p_zRcvrConfig->u_Test1;
  *p_DataBuff++ = p_zRcvrConfig->u_Test2;
  *p_DataBuff++ = p_zRcvrConfig->u_DiagSetting;
  *p_DataBuff++ = p_zRcvrConfig->u_DiagSetting_1;
  *p_DataBuff   = p_zRcvrConfig->u_Rsvd;


  z_PutMsgDesc.u_AckType = C_AI2_ACK_REQ;
  z_PutMsgDesc.u_PacketId = AI_INJ_REC_CFG;
  z_PutMsgDesc.p_Buf = u_DataBuff;
  z_PutMsgDesc.w_PacketLen = 24;
  z_PutMsgDesc.u_MsgSend = FALSE;
  AI2TX("Receiver Config",AI_INJ_REC_CFG);
  gpsc_comm_write(p_zGPSCControl, &z_PutMsgDesc );

   STATUSMSG("gpsc_mgp_rcvr_cfg: RcvrConfig injected to Sensor" );
}

