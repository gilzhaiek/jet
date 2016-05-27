/**
 *  @file  SUPLStart.h
 *  @brief CSUPLStart declaration.
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
Alexander V. Morozov         29.10.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#include "common/QoP.h"

using namespace std;

namespace Engine {

QoP::QoP() : m_pVeracc(NULL), m_pMaxLocAge(NULL), m_pDelay(NULL), m_ObjectReady(TRUE)
{
	/* KW changes*/
	m_Horacc = NULL;
}


QoP::~QoP()
{
	if (m_pDelay != NULL)
	{
		delete m_pDelay;
	}

	if (m_pMaxLocAge != NULL)
	{
		delete m_pMaxLocAge;
	}

	if (m_pVeracc != NULL)
	{
		delete m_pVeracc;
	}
}

uint8_t QoP::Copy(const QoP& cpy)
{
	if (this == &cpy)
	{
		return RETURN_OK;
	}

	if (m_pDelay) 
	{
		delete m_pDelay;
	}
	if (m_pMaxLocAge)
	{
		delete m_pMaxLocAge;
	}
	if (m_pVeracc)
	{
		delete m_pVeracc;
	}

	m_pDelay = NULL;
	m_pMaxLocAge = NULL;
	m_pVeracc = NULL;

	m_Horacc = cpy.m_Horacc;
	if (cpy.m_pDelay != NULL)
	{
		m_pDelay = new(nothrow) uint8_t;
		if(!m_pDelay)
			return RETURN_FATAL;
		*m_pDelay = *cpy.m_pDelay;
	}
	if (cpy.m_pMaxLocAge != NULL)
	{
		m_pMaxLocAge = new(nothrow) uint32_t;
		if(!m_pMaxLocAge)
			return RETURN_FATAL;
		*m_pMaxLocAge = *cpy.m_pMaxLocAge;
	}
	if (cpy.m_pVeracc != NULL)
	{
		
		m_pVeracc = new(nothrow) uint8_t;
		if(!m_pVeracc)
			return RETURN_FATAL;
		*m_pVeracc = *cpy.m_pVeracc;
	}
	return RETURN_OK;
}

} // end of namespace Engine
