/*================================================================================================*/
/**
   @file   Session.h

   @brief CSession implementation (declaration)
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
Alexander V. Morozov		 13.11.2007					 fixed CSession. deleted CSessionList.
====================================================================================================
Portability:  GCC  compiler
==================================================================================================*/

#ifndef __SESSION_H__
#define __SESSION_H__

#define WIN32_LEAN_AND_MEAN
//#include "SessionParams.h" 
#include "common/types.h"
#include "common/MSG.h"
#include "common/sessionid.h"
#include "common/setsessionid.h"
#include "common/SETCapabilities.h"
#include "common/setauthkey.h"
#include "common/QoP.h"
#include "common/Address.h"
#include "common/commonerrors.h"
#include "common/PosMethod.h"
#include "common/StatusCode.h"
#include "common/RequestedAssistData.h"
#include "common/Notification.h"
#include "common/Position.h"
#include "common/Velocity.h"
#include "common/PosPayLoad.h"
#include "common/LocationID.h"
#include "gps/Command.h"
#include "android/_linux_specific.h"
//#include "windows.h"

namespace Engine {
/*==================================================================================================
GLOBAL CONSTANTS
===================================================================================================*/


/*==================================================================================================
ENUMS
==================================================================================================*/
typedef enum eSessionStatus 
{
	SESSION_ACTIVE = 0,		// Session wait execution 
	SESSION_INACTIVE = 1,		// session is ready to be killed.
	SESSION_READY_TO_DIE = 2

} SessionStatus;

/*==================================================================================================
CLASS DECLARATIONS
==================================================================================================*/

class CAlgorithmBase;

class CSession
{
private:
	SessionStatus			iSessionStatus;
	CAlgorithmBase*			alg;
#if defined(_LINUX_)
	struct timeval          timestamp;
	int8_t					valid;
#endif
//	int8_t					timestamp; // For timeout reasons
public:
	//CSession(CSessionParams *aSessionID);	
	CSession();
	CSession(int32_t ni);
	~CSession();

	static uint16_t			GenerateSessionId();
	SessionStatus			GetSessionStatus(void);	
	void					SetSessionStatus(SessionStatus);	
	bool_t					ExecuteSession(MSG*);
	uint32_t				GetSessionUID();
	void					SetTimestamp();
	void					ResetTimestamp();
	int32_t					GetTimestamp();
	int32_t					GetTimeDiff();
	// type of address, address view, rand number, ip type (default IP_NOTHING)
	uint8_t					SetSETSessionID(ESETId_choice, COctetString&, 
									uint32_t, IP_TYPE iptype = IP_NOTHING);
	uint8_t					SetSETSessionID(ESETId_choice type, uint8_t* pData, uint32_t size, 
									uint32_t rand, IP_TYPE iptype = IP_NOTHING);
	// SETID, SLP address type.
	uint8_t					SetSLPSessionID(COctet&, CChoice&);
// 	uint8_t					SetAuthKey(CChoice&);
// 	uint8_t					SetMAC(COctetString&);
// 	uint8_t					SetKeyIdentity(COctetString&);
	bool_t					IsObjectReady() { return m_ObjectReady;} 

public:
	uint16_t				m_AppID;		// must be equal to session uid.
	bool_t					m_LastRRLPPacket;
	PosMethod				m_UsedPosTechnology;
	PrefMethod				m_PrefMethod;
	PosProtocol				m_PosProtocol;
	PosTechnology			iPosTechnology;
	LocationID				m_LocationID;
	QoP*					iQoP;
	RequestedAssistData*	m_RAD;
	Notification*			m_Notification;
	Position*				m_SuplPosPosition;   //Position from GPS for sending in SUPLPOSINIT
	CChoice*				m_pVelocity;		 //Velocity from GPS for sending in SUPLPOS
	CBitString*				m_pVer;
	bool_t					m_GpsSessionCreated;
	MSG*					m_PendingMessage;	//message that waits for data from gps
	CSessionID				iSessionID;
	MessageType				CurrentState;
	//may be Short or Long
	//CChoice*				iAuthKey;			
	//128bit
	//COctetString*			iKeyIdentity;		
	//64bit
	//COctetString*			iMAC;	

	// message that wait for GPS.
	
	// data will be passed to/from gps

	/*
	GPS Result 
			SUCCESS - indicates that GPS calculated position 
			FAILURE - there is a problem with calculating position
	*/
	enum GPSResult{
		SUCCESS = 0,
		FAILURE = 1
	};
	
	GPSResult				m_GPSResult;    

private:
	uint8_t m_ObjectReady;
};

} //namespace Engine

#endif //__SESSION_H__
