/**
*  @file  StopReqCmd.h
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
#ifndef STOP_REQ_CMD_H
#define STOP_REQ_CMD_H

#include "GPSCommand.h"
#include "suplcontroller/SUPLController.h"
#include "algorithm/SUPLEndAlgorithm.h"

namespace Platform {

	class CStopReqCmd:
		public CGPSCommand
	{
	public:
		CStopReqCmd(Engine::uint16_t appId, Engine::CSession* ses, Engine::MSG* msg,void* data);
		virtual ~CStopReqCmd();
	public:
		// execute command
		virtual Engine::bool_t Execute();
		virtual Engine::uint64_t GetCmdCode();
	};

} // end of namespace 

#endif // _STOP_REQ_CMD_H_
