/**
*  @file  DataReqCmd.h
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
Evgenia I. Kobacheva         10.01.2008                   initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/
#ifndef DATA_REQ_CMD_H
#define DATA_REQ_CMD_H

#include "GPSCommand.h"
#include "common/PosPayLoad.h"

namespace Platform {

	class CDataReqCmd:
		public CGPSCommand
	{
	public:
		CDataReqCmd(Engine::uint16_t appId, Engine::CSession* ses, Engine::MSG* msg,void* data);
		virtual ~CDataReqCmd();
	public:		
		// execute command
		virtual Engine::bool_t Execute();
		virtual Engine::uint64_t GetCmdCode();
	};

} // end of namespace 

#endif // _DATA_REQ_CMD_H_
