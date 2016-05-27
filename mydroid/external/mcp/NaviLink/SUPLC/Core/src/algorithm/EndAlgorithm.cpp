/**
*  @file  EndAlgorithm.cpp
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

#include "algorithm/EndAlgorithm.h"
#include "utils/Log.h"

namespace Engine {

CEndAlgorithm::CEndAlgorithm()
{

}

CEndAlgorithm::~CEndAlgorithm()
{

}

bool_t CEndAlgorithm::Response(MSG* msg, CSession* session)
{
	LOGD("CEndAlgorithm::Response");
	if(session)
		session->SetSessionStatus(SESSION_INACTIVE);	
	
	return TRUE;
}

CAlgorithmBase* CEndAlgorithm::CreateNewResponse(CAlgorithmBase*, MSG*)
{	
	return this;
}

} // end of namespace Engine
