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

#include "gps/GPSCommand.h"
#include "gps/StartReqCmd.h"
#include "gps/DataIndCmd.h"
#include "gps/DataReqCmd.h"
#include "gps/StopReqCmd.h"
#include "gps/PosIndCmd.h"
#include "gps/StopIndCmd.h"
#include "gps/StartLocIndCmd.h"
#include "gps/NotifyIndCmd.h"
#include "gps/NotifyRespCmd.h"

using namespace Engine;

namespace Platform {

CGPSCommand::CGPSCommand(Engine::uint16_t appId, Engine::CSession* ses, Engine::MSG* msg, void* data):
	appId(appId), data(data), session(ses), message(msg)
{

}
CGPSCommand::~CGPSCommand()
{
	/*if (data)
	{
		delete data;
	}
	data = NULL;*/
}
Engine::uint16_t CGPSCommand::GetAppID()
{
	return appId;
}

void CGPSCommand::SetSession(Engine::CSession* ses)
{
	this->session = ses;
}
void CGPSCommand::SetMessage(Engine::MSG* msg)
{
	this->message = msg;
}

CGPSCommand* CGPSCommand::CreateCommand(Engine::uint64_t cmd, 
										Engine::uint16_t appId, Engine::CSession* ses, Engine::MSG* msg,
										void* data)
{
	LOGD("+CreateCommand");
	switch (cmd)
	{
	case UPL_START_REQ:
		{
			LOGD("UPL_START_REQ");
			StartReqData *dta = (StartReqData*)data;
			LOGD("\nIn CreateCommand()\n");
			LOGD("\nThe Address of pointer = %p, %p\n",data,dta);
			LOGD("\nThe Address of mcc in CreateCommand() = %p\n",&(dta->lac.cell_info.cell_info_wcdma.wcdma_mcc));
			LOGD("\nSize of StartReqData in CreateCommand = %d\n",sizeof(*dta));
			LOGD("\nmcc = %ld\n",dta->lac.cell_info.cell_info_wcdma.wcdma_mcc);
			LOGD("\nmnc = %ld\n",dta->lac.cell_info.cell_info_wcdma.wcdma_mnc);
			LOGD("\nci = %ld\n",dta->lac.cell_info.cell_info_wcdma.wcdma_ci);
			return new(std::nothrow) CStartReqCmd(appId, ses, msg, data);
		}
	case UPL_DATA_REQ:
		{
			LOGD("UPL_DATA_REQ");
			return new(std::nothrow) CDataReqCmd(appId, ses, msg, data);
		}
	case UPL_STOP_REQ:
		{
			LOGD("UPL_STOP_REQ");
			return new(std::nothrow) CStopReqCmd(appId, ses, msg, data);
		}
	case UPL_DATA_IND:
		{
			LOGD("UPL_DATA_IND");
			return new(std::nothrow) CDataIndCmd(appId, ses, msg, data);
		}
	case UPL_POS_IND:
		{
			LOGD("UPL_POS_IND");
			return new(std::nothrow) CPosIndCmd(appId, ses, msg, data);
		}
	case UPL_STOP_IND:
		{
			LOGD("UPL_STOP_IND");
			return new(std::nothrow) CStopIndCmd(appId, ses, msg, data);
		}
	case UPL_START_LOC_IND:
		{
			LOGD("UPL_START_LOC_IND");
			return new(std::nothrow) CStartLocIndCmd(appId, ses, msg, data);
		}
	case UPL_NOTIFY_IND:
		{
			LOGD("UPL_NOTIFY_IND");
			return new(std::nothrow) CNotifyIndCmd(appId, ses, msg, data);
		}
	case UPL_NOTIFY_RESP:
		{
			LOGD("UPL_NOTIFY_RESP");
			return new(std::nothrow) CNotifyRespCmd(appId, ses, msg, data);
		}
	default:
		return 0;
	}
	return 0;
}

} // end of namespace Platform
