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

#ifndef _SUPL_END_H_
#define _SUPL_END_H_

#include "messages/suplmessage.h"
#include "common/StatusCode.h"
#include "common/Position.h"


namespace Engine {

class CSUPLEnd: public CSUPLMessage
{
public:
	CSUPLEnd();
	~CSUPLEnd();
virtual MessageType GetType();

public:
	Position*	m_pPosition;	
	StatusCode*	m_pStatusCode;
	// a 64 bits bit string
	CBitString*	m_pVer;
};

} // end of namespace Engine

#endif // _SUPL_END_H_
