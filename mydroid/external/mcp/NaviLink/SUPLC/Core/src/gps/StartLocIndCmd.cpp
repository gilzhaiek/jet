/**
*  @file  StartLocIndCmd.cpp
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

#include "gps/StartLocIndCmd.h"


namespace Platform {

	CStartLocIndCmd::CStartLocIndCmd(Engine::uint16_t appId, Engine::CSession* ses, Engine::MSG* msg, void* data):
	CGPSCommand(appId, ses, msg, data)
	{

	}

	CStartLocIndCmd::~CStartLocIndCmd()
	{

	}

	Engine::bool_t CStartLocIndCmd::Execute()
	{
		StartLocIndData* dta = (StartLocIndData*)malloc(sizeof(StartLocIndData));
		if (dta == NULL)
		{
			return FALSE;
		}


		//saving Application Id
		dta->app_id = session->m_AppID;

		dta->resp_opt_bitmap = 0;
		dta->resp_opt_bitmap |= LOC_FIX_MODE;
		if(session->iQoP)
		{
			dta->resp_opt_bitmap |= START_LOC_QOP_BIT;
			dta->qop.horr_acc = session->iQoP->m_Horacc;
			if(session->iQoP->m_pDelay)
			{
				dta->qop.qop_optional_bitmap |= DELAY;
				dta->qop.delay = *session->iQoP->m_pDelay;
			}
			if(session->iQoP->m_pMaxLocAge)
			{
				dta->qop.qop_optional_bitmap |= MAX_LOC_AGE;
				dta->qop.max_loc_age = *session->iQoP->m_pMaxLocAge;
			}
			if(session->iQoP->m_pVeracc)
			{
				dta->qop.qop_optional_bitmap |= VER_ACC;
				dta->qop.ver_acc = *session->iQoP->m_pVeracc;
			}
		}

		if(session->m_UsedPosTechnology == Engine::POS_METHOD_AGPS_SET_ASSISTED || session->m_UsedPosTechnology == Engine::POS_METHOD_AGPS_SET_ASSISTED_PREF)
			dta->loc_fix_mode = Platform::MSASSISTED;
		else if(session->m_UsedPosTechnology == Engine::POS_METHOD_AGPS_SET_BASED || session->m_UsedPosTechnology == Engine::POS_METHOD_AGPS_SET_BASED_PREF)
			dta->loc_fix_mode = Platform::MSBASED;
		else if(session->m_UsedPosTechnology == Engine::POS_METHOD_AUTONOMUS_GPS)
			dta->loc_fix_mode = Platform::AUTONOMOUS;
		else if(session->m_UsedPosTechnology == Engine::POS_METHOD_AFLT)
			dta->loc_fix_mode = Platform::AFLT;
		else if(session->m_UsedPosTechnology == Engine::POS_METHOD_ECID)
			dta->loc_fix_mode = Platform::E_CID;
		else if(session->m_UsedPosTechnology == Engine::POS_METHOD_EOTD)
			dta->loc_fix_mode = Platform::E_OTD;
		else if(session->m_UsedPosTechnology == Engine::POS_METHOD_OTDOA)
			dta->loc_fix_mode = Platform::OTDOA;
		else if(session->m_UsedPosTechnology == Engine::POS_METHOD_NO_POSITION)
			dta->loc_fix_mode = Platform::NO_POSITIONING;
		else 
			dta->loc_fix_mode = Platform::AUTONOMOUS;

		Engine::CSUPLController::GetInstance().GetGPS().SendData(this, dta);

		return TRUE;
	}

	Engine::uint64_t CStartLocIndCmd::GetCmdCode()
	{
		return UPL_START_LOC_IND;
	}

};
