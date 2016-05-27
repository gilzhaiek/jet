/*
* Copyright (c) 2007-2012 Rx Networks, Inc. All rights reserved.
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
* $LastChangedDate: 2009-06-25 13:19:02 -0700 (Thu, 25 Jun 2009) $
* $Revision: 11689 $
*************************************************************************
*
* This file contains implementation of APIs exposed within the RXN_CL_Common.h file.
* 
*/

#include <stdarg.h>   /* Req'd for va_start and va_end. */

#ifdef WINCE
#include "time_ce.h"
#endif

#include "RXN_CL_Common.h"

/***********************************************************************************/
/* Global Vars Below. **************************************************************/
/***********************************************************************************/

/*
* \brief
* Declare a static array of instance vars to track current instances.
*/
CL_instance_t gCL_Instances[CL_MAX_INSTANCES];

/*
* \brief
* Keep track of the number of instances.
*/
U08 gCL_Instance_Cnt = 0;

/*
* \brief
* Keep track of the next available handle to use.
*/
U16 gCL_Next_Instance_Handle = 1;

/*
* \brief
* Log file handle.
*/
FILE* gCL_LogFile = NULL;

/* Log severity threashold. */
U08 gCL_LogSevThreash = RXN_LOG_SEV_ERROR;

/* Log severity threashold. */
U16 gCL_LogZoneMask = 0xFFFF;

/* Flag indicating that the CL log is full. */
BOOL gCL_LogFull = FALSE;

/* Offset from internal clock - to be used when calculating GPS time.
* Whenever this values is equal to CL_INVALID_CLK_OFFSET, recievers
* should always be told that no time data is available so that they
* can use clock data from a GPS navigation msg. Otherwise - we can
* give them an invalid offset. This offset will be set from recievers
* after they have recieved clock data and passed it on, or this value
* will be set during CL initialization from a file (when available). */
S32 gCL_ClockOffset = RXN_CL_INVALID_CLK_OFFSET;

/* Path to a file that will persist the offset. */
char gCL_prstPath[RXN_CL_CONFIG_MAX_STR_LEN];

/***************************
* Utility Functions Below *
***************************/

U08 CL_FindHandle(U16 handle)
{
  U08 x; /* Counter */
  for(x=0; x<CL_MAX_INSTANCES; x++)
  {
    if(gCL_Instances[x].CL_Handle == handle)
    {
      return x;
    }
  }

  /* If no match could be found. */
  return 255;
}

void CL_ProcessLogParams(char config[RXN_CL_CONFIG_MAX_STR_LEN])
{
  S64 paramValue = 0;  /* Param value store. */
  BOOL zoneMaskSet = FALSE;

  /* External vars. Seed RXN_CL_Common.c for declarations. */
  extern U08 gCL_LogSevThreash;
  extern U16 gCL_LogZoneMask;

  /* Look for a "S:" within config with base 10 value. */
  if(CL_GetParamInt(config, "S:", 10, &paramValue) == RXN_FAIL)
  {
    /* Look for a "s:" within config with base 10 value. */
    if(CL_GetParamInt(config, "s:", 10, &paramValue) == RXN_FAIL)
    {
      /* Give up looking for severity - set to default. */
      gCL_LogSevThreash = RXN_LOG_SEV_ERROR;
    }
    else
    {
      gCL_LogSevThreash = (U08) paramValue;
    }
  }
  else
  {
    gCL_LogSevThreash = (U08) paramValue;
  }

  /* Clamp the log severity threashold if too high. */
  if(gCL_LogSevThreash > RXN_LOG_SEV_TRACE)
  {
    gCL_LogSevThreash = RXN_LOG_SEV_TRACE;
  }

  /* Look for a "Z: 0x" within config with base 16 (hex) value. */
  if(CL_GetParamInt(config, "Z:0x", 16, &paramValue) == RXN_FAIL)
  {
    /* Look for a "z: 0x" within config with base 16 (hex) value. */
    if(CL_GetParamInt(config, "z:0x", 16, &paramValue) == RXN_FAIL)
    {
      /* Look for a "Z: " within config with base 16 (hex) value. */
      if(CL_GetParamInt(config, "Z:", 16, &paramValue) == RXN_FAIL)
      {
        /* Look for a "z:" within config with base 16 (hex) value. */
        if(CL_GetParamInt(config, "z:", 16, &paramValue) == RXN_FAIL)
        {
          /* Give up looking for severity - set to default. */
          gCL_LogZoneMask = 0xFFFF;
          zoneMaskSet = TRUE;
        }
      }
    }
  }

  if (zoneMaskSet == FALSE)
  {
	/* Set the log zone mask. */
	gCL_LogZoneMask = (U16) paramValue;
  }

  CL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_FATAL, RXN_LOG_ZONE01, "CL_ProcessLogParams: Setting log severity to %1.0f, and log zone mask to %1.0f", gCL_LogSevThreash, gCL_LogZoneMask);

  return;
}

U16 CL_GetParamInt(char config[RXN_CL_CONFIG_MAX_STR_LEN],
                   char label[CL_MAX_LOG_ELEMENT], int base, S64* pValue)
{
  char* pParam = NULL;            /* Pointer to a param (includes prefix). */
  char* pEnd;                     /* End pointer as req'd by strtol(). */
  char paramChar;                 /* Pointer to the char after a param. */
  char param[CL_MAX_LOG_ELEMENT]; /* Temp string to hold the param value. */
  U08 cnt = 0;                    /* Counter. */
  U08 labelLen = 0;

  /* Clear the value. */
  *pValue = 0;

  /* Look for the label within config. */
  pParam = strstr(config, label);

  /* Could not find? */
  if(pParam == NULL)
  {
    return RXN_FAIL;
  }

  /* Get the label length. */
  labelLen = (U08) strlen(label);

  /* Look for a non-numeric char following pParam + strlen(pParam). */
  cnt = 0;
  while(TRUE)
  {
    /* Check if the currently indexed chars is numeric. */
    paramChar = *(pParam + labelLen + cnt);
    if(((base == 10) && (paramChar >= '0') && (paramChar <= '9')) ||
       ((base == 16) && (((paramChar >= '0') && (paramChar <= '9')) ||
                        ((paramChar >= 'a') && (paramChar <= 'f')) ||
                        ((paramChar >= 'A') && (paramChar <= 'F')))))
    {
      /* Check the count. Give up after CL_MAX_LOG_ELEMENT - strlen(pParam) chars. */
      if(cnt >= CL_MAX_LOG_ELEMENT - labelLen)
      {
        return RXN_FAIL;
      }
      else
      {
        cnt++;
      }
    }
    else /* Not a numeric char. */
    {
      /* Must be at least 1 char. */
      if(cnt == 0)
      {
        return RXN_FAIL;
      }
      else
      {
        /* Get the param string. */
        memset((void*) param, 0, sizeof(char) * CL_MAX_LOG_ELEMENT);
        strncpy(param, pParam + labelLen, cnt);

        /* Convert the param value using the base provided. */
        *pValue = (S64) (strtol(param, &pEnd, base));

        /* Done. */
        return RXN_SUCCESS;
      }
    }
  }
}

U16 CL_GetParamString(const char config[RXN_CL_CONFIG_MAX_STR_LEN],
                   const char label[CL_MAX_LOG_ELEMENT], char param[RXN_CL_CONFIG_MAX_STR_LEN])
{
  char* pParam = NULL;            /* Pointer to a param (includes prefix). */
  char* pSeparator = NULL;        /* Pointer to a '|' char if present. */
  U08 labelLen = 0;               /* Label length store. */
  U08 paramLen = 0;               /* Param length store. */

  /* Look for the label within config and set a pointer to follow it. */
  pParam = strstr(config, label);

  /* Could not find? */
  if(pParam == NULL)
  {
    return RXN_FAIL;
  }

  /* Get the label length. */
  labelLen = (U08) strlen(label);

  /* Update pParam to point at the parameter (following the label). */
  pParam += labelLen;

  /* Look for a '|' delimitor. after the param*/
  pSeparator = strstr(pParam, "|");

  if(pSeparator != NULL)
  {
    /* If a separator is found get the param string between the label and the delimitor. */
    paramLen = (U08) (pSeparator - pParam);
  }
  else
  {
    /* If no separator is found get the param string by subtracting the num chars to
     * the param from the total string chars. */
    paramLen = (U08) (strlen(config) - (pParam - config));
  }
  strncpy(param, pParam, paramLen);
  param[paramLen] = '\0';
  
  return RXN_SUCCESS;
}

U16 CL_PersistClkOffset(S32 offset)
{
  FILE* file;
  char offsetStr[32];

  /* Don't continue if no persistant path is provided. */
  if(gCL_prstPath[0] == 0)
  {
    return RXN_SUCCESS;
  }

  /* Open a persist store file. */
  file = fopen(gCL_prstPath, "wt");
  if(file == NULL)
  {
    CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
        "CL_PersistClkOffset: Error opening a persist file with path: %s",
        gCL_prstPath);

    return RXN_CL_OPEN_OR_CLOSE_ERR;
  }

  /* Write the offset. */
  memset((void*) offsetStr, 0, sizeof(char) * 32);
  sprintf(offsetStr, "Prst:%i\r", offset );

  CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE01,
        "CL_PersistClkOffset: Value: %1.0f", (double) offset);
  CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE01,
        "CL_PersistClkOffset: Value: %s", offsetStr);
  
  if(fwrite(offsetStr, sizeof(char), strlen(offsetStr) + 1, file) == 0)
  {
    CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
        "CL_PersistClkOffset: Error writing to a peristant file.");

    fclose(file);
    return RXN_FAIL;
  }

  /* Close the file. */
  fclose(file);

  return RXN_SUCCESS;
}

U16 CL_RetrieveClkOffset(S32* pOffset)
{
  FILE* file;
  char offsetStr[32];

  /* Don't continue if no persistant path is provided. */
  if(gCL_prstPath[0] == 0)
  {
    return RXN_SUCCESS;
  }

  /* Open a persist store file. */
  file = fopen(gCL_prstPath, "rt");
  if(file == NULL)
  {
    CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_WARNING, RXN_LOG_ZONE01,
        "CL_RetrieveClkOffset: Error opening a persist file with path: %s",
        gCL_prstPath);

    return RXN_CL_OPEN_OR_CLOSE_ERR;
  }

  /* Read the offset. */
  memset((void*) offsetStr, 0, sizeof(char) * 32);
  if(fread(offsetStr, sizeof(char), 32, file) == 0)
  {
    CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
        "CL_RetrieveClkOffset: Error reading to a peristant file.");

    fclose(file);
    return RXN_FAIL;
  }

  /* Get a pointer to the offset lable within the file. */
  if(strstr(offsetStr, "Prst:") ==  NULL)
  {
    CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE01,
          "CL_RetrieveClkOffset: Can't find the label within file contents: %s",
          offsetStr);

    fclose(file);
    return RXN_FAIL;
  }

  /* Get the data value. */
  *pOffset = (S32) atof(offsetStr + strlen("Prst:"));

  /* Close the file. */
  fclose(file);

  return RXN_SUCCESS;
}


U16 CL_OpenFile(FILE** ppFile, const char config[RXN_CL_CONFIG_MAX_STR_LEN])
{
  char path[CL_PATH_MAX_STR_LEN]; /* Extracted path from config. */
  char mode[CL_MODE_MAX_STR_LEN]; /* Extracted mode from config. */
  char* pDelimiter;               /* Ptr to the '|' delimiter. */
  U16 pathStrLen = 0;             /* Length of the path string. */
  U16 modeStrLen = 0;             /* Length of the mode string. */

  /* Clear out the path and mode arrays. */
  memset(path, 0, sizeof(char) * CL_PATH_MAX_STR_LEN);
  memset(mode, 0, sizeof(char) * CL_MODE_MAX_STR_LEN);

  /* Find the delimiter. */
  pDelimiter = strchr(config, '|');
  if(pDelimiter == NULL)
  {
    return RXN_CL_CONFIG_FORMAT_ERR;
  }

  /* Calc the length of the path string. */
  pathStrLen = (U16) (pDelimiter - config);

  /* Get the path. */
  strncpy(path, config, pathStrLen);

  /* Calc the length of the mode string.
   * (Total length - length of path - 1 for '|'). */
  modeStrLen = (U16) (strlen(config) - pathStrLen - 1);

  /* Get the mode. */
  strncpy(mode, (pDelimiter + 1), modeStrLen);

  /* Open the file. */
  *ppFile = fopen(path, mode);
  if(*ppFile == NULL)
  {
    return RXN_CL_OPEN_OR_CLOSE_ERR;
  }

  return RXN_SUCCESS;
}

U16 CL_CloseFile(FILE** ppFile)
{
  U16 result = RXN_SUCCESS; /* Result store. */

  /* Don't bother doing anything if pFile is NULL. */
  if(*ppFile == NULL)
  {
    /* Already closed. */
    return result;
  }

  /* Close the file and handle an error.*/
  if(fclose(*ppFile) != 0)
  {
    result = RXN_CL_OPEN_OR_CLOSE_ERR;
  }

  /* Reset *ppFile. */
  *ppFile = NULL;

  return result;
}

U16 CL_ReadLine(CL_Chipset_Attrib_t* pAttrib, char fileLine[CL_MAX_FILE_LINE_LEN])
{
  if(fgets(fileLine, CL_MAX_FILE_LINE_LEN, pAttrib->CL_pFile) == NULL)
  {
		/* Check for EOF. */
    if(feof(pAttrib->CL_pFile) != 0)
    {
	    /* Got to the end of file. */
	    return RXN_CL_CHIPSET_AT_EOF;
    }
    else
    {
      /* Some other error. */
      return RXN_CL_CHIPSET_READ_ERR;
    }
  }
  
  return RXN_SUCCESS;
}

U16 CL_WriteLine(CL_Chipset_Attrib_t* pAttrib, char fileLine[CL_MAX_FILE_LINE_LEN])
{
  if(fputs(fileLine, pAttrib->CL_pFile) == EOF)
  {
    return RXN_CL_CHIPSET_WRITE_ERR;
  }

  return RXN_SUCCESS;
}

U16 CL_ResetFilePtr(CL_Chipset_Attrib_t* pAttrib)
{
  fseek(pAttrib->CL_pFile, 0L, SEEK_SET);

  return RXN_SUCCESS;
}

U16 CL_OpenLog(const char logFile[RXN_CL_MAX_LOG_PATH])
{
  /* Check if a file has already been opened. */
  if(gCL_LogFile != NULL)
  {
    return RXN_CL_TOO_MANY_INSTANCES_ERR;
  }

  /* Open the log file. */
  gCL_LogFile = fopen(logFile, "w");
  if(gCL_LogFile == NULL)
  {
    return RXN_CL_OPEN_OR_CLOSE_ERR;
  }

  return RXN_SUCCESS;
}

void CL_CloseLog(void)
{
  /* If the log file is not open, simply return. */
  if(gCL_LogFile == NULL)
  {
    return;
  }

  /* Try to close the log and handle errors. */
  fclose(gCL_LogFile);

  /* Reset gCL_LogFile. */
  gCL_LogFile = NULL;

  return;
}

/*********************
* Logging Functions *
*********************/

void CL_LogSimple(const char logEntry[CL_MAX_LOG_ENTRY])
{
  char fullLogEntry[CL_MAX_LOG_ENTRY];       /* Log entry. */
  char timeEntry[CL_MAX_LOG_TIME_STR_LEN];   /* Entry time component. */

  /* Don't proceed if gCL_LogFile indicates no log opened or if the log is full. */
  if((gCL_LogFile == NULL) || (gCL_LogFull))
  {
    return;
  }

  /* Check if the log is full and provide output accordingly.
   * Set the flag so that subsequent logs don't occur. */
  if(ftell(gCL_LogFile) > (long) (CL_GetMaxLogSize() * 1024))
  {
    fputs("FILE AT CAPACITY - NO ADDITIONAL LOGGING AFTER THIS ENTRY!", gCL_LogFile);
    
    gCL_LogFull = TRUE;

    return;
  }

  /* Update the timeEntry string. */
  CL_SetupEntryTime(timeEntry);

  /* Setup the full entry. */
  sprintf(fullLogEntry, "%s %s\n", timeEntry, logEntry);

  /* Write the log entry. */
  fputs(fullLogEntry, gCL_LogFile);
  fflush(gCL_LogFile);

  return;
}

void CL_Log(BOOL includeTime, BOOL includeEndline, U08 severity, U16 zone,
                const char logEntry[CL_MAX_LOG_ENTRY])
{
  char fullLogEntry[CL_MAX_LOG_ENTRY];       /* Log entry. */
  char timeEntry[CL_MAX_LOG_TIME_STR_LEN];   /* Entry time component. */
  char prefixEntry[CL_MAX_LOG_TIME_STR_LEN]; /* Entry prefix component. */

  /* Don't proceed if gCL_LogFile indicates no log opened or if the log is full. */
  if((gCL_LogFile == NULL) || (gCL_LogFull))
  {
    return;
  }

  /* Check if the log is full and provide output accordingly.
   * Set the flag so that subsequent logs don't occur. */
  if(ftell(gCL_LogFile) > (long) (CL_GetMaxLogSize() * 1024))
  {
    fputs("FILE AT CAPACITY - NO ADDITIONAL LOGGING AFTER THIS ENTRY!", gCL_LogFile);

    gCL_LogFull = TRUE;

    return;
  }

  /* Process severity and zone values. Determines if a log entry should
   * be added and builds a prefix (if addition supported). */
  if(CL_ProcessSevZone(severity, zone, prefixEntry) != RXN_SUCCESS)
  {
    return;
  }

  /* Clear all strings. */
  memset((void*) fullLogEntry, 0, CL_MAX_LOG_ENTRY);
  memset((void*) timeEntry, 0, CL_MAX_LOG_TIME_STR_LEN);

  if(includeTime)
  {
    /* Update the timeEntry string. */
    CL_SetupEntryTime(timeEntry);

    /* Init the log entry with the time. */
    strcpy(fullLogEntry, timeEntry);

    /* Append a space. */
    strcat(fullLogEntry, " ");
  }

  /* Append the prefixEntry. */
  strcat(fullLogEntry, prefixEntry);

  /* Append (concatinate) the incoming entry. */
  strcat(fullLogEntry, logEntry);

  if(includeEndline)
  {
    /* Append (concatinate) a '\n' char. */
    strcat(fullLogEntry, "\n");
  }

  /* Write the log entry. */
  fputs(fullLogEntry, gCL_LogFile);
  fflush(gCL_LogFile);

  return;
}

void CL_LogStr(BOOL includeTime, BOOL includeEndline, U08 severity, U16 zone,
                const char logFmtEntry[CL_MAX_LOG_ENTRY], const char logElement1[CL_MAX_LOG_ELEMENT])
{
  char fullFmtEntry[CL_MAX_LOG_ENTRY];     /* Log entry format with args. */
  char fullLogEntry[CL_MAX_LOG_ENTRY];     /* Log entry. */
  char prefixEntry[CL_MAX_LOG_TIME_STR_LEN]; /* Entry prefix component. */

  /* Don't proceed if gCL_LogFile indicates no log opened or if the log is full. */
  if((gCL_LogFile == NULL) || (gCL_LogFull))
  {
    return;
  }

  /* Check if the log is full and provide output accordingly.
   * Set the flag so that subsequent logs don't occur. */
  if(ftell(gCL_LogFile) > (long) (CL_GetMaxLogSize() * 1024))
  {
    fputs("FILE AT CAPACITY - NO ADDITIONAL LOGGING AFTER THIS ENTRY!", gCL_LogFile);

    gCL_LogFull = TRUE;

    return;
  }

  /* Process severity and zone values. Determines if a log entry should
   * be added and builds a prefix (if addition supported). */
  if(CL_ProcessSevZone(severity, zone, prefixEntry) != RXN_SUCCESS)
  {
    return;
  }

  /* Clear all strings. */
  memset((void*) fullFmtEntry, 0, CL_MAX_LOG_ENTRY);
  memset((void*) fullLogEntry, 0, CL_MAX_LOG_ENTRY);

  /* Setup the full format string to include time, endlines, etc. */
  CL_SetupFullFmtString(fullFmtEntry, includeTime, includeEndline,
    prefixEntry, logFmtEntry);

  /* Setup the log entry with one string element. */
  sprintf(fullLogEntry, fullFmtEntry, logElement1);

  /* Write the log entry. */
  fputs(fullLogEntry, gCL_LogFile);
  fflush(gCL_LogFile);

  return;
}

/* Log an entry with two strings within format string.
* ("..." argument list may not be supported on some targets.)
* logFmtEntry MUST contain two '%s' chars. */
void CL_LogStrStr(BOOL includeTime, BOOL includeEndline,
                  U08 severity, U16 zone,
                  const char logFmtEntry[CL_MAX_LOG_ENTRY],
                  const char logElement1[CL_MAX_LOG_ELEMENT],
                  const char logElement2[CL_MAX_LOG_ELEMENT])
{
  char fullFmtEntry[CL_MAX_LOG_ENTRY];     /* Log entry format with args. */
  char fullLogEntry[CL_MAX_LOG_ENTRY];     /* Log entry. */
  char prefixEntry[CL_MAX_LOG_TIME_STR_LEN]; /* Entry prefix component. */

  /* Don't proceed if gCL_LogFile indicates no log opened or if the log is full. */
  if((gCL_LogFile == NULL) || (gCL_LogFull))
  {
    return;
  }

  /* Check if the log is full and provide output accordingly.
   * Set the flag so that subsequent logs don't occur. */
  if(ftell(gCL_LogFile) > (long) (CL_GetMaxLogSize() * 1024))
  {
    fputs("FILE AT CAPACITY - NO ADDITIONAL LOGGING AFTER THIS ENTRY!", gCL_LogFile);

    gCL_LogFull = TRUE;

    return;
  }

  /* Process severity and zone values. Determines if a log entry should
   * be added and builds a prefix (if addition supported). */
  if(CL_ProcessSevZone(severity, zone, prefixEntry) != RXN_SUCCESS)
  {
    return;
  }

  /* Clear all strings. */
  memset((void*) fullFmtEntry, 0, CL_MAX_LOG_ENTRY);
  memset((void*) fullLogEntry, 0, CL_MAX_LOG_ENTRY);

  /* Setup the full format string to include time, endlines, etc. */
  CL_SetupFullFmtString(fullFmtEntry, includeTime, includeEndline,
    prefixEntry, logFmtEntry);

  /* Setup the log entry with one string element. */
  sprintf(fullLogEntry, fullFmtEntry, logElement1, logElement2);

  /* Write the log entry. */
  fputs(fullLogEntry, gCL_LogFile);
  fflush(gCL_LogFile);

  return;
}

void CL_LogFlt(BOOL includeTime, BOOL includeEndline, U08 severity, U16 zone,
                const char logFmtEntry[CL_MAX_LOG_ENTRY], double logElement1)
{
  char fullFmtEntry[CL_MAX_LOG_ENTRY];     /* Log entry format with args. */
  char fullLogEntry[CL_MAX_LOG_ENTRY];     /* Log entry. */
  char prefixEntry[CL_MAX_LOG_TIME_STR_LEN]; /* Entry prefix component. */

  /* Don't proceed if gCL_LogFile indicates no log opened or if the log is full. */
  if((gCL_LogFile == NULL) || (gCL_LogFull))
  {
    return;
  }

  /* Check if the log is full and provide output accordingly.
   * Set the flag so that subsequent logs don't occur. */
  if(ftell(gCL_LogFile) > (long) (CL_GetMaxLogSize() * 1024))
  {
    fputs("FILE AT CAPACITY - NO ADDITIONAL LOGGING AFTER THIS ENTRY!", gCL_LogFile);

    gCL_LogFull = TRUE;

    return;
  }

  /* Process severity and zone values. Determines if a log entry should
   * be added and builds a prefix (if addition supported). */
  if(CL_ProcessSevZone(severity, zone, prefixEntry) != RXN_SUCCESS)
  {
    return;
  }

  /* Clear all strings. */
  memset((void*) fullFmtEntry, 0, CL_MAX_LOG_ENTRY);
  memset((void*) fullLogEntry, 0, CL_MAX_LOG_ENTRY);

  /* Setup the full format string to include time, endlines, etc. */
  CL_SetupFullFmtString(fullFmtEntry, includeTime, includeEndline,
    prefixEntry, logFmtEntry);

  /* Setup the log entry with one string element. */
  sprintf(fullLogEntry, fullFmtEntry, logElement1);

  /* Write the log entry. */
  fputs(fullLogEntry, gCL_LogFile);
  fflush(gCL_LogFile);

  return;
}

void CL_LogFltFlt(BOOL includeTime, BOOL includeEndline, U08 severity, U16 zone,
                const char logFmtEntry[CL_MAX_LOG_ENTRY],
                double logElement1, double logElement2)
{
  char fullFmtEntry[CL_MAX_LOG_ENTRY];        /* Log entry format with args. */
  char fullLogEntry[CL_MAX_LOG_ENTRY];        /* Log entry. */
  char prefixEntry[CL_MAX_LOG_TIME_STR_LEN];  /* Entry prefix component. */

  /* Don't proceed if gCL_LogFile indicates no log opened or if the log is full. */
  if((gCL_LogFile == NULL) || (gCL_LogFull))
  {
    return;
  }

  /* Check if the log is full and provide output accordingly.
   * Set the flag so that subsequent logs don't occur. */
  if(ftell(gCL_LogFile) > (long) (CL_GetMaxLogSize() * 1024))
  {
    fputs("FILE AT CAPACITY - NO ADDITIONAL LOGGING AFTER THIS ENTRY!", gCL_LogFile);

    gCL_LogFull = TRUE;

    return;
  }

  /* Process severity and zone values. Determines if a log entry should
   * be added and builds a prefix (if addition supported). */
  if(CL_ProcessSevZone(severity, zone, prefixEntry) != RXN_SUCCESS)
  {
    return;
  }

  /* Clear all strings. */
  memset((void*) fullFmtEntry, 0, CL_MAX_LOG_ENTRY);
  memset((void*) fullLogEntry, 0, CL_MAX_LOG_ENTRY);

  /* Setup the full format string to include time, endlines, etc. */
  CL_SetupFullFmtString(fullFmtEntry, includeTime, includeEndline,
    prefixEntry, logFmtEntry);

  /* Setup the log entry with one string element. */
  sprintf(fullLogEntry, fullFmtEntry, logElement1, logElement2);

  /* Write the log entry. */
  fputs(fullLogEntry, gCL_LogFile);
  fflush(gCL_LogFile);

  return;
}

void CL_LogFltFltFlt(BOOL includeTime, BOOL includeEndline, U08 severity, U16 zone,
                const char logFmtEntry[CL_MAX_LOG_ENTRY],
                double logElement1, double logElement2, double logElement3)
{
  char fullFmtEntry[CL_MAX_LOG_ENTRY];        /* Log entry format with args. */
  char fullLogEntry[CL_MAX_LOG_ENTRY];        /* Log entry. */
  char prefixEntry[CL_MAX_LOG_TIME_STR_LEN];  /* Entry prefix component. */

  /* Don't proceed if gCL_LogFile indicates no log opened or if the log is full. */
  if((gCL_LogFile == NULL) || (gCL_LogFull))
  {
    return;
  }

  /* Check if the log is full and provide output accordingly.
   * Set the flag so that subsequent logs don't occur. */
  if(ftell(gCL_LogFile) > (long) (CL_GetMaxLogSize() * 1024))
  {
    fputs("FILE AT CAPACITY - NO ADDITIONAL LOGGING AFTER THIS ENTRY!", gCL_LogFile);

    gCL_LogFull = TRUE;

    return;
  }

  /* Process severity and zone values. Determines if a log entry should
   * be added and builds a prefix (if addition supported). */
  if(CL_ProcessSevZone(severity, zone, prefixEntry) != RXN_SUCCESS)
  {
    return;
  }

  /* Clear all strings. */
  memset((void*) fullFmtEntry, 0, CL_MAX_LOG_ENTRY);
  memset((void*) fullLogEntry, 0, CL_MAX_LOG_ENTRY);

  /* Setup the full format string to include time, endlines, etc. */
  CL_SetupFullFmtString(fullFmtEntry, includeTime, includeEndline,
    prefixEntry, logFmtEntry);

  /* Setup the log entry with one string element. */
  sprintf(fullLogEntry, fullFmtEntry, logElement1, logElement2, logElement3);

  /* Write the log entry. */
  fputs(fullLogEntry, gCL_LogFile);
  fflush(gCL_LogFile);

  return;
}
void CL_ConvertECEFToLLA(RXN_ECEF_LOC_t ECEFIn, RXN_LLA_LOC_t* pLLAOut)
{
	/* Constants (WGS ellipsoid) */
    const double a = 6378137.0;
    const double e = 8.1819190842622e-2;
    const double pi = 3.1415926535897932384626433832795;

  /* Variables */
  double b, ep, p, th, lon, lat, n, alt;

	/* Simply set all LLA params to 0 of all ECEF params 0. */
	if( (ECEFIn.ECEF_X == 0) &&
		(ECEFIn.ECEF_Y == 0) &&
		 (ECEFIn.ECEF_Z == 0))
	{
		pLLAOut->Lat = 0;
		pLLAOut->Lon = 0;
		pLLAOut->Alt = 0;

		return;
	}

	/* Calcs. */
	b = sqrt((double) (pow(a, 2) * (1 - pow(e, 2))));
	ep = sqrt((double) (((pow(a, 2) - pow(b, 2))/pow(b,2))));
	p = sqrt((double) (pow((double) ECEFIn.ECEF_X, 2) + pow((double) ECEFIn.ECEF_Y, 2)));
	th = atan2(a * (double) ECEFIn.ECEF_Z, b * p);
	lon = atan2((double) ECEFIn.ECEF_Y, (double) ECEFIn.ECEF_X);
	lat = atan2(((double) ECEFIn.ECEF_Z + ep * ep * b * pow(sin(th), 3)),
		(p - e * e * a * pow(cos(th), 3)));
	n = a / sqrt(1 - e * e * pow(sin(lat), 2));
	alt = p / cos(lat) - n;
	lat = (lat * 180) / pi;
	lon = (lon * 180) / pi;

	/* Set pLLAOut. */
	pLLAOut->Lat = (R32) lat;
	pLLAOut->Lon = (R32) lon;
	pLLAOut->Alt = (R32) alt;

	return;
}

void CL_ConvertLLAToECEF(RXN_LLA_LOC_t LLAIn, RXN_ECEF_LOC_t* pECEFOut)
{
	/* Constants (WGS ellipsoid) */
  const double a = 6378137.0;
  const double e = 8.1819190842622e-2;
  const double pi = 3.1415926535897932384626433832795;

  /* Variables */
  double n, X, Y, Z;
	double lat = LLAIn.Lat;
	double lon = LLAIn.Lon;
	double alt = LLAIn.Alt;

	/* Simply set all ECEF params to 0 of all LLA params effectivly 0 (< 0.001). */
	if( (LLAIn.Lat < 0.001)  && (LLAIn.Lat > -0.001) &&
		(LLAIn.Lon < 0.001)  && (LLAIn.Lon > -0.001) &&
		(LLAIn.Alt < 0.001) && (LLAIn.Alt > -0.001))
	{
		pECEFOut->ECEF_X = 0;
		pECEFOut->ECEF_Y = 0;
		pECEFOut->ECEF_Z = 0;

		return;
	}

	/* Calcs. */
	lat = lat * (pi / 180);
	lon = lon * (pi / 180);
	n = a / sqrt((1 - pow(e, 2) * pow(sin(lat), 2)));
	X = (n + alt) * cos(lat) * cos(lon);
	Y = (n + alt) * cos(lat) * sin(lon);
	Z = (n* (1 - pow(e, 2)) + alt) * sin(lat);

	/* Set pECEFOut. */
	pECEFOut->ECEF_X = (S32) X;
	pECEFOut->ECEF_Y = (S32) Y;
	pECEFOut->ECEF_Z = (S32) Z;

	return;
}

U16 CL_ProcessSevZone(U08 severity, U16 zone, char prefixEntry[CL_MAX_LOG_TIME_STR_LEN])
{
  char severityChar;
  char zoneStr[4];

  /* If an erroneous zone value of 0 is provided, replace it with
   * RXN_LOG_ZONE_MISC. */
  if(zone == 0)
  {
    zone = RXN_LOG_ZONE01;
  }

  /* Verify the severity AND zone support a log entry being added. */
  if((severity > gCL_LogSevThreash) || ((zone & gCL_LogZoneMask) == 0))
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
      strcpy(zoneStr, "Z01");
      break;
    case RXN_LOG_ZONE02:
      strcpy(zoneStr, "Z02");
      break;
    case RXN_LOG_ZONE03:
      strcpy(zoneStr, "Z03");
      break;
    case RXN_LOG_ZONE04:
      strcpy(zoneStr, "Z04");
      break;
    case RXN_LOG_ZONE05:
      strcpy(zoneStr, "Z05");
      break;
    case RXN_LOG_ZONE06:
      strcpy(zoneStr, "Z06");
      break;
    case RXN_LOG_ZONE07:
      strcpy(zoneStr, "Z07");
      break;
    case RXN_LOG_ZONE08:
      strcpy(zoneStr, "Z08");
      break;
    case RXN_LOG_ZONE09:
      strcpy(zoneStr, "Z09");
      break;
    case RXN_LOG_ZONE10:
      strcpy(zoneStr, "Z10");
      break;
    case RXN_LOG_ZONE11:
      strcpy(zoneStr, "Z11");
      break;
    case RXN_LOG_ZONE12:
      strcpy(zoneStr, "Z12");
      break;
    case RXN_LOG_ZONE13:
      strcpy(zoneStr, "Z13");
      break;
    case RXN_LOG_ZONE14:
      strcpy(zoneStr, "Z14");
      break;
    case RXN_LOG_ZONE15:
      strcpy(zoneStr, "Z15");
      break;
    case RXN_LOG_ZONE16:
      strcpy(zoneStr, "Z16");
      break;
   default:
      strcpy(zoneStr, "Zxx");
      break;
  };


  /* Build the prefix. */
  sprintf(prefixEntry, "S:%c %s ", severityChar, zoneStr);

  return RXN_SUCCESS;
}

void CL_SetupEntryTime(char timeEntry[CL_MAX_LOG_TIME_STR_LEN])
{
    CL_time_t curUTCTime;   /* UTC or GMT time struct. */

    /* Get the current GMT time.
     * Note that the implementation of MSL_GetTime is platform specific. */
    CL_GetTime(&curUTCTime, TRUE);

    /* Build the entry with time values. */
    sprintf(timeEntry, "%d/%02d/%02d %02d:%02d:%02d.%03d",
      curUTCTime.CL_Year,
      curUTCTime.CL_Month,
      curUTCTime.CL_Day,
      curUTCTime.CL_Hour,
      curUTCTime.CL_Min,
      curUTCTime.CL_Sec,
      curUTCTime.CL_mSec);
}

void CL_SetupFullFmtString(char fullFmtEntry[CL_MAX_LOG_ENTRY],
                   BOOL includeTime, BOOL includeEndline,
                   char prefixEntry[CL_MAX_LOG_TIME_STR_LEN],
                   const char logFmtEntry[CL_MAX_LOG_ENTRY])
{
  char timeEntry[CL_MAX_LOG_TIME_STR_LEN]; /* Entry time component. */

  /* Clear the timeEntry string. */
  memset((void*) timeEntry, 0, CL_MAX_LOG_TIME_STR_LEN);

  if(includeTime)
  {
    /* Update the timeEntry string. */
    CL_SetupEntryTime(timeEntry);

    /* Init the full format string with the time. */
    strcpy(fullFmtEntry, timeEntry);

    /* Append a space. */
    strcat(fullFmtEntry, " ");
  }

  /* Append (concatinate) the incoming prefix entry. */
  strcat(fullFmtEntry, prefixEntry);

  /* Append (concatinate) the incoming format entry. */
  strcat(fullFmtEntry, logFmtEntry);

  if(includeEndline)
  {
    /* Append (concatinate) a '\n' char. */
    strcat(fullFmtEntry, "\n");
  }
}

U08 CL_UREfromURA(U08 URAIdx)
{
  switch(URAIdx)
  {
    case 0:
      return 2;
    case 1:
      return 3;
    case 2:
      return 4;
    case 3:
      return 6;
    case 4:
      return 9;
    case 5: 
      return 13;
    case 6:
      return 24;
    case 7:
      return 48;
    case 8:
      return 96;
    case 9:
      return 192;
    default:
      return 255;
  }
}

U08 CL_URAfromURE(U08 URE)
{
  if(URE < 2)
  {
    return 0;
  }
  else if(URE < 3)
  {
    return 1;
  }
  else if(URE < 4)
  {
    return 2;
  }
  else if(URE < 6)
  {
    return 3;
  }
  else if(URE < 9)
  {
    return 4;
  }
  else if(URE < 13)
  {
    return 5;
  }
  else if(URE < 24)
  {
    return 6;
  }
  else if(URE < 48)
  {
    return 7;
  }
  else if(URE < 96)
  {
    return 8;
  }
  else if(URE < 192)
  {
    return 9;
  }
  else
  {
    return 10;
  }
}


/* Get GPS time values (Year, month, ...min, sec). */
// PGPS-1220
// This function converts the input GPS time to a UTC time and output
// the value as a string. The provided GPS time is the number of 
// seconds since January 6, 1980 UTC.
//
// As the GPS time is reference to 1980, we need to add 
// SYSTEM_GPS_OFFSET because time_t is used, and time_t
// represents the total number number of seconds since 
// January 1, 1970 UTC.
//
// See:
// http://www.cplusplus.com/reference/clibrary/ctime/time_t/ 
// 
//   SYSTEM_GPS_OFFSET
//     = Number of seconds between January 6, 1980 UTC & January 1, 1970 UTC.
//     = 86400 *                  // Number of seconds in a day
//       (365 * 8 + 366 * 2 + 5)  // (Number of days between 1970 to 1980,
//                                // as 1972 and 1976 are leap years -> 366 days;
//                                // add 5 days to account for Jan 1 1980 to Jan 6 1980.
// 
// Finally, as time passes, the UTC time and GPS time drifts apart,
// therefore, need to handle the leap seconds in the calculation.
// 
// Resulting in the conversion formula:
//   GPS time = UTC time + Leap seconds, or
//   UTC time = GPS time - Leap seconds
void CL_GetUTCTime(CL_time_t* pTime, U32 gpsSeconds, U16 leapSeconds)
{
  //note that seed time is 3 or 4 hrs behind the date string
  static const double SYSTEM_GPS_OFFSET = 315964800;
#ifdef WINCE
  time_t_ce time = (time_t_ce) (gpsSeconds + SYSTEM_GPS_OFFSET - leapSeconds );
  struct tm* tstr = gmtime_ce(&time);
#else
  time_t time = (time_t) (gpsSeconds + SYSTEM_GPS_OFFSET - leapSeconds);
  struct tm* tstr = gmtime(&time);
#endif

  pTime->CL_Year = tstr->tm_year + 1900;
  pTime->CL_Month = tstr->tm_mon + 1;
  pTime->CL_Day = tstr->tm_mday;
  pTime->CL_Hour = tstr->tm_hour;
  pTime->CL_Min = tstr->tm_min;
  pTime->CL_Sec =  tstr->tm_sec;
  pTime->CL_mSec = 0;
}

