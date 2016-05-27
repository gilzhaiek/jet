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
 * $LastChangedDate: 2009-04-23 16:16:15 -0700 (Thu, 23 Apr 2009) $
 * $Revision: 10337 $
 *************************************************************************
 *
 */

#ifndef RXN_MSL_TIME_H
#define RXN_MSL_TIME_H

#ifdef __cplusplus
extern "C" {
#endif

#include "RXN_data_types.h"
#include "RXN_structs.h"
#include "RXN_MSL_Common.h"

enum MSL_Current_Time_Source 
{
    MSL_CURRENT_TIME_SOURCE_GPS = 0,    
    MSL_CURRENT_TIME_SOURCE_SNTP,    
    MSL_CURRENT_TIME_SOURCE_SYSTEM,
    MSL_CURRENT_TIME_SOURCE_NO,    
};

/* The default current value of GPS leap seconds*/
#define MSL_DEFAULT_GPS_LEAP_SECOND_OFFSET 16
/* GLONASS N4 base year, the year which N4 was equal to 0 */
#define MSL_GLONASS_N4_BASE_YEAR 1996

U32 MSL_GetBestGPSTime(enum MSL_Current_Time_Source* pTimeSource);
U16 MSL_SetGPSTime(RXN_RefTime_t* pRefTime, U16 gps_wn);

void MSL_SetSNTPTime(U32 SNTPTime);
U16 MSL_GetSNTPTime(RXN_RefTime_t* pRefTime);

void MSL_SetTimeUncertThresh(U32 thresh);
U32 MSL_GetTimeUncertThresh();

BOOL MSL_IsGPSTimeAcceptable();
BOOL MSL_IsSNTPTimeAcceptable();

BOOL MSL_UseBoundedGLOSec();
U32 MSL_ConvertGLOSecToBoundedGLOSec(U32 gloSec);
void MSL_ConvertGLOSecToComponents(U32 gloSec, U08* N4, U16* NT, U08* tb);
U32 MSL_ConvertGLOComponentsToGLOSec(U08 N4, U16 NT, U08 tb);
U08 RXN_GetTauGPS(R32 *tauGPS);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* RXN_MSL_TIME_H */
