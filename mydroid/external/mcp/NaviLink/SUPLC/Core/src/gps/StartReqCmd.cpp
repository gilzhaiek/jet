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
Alexander V. Morozov         09.01.2008                   initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/
#include "gps/StartReqCmd.h"
#include "utils/Log.h"
namespace Platform
{



CStartReqCmd::CStartReqCmd(Engine::uint16_t appId, Engine::CSession* ses, Engine::MSG* msg, void* data):
	CGPSCommand(appId, ses, msg, data)
{
	StartReqData *dta = (StartReqData *)data;
}

CStartReqCmd::~CStartReqCmd()
{
	
}

Engine::bool_t CStartReqCmd::Execute()
{	
	StartReqData* dta = (StartReqData*) this->data;

	//session->m_AppID = dta->app_id;
	// copy data in internal structures.
	/*
		1. save set capabilities
	*/
	if (dta == NULL)
	{
		return FALSE;
	}

	LOGD("SUPL:In StartReqData handle");
	//session->m_AppID = dta->app_id;
#if defined(_DEBUG_IMSI)
	Engine::CSUPLController::GetInstance().GetDevice().SetIMSI(dta->imsi.imsi_buf,dta->imsi.imsi_size);
#endif

	// save supported protocol type.
	session->m_PosProtocol.m_TIA801 = ((dta->set.pos_protocol_bitmap & TIA_801_MASK) != 0x0);
	session->m_PosProtocol.m_RRLP = ((dta->set.pos_protocol_bitmap & RRLP_MASK) != 0x0);
	session->m_PosProtocol.m_RRC = ((dta->set.pos_protocol_bitmap & RRC_MASK) != 0x0);	
	// save pref method
	session->m_PrefMethod = (Engine::PrefMethod) dta->set.pref_method;
	// save supported technologies
	session->iPosTechnology.m_aFLT = ((dta->set.pos_technology_bitmap & AFLT_OFFSET) != 0x0);
	session->iPosTechnology.m_AGPSSETAssisted = ((dta->set.pos_technology_bitmap & SET_ASSISTED_AGPS_OFFSET) != 0x0);
	session->iPosTechnology.m_AGPSSETBased = ((dta->set.pos_technology_bitmap & SET_BASED_AGPS_OFFSET) != 0x0);
	session->iPosTechnology.m_AutonomusGPS = ((dta->set.pos_technology_bitmap & AUTONOMUS_GPS_OFFSET) != 0x0);
	session->iPosTechnology.m_eCID = ((dta->set.pos_technology_bitmap & E_CID_OFFSET) != 0x0);
	session->iPosTechnology.m_eOTD = ((dta->set.pos_technology_bitmap & E_OTD_OFFSET) != 0x0);
	session->iPosTechnology.m_oTDOA = ((dta->set.pos_technology_bitmap & OTDOA_OFFSET) != 0x0);
	
	/*
		2. save location id
	*/
	Engine::LocationID& loc = session->m_LocationID;
	if(dta->lac.cell_info.cell_type == CellInfo_t/*::CELL_TYPE*/::GSM)
	{
		loc.m_pCellInformation = new(std::nothrow) Engine::GSMCellInformation;
		if(!loc.m_pCellInformation)
		{
			free(dta);
			dta = NULL;
			return FALSE;
		}
		Engine::GSMCellInformation* inf = (Engine::GSMCellInformation*) loc.m_pCellInformation;
		LOGD("\n\nStartReqCmd:gsm_mcc = %d\n\n",dta->lac.cell_info.cell_info_gsm.gsm_mcc);
		LOGD("\n\nStartReqCmd:gsm_mnc = %d\n\n",dta->lac.cell_info.cell_info_gsm.gsm_mnc);
		LOGD("\n\nStartReqCmd:gsm_ci = %d\n\n",dta->lac.cell_info.cell_info_gsm.gsm_ci);
		LOGD("\n\nStartReqCmd:gsm_lac = %d\n\n",dta->lac.cell_info.cell_info_gsm.gsm_lac);
		inf->m_MCC = dta->lac.cell_info.cell_info_gsm.gsm_mcc;
		inf->m_MNC = dta->lac.cell_info.cell_info_gsm.gsm_mnc;
		inf->m_LAC = dta->lac.cell_info.cell_info_gsm.gsm_lac;
		inf->m_CI = dta->lac.cell_info.cell_info_gsm.gsm_ci;
		if(dta->lac.cell_info.cell_info_gsm.gsm_opt_param & TA_BIT)
		{
			inf->m_TA = new(std::nothrow)  Engine::uint8_t;
			if(!inf->m_TA)
			{
				free(dta);
				dta = NULL;
				return FALSE;
			}
			*inf->m_TA = dta->lac.cell_info.cell_info_gsm.gsm_ta;
			// copy NMR's
		}
		else
		{
			inf->m_TA=NULL;
		}
		if(dta->lac.cell_info.cell_info_gsm.gsm_opt_param & MEASUREMENT_REPORT_BIT)
		{
			if (dta->lac.cell_info.cell_info_gsm.nmr_quantity > 0)
			{		
				//inf->m_aNMR = new(std::nothrow)  std::vector<Engine::NMR>();
				inf->m_aNMR = new(std::nothrow)  android::Vector<Engine::NMR>();  //Android STL
				if(!inf->m_aNMR)
				{
					free(dta);
					dta = NULL;
					return FALSE;
				}
				// copy nmr's array.
				for (int i = 0; i < dta->lac.cell_info.cell_info_gsm.nmr_quantity; i++)
				{
					Engine::NMR nmr;
					nmr.m_RFCN = dta->lac.cell_info.cell_info_gsm.gsm_nmr[i].arfcn;
					nmr.m_SIC = dta->lac.cell_info.cell_info_gsm.gsm_nmr[i].bsic;
					nmr.m_Lev = dta->lac.cell_info.cell_info_gsm.gsm_nmr[i].rxlev;
					//inf->m_aNMR->push_back(nmr);
					inf->m_aNMR->push(nmr); //Android STL
				}
			}
			else
			{
				inf->m_aNMR = NULL;
			}
		}
		else
		{
			inf->m_aNMR = NULL;
		}
		LOGD("\n\nStartReqCmd.cpp:m_MCC = %d\n\n",inf->m_MCC);
		LOGD("\n\nStartReqCmd.cpp:m_MNC = %d\n\n",inf->m_MNC);
		LOGD("\n\nStartReqCmd.cpp:m_CI = %d\n\n",inf->m_CI);
		LOGD("\n\nStartReqCmd.cpp:m_LAC = %d\n\n",inf->m_LAC);
		switch(dta->lac.cell_info_status)
		{
		case PRESENT_CELL_INFO:		loc.m_Status = Engine::CURRENT;		break;
		case UNKNOWN_CELL_INFO:		loc.m_Status = Engine::UNKNOWN;		break;
		case PREVIOUS_CELL_INFO:	loc.m_Status = Engine::NO_CURRENT;	break;
		case NO_CELL_INFO:			loc.m_Status = Engine::NO_CURRENT;	break;
		default: loc.m_Status = Engine::UNKNOWN; break;
		}
	}
	else if(dta->lac.cell_info.cell_type == CellInfo_t/*::CELL_TYPE*/::WCDMA)
	{
		LOGD("\n\nStartReqCmd.cpp:----Filling WCDMA INFO in Message----\n\n");	
		loc.m_pCellInformation = new(std::nothrow) Engine::WCDMACellInformation;
		if(!loc.m_pCellInformation)
		{
			free(dta);
			dta = NULL;
			return FALSE;
		}
		Engine::WCDMACellInformation *inf = (Engine::WCDMACellInformation*) loc.m_pCellInformation;
		LOGD("\n\nStartReqCmd:wcdma_mcc = %ld\n\n",dta->lac.cell_info.cell_info_wcdma.wcdma_mcc);
		LOGD("\n\nStartReqCmd:wcdma_mnc = %ld\n\n",dta->lac.cell_info.cell_info_wcdma.wcdma_mnc);
		LOGD("\n\nStartReqCmd:wcdma_ci = %ld\n\n",dta->lac.cell_info.cell_info_wcdma.wcdma_ci);
		inf->m_MCC = dta->lac.cell_info.cell_info_wcdma.wcdma_mcc;
		inf->m_MNC = dta->lac.cell_info.cell_info_wcdma.wcdma_mnc;
		inf->m_CI = dta->lac.cell_info.cell_info_wcdma.wcdma_ci;
		inf->m_frequencyInfo = NULL;
		inf->m_primaryScramblingCode = NULL;
		inf->m_measuredResultsList = NULL;
		LOGD("\n\nStartReqCmd.cpp:m_MCC = %ld\n\n",inf->m_MCC);
		LOGD("\n\nStartReqCmd.cpp:m_MNC = %ld\n\n",inf->m_MNC);
		LOGD("\n\nStartReqCmd.cpp:m_CI = %ld\n\n",inf->m_CI);

		switch(dta->lac.cell_info_status)
		{
		case PRESENT_CELL_INFO:		loc.m_Status = Engine::CURRENT;		break;
		case UNKNOWN_CELL_INFO:		loc.m_Status = Engine::UNKNOWN;		break;
		case PREVIOUS_CELL_INFO:	loc.m_Status = Engine::NO_CURRENT;	break;
		case NO_CELL_INFO:			loc.m_Status = Engine::NO_CURRENT;	break;
		default: loc.m_Status = Engine::UNKNOWN; break;
		}
	}
	else
	{
		free(dta);
		dta = NULL;
		return FALSE; //We currently not support CDMA/WCDMA
	}
	
	/*
		3. save QoP
	*/
	if (session->iQoP == NULL && dta->start_opt_param & QOP)
	{
		session->iQoP = new(std::nothrow)  Engine::QoP();
		if(!session->iQoP)
		{
			free(dta);
            dta = NULL;
			return FALSE;
		}
		if(!session->iQoP->IsObjectReady())
		{
			free(dta);
			dta = NULL;
			return FALSE;
		}
		session->iQoP->m_Horacc = dta->qop.horr_acc;
		if(dta->qop.qop_optional_bitmap & DELAY)
		{
			session->iQoP->m_pDelay = new(std::nothrow)  Engine::uint8_t;
			if(!session->iQoP->m_pDelay)
			{
				free(dta);
				dta = NULL;
				return FALSE;
			}
			*session->iQoP->m_pDelay = dta->qop.delay;
		}
		if(dta->qop.qop_optional_bitmap & MAX_LOC_AGE)
		{
			session->iQoP->m_pMaxLocAge = new(std::nothrow)  Engine::uint32_t;
			if(!session->iQoP->m_pMaxLocAge)
			{
				free(dta);
				dta = NULL;
				return FALSE;
			}
			*session->iQoP->m_pMaxLocAge = dta->qop.max_loc_age;
		}
		if(dta->qop.qop_optional_bitmap & VER_ACC)
		{
			session->iQoP->m_pVeracc = new(std::nothrow)  Engine::uint8_t;
			if(!session->iQoP->m_pVeracc)
			{
				free(dta);
				dta = NULL;
				return FALSE;
			}
			*session->iQoP->m_pVeracc = dta->qop.ver_acc;
		}
	}
	/*
		4. save RAD
	*/
	if(dta->start_opt_param & REQUEST_ASSISTANCE)
	{
		if (session->m_RAD == NULL)
		{
			session->m_RAD = new(std::nothrow)  Engine::RequestedAssistData();
			if(!session->m_RAD)
			{
				free(dta);
				dta = NULL;
				return FALSE;
			}
		}

		session->m_RAD->m_AlmanacRequested = (dta->a_data.assitance_req_bitmap & ALMANAC_REQ_MASK) >> ALMANAC_REQ_OFFSET;
		session->m_RAD->m_UTCModeRequested = (dta->a_data.assitance_req_bitmap & UTC_MODE_REQ_MASK) >> UTC_MODE_REQ_OFFSET;
		session->m_RAD->m_IonosphericModeRequested = (dta->a_data.assitance_req_bitmap & IONOSPHERIC_MODE_REQ_MASK) >> IONOSPHERIC_MODE_REQ_OFFSET;
		session->m_RAD->m_DGPSCorrectionsRequested = (dta->a_data.assitance_req_bitmap & DGPS_CORRECTIONS_REQ_MASK) >> DGPS_CORRECTIONS_REQ_OFFSET;
		session->m_RAD->m_ReferenceLocationRequested = (dta->a_data.assitance_req_bitmap & REFERENCE_LOCATION_REQ_MASK) >> REFERENCE_LOCATION_REQ_OFFSET;
		session->m_RAD->m_ReferenceTimeRequested = (dta->a_data.assitance_req_bitmap & REFERENCE_TIME_REQ_MASK) >> REFERENCE_TIME_REQ_OFFSET;
		session->m_RAD->m_AcquisitionAssistanceRequested = (dta->a_data.assitance_req_bitmap & ACQUI_ASSIST_REQ_MASK) >> ACQUI_ASSIST_REQ_OFFSET;
		session->m_RAD->m_RealTimeIntegrityRequested = (dta->a_data.assitance_req_bitmap & REAL_TIME_INT_REQ_MASK) >> REAL_TIME_INT_REQ_OFFSET;
		session->m_RAD->m_NavigationModelRequested = (dta->a_data.assitance_req_bitmap & NAVIGATION_MODEL_REQ_MASK) >> NAVIGATION_MODEL_REQ_OFFSET;
	
		if(session->m_RAD->m_NavigationModelRequested)
		{
			session->m_RAD->m_pNavigationModelData = new(std::nothrow)  Engine::NavigationModel;
			if(!session->m_RAD->m_pNavigationModelData)
			{
				free(dta);
				dta = NULL;
				return FALSE;
			}
			session->m_RAD->m_pNavigationModelData->m_GPSToe = dta->a_data.gpsToe;
			session->m_RAD->m_pNavigationModelData->m_GPSWeek = dta->a_data.gpsWeak;
			session->m_RAD->m_pNavigationModelData->m_NSAT = dta->a_data.nSAT;
			session->m_RAD->m_pNavigationModelData->m_ToeLimit = dta->a_data.toeLimit;
			if(dta->a_data.num_sat_info > 0)
			{
				//session->m_RAD->m_pNavigationModelData->m_SatInfo = new(std::nothrow)  std::vector<Engine::SatelliteInfoElement>();
				session->m_RAD->m_pNavigationModelData->m_SatInfo = new(std::nothrow)  android::Vector<Engine::SatelliteInfoElement>(); //Android STL
				if(!session->m_RAD->m_pNavigationModelData->m_SatInfo)
				{
					free(dta);
					dta = NULL;
					return FALSE;
				}
				for (int i = 0; i < dta->a_data.num_sat_info; i++)
				{
					Engine::SatelliteInfoElement sat_info;
					sat_info.m_IODE = dta->a_data.sat_info_elemnet[0].iode;
					sat_info.m_SatID = dta->a_data.sat_info_elemnet[0].satelliteID;
					//session->m_RAD->m_pNavigationModelData->m_SatInfo->push_back(sat_info);
					session->m_RAD->m_pNavigationModelData->m_SatInfo->push(sat_info); //Android STL
				}
			}
			else
			{
				session->m_RAD->m_pNavigationModelData->m_SatInfo = NULL;
			}
		}
		else
		{
			session->m_RAD->m_pNavigationModelData = NULL;
		}
		if((dta->a_data.assitance_req_bitmap & EXTENDED_EPHEMERIS_REQ_MASK))//rsuvorov extended ephemeris improvement
		{
			session->m_RAD->m_pVer2RequestedAssistDataExtension = new(std::nothrow)  Engine::Ver2RequestedAssistDataExtension;
			if(!session->m_RAD->m_pVer2RequestedAssistDataExtension)
			{
				free(dta);
				dta = NULL;
				return FALSE;
			}
			if(dta->a_data.ee_info.validity>0)
			{
				session->m_RAD->m_pVer2RequestedAssistDataExtension->m_pExtendedEphemeris=new(std::nothrow) Engine::CExtendedEphemeris;
				session->m_RAD->m_pVer2RequestedAssistDataExtension->m_pExtendedEphemeris->validity=dta->a_data.ee_info.validity;
			}
			if((dta->a_data.ee_info.StartCurExt.week!=0) || (dta->a_data.ee_info.StartCurExt.HourOfWeek!=0)||(dta->a_data.ee_info.EndCurExt.week!=0) ||(dta->a_data.ee_info.EndCurExt.HourOfWeek!=0))
			{
				session->m_RAD->m_pVer2RequestedAssistDataExtension->m_pExtendedEphemerisCheck = new (std::nothrow) Engine::CExtendedEphCheck;
				session->m_RAD->m_pVer2RequestedAssistDataExtension->m_pExtendedEphemerisCheck->m_pBeginTime.m_GpsWeek=dta->a_data.ee_info.StartCurExt.week;
				session->m_RAD->m_pVer2RequestedAssistDataExtension->m_pExtendedEphemerisCheck->m_pBeginTime.m_GpsTOWHour=dta->a_data.ee_info.StartCurExt.HourOfWeek;

				session->m_RAD->m_pVer2RequestedAssistDataExtension->m_pExtendedEphemerisCheck->m_pEndTime.m_GpsWeek=dta->a_data.ee_info.EndCurExt.week;
				session->m_RAD->m_pVer2RequestedAssistDataExtension->m_pExtendedEphemerisCheck->m_pEndTime.m_GpsTOWHour=dta->a_data.ee_info.EndCurExt.HourOfWeek;
			}
			else
			{
				session->m_RAD->m_pVer2RequestedAssistDataExtension->m_pExtendedEphemerisCheck=0;
			}
		}
		else
		{
			session->m_RAD->m_pVer2RequestedAssistDataExtension=NULL;
		}
	}
	else
	{
		session->m_RAD = NULL;
	}

	
	if(dta->start_opt_param & POSITION)
	{
		session->m_SuplPosPosition = new(std::nothrow)  Engine::Position;
		if(!session->m_SuplPosPosition)
		{
			free(dta);
			dta = NULL;
			return FALSE;
		}


		session->m_SuplPosPosition->m_UTCTime.m_Data = new(nothrow) uint8_t[dta->position.UTCTimeStampNumByte]; //Kaushal.k - Need to delete
		if(!session->m_SuplPosPosition->m_UTCTime.m_Data)
		{
			free(dta);
			dta = NULL;
			return FALSE;
		}
		memset(session->m_SuplPosPosition->m_UTCTime.m_Data,0x00,dta->position.UTCTimeStampNumByte);
		memcpy(session->m_SuplPosPosition->m_UTCTime.m_Data,dta->position.UTCTimeStamp,dta->position.UTCTimeStampNumByte);
		session->m_SuplPosPosition->m_UTCTime.m_Size=dta->position.UTCTimeStampNumByte; 

		LOGD("\n\nStartReqCmd.cpp:UTCTimeStampNumByte = %d\n\n",session->m_SuplPosPosition->m_UTCTime.m_Size);
		LOGD("\n\nStartReqCmd.cpp:UTCTimeStampNumByte = %c%c%c%c%c%c\n\n",session->m_SuplPosPosition->m_UTCTime.m_Data[0],session->m_SuplPosPosition->m_UTCTime.m_Data[1],session->m_SuplPosPosition->m_UTCTime.m_Data[2],session->m_SuplPosPosition->m_UTCTime.m_Data[3],session->m_SuplPosPosition->m_UTCTime.m_Data[4],session->m_SuplPosPosition->m_UTCTime.m_Data[5]);
		
		
		/* Mandatory parameters in position*/
		session->m_SuplPosPosition->m_PositionEstimate.m_Latitude = dta->position.latitude;
		session->m_SuplPosPosition->m_PositionEstimate.m_LatitudeSign = (Engine::LatitudeSign)dta->position.latitude_sign;
		session->m_SuplPosPosition->m_PositionEstimate.m_Longitude = dta->position.longtitude;
	
		if(dta->position.pos_opt_bitmap & POS_ALTITUDE)
		{
			session->m_SuplPosPosition->m_PositionEstimate.m_pAltitudeInfo = new(std::nothrow)  Engine::AltitudeInfo;
			if(!session->m_SuplPosPosition->m_PositionEstimate.m_pAltitudeInfo)
			{
				free(dta);
				dta = NULL;
				return FALSE;
			}
			session->m_SuplPosPosition->m_PositionEstimate.m_pAltitudeInfo->m_Altitude = dta->position.altitude;
			session->m_SuplPosPosition->m_PositionEstimate.m_pAltitudeInfo->m_AltitudeDirection = (Engine::AltitudeDirection)dta->position.altitudeDirection;
			session->m_SuplPosPosition->m_PositionEstimate.m_pAltitudeInfo->m_AltUncertainty = dta->position.altUncertainty;
		}
		else
		{
			session->m_SuplPosPosition->m_PositionEstimate.m_pAltitudeInfo = NULL;
		}
		if(dta->position.pos_opt_bitmap & POS_CONFIDENCE)
		{
			session->m_SuplPosPosition->m_PositionEstimate.m_pConfidence = new(std::nothrow)  Engine::uint8_t;
			if(!session->m_SuplPosPosition->m_PositionEstimate.m_pConfidence)
			{
				free(dta);
				dta = NULL;
				return FALSE;
			}
			*(session->m_SuplPosPosition->m_PositionEstimate.m_pConfidence) = dta->position.confidence;
		}
		else
		{
			session->m_SuplPosPosition->m_PositionEstimate.m_pConfidence = NULL;
		}
		if(dta->position.pos_opt_bitmap & POS_UNCERTAINTY)
		{
			session->m_SuplPosPosition->m_PositionEstimate.m_pUncertainty = new(std::nothrow)  Engine::Uncertainty;
			if(!session->m_SuplPosPosition->m_PositionEstimate.m_pUncertainty)
			{
				free(dta);
				dta = NULL;
				return FALSE;
			}
			session->m_SuplPosPosition->m_PositionEstimate.m_pUncertainty->m_OrientationMajorAxis = dta->position.orientationMajorAxis;
			session->m_SuplPosPosition->m_PositionEstimate.m_pUncertainty->m_UncertaintySemiMajor = dta->position.uncertaintySemiMajor;
			session->m_SuplPosPosition->m_PositionEstimate.m_pUncertainty->m_UncertaintySeminMinor = dta->position.uncertaintySemiMinor;
		}
		else
		{
			session->m_SuplPosPosition->m_PositionEstimate.m_pUncertainty = NULL;
		}
		if(dta->position.pos_opt_bitmap & POS_VELOCITY)
		{
			if(dta->position.velocity.velocity_flag & (BEARING | HORSPEED))
			{
				Engine::CHorvel* velocity = new(std::nothrow)  Engine::CHorvel;
				if(!velocity)	
				{
					free(dta);
					dta = NULL;
					return FALSE;
				}
				session->m_SuplPosPosition->m_pVelocity = velocity;
				velocity->m_Bearing.Copy(dta->position.velocity.bearing);
				velocity->m_HorSpeed.Copy(dta->position.velocity.horspeed);
			}
			else if(dta->position.velocity.velocity_flag & (BEARING | HORSPEED | VERDIRECT |VERSPEED))
			{
				Engine::CHorandvervel* velocity = new(std::nothrow)  Engine::CHorandvervel;
				if(!velocity)	
				{
					free(dta);
					dta = NULL;
					return FALSE;
				}
				session->m_SuplPosPosition->m_pVelocity = velocity;
				velocity->m_Bearing.Copy(dta->position.velocity.bearing);
				velocity->m_HorSpeed.Copy(dta->position.velocity.horspeed);
				velocity->m_VerDirect.Copy(dta->position.velocity.verdirect);
				velocity->m_VerSpeed.Copy(dta->position.velocity.verspeed);
			}
			else if(dta->position.velocity.velocity_flag & (BEARING | HORSPEED | HORUNCERTSPEED))
			{
					Engine::CHorveluncert* velocity = new(std::nothrow)  Engine::CHorveluncert;
					if(!velocity)	
					{
						free(dta);
						dta = NULL;
						return FALSE;
					}
					session->m_SuplPosPosition->m_pVelocity = velocity;
					velocity->m_Bearing.Copy(dta->position.velocity.bearing);
					velocity->m_HorSpeed.Copy(dta->position.velocity.horspeed);
					velocity->m_UncertSpeed.Copy(dta->position.velocity.horuncertspeed);
			}
			else if(dta->position.velocity.velocity_flag & (BEARING | HORSPEED | HORUNCERTSPEED | VERDIRECT | VERSPEED | VERUNCERTSPEED))
			{
				    Engine::CHorandveruncert* velocity = new(std::nothrow)  Engine::CHorandveruncert;
					if(!velocity)	
					{
						free(dta);
						dta = NULL;
						return FALSE;
					}
					session->m_SuplPosPosition->m_pVelocity = velocity;
					velocity->m_Bearing.Copy(dta->position.velocity.bearing);
					velocity->m_HorSpeed.Copy(dta->position.velocity.horspeed);
					velocity->m_HorUncertSpeed.Copy(dta->position.velocity.horuncertspeed);
					velocity->m_VerDirect.Copy(dta->position.velocity.verdirect);
					velocity->m_VerSpeed.Copy(dta->position.velocity.verspeed);
					velocity->m_VerUncertSpeed.Copy(dta->position.velocity.veruncertspeed);
			}
		}
		else
		{
			session->m_SuplPosPosition->m_pVelocity = NULL;
		}
	}
	else
	{
		session->m_SuplPosPosition = NULL;
	}
	
	free(dta);
	dta = NULL;
	return TRUE;
}

Engine::uint64_t CStartReqCmd::GetCmdCode()
{
	return UPL_START_REQ;
}


};
