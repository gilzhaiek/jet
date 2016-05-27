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
 * FileName			:	gpsc_sequence.h
 *
 * Description     	:	This file includes declaration and initialization for state sequnce. 
 *
 * Author         	: 	Viren Shah - virenshah@ti.com
 *
 *
 ******************************************************************************
 */
#ifndef _GPSC_SEQUENCE_H
#define _GPSC_SEQUENCE_H
#include "gpsc_sap.h"
#include "gpsc_init.h"

/************************************************************************
 * Types
 ************************************************************************/

/** 
 * \fn     NavcSmCreate
 * \brief  Initialize NAVC state machine and issue Create event
 * 
 * the function initialize the SM data structures and call the Create
 * Event handler. Upon complete the state will be NAVC_ST_IDLE. 
 * 
 * \note
 * \param	hNavc 	  - pointer to NAVC object
 * \param	hMcpf	  - pointer to MCPF object
 * \return	handle_t  - pointer to allocated SM context (NULL upon failure)
 * \sa
 */



/** 
 * \fn     navcSm
 * \brief  Send Event to NAVC state machine
 * 
 * The function is used to issue event to NAVC SM. It will process the relevant 
 * action and move to the next state.
 *
 * \note
 * \param	hNavcSm   - pointer to NAVC SM object
 * \param	event     - event to be issued
 * \return	void
 * \sa
 */
T_GPSC_result gpsc_init_sequence(gpsc_ctrl_type*	p_zGPSCControl, E_GPSC_SM_SEQ e_GpscSmSequence);

T_GPSC_result gpsc_ready_sequence(gpsc_ctrl_type*	p_zGPSCControl, E_GPSC_SM_SEQ e_GpscSmSequence);

T_GPSC_result gpsc_sleep_sequence(gpsc_ctrl_type* p_zGPSCControl, E_GPSC_SM_SEQ e_GpscSmSequence);

T_GPSC_result gpsc_wakeup_sequence(gpsc_ctrl_type*	p_zGPSCControl, E_GPSC_SM_SEQ e_GpscSmSequence);

T_GPSC_result gpsc_active_sequence(gpsc_ctrl_type* p_zGPSCControl, E_GPSC_SM_SEQ e_GpscSmSequence);

T_GPSC_result gpsc_session_sequence(gpsc_ctrl_type* p_zGPSCControl, E_GPSC_SM_SEQ e_GpscSmSequence);


T_GPSC_result gpsc_inject_comm_protocol (gpsc_ctrl_type*	p_zGPSCControl);

T_GPSC_result gpsc_inject_init_config(gpsc_ctrl_type*	p_zGPSCControl);

T_GPSC_result gpsc_inject_gps_idle(gpsc_ctrl_type*	p_zGPSCControl);

T_GPSC_result gpsc_inject_gps_sleep(gpsc_ctrl_type*	p_zGPSCControl);

T_GPSC_result gpsc_wakeup_receiver (gpsc_ctrl_type* p_zGPSCControl);

T_GPSC_result gpsc_calibration_control(gpsc_ctrl_type* p_zGPSCControl);

void gpsc_init_event_config(gpsc_ctrl_type* p_zGPSCControl);

T_GPSC_result gpsc_session_config(gpsc_ctrl_type* p_zGPSCControl);

T_GPSC_result gpsc_ready_complete(gpsc_ctrl_type* p_zGPSCControl);

T_GPSC_result gpsc_activation_sequence_start(gpsc_ctrl_type* p_zGPSCControl);

T_GPSC_result gpsc_session_apply_assistance(gpsc_ctrl_type* p_zGPSCControl);

T_GPSC_result gpsc_session_wait_for_loc_req(gpsc_ctrl_type* p_zGPSCControl);

T_GPSC_result gpsc_get_assistance_from_network(gpsc_ctrl_type* p_zGPSCControl);

T_GPSC_result gpsc_activate_presession(gpsc_ctrl_type* p_zGPSCControl);

T_GPSC_result gpsc_session_on(gpsc_ctrl_type* p_zGPSCControl);
extern T_GPSC_result gpsc_drv_sensor_calib_req_timepulse_refclk_res (U8 u_OperType);


#endif //_GPSC_STATE_H
