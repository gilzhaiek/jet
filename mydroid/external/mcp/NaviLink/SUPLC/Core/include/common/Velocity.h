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

#ifndef _VELOCITY_H_
#define _VELOCITY_H_

#include "types.h"

namespace Engine {

#define BEARING_BIT_SZ			9
#define HOR_SPEED_BIT_SZ		16
#define VER_DIRECT_BIT_SZ		1
#define VER_SPEED_BIT_SZ		8
#define UNCERT_SPEED_BIT_SZ		8
#define HOR_UNCERT_SPEED_BIT_SZ	8
#define VER_UNCERT_SPEED_BIT_SZ	8


enum Velocity
{
	HOR_VEL,
	HOR_AND_VER_VEL,
	HOR_VEL_UNCERT,
	HOR_AND_VER_UNCERT
};

//Velocity type definition

class CHorvel: public CChoice
{
public:
	CBitString	m_Bearing;
	CBitString	m_HorSpeed;

public:
					CHorvel();
virtual				~CHorvel();
virtual uint32_t	GetType();
uint8_t				Copy(const CHorvel& horvel) ;
};

class CHorandvervel: public CChoice
{
public:
	CBitString	m_VerDirect;
	CBitString	m_Bearing;
	CBitString	m_HorSpeed;
	CBitString	m_VerSpeed;

public:
		CHorandvervel();
virtual ~CHorandvervel();
virtual uint32_t	GetType();
uint8_t				Copy(const CHorandvervel& horandvervel) ;
};

class CHorveluncert: public CChoice
{
public:
	CBitString	m_Bearing;
	CBitString	m_HorSpeed;
	CBitString	m_UncertSpeed;

public:
		CHorveluncert();
virtual ~CHorveluncert();
virtual uint32_t	GetType();
uint8_t				Copy(const CHorveluncert& horveluncert) ;
};

class CHorandveruncert: public CChoice
{
public:
	CBitString	m_VerDirect;
	CBitString	m_Bearing;
	CBitString	m_HorSpeed;
	CBitString	m_VerSpeed;
	CBitString	m_HorUncertSpeed;
	CBitString	m_VerUncertSpeed;

public:
		CHorandveruncert();
virtual ~CHorandveruncert();
virtual uint32_t	GetType();
uint8_t				Copy(const CHorandveruncert& horandveruncert) ;
};

} // end of namespace Engine

#endif // _VELOCITY_H_
