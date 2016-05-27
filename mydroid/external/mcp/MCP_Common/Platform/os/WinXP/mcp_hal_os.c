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
/*******************************************************************************\
*
*   FILE NAME:      mcp_hal_os.c
*
*   DESCRIPTION:    This file contain implementation of MCP HAL in WIN
*
*   AUTHOR:         Udi Ron
*
\*******************************************************************************/

/********************************************************************************
 *
 * Include files
 *
 *******************************************************************************/
#include <windows.h>
#include <stdarg.h>
#include <stdio.h>
#include <assert.h>
#include <math.h>

#include "mcp_hal_os.h"

/********************************************************************************
 *
 * Constants 
 *
 *******************************************************************************/

/*******************************************************************************
 *
 * Macro definitions
 *
 ******************************************************************************/
#define _MCP_HAL_OS_UNUSED_PARAMETER(_PARM)     ((_PARM) = (_PARM))
 
/*

/********************************************************************************
 *
 * Internal function prototypes
 *
 *******************************************************************************/

/********************************************************************************
 *
 * Function definitions
 *
 *******************************************************************************/

/*-------------------------------------------------------------------------------
 * MCP_HAL_OS_Init()
 *
 */
McpHalOsStatus MCP_HAL_OS_Init(void)
{
    return MCP_HAL_OS_STATUS_SUCCESS;    
}


/*-------------------------------------------------------------------------------
 * MCP_HAL_OS_Deinit()
 *
 */
McpHalOsStatus MCP_HAL_OS_Deinit(void)
{
	return MCP_HAL_OS_STATUS_SUCCESS;
}


/*-------------------------------------------------------------------------------
 * MCP_HAL_OS_CreateSemaphore()
 *
 */
McpHalOsStatus MCP_HAL_OS_CreateSemaphore(const char 				*semaphoreName, 
													McpHalOsSemaphoreHandle 	*semaphoreHandle)
{
	HANDLE mutex = CreateMutex(NULL, FALSE, semaphoreName);
	assert (mutex != 0);
		
	*semaphoreHandle = mutex;

	return MCP_HAL_OS_STATUS_SUCCESS;
}


/*-------------------------------------------------------------------------------
 * MCP_HAL_OS_DestroySemaphore()
 *
 */
McpHalOsStatus MCP_HAL_OS_DestroySemaphore(McpHalOsSemaphoreHandle *semaphoreHandle)
{
	HANDLE mutex = (HANDLE)(*semaphoreHandle);
	
	BOOL result = CloseHandle(mutex);
	assert(result != 0);

	*semaphoreHandle = NULL;
	
	return MCP_HAL_OS_STATUS_SUCCESS;
}


/*-------------------------------------------------------------------------------
 * MCP_HAL_OS_LockSemaphore()
 *
 */
McpHalOsStatus MCP_HAL_OS_LockSemaphore(McpHalOsSemaphoreHandle 	semaphoreHandle,
													McpHalOsTimeInMs 		timeout)
{
	HANDLE 	mutex = (HANDLE)semaphoreHandle;
	DWORD 	result;
	DWORD	winTimeout;

	if (timeout != MCP_HAL_OS_TIME_INFINITE)
	{
		winTimeout = (DWORD)timeout;
	}
	else
	{
		winTimeout = INFINITE;
	}

	result = WaitForSingleObject(mutex, winTimeout);
	
	if (result == WAIT_OBJECT_0)
	{
		return MCP_HAL_OS_STATUS_SUCCESS;
	}
	else if (result == WAIT_TIMEOUT)
	{
		return MCP_HAL_OS_STATUS_TIMEOUT;
	}
	else
	{
		assert(0);
	}
	
	return MCP_HAL_OS_STATUS_SUCCESS;
}
	

/*-------------------------------------------------------------------------------
 * MCP_HAL_OS_UnlockSemaphore()
 *
 */
McpHalOsStatus MCP_HAL_OS_UnlockSemaphore(McpHalOsSemaphoreHandle semaphoreHandle)
{
	HANDLE 	mutex = (HANDLE)semaphoreHandle; 
	BOOL	result;
	
	result = ReleaseMutex(mutex);
	assert(result != 0);
	
	return MCP_HAL_OS_STATUS_SUCCESS;
}


/*-------------------------------------------------------------------------------
 * MCP_HAL_OS_Sleep()
 *
 */
McpHalOsStatus MCP_HAL_OS_Sleep(McpHalOsTimeInMs time)
{
	Sleep(time);
	
	return MCP_HAL_OS_STATUS_SUCCESS;
}

/*-------------------------------------------------------------------------------
 * MCP_HAL_OS_SleepMicroSec()
 *
 */
McpHalOsStatus MCP_HAL_OS_SleepMicroSec(McpUint time)
{
	MCPF_UNUSED_PARAMETER(time);
	
	/* Function not supported in WinXP */
	return MCP_HAL_OS_STATUS_FAILED;
}

McpHalOsTimeInMs MCP_HAL_OS_GetSystemTime(void)
{
	return timeGetTime();
}

/*-------------------------------------------------------------------------------
 * os_memoryAlloc()
 *
 */
void *os_memoryAlloc (handle_t OsContext, McpU32 Size)
{
	_MCP_HAL_OS_UNUSED_PARAMETER(OsContext);
	return malloc(Size);
}

/*-------------------------------------------------------------------------------
 * os_memorySet()
 *
 */
void os_memorySet (handle_t OsContext, void *pMemPtr, McpS32 Value, McpU32 Length)
{
	_MCP_HAL_OS_UNUSED_PARAMETER(OsContext);
	memset(pMemPtr, Value, Length);
}

/*-------------------------------------------------------------------------------
 * os_memoryZero()
 *
 */
void os_memoryZero (handle_t OsContext, void *pMemPtr, McpU32 Length)
{
	_MCP_HAL_OS_UNUSED_PARAMETER(OsContext);
	memset(pMemPtr, 0, Length);
}

/*-------------------------------------------------------------------------------
 * os_memoryFree()
 *
 */
void os_memoryFree (handle_t OsContext, void *pMemPtr, McpU32 Size)
{
	_MCP_HAL_OS_UNUSED_PARAMETER(OsContext);
	_MCP_HAL_OS_UNUSED_PARAMETER(Size);
	free(pMemPtr);
}

/*-------------------------------------------------------------------------------
 * os_memoryCopy()
 *
 */
void os_memoryCopy (handle_t OsContext, void *pDestination, void *pSource, McpU32 Size)
{
	_MCP_HAL_OS_UNUSED_PARAMETER(OsContext);
	memcpy(pDestination, pSource, Size);
}

/*-------------------------------------------------------------------------------
 * os_timerStart()
 *
 */
McpUint os_timerStart(McpUint expiry_time,	mcpf_timer_cb cb)
{
	return SetTimer(0, 0, expiry_time, (TIMERPROC)cb);
}

/*-------------------------------------------------------------------------------
 * os_timerStop()
 *
 */
McpBool os_timerStop(McpUint timer_id)
{
	return KillTimer(0, timer_id);
}

/*-------------------------------------------------------------------------------
 * os_printf()
 *
 */
void os_printf (const char *format ,...)
{
	va_list args;

	va_start (args, format);
	vprintf (format, args);
	va_end (args);
}

/*-------------------------------------------------------------------------------
 * MCP_HAL_OS_COS()
 *
*/
McpDBL MCP_HAL_OS_COS(McpDBL A)
{
	return cos(A);
}

/*-------------------------------------------------------------------------------
 * MCP_HAL_OS_SIN()
 *
*/
McpDBL MCP_HAL_OS_SIN(McpDBL A)
{
	return sin(A);
}

/*-------------------------------------------------------------------------------
 * MCP_HAL_OS_ATAN2()
 *
*/
McpDBL	MCP_HAL_OS_ATAN2(McpDBL A, McpDBL B)
{
	return atan2(A,B);
}

/*-------------------------------------------------------------------------------
 * MCP_HAL_OS_SQRT()
 *
*/
McpDBL	 MCP_HAL_OS_SQRT(McpDBL A)
{
	return sqrt(A);
}

/*-------------------------------------------------------------------------------
 * MCP_HAL_OS_FABS()
 *
*/
McpDBL	 MCP_HAL_OS_FABS(McpDBL A)
{
	return fabs(A);
}

/*-------------------------------------------------------------------------------
 * MCP_HAL_OS_POW()
 *
*/
McpDBL	 MCP_HAL_OS_POW(McpDBL A,McpDBL B)
{
	return pow(A,B);
}

