/**
*  @file  NetListener.h
*  @brief listeners class declarations
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

#ifndef _NET_LISTENER_H
#define _NET_LISTENER_H

//#if defined(WIN32) || defined(WINCE)
//#include <windows.h>


//#ifdef WINCE
//#include <wap.h>
//#include <Pushclient.h>
//#endif

//#endif // WIN32  || WINCE
#include "common/types.h"
#include "NetworkComponent.h"
#include "android/ags_jni_interface.h"

class CListenerBase
{
public:
	CListenerBase() {};
	virtual ~CListenerBase() {};
	virtual int  Init()=0;
	virtual int  Open()=0;
  virtual int  HandleMessage(char * body, int size)=0; //calls from native library
	virtual void Close()=0;
};

class CWinNetListener;

class CNetListener : public CNetworkComponentBase
{
public:
	CNetListener() {};
	virtual ~CNetListener() {};
	virtual int StartListen()=0;
	CListenerBase* wap_listener1;
	CListenerBase* wap_listener2;
	CListenerBase* sms_listener; 
	static CNetListener* CreateListener();
};


class CLinNetListener : public CNetListener
{
public:
	CLinNetListener();
	~CLinNetListener();
	int StartListen();
private:
	void FreeResources();
};

class CListenerBase;

class CLinSMSListener : public CListenerBase
{
public:
	CLinSMSListener();
	~CLinSMSListener();
	int  Init();
	int  Open();
	int HandleMessage(char * body, int size); //calls from native library
	void Close();
private:

/*#ifdef WINCE
	const WAP_LAYER wlLayer;
	const DWORD dwRecvPort;
	WAP_HANDLE hWapRecv;
#endif*/
};

class CLinWAPListener : public CListenerBase
{
public:
	//unsigned char WapPush_Configuration;
// 	TCHAR const c_szAppId[];
// 	TCHAR const c_szPath[];
// 	TCHAR const c_szParams[];
// 	TCHAR const c_szContentType[];
public:
	CLinWAPListener();
	CLinWAPListener(unsigned char Conf);
	~CLinWAPListener();
	int  Init();
	int  Open();
	int HandleMessage(char * body, int size); //calls from native library
	void Close();
private:

//#ifdef WINCE
//	HPUSHROUTER hPushRouter;
//	PUSHMSG PushMsg;
//#endif
};

#endif //ifndef _NET_LISTENER_H
