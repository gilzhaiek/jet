/**
*  @file  SUPLResponseAlgorithm.cpp
*  @brief CSUPLResponseAlgorithm  declaration.
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

#include "algorithm/SUPLResponseAlgorithm.h"

#include "nav_log_msg.h"
#include "nav_log_cust_codes.h"
namespace Engine {

CSUPLResponseAlgorithm::CSUPLResponseAlgorithm()
{

}

CSUPLResponseAlgorithm::~CSUPLResponseAlgorithm()
{

}

bool_t CSUPLResponseAlgorithm::Response(MSG* msg, CSession* session)
{
  SESSLOGMSG("[%s]%s:%d, %s\n",get_utc_time(),SESS_OTA_MSG_TYPE,nav_sess_getCount(0),"SUPL RESPONSE");
	if (msg->m_pIncomingMessage->GetType() != SUPL_RESPONSE)
	{
		if(!CreateNewResponse(this, msg)->Response(msg, session))
			return FALSE;

		return TRUE;
	}

	CSUPLResponse* resp		= (CSUPLResponse*) (msg->m_pIncomingMessage);

	CommonError ret = CheckPacket(msg,session);
	if(ret != RETURN_OK)
	{
		if(ret == RETURN_ERROR)
		{
			if(!CreateNewResponse(this, msg)->Response(msg, session))
				return FALSE;

			return TRUE;
		}
		else
		{
			return FALSE;
		}
	}
	/* 
	Analyze the incoming message
	*/
	// store SLP session ID
	session->iSessionID.iSLPSessionID = new(std::nothrow) CSLPSessionID();
	session->iSessionID.iSLPSessionID->Copy(*resp->m_SessionID.iSLPSessionID);
	
	// store positioning method
	// switch GPS to desired mode here!
	session->m_UsedPosTechnology = resp->m_PosMethod;			

	if(!CreateNewResponse(this, msg)->Response(msg, session))
		return FALSE;

	return TRUE;
}

CommonError CSUPLResponseAlgorithm::CheckPacket(MSG* msg, CSession* session)
{
	CSUPLResponse* resp		= (CSUPLResponse*) (msg->m_pIncomingMessage);
	CSUPLEnd*	   supl_end = NULL;
	StatusCode	   status_code = (StatusCode)0;

	// mandatory fields check place here.

	if (resp->m_PosMethod == POS_METHOD_NO_POSITION)
	{
		status_code = Engine::POS_METHOD_FAILURE;
	}
	if(!(session->iSessionID.IsEqual(resp->m_SessionID)))
	{
		status_code = Engine::INVALID_SESSION_ID;
	}
	
	if(status_code != 0)
	{
		// create SUPL_END
		supl_end = new(std::nothrow) CSUPLEnd();
		if(!supl_end)
			return RETURN_FATAL;
		msg->m_pOutgoingMessage = supl_end;
		// set session id
		if(supl_end->m_SessionID.Copy(resp->m_SessionID) != RETURN_OK)
			return RETURN_FATAL;
		// set protocol version.
		supl_end->m_Version.m_Major = MAJ_VERSION;
		supl_end->m_Version.m_Minor = MIN_VERSION;
		supl_end->m_Version.m_Servind = SER_VERSION;
		supl_end->m_pStatusCode = new(std::nothrow) StatusCode;
		if(!supl_end->m_pStatusCode)
			return RETURN_FATAL;
		*supl_end->m_pStatusCode = status_code;		
		return RETURN_ERROR;
	}

	return RETURN_OK;
}

CAlgorithmBase* CSUPLResponseAlgorithm::CreateNewResponse(CAlgorithmBase* old, MSG* msg)
{
	// unconditional jump to CSUPLPosInitAlgorithm if we create SUPL_POS_INIT response
	// if we didn't reject this message (didn't create SUPLEnd) create SUPLPosInit
	if (msg->m_pOutgoingMessage == NULL)
	{
		return new (old) CSUPLPosInitAlgorithm();
	}
	
	// else we have SUPL_END.
	return new (old) CSUPLEndAlgorithm();
}

} // end of namespace Engine
