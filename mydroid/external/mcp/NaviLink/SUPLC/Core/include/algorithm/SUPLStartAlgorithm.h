/**
*  @file  SUPLStartAlgorithm .h
*  @brief CSUPLStartAlgorithm  declaration.
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

#ifndef _SUPL_START_ALGORITHM_H_
#define _SUPL_START_ALGORITHM_H_

#include "Algorithm.h"
#include "algorithm/SUPLResponseAlgorithm.h"
#include "algorithm/SUPLEndAlgorithm.h"
#include "messages/SUPLStart.h"
#include "device/ags_Device.h"
#include "suplcontroller/SUPLController.h"


namespace Engine {

class CSUPLStartAlgorithm: 
	public CAlgorithmBase
{
public:
			 CSUPLStartAlgorithm();
	virtual ~CSUPLStartAlgorithm();

public:
	virtual bool_t Response(MSG*, CSession*);

protected:
	virtual CAlgorithmBase* CreateNewResponse(CAlgorithmBase*, MSG*);	
};

} // enf of namespace Engine

#endif // _SUPL_START_ALGORITHM_H_
