/**
*  @file  NotifyIndCmd.h
*  @brief Command declaration.
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
Ildar Abdullin		         13.03.2008                   initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/
#ifndef NOTIFY_IND_H
#define NOTIFY_IND_H

#include "GPSCommand.h"

namespace Platform {

	class CNotifyIndCmd:
		public CGPSCommand
	{
	public:
		CNotifyIndCmd(Engine::uint16_t appId, Engine::CSession* ses, Engine::MSG* msg,void* data);
		virtual ~CNotifyIndCmd();
	public:		
		// execute command
		virtual Engine::bool_t   Execute();
		virtual Engine::uint64_t GetCmdCode();
	};

} // end of namespace 

#endif //NOTIFY_IND_H
