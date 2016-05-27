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
Alexander V. Morozov         12.11.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#include "messages/SUPLInit.h"

namespace Engine {

CSUPLInit::CSUPLInit():
	m_pNotification(NULL),
	m_pSLPAddress(NULL),
	m_pQoP(NULL),
	m_pMAC(NULL),
	m_pKeyIdentity(NULL)
{

}

CSUPLInit::~CSUPLInit()
{
	if (m_pNotification != NULL)
	{
		delete m_pNotification;
	}
	if (m_pSLPAddress != NULL)
	{
		delete m_pSLPAddress;
	}
	if (m_pQoP != NULL)
	{
		delete m_pQoP;
	}
	if (m_pMAC != NULL)
	{
		delete m_pMAC;
	}
	if (m_pKeyIdentity != NULL)
	{
		delete m_pKeyIdentity;
	}
}	
	
MessageType CSUPLInit::GetType()
{
	return SUPL_INIT;
}


}
