/**
*  @file  EndAlgorithm .h
*  @brief CEndAlgorithm  declaration.
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
Alexander V. Morozov         20.11.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#ifndef _END_ALGORITHM_H_
#define _END_ALGORITHM_H_

#include "Algorithm.h"

namespace Engine {

class CEndAlgorithm:
	public CAlgorithmBase
{
public:
	CEndAlgorithm();
   ~CEndAlgorithm();

public:
	virtual bool_t Response(MSG*, CSession*);

protected:
	virtual CAlgorithmBase* CreateNewResponse(CAlgorithmBase*, MSG*);

};

} // end of namespace Engine

#endif // _SUPL_END_ALGORITHM_H_
