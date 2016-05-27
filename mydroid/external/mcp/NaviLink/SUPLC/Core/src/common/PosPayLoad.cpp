/**
*  @file  SUPLPos.h
*  @brief CSUPLPos declaration.
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

#include "common/PosPayLoad.h"

namespace Engine {

CTIA801PayLoad::CTIA801PayLoad():m_Tia801PayLoad(TI801_PAY_LOAD_LEN)
{
	if(!m_Tia801PayLoad.IsObjectReady())
		m_ObjectReady = FALSE;
}

CTIA801PayLoad::~CTIA801PayLoad()
{

}

uint32_t CTIA801PayLoad::GetType() 
{
	return TI801_PAY_LOAD;
}



CRRCPayLoad::CRRCPayLoad():m_RRCPayLoad(RRC_PAY_LOAD_LEN)
{
	if(!m_RRCPayLoad.IsObjectReady())
		m_ObjectReady = FALSE;
}

CRRCPayLoad::~CRRCPayLoad()
{

}

CRRCPayLoad::CRRCPayLoad(uint8_t* data, uint32_t size):m_RRCPayLoad(data, size)      
{
        if(!m_RRCPayLoad.IsObjectReady())
                m_ObjectReady = FALSE;
}

uint32_t CRRCPayLoad::GetType() 
{
	return RRC_PAY_LOAD;
}

CRRLPPayLoad::CRRLPPayLoad():m_RRLPPayLoad(RRLP_PAY_LOAD_LEN)
{
	if(!m_RRLPPayLoad.IsObjectReady())
		m_ObjectReady = FALSE;
}

CRRLPPayLoad::CRRLPPayLoad(uint8_t* data, uint32_t size):m_RRLPPayLoad(data, size)	
{
	if(!m_RRLPPayLoad.IsObjectReady())
		m_ObjectReady = FALSE;
}

CRRLPPayLoad::~CRRLPPayLoad()
{

}

uint32_t CRRLPPayLoad::GetType() 
{
	return RRLP_PAY_LOAD;
}


} // end of namespace Engine
