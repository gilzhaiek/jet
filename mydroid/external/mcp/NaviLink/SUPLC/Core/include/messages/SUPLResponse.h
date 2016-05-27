/**
*  @file  SUPLResponse.h
*  @brief CSUPLResponse declaration.
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
Alexander V. Morozov         29.10.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#ifndef _SUPL_RESPONSE_H_
#define _SUPL_RESPONSE_H_

#include "messages/suplmessage.h"
#include "common/SETCapabilities.h"
#include "common/slpsessionid.h"
#include "common/PosMethod.h"
#include "common/setauthkey.h"

namespace Engine {

class CSUPLResponse: public CSUPLMessage
{
public:
	// PosMethod
	PosMethod		m_PosMethod;
	// Choice from IPAddress CIPv4, CIPv6, and CFQDN
	CChoice*		m_pSLPAddress;
	// Choice from CLONGAuthKey CSHORTAuthKey
	CChoice*		m_pSETAuthKey;
	// key identity (optional)
	CBitString*		m_pKeyIdentity4;

public:	
	CSUPLResponse();
	virtual ~CSUPLResponse();

	virtual MessageType GetType();
};

} // end of namespace Engine

#endif
