/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_core_api.c
 *
 * Description      :
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */

#include "mcpf_defs.h"
#include "mcpf_mem.h"
#include "mcpf_nal.h"
#include "suplc_defs.h"
#include "navc_api.h"
#include "suplc_outEvt.h"

#include "suplc_protPos_api.h"
#include "suplc_core_api.h"
#include "suplc_api.h"
#include "suplc_state.h"
#include "suplc_hal_os.h"
#include "suplc_core_wrapper.h"
#include <utils/Log.h>
#include "nav_log_msg.h"
#include "nav_log_cust_codes.h"

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
  * processResponseMsg needs this information. */
TSUPL_Ctrl *gp_suplCntl = NULL;

extern SEM_HANDLE gp_rrlpPkt;
extern navLog navLogData;
static eSUPLC_Result getPositionFix(void *p_inBufHandle);


//Requested assited data
#define ALMANAC_REQ_MASK                0x001
#define UTC_MODE_REQ_MASK               0x002
#define IONOSPHERIC_MODE_REQ_MASK       0x004
#define DGPS_CORRECTIONS_REQ_MASK       0x008
#define REFERENCE_LOCATION_REQ_MASK     0x010
#define REFERENCE_TIME_REQ_MASK         0x020
#define ACQUI_ASSIST_REQ_MASK           0x040
#define REAL_TIME_INT_REQ_MASK          0x080
#define NAVIGATION_MODEL_REQ_MASK       0x100
#define EXTENDED_EPHEMERIS_REQ_MASK     0x200 //rsuvorov extended ephemeris improvement

/* Map to suplc_core_wrapper.h */
#define RRC_PAYLOAD						0x000
#define RRLP_PAYLOAD					0x001
#define TIA_801_PAYLOAD                 0x002 /* KlockWork - was defined 3, but PayloadType enum is 2 */

#define SUPL_APP_ID             1001 //zz_Aby - appID
#define EE_CLIENT_APP_ID        0x5050 //aplication id for ee client

#define TRUE 1
#define FALSE 0



extern int RRLPPacketAvailable;
extern char *p_RRLPbuffer;
extern int RRLPbuffersize;
//extern bool SUPLErrorFlag;

/**
 * Function:        SUPLC_initSuplClient
 * Brief:           Entry functions for INIT state.
 * Description:
 * Note:            External Function.
 * Params:          p_data - Data. NULL here.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_initSuplClient(void *p_inData, const void *const p_inSuplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_enterInitState: Entering \n"));
    //MCPF_UNUSED_PARAMETER(p_inData); /* KlockWork Changes */

    /* Register MCPF_NAL callbacks with SUPL Core. */
    retVal = registerNalCallbacks(p_inData);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPLC_initSuplClient: NAL registration with SUPL Core FAILED !!! \n");
        return retVal;
    }

    /* Initiatialize SUPL Client */
    retVal = initSuplClient(NULL);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPLC_initSuplClient: SUPL Client Initialization FAILED !!! \n");
        return retVal;
    }
	gp_suplCntl = p_inSuplCntl;

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_initSuplClient: Exiting Successfully \n"));
    return retVal;
}


/**
 * Function:        SUPLC_deInitSuplClient
 * Brief:           Entry functions for INIT state.
 * Description:
 * Note:            External Function.
 * Params:          p_data - Data. NULL here.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_deInitSuplClient(void *p_inData)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;

    DEBUG_SUPLC(PRINT_TAG(" +SUPLC_deInitSuplClient \n"));
    //MCPF_UNUSED_PARAMETER(p_inData); /* KlockWork Changes */

    /* ToDo: UnRegister MCPF_NAL callbacks with SUPL Core. */

    /* DeInitiatialize SUPL Client */
    retVal = deInitSuplClient(NULL);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPLC_deInitSuplClient: SUPL Client DeInitialization FAILED !!! \n");
        return retVal;
    }

    DEBUG_SUPLC(PRINT_TAG(" -SUPLC_deInitSuplClient \n"));
    return retVal;
}

/**
 * Function:        SUPLC_handleNetworkMessage
 * Brief:           Handles network messages.
 * Description:
 * Note:
 * Params:          p_data - Data. NULL here.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_handleNetworkMessage(const void *const p_data, const void *const p_inSuplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_enterNetInitState: Entering \n"));

    retVal = handleNetworkMessage(p_data);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPLC_enterNetInitState: Network Message Handling FAILED !!! \n");
        return retVal;
    }

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_enterNetInitState: Exiting Successfully \n"));
    return retVal;
}


/**
 * Function:        SUPLC_startSuplSession
 * Brief:           Starts SUPL Session with SLP.
 * Description:
 * Note:
 * Params:          p_inData - Data. NULL here.
                    p_inSuplCntl - Pointer to SUPLC's Control Structure
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_startSuplSession(const void *const p_inData,
                                     const void *const p_inSuplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" SUPLC_startSuplSession: Entering \n"));

    retVal = startSuplSession(p_inData, p_inSuplCntl);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPLC_startSuplSession: Session Start FAILED !!! \n");
        return retVal;
    }

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_startSuplSession: Exiting Successfully \n"));
    return retVal;
}




eSUPLC_Result SUPLC_stopSuplSession(const void *const p_inData,
                                     const void *const p_inSuplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    DEBUG_SUPLC(PRINT_TAG(" SUPLC_stopSuplSession: Entering \n"));

    retVal = stopSuplSession(p_inData, p_inSuplCntl);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPLC_stopSuplSession: Session Start FAILED !!! \n");
        return retVal;
    }

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_stopSuplSession: Exiting Successfully \n"));
    return retVal;
}






/**
 * Function:        SUPLC_posReport
 * Brief:           Processes pos report from NAVC.
 * Description:
 * Note:
 * Params:          p_data - Data. NULL here.
                    p_suplCntl - Pointer to SUPLC's Control Structure
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_posReport(const void *const p_data,const void *const p_suplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_posReport: Entering \n"));

    retVal = handlePositionReport(p_data, p_suplCntl);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPLC_posReport: Position Report Handling FAILED !!! \n");
        return retVal;
    }

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_posReport: Exiting Successfully \n"));
    return retVal;
}


/**
 * Function:        SUPLC_posResponse
 * Brief:           Processes RRLP/RRC position responses from NAVC.
 * Description:
 * Note:
 * Params:          p_data - Data. NULL here.
                    p_suplCntl - Pointer to SUPLC's Control Structure
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_posResponse(const void *const p_data, const void *const p_suplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_posResponse: Entering \n"));

    retVal = handlePositionResponse(p_data, p_suplCntl);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPLC_posResponse: Position Response Handling FAILED !!! \n");
        return retVal;
    }

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_posResponse: Exiting Successfully \n"));
    return retVal;
}



/**
 * Function:        initSuplClient
 * Brief:           Initializes SUPL Client.
 * Description:
 * Note:            Internal Function.
 * Params:          p_inData - Data (NULL)
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result initSuplClient(void *p_inData)
{
    McpU8 retVal = 0;

    DEBUG_SUPLC(PRINT_TAG(" initSuplClient: Entering \n"));
    //MCPF_UNUSED_PARAMETER(p_inData); /* KlockWork Changes */

    /** This initializes SUPL Client.
      * Pass callback function pointer which will be called by SUPLCore for sending
      * responses to NAVC. */
    LOGD("initSuplClient: navLogDate: %x", &navLogData);

	if(NAL_connectToHS()!=RES_COMPLETE)
	{
		PRINT_TAG(" initSuplClient: NAL_connectToHS FAILED !!! \n");
		return SUPLC_ERROR_INIT_FAILED;
	}
		
    retVal = SUPLC_initClient(processResponseMsg, (void *) &navLogData);
    if (retVal != 1)
    {
        PRINT_TAG(" initSuplClient: 1. SUPL Client initialization FAILED !!! \n");
        return SUPLC_ERROR_INIT_FAILED;
    }

    /* This starts SUPL Client. */
    retVal = SUPLC_initClient(NULL, &navLogData);
    if (retVal != 1)
    {
        PRINT_TAG(" initSuplClient: 2. SUPL Client initialization FAILED !!! \n");
        return SUPLC_ERROR_INIT_FAILED;
    }



    DEBUG_SUPLC(PRINT_TAG(" initSuplClient: Exiting Successfully \n"));
    return SUPLC_SUCCESS;
}


/**
 * Function:        deInitSuplClient
 * Brief:           DeInitializes SUPL Client.
 * Description:
 * Note:            Internal Function.
 * Params:          p_inData - Data (NULL)
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result deInitSuplClient(void *p_inData)
{
    McpU8 retVal = 0;

    DEBUG_SUPLC(PRINT_TAG(" +deInitSuplClient ### \n"));
    //MCPF_UNUSED_PARAMETER(p_inData); /* KlockWork Changes */

    /* DeInit SUPL Client. */
	DEBUG_SUPLC(PRINT_TAG(" deInitSuplClient: -> SUPLC_deInitClient \n"));
    retVal = SUPLC_deInitClient();
    if (retVal != 1)
    {
  		DEBUG_SUPLC(PRINT_TAG(" deInitSuplClient: <- SUPLC_deInitClient \n"));
        PRINT_TAG(" deInitSuplClient: SUPL Client deinitialization FAILED !!! \n");
        return SUPLC_ERROR_INIT_FAILED;
    }
	usleep(1000);
	NAL_closeConnectionToHS();
	
	DEBUG_SUPLC(PRINT_TAG(" deInitSuplClient: <- SUPLC_deInitClient \n"));

    DEBUG_SUPLC(PRINT_TAG(" -deInitSuplClient ### \n"));
    return SUPLC_SUCCESS;
}


/**
 * Function:        registerNalCallbacks
 * Brief:           Registers MCPF_NAL callbacks.
 * Description:
 * Note:            Internal Function.
 * Params:          p_inData - Data (NULL)
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result registerNalCallbacks(void *p_inData)
{
    McpU8 retVal = 0;

    DEBUG_SUPLC(PRINT_TAG(" registerNalCallbacks: Entering \n"));
    //MCPF_UNUSED_PARAMETER(p_inData); /* KlockWork Changes */

    /** Register with MCPF_NAL function with SUPL Core.
      * SUPL Core will call this function for any network specific operation. */
    retVal = SUPLC_registerNalHandler(nalExecuteCommand);
    if (retVal != 1)
    {
        PRINT_TAG(" registerNalCallbacks: Registering NAL Handler FAILED !!!  \n");
    return SUPLC_ERROR_NAL_REG_FAILED;
    }

    DEBUG_SUPLC(PRINT_TAG(" registerNalCallbacks: Exiting Successfully \n"));
    return SUPLC_SUCCESS;
}

/**
 * Function:        handleNetworkMessage
 * Brief:           Handles network message.
 * Description:
 * Note:
 * Params:          p_inData - Data
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result handleNetworkMessage(const void *const p_inData)
{
    TSUPLC_NetworkMsg *p_networkMessage = NULL;
    McpS8 retVal = 0;

    DEBUG_SUPLC(PRINT_TAG(" handleNetworkMessage: Entering \n"));

    p_networkMessage = (TSUPLC_NetworkMsg *)p_inData;

    /* Just in case...*/
    if ( (p_networkMessage == NULL)/* ||
         (p_networkMessage->p_msg == NULL)*/ )
    {
        PRINT_TAG(" SUPLC_enterNetInitState: NULL Pointers \n");
        return SUPLC_ERROR_NULL_PTRS;
    }

    /* Ask SUPLCore's network component to process the received data. */
    retVal = SUPLC_sendNetworkMessage((const char *)p_networkMessage->a_message,
                                      (uInt16)(p_networkMessage->u16_msgSize)); /* KlockWork - loss of Precision */
    if ( retVal < 0 )
    {
        PRINT_TAG(" handleNetworkMessage: Network::HandleMessage FAILED !!! \n");
        return SUPLC_ERROR_UNKNOWN;
    }

    DEBUG_SUPLC(PRINT_TAG(" handleNetworkMessage: Exiting Successfully \n"));
    return SUPLC_SUCCESS;
}

/**
 * Function:        startSuplSession
 * Brief:           Starts SUPL Session with SLP.
 * Description:
 * Note:
 * Params:          p_inData - Data
                    p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result startSuplSession(const void *const p_inData,
                                      const void *const p_inSuplCntl)
{
    StartReqData *p_startReqData = NULL;
    eSUPLC_Result retVal = SUPLC_SUCCESS;

    McpU16 u16_appId = 0;

    DEBUG_SUPLC(PRINT_TAG(" startSuplSession: Entering \n"));

    /* Fill StartReqData structure. */
    retVal = fillStartRequestData(&p_startReqData, &u16_appId, p_inData, p_inSuplCntl);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" startSuplSession: Data filling FAILED \n");
		/* KlockWork Changes */
		if ((p_startReqData) != NULL)
		{
			free((p_startReqData));
			p_startReqData = NULL;
		}
        return retVal;
    }
    PRINT_TAG("\n\nIn startSuplSession\n\n");
	DEBUG_SUPLC(PRINT_TAG(" startSuplSession: u16_appId = %d \n",u16_appId));
    /* Send UPL_START_REQ to SUPL Core Module. */
    retVal = sendDataToCore(u16_appId, (McpU8 *)p_startReqData, UPL_START_REQ);
    if (retVal != SUPLC_SUCCESS)
    {
            PRINT_TAG(" startSuplSession: Sending data to Core FAILED \n");
            return retVal;
    }

    DEBUG_SUPLC(PRINT_TAG(" startSuplSession: Exiting Successfully \n"));
    return retVal;
}





static eSUPLC_Result stopSuplSession(const void *const p_inData,
                                      const void *const p_inSuplCntl)
{
    //StartReqData *p_startReqData = NULL;
    eSUPLC_Result retVal = SUPLC_SUCCESS;
	//TNAVC_stopReq *p_stopReqData = (TNAVC_stopReq *)p_inData;

	//TSUPL_Ctrl * p_suplCntl = NULL; /* KlockWork Changes */
    //p_suplCntl = (TSUPL_Ctrl *) p_inSuplCntl; /* KlockWork Changes */

	StopReqData* p_stopReqData =NULL;

	//p_stopReqData = (StopReqData *) mcpf_mem_alloc_from_pool(p_suplCntl->hMcpf, p_suplCntl->mMemPool);

	p_stopReqData = (StopReqData *) malloc(sizeof(StopReqData));

	if(NULL==p_stopReqData)
	{
		DEBUG_SUPLC(PRINT_TAG(" stopSuplSession: Failed - Memory Full \n"));
		return SUPLC_ERROR_NULL_PTRS;
	}
	p_stopReqData->app_id=SUPL_APP_ID;
	p_stopReqData->gps_stoped=FAILURE;



    DEBUG_SUPLC(PRINT_TAG(" stopSuplSession: Entering \n"));
	DEBUG_SUPLC(PRINT_TAG("p_stopReqData->app_id = %d,p_stopReqData->gps_stoped = %d \n",p_stopReqData->app_id,p_stopReqData->gps_stoped));

	//First Remove all commands from Command Queue
	SUPLC_RemoveQueuedUplMsgs();
	retVal = sendDataToCore(p_stopReqData->app_id, (McpU8 *)p_stopReqData, UPL_STOP_REQ);

    if (retVal != SUPLC_SUCCESS)
    {
            PRINT_TAG(" stopSuplSession: Sending data to Core FAILED \n");
            return retVal;
    }

	DEBUG_SUPLC(PRINT_TAG(" stopSuplSession: Stopping LP Position Request \n"));

	//Stop the Lp session if it is ongoing
	//Removed as it cuases Stop Failure
	//SUPLC_StopLpPositionRequest(p_inSuplCntl);

    DEBUG_SUPLC(PRINT_TAG(" stopSuplSession: Exiting Successfully \n"));
    return retVal;
}





/**
 * Function:        handlePositionReport
 * Brief:           Handles the position report from NAVC.
 * Description:
 * Note:
 * Params:          p_data - Data
                    p_suplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result handlePositionReport(const void *const p_data,
                                          const void *const p_suplCntl)
{
    DataReqData *p_dataReqData = NULL;
    eSUPLC_Result retVal = SUPLC_SUCCESS;
    //StartReqData *p_startReqData = NULL; /* KlockWork - Unused */

    McpU16 u16_appId = 0;

    DEBUG_SUPLC(PRINT_TAG(" handlePositionReport: Entering \n"));

    /* Fill DataReqData structure. */
    retVal = fillDataReqData(&p_dataReqData, &u16_appId, p_data, p_suplCntl);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" handlePositionReport: Data filling FAILED \n");
        return retVal;
    }

    /* Send UPL_DATA_REQ to SUPL Core Module. */
    retVal = sendDataToCore(u16_appId, (McpU8 *)p_dataReqData, UPL_DATA_REQ);
    if (retVal != SUPLC_SUCCESS)
    {
            PRINT_TAG(" handlePositionReport: Sending data to Core FAILED \n");
            return retVal;
    }

    DEBUG_SUPLC(PRINT_TAG(" handlePositionReport: Exiting Successfully \n"));
    return retVal;
}


/**
 * Function:        handlePositionResponse
 * Brief:           Handles the position response from NAVC.
 * Description:
 * Note:
 * Params:          p_data - Data
                    p_suplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result handlePositionResponse(const void *const p_data,
                                            const void *const p_suplCntl)
{
    /* Note: This function is same as the previous one, but included this for readability. */

    DataReqData *p_dataReqData = NULL;
    eSUPLC_Result retVal = SUPLC_SUCCESS;

    McpU16 u16_appId = 0;

    DEBUG_SUPLC(PRINT_TAG(" handlePositionResponse: Entering \n"));

    /* Fill DataReqData structure. */
    retVal = fillDataReqDataForPosResp(&p_dataReqData, &u16_appId, p_data, p_suplCntl);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" handlePositionResponse: Data filling FAILED \n");
        return retVal;
    }

    /* Send UPL_DATA_REQ to SUPL Core Module. */
    retVal = sendDataToCore(u16_appId, (McpU8 *)p_dataReqData, UPL_DATA_REQ);
    if (retVal != SUPLC_SUCCESS)
    {
            PRINT_TAG(" handlePositionResponse: Sending data to Core FAILED \n");
            return retVal;
    }

    DEBUG_SUPLC(PRINT_TAG(" handlePositionResponse: Exiting Successfully \n"));
    return retVal;
}

/**
 * Function:        fillStartRequestData
 * Brief:           Fills StartRequestData Structure .
 * Description:
 * Note:
 * Params:          p_startReqData - Pointer to startReqData structure.
                    p_outAppId - App Id.
                    p_inData - Data
                    p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result fillStartRequestData(StartReqData **p_startReqData,
                                          McpU16 *p_outAppId,
                                          const void *const p_inData,
                                          const void *const p_inSuplCntl)
{
    TNAVC_assistReq *p_assistReqData = NULL;
    TSUPL_Ctrl *p_suplCntl = NULL;

    eSUPLC_Result retVal = SUPLC_SUCCESS;

    DEBUG_SUPLC(PRINT_TAG(" fillStartRequestData: Entering \n"));

    p_assistReqData = (TNAVC_assistReq *)p_inData;
    p_suplCntl = (TSUPL_Ctrl *) p_inSuplCntl;

    /* Just in case... */
    if ( (p_assistReqData == NULL) ||
         (p_suplCntl == NULL) )
    {
        PRINT_TAG(" fillStartRequestData: NULL Pointers - Non GPS case \n");
        return SUPLC_ERROR_NULL_PTRS; /* Added with KlockWork Changes*/
    }


    /** Temporary Hack: Has to be removed.
      * SUPLCore callback function "processResponseMsg" needs this information. */
    gp_suplCntl = p_suplCntl;

	(*p_startReqData) = (StartReqData*)malloc(sizeof(StartReqData));

    if ((*p_startReqData) == NULL)
    {
        PRINT_TAG(" fillStartRequestData: NULL Pointers \n");
        return SUPLC_ERROR_NULL_PTRS;
    }

    *p_outAppId = SUPL_APP_ID;          /* TBD: Check as 2 y dis data is hardcoded. */

    (*p_startReqData)->start_opt_param = 0;

	if(NULL!=p_suplCntl)
	{
		/* Set the preferred mode based on the FW request */
	    if (p_suplCntl->u8_fixMode == SUPLC_FIXMODE_MSBASED)
	        /* MS-Based SUPL AGPS. */
	        (*p_startReqData)->set.pref_method = AGPS_SET_BASED;

	    else if(p_suplCntl->u8_fixMode == SUPLC_FIXMODE_MSASSISTED)
	        /* MS-Assisted SUPL AGPS. */
	        (*p_startReqData)->set.pref_method = AGPS_SET_ASSISTED;

		else if(p_suplCntl->u8_fixMode == SUPLC_FIXMODE_AUTONOMOUS)
			/* Autonomous SUPL AGPS. */
			(*p_startReqData)->set.pref_method = AGPS_NO_PREF;

	    else
	    {
	        PRINT_TAG(" fillStartRequestData: Non GPS Fix mode \n");
			(*p_startReqData)->set.pref_method = AGPS_NO_PREF;
	        //return SUPLC_ERROR_INVALID_FIXMODE;
	    }
	}
	else
	{
		PRINT_TAG(" fillStartRequestData: SUPL Controller not initialized \n");
	}
	/* Set the SET Capabilities Bit Map */
//(*p_startReqData)->set.pos_technology_bitmap = SET_ASSISTED_AGPS_OFFSET|E_CID_OFFSET|SET_BASED_AGPS_OFFSET|AUTONOMUS_GPS_OFFSET;
(*p_startReqData)->set.pos_technology_bitmap = SET_ASSISTED_AGPS_OFFSET | SET_BASED_AGPS_OFFSET | AUTONOMUS_GPS_OFFSET;
     (*p_startReqData)->start_opt_param |= QOP;
    /* Fill Quality Of Position. Hardcoding has to be removed. */
#if 1
/* ++KlockWork Changes */
		if(NULL!=p_assistReqData)
		{
			(*p_startReqData)->qop.horr_acc = p_assistReqData->qop.horr_acc;
			(*p_startReqData)->qop.qop_optional_bitmap = p_assistReqData->qop.qop_optional_bitmap;
			/* ++KlockWork Changes - related to startSuplSession, passing NULL as param earlier */
			if(p_assistReqData->qop.qop_optional_bitmap & QOP_VER_ACC)
			{
				(*p_startReqData)->qop.ver_acc = p_assistReqData->qop.ver_acc;
			}
			if(p_assistReqData->qop.qop_optional_bitmap & QOP_MAX_LOC_AGE)
			{
				(*p_startReqData)->qop.max_loc_age = p_assistReqData->qop.max_loc_age;
			}
			if(p_assistReqData->qop.qop_optional_bitmap & QOP_DELAY)
			{
				(*p_startReqData)->qop.delay = p_assistReqData->qop.delay;
			}
			/* --KlockWork Changes - */
		}
/* --KlockWork Changes */
#endif



    /* Set Position Protocol Type. TIA_801_MASK or RRLP_MASK or RRC_MASK*/
    (*p_startReqData)->set.pos_protocol_bitmap = RRLP_MASK | RRC_MASK;

    /** Fill location id.
      * TBD: Modem support is required for this. Use MCPF's modem wrappers to fill this data. */
#if defined(DOCOMO_SUPPORT_WCDMA)
	(*p_startReqData)->lac.cell_info.cell_type = WCDMA;
	printf("\n\n----USING WCDMA----\n\n");
	if ( 1 != SUPLC_getWcdmaCellInfo(&(*p_startReqData)->lac.cell_info.cell_info_wcdma))
    {
        PRINT_TAG(" fillStartRequestData: SUPLC_getGsmCellInfo FAILED !!! \n");
		/* KlockWork Changes */
		if ((*p_startReqData) != NULL)
		{
			free((*p_startReqData));
			*p_startReqData = NULL;
		}
        return SUPLC_ERROR_UNKNOWN;
    }
	(*p_startReqData)->lac.cell_info.cell_info_wcdma.frequencyInfo = NULL;
	(*p_startReqData)->lac.cell_info.cell_info_wcdma.primaryScramblingCode = NULL;
	(*p_startReqData)->lac.cell_info.cell_info_wcdma.measuredResultsList = NULL;
	(*p_startReqData)->lac.cell_info_status = PRESENT_CELL_INFO;
	(*p_startReqData)->lac.cell_info.cell_info_wcdma.wcdma_opt_param = 0;
#else
    //(*p_startReqData)->lac.cell_info.cell_type = GSM;
    /* Get GSM Cell Information. */
    if ( 1 != SUPLC_getGsmCellInfo(&(*p_startReqData)->lac.cell_info))
    {
        PRINT_TAG(" fillStartRequestData: SUPLC_getGsmCellInfo FAILED !!! \n");
		/* KlockWork Changes */
		if ((*p_startReqData) != NULL)
		{
			free((*p_startReqData));
			*p_startReqData = NULL;
		}
        return SUPLC_ERROR_UNKNOWN;
    }

    (*p_startReqData)->lac.cell_info.cell_info_gsm.gsm_ta=10;
    (*p_startReqData)->lac.cell_info_status = PRESENT_CELL_INFO;
    (*p_startReqData)->lac.cell_info.cell_info_gsm.gsm_opt_param = 0xff;

#endif

    /**/

if(NULL!=p_assistReqData)
{
    /** Fill requested assistance data. (Optional)
      * Ask for mandatory & optional assistance as requested by NAVC. */
	if(p_assistReqData->position.pos_opt_bitmap)
	{
		memset((*p_startReqData)->position.UTCTimeStamp, 0x00, 20);
		(*p_startReqData)->position.UTCTimeStampNumByte = p_assistReqData->position.UTCTimeStampNumByte;
	    memcpy((*p_startReqData)->position.UTCTimeStamp, p_assistReqData->position.UTCTimeStamp, p_assistReqData->position.UTCTimeStampNumByte);
	    (*p_startReqData)->position.latitude_sign = p_assistReqData->position.latitude_sign;
	    (*p_startReqData)->position.pos_opt_bitmap = 0x03; //Altitude and Uncertainity
	    (*p_startReqData)->position.altitudeDirection = p_assistReqData->position.altitudeDirection;
	    (*p_startReqData)->position.altUncertainty = p_assistReqData->position.altUncertainty;
	    (*p_startReqData)->position.uncertaintySemiMajor = p_assistReqData->position.uncertaintySemiMajor;
	    (*p_startReqData)->position.uncertaintySemiMinor = p_assistReqData->position.uncertaintySemiMinor;
	    (*p_startReqData)->position.orientationMajorAxis = p_assistReqData->position.orientationMajorAxis;
	    (*p_startReqData)->position.confidence = p_assistReqData->position.confidence;
	    (*p_startReqData)->position.latitude = p_assistReqData->position.latitude;
	    (*p_startReqData)->position.longtitude = p_assistReqData->position.longtitude;
	    (*p_startReqData)->position.altitude = p_assistReqData->position.altitude;

		(*p_startReqData)->start_opt_param |= POSITION;

		PRINT_TAG("\np_assistReqData->position.latitude = %d",p_assistReqData->position.latitude);
		PRINT_TAG("\n(*p_startReqData)->position.longtitude = %d",(*p_startReqData)->position.longtitude);
		PRINT_TAG("\n(*p_startReqData)->position.altitude = %d",(*p_startReqData)->position.altitude);
		PRINT_TAG("\n(*p_startReqData)->position.uncertaintySemiMajor = %d",(*p_startReqData)->position.uncertaintySemiMajor);
		PRINT_TAG("\n(*p_startReqData)->position.uncertaintySemiMinor = %d",(*p_startReqData)->position.uncertaintySemiMinor);
		PRINT_TAG("\n(*p_startReqData)->position.orientationMajorAxis = %d",(*p_startReqData)->position.orientationMajorAxis);
		PRINT_TAG("\n(*p_startReqData)->position.UTCTimeStampNumByte = %d",(*p_startReqData)->position.UTCTimeStampNumByte);
		PRINT_TAG("\n(*p_startReqData)->position.pos_opt_bitmap= %d",(*p_startReqData)->position.pos_opt_bitmap);
		LOGD("(*p_startReqData)->position.UTCTimeStamp : %c%c%c%c%c%c",(*p_startReqData)->position.UTCTimeStamp[7],(*p_startReqData)->position.UTCTimeStamp[8],(*p_startReqData)->position.UTCTimeStamp[9],(*p_startReqData)->position.UTCTimeStamp[10],(*p_startReqData)->position.UTCTimeStamp[11],(*p_startReqData)->position.UTCTimeStamp[12]);
	}
/*We do Not ask Any assistance from SLP if we're sunning in Autonomous mode*/
		if(NULL!=p_suplCntl)
		{

			if(p_suplCntl->u8_fixMode != SUPLC_FIXMODE_AUTONOMOUS)
			{

				/* Navc Bitmap is not supl compliance, so bit by bit masking is done */
				/* Assistance data Request for MSA and MSB is handled */

				(*p_startReqData)->a_data.assitance_req_bitmap =0;

				if(((p_assistReqData->uAssistBitmapMandatory)&GPSC_REQ_ALMANAC))
				{
					DEBUG_SUPLC(PRINT_TAG(" fillStartRequestData: Requesting ALAMANAC \n"));
					(*p_startReqData)->a_data.assitance_req_bitmap|=ALMANAC_REQ_MASK;
				}

				if((p_assistReqData->uAssistBitmapMandatory)&GPSC_REQ_UTC)
				{
					DEBUG_SUPLC(PRINT_TAG(" fillStartRequestData: Requesting UTC \n"));
					(*p_startReqData)->a_data.assitance_req_bitmap|=UTC_MODE_REQ_MASK;
				}

				if((p_assistReqData->uAssistBitmapMandatory)&GPSC_REQ_IONO)
				{
					DEBUG_SUPLC(PRINT_TAG(" fillStartRequestData: Requesting IONO \n"));
					(*p_startReqData)->a_data.assitance_req_bitmap|=IONOSPHERIC_MODE_REQ_MASK;
				}

				if(((p_assistReqData->uAssistBitmapMandatory)&GPSC_REQ_NAV))
				{
					DEBUG_SUPLC(PRINT_TAG(" fillStartRequestData: Requesting Navigation Model \n"));
					McpU16 sat_count=0;
					(*p_startReqData)->a_data.assitance_req_bitmap|=NAVIGATION_MODEL_REQ_MASK;
					//Navigation model
					(*p_startReqData)->a_data.num_sat_info=p_assistReqData->tNavModelReqParams.nav_num_svs;
					(*p_startReqData)->a_data.gpsWeak = p_assistReqData->tNavModelReqParams.gps_week;
				    (*p_startReqData)->a_data.gpsToe = p_assistReqData->tNavModelReqParams.nav_toe;
				    (*p_startReqData)->a_data.nSAT = p_assistReqData->tNavModelReqParams.nav_num_svs;
				    (*p_startReqData)->a_data.toeLimit = p_assistReqData->tNavModelReqParams.nav_toe_limit;
					for(sat_count=0;sat_count<p_assistReqData->tNavModelReqParams.nav_num_svs;sat_count++)
					{
						(*p_startReqData)->a_data.sat_info_elemnet[sat_count].satelliteID=p_assistReqData->tNavModelReqParams.nav_data[sat_count].svid;
						(*p_startReqData)->a_data.sat_info_elemnet[sat_count].iode=p_assistReqData->tNavModelReqParams.nav_data[sat_count].iode;
					}
				}

				if(((p_assistReqData->uAssistBitmapMandatory)&GPSC_REQ_DGPS))
				{
					DEBUG_SUPLC(PRINT_TAG(" fillStartRequestData: Requesting IONO \n"));
					(*p_startReqData)->a_data.assitance_req_bitmap|=DGPS_CORRECTIONS_REQ_MASK;
				}

				if(((p_assistReqData->uAssistBitmapMandatory)&GPSC_REQ_LOC))
				{
					DEBUG_SUPLC(PRINT_TAG(" fillStartRequestData: Requesting REF LOC \n"));
					(*p_startReqData)->a_data.assitance_req_bitmap|=REFERENCE_LOCATION_REQ_MASK;
				}

				if((p_assistReqData->uAssistBitmapMandatory)&GPSC_REQ_TIME)
				{
					DEBUG_SUPLC(PRINT_TAG(" fillStartRequestData: Requesting REF TIME \n"));
					(*p_startReqData)->a_data.assitance_req_bitmap|=REFERENCE_TIME_REQ_MASK;
				}

				if((p_assistReqData->uAssistBitmapMandatory)&GPSC_REQ_AA)
				{
					DEBUG_SUPLC(PRINT_TAG(" fillStartRequestData: Requesting AA \n"));
					(*p_startReqData)->a_data.assitance_req_bitmap|=ACQUI_ASSIST_REQ_MASK;
				}

				if((p_assistReqData->uAssistBitmapMandatory)&GPSC_REQ_RTI)
				{
					DEBUG_SUPLC(PRINT_TAG(" fillStartRequestData: Requesting RTI \n"));
					(*p_startReqData)->a_data.assitance_req_bitmap|=REAL_TIME_INT_REQ_MASK;
				}

				if((*p_startReqData)->a_data.assitance_req_bitmap!=0)
				{
					(*p_startReqData)->start_opt_param |= REQUEST_ASSISTANCE;
					DEBUG_SUPLC(PRINT_TAG(" fillStartRequestData: Assistance data Requested \n"));
				}
				else
				{
					DEBUG_SUPLC(PRINT_TAG(" fillStartRequestData: Assistance data Not Requested \n"));
				}

			}
			else
			{
				/* set assistance request bitmap to 0 */
				(*p_startReqData)->a_data.assitance_req_bitmap =0;
				DEBUG_SUPLC(PRINT_TAG(" fillStartRequestData: Assistance data Not Requested in Autmonomous case\n"));
			}

		}
}

else
{
	DEBUG_SUPLC(PRINT_TAG(" fillStartRequestData: Assistance data Not Requested in Non Gps case\n"));
}

DEBUG_SUPLC(PRINT_TAG(" fillStartRequestData: start_opt_param = %d, REQUEST_ASSISTANCE = %d \n",
								(*p_startReqData)->start_opt_param, REQUEST_ASSISTANCE));

DEBUG_SUPLC(PRINT_TAG(" fillStartRequestData: Exiting Successfully \n"));
return retVal; /* KlockWork Change */


}

/**
 * Function:        fillDataReqData
 * Brief:           Fills DataReqData Structure .
 * Description:
 * Note:
 * Params:          p_dataReqData - Pointer to DataReqData structure.
                    p_appId - App Id.
                    p_inData - Data
                    p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result fillDataReqData(DataReqData **p_dataReqData,
                                     McpU16 *p_appId,
                                     const void *const p_inData,
                                     const void *const p_inSuplCntl)
{
    TNAVC_locFixReport *p_locFixResponse = NULL;
    TSUPL_Ctrl *p_suplCntl = NULL;
    eSUPLC_Result retVal = SUPLC_SUCCESS;

    DEBUG_SUPLC(PRINT_TAG(" fillDataReqData: Entering \n"));

    p_locFixResponse = (TNAVC_locFixReport *)p_inData;
    p_suplCntl = (TSUPL_Ctrl *)p_inSuplCntl;

    /* Just in case... */
    if ( (p_locFixResponse == NULL) ||
         (p_suplCntl == NULL) )
    {
        PRINT_TAG(" fillDataReqData: NULL Pointers \n");
        return SUPLC_ERROR_NULL_PTRS;
    }

    /** Temporary Hack: Has to be removed.
      * processResponseMsg needs this information. */
    gp_suplCntl = p_suplCntl;

  (*p_dataReqData) = (DataReqData *)malloc(sizeof(DataReqData));
    if ((*p_dataReqData) == NULL)
    {
        PRINT_TAG(" fillDataReqData: NULL Pointers \n");
        return SUPLC_ERROR_NULL_PTRS;
    }

    *p_appId = SUPL_APP_ID;          /* TBD: Check as 2 y dis is hardcoded. */
    (*p_dataReqData)->app_id = *p_appId;

    /* Fill position payload type. */
    switch(p_suplCntl->e_protocol)
    {
        case SUPLC_PROT_RRLP:
        {
            (*p_dataReqData)->pos_payload.type = RRLP_PAYLOAD;
        }
        break;

        case SUPLC_PROT_RRC:
        {
            (*p_dataReqData)->pos_payload.type = RRC_PAYLOAD;
        }
        break;

        default:
        {
            PRINT_TAG(" fillDataReqData: Positioning protocol not supported \n");
			/* KlockWork Changes */
			if ((*p_dataReqData) != NULL)
			{
				free((*p_dataReqData));
				*p_dataReqData = NULL;
			}
            return SUPLC_ERROR_INVALID_POS_PROT;
        }
    }

    /* Build RRLP/RRC Response. */
    retVal = SUPLC_getPositionPayload((*p_dataReqData)->pos_payload.payload.ctrl_pdu,
                                       &(*p_dataReqData)->pos_payload.payload.ctrl_pdu_len,
                                       p_locFixResponse, p_suplCntl);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" fillDataReqData: Positioning protocol filling FAILED !!! \n");
		/* KlockWork Changes */
		if ((*p_dataReqData) != NULL)
		{
			free((*p_dataReqData));
			*p_dataReqData = NULL;
		}

        return SUPLC_ERROR_DATA_FILLING;
    }

    DEBUG_SUPLC(PRINT_TAG(" fillDataReqData: Exiting Successfully \n"));
    return retVal;
}
/**
 *  * Function:        fillDataAck
 *   * Brief:           Fills DataReqData Structure .
 *    * Description:
 *     * Note:
 *      * Params:          p_dataReqData - Pointer to DataReqData structure.
 *                          p_appId - App Id.
 *                                              p_inData - Data
 *                                                                  p_inSuplCntl - Pointer to SUPLC's Control Structure.
 *                                                                   * Return:          Success: SUPLC_SUCCESS.
 *                                                                                       Failure: SUPLC_ERROR_XXX.
 *                                                                                        */
static eSUPLC_Result fillDataAck(DataReqData **p_dataReqData,
                                     McpU16 *p_appId,
                                     const void *const p_inData,
                                     const void *const p_inSuplCntl)
{
    TNAVC_rrFrame	*p_rrframe = NULL;
    TSUPL_Ctrl *p_suplCntl = NULL;
    eSUPLC_Result retVal = SUPLC_SUCCESS;

    DEBUG_SUPLC(PRINT_TAG(" fillDataReqData: Entering \n"));

    p_rrframe = (TNAVC_rrFrame	*)p_inData;
    p_suplCntl = (TSUPL_Ctrl *)p_inSuplCntl;

    /* Just in case... */
    if ( (p_rrframe == NULL) ||
         (p_suplCntl == NULL) )
    {
        PRINT_TAG(" fillDataAck: NULL Pointers \n");
        return SUPLC_ERROR_NULL_PTRS;
    }

    /** Temporary Hack: Has to be removed.
 *       * processResponseMsg needs this information. */
    gp_suplCntl = p_suplCntl;


	(*p_dataReqData) = (DataReqData *)malloc(sizeof(DataReqData));
    if ((*p_dataReqData) == NULL)
    {
        PRINT_TAG(" fillDataReqData: NULL Pointers \n");
        return SUPLC_ERROR_NULL_PTRS;
    }

    *p_appId = SUPL_APP_ID;          /* TBD: Check as 2 y dis is hardcoded. */
    (*p_dataReqData)->app_id = *p_appId;

	mcpf_mem_copy(p_suplCntl->hMcpf, (*p_dataReqData)->pos_payload.payload.ctrl_pdu,
					  p_rrframe->uRRpayload, p_rrframe->uRrFrameLength);

    (*p_dataReqData)->pos_payload.payload.ctrl_pdu_len = p_rrframe->uRrFrameLength;
	if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" fillDataReqData: Positioning protocol filling FAILED !!! \n");
        return SUPLC_ERROR_DATA_FILLING;
    }

    DEBUG_SUPLC(PRINT_TAG(" fillDataReqData: Exiting Successfully \n"));
    return retVal;
}


/* Function:        fillDataReqDataForPosResp
 * Brief:           Fills DataReqData Structure .
 * Description:
 * Note:
 * Params:          p_dataReqData - Pointer to DataReqData structure.
                    p_appId - App Id.
                    p_inData - Data
                    p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result fillDataReqDataForPosResp(DataReqData **p_dataReqData,
                                               McpU16 *p_appId,
                                               const void *const p_inData,
                                               const void *const p_inSuplCntl)
{
    TNAVC_rrFrame *p_rrFrame = NULL;
    TSUPL_Ctrl *p_suplCntl = NULL;

    eSUPLC_Result retVal = SUPLC_SUCCESS;

    DEBUG_SUPLC(PRINT_TAG(" fillDataReqDataForPosResp: Entering \n"));

    p_rrFrame = (TNAVC_rrFrame *)p_inData;
    p_suplCntl = (TSUPL_Ctrl *)p_inSuplCntl;

    /* Just in case... */
    if ( (p_rrFrame == NULL) ||
         (p_suplCntl == NULL) )
    {
        PRINT_TAG(" fillDataReqDataForPosResp: NULL Pointers \n");
        return SUPLC_ERROR_NULL_PTRS;
    }

    /** Temporary Hack: Has to be removed.
      * processResponseMsg needs this information. */
    gp_suplCntl = p_suplCntl;


	(*p_dataReqData) = (DataReqData *)malloc(sizeof(DataReqData));

    if ( (*p_dataReqData) == NULL)
    {
        PRINT_TAG(" fillDataReqData: NULL Pointers \n");
        return SUPLC_ERROR_NULL_PTRS;
    }

    *p_appId = SUPL_APP_ID;          /* TBD: Check as 2 y dis is hardcoded. */

    (*p_dataReqData)->app_id = *p_appId;

    /* Fill position payload type. */
    switch(p_suplCntl->e_protocol)
    {
        case SUPLC_PROT_RRLP:
        {
            (*p_dataReqData)->pos_payload.type = RRLP_PAYLOAD;
        }
        break;

        case SUPLC_PROT_RRC:
        {
            (*p_dataReqData)->pos_payload.type = RRC_PAYLOAD;
        }
        break;

        default:
        {
            PRINT_TAG(" fillDataReqDataForPosResp: Positioning protocol not supported \n");
			/* KlockWork Changes */
			if ( (*p_dataReqData) != NULL)
			{
				free((*p_dataReqData));
				*p_dataReqData = NULL;
			}
            return SUPLC_ERROR_INVALID_POS_PROT;
        }
    }

    /** RRLP/RRC library has constructed appropriate packets.
      * Just copy into DataReqData structure.
      * TBD: Check the logic... */
    mcpf_mem_copy(p_suplCntl->hMcpf, (*p_dataReqData)->pos_payload.payload.ctrl_pdu,
                  p_rrFrame->uRRpayload, p_rrFrame->uRrFrameLength);

    (*p_dataReqData)->pos_payload.payload.ctrl_pdu_len = p_rrFrame->uRrFrameLength;


    DEBUG_SUPLC(PRINT_TAG(" fillDataReqDataForPosResp: Exiting Successfully \n"));
    return retVal;
}


/**
 * Function:        sendDataToCore
 * Brief:           Sends UPL commands to SUPLCore.
 * Description:
 * Note:
 * Params:          u16_appId - App Id.
                    p_data - Pointer to data that has to be sent to Core.
                    u32_command - Command to be sent
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result sendDataToCore(McpU16 u16_appId, McpU8* p_data, McpU32 u32_command)
{
    McpU8 retVal = 1;

    DEBUG_SUPLC(PRINT_TAG(" sendDataToCore: Entering \n"));
	PRINT_TAG("\nIn sendDataToCore, Address = %p\n",p_data);

	retVal = SUPLC_sendUplMsg(u16_appId, u32_command, p_data);
    if (retVal == 0)
    {
        PRINT_TAG(" sendDataToCore: SUPL_Control FAILED !!! \n");
        return SUPLC_ERROR_UNKNOWN;
    }

    DEBUG_SUPLC(PRINT_TAG(" sendDataToCore: Exiting Successfully \n"));
    return SUPLC_SUCCESS;
}

/**
 * Function:        processResponseMsg
 * Brief:           Processes the UPL response messages from SUPL Client.
 * Description:     It's a callback functions which is called by SUPL Client
                    whenever it has some data to be sent to NAVC.
 * Note:
 * Params:          appId: Application ID.
                    command: SUPL Controller Command.
                    inbuf: Pointer to command data.
 * Return:          Success: TRUE.
                    Failure: FALSE.
 */
static boolVal processResponseMsg(uInt16 appId,
                                  uInt32 Command,
                                  void *inbuf)
{
    boolVal retVal = TRUE;
    DEBUG_SUPLC(PRINT_TAG(" processResponseMsg: Entering \n"));

    /** TBD: Check for appId and return on ERROR.
      * Need to check this out. */
    if (SUPL_APP_ID == appId)
    {
        retVal = processResponseFromSuplClient(Command, inbuf);
        free(inbuf);
        inbuf = NULL;
        if (retVal != TRUE)
        {
            PRINT_TAG(" processResponseMsg: SUPL Client's Response Processing FAILED !!! \n");
            return FALSE;
        }
    }

    DEBUG_SUPLC(PRINT_TAG(" processResponseMsg: Exiting Successfully \n"));
    return retVal;
}

/**
 * Function:        processResponseFromSuplClient
 * Brief:           Processes the UPL response messages from SUPL Client.
 * Description:     It's a callback functions which is called by SUPL Client
                    whenever it has some data to be sent to NAVC.
 * Note:
 * Params:          command: SUPL Controller Command.
                    inbuf: Pointer to command data.
 * Return:          Success: TRUE.
                    Failure: FALSE.
 */
static boolVal processResponseFromSuplClient(uInt32 Command, void *inbuf)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;

    DEBUG_SUPLC(PRINT_TAG(" processResponseFromSuplClient: Entering \n"));

    /* Check for the command sent by SUPLCore. */
    switch(Command)
    {
        case UPL_START_REQ:
        {
			DEBUG_SUPLC(PRINT_TAG(" processResponseFromSuplClient: UPL_START_REQ (xxx NOT HANDLED)\n"));
        }
        break;
        case UPL_DATA_REQ:
        {
			LOGD(" processResponseFromSuplClient: UPL_DATA_REQ (xxx NOT HANDLED)\n");
        }
        break;
        case UPL_STOP_REQ:
        {
			DEBUG_SUPLC(PRINT_TAG(" processResponseFromSuplClient: UPL_STOP_REQ (xxx NOT HANDLED)\n"));
        }
        break;
        case UPL_NOTIFY_RESP:
        {
			DEBUG_SUPLC(PRINT_TAG(" processResponseFromSuplClient: UPL_NOTIFY_RESP (xxx NOT HANDLED)\n"));
        }
        break;
        case UPL_DATA_IND:
        {
            /** SUPLCore sends this command for sending assistance data to NAVC.
              * The received data (RRLP/RRC) is processed here and the relevent information
              * is sent to NAVC. */
            retVal = processAssistanceData(inbuf);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" processResponseFromSuplClient: Assistance Data Processing FAILED !!! \n");
                return FALSE;
            }
        }
        break;

        case UPL_POS_IND:
		{
				gp_suplCntl->bLpPosReq=FALSE;
			retVal = getPositionFix(inbuf);
			if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" processResponseFromSuplClient: Net Init PASSED !!! \n");
                return FALSE;
            }
				retVal = SUPLC_sendSessResult(gp_suplCntl, inbuf);
				if (retVal != SUPLC_SUCCESS)
				{
					PRINT_TAG(" processResponseFromSuplClient: Net Stop FAILED !!! \n");
					return FALSE;
				}

                PRINT_TAG(" UPL_POS_IND: Position Sent to GPS Driver, Stopping it now \n");

			retVal = stopNetInitiatedFix(inbuf);
            if (retVal != SUPLC_SUCCESS)
            {
               PRINT_TAG(" processResponseFromSuplClient: Net Stop FAILED !!! \n");
               return FALSE;
            }
		    DEBUG_SUPLC(PRINT_TAG(" processResponseFromSuplClient: Session Terminated..Handle state \n"));
			handle_evSessionEnd(gp_suplCntl);
        }
		break;
        case UPL_STOP_IND:
        {
			DEBUG_SUPLC(PRINT_TAG(" processResponseFromSuplClient: UPL_STOP_IND (xxx CHECK IMPL)\n"));
            gp_suplCntl->bLpPosReq=FALSE;

		   retVal = SUPLC_sendSessResult(gp_suplCntl, inbuf);
           if (retVal != SUPLC_SUCCESS)
           {
               PRINT_TAG(" processResponseFromSuplClient: Net Stop FAILED !!! \n");
               return FALSE;
           }

           retVal = stopNetInitiatedFix(inbuf);
           if (retVal != SUPLC_SUCCESS)
           {
               PRINT_TAG(" processResponseFromSuplClient: Net Stop FAILED !!! \n");
               return FALSE;
           }
		   DEBUG_SUPLC(PRINT_TAG(" processResponseFromSuplClient: Session Terminated..Handle state \n"));
		   handle_evSessionEnd(gp_suplCntl);
        }
        break;
        case UPL_START_LOC_IND:
        {
            /* SUPLCore sends this command for Network Initiated SUPL aGPS request to NAVC. */
            retVal = startNetInitiatedFix(inbuf);
            if (retVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" processResponseFromSuplClient: Net Init FAILED !!! \n");
                return FALSE;
            }
        }
        break;


        case UPL_NOTIFY_IND:
        {
        }
        break;

        default:
        {
        }
        break;
    }

    DEBUG_SUPLC(PRINT_TAG(" processResponseFromSuplClient: Exiting Successfully \n"));
    return TRUE;
}

/**
 * Function:        processResponseFromEeClient
 * Brief:           Processes the UPL response messages from EE Client.
 * Description:     It's a callback functions which is called by EE Client
                    whenever it has some data to be sent to NAVC.
 * Note:
 * Params:          command: SUPL Controller Command.
                    inbuf: Pointer to command data.
 * Return:          Success: TRUE.
                    Failure: FALSE.
 */
static boolVal processResponseFromEeClient(uInt16 appId,
                                           uInt32 Command,
                                           void *inbuf)
{
#if 0
    boolVal retVal = TRUE;

    DEBUG_SUPLC(PRINT_TAG(" processResponseFromEeClient: Entering \n"));
    retVal = SUPLC_eeCallback(appId, Command, inbuf);

    DEBUG_SUPLC(PRINT_TAG(" processResponseFromEeClient: Exiting Successfully. \n"));
    return retVal;
#endif

    eSUPLC_Result suplRetVal = SUPLC_SUCCESS;
    boolVal retVal = TRUE;

    DEBUG_SUPLC(PRINT_TAG(" processResponseFromEeClient: Entering \n"));

    /* Check for the command sent by SUPLCore. */
    switch(Command)
    {
        case UPL_DATA_IND:
        {
            DataIndData* dta = (DataIndData *) inbuf;

            RRLPbuffersize=dta->pos_payload.payload.ctrl_pdu_len;
            if(NULL != p_RRLPbuffer )
            {
                PRINT_TAG("processResponseFromEeClient: Previous RRLP Packet not cleared. \n");
            }

            p_RRLPbuffer = (char *)malloc(RRLPbuffersize * sizeof(char));
            if (p_RRLPbuffer==NULL)
            {
                PRINT_TAG("processResponseFromEeClient: malloc failed !!! \n");
                return FALSE;
            }
            memcpy(p_RRLPbuffer, dta->pos_payload.payload.ctrl_pdu, RRLPbuffersize);
            RRLPPacketAvailable = 1;

#if 1
            /* Release semaphore for SUPL_Core to process this message. */
            DEBUG_SUPLC(PRINT_TAG(" processResponseFromEeClient: Calling SUPLC_releaseSemaphore \n"));
            if ( SUPLC_releaseSemaphore(gp_rrlpPkt) < 0 )
            {
                  PRINT_TAG("processResponseFromEeClient: sem_post FAILED !!! \n");
                  retVal = FALSE;
                  break;
            }
#endif

            /* Trigger appropriate event. SUPLC's state machine processes it. */
            suplRetVal = SUPLC_triggerEvent(evAssistInd, NULL, NULL);
            if (suplRetVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" processResponseFromEeClient: Event triggering FAILED !!! \n");
                PRINT_TAG(" REASON: %d \n", suplRetVal);
                retVal = FALSE;
            }
        }
        break;

        case UPL_STOP_IND:
        {
            /* Trigger appropriate event. SUPLC's state machine processes it. */
            suplRetVal = SUPLC_triggerEvent(evAssistInd, NULL, NULL);
            if (suplRetVal != SUPLC_SUCCESS)
            {
                PRINT_TAG(" processResponseFromEeClient: Event triggering FAILED !!! \n");
                PRINT_TAG(" REASON: %d \n", suplRetVal);
                retVal = FALSE;
            }

        }
        break;

        default:
        break;
    }

    DEBUG_SUPLC(PRINT_TAG(" processResponseFromEeClient: Exiting Successfully. \n"));
    return retVal;
}



/**
 * Function:       nalExecuteCommand
 * Brief:
 * Description:
 * Note:
 * Params:
 * Return:          Success: TRUE.
                    Failure: FALSE.
 */

static pthread_mutex_t g_nalExecuteCommand             = PTHREAD_MUTEX_INITIALIZER;

static eNAL_Result nalExecuteCommand(const eNetCommands e_netCommand, void *p_inBuf)
{
    EMcpfRes retVal = RES_COMPLETE;
	pthread_mutex_lock( &g_nalExecuteCommand );
    DEBUG_SUPLC(PRINT_TAG(" nalExecuteCommand: Entering \n"));

	/* KlockWork Changes */
	/*
	if (NULL == p_inBuf )
    {
        DEBUG_SUPLC(PRINT_TAG(" nalExecuteCommand: p_inBuf NULL !!! \n"));
    }
    */

    retVal = NAL_executeCommand(e_netCommand, p_inBuf);

    if (retVal != RES_COMPLETE)
    {
        PRINT_TAG(" nalExecuteCommand: NAL_executeCommand returned 1\n");
   		pthread_mutex_unlock(&g_nalExecuteCommand);
		return NAL_FALSE;
    }

    DEBUG_SUPLC(PRINT_TAG(" nalExecuteCommand: Exiting Successfully \n"));
   	pthread_mutex_unlock(&g_nalExecuteCommand);
	return NAL_TRUE;
}


/**
 * Function:        processAssistanceData
 * Brief:           Processes UPL Commands from SUPLCore and send appropriate
                    requests to NAVC.
 * Description:
 * Note:
 * Params:          p_inBufHandle - Contains RRLP/RRC data from SUPLCore.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result processAssistanceData(void *p_inBufHandle)
{
    DataIndData *p_dataIndData = NULL;
    eSUPLC_Result retVal = SUPLC_SUCCESS;

    DEBUG_SUPLC(PRINT_TAG(" processAssistanceData: Entering \n"));

    p_dataIndData = (DataIndData *)p_inBufHandle;

    /* Just in case... */
    if (p_dataIndData == NULL)
    {
        PRINT_TAG(" processAssistanceData: NULL Pointers \n");
        return SUPLC_ERROR_NULL_PTRS;
    }

	if(p_dataIndData->pos_payload.type == RRC_PAYLOAD)
	gp_suplCntl->e_protocol = SUPLC_PROT_RRC;

	else if(p_dataIndData->pos_payload.type == RRLP_PAYLOAD)
	gp_suplCntl->e_protocol = SUPLC_PROT_RRLP;

	else if(p_dataIndData->pos_payload.type == TIA_801_PAYLOAD)
	gp_suplCntl->e_protocol = TIA_801_PAYLOAD;

	else
	gp_suplCntl->e_protocol = SUPLC_PROT_NONE;

    /** This data has to be sent to NAVC.
      * gp_suplCntl was added on temporary basis. Has to be removed. */
    if(IsSuplcStopInProgress()==MCP_FALSE)
    {
    retVal = SUPLC_assistanceDataInd(gp_suplCntl,
                                     p_dataIndData->pos_payload.payload.ctrl_pdu,
                                     p_dataIndData->pos_payload.payload.ctrl_pdu_len);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" processAssistanceData: Assistance Data Indication FAILED !!! \n");
        return retVal;
    }

    }
    else
    {
        DEBUG_SUPLC(PRINT_TAG(" processAssistanceData: Suplc Stop In Progress, assistance data ignored \n"));
    }

    DEBUG_SUPLC(PRINT_TAG(" processAssistanceData: Exiting Successfully \n"));
    return SUPLC_SUCCESS;
}



/**
 * Function:        processAssistanceData
 * Brief:           Processes UPL Commands from SUPLCore and send appropriate
                    requests to NAVC.
 * Description:
 * Note:
 * Params:          p_inBufHandle - Contains StartLocIndData.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result startNetInitiatedFix(void *p_inBufHandle)
{
    StartLocIndData *p_startLocIndData = NULL;
    int NavcState = 0;
    T_GPSC_loc_fix_mode e_gpsMode = GPSC_FIXMODE_MSBASED;
	T_GPSC_loc_fix_qop e_qop;
	e_qop.qop_flag=0;
    eSUPLC_Result retVal = SUPLC_SUCCESS;

    DEBUG_SUPLC(PRINT_TAG(" startNetInitiatedFix: Entering \n"));

    p_startLocIndData = (StartLocIndData *)p_inBufHandle;

    /* Just in case... */
    if (p_startLocIndData == NULL)
    {
        PRINT_TAG(" startNetInitiatedFix: NULL Pointers \n");
        return SUPLC_ERROR_NULL_PTRS;
    }

    if (p_startLocIndData->resp_opt_bitmap & LOC_FIX_MODE)
    {
        /* Store the location fix mode. */
        e_gpsMode = p_startLocIndData->loc_fix_mode;
		if(e_gpsMode==GPSC_FIXMODE_AUTONOMOUS)
		{
			e_gpsMode=GPSC_FIXMODE_AUTOSUPL;
			gp_suplCntl->u8_fixMode = SUPLC_FIXMODE_AUTONOMOUS;
		}
			
		DEBUG_SUPLC(PRINT_TAG(" startNetInitiatedFix: Positioning mode is %d\n",p_startLocIndData->loc_fix_mode));
		if(p_startLocIndData->loc_fix_mode == 0x04)//ECID case
		{
			DEBUG_SUPLC(PRINT_TAG(" startNetInitiatedFix: ECID Mode - Starting SUPL Session\n"));
			/* ++KlockWork Changes - related to startSuplSession, passing NULL as param earlier */
			TNAVC_assistReq assReq;
			memset(&assReq,0,sizeof(TNAVC_assistReq));
			assReq.qop.horr_acc=127; //setting high value of Horizontal accuracy in CID case
			assReq.qop.qop_optional_bitmap=0; //Qop Opt parameter not needed.
			retVal = startSuplSession(&assReq,gp_suplCntl);
			/* --KlockWork Changes - related to startSuplSession, passing NULL as param earlier */
			return retVal;
		}
    }


	e_qop.max_response_time=90;
    if (p_startLocIndData->resp_opt_bitmap & START_LOC_QOP_BIT)
    {
        /* Store the location fix mode. */
        e_qop.horizontal_accuracy=p_startLocIndData->qop.horr_acc;
        if(p_startLocIndData->qop.qop_optional_bitmap)
        {
            if(p_startLocIndData->qop.qop_optional_bitmap & QOP_DELAY)
                e_qop.delay_n=p_startLocIndData->qop.delay;
            else
                e_qop.delay_n=0xFF;
            if(p_startLocIndData->qop.qop_optional_bitmap & QOP_MAX_LOC_AGE)
                e_qop.max_loc_age=(McpU8)p_startLocIndData->qop.max_loc_age;
            else
                e_qop.max_loc_age=0xFF;
            if(p_startLocIndData->qop.qop_optional_bitmap & QOP_VER_ACC)
                e_qop.vertical_accuracy=p_startLocIndData->qop.ver_acc;
            else
                e_qop.vertical_accuracy=0xFFFF;

            e_qop.qop_flag=0x01;
        }
        else
            {
                e_qop.delay_n=0xFF;
                e_qop.max_loc_age=0xFF;
                e_qop.vertical_accuracy=0xFFFF;
				e_qop.qop_flag=0x01;
            }

    }
	else
		{
			e_qop.delay_n=0xFF;
			e_qop.max_loc_age=0;
			e_qop.vertical_accuracy=0xFFFF;
			e_qop.horizontal_accuracy=100;
			e_qop.qop_flag=0x01;

		}


    PRINT_TAG("\n(p_startLocIndData)->e_qop.horr_acc = %d",e_qop.horizontal_accuracy);
    PRINT_TAG("\n(p_startLocIndData)->e_qop.delay = %d",e_qop.delay_n);
    PRINT_TAG("\n(p_startLocIndData)->e_qop.max_loc_age = %d",e_qop.max_loc_age);
    PRINT_TAG("\n(p_startLocIndData)->e_qop.ver_acc = %d",e_qop.vertical_accuracy);
	PRINT_TAG("\n(p_startLocIndData)->e_qop.max_response_time = %d",e_qop.max_response_time);


	gp_suplCntl->e_sessFlow=SUPLC_FLOW_NI;
	retVal = SUPLC_sendLocationFix(gp_suplCntl,e_gpsMode,&e_qop);
	if (retVal != SUPLC_SUCCESS)
	{
		PRINT_TAG(" processEvtCmdComplete: Sending NAVC_CMD_REQUEST_FIX FAILED !!! \n");
		return retVal;
	}

    DEBUG_SUPLC(PRINT_TAG(" startNetInitiatedFix: Exiting Successfully \n"));
    return retVal;
}
/**
 * Function:        startNetInitiatedFix
 * Brief:           Processes UPL Commands from SUPLCore and send appropriate
                    requests to NAVC.
 * Description:
 * Note:
 * Params:          p_inBufHandle - Contains StartLocIndData.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
 static eSUPLC_Result getPositionFix(void *p_inBufHandle)
{
   TNAVC_posIndData *p_posIndData =  NULL;
   eSUPLC_Result retVal = SUPLC_SUCCESS;

   p_posIndData = (TNAVC_posIndData*)p_inBufHandle;
   if (p_posIndData == NULL || gp_suplCntl == NULL)
	   {
		   return SUPLC_ERROR_NULL_PTRS;
	   }
        retVal = SUPLC_sendPosInd(gp_suplCntl, p_posIndData, (const McpU16)sizeof(TNAVC_posIndData)); /* KlockWork Changes - Typecast */
	if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" getPositionFix: Navc Start FAILED !!! \n");
        return retVal;
    }
    DEBUG_SUPLC(PRINT_TAG(" getPositionFix: Exiting Successfully \n"));
   return retVal;
}
 /* Added for NI */
static eSUPLC_Result stopNetInitiatedFix(void *p_inBufHandle)
{

	if(gp_suplCntl->e_sessFlow==SUPLC_FLOW_NI)
	{
		TNAVC_cmdParams *p_navcCmdParams;
	    eSUPLC_Result retVal = SUPLC_SUCCESS;
		TNAVC_stopLocFix *pStopLocFix = NULL;
	    DEBUG_SUPLC(PRINT_TAG(" stopNetInitiatedFix: Entering \n"));
		p_navcCmdParams = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool(gp_suplCntl->hMcpf,
	                                                                 gp_suplCntl->mMemPool);

	       //klockworks
	        if (p_navcCmdParams == NULL )
	         {
	          LOGD("stopNetInitiatedFix: Malloc failed \n  ");
	          return SUPLC_ERROR_NULL_PTRS;
	          }
		pStopLocFix = (TNAVC_stopLocFix *)p_navcCmdParams;

		pStopLocFix->uSessionId = AUTONOMOUS_SESSION;


	    /* Send command to start NAVC. */
	    retVal = SUPLC_stopNavc(gp_suplCntl, (void*) pStopLocFix, sizeof(TNAVC_stopLocFix));
	    if (retVal != SUPLC_SUCCESS)
	    {
	        PRINT_TAG(" stopNetInitiatedFix: Navc Stop FAILED !!! \n");
			LOGD("-stopNetInitiatedFix: Navc Stop FAILED  ");
	        return retVal;
	    }

	    DEBUG_SUPLC(PRINT_TAG(" stopNetInitiatedFix: Exiting Successfully \n"));
		LOGD("-stopNetInitiatedFix");
		gp_suplCntl->e_sessFlow=SUPLC_FLOW_NONE;
	    return retVal;
	}
	else

	{
		LOGD("stopNetInitiatedFix - Invalid state - Not stopping NAVC");
		return SUPLC_SUCCESS;
	}

}


int SetNavcInit(void *pNavcState, int mode)
{
  static int written;
  static int NavcState;
  if(mode == 1)
  {
    written = 1;
    NavcState = *(int*)pNavcState;
    return 1;
  }
 else
 {
    if(written == 1)
    {
      written = 0;
      return NavcState;
    }
  return -1;
 }
}
/* Added for NI */
void passNotificationResult(int res,int tid)
{
	SUPLC_passNotificationResult((int) res, (int)tid);
}
static eSUPLC_Result handleAssistAck(const void *const p_data,
                                            const void *const p_suplCntl)
{
    /* Note: This function is same as the previous one, but included this for readability. */

    DataReqData *p_dataReqData = NULL;
    eSUPLC_Result retVal = SUPLC_SUCCESS;

    McpU16 u16_appId = SUPL_APP_ID;

    DEBUG_SUPLC(PRINT_TAG(" handleAssistAck: Entering \n"));

    /* Fill DataReqData structure. */
    retVal = fillDataReqDataForPosResp(&p_dataReqData, &u16_appId, p_data, p_suplCntl);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" handleAssistAck: Data filling FAILED \n");
        return retVal;
    }

    /* Send UPL_DATA_REQ to SUPL Core Module. */
    retVal = sendDataToCore(u16_appId, (McpU8 *)p_dataReqData, UPL_DATA_REQ);
    if (retVal != SUPLC_SUCCESS)
    {
            PRINT_TAG(" handleAssistAck: Sending data to Core FAILED \n");
            return retVal;
    }

    DEBUG_SUPLC(PRINT_TAG(" handleAssistAck: Exiting Successfully \n"));
    return retVal;
}



eSUPLC_Result SUPLC_AssistAck(const void *const p_data, const void *const p_suplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_AssistAck Entering \n"));

    retVal = handleAssistAck(p_data, p_suplCntl);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPLC_AssistAck: Position Response Handling FAILED !!! \n");
        return retVal;
    }

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_AssistAck  Exiting Successfully \n"));
    return retVal;
}

eSUPLC_Result SUPLC_ConRes(const void *const p_data, const void *const p_suplCntl)
{
    eSUPLC_Result retVal = SUPLC_SUCCESS;
	McpU8 conRes=(McpU8)(*(McpU8*)p_data);
	LOGD("SUPLC_ConRes: Con Res=[%d]", conRes);

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_ConRes Entering \n"));

    retVal = SUPLC_sendConnectionResult(conRes);
    if (retVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPLC_ConRes: Connection Response Handling FAILED !!! \n");
        return retVal;
    }

    DEBUG_SUPLC(PRINT_TAG(" SUPLC_ConRes  Exiting Successfully \n"));
    return retVal;
}

