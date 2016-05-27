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
Alexander V. Morozov         25.10.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#ifndef _SUPL_TYPES_H_
#define _SUPL_TYPES_H_
#ifdef MCP_LOGD
#define LOGD ALOGD
#else
#define LOGD
#endif
#include "endianess.h"
#include "commonerrors.h"
#include <string.h>
#include "new"
//#include "/opt/L25.2/mydroid/bionic/libstdc++/include/new"

namespace Engine {

/***************************************************************************************************
 *	Define section
 **************************************************************************************************/
//#define NULL	0
#define TRUE	1
#define FALSE	0

/**************************************************************************************************
 * Types section
 *************************************************************************************************/
typedef unsigned char	uint8_t;
typedef unsigned short	uint16_t;
typedef unsigned int	uint32_t;
typedef unsigned long	uint64_t;

typedef signed char		int8_t;
typedef signed short	int16_t;
typedef signed int		int32_t;
typedef signed long	int64_t;

typedef uint8_t			bool_t;

typedef char			char_t;	// ascii char


typedef const char*		CONST_STRING;
typedef const wchar_t*  CONST_WSTRING;

typedef bool_t (*GPSCallBack) (Engine::uint16_t,Engine::uint32_t,void*); 

#define SAFE_NEW_OBJ(val,type) do{    \
						val = 0; \
						val = new(std::nothrow) type; \
						if(!val) \
						return RETURN_FATAL; \
						if(!val->IsObjectReady()) \
						return RETURN_FATAL; \
						} while(0)

// template <class Val,class Type> uint8_t Safe_New_Obj(Val a, Type b)
// {
// 	a = 0;
// 	a = new(std::nothrow) b;
// 	if(!a) 
// 		return RETURN_FATAL; 
// 	if(!a->isObjectReady())
// 		return RETURN_FATAL; 
// 
// 	return RETURN_OK;
// }

/**************************************************************************************************
 * Class section
 *************************************************************************************************/
class CChoice
{
public:
	bool_t IsObjectReady() { return m_ObjectReady;} 
	// Constructors
	CChoice();	
	
	// Destructor
	virtual ~CChoice();
	
	// methods
	virtual uint32_t	GetType() = 0;		
protected:
	bool_t m_ObjectReady;
};

class COctetString
{
public:
	COctetString();
	COctetString(uint32_t sz);
	COctetString(uint8_t* aData,uint32_t aSize);
	virtual ~COctetString();
	void	Clean(void);
	uint8_t Copy(const COctetString &aString);
	bool_t	IsObjectReady() { return m_ObjectReady;} 
	
	uint16_t ToUint16(void);
	uint8_t  ToUint8(void);

public:
	uint8_t*	m_Data;
	uint32_t	m_Size;
protected:
	bool_t m_ObjectReady;
private:
	COctetString(const COctetString &sz);
	COctetString& operator=(const COctetString &aString);
};

/**
 *	@breif CBitString represent a bit set.
 *
 */

class CBitString: public COctetString
{
public:
	CBitString(uint32_t);
	CBitString();
	virtual ~CBitString();
	uint8_t Copy(const CBitString&);
	uint8_t Copy(const Engine::uint8_t);
	uint8_t Copy(Engine::uint16_t);
private:
	CBitString(const CBitString&);
	CBitString& operator=(const CBitString&);
	CBitString& operator=(const Engine::uint8_t);
	CBitString& operator=(Engine::uint16_t);
};

typedef COctetString COctet;
} // namespace Engine

#ifdef LEAK_DUMPER

class MemoryLeakCatcher{
private:
	static uint32_t counter;
private:
	//Constructor
	MemoryLeakCatcher();
public:
	//Destructor
	~MemoryLeakCatcher();

	//Get instance function
	static MemoryLeakCatcher &getInstance();

	//Methods
	void increment();
	void decrement();
	int getCounter();	
};

inline void * operator new (size_t bytes){
	void *ptr= malloc(bytes);
	MemoryLeakCatcher::getInstance().increment();
	return ptr;
}

inline void operator delete(void *ptr){
	MemoryLeakCatcher::getInstance().decrement();
	free(ptr);
}

inline void * operator new[](size_t bytes){
	void *ptr= malloc(bytes);
	MemoryLeakCatcher::getInstance().increment();
	return ptr;
}

inline void operator delete [](void *ptr){
	MemoryLeakCatcher::getInstance().decrement();
	free(ptr);
}

#endif //LEAK_DUMPER

#endif //_TYPES_H_
