/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_main.c
 *
 * Description      :
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */

#include "mcpf_main.h"
#include "mcpf_mem.h"
#include "mcpf_msg.h"

#include "navc_api.h"

#include "suplc_actionHandler.h"
#include "suplc_cmdHandler.h"
#include "suplc_evtHandler.h"
#include "suplc_app_api.h"
#include "suplc_outEvt.h"
#include "suplc_main.h"
#include "nav_log_msg.h"
#include "nav_log_cust_codes.h"

#define ENABLE_SUPLC_DEBUG
#ifdef ENABLE_SUPLC_DEBUG
    #include <utils/Log.h>
    #define PRINT_TAG LOGD 
	#define  DEBUG_SUPLC(x)   x
#endif	
#define MAX_BUF_LENGTH 128
#define GPS_CONFIG_FILE_PATH "/system/etc/gps/config/GpsConfigFile.txt"
#define RDONLY "r"
#define PROT_SUPPORT "pos_protocol"
#define ENABLE_RRLP "rrlp"
#define ENABLE_RRC "rrc"
#define TRUE 1
#define FALSE 0

TSUPL_Ctrl *p_suplCtrl = NULL;        /* SUPLC's Control Structure. */

static int stringCompare(const char *const p_string_1,
                                   const char *const p_string_2)
{
    if ( strcmp(p_string_1, p_string_2) == 0)
    {
        return TRUE;
    }
    return FALSE;
}
/**
 * Function:        SUPL_Create
 * Brief:           Init the SUPL
 * Description:     This function is called from MCPF?s socket server task for creating
                    SUPL_C. It creates a SUPLC task and registers its event callbacks
                    with MCPF. It also creates SUPL_Core component.
                    (Allocates internal data and starts SUPLC main thread.)
 * Note:            External Function.
 * Params:          hMCpf - handle to MCPF
 * Return:          Success: RES_COMPLETE.
                    Failure: RES_ERROR.
 */
DLL_EXPORT EMcpfRes SUPL_Create(handle_t hMcpf)
{
    //static TSUPL_Ctrl *p_suplCtrl = NULL;        /* SUPLC's Control Structure. */

    EMcpfRes retVal = RES_COMPLETE;
    eSUPLC_Result suplcRetVal = SUPLC_SUCCESS;
    FILE *fp = NULL;
    char a_inputBuffer[MAX_BUF_LENGTH] = {'\0'};
    char *p_token = NULL;

    DEBUG_SUPLC(PRINT_TAG("SUPL_Create: Entering. \n"));

    /* Allocate SUPL Control Structure */
    p_suplCtrl = (TSUPL_Ctrl *) mcpf_mem_alloc(hMcpf, sizeof(TSUPL_Ctrl));
    if (p_suplCtrl == NULL)
    {
        PRINT_TAG(" SUPL_Create: Memory Allocation for SUPL Control Structure FAILED !!! \n");
        return RES_ERROR;
    }

    /* Initialize the memory.*/
    mcpf_mem_zero(hMcpf, p_suplCtrl, sizeof(TSUPL_Ctrl));
	    
    PRINT_TAG(" readConfigFile: Start \n");

    fp = fopen(GPS_CONFIG_FILE_PATH, RDONLY);
    if (NULL == fp)
    {
        PRINT_TAG(" readConfigFile: fopen FAILED !!! \n");
        return RES_ERROR;
    }

    while( (fgets(a_inputBuffer, sizeof(a_inputBuffer), fp) ) &&
           (stringCompare(a_inputBuffer, "\n") != TRUE ) )
    {
       PRINT_TAG(" readConfigFile: a_inputBuffer = %s \n", a_inputBuffer);
       p_token = (char *)strtok(a_inputBuffer, ":");
       if ( NULL == p_token )
       {
           /* Continue with the next line. */
           continue;
       }
	   if((stringCompare(p_token, "gps_mode") == TRUE))
	   	{
			p_token = (char *) strtok(NULL, ":");
            if (NULL == p_token)
            {
                PRINT_TAG(" readConfigFile: strtok returned NULL !!! \n");
                fclose(fp);
                return RES_ERROR;;
            }
			if (stringCompare(p_token, "msbased_mode") ==  TRUE)
			{
				p_suplCtrl->u8_fixMode = SUPLC_FIXMODE_MSBASED; // For NI
				LOGD(" readConfigFile: MS_BASED Support Requested. \n");
			}
			else if(stringCompare(p_token, "msassisted_mode") ==  TRUE)
			{
				p_suplCtrl->u8_fixMode = SUPLC_FIXMODE_MSASSISTED; // For NI
				LOGD(" readConfigFile: MS_ASSISTED Support Requested. \n");
			}
	   	}
#if 0 	   
        if ((stringCompare(p_token, PROT_SUPPORT) == TRUE))
       {
            p_token = (char *) strtok(NULL, ":");
            if (NULL == p_token)
            {
                PRINT_TAG(" readConfigFile: strtok returned NULL !!! \n");
                fclose(fp);
                return RES_ERROR;;
            }
            if ( stringCompare(p_token, ENABLE_RRLP) == TRUE )
            {
    p_suplCtrl->e_protocol = SUPLC_PROT_RRLP;
               PRINT_TAG(" readConfigFile: RRLP Support Requested. \n");
            }
            else if (stringCompare(p_token, ENABLE_RRC) == TRUE)
            {
               p_suplCtrl->e_protocol = SUPLC_PROT_RRC;
               PRINT_TAG(" readConfigFile: RRC Support Not Requested. \n");
            }
	    else
	    {
                PRINT_TAG(" readConfigFile: PROTOCOL NOT SUPPORTED !!! \n");
                fclose(fp);
                return RES_ERROR;;
            }
       }
#endif		
    }

    fclose(fp);
    PRINT_TAG(" readConfigFile: Over. \n");

    /* Init SUPL control structure */
    p_suplCtrl->hMcpf = hMcpf;
    p_suplCtrl->mMemPool = mcpf_memory_pool_create(p_suplCtrl->hMcpf,
                           MAX_SIZE_PER_ELEMENT,
                           MAX_NUMBER_OF_ELEMENTS);
    if (p_suplCtrl->mMemPool == NULL)
    {
        PRINT_TAG(" SUPL_Create: Memory Pool Creation FAILED !!! \n");
        return RES_ERROR;
    }

    /* Create SUPL_C task and queues */
    retVal = mcpf_CreateTask(p_suplCtrl->hMcpf, TASK_SUPL_ID, "SUPLC",
                             SUPLC_MAX_NUM_QUEUES, NULL, NULL);
    if (retVal != RES_COMPLETE)
    {
        PRINT_TAG(" SUPL_Create: SUPLC Task Creation FAILED !!! \n");
        return RES_ERROR;
    }

    /* Register SUPLC's Handlers with MCPF. */
    retVal = suplcRegisterHandlers(p_suplCtrl);
    if (retVal != RES_COMPLETE)
    {
        PRINT_TAG(" SUPL_Create: SUPLC's handler registration FAILED !!! \n");
        return RES_ERROR;
    }

#if 0
    /** Initialize SUPL Core.
      * This is implemented in suplc_app_api.c */
    suplcRetVal = SUPLC_appInitReq(p_suplCtrl);
    if (suplcRetVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPL_Create: SUPL Core's initialization FAILED !!! \n");
        return RES_ERROR;
    }
#endif

    /* Enable SUPLC's Queues for receiving various messages */
    retVal = suplcEnableMessageQueues(p_suplCtrl);
    if (retVal != RES_COMPLETE)
    {
        PRINT_TAG(" SUPL_Create: SUPLC's queue enable FAILED !!! \n");
        return RES_ERROR;
    }

    /* Register with NAVC */
    suplcRetVal = SUPLC_registerAssistanceSource(p_suplCtrl);
    if (suplcRetVal != SUPLC_SUCCESS)
    {
        PRINT_TAG(" SUPL_Create: SUPLC send message FAILED !!! \n");
        return RES_ERROR;
    }

    DEBUG_SUPLC(PRINT_TAG("SUPL_Create: Exiting Successfully \n"));
    return retVal;
}


/**
 * Function:        SUPL_Destroy
 * Brief:           DeInit the SUPL
 * Description:     This function is called from MCPF?s socket server task for destroying
                    SUPL_C.
 * Note:            External Function.
 * Params:          None.
 * Return:          Success: RES_COMPLETE.
                    Failure: RES_ERROR.
 */
DLL_EXPORT EMcpfRes SUPL_Destroy(void)
{
    EMcpfRes retVal = RES_COMPLETE;

    DEBUG_SUPLC(PRINT_TAG(" +SUPL_Destroy \n"));

    if (p_suplCtrl != NULL)
    {

        /* ToDo: UnRegister with NAVC - When support is provided */

        /* Disable SUPLC's Queues */
        retVal = suplcDisableMessageQueues(p_suplCtrl);
        if (retVal != RES_COMPLETE)
        {
            PRINT_TAG(" SUPL_Destroy: SUPLC's queue disable FAILED !!! \n");
            return RES_ERROR;
        }

        /* UnRegister SUPLC's Handlers with MCPF. */
        retVal = suplcUnRegisterHandlers(p_suplCtrl);
        if (retVal != RES_COMPLETE)
        {
            PRINT_TAG(" SUPL_Destroy: SUPLC's handler de-registration FAILED !!! \n");
            return RES_ERROR;
        }

        /* Destroy SUPL_C task and queues */
        retVal = mcpf_DestroyTask(p_suplCtrl->hMcpf, TASK_SUPL_ID);
        if (retVal != RES_COMPLETE)
        {
            PRINT_TAG(" SUPL_Destroy: SUPLC Task Destruction FAILED !!! \n");
            return RES_ERROR;
        }

        /* DeInit SUPL control structure */
        retVal = mcpf_memory_pool_destroy(p_suplCtrl->hMcpf, p_suplCtrl->mMemPool);
        if (retVal != RES_COMPLETE)
        {
            PRINT_TAG(" SUPL_Destroy: Memory Pool Destruction FAILED !!! \n");
            return RES_ERROR;
        }

        /* DeAllocate SUPL Control Structure */
        mcpf_mem_free(p_suplCtrl->hMcpf, p_suplCtrl);
        p_suplCtrl = NULL;
    }
    else
    {
        PRINT_TAG(" p_suplCtrl is NULL \n");
        retVal = RES_ERROR;
    }

    DEBUG_SUPLC(PRINT_TAG(" -SUPL_Destroy \n"));
    return retVal;
}


/**
 * Function:        suplcRegisterHandlers
 * Brief:           Registers various SUPLC's queue handlers.
 * Description:     In order for SUPLC to be able to receive messages,
                    it should register its message handler callbacks with MCPF.
 * Note:            Internal Function.
 * Params:          p_inSuplCtrl - Handle to SUPLC Control Structure.
 * Return:          Success: RES_COMPLETE.
                    Failure: RES_ERROR.
 */
static EMcpfRes suplcRegisterHandlers(const TSUPL_Ctrl *const p_inSuplCtrl)
{
    EMcpfRes retVal = RES_COMPLETE;

    DEBUG_SUPLC(PRINT_TAG("suplcRegisterHandlers: Entering \n"));

    /* Register Command Handlers */
    retVal = mcpf_RegisterTaskQ(p_inSuplCtrl->hMcpf, TASK_SUPL_ID, SUPLC_CMD_QUEUE_ID,
                                SUPLC_commandHandler, (void *)p_inSuplCtrl);
    if (retVal != RES_COMPLETE)
    {
        PRINT_TAG(" suplcRegisterHandlers: SUPLC's command handler registration FAILED !!! \n");
        return RES_ERROR;
    }

    /* Register Action Handler */
    retVal = mcpf_RegisterTaskQ(p_inSuplCtrl->hMcpf, TASK_SUPL_ID, SUPLC_ACTION_QUEUE_ID,
                                SUPLC_actionHandler, (void *)p_inSuplCtrl);
    if (retVal != RES_COMPLETE)
    {
        PRINT_TAG(" suplcRegisterHandlers: SUPLC's action handler registration FAILED !!! \n");
        return RES_ERROR;
    }

    /* Register Event Handler */
    retVal = mcpf_RegisterTaskQ(p_inSuplCtrl->hMcpf, TASK_SUPL_ID, SUPLC_INTERNAL_EVT_QUEUE_ID,
                                SUPLC_eventHandler, (void *)p_inSuplCtrl);
    if (retVal != RES_COMPLETE)
    {
        PRINT_TAG(" suplcRegisterHandlers: SUPLC's event handler registration FAILED !!! \n");
        return RES_ERROR;
    }

    DEBUG_SUPLC(PRINT_TAG("suplcRegisterHandlers: Exiting Successfully \n"));
    return retVal;
}

/**
 * Function:        suplcUnRegisterHandlers
 * Brief:           UnRegisters various SUPLC's queue handlers.
 * Description:     UnRegister SUPC's message handler callbacks with MCPF.
 * Note:            Internal Function.
 * Params:          p_inSuplCtrl - Handle to SUPLC Control Structure.
 * Return:          Success: RES_SUCCESS.
                    Failure: RES_ERROR.
 */
static EMcpfRes suplcUnRegisterHandlers(const TSUPL_Ctrl *const p_inSuplCtrl)
{
    EMcpfRes retVal = RES_COMPLETE;

    DEBUG_SUPLC(PRINT_TAG(" +suplcUnRegisterHandlers \n"));


    /* UnRegister Event Handler */
    retVal = mcpf_UnregisterTaskQ(p_inSuplCtrl->hMcpf, TASK_SUPL_ID, SUPLC_INTERNAL_EVT_QUEUE_ID);
    if (retVal != RES_COMPLETE)
    {
        PRINT_TAG(" suplcUnRegisterHandlers: SUPLC's event handler de-registration FAILED !!! \n");
        return RES_ERROR;
    }

    /* UnRegister Action Handler */
    retVal = mcpf_UnregisterTaskQ(p_inSuplCtrl->hMcpf, TASK_SUPL_ID, SUPLC_ACTION_QUEUE_ID);
    if (retVal != RES_COMPLETE)
    {
        PRINT_TAG(" suplcUnRegisterHandlers: SUPLC's action handler de-registration FAILED !!! \n");
        return RES_ERROR;
    }

    /* UnRegister Command Handlers */
    retVal = mcpf_UnregisterTaskQ(p_inSuplCtrl->hMcpf, TASK_SUPL_ID, SUPLC_CMD_QUEUE_ID);
    if (retVal != RES_COMPLETE)
    {
        PRINT_TAG(" suplcUnRegisterHandlers: SUPLC's command handler de-registration FAILED !!! \n");
        return RES_ERROR;
    }

    DEBUG_SUPLC(PRINT_TAG(" -suplcUnRegisterHandlers \n"));
    return retVal;
}


/**
 * Function:        suplcEnableMessageQueues
 * Brief:           Enables various SUPLC's queues.
 * Description:
 * Note:
 * Params:          p_inSuplCtrl - Handle to SUPLC Control Structure.
 * Return:          Success: RES_COMPLETE.
                    Failure: RES_ERROR.
 */
static EMcpfRes suplcEnableMessageQueues(const TSUPL_Ctrl *const p_inSuplCtrl)
{
    EMcpfRes retVal = RES_COMPLETE;
    DEBUG_SUPLC(PRINT_TAG(" suplcEnableMessageQueues: Entering \n"));

    /* Enable SUPLC's Command Queue. */
    retVal = mcpf_MsgqEnable(p_inSuplCtrl->hMcpf, TASK_SUPL_ID, SUPLC_CMD_QUEUE_ID);
    if (retVal != RES_COMPLETE)
    {
        PRINT_TAG(" suplcEnableMessageQueues: Enabling command queue FAILED !!! \n");
        return RES_ERROR;
    }

    /* Enable SUPLC's Action Queue. */
    retVal = mcpf_MsgqEnable(p_inSuplCtrl->hMcpf, TASK_SUPL_ID, SUPLC_ACTION_QUEUE_ID);
    if (retVal != RES_COMPLETE)
    {
        PRINT_TAG(" suplcEnableMessageQueues: Enabling action queue FAILED !!! \n");
        return RES_ERROR;
    }

    /* Enable SUPLC's Intenal Event Queue. */
    retVal = mcpf_MsgqEnable(p_inSuplCtrl->hMcpf, TASK_SUPL_ID, SUPLC_INTERNAL_EVT_QUEUE_ID);
    if (retVal != RES_COMPLETE)
    {
        PRINT_TAG(" suplcEnableMessageQueues: Enabling event queue FAILED !!! \n");
        return RES_ERROR;
    }

    DEBUG_SUPLC(PRINT_TAG(" suplcEnableMessageQueues: Exiting Successfully \n"));
    return retVal;
}

/**
 * Function:        suplcDisableMessageQueues
 * Brief:           Disable SUPLC's queues.
 * Description:
 * Note:
 * Params:          p_inSuplCtrl - Handle to SUPLC Control Structure.
 * Return:          Success: RES_COMPLETE.
                    Failure: RES_ERROR.
 */
static EMcpfRes suplcDisableMessageQueues(const TSUPL_Ctrl *const p_inSuplCtrl)
{
    EMcpfRes retVal = RES_COMPLETE;
    DEBUG_SUPLC(PRINT_TAG(" +suplcDisableMessageQueues \n"));

    /* Disable SUPLC's Intenal Event Queue. */
    retVal = mcpf_MsgqDisable(p_inSuplCtrl->hMcpf, TASK_SUPL_ID, SUPLC_INTERNAL_EVT_QUEUE_ID);
    if (retVal != RES_COMPLETE)
    {
        PRINT_TAG(" suplcDisableMessageQueues: Disabling event queue FAILED !!! \n");
        return RES_ERROR;
    }

    /* Disable SUPLC's Action Queue. */
    retVal = mcpf_MsgqDisable(p_inSuplCtrl->hMcpf, TASK_SUPL_ID, SUPLC_ACTION_QUEUE_ID);
    if (retVal != RES_COMPLETE)
    {
        PRINT_TAG(" suplcDisableMessageQueues: Disabling action queue FAILED !!! \n");
        return RES_ERROR;
    }

    /* Disable SUPLC's Command Queue. */
    retVal = mcpf_MsgqDisable(p_inSuplCtrl->hMcpf, TASK_SUPL_ID, SUPLC_CMD_QUEUE_ID);
    if (retVal != RES_COMPLETE)
    {
        PRINT_TAG(" suplcDisableMessageQueues: Disabling command queue FAILED !!! \n");
        return RES_ERROR;
    }

    DEBUG_SUPLC(PRINT_TAG(" -suplcDisableMessageQueues \n"));
    return retVal;
}
