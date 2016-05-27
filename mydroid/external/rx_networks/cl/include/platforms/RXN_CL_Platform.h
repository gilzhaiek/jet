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
 * $LastChangedDate: 2009-03-02 15:53:19 -0800 (Mon, 02 Mar 2009) $
 * $Revision: 9396 $
 *************************************************************************
 *
 */

#ifndef RXN_CL_PLATFORM_H
#define RXN_CL_PLATFORM_H

#ifdef __cplusplus
extern "C" {
#endif

#ifdef _MSC_VER
#include "RXN_CL_Windows.h"
#else
#include "RXN_CL_Linux.h"    
#endif

/********************
 * Log Functions    *
 ********************/

U32 CL_GetMaxLogSize(void);

/********************
 * Thread Functions *
 ********************/

/* Sleep a thread. */
void CL_Sleep(U32 mSecToSleep);

/******************
 * Time Functions *
 ******************/

/* Get current time values (Year, month, ...min, sec). */
void CL_GetTime(CL_time_t* pTime, BOOL GMT);

/* Get the current GPS time (UTC/GMT)
 *(i.e. seconds since GPS start). */
U32 CL_GetGPSTime(void);

/* Get the number of mSec ticks since target start. */
U32 CL_GetTickCount(void);

/********************
 * Serial Functions *
 ********************/

/** 
 * \brief 
 * Open a port for supbsequent I/O. Port handle stored within pAttrib.
 *
 * \param config 
 * [IN] The configuration string used to initialize the serial port.
 * \param pAttrib
 * [OUT] Pointer to CL_Chipset_Attrib_t structure that will hold the handle to reference the opened serial port.
 *
 * \details
 * <b>Description</b>\n
 * This function is used to open a serial port and store the handle in the pAttrib pointer.
 * 
 */
U16 CL_OpenPort(char config[RXN_CL_CONFIG_MAX_STR_LEN], CL_Chipset_Attrib_t* pAttrib);

/** 
 * \brief 
 * Setup a port (using handle within pAttrib) including timeout parameters.
 *
 * \param pAttrib 
 * [IN] Pointer to serial port handle.
 * \param uReadIntervalTimeout
 * [IN] The maximum time allowed to elapse between the arrival of two bytes on the communications line, in milliseconds.
 * \param uReadTimeoutMult
 * [IN] The multiplier used to calculate the total time-out period for read operations, in milliseconds.
 * \param m_uReadTimeoutConstant
 * [IN] A constant used to calculate the total time-out period for read operations, in milliseconds.
 * \param uWriteTimeoutMult 
 * [IN] The multiplier used to calculate the total time-out period for write operations, in milliseconds.
 * \param uWriteTimeoutConstant 
 * [IN] A constant used to calculate the total time-out period for write operations, in milliseconds.
 * \param uBaudRate 
 * [IN] The baud rate at which read and write operations will operate at on the serial port.
 * \param uByteSize 
 * [IN] The number of bits in the bytes transmitted and received.
 * \param uParity 
 * [IN] The parity scheme to be used.
 * \param uStopBits 
 * [IN] The number of stop bits to be used.
 * \details
 * <b>Description</b>\n
 * This function is used to configure the operating parameters and timeout values for the serial port operations.
 * 
 */
U16 CL_SetupPort(CL_Chipset_Attrib_t* pAttrib, U32 uReadIntervalTimeout, U32 uReadTimeoutMult,
	U32 m_uReadTimeoutConstant, U32 uWriteTimeoutMult, U32 uWriteTimeoutConstant,
	U32 uBaudRate, U08 uByteSize, U08 uParity, U08 uStopBits);

/** 
 * \brief 
 * Read bytes from an already opened port.
 *
 * \param pAttrib 
 * [IN] Pointer to serial port handle.
 * \param pBuf 
 * [IN] Pointer to read buffer.
 * \param uBufSize 
 * [IN] Size of read buffer.
 * \param puNumBytesRead 
 * [OUT] Pointer to the number of bytes read.
 * \details
 * <b>Description</b>\n
 * Call to read data from the serial port and place it into buffer supplied.
 * 
 */
U16 CL_ReadBytes(CL_Chipset_Attrib_t* pAttrib, U08* pBuf, U16 uBufSize, U32* puNumBytesRead); 

/** 
 * \brief 
 * Write bytes to an already opened port. 
 *
 * \param pAttrib 
 * [IN] Pointer to serial port handle.
 * \param pBuf 
 * [IN] Pointer to write buffer.
 * \param uNumBytesToWrite 
 * [IN] Size of write buffer.
 *
 * \details
 * <b>Description</b>\n
 * Call to write supplied buffered data to the serial port.
 * 
 */
U16 CL_WriteBytes(CL_Chipset_Attrib_t* pAttrib, U08* pBuf, U32 uNumBytesToWrite); 

/** 
 * \brief 
 * Close a previously opened port.
 *
 * \param pAttrib 
 * [IN] Pointer to serial port handle.
 *
 * \details
 * <b>Description</b>\n
 * Close the serial port identified by pAttrib.
 * 
 */
U16 CL_ClosePort(CL_Chipset_Attrib_t* pAttrib);

/** 
 * \brief 
 * Purge a previously opened port Tx buffer.
 *
 * \param pAttrib 
 * [IN] Pointer to serial port handle.
 *
 * \details
 * <b>Description</b>\n
 * Purge a previously opened port Tx buffer.
 * 
 */
U16 CL_PurgeTx(CL_Chipset_Attrib_t* pAttrib);

/** 
 * \brief 
 * Purge a previously opened port Rx buffer.
 *
 * \param pAttrib 
 * [IN] Pointer to serial port handle.
 *
 * \details
 * <b>Description</b>\n
 * Purge a previously opened port Rx buffer.
 * 
 */
U16 CL_PurgeRx(CL_Chipset_Attrib_t* pAttrib);

/** 
 * \brief 
 * Closes a socket.
 *
 * \param GenSocket 
 * [IN] Pointer to socket.
 * \details
 * <b>Description</b>\n
 * This function closes the socket reference by the pointer.
 * 
 */
U16 CL_CloseSckt(SOCKET GenSocket);

/* Initialize critical section for subsequent use. */
void CL_InitBlock(LOCK* lock);

/* Free resources associated with critical section. */
void CL_UninitBlock(LOCK* lock);

/* Try entering a block of code that is controlled by a critical section.
 * Will return FALSE if the code is already blocked. */
BOOL CL_TryEnterBlock(LOCK* lock);

/* Try entering a block of code that is controlled by a critical section.
 * Will block on this function until the code is free if already blocked. */
void CL_EnterBlock(LOCK* lock);

/* Leave a block of code that is controlled by a critical section. */
void CL_LeaveBlock(LOCK* lock);

void CL_InitEvent(EVENT* event);
void CL_UninitEvent(EVENT* event);
BOOL CL_WaitForEvent(EVENT* event);
void CL_SignalEvent(EVENT* event);

THREAD CL_StartThread(void* threadStartFunc, void* pArg);
void CL_StopThread(THREAD thread);

S32 CL_GetOSError();
const char* CL_GetOSErrorString(S32 error);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* RXN_CL_PLATFORM_H */
