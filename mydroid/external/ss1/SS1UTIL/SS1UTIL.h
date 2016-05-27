/*****< SS1UTIL.h >************************************************************/
/*      Copyright 2000 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  SS1UTIL - Definitions, Constants, and Prototypes, for use with the        */
/*            utility routines used by the Stonestreet One Android            */
/*            CompatibilityLayers (BTJB, A2DP, etc).                          */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   07/18/10  G. Hensley     Initial creation.                               */
/******************************************************************************/

#ifndef __SS1UTILH__
#define __SS1UTILH__

#include "utils/Log.h"

#include <pthread.h>
#include <stdio.h>

#ifdef __cplusplus
extern "C"
{
#endif

#include "SS1BTPM.h"
#include "SS1UTILI.h"

int PthreadCondWaitMS(pthread_cond_t *Cond, pthread_mutex_t *Mutex, unsigned long TimeoutMS);

   /* The following macros are used to convert Bluetooth device         */
   /* addresses between the string formats used by Android (in the style*/
   /* of BlueZ), to/from BD_ADDR_t structures used in Bluetopia.        */
#define BD_ADDRToStr(_buffer, _blen, _addr) ( ((_blen) > 17) && (snprintf((_buffer), (_blen), "%02hhX:%02hhX:%02hhX:%02hhX:%02hhX:%02hhX", (_addr).BD_ADDR5, (_addr).BD_ADDR4, (_addr).BD_ADDR3, (_addr).BD_ADDR2, (_addr).BD_ADDR1, (_addr).BD_ADDR0) == 17) )
#define BD_ADDRToPath(_buffer, _blen, _addr) ( ((_blen) > 22) && (snprintf((_buffer), (_blen), "/dev_%02hhX_%02hhX_%02hhX_%02hhX_%02hhX_%02hhX", (_addr).BD_ADDR5, (_addr).BD_ADDR4, (_addr).BD_ADDR3, (_addr).BD_ADDR2, (_addr).BD_ADDR1, (_addr).BD_ADDR0) == 22) )
#define StrToBD_ADDR(_addr, _str) ( sscanf((_str), "%2hhX:%2hhX:%2hhX:%2hhX:%2hhX:%2hhX", &((_addr)->BD_ADDR5), &((_addr)->BD_ADDR4), &((_addr)->BD_ADDR3), &((_addr)->BD_ADDR2), &((_addr)->BD_ADDR1), &((_addr)->BD_ADDR0)) == 6 )
#define PathToBD_ADDR(_addr, _path) ( sscanf((_path), "/dev_%2hhX_%2hhX_%2hhX_%2hhX_%2hhX_%2hhX", &((_addr)->BD_ADDR5), &((_addr)->BD_ADDR4), &((_addr)->BD_ADDR3), &((_addr)->BD_ADDR2), &((_addr)->BD_ADDR1), &((_addr)->BD_ADDR0)) == 6 )

   /* The following macros are used to convert 128-bit Bluetooth service*/
   /* UUIDs between the string format used by Android and the UUID_128_t*/
   /* structure used in Bluetopia.                                      */
#define UUID128ToStr(_buffer, _blen, _uuid128) ( ((_blen) > 36) && (snprintf((_buffer), (_blen), "%02hhX%02hhX%02hhX%02hhX-%02hhX%02hhX-%02hhX%02hhX-%02hhX%02hhX-%02hhX%02hhX%02hhX%02hhX%02hhX%02hhX", (_uuid128).UUID_Byte0, (_uuid128).UUID_Byte1, (_uuid128).UUID_Byte2, (_uuid128).UUID_Byte3, (_uuid128).UUID_Byte4, (_uuid128).UUID_Byte5, (_uuid128).UUID_Byte6, (_uuid128).UUID_Byte7, (_uuid128).UUID_Byte8, (_uuid128).UUID_Byte9, (_uuid128).UUID_Byte10, (_uuid128).UUID_Byte11, (_uuid128).UUID_Byte12, (_uuid128).UUID_Byte13, (_uuid128).UUID_Byte14, (_uuid128).UUID_Byte15) == 36) )
#define StrToUUID128(_uuid128, _str) (sscanf((_str), "%2hhX%2hhX%2hhX%2hhX-%2hhX%2hhX-%2hhX%2hhX-%2hhX%2hhX-%2hhX%2hhX%2hhX%2hhX%2hhX%2hhX", &((_uuid128)->UUID_Byte0), &((_uuid128)->UUID_Byte1), &((_uuid128)->UUID_Byte2), &((_uuid128)->UUID_Byte3), &((_uuid128)->UUID_Byte4), &((_uuid128)->UUID_Byte5), &((_uuid128)->UUID_Byte6), &((_uuid128)->UUID_Byte7), &((_uuid128)->UUID_Byte8), &((_uuid128)->UUID_Byte9), &((_uuid128)->UUID_Byte10), &((_uuid128)->UUID_Byte11), &((_uuid128)->UUID_Byte12), &((_uuid128)->UUID_Byte13), &((_uuid128)->UUID_Byte14), &((_uuid128)->UUID_Byte15)) == 16)

int InitBTPMClient(BTPM_Initialization_Info_t *InitializationInfo, BTPM_Server_UnRegistration_Callback_t ServerUnRegistrationCallback, void *ServerUnRegistrationParameter, unsigned int MaximumRetries, unsigned int TimeoutMS);

#define InitBTPMClientNoRetries(InitializationInfo, ServerUnRegistrationCallback, ServerUnRegistrationParameter) InitBTPMClient(InitializationInfo, ServerUnRegistrationCallback, ServerUnRegistrationParameter, BTPM_CLIENT_INIT_DEFAULT_RETRY_COUNT, BTPM_CLIENT_INIT_DEFAULT_DELAY_MS)

void CloseBTPMClient();

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* __SS1UTILH__ */
