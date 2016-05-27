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
* $LastChangedDate: 2009-04-17 13:53:14 -0700 (Fri, 17 Apr 2009) $
* $Revision: 10192 $
*************************************************************************
*
* This file contains implementation of APIs exposed within the RXN_MSL_Common.h file.
* 
*/

#include <stdio.h>          /* Required for string manipulation. */
#include <stdlib.h>         /* Required for std functions such as atof(). */

#include "RXN_MSL_Common.h" /* Contains declarations for fcns within. */
#include "RXN_MSL_Time.h"

/************************************************************************
* Config Below.  The values provided are defaults which are used if no *
* MSLConfig.txt is present.                                            *
************************************************************************/
MSL_Config_t gConfig =
{
    /* Base GPS week used to determine GPS week roll over (%1024) */
    MSL_GPS_WEEK_BUILD_TIME,
    /* Time to wait during start up before making seed or SNTP request */
    10,
    /* How frequently (in seconds) to check seeds for required updates. */
    3600,
    /* Version of the config file. */
    "",
    /* Pantry file path store. */
    "./RxN",
    /* Age of seed to download (in days) */
    0,
    /* Number of SAGPS segments used */
    -1,
    /* Constellation list */
    { RXN_GPS_CONSTEL, RXN_GLO_CONSTEL },
    /* Number of constellations */
    1,
    /* CL config */
    {
        /* CL config string store. */
        "",
            /* CL Log file path store. */
            "./CL_Log.txt"
    },
    /* Net config */
    {
        /* Host port list */
        {9280, 80, 0, 0},
            /* Host port index */
            0,
            /* Host name */
            "pgps7-%.gpstream.net",
            /* Maximum hostname load balancing index */
            3,
            /* If set to FALSE MSL will blindly try to download seed, if set to TRUE MSL 
            * will smartly try only when data connectivity and roaming settings permit 
            * data transfer. */
            TRUE,
            /* How frequent we hit the seed server after a failed initial attempt (in seconds)*/
            60,
            /* Duration until next set of retries */
            86400,
            /* Maximum number of failed retries */
            3
        },
        /* Security config */
        {
            /* Vendor ID store. */
            "evaluation",
                /* Model ID store. */
                "model1",
                /* License key path store. */
                "./license.key",
                /* Security key path store. */
                "./security.key"
        },
        {
            {
                /* GPS Propagation config */
                {
                    /* How far to propagate forward in seconds. */
                    1209600,
                        /* How far to propagate forward SAGPS seeds in seconds. Max 3 days. Default 5 days */
                        432000
                },
                /* GPS Seed config */
                {
                    /* How old a seed should be before its clock is updated (in seconds). */
                    345600,
                        /* How Maximum offset used by a random function to added to the 
                        * Clock_Update_Age for server access load balancing.*/
                        21600,
                        /* How old a seed should be before it is replaced (in seconds). */
                        345600, /* 4 days */
                        /* How Maximum offset used by a random function to added to the 
                        * Seed_Update_Age for server access load balancing.*/
                        21600,
                    },
                    /* The max URE value in meters before EE is un-usable. */
                    100,
                    /* GPS flags */
                    0
            },
            {
                /* GLO Propagation config */
                {
                    /* How far to propagate forward in seconds. */
                    1209600,
                        /* How far to propagate forward SAGPS seeds in seconds. Max 3 days. Default 3 days */
                        432000
                },
                /* GLO Seed config */
                {
                    /* How old a seed should be before its clock is updated (in seconds). */
                    345600,
                        /* How Maximum offset used by a random function to added to the 
                        * Clock_Update_Age for server access load balancing.*/
                        21600,
                        /* How old a seed should be before it is replaced (in seconds). */
                        345600, /* 4 days */
                        /* How Maximum offset used by a random function to added to the 
                        * Seed_Update_Age for server access load balancing.*/
                        21600,
                    },
                    /* The max FT value in meters before EE is un-usable. */
                    100,
                    /* GLO flags */
                    0
                    }
            },

                /* SNTP config */
            {
                /* SNTP Host URL string. */
                "time1.gpstream.net",
                    /* SNTP Host port. */
                    123,
                    /* How frequent we hit the SNTP server after a failed initial attempt (in seconds)*/
                    60,
                    /* Duration until next set of retries */
                    86400,
                    /* Maximum number of failed retries */
                    3
            },
            /* TOE config */
            {
                /* Ephemeris duration as specified by the TOE. */
                300,
                    /* Chipset ephemeris expiration period. */
                    7200,
                    /* TOE buffer offset (how far seed TOE times will be rolled back). */
                    -7200
                },
                /* flags */
                0,
#ifdef XYBRID
                /* XYBRID config */
                {
                    /* Xybrid host */ 
                    "xybrid%.gpstream.net",
                        /* Xybrid host port */ 
                        9380,
                        /* Maximum hostname load balancing index */
                        2,
                        /* Xybrid vendor ID */
                        "evaluation",
                        /* Xybrid vendor salt */
                        "",
                        /* Xybrid flags */
                        RXN_MSL_XBD_CONFIG_WIFI_LOOKUP_ENABLED | 
                        RXN_MSL_XBD_CONFIG_DONATE_ENABLED |
                        RXN_MSL_XBD_CONFIG_CELL_LOOKUP_ENABLED

                }
#endif
};

/* Number of users requesting propagation abort */
static U08 mAbortCount = 0;

#define RXN_MSL_COMMA ","

/* Function prototype */
U16 MSL_ProcessServerRetryConfig();

/*************************
* Abort Functions Below *
*************************/

BOOL MSL_CheckAbort(void)
{
  return mAbortCount > 0;
}

void MSL_SetAbort(BOOL bAbort)
{
  if (bAbort)
  {
    mAbortCount++;
  }
  else if (mAbortCount > 0)
  {
    mAbortCount--;
  }
}

/***************************
* Utility Functions Below *
***************************/

static double bound(double parameter, S32 min, S32 max)
{
    if (parameter < min)
        return min;
    else if (parameter > max)
        return max;

    return parameter;
}

U16 MSL_ProcessConfig(void)
{
    char strParam[MSL_MAX_PARAM_STR_LEN];   /* String holding params. */
    double dblParam = 0;                    /* Double holding params. */
    char * pTempChar;                       /* Req'd by strtol(). */
    U16 result = RXN_FAIL;

    /* Process base GPS week number. */
    if(MSL_ReadParam("Base_GPS_Week", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.baseGPSWeekNum = (U16) bound(dblParam, 0, 1024);
    }

    /* Process the config file version. */
    if(MSL_ReadParam("Config_File_Ver", NULL, strParam) == RXN_SUCCESS)
    {
        memset((void*) gConfig.version, 0, sizeof(char) * MSL_MAX_FILE_CONFIG_STR_LEN);
        strcpy(gConfig.version, strParam);
    }

    /* Process the log file max size. */
    if(MSL_ReadParam("Log_Max_Size", &dblParam, NULL) == RXN_SUCCESS)
    {
        MSL_LogSetMaxFilesize((U32)dblParam);
    }

    /* Process the log severity threshold. */
    if(MSL_ReadParam("Log_Sev_Threshold", &dblParam, NULL) == RXN_SUCCESS)
    {
        MSL_LogSetSevThreshold((U08)dblParam);
    }

    /* Process the log zone mask. */
    if(MSL_ReadParam("Log_Zone_Mask", NULL, strParam) == RXN_SUCCESS)
    {
        /* Convert the zone mask (provided in hex) to an unsigned int. */
        MSL_LogSetZoneMask((U16) strtol(strParam, &pTempChar, 16));
    }

    /* Process the license key file path. */
    if(MSL_ReadParam("Lic_Key_Path", NULL, strParam) == RXN_SUCCESS)
    {
        strncpy(gConfig.sec.licKeyPath, strParam, sizeof(gConfig.sec.licKeyPath));
    }

    /* Process the security key file path. */
    if(MSL_ReadParam("Sec_Key_Path", NULL, strParam) == RXN_SUCCESS)
    {
        strncpy(gConfig.sec.secKeyPath, strParam, sizeof(gConfig.sec.secKeyPath));
    }

    /* Process the ephemeris duration. */
    if(MSL_ReadParam("TOE_Eph_Dur", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.TOE.ephDur = (U16) bound(dblParam, MSL_EPHEMERIS_DURATION_MIN, MSL_EPHEMERIS_DURATION_MAX);
    }

    /* Process the chipset expiry. */
    if(MSL_ReadParam("TOE_Chip_Exp", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.TOE.chipExp = (U16) bound(dblParam, MSL_CHIPSET_EXPIRY_MIN, MSL_CHIPSET_EXPIRY_MAX);
    }

    /* Process the seed TOE buffer offset. */
    if(MSL_ReadParam("TOE_Buf_Offset", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.TOE.bufOffset = (S16) bound(dblParam, MSL_TOE_BUFFER_OFFSET_MIN, MSL_TOE_BUFFER_OFFSET_MAX);
    }

    /* Process the hours to propagate forward. */
    if(MSL_ReadParam("GPS_Prop_Fwd", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.constel[RXN_GPS_CONSTEL].prop.PGPSFwd = 
            (U32) bound(dblParam, MSL_PROPAGATE_FORWARD_MIN, MSL_PROPAGATE_FORWARD_MAX);

        /* Set how far to propagate SAGPS seeds */
        if(gConfig.constel[RXN_GPS_CONSTEL].prop.PGPSFwd < gConfig.constel[RXN_GPS_CONSTEL].prop.SAGPSFwd)
        {
            gConfig.constel[RXN_GPS_CONSTEL].prop.SAGPSFwd = gConfig.constel[RXN_GPS_CONSTEL].prop.PGPSFwd;
        }
    }

    /* Process the URE threshold. */
    if(MSL_ReadParam("URE_Threshold", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.constel[RXN_GPS_CONSTEL].AssistanceThreshold = 
            (U16) bound(dblParam, MSL_URE_THREASHOLD_MIN, MSL_URE_THREASHOLD_MAX);
    }

    /* Process the FT threshold. */
    if(MSL_ReadParam("FT_Threshold", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.constel[RXN_GLO_CONSTEL].AssistanceThreshold = 
            (U16) bound(dblParam, MSL_URE_THREASHOLD_MIN, MSL_URE_THREASHOLD_MAX);
    }

    /* Process the vendor ID. */
    if(MSL_ReadParam("Vendor_ID", NULL, strParam) == RXN_SUCCESS)
    {
        memset((void*) gConfig.sec.vendorId, 0, sizeof(char) * MSL_MAX_VENDOR_STR_LEN);
        strncpy(gConfig.sec.vendorId, strParam, sizeof(gConfig.sec.vendorId));
    }

    /* Process the model ID. */
    if(MSL_ReadParam("Model_ID", NULL, strParam) == RXN_SUCCESS)
    {
        memset((void*) gConfig.sec.modelId, 0, sizeof(char) * MSL_MAX_MODEL_STR_LEN);
        strncpy(gConfig.sec.modelId, strParam, sizeof(gConfig.sec.modelId));
    }

    /* Process the host URL. */
    if(MSL_ReadParam("Seed_Host", NULL, strParam) == RXN_SUCCESS)
    {
        memset((void*) gConfig.net.host, 0, sizeof(char) * MSL_MAX_HOST_STR_LEN);
        strncpy(gConfig.net.host, strParam, sizeof(gConfig.net.host));
    }

    /* Process the host max index. */
    if(MSL_ReadParam("Seed_Max_Host_Idx", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.net.hostMaxIdx = (U08) dblParam;
    }

    /* Process the host port. */
    if(MSL_ReadParam("Seed_Port", NULL, strParam) == RXN_SUCCESS)
    {
        char* token = strtok(strParam, RXN_MSL_COMMA);
        U08 index = 0;

        memset((void*) gConfig.net.hostPortList, 0, sizeof(U16) * MSL_MAX_HOST_PORT_INDEX);

        while (token && index < MSL_MAX_HOST_PORT_INDEX)
        {
            gConfig.net.hostPortList[index++] = atoi(token);
            token = strtok(NULL, RXN_MSL_COMMA);
        }
    }

    /* Process smart downloading feature. */
    if(MSL_ReadParam("Respect_Data_Settings", &dblParam, NULL) == RXN_SUCCESS)
    {
        if(dblParam == 0)
        {
            gConfig.net.respect_Data_Settings = FALSE;
        }
        else
        {
            gConfig.net.respect_Data_Settings = TRUE;
        }
    }

    /* Process the seed check freq. */
    if(MSL_ReadParam("System_Check_Freq", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.systemCheckFreq = (U32) bound(dblParam, MSL_SEED_CHECK_MIN, MSL_SEED_CHECK_MAX);
    }

    if(MSL_ReadParam("Use_System_Time", &dblParam, NULL) == RXN_SUCCESS)
    {
        if (dblParam == 0)
        {
            /* Unset the use system time flag */
            gConfig.flags &= ~MSL_CONFIG_FLAG_USE_SYSTEM_TIME;
        }
        else
        {
            /* Set the use system time flag */
            gConfig.flags |= MSL_CONFIG_FLAG_USE_SYSTEM_TIME;
        }
    }

    /* Process the seed update age. */
    if(MSL_ReadParam("GPS_Seed_Update_Age", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.constel[RXN_GPS_CONSTEL].seed.updateAge = 
            (U32) bound(dblParam, MSL_SEED_UPDATE_AGE_MIN, MSL_SEED_UPDATE_AGE_MAX);
    }

    /* Process the seed update age. */
    if(MSL_ReadParam("GPS_Seed_Update_Age_Offset", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.constel[RXN_GPS_CONSTEL].seed.updateAgeOffset = 
            (U32) bound(dblParam, MSL_SEED_UPDATE_AGE_OFFSET_MIN, MSL_SEED_UPDATE_AGE_OFFSET_MAX);
    }

    /* Process the clock update age. */
    if(MSL_ReadParam("GPS_Clock_Update_Age", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.constel[RXN_GPS_CONSTEL].seed.clockUpdateAge = 
            (U32) bound(dblParam, MSL_CLOCK_UPDATE_AGE_MIN, MSL_CLOCK_UPDATE_AGE_MAX);
    }

    /* Process the clock update age. */
    if(MSL_ReadParam("GPS_Clock_Update_Age_Offset", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.constel[RXN_GPS_CONSTEL].seed.clockUpdateAgeOffset = 
            (U32) bound(dblParam, MSL_CLOCK_UPDATE_AGE_OFFSET_MIN, MSL_CLOCK_UPDATE_AGE_OFFSET_MAX);
    }

    /* Process the MSL Log file path. */
    if(MSL_ReadParam("MSL_Log_Path", NULL, strParam) == RXN_SUCCESS)
    {
        MSL_LogSetLogPath(strParam, sizeof(strParam));
    }

    /* Process the CL Log file path. */
    if(MSL_ReadParam("CL_Log_Path", NULL, strParam) == RXN_SUCCESS)
    {
        memset((void*) gConfig.CL.logPath, 0, sizeof(char) * MSL_MAX_PATH);
        strncpy(gConfig.CL.logPath, strParam, sizeof(gConfig.CL.logPath));
    }

    /* Process the CL Config string. */
    if(MSL_ReadParam("CL_Config_Str", NULL, strParam) == RXN_SUCCESS)
    {
        memset((void*) gConfig.CL.configStr, 0, sizeof(char) * RXN_MSL_CONFIG_MAX_STR_LEN);
        strncpy(gConfig.CL.configStr, strParam, sizeof(gConfig.CL.configStr));
    }

    /* Process the CL Config string. */
    if(MSL_ReadParam("Pantry_Path", NULL, strParam) == RXN_SUCCESS)
    {
        memset((void*) gConfig.pantryPath, 0, sizeof(char) * MSL_MAX_PATH);
        strncpy(gConfig.pantryPath, strParam, sizeof(gConfig.pantryPath));
    }

    /* Process the SNTP server host URL. */
    if(MSL_ReadParam("SNTP_Host", NULL, strParam) == RXN_SUCCESS)
    {
        memset((void*) gConfig.SNTP.host, 0, sizeof(char) * MSL_MAX_HOST_STR_LEN);
        strncpy(gConfig.SNTP.host, strParam, sizeof(gConfig.SNTP.host));
    }

    /* Process the host port. */
    if(MSL_ReadParam("SNTP_Port", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.SNTP.port = (U16) dblParam;
    }

    if(MSL_ReadParam("MSL_Log_Enabled", &dblParam, NULL) == RXN_SUCCESS)
    {
        MSL_LogEnable(dblParam != 0);
    }
    /* Process the hours to propagate forward. */
    if(MSL_ReadParam("GLO_Prop_Fwd", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.constel[RXN_GLO_CONSTEL].prop.PGPSFwd = 
            (U32) bound(dblParam, MSL_PROPAGATE_FORWARD_MIN, MSL_PROPAGATE_FORWARD_MAX);

        /* Set how far to propagate SAGPS seeds */
        if(gConfig.constel[RXN_GLO_CONSTEL].prop.PGPSFwd < gConfig.constel[RXN_GPS_CONSTEL].prop.SAGPSFwd)
        {
            gConfig.constel[RXN_GLO_CONSTEL].prop.SAGPSFwd = gConfig.constel[RXN_GLO_CONSTEL].prop.PGPSFwd;
        }
    }

    /* Process the seed update age. */
    if(MSL_ReadParam("GLO_Seed_Update_Age", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.constel[RXN_GLO_CONSTEL].seed.updateAge = 
            (U32) bound(dblParam, MSL_SEED_UPDATE_AGE_MIN, MSL_SEED_UPDATE_AGE_MAX);
    }

    /* Process the seed update age. */
    if(MSL_ReadParam("GLO_Seed_Update_Age_Offset", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.constel[RXN_GLO_CONSTEL].seed.updateAgeOffset = 
            (U32) bound(dblParam, MSL_SEED_UPDATE_AGE_OFFSET_MIN, MSL_SEED_UPDATE_AGE_OFFSET_MAX);
    }

    /* Process the clock update age. */
    if(MSL_ReadParam("GLO_Clock_Update_Age", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.constel[RXN_GLO_CONSTEL].seed.clockUpdateAge = 
            (U32) bound(dblParam, MSL_CLOCK_UPDATE_AGE_MIN, MSL_CLOCK_UPDATE_AGE_MAX);
    }

    /* Process the clock update age. */
    if(MSL_ReadParam("GLO_Clock_Update_Age_Offset", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.constel[RXN_GLO_CONSTEL].seed.clockUpdateAgeOffset = 
            (U32) bound(dblParam, MSL_CLOCK_UPDATE_AGE_OFFSET_MIN, MSL_CLOCK_UPDATE_AGE_OFFSET_MAX);
    }

    /* Process GLO_Enable_En */
    if(MSL_ReadParam("GLO_Enable_En", &dblParam, NULL) == RXN_SUCCESS)
    {
        if (dblParam == 0)
        {
            /* Unset the Enable En flag */
            gConfig.constel[RXN_GLO_CONSTEL].flags &= ~MSL_CONFIG_FLAG_GLO_ENABLE_EN;
        }
        else
        {
            /* Set the Enable En flag */
            gConfig.constel[RXN_GLO_CONSTEL].flags |= MSL_CONFIG_FLAG_GLO_ENABLE_EN;
        }
    }

    /* Process GLO_Use_Bounded_Time */
    if(MSL_ReadParam("GLO_Use_Bounded_Time", &dblParam, NULL) == RXN_SUCCESS)
    {
        if (dblParam == 0)
        {
            /* Unset the bounded time flag */
            gConfig.constel[RXN_GLO_CONSTEL].flags &= ~MSL_CONFIG_FLAG_GLO_USE_BOUNDED_TIME;
        }
        else
        {
            /* Set the bounded time flag */
            gConfig.constel[RXN_GLO_CONSTEL].flags |= MSL_CONFIG_FLAG_GLO_USE_BOUNDED_TIME;
        }
    }
    result = MSL_ProcessServerRetryConfig();

#ifdef RXN_CONFIG_INCLUDE_GLONASS
    /* Process the constellation config. */
    if(MSL_ReadParam("Constel_Config", NULL, strParam) == RXN_SUCCESS)
    {
        U08 index = 0;
        char* token = strtok(strParam, RXN_MSL_COMMA);

        while (token && index < MSL_MAX_CONSTEL)
        {
            // Eat up leading spaces
            while (token && *token == ' ') ++token;

            if (strcmp(token, "GPS") == 0)
            {
                gConfig.constelList[index] = RXN_GPS_CONSTEL;
            }
            else if (strcmp(token, "GLONASS") == 0)
            {
                gConfig.constelList[index] = RXN_GLO_CONSTEL;
            }
            else
            {
                /* return RXN_MSL_CONFIG_READ_ERR? */
            }
            token = strtok(NULL, RXN_MSL_COMMA);
            ++index;
        }
        gConfig.numConstels = index;
    }
#else
    gConfig.constelList[0] = RXN_GPS_CONSTEL;
    gConfig.numConstels = 1;
#endif

#ifdef XYBRID
    /* Process the xybrid HOST entry */
    if(MSL_ReadParam("Xybrid_Host", NULL, strParam) == RXN_SUCCESS)
    {
        strncpy(gConfig.xybrid.host, strParam, MSL_MAX_HOST_STR_LEN);
    }

    /* Process the xybrid port entry */
    if(MSL_ReadParam("Xybrid_Port", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.xybrid.hostPort = (U16) dblParam;
    }

    /* Process the host max index. */
    if(MSL_ReadParam("Xybrid_Max_Host_Idx", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.xybrid.hostMaxIdx = (U16) dblParam;
    }

    /* Process the xybrid vendor ID */
    if(MSL_ReadParam("Xybrid_VendorID", NULL, strParam) == RXN_SUCCESS)
    {
        strncpy(gConfig.xybrid.vendorId, strParam, MSL_MAX_VENDOR_STR_LEN);
    }

    /* Process the xybrid vendor salt */
    if(MSL_ReadParam("Xybrid_VendorSalt", NULL, strParam) == RXN_SUCCESS)
    {
        strncpy(gConfig.xybrid.vendorSalt, strParam, MSL_MAX_VENDOR_STR_LEN);
    }

    /* Process the XYBRID observations flag*/
    if(MSL_ReadParam("Xybrid_EnableObservations", &dblParam, NULL) == RXN_SUCCESS)
    {
        if (dblParam == 0)
        {
            /* Unset the XYBRID observations flag */
            gConfig.xybrid.flags &= ~RXN_MSL_XBD_CONFIG_DONATE_ENABLED;
        }
        else
        {
            /* Set the XYBRID observatiosn flag */
            gConfig.xybrid.flags |= RXN_MSL_XBD_CONFIG_DONATE_ENABLED;
        }
    }

    /* Process the XYBRID wifi flag*/
    if(MSL_ReadParam("Xybrid_UseWiFi", &dblParam, NULL) == RXN_SUCCESS)
    {
        if (dblParam == 0)
        {
            /* Unset the XYBRID wifi flag */
            gConfig.xybrid.flags &= ~RXN_MSL_XBD_CONFIG_WIFI_LOOKUP_ENABLED;
        }
        else
        {
            /* Set the XYBRID wifi flag */
            gConfig.xybrid.flags |= RXN_MSL_XBD_CONFIG_WIFI_LOOKUP_ENABLED;
        }
    }

    /* Process the XYBRID cell flag*/
    if(MSL_ReadParam("Xybrid_UseCell", &dblParam, NULL) == RXN_SUCCESS)
    {
        if (dblParam == 0)
        {
            /* Unset the XYBRID cell flag */
            gConfig.xybrid.flags &= ~RXN_MSL_XBD_CONFIG_CELL_LOOKUP_ENABLED;
        }
        else
        {
            /* Set the XYBRID cell flag */
            gConfig.xybrid.flags |= RXN_MSL_XBD_CONFIG_CELL_LOOKUP_ENABLED;
        }
    }
#endif

    return result;
}

U16 MSL_ProcessServerRetryConfig()
{
    double dblParam = 0;                        /* Double holding params. */

    if(MSL_ReadParam("Startup_Data_Wait_Duration", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.startupDataWaitDuration = (U32) bound(dblParam, 0, MSL_SEED_UPDATE_AGE_MAX);
    }

    if(MSL_ReadParam("SNTP_GPSTime_Uncert_Thresh", &dblParam, NULL) == RXN_SUCCESS)
    {
        MSL_SetTimeUncertThresh((U32)bound(dblParam, 0, MSL_SNTP_GPSTIME_UNCERT_MAX));
    }

    if(MSL_ReadParam("Seed_Retry_Period", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.net.downloadRetryPeriod = (U32) bound(dblParam, MSL_RETRY_PERIOD_MIN, MSL_RETRY_PERIOD_MAX);
    }

    if(MSL_ReadParam("SNTP_Retry_Period", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.SNTP.requestRetryPeriod = (U32) bound(dblParam, MSL_RETRY_PERIOD_MIN, MSL_RETRY_PERIOD_MAX);
    }

    if(MSL_ReadParam("Seed_Retry_Max", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.net.downloadRetryMax = (U08) bound(dblParam, 0, MSL_SEED_UPDATE_RETRY_MAX);
    }

    if(MSL_ReadParam("SNTP_Retry_Max", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.SNTP.requestRetryMax = (U08) bound(dblParam, 0, MSL_SNTP_RETRY_MAX);
    }

    if(MSL_ReadParam("Seed_Retry_Timer", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.net.downloadRetryTimer = (U32) bound(dblParam, MSL_RETRY_TIMER_MIN, MSL_RETRY_TIMER_MAX);
    }

    if(MSL_ReadParam("Seed_Age", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.seedAge = (U32) bound(dblParam, MSL_SEED_AGE_MIN, MSL_SEED_AGE_MAX);
    }

    if(MSL_ReadParam("Autonomous_Segments", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.sagpsSegments = (S08) bound(dblParam, MSL_SAGPS_SEGMENTS_MIN, MSL_SAGPS_SEGMENTS_MAX);
    }

    if(MSL_ReadParam("SNTP_Retry_Timer", &dblParam, NULL) == RXN_SUCCESS)
    {
        gConfig.SNTP.requestRetryTimer = (U32) bound(dblParam, MSL_RETRY_TIMER_MIN, MSL_RETRY_TIMER_MAX);
    }

  return RXN_SUCCESS;
}

U16 MSL_ReadParam(char* paramLabel, double* paramData, char paramStr[MSL_MAX_PARAM_STR_LEN])
{
    char fileLine[MSL_MAX_LINE_STR_LEN];  /* File line store. */
    char* pLabel = NULL;                  /* Will point to the start of the paramLabel if found. */
    char* pParam = NULL;                  /* Will point to the start of the param. */
    char* pExChar = NULL;                 /* Will point at extra chars following a param string. */
    //    U16 Result = RXN_FAIL;

    /* Init the fileLine string. */
    memset(fileLine, 0, sizeof(char) * MSL_MAX_LINE_STR_LEN);

    /* If a pointer is provided for paramStr, init it. */
    if(paramStr != NULL)
    {
        memset((void*) paramStr, 0, sizeof(char) * MSL_MAX_PARAM_STR_LEN);
    }

    /* Reset the file pointer. */
    MSL_SetFilePtrOffset(MSL_CONFIG_FILE_RSRC_ID, 0);

    /* Loop through lines within the file looking for paramLabel. */
    while(1)
    {
        /* Read a line. */
        if(MSL_ReadFileLine(MSL_CONFIG_FILE_RSRC_ID, (U08*)fileLine, MSL_MAX_PARAM_STR_LEN) != RXN_SUCCESS)
        {
            return MSL_FILE_IO_ERROR;
        }

        /* Look for a comment. */
        if(strstr(fileLine, "//") != NULL)
        {
            continue;
        }

        /* Look for the label. */
        pLabel = strstr(fileLine, paramLabel);
        if(pLabel != NULL)
        {
            /* Calc the start of param. */

            /* accept MSLConfig.txt to use "=" format, ie. SNTP_Retry_Max=3, original format is SNTP_Retry_Max: 3 */
            pParam = strstr(fileLine, "=");   
            if(pParam != NULL)
            {
                pParam = pParam + 1;
            }
            else
            {
                pParam = pLabel + strlen(paramLabel) + 2;
            }

            /* Get data and/or string values. */
            if(paramData != NULL)
            {
                /* Convert the paramData. */
                *paramData = atof(pParam);
            }

            if(paramStr != NULL)
            {
                /* Convert the paramStr. */
                strcpy(paramStr, pParam);

                /* Replace training spaces, \r or \n with \0. */

                pExChar = (char *) strchr(paramStr, '\r');
                if(pExChar != NULL)
                {
                    *pExChar = '\0';
                    break;
                }
                pExChar = (char *) strchr(paramStr, '\n');
                if(pExChar != NULL)
                {
                    *pExChar = '\0';
                }
            }

            /* Got a double, string or both. */
            break;

        } /*if(pLabel != NULL) */
    } /* while(1) */

    /* Success. */
    return RXN_SUCCESS;
}


void MSL_GPS_RXNToFull( RXN_ephem_t* pRXN, RXN_FullEph_t* pFull, U08 CAOrPOnL2,
                U16 iodc, U08 L2PData, S08 TGD, U08 AODO)
{
	/* Map pRXN to pFull and set additional params within pFull. */
	pFull->prn = pRXN->prn;
	pFull->gps_week = pRXN->gps_week;
	pFull->CAOrPOnL2 = CAOrPOnL2;
	pFull->ura = pRXN->ura;
	pFull->health = pRXN->health;
	pFull->iodc = iodc;
	pFull->L2PData = L2PData;
	pFull->TGD = TGD;
	pFull->toc = pRXN->toc;
	pFull->af2 = pRXN->af2;
	pFull->af1 = pRXN->af1;
	pFull->af0 = pRXN->af0;
	pFull->crs = pRXN->crs;
	pFull->delta_n = pRXN->delta_n;
	pFull->m0 = pRXN->m0;
	pFull->cuc = pRXN->cuc;
	pFull->e = pRXN->e;
	pFull->cus = pRXN->cus;
	pFull->sqrt_a = pRXN->sqrt_a;
	pFull->toe = pRXN->toe;
	pFull->ephem_fit = pRXN->ephem_fit;
    pFull->ure = pRXN->ure;
	pFull->AODO = AODO;
	pFull->cic = pRXN->cic;
	pFull->omega0 = pRXN->omega0;
	pFull->cis = pRXN->cis;
	pFull->i0 = pRXN->i0;
	pFull->crc = pRXN->crc;
	pFull->w = pRXN->w;
	pFull->omega_dot = pRXN->omega_dot;
	pFull->iode = pRXN->iode;
	pFull->i_dot = pRXN->i_dot;

	return;
}

void MSL_GPS_FullToRXN(RXN_FullEph_t* pFull, RXN_ephem_t* pRXN)
{
	/* Map pFull to pRXN. */
	pRXN->prn = pFull->prn;
	pRXN->gps_week = pFull->gps_week;
	pRXN->ura = pFull->ura;
	pRXN->health = pFull->health;
	pRXN->toc = pFull->toc;
	pRXN->af2 = pFull->af2;
	pRXN->af1 = pFull->af1;
	pRXN->af0 = pFull->af0;
	pRXN->crs = pFull->crs;
	pRXN->delta_n = pFull->delta_n;
	pRXN->m0 = pFull->m0;
	pRXN->cuc = pFull->cuc;
	pRXN->e = pFull->e;
	pRXN->cus = pFull->cus;
	pRXN->sqrt_a = pFull->sqrt_a;
	pRXN->toe = pFull->toe;
	pRXN->ephem_fit = pFull->ephem_fit;
    pRXN->ure = pFull->ure;
	pRXN->cic = pFull->cic;
	pRXN->omega0 = pFull->omega0;
	pRXN->cis = pFull->cis;
	pRXN->i0 = pFull->i0;
	pRXN->crc = pFull->crc;
	pRXN->w = pFull->w;
	pRXN->omega_dot = pFull->omega_dot;
	pRXN->iode = pFull->iode;
	pRXN->i_dot = pFull->i_dot;

    return;
}

void MSL_GLO_RXNToFull(RXN_glonass_ephem_t* pRXN, RXN_FullEph_GLO_t* pFull)
{
    RXN_seed_information_t seedInfo;
    U32 result = 0;
    U08 N4;
    U16 NT;
    U08 tb;

	pFull->slot			= pRXN->slot;
	pFull->FT			= pRXN->FT;
	pFull->freqChannel	= pRXN->freqChannel;
	pFull->M			= pRXN->M;
	pFull->Bn			= pRXN->Bn;
	pFull->utc_offset	= pRXN->utc_offset;
	pFull->gamma		= pRXN->gamma;
	pFull->tauN			= pRXN->tauN;
	pFull->x			= pRXN->x;
	pFull->y			= pRXN->y;
	pFull->z			= pRXN->z;
	pFull->vx			= pRXN->vx;
	pFull->vy			= pRXN->vy;
	pFull->vz			= pRXN->vz;
	pFull->lsx			= pRXN->lsx;
	pFull->lsy			= pRXN->lsy;
	pFull->lsz			= pRXN->lsz;
    pFull->gloSec		= pRXN->gloSec;

    /* En is the age of Ephemeris in days.  Since some chipsets may discard ephemeris
       that is too old, En is by default 0 so that our EE is not discarded.  The
       configuration parameter GLO_Enable_En allows En to be set. */
    pFull->En = 0;
    if ((gConfig.constel[RXN_GLO_CONSTEL].flags & MSL_CONFIG_FLAG_GLO_ENABLE_EN) == MSL_CONFIG_FLAG_GLO_ENABLE_EN)
    {
        result = RXN_Get_Seed_Information(pRXN->slot + 1, &seedInfo, RXN_GLO_CONSTEL);
        if (result == RXN_SUCCESS)
        {
            /* En is (current time - seed creation time) converted to days from seconds */
            pFull->En = (RXN_MSL_GetBestGPSTime() - seedInfo.seed_creation_time) / 86400; 
        }
    }

    /* Should always be set to 30 minutes */
    pFull->P1 = 30;               

    /* P2 is the measure of the "oddness" of tb, so first calculate tb */
    MSL_ConvertGLOSecToComponents(pRXN->gloSec, &N4, &NT, &tb);
    pFull->P2 = tb % 2;

    /* Delta Tau cannot currently be calculated, so set to 0 */
    pFull->deltaTau = 0;
}

void MSL_GLO_FullToRXN(RXN_FullEph_GLO_t* pFull,RXN_glonass_ephem_t* pRXN)
{
	pRXN->slot			= pFull->slot;
	pRXN->FT			= pFull->FT;
	pRXN->freqChannel	= pFull->freqChannel;
	pRXN->M				= pFull->M;
	pRXN->Bn			= pFull->Bn;
	pRXN->utc_offset	= pFull->utc_offset;
	pRXN->gamma			= pFull->gamma;
	pRXN->tauN			= pFull->tauN;
	pRXN->gloSec		= pFull->gloSec;
	pRXN->x				= pFull->x;
	pRXN->y				= pFull->y;
	pRXN->z				= pFull->z;
	pRXN->vx			= pFull->vx;
	pRXN->vy			= pFull->vy;
	pRXN->vz			= pFull->vz;
	pRXN->lsx			= pFull->lsx;
	pRXN->lsy			= pFull->lsy;
	pRXN->lsz			= pFull->lsz;
}

U16 MSL_SetNextHostPort()
{
  /* Search for the next non-zero port in list */
  do
  {
    ++gConfig.net.hostPortIndex;
  }
  while (gConfig.net.hostPortIndex < MSL_MAX_HOST_PORT_INDEX &&
         gConfig.net.hostPortList[gConfig.net.hostPortIndex] == 0); 

  /* if no ports left to try, reset and fail */
  if (gConfig.net.hostPortIndex == MSL_MAX_HOST_PORT_INDEX)
  {
    gConfig.net.hostPortIndex = 0;
    return RXN_FAIL;
  }

    return RXN_SUCCESS;
}

U16 MSL_GetHostPort()
{
  MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
	  "MSL_GetHostPort: Host port: %1.0f", (float) gConfig.net.hostPortList[gConfig.net.hostPortIndex]);
  return gConfig.net.hostPortList[gConfig.net.hostPortIndex];
}

static void LogHostList()
{
#define RXN_MSL_MAX_PORT_STRING_LENGTH 7	/* Max port is 65535 + 2 chars for " ," */
    char portString[RXN_MSL_MAX_PORT_STRING_LENGTH];
    char logString[MSL_MAX_LOG_ENTRY] = "RXN_MSL_Initialize: Seed_Port: ";
    int i;

  for (i = 0; i < MSL_MAX_HOST_PORT_INDEX && gConfig.net.hostPortList[i] != 0; i++)
  {
    const char* formatString = (i == 0) ? "%d" : ", %d";
    snprintf(portString, RXN_MSL_MAX_PORT_STRING_LENGTH, formatString, gConfig.net.hostPortList[i]);
    strncat(logString, portString, MSL_MAX_LOG_ENTRY);
  }

    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08, logString);
}

static void LogConstellationList()
{
  char logString[MSL_MAX_LOG_ENTRY] = "RXN_MSL_Initialize: Constel_Config: ";
  int i;

  for (i = 0; i < gConfig.numConstels; i++)
  {
    if (gConfig.constelList[i] == RXN_GPS_CONSTEL)
    {
      strncat(logString, "GPS ", MSL_MAX_LOG_ENTRY);
    }
    else if (gConfig.constelList[i] == RXN_GLO_CONSTEL)
    {
      strncat(logString, "GLONASS ", MSL_MAX_LOG_ENTRY);
    }
  }

  MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08, logString);
}

void MSL_PrintConfig(void)
{
  const char* logPath = MSL_LogGetLogPath();

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: Log_Max_Size: %1.0f", (float) MSL_LogGetMaxFilesize());

	MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: Vendor_ID: %s", gConfig.sec.vendorId);

	MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: Lic_Key_Path: %s", gConfig.sec.licKeyPath);

	MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: CL_Log_Path: %s", gConfig.CL.logPath);

	MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: MSL_Log_Path: %s", logPath);

	MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: CL_Config_Str: %s", gConfig.CL.configStr);

	MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: Pantry_Path: %s", gConfig.pantryPath);

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: TOE_Eph_Dur: %1.0f", (float) gConfig.TOE.ephDur);

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: TOE_Chip_Exp: %1.0f", (float) gConfig.TOE.chipExp);

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "RXN_MSL_Initialize: URE_Threshold: %1.0f", (float) gConfig.constel[RXN_GPS_CONSTEL].AssistanceThreshold);

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "RXN_MSL_Initialize: FT_Threshold: %1.0f", (float) gConfig.constel[RXN_GLO_CONSTEL].AssistanceThreshold);

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: System_Check_Freq: %1.0f", (float) gConfig.systemCheckFreq);	

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: TOE_Buf_Offset: %1.0f", (float) gConfig.TOE.bufOffset);

	MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: Sec_Key_Path: %s", gConfig.sec.secKeyPath);

	MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: Model_ID: %s", gConfig.sec.modelId);

	MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "RXN_MSL_Initialize: Seed_Host: %s", gConfig.net.host);

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "RXN_MSL_Initialize: Seed_Max_Host_Idx: %1.0f", (float) gConfig.net.hostMaxIdx);

    LogHostList();

    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "RXN_MSL_Initialize: GPS_Prop_Fwd: %1.0f", (float) gConfig.constel[RXN_GPS_CONSTEL].prop.PGPSFwd);

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: GPS_Seed_Update_Age: %1.0f", (float) gConfig.constel[RXN_GPS_CONSTEL].seed.updateAge);

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: GPS_Seed_Update_Age_Offset: %1.0f", (float) gConfig.constel[RXN_GPS_CONSTEL].seed.updateAgeOffset);

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: GPS_Clock_Update_Age: %1.0f", (float) gConfig.constel[RXN_GPS_CONSTEL].seed.clockUpdateAge);

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: GPS_Clock_Update_Age_Offset: %1.0f", (float) gConfig.constel[RXN_GPS_CONSTEL].seed.clockUpdateAgeOffset);

	MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: SNTP_Host: %s", gConfig.SNTP.host);

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: SNTP_Port: %1.0f", gConfig.SNTP.port);

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: Startup_Data_Wait_Duration: %1.0f", gConfig.startupDataWaitDuration);

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "RXN_MSL_Initialize: Use_System_Time: %1.0f", (float) (gConfig.flags & MSL_CONFIG_FLAG_USE_SYSTEM_TIME));

    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: Seed_Retry_Period: %1.0f", gConfig.net.downloadRetryPeriod);

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: Seed_Retry_Max: %1.0f", gConfig.net.downloadRetryMax);

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: Seed_Retry_Timer: %1.0f", gConfig.net.downloadRetryTimer);

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: Seed_Age: %1.0f", (float)gConfig.seedAge);

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: Autonomous_Segments: %1.0f", (float)gConfig.sagpsSegments);

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "RXN_MSL_Initialize: Respect_Data_Settings: %1.0f", gConfig.net.respect_Data_Settings? 1:0);

    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: SNTP_GPSTime_Uncert_Thresh: %1.0f", MSL_GetTimeUncertThresh());

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: SNTP_Retry_Max: %1.0f", gConfig.SNTP.requestRetryMax);

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: SNTP_Retry_Timer: %1.0f", gConfig.SNTP.requestRetryTimer);

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: GLO_Prop_Fwd: %1.0f", (float) gConfig.constel[RXN_GLO_CONSTEL].prop.PGPSFwd);
		
	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: GLO_Seed_Update_Age: %1.0f", (float) gConfig.constel[RXN_GLO_CONSTEL].seed.updateAge);

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: GLO_Seed_Update_Age_Offset: %1.0f", (float) gConfig.constel[RXN_GLO_CONSTEL].seed.updateAgeOffset);

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: GLO_Clock_Update_Age: %1.0f", (float) gConfig.constel[RXN_GLO_CONSTEL].seed.clockUpdateAge);

	MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
		"RXN_MSL_Initialize: GLO_Clock_Update_Age_Offset: %1.0f", (float) gConfig.constel[RXN_GLO_CONSTEL].seed.clockUpdateAgeOffset);

    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "RXN_MSL_Initialize: GLO_Enable_En: %1.0f", (float) (gConfig.constel[RXN_GLO_CONSTEL].flags & MSL_CONFIG_FLAG_GLO_ENABLE_EN));

    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "RXN_MSL_Initialize: GLO_Use_Bounded_Time: %1.0f", (float) MSL_UseBoundedGLOSec());

	LogConstellationList();

#ifdef XYBRID
    MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "RXN_MSL_Initialize: Xybrid_Host: %s", gConfig.xybrid.host);

    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "RXN_MSL_Initialize: Xybrid_Port: %1.0f", gConfig.xybrid.hostPort);

    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "RXN_MSL_Initialize: Xybrid_Max_Host_Idx: %1.0f", gConfig.xybrid.hostMaxIdx);

    MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "RXN_MSL_Initialize: Xybrid_VendorID: %s", gConfig.xybrid.vendorId);
#endif
}

void MSL_CreateHostName(
        char hostName[MSL_MAX_HOST_STR_LEN], 
        const char format[MSL_MAX_HOST_STR_LEN],
        U08 maxIndex)
{
    CHAR* delimiter;            /* Pointer to delimiter within format string */

  /* First clear hostName. */
  memset((void*) hostName, 0, sizeof(char) * MSL_MAX_HOST_STR_LEN);

  /* Get a pointer to the '%' delimiter. */
    delimiter = strchr(format, '%');

  if(delimiter != NULL)
  {
    /* Assemble hostName including %d for the server index
     * (replaces '%'). */
        strncpy(hostName, format, ((delimiter - format) / sizeof(char)));
    strcat(hostName, "%d");
    strcat(hostName, delimiter + sizeof(char));

    /* Replace %d with a random index to support load balancing between
        * servers. Index will range from 1 to maxIndex. */
        sprintf(hostName, hostName, (RXN_MSL_GetBestGPSTime() % maxIndex) + 1);
  }
  else
  {
        strncpy(hostName, format, MSL_MAX_HOST_STR_LEN);
  }
}
