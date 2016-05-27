/*================================================================================================*/
/**
@file   SlpAddr.h

@brief CSlpAddr implementation (declaration)
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

#ifndef __SLPADDR_H__
#define __SLPADDR_H__

#include "types.h"
#include "Address.h"
namespace Engine
{

/*==================================================================================================
ENUMS
==================================================================================================*/

/*==================================================================================================
CLASS DECLARATIONS
==================================================================================================*/

/* SlpSessionID */
class CSLPSessionID
{
public:
	COctet	 iSessionId;
	//may be CFQDN,CIPAddress
	CChoice *iSLPaddr;
private:
	bool_t m_ObjectReady;
public:
	uint8_t Copy(const CSLPSessionID&);
	bool_t	IsObjectReady() { return m_ObjectReady;} 
	CSLPSessionID();
	CSLPSessionID(CChoice *aSLPaddr);
	CSLPSessionID(ADDRESS_TYPE aType);
	CSLPSessionID(ADDRESS_TYPE aType, COctetString *aData);
	CSLPSessionID(IP_TYPE aType, COctetString *aData);
	~CSLPSessionID();
private:
	static const uint8_t SESSION_ID_SIZE=4;
	void Init(ADDRESS_TYPE aType,IP_TYPE aIPType, COctetString *aData);
private:
	CSLPSessionID(const CSLPSessionID&){}
	CSLPSessionID& operator = (const CSLPSessionID&){return *this;}
};

}

#endif //__SLPSESSION_H__
