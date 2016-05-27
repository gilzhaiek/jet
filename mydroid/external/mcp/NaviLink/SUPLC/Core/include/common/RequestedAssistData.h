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
Alexander V. Morozov         01.11.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#ifndef _REQUESTED_ASSIST_DATA_H_
#define _REQUESTED_ASSIST_DATA_H_

#include "types.h"
#include "ExtendedEphemeris.h"
//#include <vector>

#include "utils/Vector.h"

#define SITELLITE_INFO_LEN 30

namespace Engine {


class NavigationModel;
struct SatelliteInfo;
struct SatelliteInfoElement;

class RequestedAssistData
{
public:
	bool_t				m_AlmanacRequested;
	bool_t				m_UTCModeRequested;
	bool_t				m_IonosphericModeRequested;
	bool_t				m_DGPSCorrectionsRequested;
	bool_t				m_ReferenceLocationRequested;
	bool_t				m_ReferenceTimeRequested;
	bool_t				m_AcquisitionAssistanceRequested;
	bool_t				m_RealTimeIntegrityRequested;
	bool_t				m_NavigationModelRequested;
	//  optional
	NavigationModel*	m_pNavigationModelData;
	// extension 
	Ver2RequestedAssistDataExtension *m_pVer2RequestedAssistDataExtension; //rsuvorov extended ephemeris improvement

public:
	uint8_t Copy(const RequestedAssistData&);
	bool_t IsObjectReady() { return m_ObjectReady;} 
	// const&destr
	RequestedAssistData();
    ~RequestedAssistData();
private:
	bool_t m_ObjectReady;
private:
	RequestedAssistData& operator = (const RequestedAssistData&){return *this;}
	RequestedAssistData(const RequestedAssistData&){}
};

class NavigationModel
{
public:
	uint32_t							m_GPSWeek;
	uint8_t								m_GPSToe;
	uint8_t								m_NSAT;
	uint8_t								m_ToeLimit;
	android::Vector<SatelliteInfoElement>*	m_SatInfo;
	//std::vector<SatelliteInfoElement>*	m_SatInfo;
public:
	uint8_t Copy(const NavigationModel&);
	bool_t IsObjectReady() { return m_ObjectReady;} 
	// const&dest
	NavigationModel();
	~NavigationModel();
private:
	bool_t m_ObjectReady;
private:
	NavigationModel(const NavigationModel&) {}
	NavigationModel& operator = (const NavigationModel&) {return *this;}
};

struct SatelliteInfoElement
{
	uint8_t	m_SatID;
	uint8_t	m_IODE;
};

} // end of namespace Engine

#endif // _REQUESTED_ASSIST_DATA_H_
