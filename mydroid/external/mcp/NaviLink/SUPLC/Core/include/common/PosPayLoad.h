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

#ifndef _POS_PAY_LOAD_
#define _POS_PAY_LOAD_

#include "types.h"

namespace Engine {

//0x2000 = 8192
#define TI801_PAY_LOAD_LEN	0x2000 
#define RRC_PAY_LOAD_LEN	0x2000
#define RRLP_PAY_LOAD_LEN	0x2000		

enum PosPayLoad
{
	PosPayLoad_NOTHING,
	TI801_PAY_LOAD,
	RRC_PAY_LOAD,
	RRLP_PAY_LOAD
};

class CTIA801PayLoad: public CChoice
{
public:
	 CTIA801PayLoad();
	~CTIA801PayLoad();
virtual uint32_t	GetType();

public:
	COctetString	m_Tia801PayLoad;	
};

class CRRCPayLoad: public CChoice
{
public:
	 CRRCPayLoad();
     CRRCPayLoad(uint8_t*, uint32_t);
	~CRRCPayLoad();
virtual uint32_t	GetType();

public:
	COctetString	m_RRCPayLoad;	
};

class CRRLPPayLoad: public CChoice
{
public:
	 CRRLPPayLoad();
	 CRRLPPayLoad(uint8_t*, uint32_t);
	~CRRLPPayLoad();	
virtual uint32_t	GetType();

public:
	COctetString	m_RRLPPayLoad;	
};


}

#endif // _POS_PAY_LOAD_
