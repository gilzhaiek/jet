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

/** Include Files **/
#include <windows.h>
#include <stdarg.h>
#include <stdio.h>
#include <time.h>

#include "pla_os.h"


/************************************************************************/
/*	                         Definitions                                */
/************************************************************************/

#define CLOCK_DIVIDER_POWER        	8	/* The performance counter divider */


/* Client's structure */
typedef struct
{
	handle_t			hRegClient;
	mcpf_TaskProcedure	pThreadProcArry[TASK_MAX_ID];
	EmcpTaskId			pTaskId[TASK_MAX_ID];
} TPlaClient;

/* Timer */
typedef struct
{
	MMRESULT timerId;
	mcpf_timer_cb cb;
	handle_t hCaller;
} timer_t;


/************************************************************************/
/*						    Global Variables                            */
/************************************************************************/
TPlaClient		tPlaClient;
timer_t			timerData;


/************************************************************************/
/*					Internal Function Deceleration                      */
/************************************************************************/
/* Timer Callback */
VOID CALLBACK os_timer_cb(UINT uTimerID, UINT uMsg, DWORD dwUser, DWORD dw1, DWORD dw2);

/* Thread Procedure */
static void os_threadProc(void *param);



/************************************************************************/
/*								APIs                                    */
/************************************************************************/
/* Init */

/** 
 * \fn     pla_create
 * \brief  Create Platform Abstraction Context
 * 
 * This function creates the Platform Abstraction Context.
 * 
 * \note
 * \return 	Handler to PLA
 * \sa     	pla_create
 */
handle_t 	pla_create ()
{
	return NULL;
}

/** 
 * \fn     pla_registerClient
 * \brief  Register a client (handler) to the PLA
 * 
 * This function registers a client (handler) to the PLA. 
 * Only one client is allowed.
 * 
 * \note
 * \param	hPla - PLA handler. 
 * \param	hClient - Client's handler.
 * \return 	None
 * \sa     	pla_registerClient
 */
void 		pla_registerClient(handle_t hPla, handle_t hClient)
{
	int i;

	MCPF_UNUSED_PARAMETER(hPla);

	tPlaClient.hRegClient = hClient;
	for(i = 0; i < TASK_MAX_ID; i++)
	{
		tPlaClient.pThreadProcArry [i] = NULL;
		tPlaClient.pTaskId[i] = i;
	}

}


/* Memory */

/** 
 * \fn     os_malloc
 * \brief  Memory Allocation
 * 
 * This function allocates a memory according to the requested size.
 * 
 * \note
 * \param	hPla - PLA handler. 
 * \param	mem_size - size of buffer to allocate.
 * \return 	Pointer to the allocated memory.
 * \sa     	os_malloc
 */
McpU8 		*os_malloc (handle_t hPla, McpU32 mem_size)
{
	MCPF_UNUSED_PARAMETER(hPla);
	return malloc(mem_size);
}

/** 
 * \fn     os_free
 * \brief  Memory Free
 * 
 * This function frees the received memory.
 * 
 * \note
 * \param	hPla - PLA handler. 
 * \param	*mem - pointer to the memory to free.
 * \return 	Result of operation: OK or ERROR
 * \sa     	os_free
 */
EMcpfRes  	os_free (handle_t hPla, McpU8 *mem)
{
	MCPF_UNUSED_PARAMETER(hPla);
	free(mem);
	return RES_COMPLETE;
}

/** 
 * \fn     os_memory_set
 * \brief  Memory Set
 * 
 * This function sets the given memory with a given value.
 * 
 * \note
 * \param	hPla - PLA handler. 
 * \param	*pMemPtr - pointer to the memory to set.
 * \param	Value - value to set.
 * \param	Length - number of bytes to set.
 * \return 	None
 * \sa     	os_memory_set
 */
void 		os_memory_set (handle_t hPla, void *pMemPtr, McpS32 Value, McpU32 Length)
{
	MCPF_UNUSED_PARAMETER(hPla);
	memset(pMemPtr,Value, Length);
}

/** 
 * \fn     os_memory_zero
 * \brief  Set memory to zero
 * 
 * This function sets the given memory to zero.
 * 
 * \note
 * \param	hPla - PLA handler. 
 * \param	*pMemPtr - pointer to the memory to set.
 * \param	Length - number of bytes to set.
 * \return 	None
 * \sa     	os_memory_zero
 */
void 		os_memory_zero (handle_t hPla, void *pMemPtr, McpU32 Length)
{
	MCPF_UNUSED_PARAMETER(hPla);
	memset(pMemPtr, 0, Length);
}

/** 
 * \fn     os_memory_copy
 * \brief  Copy Memory
 * 
 * This function copies given memory size from src to dest.
 * 
 * \note
 * \param	hPla - PLA handler. 
 * \param	*pDestination - pointer to the memory to copy to.
 * \param	*pSource - pointer to the memory to copy from.
 * \param	uSize - number of bytes to cpoy.
 * \return 	None
 * \sa     	os_memory_copy
 */
void 		os_memory_copy (handle_t hPla, void *pDestination, void *pSource, McpU32 uSize)
{
	MCPF_UNUSED_PARAMETER(hPla);
	memcpy(pDestination, pSource, uSize);
}


/* Time */
/************************************************************************/
/* NOTE: Timers support in PLA only supports one OS timer to be active  */
/*       at a time. MCPF is responsible of managing multiple timers.    */
/************************************************************************/

/** 
 * \fn     os_timer_start
 * \brief  Starts an OS timer
 * 
 * This function starts an OS timer. It should supply it's own cb function, 
 * and should store the received cb & handler with the returned timer ID.
 * 
 * \note
 * \param	hPla - PLA handler. 
 * \param	cb - timer's callback function
 * \param	hCaller - callback's handler
 * \param	expiry_time - timer's expiry time
 * \return 	Timer ID
 * \sa     	os_timer_start
 */
McpUint  	os_timer_start (handle_t hPla, mcpf_timer_cb cb, handle_t hCaller, McpU32 expiry_time)
{
	MCPF_UNUSED_PARAMETER(hPla);

	timerData.timerId = timeSetEvent(expiry_time, 0, (LPTIMECALLBACK)os_timer_cb, 0, TIME_ONESHOT);
	timerData.cb = cb;
	timerData.hCaller = hCaller;
	
	return (McpUint)timerData.timerId;

}

/** 
 * \fn     os_timer_stop
 * \brief  Stops the received timer
 * 
 * This function stops the received timer 
 * and clears its related stored parameters.
 * 
 * \note
 * \param	hPla - PLA handler. 
 * \param	timer_id - The ID of the timer to stop.
 * \return 	Result of operation: OK or ERROR
 * \sa     	os_timer_stop
 */
McpBool  	os_timer_stop (handle_t hPla, McpUint timer_id)
{
	McpBool	ret_val;

	MCPF_UNUSED_PARAMETER(hPla);
	MCPF_UNUSED_PARAMETER(timer_id);

	/* Kill Timer */
	if((McpUint)timerData.timerId == timer_id)
	{
		if (timeKillEvent(timerData.timerId) == TIMERR_NOERROR)
			ret_val = TRUE;
		else
			ret_val = FALSE;
	}
	else
		return FALSE;

	/* Clear timer data */
	timerData.timerId = 0;
	timerData.cb = NULL;
	timerData.hCaller = NULL;
	
	return ret_val;
}

/** 
 * \fn     os_get_current_time_inSec
 * \brief  Get time in seconds
 * 
 * This function returns time in seconds since 1.1.1970.
 * 
 * \note
 * \param	hPla - PLA handler. 
 * \return 	time in seconds since 1.1.1970
 * \sa     	os_get_current_time_inSec
 */
McpU32  	os_get_current_time_inSec (handle_t hPla)
{
	McpS32 uTime;
	MCPF_UNUSED_PARAMETER(hPla);
	time(&uTime);
	return (McpU32)uTime;
}

/** 
 * \fn     os_get_current_time_inMilliSec
 * \brief  Get time in milliseconds
 * 
 * This function returns system time in milliseconds.
 * 
 * \note
 * \param	hPla - PLA handler. 
 * \return 	system time in milliseconds
 * \sa     	os_get_current_time_inMilliSec
 */
McpU32  	os_get_current_time_inMilliSec (handle_t hPla)
{
	MCPF_UNUSED_PARAMETER(hPla);
	return timeGetTime();
}

/** 
 * \fn     os_get_performance_counter
 * \brief  Read performance counter
 * 
 */
McpU32 os_get_performance_counter()
{
  LARGE_INTEGER performanceCount;
  McpU32		tick;


  QueryPerformanceCounter (&performanceCount);

  /* The modern PC has high clock of 2 - 3 Ghz so take one byte of high word and 
   * OR it with 3 MSB bytes of low word
   */

  tick = (performanceCount.HighPart << (32 - CLOCK_DIVIDER_POWER)) | 
	     (performanceCount.LowPart >> CLOCK_DIVIDER_POWER);

  return tick;

}

/** 
 * \fn     os_get_performance_tick_nsec
 * \brief  Get performance counter resolution
 * 
 */
McpU32 os_get_performance_tick_nsec (void)
{
	McpU32	uTicTime;

    LARGE_INTEGER Frequency ;

    QueryPerformanceFrequency(&Frequency );

	uTicTime = 1000000000L / (Frequency.LowPart >> CLOCK_DIVIDER_POWER);

	return uTicTime;
}

/* Signaling Object */

/** 
 * \fn     os_sigobj_create
 * \brief  Create signaling object
 * 
 * This function creates a signaling object.
 * 
 * \note
 * \param	hPla - PLA handler. 
 * \return 	Handler to object
 * \sa     	os_sigobj_create
 */
void  		*os_sigobj_create (handle_t hPla)
{
	MCPF_UNUSED_PARAMETER(hPla);
	return CreateEvent(NULL, FALSE, FALSE, NULL);

}

/** 
 * \fn     os_sigobj_wait
 * \brief  Wait on the signaling object
 * 
 * This function waits on the signaling object.
 * 
 * \note
 * \param	hPla - PLA handler. 
 * \param	*evt - signaling object handler.
 * \return 	Result of operation: OK or ERROR
 * \sa     	os_sigobj_wait
 */
EMcpfRes    os_sigobj_wait (handle_t hPla, void *evt)
{
	MCPF_UNUSED_PARAMETER(hPla);

	if (WaitForSingleObject(evt, INFINITE) == WAIT_OBJECT_0)
		return RES_COMPLETE;
	else
		return RES_ERROR;
}

/** 
 * \fn     os_sigobj_set
 * \brief  Set the signaling object
 * 
 * This function sets the signaling object.
 * 
 * \note
 * \param	hPla - PLA handler. 
 * \param	*evt - signaling object handler.
 * \return 	Result of operation: OK or ERROR
 * \sa     	os_sigobj_set
 */
EMcpfRes    os_sigobj_set (handle_t hPla, void *evt)
{
	MCPF_UNUSED_PARAMETER(hPla);

	if (SetEvent(evt))
		return RES_COMPLETE;
	else
		return RES_ERROR;
}

/** 
 * \fn     os_sigobj_destroy
 * \brief  Destroy the signaling object
 * 
 * This function destroys the signaling object.
 * 
 * \note
 * \param	hPla - PLA handler. 
 * \param	*evt - signaling object handler.
 * \return 	Result of operation: OK or ERROR
 * \sa     	os_sigobj_destroy
 */
EMcpfRes    os_sigobj_destroy (handle_t hPla, void *evt)
{
	MCPF_UNUSED_PARAMETER(hPla);
	
	if (CloseHandle(evt))
		return RES_COMPLETE;
	else
		return RES_ERROR;
}


/* Messaging */

/** 
 * \fn     os_send_message
 * \brief  Send a message to another process
 * 
 * This function sends a given message to the destination process. 
 * 
 * \note
 * \param	hPla - PLA handler. 
 * \param	dest - Index of destination process/application.
 * \param	*msg - The message to send.
 * \return 	Result of operation: OK or ERROR
 * \sa     	os_send_message
 */
EMcpfRes    os_send_message (handle_t hPla, McpU32 dest, TmcpfMsg *msg)
{
	MCPF_UNUSED_PARAMETER(hPla);
	MCPF_UNUSED_PARAMETER(dest);
	MCPF_UNUSED_PARAMETER(msg);

	return RES_COMPLETE;
}


/* Threads */

/** 
 * \fn     os_createThread
 * \brief  Create a Thread
 * 
 * This function should save the thread procedure that 
 * it received (lpStartAddress) in the array, according 
 * to the task id that is received (lpParameter). 
 * Than it should create the thread, with a local function as the thread's procedure 
 * and the received parameter (lpParameter) as the param.
 * 
 * \note
 * \param	*lpThreadAttributes - Thread's attributes. 
 * \param	uStackSize - Stack Size.
 * \param	*lpStartAddress - Pointer tot he thread's procedure.
 * \param	*lpParameter - Parameter of the thred's procedure. 
 * \param	uCreationFlags - Thread's creation flags.
 * \param	*puThreadId - pointer to the created thread id.
 * \return 	Thread's Handler
 * \sa     	os_createThread
 */
handle_t	os_createThread (void *lpThreadAttributes, McpU32 uStackSize, mcpf_TaskProcedure lpStartAddress, 
							 void *lpParameter, McpU32 uCreationFlags, McpU32 *puThreadId)
{
	EmcpTaskId *pTaskId = (EmcpTaskId *)(lpParameter);
	EmcpTaskId eTaskId = *pTaskId;
	
	tPlaClient.pThreadProcArry[eTaskId] = lpStartAddress;
	
	return CreateThread(lpThreadAttributes, uStackSize, (LPTHREAD_START_ROUTINE)os_threadProc, 
						&tPlaClient.pTaskId[eTaskId], uCreationFlags, puThreadId);
}

/** 
 * \fn     os_exitThread
 * \brief  Exit a running thread
 * 
 * This function exits a running thread.
 * 
 * \note
 * \param	uExitCode - Exit Code. 
 * \return 	None
 * \sa     	os_exitThread
 */
void		os_exitThread (McpU32 uExitCode)
{
	ExitThread(uExitCode);
}

/** 
 * \fn     os_SetTaskPriority
 * \brief  Set the task's priority
 * 
 * This function will set the priority of the given task.
 * 
 * \note
 * \param	hTaskHandle     - Task's handler.
 * \param	sPriority - Priority to set.
 * \return 	Result of operation: OK or ERROR
 * \sa     	os_SetTaskPriority
 */ 
EMcpfRes		os_SetTaskPriority (handle_t hTaskHandle, McpInt sPriority)
{
	BOOL	retVal;

	retVal = SetThreadPriority(hTaskHandle, sPriority);
	
	if(retVal)
		return RES_OK;
	else
		return RES_ERROR;
}


/* Print */

/** 
 * \fn     os_print
 * \brief  Print String
 * 
 * This function prints the string to the default output.
 * 
 * \note
 * \param	format - String's format.
 * \param	...		- String's arguments.
 * \return 	None
 * \sa     	os_print
 */
void		os_print (const char *format ,...)
{
	va_list args;

	va_start (args, format);
	vprintf (format, args);
	va_end (args);
}


/************************************************************************/
/*						Internal Functions                              */
/************************************************************************/
VOID CALLBACK os_timer_cb(UINT uTimerID, UINT uMsg, DWORD dwUser, DWORD dw1, DWORD dw2)
{
	MCPF_UNUSED_PARAMETER(uMsg);
	MCPF_UNUSED_PARAMETER(dwUser);
	MCPF_UNUSED_PARAMETER(dw1);
	MCPF_UNUSED_PARAMETER(dw2);
	
	if ( (timerData.timerId == uTimerID) && (timerData.cb) )
		timerData.cb (timerData.hCaller, uTimerID);
}

static void os_threadProc(void *param)
{
	EmcpTaskId *eTaskId = (EmcpTaskId *)(param);
	
	if (tPlaClient.pThreadProcArry[*eTaskId] != NULL)
		tPlaClient.pThreadProcArry[*eTaskId](tPlaClient.hRegClient, *eTaskId);
}
