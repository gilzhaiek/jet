/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_state.h
 *
 * Description      :
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */

#ifndef __SUPLC_STATE_H__
#define __SUPLC_STATE_H__

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#include "suplc_defs.h"


/* Events which could be triggered from outside */
typedef enum{
    evNull = 0,
    evInit = 1,         /* */
    evDeInit,           /* */
    evNetMsg,           /* */
    evAssistReq,        /* */
    evStopReq,          /* Newley Addeed */
    evPosReport,        /* */
    evPosResp,          /* */
    evAssistInd,        /* */
    evActivateEE,       /* */
    evAssistAck,
    evStopInd,          /* */
    evPosInd,           /* */
    evConRes,

}eSUPLC_EVENT;


/**
 * Function:        SUPLC_triggerEvent
 * Brief:           External Interface for triggering events.
 * Description:     Based on this trigger, SUPLC's state machine would be taken care here.
 * Note:            External Function.
 * Params:          triggerdEvent - Event triggered from external world.
                    p_inData - Data.
                    p_inSuplCntl - Pointer to SUPLC's Control Structure
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_triggerEvent(const eSUPLC_EVENT e_triggeredEvent,
                                 const void *const p_inData,
                                 const void *const p_inSuplCntl);

/**
 * Function:        handle_evNullTrigger
 * Brief:           Handle evNull trigger.
 * Description:
 * Note:
 * Params:          p_inData - Data.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result handle_evNullTrigger(void *p_inData);


/**
 * Function:        handle_evInitTrigger
 * Brief:           Handle evInit trigger.
 * Description:
 * Note:
 * Params:          p_inData - Data.
                    p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result handle_evInitTrigger(const void *const p_inData,
                                          const void *const p_inSuplCntl);

/**
 * Function:        handle_evDeInitTrigger
 * Brief:           Handle evDeInit trigger.
 * Description:
 * Note:
 * Params:          p_inData - Data.
                    p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result handle_evDeInitTrigger(const void *const p_inData,
                                            const void *const p_inSuplCntl);

/**
 * Function:        handle_evSessionEnd
 * Brief:           Handle Session End
 * Description:
 * Note:
 * Params:          p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result handle_evSessionEnd(const void *const p_inSuplCntl);


/**
 * Function:        handle_evNetMsgTrigger
 * Brief:           Handle evNetMsg trigger.
 * Description:
 * Note:
 * Params:          p_inData - Data.
                    p_inSuplCntl - Pointer to SUPLC's Control Structure
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result handle_evNetMsgTrigger(const void *const p_inData,
											const void *const p_inSuplCntl);

/**
 * Function:        handle_evAssistReqTrigger
 * Brief:           Handle evAssistReq trigger.
 * Description:
 * Note:
 * Params:          p_inData - Data.
                    p_inSuplCntl - Pointer to SUPLC's Control Structure
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result handle_evAssistReqTrigger(const void *const p_inData,
                                               const void *const p_inSuplCntl);

static eSUPLC_Result handle_evStopReqTrigger(const void *const p_inData,
                                               const void *const p_inSuplCntl);


/**
 * Function:        handle_evPosReportTrigger
 * Brief:           Handle evPosReport trigger.
 * Description:
 * Note:
 * Params:          p_data - Data.
                    p_suplCntl - Pointer to SUPLC's Control Structure
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result handle_evPosReportTrigger(const void *const p_data,
                                               const void *const p_suplCntl);



/**
 * Function:        handle_evPosReportTrigger
 * Brief:           Handle evPosReport trigger.
 * Description:
 * Note:
 * Params:          p_data - Data.
                    p_suplCntl - Pointer to SUPLC's Control Structure
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result handle_evPosRespTrigger(const void *const p_data,
                                             const void *const p_suplCntl);



/**
 * Function:        handle_evAssistIndTrigger
 * Brief:           Handle evAssistInd trigger.
 * Description:
 * Note:
 * Params:          p_data - Data.
 *                  p_suplCntl - Pointer to SUPLC's Control Structure
 * Return:          Success: SUPLC_SUCCESS.
 *                  Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result handle_evAssistIndTrigger(const void *const p_data,
                                               const void *const p_suplCntl);


/**
 * Function:        handle_evActivateEeTrigger
 * Brief:           Handle evActivateEE trigger.
 * Description:
 * Note:
 * Params:          p_inData - Data.
                    p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result handle_evActivateEeTrigger(const void *const p_inData,
                                                const void *const p_inSuplCntl);

static eSUPLC_Result handle_AssistAck(const void *const p_data,
                                             const void *const p_suplCntl);
/* APK - Start */
/**
 * Function:        handle_evStopIndTrigger
 * Brief:           Handle evStopInd trigger.
 * Description:
 * Note:
 * Params:          p_data - Data.
 *                  p_suplCntl - Pointer to SUPLC's Control Structure
 * Return:          Success: SUPLC_SUCCESS.
 *                  Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result handle_evStopIndTrigger(const void *const p_data,
                                             const void *const p_suplCntl);
/* APK - End */


/* APK - Start */
/**
 * Function:        handle_evPosIndTrigger
 * Brief:           Handle evPosInd trigger.
 * Description:
 * Note:
 * Params:          p_data - Data.
 *                  p_suplCntl - Pointer to SUPLC's Control Structure
 * Return:          Success: SUPLC_SUCCESS.
 *                  Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result handle_evPosIndTrigger(const void *const p_data,
                                            const void *const p_suplCntl);
/* APK - End */

/**
 * Function:        handle_ConRes
 * Brief:           Handle Connection Response from Helper Service
 * Description:
 * Note:
 * Params:          p_inData - Data.
                    p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */

static eSUPLC_Result handle_ConRes(const void *const p_data, const void *const p_suplCntl);

/**
 * Function:        IsSuplcStopInProgress
 * Brief:           Return Suplc Stop status
 * Description:
 * Note:
 * Return:          Stop In Progress: MCP_TRUE.
                    Idle : MCP_FALSE.
 */

McpBool IsSuplcStopInProgress();
#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /* __SUPLC_STATE_H__ */
