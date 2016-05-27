/**
*  @file  FailIndCmd.cpp
*  @brief Command declaration.
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
Evgenia I. Kobacheva         10.01.2008                   initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#include "gps/StopIndCmd.h"
#include "gps/DataUPL.h"
#include "session/Session.h"

namespace Platform {

CStopIndCmd::CStopIndCmd(Engine::uint16_t appId, Engine::CSession* ses, Engine::MSG* msg, void* data):
	CGPSCommand(appId, ses, msg, data)
{

}

CStopIndCmd::~CStopIndCmd()
{
}

Engine::bool_t CStopIndCmd::Execute()
{
	// this message goes from SUPL to GPSM	
	// get data
	StopIndData* dta = (StopIndData*)malloc(sizeof(StopIndData));
	if (dta == NULL)
	{
		return FALSE;
	}

	//saving Application Id
	dta->app_id = appId;

	//store status code
	if(data)
	{
		dta->gps_stoped = Platform::SUCCESS; 
	}
	else
	{
		dta->gps_stoped = Platform::FAILURE; 
	}

// 	if(session)
// 	{
// 		session->SetSessionStatus(Engine::SESSION_READY_TO_DIE);
// 	}

	// send out data
	Engine::CSUPLController::GetInstance().GetGPS().SendData(this, dta);

	return TRUE;
}

Engine::uint64_t CStopIndCmd::GetCmdCode()
{
	return UPL_STOP_IND;
}

};
