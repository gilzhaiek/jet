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
* $LastChangedDate: 2009-06-25 13:19:37 -0700 (Thu, 25 Jun 2009) $
* $Revision: 11690 $
*************************************************************************
*
* This file contains implementation of common APIs exposed within the RXN_CL.h file.
* 
*/

#include "RXN_CL_Common.h"  /* Internal common declarations. */

static const U08 numOfPRNs[2] =
{
    RXN_CONSTANT_NUM_GPS_PRNS,
    RXN_CONSTANT_NUM_GLONASS_PRNS
};

U16 RXN_CL_Get_API_Version(char version[RXN_CONSTANT_VERSION_STRING_LENGTH])
{
  sprintf(version, "%s", RXN_CL_VER);
  
  return RXN_SUCCESS;
}

U16 RXN_CL_Get_Chipset(char chipset[RXN_CL_CHIP_MAX_STR_LEN])
{
  /* Call the chipset specific implementation. */
  return CL_GetChipset(chipset);
}

U16 RXN_CL_Initialize(U16 chipsetVer, char config[RXN_CL_CONFIG_MAX_STR_LEN], U16* phandle)
{
  U16 result = RXN_SUCCESS;               /* Fcn call result store. */
  char zoneBitmask[8];                    /* For print of the logging debug zone bitmask. */
  char chipset[RXN_CL_CHIP_MAX_STR_LEN];  /* Chipset label store. */

  /* See RXN_CL_Common.c for global declaraions. */
  extern CL_instance_t gCL_Instances[];
  extern U08 gCL_Instance_Cnt;
  extern U16 gCL_Next_Instance_Handle;
  extern U08 gCL_LogSevThreash;
  extern U16 gCL_LogZoneMask;
  extern char gCL_prstPath[];
  extern S32 gCL_ClockOffset;

  /* If this is the first initialization (i.e. the first CL instance),
   * clear the instances array. */
  if(gCL_Instance_Cnt == 0)
  {
    memset(&gCL_Instances, 0, (sizeof(CL_instance_t) * CL_MAX_INSTANCES));
  }

  /* Validate that another instance is ok. */
  if(gCL_Instance_Cnt >= CL_MAX_INSTANCES)
  {
    return RXN_CL_TOO_MANY_INSTANCES_ERR;
  }

  /* Set chipset-specific function pointers. Clear all pointers out first
   * so that chipset-specifc CL_SetFcns implementations only have to set
   * pointers for functions that will be used and will not have to clear
   * pointers for unused functions. */
  memset((void*) &(gCL_Instances[gCL_Instance_Cnt].CL_Fcns), 0, sizeof(CL_Chipset_Fcn_Table_t));
  CL_SetFcns(&(gCL_Instances[gCL_Instance_Cnt]));

  /* Add a new instance to gCL_Instances. */
  gCL_Instances[gCL_Instance_Cnt].CL_Handle = gCL_Next_Instance_Handle;

  /* Set the handle. */
  *phandle = gCL_Next_Instance_Handle;

  /* Get log parameters such as severity threashold and zone mask before continuing to
   * ensure that only appropriate log entries are processed. */
  CL_ProcessLogParams(config);

  /* Get the persist data file path using the label: "Prst:". */
  memset((void*) gCL_prstPath, 0, sizeof(char) * RXN_CL_CONFIG_MAX_STR_LEN);
  if(CL_GetParamString(config, "Prst:", gCL_prstPath) != RXN_SUCCESS)
  {
    CL_Log(TRUE, TRUE, RXN_LOG_SEV_WARNING, RXN_LOG_ZONE03,
      "RXN_CL_Initialize: Unable to find an persistant file store path within the config string provided. No clock offset persist.");
  }
  else
  {
    /* Init the offset value. */
    if(CL_RetrieveClkOffset(&gCL_ClockOffset) != RXN_SUCCESS)
    {
      CL_Log(TRUE, TRUE, RXN_LOG_SEV_WARNING, RXN_LOG_ZONE03,
        "RXN_CL_Initialize: Unable to retrieve a clock offset from the persistant store. Will use default.");
    }
    else
    {
      CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE03,
              "RXN_CL_Initialize: Read a clock offset value of: %1.0f",
              (double) gCL_ClockOffset);
    }
  }

  /* If the config string looks like the default, report an error an exit. */
  if (strstr(config, "RINEX_OUT.txt") != NULL)
  {
      CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE03,
              "RXN_CL_Initialize: CL_Config_Str is not correct for this chipset (RINEX default used).");
      CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE03,
              "RXN_CL_Initialize: CL_Config_Str is %s", config);
      return RXN_CL_CONFIG_FORMAT_ERR;
  }

  /* Call the chipset specific implementation. */
  if(gCL_Instances[gCL_Instance_Cnt].CL_Fcns.Initialize != NULL)
  {
    result = gCL_Instances[gCL_Instance_Cnt].CL_Fcns.Initialize(&(gCL_Instances[gCL_Instance_Cnt].CL_Attrib),
                                                          chipsetVer, config);
  }
  else
  {
    return RXN_CL_NO_CHIPSET_SUPPORT_ERR;
  }

  /* Increment the counter and the next handle. */
  gCL_Next_Instance_Handle++;
  gCL_Instance_Cnt++;

  /* Get the chipset label for subsequent logging. */
  CL_GetChipset(chipset);

  /* If the call to Initialize() wasn't successful - un-init. */
  if(result != RXN_SUCCESS)
  {
    /* Log an error. */
    CL_LogFlt( TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE03,
            "RXN_CL_Initialize: Error (%1.0f) initializing a chipset. Will Un-Init.",
            (double) result);
    CL_LogStr( TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE03, "RXN_CL_Initialize: Error for chipset: %s.", chipset);

    /* Un-Init for cleanup. */
    RXN_CL_Uninitialize(*phandle);

    return result;
  }

  /* Log the init. */
  CL_LogStr( TRUE, TRUE,  RXN_LOG_SEV_INFO, RXN_LOG_ZONE03,
          "RXN_CL_Initialize: Initialized CL for chipset (%s)",
          chipset);
  CL_LogFlt( TRUE, TRUE,  RXN_LOG_SEV_INFO, RXN_LOG_ZONE03,
          "RXN_CL_Initialize: Will log for severity levels 0 - %1.0f",
          (double) gCL_LogSevThreash);
  sprintf(zoneBitmask, "0x%4X", gCL_LogZoneMask);
  CL_LogStr( TRUE, TRUE,  RXN_LOG_SEV_INFO, RXN_LOG_ZONE03,
    "RXN_CL_Initialize: Will log using debug zone bitmask %s", zoneBitmask);

  return result;
}

U16 RXN_CL_Uninitialize(U16 handle)
{
  U08 y;                  /* Counter. */
  U08 handleIdx;          /* Index to the handle within gCL_Instances. */
  U16 result = RXN_FAIL;  /* RXN_CL_Uninitialize result. */
  
  /* See RXN_CL_Common.c for global declaraions. */
  extern CL_instance_t gCL_Instances[];
  extern U08 gCL_Instance_Cnt;

  /* Look for the handle within gCL_Instances. */
  handleIdx = CL_FindHandle(handle);

  /* Handle the case where it isn't found. */
  if(handleIdx == 255)
  {
    return RXN_CL_INSTANCE_NOT_FOUND_ERR;
  }

  /* Call the chipset specific implementation. */
  if(gCL_Instances[handleIdx].CL_Fcns.Uninitialize != NULL)
  {
    result = gCL_Instances[handleIdx].CL_Fcns.Uninitialize(&(gCL_Instances[handleIdx].CL_Attrib));
  }
  else
  {
    return RXN_CL_NO_CHIPSET_SUPPORT_ERR;
  }

  /*  Shuffle down any instances that follow in the
      array to replace this instance. */
  for(y=handleIdx+1; y<CL_MAX_INSTANCES; y++)
  {
    gCL_Instances[y-1] = gCL_Instances[y];
  }

  /* Reduce the instance count. */
  gCL_Instance_Cnt--;

  /* Log the init. */
  CL_LogFlt(TRUE, TRUE,  RXN_LOG_SEV_INFO, 4, "RXN_CL_Uninitialize: Uninitialized CL for instance (%1.0f)",
    (double) gCL_Instance_Cnt+1);

  /* Handle chipset init result. */
  if(result != RXN_SUCCESS)
  {
    return result;
  }

  /* No Error. */
  return RXN_SUCCESS;
}

U16 RXN_CL_Log_Init(const char logFile[RXN_CL_MAX_LOG_PATH])
{
  U16 result;   /* CL_OpenLog result. */

  /* Open the log. */
  result = CL_OpenLog(logFile);

  /* Indicate that the log has been opened. */
  CL_LogSimple("Opened the CL Log.");
  CL_LogSimple("Until RXN_CL_Initialize is called specifying a different severity threashold...");
  CL_LogSimple("...only log entries specifying severity RXN_LOG_SEV_FATAL or RXN_LOG_SEV_ERROR will be logged.");

  return result;
}

void RXN_CL_Log_UnInit(void)
{
  /* Indicate that the log is closing. */
  CL_LogSimple("Closing the CL Log.");

  /* Close the log. */
  CL_CloseLog();

  return;
}

U16 RXN_CL_Get_Chipset_Support_Version(U16 handle, char version[RXN_CONSTANT_VERSION_STRING_LENGTH])
{
  U08 handleIdx;          /* Index to the handle within gCL_Instances. */
  U16 result = RXN_FAIL;  /* CL_GetChipsetVer result. */
  
  /* See RXN_CL_Common.c for global declaraions. */
  extern CL_instance_t gCL_Instances[];

  /* Look for the handle within gCL_Instances. */
  handleIdx = CL_FindHandle(handle);

  /* Handle the case where it isn't found. */
  if(handleIdx == 255)
  {
    return RXN_CL_INSTANCE_NOT_FOUND_ERR;
  }

  /* Call the chipset specific implementation. */
  if(gCL_Instances[handleIdx].CL_Fcns.GetChipsetSupportVersion != NULL)
  {
    result = gCL_Instances[handleIdx].CL_Fcns.GetChipsetSupportVersion(&(gCL_Instances[handleIdx].CL_Attrib), version);
  }
  else
  {
    return RXN_CL_NO_CHIPSET_SUPPORT_ERR;
  }

  return result;
}

U16 RXN_CL_Work(U16 handle)
{
  U08 handleIdx;          /* Index to the handle within gCL_Instances. */
  U16 result = RXN_FAIL;  /* CL_GetChipsetVer result. */
  
  /* See RXN_CL_Common.c for global declaraions. */
  extern CL_instance_t gCL_Instances[];

  /* Look for the handle within gCL_Instances. */
  handleIdx = CL_FindHandle(handle);

  /* Handle the case where it isn't found. */
  if(handleIdx == 255)
  {
    return RXN_CL_INSTANCE_NOT_FOUND_ERR;
  }

  /* Call the chipset specific implementation. */
  if(gCL_Instances[handleIdx].CL_Fcns.Work != NULL)
  {
    result = gCL_Instances[handleIdx].CL_Fcns.Work(&(gCL_Instances[handleIdx].CL_Attrib));
  }
  else
  {
    return RXN_CL_NO_CHIPSET_SUPPORT_ERR;
  }

  return result;
}

U16 RXN_CL_Restart(U16 handle, RXN_CL_Restarts_t restartType, RXN_RefLocation_t* pRefLoc, RXN_RefTime_t* pRefTime)
{
  U08 handleIdx;          /* Index to the handle within gCL_Instances. */
  U16 result = RXN_FAIL;  /* CL_ReadEphemeris result. */
  char LocStr[256];       /* Loc string for trace log. */
  char TimeStr[256];      /* Time string for trace log. */
  
  /* See RXN_CL_Common.c for global declaraions. */
  extern CL_instance_t gCL_Instances[];

  /* Look for the handle within gCL_Instances. */
  handleIdx = CL_FindHandle(handle);

  /* Handle the case where it isn't found. */
  if(handleIdx == 255)
  {
    return RXN_CL_INSTANCE_NOT_FOUND_ERR;
  }

  /* If pRefLoc is specified with only one format, convert to get the other. */
  if(pRefLoc != NULL)
  {
    if(pRefLoc->type == RXN_LOC_ECEF)
    {
      CL_ConvertECEFToLLA(pRefLoc->ECEF, &(pRefLoc->LLA));
      pRefLoc->type = RXN_LOC_BOTH;
    }
    else if(pRefLoc->type == RXN_LOC_LLA)
    {
      CL_ConvertLLAToECEF(pRefLoc->LLA, &(pRefLoc->ECEF));
      pRefLoc->type = RXN_LOC_BOTH;
    }

    /* Setup a string to store LLA. */
    sprintf(LocStr, "Set location LLA: %1.6f %1.6f %1.2f",
      pRefLoc->LLA.Lat, pRefLoc->LLA.Lon, pRefLoc->LLA.Alt);
  }
  else
  {
    /* Setup a string to store LLA. */
    sprintf(LocStr, "No location set.");
  }

  /* Setup a string to store time. */
  if(pRefTime == NULL)
  {
    sprintf(TimeStr, "No time set.");
  }
  else
  {
    /* Setup a string to store LLA. */
    sprintf(TimeStr, "Set time WN|TOW: %d|%d",
      pRefTime->weekNum, (pRefTime->TOWmSec / 1000));
  }

  /* Trace log. */
  CL_LogStrStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE01,
      "RXN_CL_Restart: %s. %s.", LocStr, TimeStr);

  /* Call the chipset specific implementation. */
  if(gCL_Instances[handleIdx].CL_Fcns.Restart != NULL)
  {
    result = gCL_Instances[handleIdx].CL_Fcns.Restart(&(gCL_Instances[handleIdx].CL_Attrib),
                restartType, pRefLoc, pRefTime);
  }
  else
  {
    return RXN_CL_NO_CHIPSET_SUPPORT_ERR;
  }

  return result;
}

U16 RXN_CL_SetRefLocTime(U16 handle, RXN_RefLocation_t* pRefLoc, RXN_RefTime_t* pRefTime)
{
  U08 handleIdx;          /* Index to the handle within gCL_Instances. */
  U16 result = RXN_FAIL;  /* CL_ReadEphemeris result. */
  char LocStr[256];       /* Loc string for trace log. */
  char TimeStr[256];      /* Time string for trace log. */
  
  /* See RXN_CL_Common.c for global declaraions. */
  extern CL_instance_t gCL_Instances[];

  /* Look for the handle within gCL_Instances. */
  handleIdx = CL_FindHandle(handle);

  /* Handle the case where it isn't found. */
  if(handleIdx == 255)
  {
    return RXN_CL_INSTANCE_NOT_FOUND_ERR;
  }

  /* If pRefLoc is specified with only one format, convert to get the other. */
  if(pRefLoc != NULL)
  {
    if(pRefLoc->type == RXN_LOC_ECEF)
    {
      CL_ConvertECEFToLLA(pRefLoc->ECEF, &(pRefLoc->LLA));
      pRefLoc->type = RXN_LOC_BOTH;
    }
    else if(pRefLoc->type == RXN_LOC_LLA)
    {
      CL_ConvertLLAToECEF(pRefLoc->LLA, &(pRefLoc->ECEF));
      pRefLoc->type = RXN_LOC_BOTH;
    }

    /* Setup a string to store LLA. */
    sprintf(LocStr, "Set location LLA: %1.6f %1.6f %1.2f",
      pRefLoc->LLA.Lat, pRefLoc->LLA.Lon, pRefLoc->LLA.Alt);
  }
  else
  {
    /* Setup a string to store LLA. */
    sprintf(LocStr, "No location set.");
  }

  /* Setup a string to store time. */
  if(pRefTime == NULL)
  {
    sprintf(TimeStr, "No time set.");
  }
  else
  {
    /* Setup a string to store LLA. */
    sprintf(TimeStr, "Set time WN|TOW: %d|%d",
      pRefTime->weekNum, (pRefTime->TOWmSec / 1000));
  }

  /* Trace log. */
  CL_LogStrStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE01,
        "RXN_CL_SetRefLocTime: %s. %s.", LocStr, TimeStr);

  /* Call the chipset specific implementation. */
  if(gCL_Instances[handleIdx].CL_Fcns.SetRefPosTime != NULL)
  {
    result = gCL_Instances[handleIdx].CL_Fcns.SetRefPosTime(&(gCL_Instances[handleIdx].CL_Attrib),
      pRefLoc, pRefTime);
  }
  else
  {
    return RXN_CL_NO_CHIPSET_SUPPORT_ERR;
  }

  return result;
}

U16 RXN_CL_ReadEphemeris(U16 handle, RXN_MSL_NavDataList_t* pNavDataList, RXN_constel_t constel)
{
  U08 handleIdx;          /* Index to the handle within gCL_Instances. */
  U16 result = RXN_FAIL;  /* CL_ReadEphemeris result. */
  U08 index;
  
  /* See RXN_CL_Common.c for global declaraions. */
  extern CL_instance_t gCL_Instances[];

  /* Look for the handle within gCL_Instances. */
  handleIdx = CL_FindHandle(handle);

  /* Handle the case where it isn't found. */
  if(handleIdx == 255)
  {
    return RXN_CL_INSTANCE_NOT_FOUND_ERR;
  }

  /* Verify that applicable storage is available. */
  if (pNavDataList->numEntries == 0 || pNavDataList->numEntries > numOfPRNs[constel])
  {
    return RXN_CL_ALLOCATION_ERROR;
  }

  /* Call the chipset specific implementation. */
  if(gCL_Instances[handleIdx].CL_Fcns.ReadEphemeris != NULL)
  {
        result = gCL_Instances[handleIdx].CL_Fcns.ReadEphemeris(&(gCL_Instances[handleIdx].CL_Attrib), pNavDataList, constel);
  }
  else
  {
    return RXN_CL_NO_CHIPSET_SUPPORT_ERR;
  }


  if (constel == RXN_GPS_CONSTEL)
  {
      /* Go through ephemeris records and ensure that any with all "C" terms having a value
       * of "0" will not be processed as these are likely to be EE data previously injected. */
        for(index = 0; index < pNavDataList->numEntries ; index++)
      {
            if( (pNavDataList->ephList.gps[index].cic == 0) && (pNavDataList->ephList.gps[index].cis == 0) &&
                (pNavDataList->ephList.gps[index].crc == 0) && (pNavDataList->ephList.gps[index].crs == 0) &&
                (pNavDataList->ephList.gps[index].cuc == 0) && (pNavDataList->ephList.gps[index].cus == 0) )
        {
          /* Record likely EE. Ensure that the record values are zero'd 
           * so that it is not used. */
                memset((void*) &(pNavDataList->ephList.gps[index]), 0, sizeof(RXN_FullEph_t));
        }
      }
  }

  return result;
}

U16 RXN_CL_WriteEphemeris(U16 handle, RXN_MSL_NavDataList_t* pNavDataList, RXN_constel_t constel)
{
  U08 handleIdx;          /* Index to the handle within gCL_Instances. */
  U16 result = RXN_FAIL;  /* CL_ReadEphemeris result. */
  
  /* See RXN_CL_Common.c for global declaraions. */
  extern CL_instance_t gCL_Instances[];

    if(pNavDataList == NULL)
    {
        return result;        
    }

  /* Look for the handle within gCL_Instances. */
  handleIdx = CL_FindHandle(handle);

  /* Handle the case where it isn't found. */
  if(handleIdx == 255)
  {
    return RXN_CL_INSTANCE_NOT_FOUND_ERR;
  }

    /* Verify that applicable storage is available. */
    if(pNavDataList->numEntries == 0 || pNavDataList->numEntries > numOfPRNs[constel]) 
  {
    return RXN_CL_ALLOCATION_ERROR;
  }

  /* Call the chipset specific implementation. */
  if(gCL_Instances[handleIdx].CL_Fcns.WriteEphemeris != NULL)
  {
        result = gCL_Instances[handleIdx].CL_Fcns.WriteEphemeris(&(gCL_Instances[handleIdx].CL_Attrib), pNavDataList, constel);

    /* Trace log. */
    CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE01,
      "RXN_CL_WriteEphemeris: Wrote ephemeris with result: %1.0f", (float) result);
  }
  else
  {
    return RXN_CL_NO_CHIPSET_SUPPORT_ERR;
  }

  return result;
}

U16 RXN_CL_GetFixData(U16 handle, RXN_CL_FixData_t* pFixData)
{
  U08 handleIdx;          /* Index to the handle within gCL_Instances. */
  U16 result = RXN_FAIL;  /* CL_ReadEphemeris result. */
  
  /* See RXN_CL_Common.c for global declaraions. */
  extern CL_instance_t gCL_Instances[];

  /* Look for the handle within gCL_Instances. */
  handleIdx = CL_FindHandle(handle);

  /* Handle the case where it isn't found. */
  if(handleIdx == 255)
  {
    return RXN_CL_INSTANCE_NOT_FOUND_ERR;
  }

  /* Call the chipset specific implementation. */
  if(gCL_Instances[handleIdx].CL_Fcns.GetFixData != NULL)
  {
    result = gCL_Instances[handleIdx].CL_Fcns.GetFixData(&(gCL_Instances[handleIdx].CL_Attrib),
                                                          pFixData);
  }
  else
  {
    return RXN_CL_NO_CHIPSET_SUPPORT_ERR;
  }

  return result;
}

U16 RXN_CL_GetGPSRcvrTime(U16 handle, U16* pRcvrWeekNum, U32* pRcvrTOW)
{
  U08 handleIdx;          /* Index to the handle within gCL_Instances. */
  U16 result = RXN_FAIL;  /* CL_ReadEphemeris result. */
  
  /* See RXN_CL_Common.c for global declaraions. */
  extern CL_instance_t gCL_Instances[];

  /* Look for the handle within gCL_Instances. */
  handleIdx = CL_FindHandle(handle);

  /* Handle the case where it isn't found. */
  if(handleIdx == 255)
  {
    return RXN_CL_INSTANCE_NOT_FOUND_ERR;
  }

  /* Call the chipset specific implementation. */
  if(gCL_Instances[handleIdx].CL_Fcns.GetGPSRcvrTime != NULL)
  {
    result = gCL_Instances[handleIdx].CL_Fcns.GetGPSRcvrTime(&(gCL_Instances[handleIdx].CL_Attrib),
                                                          pRcvrWeekNum, pRcvrTOW);
  }
  else
  {
    return RXN_CL_NO_CHIPSET_SUPPORT_ERR;
  }

  return result;
}

S32 RXN_CL_GetClkOffset(void)
{
    extern S32 gCL_ClockOffset;
    
    return gCL_ClockOffset;
}

void RXN_CL_RXNToFull( RXN_ephem_t* pRXN, RXN_FullEph_t* pFull, U08 CAOrPOnL2,
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

void RXN_CL_FullToRXN(RXN_FullEph_t* pFull, RXN_ephem_t* pRXN)
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


