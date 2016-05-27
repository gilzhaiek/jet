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

#ifndef RXN_CL_LINUX_H
#define RXN_CL_LINUX_H

#ifdef __cplusplus
extern "C" {
#endif

#include <sys/socket.h>             /* Required for socket support */
#include <linux/un.h>
#include <netinet/in.h>             /* Required for socket support */
#include <arpa/inet.h>              /* Required for socket support */
#include <sys/select.h>             /* Definitions used for the select function */
#include <errno.h>                  /* Required for error handling */

#include <pthread.h>                /* Used for thread support */

#define LOCK            pthread_mutex_t
#define EVENT			pthread_cond_t
#define THREAD 			pthread_t

#define SOCKET 			int
#define SOCKET_ERROR    -1
#define INVALID_SOCKET  -1
    
#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* RXN_CL_LINUX_H */
