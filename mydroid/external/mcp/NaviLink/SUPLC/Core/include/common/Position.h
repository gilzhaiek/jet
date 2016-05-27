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
Alexander V. Morozov         01.11.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#ifndef _POSITION_H_
#define _POSITION_H_

#include "types.h"
#include "Velocity.h"

namespace Engine {


enum LatitudeSign 
{
	NORTH,
	SOUTH
};

enum AltitudeDirection
{
	HEIGHT,
	DEPTH
};

struct Uncertainty
{
	uint8_t	m_UncertaintySemiMajor;
	uint8_t	m_UncertaintySeminMinor;
	uint8_t	m_OrientationMajorAxis;
};

struct AltitudeInfo
{
	AltitudeDirection	m_AltitudeDirection;
	uint32_t				m_Altitude;
	uint32_t				m_AltUncertainty;	
};

class PositionEstimate
{
public:
	LatitudeSign	m_LatitudeSign;
	uint32_t		m_Latitude;
	int32_t			m_Longitude;
	Uncertainty*	m_pUncertainty;
	uint8_t*		m_pConfidence;
	AltitudeInfo*	m_pAltitudeInfo;
public:
	uint8_t Copy(const PositionEstimate&);
	bool_t IsObjectReady() { return m_ObjectReady;} 
	//Constructors
	PositionEstimate();
	~PositionEstimate();
private:
	bool_t m_ObjectReady;
private:
	PositionEstimate& operator = (const PositionEstimate&){return *this;}
	PositionEstimate(const PositionEstimate&){}
};

class Position
{
public:
	COctetString		m_UTCTime;
	PositionEstimate	m_PositionEstimate;
	// choice from Horvel, Horandvervel, Horveluncert, Horandveruncert
	CChoice*			m_pVelocity;
public:
	uint8_t Copy(const Position&);
	bool_t IsObjectReady() { return m_ObjectReady;} 
	// Constructors
	Position();
	~Position();
private:
	bool_t m_ObjectReady;
private:
	Position& operator = (const Position&);
	Position(const Position&);
};



} // end of namespace Engine

#endif // _POSITION_H_
