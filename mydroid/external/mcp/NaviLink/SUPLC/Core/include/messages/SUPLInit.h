/**
*  @file  SUPLInit.h
*  @brief CSUPLInit declaration.
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
Alexander V. Morozov         01.11.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#ifndef _SUPL_INIT_H_
#define _SUPL_INIT_H_

#include "messages/suplmessage.h"
#include "common/PosMethod.h"
#include "common/Notification.h"
#include "common/QoP.h"
#include "common/SLPMode.h"

namespace Engine {

class CSUPLInit: public CSUPLMessage
{
public:
	PosMethod		m_PosMethod;
	Notification*	m_pNotification;
	// choose from fqdn, ipv4, ipv6
	CChoice*		m_pSLPAddress;
	QoP*			m_pQoP;
	SLPMode			m_SLPMode;
	// 64 bit string.
	CBitString*		m_pMAC;
	// 128 bit string.
	CBitString*		m_pKeyIdentity;

public:
		CSUPLInit();
virtual	~CSUPLInit();
virtual MessageType GetType();

};

}

#endif // _SUPL_INIT_H_
