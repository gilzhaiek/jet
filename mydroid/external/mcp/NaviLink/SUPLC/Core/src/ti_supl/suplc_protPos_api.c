/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_protPos_api.c
 *
 * Description      :
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */

#include "mcpf_time.h"

#include "navc_rrlp.h"
#include "navc_rrc.h"

#include "suplc_protPos_api.h"
#include "suplc_state.h"

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
 * Function:        SUPLC_initProtocolLib
 * Brief:           Initializes Position Protocol Library.
 * Description:
 * Note:            External Function
 * Params:          p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_initProtocolLib(TSUPL_Ctrl *const p_inSuplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" SUPLC_initProtocolLib: Entering \n"));

    /* Just in case... */
    if (p_inSuplCntl == NULL)
    {
        PRINT_TAG(" SUPLC_initProtocolLib: NULL Pointers (p_inSuplCntl) \n");
        return SUPLC_ERROR_NULL_PTRS;
    }

    /** Initialize both position protocols.
      * Task ID & QueID are for SUPLC to post a message to itself. */
    p_inSuplCntl->hRrlp = RRLP_Init (p_inSuplCntl->hMcpf,
                                     p_inSuplCntl->mMemPool,
                                     p_inSuplCntl->mMemPool,
                                     TASK_SUPL_ID,
                                     SUPLC_INTERNAL_EVT_QUEUE_ID);

    p_inSuplCntl->hRrc = RRC_Init (p_inSuplCntl->hMcpf,
                                   p_inSuplCntl->mMemPool,
                                   p_inSuplCntl->mMemPool,
                                   TASK_SUPL_ID,
                                   SUPLC_INTERNAL_EVT_QUEUE_ID);

    if ( (p_inSuplCntl->hRrlp == NULL) ||
         (p_inSuplCntl->hRrc == NULL) )
    {
        PRINT_TAG(" SUPLC_initProtocolLib: NULL Pointers (hRrlp) or (hRrc) \n");
        return SUPLC_ERROR_NULL_PTRS;
    }

	p_inSuplCntl->bLpPosReq=FALSE;

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_initProtocolLib: Exiting Successfully \n"));
    return retVal;
}


/**
 * Function:        SUPLC_initProtocolLib
 * Brief:           Initializes Position Protocol Library.
 * Description:
 * Note:            External Function
 * Params:          p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_deInitProtocolLib(TSUPL_Ctrl *const p_inSuplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" +SUPLC_deInitProtocolLib \n"));

    /* Just in case... */
    if (p_inSuplCntl == NULL)
    {
        PRINT_TAG(" SUPLC_deInitProtocolLib: NULL Pointers (p_inSuplCntl) \n");
        return SUPLC_ERROR_NULL_PTRS;
    }

    /** DeInitialize both position protocols.
      * Task ID & QueID are for SUPLC to post a message to itself. */
    if(p_inSuplCntl->hRrlp != NULL)
    RRLP_Destroy (p_inSuplCntl->hRrlp);
    if(p_inSuplCntl->hRrc != NULL)
    RRC_Destroy (p_inSuplCntl->hRrc);
    p_inSuplCntl->hRrc = NULL;
    p_inSuplCntl->hRrlp = NULL;

	p_inSuplCntl->bLpPosReq=FALSE;

    DEBUG_SUPLC(PRINT_TAG(" -SUPLC_deInitProtocolLib \n"));
    return retVal;
}


/**
 * Function:        SUPLC_getPositionPayload
 * Brief:           Encodes data into appropriate position protocols.
 * Description:
 * Note:
 * Params:          p_outData - RRLP/RRC data.
                    p_outDataLen - Data length.
                    p_inLocFixReport - Position Report Data.
                    p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_getPositionPayload(McpU8 *p_outData, McpU16 *p_outDataLen,
                                       const TNAVC_locFixReport *const p_inLocFixReport,
                                       TSUPL_Ctrl *const p_inSuplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" SUPLC_getPositionPayload: Entering \n"));

    switch(p_inSuplCntl->e_protocol)
    {
        case SUPLC_PROT_RRLP:
        {
            /* Encode RRLP Response message */
            *p_outDataLen = RRLP_encodeResponse(p_inSuplCntl->hRrlp,
                                                p_inLocFixReport,
                                                NULL,
                                                C_RRLP_REP_POS,
                                                C_RRLP_LOC_REASON_GOOD,
                                                p_outData);
        }
        break;

        case SUPLC_PROT_RRC:
        {
            /* Encode RRC Response message */
            *p_outDataLen = RRC_encodeResponse(p_inSuplCntl->hRrc,
                                               p_inLocFixReport,
                                               NULL,
                                               RRC_REP_POS,
                                               C_RRC_LOC_REASON_GOOD,
                                               p_outData);
        }
        break;
    }

	
	//Position/Measurement is received. No Lp Position request pending.
	p_inSuplCntl->bLpPosReq=FALSE;

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_getPositionPayload: Exiting Successfully \n"));
    return retVal;
}

/**
 * Function:        SUPLC_StopLpPositionRequest
 * Brief:           Encodes data into appropriate position protocols.
 * Description:
 * Note:
 * Params:       p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_StopLpPositionRequest(TSUPL_Ctrl *const p_inSuplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" SUPLC_StopLpPositionRequest: Entering \n"));

	if(FALSE==p_inSuplCntl->bLpPosReq)
		return SUPLC_SUCCESS;

    switch(p_inSuplCntl->e_protocol)
    {
        case SUPLC_PROT_RRLP:
        {
            /* Encode RRLP Response message */
            RRLP_StopLocationFixRequest(p_inSuplCntl->hRrlp);
        }
        break;

        case SUPLC_PROT_RRC:
        {
            /* Encode RRC Response message */
  			RRC_StopLocationFixRequest(p_inSuplCntl->hRrc);
        }
        break;
    }

	//User Stopped the location session. Stop the Lp Position
	p_inSuplCntl->bLpPosReq=FALSE;

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_StopLpPositionRequest: Exiting Successfully \n"));
    return retVal;
}




/**
 * Function:        SUPLC_sendMessage
 * Brief:           Sends messages to NAVC.
 * Description:
 * Note:
 * Params:          p_suplCtrl - SUPLC's control structure.
                    p_assistanceData - RRLP/RRC data comming from NW.
                    u16_assistDataLength - Assistance Data Length.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
*/
eSUPLC_Result SUPLC_assistanceDataInd(TSUPL_Ctrl *const p_inSuplCtrl,
                                      const McpU8 *const p_assistanceData,
                                      const McpU16 u16_assistDataLength)
{
    EMcpfRes retVal = RES_OK;
    eSUPLC_Result suplRetVal = SUPLC_SUCCESS;
    McpU32   u32_SystemTimeInMsec = 0;

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_assistanceDataInd: Entering \n"));

    /* Get System Time. */
    u32_SystemTimeInMsec = mcpf_getCurrentTime_InMilliSec(p_inSuplCtrl->hMcpf);

    switch(p_inSuplCtrl->e_protocol)
    {
        case SUPLC_PROT_RRLP:
        {
            p_inSuplCtrl->bLpPosReq = TRUE;
            /* Process incoming RRLP message. */
            retVal = RRLP_processRxMsg (p_inSuplCtrl->hRrlp,
                                        p_assistanceData,
                                        u16_assistDataLength,
                                        u32_SystemTimeInMsec);
            if (retVal != RES_OK)
            {
                PRINT_TAG(" SUPLC_assistanceDataInd: RRLP Processing FAILED !!! \n");
                return SUPLC_ERROR_ASSIST_DATA;
            }

#if 0
            /* Trigger appropriate event. SUPLC's state machine processes it. */
            suplRetVal = SUPLC_triggerEvent(evAssistInd, NULL, NULL);
            if (suplRetVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" SUPLC_assistanceDataInd: Event triggering FAILED !!! \n");
                PRINT_TAG(" REASON: %d \n", retVal);
            }
#endif

        }
        break;

        case SUPLC_PROT_RRC:
        {
            //RRC may trigger position Request.
            p_inSuplCtrl->bLpPosReq = TRUE;
            /* Process incoming RRC message. */
            retVal = RRC_processRxMsg (p_inSuplCtrl->hRrc,
                                       p_assistanceData,
                                       u16_assistDataLength,
                                       u32_SystemTimeInMsec);
            if (retVal != RES_OK)
            {
                PRINT_TAG(" SUPLC_assistanceDataInd: RRC Processing FAILED !!! \n");
                suplRetVal = SUPLC_ERROR_ASSIST_DATA;
            }

        }
        break;

        default:
        {
            PRINT_TAG(" SUPLC_assistanceDataInd: Position Protocol INVALID !!! \n");
            suplRetVal = SUPLC_ERROR_INVALID_POS_PROT;
        }
        break;
    }

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_assistanceDataInd: Exiting Successfully. \n"));
    return suplRetVal;
}
