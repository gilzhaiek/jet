/**
*  @file  setsessionid.cpp
*  @brief CSETSessionID declaration.
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

#include "common/setsessionid.h"

using namespace std;

namespace Engine {

CSETSessionID::CSETSessionID():iSetID(NULL),m_ObjectReady(TRUE)
{

}

CSETSessionID::CSETSessionID(CChoice *aSetID):iSetID(aSetID), m_ObjectReady(TRUE)
{

}

CSETSessionID::CSETSessionID(ESETId_choice aType, COctet *aData) : m_ObjectReady(TRUE)
{
	Init(aType,aData);
}

CSETSessionID::CSETSessionID(ESETId_choice aType,IP_TYPE aIPType,COctet *aData) : m_ObjectReady(TRUE)
{
	if(aType==IPADDRESS)
	{
		Init(aIPType,aData);
	}
	else
	{
		Init(aType,aData);
	}
}

CSETSessionID::CSETSessionID(ESETId_choice aType, CBitString *aBitData) : m_ObjectReady(TRUE)
{
	Init(aType,aBitData);
}

CSETSessionID::~CSETSessionID()
{
	if(iSetID != NULL) 
	{
		delete iSetID;
	}
}

void CSETSessionID::Init(ESETId_choice aType,COctet *aData)
{
	switch(aType)
	{
	case MSISDN:
		iSetID=new(nothrow) MSDNChoice(aData);
		if(!iSetID)
			m_ObjectReady = FALSE;
		break;
	case MDN:
		iSetID=new(nothrow) MDNChoice(aData);
		if(!iSetID)
			m_ObjectReady = FALSE;
		break;
	case MIN:
		m_ObjectReady = FALSE;
		break;
	case IMSI:
		iSetID=new(nothrow) IMSIChoice(aData);
		if(!iSetID)
			m_ObjectReady = FALSE;
		break;
	case NAI:
		iSetID=new(nothrow) NAIChoice(aData);
		if(!iSetID)
			m_ObjectReady = FALSE;
		break;
	case IPADDRESS:
		Init(IPv4,aData);
		break;
	default:
		m_ObjectReady = FALSE;
		break;
	}
}


uint8_t CSETSessionID::Copy(const CSETSessionID& cpy)
{
	if (this == &cpy)
	{
		return RETURN_OK;
	}

	if (cpy.iSetID == NULL)
	{
		return RETURN_OK;
	}

	if (this->iSetID != NULL)
	{
		delete iSetID;
	}

	iSetID = NULL;

	switch(cpy.iSetID->GetType())
	{
	case MSISDN:
		iSetID = new(nothrow) MSDNChoice(cpy.iSetID);
		if(!iSetID)
			return RETURN_FATAL;
		break;
	case MDN:
		iSetID = new(nothrow) MDNChoice(cpy.iSetID);
		if(!iSetID)
			return RETURN_FATAL;
		break;
	case MIN:
		return RETURN_FATAL;
		break;
	case IMSI:
		iSetID = new(nothrow) IMSIChoice(cpy.iSetID);
		if(!iSetID)
			return RETURN_FATAL;
		break;
	case NAI:
		iSetID = new(nothrow) NAIChoice(cpy.iSetID);
		if(!iSetID)
			return RETURN_FATAL;
		break;
	case IPADDRESS:
		iSetID = new(nothrow) CIPAddress(cpy.iSetID);
		if(!iSetID)
			return RETURN_FATAL;
		break;
	default:
		return RETURN_FATAL;
	}
	sessionId = cpy.sessionId;

	return RETURN_OK;
}

void CSETSessionID::Init(IP_TYPE aIPType,COctet *aData)
{
	iSetID=new(nothrow) CIPAddress(aIPType,aData);
	if(!iSetID)
		m_ObjectReady = FALSE;
}
void CSETSessionID::Init(ESETId_choice aType,CBitString *aData)
{
	if(aType==MIN) 
	{
		iSetID=new(nothrow) MINChoice(aData);
		if(!iSetID)
			m_ObjectReady = FALSE;
	}
	else 
		iSetID=0;
}


MSDNChoice::MSDNChoice():iMSISDN(MSISDN_STRING_SIZE)
{
	if(!iMSISDN.IsObjectReady())
		m_ObjectReady = FALSE;
}

MSDNChoice::MSDNChoice(COctet *aData):iMSISDN(MSISDN_STRING_SIZE)
{
	if(!iMSISDN.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
	if(iMSISDN.Copy(*aData) != RETURN_OK)
		m_ObjectReady = FALSE;
}

MSDNChoice::MSDNChoice(const CChoice* cpy)
{
	MSDNChoice* msdn = (MSDNChoice*) cpy;
	if(iMSISDN.Copy(msdn->iMSISDN) != RETURN_OK)
		m_ObjectReady = FALSE;
}

MSDNChoice:: MSDNChoice(uint8_t* data, uint32_t size)
{
	iMSISDN.Clean();
	iMSISDN.m_Data = new(nothrow) uint8_t[size];
	if(!iMSISDN.m_Data)
	{
		m_ObjectReady = FALSE;
		return;
	}
	iMSISDN.m_Size = size;
	for (uint32_t i = 0; i < size; i++)
	{
		iMSISDN.m_Data[i] = data[i];
	}
}
uint32_t MSDNChoice::GetType()
{
	return MSISDN;
}

MDNChoice::MDNChoice() : iMDN(MDN_STRING_SIZE)
{
	if(!iMDN.IsObjectReady())
		m_ObjectReady = FALSE;
}

MDNChoice::MDNChoice(COctet *aData) : iMDN(MDN_STRING_SIZE)
{
	if(!iMDN.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
	if(iMDN.Copy(*aData) != RETURN_OK)
		m_ObjectReady = FALSE;
}

MDNChoice::MDNChoice(const CChoice* cpy)
{
	MDNChoice* mdn = (MDNChoice*) cpy;
	if(iMDN.Copy(mdn->iMDN) != RETURN_OK)
		m_ObjectReady = FALSE;
}

uint32_t	MDNChoice::GetType()
{
	return MDN;
}

MINChoice::MINChoice() : iMin(MIN_STRING_SIZE)
{
	if(!iMin.IsObjectReady())
		m_ObjectReady = FALSE;
}

MINChoice::MINChoice(CBitString *aData) : iMin(MIN_STRING_SIZE)
{
	if(!iMin.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
	if(iMin.Copy(*aData) != RETURN_OK)
		m_ObjectReady = FALSE;
}

uint32_t MINChoice::GetType()
{
	return MIN;
}

IMSIChoice::IMSIChoice() : iIMSI(IMSI_STRING_SIZE)
{
	if(!iIMSI.IsObjectReady())
		m_ObjectReady = FALSE;
}

IMSIChoice::IMSIChoice(uint8_t* data, uint32_t size)
{
	iIMSI.Clean();
	iIMSI.m_Data = new(nothrow) uint8_t[size];
	if(!iIMSI.m_Data)
	{
		m_ObjectReady = FALSE;
		return;
	}
	iIMSI.m_Size = size;
	for (uint32_t i = 0; i < size; i++)
	{
		iIMSI.m_Data[i] = data[i];
	}
}

IMSIChoice::IMSIChoice(COctet *aData) : iIMSI(IMSI_STRING_SIZE)
{
	if(!iIMSI.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
	if(!iIMSI.Copy(*aData))
		m_ObjectReady = FALSE;
}

IMSIChoice::IMSIChoice(const CChoice* cpy)
{
	iIMSI.Clean();
	iIMSI.m_Data = new(nothrow) uint8_t[((IMSIChoice*)cpy)->iIMSI.m_Size];
	if(!iIMSI.m_Data)
	{
		m_ObjectReady = FALSE;
		return;
	}
	iIMSI.m_Size = ((IMSIChoice*)cpy)->iIMSI.m_Size;
	for (uint32_t i = 0; i < ((IMSIChoice*)cpy)->iIMSI.m_Size; i++)
	{
		iIMSI.m_Data[i] = ((IMSIChoice*)cpy)->iIMSI.m_Data[i];
	}
}

uint32_t IMSIChoice::GetType()
{
	return IMSI;
}

NAIChoice::NAIChoice() : iNai(NAI_STRING_SIZE)
{
	if(!iNai.IsObjectReady())
		m_ObjectReady = FALSE;
}

NAIChoice::NAIChoice(uint32_t aSize) : iNai(aSize)
{
	if(!iNai.IsObjectReady())
		m_ObjectReady = FALSE;
}

NAIChoice::NAIChoice(COctet *aData) : iNai(NAI_STRING_SIZE)
{
	if(!iNai.IsObjectReady())
	{
		m_ObjectReady = FALSE;
		return;
	}
	if(iNai.Copy(*aData) != RETURN_OK)
		m_ObjectReady = FALSE;
}

NAIChoice::NAIChoice(const CChoice* cpy)
{
	NAIChoice* nai = (NAIChoice*) cpy;
	if(iNai.Copy(nai->iNai) != RETURN_OK)
		m_ObjectReady = FALSE;
}

uint32_t NAIChoice::GetType()
{
	return NAI;
}


} // end of namespace Engine
