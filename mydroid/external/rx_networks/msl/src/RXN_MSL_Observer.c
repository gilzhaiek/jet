/*
* Copyright (c) 2011-2012 Rx Networks, Inc. All rights reserved.
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
* $LastChangedDate: 2009-04-17 13:53:14 -0700 (Fri, 17 Apr 2009) $
* $Revision: 10192 $
*************************************************************************
*
*/

#include <stdlib.h>
#include <assert.h>

#include "RXN_MSL_Observer.h"
#include "RXN_MSL_Common.h"
#include "RXN_MSL_Platform.h"

void MSL_AddObserver(RXN_MSL_Observer_t** observerList, void* callback)
{
    RXN_MSL_Observer_t* ptr = NULL;
    RXN_MSL_Observer_t* newNode = NULL;

    assert(observerList);
    assert(callback);

    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE01, 
            "MSL_AddObserver: Adding observer");

    MSL_EnterBlock(RXN_MSL_CS_Observer);

    /* First check to see if the callback has already been added */
    ptr = *observerList;
    while (ptr)
    {
        if (ptr->callback == callback)
        {
            MSL_LeaveBlock(RXN_MSL_CS_Observer);
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_WARNING, RXN_LOG_ZONE01, 
                    "MSL_AddObserver: Observer already exists.  Nothing added.");
            return;
        }

        ptr = ptr->next;
    }

    /* Create a new node and add it to the list */
    newNode = malloc(sizeof(RXN_MSL_Observer_t));
    if (newNode)
    {
        newNode->next = *observerList;
        newNode->callback = callback;
        *observerList = newNode;
    }

    MSL_LeaveBlock(RXN_MSL_CS_Observer);
}

void MSL_RemoveObserver(RXN_MSL_Observer_t** observerList, void* callback)
{
    RXN_MSL_Observer_t* ptr = NULL;
    RXN_MSL_Observer_t* prev = NULL;

    assert(observerList);
    assert(callback);

    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE01, 
            "MSL_RemoveObserver: Removing observer");

    MSL_EnterBlock(RXN_MSL_CS_Observer);

    ptr = *observerList;
    prev = NULL;
    while (ptr)
    {
        if (ptr->callback == callback)
        {
            /* If we are deleting part way into the list, reconnect the links.
               Otherwise if we are deleting the first element, change the observerList
               to point to the second element */
            if (prev)
            {
                prev->next = ptr->next;
            }
            else
            {
                *observerList = ptr->next;
            }
            free(ptr);
            MSL_LeaveBlock(RXN_MSL_CS_Observer);
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE01, 
                    "MSL_RemoveObserver: Observer removed");
            return;
        }

        prev = ptr;
        ptr = ptr->next;
    }
    MSL_LeaveBlock(RXN_MSL_CS_Observer);

    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_WARNING, RXN_LOG_ZONE01, 
            "MSL_RemoveObserver: Observer not found");
}

U32 MSL_ObserverCount(RXN_MSL_Observer_t* observerList)
{
    const RXN_MSL_Observer_t* ptr = NULL;
    U32 count = 0;
    MSL_EnterBlock(RXN_MSL_CS_Observer);
    ptr = observerList;
    while (ptr)
    {
        count++;
        ptr = ptr->next;
    }
    MSL_LeaveBlock(RXN_MSL_CS_Observer);
    return count;
}

