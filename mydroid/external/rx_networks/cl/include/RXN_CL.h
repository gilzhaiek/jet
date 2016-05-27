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
 * 
 */

#ifndef RXN_CL_H
#define RXN_CL_H

#include "RXN_data_types.h"     /* Defines RXN Data Types. */
#include "RXN_constants.h"      /* Defines RXN Constants. */
#include "RXN_structs.h"        /* Defines RXN Data Structs. */
#include "RXN_debug.h"          /* Defines RXN Debug APIs and structs. */ 

#include "RXN_MSL.h"            /* Defines constants, structs and data types used by MSL, CL and RL.*/

#include "RXN_CL_Constants.h"   /* Defines constants used specifically by the CL. */

#ifdef __cplusplus
extern "C" {
#endif

/**
 * \brief
 * Enumeration of the currently supported reset or restart types.
 */
typedef enum {
  RXN_CL_NO_RESTART = 0,										/*!< No receiver restart.*/
  RXN_CL_CLEAR_ALL_RESTART,										/*!< Restart without time, position, ephemeris, Almanac */
  RXN_CL_COLD_RESTART = RXN_CL_CLEAR_ALL_RESTART,				/*!< Cold receiver restart. */
  RXN_CL_CLEAR_EPHEMERIS_RESTART,								/*!< Restart without ephemeris */
  RXN_CL_WARM_RESTART = RXN_CL_CLEAR_EPHEMERIS_RESTART,			/*!< Warm reciever restart.*/
  RXN_CL_CLEAR_NOTHING_RESTART,									/*!< REstart without clear anything*/
  RXN_CL_HOT_RESTART = RXN_CL_CLEAR_NOTHING_RESTART,			/*!< Hot receiver restart.*/
  RXN_CL_CLEAR_TIME = (1<<2),									/*!< Clear time */
  RXN_CL_CLEAR_POSITION = (1<<3),								/*!< Clear Position */
  RXN_CL_CLEAR_EPHEMERIS = (1<<4),								/*!< Clear ephemeris */
  RXN_CL_CLEAR_ALMANAC = (1<<5),								/*!< Clear Almanac */
  RXN_CL_ASSIST_EXEPHEMERIS = (1<<6),							/*!< Provide ephemeris assistance */
  RXN_CL_ASSIST_LOC = (1<<7),									/*!< Provide location assistance */
  RXN_CL_ASSIST_TIME = (1<<8),									/*!< Provide time assistance */
  RXN_CL_RESTART_END = ((1<<9) + 1)								/*!< Restart boundary.*/
}RXN_CL_Restarts_t;

/**
 * \brief
 * Struct describing fix data.
 */
typedef struct RXN_CL_FixData
{
  RXN_LLA_LOC_t LLA;					/*!< LLA Fix Location. */
  RXN_ECEF_LOC_t ECEF;					/*!< ECEF Fix Location */
  U08 numPRN;							/*!< Number of PRNs tracking. */
  U08 PRNs[RXN_CONSTANT_NUM_PRNS];		/*!< PRNs Tracking. svid, PRNs used for navigation*/

  U08 numCh;							/*!< PRNs Tracking. number of channel */
  U08 viewPRNs[RXN_CONSTANT_NUM_PRNS];  /*!< PRNs Tracking. all PRNs in view */
  U08 cno[RXN_CONSTANT_NUM_PRNS];		/*!< PRNs Tracking. carrier to Nosize Ration */
  S08 elev[RXN_CONSTANT_NUM_PRNS];		/*!< PRNs Tracking. elevation degree */
  S16 azim[RXN_CONSTANT_NUM_PRNS];		/*!< PRNs Tracking. azimuth degree*/
  U16 svFlags[RXN_CONSTANT_NUM_PRNS];	/*!< PRNs Tracking. SV status flag used for navi*/

  U08 numEphSV;							/*!< PRNs Tracking.  number of SV has valid Ephemris*/
  U08 viewEphSV[RXN_CONSTANT_NUM_PRNS];	/*!< PRNs Tracking.  list of SV has valid ephemeris */
  U08 ageEphSV[RXN_CONSTANT_NUM_PRNS];	/*!< PRNs Tracking. ephemeris age */
  U08 flagEphSV[RXN_CONSTANT_NUM_PRNS];	/*!< PRNs Tracking. ephermis flag */
  U32 gpsTime[RXN_CONSTANT_NUM_PRNS];   /*!< PRNs Tracking. SV status flag used for navi*/

  U08 fixDimensions;					/*!< 0-1=No Fix, 2=2D, 3=3D. */
  float HDOP;							/*!< HDOP. */
  U32 pAcc;								/* 3D Position Accuracy Estimate */
} RXN_CL_FixData_t;

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
 *
 * <b>Example Usage</b>
 * \code
 * // Declare a string to hold version info.
 * char version[RXN_CONSTANT_VERSION_STRING_LENGTH];
 * 
 * // Retrieve and output version information.
 * RXN_CL_Get_API_Version(version);
 * printf("RXN_CL_Get_API_Version(): %s\n", version);
 * \endcode
 *
 * <b>Output</b>\n
 * RXN_CL_Get_API_Version(): 1.2.0
 */
U16 RXN_CL_Get_API_Version(char version[RXN_CONSTANT_VERSION_STRING_LENGTH]);

/**
 * \brief 
 * Retrieve a chipset descriptor from the library (for which it has been compiled).
 *
 * \param chipset [OUT] A memory location to be filled with a list of supported chipsets. 
 *
 * \return RXN_SUCCESS If the list is returned successfully (always).
 *
 * <b>Example Usage</b>
 * \code
 * // Declare a string to hold a chipset label.
 * char chipset[RXN_CL_CHIP_MAX_STR_LEN];
 * 
 * // Retrieve and output the chipset label.
 * RXN_CL_Get_Chipset(chipset);
 * printf("RXN_CL_Get_Chipset(): %s\n", chipset);
 * \endcode
 *
 * <b>Output</b>\n
 * RXN_CL_Get_Chipset(): "u-blox 5.0" 
 */
U16 RXN_CL_Get_Chipset(char chipset[RXN_CL_CHIP_MAX_STR_LEN]);

/** 
 * \brief 
 * Initialize the CL library for use with a specific chipset and version of chipset support.
 *
 * \param chipsetVer 
 * [IN] Specifies a chipset version. CL implementations may be different for different
 * versions of chipset firmware (for example). The chipset version number can be checked within
 * CL implementation code to support different CL functionality for different chipset versions.
 * \param config 
 * [IN] Specifies the port to open during chipset initialization as well as any
 * other config parameters. The format of the config string is chipset specific. Some chipsets
 * (typically those supported by a host library or protocol interface) are initialized with a com
 * port string and baudrate. Note that a debug log severity max level and debug zone mask are also
 * stipulated. See details below.
 * \param phandle 
 * [OUT] The pointer to a handle that identifies a CL instance for subsequent CL use.
 *
 * \return RXN_SUCCESS if the CL is initialized successfully. 
 * \return RXN_CL_NO_CHIPSET_SUPPORT_ERR if the compile of the CL does not include support for the requested chipset.
 * \return RXN_CL_CHIPSET_INIT_ERR if the chipset could not be initiated properly. Consult the CL log for more info
 * as this error is chipset specific.
 * \return RXN_CL_TOO_MANY_INSTANCES_ERR if too many instances of the CL are currently initialized.
 * \return RXN_CL_CONFIG_FORMAT_ERR if the config string is malformed (e.g. does not include '|' if required),
 * \return RXN_CL_OPEN_OR_CLOSE_ERR if the interface could not be opened (i.e. file or port open error).
 * \return RXN_CL_PORT_SETUP_ERR if a com port could not be setup properly.
 *
 * \details
 * <b>Description</b>\n
 * Initialize the CL library before using it. This will setup a CL instance (with associate
 * handle). Instances are required to support clients that may have to utilize multiple copies
 * of the CL at the same time.\n
 * Specify a debug log max severity threshold to determine which CL log entries are processed. CL log severities
 * defines are:
 * <ul>
 * <li> RXN_LOG_SEV_FATAL (0) - Log entry describing a fatal issue (CL will crash).
 * <li> RXN_LOG_SEV_ERROR (1) - Log entry describing an error.
 * <li> RXN_LOG_SEV_WARNING (2) - Log entry describing a warning.
 * <li> RXN_LOG_SEV_INFO (3) - Log entry includes general information.
 * <li> RXN_LOG_SEV_TRACE (4) - Log entry includes trace data that can be used for troubleshooting.
 * </ul>
 * When a severity threshold is set, all logs with severity matching, or more serious than
 * this threshold will be processed. The default threshold of RXN_LOG_SEV_ERROR will be used
 * when no threshold is specified (i.e. logs entries of severity RXN_LOG_SEV_ERROR and
 * RXN_LOG_SEV_FATAL will be processed). To specify a severity threshold use "S:n" or "s:n" 
 * as the second last config parameter where n is the threshold value (i.e. "...|S:4|..."
 * to specify RXN_LOG_SEV_TRACE threshold).\n
 * Specify a debug zone mask to stipulate which debug zone log entries are processed. CL log
 * debug zone defines are:
 * <ul>
 * <li> RXN_LOG_ZONE01 (1 - 0x0001) - Misc functionality (catch all when no other zones apply).
 * <li> RXN_LOG_ZONE02 (2 - 0x0002) - Third party log entries.
 * <li> RXN_LOG_ZONE03 (4 - 0x0004) - Init/Shutdown functionality.
 * <li> RXN_LOG_ZONE04 (8 - 0x0008) - Serial and socket I/O and other I/O functionality.
 * <li> RXN_LOG_ZONE05 (16 - 0x0010) - File I/O functionality.
 * <li> RXN_LOG_ZONE06 (32 - 0x0020) - Security functionality.
 * <li> RXN_LOG_ZONE07 - RXN_LOG_ZONE16 - Unused at present.
 * </ul>
 *
 * When a debug zone mask is set, this mask will be used to filter out which zone log entries are
 * processed. E.g. debug zone mask of 0x0003 will only support log entries with specified zones
 * RXN_LOG_ZONE01 and RXN_LOG_ZONE02. A default zone mask of 0xFFFF will be used when no mask is
 * specified (supports logging all entries regardless of their specified zone). To specify a zone mask
 * use "Z:n", "Z:0xn", "z:n" or "z:0xn" as the last config parameter where n is the zone mask
 * (i.e. "...|Z:0x000F").
 *
 * <b>See Also</b>\n
 * RXN_CL_Uninitialize()
 *
 * <b>Example Usage</b>
 * \code
 * U16 Result = RXN_FAIL;
 * char UBLOXConfig[RXN_CL_CONFIG_MAX_STR_LEN];
 * U16 CLHandle = 0;
 *
 * // Initialize logging first so that any problems that occur during subsequent CL
 * // calls will be logged for troubleshooting.
 * Result = RXN_CL_Log_Init("./CL_Log.txt");
 *
 * // Handle errors.
 * if(Result != RXN_SUCCESS)
 * {
 *    // ToDo: Notify the user that a log will not be supported.
 * }
 *
 * // Setup a config string to support a USB Virtual Com port on COM3 at 115200 kBps
 * // with severity level 1 logging (Fatal and Error log entries only) and debug zone
 * // mask of 0xFFFF (permits logging for all zones).
 * sprintf(UBLOXConfig, "P:\\\\.\\COM3|B:115200|S:1|Z:0xFFFF");
 *
 * // Initialize the CL to support a u-blox chipset with version 1.00 firmware.
 * Result = RXN_CL_Initialize(100, UBLOXConfig, &CLHandle);
 *
 * // Handle errors.
 * if(Result != RXN_SUCCESS)
 * {
 *    // ToDo: Handle a CL initialization error. May have to terminate function.
 * }
 *
 * // ToDo; Call other functions using this instance of the CL by utilizing CLHandle.
 *
 * // Shutdown the CL instance.
 * Result = RXN_CL_Uninitialize(CLHandle);
 *
 * // Handle errors.
 * if(Result != RXN_SUCCESS)
 * {
 *    // ToDo: Handle a CL un-initialization error.
 * }
 * 
 * // Shutdown logging. Always shutdown logging last to ensure that any CL
 * issues that preceed this call are logged.
 * RXN_CL_Log_UnInit();
 *
 * \endcode
 */ 
U16 RXN_CL_Initialize(U16 chipsetVer, char config[RXN_CL_CONFIG_MAX_STR_LEN], U16* phandle);

/**
 * \brief 
 * UnInitialize a CL library instance.
 * 
 * \param handle 
 * [IN] A handle that identifies a CL instance.
 *
 * \return RXN_SUCCESS if the CL is uninitialized successfully. 
 * \return RXN_CL_INSTANCE_NOT_FOUND_ERR if an instance corresponding the provided handle cannot be found.
 * \return RXN_CL_OPEN_OR_CLOSE_ERR if a port could not be closed properly.
 *
 * \details
 * <b>Description</b>\n
 * Shut down a previously initialized CL instance to free up instances and resources for subsequent use.
 *
 * <b>See Also</b>\n
 * RXN_CL_Initialize()
 *
 * <b>Example Usage</b>
 * \code
 * See the example code provided for RXN_CL_Initialize().
 * \endcode
 */ 
U16 RXN_CL_Uninitialize(U16 handle);

/** 
 * \brief 
 * Initialize a log for CL library use.
 *
 * \param logFile 
 * [IN] Explicit or relative path to the log file. Must specify a full path including file name.
 *
 * \return RXN_SUCCESS if the log is initialized successfully.
 * \return RXN_CL_TOO_MANY_INSTANCES_ERR - if an log has already been opened.
 * \return RXN_CL_OPEN_OR_CLOSE_ERR - if a log file cannot be opened.
 *
 * \details
 * <b>Description</b>\n
 * Open a log file that can be used by library implementation code directly, or by an third party library that is
 * being hosted by the CL (i.e. for hosted systems). If the log file exists, it will be replaced.
 * If the log file does not exist, it will be created. Note that all instances of the CL will share a
 * common log.
 *
 * <b>See Also</b>\n
 * RXN_CL_Log_UnInit()
 *
 * <b>Example Usage</b>
 * \code
 * See the example code provided for RXN_CL_Initialize().
 * \endcode
 */ 
U16 RXN_CL_Log_Init(const char logFile[RXN_CL_MAX_LOG_PATH]);

/** 
 * \brief 
 * Un-Initialize a log that has been opened for CL library use.
 *
 * \details
 * <b>Description</b>\n
 * Close a previously opened log file (if opened).
 *
 * <b>See Also</b>\n
 * RXN_CL_Log_Init()
 *
 * <b>Example Usage</b>
 * \code
 * See the example code provided for RXN_CL_Initialize().
 * \endcode
 */ 
void RXN_CL_Log_UnInit(void);

/** 
 * \brief 
 * Retrieve version info from the chipset.
 *
 * \param handle 
 * [IN] A handle that identifies a CL instance.
 * \param version 
 * [OUT] Buffer to copy a chipset version string into.
 *
 * \return RXN_SUCCESS if version data is retrieved successfully.
 * \return RXN_CL_INSTANCE_NOT_FOUND_ERR if the handle provided does not support an open CL instance.
 * \return RXN_CL_NO_CHIPSET_SUPPORT_ERR if the chipset selected within RXN_CL_Initialize() does
 * not support this function.
 * \return RXN_CL_OPEN_OR_CLOSE_ERR if a port supporting access to the chipset has not been opened.
 * \return RXN_CL_CHIPSET_READ_ERR if the version info cannot be read for the chipset.
 * \return RXN_CL_CHIPSET_READ_TIMEOUT if a timeout occurred while waiting for a chipset msg.
 * \return RXN_CL_MSG_NACK_ERROR if the request for version data is nacked.
 * 
 * <b>Notes:</b>
 * <ul>
 * <li>The version string returned is intended to show firmware version for chipsets with a
 * protocol interface or lib version for hosted chipsets.
 * <li>If the chipset does not support version access, "N/A" (or similar) will be provided
 * as a version string.
 * <li>The string will be terminated by "\0".
 * <li>String allocated length (including support for "\0") must be RXN_CONSTANT_VERSION_STRING_LENGTH.
 * </ul>
 *
 * <b>Example Usage</b>
 * \code
 * // ToDo: Initialize a CL instance for use. See RXN_CL_Initialize() example usage.
 * // Initialization will yield a "handle" for use within the
 * // RXN_CL_Get_Chipset_Support_Version() call below.
 *
 * // Declare a string to hold version info.
 * char version[RXN_CONSTANT_VERSION_STRING_LENGTH];
 * 
 * // Retrieve version information.
 * if(RXN_CL_Get_Chipset_Support_Version(handle, version) != RXN_SUCCESS)
 * {
 *    // ToDo: Handle error.
 * }
 *
 * // Print out the version.
 * printf("RXN_CL_Get_Chipset_Support_Version(): %s\n", version);
 *
 * \endcode
 *
 * <b>Output</b>\n
 * RXN_CL_Get_Chipset_Support_Version(): Version. Major: 1, Minor: 2
 */ 
U16 RXN_CL_Get_Chipset_Support_Version(U16 handle, char version[RXN_CONSTANT_VERSION_STRING_LENGTH]);

/** 
 * \brief 
 * Give the chipset CL implementation time to perform periodic tasks.
 *
 * \param handle 
 * [IN] A handle that identifies a CL instance.
 *
 * \return RXN_SUCCESS if work is performed successfully.
 * \return RXN_CL_INSTANCE_NOT_FOUND_ERR if the handle provided does not support an open CL instance.
 * \return RXN_CL_OPEN_OR_CLOSE_ERR if a port supporting access to the chipset has not been opened.
 * (chipsets with protocol interface only).
 * \return RXN_CL_CHIPSET_READ_ERR if a chipset read error occurred (chipsets with protocol interface only).
 * \return RXN_CL_CHIPSET_WRITE_ERR if a chipset read error occurred (chipsets with protocol interface only).
 * \return RXN_CL_MSG_PARSE_ERROR if a chipset msg parse occurred (chipsets with protocol interface only).
 * \return RXN_CL_MSG_NACK_ERROR if a chipset msg is nack'd (chipsets with protocol interface only).
 * 
 * \details
 * <b>Description</b>\n
 * The RXN_CL_Work function is intended to support CL implementations that require periodic processing
 * (e.g. to poll looking for ephemeris to see if a restart occurred, or for hosted chipset libraries - perform
 * internal work). Applications consuming any CL implementation requiring periodic processing should call this
 * function on a periodic basis. This periodic processing will not be implemented within threads spawned from
 * CL implementation code as such threads would break the CL's requirement for single threaded app support.
 *
 * <b>Notes:</b>
 * <ul>
 * <li>CL consumers should call this work function at period no greater than 50mSec. This requirement
 * will ensure that any CL implementation requiring very periodic processing can be supported.
 * <li>If any CL implementation has a min periodicity requirement, that implementation should "Sleep" within
 * it's CL RXN_CL_Work() implementation to satisfy this requirement. (E.g. CL implementation requires
 * that its work function be called with cycle time of no less than 100 mSec. CL implementation should keep
 * track of the tick count at the beginning of each CL RXN_CL_Work() call and sleep the requried number
 * of mSec to ensure that internally, functions are not called more than every 100mSec.
 * </ul>
 *
 * <b>Example Usage</b>
 * \code
 * // Application's main thread, or spawed thread (if permitted).
 * while(true)
 * {
 *    // Give the CL implementation cycles to do its work.
 *    RXN_CL_Work();
 *
 *    // Do other periodic tasks.
 *
 *    // Ensure that other tasks have cycles and don't call
 *    // RXN_CL_Work() too frequently. Sleep the thread for 40 mSec.
 *    Sleep(40);
 * }
 *
 * \endcode
 *
 */ 
U16 RXN_CL_Work(U16 handle);

/** 
 * \brief 
 * Restart the chipset.
 *
 * \param handle 
 * [IN] A handle that identifies a CL instance.
 * \param restartType 
 * [IN] A value (enumerated RXN_CL_Restarts_t) that identifies what type of restart to perform.
 * \param pRefLoc 
 * [IN] A pointer to a RXN_RefLocation_t struct that includes reference location data, or NULL (0) if ref
 * location set not supported within reset msg or function or is not required.
 * \param pRefTime 
 * [IN] A pointer to a RXN_RefTime_t struct that includes reference time data, or NULL (0) if ref
 * time set not supported within reset msg or function or is not required.
 *
 * \return RXN_SUCCESS if a restart is successful.
 * \return RXN_CL_INSTANCE_NOT_FOUND_ERR if the handle provided does not support an open CL instance.
 * \return RXN_CL_NO_CHIPSET_SUPPORT_ERR if the chipset selected within RXN_CL_Initialize() does
 * not support this function.
 * \return RXN_CL_INVALID_RESTART_TYPE_ERR if an invalid restartType is specified (must be RXN_CL_Restarts_t
 * enum member).
 * \return RXN_CL_INVALID_REF_LOC_FORMAT_ERR if the pRefLoc format value is not either RXN_LOC_ECEF
 * or RXN_LOC_LLA.
 * \return RXN_CL_OPEN_OR_CLOSE_ERR if a port supporting access to the chipset has not been opened.
 * \return RXN_CL_CHIPSET_WRITE_ERR if a request msg could not be sent to the chipset.
 * \return RXN_CL_CHIPSET_READ_ERR if a response msg could not be read from the chipset.
 * \return RXN_CL_CHIPSET_READ_TIMEOUT if a timeout occurred  while waiting for a chipset msg.
 * \return RXN_CL_MSG_NACK_ERROR if restart request is nacked.
 * 
 * \details
 * <b>Description</b>\n
 * Perform a cold, warm or hot restart. If a chipset supports setting reference location and time
 * within the reset msg or function, include these structures. If not, use "0" for reference location
 * and/or reference time pointers.
 * 
 * <b>Notes:</b>
 * <ul>
 * <li>If a separate msg(s) or function call(s) is required to set reference location and/or time,
 * this msg will be sent or function call made after the reset msg or function call. Use
 * RXN_CL_SetRefLocTime() for this purpose.
 * <li>Integrators can elect to set pRefLoc and/or pRefTime to NULL to avoid setting either
 * or both of these parameters within a chipset during restart.
 * <li>If a pRefLoc and/or pRefTime provided cannot be used by the chipset, these parameters
 * may be ignored. Chipset behaviour may differ from chipset to chipset. For example, some
 * chipsets may require that a reference location be provided if a reference time is processed
 * as both may be included within a function to set reference data.
 * </ul>
 *
 * <b>See Also</b>\n
 * RXN_CL_SetRefLocTime()
 *
 * <b>Example Usage</b>
 * \code
 * // ToDo: Initialize a CL instance for use. See RXN_CL_Initialize() example usage.
 * // Initialization will yield a "handle" for use within the RXN_CL_Restart() call below.
 * 
 * // Setup ref location data.
 * RXN_RefLocation_t RefLoc;
 * memset((void*) &RefLoc, 0, sizeof(RXN_RefLocation_t));
 * RefLoc.format = RXN_LOC_LLA;
 * RefLoc.LLA.Lat = 49.28381;       // RXN Office
 * RefLoc.LLA.Lon = -123.10463;     // RXN Office
 * RefLoc.LLA.Alt = 10.60308;       // RXN Office
 *
 * // Setup ref time data.
 * RXN_RefTime_t RefTime;
 * memset((void*) &RefTime, 0, sizeof(RXN_RefTime_t));
 * RefTime.weekNum = CL_GetGPSWeekNum();  // CL_GetGPSWeekNum() is util fcn supported within the CL.
 * RefTime.TOWmSec = CL_GetGPSTOW();      // CL_GetGPSTOW() is a util fcn supported within the CL.
 * RefTime.TOWnSec = 0;
 * RefTime.TAccmSec = 0;
 * RefTime.TAccnSec = 0;
 *
 * // Perform a cold restart.
 * if(RXN_CL_Restart(handle, RXN_CL_COLD_RESTART, &RefLoc, &RefTime) != RXN_SUCCESS)
 * {
 *    // ToDo: Handle error.
 * }
 * \endcode
*/ 
U16 RXN_CL_Restart(U16 handle, RXN_CL_Restarts_t restartType, RXN_RefLocation_t* pRefLoc, RXN_RefTime_t* pRefTime);

/** 
 * \brief 
 * Set a reference location and/or time within the chipset.
 *
 * \param handle 
 * [IN] A handle that identifies a CL instance.
 * \param pRefLoc 
 * [IN] A pointer to a RXN_RefLocation_t struct that includes reference location data, or NULL (0) if ref
 * location set not supported within reset msg or function or is not required..
 * \param pRefTime 
 * [IN] A pointer to a RXN_RefTime_t struct that includes reference time data, or NULL (0) if ref
 * time set not supported within reset msg or function or is not required..
 *
 * \return RXN_SUCCESS if either ref location or time set is successful.
 * \return RXN_CL_INSTANCE_NOT_FOUND_ERR if the handle provided does not support an open CL instance.
 * \return RXN_CL_NO_CHIPSET_SUPPORT_ERR if the chipset selected within RXN_CL_Initialize() does
 * not support this function.
 * \return RXN_CL_INVALID_REF_LOC_FORMAT_ERR if the pRefLoc format value is not either RXN_LOC_ECEF
 * or RXN_LOC_LLA.
 * \return RXN_CL_OPEN_OR_CLOSE_ERR if a port supporting access to the chipset has not been opened.
 * \return RXN_CL_CHIPSET_WRITE_ERR if a request msg could not be sent to the chipset.
 * \return RXN_CL_CHIPSET_READ_ERR if a response msg could not be read from the chipset.
 * \return RXN_CL_CHIPSET_READ_TIMEOUT if a timeout occurred  while waiting for a chipset msg.
 * \return RXN_CL_MSG_NACK_ERROR if request to set init time and location is nacked.
 *
 * \details
 * <b>Description</b>\n
 * Set reference location or time (or both). Only if a valid value pointer is provided will a value
 * be set within the chipset. Typically, these values are written to chipsets after a restart
 * occurs. On some chipsets, these values are set as a restart is performed. If the CL is used
 * to initiate such a restart for such chipsets, the RXN_CL_Restart() can support reference 
 * location and time set. RXN_CL_SetRefLocTime() use is intended for integrations where applications
 * will react to restarts and set reference location and time data as required.
 * 
 * <b>Notes:</b>
 * <ul>
 * <li>Integrators can elect to set pRefLoc and/or pRefTime to NULL to avoid setting either
 * of these parameters within a chipset.
 * <li>If a pRefLoc and/or pRefTime provided cannot be used by the chipset, these parameters
 * may be ignored.
 * </ul>
 *
 * <b>See Also</b>\n
 * RXN_CL_Restart()
 *
 * <b>Example Usage</b>
 * \code
 * // ToDo: Initialize a CL instance for use. See RXN_CL_Initialize() example usage.
 * // Initialization will yield a "handle" for use within the RXN_CL_SetRefLocTime() call below.
 * 
 * // Setup ref location data.
 * RXN_RefLocation_t RefLoc;
 * memset((void*) &RefLoc, 0, sizeof(RXN_RefLocation_t));
 * RefLoc.format = RXN_LOC_LLA;
 * RefLoc.LLA.Lat = 49.28381;       // RXN Office
 * RefLoc.LLA.Lon = -123.10463;     // RXN Office
 * RefLoc.LLA.Alt = 10.60308;       // RXN Office
 *
 * // Wait on a restart (by polling, waiting for an event, msg, interrupt, etc). Very high level
 * eg code below.
 * if(RestartPerformed)
 * {
 *    // Setup ref time data.
 *    RXN_RefTime_t RefTime;
 *    memset((void*) &RefTime, 0, sizeof(RXN_RefTime_t));
 *    RefTime.weekNum = CL_GetGPSWeekNum();  // CL_GetGPSWeekNum() is util fcn supported within the CL.
 *    RefTime.TOWmSec = CL_GetGPSTOW();      // CL_GetGPSTOW() is a util fcn supported within the CL.
 *    RefTime.TOWnSec = 0;
 *    RefTime.TAccmSec = 0;
 *    RefTime.TAccnSec = 0;
 *
 *    // Set the reference time and location.
 *    if(RXN_CL_SetRefLocTime(handle, &RefLoc, &RefTime) != RXN_SUCCESS)
 *    {
 *        // ToDo: Handle error.
 *    }
 * }
 * else
 * {
 *    // ToDo: Loop back to check for restart.
 * }
 * 
 * \endcode
*/ 
U16 RXN_CL_SetRefLocTime(U16 handle, RXN_RefLocation_t* pRefLoc, RXN_RefTime_t* pRefTime);

/** 
 * \brief 
 * Read broadcast ephemeris data.
 *
 * \param handle 
 * [IN] A handle that identifies a CL instance.
 * \param pNavDataList 
 * [OUT] A pointer to a RXN_MSL_NavDataList_t struct that will be populated with ephemeris
 * data.
 * \param constel
 * [IN] The constellation 
 *
 * \return RXN_SUCCESS if ephemeris is read successfully.
 * \return RXN_CL_INSTANCE_NOT_FOUND_ERR if the handle provided does not support an open CL instance.
 * \return RXN_CL_NO_CHIPSET_SUPPORT_ERR if the chipset selected within RXN_CL_Initialize() does
 * not support this function.
 * \return RXN_CL_OPEN_OR_CLOSE_ERR if a port supporting access to the chipset has not been opened.
 * \return RXN_CL_CHIPSET_WRITE_ERR if a request msg could not be sent to the chipset.
 * \return RXN_CL_CHIPSET_READ_ERR if a response msg could not be read from the chipset.
 * \return RXN_CL_CHIPSET_READ_TIMEOUT if a timeout occurred  while waiting for a chipset msg.
 * \return RXN_CL_MSG_NACK_ERROR if a read request is nacked.
 * \return RXN_CL_ALLOCATION_ERROR if the array size specified is invalid (< 1 or > 32).
 *
 * \details
 * <b>Description</b>\n
 * Read an array of broadcast ephemeris (BCE) data structs through a previously initialized
 * CL instance.
 * 
 * <b>Notes:</b>
 * <ul>
 * <li>For any given PRN within PRNArr, the prn value will be set to "0" if ephemeris
 * was not available for that PRN.
 * <li>To convert the PRNArr elements returned from RXN_FullEph_t to RXN_ephem_t
 * as supported within Rx Networks GPStream products, utilize RXN_CL_FullToRXN().
 * <li>Reading broadcast ephemeris from a chipset may take up to 32 function calls
 * or protocol messages. For this reason, this function may not return for several seconds.
 * </ul>
 *
 * <b>See Also</b>\n
 * RXN_CL_WriteEphemeris(), RXN_CL_FullToRXN()
 *
 * <b>Example Usage</b>
 * \code
 * // ToDo: Initialize a CL instance for use. See RXN_CL_Initialize() example usage.
 * // Initialization will yield a "handle" for use within the RXN_CL_ReadEphemeris() call below.
 *
 * // Setup an array for ephemeris data store.
 * RXN_FullEphem_u PRNEphArr[RXN_CONSTANT_NUM_PRNS];
 *
 * // Read ephemeris into pNavDataList.
 * if(RXN_CL_ReadEphemeris(handle, pNavDataList, RXN_GPS_CONSTEL) != RXN_SUCCESS)
 * {
 *    // ToDo: Handle error.
 * }
 *
 * // ToDo: Do something meaningful with pNavDataList such as set it within SAGPS.
 * 
 * \endcode
*/ 
U16 RXN_CL_ReadEphemeris(U16 handle, RXN_MSL_NavDataList_t* pNavDataList, RXN_constel_t constel);

/** 
 * \brief 
 * Write extended ephemeris data.
 *
 * \param handle 
 * [IN] A handle that identifies a CL instance.
 * \param pNavDataList 
 * [IN] A pointer to a RXN_MSL_NavDataList_t struct that is populated with ephemeris
 * data.
 * \param constel
 * [IN] The constellation
 *
 * \return RXN_SUCCESS if ephemeris is written successfully.
 * \return RXN_CL_INSTANCE_NOT_FOUND_ERR if the handle provided does not support an open CL instance.
 * \return RXN_CL_NO_CHIPSET_SUPPORT_ERR if the chipset selected within RXN_CL_Initialize() does
 * not support this function.
 * \return RXN_CL_OPEN_OR_CLOSE_ERR if a port supporting access to the chipset has not been opened.
 * \return RXN_CL_CHIPSET_WRITE_ERR if a request msg could not be sent to the chipset.
 * \return RXN_CL_CHIPSET_READ_ERR if a response msg could not be read from the chipset.
 * \return RXN_CL_CHIPSET_READ_TIMEOUT if a timeout occurred  while waiting for a chipset msg.
 * \return RXN_CL_MSG_NACK_ERROR if a write request is nacked.
 * \return RXN_CL_ALLOCATION_ERROR if the array size specified is invalid (< 1 or > 32).
 *
 * \details
 * <b>Description</b>\n
 * Write an array of extended ephemeris (EE) data structs through a previously initialized
 * CL instance.
 * 
 * <b>Notes:</b>
 * <ul>
 * <li>To convert ephemeris that is acquired from Rx Networks GPStream products from
 * RXN_ephem_t to RXN_FullEph_t, utilize the RXN_CL_RXNToFull() function.
 * <li>It is expected that all PRNArr records will contain valid ephemeris data.
 * CL consumers will not have to validate whether a PRNArr element contains valid
 * data by checking that the "prn" is non-zero.
 * </ul>
 *
 * <b>See Also</b>\n
 * RXN_CL_ReadEphemeris(), RXN_CL_RXNToFull()
 *
 * <b>Example Usage</b>
 * \code
 * // ToDo: Initialize a CL instance for use. See RXN_CL_Initialize() example usage.
 * // Initialization will yield a "handle" for use within the RXN_CL_Restart() call below.
 *
 * // Setup an array for ephemeris data store.
 * RXN_FullEphem_u PRNEphArr[RXN_CONSTANT_NUM_PRNS];
 *
 * // ToDo: Populate PRNEphArr utilizing an Rx Networks GPStream API.
 *
 * // Write ephemeris into a chipset from pNavDataList.
 * if(RXN_CL_WriteEphemeris(handle, pNavDataList, RXN_GPS_CONSTEL) != RXN_SUCCESS)
 * {
 *    // ToDo: Handle error.
 * }
 *
 * \endcode
*/ 
U16 RXN_CL_WriteEphemeris(U16 handle, RXN_MSL_NavDataList_t* pNavDataList, RXN_constel_t constel);

/** 
 * \brief 
 * Get fix data.
 *
 * \param handle 
 * [IN] A handle that identifies a CL instance.
 * \param pFixData 
 * [OUT] Pointer to a RXN_CL_FixData_t struct that will be populated with fix data.
 *
 * \return RXN_SUCCESS if fix data is read successfully.
 * \return RXN_CL_INSTANCE_NOT_FOUND_ERR if the handle provided does not support an open CL instance.
 * \return RXN_CL_NO_CHIPSET_SUPPORT_ERR if the chipset selected within RXN_CL_Initialize() does
 * not support this function.
 * \return RXN_CL_OPEN_OR_CLOSE_ERR if a port supporting access to the chipset has not been opened.
 * \return RXN_CL_CHIPSET_WRITE_ERR if a request msg could not be sent to the chipset.
 * \return RXN_CL_CHIPSET_READ_ERR if a response msg could not be read from the chipset.
 * \return RXN_CL_CHIPSET_READ_TIMEOUT if a timeout occurred  while waiting for a chipset msg.
 * \return RXN_CL_MSG_PARSE_ERROR if a response msg could not be parsed properly (i.e. msg format error).
 * \return RXN_CL_MSG_NACK_ERROR if a fix data request is nacked.
 *
 * \details
 * <b>Description</b>\n
 * Get fix data from a connected chipset. Fix data will include a fix status flag.
 * 
 * <b>Notes:</b>
 * <ul>
 * <li>RXN_CL_FixData_t supports both ECEF and LLA data elements. Both LLA and ECEF
 * data elements will be set with location data regardless of the location data format
 * supported by the chipset (i.e. if the chipset supports LLA, ECEF will be obtained
 * by converting LLA data received from the chipset and visa-versa). This conversion
 * is supported by internal CL utility functions (not exposed to CL consumers).
 * </ul>
 *
 * <b>Example Usage</b>
 * \code
 * // ToDo: Initialize a CL instance for use. See RXN_CL_Initialize() example usage.
 * // Initialization will yield a "handle" for use within the RXN_CL_Restart() call below.
 *
 * // Setup a struct to store fix data.
 * RXN_CL_FixData_t FixData;
 *
 * // Read fix data.
 * if(RXN_CL_GetFixData(handle, &FixData) != RXN_SUCCESS)
 * {
 *    // ToDo: Handle error.
 * }
 *
 * // ToDo: Do something meaningful with FixData.
 *
 * \endcode
*/ 
U16 RXN_CL_GetFixData(U16 handle, RXN_CL_FixData_t* pFixData);

/** 
 * \brief 
 * Get the current GPS time from the reciever (after the reciver fixes).
 *
 * \param handle
 * [IN] A handle that identifies a CL instance.
 * \param pRcvrWeekNum 
 * [OUT] Pointer to the current GPS week number (since GPS start in 1970).
 * \param pRcvrTOW
 * [OUT] Pointer to the current GPS time of week.
 *
 * \return RXN_SUCCESS if fix data is read successfully.
 * \return RXN_CL_DATA_UNAVAILABLE if the data is unavailable within the chipset at present.
 * \return RXN_CL_INSTANCE_NOT_FOUND_ERR if the handle provided does not support an open CL instance.
 * \return RXN_CL_NO_CHIPSET_SUPPORT_ERR if the chipset selected within RXN_CL_Initialize() does
 * not support this function.
 * \return RXN_CL_OPEN_OR_CLOSE_ERR if a port supporting access to the chipset has not been opened.
 * \return RXN_CL_CHIPSET_WRITE_ERR if a request msg could not be sent to the chipset.
 * \return RXN_CL_CHIPSET_READ_ERR if a response msg could not be read from the chipset.
 * \return RXN_CL_CHIPSET_READ_TIMEOUT if a timeout occurred  while waiting for a chipset msg.
 * \return RXN_CL_MSG_PARSE_ERROR if a response msg could not be parsed properly (i.e. msg format error).
 * \return RXN_CL_MSG_NACK_ERROR if a fix data request is nacked.
 *
 * \details
 * <b>Description</b>\n
 * Get the current GPS time from a connected chipset.
 * 
 * <b>Notes:</b>
 * <ul>
 * <li>Access to GPS time may not be supported on all chipsets. When access to the GPS time is not
 * supported on a chipset, it will return RXN_CL_NO_CHIPSET_SUPPORT_ERR.
 * <li>Before any chipset can return a GPS time, it must have a position fix. If this function
 * is called before a position fix is completed and GPS reciever time is still unavailable,
 * RXN_CL_DATA_UNAVAILABLE will be returned.
 * </ul>
 *
 * <b>Example Usage</b>
 * \code
 * // ToDo: Initialize a CL instance for use. See RXN_CL_Initialize() example usage.
 * // Initialization will yield a "handle" for use within the RXN_CL_Restart() call below.
 *
 * // Setup a var to store reciever GPS .
 * U32 RcvrTOW;
 * U16 RcvrWeekNum;
 *
 * // Read the GPS time.
 * if(RXN_CL_GetGPSRcvrTime(handle, &RcvrWeekNum, &RcvrTOW) != RXN_SUCCESS)
 * {
 *    // ToDo: Handle error. If the error is RXN_CL_DATA_UNAVAILABLE try this call
 *    // later after a fix has been obtained.
 * }
 *
 * // ToDo: Do something meaningful with RcvrGPSTime.
 *
 * \endcode
*/ 
U16 RXN_CL_GetGPSRcvrTime(U16 handle, U16* pRcvrWeekNum, U32* pRcvrTOW);

/** 
 * \brief 
 * Get the offset between RTC time and GPS time maintained within the CL.
 *
 * \return The current offset between GPS time and the system RTC time. If the offset has
 * not been set (i.e. as a result of measuring the offset after getting a position fix
 * or because the offset is not maintained for the current chipset) a value of 
 * RXN_CL_INVALID_CLK_OFFSET will be returned.
 *
 * \details
 * <b>Description</b>\n
 * Implementations of the CL some chipsets, will maintain an offset between the system
 * RTC value and the current GPS time. This offset can be used by CL consumers and added to the
 * RTC time to get an accurate time value (even when the RTC is inaccurate due to drift or an
 * erroneous user setting).
 * 
 * <b>Notes:</b>
 * <ul>
 * <li>Typically CL consumers will get the current offset after CL initialization and whenever
 * required. Consumers will be notified of offset changes via the RXN_CL_ClkOffsetChange_CB()
 * function. This callback will support efficient notification to CL consumers of an offset change
 * so that consumers need not poll the CL to determine if any offset change has occured.
 * </ul>
 */ 
S32 RXN_CL_GetClkOffset(void);

/** 
 * \brief 
 * Get assistance.
 *
 * \param assistTypeBitMask 
 * [IN] a bitmask of assistance types. Please refert to RXN_MSL_Assistance_Type.
 * \param gpsPrnBitMask
 * [IN] gpsPrnBitMask is a bitmask where bit0 is equal to PRN1, bit1 is PRN2 ...
 * \param gloSlotBitMask
 * [IN] gloSlotBitMask is a bitmask where bit0 is equal to Slot1, bit1 is Slot2 ...
 *
 * \details
 * <b>Description</b>\n
 * Called by the CL to notify consumers that an assistance is required. This
 * function is defined by the CL and must be implemented by CL consumers.
 * 
 * <b>Notes:</b>
 * <ul>
 * <li>Regardless of whether this function is used, CL consumers must provide an
 * implementation to avoid linkage errors. "Stubbing Out" the implementation
 * will be sufficient to avoid linkage errors.
 * </ul>
 *
 * <b>Example Usage</b>
 * \code
 * // Example stub implementation: RXN_CL_GetAssistance(U08 assistTypeBitMask, U32 gpsPrnBitMask, U32 gloSlotBitMask) {return;}
 *
 * // Implement the function.
 * RXN_CL_GetAssistance(U08 assistTypeBitMask, U32 gpsPrnBitMask, U32 gloSlotBitMask)
 * {
 *    // ToDo: Inject time, position, EE or BCE .
 *  
 *    return;
 * }
 *
 * \endcode
*/ 
void RXN_CL_GetAssistance(U08 assistTypeBitMask, U32 gpsPrnBitMask, U32 gloSlotBitMask);

/** 
 * \brief 
 * Notifies of an offset change (current GPS time from the receiver - GPS time calculated from RTC).
 *
 * \param newOffset 
 * [OUT] New offset value.
 *
 * \details
 * <b>Description</b>\n
 * This callback notifies CL consumers of a change in the offset between the GPS time obtained
 * from a receiver after a fix and the GPS time calculated using the system RTC. This offset will
 * only be non-zero when the RTC is inaccurate due to drift or improper user setting.
 * 
 * <b>Notes:</b>
 * <ul>
 * <li>Access to GPS time may not be supported on all chipsets. When access to the GPS time is not
 * supported on a chipset, this callback will never be called.
 * <li>To request the current offset value within the CL, RXN_CL_GetClkOffset can be used.
 * </ul>
 *
*/
void RXN_CL_ClkOffsetChange_CB(S32 newOffset);

/** 
 * \brief 
 * Gets GPS measurement sets including pseudo range data.
 *
 * \param MeasSetElements 
 * [OUT] Measurement data sets including GPS measurement data.
 *
 * \return RXN_SUCCESS if data sets could be read successfully.
 * \return RXN_FAIL if an error occurs when retrieving data sets.
 *
 * \details
 * <b>Description</b>\n
 * This function retrieves GPS measurement sets from the receiver. These sets can be used within TRKR to
 * get position data.
 * 
 * <b>Notes:</b>
 * <ul>
 * <li>Additional return values to be determined upon implementation of the function.
 * </ul>
 *
*/
U16 RXN_CL_GetGPSMeasSet(RXN_GPSMsrSetList_t* MeasSetElements);

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
void RXN_CL_RXNToFull( RXN_ephem_t* pRXN, RXN_FullEph_t* pFull, U08 CAOrPOnL2,
                U16 iodc, U08 L2PData, S08 TGD, U08 AODO);

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
void RXN_CL_FullToRXN(RXN_FullEph_t* pFull, RXN_ephem_t* pRXN);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* RXN_CL_H */
