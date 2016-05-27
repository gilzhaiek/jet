/**
*  @file  StopReqCmd.cpp
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

#include "gps/StopReqCmd.h"
#include "utils/Log.h"

namespace Platform {



CStopReqCmd::CStopReqCmd(Engine::uint16_t appId, Engine::CSession* ses, Engine::MSG* msg, void* data):
	CGPSCommand(appId, ses, msg, data)
{

}

CStopReqCmd::~CStopReqCmd()
{

}

Engine::bool_t CStopReqCmd::Execute()
{
	StopReqData* dta = (StopReqData*)this->data;
	if(!dta)
	{
		return FALSE;
	}
	if(dta->gps_stoped == SUCCESS)
	{
		free(dta);
		return TRUE;
	}
	LOGD("CStopReqCmd: dta->app_id = %d dta->gps_stoped = %d\n",dta->app_id,dta->gps_stoped);
	
	//Engine::CSUPLController::GetInstance().CreateFailureResponse(dta->app_id);

	session=Engine::CSUPLController::GetInstance().GetSession(dta->app_id);
	if(session)
	{
		Engine::CSUPLController::GetInstance().StopSession(dta->app_id,Engine::UNSPECIFIED);
	}
	else		//session->m_GPSResult = Engine::CSession::GPSResult::FAILURE;
		LOGD("CStopReqCmd: session = %d\n",session);
	
	free(dta);
	
	return TRUE;
}

Engine::uint64_t CStopReqCmd::GetCmdCode()
{
	return UPL_STOP_REQ;
}

};
