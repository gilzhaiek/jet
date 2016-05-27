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

/** \file   mcp_hal_uart.c 
 *  \brief  Win OS Adaptation Layer for UART Interface implementation
 * 
 *  \see    mcp_hal_uart.h
 */

#include <windows.h>
#include <stdio.h>
#include "mcpf_mem.h"
#include "mcpf_report.h"
#include "mcp_hal_os.h"
#include "BusDrv.h"
#include "TxnDefs.h"
#include "bmtrace.h"
#include "mcp_hal_uart.h"

/************************************************************************
 * Defines
 ************************************************************************/

/* Software flow control characters */
#define XON_CHAR       0x11
#define XOFF_CHAR      0x13

#define EVENT_NUM		  3

/************************************************************************
 * Types
 ************************************************************************/

/* HAL UART Events */
typedef enum
{
	Event_WriteCompl,
	Event_ReadReady,
	Event_Mng,
	Event_MaxNum

} EUartEvent;

/* Management Event types */
typedef enum
{
	MngEvt_Config = 1,
	MngEvt_Destroy,
	MngEvt_Last

} EMngEvt;


#ifdef TRAN_DBG
/* Debug counters */
typedef struct
{

	McpU32       Tx;          
	McpU32       TxCompl;         
	McpU32       Rx;          
	McpU32       RxReadyInd;         

} THalUartDebugCount;
#endif

/* Transport Layer Object */
typedef struct
{
	/* module handles */
	handle_t       hMcpf;             

	TI_TxnEventHandlerCb  fEventHandlerCb;
	handle_t 			hHandleCb;
	TUartCfg			uartCfg;
	char				cDrvName[BUSDRV_NAME_MAX_SIZE];

	OVERLAPPED			aOsEvent[Event_MaxNum];
	HANDLE				aOsEventHandle[Event_MaxNum];
	HANDLE       		hOsPort;
	HANDLE				hOsThread;
	DWORD				uOsThreadID;
	HANDLE				hOsSem;
	EMngEvt				eMngEvt;

#ifdef TRAN_DBG
	THalUartDebugCount dbgCount;
#endif

} THalUartObj;

#ifdef TRAN_DBG
THalUartObj * g_pHalUartObj;
#endif


/************************************************************************
 * Internal functions prototypes
 ************************************************************************/

static EMcpfRes uartPortInit (THalUartObj  *pHalUart);
static EMcpfRes uartPortConfig (THalUartObj  *pHalUart);
static DWORD WINAPI uartThreadFun (LPVOID pParam);
static EMcpfRes uartPortDestroy (THalUartObj  	*pHalUart);
static EMcpfRes uartPortRxReset (THalUartObj *pHalUart, McpU8 *pBuf, McpU16 len);
static EMcpfRes uartPortRxStart (THalUartObj *pHalUart, McpU8 *pBuf, McpU16 len);
static DWORD uartSpeedToOSbaudrate(ThalUartSpeed  speed);

static void signalSem  (handle_t hMcpf, HANDLE hSem);
static void waitForSem (handle_t hMcpf, HANDLE hSem);
static void halUartDestroy (THalUartObj  * pHalUart);
 

void rxTest (THalUartObj  *pHalUart);

/************************************************************************
 *
 *   Module functions implementation
 *
 ************************************************************************/

/** 
 * \fn     HAL_UART_Create 
 * \brief  Create the HAL UART object
 * 
 */

handle_t HAL_UART_Create (const handle_t hMcpf, const char *pDrvName)
{
	THalUartObj  * pHalUart;
	int	i;

	pHalUart = mcpf_mem_alloc (hMcpf, sizeof(THalUartObj));
	if (pHalUart == NULL)
	{
		return NULL;
	}

#ifdef TRAN_DBG
	g_pHalUartObj = pHalUart;
#endif

	mcpf_mem_zero (hMcpf, pHalUart, sizeof(THalUartObj));

	pHalUart->hMcpf = hMcpf;

	if (strlen(pDrvName) < BUSDRV_NAME_MAX_SIZE)
	{
		strcpy (pHalUart->cDrvName, pDrvName);
	}
	else
	{
		/* driver name string is too long, truncate it */
		mcpf_mem_copy (hMcpf, pHalUart->cDrvName, (McpU8 *) pDrvName, BUSDRV_NAME_MAX_SIZE - 1);
		pHalUart->cDrvName[BUSDRV_NAME_MAX_SIZE-1] = 0; /* line termination */
	}

	for (i=0; i < Event_MaxNum; i++)
	{
	   pHalUart->aOsEvent[i].hEvent = CreateEvent (NULL, 	/* event security attrib */
												   TRUE, 	/* manual reset  */
												   FALSE,  	/* initial state */
												   NULL		/* event name string */);
	   if (pHalUart->aOsEvent[i].hEvent == NULL)
	   {
		   MCPF_REPORT_ERROR (pHalUart->hMcpf, HAL_UART_MODULE_LOG,
			   ("%s: OS event creation failed!\n", __FUNCTION__));
		   halUartDestroy (pHalUart);
		   return NULL;		   
	   }
	   pHalUart->aOsEventHandle[i] = pHalUart->aOsEvent[i].hEvent;
	}

	pHalUart->hOsSem = CreateSemaphore (NULL,  		/* default security attributes 	*/
										0,  	 	/* initial count 				*/
										1,  	 	/* maximum count 				*/
										NULL);    	/* no name for semaphore 		*/

    if (pHalUart->hOsSem == NULL) 
    {
		MCPF_REPORT_ERROR (pHalUart->hMcpf, HAL_UART_MODULE_LOG,
			("%s: CreateSemaphore failed!\n", __FUNCTION__));
		halUartDestroy (pHalUart);
		return NULL;
    }

	return ((handle_t) pHalUart);
}


/** 
 * \fn     HAL_UART_Init
 * \brief  HAL UART object initialization
 * 
 * Initiate state of HAL UART object
 * 
 */ 
EMcpfRes HAL_UART_Init (handle_t hHalUart, 
			const TI_TxnEventHandlerCb fEventHandlerCb, 
			const handle_t hHandleCb,
			const TBusDrvCfg *pConf,
			McpU8 *pBuf,
			const McpU16 len)
{
	THalUartObj *pHalUart = (THalUartObj *) hHalUart;
	EMcpfRes status;
	McpU16 len_tmp = len; /* for reducing compiler warnings */

	MCP_UNUSED_PARAMETER(pBuf);
	MCP_UNUSED_PARAMETER(len_tmp);

   

	pHalUart->fEventHandlerCb = fEventHandlerCb;
	pHalUart->hHandleCb = hHandleCb;

	mcpf_mem_copy(pHalUart->hMcpf, &pHalUart->uartCfg, 
				  (void *) &pConf->tUartCfg, sizeof(pConf->tUartCfg)); 

	status = uartPortInit (pHalUart);

	if (status != RES_OK)
	{
		if (pHalUart->hOsPort) CloseHandle(pHalUart->hOsPort);
		halUartDestroy (pHalUart);
		return RES_ERROR;
	}

	MCPF_REPORT_DEBUG_CONTROL(pHalUart->hMcpf, HAL_UART_MODULE_LOG, ("HAL_UART_Init: create thread, hHalUart=%p\n", hHalUart));

	pHalUart->hOsThread = CreateThread((LPSECURITY_ATTRIBUTES)NULL,  /* security attrib    */
									   0,                            /* default stack size */
									   (LPTHREAD_START_ROUTINE) uartThreadFun,
									   (LPVOID) hHalUart,            /* thread function parameter */
									   0,                            /* creation flags */
									   &pHalUart->uOsThreadID		 /* thread id to return */);

	if (pHalUart->hOsThread == NULL)
	{
		if (pHalUart->hOsPort) CloseHandle(pHalUart->hOsPort);
		halUartDestroy (pHalUart);
		return RES_ERROR;
	}

	MCPF_REPORT_DEBUG_CONTROL(pHalUart->hMcpf, HAL_UART_MODULE_LOG, ("uartThreadFun waiting for thread initialization completion ...\n"));
	waitForSem (pHalUart->hMcpf, pHalUart->hOsSem);		/* wait for completion of thread initialization */
	MCPF_REPORT_DEBUG_CONTROL(pHalUart->hMcpf, HAL_UART_MODULE_LOG, ("uartThreadFun thread initialization is completed\n"));

	return status;
}


/** 
 * \fn     HAL_UART_Deinit 
 * \brief  Deinitializes the HAL UART object
 * 
 */ 
EMcpfRes HAL_UART_Deinit (handle_t	hHalUart)
{
	THalUartObj  *pHalUart = (THalUartObj *) hHalUart;

	/* Send destroy event to uart thread */
	pHalUart->eMngEvt = MngEvt_Destroy;
	SetEvent (pHalUart->aOsEvent[Event_Mng].hEvent);

	MCPF_REPORT_DEBUG_CONTROL(pHalUart->hMcpf, HAL_UART_MODULE_LOG, ("uartThreadFun waiting for completion of thread deletion ...\n"));

	waitForSem (pHalUart->hMcpf, pHalUart->hOsSem);		/* wait for completion of thread deletion */

	MCPF_REPORT_DEBUG_CONTROL(pHalUart->hMcpf, HAL_UART_MODULE_LOG, ("uartThreadFun thread deletion is completed\n"));

	return RES_OK;
}


/** 
 * \fn     HAL_UART_Destroy 
 * \brief  Destroy the HAL UART object
 * 
 */ 
EMcpfRes HAL_UART_Destroy (handle_t	hHalUart)
{
	THalUartObj  *pHalUart = (THalUartObj *) hHalUart;

	CloseHandle (pHalUart->hOsSem);
	mcpf_mem_free (pHalUart->hMcpf, pHalUart);

	return RES_OK;
}


/** 
 * \fn     HAL_UART_Write
 * \brief  Wrtite data to OS UART device
 *  
 */ 
ETxnStatus HAL_UART_Write (const handle_t hHalUart, 
						   const McpU8 *pBuf, 
						   const McpU16 len,  
						   McpU16 *sentLen)
{
	THalUartObj  *pHalUart = (THalUartObj *) hHalUart;
	BOOL 	result;
	DWORD	wLen;

	MCPF_REPORT_DEBUG_TX(pHalUart->hMcpf, HAL_UART_MODULE_LOG, ("HAL_UART_Write: pBuf=%x len=%u\n", pBuf, len));

	result = WriteFile (pHalUart->hOsPort, 
						pBuf, 
						(DWORD) len, 
						&wLen, 
						&pHalUart->aOsEvent[Event_WriteCompl]);

	*sentLen = (McpU16) wLen;
	if (!result)
	{
		DWORD err = GetLastError();
		if (err == ERROR_IO_PENDING)
		{
			*sentLen = len;				/* In Tx async mode completion indication is triggered  
										 * for entire buffer 
										 */
			return TXN_STATUS_PENDING;
		}
		else
		{
			MCPF_REPORT_ERROR (pHalUart->hMcpf, HAL_UART_MODULE_LOG,
							  ("%s: WriteFile failed, err=%u\n", __FUNCTION__, err));
			*sentLen = 0;	/* consider in the case of error nothing is sent */
			return TXN_STATUS_ERROR; 
		}
	}
	return TXN_STATUS_COMPLETE;
}


/** 
 * \fn     HAL_UART_Read
 * \brief  Read data from OS UART device
 * 
 */ 
ETxnStatus HAL_UART_Read (const handle_t  	hHalUart, 
						  McpU8			*pBuf, 
						  const McpU16	len,  
						  McpU16 		*readLen)
{
	THalUartObj  *pHalUart = (THalUartObj *) hHalUart;
	BOOL 	result;
	DWORD	rLen;

	result = ReadFile (pHalUart->hOsPort, 
					   pBuf, 
					   (DWORD) len, 
					   &rLen, 
					   &pHalUart->aOsEvent[Event_ReadReady]);

	
	*readLen = (McpU16) rLen;
	if (!result)
	{
		DWORD err = GetLastError();
		if (err == ERROR_IO_PENDING)
		{
#ifndef TI_STAND_ALONE_UART_BUSDRV
			MCPF_REPORT_DEBUG_RX(pHalUart->hMcpf, HAL_UART_MODULE_LOG, ("HAL_UART_Read Pending: pBuf=%x len=%u rLen=%u\n", pBuf, len, rLen));
#endif
			return TXN_STATUS_PENDING;
		}
		else
		{
			MCPF_REPORT_ERROR (pHalUart->hMcpf, HAL_UART_MODULE_LOG,
							  ("%s: ReadFile failed, err=%u\n", __FUNCTION__, err));
			return TXN_STATUS_ERROR; 
		}
	}
#ifndef TI_STAND_ALONE_UART_BUSDRV
	MCPF_REPORT_DEBUG_RX(pHalUart->hMcpf, HAL_UART_MODULE_LOG, ("HAL_UART_Read: pBuf=%x len=%u rLen=%u\n", pBuf, len, rLen));
#endif
	return TXN_STATUS_COMPLETE;
}


/** 
 * \fn     HAL_UART_ReadResult
 * \brief  Return the read status and the number of received bytes
 * 
 */ 
EMcpfRes HAL_UART_ReadResult (const handle_t  hHalUart, McpU16 *pLen)
{
	THalUartObj  *pHalUart = (THalUartObj *) hHalUart;
	BOOL  result;
	DWORD len;

	result = GetOverlappedResult (pHalUart->hOsPort,
								  &pHalUart->aOsEvent[Event_ReadReady], 
								  &len, FALSE);
	*pLen = (McpU16) len;

	if (!result)
	{
		*pLen = 0;
	}
	return ((result) ? RES_OK : RES_ERROR);
}


/** 
 * \fn     HAL_UART_RestartRead
 * \brief  Signals to start read operation
 * 
 */ 
EMcpfRes HAL_UART_RestartRead (const handle_t  hHalUart)
{
	THalUartObj  *pHalUart = (THalUartObj *) hHalUart;

	SetEvent (pHalUart->aOsEventHandle[Event_ReadReady]);

	return RES_OK;
}

/** 
 * \fn     HAL_UART_ResetRead
 * \brief  Cleans Rx buffer and starts a new read operation
 * 
 */ 
EMcpfRes HAL_UART_ResetRead (const handle_t  hHalUart, McpU8 *pBuf, const McpU16 len)
{
	THalUartObj  *pHalUart = (THalUartObj *) hHalUart;

	uartPortRxReset (pHalUart, pBuf, len);

	return RES_OK;
}

/** 
 * \fn     HAL_UART_Set
 * \brief  Set HAL UART object new configuration
 * 
 */ 

EMcpfRes HAL_UART_Set (handle_t 			hHalUart, 
						 const TBusDrvCfg 	*pConf)
{
	THalUartObj  *pHalUart = (THalUartObj *) hHalUart;
	
	pHalUart->uartCfg.uBaudRate = pConf->tUartCfg.uBaudRate;

	/* Config UART */
	uartPortConfig(pHalUart);
	
	return RES_OK;
}

/** 
 * \fn     HAL_UART_Port_Reset
 * \brief  reset and optionally start HAL UART  hardware to
 * 
 * 
 */ 
EMcpfRes HAL_UART_Port_Reset (handle_t  *hHalUart, McpU8 *pBuf, McpU16 len, const McpU16 baudrate)
{
    THalUartObj     *pHalUart = (THalUartObj *)hHalUart;
    COMMTIMEOUTS	comTimeout;
    DCB        		devCtrl;
	BOOL			res;
    EMcpfRes        status = RES_ERROR;
    static          portRxStart = MCP_FALSE;
    
#ifndef TI_STAND_ALONE_UART_BUSDRV
   
	/* Specify a set of events to be monitored for a communications device */
	SetCommMask (pHalUart->hOsPort, 
		     EV_RXCHAR |   	/* character was received into input buffer 	  */
		     EV_TXEMPTY		/* the last character was sent from output buffer */);

	/* Initialize communications parameters */
	SetupComm (pHalUart->hOsPort, 
			   pHalUart->uartCfg.XonLimit * 4,  /* size of device internal input buffer  */
			   pHalUart->uartCfg.XonLimit * 4   /* size of device internal output buffer */);

	/* 
	 * Discard all characters from the output & input buffer and 
	 * terminate pending read or write operations 
	 */
	PurgeComm(pHalUart->hOsPort, 
			  PURGE_TXABORT | PURGE_RXABORT | PURGE_TXCLEAR | PURGE_RXCLEAR);
#endif


    /* Sets the timeout parameters for all read and write operations  */
#ifdef TI_STAND_ALONE_UART_BUSDRV
	GetCommTimeouts(pHalUart->hOsPort, &comTimeout);
	comTimeout.ReadIntervalTimeout 			= MAXWORD;
	comTimeout.WriteTotalTimeoutConstant 	= 0;
#else
	comTimeout.ReadIntervalTimeout 		= 10;
	comTimeout.WriteTotalTimeoutConstant 	= 750;
#endif	;
	comTimeout.ReadTotalTimeoutMultiplier 	= 0;
	comTimeout.ReadTotalTimeoutConstant 	= 0;
	comTimeout.WriteTotalTimeoutMultiplier 	= 0;
	 
	SetCommTimeouts(pHalUart->hOsPort, &comTimeout);

	/* Set device control: baudrate, parity & stop bits, flow control etc. options */
	devCtrl.DCBlength = sizeof(DCB);
	GetCommState(pHalUart->hOsPort, &devCtrl);

    pHalUart->uartCfg.uBaudRate = baudrate;
	devCtrl.BaudRate = uartSpeedToOSbaudrate ((ThalUartSpeed) pHalUart->uartCfg.uBaudRate);
    devCtrl.ByteSize = 8;
    devCtrl.Parity 	 = NOPARITY;
    devCtrl.StopBits = ONESTOPBIT;

#ifdef TI_STAND_ALONE_UART_BUSDRV
    devCtrl.fDtrControl  = DTR_CONTROL_ENABLE;
    //devCtrl.fRtsControl  = RTS_CONTROL_DISABLE;
    devCtrl.fRtsControl  = RTS_CONTROL_ENABLE;
    devCtrl.fOutxCtsFlow = FALSE	;
    // devCtrl.fBinary 	 = FALSE;
    //devCtrl.fParity 	 = FALSE;
#else
    devCtrl.fBinary 	 = TRUE;
    devCtrl.fParity 	 = TRUE;
    devCtrl.fOutxDsrFlow = FALSE;

#ifndef TI_TRANS_TEST
    devCtrl.fDtrControl  = DTR_CONTROL_ENABLE;
    devCtrl.fOutxCtsFlow = TRUE;
    devCtrl.fRtsControl  = RTS_CONTROL_HANDSHAKE;

#else

    /* There is no hardware handshake configured in PC loopack test */
    devCtrl.fDtrControl  = DTR_CONTROL_DISABLE;
    devCtrl.fOutxCtsFlow = FALSE;
    devCtrl.fRtsControl  = RTS_CONTROL_DISABLE;
#endif /* TI_TRANS_TEST */

    devCtrl.fInX 	     = FALSE;
    devCtrl.fOutX	     = FALSE;
    devCtrl.fDsrSensitivity  = FALSE;
    devCtrl.fNull 	     = FALSE;
    devCtrl.fAbortOnError    = FALSE;

    devCtrl.XonLim  	 = pHalUart->uartCfg.XonLimit;
    devCtrl.XoffLim 	 = pHalUart->uartCfg.XoffLimit;
    
	devCtrl.XonChar  = XON_CHAR;
    devCtrl.XoffChar = XOFF_CHAR;
#endif // #ifdef TI_STAND_ALONE_UART_BUSDRV

	res = SetCommState (pHalUart->hOsPort, &devCtrl);
   if (res)
	{
       if (portRxStart == MCP_FALSE)
       {
            uartPortRxStart (pHalUart, pBuf, len);
            portRxStart = MCP_TRUE;
       }
		status = RES_OK;
	}
	else
	{
		MCPF_REPORT_ERROR (pHalUart->hMcpf, HAL_UART_MODULE_LOG,
						  ("%s: Com Port SetCommState failed!\n", __FUNCTION__));
	/*	CloseHandle(pHalUart->hOsPort); 
		pHalUart->hOsPort = NULL;
     tbd */
		status = RES_ERROR;
	}

    return status;
}
/** 
 * \fn     uartPortInit
 * \brief  UART port initialization
 * 
 * Open and initiate Win OS UART device
 * 
 * \note
 * \param	pHalUart  - pointer to HAL UART object
 * \return 	Returns the status of operation: OK or Error
 * \sa     	HAL_UART_Init
 */ 
static EMcpfRes uartPortInit (THalUartObj  *pHalUart)
{
    char			comFileName[10];

    wsprintf (comFileName, "\\\\.\\COM%d", pHalUart->uartCfg.portNum);

	pHalUart->hOsPort = CreateFile (comFileName, 
									GENERIC_READ | GENERIC_WRITE,
									0,  	/* share mode */
									NULL,   /* security attrib */
									OPEN_EXISTING,
									FILE_ATTRIBUTE_NORMAL | FILE_FLAG_OVERLAPPED,
									NULL	/* template file */);

	if (pHalUart->hOsPort == INVALID_HANDLE_VALUE)
	{
		MCPF_REPORT_ERROR (pHalUart->hMcpf, HAL_UART_MODULE_LOG,
						  ("%s: Com Port CreateFile failed, file=%s!\n", 
						   __FUNCTION__, comFileName));
		return RES_ERROR;
	}
    return RES_OK;
}

/** 
 * \fn     uartPortConfig
 * \brief  UART port configuration
 * 
 * Change UART port configuration parameters as baudrate and Xon/Xoff limits 
 * 
 * \note    UART port is to be initialized before the function call
 * \param	pHalUart  - pointer to HAL UART object
 * \return 	Returns the status of operation: OK or Error
 * \sa     	uartPortInit
 */ 
static EMcpfRes uartPortConfig (THalUartObj  *pHalUart)
{
    DCB   devCtrl;
	BOOL  res;

	devCtrl.DCBlength = sizeof(DCB);
	GetCommState(pHalUart->hOsPort, &devCtrl);

	/* Set device control: baudrate and  Xon/Xoff limits */
	devCtrl.BaudRate = uartSpeedToOSbaudrate ((ThalUartSpeed) pHalUart->uartCfg.uBaudRate);

	res = SetCommState (pHalUart->hOsPort, &devCtrl);

	if (res)
	{
		return RES_OK;
	}
	else
	{
		MCPF_REPORT_ERROR (pHalUart->hMcpf, HAL_UART_MODULE_LOG,
						  ("%s: Com Port SetCommState failed!\n", __FUNCTION__));
		CloseHandle(pHalUart->hOsPort);
		pHalUart->hOsPort = NULL;
		return RES_ERROR;
	}
}


/** 
 * \fn     uartThreadFun
 * \brief  UART thread main function
 * 
 * Thread function waits on and processes OS events (write complete, read ready and management)
 * 
 * \note
 * \param	pHalUart  - pointer to HAL UART object
 * \return 	N/A
 * \sa     	HAL_UART_Init
 */ 
static DWORD WINAPI uartThreadFun (LPVOID pParam)
{
	THalUartObj  *pHalUart = (THalUartObj *) pParam;
	DWORD 		  objectIndex;
	McpBool		  doLoop = TRUE;
	TTxnEvent 	  tEvent;

	CL_TRACE_TASK_DEF();

    MCP_HAL_LOG_SetThreadName("UART");

	MCPF_REPORT_DEBUG_CONTROL(pHalUart->hMcpf, HAL_UART_MODULE_LOG, ("uartThreadFun started, pHalUart=%p\n", pHalUart));

	signalSem (pHalUart->hMcpf, pHalUart->hOsSem);	/* signal semaphore the thread is created */

    while (doLoop) {

		MCPF_REPORT_DEBUG_CONTROL(pHalUart->hMcpf, HAL_UART_MODULE_LOG, ("uartThreadFun: waiting for event ...\n"));

        /* Wake up every now and then just in case a Deinit is requested */
        objectIndex = WaitForMultipleObjects (Event_MaxNum, 
											  pHalUart->aOsEventHandle, 
											  FALSE,		/* any one of the objects is signaled */ 
											  INFINITE );
		CL_TRACE_TASK_START();

		MCPF_REPORT_DEBUG_CONTROL(pHalUart->hMcpf, HAL_UART_MODULE_LOG, ("uartThreadFun: event: objectIndex=%x\n", objectIndex));

		switch (objectIndex)
		{
		case WAIT_OBJECT_0:
			/* Write complete event */

			MCPF_REPORT_DEBUG_TX(pHalUart->hMcpf, HAL_UART_MODULE_LOG, ("uartThreadFun: WriteCompl event\n"));

			ResetEvent (pHalUart->aOsEventHandle[Event_WriteCompl]);
			tEvent.eEventType = Txn_WriteComplInd;
			pHalUart->fEventHandlerCb (pHalUart->hHandleCb, &tEvent);
			break;

		case (WAIT_OBJECT_0+1):
			/* Read ready event */
			MCPF_REPORT_DEBUG_RX(pHalUart->hMcpf, HAL_UART_MODULE_LOG, ("uartThreadFun: ReadReady event\n"));

			ResetEvent (pHalUart->aOsEventHandle[Event_ReadReady]);
			tEvent.eEventType = Txn_ReadReadylInd;
			pHalUart->fEventHandlerCb (pHalUart->hHandleCb, &tEvent);

			break;

		case (WAIT_OBJECT_0+2):
			/* Management event */

			MCPF_REPORT_DEBUG_CONTROL(pHalUart->hMcpf, HAL_UART_MODULE_LOG, ("uartThreadFun: Management event=%d\n", pHalUart->eMngEvt));

			ResetEvent (pHalUart->aOsEventHandle[Event_Mng]);

			switch (pHalUart->eMngEvt)
			{
			case MngEvt_Config:
				uartPortConfig (pHalUart);
				break;

			case MngEvt_Destroy:
				/* Exit loop and thread */
				doLoop = FALSE;
				break;

			default:
				MCPF_REPORT_ERROR (pHalUart->hMcpf, HAL_UART_MODULE_LOG,
								  ("%s: Unknown management event received Id=%u!\n", 
								   __FUNCTION__, pHalUart->eMngEvt));
				break;
			}
			break;

		default:
			MCPF_REPORT_ERROR (pHalUart->hMcpf, HAL_UART_MODULE_LOG,
							  ("%s: Unknown event received objId=%u!\n", 
							   __FUNCTION__, objectIndex));
			break;
		}

		CL_TRACE_TASK_END("uartThreadFun", CL_TRACE_CONTEXT_TASK, pHalUart->cDrvName, "HAL");
	} /* while */

	MCPF_REPORT_DEBUG_CONTROL(pHalUart->hMcpf, HAL_UART_MODULE_LOG, ("uartThreadFun: exit thread\n"));

	uartPortDestroy (pHalUart);

	signalSem (pHalUart->hMcpf, pHalUart->hOsSem);	/* signal semaphore the thread is about to exit */
	
	return TRUE;
}

/** 
 * \fn     uartPortDestroy
 * \brief  UART port de-initialization
 * 
 * Stop and close OS UART port, free allocated memory for HAL UART object
 * 
 * \note
 * \param	pHalUart  - pointer to HAL UART object
 * \return 	Returns the status of operation: OK or Error
 * \sa     	uartThreadFun
 */ 
static EMcpfRes uartPortDestroy (THalUartObj  *pHalUart)
{
	int i;

    /* Deactivate DTR  line */
    EscapeCommFunction (pHalUart->hOsPort, CLRDTR);

	/* 
	 * Discard all characters from the output & input buffer and 
	 * terminate pending read or write operations 
	 */
	PurgeComm(pHalUart->hOsPort, 
			  PURGE_TXABORT | PURGE_RXABORT | PURGE_TXCLEAR | PURGE_RXCLEAR);

	CloseHandle(pHalUart->hOsPort);

	for (i=0; i < Event_MaxNum; i++)
	{
		if (pHalUart->aOsEvent[i].hEvent)
		{
			CloseHandle (pHalUart->aOsEvent[i].hEvent);
		}
	}


	return RES_OK;
}


/** 
 * \fn     uartPortRxStart
 * \brief  UART port receive start
 * 
 * Resets and start UART receive operation
 * 
 * \note
 * \param	pHalUart  - pointer to HAL UART object
 * \return 	Returns the status of operation: OK or Error
 * \sa     	HAL_UART_Init
 */ 
static EMcpfRes uartPortRxStart (THalUartObj *pHalUart, McpU8 *pBuf, McpU16 len)
{
	EMcpfRes 	result;


    ResetEvent (pHalUart->aOsEvent[Event_ReadReady].hEvent);

	result = uartPortRxReset (pHalUart, pBuf, len);

	/* Activate DTR line */
    EscapeCommFunction(pHalUart->hOsPort, SETDTR);

	return result;
}


/** 
 * \fn     uartPortRxReset
 * \brief  UART port receive reset
 * 
 * Purges UART device input buffer and triggers the new receive operation
 * 
 * \note
 * \param	pHalUart  - pointer to HAL UART object
 * \return 	Returns the status of operation: OK or Error
 * \sa     	uartPortRxStart
 */ 
static EMcpfRes uartPortRxReset (THalUartObj *pHalUart, McpU8 *pBuf, McpU16 len)
{
	BOOL 	result;
	DWORD 	lenRead;
	DWORD	err;

	/* 
	 * Read and discard all available bytes from UART port input buffer.
	 * ReadFile returns 0 when fails (no more data to read or error)
	 */
	do
	{
		result = ReadFile (pHalUart->hOsPort, 
						   pBuf,
						   len,
						   &lenRead, 
						   &pHalUart->aOsEvent[Event_ReadReady]);
	}while (result); 

	err = GetLastError();
	if (err != ERROR_IO_PENDING)
	{
		MCPF_REPORT_ERROR (pHalUart->hMcpf, HAL_UART_MODULE_LOG,
						  ("%s: ReadFile failed, err=%u\n", __FUNCTION__, err));
		return RES_ERROR;
	}

	return RES_OK;
}


/** 
 * \fn     uartSpeedToOSbaudrate
 * \brief  Convert UART speed to baudrate
 * 
 * Convert UART configuration speed to Win OS baudrate
 * 
 * \note
 * \param	speed  - HAL UART speed value
 * \return 	baudrate
 * \sa     	HAL_UART_Init
 */ 
static DWORD uartSpeedToOSbaudrate(ThalUartSpeed  speed)
{
    DWORD baudrate;

    switch (speed) {
    
    case HAL_UART_SPEED_9600:
        baudrate = CBR_9600;
        break;
    case HAL_UART_SPEED_38400:
        baudrate = CBR_38400;
        break;
    case HAL_UART_SPEED_57600:
        baudrate = CBR_57600;
        break;
    case HAL_UART_SPEED_115200:
        baudrate = CBR_115200;
        break;
    case HAL_UART_SPEED_128000:
        baudrate = CBR_128000;
        break;
	case HAL_UART_SPEED_230400: 
		baudrate = 230400;
		break;
    case HAL_UART_SPEED_256000:
        baudrate = CBR_256000;
        break;
	case HAL_UART_SPEED_460800:
		baudrate = 460800;
		break;
	case HAL_UART_SPEED_921600:
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
		MCPF_REPORT_ERROR (hMcpf, HAL_UART_MODULE_LOG,
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
		MCPF_REPORT_ERROR (hMcpf, HAL_UART_MODULE_LOG,
			("%s: WaitForSingleObject failed, result=%x!\n", __FUNCTION__, result));
	}
}


/** 
 * \fn     halUartDestroy
 * \brief  Destroy HAL UART Object
 * 
 * Frees memory occupied by HAL UART Object
 * 
 * \note
 * \param	pHalUart - pointer to HAL UART object
 * \return 	void
 * \sa     	
 */ 
static void halUartDestroy (THalUartObj  * pHalUart)
{
	int i;

	for (i=0; i < Event_MaxNum; i++)
	{
		if (pHalUart->aOsEvent[i].hEvent)
		{
			CloseHandle (pHalUart->aOsEvent[i].hEvent);
		}
	}
	mcpf_mem_free (pHalUart->hMcpf, pHalUart);
}

