/**
*  @file  SUPLPos.h
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

#ifndef _SUPL_POS_H_
#define _SUPL_POS_H_

#include "messages/suplmessage.h"
#include "common/Velocity.h"
#include "common/PosPayLoad.h"

namespace Engine {

class CSUPLPos: public CSUPLMessage
{
public:
	
public:
	 CSUPLPos();
	~CSUPLPos();

virtual MessageType GetType();

public:
	// choice form CTIA801PayLoad, CRRCPayLoad, CRRLPPayLoad
	CChoice*	m_PosPayLoad;
	// choice from CHorvel, CHorandvervel, CHorveluncert, CHorandveruncert
	CChoice*	m_pVelocity;
};

} // end of namespace Engine

#endif // define _SUPL_POS_H_
