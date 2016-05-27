/*================================================================================================*/
/**
@file   setsessionid.h

@brief SetSessionID implementation (declaration)
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
Alexander V. Morozov		 19.11.2007					 constructors(copy) added.
====================================================================================================
Portability:  MVSC  compiler
==================================================================================================*/

#ifndef __SETSESSIONID_H__
#define __SETSESSIONID_H__

#include "common/types.h"
#include "common/Address.h"

namespace Engine
{
/*==================================================================================================
ENUMS
==================================================================================================*/
typedef enum eSETId_choice 
{
	SETId_NOTHING = 0,
	MSISDN = 1,		
	MDN = 2,	
	MIN = 3,
	IMSI = 4,
	NAI = 5,
	IPADDRESS = 6

} ESETId_choice;

/*==================================================================================================
CLASS DECLARATIONS
==================================================================================================*/
/* MSISDN */
class MSDNChoice : public CChoice
{
private:
	static const uint8_t MSISDN_STRING_SIZE=8;
public:
	MSDNChoice();
	MSDNChoice(COctet *aData);
	MSDNChoice(const CChoice*);
	MSDNChoice(uint8_t*, uint32_t);
	uint32_t	GetType();
	COctet iMSISDN;
}; 

class MDNChoice : public CChoice
{
private:
	static const uint8_t MDN_STRING_SIZE=8;
public:
	MDNChoice();
	MDNChoice(COctet *aData);
	MDNChoice(const CChoice*);
	uint32_t	GetType();
	COctet iMDN;
}; 

class MINChoice : public CChoice
{
private:
	static const uint8_t MIN_STRING_SIZE=34;
public:
	MINChoice();
	MINChoice(CBitString *aData);
	uint32_t	GetType();
	CBitString iMin;
};

class IMSIChoice : public CChoice
{
private:
	static const uint8_t IMSI_STRING_SIZE=8;
public:
	IMSIChoice();
	IMSIChoice(uint8_t*, uint32_t);
	IMSIChoice(COctet *aData);
	IMSIChoice(const CChoice*);
	uint32_t	GetType();
	COctet iIMSI;
};

class NAIChoice : public CChoice
{
private:
	static const uint8_t NAI_STRING_SIZE=1;
public:
	NAIChoice();
	NAIChoice(uint32_t aSize);
	NAIChoice(const CChoice*);
	NAIChoice(COctet *aData);
	uint32_t	GetType();
	COctet iNai;
}; 

/* SETSessionID */
class CSETSessionID
{
public:
	uint32_t	 sessionId;
	CChoice		*iSetID;
public:
	uint8_t Copy(const CSETSessionID&);
	bool_t	IsObjectReady() { return m_ObjectReady;} 
	CSETSessionID();
	CSETSessionID(CChoice *aSetID);
	CSETSessionID(ESETId_choice aType,COctet *aData);
	CSETSessionID(ESETId_choice aType, CBitString *aBitData);
	CSETSessionID(ESETId_choice aType,IP_TYPE aIPType,COctet *aData);

	~CSETSessionID();
private:
	bool_t m_ObjectReady;
private:
	void Init(ESETId_choice aType,COctet *aData);
	void Init(IP_TYPE aType,COctet *aData);
	void Init(ESETId_choice aType,CBitString *aData);
private:
	CSETSessionID(const CSETSessionID&){}
	CSETSessionID& operator = (const CSETSessionID&){return *this;}
};

}//namespace Engine

#endif //__SETSESSIONID_H__
