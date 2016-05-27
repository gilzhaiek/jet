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
Dmitriy  Kardakov       20.12.2008	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#include "gps/LinGPS.h"

using namespace Engine;

//extern GPSCallBack __gpscallback;
typedef bool_t (*GPSCallBack) (Engine::uint16_t,Engine::uint32_t,void*,Engine::uint32_t);

namespace Platform {

CLinGPS::CLinGPS()
{
	pthread_mutex_init(&hCriticalSection, NULL);
}

CLinGPS::~CLinGPS()
{	
	pthread_mutex_destroy(&hCriticalSection);
}

void CLinGPS::Lock()
{
	pthread_mutex_lock(&hCriticalSection);
}

void CLinGPS::Unlock()
{
	pthread_mutex_unlock(&hCriticalSection);
}

bool_t CLinGPS::SendData(CGPSCommand* cmd, void* data)
{	
	return  CSUPLController::GetInstance().gps_callback(cmd->GetAppID(), cmd->GetCmdCode(), data);
}

}
