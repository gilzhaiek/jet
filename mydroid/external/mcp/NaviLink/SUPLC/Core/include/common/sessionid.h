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
Alexander V. Morozov		 13.11.2007					 Added copy constructor and '=' operator	
====================================================================================================
Portability:  MVSC  compiler
==================================================================================================*/

#ifndef __SESSION_ID_H__
#define __SESSION_ID_H__

#include "types.h"
#include "slpsessionid.h"
#include "setsessionid.h"

namespace Engine {

/*==================================================================================================
ENUMS
==================================================================================================*/
/*==================================================================================================
CLASS DECLARATIONS
==================================================================================================*/

class CSessionID
{
public:
	CSessionID();
	CSessionID(CSETSessionID *aSETSessionID);
	CSessionID(CSLPSessionID *aSLPSessionID);
	~CSessionID();
	uint8_t     Copy(const CSessionID&);
	uint8_t		IsEqual(const CSessionID&);
	uint32_t	GetSessionUID();
	bool_t		IsObjectReady() { return m_ObjectReady;} 
public:
	CSETSessionID *iSETSessionID;
	CSLPSessionID *iSLPSessionID;
private:
	bool_t m_ObjectReady;
private:
	CSessionID& operator = (const CSessionID&);
	CSessionID(const CSessionID&);
};

}//namespace Engine

#endif //__SESSION_ID_H__
