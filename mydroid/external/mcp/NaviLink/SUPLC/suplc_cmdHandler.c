/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_cmdHandler.c
 *
 * Description      :
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */

#include "mcpf_mem.h"
#include "mcpf_nal_common.h"

#include "suplc_app_api.h"
#include "suplc_api.h"

#include "suplc_cmdHandler.h"

#define ENABLE_SUPLC_DEBUG
#ifdef ENABLE_SUPLC_DEBUG
    #include <utils/Log.h>
    #define PRINT_TAG LOGD
    #define  DEBUG_SUPLC(x)   x
#else
    #define  DEBUG_SUPLC(x)   ((void)0)
#endif /* ENABLE_SUPLC_DEBUG */

static eSUPLC_Result setLocationFixMode(const TSUPLC_CmdParams *const p_cmdParams,
                                       TSUPL_Ctrl *const p_suplCntl);

/**
 * Function:        SUPLC_commandHandler
 * Brief:           Handles various commands sent to SUPLC
 * Description:     This functions is called whenever a message intended for SUPLC is
                    put on its command queue.
 * Note:
 * Params:          hSuplc - handle to SUPLC's control structure.
                    pMsg - Contains all details about the message.
 * Return:          Success: RES_SUCCESS.
                    Failure: RES_ERROR.
 */
EMcpfRes SUPLC_commandHandler(handle_t hSuplc, TmcpfMsg *p_msg)
{
    TSUPL_Ctrl *p_suplCntl = NULL;
	
    TSUPLC_CmdParams *p_cmdParams = NULL;
    s_NAL_msgBoxResp *p_msgRespData = NULL;


    eSUPLC_Result retVal = SUPLC_SUCCESS;
    EMcpfRes mcpfRetVal = RES_COMPLETE;

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_commandHandler: Entering \n"));

	/* Klocwork Changes */
	/*if(p_msg == NULL)
	{
		return RES_ERROR;
	}
	*/
    p_suplCntl = (TSUPL_Ctrl *) hSuplc;
    p_cmdParams = (TSUPLC_CmdParams *) p_msg->pData;
	p_msgRespData = (s_NAL_msgBoxResp*) p_msg->pData;
	if(p_msg->pData == NULL)
		LOGD("pData is null");

    /* Just in case... */
    if ( (p_suplCntl == NULL) ||
         (p_cmdParams == NULL) )
    {
        PRINT_TAG(" SUPLC_commandHandler: NULL Pointers \n");
       mcpfRetVal = RES_ERROR; //return RES_ERROR; /* Klocwork Changes */
    }

    /* Store the source of message to return response to it if required. */
	/* Klocwork Changes */
	if(p_suplCntl != NULL)
    {
    	p_suplCntl->e_srcTaskId = p_msg->eSrcTaskId;
    	p_suplCntl->u8_srcQId = p_msg->uSrcQId;
	}

    switch(p_msg->uOpcode)
    {
        case SUPLC_CMD_FROM_NETWORK:
        {
			/* This message could either be SMS/WAP or SLP message */
            retVal = SUPLC_processNetworkMessage(p_cmdParams);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" SUPLC_commandHandler: N/W Message Processing FAILED !!! \n");
                mcpfRetVal = RES_ERROR;
            }
        }
        break;

        case SUPLC_CMD_SET_MODE:
        {
            /* Store location fix mode in SUPLC's internal data structures */
            retVal = setLocationFixMode(p_cmdParams, p_suplCntl);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" SUPLC_commandHandler: Setting Location Fix Mode FAILED !!! \n");
                mcpfRetVal = RES_ERROR;
            }
        }
        break;

        /* Added for NI */
        case SUPLC_CMD_MSGBOX_RESP:
        {
            /** Response from MSGBOX.
              * This is implemented in suplc_app_api.c */
            LOGD("SUPLC_commandHandler: SUPLC_CMD_MSGBOX_RESP");
			/* Klocwork Changes */
			if(p_msgRespData != NULL)
			{
				passNotificationResult(p_msgRespData->res, p_msgRespData->tid);
			}
            
        }
        break;

        case SUPLC_CMD_INIT_CORE:
        {
            /** Initialize SUPL Core.
              * This is implemented in suplc_app_api.c */
            retVal = SUPLC_appInitReq(p_suplCntl);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" SUPLC_commandHandler: SUPL Core's initialization FAILED !!! \n");
                return RES_ERROR;
            }
			//zz_Aby
			/* Added for NI */
			 retVal = setLocationFixMode(p_cmdParams, p_suplCntl);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" SUPLC_commandHandler: Setting Location Fix Mode FAILED !!! \n");
                mcpfRetVal = RES_ERROR;
            }
        }
        break;

        case SUPLC_CMD_DEINIT_CORE:
        {
            /** Initialize SUPL Core.
              * This is implemented in suplc_app_api.c */
            retVal = SUPLC_appDeInitReq(p_suplCntl);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" SUPLC_commandHandler: SUPL Core's initialization FAILED !!! \n");
                return RES_ERROR;
            }
        }
        break;		
       		
       case SUPLC_CMD_SET_SLP:
	    {
                unsigned char packed_buff[16];
                LOGD(" SUPLC_commandHandler:SUPLC_CMD_SLP_ADDRESS %s ", p_cmdParams );
            // SetSLPAddress_c("208.8.164.7:7275"); // For testing 
	 	   SetSLPAddress_c(p_cmdParams);
	    }
           break;
       case SUPLC_RSP_NAVC_STATE:
           {
	   		if(p_msg->pData != NULL)  /* Klocwork Changes */
	   		{
				if(SetNavcInit(p_msg->pData, 1) == -1)
              	LOGD("SUPLC_commandHandler: SUPLC_RSP_NAVC_STATE Failed!!!"); 
	   		}
             
           }
           break;

	     case SUPLC_CMD_SERVER_CONNECT:
        {
			/* This message could either be SMS/WAP or SLP message */
			if(p_msg->pData != NULL) /* Klocwork Changes */
			{
				retVal = SUPLC_ConnectionResponse(*((McpU8*)(p_msg->pData)));
	            if (retVal != SUPLC_SUCCESS) 
	            {
	                PRINT_TAG(" SUPLC_commandHandler: Server Connection Response Processing FAILED !!! \n");
	                mcpfRetVal = RES_ERROR;
	            }
			}
			
            
        }	   

        default:
        {
            PRINT_TAG(" SUPLC_commandHandler: Default Case !!! \n");
            mcpfRetVal = RES_ERROR;
        }
        break;
    }

	/* Free received msg and data buffer */ 
	if (p_msg->pData && p_suplCntl)/* Klockwork changes */
	{
		mcpf_mem_free_from_pool (p_suplCntl->hMcpf, p_msg->pData);
		p_msg->pData = NULL;
	}
	if(p_msg && p_suplCntl) /* Klockwork changes */
	{
		mcpf_mem_free_from_pool (p_suplCntl->hMcpf, (void *) p_msg);
		p_msg =  NULL;
	}
    return mcpfRetVal;
}



/**
 * Function:        setLocationFixMode
 * Brief:           This function sets the location fix mode.
 * Description:
 * Note:
 * Params:          p_cmdParams - Command Params.
                    p_suplCntl - SUPLC's Control Structure.
                    pMsg - Contains all details about the message.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result setLocationFixMode(const TSUPLC_CmdParams *const p_cmdParams,
                                        TSUPL_Ctrl *const p_suplCntl)
{
    TSUPLC_FixMode *p_fixMode = NULL;

    DEBUG_SUPLC(PRINT_TAG(" setLocationFixMode: Entering \n"));

    p_fixMode = (TSUPLC_FixMode *)&p_cmdParams->s_fixMode;
    /* Just in case... */
    if (p_fixMode == NULL)
    {
        PRINT_TAG(" setLocationFixMode: NULL Pointers. \n");
       return SUPLC_ERROR_NULL_PTRS;
    }

    /* */
    p_suplCntl->u8_fixMode = p_fixMode->e_locFixMode;
    p_suplCntl->u16_fixFrequency = p_fixMode->u16_fixFrequency;

    DEBUG_SUPLC(PRINT_TAG(" setLocationFixMode: Exiting Successfully \n"));
    return SUPLC_SUCCESS;
}
