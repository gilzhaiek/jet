/**
*  @file  SUPLInit.h
*  @brief CSUPLInit declaration.
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
Alexander V. Morozov         12.11.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#ifndef _NOTIFICATION_H_
#define _NOTIFICATION_H_

#include "types.h"

namespace Engine {

const uint8_t MAX_CLIENT_LENGTH = 50;
const uint8_t MAX_REQ_LENGTH = 50;

enum NotificationType{
		noNotificationNoVerification		= 0,
		notificationOnly					= 1,
		notificationAndVerficationAllowedNA	= 2,
		notificationAndVerficationDeniedNA	= 3,
		privacyOverride						= 4
};

enum EncodingType {
	ucs2	= 0,
	gsmDefault	= 1,
	utf8	= 2
};

enum FormatIndicator {
	logicalName	= 0,
	e_mailAddress	= 1,
	msisdn	= 2,
	url	= 3,
	sipUrl	= 4,
	min	= 5,
	mdn	= 6,
	imsPublicIdentity	= 7
};

struct Notification
{
	NotificationType	 notificationType;
	EncodingType		*encodingType	/* OPTIONAL */;
	COctetString		*requestorId	/* OPTIONAL */;
	FormatIndicator		*requestorIdType	/* OPTIONAL */;
	COctetString		*clientName	/* OPTIONAL */;
	FormatIndicator		*clientNameType	/* OPTIONAL */;
	Notification() : encodingType(NULL),requestorId(NULL),requestorIdType(NULL),clientName(NULL),clientNameType(NULL)
	{
		/* KW changes*/
		notificationType = noNotificationNoVerification;
	}
	~Notification()
	{
		if(encodingType)
		{
			delete encodingType;
			encodingType = NULL;
		}
		if(requestorId)
		{
			delete requestorId;
			requestorId = NULL;
		}
		if(clientName)
		{
			delete clientName;
			clientName = NULL;
		}
		if(clientNameType)
		{
			delete clientNameType;
			clientNameType = NULL;
		}
	}
};

}

#endif // _NOTIFICATION_H_
