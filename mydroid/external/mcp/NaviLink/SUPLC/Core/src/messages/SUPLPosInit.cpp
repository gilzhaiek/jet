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
#include "messages/SUPLPosInit.h"

namespace Engine {

CSUPLPosInit::CSUPLPosInit():
	m_pRequestedAssistData(NULL),
	m_pPosition(NULL),
	m_pSUPLPos(NULL),
	m_pVer(NULL)
{
	
}

CSUPLPosInit::~CSUPLPosInit()
{
	if (m_pSUPLPos != NULL)
	{
		delete m_pSUPLPos;
	}
	if (m_pVer != NULL)
	{
		delete m_pVer;
	}
	if (m_pRequestedAssistData != NULL)
	{
		delete m_pRequestedAssistData;
	}
	if (m_pPosition != NULL)
	{
		delete m_pPosition;
	}
}

MessageType CSUPLPosInit::GetType()
{
	return SUPL_POS_INIT;
}

} // end of namespace Engine
