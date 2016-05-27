/**
*  @file  IGPS.h
*  @brief IGPS declaration.
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
Alexander V. Morozov         07.11.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#ifndef _GPS_H_
#define _GPS_H_

#include "common/types.h"
//#include <queue>
//#include <map> 
//#include "android/ags_queue.h" //Android STL
#include "utils/KeyedVector.h" //Android STL
//#include <windows.h>

namespace Platform {

class CGPSCommand;

//typedef std::map<Engine::uint32_t, CGPSCommand*> OutputCommandMap;
typedef android::KeyedVector<uint32_t, CGPSCommand*> OutputCommandMap; //AndroidSTL
typedef CGPSCommand*		CommandIter; //AndroidSTL
class IGPS
{
protected:
	
	// uses as for processed messages buffer
	OutputCommandMap  m_CmdMap;

public:		
			 IGPS();
	virtual	~IGPS();

public:
	static	IGPS*			NewGpsIF();
	CGPSCommand*			GetCommand();
	Engine::bool_t			AddCommand(CGPSCommand*);
	Engine::bool_t          RemoveAllCmd();
	Engine::bool_t			DataReady();
	virtual Engine::bool_t	SendData(CGPSCommand*, void*) = 0;
	
	virtual void Lock() = 0;
	virtual void Unlock() = 0;
};

}

#endif // _GPS_H_
