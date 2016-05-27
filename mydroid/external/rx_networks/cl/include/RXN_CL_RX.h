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
 * $LastChangedDate: 2009-06-23 11:25:43 -0700 (Tue, 23 Jun 2009) $
 * $Revision: 11559 $
 *************************************************************************
 *
 */

/**
 * \file
 * \brief
 * Chipset Library (CL) programming interface for Rx Networks technologies.
 * 
 * \details
 * The CL is used to provide a common interface for a variety of chipsets.
 * Any chipset supporting this common interface can easily be plugged into
 * Rx Networks applications or any other applications supporting the interface.
 * Note that this header describes functions specific to use of the CL 
 * to support RINEX as a chipset. Function interfaces should match those 
 * specified within RXN_CL.h. The only difference between functions should
 * be that functions declared within this header include "_RINEX_". * 
 */

#ifndef RXN_CL_RX_H
#define RXN_CL_RX_H

#ifdef __cplusplus
extern "C" {
#endif

#include "RXN_data_types.h"     /* Defines RXN Data Types. */
#include "RXN_constants.h"      /* Defines RXN Constants. */
#include "RXN_structs.h"        /* Defines RXN Data Structs. */
#include "RXN_debug.h"          /* Defines RXN Debug APIs and structs. */ 

#include "RXN_MSL.h"            /* Defines constants, structs and data types used by MSL, CL and RL.*/

#include "RXN_CL_Constants.h"   /* Defines constants used specifically by the CL. */

/**
 * \brief 
 * Retrieve the Chipset Lib (CL) API version.
 *
 * \param version [OUT] A memory location to be filled with the current API version.
 * This is a null terminated string and is at most RXN_CONSTANT_VERSION_STRING_LENGTH
 * (50) characters long. 
 *
 * \return RXN_SUCCESS If the version is returned successfully (always).
 *
 * \details
 * <b>Description</b>\n
 * This function returns the version of the CL. The version number will be
 * incremented as new features, enhancements, and bug fixes are added to the CL.
 * The version number is an important identification when reporting and troubleshooting issues.
 * Rx Networks version numbers are broken down within Integration User's Manuals.
 * Note that the version number returned from this function will match that of
 * RXN_CL_Get_API_Version(). 
 *
 * <b>Example Usage</b>
 * \code
 * // Declare a string to hold version info.
 * char version[RXN_CONSTANT_VERSION_STRING_LENGTH];
 * 
 * // Retrieve and output version information.
 * RXN_CL_RINEX_Get_API_Version(version);
 * printf("RXN_CL_RINEX_Get_API_Version(): %s\n", version);
 * \endcode
 *
 * <b>Output</b>\n
 * RXN_CL_RINEX_Get_API_Version(): 1.2.0
 */
U16 RXN_CL_RINEX_Get_API_Version(char version[RXN_CONSTANT_VERSION_STRING_LENGTH]);

/** 
 * \brief 
 * Initialize the CL library for use with RINEX chipset support.
 *
 * \param config 
 * [In] Specifies the file to open during chipset initialization as well as any
 * other config parameters. The format of the config string is <filepath>|<mode> where <filepath>
 * is a relative path to the file and <mode> specifies the file I/O mode ('w' - write new file, 
 * 'r' - read from an existing file, 'w+' - write and then read from a file).
 * \param phandle 
 * [Out] The pointer to a handle that identifies a CL instance for subsequent CL use.
 *
 * \return RXN_SUCCESS if the CL is initialized successfully., 
 * \return RXN_CL_TOO_MANY_INSTANCES_ERR if too many instances of the CL are currently initialized.,
 * \return RXN_CL_CONFIG_FORMAT_ERR if the config string is malformed (e.g. does not include '|' if required),
 * \return RXN_CL_OPEN_OR_CLOSE_ERR if the interface could not be opened (i.e. file open error).,
 *
 * \details
 * <b>Description</b>\n
 * Initialize the CL library before using it. This will setup a CL instance (with associate
 * handle). Instances are required to support clients that may have to utilize multiple copies
 * of the CL at the same time. (e.g. a CL user may wish to utilize a RINEX chipset and physical
 * chipset at the same time.) The handle will be used within subsequent calls to the initialized
 * instance of the CL.\n
 * To export to a RINEX file without subsequent import, utilize a config string similiar
 * to "./filename.rin|w". The 'w' will replace any existing file, but will only support export.\n
 * To import from a RINEX file without previous export, utilize a config string similiar
 * to "./filename.rin|r". The 'r' will require that the file exist at the location specified.
 * To import and export to a RINEX file using the same instance, utilize a config string similiar
 * to "./filename.rin|w+". The 'w+' will replace any existing file, therefore an export will be
 * required to populate the file before subsequent import. Note that calling RXN_CL_RINEX_WriteEphemeris()
 * multiple times with the same CL instance will lead to erroneous results. If multiple exports are 
 * required, multiple CL instances will have to be instantiated.\n
 * Logging is not supported by RINEX CL instances. 
 *
 * <b>See Also</b>\n
 * RXN_CL_RINEX_Uninitialize()
 *
 * <b>Example Usage</b>
 * \code
 * U16 Result = RXN_FAIL;
 * char Config[RXN_CL_CONFIG_MAX_STR_LEN];
 * U16 CLHandle = 0;
 * // Setup an array for ephemeris data store.
 * RXN_FullEphem_u PRNEphArr[RXN_CONSTANT_NUM_PRNS];
 * 
 * // Setup a config string to support RINEX export and re-import.
 * sprintf(Config, "./File.rin|w+");
 *
 * // Initialize the CL to support a u-blox chipset with version 1.00 firmware.
 * Result = RXN_CL_RINEX_Initialize(Config, &CLHandle);
 *
 * // Handle errors.
 * if(Result != RXN_SUCCESS)
 * {
 *    // ToDo: Handle a CL initialization error (file I/O error).
 * }
 *
 * // ToDo: Populate PRNEphArr[] from a chipset or assistance data source.
 *
 * // Export RINEX data.
 * RXN_CL_RINEX_WriteEphemeris(CLHandle, PRNEphArr, RXN_GPS_CONSTEL);
 *
 * // Import RINEX data.
 * RXN_CL_RINEX_ReadEphemeris(CLHandle, PRNEphArr, RXN_GPS_CONSTEL);
 *
 *
 * // Shutdown the CL instance.
 * RXN_CL_RINEX_Uninitialize(CLHandle);
 *
 * \endcode
 */ 
U16 RXN_CL_RINEX_Initialize(const char config[RXN_CL_CONFIG_MAX_STR_LEN], U16* phandle);

/**
 * \brief 
 * UnInitialize a CL library instance.
 * 
 * \param handle 
 * [In] A handle that identifies a CL instance.
 *
 * \return RXN_SUCCESS if the CL is uninitialized successfully., 
 * \return RXN_CL_INSTANCE_NOT_FOUND_ERR if an instance corresponding the provided handle cannot be found.,
 * \return RXN_CL_OPEN_OR_CLOSE_ERR if a file could not be closed properly.
 *
 * \details
 * <b>Description</b>\n
 * Shut down a previously initialized CL instance to free up instances and resources for subsequent use.
 *
 * <b>See Also</b>\n
 * RXN_CL_RINEX_Initialize()
 *
 * <b>Example Usage</b>
 * \code
 * See the example code provided for RXN_CL_RINEX_Initialize().
 * \endcode
 */ 
U16 RXN_CL_RINEX_Uninitialize(U16 handle);

/** 
 * \brief 
 * Read ephemeris data from a RINEX file.
 *
 * \param handle 
 * [IN] A handle that identifies a CL instance.
 * \param pNavDataList 
 * [OUT] A pointer to a RXN_MSL_NavDataList_t struct that will be populated with ephemeris
 * data.
 * \param constel
 * [IN] The constellation 
 *
 * \return RXN_SUCCESS if ephemeris is read successfully.,
 * \return RXN_CL_INSTANCE_NOT_FOUND_ERR - if the handle provided does not support an open CL instance.,
 * \return RXN_CL_CHIPSET_AT_EOF - if trying to read ephemeris from a RINEX file and an EOF is
 * encountered early.,
 * \return RXN_CL_CHIPSET_READ_ERR - if a RINEX file read error occurred.,
 * \return RXN_CL_ALLOCATION_ERROR - if the array size specified is invalid (< 1 or > 32).
 *
 * \details
 * <b>Description</b>\n
 * Read an array of ephemeris data structs through a previously initialized CL instance.
 * 
 * <b>Notes:<\b>
 * <li>For any given PRN within PRNArr, the prn value will be set to "0" if ephemeris
 * was not available for that PRN.<\li>
 * <li>To convert the PRNArr elements returned from RXN_FullEph_t to RXN_ephem_t
 * as supported within Rx Networks GPStream products, utilize RXN_CL_FullToRXN().<\li>
 *
 * <b>See Also</b>\n
 * RXN_CL_RINEX_WriteEphemeris(), RXN_CL_FullToRXN()
 *
 * <b>Example Usage</b>
 * \code
 * See the example code provided for RXN_CL_RINEX_Initialize().
 * \endcode
*/ 
U16 RXN_CL_RINEX_ReadEphemeris(U16 handle, RXN_MSL_NavDataList_t* pNavDataList, RXN_constel_t constel);

/** 
 * \brief 
 * Read ephemeris data from a RINEX file.
 *
 * [IN] A handle that identifies a CL instance.
 * \param pNavDataList 
 * [IN] A pointer to a RXN_MSL_NavDataList_t struct that is populated with ephemeris
 * data.
 * \param constel
 * [IN] The constellation
 *
 * \return RXN_SUCCESS if ephemeris is read successfully.,
 * \return RXN_CL_INSTANCE_NOT_FOUND_ERR - if the handle provided does not support an open CL instance.,
 * \return RXN_CL_CHIPSET_AT_EOF - if trying to read ephemeris from a RINEX file and an EOF is
 * encountered early.,
 * \return RXN_CL_CHIPSET_READ_ERR - if a RINEX file read error occurred.,
 * \return RXN_CL_ALLOCATION_ERROR - if the array size specified is invalid (< 1 or > 32).
 *
 * \details
 * <b>Description</b>\n
 * Read an array of ephemeris data structs through a previously initialized CL instance.
 * 
 * <b>Notes:<\b>
 * <li>For any given PRN within PRNArr, the prn value will be set to "0" if ephemeris
 * was not available for that PRN.<\li>
 * <li>To convert the PRNArr elements returned from RXN_FullEph_GLO_t to RXN_ephem_glonass_t
 * as supported within Rx Networks GPStream products, utilize RXN_CL_FullToRXN().<\li>
 *
 * <b>See Also</b>\n
 * RXN_CL_RINEX_WriteEphemerisGLO(), RXN_CL_FullToRXN()
 *
 * <b>Example Usage</b>
 * \code
 * See the example code provided for RXN_CL_RINEX_Initialize().
 * \endcode
*/ 
U16 RXN_CL_RINEX_WriteEphemeris(U16 handle, RXN_MSL_NavDataList_t* pNavDataList, RXN_constel_t constel);

/** 
 * \brief 
 * Check RINEX file age and replace as required.
 *
 * \param handle 
 * [In] A handle that identifies a CL instance.
 *
 * \return RXN_SUCCESS if ephemeris is written successfully.,
 * \return RXN_CL_INSTANCE_NOT_FOUND_ERR - if the handle provided does not support an open CL instance.,
 *
 * \details
 * <b>Description</b>\n
 * CL consumers can leverage the RINEX CL to track the last RINEX file output and notify the 
 * consumer (via the RXN_CL_GetAssistance fcn) that replacement RINEX data is required (i.e. existing
 * data does not exist, or is > 5 min old. CL consumers can then get replacement ephemeris data and
 * write this to RINEX using RXN_CL_RINEX_WriteEphemeris().
 * 
 * <b>Notes:<\b>
 * <li>To convert ephemeris that is acquired from Rx Networks GPStream products from
 * RXN_ephem_t to RXN_FullEph_t, utilize the RXN_CL_RXNToFull() function.<\li>
 *
 * <b>See Also</b>\n
 * RXN_CL_RINEX_WriteEphemeris()
 *
*/ 
U16 RXN_CL_RINEX_Work(U16 handle);

/** 
 * \brief 
 * Convert ephemeris data (RXN ephemeris to full ephemeris).
 *
 * \param pRXN 
 * [IN] Pointer to a source RXN_ephem_t struct.
 * \param pFull 
 * [OUT] Pointer to a destination RXN_FullEph_t struct.
 * \param CAOrPOnL2 
 * [IN] CA or P On L2 flag value to set within the struct pointed to by pFull.
 * \param iodc 
 * [IN] IODC value to set within the struct pointed to by pFull.
 * \param L2PData 
 * [IN] L2 PData value to set within the struct pointed to by pFull.
 * \param TGD 
 * [IN] TGD value to set within the struct pointed to by pFull.
 * \param AODO 
 * [IN] AODO value to set within the struct pointed to by pFull.
 *
 * \details
 * <b>Description</b>\n
 * Convert ephemeris data from the standard expected by Rx Networks libraries
 * (RXN_ephem_t) to a more comprehensive structure that also supports elements
 * that are found within RINEX data files and used by other Rx Networks libraries (RXN_FullEph_t).
 *
 * <b>See Also</b>\n
 * RXN_CL_FullToRXN()
 *
 * <b>Example Usage</b>
 * \code
 * // Setup an array for full ephemeris data storage.
 * RXN_FullEph_t FullArr[RXN_CONSTANT_NUM_PRNS];
 *
 * // Setup an array for RXN ephemeris data storage.
 * RXN_ephem_t RXNArr[RXN_CONSTANT_NUM_PRNS];
 *
 * // ToDo: Populate RXNArr using a GPStream product.
 *
 * // Convert RXNArr to FullArr. Use "0" for all values included within
 * // RXN_FullEph_t that are not supported by RXN_ephem_t.
 * // (e.g. CAOrPOnL2, iodc, etc).
 * RXN_CL_RXNToFull(RXNArr, FullArr, 0, 0, 0, 0, 0);
 *
 * \endcode
*/ 

void RXN_CL_RINEX_RXNToFull( RXN_ephem_t* pRXN, RXN_FullEph_t* pFull, U08 CAOrPOnL2,
                U16 iodc, U08 L2PData, S08 TGD, U08 AODO);

/** 
 * \brief 
 * Convert ephemeris data (RXN ephemeris to full ephemeris).
 *
 * \param pRXN 
 * [IN] Pointer to a source RXN_glonass_ephem_t struct.
 * \param pFull 
 * [OUT] Pointer to a destination RXN_FullEph_GLO_t struct.
 *
 * \details
 * <b>Description</b>\n
 * Convert ephemeris data from the standard expected by Rx Networks libraries
 * (RXN_glonass_ephem_t) to a same (comprehensive) structure that also supports elements
 * that are found within RINEX data files and used by other Rx Networks libraries (RXN_FullEph_GLO_t).
 *
 * <b>See Also</b>\n
 * RXN_CL_FullToRXN()
 *
 * <b>Example Usage</b>
 * \code
 * // Setup an array for full ephemeris data storage.
 * RXN_FullEph_GLO_t FullArr[RXN_CONSTANT_NUM_PRNS];
 *
 * // Setup an array for RXN ephemeris data storage.
 * RXN_glonass_ephem_t RXNArr[RXN_CONSTANT_NUM_PRNS];
 *
 * // ToDo: Populate RXNArr using a GPStream product.
 *
 * // Convert RXNArr to FullArr. Use "0" for all values included within
 * // RXN_FullEph_GLO_t that are not supported by RXN_ephem_t.
 * RXN_CL_RXNToFull_GLO(RXNArr, FullArr);
 *
 * \endcode
*/ 
void RXN_CL_RINEX_RXNToFull_GLO(RXN_glonass_ephem_t* pRXN, RXN_FullEph_GLO_t* pFull);
/** 
 * \brief 
 * Convert ephemeris data (full ephemeris to RXN ephemeris).
 *
 * \param pFull 
 * [IN] Pointer to a source RXN_FullEph_t struct.
 * \param pRXN 
 * [OUT] Pointer to a destination RXN_ephem_t struct.
 *
 * \details
 * <b>Description</b>\n
 * Convert ephemeris data from a comprehensive struct that supports elements
 * that are found within RINEX data files (RXN_FullEph_t) to a more compact 
 * struct that is expected by Rx Networks libraries as standard (RXN_ephem_t).
 * 
 * <b>See Also</b>\n
 * RXN_CL_RXNToFull()
 *
 * <b>Example Usage</b>
 * \code
 * // Setup an array for full ephemeris data storage.
 * RXN_FullEph_t FullArr[RXN_CONSTANT_NUM_PRNS];
 *
 * // Setup an array for RXN ephemeris data storage.
 * RXN_ephem_t RXNArr[RXN_CONSTANT_NUM_PRNS];
 *
 * // ToDo: Populate FullArr from a source such as RINEX (for example).
 *
 * // Convert FullArr to RXNArr.
 * RXN_CL_FullToRXN(FullArr, RXNArr);
 *
 * \endcode
*/ 
void RXN_CL_RINEX_FullToRXN(RXN_FullEph_t* pFull, RXN_ephem_t* pRXN);

/** 
 * \brief 
 * Convert ephemeris data (full ephemeris to RXN ephemeris).
 *
 * \param pFull 
 * [IN] Pointer to a source RXN_FullEph_GLO_t struct.
 * \param pRXN 
 * [OUT] Pointer to a destination RXN_gloanss_ephem_t struct.
 *
 * \details
 * <b>Description</b>\n
 * Convert ephemeris data from a comprehensive struct that supports elements
 * that are found within RINEX data files (RXN_glanass_FullEph_t) to a same  
 * struct that is expected by Rx Networks libraries as standard (RXN_ephem_GLO_t).
 * 
 * <b>See Also</b>\n
 * RXN_CL_RXNToFull()
 *
 * <b>Example Usage</b>
 * \code
 * // Setup an array for full ephemeris data storage.
 * RXN_FullEph_GLO_t FullArr[RXN_CONSTANT_NUM_PRNS];
 *
 * // Setup an array for RXN ephemeris data storage.
 * RXN_gloanss_ephem_t RXNArr[RXN_CONSTANT_NUM_PRNS];
 *
 * // ToDo: Populate FullArr from a source such as RINEX (for example).
 *
 * // Convert FullArr to RXNArr.
 * RXN_CL_FullToRXN_GLO(FullArr, RXNArr);
 *
 * \endcode
*/ 
void RXN_CL_RINEX_FullToRXN_GLO(RXN_FullEph_GLO_t* pFull,RXN_glonass_ephem_t* pRXN);


#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* RXN_CL_RX_H */
