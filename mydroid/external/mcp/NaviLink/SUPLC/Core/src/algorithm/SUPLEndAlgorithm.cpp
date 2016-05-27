/**
*  @file  SUPLEndAlgorithm.cpp
*  @brief CSUPLEndAlgorithm  declaration.
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
Alexander V. Morozov         20.11.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#include <new>
#include "algorithm/SUPLEndAlgorithm.h"
#include <utils/Log.h>

#include "nav_log_msg.h"
#include "nav_log_cust_codes.h"

namespace Engine {

CSUPLEndAlgorithm::CSUPLEndAlgorithm()
{

}

CSUPLEndAlgorithm::~CSUPLEndAlgorithm()
{

}

bool_t CSUPLEndAlgorithm::Response(MSG* msg, CSession* session)

{
	SESSLOGMSG("[%s]%s:%d, %s \n",get_utc_time(),SESS_OTA_MSG_TYPE,nav_sess_getCount(0),"SUPL END");
	if((msg->m_pIncomingMessage->GetType() != Engine::SUPL_END) && !msg->m_pOutgoingMessage)
	{
		CSUPLEnd* end = NULL;
		end = new(std::nothrow)  CSUPLEnd;
		if(!end)
			return FALSE;
		msg->m_pOutgoingMessage = end;
		end->m_Lenght = 0;
		if(end->m_SessionID.Copy(session->iSessionID) != RETURN_OK)
			return FALSE;
		end->m_Version.m_Major = MAJ_VERSION;
		end->m_Version.m_Minor = MIN_VERSION;
		end->m_Version.m_Servind = SER_VERSION;
		if(session->m_pVer)
		{
			end->m_pVer = new(std::nothrow)  Engine::CBitString(64);
			if(!end->m_pVer)
				return FALSE;
			if(!end->m_pVer->IsObjectReady())
				return FALSE;
			if(end->m_pVer->Copy(*(session->m_pVer)) != RETURN_OK)
				return FALSE;
		}
		end->m_pPosition = NULL;
		end->m_pStatusCode = new(std::nothrow)  StatusCode;
		if(!end->m_pStatusCode)
			return FALSE;
		*(end->m_pStatusCode) = Engine::UNEXPECTED_MESSAGE;
		
		CreateNewResponse(this, msg)->Response(msg, session);
		return TRUE;
	}
// 	CSUPLEnd* end = NULL;

// 	if (msg->m_pIncomingMessage != NULL)
// 	{
// 		end = (CSUPLEnd*) msg->m_pIncomingMessage;
// 		/*
// 			this is SUPL_END from server
// 			process message
// 		*/
// 
// 		// GPS interface needed
// 	}
// 	else if (msg->m_pOutgoingMessage != NULL)
// 	{
// 		end = (CSUPLEnd*) msg->m_pOutgoingMessage;
// 		/*
// 			this is SUPL_END from client
// 			process data
// 		*/
// 		// GPS interface needed
// 		
// 		session->m_OutputData.m_Error = *end->m_pStatusCode;
// 	}
	
	// we switch algorithm in END state because we NETHER enter in this algorithm again.
	if(!CreateNewResponse(this, msg)->Response(msg, session))
		return FALSE;

	return TRUE;
}

CAlgorithmBase* CSUPLEndAlgorithm::CreateNewResponse(CAlgorithmBase* old, MSG* msg)
{
	CAlgorithmBase* NEW = new (old) CEndAlgorithm();
	//LOGD("CSUPLEndAlgorithm::CreateNewResponse - OLD = 0X%x, NEW = 0X%x", old, NEW);
	return NEW;
	//return new (old) CEndAlgorithm();	
}

bool_t CSUPLEndAlgorithm::PrepareErrorMessage(MSG* msg,Engine::CSessionID& SessionId,Engine::StatusCode code,Engine::CBitString** Ver)
{
	CSUPLEnd*  supl_end = new(std::nothrow)  CSUPLEnd;
	CSession* session = 0;
	LOGD(" SUPLEndAlgorithm.cpp: PrepareErrorMessage entering \n");
	if(!supl_end)
		return FALSE;

	msg->m_pOutgoingMessage=supl_end;

	if(SessionId.iSETSessionID != NULL)
	{
		msg->m_SETSessionID = SessionId.iSETSessionID->sessionId;
		LOGD(" SUPLEndAlgorithm.cpp: PrepareErrorMessage before getsession \n");
		session = CSUPLController::GetInstance().GetSession(SessionId.iSETSessionID->sessionId);
	}		

	if(supl_end->m_SessionID.Copy(SessionId) != RETURN_OK)
		return FALSE;

	supl_end->m_pPosition = NULL;
	if(Ver != NULL)
	{
		supl_end->m_pVer = *Ver;
		*Ver=NULL;
	}
	else
	{
		supl_end->m_pVer = NULL;
	}
	if(code != Engine::FAILURE)
	{
		supl_end->m_pStatusCode = new(std::nothrow)  Engine::StatusCode;
		if(!supl_end->m_pStatusCode)
                {

			return FALSE;
                }

		*(supl_end->m_pStatusCode) = code;

		
	}
	else
	{
		supl_end->m_pStatusCode = NULL;
	}

	supl_end->m_Lenght = 0;
	supl_end->m_Version.m_Major = MAJ_VERSION;
	supl_end->m_Version.m_Minor = MIN_VERSION;
	supl_end->m_Version.m_Servind = SER_VERSION;
	LOGD(" SUPLEndAlgorithm.cpp: PrepareErrorMessage before if(session) \n");
	if(session)
	{
		session->SetSessionStatus(Engine::SESSION_INACTIVE);
		LOGD(" SUPLEndAlgorithm.cpp: PrepareErrorMessage SetSessionStatus \n");
		if(session->m_pVer && !Ver)
		{
			supl_end->m_pVer = session->m_pVer;
			session->m_pVer = NULL;
			/*supl_end->m_pVer = new(std::nothrow)  Engine::CBitString(64);
			if(!supl_end->m_pVer)
				return FALSE;
			if(!supl_end->m_pVer->IsObjectReady())
				return FALSE;
			if(supl_end->m_pVer->Copy(*(session->m_pVer)) != RETURN_OK)
				return FALSE;*/
		}
	}
	
	msg->m_Status = Engine::MSG_READY_TO_ENCODE;
	LOGD(" SUPLEndAlgorithm.cpp: PrepareErrorMessage leaving \n");
	return TRUE;
}

bool_t CSUPLEndAlgorithm::PrepareErrorMessage(MSG* msg,Engine::StatusCode code,Engine::CBitString** Ver)
{
	CSUPLEnd*  supl_end = new(std::nothrow)  CSUPLEnd;
	LOGD(" SUPLEndAlgorithm.cpp: PrepareErrorMessage entering(2) \n");
	if(!supl_end)
		return FALSE;

	msg->m_pOutgoingMessage=supl_end;

	supl_end->m_pPosition = NULL;
	supl_end->m_pVer = *Ver;
	*Ver=NULL;
	if(code != FAILURE)
	{
		supl_end->m_pStatusCode = new(std::nothrow)  Engine::StatusCode;
		if(!supl_end->m_pStatusCode)
			return FALSE;
		*(supl_end->m_pStatusCode) = code;
	}
	else
	{
		supl_end->m_pStatusCode = NULL;
	}

	supl_end->m_Lenght = 0;
	supl_end->m_Version.m_Major = MAJ_VERSION;
	supl_end->m_Version.m_Minor = MIN_VERSION;
	supl_end->m_Version.m_Servind = SER_VERSION;

	msg->m_Status = Engine::MSG_READY_TO_ENCODE;

	return TRUE;
}

} // end of namespace Engine
