/**
 *  @file  SUPLStart.h
 *  @brief CSUPLStart declaration.
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
Alexander V. Morozov         29.10.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#ifndef _QOP_H_
#define _QOP_H_

#include "types.h"

namespace Engine {

class QoP
{
public:
	uint8_t		m_Horacc;
	uint8_t*	m_pVeracc;
	uint32_t*	m_pMaxLocAge;
	uint8_t*	m_pDelay;
public:
	uint8_t Copy(const QoP&);
	bool_t IsObjectReady() { return m_ObjectReady;} 
	// Constructors
	QoP();
   ~QoP();

private:
	QoP& operator = (const QoP&){return *this;}
	QoP(const QoP&){}
private:
	bool_t m_ObjectReady;
};

} // namespace Engine {

#endif //_SUPL_QOP_H_
