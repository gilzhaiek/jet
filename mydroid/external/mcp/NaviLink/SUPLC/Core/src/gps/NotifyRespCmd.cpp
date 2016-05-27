/**
*  @file  NotifyIndCmd.cpp
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
Ildar Abdullin			    13.03.2008                   initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#include "gps/NotifyRespCmd.h"


namespace Platform {



	CNotifyRespCmd::CNotifyRespCmd(Engine::uint16_t appId, Engine::CSession* ses, Engine::MSG* msg, void* data):
	CGPSCommand(appId, ses, msg, data)
	{

	}

	CNotifyRespCmd::~CNotifyRespCmd()
	{

	}

	Engine::bool_t CNotifyRespCmd::Execute()
	{

		NotifyRespData* dta = (NotifyRespData*) this->data;

		if (dta == NULL)
		{
			return FALSE;
		}

		Engine::CSession* session=Engine::CSUPLController::GetInstance().GetSession(dta->app_id);
		if(!session)
		{
			return FALSE;
		}

		if(dta->notification_status == Platform::DENIED)
		{
			if(session->m_UsedPosTechnology == Engine::POS_METHOD_NO_POSITION)
			{
				Engine::CSUPLController::GetInstance().StopSession(dta->app_id,Engine::CONSENT_DENIED_BY_USER);
			}
			else
			{
				Engine::CSUPLController::GetInstance().StopSession(dta->app_id,Engine::POSITIONING_NOT_PERMITTED);
			}
		}
		else
		{
			if(session->m_UsedPosTechnology == Engine::POS_METHOD_NO_POSITION)
			{
				if(session->m_Notification->notificationType == Engine::notificationOnly)
				{
					Engine::CSUPLController::GetInstance().StopSession(dta->app_id,(Engine::StatusCode)-1);
				}
				else
				{
					Engine::CSUPLController::GetInstance().StopSession(dta->app_id,Engine::CONSENT_GRANTED_BY_USER);
				}
			}
			else
			{
				session->m_PendingMessage->m_Status = Engine::MSG_SEND_TO_GPS_NI_REQUEST;
			}
		}

		free(dta);

		return TRUE;
	}

	Engine::uint64_t CNotifyRespCmd::GetCmdCode()
	{
		return UPL_NOTIFY_RESP;
	}

};
