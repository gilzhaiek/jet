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

#include "gps/NotifyIndCmd.h"


namespace Platform {



	CNotifyIndCmd::CNotifyIndCmd(Engine::uint16_t appId, Engine::CSession* ses, Engine::MSG* msg,void* data):
	CGPSCommand(appId, ses, msg, data)
	{

	}

	CNotifyIndCmd::~CNotifyIndCmd()
	{

	}

	Engine::bool_t CNotifyIndCmd::Execute()
	{ 
		NotifyIndData* dta = (NotifyIndData*)malloc(sizeof(NotifyIndData));
		
		if (dta == NULL)
		{
			return FALSE;
		}

		//saving Application Id
		dta->app_id = appId;

		dta->notification_type = (Platform::NotificationType)session->m_Notification->notificationType;
		if(session->m_Notification->clientName)
		{
			memcpy(dta->client_name,session->m_Notification->clientName->m_Data,session->m_Notification->clientName->m_Size);
		}
		if(session->m_Notification->clientNameType)
		{
			dta->client_name_type = (Platform::ClientNameType)(*session->m_Notification->clientNameType);
		}
		if(session->m_Notification->requestorId)
		{
			memcpy(dta->requestor_id,session->m_Notification->requestorId->m_Data,session->m_Notification->requestorId->m_Size);
		}
		if(session->m_Notification->requestorIdType)
		{
			dta->requestor_id_type = (Platform::RequesterIDType)*session->m_Notification->requestorIdType;
		}
		if(session->m_Notification->encodingType)
		{
			dta->encoding_type = (Platform::EncodingType)*session->m_Notification->encodingType;
		}
		session->m_PendingMessage = message;

		// send out data
		Engine::CSUPLController::GetInstance().GetGPS().SendData(this, dta);

		return TRUE;
	}

	Engine::uint64_t CNotifyIndCmd::GetCmdCode()
	{
		return UPL_NOTIFY_IND;
	}

};
