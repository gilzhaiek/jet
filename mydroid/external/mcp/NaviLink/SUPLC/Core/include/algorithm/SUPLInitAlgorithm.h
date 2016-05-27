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

#ifndef _SUPL_INIT_ALGORITHM_H_
#define _SUPL_INIT_ALGORITHM_H_

#include "Algorithm.h"
#include "algorithm/SUPLPosInitAlgorithm.h"
#include "algorithm/SUPLEndAlgorithm.h"
#include "messages/SUPLInit.h"
#include "device/ags_Device.h"
#include "messages/SUPLEnd.h"
#include "suplcontroller/SUPLController.h"

namespace Engine {

class CSUPLInitAlgorithm:
	public CAlgorithmBase
{
public:
			 CSUPLInitAlgorithm();
	virtual ~CSUPLInitAlgorithm();

public:
	virtual bool_t Response(MSG*, CSession*);
			CommonError CheckPacket(MSG*, CSession*);

protected:
	virtual CAlgorithmBase* CreateNewResponse(CAlgorithmBase*, MSG*);


};

}

#endif
