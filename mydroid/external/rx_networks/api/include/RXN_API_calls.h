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
 * Abstraction functions to be implemented in a target by target basis.
 *
 *************************************************************************
 * $LastChangedDate: 2012-12-07 15:20:20 -0800 (Fri, 07 Dec 2012) $
 * $Revision: 104460 $
 *************************************************************************
 *
 */

/**
 * \file
 * \brief
 * Functions to be implemented by the client.
 * Contains prototypes which must be implemented by an integrator.
 *
 * Except for RXN_Mem_Write_Debug() (which is optional) each of the abstraction functions is based per prn.
 *
 * In the case of multi constellation support, prn can range from 1 to RXN_CONSTANT_NUM_GPS_PRNS + RXN_CONSTANT_NUM_GLONASS_PRNS.
 *
 * GPS related data is stored in slots 1 through RXN_CONSTANT_NUM_GPS_PRNS.
 *
 * The intent is for there be 2 "files" per prn. The "prediction" file encodes 2 seeds and the correspoding polynomials:
 *
 * |seed0|seed1|poly0|poly1|....|poly47|
 * 
 * The eph file encodes the ephemeris store (note index refers to the index of the next eph to be written):
 *
 * |index|eph0|eph1|...|eph_n|  n could be RXN_CONSTANT_MAX_GLONASS_EPHEMERIS_RECORDS or RXN_CONSTANT_MAX_EPHEMERIS_RECORDS
 *
 * Note that while the maximum size of the prediction file (for any prn) is fixed, the size of the eph file depends on the 
 * prn due to the different sizes of RXN_ephem_t and RXN_glonass_ephem_t as well as the number of records. 
 *
 * \note
 * Currently, the functions uses 'prn' as the parameter name. This might be 
 * changed in a future release when additional GNSS constellations are supported.
 * 
 * \attention 
 * <b> It is important to know these maximum file sizes. See below </b>
 *
 * It is absolutely vital that file structure be initialized to <b>0xff</b>. Any other pattern may falsely pass checksum
 * and corrupt the seeds and polys. 
 *
 * Note only seed and poly data are check summed; ephemeris data is <b>not</b> check summed.
 * This makes it even more important to initialize ephemeris data to <b>0xff</b>.  
 * 
 * \since
 * 1.1.0
 * 
 */

#ifndef RXN_API_CALLS_H
#define RXN_API_CALLS_H

#include "RXN_structs.h"
#include <stdlib.h>

#ifdef __cplusplus
extern "C" {
#endif

  /**
   * \brief
   * Called by GPStream library to read EE store
   * 
   * \param prn 
   * [IN] PRN for which the ephemeris index is read.
   *
   * Range: [1, RXN_CONSTANT_NUM_GPS_PRNS + RXN_CONSTANT_NUM_GLONASS_PRNS]
   *
   * \param offset
   * [in] byte offset from the beginning of the store
   *
   * Range: 0 to RXN_CONSTANT_SEED_RECORD_SIZE * 2 + RXN_CONSTANT_POLY_RECORD_SIZE * RXN_CONSTANT_MAX_PGPS_POLY_RECORDS  =  6576 bytes
   *
   * \param size
   * [IN] number of bytes to be read. Either RXN_CONSTANT_SEED_RECORD_SIZE or RXN_CONSTANT_POLY_RECORD_SIZE
   *
   * \param p_data
   * [OUT] Pointer to buffer which has the bytes starting from offset upto offset + size
   *
   * \return RXN_SUCCESS
   * If the value is read successfully.
   *
   * \return RXN_FAIL
   * If the value has not been read successfully.
   *
   * <b>Applicable Standards</b>\n
   * SAGPS™ client software.
   *
   * \since
   * 4.0.0
   *   
   * \see
   * RXN_Mem_Write_Prediction().
   */ 
  U16 RXN_Mem_Read_Prediction(
    unsigned prn,
    size_t offset,
    size_t size,
    void* p_data
  );

  /**
   * \brief
   * Called by GPStream library to write EE store
   * 
   * \param prn 
   * [IN] PRN for which the ephemeris index is written.
   *
   * Range: [1, RXN_CONSTANT_NUM_GPS_PRNS + RXN_CONSTANT_NUM_GLONASS_PRNS]
   *
   * \param offset
   * [in] byte offset from the beginning of the store
   *
   * Range: 0 to RXN_CONSTANT_SEED_RECORD_SIZE * 2 + RXN_CONSTANT_POLY_RECORD_SIZE * RXN_CONSTANT_MAX_PGPS_POLY_RECORDS  =  6576 bytes
   *
   * \param size
   * [IN] number of bytes to be read. Either RXN_CONSTANT_SEED_RECORD_SIZE or RXN_CONSTANT_POLY_RECORD_SIZE
   *
   * \param p_data
   * [IN] Pointer to buffer which has the bytes starting from offset upto offset + size
   *
   * \return RXN_SUCCESS
   * If the value is read successfully.
   *
   * \return RXN_FAIL
   * If the value has not been read successfully.
   *
   * <b>Applicable Standards</b>\n
   * SAGPS™ client software.
   *
   * \since
   * 4.0.0
   *   
   * \see
   * RXN_Mem_Read_Prediction().
   */ 
  U16 RXN_Mem_Write_Prediction(
    unsigned prn,
    size_t offset,
    size_t size,
    const void* p_data
  );

  /**
   * \brief
   * Called by GPStream library to read from Ephemeris store
   * 
   * \param prn 
   * [IN] PRN for which the ephemeris index is written.
   *
   * Range: [1, RXN_CONSTANT_NUM_GPS_PRNS + RXN_CONSTANT_NUM_GLONASS_PRNS]
   *
   * \param offset
   * [in] byte offset from the beginning of the store
   *
   * Range: 0 to RXN_CONSTANT_MAX_EPHEMERIS_RECORDS * RXN_CONSTANT_EPHEMERIS_RECORD_SIZE + 1 for prn in [1,RXN_CONSTANT_NUM_GPS_PRNS]
   * 
   * or
   *
   * 0 to RXN_CONSTANT_MAX_GLONASS_EPHEMERIS_RECORDS * RXN_CONSTANT_GLONASS_EPHEMERIS_RECORD_SIZE + 1 for prn in [RXN_CONSTANT_NUM_GPS_PRNS + 1, RXN_CONSTANT_NUM_GLONASS_PRNS]
   *
   * \param size Either RXN_CONSTANT_EPHEMERIS_RECORD_SIZE or RXN_CONSTANT_GLONASS_EPHEMERIS_RECORD_SIZE or 1
   * [IN] number of bytes to be read
   *
   * \param p_data
   * [OUT] Pointer to buffer which has the bytes starting from offset upto offset + size
   *
   * \return RXN_SUCCESS
   * If the value is read successfully.
   *
   * \return RXN_FAIL
   * If the value has not been read successfully.
   *
   * <b>Applicable Standards</b>\n
   * SAGPS™ client software.
   *
   * \since
   * 4.0.0
   *   
   * \see
   * RXN_Mem_Write_Eph().
   */ 
  U16 RXN_Mem_Read_Eph(
    unsigned prn,
    size_t offset,
    size_t size,
    void* p_data
  );

  /**
   * \brief
   * Called by GPStream library to write from Ephemeris store
   * 
   * \param prn 
   * [IN] PRN for which the ephemeris index is written.
   *
   * Range: [1, RXN_CONSTANT_NUM_GPS_PRNS + RXN_CONSTANT_NUM_GLONASS_PRNS]
   *
   * \param offset
   * [in] byte offset from the beginning of the store
   *
   * Range: 0 to RXN_CONSTANT_MAX_EPHEMERIS_RECORDS * RXN_CONSTANT_EPHEMERIS_RECORD_SIZE + 1 for prn in [1,RXN_CONSTANT_NUM_GPS_PRNS]
   * 
   * or
   *
   * 0 to RXN_CONSTANT_MAX_GLONASS_EPHEMERIS_RECORDS * RXN_CONSTANT_GLONASS_EPHEMERIS_RECORD_SIZE + 1 for prn in [RXN_CONSTANT_NUM_GPS_PRNS + 1, RXN_CONSTANT_NUM_GLONASS_PRNS]
   *
   * \param size Either RXN_CONSTANT_EPHEMERIS_RECORD_SIZE or RXN_CONSTANT_GLONASS_EPHEMERIS_RECORD_SIZE or 1
   * [IN] number of bytes to be read
   *
   * \param p_data
   * [IN] Pointer to buffer which has the bytes starting from offset upto offset + size
   *
   * \return RXN_SUCCESS
   * If the value is read successfully.
   *
   * \return RXN_FAIL
   * If the value has not been read successfully.
   *
   * <b>Applicable Standards</b>\n
   * SAGPS™ client software.
   *
   * \since
   * 4.0.0
   *   
   * \see
   * RXN_Mem_Read_Eph().
   */ 
  U16 RXN_Mem_Write_Eph(
    unsigned prn,
    size_t offset,
    size_t size,
    const void* p_data
  );

  /**
   * \brief Force out ascii formatted buffer to be appended to a log.
   *
   * \see logging_level in RXN_config_t 
   *
   * \param size (variable. max 512)
   * [IN] Number of bytes to be written. 
   *
   * \param p_data
   * [OUT] Pointer to pre-assigned buffer data
   *
   * \since
   * 4.0.0
   *   
   */
  void RXN_Mem_Write_Debug(
    const U16 size,
    const U08* p_data
  );

  /**
   * \brief
   * Called by GPStream library to write config data such as ephemeris roll back, leap_secs etc as 
   * described by RXN_config_t in RXN_structs.h 
   *
   * \param offset
   * [in] byte offset from the beginning of the store
   *
   * \param size 
   * [IN] number of bytes to be read
   *
   * \param p_data
   * [IN] Pointer to buffer which has the bytes starting from offset upto offset + size
   *
   * \return RXN_SUCCESS
   * If the value is read successfully.
   *
   * \return RXN_FAIL
   * If the value has not been read successfully.
   *
   * <b>Applicable Standards</b>\n
   * SAGPS™ client software.
   *
   * \since
   * 6.0.0
   *   
   * \see
   * RXN_Mem_Read_Config().
   */ 
  U16 RXN_Mem_Write_Config(
    size_t offset,
    size_t size,
    const void* p_data
  );

  /**
   * \brief
   * Called by GPStream library to read config data such as ephemeris roll back, leap_secs etc as 
   * described by RXN_config_t in RXN_structs.h 
   *
   * \param offset
   * [in] byte offset from the beginning of the store
   *
   * \param size 
   * [IN] number of bytes to be read
   *
   * \param p_data
   * [IN] Pointer to buffer which has the bytes starting from offset upto offset + size
   *
   * \return RXN_SUCCESS
   * If the value is read successfully.
   *
   * \return RXN_FAIL
   * If the value has not been read successfully.
   *
   * <b>Applicable Standards</b>\n
   * SAGPS™ client software.
   *
   * \since
   * 6.0.0
   *   
   * \see
   * RXN_Mem_Write_Config().
   */ 
  U16 RXN_Mem_Read_Config(
    size_t offset,
    size_t size,
    void* p_data
  );

  /**
   * \brief
   * Called by GPStream library to read from frequency channel store
   * 
   * \param prn 
   * [IN] PRN for which the ephemeris index is written.
   *
   * Range: [1, RXN_CONSTANT_NUM_GPS_PRNS + RXN_CONSTANT_NUM_GLONASS_PRNS]
   *
   * \param offset
   * [in] byte offset from the beginning of the store
   *
   * \param size
   * [IN] number of bytes to be read
   *
   * \param p_data
   * [OUT] Pointer to buffer which has the bytes starting from offset upto offset + size
   *
   * \return RXN_SUCCESS
   * If the value is read successfully.
   *
   * \return RXN_FAIL
   * If the value has not been read successfully.
   *
   * <b>Applicable Standards</b>\n
   * SAGPS™ client software.
   *
   * \since
   * 7.1.0
   *   
   * \see
   * RXN_Mem_Write_Freq_Chan().
   */ 
  U16 RXN_Mem_Read_Freq_Chan(
    unsigned prn,
    size_t offset,
    size_t size,
    void* p_data
  );

  /**
   * \brief
   * Called by GPStream library to write from frequency channel store
   * 
   * \param prn 
   * [IN] PRN for which the ephemeris index is written.
   *
   * Range: [1, RXN_CONSTANT_NUM_GPS_PRNS + RXN_CONSTANT_NUM_GLONASS_PRNS]
   *
   * \param offset
   * [in] byte offset from the beginning of the store
   *
   * \param size
   * [IN] number of bytes to be read
   *
   * \param p_data
   * [IN] Pointer to buffer which has the bytes starting from offset upto offset + size
   *
   * \return RXN_SUCCESS
   * If the value is read successfully.
   *
   * \return RXN_FAIL
   * If the value has not been read successfully.
   *
   * <b>Applicable Standards</b>\n
   * SAGPS™ client software.
   *
   * \since
   * 4.0.0
   *   
   * \see
   * RXN_Mem_Read_Freq_Chan().
   */ 
  U16 RXN_Mem_Write_Freq_Chan(
    unsigned prn,
    size_t offset,
    size_t size,
    const void* p_data
  );

  /**
   * \brief
   * Called by GPStream library to read from block type store
   * 
   * \param prn 
   * [IN] PRN for which the ephemeris index is written.
   *
   * Range: [1, RXN_CONSTANT_NUM_GPS_PRNS + RXN_CONSTANT_NUM_GLONASS_PRNS]
   *
   * \param offset
   * [in] byte offset from the beginning of the store
   *
   * \param size
   * [IN] number of bytes to be read
   *
   * \param p_data
   * [OUT] Pointer to buffer which has the bytes starting from offset upto offset + size
   *
   * \return RXN_SUCCESS
   * If the value is read successfully.
   *
   * \return RXN_FAIL
   * If the value has not been read successfully.
   *
   * <b>Applicable Standards</b>\n
   * SAGPS™ client software.
   *
   * \since
   * 7.1.0
   *   
   * \see
   * RXN_Mem_Write_Block_Type().
   */ 
  U16 RXN_Mem_Read_Block_Type(
    unsigned prn,
    size_t offset,
    size_t size,
    void* p_data
  );

  /**
   * \brief
   * Called by GPStream library to write to block type store
   * 
   * \param prn 
   * [IN] PRN for which the ephemeris index is written.
   *
   * Range: [1, RXN_CONSTANT_NUM_GPS_PRNS + RXN_CONSTANT_NUM_GLONASS_PRNS]
   *
   * \param offset
   * [in] byte offset from the beginning of the store
   *
   * \param size
   * [IN] number of bytes to be read
   *
   * \param p_data
   * [IN] Pointer to buffer which has the bytes starting from offset upto offset + size
   *
   * \return RXN_SUCCESS
   * If the value is read successfully.
   *
   * \return RXN_FAIL
   * If the value has not been read successfully.
   *
   * <b>Applicable Standards</b>\n
   * SAGPS™ client software.
   *
   * \since
   * 4.0.0
   *   
   * \see
   * RXN_Mem_Read_Block_Type().
   */ 
  U16 RXN_Mem_Write_Block_Type(
    unsigned prn,
    size_t offset,
    size_t size,
    const void* p_data
  );
	
  /**
   * \brief
   * Called by GPStream library to write satellite outage information.
   *
	 * \note
	 * The outage information could be embedded in a server response message.
	 * eg. As a part of a PGPS seed request.
	 *
   * \param offset
   * [in] byte offset from the beginning of the store
   *
   * \param size 
   * [IN] number of bytes to be read
   *
   * \param p_data
   * [IN] Pointer to buffer which has the bytes starting from offset upto offset + size
   *
   * \return RXN_SUCCESS
   * If the value is read successfully.
   *
   * \return RXN_FAIL
   * If the value has not been read successfully.
   *
   * \since
   * 7.0.0
   *   
   * \see
   * RXN_Mem_Read_Outage_Information().
   */ 	
  U16 RXN_Mem_Write_Outage_Information(
    size_t offset,
    size_t size,
    void* p_data
  );

  /**
   * \brief
   * Called by GPStream library to read satellite outage information.
   *
   * \param offset
   * [in] byte offset from the beginning of the store
   *
   * \param size 
   * [IN] number of bytes to be read
   *
   * \param p_data
   * [IN] Pointer to buffer which has the bytes starting from offset upto offset + size
   *
   * \return RXN_SUCCESS
   * If the value is read successfully.
   *
   * \return RXN_FAIL
   * If the value has not been read successfully.
   *
   * \since
   * 7.0.0
   *   
   * \see
   * RXN_Mem_Write_Config().
   */ 	
  U16 RXN_Mem_Read_Outage_Information(
    size_t offset,
    size_t size,
    void* p_data
  );

  /**
   * \brief
   * Called by GPStream library to request the allocation of a memory block.
   * 
   * \param size 
   * [IN] The number of bytes to allocate.
   *
   * \returns 
   * Pointer to an available chunk of memory.
   *
   * \note
   * This function defines the memory management interface only.
   * It is dependent on a 3rd party memory manager (to be implemented by 
   * the system integrator).
   *
   * \since 
   * 6.0.3
   *
   * \see
   * RXN_Mem_Free().
   */
  void* RXN_Mem_Alloc(
    size_t size
  );

  /**
   * \brief
   * Called by GPStream library to request the deallocation of a memory block.
   * 
   * \param p 
   * [IN] The pointer to the allocated memory block to be freed.
   *
   * \note
   * This function defines the memory management interface only.
   * It is dependent on a 3rd party memory manager (to be implemented by 
   * the system integrator).
   *
   * \since 
   * 6.0.3
   *
   * \see
   * RXN_Mem_Alloc().
   */
  void RXN_Mem_Free(
    void* p
  );

#ifdef __cplusplus
} // extern "C"
#endif

#endif // RXN_API_CALLS_H
