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
Alexander V. Morozov         25.10.2007	                 initial version
Alexander V. Morozov		 08.11.2007					 added copy constructor and '='
														 operator to LocationID.
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#ifndef _LOCATION_ID_H_
#define _LOCATION_ID_H_

/********************************************************************************
 *	Include section
 ********************************************************************************/
//#include "/usr/include/c++/4.2/vector"
#include "utils/Vector.h"
#include "common/types.h"
#include "codec/Frequencyinfo.h"
#include "codec/Measuredresultslist.h"
#include "codec/Wcdmacellinformation.h"

namespace Engine {

/********************************************************************************
 * Define section
 ********************************************************************************/
#define NMR_COUNT	15

/********************************************************************************
 * Enum section
 ********************************************************************************/
enum Status
{
	NO_CURRENT,
	CURRENT,
	UNKNOWN
};

/********************************************************************************
 * Struct section
 ********************************************************************************/

/**
 *	@breif This struct represent NMR element in GSMCellInformation
 *
 */
struct NMR
{
	uint16_t	m_RFCN;
	uint8_t	m_SIC;
	uint8_t	m_Lev;
};

enum CellType
{
		CellType_NOTHING,
		GSM,
		WCDMA,
		CDMA
};
/**
 *	@breif This struct represent GSM information for SET.
 *
 */
class GSMCellInformation : public CChoice 
{
public:
	// Mobile Country Code
	uint16_t				m_MCC;
	// Mobile Network Code
	uint16_t				m_MNC;
	// Location Area Code
	uint32_t				m_LAC;
	// Cell identity
	uint32_t				m_CI;
	// NMR
	android::Vector<NMR>*	m_aNMR;
	//std::vector<NMR>*	m_aNMR;
	// Timing Advance
	uint8_t*	m_TA;
public:
	// Const&Dest
	GSMCellInformation();
	
	virtual ~GSMCellInformation();
	// Methods
    virtual uint32_t GetType();

	uint8_t Copy(const GSMCellInformation&);

private:
	GSMCellInformation(const GSMCellInformation&) : CChoice() {}
	// operators
	GSMCellInformation& operator = (const GSMCellInformation&){return *this;}
};

/**
 *	@breif This stub struct.
 *
 */
class WCDMACellInformation: public CChoice 
{
public:
	uint64_t            m_MCC;
	uint64_t            m_MNC;
	uint64_t            m_CI;
	FrequencyInfo_t *m_frequencyInfo;	/* OPTIONAL */
	long	*m_primaryScramblingCode;	/* OPTIONAL */
	MeasuredResultsList_t *m_measuredResultsList;	/* OPTIONAL */
public:	
	WCDMACellInformation();
	~WCDMACellInformation();
	virtual uint32_t GetType();
	uint8_t Copy(const WCDMACellInformation&); 
	private:
	WCDMACellInformation(const WCDMACellInformation&) : CChoice() {}
	WCDMACellInformation& operator = (const WCDMACellInformation&) {return *this;}
};

/**
 *	@breif This stub struct.
 *
 */
class CDMACellInformation: public CChoice 
{
public:
	CDMACellInformation();
	~CDMACellInformation();
	virtual uint32_t GetType();
	uint8_t Copy(const CDMACellInformation&) { return RETURN_FATAL;} //Not supported yet
};

/**
 *	@breif LacationID is a part of SET information. 
 *
 */
class LocationID
{
public:
	CChoice*	m_pCellInformation;
	Status		m_Status;

	// Constructor
	 LocationID();	 
	 LocationID(CChoice* cellType);
    // Destructor
    ~LocationID();	
public:
	uint8_t Copy(const LocationID&);
	bool_t IsObjectReady() { return m_ObjectReady;} 
private:
	bool_t m_ObjectReady;
private:
	LocationID(const LocationID&){}
	LocationID& operator = (const LocationID&){return *this;}
};

} // namespace Engine

#endif // _LOCATION_ID_H_
