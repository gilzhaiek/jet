/**
 *  @file  LocationID.h
 *  @brief LocationID declaration.
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
Alexander V. Morozov         25.10.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#include "common/LocationID.h"
#include "utils/Log.h"

using namespace std;

namespace Engine {

// Const&Dest
GSMCellInformation::GSMCellInformation():
m_MCC(0), m_MNC(0), m_LAC(0), m_CI(0), m_aNMR(NULL), m_TA(NULL)
{

}

GSMCellInformation::~GSMCellInformation()
{
	if (m_aNMR != NULL)
	{
		m_aNMR->clear();
		delete m_aNMR;
	}
	if (m_TA != NULL)
	{
		delete m_TA;
	}
}

uint8_t GSMCellInformation::Copy(const GSMCellInformation& cpy)
{
	if (&cpy == this)
	{
		return RETURN_OK;
	}
	if (m_TA != NULL)
	{
		delete m_TA;
	}
	if (m_aNMR != NULL)
	{
		delete m_aNMR;
	}

	m_CI = 0;
	m_LAC = 0; 
	m_MCC = 0;
	m_MNC = 0;
	m_TA = NULL;
	m_aNMR = NULL;

	if (cpy.m_TA != NULL)
	{
		m_TA = new(nothrow) uint8_t(*cpy.m_TA);
		if(!m_TA)
			return RETURN_FATAL;
	}
	if (cpy.m_aNMR != NULL)
	{
		m_aNMR = new(nothrow) android::Vector<NMR>(*cpy.m_aNMR);
		//m_aNMR = new(nothrow) std::vector<NMR>(*cpy.m_aNMR);
		if(!m_aNMR)
			return RETURN_FATAL;
	}

	this->m_CI = cpy.m_CI;
	this->m_MNC = cpy.m_MNC;
	this->m_MCC = cpy.m_MCC;
	this->m_LAC = cpy.m_LAC;

	return RETURN_OK;
}

uint32_t GSMCellInformation::GetType()
{
	return GSM;
}

CDMACellInformation::CDMACellInformation()
{

}

CDMACellInformation::~CDMACellInformation()
{

}

uint32_t CDMACellInformation::GetType()
{
	return CDMA;
}

WCDMACellInformation::WCDMACellInformation():
m_MCC(0),m_MNC(0),m_CI(0),m_frequencyInfo(NULL),m_primaryScramblingCode(NULL),m_measuredResultsList(NULL)
{

}

WCDMACellInformation::~WCDMACellInformation()
{
	if(m_frequencyInfo != NULL)
		delete m_frequencyInfo;
	if(m_primaryScramblingCode != NULL)
		delete m_primaryScramblingCode;
	if(m_measuredResultsList != NULL)
		delete m_measuredResultsList;
}

uint8_t WCDMACellInformation::Copy(const WCDMACellInformation& cpy)
{
	if (&cpy == this)
	{
		return RETURN_OK;
	}
	if (m_frequencyInfo!= NULL)
	{
		delete m_frequencyInfo;
	}
	if (m_primaryScramblingCode != NULL)
	{
		delete m_primaryScramblingCode;
	}
	if (m_measuredResultsList != NULL)
	{
		delete m_measuredResultsList;
	}
	m_CI = 0;
	m_MCC = 0;
	m_MNC = 0;
	m_frequencyInfo=NULL;
	m_primaryScramblingCode=NULL;
	m_measuredResultsList=NULL;
	if (cpy.m_frequencyInfo != NULL)
	{
		m_frequencyInfo = new(nothrow) FrequencyInfo(*cpy.m_frequencyInfo);
		if(!m_frequencyInfo)
			return RETURN_FATAL;
	}
	if (cpy.m_primaryScramblingCode != NULL)
	{
		m_primaryScramblingCode = new(nothrow) long(*cpy.m_primaryScramblingCode);
		if(!m_primaryScramblingCode)
			return RETURN_FATAL;
	}
	if (cpy.m_measuredResultsList != NULL)
	{
		m_measuredResultsList = new(nothrow) MeasuredResultsList(*cpy.m_measuredResultsList);
		if(!m_measuredResultsList)
			return RETURN_FATAL;
	}
	this->m_CI = cpy.m_CI;
	this->m_MNC = cpy.m_MNC;
	this->m_MCC = cpy.m_MCC;
	LOGD("\nInside copy function of WCDMACellInformation\n");
	LOGD("\nm_CI = %ld\n",this->m_CI);
	LOGD("\nm_MNC = %ld\n",this->m_MNC);
	LOGD("\nm_MCC = %ld\n",this->m_MCC);
	return RETURN_OK;
}

uint32_t WCDMACellInformation::GetType()
{
	return WCDMA;
}

LocationID::LocationID() : m_ObjectReady(TRUE)
{
	m_pCellInformation = NULL;
	/* KW changes*/
	m_Status    = NO_CURRENT;
}

/**
 * @breif Constructor. Allocate memory for internal structs.
 * @param cellType - pointer to object of current cell. i.g. GSMCellInformation
 * @usage LocationID* id = new LocationID(new GSMCellInformation())
 */
LocationID::LocationID(CChoice* cellType):m_pCellInformation(cellType), m_ObjectReady(TRUE)
{
	m_pCellInformation = NULL;
	/* KW changes*/
	m_Status    = NO_CURRENT;
}

uint8_t LocationID::Copy(const LocationID& loc)
{
	LOGD("\nInside copy of LocationID:\n");
	if (this == &loc)
	{
		return RETURN_OK;
	}
	if (loc.m_pCellInformation == NULL)
	{
		return RETURN_FATAL;
	}
	if(m_pCellInformation)
	{
		delete m_pCellInformation;
		m_pCellInformation = NULL;
	}

	this->m_Status = loc.m_Status;

	switch (loc.m_pCellInformation->GetType())
	{
		case GSM:
			m_pCellInformation = new(nothrow) GSMCellInformation();
			if(!m_pCellInformation)
			{
				return RETURN_FATAL;
			}
			((GSMCellInformation*) m_pCellInformation)->Copy(*((GSMCellInformation*)loc.m_pCellInformation));
			break;
		case WCDMA:
			LOGD("\nEntering case WCDMA:\n");
			m_pCellInformation = new(nothrow) WCDMACellInformation();
			if(!m_pCellInformation)
			{
				return RETURN_FATAL;
			}
			((WCDMACellInformation*) m_pCellInformation)->Copy(*((WCDMACellInformation*)loc.m_pCellInformation));
			break;
		case CDMA:
			m_pCellInformation = new(nothrow) CDMACellInformation();
			if(!m_pCellInformation)
			{
				return RETURN_FATAL;
			}
			((CDMACellInformation*) m_pCellInformation)->Copy(*((CDMACellInformation*)loc.m_pCellInformation));
			break;
		default:
				return RETURN_FATAL;;
	}	
	return RETURN_OK;
}

LocationID::~LocationID()
{
	if (m_pCellInformation != NULL)
	{
		delete m_pCellInformation;
	}
}

} // end of namespace Engine
