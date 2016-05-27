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
 * This header documents the core PGPS library function calls.
 *
 *************************************************************************
 * $LastChangedDate: 2012-09-25 13:39:10 -0700 (Tue, 25 Sep 2012) $
 * $Revision: 102526 $
 *************************************************************************
 *
 */

/**
 * \file
 * \brief
 * Unified application programming interface for Rx Networks predictive system.
 * 
 * \details
 * RXN API is a facade for users of the system. It hides the implementation 
 * details behind a simple API. The goal is to allow users to integrate 
 * Predictive GPS (PGPS (tm)) and Self-Assisted GPS (SAGPS (tm)) into their target
 * platform easily.  
 * 
 * \since
 * 1.1.0
 */
 
#ifndef RXN_API_H
#define RXN_API_H

#include "RXN_structs.h"
#include "RXN_constants.h"

#ifdef __cplusplus
extern "C" {
#endif


  /**
   * \brief 
   * Retrieve the API version.
   *
   * \param version [OUT] 
   * A memory location to be filled with the current API version.
   * This is a null terminated string and is at most 
   * RXN_CONSTANT_VERSION_STRING_LENGTH (50) characters long.
   *
   * \return RXN_SUCCESS 
   * If the version is returned successfully.
   * 
   * \return RXN_FAIL 
   * If the version cannot be returned.
   *
   * \details
   * <b>Description</b>\n
   * This function returns the version of the GPStream (tm) API library.
   * The version number will be incremented as new features, enhancements, 
   * and bug fixes are added to the library. The version number is an important 
   * identification when reporting and troubleshooting issues.
   * Rx Networks' revision numbering convention are described within the 
   * Integration User's Manuals.
   *
   * <b>Applicable Standards</b>\n
   * Proprietary to GPStream (tm) PGPS (tm) client software, SAGPS (tm) client software and
   * GPStream (tm) PGPS (tm) server.
   *
   * \since
   * 1.1.0
   *
   * \see
   * RXN_Get_Propagator_Version()
   *
   * \see
   * RXN_Get_Generator_Version()
   *
   * <b>Example Usage</b>
   * \code
   * // Declare a string to hold version information.
   * char version[RXN_CONSTANT_VERSION_STRING_LENGTH];
   * 
   * // Retrieve and output version information.
   * RXN_Get_API_Version(&version[0]);
   * printf("RXN_Get_API_Version(): %s\n", version);
   * \endcode
   *
   * <b>Output</b>\n
   * \code   
   * RXN_Get_API_Version(): RXN_PGPS_SDK_1.1.0.6-BETA
   * \endcode
   */
  U16 RXN_Get_API_Version( 
    char version[RXN_CONSTANT_VERSION_STRING_LENGTH] 
  );

  /**
   * \brief 
   * Retrieve the Propagator version. 
   *
   * \param version [OUT] 
   * A memory location to be filled with the current propagator version.
   * This is a null terminated string and is at most 
   * RXN_CONSTANT_VERSION_STRING_LENGTH (50) characters long.
   *
   * \return RXN_SUCCESS 
   * If the version is returned successfully.
   * 
   * \return RXN_FAIL 
   * If the version cannot be returned.
   *
   * \details
   * <b>Description</b>\n
   * This function returns the version of the propagator used within this 
   * revision of the GPStream (tm) product. The version number will be incremented
   * as new features, enhancements, and bug fixes are added to the library. 
   * The version number is an important identification when reporting and
   * troubleshooting issues. Rx Networks' revision numbering convention are 
   * described within the Integration User's Manuals.
   *
   * <b>Applicable Standards</b>\n
   * Proprietary to GPStream (tm) PGPS (tm) client software, SAGPS (tm) client software and
   * GPStream (tm) PGPS (tm) server.
   *
   * \since
   * 1.1.0
   *
   * \see
   * RXN_Get_API_Version()
   * 
   * \see
   * RXN_Get_Generator_Version()
   *
   * <b>Example Usage</b>
   * \code
   * // Declare a string to hold version info.
   * char version[RXN_CONSTANT_VERSION_STRING_LENGTH];
   * 
   * // Retrieve and output version information.
   * RXN_Get_Propagator_Version(&version[0]);
   * printf("RXN_Get_Propagator_Version(): %s\n", version);
   * \endcode
   *
   * <b>Output</b>\n
   * \code
   * RXN_Get_Propagator_Version(): 1.0.1.10-BETA-2
   * \endcode
   */
  U16 RXN_Get_Propagator_Version( 
    char version[RXN_CONSTANT_VERSION_STRING_LENGTH] 
  );

  /**
   * \brief 
   * Retrieve the Generator version. 
   *
   * \param version [OUT] 
   * A memory location to be filled with the current generator version.
   * This is a null terminated string and is at most 
   * RXN_CONSTANT_VERSION_STRING_LENGTH (50) characters long.
   *
   * \return RXN_SUCCESS 
   * If the version is returned successfully.
   * 
   * \return RXN_FAIL 
   * If the version cannot be returned.
   *
   * \details
   * <b>Description</b>\n
   * This function returns the version of the generator used within this 
   * revision of the GPStream (tm) product. The version number will be incremented
   * as new features, enhancements, and bug fixes are added to the library. 
   * The version number is an important identification when reporting and
   * troubleshooting issues. Rx Networks' revision numbering convention are 
   * described within the Integration User's Manuals.
   *
   * <b>Applicable Standards</b>\n
   * Proprietary to GPStream (tm) PGPS (tm) client software, SAGPS (tm) client software and
   * GPStream (tm) PGPS (tm) server.
   *
   * \since
   * 1.2.0
   *
   * \see
   * RXN_Get_API_Version()
   *
   * \see
   * RXN_Get_Propagator_Version()
   *
   * <b>Example Usage</b>
   * \code
   * // Declare a string to hold version info.
   * char version[RXN_CONSTANT_VERSION_STRING_LENGTH];
   * 
   * // Retrieve and output version information.
   * RXN_Get_Generator_Version(&version[0]);
   * printf("RXN_Get_Generator_Version(): %s\n", version);
   * \endcode
   *
   * <b>Output</b>\n
   * \code
   * RXN_Get_Generator_Version(): 1.0.1.10-BETA-2
   * \endcode
   */
  U16 RXN_Get_Generator_Version( 
    char version[RXN_CONSTANT_VERSION_STRING_LENGTH] 
  );

  /**
   * \brief 
   * Provide a block of memory (volatile) for caching to improve the
   * performance when generating the prediction buffer.
   *
   * \param cache [IN] 
   * The pointer to a block of memory to be used as cache for the propagator.
   *
   * \param size [IN] 
   * The size of the allocated cache memory in bytes.
   *
   * \return RXN_SUCCESS
   * If the function completes successfully.
   *
   * \return RXN_DEBUG_CODE_ERROR_NULL_ARGUMENT
   * If cache is a NULL pointer.
   *
   * \return RXN_DEBUG_CODE_ERROR_CACHE_SIZE_TOO_SMALL
   * If size is smaller than RXN_CONSTANT_MIN_CACHE_SIZE.
   * 
   * \return RXN_FAIL
   * Other errors.
   *
   * \details 
   * When generating prediction buffer for multiple PRNs, values calculated
   * for the first PRN can be partially reused for subsequent PRNs for the same
   * generation/propagation period.
   *
   * In the current release, the cache is used mainly to store the sun and moon 
   * position during a 4 hour period. This cached values can be re-used
   * after when generating prediction buffer for subsequent PRNs. Data cached
   * from each 4 hour block is approximately 7kB.
   * For PGPS (tm), 1 x 4Hr block is cached and therefore a 7kB cache is required.
   * The cache size is defined in RXN_CONSTANT_MIN_CACHE_SIZE.
   * For SAGPS (tm), 24 Hrs or 6 x 4 Hr blocks are cached requiring 42 kB.
   * The cache size is defined in RXN_CONSTANT_MAX_CACHE_SIZE.
   * Our benchmark has shown this strategy can improve the performance
   * significantly especially on a processor without a built-in FPU/VFP.
   *
   * It should be noted that not every target system has available free memory.
   * For these systems caching may not be supported. Because caching simply
   * enhances performance, its use is optional.

   * <b>Applicable Standards</b>\n
   * Proprietary to GPStream (tm) PGPS (tm) client software, SAGPS (tm) client software.
   *
   * \since
   * 1.1.0
   *
   * \see
   * RXN_Uninitialize_Cache()
   *
   * <b>Example Usage</b>
   * \code
   * // SAGPS (tm) Example : Initialize 42kB cache.
   * // Initialize should occur at application startup.
   * RXN_Initialize_Cache(RXN_CONSTANT_MAX_CACHE_SIZE);
   *
   * // PGPS (tm) Example : Initialize ~7kB cache.
   * // Initialize should occur at application startup.
   * RXN_Initialize_Cache(RXN_CONSTANT_MIN_CACHE_SIZE);
   * \endcode
   */
  U16 RXN_Initialize_Cache(
    U08* cache, 
    const U32 size
  );

  /**
   * \brief 
   * Release and reclaim cache memory.
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
   * \since
   * 1.1.0
   *
   * \see
   * RXN_Initialize_Cache()
   *
   * <b>Example Usage</b>
   * \code
   * // Un-initialize should occur at application shutdown.
   * if(RXN_Uninitialize_Cache() != RXN_SUCCESS)
   * {
   *    // Handle Errors.
   * }
   * \endcode
   */
  U16 RXN_Uninitialize_Cache(void);

  /**
   * \deprecated
   * This function is maintained for backwards compatibility and will be 
   * removed in a future release.
   *
   * \brief 
   * Generate or update a prediction buffer including seed and polynomial data.
   *
   * \param prn [IN]
   * The PRN to generate or update a prediction buffer for. Data is calculated
   * and stored independently for each PRN.
   *
   * \param targetTime [IN]
   * How far into the future the prediction buffer will be valid for. This time
   * is represented by the number of seconds since the start of GPS time. A GPS
   * time of 0 is equivalent to January 6, 1980 00:00:00 UTC. When the function
   * completes, EE may be acquired from the prediction buffer up and until the
   * targetTime specified.
   *
   * \param constel [IN]
   * The constellation to produce assistance. Must be one of the types of
   * RXN_constel.
   *
   * \return RXN_SUCCESS
   * If the function completes successfully.
   *
   * \return RXN_DEBUG_CODE_ERROR_INVALID_PRN
   * If prn is an invalid number.
   *
   * \return RXN_DEBUG_CODE_ERROR_EPHEMERIS_UNAVAILABLE
   * No ephemeris is available to generate SAGPS seed.
   *
   * \return RXN_DEBUG_CODE_ERROR_EVALUATION_LICENSE_EXPIRED
   * If evaluation license is expired.
   *
   * \return RXN_DEBUG_CODE_ERROR_MEM_READ_EPH_INDEX_FAIL
   * If RXN_Mem_Read_Ephemeris_Index() abstraction call fails.
   *
   * \return RXN_DEBUG_CODE_ERROR_MEM_READ_EPH_FAIL
   * If RXN_Mem_Read_Ephemeris() abstraction call fails.
   *
   * \return RXN_DEBUG_CODE_ERROR_MEM_WRITE_SEED_FAIL
   * If RXN_Mem_Write_Seed() abstraction call fails.
   *
   * \return RXN_DEBUG_CODE_ERROR_MEM_WRITE_POLY_FAIL
   * If RXN_Mem_Write_Poly() abstraction call fails.
   *
   * \return RXN_FAIL
   * Other errors.
   *
   * \details
   * <b>SAGPS (tm) Seed Generation or Update</b>
   * For SAGPS (tm) based systems, the first process in generating or updating a
   * prediction buffer is the generation or update of a model seed using data
   * from the BCE Store.
   * Seeds are generated or updated by referencing the TOE within the BCE
   * records within the BCE Store and by referencing seed start times. If no
   * seed exists and at least one BCE record exists within the Store, a
   * prediction buffer seed is generated. If a seed exists, the start time for
   * the seed is compared with the TOE value within the last stored BCE record
   * within the BCE Store. If the latest BCE record TOE is more that 14 hours
   * newer than the seed start time, and if the targetTime requested is not
   * more than 72 hours greater than the TOE within the latest BCE record in the
   * store, then the prediction buffer is updated. If not, no update occurs.
   * When an update occurs, the following rules will determine if 1 or 2 BCE
   * records will be used for the seed update.
   * \li If the latest and second latest BCE records within the BCE store are
   * more than 50 hours apart, 1 BCE record (the latest) will be used for the
   * update.
   * \li Else If the latest and earliest BCE records within the BCE store are
   * less than 18 hours apart, 1 BCE records will be used for the update.
   * \li Else 2 BCE records will be used for the update.
   * If a seed update occurs, a subsequent prediction buffer update can occur.
   * When no seed update occurs, no such prediction buffer update occurs. Seed
   * generation will occur during the first call to RXN_Generate_Assistance()
   * when iterative calls are made
   * (see the <i>Propagation</i> section below). When a seed is generated or
   * updated, its start time will be set to the TOE of the last BCE record used
   * in its creation. This start time can be altered using the
   * RXN_Set_Toe_Buffer_Offset()
   *
   * <b>PGPS (tm) Seed Download</b>
   * In contrast to SAGPS (tm), PGPS (tm) utilizes an available IP connection to
   * download a seed from the Rx Networks server instead of generating a seed
   * using BCE records as observations.
   *
   * <b>Propagation</b>
   * After seed download (PGPS (tm)) or generation/update (SAGPS (tm)) seed propagation
   * (prediction buffer update) can occur. Seed data is used to update the
   * Poly Store with PRN position predictions. The seed is propagated out in
   * time in 4 hour blocks. Integration applications are expected to call
   * RXN_Generate_Assistance() iteratively for each PRN to propagate out
   * that PRN's seed 4 hours with each call. With each iteration, the targetTime
   * should increase by 4 hours. Iterations should continue until the seed has
   * been fully propagated into the future (i.e. prediction buffer update)
   * sufficiently to support acquisition of EE from the prediction buffer as
   * required. For PGPS (tm) systems, this will typically be 7 days or 42 x 4 hour
   * blocks (42 iterations of RXN_Generate_Assistance()). For SAGPS (tm) systems,
   * this will typically be 1 day or 6 x 4 hour blocks (when a seed has been
   * created or updated with only 1 BCE record) or 3 days or 18 x 4 hour blocks
   * (when a seed has been created or updated with 2 BCE records). For SAGPS (tm),
   * integration code can propagate 6 x 4 hour blocks to get a 24 hour
   * prediction buffer and then integration code can call
   * RXN_Get_Seed_Information() to determine if 1 or 2 BCE records were used to
   * create the seed. If 1 BCE record was used, the seed propagation is
   * complete. If 2 BCE records were used, seed propagation can continue until
   * 18 x 4 hour blocks have been propagated.

   * <b>Performance</b>
   * Seed generation can require significant processing time for SAGPS (tm) systems.
   * Moreover, seed propagation can also require non-trivial processing time.
   * Detailed SAGPS (tm)/PGPS (tm) performance figures (i.e. empirically determined
   * processing times) are available from Rx Networks within benchmark
   * performance documents for these products. Consult these references when
   * determining processing requirements for any target system.
   *
   * <b>Seed Detail</b>
   * Information on downloaded, generated or updated seeds can be acquired using
   * RXN_Get_Seed_Information().
   * 
   * <b>Applicable Standards</b>\n
   * Proprietary to GPStream (tm) PGPS (tm) client software, SAGPS (tm) client software.
   *
   * \since
   * 1.1.0
   *
   * \see
   * RXN_Get_Seed_Information()
   *
   * \see
   * RXN_Set_Toe_Buffer_Offset()
   *
   * \see
   * RXN_Set_Abort()
   *
   * <b>Example Usage</b>
   * \code
   * // SAGPS (tm) Example - See the Rx Networks Example Integration Application for full details:
   *
   * // Setup an array that will track if there has been any BCE change.
   * U08 ChangedPRNArr[RXN_CONSTANT_NUM_PRNS];
   *
   * // Psuedo Code - Whenever the receiver provides a BCE record that has a TOE that is
   * // different from previously processed BCE records (for a specific PRN), set the
   * // BCE within the SAGPS (tm) BCE Store (RXN_Set_Ephemeris), and then set ChangedPRNArr[PRN-1]
   * // to a non-zero value to indicate that a BCE record has changed and EE generation should
   * // occur for that PRN.
   * 
   * // Loop through and generate EE for each PRN. Complete generation for each PRN before
   * // moving on to the next as appreciable time will be required to generate a seed
   * // before creation of an EE prediction buffer. Once EE prediction buffer generation
   * // has been started (after seed generation), a relatively  short time is required for
   * // completion.
   * for(x=0; x<RXN_CONSTANT_NUM_PRNS; x++)
   * {
   *    // Only generate EE if there has been a BCE change. Otherwise, handle the next PRN.
   *    if(ChangedPRNArr[x] == 0)
   *    {
   *      continue;
   *    }
   *  
   *    // Loop through 4-hour blocks.
   *    U16 Result;
   *    for(y=4; y<=gProp_Hours; y=y+4)
   *    {
   *      // Generate Assistance for the current PRN and block.
   *      Result = RXN_Generate_Assistance(x+1, GPSTime + (y*3600));
   *
   *      if(Result != RXN_SUCCESS)
   *      {
   *        // Handle Errors.
   *      }
   *    }
   * }
   * \endcode
   */
  U16 RXN_Generate_Assistance(
    const U08 prn,
    const U32 targetTime,
    RXN_constel_t constel
  );
  
  /**
   * \brief 
   * Non-Blocking version of the RXN_Generate_Assistance() function.
   * Generate or update a prediction buffer including seed and polynomial data.
   *
   * \param prn [IN]
   * The PRN to generate or update a prediction buffer for. Data is calculated
   * and stored independently for each PRN.
   *
   * \param targetTime [IN]
   * How far into the future the prediction buffer will be valid for. This time
   * is represented by the number of seconds since the start of GPS time. A GPS
   * time of 0 is equivalent to January 6, 1980 00:00:00 UTC. When the function
   * completes, EE may be acquired from the prediction buffer up and until the
   * targetTime specified.
   *
   * \param constel [IN]
   * The constellation to produce assistance. Must be one of the types of
   * RXN_constel.
   *
   * \param state [IN][OUT] 
   * A RXN_state_t structure to maintain the state of the system. This API has to
   * be called continuously in a loop. When the API is called for the first time, 
   * the status member of the state(state.status) must be initialized to 
   * RXN_STATUS_START. When the function returns, the state.status member 
   * must be checked for completion of execution. If state.status is 
   * RXN_STATUS_DONE, the execution is complete. If the state.status is 
   * RXN_STATUS_PENDING, the API has to be called again with same set of 
   * arguments for 'prn' and 'targetTime' until state.status becomes 
   * RXN_STATUS_DONE.   
   *
   * \return RXN_SUCCESS 
   * If the function completes successfully. The return value is valid only when the state.status
   * is RXN_STATUS_DONE.
   *
   * \return RXN_DEBUG_CODE_ERROR_NULL_ARGUMENT
   * If state is a NULL pointer.
   *
   * \return RXN_DEBUG_CODE_ERROR_INVALID_PRN
   * If prn is an invalid number.
   *
   * \return RXN_DEBUG_CODE_ERROR_EPHEMERIS_UNAVAILABLE
   * No ephemeris is available to generate SAGPS seed.
   *
   * \return RXN_DEBUG_CODE_ERROR_EVALUATION_LICENSE_EXPIRED
   * If evaluation license is expired.
   *
   * \return RXN_DEBUG_CODE_ERROR_MEM_READ_EPH_INDEX_FAIL
   * If RXN_Mem_Read_Ephemeris_Index() abstraction call fails.
   *
   * \return RXN_DEBUG_CODE_ERROR_MEM_READ_EPH_FAIL
   * If RXN_Mem_Read_Ephemeris() abstraction call fails.
   *
   * \return RXN_DEBUG_CODE_ERROR_MEM_WRITE_SEED_FAIL
   * If RXN_Mem_Write_Seed() abstraction call fails.
   *
   * \return RXN_DEBUG_CODE_ERROR_MEM_WRITE_POLY_FAIL
   * If RXN_Mem_Write_Poly() abstraction call fails.
   *
   * \return RXN_FAIL
   * Other errors.
   *
   * \details
   * <b>SAGPS (tm) Seed Generation or Update</b>
   * For SAGPS (tm) based systems, the first process in generating or updating a
   * prediction buffer is the generation or update of a model seed using data
   * from the BCE Store.
   * Seeds are generated or updated by referencing the TOE within the BCE
   * records within the BCE Store and by referencing seed start times. If no
   * seed exists and at least one BCE record exists within the Store, a
   * prediction buffer seed is generated. If a seed exists, the start time for
   * the seed is compared with the TOE value within the last stored BCE record
   * within the BCE Store. If the latest BCE record TOE is more that 14 hours
   * newer than the seed start time, and if the targetTime requested is not
   * more than 72 hours greater than the TOE within the latest BCE record in the
   * store, then the prediction buffer is updated. If not, no update occurs.
   * When an update occurs, the following rules will determine if 1 or 2 BCE
   * records will be used for the seed update.
   * \li If the latest and second latest BCE records within the BCE store are
   * more than 50 hours apart, 1 BCE record (the latest) will be used for the
   * update.
   * \li Else If the latest and earliest BCE records within the BCE store are
   * less than 18 hours apart, 1 BCE records will be used for the update.
   * \li Else 2 BCE records will be used for the update.
   * If a seed update occurs, a subsequent prediction buffer update can occur.
   * When no seed update occurs, no such prediction buffer update occurs. Seed
   * generation will occur during the first call to RXN_Generate_Assistance()
   * when iterative calls are made
   * (see the <i>Propagation</i> section below). When a seed is generated or
   * updated, its start time will be set to the TOE of the last BCE record used
   * in its creation. This start time can be altered using the
   * RXN_Set_Toe_Buffer_Offset()
   *
   * <b>PGPS (tm) Seed Download</b>
   * In contrast to SAGPS (tm), PGPS (tm) utilizes an available IP connection to
   * download a seed from the Rx Networks server instead of generating a seed
   * using BCE records as observations.
   *
   * <b>Propagation</b>
   * After seed download (PGPS (tm)) or generation/update (SAGPS (tm)) seed propagation
   * (prediction buffer update) can occur. Seed data is used to update the
   * Poly Store with PRN position predictions. The seed is propagated out in
   * time in 4 hour blocks. Integration applications are expected to call
   * RXN_Generate_Assistance() iteratively for each PRN to propagate out
   * that PRN's seed 4 hours with each call. With each iteration, the targetTime
   * should increase by 4 hours. Iterations should continue until the seed has
   * been fully propagated into the future (i.e. prediction buffer update)
   * sufficiently to support acquisition of EE from the prediction buffer as
   * required. For PGPS (tm) systems, this will typically be 7 days or 42 x 4 hour
   * blocks (42 iterations of RXN_Generate_Assistance()). For SAGPS (tm) systems,
   * this will typically be 1 day or 6 x 4 hour blocks (when a seed has been
   * created or updated with only 1 BCE record) or 3 days or 18 x 4 hour blocks
   * (when a seed has been created or updated with 2 BCE records). For SAGPS (tm),
   * integration code can propagate 6 x 4 hour blocks to get a 24 hour
   * prediction buffer and then integration code can call
   * RXN_Get_Seed_Information() to determine if 1 or 2 BCE records were used to
   * create the seed. If 1 BCE record was used, the seed propagation is
   * complete. If 2 BCE records were used, seed propagation can continue until
   * 18 x 4 hour blocks have been propagated.

   * <b>Performance</b>
   * Seed generation can require significant processing time for SAGPS (tm) systems.
   * Moreover, seed propagation can also require non-trivial processing time.
   * Detailed SAGPS (tm)/PGPS (tm) performance figures (i.e. empirically determined
   * processing times) are available from Rx Networks within benchmark
   * performance documents for these products. Consult these references when
   * determining processing requirements for any target system.
   *
   * <b>Seed Detail</b>
   * Information on downloaded, generated or updated seeds can be acquired using
   * RXN_Get_Seed_Information().
   * 
   * <b>Applicable Standards</b>\n
   * Proprietary to GPStream (tm) PGPS (tm) client software, SAGPS (tm) client software.
   *
   * \since
   * 3.1.0
   *
   * \see
   * RXN_Get_Seed_Information()
   *
   * \see
   * RXN_Set_Toe_Buffer_Offset()
   *
   * \see
   * RXN_Set_Abort()
   *
   * <b>Example Usage</b>
   * \code
   * // SAGPS™ Example - See the RX Networks Example Integration App for full details:
   *
   * // Setup an array that will track if there has been any BCE change.
   * U08 ChangedPRNArr[RXN_CONSTANT_NUM_PRNS];
   *
   * // Psuedo Code - Whenever the receiver provides a BCE record that has a TOE that is
   * // different from previously processed BCE records (for a specific PRN), set the
   * // BCE within the SAGPS™ BCE Store (RXN_Set_Ephemeris), and then set ChangedPRNArr[PRN-1]
   * // to a non-zero value to indicate that a BCE record has changed and EE generation should
   * // occur for that PRN.
   * 
   * // Loop through and generate EE for each PRN. Complete generation for each PRN before
   * // moving on to the next as appreciable time will be required to generate a seed
   * // before creation of an EE prediction buffer. Once EE prediction buffer generation
   * // has been started (after seed generation), a relatively  short time is required for
   * // completion.
   * for(x=0; x<RXN_CONSTANT_NUM_PRNS; x++)
   * {
   *    // Only generate EE if there has been a BCE change. Otherwise, handle the next PRN.
   *    if(ChangedPRNArr[x] == 0)
   *    {
   *      continue;
   *    }
   *  
   *    // Loop through 4-hour blocks.
   *    U16 Result;
   *	RXN_state_t state;
   *    for(y=4; y<=gProp_Hours; y=y+4)
   *    {
   *      // Generate Assistace for the current PRN and block.
   *	  state.status = RXN_STATUS_START;
   *	  do
   *	  {
   *      	Result = RXN_Generate_Assistance_NB(x+1, GPSTime + (y*3600), &state);
   *      }
   *      while(state.status == RXN_STATUS_PENDING);
   *
   *      if(Result != RXN_SUCCESS)
   *      {
   *        // Handle Errors.
   *      }
   *    }
   * }
   * \endcode
   */
  U16 RXN_Generate_Assistance_NB(
    const U08 prn,
    const U32 targetTime,
    RXN_constel_t constel,
    RXN_state_t* state
  );

  /**
   * \brief 
   * Get EE from the prediction buffer. In ECEF WGS84 for GPS/Glonass
   *
   * \param prn [IN]
   * The PRN to get EE for.
   *
   * \param time [IN]
   * The current GPS time. This time is represented by the number of seconds
   * since the start of GPS time. A GPS time of 0 is equivalent to January 6,
   * 1980 00:00:00 UTC.
   *
   * \param ephemeris [OUT]
   * A RXN_ephem_t structure with current EE for the specified PRN. This EE will
   * be scaled to match broadcast ephemeris as specified within the IS-GPS-200D
   * spec.
   *
   * \param constel [IN]
   * The constellation to get ephemeris. Must be one of the types of
   * RXN_constel.
   *
   * \return RXN_SUCCESS
   * If the function succeeds in getting EE for the specified PRN.
   * 
   * \return RXN_DEBUG_CODE_ERROR_NULL_ARGUMENT
   * If ephemeris is a NULL pointer.
   *
   * \return RXN_DEBUG_CODE_ERROR_INVALID_PRN
   * If prn is an invalid number.
   *
   * \return RXN_DEBUG_CODE_ERROR_NO_SEED_AVAILABLE
   * If no PGPS seed or SAGPS seed is available.
   *
   * \return RXN_DEBUG_CODE_ERROR_SEED_CURRENT_TIME_LESS_THAN_TARGET_TIME
   * If the seed current time is less than the target time, meaning the seed
   * has not been propagated sufficiently to generate assistance data 
   * required for the target time.
   *
   * \return RXN_DEBUG_CODE_ERROR_POLY_TIME_FAIL
   * If the polynomial data does not correspond to the expected time.
   *
   * \return RXN_DEBUG_CODE_ERROR_MEM_READ_POLY_FAIL
   * If RXN_Mem_Read_Poly() abstraction function call fails.
   *
   * \return RXN_DEBUG_CODE_ERROR_EVALUATION_LICENSE_EXPIRED
   * If evaluation license is expired.
   *
   * \return RXN_FAIL
   * Other errors.
   *
   * \details
   * For EE to be available for the specified PRN, a prediction buffer for the
   * PRN must be available and must span the GPS time specified. To validate
   * that a prediction buffer exists for the intended time, integration code can
   * utilize RXN_Get_Seed_Information(). If the current GPS time is before the
   * start time or after the current time of the seed within the prediction
   * buffer no EE data is available. The current time should never be before
   * the start time of the current seed if an appropriate offset has been
   * set using RXN_Set_Toe_Buffer_Offset().
   * 
   * <b>Applicable Standards</b>\n
   * Proprietary to GPStream (tm) PGPS (tm) client software, SAGPS (tm) client software.
   *
   * \since
   * 1.1.0
   *
   * \see
   * RXN_Get_Seed_Information()
   *
   * \see
   * RXN_Set_Toe_Buffer_Offset()
   *
   * <b>Example Usage</b>
   * \code
   * // Example. Utilize a flag to signal when EE is required by the GPS receiver.
   * // This flag must be set by integration code when the GPS receiver needs EE.
   * bool needEE;
   *
   * // Wait untiled signalled by the GPS reciever that EE is required. The needEE
   * // signal may be checked within a thread loop or timer event as required by
   * // the integration application.
   * if(needEE)
   * {
   *    // Get the current GPS time (num sec since GPS start at midnight on
   *    // Jan 6, 1980).
   *    U32 CurrGPSTime = GetGPSTime();
   *
   *    // Setup an array to store EE (ensure array elements are clear).
   *    RXN_ephem_t EEArr[RXN_CONSTANT_NUM_PRNS];
   *    memset(EEArr, 0, sizeof(EEArr));
   *
   *    // Get EE from SAGPS (tm) or PGPS (tm) for each PRN.
   *    for(int x=0; x< RXN_CONSTANT_NUM_PRNS; x++)
   *    {
   *      if(RXN_Get_Ephemeris(x+1, CurrGPSTime, &(EEArr[x+1]))!= RXN_SUCCESS)
   *      {
   *        // Handle Errors.
   *      }
   *    }
   *
   *    // Inject each element within EEArr that has a non-zero PRN.
   *    for(int x=0; x< RXN_CONSTANT_NUM_PRNS; x++)
   *    {
   *      if(EEArr.prn != 0)
   *      {
   *        // Inject EEArr[x] into the GPS reciever for PRN = x+1.
   *        // Scale EEArr elements into the range required by the reciever.
   *      }
   *    }
   * }
   * \endcode
   *
   */
  U16 RXN_Get_Ephemeris(
    const U08 prn,
    const U32 time,
    RXN_ephem_t* ephemeris,
    RXN_constel_t  constel
  );

  /**
   * \brief 
   * Get EE from the prediction buffer for Glonass in ECEF PZ-90.
   *
   * \param slot [IN]
   * The slot number to get EE for.
   *
   * \param time [IN]
   * The current Glonass time in seconds.  
   *   time = [(N4-1)*1461 + (NT-1)]*86400 + tb*900 [sec]
   *
   * Using params as in Table 4.9 Glonass ICD 5.1:
   *   N4: range bf1, ..., 31    [4 year interval] 5 bits 
   *   NT: range bf1, ..., 1461  [days] 11 bits
   *   tb: range 15,  ..., 1425  [units of 15 minutes] 7 bits with scale factor 15 min
   *
   * \param ephemeris [OUT]
   * A RXN_glonass_ephem_t structure with current EE for the specified slot. This EE will
   * be scaled to match broadcast ephemeris as specified within the Glonas ICD 5.1
   * spec.
   *
   * \return RXN_SUCCESS
   * If the function succeeds in getting EE for the specified PRN.
   * 
   * \return RXN_FAIL
   * If the function fails. EE may not be available within the prediction buffer
   * at the GPS time specified for the PRN specified.
   *
   *
   * \details
   * For EE to be available for the specified slot, a prediction buffer for the
   * slot must be available and must span the Glonass time specified. To validate
   * that a prediction buffer exists for the intended time, integration code can
   * utilize RXN_Get_Seed_Information(). If the current Glonass time is before the
   * start time or after the current time of the seed within the prediction
   * buffer no EE data is available. The current time should never be before
   * the start time of the current seed.
   * 
   * <b>Applicable Standards</b>\n
   * Proprietary to GPStream (tm) PGPS (tm) client software, SAGPS (tm) client software.
   *
   * \since
   * 6.2.0
   *
   * \see
   * RXN_Get_Seed_Information()

   *
   * <b>Example Usage</b>
   * \code
   * // Example. Utilize a flag to signal when EE is required by the Glonass receiver.
   * // This flag must be set by integration code when the Glonass receiver needs EE.
   * bool needEE;
   *
   * // Wait untiled signalled by the Glonass reciever that EE is required. The needEE
   * // signal may be checked within a thread loop or timer event as required by
   * // the integration application.
   * if(needEE)
   * {
   *    // Get the current Glonass time    
   *    U32 CurrGlonassTime = GetGlonassTime();
   *
   *    // Setup an array to store EE (ensure array elements are clear).
   *    RXN_glonass_ephem_t EEArr[RXN_CONSTANT_NUM_GLONASS_PRNS];
   *    memset(EEArr, 0, sizeof(EEArr));
   *
   *    // Get EE from SAGPS (tm) or PGPS (tm) for each slot.
   *    for(int x=0; x< RXN_CONSTANT_NUM_GLONASS_PRNS; x++)
   *    {
   *      if(RXN_Get_Glonass_Ephemeris(x+1, CurrGlonassTime, &(EEArr[x+1]))!= RXN_SUCCESS)
   *      {
   *        // Handle Errors.
   *      }
   *    }
   *
   *    // Inject each element within EEArr that has a non-zero slot.
   *    for(int x=0; x< RXN_CONSTANT_NUM_GLONASS_PRNS; x++)
   *    {
   *      if(EEArr.prn != 0)
   *      {
   *        // Inject EEArr[x] into the Glonass reciever for slot = x+1.
   *        // Scale EEArr elements into the range required by the reciever.
   *      }
   *    }
   * }
   * \endcode
   *
   */
  U16 RXN_Get_Glonass_Ephemeris(
    const U08 slot,
    const U32 time,
    RXN_glonass_ephem_t* ephemeris    
  );

  /**
   * \brief 
   * Update the GPS ephemeris Store within SAGPS (tm) an update seed clock values within
   * PGPS (tm).
   *
   * \param ephemeris [IN]
   * A RXN_ephem_t structure with a BCE record (within included PRN
   * specification).
   *
   * \return RXN_SUCCESS
   * If the function succeeds in storing a BCE record or updating seed clock
   * values.
   * 
   * \return RXN_FAIL
   * If the function fails.
   *
   * \details
   * <b>SAGPS (tm)</b>\n
   * SAGPS (tm) systems utilize BCE records within the BCE Store to build prediction
   * buffers. This method supports storage of these records. If a record already
   * exists within the store for the time that the ephemeris BCE record
   * describes, this function will simply return without altering the store.
   * 
   * <b>PGPS (tm)</b>\n
   * PGPS (tm) systems utilize downloaded seeds within their prediction buffer.
   * Clock values within these seeds can support accurate EE for approximately
   * 3.5 days. After 3.5 days, EE clock values will deteriorate. To address
   * this, PGPS (tm) systems can update clocks within seeds by downloading clock
   * updates from the PGPS (tm) server. An alternative is to update clock data
   * within seeds using clock data found within BCE data. RXN_Set_Ephemeris()
   * support such updates. When RXN_Set_Ephemeris is called, clock data within
   * ephemeris will be used to update the seed (corresponds to the PRN value
   * within ephemeris). Seed age (i.e. whether it is older than 3.5 days can be
   * determined using RXN_Get_Seed_Information()).
   *
   * <b>Applicable Standards</b>\n
   * Proprietary to GPStream (tm) PGPS (tm) client software, SAGPS (tm) client software.
   *
   * \since
   * 1.1.0
   *
   * \see
   * RXN_Get_Seed_Information()
   *
   * <b>Example Usage</b>
   * \code
   * // Example. Utilize a flag to signal when new BCE is available from the GPS receiver.
   * // This flag must be set by integration code.
   * bool newBCEAvailable;
   *
   * // Wait until signaled by the integration application that new BCE is available.
   * // signal may be checked within a thread loop or timer event as required by
   * // the integration application.
   * if(newBCEAvailable)
   * {
   *    // Storage for each BCE record.
   *    RXN_ephem_t newBCE;
   *
   *    // Loop through all PRNs.
   *    for(int x=0; x< RXN_CONSTANT_NUM_PRNS; x++)
   *    {
   *      // Get BCE from the receiver. The following example application would return
   *      // "false" if BCE had not changed for the specified PRN (first param).
   *      if(GetBCE(x+1, &newBCE) == false)
   *      {
   *        continue;
   *      }
   *
   *      // Set the BCE within SAGPS (tm)/PGPS (tm).
   *      if(RXN_Set_Ephemeris(newBCE)!= RXN_SUCCESS)
   *      {
   *        // Handle Errors.
   *      }
   *    }
   *  }
   * \endcode
   */
  U16 RXN_Set_Ephemeris(
    const RXN_ephem_t* ephemeris
  );

  /**
   * \brief 
   * Update the Glonass BCE Store within SAGPS (tm) an update seed clock values within
   * PGPS (tm).
   *
   * \param ephemeris [IN]
   * A RXN_glonass_ephem_t structure with a Glonass record (within included PRN
   * specification).
   *
   * \return RXN_SUCCESS
   * If the function succeeds in storing a BCE record or updating seed clock
   * values.
   * 
   * \return RXN_FAIL
   * If the function fails.
   *
   * \details
   * <b>SAGPS (tm)</b>\n
   * SAGPS (tm) systems utilize Glonass records within the Glonass Store to build 
   * prediction buffers. This method supports storage of these records. If a record 
   * already exists within the store for the time that the ephemeris BCE record
   * describes, this function will simply return without altering the store.
   * 
   * <b>PGPS (tm)</b>\n
   * PGPS (tm) systems utilize downloaded seeds within their prediction buffer.
   * Clock values within these seeds can support accurate EE for approximately
   * 3.5 days. After 3.5 days, EE clock values will deteriorate. To address
   * this, PGPS (tm) systems can update clocks within seeds by downloading clock
   * updates from the PGPS (tm) server. An alternative is to update clock data
   * within seeds using clock data found within BCE data. RXN_Set_Glonass_Ephemeris()
   * support such updates. When RXN_Set_Glonass_Ephemeris is called, clock data within
   * ephemeris will be used to update the seed (corresponds to the PRN value
   * within ephemeris). Seed age (i.e. whether it is older than 3.5 days can be
   * determined using RXN_Get_Seed_Information()).
   *
   * <b>Applicable Standards</b>\n
   * Proprietary to GPStream (tm) PGPS (tm) client software, SAGPS (tm) client software.
   *
   * \attention 
   * Sign of af0 is -tauN
   *
   * \see
   * RXN_Get_Seed_Information()
   *
   * \since
   * 4.0.0
   *
   * <b>Example Usage</b>
   * \code
   * // Example. Utilize a flag to signal when new Globass record is available from the 
   * Glonass receiver.
   * // This flag must be set by integration code.
   * bool newRecordAvailable;
   *
   * // Wait until signaled by the integration application that new BCE is available.
   * // signal may be checked within a thread loop or timer event as required by
   * // the integration application.
   * if(newRecordAvailable)
   * {
   *    // Storage for each Glonass record.
   *    RXN_glonass_ephem_t newRecord;
   *
   *    // Loop through all PRNs.
   *    for(int x=0; x< RXN_CONSTANT_NUM_GLONASS_PRNS; x++)
   *    {
   *      // Get glonass record from the receiver. The following example application would return
   *      // "false" if the record had not changed for the specified PRN (first param).
   *      if(GetGlonassRecord(x+1, &newRecord) == false)
   *      {
   *        continue;
   *      }
   *
   *      // Set the record within SAGPS (tm)/PGPS (tm).
   *      if(RXN_Set_Glonass_Ephemeris(newRecord)!= RXN_SUCCESS)
   *      {
   *        // Handle Errors.
   *      }
   *    }
   *  }
   * \endcode
   */
  U16 RXN_Set_Glonass_Ephemeris(
    const RXN_glonass_ephem_t* ephemeris
  );

  /**
   * \deprecated
   * This function is maintained for backwards compatibility and will be 
   * removed in a future release.
   *
   * \brief 
   * Decodes the seed data or clock update data for all PRNs received 
   * from the PGPS (tm) server.
   *
   * \param response [IN]
   * A pointer to the bytes received from PGPS (tm) server. In order to 
   * minimize the size of the network transmission, data is sent from the 
   * server in a packed format, and needs to be decoded on the
   * client. After the data is decoded to the internal data structures, 
   * the library will call the appropriate abstraction functions to save the 
   * data permanently.
   *
   * \param size [IN]
   * The length of the response byte buffer response points to.
   *
   * \param constel [IN]
   * The constellation to get seed information. Must be one of the types of
   * RXN_constel.
   *
   * \return RXN_SUCCESS
   * If the function succeeds in storing a BCE record or updating seed clock 
   * values. 
   * 
   * \return RXN_DEBUG_CODE_ERROR_NULL_ARGUMENT
   * If response is a NULL pointer.
   *
   * \return RXN_DEBUG_CODE_ERROR_INVALID_PRN
   * If prn is an invalid number.
   *
   * \return RXN_DEBUG_CODE_ERROR_UNSUPPORT_SEED_VERSION
   * If the PGPS seed data is not supported by the current version of the library.
   *
   * \return RXN_DEBUG_CODE_ERROR_INCOMPLETE_SEED_RESPONSE
   * If response containing PGPS seed data is incomplete.
   *
   * \return RXN_DEBUG_CODE_ERROR_MEM_WRITE_SEED_FAIL
   * If RXN_Mem_Write_Seed() abstraction function call fails.
   *
   * \return RXN_FAIL
   * Other errors.
   *
   * \details
   * This function will decode the binary data in packed format received from 
   * RxNetworks PGPS (tm) server and expand the contents into the PGPS (tm) 
   * data structure format and saved to memory. Specifically, the decode 
   * function will call the abstraction function RXN_Mem_Write_Prediction() 
   * to save the seed data permanently in memory. The integrator is required 
   * to implement the abstraction functions based on the characteristic of 
   * their platform.
   *
   * \note
   * The seed data and clock update data received from the PGPS (tm) server 
   * contains both healthy and unhealthy satellites.
   *
   * Satellite health is determined on the server, at the time when the seed 
   * is created, based on the following informations:
   * \li NANU historical information up to 7 days back.
   * \li NANU forecast informat up to 7 days ahead.
   * \li Real time integrity monitoring information.
   * 
   * When the client receives a satellite marked as 'unhealthy', any ephemeris
   * observations stored will be removed. As a result, the client will 
   * not be able to generate SAGPS seed data for the particular satellite.
   * This is the expect behavior of the library and is meant to prevent 
   * bad prediction data from being generated.
   *
   * \note
   * This method parses the payload of the HTTP response message and not the entire 
   * HTTP response message. Parsing the HTTP response message and handling of the
   * HTTP status codes must be handled by the integrator.
   *
   * <b>Applicable Standards</b>\n
   * Proprietary to GPStream (tm) PGPS (tm) client software.
   *
   * \since
   * 1.1.0
   *
   * \see
   * RXN_Mem_Write_Prediction()
   *
   */
  U16 RXN_Decode(
    const U08* response,
    const U32 size,
    RXN_constel_t constel
  );
  
  /**
   * \brief 
   * Decodes the seed data or clock update data for the specified PRN received 
   * from the PGPS (tm) server.
   *
   * \param prn [IN]
   * The PRN to decode seed data for.
   *
   * \param response [IN]
   * A pointer to the bytes received from PGPS (tm) server. In order to 
   * minimize the size of the network transmission, data is sent from the 
   * server in a packed format, and needs to be decoded on the
   * client. After the data is decoded to the internal data structures, 
   * the library will call the appropriate abstraction functions to save the 
   * data permanently.
   *
   * \param size [IN]
   * The length of the response byte buffer response points to.
   *
   * \param constel [IN]
   * The constellation to get seed information. Must be one of the types of
   * RXN_constel.
   *
   * \param state
   * [IN][OUT] A RXN_state_t structure to keep the state of the system. This 
   * API has to be called continuously in a loop. When the API is called for 
   * the first time, the status member of the state(state.status) must be 
   * initialized to RXN_STATUS_START. When the function returns, the 
   * state.status member must be checked for completion of execution. If 
   * state.status is RXN_STATUS_DONE, the execution of the API is complete. 
   * If the state.status is RXN_STATUS_PENDING, the API has to be called again 
   * with same set of parameter 'response' until state.status becomes 
   * RXN_STATUS_DONE.
   *   
   * \return RXN_SUCCESS
   * If the function succeeds in storing a BCE record or updating seed clock 
   * values. The return value is valid only when the state.status is 
   * RXN_STATUS_DONE.
   * 
   * \return RXN_DEBUG_CODE_ERROR_NULL_ARGUMENT
   * If response is a NULL pointer.
   *
   * \return RXN_DEBUG_CODE_ERROR_INVALID_PRN
   * If prn is an invalid number.
   *
   * \return RXN_DEBUG_CODE_ERROR_UNSUPPORT_SEED_VERSION
   * If the PGPS seed data is not supported by the current version of the library.
   *
   * \return RXN_DEBUG_CODE_ERROR_INCOMPLETE_SEED_RESPONSE
   * If response containing PGPS seed data is incomplete.
   *
   * \return RXN_DEBUG_CODE_ERROR_MEM_WRITE_SEED_FAIL
   * If RXN_Mem_Write_Seed() abstraction function call fails.
   *
   * \return RXN_FAIL
   * Other errors.
   *
   * \details
   * This function will decode the binary data in packed format received from 
   * RxNetworks PGPS (tm) server and expand the contents into the PGPS (tm) 
   * data structure format and saved to memory. Specifically, the decode 
   * function will call the abstraction function RXN_Mem_Write_Prediction() 
   * to save the seed data permanently in memory. The integrator is required 
   * to implement the abstraction functions based on the characteristic of 
   * their platform.
   *
   * \note
   * The seed data and clock update data received from the PGPS (tm) server 
   * contains the full set of healthy satellites only.
   *
   * \note
   * This method parses the payload of the HTTP response message and not the entire 
   * HTTP response message. Parsing the HTTP response message and handling of the
   * HTTP status codes must be handled by the integrator.
   *
   * <b>Applicable Standards</b>\n
   * Proprietary to GPStream™ PGPS™ client software.
   *
   * \since
   * 3.1.0
   *
   * <b>See Also</b>\n
   * RXN_Mem_Write_Prediction()
   *
   */
  U16 RXN_Decode_NB(
    const U08 prn,
    const U08* response,
    const U32 size,
    RXN_constel_t constel,
    RXN_state_t* state
  );
  
  /**
   * \brief 
   * Get seed data including start time, current time, end time and the amount
   * of BCE records used to create the seed (SAGPS (tm) only).
   *
   * \param prn [IN]
   * The PRN to get seed data for.
   *
   * \param information [OUT]
   * A pointer to a RXN_seed_information_t that will be updated with data for
   * the seed corresponding to the specified PRN.
   *
   * \param constel [IN]
   * The constellation to get seed information. Must be one of the types of
   * RXN_constel.
   *
   * \return RXN_SUCCESS 
   * If the function succeeds in getting seed information for the specified PRN.
   * 
   * \return RXN_DEBUG_CODE_ERROR_NULL_ARGUMENT
   * If information is a NULL pointer.
   *
   * \return RXN_DEBUG_CODE_ERROR_INVALID_PRN
   * If prn is an invalid number.
   *
   * \return RXN_DEBUG_CODE_ERROR_MEM_READ_SEED_FAIL
   * If RXN_Mem_Read_Seed() abstraction function call fails or if the specified seed cannot be found.
   *
   * \return RXN_FAIL
   * Other errors.
   *
   * \details
   * The information RXN_seed_information_t will contain a variety of parameters
   * that can be used to determine when an EE update should occur. See the
   * description for RXN_seed_information_t for detail on the data provided.
   * Note that use of this function can be considered optional of the decision
   * to update EE is based on system clock data and not seed data (e.g. it may
   * be simpler for integration code can elect to update EE data every x hours
   * regardless of how old a seed is - simpler).
   *
   * <b>Applicable Standards</b>\n
   * Proprietary to GPStream (tm) PGPS (tm) client software, SAGPS (tm) client software.
   *
   * \note
   * The expiry time for PGPS (tm) seeds will always be the seed start time plus
   * 7 days (168 Hrs).
   *
   * \note
   * The expiry time for SAGPS (tm) seeds will always be the seed start time plus
   * 3 days (72 Hrs).
   *
   * \since
   * 1.2.0
   *
   * \see
   * RXN_Generate_Assistance()
   *
   * <b>Example Usage</b>
   * \code
   * // Example. Get seed info for each PRN to determine whether a EE update is required.
   * RXN_seed_information_t seedInfo;
   *
   * // Loop through PRNs.
   * for(int x=0; x< RXN_CONSTANT_NUM_PRNS; x++)
   * {
   *    // Get seed data.
   *    if(RXN_Get_Seed_Information(x+1, &seedInfo) != RXN_SUCCESS)
   *    {
   *      // Handle Errors.
   *    }
   *    
   *    // Do something based on seed info.
   * }
   * \endcode
   */
  U16 RXN_Get_Seed_Information(
    const U08 prn,
    RXN_seed_information_t* information,
    RXN_constel_t constel
  );

  /**
   * \brief 
   * Set configuration structure
   *
   * \param config [IN] 
   * A RXN_config_t structure with time offsets & configuration parameters.
   *
   * \return RXN_SUCCESS
   * If the function completes successfully.
   *
   * \return RXN_DEBUG_CODE_ERROR_NULL_ARGUMENT
   * If config is a NULL pointer.
   *
   * \return RXN_DEBUG_CODE_ERROR_MEM_WRITE_CONFIG_FAIL
   * If RXN_Mem_Write_Config() abstraction function call fails.
   * 
   * \return RXN_FAIL
   * Other errors.
   *   
   * \details
   * Use this to set the configuration parameters in the RXN_config_t 
   * structure after reading the configuration parameters.
   *
   * \note
   * The library does not use tau_gps or tt_tau_gps but provides it. 
   *
   * \note
   * However the library *does* need leap_secs for Glonass and will use gps_time_next_leap
   * if not zero. The user is therefore advised not zero either of these out when 
   * calling RXN_Set_Config(). Note that PGPS seeds are synced to GPS time so calling
   * RXN_Get_Ephemeris() is fine if PGPS seeds are available. For SA Glonass seeds, the value 
   * of leap_secs must be correct.
   *
   * The best practice is to call RXN_Get_Configuration() and then alter the ephemeris 
   * duration parameters. It is not a good idea to declare a config instance, memset 
   * and then set.
   *
   * <b>Applicable Standards</b>\n
   * Proprietary to GPStream (tm) PGPS (tm) client software, SAGPS (tm) client software.
   *
   * \since
   * 4.0.0
   *
   * \see
   * RXN_Get_Configuration()
   *
   * <b>Example Usage</b>
   * \code
   * RXN_config_t config;
   *    //read leap_sec and tau_gps ... don't clobber when setting bellow
   * RXN_Get_Configuration(config);
   * 	//time offset [sec] between prediction buffer and last toe [default 0 sec].
   * config.toe_buffer_offset = 0;
   * 	// nominal chipset ephemeris expiration after toe.
   * config.chip_expiry_from_TOE = 7200; 
   * 	// duration of predicted Keplarian ephemeris.
   * config.ephemeris_duration = 300;
   * 	// 0 no logging, 1 error, 2 info, 3 debug, 4 full dump.
   * config.logging_level = 1;               
   * RXN_Set_Configuration(&config);
   * \endcode
   *
   */
  U16 RXN_Set_Configuration(const RXN_config_t* config);
 
  /**
   * \brief 
   * Get current configuration structure
   *
   * \param config [OUT] 
   * A RXN_config_t structure with the current configuration parameters.
   *   
   * \returns RXN_SUCCESS
   * If the function completes successfully.
   *
   * \return RXN_DEBUG_CODE_ERROR_NULL_ARGUMENT
   * If config is a NULL pointer.
   *
   * \return RXN_DEBUG_CODE_ERROR_MEM_READ_CONFIG_FAIL
   * If RXN_Mem_Read_Config() abstraction function call fails.
   * 
   * \return RXN_FAIL
   * Other errors.
   * 
   * \details
   * Use this to get the configuration parameters in the RXN_config_t structure
   *
   * \note
   * The configuration parameters will be set to default values 
   * if RXN_Mem_Read_Config() abstraction function call fails.
   *
   * <b>Applicable Standards</b>\n
   * Proprietary to GPStream (tm) PGPS (tm) client software, SAGPS (tm) client software.
   *
   * \since
   * 4.0.0
   *
   * \see
   * RXN_Get_Configuration()
   *
   * <b>Example Usage</b>
   * \code
   * RXN_config_t config;          
   * RXN_Get_Configuration(&config);
   * \endcode
   *
   */
  U16 RXN_Get_Configuration(RXN_config_t* config);
  
  /**
   * \brief
   * Run a self diagnostic test after adding at least one ephemeris into the appropriate store.
   * 
   * \param constel [IN] 
   * The constellation to be diagnosed.
   * \return RXN_SUCCESS Upon finding the first valid ephemeris and making a prediction with it
   * which agrees within tolerance when cross-checked against parent ephemeris
   * \return RXN_DEBUG_CODE_WARN_BROADCAST_EPH_TOO_OLD if nothing present to allow a test to be performed
   * or RXN_FAIL if comparison out of tolerance
   *
   * \details
   * This function will run through the ephemerides in the data base and run a short prediction 
   * against the parent ephemeris itself, all logging will be output through the debug abstraction 
   * call. Failure to have any ephemerides in the system before running the test will invalidate
   * the test. This allows abstraction layer bugs to be flushed out.
   * Currently only the default constellation [NAVSTAR] is supported. 
   *
   * \since
   * 4.0.0
   */
  U16 RXN_Self_Test(RXN_constel_t constel);

#ifdef __cplusplus
} // extern "C"
#endif

#endif // RXN_API_H
