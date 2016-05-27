/**
*  @file  StatusCode.h
*  @brief StatusCode declaration.
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
Alexander V. Morozov         01.11.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#ifndef _STATUS_CODE_H_
#define _STATUS_CODE_H_

namespace Engine {

enum StatusCode 
{
	FAILURE     = -1,
	UNSPECIFIED = 0,
	SYSTEM_FAILURE = 1,
	UNEXPECTED_MESSAGE = 2,
	PROTOCOL_ERROR = 3,
	DATA_MISSING = 4,
	UNEXPECTED_DATA_VALUE = 5,
	POS_METHOD_FAILURE = 6,
	POS_METHOD_MISMATCH = 7,
	POS_PROTOCOL_MISMATCH = 8,
	TARGET_SET_NOT_REACHABLE = 9,
	VERSION_NOT_SUPPORTED = 10,
	RESOURCE_SHORTAGE = 11,
	INVALID_SESSION_ID = 12,
	NON_PROXY_MODE_NOT_SUPPORTED = 13,
	PROXY_MODE_NOT_SUPPORTED = 14,
	POSITIONING_NOT_PERMITTED = 15,
	AUTH_NET_FAILURE = 16,
	AUTH_SUPL_INIT_FAILURE = 17,
	CONSENT_DENIED_BY_USER = 100,
	CONSENT_GRANTED_BY_USER = 101

};

}
#endif // _STATUS_CODE_H_
