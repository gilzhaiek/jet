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
 * FileName		: gpsc_state.c
 *
 * Description     	: This file contains the functions of gpsc state machine.
 *
 * Author         	: Viren Shah - virenshah@ti.com
 *
 *
 ******************************************************************************
 */



#define DEFINE_STATE_STRINGS
#include "gpsc_types.h"
#include "gpsc_sap.h"
#include "gpsc_data.h"
#include "gpsc_sequence.h"
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
#include "gpsc_mgp_tx.h"
#include "mcpf_time.h"
#include "mcpf_services.h"
#include "nt_adapt.h"
#include <math.h>
#ifdef TIMER_HACK
#include "navc_api.h"
#endif
#include "mcpf_msg.h"

#include "navc_defs.h"
#include "mcpf_mem.h"

#include <utils/Log.h>
#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG "NAVD.GPSC_STATE"    // our identification for logcat (adb logcat NAVD.GPSC_STATE:V *:S)

#define GPSC_5300_VERSION  17367044


void gpsc_populate_time_assist
(
   gpsc_ctrl_type*      p_zGPSCControl,
   T_GPSC_time_assist*  p_time_assist
);

void gpsc_send_time_report
(
   gpsc_ctrl_type*      p_zGPSCControl,
   T_GPSC_time_accuracy time_accuracy
);

E_GPSC_SM_STATE StateGpscIdle     (gpsc_sm* pGpscSm, E_GPSC_SM_EVENT event);
E_GPSC_SM_STATE StateGpscSleep    (gpsc_sm* pGpscSm, E_GPSC_SM_EVENT event);
E_GPSC_SM_STATE StateGpscActive   (gpsc_sm* pGpscSm, E_GPSC_SM_EVENT event);
E_GPSC_SM_STATE StateGpscShutdown (gpsc_sm* pGpscSm, E_GPSC_SM_EVENT event);
E_GPSC_SM_STATE StateGpscFoward   (gpsc_sm* pGpscSm, E_GPSC_SM_EVENT event);

void* GpscSmCreate(void* hGpsc, gpsc_sys_handlers* p_sysHandlers)
{
	gpsc_sm* pGpscSm = (gpsc_sm*) mcpf_mem_alloc(p_sysHandlers->hMcpf, sizeof(gpsc_sm));
	if (pGpscSm)
	{
		/* Prepare SM database */
		pGpscSm->stateHnd[E_GPSC_STATE_SHUTDOWN] = StateGpscShutdown;
		pGpscSm->stateHnd[E_GPSC_STATE_IDLE] = StateGpscIdle;
		pGpscSm->stateHnd[E_GPSC_STATE_SLEEP] = StateGpscSleep;
		pGpscSm->stateHnd[E_GPSC_STATE_ACTIVE] = StateGpscActive;
		pGpscSm->stateHnd[E_GPSC_STATE_FOWARD] = StateGpscFoward;
		
		pGpscSm->e_GpscCurrState = E_GPSC_STATE_SHUTDOWN;
		pGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SHUTDOWN;
		pGpscSm->hGpsc = hGpsc;
	}
	else
	{
		/* Memory not allocated for GPSC State Machine */
		pGpscSm = NULL;
	}
	
	return pGpscSm;
}


void GpscSm(void* hGpscSm, E_GPSC_SM_EVENT eGpscSmEvent)
{
	gpsc_sm* pGpscSm = (gpsc_sm*) hGpscSm;
	pGpscSm->e_GpscCurrState = pGpscSm->stateHnd[pGpscSm->e_GpscCurrState](pGpscSm, eGpscSmEvent);

	if (pGpscSm->e_GpscCurrState == E_GPSC_STATE_SHUTDOWN)
	{
		if (pGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_SHUTDOWN_COMPLETE)
		{
		   GpscSm(pGpscSm, E_GPSC_EVENT_SHUTDOWN);
		}
	}
		
}

E_GPSC_SM_STATE StateGpscIdle (gpsc_sm* pGpscSm, E_GPSC_SM_EVENT event)
{
	gpsc_ctrl_type* p_zGPSCControl = (gpsc_ctrl_type*) pGpscSm->hGpsc;
	gpsc_state_type      *p_zGPSCState = p_zGPSCControl->p_zGPSCState;
	gpsc_productline_test_params*   p_zProductlineTest = p_zGPSCControl->p_zProductlineTest;
	gpsc_smlc_assist_type*  p_zSmlcAssist  = p_zGPSCControl->p_zSmlcAssist;

	E_GPSC_SM_STATE nextState = E_GPSC_STATE_IDLE;

	ALOGI("+++ %s: State [%s], Event: [%s] +++\n", __FUNCTION__, GpscSmState(pGpscSm->e_GpscCurrState), GpscSmEvent(event) );

	switch (event) 
	{
		case E_GPSC_EVENT_IDLE:
			{
			p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_GPS_SLEEP;
			gpsc_sleep_sequence(p_zGPSCControl, E_GPSC_SEQ_GPS_SLEEP);

			/* send ready sequence complete message to navc or gpsm */
			nextState = E_GPSC_STATE_IDLE;
			}
			break;

		/* E_GPSC_EVENT_READY_COMPLETE - gpsc is in ready state while GPS receiver is in IDLE state */
		case E_GPSC_EVENT_READY_COMPLETE:
			{
				pGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_READY_IDLE;

				/* send ready/sleep sequence complete message to navc or gpsm */
				gpsc_ready_complete(p_zGPSCControl);
			}
			break;
			
		case E_GPSC_EVENT_GPS_WAKEUP:
		{
			/* If event is for PLT, move GPSC state to PLT state */
			if (p_zGPSCState->u_ProductlinetestRequestPending)
			{
				p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_PLT;
				gpsc_mgp_tx_prodlinetest_req(p_zGPSCControl);

				if (gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
				{
					//   return GPSC_TX_INITIATE_FAIL;
					FATAL_ERROR(GPSC_TX_INITIATE_FAIL); 
					GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
					return GPSC_TX_INITIATE_FAIL;
				}

				/* start timer and wait for product line test response gpsc_mgp_rx_prodlinetest_report */
				gpsc_timer_start(p_zGPSCControl,C_TIMER_PLT_SEQUENCE,p_zProductlineTest->timeout);
			
			}
			else 
			{
				p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_WAIT_GPS_WAKEUP;
				gpsc_wakeup_sequence(p_zGPSCControl, E_GPSC_SEQ_WAIT_GPS_WAKEUP);
			}
		}
		break;

		case E_GPSC_EVENT_LOCATION_START:
			{
    			/* start active sequence */
				pGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_ACTIVE_APPLY_REF_CLOCK;
				gpsc_active_sequence(p_zGPSCControl, E_GPSC_SEQ_ACTIVE_APPLY_REF_CLOCK);
			}
			break;

		case E_GPSC_EVENT_ASSIST_INJECT:
			{
				if (pGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_ACTIVE_APPLY_REF_CLOCK ||
					pGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_ACTIVE_WAIT_CALIB ||
					pGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_ACTIVE_INJECT_ASSIST_SLEEP_FIX||
					pGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_WAIT_ACTIVE ||
					pGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_SESSION_REQUEST_TIME)
				{
					// do nothing, as this will this will naturally lead to apply assistance. no need to force.
					STATUSMSG("StateGpscIdle: Do nothing, for Assist Inj as seq is in progress");
					p_zSmlcAssist->u_AssistPendingFlag = TRUE;

				}
				else if (pGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_SESSION_WAIT_FOR_LOCATION_REQUEST)
				{
					STATUSMSG("StateGpscIdle: waiting for loc req, begin Assist Inj");
					pGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_REQUEST_TIME;
					gpsc_session_sequence(p_zGPSCControl,E_GPSC_SEQ_SESSION_REQUEST_TIME);
				}
				else
				{
					STATUSMSG("StateGpscIdle: being with ref clock");
					pGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_ACTIVE_APPLY_REF_CLOCK;
					gpsc_active_sequence(p_zGPSCControl,E_GPSC_SEQ_ACTIVE_APPLY_REF_CLOCK);
				}

			}
			break;

		/* Product Line Test */
		case E_GPSC_EVENT_PLT:
			{
				/* set current event in SM as E_GPSC_EVENT_PLT which ensure PLT test started */
				if(pGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_READY_IDLE)
					{
					pGpscSm->e_GpscCurrEvent = E_GPSC_EVENT_PLT;
					}

	
			/*  Send PLT request inject to GPS receiver */
				p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_PLT;
				gpsc_mgp_tx_prodlinetest_req(p_zGPSCControl);

				if (gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK)==FALSE)
				{
					FATAL_ERROR(GPSC_TX_INITIATE_FAIL); 
					GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
					return GPSC_TX_INITIATE_FAIL;
				}
				gpsc_timer_start(p_zGPSCControl, C_TIMER_PLT_SEQUENCE, p_zProductlineTest->timeout);
				
			}
			break;

		case E_GPSC_EVENT_SESSION_SLEEP:
			{
				/* move to GPSC sleep sequence */
				p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_GPS_SLEEP;
				gpsc_sleep_sequence(p_zGPSCControl, E_GPSC_SEQ_GPS_SLEEP);
				nextState = E_GPSC_STATE_IDLE;
			}
			break;

		case E_GPSC_EVENT_SESSION_WAKEUP:
			{
				STATUSMSG("STATUS: Start Active Sequence from Session Sleep\n");
				/* GPS wakeup starting active sequence */
				pGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_ACTIVE_APPLY_REF_CLOCK;
				gpsc_active_sequence(p_zGPSCControl,E_GPSC_SEQ_ACTIVE_APPLY_REF_CLOCK);
			}
			break;

		case E_GPSC_EVENT_SHUTDOWN:
			{
				/*Reset All timers*/
				gpsc_timer_stop(p_zGPSCControl,C_TIMER_ALL);
				
				/* Release GPSC memory to be done after returning from this */
				p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SHUTDOWN_COMPLETE;
			
			}
			break;

		case E_GPSC_EVENT_ENTER_FWD:
			{
		         p_zGPSCState->u_FowardMode = TRUE;
                         nextState = E_GPSC_STATE_FOWARD;
			}
			break;
			
		default:
			STATUSMSG("ERROR GPSC Event:%s NOT EXPECTED in State:%s\n",GpscSmEvent(event),GpscSmState(pGpscSm->e_GpscCurrState));
			
	}
	return nextState;
}

E_GPSC_SM_STATE StateGpscSleep(gpsc_sm *pGpscSm, E_GPSC_SM_EVENT event)
{
	gpsc_ctrl_type* p_zGPSCControl = (gpsc_ctrl_type*) pGpscSm->hGpsc;
        gpsc_state_type      *p_zGPSCState = p_zGPSCControl->p_zGPSCState;
	E_GPSC_SM_STATE nextState = E_GPSC_STATE_SLEEP;

	ALOGI("+++ %s: State [%s], Event: [%s] +++\n", __FUNCTION__, GpscSmState(pGpscSm->e_GpscCurrState), GpscSmEvent(event) );

	switch(event) 
	{
		/* E_GPSC_EVENT_READY_COMPLETE - gpsc is in ready state while GPS receiver is in OFF state */
		case E_GPSC_EVENT_READY_COMPLETE:
			{
				pGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_READY_SLEEP;
				/* send ready/sleep sequence complete message to navc or gpsm */
				gpsc_ready_complete(pGpscSm->hGpsc);
			}
			break;

		case E_GPSC_EVENT_LOCATION_START:
		case E_GPSC_EVENT_ASSIST_INJECT:
			{
				if(pGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_READY_SLEEP)
				{
					pGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_WAIT_GPS_WAKEUP;
					gpsc_wakeup_sequence(pGpscSm->hGpsc,E_GPSC_SEQ_WAIT_GPS_WAKEUP);
				}
				else if (pGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_GPS_WAIT_SLEEP)
					{
					/*If we are waiting on a sleep timer, disable it and move to operation mode*/
					/* stop timer which was started in E_GPSC_SEQ_GPS_WAIT_SLEEP */
					gpsc_timer_stop(p_zGPSCControl,C_TIMER_SEQUENCE);

					pGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_WAIT_GPS_WAKEUP;
					gpsc_wakeup_sequence(pGpscSm->hGpsc,E_GPSC_SEQ_WAIT_GPS_WAKEUP);
					
					}
				else
					{
					STATUSMSG("ERROR: Location/Assistance Inject Request failed in SLEEP STATE\n");
					}
				
			}
			break;
			
		case E_GPSC_EVENT_PLT:
			{
				/* set current event in SM as E_GPSC_EVENT_PLT which ensure PLT test started */
				if(pGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_READY_SLEEP)
					{
					pGpscSm->e_GpscCurrEvent = E_GPSC_EVENT_PLT;
					}
				/* start wakeup sequence */
				p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_WAIT_GPS_WAKEUP;
				gpsc_wakeup_sequence(p_zGPSCControl,E_GPSC_SEQ_WAIT_GPS_WAKEUP);
			}
			break;

		case E_GPSC_EVENT_SESSION_WAKEUP:
			{
				/* GPS wakeup by starting wakeup sequence */
				pGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_WAIT_GPS_WAKEUP;
				gpsc_wakeup_sequence(p_zGPSCControl,E_GPSC_SEQ_WAIT_GPS_WAKEUP);
			}
			break;

		case E_GPSC_EVENT_SHUTDOWN:
			{
				/* Reset All timers */
				gpsc_timer_stop(p_zGPSCControl, C_TIMER_ALL);

				/* Release GPSC memory to be done after returning from this */
				p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SHUTDOWN_COMPLETE;
			
			}
			break;

                case E_GPSC_EVENT_ENTER_FWD:
                        {
                         p_zGPSCState->u_FowardMode = TRUE;
                         nextState = E_GPSC_STATE_FOWARD;
                        }
                        break;

		default:
			STATUSMSG("ERROR GPSC Event:%s NOT EXPECTED in State:%s\n",GpscSmEvent(event),GpscSmState(pGpscSm->e_GpscCurrState));
	}
	return nextState;
}

E_GPSC_SM_STATE StateGpscActive(gpsc_sm* pGpscSm, E_GPSC_SM_EVENT event)
{
	gpsc_ctrl_type* p_zGPSCControl = (gpsc_ctrl_type*) pGpscSm->hGpsc;
	gpsc_sess_cfg_type *p_zSessConfig = p_zGPSCControl->p_zSessConfig;
    gpsc_sess_specific_cfg_type * p_zSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;
	gpsc_cfg_type*	p_zGPSCConfig = p_zGPSCControl->p_zGPSCConfig;
	gpsc_smlc_assist_type*  p_zSmlcAssist  = p_zGPSCControl->p_zSmlcAssist;
	E_GPSC_SM_STATE nextState = E_GPSC_STATE_ACTIVE;
	gpsc_state_type* p_zGPSCState = p_zGPSCControl->p_zGPSCState;
	gpsc_event_cfg_type * p_zEventCfg = p_zGPSCControl->p_zEventCfg;
	U16 w_NMEABitmap = 0;
	EMcpfRes e_result;

	ALOGI("+++ %s: State [%s], Event: [%s] +++\n", __FUNCTION__, GpscSmState(pGpscSm->e_GpscCurrState), GpscSmEvent(event) );

	switch(event) 
	{
		case E_GPSC_EVENT_ACTIVE:
			{
			/* Send a command to enable NAVC command Queue */
			e_result = mcpf_SendMsg (p_zGPSCControl->p_zSysHandlers->hMcpf, 
								 TASK_NAV_ID, 
								 NAVC_QUE_INTERNAL_EVT_ID, 
								 TASK_EXT_ID, 
								 0, 
								 NAVC_EVT_GPSC_IN_ACTIVE_STATE, 
								 0, 
								 0, 
								 NULL);

			/* for standalone autonomous session that will not inject any network assistance */
			if( (p_zSessConfig->u_SmlcAssisPermit == C_SMLC_ASSIS_NOT_PERMITTED) && 
				(p_zSmlcAssist->w_AvailabilityFlags == 0)
				)
				{
				p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE;
				}
			else
				{
				/* start session sequence */
				p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_REQUEST_TIME;
				}
				gpsc_session_sequence(p_zGPSCControl,p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq);
			}
			break;
			
		case E_GPSC_EVENT_ASSIST_INJECT:
			{
				if(pGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_SESSION_REQUEST_TIME ||
						pGpscSm->e_GpscCurrSeq == E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE)
				{
					//do nothing as assistance injection is in progress
					p_zSmlcAssist->u_AssistPendingFlag = TRUE;
				}
				else
				{
					p_zSmlcAssist->u_AssistPendingFlag = FALSE;
				}
			p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_REQUEST_TIME;
			gpsc_session_sequence(p_zGPSCControl,E_GPSC_SEQ_SESSION_REQUEST_TIME);
     		}
			break;

		case E_GPSC_EVENT_APPLY_ASSISTANCE:
			{
	//			p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE;
		//		gpsc_session_sequence(p_zGPSCControl,E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE);
			}
			break;

		case E_GPSC_EVENT_LOCATION_START:
			{
    			/* following to support parallel sessions */ 
				if (p_zSessConfig->w_TotalNumSession > 1)
				{
					/* Update NMEA control */
               w_NMEABitmap = gpsc_sess_get_nmea_bitmap(p_zSessConfig);

					if (w_NMEABitmap)
					{
						/* enable NMEA */
						gpsc_mgp_tx_nmea_ctrl(p_zGPSCControl, w_NMEABitmap);

						if (gpsc_comm_transmit(p_zGPSCControl, C_AI2_NO_ACK) == FALSE)
						{
							FATAL_ERROR(GPSC_TX_INITIATE_FAIL); 
							GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);

							return GPSC_TX_INITIATE_FAIL;
						}
					}

					/* existing session, some parameters in existing session are modified */
						/* change Event Configuration */
					p_zSessConfig->u_ReConfigFlag = TRUE;
					gpsc_sess_update_period(p_zGPSCControl,C_PERIOD_NOTSET);
					p_zSessConfig->u_ReConfigFlag = FALSE;

	        			/* update timeout if first fix is already obtained */
	        			if (p_zSessSpecificCfg->u_WaitFirstFix == FALSE)
	        			{
							if((p_zGPSCConfig->recv_min_rep_period == 500) && (p_zSessSpecificCfg->w_ReportPeriod==0))
							{
							   p_zSessSpecificCfg->q_ValidFixTimeout = (p_zGPSCConfig->recv_min_rep_period);
							}
							else
							{
	        				   p_zSessSpecificCfg->q_ValidFixTimeout = (U32)(p_zSessSpecificCfg->w_ReportPeriod * 1000);
							}
	        			}
					}
				else
				{
					/* handle location request when waiting for location request after applying assistance */ 
					pGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_REQUEST_TIME;
					gpsc_session_sequence(pGpscSm->hGpsc, E_GPSC_SEQ_SESSION_REQUEST_TIME);
				}
			}
			break;

		case E_GPSC_EVENT_LOCATION_STOP:
			{

				/* check to delete only one session or all session */
				if(C_SESSION_INVALID != p_zGPSCControl->p_zGPSCState->q_LocStopSessId)
					{
					 /* check timer status */
					 if(gpsc_timer_status(p_zGPSCControl, C_TIMER_PERIODIC_REPORTING) == GPSC_FAIL)
					  {
					     /*Timer not running, delete the node*/
		    			 gpsc_session_del_node(p_zGPSCControl->p_zSessConfig, p_zGPSCControl->p_zGPSCState->q_LocStopSessId);
						 if(p_zSessConfig->u_BLF_State == C_BLF_STATE_ENABLED)
						 {
							/* If BLF got enabled, disable Extended reports */
							p_zEventCfg->u_Ext_reports = 0;
							p_zSessConfig->u_ReConfigFlag = TRUE;
						 }
						 gpsc_sess_update_period(p_zGPSCControl, C_PERIOD_NOTSET);

					  }
					  else
					  {
						  /*Just set fix counter as 0 and delete the session after you get the first report from Receiver*/
                          gpsc_sess_specific_cfg_type*  p_zSessSpecificCfg =
								 gpsc_session_get_node(p_zGPSCControl->p_zSessConfig, p_zGPSCControl->p_zGPSCState->q_LocStopSessId);
                                                         if (p_zSessSpecificCfg != NULL)
							 p_zSessSpecificCfg->w_FixCounter = 0;
							 p_zGPSCState->u_FixStopRequestPending = TRUE;

					  }
					}
				else
					{

					 if(gpsc_timer_status(p_zGPSCControl, C_TIMER_PERIODIC_REPORTING) == GPSC_FAIL)
					  {
					     /*Timer not running, delete the node*/
						  STATUSMSG("Loc stop Deleting valid ID periodicreports not running");
		    			 gpsc_session_del_node(p_zGPSCControl->p_zSessConfig, p_zGPSCControl->p_zGPSCState->q_LocStopSessId);
						 if(p_zSessConfig->u_BLF_State == C_BLF_STATE_ENABLED)
						 {
							/* If BLF got enabled, disable Extednded reports */
							p_zEventCfg->u_Ext_reports = 0;
							p_zSessConfig->u_ReConfigFlag = TRUE;
						 }
						 gpsc_sess_update_period(p_zGPSCControl, C_PERIOD_NOTSET);
						 /* send location fix request confirmation only for one session delete request */
						 //gpsc_cmd_complete_cnf(GPSC_SUCCESS);
		//				 gpsc_app_loc_fix_stop_cnf(GPSC_SUCCESS, w_SessionTagId);

					  }
					  else
					  {


						gpsc_sess_specific_cfg_type*  p_zSessSpecificCfg=
							p_zGPSCControl->p_zSessConfig->p_zSessSpecificCfg;
		                 while(p_zSessSpecificCfg != NULL)
						 {
							p_zSessSpecificCfg->w_FixCounter = 0;
							p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type* )p_zSessSpecificCfg->p_nextSession;
						 }
		                 p_zGPSCState->u_FixStopRequestPending = TRUE;
					  }
					}
			
		  	if(gpsc_timer_status(p_zGPSCControl, C_TIMER_PERIODIC_REPORTING) != GPSC_FAIL)
		  	{
				 gpsc_timer_stop(p_zGPSCControl, C_TIMER_PERIODIC_REPORTING);
				  STATUSMSG("Loc stop Deleting valid ID periodicreports not running");
				 gpsc_session_del_node(p_zGPSCControl->p_zSessConfig, p_zGPSCControl->p_zGPSCState->q_LocStopSessId);
				 if(p_zSessConfig->u_BLF_State == C_BLF_STATE_ENABLED)
				 {
					/* If BLF got enabled, disable Extednded reports */
					p_zEventCfg->u_Ext_reports = 0;
					p_zSessConfig->u_ReConfigFlag = TRUE;
				 }
				 gpsc_sess_update_period(p_zGPSCControl, C_PERIOD_NOTSET);
				//gpsc_app_loc_fix_stop_cnf(GPSC_SUCCESS, w_SessionTagId);
		  	}
			
			if(p_zGPSCControl->p_zSessConfig->p_zSessSpecificCfg == NULL)
			{
                STATUSMSG("Loc stop Deleting aborted");
				p_zGPSCState->u_FixStopRequestPending = TRUE;
				gpsc_sess_coll_abort(p_zGPSCControl, C_ABORT_CAUSE_TERMINATED);
			}

		}
		break;

		case E_GPSC_EVENT_SHUTDOWN:
			{

				/*Reset All timers*/
				gpsc_timer_stop(p_zGPSCControl,C_TIMER_ALL);
				
				/* Release GPSC memory to be done after returning from this */
				p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SHUTDOWN_COMPLETE;
			
			}
			break;


		default:
			STATUSMSG("ERROR GPSC Event:%s NOT EXPECTED in State:%s\n",GpscSmEvent(event),GpscSmState(pGpscSm->e_GpscCurrState));
			
	}
return nextState;	
}

E_GPSC_SM_STATE StateGpscFoward(gpsc_sm *pGpscSm, E_GPSC_SM_EVENT event)
{
	gpsc_ctrl_type* p_zGPSCControl = (gpsc_ctrl_type*) pGpscSm->hGpsc;
	E_GPSC_SM_STATE nextState = E_GPSC_STATE_ACTIVE;
	gpsc_state_type* p_zGPSCState = p_zGPSCControl->p_zGPSCState;

	ALOGI("+++ %s: State [%s], Event: [%s] +++\n", __FUNCTION__, GpscSmState(pGpscSm->e_GpscCurrState), GpscSmEvent(event) );

	switch(event) 
	{
		case E_GPSC_EVENT_EXIT_FWD:
		{
		  p_zGPSCState->u_FowardMode = FALSE;
          nextState = E_GPSC_STATE_IDLE;
     	}
		break;

		default:
			STATUSMSG("ERROR GPSC Event:%s NOT EXPECTED in State:%s\n",GpscSmEvent(event),GpscSmState(pGpscSm->e_GpscCurrState));
			
	}
	return nextState;
}
E_GPSC_SM_STATE StateGpscShutdown(gpsc_sm* pGpscSm, E_GPSC_SM_EVENT event)
{
	E_GPSC_SM_STATE nextState = E_GPSC_STATE_SHUTDOWN;
	gpsc_ctrl_type* p_zGPSCControl = (gpsc_ctrl_type*) pGpscSm->hGpsc;
	U8 u_Result = 0;

	ALOGI("+++ %s: State [%s], Event: [%s] +++\n", __FUNCTION__, GpscSmState(pGpscSm->e_GpscCurrState), GpscSmEvent(event) );

	switch(event) 
	{

		case E_GPSC_EVENT_INIT:
		{
			/* initialize GPSC */
			STATUSMSG("GPSC SM Status: Starting Init Sequence");
			u_Result = (U8) gpsc_init_sequence(pGpscSm->hGpsc, E_GPSC_SEQ_INIT);
			
			if(GPSC_SUCCESS == u_Result)
			{
				pGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_INIT;
				nextState = E_GPSC_STATE_SHUTDOWN;				
			}
			else
			{
				FATAL_ERROR(GPSC_UNITIALIZED_FAIL);
				p_zGPSCControl->p_zGpscSm->e_GpscCurrState = E_GPSC_STATE_SHUTDOWN;
				GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
			}
		}
		break;
		
		case E_GPSC_EVENT_READY_START:
		{
			/* start ready sequence */
			STATUSMSG("GPSC SM Status: Starting Ready Sequence");
			pGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_READY_START;
			u_Result = (U8) gpsc_ready_sequence(pGpscSm->hGpsc, E_GPSC_SEQ_READY_START);

			if(u_Result == GPSC_SUCCESS)
				{
				/* send GPSC READY pending message to NAVC or GPSM */
//				gpsc_cmd_complete_cnf(GPSC_PENDING);
				}
			nextState = E_GPSC_STATE_SHUTDOWN;

		}
		break;

		case E_GPSC_EVENT_NVS_INJECTION:
		{
			pGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_READY_GPS_INJECT_NVS;
			/* inject and request almanac and ephmeris data */
			gpsc_ready_sequence(p_zGPSCControl, E_GPSC_SEQ_READY_GPS_INJECT_NVS);
		}
		break;

		case E_GPSC_EVENT_RESET:
			{
				/*Reset All timers*/
			    gpsc_timer_stop(p_zGPSCControl,C_TIMER_ALL);

				p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_READY_INJECT_INIT_CONFIG;
                gpsc_ready_sequence(p_zGPSCControl,E_GPSC_SEQ_READY_INJECT_INIT_CONFIG);
			}
			break;

		case E_GPSC_EVENT_SHUTDOWN:
		{
			/*Reset All timers*/
			gpsc_timer_stop(p_zGPSCControl,C_TIMER_ALL);
			
			/* Release GPSC memory to be done after returning from this */
			p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SHUTDOWN_COMPLETE;
			}
			break;
		
		
		default:
			STATUSMSG("ERROR GPSC Event:%s NOT EXPECTED in State:%s\n",GpscSmEvent(event),GpscSmState(pGpscSm->e_GpscCurrState));
			
	}
	return nextState;
}

char* GpscSmState(E_GPSC_SM_STATE eGpscState)
{
	char *rc;
	switch(eGpscState) 
	{
	case E_GPSC_STATE_SHUTDOWN:
		rc =  "SHUTDOWN   ";
		break;
	case E_GPSC_STATE_IDLE:
		rc =  "IDLE   ";
		break;
	case E_GPSC_STATE_SLEEP:
		rc =  "SLEEP";
		break;
	case E_GPSC_STATE_ACTIVE:
		rc =  "ACTIVE";
		break;
	case E_GPSC_STATE_FOWARD:
		rc =  "FOWARD";
		break;
	default:
		rc = "UNKNOWN";
	}
	
	return rc;
}

char* GpscSmEvent(E_GPSC_SM_EVENT eGpscEvent)
{
	char *rc;
	switch(eGpscEvent) 
	{
	case E_GPSC_EVENT_SHUTDOWN:
		rc =  "SHUTDOWN  ";
		break;
	case E_GPSC_EVENT_INIT:
		rc =  "INIT   ";
		break;
	case E_GPSC_EVENT_READY_START:
		rc =  "READY_START    ";
		break;
	case E_GPSC_EVENT_READY_COMPLETE:
		rc =  "READY_COMPLETE ";
		break;
	case E_GPSC_EVENT_IDLE:
		rc =  "IDLE ";
		break;
	case E_GPSC_EVENT_GPS_WAKEUP:
		rc =  "GPS_WAKEUP ";
		break;
	case E_GPSC_EVENT_LOCATION_START:
		rc =  "LOCATION_START ";
		break;
	case E_GPSC_EVENT_LOCATION_STOP:
		rc =  "LOCATION_STOP ";
		break;
	case E_GPSC_EVENT_LOCATION_COMPLETE:
		rc =  "LOCATION_COMPLETE ";
		break;
	case E_GPSC_EVENT_ASSIST_INJECT:
		rc =  "ASSIST_INJECT ";
		break;
	case E_GPSC_EVENT_APPLY_ASSISTANCE:
		rc =  "APPLY ASSISTANCE";
		break;
	case E_GPSC_EVENT_ACTIVE:
		rc =  "ACTIVE ";
		break;
	case E_GPSC_EVENT_SESSION_SLEEP:
		rc =  "SESSION_SLEEP";
		break;
	case E_GPSC_EVENT_SESSION_WAKEUP:
		rc =  "SESSION_WAKEUP";
		break;
	case E_GPSC_EVENT_PLT:
		rc =  "PLT ";
		break;
	case E_GPSC_EVENT_ENTER_FWD:
		rc = "ENTER_FWD";
		break;
	case E_GPSC_EVENT_EXIT_FWD:
		rc = "EXIT_FWD";
		break;
	default:
		rc =  "UNKNOWN ";
		break;
	}
	return rc;
}

void gpsc_state_process_ack(gpsc_ctrl_type*	p_zGPSCControl)
{

	gpsc_state_type* p_zGPSCState = p_zGPSCControl->p_zGPSCState;
	STATUSMSG("GPSC ACK received, State:%s, Sequence:%s\n",GpscSmState(p_zGPSCControl->p_zGpscSm->e_GpscCurrState),GpscSmSequence(p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq));
	/* process ack in ready sequence */
	switch(p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq)
		{
		case E_GPSC_SEQ_READY_START:
		case E_GPSC_SEQ_READY_WAIT_FOR_WAKEUP:
		case E_GPSC_SEQ_READY_BAUD_CHANGE:
		case E_GPSC_SEQ_READY_BEGIN_DOWNLOAD:
		case E_GPSC_SEQ_READY_READ_INJECT_RECORD:
		case E_GPSC_SEQ_READY_SKIP_DOWNLOAD:
		case E_GPSC_SEQ_READY_DOWNLOAD_COMPLETE:
		case E_GPSC_SEQ_SESSION_REQUEST_TIME:
		case E_GPSC_SEQ_SESSION_WAIT_FOR_LOCATION_REQUEST:
		case E_GPSC_SEQ_SESSION_GET_ASSISTANCE:
		case E_GPSC_SEQ_SESSION_ON:
			{
			   STATUSMSG(" gpsc_state_process_ack() ACK not expected, State:%s, Sequence:%s\n",GpscSmState(p_zGPSCControl->p_zGpscSm->e_GpscCurrState),GpscSmSequence(p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq));
			}
			break;
		case E_GPSC_SEQ_READY_INJECT_INIT_CONFIG:
			{
				p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_READY_GPS_INJECT_NVS;
				GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_NVS_INJECTION);
			}
			break;

		case E_GPSC_SEQ_READY_GPS_INJECT_NVS:
			{
			p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_READY_GPS_INJECT_NVS;
			/* continue NVS data injection */
			GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_NVS_INJECTION);
			}
			break;

		case E_GPSC_SEQ_READY_GPS_INJECT_NVS_COMPLETE:
			{
			/* GPS Receiver IDLE responce received, move to IDLE state */
			p_zGPSCControl->p_zGpscSm->e_GpscCurrState = E_GPSC_STATE_IDLE;
			GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_IDLE);
			}
			break;

		case E_GPSC_SEQ_READY_GPS_WAIT_SLEEP:
			{
				/* GPS Receiver  SLEEP responce received, move to IDLE state */
				p_zGPSCControl->p_zGpscSm->e_GpscCurrState = E_GPSC_STATE_SLEEP;
				p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_READY_SLEEP;
				GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_READY_COMPLETE);
			}
			break;

		/* Wakeup sequence */
		case E_GPSC_SEQ_GPS_WAKEUP:
			{
				/* Before requesting time, receiver needs to compelete RTC calib to compute total sleep time
					this takes about 800ms, start the timer here */
				p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_GPS_WAKEUP_COMPLETE;
				gpsc_wakeup_sequence(p_zGPSCControl,E_GPSC_SEQ_GPS_WAKEUP_COMPLETE);

			}
			break;
		case E_GPSC_SEQ_GPS_WAKEUP_INJ_ASSISTANCE:
			{
				   gpsc_wakeup_sequence(p_zGPSCControl,E_GPSC_SEQ_GPS_WAKEUP_INJ_ASSISTANCE);
			}
			break;
		case E_GPSC_SEQ_GPS_WAIT_SLEEP:
			{
				STATUSMSG("E_GPSC_SEQ_GPS_WAIT_SLEEP : do nothing, wait for ASYNC OFF");
#if 0
				/* receiver put to sleep */
				gpsc_sleep_sequence(p_zGPSCControl,E_GPSC_SEQ_GPS_WAIT_SLEEP);
				//send stop confirmation
				STATUSMSG("E_GPSC_SEQ_GPS_WAIT_SLEEP : Sending gpsc_app_stop_fix_ind");
				gpsc_app_stop_fix_ind();

#endif
			}
			break;
		case E_GPSC_SEQ_GPS_IDLE:
			{
				/* receiver put to Idle */
				gpsc_sleep_sequence(p_zGPSCControl,E_GPSC_SEQ_GPS_IDLE);
				//send stop confirmation
			}
			break;
		/* active sequence */
		case E_GPSC_SEQ_ACTIVE_WAIT_CALIB:
			{
				p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_WAIT_ACTIVE;
				gpsc_active_sequence(p_zGPSCControl,E_GPSC_SEQ_WAIT_ACTIVE);
			}
			break;

		case E_GPSC_SEQ_ACTIVE_INJECT_ASSIST_SLEEP_FIX:
			{
				p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_ACTIVE_INJECT_ASSIST_SLEEP_FIX;
				gpsc_active_sequence(p_zGPSCControl,E_GPSC_SEQ_ACTIVE_INJECT_ASSIST_SLEEP_FIX);
			}
			break;

		case E_GPSC_SEQ_WAIT_ACTIVE:
			{
				STATUSMSG("Status: GPS RECEIVER ON in case of RTI Enable");
				p_zGPSCControl->p_zGpscSm->e_GpscCurrState = E_GPSC_STATE_ACTIVE;
				GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_ACTIVE);
			}
			break;

		
		case E_GPSC_SEQ_SESSION_INJECT_TIME:
			{
				/* ack E_GPSC_SEQ_SESSION_INJECT_TIME received, stop timer */

				/* If the wait for the clk report is true then don't move to next state
						   be in the same state*/
		        if( p_zGPSCState->u_WaitForClkReport == FALSE)
		        {
				p_zGPSCControl->p_zGpscSm->e_GpscCurrSeq = E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE;
				gpsc_session_sequence(p_zGPSCControl,E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE);
			    }
			
			}
			break;

		case E_GPSC_SEQ_SESSION_CONFIG:
			{
			/*	if(FALSE == p_zGPSCControl->p_zGPSCState->u_SkipCalib)
				{
				// this ack received after injecting Osc param to GPS receiver 
				//give 512 milliseconds have passed since last CE OSC injection
				gpsc_timer_start(p_zGPSCControl,
				C_TIMER_CLOCK_CALIB_WAIT, C_TIMER_CLOCK_CALIB_WAIT_PERIOD);
				}
				else
				{*/
				STATUSMSG("Status: Skip calib and move to session ON");
				gpsc_session_sequence(p_zGPSCControl,E_GPSC_SEQ_SESSION_ON);
				//}
			}
			break;
		case E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE:
			{
				//gpsc_session_apply_assistance(p_zGPSCControl);
				gpsc_session_sequence(p_zGPSCControl,E_GPSC_SEQ_SESSION_APPLY_ASSISTANCE);

			}
			break;

		

		case E_GPSC_SEQ_SESSION_SLEEP:
			{
				/* start session sleep timer */
				SAPCALL(gpsc_os_timer_start_ind);
#ifdef LARGE_UPDATE_RATE
				gpsc_timer_start(p_zGPSCControl, C_TIMER_SESSION_SLEEP, (U32) p_zGPSCControl->p_zSessConfig->q_SleepDuration);
#endif	
				/* GPS receiver is IDLE so move GPSC to IDLE state */
				p_zGPSCControl->p_zGpscSm->e_GpscCurrState = E_GPSC_STATE_IDLE;
				GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SESSION_SLEEP);
			}
			break;
		case E_GPSC_SEQ_SESSION_LOCATION_COMPLETE:
			{
				/* GPS Receiver IDLE responce received, move to IDLE state */
				p_zGPSCControl->p_zGpscSm->e_GpscCurrState = E_GPSC_STATE_IDLE;
				GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_IDLE);
                                /* REJO commented the following to prevent early location start
                                   requests before completing the whole stop sequence */
                                // gpsc_app_stop_fix_ind(p_zGPSCControl->p_zGPSCState->q_LocStopSessId);
				STATUSMSG("Status: Send Notification to Priority handler \n");
				gpsc_send_noti_to_priority_handler(p_zGPSCControl);

			}
			break;
		case E_GPSC_SEQ_PLT:
			{
				
				/* send PLT test complete confirmation to NAVC */
				gpsc_cmd_complete_cnf(GPSC_SUCCESS);
				/* GPS Receiver IDLE responce received, move to IDLE state */
				p_zGPSCControl->p_zGpscSm->e_GpscCurrState = E_GPSC_STATE_IDLE;
				GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_IDLE);
			}
			break;

		default:
			{
				 STATUSMSG("Error Status: gpsc_state_process_ack() ACK not handled");
			}
			break;
			
		}
}


void gpsc_process_eph_validate(gpsc_ctrl_type*	p_zGPSCControl)
{

	McpU8 u_i, reRequestAll = 1;
	pe_RawEphemeris *p_zSmlcRawEph;
	gpsc_smlc_assist_type*	p_zSmlcAssist  = p_zGPSCControl->p_zSmlcAssist;
	gpsc_db_type*		p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_db_gps_clk_type* p_zDBGpsClock = &p_zGPSCDatabase->z_DBGpsClock;
	gpsc_custom_struct* p_zCustom = p_zGPSCControl->p_zCustomStruct;
	pe_RawEphemeris *p_RawEph;
	McpU32  d_ephFullToe =0; 
	McpU32 d_CurrentAccGpsSec = 0;
	McpU32	smlc_toe_exp_bmap = 0; /*This bitmap is set when the toe is expired in SMLC database*/
	McpU32	db_toe_exp_bmap = 0; /*This bitmap is set when the toe is expired in GPSC database*/
	McpU8	isEphValid	= FALSE;

	if(p_zCustom->custom_assist_availability_flag & C_EPH_AVAIL)
	{
	
		if ((!p_zDBGpsClock->u_Valid) || (p_zDBGpsClock->w_GpsWeek == C_GPS_WEEK_UNKNOWN ))
		{
			/*re-start the timer and return as the GPS time is not known yet*/
			STATUSMSG("gpsc_process_eph_validate: GPS Time Not Known, restarted EPH_RE_REQUEST timer");
			return;
		}

		/*GPS Time known - process for each valid ephemeris in SMLC database and GPSC database*/
		for(u_i=0;u_i<N_SV;u_i++)
		{
			p_zSmlcRawEph = &p_zSmlcAssist->z_SmlcRawEph[u_i];
			if( FALSE == p_zSmlcRawEph->toe_expired)
			{
				d_ephFullToe = (p_zSmlcRawEph->w_Toe * 16.0);
				d_CurrentAccGpsSec  = (p_zDBGpsClock->q_GpsMs /1000);
		
				isEphValid = is_eph_valid_for_gpssec(d_ephFullToe, d_CurrentAccGpsSec);
			
				if(isEphValid == TRUE)
				{
					reRequestAll = 0;
					smlc_toe_exp_bmap &= ~((McpU32) pow(2, u_i));
				}
				else
				{
					p_zSmlcRawEph->toe_expired = TRUE;
					smlc_toe_exp_bmap |= (McpU32) pow(2, u_i);
					STATUSMSG("SMLC TOE EXP: u_i: %d, sv: %d, fullToe :%ld, currTime: %ld",
						u_i, p_zSmlcRawEph->u_Prn, d_ephFullToe, d_CurrentAccGpsSec);
				}
			}
			else
			{
				smlc_toe_exp_bmap |= (McpU32) pow(2, u_i);
			}

			/* process for each ephemeris expiry in Database - if DB eph is valid and toe is not expired then check for expiry*/
			p_RawEph = &p_zGPSCDatabase->z_DBEphemeris[u_i].z_RawEphemeris;

			if( (p_zGPSCDatabase->z_DBEphemeris[u_i].u_Valid) && (FALSE == p_RawEph->toe_expired))
			{
				d_ephFullToe = (DBL) (p_RawEph->w_Toe * 16.0);
				d_CurrentAccGpsSec  = (DBL) (p_zDBGpsClock->q_GpsMs / 1000);

				isEphValid = is_eph_valid_for_gpssec(d_ephFullToe, d_CurrentAccGpsSec);
				
				if(isEphValid == TRUE)
				{
					reRequestAll = 0;
					db_toe_exp_bmap &= ~ ((McpU32) pow(2, u_i));
				}
				else
				{
					p_RawEph->toe_expired = TRUE;
					db_toe_exp_bmap |= (McpU32) pow(2, u_i);
					STATUSMSG("DB TOE EXP: u_i: %d, sv: %d, fullToe :%ld, currTime: %ld",
						u_i, p_RawEph->u_Prn, d_ephFullToe, d_CurrentAccGpsSec);
				}
			}
			else
			{
				db_toe_exp_bmap |= (McpU32) pow(2, u_i);	
			}
		}

		STATUSMSG("gpsc_process_eph_validate: smlc_toe_exp: 0x%x, db_toe_exp: 0x%x ", smlc_toe_exp_bmap, db_toe_exp_bmap);

      }
	else
	{
		STATUSMSG("gpsc_process_eph_validate: EPH Not Available ASSIST_AVAIL: 0x%x", p_zCustom->custom_assist_availability_flag);
	}
}


