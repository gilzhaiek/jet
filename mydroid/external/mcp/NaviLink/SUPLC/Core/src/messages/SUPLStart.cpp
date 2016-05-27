/**
 *  @file  SUPLStart.cpp
 *  @brief CSUPLStart definition
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
Alexander V. Morozov         30.10.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/
#include "messages/SUPLStart.h"

namespace Engine {

CSUPLStart::CSUPLStart():
	m_pQoP(NULL)
{

}

CSUPLStart::~CSUPLStart()
{
	if (m_pQoP != NULL)
	{
		delete m_pQoP;
	}
}

MessageType CSUPLStart::GetType()
{
	return SUPL_START;
}

} // end of namespace Engine
