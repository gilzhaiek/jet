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
 * FileName			:	gpsc_mgp.c
 *
 * Description     	:
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */


#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>
#include <errno.h>

#include "gpsc_types.h"
#include "gpsc_sap.h"
#include "gpsc_database.h"
#include "gpsc_init.h"
#include "gpsc_mgp.h"
#include "gpsc_mgp_cfg.h"
#include "gpsc_mgp_rx.h"
#include "gpsc_mgp_tx.h"
#include "gpsc_msg.h"
#include "gpsc_sess.h"
#include "gpsc_sequence.h"
#include "gpsc_state.h"
#include "gpsc_timer_api.h"
#include "gpsc_comm.h"
#include "gpsc_drv_api.h"
#include "gpsc_app_api.h"
#include "nt_adapt.h"
#include "navc_errHandler.h"
#include "navc_inavc_if.h"

#include <utils/Log.h>
#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG "NAVD.GPSC_MGP"    // our identification for logcat (adb logcat NAVD.GPSC_MGP:V *:S)

#define POSITION_DATA_BUFF_EN 1

extern T_GPSC_result gpsc_calib_timepulse_refclk_sensor_req (U8 enable_disable_calib);

static U8 mgp_ver_ck
(
   gpsc_ctrl_type*  p_zGPSCControl,
   Ai2Field*    p_zAi2Field
);

extern U8 resetFlag;
extern void gpsc_app_fatal_err_ind (McpU32 uSesId, McpU32 errCode);

static int recon_data_fd = -1;
static int recon_control_fd = -1;

int open_recon_port (int type)
{
   char* pipe = 0;

   if (type == RECON_PIPE_TYPE_DATA) pipe = RECON_DATA_PIPE;
   else if (type == RECON_PIPE_TYPE_CONTROL) pipe = RECON_CONTROL_PIPE;
   else
   {
      ALOGE("+++ %s: Invalid Pipe Type %d +++\n", __FUNCTION__, type);
      return -1;
   }

   unlink(pipe);
   int ret = mkfifo(pipe, 0666);

   if (ret == 0)
   {
      char buf[100];
      sprintf(buf, "chmod 666 %s", pipe);
      system(buf);

      sprintf(buf, "chown system.system %s", pipe);
      system(buf);

      ALOGI("+++ %s: RECON Port [%s] successfully created +++\n", __FUNCTION__, pipe);
   } 
   else
      ALOGE("+++ %s: Could not create RECON Port [%s]. Error %d +++\n", __FUNCTION__, pipe, -errno); 

   return ret;
}

void close_recon_port (int type)
{
   int* pfd = 0;
   char* pipe = 0;

   if (type == RECON_PIPE_TYPE_DATA)
   { 
      pipe = RECON_DATA_PIPE;
      pfd  = &recon_data_fd;
   }
   else if (type == RECON_PIPE_TYPE_CONTROL) 
   {
      pipe = RECON_CONTROL_PIPE;
      pfd  = &recon_control_fd;
   }
   else
   {
      ALOGE("+++ %s: Invalid Pipe Type %d +++\n", __FUNCTION__, type);
      return;
   }

   close(*pfd);
   *pfd = -1;

   unlink(pipe);

   ALOGI("+++ %s: RECON Port [%s] Closed +++\n", __FUNCTION__, pipe);
}

/*===========================================================================

                DEFINITIONS AND DECLARATIONS FOR MODULE

This section contains definitions for constants, macros, types, variables
and other items needed by this module.

===========================================================================*/


/*===========================================================================

FUNCTION
  GPSC_MGP_PROCESS_MSG

DESCRIPTION
  This function processes AI2 message coming from the Sensor

===========================================================================*/

void gpsc_mgp_process_msg
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*        p_zAi2Field
)
{
  gpsc_db_type*                 p_zGPSCDatabase    = p_zGPSCControl->p_zGPSCDatabase;
  gpsc_state_type*              p_GPSCState        = p_zGPSCControl->p_zGPSCState;
  gpsc_cfg_type*                p_zGPSCConfig	   = p_zGPSCControl->p_zGPSCConfig;
  T_GPSC_comm_config*	        p_comm_config	   = &(p_zGPSCConfig->comm_config);
  gpsc_smlc_assist_type*        p_zSmlcAssist      = p_zGPSCControl->p_zSmlcAssist;
  gpsc_custom_struct*		     p_zCustom          = p_zGPSCControl->p_zCustomStruct;
  gpsc_sess_cfg_type*	        p_zSessConfig      = p_zGPSCControl->p_zSessConfig;
  gpsc_sess_specific_cfg_type*  p_zSessSpecificCfg = NULL;

  U8* p_B = p_zAi2Field->p_B;
  U8  p_SubPacket = 0;


  switch (p_zAi2Field->u_Id)
  {
    case AI_REP_PING:
		ALOGI("+++ %s: AI_REP_PING +++\n", __FUNCTION__);
      break;

    case  AI_REP_BLF_STATUS:
       ALOGI("+++ %s: AI_REP_BLF_STATUS +++\n", __FUNCTION__);
       mgp_send_blf_sts(p_zGPSCControl, p_zAi2Field);
 
       break;

	 case AI_REP_BLF_DUMP:
	 {
		U8 filterMap = 0;
  		U8 * p_Buff = p_zAi2Field->p_B;
		U16 readCount = 0;
  		Ai2Field tempAi2Field;
	  	Ai2Field* p_tempAi2Field = &tempAi2Field;

	  	ALOGI("+++ %s: AI_REP_BLF_DUMP +++\n", __FUNCTION__);

		/* For now only position buffer dump report is supported */
		while (readCount < p_zAi2Field->w_Length)
		{
  			filterMap = (U8)NBytesGet(&p_Buff, sizeof(U8));
			readCount += sizeof(U8);

			/* Check the filter bit map and process only position data */
			if (filterMap & POSITION_DATA_BUFF_EN)
			{
				p_tempAi2Field->u_Id = AI_REP_POS;
				p_tempAi2Field->w_Length = (U16)NBytesGet(&p_Buff, sizeof(U16));
				readCount += sizeof(U16);
				p_tempAi2Field->p_B = (U8*)p_Buff;

      				if (gpsc_db_update_pos( p_tempAi2Field, p_zGPSCDatabase ) ) /* inside this
  	    			function, only position 5 minutes younger than what's in NVS gets saved */
      				{
         				gpsc_session_status_type* p_zSessionStatus  = &p_GPSCState->z_SessionStatus;
	          			if(p_zSessionStatus->u_BuildWishlistLater)
			  		{
						p_zSessionStatus->u_BuildWishlistLater=0;

			 			/* this session sequence should go to get assistance sequence */
			 			gpsc_session_sequence(p_zGPSCControl,E_GPSC_SEQ_SESSION_GET_ASSISTANCE);
			  		}
			  		p_zCustom->custom_position_available_flag = 1;
        				gpsc_mgp_rx_proc_pos( p_zGPSCControl, 1 );

	    			}
			}

			/* Point to the next result */
			p_Buff += p_tempAi2Field->w_Length;
			readCount += p_tempAi2Field->w_Length;
		}
	  }
	  break;

	  case AI_CLAPP_REP_VERSION:

	  ALOGI("+++ %s: AI_CLAPP_REP_VERSION +++\n", __FUNCTION__);

	  /* AI_CLAPP_REP_VERSION received, stop timer started in download complete seq */
	  gpsc_timer_stop(p_zGPSCControl, C_TIMER_SEQUENCE);

	  if ( p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq ==  E_GPSC_SEQ_READY_DOWNLOAD_COMPLETE)
     {
		   /* check ROM version */
		   if ( mgp_ver_ck(p_zGPSCControl, p_zAi2Field) )
		   {
			     p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_READY_INJECT_INIT_CONFIG;
			     gpsc_ready_sequence(p_zGPSCControl, E_GPSC_SEQ_READY_INJECT_INIT_CONFIG);
		   }
		   else
		   {
			     gpsc_os_fatal_error_ind(GPSC_INCOMPATIBLE_ROM_VERSION_FAIL);
			     GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);

		   }
	  }
	  else if ( (p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_READY_SKIP_DOWNLOAD) ||
	  		   (p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_READY_READ_INJECT_RECORD) )
	  {
	  	   
	  }
	  else
	  {
          if (resetFlag ==1)
          {
             resetFlag =0;
			    p_zGPSCControl->p_zGpscSm->e_GpscCurrState = E_GPSC_EVENT_SHUTDOWN;
			    GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_RESET);
          }
	  }

	  mgp_custom_ver_resp(p_zGPSCControl, p_zAi2Field);

	  break;

    case AI_REP_CLOCK:
    {
            ALOGI("+++ %s: AI_REP_CLOCK +++\n", __FUNCTION__);

            // update clock database everytime a clock report is received
            gpsc_db_update_clock( p_zAi2Field, p_zGPSCDatabase );

	    // wait here till get the clk report requested while injecting the time
	    if ( p_GPSCState->u_WaitForClkReport == TRUE)
	    {
	  	    p_GPSCState->u_WaitForClkReport = FALSE;
		    if ((p_zCustom->loc_req_timer_first_time_check_flag == FALSE) &&
			   (p_zGPSCConfig->systemtime_or_injectedtime == GPSC_CFG_INJECTED_TIME))
		    {
			   p_GPSCState->z_nvm_inj_state = NVM_INJ_EPH;
			   p_zCustom->loc_req_timer_first_time_check_flag = TRUE;
		    }

		    if (p_zGPSCDatabase->z_DBGpsClock.w_GpsWeek == C_GPS_WEEK_UNKNOWN)
		    {
	          // set the time avaibility flag
			    p_zSmlcAssist->w_AvailabilityFlags |= C_REF_GPSTIME_AVAIL;

			    // reinject the time
			    p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_REQUEST_TIME;
			    gpsc_session_sequence(p_zGPSCControl, E_GPSC_SEQ_SESSION_REQUEST_TIME);
 		    }
		    else
		    {
			    // the receiver has picked up the time, continue to inject the assistance
		       p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE;
		       gpsc_session_sequence(p_zGPSCControl, E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE);
	       }
	    }

	    // Request Assistance data when time is available and it is not requested
	    if (p_GPSCState->u_ClkRptforWlPending)
	    {
	  	    p_GPSCState->u_ClkRptforWlPending = FALSE;
		    p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_GET_ASSISTANCE;
		    gpsc_session_sequence(p_zGPSCControl, E_GPSC_SEQ_SESSION_GET_ASSISTANCE);
	    }

	    if (p_zGPSCControl->p_zSmlcAssist->u_RequestedAssistTime)
	    {
		    p_zGPSCControl->p_zSmlcAssist->u_RequestedAssistTime = FALSE;
		    gpsc_app_assist_time_ind();
	    }

	    if(p_zGPSCControl ->u_reqAssistPending == TRUE)
	    {
	    	/* new clock report available. since req for assistance is pending request the assistance */
	    	/* RXN assistance timer has expired. Request for fresh assistance from PGPS*/
	    	ALOGI("C_TIMER_RXN_EPHE_VALID Timer expired: request for assistance");
	    	p_zSmlcAssist->u_NumAssistAttempt = 0;
	    	gpsc_request_assistance(p_zGPSCControl);
                /* Clear the reqAssistPending flag since we have sent an
                   assistance request */
	    	p_zGPSCControl->u_reqAssistPending = FALSE;
	    }
    }
    break;

    case AI_REP_POS:
      ALOGI("+++ %s: AI_REP_POS +++\n", __FUNCTION__);

      if(!p_zGPSCControl->p_zGPSCDatabase->z_DBGpsClock.u_Valid)
  	    gpsc_mgp_tx_req(p_zGPSCControl, AI_REQ_CLOCK_REP, 0);

      /* update position database every time a position report is received */
      if ( p_zGPSCControl->p_zGPSCDatabase->z_DBGpsClock.u_Valid && gpsc_db_update_pos( p_zAi2Field, p_zGPSCDatabase ) )
      {
         /* inside this function, only position 5 minutes younger than what's in NVS gets saved */
         gpsc_session_status_type* p_zSessionStatus  = &p_GPSCState->z_SessionStatus;
         if (p_zSessionStatus->u_BuildWishlistLater)
		   {
			   p_zSessionStatus->u_BuildWishlistLater=0;

			   /* this session sequence should go to get assistance sequence */
			   gpsc_session_sequence(p_zGPSCControl, E_GPSC_SEQ_SESSION_GET_ASSISTANCE);
		    }

		    p_zCustom->custom_position_available_flag = 1;
          gpsc_mgp_rx_proc_pos( p_zGPSCControl, 1);
	    }
      break;


	case AI_REP_POS_EXT:
	 // ALOGI("+++ %s: AI_REP_POS_EXT +++\n", __FUNCTION__);

	  /* update position database everytime a extended position report is received
        inside this function, only position 5 minutes younger than what's in NVS gets saved */
	  if ( gpsc_db_update_ext_pos( p_zAi2Field, p_zGPSCDatabase ) ) 
     {
		  gpsc_session_status_type* p_zSessionStatus  = &p_GPSCState->z_SessionStatus;

        if (p_zSessionStatus->u_BuildWishlistLater)
		  {
			 p_zSessionStatus->u_BuildWishlistLater=0;

			 /* this session sequence should go to get assistance sequence */
			 gpsc_session_sequence(p_zGPSCControl, E_GPSC_SEQ_SESSION_GET_ASSISTANCE);

		  }

        gpsc_mgp_rx_proc_pos( p_zGPSCControl, 1); 
	  }
	  break;

    case AI_REP_ALM:
	   ALOGI("+++ %s: Almanac Report Received. SV ID: %d +++\n", __FUNCTION__, *(p_zAi2Field->p_B) );

      /* update almanac database everytime an almanac report is received */
      gpsc_db_update_alm( p_zAi2Field, p_zGPSCDatabase );
      break;

    // This get sent by AI2 chip as response to AI_REQ_EPHEM Command Packet, which
    // is sent by this cumbersome Core everytime asynchronous AI_EVENT_NEW_EPHEM is received
    case AI_REP_EPHEM:
	   ALOGI("+++ %s: Ephemeris Report Received. SV ID: %d +++\n", __FUNCTION__, *(p_zAi2Field->p_B) );

      /* update ephemeris database everytime ephemeris report is received. */
      gpsc_db_update_eph(p_zAi2Field, p_zGPSCDatabase );
      break;

    case AI_REP_IONO:
      ALOGI("+++ %s: AI_REP_IONO +++\n", __FUNCTION__);
	   
      /* update IONO database everytime an IONO report is received */
      gpsc_db_update_iono( p_zAi2Field, p_zGPSCDatabase );
      break;

    case AI_REP_UTC:
	   ALOGI("+++ %s: AI_REP_UTC +++\n", __FUNCTION__);

		/* update UTC database everytime an IONO report is received */
	   gpsc_db_update_utc( p_zAi2Field, p_zGPSCDatabase );
      break;

    case AI_REP_SV_HEALTH:
	   ALOGI("+++ %s: AI_REP_SV_HEALTH +++\n", __FUNCTION__);

      /* update Health basebase everytime an SV Health report is received */
      gpsc_db_update_health( p_zAi2Field, p_zGPSCDatabase );
      break;

    case AI_REP_SV_DIR:
      ALOGI("+++ %s: AI_REP_SV_DIR +++\n", __FUNCTION__);

	  /* Update SV direction database everytime an SV direction report is
         received.Note we don't save SV direction to NVS */
      gpsc_db_update_sv_dir( p_zAi2Field, p_zGPSCDatabase );


#ifndef NO_TRACKING_SESSIONS
      if (p_GPSCState->u_GPSCState==E_GPSC_STATE_ACTIVE &&
          p_zGPSCControl->p_zGPSCState->z_SessionStatus.u_TrackSubState==TRKSTATE_WAIT_SV_DIR)
      {
          p_zGPSCControl->p_zGPSCState->z_SessionStatus.u_TrackSubState =TRKSTATE_SV_DIR_RDY; 
      }
#endif
	  break;

    case AI_REP_MEAS:
      ALOGI("+++ %s: AI_REP_MEAS +++\n", __FUNCTION__);

      /* Update SV measurement database */
      gpsc_db_update_sv_meas( p_zAi2Field, p_zGPSCDatabase );
	   gpsc_process_eph_validate(p_zGPSCControl);
      gpsc_mgp_rx_proc_meas( p_zGPSCControl, 1 );

      /* more than 17 Eph have been injected and TIME/POS info not available */
		/*  inject EPH for SVs that are visiable in measurement report */
     gpsc_mgp_tx_visible_sv_eph(p_zGPSCControl);

		if (p_zGPSCControl->p_zGPSCState->injectSvdirectionColdstart == TRUE) 
      {
          gpsc_update_sv_visibility_Almanac(p_zGPSCControl);
      }

      break;

    case AI_REP_MEAS_STAT:
      ALOGI("+++ %s: AI_REP_MEAS_STAT +++\n", __FUNCTION__);
      gpsc_db_update_sv_meas_stat(p_zAi2Field, p_zGPSCDatabase);

      break;

    case AI_REP_DIAG_STR:

	   // display Sensor ASCII diagnostic messages. NOTE: This is just for debugging, should always be
      // commented out in production. However, do NOT comment whole case as we still wan't to see what messages
      // are not handled at bottom of this giant switch
      
  	   //ALOGI("+++ %s: AI_REP_DIAG_STR [%s] +++\n", __FUNCTION__, (char*)p_zAi2Field->p_B);

      break; 

    case AI_REP_ASYNC_REC_EVENT:
     // ALOGI("+++ %s: AI_REP_ASYNC_REC_EVENT. Event Type: [%d] +++\n", __FUNCTION__,  *p_zAi2Field->p_B);
      gpsc_mgp_rx_proc_asyn_evt( p_zGPSCControl, p_zAi2Field );
      break;

 /*   case AI_REP_REC_CFG:
      ALOGI("+++ %s: AI_REP_REC_CFG +++\n", __FUNCTION__);
      break; */

	 case AI_REP_CUSTOM_PACKET:
	   p_SubPacket = *p_B++;
	   switch(p_SubPacket)
	   {
		case AI_REP_CALIB_CONTROL:
         ALOGI("+++ %s: Custom Packet: Calibration Control Status +++\n", __FUNCTION__);
	  		gpsc_mgp_rx_calibcontrol_report(p_zGPSCControl,p_zAi2Field);

      	break;

      case	  AI_CLAPP_CUSTOM_SUB_PKT_REP_MEM_CONTENTS:
		   ALOGI("+++ %s: Custom Packet: RF Reg Memory Contents +++\n", __FUNCTION__);
		   gpsc_mgp_rx_reg_read_response(p_zGPSCControl, p_zAi2Field); 
		   break;
			
      case AI_REP_MOTION_MASK_STATUS:
      	ALOGI("+++ %s: Custom Packet: AI_REP_MOTION_MASK_STATUS +++\n", __FUNCTION__);

      	break;
		  
      case AI_REP_MOTION_MASK_SETTINGS:
       	  ALOGI("+++ %s: Custom Packet: AI_REP_MOTION_MASK_SETTINGS +++\n", __FUNCTION__);
		     gpsc_mgp_rx_motion_mask_setting(p_zGPSCControl,p_zAi2Field);

      	  break;
		  
      case AI_REP_MEAS_EXT:
          //ALOGI("+++ %s: Custom Packet: AI_REP_MEAS_EXT +++\n", __FUNCTION__);

			 /* Request for SV direction */
			 if (gpsc_get_gf_report_config(p_zSessConfig, p_zSessSpecificCfg) != 0) /* normal session */
			 {
					if (p_zGPSCConfig->apm_control == 0)
               {
			         gpsc_sess_get_sv_dir(p_zGPSCControl);
               }
			  }
			  else 
           { 
              /* Geofence Session */
				  if (p_GPSCState->u_GeofenceAPMEnabled == 0)
				     gpsc_sess_get_sv_dir(p_zGPSCControl);
			  }

            gpsc_db_gps_clk_type* p_DBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;

            // Cache the old valid flag
            McpU8 valid = p_DBGpsClock->u_Valid;

           /* Update SV measurement database */
	  		 gpsc_db_update_sv_ext_meas( p_zAi2Field, p_zGPSCDatabase );
          gpsc_db_update_clock( p_zAi2Field, p_zGPSCDatabase );
			 gpsc_process_eph_validate(p_zGPSCControl);
			 gpsc_mgp_rx_proc_meas( p_zGPSCControl, 1);

            // See if we changed from an invalid clock to valid clock. We want to know
            // when we first get a valid clock but make sure that we've had a couple invalid
            // ones first.
            if (valid == FALSE && p_DBGpsClock->u_Valid == TRUE && p_DBGpsClock->u_NumInvalids > 0) {
                ALOGI("Received a valid clock from satellites. (%d)", p_DBGpsClock->u_NumInvalids);

                // Reset number of assistance attempts.
                p_zSmlcAssist->u_NumAssistAttempt = 0;

                // Check to see if the 4 minute C_TIMER_RXN_EPHE_VALID timer has been started. If so,
                // we should stop it and restart it.
                if (gpsc_timer_status(p_zGPSCControl, C_TIMER_RXN_EPHE_VALID) == GPSC_SUCCESS) {
                    if (gpsc_timer_stop(p_zGPSCControl, C_TIMER_RXN_EPHE_VALID) == GPSC_SUCCESS) {
                        gpsc_timer_start(p_zGPSCControl, C_TIMER_RXN_EPHE_VALID, (4.0 * 60.0 * 1000.0));
                        ALOGI("C_TIMER_RXN_EPHE_VALID Timer restarted");
                    } else {
                        ALOGE("Failed to stop C_TIMER_RXN_EPHE_VALID timer");
                    }
                }

                // this session sequence should go to get assistance sequence
                gpsc_session_sequence(p_zGPSCControl, E_GPSC_SEQ_SESSION_GET_ASSISTANCE);
            }

         /* more than 17 Eph have been injected and TIME/POS info not available */
			/*  inject EPH for SVs that are visible in measurement report */
         gpsc_mgp_tx_visible_sv_eph(p_zGPSCControl);

		   if (p_zGPSCControl->p_zGPSCState->injectSvdirectionColdstart == TRUE)
         {
            gpsc_update_sv_visibility_Almanac(p_zGPSCControl);
         }
  	   break;

		case AI_REP_GPIO_STATUS:
       	ALOGI("+++ %s: Custom Packet: AI_REP_GPIO_STATUS +++\n", __FUNCTION__);
			gpsc_mgp_rx_prodlinetest_report(p_zGPSCControl, p_zAi2Field);
		break;

		/* Request from GPS receiver to enable/disable Ref Clock */
      case AI_CLAPP_CUSTOM_SUB_PKT_REP_CALIB_TIME_PULSE_REF_CLK:
		{
		   U8 u_CalibOption = (U8)NBytesGet(&p_B, sizeof(U8));
       	ALOGI("+++ %s: Custom Packet: AI_CLAPP_CUSTOM_SUB_PKT_REP_CALIB_TIME_PULSE_REF_CLK +++\n", __FUNCTION__);
		   gpsc_calib_timepulse_refclk_sensor_req (u_CalibOption);
      }
		break;
#ifdef ENABLE_INAV_ASSIST
		case AI_CLAPP_CUSTOM_SUB_PKT_REP_SENSOR_PE_DATA:
		{
       	 ALOGI("+++ %s: Custom Packet: AI_CLAPP_CUSTOM_SUB_PKT_REP_SENSOR_PE_DATA +++\n", __FUNCTION__);
		    /* Receive and Send the sensor PE data to SE lib. */
		    gpsc_mgp_rx_sensor_pe_data(p_B);
		}
		break;
#endif

	default:
	    ALOGI("+++ %s: Custom Packet: Unknown +++\n", __FUNCTION__);
	break;


	} /* end of switch(p_SubPacket) */
	break; /* end of AI_REP_CUSTOM_PACKET */

	case AI_CLAPP_REP_BEGIN_DWNLD:
		ALOGI("+++ %s: AI_CLAPP_REP_BEGIN_DWNLD +++\n", __FUNCTION__);

		/* stop timer as AI_CLAPP_REP_BEGIN_DWNLD received, which was started when begin download msg sent to GPS receiver */
		gpsc_timer_stop(p_zGPSCControl,C_TIMER_SEQUENCE);

		if ( (p_GPSCState->u_GPSCState != E_GPSC_STATE_SHUTDOWN) &&
		     (p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq != E_GPSC_SEQ_READY_BEGIN_DOWNLOAD) )
		{
			ALOGE("+++ %s: Warning : Unexpected message, in current state : Ignoring +++\n", __FUNCTION__);
		}
		else if(p_zGPSCControl->p_zGPSCState->z_PatchStatus.w_NextRecord !=0)
		{
			ALOGE("+++ %s: Warning : Unexpected state waiting for begin download in between a download session +++\n", __FUNCTION__);
		}
		else if(p_zGPSCControl->p_zGPSCState->z_PatchStatus.u_PatchFileOpen==FALSE)
		{
			ALOGE("+++ %s: Warning : Patch file not open : Invalid sequence +++\n", __FUNCTION__);
		}
		else
		{
			p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_READY_READ_INJECT_RECORD;
  		   gpsc_ready_sequence(p_zGPSCControl, E_GPSC_SEQ_READY_READ_INJECT_RECORD);

			return;
		}
		/* If any of the error cases, send no download patch control to move to AI thread */
		gpsc_mgp_skip_download(p_zGPSCControl);
	  break;

	case AI_CLAPP_REP_DWNLD_REC_RES:
		STATUSMSG("+++ %s: AI_CLAPP_REP_DWNLD_REC_RES +++\n", __FUNCTION__);
		gpsc_mgp_rx_proc_dwld_rec_res(p_zGPSCControl,p_zAi2Field);
		break;

	case AI_CLAPP_REP_DWNLD_COMPLETE:
		{
		   ALOGI("+++ %s: AI_CLAPP_REP_DWNLD_COMPLETE +++\n", __FUNCTION__);

		   /* stop timer as AI_CLAPP_REP_DWNLD_COMPLETE response received */
		   gpsc_timer_stop(p_zGPSCControl, C_TIMER_SEQUENCE);
		
         /* verify download complete response message from GPS receiver */
		   if (TRUE == gpsc_mgp_rx_proc_dwld_complete(p_zGPSCControl,p_zAi2Field))
			{
			   p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_READY_DOWNLOAD_COMPLETE;
			   gpsc_ready_sequence(p_zGPSCControl, E_GPSC_SEQ_READY_DOWNLOAD_COMPLETE);
			}
			else
			{
			   p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_READY_SKIP_DOWNLOAD;
			   gpsc_ready_sequence(p_zGPSCControl, E_GPSC_SEQ_READY_SKIP_DOWNLOAD);
			}
		}
		break;


    case AI_CLAPP_REP_GPS_STATUS:
      /* End local session processing   */
      /* Clear smlc session variables   */
      /* Request Sensor Version Report  */
      /* Transit LM to Version Check State */
      /* Send Msg to SIA "MGP Reset"    */

	   ALOGI("+++ %s: AI_CLAPP_REP_GPS_STATUS +++\n", __FUNCTION__);

	  /* AI_CLAPP_REP_GPS_STATUS msg received, stop timer */
	  gpsc_timer_stop(p_zGPSCControl, C_TIMER_SEQUENCE);

	  /* If configured for I2C, inject I2C configuration */
	  if (p_zGPSCConfig->comm_config.ctrl_comm_config_union == GPSC_COMM_MODE_I2C)
		{
			gpsc_mgp_tx_i2c_config(p_zGPSCControl);
			if (gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
			{
				// return GPSC_TX_INITIATE_FAIL;
				FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
				GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
			}
		}

      /* Process Patch download */
	   {
			if ((p_zGPSCControl->p_zGpscSm->u_ErrorStatus = (U8) gpsc_rx_examine_gps_status(p_zGPSCControl, p_zAi2Field))!= GPSC_SUCCESS)
			{
				gpsc_os_fatal_error_ind (GPSC_GPS_STATUS_MSG_FAIL);
				GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
				return;
			}
			else   /* for baud rate */
			{

				if(p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_READY_WAIT_FOR_WAKEUP)
				{
					/* If configured for I2C, inject I2C configuration */
	            	if(p_zGPSCConfig->comm_config.ctrl_comm_config_union == GPSC_COMM_MODE_I2C)
			    	{
				    	gpsc_mgp_tx_i2c_config(p_zGPSCControl);
				        if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
						{
							//   return GPSC_TX_INITIATE_FAIL;
							FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
							GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
						}
			    	}

					/* if baud rate change required, move to baud change sequence */
					if((p_GPSCState->u_BaudRateConfigured != GPSC_BAUD_115200) && (p_zGPSCControl->p_zGPSCConfig->patch_available != GPSC_FALSE) && (p_GPSCState->u_ValidPatch==0 ))
					{
						p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_READY_BAUD_CHANGE;
						gpsc_ready_sequence(p_zGPSCControl, E_GPSC_SEQ_READY_BAUD_CHANGE);
					}
					else /* process patch download and begin download control message */
					{
			         MCPF_UNUSED_PARAMETER(p_comm_config);

						if (TRUE == gpsc_process_patch_download(p_zGPSCControl))
						{
							p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_READY_BEGIN_DOWNLOAD;
							gpsc_ready_sequence(p_zGPSCControl,E_GPSC_SEQ_READY_BEGIN_DOWNLOAD);
						}
						else
						{
							p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_READY_SKIP_DOWNLOAD;
							gpsc_ready_sequence(p_zGPSCControl,E_GPSC_SEQ_READY_SKIP_DOWNLOAD);
						}
					}

				}
			} /* else for baud rate */
	  }

      break;


	  case  AI_CLAPP_REP_NMEA_SENTENCE:
     {
        //ALOGI("+++ %s: AI_CLAPP_REP_NMEA_SENTENCE +++\n", __FUNCTION__);
        char* p = 0; 
		  gpsc_init_result_buffer(p_zGPSCControl);

        p = gpsc_populate_nmea_buffer(p_zGPSCControl, p_zAi2Field);
		  if (p)
        {
           if (recon_data_fd == -1)
           {
              // open with RDWR to guaranteee we dont' block - even if we don't read - because of HAL startup timing issues
              recon_data_fd = open(RECON_DATA_PIPE, O_RDWR);

              if (recon_data_fd > 0)
                ALOGI("+++ %s: Recon Data Port [%s] successfully created for NMEA routing +++\n", __FUNCTION__, RECON_DATA_PIPE);
              else
                ALOGE("+++ %s: Recon Data Port [%s] could not be created. Routing NMEA NAV way +++\n", __FUNCTION__, RECON_DATA_PIPE);
           }

           if (recon_data_fd > 0)  // route to recon data pipe
           {
              recongpsmessage msg;
              msg.opcode = RECON_DATA_NMEA;
              msg.i_msglen = strlen(p) + 1;
              msg.pPayload = (unsigned char*)p;

              write(recon_data_fd, &msg, sizeof(msg.opcode) + sizeof(msg.i_msglen) );
              write(recon_data_fd, msg.pPayload, msg.i_msglen);

              //ALOGI("+++ %s: Sent NMEA sentence [%s] to Recon Data Pipe %s +++\n", __FUNCTION__, msg.pPayload, RECON_DATA_PIPE);
           }
           else
           {
		        gpsc_mgp_rx_proc_nmea(p_zGPSCControl);   // route nav way
           }
        }

     }
	  break;

	  case AI_CLAPP_REP_RTC_OSC_TEST_RES:
     {
		   ALOGI("+++ %s: AI_CLAPP_REP_RTC_OSC_TEST_RES +++\n", __FUNCTION__);
		   gpsc_mgp_rx_prodlinetest_report(p_zGPSCControl, p_zAi2Field);
     }
	  break;

	  case AI_CLAPP_REP_RAM_CHKSUM_RES:
     {
		   ALOGI("+++ %s: AI_CLAPP_REP_RAM_CHKSUM_RES +++\n", __FUNCTION__);
		   gpsc_mgp_rx_prodlinetest_report(p_zGPSCControl, p_zAi2Field);
     }
	  break;

	  case AI_CLAPP_REP_GPS_OSC_TEST_RES:
     {
			ALOGI("+++ %s: AI_CLAPP_REP_GPS_OSC_TEST_RES +++\n", __FUNCTION__);
		   gpsc_mgp_rx_prodlinetest_report(p_zGPSCControl, p_zAi2Field);
     }
	  break;

	  case AI_CLAPP_REP_GPS_SIGNAL_ACQ_TEST_RES:
			ALOGI("+++ %s: AI_CLAPP_REP_GPS_SIGNAL_ACQ_TEST_RES +++\n", __FUNCTION__);
		   gpsc_mgp_rx_prodlinetest_report(p_zGPSCControl,p_zAi2Field);
		   break;

	  case AI_CW_TEST_RESULTS:
			ALOGI("+++ %s: AI_CW_TEST_RESULTS +++\n", __FUNCTION__);
		   gpsc_mgp_rx_prodlinetest_report(p_zGPSCControl,p_zAi2Field);
		   break;

    case AI_CLAPP_REP_INVALID_MSG:
 		  ALOGI("+++ %s: AI_CLAPP_REP_INVALID_MSG +++\n", __FUNCTION__);
		  break;

      case AI_REP_SYNCTEST_EVENT:
 		   ALOGI("+++ %s: AI_REP_SYNCTEST_EVENT +++\n", __FUNCTION__);
		   gpsc_mgp_rx_prodlinetest_report(p_zGPSCControl,p_zAi2Field);
		   break;

	  case AI_CLAPP_REP_RCVR_SELF_TEST_RES:
 		   ALOGI("+++ %s: AI_CLAPP_REP_RCVR_SELF_TEST_RES +++\n", __FUNCTION__);
		   gpsc_mgp_rx_prodlinetest_report(p_zGPSCControl,p_zAi2Field);
		   break;

	  case AI_REP_ERROR_STATUS:
	  {
			gpsc_sess_specific_cfg_type* p_zNextCfg = p_zSessConfig->p_zSessSpecificCfg;
 		   ALOGI("+++ %s: AI_REP_ERROR_STATUS - Fatal Response from Chip +++\n", __FUNCTION__);

			gpsc_report_error_status(p_zGPSCControl,p_zAi2Field);

         // chip will come to ideal state... by default...need to put the GPSC to idle state...
         // first close all the sessions and move to idle state...
         gpsc_os_fatal_error_ind(GPSC_GPS_EXCEPTION_RESET_FAIL);

         // exctract the each session id and intimate that fatal error has occured...
         while (p_zNextCfg != NULL)
         {
             gpsc_app_fatal_err_ind (p_zNextCfg->w_SessionTagId,GPSC_GPS_EXCEPTION_RESET_FAIL);
             p_zNextCfg = (gpsc_sess_specific_cfg_type*) p_zNextCfg->p_nextSession;
         }

         // now close all the sessions... and go to Idle state...
         gpsc_sess_end(p_zGPSCControl);
	 }
	 break;

	  default:
      ALOGI("+++ %s: Unhandled Message [0x%x] +++\n", __FUNCTION__, p_zAi2Field->u_Id);
      break;
  }
}


/*===========================================================================

FUNCTION
  mgp_ver_ck

DESCRIPTION
  This function processes AI2 message coming from the Sensor

===========================================================================*/
#define GPS_VERSION_LENGTH 16
static U8 mgp_ver_ck
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*    p_zAi2Field
)
{

  U8  *p_B = p_zAi2Field->p_B;
  U8  u_major_ver, u_minor_ver,u_subminor1,u_subminor2;
  U8 u_patch_major, u_patch_minor,u_i;
  U32 q_ROMVersion;
  gpsc_cfg_type *p_zGPSCConfig = p_zGPSCControl->p_zGPSCConfig;
  if(p_zAi2Field->w_Length != GPS_VERSION_LENGTH)
	  return FALSE;

  /* The first 2 bytes corresponds to Chip ID and System Config.This 2 bytes are ignored,
     thus not been taken into consideration for NL5500 ICD changes */

  p_B++; /*Hardware version*/
  p_B++; /*RF Version*/

  u_subminor2 = *p_B++;
  u_subminor1 = *p_B++;
  u_minor_ver = *p_B++;
  u_major_ver = *p_B++;

  p_B++; /* skip month */
  p_B++; /* skip day */

  p_B++; /* skip year */
  p_B++;

  u_patch_minor = *p_B++;
  u_patch_major = *p_B++;
/*  w_patch_ver = (U16)(*p_B++<<8);
  w_patch_ver = (U16)(w_patch_ver + *p_B++);*/

  /*** decide if the version if OK **/
  STATUSMSG("Status: GPS Version: ROM: %u.%u.%u.%u,  Patch Version: %u.%u",
	    u_major_ver, u_minor_ver,u_subminor1,u_subminor2, u_patch_major, u_patch_minor);

  q_ROMVersion = (U32)((u_major_ver<<24) + (u_minor_ver<<16) +
	  (u_subminor1<<8) + u_subminor2);

  /*Search for compatible version in list*/
  /*KlocWork Critical Issue:53 Resolved by changing boundary check*/
  for(u_i = 0; u_i< GPSC_MAX_COMPATIBLE_VERSIONS; u_i++)
  {
	if(q_ROMVersion == p_zGPSCConfig->compatible_gps_versions[u_i])
		return TRUE;
  }
  return FALSE;
}

U8 mgp_custom_ver_resp
(
  gpsc_ctrl_type*  p_zGPSCControl,
  Ai2Field*    p_zAi2Field
)
{

  U8  *p_B = p_zAi2Field->p_B;
  U8  u_ChipID;
  U16 w_Year;
  gps_version *p_zGPSVersion = p_zGPSCControl->p_zGPSVersion;

  if(p_zAi2Field->w_Length != GPS_VERSION_LENGTH)
	  return FALSE;

  /* The first 2 bytes corresponds to Chip ID and System Config.*/

	/* Extract Chip ID*/
	u_ChipID = (U8)NBytesGet(&p_B, sizeof(U8));
	p_zGPSVersion->u_ChipIDMajorVerNum = (U8)((u_ChipID >> 4) & 0x0F);
	p_zGPSVersion->u_ChipIDMinorVerNum = (U8)(u_ChipID & 0x0F);


	/* System Config */
	p_zGPSVersion->u_SysConfig = (U8)NBytesGet(&p_B, sizeof(U8));

		/* ROM ID*/
		p_zGPSVersion->u_ROMIDSubMinor2VerNum = (U8)NBytesGet(&p_B, sizeof(U8));
		p_zGPSVersion->u_ROMIDSubMinor1VerNum = (U8)NBytesGet(&p_B, sizeof(U8));
		p_zGPSVersion->u_ROMIDMinorVerNum = (U8)NBytesGet(&p_B, sizeof(U8));
		p_zGPSVersion->u_ROMIDMajorVerNum = (U8)NBytesGet(&p_B, sizeof(U8));

		/* ROM: Month Day Year */
		p_zGPSVersion->u_ROMMonth = (U8)NBytesGet(&p_B, sizeof(U8)); /* month */
		p_zGPSVersion->u_ROMDay = (U8)NBytesGet(&p_B, sizeof(U8)); /* day */

		w_Year = (U16)NBytesGet(&p_B, sizeof(U16)); /* year */
		p_zGPSVersion->w_ROMYear = w_Year;

		/* PATCH version */
		p_zGPSVersion->u_PATCHMinor = *p_B++;
		p_zGPSVersion->u_PATCHMajor = *p_B++;

		/* PATCH: Month Day Year */
		p_zGPSVersion->u_PATCHMonth = (U8)NBytesGet(&p_B, sizeof(U8)); /* month */
		p_zGPSVersion->u_PATCHDay = (U8)NBytesGet(&p_B, sizeof(U8)); /* day */

		w_Year = (U16)NBytesGet(&p_B, sizeof(U16)); /* year */
		p_zGPSVersion->w_PATCHYear = w_Year;

		gpsc_app_gps_ver_res();	// send the verison response to application
	return TRUE;

}

#define BLF_STATUS_LENGTH 10

U8 mgp_send_blf_sts(gpsc_ctrl_type* p_zGPSCControl, Ai2Field* p_zAi2Field)
{
  U8  *p_B = p_zAi2Field->p_B;
  gpsc_state_type *p_GPSCState = p_zGPSCControl->p_zGPSCState;

  if(p_zAi2Field->w_Length != BLF_STATUS_LENGTH)
  {
          return FALSE;
  }

  p_GPSCState->blf_fix_count = (U16)NBytesGet(&p_B, sizeof(U16));
  p_GPSCState->blf_fix_count_threshold = (U16)NBytesGet(&p_B, sizeof(U16));
  p_GPSCState->blf_fix_count_rx = (U16)NBytesGet(&p_B, sizeof(U16));
  p_GPSCState->blf_position_sts = (U8)NBytesGet(&p_B, sizeof(U8));
  p_GPSCState->blf_velocity_sts = (U8)NBytesGet(&p_B, sizeof(U8));
  p_GPSCState->blf_dop_sts = (U8)NBytesGet(&p_B, sizeof(U8));
  p_GPSCState->blf_sv_sts = (U8)NBytesGet(&p_B, sizeof(U8));
  p_GPSCState->blf_sts = 1;

  gpsc_app_blf_status_res();    // send the blf status response to application

  return TRUE;
}


T_GPSC_result recon_request_assistance(U16 mask)
{
   if (recon_control_fd == -1)
   {
      // it is essential this is with RDWR, even if we never read. Otherwise it would block till reader (assistant)
      // starts, which can lead to exhaust of MCPF message pool and all hell breaks loose!
      recon_control_fd = open(RECON_CONTROL_PIPE, O_RDWR);

      if (recon_control_fd > 0)
         ALOGI("+++ %s: Recon_Control Port [%s] successfully created for Assistance Requests +++\n", __FUNCTION__, RECON_CONTROL_PIPE);
      else
      {
         ALOGE("+++ %s: Recon Control Port [%s] could not be created. Routing Assistance Requests NAV way (God help us) +++\n", __FUNCTION__, RECON_CONTROL_PIPE);
         return GPSC_FAIL;
      }
   }

   recongpsmessage msg; 
   msg.opcode = RECON_CONTROL_STATUS_REQUEST_ASSIST;
   msg.i_msglen = sizeof(unsigned int);
   unsigned int umask = mask;
   msg.pPayload = (unsigned char*)&umask;  // want to send 4 bytes always in status

   write(recon_control_fd, &msg, sizeof(msg.opcode) + sizeof(msg.i_msglen) );
   write(recon_control_fd, msg.pPayload, msg.i_msglen);

   ALOGI("+++ %s: Sent Assistance Request [mask = 0x%x] to Recon Status Pipe %s +++\n", __FUNCTION__, mask, RECON_CONTROL_PIPE);
   return GPSC_SUCCESS;
}

