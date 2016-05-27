/**
*  @file  NetworkComponent.h
*  @brief network component base class
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

#ifndef _NETWORK_COMPONENT_H_
#define _NETWORK_COMPONENT_H_


#if defined(WIN32) || defined(WINCE)
#include <windows.h>
#endif

#if defined(_LINUX_)
#include "android/_linux_specific.h"
#endif

//#include <memory>
//#include <iostream>
//#include <queue>
#include "android/ags_queue.h" //Android STL
using namespace std;
using namespace android; //Android STL
#define NETWORK_BUFFER_SIZE 100 //100 bytes

class CNetworkComponentBase
{
public:
	class RawData_t
	{
	public:
		unsigned char* RawData;
		unsigned int size;
		RawData_t() : RawData(NULL)
		{
			/* KW changes*/
			size = 0;
		}
		~RawData_t()
		{
			if(RawData)
			{
				if(size > 1)
					delete[] RawData;
				else
					delete RawData;
			}
			RawData = NULL;
		}
	};
	//static	queue<RawData_t*> nw_queue;
	static	Queue<RawData_t*> nw_queue; //Android STL
	static void Lock();
	static void UnLock();
	static void ClearIncomingQueue();
private:
#if defined(WIN32) || defined(WINCE)
	static CRITICAL_SECTION cs;
#endif

#if defined(_LINUX_)
	static pthread_mutex_t cs;
#endif

protected:
	CNetworkComponentBase()
	{
		pthread_mutex_init(&cs, NULL);
	}
	virtual ~CNetworkComponentBase();
};

#endif //_NETWORK_COMPONENT_H_
