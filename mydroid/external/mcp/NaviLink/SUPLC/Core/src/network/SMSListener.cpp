/**
*  @file  SMSListener.cpp
*  @brief SMS listener definitions
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
#if defined(_LINUX_)
#include "suplcontroller/SUPLController.h"
#include "network/NetListener.h"
#include "android/ags_jni_interface.h"
#include <utils/Log.h>
using namespace Engine;

CLinSMSListener::CLinSMSListener() 
{
}

CLinSMSListener::~CLinSMSListener()
{
}

int CLinSMSListener::Init()
{
	return 0;
}

int CLinSMSListener::Open()
{	
	if (startSMSListening() < 0) 
		return -1;
	else
		return 0;
}

/*int CLinSMSListener::HandleMessage()
{
	return 0;
}*/
int CLinSMSListener::HandleMessage(char * body, int size)
{
	LOGD("+CLinSMSListener::HandleMessage");
	if (body == NULL || size < 0)
		return -1;
	
	CNetworkComponentBase::Lock();

	if(CSUPLController::GetInstance().SUPLStart() != TRUE) 
	{
		CNetworkComponentBase::UnLock();
		return 0;
	}
	CNetworkComponentBase::UnLock();
	
	//Copy WDP data to SUPL queue
	CNetworkComponentBase::RawData_t* data=new(nothrow) CNetworkComponentBase::RawData_t; 
	if(!data)
	{
		return -1;
	}
	
	data->RawData = new(nothrow) unsigned char[size];
	if(!data->RawData)
	{
		delete data;
		return -1;
	}
	
	memcpy(data->RawData,body,size);
	data->size = size;
	CNetworkComponentBase::Lock();
	CNetworkComponentBase::nw_queue.push(data);
	CNetworkComponentBase::UnLock();

	return 0;
}

void CLinSMSListener::Close()
{
	stopSMSListening();
}

#endif //_LINUX_

