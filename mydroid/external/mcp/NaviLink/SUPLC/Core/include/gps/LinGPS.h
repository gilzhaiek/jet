/**
*  @file  LinGPS.h
*  @brief LinGPS declaration.
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
Dmitriy Kardakov         20.12.2008	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#ifndef _LIN_GPS_H_
#define _LIN_GPS_H_

//#include <map>
#include "android/ags_queue.h" //Android STL
#include "gps/GPS.h"
#include "suplcontroller/SUPLController.h"
#include "android/_linux_specific.h"

namespace Platform {

class CGPSCommand;

class CLinGPS : public IGPS
{
private:
	//HANDLE hGPS;
	pthread_t hThread;
	pthread_mutex_t  hCriticalSection;
public:
	 CLinGPS();
	~CLinGPS();
public:

	virtual void Lock();
	virtual void Unlock();

public:	
	virtual Engine::bool_t	SendData(CGPSCommand*, void*);

};


} // end of namespace Platform

#endif // _LIN_GPS_H_
