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
* $LastChangedDate: 2009-04-22 17:33:16 -0700 (Wed, 22 Apr 2009) $
* $Revision: 10292 $
*************************************************************************
*
*/
#include "RXN_MSL_Common.h"
#include "RXN_MSL_Platform.h"
#include "RXN_MSL_Log.h"

/* Module variables */
static BOOL mLogEnabled = TRUE;				/* Enable or disable MSL_Log */ 
static U32 mMaxLogFilesize = 10240;		    /* Max log filesize in kB. */
static U08 mSevThreshold = 4; 				/* Log severity threashold. */
static U16 mZoneMask = 0xFFFF; 				/* Log severity threashold. */
static char mLogPath[MSL_MAX_PATH] = "./MSL_Log.txt"; 	/* MSL Log file path store. */

/* Internal prototypes */
static U16 MSL_OpenLogFile();
static U16 MSL_ProcessSevZone(U08 severity, U16 zone, char prefixEntry[MSL_MAX_LOG_TIME_ELEMENT]);
static void MSL_SetupEntryTime(char timeEntry[MSL_MAX_LOG_TIME_ELEMENT]);
static void MSL_SetupFullFmtString(char fullFmtEntry[MSL_MAX_FORMATTED_LOG_ENTRY],
                                   BOOL includeTime, BOOL includeEndline,
                                   const char prefixEntry[MSL_MAX_LOG_TIME_ELEMENT],
                                   const char logFmtEntry[MSL_MAX_LOG_ENTRY]);

/****************************
* Initialization Functions *
****************************/

U16 MSL_Log_Init()
{
    /* Call the target - specific function that opens a file.
    * Function will return a U32 that contains some sort of
    * handle or pointer (gFile) that can be used by subsequent
    * log file access.*/

    if(mLogEnabled)
    {
        if(MSL_OpenLogFile() == RXN_SUCCESS)
        {
            /* Indicate that the log has been opened. */
            MSL_LogSimple("MSL Log opened.");
            return RXN_SUCCESS;
        }
        else
        {
            printf("MSL Log cannot be opened.\n");
            return RXN_MSL_FILE_OPEN_OR_CLOSE_ERR;				
        }
    }
    else
    {
        printf("MSL Log disabled.\n");
        return RXN_SUCCESS;
    }  
}

void MSL_Log_UnInit()
{
    /* Indicate that the log is closing. */
    MSL_LogSimple("MSL Log closed.");

    /* Call the target - specific function that closes a file. */
    MSL_CloseFile(MSL_LOG_FILE_RSRC_ID);

    return;
}

static U16 MSL_OpenLogFile()
{
    return MSL_OpenFile(MSL_LOG_FILE_RSRC_ID, mLogPath, "a");
}



/*********************
* Access Functions  *
*********************/
void MSL_LogEnable(BOOL enable)
{
    mLogEnabled = enable;
}

void MSL_LogSetMaxFilesize(U32 filesize)
{
    /* Clamp the value at min or max if below/above limits. */
    if (filesize < MSL_LOG_SIZE_MIN)
    {
        mMaxLogFilesize = MSL_LOG_SIZE_MIN;
    }
    else if (filesize > MSL_LOG_SIZE_MAX)
    {
        mMaxLogFilesize = MSL_LOG_SIZE_MAX;
    }
    else
    {
        mMaxLogFilesize = filesize;
    }
}

U32 MSL_LogGetMaxFilesize()
{
    return mMaxLogFilesize;
}

void MSL_LogSetSevThreshold(U08 threshold)
{
    /* Clamp the value at min or max if below/above limits. */
    if (threshold <= MSL_LOG_SEVERITY_MIN)
    {
        mSevThreshold = MSL_LOG_SEVERITY_MIN;
    }
    else if (threshold > MSL_LOG_SEVERITY_MAX)
    {
        mSevThreshold = MSL_LOG_SEVERITY_MAX;
    }
    else
    {
        mSevThreshold = threshold;
    }
}

U08 MSL_LogGetSevThreshold()
{
    return mSevThreshold;
}

void MSL_LogSetZoneMask(U16 zoneMask)
{
    mZoneMask = zoneMask;
}

U16 MSL_LogGetZoneMask()
{
    return mZoneMask;
}

void MSL_LogSetLogPath(char* logPath, U16 size)
{
    U16 length = (size < sizeof(mLogPath)) ? size : sizeof(mLogPath);
    memset((void*) mLogPath, 0, sizeof(char) * MSL_MAX_PATH);
    strncpy(mLogPath, logPath, length);
}

const char* MSL_LogGetLogPath()
{
    return mLogPath;
}

/*********************
* Logging Functions *
*********************/
void MSL_LogSimple(const char logEntry[MSL_MAX_LOG_ENTRY])
{
    char fullLogEntry[MSL_MAX_FULL_LOG_ENTRY];       /* Log entry. */
    char timeEntry[MSL_MAX_LOG_TIME_ELEMENT];   /* Entry time component. */

    /* Update the timeEntry string. */
    MSL_SetupEntryTime(timeEntry);

    /* Setup the full entry. */
    snprintf(fullLogEntry, MSL_MAX_LOG_ENTRY, "%s %s\n", timeEntry, logEntry);

    /* Write the entry provided. */
    if(MSL_WriteFileLine(MSL_LOG_FILE_RSRC_ID, (U08*)fullLogEntry, mMaxLogFilesize) != RXN_SUCCESS)
    {
        return;
    }

    /* Flush file buffer contents to the file. */
    MSL_FileFlush(MSL_LOG_FILE_RSRC_ID);

    return;
}

void MSL_Log(BOOL includeTime, BOOL includeEndline, U08 severity, U16 zone,
             const char logEntry[MSL_MAX_LOG_ENTRY])
{
    char fullLogEntry[MSL_MAX_FULL_LOG_ENTRY];       /* Log entry. */
    char timeEntry[MSL_MAX_LOG_TIME_ELEMENT];   /* Entry time component. */
    char prefixEntry[MSL_MAX_LOG_TIME_ELEMENT]; /* Entry prefix component. */

    /* Process severity and zone values. Determines if a log entry should
    * be added and builds a prefix (if addition supported). */
    if(MSL_ProcessSevZone(severity, zone, prefixEntry) != RXN_SUCCESS)
    {
        return;
    }

    /* Clear all strings. */
    memset((void*) fullLogEntry, 0, MSL_MAX_FULL_LOG_ENTRY);
    memset((void*) timeEntry, 0, MSL_MAX_LOG_TIME_ELEMENT);

    if(includeTime)
    {
        /* Update the timeEntry string. */
        MSL_SetupEntryTime(timeEntry);

        /* Init the log entry with the time. */
        strncpy(fullLogEntry, timeEntry, MSL_MAX_LOG_TIME_ELEMENT);

        /* Append a space. */
        strncat(fullLogEntry, " ", MSL_LOG_SPACE_STR_LEN);
    }

    /* Append the prefixEntry. */
    strncat(fullLogEntry, prefixEntry, MSL_MAX_LOG_TIME_ELEMENT);

    /* Append (concatinate) the incoming entry. */
    strncat(fullLogEntry, logEntry, MSL_MAX_LOG_ENTRY);

    if(includeEndline)
    {
        /* Append (concatinate) a '\n' char. */
        strncat(fullLogEntry, "\n", MSL_LOG_END_LINE_STR_LEN);
    }

    /* Write the log entry. */
    if(MSL_WriteFileLine(MSL_LOG_FILE_RSRC_ID, (U08*)fullLogEntry, mMaxLogFilesize) != RXN_SUCCESS)
    {
        return;
    }

    /* Flush file buffer contents to the file. */
    MSL_FileFlush(MSL_LOG_FILE_RSRC_ID);

    return;
}

void MSL_LogStr(BOOL includeTime, BOOL includeEndline, U08 severity, U16 zone,
                const char logFmtEntry[MSL_MAX_LOG_ENTRY], 
                const char logElement1[MSL_MAX_LOG_ELEMENT])
{
    char fullFmtEntry[MSL_MAX_FORMATTED_LOG_ENTRY];         /* Log entry format with args. */
    char fullLogEntry[MSL_MAX_FULL_LOG_ENTRY];    /* Log entry. */
    char prefixEntry[MSL_MAX_LOG_TIME_ELEMENT];   /* Entry prefix component. */

    /* Process severity and zone values. Determines if a log entry should
    * be added and builds a prefix (if addition supported). */
    if(MSL_ProcessSevZone(severity, zone, prefixEntry) != RXN_SUCCESS)
    {
        return;
    }

    /* Clear all strings. */
    memset((void*) fullFmtEntry, 0, MSL_MAX_FORMATTED_LOG_ENTRY);
    memset((void*) fullLogEntry, 0, MSL_MAX_FULL_LOG_ENTRY);

    /* Setup the full format string to include time, endlines, etc. */
    MSL_SetupFullFmtString(fullFmtEntry, includeTime, includeEndline,
        prefixEntry, logFmtEntry);

    /* Setup the log entry with one string element. */
    snprintf(fullLogEntry, MSL_MAX_FULL_LOG_ENTRY, fullFmtEntry, logElement1);

    /* Write the log entry. */
    if(MSL_WriteFileLine(MSL_LOG_FILE_RSRC_ID, (U08*)fullLogEntry, mMaxLogFilesize) != RXN_SUCCESS)
    {
        return;
    }

    /* Flush file buffer contents to the file. */
    MSL_FileFlush(MSL_LOG_FILE_RSRC_ID);

    return;
}

/* Log an entry with two strings within format string.
* ("..." argument list may not be supported on some targets.)
* logFmtEntry MUST contain two '%s' chars. */
void MSL_LogStrStr(BOOL includeTime, BOOL includeEndline,
                   U08 severity, U16 zone,
                   const char logFmtEntry[MSL_MAX_LOG_ENTRY],
                   const char logElement1[MSL_MAX_LOG_ELEMENT],
                   const char logElement2[MSL_MAX_LOG_ELEMENT])
{
    char fullFmtEntry[MSL_MAX_FORMATTED_LOG_ENTRY];     /* Log entry format with args. */
    char fullLogEntry[MSL_MAX_FULL_LOG_ENTRY];     /* Log entry. */
    char prefixEntry[MSL_MAX_LOG_TIME_ELEMENT]; /* Entry prefix component. */

    /* Process severity and zone values. Determines if a log entry should
    * be added and builds a prefix (if addition supported). */
    if(MSL_ProcessSevZone(severity, zone, prefixEntry) != RXN_SUCCESS)
    {
        return;
    }

    /* Clear all strings. */
    memset((void*) fullFmtEntry, 0, MSL_MAX_FORMATTED_LOG_ENTRY);
    memset((void*) fullLogEntry, 0, MSL_MAX_FULL_LOG_ENTRY);

    /* Setup the full format string to include time, endlines, etc. */
    MSL_SetupFullFmtString(fullFmtEntry, includeTime, includeEndline,
        prefixEntry, logFmtEntry);

    /* Setup the log entry with one string element. */
    snprintf(fullLogEntry, MSL_MAX_FULL_LOG_ENTRY, fullFmtEntry, logElement1, logElement2);

    /* Write the log entry. */
    if(MSL_WriteFileLine(MSL_LOG_FILE_RSRC_ID, (U08*)fullLogEntry, mMaxLogFilesize) != RXN_SUCCESS)
    {
        return;
    }

    /* Flush file buffer contents to the file. */
    MSL_FileFlush(MSL_LOG_FILE_RSRC_ID);

    return;
}

void MSL_LogFlt(BOOL includeTime, BOOL includeEndline, U08 severity, U16 zone,
                const char logFmtEntry[MSL_MAX_LOG_ENTRY], double logElement1)
{
    char fullFmtEntry[MSL_MAX_FORMATTED_LOG_ENTRY];     /* Log entry format with args. */
    char fullLogEntry[MSL_MAX_FULL_LOG_ENTRY];     /* Log entry. */
    char prefixEntry[MSL_MAX_LOG_TIME_ELEMENT]; /* Entry prefix component. */

    /* Process severity and zone values. Determines if a log entry should
    * be added and builds a prefix (if addition supported). */
    if(MSL_ProcessSevZone(severity, zone, prefixEntry) != RXN_SUCCESS)
    {
        return;
    }

    /* Clear all strings. */
    memset((void*) fullFmtEntry, 0, MSL_MAX_FORMATTED_LOG_ENTRY);
    memset((void*) fullLogEntry, 0, MSL_MAX_FULL_LOG_ENTRY);

    /* Setup the full format string to include time, endlines, etc. */
    MSL_SetupFullFmtString(fullFmtEntry, includeTime, includeEndline,
        prefixEntry, logFmtEntry);

    /* Setup the log entry with one string element. */
    snprintf(fullLogEntry, MSL_MAX_FULL_LOG_ENTRY, fullFmtEntry, logElement1);

    /* Write the log entry. */
    if(MSL_WriteFileLine(MSL_LOG_FILE_RSRC_ID, (U08*)fullLogEntry, mMaxLogFilesize) != RXN_SUCCESS)
    {
        return;
    }

    /* Flush file buffer contents to the file. */
    MSL_FileFlush(MSL_LOG_FILE_RSRC_ID);

    return;
}

void MSL_LogFltFlt(BOOL includeTime, BOOL includeEndline, U08 severity, U16 zone,
                   const char logFmtEntry[MSL_MAX_LOG_ENTRY],
                   double logElement1, double logElement2)
{
    char fullFmtEntry[MSL_MAX_FORMATTED_LOG_ENTRY];     /* Log entry format with args. */
    char fullLogEntry[MSL_MAX_FULL_LOG_ENTRY];     /* Log entry. */
    char prefixEntry[MSL_MAX_LOG_TIME_ELEMENT]; /* Entry prefix component. */

    /* Process severity and zone values. Determines if a log entry should
    * be added and builds a prefix (if addition supported). */
    if(MSL_ProcessSevZone(severity, zone, prefixEntry) != RXN_SUCCESS)
    {
        return;
    }

    /* Clear all strings. */
    memset((void*) fullFmtEntry, 0, MSL_MAX_FORMATTED_LOG_ENTRY);
    memset((void*) fullLogEntry, 0, MSL_MAX_FULL_LOG_ENTRY);

    /* Setup the full format string to include time, endlines, etc. */
    MSL_SetupFullFmtString(fullFmtEntry, includeTime, includeEndline,
        prefixEntry, logFmtEntry);

    /* Setup the log entry with one string element. */
    snprintf(fullLogEntry, MSL_MAX_FULL_LOG_ENTRY, fullFmtEntry, logElement1, logElement2);

    /* Write the log entry. */
    if(MSL_WriteFileLine(MSL_LOG_FILE_RSRC_ID, (U08*)fullLogEntry, mMaxLogFilesize) != RXN_SUCCESS)
    {
        return;
    }

    /* Flush file buffer contents to the file. */
    MSL_FileFlush(MSL_LOG_FILE_RSRC_ID);

    return;
}

void MSL_LogGPSTime(char logEntry[MSL_SHORT_LOG_ENTRY], U32 gpsTime)
{
    char logFullEntry[MSL_MAX_LOG_ENTRY];
    /* Clear all strings. */
    memset((void*) logFullEntry, 0, MSL_MAX_LOG_ENTRY);

    snprintf(logFullEntry, MSL_MAX_LOG_ENTRY, "%s %1.0f <=> %1.0f | %1.0f", logEntry, (double) gpsTime, (double) (gpsTime / 604800),(double) (gpsTime % 604800));
    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08, logFullEntry);

}

static U16 MSL_ProcessSevZone(U08 severity, U16 zone, char prefixEntry[MSL_MAX_LOG_TIME_ELEMENT])
{
#define MSL_LOG_ZONE_SIZE 4
    char severityChar;
    char zoneStr[MSL_LOG_ZONE_SIZE];

    /* If an erroneous zone value of 0 is provided, replace it with
    * RXN_LOG_ZONE_MISC. */
    if(zone == 0)
    {
        zone = RXN_LOG_ZONE08;
    }

    /* Verify the severity AND zone support a log entry being added. */
    if((severity > mSevThreshold) || ((zone & mZoneMask) == 0))
    {
        return RXN_FAIL;
    }

    /* Set severityChar based on severity. */
    switch(severity)
    {
    case RXN_LOG_SEV_FATAL:
        severityChar = 'F';
        break;
    case RXN_LOG_SEV_ERROR:
        severityChar = 'E';
        break;
    case RXN_LOG_SEV_WARNING:
        severityChar = 'W';
        break;
    case RXN_LOG_SEV_INFO:
        severityChar = 'I';
        break;
    case RXN_LOG_SEV_TRACE:
        severityChar = 'T';
        break;
    default:
        severityChar = '_';
        break;
    };

    /* Set zoneStr based on zone. */
    switch(zone)
    {
    case RXN_LOG_ZONE01:
        strncpy(zoneStr, "Z01", MSL_LOG_ZONE_SIZE);
        break;
    case RXN_LOG_ZONE02:
        strncpy(zoneStr, "Z02", MSL_LOG_ZONE_SIZE);
        break;
    case RXN_LOG_ZONE03:
        strncpy(zoneStr, "Z03", MSL_LOG_ZONE_SIZE);
        break;
    case RXN_LOG_ZONE04:
        strncpy(zoneStr, "Z04", MSL_LOG_ZONE_SIZE);
        break;
    case RXN_LOG_ZONE05:
        strncpy(zoneStr, "Z05", MSL_LOG_ZONE_SIZE);
        break;
    case RXN_LOG_ZONE06:
        strncpy(zoneStr, "Z06", MSL_LOG_ZONE_SIZE);
        break;
    case RXN_LOG_ZONE07:
        strncpy(zoneStr, "Z07", MSL_LOG_ZONE_SIZE);
        break;
    case RXN_LOG_ZONE08:
        strncpy(zoneStr, "Z08", MSL_LOG_ZONE_SIZE);
        break;
    case RXN_LOG_ZONE09:
        strncpy(zoneStr, "Z09", MSL_LOG_ZONE_SIZE);
        break;
    case RXN_LOG_ZONE10:
        strncpy(zoneStr, "Z10", MSL_LOG_ZONE_SIZE);
        break;
    case RXN_LOG_ZONE11:
        strncpy(zoneStr, "Z11", MSL_LOG_ZONE_SIZE);
        break;
    case RXN_LOG_ZONE12:
        strncpy(zoneStr, "Z12", MSL_LOG_ZONE_SIZE);
        break;
    case RXN_LOG_ZONE13:
        strncpy(zoneStr, "Z13", MSL_LOG_ZONE_SIZE);
        break;
    case RXN_LOG_ZONE14:
        strncpy(zoneStr, "Z14", MSL_LOG_ZONE_SIZE);
        break;
    case RXN_LOG_ZONE15:
        strncpy(zoneStr, "Z15", MSL_LOG_ZONE_SIZE);
        break;
    case RXN_LOG_ZONE16:
        strncpy(zoneStr, "Z16", MSL_LOG_ZONE_SIZE);
        break;
    default:
        strncpy(zoneStr, "Zxx", MSL_LOG_ZONE_SIZE);
        break;
    };


    /* Build the prefix. */
    snprintf(prefixEntry, MSL_MAX_LOG_TIME_ELEMENT, "S:%c %s ", severityChar, zoneStr);

    return RXN_SUCCESS;
}

static void MSL_SetupEntryTime(char timeEntry[MSL_MAX_LOG_TIME_ELEMENT])
{
    MSL_time_t curUTCTime;   /* UTC or GMT time struct. */

    /* Get the current GMT time.
    * Note that the implementation of MSL_GetLogTime is platform specific. */
    MSL_GetLogTime(&curUTCTime, TRUE);

    /* Build the entry with time values. */
    snprintf(timeEntry, MSL_MAX_LOG_TIME_ELEMENT, "%d/%02d/%02d %02d:%02d:%02d.%03d",
        curUTCTime.MSL_Year,
        curUTCTime.MSL_Month,
        curUTCTime.MSL_Day,
        curUTCTime.MSL_Hour,
        curUTCTime.MSL_Min,
        curUTCTime.MSL_Sec,
        curUTCTime.MSL_mSec);
}

static void MSL_SetupFullFmtString(char fullFmtEntry[MSL_MAX_FORMATTED_LOG_ENTRY],
                                   BOOL includeTime, BOOL includeEndline,
                                   const char prefixEntry[MSL_MAX_LOG_TIME_ELEMENT],
                                   const char logFmtEntry[MSL_MAX_LOG_ENTRY])
{
    char timeEntry[MSL_MAX_LOG_TIME_ELEMENT]; /* Entry time component. */

    /* Clear the timeEntry string. */
    memset((void*) timeEntry, 0, MSL_MAX_LOG_TIME_ELEMENT);

    if(includeTime)
    {
        /* Update the timeEntry string. */
        MSL_SetupEntryTime(timeEntry);

        /* Init the full format string with the time. */
        strncpy(fullFmtEntry, timeEntry, MSL_MAX_LOG_TIME_ELEMENT);

        /* Append a space. */
        strncat(fullFmtEntry, " ", MSL_LOG_SPACE_STR_LEN);
    }

    /* Append (concatinate) the incoming prefix entry. */
    strncat(fullFmtEntry, prefixEntry, MSL_MAX_LOG_TIME_ELEMENT);

    /* Append (concatinate) the incoming format entry. */
    strncat(fullFmtEntry, logFmtEntry, MSL_MAX_LOG_ENTRY);

    if(includeEndline)
    {
        /* Append (concatinate) a '\n\r' char. */
        strncat(fullFmtEntry, "\n", MSL_LOG_END_LINE_STR_LEN);
    }
}
