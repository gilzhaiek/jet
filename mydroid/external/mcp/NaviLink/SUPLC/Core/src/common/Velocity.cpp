/**
*  @file  Velocity.h
*  @brief Velocity declaration.
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

#include "common/Velocity.h"

namespace Engine {

CHorvel::CHorvel():
	m_Bearing(BEARING_BIT_SZ),
	m_HorSpeed(HOR_SPEED_BIT_SZ)
{
	if(!m_Bearing.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
	if(!m_HorSpeed.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
}

CHorvel::~CHorvel()
{

}

uint8_t CHorvel::Copy(const CHorvel& horvel)
{
	if(m_Bearing.Copy(horvel.m_Bearing) != RETURN_OK)
		return RETURN_FATAL;
	if(m_HorSpeed.Copy(horvel.m_HorSpeed) != RETURN_OK)
		return RETURN_FATAL;
	
	return RETURN_OK;
}

uint32_t	CHorvel::GetType()
{
	return HOR_VEL;
}

CHorandvervel::CHorandvervel():
	m_VerDirect(VER_DIRECT_BIT_SZ),
	m_Bearing(BEARING_BIT_SZ),
	m_HorSpeed(HOR_SPEED_BIT_SZ),
	m_VerSpeed(VER_SPEED_BIT_SZ)
{
	if(!m_VerDirect.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
	if(!m_Bearing.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
	if(!m_HorSpeed.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
	if(!m_VerSpeed.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
}

CHorandvervel::~CHorandvervel()
{

}

uint8_t CHorandvervel::Copy(const CHorandvervel& horandvervel)
{
	if(m_Bearing.Copy(horandvervel.m_Bearing) != RETURN_OK)
		return RETURN_FATAL;
	if(m_HorSpeed.Copy(horandvervel.m_HorSpeed) != RETURN_OK)
		return RETURN_FATAL;
	if(m_VerDirect.Copy(horandvervel.m_VerDirect) != RETURN_OK)
		return RETURN_FATAL;
	if(m_VerSpeed.Copy(horandvervel.m_VerSpeed) != RETURN_OK)
		return RETURN_FATAL;
	
	return RETURN_OK;
}

uint32_t CHorandvervel::GetType()
{
	return HOR_AND_VER_VEL;
}


CHorveluncert::CHorveluncert():
	m_Bearing(BEARING_BIT_SZ),
	m_HorSpeed(HOR_SPEED_BIT_SZ),
	m_UncertSpeed(UNCERT_SPEED_BIT_SZ)
{
	if(!m_Bearing.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
	if(!m_HorSpeed.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
	if(!m_UncertSpeed.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
}

CHorveluncert::~CHorveluncert()
{

}

uint8_t CHorveluncert::Copy(const CHorveluncert& horveluncert)
{
	if(m_Bearing.Copy(horveluncert.m_Bearing) != RETURN_OK)
		return RETURN_FATAL;
	if(m_HorSpeed.Copy(horveluncert.m_HorSpeed) != RETURN_OK)
		return RETURN_FATAL;
	if(m_UncertSpeed.Copy(horveluncert.m_UncertSpeed) != RETURN_OK)
		return RETURN_FATAL;

	return RETURN_OK;
}

uint32_t CHorveluncert::GetType()
{
	return HOR_VEL_UNCERT;
}


CHorandveruncert::CHorandveruncert():
	m_VerDirect(VER_DIRECT_BIT_SZ),
	m_Bearing(BEARING_BIT_SZ),
	m_HorSpeed(HOR_SPEED_BIT_SZ),
	m_VerSpeed(VER_SPEED_BIT_SZ),
	m_HorUncertSpeed(HOR_UNCERT_SPEED_BIT_SZ),
	m_VerUncertSpeed(VER_UNCERT_SPEED_BIT_SZ)
{
	if(!m_Bearing.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
	if(!m_HorSpeed.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
	if(!m_VerDirect.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
	if(!m_VerSpeed.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
	if(!m_HorUncertSpeed.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
	if(!m_VerUncertSpeed.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
}

CHorandveruncert::~CHorandveruncert()
{

}

uint8_t CHorandveruncert::Copy(const CHorandveruncert& horandveruncert)
{
	if(m_Bearing.Copy(horandveruncert.m_Bearing) != RETURN_OK)
		return RETURN_FATAL;
	if(m_HorSpeed.Copy(horandveruncert.m_HorSpeed) != RETURN_OK)
		return RETURN_FATAL;
	if(m_HorUncertSpeed.Copy(horandveruncert.m_HorUncertSpeed) != RETURN_OK)
		return RETURN_FATAL;
	if(m_VerDirect.Copy(horandveruncert.m_VerDirect) != RETURN_OK)
		return RETURN_FATAL;
	if(m_VerSpeed.Copy(horandveruncert.m_VerSpeed) != RETURN_OK)
		return RETURN_FATAL;
	if(m_VerUncertSpeed.Copy(horandveruncert.m_VerUncertSpeed) != RETURN_OK)
		return RETURN_FATAL;

	return RETURN_OK;
}

uint32_t CHorandveruncert::GetType()
{
	return HOR_AND_VER_UNCERT;
}


} // end of namespace Engine
