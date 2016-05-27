/**
*  @file  Position.h
*  @brief Position declaration.
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
Alexander V. Morozov         08.11.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#include "common/Position.h"

using namespace std;

namespace Engine {


Position::Position():m_pVelocity(NULL),m_ObjectReady(TRUE)
{
	
}

Position::~Position()
{
	if (m_pVelocity != NULL)
	{
		delete m_pVelocity;
	}

	m_UTCTime.Clean();
}
	
uint8_t Position::Copy(const Position& cpy)
{
	if (this == &cpy)
	{
		return RETURN_OK;
	}
	if(m_pVelocity)
	{
		delete m_pVelocity;
		m_pVelocity = NULL;
	}

	m_pVelocity = NULL;
	if(m_UTCTime.Copy(cpy.m_UTCTime) != RETURN_OK)
		return RETURN_FATAL;
	if(m_PositionEstimate.Copy(cpy.m_PositionEstimate) != RETURN_OK)
		return RETURN_FATAL;
	
	if (cpy.m_pVelocity == NULL)
	{
		return RETURN_OK;
	}
	switch (cpy.m_pVelocity->GetType())
	{
		case HOR_VEL:
		{	
			CHorvel* tmp = new(nothrow) CHorvel();
			if(!tmp)
				return RETURN_FATAL;
			if(tmp->Copy(*((CHorvel*) cpy.m_pVelocity)) != RETURN_OK)
				return RETURN_FATAL;
			m_pVelocity = tmp;
			break;
		}
		case HOR_AND_VER_VEL:
		{
			CHorandvervel* tmp = new(nothrow) CHorandvervel();
			if(!tmp)
				return RETURN_FATAL;
			(*tmp).Copy(*((CHorandvervel*) cpy.m_pVelocity));
			m_pVelocity = tmp;
			break;
		}
		case HOR_VEL_UNCERT:
		{
			CHorveluncert* tmp = new(nothrow) CHorveluncert();
			if(!tmp)
				return RETURN_FATAL;
			(*tmp).Copy(*((CHorveluncert*) cpy.m_pVelocity));
			m_pVelocity = tmp;
			break;
		}
		case HOR_AND_VER_UNCERT:
		{
			CHorandveruncert* tmp = new(nothrow) CHorandveruncert();
			if(!tmp)
				return RETURN_FATAL;
			(*tmp).Copy(*((CHorandveruncert*) cpy.m_pVelocity));
			m_pVelocity = tmp;
			break;
		}
		default:
			return RETURN_FATAL;
	}
	return RETURN_OK;
}

PositionEstimate::PositionEstimate():
	m_pUncertainty(NULL),
	m_pConfidence(NULL),
	m_pAltitudeInfo(NULL),
	m_ObjectReady(TRUE),
	/* KW changes*/
	m_Latitude(0),
	m_Longitude(0)
{
	
}

uint8_t PositionEstimate::Copy(const PositionEstimate& cpy)
{
	if (m_pUncertainty)
	{
		delete m_pUncertainty;
	}
	if (m_pConfidence)
	{
		delete m_pConfidence;
	}
	if (m_pAltitudeInfo)
	{
		delete m_pAltitudeInfo;
	}

	m_pUncertainty = NULL;
	m_pConfidence = NULL;
	m_pAltitudeInfo = NULL;

	m_Latitude = cpy.m_Latitude;
	m_LatitudeSign = cpy.m_LatitudeSign;
	m_Longitude = cpy.m_Longitude;
	
	if (cpy.m_pUncertainty)
	{
		m_pUncertainty = new(nothrow) Uncertainty;
		if(!m_pUncertainty)
			return RETURN_FATAL;
		*m_pUncertainty = *cpy.m_pUncertainty;
	}
	if (cpy.m_pConfidence)
	{
		m_pConfidence = new(nothrow) uint8_t;
		if(!m_pConfidence)
			return RETURN_FATAL;
		*m_pConfidence = *cpy.m_pConfidence;
	}
	if (cpy.m_pAltitudeInfo)
	{
		m_pAltitudeInfo = new(nothrow) AltitudeInfo;
		if(!m_pAltitudeInfo)
			return RETURN_FATAL;
		*m_pAltitudeInfo = *cpy.m_pAltitudeInfo;
	}
	return RETURN_OK;
}

PositionEstimate::~PositionEstimate()
{
	if (m_pUncertainty != NULL)
	{
		delete m_pUncertainty;
	}
	if (m_pConfidence != NULL)
	{
		delete m_pConfidence;
	}
	if (m_pAltitudeInfo != NULL)
	{
		delete m_pAltitudeInfo;
	}	
}

} // end of namesapce Engine
