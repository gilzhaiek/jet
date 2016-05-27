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
 * $LastChangedDate: 2012-12-19 16:31:16 -0800 (Wed, 19 Dec 2012) $
 * $Revision: 104572 $
 *************************************************************************
 *
 */

/**
 * \file
 * \brief
 * Defines the structures and functions to access debug information.
 *
 * \details
 * These functions are supplemental to the main API functions and are
 * intended to provide more information for troubleshooting
 * purposes. They are not required for the normal operation of the system.
 *
 * \since
 * 1.1.0
 */

#ifndef RXN_DEBUG_H
#define RXN_DEBUG_H

#include "RXN_constants.h"
#include "RXN_data_types.h"
#include "RXN_structs.h"

#ifdef __cplusplus
extern "C" {
#endif

  /**
   * \brief
  * Defines the maximum number of characters for a debug code message string.
  *
  * \since
  * 1.2.0
  */
  #define RXN_DEBUG_CODE_STRING_LENGTH              0200
  
  /**
  * \brief
  * Defines the value where the range of RESERVED codes begins.
  *
  * \since
  * 1.2.0
  */
  #define RXN_DEBUG_CODE_RESERVED_RANGE_BEGIN       0001
  
  /**
  * \brief
  * Defines the value where the range of RESERVED codes ends.
  *
  * \since
  * 1.2.0
  */
  #define RXN_DEBUG_CODE_RESERVED_RANGE_END         2000
  
  /**
  * \brief
  * Defines the value where the range of INFO codes begins.
  *
  * \since
  * 1.2.0
  */
  #define RXN_DEBUG_CODE_INFO_RANGE_BEGIN           2001
  
  /**
  * \brief
  * Defines the value where the range of INFO codes ends.
  *
  */
  #define RXN_DEBUG_CODE_INFO_RANGE_END             4000
  
  /**
  * \brief
  * Defines the value where the range of WARN codes begins.
  *
  * \since
  * 1.2.0
  */
  #define RXN_DEBUG_CODE_WARN_RANGE_BEGIN           4001
  
  /**
  * \brief
  * Defines the value where the range of WARN codes ends.
  *
  * \since
  * 1.2.0
  */
  #define RXN_DEBUG_CODE_WARN_RANGE_END             6000
  
  /**
  * \brief
  * Defines the value where the range of ERROR codes begins.
  *
  * \since
  * 1.2.0
  */
  #define RXN_DEBUG_CODE_ERROR_RANGE_BEGIN          6001
  
  /**
  * \brief
  * Defines the value where the range of ERROR codes ends.
  *
  * \since
  * 1.2.0
  */
  #define RXN_DEBUG_CODE_ERROR_RANGE_END            8000
  
  /**
  * \brief
  * An enumeration of all the available debug codes used throughout the library.
  *
  * The codes are separated into the following categories:
  * \li OK
  * \li RESERVED
  * \li INFO
  * \li WARN
  * \li ERROR
  *
  * With the exception of OK (0), the rest of the categories are organized
  * into sets of numerical ranges bounded by the following DEFINE constants:
  * \li RXN_DEBUG_CODE_RESERVED_RANGE_BEGIN --> RXN_DEBUG_CODE_RESERVED_RANGE_END
  * \li RXN_DEBUG_CODE_INFO_RANGE_BEGIN     --> RXN_DEBUG_CODE_INFO_RANGE_END
  * \li RXN_DEBUG_CODE_WARN_RANGE_BEGIN     --> RXN_DEBUG_CODE_WARN_RANGE_END
  * \li RXN_DEBUG_CODE_ERROR_RANGE_BEGIN    --> RXN_DEBUG_CODE_ERROR_RANGE_END
  *
  * \remark
  * Please note the enumeration value may change in the future as more 
  * codes are added. The names are sorted to aid reading and usability.
  * Always refer to the enumeration name in the code (instead of the int value)
  * to avoid compatibilities problem when migrating to a future version. 
  *
  * \since
  * 1.2.0
  */
  typedef enum RXN_debug_code {
    // ==========================================================================
    //                        [BASIC             0000]                        
    // ==========================================================================
    
    RXN_DEBUG_CODE_SUCCESS = RXN_SUCCESS,
    /**< \brief Success, equivalent to RXN_SUCCESS code.*/
    RXN_DEBUG_CODE_FAIL = RXN_FAIL,
    /**< \brief Fail, equivalent to RXN_FAIL code.*/
  
    // ==========================================================================
    //                        [RESERVED   0001 - 2000]                        
    // ==========================================================================
  
  
    // ==========================================================================
    //                        [INFO       2001 - 4000]                        
    // ==========================================================================
  
    RXN_DEBUG_CODE_INFO_GENERATED_NEW_SEED = RXN_DEBUG_CODE_INFO_RANGE_BEGIN, 
    /**< \brief New seed generated. */
    RXN_DEBUG_CODE_INFO_PROPAGATED_OLD_SEED, 
    /**< \brief Propagated old seed. */
    RXN_DEBUG_CODE_INFO_PROPAGATION_COMPLETED_PREVIOUSLY,
    /**< \brief Seed have already been propagated to the target time. */
    RXN_DEBUG_CODE_INFO_GPS_SEED_DECODE_SUCCESSFUL,
    /**< \brief Decoded PGPS GPS seed successfully.*/
    RXN_DEBUG_CODE_INFO_GLO_SEED_DECODE_SUCCESSFUL,
    /**< \brief Decoded PGPS GLONASS seed successfully.*/
    RXN_DEBUG_CODE_INFO_SEED_GLO_AUX_DATA_DECODE_SUCCESSFUL,               
    /**< \brief Decoded PGPS seed GLONASS auxiliary data successfully.*/
    RXN_DEBUG_CODE_INFO_SEED_GPS_BLOCK_TYPE_DATA_DECODE_SUCCESSFUL,               
    /**< \brief Decoded PGPS seed GPS block type data successfully.*/
    RXN_DEBUG_CODE_INFO_SEED_GLO_BLOCK_TYPE_DATA_DECODE_SUCCESSFUL,               
    /**< \brief Decoded PGPS seed GLONASS block type data successfully.*/
    RXN_DEBUG_CODE_HOT_EPH,
    /**< \brief Ephemeris computed directly from hot seed (bypassing polys). */
    
    // ==========================================================================
    //                        [WARN       4001 - 6000]                        
    // ==========================================================================
  
    RXN_DEBUG_CODE_WARN_UNKNOWN = RXN_DEBUG_CODE_WARN_RANGE_BEGIN,
    /**< \brief Unknown debug code. Either the code is uninitalized or is undetermined. */
    RXN_DEBUG_CODE_WARN_NO_GOOD_SEED,               
    /**< \brief Tried to propagate but there was no seed. Unable to propagate any seed.*/
    RXN_DEBUG_CODE_WARN_UNSUPPORTED,               
    /**< \brief Tried to use an unsupported feature.*/

    // ==========================================================================
    //                        [ERROR      6001 - 8000]                        
    // ==========================================================================
  
    RXN_DEBUG_CODE_ERROR_ABORTED = RXN_DEBUG_CODE_ERROR_RANGE_BEGIN, 
    /**< \brief Function aborted as a result of calling RXN_Set_Abort(). */
    RXN_DEBUG_CODE_ERROR_NULL_ARGUMENT,
    /**< \brief The supplied argument is null. */
    RXN_DEBUG_CODE_ERROR_CACHE_SIZE_TOO_SMALL,
    /**< \brief The supplied cache size is too small, cache will be disabled. */
    RXN_DEBUG_CODE_ERROR_INVALID_PRN_NUMBER,
    /**< \brief The supplied PRN is invalid. */
    RXN_DEBUG_CODE_ERROR_RE_PHASE_DETECTED,               
    /**< \brief PRN was deemed re-phased.*/
    RXN_DEBUG_CODE_ERROR_UNSUPPORT_SEED_VERSION,
    /**< \brief The version of the PGPS seed is unsupported. */
    RXN_DEBUG_CODE_ERROR_INCOMPLETE_SEED_RESPONSE,
    /**< \brief The PGPS seed is incomplete. */
    RXN_DEBUG_CODE_ERROR_SEED_GLO_AUX_DATA_DECODE_FAILED,
    /**< \brief The PGPS seed GLONASS auxiliary data failed to decode. */
    RXN_DEBUG_CODE_ERROR_SEED_GPS_BLOCK_TYPE_DATA_DECODE_FAILED,
    /**< \brief The PGPS seed GPS block type data decode failed. */
    RXN_DEBUG_CODE_ERROR_SEED_GLO_BLOCK_TYPE_DATA_DECODE_FAILED,
    /**< \brief The PGPS seed GLONASS block type data decode failed. */
    RXN_DEBUG_CODE_ERROR_SEED_CURRENT_TIME_LESS_THAN_TARGET_TIME,
    /**< \brief Requested target time is more than seed curren time. The seed has not been propagated far enough. */
    RXN_DEBUG_CODE_ERROR_EVALUATION_LICENSE_EXPIRED,
    /**< \brief Evaluation license has expired. */
    RXN_DEBUG_CODE_ERROR_EVALUATION_LICENSE_MANAGER_KEY_IS_NULL,
    /**< \brief Evaluation license key supplied is null. */
    RXN_DEBUG_CODE_ERROR_EVALUATION_LICENSE_MANAGER_KEY_LENGTH_IS_ZERO,
    /**< \brief Evaluation license key supplied string has length 0. */
    RXN_DEBUG_CODE_ERROR_EVALUATION_LICENSE_MANAGER_KEY_LENGTH_MISTMATCH,
    /**< \brief Evaluation license key supplied length mismatch with expectation. */
    RXN_DEBUG_CODE_ERROR_EVALUATION_LICENSE_MANAGER_VENDOR_ID_IS_NULL,
    /**< \brief Evaluation license vendor id supplied is null. */
    RXN_DEBUG_CODE_ERROR_EVALUATION_LICENSE_MANAGER_VENDOR_ID_LENGTH_IS_ZERO,
    /**< \brief Evaluation license vendor id string has length 0. */    
    RXN_DEBUG_CODE_ERROR_EVALUATION_LICENSE_MANAGER_STATE_D1,
    /**< \brief Evaluation license internal error. */
    RXN_DEBUG_CODE_ERROR_EVALUATION_LICENSE_MANAGER_STATE_D2,
    /**< \brief Evaluation license internal error. */
    RXN_DEBUG_CODE_ERROR_EVALUATION_LICENSE_MANAGER_STATE_C1,
    /**< \brief Evaluation license internal error. */
    RXN_DEBUG_CODE_ERROR_EVALUATION_LICENSE_MANAGER_STATE_C2,
    /**< \brief Evaluation license internal error. */
    RXN_DEBUG_CODE_ERROR_EVALUATION_LICENSE_MANAGER_STATE_C3,
    /**< \brief Evaluation license internal error. */
    RXN_DEBUG_CODE_ERROR_EVALUATION_LICENSE_MANAGER_STATE_C4,
    /**< \brief Evaluation license internal error. */
    RXN_DEBUG_CODE_ERROR_EVALUATION_LICENSE_MANAGER_STATE_C5,
    /**< \brief Evaluation license internal error. */
    RXN_DEBUG_CODE_ERROR_EVALUATION_LICENSE_MANAGER_STATE_C6,
    /**< \brief Evaluation license internal error. */    
    RXN_DEBUG_CODE_ERROR_MEM_READ_EPH_FAIL,
    /**< \brief RXN_Mem_Read_Ephemeris() failed. */
    RXN_DEBUG_CODE_ERROR_MEM_READ_EPH_INDEX_FAIL,
    /**< \brief RXN_Mem_Read_Ephemeris_Index failed. */
    RXN_DEBUG_CODE_ERROR_MEM_READ_POLY_FAIL,
    /**< \brief RXN_Mem_Read_Poly() failed. */
    RXN_DEBUG_CODE_ERROR_MEM_READ_SEED_FAIL,
    /**< \brief RXN_Mem_Read_Seed() failed. */
    RXN_DEBUG_CODE_ERROR_MEM_WRITE_EPH_FAIL,
    /**< \brief RXN_Mem_Write_Ephemeris() failed. */
    RXN_DEBUG_CODE_ERROR_MEM_WRITE_EPH_INDEX_FAIL,
    /**< \brief RXN_Mem_Wite_Ephemeris_Index() failed. */
    RXN_DEBUG_CODE_ERROR_MEM_WRITE_POLY_FAIL,
    /**< \brief RXN_Mem_Write_Seed() failed. */
    RXN_DEBUG_CODE_ERROR_MEM_WRITE_SEED_FAIL,
    /**< \brief RXN_Mem_Write_Seed() failed. */
    RXN_DEBUG_CODE_ERROR_MEM_WRITE_CONFIG_FAIL,
    /**< \brief RXN_Mem_Write_Config() failed. */
    RXN_DEBUG_CODE_ERROR_MEM_READ_CONFIG_FAIL,
    /**< \brief RXN_Mem_Read_Config() failed. */
    RXN_DEBUG_CODE_ERROR_MEM_WRITE_FREQ_CHAN_FAIL,
    /**< \brief RXN_Mem_Write_Freq_Chan() failed. */
    RXN_DEBUG_CODE_ERROR_MEM_READ_FREQ_CHAN_FAIL,
    /**< \brief RXN_Mem_Read_Freq_Chan() failed. */
    RXN_DEBUG_CODE_ERROR_MEM_WRITE_BLOCK_TYPE_FAIL,
    /**< \brief RXN_Mem_Write_Block_Type() failed. */
    RXN_DEBUG_CODE_ERROR_MEM_READ_BLOCK_TYPE_FAIL,
    /**< \brief RXN_Mem_Read_Block_Type() failed. */
    RXN_DEBUG_CODE_ERROR_POLY_READ_FAIL,      
    /**< \brief Polynomial read failed. */                 
    RXN_DEBUG_CODE_ERROR_POLY_TIME_FAIL,
    /**< \brief Tried to use outside poly's range. */
    RXN_DEBUG_CODE_ERROR_SEED_CHECKSUM_FAIL,
    /**< \brief Seed failed check-sum. */
    RXN_DEBUG_CODE_ERROR_POLY_INDEX_RANGE,
    /**< \brief Poly index bounds error cf RXN_constants.h */
    RXN_DEBUG_CODE_ERROR_SEED_INDEX_RANGE,
    /**< \brief Seed index bounds error cf RXN_constants.h */
    RXN_DEBUG_CODE_ERROR_INPUT_ARGS,
    /**< \brief Generic error please see RXN_API.h for inputs */
    RXN_DEBUG_CODE_ERROR_NO_SEED_AVAILABLE,
    /**< \brief No seed available */
    RXN_DEBUG_CODE_ERROR_BAD_EPHEMERIS_CROSSCHECK_PREVIOUS_EPHEMERIS_FAIL,
    /**< \brief Crosscheck against previous ephemeris failed */
    RXN_DEBUG_CODE_ERROR_EPHEMERIS_URA_EXCEED_THRESHOLD,
    /**< \brief Broadcast ephemeris exceed the threshold. */
    RXN_DEBUG_CODE_ERROR_BAD_EPHEMERIS_CROSSCHECK_PGPS_FAIL,
    /**< \brief Ephemeris failed against crosscheck (with PGPS). */
    RXN_DEBUG_CODE_ERROR_EPHEMERIS_TOO_OLD,
    /**< \brief Broadcast ephemeris is too old, unable to generate SAGPS seed. */
    RXN_DEBUG_CODE_ERROR_EPHEMERIS_UNAVAILABLE,
    /**< \brief No broadcast ephemeris available, unable to generate SAGPS seed. */
    RXN_DEBUG_CODE_ERROR_UNHEALTHY_EPHEMERIS,
    /**< \brief Unhealhty ephemeris detected, assume re-phasing event & purge eph database. */
    RXN_DEBUG_CODE_ERROR_PROT_UNSUPPORTED,
    /**< \brief Over-air protocol not supported */
    RXN_DEBUG_CODE_ERROR_EVALUATION_LICENSE_FEATURE_NOT_SUPPORTED,
    /**< \brief Feature not supported by current software license */
    RXN_DEBUG_CODE_ERROR_INVALID_EPHEMERIS_DATA_OUT_OF_BOUNDS,
    /**< \brief Ephemeris contains data that is out of bounds. */
    RXN_DEBUG_CODE_ERROR_SEED_URA_TOO_HIGH,
    /**< \brief Seed prediction is too high. */
    RXN_DEBUG_CODE_ERROR_SEED_BLOCK_TYPE_UNKNOWN,
    /**< \brief Seed block type is unknown. */
  } RXN_debug_code_t;

  /**
   * \brief
   * A structure containing the debug information.
   *
   * \since
   * 1.2.0
   */
  typedef struct RXN_debug_info
  {
    S32 target_time;                                /**< \brief Echo of input time used when calling RXN_Generate_Assistance. */
    S32 first_num_eph;                              /**< \brief number of ephmerides used to mint seed in first buffer. */
    S32 first_seed_last_toe;                        /**< \brief Toe of oldest ephemeris used to mint seed in first buffer. */
    S32 first_seed_first_toe;                       /**< \brief Toe of newest ephemeris used to mint seed in first buffer.*/
    RXN_seed_information_t first_seed_data;         /**< \brief Meta data for first seed. */

    S32 second_num_eph;                             /**< \brief number of ephmerides used to mint seed in second buffer. */
    S32 second_seed_last_toe;                       /**< \brief Toe of oldest ephemeris used to mint seed in second buffer. */
    S32 second_seed_first_toe;                      /**< \brief Toe of newest ephemeris used to mint seed in second buffer.*/
    RXN_seed_information_t second_seed_data;        /**< \brief Meta data for second seed. */

    // move here for better packing
    U08 first_seed_URA;                             /**< \brief RXN proprietary index used to map into ICD200 URA (different for PGPS than for SA) in first buffer.*/
    U08 second_seed_URA;                            /**< \brief RXN proprietary index used to map into ICD200 URA (different for PGPS than for SA) in second buffer.*/
    U08 pad;                                        /**< \brief pad byte */
    U08 prn;                                        /**< \brief Index for the PRN for which the above applies. 1 to RXN_CONSTANT_NUM_GPS_PRNS + RXN_CONSTANT_NUM_GLONASS_PRNS. */
  } RXN_debug_information_t;

  /**
   * \brief
   * Get the seed information for last call to RXN_Generate_Assistance().
   *
   * \param prn 
   * [IN] The PRN to generate or update a prediction buffer for. Data is calculated
   * and stored independantly for each PRN.
   *
   *\param constel
   * [IN] Constellation for which to produce assistance (optinal param). Default GPS. Glonass not yet supported
   *
   * \return RXN_SUCCESS
   * If the function completes successfully.
   *
   * \return RXN_FAIL
   * If the function fails.
   *
   * \details
   * <b>Description</b>\n
   * This function returns the seed information associated with the last function
   * call. The information provides additional information for debugging the operation
   * of the library. You should call the function immediately 
   * when a function's return value indicates that such a call will 
   * return useful data.
   * 
   * \param information
   * [OUT] A pointer to a RXN_debug_information_t that will be updated with data
   * from the most recent operation. The information provides a snapshot of 
   * persisted data.
   *
   * <b>Applicable Standards</b>\n
   * Proprietary to GPStream (tm) PGPS (tm) client software, SAGPS (tm) client software
   * and GPStream (tm) PGPS (tm) server.
   *
   * \attention
   * This function is useful for troubleshooting problems with
   * RXN_Generate_Assistance() and RXN_Get_Ephemeris().
   *
   * \see
   * RXN_Debug_Message()
   *
   * \since
   * 1.2.0
   */  
  U16 RXN_Debug_Information(const U08 prn, const RXN_constel_t constel, RXN_debug_information_t* information);

#ifdef __cplusplus
} // extern "C"
#endif

#endif // RXN_DEBUG_H
