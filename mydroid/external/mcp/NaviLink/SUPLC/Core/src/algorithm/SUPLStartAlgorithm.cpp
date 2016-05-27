/**
*  @file  SUPLStartAlgorithm .h
*  @brief CSUPLStartAlgorithm  declaration.
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
Alexander V. Morozov         19.11.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/
//#include <Windows.h>
#include <new>

#include "algorithm/SUPLStartAlgorithm.h"
#include "android/ags_androidlog.h"
#include "sys/time.h"
#include "nav_log_msg.h"
#include "nav_log_cust_codes.h"
using namespace Platform;

namespace Engine {

CSUPLStartAlgorithm::CSUPLStartAlgorithm()
{

}

CSUPLStartAlgorithm::~CSUPLStartAlgorithm()
{

}

bool_t CSUPLStartAlgorithm::Response(MSG* msg, CSession* session)
{
	// we created SUPL start. Now we can check incoming buffer.
	// if this buffer is NULL this is SUPLStart else create new ALG
	if (msg->m_pIncomingMessage != NULL)
	{
		if(!CreateNewResponse(this, msg)->Response(msg, session))
			return FALSE;

		return TRUE;
	}
	// create SUPL_START
	CSUPLStart*  mess = NULL;
	mess = new(std::nothrow) CSUPLStart();
	if(!mess)
	{
		return FALSE;
	}
	msg->m_pOutgoingMessage = mess;
	//CSUPLGlobal* global = CSUPLGlobal::GetInstance();	

#if defined(_DEBUG_IMSI)
	uint8_t  buffer[8];
	uint32_t size=8;
#elif defined(_ANDROID_IMSI_)
	CMSISDN   msisdn;
	CIMSI     imsi;
	uint32_t  size = 0;
	uint8_t   tmp[16] ;
	uint8_t   *buffer = tmp;
#endif
    
	/*
		Configure csession object
	*/
	// obtain imsi

#if defined(_DEBUG_IMSI)
	CSUPLController::GetInstance().GetDevice().GetIMSIFromSIM(buffer,&size);	
#elif defined(_ANDROID_IMSI_)
	if((getSubscriberIdType()) == 1)
	{
	   LOGD("\n----Calling GetMSISDNFromSIM in SuplStartAlgorithm.cpp---\n");
	   CSUPLController::GetInstance().GetDevice().GetMSISDNFromSIM(&msisdn);
	   msisdn.getCodedMSISDN(&buffer, &size);
	}
	else
	{
	   CSUPLController::GetInstance().GetDevice().GetIMSIFromSIM(&imsi);
	   imsi.getCodedIMSI(&buffer, &size);
	}
#endif
	
	// create session id	warning this place may contains BUG in SetSETSessionID
	COctetString CoC(buffer, size);
	if((getSubscriberIdType()) == 1)
	{
	   LOGD("\n----Calling SetSETSessionID with type MSISDN from SuplStartAlgorithm.cpp----\n");
	   if(session->SetSETSessionID(MSISDN, CoC, msg->m_SETSessionID) != RETURN_OK)
		return FALSE;
	}
	else
	{
	   if(session->SetSETSessionID(IMSI, CoC, msg->m_SETSessionID) != RETURN_OK)
		return FALSE;
	}
	
	if(mess->m_SessionID.Copy(session->iSessionID) != RETURN_OK)
		return FALSE;
	// set location id

	if(mess->m_LocationID.Copy(session->m_LocationID) != RETURN_OK)
		return FALSE;

    // set set capabilities
    mess->m_SETCaps.m_PosProtocol = session->m_PosProtocol;
    mess->m_SETCaps.m_PosTechnology = session->iPosTechnology;
    mess->m_SETCaps.m_PrefMethod = session->m_PrefMethod;

    if(session->iQoP)
    {
        mess->m_pQoP = new(std::nothrow) Engine::QoP;
        if(!mess->m_pQoP)
        {
            return FALSE;
        }
        if(!mess->m_pQoP->IsObjectReady())
        {
            return FALSE;
        }
        if(mess->m_pQoP->Copy(*(session->iQoP)) != RETURN_OK)
        {
            return FALSE;
        }
        
    }


    // set version number
    mess->m_Version.m_Major = MAJ_VERSION;
    mess->m_Version.m_Minor = MIN_VERSION;
    mess->m_Version.m_Servind = SER_VERSION;//CSUPLGlobal::GetInstance()->GetSWCaps().m_Version;//global->GetSWCaps().m_Version;
    mess->m_Lenght = 0;

//	msg->m_pOutgoingMessage = mess;
//	msg->m_SETSessionID = mess->m_SessionID.GetSessionUID();

	return TRUE;
}

CAlgorithmBase* CSUPLStartAlgorithm::CreateNewResponse(CAlgorithmBase* old, MSG* msg)
{
	CAlgorithmBase* NEW;
	if (msg->m_pIncomingMessage->GetType() == SUPL_RESPONSE)
	{
		NEW = new (old) CSUPLResponseAlgorithm();
		return NEW; 
		//return new (old) CSUPLResponseAlgorithm();
	}
	
	NEW = new (old) CSUPLEndAlgorithm(); 
	return NEW;
	//return new (old) CSUPLEndAlgorithm();
}


} // enf of namespace Engine
