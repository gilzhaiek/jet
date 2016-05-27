/**
*  @file  Algorithm.h
*  @brief Algorithm declaration.
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
Alexander V. Morozov         19.11.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#ifndef _ALGORITHM_H_
#define _ALGORITHM_H_

#include "messages/suplmessage.h"
#include "common/MSG.h"
#include "session/Session.h"
#include "common/SUPL_defines.h"

namespace Engine {

class CSession;

class CAlgorithmBase
{	
public:
			 CAlgorithmBase();
    virtual ~CAlgorithmBase();

public:
	virtual bool_t Response(MSG*, CSession*) = 0;	

protected:
	virtual CAlgorithmBase* CreateNewResponse(CAlgorithmBase*, MSG*) = 0;	
};

}

#endif // _ALGORITHM_H_
