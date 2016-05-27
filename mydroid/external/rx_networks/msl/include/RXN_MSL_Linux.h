/*
 * Copyright (c) 2011 Rx Networks, Inc. All rights reserved.
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

#ifndef RXN_MSL_LINUX_H
#define RXN_MSL_LINUX_H

#include <pthread.h>

#define SOCKET 			int
#define SOCKET_ERROR    -1
#define INVALID_SOCKET  -1
#define MSL_OS_WOULDBLOCK 	EAGAIN
#define MSL_OS_INPROGRESS 	EINPROGRESS
#define EVENT			char
#define NULLEVENT		0
#define THREAD pthread_t 

enum MSL_REQUEST_EVENTS {
	MSL_UPDATE_CLK_REQUEST_EVENT = 'c',
	MSL_UPDATE_SEED_REQUEST_EVENT = 's',	
	MSL_SNTP_TIME_REQUEST_EVENT = 't',
	MSL_SEND_DEVICE_ID_REQUEST_EVENT = 'i',
	MSL_EXIT_MSG_SOCKET_THREAD_EVENT = 'x',
    MSL_XYBRID_MESSAGE_EVENT = 'p',
	MSL_RXNSERVICES_TERMINATION_EVENT = 'z',
    MSL_REQUEST_EVENT_MAX
};

void MSL_CreatePipe();


#include <stdio.h>
#include <netdb.h>
#include <unistd.h>
#include <errno.h>
#include <netinet/in.h>

#endif

