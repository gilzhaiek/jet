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

#ifndef RXN_MSL_OPTIONS_H
#define RXN_MSL_OPTIONS_H

/* The purpose of this header file is to define preprocessor defines used  
 * to configure how the MSL is built. It is strongly recommend that the
 * preprocessor defines in this file are only used to configure source 
 * files only. If these preprocessor defines are used to configure header
 * files special care must be taken to ensure the header include order is 
 * correct.
 */

#define RXN_CONFIG_INCLUDE_GLONASS      /* Enable GLONASS support. */
#define RXN_CORE_LIBRARY_7              /* Use core library version 7. */

#endif /* RXN_MSL_OPTIONS_H */
