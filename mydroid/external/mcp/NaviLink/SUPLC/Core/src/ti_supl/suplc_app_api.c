/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_app_api.c
 *
 * Description      :
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */

#include "mcp_hal_types.h"

#include "suplc_app_api.h"
#include "suplc_state.h"

#include "nav_log_msg.h"
#include "nav_log_cust_codes.h"
#define ENABLE_SUPLC_DEBUG
#ifdef ENABLE_SUPLC_DEBUG
    #include <utils/Log.h>
    #define PRINT_TAG LOGD
    #define  DEBUG_SUPLC(x)   x
#else
    #define  DEBUG_SUPLC(x)   ((void)0)
#endif /* ENABLE_SUPLC_DEBUG */

#define TRUE 1
#define FALSE 0



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
eSUPLC_Result SUPLC_appInitReq(const TSUPL_Ctrl *const p_inSuplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_appInitReq: Entering \n"));

    /** Trigger appropriate event. SUPLC's state machine processes it.
      * This is implemented in suplc_state.c */
    retVal = SUPLC_triggerEvent(evInit, NULL, p_inSuplCntl);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPLC_appInitReq: Event triggering FAILED !!! \n");
        PRINT_TAG(" REASON: %d \n", retVal);
        return RES_ERROR;
    }

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_appInitReq: Exiting Successfully \n"));
    return retVal;
}

/**
 * Function:        SUPLC_appDeInitReq
 * Brief:           External Interface for DeInit SUPL Core
 * Note:            External Function.
 * Params:          p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_appDeInitReq(const TSUPL_Ctrl *const p_inSuplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;

    DEBUG_SUPLC(PRINT_TAG(" +SUPLC_appDeInitReq \n"));

    /** Trigger appropriate event. SUPLC's state machine processes it.
      * This is implemented in suplc_state.c */
    retVal = SUPLC_triggerEvent(evDeInit, NULL, p_inSuplCntl);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPLC_appDeInitReq: Event triggering FAILED !!! \n");
        PRINT_TAG(" REASON: %d \n", retVal);
        return RES_ERROR;
    }

    DEBUG_SUPLC(PRINT_TAG(" -SUPLC_appDeInitReq \n"));
    return retVal;
}


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
eSUPLC_Result SUPLC_processNetworkMessage(const TSUPLC_CmdParams *const p_cmdParams)
{
    TSUPLC_NetworkMsg *p_networkMessage = NULL;
    eSUPLC_Result retVal = SUPLC_SUCCESS;

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_processNetworkMessage: Entering \n"));

    p_networkMessage = (TSUPLC_NetworkMsg *)&p_cmdParams->s_networkMsg;

    /* Just in case... */
    if (p_networkMessage == NULL)
    {
        PRINT_TAG(" SUPLC_processNetworkMessage: NULL Pointers \n");
        return SUPLC_ERROR_NULL_PTRS;
    }

    /* Trigger appropriate event. SUPLC's state machine processes it. */
    retVal = SUPLC_triggerEvent(evNetMsg, (void *)p_networkMessage, NULL);

    /* Free memory which was alocated for Network Message. */ 
    //if (p_networkMessage->p_msg == NULL) free(p_networkMessage->p_msg);

    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPLC_processNetworkMessage: Event triggering FAILED !!! \n");
        PRINT_TAG(" REASON: %d \n", retVal);
        return retVal;
    }

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_processNetworkMessage: Exiting Successfully \n"));
    return retVal;
}


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
                                             const TSUPL_Ctrl *const p_inSuplCntl)
{
    TNAVC_assistReq *p_assistReqData = NULL;
    eSUPLC_Result retVal = SUPLC_SUCCESS;

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_processAssistanceRequest: Entering \n"));

    p_assistReqData = (TNAVC_assistReq *) &p_inEvtParams->tParams.tAssistReq;

    /* Just in case... */
    if (p_assistReqData == NULL)
    {
        PRINT_TAG(" SUPLC_processAssistanceRequest: NULL Pointers \n");
        return SUPLC_ERROR_NULL_PTRS;
    }

    /* Trigger appropriate event. SUPLC's state machine processes it. */
    retVal = SUPLC_triggerEvent(evAssistReq, (void *)p_assistReqData, (void *)p_inSuplCntl);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPLC_processAssistanceRequest: Event triggering FAILED !!! \n");
        PRINT_TAG(" REASON: %d \n", retVal);
        return retVal;
    }

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_processAssistanceRequest: Exiting Successfully \n"));
    return retVal;
}



eSUPLC_Result SUPLC_processStopRequest(const TNAVC_evt *const p_inEvtParams,
                                             const TSUPL_Ctrl *const p_inSuplCntl)
{
    TNAVC_stopReq *p_stopReqData = NULL;
    eSUPLC_Result retVal = SUPLC_SUCCESS;

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_processStopRequest: Entering \n"));

    p_stopReqData = (TNAVC_stopReq *) &p_inEvtParams->tParams.tStopReq;

    /* Just in case... */
    if (p_stopReqData == NULL)
    {
        PRINT_TAG(" SUPLC_processStopRequest: NULL Pointers \n");
        return SUPLC_ERROR_NULL_PTRS;
    }

    /* Trigger appropriate event. SUPLC's state machine processes it. */
    retVal = SUPLC_triggerEvent(evStopReq, (void *)p_stopReqData, (void *)p_inSuplCntl);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPLC_processStopRequest: Event triggering FAILED !!! \n");
        PRINT_TAG(" REASON: %d \n", retVal);
        return retVal;
    }

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_processStopRequest: Exiting Successfully \n"));
    return retVal;
}




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
                                          TSUPL_Ctrl *const p_suplCntl)
{
    TNAVC_locFixReport *p_locFixResponse = NULL;

    eSUPLC_Result retVal = SUPLC_SUCCESS;


    DEBUG_SUPLC(PRINT_TAG(" SUPLC_processPositionReport: Entering \n"));

    p_locFixResponse = &p_evtParams->tParams.tLocFixReport;

    /* Just in case... */
    if ( p_locFixResponse == NULL)
    {
        PRINT_TAG(" SUPLC_processPositionReport: NULL Pointers \n");
        return SUPLC_ERROR_NULL_PTRS;
    }

	/* Is the requestor rrc or rrlp  */

if((p_suplCntl->bLpPosReq)&&(((p_locFixResponse->loc_fix_session_id)& 0xFF) ==  0))
{

    if((p_locFixResponse->ctrl_loc_fix_response_union == GPSC_RESULT_PROT_POSITION && (p_locFixResponse->loc_fix_req_mode==GPSC_FIXMODE_MSBASED) )  ||   ((p_locFixResponse->ctrl_loc_fix_response_union == GPSC_RESULT_PROT_MEASUREMENT) && (p_locFixResponse->loc_fix_req_mode==GPSC_FIXMODE_MSASSISTED)))
	{
    /* Trigger appropriate event. SUPLC's state machine processes it. */
    retVal = SUPLC_triggerEvent(evPosReport, (void *)p_locFixResponse, (void *)p_suplCntl);
    if (retVal != SUPLC_SUCCESS)
    	{
        PRINT_TAG(" SUPLC_processPositionReport: Event triggering FAILED !!! \n");
        PRINT_TAG(" REASON: %d \n", retVal);
        return retVal;
    	}
        p_suplCntl->bLpPosReq = FALSE;
	}
	else
	{
		DEBUG_SUPLC(PRINT_TAG(" SUPLC_processPositionReport: Position Report ignored as Report doesn't match requested pos mode by requestor rrlp or rrc\n"));
		retVal = SUPLC_ERROR_INVALID_POS_PROT;
	}
}

         else
        {
                DEBUG_SUPLC(PRINT_TAG(" SUPLC_processPositionReport: Position Report ignored as Report doesn't match requested pos mode \n"));
                retVal = SUPLC_ERROR_INVALID_POS_PROT;
        }

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_processPositionReport: Exiting Successfully \n"));
    return retVal;
}

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
                                    const TSUPL_Ctrl *const p_suplCntl)
{
    TNAVC_rrFrame *p_rrFrame = NULL;
    eSUPLC_Result retVal = SUPLC_SUCCESS;

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_processResponse: Entering \n"));
   SESSLOGMSG("[%s]%s:%d, %s %s \n",get_utc_time(),SESS_OTA_MSG_TYPE,nav_sess_getCount(0),"SUPL RESPONSE"," ");
    p_rrFrame = &p_evtParams->tParams.tRrFrame;

    /* Just in case... */
    if (p_rrFrame == NULL)
    {
        PRINT_TAG(" SUPLC_processResponse: NULL Pointers \n");
        return SUPLC_ERROR_NULL_PTRS;
    }

    /* Trigger appropriate event. SUPLC's state machine processes it. */
    retVal = SUPLC_triggerEvent(evPosResp, (void *)p_rrFrame, (void *)p_suplCntl);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPLC_processResponse: Event triggering FAILED !!! \n");
        PRINT_TAG(" REASON: %d \n", retVal);
        return retVal;
    }

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_processResponse: Exiting Successfully \n"));
    return retVal;
}

/**
 * Function:        SUPLC_activateEE
 * Brief:           External Interface for activating EE Client.
 * Description:     It activates EE Client.

 * Note:            External Function.
 * Params:          p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_activateEE(const TSUPL_Ctrl *const p_inSuplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_activateEE: Entering \n"));

    /** Trigger appropriate event. SUPLC's state machine processes it.
      * This is implemented in suplc_state.c */
    retVal = SUPLC_triggerEvent(evActivateEE, NULL, p_inSuplCntl);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPLC_activateEE: Event triggering FAILED !!! \n");
        PRINT_TAG(" REASON: %d \n", retVal);
        return RES_ERROR;
    }

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_activateEE: Exiting Successfully \n"));
    return retVal;
}

eSUPLC_Result SUPLC_processAssistAck(const TNAVC_evt *const p_evtParams,
                                          TSUPL_Ctrl *const p_suplCntl)
{
    TNAVC_rrFrame *p_rrFrame = NULL;
    eSUPLC_Result retVal = SUPLC_SUCCESS;


	//LP ACK Received. Lp Position Session was not triggered
	p_suplCntl->bLpPosReq = FALSE;

	p_rrFrame = &p_evtParams->tParams.tRrFrame;
	LOGD("SUPLC_processAssistAck Entering\n\r ");
    /* Just in case... */
    if (p_rrFrame == NULL)
    {
        PRINT_TAG(" SUPLC_processAssistAck: NULL Pointers \n");
        return SUPLC_ERROR_NULL_PTRS;
    }

    /* Trigger appropriate event. SUPLC's state machine processes it. */
    retVal = SUPLC_triggerEvent(evAssistAck, (void *)p_rrFrame, (void *)p_suplCntl);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPLC_processAssistAck: Event triggering FAILED !!! \n");
        PRINT_TAG(" REASON: %d \n", retVal);
        return retVal;
    }

	LOGD("SUPLC_processAssistAck Exiting \n\r");
    DEBUG_SUPLC(PRINT_TAG(" SUPLC_processAssistAck: Exiting Successfully \n"));
    return retVal;
	
				
}

/**
 * Function:        SUPLC_ConnectionResponse
 * Brief:           Handles Server Connection Response sent to SUPLC
 * Description:     This function is called whenever socket server connection Result is Received from Helper service.
                    These messages could either be SLP messages or SMS/WAP messages.
 * Note:            Helper Service connects to MCPF’s socket server and sends these messages.
                    The later then builds SUPL_C commands and send them to it.
 * Params:          p_cmdParams - Command with payload
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_ConnectionResponse(McpU8 conRes)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_ConnectionResponse: Entering \n"));
	LOGD("SUPLC_ConnectionResponse: Con Res=[%d]", conRes);

    /* Trigger appropriate event. SUPLC's state machine processes it. */
    retVal = SUPLC_triggerEvent(evConRes, (void *)&conRes, NULL);
	

    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPLC_ConnectionResponse: Event triggering FAILED !!! \n");
        PRINT_TAG(" REASON: %d \n", retVal);
        return retVal;
    }

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_ConnectionResponse: Exiting Successfully \n"));
    return retVal;
}


