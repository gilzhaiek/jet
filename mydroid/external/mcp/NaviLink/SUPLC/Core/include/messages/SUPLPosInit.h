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
Alexander V. Morozov         01.11.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#ifndef _SUPL_POS_INIT_H_
#define _SUPL_POS_INIT_H_

#include "common/types.h"
#include "messages/suplmessage.h"
#include "common/LocationID.h"
#include "messages/SUPLPos.h"
#include "common/Position.h"
#include "common/RequestedAssistData.h"


namespace Engine {

class CSUPLPosInit: public CSUPLMessage
{
public:
	 CSUPLPosInit();
	~CSUPLPosInit();

virtual MessageType GetType();
public:
	
	RequestedAssistData*	m_pRequestedAssistData;
	Position*				m_pPosition;
	CSUPLPos*				m_pSUPLPos;
	CBitString*				m_pVer;
	LocationID				m_LocationID;
	SETCapabilities			m_SETCapabilities;
};

}

#endif //_SUPL_POS_INIT_H_
