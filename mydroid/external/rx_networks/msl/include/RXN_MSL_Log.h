/*
 * Copyright (c) 2011 Rx Networks, Inc. All rights reserved.
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
 * $LastChangedDate: 2009-04-23 16:16:15 -0700 (Thu, 23 Apr 2009) $
 * $Revision: 10337 $
 *************************************************************************
 *
 */

#ifndef RXN_MSL_LOG_H
#define RXN_MSL_LOG_H

#ifdef __cplusplus
extern "C" {
#endif

#include "RXN_data_types.h"

/**
 *\brief
 * Enumeration of log entry severity levels - FOR INTERNAL RXN USE ONLY.
 */
enum RXN_LogSeverity {
  RXN_LOG_SEV_FATAL = 0,  /*!< Unrecoverable failure from which system is in indeterminate state, unwise to proceed. */
  RXN_LOG_SEV_ERROR,      /*!< Recoverable error, program can continue. */
  RXN_LOG_SEV_WARNING,    /*!< Not as serious as errors. */
  RXN_LOG_SEV_INFO,       /*!< Useful data regarding flow of the code. */
  RXN_LOG_SEV_TRACE       /*!< Supports RxN internal debugging only. */
};

/**
 *\brief
 * Enumeration of log entry zones - FOR INTERNAL RXN USE ONLY.
 */
enum RXN_LogZone {
  RXN_LOG_ZONE01 =  0x0001,  /*!< Misc functions (catch all). */
  RXN_LOG_ZONE02 =  0x0002,  /*!< Third Party log entry. */
  RXN_LOG_ZONE03 =  0x0004,  /*!< Init/Shutdown functions. */
  RXN_LOG_ZONE04 =  0x0008,  /*!< Serial, Socket and other I/O functions. */
  RXN_LOG_ZONE05 =  0x0010,  /*!< File I/O functions. */
  RXN_LOG_ZONE06 =  0x0020,  /*!< Security functions. */
  RXN_LOG_ZONE07 =  0x0040,  /*!< SAGPS/PGPS Debug Info I/O. */
  RXN_LOG_ZONE08 =  0x0080,  /*!< MSL Debug Info. */
  RXN_LOG_ZONE09 =  0x0100,  /*!< Xybrid */
  RXN_LOG_ZONE10 =  0x0200,  /*!< Reserved for RxN Internal. */
  RXN_LOG_ZONE11 =  0x0400,  /*!< Reserved for RxN Internal. */
  RXN_LOG_ZONE12 =  0x0800,  /*!< Reserved for RxN Internal. */
  RXN_LOG_ZONE13 =  0x1000,  /*!< Reserved for RxN Internal. */
  RXN_LOG_ZONE14 =  0x2000,  /*!< Reserved for RxN Internal. */
  RXN_LOG_ZONE15 =  0x4000,  /*!< Reserved for RxN Internal. */
  RXN_LOG_ZONE16 =  0x8000,  /*!< Reserved for RxN Internal. */
};

// Zone 1 (0x0001) � Misc functions (catch all).
// Zone 2 (0x0002) � Third Party log entry.
// Zone 3 (0x0004) � Init/Shutdown functions.
// Zone 4 (0x0008) � Serial Port I/O functions.
// Zone 5 (0x0010) � File I/O functions.
// Zones 6 - 15 - Reserved for RxN Internal Use.

#define MSL_MAX_LOG_ENTRY             1024  /* Log entry max chars. */
#define MSL_SHORT_LOG_ENTRY           512
#define MSL_MAX_LOG_ELEMENT           64    /* Log entry element max chars. */
#define MSL_LOG_SPACE_STR_LEN          1    /* Length of " "*/
#define MSL_LOG_END_LINE_STR_LEN       2    /* Length of \n*/
#define MSL_LOG_START_LINE_STR_LEN    MSL_MAX_LOG_TIME_ELEMENT+MSL_LOG_SPACE_STR_LEN+MSL_MAX_LOG_TIME_ELEMENT
#define MSL_MAX_FORMATTED_LOG_ENTRY   MSL_LOG_START_LINE_STR_LEN+MSL_MAX_LOG_ENTRY+MSL_LOG_END_LINE_STR_LEN
#define MSL_MAX_FULL_LOG_ENTRY        MSL_MAX_FORMATTED_LOG_ENTRY+MSL_MAX_LOG_ELEMENT+MSL_MAX_LOG_ELEMENT
#define MSL_LOG_SIZE_MIN              1
#define MSL_LOG_SIZE_MAX              1048576
#define MSL_LOG_SEVERITY_MIN          0
#define MSL_LOG_SEVERITY_MAX          4

/* Opens the log file if logging is enabled and performs other necessary
 * initialization. */
U16 MSL_Log_Init();

/* Closes the log file if opened and performs other neccessary shutdown */
void MSL_Log_UnInit();

/* Accessors */
void MSL_LogEnable(BOOL enable);
void MSL_LogSetMaxFilesize(U32 filesize);
U32 MSL_LogGetMaxFilesize();
void MSL_LogSetSevThreshold(U08 threshold);
U08 MSL_LogGetSevThreshold();
void MSL_LogSetZoneMask(U16 zoneMask);
U16 MSL_LogGetZoneMask();
void MSL_LogSetLogPath(char* logPath, U16 size);
const char* MSL_LogGetLogPath();


/* Simple log function that only writes the entry provided. *
 * (useful for open and close msgs).
 * Note that the time and '\n' termination will always be provided. */
void MSL_LogSimple(const char logEntry[MSL_MAX_LOG_ENTRY]);

/* Log an entry with no format elements.
 * severity should be a RXN_LogSeverity enumerated value.
 * zone should be a RXN_LogZone enumerated value.*/
void MSL_Log(BOOL includeTime, BOOL includeEndline, U08 severity, U16 zone,
			 const char logEntry[MSL_MAX_LOG_ENTRY]);

/* Log an entry with a single string within format string.
 * ("..." argument list may not be supported on some targets.) 
 * logFmtEntry MUST contain one '%s'. Eg "Logging string 1: %s".
 * severity should be a RXN_LogSeverity enumerated value.
 * zone should be a RXN_LogZone enumerated value.*/
void MSL_LogStr(BOOL includeTime, BOOL includeEndline, U08 severity, U16 zone,
				const char logFmtEntry[MSL_MAX_LOG_ENTRY], 
				const char logElement1[MSL_MAX_LOG_ELEMENT]);

/* Log an entry with two strings within format string.
 * ("..." argument list may not be supported on some targets.)
 * logFmtEntry MUST contain two '%s' chars. Eg "Logging string 1: %s and string 2: %s".
 * severity should be a RXN_LogSeverity enumerated value.
 * zone should be a RXN_LogZone enumerated value.*/
void MSL_LogStrStr(BOOL includeTime, BOOL includeEndline,
				   U08 severity, U16 zone,
				   const char logFmtEntry[MSL_MAX_LOG_ENTRY],
				   const char logElement1[MSL_MAX_LOG_ELEMENT],
				   const char logElement2[MSL_MAX_LOG_ELEMENT]);

/* Log an entry with a single float within format string.
 * ("..." argument list may not be supported on some targets.)
 * logFmtEntry MUST contain one '%f'. Eg "Logging float 1: %2.5f"
 * severity should be a RXN_LogSeverity enumerated value.
 * zone should be a RXN_LogZone enumerated value.*/
void MSL_LogFlt(BOOL includeTime, BOOL includeEndline, U08 severity, U16 zone,
		const char logFmtEntry[MSL_MAX_LOG_ENTRY], double logElement1);

/* Log an entry with two floats within format string.
 * ("..." argument list may not be supported on some targets.)
 * logFmtEntry MUST contain two '%f' chars. Eg "Logging float 1: %2.5f and float 2: %0.2f"
 * severity should be a RXN_LogSeverity enumerated value.
 * zone should be a RXN_LogZone enumerated value.*/
void MSL_LogFltFlt(BOOL includeTime, BOOL includeEndline, U08 severity, U16 zone,
				   const char logFmtEntry[MSL_MAX_LOG_ENTRY],
				   double logElement1, double logElement2);

/* Log to output a one line entry indicating the GPS time in GPS seconds
 * as well as GPS week | TOW format */
void MSL_LogGPSTime(char logEntry[MSL_SHORT_LOG_ENTRY], U32 gpsTime);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* RXN_MSL_LOG_H */
