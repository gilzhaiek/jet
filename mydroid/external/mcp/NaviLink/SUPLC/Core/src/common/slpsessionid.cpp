/**
*  @file  slpsessionid.cpp
*  @brief CSLPSessionID declaration.
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
Roman Suvorov                2.11.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#include "common/slpsessionid.h"

using namespace std;

namespace Engine{

//part-time constructor
CSLPSessionID::CSLPSessionID(ADDRESS_TYPE aType):iSessionId(SESSION_ID_SIZE),m_ObjectReady(TRUE)
{
	if(!iSessionId.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
	Init(aType,IP_NOTHING,0);
}
//FQDN address constructor
CSLPSessionID::CSLPSessionID(ADDRESS_TYPE aType, COctetString *aData):iSessionId(SESSION_ID_SIZE), iSLPaddr(NULL),m_ObjectReady(TRUE)
{
	if(!iSessionId.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
	Init(aType,IP_NOTHING,aData);
}
//IP address constructor
CSLPSessionID::CSLPSessionID(IP_TYPE aType, COctetString *aData):iSessionId(SESSION_ID_SIZE),m_ObjectReady(TRUE)
{
	if(!iSessionId.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
	Init(IP_ADDRESS,aType,aData);
}


void CSLPSessionID::Init(ADDRESS_TYPE aAddrType,IP_TYPE aIPType, COctetString *aData)
{
	switch(aAddrType)
	{
	case IP_ADDRESS:
		iSLPaddr=new(nothrow) CIPAddress(aIPType,aData);
		if(!iSLPaddr)
			m_ObjectReady = FALSE;
		break;
	case FQDN:
		iSLPaddr=new(nothrow) CFQDN(aData);
		if(!iSLPaddr)
			m_ObjectReady = FALSE;
		break;
	default:
		m_ObjectReady = FALSE;
		break;
	};
}

CSLPSessionID::CSLPSessionID():
	iSessionId(SESSION_ID_SIZE),
	iSLPaddr(NULL),
	m_ObjectReady(TRUE)
{
	if(!iSessionId.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
}

CSLPSessionID::CSLPSessionID(CChoice *aSLPaddr):
	iSessionId(SESSION_ID_SIZE),
	iSLPaddr(aSLPaddr),
	m_ObjectReady(TRUE)
{
	if(!iSessionId.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
}

CSLPSessionID::~CSLPSessionID()
{
	if(iSLPaddr != NULL) 
	{
		delete iSLPaddr;
	}
}


uint8_t CSLPSessionID::Copy(const CSLPSessionID& cpy)
{
	if (&cpy == this)
	{
		return RETURN_OK;
	}
	if (cpy.iSLPaddr == NULL)
	{
		return RETURN_OK;
	}
	if(iSLPaddr != NULL) 
	{
		delete iSLPaddr;
	}

    iSLPaddr = NULL;
	//iSessionId = SESSION_ID_SIZE;
	
	if(iSessionId.Copy(cpy.iSessionId) != RETURN_OK)
		return RETURN_FATAL;

	switch (cpy.iSLPaddr->GetType())
	{
		case FQDN:
			{
				iSLPaddr = new(nothrow) CFQDN();
				if(!iSLPaddr)
					return RETURN_FATAL;
				if(((CFQDN*)iSLPaddr)->Copy(*((CFQDN*) cpy.iSLPaddr)) != RETURN_OK)
					return RETURN_FATAL;
				break;
			}
		case IP_ADDRESS:
			{
				CIPAddress* ip_addr = (CIPAddress*)cpy.iSLPaddr;
				iSLPaddr = new(nothrow) CIPAddress;
				if(!iSLPaddr)
					return RETURN_FATAL;
				switch (ip_addr->iIP_Addr->GetType())
				{
					case IPv4:
						{
							((CIPAddress*)iSLPaddr)->iIP_Addr = new(nothrow) CIPv4();
							CIPv4* ip_v4 = (CIPv4*)(((CIPAddress*)iSLPaddr)->iIP_Addr);
							if(!((CIPAddress*)iSLPaddr)->iIP_Addr)
								return RETURN_FATAL;
							if(ip_v4->Copy(*(((CIPv4*) ip_addr->iIP_Addr))) != RETURN_OK)
								return RETURN_FATAL;
							break;
						}
					case IPv6:
						{
							((CIPAddress*)iSLPaddr)->iIP_Addr = new(nothrow) CIPv6();
							CIPv6* ip_v6 = (CIPv6*)(((CIPAddress*)iSLPaddr)->iIP_Addr);
							if(!((CIPAddress*)iSLPaddr)->iIP_Addr)
								return RETURN_FATAL;
							if(ip_v6->Copy(*(((CIPv6*) ip_addr->iIP_Addr))) != RETURN_OK)
								return RETURN_FATAL;
							break;
						}
					default:
						return RETURN_FATAL;
				}
				break;
			}
		default: return RETURN_FATAL;
	}
	return RETURN_OK;
}

} // end of namespace Engine
