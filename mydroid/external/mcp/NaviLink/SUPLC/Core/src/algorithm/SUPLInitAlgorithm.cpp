/**
*  @file  SUPLEndAlgorithm .h
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

//#include <Windows.h>
#include <stdlib.h>
#include "algorithm/SUPLInitAlgorithm.h"
#include "android/ags_androidlog.h"

#include "nav_log_msg.h"
#include "nav_log_cust_codes.h"
using namespace Platform;

namespace Engine {

CSUPLInitAlgorithm::CSUPLInitAlgorithm()
{

}

CSUPLInitAlgorithm::~CSUPLInitAlgorithm()
{

}

/**
 * @breif The purpose of this algorithm is to store all incoming data form message 
 *		  and fill appropriate fields. After this it switch to response generator.
		  also, we should check fields.
 *
 */
bool_t CSUPLInitAlgorithm::Response(MSG* msg, CSession* session)
{

	/*
		Store data from SUPLInit message and fill SET session ID
	*/	

	
#if defined(_DEBUG_IMSI)
	uint8_t  buffer[8];
	uint32_t size=8;
#elif defined(_ANDROID_IMSI_)
	CMSISDN   msisdn;
	CIMSI     imsi;
	uint32_t size = 0;
	uint8_t tmp[32];
	uint8_t *buffer = tmp;
#endif
	//CIMSI imsi;
	CSUPLInit* init = (CSUPLInit*) msg->m_pIncomingMessage;	

	// store SLP session ID
	if(session->iSessionID.Copy(init->m_SessionID) != RETURN_OK)
		return FALSE;
	/*
	generate SET session ID
	*/

#if defined(_DEBUG_IMSI)
	CSUPLController::GetInstance().GetDevice().GetIMSIFromSIM(buffer,&size);	
#elif defined(_ANDROID_IMSI_)
	if((getSubscriberIdType()) == 1)
	{
	   LOGD("\n----Calling GetMSISDNFromSIM in SuplInitAlgorithm.cpp----\n");
	   CSUPLController::GetInstance().GetDevice().GetMSISDNFromSIM(&msisdn);
	   msisdn.getCodedMSISDN(&buffer, &size);
	}
	else
	{
	   CSUPLController::GetInstance().GetDevice().GetIMSIFromSIM(&imsi);
	   imsi.getCodedIMSI(&buffer, &size);
	}
#endif
	//imsi.getCodedIMSI(&buffer, &size);
	COctetString CoC(buffer, size);
	if((getSubscriberIdType()) == 1)
	{
	   if(session->SetSETSessionID(MSISDN, CoC, msg->m_SETSessionID, IP_NOTHING) != RETURN_OK)
		return FALSE;
	}
	else
	{
	   if(session->SetSETSessionID(IMSI, CoC, msg->m_SETSessionID, IP_NOTHING) != RETURN_OK)
		return FALSE;
	}

	CommonError ret_code = CheckPacket(msg,session);
	if(ret_code != RETURN_OK)
	{
		if(ret_code == RETURN_ERROR)
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

	session->m_UsedPosTechnology = init->m_PosMethod;
/*
	if((session->m_UsedPosTechnology == POS_METHOD_AGPS_SET_ASSISTED)||(session->m_UsedPosTechnology == POS_METHOD_AGPS_SET_ASSISTED_PREF))
	{
		SESSLOGMSG("[%s]%s:%d, %s %s\n",get_utc_time(),SESS_OTA_MSG_TYPE,nav_sess_getCount(0),"SUPL INIT","#NI MSA");
	}
	else if((session->m_UsedPosTechnology == POS_METHOD_AGPS_SET_BASED)||(session->m_UsedPosTechnology == POS_METHOD_AGPS_SET_BASED_PREF))
	{
		SESSLOGMSG("[%s]%s:%d, %s %s\n",get_utc_time(),SESS_OTA_MSG_TYPE,nav_sess_getCount(0),"SUPL INIT","#NI MSB");
	}
*/
	
	if(init->m_pNotification)
	{
		session->m_Notification = init->m_pNotification;
		init->m_pNotification = NULL;
	}

	if(init->m_pQoP)
	{
		session->iQoP = init->m_pQoP;
		init->m_pQoP = 0;
	}
	session->m_PendingMessage = msg;
	
	CreateNewResponse(this, msg);
		
	return TRUE;
}

CommonError CSUPLInitAlgorithm::CheckPacket(MSG* msg, CSession* session)
{
	CSUPLInit* resp		= (CSUPLInit*) (msg->m_pIncomingMessage);
	CSUPLEnd*	   supl_end = NULL;
	StatusCode	   status_code = (StatusCode)0;
	int                pos_method  = resp->m_PosMethod;
	

	// mandatory fields check place here.
    if(pos_method < 0 || pos_method > 9)
	{
		status_code = Engine::UNEXPECTED_DATA_VALUE;
	}
	
	if(resp->m_SLPMode != SLP_PROXY_MODE)
	{
		status_code = Engine::NON_PROXY_MODE_NOT_SUPPORTED;
	}
	
	if(status_code != 0)
	{
		// create SUPL_END
		supl_end = new(std::nothrow) CSUPLEnd();
		if(!supl_end)
			return RETURN_FATAL;
		msg->m_pOutgoingMessage = supl_end;
		// set session id
		if(supl_end->m_SessionID.Copy(session->iSessionID) != RETURN_OK)
			return RETURN_FATAL;
		// set protocol version.
		supl_end->m_Version.m_Major = MAJ_VERSION;
		supl_end->m_Version.m_Minor = MIN_VERSION;
		supl_end->m_Version.m_Servind = SER_VERSION;
		supl_end->m_pStatusCode = new(std::nothrow) StatusCode;
		if(!supl_end->m_pStatusCode)
			return RETURN_FATAL;
		if(session->m_pVer)
		{
			supl_end->m_pVer = new(std::nothrow)  Engine::CBitString(64);
			if(!supl_end->m_pVer)
				return RETURN_FATAL;
			if(!supl_end->m_pVer->IsObjectReady())
				return RETURN_FATAL;
			if(supl_end->m_pVer->Copy(*(session->m_pVer)) != RETURN_OK)
				return RETURN_FATAL;
		}
		*supl_end->m_pStatusCode = status_code;		
		return RETURN_ERROR;
	}
	
	return RETURN_OK;
}

CAlgorithmBase* CSUPLInitAlgorithm::CreateNewResponse(CAlgorithmBase* old, MSG* msg)
{
	if (msg->m_pIncomingMessage->GetType() == SUPL_INIT && !msg->m_pOutgoingMessage)
	{
		return new (old) CSUPLPosInitAlgorithm();
	}
	
	return new (old) CSUPLEndAlgorithm();
}

}
