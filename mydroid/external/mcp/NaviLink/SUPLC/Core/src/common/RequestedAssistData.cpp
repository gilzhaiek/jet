/**
*  @file  RequestedAssistData.h
*  @brief RequestedAssistData declaration.
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
Alexander V. Morozov         09.11.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#include "common/RequestedAssistData.h"

using namespace std;

namespace Engine {

RequestedAssistData::RequestedAssistData():
	m_pNavigationModelData(NULL),m_pVer2RequestedAssistDataExtension(NULL), m_ObjectReady(TRUE)
{
	
}

uint8_t RequestedAssistData::Copy(const RequestedAssistData& cpy)
{
	if (&cpy == this)
	{
		return RETURN_OK;
	}
	if (m_pNavigationModelData)
	{
		delete m_pNavigationModelData;
	}

	m_pNavigationModelData = NULL;

	m_AcquisitionAssistanceRequested = cpy.m_AcquisitionAssistanceRequested;
	m_AlmanacRequested = cpy.m_AlmanacRequested;
	m_DGPSCorrectionsRequested = cpy.m_DGPSCorrectionsRequested;
	m_ReferenceLocationRequested = cpy.m_ReferenceLocationRequested;
	m_ReferenceTimeRequested = cpy.m_ReferenceTimeRequested;
	m_UTCModeRequested = cpy.m_UTCModeRequested;
	m_RealTimeIntegrityRequested = cpy.m_RealTimeIntegrityRequested;
	m_IonosphericModeRequested =cpy.m_IonosphericModeRequested;
	m_NavigationModelRequested = cpy.m_NavigationModelRequested;
	
	if (cpy.m_pNavigationModelData != NULL)
	{
		m_pNavigationModelData = new(nothrow) NavigationModel();
		if(!m_pNavigationModelData)
		{
			return RETURN_FATAL;
		}
		(*m_pNavigationModelData).Copy(*cpy.m_pNavigationModelData);
	}	
	if(cpy.m_pVer2RequestedAssistDataExtension!=NULL)//rsuvorov extended ephemeris improvement
	{
		m_pVer2RequestedAssistDataExtension=new(nothrow) Ver2RequestedAssistDataExtension();
		if(!m_pVer2RequestedAssistDataExtension)
		{
			return RETURN_FATAL;
		}
		(*m_pVer2RequestedAssistDataExtension).Copy(*cpy.m_pVer2RequestedAssistDataExtension);
	}

	return RETURN_OK;
}

RequestedAssistData::~RequestedAssistData()
{
	if (m_pNavigationModelData)
	{
		delete m_pNavigationModelData;
	}
	if (m_pVer2RequestedAssistDataExtension) //rsuvorov extended ephemeris improvement
	{
		delete m_pVer2RequestedAssistDataExtension;
	}
}

NavigationModel::~NavigationModel()
{
	if (m_SatInfo != NULL)
	{
		m_SatInfo->clear();
		delete m_SatInfo;
	}
}

NavigationModel::NavigationModel():
	m_SatInfo(NULL), m_ObjectReady(TRUE)
{

}

uint8_t NavigationModel::Copy(const NavigationModel& cpy)
{
	if (&cpy == this)
	{
		return RETURN_OK;
	}
	if (m_SatInfo != NULL)
	{
		delete m_SatInfo;
	}
	
	m_SatInfo = NULL;

	if (cpy.m_SatInfo != NULL)
	{
		m_SatInfo = new(nothrow) android::Vector<SatelliteInfoElement>(*cpy.m_SatInfo);
		//m_SatInfo = new(nothrow) std::vector<SatelliteInfoElement>(*cpy.m_SatInfo);
		if(!m_SatInfo)
			return RETURN_FATAL;
	}

	m_GPSToe = cpy.m_GPSToe;
	m_GPSWeek = cpy.m_GPSWeek;
	m_ToeLimit = cpy.m_ToeLimit;
	m_NSAT = cpy.m_NSAT;
	
	return RETURN_OK;
}


}
