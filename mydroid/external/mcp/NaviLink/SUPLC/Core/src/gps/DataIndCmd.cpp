/**
*  @file  DataIndCmd.cpp
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

#include "gps/DataIndCmd.h"


using namespace Engine;

namespace Platform {

CDataIndCmd::CDataIndCmd(Engine::uint16_t appId, Engine::CSession* ses, Engine::MSG* msg,void* dta):
	CGPSCommand(appId, ses, msg, dta)
{
	
}

CDataIndCmd::~CDataIndCmd()
{
	
}

Engine::bool_t CDataIndCmd::Execute()
{
	// this message goes from SUPL to GPSM	
	// get data
	DataIndData* dta = (DataIndData*)malloc(sizeof(DataIndData));
	if (dta == NULL)
	{
		return FALSE;
	}


	//saving Application Id
	dta->app_id = appId;//session->m_AppID;

	
	//save and send  Positioning Payload
	if(data != NULL)
	{
		Engine::MSG::SGPSData* pdu = (Engine::MSG::SGPSData*) data;
		dta->pos_payload.payload.ctrl_pdu_len = pdu->m_pdu_size;
		memcpy(dta->pos_payload.payload.ctrl_pdu, pdu->m_pdu, pdu->m_pdu_size);
		delete[] pdu->m_pdu;
		pdu->m_pdu = NULL;
		dta->vel.velocity_flag=0;
		if(session)
		{

		  if(session->m_PosProtocol.m_RRLP == TRUE)
		  	dta->pos_payload.type = Platform::SUPLPosPayload_t::RRLP_PAYLOAD;
		  
		  if(session->m_PosProtocol.m_RRC == TRUE)
		  	dta->pos_payload.type = Platform::SUPLPosPayload_t::RRC_PAYLOAD;
		  
		  else if(session->m_PosProtocol.m_TIA801== TRUE)
		  	dta->pos_payload.type = Platform::SUPLPosPayload_t::TIA_801_PAYLOAD;
		  	
		  if(session->m_pVelocity != NULL)
		  {
			switch(session->m_pVelocity->GetType())
			{
				case HOR_VEL:
					{
						Engine::CHorvel* hv_velocity = (Engine::CHorvel*)session->m_pVelocity;
						dta->vel.velocity_flag = 0;
						dta->vel.velocity_flag = BEARING | HORSPEED;
						dta->vel.bearing = hv_velocity->m_Bearing.ToUint16();
						dta->vel.horspeed = hv_velocity->m_HorSpeed.ToUint16();
					}
					break;
				case Engine::HOR_AND_VER_VEL:
					{
						Engine::CHorandvervel* hvv_velocity =  (Engine::CHorandvervel*)session->m_pVelocity;
						dta->vel.velocity_flag = 0;
						dta->vel.velocity_flag = BEARING | HORSPEED | VERDIRECT | VERSPEED;
						dta->vel.bearing = hvv_velocity->m_Bearing.ToUint16();
						dta->vel.horspeed = hvv_velocity->m_HorSpeed.ToUint16();
						dta->vel.verdirect = hvv_velocity->m_VerDirect.ToUint8();
						dta->vel.verspeed = hvv_velocity->m_VerSpeed.ToUint8();
					}
					break;

				case Engine::HOR_VEL_UNCERT:
					{
						Engine::CHorveluncert* hvu_velocity = (Engine::CHorveluncert*)session->m_pVelocity;
						dta->vel.velocity_flag = 0;
						dta->vel.velocity_flag = BEARING | HORSPEED | HORUNCERTSPEED;
						dta->vel.bearing = hvu_velocity->m_Bearing.ToUint16();
						dta->vel.horspeed = hvu_velocity->m_HorSpeed.ToUint16();
						dta->vel.horuncertspeed = hvu_velocity->m_UncertSpeed.ToUint8();
					}
					break;

				case Engine::HOR_AND_VER_UNCERT:
					{
						Engine::CHorandveruncert* hva_velocity = (Engine::CHorandveruncert*)session->m_pVelocity;
						dta->vel.velocity_flag = 0;
						dta->vel.velocity_flag = BEARING | HORSPEED | HORUNCERTSPEED | VERDIRECT | VERSPEED | VERUNCERTSPEED;
						dta->vel.bearing = hva_velocity->m_Bearing.ToUint16();
						dta->vel.horspeed = hva_velocity->m_HorSpeed.ToUint16();
						dta->vel.horuncertspeed = hva_velocity->m_HorUncertSpeed.ToUint8();
						dta->vel.verdirect = hva_velocity->m_VerDirect.ToUint8();
						dta->vel.verspeed = hva_velocity->m_VerSpeed.ToUint8();
						dta->vel.veruncertspeed = hva_velocity->m_VerUncertSpeed.ToUint8();
					}
					break;
				}
				session->m_pVelocity = NULL;
			}
		}
			// send out data
			CSUPLController::GetInstance().GetGPS().SendData(this, dta);

			return TRUE;
		}
	return FALSE;
}

Engine::uint64_t CDataIndCmd::GetCmdCode()
{
	return UPL_DATA_IND;
}

};
