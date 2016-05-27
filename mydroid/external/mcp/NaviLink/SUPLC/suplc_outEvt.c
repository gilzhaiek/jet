/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_outEvt.c
 *
 * Description      :
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */

#include "mcpf_mem.h"
#include "mcpf_msg.h"

#include "navc_api.h"
#include "navc_priority_handler.h"
#include "suplc_core_wrapper.h"

#include "suplc_outEvt.h"
#include "suplc_defs.h"
#include <utils/Log.h>
/* Temporary. To be removed. */
#define AUTONOMOUS_SESSION  254

#define ENABLE_SUPLC_DEBUG
#ifdef ENABLE_SUPLC_DEBUG
    #include <utils/Log.h>
    #define PRINT_TAG LOGD
    #define  DEBUG_SUPLC(x)   x
#else
    #define  DEBUG_SUPLC(x)   ((void)0)
#endif /* ENABLE_SUPLC_DEBUG */

/** Temporary Hack: Has to be removed.
  * Used when sending location fix request to NAVC. */
//T_GPSC_loc_fix_mode ge_gpsMode;
//T_GPSC_loc_fix_qop ge_gpsQop;



/**
 * Function:        SUPLC_registerAssistanceSource
 * Brief:           Sends messages to NAVC.
 * Description:
 * Note:
 * Params:          p_suplCtrl - SUPLC's control structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_registerAssistanceSource(const TSUPL_Ctrl *const p_inSuplCtrl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" SUPLC_registerAssistanceSource: Entering \n"));
    TNAVC_cmdParams *p_navcCmdParams = NULL;
    TNAVC_assist_src_type *pRegisterClient = NULL;
    p_navcCmdParams = (TNAVC_cmdParams *)mcpf_mem_alloc_from_pool(p_inSuplCtrl->hMcpf,p_inSuplCtrl->mMemPool);
    pRegisterClient = (TNAVC_assist_src_type *)p_navcCmdParams;
	if (NULL == pRegisterClient)
	{
	       PRINT_TAG(" register_client_with_navc(): Memory Allocation FAILED !!! \n");
	       return SUPLC_ERROR_MSG_SEND_FAILED;
	}
	pRegisterClient->eAssistSrcType = SUPL_PROVIDER;
	if(retVal != RES_OK)
	{
	        PRINT_TAG(" suplcSendMessage: Message sending FAILED !!! \n");
	        return SUPLC_ERROR_MSG_SEND_FAILED;
	}
   		
	PRINT_TAG(" suplcSendMessage: NAVC_CMD_REGISTER_ASSIST_SRC  \n");
    retVal = suplcSendMessage(p_inSuplCtrl, NAVC_CMD_REGISTER_ASSIST_SRC,
                              TASK_NAV_ID, (void *)pRegisterClient, sizeof(TNAVC_assist_src_type));
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPLC_registerAssistanceSource: Message sending FAILED !!! \n");
        return retVal;
    }

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_registerAssistanceSource: Exiting Successfully \n"));
    return SUPLC_SUCCESS;
}

/**
 * Function:        SUPLC_setAssistancePriority
 * Brief:           Sets the assistance source priority 
 * Description:
 * Note:
 * Params:          p_suplCtrl - SUPLC's control structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_setAssistancePriority(const TSUPL_Ctrl *const p_inSuplCtrl)
{
	eSUPLC_Result retVal = SUPLC_SUCCESS;
	TNAVC_assist_src_priority_set *pClientPriority;
    TNAVC_cmdParams *p_navcCmdParams = NULL;
	McpU16 index;
 
	p_navcCmdParams  = (TNAVC_cmdParams *)mcpf_mem_alloc_from_pool(p_inSuplCtrl->hMcpf,p_inSuplCtrl->mMemPool);
    pClientPriority = (TNAVC_assist_src_priority_set *)p_navcCmdParams;
        if (NULL == pClientPriority)
            {
                PRINT_TAG(" set_aGpsClient_priority(): Memory Allocation FAILED !!! \n");
                return SUPLC_ERROR_MSG_SEND_FAILED;
            }
            // first disable all the assisatnce providers and set their priority to 0 (disable)
            for(index = 0;index<MAX_ASSIST_PROVIDER;index++)
             {
                      pClientPriority[index].eAssistSrcType = 0xFF;
                      pClientPriority[index].assist_src_priority = 0;
             }
            // now set highest priority for SUPL_PROVIDER client
            pClientPriority[SUPL_PROVIDER].eAssistSrcType = SUPL_PROVIDER;
            pClientPriority[SUPL_PROVIDER].assist_src_priority = 1;

            if(retVal != RES_OK)
            {
                PRINT_TAG("NAVC_CMD_SET_ASSIST_PRIORITY  suplcSendMessage: Message sending FAILED !!! \n");
                return SUPLC_ERROR_MSG_SEND_FAILED;
            }
                PRINT_TAG(" suplcSendMessage: NAVC_CMD_SET_ASSIST_PRIORITY Exiting \n");

    	retVal = suplcSendMessage(p_inSuplCtrl, NAVC_CMD_SET_ASSIST_PRIORITY,
                     TASK_NAV_ID, (void *)pClientPriority, MAX_ASSIST_PROVIDER * sizeof(TNAVC_assist_src_priority_set));
	    if (retVal != SUPLC_SUCCESS)
    	    {
	        PRINT_TAG(" SUPLC_setAssistancePriority: Message sending FAILED !!! \n");
        	return retVal;
    	    }
	    return SUPLC_SUCCESS;
}


void SUPLC_delAidingData(const TSUPL_Ctrl *const p_suplCtrl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    
    TNAVC_delAssist *pDelAssist = NULL;

    LOGD("[GPS]  SUPLC_delAidingData Entering\n");

    pDelAssist = (TNAVC_delAssist*)mcpf_mem_alloc_from_pool(p_suplCtrl->hMcpf, p_suplCtrl->mMemPool);
    if (NULL == pDelAssist)
    {
        LOGD(" SUPLC_delAidingData(): Memory Allocation FAILED !!! \n");
        /* NT-APK - Start - End */
        return;
    }

    pDelAssist->uDelAssistBitmap = GPSC_DEL_AIDING_EPHEMERIS|GPSC_DEL_AIDING_ALMANAC|GPSC_DEL_AIDING_IONO_UTC|GPSC_DEL_AIDING_SVHEALTH|GPSC_DEL_AIDING_SVDIR|GPSC_DEL_AIDING_ACQ;
    pDelAssist->uSvBitmap = 0;

    retVal = suplcSendMessage(p_suplCtrl,NAVC_CMD_DELETE_ASISTANCE,TASK_NAV_ID,
                               (void *)pDelAssist,sizeof(TNAVC_delAssist));


    if (retVal != SUPLC_SUCCESS)
    {
        LOGD("SUPLC_delAidingData(): Sending request to MCPF FAILED !!! \n");
        //return retVal;
    }
}


/**
 * Function:        SUPLC_sendLocationFix
 * Brief:           Sends location fix request to NAVC.
 * Description:
 * Note:
 * Params:          p_suplCtrl - SUPLC's control structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_sendLocationFix(const TSUPL_Ctrl *const p_suplCtrl,T_GPSC_loc_fix_mode e_inGpsMode,T_GPSC_loc_fix_qop* p_inQop)
{
    TNAVC_cmdParams *p_navcCmdParams = NULL;
    TNAVC_reqLocFix *pReqLocFix = NULL;

    eSUPLC_Result retVal = SUPLC_SUCCESS;
    McpU16 u32_length = 0;

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_sendLocationFix: Entering \n"));

	//Always doing cold start in NI
	SUPLC_delAidingData(p_suplCtrl);

    /* TBD: Build Data to send */
    p_navcCmdParams = (TNAVC_cmdParams*)mcpf_mem_alloc_from_pool(p_suplCtrl->hMcpf,
                                                                 p_suplCtrl->mMemPool);
    if (p_navcCmdParams == NULL)
    {
        PRINT_TAG(" SUPLC_sendLocationFix: NULL Pointers \n");
        return SUPLC_ERROR_NULL_PTRS;
    }

    /* TBD: Fix Mode & Fix Frequency should be updated dynamically in NET Init Case. */
    pReqLocFix = (TNAVC_reqLocFix *)p_navcCmdParams;
    pReqLocFix->loc_fix_session_id = AUTONOMOUS_SESSION;
    pReqLocFix->loc_fix_mode = e_inGpsMode;
	memcpy(&pReqLocFix->loc_fix_qop,p_inQop,sizeof(T_GPSC_loc_fix_qop));
	pReqLocFix->loc_sess_type=GPSC_SESSTYPE_NI;
    pReqLocFix->loc_fix_result_type_bitmap = (GPSC_RESULT_PROT);
    //pReqLocFix->loc_fix_max_ttff = 0;
    pReqLocFix->loc_fix_max_ttff = 500;
    pReqLocFix->loc_fix_period = 1;
    pReqLocFix->loc_fix_num_reports = 1;
    pReqLocFix->call_type = NAVC_SUPL_MT_POSITIONING;


	//elements related wifi/sensor pos fix
	pReqLocFix->loc_fix_pos_type_bitmap =0;
    u32_length = sizeof(TNAVC_reqLocFix);

    retVal = suplcSendMessage(p_suplCtrl, NAVC_CMD_REQUEST_FIX,
                              TASK_NAV_ID, (void *)pReqLocFix, u32_length);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPLC_sendLocationFix: Message sending FAILED !!! \n");
        mcpf_mem_free_from_pool(p_suplCtrl->hMcpf, p_navcCmdParams);
        return retVal;
    }

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_sendLocationFix: Exiting Successfully \n"));
    return SUPLC_SUCCESS;
}

eSUPLC_Result SUPLC_pingNavc(const TSUPL_Ctrl *const p_inSuplCtrl)
{
  eSUPLC_Result retVal = SUPLC_ERROR_UNKNOWN;

    retVal = suplcSendMessage(p_inSuplCtrl, NAVC_CMD_CHECK_INIT_STATE, TASK_NAV_ID, NULL, 0);  
	if (retVal != SUPLC_SUCCESS)
        {
                PRINT_TAG(" SUPLC_startNavc: Sending NAVC_CMD_START FAILED !!! \n");
                return RES_ERROR;
        }
 return  retVal;
} 
/**
 * Function:        SUPLC_startNavc
 * Brief:           Sends messages to NAVC.
 * Description:
 * Note:
 * Params:          p_inSuplCtrl - SUPLC's Control Structure.
                    e_inGpsMode - GPS Fix Mode.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
*/
eSUPLC_Result SUPLC_startNavc(const TSUPL_Ctrl *const p_inSuplCtrl,
                              T_GPSC_loc_fix_mode e_inGpsMode,T_GPSC_loc_fix_qop* p_inQop,int startorlocfixr)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    TNAVC_cmdParams *p_navcCmdParams = NULL;
    TNAVC_reqLocFix *pReqLocFix = NULL;
    McpU16 u32_length = 0;
    DEBUG_SUPLC(PRINT_TAG(" SUPLC_startNavc: Entering \n"));

    /** Temporary Hack: Has to be removed.
      * Used when sending location fix request to NAVC. */
	//zz_Aby -NI  
    	
	//memcpy(&ge_gpsQop,p_inQop,sizeof(T_GPSC_loc_fix_qop));	
    if (startorlocfixr == 0)
    {
        retVal = suplcSendMessage(p_inSuplCtrl, NAVC_CMD_START, TASK_NAV_ID, NULL, 0);
        if (retVal != SUPLC_SUCCESS)
        {
            PRINT_TAG(" SUPLC_startNavc: Sending NAVC_CMD_START FAILED !!! \n");
            return RES_ERROR;
        }
    }
    else if (startorlocfixr == 1)
    {
        p_navcCmdParams = (TNAVC_cmdParams*)mcpf_mem_alloc_from_pool(p_inSuplCtrl->hMcpf,
                          p_inSuplCtrl->mMemPool);
        if (p_navcCmdParams == NULL)
        {
            PRINT_TAG(" SUPLC_sendLocationFix: NULL Pointers \n");
            return SUPLC_ERROR_NULL_PTRS;
        }
        pReqLocFix = (TNAVC_reqLocFix *)p_navcCmdParams;
        pReqLocFix->loc_fix_session_id = AUTONOMOUS_SESSION;
        pReqLocFix->loc_fix_mode = e_inGpsMode;
		pReqLocFix->loc_sess_type=GPSC_SESSTYPE_NI;
        pReqLocFix->loc_fix_result_type_bitmap = GPSC_RESULT_PROT;
        pReqLocFix->loc_fix_max_ttff = 500;
        pReqLocFix->loc_fix_period = 20;
		memcpy(&pReqLocFix->loc_fix_qop,p_inQop,sizeof(T_GPSC_loc_fix_qop));
        pReqLocFix->loc_fix_num_reports = 1;
        pReqLocFix->call_type = NAVC_SUPL_MT_POSITIONING;
        u32_length = sizeof(TNAVC_reqLocFix);
		
		SUPLC_delAidingData(p_inSuplCtrl);
        retVal = suplcSendMessage(p_inSuplCtrl, NAVC_CMD_REQUEST_FIX, TASK_NAV_ID, (void *)pReqLocFix, u32_length);
        if (retVal != SUPLC_SUCCESS)
        {
            PRINT_TAG(" SUPLC_startNavc: Sending NAVC_CMD_REQUEST_FIX FAILED !!! \n");
            return RES_ERROR;
        }
    }

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_startNavc: Exiting Successfully \n"));
    return SUPLC_SUCCESS;

}
eSUPLC_Result SUPLC_sendPosInd(const TSUPL_Ctrl *const p_inSuplCtrl,
                               TNAVC_posIndData* posdata, const McpU16 Len)
{
   eSUPLC_Result retVal = SUPLC_SUCCESS;
   TNAVC_cmdParams *p_navcCmdParams = NULL;
   TNAVC_posIndData *p_posIndData = NULL;
   p_navcCmdParams = (TNAVC_cmdParams *)mcpf_mem_alloc_from_pool(p_inSuplCtrl->hMcpf,p_inSuplCtrl->mMemPool);
    if (p_navcCmdParams == NULL)
    {
        PRINT_TAG(" SUPLC_sendPosInd: NULL Pointers \n");
        return SUPLC_ERROR_NULL_PTRS;
    }
	p_navcCmdParams->tPosInd.position = posdata->position;
	p_navcCmdParams->tPosInd.app_id= posdata->app_id;
	retVal = suplcSendMessage(p_inSuplCtrl, NAVC_CMD_POS_IND,
                              TASK_NAV_ID, (void *)p_navcCmdParams, sizeof(TNAVC_posIndData));
    if (retVal != SUPLC_SUCCESS)
    {
        LOGD(" SUPLC_sendPosInd: Sending NAVC_CMD_START FAILED !!! \n");
        return RES_ERROR;
    }
	return SUPLC_SUCCESS;
}

/**
 * Function:        SUPLC_stopNavc
 * Brief:           Sends messages to NAVC.
 * Description:
 * Note:
 * Params:          p_inSuplCtrl - SUPLC's Control Structure.
                    e_inGpsMode - GPS Fix Mode.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
*/
eSUPLC_Result SUPLC_stopNavc(const TSUPL_Ctrl *const p_inSuplCtrl,
                              void* p_inData, McpU32 length)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" SUPLC_stopNavc: Entering \n"));

    retVal = suplcSendMessage(p_inSuplCtrl, NAVC_CMD_STOP_FIX, TASK_NAV_ID, p_inData, length);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPLC_stopNavc: Sending NAVC_CMD_STOP_FIX FAILED !!! \n");
        return RES_ERROR;
    }

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_stopNavc: Exiting Successfully \n"));
    return SUPLC_SUCCESS;

}

/**
 * Function:		SUPLC_sendSessResult
 * Brief:			Sends the result of the SUPL session to NAVC.
 * Description:
 * Note:
 * Params:			p_inSuplCtrl - SUPLC's Control Structure.
					p_inData - input param
					length - length of data.
 * Return:			Success: SUPLC_SUCCESS.
					Failure: RES_ERROR.
*/
eSUPLC_Result SUPLC_sendSessResult(const TSUPL_Ctrl *const p_inSuplCtrl, 
											void* p_inData)
{
    EMcpfRes retVal = RES_COMPLETE; 
	StopIndData* dta = (StopIndData*)p_inData;
	TNAVC_cmdParams *p_navcCmdParams;
	p_navcCmdParams = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool(p_inSuplCtrl->hMcpf,
															  p_inSuplCtrl->mMemPool);

	/* Klocwork Changes */
	if (p_navcCmdParams == NULL)
	{
		PRINT_TAG(" SUPLC_stopNavc: mcpf_mem_alloc_from_pool return NULL !!! \n");
		return RES_ERROR;
	}

	if(dta->gps_stoped == 0)
	{
		 p_navcCmdParams->tSuplSessionSuccess = 1;
	}
	else
	{
		 p_navcCmdParams->tSuplSessionSuccess = 0;
	}

	retVal = suplcSendMessage(p_inSuplCtrl, NAVC_EVT_SUPLC_SESSION_RESULT, TASK_NAV_ID, 
								 (void *)p_navcCmdParams, sizeof(McpU8));
	if (retVal != SUPLC_SUCCESS)
	{
		PRINT_TAG(" SUPLC_stopNavc: Sending NAVC_CMD_STOP_FIX FAILED !!! \n");
		return RES_ERROR;
	}

	return SUPLC_SUCCESS;
}


/**
 * Function:        SUPLC_sendMessage
 * Brief:           Sends messages to NAVC.
 * Description:
 * Note:
 * Params:          p_inSuplCtrl - SUPLC's control structure.
                    e_inOpcode - Opcode of the message.
                    e_inDestTaskId - Destination Task ID.
                    p_inData - Payload.
                    u16_inlength - Data length.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result suplcSendMessage(const TSUPL_Ctrl *const p_inSuplCtrl,
                                      const ENAVC_cmdOpcode e_inOpcode,
                                      const EmcpTaskId e_inDestTaskId,
                                      const void *const p_inData,
                                      const McpU16 u16_inlength)
{
    EMcpfRes retVal = RES_COMPLETE;                     /* Return Value */
    McpU8   u8_DestQueId;                                 /* Destination Queue ID */
    PRINT_TAG(" suplcSendMessage: Entering \n");
    McpU16 index ;
    switch (e_inOpcode)
    {
    	case NAVC_EVT_SUPLC_SESSION_RESULT:
        {
            u8_DestQueId = NAVC_QUE_INTERNAL_EVT_ID;
    		PRINT_TAG(" suplcSendMessage: NAVC_EVT_SUPLC_SESSION_RESULT Result \n");
        }
         break;			
        case NAVC_CMD_REGISTER_ASSIST_SRC:
        {
		
		u8_DestQueId = NAVC_QUE_CMD_ID;

        }     
         break;
        case NAVC_CMD_SET_ASSIST_PRIORITY :
        {
            u8_DestQueId = NAVC_QUE_CMD_ID;
    		PRINT_TAG(" suplcSendMessage: NAVC_CMD_SET_ASSIST_PRIORITY Exiting \n");
        }
         break;
        case NAVC_CMD_SUPL_PROVIDER_REGISTER:
        {
            /** Register SUPLC with NAVC.
              * Needn't send any payload as it registers with its task id and queue id.
              * Hitherto, NAVC will directly communicate with SUPLC for assistance requests.
              */
            u8_DestQueId = NAVC_QUE_CMD_ID;
        }
        break;

        case NAVC_CMD_REQUEST_FIX:
        case NAVC_CMD_STOP_FIX:
		case NAVC_CMD_DELETE_ASISTANCE:
        {
            /* Location fix command is sent to NAVC on its command queue. */
            u8_DestQueId = NAVC_QUE_CMD_ID;
        }
        break;

    	case NAVC_CMD_POS_IND:
			{
							  u8_DestQueId = NAVC_QUE_CMD_ID;
			}
							  break; 
		
        case NAVC_CMD_START:
        case NAVC_CMD_STOP:
        default:
        {
            u8_DestQueId = NAVC_QUE_ACTION_ID;
        }
        break;
    }


    /* Send message */
    retVal = mcpf_SendMsg(p_inSuplCtrl->hMcpf, e_inDestTaskId, u8_DestQueId,
                          TASK_SUPL_ID, SUPLC_INTERNAL_EVT_QUEUE_ID, e_inOpcode,
                          u16_inlength, 0, p_inData);
    if(retVal != RES_OK)
    {
        PRINT_TAG(" suplcSendMessage: Message sending FAILED !!! \n");
        return SUPLC_ERROR_MSG_SEND_FAILED;
    }

    DEBUG_SUPLC(PRINT_TAG(" suplcSendMessage: Exiting Successfully \n"));
    return SUPLC_SUCCESS;
}
