/**
*  @file  ExtendedEphemeris.h
*  @brief Ver2RequestedAssistDataExtension declaration.
*  rsuvorov extended ephemeris improvement
*/

#ifndef _EXTENDED_EPHEMERIS_H_
#define _EXTENDED_EPHEMERIS_H_

#include "types.h"

namespace Engine {

struct CGanssRequestedCommonAssistanceDataList;
struct CGanssRequestedGenericAssistanceDataList;
struct CExtendedEphemeris;
struct CExtendedEphCheck;
//struct CGPSTime;

class Ver2RequestedAssistDataExtension
{
public:
	CGanssRequestedCommonAssistanceDataList	*m_pGanssRequestedCommonAssistanceDataList; //we null this structure because it ignored by XSS.
	CGanssRequestedGenericAssistanceDataList *m_pGanssRequestedGenericAssistanceDataList;//we null this structure because it ignored by XSS.
	CExtendedEphemeris	*m_pExtendedEphemeris;
	CExtendedEphCheck	*m_pExtendedEphemerisCheck;

public:
	uint8_t Copy(const Ver2RequestedAssistDataExtension&);
	bool_t IsObjectReady() { return m_ObjectReady;} 
	// const&destr
	Ver2RequestedAssistDataExtension();
    ~Ver2RequestedAssistDataExtension();
private:
	bool_t m_ObjectReady;
private:
	Ver2RequestedAssistDataExtension& operator = (const Ver2RequestedAssistDataExtension&){return *this;}
	Ver2RequestedAssistDataExtension(const Ver2RequestedAssistDataExtension&){}
};

struct CGanssRequestedCommonAssistanceDataList
{
	//void structure
};

struct CGanssRequestedGenericAssistanceDataList
{
	//void structure
};

struct CExtendedEphemeris	
{
	uint16_t validity; //value from 1 to 256
};

struct CGPSTime 
{
	uint16_t m_GpsWeek;    //number of week 0..1023
	uint16_t m_GpsTOWHour; //number of hurs in the week 0..167
};

struct CExtendedEphCheck
{
	CGPSTime m_pBeginTime;
	CGPSTime m_pEndTime;
};


}// end of namespace Engine
#endif // _EXTENDED_EPHEMERIS_H_
