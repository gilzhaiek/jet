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

#include "messages/SUPLResponse.h"

namespace Engine {


CSUPLResponse::CSUPLResponse():
	m_pSLPAddress(0)
{

}

CSUPLResponse::~CSUPLResponse()
{
	if (m_pSLPAddress != NULL)
	{
		delete m_pSLPAddress;
	}
	if (m_pSETAuthKey != NULL)
	{
		delete m_pSETAuthKey;
	}

	if (m_pKeyIdentity4 != NULL)
	{
		delete m_pKeyIdentity4;
	}
}

MessageType CSUPLResponse::GetType()
{
	return SUPL_RESPONSE;
}


} // end of namespace Engine
