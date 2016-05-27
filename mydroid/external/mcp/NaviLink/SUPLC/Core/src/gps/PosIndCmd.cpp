/**
*  @file  PosIndCmd.cpp
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

#include "gps/PosIndCmd.h"
#include <time.h>

namespace Platform {

	CPosIndCmd::CPosIndCmd(Engine::uint16_t appId, Engine::CSession* ses, Engine::MSG* msg, void* data):
	CGPSCommand(appId, ses, msg, data)
	{

	}

	CPosIndCmd::~CPosIndCmd()
	{

	}

	Engine::bool_t CPosIndCmd::Execute()
	{
		// this message goes from SUPL to GPSM
		// get pointer
		PosIndData* dta = (PosIndData*)malloc(sizeof(PosIndData));
		if (dta == NULL)
		{
			//include here sending FAIL command to GPS driver!!!
			return FALSE;
		}
		memset(dta,0,sizeof(PosIndData));
		//saving Application Id
		dta->app_id = appId;//session->m_AppID;

		//store position
		Engine::Position* position = (Engine::Position*)this->data;

		if(position != NULL)
		{
			//-----------------------------------------------mandatory parameters------------------

			//Time Stamp
			dta->position.UTCTimeStampNumByte = position->m_UTCTime.m_Size;
			memcpy(dta->position.UTCTimeStamp,position->m_UTCTime.m_Data,dta->position.UTCTimeStampNumByte);


			//Latitude Sign
			switch(position->m_PositionEstimate.m_LatitudeSign)
			{// lets assume our enum values are compatible with SUPL
			case Engine::NORTH	:
				dta->position.latitude_sign = 0;
				break;

			case Engine::SOUTH	:
				dta->position.latitude_sign = 1;
				break;
			}

			//Latitude
			dta->position.latitude = position->m_PositionEstimate.m_Latitude;

			//Longitude
			dta->position.longtitude = position->m_PositionEstimate.m_Longitude;


			// ------------------------------------------- optional parameters----------------------

			//Altitude
			Engine::AltitudeInfo* p_ainfo = position->m_PositionEstimate.m_pAltitudeInfo;
			if(p_ainfo == NULL)	// is Altitude set?
			{
				// dta->position.altitude is not set
				dta->position.pos_opt_bitmap &= !POS_ALTITUDE;
			}
			else
			{
				dta->position.pos_opt_bitmap |= POS_ALTITUDE;
				dta->position.altitude = p_ainfo->m_Altitude;
				dta->position.altUncertainty = p_ainfo->m_AltUncertainty;
				dta->position.altitudeDirection = p_ainfo->m_AltitudeDirection;
			}

			//Uncertainity
			Engine::Uncertainty* p_uncenrtainity = position->m_PositionEstimate.m_pUncertainty;

			//Uncertainity Semi Major
			if (p_uncenrtainity == NULL) // is Uncertainity Semi Major set?
			{
				//dta->position.uncertaintySemiMajor is not set
				dta->position.pos_opt_bitmap &= !POS_UNCERTAINTY;
			}
			else
			{
				dta->position.pos_opt_bitmap |= POS_UNCERTAINTY;
				dta->position.uncertaintySemiMajor = p_uncenrtainity->m_UncertaintySemiMajor;
				dta->position.uncertaintySemiMinor = p_uncenrtainity->m_UncertaintySeminMinor;
				dta->position.orientationMajorAxis = p_uncenrtainity->m_OrientationMajorAxis;
			}

			// Confidence
			Engine::uint8_t*  p_confidence = position->m_PositionEstimate.m_pConfidence;
			if(p_confidence == NULL)
			{
				//dta->position.confidence is not set
				dta->position.pos_opt_bitmap &= !POS_CONFIDENCE;
			}
			else
			{
				dta->position.pos_opt_bitmap |= POS_CONFIDENCE;
				dta->position.confidence = *p_confidence;
			}

			// Velocity
			if(!position->m_pVelocity)	// how to check is velocity set?
			{
				// dta->position.velocity is not set
				dta->position.pos_opt_bitmap &= !POS_VELOCITY;
			}
			else
			{
				// dta->position.velocity is set
				dta->position.pos_opt_bitmap |= POS_VELOCITY;

				switch(position->m_pVelocity->GetType())
				{
				case Engine::HOR_VEL:
					{
						Engine::CHorvel* hv_velocity = (Engine::CHorvel*)position->m_pVelocity;
						Platform::Velocity_t* p_velocity = &dta->position.velocity;
						p_velocity->velocity_flag = 0;
						p_velocity->velocity_flag = BEARING | HORSPEED;
						p_velocity->bearing = hv_velocity->m_Bearing.ToUint16();
						p_velocity->horspeed = hv_velocity->m_HorSpeed.ToUint16();
					}
					break;

				case Engine::HOR_AND_VER_VEL:
					{
						Engine::CHorandvervel* hvv_velocity =  (Engine::CHorandvervel*)position->m_pVelocity;
						Platform::Velocity_t* p_velocity = &dta->position.velocity;
						p_velocity->velocity_flag = 0;
						p_velocity->velocity_flag = BEARING | HORSPEED | VERDIRECT | VERSPEED;
						p_velocity->bearing = hvv_velocity->m_Bearing.ToUint16();
						p_velocity->horspeed = hvv_velocity->m_HorSpeed.ToUint16();
						p_velocity->verdirect = hvv_velocity->m_VerDirect.ToUint8();
						p_velocity->verspeed = hvv_velocity->m_VerSpeed.ToUint8();
					}
					break;

				case Engine::HOR_VEL_UNCERT:
					{
						Engine::CHorveluncert* hvu_velocity = (Engine::CHorveluncert*)position->m_pVelocity;
						Platform::Velocity_t* p_velocity = &dta->position.velocity;
						p_velocity->velocity_flag = 0;
						p_velocity->velocity_flag = BEARING | HORSPEED | HORUNCERTSPEED;
						p_velocity->bearing = hvu_velocity->m_Bearing.ToUint16();
						p_velocity->horspeed = hvu_velocity->m_HorSpeed.ToUint16();
						p_velocity->horuncertspeed = hvu_velocity->m_UncertSpeed.ToUint8();
					}
					break;

				case Engine::HOR_AND_VER_UNCERT:
					{
						Engine::CHorandveruncert* hva_velocity = (Engine::CHorandveruncert*)position->m_pVelocity;
						Platform::Velocity_t* p_velocity = &dta->position.velocity;
						p_velocity->velocity_flag = 0;
						p_velocity->velocity_flag = BEARING | HORSPEED | HORUNCERTSPEED | VERDIRECT | VERSPEED | VERUNCERTSPEED;
						p_velocity->bearing = hva_velocity->m_Bearing.ToUint16();
						p_velocity->horspeed = hva_velocity->m_HorSpeed.ToUint16();
						p_velocity->horuncertspeed = hva_velocity->m_HorUncertSpeed.ToUint8();
						p_velocity->verdirect = hva_velocity->m_VerDirect.ToUint8();
						p_velocity->verspeed = hva_velocity->m_VerSpeed.ToUint8();
						p_velocity->veruncertspeed = hva_velocity->m_VerUncertSpeed.ToUint8();
					}
					break;
				}

			}
			delete position;
			// send out data
			Engine::CSUPLController::GetInstance().GetGPS().SendData(this, dta);
			//session->SetSessionStatus(Engine::SESSION_READY_TO_DIE);
			return TRUE;

		}// end of sending position code
		//session->SetSessionStatus(Engine::SESSION_READY_TO_DIE);
		return FALSE;
	}

	Engine::uint64_t CPosIndCmd::GetCmdCode()
	{
		return UPL_POS_IND;
	}

};
