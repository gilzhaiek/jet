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
 * $LastChangedDate: 2012-06-22 16:46:51 -0700 (Fri, 22 Jun 2012) $
 * $Revision: 97267 $
 *************************************************************************
 *
 */

/**
 * \file
 * \brief
 * Defines the primitive data types and their sizes.
 *
 * \since
 * 1.1.0
 */

#ifndef RXN_DATA_TYPES_H
#define RXN_DATA_TYPES_H

#ifdef __cplusplus
extern "C" {
#endif

  // --- Unsigned integer types: 8 bits, 16 bits, 32 bits

  /**
   * \brief
   * 1 byte unsigned integer value. 
   *
   * \since
   * 1.1.0
   */
  typedef unsigned char       U08;      

  /**
   * \brief
   * 2 byte unsigned integer value. 
   *
   * \since
   * 1.1.0
   */
  typedef unsigned short      U16;

  /**
   * \brief
   * 4 byte unsigned integer value.
   *
   * \since
   * 1.1.0
   */
  typedef unsigned int        U32;

  /**
   * \brief
   * 8 byte unsigned integer value. 
   *
   * \since
   * 1.1.0
   */
#if defined(_MSC_VER) && (_MSC_VER == 1200) /* Microsoft Visual C++ 6.0 */ 
  typedef unsigned __int64    U64;
#else   
  typedef unsigned long long  U64;
#endif  

  // --- Signed integer types: 8 bits, 16 bits, 32 bits

  /**
   * \brief
   * 1 byte signed integer value. 
   *
   * \since
   * 1.1.0
   */
  typedef signed char         S08;

  /**
   * \brief
   * 2 byte signed integer value. 
   *
   * \since
   * 1.1.0
   */
  typedef signed short        S16;

  /**
   * \brief
   * 4 byte signed integer value.
   *
   * \since
   * 1.1.0
   */
  typedef signed int          S32;

  /**
   * \brief
   * 8 byte signed integer value. 
   *
   * \since
   * 1.1.0
   */
#if defined(_MSC_VER) && (_MSC_VER == 1200) /* Microsoft Visual C++ 6.0 */ 
  typedef signed   __int64    S64;
#else      
  typedef signed long long    S64;
#endif  

  // --- Float point types: 32 bits, 64 bits

  /**
   * \brief
   * 4 byte floating point value. 
   *
   * \since
   * 1.1.0
   */
  typedef float               R32;

  /**
   * \brief
   * 8 byte floating point value. 
   *
   * \since
   * 1.1.0
   */
  typedef double              R64;

  // Boolean type
  /**
   * \brief
   * Boolean value
   *
   * \since
   * 1.1.0
   */
  typedef int                 BOOL;

  // Boolean values: TRUE, FALSE
  /**
   * \brief
   * Boolean TRUE value.
   *
   * \since
   * 1.1.0
   */
#ifndef TRUE
  #define TRUE  1
#endif

  /**
   * \brief
   * Boolean FALSE value.
   *
   * \since
   * 1.1.0
   */
#ifndef FALSE
  #define FALSE 0
#endif

  // NULL
  /**
   * \brief
   * NULL pointer value.
   *
   * \since
   * 1.1.0
   */
#ifndef NULL
  #define NULL (void*)0
#endif

#ifdef __cplusplus
} // extern "C"
#endif

#endif // RXN_DATA_TYPES_H
