/**
*  @file  NetworkComponent.cpp
*  @brief NetworkComponent definitions
*/

/***************************************************************************************************
====================================================================================================

Copyright (c) 2007 Teleca Inc. All rights reserved.
Redistribution and modifications are permitted subject to BSD license. 

====================================================================================================
Revision History:
Modification     Tracking
Author                          Date          Number    Description of Changes
-------------------------   ------------    ----------  --------------------------------------------
Ildar Abdullin				 25.11.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#include "network/NetworkComponent.h"

//queue<CNetworkComponentBase::RawData_t*> CNetworkComponentBase::nw_queue;
Queue<CNetworkComponentBase::RawData_t*> CNetworkComponentBase::nw_queue; //Android STL
pthread_mutex_t CNetworkComponentBase::cs = {0};

CNetworkComponentBase::~CNetworkComponentBase()
{
	Lock();
	ClearIncomingQueue();
	UnLock();
}

void CNetworkComponentBase::Lock()
{
#if defined(_LINUX_)
	pthread_mutex_lock(&cs);
#endif

#if defined(WIN32) || defined(WINCE)
	EnterCriticalSection(&cs);
#endif
}

void CNetworkComponentBase::UnLock()
{
#if defined(_LINUX_)
	pthread_mutex_unlock(&cs);
#endif

#if defined(WIN32) || defined(WINCE)
	LeaveCriticalSection(&cs);
#endif
}

void CNetworkComponentBase::ClearIncomingQueue()
{
	RawData_t* raw_data = NULL;
	for(unsigned int i=0; i < nw_queue.size(); i++)
	{
		raw_data = nw_queue.front();
		delete raw_data;
		nw_queue.pop();
	}
}
