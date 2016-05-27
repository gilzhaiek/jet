/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_evtHandler.c
 *
 * Description      :
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */

#include "navc_api.h"

#include "suplc_evtHandler.h"
#include "suplc_app_api.h"
#include "suplc_outEvt.h"

#define ENABLE_SUPLC_DEBUG
#ifdef ENABLE_SUPLC_DEBUG
    #include <utils/Log.h>
    #define PRINT_TAG LOGD
    #define  DEBUG_SUPLC(x)   x
#else
    #define  DEBUG_SUPLC(x)   ((void)0)
#endif /* ENABLE_SUPLC_DEBUG */

/**
 * Function:        SUPLC_eventHandler
 * Brief:           Handles various events sent to SUPLC
 * Description:     This functions is called whenever a message intended for SUPLC is
                    put on its event queue.
 * Note:            External Function.
 * Params:          hSuplc - handle to SUPLC's control structure.
                    p_msg - Contains all details about the message.
 * Return:          Success: RES_COMPLETE.
                    Failure: RES_ERROR.
 */
EMcpfRes SUPLC_eventHandler(handle_t hSuplc, TmcpfMsg *p_msg)
{
    TNAVC_evt *p_evtParams = NULL;
    TSUPL_Ctrl *p_suplCntl = NULL;

    eSUPLC_Result retVal = SUPLC_SUCCESS;
    EMcpfRes mcpfRetVal = RES_COMPLETE;

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_cSUPLC_eventHandler: Entering \n"));
	/* Klocwork Changes */
	/*if(p_msg == NULL)
	{
		return RES_ERROR;
	}
	*/
    p_suplCntl = (TSUPL_Ctrl *) hSuplc;
    p_evtParams = (TNAVC_evt *)(p_msg->pData);

    /* Just in case... */
    if ( (p_suplCntl == NULL) ||
         (p_evtParams == NULL) )
    {
        PRINT_TAG(" SUPLC_eventHandler: NULL Pointers \n");
        //mcpfRetVal = RES_ERROR;
        return RES_ERROR;  /* KW Changes */
    }

    switch(p_msg->uOpcode)
    {
        case NAVC_EVT_ASSISTANCE_REQUEST:
        {
            /* NAVC has request for assistance information. Process it. */
            retVal = SUPLC_processAssistanceRequest(p_evtParams, p_suplCntl);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" SUPLC_eventHandler: Assistance Request Processing FAILED !!! \n");
                mcpfRetVal = RES_ERROR;
            }
        }
        break;

		
		case NAVC_EVT_STOP_REQUEST:
        {
            /* NAVC has request for stop information. Process it. */
			PRINT_TAG(" SUPLC_eventHandler: NAVC_EVT_STOP_REQUEST \n");
            retVal = SUPLC_processStopRequest(p_evtParams, p_suplCntl);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" SUPLC_eventHandler: Stop Request Processing FAILED !!! \n");
                mcpfRetVal = RES_ERROR;
            }
        }
        break;
		
        case NAVC_EVT_POSITION_FIX_REPORT:
        {
            /* NAVC has given location fix. Process it. */
            retVal = SUPLC_processPositionReport(p_evtParams, p_suplCntl);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" SUPLC_eventHandler: Position Report Processing FAILED !!! \n");
                mcpfRetVal = RES_ERROR;
            }
        }
        break;

        case NAVC_EVT_RRC_RESP:
        case NAVC_EVT_RRLP_RESP:
        {
            /** NAVC has sent RRLP/RRC Response. Process it.
              * Note: Sometimes SUPLC also sends this event with src task as NAVC. */
            retVal = SUPLC_processResponse(p_evtParams, p_suplCntl);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" SUPLC_eventHandler: RRLP/RRC Response Processing FAILED !!! \n");
                mcpfRetVal = RES_ERROR;
            }
        }
        break;

        case NAVC_EVT_CMD_COMPLETE:
        {
            /* Process this event and send appropriate commands to NAVC. */
            retVal = processEvtCmdComplete(p_evtParams, p_suplCntl);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" SUPLC_eventHandler: Processing EVT_COMPLETION FAILED !!! \n");
                mcpfRetVal = RES_ERROR;
            }
        }
        break;
	case NAVC_EVT_RRC_ACK:
	case NAVC_EVT_RRLP_ACK:
	{
		LOGD("SUPLC_eventHandler: processEvtRrlpAck %d", p_evtParams->eComplCmd);
		/* Process RRLP ACK. */
		retVal = SUPLC_processAssistAck(p_evtParams, p_suplCntl);
		if (retVal != SUPLC_SUCCESS)
		{
			PRINT_TAG(" SUPLC_eventHandler: Processing NAVC_EVT_RRLP_ACK FAILED !!! \n");
			mcpfRetVal = RES_ERROR;
		}
	}
	break;
	
		default:
		{
		PRINT_TAG("###suplevthandler.c--UNHANDLED EVENT %x!!###",p_msg->uOpcode);
		}
    }
    if (p_msg->pData)
    {
        DEBUG_SUPLC(PRINT_TAG(" SUPLC_eventHandler: Free Memory 1"));
        mcpf_mem_free_from_pool (p_suplCntl->hMcpf, p_msg->pData);
        p_msg->pData = NULL;
    }
    DEBUG_SUPLC(PRINT_TAG(" SUPLC_eventHandler: Exiting  with %d \n", mcpfRetVal));
    mcpf_mem_free_from_pool (p_suplCntl->hMcpf, (void *) p_msg);
    p_msg = NULL;
    return mcpfRetVal;
}


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
                                           const TSUPL_Ctrl *const p_suplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" processEvtCmdComplete: Entering \n"));

    switch(p_evtParams->eComplCmd)
    {
        case NAVC_CMD_START:
        {
            /* NAVC has started. Send Location Fix Command. */
            if (p_evtParams->eResult == RES_OK)
            {
            }
            else
            {
                PRINT_TAG(" processEvtCmdComplete: NAVC_CMD_START FAILED !!! \n");
                return retVal;
            }
        }
        break;

        case NAVC_CMD_STOP:
        {
            /* NAVC has stoped. */
            if (p_evtParams->eResult == RES_OK)
            {
            }
            else
            {
            }
        }
        break;

        case NAVC_CMD_STOP_FIX:
        {
            if (p_evtParams->eResult == RES_OK)
            {
            }
            else
            {
            }
        }
        break;

        default:
        {
            PRINT_TAG(" processEvtCmdComplete: Default Case. \n");
            return SUPLC_ERROR_UNKNOWN;
        }
        break;
    }

    DEBUG_SUPLC(PRINT_TAG(" processEvtCmdComplete: Exiting Successfully \n"));
    return retVal;
}
