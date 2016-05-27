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
 * FileName			:	gpsc_state.h
 *
 * Description     	:	This file includes declaration and initialization for new state machine. 
 *
 * Author         	: 	Viren Shah - virenshah@ti.com
 *
 *
 ******************************************************************************
 */
#ifndef _GPSC_STATE_H
#define _GPSC_STATE_H
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
void * GpscSmCreate(void * hGpsc, gpsc_sys_handlers* p_sysHandlers);



/** 
 * \fn     gpsc_state_process_ack
 * \brief  Process ACK from receiver.
 * 
 * The function is used to process the ack. 
 *
 * \note
 * \param	gpsc_ctrl_type*   - pointer to GPSC Control object
 * \param	
 * \return	void
 * \sa
 */

void gpsc_state_process_ack(gpsc_ctrl_type*	p_zGPSCControl);

/** 
 * \fn     gpsc_process_eph_validate
 * \brief  Process Ephemeris Re-Request
 * 
 * The function check EPH validation
 *
 * \note
 * \param	gpsc_ctrl_type*   - pointer to GPSC Control object
 * \param	
 * \return	void
 * \sa
 */
void gpsc_process_eph_validate(gpsc_ctrl_type*	p_zGPSCControl);


#define FATAL_INIT(error_code) {\
  gpsc_diagnostic(GPSC_DIAG_ERROR_WARNING, "FATAL ERROR %d", error_code); \
  gpsc_init_flags(p_zGPSCControl);\
  gpsc_init_smlc(p_zGPSCControl);\
  gpsc_os_fatal_error_ind(error_code);\
  }

#define FATAL_ERROR(error_code) {\
  gpsc_diagnostic(GPSC_DIAG_ERROR_WARNING, "FATAL ERROR %d", error_code); \
  gpsc_os_fatal_error_ind(error_code);\
  }




#endif //_GPSC_STATE_H
