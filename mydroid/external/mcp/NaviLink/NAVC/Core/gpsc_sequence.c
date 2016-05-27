

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
 * FileName		: gpsc_sequence.c
 *
 * Description     	: This file contains the functions of inter state sequence.
 *
 * Author         	: Viren Shah - virenshah@ti.com
 *
 *
 ******************************************************************************
 */
#include "gpsc_types.h"
#include "gpsc_sap.h"
#include "gpsc_data.h"
#include "gpsc_state.h"
#include "gpsc_msg.h"
#include "gpsc_mgp_tx.h"
#include "gpsc_mgp_cfg.h"
#include "gpsc_sess.h"
#include "gpsc_init.h"
#include "gpsc_comm.h"
#include "gpsc_drv_api.h"
#include "gpsc_app_api.h"
#include "gpsc_mgp_assist.h"
#include "gpsc_database.h"
#include "gpsc_timer_api.h"
#include "gpsc_sequence.h"
#include "mcpf_time.h"
#include "mcpf_services.h"
#include "nt_adapt.h"
#include "mcp_hal_fs.h"
#include "mcp_hal_os.h"
#include "navc_cmdHandler.h"


#include <math.h>
#ifdef TIMER_HACK
#include "navc_api.h"
#include "mcpf_msg.h"
#endif

#include <utils/Log.h>
#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG "NAVD.GPSC_SESS"    // our identification for logcat (adb logcat NAVD.GPSC_SESS:V *:S)


#define GPSC_5300_VERSION  17367044

extern T_GPSC_result gpsc_app_refclk_req(T_GPSC_refclk_req refclkreq);
extern void gpsc_init_priority();
extern T_GPSC_result gpsc_app_stop_fix_ind(void);

extern void gpsc_mgp_inject_freq_est
(
  gpsc_ctrl_type*             p_zGPSCControl,
  gpsc_inject_freq_est_type*  p_zInjectFreqEst
);

void gpsc_populate_time_assist
(
 gpsc_ctrl_type* p_zGPSCControl,
 T_GPSC_time_assist*        p_time_assist
);


T_GPSC_result gpsc_init_sequence(gpsc_ctrl_type* p_zGPSCControl, E_GPSC_SM_SEQ e_GpscSmSequence)
{
	gpsc_sys_handlers* p_sysHandlers;

	gpsc_sess_cfg_type*		p_zSessConfig		= p_zGPSCControl->p_zSessConfig;

	gpsc_db_type*			p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;

	T_GPSC_result x_Result = GPSC_FAIL;

	STATUSMSG("Sequence Start: %s\n", GpscSmSequence(e_GpscSmSequence) );

	if(E_GPSC_SEQ_INIT != e_GpscSmSequence)
		{
		return x_Result;
		}

	p_sysHandlers = (gpsc_sys_handlers *)p_zGPSCControl->p_zSysHandlers;

	/* Initialise internal communication buffers */
	gpsc_comm_init(p_zGPSCControl);

	/* initialize session config*/
	gpsc_init_sess_config(p_zSessConfig);

	/*Clean database and then load from NVS, this will ensure validity even if
		  NVS file was not found*/
	gpsc_init_database(p_zGPSCDatabase, TRUE);

	/*initialize GPSC state and state  flags */
	gpsc_init_state( p_zGPSCControl );

	/*  Initialize the structure holding assistance data from SMLC */
	gpsc_init_smlc(p_zGPSCControl);

	/* Load structures from NVS */
	if((x_Result = gpsc_load_from_nvs(p_zGPSCControl, &p_sysHandlers->GpscConfigFile))!=GPSC_SUCCESS)
		{
		return x_Result;
		}

	/* initialize event config */
	gpsc_init_event_config(p_zGPSCControl);

	gpsc_init_priority();
	/*Stop all timers*/
	gpsc_timer_stop(p_zGPSCControl,C_TIMER_ALL);

	return GPSC_SUCCESS;
}

T_GPSC_result gpsc_ready_sequence(gpsc_ctrl_type*	p_zGPSCControl, E_GPSC_SM_SEQ e_GpscSmSequence)
{
	gpsc_state_type*		p_zGPSCState    = p_zGPSCControl->p_zGPSCState;
	gpsc_cfg_type*			p_zGPSCConfig	= p_zGPSCControl->p_zGPSCConfig;


	STATUSMSG("Sequence Start: %s\n", GpscSmSequence(e_GpscSmSequence) );

	switch(e_GpscSmSequence)
	{
		case E_GPSC_SEQ_READY_START:
			{
			p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_READY_WAIT_FOR_WAKEUP;
			/*power down the GPS IP */
			gpsc_mgp_inject_gps_shutdown_ctrl(p_zGPSCControl);
			gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK);
			MCP_HAL_OS_Sleep(100);

			/* injecting communication protocol message to GPS receiver and wait for GPS Status Message */
			gpsc_inject_comm_protocol (p_zGPSCControl);
			}
			break;

		case E_GPSC_SEQ_READY_BAUD_CHANGE:
			{
				/* injecting baud rate to GPS receiver at initialization - issue 152 */
				p_zGPSCControl->p_zGPSCConfig->comm_config.comm_config_union.comm_config_uart.uart_baud_rate = p_zGPSCState->u_BaudRateConfigured;
				gpsc_mgp_tx_inject_baud_rate (p_zGPSCControl);
				if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
				{
				   FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
			       GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
				   //FATAL_INIT(GPSC_TX_INITIATE_FAIL);
				}
				/*Wait for baud rate change AI2 msg from GPS - AI_REP_BAUD_RATE_CHANGE */
		    	gpsc_timer_start(p_zGPSCControl,C_TIMER_SEQUENCE,p_zGPSCConfig->ai2_comm_timeout);
			}
			break;
		case E_GPSC_SEQ_READY_BEGIN_DOWNLOAD:
			{
				/* Send Download Control Message and Wait for Begin Download Message from GPS Rx */
				if(p_zGPSCControl->p_zGPSCConfig->patch_available == GPSC_STACKRAM_PATCH)
			    {
				STATUSMSG("Status: Stack Ram Patch Download");
				gpsc_mgp_send_patch_dwld_ctrl(p_zGPSCControl,C_GPSC_PATCH_STACKRAM_DWLD);
			    }
			    else
			    {
			     STATUSMSG("Status: Current Link Patch Download");
			      gpsc_mgp_send_patch_dwld_ctrl(p_zGPSCControl,C_GPSC_PATCH_CUR_LINK_DWLD);
			    }
				if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
				   {
						//   return GPSC_TX_INITIATE_FAIL;
						FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
						GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
					}
				/* stop timer when AI_CLAPP_REP_BEGIN_DWNLD report received */
				gpsc_timer_start(p_zGPSCControl,C_TIMER_SEQUENCE,p_zGPSCConfig->ai2_comm_timeout);
			}
			break;

		case E_GPSC_SEQ_READY_READ_INJECT_RECORD:
			{
				/*Wait for record received acknowledgement from GPS*/
				gpsc_read_next_dwld_record(p_zGPSCControl);
			}
		    break;

		case E_GPSC_SEQ_READY_SKIP_DOWNLOAD:
			{
			/* Sending Download Control with No Download Selected */
			STATUSMSG("Status: Sending Download Control with No Download Selected");
	        gpsc_mgp_send_patch_dwld_ctrl(p_zGPSCControl,C_GPSC_PATCH_NO_DOWNLOAD);
	        if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
				{
					//   return GPSC_TX_INITIATE_FAIL;
					FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
					GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
				}
			p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_READY_DOWNLOAD_COMPLETE;
			return gpsc_ready_sequence(p_zGPSCControl, E_GPSC_SEQ_READY_DOWNLOAD_COMPLETE);
			}
		    break;

		case E_GPSC_SEQ_READY_DOWNLOAD_COMPLETE:
			{
			/*Do nothing, wait for version record, stop timer when AI_CLAPP_REP_VERSION msg received */
	    	gpsc_timer_start(p_zGPSCControl,C_TIMER_SEQUENCE,p_zGPSCConfig->ai2_comm_timeout);
			}
			break;

		case E_GPSC_SEQ_READY_INJECT_INIT_CONFIG:
			{
			/* inject GPS receiver configuration */
			gpsc_inject_init_config(p_zGPSCControl);
			}
            break;

		case E_GPSC_SEQ_READY_GPS_INJECT_NVS:
			{
				if(gpsc_mgp_inject_nvs_data(p_zGPSCControl))
				{
					/* NVS injection complete, change sequence to handle ack of RECEIVER_IDLE */
					p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_READY_GPS_INJECT_NVS_COMPLETE;

					/* send receiver idle message to GPS receiver */
					gpsc_inject_gps_idle(p_zGPSCControl);
				}
			}
            break;

		case E_GPSC_SEQ_READY_GPS_WAIT_SLEEP:
			{
			/* inject receiver configuration and put receiver in sleep */
			gpsc_inject_gps_sleep(p_zGPSCControl);
			}
	        break;

		default:
			{
			STATUSMSG("WARNING: GPSC_SM : UNKNOWN SEQUENCE:%d\n",e_GpscSmSequence);
			}
	}
	return GPSC_SUCCESS;
}

T_GPSC_result gpsc_sleep_sequence(gpsc_ctrl_type* p_zGPSCControl, E_GPSC_SM_SEQ e_GpscSmSequence)
{
	gpsc_cfg_type* p_zGPSCConfig = p_zGPSCControl->p_zGPSCConfig;

	STATUSMSG("Sequence Start: %s\n", GpscSmSequence(e_GpscSmSequence) );
	/* stop timer of sequence */
//	gpsc_timer_stop(p_zGPSCControl,C_TIMER_SEQUENCE);

	switch(e_GpscSmSequence)
	{
		case E_GPSC_SEQ_GPS_SLEEP:
			{
				 /* check low power state is idle or sleep state */
				if(p_zGPSCConfig->low_power_state == LOW_POWER_MODE_IDLE)
				{
					STATUSMSG("gpsc_sleep_sequence: LP state set to IDLE");

					if(p_zGPSCControl->p_zCustomStruct->custom_sleep_req_flag==FALSE)
				{
						/* There is no external request for sleep */
					gpsc_inject_gps_idle(p_zGPSCControl);
					p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_GPS_IDLE;
				}
					else if (p_zGPSCControl->p_zCustomStruct->custom_sleep_req_flag==TRUE)
					{
						STATUSMSG("gpsc_sleep_sequence: external sleep requested");
						/* external request for sleep pending */
						gpsc_inject_gps_sleep(p_zGPSCControl);
						p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_GPS_WAIT_SLEEP;
					}
					else
						STATUSMSG("gpsc_sleep_sequence: 1should not come here!!");

				}
				else if (p_zGPSCConfig->low_power_state == LOW_POWER_MODE_SLEEP)
				{

					STATUSMSG("gpsc_sleep_sequence: LP state set to SLEEP; sending Idle and starting timer");
					/* There is no external request for sleep */
					gpsc_inject_gps_idle(p_zGPSCControl);
					p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_GPS_IDLE;

#if 0
					/* low power state is sleep state */
					gpsc_inject_gps_sleep(p_zGPSCControl);
					p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_GPS_WAIT_SLEEP;
#endif
				}
				else
					STATUSMSG("gpsc_sleep_sequence: 2should not come here!!");
			}
			break;

		case E_GPSC_SEQ_GPS_IDLE:
			{
				p_zGPSCControl->p_zGpscSm->e_GpscCurrState = E_GPSC_STATE_IDLE;
				p_zGPSCControl->p_zGpscSm->e_GpscCurrEvent = E_GPSC_EVENT_READY_COMPLETE;
				GpscSm(p_zGPSCControl->p_zGpscSm,E_GPSC_EVENT_READY_COMPLETE);
				if((p_zGPSCControl->p_zGPSCState->u_SensorOpMode == C_RC_IDLE ||
						p_zGPSCControl->p_zGPSCState->u_SensorOpModeConfirmed == TRUE) &&
						(p_zGPSCConfig->low_power_state == LOW_POWER_MODE_SLEEP))
				 {
					gpsc_timer_start(p_zGPSCControl,C_TIMER_AUTO_POWER_SLEEP,p_zGPSCConfig->sleep_entry_delay_timeout);
			}
				 else
				 STATUSMSG("gpsc_sleep_sequence: LP State is Idle Do not start sleep timer");
			}
			break;
		case E_GPSC_SEQ_GPS_WAIT_SLEEP:
			{
				p_zGPSCControl->p_zGpscSm->e_GpscCurrState = E_GPSC_STATE_SLEEP;
				p_zGPSCControl->p_zGpscSm->e_GpscCurrEvent = E_GPSC_EVENT_READY_COMPLETE;
				GpscSm(p_zGPSCControl->p_zGpscSm,E_GPSC_EVENT_READY_COMPLETE);
				NavcCmdEnableQueue(p_zGPSCControl->p_zSysHandlers->hNavcCtrl);
//				gpsc_app_gps_sleep_res();
				/* start timer E_GPSC_SEQ_GPS_WAIT_SLEEP - sleep entry delay provided, wait till timer expires */
				//gpsc_timer_start(p_zGPSCControl, C_TIMER_SLEEP, p_zGPSCConfig->sleep_entry_delay_timeout);
			}
			break;
		default:
			{
			STATUSMSG("WARNING: GPSC_SM : UNKNOWN SEQUENCE:%d\n",e_GpscSmSequence);
			}
	}
	return GPSC_SUCCESS;
}

T_GPSC_result gpsc_wakeup_sequence(gpsc_ctrl_type* p_zGPSCControl, E_GPSC_SM_SEQ e_GpscSmSequence)
{
	gpsc_cfg_type* p_zGPSCConfig	 = p_zGPSCControl->p_zGPSCConfig;


	STATUSMSG("Sequence Start: %s\n", GpscSmSequence(e_GpscSmSequence) );

	switch(e_GpscSmSequence)
	{
	case E_GPSC_SEQ_WAIT_GPS_WAKEUP:
			STATUSMSG("gpsc_wakeup_sequence: wakeup the receiver");
			/* send wakeup event to wake up gps receiver */
			gpsc_wakeup_receiver(p_zGPSCControl);
			/* Wait for GPSAWAKE ASYNC msg */
		break;

	case E_GPSC_SEQ_GPS_WAKEUP:
	     /* Wait approx 1 sec here till the receiver actual wake up */
	        STATUSMSG("gpsc_wakeup_sequence:Added Delay 1 sec afetr wakeup event");
	        MCP_HAL_OS_Sleep(980);

		/* after wakeup, put receiver to idle */
			STATUSMSG("gpsc_wakeup_sequence: put receiver to idle");
		gpsc_inject_gps_idle(p_zGPSCControl);
			/*setup parameter for next sequence */
			//p_zGPSCDatabase->u_AlmCounter = 17;
		break;
#if 0   // this case is not required any more as it has been move to active sequence
	case E_GPSC_SEQ_GPS_WAKEUP_INJ_ASSISTANCE:
			/* inject ALM for SV17 to SV32 once receiver has woken up*/
			if(gpsc_mgp_inj_alm_on_wakeup(p_zGPSCControl))
			{
				if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_ACK_REQ)==FALSE)
				{
					FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
					GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
				}

			}
			else
			{
				STATUSMSG("gpsc_wakeup_sequence: ALM injection complete, injecting last know position");
				if (z_DBPos->d_ToaSec > 0 &&
					z_DBPos->z_PosTime.w_GpsWeek != C_GPS_WEEK_UNKNOWN)
				{
					gpsc_inject_pos_est_type  p_InjectPosEst;

					p_InjectPosEst.l_Lat = p_zReportPos->l_Lat;
					p_InjectPosEst.l_Long = p_zReportPos->l_Long;
					p_InjectPosEst.x_Ht = p_zReportPos->x_Height;
					p_InjectPosEst.q_Unc = sqrt(pow(p_zReportPos->w_EastUnc,2)+pow(p_zReportPos->w_NorthUnc,2)+pow(p_zReportPos->w_VerticalUnc,2));

					p_InjectPosEst.u_Mode = 0;
					p_InjectPosEst.w_GpsWeek = z_DBPos->z_PosTime.w_GpsWeek;
					p_InjectPosEst.q_GpsMsec = z_DBPos->z_PosTime.q_GpsMsec;

					p_InjectPosEst.u_ForceFlag = 0;

					STATUSMSG("gpsc_wakeup_sequence: Inject Lat: %d, Lon: %d, Ht:%d, Unc: %d",
						p_InjectPosEst.l_Lat, p_InjectPosEst.l_Long, p_InjectPosEst.x_Ht, p_InjectPosEst.q_Unc);

					gpsc_mgp_inject_pos_est(p_zGPSCControl, &p_InjectPosEst, FALSE); // True signifies request back position
					if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
					{
						FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
						GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
					}

				}
				else
				{
					STATUSMSG("gpsc_wakeup_sequence: DB position not useful");
				}

				p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_GPS_WAKEUP_COMPLETE;
				gpsc_wakeup_sequence(p_zGPSCControl,E_GPSC_SEQ_GPS_WAKEUP_COMPLETE);
			}

		break;

#endif
	case E_GPSC_SEQ_GPS_WAKEUP_COMPLETE:
			/* Move to ready complete */
			/* GPS receiver is in IDLE state, change GPSC state to IDLE after wakeup */
			p_zGPSCControl->p_zGpscSm->e_GpscCurrState = E_GPSC_STATE_IDLE;
			GpscSm(p_zGPSCControl->p_zGpscSm,E_GPSC_EVENT_READY_COMPLETE);

			/* Send wake up event to client */
			gpsc_app_gps_wakeup_res();
			p_zGPSCControl->p_zCustomStruct->wakeup = TRUE;
			/* check if a location fix request is pending*/
			if(p_zGPSCConfig->low_power_state == LOW_POWER_MODE_SLEEP)
			{
				 if(p_zGPSCControl->p_zGPSCState->u_LocationRequestPending == TRUE)
				 {
					/* process location fix request */
					GpscSm(p_zGPSCControl->p_zGpscSm,E_GPSC_EVENT_LOCATION_START);
				 }
			}

		break;

	default:
			STATUSMSG("WARNING: GPSC_SM : UNKNOWN SEQUENCE:%d\n",e_GpscSmSequence);
		break;

	}
	return GPSC_SUCCESS;
}

T_GPSC_result gpsc_active_sequence(gpsc_ctrl_type* p_zGPSCControl, E_GPSC_SM_SEQ e_GpscSmSequence)
{
	gpsc_state_type*		p_zGPSCState    = p_zGPSCControl->p_zGPSCState;
	gpsc_db_type* p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_db_pos_type *z_DBPos = &p_zGPSCDatabase->z_DBPos;
	gpsc_report_pos_type  *p_zReportPos  = &p_zGPSCDatabase->z_DBPos.z_ReportPos;

	switch(e_GpscSmSequence)
		{

		case E_GPSC_SEQ_ACTIVE_APPLY_REF_CLOCK:
			{
				p_zGPSCDatabase->u_AlmCounter = 17;

				/* apply ref clock and inject calibration based on response from NAVC or GPSM */
				if (p_zGPSCState->u_CalibType != GPSC_DISABLE_CALIBRATION)
				{
					gpsc_drv_apply_calib_timepulse_refclk_ind (C_APPLY_TIMESTAMP_REFCLK_ENABLE);
            }
            else
            {
					T_GPSC_refclk_req refclkreq;

					refclkreq.refclk_req = GPSC_REFCLK_ENABLE;
 					gpsc_app_refclk_req(refclkreq);
					p_zGPSCState->u_SkipCalib = TRUE;
					p_zGPSCState->u_CalibRequested = TRUE;

					/* process for pre active (pre session) state */
					p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_ACTIVE_INJECT_ASSIST_SLEEP_FIX;
					gpsc_active_sequence(p_zGPSCControl, E_GPSC_SEQ_ACTIVE_INJECT_ASSIST_SLEEP_FIX);

            }
			}
			break;

		case E_GPSC_SEQ_ACTIVE_WAIT_CALIB: /* E_GPSC_SEQ_CALIBRATION */
			{
			gpsc_calibration_control(p_zGPSCControl);
			}
			break;

		case E_GPSC_SEQ_ACTIVE_INJECT_ASSIST_SLEEP_FIX:
			{
				/* inject ALM for SV17 to SV32 once receiver has woken up*/
				if (gpsc_mgp_inj_alm_on_wakeup(p_zGPSCControl))
				{
					if (gpsc_comm_transmit(p_zGPSCControl, C_AI2_ACK_REQ)==FALSE)
					{
						FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
						GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
					}

				}
				else
				{
					if ((z_DBPos->d_ToaSec > 0) && (z_DBPos->z_PosTime.w_GpsWeek != C_GPS_WEEK_UNKNOWN) && (z_DBPos->z_PosTime.w_GpsWeek > 0) )
					{
						gpsc_inject_pos_est_type  p_InjectPosEst;

						p_InjectPosEst.l_Lat = p_zReportPos->l_Lat;
						p_InjectPosEst.l_Long = p_zReportPos->l_Long;
						p_InjectPosEst.x_Ht = p_zReportPos->x_Height;
						p_InjectPosEst.q_Unc = sqrt(pow(p_zReportPos->w_EastUnc,2)+pow(p_zReportPos->w_NorthUnc,2)+pow(p_zReportPos->w_VerticalUnc,2));

						// capping the unc to min of 1km
						if (p_InjectPosEst.q_Unc < 1000.0)
						{
							p_InjectPosEst.q_Unc = 1000.0;
						}

						p_InjectPosEst.u_Mode = 1;

						p_InjectPosEst.w_GpsWeek = z_DBPos->z_PosTime.w_GpsWeek;
						p_InjectPosEst.q_GpsMsec = z_DBPos->z_PosTime.q_GpsMsec;

						p_InjectPosEst.u_ForceFlag = 0;

						STATUSMSG("gpsc_active_sequence: Inject Lat: %d, Lon: %d, Ht:%d, Unc: %d",
							p_InjectPosEst.l_Lat, p_InjectPosEst.l_Long, p_InjectPosEst.x_Ht, p_InjectPosEst.q_Unc);

						gpsc_mgp_inject_pos_est_wakeup(p_zGPSCControl, &p_InjectPosEst, FALSE); // True signifies request back position
						if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
						{
							FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
							GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
						}

					}
					else
					{
						STATUSMSG("gpsc_active_sequence: DB position not useful");
					}

					p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_WAIT_ACTIVE;
					gpsc_active_sequence(p_zGPSCControl,E_GPSC_SEQ_WAIT_ACTIVE);
				}
			}
			break;
		case E_GPSC_SEQ_WAIT_ACTIVE:
			{
			/* process for pre active (pre session) state */
			gpsc_activate_presession(p_zGPSCControl);

			}
			break;

		case E_GPSC_SEQ_ACTIVE:
			{
				gpsc_session_sequence(p_zGPSCControl,E_GPSC_SEQ_SESSION_REQUEST_TIME);
			}
			break;

		default:
			{
			STATUSMSG("WARNING: GPSC_SM : UNKNOWN SEQUENCE:%d\n",e_GpscSmSequence);
			return GPSC_FAIL;
			}
		break;
		}
	return GPSC_SUCCESS;
}

T_GPSC_result gpsc_session_sequence(gpsc_ctrl_type* p_zGPSCControl, E_GPSC_SM_SEQ e_GpscSmSequence)
{
	gpsc_cfg_type*			   p_zGPSCConfig	 = p_zGPSCControl->p_zGPSCConfig;
	gpsc_state_type*		   p_zGPSCState    = p_zGPSCControl->p_zGPSCState;
	gpsc_smlc_assist_type*  p_zSmlcAssist   = p_zGPSCControl->p_zSmlcAssist;

	T_GPSC_result x_Result = GPSC_FAIL;
	gpsc_smlc_gps_time_type*	p_zSmlcGpsTime = &p_zSmlcAssist->z_SmlcGpsTime;
	gpsc_inject_time_est_type*	p_zInjectTimeEst = &p_zSmlcGpsTime->z_InjectGpsTimeEst;

	T_GPSC_time_assist z_CurrentTime;

	switch (e_GpscSmSequence)
		{
		case E_GPSC_SEQ_SESSION_REQUEST_TIME:
			{
				z_CurrentTime.gps_week = C_GPS_WEEK_UNKNOWN;
				/* check if fine time injection is enabled in gpsc config file */
				if (p_zGPSCConfig->enable_finetime)
				{
				   /* GPSC will select arbitrary reference time mode if the receiver in Clock configuration 3 */
				   if (p_zGPSCState->u_SensorClockConfig == C_GPSC_SENSOR_CONFIG3)
				   {
					   z_CurrentTime.time_accuracy = GPSC_TIME_ACCURACY_FINE_ARBITRARY_REFFERENCE;
				   }
				   /* fine time last pulse */
				   else
				   {
					   z_CurrentTime.time_accuracy = GPSC_TIME_ACCURACY_FINE_LAST_PULSE;
				   }
				}
				else /* coarse mode */
				{
					z_CurrentTime.time_accuracy = GPSC_TIME_ACCURACY_COARSE;
				}

				x_Result = gpsc_time_request_tow_ind(&z_CurrentTime);

			   /* process result of reference time requested from network */
				if ((GPSC_SUCCESS == x_Result) || (x_Result == GPSC_TIME_PULSE_GENERATION_FAIL))
				{

					if ((z_CurrentTime.gps_week == C_GPS_WEEK_UNKNOWN) ||
						   (z_CurrentTime.time_unc > C_MAX_VALID_TIME_UNC_MS * 1000)
						  )
						{
							/*No time returned in coarse mode also*/

							/* Check for Time assistance without week.If present the inject time.Incase of AA data */
							if ((p_zSmlcAssist->w_AvailabilityFlags & C_REF_GPSTIME_AVAIL))
							{
								gpsc_populate_time_assist(p_zGPSCControl,&z_CurrentTime);

								p_zSmlcAssist->w_AvailabilityFlags &=  ~C_REF_GPSTIME_AVAIL;
								p_zGPSCState->u_TimeInjected = TRUE;

								/* to send SAP return as pending */
								p_zGPSCState->u_ReturnPending = TRUE;

								/* inject time */
								p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_INJECT_TIME;
								gpsc_mgp_inject_gps_time(p_zGPSCControl, p_zInjectTimeEst);

								/* wait for the clk report from the GPS before applying the other assistance*/
		                  p_zGPSCState->u_WaitForClkReport = TRUE;

								if (gpsc_comm_transmit(p_zGPSCControl, C_AI2_ACK_REQ)==FALSE)
							   {
									FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
									GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
								}

								return GPSC_SUCCESS;

							}
							else
							{
								p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE;
								return gpsc_session_sequence(p_zGPSCControl, E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE);
							}

						}


						if ((p_zSmlcAssist->w_AvailabilityFlags & C_REF_GPSTIME_AVAIL))
						{
							gpsc_populate_time_assist(p_zGPSCControl,&z_CurrentTime);

							p_zSmlcAssist->w_AvailabilityFlags &=  ~C_REF_GPSTIME_AVAIL;
							p_zGPSCState->u_TimeInjected = TRUE;
							p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_INJECT_TIME;

							/* Inject Time */
							/* to send SAP return as pending */
							p_zGPSCState->u_ReturnPending = TRUE;
							gpsc_mgp_inject_gps_time(p_zGPSCControl, p_zInjectTimeEst);

							/* wait for the clk report from the GPS before applying the other assistance*/
		               p_zGPSCState->u_WaitForClkReport = TRUE;

							if (gpsc_comm_transmit(p_zGPSCControl, C_AI2_ACK_REQ) == FALSE)
							{
									FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
									GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
							}

							return GPSC_SUCCESS;

						}
						else
						{
								p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE;
								return gpsc_session_sequence(p_zGPSCControl, E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE);
						}
				}

				else if ((x_Result == GPSC_TIME_PULSE_GENERATION_PENDING) && (p_zGPSCConfig->enable_finetime))
				{

                   /* Set the timer for HW event and tow response be in the same state till the response or time out */
                   p_zGPSCState->u_FineTimeRespPending  = TRUE;
                   p_zGPSCState->u_AsyncPulseEventPending  = TRUE;

				}

			}
			break;
		case E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE:
			{
				if (gpsc_mgp_inject_nvs_data(p_zGPSCControl))
				{
					gpsc_session_apply_assistance(p_zGPSCControl);
				}
			}
				break;

			case E_GPSC_SEQ_SESSION_WAIT_FOR_LOCATION_REQUEST:
				{
					gpsc_session_wait_for_loc_req(p_zGPSCControl);
				}
				break;

			case E_GPSC_SEQ_SESSION_GET_ASSISTANCE:
				{
					gpsc_get_assistance_from_network(p_zGPSCControl);
				}
				break;

			case E_GPSC_SEQ_SESSION_CONFIG:
				{
					/* do nothing here as seq change based on ack */
				}
				break;

			case E_GPSC_SEQ_SESSION_ON:
				{
               gpsc_session_on(p_zGPSCControl);
				}
				break;
			case E_GPSC_SEQ_SESSION_SLEEP:
				{
				}
				break;
			default:
				{
				return GPSC_FAIL;
				}
			break;

	}
	return GPSC_SUCCESS;
}

T_GPSC_result gpsc_inject_comm_protocol (gpsc_ctrl_type*	p_zGPSCControl)
{

	gpsc_state_type*		p_zGPSCState    = p_zGPSCControl->p_zGPSCState;
	gpsc_cfg_type*			p_zGPSCConfig	= p_zGPSCControl->p_zGPSCConfig;

	/*	copy the configured baud rate to here and use it later */
	p_zGPSCState->u_BaudRateConfigured = (U8)p_zGPSCControl->p_zGPSCConfig->comm_config.comm_config_union.comm_config_uart.uart_baud_rate;
	p_zGPSCControl->p_zGPSCConfig->comm_config.comm_config_union.comm_config_uart.uart_baud_rate = GPSC_BAUD_115200; //set to 115200


	STATUSMSG("Injecting Communication Protocol - AI2");
	gpsc_mgp_inject_comm_prot(p_zGPSCControl);

	if (gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
	   {
			//   return GPSC_TX_INITIATE_FAIL;
			FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
			GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
		}

	/* start timer, stop when AI_CLAPP_REP_GPS_STATUS status message received */
	gpsc_timer_start(p_zGPSCControl,C_TIMER_SEQUENCE,p_zGPSCConfig->ai2_comm_timeout);

	return GPSC_SUCCESS;
		//return state_change(p_zGPSCControl,C_GPSC_STATE_WAIT_FOR_STATUS);
}

T_GPSC_result gpsc_inject_init_config(gpsc_ctrl_type*	p_zGPSCControl)
{
	gpsc_cfg_type*			p_zGPSCConfig	= p_zGPSCControl->p_zGPSCConfig;
	gpsc_dyna_feature_cfg_type *p_zGPSCDynaFeatureConfig = p_zGPSCControl->p_zGPSCDynaFeatureConfig;
	gpsc_db_type * p_zGPSCDatabase =  p_zGPSCControl->p_zGPSCDatabase;

		STATUSMSG("Status: Injecting config");

		gpsc_mmgps_init(p_zGPSCControl);

		/* dynamic configuration */
		if(!p_zGPSCDynaFeatureConfig->feature_flag)
		{
		  gpsc_mpg_init_dynamic_feature_config (p_zGPSCControl);
		}

		/* customer configuration injection */
        if(p_zGPSCConfig->compatible_gps_versions[0] ==GPSC_5300_VERSION)
           gpsc_mgp_tx_customer_config(p_zGPSCControl);
        else
           gpsc_mgp_tx_customer_config_extended(p_zGPSCControl);

	/* receiver configuration. -->-->--> ack req set here */
	gpsc_mgp_rcvr_cfg(p_zGPSCControl);

	/*inject RTC TIME control settings */
	gpsc_mgp_tx_rtc_control(p_zGPSCControl);

	/* inject time if available with NAVC or GPSC database */
	p_zGPSCDatabase->q_InjEphList =0;
	p_zGPSCDatabase->q_InjAlmList =0;

	if (p_zGPSCConfig->systemtime_or_injectedtime == GPSC_CFG_SYSTEM_TIME)
	{
		gpsc_mgp_inject_init_time(p_zGPSCControl);
	}

#if 1 //added on 26-04-11
		gpsc_mgp_tx_write_reg(p_zGPSCControl, 0x0009528C, 0xFFFFFFFF, 0x08000000);
#endif

	/* The following line is added to enable Loop Error (LE) messages from the Device */
	gpsc_mgp_tx_write_reg(p_zGPSCControl, 0x0009528C, 0xFFFFFFFF, 0x08000000);

	if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_ACK_REQ)==FALSE)
		return GPSC_TX_INITIATE_FAIL;


	return GPSC_SUCCESS;
}

T_GPSC_result gpsc_inject_gps_idle(gpsc_ctrl_type*	p_zGPSCControl)
{
        gpsc_dyna_feature_cfg_type *p_zGPSCDynaFeatureConfig = p_zGPSCControl->p_zGPSCDynaFeatureConfig;

        /* Check if APM need to be disabled */
        if((p_zGPSCDynaFeatureConfig->feature_flag & GPSC_FEAT_APM) && (p_zGPSCDynaFeatureConfig->apm_control == 1))
        {
	    STATUSMSG("Status: Send Disable APM command to sensor.");
   
            /* Chip might be sleeping. Send a wake up command */
            gpsc_wakeup_receiver(p_zGPSCControl);
            /* Sleep for 100 msec. to allow the chip to wakeup */
	    MCP_HAL_OS_Sleep(100);
            
            p_zGPSCDynaFeatureConfig->apm_control = 0;
            gpsc_mgp_tx_inject_advanced_power_management(p_zGPSCControl);
            if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
            {
                FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
                GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
                return GPSC_TX_INITIATE_FAIL;
            }
        }

	/* Turn off all async/periodic events */
	gpsc_mgp_cfg_evt_rep_init ( p_zGPSCControl );

	STATUSMSG("Status: GPS Idle (C_RECEIVER_IDLE) Message Sent");
	gpsc_mgp_tx_rc_act (p_zGPSCControl, C_RECEIVER_IDLE, 0);

	/* Avoid putting the receiver to sleep here, to handle possible race condition
	* in handling async events. If the receiver is put to sleep here and there is
	* an async event in the Rx buffer, processing the event will wake up the
	* receiver. Now, pulling the sleep_x to off, will cause gps to crash.
	*/

	if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_ACK_REQ)==FALSE)
		{
			//   return GPSC_TX_INITIATE_FAIL;
			FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
			GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
			return GPSC_TX_INITIATE_FAIL;
		}
#ifndef CLIENT_CUSTOM
	/*Enable Clibration control if calibration is not disabled, so calib param injection happens during active-seq*/
    if(p_zGPSCControl->p_zGPSCState->u_CalibType != GPSC_DISABLE_CALIBRATION)
    {
      p_zGPSCControl->p_zGPSCState->u_CalibRequestPending = TRUE;
    }
#endif
	return GPSC_SUCCESS;
}


T_GPSC_result gpsc_inject_gps_sleep(gpsc_ctrl_type*	p_zGPSCControl)
{
	STATUSMSG("Status: GPS Sleep (C_RECEIVER_OFF) Message Sent");
	gpsc_mgp_tx_rc_act (p_zGPSCControl, C_RECEIVER_OFF, 0);

	if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_ACK_REQ)==FALSE)
		{
			//   return GPSC_TX_INITIATE_FAIL;
			FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
			GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
			return GPSC_TX_INITIATE_FAIL;
		}
	return GPSC_SUCCESS;
}




T_GPSC_result gpsc_wakeup_receiver (gpsc_ctrl_type* p_zGPSCControl)
{

	/* receiver to wake up only if Power Management state is SLEEP (PM-SLEEP) */
	/* set GPSC sequence to wakeup sequence */

	/* wakeup GPS from sleep state */
	STATUSMSG("Status: Injecting wakeup sequence\n");
	gpsc_mgp_inject_wakeup_sequence(p_zGPSCControl);

	if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
		{
			//   return GPSC_TX_INITIATE_FAIL;
			FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
			GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
			return GPSC_TX_INITIATE_FAIL;
		}

	return GPSC_SUCCESS;
}





T_GPSC_result gpsc_calibration_control(gpsc_ctrl_type* p_zGPSCControl)
{
    gpsc_state_type*		p_zGPSCState    = p_zGPSCControl->p_zGPSCState;

	if ((p_zGPSCState->u_SensorClockConfig == C_GPSC_SENSOR_CONFIG4) && (p_zGPSCState->u_CalibRequestPending))
	{
	   if (p_zGPSCState->u_CalibType == GPSC_ENABLE_PERIODIC_CALIB_TIME_STAMP
			|| p_zGPSCState->u_CalibType == GPSC_ENABLE_ONESHOT_CALIB_TIME_STAMP)
	   {
		   gpsc_mgp_inject_CalibTimestamp_Period(p_zGPSCControl);
	   }
	   else if(p_zGPSCState->u_CalibType == GPSC_ENABLE_PERIODIC_CALIB_REF_CLK
			|| p_zGPSCState->u_CalibType == GPSC_ENABLE_ONESHOT_CALIB_REF_CLK)
	   {
		   gpsc_mgp_inject_OscParams(p_zGPSCControl);
	   }

		gpsc_mgp_inject_calibrationcontrol(p_zGPSCControl, p_zGPSCState->u_CalibType);

		if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
		{
			FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
			GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
			return GPSC_TX_INITIATE_FAIL;
		}

	}
	return TRUE;
}



T_GPSC_result gpsc_ready_complete(gpsc_ctrl_type* p_zGPSCControl)
{
	gpsc_state_type*		p_zGPSCState    = p_zGPSCControl->p_zGPSCState;
	gpsc_assist_wishlist_type*  p_zGpsAssisWishlist = p_zGPSCControl->p_zGpsAssisWishlist;
	gpsc_sv_steer_inject_type* p_zSvInject = p_zGPSCControl->p_zSvInject;

#ifdef LARGE_UPDATE_RATE
	if(p_zSessConfig->q_SleepDuration)
	{
		STATUSMSG("Going to Session Sleep for %d seconds", p_zSessConfig->q_SleepDuration);
		return TRUE;
	}
#endif
	/* initialize sv steering inject count here */
	p_zSvInject->u_SvSteerInjectCount =0;

	/*wait for location fix request*/

	 p_zGpsAssisWishlist->w_Wishlist =	GPSC_REQ_TIME |
								GPSC_REQ_LOC |
								GPSC_REQ_NAV |
								GPSC_REQ_ALMANAC |
								GPSC_REQ_IONO |
								GPSC_REQ_RTI |
								GPSC_REQ_DGPS |
								GPSC_REQ_UTC|
								GPSC_REQ_AA;

	if (p_zGPSCState->u_ReadyRequestPending ==TRUE)
	{
		/* send ready complete msg to NAVC or GPSM */
		gpsc_cmd_complete_cnf(GPSC_SUCCESS);
		p_zGPSCState->u_ReadyRequestPending =FALSE;
	}

	if (p_zGPSCState->u_FixStopRequestPending ==TRUE)
	{
		p_zGPSCState->u_FixStopRequestPending =FALSE;

      /* Send the location stop command completion: REJO */
      gpsc_app_stop_fix_ind(); 

		/* send command complete confirmation for location stop request */
		gpsc_cmd_complete_cnf(GPSC_SUCCESS);

	}

	gpsc_timer_stop(p_zGPSCControl,C_TIMER_ALL);

	STATUSMSG("Status: GPSC READY");

/*Sharath: dont do auto power save for CLIENT_CUSTOM*/
	/* Wait in READY state for further action */

	return TRUE;

}

T_GPSC_result gpsc_activation_sequence_start(gpsc_ctrl_type* p_zGPSCControl)
{
	gpsc_state_type*		p_zGPSCState    = p_zGPSCControl->p_zGPSCState;


		/* for product line test, to be taken care in product line  sequence */
		if(p_zGPSCState->u_ProductlinetestRequestPending)
		{
			p_zGPSCControl->p_zGpscSm->e_GpscCurrState = E_GPSC_STATE_IDLE;
			GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_PLT);
			//return tate_change(C_GPSC_STATE_PRODUCTLINE_TEST);
		}
		else
		{
		    if((p_zGPSCState->u_SensorClockConfig == C_GPSC_SENSOR_CONFIG4) && (p_zGPSCState->u_CalibRequestPending == TRUE))
			{
				STATUSMSG("gpsc_activation_sequence_start: waiting for confirmation for refclk");
				gpsc_drv_apply_calib_timepulse_refclk_ind (C_APPLY_TIMESTAMP_REFCLK_ENABLE);
			}
			else
			{
				p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_WAIT_ACTIVE;

    		}

		}
	return GPSC_SUCCESS;

}

T_GPSC_result gpsc_activate_presession(gpsc_ctrl_type* p_zGPSCControl)
{
   gpsc_state_type*		p_zGPSCState    = p_zGPSCControl->p_zGPSCState;
	gpsc_cfg_type*			p_zGPSCConfig	= p_zGPSCControl->p_zGPSCConfig;

	/* session pre act should be called first */
	gpsc_sess_pre_act( p_zGPSCControl);

	/* set u_2DPdopMask based on configuration file settings */
	p_zGPSCControl->p_zRcvrConfig->u_2DPdopMask = p_zGPSCConfig->alt_2d_pdop;

	/* inject the receiver config after get the loc fix req */
	gpsc_mgp_rcvr_cfg(p_zGPSCControl);

	/* Time between first assistance to loc fix req is initialized to zero */
	p_zGPSCState->q_FCountRecOnFirstAssistToLocReq = 0;
	p_zGPSCState->u_FcountDelta = 0;
	p_zGPSCState->q_FCountRecOnFirstMeas = C_FIRSTMEAS_INVALID;

	/* injecting timeout and cep info for single pe */
	if ((p_zGPSCConfig->enable_timeout) && (p_zGPSCConfig->rti_enable))
	{
		/* inject cep based on config file here*/
		/* if parameter are set as unknown, cep is picked up from config file*/
		gpsc_loc_fix_qop zQop;
		zQop.u_HorizontalAccuracy = C_ACCURACY_UNKNOWN;
		zQop.w_MaxResponseTime = C_TIMEOUT_UNKNOWN;

	}

	/* inject dynamic features to sensor */
	gpsc_mpg_inject_dynamic_feature_config(p_zGPSCControl);

	/* A request to start a collection has been received, and is doing
	 pre-session preparation */

	gpsc_sess_prepare(p_zGPSCControl);
	/* Initialize Event Config to enable asynchronous events
	 * - new almanac
	 * - new ephemeris
	 * - new Iono/UTC
	 * - new SV health
	 */

	McpU32 u_EvtCfg=0x0;

	if(p_zGPSCConfig->apm_control==0)
	{
		u_EvtCfg = C_EVENT_ASYNC_ALL;
	}
	else
   { /* Geofence Session */
		if (p_zGPSCState->u_GeofenceAPMEnabled == 0)
		   u_EvtCfg = C_EVENT_ASYNC_ALL;
  	}

	/* for NL5500, to check if 0.5 sec (500 msec) report period is enabled or not */
   if (p_zGPSCConfig->recv_min_rep_period == 500)
	{
	/* for NL5500, initially periodic report rate is set to minimum report period */
	gpsc_mgp_cfg_set_evt(	p_zGPSCControl,
						  	1,
							  	(U32) u_EvtCfg,
						  	0,
							C_PERIOD_PRESET
							);
	}
	else /* if(p_zGPSCConfig->recv_min_rep_period = 500) */
	{
				gpsc_mgp_cfg_set_evt(	p_zGPSCControl,
									  	1,
							  	(U32) u_EvtCfg,
									  	C_PERIOD_PRESET,
										0
										);

   }



	/* to inject frequency/clock estimate, gpsc3.7.0.0 changes */
	maximize_frunc(p_zGPSCControl);

	/* initialize result buffer and session parameters */
	gpsc_init_result_buffer(p_zGPSCControl);
	gpsc_sess_init_counters(p_zGPSCControl);

   /* check for RTI enable*/
   if (p_zGPSCConfig->rti_enable)
	{
	   gpsc_sess_start(p_zGPSCControl);
	}

	if (gpsc_comm_transmit(p_zGPSCControl, C_AI2_ACK_REQ)==FALSE)
	{
		FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
		GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
			
      return GPSC_TX_INITIATE_FAIL;
	}
	return GPSC_SUCCESS;
}

T_GPSC_result gpsc_session_config(gpsc_ctrl_type* p_zGPSCControl)
{
	gpsc_state_type*		p_zGPSCState	= p_zGPSCControl->p_zGPSCState;

	/*If 512 msecs have passed since last clock qual injection, proceed to ON*/

	p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_CONFIG;
	if (p_zGPSCState->u_ClockQualInjectPending ==  FALSE)
	{
      p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_ON;
		gpsc_session_sequence(p_zGPSCControl, E_GPSC_SEQ_SESSION_ON);
	}


	/* If there is a clock quality to inject then Inject, else */
	if(p_zGPSCState->u_ClockQualInjectPending)
	{
		gpsc_mgp_inject_OscParams(p_zGPSCControl);
		if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_ACK_REQ)==FALSE)
		{
			//   return GPSC_TX_INITIATE_FAIL;
			FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
			GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
			return GPSC_TX_INITIATE_FAIL;
		}
	}
	return GPSC_SUCCESS;

}

T_GPSC_result gpsc_session_apply_assistance(gpsc_ctrl_type* p_zGPSCControl)
{
	gpsc_state_type*		p_zGPSCState    = p_zGPSCControl->p_zGPSCState;
	gpsc_smlc_assist_type*  p_zSmlcAssist  = p_zGPSCControl->p_zSmlcAssist;
	U16   w_AvailabilityFlags ;

	/*Apply the assistance that was injected to the the GPS*/
	w_AvailabilityFlags  = p_zSmlcAssist->w_AvailabilityFlags;

	p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE;

	if (gpsc_mgp_assist_apply(p_zGPSCControl)==TRUE)
		{
			/* assistance complete, Tx send to gps receiver */
			if (gpsc_comm_transmit(p_zGPSCControl, C_AI2_ACK_REQ)==FALSE)
			{
				FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
				GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
				return GPSC_TX_INITIATE_FAIL;
			}
			/* to send SAP return as pending */
			p_zGPSCState->u_ReturnPending = TRUE;

		}
	else
		{
			/* Init injection substate */
			p_zGPSCState->u_GPSCSubState =0;

			/* assistance injection complete, so wait for location request */
			if (p_zGPSCState->u_AssistInjectPending==TRUE)
			{
				p_zGPSCControl->p_zGPSCState->u_AssistInjectPending = FALSE;
				p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_REQUEST_TIME;

				gpsc_session_sequence(p_zGPSCControl, E_GPSC_SEQ_SESSION_REQUEST_TIME);
			}
			else
			{
			   /* wait for location request OR move to get assistance from network based on u_LocationRequestPending flag */
				p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_WAIT_FOR_LOCATION_REQUEST;
				gpsc_session_sequence(p_zGPSCControl,E_GPSC_SEQ_SESSION_WAIT_FOR_LOCATION_REQUEST);
			}
		}
	return TRUE;

}

T_GPSC_result gpsc_session_wait_for_loc_req(gpsc_ctrl_type* p_zGPSCControl)
{
	gpsc_state_type*	p_zGPSCState =	p_zGPSCControl->p_zGPSCState;

	/* check if location request is made */
	if (FALSE == p_zGPSCControl->p_zGPSCState->u_LocationRequestPending)
	{
		/* send GPSC Location request Pending message to NAVC or GPSM */
		gpsc_cmd_complete_cnf(GPSC_SUCCESS);

		/* to send SAP return as success to NAVC */
		p_zGPSCState->u_ReturnPending = FALSE;
		gpsc_timer_start(p_zGPSCControl, C_TIMER_WAIT_LOCREQ, C_GPSC_TIMEOUT_NO_LOCREQ);
	}
	else
	{
		/* now change sequence to E_GPSC_SEQ_SESSION_CONFIG and request clock report */
		/* Get assistance data sequence is started from the clock report callback*/
		p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_CONFIG;

		//Request for SV directions. This is used to esitmate valid EPH for visible SVs in the build wish list funtion.
		gpsc_sess_get_sv_dir(p_zGPSCControl);

		// Request Clock Report
		gpsc_sess_req_clock_to_get_assist(p_zGPSCControl);
		gpsc_session_config(p_zGPSCControl);
	}
	return GPSC_SUCCESS;
}


T_GPSC_result gpsc_get_assistance_from_network(gpsc_ctrl_type* p_zGPSCControl)
{
	gpsc_cfg_type*			       p_zGPSCConfig	      = p_zGPSCControl->p_zGPSCConfig;
	gpsc_sess_cfg_type*		    p_zSessConfig	      = p_zGPSCControl->p_zSessConfig;
	gpsc_smlc_assist_type*      p_zSmlcAssist       = p_zGPSCControl->p_zSmlcAssist;
	gpsc_assist_wishlist_type*  p_zGpsAssisWishlist = p_zGPSCControl->p_zGpsAssisWishlist;
	gpsc_state_type*	          p_zGPSCState        = p_zGPSCControl->p_zGPSCState;
	U32 toe_diff = 0, timer_expired = 0;
	U16 w_FilteredWishlist = 0;

	/* to send SAP return as success to NAVC */
	p_zGPSCState->u_ReturnPending = FALSE;

	  if (gpsc_sess_build_wishlist(p_zGPSCControl)== FALSE)
	  {
        STATUSMSG("Waiting for position report for building wishlist");
		  return 1;
	  }
	  STATUSMSG("gpsc_get_assistance_from_network: p_zGpsAssisWishlist->w_Wishlist 0x%X",p_zGpsAssisWishlist->w_Wishlist);

	  if(p_zSessConfig->u_PosCompEntity == C_SMLC_COMPUTED)
	  {
		w_FilteredWishlist = (U16)(p_zGpsAssisWishlist->w_Wishlist &
					p_zGPSCConfig->assist_bitmap_msassisted_mandatory_mask);
	  }
	  else
	  {
		w_FilteredWishlist = (U16)(p_zGpsAssisWishlist->w_Wishlist &
					p_zGPSCConfig->assist_bitmap_msbased_mandatory_mask);

	  }
	  STATUSMSG("gpsc_get_assistance_from_network: w_InjectedFlags = 0x%X\n",p_zSmlcAssist->w_InjectedFlags);

	  if (p_zSmlcAssist->w_InjectedFlags & C_ACQ_AVAIL)
	     w_FilteredWishlist &= ~GPSC_REQ_AA;

	  if (p_zSmlcAssist->w_InjectedFlags & C_DGPS_AVAIL)
	     w_FilteredWishlist &= ~GPSC_REQ_DGPS;

	  STATUSMSG("gpsc_get_assistance_from_network: w_FilteredWishlist = 0x%X \n",w_FilteredWishlist);

	  /* If EPH's are injected to receiver, dont ask it again  */
	  if (p_zSmlcAssist->w_InjectedFlags & C_EPH_AVAIL)
		  w_FilteredWishlist &= ~GPSC_REQ_NAV;

	  STATUSMSG("Wishlist->  Built :%X, Essential:%X",
		  p_zGpsAssisWishlist->w_Wishlist,w_FilteredWishlist);

    if (p_zSessConfig->u_SmlcAssisPermit == C_SMLC_ASSIS_PERMITTED)
    {
	  if ( w_FilteredWishlist == 0 )
	  {
			toe_diff   = gpsc_get_min_toe_diff(p_zGPSCControl);
			timer_expired = toe_diff * 1000; //convet it to msec
		
         if (gpsc_timer_status(p_zGPSCControl, C_TIMER_EXP_REQ_CLOCK) == GPSC_SUCCESS)
			{
				/* timer is already running. At any point of time there should be only one timer
				  before starting a new timer first stop the running one.
				 */
				gpsc_timer_stop(p_zGPSCControl,C_TIMER_EXP_REQ_CLOCK);
			}

			// EPH assistance is injected to receiver
			if ((toe_diff == 0))
			{
				timer_expired = 30 * 1000; // dummy timer 
				gpsc_timer_start(p_zGPSCControl, C_TIMER_EXP_REQ_CLOCK, timer_expired);
			}

			else if (toe_diff != 0)
			{
				timer_expired = toe_diff * 1000; //convert it to msec
				gpsc_timer_start(p_zGPSCControl ,C_TIMER_EXP_REQ_CLOCK, timer_expired);
			}

            if (gpsc_session_is_nw_session_running(p_zSessConfig)==TRUE)
            {
                ALOGI("+++ %s: Requesting Assistance +++\n", __FUNCTION__);
                T_GPSC_result x_Result = gpsc_request_assistance(p_zGPSCControl);
 	    	       if (x_Result != GPSC_SUCCESS)
			       {
				       ALOGE("+++ %s: Assistance Request Failure +++\n", __FUNCTION__);
		    	       return x_Result;
	             }
            }

	  }
     else
     {
		   // Wait for assistance to be injected by the CE
		   T_GPSC_result x_Result = GPSC_SUCCESS;
			toe_diff   = gpsc_get_min_toe_diff(p_zGPSCControl);

			if (toe_diff == 0) // good_eph_count < 4
				timer_expired = (1*60*60 + 20)*1000;
			else
				timer_expired = toe_diff * 1000; // convert it to msec

         ALOGI("+++ %s: Requesting Assistance (toe_diff = %d) +++\n", __FUNCTION__, (int)toe_diff);
	    	x_Result = gpsc_request_assistance(p_zGPSCControl);
	    	if (x_Result != GPSC_SUCCESS)
			{
				ALOGE("+++ %s: Assistance Request Failure +++\n", __FUNCTION__);
		    	return x_Result;
	      }

			if (gpsc_timer_status(p_zGPSCControl, C_TIMER_EXP_REQ_CLOCK) == GPSC_SUCCESS)
				gpsc_timer_stop(p_zGPSCControl, C_TIMER_EXP_REQ_CLOCK);

			gpsc_timer_start(p_zGPSCControl, C_TIMER_EXP_REQ_CLOCK, timer_expired);
      }
	}

	return TRUE;
}

T_GPSC_result gpsc_session_on(gpsc_ctrl_type* p_zGPSCControl)
{
	gpsc_state_type*		p_zGPSCState    = p_zGPSCControl->p_zGPSCState;
	gpsc_cfg_type*			p_zGPSCConfig	= p_zGPSCControl->p_zGPSCConfig;
	gpsc_sess_cfg_type*		p_zSessConfig		= p_zGPSCControl->p_zSessConfig;
	gpsc_loc_fix_qop* p_zQop = &p_zGPSCControl->p_zSmlcAssist->z_Qop;

	p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_ON;

	/* update period */
	gpsc_sess_update_period(p_zGPSCControl,C_PERIOD_PRESET);

	/* if paraleel sessions then update q_PeriodicExpTimerVal */
	if (p_zSessConfig->w_TotalNumSession > 1)
	{
	   p_zSessConfig->q_PeriodicExpTimerVal = ((p_zSessConfig->w_AvgReportPeriod*1000) + C_GPSC_TIMEOUT_PERIODIC_REPORT);
	}

	/* start periodic timer */
	gpsc_timer_start(p_zGPSCControl, C_TIMER_PERIODIC_REPORTING,p_zSessConfig->q_PeriodicExpTimerVal);

	/* Start Measurement Tracking
      WHATEVER THIS CRAP IS SUPPOSED TO DO IT CANT BE STARTED AS IT CAUSES MEMORY CORRUPTION
      SOMEWHERE ALONG THE LINE
   */
	/* gpsc_timer_start(p_zGPSCControl, C_TIMER_MEAS_TRACKING, 
     (C_GPSC_TIMEOUT_MEAS_TRACKING + p_zSessConfig->w_AvgReportPeriod * 1000 ) ); */
 

	/* if the loc_fix_req comes with new cep params, the smlc structure will be updated by now, go ahead and inject it*/
        if (p_zQop->u_TimeoutCepInjected == FALSE)
        {
	    gpsc_mgp_tx_inject_timeout_cep_info(p_zGPSCControl, p_zQop);
			
	    if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
	    {
	        FATAL_ERROR(GPSC_TX_INITIATE_FAIL); 
	        GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
	        return GPSC_TX_INITIATE_FAIL;
	    }

	    p_zQop->u_TimeoutCepInjected = TRUE;
	 }

	/* in case of RTI disable turn on GPS receiver ON */
	if (FALSE == p_zGPSCConfig->rti_enable && (!p_zGPSCState->u_GpsSessStarted))
	{
		/* update NMEA control and set GPS receiver ON */
		gpsc_sess_start(p_zGPSCControl);

		if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_ACK_REQ)==FALSE)
		{
			FATAL_ERROR(GPSC_TX_INITIATE_FAIL);
			GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);

			return GPSC_TX_INITIATE_FAIL;
		}
	}

	gpsc_cmd_complete_cnf(GPSC_SUCCESS);

	return GPSC_SUCCESS;
}



/* Handle the refclock enable/disable by application*/
extern T_GPSC_result gpsc_drv_apply_calib_timepulse_refclk_res (T_GPSC_result x_Result,U8 u_OperType)
{
    gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	gpsc_cfg_type * p_zGPSCConfig;
    gpsc_state_type* p_zGPSCState;

	SAPENTER(gpsc_drv_apply_calib_timepulse_refclk_res);

	p_zGPSCConfig = p_zGPSCControl->p_zGPSCConfig;
	p_zGPSCState = p_zGPSCControl->p_zGPSCState;

    if(x_Result == GPSC_FAIL)
    {
        STATUSMSG("Status: Err to get confirmation for refclk");     /* TBD Indicate fatal error */
        /* Always return fatal error when refclock req fails fails*/
        FATAL_ERROR(GPSC_DISCRETE_CONTROL_FAIL);
		p_zGPSCControl->p_zGpscSm->e_GpscCurrState = E_GPSC_STATE_IDLE;
		GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_IDLE);
		SAPLEAVE(gpsc_drv_apply_calib_timepulse_refclk_res,GPSC_FAIL);
    }
    else
    {
	    if (u_OperType == C_APPLY_TIMESTAMP_REFCLK_DISABLE)
		{
		    STATUSMSG("Status: Refclk Disabled by GPSM");
		}
	    else
		{
			STATUSMSG("gpsc_drv_apply_calib_timepulse_refclk_res: Inject Calib Ctrl");

			gpsc_calibration_control(p_zGPSCControl);

			/* process for pre active (pre session) state */
			p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_ACTIVE_INJECT_ASSIST_SLEEP_FIX;
			gpsc_active_sequence(p_zGPSCControl,E_GPSC_SEQ_ACTIVE_INJECT_ASSIST_SLEEP_FIX);

		} /* end of if (u_OperType == C_APPLY_TIMESTAMP_REFCLK_DISABLE) */
	} /* end of if(x_Result == GPSC_FAIL) */
    SAPLEAVE(gpsc_drv_apply_calib_timepulse_refclk_res,GPSC_SUCCESS);
}

extern T_GPSC_result gpsc_drv_sensor_calib_req_timepulse_refclk_res (U8 u_OperType)
{
	gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;

	gpsc_mgp_inject_timepulse_ref_clk_req_res(p_zGPSCControl, u_OperType);

	if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
				return GPSC_TX_INITIATE_FAIL;

	return GPSC_SUCCESS;

}

char* GpscSmSequence(E_GPSC_SM_SEQ eGpscSequence)
{
		char *rc = NULL;

		switch(eGpscSequence)
		{
		case  E_GPSC_SEQ_SHUTDOWN:
			{
			rc =  "SHUTDOWN  ";
			break;
			}
		case  E_GPSC_SEQ_INIT:
			{
			rc =  "INIT  ";
			break;
			}
		case  E_GPSC_SEQ_READY_START:
			{
			rc =  "READY_START  ";
			break;
			}
		case  E_GPSC_SEQ_READY_WAIT_FOR_WAKEUP:
			{
			rc =  "WAIT FOR WAKEUP  ";
			break;
			}
		case  E_GPSC_SEQ_READY_BAUD_CHANGE:
			{
			rc =  "BAUD RATE CHANGE  ";
			break;
			}
		case  E_GPSC_SEQ_READY_BEGIN_DOWNLOAD:
			{
			rc =  "BEGIN DOWNLOAD  ";
			break;
			}
		case  E_GPSC_SEQ_READY_READ_INJECT_RECORD:
			{
			rc =  "READ_INJECT_RECORD  ";
			break;
			}
		case  E_GPSC_SEQ_READY_SKIP_DOWNLOAD:
			{
			rc =  "SKIP DOWNLOAD  ";
			break;
			}
		case  E_GPSC_SEQ_READY_DOWNLOAD_COMPLETE:
			{
			rc =  "DOWNLOAD COMPLETE  ";
			break;
			}
	    case  E_GPSC_SEQ_READY_INJECT_INIT_CONFIG:
			{
			rc =  "INJECT_INIT_CONFIG";
			break;
			}

		case E_GPSC_SEQ_READY_GPS_INJECT_NVS:
			{
			rc =  "INJECT_NVS_DATA";
			break;
			}
		case E_GPSC_SEQ_READY_GPS_INJECT_NVS_COMPLETE:
			{
			rc =  "INJECT_NVS_DATA_COMPLETE";
			break;
			}
	    case  E_GPSC_SEQ_READY_GPS_WAIT_SLEEP:
			{
			rc =  "GPS WAIT IDLE";
			break;
			}
		case	  E_GPSC_SEQ_READY_IDLE:
			{
			rc =  "READY_IDLE  ";
			break;
			}
		case	  E_GPSC_SEQ_READY_SLEEP:
			{
			rc =  "READY_SLEEP  ";
			break;
			}
		case	  E_GPSC_SEQ_GPS_SLEEP:
			{
			rc =  "GPS_SLEEP  ";
			break;
			}
		case	  E_GPSC_SEQ_GPS_WAIT_SLEEP:
			{
			rc =  "GPS_WAIT_SLEEP  ";
			break;
			}
		case	  E_GPSC_SEQ_WAIT_GPS_WAKEUP: /* E_GPSC_SEQ_GPS_WAKEUP_START */
			{
			rc =  "WAIT_GPS_WAKEUP  ";
			break;
			}
		case	  E_GPSC_SEQ_GPS_WAKEUP:
			{
			rc =  "GPS_WAKEUP  ";
			break;
			}
		case   E_GPSC_SEQ_GPS_IDLE:
			{
			rc = "GPS_IDLE";
			break;
			}
		case   E_GPSC_SEQ_GPS_WAKEUP_INJ_ASSISTANCE:
			{
			rc = "WAKEUP_INJ_ASSISTANCE";
			break;
			}
		case	  E_GPSC_SEQ_GPS_WAKEUP_COMPLETE:
			{
			rc =  "GPS_WAKEUP_COMPLETE  ";
			break;
			}
		case	  E_GPSC_SEQ_ACTIVE_APPLY_REF_CLOCK:
			{
			rc =  "APPLY_REFCLK  ";
			break;
			}
		case	  E_GPSC_SEQ_ACTIVE_WAIT_CALIB:
			{
			rc =  "WAIT_CALIB  ";
			break;
			}
		case	E_GPSC_SEQ_ACTIVE_INJECT_ASSIST_SLEEP_FIX:
			{
			rc="INJECT_ASSIST_SLEEP_FIX";
			break;
			}
		case	  E_GPSC_SEQ_WAIT_ACTIVE:
			{
			rc =  "WAIT_ACTIVE  ";
			break;
			}
		case	  E_GPSC_SEQ_ACTIVE:
			{
			rc =  "ACTIVE  ";
			break;
			}
		case	  E_GPSC_SEQ_SESSION_REQUEST_TIME:
			{
			rc =  "SESSION_REQUEST_TIME ";
			break;
			}

		case	  E_GPSC_SEQ_SESSION_INJECT_TIME:
			{
			rc =  "SESSION_INJECT_TIME ";
			break;
			}
		case	  E_GPSC_SEQ_SESSION_LOC_START:
			{
			rc =  "LOCATION_START  ";
			break;
			}
		case	  E_GPSC_SEQ_SESSION_LOC_STOP:
			{
			rc =  "LOCATION_STOP  ";
			break;
			}
		case	  E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE:
			{
			rc =  "APPLY_ASSISTANCE  ";
			break;
			}
		case	  E_GPSC_SEQ_SESSION_GET_ASSISTANCE:
		    {
			rc =  "GET ASSISTANCE  ";
			break;
			}
		case	  E_GPSC_SEQ_SESSION_WAIT_FOR_LOCATION_REQUEST:
			{
			rc =  "WAIT_FOR_LOC_REQ  ";
			break;
			}
		case	  E_GPSC_SEQ_SESSION_LOCATION_COMPLETE:
			{
			rc =  "LOCATION_COMPLETE  ";
			break;
			}
		case	  E_GPSC_SEQ_SESSION_CONFIG:
			{
			rc =  "SESSION_CONFIG  ";
			break;
			}
		case	  E_GPSC_SEQ_SESSION_ON:
		    {
			rc =  "SESSION_ON  ";
			break;
			}
		case E_GPSC_SEQ_SESSION_SLEEP:
			{
			rc =  "SESSION SLEEP  ";
			break;
			}

		case	  E_GPSC_SEQ_PLT:
			{
			rc =  "PLT  ";
			break;
			}
		default:
			rc = "UNKNOWN";
		}
		return rc;
}


extern T_GPSC_result gpsc_drv_discrete_control_res (T_GPSC_result x_Result)
{
	UNREFERENCED_PARAMETER(x_Result);
	/* to do - delete this function after confirmation from NAVC  */

	SAPLEAVE(gpsc_drv_discrete_control_res,GPSC_SUCCESS)

}


