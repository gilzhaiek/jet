/**
*  @file  NetListener.cpp
*  @brief Net listener definitions
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


#include "network/NetListener.h"
//#include <map>
//#include "suplcontroller/SUPLController.h"

//using namespace Engine;
//namespace Engine {
using namespace std;
CNetListener* CNetListener::CreateListener()
{
#if defined(_LINUX_)
	return new(nothrow) CLinNetListener;
#endif
}

CLinNetListener::CLinNetListener()
{
	sms_listener  = new(nothrow) CLinSMSListener;
	wap_listener1 = new(nothrow) CLinWAPListener;
	wap_listener2 = NULL;
}

CLinNetListener::~CLinNetListener()
{
#if defined(_LINUX_)
	FreeResources();
#endif
	delete sms_listener;
	delete wap_listener1;
}

int CLinNetListener::StartListen()
{
	int hResult = 0;
	
	hResult = sms_listener->Init();
	if(hResult < 0)
		return hResult;
		
	hResult = wap_listener1->Init();
	if(hResult < 0)
		return hResult;

	hResult = sms_listener->Open();
	if(hResult < 0)
		return hResult;
		
	hResult = wap_listener1->Open();
	if(hResult < 0)
		return hResult;
	
	return 0;
}

/*int CLinNetListener::HandleMessage(char * body, int size, int type)
{
	if (type == 1) 
		sms_listener->HandleMessage(body, size);
	
	if (type == 2)
		wap_listener1->HandleMessage(body, size);
}*/
void CLinNetListener::FreeResources()
{
	Lock();
	sms_listener->Close();
	wap_listener1->Close();
	wap_listener2 = NULL;
	UnLock();
	return;
}



