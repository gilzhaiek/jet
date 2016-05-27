/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_evtHandler.h
 *
 * Description      :
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */


#ifndef __SUPLC_EVT_HANDLER_H__
#define __SUPLC_EVT_HANDLER_H__

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus*/

#include "mcpf_defs.h"
#include "suplc_defs.h"


/**
 * Function:        SUPLC_eventHandler
 * Brief:           Handles various events sent to SUPLC
 * Description:     This functions is called whenever a message intended for SUPLC is
                    put on its event queue.
 * Note:            External Function.
 * Params:          hSuplc - handle to SUPLC's control structure.
                    p_msg - Contains all details about the message.
 * Return:          Success: RES_SUCCESS.
                    Failure: RES_ERROR.
 */
EMcpfRes SUPLC_eventHandler(handle_t hSuplc, TmcpfMsg *p_msg);



/**
 * Function:        processEvtCmdComplete
 * Brief:           Processes EvtCmdCompletion
 * Description:
 * Note:
 * Params:          p_evtParams - Event Params.
                    p_suplCntl - SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result processEvtCmdComplete(const TNAVC_evt *const p_evtParams,
                                           const TSUPL_Ctrl *const p_suplCntl);

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /* __SUPLC_EVT_HANDLER_H__ */
