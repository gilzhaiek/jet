/**
*  @file  SUPLEndAlgorithm .h
*  @brief CSUPLEndAlgorithm  declaration.
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

#ifndef _SUPL_END_ALGORITHM_H_
#define _SUPL_END_ALGORITHM_H_

#include "Algorithm.h"
#include "algorithm/EndAlgorithm.h"
#include "messages/SUPLEnd.h"
#include "common/SWCaps.h"
#include "suplcontroller/SUPLController.h"

namespace Engine {

class CSUPLEndAlgorithm:
	public CAlgorithmBase
{
public:
	CSUPLEndAlgorithm();
   ~CSUPLEndAlgorithm();

public:
	virtual bool_t Response(MSG*, CSession*);
	static	bool_t PrepareErrorMessage(MSG*,Engine::CSessionID&,Engine::StatusCode,Engine::CBitString** Ver=NULL);
	static  bool_t PrepareErrorMessage(MSG* msg,Engine::StatusCode code,Engine::CBitString** Ver);

protected:
	virtual CAlgorithmBase* CreateNewResponse(CAlgorithmBase*, MSG*);

};

} // end of namespace Engine

#endif // _SUPL_END_ALGORITHM_H_
