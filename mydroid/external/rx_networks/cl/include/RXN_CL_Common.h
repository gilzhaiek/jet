/*
 * Copyright (c) 2007-2011 Rx Networks, Inc. All rights reserved.
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
 * $LastChangedDate: 2009-06-25 13:17:20 -0700 (Thu, 25 Jun 2009) $
 * $Revision: 11688 $
 *************************************************************************
 *
 */

/*
 * Contains common elements that are not to be exposed to CL consumers.
 */ 

#ifndef RXN_CL_COMMON_H
#define RXN_CL_COMMON_H

#ifdef __cplusplus
extern "C" {
#endif

#include <time.h>             /* Required for time funcs such as gmtime, etc. */
//#include <memory.h>           /* Required for memset, memcpy, etc. */
#include <math.h>             /* Required for math funcs such as pow. */
#include <string.h>           /* Required for string funcs such as strlen. */
#include <stdlib.h>           /* Required for ANSI-C funcs such as atoi. */
#include <stdio.h>            /* Required for ANSI-C file I/O. */
#include "RXN_CL.h"           /* Has exposed APIs for standard CL chipsets. */
#include "RXN_CL_RX.h"        /* Has exposed APIs for the RINEX chipset. */
#include "RXN_MSL.h"          /* Includes structs, constants and data types used by CL, RL and MSL. */

/* The max number of chars in a RINEX file line. */ 
#define CL_MAX_FILE_LINE_LEN      1024

/* The max number of CL instances supported. */ 
#define CL_MAX_INSTANCES          10

/* The max chars to support timestamps in the log. */ 
#define CL_MAX_LOG_TIME_STR_LEN   64

/* The max chars to support an entire log file entry. */ 
#define CL_MAX_LOG_ENTRY          1024

/* The max chars to support a single element within a log file entry. */
#define CL_MAX_LOG_ELEMENT        32

/* The max chars to store paths. */ 
#define CL_PATH_MAX_STR_LEN       224

/* The max chars to store the config string. */ 
#define CL_MODE_MAX_STR_LEN       30

/* The max chars to store a baudrate. */ 
#define CL_PORT_BAUD_MAX_STR_LEN  32

/* The number of minutes to wait for BCE before EE becomes stale. */
#define CL_MAX_EE_MIN             4

/* Number of seconds between Jan 1, 1970. and Jan 1 1996 minus 3 hrs*/
#define GPS_GLO_OFFSET 504478800

/* Structure defining values returned from chipset initializations that
 * are required for chipset interaction later. */ 
typedef struct CL_Chipset_Attrib
{
  FILE* CL_pFile;     /* Stores a persisted file stream pointer as required by RINEX file I/O. */
  void* CL_Port_Hdl;  /* Stores a port handle (Win32 - specific). */
  int CL_FD;          /* Stores a port handle (Linux - specific). */
  void* CL_ScktIn;    /* Stores a sckt for incomming msgs. */
  void* CL_ScktOut;   /* Stores a sckt for outgoing msgs. */
} CL_Chipset_Attrib_t;

/* Function prototypes. Each chipset implements some or all of the functions below.
 * Prototypes ensure that each chipset supports a common interface to RXN_CL_Common functions. */
typedef U16 (*CD_Initialize)(CL_Chipset_Attrib_t* pAttrib, U16 chipsetVer, char config[RXN_CL_CONFIG_MAX_STR_LEN]);
typedef U16 (*CD_Uninitialize)(CL_Chipset_Attrib_t* pAttrib);
typedef U16 (*CD_GetChipsetSupportVersion)(CL_Chipset_Attrib_t* pAttrib, char version[RXN_CONSTANT_VERSION_STRING_LENGTH]);
typedef U16 (*CD_Work)(CL_Chipset_Attrib_t* pAttrib);
typedef U16 (*CD_Restart)(CL_Chipset_Attrib_t* pAttrib, RXN_CL_Restarts_t restartType,
                          RXN_RefLocation_t* pRefLoc, RXN_RefTime_t* pRefTime);
typedef U16 (*CD_SetRefLocTime)(CL_Chipset_Attrib_t* pAttrib, RXN_RefLocation_t* pRefLoc, RXN_RefTime_t* pRefTime);
typedef U16 (*CD_ReadEphemeris)(CL_Chipset_Attrib_t* pAttrib, RXN_MSL_NavDataList_t* pNavDataList, RXN_constel_t constel);
typedef U16 (*CD_WriteEphemeris)(CL_Chipset_Attrib_t* pAttrib, RXN_MSL_NavDataList_t* pNavDataList, RXN_constel_t constel);
typedef U16 (*CD_GetFixData)(CL_Chipset_Attrib_t* pAttrib, RXN_CL_FixData_t* pFixData);
typedef U16 (*CD_GetGPSRcvrTime)(CL_Chipset_Attrib_t* pAttrib, U16* pRcvrWeekNum, U32* pRcvrTOW);

/* Declarations matching the above prototypes are below. These functions will be implemented
 * within chipset-specifc code. */
U16 CL_Initialize(CL_Chipset_Attrib_t* pAttrib, U16 chipsetVer, char config[RXN_CL_CONFIG_MAX_STR_LEN]);
U16 CL_Uninitialize(CL_Chipset_Attrib_t* pAttrib);
U16 CL_GetChipsetSupportVersion(CL_Chipset_Attrib_t* pAttrib, char version[RXN_CONSTANT_VERSION_STRING_LENGTH]);
U16 CL_Work(CL_Chipset_Attrib_t* pAttrib);
U16 CL_Restart(CL_Chipset_Attrib_t* pAttrib, RXN_CL_Restarts_t restartType, RXN_RefLocation_t* pRefLoc, RXN_RefTime_t* pRefTime);
U16 CL_SetRefLocTime(CL_Chipset_Attrib_t* pAttrib, RXN_RefLocation_t* pRefLoc, RXN_RefTime_t* pRefTime);
U16 CL_ReadEphemeris(CL_Chipset_Attrib_t* pAttrib, RXN_MSL_NavDataList_t* pNavDataList, RXN_constel_t constel);
U16 CL_WriteEphemeris(CL_Chipset_Attrib_t* pAttrib, RXN_MSL_NavDataList_t* pNavDataList, RXN_constel_t constel);
U16 CL_GetFixData(CL_Chipset_Attrib_t* pAttrib, RXN_CL_FixData_t* pFixData);
U16 CL_GetGPSRcvrTime(CL_Chipset_Attrib_t* pAttrib, U16* pRcvrWeekNum, U32* pRcvrTOW);

U16 CL_ReadEphemerisGLO(CL_Chipset_Attrib_t* pAttrib, RXN_FullEph_GLO_t* PRNArr, U08 ArrSize);
U16 CL_WriteEphemerisGLO(CL_Chipset_Attrib_t* pAttrib, RXN_FullEph_GLO_t* PRNArr, U08 ArrSize);


/* Structure that supports storage of each CL implementations functions or NULL 
 * for any functions not supported by a CL implementation. */
typedef struct CL_Chipset_Fcn_Table
{
    CD_Initialize               Initialize;
    CD_Uninitialize             Uninitialize;
    CD_GetChipsetSupportVersion GetChipsetSupportVersion;
    CD_Work                     Work;
    CD_Restart                  Restart;
    CD_SetRefLocTime            SetRefPosTime;
    CD_ReadEphemeris            ReadEphemeris;
    CD_WriteEphemeris           WriteEphemeris;
    CD_GetFixData               GetFixData;
    CD_GetGPSRcvrTime           GetGPSRcvrTime;
} CL_Chipset_Fcn_Table_t;

/* Struct that describes local or GMT/UTC time. */
typedef struct CL_time
{
  U16 CL_Year;      /*!< Stores the current Year (e.g. 2008). */
  U08 CL_Month;     /*!< Stores the current Month (1-12). */
  U08 CL_Day;       /*!< Stores the current Day (1-31). */
  U08 CL_Hour;      /*!< Stores the current Hour (0-23). */
  U08 CL_Min;       /*!< Stores the current Min (0-59). */
  U08 CL_Sec;       /*!< Stores the current Sec (0-59). */
  U16 CL_mSec;      /*!< Stores the current mSec (0-999). */
} CL_time_t;  

/**
 * \brief
 * Struct that describes each instance..
 *
 * \details
 * This struct contains info describing an instance of the CL. It includes vars that
 * can be used to store data between calls such as a port for file handle.
 */
typedef struct CL_instance
{
  U16 CL_Handle;                    /*!< Stores a CL instance handle for use in CL calls. */
  CL_Chipset_Attrib_t CL_Attrib;    /*!< Stores CL specific vars set during init. */
  CL_Chipset_Fcn_Table_t CL_Fcns;   /*!< Stores CL implementation function pointers. */
  S32 CL_Var1;                      /*!< Stores a persisted var that can be use to store instance data (future).*/
} CL_instance_t;

/**********************************************************************************
 * Required Common Inclusions                                                     *
 **********************************************************************************/

#include "RXN_CL_Protocol.h"  /* Protocol Support. */
#include "RXN_CL_Data.h"      /* Data Manipulation Support. */

/************************************************************************************
 * Platform Optional Inclusions                                                     *
 ************************************************************************************/

#if defined (WIN32) || defined (WINCE) || defined (LINUX)
#include "RXN_CL_Platform.h"
#endif /* #if defined (WIN32) || defined (WINCE) */

/************************************************************************************
 * Chipset Inclusions                                                               *
 *                                                                                  *
 * The following functions must be implemented for the chipset supported within the *
 * current project.                                                                 *
 ************************************************************************************/

/* Retrieve a chipset-specific label. */
U16 CL_GetChipset(char chipset[RXN_CL_CHIP_MAX_STR_LEN]);

/* Set chipset-specific function pointers. */
U16 CL_SetFcns(CL_instance_t* pInstance);

/***************************
 * Utility Functions Below *
 ***************************/

/* Gets the chipset instance index within gCL_Instances using
 * a handle. Returns 255 if the handle can't be found. */
U08 CL_FindHandle(U16 handle);

/* Determine what severity threashold and zone mask has been provided 
 * within the config string. */
void CL_ProcessLogParams(char config[RXN_CL_CONFIG_MAX_STR_LEN]);

/* Get an integer parameter from within the config sentence. */
U16 CL_GetParamInt(char config[RXN_CL_CONFIG_MAX_STR_LEN],
                   char label[CL_MAX_LOG_ELEMENT], int base, S64* pValue);

/* Get an string parameter from within the config sentence. */
U16 CL_GetParamString(const char config[RXN_CL_CONFIG_MAX_STR_LEN],
                   const char label[CL_MAX_LOG_ELEMENT], char param[RXN_CL_CONFIG_MAX_STR_LEN]);

U16 CL_PersistClkOffset(S32 offset);

U16 CL_RetrieveClkOffset(S32* pOffset);

/* 
 * \brief 
 * Open a file for reads or writes.
 * 
 * \param pCLInstance 
 * [In] Pointer to a CL instance through which the file will be opened.
 * \param config 
 * [In] An allocated string with the following format:
 * "<path>|<mode>" where <path> denotes the full path to the file and 
 * <mode> is either "r" to support file reads or "w" to support file writes. 
 * WinXP Eg. "C:\\RINEXFile.bin|w". Further not that <path> formats can be O/S specific.
 * WinCE Eg. "\\RINEXFolder\\RINEXFile.bin|r".
 *
 * \returns 
 * RXN_SUCCESS if the file is opened successfully, 
 * RXN_CL_CONFIG_FORMAT_ERR if the config string is malformed (e.g. does not include '|'),
 * RXN_CL_OPEN_OR_CLOSE_ERR if the file could not be opened.
*/ 
U16 CL_OpenFile(FILE** ppFile, const char config[RXN_CL_CONFIG_MAX_STR_LEN]);

/* 
 * \brief 
 * Close a file.
 * 
 * \param pCLInstance 
 * [In] Pointer to a CL instance through which the file was opened.
 *
 * \returns 
 * RXN_SUCCESS if the file is closed successfully, 
 * RXN_CL_OPEN_OR_CLOSE_ERR if the file could not be closed.
*/ 
U16 CL_CloseFile(FILE** ppFile);

/* 
 * \brief 
 * Read a line from an open file.
 * 
 * \param pCLInstance 
 * [In] Pointer to a CL instance through which a file has been opened.
 * \param fileLine 
 * [Out] A char array that will be populated with the next available line from the file
 * (stipulated by the file pointer).
 *
 * \returns 
 * RXN_SUCCESS if the line was read successfully, 
 * RXN_CL_CHIPSET_AT_EOF if the file end was reached,
 * RXN_CL_CHIPSET_READ_ERR if an error occured when trying to read the line.
*/ 
U16 CL_ReadLine(CL_Chipset_Attrib_t* pAttrib, char fileLine[CL_MAX_FILE_LINE_LEN]);

/* 
 * \brief 
 * Write a line to an open file.
 * 
 * \param pCLInstance 
 * [In] Pointer to a CL instance through which a file has been opened.
 * \param fileLine 
 * [In] A char populated char array that is written into the file at its current location
 * (stipulated by the file pointer).
 *
 * \returns 
 * RXN_SUCCESS if the line was written successfully, 
 * RXN_CL_CHIPSET_WRITE_ERR if an error occured when trying to write the line.
*/ 
U16 CL_WriteLine(CL_Chipset_Attrib_t* pAttrib, char fileLine[CL_MAX_FILE_LINE_LEN]);

U16 CL_ResetFilePtr(CL_Chipset_Attrib_t* pAttrib);

U16 CL_OpenLog(const char logFile[RXN_CL_MAX_LOG_PATH]);

void CL_CloseLog(void);

void CL_LogSimple(const char logEntry[CL_MAX_LOG_ENTRY]);

void CL_Log(BOOL includeTime, BOOL includeEndline, U08 severity, U16 zone,
                const char logEntry[CL_MAX_LOG_ENTRY]);

void CL_LogStr(BOOL includeTime, BOOL includeEndline, U08 severity, U16 zone,
                const char logFmtEntry[CL_MAX_LOG_ENTRY], const char logElement1[CL_MAX_LOG_ELEMENT]);

void CL_LogStrStr(BOOL includeTime, BOOL includeEndline,
                  U08 severity, U16 zone,
                  const char logFmtEntry[CL_MAX_LOG_ENTRY],
                  const char logElement1[CL_MAX_LOG_ELEMENT],
                  const char logElement2[CL_MAX_LOG_ELEMENT]);

void CL_LogFlt(BOOL includeTime, BOOL includeEndline, U08 severity, U16 zone,
                const char logFmtEntry[CL_MAX_LOG_ENTRY], double logElement1);

void CL_LogFltFlt(BOOL includeTime, BOOL includeEndline, U08 severity, U16 zone,
                const char logFmtEntry[CL_MAX_LOG_ENTRY],
                double logElement1, double logElement2);

void CL_LogFltFltFlt(BOOL includeTime, BOOL includeEndline, U08 severity, U16 zone,
                const char logFmtEntry[CL_MAX_LOG_ENTRY],
                double logElement1, double logElement2, double logElement3);

void CL_ConvertECEFToLLA(RXN_ECEF_LOC_t ECEFIn, RXN_LLA_LOC_t* pLLAOut);

void CL_ConvertLLAToECEF(RXN_LLA_LOC_t LLAIn, RXN_ECEF_LOC_t* pECEFOut);

/* Validate if a log entry is required and prepare a log entry prefix. */
U16 CL_ProcessSevZone(U08 severity, U16 zone, char prefixEntry[CL_MAX_LOG_TIME_STR_LEN]);

/* Setup the time element within a log entry. */
void CL_SetupEntryTime(char timeEntry[CL_MAX_LOG_TIME_STR_LEN]);

/* Setup a format string for logging. */
void CL_SetupFullFmtString(char fullFmtEntry[CL_MAX_LOG_ENTRY],
                   BOOL includeTime, BOOL includeEndline,
                   char prefixEntry[CL_MAX_LOG_TIME_STR_LEN],
                   const char logFmtEntry[CL_MAX_LOG_ENTRY]);


/* Return a crude URE in meters (corresponding to max URA index 9).
 * If any URAIdx value > 9 provided, 255 will be returned. */
U08 CL_UREfromURA(U08 URAIdx);

/* Return the URA index corresponding to the URE. URE limited to
 * 255 or less. */
U08 CL_URAfromURE(U08 URE);

/* Get GPS time values (Year, month, ...min, sec). */
void CL_GetUTCTime(CL_time_t* pTime, U32 gpsSeconds,U16 leapSeconds);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* RXN_CL_COMMON_H */
