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
 * $LastChangedDate: 2009-09-03 10:44:55 -0700 (Thu, 03 Sep 2009) $
 * $Revision: 13068 $
 *************************************************************************
 *
 */

#ifndef RXN_API_CALLS_EXTRA_H
#define RXN_API_CALLS_EXTRA_H

#ifdef __cplusplus
extern "C" {
#endif

/* 
 * \brief 
 * Initialize the O/S abstraction layer.
 *
 * \param pantryPath 
 * [In] Explicit or relative path to the pantry files.
 * \param verbose 
 *
 * \returns 
 * RXN_SUCCESS if the abstraction layer functions are initialized successfully. 
 * RXN_FAIL if an error is encountered. 
 */ 
U16 RXN_Init_Abstraction(char pantryPath[]);

/* 
 * \brief 
 * Uninitialize the O/S abstraction layer.
 *
 * \returns 
 * RXN_SUCCESS always at present. 
 */ 
U16 RXN_Uninit_Abstraction(void);

#ifdef __cplusplus
}
#endif

#endif /* RXN_API_CALLS_EXTRA_H */
