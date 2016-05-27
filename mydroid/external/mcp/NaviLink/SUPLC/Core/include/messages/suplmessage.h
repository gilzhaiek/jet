/**
 *  @file  SUPLMessage.h
 *  @brief CSUPLMessage declaration.
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

#ifndef _SUPL_MESSAGE_H_
#define _SUPL_MESSAGE_H_

#include "common/SWCaps.h"
#include "common/sessionid.h"

namespace Engine {

enum MessageType
{
	SUPL_INIT,
	SUPL_RESPONSE,
	SUPL_POS_INIT,
	SUPL_START,
	SUPL_POS_RESPONSE,
	SUPL_POS,
	SUPL_END
};

/**
 *	@breif This is abstract class. It is based for all messages n system
 *
 */
class CSUPLMessage 
{
public:
	// Common part of message
	uint32_t		m_Lenght;
	Version		m_Version;
	CSessionID  m_SessionID;	
	
	CSUPLMessage();	
	virtual ~CSUPLMessage();
	
	virtual MessageType GetType() = 0;
};

} // end of namespace Engine

#endif // _SUPL_MESSAGE_H_
