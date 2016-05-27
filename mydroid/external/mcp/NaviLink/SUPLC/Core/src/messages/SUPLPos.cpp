/**
*  @file  SUPLPos.cpp
*  @brief CSUPLPos declaration.
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

#include "messages/SUPLPos.h"

namespace Engine {

CSUPLPos::CSUPLPos():
	m_PosPayLoad(NULL),
	m_pVelocity(NULL)
{

}

CSUPLPos::~CSUPLPos()
{
	if (m_PosPayLoad != NULL)
	{
		delete m_PosPayLoad;
	}
	if (m_pVelocity != NULL)
	{
		delete m_pVelocity;
	}
}


MessageType CSUPLPos::GetType()
{
	return SUPL_POS;
}

} // end of namespace Engine
