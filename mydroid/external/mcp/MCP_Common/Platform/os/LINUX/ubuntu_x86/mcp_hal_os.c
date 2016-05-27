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
#define _GNU_SOURCE /* needed for PTHREAD_MUTEX_RECURSIVE */

#include <unistd.h>
#include <pthread.h>
#include <stdarg.h>
#include <stdio.h>
#include <sys/time.h>
#include <time.h>
#include <osapi.h>
#include <errno.h>
#include <string.h>
#include <memory.h>
#include "mcp_defs.h"


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

/* A define to convert asserts to OS_Assert calls */
#undef Assert
#define Assert(exp)  (((exp) != 0) ? (void)0 : MCP_HAL_MISC_Assert(#exp,__FILE__,(U16)__LINE__))



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
	int rc;
	pthread_mutexattr_t mutex_attr;
	MCP_UNUSED_PARAMETER(semaphoreName);


	*semaphoreHandle = (pthread_mutex_t *)malloc(sizeof(pthread_mutex_t));
	Assert(*semaphoreHandle != NULL);

	/* we use recursive mutex - a mutex owner can relock the mutex. to release it,
	     the mutex shall be unlocked as many times as it was locked
	  */
	rc = pthread_mutexattr_init(&mutex_attr);
	if(rc != 0)
	{
		MCP_HAL_LOG_INFO(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_OS, ("MCP_HAL_OS_CreateSemaphore | pthread_mutexattr_init() failed: %s", strerror(rc)));
		return MCP_HAL_OS_STATUS_FAILED;
	}

	rc = pthread_mutexattr_settype( &mutex_attr, PTHREAD_MUTEX_RECURSIVE);
	if(rc != 0)
	{
		MCP_HAL_LOG_INFO(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_OS, ("MCP_HAL_OS_CreateSemaphore | pthread_mutexattr_settype() failed: %s", strerror(rc)));
		pthread_mutexattr_destroy(&mutex_attr);
		return MCP_HAL_OS_STATUS_FAILED;
	}

	rc = pthread_mutex_init(*semaphoreHandle, &mutex_attr);
	if(rc != 0)
	{
		MCP_HAL_LOG_INFO(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_OS, ("MCP_HAL_OS_CreateSemaphore | pthread_mutex_init() failed: %s", strerror(rc)));
		pthread_mutexattr_destroy(&mutex_attr);
		return MCP_HAL_OS_STATUS_FAILED;
	}

	rc = pthread_mutexattr_destroy(&mutex_attr);
	if(rc != 0)
	{
		MCP_HAL_LOG_INFO(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_OS, ("MCP_HAL_OS_CreateSemaphore | pthread_mutexattr_destroy() failed: %s", strerror(rc)));
		pthread_mutex_destroy(*semaphoreHandle);
		return MCP_HAL_OS_STATUS_FAILED;
	}


	return MCP_HAL_OS_STATUS_SUCCESS;
}


/*-------------------------------------------------------------------------------
 * MCP_HAL_OS_DestroySemaphore()
 *
 */
McpHalOsStatus MCP_HAL_OS_DestroySemaphore(McpHalOsSemaphoreHandle *semaphoreHandle)
{
	int rc;


	MCP_HAL_LOG_INFO(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_OS, ("MCP_HAL_OS_DestroySemaphore: killing semaphore"));

	rc = pthread_mutex_destroy(*semaphoreHandle);
	*semaphoreHandle = NULL;
	if (rc != 0)
	{
		MCP_HAL_LOG_INFO(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_OS, ("MCP_OS_DestroySemaphore | pthread_mutex_destroy() failed: %s", strerror(rc)));
		return MCP_HAL_OS_STATUS_FAILED;
	}

	return MCP_HAL_OS_STATUS_SUCCESS;
}

/*-------------------------------------------------------------------------------
 * MCP_HAL_OS_LockSemaphore()
 *
 */
McpHalOsStatus MCP_HAL_OS_LockSemaphore(McpHalOsSemaphoreHandle 	semaphoreHandle,
													McpHalOsTimeInMs 		timeout)
{
	int rc;

	if (timeout != MCP_HAL_OS_TIME_INFINITE)
	{
		Assert("finite locking time not supported yet");
	}

	rc = pthread_mutex_lock(semaphoreHandle);
	if (rc != 0)
	{
		MCP_HAL_LOG_INFO(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_OS, ("MCP_HAL_OS_LockSemaphore: pthread_mutex_lock() failed: %s", strerror(rc)));
		return MCP_HAL_OS_STATUS_FAILED;
	}

	return MCP_HAL_OS_STATUS_SUCCESS;
}


/*-------------------------------------------------------------------------------
 * MCP_HAL_OS_UnlockSemaphore()
 *
 */
McpHalOsStatus MCP_HAL_OS_UnlockSemaphore(McpHalOsSemaphoreHandle semaphoreHandle)
{
	int rc;

	rc = pthread_mutex_unlock(semaphoreHandle);
	if(rc != 0)
	{
		MCP_HAL_LOG_INFO(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_OS, ("MCP_HAL_OS_UnlockSemaphore: pthread_mutex_unlock() failed: %s", strerror(rc)));
		return MCP_HAL_OS_STATUS_FAILED;
	}

	return MCP_HAL_OS_STATUS_SUCCESS;
}


/*-------------------------------------------------------------------------------
 * MCP_HAL_OS_Sleep()
 *
 */
McpHalOsStatus MCP_HAL_OS_Sleep(McpHalOsTimeInMs time)
{
	// time is in milliseconds
	if (0 == usleep(time * 1000))
	{
		return MCP_HAL_OS_STATUS_SUCCESS;
	}

	MCP_HAL_LOG_INFO(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_OS, ("BTHAL_OS_Sleep | usleep() failed: %s", strerror(errno)));
	return MCP_HAL_OS_STATUS_FAILED;
}

/*-------------------------------------------------------------------------------
 * MCP_HAL_OS_SleepMicroSec()
 *
 */
McpHalOsStatus MCP_HAL_OS_SleepMicroSec(McpUint time)
{
	// time is in microseconds
	usleep(time);
	return MCP_HAL_OS_STATUS_SUCCESS;
}

McpHalOsTimeInMs MCP_HAL_OS_GetSystemTime(void)
{
	int rc;
	// should return the time since system started, in millisec
	struct timeval tv;

	rc = gettimeofday( &tv, 0 );
	if (rc == 0)
	{
		return tv.tv_sec * 1000 + tv.tv_usec / 1000;
	}

	MCP_HAL_LOG_INFO(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_OS, ("MCP_HAL_OS_GetSystemTime | gettimeofday() failed: %s", strerror(errno)));
	return 0;
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
	_MCP_HAL_OS_UNUSED_PARAMETER(expiry_time);
	_MCP_HAL_OS_UNUSED_PARAMETER(cb);
	/* not required yet */
	Assert(0);
    return 0;
}

/*-------------------------------------------------------------------------------
 * os_timerStop()
 *
 */
McpBool os_timerStop(McpUint timer_id)
{
	_MCP_HAL_OS_UNUSED_PARAMETER(timer_id);
	/* not required yet */
	Assert(0);
    return MCP_TRUE;
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
