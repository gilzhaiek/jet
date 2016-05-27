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

#ifndef _SET_CAPABILITIES_H_
#define _SET_CAPABILITIES_H_

/********************************************************************************
 *	Include section
 ********************************************************************************/
#include "common/types.h"

namespace Engine {

/********************************************************************************
 * Enum section
 ********************************************************************************/
enum PrefMethod
{
	AGPS_SET_ASSISTED_PREF	= 0,
	AGPS_SET_BASED_PREF		= 1,
	NO_PREF					= 2
};

/********************************************************************************
 * Struct section
 ********************************************************************************/

/**
 *	@breif Position technologies supported by SET.
 *
 */
struct PosTechnology
{
	bool_t	m_AGPSSETAssisted;
	bool_t	m_AGPSSETBased;
	bool_t	m_AutonomusGPS;
	bool_t	m_aFLT;
	bool_t	m_eCID;
	bool_t	m_eOTD;
	bool_t	m_oTDOA;
};

/**
 *	@breif Position protocol supported by GPS
 *
 */
struct PosProtocol
{
	bool_t	m_TIA801;
	bool_t	m_RRLP;
	bool_t	m_RRC;
};

/**
 *	@breif SET capabilities
 *
 */
struct SETCapabilities
{
	PrefMethod		m_PrefMethod;
	PosTechnology	m_PosTechnology;
	PosProtocol		m_PosProtocol;
	// Constructors
	SETCapabilities();
};

} // namespace Engine

#endif //_SET_CAPABILITIES_H_
