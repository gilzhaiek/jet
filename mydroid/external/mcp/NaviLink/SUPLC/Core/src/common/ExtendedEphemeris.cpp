/**
*  @file  RequestedAssistData.h
*  @brief RequestedAssistData declaration.
*/

#include "common/ExtendedEphemeris.h"

using namespace std;

namespace Engine {

Ver2RequestedAssistDataExtension::Ver2RequestedAssistDataExtension():
	m_pGanssRequestedCommonAssistanceDataList(NULL),
	m_pGanssRequestedGenericAssistanceDataList(NULL),
	m_pExtendedEphemeris(NULL),
	m_pExtendedEphemerisCheck(NULL),
	m_ObjectReady(TRUE)
{
	
}

uint8_t Ver2RequestedAssistDataExtension::Copy(const Ver2RequestedAssistDataExtension& cpy)
{
	if (&cpy == this)
	{
		return RETURN_OK;
	}

	if (cpy.m_pExtendedEphemeris != NULL)
	{
		m_pExtendedEphemeris = new(nothrow) CExtendedEphemeris();
		if(!m_pExtendedEphemeris)
		{
			return RETURN_FATAL;
		}
		m_pExtendedEphemeris->validity=cpy.m_pExtendedEphemeris->validity;
	}

	if (cpy.m_pExtendedEphemerisCheck != NULL)
	{
		m_pExtendedEphemerisCheck = new(nothrow) CExtendedEphCheck();
		if(!m_pExtendedEphemerisCheck)
		{
			return RETURN_FATAL;
		}
		m_pExtendedEphemerisCheck->m_pBeginTime=cpy.m_pExtendedEphemerisCheck->m_pBeginTime;
		m_pExtendedEphemerisCheck->m_pEndTime=cpy.m_pExtendedEphemerisCheck->m_pEndTime;
	}
	return RETURN_OK;
}

Ver2RequestedAssistDataExtension::~Ver2RequestedAssistDataExtension()
{
	if(m_pGanssRequestedCommonAssistanceDataList) 
	{
		delete m_pGanssRequestedCommonAssistanceDataList;
	}

	if(m_pGanssRequestedGenericAssistanceDataList)
	{
		delete m_pGanssRequestedGenericAssistanceDataList;
	}

	if (m_pExtendedEphemeris)
	{
		delete m_pExtendedEphemeris;
	}

	if (m_pExtendedEphemerisCheck)
	{
		delete m_pExtendedEphemerisCheck;
	}
}



}//namespace Engine 
