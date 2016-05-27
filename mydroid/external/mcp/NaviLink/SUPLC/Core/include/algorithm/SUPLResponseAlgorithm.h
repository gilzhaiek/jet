/**
*  @file  SUPLResponseAlgorithm.h
*  @brief CSUPLResponseAlgorithm  declaration.
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

#ifndef _SUPL_RESPONSE_ALGORITHM_H_
#define _SUPL_RESPONSE_ALGORITHM_H_

#include "Algorithm.h"
#include "algorithm/SUPLEndAlgorithm.h"
#include "messages/SUPLResponse.h"
#include "messages/SUPLEnd.h"
#include "messages/SUPLPosInit.h"
#include "algorithm/SUPLPosInitAlgorithm.h"

namespace Engine {

class CSUPLResponseAlgorithm:
	public CAlgorithmBase
{
public:
	CSUPLResponseAlgorithm();
   ~CSUPLResponseAlgorithm();

public:
	virtual bool_t	Response(MSG*, CSession*);
			CommonError	CheckPacket(MSG*, CSession*);

protected:
	virtual CAlgorithmBase* CreateNewResponse(CAlgorithmBase*, MSG*);

};

} // end of namespace Engine

#endif // _SUPL_RESPONSE_ALGORITHM_H_
