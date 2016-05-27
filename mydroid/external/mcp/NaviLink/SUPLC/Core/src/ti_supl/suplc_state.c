/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_state.c
 *
 * Description      :
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */


#include "suplc_protPos_api.h"
#include "suplc_core_api.h"
#include "suplc_state.h"
#include "suplc_outEvt.h"


#define ENABLE_SUPLC_DEBUG
#ifdef ENABLE_SUPLC_DEBUG
#include <utils/Log.h>
#define PRINT_TAG LOGD
#define  DEBUG_SUPLC(x)   x
#else
#define  DEBUG_SUPLC(x)   ((void)0)
#endif /* ENABLE_SUPLC_DEBUG */

/** SUPLC's Internal States.
  * Isolate it from external world. */
typedef enum{
    SUPLC_STATE_NULL = 0,
    SUPLC_STATE_INITIALIZED = 1,
    SUPLC_STATE_NET_INIT,
    SUPLC_STATE_SET_INIT,

    SUPLC_MAX
}eSUPLC_State;


/**
 * Function:        suplcStateChange
 * Brief:           Changes SUPLC's state.
 * Description:
 * Note:
 * Params:          e_currentState - Current State.
                    e_futureState - Future State.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result suplcStateChange(const eSUPLC_State e_currentState,
                                      const eSUPLC_State e_futureState);

/**
 * Function:        callExitFunctionOfCurrentState
 * Brief:           State Exit Functions.
 * Description:
 * Note:
 * Params:          e_state - Current State.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result callExitFunctionOfCurrentState(const eSUPLC_State e_state);

/**
 * Function:        callEntryFunctionOfFutureState
 * Brief:           State Entry Functions.
 * Description:
 * Note:
 * Params:          e_state - Current State.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result callEntryFunctionOfFutureState(const eSUPLC_State e_state);
eSUPLC_Result SUPLC_stopSuplSession(const void *const p_inData,
                                     const void *const p_inSuplCntl);



/* State variable. Local to this file */
static eSUPLC_State g_suplcCurrentState = SUPLC_STATE_NULL;

static McpBool bStopInProgress = MCP_FALSE;

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
                                 const void *const p_inSuplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" SUPLC_triggerEvent: Entering \n"));

    switch(e_triggeredEvent)
    {
        case evNull:
        {
            retVal = handle_evNullTrigger(p_inData);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" SUPLC_triggerEvent: Handling of evNull FAILED !!! \n");
            }
        }
        break;

        case evInit:
        {
            retVal = handle_evInitTrigger(p_inData, p_inSuplCntl);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" SUPLC_triggerEvent: Handling of evInit FAILED !!! \n");
            }
        }
        break;
    
    case evDeInit:
    {
        retVal = handle_evDeInitTrigger(p_inData, p_inSuplCntl);
        if (retVal != SUPLC_SUCCESS)
        {
            PRINT_TAG(" SUPLC_triggerEvent: Handling of evDeInit FAILED !!! \n");
        }
    }
    break;    

        case evNetMsg:
        {
			retVal = handle_evNetMsgTrigger(p_inData, p_inSuplCntl);
            if(retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" SUPLC_triggerEvent: Handling of evNetMsg FAILED !!! \n");
            }
        }
        break;

        case evAssistReq:
        {
            retVal = handle_evAssistReqTrigger(p_inData, p_inSuplCntl);
            if(retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" SUPLC_triggerEvent: Handling of evAssistReq FAILED !!! \n");
            }
        }
        break;

		case evStopReq:
        {
			PRINT_TAG(" SUPLC_triggerEvent: evStopReq\n");
            retVal = handle_evStopReqTrigger(p_inData, p_inSuplCntl);
            if(retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" SUPLC_triggerEvent: Handling of evStopReq FAILED !!! \n");
            }
        }
        break;
		
        case evPosReport:
        {
            retVal = handle_evPosReportTrigger(p_inData, p_inSuplCntl);
            if(retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" SUPLC_triggerEvent: Handling of evPosReport FAILED !!! \n");
            }
        }
        break;

        case evPosResp:
        {
            retVal = handle_evPosRespTrigger(p_inData, p_inSuplCntl);
            if(retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" SUPLC_triggerEvent: Handling of evPosResp FAILED !!! \n");
            }
        }
        break;

        case evAssistInd:
        {
            retVal = handle_evAssistIndTrigger(p_inData, p_inSuplCntl);
            if(retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" SUPLC_triggerEvent: Handling of evAssistInd FAILED !!! \n");
            }
        }
        break;  
       
	case evAssistAck:
	{
			 LOGD("Suplc trigger Evt handle_AssistAck  \n");
			retVal =handle_AssistAck(p_inData, p_inSuplCntl);
			if(retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" SUPLC_triggerEvent: Handling of evActivateEE FAILED !!! \n");
            }
	}
	break;
    case evStopInd:
    {
        DEBUG_SUPLC(PRINT_TAG(" SUPLC_triggerEvent: evStopInd + \n"));
        retVal = handle_evStopIndTrigger(p_inData, p_inSuplCntl);
        if(retVal != SUPLC_SUCCESS)
        {
            PRINT_TAG(" SUPLC_triggerEvent: Handling of evStopInd FAILED !!! \n");
        }
    }
    break;
    /* APK - End */
    /* APK - Start */
    case evPosInd:
    {
        DEBUG_SUPLC(PRINT_TAG(" SUPLC_triggerEvent: evPosInd + \n"));
        retVal = handle_evPosIndTrigger(p_inData, p_inSuplCntl);
        if(retVal != SUPLC_SUCCESS)
        {
            PRINT_TAG(" SUPLC_triggerEvent: Handling of evStopInd FAILED !!! \n");
        }
    }
    break;

	case evConRes:
	{
			LOGD("Supl Connection Response Event  \n");
			retVal =handle_ConRes(p_inData, p_inSuplCntl);
			if(retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" SUPLC_triggerEvent: Handling of evConRes FAILED !!! \n");
            }
	}
    break;
	break;
        default:
        {
            PRINT_TAG(" SUPLC_triggerEvent: Unknown event !!! \n");
            retVal = SUPLC_ERROR_INVALID_EVENT;
        }
        break;
    }

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_triggerEvent: Exiting with status %d \n", retVal));
    return retVal;
}


/**
 * Function:        handle_evNullTrigger
 * Brief:           Handle evNull trigger.
 * Description:
 * Note:
 * Params:          p_inData - Data.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result handle_evNullTrigger(void *p_inData)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;

    DEBUG_SUPLC(PRINT_TAG(" handle_evNullTrigger: Entering \n"));
    MCPF_UNUSED_PARAMETER(p_inData);

    switch(g_suplcCurrentState)
    {
        /* Handle evNull accordingly if required */
        case SUPLC_STATE_NULL:
        case SUPLC_STATE_INITIALIZED:
        default:
        {
            PRINT_TAG(" handle_evNullTrigger: Event Not Handled. Return SUCCESS \n");
        }
        break;
    }

    DEBUG_SUPLC(PRINT_TAG(" handle_evNullTrigger: Exiting with status %d \n", retVal));
    return retVal;
}


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
                                          const void *const p_inSuplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" handle_evInitTrigger: Entering \n"));

    switch(g_suplcCurrentState)
    {
        case SUPLC_STATE_NULL:
        {
            /* Instantiate SUPL Client. */
            retVal = SUPLC_initSuplClient(p_inData, p_inSuplCntl);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" handle_evInitTrigger: SUPL Client initialization FAILED !!! \n");
                break;
            }

            /* Instantiate Protocol Library. */
            retVal = SUPLC_initProtocolLib(p_inSuplCntl);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" handle_evInitTrigger: Protocol library initialization FAILED !!! \n");
                return retVal;
            }

            /* Change state. */
            retVal = suplcStateChange(g_suplcCurrentState, SUPLC_STATE_INITIALIZED);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" handle_evInitTrigger: State Change FAILED !!! \n");
            }
        }
        break;

        case SUPLC_STATE_INITIALIZED:
        default:
        {
            PRINT_TAG(" handle_evInitTrigger: Event Not Handled. Return SUCCESS \n");
        }
        break;
    }

    DEBUG_SUPLC(PRINT_TAG(" handle_evInitTrigger: Exiting Successfully \n"));
    return retVal;
}


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
        const void *const p_inSuplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" +handle_evDeInitTrigger \n"));

    switch(g_suplcCurrentState)
    {
        case SUPLC_STATE_INITIALIZED:
        case SUPLC_STATE_SET_INIT:
        case SUPLC_STATE_NET_INIT:
        {
    /* DeInit Protocol Library. */
    retVal = SUPLC_deInitProtocolLib(p_inSuplCntl);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" handle_evDeInitTrigger: Protocol library deinitialization FAILED !!! \n");
    }

    /* DeInit SUPL Client. */
    retVal = SUPLC_deInitSuplClient(p_inData);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" handle_evDeInitTrigger: SUPL Client deinitialization FAILED !!! \n");
    }

    /* Change state. */
    retVal = suplcStateChange(g_suplcCurrentState, SUPLC_STATE_NULL);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" handle_evDeInitTrigger: State Change FAILED !!! \n");
            }
        }
        break;

        case SUPLC_STATE_NULL:
        default:
        {
            PRINT_TAG(" handle_evDeInitTrigger: Event Not Handled. Return SUCCESS \n");
        }
        break;
    }
    
    DEBUG_SUPLC(PRINT_TAG(" handle_evDeInitTrigger: retVal = 0x%x \n", retVal));
    DEBUG_SUPLC(PRINT_TAG(" -handle_evDeInitTrigger \n"));
    return retVal;
}



/**
 * Function:        handle_evSessionEnd
 * Brief:           Handle SessionEnd
 * Description:
 * Note:
 * Params:          p_inData - Data.
                    p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result handle_evSessionEnd(
        const void *const p_inSuplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" +handle_evSessionEnd: %d \n",g_suplcCurrentState));
	switch(g_suplcCurrentState)
		{
			case SUPLC_STATE_NET_INIT:
			case SUPLC_STATE_SET_INIT:
			{

			    /* Change state. */
			    retVal = suplcStateChange(g_suplcCurrentState, SUPLC_STATE_INITIALIZED);
			    if (retVal != SUPLC_SUCCESS)
			    {
			        PRINT_TAG(" handle_evSessionEnd: State Change FAILED !!! \n");
			    }
    
    			DEBUG_SUPLC(PRINT_TAG(" handle_evSessionEnd: retVal = 0x%x \n", retVal));
			}
			break;
			case SUPLC_STATE_INITIALIZED:
			case SUPLC_STATE_NULL:
			default:
			{
				DEBUG_SUPLC(PRINT_TAG(" -state already in end state \n"));
				break;
			}

		}
			
    DEBUG_SUPLC(PRINT_TAG(" -handle_evSessionEnd \n"));
	DEBUG_SUPLC(PRINT_TAG(" -Session Stop Completed.. StopIndProgress = FALSE \n"));
	bStopInProgress = MCP_FALSE;
    return retVal;
}



/**
 * Function:        handle_evNetMsgTrigger
 * Brief:           Handle evNetMsg trigger.
 * Description:
 * Note:
 * Params:          p_inData - Data.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result handle_evNetMsgTrigger(const void *const p_inData,
												 const void *const p_inSuplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" handle_evNetMsgTrigger: Entering \n"));

	if(MCP_FALSE==bStopInProgress)
	{
    switch(g_suplcCurrentState)
    {
        case SUPLC_STATE_INITIALIZED:
        {
            /** If network message is received in this state, then it should be SUPL_INIT.
              * Hence, state is changed to SUPLC_STATE_NET_INIT. */
	              
	            /* Change state. */
	            retVal = suplcStateChange(g_suplcCurrentState, SUPLC_STATE_NET_INIT);
	            if (retVal != SUPLC_SUCCESS)
	            {
	                PRINT_TAG(" handle_evNetMsgTrigger: State Change FAILED !!! \n");
	            }
				
	            retVal = SUPLC_handleNetworkMessage(p_inData, p_inSuplCntl);
	            if (retVal != SUPLC_SUCCESS)
	            {
	                PRINT_TAG(" handle_evNetMsgTrigger: Net Msg Handling FAILED !!! \n");
		         /* Change state. */
	            	retVal = suplcStateChange(g_suplcCurrentState, SUPLC_STATE_INITIALIZED);
	               if (retVal != SUPLC_SUCCESS)
	              {
	                    PRINT_TAG(" handle_evNetMsgTrigger: State Change FAILED !!! \n");
	               }			
	                break;
	            }
	        }
        break;

        case SUPLC_STATE_NET_INIT:
        case SUPLC_STATE_SET_INIT:
        {
            /* Process the incoming messages without state transition. */
            retVal = SUPLC_handleNetworkMessage(p_inData, p_inSuplCntl);
            if (retVal != SUPLC_SUCCESS)
            {
	                PRINT_TAG(" handle_evNetMsgTrigger: Net Msg Handling FAILED !!! \n");
            }
        }
        break;

        case SUPLC_STATE_NULL:
        default:
        {
	            PRINT_TAG(" handle_evNetMsgTrigger: Event Not Handled. Return SUCCESS \n");
        }
        break;
	    }
	}
	else
		{
			DEBUG_SUPLC(PRINT_TAG(" handle_evNetMsgTrigger: Net Message Rejected as Stop Progress = %d \n", bStopInProgress));
    }

    DEBUG_SUPLC(PRINT_TAG(" handle_evNetMsgTrigger: Exiting with error code %d \n", retVal));
    return retVal;
}

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
                                               const void *const p_inSuplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" handle_evAssistReqTrigger: Entering \n"));

    switch(g_suplcCurrentState)
    {
        case SUPLC_STATE_INITIALIZED:
        {
            retVal = suplcStateChange(g_suplcCurrentState, SUPLC_STATE_SET_INIT);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" handle_evAssistReqTrigger: State Change FAILED !!! \n");
            }	
			
            /** When assistance data is requested in this state, a supl session has to
              * be extablished with SLP. */
            retVal = SUPLC_startSuplSession(p_inData, p_inSuplCntl);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" handle_evAssistReqTrigger: SUPL session starting FAILED !!! \n");
	       retVal = suplcStateChange(g_suplcCurrentState, SUPLC_STATE_INITIALIZED);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" handle_evAssistReqTrigger: State Change FAILED !!! \n");
              }
			   
                break;
            }
        }
        break;

        case SUPLC_STATE_NET_INIT:
        {
            /** TBD: Session has to be started when this event is triggered for the first
              *      time in this state.
              * Process the event without state transition. */
            retVal = SUPLC_startSuplSession(p_inData, p_inSuplCntl);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" handle_evAssistReqTrigger: SUPL session starting FAILED !!! \n");
                break;
            }
        }
        break;

        case SUPLC_STATE_SET_INIT:
        case SUPLC_STATE_NULL:
        default:
        {
            PRINT_TAG(" handle_evAssistReqTrigger: Event Not Handled. Return SUCCESS \n");
        }
        break;
    }

    DEBUG_SUPLC(PRINT_TAG(" handle_evAssistReqTrigger: Exiting Successfully \n"));
    return retVal;
}




static eSUPLC_Result handle_evStopReqTrigger(const void *const p_inData,
                                               const void *const p_inSuplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" handle_evStopReqTrigger: Entering \n"));
	DEBUG_SUPLC(PRINT_TAG(" handle_evStopReqTrigger: g_suplcCurrentState = %d \n",g_suplcCurrentState));
    switch(g_suplcCurrentState)
    {
        case SUPLC_STATE_SET_INIT:
	 	case SUPLC_STATE_NET_INIT: 		
        {
            /** When assistance data is requested in this state, a supl session has to
                            * be extablished with SLP. */
            retVal = SUPLC_stopSuplSession(p_inData, p_inSuplCntl);
	    bStopInProgress = MCP_TRUE;
	    DEBUG_SUPLC(PRINT_TAG(" -Session Stop Started.. StopIndProgress = TRUE \n"));
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" handle_evStopReqTrigger: SUPL session starting FAILED !!! \n");
                break;
            }

#if 0
            /* Change state. */
            retVal = suplcStateChange(g_suplcCurrentState, SUPLC_STATE_INITIALIZED);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" handle_evStopReqTrigger: State Change FAILED !!! \n");
            }
			//PRINT_TAG(" handle_evStopReqTrigger: suplcStateChange to SUPLC_STATE_INITIALIZED \n");
#endif
        }
        break;

             
        case SUPLC_STATE_NULL:
		case SUPLC_STATE_INITIALIZED:			
        default:
        {
			StopIndData stopData;
			stopData.gps_stoped=1; //Failure
			retVal = SUPLC_sendSessResult(p_inSuplCntl,(void*)&stopData);
            PRINT_TAG(" handle_evStopReqTrigger: Event Not Handled. Return SUCCESS \n");
        }
        break;
    }

    DEBUG_SUPLC(PRINT_TAG(" handle_evStopReqTrigger: Exiting Successfully \n"));
    return retVal;
}









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
                                               const void *const p_suplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" handle_evPosReportTrigger: Entering \n"));
	T_GPSC_loc_fix_response* plocfix;
	plocfix = (T_GPSC_loc_fix_response*)p_data;

    switch(g_suplcCurrentState)
    {

        case SUPLC_STATE_SET_INIT:
        case SUPLC_STATE_NET_INIT:
        {
            /* Process the event without state transition. */
            retVal = SUPLC_posReport(p_data, p_suplCntl);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" handle_evPosReportTrigger: SUPL PosRep handling FAILED !!! \n");
                break;
            }
        }
        break;

        case SUPLC_STATE_INITIALIZED: 
        {
            retVal = SUPLC_posReport(p_data, p_suplCntl);
            if (retVal != SUPLC_SUCCESS)
            {
            PRINT_TAG(" handle_evPosReportTrigger: Event Handled. Return SUCCESS \n");
                break;
            }
         }break;
		case SUPLC_STATE_NULL:
        default:
        {
            PRINT_TAG(" handle_evPosReportTrigger: Event Not Handled. Return SUCCESS \n");		
        }
        break;
    }

    DEBUG_SUPLC(PRINT_TAG(" handle_evPosReportTrigger: Exiting Successfully \n"));
    return retVal;
}


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
                                             const void *const p_suplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" handle_evPosRespTrigger: Entering \n"));

    switch(g_suplcCurrentState)
    {

        case SUPLC_STATE_SET_INIT:
        case SUPLC_STATE_NET_INIT:
        {
            /* Process the event without state transition. */
            retVal = SUPLC_posResponse(p_data, p_suplCntl);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" handle_evPosRespTrigger: SUPL PosResp handling FAILED !!! \n");
            }
        }
        break;

        case SUPLC_STATE_INITIALIZED:
        case SUPLC_STATE_NULL:
        default:
        {
            PRINT_TAG(" handle_evPosRespTrigger: Event Not Handled. Return SUCCESS \n");
        }
        break;
    }

    DEBUG_SUPLC(PRINT_TAG(" handle_evPosRespTrigger: Exiting with status %d \n", retVal));
    return retVal;
}


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
                                             const void *const p_suplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" handle_evAssistIndTrigger: Entering \n"));

    switch(g_suplcCurrentState)
    {
        case SUPLC_STATE_SET_INIT:
        case SUPLC_STATE_NET_INIT:
        {
            /* Change state. */
            retVal = suplcStateChange(g_suplcCurrentState, SUPLC_STATE_INITIALIZED);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" handle_evAssistIndTrigger: State Change FAILED !!! \n");
            }
        }
        break;

        case SUPLC_STATE_INITIALIZED:
        case SUPLC_STATE_NULL:
        default:
        {
            PRINT_TAG(" handle_evAssistIndTrigger: Event Not Handled. Return SUCCESS \n");
        }
        break;
    }

    DEBUG_SUPLC(PRINT_TAG(" handle_evAssistIndTrigger: Exiting with status %d \n", retVal));
    return retVal;
}


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
                                                const void *const p_inSuplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" handle_evActivateEeTrigger: Entering \n"));

    switch(g_suplcCurrentState)
    {
        case SUPLC_STATE_INITIALIZED:
        {
            /* Activate EE Client on receiving this trigger. */
            retVal = SUPLC_activateEeClient(p_inData, p_inSuplCntl);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" handle_evActivateEeTrigger: SUPL session starting FAILED !!! \n");
                break;
            }

            /* Change state. */
            retVal = suplcStateChange(g_suplcCurrentState, SUPLC_STATE_SET_INIT);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" handle_evActivateEeTrigger: State Change FAILED !!! \n");
            }
        }
        break;

        case SUPLC_STATE_NET_INIT:
        case SUPLC_STATE_SET_INIT:
        case SUPLC_STATE_NULL:
        default:
        {
            PRINT_TAG(" handle_evInitTrigger: Event Not Handled. Return SUCCESS \n");
        }
        break;
    }

    DEBUG_SUPLC(PRINT_TAG(" handle_evAssistReqTrigger: Exiting Successfully \n"));
    return retVal;
}


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
                                             const void *const p_suplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" handle_evStopIndTrigger: Entering \n"));

    switch(g_suplcCurrentState)
    {
        case SUPLC_STATE_SET_INIT:
        case SUPLC_STATE_NET_INIT:
        {
            /* Change state. */
            retVal = suplcStateChange(g_suplcCurrentState, SUPLC_STATE_INITIALIZED);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" handle_evStopIndTrigger: State Change FAILED !!! \n");
            }
        }
        break;

        case SUPLC_STATE_INITIALIZED:
        case SUPLC_STATE_NULL:
        default:
        {
            PRINT_TAG(" handle_evStopIndTrigger: Event Not Handled. Return SUCCESS \n");
        }
        break;
    }

    DEBUG_SUPLC(PRINT_TAG(" handle_evStopIndTrigger: Exiting with status %d \n", retVal));
    return retVal;
}
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
                                            const void *const p_suplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" handle_evPosIndTrigger: Entering \n"));

    switch(g_suplcCurrentState)
    {
        case SUPLC_STATE_SET_INIT:
        case SUPLC_STATE_NET_INIT:
        {
            /* Change state. */
            retVal = suplcStateChange(g_suplcCurrentState, SUPLC_STATE_INITIALIZED);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" handle_evPosIndTrigger: State Change FAILED !!! \n");
            }
        }
        break;

        case SUPLC_STATE_INITIALIZED:
        case SUPLC_STATE_NULL:
        default:
        {
            PRINT_TAG(" handle_evPosIndTrigger: Event Not Handled. Return SUCCESS \n");
        }
        break;
    }

    DEBUG_SUPLC(PRINT_TAG(" handle_evPosIndTrigger: Exiting with status %d \n", retVal));
    return retVal;

}
/* APK - End */


/**
 * Function:        suplcStateChange
 * Brief:           Changes SUPLC's state.
 * Description:
 * Note:
 * Params:          e_currentState - Current State.
                    e_futureState - Future State.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result suplcStateChange(const eSUPLC_State e_currentState,
                                      const eSUPLC_State e_futureState)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" suplcStateChange: Entering \n"));

    /**/
    retVal = callExitFunctionOfCurrentState(e_currentState);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" suplcStateChange: State Exit Function FAILED !!! \n");
        return retVal;
    }


    /**/
    retVal = callEntryFunctionOfFutureState(e_futureState);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" suplcStateChange: State Entry Function FAILED !!! \n");
        return retVal;
    }

    /* Update state variable */
    g_suplcCurrentState = e_futureState;
	//PRINT_TAG(" suplcStateChange: g_suplcCurrentState = %d \n",g_suplcCurrentState)
    DEBUG_SUPLC(PRINT_TAG(" suplcStateChange: Exiting Successfully \n"));
    return SUPLC_SUCCESS;
}


/**
 * Function:        callExitFunctionOfCurrentState
 * Brief:           State Exit Functions.
 * Description:
 * Note:
 * Params:          e_state - Current State.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result callExitFunctionOfCurrentState(const eSUPLC_State e_state)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" callExitFunctionOfCurrentState: Entering \n"));

    switch(e_state)
    {
        case SUPLC_STATE_NULL:
        {
            //retVal = SUPLC_exitNullState();
        }
        break;

        case SUPLC_STATE_INITIALIZED:
        {
            //retVal = SUPLC_exitInitState();
        }
        break;

        case SUPLC_STATE_NET_INIT:
        {
            //retVal = SUPLC_exitNetInitState();
        }
        break;

        case SUPLC_STATE_SET_INIT:
        {
            //retVal = SUPLC_exitSetInitState();
        }
        break;

        default:
        {
        }
        break;
    }

    DEBUG_SUPLC(PRINT_TAG(" callExitFunctionOfCurrentState: Exiting Successfully \n"));
    return retVal;
}


/**
 * Function:        callEntryFunctionOfFutureState
 * Brief:           State Entry Functions.
 * Description:
 * Note:
 * Params:          e_state - Current State.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result callEntryFunctionOfFutureState(const eSUPLC_State e_state)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" callEntryFunctionOfFutureState: Entering \n"));

    switch(e_state)
    {
        case SUPLC_STATE_NULL:
        {
            //retVal = SUPLC_enterNullState();
        }
        break;

        case SUPLC_STATE_INITIALIZED:
        {
            //retVal = SUPLC_enterInitState();
        }
        break;

        case SUPLC_STATE_NET_INIT:
        {
            //retVal = SUPLC_enterNetInitState();
        }
        break;

        case SUPLC_STATE_SET_INIT:
        {
            //retVal = SUPLC_enterSetInitState();
        }
        break;

        default:
        {
        }
        break;
    }

    DEBUG_SUPLC(PRINT_TAG(" callEntryFunctionOfFutureState: Exiting Successfully \n"));
    return retVal;
}
static eSUPLC_Result handle_AssistAck(const void *const p_data, const void *const p_suplCntl)
{
	eSUPLC_Result retVal = SUPLC_SUCCESS;
	  LOGD("handle_AssistAck Entering \n");
	retVal = SUPLC_AssistAck(p_data, p_suplCntl);
	if (retVal != SUPLC_SUCCESS)
	{
	    PRINT_TAG("handle_AssistAck FAILED !!! \n");
	}
	  LOGD("handle_AssistAck Exiting \n");
	return retVal;
}


static eSUPLC_Result handle_ConRes(const void *const p_data, const void *const p_suplCntl)
{
	eSUPLC_Result retVal = SUPLC_SUCCESS;
	  LOGD("handle_ConRes Entering \n");
	retVal = SUPLC_ConRes(p_data, p_suplCntl);
	if (retVal != SUPLC_SUCCESS)
	{
	    PRINT_TAG("handle_ConRes FAILED !!! \n");
	}
	  LOGD("handle_ConRes Exiting \n");
	return retVal;
}

McpBool IsSuplcStopInProgress()
{
	return bStopInProgress;
}
