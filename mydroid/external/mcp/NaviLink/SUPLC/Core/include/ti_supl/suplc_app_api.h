/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_app_api.h
 *
 * Description      :
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */


#ifndef __SUPL_APP_API_H__
#define __SUPL_APP_API_H__

#ifdef __cplusplus
extern "C" {
#endif /*__cplusplus */

#include "navc_api.h"

#include "suplc_defs.h"
#include "suplc_api.h"


/**
 * Function:        SUPLC_appInitReq
 * Brief:           External Interface for creating SUPL Core
 * Description:     It creates SUPL_Core component.
                    This function should be called before doing any operation with SUPL_Core.
 * Note:            External Function.
 * Params:          p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_appInitReq(const TSUPL_Ctrl *const p_inSuplCntl);

/**
 * Function:        SUPLC_appDeInitReq
 * Brief:           External Interface for DeInit SUPL Core
 * Note:            External Function.
 * Params:          p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_appDeInitReq(const TSUPL_Ctrl *const p_inSuplCntl);



/**
 * Function:        SUPLC_processNetworkMessage
 * Brief:           Handles various commands sent to SUPLC
 * Description:     This function is called whenever socket server forwards Helper Service messages to SUPL_C.
                    These messages could either be SLP messages or SMS/WAP messages.
 * Note:            Helper Service connects to MCPF’s socket server and sends these messages.
                    The later then builds SUPL_C commands and send them to it.
 * Params:          p_cmdParams - Command with payload
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_processNetworkMessage(const TSUPLC_CmdParams *const p_cmdParams);

/**
 * Function:        SUPLC_processAssistanceRequest
 * Brief:           Handles assistance requests from NAVC.
 * Description:     This function is called whenever NAVC requests for some assistance data.
 * Params:          p_inEvtParams - Event with payload
                    p_inSuplCntl - Pointer to SUPLC's Control Structure
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_processAssistanceRequest(const TNAVC_evt *const p_inEvtParams,
                                             const TSUPL_Ctrl *const p_inSuplCntl);

eSUPLC_Result SUPLC_processStopRequest(const TNAVC_evt *const p_inEvtParams,
                                             const TSUPL_Ctrl *const p_inSuplCntl);


/**
 * Function:        SUPLC_processPositionReport
 * Brief:           Handles postion reports from NAVX.
 * Description:
 * Params:          p_evtParams - Event with payload
                    p_suplCntl - Pointer to SUPLC's Control Structure
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_processPositionReport(const TNAVC_evt *const p_evtParams,
                                          TSUPL_Ctrl *const p_suplCntl);




/**
 * Function:        SUPLC_processResponse
 * Brief:           Handles postion reports from NAVX.
 * Description:
 * Params:          p_evtParams - Event with payload
                    p_suplCntl - Pointer to SUPLC's Control Structure
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_processResponse(const TNAVC_evt *const p_evtParams,
                                    const TSUPL_Ctrl *const p_suplCntl);


/**
 * Function:        SUPLC_activateEE
 * Brief:           External Interface for activating EE Client.
 * Description:     It activates EE Client.

 * Note:            External Function.
 * Params:          p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_activateEE(const TSUPL_Ctrl *const p_inSuplCntl);

eSUPLC_Result SUPLC_processAssistAck(const TNAVC_evt *const p_evtParams,
                                          TSUPL_Ctrl *const p_suplCntl);


eSUPLC_Result SUPLC_ConnectionResponse(McpU8 conRes);

#ifdef __cplusplus
}
#endif /*__cplusplus */
#endif /* __SUPL_APP_API_H__ */
