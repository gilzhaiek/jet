/******************************************************************************\
##                                                                            *
## Unpublished Proprietary and Confidential Information of Texas Instruments  *
## Israel Ltd. Do Not Disclose.                                               *
## Copyright 2008 Texas Instruments Israel Ltd.                               *
## All rights reserved. All unpublished rights reserved.                      *
##                                                                            *
## No part of this work may be used or reproduced in any form or by any       *
## means, or stored in a database or retrieval system, without prior written  *
## permission of Texas Instruments Israel Ltd. or its parent company Texas    *
## Instruments Incorporated.                                                  *
## Use of this work is subject to a license from Texas Instruments Israel     *
## Ltd. or its parent company Texas Instruments Incorporated.                 *
##                                                                            *
## This work contains Texas Instruments Israel Ltd. confidential and          *
## proprietary information which is protected by copyright, trade secret,     *
## trademark and other intellectual property rights.                          *
##                                                                            *
## The United States, Israel  and other countries maintain controls on the    *
## export and/or import of cryptographic items and technology. Unless prior   *
## authorization is obtained from the U.S. Department of Commerce and the     *
## Israeli Government, you shall not export, reexport, or release, directly   *
## or indirectly, any technology, software, or software source code received  *
## from Texas Instruments Incorporated (TI) or Texas Instruments Israel,      *
## or export, directly or indirectly, any direct product of such technology,  *
## software, or software source code to any destination or country to which   *
## the export, reexport or release of the technology, software, software      *
## source code, or direct product is prohibited by the EAR. The subject items *
## are classified as encryption items under Part 740.17 of the Commerce       *
## Control List (“CCL”). The assurances provided for herein are furnished in  *
## compliance with the specific encryption controls set forth in Part 740.17  *
## of the EAR -Encryption Commodities and Software (ENC).                     *
##                                                                            *
## NOTE: THE TRANSFER OF THE TECHNICAL INFORMATION IS BEING MADE UNDER AN     *
## EXPORT LICENSE ISSUED BY THE ISRAELI GOVERNMENT AND THE APPLICABLE EXPORT  *
## LICENSE DOES NOT ALLOW THE TECHNICAL INFORMATION TO BE USED FOR THE        *
## MODIFICATION OF THE BT ENCRYPTION OR THE DEVELOPMENT OF ANY NEW ENCRYPTION.*
## UNDER THE ISRAELI GOVERNMENT'S EXPORT LICENSE, THE INFORMATION CAN BE USED *
## FOR THE INTERNAL DESIGN AND MANUFACTURE OF TI PRODUCTS THAT WILL CONTAIN   *
## THE BT IC.                                                                 *
##                                                                            *
\******************************************************************************/

/** \file   mcp_hal_st.c 
 *  \brief  Win OS Adaptation Layer for ST Interface implementation
 * 
 *  \see    mcp_hal_st.h
 */

#include <windows.h>
#include <stdio.h>
#include "mcpf_mem.h"
#include "mcpf_report.h"
#include "mcp_hal_os.h"
#include "bmtrace.h"
#include "mcp_hal_string.h"
#include "mcp_hal_st.h"

/************************************************************************
 * Defines
 ************************************************************************/

#define HALST_MODULE_NAME_MAX_SIZE		10

#define MAX_FILENAME_SIZE				20

/************************************************************************
 * Types
 ************************************************************************/

/* HAL ST Events */
typedef enum
{
	Event_WriteCompl,
	Event_ReadReady,
	Event_Mng,
	Event_MaxNum

} EStEvent;

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
	handle_t       hMcpf;             

    HalST_EventHandlerCb  	fEventHandlerCb;
	handle_t 			hHandleCb;
    THalSt_PortCfg         		tPortCfg;
    char					cDrvName[HALST_MODULE_NAME_MAX_SIZE];
    char 					devFileName[MAX_FILENAME_SIZE];

	OVERLAPPED			aOsEvent[Event_MaxNum];
	HANDLE				aOsEventHandle[Event_MaxNum];
	HANDLE       		hOsPort;
	HANDLE				hOsThread;
	DWORD				uOsThreadID;
	HANDLE				hOsSem;
	EHalStEvent			eMngEvt;

    McpBool				bIsBlockOnWrite; /* Whether the client should work with 'Block on Write' Mode */
#ifdef TRAN_DBG
    THalStDebugCount 		dbgCount;
#endif

} THalStObj;

#ifdef TRAN_DBG
THalStObj * g_pHalStObj;
#endif


/************************************************************************
 * Internal functions prototypes
 ************************************************************************/

static EMcpfRes stPortInit (THalStObj  *pHalSt);
static EMcpfRes stPortConfig (THalStObj  *pHalSt);
static DWORD WINAPI stThreadFun (LPVOID pParam);
static EMcpfRes stPortDestroy (THalStObj    *pHalSt);
static EMcpfRes stPortRxReset (THalStObj *pHalSt, McpU8 *pBuf, McpU16 len);
static EMcpfRes stPortRxStart (THalStObj *pHalSt, McpU8 *pBuf, McpU16 len);
static DWORD stSpeedToOSbaudrate(ThalStSpeed  speed);

static void signalSem  (handle_t hMcpf, HANDLE hSem);
static void waitForSem (handle_t hMcpf, HANDLE hSem);
static void halStDestroy (THalStObj  * pHalSt);
 

void rxTest (THalStObj  *pHalSt);

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

handle_t HAL_ST_Create (const handle_t hMcpf, const char *pModuleName)
{
    THalStObj  * pHalSt;
	int	i;

    pHalSt = mcpf_mem_alloc (hMcpf, sizeof(THalStObj));
    if (pHalSt == NULL)
	{
		return NULL;
	}

#ifdef TRAN_DBG
    g_pHalStObj = pHalSt;
#endif

    mcpf_mem_zero (hMcpf, pHalSt, sizeof(THalStObj));

    pHalSt->hMcpf = hMcpf;

	if (strlen(pModuleName) < HALST_MODULE_NAME_MAX_SIZE)
	{
		strcpy (pHalSt->cDrvName, pModuleName);
	}
	else
	{
		/* driver name string is too long, truncate it */
		mcpf_mem_copy (hMcpf, pHalSt->cDrvName, (McpU8 *) pModuleName, HALST_MODULE_NAME_MAX_SIZE - 1);
		pHalSt->cDrvName[HALST_MODULE_NAME_MAX_SIZE-1] = 0; /* line termination */
	}

	for (i=0; i < Event_MaxNum; i++)
	{
	   pHalSt->aOsEvent[i].hEvent = CreateEvent (NULL, 	/* event security attrib */
												   TRUE, 	/* manual reset  */
												   FALSE,  	/* initial state */
												   NULL		/* event name string */);
	   if (pHalSt->aOsEvent[i].hEvent == NULL)
	   {
		   MCPF_REPORT_ERROR (pHalSt->hMcpf, HAL_ST_MODULE_LOG,
			   ("%s: OS event creation failed!\n", __FUNCTION__));
        halStDestroy (pHalSt);
		   return NULL;		   
	   }
	   pHalSt->aOsEventHandle[i] = pHalSt->aOsEvent[i].hEvent;
	}

	pHalSt->hOsSem = CreateSemaphore (NULL,  		/* default security attributes 	*/
										0,  	 	/* initial count 				*/
										1,  	 	/* maximum count 				*/
										NULL);    	/* no name for semaphore 		*/

    if (pHalSt->hOsSem == NULL) 
    {
		MCPF_REPORT_ERROR (pHalSt->hMcpf, HAL_ST_MODULE_LOG,
			("%s: CreateSemaphore failed!\n", __FUNCTION__));
		halStDestroy (pHalSt);
		return NULL;
    }

	return ((handle_t) pHalSt);
}


/** 
 * \fn     HAL_ST_Init
 * \brief  HAL ST object initialization
 *
 * Initiate state of HAL ST object
 * 
 */ 
EMcpfRes HAL_ST_Init ( 	handle_t                    			hHalSt,
                      			const HalST_EventHandlerCb 	fEventHandlerCb,
                          			const handle_t            			hHandleCb,
                          			const THalSt_PortCfg          		*pConf,
                          			const char					*pDevName,
                          			McpU8                     			*pBuf,
                          			const McpU16              			len,
                          			McpBool						bIsBlockOnWrite)
{
    THalStObj  *pHalSt = (THalStObj *) hHalSt;
	EMcpfRes	  status;   

	pHalSt->fEventHandlerCb = fEventHandlerCb;
	pHalSt->hHandleCb = hHandleCb;
    pHalSt->bIsBlockOnWrite = bIsBlockOnWrite;
    if(pConf)
		mcpf_mem_copy(pHalSt->hMcpf, &pHalSt->tPortCfg, (void *)pConf, sizeof(THalSt_PortCfg));

    if(pDevName)
		mcpf_mem_copy(pHalSt->hMcpf, pHalSt->devFileName, (void *)pDevName, 
						MCP_HAL_STRING_StrLen(pDevName));
    else
    {
    		MCPF_REPORT_ERROR(pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("HAL_ST_Init: Missing device Name to open\n") );
		return RES_ERROR;
    }

    status = stPortInit (pHalSt);

	if (status != RES_OK)
	{
		if (pHalSt->hOsPort) CloseHandle(pHalSt->hOsPort);
        halStDestroy (pHalSt);
		return RES_ERROR;
	}

    MCPF_REPORT_DEBUG_CONTROL(pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("HAL_ST_Init: create thread, hHalSt=%p \n", hHalSt));

	pHalSt->hOsThread = CreateThread((LPSECURITY_ATTRIBUTES)NULL,  /* security attrib    */
									   0,                            /* default stack size */
									   (LPTHREAD_START_ROUTINE) stThreadFun,
									   (LPVOID) hHalSt,            /* thread function parameter */
									   0,                            /* creation flags */
									   &pHalSt->uOsThreadID		 /* thread id to return */);

	if (pHalSt->hOsThread == NULL)
	{
		if (pHalSt->hOsPort) CloseHandle(pHalSt->hOsPort);
	          halStDestroy (pHalSt);
		return RES_ERROR;
	}

	    MCPF_REPORT_DEBUG_CONTROL(pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("HAL_ST_Init: waiting for ST thread initialization completion ...\n"));
	waitForSem (pHalSt->hMcpf, pHalSt->hOsSem);		/* wait for completion of thread initialization */
	    MCPF_REPORT_DEBUG_CONTROL(pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("HAL_ST_Init: ST thread initialization is completed\n"));

	stPortRxStart(pHalSt, pBuf, len);

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

	/* Send destroy event to ST thread */
	pHalSt->eMngEvt = HalStEvent_Terminate;
	SetEvent (pHalSt->aOsEvent[Event_Mng].hEvent);

	MCPF_REPORT_DEBUG_CONTROL(pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("hal-st-Deinit: waiting for completion of thread deletion ...\n"));

	waitForSem (pHalSt->hMcpf, pHalSt->hOsSem);		/* wait for completion of thread deletion */

	MCPF_REPORT_DEBUG_CONTROL(pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("HAL_ST_Deinit: thread deletion is completed\n"));

	return RES_OK;
}


/** 
 * \fn     HAL_ST_Destroy 
 * \brief  Destroy the HAL ST object
 * 
 */ 
EMcpfRes HAL_ST_Destroy (handle_t	hHalSt)
{
	THalStObj  *pHalSt = (THalStObj *) hHalSt;

	CloseHandle (pHalSt->hOsSem);
	mcpf_mem_free (pHalSt->hMcpf, pHalSt);

	return RES_OK;
}


/** 
 * \fn     HAL_ST_Write
 * \brief  Write data to OS ST device
 *  
 */ 
EMcpfRes HAL_ST_Write (const handle_t 	hHalSt, 
						   const McpU8	*pBuf, 
						   const McpU16	len,  
						   McpU16		*sentLen)
{
	THalStObj  *pHalSt = (THalStObj *) hHalSt;
	BOOL 	result;
	DWORD	wLen;

	MCPF_REPORT_DEBUG_TX(pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("HAL_ST_Write: pBuf=%x len=%u\n", pBuf, len));

	result = WriteFile (pHalSt->hOsPort, 
						pBuf, 
						(DWORD) len, 
						&wLen, 
						&pHalSt->aOsEvent[Event_WriteCompl]);

	*sentLen = (McpU16) wLen;
	if (!result)
	{
		DWORD err = GetLastError();
		if (err == ERROR_IO_PENDING)
		{
			*sentLen = len;				/* In Tx async mode completion indication is   
										 * triggered for entire buffer 
										 */
			return RES_PENDING;
		}
		else
		{
			MCPF_REPORT_ERROR (pHalSt->hMcpf, HAL_ST_MODULE_LOG,
							  ("%s: WriteFile failed, err=%u\n", __FUNCTION__, err));
			*sentLen = 0;	/* consider in the case of error nothing is sent */
			return RES_ERROR; 
		}
	}
	return RES_COMPLETE;
}


/** 
 * \fn     HAL_ST_Read
 * \brief  Read data from OS ST device
 * 
 */ 
EMcpfRes HAL_ST_Read (const handle_t  	hHalSt, 
						  McpU8			*pBuf, 
						  const McpU16	len,  
						  McpU16 		*readLen)
{
	THalStObj  *pHalSt = (THalStObj *) hHalSt;
	BOOL 	result;
	DWORD	rLen;

	result = ReadFile (pHalSt->hOsPort, 
					   pBuf, 
					   (DWORD) len, 
					   &rLen, 
					   &pHalSt->aOsEvent[Event_ReadReady]);

	
	*readLen = (McpU16) rLen;
	if (!result)
	{
		DWORD err = GetLastError();
		if (err == ERROR_IO_PENDING)
		{
			return RES_PENDING;
		}
		else
		{
			MCPF_REPORT_ERROR (pHalSt->hMcpf, HAL_ST_MODULE_LOG,
							  ("%s: ReadFile failed, err=%u\n", __FUNCTION__, err));
			return RES_ERROR; 
		}
	}
	return RES_COMPLETE;
}


/** 
 * \fn     HAL_ST_ReadResult
 * \brief  Return the read status and the number of received bytes
 * 
 */ 
EMcpfRes HAL_ST_ReadResult (const handle_t  hHalSt, McpU16 *pLen)
{
	THalStObj  *pHalSt = (THalStObj *) hHalSt;
	BOOL  result;
	DWORD len;

	result = GetOverlappedResult (pHalSt->hOsPort,
								  &pHalSt->aOsEvent[Event_ReadReady], 
								  &len, FALSE);
	*pLen = (McpU16) len;

	if (!result)
	{
		*pLen = 0;
	}
	return ((result) ? RES_OK : RES_ERROR);
}


/** 
 * \fn     HAL_ST_RestartRead
 * \brief  Signals to start read operation
 * 
 */ 
EMcpfRes HAL_ST_RestartRead (const handle_t  hHalSt)
{
	THalStObj  *pHalSt = (THalStObj *) hHalSt;

	SetEvent (pHalSt->aOsEventHandle[Event_ReadReady]);

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

	stPortRxReset (pHalSt, pBuf, len);

	return RES_OK;
}

/** 
 * \fn     HAL_ST_Set
 * \brief  Set HAL ST object new configuration
 * 
 */ 

EMcpfRes HAL_ST_Set (handle_t 			hHalSt, 
						 const THalSt_PortCfg 	*pConf)
{
	THalStObj  *pHalSt = (THalStObj *) hHalSt;
	
	pHalSt->tPortCfg.uBaudRate = pConf->uBaudRate;

	/* Config ST */
	stPortConfig(pHalSt);
	
	return RES_OK;
}

/** 
 * \fn     HAL_ST_Port_Reset
 * \brief  reset and optionally start HAL ST hardware to
 * 
 * 
 */ 
EMcpfRes HAL_ST_Port_Reset (handle_t  *hHalSt, McpU8 *pBuf, McpU16 len, const McpU16 baudrate)
{
    THalStObj     *pHalSt = (THalStObj *)hHalSt;
    COMMTIMEOUTS	comTimeout;
    DCB        		devCtrl;
	BOOL			res;
    EMcpfRes        status = RES_ERROR;
    static          portRxStart = MCP_FALSE;
    
    /* Sets the timeout parameters for all read and write operations  */
	GetCommTimeouts(pHalSt->hOsPort, &comTimeout);
	comTimeout.ReadIntervalTimeout 			= MAXWORD;
	comTimeout.WriteTotalTimeoutConstant 	= 0;
	comTimeout.ReadTotalTimeoutMultiplier 	= 0;
	comTimeout.ReadTotalTimeoutConstant 	= 0;
	comTimeout.WriteTotalTimeoutMultiplier 	= 0;
	 
	SetCommTimeouts(pHalSt->hOsPort, &comTimeout);

	/* Set device control: baudrate, parity & stop bits, flow control etc. options */
	devCtrl.DCBlength = sizeof(DCB);
	GetCommState(pHalSt->hOsPort, &devCtrl);

    pHalSt->tPortCfg.uBaudRate = baudrate;
	devCtrl.BaudRate = stSpeedToOSbaudrate ((ThalStSpeed) pHalSt->tPortCfg.uBaudRate);
    devCtrl.ByteSize = 8;
    devCtrl.Parity 	 = NOPARITY;
    devCtrl.StopBits = ONESTOPBIT;

	devCtrl.fDtrControl  = DTR_CONTROL_ENABLE;
	//devCtrl.fRtsControl  = RTS_CONTROL_DISABLE;
	devCtrl.fRtsControl  = RTS_CONTROL_ENABLE;
	devCtrl.fOutxCtsFlow = FALSE	;
	// devCtrl.fBinary 	 = FALSE;
    //devCtrl.fParity 	 = FALSE;

	res = SetCommState (pHalSt->hOsPort, &devCtrl);
   if (res)
	{
       if (portRxStart == MCP_FALSE)
       {
            stPortRxStart (pHalSt, pBuf, len);
            portRxStart = MCP_TRUE;
       }
		status = RES_OK;
	}
	else
	{
		MCPF_REPORT_ERROR (pHalSt->hMcpf, HAL_ST_MODULE_LOG,
						  ("%s: Com Port SetCommState failed!\n", __FUNCTION__));
	/*	CloseHandle(pHalSt->hOsPort); 
		pHalSt->hOsPort = NULL;
     tbd */
		status = RES_ERROR;
	}

    return status;
}

/** 
 * \fn     HAL_ST_RegisterVSEvent
 * \brief  This is not applicable for Windows
 * 
 */ 
EMcpfRes HAL_ST_RegisterVSEvent (handle_t	hHalSt, 
					    		 McpU32		uVSEvent)
{
	MCP_UNUSED_PARAMETER(hHalSt);
	MCP_UNUSED_PARAMETER(uVSEvent);
	return RES_OK;
}

/** 
 * \fn     HAL_ST_SetWriteBlockMode
 * \brief  This is not applicable for Windows
 * 
 */ 
EMcpfRes HAL_ST_SetWriteBlockMode (handle_t hHalSt, 
					    		   McpBool	bIsWriteBlock)
{
	MCP_UNUSED_PARAMETER(hHalSt);
	MCP_UNUSED_PARAMETER(bIsWriteBlock);
	return RES_OK;
}

/************************************************************************
 * Internal functions Implementation
 ************************************************************************/

/** 
 * \fn     stPortInit
 * \brief  ST port initialization
 * 
 * Open and initiate Win OS ST device
 * 
 * \note
 * \param	pHalSt  - pointer to HAL ST object
 * \return 	Returns the status of operation: OK or Error
 * \sa     	HAL_ST_Init
 */ 
static EMcpfRes stPortInit (THalStObj  *pHalSt)
{
    char			comFileName[10];

    wsprintf (comFileName, "\\\\.\\COM%d", pHalSt->tPortCfg.portNum);

	pHalSt->hOsPort = CreateFile (comFileName, 
									GENERIC_READ | GENERIC_WRITE,
									0,  	/* share mode */
									NULL,   /* security attrib */
									OPEN_EXISTING,
									FILE_ATTRIBUTE_NORMAL | FILE_FLAG_OVERLAPPED,
									NULL	/* template file */);

	if (pHalSt->hOsPort == INVALID_HANDLE_VALUE)
	{
		MCPF_REPORT_ERROR (pHalSt->hMcpf, HAL_ST_MODULE_LOG,
						  ("%s: Com Port CreateFile failed, file=%s!\n", 
						   __FUNCTION__, comFileName));
		return RES_ERROR;
	}
    return RES_OK;
}

/** 
 * \fn     stPortConfig
 * \brief  ST port configuration
 * 
 * Change ST port configuration parameters as baudrate and Xon/Xoff limits 
 * 
 * \note    ST port is to be initialized before the function call
 * \param	pHalSt  - pointer to HAL ST object
 * \return 	Returns the status of operation: OK or Error
 * \sa     	stPortInit
 */ 
static EMcpfRes stPortConfig (THalStObj  *pHalSt)
{
    DCB   devCtrl;
	BOOL  res;

	devCtrl.DCBlength = sizeof(DCB);
	GetCommState(pHalSt->hOsPort, &devCtrl);

	/* Set device control: baudrate and  Xon/Xoff limits */
	devCtrl.BaudRate = stSpeedToOSbaudrate ((ThalStSpeed) pHalSt->tPortCfg.uBaudRate);

	res = SetCommState (pHalSt->hOsPort, &devCtrl);

	if (res)
	{
		return RES_OK;
	}
	else
	{
		MCPF_REPORT_ERROR (pHalSt->hMcpf, HAL_ST_MODULE_LOG,
						  ("%s: Com Port SetCommState failed!\n", __FUNCTION__));
		CloseHandle(pHalSt->hOsPort);
		pHalSt->hOsPort = NULL;
		return RES_ERROR;
	}
}


/** 
 * \fn     stThreadFun
 * \brief  ST thread main function
 * 
 * Thread function waits on and processes OS events (write complete, read ready and management)
 * 
 * \note
 * \param	pHalSt  - pointer to HAL ST object
 * \return 	N/A
 * \sa     	HAL_ST_Init
 */ 
static DWORD WINAPI stThreadFun (LPVOID pParam)
{
	THalStObj		*pHalSt = (THalStObj *) pParam;
	DWORD 			objectIndex;
	McpBool			doLoop = TRUE;
	EHalStEvent		eEvent;

	CL_TRACE_TASK_DEF();

    MCP_HAL_LOG_SetThreadName("ST");

	MCPF_REPORT_DEBUG_CONTROL(pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("stThreadFun started, pHalSt=%p\n", pHalSt));

	signalSem (pHalSt->hMcpf, pHalSt->hOsSem);	/* signal semaphore the thread is created */

    while (doLoop) {

		MCPF_REPORT_DEBUG_CONTROL(pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("stThreadFun: waiting for event ...\n"));

        /* Wake up every now and then just in case a Deinit is requested */
        objectIndex = WaitForMultipleObjects (Event_MaxNum, 
											  pHalSt->aOsEventHandle, 
											  FALSE,		/* any one of the objects is signaled */ 
											  INFINITE );
		CL_TRACE_TASK_START();

		MCPF_REPORT_DEBUG_CONTROL(pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("stThreadFun: event: objectIndex=%x\n", objectIndex));

		switch (objectIndex)
		{
		case WAIT_OBJECT_0:
			/* Write complete event */

			MCPF_REPORT_DEBUG_TX(pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("stThreadFun: WriteCompl event\n"));

			ResetEvent (pHalSt->aOsEventHandle[Event_WriteCompl]);
			eEvent = HalStEvent_WriteComplInd;
			pHalSt->fEventHandlerCb (pHalSt->hHandleCb, eEvent);
			break;

		case (WAIT_OBJECT_0+1):
			/* Read ready event */
			MCPF_REPORT_DEBUG_RX(pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("stThreadFun: ReadReady event\n"));

			ResetEvent (pHalSt->aOsEventHandle[Event_ReadReady]);
			eEvent = HalStEvent_ReadReadylInd;
			pHalSt->fEventHandlerCb (pHalSt->hHandleCb, eEvent);

			break;

		case (WAIT_OBJECT_0+2):
			/* Management event */

			MCPF_REPORT_DEBUG_CONTROL(pHalSt->hMcpf, HAL_ST_MODULE_LOG, 
										("stThreadFun: Management event=%d\n", pHalSt->eMngEvt));

			ResetEvent (pHalSt->aOsEventHandle[Event_Mng]);

			switch (pHalSt->eMngEvt)
			{
			case HalStEvent_AwakeRxThread:
				break;

			case HalStEvent_Terminate:
				/* Exit loop and thread */
				doLoop = FALSE;
				break;

			default:
				MCPF_REPORT_ERROR (pHalSt->hMcpf, HAL_ST_MODULE_LOG,
								  ("%s: Unknown management event received Id=%u!\n", 
								   __FUNCTION__, pHalSt->eMngEvt));
				break;
			}
			break;

		default:
			MCPF_REPORT_ERROR (pHalSt->hMcpf, HAL_ST_MODULE_LOG,
							  ("%s: Unknown event received objId=%u!\n", 
							   __FUNCTION__, objectIndex));
			break;
		}

		CL_TRACE_TASK_END("stThreadFun", CL_TRACE_CONTEXT_TASK, pHalSt->cDrvName, "HAL");
	} /* while */

	MCPF_REPORT_DEBUG_CONTROL(pHalSt->hMcpf, HAL_ST_MODULE_LOG, ("stThreadFun: exit thread\n"));

	stPortDestroy (pHalSt);

	signalSem (pHalSt->hMcpf, pHalSt->hOsSem);	/* signal semaphore the thread is about to exit */
	
	return TRUE;
}

/** 
 * \fn     stPortDestroy
 * \brief  ST port de-initialization
 * 
 * Stop and close OS ST port, free allocated memory for HAL ST object
 * 
 * \note
 * \param	pHalSt  - pointer to HAL ST object
 * \return 	Returns the status of operation: OK or Error
 * \sa     	stThreadFun
 */ 
static EMcpfRes stPortDestroy (THalStObj  *pHalSt)
{
	int i;

    /* Deactivate DTR  line */
    EscapeCommFunction (pHalSt->hOsPort, CLRDTR);

	/* 
	 * Discard all characters from the output & input buffer and 
	 * terminate pending read or write operations 
	 */
	PurgeComm(pHalSt->hOsPort, 
			  PURGE_TXABORT | PURGE_RXABORT | PURGE_TXCLEAR | PURGE_RXCLEAR);

	CloseHandle(pHalSt->hOsPort);

	for (i=0; i < Event_MaxNum; i++)
	{
		if (pHalSt->aOsEvent[i].hEvent)
		{
			CloseHandle (pHalSt->aOsEvent[i].hEvent);
		}
	}


	return RES_OK;
}


/** 
 * \fn     stPortRxStart
 * \brief  ST port receive start
 * 
 * Resets and start ST receive operation
 * 
 * \note
 * \param	pHalSt  - pointer to HAL ST object
 * \return 	Returns the status of operation: OK or Error
 * \sa     	HAL_ST_Init
 */ 
static EMcpfRes stPortRxStart (THalStObj *pHalSt, McpU8 *pBuf, McpU16 len)
{
	EMcpfRes 	result;


    ResetEvent (pHalSt->aOsEvent[Event_ReadReady].hEvent);

	result = stPortRxReset (pHalSt, pBuf, len);

	/* Activate DTR line */
    EscapeCommFunction(pHalSt->hOsPort, SETDTR);

	return result;
}


/** 
 * \fn     stPortRxReset
 * \brief  ST port receive reset
 * 
 * Purges ST device input buffer and triggers the new receive operation
 * 
 * \note
 * \param	pHalSt  - pointer to HAL ST object
 * \return 	Returns the status of operation: OK or Error
 * \sa     	stPortRxStart
 */ 
static EMcpfRes stPortRxReset (THalStObj *pHalSt, McpU8 *pBuf, McpU16 len)
{
	BOOL 	result;
	DWORD 	lenRead;
	DWORD	err;

	/* 
	 * Read and discard all available bytes from ST port input buffer.
	 * ReadFile returns 0 when fails (no more data to read or error)
	 */
	do
	{
		result = ReadFile (pHalSt->hOsPort, 
						   pBuf,
						   len,
						   &lenRead, 
						   &pHalSt->aOsEvent[Event_ReadReady]);
	}while (result); 

	err = GetLastError();
	if (err != ERROR_IO_PENDING)
	{
		MCPF_REPORT_ERROR (pHalSt->hMcpf, HAL_ST_MODULE_LOG,
						  ("%s: ReadFile failed, err=%u\n", __FUNCTION__, err));
		return RES_ERROR;
	}

	return RES_OK;
}


/** 
 * \fn     sSpeedToOSbaudrate
 * \brief  Convert ST speed to baudrate
 * 
 * Convert ST configuration speed to Win OS baudrate
 * 
 * \note
 * \param	speed  - HAL ST speed value
 * \return 	baudrate
 * \sa     	HAL_ST_Init
 */ 
static DWORD stSpeedToOSbaudrate(ThalStSpeed  speed)
{
    DWORD baudrate;

    switch (speed) {
    
    case HAL_ST_SPEED_9600:
        baudrate = CBR_9600;
        break;
    case HAL_ST_SPEED_38400:
        baudrate = CBR_38400;
        break;
    case HAL_ST_SPEED_57600:
        baudrate = CBR_57600;
        break;
    case HAL_ST_SPEED_115200:
        baudrate = CBR_115200;
        break;
    case HAL_ST_SPEED_128000:
        baudrate = CBR_128000;
        break;
	case HAL_ST_SPEED_230400: 
		baudrate = 230400;
		break;
    case HAL_ST_SPEED_256000:
        baudrate = CBR_256000;
        break;
	case HAL_ST_SPEED_460800:
		baudrate = 460800;
		break;
	case HAL_ST_SPEED_921600:
		baudrate = 921600;
		break;
    default:
        baudrate = speed * 1000;
    } 

	return baudrate;
}


/** 
 * \fn     signalSem
 * \brief  Signal semaphore
 * 
 * Release counting semaphore and increments count by one unit
 * 
 * \note
 * \param	hMcpf - OS framework handle
 * \param	hSem  - OS semphore handle
 * \return 	void
 * \sa     	
 */ 
static void signalSem (handle_t hMcpf, HANDLE hSem)
{
	BOOL result;   

	result = ReleaseSemaphore (hSem, 1, NULL);

	if (!result)
	{
		MCPF_REPORT_ERROR (hMcpf, HAL_ST_MODULE_LOG,
			("%s: ReleaseSemaphore failed, handle=%x!\n", __FUNCTION__, hSem));
	}
}


/** 
 * \fn     waitForSem
 * \brief  Waits for semaphore
 * 
 * Wait for counting semaphore forever
 * 
 * \note
 * \param	hMcpf - OS framework handle
 * \param	hSem  - OS semphore handle
 * \return 	void
 * \sa     	
 */ 
static void waitForSem (handle_t hMcpf, HANDLE hSem)
{
	DWORD result;

    result = WaitForSingleObject (hSem, INFINITE);

	if (result != WAIT_OBJECT_0)
	{
		MCPF_REPORT_ERROR (hMcpf, HAL_ST_MODULE_LOG,
			("%s: WaitForSingleObject failed, result=%x!\n", __FUNCTION__, result));
	}
}


/** 
 * \fn     halStDestroy
 * \brief  Destroy HAL ST Object
 * 
 * Frees memory occupied by HAL ST Object
 * 
 * \note
 * \param	pHalSt - pointer to HAL ST object
 * \return 	void
 * \sa     	
 */ 
static void halStDestroy (THalStObj  * pHalSt)
{
	int i;

	for (i=0; i < Event_MaxNum; i++)
	{
		if (pHalSt->aOsEvent[i].hEvent)
		{
			CloseHandle (pHalSt->aOsEvent[i].hEvent);
		}
	}
	mcpf_mem_free (pHalSt->hMcpf, pHalSt);
}

void resetDataPtr(handle_t hHalSt, McpU8 *pBuf)
{
    MCP_UNUSED_PARAMETER(hHalSt);
    MCP_UNUSED_PARAMETER(pBuf);
}
