/***************************************************************************
**+----------------------------------------------------------------------+**
**|                                ****                                  |**
**|                                ****                                  |**
**|                                ******o***                            |**
**|                          ********_///_****                           |**
**|                           ***** /_//_/ ****                          |**
**|                            ** ** (__/ ****                           |**
**|                                *********                             |**
**|                                 ****                                 |**
**|                                  ***                                 |**
**|                                                                      |**
**|     Copyright (c) 1998-2008 Texas Instruments Incorporated           |**
**|                        ALL RIGHTS RESERVED                           |**
**|                                                                      |**
**| Permission is hereby granted to licensees of Texas Instruments       |**
**| Incorporated (TI) products to use this computer program for the sole |**
**| purpose of implementing a licensee product based on TI products.     |**
**| No other rights to reproduce, use, or disseminate this computer      |**
**| program, whether in part or in whole, are granted.                   |**
**|                                                                      |**
**| TI makes no representation or warranties with respect to the         |**
**| performance of this computer program, and specifically disclaims     |**
**| any responsibility for any damages, special or consequential,        |**
**| connected with the use of this program.                              |**
**|                                                                      |**
**+----------------------------------------------------------------------+**
***************************************************************************/

/** \file   mcp_hal_st.c
 *  \brief  Linux OS Adaptation Layer for ST Interface implementation
 *
 *  \see    mcp_hal_st.h
 */

#include <stdio.h>
#include <termios.h>
#include <fcntl.h>
#include <errno.h>
#include <semaphore.h>
#include <pthread.h>
#include <unistd.h>
#include <sys/ioctl.h>

#include "mcpf_mem.h"
#include "mcpf_report.h"
#include "mcp_hal_os.h"
#include "mcp_hal_types.h"
#include "mcp_hal_string.h"

#include "mcp_hal_st.h"

#include <utils/Log.h>
#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG "MCP.MCP_HAL_ST"    // our identification for logcat (adb logcat MCP.MCP_HAL_ST:V *:S)

/************************************************************************
 * Defines
 ************************************************************************/

#define HALST_MODULE_NAME_MAX_SIZE			10

#define NUMBER_ZERO         0
#define MAX_FILENAME_SIZE   20

#define MAX_RX_DATA_SIZE	2048

#define MIN(a,b) (((a) < (b)) ? (a):(b))

#define HALST_IOCTL_VS_EVENT				1
#define HALST_IOCTL_WAIT_4_CMD_CMPLT		2

/************************************************************************
 * Types
 ************************************************************************/
typedef int             	PORT_HANDLE;
typedef pthread_t       	THREAD_HANDLE;
typedef sem_t*          	SEM_HANDLE;

#ifdef TRAN_DBG
/* Debug counters */
typedef struct
{

    McpU32       Tx;
    McpU32       TxCompl;
    McpU32       Rx;
    McpU32       RxReadyInd;

} THalStDebugCount;
#endif

/* Transport Layer Object */
typedef struct
{
    /* module handles */
    handle_t              		hMcpf;
    HalST_EventHandlerCb  	   fEventHandlerCb;
    handle_t              		hHandleCb;
    THalSt_PortCfg         	tPortCfg;
    char					         cDrvName[HALST_MODULE_NAME_MAX_SIZE];
    char 					      devFileName[MAX_FILENAME_SIZE];

	 char 					      cRxDataStore[MAX_RX_DATA_SIZE]; /* Stores the data received over ST */

    PORT_HANDLE           	   hOsPort;
    THREAD_HANDLE         	   hOsThread;
    sem_t                 		tSem;
    int 				  	         pipeFd[2];

    McpBool               		terminationFlag;
    McpS32                		iRxReadNum;	      /* number of bytes requested to read */
    McpS32                		iRxAvailNum;	   /* number of available bytes to read from ST device */
    McpS32                		iRxRepNum;	    	/* number of bytes to report for previous read call */

	 McpS32                		iRxBufferedNum;	/* number of bytes buffered in cRxDataStore */
	 McpS32                		iRxBufferRdIndx;	/* Read index in cRxDataStore */

    McpU8*                    pInData;       	/* Current data pointer for receiver */
    McpBool				         bIsBlockOnWrite;  /* Whether the client should work with 'Block on Write' Mode */

} THalStObj;


/************************************************************************
 * Internal functions prototypes
 ************************************************************************/

static EMcpfRes stPortInit    (THalStObj*    pHalSt);
static EMcpfRes stPortConfig  (THalStObj*    pHalSt);
static void*    stThreadFun   (void*         pParam);
static EMcpfRes stPortDestroy (THalStObj*    pHalSt);
static EMcpfRes stPortRxReset (THalStObj*    pHalSt, McpU8* pBuf, McpU16 len);
static McpU32   stSpeedToOSbaudrate (handle_t hMcpf, ThalStSpeed speed);

static void     signalSem  (handle_t hMcpf, SEM_HANDLE hSem);
static void     waitForSem (handle_t hMcpf, SEM_HANDLE hSem);
static void     halStDestroy (THalStObj* pHalSt);

/************************************************************************
 *
 *   Module functions implementation
 *
 ************************************************************************/

/**
 * \fn     HAL_ST_Create
 * \brief  Create the HAL ST object
 *
 */

handle_t HAL_ST_Create (const handle_t hMcpf, const char* pModuleName)
{
    THalStObj* pHalSt = 0;

    pHalSt = mcpf_mem_alloc (hMcpf, sizeof(THalStObj));
    if (pHalSt == NULL)
        return NULL;

    mcpf_mem_zero (hMcpf, pHalSt, sizeof(THalStObj));

    pHalSt->hMcpf = hMcpf;
	 if (strlen(pModuleName) < HALST_MODULE_NAME_MAX_SIZE)
	 {
		 strncpy (pHalSt->cDrvName, pModuleName, sizeof(pHalSt->cDrvName) - 1);
	 }
	 else
	 {
		 /* driver name string is too long, truncate it */
		 mcpf_mem_copy (hMcpf, pHalSt->cDrvName, (McpU8*) pModuleName, HALST_MODULE_NAME_MAX_SIZE - 1);
		 pHalSt->cDrvName[HALST_MODULE_NAME_MAX_SIZE - 1] = 0; /* line termination */
	 }

    /* Create Rx thread semaphore semaphore */
    if (sem_init(&pHalSt->tSem,
                 0,                            /* Not shared between processes */
                 0) < NUMBER_ZERO )            /* Not available */
    {
        MCPF_REPORT_ERROR (pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("Rx sem_init failed!\n"));
        halStDestroy (pHalSt);

        return NULL;
    }
   
    ALOGI("+++ %s: TI Shared Transport Alloc OK +++\n", __FUNCTION__);
    return ((handle_t) pHalSt);
}


/**
 * \fn     HAL_ST_Destroy
 * \brief  Destroy the HAL ST object
 *
 */
EMcpfRes HAL_ST_Destroy (handle_t hHalSt)
{
    THalStObj*  pHalSt = (THalStObj*) hHalSt;

    /* Release all the thread waiting for this semaphore before deleting it. */
    signalSem (pHalSt->hMcpf, &pHalSt->tSem);

    /* Destroy the semaphore */
    if (sem_destroy(&pHalSt->tSem) < NUMBER_ZERO)
    {
        MCPF_REPORT_ERROR (pHalSt->hMcpf, HAL_ST_MODULE_LOG,  ("sem_destroy failed!\n"));
        mcpf_mem_free (pHalSt->hMcpf, pHalSt);

        return RES_ERROR;
    }

    mcpf_mem_free (pHalSt->hMcpf, pHalSt);

    ALOGI("+++ %s: TI Shared Transport De-alloc OK +++\n", __FUNCTION__);
    return RES_OK;
}


/**
 * \fn     HAL_ST_Init
 * \brief  HAL ST object initialization
 *
 * Initiate state of HAL ST object
 *
 */
EMcpfRes HAL_ST_Init 
( 	
   handle_t                      hHalSt,
   const HalST_EventHandlerCb 	fEventHandlerCb,
   const handle_t            		hHandleCb,
   const THalSt_PortCfg*         pConf,
   const McpS8*                  pDevName,
   McpU8*                        pBuf,
   const McpU16              		len,
   McpBool						      bIsBlockOnWrite)
{
    THalStObj*    pHalSt = (THalStObj*) hHalSt;
    EMcpfRes      status = RES_OK;
    McpU8		   retry_count = 0;

    pHalSt->fEventHandlerCb = fEventHandlerCb;
    pHalSt->hHandleCb = hHandleCb;

    // Data will be read into this buffer
    pHalSt->pInData = pBuf;
    pHalSt->iRxReadNum  = len;
    pHalSt->bIsBlockOnWrite = bIsBlockOnWrite;

    if (pConf)
		mcpf_mem_copy(pHalSt->hMcpf, &pHalSt->tPortCfg, (void*)pConf, sizeof(THalSt_PortCfg));

    if (pDevName)
    {
		   mcpf_mem_copy(pHalSt->hMcpf, pHalSt->devFileName, (void*)pDevName, 
						MCP_HAL_STRING_StrLen(pDevName));
    }
    else
    {
    		MCPF_REPORT_ERROR(pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("HAL_ST_Init: Missing device Name to open\n") );
         halStDestroy (pHalSt);
		
         return RES_ERROR;
    }

    while (stPortInit (pHalSt) != RES_OK)
    {
        MCPF_REPORT_ERROR(pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("stPortInit failed! will re-try\n") );
        if (pHalSt->hOsPort)
        {
            close(pHalSt->hOsPort);
        }

	     retry_count++;
	     sleep(1);
	
	     if (retry_count > 3)
	     {
	        MCPF_REPORT_ERROR(pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("stPortInit failed! giving up\n") );
           halStDestroy (pHalSt);

           return RES_ERROR;
        }
    }

    /* Used for exiting from ST thread */
    pHalSt->terminationFlag = MCP_FALSE;

	 /* Reset Local buffer and its management variables */
	 mcpf_mem_zero (pHalSt->hMcpf, pHalSt->cRxDataStore, MAX_RX_DATA_SIZE);
	 pHalSt->iRxBufferedNum = 0;
	 pHalSt->iRxBufferRdIndx = 0;

    /* If recevied 'bIsBlockOnWrite' is TRUE --> no need for RX thread (will be working in Block on Write Mode) */
    if (pHalSt->bIsBlockOnWrite == MCP_FALSE)
    {
	    if (pthread_create( &pHalSt->hOsThread,           /* Thread handle */
	                       NULL,                           /* Default attributes */
	                       stThreadFun,                  /* Function */
	                       pHalSt) != NUMBER_ZERO)    /* Thread function parameter */
	    {
	          MCPF_REPORT_ERROR (pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("pthread_create failed!\n"));
	          if (pHalSt->hOsPort)
	          {
	            close(pHalSt->hOsPort);
	          }

	          halStDestroy (pHalSt);
	          return RES_ERROR;
	    }

	    /* wait for completion of thread initialization */
	    waitForSem (pHalSt->hMcpf, &pHalSt->tSem);
    }

    ALOGI("+++ %s: TI Shared Transport Init OK. Device: [%s] +++\n", __FUNCTION__, pHalSt->devFileName);
    return status;
}


/**
 * \fn     HAL_ST_Deinit
 * \brief  Deinitializes the HAL ST object
 *
 */
EMcpfRes HAL_ST_Deinit (handle_t  hHalSt)
{
    THalStObj  *pHalSt = (THalStObj *) hHalSt;
    McpU8   uMngEvt =  (McpU8) HalStEvent_Terminate; 

    /* If 'bIsBlockOnWrite' is TRUE --> there is no RX thread (working in Block on Write Mode) */
    if (pHalSt->bIsBlockOnWrite == MCP_TRUE)
		stPortDestroy (pHalSt);
    else
    {
	    /* Send destroy event to ST thread */
	    pHalSt->terminationFlag = MCP_TRUE;

	    /* Signal ST thread to terminate*/
	    if (write(pHalSt->pipeFd[1], (const void*) &uMngEvt, 1) == -1)
	    {
			MCPF_REPORT_ERROR (pHalSt->hMcpf, HAL_ST_MODULE_LOG,
								  ("%s: Pipe write failed, err=%u\n", __FUNCTION__, errno));
			return RES_ERROR;
	    }

	    MCPF_REPORT_DEBUG_CONTROL(pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("%s: waiting for completion of ST thread deletion ...\n", __FUNCTION__));
	    /* wait for completion of ST thread deletion */
	    waitForSem (pHalSt->hMcpf, &pHalSt->tSem);
	    MCPF_REPORT_DEBUG_CONTROL(pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("%s: thread deletion is completed\n", __FUNCTION__));
    }
    return RES_OK;
}


/**
 * \fn     HAL_ST_Read
 * \brief  Read data from OS ST device
 *
 */
EMcpfRes HAL_ST_Read 
(	
   const handle_t  hHalSt,
   McpU8*          pBuf,
   const McpU16  	 uLen,
   McpU16*         uReadLen
)
{
   THalStObj*  pHalSt  = (THalStObj*) hHalSt;
	McpS32		iReadNum = 0;
   McpS32		iBytesRead = 0;
	EMcpfRes   eRes = RES_ERROR;

	if (!pHalSt->terminationFlag)
   {
		iReadNum = MIN(uLen, pHalSt->iRxBufferedNum);
		if (iReadNum > 0)
		{
			/* Read received bytes from locally buffered data store */
			mcpf_mem_copy(pHalSt->hMcpf, pBuf, &pHalSt->cRxDataStore[pHalSt->iRxBufferRdIndx], 
										   iReadNum);
			iBytesRead = iReadNum;
			pHalSt->iRxBufferedNum -= iReadNum;
			pHalSt->iRxBufferRdIndx += iReadNum;
			eRes = RES_COMPLETE;
		}
		else
		{
			McpU8   uMngEvt =  (McpU8) HalStEvent_AwakeRxThread; 
			
			/* Nothing is available to read for now, prepare for the incoming data */
			eRes = RES_PENDING;
			pHalSt->iRxReadNum = uLen;	/* byte number requested to read  	   */
			pHalSt->pInData 	 = pBuf;    /* buffer to use for the incoming data */

			/* Signal ST thread to d oselect also on osPort */
	    	if (write(pHalSt->pipeFd[1], (const void*) &uMngEvt, 1) == -1)
	    	{
				MCPF_REPORT_ERROR (pHalSt->hMcpf, HAL_ST_MODULE_LOG,
								  ("%s: Pipe write failed, err=%u\n", __FUNCTION__, errno));
				return RES_ERROR;
	    	}
		}
	}
	else
	{
		eRes = RES_ERROR;
	}

	*uReadLen = (McpU16) iBytesRead;
	return eRes;
}

/**
 * \fn     HAL_ST_ReadResult
 * \brief  Return the read status and the number of received bytes
 *
 */
EMcpfRes HAL_ST_ReadResult (const handle_t  hHalSt, McpU16* pLen)
{
    THalStObj* pHalSt = (THalStObj*) hHalSt;
    *pLen = (McpU16) pHalSt->iRxRepNum;
    return RES_OK;
}


/**
 * \fn     HAL_ST_Write
 * \brief  Wrtite data to OS ST device
 *
 */
EMcpfRes HAL_ST_Write (const handle_t   hHalSt,
                           const McpU8  *pBuf,
                           const McpU16 len,
                           McpU16       *sentLen)
{
    THalStObj  *pHalSt = (THalStObj *) hHalSt;
    McpS32          iBytesSent ;
    McpS32          iBytesToSend = (McpS32) len;
    McpU8           uTxComplEvt =  (McpU8) HalStEvent_WriteComplInd; 
  
    for(;;)
    {
        iBytesSent = write (pHalSt->hOsPort,
                            pBuf,
                            iBytesToSend);

        if (iBytesSent == iBytesToSend)
        {
            /* write is done */
            break;
        }
        else if (iBytesSent < 0)
        {
            MCPF_REPORT_ERROR (pHalSt->hMcpf, HAL_ST_MODULE_LOG,
                                  ("%s: WriteFile failed, err=%u\n", __FUNCTION__, errno));

            *sentLen = 0;   /* consider in the case of error nothing is sent */
            return RES_ERROR;
        }
        else 
        {
            /* continue to send */
            pBuf += iBytesSent;
            iBytesToSend -= iBytesSent;
        }
    }
    *sentLen = (McpU16) len;

    /* If 'bIsBlockOnWrite' is TRUE --> there is no ST thread (working in Block on Write Mode) */
    if (pHalSt->bIsBlockOnWrite == MCP_TRUE)
    {
		 return RES_COMPLETE;
    }
    else
    {
	    /* Signal Tx completion to ST thread */
	    if (write(pHalSt->pipeFd[1], (const void*) &uTxComplEvt, 1) == -1)
	    {
			MCPF_REPORT_ERROR (pHalSt->hMcpf, HAL_ST_MODULE_LOG,
								  ("%s: Pipe write failed, err=%u\n", __FUNCTION__, errno));
			return RES_ERROR;
	    }

	    return RES_PENDING;
    }
}

/**
 * \fn     HAL_ST_Port_Reset
 * \brief  reset and optionally start HAL ST  hardware to
 * 
 * 
 */ 
EMcpfRes HAL_ST_Port_Reset (handle_t  *hHalSt, McpU8 *pBuf, McpU16 len, const McpU16 baudrate)
{
    THalStObj     *pHalSt = (THalStObj *) hHalSt;

    MCP_UNUSED_PARAMETER(pBuf);
    MCP_UNUSED_PARAMETER(len);

    pHalSt->tPortCfg.uBaudRate = (McpU32)baudrate;
    return stPortConfig(pHalSt);
}

/**
 * \fn     HAL_ST_RestartRead
 * \brief  Signals to start read operation
 *
 */
EMcpfRes HAL_ST_RestartRead (const handle_t  hHalSt)
{
    handle_t  lHalSt = hHalSt; /* reduce compiler warnings*/
    
    MCP_UNUSED_PARAMETER(lHalSt);
    /* Nothing to do, the read is not stopped */
    return RES_OK;
}

/**
 * \fn     HAL_ST_ResetRead
 * \brief  Cleans Rx buffer and starts a new read operation
 *
 */
EMcpfRes HAL_ST_ResetRead (const handle_t  hHalSt, McpU8 *pBuf, const McpU16 len)
{
    THalStObj  *pHalSt = (THalStObj *) hHalSt;
    EMcpfRes eRes;

    eRes = stPortRxReset (pHalSt, pBuf, len);

    return eRes;
}


/**
 * \fn     HAL_ST_Set
 * \brief  Set HAL ST object new configuration
 *
 */

EMcpfRes HAL_ST_Set (handle_t             hHalSt,
                         			const THalSt_PortCfg   *pConf)
{
    THalStObj  *pHalSt = (THalStObj *) hHalSt;

    pHalSt->tPortCfg.uBaudRate = pConf->uBaudRate;
    stPortConfig(pHalSt);

    return RES_OK;
}

/** 
 * \fn     HAL_ST_RegisterVSEvent
 * \brief  Register to receive a VS event (or DEFAULT for all events)
 * 	       NOTE: THIS IOCTL IS ONLY SUPPORTED BY THE TI_BT_DRV
 * 
 */ 
EMcpfRes HAL_ST_RegisterVSEvent (handle_t 			hHalSt, 
					    				    McpU32			uVSEvent)
{
	int     	  iRetCode;
	THalStObj  *pHalSt = (THalStObj *) hHalSt;
	
	iRetCode = ioctl (pHalSt->hOsPort, HALST_IOCTL_VS_EVENT, uVSEvent);

	if(iRetCode < 0)
		return RES_ERROR;
	else
		return RES_OK;
}

/** 
 * \fn     HAL_ST_SetWriteBlockMode
 * \brief  Enable/Disable Write Blocking Mode
 * 	       NOTE: THIS IOCTL IS ONLY SUPPORTED BY THE TI_BT_DRV
 * 
 */ 
EMcpfRes HAL_ST_SetWriteBlockMode (handle_t 			hHalSt, 
					    					  McpBool				bIsWriteBlock)
{
	int     	  iRetCode;
	THalStObj  *pHalSt = (THalStObj *) hHalSt;
	
	iRetCode = ioctl (pHalSt->hOsPort, HALST_IOCTL_WAIT_4_CMD_CMPLT, bIsWriteBlock);

	if (iRetCode < 0)
		return RES_ERROR;
	else
		return RES_OK;
}

/** 
 * \fn     HAL_ST_GetChipVersion
 * \brief  Get chip's version from the file which was written by the ST driver
 * 
 */ 
EMcpfRes HAL_ST_GetChipVersion (handle_t hHalSt,
                                THalSt_ChipVersion *chipVersion)
{
    THalStObj *pHalSt = (THalStObj *)hHalSt;
    FILE *fVersion;
    int iVerFull, iProjType, iVerMajor, iVerMinor;
    int iParamsReadNum;
    EMcpfRes eRes = RES_OK;

    MCPF_REPORT_INFORMATION (pHalSt->hMcpf,
                             HAL_ST_MODULE_LOG,
                             ("HAL_ST_GetChipVersion"));

    fVersion = fopen(HAL_ST_CHIP_VERSION_FILE_NAME, "r");

    if (NULL == fVersion)
    {
        /* Error opening the file */
        MCPF_REPORT_ERROR (pHalSt->hMcpf,
                           HAL_ST_MODULE_LOG,
                           ("Version file %s open failed! \n", HAL_ST_CHIP_VERSION_FILE_NAME));
        return RES_ERROR;
    }

    /* Read the chip's version */
    iParamsReadNum = fscanf(fVersion,
                            "%x %d.%d.%d",
                            &iVerFull,
                            &iProjType,
                            &iVerMajor,
                            &iVerMinor);

    if (iParamsReadNum < 4)
    {
        /* Error reading from the version file */
        MCPF_REPORT_ERROR (pHalSt->hMcpf,
                           HAL_ST_MODULE_LOG,
                           ("Error reading from the version file %s, read only %d params! \n",
                            HAL_ST_CHIP_VERSION_FILE_NAME,
                            iParamsReadNum));
        eRes = RES_ERROR;
    }

    if (0 == iVerFull)
    {
        /* Chip is not on */
        MCPF_REPORT_ERROR (pHalSt->hMcpf,
                           HAL_ST_MODULE_LOG,
                           ("Version full is 0 - chip is not on! \n"));
        eRes = RES_ERROR;
    }
    
    /* Print the version */
    MCPF_REPORT_INFORMATION (pHalSt->hMcpf,
                             HAL_ST_MODULE_LOG,
                             ("Chip's version %d.%d.%d, full version 0x%04x",
                              iProjType, iVerMajor, iVerMinor, iVerFull));

    /* Close the version file */
    fclose(fVersion);

    /* Copy version info */
    chipVersion->full = (McpU16)iVerFull;
    chipVersion->projectType = (McpU16)iProjType;
    chipVersion->major = (McpU16)iVerMajor;
    chipVersion->minor = (McpU16)iVerMinor;

    return eRes;
}

/**
 * \fn     halStDestroy
 * \brief  Destroy HAL ST Object
 *
 * Frees memory occupied by HAL ST Object
 *
 * \note
 * \param   pHalSt - pointer to HAL ST object
 * \return  void
 * \sa
 */
static void halStDestroy (THalStObj  * pHalSt)
{
    mcpf_mem_free (pHalSt->hMcpf, pHalSt);
}



/**
 * \fn     stPortDestroy
 * \brief  ST port de-initialization
 *
 * Stop and close OS ST port, free allocated memory for HAL ST object
 *
 * \note
 * \param   pHalSt  - pointer to HAL ST object
 * \return  Returns the status of operation: OK or Error
 * \sa      stPortDestroy
 */
static EMcpfRes stPortDestroy (THalStObj  *pHalSt)
{
    /*
     * Discard all characters from the output & input buffer.
     * We neednot terminate pending read or write operations as no async calls are used */
    tcflush (pHalSt->hOsPort, TCIOFLUSH);
    close (pHalSt->hOsPort);

	 close (pHalSt->pipeFd[ 0 ]);
	 close (pHalSt->pipeFd[ 1 ]);

    return RES_OK;
}

/**
 * \fn     stPortInit
 * \brief  ST port initialization
 *
 * Open and initiate Win OS ST device
 *
 * \note
 * \param   pHalSt  - pointer to HAL ST object
 * \return  Returns the status of operation: OK or Error
 * \sa      stPortInit
 */
static EMcpfRes stPortInit (THalStObj* pHalSt)
{
    pHalSt->hOsPort = open( pHalSt->devFileName, O_RDWR );
    if (pHalSt->hOsPort < NUMBER_ZERO)
    {
        /* Error opening the file */
        MCPF_REPORT_ERROR (pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("Port open failed, dev=%s! \n", pHalSt->devFileName));
        return RES_ERROR;
    }

	 if (pipe (pHalSt->pipeFd) != 0)
	 {
        MCPF_REPORT_ERROR (pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("Pipe create failed!\n"));
        return RES_ERROR;
	 }

    pHalSt->tPortCfg.uBaudRate = HAL_ST_SPEED_115200;    /* Set default baud rate */

    ALOGI("+++ %s: Port [%s] opened OK. Initial Baud Rate: [%d] +++\n", 
      __FUNCTION__, pHalSt->devFileName, pHalSt->tPortCfg.uBaudRate);

    return RES_OK;
}


/**
 * \fn     stPortConfig
 * \brief  ST port configuration
 *
 * Change ST port configuration parameters as baudrate and Xon/Xoff limits
 *
 * \note    ST port is to be initialized before the function call
 * \param   pHalSt  - pointer to HAL ST object
 * \return  Returns the status of operation: OK or Error
 * \sa      stPortConfig
 */
static EMcpfRes stPortConfig (THalStObj* pHalSt)
{
    struct termios  devCtrl;               /* data structure containing terminal information */
    McpU32 uFlagRate = stSpeedToOSbaudrate (pHalSt->hMcpf, pHalSt->tPortCfg.uBaudRate);

    /* Get and set the comm port state */
    tcgetattr( pHalSt->hOsPort, &devCtrl );

    cfsetospeed(&devCtrl, uFlagRate);
    cfsetispeed(&devCtrl, uFlagRate);

    if (tcsetattr(pHalSt->hOsPort, TCSANOW, &devCtrl))
    {
        MCPF_REPORT_ERROR (pHalSt->hMcpf, HAL_ST_MODULE_LOG,  ("stPortConfig set baudrate failed! \n"));
        return RES_ERROR;     
    }

    tcflush(pHalSt->hOsPort, TCIFLUSH);

    return RES_OK;
}

/**
 * \fn     stThreadFun
 * \brief  ST thread main function
 *
 * Thread function waits on and processes OS events (write complete, read ready and management)
 *
 * \note
 * \param   pHalSt  - pointer to HAL ST object
 * \return  N/A
 * \sa      stThreadFun
 */
static void* stThreadFun (void* pParam)
{
    THalStObj* pHalSt = (THalStObj*) pParam;
    fd_set  	  readFds;
    int     	  iRetCode;
    EHalStEvent event;

    /* This thread would continue running till termination flag is set to TRUE */
    pHalSt->terminationFlag = MCP_FALSE;

    /* signal semaphore the thread is created */
    signalSem (pHalSt->hMcpf,  &pHalSt->tSem);

    while (!pHalSt->terminationFlag)
    {
        /* initialize file descr. bit sets */
        FD_ZERO (&readFds);
		  FD_SET (pHalSt->hOsPort,   &readFds);

        FD_SET (pHalSt->pipeFd[0], &readFds);

		  iRetCode = select (FD_SETSIZE, &readFds, NULL, NULL, NULL);
		  if (iRetCode > 0)
        {
			   /* Determine which file descriptor is ready for read */
			   if (FD_ISSET(pHalSt->hOsPort, &readFds))
			   {
				McpS32	iReadNum, iBytesRead;

				if (pHalSt->iRxBufferedNum == 0)
				{
					iRetCode = ioctl (pHalSt->hOsPort, FIONREAD, &pHalSt->iRxAvailNum);
					if (pHalSt->iRxAvailNum > 0)
					{
						/* Copy the as much as possible and store for future reads */
						pHalSt->iRxBufferedNum = MIN(MAX_RX_DATA_SIZE, pHalSt->iRxAvailNum);
						pHalSt->iRxBufferRdIndx = 0;

						/* Read received bytes from ST device */
						iBytesRead = read (pHalSt->hOsPort, 
										   &pHalSt->cRxDataStore[pHalSt->iRxBufferRdIndx], 
										   pHalSt->iRxBufferedNum);

						if (iBytesRead >= 0)
						{
							event = HalStEvent_ReadReadylInd;
							pHalSt->iRxAvailNum -= iBytesRead;
							pHalSt->iRxBufferedNum = iBytesRead;
						}
						else
						{
							MCPF_REPORT_ERROR (pHalSt->hMcpf, HAL_ST_MODULE_LOG,
											   ("%s: Read failed, err=%u\n", __FUNCTION__, errno));
							event = HalStEvent_Error;
						}
					}
					else
					{
						MCPF_REPORT_ERROR (pHalSt->hMcpf, HAL_ST_MODULE_LOG,
										   ("%s: uexpected num zero\n", __FUNCTION__));
						event = HalStEvent_Error;
					}
				}
				else
				{
					event = HalStEvent_ReadReadylInd;
				}

				if (event != HalStEvent_Error)
				{
					/* read requested number of bytes */
					iReadNum = MIN(pHalSt->iRxReadNum, pHalSt->iRxBufferedNum);

					/* Read received bytes from locally buffered data store */
					mcpf_mem_copy(pHalSt->hMcpf, pHalSt->pInData, &pHalSt->cRxDataStore[pHalSt->iRxBufferRdIndx], 
												   iReadNum);

					pHalSt->iRxBufferedNum -= iReadNum;
					pHalSt->iRxBufferRdIndx += iReadNum;
					pHalSt->iRxRepNum = iReadNum;
					pHalSt->iRxReadNum = 0;

				}

				pHalSt->fEventHandlerCb (pHalSt->hHandleCb, event);
			}

			if (FD_ISSET(pHalSt->pipeFd[0], &readFds))
			{
				McpU8	uEvt;

                /* Read management event */
				iRetCode = read (pHalSt->pipeFd[0], (void*)&uEvt, 1);
				if (iRetCode > 0)
				{
                    switch (uEvt)
                    {
                    case HalStEvent_WriteComplInd:
                        pHalSt->fEventHandlerCb (pHalSt->hHandleCb, HalStEvent_WriteComplInd);
                        break;

                    case HalStEvent_Terminate:
                        break;

			           case HalStEvent_AwakeRxThread:
			               break;
				
                    default:
                        break;
                    }
				}
				else
				{
					MCPF_REPORT_ERROR(pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("%s: pipe read error\n", __FUNCTION__));
				}
			}
        }
		else if (iRetCode == 0)
		{
			MCPF_REPORT_ERROR(pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("%s: select timeout\n", __FUNCTION__));
		}
		else
		{
			MCPF_REPORT_ERROR(pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("%s: select failed\n", __FUNCTION__));
			return NULL;

		}
    }/* while */

    /* Termination flag is set. Exit from this context */
    stPortDestroy (pHalSt);
    signalSem (pHalSt->hMcpf, &pHalSt->tSem);  /* signal semaphore the thread is about to exit */

    return NULL;
}



/**
 * \fn     signalSem
 * \brief  Signal semaphore
 *
 * Release counting semaphore and increments count by one unit
 *
 * \note
 * \param   hMcpf - OS framework handle
 * \param   hSem  - OS semphore handle
 * \return  void
 * \sa
 */
static void signalSem (handle_t hMcpf, SEM_HANDLE hSem)
{
    int result;

    /* Returns 0 on success */
    result = sem_post(hSem);

    if (result)
    {
        MCPF_REPORT_ERROR (hMcpf, HAL_ST_MODULE_LOG, ("%s: sem_post failed, handle=%p!\n", __FUNCTION__, hSem));
    }
}


/**
 * \fn     waitForSem
 * \brief  Waits for semaphore
 *
 * Wait for counting semaphore forever
 *
 * \note
 * \param   hMcpf - OS framework handle
 * \param   hSem  - OS semphore handle
 * \return  void
 * \sa
 */
static void waitForSem (handle_t hMcpf, SEM_HANDLE hSem)
{
    int result;

    /* wait infinitely for the semaphore.
     * sem_wait() is used to acquire the semaphore access.
     * This function suspends the calling thread until the semaphore has a non-zero count.
     * It then atomically decreases the semaphore count */
    result = sem_wait (hSem);

    /* If the call was unsuccessful, the state of the semaphore is unchanged,
     * and the function returns a value of -1 and sets errno to indicate the error. */
    if (result == -1)
    {
        MCPF_REPORT_ERROR (hMcpf, HAL_ST_MODULE_LOG,
            ("%s: sem_wait failed, result = %d \n", __FUNCTION__, result));
    }
}

#if 0 /* tbd remove */
/**
 * \fn     stPortRxStart
 * \brief  ST port receive start
 *
 * Resets and start ST receive operation
 *
 * \note
 * \param   pHalSt  - pointer to HAL ST object
 * \return  Returns the status of operation: OK or Error
 * \sa      HAL_ST_Init
 */
static EMcpfRes stPortRxStart (THalStObj *pHalSt, McpU8 *pBuf, McpU16 len)
{
    /* Activate DTR line */
    MCP_UNUSED_PARAMETER(pHalSt);
    MCP_UNUSED_PARAMETER(pBuf);
    MCP_UNUSED_PARAMETER(len);
    return RES_OK;
}
#endif

/**
 * \fn     stPortRxReset
 * \brief  ST port receive reset
 *
 * Purges ST device input buffer and triggers the new receive operation
 *
 * \note
 * \param   pHalSt  - pointer to HAL ST object
 * \return  Returns the status of operation: OK or Error
 * \sa      stPortRxStart
 */
static EMcpfRes stPortRxReset (THalStObj *pHalSt, McpU8 *pBuf, McpU16 len)
{
    McpS32	iReadNum, iRetCode;
    
    MCP_UNUSED_PARAMETER(len);

	/* 
	 * Read and discard all available bytes from ST port input buffer.
	 * ReadFile returns 0 when fails (no more data to read or error)
	 */
	pHalSt->iRxBufferedNum = 0;
	pHalSt->iRxBufferRdIndx = 0;

    iRetCode = ioctl (pHalSt->hOsPort, FIONREAD, &iReadNum);

    MCPF_REPORT_DEBUG_CONTROL(pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("%s: Rx queue  res=%d avail=%d\n", __FUNCTION__, iRetCode, iReadNum));

    /* read available number of bytes from ST input queue */
    iRetCode = read (pHalSt->hOsPort, pBuf, iReadNum);
    if (iRetCode < 0)
    {
        MCPF_REPORT_ERROR (pHalSt->hMcpf, HAL_ST_MODULE_LOG,
                           ("%s: Read failed, err=%u\n", __FUNCTION__, errno));
		return RES_ERROR;
    }

	return RES_OK;
}


/**
 * \fn     stSpeedToOSbaudrate
 * \brief  Convert ST speed to baudrate
 *
 * Convert ST configuration speed to Win OS baudrate
 *
 * \note
 * \param   speed  - HAL ST speed value
 * \return  baudrate
 * \sa      stSpeedToOSbaudrate
 */
static McpU32 stSpeedToOSbaudrate(handle_t hMcpf, ThalStSpeed  speed)
{
    McpU32 baudrate;

    switch (speed) {
    case HAL_ST_SPEED_9600:
        baudrate = B9600;
        break;
    case HAL_ST_SPEED_38400:
        baudrate = B38400;
        break;
    case HAL_ST_SPEED_57600:
        baudrate = B57600;
        break;
    case HAL_ST_SPEED_115200:
        baudrate = B115200;
        break;
#if 0
    case HAL_ST_SPEED_128000:
        baudrate = B128000;
        break;
#endif
    case HAL_ST_SPEED_230400:
        baudrate = B230400;
        break;
#if 0
    case HAL_ST_SPEED_256000:
        baudrate = B256000;
        break;
#endif
    case HAL_ST_SPEED_460800:
        baudrate = B460800;
        break;
    case HAL_ST_SPEED_921600:
        baudrate = B921600;
        break;
    case HAL_ST_SPEED_1000000:
        baudrate = B1000000;
        break;
    case HAL_ST_SPEED_2000000:
        baudrate = B2000000;
        break;
    case HAL_ST_SPEED_3000000:
        baudrate = B3000000;
        break;
    default:
        MCPF_REPORT_ERROR (hMcpf, HAL_ST_MODULE_LOG, ("baud rate = %u is not supported!\n", speed));
        baudrate = speed * 1000;
    }
    return baudrate;
}
void resetDataPtr(handle_t hHalSt, McpU8 *pBuf)
{
    THalStObj  *pHalSt = (THalStObj *) hHalSt;
    pHalSt->pInData = pBuf;
}

