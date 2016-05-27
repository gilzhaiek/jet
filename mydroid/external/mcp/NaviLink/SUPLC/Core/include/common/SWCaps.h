/*================================================================================================*/
/**
 *  @file  SUPLGlobal.cpp
 *
 *  @brief CSUPLGlobal implementation
 */

/*
===================================================================================================

Copyright (c) 2007 Teleca Inc. All rights reserved.
Redistribution and modifications are permitted subject to BSD license. 
     
===================================================================================================
Revision History:
                            Modification     Tracking
Author                          Date          Number    Description of Changes
-------------------------   ------------    ----------  -------------------------------------------
Alexander V. Morozov        29.10.2007	                 initial version
===================================================================================================
Portability:  MSVC compiler
===================================================================================================
*/

#ifndef _SW_CAPS_H_
#define _SW_CAPS_H_

/**************************************************************************************************
 * Include section
 **************************************************************************************************/
#include "common/SETCapabilities.h"

namespace Engine {

/********************************************************************************
 * Enum section
 ********************************************************************************/
enum SupportedMode
{
	PROXY_MODE = 0,
	NON_PROXY_MODE = 1
};

/********************************************************************************
 * Struct section
 ********************************************************************************/

/**
 *	@breif Protocol version.
 *
 */
struct Version
{
	uint8_t m_Major;
	uint8_t m_Minor;
	uint8_t m_Servind;
};

/**
 *	@breif Supported mode and protocol by this application
 *
 */
struct SoftwareCapabilities
{
	SupportedMode	m_SuppoteredMode;
	Version			m_Version;
};

} // namespace Engine

#endif // _SW_CAPS_H_
