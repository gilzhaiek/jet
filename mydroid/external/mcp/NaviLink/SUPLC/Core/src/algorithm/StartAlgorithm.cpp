/**
*  @file  StartAlgorithm.cpp
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

#include "android/ags_androidlog.h"
#include <new>							// for operator new(size_t, void *_Where)

#include "algorithm/StartAlgorithm.h"


namespace Engine {

CStartAlgorithm::CStartAlgorithm()
{

}

CStartAlgorithm::~CStartAlgorithm()
{

}

bool_t CStartAlgorithm::Response(MSG* msg, CSession* session)
{
	debugMessage("%s BEGIN", __FUNCTION__);
	if(!CreateNewResponse(this, msg)->Response(msg, session))
		return FALSE;

	return TRUE;
}

CAlgorithmBase* CStartAlgorithm::CreateNewResponse(CAlgorithmBase* old, MSG* msg)
{
	CAlgorithmBase* NEW;
    debugMessage("%s BEGIN, old = 0x%x", __FUNCTION__, (unsigned int) old);
	// if incoming message doesn't present this is SUPL_START. Create this algorithm
	if (msg->m_pIncomingMessage == NULL)
	{
		debugMessage("%s before CSUPLStartAlgorithm", __FUNCTION__);
		NEW = new (old) CSUPLStartAlgorithm();
		debugMessage("%s New = 0x%x", __FUNCTION__, (unsigned int) NEW);
		return NEW;
		//return new (old) CSUPLStartAlgorithm();
	}

	// otherwise, we have SUPL_INIT. Create this algorithm
	if (msg->m_pIncomingMessage->GetType() == SUPL_INIT)
	{
		debugMessage("%s before CSUPLInitAlgorithm", __FUNCTION__);
		NEW = new (old) CSUPLInitAlgorithm();
		debugMessage("%s New = 0x%x", __FUNCTION__, (unsigned int) NEW);
		return NEW;
		//return new(old) CSUPLInitAlgorithm();				
	}	
	debugMessage("%s before CSUPLEndAlgorithm", __FUNCTION__);
	NEW = new (old) CSUPLEndAlgorithm();
	debugMessage("%s New = 0x%x", __FUNCTION__, (unsigned int) NEW);
    debugMessage("%s END", __FUNCTION__);	
    return NEW;
    
	//return new(old) CSUPLEndAlgorithm();
}


} // enf of namespace Engine
