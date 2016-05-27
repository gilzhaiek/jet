/**
*  @file  WAPListener.cpp
*  @brief WAP listener definitions
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

// TCHAR const CWinWAPListener::c_szAppId[]=TEXT("x-oma-application:ulp.ua");
// TCHAR const CWinWAPListener::c_szPath[]=TEXT("TestMain.exe");
// TCHAR const CWinWAPListener::c_szParams[]=TEXT("");
// TCHAR const CWinWAPListener::c_szContentType[]=TEXT("application/vnd.omaloc-supl-init");

CLinWAPListener::CLinWAPListener(/*unsigned char Conf*/)
{
//	WapPush_Configuration = Conf;
}

CLinWAPListener::~CLinWAPListener()
{}

int CLinWAPListener::Init()
{
	return 0;
}

int CLinWAPListener::Open()
{
	if (startWAPListening() < 0) 
		return -1;
	else
		return 0;
}
/*int CLinWAPListener::HandleMessage()
{
	return 0;
}*/
int CLinWAPListener::HandleMessage(char * body, int size)
{

	if (body == NULL || size < 0)
		return -1;
	
	CNetworkComponentBase::Lock();

	if(CSUPLController::GetInstance().SUPLStart() != TRUE)
	{
		CNetworkComponentBase::UnLock();
		return 0;
	}
	CNetworkComponentBase::UnLock();
	
	//Copy raw data to SUPL queue
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


void CLinWAPListener::Close()
{
	stopWAPListening();
}

#endif //_LINUX_
