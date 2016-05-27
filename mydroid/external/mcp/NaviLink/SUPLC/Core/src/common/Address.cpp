/**
*  @file  Address.h
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

#include "common/Address.h"

using namespace std;

namespace Engine {

CIPv4::CIPv4(): m_Addr(IPv4_LEN)
{
	if(!m_Addr.IsObjectReady())
		m_ObjectReady = FALSE;
}

CIPv4::CIPv4(COctetString *aData): m_Addr(IPv4_LEN)
{
	if(!m_Addr.IsObjectReady())
		m_ObjectReady = FALSE;

	if(aData == NULL) 
	{
		m_Addr.Clean();
	}
	else 
	{
		uint8_t ret = m_Addr.Copy(*aData);
		if(ret != RETURN_OK)
			m_ObjectReady = FALSE;
	}
}


uint8_t CIPv4::Copy(const CIPv4& cpy)
{
	if (&cpy == this)
	{
		return RETURN_OK;
	}

	return (m_Addr.Copy(cpy.m_Addr));
}

CIPv4::~CIPv4()
{

}



uint32_t CIPv4::GetType() 
{
	return IPv4;
}

CIPv6::CIPv6(): m_Addr(IPv6_LEN)
{

}

CIPv6::CIPv6(COctetString *aData) : m_Addr(IPv6_LEN)
{
	if(!m_Addr.IsObjectReady())
		m_ObjectReady = FALSE;

	if(aData == NULL) 
	{
		m_Addr.Clean();
	}
	else
	{
		uint8_t ret = m_Addr.Copy(*aData);
		if(ret != RETURN_OK)
			m_ObjectReady = FALSE;
	}
}


uint8_t CIPv6::Copy(const CIPv6& cpy)
{	
	if (&cpy == this)
	{
		return RETURN_OK;
	}

	return (m_Addr.Copy(cpy.m_Addr));
}

CIPv6::~CIPv6()
{

}

uint32_t CIPv6::GetType()
{
	return IPv6;
}

CFQDN::CFQDN():m_FQDN(FQDN_STRING_SIZE)
{
	if(!m_FQDN.IsObjectReady())
		m_ObjectReady = FALSE;
}

CFQDN::CFQDN(COctetString *aFQDN):m_FQDN(FQDN_STRING_SIZE)
{
	if(!m_FQDN.IsObjectReady())
		m_ObjectReady = FALSE;

	if(aFQDN == NULL) 
	{
		m_FQDN.Clean();
	}
	else 
	{
		uint8_t ret = m_FQDN.Copy(*aFQDN);
		if(ret != RETURN_OK)
			m_ObjectReady = FALSE;
	}
}

CFQDN::~CFQDN()
{

}

uint32_t	CFQDN::GetType()
{
	return FQDN;
}


uint8_t CFQDN::Copy(const CFQDN& cpy)
{
	if (&cpy == this)
	{
		return RETURN_OK;
	}
	
	return (m_FQDN.Copy(cpy.m_FQDN));
}

uint32_t CIPAddress::GetType()
{
	return IP_ADDRESS;
}

CIPAddress::CIPAddress()
{
	iIP_Addr = NULL;
};

CIPAddress::CIPAddress(IP_TYPE aType)
{
	Init(aType, NULL);
}

CIPAddress::CIPAddress(IP_TYPE aType,COctetString *aData) 
{
	Init(aType, aData);
}

CIPAddress::CIPAddress(const CChoice* cpy) : iIP_Addr(NULL)
{
	CIPAddress* ip = (CIPAddress*) cpy;
	switch (ip->iIP_Addr->GetType())
	{
	case IPv4:
		{
			iIP_Addr = new(nothrow) CIPv4;
			if(!iIP_Addr)
			{
				m_ObjectReady = FALSE;
				return;
			}
			if(!((CIPv4*)iIP_Addr)->Copy(*((CIPv4*) ip->iIP_Addr)))
				m_ObjectReady = FALSE;
			break;
		}
	case IPv6:
		{
			iIP_Addr = new(nothrow) CIPv6;
			if(!iIP_Addr)
			{
				m_ObjectReady = FALSE;
				return;
			}
			if(!((CIPv6*)iIP_Addr)->Copy(*((CIPv6*) ip->iIP_Addr)))
				m_ObjectReady = FALSE;
			break;
		}
	}
}

void CIPAddress::Init(IP_TYPE aType, COctetString *aData)
{
	switch(aType)
	{
	case IPv4:
		iIP_Addr = new(nothrow) CIPv4(aData);
		if(!iIP_Addr)
			m_ObjectReady = FALSE;
		break;
	case IPv6:
		iIP_Addr = new(nothrow) CIPv6(aData);
		if(!iIP_Addr)
			m_ObjectReady = FALSE;
		break;
	default:
		iIP_Addr = NULL;
		if(!iIP_Addr)
			m_ObjectReady = FALSE;
		break;
	}
}

CIPAddress::~CIPAddress()
{
	if(iIP_Addr != NULL)
	{
		delete iIP_Addr;
	}
}

} // end of namespace Engine
