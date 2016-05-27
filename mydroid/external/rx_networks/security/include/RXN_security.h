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
 * $LastChangedDate: 2012-06-19 10:37:16 -0700 (Tue, 19 Jun 2012) $
 * $Revision: 96887 $
 *************************************************************************
 *
 */

/**
 * \file
 * \brief
 * Defines the security related functions.
 */

#ifndef RXN_SECURITY_H
#define RXN_SECURITY_H

#include "RXN_data_types.h"

#ifdef __cplusplus
extern "C" {
#endif

  /**
   * \brief
   * The maximum length for fields
   */
  #define MAX_FIELD_LENGTH 50

  /**
   * \brief
   * The maximum length for password
   */
  #define PASSWORD_LENGTH 41

  /**
   * \brief
   * The maximum length for the expiry date string.
   *
   * \since
   * 4.0.2
   */
  #define RXN_CONSTANT_MAX_SECURITY_EXPIRY_DATE_LENGTH 20

  /**
   * \brief
   * A structure containing client information.
   *
   * \details
   * The client information is necessary to perform authentication
   * when communicating with RXN servers.
   */
  typedef struct RXN_client_info {
    char vendor_id[MAX_FIELD_LENGTH]; /**< RXN provided vendor ID. */
    char model_id[MAX_FIELD_LENGTH]; /**< Customer supplied model ID. */
    char device_id[MAX_FIELD_LENGTH]; /**< Customer supplied device ID. */
    U32 gps_seconds; /**< Current time in GPS seconds since start of GPS. */
  } RXN_client_info_t;

  /**
   * \brief
   * A structure containing client password.
   *
   * \details
   * The password is a hash value computed based on the supplied RXN_client_info.
   */
  typedef struct RXN_password {
    U08 password[PASSWORD_LENGTH]; /**< Password value. */
  } RXN_password_t;

  /**
   * \brief
   * Provide the security key to the library.
   *
   * \param security_key [IN]
   * The pointer to a block of memory containing the security key.
   *
   * \param security_key_length [IN]
   * The length of the security key.
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
   * RXN_Generate_Password() can be used.
   *
   * \since
   * 3.0.0
   */
  U16 RXN_Set_Security_Key(const char* security_key,
    const U32 security_key_length, const char* vendor_id);

  /**
   * \brief 
   * Validates the security key being used against the specified time.
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
   * Proprietary to GPStream (tm) PGPS (tm) client software.
   *
   * \since
   * 4.0.2
   *
   * \see
   * RXN_Set_Security_Key()
   *
   * \see
   * RXN_Get_Security_Expiry_Date()
   */
  U16 RXN_Is_Security_Valid(
    const U32 time
    );

  /**
   * \brief 
   * Get the expiry time of the security key in ASCII text.
   *
   * \param expiryDate [OUT]
   * A memory location to be filled with the expiry date.
   * This is a null terminated string and is at most RXN_CONSTANT_MAX_SECURITY_EXPIRY_DATE_LENGTH
   * (50) characters long.
   *
   * \details
   * This function returns the expiry date in "YYYY-MM-DD UTC" format.
   * If no security key was provided, this function will return a null terminated
   * empty string.
   * 
   * <b>Applicable Standards</b>\n
   * Proprietary to GPStream (tm) PGPS (tm) client software.
   *
   * \since
   * 4.0.2
   *
   * \see
   * RXN_Set_Security_Key()
   *
   * \see
   * RXN_Is_Security_Valid()
   */  
  void RXN_Get_Security_Expiry_Date(
    char expiryDate[RXN_CONSTANT_MAX_SECURITY_EXPIRY_DATE_LENGTH]
  );

  /**
   * \brief
   * Generates the password based on client information for authenticating
   * with PGPS (tm) server.
   *
   * \param client_info
   * [IN] This is a data structure described in the PGPS (tm) data structures
   * section of this document. The data structure contains all the information
   * that was used to register and enable the GPStream (tm) PGPS (tm) data service for
   * each specific device. The structure includes data fields to hold the vendor
   * id, a device id, and the current time in GPS seconds.
   *
   * \param password
   * [OUT] This is a data structure described in the PGPS (tm) data structures
   * section of this document. Upon successful completion of this function,
   * a password will be computed and filled into the supplied memory location.
   * The password is required when performing Seed requests and Clock update
   * requests from PGPS (tm) data service.
   *
   * \return
   * RXN_SUCCESS if the password can be computed successfully. RXN_FAIL otherwise.
   *
   * \details
   * PGPS (tm) is a server based assistance technology. In order to restrict PGPS (tm)
   * servers from authorized access only, all requests to the PGPS (tm) server for
   * Seeds and Clock updates must be authenticated. This function is used to
   * generate the required password based on the supplied client information. After
   * the password has been generated, the client can perform an HTTP GET request
   * containing the vendor ID, model ID, device ID, and password to request the
   * appropriate type of data from the server. At the time of registration, each
   * customer will be assigned a unique vendor ID and will also receive a customer
   * specific security library containing the RXN_Generate_Password() function.
   *
   * <b>Applicable Standards</b>\n
   * Proprietary to GPStream (tm) PGPS (tm) client software, GPStrem PGPS (tm) server.
   *
   * \attention
   * The password is time sensitive so the generate password function
   * must be used every time prior to making a PGPS (tm) server request.
   *
   * \since
   * 1.1.0
   *
   * \see
   * RXN_client_info_t
   *
   * \see
   * RXN_password_t
   *
   * \see
   * RXN_Decode()
   *
   * <b>Example Usage</b>
   * \code
   * //Variables for required to generate assistance data.
   * RXN_client_info_t client_info;
   * RXN_password_t password;
   
   * //Variable to store the status of the following operations.
   * U16 password_generated;
   
   * Populate the RXN_client_info_t structure with the appropriate
   *  client information.
   * memcpy(client_info.vendor_id, DEFAULT_VENDOR_ID, sizeof(client_info.vendor_id));
   * memcpy(client_info.device_id, DEFAULT_DEVICE_ID, sizeof(client_info.device_id));
   * client_info.gps_seconds = get_current_time_in_gps_seconds();
   *
   * //Generate the password.
   * password_generated = RXN_Generate_Password(&client_info, &password);
   *
   * printf("vendor_id: %s\n", client_info.vendor_id);
   * printf("device_id: %s\n", client_info.device_id);
   * printf("gps_seconds: %u\n", client_info.gps_seconds);
   * printf("password generated: %s\n", (password_generated == RXN_SUCCESS) ? "SUCCESS" : "FAIL");
   * printf("password: %s\n", password.password);
   *
   * \endcode
   */
  U16 RXN_Generate_Password(const RXN_client_info_t* client_info,
    RXN_password_t* password);

#ifdef __cplusplus
} // extern "C"
#endif

#endif // RXN_SECURITY_H
