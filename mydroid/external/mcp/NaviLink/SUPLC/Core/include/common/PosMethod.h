/**
 *  @file  LocationID.h
 *  @brief LocationID declaration.
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
Alexander V. Morozov         09.10.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#ifndef _POS_METHOD_H_
#define _POS_METHOD_H_

namespace Engine {

enum PosMethod
{
	POS_METHOD_AGPS_SET_ASSISTED =0,
	POS_METHOD_AGPS_SET_BASED =1,
	POS_METHOD_AGPS_SET_ASSISTED_PREF =2,
	POS_METHOD_AGPS_SET_BASED_PREF =3,
	POS_METHOD_AUTONOMUS_GPS =4,
	POS_METHOD_AFLT =5,
	POS_METHOD_ECID =6,
	POS_METHOD_EOTD =7,
	POS_METHOD_OTDOA =8,
	POS_METHOD_NO_POSITION =9
};

}

#endif //_POS_METHOD_H_
