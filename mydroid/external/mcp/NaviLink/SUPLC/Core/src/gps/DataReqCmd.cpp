/**
*  @file  DataReqCmd.cpp
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

#include "android/ags_androidlog.h"
#include "gps/DataReqCmd.h"
#include <new>

namespace Platform {
	
CDataReqCmd::CDataReqCmd(Engine::uint16_t appId, Engine::CSession* ses, Engine::MSG* msg,void* dta):
	CGPSCommand(appId, ses, msg, dta)
{	

}

CDataReqCmd::~CDataReqCmd()
{

}

Engine::bool_t CDataReqCmd::Execute()
{
	DataReqData* dta = (DataReqData*) this->data;
	debugMessage("%s START", __FUNCTION__);
	if (dta == NULL)
	{
		return FALSE;
	}

	dta->app_id = 1001;
	appId = dta->app_id;

	session=Engine::CSUPLController::GetInstance().GetSession(dta->app_id);

	if(session == NULL)
	{
		debugMessage("%s session == NULL free(dta)!", __FUNCTION__);
		free(dta);
		dta = NULL;
		return FALSE;
	}
    else
	   debugMessage("Session id = %d", session->m_AppID);
	if(dta->pos_payload.payload.ctrl_pdu)
	{
		debugMessage("%s dta->pos_payload.payload.ctrl_pdu != 0, size = %d", __FUNCTION__, dta->pos_payload.payload.ctrl_pdu_len);
		//debugMessage("m_PendingMessage = %d", session->m_PendingMessage);
		//debugMessage("m_GPSData = %d", session->m_PendingMessage->m_GPSData);
		session->m_PendingMessage->m_GPSData.m_pdu = new(std::nothrow)  Engine::uint8_t[dta->pos_payload.payload.ctrl_pdu_len];
		if(!session->m_PendingMessage->m_GPSData.m_pdu)
		{
			free(dta);
			dta = NULL;
			return FALSE;
		}
		session->m_PendingMessage->m_GPSData.m_pdu_size = dta->pos_payload.payload.ctrl_pdu_len;
		debugMessage("%s Try copy %d bytes... ", __FUNCTION__, dta->pos_payload.payload.ctrl_pdu_len);
		memcpy(session->m_PendingMessage->m_GPSData.m_pdu,dta->pos_payload.payload.ctrl_pdu,dta->pos_payload.payload.ctrl_pdu_len);
		debugMessage("%s bytes was coped", __FUNCTION__);
		//free(dta);
		debugMessage(" ===>>> OUTSIDE FREE \n");
	}
	else
	{
		debugMessage("%s dta->pos_payload.payload.ctrl_pdu == 0", __FUNCTION__);
		free(dta);
		dta = NULL;
		return FALSE;
	}

	session->m_PendingMessage->m_Status = Engine::MSG_READY_TO_PROCESS;
	//saving Application Id
	//session->m_AppID = dta->app_id;
	free(dta);
	dta = NULL;
	debugMessage("%s END", __FUNCTION__);
	
	return TRUE;
}

Engine::uint64_t CDataReqCmd::GetCmdCode()
{
	debugMessage("%s return UPL_DATA_REQ", __FUNCTION__);
	return UPL_DATA_REQ;
}

};
