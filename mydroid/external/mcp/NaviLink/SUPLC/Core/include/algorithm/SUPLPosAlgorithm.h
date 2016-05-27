/**
*  @file  SUPLPosAlgorithm.h
*  @brief CSUPLPosAlgorithm  declaration.
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

#ifndef _SUPL_POS_ALGORITHM_H_
#define _SUPL_POS_ALGORITHM_H_

#include "Algorithm.h"
#include "algorithm/SUPLEndAlgorithm.h"
#include "messages/SUPLPos.h"
#include "messages/SUPLEnd.h"
#include "gps/GPS.h"
#include "common/PosPayLoad.h"

namespace Engine {

class CSUPLPosAlgorithm: 
	public CAlgorithmBase
{
public:
			 CSUPLPosAlgorithm();
	virtual ~CSUPLPosAlgorithm();

public:
	virtual bool_t   Response(MSG*, CSession*);
			CommonError CheckPacket(MSG*,CSession*);

protected:
	virtual CAlgorithmBase* CreateNewResponse(CAlgorithmBase*, MSG*);	
};

} // end of namespace Engine

#endif //_SUPL_POS_INIT_ALGORITHM_H_
