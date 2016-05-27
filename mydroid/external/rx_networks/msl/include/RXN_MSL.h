/*
 * Copyright (c) 2012 Rx Networks, Inc. All rights reserved.
 *
 * Property of Rx Networks
 * Proprietary and Confidential
 * Do NOT distribute
 * 
 * Any use, distribution, or copying of this document requires a 
 * license agreement with Rx Networks. 
 * Any product development based on the contents of this document 
 * requires a license agreement with Rx Networks. 
 * If you have received this document in error, please notify the 
 * sender immediately by telephone and email, delete the original 
 * document from your electronic files, and destroy any printed 
 * versions.
 *
 * This file contains sample code only and the code carries no 
 * warranty whatsoever.
 * Sample code is used at your own risk and may not be functional. 
 * It is not intended for commercial use.   
 *
 * Example code to illustrate how to integrate Rx Networks PGPS 
 * System into a client application.
 *
 * The emphasis is in trying to explain what goes on in the software,
 * and how to the various API functions relate to each other, 
 * rather than providing a fully optimized implementation.
 *
 *************************************************************************
 * $LastChangedDate: 2009-03-23 14:19:48 -0700 (Mon, 23 Mar 2009) $
 * $Revision: 9903 $
 *************************************************************************
 */

/**
 * \file
 * \brief
 * The Mobile Suite API for Rx Networks technologies.
 * 
 * \details
 * The Mobile Suite Library (MSL) will support a unified interface to all GPStream products. GL implementation will 
 * start and stop threads. These threads will perform all tasks required to generate EE. The GL
 * API will also contain functions to support getting assistance (EE and anciliary data) as well
 * as functions to support setting broadcast ephemeris records within autonomous mode to support EE creation.
 * 
 */

#ifndef RXN_MSL_H
#define RXN_MSL_H

#ifdef __cplusplus
extern "C" {
#endif

#include "RXN_data_types.h"     /* Defines RXN Data Types. */
#include "RXN_constants.h"      /* Defines RXN Constants. */
#include "RXN_structs.h"        /* Defines RXN Data Structs. */
#include "RXN_MSL_Log.h"        /* Defines logging fuctions and constants */
#include "RXN_MSL_Options.h"    /* Compile options */

#include <time.h>

/*********************************************************
 * Common elements used by the MSL, CL and RL are below. *
 *********************************************************/

/**
 *\brief
 * Enumeration of cell types. Based on OMA-TS-ULP V1.0 section 8.3
 */
enum RXN_CellType {
  RXN_CELLTYPE_GSM = 0, /*!< GSM Cell Type. */
  RXN_CELLTYPE_CDMA,    /*!< CDMA Cell Type. */
  RXN_CELLTYPE_WCDMA    /*!< WCDMA Cell Type. */
};

/**
 *\brief
 * Structure to store Network Measurement Report data. Based on OMA-TS-ULP V1.0 section 8.3.
 */
typedef struct RXN_NMR {
  U16 aRFCN;    /*!< ARFCN. Range 0 - 1023. */
  U08 bSIC;     /*!< BSIC. Range 0 - 63. */
  U08 rxLev;    /*!< Reciever Level. Range 0 - 63. */
}RXN_NMR_t;

/**
 *\brief
 * Structure to a list of RXN_NMR_t elements. Based on OMA-TS-ULP V1.0 section 8.3.
 */
typedef struct RXN_NMRLIST {
  U08 numEntries;     /*!< Number of entries in the list. Range 1 - 15. */
  RXN_NMR_t List[15]; /*!< RXN_NMR_t elements. */
} RXN_NMRLIST_t;

/**
 *\brief
 * Structure to store GSM cell info. Based on OMA-TS-ULP V1.0 section 8.3.
 * Note that optional values may be NULL (0) if unused.
 */
typedef struct RXN_GSMCellInformation {
  U16 refMCC;           /*!< Mobile Country Code. Range 0 - 999. */
  U16 refMNC;           /*!< Mobile Network Code. Range 0 - 999. */
  U16 refLAC;           /*!< Location Area Code. Range 0 - 65535. */
  U16 refCI;            /*!< Cell Identity. Range 0 - 65535. */
  RXN_NMRLIST_t* nMR;   /*!< NMR list (OPTIONAL). */
  U08 tA;               /*!< Timing Advance. Range 0 - 255 (OPTIONAL). */
} RXN_GSMCellInformation_t;

/**
 *\brief
 * Structure to store CDMA cell info. Based on OMA-TS-ULP V1.0 section 8.3.
 */
typedef struct RXN_CDMACellInformation{
  U16 refNID;           /*!< Network ID. Range 0 - 65535. */
  U16 refSID;           /*!< System ID. Range 0 - 32767. */
  U16 refBASEID;        /*!< Base Station ID. Range 0 - 65535. */
  U32 refBASELAT;       /*!< Base Station Latitude. Range 0 - 4194303. */
  U32 refBASELON;       /*!< Base Station Longitude. Range 0 - 8388607. */
  U16 refBASEPN;        /*!< Base Station PN Code. Range 0 - 511. */
  U16 refWeekNum;       /*!< GPS Week Number. Range 0 - 65535. */
  U32 refSeconds;       /*!< GPS Seconds. Range 0 - 4194303. */
} RXN_CDMACellInformation_t;

/**
 *\brief
 * Enumeration of frequency info types Based on OMA-TS-ULP V1.0 section 8.3
 */
enum RXN_WCDMAFreqInfoType {
  RXN_FREQINFOTYPE_FDD = 0, /*!< FDD type.*/
  RXN_FREQINFOTYPE_TDD      /*!< TDD type.*/
};

/**
 *\brief
 * Structure to store WCDMA FDD freqinfo. Based on OMA-TS-ULP V1.0 section 8.3
 */
typedef struct RXN_WCDMAFreqInfoFDD {
  U16 uarfcnUL;             /*!< UARFCN-UL. Range 0 - 16383. */
  U16 uarfcnDL;             /*!< UARFCN-DL. Range 0 - 16383. */
} RXN_WCDMAFreqInfoFDD_t;

/**
 *\brief
 * Structure to store WCDMA TDD freqinfo. Based on OMA-TS-ULP V1.0 section 8.3
 */
typedef struct RXN_WCDMAFreqInfoTDD {
  U16 uarfcnNt;             /*!< UARFCN-Nt. Range 0 - 16383. */
} RXN_WCDMAFreqInfoTDD_t;

/**
 *\brief
 * Union to store WCDMA freqinfo. Based on OMA-TS-ULP V1.0 section 8.3
 */
typedef union RXN_WCDMAFreqInfo {
  RXN_WCDMAFreqInfoFDD_t FDDInfo; /*!< FDD Freq Info. */
  RXN_WCDMAFreqInfoTDD_t TDDInfo; /*!< TDD Freq Info. */
} RXN_WCDMAFreqInfo_t;

/**
 *\brief
 * Structure to store WCDMA FDD cell measurement results. Based on OMA-TS-ULP V1.0 section 8.3
 */
typedef struct RXN_WCDMAFDDCellMeasResults {
  U32 cellID;                     /*!< Cell Id. Range 0 - 268435455. */
  U16 priCPICHInfo;               /*!< Primary CPICH Info. Range 0 - 511. */
  U08 CPICHEcN0;                  /*!< CPICH-Ec-N0. Range 0 - 63 (OPTIONAL). */
  U08 CPICHRSCP;                  /*!< CPICH-RCSP. Range 0 - 91, 123 - 127 (OPTIONAL). */
  U08 pathLoss;                   /*!< Path Loss. Range 46 - 173. */
} RXN_WCDMAFDDCellMeasResults_t;

/**
 *\brief
 * Structure to store WCDMA TDD cell measurement results. Based on OMA-TS-ULP V1.0 section 8.3
 */
typedef struct RXN_WCDMATDDCellMeasResults {
  U32 cellID;                     /*!< Cell Id. Range 0 - 268435455. */
  U08 cellParamID;                /*!< Cell Parameters ID. Range 0 - 127. */
  U08 propTGSN;                   /*!< Proposed TGSN. Range 0 - 14 (OPTIONAL). */
  U08 priCCPCH_RSCP;              /*!< Primary CCPCH-RSCP. Range 0 - 127 (OPTIONAL). */
  U08 pathLoss;                   /*!< Path Loss. Range 46 - 173 (OPTIONAL). */
  U08 timeslotISCPList[14];       /*!< Timeslot ISCP List. Range 0 - 127 (OPTIONAL). */
} RXN_WCDMATDDCellMeasResults_t;

/**
 *\brief
 * Union to store WCDMA cell measurement results. Based on OMA-TS-ULP V1.0 section 8.3
 */
typedef union RXN_WCDMACellMeasResults {
  RXN_WCDMAFDDCellMeasResults_t FDDResults; /*!< FDD Cell Measurement Results. */
  RXN_WCDMATDDCellMeasResults_t TDDResults; /*!< TDD Cell Measurement Results. */
} RXN_WCDMACellMeasResults_t; 

/**
 *\brief
 * Structure including a list of RXN_WCDMACellMeasResults_t elements.
 * Based on OMA-TS-ULP V1.0 section 8.3.
 */
typedef struct RXN_WCDMACellMeasResultsList {
  U08 numEntries;                       /*!< Number of entries in the list. Range 1 - 32. */
  RXN_WCDMACellMeasResults_t List[32];  /*!< RXN_WCDMACellMeasResults_t elements. */
} RXN_WCDMACellMeasResultsList_t;

/**
 *\brief
 * Structure to store WCDMA measurement results. Based on OMA-TS-ULP V1.0 section 8.3.
 * Note that optional values may be NULL (0) if unused.
 */
typedef struct RXN_WCDMAMeasResults {
  U08 FreqInfoType;                                   /*!< Enumerated frequency info types (RXN_WCDMAFreqInfoType).*/
  RXN_WCDMAFreqInfo_t FreqInfo;                       /*!< Freq Info union (OPTIONAL). */
  U08 ultraCarrierRSSI;                               /*!< ULTRA Carrier RSSI. Range 0 - 127 (OPTIONAL). */
  RXN_WCDMACellMeasResultsList_t* pCellMeasResults;   /*!< Pointer to cell measurement results (OPTIONAL). */
} RXN_WCDMAMeasResults_t;

/**
 *\brief
 * Structure including a list of RXN_WCDMAMeasResults_t elements.
 * Based on OMA-TS-ULP V1.0 section 8.3.
 */
typedef struct RXN_WCDMAMeasResultsList {
  U08 numEntries;                   /*!< Number of entries in the list. Range 1 - 8. */
  RXN_WCDMAMeasResults_t List[15];  /*!< RXN_WCDMAMeasResults_t elements. */
} RXN_WCDMAMeasResultsList_t;

/**
 *\brief
 * Structure to store WCDMA cell info. Based on OMA-TS-ULP V1.0 section 8.3.
 * Note that optional values may be NULL (0) if unused.
 */
typedef struct RXN_WCDMACellInformation {
  U16 refMCC;                               /*!< Mobile Country Code. Range 0 - 999. */
  U16 refMNC;                               /*!< Mobile Network Code. Range 0 - 999. */
  U32 refUC;                                /*!< Cell Identity. Range 0 - 268435455. */
  U08 FreqInfoType;                         /*!< Enumerated frequency info types (RXN_WCDMAFreqInfoType).*/
  RXN_WCDMAFreqInfo_t FreqInfo;             /*!< Freq Info union. */
  U16 priScramblingCode;                    /*!< Primary Scrambling Code. Range 0 - 511 (OPTIONAL). */
  RXN_WCDMAMeasResultsList_t* pMeasResults; /*!< Pointer to measurement results (OPTIONAL). */
} RXN_WCDMACellInformation_t;

/**
 *\brief
 * Union to store cell info. Based on OMA-TS-ULP V1.0 section 8.3
 */
typedef union RXN_CellInfo {
  RXN_GSMCellInformation_t GsmCellInfo;     /*!< Cell info for a GSM radio.*/
  RXN_CDMACellInformation_t CdmaCellInfo;   /*!< Cell info for a CDMA radio.*/
  RXN_WCDMACellInformation_t WcdmaCellInfo; /*!< Cell info for a WCDMA radio.*/
} RXN_CellInfo_t;

/**
 *\brief
 * Enumeration of cell status info. Based on OMA-TS-ULP V1.0 section 8.3
 */
enum RXN_CellStatus {
  RXN_CELLSTATUS_STALE = 0, /*!< Status is stale.*/
  RXN_CELLSTATUS_CURRENT,   /*!< Status is current.*/
  RXN_CELLSTATUS_UNKNOWN    /*!< Status is unknown.*/
};

/**
 *\brief
 * Structure to store cell location Id data. Based on OMA-TS-ULP V1.0 section 8.3.
 */
typedef struct RXN_LocationId {
  U08 CellType;                 /*!< Enumerated cell type (RXN_CellType).*/
  RXN_CellInfo_t CellInfo;      /*!< Cell Information.*/
  U08 CellStatus;               /*!< Enumerated cell status (RXN_CellStatus).*/
} RXN_LocationId_t;

/**
 *\brief
 * Structure including a list of RXN_LocationId_t elements.
 * Based on OMA-TS-ULP V1.0 section 8.3.
 */
typedef struct RXN_LocationIdList {
  U08 numEntries;             /*!< Number of entries in the list. Range 1 - 6. */
  RXN_LocationId_t List[16];  /*!< RXN_LocationId_t elements. */
} RXN_LocationIdList_t;

/**
 * \brief
 * Structure to store reference time data.
 */
typedef struct RXN_RefTime {
  U16 weekNum;    /*!< Week number in extended format (i.e. > 1024).*/
  U32 TOWmSec;    /*!< Time-Of-Week in mSec. */
  U32 TOWnSec;    /*!< Time-Of-Week in nSec. Set to 0, if nSec accuracy not supported.*/
  U32 TAccmSec;   /*!< Time-Accuracy in mSec. Set to 0, if accuracy cannot be specified.*/
  U32 TAccnSec;   /*!< Time-Accuracy in nSec. Set to 0, if nSec accuracy cannot be specified.*/
} RXN_RefTime_t;

/**
 * \brief
 * Enumeration of the currently supported location types.
 */
enum RXN_LocationType {
  RXN_LOC_UNSET = 0,       /*!< No location set.*/
  RXN_LOC_ECEF,            /*!< ECEF location values set.*/
  RXN_LOC_LLA,             /*!< LLA location values set.*/
  RXN_LOC_BOTH             /*!< ECEF and LLA location values set.*/
};

/**
 * \brief
 * Struct describing an ECEF location.
 */
typedef struct RXN_ECEF_LOC
{
	S32 ECEF_X;	    /*!< Value in meters. */
	S32 ECEF_Y;	    /*!< Value in meters. */
	S32 ECEF_Z;	    /*!< Value in meters. */
} RXN_ECEF_LOC_t;

/**
 * \brief
 * Struct describing an LLA location.
 */
typedef struct RXN_LLA_LOC
{
	R32 Lat;		  /*!< Value in degrees. */
	R32 Lon;		  /*!< Value in degrees. */
	R32 Alt;	      /*!< Value in meters. */
} RXN_LLA_LOC_t;

/**
 * \brief
 * Structure to store reference location data. 
 * See the RXN_LocationType enumeration for possible type values.
 */
typedef struct RXN_RefLocation {
  U08 type;            /*!< Type. See RXN_LocationType.*/
  RXN_ECEF_LOC_t ECEF; /*!< ECEF location in meters. Only valid if type == RXN_LOC_ECEF or RXN_LOC_BOTH. */
  RXN_LLA_LOC_t LLA;   /*!< LLA location in degrees. Only valid if type == RXN_LOC_LLA or RXN_LOC_BOTH. */
  U32 uncertSemiMajor; /*!< Uncertainty along the major axis in centimeters. */
  U32 uncertSemiMinor; /*!< Uncertainty along the minor axis in centimeters. */
  U16 OrientMajorAxis; /*!< Angle between major and north in degrees Range 0 - 180 (OPTIONAL). 0 - circle.*/
  U08 confidence;      /*!< General indication of confidence. Range 0 - 100 (OPTIONAL). */
} RXN_RefLocation_t;

/**
 * \brief
 * Structure to store full ephemeris data.
 *
 * \details
 * This struct contains comprehensive ephemeris data.
 * This struct is a superset of the RXN_ephem_t struct. Functions exist to convert vars
 * between this struct and RXN_ephem_t (defined within RXN_API.h). Some values are scaled
 * to ICD-200. When this is the case, the scale factors are provided.
 */
typedef struct RXN_FullEph {
  U08 prn;        /*!< Ephemeris PRN or SV. Range 1-32.*/
  U16 gps_week;   /*!< Extended week number (i.e. > 1024, e.g.1486). */
  U08 CAOrPOnL2;  /*!< Only 2 least sig bits used. Not in RXN std ephemeris struct. */
  U08 ura;        /*!< User Range Accuracy index.  See IS-GPS-200 Rev D for index values.*/
  U08 health;     /*!< Corresponds to the SV health value. 6 bits as described within IS-GPS-200 Rev D.*/
  U16 iodc;		  /*!< Issue Of Data (Clock). */
  U08 L2PData;	  /*!< When 1 indicates that the NAV data stream was commanded OFF on the P-code of the L2 channel. Descrete 1/0. */
  S08 TGD;		  /*!< Time Group Delay. Scale: 2^-31. Units: seconds.*/
  U16 toc;        /*!< Time Of Clock or time of week. Scale: 2^4. Units: seconds. */
  S08 af2;        /*!< Clock Drift Rate Correction Coefficient. Scale: 2^-55. Units: sec/sec^2. */
  S16 af1;        /*!< Clock Drift Correction Coefficient. Scale: 2^-43. Units: sec/sec. */
  S32 af0;        /*!< Clock Bias Correction Coefficient. Scale: 2^-31. Units: sec. */
  S16 crs;        /*!< Amplitude of the Sine Harmonic Correction Term to the Orbit Radius. Scale: 2^-5. Units: meters. */
  S16 delta_n;    /*!< Mean Motion Difference from Computed Value. Scale: 2^-43. Units: semi-circles/sec. */
  S32 m0;         /*!< Mean Anomaly at Reference Time. Scale: 2^-31/PI. Units: semi-circles. */
  S16 cuc;        /*!< Amplitude of the Cos Harmonic Correction Term to the Arguement of Latitude. Scale: 2^-29. Units: radians. */
  U32 e;          /*!< Eccentricity. Scale: 2^-33. Units: N/A - Dimensionless. */
  S16 cus;        /*!< Amplitude of the Sine Harmonic Correction Term to the Argument of Latitude. Scale: 2^-29. Units: radians. */
  U32 sqrt_a;     /*!< Square Root of the Semi-Major Axis. Scale: 2^-19. Units: square root of meters. */
  U16 toe;        /*!< Time Of Ephemeris. Scale: 2^4. Units: seconds. */
  S08 ephem_fit;  /*!< Fit interval relating to the fit interval flag. Typically 4 hrs. */
  U08 ure;        /*!< User Range Error. Indicates EE accuracy. Units: meters.*/
  U08 AODO;		  /*!< Age Of Data Offset.*/
  S16 cic;        /*!< Amplitude of the Cos Harmonic Correction Term to the Angle of Inclination. Scale:  2^-29. Units: radians. */
  S32 omega0;     /*!< Longitude of Ascending Node of Orbit Plane and Weekly Epoch. Scale: 2^-31/PI. Units: semi-circles. */
  S16 cis;        /*!< Amplitude of the Sine Harmonic Correction Term to the Angle of Inclination. Scale: 2^-29. Units: radians. */
  S32 i0;         /*!< Inclination Angle at Reference Time. Scale: 2^-31/PI. Units: semi-circles. */
  S16 crc;        /*!< Amplitude of the Cos Harmonic Correction Term to the Orbit Radius. Scale: 2^-5. Units: meters. */
  S32 w;          /*!< Argument of Perigee. Scale: 2^-31/PI. Units: semi-circles. */
  S32 omega_dot;  /*!< Rate of Right Ascension. Scale: 2^-43. Units: semi-circles/sec. */
  U16 iode;       /*!< Issue Of Data (Ephemeris). */
  S16 i_dot;      /*!< Rate of Inclination Angle. Scale: 2^-43. Units: semi-circles/sec. */
} RXN_FullEph_t;

typedef struct RXN_FullEph_Glonass
  {
    U08 slot;         /*!< \brief Ephemeris Id for SV. Range 1-24.*/
    U08 FT;           /*!< \brief User Range Accuracy index.  P32 ICD Glonass for value of Ft.*/
    S08 freqChannel;  /*!< \brief Freq slot: -7 to +13 incl. */
    U08 M;            /*!< \brief Glonass vehicle type. M=1 means type M*/
    U08 Bn;           /*!< \brief Bn SV health see p30 ICD Glonass. */
    U08 utc_offset;   /*!< \brief Current GPS-UTC leap seconds [sec]; 0 if unknown. */
    S16 gamma;        /*!< \brief SV clock frequency error ratio scale factor 2^-40 [seconds / second] */
    S32 tauN;         /*!< \brief SV clock bias scale factor 2^-30 [seconds]. */
    U32 gloSec;       /*!< \brief gloSec=[(N4-1)*1461 + (NT-1)]*86400 + tb*900, [sec] ie sec since Jan 1st 1996 <b>see caution note in struct details description</b> */
    S32 x;            /*!< \brief x position at toe scale factor 2^-11 Km PZ90 datum. */
    S32 y;            /*!< \brief y position at toe scale factor 2^-11 Km PZ90 datum. */
    S32 z;            /*!< \brief z position at toe scale factor 2^-11 Km PZ90 datum. */
    S32 vx;           /*!< \brief x velocity at toe scale factor 2^-20 Km/s PZ90 datum. */
    S32 vy;           /*!< \brief y velocity at toe scale factor 2^-20 Km/s PZ90 datum. */
    S32 vz;           /*!< \brief z velocity at toe scale factor 2^-20 Km/s PZ90 datum. */
    S16 lsx;          /*!< \brief x luni solar accel scale factor 2^-30 Km/s^2 PZ90 datum. */ 
    S16 lsy;          /*!< \brief y luni solar accel scale factor 2^-30 Km/s^2 PZ90 datum. */ 
    S16 lsz;          /*!< \brief z luni solar accel scale factor 2^-30 Km/s^2 PZ90 datum. */ 
    U08 En;           /*!< \brief Age of current information in days */
    U08 P1;           /*!< \brief Time interval between adjacent values of tb in minutes */
    U08 P2;           /*!< \brief 1 if tb is odd and 0 if tb is even */
    U08 deltaTau;     /*!< \brief Not currently provided */
  } RXN_FullEph_GLO_t;

typedef union RXN_FullEphem
{
    RXN_FullEph_t* gpsPRNArr;
    RXN_FullEph_GLO_t* gloPRNArr;
}RXN_FullEphem_u;

typedef union RXN_Ephemeris
{
    RXN_ephem_t gpsEphemeris;
    RXN_glonass_ephem_t gloEphemeris;
}RXN_Ephemeris_u;


/**
 * \brief
 * Enumeration of the multipath indication types.
 */
enum RXN_MpathIndic {
  RXN_MPATH_NOT_MEAS  = 0,          /*!< Unmeasured.*/
  RXN_MPATH_IND_LOW,                /*!< Low.*/
  RXN_MPATH_IND_MED,                /*!< Medium.*/
  RXN_MPATH_IND_HGH                 /*!< High.*/
};

/**
 * \brief
 * GPS measurements.
 *
 * \details
 * This struct contains GPS measurements required by MSL TRKR elements including pseudo range data.
 * Based on OMA-TS-ULP V1.0.
 */

typedef struct RXN_GPSMsrElement {
  U08 satelliteID;                  /*!< Satellite ID. */
  U08 cNo;                          /*!< Carrier noise. Range 0 - 63. */
  S32 doppler;                      /*!< Doppler. Range -32768 - 32767. */
  U16 wholeChips;                   /*!< Whole value of the code phase measurement. Range 0 - 1022. */
  U16 fracChips;                    /*!< Fractional value of the code phase measurement. Range 0 - 1024. */
  U08 mpathIndic;                   /*!< Multipath Indication Type. See the RXN_MpathIndic enum. */
  U08 pseudoRangeRMSErr;            /*!< Pseudo range RMS error. Range 0 - 63. */
} RXN_GPSMsrElement_t; 

/**
 * \brief
 * GPS measurement data set element.
 *
 * \details
 * This structure contains a GPS measurement data set element. 
 * This structure is based on OMA-TS-ULP V1.0.
 */
typedef struct RXN_GPSMsrSetElement {
  U32 refFrame;                     /*!< Reference Frame. Range 0 - 42431. */
  U32 gpsTOW;                       /*!< TOW in the format matching the GPSTOW24b format within Nav Data messages (see ICD-200) */
  U08 numEntries;                   /*!< Number of entries in MsrList. Range 1 - 16. */
  RXN_GPSMsrElement_t MsrList[16];  /*!< Measurement Element List. Alloc may not be in place for 16 entries. Check numEntries! */
} RXN_GPSMsrSetElement_t;

/**
 * \brief
 * GPS measurement data sets.
 *
 * \details
 * This structure contains an array of GPS measurement data set elements required by MSL TRKR elements.
 * This structure is based on OMA-TS-ULP V1.0.
 */
typedef struct RXN_GPSMsrSetList {
  U08 numEntries;                     /*!< Number of entries in SetList. Range 1 - 3. */
  RXN_GPSMsrSetElement_t SetList[3];  /*!< List of Measurement Set Elements. Alloc may not be in place for 3 entries. Check numEntries! */
} RXN_GPSMsrSetList_t;
/*!< Measurement Set Element List. Alloc may not be in place for 3 entries. Check numEntries! */


/**************************
 * End of common elements *
 **************************/

/**
 * \brief
 * Defines the length of the license and security key.
 */
#define RXN_MSL_KEY_LENGTH             513

/** 
 * \brief
 * Defines a max length for a string containing Mobile Suite config parameters.
 */ 
#define RXN_MSL_MAX_CONFIG_PATH             512

/** 
 * \brief
 * Defines a max length for a string describing the file path.
 */ 
#define RXN_MSL_MAX_PATH                    RXN_MSL_MAX_CONFIG_PATH

/**
 * \brief
 * Defines a max length for a string describing the port or file path to open
 * or other config params. It should be the same as RXN_CL_CONFIG_MAX_STR_LEN
 */
#define RXN_MSL_CONFIG_MAX_STR_LEN           256


/*************************
 ** Error codes follow. **
 *************************/

/**
 * \brief
 * Defines an error that results when a configPath does not support opening
 * a config file.
 */
#define RXN_MSL_CONFIG_FORMAT_ERR           1

/**
 * \brief
 * Defines an error that results when a configPath does not support opening
 * a config file.
 */
#define RXN_MSL_CONFIG_READ_ERR             2

/**
 * \brief
 * Defines a general error that can result during Mobile Suite startup.
 */
#define RXN_MSL_INIT_ERR                    3

/**
 * \brief
 * Defines a general error that can result during Mobile Suite shutdown.
 */
#define RXN_MSL_UNINIT_ERR                  4

/**
 * \brief
 * Defines a general error that can result when acquiring assistance.
 */
#define RXN_MSL_GET_ASSIST_ERR              5

/**
 * \brief
 * Defines a general error that can result when setting broadcast ephemeris.
 */
#define RXN_MSL_SET_EPHEMERIS_ERR           6

/**
 * \brief
 * Defines an error indicating that a log is already open.
 */
#define RXN_MSL_TOO_MANY_INSTANCES_ERR      7

/**
 * \brief
 * Defines an error indicating that a log cannot be opened.
 */
#define RXN_MSL_FILE_OPEN_OR_CLOSE_ERR      8

/**
 * \brief
 * Defines an error indicating that a network connection is unavailable.
 */
#define RXN_MSL_SKT_COMMS_ERR               9

/**
 * \brief
 * Defines an error indicating that EOL of product has been reached.
 */
#define RXN_MSL_EOL_ERR               10


/**
 * \brief
 * Structure to store navigation data elements. Based on 3GPP TS 44.031.
 * Note that the Satellite ID value can be derived from the RXN_FullEph_t
 * prn value (Sat ID with range 0 - 31 = RXN_FullEph_t.prn - 1).
 */
typedef struct RXN_MSL_NavDataList {
  U08 numEntries;           /*!< Number of entries in the list. Range 1 - 32. */
  union {
      RXN_FullEph_t gps[RXN_CONSTANT_NUM_GPS_PRNS];				    /*!< RXN_FullEph_t elements. */
      RXN_FullEph_GLO_t glo[RXN_CONSTANT_NUM_GLONASS_PRNS];		/*!< RXN_FullEph_GLO_t elements. */
  } ephList;
} RXN_MSL_NavDataList_t;

/**
 * \brief
 * Structure to store ionospheric model data components.
 *
 */
typedef struct RXN_MSL_IonoModel {
  S08 alfa0;                /*!< Alfa 0 value. Range -128 - 127. */
  S08 alfa1;                /*!< Alfa 1 value. Range -128 - 127. */
  S08 alfa2;                /*!< Alfa 2 value. Range -128 - 127. */
  S08 alfa3;                /*!< Alfa 3 value. Range -128 - 127. */
  S08 beta0;                /*!< Beta 0 value. Range -128 - 127. */
  S08 beta1;                /*!< Beta 1 value. Range -128 - 127. */
  S08 beta2;                /*!< Beta 2 value. Range -128 - 127. */
  S08 beta3;                /*!< Beta 3 value. Range -128 - 127. */
} RXN_MSL_IonoModel_t;

/**
 * \brief
 * Structure to store UTC model data components.
 *
 */
typedef struct RXN_MSL_UTCModel {
  S32 A1;                   /*!< A1 value. Range -8388608 - 8388607. */
  S32 A0;                   /*!< A0 value. Range -2147483648 - 2147483647. */
  U08 Tot;                  /*!< Tot value. Range 0 - 255. */
  U08 WNt;                  /*!< WNt value. Range 0 - 255. */
  S08 DeltaTls;             /*!< Delta Tls value. Range -128 - 127. */
  U08 WNlsf;                /*!< WNlsf value. Range 0 - 255. */
  S08 DN;                   /*!< DN value. Range -128 - 127. */
  S08 DeltaTlsf;            /*!< Delta Tlsf value. Range -128 - 127. */
} RXN_MSL_UTCModel_t;

/**
 * \brief
 * Structure to store almanac data elmenent components.
 *
 */
typedef struct RXN_MSL_Almanac {
  U08 SatID;      /*!< Satellite ID. Range 0 - 63 (for PRN 1- 64). */  
  U16 e;          /*!< Eccentricity. Range 0 - 65535. */
  U08 Toa;        /*!< Toa value. Range 0 - 255 */
  S16 Ksii;       /*!< Ksii value. Range -32768 - 32767. */
  S16 omega_dot;  /*!< Rate of Right Ascension. Range -32768 - 32767. */
  U08 health;     /*!< Corresponds to the SV health value. Range 0 - 255.*/
  U32 sqrt_a;     /*!< Square Root of the Semi-Major Axis. Range 0 - 16777215. */
  S32 omega0;     /*!< Longitude of Ascending Node of Orbit Plane and Weekly Epoch. Range -8388608 - 8388607. */
  S32 w;          /*!< Argument of Perigee. Range -8388608 - 8388607. */
  S32 m0;         /*!< Mean Anomaly at Reference Time. Range -8388608 - 8388607. */
  S16 af0;        /*!< Clock Bias Correction Coefficient. Range -1024 - 1023. */
  S16 af1;        /*!< Clock Drift Correction Coefficient. Range -1024 - 1023. */
} RXN_MSL_Almanac_t;

/**
 * \brief
 * Structure to store a list of almanac data elmenents.
 *
 */
typedef struct RXN_MSL_AlmanacList {
  U08 WNa;                /*!< Almanac WNa value. Range 0 - 255. */
  U08 numEntries;         /*!< Number of entries in the list. Range 1 - 64. */
  RXN_MSL_Almanac_t List; /*!< RXN_MSL_Almanac_t elements. */
} RXN_MSL_AlmanacList_t;

/**
 * \brief
 * Structure to store a list of bad PRNs. Not that Satellite IDs can be derived
 * from the PRN list by simply subtracting '1' from each PRN number.
 *
 */
typedef struct RXN_MSL_BadPRNList {
  U08 numBadPRNs;         /*!< Number of bad PRNs in the list. */
  U08 BadPRNs[16];        /*!< Bad PRN elements. */
} RXN_MSL_BadPRNList;

/**
 * \brief
 * Structure to store assistance data elements. Based on 3GPP TS 44.031.
 *
 * \details
 * The structure includes pointers to a number of other structures. Each
 * of these structures contain various forms of assistance info.
 *
 */
typedef struct RXN_MSL_Assistance
{
  RXN_RefTime_t* pRefTime;              /*!< Reference time data. */
  RXN_RefLocation_t* pRefLocation;      /*!< Reference location data. */
  RXN_MSL_NavDataList_t* pNavModelData; /*!< Navigation model data (including ephemeris). */
  RXN_MSL_IonoModel_t* pIonoModelData;  /*!< Ionospheric model data. */
  RXN_MSL_UTCModel_t* pUTCModelData;    /*!< UTC model data. */
  RXN_MSL_AlmanacList_t* pAlmanacData;  /*!< Almanac data. */
  RXN_MSL_BadPRNList* pBadPRNs;         /*!< List of bad PRNs. */
} RXN_MSL_Assistance_t;


/**
 * \brief
 * Enumeration of factory reset type.
 *
 */
enum MSL_Reset_Type
{
    MSL_RESET_INVALID,					/*!< Invalid reset type. No factory reset will be executed. */
    MSL_RESET_SEED,						/*!< Remove all data from eph and pred folders. */
    MSL_RESET_TIME,						/*!< Reset all sources of GPS time supplied (SNTP and through the API call RXN_MSL_SetGPSTime(). */
    MSL_RESET_SEED_AND_TIME				/*!< Remove all data from eph and pred folders and reset time. */
};

/**
 * \brief
 * RXN_constel_t in the RXN_API.h does not enumeration a value for all constellations
 *
 */
#define RXN_ALL_CONSTEL 2

/**
 * \brief 
 * Retrieve the Mobile Suite Library (MSL) API version.
 *
 * \param version [OUT] A memory location to be filled with the current API version.
 * This is a null terminated string and is at most RXN_CONSTANT_VERSION_STRING_LENGTH
 * (50) characters long. 
 *
 * \return RXN_SUCCESS If the version is returned successfully (always).
 *
 * \details
 * <b>Description</b>\n
 * This function returns the version of the MSL API. The version number will be
 * incremented as new features, enhancements, and bug fixes are added to the MSL API.
 * The version number is an important identification when reporting and troubleshooting issues.
 * Rx Networks version numbers are broken down within Integration Users Manuals and API Manuals.
 *
 * <b>Example Usage</b>
 * \code
 * // Declare a string to hold version info.
 * char version[RXN_CONSTANT_VERSION_STRING_LENGTH];
 * 
 * // Retrieve and output version information.
 * RXN_MSL_Get_API_Version(version);
 * printf("RXN_MSL_Get_API_Version(): %s\n", version);
 * \endcode
 *
 * <b>Output</b>\n
 * RXN_MSL_Get_API_Version(): 1.0.0
 */
U16 RXN_MSL_Get_API_Version(char version[RXN_CONSTANT_VERSION_STRING_LENGTH]);

/** 
 * \brief 
 * Initialize a log for MSL use.
 *
 * \return RXN_SUCCESS if the log is initialized successfully or if the log is disabled.
 * \return RXN_MSL_FILE_OPEN_OR_CLOSE_ERR if a log file cannot be opened.
 *
 * \details
 * <b>Description</b>\n
 * Open a log file that can be used by the MSL for logging. If the log file
 * exists, it will be appended. If the log file does not exist, it will be created. 
 * Note that all instances of the MSL will share a common log.
 *
 * <b>Example Usage</b>
 * \code
 * if(RXN_MSL_Log_Init() != RXN_SUCCESS)
 * {
 *     exit(EXIT_FAILURE);
 * }
 * \endcode
 */ 
U16 RXN_MSL_Log_Init();

/** 
 * \brief 
 * Un-Initialize a log that has been opened for MSL use.
 *
 * \details
 * <b>Description</b>\n
 * Close a previously opened log file (if opened).
 *
 * <b>Example Usage</b>
 * \code
 * // Shutdown the MSL logger. Always shutdown logging last to ensure that
 * // any MSL issues that precedes this call are logged.
 * RXN_MSL_Log_UnInit();
 * \endcode
 */ 
void RXN_MSL_Log_UnInit(void);

/** 
 * \brief 
 * Load the configuration file. This must be called before RXN_MSL_Initialize().
 *
 * \param configPath 
 * [In] Specifies the path to a config file. This file will contain config
 * parameters used by the MSL. Detail on the MSL config file can be found within
 * the PGPS Integration Guide.
 *
 * \return RXN_SUCCESS if the configuration file is loaded successfully. 
 * \return RXN_MSL_CONFIG_FORMAT_ERR if the configPath string does not support opening a config file.
 * \return RXN_FAIL for all other errors.
 *
 * \details
 * <b>Description</b>\n
 * Read the MSLConfig.txt and configure the MSL to operate based on those inputs.
 *
 * <b>See Also</b>\n
 * RXN_MSL_Initialize()
 *
 * <b>Example Usage</b>
 * \code
 * #define RXN_CONFIG_FILE       ".\\MSLConfig.txt"
 * if(RXN_MSL_Load_Config(RXN_CONFIG_FILE) != RXN_SUCCESS)
 * {
 *     exit(EXIT_FAILURE);
 * }
 * \endcode
 */ 
U16 RXN_MSL_Load_Config(const char configPath[RXN_MSL_MAX_CONFIG_PATH]);

/** 
 * \brief 
 * Get the configuration string for chipset initialization.
 *
 * \param clConfigStr
 * [Out] Return the configuration string with port to open during chipset initialization as well as any
 * other config parameters. The format of the config string is chipset dependent. Some chipsets
 * (typically those supported by a host library or protocol interface) are initialized with a com
 * port string and baudrate. Note that a debug log severity max level and debug zone mask are also
 * stipulated. See details in <RXN_CL.h> RXN_CL_Initialize().
 *
 * \return RXN_SUCCESS if the chipset configuration string is returned successfully (always).
 *
 * \details
 * <b>Description</b>\n
 * Get the chipset configuration string used to connect to the GPS receiver.
 * This string is configured in MSLConfig.txt
 *
 * <b>Example Usage</b>
 * \code
 * // Declare a string to hold chipset configuration string.
 * char gCL_ConfigStr[RXN_CL_CONFIG_MAX_STR_LEN];
 *
 * if(RXN_MSL_Get_CL_Config_Str(gCL_ConfigStr) != RXN_SUCCESS)
 * {
 *     exit(EXIT_FAILURE);
 * }
 * \endcode 
 */
U16 RXN_MSL_Get_CL_Config_Str(char clConfigStr[RXN_MSL_CONFIG_MAX_STR_LEN]);

/**
 * \brief
 * Initialize the Mobile Suite Library to support generating synthetic EE.
 *
 * \return RXN_SUCCESS if the MSL is initialized successfully.
 * \return RXN_MSL_EOL_ERR if EOL (End of Life) file exists.
 * \return RXN_MSL_INIT_ERR for all other errors.
 *
 * \details
 * <b>Description</b>\n
 * Initialize the MSL component including validating license key and security key,
 * checking EOL status, initializing the O/S abstraction layer and spawning a messaging thread.
 *
 * <b>Example Usage</b>
 * \code
 * // Initialize the MSL.
 * if(RXN_MSL_Initialize() != RXN_SUCCESS)
 * {
 *     // Shutdown the CL.
 *     RXN_CL_Uninitialize(gCL_Handle);
 *
 *     // Shutdown the CL logger.
 *     RXN_CL_Log_UnInit();
 *
 *     // Shutdown the MSL logger.
 *     RXN_MSL_Log_UnInit();
 *
 *     exit(EXIT_FAILURE);
 * } 
 * \endcode
 */
U16 RXN_MSL_Initialize(void);

/** 
 * \brief 
 * Un-Initialize Mobile Suite Library (MSL).
 *
 * \return RXN_SUCCESS if the MSL is stopped successfully. 
 * \return RXN_FAIL for all errors.
 *
 * \details
 * <b>Description</b>\n
 * Un-initialize the MSL component including freeing cache memory, aborting any ephemeris
 * generation that may be underway, un-initializing thread protection, un-initializing the
 * O/S abstraction to free resources and stopping the MSL logger.
 *
 * <b>See Also</b>\n
 * RXN_MSL_Initialize()
 *
 * <b>Example Usage</b>
 * \code
 * // Stop the MSL.
 * if(RXN_MSL_Uninitialize()!= RXN_SUCCESS)
 * {
 *     exit(EXIT_FAILURE);
 * }
 * \endcode
 */ 
U16 RXN_MSL_Uninitialize(void);

/** 
 * \brief 
 * Give the Mobile Suite Library implementation time to perform PGPS tasks.
 *
 * \return RXN_SUCCESS if PGPS work is done successfully (always). 
 *
 * \details
 * <b>Description</b>\n
 * Give the MSL a chance to work. Perform a single step towards any outstanding tasks
 * required to support generation of EE. Tasks will depend on the MSL state machine.
 *
 * <b>Example Usage</b>
 * \code
 * // Declare a flag to control exit
 * static BOOL gRun = TRUE;
 * 
 * while(gRun)
 * {
 *     // Give other threads some cycles.
 *     Sleep(50);
 *	   
 *     if(RXN_MSL_Work() != RXN_SUCCESS)
 *     {
 *          break;
 *     }
 * }
 * \endcode
 */ 
U16 RXN_MSL_Work(void);


/** 
 * \brief 
 * Abort generating ephemeris.
 *
 * \param bAbort 
 * [In] TRUE to set the abort flag or FALSE to clear the abort flag.
 *
 * \return RXN_SUCCESS if the abort flag is set successfully (always).
 *
 * \details
 * <b>Description</b>\n
 * Use this function to set or clear the abort flag for generating ephemeris.
 * Generating ephemeris can require significant processing time. Integrators 
 * can use this function to abort any ephemeris generation thay may be in 
 * progress when the receiver is getting assistance data or for reducing 
 * power consumption.
 *
 * <b>See Also</b>\n
 * RXN_MSL_GetEphemeris()
 *
 * <b>Example Usage</b>
 * \code
 * // Declare a storage for assistance data.
 * RXN_MSL_Assistance_t assistData;
 *
 * memset((void*) &assistData, 0, sizeof(RXN_MSL_Assistance_t));
 *
 * // Abort generating ephemeris.
 * RXN_MSL_Abort(TRUE);
 * 
 * // Get assistance from the MSL.
 * Result = RXN_MSL_GetEphemeris(0xFFFFFFFF, &assistData, RXN_GPS_CONSTEL);
 * 
 * // Allow generating ephemeris.
 * RXN_MSL_Abort(FALSE);
 * \endcode
 */ 
U16 RXN_MSL_Abort(BOOL bAbort);

/** 
 * \brief 
 * Get extended ephemeris from the Mobile Suite Library.
 *
 * \param prnBitMask
 * [In] prnBitMask is a bitmask where bit0 is equal to PRN1, bit1 is PRN2 ...
 *
 * \param pAssist 
 * [In] A RXN_MSL_NavDataList_t structure that is populated with ephemeris data. 
 * 
 * \param constel
 * [In] The constellation to get seed information. Must be one of the types of RXN_constel.
 *
 * \return RXN_SUCCESS if assistance is acquired successfully.
 * \return RXN_FAIL if no assistance data could be returned.
 *
 * \details
 * <b>Description</b>\n
 * Use this function to acquire MSL assistance data.
 *
 * <b>See Also</b>\n
 * RXN_MSL_Abort()
 *
 * <b>Example Usage</b>
 * \code
 * // Declare a storage for assistance data.
 * RXN_MSL_NavDataList_t navData;
 *
 * memset((void*) &navData, 0, sizeof(RXN_MSL_NavDataList_t));
 *
 * // Abort generating ephemeris.
 * RXN_MSL_Abort(TRUE);
 * 
 * // Get assistance from the MSL.
 * Result = RXN_MSL_GetEphemeris(0xFFFFFFFF, &navData, RXN_GPS_CONSTEL);
 * 
 * // Allow generating ephemeris.
 * RXN_MSL_Abort(FALSE);
 * \endcode
 *
 * \see RXN_MSL_TriggerAssistance
*/ 
U16 RXN_MSL_GetEphemeris(U32 prnBitMask, RXN_MSL_NavDataList_t* pNavModelData, RXN_constel_t constel);

/** 
 * \brief 
 * Get assistance data from the Mobile Suite Library.
 *
 * \deprecated
 * This function has been deprecated and will be removed in a future release.  RXN_MSL_GetEphemeris() 
 * should be used instead.
 *
 * \see RXN_MSL_GetEphemeris
 *
*/ 
U16 RXN_MSL_GetAssistance(U32 prnBitMask, RXN_MSL_Assistance_t* pAssist, RXN_constel_t constel);

/** 
 * \brief 
 * Write an array of broadcast ephemeris data into the MSL.  It is preferable to use
 * RXN_MSL_WriteSingleEphemeris() instead so that it is possible to determine whether the
 * ephemeris for a specific PRN was written successfully.  
 *
 * \param PRNArr 
 * [In] An array of RXN_FullEph_t structs that will be populated with broadcast ephemeris
 * data.
 * 
 * \param ArrSize
 * [In] The number of array elements allocated in memory. 
 * 
 * \param constel
 * [In] The satellite constellation the ephemeris is for. Must be one of the types of RXN_constel.
 *
 * \return RXN_SUCCESS if all ephemeris is written successfully.
 * \return RXN_MSL_SET_EPHEMERIS_ERR if the data for one or more PRN's failed to write
 *
 * \details
 * <b>Description</b>\n
 * The autonomous mode functionality will consume PRN broadcast ephemeris
 * data records so that it may model PRN locations and generate EE for PRNs. Predicted GPS (PGPS)
 * functionality will consume broadcast ephemeris and update internal clocks using clock data
 * that is included within broadcast ephemeris data.  
 *
 * To enable optimal collections for autonomous mode this function should be called periodically 
 * at 5 minute intervals regardless of GPS fix status to ensure in a general use case of 20 
 * minutes it will make at least 3 collections. 
*/ 
U16 RXN_MSL_WriteEphemeris(RXN_FullEphem_u PRNArr, U08 ArrSize, RXN_constel_t constel);

/** 
 * \brief 
 * Write broadcast ephemeris data into the MSL. To enable optimal collections for autonomous mode
 * this function should be called periodically at 5 minute intervals regardless of GPS fix
 * status to ensure in a general use case of 20 minutes it will make at least 3 collections. 
 *
 * \param eph
 * [In] The ephemeris data to write
 * 
 * \param constel
 * [In] The satellite constellation the ephemeris is for. Must be one of the types of RXN_constel.
 *
 * \return RXN_SUCCESS if ephemeris is written successfully.,
 * \return RXN_FAIL for all errors.
 *
 * \details
 * <b>Description</b>\n
 * The autonomous mode functionality will consume PRN broadcast ephemeris
 * data records so that it may model PRN locations and generate EE for PRNs. Predicted GPS (PGPS)
 * functionality will consume broadcast ephemeris and update internal clocks using clock data
 * that is included within broadcast ephemeris data.
 *
 * <b>Example Usage</b>
 * \code
 * RXN_Ephemeris_u ephemeris;
 * 
 * // Loop through PRNs checking for BCE changes for each.
 * for(U08 Idx = 0; Idx < RXN_CONSTANT_NUM_PRNS; Idx++)
 * {
 *      // Get BCE from the receiver. The following example application would return
 *      // "TRUE"  if BCE had changed for the specified PRN (first param).
 *      if(GetBCE(Idx, &ephemeris) == TRUE)
 *      {
 *          // Records with readBCEArr[].prn == 0 will be ignored.
 *          if(ephemeris.gpsEphemeris.prn != 0) 
 *          {
 *              // Write ephemeris 
 *              RXN_MSL_WriteSingleEphemeris(&ephemeris, RXN_GPS_CONSTEL);
 *          }   
 *      }
 * }
 *      
 * \endcode
*/ 
U16 RXN_MSL_WriteSingleEphemeris(RXN_Ephemeris_u* eph, RXN_constel_t constel);

/** 
 * \brief 
 * Get the current GPS time.
 *
 * \return The number of seconds since GPS start.
 *
 * \details
 * <b>Description</b>\n
 * MSL implementation requires target-specific support for getting the 
 * current GPS time. This value may also be required outside the MSL
 * by integration applications. It is for the above reason that this
 * function is exposed. The best GPS time is determined by checking if
 * we have an accurate GPS time from the receiver. If not, we will try
 * SNTP time. We may not get any time update from the receiver and SNTP.
 * We will fall back to the most recent valid time. If the old GPS time
 * is not good to use, fall back to the old SNTP time. If we don't have
 * an accurate time, system time will be returned instead.
 *
 * <b>Example Usage</b>
 * \code
 * // Declare an integer to hold the current GPS time.
 * U32 GPSTime;
 * 
 * // Get the GPS time from the system.
 * GPSTime = RXN_MSL_GetBestGPSTime();
 * \endcode
 */ 
U32 RXN_MSL_GetBestGPSTime(void);

/** 
 * \brief 
 * Set the current GPS time.
 *
 * \param pRefTime
 * [In] The current GPS time in RXN_RefTime_t format
 *
 * \return RXN_SUCCESS if time has been set successfully (always).
 *
 * \attention Integrators have the responsibility to provide accurate time to the system.
 *
 * \details
 * <b>Description</b>\n
 * MSL implementation requires target-specific support for setting the
 * current GPS time. This value may also be required outside the MSL
 * by integration applications. It is for the above reason that this
 * function is exposed. 
 *
 * <b>Example Usage</b>
 * \code
 * // Declare an integer to hold the GPS time retrieved from receiver in seconds.
 * U32 actualGPSTime;
 *
 * // Declare a storage to hold the GPS time in RXN_RefTime_t format.
 * RXN_RefTime_t refTime;
 * 
 * // Get actual GPS time from receiver.
 * actualGPSTime = GetActualGPSTime();
 *
 * // Take the 1024 week roll over into account.
 * refTime.weekNum = (actualGPSTime / 604800) % 1024;
 * refTime.TOWmSec = (actualGPSTime % 604800) * 1000;
 * refTime.TAccmSec = 1;
 * 
 * // Set time from receiver.
 * RXN_MSL_SetGPSTime(&refTime);
 * \endcode
 */ 
U16 RXN_MSL_SetGPSTime(RXN_RefTime_t* pRefTime);

/**
 * \brief
 * Set the current GLONASS time.
 *
 * \param N4
 * [In] The GLONASS N4 parameter:  The number of 4 year periods since 1996.
 *
 * \param NT
 * [In] The GLONASS NT parameter:  The number of days elapsed in the current 4 year period
 *
 * \param tb
 * [In] The GLONASS tb parameter:  The number of 15 minutes periods elapsed in the current day
 *
 *
 * \return RXN_SUCCESS if time has been set successfully (always).
 *
 * \attention Integrators have the responsibility to provide accurate time to the system.
 *
 * \details
 * <b>Description</b>\n
 * This function takes the set of GLONASS time parameters and sets accurate GNSS time
 * within the MSL.
 *
 */ 
U16 RXN_MSL_SetGLOTime(U08 N4, U16 NT, U08 tb);

/**
 * \brief
 * Get the current GPS time derived from SNTP time.
 *
 * \param pRefTime
 * [Out] The current GPS time derived from SNTP time in RXN_RefTime_t format.
 *
 * \return RXN_SUCCESS if SNTP time is the current time source.
 * \return RXN_FAIL if current time source isn't SNTP.
 *
 * \details
 * <b>Description</b>\n
 * This function obtains the current time received from a SNTP server and returns it in RXN_RefTime_t format.
 *
 * <b>Example Usage</b>
 * \code
 * // Declare a storage to hold the current GPS time in RXN_RefTime_t format.
 * RXN_RefTime_t refTime;
 *
 * if(RXN_MSL_GetSNTPTime(&refTime) != RXN_SUCCESS)
 * {
 *     printf("The current time source isn't SNTP.\n");
 * }
 * \endcode
 */
U16 RXN_MSL_GetSNTPTime(RXN_RefTime_t* pRefTime);

/**
 * \brief
 * Sets the latest leap second information by supplying KP value from GLONASS almanac.
 *
 * \param kP
 * [In] The KP value obtained from GLONASS almanac. This value determines what happens at the end of the current quarter year (UTC).
 *
 *
 * \param N4
 * [In] The N4 value obtained from GLONASS almanac. This value is the number of leap years that have occurred since 1996.
 * N4 and NA combined are used to determine our current year.
 *
 * \param NA
 * [In] The NA value obtained from GLONASS almanac. This value is the number of days since the last leap year.
 * N4 and NA combined are used to determine our current year.
 *
 * \return RXN_SUCCESS if leap second information has been set successfully.
 * \return RXN_FAIL if the information was rejected.
 *
 * \details
 * <b>Description</b>\n
 * This function sets the leap seconds that will be used by the MSL to handle a leap second roll over, and
 * perform the appropriate adjusts when converting between time systems (UTC, GPS, GLONASS).
 *
 */
U16 RXN_MSL_SetLeapSecondsInfoKP(U08 kP, U08 N4, U16 NA);


/**
 * \brief
 * Sets the latest leap second information.
 *
 * \param curLeapSecVal
 * [In] The current leap second value.
 *
 * \param nextLeapSecVal
 * [In] Currently this parameter only supports a value of curLeapSecVal or curLeapSecVal + 1. [Provisioned for future use. Current PGPS 6.x core supports +1 second
 * leap seconds only.]
 *
 * \param nextLeapSecGPStime
 * [In] When the next leap second will occur, in GPS seconds.
 *
 * \return RXN_SUCCESS if leap second information has been set successfully.
 * \return RXN_FAIL if the information was rejected.
 *
 * \details
 * <b>Description</b>\n
 * This function sets the leap seconds that will be used by the MSL to handle a leap second roll over, and
 * perform the appropriate adjusts when converting between time systems (UTC, GPS, GLONASS).
 *
 */
U16 RXN_MSL_SetLeapSecondsInfo(U08 curLeapSecVal, U08 nextLeapSecVal, U32 nextLeapSecGPStime);

/*
 * This function returns the uncertainty of the SNTP time in milliseconds,
 * it effectively adds 1 second of uncertainty per 8 hours.
 */
U32 MSL_GetSNTPTimeUncert(void);

/*
 * This function returns the uncertainty of accurate GPS time in milliseconds,
 * it effectively adds 1 second of uncertainty per 8 hours.
 */
U32 MSL_GetAccGPSTimeUncert(void);

/*
 * This converts UTC to GPS time and also adds the UTC / GPS offset.
 */
U32 MSL_ConvertUTCToGPSTime(time_t tTime);

/*
 * This converts UTC to Glonass time.
 */
U32 MSL_ConvertUTCToGLOTime(time_t tTime);

/*
 * This converts GPS time to Glonass time.
 */
U32 MSL_ConvertGPSTimeToGLOTime(U32 currentGPSTime);

/*
 * This converts Glonass time to GPS time.
 */
U32 MSL_ConvertGLOTimeToGPSTime(U32 gloSec);

/*
 * This returns the current GPS leap seconds value.
 */
U32 MSL_GetGPSLeapSec();

/** 
 * \brief 
 * Delete all pantry data from file system.
 *
 * \param resetType
 * [In] The enumeration of reset type.
 *
 * \details
 * <b>Description</b>\n
 * This will remove all data from eph and pred folders and/or reset time.
 *
 * <b>Example Usage</b>
 * \code
 * // Delete all pantry data from file system and reset time.
 * RXN_MSL_Factory_Reset(MSL_RESET_SEED_AND_TIME);
 * \endcode
 */
void RXN_MSL_Factory_Reset(enum MSL_Reset_Type resetType);

/*
 * This will clear all time parameters sent by the receiver or obtained using SNTP.
 */
void MSL_Time_Reset();


/** 
 * \brief 
 * Enable or disable download of data.
 *
 * \param isEnabled
 * [In] TRUE to enable data access or FALSE to disable data access.
 *
 * \details
 * <b>Description</b>\n
 * This function is used to control data access. If isEnabled is set to TRUE,
 * MSL is allowed to download data. If isEnabled is set to FALSE, all seed download
 * requests and SNTP requests will be ignored.
 *
 * <b>Example Usage</b>
 * \code
 * if(dataEnabled > 0)
 * {
 *     RXN_MSL_SetDataAccess(TRUE);
 * }
 * else
 * {
 *     RXN_MSL_SetDataAccess(FALSE);
 * }
 * \endcode
 */
void RXN_MSL_SetDataAccess(BOOL isEnabled);

/** 
 * \brief 
 * Get the setting of data access.
 *
 * \return MSL_DATA_ACCESS_DISABLED if the system disallows for download of data.
 * \return MSL_DATA_ACCESS_ENABLED if the system disallows for download of data.
 * \return MSL_DATA_ACCESS_UNDEFINED if the data access has not been set yet.
 *
 * \details
 * <b>Description</b>\n
 * This function is used to get the setting of data access. If gMSL_Respect_Data_Settings
 * is set to TRUE then we will smartly determine if the system allows for download of data.
 * If gMSL_Respect_Data_Settings is set to FALSE we will blindly allow for data.
 *
 * <b>Example Usage</b>
 * \code
 * if(RXN_MSL_GetDataAccess() != MSL_DATA_ACCESS_ENABLED)
 * {
 *     printf("Network access not available or allowed.\n");
 * }
 * \endcode
 */
U08 RXN_MSL_GetDataAccess();

/** 
* \brief 
* This function supports importing ephemeris data from a Rinex file.
* 
* \param rinexFile
* [In] Specifies the path to a Rinex file. 
*
* \param constel
* [In] The constellation to process a Rinex file. Must be one of the types of RXN_constel.
*
* \return RXN_SUCCESS if ephemeris is imported successfully.
* \return RXN_FAIL for any errors.
*
* \details
* <b>Description</b>\n
* Use this function to process a Rinex file and write ephemeris to the eph folder.
* This function processes one file only. Call this function multiple times if you have 
* more than one Rinex files to process. Rinex files must be processed in chronological order.
*
* <b>Example Usage</b>
* \code
* #define RINEX_FILE       "/data/rinex/rinex_file"
*
* // Process a GPS Rinex file.
* Result = RXN_MSL_ProcessRinexFile(RINEX_FILE, RXN_GPS_CONSTEL);
* 
* \endcode
*/ 
U08 RXN_MSL_ProcessRinexFile(const char rinexFile[RXN_MSL_MAX_PATH], RXN_constel_t constel);

/*** Asynchronous assistance interface ***/

/**
 * \brief Functions of this format can be added as EE and BCE observers.
 * \see RXN_MSL_AddEEObserver
 * \see RXN_MSL_AddBCEObserver
 */
typedef void (*RXN_MSL_Ephemeris_Callback)(RXN_MSL_NavDataList_t* ephemeris, RXN_constel_t constel);

/**
 * \brief Functions of this format can be added as location observers.
 * \see RXN_MSL_AddLocationObserver
 */
typedef void (*RXN_MSL_Location_Callback)(RXN_RefLocation_t* location);

/**
 * \brief Functions of this format can be added as finished assistance observers.
 * \see RXN_MSL_AddFinishedAssistanceObserver
 */
typedef void (*RXN_MSL_FinishedAssistance_Callback)();

/**
  * \brief Adds an observer that is notified when EE is available.
  */
void RXN_MSL_AddEEObserver(RXN_MSL_Ephemeris_Callback callback);

/**
  * \brief Removes an observer from EE notifications
  */
void RXN_MSL_RemoveEEObserver(RXN_MSL_Ephemeris_Callback callback);

/**
  * \brief Adds an observer that is notified when reference position is available.
  */
void RXN_MSL_AddLocationObserver(RXN_MSL_Location_Callback callback);

/**
  * \brief Removes an observer from reference position notifications
  */
void RXN_MSL_RemoveLocationObserver(RXN_MSL_Location_Callback callback);

/**
  * \brief Adds an observer that is notified when BCE is available.
  */
void RXN_MSL_AddBCEObserver(RXN_MSL_Ephemeris_Callback callback);

/**
  * \brief Removes an observer from BCE notifications
  */
void RXN_MSL_RemoveBCEObserver(RXN_MSL_Ephemeris_Callback callback);

/**
  * \brief Adds an observer that is notified when all assistance has been provided.
  */
void RXN_MSL_AddFinishedAssistanceObserver(RXN_MSL_FinishedAssistance_Callback callback);

/**
  * \brief Removes an observer from finished assistance notifications.
  */
void RXN_MSL_RemoveFinishedAssistanceObserver(RXN_MSL_FinishedAssistance_Callback callback);

/**
 * \brief
 * This enumeration defines the different types of assistance provided by the MSL.  These
 * bitflags can be OR'd together.
 *
 * \see RXN_MSL_TriggerAssistance
 */
enum RXN_MSL_Assistance_Type 
{
    RXN_MSL_ASSISTANCE_NONE = 0,
    RXN_MSL_ASSISTANCE_PGPS = 1,
    RXN_MSL_ASSISTANCE_XBD_RT = 1 << 1,
    RXN_MSL_ASSISTANCE_XBD_BCE = 1 << 2,
    RXN_MSL_ASSISTANCE_SYNCHRO = 1 << 3,
    RXN_MSL_ASSISTANCE_REF_TIME  = 1 << 4, 
    RXN_MSL_ASSISTANCE_ALL = 0xFFFFFFFF
};

/**
 * \brief
 * This function starts an asynchronous assistance request.  In order to be notified when
 * assistance is available, register as an observer using the observer interface.
 *
 * \see RXN_MSL_AddEEObserver
 * \see RXN_MSL_AddLocationObserver
 * \see RXN_MSL_AddBCEObserver
 *
 * \param assistTypeBitMask
 * [IN] Use the bit flags defined in RXN_MSL_Assistance_Type to select which products will
 * supply assistance.
 *
 * \param gpsPrnBitMask
 * [IN] Select the GPS prn's for which assistance is provided using this bitmask where bit0 
 * is equal to PRN1, bit1 is PRN2 ...
 *
 * \param gloSlotBitMask
 * [IN] Select the GLONASS prn's for which assistance is provided using this bitmask where bit0 
 * is equal to PRN1, bit1 is PRN2 ...
 *
 * \details
 * <b>Description</b>\n
 * RXN_MSL_TriggerAssistance starts acquiring assistance from the products requested in
 * assistTypeBitMask.  As assistance becomes available from each product, the assistance is
 * provided to EE, location, and BCE observers.
 *
 * <b>Example Usage</b>\n
 * \code
 * static void ephCallback(RXN_MSL_NavDataList_t* ephemeris, RXN_constel_t constel)
 * {
 *     RXN_CL_WriteEphemeris(mCL_Handle, ephemeris, constel);
 * }
 * 
 * static void locationCallback(RXN_RefLocation_t* location)
 * {
 *     RXN_CL_SetRefLocTime(mCL_Handle, location, NULL);
 * }
 *
 * ...
 * void init()
 * {
 *     RXN_MSL_AddEEObserver(ephCallback);
 *     RXN_MSL_AddBCEObserver(ephCallback);
 *     RXN_MSL_AddLocationObserver(locationCallback);
 * }
 *
 * void getAssistance()
 * {
 *     RXN_MSL_TriggerAssistance(RXN_MSL_ASSISTANCE_ALL, 0xFFFFFFFF, 0x00FFFFFF);
 * }
 * \endcode
 *
 */
void RXN_MSL_TriggerAssistance(U32 assistTypeBitMask, U32 gpsPrnBitMask, U32 gloSlotBitMask);


#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* RXN_MSL_H */
