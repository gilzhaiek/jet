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

#ifndef RXN_MSL_RINEX_H
#define RXN_MSL_RINEX_H

#ifdef __cplusplus
extern "C" {
#endif

U08 MSL_GPS_ProcessRinexFile(const char rinexFile[RXN_MSL_MAX_PATH]);
U08 MSL_GLO_ProcessRinexFile(const char rinexFile[RXN_MSL_MAX_PATH]);


#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* RXN_MSL_TIME_H */
