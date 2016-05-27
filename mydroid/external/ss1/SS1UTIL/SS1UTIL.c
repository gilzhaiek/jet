/*****< SS1UTIL.cpp >**********************************************************/
/*      Copyright 2000 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  SS1UTIL - Utility routines used by Stonestreet One Android Compatibility  */
/*            Layers (BTJB, A2DP, etc).                                       */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   09/23/10  G. Hensley     Initial creation.                               */
/******************************************************************************/

#define LOG_TAG "SS1UTIL"

#include "utils/Log.h"

#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>

#ifdef HAVE_BLUETOOTH

#include "SS1BTPM.h"

#include "SS1UTIL.h"

typedef struct _tagIPCLostCallbackParameter_t
{
   BTPM_Server_UnRegistration_Callback_t Callback;
   void                                 *Parameter;
} IPCLostCallbackParameter_t;

   /* BTPM connections are shared across the process, so we use global  */
   /* storage to track connection state.                                */
static int                        BTPMServiceConnected      = 0;
static pthread_mutex_t            BTPMServiceConnectedMutex = PTHREAD_MUTEX_INITIALIZER;
static IPCLostCallbackParameter_t IPCLostCallbackParameter  = {NULL, NULL};

   /* The following function is responsible for noting when the IPC     */
   /* connection to the BluetopiaPM service is lost. It will also       */
   /* call a additional callback if one has been registered via         */
   /* InitBTPMClient().                                                 */
static void IPCLostCallback(void *CallbackParameter)
{
   IPCLostCallbackParameter_t *RealCallback;

   if(pthread_mutex_lock(&BTPMServiceConnectedMutex) == 0)
   {
      BTPMServiceConnected = 0;
      pthread_mutex_unlock(&BTPMServiceConnectedMutex);
   }
   else
   {
      /* Having the mutex is nice, but set the flag anyway even when the*/
      /* mutex throws an error. If the mutex is working, we'll never hit*/
      /* this, so it's only an emergency fallback.                      */
      BTPMServiceConnected = 0;
   }

   /* If we have a real callback specified, and it appears valid, call  */
   /* it now.                                                           */
   if(CallbackParameter)
   {
      RealCallback = (IPCLostCallbackParameter_t *)CallbackParameter;

      if(RealCallback->Callback)
      {
         (*(RealCallback->Callback))(RealCallback->Parameter);
      }
   }
}

   /* The following function wraps the pthread_cond_timedwait() function*/
   /* to streamline the use of relative timeouts. The timeout duration  */
   /* is specified in milliseconds and the function will maintain the   */
   /* wait until either the condition is flagged or the timeout elapses,*/
   /* regardless of interrupt signals.                                  */
int PthreadCondWaitMS(pthread_cond_t *Cond, pthread_mutex_t *Mutex, unsigned long TimeoutMS)
{
   int             Result;
   struct timespec TS;

   clock_gettime(CLOCK_REALTIME, &TS);

   TS.tv_sec  += (TimeoutMS / 1000);
   TS.tv_nsec += ((TimeoutMS % 1000) * 1000 * 1000);

   if(TS.tv_nsec > 1000000000)
   {
      TS.tv_sec  += 1;
      TS.tv_nsec -= 1000000000;
   }

   while((Result = pthread_cond_timedwait(Cond, Mutex, &TS)) == EINTR)
   {
      /* Keep waiting when we're interrupted. Only stop if the condition*/
      /* is triggered or we time out.                                   */
   }

   return(Result);
}

   /* The following function wraps the BTPM_Initialize() routine to     */
   /* initialize an IPC link to the BluetopiaPM service. In addition to */
   /* the services provided by BTPM_Initialize() (configuration options */
   /* and "link-lost" callback registration), this function will also   */
   /* repeatedly retry a failed initialization. The fourth parameter    */
   /* specifies the maximum number of retries (not counting the first   */
   /* attempt), and the fifth parameter gives the delay between attempts*/
   /* (specified in milliseconds). Connection state is maintained, so   */
   /* initialization will only be attempted if the IPC link is not yet  */
   /* connected or has been lost. On success (or if the link is already */
   /* established), the function returns 0. A negative value indicates  */
   /* an error.                                                         */
int InitBTPMClient(BTPM_Initialization_Info_t *InitializationInfo, BTPM_Server_UnRegistration_Callback_t ServerUnRegistrationCallback, void *ServerUnRegistrationParameter, unsigned int MaximumRetries, unsigned int TimeoutMS)
{
   int                         ret_val;
   unsigned int                RetryCount;
   IPCLostCallbackParameter_t *RealCallbackPtr;

//   SS1_LOGD("Enter (%p, %p, %p, %u, %u)", InitializationInfo, ServerUnRegistrationCallback, ServerUnRegistrationParameter, MaximumRetries, TimeoutMS);

   if(pthread_mutex_lock(&BTPMServiceConnectedMutex) == 0)
   {
      if(BTPMServiceConnected == 0)
      {
         BTPM_Cleanup();

         /* Set the debug zone mask even before initializing the client */
         /* library. This will avoid any unwanted chatter during the    */
         /* handshake with the service.                                 */
         //if((ret_val = BTPM_SetDebugZoneMask(FALSE, (BTPM_CLIENT_DEBUG_ZONES | BTPM_CLIENT_DEBUG_LEVELS))) != 0)
         //   SS1_LOGW("BTPM_SetDebugZoneMask failed (%d)", ret_val);
         BTPM_DebugSetZoneMask((BTPM_CLIENT_DEBUG_ZONES | BTPM_CLIENT_DEBUG_LEVELS));
         BTPM_DebugSetZoneMask((BTPM_CLIENT_DEBUG_ZONES_PAGE_1 | BTPM_CLIENT_DEBUG_LEVELS));
         BTPM_DebugSetZoneMask((BTPM_CLIENT_DEBUG_ZONES_PAGE_2 | BTPM_CLIENT_DEBUG_LEVELS));

         if(ServerUnRegistrationCallback && ServerUnRegistrationParameter)
         {
            IPCLostCallbackParameter.Callback  = ServerUnRegistrationCallback;
            IPCLostCallbackParameter.Parameter = ServerUnRegistrationParameter;

            RealCallbackPtr                    = &IPCLostCallbackParameter;
         }
         else
            RealCallbackPtr = NULL;

         ret_val = -1;
         for(RetryCount = 0; ((RetryCount <= MaximumRetries) && (ret_val != 0)); RetryCount++)
         {
            if((ret_val = BTPM_Initialize(getpid(), InitializationInfo, IPCLostCallback, RealCallbackPtr)) != 0)
            {
               if(ret_val == BTPM_ERROR_CODE_ALREADY_INITIALIZED)
                  ret_val = 0;
               else
               {
                  SS1_LOGE("Bluetooth library not initialized (%d)", ret_val);

                  if(RetryCount < MaximumRetries)
                     BTPS_Delay(TimeoutMS);
               }
            }
         }

         /* If we were able to connect to the stack service, flag it.   */
         if(ret_val == 0)
         {
            SS1_LOGI("Bluetooth library initialized for process %u", getpid());
            BTPMServiceConnected = 1;
         }
         else
         {
            SS1_LOGE("Bluetooth library initialization failed (%d)", ret_val);
         }
      }
      else
      {
         /* Already initialized.                                        */
         ret_val = 0;
      }

      pthread_mutex_unlock(&BTPMServiceConnectedMutex);
   }
   else
   {
      SS1_LOGE("Failed to acquire lock");
      ret_val = -1;
   }

//   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
}

   /* The following function is used to terminate an IPC link which was */
   /* established via InitBTPMClient().                                 */
void CloseBTPMClient()
{
   int HoldResult;

   SS1_LOGD("Enter");

   HoldResult = pthread_mutex_lock(&BTPMServiceConnectedMutex);

   BTPM_Cleanup();
   BTPMServiceConnected = 0;

   if(HoldResult == 0)
      pthread_mutex_unlock(&BTPMServiceConnectedMutex);

   SS1_LOGD("Exit");
}

#endif /* HAVE_BLUETOOTH */

