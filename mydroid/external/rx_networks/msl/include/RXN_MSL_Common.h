/*
 * Copyright (c) 2012 Rx Networks, Inc. All rights reserved.
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
 * $LastChangedDate: 2009-04-23 16:16:15 -0700 (Thu, 23 Apr 2009) $
 * $Revision: 10337 $
 *************************************************************************
 *
 */

/*
 * Contains common elements that are not to be exposed to CL consumers.
 */ 

#ifndef RXN_MSL_COMMON_H
#define RXN_MSL_COMMON_H

#ifdef __cplusplus
extern "C" {
#endif

#include <string.h>               /* Required for string funcs such as strlen. */
#include <stdio.h>                /* Required for std IO funcs such as sprintf. */
#include "RXN_API.h"              /* Defines the PGPS and SAGPS interfaces. */
#include "RXN_security.h"         /* For RXN_Generate_Password() and RXN_Set_Security_Key(). */
#include "RXN_license.h"          /* For RXN_Set_License_Key().*/
#include "RXN_debug.h"            /* Required for debug return codes. */

#ifdef XYBRID
#include "RXN_MSL_Xybrid.h"
#endif

#define SYSTEM_MINUTE 60
#define SYSTEM_HOUR 3600

/****************************
 * State definitions below. *
 ****************************/

#define MSL_ALL_PRNS                  0   /* Index within gMSL_States that applies to all PRNs. */

/************************************************
 * Max Internal String And Other Array Lengths. *
 ************************************************/

#define MSL_MAX_STATES                33    /* Max number of state vars (1/PRN plus 1 global). */
#define MSL_MAX_FILE_CONFIG_STR_LEN   32    /* Config file path max chars. */
#define MSL_MAX_LINE_STR_LEN          256   /* Config file line max chars. */
#define MSL_MAX_PARAM_STR_LEN         256   /* Config file param max chars. */
#define MSL_MAX_HOST_STR_LEN          256   /* Host URL max chars. */
#define MSL_MAX_PATH                  400   /* Max path to log file, key file, etc. */
#define MSL_MAX_FOLDER_NAME           50    /* Max length of folder name*/
#define MSL_MAX_FILE_NAME             50
#define MSL_MAX_FULL_FILE_PATH        MSL_MAX_PATH + MSL_MAX_FOLDER_NAME + MSL_MAX_FILE_NAME /* */
#define MSL_MAX_LOG_TIME_ELEMENT      64    /* Log entry time string max chars. */
#define MSL_MAX_VENDOR_STR_LEN        MAX_FIELD_LENGTH  /* Vendor string max chars. */
#define MSL_MAX_DEVICE_STR_LEN        MAX_FIELD_LENGTH  /* Device string max chars. */
#define MSL_MAX_MODEL_STR_LEN         MAX_FIELD_LENGTH  /* Model string max chars. */
#define MSL_SKT_REQ_MSG_MAX_LEN       512   /* PGPS request msg max chars. */
#define MSL_SKT_RESP_MSG_MAX_LEN      9216  /* PGPS response msg max chars. */
#define MSL_SKT_ADDR_MAX_LEN          64    /* Server IP or host nam max length*/
#define MSL_MAX_HOST_PORT_INDEX       4
#define MSL_MAX_CONSTEL				  2		/* Max number of constellations. This can be removed once PGPS-1330 is resolved. */

/*****************************
 * Internal MSL Error Codes. *
 ****************************/

#define MSL_FILE_OPENCLOSE_ERROR      101   /* File Open/Close error. */
#define MSL_FILE_IO_ERROR             102   /* File Read/Write error. */ 
#define MSL_FILE_EOF_ERROR            103   /* EOF reached during read or write. */
#define MSL_FILE_OVER_CAP_ERROR       104   /* Write to a log cannot be compeleted - over capacity. */
#define MSL_SKT_HOST_RESOLVE_ERROR    105   /* Error resolving an IP address from a host name. */
#define MSL_SKT_CREATE_ERROR          106   /* Error creating a socket. */
#define MSL_SKT_SETUP_ERROR           107   /* Error setting up a socket. */
#define MSL_SKT_CONNECT_ERROR         108   /* Error connecting a socket to a host. */
#define MSL_SKT_READ_ERROR            109   /* Error reading on a socket. */
#define MSL_SKT_WRITE_ERROR           110   /* Error writing on a socket. */
#define MSL_SKT_TIMEOUT_ERROR         111   /* Timeout while reading or writing. */
#define MSL_SEC_PW_GEN_ERROR          112  /* Error generating a password for seed access. */
#define MSL_STATE_INVALID_ERROR       113  /* An invalid state was somehow entered. */
#define MSL_PROP_ERROR                114  /* An invalid state was somehow entered. */
#define MSL_PGPS_RESP_PROC_ERROR      115  /* Error processing a clock or seed response. */
#define MSL_PGPS_PW_GEN_ERROR         116  /* Error gen a password for seed or clk updates. */
#define MSL_PGPS_HOST_FMT_ERROR       117  /* Error with the host string format. */
#define MSL_SAGPS_SEED_MINT_ERROR     118  /* Error with minting an SAGPS seed. */
#define MSL_PROPAGATION_COMPLETE      119  /* Propagation has completed */
#define MSL_LICENSE_KEY_EXPIRED       120  /* License key is expired */  

/**************************
 * PGPS request type IDs. *
 **************************/

#define MSL_REQ_TYPE_SEED             1
#define MSL_REQ_TYPE_CLOCK            2

/************************************
 * Config value min and max limits. *
 ************************************/

#define MSL_EPHEMERIS_DURATION_MIN      0
#define MSL_EPHEMERIS_DURATION_MAX      14400
#define MSL_CHIPSET_EXPIRY_MIN          3600
#define MSL_CHIPSET_EXPIRY_MAX          14400
#define MSL_TOE_BUFFER_OFFSET_MIN       -14400
#define MSL_TOE_BUFFER_OFFSET_MAX       14400
#define MSL_PROPAGATE_FORWARD_MIN       14400
#define MSL_PROPAGATE_FORWARD_MAX       1209600
#define MSL_URE_THREASHOLD_MIN          20
#define MSL_URE_THREASHOLD_MAX          200
#define MSL_SEED_CHECK_MIN              60
#define MSL_SEED_CHECK_MAX              14400
#define MSL_SEED_UPDATE_AGE_MIN         86400
#define MSL_SEED_UPDATE_AGE_MAX         1209600
#define MSL_SEED_UPDATE_AGE_OFFSET_MIN  0
#define MSL_SEED_UPDATE_AGE_OFFSET_MAX  86400
#define MSL_SEED_UPDATE_RETRY_MAX	    5
#define MSL_SEED_AGE_MIN	            0
#define MSL_SEED_AGE_MAX	            14
#define MSL_SAGPS_SEGMENTS_MIN          -1
#define MSL_SAGPS_SEGMENTS_MAX          2
#define MSL_CLOCK_UPDATE_AGE_MIN        86400
#define MSL_CLOCK_UPDATE_AGE_MAX        604800
#define MSL_CLOCK_UPDATE_AGE_OFFSET_MIN 0
#define MSL_CLOCK_UPDATE_AGE_OFFSET_MAX 86400

#define MSL_RETRY_PERIOD_MIN            0
#define MSL_RETRY_PERIOD_MAX            604800
#define MSL_RETRY_TIMER_MIN             86400
#define MSL_RETRY_TIMER_MAX             604800

#define MSL_SEED_GPSTIME_UNCERT_MAX   7200
#define MSL_SNTP_GPSTIME_UNCERT_MAX   7200
#define MSL_SNTP_RETRY_MAX			  5

/************************************
 * MSL Constants. *
 ************************************/

#define MSL_INVALID_CLK_OFFSET        0x0
#define MSL_INVALID_GPS_TIME          0x0
#define MSL_INVALID_GPS_TIME_UNCERTAINTY   604800000

#define MSL_GPSTIME_VALID_DURATION_SECOND  24 * SYSTEM_HOUR /* a day */
#define MSL_SNTPTIME_VALID_DURATION_SECOND 24 * SYSTEM_HOUR /* a day; SNTP sync at least once per day */
#define MSL_GPS_WEEK_BUILD_TIME            670
#define MSL_SNTP_REQUEST_INTERVAL          60           /* try SNTP once per minutes until SNTP time is received */
/* Number of seconds between Jan 1, 1900 and Jan 1, 1970. ((365 * 70) + 17) * 24 * 60 * 60 = 2208988800 = 0x83aa7e80 */
#define MSL_OFFSET_1900_TO_1970 0x83aa7e80
/* Number of seconds between Jan 1, 1970. and Jan 1 1996 minus 3 hrs*/
#define GPS_GLO_OFFSET 504478800
/* Seconds between Jan 1 1970 & GPS Jan 6 1980 */
#define SYSTEM_GPS_OFFSET 315964800

/************************************
 * MSL Configuration Flags.         *
 ************************************/

/* Generic flags */
enum
{
    MSL_CONFIG_FLAG_USE_SYSTEM_TIME = 1 << 0,
    MSL_CONFIG_FLAG_RESERVED2 = 1 << 1,
    MSL_CONFIG_FLAG_RESERVED3 = 1 << 2,
    MSL_CONFIG_FLAG_RESERVED4 = 1 << 3,
    /* ... */
    MSL_CONFIG_FLAG_MAX = 1 << 31
};

/* GPS flags */
enum
{
    MSL_CONFIG_FLAG_GPS_RESERVED1 = 1 << 0,
    MSL_CONFIG_FLAG_GPS_RESERVED2 = 1 << 1,
    MSL_CONFIG_FLAG_GPS_RESERVED3 = 1 << 2,
    MSL_CONFIG_FLAG_GPS_RESERVED4 = 1 << 3,
    /* ... */
    MSL_CONFIG_FLAG_GPS_MAX = 1 << 31
};

/* GLONASS flags */
enum
{
    MSL_CONFIG_FLAG_GLO_ENABLE_EN           = 1 << 0,
    MSL_CONFIG_FLAG_GLO_USE_BOUNDED_TIME    = 1 << 1,
    MSL_CONFIG_FLAG_GLO_RESERVED3           = 1 << 2,
    MSL_CONFIG_FLAG_GLO_RESERVED4           = 1 << 3,
    /* ... */
    MSL_CONFIG_FLAG_GLO_MAX = 1 << 31
};


/****************
 * MSL Includes *
 ****************/
#include "RXN_MSL_Options.h" /* Compile options */
#include "RXN_MSL.h"         /* Defines the MSL interface. */
#include "RXN_MSL_SM.h"      /* State Machine */
#include "RXN_MSL_PGPS.h"    /* Functions supporting PGPS. */
#include "RXN_MSL_Log.h"     /* Logging functions */

/****************************************
 * Platform - specific includes follow. *
 ****************************************/

#include "RXN_MSL_Platform.h"

enum MSL_Data_Access 
{
    MSL_DATA_ACCESS_DISABLED = 0,    
    MSL_DATA_ACCESS_ENABLED,    
    MSL_DATA_ACCESS_UNDEFINED
};

/******************************
 * File resources (e.g. handles) must be stored within O/S specific implementations so that 
 * common code can open files, read, write, etc. To support simultaneous access to multiple files
 * (e.g. log and key file) separate resources are stored for each type. Within types, multiple
 * resources are used (i.e. file store) to ensure that file access is thread safe.*/  
enum MSL_FileResourceIDs
{
	MSL_LOG_FILE_RSRC_ID = RXN_MSL_CS_POLY_FILE_LAST + 1,
	//MSL_EPH_STORE_FILE_RSRC_ID,
	//MSL_SEED_STORE_FILE_RSRC_ID,
	//MSL_POLY_STORE_FILE_RSRC_ID,
	MSL_CONFIG_FILE_RSRC_ID,
	MSL_KEY_FILE_RSRC_ID,
	MSL_PGPS_SEED_FILE_RSRC_ID,
	MSL_EOL_RSRC_ID,
	MSL_FILE_RSRC_MAX
};

/****************************
 * MSL Config Structures.   *
 ****************************/

typedef struct MSL_Config_CL
{
  /* CL Log file path store. */
  char configStr[RXN_MSL_CONFIG_MAX_STR_LEN];
  /* CL config string store. */
  char logPath[MSL_MAX_PATH];
} MSL_Config_CL_t;

typedef struct MSL_Config_Net
{
  /* Host port list */
  U16 hostPortList[MSL_MAX_HOST_PORT_INDEX];

  /* Host port index */
  U08 hostPortIndex;

  /* Host name */
  char host[MSL_MAX_HOST_STR_LEN];

  /* Host max index. */
  U08 hostMaxIdx;

  /* If set to FALSE MSL will blindly try to download seed, if set to TRUE MSL 
   * will smartly try only when data connectivity and roaming settings permit 
   * data transfer. */
  BOOL respect_Data_Settings;

  /* How frequent we hit the seed server after a failed initial attempt (in seconds)*/
  U32 downloadRetryPeriod; 
  /* Duration until next set of retries */
  U32 downloadRetryTimer;
  /* Maximum number of failed retries */
  U08 downloadRetryMax;
} MSL_Config_Net_t;

typedef struct MSL_Config_Prop
{
  /* How far to propagate forward in seconds. */
  U32 PGPSFwd;
  /* How far to propagate forward SAGPS seeds in seconds. Max 3 days. Default 3 days */
  U32 SAGPSFwd;
} MSL_Config_Prop_t;

typedef struct MSL_Config_Security
{
  /* Vendor ID store. */
  char vendorId[MSL_MAX_VENDOR_STR_LEN];
  /* Model ID store. */
  char modelId[MSL_MAX_MODEL_STR_LEN];
  /* License key path store. */
  char licKeyPath[MSL_MAX_PATH];
  /* Security key path store. */
  char secKeyPath[MSL_MAX_PATH];
} MSL_Config_Security_t;

typedef struct MSL_Config_Seed
{
  /* How old a seed should be before its clock is updated (in seconds). */
  U32 clockUpdateAge;
  /* How Maximum offset used by a random function to added to the 
   * Clock_Update_Age for server access load balancing.*/
  U32 clockUpdateAgeOffset; 
  /* How old a seed should be before it is replaced (in seconds). */
  U32 updateAge;
  /* How Maximum offset used by a random function to added to the 
   * Seed_Update_Age for server access load balancing.*/
  U32 updateAgeOffset;
} MSL_Config_Seed_t;

typedef struct MSL_Config_SNTP
{
  /* SNTP Host URL string. */
  char host[MSL_MAX_HOST_STR_LEN];
  /* SNTP Host port. */
  U16 port;
  /* How frequent we hit the SNTP server after a failed initial attempt (in seconds)*/
  U32 requestRetryPeriod;
  /* Duration until next set of retries */
  U32 requestRetryTimer;
  /* Maximum number of failed retries */
  U08 requestRetryMax;
} MSL_Config_SNTP_t;

typedef struct MSL_Config_TOE
{
  /* Ephemeris duration as specified by the TOE. */
  U16 ephDur;
  /* Chipset ephemeris expiration period. */
  U16 chipExp;
  /* TOE buffer offset (how far seed TOE times will be rolled back). */
  S16 bufOffset;
} MSL_Config_TOE_t;

typedef struct MSL_Config_Constel
{
	MSL_Config_Prop_t prop;
	MSL_Config_Seed_t seed;
    U16 AssistanceThreshold;
    U32 flags;
} MSL_Config_Constel_t;

#ifdef XYBRID
typedef struct MSL_Config_XYBRID
{
    /* Xybrid host name */
    char host[MSL_MAX_HOST_STR_LEN];
    /* Xybrid port */
    U16 hostPort;
    /* The maximum number that will be used to replace a '%' in the host name
       for load balancing. See MSL_CreateHostName(). */
    U08 hostMaxIdx;
    /* Xybrid vendor id */
    char vendorId[MSL_MAX_VENDOR_STR_LEN];
    /* Xybrid vendor salt */
    char vendorSalt[MSL_MAX_VENDOR_STR_LEN];
    /* Xybrid flags */
    U32 flags;
} MSL_Config_XYBRID_t;
#endif

/* NB: If you change this struct, you must also change the initialization 
 * in RXN_MSL_Common.c */
typedef struct MSL_Config
{
  /* Base GPS week used to determine GPS week roll over (%1024) */
  U16 baseGPSWeekNum;
  /* Time to wait during start up before making seed or SNTP request */
  U32 startupDataWaitDuration; 
  /* How frequently (in seconds) to check seeds for required updates. */
  U32 systemCheckFreq;
  /* Version of the config file. */
  char version[MSL_MAX_FILE_CONFIG_STR_LEN];
  /* Pantry file path store. */
  char pantryPath[MSL_MAX_PATH];
  /* Age of seed to download (in days) */
  U08 seedAge;
  /* Number of SAGPS segments used */
  S08 sagpsSegments;
  /* The list of constellations to process, in priority order */
  RXN_constel_t constelList[MSL_MAX_CONSTEL]; 
  /* The number of constellations configured */
  U08 numConstels;

  MSL_Config_CL_t CL;
  MSL_Config_Net_t net;   
  MSL_Config_Security_t sec;
  MSL_Config_Constel_t constel[MSL_MAX_CONSTEL];
  MSL_Config_SNTP_t SNTP;
  MSL_Config_TOE_t TOE;
  U32 flags;
#ifdef XYBRID
  MSL_Config_XYBRID_t xybrid;
#endif
} MSL_Config_t;

static const U08 numOfPRNs[MSL_MAX_CONSTEL] =
{
    RXN_CONSTANT_NUM_GPS_PRNS,
    RXN_CONSTANT_NUM_GLONASS_PRNS
};

/*************************
 * Abort Functions Below *
 *************************/

/* Check the internal abort flag. */
BOOL MSL_CheckAbort(void);

/* Set the internal abort flag. */
void MSL_SetAbort(BOOL bAbort);

/***************************
 * Utility Functions Below *
 ***************************/

/* Read and process config file contents. */
U16 MSL_ProcessConfig(void);

/* Read a delimited parameter from within the config file. */
U16 MSL_ReadParam(CHAR* paramLabel, double* paramData, CHAR paramStr[MSL_MAX_PARAM_STR_LEN]);

/* Convert data within a RXN_ephem_t struct to a RXN_FullEph_t.
 * Additional arguments are RXN_FullEph_t elements not found
 * within RXN_ephem_t. */
void MSL_GPS_RXNToFull( RXN_ephem_t* pRXN, RXN_FullEph_t* pFull, U08 CAOrPOnL2,
                U16 iodc, U08 L2PData, S08 TGD, U08 AODO);

/* Convert data within a RXN_FullEph_t struct to a RXN_ephem_t.*/
void MSL_GPS_FullToRXN(RXN_FullEph_t* pFull, RXN_ephem_t* pRXN);

void MSL_GLO_RXNToFull(RXN_glonass_ephem_t* pRXN, RXN_FullEph_GLO_t* pFull);
void MSL_GLO_FullToRXN(RXN_FullEph_GLO_t* pFull,RXN_glonass_ephem_t* pRXN);

/* This function fills the right structure depending on the constellation. */
void MSL_RXNToFull(RXN_Ephemeris_u* pEphemeris, RXN_MSL_NavDataList_t* pNavModelData, U08 ephCount, RXN_constel_t constel);

/* This function fills the right structure depending on the constellation. */
void MSL_FullToRXN(RXN_FullEphem_u PRNArr, U08 Idx, RXN_Ephemeris_u* pEphemeris, RXN_constel_t constel);

/* Set the host port to the next port in the list.  This is used when the
 * connection to the host has failed and it is time to failover to the next
 * port in the list. */
U16 MSL_SetNextHostPort();

/* Gets the current host port */
U16 MSL_GetHostPort();


/**
 * \brief
 * Print the MSL configurable parameters in MSL_Log.txt.
 *
 *
 * \details
 * <b>Description</b>\n
 * Regardless of if a parameter is missing from the MSLConfig and
 * therefore used hardcode default parameters, the MSL_Log.txt should
 * capture the MSL configurable parameters to allow for better debugging.
*/ 
void MSL_PrintConfig(void);

/**
  * \brief
  * Creates a load balanced host name string.
  *
  * \details
  * <b>Description</b>\n
  * Converts a hostname of the form "hostname%.domain" to one
  * containing a randomly generated number (eg "hostname2.domain").
  * 
 */
void MSL_CreateHostName(
        char hostName[MSL_MAX_HOST_STR_LEN], 
        const char format[MSL_MAX_HOST_STR_LEN],
        U08 maxIndex);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* RXN_MSL_COMMON_H */
