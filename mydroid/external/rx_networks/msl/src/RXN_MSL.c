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
* $LastChangedDate: 2009-04-22 17:33:16 -0700 (Wed, 22 Apr 2009) $
* $Revision: 10292 $
*************************************************************************
*
* This file contains implementation of common APIs exposed within the RXN_MSL.h file.
* 
*/
#include "RXN_MSL_Common.h"  /* Internal common declarations. */
#include "RXN_MSL_MsgThread.h"
#include "RXN_MSL_Time.h"
#include "RXN_MSL_Rinex.h"
#include "RXN_MSL_Observer.h"

#ifdef XYBRID
#include "RXN_MSL_Xybrid.h"
#endif

/* Include abstraction initialisation definitions. */
#include "RXN_API_calls_extra.h"

#ifndef BUILD_MSL_VERSION
/* "INTERNAL" version used for development only and should not be released to the customer */
#define BUILD_MSL_VERSION "GPStream MSL 7.1.0"
#endif

/* Time in seconds before a XYBRID request times out and retries are permitted */
#define XYBRID_TIMEOUT 30

/* Version string returned from RXN_MSL_Get_API_Version(). */
static const char mMSL_API_VersionString[RXN_CONSTANT_VERSION_STRING_LENGTH] = BUILD_MSL_VERSION;

static U08 cache[MSL_PGPS_CACHE_SIZE];   /* Cache storage. */

static RXN_MSL_Observer_t* mLocationObservers = NULL;
static RXN_MSL_Observer_t* mBCEObservers = NULL;
static RXN_MSL_Observer_t* mEEObservers = NULL;
static RXN_MSL_Observer_t* mFinishedAssistanceObservers = NULL;

extern MSL_Config_t gConfig;
extern U32 MSL_GetAssistanceMask();

static void LogConstellationEE(const U08 prn, RXN_Ephemeris_u* pEphemeris, RXN_constel_t constel);
static U16 CheckThreshold(const U08 prn, RXN_Ephemeris_u* pEphemeris, RXN_constel_t constel);
static void LogEphemeris(RXN_Ephemeris_u* pEphemeris, RXN_constel_t constel);
static U16 HasEphemeris(RXN_FullEphem_u PRNArr, U08 Idx, RXN_constel_t constel);
static U16 GetEphemeris(const U08 prn, RXN_Ephemeris_u* pEphemeris, RXN_constel_t constel);
static U16 SetEphemeris(RXN_Ephemeris_u* pEphemeris, RXN_constel_t constel);
static void CheckAssistanceDone();

void MSL_SendEEToObservers(RXN_MSL_NavDataList_t* ephemeris, RXN_constel_t constel);

/* Conversion from FT index to metres according to GLONASS ICD */
static float mFT_in_m[] = {1, 2, 2.5, 4, 5, 7, 10, 12, 14, 16, 32, 64, 128, 256, 512};


U16 RXN_MSL_Get_API_Version(char version[RXN_CONSTANT_VERSION_STRING_LENGTH])
{
    memcpy(&version[0], mMSL_API_VersionString, RXN_CONSTANT_VERSION_STRING_LENGTH);
    /* Terminate a string if it is too long */
    version[RXN_CONSTANT_VERSION_STRING_LENGTH - 1] = '\0';
    return RXN_SUCCESS;
}

U16 RXN_MSL_Log_Init()
{
    return MSL_Log_Init();
}

void RXN_MSL_Log_UnInit(void)
{
    MSL_Log_UnInit();
}

U16 RXN_MSL_Load_Config(const char configPath[RXN_MSL_MAX_CONFIG_PATH])
{
    U16 result = RXN_FAIL;                                      /* Result store. */

    /* Open the config file. */
    if(MSL_OpenFile(MSL_CONFIG_FILE_RSRC_ID, configPath, "r") != RXN_SUCCESS)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE03, "RXN_MSL_Initialize: Error opening configuration file.");
        return RXN_MSL_CONFIG_FORMAT_ERR;
    }

    /* Set globals using config file contents. */
    result = MSL_ProcessConfig();

    if(result != RXN_SUCCESS)
    {
        /* Close the config file. */
        MSL_CloseFile(MSL_CONFIG_FILE_RSRC_ID);

        /* Trace Error. */
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE03, "RXN_MSL_Initialize: MSL_ProcessConfig failed due to err %1.0f", (double) result);
        return result;
    }

    /* Close the config file. */
    if(MSL_CloseFile(MSL_CONFIG_FILE_RSRC_ID) != RXN_SUCCESS)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE03, "RXN_MSL_Initialize: Could not close config file.");
        return RXN_MSL_FILE_OPEN_OR_CLOSE_ERR;
    }
    /*Initialize Logging */
    RXN_MSL_Log_Init();

    return result;
}

U16 RXN_MSL_Get_CL_Config_Str(char clConfigStr[RXN_MSL_CONFIG_MAX_STR_LEN])
{
    snprintf(clConfigStr, RXN_MSL_CONFIG_MAX_STR_LEN, "%s", gConfig.CL.configStr);
    return RXN_SUCCESS;
}

U16 RXN_MSL_Initialize(void)
{
    U16 result = RXN_FAIL;                                      /* Result store. */
    CHAR ver[RXN_CONSTANT_VERSION_STRING_LENGTH];               /* Ver string store. */
    CHAR zoneBitmask[8];                                        /* For print of the logging debug zone bitmask. */
    CHAR key[RXN_MSL_KEY_LENGTH];                       /* Security key store. */
    CHAR expDate[RXN_CONSTANT_MAX_LICENSE_EXPIRY_DATE_LENGTH];  /* Licesne expire date string store */
    RXN_config_t config;
    CHAR eolFile[MSL_MAX_FULL_FILE_PATH];

    strncpy(eolFile, gConfig.pantryPath, MSL_MAX_PATH);
    strncat(eolFile, "/EOL.eol", MSL_MAX_FOLDER_NAME);

    /* Check if EOL file exists.*/
    if(MSL_OpenFile(MSL_EOL_RSRC_ID, eolFile, "r") == RXN_SUCCESS)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE03, "RXN_MSL_Initialize: EOL file found. PGPS server has been decommissioned.");
        MSL_CloseFile(MSL_EOL_RSRC_ID);
        return RXN_MSL_EOL_ERR;
    }
    /* Clear key value */
    memset((void*) key, 0, RXN_MSL_KEY_LENGTH);

    /* Open the license key file. */
    if(MSL_OpenFile(MSL_KEY_FILE_RSRC_ID, gConfig.sec.licKeyPath, "r") != RXN_SUCCESS)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE03, "RXN_MSL_Initialize: Error opening license key file.");
        return RXN_MSL_INIT_ERR;
    }

    /* Read license key file data. Note that we will likely hit the EOF before getting all requested bytes.*/
    result = MSL_ReadFileBytes(MSL_KEY_FILE_RSRC_ID, (U08*)key, (U16)RXN_MSL_KEY_LENGTH);
    if((result != RXN_SUCCESS) && (result != MSL_FILE_EOF_ERROR))
    {
        /* Close the license key file. */
        MSL_CloseFile(MSL_KEY_FILE_RSRC_ID);

        /* Trace Log. */
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE03, "RXN_MSL_Initialize: Error reading license key file.");
        return RXN_MSL_INIT_ERR;
    }

    /* Close the license key file. */
    MSL_CloseFile(MSL_KEY_FILE_RSRC_ID);

    /* Set the license key so that subsequent RXN_GetEphemeris calls will succeed.
    * Note that the vendor ID used within this call will also be used to get 
    * seed and clock updates from the PGPS server (for PGPS compiles of the MSL). */

    if(RXN_Set_License_Key(key, (U32) strlen(key), gConfig.sec.vendorId) != RXN_SUCCESS)
    {
        MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE03,

            "RXN_MSL_Initialize: Error processing license key file data for vendor id: %s. Will not be able to get subsequent assistance.", gConfig.sec.vendorId);
        return RXN_MSL_INIT_ERR;
    }
    else
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE03,
            "RXN_MSL_Initialize: License key SUCESSFULLY validated.");
    }
    /* Process security.key file data. */

    /* Clear key data. */
    memset((void*) &key, 0, RXN_MSL_KEY_LENGTH);

    /* Open the security key file. */
    if(MSL_OpenFile(MSL_KEY_FILE_RSRC_ID, gConfig.sec.secKeyPath, "r") != RXN_SUCCESS)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE03, "RXN_MSL_Initialize: Error opening security key file.");
        return RXN_MSL_INIT_ERR;
    }

    /* Read security key file data. Note that we will likely hit the EOF before getting all requested bytes.*/
    result = MSL_ReadFileBytes(MSL_KEY_FILE_RSRC_ID, (U08*)key, (U16)RXN_MSL_KEY_LENGTH);
    if((result != RXN_SUCCESS) && (result != MSL_FILE_EOF_ERROR))
    {
        /* Close the security key file. */
        MSL_CloseFile(MSL_KEY_FILE_RSRC_ID);

        /* Trace Log. */
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE03, "RXN_MSL_Initialize: Error reading security key file.");
        return RXN_MSL_INIT_ERR;
    }

    /* Close the security key file. */
    MSL_CloseFile(MSL_KEY_FILE_RSRC_ID);

    /* Set the security key so that subsequent RXN_GeneratePassword calls will succeed.
    * Note that the vendor ID used within this call will also be used to get 
    * seed and clock updates from the PGPS server (for PGPS compiles of the MSL). */
    if(RXN_Set_Security_Key(key, (U32) strlen(key), gConfig.sec.vendorId) != RXN_SUCCESS)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE03,
            "RXN_MSL_Initialize: Error processing security key file data. Will not be able to gen passwords.");
        return RXN_MSL_INIT_ERR;  
    }
    else
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE03,
            "RXN_MSL_Initialize: Security key SUCESSFULLY validated.");
    }

    /* Initialize the O/S abstraction layer. */
    if(RXN_Init_Abstraction(gConfig.pantryPath) != RXN_SUCCESS)
    {
        return RXN_MSL_INIT_ERR;
    }

    /* Initialize critical sections for blocking. */
    MSL_InitBlocks();

    /* Log MSL version data. */
    MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE03,
        "RXN_MSL_Initialize: MSL API Ver: %s", (char*) mMSL_API_VersionString);
    MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE03,
        "RXN_MSL_Initialize: MSL Config File Ver: %s", gConfig.version);

    /* Log Mobile Suite assistance API type and version. */
    /* Get and log the PGPS API version. */
    RXN_Get_API_Version(ver);
    MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE03, "RXN_MSL_Initialize: PGPS API Ver: %s", ver);

    /* Log propagator and generator versions for appropriate assistance libs. */
    RXN_Get_Propagator_Version(ver);
    MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE03, "RXN_MSL_Initialize: Propagator Ver: %s", ver);
    RXN_Get_Generator_Version(ver);
    MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE03, "RXN_MSL_Initialize: Generator Ver: %s", ver);

    /* Check the license validity. Use a clock offset if set. */
    result = RXN_Is_License_Valid(RXN_MSL_GetBestGPSTime());

    /* Log license validity. */
    if(result != RXN_SUCCESS)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_WARNING, RXN_LOG_ZONE03,
            "RXN_MSL_Initialize: RXN_Is_License_Valid() call is failing. May be due to incorrect clock or clock offset");
    }
    else
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE03,
            "RXN_MSL_Initialize: RXN_Is_License_Valid() call passed.");

        /* Continue to get the license expiry date and log. */
        RXN_Get_License_Expiry_Date(expDate);
        MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE03,
            "RXN_MSL_Initialize: License expire date string: %s", expDate);
    }

    /* Initialize the RXN library to stipulate that extended ephemeris times out after
    * gTOE_Eph_Dur_Min where gConfig.TOE.chipExp defines the length of time that EE is valid
    * on a chipset as standard. [e.g. for a chipset with expire time of 7200 sec
    * (gConfig.TOE.chipExp), roll back the TOE within EE by 7200 (gMSL_ChipExp) - 300 (gConfig.TOE.ephDur)
    * or 6900 seconds (1 hr - 55 sec).]*/
    //RXN_Config_TOE(gConfig.TOE.ephDur, gConfig.TOE.chipExp);

    RXN_Get_Configuration(&config);
    /* time offset [sec] between prediction fuffer and last toe [default 0 sec] */
    config.toe_buffer_offset = gConfig.TOE.bufOffset;
    /* nominal chipset ephemeris expiration after toe */
    config.chip_expiry_from_TOE = gConfig.TOE.chipExp;
    /* duration of predicted Keplarian ephemeris */
    config.ephemeris_duration = gConfig.TOE.ephDur;
    /* 0 no logging, 1 error, 2 info, 3 debug, 4 full dump */
    config.logging_level = 0;	//Todo: change to configurable if needed
    /* Set the number of SAGPS segments used */
    config.sagps_orbit_segment_mask_gps = (gConfig.sagpsSegments == -1) ? DEFAULT_SEGMENTS : gConfig.sagpsSegments;
    config.sagps_orbit_segment_mask_glo = (gConfig.sagpsSegments == -1) ? DEFAULT_SEGMENTS : gConfig.sagpsSegments;

    RXN_Set_Configuration(&config);

    /* Log the init. */
    MSL_LogFlt( TRUE, TRUE,  RXN_LOG_SEV_INFO, RXN_LOG_ZONE03,
        "RXN_MSL_Initialize: Will log for severity levels 0 - %1.0f", (double) MSL_LogGetSevThreshold());
    snprintf(zoneBitmask, sizeof(zoneBitmask), "0x%4X", MSL_LogGetZoneMask());
    MSL_LogStr( TRUE, TRUE,  RXN_LOG_SEV_INFO, RXN_LOG_ZONE03,
        "RXN_MSL_Initialize: Will log using debug zone bitmask %s", zoneBitmask);

    MSL_StartMessageThread();

    /* Initialize cache. Just need to initialize cache once when MSL_Initialize */
    RXN_Initialize_Cache(&cache[0], MSL_PGPS_CACHE_SIZE);
    MSL_SM_Initialize();
    MSL_PrintConfig();

#ifdef XYBRID
    MSL_Xybrid_Initialize();
#endif

    return RXN_SUCCESS;
}

U16 RXN_MSL_Uninitialize(void)
{
    U16 result = RXN_SUCCESS;  /* Result store. */

    /* Free cache mem. */
    RXN_Uninitialize_Cache();

    MSL_SM_Uninitialize();

    /* Abort any ephemeris generation that may be underway. */
    MSL_SetAbort(TRUE);

    /* Un-init the thread protection */
    MSL_UninitBlocks();

    /* Un-Init the O/S abstraction to free resources. */
    RXN_Uninit_Abstraction();

#ifdef XYBRID
    /* Un-init Xybrid */
    MSL_Xybrid_Uninitialize();
#endif

    /* Shutdown the MSL logger. */
    RXN_MSL_Log_UnInit();
    return result;  
}

U16 RXN_MSL_Work(void)
{
    U16 result = RXN_FAIL;  /* Result store. */

    /* Get PGPS work done. Work done will depend on the current state */
    result = MSL_PGPS_Work();

    return result;
}

U16 RXN_MSL_Abort(BOOL bAbort)
{
    /* Call the internal MSL_SetAbort(). */
    MSL_SetAbort(bAbort);

    return RXN_SUCCESS;
}

U16 RXN_MSL_GetAssistance(U32 prnBitMask, RXN_MSL_Assistance_t* pAssist, RXN_constel_t constel)
{
    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
        "RXN_MSL_GetAssistance: WARNING - This function is deprecated and "
        "will be removed in a future release.");

    if (pAssist == NULL)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
            "RXN_MSL_GetAssistance: No assistance structure provided.");
        return RXN_FAIL;
    }

    return RXN_MSL_GetEphemeris(prnBitMask, pAssist->pNavModelData, constel);
}

U16 RXN_MSL_GetEphemeris(U32 prnBitMask, RXN_MSL_NavDataList_t* pNavModelData, RXN_constel_t constel)
{
    U32 currentGPSTime = 0;
    U08 PRN = 0;
    RXN_Ephemeris_u tempEphemeris;
    U08 ephCount = 0;
    U16 result = RXN_SUCCESS;  /* Result store. */ 
    enum MSL_Current_Time_Source currentTimeSource = MSL_CURRENT_TIME_SOURCE_NO;

    MSL_SM_ResetCounters();

    /* Get the current GPS time from the system. */
    currentGPSTime = MSL_GetBestGPSTime(&currentTimeSource);
    
    if (( currentTimeSource != MSL_CURRENT_TIME_SOURCE_GPS ) &&
        ( currentTimeSource != MSL_CURRENT_TIME_SOURCE_SNTP ))
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
                "RXN_MSL_GetEphemeris: Both receiver and SNTP not acceptable.");
        if((gConfig.flags & MSL_CONFIG_FLAG_USE_SYSTEM_TIME) == 1)
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
                "RXN_MSL_GetEphemeris: Using device system time as fallback time source.");
        }
        else
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
                "RXN_MSL_GetEphemeris: No Assistance provided due to inaccurate clock.");
            MSL_SetNextSystemEvent(MSL_EVENT_SYSTEM_CHECK);
            return RXN_FAIL;
        }
    }
    MSL_LogGPSTime("RXN_MSL_GetEphemeris: Getting EE for GPS time:", currentGPSTime);

#ifdef RXN_CONFIG_INCLUDE_GLONASS
    if((constel == RXN_GLO_CONSTEL)&& MSL_UseBoundedGLOSec())
    {
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
            "RXN_MSL_GetEphemeris: Getting bounded GLONASS EE at GLONASS Tb: %1.0f.", MSL_ConvertGLOSecToBoundedGLOSec(MSL_ConvertGPSTimeToGLOTime(currentGPSTime)) % 86400 /900);
    }
#endif

    /* Cancel and don't allow any propagation */
    MSL_SetAbort(TRUE);

    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
        "RXN_MSL_GetEphemeris: *** Providing EE Assistance for Constel: %1.0f ***", (float)constel);

    /* Loop through PRNs getting ephemeris within RXN_ephem_t
    * structs and converting these to RXN_FullEph_t structs
    * when ephemeris is available. */
    for(PRN=1; PRN<=numOfPRNs[constel]; PRN++)
    {
        /* prnBitMask is a bitmask where bit0 is equal to PRN1, bit1 is PRN2 ... */
        if((prnBitMask&(1<<(PRN-1))) == 0)
        {
            continue;
        }

        /* Clear tempEphemeris */
        memset((void*) &tempEphemeris, 0, sizeof(RXN_Ephemeris_u));

        /* Get ephemeris from the RxN lib. */      
        if((result = GetEphemeris(PRN, &tempEphemeris, constel)) != RXN_SUCCESS)
        {

            /* If there is no seed available or the seed isn't propagated far enough, 
             * try to propagate ahead 4 hours and try again */
            if (result == RXN_DEBUG_CODE_ERROR_NO_SEED_AVAILABLE
                || result == RXN_DEBUG_CODE_ERROR_SEED_CURRENT_TIME_LESS_THAN_TARGET_TIME) 
            {
                U32 targetTime = RXN_MSL_GetBestGPSTime() + 4 * SYSTEM_HOUR;

                MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_WARNING, RXN_LOG_ZONE08,
                    "RXN_MSL_GetEphemeris: No assistance available for PRN %1.0f.  Attempting to generate assistance.",
                    (double)PRN);

                if (RXN_Generate_Assistance(PRN, targetTime, constel) == RXN_SUCCESS)
                {
                    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08,
                            "RXN_MSL_GetEphemeris: Assistance generated. Getting EE.");

                    result = GetEphemeris(PRN, &tempEphemeris, constel);
                }
            }

            if(result == RXN_DEBUG_CODE_ERROR_EVALUATION_LICENSE_EXPIRED)
            {
                MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
                    "RXN_MSL_GetEphemeris: License expired!");
            }

            /* If the second attempt at getting assistance failed, try the next PRN */
            if (result != RXN_SUCCESS)
            {
                /*Std TRACE output. */
                MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
                    "RXN_MSL_GetEphemeris: No EE available for PRN: %1.0f. Debug Code: %1.0f",
                    (double) PRN, (double) result);

                /* Try the next PRN. */
                continue;
            }
        }      

        if(CheckThreshold(PRN, &tempEphemeris, constel) != RXN_SUCCESS)
        {
            /* Try the next PRN. */
            continue;
        }

        MSL_RXNToFull(&tempEphemeris, pNavModelData, ephCount++, constel);

        /* TRACE output. */
        LogConstellationEE(PRN, &tempEphemeris, constel);
    }
    /* Allow propagation */
    MSL_SetAbort(FALSE);

    /* Set the number of entries within pAssist->pNavModelData. */
    pNavModelData->numEntries = ephCount;
    return RXN_SUCCESS;
}

U16 RXN_MSL_WriteSingleEphemeris(RXN_Ephemeris_u* eph, RXN_constel_t constel)
{
    U16 result = RXN_SUCCESS;

    /* Set the ephemeris within the RxN library. */
    if((result = SetEphemeris(eph, constel)) != RXN_SUCCESS)
    {
        MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
            "RXN_MSL_WriteEphemeris: Error setting ephemeris within SAGPS for Constel: %1.0f. Error: %1.0f.",
            (double)constel, (double) result);

        return RXN_FAIL;                  
    }
    switch(constel)
    {
    case RXN_GPS_CONSTEL:
        {
            MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
                "RXN_MSL_WriteEphemeris: GPS BCE change (TOE %1.0f) for PRN: %1.0f...",
                (double) eph->gpsEphemeris.toe, (double) eph->gpsEphemeris.prn);

        }
        break;

    case RXN_GLO_CONSTEL:
        {
            MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
                "RXN_MSL_WriteEphemeris: GLONASS BCE change (gloSec %1.0f) for PRN: %1.0f...",
                (double) eph->gloEphemeris.gloSec, (double) eph->gloEphemeris.slot);

        }
        break;
    } 
    LogEphemeris(eph, constel);
    MSL_SetNextSystemEvent(MSL_EVENT_WRITE_EPHEMERIS);

    return RXN_SUCCESS;
}

U16 RXN_MSL_WriteEphemeris(RXN_FullEphem_u PRNArr, U08 ArrSize, RXN_constel_t constel)
{
    U16 ret = RXN_SUCCESS;
    U08 Idx = 0;
    RXN_Ephemeris_u tempEphemeris;

    for(Idx=0; Idx<ArrSize; Idx++)
    {
        if(HasEphemeris(PRNArr, Idx, constel) != RXN_SUCCESS)
        {
            continue;
        }

        MSL_FullToRXN(PRNArr, Idx, &tempEphemeris, constel);

        if (RXN_MSL_WriteSingleEphemeris(&tempEphemeris, constel) != RXN_SUCCESS)
        {
            ret = RXN_MSL_SET_EPHEMERIS_ERR;
        }
    }

    return ret;
}

/******************
* Time Functions *
******************/
/*!
* \attention Integrators have the responsibility to provide accurate time to the system.
*/
U16 RXN_MSL_SetGPSTime(RXN_RefTime_t* pRefTime)
{
    /* set the accurate GPS Time (in second) */
    U16 gps_wn = pRefTime->weekNum;

    if (gps_wn < 1024)
    {
        if (gps_wn >= gConfig.baseGPSWeekNum)
        {
            /* one 1024 weeks roll over for now*/
            gps_wn += 1024;
        }
        else
        {
            /* Two 1024 weeks roll over after another 464 weeks.
            more to come, but it's 20 years later.*/
            gps_wn += 2048;
        }
    }
    return MSL_SetGPSTime(pRefTime, gps_wn);
}

U16 RXN_MSL_SetGLOTime(U08 N4, U16 NT, U08 tb)
{
    RXN_RefTime_t refTime;
    U32 gpsTime = MSL_ConvertGLOTimeToGPSTime(MSL_ConvertGLOComponentsToGLOSec(N4, NT, tb));

    memset(&refTime, 0, sizeof(RXN_RefTime_t));
    refTime.weekNum = gpsTime / 604800;
    refTime.TOWmSec = gpsTime % 604800 * 1000;

    return RXN_MSL_SetGPSTime(&refTime);
}

U32 RXN_MSL_GetBestGPSTime()
{
    return MSL_GetBestGPSTime(NULL);
}

U16 RXN_MSL_GetSNTPTime(RXN_RefTime_t* pRefTime)
{
    return MSL_GetSNTPTime(pRefTime);
}

U32 MSL_GPSSecondsOfEndUTCQuarter(U08 N4, U16 NA)
{
    U16 curYear =0;
    U08 leapyearadjustedNA = 0;
    U32 curGPSTime = 0;
    U32 refGPSTime = 0;
    MSL_time_t refTime;
    enum MSL_Current_Time_Source curTimeSource = MSL_CURRENT_TIME_SOURCE_NO;
    U08 i;

    /* clear variable */
    memset(&refTime, 0, sizeof(MSL_time_t));

    /* Ensures we account for the leap year */
    leapyearadjustedNA = (NA == 1461) ? 1460 : NA;
    curYear = MSL_GLONASS_N4_BASE_YEAR + N4*4 + leapyearadjustedNA/365;
    
    /* Get the current GPS time from the system. */
    curGPSTime = MSL_GetBestGPSTime(&curTimeSource);
    
    if (( curTimeSource != MSL_CURRENT_TIME_SOURCE_GPS ) &&
        ( curTimeSource != MSL_CURRENT_TIME_SOURCE_SNTP ))
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
            "MSL_GPSSecondsOfEndUTCQuarter: No accurate GPS time source.");
        return 0;
    }
    
    refTime.MSL_Day = 1;
    refTime.MSL_Month = 1;
    refTime.MSL_Year = curYear;
    refGPSTime = MSL_ConvertUTCToGPSTime(MSL_ConvertTimeStructToSeconds(&refTime));
    if(curGPSTime < refGPSTime)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
            "MSL_GPSSecondsOfEndUTCQuarter: N4 and NA seems to represent a time in the past.");
        return 0;
    }


    /* If it is in the first 3 quarters */
    for(i = 0; i <= 2; i++)
    {
        refTime.MSL_Month = 4+i*3;
        refGPSTime = MSL_ConvertUTCToGPSTime(MSL_ConvertTimeStructToSeconds(&refTime));
        if(curGPSTime < refGPSTime)
        {
            return refGPSTime;
        }
    }
    /* If it is in the 4th quarter */
    refTime.MSL_Day = 1;
    refTime.MSL_Month = 1;
    refTime.MSL_Year = curYear + 1;
    return MSL_ConvertUTCToGPSTime(MSL_ConvertTimeStructToSeconds(&refTime));
}

U16 RXN_MSL_SetLeapSecondsInfoKP(U08 kP, U08 N4, U16 NA)
{
    RXN_config_t config;
    U32 gpsTimeLeapSec;

    if(RXN_Get_Configuration(&config) != RXN_SUCCESS)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
            "RXN_MSL_SetLeapSecondsInfoKP: Unable to retrieve Core configuration settings.");
        return RXN_FAIL;
    }
    switch(kP)
    {
    case 0: /* 00 */
        {
            return RXN_MSL_SetLeapSecondsInfo(config.leap_secs, config.leap_secs, 0);
        }
    case 1: /* 01 */
        {
            gpsTimeLeapSec = MSL_GPSSecondsOfEndUTCQuarter(N4, NA);

            if(gpsTimeLeapSec != 0)
            {
                return RXN_MSL_SetLeapSecondsInfo(config.leap_secs, config.leap_secs+1, gpsTimeLeapSec+1);
            }
            break;
        }
    case 3: /* 11 */
        {
            gpsTimeLeapSec = MSL_GPSSecondsOfEndUTCQuarter(N4, NA);

            if(gpsTimeLeapSec != 0)
            {
                return RXN_MSL_SetLeapSecondsInfo(config.leap_secs, config.leap_secs-1, gpsTimeLeapSec);
            }
            break;
        }
    case 2:
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
                "RXN_MSL_SetLeapSecondsInfoKP: No update required.");
            return RXN_SUCCESS;
        }
    default:
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
                "RXN_MSL_SetLeapSecondsInfoKP: Invalid KP value.");
            return RXN_FAIL;
        }
    }
    return RXN_FAIL;
}

U16 RXN_MSL_SetLeapSecondsInfo(U08 curLeapSecVal, U08 nextLeapSecVal, U32 nextLeapSecGPStime)
{
    /* As of Core 6.x, only +1 leap seconds are supported. No operation is done with nextLeapSecVal. */
    RXN_config_t config;

    if(RXN_Get_Configuration(&config) != RXN_SUCCESS)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
            "RXN_MSL_SetLeapSecondsInfo: Unable to retrieve Core configuration settings.");
        return RXN_FAIL;
    }
    if((nextLeapSecVal != curLeapSecVal)&&(nextLeapSecVal != curLeapSecVal + 1))
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
            "RXN_MSL_SetLeapSecondsInfo: Unable to support a nextLeapSecVal that is not not the same as curLeapSecVal or curLeapSecVal + 1.");
        return RXN_FAIL;
    }

    if(config.leap_secs != curLeapSecVal)
    {
        MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "RXN_MSL_SetLeapSecondsInfo: Original leap second value: %1.0f. New leap second value: %1.0f.", config.leap_secs, curLeapSecVal);
        config.leap_secs = curLeapSecVal;
    }

    if( config.gps_time_next_leap != nextLeapSecGPStime)
    {
        MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "RXN_MSL_SetLeapSecondsInfo: Original next leap second (GPS time): %1.0f. New next leap second (GPS time): %1.0f.", config.gps_time_next_leap, nextLeapSecGPStime);
        config.gps_time_next_leap = nextLeapSecGPStime;
    }
    RXN_Set_Configuration(&config);
    return RXN_SUCCESS;
}

static U16 GetEphemeris(const U08 prn, RXN_Ephemeris_u* pEphemeris, RXN_constel_t constel)
{
    U32 currentGPSTime = 0;
    U32 currentGLOTime = 0;
    U16 result = RXN_SUCCESS;  /* Result store. */ 

    /* Get the current GPS time from the system. */
    currentGPSTime = RXN_MSL_GetBestGPSTime();

    /* Get ephemeris from the RxN lib. */
    switch(constel)
    {
    case RXN_GPS_CONSTEL:
        {
            result = RXN_Get_Ephemeris(prn, currentGPSTime, &pEphemeris->gpsEphemeris, constel);
        }
        break;

    case RXN_GLO_CONSTEL:
        {
#ifdef RXN_CONFIG_INCLUDE_GLONASS
            currentGLOTime = MSL_ConvertGPSTimeToGLOTime(currentGPSTime);

            /* GLONASS time is bounded to 15 minute intervals.  By default we allow assistance
               requests for any continuous time.  If GLO_Use_Bounded_Time is set in MSLConfig.txt
               then force the assistance request to be for the :15 or :45 minute interval, emulating
               GLONASS BCE */
            if (MSL_UseBoundedGLOSec())
            {
                currentGLOTime = MSL_ConvertGLOSecToBoundedGLOSec(currentGLOTime);
            }
            result = RXN_Get_Glonass_Ephemeris(prn, currentGLOTime, &pEphemeris->gloEphemeris);
#else
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
                    "GetEphemeris: GLONASS support has not been enabled.");
            result = RXN_FAIL;
#endif
        }
        break;
    } 
    return result;
}

static U16 SetEphemeris(RXN_Ephemeris_u* pEphemeris, RXN_constel_t constel)
{
    U16 result = RXN_SUCCESS;  /* Result store. */ 

    /* Get ephemeris from the RxN lib. */
    switch(constel)
    {
    case RXN_GPS_CONSTEL:
        {
            result = RXN_Set_Ephemeris(&pEphemeris->gpsEphemeris);
        }
        break;

    case RXN_GLO_CONSTEL:
        {
#ifdef RXN_CONFIG_INCLUDE_GLONASS
            result = RXN_Set_Glonass_Ephemeris(&pEphemeris->gloEphemeris);
#else
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
                    "SetEphemeris: GLONASS support has not been enabled.");
            result = RXN_FAIL;
#endif
        }
        break;
    } 
    return result;
}

static U16 FTinMetres(U08 FT)
{
    /* If FT too high, returned maxed out value */
    if (FT >= 15)
        return 0xFFFF;

    /* Return the value in metres */
    return mFT_in_m[FT];
}

static U16 CheckThreshold(const U08 prn, RXN_Ephemeris_u* pEphemeris, RXN_constel_t constel)
{
    U16 result = RXN_SUCCESS;  /* Result store. */ 

    switch(constel)
    {
    case RXN_GPS_CONSTEL:
        {
            /* If the URE value within EE is above the user provided threashold,
            * don't do anything with it and move on. */
            if(pEphemeris->gpsEphemeris.ure > gConfig.constel[constel].AssistanceThreshold)
            {
                MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_WARNING, RXN_LOG_ZONE08,
                    "RXN_MSL_GetEphemeris: EE URE higher than URE threshold for PRN: %1.0f ...", (double) prn);
                MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_WARNING, RXN_LOG_ZONE08,
                    "RXN_MSL_GetEphemeris: ... EE URE value %1.0f m higher than threshold value %1.0f m. ",
                    (double) pEphemeris->gpsEphemeris.ure, (double) gConfig.constel[constel].AssistanceThreshold);

                result = RXN_FAIL; 
            }
        }
        break;

    case RXN_GLO_CONSTEL:
        {
            /* If the FT value within EE is above the user provided threashold,
            * don't do anything with it and move on. */
            if(FTinMetres(pEphemeris->gloEphemeris.FT) > gConfig.constel[constel].AssistanceThreshold)
            {
                MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_WARNING, RXN_LOG_ZONE08,
                    "RXN_MSL_GetEphemeris: FT URE higher than FT threshold for PRN: %1.0f ...", (double) prn);
                MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_WARNING, RXN_LOG_ZONE08,
                    "RXN_MSL_GetEphemeris: ... EE FT value %1.0f higher than threshold value %1.0f m. ",
                    (double) pEphemeris->gloEphemeris.FT, (double) gConfig.constel[constel].AssistanceThreshold);

                result = RXN_FAIL; 
            }
        }
        break;
    } 
    return result;
}

void MSL_RXNToFull(RXN_Ephemeris_u* pEphemeris, RXN_MSL_NavDataList_t* pNavModelData, U08 ephCount, RXN_constel_t constel)
{
    switch(constel)
    {
    case RXN_GPS_CONSTEL:
        {
            /* Convert the ephemeris to the RXN_FullEph_t within pAssist. */
            MSL_GPS_RXNToFull(&pEphemeris->gpsEphemeris, &pNavModelData->ephList.gps[ephCount],
                0, 0, 0, 0, 0);
        }
        break;

    case RXN_GLO_CONSTEL:
        {
            MSL_GLO_RXNToFull(&pEphemeris->gloEphemeris, &pNavModelData->ephList.glo[ephCount]);
        }
        break;
    }
}

void MSL_FullToRXN(RXN_FullEphem_u PRNArr, U08 Idx, RXN_Ephemeris_u* pEphemeris, RXN_constel_t constel)
{
    switch(constel)
    {
    case RXN_GPS_CONSTEL:
        {
            /* Convert the ephemeris to the RXN_FullEph_t within pAssist. */
            MSL_GPS_FullToRXN(&PRNArr.gpsPRNArr[Idx], &pEphemeris->gpsEphemeris);
        }
        break;

    case RXN_GLO_CONSTEL:
        {
            MSL_GLO_FullToRXN(&PRNArr.gloPRNArr[Idx], &pEphemeris->gloEphemeris);
        }
        break;
    }
}

static void LogConstellationEE(const U08 prn, RXN_Ephemeris_u* pEphemeris, RXN_constel_t constel)
{
    switch(constel)
    {
    case RXN_GPS_CONSTEL:
        {
            MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
                "RXN_MSL_GetEphemeris: Acquired EE for PRN: %1.0f with URE: %1.0f",
                (double) prn, (double) pEphemeris->gpsEphemeris.ure);
            LogEphemeris(pEphemeris, constel);
        }
        break;

    case RXN_GLO_CONSTEL:
        {
            MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
                "RXN_MSL_GetEphemeris: Acquired EE for PRN: %1.0f. FT: %1.0f",
                (double) prn, (double) pEphemeris->gloEphemeris.FT);
            LogEphemeris(pEphemeris, constel);
        }
        break;
    }
}

static U16 HasEphemeris(RXN_FullEphem_u PRNArr, U08 Idx, RXN_constel_t constel)
{
    switch(constel)
    {
    case RXN_GPS_CONSTEL:
        {
            if(PRNArr.gpsPRNArr[Idx].prn == 0)
            {
                return RXN_FAIL;
            }
        }
        break;

    case RXN_GLO_CONSTEL:
        {
            if(PRNArr.gloPRNArr[Idx].slot == 0)
            {
                return RXN_FAIL;
            }
        }
        break;
    }

    return RXN_SUCCESS;
}

static void LogEphemeris(RXN_Ephemeris_u* pEphemeris, RXN_constel_t constel)
{
    char ephList[1024];

    switch(constel)
    {
    case RXN_GPS_CONSTEL:
        {
            /* DEBUG */
            snprintf(ephList, sizeof(ephList), "N,%u,%u,%u,%d | %d,%u,%u,%u | %u,%u,%d,%d | %d,%d,%d,%d | %d,%d,%d,%d | %d,%u,%u,%d | %d, %d, %d",
                pEphemeris->gpsEphemeris.prn,
                pEphemeris->gpsEphemeris.ura,
                pEphemeris->gpsEphemeris.health,
                pEphemeris->gpsEphemeris.af2,
                /* | */
                pEphemeris->gpsEphemeris.ephem_fit,
                pEphemeris->gpsEphemeris.ure,
                pEphemeris->gpsEphemeris.gps_week,
                pEphemeris->gpsEphemeris.iode,
                /* | */
                pEphemeris->gpsEphemeris.toc,
                pEphemeris->gpsEphemeris.toe,
                pEphemeris->gpsEphemeris.af1,
                pEphemeris->gpsEphemeris.i_dot,
                /* | */
                pEphemeris->gpsEphemeris.delta_n,
                pEphemeris->gpsEphemeris.cuc,
                pEphemeris->gpsEphemeris.cus,
                pEphemeris->gpsEphemeris.cic,
                /* | */
                pEphemeris->gpsEphemeris.cis,
                pEphemeris->gpsEphemeris.crc,
                pEphemeris->gpsEphemeris.crs,
                pEphemeris->gpsEphemeris.af0,
                /* | */
                pEphemeris->gpsEphemeris.m0,
                pEphemeris->gpsEphemeris.e,
                pEphemeris->gpsEphemeris.sqrt_a,
                pEphemeris->gpsEphemeris.omega0,
                /* | */
                pEphemeris->gpsEphemeris.i0,
                pEphemeris->gpsEphemeris.w,
                pEphemeris->gpsEphemeris.omega_dot);

            MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
                "LogEphemeris: GPS: %s", ephList);
        }
        break;

    case RXN_GLO_CONSTEL:
        {
            /* DEBUG */
            snprintf(ephList, sizeof(ephList), "N,%u,%u,%d,%u | %u,%u,%d,%d | %u,%d,%d,%d | %d,%d,%d,%d | %d,%d",
                pEphemeris->gloEphemeris.slot,
                pEphemeris->gloEphemeris.FT,
                pEphemeris->gloEphemeris.freqChannel,
                pEphemeris->gloEphemeris.M,
                /* | */
                pEphemeris->gloEphemeris.Bn,
                pEphemeris->gloEphemeris.utc_offset,
                pEphemeris->gloEphemeris.gamma,
                pEphemeris->gloEphemeris.tauN,
                /* | */
                pEphemeris->gloEphemeris.gloSec,
                pEphemeris->gloEphemeris.x,
                pEphemeris->gloEphemeris.y,
                pEphemeris->gloEphemeris.z,
                /* | */
                pEphemeris->gloEphemeris.vx,
                pEphemeris->gloEphemeris.vy,
                pEphemeris->gloEphemeris.vz,
                pEphemeris->gloEphemeris.lsx,
                /* | */
                pEphemeris->gloEphemeris.lsy,
                pEphemeris->gloEphemeris.lsz
                );

            MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
                "LogEphemeris: GLONASS: %s", ephList);
        }
        break;
    }
}

U08 RXN_MSL_ProcessRinexFile(const char rinexFile[RXN_MSL_MAX_PATH], RXN_constel_t constel)
{   
    switch(constel)
    {
    case RXN_GPS_CONSTEL:
        {
            return MSL_GPS_ProcessRinexFile(rinexFile);
        }
        break;

    case RXN_GLO_CONSTEL:
        {
            return MSL_GLO_ProcessRinexFile(rinexFile);
        }
        break;
    default:
        {
            MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
                "RXN_MSL_ProcessRinexFile: Constel: %1.0f is not supported.", (double) constel);
            return RXN_FAIL;
        }
    }
}

static BOOL IsXybridRequest(U32 assistTypeBitMask)
{
    return (assistTypeBitMask & RXN_MSL_ASSISTANCE_XBD_RT
            || assistTypeBitMask & RXN_MSL_ASSISTANCE_XBD_BCE
            || assistTypeBitMask & RXN_MSL_ASSISTANCE_SYNCHRO);
}

void RXN_MSL_TriggerAssistance(U32 assistTypeBitMask, U32 gpsPrnBitMask, U32 gloSlotBitMask)
{
    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE01, "RXN_MSL_TriggerAssistance: Assistance triggered: %1.0f", (float)assistTypeBitMask);

    if (IsXybridRequest(assistTypeBitMask))
    {
#ifdef XYBRID
        U32 currentAssistanceMask = MSL_GetAssistanceMask();
        static U32 timeOfLastXybridRequest = 0;

        /* If there is a XYBRID request in progress, block the request, unless it should be timed out. */
        if (IsXybridRequest(currentAssistanceMask)
            && MSL_GetTickSecond() <= timeOfLastXybridRequest + XYBRID_TIMEOUT)
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_WARNING, RXN_LOG_ZONE09, 
                    "RXN_MSL_TriggerAssistance: XYBRID request already in progress.");
        }
        else
        {
            timeOfLastXybridRequest = MSL_GetTickSecond();
            MSL_SetBCEFiltered(FALSE);

            /* Request Xybrid RT/BCE/Synchro from RXN Services */
            MSL_RequestXybridAssistance(assistTypeBitMask);
        }
#else
    /* Check if Xybrid assistance has been requested, but isn't available in this release. */
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE09, 
                "RXN_MSL_TriggerAssistance: Xybrid assistance requested but not available in this release.");
#endif
    }

    /* If requested, get PGPS assistance and return it to observers */
    if (assistTypeBitMask & RXN_MSL_ASSISTANCE_PGPS)
    {
        RXN_MSL_NavDataList_t ephData;    

        memset((void*) &ephData, 0, sizeof(RXN_MSL_NavDataList_t));

        if (gpsPrnBitMask > 0)
        {
            if(RXN_MSL_GetEphemeris(gpsPrnBitMask, &ephData, RXN_GPS_CONSTEL) == RXN_SUCCESS
                    && ephData.numEntries > 0)
            {
                MSL_SendEEToObservers(&ephData, RXN_GPS_CONSTEL);
            }
        }

        if (gloSlotBitMask > 0)
        {
            memset((void*) &ephData, 0, sizeof(RXN_MSL_NavDataList_t));
            if(RXN_MSL_GetEphemeris(gloSlotBitMask, &ephData, RXN_GLO_CONSTEL) == RXN_SUCCESS
                    && ephData.numEntries > 0)
            {
                MSL_SendEEToObservers(&ephData, RXN_GLO_CONSTEL);
            }
        }

        CheckAssistanceDone();
    }
}

static void CheckAssistanceDone()
{
#ifdef XYBRID
    if (MSL_GetAssistanceMask() == 0)
#endif
    {
        RXN_MSL_Observer_t* ptr = NULL;
        MSL_EnterBlock(RXN_MSL_CS_Observer);
        ptr = mFinishedAssistanceObservers;

        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE09, 
                "CheckAssistanceDone: Sending finished assistance to observers");

        while (ptr)
        {
            ((RXN_MSL_FinishedAssistance_Callback)ptr->callback)();
            ptr = ptr->next;
        }
    MSL_LeaveBlock(RXN_MSL_CS_Observer);
    }
}

void MSL_SendLocationToObservers(RXN_RefLocation_t* position)
{
    const RXN_MSL_Observer_t* ptr = NULL;
    
    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE09, 
            "MSL_SendLocationToObservers: Sending location to observers");

    MSL_EnterBlock(RXN_MSL_CS_Observer);
    ptr = mLocationObservers;
    while (ptr)
    {
        ((RXN_MSL_Location_Callback)ptr->callback)(position);
        ptr = ptr->next;
    }
    MSL_LeaveBlock(RXN_MSL_CS_Observer);

    CheckAssistanceDone();
}

void MSL_SendBCEToObservers(RXN_MSL_NavDataList_t* ephemeris, RXN_constel_t constel)
{
    const RXN_MSL_Observer_t* ptr;
    
    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE09, 
            "MSL_SendBCEToObservers: Sending BCE to observers for constel %1.0f", constel);

    MSL_EnterBlock(RXN_MSL_CS_Observer);
    ptr = mBCEObservers;
    while (ptr)
    {
        ((RXN_MSL_Ephemeris_Callback)ptr->callback)(ephemeris, constel);
        ptr = ptr->next;
    }
    MSL_LeaveBlock(RXN_MSL_CS_Observer);

    CheckAssistanceDone();
}

void MSL_SendEEToObservers(RXN_MSL_NavDataList_t* ephemeris, RXN_constel_t constel)
{
    const RXN_MSL_Observer_t* ptr = NULL;

    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE07, 
            "MSL_SendEEToObservers: Sending EE to observers for constel %1.0f", constel);

    MSL_EnterBlock(RXN_MSL_CS_Observer);
    ptr = mEEObservers;
    while (ptr)
    {
        ((RXN_MSL_Ephemeris_Callback)ptr->callback)(ephemeris, constel);
        ptr = ptr->next;
    }
    MSL_LeaveBlock(RXN_MSL_CS_Observer);
}

void RXN_MSL_AddLocationObserver(RXN_MSL_Location_Callback callback)
{
    MSL_AddObserver(&mLocationObservers, (void*)callback);
}

void RXN_MSL_AddBCEObserver(RXN_MSL_Ephemeris_Callback callback)
{
    MSL_AddObserver(&mBCEObservers, (void*)callback);
}

void RXN_MSL_AddEEObserver(RXN_MSL_Ephemeris_Callback callback)
{
    MSL_AddObserver(&mEEObservers, (void*)callback);
}

void RXN_MSL_AddFinishedAssistanceObserver(RXN_MSL_FinishedAssistance_Callback callback)
{
    MSL_AddObserver(&mFinishedAssistanceObservers, (void*)callback);
}

void RXN_MSL_RemoveLocationObserver(RXN_MSL_Location_Callback callback)
{
    MSL_RemoveObserver(&mLocationObservers, (void*)callback);
}

void RXN_MSL_RemoveBCEObserver(RXN_MSL_Ephemeris_Callback callback)
{
    MSL_RemoveObserver(&mBCEObservers, (void*)callback);
}

void RXN_MSL_RemoveEEObserver(RXN_MSL_Ephemeris_Callback callback)
{
    MSL_RemoveObserver(&mEEObservers, (void*)callback);
}

void RXN_MSL_RemoveFinishedAssistanceObserver(RXN_MSL_FinishedAssistance_Callback callback)
{
    MSL_RemoveObserver(&mFinishedAssistanceObservers, (void*)callback);
}

