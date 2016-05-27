/**
*  @file  StartReqCmd.h
*  @brief StartReqCmd declaration.
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
Alexander V. Morozov         09.01.2008	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#ifndef START_REQ_CMD_H
#define START_REQ_CMD_H

#include "GPSCommand.h"

namespace Platform {

class CStartReqCmd:
	public CGPSCommand
{
public:
	CStartReqCmd(Engine::uint16_t appId, Engine::CSession* ses, Engine::MSG* msg,void* data);
	~CStartReqCmd();
public:
	// execute command
	virtual Engine::bool_t Execute();
	virtual Engine::uint64_t GetCmdCode();
};

} // end of namespace 

#endif // START_REQ_CMD_H
