/*================================================================================================*/
/**
 *  @file  Types.h
 *
 *  @brief types declaration
 */

/*
====================================================================================================

Copyright (c) 2007 Teleca Inc. All rights reserved.
Redistribution and modifications are permitted subject to BSD license. 
     
====================================================================================================
Revision History:
                            Modification     Tracking
Author                          Date          Number    Description of Changes
-------------------------   ------------    ----------  --------------------------------------------
Alexander V. Morozov         30.10.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#include "common/types.h"
//#include <stdio.h>
//#include <conio.h>

using namespace std;

namespace Engine {

CChoice::CChoice() : m_ObjectReady(TRUE)
{
	
}

CChoice::~CChoice()
{

}


COctetString::COctetString(uint32_t sz):
	m_Size(sz), m_ObjectReady(TRUE)
{
	m_Data = new(nothrow) uint8_t[sz];
	if(!m_Data)
		m_ObjectReady = FALSE;
}

COctetString::COctetString() : m_ObjectReady(TRUE)
{
	m_Data=0;
	m_Size=0;
}

uint8_t COctetString::ToUint8(void)
{
	uint8_t result = 0;
	if(m_Size>0)
		result=m_Data[0];
	return result;
}


uint16_t COctetString::ToUint16(void)
{
	uint16_t result = ToUint8();

	if(m_Size>=1)
		result += m_Data[1]<<8;
	return result;
}


COctetString::COctetString(uint8_t* aData,uint32_t aSize) : m_ObjectReady(TRUE)
{
	m_Data = new(nothrow) uint8_t[aSize];
	if(!m_Data)
	{
		m_ObjectReady = FALSE;
		return;
	}
	memcpy(m_Data,aData,aSize);
	m_Size=aSize;
}

uint8_t COctetString::Copy(const COctetString &aString)
{
	if(m_Data) delete m_Data;
	m_Data = new(nothrow) uint8_t[aString.m_Size];
	if(!m_Data)
		return RETURN_FATAL;
	m_Size=aString.m_Size;
	memcpy(m_Data,aString.m_Data,m_Size);
	return RETURN_OK;
}

void COctetString::Clean(void)
{
	if(m_Data != NULL) 
	{
		if(m_Size > 1)
			delete[] m_Data;
		else
			delete m_Data;
	}
	m_Data = NULL;
	m_Size = 0;
}

COctetString::~COctetString()
{
	Clean();
}
/*
COctetString::COctetString(const uint8_t* str)
{
	m_Size = (uint32_t) strlen((const char*) str);
	m_Data = new uint8_t[m_Size];
	for (uint32_t i = 0; i < m_Size; i++)
	{
		m_Data[i] = str[i];
	}
}
*/

uint8_t CBitString::Copy(const CBitString& cpy)
{
	if (&cpy == this)
	{
		return RETURN_OK;
	}
	if(m_Data)
	{
		delete m_Data;
	}
	m_Size = cpy.m_Size;
	m_Data = new(nothrow) uint8_t[m_Size];
	if(!m_Data)
		return RETURN_FATAL;
	for (uint32_t i = 0; i < m_Size; i++)
	{
		m_Data[i] = cpy.m_Data[i];
	}
	return RETURN_OK;
}

uint8_t CBitString::Copy(Engine::uint8_t value)
{
	*(m_Data) = value;
	return RETURN_OK;
}

uint8_t CBitString::Copy(Engine::uint16_t value)
{
	*(m_Data) = (uint8_t)value;
	return RETURN_OK;
}

CBitString::CBitString(uint32_t bit_count)
{
	//m_Size =  ((8 - (bit_count % 8))  + bit_count) / 8;
	m_Size = bit_count % 8;	
	m_Size = (m_Size) ? (8 - m_Size) : (m_Size);
	m_Size = m_Size + bit_count;
	m_Size = m_Size / 8;

	this->m_Data = new(nothrow) uint8_t[m_Size];
	if(!this->m_Data)
		 m_ObjectReady = FALSE;
}

CBitString::CBitString()
{
}

CBitString::~CBitString()
{
	//Engine::COctetString::~COctetString();
}

} // end of namespace Engine

#ifdef LEAK_DUMPER

uint32_t MemoryLeakCatcher::counter;

//Private Constructor
MemoryLeakCatcher::MemoryLeakCatcher(){
	counter= 0;
}
//Public Destructor
MemoryLeakCatcher::~MemoryLeakCatcher(){

}
//Public Get Instance
MemoryLeakCatcher &MemoryLeakCatcher::getInstance(){
	static MemoryLeakCatcher memoryLeakCatcher;
	return memoryLeakCatcher;
}

//Increment counter
void
MemoryLeakCatcher::increment(){
	counter++;
}
//Decrement counter
void
MemoryLeakCatcher::decrement(){
	counter--;
}
//Get counter
int
MemoryLeakCatcher::getCounter(){
	return counter;
}
#endif //LEAK_DUMPER
