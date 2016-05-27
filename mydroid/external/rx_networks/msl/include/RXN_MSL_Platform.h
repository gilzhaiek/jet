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
 * $LastChangedDate: 2009-03-02 15:53:19 -0800 (Mon, 02 Mar 2009) $
 * $Revision: 9396 $
 *************************************************************************
 *
 */

#ifndef RXN_MSL_FILE_PLATFORM_H
#define RXN_MSL_FILE_PLATFORM_H

/* Note: File critical sections are also used as file handle indices, so they
   must come first. */
enum MSL_CriticalSections
{
  /* Ephemeris Files */
  RXN_MSL_CS_EPH_FILE = 0,
  RXN_MSL_CS_EPH_FILE_LAST = 
    RXN_MSL_CS_EPH_FILE + RXN_CONSTANT_NUM_GPS_PRNS + RXN_CONSTANT_NUM_GLONASS_PRNS,

  /* Poly files */
  RXN_MSL_CS_POLY_FILE,
  RXN_MSL_CS_POLY_FILE_LAST =
    RXN_MSL_CS_POLY_FILE + RXN_CONSTANT_NUM_GPS_PRNS + RXN_CONSTANT_NUM_GLONASS_PRNS,

  RXN_MSL_CS_Base,
  RXN_MSL_CS_Time,
  RXN_MSL_CS_SM,

#ifndef _MSC_VER
  RXN_MSL_CS_GetTick,
#endif

#ifdef XYBRID
  RXN_MSL_CS_Xybrid,
#endif

  RXN_MSL_CS_Observer,

  RXN_MSL_MAX_CS 
};

#if _MSC_VER
#include "RXN_MSL_Windows.h"
#else
#include "RXN_MSL_Linux.h"
#endif

#ifdef __cplusplus
extern "C" {
#endif

/****************************
 * Internal MSL Structures. *
 ****************************/

/* Struct that describes local or GMT/UTC time. */
typedef struct MSL_time
{
  U16 MSL_Year;     /*!< Stores the current Year (e.g. 2008). */
  U08 MSL_Month;    /*!< Stores the current Month (1-12). */
  U08 MSL_Day;      /*!< Stores the current Day (1-31). */
  U08 MSL_Hour;     /*!< Stores the current Hour (0-23). */
  U08 MSL_Min;      /*!< Stores the current Min (0-59). */
  U08 MSL_Sec;      /*!< Stores the current Sec (0-59). */
  U32 MSL_mSec;     /*!< Stores the current mSec (0-999) or mSec tick count (targets without mSec support). */
} MSL_time_t;  

/******************
 * File Functions *
 ******************/

/* Create a directory within a file system using the path specified.
 * Specify a directory path using the same format supported by standard
 * lib file I/O (e.g. "./subdir" for a sub directory to the dir in
 * which the app runs). */
U16 MSL_CreateDir(char dir[RXN_MSL_MAX_PATH]);

/* Clears the directory using code derived from the linux system call "rm -r".
 * This function clears all files found in a directory.*/
void MSL_CleanDir(char dir[RXN_MSL_MAX_PATH]);

/* Open a file for subsequent I/O.
 * For windows - use standard lib support and fileConfig values of
 * "w", "r+", etc. For targets not supporting the standard lib for
 * file I/O, substitution of appropriate call arguements may be
 * required to specify file overwrite, append, binary I/O, etc.
 * Also - use file path format as supported by the
 * standard lib (e.g. "./" for the dir in which the app executes,
 * "./subDir" for the subDir to the dir in which the app executes, e.t.c.).
 * If a target does not support this format, the format can be altered as
 * required by the target (within the target specific implementation). */
U16 MSL_OpenFile(U08 rsrcID, const CHAR file[RXN_MSL_MAX_PATH], CHAR fileConfig[MSL_MAX_FILE_CONFIG_STR_LEN]);

/* Close a file following I/O. */
U16 MSL_CloseFile(U08 rsrcID);

/* Read a line from a file.
 * If EOF is reached, an error will result. */
U16 MSL_ReadFileLine(U08 rsrcID, U08* readBuf, const U16 readBufSize);

/* Write a line to a file. The writeBuf MUST be null terminated. */
U16 MSL_WriteFileLine(U08 rsrcID, const U08* writeBuf, U32 maxFilesize);

/* Read a number of bytes from a file. */
U16 MSL_ReadFileBytes(U08 rsrcID, U08* readBuf, const U16 bytesToRead);

/* Write a number of bytes to a file. */
U16 MSL_WriteFileBytes(U08 rsrcID, const U08* writeBuf, const U16 bytesToWrite);

/* Reset a file pointer to an offset from file start. */
U16 MSL_SetFilePtrOffset(U08 rsrcID, U32 offset);

/* Flush buffer contents to a file. */
U16 MSL_FileFlush(U08 rsrcID);

/******************
 * Time Functions *
 ******************/

/* Get local or GMT/UTC date and time data. */
void MSL_GetLogTime(MSL_time_t* pTime, const BOOL GMT);

/* Get mSec ticks since device startup. */
U64 MSL_GetTickMilliSecond(void);

/* Get sec ticks since device startup */
U32 MSL_GetTickSecond(void);

/* Abstracts the system sleep function */
void MSL_Sleep(U32 sleepMSec);

#if defined (WIN32) || (WINCE)
void MSL_SetPerformanceFrequency(void);
#endif

/*
 * This converts MSL_time time struct to seconds since the Epoch (00:00:00 UTC, January 1, 1970).
 */
time_t MSL_ConvertTimeStructToSeconds(MSL_time_t* pTime);


/********************
 * Socket Functions *
 ********************/

/* Resolve an IP address given a URL */
U16 MSL_ScktGetHostIP(const char Host[MSL_MAX_HOST_STR_LEN], char IPAdd[MSL_SKT_ADDR_MAX_LEN]);

/* Open a socket (UDP) and connect to a specified host using a specified port. 
 * Performs a DNS lookup on the host argument. */
U16 MSL_ScktOpenUDP(const char* host, U16 port, SOCKET* pUDPSocket);

/* Open a socket (TCP) and connect to a specified host using a specified port. */
U16 MSL_ScktOpenTCP(char IPAdd[MSL_SKT_ADDR_MAX_LEN], U16 port, U32 readTO, SOCKET* pTCPSocket);

/* Disconnect from a connected host and close a socket. */
U16 MSL_ScktClose(SOCKET GenSocket);

/* Read a msg from an opened socket. Won't block. */
U16 MSL_ScktReadBytes(SOCKET GenSocket, U08* pBuf, U16 uBufSize, U32 msTO, U32* puNumBytesRead);

/* Write a msg through an opened socket. Won't block. */
U16 MSL_ScktWriteBytes(SOCKET GenSocket, U08* pBuf, U32 uNumBytesToWrite, U32 msTO,
                      char IPAdd[MSL_SKT_ADDR_MAX_LEN], U16 port);

/* Fills addr with the given ip and port */
void MSL_SetSockaddr(struct sockaddr_in* addr, const char* ip, U16 port);

/* Sets a socket to be non-blocking */
U16 MSL_SetSocketNonBlocking(SOCKET sckt);

/******************************
 * Critical Section Functions *
 ******************************/

/* Initialize 32 critical sections for subsequent use. */
U16 MSL_InitBlocks(void);

/* Free resources associated with all critical sections. */
void MSL_UninitBlocks(void);

/* Try entering a block of code that is controlled by a critical section.
 * Will return FALSE if the code is already blocked.
 * Use enum MSL_CriticalSections to choose the critical section. */
BOOL MSL_TryEnterBlock(U08 CSIdx);

/* Try entering a block of code that is controlled by a critical section.
 * Will block on this function until the code is free if already blocked.
 * Use enum MSL_CriticalSections to choose the critical section. */
void MSL_EnterBlock(U08 CSIdx);

/* Leave a block of code that is controlled by a critical section.
 * Use enum MSL_CriticalSections to choose the critical section. */
U16 MSL_LeaveBlock(U08 CSIdx);

/* starts a thread */
THREAD MSL_StartThread(void* threadStartFunc);

#ifdef ANDROID
/* Release Android wakeLock */
void MSL_ReleaseMainWakeLock();

/* Acquire wakeLock function */
void MSL_AcquireMainWakeLock();
#endif

/*********************
 * Utility Functions *
 ********************/

/* Returns the OS specific error number (errno or WSAGetLastError()) */
S32 MSL_GetOSError();

/* Returns a string representation of an OS specific error */
const char* MSL_GetOSErrorString(S32 error);

/* Sends a command to the messaging thread */
U16 MSL_CmdRequest(EVENT event);

/*******************
 * Timer Functions *
 ******************/

/* Creates the timer */
void MSL_CreateTimer();

/* Closes the timer */
void MSL_CloseTimer();

/* Exits the current timeout early */
U16 MSL_ExitTimer();

/* Waits until the timeout expires */
U16 MSL_WaitTimer(U32 timeoutSeconds);


/********************
 * Thread Functions *
 ********************/
#if defined(WIN32) || defined(WINCE)
#include <windows.h>
enum RXN_MSL_Priority
{
	RXN_MSL_PRIORITY_LOW = THREAD_PRIORITY_BELOW_NORMAL,
	RXN_MSL_PRIORITY_NORMAL = THREAD_PRIORITY_NORMAL,
	RXN_MSL_PRIORITY_HIGH = THREAD_PRIORITY_ABOVE_NORMAL
};
#else
enum RXN_MSL_Priority
{
	RXN_MSL_PRIORITY_LOW = 5,
	RXN_MSL_PRIORITY_NORMAL = 0,
	RXN_MSL_PRIORITY_HIGH = -20
};
#endif

#ifdef ANDROID
typedef struct {  
    pthread_mutex_t mutex;
    pthread_cond_t cond;
} MSL_SYNC;
#endif

/* On Windows, sets the priority of the current thread.  On Linux, sets the
 * priority of the entire process. */
void RXN_MSL_SetPriority(enum RXN_MSL_Priority priority);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* RXN_MSL_FILE_PLATFORM_H */
