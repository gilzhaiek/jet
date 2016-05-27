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
 * FileName			:	gpsc_timer_api.c
 *
 * Description     	:
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */
#include "gpsc_types.h"
#include "gpsc_sap.h"
#include "gpsc_mgp_tx.h"
#include "gpsc_sequence.h"
#include "gpsc_state.h"
#include "gpsc_msg.h"
#include "gpsc_sess.h"
#include "gpsc_database.h"
#include "gpsc_app_api.h"
#include "gpsc_timer_api.h"
#include "gpsc_comm.h"
#include "gpsc_mgp_cfg.h"
#include "gpsc_data.h"

#include <utils/Log.h>
#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG "NAVD.GPSC_TIMER_API"    // our identification for logcat (NAVD.GPSC_TIMER_API *:S)

extern T_GPSC_result gpsc_os_timer_expired_res (U32 u_TimerId)
{
	gpsc_ctrl_type*	p_zGPSCControl;
	gpsc_state_type      *p_zGPSCState;
	gpsc_sess_cfg_type*	p_zSessConfig;
	gpsc_cfg_type * p_zGPSCConfig;
	gpsc_event_cfg_type*    p_zEventCfg;
	U8 sequence;

	SAPENTER(gpsc_os_timer_expired_res);

	p_zGPSCControl = gp_zGPSCControl;
	p_zGPSCState  = p_zGPSCControl->p_zGPSCState;
	p_zSessConfig = p_zGPSCControl->p_zSessConfig;
	p_zGPSCConfig = p_zGPSCControl->p_zGPSCConfig;
	p_zEventCfg = p_zGPSCControl->p_zEventCfg;

	ALOGI("+++ %s: Timer Expired:%d, GPSC State:%s, Sequence:%s +++\n", __FUNCTION__, u_TimerId,
				       GpscSmState(p_zGPSCControl->p_zGpscSm->e_GpscCurrState),
					   GpscSmSequence(p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq)
			  );

	sequence = (U8) p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq;

	if (p_zGPSCState->w_TimerTrackBitmap & (1<<u_TimerId))
	{
		p_zGPSCState->w_TimerTrackBitmap &= ~(1<<u_TimerId);
	}
	else
	{
		ALOGE("+++ %s: Not Expected timer expiry of timer id %d in sequence %s +++\n",
				__FUNCTION__, u_TimerId,GpscSmSequence(p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq));
#ifndef TIMER_HACK
        return GPSC_FAIL;
#endif
	}	
	switch (u_TimerId)
	{
		case C_TIMER_PLT_SEQUENCE:
			{
			ERRORMSG("Error: Product Line Test Timer Expired");
			ERRORMSG("Error: GPS product line test result not received");
			p_zGPSCControl->p_zGpscSm->e_GpscCurrState = E_GPSC_STATE_IDLE;
			GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_IDLE);
			}
			break;
		case C_TIMER_SEQUENCE:
			switch(p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq)
				{
				/* process BAUD RATE CHANGE sequence */
				case E_GPSC_SEQ_READY_BAUD_CHANGE:
					{
					ERRORMSG("Error: GPSC baud rate change ack not received");
					gpsc_ready_sequence(p_zGPSCControl,E_GPSC_SEQ_READY_BAUD_CHANGE);
					}
					break;

				/* skip sequence */
				case E_GPSC_SEQ_READY_SKIP_DOWNLOAD:
					{
					gpsc_ready_sequence(p_zGPSCControl, E_GPSC_SEQ_READY_DOWNLOAD_COMPLETE);
					}
					break;

				case E_GPSC_SEQ_READY_DOWNLOAD_COMPLETE:
					{
					/* version info message not received */
					
					}
					break;

				case E_GPSC_SEQ_WAIT_ACTIVE:
					{
						/* active sequence complete and timer expire in case of RTI disable case, move to session sequence */
						if(FALSE == p_zGPSCConfig->rti_enable)
						{
							/* active sequence complete, and GPS recever is active (ON), start session sequence */
							p_zGPSCControl->p_zGpscSm->e_GpscCurrState = E_GPSC_STATE_ACTIVE;
							GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_ACTIVE);
						}
					}
					break;

				case E_GPSC_SEQ_SESSION_REQUEST_TIME:
					{
						p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE;
						gpsc_session_sequence(p_zGPSCControl,E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE);
					}
					break;
				case E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE:
			//	case E_GPSC_SEQ_SESSION_WAIT_FOR_LOCATION_REQUEST:
					{
					//	p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE;
						gpsc_session_sequence(p_zGPSCControl,E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE);
					}
					break;
				case E_GPSC_SEQ_SESSION_WAIT_FOR_LOCATION_REQUEST:
					{
					//	p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE;
						gpsc_session_sequence(p_zGPSCControl,E_GPSC_SEQ_SESSION_WAIT_FOR_LOCATION_REQUEST);
					}
					break;
				case E_GPSC_SEQ_SESSION_GET_ASSISTANCE:
					{
					p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_CONFIG;
					gpsc_session_sequence(p_zGPSCControl,E_GPSC_SEQ_SESSION_CONFIG);
					}
					break;

				case E_GPSC_SEQ_SESSION_CONFIG:
					{
					p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_ON;
					gpsc_session_sequence(p_zGPSCControl,E_GPSC_SEQ_SESSION_ON);
					}
					break;
 				}
				break;
		case C_TIMER_AI2_ACK_PENDING :

			{
			ERRORMSG("ERROR: GPS is not responding for ACK");
			gpsc_comm_tx_buff_type * p_Ai2TxBuf  = p_zGPSCControl->p_zGPSCCommTxBuff;
			ERRORMSG("ERROR: GPS is not responding for ACK i apply assistance");
			p_Ai2TxBuf->u_AI2AckPending =FALSE;
			gpsc_mgp_ver_req(p_zGPSCControl);
			if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_ACK_REQ)==FALSE)
			 	ERRORMSG("Error: transmit Error..");
			}
			break;
		case C_TIMER_TX_RES_PENDING :
			ERRORMSG("Error: GPSM - Driver Not responding");
			FATAL_ERROR(GPSC_TX_TIMEOUT_FAIL);
			GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
//			FATAL_INIT(GPSC_TX_TIMEOUT_FAIL);
			break;
		
		case C_TIMER_CLOCK_CALIB_WAIT:
			{
				/*512 milliseconds have passed since last CE OSC injection*/
				if(p_zGPSCState->u_ClockQualInjectPending ==C_CLOCK_CALIB_WAIT)
					p_zGPSCState->u_ClockQualInjectPending = FALSE;

				if(p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_SESSION_CONFIG)
					{
					p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_ON;
					gpsc_session_sequence(p_zGPSCControl,E_GPSC_SEQ_SESSION_ON);
					}
				break;
			}
		case C_TIMER_PERIODIC_REPORTING:
         break;
					 
		case C_TIMER_WAIT_LOCREQ :
    			
				if(p_zGPSCState->u_LocationRequestPending == FALSE)
					{
                         			STATUSMSG("C_TIMER_WAIT_LOCREQ: LOC_FIX_REQ not Received, end sessions");
						 gpsc_sess_end(p_zGPSCControl);
					}
				else
					{
                       				STATUSMSG("C_TIMER_WAIT_LOCREQ: timed out, LOC_FIX_REQ not received, moving to idle");	
					   p_zGPSCControl->p_zGpscSm->e_GpscCurrState = E_GPSC_STATE_IDLE;
					   GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_IDLE);					   
					}


                     break;

		case C_TIMER_WAIT_LOC_REQ_SEQ:
				{
				/* if location request is already made */
				if(p_zGPSCState->u_LocationRequestPending == TRUE)
					{

						/*If no SMLC Access, start session immediately -Autonomous case*/
						if (p_zSessConfig->u_SmlcAssisPermit == C_SMLC_ASSIS_NOT_PERMITTED)
						{
							STATUSMSG("Status: For autonomous change to session config state");
							p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_CONFIG;
							//return state_change(p_zGPSCControl,C_GPSC_STATE_SESSION_CONFIG);
						}
						else
						{
							p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_GET_ASSISTANCE;
						}
					}
				else
					{
					/* waiting for location request */
					p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_WAIT_FOR_LOCATION_REQUEST;
					//E_GPSC_SEQ_WAIT_FOR_LOCATION_REQUEST;
					}
				/* move to next sequence */
				gpsc_session_sequence(p_zGPSCControl,p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq);
				}
				break;

		case C_TIMER_SESSION_SLEEP:
			{
				/* wakeup from sleep session */
				GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SESSION_WAKEUP);
			}
			break;

		case C_TIMER_AUTO_POWER_SLEEP:
			{
			STATUSMSG("C_TIMER_AUTO_POWER_SLEEP Timer Expired, GPSC State:%s, Sequence:%s\n",
				       GpscSmState(p_zGPSCControl->p_zGpscSm->e_GpscCurrState),
					   GpscSmSequence(p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq));

					NavcCmdDisableQueue(p_zGPSCControl->p_zSysHandlers->hNavcCtrl);
					gpsc_inject_gps_sleep(p_zGPSCControl);
					p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_GPS_WAIT_SLEEP;

			}
			break;
		case C_TIMER_MEAS_TRACKING:
			{
				STATUSMSG("NO MEAS RX for %d msec", C_GPSC_TIMEOUT_MEAS_TRACKING);
			}
			break;

		case C_TIMER_SLEEP:
			{
				/* timer expires - E_GPSC_SEQ_GPS_WAIT_SLEEP */
				p_zGPSCControl->p_zGpscSm->e_GpscCurrState = E_GPSC_STATE_SLEEP;
				p_zGPSCControl->p_zGpscSm->e_GpscCurrEvent = E_GPSC_EVENT_READY_COMPLETE;
				GpscSm(p_zGPSCControl->p_zGpscSm,E_GPSC_EVENT_READY_COMPLETE);
				gpsc_app_gps_sleep_res();
			}
			break;
			
		case C_TIMER_EXP_REQ_CLOCK:
		{
			
			STATUSMSG("C_TIMER_EXP_REQ_CLOCK Timer expired");
			STATUSMSG("C_TIMER_EXP_REQ_CLOCK p_zSmlcAssist->w_InjectedFlags: 0x%X \n",p_zGPSCControl->p_zSmlcAssist->w_InjectedFlags);
			if(p_zGPSCControl->p_zSmlcAssist->w_InjectedFlags & C_EPH_AVAIL)
				p_zGPSCControl->p_zSmlcAssist->w_InjectedFlags &= ~C_EPH_AVAIL;
			STATUSMSG("C_TIMER_EXP_REQ_CLOCK after reset p_zSmlcAssist->w_InjectedFlags: 0x%X \n",p_zGPSCControl->p_zSmlcAssist->w_InjectedFlags);
			//Request Clock Report
			gpsc_sess_req_clock_to_get_assist(p_zGPSCControl);

		}
			break;
			
		case C_TIMER_RXN_EPHE_VALID:
		{
			/* request GPS time. This is required for requesting EE */
			gpsc_mgp_tx_req(p_zGPSCControl, AI_REQ_CLOCK_REP, 0);
			if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
			{
				STATUSMSG("gpsc_app_assist_database_req: GPSC_TX_INITIATE_FAIL - FATAL_ERROR");
				return GPSC_TX_INITIATE_FAIL;
			}

			/* set notification to indicate requesting for assistance is pending */
			p_zGPSCControl->u_reqAssistPending = TRUE;
		}
		break;

		case C_TIMER_NVS_SAVE:
		{
			ALOGI("C_TIMER_NVS_SAVE timer expired, saving NVS state");
			gpsc_app_nvs_file_req();

			/* Restart 10 minutes NVS timer. When this timer expires, NAVD will save its state to NVS */
			/* check if the timer is already started just to be safe. if already started then do not restart */
			if (GPSC_SUCCESS !=  gpsc_timer_status(p_zGPSCControl, C_TIMER_NVS_SAVE))
			{
				ALOGI("C_TIMER_NVS_SAVE Timer started");
				gpsc_timer_start(p_zGPSCControl, C_TIMER_NVS_SAVE, C_TIMER_NVS_SAVE_TIMEOUT);
			}

		}
		break;

		default:
			ERRORMSG2("Error: Invalid TIMER ID %d", u_TimerId);
			SAPLEAVE(gpsc_os_timer_expired_res,GPSC_FAIL)
	}
	SAPLEAVE(gpsc_os_timer_expired_res,GPSC_SUCCESS)
}

T_GPSC_result gpsc_timer_start(gpsc_ctrl_type*  p_zGPSCControl,
							        U8 timerid, U32 expiry_time)
{
	gpsc_state_type*    p_zGPSCState   = p_zGPSCControl->p_zGPSCState;
	if(p_zGPSCState->w_TimerTrackBitmap & (1<<timerid))
	{
		SAPCALL(gpsc_os_timer_stop_ind);
		gpsc_os_timer_stop_ind(timerid);		
	}
	else
	{
		p_zGPSCState->w_TimerTrackBitmap |= (1<<timerid);
	}
	SAPCALL(gpsc_os_timer_start_ind);
	return gpsc_os_timer_start_ind(timerid,expiry_time);
}

T_GPSC_result gpsc_timer_stop(gpsc_ctrl_type*  p_zGPSCControl, U8 timerid)
{
	gpsc_state_type*    p_zGPSCState   = p_zGPSCControl->p_zGPSCState;
	
	if(timerid==0)
	{
		/*TimerID of 0 means reset all timers*/
		SAPCALL(gpsc_os_timer_stop_ind);
		p_zGPSCState->w_TimerTrackBitmap = 0;
		return gpsc_os_timer_stop_ind(timerid);	
	}
	
	if(p_zGPSCState->w_TimerTrackBitmap & (1<<timerid))
	{
		SAPCALL(gpsc_os_timer_stop_ind);
		p_zGPSCState->w_TimerTrackBitmap &= ~(1<<timerid);
		return gpsc_os_timer_stop_ind(timerid);	
	}
	else
	{
		return GPSC_SUCCESS;
	}
}



T_GPSC_result gpsc_timer_status(gpsc_ctrl_type*  p_zGPSCControl, U8 timerid)
{
	gpsc_state_type*    p_zGPSCState   = p_zGPSCControl->p_zGPSCState;
	
	if(p_zGPSCState->w_TimerTrackBitmap & (1<<timerid))
		return GPSC_SUCCESS;
    return GPSC_FAIL;
}
