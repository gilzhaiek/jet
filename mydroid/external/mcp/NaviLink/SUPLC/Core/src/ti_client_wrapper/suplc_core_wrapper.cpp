/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_core_wrapper.cpp
 *
 * Description      :   Common interface between SUPL Core and NaviLink.
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */

#include "ti_client_wrapper/suplc_core_wrapper.h"
#include "ti_client_wrapper/suplc_hal_os.h"
#include "suplcontroller/SUPLController.h"

#include <errno.h>


#define ENABLE_WRAPPER_DEBUG
#ifdef ENABLE_WRAPPER_DEBUG
#define LOG_TAG "libhardware_legacy"
#include <utils/Log.h>
    #define  DEBUG_SUPLC_WRAPPER(...)   LOGD(__VA_ARGS__)
#else
    #define  DEBUG_SUPLC_WRAPPER(...)   ((void)0)
#endif /* ENABLE_WRAPPER_DEBUG */

#define GSM_INT_CI_LAC_MCC_MNC 5
#define GSM_INT_CI_LAC_MCC_MNC_NMR 53


extern SEM_HANDLE gp_semHandle;

/**
 * Function:        SUPLC_initClient
 * Brief:           Wrapper Function for calling SUPL Core's SUPL_Init.
 * Description:
 * Note:
 * Params:          p_callBackResponse - SUPLC's callback function.
 * Return:          Success: 1.
                    Failure: 0.
 */
uInt8 SUPLC_initClient(GPSCallBack p_callBackResponse, void * pNavPtr)
{
    /** This initializes SUPL Client if sent with valid callback.
      * Else starts SUPL Client. */
    //return SUPL_Init(p_callBackResponse);

    DEBUG_SUPLC_WRAPPER(" SUPLC_initClient: Entering \n");

    if(p_callBackResponse == NULL)
    {
        DEBUG_SUPLC_WRAPPER(" SUPLC_initClient: Calling SUPLStart \n");
        return Engine::CSUPLController::GetInstance().SUPLStart();
    }
    else
    {
        DEBUG_SUPLC_WRAPPER(" SUPLC_initClient: Calling SUPLInit \n");
        Engine::CSUPLController::GetInstance().gps_callback = p_callBackResponse;
        if(Engine::CSUPLController::GetInstance().SUPLInit(pNavPtr) != TRUE)
        {
            return FALSE;
        }

    }

    DEBUG_SUPLC_WRAPPER(" SUPLC_initClient: Exiting Successfully \n");
    return TRUE;
}

/**
 * Function:        SUPLC_deInitClient
 * Brief:           Wrapper Function for calling SUPL Core's SUPL_DeInit.
 * Description:
 * Note:
 * Params:          None.
 * Return:          Success: 1.
                    Failure: 0.
 */
uInt8 SUPLC_deInitClient()
{
    DEBUG_SUPLC_WRAPPER(" +SUPLC_deInitClient \n");

	Engine::CSUPLController::GetInstance().SUPLCleanup();

    DEBUG_SUPLC_WRAPPER(" -SUPLC_deInitClient \n");
    return TRUE;
}

/**
 * Function:        SUPLC_sendNetworkMessage
 * Brief:           Wrapper Function for calling SUPL Core's HandleMessage.
                    ( Its a Network Component Function. )
 * Description:
 * Note:
 * Params:          body - Network Message.
                    size - Message Size.
 * Return:          Success: 0.
                    Failure: -1.
 */
sInt32 SUPLC_sendNetworkMessage(const char *body, uInt16 size) /* KlockWork Changes */
{
    sInt32 retVal = 0;
    /* Post message to SUPL Controller's queue for processing. */
    DEBUG_SUPLC_WRAPPER(" SUPLC_sendNetworkMessage: Calling Network.HandleMessage \n");
    DEBUG_SUPLC_WRAPPER(" SUPLC_sendNetworkMessage: Size = %d \n", size);
#if 0
    for(sInt32 count = 0; count < size; count++)
    {
        DEBUG_SUPLC_WRAPPER(" SUPLC_sendNetworkMessage: body[%d] is %x \n", count, body[count]);
    }
#endif
    retVal = Engine::CSUPLController::GetInstance().GetNetwork().HandleMessage(body, size);

    /* Release semaphore for SUPL_Core to process this message. */
    DEBUG_SUPLC_WRAPPER(" SUPLC_sendNetworkMessage: Releasing sem... \n");
    if ( SUPLC_releaseSemaphore(gp_semHandle) < 0 )
    {
        DEBUG_SUPLC_WRAPPER(" SUPLC_sendNetworkMessage: sem_post FAILED !!! errno = %d  \n", errno);
        return -1;
    }

    return retVal;
}

/* Added for NI */
void SUPLC_passNotificationResult(int res, int tid)
{
    if(Engine::CSUPLController::GetInstance().SUPLStart() != TRUE)
    {
        return;
    }

    Engine::CSUPLController::GetInstance().GetDevice().NotificationResult(res, tid);
}

/**
 * Function:        SUPLC_RemoveQueuedUplMsgs
 * Brief:           Wrapper Function for calling SUPL Core's SUPL_Control.
 * Description:
 * Note:
 */
void SUPLC_RemoveQueuedUplMsgs()
{
	Engine::CSUPLController::GetInstance().GetGPS().RemoveAllCmd();
}
/**
 * Function:        SUPLC_sendUplMsg
 * Brief:           Wrapper Function for calling SUPL Core's SUPL_Control.
 * Description:
 * Note:
 * Params:          u16_appId - App Id.
                    p_data - Pointer to data that has to be sent to Core.
                    u32_command - Command to be sent
 * Return:          Success: 1.
                    Failure: 0.
 */
uInt8 SUPLC_sendUplMsg(uInt16 u16_appId, uInt32 u32_command, uInt8 *p_data)
{
    DEBUG_SUPLC_WRAPPER(" SUPLC_sendUplMsg: Entering \n");

   if(Engine::CSUPLController::GetInstance().hThread != 0)
    {
        switch (u32_command)
        {
            case UPL_START_REQ:
            case UPL_DATA_REQ:
            case UPL_STOP_REQ:
            case UPL_NOTIFY_RESP:
                {
                    Platform::CGPSCommand *cmd = NULL;
                    cmd = Platform::CGPSCommand::CreateCommand((Engine::uint64_t) u32_command,
                                                      u16_appId, NULL, 0, p_data);
                    if(!cmd)
                    {
                        return FALSE;
                    }

                    if(Engine::CSUPLController::GetInstance().GetGPS().AddCommand(cmd) != TRUE)
                    {
                    	DEBUG_SUPLC_WRAPPER("\nIn SUPLC_sendUplMsg, u32_command = %d, delete cmd\n",u32_command);
                        delete cmd;
                        return FALSE;
                    }
					DEBUG_SUPLC_WRAPPER("\nIn SUPLC_sendUplMsg, u32_command = %d, AddCommand(cmd) is TRUE\n",u32_command);
                    break;
                }
            default: return FALSE;
        }
        //debugMessage("SUPL:SUPL_Control was successfully called");

        /* Release semaphore for SUPL_Core to process this message. */
        DEBUG_SUPLC_WRAPPER(" SUPLC_sendUplMsg: sem_posting...  \n");
        if ( SUPLC_releaseSemaphore(gp_semHandle) < 0 )
        {
            DEBUG_SUPLC_WRAPPER(" SUPLC_sendUplMsg: sem_post FAILED !!! err num = %d \n", errno);
            return -1;
        }

        return TRUE;
    }
    else
    {
        //debugMessage("SUPL controller thread is not started!");
        return FALSE;
    }
}


/**
 * Function:        SUPLC_registerNalHandler
 * Brief:           Wrapper Function for registering NAL Functions with SUPL Core.
 * Description:
 * Note:
 * Params:          p_nalExecuteCommand - Callback Function.
 * Return:          Success: 1.
                    Failure: 0.
 */
uInt8 SUPLC_registerNalHandler(NALCallback p_nalCallback)
{
    if (p_nalCallback == NULL)
    {
        /* Don't proceed without a registered callback for MCPF_NAL. */
        return 0;
    }

    /* For now, maintain a global variable for this. */
    Engine::CSUPLController::GetInstance().p_nalExecuteCommand = p_nalCallback;
    return 1;
}


/**
 *  Function:        SUPLC_getGsmCellInfo
 *  Brief:           Gets the cell information.
 *  Description:
 *  Note:
 *  Params:
 *  Return:          Success: 1.
 *                   Failure: 0.
 *
 */

uInt8 SUPLC_getGsmCellInfo(my_CellInfo_t *p_outCellInfo)
{
    int info[GSM_INT_CI_LAC_MCC_MNC_NMR];

    if (getGSMInfo(info) < 0)
    {
        DEBUG_SUPLC_WRAPPER(" SUPLC_getGsmCellInfo: getGSMInfo FAILED !!! \n");
        return 0;
    }

	if(info[4]==cellInfo::GSM||(info[4]==cellInfo::UNKNOWN))
	{
		DEBUG_SUPLC_WRAPPER(" SUPLC_getGsmCellInfo: Cell Id Type is GSM !!! \n");
	    p_outCellInfo->cell_info_gsm.gsm_mcc = info[2];
	    p_outCellInfo->cell_info_gsm.gsm_mnc = info[3];
	    p_outCellInfo->cell_info_gsm.gsm_lac = info[1];
	    p_outCellInfo->cell_info_gsm.gsm_ci = info[0];
		p_outCellInfo->cell_type=cellInfo::GSM;
		//Currently hardcoded Only for 4 cells, to be removed while modem integration
		int nmr_count=0;
		p_outCellInfo->cell_info_gsm.nmr_quantity=4;
		for(int count=5;count<17;count+=3)
		{
			p_outCellInfo->cell_info_gsm.gsm_nmr[nmr_count].arfcn=info[count];
			p_outCellInfo->cell_info_gsm.gsm_nmr[nmr_count].bsic=info[count+1];
			p_outCellInfo->cell_info_gsm.gsm_nmr[nmr_count].rxlev=info[count+2];
			nmr_count++;
		}
	}
	else if(info[4]==cellInfo::WCDMA)
	{
		DEBUG_SUPLC_WRAPPER(" SUPLC_getGsmCellInfo: Cell Id Type is WCDMA !!! \n");
	    p_outCellInfo->cell_info_wcdma.wcdma_mcc = (long) info[2];
    	p_outCellInfo->cell_info_wcdma.wcdma_mnc = (long) info[3];
    	p_outCellInfo->cell_info_wcdma.wcdma_ci = (long) info[0];
		p_outCellInfo->cell_type=cellInfo::WCDMA;
		//tbd - Network measurement list for WCDMA, ecid

	}
	
	else if(info[4]==cellInfo::CDMA)
	{
		//Not supported
		DEBUG_SUPLC_WRAPPER(" SUPLC_getGsmCellInfo: CDMA cell Info not Supported in SUPL !!! \n");
		return 0;

	}

    return 1;
}
#if defined(DOCOMO_SUPPORT_WCDMA)
/**
 *  Function:        SUPLC_getWcdmaCellInfo
 *  Brief:           Gets the cell information.
 *  Description:
 *  Note:
 *  Params:
 *  Return:          Success: 1.
 *                   Failure: 0.
 *
 */
uInt8 SUPLC_getWcdmaCellInfo(my_CellInfoWCDMA_t *p_outWcdmaCellInfo)
{
    int info[GSM_INT_CI_LAC_MCC_MNC];
	DEBUG_SUPLC_WRAPPER("\n----Entering SUPLC_getWcdmaCellInfo----\n");
    if (getGSMInfo(info) < 0)
    {
        DEBUG_SUPLC_WRAPPER(" SUPLC_getGsmCellInfo: getGSMInfo FAILED !!! \n");
        return 0;
    }
    p_outWcdmaCellInfo->wcdma_mcc = (long) info[2];
    p_outWcdmaCellInfo->wcdma_mnc = (long) info[3];
    p_outWcdmaCellInfo->wcdma_ci = (long) info[0];
    DEBUG_SUPLC_WRAPPER("\np_outWcdmaCellInfo->wcdma_mcc = %ld\n",p_outWcdmaCellInfo->wcdma_mcc);
    DEBUG_SUPLC_WRAPPER("\np_outWcdmaCellInfo->wcdma_mnc = %ld\n",p_outWcdmaCellInfo->wcdma_mnc);
    DEBUG_SUPLC_WRAPPER("\np_outWcdmaCellInfo->wcdma_ci = %ld\n",p_outWcdmaCellInfo->wcdma_ci);
    return 1;
}
#endif

/**
 * Function:        SUPLC_eeCallback
 * Brief:           Wrapper Function for calling EE Callback.
 * Description:
 * Note:
 * Params:          u16_appId - App Id.
                    p_data - Pointer to data that has to be sent to Core.
                    u32_command - Command to be sent
 * Return:          Success: 1.
                    Failure: 0.
 */
uInt8 SUPLC_eeCallback(uInt16 u16_appId, uInt32 u32_command, uInt8 *p_data)
{
    DEBUG_SUPLC_WRAPPER(" SUPLC_eeCallback: Entering \n");
    //EECallBack(u16_appId, u32_command, p_data);

    DEBUG_SUPLC_WRAPPER(" SUPLC_eeCallback: Exiting Successfully. \n");
    return TRUE;
}

/**
 * Function:        SUPLC_sendConnectionResult
 * Brief:           Wrapper Function for calling SUPL Core's Connection Handler.
                    ( Its a Network Component Function. )
 * Description:
 * Note:
 * Params:          body - Connection Response.
                    size - Message Size.
 * Return:          Success: 0.
                    Failure: -1.
 */
sInt32 SUPLC_sendConnectionResult(uInt8 conRes)
{
    sInt32 retVal = 0;
    /* Post message to SUPL Controller's queue for processing. */
    DEBUG_SUPLC_WRAPPER(" SUPLC_sendConnectionResult: Calling Connection.HandleMessage \n");
	//LOGD("SUPLC_sendConnectionResult: Con Res=[%d]", conRes);

    retVal = Engine::CSUPLController::GetInstance().HandleServerConnection(conRes);

    /* Release semaphore for SUPL_Core to process this message. */
    DEBUG_SUPLC_WRAPPER(" SUPLC_sendConnectionResult: Releasing sem... \n");
#if 1
	if ( SUPLC_releaseSemaphore(gp_semHandle) < 0 )
    {
        DEBUG_SUPLC_WRAPPER(" SUPLC_sendNetworkMessage: sem_post FAILED !!! errno = %d  \n", errno);
        return -1;
    }
#endif

    return retVal;
}

