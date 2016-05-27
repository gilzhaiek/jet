/**
*  @file  Command.h
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
Alexander V. Morozov         03.12.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#ifndef _GPS_COMMAND_H_
#define _GPS_COMMAND_H_

#include "common/types.h"
#include "GPS.h"
#include "session/Session.h"
#include "suplcontroller/SUPLController.h"
#include "common/SETCapabilities.h"
#include "common/RequestedAssistData.h"
#include "gps/DataUPL.h"

//#define	GPS_S_OK				0x1
//#define GPS_E_FAIL				0x2
//#define GPS_E_CMD_NOT_SUPPORTED 0x3
//#define GPS_E_GPS_INVALID_PARAM	0x4

// direction GPSM->SUPL
#define UPL_START_REQ		0x00030000
#define UPL_DATA_REQ		0x00030001
#define UPL_STOP_REQ		0x00030002
#define	UPL_NOTIFY_RESP		0x00030003
// direction SUPL->GPSM
#define UPL_DATA_IND		0x00038000
#define UPL_POS_IND			0x00038001
#define UPL_STOP_IND		0x00038002
#define UPL_START_LOC_IND	0x00038003
#define UPL_NOTIFY_IND		0x00038004

namespace Engine {

class CSession;
class CSUPLGlobal;

}

namespace Platform {


class IGPS;

/*
	This class contains data processing logic.
*/
class CGPSCommand
{
protected:		
	Engine::uint16_t	 appId;
	void*				 data;
	Engine::CSession*	 session;
	Engine::MSG*		 message;
	Engine::CSUPLGlobal* supl_global;

public:
	// construct command with specific app id and gps interface
	CGPSCommand(Engine::uint16_t appId, Engine::CSession* ses, Engine::MSG* msg, void* data);	
	virtual ~CGPSCommand();
public:
	void		SetSession(Engine::CSession* ses);
	void		SetMessage(Engine::MSG* msg);
	// execute command
	virtual Engine::bool_t   Execute() = 0;
	Engine::uint16_t		 GetAppID();	
	virtual Engine::uint64_t GetCmdCode() = 0;
	// method to create command (fabric method)
	static	CGPSCommand*     CreateCommand(Engine::uint64_t cmd, Engine::uint16_t app_id, Engine::CSession* ses, 
										   Engine::MSG* msg, void* data);
};

} // end of namespace Platform

#endif
