/**
 *  @file  SUPLStart.h
 *  @brief CSUPLStart declaration.
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

#ifndef _SUPL_START_H_
#define _SUPL_START_H_

#include "messages/suplmessage.h"
#include "common/SETCapabilities.h"
#include "common/LocationID.h"
#include "common/QoP.h"

namespace Engine {

class CSUPLStart: public CSUPLMessage
{
public:
	SETCapabilities	m_SETCaps;
	LocationID		m_LocationID;
	QoP*			m_pQoP;

public:
	 CSUPLStart();
    ~CSUPLStart();

	virtual MessageType GetType();
};

} // end of namespace Engine

#endif //_SUPL_START_H_
