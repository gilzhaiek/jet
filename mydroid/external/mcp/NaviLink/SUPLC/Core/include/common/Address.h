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

#ifndef _ADDRESS_H_
#define _ADDRESS_H_

#include "types.h"

namespace Engine {

enum IP_TYPE
{
	IP_NOTHING = 0,
	IPv4 = 1,
	IPv6 = 2,
};

enum ADDRESS_TYPE
{
	ADDR_NOTHING = 0,
	IP_ADDRESS = 1,
	FQDN = 2
};

class CIPAddress: public CChoice
{
private:
	void Init(IP_TYPE aType, COctetString *aData);
public:
	CIPAddress();
	CIPAddress(IP_TYPE aType);
	CIPAddress(IP_TYPE aType,COctetString *aData);
	CIPAddress(const CChoice*);
	~CIPAddress();
	uint32_t GetType();
public:
	CChoice *iIP_Addr;
};

class CIPv4: public CChoice
{
	static const uint8_t IPv4_LEN = 4;
public:
	CIPv4();
	CIPv4(COctetString *aData);
   ~CIPv4();   
   uint32_t GetType();
   uint8_t Copy(const CIPv4&);

public:
	COctet m_Addr;

private:
	CIPv4(const CIPv4& mCIPv4): CChoice() {}
	CIPv4& operator = (const CIPv4&) {return *this;}
};

class CIPv6: public CChoice
{
	static const uint8_t IPv6_LEN = 16;
public:
	 CIPv6();
	 CIPv6(COctetString *aData);
	~CIPv6();
	uint32_t GetType();
	uint8_t Copy(const CIPv6&);

public:
	COctet	m_Addr;
protected:
	CIPv6(const CIPv6& mCIPv6): CChoice(){}
	CIPv6& operator = (const CIPv6& mCIPv6)
	{
		m_ObjectReady = mCIPv6.m_ObjectReady;
		return *this;
	}
};

class CFQDN: public CChoice
{
private:
	static const uint8_t FQDN_STRING_SIZE = 255;
public:
	CFQDN();
	CFQDN(COctetString *aFQDN);
	~CFQDN();
	uint32_t GetType();
	uint8_t Copy(const CFQDN&);
public:
	COctet	 m_FQDN; 
private:
	CFQDN(const CFQDN& mCFQDN): CChoice(){}
	CFQDN& operator = (const CFQDN&){return *this;}
};

} // namespace Engine

#endif // _ADDRESS_H_
