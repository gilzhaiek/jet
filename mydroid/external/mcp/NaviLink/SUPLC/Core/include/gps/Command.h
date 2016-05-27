/**
*  @file  Command.h
*  @brief Command declaration.
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
Alexander V. Morozov         03.12.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#ifndef _COMMAND_H_
#define _COMMAND_H_

#include "common/types.h"

namespace Platform {

class CCommand
{
public:
			 CCommand();
	virtual ~CCommand();
public:
	virtual Engine::uint32_t Execute(void* in, Engine::uint32_t in_size, 
									 void* out, Engine::uint32_t out_size) = 0;
};

} // end of namespace Platform

#endif // _ABSTRACT_COMMAND_
