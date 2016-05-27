/*================================================================================================*/
/**
@file   sessionid.h

@brief CSessionParams implementation (declaration)
*/
/*==================================================================================================

Copyright (c) 2007 Teleca Inc. All rights reserved.
Redistribution and modifications are permitted subject to BSD license. 

====================================================================================================
Revision History:
Modification								 Tracking
Author                          Date          Number    Description of Changes
-------------------------   ------------    ----------  -------------------------------------------
Roman Suvorov                30.10.2007	                 initial version
====================================================================================================
Portability:  MVSC  compiler
==================================================================================================*/

#include "common/sessionid.h"

using namespace std;

namespace Engine {

CSessionID::CSessionID():
	iSETSessionID(NULL),
	iSLPSessionID(NULL),
	m_ObjectReady(TRUE)
{

}

CSessionID::CSessionID(CSETSessionID *aSETSessionID):
	iSETSessionID(aSETSessionID),
	iSLPSessionID(NULL),
	m_ObjectReady(TRUE)
{

}

CSessionID::CSessionID(CSLPSessionID *aSLPSessionID):
	iSETSessionID(NULL),
	iSLPSessionID(aSLPSessionID),
	m_ObjectReady(TRUE)
{

}

CSessionID::~CSessionID()
{
	if(iSLPSessionID != NULL) 
	{
		delete iSLPSessionID;
	}
	if(iSETSessionID != NULL) 
	{
		delete iSETSessionID;
	}
	
}

uint8_t CSessionID::Copy(const CSessionID& cpy)
{
	if (&cpy == this)
	{
		return RETURN_OK;
	}

	if(iSETSessionID != NULL) 
	{
		delete iSETSessionID;
	}

	if(iSLPSessionID != NULL) 
	{
		delete iSLPSessionID;
	}

	iSETSessionID = NULL;
	iSLPSessionID = NULL;

	if (cpy.iSETSessionID != NULL)
	{
		iSETSessionID = new(nothrow) CSETSessionID();
		if(!iSETSessionID)
			return RETURN_FATAL;
		if((*iSETSessionID).Copy(*cpy.iSETSessionID) != RETURN_OK)
			return RETURN_FATAL;
	}

	if (cpy.iSLPSessionID != NULL)
	{
		iSLPSessionID = new(nothrow) CSLPSessionID();
		if(!iSLPSessionID)
			return RETURN_FATAL;
		if((*iSLPSessionID).Copy(*cpy.iSLPSessionID) != RETURN_OK)
			return RETURN_FATAL;
	}

	return RETURN_OK;
}

bool_t CSessionID::IsEqual(const CSessionID& cpy)
{
	if(this->iSETSessionID->sessionId == cpy.iSETSessionID->sessionId)
	{
		uint32_t this_type = this->iSETSessionID->iSetID->GetType();
		uint32_t cpy_type = cpy.iSETSessionID->iSetID->GetType();

		if(this_type == IMSI && cpy_type == IMSI)
		{
			IMSIChoice* this_imsi = (IMSIChoice*)this->iSETSessionID->iSetID;
			IMSIChoice* cpy_imsi = (IMSIChoice*)cpy.iSETSessionID->iSetID;
			if(memcmp(this_imsi->iIMSI.m_Data,cpy_imsi->iIMSI.m_Data,this_imsi->iIMSI.m_Size) != 0)
			{
				return FALSE;
			}
		}
		else if(this_type == MSISDN && cpy_type == MSISDN)
		{
			MSDNChoice* this_imsi = (MSDNChoice*)this->iSETSessionID->iSetID;
			MSDNChoice* cpy_imsi = (MSDNChoice*)cpy.iSETSessionID->iSetID;
			if(memcmp(this_imsi->iMSISDN.m_Data,cpy_imsi->iMSISDN.m_Data,this_imsi->iMSISDN.m_Size) != 0)
			{
				return FALSE;
			}
		}
		else
			return FALSE;
	}
	else
		return FALSE;

	if(this->iSLPSessionID)
	{
		if(memcmp(this->iSLPSessionID->iSessionId.m_Data,cpy.iSLPSessionID->iSessionId.m_Data,this->iSLPSessionID->iSessionId.m_Size)==0)
		{
			uint32_t this_type = this->iSLPSessionID->iSLPaddr->GetType();
			uint32_t cpy_type = cpy.iSLPSessionID->iSLPaddr->GetType();
			if(this_type == cpy_type)
			{
				switch(this_type)
				{
					case IP_ADDRESS:
						{
							CIPAddress* this_address = (CIPAddress*)this->iSLPSessionID->iSLPaddr;
							CIPAddress* cpy_address = (CIPAddress*)cpy.iSLPSessionID->iSLPaddr;
							switch(this_address->iIP_Addr->GetType())
							{
								case IPv4:
								{
									CIPv4* this_ip = (CIPv4*)this_address->iIP_Addr;
									CIPv4* cpy_ip = (CIPv4*)cpy_address->iIP_Addr;
									if(memcmp(this_ip->m_Addr.m_Data,cpy_ip->m_Addr.m_Data,this_ip->m_Addr.m_Size) != 0)
									{
										return FALSE;
									}
									break;
								}
								case IPv6:
								{
									CIPv6* this_ip = (CIPv6*)this_address->iIP_Addr;
									CIPv6* cpy_ip = (CIPv6*)cpy_address->iIP_Addr;
									if(memcmp(this_ip->m_Addr.m_Data,cpy_ip->m_Addr.m_Data,this_ip->m_Addr.m_Size) != 0)
									{
										return FALSE;
									}
									break;
								}
								default: return FALSE;
							}
							break;
						}
					case FQDN:
						{
							CFQDN* this_ip = (CFQDN*)this->iSLPSessionID->iSLPaddr;
							CFQDN* cpy_ip = (CFQDN*)cpy.iSLPSessionID->iSLPaddr;
							if(memcmp(this_ip->m_FQDN.m_Data,cpy_ip->m_FQDN.m_Data,this_ip->m_FQDN.m_Size) != 0)
							{
								return FALSE;
							}
							break;
						}
					default: return FALSE;
				}	
			}
			else
			{
				return FALSE;
			}
		}
		else
		{
			return FALSE;
		}

	}
	
	return TRUE;
}

uint32_t CSessionID::GetSessionUID()
{
	if(iSETSessionID)
		return iSETSessionID->sessionId;
	else
		return 0;
}

} // end of namespace Engine
