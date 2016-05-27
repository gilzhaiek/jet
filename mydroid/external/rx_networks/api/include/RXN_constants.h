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
 * $LastChangedDate: 2012-12-12 16:24:05 -0800 (Wed, 12 Dec 2012) $
 * $Revision: 104507 $
 *************************************************************************
 *
 */

/**
 * \file
 * \brief
 * Defines the constants used in the library.
 *
 * \since
 * 1.1.0
 */

#ifndef RXN_CONSTANTS_H
#define RXN_CONSTANTS_H

#ifdef __cplusplus
extern "C" {
#endif

#include "RXN_structs.h"

/**
 * \brief
 * Offset in the beginning of the abstraction file, prior to the data.
 *
 * \details
 * The abstraction file is typically structured as:
 *     size of content + checksum + [data]
 * The offset is the sum of content + checksum.
 * 
 * \since
 * 6.1.0
 */
#define OFFSET 4
	
/**
 * \brief
 * Defines the maximum number of characters for a version string.
 *
 * \since
 * 1.1.0
 */
#define RXN_CONSTANT_VERSION_STRING_LENGTH 50

/**
 * \brief
 * Defines the maximum number of GPS PRNs.
 *
 * \since
 * 1.1.0
 */
#define RXN_CONSTANT_NUM_GPS_PRNS 32

/**
 * \brief
 * Defines the maximum number of Glonass PRNs.
 *
 * \since
 * 4.0.0
 */
#define RXN_CONSTANT_NUM_GLONASS_PRNS 24

/**
 * \brief
 * Defines the max 'prn' index for any constellation
 * Each constellation is stored with its SVs numbered 1-N (eg 32 GPS, 24 Glonass etc)
 * This constant is used for range checking in http decoding and abstraction layer logic
 *
 * \since
 * 1.1.0
 */
#define RXN_CONSTANT_NUM_PRNS 32

/**
 * \brief
 * Defines the maximum number of broadcast ephemeris (BCE) records
 * stored within the BCE Store (per PRN). GPS ephemeris are 
 * updated every 2 hrs.
 *
 * \since
 * 1.1.0
 */
#define RXN_CONSTANT_MAX_EPHEMERIS_RECORDS 15

/**
 * \brief
 * Defines the maximum number of GLONASS ephemeris records
 * stored within the client(per PRN). GLONASS ephemeris are 
 * updated every 30 mins rather than 2 hrs like GPS.
 *
 * \since
 * 4.0.0
 */
#define RXN_CONSTANT_MAX_GLONASS_EPHEMERIS_RECORDS 50

/**
 * \brief
 * Defines the maximum number of seed records
 * stored within the Seed Store (per PRN).
 *
 * \since
 * 1.1.0
 */
#define RXN_CONSTANT_MAX_SEED_RECORDS 2

/** 
 * \brief
 * Defines the maximum number of polynomial records
 * stored within a PGPS Poly Store (per PRN).
 *
 * Each polynomial record represents 4 hrs of prediction. 
 *
 * \since
 * 1.1.0
 */
#define RXN_CONSTANT_MAX_PGPS_POLY_RECORDS 90

/** 
 * \brief
 * Defines the maximum number of polynomial records
 * stored within a SAGPS Poly Store (per PRN).
 *
 * Each polynomial record represents 4 hrs of prediction.
 *
 * \since
 * 1.1.0
 */
#define RXN_CONSTANT_MAX_SAGPS_POLY_RECORDS 31
 
/**
 * \brief
 * Defines the size (in bytes) of a broadcast ephemeris (BCE) record.
 *
 * \note
 * This value may change if compiler is not set to default byte alignment.
 * 
 * \see
 * RXN_ephem_t
 *
 * \since
 * 1.1.0
 */
#define RXN_CONSTANT_EPHEMERIS_RECORD_SIZE (80 + OFFSET)

/**
 * \brief
 * Defines the size (in bytes) of a Glonass ephemeris record. 
 *
 * \note
 * This value may change if compiler is not set to default byte alignment.
 *
 * \see 
 * RXN_glonass_ephem_t
 *
 * \since
 * 4.0.0
 */
#define RXN_CONSTANT_GLONASS_EPHEMERIS_RECORD_SIZE (56 + OFFSET)

/**
 * \brief
 * Defines the size (in bytes) of the index into the next ephemeris record. 
 *
 * \note
 * This value may change if compiler is not set to default byte alignment.
 *
 * \since
 * 4.0.0
 */
#define RXN_CONSTANT_EPHEMERIS_RECORD_INDEX_SIZE (16 + OFFSET)

/**
 * \brief
 * Defines the size (in bytes) of a seed record. 
 * Note that this value may change if compiler is not set to default byte alignment.
 *
 * \note
 * This value may change if compiler is not set to default byte alignment.
 *
 * \since
 * 1.1.0
 */
#define RXN_CONSTANT_SEED_RECORD_SIZE (680 + OFFSET)

/**
 * \brief
 * Defines the maximum size (in bytes) of a polynomial record.
 *
 * \note
 * This value may change if compiler is not set to default byte alignment.
 *
 * \since
 * 1.1.0
 */
#define RXN_CONSTANT_POLY_RECORD_SIZE (144 + OFFSET)

/**
 * \brief
 * Defines the maximum size (in bytes) of a Satellite's prediction buffer (seed  & polys).
 *
 * \note
 * This value may change if compiler is not set to default byte alignment.
 *
 * \since
 * 1.1.0
 */
#define RXN_PRED_FILE_SIZE (RXN_CONSTANT_SEED_RECORD_SIZE * RXN_CONSTANT_MAX_SEED_RECORDS + RXN_CONSTANT_POLY_RECORD_SIZE * RXN_CONSTANT_MAX_PGPS_POLY_RECORDS)

/**
 * \brief
 * Defines the maximum size (in bytes) of the Glonass ephemeris store for a given satellite.
 *
 * \note
 * This value may change if compiler is not set to default byte alignment.
 *
 * \since
 * 1.1.0
 */
#define RXN_GLONASS_FILE_SIZE (RXN_CONSTANT_EPHEMERIS_RECORD_INDEX_SIZE + RXN_CONSTANT_GLONASS_EPHEMERIS_RECORD_SIZE * RXN_CONSTANT_MAX_GLONASS_EPHEMERIS_RECORDS)

/**
 * \brief
 * Defines the maximum size (in bytes) of the GPS ephemeris store for a given satellite.
 *
 * \note
 * This value may change if compiler is not set to default byte alignment.
 *
 * \since
 * 1.1.0
 */
#define RXN_NAVSTAR_FILE_SIZE (RXN_CONSTANT_EPHEMERIS_RECORD_INDEX_SIZE + RXN_CONSTANT_EPHEMERIS_RECORD_SIZE * RXN_CONSTANT_MAX_EPHEMERIS_RECORDS)

/**
 * \brief
 * Defines the maximum size (in bytes) config data
 *
 * \note
 * This value may change if compiler is not set to default byte alignment.
 *
 * \since
 * 6.0.0
 */
#define RXN_CONSTANT_CONFIG_RECORD_SIZE (56 + OFFSET)

/**
 * \brief
 * Defines the maximum size (in bytes) frequency channel data
 *
 * \note
 * This value may change if compiler is not set to default byte alignment.
 *
 * \since
 * 7.1.0
 */
#define RXN_CONSTANT_FREQCHAN_RECORD_SIZE (16 + OFFSET)

/**
 * \brief
 * Defines the maximum size (in bytes) block type data
 *
 * \note
 * This value may change if compiler is not set to default byte alignment.
 *
 * \since
 * 7.1.0
 */
#define RXN_CONSTANT_BLOCKTYPE_RECORD_SIZE (16 + OFFSET)

/**
 * \brief
 * Defines the maximum size (in bytes) of a outage information record.
 *
 * \note
 * This value may change if the compiler is not set to default byte alignment.
 *
 * \since
 * 7.0.0
 */
#define RXN_CONSTANT_OUTAGE_INFORMATION_RECORD_SIZE (32 + OFFSET)

/**
 * \brief
 * Defines the maximum size (in bytes) of a satellite's outage information.
 *
 * \note
 * This value may change if compiler is not set to default byte alignment.
 *
 * \since
 * 7.0.0
 */
#define RXN_OUTAGE_INFORMATION_FILE_SIZE (RXN_CONSTANT_OUTAGE_INFORMATION_RECORD_SIZE)

/**
 * \brief
 * This is the minimum size of the cache to used with RXN_Initialize_Cache. This size
 * is typically used for PGPS systems.
 *
 * \since
 * 1.1.0
 */
#define RXN_CONSTANT_MIN_CACHE_SIZE 6912

/** 
 * \brief
 * This is the maximum size of the cache to be used with RXN_Initialize_Cache. This size
 * is typically used for SAGPS systems.
 *
 * \since
 * 1.2.0
 */
#define RXN_CONSTANT_MAX_CACHE_SIZE 43008


/**
 * \brief
 * The maximum length for the expiry date string.
 *
 * \since
 * 3.0.0
 */
#define RXN_CONSTANT_MAX_LICENSE_EXPIRY_DATE_LENGTH 20

/**
 * \brief
 * Defines the PGPS/SAGPS return code for functions that complete successfully.
 *
 * \since
 * 1.1.0
 */
#define RXN_SUCCESS 0

/**
 * \brief
 * Defines the PGPS/SAGPS return code for functions that complete unsuccessfully.
 *
 * \since
 * 1.1.0
 */
#define RXN_FAIL 255
  

#ifdef __cplusplus
} // extern "C"
#endif

#endif // RXN_CONSTANTS_H
