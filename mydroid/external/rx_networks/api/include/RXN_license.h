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
 * $LastChangedDate: 2009-05-28 14:46:08 -0700 (Thu, 28 May 2009) $
 * $Revision: 11016 $
 *************************************************************************
 *
 */

/**
 * \file
 * \brief
 * Defines the license management functions.
 * 
 * \details
 * The license management interface provides an interface to 
 * validate the use of the RxN software and available functions.
 * 
 * \since
 * 3.0.0
 */

#ifndef RXN_LICENSE_H
#define RXN_LICENSE_H

#include "RXN_constants.h"

#ifdef __cplusplus
extern "C" {
#endif

  /**
   * \brief 
   * Provide the license key to the library.
   *
   * \param license_key [IN]
   * The pointer to a block of memory containing the license key.
   *
   * \param license_key_length [IN]
   * The length of the license key.
   *
   * \param vendor_id [IN]
   * The vendor specific id.
   *
   * \return RXN_SUCCESS
   * If the function completes successfully.
   *
   * \return RXN_FAIL
   * If the function fails. 
   *
   * <b>Applicable Standards</b>\n
   * Proprietary to GPStream (tm) PGPS (tm) client software, SAGPS (tm) client software.
   *
   * \attention
   * This function must be called with a valid key before
   * RXN_Generate_Assistance() and RXN_Get_Ephemeris() can be used.
   *
   * \since
   * 3.0.0
   *
   * \see
   * RXN_Is_License_Valid()
   *
   * \see
   * RXN_Get_License_Expiry_Date()
   */
  U16 RXN_Set_License_Key(
    const char* license_key,    
    const U32 license_key_length,
    const char* vendor_id
  );


  /**
   * \brief 
   * Validates the license key being used against the specified time.
   *
   * \param time [IN]
   * The current time in GPS seconds.
   *
   * \return RXN_SUCCESS
   * If the function completes successfully.
   *
   * \return RXN_FAIL
   * If the function fails. 
   *
   * \details
   * This function will validate if the user supplied key is valid 
   * for the specified time. Typically, the user will pass in the current GPS
   * time for validation.
   * 
   * <b>Applicable Standards</b>\n
   * Proprietary to GPStream (tm) PGPS (tm) client software, SAGPS (tm) client software.
   *
   * \since
   * 3.0.0
   *
   * \see
   * RXN_Set_License_Key()
   *
   * \see
   * RXN_Get_License_Expiry_Date()
   */
  U16 RXN_Is_License_Valid(
    const U32 time
  );


  /**
   * \brief 
   * Get the expiry time of the license key in ASCII text.
   *
   * \param expiryDate [OUT]
   * A memory location to be filled with the expiry date.
   * This is a null terminated string and is at most RXN_CONSTANT_MAX_LICENSE_EXPIRY_DATE_LENGTH
   * (50) characters long.
   *
   * \details
   * This function returns the expiry date in "YYYY-MM-DD UTC" format.
   * If no license key was provided, this function will return a null terminated
   * empty string.
   * 
   * <b>Applicable Standards</b>\n
   * Proprietary to GPStream (tm) PGPS (tm) client software, SAGPS (tm) client software.
   *
   * \since
   * 3.0.0
   *
   * \see
   * RXN_Set_License_Key()
   *
   * \see
   * RXN_Is_License_Valid()
   */  
  void RXN_Get_License_Expiry_Date(
    char expiryDate[RXN_CONSTANT_MAX_LICENSE_EXPIRY_DATE_LENGTH]
  );

#ifdef __cplusplus
} // extern "C"
#endif

#endif // RXN_LICENSE_h
