/**
*  @file  MSG.h
*  @brief MSG declaration.
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
Alexander V. Morozov         14.11.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#include "common/MSG.h"

namespace Engine {


MSG::MSG():
	m_SETSessionID(0),
	m_pIncomingRawData(NULL),
	m_IncomingRawDataSize(0),
	m_pOutgoingRawData(NULL),
	m_OutgoingRawDataSize(0),
	m_pIncomingMessage(NULL),
	m_pOutgoingMessage(NULL)
{
	m_GPSData.m_pdu = NULL;
	m_GPSData.m_pdu_size = 0;
}

MSG::~MSG()
{
	if (m_pIncomingRawData)
	{
		delete[] m_pIncomingRawData;
	}

	m_pOutgoingRawData = NULL; // pointer to a memory which controlled by ULP_Processor

	if (m_pIncomingMessage)
	{
		delete m_pIncomingMessage;
	}
	if (m_pOutgoingMessage)
	{
		delete m_pOutgoingMessage;
	}
	if (m_GPSData.m_pdu)
	{
		delete[] m_GPSData.m_pdu;
	}
}


}
