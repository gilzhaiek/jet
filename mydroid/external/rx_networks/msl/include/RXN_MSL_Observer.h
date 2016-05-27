/*
 * Copyright (c) 2012 Rx Networks, Inc. All rights reserved.
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
 * $LastChangedDate: 2009-03-23 14:19:48 -0700 (Mon, 23 Mar 2009) $
 * $Revision: 9903 $
 *************************************************************************
 */

#ifndef RXN_MSL_OBSERVER_H
#define RXN_MSL_OBSERVER_H

#ifdef __cplusplus
extern "C" {
#endif

#include "RXN_data_types.h"

typedef struct RXN_MSL_Observer
{
    struct RXN_MSL_Observer* next;
    void* callback;

} RXN_MSL_Observer_t;

/* MSL_AddObserver:
 * Adds the callback to the specifid observer list.  The callback will not be added if
 * it has previously been added to the list.
 */
void MSL_AddObserver(RXN_MSL_Observer_t** observerList, void* callback);

/* MSL_RemoveObserver:
 * Removes the callback from the specified observer list.  Once the first callback is
 * removed the search terminated.  This is becauase MSL_AddObserver() enforces the constraint
 * that a callback can be added at most once.
 */
void MSL_RemoveObserver(RXN_MSL_Observer_t** observerList, void* callback);

/* MSL_ObserverCount:
 * Counts the number of observers in an observer list.
 */
U32 MSL_ObserverCount(RXN_MSL_Observer_t* observerList);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif 
