/**
*  @file  StartAlgorithm .h
*  @brief CStartAlgorithm  declaration.
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

#ifndef _START_ALGORITHM_H_
#define _START_ALGORITHM_H_

#include "Algorithm.h"
#include "algorithm/SUPLStartAlgorithm.h"
#include "algorithm/SUPLInitAlgorithm.h"
#include "algorithm/SUPLEndAlgorithm.h"

namespace Engine {

class CStartAlgorithm:
	public CAlgorithmBase
{
public:
			 CStartAlgorithm();
	virtual ~CStartAlgorithm();

public:
	virtual bool_t Response(MSG*, CSession*);

protected:
	virtual CAlgorithmBase* CreateNewResponse(CAlgorithmBase*, MSG*);
};

}

#endif // _START_ALGORITHM_H_
