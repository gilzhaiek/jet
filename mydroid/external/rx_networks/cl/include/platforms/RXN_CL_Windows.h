/*
 * Copyright (c) 2007-2011 Rx Networks, Inc. All rights reserved.
 *
 * Property of Rx Networks
 * Proprietary and Confidential
 * Do NOT distribute
 * 
 * Any use, distribution, or copying of this document requires a 
 * license agreement with Rx Networks. 
 * Any product development based on the contents of this document 
 * requires a license agreement with Rx Networks. 
 * If you have received this document in error, please notify the 
 * sender immediately by telephone and email, delete the original 
 * document from your electronic files, and destroy any printed 
 * versions.
 *
 * This file contains sample code only and the code carries no 
 * warranty whatsoever.
 * Sample code is used at your own risk and may not be functional. 
 * It is not intended for commercial use.   
 *
 * Example code to illustrate how to integrate Rx Networks PGPS 
 * System into a client application.
 *
 * The emphasis is in trying to explain what goes on in the software,
 * and how to the various API functions relate to each other, 
 * rather than providing a fully optimized implementation.
 *
 *************************************************************************
 * $LastChangedDate: 2009-03-02 15:53:19 -0800 (Mon, 02 Mar 2009) $
 * $Revision: 9396 $
 *************************************************************************
 *
 */

#ifndef RXN_CL_WINDOWS_H
#define RXN_CL_WINDOWS_H

#ifdef __cplusplus
extern "C" {
#endif

#if defined (WIN32)
#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0501 /* Required for TryEnterCriticalSection */
#endif
#include <windows.h>        /* Required for Win32 functions. */
#endif

#define LOCK            CRITICAL_SECTION
#define EVENT			HANDLE
#define THREAD 			HANDLE
#define snprintf        _snprintf

BOOL CL_StartWinsock();

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* RXN_CL__WINDOWS_H */
