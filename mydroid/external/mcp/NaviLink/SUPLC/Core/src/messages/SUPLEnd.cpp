/**
*  @file  SUPLEnd.h
*  @brief CSUPLEnd declaration.
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

#include "messages/SUPLEnd.h"

namespace Engine {

CSUPLEnd::CSUPLEnd():
	m_pPosition(NULL),
	m_pStatusCode(NULL),
	m_pVer(NULL)
{

}
CSUPLEnd::~CSUPLEnd()
{
	if (m_pPosition != NULL)
	{
		delete m_pPosition;
	}
	if (m_pStatusCode != NULL)
	{
		delete m_pStatusCode;
	}
	if (m_pVer != NULL)
	{
		delete m_pVer;
	}
}

MessageType CSUPLEnd::GetType()
{
	return SUPL_END;
}

}
