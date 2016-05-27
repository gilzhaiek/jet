/*================================================================================================*/
/**
@file   setauthkey.h

@brief CSetAuthKey implementation (declaration)
*/
/*==================================================================================================

Copyright (c) 2007 Teleca Inc. All rights reserved.
Redistribution and modifications are permitted subject to BSD license. 

====================================================================================================
Revision History:
Modification								 Tracking
Author                          Date          Number    Description of Changes
-------------------------   ------------    ----------  -------------------------------------------
Roman Suvorov                30.10.2007	                 initial version
====================================================================================================
Portability:  MVSC  compiler
==================================================================================================*/

#ifndef __SET_AUTH_KEY_H__
#define __SET_AUTH_KEY_H__

#include "common/types.h"
namespace Engine
{
/*==================================================================================================
ENUMS
==================================================================================================*/
typedef enum eAUTHKEY_choice 
{
	SHORTKEY=0,		
	LONGKEY=1	
}EAUTHKEY_choice;

/*==================================================================================================
CLASS DECLARATIONS
==================================================================================================*/

class CSHORTAuthKey: public CChoice
{
	static const uint8_t IKEY_LEN=128;
public:
	CSHORTAuthKey():iKey(IKEY_LEN){;};
	~CSHORTAuthKey(){};   
	uint32_t	GetType(){return SHORTKEY;};
	CBitString iKey;
private:
	CSHORTAuthKey& operator=(const CSHORTAuthKey&){return *this;}
	CSHORTAuthKey(const CSHORTAuthKey&) : CChoice() {}
};

class CLONGAuthKey: public CChoice
{	
private:
	static const uint16_t IKEY_LEN=256;
public:
	CLONGAuthKey():iKey(IKEY_LEN){;};
	~CLONGAuthKey(){};   

	uint32_t	GetType(){return LONGKEY;};
	CBitString iKey;
};

class CSetAuthKey
{
public:
	CSetAuthKey():iAuthKey(0){;};
	CSetAuthKey(CChoice *aAuthKey):iAuthKey(aAuthKey){;};
	~CSetAuthKey(){if(iAuthKey!=0)delete iAuthKey;};
	CChoice	 *iAuthKey;
};
}//namespace Engine
#endif //__SET_AUTH_KEY_H__
