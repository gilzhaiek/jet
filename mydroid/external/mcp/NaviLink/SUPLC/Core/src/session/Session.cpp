/*================================================================================================*/
/**
   @file   Session.cpp

   @brief CSession implementation 
*/
/*==================================================================================================

Copyright (c) 2007 Teleca Inc. All rights reserved.
Redistribution and modifications are permitted subject to BSD license. 
     
====================================================================================================
Revision History:
                            Modification     Tracking
Author                          Date          Number    Description of Changes
-------------------------   ------------    ----------  -------------------------------------------
Roman Suvorov                25.10.2007	                 initial version
====================================================================================================
Portability:  GCC  compiler
==================================================================================================*/

#include "android/ags_androidlog.h"
#include "session/Session.h" 
#include "algorithm/StartAlgorithm.h"
#include <stdio.h>
#include "utils/Log.h"

#define SUPL_TIMER_TVSEC		5
#define SUPL_TIMER_TVNSEC		0
#define SUPL_TIMER_INT_TVSEC 	5
#define SUPL_TIMER_INT_TVNSEC 	0
#define SIG SIGRTMIN
extern void supl_timer_handler(void);    
extern int supl_timer_stop();  
extern int supl_timer_start();   
extern timer_t timerid;
extern sigset_t mask;	  
extern struct sigaction sa;	
extern struct sigevent sev;	
extern struct itimerspec its;
using namespace std;

namespace Engine {

/*
CSession::CSession(CSessionParams *aSessionID)
{
	//iSessionID.SetId(aSessionID->GetId()); 
	//iSessionStatus = IDLE;
}
*/

CSession::CSession():
	iSessionStatus(SESSION_ACTIVE),
#if defined(_LINUX_)
	valid(-1),
#endif
	m_AppID(0),
	m_LastRRLPPacket(FALSE),
	m_UsedPosTechnology(POS_METHOD_NO_POSITION),
	m_PrefMethod(NO_PREF),
	iQoP(NULL),
	m_RAD(NULL),
	m_Notification(0),
	m_SuplPosPosition(NULL),
	m_pVelocity(NULL),
	m_pVer(0),
	m_GpsSessionCreated(FALSE),
	m_PendingMessage(NULL),
	CurrentState((MessageType)-1),
	m_GPSResult(SUCCESS),
	m_ObjectReady(TRUE)
{
	alg = new(std::nothrow) CStartAlgorithm();
	memset(&timestamp, 0, sizeof(struct timeval));
	if(!alg)
		m_ObjectReady = FALSE;
}

/* Added for NI */
CSession::CSession(int32_t ni):
	iSessionStatus(SESSION_ACTIVE),
#if defined(_LINUX_)
	valid(-1),
#endif
	m_AppID(0),
	m_LastRRLPPacket(FALSE),
	m_UsedPosTechnology(POS_METHOD_NO_POSITION),
	m_PrefMethod(NO_PREF),
	iQoP(NULL),
	m_RAD(NULL),
	m_Notification(0),
	m_SuplPosPosition(NULL),
	m_pVelocity(NULL),
	m_pVer(0),
	m_GpsSessionCreated(FALSE),
	m_PendingMessage(NULL),
	CurrentState((MessageType)-1),
	m_GPSResult(SUCCESS),
	m_ObjectReady(TRUE)
{
	alg = new(std::nothrow) CSUPLInitAlgorithm();
	memset(&timestamp, 0, sizeof(struct timeval));
	if(!alg)
		m_ObjectReady = FALSE;
}
CSession::~CSession()
{
	if(alg)
	{
		delete alg;
	}
	if (m_RAD)
	{
		delete m_RAD;
	}
// 	if (iAuthKey)
// 	{
// 		delete iAuthKey;
// 	}
// 	if (iKeyIdentity)
// 	{
// 		delete iKeyIdentity;
// 	}
// 	if (iMAC)
// 	{
// 		delete iMAC;
// 	}
	if (iQoP)
	{
		delete iQoP;
	}
	if (m_SuplPosPosition)
	{
		delete m_SuplPosPosition;
	}	
	m_PendingMessage = NULL;

	if(m_pVelocity)
	{
		delete m_pVelocity;
	}
	if(m_Notification)
	{
		delete m_Notification;
	}
	if(m_pVer)
	{
		delete m_pVer;
	}
}

uint16_t CSession::GenerateSessionId()
{
	static uint16_t id = 0x3e9;  //start generate from 1000
#ifndef NI_CONST_APP_ID 	
	id++;
#endif
	if(id > 0xea60) //60000
		id = 0x3e8;

	return id;
}

void CSession::SetTimestamp()
{
	
//	LOGD("entered SetTimestamp, calling supl_timer_start()");	
	supl_timer_start();	
	gettimeofday(&timestamp, NULL);
	LOGD("[TIME]:SetTimestamp: %u:%u", timestamp.tv_sec, timestamp.tv_usec);
	valid = 1;
	return;
}

int32_t CSession::GetTimeDiff()
{

	timeval systime;
	gettimeofday(&systime, NULL);
	LOGD("[TIME]:GetTimeDiff: %u:%u", systime.tv_sec, systime.tv_usec);
	int32_t sec = ((systime.tv_sec - timestamp.tv_sec) * 1000000 + (systime.tv_usec - timestamp.tv_usec)) / 1000000; //timestamp - systime.wSecond;
	
	return abs(sec);
	
}

void CSession::ResetTimestamp()
{
	int ret = supl_timer_stop();
	if(ret == -1)
		LOGD("supl_timer_handler, Stop timer failed");	
	valid = -1;
}

int32_t CSession::GetTimestamp()
{
	return valid;
}

uint8_t CSession::SetSETSessionID(ESETId_choice type, uint8_t* pData, uint32_t size, 
							   uint32_t rand, IP_TYPE iptype)
{
	if (iSessionID.iSETSessionID != NULL)
	{
		delete 	iSessionID.iSETSessionID;
	}

	//iSessionID.iSETSessionID = new(std::nothrow) CSETSessionID();
	SAFE_NEW_OBJ(iSessionID.iSETSessionID,CSETSessionID);

	switch (type)
	{
		case IMSI:
			{
				SAFE_NEW_OBJ(iSessionID.iSETSessionID->iSetID,IMSIChoice(pData, size));
				break;
			}
		case MSISDN:
			{
				LOGD("\n----Inside case MSISDN of SetSETSessionID----\n");
				SAFE_NEW_OBJ(iSessionID.iSETSessionID->iSetID,MSDNChoice(pData, size));
			}

		default:
			return RETURN_FATAL;
	}
	iSessionID.iSETSessionID->sessionId = rand;

	return RETURN_OK;
}

/**
 *	@breif SetSETSessionID configure sessionID from octet string od bit string.
 *	@param type - Type of SET address.
 *         str  - String that initialize SET address
 *		   rand - random generated number unic. for session
 *         iptype - if SET address is IPADDRESS then you must specify IP dress type (IPv4 or IPv6)
 */

uint8_t CSession::SetSETSessionID(ESETId_choice type, COctetString& str, uint32_t rand, 
							   IP_TYPE iptype)
{
	if (iSessionID.iSETSessionID != NULL)
	{
		delete 	iSessionID.iSETSessionID;
	}

	//iSessionID.iSETSessionID = new(std::nothrow) CSETSessionID();
	SAFE_NEW_OBJ(iSessionID.iSETSessionID,CSETSessionID);
	
	switch (type)
	{
		case MSISDN:
			//iSessionID.iSETSessionID->iSetID = new(std::nothrow) MSDNChoice(&str);
			
			SAFE_NEW_OBJ(iSessionID.iSETSessionID->iSetID,MSDNChoice(&str));
			break;

		case MDN:
			//iSessionID.iSETSessionID->iSetID = new(std::nothrow) MDNChoice(&str);
			SAFE_NEW_OBJ(iSessionID.iSETSessionID->iSetID,MDNChoice(&str));
			break;

		case MIN:
			//wrong parameters
			//iSessionID.iSETSessionID->iSetID = new(std::nothrow) MINChoice((CBitString*) &str);
			SAFE_NEW_OBJ(iSessionID.iSETSessionID->iSetID,MINChoice((CBitString*)&str));
			break;

		case IMSI:
			//iSessionID.iSETSessionID->iSetID = new(std::nothrow) IMSIChoice(&str);
			SAFE_NEW_OBJ(iSessionID.iSETSessionID->iSetID,IMSIChoice(&str));
			break;

		case NAI:
			//iSessionID.iSETSessionID->iSetID = new(std::nothrow) NAIChoice(&str);
			SAFE_NEW_OBJ(iSessionID.iSETSessionID->iSetID,NAIChoice(&str));
			break;

		case IPADDRESS:
			switch(iptype)
			{
				case IPv4:
					//iSessionID.iSETSessionID->iSetID = new(std::nothrow) CIPv4(&str);
					SAFE_NEW_OBJ(iSessionID.iSETSessionID->iSetID,CIPv4(&str));
					break;
				case IPv6:
					//iSessionID.iSETSessionID->iSetID = new(std::nothrow) CIPv6(&str);
					SAFE_NEW_OBJ(iSessionID.iSETSessionID->iSetID,CIPv6(&str));
					break;
				case IP_NOTHING:
					break;
			}
			break;

		default:
			return RETURN_FATAL;
	}
	iSessionID.iSETSessionID->sessionId = rand;
	m_AppID	= rand;

	return RETURN_OK;
}

uint8_t CSession::SetSLPSessionID(COctet& uid, CChoice& addr)
{
	if (iSessionID.iSLPSessionID != NULL)
	{
		delete iSessionID.iSLPSessionID;
	}
	
	//iSessionID.iSLPSessionID = new CSLPSessionID();
	SAFE_NEW_OBJ(iSessionID.iSLPSessionID,CSLPSessionID);

	if(iSessionID.iSLPSessionID->iSessionId.Copy(uid) != RETURN_OK)
		return RETURN_FATAL;
	
	switch (addr.GetType())
	{
		case IP_ADDRESS:
			{
				CIPAddress* ad = (CIPAddress*) (&addr);
				CIPAddress* ad2 = 0;
			
				//ad2 = new CIPAddress();
				SAFE_NEW_OBJ(ad2,CIPAddress);

				switch (ad->iIP_Addr->GetType())
				{				
					case IPv6:
					{
						//ad2->iIP_Addr = new(nothrow) CIPv6;
						SAFE_NEW_OBJ(ad2->iIP_Addr,CIPv6);
						if(((CIPv6*)ad2->iIP_Addr)->Copy(*((CIPv6*) ad->iIP_Addr)) != RETURN_OK)
							return RETURN_FATAL;
						break;
					}
					case IPv4:
					{
						//ad2->iIP_Addr = new(nothrow) CIPv4;
						SAFE_NEW_OBJ(ad2->iIP_Addr,CIPv4);
						if(((CIPv4*)ad2->iIP_Addr)->Copy(*((CIPv4*) ad->iIP_Addr)) != RETURN_OK)
							return RETURN_FATAL;
						break;
					}
				}
				iSessionID.iSLPSessionID->iSLPaddr = ad2;
				break;
			}

		case FQDN:
		{
			//iSessionID.iSLPSessionID->iSLPaddr = new CFQDN();
			SAFE_NEW_OBJ(iSessionID.iSLPSessionID->iSLPaddr,CFQDN);
			if(((CFQDN*)iSessionID.iSLPSessionID->iSLPaddr)->Copy(*((CFQDN*)&addr)) != RETURN_OK)
				return RETURN_FATAL;
			break;
		}
	}	

	return RETURN_OK;
}

// uint8_t CSession::SetAuthKey(CChoice& key)
// {
// 	if (iAuthKey)
// 	{
// 		delete iAuthKey;
// 	}
// 	switch (key.GetType())
// 	{
// 		case SHORTKEY:
// 			//iAuthKey = new CSHORTAuthKey();
// 			SAFE_NEW_OBJ(iAuthKey,CSHORTAuthKey);
// 			if(!((CSHORTAuthKey*) iAuthKey)->iKey.Copy((*((CSHORTAuthKey*) &key)).iKey) != RETURN_OK)
// 				return RETURN_FATAL;
// 		case LONGKEY:
// 			//iAuthKey = new CLONGAuthKey();
// 			SAFE_NEW_OBJ(iAuthKey,CLONGAuthKey);
// 			if(!((CLONGAuthKey*) iAuthKey)->iKey.Copy((*((CLONGAuthKey*) &key)).iKey) != RETURN_OK)
// 				return RETURN_FATAL;
// 	}
// 
// 	return RETURN_OK;
// }

// uint8_t CSession::SetMAC(COctetString& str)
// {
// 	if (iMAC)
// 	{
// 		delete iMAC;
// 	}
// 	//iMAC = new COctetString;
// 	SAFE_NEW_OBJ(iMAC,COctetString);
// 	if(!iMAC->Copy(str) != RETURN_OK)
// 		return RETURN_FATAL;
// 
// 	return RETURN_OK;
// }

// uint8_t CSession::SetKeyIdentity(COctetString& str)
// {
// 	if (iKeyIdentity)
// 	{
// 		delete iKeyIdentity;
// 	}
// 	
// 	//iKeyIdentity = new COctetString;
// 	SAFE_NEW_OBJ(iKeyIdentity,COctetString);
// 	if(!iKeyIdentity->Copy(str) != RETURN_OK)
// 		return  RETURN_FATAL;
// 
// 	return RETURN_OK;
// }

SessionStatus CSession::GetSessionStatus(void)
{
	return iSessionStatus;
}

uint32_t CSession::GetSessionUID(void)
{
	return iSessionID.GetSessionUID();
}

bool_t CSession::ExecuteSession(MSG* message)
{
    debugMessage("%s BEGIN", __FUNCTION__);
	return alg->Response(message, this);	
}

void CSession::SetSessionStatus(SessionStatus s)
{
	iSessionStatus = s;
}


}
